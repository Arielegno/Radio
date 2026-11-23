package com.estudio.musical.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Grabador de alta calidad: PCM 16 bits, 48 kHz (o 44,1 kHz si el teléfono no da 48), sin compresión.
 * - Prueba varias fuentes de micrófono hasta encontrar una que de verdad entregue audio
 *   (algunos teléfonos dicen soportar el modo "sin procesar" pero no envían nada).
 * - Avisa si el micrófono falla o si llega silencio total (otra app lo está usando).
 * - Overdub: reproduce una pista base mientras grabas y compensa la latencia.
 * - Monitoreo: te escuchas en los audífonos mientras grabas.
 */
class AudioRecorder(private val context: Context) {

    /** Frecuencia de la última grabación iniciada */
    var sampleRate = MotorAudio.SR
        private set
    /** Descripción de la entrada que se está usando, p. ej. "Micrófono · 48 kHz" */
    var fuenteActual = ""
        private set

    private val am = context.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())

    @Volatile var isRecording = false
        private set
    private var thread: Thread? = null
    private var usingBluetooth = false

    /** Nivel de pico 0..1 para el vúmetro */
    var onLevel: ((Float) -> Unit)? = null
    /** Problema grave: la grabación se detuvo (se llama en el hilo principal) */
    var onError: ((String) -> Unit)? = null
    /** Advertencia: sigue grabando pero algo no está bien (se llama en el hilo principal) */
    var onAviso: ((String) -> Unit)? = null

    fun inputDevices(): List<AudioDeviceInfo> =
        am.getDevices(AudioManager.GET_DEVICES_INPUTS)
            .filter { it.type != AudioDeviceInfo.TYPE_TELEPHONY }
            .toList()

    class Opciones(
        val base: FloatArray? = null,       // pista para grabar encima (a 48 kHz)
        val volumenBase: Float = 0.8f,
        val latenciaMs: Int = 0,            // compensación de latencia (solo con base)
        val monitoreo: Boolean = false,
        val volumenMonitor: Float = 0.8f
    )

    class Entrada(val rec: AudioRecord, val sampleRate: Int, val descripcion: String)

    /**
     * Abre el micrófono probando fuentes y frecuencias hasta que una entregue datos reales.
     * Devuelve la entrada ya grabando, o null si ninguna funcionó.
     */
    @SuppressLint("MissingPermission")
    fun abrirEntrada(device: AudioDeviceInfo?): Entrada? {
        val unprocessed = am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val fuentes = buildList {
            if (usingBluetooth) add(MediaRecorder.AudioSource.VOICE_COMMUNICATION to "Bluetooth")
            add(MediaRecorder.AudioSource.MIC to "Micrófono")
            if (unprocessed) add(MediaRecorder.AudioSource.UNPROCESSED to "Micrófono sin procesar")
            add(MediaRecorder.AudioSource.VOICE_RECOGNITION to "Micrófono (voz)")
            add(MediaRecorder.AudioSource.CAMCORDER to "Micrófono (video)")
            add(MediaRecorder.AudioSource.DEFAULT to "Micrófono predeterminado")
        }
        for (rate in listOf(48_000, 44_100)) for ((src, nombre) in fuentes) {
            val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (min <= 0) continue
            val rec = try {
                AudioRecord(src, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min * 2, rate / 5 * 2))
            } catch (e: Exception) { continue }
            if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); continue }
            device?.let { rec.preferredDevice = it }
            try { rec.startRecording() } catch (e: Exception) { rec.release(); continue }
            if (rec.recordingState != AudioRecord.RECORDSTATE_RECORDING) { rec.release(); continue }
            // lectura de prueba: ¿llegan datos?
            val prueba = ShortArray(rate / 25)
            var leidos = 0
            repeat(3) { if (leidos <= 0) leidos = rec.read(prueba, 0, prueba.size) }
            if (leidos <= 0) { runCatching { rec.stop() }; rec.release(); continue }
            return Entrada(rec, rate, "$nombre · ${if (rate == 48_000) "48" else "44,1"} kHz")
        }
        return null
    }

    /**
     * Empieza a grabar. Devuelve null si todo va bien o un mensaje explicando el problema.
     */
    fun start(output: File, device: AudioDeviceInfo?, op: Opciones = Opciones()): String? {
        if (isRecording) return null
        if (device?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) enableBluetooth(device)

        val entrada = abrirEntrada(device) ?: run {
            if (usingBluetooth) disableBluetooth()
            return "No se pudo abrir el micrófono. Revisa que Tonalia tenga permiso de micrófono " +
                "y que ninguna otra app (llamada, grabadora, asistente de voz) lo esté usando."
        }
        val rec = entrada.rec
        sampleRate = entrada.sampleRate
        fuenteActual = entrada.descripcion
        val rate = entrada.sampleRate
        val bufSize = (rate / 50).coerceAtLeast(512) // bloques de 20 ms
        val baseTrack = op.base?.let { crearTrack(AudioFormat.ENCODING_PCM_FLOAT, MotorAudio.SR) }
        val monitor = if (op.monitoreo) crearTrack(AudioFormat.ENCODING_PCM_16BIT, rate) else null

        isRecording = true
        val baseThread = if (baseTrack != null && op.base != null) Thread {
            try {
                baseTrack.play()
                val chunk = 1024
                var p = 0
                val tmp = FloatArray(chunk)
                while (isRecording && p < op.base.size) {
                    val n = minOf(chunk, op.base.size - p)
                    for (i in 0 until n) tmp[i] = op.base[p + i] * op.volumenBase
                    baseTrack.write(tmp, 0, n, AudioTrack.WRITE_BLOCKING)
                    p += n
                }
                baseTrack.stop()
            } catch (_: Exception) {
            } finally { baseTrack.release() }
        } else null

        thread = Thread {
            val buffer = ShortArray(bufSize)
            val bytes = ByteArray(buffer.size * 2)
            var total = 0
            var saltar = if (op.base != null) op.latenciaMs * rate / 1000 else 0
            var errores = 0
            var picoMax = 0
            var muestras = 0L
            var avisoSilencio = false
            try {
                RandomAccessFile(output, "rw").use { raf ->
                    raf.setLength(0)
                    Wav.writeHeader(raf, rate, 1, 0)
                    raf.seek(44)
                    monitor?.play()
                    baseThread?.start()
                    try {
                        while (isRecording) {
                            val n = rec.read(buffer, 0, buffer.size)
                            if (n <= 0) {
                                if (++errores > 100) {
                                    main.post { onError?.invoke("El micrófono dejó de enviar sonido (código $n). Detén y vuelve a intentar.") }
                                    break
                                }
                                Thread.sleep(5); continue
                            }
                            errores = 0
                            if (monitor != null) {
                                val m = ShortArray(n) { (buffer[it] * op.volumenMonitor).toInt().toShort() }
                                monitor.write(m, 0, n, AudioTrack.WRITE_NON_BLOCKING)
                            }
                            var peak = 0
                            var start = 0
                            if (saltar > 0) { start = minOf(saltar, n); saltar -= start }
                            var k = 0
                            for (i in start until n) {
                                val s = buffer[i].toInt()
                                if (abs(s) > peak) peak = abs(s)
                                bytes[k++] = (s and 0xff).toByte()
                                bytes[k++] = (s shr 8 and 0xff).toByte()
                            }
                            raf.write(bytes, 0, k)
                            total += k
                            muestras += n
                            if (peak > picoMax) picoMax = peak
                            // 3 segundos de ceros exactos = el sistema nos está dando silencio
                            if (!avisoSilencio && muestras > rate * 3L && picoMax == 0) {
                                avisoSilencio = true
                                main.post { onAviso?.invoke("Llega silencio total. Otra app puede estar usando el micrófono (llamada, grabadora, asistente). Ciérrala y graba de nuevo.") }
                            }
                            onLevel?.invoke(peak / 32768f)
                        }
                    } finally {
                        runCatching { rec.stop() }
                        rec.release()
                        monitor?.let { runCatching { it.stop() }; it.release() }
                        Wav.writeHeader(raf, rate, 1, total) // cabecera definitiva
                    }
                }
                if (total == 0) {
                    output.delete()
                    main.post { onError?.invoke("No llegó audio del micrófono; la toma vacía se borró.") }
                }
            } catch (e: Exception) {
                main.post { onError?.invoke("Error al grabar: ${e.message ?: e.javaClass.simpleName}") }
            }
        }.also { it.priority = Thread.MAX_PRIORITY; it.start() }
        return null
    }

    fun stop() {
        isRecording = false
        thread?.join(2000)
        thread = null
        if (usingBluetooth) disableBluetooth()
    }

    /**
     * Prueba de micrófono: escucha [segundos] y devuelve un informe en palabras.
     * Bloquea el hilo: llámala desde una corrutina en Dispatchers.IO.
     */
    fun probar(device: AudioDeviceInfo?, segundos: Int = 3, onNivel: (Float) -> Unit): String {
        if (device?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) enableBluetooth(device)
        try {
            val e = abrirEntrada(device)
                ?: return "❌ Ningún modo del micrófono respondió. Revisa el permiso de micrófono de Tonalia y cierra otras apps que graben."
            val buf = ShortArray(e.sampleRate / 20)
            var pico = 0
            var leidas = 0L
            val fin = System.currentTimeMillis() + segundos * 1000
            while (System.currentTimeMillis() < fin) {
                val n = e.rec.read(buf, 0, buf.size)
                if (n <= 0) continue
                leidas += n
                var p = 0
                for (i in 0 until n) p = maxOf(p, abs(buf[i].toInt()))
                pico = maxOf(pico, p)
                main.post { onNivel(p / 32768f) }
            }
            runCatching { e.rec.stop() }; e.rec.release()
            val pct = pico * 100 / 32768
            return when {
                leidas == 0L -> "❌ El micrófono se abrió pero no envió datos (${e.descripcion})."
                pico == 0 -> "⚠️ Llega silencio total (${e.descripcion}). Otra app está usando el micrófono, o está silenciado."
                pct < 2 -> "⚠️ Se escucha muy bajo ($pct %) con ${e.descripcion}. Habla más cerca o revisa la conexión del micrófono."
                else -> "✅ El micrófono funciona: ${e.descripcion}, nivel máximo $pct %."
            }
        } finally {
            if (usingBluetooth) disableBluetooth()
        }
    }

    /**
     * Mide la latencia de ida y vuelta: toca clicks por la bocina y los busca en la grabación.
     * Hazlo en silencio, con el volumen medio y SIN audífonos. Devuelve ms o -1 si falló.
     */
    fun calibrarLatencia(device: AudioDeviceInfo?): Int {
        val sr = MotorAudio.SR
        val periodo = sr / 2
        val base = FloatArray(sr * 3)
        val clickLen = sr / 200
        for (c in 1..4) for (i in 0 until clickLen)
            base[c * periodo + i] = (sin(2 * PI * 2000 * i / sr) * exp(-i / (clickLen / 4.0))).toFloat() * 0.9f
        val e = abrirEntrada(device) ?: return -1
        val rs = e.sampleRate
        val track = crearTrack(AudioFormat.ENCODING_PCM_FLOAT, sr)
        val captura = ShortArray(base.size * rs / sr + rs)
        var pos = 0
        val t = Thread { track.play(); track.write(base, 0, base.size, AudioTrack.WRITE_BLOCKING); track.stop(); track.release() }
        t.start()
        while (pos < captura.size) {
            val n = e.rec.read(captura, pos, minOf(1024, captura.size - pos)); if (n <= 0) break; pos += n
        }
        runCatching { e.rec.stop() }; e.rec.release(); t.join()
        val periodoR = periodo.toLong() * rs / sr
        val clickR = clickLen * rs / sr
        val retrasos = mutableListOf<Int>()
        for (c in 1..4) {
            val desde = (c * periodoR).toInt()
            var bestV = 0; var best = -1
            for (i in desde until minOf(desde + periodoR.toInt() - clickR, pos)) {
                val v = abs(captura[i].toInt()); if (v > bestV) { bestV = v; best = i }
            }
            if (bestV > 2000) retrasos += best - desde
        }
        if (retrasos.size < 3) return -1
        retrasos.sort()
        return retrasos[retrasos.size / 2] * 1000 / rs
    }

    private fun crearTrack(encoding: Int, rate: Int): AudioTrack {
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, encoding)
        return AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(encoding)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(min * 2)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }

    private fun enableBluetooth(device: AudioDeviceInfo) {
        usingBluetooth = true
        if (Build.VERSION.SDK_INT >= 31) {
            am.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO && it.address == device.address }
                ?.let { am.setCommunicationDevice(it) }
        } else {
            @Suppress("DEPRECATION")
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION") am.startBluetoothSco()
            @Suppress("DEPRECATION") am.isBluetoothScoOn = true
        }
    }

    private fun disableBluetooth() {
        usingBluetooth = false
        if (Build.VERSION.SDK_INT >= 31) {
            am.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION") am.isBluetoothScoOn = false
            @Suppress("DEPRECATION") am.stopBluetoothSco()
            am.mode = AudioManager.MODE_NORMAL
        }
    }

    companion object {
        fun describe(d: AudioDeviceInfo): String {
            val tipo = when (d.type) {
                AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Micrófono del teléfono"
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB tipo C"
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Auriculares con cable"
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth"
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth (solo salida)"
                else -> "Entrada ${d.type}"
            }
            val nombre = d.productName?.toString().orEmpty()
            // en el micrófono interno el "nombre" suele ser el modelo del teléfono: no aporta
            return if (nombre.isBlank() || d.type == AudioDeviceInfo.TYPE_BUILTIN_MIC) tipo else "$tipo · $nombre"
        }

        /** Duración en segundos de un WAV a partir de su tamaño */
        fun duracion(f: File): Float = runCatching {
            val i = Wav.info(f); i.dataLen / (2f * i.channels * i.sampleRate)
        }.getOrDefault(0f)
    }
}
