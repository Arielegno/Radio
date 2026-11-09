package io.github.arielegno.radio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/** Lanza una tarea que nunca tumba la app si algo falla. */
fun CoroutineScope.launchSafe(block: suspend CoroutineScope.() -> Unit): Job = launch {
    try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { Log.w("Radio", "Tarea falló", e) }
}

/** La voz de Luna: el motor de texto a voz del teléfono, en español. */
class VozLuna(private val ctx: Context) {
    private var listo = CompletableDeferred<Boolean>()
    private val pendientes = ConcurrentHashMap<String, (Boolean) -> Unit>()
    private val n = AtomicInteger()
    @Volatile private var hablandoTts = false
    /** Luna está hablando (con el motor del teléfono o con una voz propia de la app). */
    val hablando: Boolean get() = hablandoTts || VozPropia.hablando

    // Va antes de «tts» porque crear() lo usa al iniciar.
    private val progreso: UtteranceProgressListener = run {
        object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { if (utteranceId != "calentar") hablandoTts = true }
            override fun onDone(utteranceId: String?) { hablandoTts = false; utteranceId?.let { pendientes.remove(it)?.invoke(true) } }
            @Deprecated("Requerido por la clase base") override fun onError(utteranceId: String?) { hablandoTts = false; utteranceId?.let { pendientes.remove(it)?.invoke(false) } }
            override fun onError(utteranceId: String?, errorCode: Int) { hablandoTts = false; utteranceId?.let { pendientes.remove(it)?.invoke(false) } }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { hablandoTts = false; utteranceId?.let { pendientes.remove(it)?.invoke(false) } }
        }
    }

    private var tts: TextToSpeech = crear(RadioApp.prefs.str("motorVoz", "").let { if (it.startsWith(VozPropia.PREFIJO)) "" else it })

    private fun crear(motor: String): TextToSpeech {
        val d = CompletableDeferred<Boolean>(); listo = d
        val oyente = TextToSpeech.OnInitListener { status ->
            d.complete(status == TextToSpeech.SUCCESS)
        }
        val t = if (motor.isEmpty()) TextToSpeech(ctx.applicationContext, oyente) else TextToSpeech(ctx.applicationContext, oyente, motor)
        t.setOnUtteranceProgressListener(progreso)
        return t
    }

    /** Cambia el motor de voz (Google, SherpaTTS con Piper o Kokoro, RHVoice…). "" = el predeterminado. */
    fun cambiarMotor(motor: String) {
        RadioApp.prefs.set("motorVoz", motor); RadioApp.prefs.set("voz", "")
        if (motor.startsWith(VozPropia.PREFIJO)) return   // voz propia de la app: no hace falta otro motor
        try { tts.stop(); tts.shutdown() } catch (e: Exception) {}
        tts = crear(motor)
    }

    /** Motores de voz instalados en el teléfono (paquete, nombre). */
    suspend fun motores(): List<Pair<String, String>> {
        if (!(withTimeoutOrNull(8000) { listo.await() } ?: false)) return emptyList()
        return try { tts.engines.map { it.name to it.label } } catch (e: Exception) { emptyList() }
    }

    private suspend fun preparar(): Boolean {
        val ok = withTimeoutOrNull(20_000) { listo.await() } ?: false   // las voces neuronales tardan más en arrancar
        if (!ok) return false
        val p = RadioApp.prefs
        val loc = when (p.str("acento", "neutro")) { "mx" -> Locale("es", "MX"); "ar" -> Locale("es", "AR"); "es" -> Locale("es", "ES"); "co" -> Locale("es", "CO"); "cl" -> Locale("es", "CL"); "hn" -> Locale("es", "HN"); else -> Locale("es", "US") }
        val r = tts.setLanguage(loc)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(Locale("es"))
        val nombreVoz = p.str("voz", "")
        if (nombreVoz.isNotEmpty()) tts.voices?.firstOrNull { it.name == nombreVoz }?.let { tts.setVoice(it) }
        // Estilos de voz como en la web; «manual» usa los controles de velocidad y tono
        var (vel, ton) = when (p.str("vozPreset", "natural")) {
            "radio" -> 88 to 85; "narrador" -> 100 to 95; "energico" -> 110 to 105; "manual" -> p.int("velocidad", 100) to p.int("tono", 100); else -> 100 to 100
        }
        // SherpaTTS, RHVoice y otras voces neuronales ya suenan naturales: cambiarles el tono las deja «metálicas».
        // Solo se ajustan con el estilo «manual»; el tono se deja igual salvo con la voz de Google.
        val google = p.str("motorVoz", "").let { it.isEmpty() || it == "com.google.android.tts" }
        if (!google && p.str("vozPreset", "natural") != "manual") { vel = 100; ton = 100 }
        tts.setSpeechRate(vel / 100f)
        tts.setPitch(ton / 100f)
        return true
    }

    /** Voces en español instaladas en el teléfono. */
    suspend fun vocesEspanol(): List<Voice> {
        if (!(withTimeoutOrNull(8000) { listo.await() } ?: false)) return emptyList()
        return try {
            val todas = tts.voices.orEmpty().filter { it.locale.language == "es" || it.locale.language == "spa" }
            todas.filter { !it.isNetworkConnectionRequired }.ifEmpty { todas }.sortedBy { it.name }
        } catch (e: Exception) { emptyList() }
    }

    /** Habla y espera a que termine. Devuelve false si no se pudo. */
    /** ¿Están listos los dos locutores? (las dos voces de la app descargadas y el modo activado) */
    fun duo(): Pair<String, String>? {
        val p = RadioApp.prefs
        if (!p.bool("dosLocutores", false)) return null
        val a = p.str("motorVoz", "").removePrefix(VozPropia.PREFIJO); val b = p.str("vozPareja", "")
        val va = VozPropia.porId(a) ?: return null; val vb = VozPropia.porId(b) ?: return null
        return if (a != b && VozPropia.instalada(va) && VozPropia.instalada(vb)) a to b else null
    }

    /** Audio de Luna (y su compañero) ya generado, listo para sonar al instante. */
    class Preparado(val piezas: List<VozPropia.Pieza>)

    /**
     * Separa el guion por locutor: «Luna: … Leo: …» (en líneas distintas o todo seguido).
     * Devuelve (esElCompañero, texto) por cada turno. Sin marcas, todo es de Luna.
     */
    fun partir(texto0: String): List<Pair<Boolean, String>> {
        val p = RadioApp.prefs
        val host = p.str("host", "Luna").trim().ifEmpty { "Luna" }; val host2 = p.str("host2", "Leo").trim().ifEmpty { "Leo" }
        val marca = Regex("(?:^|(?<=[\\s.!?¡¿,;:\"«»—–-]))(${Regex.escape(host)}|${Regex.escape(host2)})\\s*:", RegexOption.IGNORE_CASE)
        val out = mutableListOf<Pair<Boolean, String>>()
        var quien = false; var desde = 0
        for (m in marca.findAll(texto0)) {
            val antes = texto0.substring(desde, m.range.first).trim()
            if (antes.isNotEmpty()) out += quien to antes
            quien = m.groupValues[1].equals(host2, true) && !host2.equals(host, true)
            desde = m.range.last + 1
        }
        texto0.substring(desde).trim().takeIf { it.isNotEmpty() }?.let { out += quien to it }
        // turnos seguidos del mismo locutor se juntan
        return out.fold(mutableListOf()) { acc, x -> if (acc.isNotEmpty() && acc.last().first == x.first) acc[acc.lastIndex] = x.first to (acc.last().second + " " + x.second) else acc += x; acc }
    }

    /** El guion para mostrar en pantalla: cada locutor en su propio renglón. */
    fun paraMostrar(texto0: String): String {
        val partes = partir(texto0)
        if (partes.none { it.first }) return partes.joinToString(" ") { it.second }
        val p = RadioApp.prefs
        return partes.joinToString("\n") { (b, t) -> (if (b) p.str("host2", "Leo") else p.str("host", "Luna")) + ": " + t }
    }

    private fun velocidad(): Float = when (RadioApp.prefs.str("vozPreset", "natural")) { "radio" -> .9f; "narrador" -> 1f; "energico" -> 1.12f; "manual" -> RadioApp.prefs.int("velocidad", 100) / 100f; else -> 1f }

    /** Genera de antemano lo que dirá Luna (solo con las voces de la app). null = se hablará en el momento. */
    suspend fun preparar(texto0: String): Preparado? {
        val p = RadioApp.prefs
        val motor = p.str("motorVoz", "")
        if (!motor.startsWith(VozPropia.PREFIJO)) return null
        val principal = motor.removePrefix(VozPropia.PREFIJO)
        val dos = duo()
        val partes = partir(texto0)
        val piezas = mutableListOf<VozPropia.Pieza>()
        for ((esPareja, t) in partes) {
            val id = if (esPareja && dos != null) dos.second else principal
            piezas += VozPropia.generar(id, t, velocidad()) ?: return null
        }
        return if (piezas.isEmpty()) null else Preparado(piezas)
    }

    /** Hace sonar lo ya preparado. */
    suspend fun decirPreparado(pr: Preparado): Boolean {
        val vol = RadioApp.prefs.int("volVoz", 100).coerceIn(10, 100) / 100f
        val dura = pr.piezas.sumOf { it.muestras.size * 1000L / it.sr }
        return withTimeoutOrNull(dura + 15_000L) { VozPropia.reproducir(pr.piezas, vol) } ?: run { VozPropia.callar(); false }
    }

    suspend fun decir(texto0: String): Boolean {
        if (texto0.isBlank()) return false
        val p = RadioApp.prefs
        val vel = velocidad()
        val vol = p.int("volVoz", 100).coerceIn(10, 100) / 100f
        // Guion con dos locutores: «Luna: …» / «Leo: …», cada turno con su voz
        val partes = partir(texto0)
        val dos = duo()
        if (dos != null && partes.any { it.first }) {
            for ((esPareja, l) in partes) {
                val voz = if (esPareja) dos.second else dos.first
                val ok = withTimeoutOrNull(30_000L + l.length * 120L) { VozPropia.decir(voz, l, vel, vol) }
                if (ok != true) { VozPropia.callar(); return false }
            }
            return true
        }
        val texto = limpiarParaVoz(partes.joinToString(" ") { it.second })
        if (texto.isBlank()) return false
        val motor = p.str("motorVoz", "")
        if (motor.startsWith(VozPropia.PREFIJO)) {
            val ok = withTimeoutOrNull(30_000L + texto.length * 120L) { VozPropia.decir(motor.removePrefix(VozPropia.PREFIJO), texto, vel, vol) }
            if (ok == true) return true
            VozPropia.callar()
            // si la voz propia falla (por ejemplo, se borró), se usa la del teléfono
        }
        if (!preparar()) return false
        // Si el motor de voz se traba, la radio no se queda esperando: se corta y sigue la música
        val limite = 15_000L + texto.length * 110L
        val r = withTimeoutOrNull(limite) { hablar(texto) }
        if (r == null) { callar(); return false }
        if (!r) { kotlinx.coroutines.delay(1200); return withTimeoutOrNull(limite) { hablar(texto) } ?: run { callar(); false } }   // un segundo intento
        return true
    }

    private suspend fun hablar(texto: String): Boolean {
        val id = "luna" + n.incrementAndGet()
        return suspendCancellableCoroutine { cont ->
            pendientes[id] = { ok -> if (cont.isActive) cont.resume(ok) }
            cont.invokeOnCancellation { pendientes.remove(id); tts.stop() }
            val r = tts.speak(texto.take(TextToSpeech.getMaxSpeechInputLength() - 1), TextToSpeech.QUEUE_FLUSH, Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, RadioApp.prefs.int("volVoz", 100).coerceIn(10, 100) / 100f) }, id)
            if (r != TextToSpeech.SUCCESS) { pendientes.remove(id); if (cont.isActive) cont.resume(false) }
        }
    }

    fun callar() { try { tts.stop() } catch (e: Exception) {} ; hablandoTts = false; VozPropia.callar() }
}
