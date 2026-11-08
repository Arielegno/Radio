package rr.revista

/** El texto de los artículos, igual que en la web (revFmt): párrafos, títulos, citas, listas, líneas de tiempo y videos. */
sealed class Bloque {
    data class Parrafo(val lineas: List<String>) : Bloque()
    data class Titulo(val texto: String, val grande: Boolean) : Bloque()
    data class Cita(val lineas: List<String>) : Bloque()
    data class Lista(val items: List<String>) : Bloque()
    data class Video(val id: String) : Bloque()
    data class LineaTiempo(val items: List<Pair<String, String>>) : Bloque()
}

/** Un trozo de texto con su formato (negrita, cursiva o enlace). */
data class Trozo(val texto: String, val negrita: Boolean = false, val cursiva: Boolean = false, val enlace: String = "")

object Formato {
    private val YT = Regex("^https?://(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|shorts/|embed/)|youtu\\.be/)([\\w-]{11})(?:[?&#]\\S*)?$")
    private val TL = Regex("^(\\d{4})\\s*[:–-]\\s+(.*)$")

    fun video(t: String): String? = YT.find(t.trim())?.groupValues?.get(1)

    fun bloques(texto: String?): List<Bloque> =
        (texto ?: "").replace("\r", "").split(Regex("\\n{2,}")).mapNotNull { b0 ->
            val b = b0.trim(); if (b.isEmpty()) return@mapNotNull null
            video(b)?.let { return@mapNotNull Bloque.Video(it) }
            val ls = b.split('\n')
            if (ls.size >= 2 && ls.all { TL.containsMatchIn(it) && Regex("^\\d{4}\\s*[:–-]\\s+\\S").containsMatchIn(it) })
                return@mapNotNull Bloque.LineaTiempo(ls.map { val m = TL.find(it)!!; m.groupValues[1] to m.groupValues[2] })
            when {
                b.startsWith("## ") -> Bloque.Titulo(b.substring(3), false)
                b.startsWith("# ") -> Bloque.Titulo(b.substring(2), true)
                ls.all { it.startsWith("> ") } -> Bloque.Cita(ls.map { it.substring(2) })
                ls.all { it.startsWith("- ") } -> Bloque.Lista(ls.map { it.substring(2) })
                else -> Bloque.Parrafo(ls)
            }
        }

    private val INLINE = Regex("\\*\\*([^*\\n]+)\\*\\*|\\*([^*\\n]+)\\*|(https?://[^\\s<]+)")

    /** **negrita** · *cursiva* · enlaces */
    fun trozos(linea: String): List<Trozo> {
        val r = ArrayList<Trozo>(); var i = 0
        for (m in INLINE.findAll(linea)) {
            if (m.range.first > i) r += Trozo(linea.substring(i, m.range.first))
            val (b, it, u) = m.destructured
            r += when { b.isNotEmpty() -> Trozo(b, negrita = true); it.isNotEmpty() -> Trozo(it, cursiva = true); else -> Trozo(u, enlace = u) }
            i = m.range.last + 1
        }
        if (i < linea.length) r += Trozo(linea.substring(i))
        return r
    }

    /** Minutos de lectura, como la web (200 palabras por minuto). */
    fun minutos(texto: String?): Int = maxOf(1, Math2.redondear((texto ?: "").split(Regex("\\s+")).size / 200.0))

    /** Texto plano para las tarjetas (sin marcas). */
    fun plano(texto: String?, max: Int = 160): String {
        val t = (texto ?: "").replace(Regex("\\*\\*|\\*"), "").replace(Regex("(?m)^(#{1,2}|>|-) "), "").replace(Regex("\\s+"), " ").trim()
        return if (t.length > max) t.take(max).trimEnd() + "…" else t
    }
}

object Math2 { fun redondear(x: Double): Int = kotlin.math.round(x).toInt() }

/** Fecha corta en español a partir de «2026-11-08T…». */
object Fechas {
    private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic")
    fun corta(iso: String): String {
        val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})").find(iso) ?: return ""
        val (a, mes, d) = m.destructured
        return "${d.toInt()} ${MESES.getOrElse(mes.toInt() - 1) { "" }} $a"
    }
    fun mesAnio(iso: String): String {
        val m = Regex("^(\\d{4})-(\\d{2})").find(iso) ?: return ""
        return MESES.getOrElse(m.groupValues[2].toInt() - 1) { "" }.uppercase() + " " + m.groupValues[1]
    }
}
