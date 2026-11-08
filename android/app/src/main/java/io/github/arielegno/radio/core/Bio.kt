package io.github.arielegno.radio.core

/** Biografía corta de un artista desde Wikipedia en español (solo si es de música). */
object Bio {
    private val MUSICA = Regex("\\b(cantante|banda|grupo|m[uú]sic[oa]|rapero|rapera|cantautor|cantautora|compositor|compositora|dj|productor musical|orquesta|d[uú]o|trío|agrupaci[oó]n|solista|int[eé]rprete|guitarrista|baterista|bajista|pianista)\\b", RegexOption.IGNORE_CASE)

    data class Ficha(val titulo: String, val texto: String, val foto: String, val url: String)

    /** De la búsqueda de Wikipedia: el título cuyo nombre coincide con el artista. */
    fun elegir(busquedaJson: String, nombre: String): String? {
        val n = normNombre(nombre)
        val res = Json.obj(busquedaJson).map("query").list("search").mapNotNull { it.asObj() }
        return (res.firstOrNull { normNombre(it.str("title").replace(Regex("\\s*\\(.*\\)$"), "")) == n && MUSICA.containsMatchIn(it.str("snippet") + " " + it.str("title")) }
            ?: res.firstOrNull { normNombre(it.str("title").replace(Regex("\\s*\\(.*\\)$"), "")) == n })?.str("title")
    }

    /** Del resumen de Wikipedia: texto y foto, solo si habla de música. */
    fun ficha(resumenJson: String, nombre: String): Ficha? {
        val o = Json.obj(resumenJson)
        if (o.str("type") == "disambiguation") return null
        val texto = o.str("extract").trim(); val desc = o.str("description")
        if (texto.length < 40 || !MUSICA.containsMatchIn("$desc $texto")) return null
        if (normNombre(o.str("title").replace(Regex("\\s*\\(.*\\)$"), "")) != normNombre(nombre)) return null
        val foto = o.map("originalimage").str("source").ifEmpty { o.map("thumbnail").str("source") }
        return Ficha(o.str("title"), texto, if (foto.startsWith("https://upload.wikimedia.org/")) foto else "", o.map("content_urls").map("mobile").str("page"))
    }
}
