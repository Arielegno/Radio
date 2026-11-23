package com.estudio.musical.audio.dsp

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Reducción de ruido por resta espectral.
 * Busca automáticamente el trozo más silencioso (≈0,4 s) como "huella" del ruido
 * (ventilador, zumbido, siseo) y lo resta de toda la grabación.
 * Consejo: deja 1 segundo de silencio al empezar a grabar.
 */
object NoiseReduction {
    fun process(x: FloatArray, sr: Int, intensidad: Float = 2.5f): FloatArray {
        val n = 2048
        val hop = n / 4
        val win = Fft.hann(n)
        if (x.size < n * 4) return x

        // 1) trozo más silencioso
        val seg = (sr * 0.4f).toInt()
        var bestStart = 0; var bestE = Double.MAX_VALUE
        var s = 0
        while (s + seg < x.size) {
            var e = 0.0
            for (i in s until s + seg step 4) e += x[i] * x[i]
            if (e < bestE) { bestE = e; bestStart = s }
            s += seg / 2
        }

        val re = DoubleArray(n); val im = DoubleArray(n)
        // 2) espectro promedio del ruido
        val noise = DoubleArray(n)
        var frames = 0
        var p = bestStart
        while (p + n <= bestStart + seg + n && p + n <= x.size) {
            for (i in 0 until n) { re[i] = x[p + i] * win[i]; im[i] = 0.0 }
            Fft.transform(re, im)
            for (k in 0 until n) noise[k] += sqrt(re[k] * re[k] + im[k] * im[k])
            frames++; p += hop
        }
        if (frames == 0) return x
        for (k in 0 until n) noise[k] = noise[k] / frames * intensidad

        // 3) resta espectral con superposición (overlap-add) y ganancia suavizada en el tiempo
        val out = DoubleArray(x.size + n)
        val gPrev = DoubleArray(n) { 1.0 }
        val norm = DoubleArray(x.size + n)
        p = 0
        while (p + n <= x.size) {
            for (i in 0 until n) { re[i] = x[p + i] * win[i]; im[i] = 0.0 }
            Fft.transform(re, im)
            for (k in 0 until n) {
                val mag = sqrt(re[k] * re[k] + im[k] * im[k])
                val ph = atan2(im[k], re[k])
                var g = if (mag > 1e-12) max(0.0, 1 - noise[k] / mag) else 0.0
                g = max(g, 0.05)                       // piso: evita el sonido "metálico"
                g = 0.6 * g + 0.4 * gPrev[k]           // suavizado entre bloques
                gPrev[k] = g
                re[k] = mag * g * cos(ph); im[k] = mag * g * sin(ph)
            }
            Fft.transform(re, im, inverse = true)
            for (i in 0 until n) {
                out[p + i] += re[i] * win[i]
                norm[p + i] += win[i] * win[i]
            }
            p += hop
        }
        return FloatArray(x.size) { if (norm[it] > 1e-6) (out[it] / norm[it]).toFloat() else x[it] }
    }
}
