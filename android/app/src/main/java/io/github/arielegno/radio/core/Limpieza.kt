package io.github.arielegno.radio.core

/**
 * Limpieza de nombres (igual que en la versión web): quita «VEVO», «- Topic», «(Video oficial)», «Lyrics»…
 * y el nombre del artista pegado al título. No usa internet.
 */
object Limpieza {
    private val RUIDO_PAREN = Regex("\\s*[(\\[][^)\\]]*\\b(official|oficial|videoclip|lyrics?|letra|audio|visuali[sz]er|hd|hq|4k|mv|clip)\\b[^)\\]]*[)\\]]", RegexOption.IGNORE_CASE)
    private val RUIDO_VIDEO = Regex("\\s*[(\\[]\\s*v[ií]deo\\s*[)\\]]", RegexOption.IGNORE_CASE)
    private val RUIDO_COLA = Regex("\\s*[-–|:]?\\s*\\b(official\\s+(music\\s+)?video|video\\s+oficial|videoclip(\\s+oficial)?|lyric\\s+video|official\\s+audio|audio\\s+oficial|video\\s+lyrics?)\\s*$", RegexOption.IGNORE_CASE)

    fun artista(raw: String?): String {
        val orig = (raw ?: "").trim()
        if (orig.isEmpty() || Regex("desconocid", RegexOption.IGNORE_CASE).containsMatchIn(orig)) return orig
        var canal = false
        var a = orig
        fun quita(re: Regex) { val n = a.replace(re, ""); if (n != a) { canal = true; a = n } }
        quita(Regex("\\s*[-–]\\s*topic$", RegexOption.IGNORE_CASE))
        quita(Regex("\\s*vevo$", RegexOption.IGNORE_CASE))
        quita(Regex("\\s+cat[aá]logo$", RegexOption.IGNORE_CASE))
        quita(Regex("\\s+(official|oficial)(\\s+(channel|canal|music|youtube|page))*$", RegexOption.IGNORE_CASE))
        a = a.trim()
        // «CarlosSadness» → «Carlos Sadness» (solo si venía de un canal y es una sola palabra pegada)
        if (canal && a.isNotEmpty() && !a.contains(Regex("\\s"))) a = a.replace(Regex("([a-záéíóúñü])([A-ZÁÉÍÓÚÑÜ])"), "$1 $2")
        return a.ifEmpty { orig }
    }

    fun titulo(titulo: String?, vararg artistas: String?): String {
        val orig = (titulo ?: "").trim(); if (orig.isEmpty()) return orig
        var t = orig.replace(RUIDO_PAREN, "").replace(RUIDO_VIDEO, "").replace(RUIDO_COLA, "").trim()
        val k = Regex("\\s[-–]\\s").find(t)?.range?.first ?: -1   // «Artista - Título»
        if (k > 0) {
            val pre = normNombre(t.substring(0, k))
            if (pre.isNotEmpty() && artistas.any { !it.isNullOrBlank() && normNombre(it) == pre }) t = t.substring(k).replace(Regex("^\\s[-–]\\s+"), "").trim()
        }
        val m = Regex("^(.*)\\s[-–]\\s([^-–]+)$").find(t)  // «Título - Artista»
        if (m != null) { val suf = normNombre(m.groupValues[2]); if (suf.isNotEmpty() && artistas.any { !it.isNullOrBlank() && normNombre(it) == suf }) t = m.groupValues[1].trim() }
        return t.ifEmpty { orig }
    }

    /** Primer artista de «A, B & C feat. D». */
    private val SEPARA_ARTISTAS = Regex("[,;&]|\\bfeat\\.?\\b|\\bft\\.?\\b", RegexOption.IGNORE_CASE)
    private val cachePrincipal = java.util.concurrent.ConcurrentHashMap<String, String>()
    /** El artista principal («Lana Del Rey feat. X» → «Lana Del Rey»). Se guarda lo ya calculado: se usa miles de veces. */
    fun artistaPrincipal(a: String?): String {
        val o = a ?: return ""
        cachePrincipal[o]?.let { return it }
        val r = o.split(SEPARA_ARTISTAS)[0].trim()
        if (cachePrincipal.size > 20_000) cachePrincipal.clear()
        cachePrincipal[o] = r
        return r
    }

    /** Datos encontrados en internet para una canción. */
    data class Datos(val album: String = "", val anio: Int = 0, val portada: String = "", val albumes: List<String> = emptyList())

    /** Completa lo que falta y corrige un álbum que no coincide con ninguno conocido (como la web). */
    fun aplicar(t: Track, d: Datos, forzar: Boolean = false): Track {
        var x = t; var cambio = false
        val conocidos = d.albumes.map(::normNombre).filter { it.isNotEmpty() }.toSet()
        val libre = { c: String -> forzar || c !in t.manual }
        if (d.album.isNotEmpty() && libre("album") && (t.album.isEmpty() || (conocidos.isNotEmpty() && normNombre(t.album) !in conocidos)) && t.album != d.album) {
            x = x.copy(album = d.album); cambio = true
            if (d.anio > 0 && libre("year")) x = x.copy(year = d.anio)
        }
        if (d.anio > 0 && x.year == 0 && libre("year") && (cambio || d.album.isEmpty() || normNombre(x.album) in conocidos)) x = x.copy(year = d.anio)
        return x
    }

    /** iTunes: la canción que coincide exactamente (título y artista). */
    fun deItunes(json: String, nombre: String, artista: String): Datos? {
        val nt = normNombre(nombre); val na = normNombre(artista)
        val t = Json.obj(json).list("results").mapNotNull { it.asObj() }
            .firstOrNull { normNombre(it.str("trackName")) == nt && normNombre(it.str("artistName")).contains(na) } ?: return null
        val anio = Regex("^(\\d{4})").find(t.str("releaseDate"))?.groupValues?.get(1)?.toInt() ?: 0
        return Datos(t.str("collectionName").replace(Regex("\\s+-\\s+(single|ep)$", RegexOption.IGNORE_CASE), ""), anio,
            t.str("artworkUrl100").replace(Regex("/\\d+x\\d+bb\\."), "/600x600bb."), listOf(t.str("collectionName")))
    }

    /** MusicBrainz: álbum y año (prefiere álbumes oficiales, sin recopilatorios, el más antiguo). */
    fun deMusicBrainz(json: String, nombre: String, artista: String): Datos? {
        val nt = normNombre(nombre); val na = normNombre(artista)
        val rec = Json.obj(json).list("recordings").mapNotNull { it.asObj() }.firstOrNull { r ->
            r.num("score") >= 85 && normNombre(r.str("title")) == nt &&
                r.list("artist-credit").mapNotNull { it.asObj() }.any { c -> normNombre(c.str("name").ifEmpty { c.map("artist").str("name") }) == na }
        } ?: return null
        val rels = rec.list("releases").mapNotNull { it.asObj() }.filter { it.str("title").isNotEmpty() }.sortedWith(
            compareBy<Map<String, Any?>>({ if (it.map("release-group").str("primary-type") == "Album") 0 else 1 },
                { it.map("release-group").list("secondary-types").size },
                { if (it.str("status") == "Official") 0 else 1 },
                { it.str("date").ifEmpty { "9999" } }))
        val rel = rels.firstOrNull()
        val anio = Regex("^(\\d{4})").find(rel?.str("date")?.ifEmpty { null } ?: rec.str("first-release-date"))?.groupValues?.get(1)?.toInt() ?: 0
        val portada = rel?.str("id")?.takeIf { it.isNotEmpty() }?.let { "https://coverartarchive.org/release/$it/front-500" } ?: ""
        return Datos(rel?.str("title") ?: "", anio, portada, rels.map { it.str("title") })
    }

    /** MusicBrainz: el nombre oficial del artista solo si es el mismo (cambia mayúsculas o tildes, nunca por otro). */
    fun nombreOficial(json: String, nombre: String): String? {
        val n = normNombre(nombre)
        return Json.obj(json).list("artists").mapNotNull { it.asObj() }.firstOrNull { it.num("score") >= 90 && normNombre(it.str("name")) == n }?.str("name")
    }
}
