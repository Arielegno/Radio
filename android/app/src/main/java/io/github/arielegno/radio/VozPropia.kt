package io.github.arielegno.radio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Voces propias de la app (sin SherpaTTS ni internet): el mismo motor libre que usa SherpaTTS
 * (sherpa-onnx con voces Piper), dentro de la app. La voz se descarga una sola vez.
 */
object VozPropia {
    data class Voz(val id: String, val nombre: String, val detalle: String, val archivo: String, val mb: Int)

    val VOCES = listOf(
        Voz("daniela", "Daniela", "Mujer · Argentina · calidad alta", "vits-piper-es_AR-daniela-high", 116),
        Voz("ald", "Ald", "Hombre · México · calidad media", "vits-piper-es_MX-ald-medium", 67),
    )
    private const val BASE = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/"

    /** «propia:daniela» en motorVoz = usar esta voz. */
    const val PREFIJO = "propia:"

    var descargando by mutableStateOf<String?>(null); private set
    var progreso by mutableStateOf(0f); private set
    var version by mutableStateOf(0); private set
    @Volatile var hablando = false; private set

    private val dirVoces get() = File(RadioApp.app.filesDir, "voces").apply { mkdirs() }
    private fun dir(v: Voz) = File(dirVoces, v.archivo)
    private fun modelo(v: Voz): File? = dir(v).listFiles()?.firstOrNull { it.name.endsWith(".onnx") }
    fun instalada(v: Voz): Boolean = modelo(v) != null && File(dir(v), "tokens.txt").exists()
    fun porId(id: String) = VOCES.firstOrNull { it.id == id }

    /** La app pudo cargar el motor (algunos teléfonos muy viejos no lo admiten). */
    val disponible: Boolean by lazy { try { System.loadLibrary("sherpa-onnx-jni"); true } catch (e: Throwable) { false } }

    // ---------- Descarga ----------
    /** Descarga y descomprime la voz. Devuelve un mensaje para mostrar. */
    suspend fun descargar(v: Voz): String = withContext(Dispatchers.IO) {
        if (descargando != null) return@withContext "Ya se está descargando otra voz."
        descargando = v.id; progreso = 0f
        val tmp = File(RadioApp.app.cacheDir, v.archivo + ".tar.bz2")
        try {
            val c = URL(BASE + v.archivo + ".tar.bz2").openConnection() as HttpURLConnection
            c.connectTimeout = 20_000; c.readTimeout = 60_000; c.instanceFollowRedirects = true
            if (c.responseCode !in 200..299) return@withContext "No se pudo descargar la voz (error ${c.responseCode})."
            val total = c.contentLengthLong.takeIf { it > 0 } ?: (v.mb * 1_000_000L)
            c.inputStream.use { inp -> FileOutputStream(tmp).use { out ->
                val buf = ByteArray(256 * 1024); var n: Int; var hecho = 0L
                while (inp.read(buf).also { n = it } >= 0) { out.write(buf, 0, n); hecho += n; progreso = (hecho.toFloat() / total * .85f).coerceAtMost(.85f) }
            } }
            // Descomprimir (tar.bz2) dentro de la carpeta de voces
            Reportes.marca("Voz de la app: descomprimiendo ${v.nombre}")
            val destino = dirVoces.canonicalPath
            TarArchiveInputStream(BZip2CompressorInputStream(BufferedInputStream(tmp.inputStream(), 512 * 1024))).use { tar ->
                var e = tar.nextEntry
                while (e != null) {
                    val f = File(dirVoces, e.name)
                    if (!f.canonicalPath.startsWith(destino)) throw SecurityException("ruta no válida")   // nada fuera de la carpeta
                    if (e.isDirectory) f.mkdirs() else { f.parentFile?.mkdirs(); FileOutputStream(f).use { tar.copyTo(it, 256 * 1024) } }
                    progreso = .85f + .15f * (if (instalada(v)) 1f else .5f)
                    e = tar.nextEntry
                }
            }
            if (!instalada(v)) "La voz se descargó incompleta. Vuelve a intentarlo." else { version++; "Voz ${v.nombre} lista: funciona sin internet." }
        } catch (e: Exception) {
            dir(v).deleteRecursively(); "No se pudo descargar la voz: ${e.message ?: "sin conexión"}"
        } finally { tmp.delete(); descargando = null }
    }

    fun borrar(v: Voz) {
        if (cargada == v.id) liberar()
        dir(v).deleteRecursively(); version++
        if (RadioApp.prefs.str("motorVoz", "") == PREFIJO + v.id) RadioApp.prefs.set("motorVoz", "")
    }

    // ---------- Voz ----------
    private var tts: OfflineTts? = null
    private var cargada: String? = null
    private val turno = Mutex()
    @Volatile private var parar = false
    private var pista: AudioTrack? = null

    private fun liberar() { try { tts?.release() } catch (e: Throwable) {}; tts = null; cargada = null }
    /** Suelta la voz de la memoria si no está hablando (Android pidió memoria). Se vuelve a cargar al hablar. */
    fun soltarMemoria() { if (!hablando && turno.tryLock()) try { liberar() } finally { turno.unlock() } }

    private fun motor(v: Voz): OfflineTts? {
        if (cargada == v.id && tts != null) return tts
        liberar()
        val m = modelo(v) ?: return null
        val d = dir(v)
        val cfg = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(model = m.absolutePath, tokens = File(d, "tokens.txt").absolutePath,
                    dataDir = File(d, "espeak-ng-data").takeIf { it.isDirectory }?.absolutePath ?: ""),
                numThreads = maxOf(2, minOf(4, Runtime.getRuntime().availableProcessors() / 2)), debug = false, provider = "cpu"),
            silenceScale = 0.1f,   // menos silencio entre frases
        )
        Reportes.marca("Voz de la app: cargando ${v.nombre}")
        tts = OfflineTts(assetManager = null, config = cfg); cargada = v.id
        Reportes.marca("Voz de la app: ${v.nombre} cargada")
        return tts
    }

    /** Habla con la voz propia y espera a que termine. velocidad 1.0 = normal. efecto: "", "fm", "telefono", "estadio", "walkie". */
    suspend fun decir(id: String, texto: String, velocidad: Float, volumen: Float, efecto: String = RadioApp.prefs.str("efectoVoz", "")): Boolean = withContext(Dispatchers.Default) {
        val v = porId(id) ?: return@withContext false
        if (!disponible || !instalada(v)) return@withContext false
        val limpio = limpiarParaVoz(texto); if (limpio.isBlank()) return@withContext true
        turno.withLock {
            parar = false; hablando = true
            try {
                val t = motor(v) ?: return@withLock false
                val sr = t.sampleRate()
                val min = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
                val p = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(sr).setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(maxOf(min, sr * 4)).setTransferMode(AudioTrack.MODE_STREAM).build()
                pista = p
                p.setVolume(volumen.coerceIn(0f, 1f))
                val fx = Efecto(efecto, sr)
                // Se genera y suena a la vez; antes de empezar se junta un colchón de ~2,5 s para que no haya huecos
                val cola = java.util.concurrent.LinkedBlockingQueue<FloatArray>()
                val generadas = java.util.concurrent.atomic.AtomicLong(0)
                val listo = java.util.concurrent.atomic.AtomicBoolean(false)
                var escritos = 0L
                val reproductor = Thread {
                    try {
                        while (!parar && !listo.get() && generadas.get() < sr * 2.5) Thread.sleep(20)
                        p.play()
                        while (!parar) {
                            val c = cola.poll(200, java.util.concurrent.TimeUnit.MILLISECONDS) ?: if (listo.get() && cola.isEmpty()) break else continue
                            val o = fx.procesar(c); p.write(o, 0, o.size, AudioTrack.WRITE_BLOCKING); escritos += o.size
                        }
                        if (!parar) { val cola2 = fx.cola(); if (cola2.isNotEmpty()) { p.write(cola2, 0, cola2.size, AudioTrack.WRITE_BLOCKING); escritos += cola2.size } }
                    } catch (e: Throwable) {}
                }
                reproductor.start()
                try {
                    // Frase por frase con generate() (sin la llamada de vuelta del motor, que cerraba la app en algunos teléfonos)
                    val frases = limpio.split(Regex("(?<=[.!?;])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
                        .fold(mutableListOf<String>()) { acc, f -> if (acc.isNotEmpty() && acc.last().length < 40) acc[acc.lastIndex] = acc.last() + " " + f else acc += f; acc }
                    for (f in frases) {
                        if (parar) break
                        Reportes.marca("Voz de la app: hablando con ${v.nombre} («${f.take(40)}»)")
                        val audio = t.generate(f, 0, velocidad.coerceIn(.5f, 2f))
                        val muestras = audio.samples
                        if (muestras.isNotEmpty()) { cola.put(muestras); generadas.addAndGet(muestras.size.toLong()) }
                    }
                } finally { listo.set(true) }
                reproductor.join()
                if (!parar) {
                    val fin = System.currentTimeMillis() + escritos * 1000L / sr + 1500
                    while (!parar && p.playbackHeadPosition < escritos && System.currentTimeMillis() < fin) Thread.sleep(40)
                }
                try { p.stop() } catch (e: Exception) {}
                p.release(); pista = null
                !parar
            } catch (e: Throwable) {
                try { pista?.release() } catch (x: Throwable) {}; pista = null; false
            } finally { hablando = false }
        }
    }

    fun callar() { parar = true; try { pista?.pause(); pista?.flush() } catch (e: Exception) {} }
}

/** Deja el texto listo para leer en voz alta: sin markdown, emojis ni puntos suspensivos (que hacen pausas largas). */
fun limpiarParaVoz(t: String): String = t
    .replace(Regex("[*_#`~>|]"), " ")
    .replace(Regex("\\((.*?)\\)"), ", $1, ")
    .replace(Regex("[\\[\\]{}]"), " ")
    .replace(Regex("…|\\.{2,}"), ".")
    .replace(Regex("\\s+[—–-]\\s+"), ", ")
    .replace(Regex("[\\p{So}\\p{Cs}\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}]"), " ")
    .replace(Regex("\\s*\\n+\\s*"), " ")
    .replace(Regex("\\s+([,.;:!?])"), "$1")
    .replace(Regex("([,;:])[,;:.]+"), "$1")
    .replace(Regex("\\.([,.])+"), ".")
    .replace(Regex("\\s{2,}"), " ")
    .trim()

/** Efectos para las voces de la app (filtros sencillos, en tiempo real). */
private class Efecto(private val tipo: String, private val sr: Int) {
    private class Biquad(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double) {
        var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0
        fun p(x: Double): Double { val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2; x2 = x1; x1 = x; y2 = y1; y1 = y; return y }
    }
    private fun norm(b0: Double, b1: Double, b2: Double, a0: Double, a1: Double, a2: Double) = Biquad(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    private fun w(f: Double) = 2 * Math.PI * f / sr
    private fun pasaAltos(f: Double, q: Double = .707): Biquad { val w = w(f); val a = Math.sin(w) / (2 * q); val c = Math.cos(w); return norm((1 + c) / 2, -(1 + c), (1 + c) / 2, 1 + a, -2 * c, 1 - a) }
    private fun pasaBajos(f: Double, q: Double = .707): Biquad { val w = w(f); val a = Math.sin(w) / (2 * q); val c = Math.cos(w); return norm((1 - c) / 2, 1 - c, (1 - c) / 2, 1 + a, -2 * c, 1 - a) }
    private fun pico(f: Double, q: Double, db: Double): Biquad { val A = Math.pow(10.0, db / 40); val w = w(f); val a = Math.sin(w) / (2 * q); val c = Math.cos(w); return norm(1 + a * A, -2 * c, 1 - a * A, 1 + a / A, -2 * c, 1 - a / A) }
    private fun gravesSuaves(f: Double, db: Double): Biquad {
        val A = Math.pow(10.0, db / 40); val w = w(f); val c = Math.cos(w); val s = Math.sin(w); val a = s / 2 * Math.sqrt(2.0); val r = 2 * Math.sqrt(A) * a
        return norm(A * ((A + 1) - (A - 1) * c + r), 2 * A * ((A - 1) - (A + 1) * c), A * ((A + 1) - (A - 1) * c - r), (A + 1) + (A - 1) * c + r, -2 * ((A - 1) + (A + 1) * c), (A + 1) + (A - 1) * c - r)
    }
    private val filtros: List<Biquad> = when (tipo) {
        "fm" -> listOf(pasaAltos(85.0), gravesSuaves(170.0, 5.0), pico(3200.0, 1.0, 3.0), pasaBajos(12000.0))
        "telefono" -> listOf(pasaAltos(380.0), pasaAltos(380.0), pasaBajos(3200.0), pasaBajos(3200.0), pico(1500.0, 1.2, 4.0))
        "walkie" -> listOf(pasaAltos(550.0), pasaAltos(550.0), pasaBajos(2600.0), pasaBajos(2600.0))
        "estadio" -> listOf(pasaAltos(70.0), pico(2500.0, 1.0, 2.0))
        else -> emptyList()
    }
    private val empuje = when (tipo) { "fm" -> 1.8; "telefono" -> 2.4; "walkie" -> 3.5; else -> 0.0 }
    // Eco de estadio: dos repeticiones con caída
    private val eco = if (tipo == "estadio") FloatArray((sr * .5).toInt()) else null
    private var pos = 0

    fun procesar(x: FloatArray): FloatArray {
        if (tipo.isEmpty() || (filtros.isEmpty() && eco == null)) return x
        val y = FloatArray(x.size)
        for (i in x.indices) {
            var v = x[i].toDouble()
            for (f in filtros) v = f.p(v)
            if (empuje > 0) v = Math.tanh(empuje * v) / Math.tanh(empuje)   // compresión suave, como en la radio
            if (eco != null) {
                val d1 = eco[(pos - (sr * .13).toInt() + eco.size) % eco.size]; val d2 = eco[(pos - (sr * .29).toInt() + eco.size) % eco.size]
                val o = v + .38 * d1 + .22 * d2
                eco[pos] = o.toFloat(); pos = (pos + 1) % eco.size; v = o * .8
            }
            y[i] = v.coerceIn(-1.0, 1.0).toFloat()
        }
        return y
    }

    /** El final del eco (para que no se corte de golpe). */
    fun cola(): FloatArray = if (eco == null) FloatArray(0) else procesar(FloatArray((sr * .6).toInt()))
}
