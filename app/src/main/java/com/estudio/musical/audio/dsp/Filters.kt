package com.estudio.musical.audio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Filtro biquad (fórmulas RBJ). Base del ecualizador. */
class Biquad private constructor(
    private val b0: Double, private val b1: Double, private val b2: Double,
    private val a1: Double, private val a2: Double
) {
    private var x1 = 0.0; private var x2 = 0.0; private var y1 = 0.0; private var y2 = 0.0

    fun process(x: Float): Float {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x.toDouble(); y2 = y1; y1 = y
        return y.toFloat()
    }

    fun process(buf: FloatArray) { for (i in buf.indices) buf[i] = process(buf[i]) }

    companion object {
        private fun make(b0: Double, b1: Double, b2: Double, a0: Double, a1: Double, a2: Double) =
            Biquad(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)

        fun lowPass(sr: Int, f: Double, q: Double = 0.707): Biquad {
            val w = 2 * PI * f / sr; val al = sin(w) / (2 * q); val c = cos(w)
            return make((1 - c) / 2, 1 - c, (1 - c) / 2, 1 + al, -2 * c, 1 - al)
        }
        fun highPass(sr: Int, f: Double, q: Double = 0.707): Biquad {
            val w = 2 * PI * f / sr; val al = sin(w) / (2 * q); val c = cos(w)
            return make((1 + c) / 2, -(1 + c), (1 + c) / 2, 1 + al, -2 * c, 1 - al)
        }
        fun bandPass(sr: Int, f: Double, q: Double = 1.0): Biquad {
            val w = 2 * PI * f / sr; val al = sin(w) / (2 * q); val c = cos(w)
            return make(al, 0.0, -al, 1 + al, -2 * c, 1 - al)
        }
        fun peaking(sr: Int, f: Double, db: Double, q: Double = 1.0): Biquad {
            val a = 10.0.pow(db / 40); val w = 2 * PI * f / sr; val al = sin(w) / (2 * q); val c = cos(w)
            return make(1 + al * a, -2 * c, 1 - al * a, 1 + al / a, -2 * c, 1 - al / a)
        }
        fun lowShelf(sr: Int, f: Double, db: Double): Biquad {
            val a = 10.0.pow(db / 40); val w = 2 * PI * f / sr; val c = cos(w)
            val al = sin(w) / 2 * sqrt(2.0); val sa = 2 * sqrt(a) * al
            return make(
                a * ((a + 1) - (a - 1) * c + sa), 2 * a * ((a - 1) - (a + 1) * c), a * ((a + 1) - (a - 1) * c - sa),
                (a + 1) + (a - 1) * c + sa, -2 * ((a - 1) + (a + 1) * c), (a + 1) + (a - 1) * c - sa
            )
        }
        fun highShelf(sr: Int, f: Double, db: Double): Biquad {
            val a = 10.0.pow(db / 40); val w = 2 * PI * f / sr; val c = cos(w)
            val al = sin(w) / 2 * sqrt(2.0); val sa = 2 * sqrt(a) * al
            return make(
                a * ((a + 1) + (a - 1) * c + sa), -2 * a * ((a - 1) + (a + 1) * c), a * ((a + 1) + (a - 1) * c - sa),
                (a + 1) - (a - 1) * c + sa, 2 * ((a - 1) - (a + 1) * c), (a + 1) - (a - 1) * c - sa
            )
        }
    }
}

object Eq {
    /** Ecualizador de 3 bandas: graves (120 Hz), medios (1 kHz), agudos (8 kHz), en dB. */
    fun tresBandas(x: FloatArray, sr: Int, graves: Float, medios: Float, agudos: Float): FloatArray {
        val out = x.copyOf()
        if (graves != 0f) Biquad.lowShelf(sr, 120.0, graves.toDouble()).process(out)
        if (medios != 0f) Biquad.peaking(sr, 1000.0, medios.toDouble(), 0.8).process(out)
        if (agudos != 0f) Biquad.highShelf(sr, 8000.0, agudos.toDouble()).process(out)
        return out
    }

    /** Quita retumbes y golpes de aire por debajo de [f] Hz */
    fun pasaAltos(x: FloatArray, sr: Int, f: Double = 80.0): FloatArray =
        x.copyOf().also { Biquad.highPass(sr, f).process(it) }
}
