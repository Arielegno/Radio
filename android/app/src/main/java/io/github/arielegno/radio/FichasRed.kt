package io.github.arielegno.radio

import io.github.arielegno.radio.core.Fichas
import io.github.arielegno.radio.core.Json
import io.github.arielegno.radio.core.Luna
import io.github.arielegno.radio.core.num
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Fichas de artistas, integrantes y festivales: tu nube → Wikipedia → la IA (todo guardado para verlo sin internet). */
object FichasRed {
    private val p get() = RadioApp.prefs
    private val ctx get() = RadioApp.app
    private val turnos = Semaphore(2)
    val dirFotos: File get() = File(ctx.filesDir, "fotos").apply { mkdirs() }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    // ---------- Tu nube (la misma de la web: artistas/, festivales/) ----------
    fun nubeBase(): String = p.str("nube", "").trim().takeIf { it.startsWith("https://") }?.let { if (it.endsWith("/")) it else "$it/" } ?: ""

    private suspend fun nube(tipo: String, id: String): String? {
        val base = nubeBase(); val sl = Fichas.slug(id); if (base.isEmpty() || sl.isEmpty()) return null
        val ck = "nube_${tipo}_$sl"
        val c = Json.obj(p.str(ck, ""))
        val fresco = c.isNotEmpty() && System.currentTimeMillis() - c.num("t") < (if (c["d"] != null) 7 else 1) * 86_400_000L
        if (fresco) return c["d"]?.let { Json.write(it) }
        if (!Red.hayInternet(ctx)) return c["d"]?.let { Json.write(it) }
        return try {
            val (cod, t) = Red.crudo("GET", base + "$tipo/$sl.json", timeoutMs = 7000)
            when {
                cod == 404 -> { p.set(ck, Json.write(linkedMapOf("t" to System.currentTimeMillis(), "d" to null))); null }
                cod in 200..299 -> { val o = Json.obj(t); p.set(ck, Json.write(linkedMapOf("t" to System.currentTimeMillis(), "d" to o.ifEmpty { null }))); if (o.isEmpty()) null else t }
                else -> c["d"]?.let { Json.write(it) }
            }
        } catch (e: Exception) { c["d"]?.let { Json.write(it) } }
    }

    // ---------- Inteligencia artificial (Gemini o Claude, según Ajustes) ----------
    private suspend fun ia(ck: String, sistema: String, usuario: String): Map<String, Any?>? {
        Fichas.jsonDeTexto(p.str(ck, ""))?.let { return it }
        if (!Red.hayInternet(ctx)) return null
        val texto = try {
            if (p.str("proveedor", "gemini") == "claude") {
                val clave = p.str("claudeKey", "").trim(); if (clave.isEmpty()) return null
                val modelo = p.str("claudeModel", "claude-sonnet-5-5").trim().ifEmpty { "claude-sonnet-5-5" }
                val (c, t) = Red.crudo("POST", "https://api.anthropic.com/v1/messages", Luna.peticionClaude(modelo, sistema, usuario),
                    mapOf("x-api-key" to clave, "anthropic-version" to "2023-06-01"), timeoutMs = 30_000)
                if (c in 200..299) Luna.crudoClaude(t) else null
            } else {
                val clave = p.str("geminiKey", "").trim(); if (clave.isEmpty()) return null
                val modelo = p.str("geminiModel", "").trim().ifEmpty { p.str("geminiModelSel", "gemini-3.5-flash-lite") }.filter { it.isLetterOrDigit() || it == '-' || it == '.' }
                val (c, t) = Red.crudo("POST", "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent", Luna.peticionGemini(sistema, usuario),
                    mapOf("x-goog-api-key" to clave), timeoutMs = 30_000)
                if (c in 200..299) Luna.crudoGemini(t) else null
            }
        } catch (e: Exception) { null }
        val j = Fichas.jsonDeTexto(texto) ?: return null
        p.set(ck, Json.write(j)); return j
    }

    fun hayIA(): Boolean = if (p.str("proveedor", "gemini") == "claude") p.str("claudeKey", "").isNotBlank() else p.str("geminiKey", "").isNotBlank()

    // ---------- Wikipedia ----------
    private suspend fun wiki(titulo: String): Fichas.Wiki? {
        val ck = "wiki_v1_" + Fichas.slug(titulo)
        p.str(ck, "").takeIf { it.isNotEmpty() }?.let { return if (it == "-") null else Fichas.wiki(it) }
        if (!Red.hayInternet(ctx)) return null
        return try {
            val (c, t) = Red.crudo("GET", "https://es.wikipedia.org/api/rest_v1/page/summary/" + enc(titulo.trim().replace(' ', '_')) + "?redirect=true", timeoutMs = 8000)
            if (c == 404) { p.set(ck, "-"); null } else if (c == 200) { p.set(ck, t); Fichas.wiki(t) } else null
        } catch (e: Exception) { null }
    }

    // ---------- Artista ----------
    /** Tu biografía editada manda; después tu nube; después la IA; si no hay clave, Wikipedia. */
    suspend fun artista(nombre: String): Fichas.Artista? {
        val propia = p.str("bio_user_" + Fichas.slug(nombre), "")
        val doc = Fichas.artistaDeNube(nube("artistas", nombre))
            ?: ia("bio_v3_" + Fichas.slug(nombre), Fichas.PROMPT_ARTISTA, "Artista: \"$nombre\".")?.let { Fichas.artistaDeIA(Json.write(it)) }
            ?: Guia.bio(nombre)?.let { Fichas.Artista(bio = it.texto, fuente = "wiki") }
        if (propia.isNotEmpty()) return (doc ?: Fichas.Artista()).copy(bio = propia, fuente = "tuya")
        return doc
    }

    fun guardarBioPropia(nombre: String, bio: String) { val k = "bio_user_" + Fichas.slug(nombre); if (bio.isBlank()) p.remove(k) else p.set(k, bio.trim()) }
    fun bioPropia(nombre: String): String = p.str("bio_user_" + Fichas.slug(nombre), "")

    suspend fun miembro(nombre: String, banda: String, obj: Map<String, Any?>): Fichas.Miembro {
        val w = if (obj["bio"] != null && obj["imagen"] != null) null else wiki(nombre)?.takeIf { Fichas.esMusico(it, banda) }
        val i = if (Fichas.miembroIncompleto(obj)) ia("miembro_v1_${Fichas.slug(banda)}_${Fichas.slug(nombre)}", Fichas.PROMPT_MIEMBRO, "Integrante: \"$nombre\" de la banda \"$banda\".") else null
        return Fichas.miembro(obj, w, i)
    }

    suspend fun festival(nombre: String, id: String): Fichas.FichaFestival {
        val limpio = Fichas.festivalLimpio(nombre)
        val nb = Json.obj(nube("festivales", id.ifEmpty { limpio }))
        val w = if (nb.isEmpty()) wiki(limpio)?.takeIf { Fichas.esFestival(it) } else null
        val base = Fichas.festival(nb, w, null)
        if (base.historia.isNotEmpty() && base.ediciones.isNotEmpty() && base.participantes.isNotEmpty()) return base
        val i = ia("festival_v1_" + Fichas.slug(limpio), Fichas.PROMPT_FESTIVAL, "Festival: \"$limpio\".")
        return Fichas.festival(nb, w, i)
    }

    // ---------- Fotos ----------
    fun fotoPropia(tipo: String, clave: String): File = File(dirFotos, "$tipo-${Fichas.slug(clave)}.jpg")

    /** Foto del artista guardada en el teléfono: la tuya, o la de Spotify, o la de Wikipedia. null = usar la carátula. */
    suspend fun fotoArtista(nombre: String): File? {
        if (nombre.isBlank() || Regex("desconocid", RegexOption.IGNORE_CASE).containsMatchIn(nombre)) return null
        val f = fotoPropia("artista", nombre)
        if (f.exists() && f.length() > 0) return f
        val ck = "artimg_" + Fichas.slug(nombre)
        val marca = p.str(ck, "").toLongOrNull() ?: 0L
        if (System.currentTimeMillis() - marca < 7 * 86_400_000L || !Red.hayInternet(ctx)) return null
        return turnos.withPermit {
            if (f.exists() && f.length() > 0) return@withPermit f
            var url = ""
            if (Spotify.conectado) {
                val (c, t) = Spotify.api("GET", "/search?type=artist&limit=5&q=" + enc("artist:\"" + nombre.replace("\"", "") + "\""))
                if (c == 200) url = Fichas.fotoSpotify(t, nombre)
            }
            if (url.isEmpty()) url = Guia.bio(nombre)?.foto ?: ""
            val ok = url.isNotEmpty() && bajar(url, f)
            if (!ok) p.set(ck, System.currentTimeMillis().toString())
            if (ok) f else null
        }
    }

    /** Descarga una imagen a un archivo (solo https y solo imágenes). */
    suspend fun bajar(url: String, destino: File): Boolean = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://")) return@withContext false
        if (RadioApp.prefs.bool("soloWifi", false) && !esWifi()) return@withContext false
        try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 10_000; c.readTimeout = 15_000; c.setRequestProperty("User-Agent", "RadioDeRuta/1.3 ( https://arielegno.github.io/Radio/ )")
            try {
                if (c.responseCode != 200 || c.contentType?.startsWith("image/") != true) return@withContext false
                val b = c.inputStream.use { it.readBytes() }
                if (b.size < 1000) return@withContext false
                val tmp = File(destino.parentFile, destino.name + ".tmp"); tmp.writeBytes(b); tmp.renameTo(destino)
            } finally { c.disconnect() }
        } catch (e: Exception) { false }
    }

    private fun esWifi(): Boolean {
        val cm = ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /** Guarda una foto elegida de la galería (reducida a 1200 px). */
    suspend fun guardarFotoPropia(tipo: String, clave: String, uri: android.net.Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext false
            val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
            var s = 1; while (o.outWidth / (s * 2) >= 1200 && o.outHeight / (s * 2) >= 1200) s *= 2
            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = s }) ?: return@withContext false
            val k = minOf(1f, 1200f / maxOf(bmp.width, bmp.height))
            val fin = if (k < 1f) android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt().coerceAtLeast(1), (bmp.height * k).toInt().coerceAtLeast(1), true) else bmp
            fotoPropia(tipo, clave).outputStream().use { fin.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
            true
        } catch (e: Exception) { false }
    }
}
