package com.estudio.musical.audio.synth

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Una nota sonando. Las voces SUMAN su sonido al buffer. */
interface Voz {
    val nota: Int
    fun render(buf: FloatArray, off: Int, n: Int)
    fun soltar()
    val terminada: Boolean
}

/** Cualquier cosa que pueda tocar notas: un sintetizador o una muestra grabada. */
interface FuenteSonido {
    val nombre: String
    fun voz(nota: Int, velocidad: Float, sr: Int): Voz
}

fun frecuencia(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

/** Envolvente ADSR (ataque, caída, sostenido, liberación) en segundos */
class Adsr(sr: Int, ataque: Float, caida: Float, private val sostener: Float, liberar: Float) {
    private val aInc = 1f / (ataque * sr).coerceAtLeast(1f)
    private val dCoef = exp(-1.0 / (caida * sr).coerceAtLeast(1f)).toFloat()
    private val rCoef = exp(-1.0 / (liberar * sr).coerceAtLeast(1f)).toFloat()
    private var etapa = 0 // 0 ataque, 1 caída/sostenido, 2 liberación, 3 fin
    var valor = 0f
        private set

    fun siguiente(): Float {
        when (etapa) {
            0 -> { valor += aInc; if (valor >= 1f) { valor = 1f; etapa = 1 } }
            1 -> valor = sostener + (valor - sostener) * dCoef
            2 -> { valor *= rCoef; if (valor < 1e-4f) { valor = 0f; etapa = 3 } }
        }
        return valor
    }

    fun soltar() { if (etapa < 2) etapa = 2 }
    val terminada get() = etapa == 3
    /** Para sonidos percusivos sin sostenido: termina cuando la caída llega a casi cero */
    val silenciosa get() = etapa == 1 && sostener == 0f && valor < 1e-4f
}

enum class Onda { SENO, TRIANGULO, SIERRA, CUADRADA, RUIDO }

/** Oscilador con polyBLEP (reduce el "aliasing" o chirrido digital en sierra y cuadrada) */
class Oscilador(private val onda: Onda, private var inc: Double) {
    private var fase = Random.nextDouble()
    private var tri = 0.0

    fun setInc(i: Double) { inc = i }

    private fun blep(t: Double): Double = when {
        t < inc -> { val x = t / inc; x + x - x * x - 1 }
        t > 1 - inc -> { val x = (t - 1) / inc; x * x + x + x + 1 }
        else -> 0.0
    }

    fun siguiente(): Float {
        val v = when (onda) {
            Onda.SENO -> sin(2 * PI * fase)
            Onda.SIERRA -> 2 * fase - 1 - blep(fase)
            Onda.CUADRADA -> {
                var s = if (fase < 0.5) 1.0 else -1.0
                s += blep(fase); s -= blep((fase + 0.5) % 1.0); s
            }
            Onda.TRIANGULO -> {
                var s = if (fase < 0.5) 1.0 else -1.0
                s += blep(fase); s -= blep((fase + 0.5) % 1.0)
                tri = inc * 4 * s + (1 - inc * 4 * 0.05) * tri; tri
            }
            Onda.RUIDO -> Random.nextDouble() * 2 - 1
        }
        fase += inc
        if (fase >= 1.0) fase -= 1.0
        return v.toFloat()
    }
}

/** Filtro pasa-bajos de estado variable (suena "analógico") */
class FiltroSvf(private val sr: Int, resonancia: Float) {
    private var low = 0f; private var band = 0f
    private val q = (1f - resonancia.coerceIn(0f, 0.95f)) * 2f
    fun proceso(x: Float, corte: Float): Float {
        val fc = corte.coerceIn(20f, sr / 6.5f)
        val f = (2 * sin(PI * fc / sr)).toFloat()
        low += f * band
        val high = x - low - q * band
        band += f * high
        if (abs(low) > 50f) { low = 0f; band = 0f } // seguridad
        return low
    }
}

// ───────────────────────── Voces concretas ─────────────────────────

class VozSustractiva(override val nota: Int, vel: Float, sr: Int, private val i: Instrumento) : Voz {
    private val f = frecuencia(nota)
    private val o1 = Oscilador(i.onda1, f / sr)
    private val o2 = Oscilador(i.onda2, f * 2.0.pow(i.octava2 + i.desafinar / 1200.0) / sr)
    private val o3 = if (i.desafinar != 0f) Oscilador(i.onda1, f * 2.0.pow(-i.desafinar / 1200.0) / sr) else null
    private val env = Adsr(sr, i.ataque, i.caida, i.sostener, i.liberar)
    private val envF = Adsr(sr, i.ataque * 0.5f, i.caidaFiltro, 0f, i.liberar)
    private val filtro = FiltroSvf(sr, i.resonancia)
    private val amp = vel * i.volumen
    private val vibInc = 2 * PI * 5.5 / sr
    private var vibFase = 0.0
    private val sr0 = sr

    override fun render(buf: FloatArray, off: Int, n: Int) {
        for (k in 0 until n) {
            if (i.vibrato > 0f) {
                vibFase += vibInc
                val m = 2.0.pow(i.vibrato * sin(vibFase) / 1200.0)
                o1.setInc(f * m / sr0)
            }
            var s = o1.siguiente() * (1 - i.mezcla2) + o2.siguiente() * i.mezcla2
            if (o3 != null) s = (s + o3.siguiente() * (1 - i.mezcla2)) * 0.7f
            val e = env.siguiente()
            val corte = i.corte + i.envFiltro * envF.siguiente() + i.seguirTecla * f.toFloat()
            buf[off + k] += filtro.proceso(s, corte) * e * amp
        }
    }
    override fun soltar() { env.soltar(); envF.soltar() }
    override val terminada get() = env.terminada || env.silenciosa
}

/** Síntesis FM: pianos eléctricos, campanas, marimbas */
class VozFm(override val nota: Int, vel: Float, private val sr: Int, private val i: Instrumento) : Voz {
    private val f = frecuencia(nota)
    private var fc = 0.0; private var fm = 0.0
    private val incC = f / sr; private val incM = f * i.fmRatio / sr
    private val env = Adsr(sr, i.ataque, i.caida, i.sostener, i.liberar)
    private val idxDecay = exp(-1.0 / (i.fmCaida * sr)).toFloat()
    private var idx = i.fmIndice * (0.5f + vel * 0.5f)
    private val amp = vel * i.volumen

    override fun render(buf: FloatArray, off: Int, n: Int) {
        for (k in 0 until n) {
            val mod = sin(2 * PI * fm) * idx
            val s = sin(2 * PI * fc + mod)
            fc += incC; fm += incM
            if (fc > 1) fc -= 1.0
            if (fm > 1) fm -= 1.0
            idx *= idxDecay
            buf[off + k] += (s * env.siguiente() * amp).toFloat()
        }
    }
    override fun soltar() = env.soltar()
    override val terminada get() = env.terminada || env.silenciosa
}

/** Órgano aditivo con "registros" (armónicos) */
class VozOrgano(override val nota: Int, vel: Float, sr: Int, i: Instrumento) : Voz {
    private val armonicos = doubleArrayOf(0.5, 1.0, 2.0, 3.0, 4.0, 6.0)
    private val pesos = i.registros
    private val fases = DoubleArray(armonicos.size)
    private val incs = DoubleArray(armonicos.size) { frecuencia(nota) * armonicos[it] / sr }
    private val env = Adsr(sr, i.ataque, i.caida, i.sostener, i.liberar)
    private val amp = vel * i.volumen / pesos.sum().coerceAtLeast(1f)

    override fun render(buf: FloatArray, off: Int, n: Int) {
        for (k in 0 until n) {
            var s = 0.0
            for (h in armonicos.indices) {
                if (pesos[h] == 0f || incs[h] >= 0.5) continue
                s += sin(2 * PI * fases[h]) * pesos[h]
                fases[h] += incs[h]; if (fases[h] > 1) fases[h] -= 1.0
            }
            buf[off + k] += (s * env.siguiente() * amp).toFloat()
        }
    }
    override fun soltar() = env.soltar()
    override val terminada get() = env.terminada
}

/** Cuerda pulsada (Karplus-Strong): guitarra, bajo, arpa */
class VozCuerda(override val nota: Int, vel: Float, private val sr: Int, private val i: Instrumento) : Voz {
    private val largo = sr / frecuencia(nota)
    private val linea = FloatArray(largo.toInt() + 2)
    private var pos = 0
    private var prev = 0f
    private var soltada = false
    private var ganancia = 1f
    private val amp = vel * i.volumen
    private val brillo = i.corte.coerceIn(0.05f, 1f)
    private var silencio = 0

    init {
        // excitación: ruido filtrado según brillo
        var lp = 0f
        for (k in linea.indices) {
            val r = Random.nextFloat() * 2 - 1
            lp += (r - lp) * brillo
            linea[k] = lp
        }
    }

    override fun render(buf: FloatArray, off: Int, n: Int) {
        val len = linea.size - 1
        val frac = (largo - largo.toInt()).toFloat()
        val decay = i.sostener.coerceIn(0.9f, 0.9999f)
        for (k in 0 until n) {
            val a = linea[pos]
            val b = linea[(pos + 1) % len]
            val v = a + (b - a) * frac
            val nuevo = (v + prev) * 0.5f * decay
            prev = v
            linea[pos] = nuevo
            pos = (pos + 1) % len
            if (soltada) ganancia *= 0.9992f
            val out = v * amp * ganancia
            buf[off + k] += out
            silencio = if (abs(out) < 1e-4f) silencio + 1 else 0
        }
    }
    override fun soltar() { soltada = true }
    override val terminada get() = silencio > sr / 10 || ganancia < 1e-3f
}

/** Reproduce un buffer una vez (batería, clicks, efectos) */
class VozBuffer(private val datos: FloatArray, private val vol: Float, override val nota: Int = -1) : Voz {
    private var p = 0
    override fun render(buf: FloatArray, off: Int, n: Int) {
        val m = minOf(n, datos.size - p)
        for (k in 0 until m) buf[off + k] += datos[p + k] * vol
        p += m.coerceAtLeast(0)
    }
    override fun soltar() {}
    override val terminada get() = p >= datos.size
}
