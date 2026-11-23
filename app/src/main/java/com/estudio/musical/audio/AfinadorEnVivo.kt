package com.estudio.musical.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.estudio.musical.audio.dsp.Pitch

/** Escucha el micrófono y entrega la frecuencia detectada ~20 veces por segundo. */
class AfinadorEnVivo {
    private val sr = MotorAudio.SR
    @Volatile private var activo = false
    private var hilo: Thread? = null
    private val main = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    fun iniciar(onFrecuencia: (Float) -> Unit) {
        if (activo) return
        activo = true
        hilo = Thread {
            val frame = 4096
            val min = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val rec = AudioRecord(MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxOf(min, frame * 2))
            val sh = ShortArray(frame / 2)
            val ventana = FloatArray(frame)
            rec.startRecording()
            var suavizada = -1f
            while (activo) {
                val n = rec.read(sh, 0, sh.size)
                if (n <= 0) continue
                System.arraycopy(ventana, n, ventana, 0, frame - n)
                for (i in 0 until n) ventana[frame - n + i] = sh[i] / 32768f
                val f = Pitch.yin(ventana, sr, minF = 50f, maxF = 1400f)
                suavizada = when {
                    f <= 0 -> -1f
                    suavizada <= 0 || kotlin.math.abs(f - suavizada) / suavizada > 0.03f -> f
                    else -> suavizada * 0.7f + f * 0.3f
                }
                val v = suavizada
                main.post { onFrecuencia(v) }
            }
            rec.stop(); rec.release()
        }.apply { start() }
    }

    fun detener() { activo = false; hilo?.join(500); hilo = null }
}
