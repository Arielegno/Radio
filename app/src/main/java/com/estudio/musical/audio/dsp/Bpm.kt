package com.estudio.musical.audio.dsp

import kotlin.math.max
import kotlin.math.sqrt

/** Detecta el tempo (BPM) a partir de los golpes de energía de la grabación. */
object Bpm {
    fun detect(x: FloatArray, sr: Int): Float {
        val hop = 512
        val frame = 1024
        val nFrames = (x.size - frame) / hop
        if (nFrames < 32) return 0f
        val win = Fft.hann(frame)
        val re = DoubleArray(frame); val im = DoubleArray(frame)
        var prev = DoubleArray(frame / 2)
        val flux = DoubleArray(nFrames)
        for (f in 0 until nFrames) {
            for (i in 0 until frame) { re[i] = x[f * hop + i] * win[i]; im[i] = 0.0 }
            Fft.transform(re, im)
            val mag = DoubleArray(frame / 2) { sqrt(re[it] * re[it] + im[it] * im[it]) }
            var s = 0.0
            for (k in mag.indices) s += max(0.0, mag[k] - prev[k]) // flujo espectral
            flux[f] = s
            prev = mag
        }
        // quitar la media
        val mean = flux.average()
        for (i in flux.indices) flux[i] = max(0.0, flux[i] - mean)

        val fps = sr.toDouble() / hop
        var best = 0.0; var bestBpm = 0.0
        var bpm = 60.0
        while (bpm <= 200.0) {
            val lag = fps * 60.0 / bpm
            val l = lag.toInt()
            val frac = lag - l
            var s = 0.0
            for (i in 0 until flux.size - l - 1) {
                s += flux[i] * (flux[i + l] * (1 - frac) + flux[i + l + 1] * frac)
            }
            // preferencia suave por tempos típicos (90–130)
            val weight = 1.0 / (1.0 + ((bpm - 110) / 60).let { it * it })
            s *= weight
            if (s > best) { best = s; bestBpm = bpm }
            bpm += 0.5
        }
        return bestBpm.toFloat()
    }
}
