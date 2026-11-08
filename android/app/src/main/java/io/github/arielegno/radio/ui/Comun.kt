package io.github.arielegno.radio.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.arielegno.radio.Red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Imágenes en memoria (carátulas y miniaturas) para no leerlas dos veces. */
object Imagenes {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun deArchivo(f: File, max: Int = 600): Bitmap? {
        val k = f.path + ":" + max + ":" + f.lastModified()
        cache.get(k)?.let { return it }
        return try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, o)
            var s = 1; while (o.outWidth / (s * 2) >= max && o.outHeight / (s * 2) >= max) s *= 2
            BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })?.also { cache.put(k, it) }
        } catch (e: Exception) { null }
    }

    private val permitidas = listOf(Regex("^https://i\\.ytimg\\.com/vi/[\\w-]+/hqdefault\\.jpg$"), Regex("^https://archive\\.org/services/img/[\\w.-]+$"),
        Regex("^https://[a-z0-9-]+\\.supabase\\.co/storage/v1/object/public/revista/[\\w/.-]+$", RegexOption.IGNORE_CASE), Regex("^https://upload\\.wikimedia\\.org/\\S+$"))

    fun deInternet(url: String): Bitmap? {
        if (permitidas.none { it.matches(url) }) return null
        cache.get(url)?.let { return it }
        return try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 8000; c.readTimeout = 8000; c.setRequestProperty("User-Agent", "RadioDeRuta-Android/1.0")
            val b = try { if (c.responseCode in 200..299) c.inputStream.use { it.readBytes() } else null } finally { c.disconnect() }
            b?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }?.also { cache.put(url, it) }
        } catch (e: Exception) { null }
    }

    /** Color promedio (para teñir el fondo según la carátula). */
    fun colorPromedio(b: Bitmap): Color = try {
        val p = Bitmap.createScaledBitmap(b, 1, 1, true).getPixel(0, 0)
        Color(android.graphics.Color.red(p), android.graphics.Color.green(p), android.graphics.Color.blue(p))
    } catch (e: Exception) { Color.Transparent }
}

@Composable
fun cargarImagen(f: File?, max: Int = 600): ImageBitmap? {
    val r by produceState<ImageBitmap?>(null, f?.path, f?.lastModified()) {
        value = if (f == null) null else withContext(Dispatchers.IO) { Imagenes.deArchivo(f, max)?.asImageBitmap() }
    }
    return r
}

@Composable
fun cargarImagenUrl(url: String): ImageBitmap? {
    val r by produceState<ImageBitmap?>(null, url) {
        value = if (url.isEmpty() || !Red.hayInternet(io.github.arielegno.radio.RadioApp.app)) null else withContext(Dispatchers.IO) { Imagenes.deInternet(url)?.asImageBitmap() }
    }
    return r
}

fun tono(semilla: String): Float = semilla.fold(7) { a, c -> (a * 31 + c.code) % 360 }.toFloat()

@Composable
fun Relleno(semilla: String, icono: ImageVector, modifier: Modifier, forma: Shape) {
    val h = tono(semilla)
    Box(modifier.clip(forma).background(Brush.linearGradient(listOf(Color.hsl(h, 0.7f, 0.56f), Color.hsl((h + 60) % 360, 0.65f, 0.42f)))), contentAlignment = Alignment.Center) {
        Icon(icono, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.fillMaxSize(0.4f))
    }
}

/** Carátula de un archivo o un relleno de color si no hay. */
@Composable
fun Caratula(f: File?, semilla: String, tam: Dp, radio: Dp = 12.dp, icono: ImageVector = Icons.Filled.MusicNote) {
    val img = cargarImagen(f, 400)
    val forma = RoundedCornerShape(radio)
    if (img != null) Image(img, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(tam).clip(forma))
    else Relleno(semilla, icono, Modifier.size(tam), forma)
}

/** Tarjeta redondeada de la app. */
@Composable
fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    val ex = LocalExtra.current
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ex.tarjeta).border(1.dp, ex.borde, RoundedCornerShape(20.dp)).padding(16.dp),
        content = contenido
    )
}

@Composable
fun Etiqueta(t: String, modifier: Modifier = Modifier) =
    Text(t.uppercase(), style = MaterialTheme.typography.labelMedium, color = LocalExtra.current.tenue, modifier = modifier)

@Composable
fun Titulo(t: String, modifier: Modifier = Modifier) =
    Text(t, style = MaterialTheme.typography.headlineSmall, modifier = modifier.padding(bottom = 10.dp))
