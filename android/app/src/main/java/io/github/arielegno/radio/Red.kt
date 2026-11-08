package io.github.arielegno.radio

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Peticiones a internet sencillas (sin librerías extra). */
object Red {
    class Error(val codigo: Int, mensaje: String) : IOException(mensaje)

    fun hayInternet(c: Context): Boolean {
        val cm = c.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun get(url: String, headers: Map<String, String> = emptyMap(), timeoutMs: Int = 10_000): String =
        pedir("GET", url, null, headers, timeoutMs)

    suspend fun post(url: String, body: String, headers: Map<String, String> = emptyMap(), timeoutMs: Int = 30_000): String =
        pedir("POST", url, body, headers + ("Content-Type" to "application/json"), timeoutMs)

    /** Petición que devuelve (código, texto) sin lanzar error por códigos 4xx/5xx. */
    suspend fun crudo(metodo: String, url: String, body: String? = null, headers: Map<String, String> = emptyMap(), tipo: String = "application/json", timeoutMs: Int = 12_000): Pair<Int, String> = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = metodo
            c.connectTimeout = timeoutMs; c.readTimeout = timeoutMs
            c.setRequestProperty("User-Agent", "RadioDeRuta-Android/1.0")
            c.setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            if (body != null) { c.doOutput = true; c.setRequestProperty("Content-Type", tipo); c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) } }
            else if (metodo == "PUT" || metodo == "POST") { c.doOutput = true; c.setFixedLengthStreamingMode(0); c.outputStream.close() }
            val code = c.responseCode
            val texto = try { (if (code in 200..299) c.inputStream else c.errorStream)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: "" } catch (e: Exception) { "" }
            code to texto
        } finally { c.disconnect() }
    }

    private suspend fun pedir(metodo: String, url: String, body: String?, headers: Map<String, String>, timeoutMs: Int): String = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = metodo
            c.connectTimeout = timeoutMs; c.readTimeout = timeoutMs
            c.setRequestProperty("User-Agent", "RadioDeRuta-Android/1.0")
            c.setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            if (body != null) { c.doOutput = true; c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) } }
            val code = c.responseCode
            val texto = (if (code in 200..299) c.inputStream else c.errorStream)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            if (code !in 200..299) throw Error(code, "Error $code")
            texto
        } finally {
            c.disconnect()
        }
    }
}
