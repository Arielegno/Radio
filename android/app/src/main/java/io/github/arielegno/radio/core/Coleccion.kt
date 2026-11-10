package io.github.arielegno.radio.core

/** Cómo se ve y se ordena la biblioteca (igual que en la versión web). */
object Coleccion {
    val VISTAS = listOf("artistas" to "Artistas", "albumes" to "Álbumes", "anios" to "Años", "generos" to "Géneros", "playlists" to "Playlists", "canciones" to "Canciones")

    val ORDENES = listOf(
        "original" to "Orden original", "nuevo" to "Año: más nuevo primero", "viejo" to "Año: más viejo primero",
        "titulo" to "Título (A–Z)", "artista" to "Artista (A–Z)", "album" to "Álbum (A–Z)",
        "escuchadas" to "Más escuchadas", "recientes" to "Agregadas recientemente"
    )

    val DECADAS = listOf("" to "Todas", "2020" to "2020s", "2010" to "2010s", "2000" to "2000s", "1990" to "90s", "1980" to "80s",
        "1970" to "70s", "1960" to "60s", "antes" to "Antes de 1960", "sin" to "Sin año")

    data class Filtros(val decada: String = "", val genero: String = "", val calidad: String = "", val favoritas: Boolean = false) {
        val activos: Int get() = listOf(decada.isNotEmpty(), genero.isNotEmpty(), calidad.isNotEmpty(), favoritas).count { it }
    }

    const val SIN_ARTISTA = "Artista desconocido"
    const val SIN_ALBUM = "Álbum desconocido"
    const val SIN_ANIO = "Año desconocido"
    const val OTROS_GENEROS = "Otros géneros"

    fun generoPrincipal(t: Track): String = t.genre.split(',', ';', '/').firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.replaceFirstChar { it.uppercase() } ?: OTROS_GENEROS

    /** Clave de grupo de una canción en cada vista. */
    // «Melophobia (Deluxe Edition)», «Melophobia [Remastered]» y «Melophobia» son el mismo álbum
    private val EDICION = Regex("\\s*[(\\[][^)\\]]*\\b(deluxe|remaster(ed)?|expanded|edition|edición|edicion|version|versión|anniversary|aniversario|bonus|explicit|special|super)\\b[^)\\]]*[)\\]]\\s*$|\\s+-\\s+(single|ep|deluxe( edition)?|remastered( \\d{4})?)\\s*$", RegexOption.IGNORE_CASE)
    private val nombresAlbum = java.util.concurrent.ConcurrentHashMap<String, String>()
    /** El nombre del álbum sin «Deluxe», «Remastered», «- Single»… (y escrito siempre igual, aunque cambien comas o mayúsculas). */
    fun albumBase(a: String): String {
        if (a.isBlank()) return ""
        var b = a.trim(); var n = 0
        while (n++ < 3) { val c = b.replace(EDICION, "").trim(); if (c == b || c.isEmpty()) break; b = c }
        val k = normNombre(b).ifEmpty { return b }
        return nombresAlbum.getOrPut(k) { b }
    }

    fun clave(t: Track, vista: String): String = when (vista) {
        "artistas" -> Limpieza.artistaPrincipal(t.artist).ifBlank { SIN_ARTISTA }   // «Lana Del Rey, A$AP Rocky» va con Lana Del Rey
        "albumes" -> albumBase(t.album).ifBlank { SIN_ALBUM }
        "generos" -> generoPrincipal(t)
        else -> if (t.year > 0) t.year.toString() else SIN_ANIO
    }

    fun pasa(t: Track, f: Filtros, esFav: (Track) -> Boolean): Boolean {
        if (f.favoritas && !esFav(t)) return false
        if (f.calidad == "hq" && !t.sinPerdida) return false
        if (f.calidad == "lossy" && t.sinPerdida) return false
        if (f.genero.isNotEmpty() && normNombre(generoPrincipal(t)) != normNombre(f.genero)) return false
        when (f.decada) {
            "" -> {}
            "sin" -> if (t.year > 0) return false
            "antes" -> if (t.year == 0 || t.year >= 1960) return false
            else -> { val d = f.decada.toIntOrNull() ?: return true; if (t.year < d || t.year >= d + 10) return false }
        }
        return true
    }

    fun buscar(t: Track, q: String): Boolean {
        val n = normNombre(q); if (n.isEmpty()) return true
        return normNombre(t.name).contains(n) || normNombre(t.artist).contains(n) || normNombre(t.album).contains(n)
    }

    fun ordenar(l: List<Track>, orden: String, escuchas: (Track) -> Int): List<Track> = when (orden) {
        "nuevo" -> l.sortedWith(compareByDescending<Track> { it.year }.thenBy { normNombre(it.artist) })
        "viejo" -> l.sortedWith(compareBy<Track> { if (it.year == 0) 9999 else it.year }.thenBy { normNombre(it.artist) })
        "titulo" -> l.sortedBy { normNombre(it.name) }
        "artista" -> l.sortedWith(compareBy<Track>({ normNombre(it.artist) }, { normNombre(it.album) }, { normNombre(it.name) }))
        "album" -> l.sortedWith(compareBy<Track>({ normNombre(it.album).ifEmpty { "~" } }, { normNombre(it.name) }))
        "escuchadas" -> l.sortedByDescending(escuchas)
        "recientes" -> l.sortedByDescending { it.addedAt }
        else -> l
    }

    data class Grupo(val clave: String, val canciones: List<Track>, val subtitulo: String = "") {
        // se calculan una sola vez por grupo (antes, en cada dibujo de cada tarjeta)
        val portada: Track? by lazy { canciones.firstOrNull { it.cover.isNotEmpty() } }
        /** El artista que más aparece (para álbumes). */
        val artistaPrincipal: String by lazy { canciones.groupingBy { it.artist }.eachCount().maxByOrNull { it.value }?.key ?: "" }
        val anio: Int by lazy { canciones.map { it.year }.filter { it > 0 }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 0 }
    }

    /** Arma los grupos de una vista; lo «desconocido» va al final. */
    fun agrupar(l: List<Track>, vista: String): List<Grupo> {
        val g = l.groupBy { clave(it, vista) + if (vista == "albumes") "\u0000" + normNombre(Limpieza.artistaPrincipal(it.artist)) else "" }
        val desconocido = { k: String -> k.startsWith(SIN_ARTISTA) || k.startsWith(SIN_ALBUM) || k.startsWith(SIN_ANIO) || k.startsWith(OTROS_GENEROS) }
        val lista = g.map { (k, v) -> val clave = k.substringBefore('\u0000'); Grupo(clave, v, if (vista == "albumes") Grupo(clave, v).artistaPrincipal else "") }
        return when (vista) {
            "anios" -> lista.sortedWith(compareBy<Grupo> { desconocido(it.clave) }.thenByDescending { it.clave })
            else -> lista.sortedWith(compareBy<Grupo> { desconocido(it.clave) }.thenBy { normNombre(it.clave) })
        }
    }

    /** Géneros presentes (para el filtro). */
    fun generos(l: List<Track>): List<String> = l.map(::generoPrincipal).filter { it != OTROS_GENEROS }.distinctBy(::normNombre).sortedBy(::normNombre)
}

/** Playlists: { nombre: { canciones: [id, …] } } — mismo formato que la versión web. */
class Playlists(texto: String?) {
    val datos: LinkedHashMap<String, MutableList<Long>> = linkedMapOf()

    init {
        Json.obj(texto).forEach { (k, v) ->
            if (k in setOf("__proto__", "constructor", "prototype")) return@forEach
            datos[k] = (v.asObj()?.list("canciones") ?: emptyList()).mapNotNull { (it as? Number)?.toLong() }.distinct().toMutableList()
        }
    }

    val nombres: List<String> get() = datos.keys.toList()
    fun canciones(n: String): List<Long> = datos[n] ?: emptyList()

    fun crear(n: String): String? {
        val t = n.trim().take(60)
        if (t.isEmpty() || t in setOf("__proto__", "constructor", "prototype")) return "Escribe un nombre."
        if (datos.keys.any { normNombre(it) == normNombre(t) }) return "Ya existe una playlist con ese nombre."
        datos[t] = mutableListOf(); return null
    }
    fun renombrar(viejo: String, nuevo: String): String? {
        val t = nuevo.trim().take(60); val l = datos[viejo] ?: return "Esa playlist ya no existe."
        if (t.isEmpty()) return "Escribe un nombre."
        if (t != viejo && datos.keys.any { it != viejo && normNombre(it) == normNombre(t) }) return "Ya existe una playlist con ese nombre."
        val copia = LinkedHashMap(datos); datos.clear()
        copia.forEach { (k, v) -> if (k == viejo) datos[t] = l else datos[k] = v }
        return null
    }
    fun borrar(n: String) { datos.remove(n) }
    /** true si se añadió; false si ya estaba. */
    fun agregar(n: String, id: Long): Boolean { val l = datos[n] ?: return false; if (id in l) return false; l += id; return true }
    fun quitar(n: String, id: Long) { datos[n]?.remove(id) }
    fun quitarDeTodas(ids: Set<Long>) { datos.values.forEach { it.removeAll(ids) } }
    fun mover(n: String, id: Long, delta: Int) {
        val l = datos[n] ?: return; val i = l.indexOf(id); val j = i + delta
        if (i < 0 || j < 0 || j >= l.size) return
        l[i] = l[j].also { l[j] = l[i] }
    }

    fun toJson(): String = Json.write(datos.mapValues { linkedMapOf("canciones" to it.value) })
}
