package com.estudio.musical.audio.synth

import com.estudio.musical.audio.dsp.Pitch
import kotlin.math.abs
import kotlin.math.pow

/**
 * Una muestra: cualquier sonido que grabes (tu voz, un vaso, una guitarra, un silbido).
 * Se toca en cualquier nota cambiando su velocidad de reproducción.
 * @param notaRaiz la nota MIDI en la que suena el sonido original (se detecta sola).
 */
class Muestra(
    override val nombre: String,
    datos: FloatArray,
    val sr: Int,
    var notaRaiz: Int = 60,
    var bucle: Boolean = false,
    var liberar: Float = 0.15f
) : FuenteSonido {

    val datos: FloatArray = recortarSilencio(datos)

    override fun voz(nota: Int, velocidad: Float, sr: Int): Voz = VozMuestra(this, nota, velocidad, sr)

    companion object {
        /** Crea una muestra y detecta automáticamente su nota */
        fun desdeGrabacion(nombre: String, x: FloatArray, sr: Int): Muestra {
            val m = Muestra(nombre, x, sr)
            val raiz = Pitch.notaPrincipal(m.datos, sr)
            if (raiz > 0) m.notaRaiz = raiz
            return m
        }

        private fun recortarSilencio(x: FloatArray): FloatArray {
            val umbral = 0.01f
            var a = 0
            while (a < x.size && abs(x[a]) < umbral) a++
            var b = x.size
            while (b > a && abs(x[b - 1]) < umbral) b--
            if (b - a < 100) return x
            // normalizar
            var peak = 0f
            for (i in a until b) peak = maxOf(peak, abs(x[i]))
            val g = if (peak > 0) 0.9f / peak else 1f
            return FloatArray(b - a) { x[a + it] * g }
        }
    }
}

class VozMuestra(private val m: Muestra, override val nota: Int, vel: Float, sr: Int) : Voz {
    private val paso = 2.0.pow((nota - m.notaRaiz) / 12.0) * m.sr / sr
    private var pos = 0.0
    private val amp = vel * 0.8f
    private var soltada = false
    private var g = 1f
    private val rel = 1f - 1f / (m.liberar * sr).coerceAtLeast(1f)
    private val fadeIn = (sr * 0.003).toInt()
    private var n0 = 0
    // zona de bucle: segunda mitad del sonido
    private val loopA = m.datos.size * 0.4
    private val loopB = m.datos.size * 0.9

    override fun render(buf: FloatArray, off: Int, n: Int) {
        val d = m.datos
        for (k in 0 until n) {
            if (pos >= d.size - 1) {
                if (m.bucle && !soltada) pos = loopA else return
            }
            if (m.bucle && !soltada && pos >= loopB) pos = loopA + (pos - loopB)
            val i = pos.toInt()
            val f = (pos - i).toFloat()
            var s = d[i] + (d[i + 1] - d[i]) * f
            if (n0 < fadeIn) s *= n0++ / fadeIn.toFloat()
            if (soltada) g *= rel
            buf[off + k] += s * amp * g
            pos += paso
        }
    }
    override fun soltar() { soltada = true }
    override val terminada get() = pos >= m.datos.size - 1 || g < 1e-3f
}
