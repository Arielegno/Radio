package com.estudio.musical.audio

import kotlin.math.abs
import kotlin.math.pow

/** Efectos de edición no destructivos: cada uno devuelve un audio nuevo. */
object Effects {

    /**
     * Eco con realimentación.
     * @param delayMs tiempo entre repeticiones (p. ej. 250–500 ms)
     * @param feedback cuánto vuelve cada repetición (0..0.9)
     * @param mix mezcla seco/húmedo (0 = original, 1 = solo eco)
     */
    fun echo(a: AudioData, delayMs: Int, feedback: Float, mix: Float): AudioData {
        val d = (a.sampleRate * delayMs / 1000).coerceAtLeast(1)
        // dejamos cola para que se oigan las últimas repeticiones
        val tailRepeats = if (feedback > 0.01f) (kotlin.math.ln(0.001) / kotlin.math.ln(feedback.toDouble())).toInt() else 1
        val out = FloatArray(a.samples.size + d * tailRepeats.coerceIn(1, 12))
        val wet = FloatArray(out.size)
        for (i in out.indices) {
            val dry = if (i < a.samples.size) a.samples[i] else 0f
            val prev = if (i >= d) wet[i - d] else 0f
            val delayedDry = if (i >= d && i - d < a.samples.size) a.samples[i - d] else 0f
            wet[i] = delayedDry + prev * feedback
            out[i] = dry + wet[i] * mix
        }
        return normalize(AudioData(out, a.sampleRate), 0.95f)
    }

    /** Recortar entre dos tiempos en segundos */
    fun trim(a: AudioData, startSec: Float, endSec: Float): AudioData {
        val s = (startSec * a.sampleRate).toInt().coerceIn(0, a.samples.size)
        val e = (endSec * a.sampleRate).toInt().coerceIn(s, a.samples.size)
        return AudioData(a.samples.copyOfRange(s, e), a.sampleRate)
    }

    /** Sube el volumen hasta que el pico llegue a [target] sin distorsionar */
    fun normalize(a: AudioData, target: Float = 0.95f): AudioData {
        val peak = a.samples.maxOfOrNull { abs(it) } ?: 0f
        if (peak < 1e-6f) return a
        val g = target / peak
        return AudioData(FloatArray(a.samples.size) { a.samples[it] * g }, a.sampleRate)
    }

    /** Ganancia en decibelios (+6 dB ≈ doble de amplitud) */
    fun gain(a: AudioData, db: Float): AudioData {
        val g = 10f.pow(db / 20f)
        return AudioData(FloatArray(a.samples.size) { (a.samples[it] * g).coerceIn(-1f, 1f) }, a.sampleRate)
    }

    fun fadeIn(a: AudioData, sec: Float): AudioData {
        val n = (sec * a.sampleRate).toInt().coerceAtMost(a.samples.size)
        val out = a.samples.copyOf()
        for (i in 0 until n) out[i] *= i / n.toFloat()
        return AudioData(out, a.sampleRate)
    }

    fun fadeOut(a: AudioData, sec: Float): AudioData {
        val n = (sec * a.sampleRate).toInt().coerceAtMost(a.samples.size)
        val out = a.samples.copyOf()
        val start = out.size - n
        for (i in 0 until n) out[start + i] *= 1f - i / n.toFloat()
        return AudioData(out, a.sampleRate)
    }

    /** Mezcla dos pistas (p. ej. voz + guitarra) con volumen para cada una */
    fun mix(a: AudioData, b: AudioData, volA: Float, volB: Float): AudioData {
        val n = maxOf(a.samples.size, b.samples.size)
        val out = FloatArray(n) {
            (if (it < a.samples.size) a.samples[it] * volA else 0f) +
                (if (it < b.samples.size) b.samples[it] * volB else 0f)
        }
        return normalize(AudioData(out, a.sampleRate))
    }
}
