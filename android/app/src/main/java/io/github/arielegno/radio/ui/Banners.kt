package io.github.arielegno.radio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.arielegno.radio.RadioApp
import kotlin.random.Random

/** Estilos de los banners de la portada (saludo y «Cerca de»), inspirados en carteles punk, Y2K y grunge. */
val ESTILOS_BANNER = listOf(
    "punk" to "Punk · recortes", "y2k" to "Y2K · años 2000", "grunge" to "Grunge · spray", "vidrio" to "Translúcido", "senal" to "Señal de ruta"
)

fun estiloBanner(): String = RadioApp.prefs.str("banner", "punk").takeIf { s -> ESTILOS_BANNER.any { it.first == s } } ?: "punk"

/** Color de la tinta que va encima de cada estilo (para iconos y botones). */
fun tintaBanner(estilo: String): Color = when (estilo) { "punk" -> Color(0xFF141414); "y2k" -> Color(0xFF2D47C4); else -> Color.White }

private val ROSA_PUNK = Color(0xFFFF3D8B)
private val CIAN = Color(0xFF26C6F0)
private val AZUL_Y2K = Color(0xFF2D47C4)

/** Texto «mal impreso»: dos copias de colores corridas detrás, como en los carteles serigrafiados. */
@Composable
fun TextoDesfasado(
    t: String, tam: TextUnit, color: Color, c1: Color, c2: Color, modifier: Modifier = Modifier,
    peso: FontWeight = FontWeight.Black, familia: FontFamily = FontFamily.SansSerif, estilo: FontStyle = FontStyle.Normal, lineas: Int = 3
) {
    val st = TextStyle(fontSize = tam, lineHeight = tam * 1.05f, fontWeight = peso, fontFamily = familia, fontStyle = estilo)
    Box(modifier) {
        Text(t, style = st, color = c1, maxLines = lineas, overflow = TextOverflow.Ellipsis, modifier = Modifier.offset(3.dp, 3.dp))
        Text(t, style = st, color = c2, maxLines = lineas, overflow = TextOverflow.Ellipsis, modifier = Modifier.offset((-1.5).dp, (-1).dp))
        Text(t, style = st, color = color, maxLines = lineas, overflow = TextOverflow.Ellipsis)
    }
}

private class Pieza(val c: Char, val tipo: Int, val giro: Float)
private val RECORTES = listOf(
    Triple(Color(0xFF141414), Color.White, FontFamily.SansSerif), Triple(Color(0xFFFFFFFF), Color(0xFF141414), FontFamily.Serif),
    Triple(ROSA_PUNK, Color(0xFF141414), FontFamily.SansSerif), Triple(Color(0xFFF4D23C), Color(0xFF141414), FontFamily.Serif),
    Triple(Color(0xFFE53935), Color.White, FontFamily.Monospace)
)

/** Letras recortadas de revista (como una nota de rescate). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Recortes(t: String, semilla: Int, tam: TextUnit = 13.sp) {
    val piezas = remember(t, semilla) { val r = Random(semilla * 31 + t.hashCode()); t.uppercase().map { Pieza(it, r.nextInt(RECORTES.size), r.nextFloat() * 10f - 5f) } }
    FlowRow(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        piezas.forEach { p ->
            if (p.c == ' ') Spacer(Modifier.width(7.dp)) else {
                val (fondo, tinta, fam) = RECORTES[p.tipo]
                Text(p.c.toString(), color = tinta, fontFamily = fam, fontWeight = if (p.tipo % 2 == 0) FontWeight.Black else FontWeight.Bold, fontSize = tam,
                    modifier = Modifier.padding(horizontal = 1.dp).rotate(p.giro).background(fondo).padding(horizontal = 3.dp, vertical = 1.dp))
            }
        }
    }
}

/** Papel con los bordes rotos. */
private fun DrawScope.papelRoto(color: Color, semilla: Int) {
    val r = Random(semilla); val w = size.width; val h = size.height; val paso = 14f
    val p = Path()
    p.moveTo(0f, r.nextFloat() * 8f)
    var x = 0f
    while (x < w) { x = minOf(w, x + paso + r.nextFloat() * 10f); p.lineTo(x, r.nextFloat() * 9f) }
    var y = 0f
    while (y < h) { y = minOf(h, y + paso + r.nextFloat() * 10f); p.lineTo(w - r.nextFloat() * 5f, y) }
    x = w
    while (x > 0f) { x = maxOf(0f, x - paso - r.nextFloat() * 10f); p.lineTo(x, h - r.nextFloat() * 10f) }
    y = h
    while (y > 0f) { y = maxOf(0f, y - paso - r.nextFloat() * 10f); p.lineTo(r.nextFloat() * 5f, y) }
    p.close()
    drawPath(p, Color(0x55000000), alpha = 1f)
    translate(-3f, -3f) { drawPath(p, color) }
}

/** Puntos de trama (halftone) que se hacen chicos hacia afuera. */
private fun DrawScope.trama(color: Color, centro: Offset, radio: Float, paso: Float = 11f) {
    var y = centro.y - radio
    while (y <= centro.y + radio) {
        var x = centro.x - radio
        while (x <= centro.x + radio) {
            val d = (Offset(x, y) - centro).getDistance() / radio
            if (d < 1f && x in 0f..size.width && y in 0f..size.height) drawCircle(color, radius = (1f - d) * paso * 0.45f, center = Offset(x, y))
            x += paso
        }
        y += paso
    }
}

/** Cinta adhesiva pegada en una esquina. */
private fun DrawScope.cinta(centro: Offset, giro: Float) {
    rotate(giro, centro) { drawRect(Color(0xB3EDE5CF), topLeft = Offset(centro.x - 46f, centro.y - 14f), size = Size(92f, 28f)) }
}

/** Código de barras. */
private fun DrawScope.barras(origen: Offset, alto: Float, color: Color, semilla: Int) {
    val r = Random(semilla); var x = origen.x
    repeat(28) { val g = 1.5f + r.nextInt(3) * 1.5f; drawRect(color, Offset(x, origen.y), Size(g, alto)); x += g + 1.5f + r.nextInt(2) * 1.5f }
}

/** Mancha de spray con bordes rugosos. */
private fun DrawScope.franjaSpray(color: Color, semilla: Int) {
    val r = Random(semilla); val w = size.width; val h = size.height
    val arriba = h * 0.18f; val abajo = h * 0.86f
    val p = Path(); p.moveTo(-10f, arriba)
    var x = -10f
    while (x < w + 10f) { x += 18f + r.nextFloat() * 16f; p.lineTo(x, arriba + (r.nextFloat() - 0.5f) * 22f) }
    while (x > -10f) { x -= 18f + r.nextFloat() * 16f; p.lineTo(x, abajo + (r.nextFloat() - 0.5f) * 22f) }
    p.close(); drawPath(p, color)
    repeat(26) {
        val c = Offset(r.nextFloat() * w, if (r.nextBoolean()) arriba - r.nextFloat() * 30f else abajo + r.nextFloat() * 30f)
        drawCircle(color.copy(alpha = 0.5f + r.nextFloat() * 0.5f), radius = 1.5f + r.nextFloat() * 6f, center = c)
    }
    // rayas diagonales en la esquina
    for (i in 0 until 7) { val o = w - 120f + i * 16f; drawLine(Color.White.copy(alpha = 0.18f), Offset(o, 0f), Offset(o + 60f, arriba + 10f), strokeWidth = 5f) }
}

/**
 * Banner de la portada. [variante] cambia los colores para que el saludo y la ubicación no se vean iguales.
 * [derecha] va arriba a la derecha (por ejemplo, compartir); [abajo] debajo del texto (por ejemplo, un botón).
 */
@Composable
fun Banner(
    estilo: String, etiqueta: String, titulo: String, sub: String, variante: Int, modifier: Modifier = Modifier, tamTitulo: TextUnit = 34.sp,
    derecha: (@Composable BoxScope.() -> Unit)? = null, abajo: (@Composable () -> Unit)? = null
) {
    val acento = Amarillo
    when (estilo) {
        "punk" -> {
            val papel = if (variante == 0) Color(0xFFEFE8DA) else Color(0xFFF4D23C)
            Box(modifier.fillMaxWidth().padding(vertical = 8.dp).drawBehind {
                papelRoto(papel, 7 + variante)
                trama(if (variante == 0) ROSA_PUNK.copy(alpha = 0.55f) else Color(0x66141414), Offset(size.width - 30f, size.height - 20f), size.height * 0.75f)
                cinta(Offset(36f, 4f), -18f); cinta(Offset(size.width - 40f, 2f), 14f)
            }.padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp)) {
                Column(Modifier.padding(end = if (derecha != null) 52.dp else 0.dp)) {
                    Recortes(etiqueta, variante)
                    TextoDesfasado(titulo, tamTitulo, Color(0xFF141414), if (variante == 0) ROSA_PUNK else Color(0xFFE53935), CIAN.copy(alpha = 0.7f), Modifier.padding(top = 6.dp))
                    if (sub.isNotEmpty()) Text(sub, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp).rotate(-1f).background(Color(0xFF141414)).padding(horizontal = 6.dp, vertical = 2.dp))
                    abajo?.invoke()
                }
                derecha?.invoke(this)
            }
        }
        "y2k" -> {
            val fondo = if (variante == 0) Color(0xFFEDE6D6) else Color(0xFFDDE4F7)
            Column(modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(6.dp)).background(fondo).border(2.dp, AZUL_Y2K, RoundedCornerShape(6.dp))) {
                Row(Modifier.fillMaxWidth().background(AZUL_Y2K).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (variante == 0) "RADIO_DE_RUTA.EXE" else "UBICACION.EXE", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("▁  ▢  ✕", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                Box(Modifier.fillMaxWidth().drawBehind {
                    barras(Offset(size.width - 120f, size.height - 34f), 22f, Color(0xFF141414), 3 + variante)
                }.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
                    Column(Modifier.padding(end = if (derecha != null) 52.dp else 0.dp)) {
                        Text("✦ " + etiqueta.uppercase(), color = AZUL_Y2K, fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 2.sp)
                        TextoDesfasado(titulo, tamTitulo, AZUL_Y2K, Color(0xFF141414), Color.Transparent, Modifier.padding(top = 4.dp), peso = FontWeight.ExtraBold, estilo = FontStyle.Italic)
                        if (sub.isNotEmpty()) Text(sub, color = Color(0xFF141414), fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, end = 70.dp))
                        abajo?.invoke()
                    }
                    Text("✦", color = AZUL_Y2K, fontSize = 22.sp, modifier = Modifier.align(Alignment.BottomEnd).offset(y = (-28).dp))
                    derecha?.invoke(this)
                }
            }
        }
        "grunge" -> {
            val franja = listOf(Color(0xFFE0489A), Color(0xFFB5D334), Color(0xFFF08A24))[variante % 3]
            Box(modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF15161A)).drawBehind {
                franjaSpray(franja, 11 + variante)
            }.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
                Column(Modifier.padding(end = if (derecha != null) 52.dp else 0.dp)) {
                    Text(etiqueta.uppercase(), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 1.5.sp,
                        modifier = Modifier.rotate(-2f).background(Color(0xFF15161A)).padding(horizontal = 8.dp, vertical = 3.dp))
                    TextoDesfasado(titulo, tamTitulo, Color.White, Color(0xFF15161A), Color.Transparent, Modifier.padding(top = 8.dp))
                    if (sub.isNotEmpty()) Text(sub, color = Color(0xFF15161A), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    abajo?.invoke()
                }
                derecha?.invoke(this)
            }
        }
        "senal" -> {
            val fondo = if (variante == 0) Color(0xE61E6B45) else Color(0xCC1B4F9C)
            Box(modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(fondo).border(3.dp, Color(0x66F4F6F8), RoundedCornerShape(14.dp)).padding(14.dp)) {
                Column(Modifier.padding(end = if (derecha != null) 52.dp else 0.dp)) {
                    Text(etiqueta, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Text(titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = tamTitulo, lineHeight = tamTitulo * 1.08f)
                    if (sub.isNotEmpty()) Text(sub, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    abajo?.invoke()
                }
                derecha?.invoke(this)
            }
        }
        else -> { // translúcido
            Box(modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = if (LocalExtra.current.oscuro) 0.07f else 0.45f))
                .drawBehind {
                    drawCircle(Brush.radialGradient(listOf(acento.copy(alpha = 0.38f), Color.Transparent), center = Offset(size.width, 0f), radius = size.width * 0.6f), radius = size.width * 0.6f, center = Offset(size.width, 0f))
                    drawCircle(Brush.radialGradient(listOf(Rosa.copy(alpha = 0.25f), Color.Transparent), center = Offset(0f, size.height), radius = size.width * 0.5f), radius = size.width * 0.5f, center = Offset(0f, size.height))
                }
                .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.04f))), RoundedCornerShape(24.dp))
                .padding(18.dp)) {
                Column(Modifier.padding(end = if (derecha != null) 52.dp else 0.dp)) {
                    Text(etiqueta.uppercase(), color = LocalExtra.current.tenue, fontSize = 12.sp, letterSpacing = 2.sp)
                    TextoDesfasado(titulo, tamTitulo, MaterialThemeColor(), acento.copy(alpha = 0.85f), Rosa.copy(alpha = 0.55f), Modifier.padding(top = 4.dp))
                    if (sub.isNotEmpty()) Text(sub, color = LocalExtra.current.tenue, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    abajo?.invoke()
                }
                derecha?.invoke(this)
            }
        }
    }
}

@Composable
private fun MaterialThemeColor(): Color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
