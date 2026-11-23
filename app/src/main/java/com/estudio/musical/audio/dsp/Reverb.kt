package com.estudio.musical.audio.dsp

/**
 * Reverb tipo "Freeverb": simula el sonido de una sala.
 * @param sala tamaño 0..1 (pequeña → catedral)
 * @param amortiguacion 0..1 (más alto = paredes más blandas, cola más oscura)
 * @param mezcla 0..1 cantidad de reverb
 */
object Reverb {
    private val COMBS = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
    private val ALLPASS = intArrayOf(556, 441, 341, 225)

    fun process(x: FloatArray, sr: Int, sala: Float = 0.6f, amortiguacion: Float = 0.4f, mezcla: Float = 0.3f): FloatArray {
        val scale = sr / 44100f
        val feedback = 0.7f + 0.28f * sala
        val tail = (sr * (1.5f + 3f * sala)).toInt()
        val n = x.size + tail
        val wet = FloatArray(n)

        for (len0 in COMBS) {
            val len = (len0 * scale).toInt()
            val buf = FloatArray(len)
            var idx = 0
            var store = 0f
            for (i in 0 until n) {
                val input = if (i < x.size) x[i] * 0.015f else 0f
                val out = buf[idx]
                store = out * (1 - amortiguacion) + store * amortiguacion
                buf[idx] = input + store * feedback
                wet[i] += out
                if (++idx >= len) idx = 0
            }
        }
        for (len0 in ALLPASS) {
            val len = (len0 * scale).toInt()
            val buf = FloatArray(len)
            var idx = 0
            for (i in 0 until n) {
                val bufOut = buf[idx]
                val input = wet[i]
                wet[i] = -input + bufOut
                buf[idx] = input + bufOut * 0.5f
                if (++idx >= len) idx = 0
            }
        }
        val out = FloatArray(n)
        for (i in 0 until n) {
            val dry = if (i < x.size) x[i] else 0f
            out[i] = dry * (1 - mezcla * 0.5f) + wet[i] * mezcla * 3f
        }
        // recortar silencio al final de la cola
        var end = n
        while (end > x.size && kotlin.math.abs(out[end - 1]) < 1e-4f) end--
        return Dynamics.normalizar(out.copyOf(end), 0.95f)
    }
}
