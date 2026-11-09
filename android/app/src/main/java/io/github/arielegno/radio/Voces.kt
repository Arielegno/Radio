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

    suspend fun decir(texto0: String): Boolean {
        if (texto0.isBlank()) return false
        val p = RadioApp.prefs
        val host = p.str("host", "Luna"); val host2 = p.str("host2", "Leo")
        val vel = when (p.str("vozPreset", "natural")) { "radio" -> .9f; "narrador" -> 1f; "energico" -> 1.12f; "manual" -> p.int("velocidad", 100) / 100f; else -> 1f }
        val vol = p.int("volVoz", 100).coerceIn(10, 100) / 100f
        // Guion con dos locutores: «Luna: …» / «Leo: …», cada línea con su voz
        val marca = Regex("^\\s*(${Regex.escape(host)}|${Regex.escape(host2)})\\s*:\\s*", RegexOption.IGNORE_CASE)
        val lineas = texto0.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        val dos = duo()
        if (dos != null && lineas.count { marca.containsMatchIn(it) } >= 2) {
            for (l in lineas) {
                val quien = marca.find(l)?.groupValues?.get(1)
                val voz = if (quien != null && quien.equals(host2, true)) dos.second else dos.first
                val ok = withTimeoutOrNull(30_000L + l.length * 120L) { VozPropia.decir(voz, l.replace(marca, ""), vel, vol) }
                if (ok != true) { VozPropia.callar(); return false }
            }
            return true
        }
        val texto = limpiarParaVoz(lineas.joinToString(" ") { it.replace(marca, "") })
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
