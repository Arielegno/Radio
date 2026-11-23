package com.estudio.musical.music

/** Teoría musical: escalas, acordes, grados y progresiones. Kotlin puro. */
object Teoria {
    val NOTAS = listOf("Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
    val NOTAS_AMERICANAS = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    val MAYOR = listOf(0, 2, 4, 5, 7, 9, 11)
    val MENOR = listOf(0, 2, 3, 5, 7, 8, 10)

    fun escala(tonica: Int, mayor: Boolean) = (if (mayor) MAYOR else MENOR).map { (tonica + it) % 12 }

    fun nombreTono(tonica: Int, mayor: Boolean) = "${NOTAS[tonica]} ${if (mayor) "mayor" else "menor"}"

    /** Acordes diatónicos con su grado */
    fun acordes(tonica: Int, mayor: Boolean): List<String> {
        val romanos = if (mayor) listOf("I", "ii", "iii", "IV", "V", "vi", "vii°")
                      else listOf("i", "ii°", "III", "iv", "v", "VI", "VII")
        return romanos.map { r -> "$r=${Grado.parse(r)!!.acorde(tonica, mayor).nombre}" }
    }

    /** "La menor" → (9, false) */
    fun parseTono(t: String): Pair<Int, Boolean>? {
        val p = t.trim().split(" ")
        if (p.size < 2) return null
        val i = NOTAS.indexOf(p[0]); if (i < 0) return null
        return i to (p[1] == "mayor")
    }
}

enum class Calidad(val sufijo: String, val intervalos: List<Int>) {
    MAYOR("", listOf(0, 4, 7)),
    MENOR("m", listOf(0, 3, 7)),
    DISMINUIDO("dim", listOf(0, 3, 6)),
    AUMENTADO("aug", listOf(0, 4, 8)),
    SEPTIMA("7", listOf(0, 4, 7, 10)),
    MENOR7("m7", listOf(0, 3, 7, 10)),
    MAYOR7("maj7", listOf(0, 4, 7, 11)),
    SUS2("sus2", listOf(0, 2, 7)),
    SUS4("sus4", listOf(0, 5, 7))
}

data class Acorde(val raiz: Int, val calidad: Calidad) {
    val nombre get() = Teoria.NOTAS[raiz] + calidad.sufijo
    /** Notas MIDI del acorde cerca de [centro], en posición cerrada */
    fun notas(centro: Int = 60): List<Int> {
        var base = centro - 6 + ((raiz - (centro - 6)) % 12 + 12) % 12
        return calidad.intervalos.map { base + it }
    }
    fun bajo(octavaBase: Int = 36): Int = octavaBase + raiz

    companion object {
        private val REGEX = Regex("^(Do|Re|Mi|Fa|Sol|La|Si|[A-G])(#|b)?(maj7|m7|m|dim|aug|sus2|sus4|7)?$")

        /** Entiende "Lam", "Sol7", "F#m", "Bb", "Domaj7" */
        fun parse(s: String): Acorde? {
            val m = REGEX.matchEntire(s.trim()) ?: return null
            val (n, alt, cal) = m.destructured
            var raiz = Teoria.NOTAS.indexOf(n).takeIf { it >= 0 } ?: Teoria.NOTAS_AMERICANAS.indexOf(n)
            if (raiz < 0) return null
            if (alt == "#") raiz += 1
            if (alt == "b") raiz -= 1
            raiz = (raiz + 12) % 12
            val c = Calidad.entries.firstOrNull { it.sufijo == cal } ?: Calidad.MAYOR
            return Acorde(raiz, c)
        }
    }
}

/** Un grado en números romanos: I, ii, V7, bVII, vii°, IVmaj7 */
data class Grado(val grado: Int, val alteracion: Int, val menor: Boolean, val dim: Boolean, val extension: String) {
    fun acorde(tonica: Int, tonoMayor: Boolean): Acorde {
        val esc = if (tonoMayor) Teoria.MAYOR else Teoria.MENOR
        val raiz = (tonica + esc[grado] + alteracion + 12) % 12
        val calidad = when {
            dim -> Calidad.DISMINUIDO
            extension == "maj7" -> Calidad.MAYOR7
            extension == "7" && menor -> Calidad.MENOR7
            extension == "7" -> Calidad.SEPTIMA
            extension == "sus4" -> Calidad.SUS4
            extension == "sus2" -> Calidad.SUS2
            menor -> Calidad.MENOR
            else -> Calidad.MAYOR
        }
        return Acorde(raiz, calidad)
    }

    companion object {
        private val ROMANOS = listOf("I", "II", "III", "IV", "V", "VI", "VII")
        private val REGEX = Regex("^(b|#)?(VII|VI|IV|V|III|II|I|vii|vi|iv|v|iii|ii|i)(°|o)?(maj7|7|sus4|sus2)?$")

        fun parse(s: String): Grado? {
            val m = REGEX.matchEntire(s.trim()) ?: return null
            val (alt, rom, dim, ext) = m.destructured
            val idx = ROMANOS.indexOf(rom.uppercase())
            return Grado(idx, if (alt == "b") -1 else if (alt == "#") 1 else 0,
                rom == rom.lowercase(), dim.isNotEmpty(), ext)
        }

        /** "I V vi IV" → lista de grados (ignora lo que no entiende) */
        fun parseLista(s: String) = s.split(Regex("[\\s,–\\-|]+")).mapNotNull { parse(it) }
    }
}

/** Estilos musicales con su ritmo y progresiones típicas */
enum class Estilo(
    val nombre: String,
    val tiempos: Int,             // tiempos por compás (4 = 4/4, 3 = 3/4)
    val bpm: Int,
    val progMayor: List<String>,
    val progMenor: List<String>
) {
    POP("Pop", 4, 100, listOf("I V vi IV", "I vi IV V", "vi IV I V", "I IV vi V"), listOf("i VI III VII", "i iv VI V", "VI VII i i")),
    BALADA("Balada", 4, 72, listOf("I V vi iii IV I IV V", "I vi ii V", "I iii IV V"), listOf("i VI III VII", "i v VI III iv i iv V")),
    ROCK("Rock", 4, 120, listOf("I bVII IV I", "I IV V IV", "I V IV IV"), listOf("i VII VI VII", "i iv VII i")),
    RANCHERA("Ranchera (vals)", 3, 140, listOf("I I V7 V7 V7 V7 I I", "I IV V7 I"), listOf("i iv V7 i", "i i V7 V7")),
    CUMBIA("Cumbia", 4, 95, listOf("I V7 V7 I", "I IV V7 I"), listOf("i iv V7 i", "i VII VI V7")),
    BACHATA("Bachata", 4, 128, listOf("I V vi IV", "I IV V IV"), listOf("i iv V i", "i VII VI V")),
    REGGAETON("Reggaetón", 4, 92, listOf("vi IV I V", "I V vi IV"), listOf("i VI III VII", "i iv VI V")),
    BLUES("Blues", 4, 90, listOf("I7 I7 I7 I7 IV7 IV7 I7 I7 V7 IV7 I7 V7"), listOf("i7 i7 i7 i7 iv7 iv7 i7 i7 VI7 V7 i7 V7")),
    JAZZ("Jazz", 4, 110, listOf("ii7 V7 Imaj7 Imaj7", "Imaj7 vi7 ii7 V7"), listOf("iio V7 i7 i7")),
    ELECTRONICA("Electrónica", 4, 124, listOf("vi IV I V", "I I IV IV"), listOf("i VI III VII", "i i VI VII")),
    ADORACION("Adoración / Góspel", 4, 70, listOf("I V vi IV", "IV I V vi", "I IV I V"), listOf("VI VII i i"));

    fun progresiones(mayor: Boolean) = if (mayor) progMayor else progMenor
}
