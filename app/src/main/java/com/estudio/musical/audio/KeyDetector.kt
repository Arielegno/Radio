package com.estudio.musical.audio

import com.estudio.musical.audio.dsp.Fft
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Detecta la tonalidad de una grabación.
 * 1) FFT por bloques → energía de cada una de las 12 notas (cromagrama).
 * 2) Se compara con los perfiles de Krumhansl-Schmuckler de las 24 tonalidades.
 */
object KeyDetector {

    val NOTAS = listOf("Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")

    private val MAYOR = doubleArrayOf(6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88)
    private val MENOR = doubleArrayOf(6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17)

    data class Resultado(
        val tonalidad: String,
        val confianza: Double,
        val alternativa: String,
        val chroma: DoubleArray
    )

    fun detect(a: AudioData): Resultado {
        val n = 8192
        val chroma = DoubleArray(12)
        val window = DoubleArray(n) { 0.5 - 0.5 * cos(2 * PI * it / (n - 1)) }
        val re = DoubleArray(n)
        val im = DoubleArray(n)
        var pos = 0
        while (pos + n <= a.samples.size) {
            for (i in 0 until n) { re[i] = a.samples[pos + i] * window[i]; im[i] = 0.0 }
            Fft.transform(re, im)
            for (k in 1 until n / 2) {
                val f = k * a.sampleRate.toDouble() / n
                if (f < 60 || f > 2500) continue
                val mag = sqrt(re[k] * re[k] + im[k] * im[k])
                val midi = 69 + 12 * ln(f / 440.0) / ln(2.0)
                val pc = ((midi.roundToInt() % 12) + 12) % 12
                chroma[pc] += mag
            }
            pos += n / 2
        }

        val scores = mutableListOf<Pair<String, Double>>()
        for (tonica in 0 until 12) {
            scores += "${NOTAS[tonica]} mayor" to correlacion(chroma, MAYOR, tonica)
            scores += "${NOTAS[tonica]} menor" to correlacion(chroma, MENOR, tonica)
        }
        scores.sortByDescending { it.second }
        return Resultado(scores[0].first, scores[0].second, scores[1].first, chroma)
    }

    private fun correlacion(c: DoubleArray, perfil: DoubleArray, tonica: Int): Double {
        val p = DoubleArray(12) { perfil[(it - tonica + 12) % 12] }
        val mc = c.average(); val mp = p.average()
        var num = 0.0; var dc = 0.0; var dp = 0.0
        for (i in 0 until 12) {
            num += (c[i] - mc) * (p[i] - mp)
            dc += (c[i] - mc) * (c[i] - mc)
            dp += (p[i] - mp) * (p[i] - mp)
        }
        return if (dc == 0.0 || dp == 0.0) 0.0 else num / sqrt(dc * dp)
    }
}
