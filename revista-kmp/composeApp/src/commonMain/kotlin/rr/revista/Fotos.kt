package rr.revista

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/* Editor de fotos como Instagram: formato y encuadre, zoom, rotar, voltear, filtros y ajustes
   (brillo, contraste, saturación, calidez, viñeta). Todo se aplica en el teléfono antes de
   subir la foto, así se ve igual en la app y en la página web. */

val FORMATOS_FOTO = listOf("orig" to "Original", "1x1" to "Cuadrada", "4x5" to "Vertical", "16x9" to "Horizontal", "9x16" to "Historia", "3x2" to "Clásica")
private val PROPORCION = mapOf("1x1" to 1f, "4x5" to .8f, "16x9" to 16f / 9f, "9x16" to 9f / 16f, "3x2" to 1.5f)

/** Filtros listos (como los de Instagram), hechos con los mismos ajustes. */
private data class Filtro(val nombre: String, val brillo: Float = 0f, val contraste: Float = 0f, val saturacion: Float = 0f, val calidez: Float = 0f, val sepia: Boolean = false, val vineta: Float = 0f, val tinte: Triple<Float, Float, Float>? = null)
private val FILTROS = listOf(
    "normal" to Filtro("Normal"),
    "clarendon" to Filtro("Clarendon", contraste = .2f, saturacion = .25f, calidez = -.1f),
    "juno" to Filtro("Juno", contraste = .1f, saturacion = .15f, calidez = .25f),
    "lark" to Filtro("Lark", brillo = .08f, contraste = -.05f, saturacion = -.1f),
    "gingham" to Filtro("Gingham", brillo = .05f, contraste = -.2f, saturacion = -.3f, calidez = .1f),
    "valencia" to Filtro("Valencia", brillo = .04f, contraste = -.05f, calidez = .3f),
    "frio" to Filtro("Frío", saturacion = -.05f, calidez = -.35f),
    "y2k" to Filtro("Y2K", contraste = .15f, saturacion = .4f, tinte = Triple(14f, -4f, 22f)),
    "grunge" to Filtro("Grunge", contraste = .3f, saturacion = -.4f, calidez = .15f, vineta = .55f),
    "vintage" to Filtro("Vintage", brillo = .03f, contraste = -.1f, sepia = true, vineta = .35f),
    "byn" to Filtro("B y N", contraste = .15f, saturacion = -1f),
    "noir" to Filtro("Noir", brillo = -.05f, contraste = .45f, saturacion = -1f, vineta = .45f),
)

/** Foto elegida con todos sus ajustes; «lista()» la deja procesada para subir. */
class FotoElegida(val foto: Foto) {
    var formato by mutableStateOf("orig")
    var encuadreX by mutableFloatStateOf(.5f)
    var encuadreY by mutableFloatStateOf(.5f)
    var zoom by mutableFloatStateOf(1f)
    var giro by mutableIntStateOf(0)
    var espejo by mutableStateOf(false)
    var filtro by mutableStateOf("normal")
    var brillo by mutableFloatStateOf(0f)
    var contraste by mutableFloatStateOf(0f)
    var saturacion by mutableFloatStateOf(0f)
    var calidez by mutableFloatStateOf(0f)
    var vineta by mutableFloatStateOf(0f)

    /** Compatibilidad con el encuadre de antes (un solo eje). */
    var encuadre: Float get() = encuadreX; set(v) { encuadreX = v; encuadreY = v }

    val proporcion: Float get() = PROPORCION[formato] ?: 0f
    val sinCambios: Boolean get() = formato == "orig" && zoom == 1f && giro == 0 && !espejo && filtro == "normal" && brillo == 0f && contraste == 0f && saturacion == 0f && calidez == 0f && vineta == 0f
    private val f: Filtro get() = FILTROS.firstOrNull { it.first == filtro }?.second ?: Filtro("Normal")
    val vinetaTotal: Float get() = (vineta + f.vineta).coerceIn(0f, .9f)

    /** Matriz de color 4×5 (valores 0–255 en la última columna), igual en Android y en el navegador. */
    fun matriz(): FloatArray = matrizDe(f, brillo, contraste, saturacion, calidez)

    fun restablecer() { formato = "orig"; encuadreX = .5f; encuadreY = .5f; zoom = 1f; giro = 0; espejo = false; filtro = "normal"; brillo = 0f; contraste = 0f; saturacion = 0f; calidez = 0f; vineta = 0f }

    suspend fun procesar(dataUrl: String, max: Int, conColor: Boolean = true): String =
        procesarFoto(dataUrl, giro, espejo, proporcion, zoom, encuadreX, encuadreY, if (conColor) matriz().joinToString(",") else "", if (conColor) vinetaTotal else 0f, max)

    /** La foto final (grande y miniatura) con todo aplicado. */
    suspend fun lista(): Foto = if (sinCambios) foto else Foto(procesar(foto.grande, 2400), procesar(foto.mini, 2400))
}

private fun identidad() = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)

/** a ∘ b: primero b, después a. */
private fun componer(a: FloatArray, b: FloatArray): FloatArray {
    val r = FloatArray(20)
    for (i in 0..3) {
        for (j in 0..3) r[i * 5 + j] = (0..3).sumOf { k -> (a[i * 5 + k] * b[k * 5 + j]).toDouble() }.toFloat()
        r[i * 5 + 4] = (0..3).sumOf { k -> (a[i * 5 + k] * b[k * 5 + 4]).toDouble() }.toFloat() + a[i * 5 + 4]
    }
    return r
}

private fun matrizDe(fl: Filtro, brillo: Float, contraste: Float, saturacion: Float, calidez: Float): FloatArray {
    var m = identidad()
    fun aplicar(x: FloatArray) { m = componer(x, m) }
    fun pasos(b: Float, c: Float, s: Float, w: Float) {
        if (s != 0f) { val v = 1f + s; val lr = .2126f * (1 - v); val lg = .7152f * (1 - v); val lb = .0722f * (1 - v)
            aplicar(floatArrayOf(lr + v, lg, lb, 0f, 0f, lr, lg + v, lb, 0f, 0f, lr, lg, lb + v, 0f, 0f, 0f, 0f, 0f, 1f, 0f)) }
        if (c != 0f) { val v = 1f + c; val o = 128f * (1 - v); aplicar(floatArrayOf(v, 0f, 0f, 0f, o, 0f, v, 0f, 0f, o, 0f, 0f, v, 0f, o, 0f, 0f, 0f, 1f, 0f)) }
        if (b != 0f) { val o = b * 70f; aplicar(floatArrayOf(1f, 0f, 0f, 0f, o, 0f, 1f, 0f, 0f, o, 0f, 0f, 1f, 0f, o, 0f, 0f, 0f, 1f, 0f)) }
        if (w != 0f) aplicar(floatArrayOf(1f, 0f, 0f, 0f, w * 28f, 0f, 1f, 0f, 0f, w * 6f, 0f, 0f, 1f, 0f, -w * 28f, 0f, 0f, 0f, 1f, 0f))
    }
    pasos(fl.brillo, fl.contraste, fl.saturacion, fl.calidez)
    if (fl.sepia) aplicar(floatArrayOf(.393f, .769f, .189f, 0f, 0f, .349f, .686f, .168f, 0f, 0f, .272f, .534f, .131f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
    fl.tinte?.let { (r, g, b) -> aplicar(floatArrayOf(1f, 0f, 0f, 0f, r, 0f, 1f, 0f, 0f, g, 0f, 0f, 1f, 0f, b, 0f, 0f, 0f, 1f, 0f)) }
    pasos(brillo, contraste, saturacion, calidez)
    return m
}

@OptIn(ExperimentalEncodingApi::class)
private fun decodificarDataUrl(u: String): ImageBitmap? = runCatching { decodificarImagen(Base64.decode(u.substringAfter(','))) }.getOrNull()

/** El editor a pantalla completa (se dibuja encima de todo, desde App). */
object EditorFotos {
    var actual by mutableStateOf<FotoElegida?>(null); private set
    private var alTerminar: ((Foto) -> Unit)? = null
    /** Abre el editor. Si das alTerminar, recibe la foto ya procesada al tocar «Listo». */
    fun abrir(f: FotoElegida, alTerminar: ((Foto) -> Unit)? = null) { this.alTerminar = alTerminar; actual = f }
    fun cerrar() { actual = null; alTerminar = null }
    internal suspend fun terminar() { val f = actual ?: return; val cb = alTerminar; if (cb != null) cb(f.lista()); cerrar() }
}

/** Vista previa con solo la geometría (recorte, zoom, giro); el color se ve al instante con un filtro de pantalla. */
@Composable
private fun rememberVistaPrevia(f: FotoElegida, max: Int = 900): ImageBitmap? {
    var img by remember(f) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(f, f.formato, f.zoom, f.giro, f.espejo, f.encuadreX, f.encuadreY) {
        delay(if (img == null) 0 else 60)
        img = decodificarDataUrl(f.procesar(f.foto.mini.ifEmpty { f.foto.grande }, max, conColor = false)) ?: img
    }
    return img
}

@Composable
private fun ImagenConAjustes(img: ImageBitmap, f: FotoElegida, modifier: Modifier) {
    Box(modifier) {
        Image(img, null, contentScale = ContentScale.Fit, colorFilter = ColorFilter.colorMatrix(ColorMatrix(f.matriz())), modifier = Modifier.fillMaxSize())
        if (f.vinetaTotal > 0f) Box(Modifier.fillMaxSize().background(Brush.radialGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = f.vinetaTotal))))
    }
}

/** Tarjeta compacta en el formulario: vista previa, formatos rápidos y botón para el editor completo. */
@Composable
fun EditorFoto(f: FotoElegida, quitar: () -> Unit) {
    val img = rememberVistaPrevia(f, 700)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.widthIn(max = 340.dp).fillMaxWidth().aspectRatio(img?.let { it.width.toFloat() / it.height.coerceAtLeast(1) }?.coerceIn(.5f, 2f) ?: 1.3f)
            .clip(RoundedCornerShape(14.dp)).background(Color(0xFF2A2E36)).mano().clickable { EditorFotos.abrir(f) }) {
            if (img != null) ImagenConAjustes(img, f, Modifier.fillMaxSize()) else CircularProgressIndicator(Modifier.align(Alignment.Center), color = Amarillo)
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(32.dp).clip(CircleShape).background(Color(0x99000000)).clickable(onClick = quitar), contentAlignment = Alignment.Center) { Icono("cerrar", Color.White, 16.dp) }
            Text("Toca para editar", color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp).clip(RoundedCornerShape(999.dp)).background(Color(0x99000000)).padding(horizontal = 10.dp, vertical = 3.dp))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip("Editar foto", true) { EditorFotos.abrir(f) }
            Chip("Rotar", false) { f.giro = (f.giro + 90) % 360 }
            FORMATOS_FOTO.take(4).forEach { (k, n) -> Chip(n, f.formato == k) { f.formato = k; f.encuadreX = .5f; f.encuadreY = .5f } }
        }
    }
}

@Composable
private fun Chip(texto: String, on: Boolean, onClick: () -> Unit) {
    Text(texto, color = if (on) Color(0xFF1B1E23) else TextoClaro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) Amarillo else Color(0x1FFFFFFF)).mano().clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 8.dp))
}

/** Editor completo a pantalla completa. */
@Composable
fun EditorFotosVista() {
    val f = EditorFotos.actual ?: return
    val scope = rememberCoroutineScope()
    var pestana by remember(f) { mutableStateOf("recortar") }
    var guardando by remember(f) { mutableStateOf(false) }
    val img = rememberVistaPrevia(f)
    var tam by remember { mutableStateOf(Offset(1f, 1f)) }
    Column(Modifier.fillMaxSize().background(Color(0xFF0B0C0F)).clickable(androidx.compose.foundation.interaction.MutableInteractionSource(), null) {}) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Enlace("Cancelar", Tenue) { EditorFotos.cerrar() }
            Text("Editar foto", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
            Enlace("Restablecer", Tenue) { f.restablecer() }
            Spacer(Modifier.width(6.dp))
            Boton("Listo", ocupado = guardando) { scope.launch { guardando = true; runCatching { EditorFotos.terminar() }.onFailure { Aviso.mostrar(it.message ?: "No se pudo procesar la foto.", true) }; guardando = false } }
        }
        // Vista previa: arrastra para encuadrar, pellizca para acercar, doble toque para volver
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
            if (img == null) CircularProgressIndicator(color = Amarillo)
            else {
                val r = img.width.toFloat() / img.height.coerceAtLeast(1)
                ImagenConAjustes(img, f, Modifier.widthIn(max = 900.dp).aspectRatio(r).clip(RoundedCornerShape(6.dp))
                    .onSizeChanged { tam = Offset(it.width.toFloat().coerceAtLeast(1f), it.height.toFloat().coerceAtLeast(1f)) }
                    .pointerInput(f) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            f.zoom = (f.zoom * zoom).coerceIn(1f, 4f)
                            // mover la foto hacia la derecha muestra lo que estaba a la izquierda
                            f.encuadreX = (f.encuadreX - pan.x / tam.x / f.zoom).coerceIn(0f, 1f)
                            f.encuadreY = (f.encuadreY - pan.y / tam.y / f.zoom).coerceIn(0f, 1f)
                        }
                    }
                    .pointerInput(f) { detectTapGestures(onDoubleTap = { f.zoom = 1f; f.encuadreX = .5f; f.encuadreY = .5f }) }
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp)))
            }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF16181D)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("recortar" to "Recortar", "filtros" to "Filtros", "ajustes" to "Ajustes").forEach { (k, n) -> Chip(n, pestana == k) { pestana = k } }
            }
            when (pestana) {
                "filtros" -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FILTROS.forEach { (k, fl) ->
                        val on = f.filtro == k
                        Column(Modifier.width(78.dp).mano().clickable { f.filtro = k }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).border(if (on) 3.dp else 0.dp, Amarillo, RoundedCornerShape(12.dp))) {
                                if (img != null) {
                                    Image(img, null, contentScale = ContentScale.Crop, colorFilter = ColorFilter.colorMatrix(ColorMatrix(matrizDe(fl, 0f, 0f, 0f, 0f))), modifier = Modifier.fillMaxSize())
                                    if (fl.vineta > 0f) Box(Modifier.fillMaxSize().background(Brush.radialGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = fl.vineta))))
                                }
                            }
                            Text(fl.nombre, fontSize = 12.sp, color = if (on) Amarillo else TextoClaro, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                "ajustes" -> Column(Modifier.heightIn(max = 260.dp).padding(end = 4.dp)) {
                    Deslizador("Brillo", f.brillo, -1f..1f) { f.brillo = it }
                    Deslizador("Contraste", f.contraste, -1f..1f) { f.contraste = it }
                    Deslizador("Saturación", f.saturacion, -1f..1f) { f.saturacion = it }
                    Deslizador("Calidez", f.calidez, -1f..1f) { f.calidez = it }
                    Deslizador("Viñeta", f.vineta, 0f..1f) { f.vineta = it }
                }
                else -> {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FORMATOS_FOTO.forEach { (k, n) -> Chip(n, f.formato == k) { f.formato = k; f.encuadreX = .5f; f.encuadreY = .5f } }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Boton("Girar a la izquierda", principal = false) { f.giro = (f.giro + 270) % 360 }
                        Boton("Girar a la derecha", principal = false) { f.giro = (f.giro + 90) % 360 }
                        Boton("Voltear", principal = false) { f.espejo = !f.espejo }
                    }
                    Deslizador("Zoom", f.zoom, 1f..4f, formato = { "${(it * 100).toInt()} %" }) { f.zoom = it }
                    Text("Arrastra la foto para encuadrar y pellizca para acercar. Doble toque para volver.", color = Tenue, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun Deslizador(nombre: String, valor: Float, rango: ClosedFloatingPointRange<Float>, formato: (Float) -> String = { v -> (if (v > 0) "+" else "") + (v * 100).toInt() }, cambiar: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(nombre, fontSize = 14.sp, modifier = Modifier.width(92.dp))
        Slider(valor, cambiar, valueRange = rango, modifier = Modifier.weight(1f).height(36.dp),
            colors = SliderDefaults.colors(thumbColor = Amarillo, activeTrackColor = Amarillo, inactiveTrackColor = Color(0x33FFFFFF)))
        Text(formato(valor), fontSize = 13.sp, color = Tenue, modifier = Modifier.width(52.dp).padding(start = 6.dp))
    }
}

/** Foto publicada: si es muy alta o muy ancha se recorta un poco; al tocarla se ve completa. */
@Composable
fun FotoPublicada(url: String, modifier: Modifier = Modifier) {
    val img = imagen(url, Supa.miniatura(url))
    val natural = img?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: 1.25f
    Box(modifier.fillMaxWidth().heightIn(max = 640.dp).aspectRatio(natural.coerceIn(.56f, 1.91f)).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A2E36)).mano().clickable { Visor.url = url }) {
        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

/** Ver una foto en grande, con zoom (pellizcar) y moverla; doble toque para acercar o volver. */
object Visor { var url by mutableStateOf<String?>(null) }

@Composable
fun VisorVista() {
    val u = Visor.url ?: return
    val img = imagen(u, Supa.miniatura(u))
    var escala by remember(u) { mutableFloatStateOf(1f) }
    var mov by remember(u) { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxSize().background(Color(0xF5000000))
        .pointerInput(u) { detectTransformGestures { _, pan, zoom, _ -> escala = (escala * zoom).coerceIn(1f, 5f); mov = if (escala == 1f) Offset.Zero else mov + pan } }
        .pointerInput(u) { detectTapGestures(onDoubleTap = { if (escala > 1f) { escala = 1f; mov = Offset.Zero } else escala = 2.5f }, onTap = { if (escala == 1f) Visor.url = null }) },
        contentAlignment = Alignment.Center) {
        if (img != null) Image(img, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = escala; scaleY = escala; translationX = mov.x; translationY = mov.y })
        Box(Modifier.align(Alignment.TopEnd).padding(16.dp).size(44.dp).clip(CircleShape).background(Color(0x66FFFFFF)).clickable { Visor.url = null }, contentAlignment = Alignment.Center) { Icono("cerrar", Color.White) }
    }
}
