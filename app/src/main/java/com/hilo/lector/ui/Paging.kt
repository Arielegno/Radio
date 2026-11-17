package com.hilo.lector.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import java.util.Locale

/**
 * Une la pantalla de lectura con la vista activa (PDF o texto):
 * regla, palabra a palabra, barra de páginas, índice, marcadores y lectura en voz alta.
 */
class ReaderBridge {
    var move: (suspend (Int) -> Unit)? = null
    var words: (suspend () -> List<String>)? = null

    /** Posición actual y total, en las unidades de la barra de páginas. */
    var position by mutableIntStateOf(0)
    var total by mutableIntStateOf(1)
    var unit by mutableStateOf("Pág.")
    var seek: (suspend (Int) -> Unit)? = null

    /** Ir a una página del PDF o a un párrafo del texto (índice, marcadores, notas). */
    var goPage: (suspend (Int) -> Unit)? = null
    var goPara: (suspend (Int) -> Unit)? = null

    /** Marcador en la posición actual: posición (página o párrafo) y una etiqueta corta. */
    var here: (() -> Pair<Int, String>)? = null

    /** Lectura en voz alta: bloques de texto desde la posición actual o después de [after]. */
    var speakFrom: (suspend (after: Int?) -> List<Pair<Int, String>>)? = null
    var follow: (suspend (Int) -> Unit)? = null
}

/**
 * Efecto de pasar página: la página que se va se desliza por encima de la siguiente,
 * que queda quieta debajo y se aclara a medida que aparece.
 */
fun Modifier.pageTurn(state: PagerState, page: Int): Modifier {
    fun off() = (state.currentPage - page) + state.currentPageOffsetFraction
    return this
        .zIndex(if (off() < 0f) 0f else 1f)
        .graphicsLayer {
            val o = off()
            if (o < 0f) {
                translationX = o * size.width
                shadowElevation = 0f
            } else {
                shadowElevation = if (o > 0.002f) 28f else 0f
            }
        }
        .drawWithContent {
            drawContent()
            val o = off()
            if (o < 0f) drawRect(Color.Black.copy(alpha = 0.32f * (-o).coerceIn(0f, 1f)))
        }
}

/** Lectura en voz alta con la voz del celular. */
class Speaker(ctx: Context) : TextToSpeech.OnInitListener {
    private val main = Handler(Looper.getMainLooper())
    private val tts = TextToSpeech(ctx.applicationContext, this)
    private var ready = false
    var speaking by mutableStateOf(false)
        private set
    private var onItem: ((Int) -> Unit)? = null
    private var onEnd: (() -> Unit)? = null
    private var lastId = ""

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return
        val r = tts.setLanguage(Locale("es"))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts.language = Locale.getDefault()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val p = utteranceId?.substringBefore(':')?.toIntOrNull() ?: return
                main.post { onItem?.invoke(p) }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == lastId) main.post { speaking = false; onEnd?.invoke() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId == lastId) main.post { speaking = false }
            }
        })
    }

    val available get() = ready

    /** Lee los bloques en orden. [onItem] avisa qué bloque empieza; [onEnd] cuando termina todo. */
    fun speak(items: List<Pair<Int, String>>, rate: Float, onItem: (Int) -> Unit, onEnd: () -> Unit, lang: String = ""): Boolean {
        if (!ready) return false
        setLang(lang)
        this.onItem = onItem
        this.onEnd = onEnd
        tts.setSpeechRate(rate)
        var first = true
        for ((pos, text) in items) {
            chunks(text).forEachIndexed { k, part ->
                val id = "$pos:$k"
                tts.speak(part, if (first) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, id)
                first = false
                lastId = id
            }
        }
        speaking = !first
        return !first
    }

    private fun setLang(lang: String) {
        runCatching {
            val r = tts.setLanguage(if (lang.isBlank()) Locale("es") else Locale.forLanguageTag(lang))
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts.language = Locale.getDefault()
        }
    }

    /** Dice una palabra o frase (tarjetas de idiomas). */
    fun say(text: String, lang: String) {
        if (!ready) return
        setLang(lang)
        tts.setSpeechRate(0.9f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "say")
    }

    fun stop() {
        onEnd = null
        onItem = null
        tts.stop()
        speaking = false
    }

    fun shutdown() {
        stop()
        tts.shutdown()
    }

    private fun chunks(t: String): List<String> {
        if (t.length < 3500) return listOf(t)
        val out = ArrayList<String>()
        val sb = StringBuilder()
        for (s in t.split(Regex("(?<=[.!?…])\\s+"))) {
            if (sb.length + s.length > 3500 && sb.isNotEmpty()) { out.add(sb.toString()); sb.setLength(0) }
            sb.append(s).append(' ')
        }
        if (sb.isNotBlank()) out.add(sb.toString())
        return out
    }
}
