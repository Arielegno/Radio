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

@JsFun("""(gr, mi, varias) => new Promise(ok => {
  const inp = document.createElement('input'); inp.type = 'file'; inp.accept = 'image/*'; if (varias) inp.multiple = true;
  inp.style.display = 'none'; document.body.appendChild(inp);
  const achicar = (img, max) => { const k = Math.min(1, max / Math.max(img.naturalWidth, img.naturalHeight)); const c = document.createElement('canvas');
    c.width = Math.max(1, Math.round(img.naturalWidth * k)); c.height = Math.max(1, Math.round(img.naturalHeight * k));
    const g = c.getContext('2d'); g.fillStyle = '#fff'; g.fillRect(0, 0, c.width, c.height); g.drawImage(img, 0, 0, c.width, c.height); return c.toDataURL('image/jpeg', 0.82); };
  const fin = r => { inp.remove(); ok(JSON.stringify(r)); };
  inp.addEventListener('cancel', () => fin([]));
  inp.addEventListener('change', async () => {
    const out = [];
    for (const f of [...inp.files].slice(0, 8)) {
      try { const u = URL.createObjectURL(f); const img = new Image(); img.src = u; await img.decode(); out.push({ g: achicar(img, gr), m: achicar(img, mi) }); URL.revokeObjectURL(u); } catch (e) {}
    }
    fin(out);
  });
  inp.click();
})""")
private external fun jsElegir(gr: Int, mi: Int, varias: Boolean): Promise<JsString>

@JsFun("""(url, cab, b64, tipo) => { const s = atob(b64), u = new Uint8Array(s.length); for (let i = 0; i < s.length; i++) u[i] = s.charCodeAt(i);
  return fetch(url, { method: 'POST', headers: Object.assign(JSON.parse(cab), { 'Content-Type': tipo }), body: new Blob([u], { type: tipo }) }).then(r => r.text().then(t => JSON.stringify({ s: r.status, t: t }))); }""")
private external fun jsSubir(url: String, cab: String, b64: String, tipo: String): Promise<JsString>

@JsFun("""(u, r, d, mx) => new Promise(ok => { const img = new Image();
  img.onload = () => { const W = img.naturalWidth, H = img.naturalHeight; let sw = W, sh = H, sx = 0, sy = 0;
    if (W / H > r) { sw = H * r; sx = (W - sw) * d; } else { sh = W / r; sy = (H - sh) * d; }
    const k = Math.min(1, mx / Math.max(sw, sh)), c = document.createElement('canvas'); c.width = Math.max(1, Math.round(sw * k)); c.height = Math.max(1, Math.round(sh * k));
    const g = c.getContext('2d'); g.fillStyle = '#fff'; g.fillRect(0, 0, c.width, c.height); g.drawImage(img, sx, sy, sw, sh, 0, 0, c.width, c.height); ok(c.toDataURL('image/jpeg', 0.85)); };
  img.onerror = () => ok(u); img.src = u; })""")
private external fun jsRecortar(u: String, r: Double, d: Double, mx: Int): Promise<JsString>

actual suspend fun recortarFoto(dataUrl: String, proporcion: Float, encuadre: Float, max: Int): String =
    jsRecortar(dataUrl, proporcion.toDouble(), encuadre.toDouble(), max).await<JsString>().toString()

@JsFun("(t, u) => { try { if (navigator.share) { navigator.share({ title: t, url: u }).catch(() => {}); return true; } } catch (e) {} try { navigator.clipboard.writeText(u); } catch (e) {} return false; }")
private external fun jsCompartir(t: String, u: String): Boolean
actual fun compartir(texto: String, url: String): Boolean = jsCompartir(texto, url)

@JsFun("""(t) => { try { const s = window.speechSynthesis; if (!s) return; s.cancel(); if (!t) return;
  const v = s.getVoices().find(v => /^es/i.test(v.lang));
  const partes = t.match(/[^.!?\n]+[.!?]*/g) || [t];   // en trozos: Chrome corta las frases muy largas
  partes.map(p => p.trim()).filter(Boolean).forEach(p => { const u = new SpeechSynthesisUtterance(p); u.lang = 'es-419'; if (v) u.voice = v; s.speak(u); });
} catch (e) {} }""")
private external fun jsVoz(t: String)
actual fun leerEnVoz(texto: String?) = jsVoz(texto ?: "")

@JsFun("() => { try { const k = localStorage.getItem('rr_geminiKey') || ''; const m = localStorage.getItem('rr_geminiModel') || localStorage.getItem('rr_geminiModelSelect') || ''; return JSON.stringify({ k: k.trim(), m: m.trim() }); } catch (e) { return '{}'; } }")
private external fun jsClaveIA(): String
actual suspend fun pedirIA(prompt: String): String {
    val c = Json.obj(jsClaveIA())
    val clave = c.str("k"); if (clave.isEmpty()) throw ErrorRevista("Pon tu clave de Gemini en la radio web (Ajustes › Sistema y Claves) para usar la IA.")
    val modelo = c.str("m").ifEmpty { "gemini-3.5-flash-lite" }.filter { it.isLetterOrDigit() || it == '-' || it == '.' }
    val r = httpPedido("POST", "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent", mapOf("content-type" to "application/json", "x-goog-api-key" to clave),
        Json.write(mapOf("contents" to listOf(mapOf("parts" to listOf(mapOf("text" to prompt)))))))
    val o = Json.obj(r.texto)
    if (r.estado !in 200..299) throw ErrorRevista(o.map("error").str("message").ifBlank { "La IA respondió con error ${r.estado}." })
    val partes = ((o["candidates"] as? List<*>)?.firstOrNull() as? Map<*, *>)?.let { (it["content"] as? Map<*, *>)?.get("parts") as? List<*> } ?: emptyList<Any?>()
    return partes.joinToString("") { (it as? Map<*, *>)?.get("text")?.toString() ?: "" }.trim().ifEmpty { throw ErrorRevista("La IA no respondió.") }
}

actual fun ponerEnRadio(canciones: List<Pair<String, String>>, artista: String?): Int = -1

@JsFun("() => { try { history.pushState({ rr: (history.state && history.state.rr || 0) + 1 }, ''); } catch (e) {} }")
private external fun jsEmpujar()
@JsFun("(n) => { try { window.__rrIgnorar = (window.__rrIgnorar || 0) + 1; history.go(-n); } catch (e) {} }")
private external fun jsAtras(n: Int)
@JsFun("(f) => { window.addEventListener('popstate', () => { if (window.__rrIgnorar > 0) { window.__rrIgnorar--; return; } f(); }); }")
private external fun jsAlVolver(f: () -> Unit)
actual fun historialEmpujar() = jsEmpujar()
actual fun historialAtras(n: Int) = jsAtras(n)
actual fun alVolverNavegador(f: () -> Unit) = jsAlVolver(f)

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

actual fun elegirImagenes(maxGrande: Int, maxMini: Int, varias: Boolean): Espera<List<Foto>> {
    val p = jsElegir(maxGrande, maxMini, varias)
    return object : Espera<List<Foto>> {
        override suspend fun esperar(): List<Foto> = Json.arr(p.await<JsString>().toString()).mapNotNull { it.asObj() }.map { Foto(it.str("g"), it.str("m")) }
    }
}

actual suspend fun subirArchivo(url: String, cabeceras: Map<String, String>, base64: String, tipo: String): Respuesta {
    val o = Json.obj(jsSubir(url, Json.write(cabeceras), base64, tipo).await<JsString>().toString())
    return Respuesta(o.num("s").toInt(), o.str("t"))
}
