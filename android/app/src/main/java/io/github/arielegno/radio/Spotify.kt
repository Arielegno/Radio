package io.github.arielegno.radio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.arielegno.radio.core.Json
import io.github.arielegno.radio.core.SpotifyAhora
import io.github.arielegno.radio.core.SpotifyDatos
import io.github.arielegno.radio.core.num
import io.github.arielegno.radio.core.str
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URLEncoder

/** Conexión con tu cuenta de Spotify (Premium) para controlar la app oficial de Spotify del celular. */
object Spotify {
    private val p get() = RadioApp.prefs
    private val candado = Mutex()

    var conectado by mutableStateOf(RadioApp.prefs.str("spRefresh", "").isNotEmpty()); private set
    var aviso by mutableStateOf(""); private set

    val clientId: String get() = p.str("spClientId", "").trim()

    /** Abre el navegador para entrar a Spotify. */
    fun entrar(ctx: Context): Boolean {
        if (clientId.isEmpty()) { aviso = "Pega primero tu Client ID de Spotify."; return false }
        val v = SpotifyDatos.verificador(); val estado = SpotifyDatos.verificador().take(16)
        p.set("spVerif", v); p.set("spEstado", estado)
        return try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SpotifyDatos.urlEntrar(clientId, v, estado))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }
        catch (e: Exception) { aviso = "No encontré un navegador para entrar a Spotify."; false }
    }

    /** Spotify vuelve a la app con radioderuta://spotify?code=…&state=… */
    suspend fun alVolver(uri: Uri) {
        val error = uri.getQueryParameter("error")
        if (error != null) { aviso = if (error == "access_denied") "Cancelaste la conexión con Spotify." else "Spotify respondió: $error"; return }
        val code = uri.getQueryParameter("code") ?: return
        if (uri.getQueryParameter("state") != p.str("spEstado", "")) { aviso = "La conexión con Spotify no coincide. Inténtalo otra vez."; return }
        val (c, t) = Red.crudo("POST", "https://accounts.spotify.com/api/token", SpotifyDatos.cuerpoCanje(clientId, code, p.str("spVerif", "")), tipo = "application/x-www-form-urlencoded")
        if (c !in 200..299) { aviso = "Spotify no aceptó la conexión ($c). Revisa que agregaste radioderuta://spotify en Redirect URIs."; return }
        guardar(t); p.remove("spVerif"); p.remove("spEstado")
        aviso = "Spotify conectado."
    }

    private fun guardar(json: String) {
        val o = Json.obj(json)
        p.set("spToken", o.str("access_token"))
        p.set("spExp", (System.currentTimeMillis() + o.num("expires_in", 3600) * 1000).toString())
        o.str("refresh_token").takeIf { it.isNotEmpty() }?.let { p.set("spRefresh", it) }
        conectado = p.str("spRefresh", "").isNotEmpty()
    }

    fun desconectar() { listOf("spToken", "spExp", "spRefresh").forEach { p.remove(it) }; conectado = false; aviso = "Spotify desconectado." }

    private suspend fun token(): String? = candado.withLock {
        val exp = p.str("spExp", "0").toLongOrNull() ?: 0L
        if (p.str("spToken", "").isNotEmpty() && exp - System.currentTimeMillis() > 60_000) return@withLock p.str("spToken", "")
        val r = p.str("spRefresh", ""); if (r.isEmpty() || clientId.isEmpty()) return@withLock null
        val (c, t) = Red.crudo("POST", "https://accounts.spotify.com/api/token", SpotifyDatos.cuerpoRenovar(clientId, r), tipo = "application/x-www-form-urlencoded")
        if (c == 400 || c == 401) { desconectar(); aviso = "Vuelve a conectar Spotify en Ajustes."; return@withLock null }
        if (c !in 200..299) return@withLock null
        guardar(t); p.str("spToken", "")
    }

    /** Llama a la API de Spotify. Devuelve (código, texto); 0 si no hay conexión. */
    suspend fun api(metodo: String, ruta: String, cuerpo: String? = null): Pair<Int, String> {
        val tk = token() ?: return 0 to ""
        return try { Red.crudo(metodo, "https://api.spotify.com/v1$ruta", cuerpo, mapOf("Authorization" to "Bearer $tk")) } catch (e: Exception) { 0 to "" }
    }

    suspend fun ahora(): SpotifyAhora? { val (c, t) = api("GET", "/me/player/currently-playing"); return if (c == 200) SpotifyDatos.ahora(t) else null }
    suspend fun siguiente(): SpotifyAhora? { val (c, t) = api("GET", "/me/player/queue"); return if (c == 200) SpotifyDatos.siguienteEnCola(t) else null }

    /** Pone a sonar un artista o una canción buscándola por nombre. */
    suspend fun buscarYPoner(q: String): Boolean {
        val (c, t) = api("GET", "/search?type=artist,track&limit=8&q=" + URLEncoder.encode(q, "UTF-8"))
        if (c != 200) return false
        val uri = SpotifyDatos.elegirBusqueda(t, q) ?: return false
        val cuerpo = if (uri.startsWith("spotify:track:")) Json.write(linkedMapOf("uris" to listOf(uri))) else Json.write(linkedMapOf("context_uri" to uri))
        return api("PUT", "/me/player/play", cuerpo).first in 200..299
    }

    /** Abre la app de Spotify (para que haya un dispositivo activo). */
    fun abrirApp(ctx: Context) {
        val i = ctx.packageManager.getLaunchIntentForPackage("com.spotify.music") ?: Intent(Intent.ACTION_VIEW, Uri.parse("spotify:"))
        try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { aviso = "Instala la app de Spotify para usarla con la radio." }
    }
}
