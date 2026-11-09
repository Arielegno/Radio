package rr.revista

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* Fotos como en Instagram: se elige el formato (cuadrada, vertical, horizontal) y qué parte
   se ve arrastrándola. El recorte se hace en el teléfono antes de subirla, así se ve igual
   en la app y en la página web. Las fotos muy altas o muy anchas se muestran recortadas
   en el muro y se ven completas tocándolas (con zoom). */

val FORMATOS_FOTO = listOf("orig" to "Original", "1x1" to "Cuadrada", "4x5" to "Vertical", "16x9" to "Horizontal")
private val PROPORCION = mapOf("1x1" to 1f, "4x5" to .8f, "16x9" to 16f / 9f)

/** Foto elegida con su formato y su encuadre, lista para subir. */
class FotoElegida(val foto: Foto) {
    var formato by mutableStateOf("orig")
    var encuadre by mutableFloatStateOf(.5f)

    /** Recorta la foto según el formato elegido (o la deja igual si es «Original»). */
    suspend fun lista(): Foto {
        val r = PROPORCION[formato] ?: return foto
        return Foto(recortarFoto(foto.grande, r, encuadre, 4096), recortarFoto(foto.mini, r, encuadre, 4096))
    }
}

/** Vista previa con los formatos para elegir; se arrastra para escoger qué parte se ve. */
@Composable
fun EditorFoto(f: FotoElegida, quitar: () -> Unit) {
    val img = imagen(f.foto.mini, f.foto.grande)
    val natural = img?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: 1f
    val r = PROPORCION[f.formato] ?: natural.coerceIn(.8f, 1.91f)
    val horizontal = natural > r   // sobra imagen a los lados (si no, arriba y abajo)
    var tam by remember { mutableStateOf(1f) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(r).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A2E36))
            .onSizeChanged { tam = (if (horizontal) it.width else it.height).toFloat().coerceAtLeast(1f) }
            .pointerInput(f.formato, horizontal) {
                if (f.formato != "orig") detectDragGestures { cambio, d ->
                    cambio.consume()
                    f.encuadre = (f.encuadre - (if (horizontal) d.x else d.y) / tam).coerceIn(0f, 1f)
                }
            }) {
            val b = f.encuadre * 2 - 1
            if (img != null) Image(img, null, contentScale = ContentScale.Crop,
                alignment = if (horizontal) BiasAlignment(b, 0f) else BiasAlignment(0f, b), modifier = Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(30.dp).clip(CircleShape).background(Color(0x99000000)).clickable(onClick = quitar), contentAlignment = Alignment.Center) { Icono("cerrar", Color.White, 16.dp) }
            if (f.formato != "orig") Text("Arrastra para encuadrar", color = Color.White, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp).clip(RoundedCornerShape(999.dp)).background(Color(0x99000000)).padding(horizontal = 8.dp, vertical = 2.dp))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FORMATOS_FOTO.forEach { (k, n) ->
                val on = f.formato == k
                Text(n, color = if (on) Color(0xFF1B1E23) else TextoClaro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) Amarillo else Color(0x14FFFFFF)).clickable { f.formato = k; f.encuadre = .5f }.padding(horizontal = 12.dp, vertical = 7.dp))
            }
        }
    }
}

/** Foto publicada: si es muy alta o muy ancha se recorta (como el feed de Instagram); al tocarla se ve completa. */
@Composable
fun FotoPublicada(url: String, modifier: Modifier = Modifier) {
    val img = imagen(url, Supa.miniatura(url))
    val natural = img?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: 1.25f
    Box(modifier.fillMaxWidth().aspectRatio(natural.coerceIn(.8f, 1.91f)).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A2E36)).clickable { Visor.url = url }) {
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
