package rr.revista

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RECORTE = listOf(
    Triple(0xFF141414, 0xFFFFFFFF, "archivo"), Triple(0xFFFFFFFF, 0xFF141414, "playfair"), Triple(0xFFFF3D8B, 0xFF141414, "anton"),
    Triple(0xFFF2C230, 0xFF141414, "abril"), Triple(0xFFE3242B, 0xFFFFFFFF, "mono"), Triple(0xFFF1E9D8, 0xFF141414, "lora")
)

/** Collage del diseño avanzado: cada recorte en su lugar (en % del lienzo), con su giro, tamaño y transparencia. */
@Composable
fun Collage(items: List<ItemCollage>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.clipToBounds()) {
        val ancho = maxWidth
        val orden = items.sortedBy { it.z }
        Layout(content = { orden.forEach { Recorte(it, ancho) } }, modifier = Modifier.fillMaxSize()) { ms, c ->
            val ps = ms.map { it.measure(Constraints()) }
            layout(c.maxWidth, c.maxHeight) {
                ps.forEachIndexed { i, p -> val it = orden[i]; p.place((c.maxWidth * it.x / 100 - p.width / 2.0).toInt(), (c.maxHeight * it.y / 100 - p.height / 2.0).toInt()) }
            }
        }
    }
}

@Composable
private fun Recorte(it: ItemCollage, lienzo: Dp) {
    val w = lienzo * (it.w / 100).toFloat()
    val base = Modifier.graphicsLayer { rotationZ = it.r.toFloat(); alpha = (it.o / 100).toFloat() }
    val c = it.c?.let(::col)
    val letra = (lienzo.value * it.w / 6 / 100).sp
    when (it.k) {
        "img" -> {
            val img = imagen(it.s)
            val cuadrada = it.cut in setOf("circulo", "estrella", "corazon")
            val ar = when { cuadrada -> 1f; it.cut == "ovalo" -> 0.75f; img != null -> img.width.toFloat() / img.height; else -> 1.3f }
            val forma = when (it.cut) { "roto" -> Formas.roto; "circulo" -> CircleShape; "ovalo" -> Formas.ovalo; "estrella" -> Formas.estrella; "corazon" -> Formas.corazon; else -> RoundedCornerShape(0.dp) }
            Box(base.width(w).then(if (it.cut == "polaroid") Modifier.shadow(8.dp).background(Color(0xFFFBFAF6)).padding(start = w * .04f, end = w * .04f, top = w * .04f, bottom = w * .16f) else if (it.cut == "rect") Modifier.shadow(6.dp) else Modifier)) {
                Box(Modifier.fillMaxWidth().aspectRatio(ar).clip(forma).background(Color(0x33FFFFFF))) {
                    if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
        }
        "txt" -> {
            if (it.v == "recorte") {
                var n = 0
                Column(base, horizontalAlignment = Alignment.CenterHorizontally) {
                    it.t.split('\n').forEach { linea ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            linea.forEach { ch ->
                                if (ch == ' ') Spacer(Modifier.width((letra.value * .4f).dp)) else {
                                    val p = RECORTE[(n * 7 + ch.code) % RECORTE.size]; val g = ((n++ * 37) % 11) - 5
                                    Text(ch.toString(), color = col(p.second), fontFamily = Fuentes.de(p.third), fontWeight = FontWeight.Black, fontSize = letra,
                                        modifier = Modifier.padding(1.dp).rotate(g.toFloat()).shadow(2.dp).background(col(p.first)).padding(horizontal = (letra.value * .18f).dp, vertical = (letra.value * .06f).dp))
                                }
                            }
                        }
                    }
                }
            } else {
                val (fam, color, fondo) = when (it.v) {
                    "titular" -> Triple("anton", c ?: Color(0xFF141414), Color(0xFFF2C230))
                    "maquina" -> Triple("mono", c ?: Color(0xFF141414), Color.White)
                    "marcador" -> Triple("caveat", c ?: Color(0xFFFF3D8B), Color.Transparent)
                    "neon" -> Triple("shrikhand", Color.White, Color.Transparent)
                    else -> Triple("archivo", Color.White, c ?: Color(0xFF141414))
                }
                val brillo = if (it.v == "neon") Shadow(c ?: Color(0xFF29D3F0), Offset.Zero, letra.value * 0.6f) else null
                Text(if (it.v == "titular") it.t.uppercase() else it.t, color = color, fontFamily = Fuentes.de(fam), fontSize = letra, textAlign = TextAlign.Center,
                    style = TextStyle(shadow = brillo, lineHeight = letra * 1.05f),
                    modifier = base.background(fondo).padding(horizontal = (letra.value * .3f).dp, vertical = (letra.value * .08f).dp))
            }
        }
        "stk" -> {
            val g = Diseno.GLIFOS[it.v]
            if (g != null) Text(g, color = c ?: Color(0xFFFF3D8B), fontFamily = Fuentes.de("simbolos"), fontSize = (lienzo.value * it.w * .8f / 100).sp, modifier = base)
            else {
                val forma = when (it.v) { "circulo" -> CircleShape; "etiqueta" -> RoundedCornerShape(8.dp); else -> Formas.estallido }
                Box(base.width(w).aspectRatio(if (it.v == "etiqueta") 2.6f else 1f).clip(forma).background(c ?: Color(0xFFF2C230))
                    .then(if (it.v == "circulo") Modifier.border(3.dp, Color(0x8C000000), CircleShape) else Modifier).padding(w * .12f), contentAlignment = Alignment.Center) {
                    Text((it.t.ifEmpty { "¡Nuevo!" }).uppercase(), color = Color(0xFF141414), fontFamily = Fuentes.de("archivo"), fontSize = (lienzo.value * it.w / 7 / 100).sp, textAlign = TextAlign.Center, lineHeight = (lienzo.value * it.w / 7 / 100 * 1.05f).sp)
                }
            }
        }
        "tape" -> Box(base.width(w).aspectRatio(3.6f).clip(Formas.cinta).background(col(Diseno.CINTAS[it.v] ?: 0xD9EDE5CF)))
        else -> {
            val cc = c ?: Color(0xFFFF3D8B)
            when (it.v) {
                "circulo" -> Box(base.width(w).aspectRatio(1f).clip(CircleShape).background(cc))
                "franja" -> Box(base.width(w).aspectRatio(5f).background(cc))
                "marco" -> Box(base.width(w).aspectRatio(.75f).border(w * .08f, cc))
                else -> Canvas(base.width(w).aspectRatio(if (it.v == "rayas" || it.v == "cuadros") 1.4f else 1f)) { formaCollage(it.v, cc) }
            }
        }
    }
}
