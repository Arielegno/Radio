package rr.revista

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.await
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

@JsFun("(url, cab) => fetch(url, { headers: JSON.parse(cab) }).then(r => r.ok ? r.text() : r.text().then(t => { throw new Error('HTTP ' + r.status + ' ' + t.slice(0, 160)); }))")
private external fun jsTexto(url: String, cab: String): Promise<JsString>

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

actual suspend fun httpTexto(url: String, cabeceras: Map<String, String>): String = jsTexto(url, Json.write(cabeceras)).await<JsString>().toString()

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
