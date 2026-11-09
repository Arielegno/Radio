package io.github.arielegno.radio

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.arielegno.radio.core.Bio
import io.github.arielegno.radio.core.Json
import io.github.arielegno.radio.core.Limpieza
import io.github.arielegno.radio.core.Track
import io.github.arielegno.radio.core.normNombre
import io.github.arielegno.radio.core.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * La guía: limpia nombres y completa álbum, año y carátula con MusicBrainz e iTunes (gratis, sin claves).
 * Lo que corregiste a mano no lo toca, salvo que tú pidas «Completar».
 */
object Guia {
    var trabajando by mutableStateOf(false); private set
    var progreso by mutableStateOf(""); private set
    private var cancelar = false
    private val turnoMb = Mutex()
    private var ultimoMb = 0L
    private const val AGENTE = "RadioDeRuta/1.2 ( https://arielegno.github.io/Radio/ )"

    private val ctx: Context get() = RadioApp.app
    private val bib get() = RadioApp.biblioteca

    fun detener() { cancelar = true }

    private fun soloWifiBloquea(): Boolean {
        if (!RadioApp.prefs.bool("soloWifi", false)) return false
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return true
        return !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun lq(s: String) = s.replace("\"", " ").replace("\\", " ")

    /** MusicBrainz pide una consulta por segundo. */
    private suspend fun mb(url: String): String? = turnoMb.withLock {
        val espera = 1100 - (System.currentTimeMillis() - ultimoMb)
        if (espera > 0) delay(espera)
        ultimoMb = System.currentTimeMillis()
        try { val (c, t) = Red.crudo("GET", url, headers = mapOf("User-Agent" to AGENTE), timeoutMs = 10_000); if (c == 200) t else null } catch (e: Exception) { null }
    }

    private suspend fun nombreOficial(artista: String): String {
        if (artista.isBlank() || Regex("desconocid|[,;&/]|\\b(feat|ft|x)\\b", RegexOption.IGNORE_CASE).containsMatchIn(artista)) return artista
        val ck = "artof_" + normNombre(artista)
        RadioApp.prefs.str(ck, "").takeIf { it.isNotEmpty() }?.let { return it }
        val j = mb("https://musicbrainz.org/ws/2/artist/?query=" + enc("artist:\"${lq(artista)}\"") + "&fmt=json&limit=5") ?: return artista
        val of = Limpieza.nombreOficial(j, artista) ?: artista
        RadioApp.prefs.set(ck, of); return of
    }

    /** Álbum, año y portada: MusicBrainz y, si no basta, iTunes. null = sin conexión. */
    private suspend fun datos(t: Track): Limpieza.Datos? {
        if (!Red.hayInternet(ctx)) return null
        val m = mb("https://musicbrainz.org/ws/2/recording/?query=" + enc("recording:\"${lq(t.name)}\" AND artist:\"${lq(t.artist)}\"") + "&fmt=json&limit=5")
            ?.let { Limpieza.deMusicBrainz(it, t.name, t.artist) }
        val it = if (m != null && m.album.isNotEmpty()) null else try {
            val (c, j) = Red.crudo("GET", "https://itunes.apple.com/search?media=music&entity=song&limit=8&term=" + enc(t.artist + " " + t.name), timeoutMs = 8000)
            if (c == 200) Limpieza.deItunes(j, t.name, t.artist) else null
        } catch (e: Exception) { null }
        val base = if (m != null && m.album.isNotEmpty()) m else it ?: m ?: return Limpieza.Datos()
        val portada = it?.portada?.ifEmpty { null } ?: m?.portada ?: ""
        return base.copy(portada = portada, albumes = (m?.albumes ?: emptyList()) + listOfNotNull(it?.album))
    }

    private suspend fun bajarImagen(url: String): ByteArray? = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://") || soloWifiBloquea()) return@withContext null
        try {
            var u = URL(url); var n = 0
            while (n++ < 4) {
                val c = u.openConnection() as HttpURLConnection
                c.instanceFollowRedirects = false; c.connectTimeout = 10_000; c.readTimeout = 10_000; c.setRequestProperty("User-Agent", AGENTE)
                try {
                    val code = c.responseCode
                    if (code in 300..399) { val loc = c.getHeaderField("Location") ?: return@withContext null; u = URL(u, loc); if (u.protocol != "https") return@withContext null; continue }
                    if (code != 200 || c.contentType?.startsWith("image/") != true) return@withContext null
                    return@withContext c.inputStream.use { it.readBytes() }.takeIf { it.size in 1500..12_000_000 }
                } finally { c.disconnect() }
            }
            null
        } catch (e: Exception) { null }
    }

    /** El artista no sirve para buscar: vacío, «desconocido» o en realidad es un título («… letra», «video oficial»). */
    private fun artistaDudoso(t: Track): Boolean {
        val a = t.artist.trim()
        return a.isEmpty() || Regex("desconocid|\\b(letra|lyrics?|video|oficial|official|audio|hd|remix)\\b", RegexOption.IGNORE_CASE).containsMatchIn(a) || normNombre(a) == normNombre(t.name)
    }

    /** El «álbum» es en realidad el nombre del archivo o un video («Jarabe De Palo - La Flaca», «… (Video Lyric, Letra)»). */
    private fun albumBasura(t: Track): Boolean {
        val a = t.album.trim(); if (a.isEmpty()) return false
        if (Regex("\\b(video|lyrics?|letra|official|oficial|audio|visualizer|hd|mv|\\d{3,4}p)\\b", RegexOption.IGNORE_CASE).containsMatchIn(a)) return true
        if (Regex("^[0-9a-fA-F]{16,}$").matches(a)) return true   // nombre de archivo tipo «5b9dbdf326974…»
        val na = normNombre(a); val nt = normNombre(t.name); val nr = normNombre(Limpieza.artistaPrincipal(t.artist))
        return nt.isNotEmpty() && nr.isNotEmpty() && na.contains(nt) && na.contains(nr) && na != nt
    }

    /** iTunes por título (y artista si sirve): saca artista, título, álbum, año y portada aunque el archivo venga sin datos. */
    private suspend fun identificar(t: Track): Map<String, Any?>? {
        val dudoso = artistaDudoso(t)
        val nt = normNombre(t.name.replace(Regex("\\(.*?\\)|\\[.*?]"), " "))
        if (nt.length < 2) return null
        val term = (if (dudoso) "" else Limpieza.artistaPrincipal(t.artist) + " ") + t.name.replace(Regex("\\b(letra|lyrics?|video oficial|official video|audio)\\b", RegexOption.IGNORE_CASE), " ")
        val (c, j) = try { Red.crudo("GET", "https://itunes.apple.com/search?media=music&entity=song&limit=15&term=" + enc(term.trim()), timeoutMs = 8000) } catch (e: Exception) { return null }
        if (c != 200) return null
        val na = normNombre(Limpieza.artistaPrincipal(t.artist))
        return Json.obj(j).let { (it["results"] as? List<*>).orEmpty() }.mapNotNull { it as? Map<String, Any?> }.firstOrNull { r ->
            val rt = normNombre(r.str("trackName").replace(Regex("\\(.*?\\)|\\[.*?]"), " ")); val ra = normNombre(r.str("artistName"))
            rt.isNotEmpty() && (rt == nt || rt.startsWith(nt) || nt.startsWith(rt)) && (dudoso || na.isEmpty() || ra.contains(na) || na.contains(ra))
        }
    }

    /** Último recurso: la IA de la persona (Gemini o Claude) adivina los datos desde el nombre del archivo. */
    private var usosIA = 0
    private suspend fun preguntarIA(t: Track): Map<String, Any?>? {
        if (usosIA >= 60 || !FichasRed.hayIA()) return null
        usosIA++
        val p = RadioApp.prefs
        val pedido = "Identifica esta canción a partir de lo que dice el archivo. Responde SOLO un JSON {\"titulo\":\"\",\"artista\":\"\",\"album\":\"\",\"anio\":0}. " +
            "Si no estás seguro de un dato, déjalo vacío o en 0. Título: «${t.name}». Artista: «${t.artist}». Álbum: «${t.album}»."
        return try {
            val texto = if (p.str("proveedor", "gemini") == "claude") {
                val (c, r) = Red.crudo("POST", "https://api.anthropic.com/v1/messages", io.github.arielegno.radio.core.Luna.peticionClaude(p.str("claudeModel", "claude-sonnet-5-5").ifBlank { "claude-sonnet-5-5" }, "Eres un experto en música.", pedido),
                    mapOf("x-api-key" to p.str("claudeKey", "").trim(), "anthropic-version" to "2023-06-01"), timeoutMs = 30_000)
                if (c in 200..299) io.github.arielegno.radio.core.Luna.crudoClaude(r) else null
            } else {
                val modelo = p.str("geminiModel", "").trim().ifEmpty { p.str("geminiModelSel", "gemini-3.5-flash-lite") }.filter { it.isLetterOrDigit() || it == '-' || it == '.' }
                val (c, r) = Red.crudo("POST", "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent", io.github.arielegno.radio.core.Luna.peticionGemini("Eres un experto en música.", pedido),
                    mapOf("x-goog-api-key" to p.str("geminiKey", "").trim()), timeoutMs = 30_000)
                if (c in 200..299) io.github.arielegno.radio.core.Luna.crudoGemini(r) else null
            }
            io.github.arielegno.radio.core.Fichas.jsonDeTexto(texto)
        } catch (e: Exception) { null }
    }

    /** Limpia y completa una canción. forzar = «Completar» (también cambia lo editado a mano). */
    private suspend fun revisar(t0: Track, forzar: Boolean): Track? {
        var t = t0
        val libre = { c: String -> forzar || c !in t0.manual }
        var artista = if (libre("artist")) Limpieza.artista(t.artist) else t.artist
        if (libre("artist") && Red.hayInternet(ctx)) artista = nombreOficial(artista)
        val nombre = if (libre("name")) Limpieza.titulo(t.name, artista, t0.artist) else t.name
        t = t.copy(name = nombre, artist = artista)
        if (libre("album") && albumBasura(t)) t = t.copy(album = "")
        if (!Red.hayInternet(ctx)) return if (t != t0) t else null
        // Para buscar se usa el artista principal («Lana Del Rey, A$AP Rocky» → Lana Del Rey)
        val busca = t.copy(artist = Limpieza.artistaPrincipal(t.artist).ifBlank { t.artist })
        val d = (if (artistaDudoso(t)) null else datos(busca)) ?: Limpieza.Datos()
        t = Limpieza.aplicar(t, d, forzar)
        var portada = d.portada
        // Si faltan artista, álbum o año: iTunes por título y, al final, la IA
        if (artistaDudoso(t) || t.album.isEmpty() || t.year == 0) {
            identificar(t)?.let { r ->
                if (artistaDudoso(t) && libre("artist")) t = t.copy(artist = r.str("artistName").take(200))
                if (t.album.isEmpty() && libre("album")) t = t.copy(album = r.str("collectionName").replace(Regex("\\s+-\\s+(single|ep)$", RegexOption.IGNORE_CASE), "").take(200))
                if (t.year == 0 && libre("year")) t = t.copy(year = Regex("^(\\d{4})").find(r.str("releaseDate"))?.groupValues?.get(1)?.toInt() ?: 0)
                if (portada.isEmpty()) portada = r.str("artworkUrl100").replace(Regex("/\\d+x\\d+bb\\."), "/600x600bb.")
            }
        }
        if (artistaDudoso(t) || t.album.isEmpty()) preguntarIA(t)?.let { r ->
            if (artistaDudoso(t) && libre("artist") && r.str("artista").isNotBlank()) t = t.copy(artist = r.str("artista").take(200))
            if (libre("name") && r.str("titulo").isNotBlank() && normNombre(r.str("titulo")).let { it.isNotEmpty() && normNombre(t.name).contains(it) }) t = t.copy(name = r.str("titulo").take(200))
            if (t.album.isEmpty() && libre("album") && r.str("album").isNotBlank()) t = t.copy(album = r.str("album").take(200))
            val y = (r["anio"] as? Number)?.toInt() ?: r.str("anio").toIntOrNull() ?: 0
            if (t.year == 0 && libre("year") && y in 1900..2100) t = t.copy(year = y)
        }
        val d2 = d.copy(portada = portada)
        if ((t.cover.isEmpty() || (forzar && "cover" !in t0.manual)) && t0.cover.isEmpty() && d2.portada.isNotEmpty() && libre("cover")) {
            bajarImagen(d2.portada)?.let { b -> bib.guardarCaratulaDe(b, t.id).takeIf { it.isNotEmpty() }?.let { t = t.copy(cover = it) } }
        }
        t = t.copy(revisada = true, manual = if (forzar) emptySet() else t0.manual)
        return if (t != t0) t else null
    }

    /** Botón «Completar» de una canción. Devuelve un mensaje para mostrar. */
    suspend fun completar(id: Long): String {
        val t = bib.porId(id) ?: return "Esa canción ya no está."
        if (!Red.hayInternet(ctx)) return "No hay conexión para consultar la guía."
        progreso = "Buscando información de «${t.name}»…"
        val n = revisar(t, true)
        progreso = ""
        if (n == null) return "«${t.name}» ya estaba completa."
        bib.actualizar(listOf(n))
        val cambios = listOf(n.name != t.name, n.artist != t.artist, n.album != t.album, n.year != t.year, n.cover != t.cover).count { it }
        return if (cambios > 0) "Listo: la guía completó $cambios dato(s) de «${n.name}»." else "«${n.name}» ya estaba completa."
    }

    /** «Limpiar y completar» toda la biblioteca. forzar = vuelve a revisar todo. */
    suspend fun mantener(forzar: Boolean): String {
        if (trabajando) return "La guía ya está trabajando."
        trabajando = true; cancelar = false
        var nombres = 0; var datos = 0
        try {
            // 1) Limpieza de nombres (sin internet)
            val todas = bib.pistas
            val limpias = todas.mapNotNull { t ->
                val a = if ("artist" in t.manual) t.artist else Limpieza.artista(t.artist)
                val n = if ("name" in t.manual) t.name else Limpieza.titulo(t.name, a, t.artist)
                if (a != t.artist || n != t.name) t.copy(name = n, artist = a) else null
            }
            if (limpias.isNotEmpty()) { bib.actualizar(limpias); nombres = limpias.size }
            if (!Red.hayInternet(ctx)) return if (nombres > 0) "Limpié $nombres nombre(s). Sin internet: los demás datos se completan después." else "Sin internet: la guía completará los datos después."
            // 2) Álbum, año y carátula (primero las más incompletas)
            val falta = { t: Track -> (if (t.album.isEmpty()) 1 else 0) + (if (t.year == 0) 1 else 0) + (if (t.cover.isEmpty()) 1 else 0) }
            val reintentar = System.currentTimeMillis() - (RadioApp.prefs.str("guiaReintento", "0").toLongOrNull() ?: 0L) > 3 * 86_400_000L
            usosIA = 0
            val hechas = if (forzar) Json.arr(RadioApp.prefs.str("guiaRonda", "[]")).mapNotNull { (it as? Number)?.toLong() }.toMutableSet() else mutableSetOf()
            val pendientes = bib.pistas.filter { if (forzar) it.id !in hechas else (!it.revisada || (reintentar && falta(it) > 0)) }.sortedByDescending(falta)
            if (reintentar) RadioApp.prefs.set("guiaReintento", System.currentTimeMillis().toString())
            val lote = mutableListOf<Track>()
            pendientes.forEachIndexed { i, t ->
                if (cancelar) return@forEachIndexed
                progreso = "Completando datos (${i + 1} de ${pendientes.size})" + if (forzar && hechas.isNotEmpty()) " · ${hechas.size} ya listas" else ""
                val actual = bib.porId(t.id) ?: return@forEachIndexed
                val n = revisar(actual, false)
                if (n != null) { lote += n; if (n.album != actual.album || n.year != actual.year || n.cover != actual.cover) datos++ }
                else lote += actual.copy(revisada = true)
                if (forzar) hechas += t.id
                if (lote.size >= 8) { bib.actualizar(lote.toList()); lote.clear(); if (forzar) RadioApp.prefs.set("guiaRonda", Json.write(hechas.toList())) }
            }
            bib.actualizar(lote)
            // Vuelta terminada: la próxima empieza de cero; si se canceló, sigue donde quedó
            if (forzar) RadioApp.prefs.set("guiaRonda", if (cancelar) Json.write(hechas.toList()) else "[]")
            return when {
                cancelar -> "Guía detenida: lo que ya se avanzó se conservó."
                nombres + datos > 0 -> "Biblioteca actualizada: $nombres nombre(s) limpiados y $datos canción(es) completadas."
                else -> "Tu biblioteca ya estaba limpia y completa."
            }
        } finally { trabajando = false; progreso = "" }
    }

    /** Biografía corta del artista (Wikipedia en español), guardada para verla sin internet. */
    suspend fun bio(artista: String): Bio.Ficha? {
        val ck = "bio_" + normNombre(artista)
        Json.obj(RadioApp.prefs.str(ck, "")).let { o -> if (o.isNotEmpty()) return if (o.str("t").isEmpty()) null else Bio.Ficha(o.str("ti"), o.str("t"), o.str("f"), o.str("u")) }
        if (!Red.hayInternet(ctx) || artista.isBlank() || Regex("desconocid", RegexOption.IGNORE_CASE).containsMatchIn(artista)) return null
        return try {
            val (c1, b) = Red.crudo("GET", "https://es.wikipedia.org/w/api.php?action=query&list=search&format=json&srlimit=5&srsearch=" + enc(artista), timeoutMs = 8000)
            val titulo = if (c1 == 200) Bio.elegir(b, artista) else null
            val f = titulo?.let {
                val (c2, r) = Red.crudo("GET", "https://es.wikipedia.org/api/rest_v1/page/summary/" + enc(it.replace(' ', '_')).replace("+", "%20"), timeoutMs = 8000)
                if (c2 == 200) Bio.ficha(r, artista) else null
            }
            RadioApp.prefs.set(ck, Json.write(linkedMapOf("ti" to (f?.titulo ?: ""), "t" to (f?.texto ?: ""), "f" to (f?.foto ?: ""), "u" to (f?.url ?: ""))))
            f
        } catch (e: Exception) { null }
    }
}
