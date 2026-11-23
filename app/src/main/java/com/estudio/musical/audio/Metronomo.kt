package com.estudio.musical.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Metrónomo preciso: genera los clicks muestra por muestra (no depende de temporizadores),
 * así nunca se desfasa aunque el teléfono esté ocupado.
 */
class Metronomo {
    @Volatile var bpm = 100f
    @Volatile var tiempos = 4
    @Volatile var volumen = 0.7f
    /** Se llama en el hilo principal con el número de tiempo (0 = primero del compás) */
    var onTiempo: ((Int) -> Unit)? = null

    @Volatile private var sonando = false
    private var hilo: Thread? = null
    private val sr = MotorAudio.SR
    private val main = Handler(Looper.getMainLooper())

    private fun click(f: Double, dur: Double) = FloatArray((sr * dur).toInt()) {
        val t = it.toDouble() / sr
        (sin(2 * PI * f * t) * exp(-t * 60)).toFloat()
    }
    private val fuerte = click(1760.0, 0.05)
    private val suave = click(1100.0, 0.04)

    val activo get() = sonando

    fun iniciar() {
        if (sonando) return
        sonando = true
        hilo = Thread {
            val min = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(sr)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(min, 2048 * 4))
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
            track.play()
            val bloque = FloatArray(512)
            var muestra = 0L
            var proximo = 0.0
            var tiempo = 0
            var clickPos = -1
            var clickBuf = fuerte
            while (sonando) {
                val beat = sr * 60.0 / bpm
                for (i in bloque.indices) {
                    if (muestra >= proximo) {
                        clickBuf = if (tiempo == 0) fuerte else suave
                        clickPos = 0
                        val t = tiempo
                        main.post { onTiempo?.invoke(t) }
                        tiempo = (tiempo + 1) % tiempos.coerceAtLeast(1)
                        proximo += beat
                    }
                    bloque[i] = if (clickPos in clickBuf.indices) clickBuf[clickPos++] * volumen else 0f
                    muestra++
                }
                track.write(bloque, 0, bloque.size, AudioTrack.WRITE_BLOCKING)
            }
            track.stop(); track.release()
        }.apply { priority = Thread.MAX_PRIORITY; start() }
    }

    fun detener() {
        sonando = false
        hilo?.join(300); hilo = null
    }
}
