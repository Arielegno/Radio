package com.estudio.musical.audio.synth

import com.estudio.musical.audio.dsp.Biquad
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Batería electrónica sintetizada (no necesita archivos de sonido). */
enum class Pieza(val nombre: String) {
    BOMBO("Bombo"), CAJA("Caja"), PALMAS("Palmas"), HH_CERRADO("Hi-hat"),
    HH_ABIERTO("Hi-hat abierto"), AROS("Aro"), TOM("Tom"), GUIRO("Güiro"),
    CENCERRO("Cencerro"), SHAKER("Shaker"), PLATILLO("Platillo")
}

object Bateria {
    private val cache = HashMap<Pair<Pieza, Int>, FloatArray>()

    fun sonido(p: Pieza, sr: Int): FloatArray = synchronized(cache) {
        cache.getOrPut(p to sr) { sintetizar(p, sr) }
    }

    private fun ruido(n: Int) = FloatArray(n) { Random.nextFloat() * 2 - 1 }

    private fun sintetizar(p: Pieza, sr: Int): FloatArray = when (p) {
        Pieza.BOMBO -> {
            val n = (sr * 0.5).toInt(); var fase = 0.0
            FloatArray(n) {
                val t = it.toDouble() / sr
                val f = 45 + 110 * exp(-t * 35)
                fase += f / sr
                val click = if (it < sr / 500) (Random.nextFloat() - 0.5f) * 0.4f else 0f
                (sin(2 * PI * fase) * exp(-t * 7)).toFloat() * 0.95f + click
            }
        }
        Pieza.CAJA -> {
            val n = (sr * 0.3).toInt(); val r = ruido(n)
            Biquad.highPass(sr, 1200.0).process(r)
            var fase = 0.0
            FloatArray(n) {
                val t = it.toDouble() / sr
                fase += 185.0 / sr
                (sin(2 * PI * fase) * exp(-t * 25) * 0.5 + r[it] * exp(-t * 16) * 0.6).toFloat()
            }
        }
        Pieza.PALMAS -> {
            val n = (sr * 0.3).toInt(); val r = ruido(n)
            Biquad.bandPass(sr, 1500.0, 1.2).process(r)
            FloatArray(n) {
                val t = it.toDouble() / sr
                // tres golpes rápidos + cola
                val env = listOf(0.0, 0.011, 0.022).sumOf { d -> if (t >= d) exp(-(t - d) * 180) else 0.0 } +
                    (if (t > 0.03) exp(-(t - 0.03) * 18) * 0.8 else 0.0)
                (r[it] * env * 1.4).toFloat()
            }
        }
        Pieza.HH_CERRADO, Pieza.HH_ABIERTO, Pieza.PLATILLO -> {
            val dur = when (p) { Pieza.HH_CERRADO -> 0.08; Pieza.HH_ABIERTO -> 0.4; else -> 1.6 }
            val n = (sr * dur).toInt(); val r = ruido(n)
            Biquad.highPass(sr, if (p == Pieza.PLATILLO) 5000.0 else 7000.0).process(r)
            val k = if (p == Pieza.HH_CERRADO) 60.0 else 4.0 / dur
            FloatArray(n) { (r[it] * exp(-it.toDouble() / sr * k) * 0.45).toFloat() }
        }
        Pieza.AROS -> {
            val n = (sr * 0.08).toInt(); val r = ruido(n)
            Biquad.bandPass(sr, 1800.0, 4.0).process(r)
            var fase = 0.0
            FloatArray(n) {
                val t = it.toDouble() / sr; fase += 820.0 / sr
                ((sin(2 * PI * fase) * 0.5 + r[it] * 2) * exp(-t * 70)).toFloat() * 0.7f
            }
        }
        Pieza.TOM -> {
            val n = (sr * 0.45).toInt(); var fase = 0.0
            FloatArray(n) {
                val t = it.toDouble() / sr
                fase += (110 + 60 * exp(-t * 20)) / sr
                (sin(2 * PI * fase) * exp(-t * 8) * 0.8).toFloat()
            }
        }
        Pieza.GUIRO -> {
            val n = (sr * 0.12).toInt(); val r = ruido(n)
            Biquad.bandPass(sr, 3500.0, 2.0).process(r)
            FloatArray(n) {
                val t = it.toDouble() / sr
                val raspado = 0.5 + 0.5 * sin(2 * PI * 90 * t)
                (r[it] * raspado * (1 - t / 0.12) * 1.2).toFloat()
            }
        }
        Pieza.CENCERRO -> {
            val n = (sr * 0.3).toInt(); var f1 = 0.0; var f2 = 0.0
            val out = FloatArray(n) {
                val t = it.toDouble() / sr
                f1 += 540.0 / sr; f2 += 800.0 / sr
                val s = (if (f1 % 1 < 0.5) 1.0 else -1.0) + (if (f2 % 1 < 0.5) 1.0 else -1.0)
                (s * exp(-t * 14) * 0.18).toFloat()
            }
            Biquad.bandPass(sr, 800.0, 1.5).process(out); out
        }
        Pieza.SHAKER -> {
            val n = (sr * 0.09).toInt(); val r = ruido(n)
            Biquad.highPass(sr, 5000.0).process(r)
            FloatArray(n) {
                val t = it.toDouble() / sr
                val env = if (t < 0.03) t / 0.03 else exp(-(t - 0.03) * 50)
                (r[it] * env * 0.5).toFloat()
            }
        }
    }
}
