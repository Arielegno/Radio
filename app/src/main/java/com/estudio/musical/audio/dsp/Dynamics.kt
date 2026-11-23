package com.estudio.musical.audio.dsp

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

object Dynamics {

    /**
     * Compresor: baja el volumen de las partes fuertes para que la voz suene pareja.
     * @param umbralDb a partir de dónde actúa (p. ej. -18 dB)
     * @param ratio cuánto comprime (4 = por cada 4 dB sobre el umbral deja pasar 1)
     */
    fun compresor(
        x: FloatArray, sr: Int, umbralDb: Float = -18f, ratio: Float = 4f,
        ataqueMs: Float = 10f, liberacionMs: Float = 120f, autoGanancia: Boolean = true
    ): FloatArray {
        val att = exp(-1.0 / (sr * ataqueMs / 1000.0))
        val rel = exp(-1.0 / (sr * liberacionMs / 1000.0))
        var env = 0.0
        val out = FloatArray(x.size)
        for (i in x.indices) {
            val lvl = abs(x[i]).toDouble()
            env = if (lvl > env) att * env + (1 - att) * lvl else rel * env + (1 - rel) * lvl
            val db = 20 * log10(max(env, 1e-9))
            val over = db - umbralDb
            val reduccion = if (over > 0) over - over / ratio else 0.0
            out[i] = (x[i] * 10.0.pow(-reduccion / 20)).toFloat()
        }
        return if (autoGanancia) normalizar(out, 0.9f) else out
    }

    /** Puerta de ruido: silencia lo que está por debajo del umbral (respiraciones, zumbido entre frases). */
    fun puertaRuido(x: FloatArray, sr: Int, umbralDb: Float = -45f): FloatArray {
        val umbral = 10f.pow(umbralDb / 20f)
        val att = exp(-1.0 / (sr * 0.002)); val rel = exp(-1.0 / (sr * 0.08))
        var env = 0.0; var g = 0.0
        val out = FloatArray(x.size)
        val holdSamples = sr / 20
        var hold = 0
        for (i in x.indices) {
            val l = abs(x[i]).toDouble()
            env = max(l, env * 0.9995)
            val target = if (env > umbral) { hold = holdSamples; 1.0 } else if (hold-- > 0) 1.0 else 0.0
            g = if (target > g) att * g + (1 - att) * target else rel * g + (1 - rel) * target
            out[i] = (x[i] * g).toFloat()
        }
        return out
    }

    fun normalizar(x: FloatArray, objetivo: Float = 0.95f): FloatArray {
        var peak = 0f
        for (s in x) if (abs(s) > peak) peak = abs(s)
        if (peak < 1e-6f) return x
        val g = objetivo / peak
        return FloatArray(x.size) { x[it] * g }
    }

    /** Limitador suave para evitar distorsión digital en mezclas */
    fun softClip(v: Float): Float = when {
        v > 1f -> 1f
        v < -1f -> -1f
        else -> 1.5f * v - 0.5f * v * v * v
    }
}
