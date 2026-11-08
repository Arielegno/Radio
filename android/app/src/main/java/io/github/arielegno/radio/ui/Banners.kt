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
import androidx.compose.ui.graphics.Shadow
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
    "punk" to "Punk · recortes", "y2k" to "Y2K · años 2000", "grunge" to "Grunge · spray", "vidrio" to "Translúcido", "senal" to "Señal de ruta",
    "neon" to "Neón", "atardecer" to "Atardecer", "vinilo" to "Vinilo", "tokio" to "Tokio kawaii", "tabloide" to "Tabloide", "cromo" to "Cromo 2000",
    "vintage" to "Cartel vintage", "teen" to "Teen 2002"
)

fun estiloBanner(): String = RadioApp.prefs.str("banner", "punk").takeIf { s -> ESTILOS_BANNER.any { it.first == s } } ?: "punk"

/** Color de la tinta que va encima de cada estilo (para iconos y botones). */
fun tintaBanner(estilo: String): Color = when (estilo) {
    "punk", "tabloide" -> Color(0xFF141414); "y2k" -> Color(0xFF2D47C4); "tokio" -> Color(0xFFD6336C); "cromo" -> Color(0xFF1F2A44); "vintage" -> Color(0xFF5A3A22); else -> Color.White
}

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
        "neon" -> {
            val c1 = if (variante == 0) ROSA_PUNK else CIAN
            val brillo = TextStyle(shadow = Shadow(c1, Offset(0f, 0f), 28f))
            Box(modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF0B0A14)).drawBehind {
                drawCircle(Brush.radialGradient(listOf(ROSA_PUNK.copy(alpha = 0.45f), Color.Transparent), center = Offset(size.width * 0.15f, 0f), radius = size.width * 0.5f), radius = size.width * 0.5f, center = Offset(size.width * 0.15f, 0f))
                drawCircle(Brush.radialGradient(listOf(CIAN.copy(alpha = 0.4f), Color.Transparent), center = Offset(size.width, size.height), radius = size.width * 0.5f), radius = size.width * 0.5f, center = Offset(size.width, size.height))
            }.border(1.5.dp, c1.copy(alpha = 0.7f), RoundedCornerShape(20.dp)).padding(18.dp)) {
                Column(Modifier.padding(end = if (derecha != null) 52.dp else 0.dp)) {
                    Text(etiqueta.uppercase(), color = c1, fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 2.sp, style = brillo)
                    Text(titulo, color = Color.White, fontSize = tamTitulo, lineHeight = tamTitulo * 1.05f, fontWeight = FontWeight.Black, style = brillo, modifier = Modifier.padding(top = 4.dp))
                    if (sub.isNotEmpty()) Text(sub, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    abajo?.invoke()
                }
                derecha?.invoke(this)
            }
        }
        "atardecer" -> {
            Box(modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(if (variante == 0) listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B), Color(0xFF6A3093)) else listOf(Color(0xFF6A3093), Color(0xFFFF7E5F))))
                .drawBehind {
                    // sol retro con franjas
                    val r = size.height * 0.62f; val c = Offset(size.width - r * 0.7f, size.height * 0.62f)
                    drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFE08A), Color(0xFFFF5F6D)), startY = c.y - r, endY = c.y + r), radius = r, center = c)
                    for (i in 0 until 5) { val y = c.y + r * (0.05f + i * 0.17f); drawRect(Color(0xFF6A3093).copy(alpha = 0.55f), Offset(c.x - r, y), Size(r * 2, 3f + i * 2.5f)) }
                }.padding(18.dp)) {
                Column(Modifier.padding(end = if (derecha != null) 52.dp else 0.dp)) {
                    Text(etiqueta.uppercase(), color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, letterSpacing = 2.sp)
                    TextoDesfasado(titulo, tamTitulo, Color.White, Color(0x88401050), Color.Transparent, Modifier.padding(top = 4.dp), estilo = FontStyle.Italic)
                    if (sub.isNotEmpty()) Text(sub, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    abajo?.invoke()
                }
                derecha?.invoke(this)
            }
        }
        "vinilo" -> {
            Box(modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF0E0E0E)).drawBehind {
                val r = size.height * 0.9f; val c = Offset(size.width + r * 0.25f, size.height / 2)
                drawCircle(Color(0xFF181818), radius = r, center = c)
                var k = r; while (k > r * 0.38f) { drawCircle(Color(0xFF262626), radius = k, center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f)); k -= 7f }
                drawCircle(acento, radius = r * 0.32f, center = c); drawCircle(Color(0xFF0E0E0E), radius = r * 0.05f, center = c)
            }.padding(18.dp)) {
                Column(Modifier.padding(end = if (derecha != null) 52.dp else 72.dp)) {
                    Text("● " + etiqueta.uppercase(), color = acento, fontSize = 12.sp, letterSpacing = 2.sp)
                    Text(titulo, color = Color.White, fontSize = tamTitulo, lineHeight = tamTitulo * 1.05f, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
                    if (sub.isNotEmpty()) Text(sub, color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    abajo?.invoke()
                }
                derecha?.invoke(this)
            }
        }
        "tokio" -> {
            val rosa = Color(0xFFD6336C)
            Box(modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(28.dp)).background(if (variante == 0) Color(0xFFFFD6E8) else Color(0xFFD9F2FF)).drawBehind {
                var y = 10f; var fila = 0
                while (y < size.height) { var x = if (fila % 2 == 0) 10f else 28f; while (x < size.width) { drawCircle(Color.White.copy(alpha = 0.8f), 5f, Offset(x, y)); x += 36f }; y += 18f; fila++ }
            }.border(3.dp, Color.White, RoundedCornerShape(28.dp)).padding(18.dp)) {
                Column(Modifier.padding(end = if (derecha != null) 52.dp else 0.dp)) {
                    Text("✿ " + etiqueta + " ✿", color = rosa, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    TextoDesfasado(titulo, tamTitulo, rosa, Color.White, Color.Transparent, Modifier.padding(top = 4.dp), peso = FontWeight.ExtraBold)
                    if (sub.isNotEmpty()) Text(sub, color = Color(0xFF7A2A4A), fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    abajo?.invoke()
                }
                Text("★", color = Color(0xFFFFC94D), fontSize = 26.sp, modifier = Modifier.align(Alignment.BottomEnd))
                derecha?.invoke(this)
            }
        }
        "tabloide" -> {
            Column(modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(4.dp)).background(Color.White)) {
                Row(Modifier.fillMaxWidth().background(Color(0xFFE3242B)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(if (variante == 0) "¡EXCLUSIVA!" else "ÚLTIMA HORA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                    Text("★ ★ ★", color = Color.White, fontSize = 12.sp)
                }
                Box(Modifier.fillMaxWidth().drawBehind {
                    var y = 0f; while (y < size.height) { drawRect(Color(0x141D4ED8), Offset(0f, y), Size(size.width, 3f)); y += 6f }
                }.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp)) {
                    Column(Modifier.padding(end = if (derecha != null) 52.dp else 0.dp)) {
                        Text(etiqueta.uppercase(), color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.5.sp)
                        Text(titulo.uppercase(), color = Color(0xFF141414), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = tamTitulo, lineHeight = tamTitulo * 1.0f, modifier = Modifier.padding(top = 2.dp))
                        if (sub.isNotEmpty()) Text(sub, color = Color(0xFF333333), fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        abajo?.invoke()
                    }
                    derecha?.invoke(this)
                }
            }
        }
        "cromo" -> {
            val tinta = Color(0xFF1F2A44)
            Box(modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFE9EEF5), Color(0xFF9FB3C8), Color(0xFFF4F7FB), Color(0xFFB8C6D6))))
                .border(2.dp, Brush.linearGradient(listOf(Color.White, Color(0xFF6B7F99), Color.White)), RoundedCornerShape(16.dp)).padding(18.dp)) {
                Column(Modifier.padding(end = if (derecha != null) 52.dp else 0.dp)) {
                    Text("✦ " + etiqueta.uppercase(), color = tinta, fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 2.sp)
                    TextoDesfasado(titulo, tamTitulo, tinta, Color.White, Color(0x552D47C4), Modifier.padding(top = 4.dp), peso = FontWeight.ExtraBold, estilo = FontStyle.Italic)
                    if (sub.isNotEmpty()) Text(sub, color = tinta.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    abajo?.invoke()
                }
                derecha?.invoke(this)
            }
        }
        "vintage" -> {
            val cafe = Color(0xFF5A3A22); val rojo = Color(0xFFB23A2B)
            Box(modifier.fillMaxWidth().padding(vertical = 4.dp).background(Color(0xFFEFE4CC)).padding(5.dp).border(2.dp, cafe).padding(4.dp).border(1.dp, cafe).padding(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(end = if (derecha != null) 52.dp else 0.dp), horizontalAlignment = if (derecha != null) Alignment.Start else Alignment.CenterHorizontally) {
                    Text("— " + etiqueta.uppercase() + " —", color = cafe, fontFamily = FontFamily.Serif, fontSize = 12.sp, letterSpacing = 3.sp)
                    Text(titulo.uppercase(), color = rojo, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = tamTitulo, lineHeight = tamTitulo * 1.02f,
                        textAlign = if (derecha != null) androidx.compose.ui.text.style.TextAlign.Start else androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    if (sub.isNotEmpty()) Text(sub, color = cafe, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    abajo?.invoke()
                }
                derecha?.invoke(this)
            }
        }
        "teen" -> {
            Box(modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(18.dp)).drawBehind {
                drawRect(Color(0xFF3D5AFE))
                val p = Path(); p.moveTo(0f, 0f); p.lineTo(size.width * 0.62f, 0f); p.lineTo(size.width * 0.42f, size.height); p.lineTo(0f, size.height); p.close()
                drawPath(p, Color(0xFFF2711C))
            }.padding(18.dp)) {
                val contorno = TextStyle(shadow = Shadow(Color(0xFF141414), Offset(3f, 3f), 0f))
                Column(Modifier.padding(end = if (derecha != null) 52.dp else 0.dp)) {
                    Text(etiqueta.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp,
                        modifier = Modifier.rotate(-2f).background(Color(0xFF141414), RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 3.dp))
                    Text(titulo, color = Color.White, fontSize = tamTitulo, lineHeight = tamTitulo * 1.05f, fontWeight = FontWeight.Black, style = contorno, modifier = Modifier.padding(top = 8.dp))
                    if (sub.isNotEmpty()) Text(sub, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, style = contorno, modifier = Modifier.padding(top = 6.dp))
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
