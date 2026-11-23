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
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Grabador de alta calidad: PCM 16 bits, 48 kHz, sin compresión.
 * - Elige la entrada: micrófono interno, USB tipo C, auriculares con cable o Bluetooth.
 * - Overdub: reproduce una pista base mientras grabas, y compensa la latencia para que quede sincronizado.
 * - Monitoreo: te escuchas en los audífonos mientras grabas.
 */
class AudioRecorder(private val context: Context) {

    val sampleRate = MotorAudio.SR
    private val am = context.getSystemService(AudioManager::class.java)

    @Volatile var isRecording = false
        private set
    private var thread: Thread? = null
    private var usingBluetooth = false

    /** Nivel de pico 0..1 para el vúmetro */
    var onLevel: ((Float) -> Unit)? = null

    fun inputDevices(): List<AudioDeviceInfo> =
        am.getDevices(AudioManager.GET_DEVICES_INPUTS)
            .filter { it.type != AudioDeviceInfo.TYPE_TELEPHONY }
            .toList()

    class Opciones(
        val base: FloatArray? = null,       // pista para grabar encima
        val volumenBase: Float = 0.8f,
        val latenciaMs: Int = 0,            // compensación de latencia (solo con base)
        val monitoreo: Boolean = false,
        val volumenMonitor: Float = 0.8f
    )

    @SuppressLint("MissingPermission") // el permiso se pide en la UI antes de grabar
    fun start(output: File, device: AudioDeviceInfo?, op: Opciones = Opciones()) {
        if (isRecording) return
        if (device?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) enableBluetooth(device)

        val rec = crearAudioRecord(device)
        val bufSize = rec.bufferSizeInFrames.coerceAtLeast(1024)
        val baseTrack = op.base?.let { crearTrack(AudioFormat.ENCODING_PCM_FLOAT) }
        val monitor = if (op.monitoreo) crearTrack(AudioFormat.ENCODING_PCM_16BIT) else null

        isRecording = true
        // hilo de la pista base
        val baseThread = if (baseTrack != null && op.base != null) Thread {
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
            baseTrack.stop(); baseTrack.release()
        } else null

        thread = Thread {
            val buffer = ShortArray(bufSize)
            val bytes = ByteArray(buffer.size * 2)
            var total = 0
            var saltar = if (op.base != null) op.latenciaMs * sampleRate / 1000 else 0
            RandomAccessFile(output, "rw").use { raf ->
                raf.setLength(0)
                Wav.writeHeader(raf, sampleRate, 1, 0)
                raf.seek(44)
                monitor?.play()
                rec.startRecording()
                baseThread?.start()
                while (isRecording) {
                    val n = rec.read(buffer, 0, buffer.size)
                    if (n <= 0) continue
                    if (monitor != null) {
                        if (op.volumenMonitor != 1f) {
                            val m = ShortArray(n) { (buffer[it] * op.volumenMonitor).toInt().toShort() }
                            monitor.write(m, 0, n, AudioTrack.WRITE_NON_BLOCKING)
                        } else monitor.write(buffer, 0, n, AudioTrack.WRITE_NON_BLOCKING)
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
                    onLevel?.invoke(peak / 32768f)
                }
                rec.stop(); rec.release()
                monitor?.let { it.stop(); it.release() }
                Wav.writeHeader(raf, sampleRate, 1, total) // cabecera definitiva
            }
        }.also { it.priority = Thread.MAX_PRIORITY; it.start() }
    }

    fun stop() {
        isRecording = false
        thread?.join()
        thread = null
        if (usingBluetooth) disableBluetooth()
    }

    /**
     * Mide la latencia de ida y vuelta: toca clicks por la bocina y los busca en la grabación.
     * Hazlo en silencio, con el volumen medio y SIN audífonos. Devuelve ms o -1 si falló.
     */
    @SuppressLint("MissingPermission")
    fun calibrarLatencia(device: AudioDeviceInfo?): Int {
        val sr = sampleRate
        val periodo = sr / 2
        val base = FloatArray(sr * 3)
        val clickLen = sr / 200
        for (c in 1..4) for (i in 0 until clickLen)
            base[c * periodo + i] = (sin(2 * PI * 2000 * i / sr) * exp(-i / (clickLen / 4.0))).toFloat() * 0.9f
        val rec = crearAudioRecord(device)
        val track = crearTrack(AudioFormat.ENCODING_PCM_FLOAT)
        val captura = ShortArray(base.size + sr)
        var pos = 0
        val t = Thread { track.play(); track.write(base, 0, base.size, AudioTrack.WRITE_BLOCKING); track.stop(); track.release() }
        rec.startRecording(); t.start()
        while (pos < captura.size) {
            val n = rec.read(captura, pos, minOf(1024, captura.size - pos)); if (n <= 0) break; pos += n
        }
        rec.stop(); rec.release(); t.join()
        // para cada click, buscar el pico de energía más cercano después
        val retrasos = mutableListOf<Int>()
        for (c in 1..4) {
            val desde = c * periodo
            var best = -1; var bestV = 0
            for (i in desde until minOf(desde + periodo - clickLen, pos)) {
                val v = abs(captura[i].toInt()); if (v > bestV) { bestV = v; best = i }
            }
            if (bestV > 2000) retrasos += best - desde
        }
        if (retrasos.size < 3) return -1
        retrasos.sort()
        return retrasos[retrasos.size / 2] * 1000 / sr
    }

    @SuppressLint("MissingPermission")
    private fun crearAudioRecord(device: AudioDeviceInfo?): AudioRecord {
        // UNPROCESSED evita filtros de llamada (supresión de ruido, AGC) si el teléfono lo soporta
        val unprocessed = am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val source = when {
            usingBluetooth -> MediaRecorder.AudioSource.VOICE_COMMUNICATION
            unprocessed -> MediaRecorder.AudioSource.UNPROCESSED
            else -> MediaRecorder.AudioSource.MIC
        }
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(source, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
        device?.let { rec.preferredDevice = it }
        return rec
    }

    private fun crearTrack(encoding: Int): AudioTrack {
        val min = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, encoding)
        return AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(encoding)
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
            return if (nombre.isBlank()) tipo else "$tipo · $nombre"
        }
    }
}
