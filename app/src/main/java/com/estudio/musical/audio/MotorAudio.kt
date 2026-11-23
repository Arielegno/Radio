package com.estudio.musical.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.estudio.musical.audio.dsp.Dynamics
import com.estudio.musical.audio.synth.FuenteSonido
import com.estudio.musical.audio.synth.Voz
import com.estudio.musical.audio.synth.VozBuffer

/**
 * Motor de sonido en tiempo real (baja latencia) para tocar los instrumentos
 * con el teclado, los pads de batería, el sampler y los ejemplos de teoría.
 * Se duerme solo cuando no suena nada para ahorrar batería.
 */
object MotorAudio {
    const val SR = 48_000
    private const val BLOQUE = 192
    private const val MAX_VOCES = 32

    private val lock = java.lang.Object()
    private val voces = ArrayList<Voz>()
    private var hilo: Thread? = null
    @Volatile private var activo = false
    @Volatile var volumen = 0.9f

    private fun asegurarHilo() {
        if (hilo != null) return
        activo = true
        hilo = Thread({ bucle() }, "MotorAudio").apply { priority = Thread.MAX_PRIORITY; start() }
    }

    private fun bucle() {
        val min = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(SR).setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(maxOf(min, BLOQUE * 4 * 4))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()
        val buf = FloatArray(BLOQUE)
        var silencioBloques = 0
        while (activo) {
            java.util.Arrays.fill(buf, 0f)
            val hay: Boolean
            synchronized(lock) {
                val it = voces.iterator()
                while (it.hasNext()) {
                    val v = it.next()
                    v.render(buf, 0, BLOQUE)
                    if (v.terminada) it.remove()
                }
                hay = voces.isNotEmpty()
                if (!hay && ++silencioBloques > SR * 5 / BLOQUE) {
                    // 5 s sin sonido: dormir hasta la próxima nota
                    track.pause()
                    (lock as java.lang.Object).wait()
                    silencioBloques = 0
                    if (activo) track.play()
                }
                if (hay) silencioBloques = 0
            }
            for (i in buf.indices) buf[i] = Dynamics.softClip(buf[i] * volumen)
            track.write(buf, 0, BLOQUE, AudioTrack.WRITE_BLOCKING)
        }
        track.stop(); track.release()
    }

    private fun agregar(v: Voz) {
        asegurarHilo()
        synchronized(lock) {
            if (voces.size >= MAX_VOCES) voces.removeAt(0)
            voces += v
            (lock as java.lang.Object).notifyAll()
        }
    }

    fun notaOn(f: FuenteSonido, nota: Int, vel: Float = 0.8f) = agregar(f.voz(nota, vel, SR))

    fun notaOff(nota: Int) = synchronized(lock) { voces.filter { it.nota == nota }.forEach { it.soltar() } }

    /** Reproduce un audio una vez (golpe de batería, vista previa de una pista) */
    fun tocar(datos: FloatArray, vol: Float = 1f) = agregar(VozBuffer(datos, vol))

    fun detenerTodo() = synchronized(lock) { voces.clear() }

    fun apagar() {
        activo = false
        synchronized(lock) { (lock as java.lang.Object).notifyAll() }
        hilo?.join(500); hilo = null
    }
}
