package rr.revista

/* La Revista nativa (la misma de revista-kmp/) dentro del APK: aquí está lo que en el
   navegador hace JavaScript — red, memoria, fotos, letras y enlaces — hecho con Android. */

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object PlataformaAndroid {
    lateinit var ctx: Context
    /** Pantalla pedida desde la app (#post=…, #perfil=…), como el «hash» de la web. */
    var hash = ""
    internal var lanzar: ((Boolean) -> Unit)? = null
    internal var pendiente: CompletableDeferred<List<Uri>>? = null
    /** Donde vive la versión web de la Revista nativa (las letras se bajan de ahí una vez). */
    const val BASE = "https://arielegno.github.io/Radio/revista-nativa/"
}

/** La Revista nativa completa, lista para ponerla en una pestaña de la app. */
@Composable
fun RevistaNativa(destino: String, alSalir: () -> Unit) {
    val ctx = LocalContext.current
    PlataformaAndroid.ctx = ctx.applicationContext
    val varias = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { PlataformaAndroid.pendiente?.complete(it); PlataformaAndroid.pendiente = null }
    val una = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { PlataformaAndroid.pendiente?.complete(listOfNotNull(it)); PlataformaAndroid.pendiente = null }
    SideEffect {
        PlataformaAndroid.lanzar = { muchas ->
            try { if (muchas) varias.launch("image/*") else una.launch("image/*") } catch (e: Exception) { PlataformaAndroid.pendiente?.complete(emptyList()); PlataformaAndroid.pendiente = null }
        }
    }
    BackHandler { alSalir() }
    BackHandler(enabled = Nav.dialogo != null || Nav.pila.isNotEmpty() || Visor.url != null || EditorFotos.actual != null) {
        when { EditorFotos.actual != null -> EditorFotos.cerrar(); Visor.url != null -> Visor.url = null; Nav.dialogo != null -> Nav.dialogo = null; else -> Nav.atras() }
    }
    key(destino) {
        PlataformaAndroid.hash = destino
        androidx.compose.runtime.remember(destino) { if (destino.isNotEmpty()) Nav.inicio(); 0 }
        App()
    }
}

private val datos by lazy { PlataformaAndroid.ctx.getSharedPreferences("revista_nativa", Context.MODE_PRIVATE) }

private fun conexion(metodo: String, url: String, cabeceras: Map<String, String>): HttpURLConnection {
    val c = URL(url).openConnection() as HttpURLConnection
    try { c.requestMethod = metodo } catch (e: java.net.ProtocolException) {
        // Por si el teléfono no acepta PATCH: se pone a la fuerza
        runCatching { HttpURLConnection::class.java.getDeclaredField("method").apply { isAccessible = true }.set(c, metodo) }
    }
    c.connectTimeout = 15_000; c.readTimeout = 30_000
    cabeceras.forEach { (k, v) -> c.setRequestProperty(k, v) }
    return c
}

private fun leer(c: HttpURLConnection): Respuesta {
    val estado = c.responseCode
    val flujo = if (estado >= 400) c.errorStream else c.inputStream
    val texto = flujo?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
    return Respuesta(estado, texto, c.getHeaderField("Content-Range") ?: "")
}

suspend fun httpPedido(metodo: String, url: String, cabeceras: Map<String, String>, cuerpo: String?): Respuesta = withContext(Dispatchers.IO) {
    val c = conexion(metodo, url, cabeceras)
    try {
        if (cuerpo != null) {
            if (cabeceras.keys.none { it.equals("Content-Type", true) }) c.setRequestProperty("Content-Type", "text/plain;charset=UTF-8")
            c.doOutput = true; c.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
        }
        leer(c)
    } finally { c.disconnect() }
}

suspend fun subirArchivo(url: String, cabeceras: Map<String, String>, base64: String, tipo: String): Respuesta = withContext(Dispatchers.IO) {
    val c = conexion("POST", url, cabeceras + ("Content-Type" to tipo))
    try { c.doOutput = true; c.outputStream.use { it.write(Base64.decode(base64, Base64.DEFAULT)) }; leer(c) } finally { c.disconnect() }
}

suspend fun httpBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
    val completa = if (url.startsWith("http")) url else PlataformaAndroid.BASE + url
    // Las letras se guardan para siempre; las imágenes, en una memoria de hasta 80 MB que se limpia sola
    val guardada = when {
        url.startsWith("fonts/") -> File(PlataformaAndroid.ctx.filesDir, "revista_" + url.replace('/', '_'))
        url.contains("/storage/v1/object/public/") || url.startsWith("https://upload.wikimedia.org/") -> File(cacheImagenes(), url.hashCode().toUInt().toString(16) + "_" + url.length + ".img")
        else -> null
    }
    if (guardada != null && guardada.length() > 0) { guardada.setLastModified(System.currentTimeMillis()); return@withContext guardada.readBytes() }
    if (url.startsWith("fonts/")) runCatching { return@withContext PlataformaAndroid.ctx.assets.open("revista_fonts/" + url.removePrefix("fonts/")).use { it.readBytes() } }
    val c = conexion("GET", completa, emptyMap())
    try {
        if (c.responseCode !in 200..299) throw Exception("HTTP ${c.responseCode}")
        val b = c.inputStream.use { it.readBytes() }
        guardada?.let { runCatching { it.writeBytes(b); if (it.parentFile?.name == "revista_img") limpiarCache(it.parentFile!!) } }
        b
    } finally { c.disconnect() }
}

private fun cacheImagenes() = File(PlataformaAndroid.ctx.cacheDir, "revista_img").apply { mkdirs() }
private var ultimaLimpieza = 0L
private fun limpiarCache(dir: File) {
    if (System.currentTimeMillis() - ultimaLimpieza < 60_000) return
    ultimaLimpieza = System.currentTimeMillis()
    val fs = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
    var total = 0L
    fs.forEach { f -> total += f.length(); if (total > 80L * 1024 * 1024) f.delete() }
}

fun leerDato(clave: String): String? = datos.getString(clave, null)
fun guardarDato(clave: String, valor: String?) { datos.edit().apply { if (valor == null) remove(clave) else putString(clave, valor) }.apply() }
fun ahoraMs(): Double = System.currentTimeMillis().toDouble()
fun ahoraIso(): String = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).toString()
fun hashInicial(): String = PlataformaAndroid.hash
fun listo() {}
// ---------- Voz, IA y radio: usan lo que ya tiene la app ----------
private var tts: android.speech.tts.TextToSpeech? = null
private var ttsListo = false
private var pendienteVoz: String? = null
fun leerEnVoz(texto: String?) {
    if (texto.isNullOrBlank()) { tts?.stop(); runCatching { io.github.arielegno.radio.RadioApp.radio.retener(false) }; return }
    val t = tts
    if (t == null) {
        pendienteVoz = texto
        tts = android.speech.tts.TextToSpeech(PlataformaAndroid.ctx) { st ->
            ttsListo = st == android.speech.tts.TextToSpeech.SUCCESS
            if (ttsListo) { tts?.language = java.util.Locale("es", "419"); pendienteVoz?.let { hablar(it) }; pendienteVoz = null }
        }
    } else if (ttsListo) hablar(texto) else pendienteVoz = texto
}
private fun hablar(texto: String) {
    val t = tts ?: return
    // La radio se pausa mientras se lee el artículo
    runCatching { io.github.arielegno.radio.RadioApp.radio.retener(true) }
    t.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
        override fun onStart(id: String?) {}
        override fun onDone(id: String?) { if (id == "fin") runCatching { android.os.Handler(android.os.Looper.getMainLooper()).post { io.github.arielegno.radio.RadioApp.radio.retener(false) } } }
        @Deprecated("") override fun onError(id: String?) {}
    })
    val partes = texto.chunked(3500)
    partes.forEachIndexed { i, p -> t.speak(p, if (i == 0) android.speech.tts.TextToSpeech.QUEUE_FLUSH else android.speech.tts.TextToSpeech.QUEUE_ADD, null, if (i == partes.lastIndex) "fin" else "p$i") }
}

suspend fun pedirIA(prompt: String): String = withContext(Dispatchers.IO) {
    val p = io.github.arielegno.radio.RadioApp.prefs
    val claude = p.str("proveedor", "gemini") == "claude"
    val (codigo, texto) = if (claude) {
        val clave = p.str("claudeKey", "").trim(); if (clave.isEmpty()) throw ErrorRevista("Pon tu clave de Claude en Ajustes de la app para usar la IA.")
        val modelo = p.str("claudeModel", "claude-sonnet-5-5").trim().ifEmpty { "claude-sonnet-5-5" }
        io.github.arielegno.radio.Red.crudo("POST", "https://api.anthropic.com/v1/messages", io.github.arielegno.radio.core.Luna.peticionClaude(modelo, "Eres un corrector y editor de una revista de música en español.", prompt),
            mapOf("x-api-key" to clave, "anthropic-version" to "2023-06-01"), timeoutMs = 40_000)
    } else {
        val clave = p.str("geminiKey", "").trim(); if (clave.isEmpty()) throw ErrorRevista("Pon tu clave de Gemini en Ajustes de la app para usar la IA.")
        val modelo = p.str("geminiModel", "").trim().ifEmpty { p.str("geminiModelSel", "gemini-3.5-flash-lite") }.filter { it.isLetterOrDigit() || it == '-' || it == '.' }
        io.github.arielegno.radio.Red.crudo("POST", "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent", io.github.arielegno.radio.core.Luna.peticionGemini("Eres un corrector y editor de una revista de música en español.", prompt),
            mapOf("x-goog-api-key" to clave), timeoutMs = 40_000)
    }
    if (codigo !in 200..299) throw ErrorRevista("La IA respondió con error $codigo.")
    (if (claude) io.github.arielegno.radio.core.Luna.crudoClaude(texto) else io.github.arielegno.radio.core.Luna.crudoGemini(texto))?.trim()?.takeIf { it.isNotEmpty() } ?: throw ErrorRevista("La IA no respondió.")
}

private fun nk(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9]+"), " ").trim()
fun ponerEnRadio(canciones: List<Pair<String, String>>, artista: String?): Int {
    val app = io.github.arielegno.radio.RadioApp
    val pistas = app.biblioteca.pistas
    val ids = if (artista != null) { val a = nk(artista); pistas.filter { nk(it.artist).contains(a) && a.isNotEmpty() }.map { it.id }.toSet() }
              else { val q = canciones.map { nk(it.first) + "|" + nk(it.second) }.toSet(); pistas.filter { (nk(it.name) + "|" + nk(it.artist)) in q }.map { it.id }.toSet() }
    if (ids.isEmpty()) return 0
    android.os.Handler(android.os.Looper.getMainLooper()).post { runCatching { app.radio.iniciar(filtro = { it.id in ids }, mezclarEsta = false) } }
    return ids.size
}

fun historialEmpujar() {}
fun historialAtras(n: Int) {}
fun alVolverNavegador(f: () -> Unit) {}

fun compartir(texto: String, url: String): Boolean {
    val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "$texto\n$url")
    return runCatching { PlataformaAndroid.ctx.startActivity(Intent.createChooser(i, "Compartir").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)
}
fun abrirEnlace(url: String) {
    runCatching { PlataformaAndroid.ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun decodificarImagen(bytes: ByteArray): ImageBitmap? = try {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
    var m = 1; while (maxOf(o.outWidth, o.outHeight) / (m * 2) >= 1600) m *= 2   // fotos enormes: se achican al leerlas
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = m })?.asImageBitmap()
} catch (e: Throwable) { null }

fun crearFuente(bytes: ByteArray, nombre: String, peso: FontWeight): Font {
    val f = File(PlataformaAndroid.ctx.cacheDir, "rv_$nombre")
    if (f.length() != bytes.size.toLong()) f.writeBytes(bytes)
    return Font(f, peso)
}

fun elegirImagenes(maxGrande: Int, maxMini: Int, varias: Boolean): Espera<List<Foto>> {
    PlataformaAndroid.pendiente?.complete(emptyList())
    val d = CompletableDeferred<List<Uri>>(); PlataformaAndroid.pendiente = d
    val l = PlataformaAndroid.lanzar
    if (l == null) d.complete(emptyList()) else l(varias)
    return object : Espera<List<Foto>> {
        override suspend fun esperar(): List<Foto> {
            val uris = d.await()
            return withContext(Dispatchers.IO) { uris.take(8).mapNotNull { u -> runCatching { foto(u, maxGrande, maxMini) }.getOrNull() } }
        }
    }
}

private fun foto(u: Uri, grande: Int, mini: Int): Foto? {
    val cr = PlataformaAndroid.ctx.contentResolver
    val bm: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(cr, u)) { dec, info, _ ->
            val k = minOf(1f, grande.toFloat() / maxOf(info.size.width, info.size.height))
            dec.setTargetSize(maxOf(1, (info.size.width * k).toInt()), maxOf(1, (info.size.height * k).toInt()))
            dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }
        var m = 1; while (maxOf(o.outWidth, o.outHeight) / (m * 2) >= grande) m *= 2
        cr.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = m }) } ?: return null
    }
    return Foto(jpeg(bm, grande), jpeg(bm, mini))
}

suspend fun procesarFoto(dataUrl: String, giro: Int, espejo: Boolean, proporcion: Float, zoom: Float, cx: Float, cy: Float, matriz: String, vineta: Float, max: Int): String = withContext(Dispatchers.Default) {
    val b = Base64.decode(dataUrl.substringAfter(','), Base64.DEFAULT)
    val bm = BitmapFactory.decodeByteArray(b, 0, b.size) ?: return@withContext dataUrl
    val m = android.graphics.Matrix().apply { if (espejo) postScale(-1f, 1f); postRotate((((giro % 360) + 360) % 360).toFloat()) }
    val rot = if (giro % 360 != 0 || espejo) Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, m, true) else bm
    val w = rot.width.toFloat(); val h = rot.height.toFloat()
    val r = if (proporcion > 0f) proporcion else w / h
    var cw = w; var ch = h
    if (w / h > r) cw = h * r else ch = w / r
    cw /= zoom.coerceAtLeast(1f); ch /= zoom.coerceAtLeast(1f)
    val sx = ((w - cw) * cx).coerceIn(0f, w - 1f); val sy = ((h - ch) * cy).coerceIn(0f, h - 1f)
    val k = minOf(1f, max / maxOf(cw, ch))
    val ow = (cw * k).toInt().coerceAtLeast(1); val oh = (ch * k).toInt().coerceAtLeast(1)
    val out = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
    val lienzo = Canvas(out); lienzo.drawColor(android.graphics.Color.WHITE)
    val pintura = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
    matriz.split(',').mapNotNull { it.trim().toFloatOrNull() }.takeIf { it.size == 20 }?.let { pintura.colorFilter = android.graphics.ColorMatrixColorFilter(it.toFloatArray()) }
    lienzo.drawBitmap(rot, android.graphics.Rect(sx.toInt(), sy.toInt(), (sx + cw).toInt().coerceAtMost(rot.width), (sy + ch).toInt().coerceAtMost(rot.height)), android.graphics.RectF(0f, 0f, ow.toFloat(), oh.toFloat()), pintura)
    if (vineta > 0f) {
        val v = android.graphics.Paint().apply { shader = android.graphics.RadialGradient(ow / 2f, oh / 2f, kotlin.math.hypot(ow.toFloat(), oh.toFloat()) / 2f,
            intArrayOf(0, 0, android.graphics.Color.argb((vineta * 255).toInt().coerceIn(0, 255), 0, 0, 0)), floatArrayOf(0f, .45f, 1f), android.graphics.Shader.TileMode.CLAMP) }
        lienzo.drawRect(0f, 0f, ow.toFloat(), oh.toFloat(), v)
    }
    val s = ByteArrayOutputStream(); out.compress(Bitmap.CompressFormat.JPEG, 86, s)
    "data:image/jpeg;base64," + Base64.encodeToString(s.toByteArray(), Base64.NO_WRAP)
}

suspend fun recortarFoto(dataUrl: String, proporcion: Float, encuadre: Float, max: Int): String = withContext(Dispatchers.Default) {
    val b = Base64.decode(dataUrl.substringAfter(','), Base64.DEFAULT)
    val bm = BitmapFactory.decodeByteArray(b, 0, b.size) ?: return@withContext dataUrl
    var sw = bm.width; var sh = bm.height; var sx = 0; var sy = 0
    if (bm.width.toFloat() / bm.height > proporcion) { sw = (bm.height * proporcion).toInt().coerceIn(1, bm.width); sx = ((bm.width - sw) * encuadre).toInt() }
    else { sh = (bm.width / proporcion).toInt().coerceIn(1, bm.height); sy = ((bm.height - sh) * encuadre).toInt() }
    jpeg(Bitmap.createBitmap(bm, sx, sy, sw, sh), max)
}

/** Achica, pone fondo blanco (por si es PNG transparente) y la pasa a JPEG como en la web. */
private fun jpeg(b: Bitmap, max: Int): String {
    val k = minOf(1f, max.toFloat() / maxOf(b.width, b.height))
    val w = maxOf(1, (b.width * k).toInt()); val h = maxOf(1, (b.height * k).toInt())
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    Canvas(out).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(Bitmap.createScaledBitmap(b, w, h, true), 0f, 0f, null) }
    val s = ByteArrayOutputStream(); out.compress(Bitmap.CompressFormat.JPEG, 82, s)
    return "data:image/jpeg;base64," + Base64.encodeToString(s.toByteArray(), Base64.NO_WRAP)
}
