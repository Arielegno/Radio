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
class VozLuna(ctx: Context) {
    private val listo = CompletableDeferred<Boolean>()
    private val pendientes = ConcurrentHashMap<String, (Boolean) -> Unit>()
    private val n = AtomicInteger()
    @Volatile var hablando = false; private set

    private val tts: TextToSpeech = TextToSpeech(ctx.applicationContext) { status ->
        listo.complete(status == TextToSpeech.SUCCESS)
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { hablando = true }
            override fun onDone(utteranceId: String?) { hablando = false; utteranceId?.let { pendientes.remove(it)?.invoke(true) } }
            @Deprecated("Requerido por la clase base") override fun onError(utteranceId: String?) { hablando = false; utteranceId?.let { pendientes.remove(it)?.invoke(false) } }
            override fun onError(utteranceId: String?, errorCode: Int) { hablando = false; utteranceId?.let { pendientes.remove(it)?.invoke(false) } }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { hablando = false; utteranceId?.let { pendientes.remove(it)?.invoke(false) } }
        })
    }

    private suspend fun preparar(): Boolean {
        val ok = withTimeoutOrNull(8000) { listo.await() } ?: false
        if (!ok) return false
        val p = RadioApp.prefs
        val loc = when (p.str("acento", "neutro")) { "mx" -> Locale("es", "MX"); "ar" -> Locale("es", "AR"); "es" -> Locale("es", "ES"); "co" -> Locale("es", "CO"); "cl" -> Locale("es", "CL"); "hn" -> Locale("es", "HN"); else -> Locale("es", "US") }
        val r = tts.setLanguage(loc)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(Locale("es"))
        val nombreVoz = p.str("voz", "")
        if (nombreVoz.isNotEmpty()) tts.voices?.firstOrNull { it.name == nombreVoz }?.let { tts.setVoice(it) }
        tts.setSpeechRate(p.int("velocidad", 100) / 100f)
        tts.setPitch(p.int("tono", 100) / 100f)
        return true
    }

    /** Voces en español instaladas en el teléfono. */
    suspend fun vocesEspanol(): List<Voice> {
        if (!(withTimeoutOrNull(8000) { listo.await() } ?: false)) return emptyList()
        return try { tts.voices.orEmpty().filter { it.locale.language == "es" && !it.isNetworkConnectionRequired }.sortedBy { it.name } } catch (e: Exception) { emptyList() }
    }

    /** Habla y espera a que termine. Devuelve false si no se pudo. */
    suspend fun decir(texto: String): Boolean {
        if (texto.isBlank() || !preparar()) return false
        val id = "luna" + n.incrementAndGet()
        return suspendCancellableCoroutine { cont ->
            pendientes[id] = { ok -> if (cont.isActive) cont.resume(ok) }
            cont.invokeOnCancellation { pendientes.remove(id); tts.stop() }
            val r = tts.speak(texto.take(TextToSpeech.getMaxSpeechInputLength() - 1), TextToSpeech.QUEUE_FLUSH, Bundle(), id)
            if (r != TextToSpeech.SUCCESS) { pendientes.remove(id); if (cont.isActive) cont.resume(false) }
        }
    }

    fun callar() { try { tts.stop() } catch (e: Exception) {} ; hablando = false }
}
