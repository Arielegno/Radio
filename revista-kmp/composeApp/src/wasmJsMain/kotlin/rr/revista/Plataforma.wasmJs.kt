package rr.revista

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.await
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

@JsFun("(m, url, cab, cuerpo) => fetch(url, { method: m, headers: JSON.parse(cab), body: cuerpo === '\\u0000' ? undefined : cuerpo }).then(r => r.text().then(t => JSON.stringify({ s: r.status, t: t, r: r.headers.get('content-range') || '' })))")
private external fun jsPedido(m: String, url: String, cab: String, cuerpo: String): Promise<JsString>

@JsFun("(k) => { try { return localStorage.getItem(k) || ''; } catch (e) { return ''; } }")
private external fun jsLeer(k: String): String

@JsFun("(k, v) => { try { if (v === '\\u0000') localStorage.removeItem(k); else localStorage.setItem(k, v); } catch (e) {} }")
private external fun jsGuardar(k: String, v: String)

@JsFun("() => Date.now()")
private external fun jsAhoraMs(): Double

@JsFun("(url) => fetch(url).then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.arrayBuffer(); }).then(b => { const u = new Uint8Array(b); let s = ''; for (let i = 0; i < u.length; i += 32768) s += String.fromCharCode.apply(null, u.subarray(i, i + 32768)); return btoa(s); })")
private external fun jsBytes64(url: String): Promise<JsString>

@JsFun("() => new Date().toISOString()")
private external fun jsAhora(): String

@JsFun("() => location.hash || ''")
private external fun jsHash(): String

@JsFun("(u) => { try { window.open(u, '_blank', 'noopener'); } catch (e) {} }")
private external fun jsAbrir(u: String)

@JsFun("() => { const c = document.getElementById('carga'); if (c) c.remove(); }")
private external fun jsListo()

actual suspend fun httpPedido(metodo: String, url: String, cabeceras: Map<String, String>, cuerpo: String?): Respuesta {
    val o = Json.obj(jsPedido(metodo, url, Json.write(cabeceras), cuerpo ?: "\u0000").await<JsString>().toString())
    return Respuesta(o.num("s").toInt(), o.str("t"), o.str("r"))
}
actual fun leerDato(clave: String): String? = jsLeer(clave).ifEmpty { null }
actual fun guardarDato(clave: String, valor: String?) = jsGuardar(clave, valor ?: "\u0000")
actual fun ahoraMs(): Double = jsAhoraMs()

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun httpBytes(url: String): ByteArray = Base64.decode(jsBytes64(url).await<JsString>().toString())

actual fun ahoraIso(): String = jsAhora()
actual fun hashInicial(): String = jsHash()
actual fun abrirEnlace(url: String) = jsAbrir(url)
actual fun listo() = jsListo()

actual fun decodificarImagen(bytes: ByteArray): ImageBitmap? =
    try { org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap() } catch (e: Throwable) { null }

actual fun crearFuente(bytes: ByteArray, nombre: String, peso: FontWeight): Font =
    androidx.compose.ui.text.platform.Font(nombre, bytes, peso)
