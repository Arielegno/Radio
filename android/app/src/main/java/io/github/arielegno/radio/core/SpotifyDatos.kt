package io.github.arielegno.radio.core

import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64
import kotlin.random.Random

/** Lo que está sonando en Spotify. */
data class SpotifyAhora(
    val uri: String, val nombre: String, val artista: String, val album: String, val anio: Int,
    val portada: String, val durMs: Long, val posMs: Long, val sonando: Boolean
) {
    /** Como canción de la radio (el id es negativo para no chocar con tu biblioteca). */
    fun comoTrack(): Track = Track(-(uri.hashCode().toLong() and 0x7fffffffL) - 1, nombre, artista, album, anio, "", durMs, file = "spotify")
}

object SpotifyDatos {
    const val REDIRECT = "radioderuta://spotify"
    const val PERMISOS = "user-read-playback-state user-modify-playback-state user-read-currently-playing"

    private const val LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    fun verificador(rnd: Random = Random.Default): String = (1..64).map { LETRAS[rnd.nextInt(LETRAS.length)] }.joinToString("")
    fun reto(verificador: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verificador.toByteArray(Charsets.US_ASCII)))

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    fun urlEntrar(clientId: String, verificador: String, estado: String): String =
        "https://accounts.spotify.com/authorize?response_type=code&client_id=${enc(clientId)}&scope=${enc(PERMISOS)}" +
            "&redirect_uri=${enc(REDIRECT)}&code_challenge_method=S256&code_challenge=${reto(verificador)}&state=${enc(estado)}"

    fun cuerpoCanje(clientId: String, code: String, verificador: String) =
        "grant_type=authorization_code&code=${enc(code)}&redirect_uri=${enc(REDIRECT)}&client_id=${enc(clientId)}&code_verifier=${enc(verificador)}"

    fun cuerpoRenovar(clientId: String, refresh: String) = "grant_type=refresh_token&refresh_token=${enc(refresh)}&client_id=${enc(clientId)}"

    /** Respuesta de /me/player/currently-playing (solo canciones). */
    fun ahora(json: String): SpotifyAhora? {
        val o = Json.obj(json); val it = o.map("item")
        if (it.isEmpty() || it.str("type", "track") != "track") return null
        val artistas = it.list("artists").mapNotNull { a -> a.asObj()?.str("name")?.takeIf { n -> n.isNotBlank() } }
        val alb = it.map("album")
        val img = alb.list("images").mapNotNull { x -> x.asObj() }.maxByOrNull { x -> x.num("width") }?.str("url").orEmpty()
        val anio = Regex("^(\\d{4})").find(alb.str("release_date"))?.groupValues?.get(1)?.toInt() ?: 0
        return SpotifyAhora(it.str("uri"), it.str("name"), artistas.joinToString(", ").ifEmpty { "Artista desconocido" }, alb.str("name"), anio,
            img, it.num("duration_ms"), o.num("progress_ms"), o.bool("is_playing"))
    }

    /** Primera canción de /me/player/queue. */
    fun siguienteEnCola(json: String): SpotifyAhora? {
        val q = Json.obj(json).list("queue").firstOrNull().asObj() ?: return null
        return ahora(Json.write(linkedMapOf("item" to q, "progress_ms" to 0, "is_playing" to false)))
    }

    /** De /search: el artista que mejor coincide (uri de contexto) o la canción. */
    fun elegirBusqueda(json: String, q: String): String? {
        val o = Json.obj(json); val n = normNombre(q)
        val art = o.map("artists").list("items").mapNotNull { it.asObj() }
        art.firstOrNull { normNombre(it.str("name")) == n }?.let { return it.str("uri") }
        val pistas = o.map("tracks").list("items").mapNotNull { it.asObj() }
        pistas.firstOrNull { normNombre(it.str("name")) == n }?.let { return it.str("uri") }
        art.firstOrNull { normNombre(it.str("name")).contains(n) }?.let { return it.str("uri") }
        return pistas.firstOrNull()?.str("uri") ?: art.firstOrNull()?.str("uri")
    }
}
