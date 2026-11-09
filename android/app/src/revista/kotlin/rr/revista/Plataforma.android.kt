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
    BackHandler(enabled = Nav.dialogo != null || Nav.pila.isNotEmpty()) { if (Nav.dialogo != null) Nav.dialogo = null else Nav.atras() }
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
    c.setRequestProperty("Accept-Encoding", "identity")
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
    // Las letras se guardan en el teléfono para no bajarlas cada vez
    val guardada = if (url.startsWith("fonts/")) File(PlataformaAndroid.ctx.filesDir, "revista_" + url.replace('/', '_')) else null
    if (guardada != null && guardada.length() > 0) return@withContext guardada.readBytes()
    val c = conexion("GET", completa, emptyMap())
    try {
        if (c.responseCode !in 200..299) throw Exception("HTTP ${c.responseCode}")
        val b = c.inputStream.use { it.readBytes() }
        guardada?.let { runCatching { it.writeBytes(b) } }
        b
    } finally { c.disconnect() }
}

fun leerDato(clave: String): String? = datos.getString(clave, null)
fun guardarDato(clave: String, valor: String?) { datos.edit().apply { if (valor == null) remove(clave) else putString(clave, valor) }.apply() }
fun ahoraMs(): Double = System.currentTimeMillis().toDouble()
fun ahoraIso(): String = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).toString()
fun hashInicial(): String = PlataformaAndroid.hash
fun listo() {}
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

/** Achica, pone fondo blanco (por si es PNG transparente) y la pasa a JPEG como en la web. */
private fun jpeg(b: Bitmap, max: Int): String {
    val k = minOf(1f, max.toFloat() / maxOf(b.width, b.height))
    val w = maxOf(1, (b.width * k).toInt()); val h = maxOf(1, (b.height * k).toInt())
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    Canvas(out).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(Bitmap.createScaledBitmap(b, w, h, true), 0f, 0f, null) }
    val s = ByteArrayOutputStream(); out.compress(Bitmap.CompressFormat.JPEG, 82, s)
    return "data:image/jpeg;base64," + Base64.encodeToString(s.toByteArray(), Base64.NO_WRAP)
}
