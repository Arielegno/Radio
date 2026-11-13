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
import io.github.arielegno.radio.core.asObj
import io.github.arielegno.radio.core.list
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
    /** Si la guía puede mover canciones de álbum (apagado de fábrica: solo limpia y completa lo que falta). */
    val cambiaAlbumes get() = RadioApp.prefs.bool("guiaCambiaAlbum", false)

    private suspend fun revisar(t0: Track, forzar: Boolean, cambiarAlbum: Boolean = cambiaAlbumes): Track? {
        var t = t0
        // lo que corregiste a mano se respeta siempre; el álbum solo se cambia si se permite (o si estaba vacío)
        val libre = { c: String -> if (c == "album") c !in t0.manual && (cambiarAlbum || t0.album.isEmpty()) else forzar || c !in t0.manual }
        var artista = if (libre("artist")) Limpieza.artista(t.artist) else t.artist
        if (libre("artist") && Red.hayInternet(ctx)) artista = nombreOficial(artista)
        val nombre = if (libre("name")) Limpieza.titulo(t.name, artista, t0.artist) else t.name
        t = t.copy(name = nombre, artist = artista)
        if ("album" !in t0.manual && albumBasura(t)) t = t.copy(album = "")   // basura («Unknown», páginas web…): se quita y se busca el real
        if (!Red.hayInternet(ctx)) return if (t != t0) t else null
        // Para buscar se usa el artista principal («Lana Del Rey, A$AP Rocky» → Lana Del Rey)
        val busca = t.copy(artist = Limpieza.artistaPrincipal(t.artist).ifBlank { t.artist })
        val d = (if (artistaDudoso(t)) null else datos(busca)) ?: Limpieza.Datos()
        t = Limpieza.aplicar(t, d, forzar, cambiarAlbum = cambiarAlbum || t.album.isEmpty())
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
        // si salió de un recopilatorio a su álbum real, también se cambia la carátula
        val salioDeRecopilatorio = Limpieza.esRecopilatorio(t0.album) && !Limpieza.esRecopilatorio(t.album) && t.album.isNotEmpty()
        if ((t.cover.isEmpty() || (forzar && "cover" !in t0.manual) || salioDeRecopilatorio) && (t0.cover.isEmpty() || salioDeRecopilatorio) && d2.portada.isNotEmpty() && libre("cover")) {
            bajarImagen(d2.portada)?.let { b -> bib.guardarCaratulaDe(b, t.id).takeIf { it.isNotEmpty() }?.let { t = t.copy(cover = it) } }
        }
        t = t.copy(revisada = true, manual = t0.manual)   // tus correcciones a mano se conservan siempre
        return if (t != t0) t else null
    }

    /** Botón «Completar este álbum»: busca los datos de sus canciones y, solo aquí, deja que la guía ordene el álbum. */
    suspend fun completarAlbum(canciones: List<Track>): String {
        if (!Red.hayInternet(ctx)) return "No hay conexión para consultar la guía."
        if (trabajando) return "La guía ya está trabajando: espera a que termine."
        trabajando = true
        try {
            val cambios = mutableListOf<Track>()
            for ((i, t0) in canciones.withIndex()) {
                progreso = "Completando el álbum (${i + 1} de ${canciones.size})…"
                bib.porId(t0.id)?.let { t -> revisar(t, forzar = false, cambiarAlbum = true)?.let { cambios += it } }
            }
            if (cambios.isNotEmpty()) bib.actualizar(cambios)
            return if (cambios.isEmpty()) "Este álbum ya estaba completo." else "Listo: la guía completó ${cambios.size} ${if (cambios.size == 1) "canción" else "canciones"} del álbum."
        } finally { progreso = ""; trabajando = false }
    }

    /** Las canciones de un disco que no tienes (según iTunes). */
    data class Faltantes(val album: String, val artista: String, val total: Int, val faltan: List<String>, val fecha: Long)

    private fun tituloBase(s: String) = normNombre(s.replace(Regex("\\s*[(\\[][^)\\]]*[)\\]]"), "").substringBefore(" - "))

    /** Busca el disco completo y compara con lo que tienes. null = no se encontró el disco. Se guarda para verlo en Ajustes. */
    suspend fun faltantes(album: String, canciones: List<Track>): Faltantes? {
        if (album.isBlank() || canciones.isEmpty() || !Red.hayInternet(ctx)) return null
        val artista = Limpieza.artistaPrincipal(canciones.groupingBy { it.artist }.eachCount().maxByOrNull { it.value }?.key ?: "").ifBlank { return null }
        val na = normNombre(artista); val nb = normNombre(io.github.arielegno.radio.core.Coleccion.albumBase(album))
        val (c, j) = try { Red.crudo("GET", "https://itunes.apple.com/search?media=music&entity=album&limit=15&term=" + enc("$artista $album"), timeoutMs = 9000) } catch (e: Exception) { return null }
        if (c != 200) return null
        val disco = try {
            Json.obj(j).list("results").mapNotNull { it.asObj() }.filter { normNombre(it.str("artistName")).contains(na) }
                .map { it to normNombre(io.github.arielegno.radio.core.Coleccion.albumBase(it.str("collectionName"))) }
                .sortedBy { (_, n) -> if (n == nb) 0 else if (n.contains(nb) || nb.contains(n)) 1 else 9 }
                .firstOrNull { (_, n) -> n == nb || (nb.length >= 4 && (n.contains(nb) || nb.contains(n))) }?.first
        } catch (e: Exception) { null } ?: return null
        val id = (disco["collectionId"] as? Number)?.toLong() ?: return null
        val (c2, j2) = try { Red.crudo("GET", "https://itunes.apple.com/lookup?entity=song&limit=200&id=$id", timeoutMs = 9000) } catch (e: Exception) { return null }
        if (c2 != 200) return null
        val pistas = try { Json.obj(j2).list("results").mapNotNull { it.asObj() }.filter { it.str("wrapperType") == "track" }
            .sortedWith(compareBy({ (it["discNumber"] as? Number)?.toInt() ?: 1 }, { (it["trackNumber"] as? Number)?.toInt() ?: 0 })).map { it.str("trackName") } } catch (e: Exception) { emptyList() }
        if (pistas.isEmpty()) return null
        val tengo = canciones.map { tituloBase(it.name) }.filter { it.isNotEmpty() }
        val faltan = pistas.filter { p -> val b = tituloBase(p); b.isNotEmpty() && tengo.none { it == b || (b.length >= 5 && (it.contains(b) || b.contains(it) && it.length >= 5)) } }.distinct()
        val r = Faltantes(album, artista, pistas.size, faltan, System.currentTimeMillis())
        guardarFaltantes(r)
        return r
    }

    fun faltantesGuardados(): List<Faltantes> = try {
        Json.arr(RadioApp.prefs.str("faltantes", "[]")).mapNotNull { it?.asObj() }.map { m ->
            Faltantes(m.str("album"), m.str("artista"), (m["total"] as? Number)?.toInt() ?: 0, (m["faltan"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(), (m["fecha"] as? Number)?.toLong() ?: 0L)
        }.sortedByDescending { it.faltan.size }
    } catch (e: Exception) { emptyList() }

    private fun guardarFaltantes(r: Faltantes) {
        val k = normNombre(r.artista) + "|" + normNombre(r.album)
        val l = faltantesGuardados().filter { normNombre(it.artista) + "|" + normNombre(it.album) != k } + listOfNotNull(r.takeIf { it.faltan.isNotEmpty() })
        RadioApp.prefs.set("faltantes", Json.write(l.takeLast(300).map { mapOf("album" to it.album, "artista" to it.artista, "total" to it.total, "faltan" to it.faltan, "fecha" to it.fecha) }))
    }
    fun olvidarFaltantes(album: String, artista: String) {
        val k = normNombre(artista) + "|" + normNombre(album)
        RadioApp.prefs.set("faltantes", Json.write(faltantesGuardados().filter { normNombre(it.artista) + "|" + normNombre(it.album) != k }.map { mapOf("album" to it.album, "artista" to it.artista, "total" to it.total, "faltan" to it.faltan, "fecha" to it.fecha) }))
    }

    /** Revisa qué canciones te faltan en tus álbumes (los que tienen al menos 2 canciones; hasta 40 por vez). */
    suspend fun revisarFaltantes(): String {
        if (!Red.hayInternet(ctx)) return "No hay conexión."
        if (trabajando) return "La guía ya está trabajando: espera a que termine."
        trabajando = true
        try {
            val albumes = bib.pistas.filter { it.album.isNotBlank() }.groupBy { io.github.arielegno.radio.core.Coleccion.albumBase(it.album) }
                .filter { it.value.size >= 2 }.entries.sortedByDescending { it.value.size }.take(40)
            var conFaltas = 0; var n = 0
            for ((a, l) in albumes) {
                if (!trabajando) break
                progreso = "Revisando tus álbumes (${++n} de ${albumes.size})…"
                if ((faltantes(a, l)?.faltan?.size ?: 0) > 0) conFaltas++
                kotlinx.coroutines.delay(1200)   // sin apurar a iTunes
            }
            return "Revisé ${albumes.size} álbumes: a $conFaltas les faltan canciones."
        } finally { progreso = ""; trabajando = false }
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
            val incompleta = { t: Track -> falta(t) > 0 || t.artist.isBlank() || Regex("desconocid", RegexOption.IGNORE_CASE).containsMatchIn(t.artist) }
            val pendientes = bib.pistas.filter { if (forzar) it.id !in hechas && (!it.revisada || incompleta(it)) else (!it.revisada || (reintentar && falta(it) > 0)) }.sortedByDescending(falta)
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
                if (lote.size >= 40) { bib.actualizar(lote.toList()); lote.clear(); if (forzar) RadioApp.prefs.set("guiaRonda", Json.write(hechas.toList())) }
            }
            bib.actualizar(lote)
            // Vuelta terminada: la próxima empieza de cero; si se canceló, sigue donde quedó
            if (forzar) RadioApp.prefs.set("guiaRonda", if (cancelar) Json.write(hechas.toList()) else "[]")
            return when {
                cancelar -> "Guía detenida: lo que ya se avanzó se conservó."
                nombres + datos > 0 -> "Biblioteca actualizada: $nombres nombre(s) limpiados y $datos canción(es) completadas."
                else -> "Tu biblioteca ya estaba limpia y completa."
            }
        } finally { trabajando = false; progreso = ""; bib.guardarAhora() }
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
