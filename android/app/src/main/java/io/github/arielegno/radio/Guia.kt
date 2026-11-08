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

    /** Limpia y completa una canción. forzar = «Completar» (también cambia lo editado a mano). */
    private suspend fun revisar(t0: Track, forzar: Boolean): Track? {
        var t = t0
        val libre = { c: String -> forzar || c !in t0.manual }
        var artista = if (libre("artist")) Limpieza.artista(t.artist) else t.artist
        if (libre("artist") && Red.hayInternet(ctx)) artista = nombreOficial(artista)
        val nombre = if (libre("name")) Limpieza.titulo(t.name, artista, t0.artist) else t.name
        t = t.copy(name = nombre, artist = artista)
        val d = datos(t) ?: return if (t != t0) t else null
        t = Limpieza.aplicar(t, d, forzar)
        if ((t.cover.isEmpty() || (forzar && "cover" !in t0.manual)) && t0.cover.isEmpty() && d.portada.isNotEmpty() && libre("cover")) {
            bajarImagen(d.portada)?.let { b -> bib.guardarCaratulaDe(b, t.id).takeIf { it.isNotEmpty() }?.let { t = t.copy(cover = it) } }
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
            val pendientes = bib.pistas.filter { forzar || !it.revisada }.sortedByDescending(falta)
            val lote = mutableListOf<Track>()
            pendientes.forEachIndexed { i, t ->
                if (cancelar) return@forEachIndexed
                progreso = "Completando datos (${i + 1} de ${pendientes.size})"
                val actual = bib.porId(t.id) ?: return@forEachIndexed
                val n = revisar(actual, false)
                if (n != null) { lote += n; if (n.album != actual.album || n.year != actual.year || n.cover != actual.cover) datos++ }
                else lote += actual.copy(revisada = true)
                if (lote.size >= 8) { bib.actualizar(lote.toList()); lote.clear() }
            }
            bib.actualizar(lote)
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
