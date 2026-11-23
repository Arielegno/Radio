package com.estudio.musical.audio.dsp

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Detección de altura (frecuencia) con el algoritmo YIN. Se usa en el afinador y el sampler. */
object Pitch {
    val NOTAS = listOf("Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")

    /** Devuelve la frecuencia en Hz, o -1 si no hay una nota clara (silencio, ruido). */
    fun yin(x: FloatArray, sr: Int, minF: Float = 60f, maxF: Float = 1500f, threshold: Float = 0.15f): Float {
        val tauMin = (sr / maxF).toInt().coerceAtLeast(2)
        val tauMax = (sr / minF).toInt()
        val w = x.size - tauMax
        if (w < 64) return -1f
        // silencio
        var energy = 0.0
        for (i in 0 until w) energy += x[i] * x[i]
        if (sqrt(energy / w) < 0.005) return -1f

        val d = FloatArray(tauMax + 1)
        for (tau in 1..tauMax) {
            var s = 0f
            for (i in 0 until w) { val diff = x[i] - x[i + tau]; s += diff * diff }
            d[tau] = s
        }
        // diferencia normalizada acumulada
        val cmnd = FloatArray(tauMax + 1); cmnd[0] = 1f
        var running = 0f
        for (tau in 1..tauMax) {
            running += d[tau]
            cmnd[tau] = if (running == 0f) 1f else d[tau] * tau / running
        }
        var tau = tauMin
        var found = -1
        while (tau < tauMax) {
            if (cmnd[tau] < threshold) {
                while (tau + 1 < tauMax && cmnd[tau + 1] < cmnd[tau]) tau++
                found = tau; break
            }
            tau++
        }
        if (found < 0) return -1f
        // interpolación parabólica para más precisión
        val t = found
        val better = if (t in 1 until tauMax) {
            val s0 = cmnd[t - 1]; val s1 = cmnd[t]; val s2 = cmnd[t + 1]
            val den = 2 * (2 * s1 - s2 - s0)
            if (den != 0f) t + (s2 - s0) / den else t.toFloat()
        } else t.toFloat()
        return sr / better
    }

    fun midi(freq: Float): Float = 69f + 12f * (ln(freq / 440f) / ln(2f))
    fun freq(midi: Float): Float = 440f * 2f.pow((midi - 69f) / 12f)
    fun nombre(midi: Int): String = NOTAS[((midi % 12) + 12) % 12] + (midi / 12 - 1)

    /** Nota principal de un sonido grabado (mediana de las alturas detectadas). Devuelve MIDI o -1. */
    fun notaPrincipal(x: FloatArray, sr: Int): Int {
        val frame = 2048
        val hop = 1024
        val notas = mutableListOf<Float>()
        var p = 0
        while (p + frame <= x.size && notas.size < 200) {
            val f = yin(x.copyOfRange(p, p + frame), sr)
            if (f > 0) notas += midi(f)
            p += hop
        }
        if (notas.isEmpty()) return -1
        notas.sort()
        return notas[notas.size / 2].roundToInt()
    }
}
