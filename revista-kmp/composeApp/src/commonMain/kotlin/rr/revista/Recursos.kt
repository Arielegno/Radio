package rr.revista

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

expect fun decodificarImagen(bytes: ByteArray): ImageBitmap?
expect fun crearFuente(bytes: ByteArray, nombre: String, peso: FontWeight): Font
expect fun listo()

/** Imágenes de internet con memoria (las últimas 80) y como mucho 4 descargas a la vez. */
object Imagenes {
    private val cache = LinkedHashMap<String, ImageBitmap>()
    private val fallidas = HashSet<String>()
    private val turno = Semaphore(4)

    fun ya(url: String): ImageBitmap? = cache[url]

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun cargar(url: String): ImageBitmap? {
        if (url.isEmpty() || url in fallidas) return null
        cache[url]?.let { return it }
        val img = try {
            val bytes = if (url.startsWith("data:")) Base64.decode(url.substringAfter(',')) else turno.withPermit { httpBytes(url) }
            decodificarImagen(bytes)
        } catch (e: Throwable) { null }
        if (img == null) { fallidas += url; return null }
        cache[url] = img; while (cache.size > 80) cache.remove(cache.keys.first())
        return img
    }
}

/** Carga una imagen; si falla, prueba la de respaldo (por ejemplo, la grande cuando no hay miniatura). */
@Composable
fun imagen(url: String, respaldo: String = ""): ImageBitmap? {
    val v by produceState(Imagenes.ya(url), url, respaldo) { if (value == null) value = Imagenes.cargar(url) ?: (if (respaldo.isNotEmpty() && respaldo != url) Imagenes.cargar(respaldo) else null) }
    return v
}

/** Letras de la Revista (Google Fonts, licencia OFL), las mismas que usa la web. */
object Fuentes {
    var familias by mutableStateOf<Map<String, FontFamily>>(emptyMap()); private set
    var cargadas by mutableStateOf(false); private set

    private val ARCHIVOS = mapOf(
        "barlow" to listOf("Barlow-Regular.ttf" to 400, "Barlow-SemiBold.ttf" to 600, "Barlow-Bold.ttf" to 700),
        "condensada" to listOf("BarlowCondensed-SemiBold.ttf" to 600, "BarlowCondensed-Bold.ttf" to 700, "BarlowCondensed-ExtraBold.ttf" to 800),
        "playfair" to listOf("PlayfairDisplay.ttf" to 700), "lora" to listOf("Lora.ttf" to 400),
        "mono" to listOf("SpaceMono-Regular.ttf" to 400, "SpaceMono-Bold.ttf" to 700), "caveat" to listOf("Caveat.ttf" to 600),
        "abril" to listOf("AbrilFatface-Regular.ttf" to 400), "anton" to listOf("Anton-Regular.ttf" to 400), "shrikhand" to listOf("Shrikhand-Regular.ttf" to 400),
        "rampart" to listOf("RampartOne-Regular.ttf" to 400), "glitch" to listOf("RubikGlitch-Regular.ttf" to 400), "archivo" to listOf("ArchivoBlack-Regular.ttf" to 400),
        "simbolos" to listOf("NotoSansSymbols2-Regular.ttf" to 400)
    )

    suspend fun cargar() {
        if (cargadas) return
        val r = coroutineScope {
            ARCHIVOS.map { (k, fs) ->
                async {
                    val fonts = fs.mapNotNull { (f, w) -> try { crearFuente(httpBytes("fonts/$f"), f, FontWeight(w)) } catch (e: Throwable) { null } }
                    if (fonts.isEmpty()) null else k to FontFamily(fonts)
                }
            }.awaitAll().filterNotNull().toMap()
        }
        familias = r; cargadas = true
    }

    fun de(k: String): FontFamily = familias[k] ?: familias["barlow"] ?: FontFamily.Default

    /** Tipografía del título según la estética del artículo (REV_FT en la web). */
    fun titulo(ft: String): FontFamily = de(when (ft) {
        "editorial" -> "playfair"; "maquina" -> "mono"; "manuscrita" -> "caveat"; "afiche" -> "abril"; "poster" -> "anton"
        "retro" -> "shrikhand"; "burbuja" -> "rampart"; "glitch" -> "glitch"; "bloque" -> "archivo"; else -> "condensada"
    })

    /** Letras del diseño avanzado (DZ_TF en la web). */
    fun diseno(k: String): FontFamily? = if (k.isEmpty()) null else de(when (k) { "sistema" -> "barlow"; else -> k })
}
