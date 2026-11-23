package com.estudio.musical.music

import com.estudio.musical.audio.dsp.Dynamics
import com.estudio.musical.audio.synth.Bateria
import com.estudio.musical.audio.synth.FuenteSonido
import com.estudio.musical.audio.synth.Pieza
import kotlin.random.Random

enum class PatronAcordes(val nombre: String) {
    SOSTENIDO("Sostenido"), NEGRAS("Golpes en cada tiempo"), ARPEGIO("Arpegio"),
    RASGUEO("Rasgueo"), CONTRATIEMPO("Contratiempo"), VALS("Vals (2 y 3)")
}

enum class PatronBajo(val nombre: String) {
    RAIZ("Raíz en 1 y 3"), RAIZ_QUINTA("Raíz y quinta"), OCTAVAS("Octavas en corcheas"),
    CAMINANTE("Caminante"), SIGUE_BOMBO("Sigue al bombo")
}

/** Patrón de batería: para cada pieza, una cadena de pasos ('x' = golpe fuerte, 'o' = suave, '.' = nada) */
typealias PatronBateria = MutableMap<Pieza, String>

object Patrones {
    fun bateria(e: Estilo): PatronBateria = when (e) {
        Estilo.POP -> mutableMapOf(
            Pieza.BOMBO to "x.......x.x.....", Pieza.CAJA to "....x.......x...", Pieza.HH_CERRADO to "x.o.x.o.x.o.x.o.")
        Estilo.BALADA, Estilo.ADORACION -> mutableMapOf(
            Pieza.BOMBO to "x.........x.....", Pieza.AROS to "....x.......x...", Pieza.HH_CERRADO to "x...o...x...o...")
        Estilo.ROCK -> mutableMapOf(
            Pieza.BOMBO to "x.....x.x.......", Pieza.CAJA to "....x.......x...", Pieza.HH_CERRADO to "x.x.x.x.x.x.x.x.",
            Pieza.PLATILLO to "x...............")
        Estilo.RANCHERA -> mutableMapOf(
            Pieza.BOMBO to "x...........", Pieza.AROS to "....x...x...", Pieza.HH_CERRADO to "x...x...x...")
        Estilo.CUMBIA -> mutableMapOf(
            Pieza.BOMBO to "x.......x.......", Pieza.GUIRO to "x.oox.oox.oox.oo", Pieza.AROS to "....x.......x...",
            Pieza.CENCERRO to "..x...x...x...x.")
        Estilo.BACHATA -> mutableMapOf(
            Pieza.BOMBO to "x.......x.......", Pieza.GUIRO to "x.oox.oox.oox.oo", Pieza.TOM to "......x.......x.",
            Pieza.AROS to "....x.......x...")
        Estilo.REGGAETON -> mutableMapOf(
            Pieza.BOMBO to "x...x...x...x...", Pieza.CAJA to "...x..x....x..x.", Pieza.HH_CERRADO to "x.o.x.o.x.o.x.o.")
        Estilo.BLUES -> mutableMapOf(
            Pieza.BOMBO to "x.......x.......", Pieza.CAJA to "....x.......x...", Pieza.HH_CERRADO to "x..ox..ox..ox..o")
        Estilo.JAZZ -> mutableMapOf(
            Pieza.BOMBO to "o.......o.......", Pieza.AROS to "....o.......o...", Pieza.HH_ABIERTO to "x...x..ox...x..o")
        Estilo.ELECTRONICA -> mutableMapOf(
            Pieza.BOMBO to "x...x...x...x...", Pieza.PALMAS to "....x.......x...", Pieza.HH_ABIERTO to "..x...x...x...x.",
            Pieza.SHAKER to "oxooxoxooxooxoxo")
    }

    fun acordes(e: Estilo) = when (e) {
        Estilo.POP, Estilo.ELECTRONICA -> PatronAcordes.NEGRAS
        Estilo.BALADA, Estilo.ADORACION, Estilo.BACHATA -> PatronAcordes.ARPEGIO
        Estilo.ROCK -> PatronAcordes.RASGUEO
        Estilo.RANCHERA -> PatronAcordes.VALS
        Estilo.CUMBIA -> PatronAcordes.CONTRATIEMPO
        Estilo.REGGAETON -> PatronAcordes.SOSTENIDO
        Estilo.BLUES, Estilo.JAZZ -> PatronAcordes.NEGRAS
    }

    fun bajo(e: Estilo) = when (e) {
        Estilo.RANCHERA, Estilo.CUMBIA -> PatronBajo.RAIZ_QUINTA
        Estilo.ROCK, Estilo.ELECTRONICA -> PatronBajo.OCTAVAS
        Estilo.BLUES, Estilo.JAZZ -> PatronBajo.CAMINANTE
        Estilo.REGGAETON -> PatronBajo.SIGUE_BOMBO
        else -> PatronBajo.RAIZ
    }
}

/**
 * Receta de una pista instrumental: con esto se genera batería, bajo, acordes y melodía
 * en la tonalidad elegida. El resultado son pistas separadas (stems) para el multipista.
 */
data class Arreglo(
    val tonica: Int,
    val mayor: Boolean,
    val bpm: Float,
    val estilo: Estilo,
    val progresion: List<Grado>,
    val compases: Int = 8,
    val acordes: FuenteSonido? = null,
    val patronAcordes: PatronAcordes = Patrones.acordes(estilo),
    val bajo: FuenteSonido? = null,
    val patronBajo: PatronBajo = Patrones.bajo(estilo),
    val bateria: PatronBateria? = Patrones.bateria(estilo),
    val melodia: FuenteSonido? = null,   // p. ej. tu muestra grabada tocando arpegios agudos
    val cuentaInicial: Boolean = false
)

/** Una nota en el tiempo (en muestras) */
class Nota(val inicio: Int, val dur: Int, val nota: Int, val vel: Float)

object GeneradorPistas {


    /** Devuelve las pistas generadas: nombre → audio mono */
    fun generar(a: Arreglo, sr: Int): LinkedHashMap<String, FloatArray> {
        val beat = sr * 60.0 / a.bpm
        val paso = beat / 4                     // semicorchea
        val pasosCompas = a.estilo.tiempos * 4
        val compasLen = beat * a.estilo.tiempos
        val total = (compasLen * a.compases + sr * 2.5).toInt()
        val prog = if (a.progresion.isEmpty()) Grado.parseLista("I") else a.progresion
        val acordes = (0 until a.compases).map { prog[it % prog.size].acorde(a.tonica, a.mayor) }
        val out = LinkedHashMap<String, FloatArray>()
        val rnd = Random(a.hashCode())
        fun humano(v: Float) = (v * (0.88f + rnd.nextFloat() * 0.12f)).coerceIn(0.05f, 1f)

        // ── Batería ──
        a.bateria?.takeIf { it.isNotEmpty() }?.let { patron ->
            val buf = FloatArray(total)
            for (c in 0 until a.compases) for ((pieza, pasos) in patron) {
                val sonido = Bateria.sonido(pieza, sr)
                for (s in 0 until minOf(pasosCompas, pasos.length)) {
                    val ch = pasos[s]
                    if (ch == '.') continue
                    // fills: último compás de cada 4 con caja extra
                    val vel = if (ch == 'x') 0.95f else 0.5f
                    mezclar(buf, sonido, (c * compasLen + s * paso).toInt(), humano(vel))
                }
                if (c % 4 == 3 && pieza == Pieza.CAJA && a.estilo != Estilo.RANCHERA) {
                    for (s in pasosCompas - 3 until pasosCompas)
                        mezclar(buf, Bateria.sonido(Pieza.CAJA, sr), (c * compasLen + s * paso).toInt(), humano(0.6f))
                }
            }
            out["Batería"] = Dynamics.normalizar(buf, 0.9f)
        }

        // ── Bajo ──
        a.bajo?.let { fuente ->
            val notas = mutableListOf<Nota>()
            acordes.forEachIndexed { c, ac ->
                val raiz = ac.bajo(36).let { if (it < 33) it + 12 else it }
                val quinta = raiz + 7
                val t0 = c * compasLen
                val sig = acordes.getOrNull(c + 1) ?: acordes[0]
                when (a.patronBajo) {
                    PatronBajo.RAIZ -> for (b in 0 until a.estilo.tiempos step 2)
                        notas += Nota((t0 + b * beat).toInt(), (beat * 1.9).toInt(), raiz, 0.9f)
                    PatronBajo.RAIZ_QUINTA -> for (b in 0 until a.estilo.tiempos step 2)
                        notas += Nota((t0 + b * beat).toInt(), (beat * 0.9).toInt(), if (b == 0) raiz else quinta - 12, 0.9f)
                    PatronBajo.OCTAVAS -> for (s in 0 until a.estilo.tiempos * 2)
                        notas += Nota((t0 + s * beat / 2).toInt(), (beat * 0.45).toInt(), if (s % 2 == 0) raiz else raiz + 12, 0.8f)
                    PatronBajo.CAMINANTE -> {
                        val tercera = raiz + ac.calidad.intervalos[1]
                        val aproximacion = sig.bajo(36).let { if (it < 33) it + 12 else it } - 1
                        listOf(raiz, tercera, quinta, aproximacion).take(a.estilo.tiempos).forEachIndexed { b, n ->
                            notas += Nota((t0 + b * beat).toInt(), (beat * 0.9).toInt(), n, 0.85f)
                        }
                    }
                    PatronBajo.SIGUE_BOMBO -> {
                        val kick = a.bateria?.get(Pieza.BOMBO) ?: "x...x...x...x..."
                        kick.forEachIndexed { s, ch ->
                            if (ch != '.' && s < pasosCompas) notas += Nota((t0 + s * paso).toInt(), (paso * 3.5).toInt(), raiz, 0.9f)
                        }
                    }
                }
            }
            out["Bajo"] = renderNotas(notas, fuente, total, sr, ::humano)
        }

        // ── Acordes ──
        a.acordes?.let { fuente ->
            val notas = mutableListOf<Nota>()
            acordes.forEachIndexed { c, ac ->
                val voces = ac.notas(60)
                val t0 = c * compasLen
                when (a.patronAcordes) {
                    PatronAcordes.SOSTENIDO -> voces.forEach { notas += Nota(t0.toInt(), (compasLen * 0.98).toInt(), it, 0.7f) }
                    PatronAcordes.NEGRAS -> for (b in 0 until a.estilo.tiempos)
                        voces.forEach { notas += Nota((t0 + b * beat).toInt(), (beat * 0.85).toInt(), it, if (b == 0) 0.75f else 0.6f) }
                    PatronAcordes.ARPEGIO -> {
                        val arp = voces + (voces[0] + 12) + voces[1].plus(12)
                        for (s in 0 until a.estilo.tiempos * 2)
                            notas += Nota((t0 + s * beat / 2).toInt(), (beat * 1.5).toInt(), arp[s % arp.size], 0.6f)
                    }
                    PatronAcordes.RASGUEO -> for (s in 0 until a.estilo.tiempos * 2) {
                        val abajo = s % 2 == 0
                        voces.forEachIndexed { k, n ->
                            val desfase = (if (abajo) k else voces.size - 1 - k) * sr / 120
                            notas += Nota((t0 + s * beat / 2).toInt() + desfase, (beat * 0.45).toInt(), n, if (abajo) 0.7f else 0.45f)
                        }
                    }
                    PatronAcordes.CONTRATIEMPO -> for (b in 0 until a.estilo.tiempos)
                        voces.forEach { notas += Nota((t0 + b * beat + beat / 2).toInt(), (beat * 0.35).toInt(), it, 0.6f) }
                    PatronAcordes.VALS -> for (b in 1 until a.estilo.tiempos)
                        voces.forEach { notas += Nota((t0 + b * beat).toInt(), (beat * 0.6).toInt(), it, 0.6f) }
                }
            }
            out["Acordes"] = renderNotas(notas, fuente, total, sr, ::humano)
        }

        // ── Melodía con la muestra o instrumento: arpegio agudo de notas del acorde ──
        a.melodia?.let { fuente ->
            val notas = mutableListOf<Nota>()
            val r = Random(7)
            acordes.forEachIndexed { c, ac ->
                val tonos = ac.notas(72)
                val esc = Teoria.escala(a.tonica, a.mayor)
                val t0 = c * compasLen
                for (b in 0 until a.estilo.tiempos) {
                    // tiempos fuertes: nota del acorde; débiles: a veces nota de paso de la escala
                    val n = if (b % 2 == 0 || r.nextFloat() < 0.5f) tonos[r.nextInt(tonos.size)]
                            else 72 + esc[r.nextInt(esc.size)]
                    notas += Nota((t0 + b * beat).toInt(), (beat * 0.9).toInt(), n, 0.7f)
                }
            }
            out["Melodía"] = renderNotas(notas, fuente, total, sr, ::humano)
        }
        return out
    }

    private fun mezclar(buf: FloatArray, sonido: FloatArray, inicio: Int, vol: Float) {
        val n = minOf(sonido.size, buf.size - inicio)
        for (i in 0 until n) buf[inicio + i] += sonido[i] * vol
    }

    /** Convierte una lista de notas en audio (usado también para guardar lo que tocas en el teclado) */
    fun renderizar(notas: List<Nota>, fuente: FuenteSonido, sr: Int): FloatArray {
        val total = (notas.maxOfOrNull { it.inicio + it.dur } ?: 0) + sr * 3
        return renderNotas(notas, fuente, total, sr) { it }
    }

    private fun renderNotas(notas: List<Nota>, fuente: FuenteSonido, total: Int, sr: Int, humano: (Float) -> Float): FloatArray {
        val buf = FloatArray(total)
        val bloque = 256
        for (n in notas) {
            if (n.inicio >= total) continue
            val v = fuente.voz(n.nota, humano(n.vel), sr)
            var p = n.inicio
            val fin = minOf(total, n.inicio + n.dur)
            while (p < fin && !v.terminada) {
                val k = minOf(bloque, fin - p); v.render(buf, p, k); p += k
            }
            v.soltar()
            val limite = minOf(total, p + sr * 3)
            while (p < limite && !v.terminada) {
                val k = minOf(bloque, limite - p); v.render(buf, p, k); p += k
            }
        }
        return Dynamics.normalizar(buf, 0.85f)
    }

    /** Mezcla rápida de todas las pistas generadas (para escuchar antes de guardar) */
    fun mezcla(pistas: Map<String, FloatArray>): FloatArray {
        val n = pistas.values.maxOfOrNull { it.size } ?: 0
        val vol = mapOf("Batería" to 0.8f, "Bajo" to 0.75f, "Acordes" to 0.55f, "Melodía" to 0.5f)
        val out = FloatArray(n)
        for ((k, p) in pistas) { val g = vol[k] ?: 0.6f; for (i in p.indices) out[i] += p[i] * g }
        for (i in out.indices) out[i] = Dynamics.softClip(out[i] * 0.8f)
        return out
    }
}
