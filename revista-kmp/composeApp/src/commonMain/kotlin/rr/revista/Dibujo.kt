package rr.revista

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun col(c: Long): Color = Color(c.toInt())

/** Forma a partir de puntos en porcentaje (igual que los clip-path de la web). */
fun formaPct(vararg p: Float): Shape = GenericShape { s, _ ->
    moveTo(s.width * p[0] / 100f, s.height * p[1] / 100f)
    var i = 2; while (i + 1 < p.size) { lineTo(s.width * p[i] / 100f, s.height * p[i + 1] / 100f); i += 2 }
    close()
}

object Formas {
    val roto = formaPct(0f, 3f, 8f, 0f, 17f, 4f, 26f, 1f, 35f, 3f, 46f, 0f, 57f, 4f, 68f, 1f, 79f, 3f, 90f, 0f, 100f, 2f, 98f, 14f, 100f, 27f, 97f, 40f, 100f, 53f, 98f, 66f, 100f, 79f, 97f, 90f, 100f, 100f,
        89f, 97f, 78f, 100f, 66f, 96f, 55f, 100f, 44f, 97f, 33f, 100f, 22f, 96f, 11f, 100f, 0f, 97f, 2f, 85f, 0f, 72f, 3f, 60f, 0f, 47f, 2f, 34f, 0f, 21f, 3f, 10f)
    val estrella = formaPct(50f, 0f, 61f, 35f, 98f, 35f, 68f, 57f, 79f, 91f, 50f, 70f, 21f, 91f, 32f, 57f, 2f, 35f, 39f, 35f)
    val corazon = formaPct(50f, 18f, 61f, 5f, 75f, 1f, 90f, 7f, 99f, 22f, 97f, 40f, 85f, 58f, 50f, 95f, 15f, 58f, 3f, 40f, 1f, 22f, 10f, 7f, 25f, 1f, 39f, 5f)
    val estallido = formaPct(50f, 0f, 58f, 15f, 72f, 5f, 74f, 21f, 90f, 16f, 85f, 32f, 100f, 35f, 89f, 48f, 100f, 60f, 85f, 65f, 92f, 82f, 75f, 79f, 72f, 96f, 59f, 86f, 50f, 100f,
        41f, 86f, 28f, 96f, 25f, 79f, 8f, 82f, 15f, 65f, 0f, 60f, 11f, 48f, 0f, 35f, 15f, 32f, 10f, 16f, 26f, 21f, 28f, 5f, 42f, 15f)
    val cinta = formaPct(2f, 0f, 98f, 0f, 100f, 15f, 97f, 30f, 100f, 50f, 97f, 70f, 100f, 85f, 98f, 100f, 2f, 100f, 0f, 85f, 3f, 70f, 0f, 50f, 3f, 30f, 0f, 15f)
    val papelPanel = formaPct(0f, 1f, 6f, 0f, 14f, 1.2f, 25f, 0f, 37f, 1f, 49f, 0f, 61f, 1.3f, 73f, 0f, 85f, 1f, 94f, 0f, 100f, 1f, 99.4f, 50f, 100f, 99f,
        92f, 100f, 81f, 98.8f, 69f, 100f, 57f, 99f, 45f, 100f, 33f, 98.9f, 21f, 100f, 9f, 99f, 0f, 100f, 0.6f, 50f)
    val ovalo = GenericShape { s, _ -> addOval(androidx.compose.ui.geometry.Rect(0f, 0f, s.width, s.height)) }
}

/** Degradado con ángulo como en CSS (linear-gradient(160deg, …)). */
fun DrawScope.degradado(colores: List<Color>, grados: Float) {
    if (colores.size == 1) { drawRect(colores[0]); return }
    val a = grados * PI.toFloat() / 180f; val dx = sin(a); val dy = -cos(a)
    val l = abs(size.width / 2 * dx) + abs(size.height / 2 * dy); val c = center
    drawRect(Brush.linearGradient(colores, start = Offset(c.x - dx * l, c.y - dy * l), end = Offset(c.x + dx * l, c.y + dy * l)))
}

private fun DrawScope.puntos(color: Color, paso: Float, radio: Float, desfase: Boolean = false) {
    var y = paso / 2; var fila = 0
    while (y < size.height + paso) { var x = if (desfase && fila % 2 == 1) paso else paso / 2; while (x < size.width + paso) { drawCircle(color, radio, Offset(x, y)); x += paso }; y += paso; fila++ }
}

private fun DrawScope.rayas(color: Color, ancho: Float, grados: Float) {
    rotate(grados) { var x = -size.height * 2; while (x < size.width * 2) { drawRect(color, Offset(x, -size.height), Size(ancho, size.height * 3)); x += ancho * 2 } }
}

private fun DrawScope.mancha(color: Color, cx: Float, cy: Float, r: Float) =
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center = Offset(size.width * cx, size.height * cy), radius = r), r, Offset(size.width * cx, size.height * cy))

/** Fondo de una estética de la Revista. */
fun DrawScope.fondoTema(e: Estilo, t: Tema) {
    val c = t.fondo.map(::col)
    degradado(c, 160f)
    when (e.th) {
        "y2k" -> puntos(Color(0x47FF6428), 22f, 3f)
        "tokio" -> rayas(Color(0x33FFC6DD), 24f, 45f)
        "punk", "vintage" -> { val r = Random(7); repeat(1400) { drawCircle(Color.White.copy(alpha = if (e.th == "punk") 0.05f else 0.08f), 1.1f, Offset(r.nextFloat() * size.width, r.nextFloat() * size.height)) } }
        "neon" -> { mancha(Color(0x55FF3D8B), 0.1f, 0.05f, size.width * 0.7f); mancha(Color(0x4429D3F0), 0.95f, 0.6f, size.width * 0.6f) }
    }
}

/** Fondo del diseño avanzado (DZ_BG en la web). */
fun DrawScope.fondoDiseno(d: Diseno) {
    when (d.bg) {
        "color" -> when (d.gt) {
            "rad" -> drawRect(Brush.radialGradient(listOf(col(d.c1), col(d.c2)), center = Offset(size.width * .3f, size.height * .2f), radius = maxOf(size.width, size.height)))
            "con" -> drawRect(Brush.sweepGradient(listOf(col(d.c1), col(d.c2), col(d.c1)), center = center))
            else -> degradado(listOf(col(d.c1), col(d.c2)), d.ga.toFloat())
        }
        "aurora" -> degradado(listOf(Color(0xFF0F2027), Color(0xFF2C5364), Color(0xFF3ECF8E)), 160f)
        "atardecer" -> degradado(listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B), Color(0xFF6A3093)), 180f)
        "neon" -> { drawRect(Color(0xFF0B0A14)); mancha(Color(0xFFFF3D8B), .2f, .2f, size.width * .55f); mancha(Color(0xFF29D3F0), .8f, .7f, size.width * .55f) }
        "oceano" -> degradado(listOf(Color(0xFF1A2A6C), Color(0xFF2A7AB0), Color(0xFFA8E6CF)), 200f)
        "fuego" -> degradado(listOf(Color(0xFF200122), Color(0xFF6F0000), Color(0xFFFF8A3D)), 10f)
        "noche" -> { degradado(listOf(Color(0xFF0B1026), Color(0xFF1C2541)), 180f); val r = Random(3); repeat(160) { drawCircle(Color.White.copy(alpha = .4f + r.nextFloat() * .6f), .6f + r.nextFloat() * 1.4f, Offset(r.nextFloat() * size.width, r.nextFloat() * size.height)) } }
        "grunge" -> { drawRect(Color(0xFF15161A)); mancha(Color(0xD9E0489A), .3f, .4f, size.width * .7f); mancha(Color(0xB3B5D334), .75f, .65f, size.width * .6f) }
        "vinilo" -> { drawRect(Color(0xFF111111)); var r = 2f; while (r < maxOf(size.width, size.height)) { drawCircle(Color(0xFF1C1C1C), r, center, style = Stroke(2f)); r += 4f } }
        "rayas" -> { drawRect(Color(0xFF1B1E23)); rayas(Color(0xFFF2C230), 18f, 45f) }
        "menta" -> degradado(listOf(Color(0xFFD4FC79), Color(0xFF96E6A1)), 135f)
        "pastel" -> degradado(listOf(Color(0xFFF6D5F7), Color(0xFFFBE9D7), Color(0xFFD4F1F4), Color(0xFFE2D1F9)), 120f)
        "cromo" -> degradado(listOf(Color(0xFFE9EEF5), Color(0xFF9FB3C8), Color(0xFFF4F7FB)), 160f)
        "y2k" -> degradado(listOf(Color(0xFFB6F23A), Color(0xFF29D3F0), Color(0xFFFF8A3D)), 135f)
        "tokio" -> { drawRect(Color(0xFFFFD6E8)); puntos(Color.White, 24f, 5f) }
        "trama" -> { drawRect(Color(0xFFF1E9D8)); puntos(Color(0xFFFF3D8B), 14f, 4.2f) }
        "cuadros" -> { drawRect(Color(0xFFF4F4F4)); val p = 40f; var y = 0f; var f = 0; while (y < size.height) { var x = if (f % 2 == 0) 0f else p; while (x < size.width) { drawRect(Color(0xFF111111), Offset(x, y), Size(p, p)); x += p * 2 }; y += p; f++ } }
        "periodico" -> { drawRect(Color(0xFFEFE8DA)); var y = 22f; while (y < size.height) { drawRect(Color(0x12000000), Offset(0f, y), Size(size.width, 1f)); y += 22f } }
    }
}

fun DrawScope.capasEncima(d: Diseno) {
    if (d.dim > 0) drawRect(Color.Black.copy(alpha = d.dim / 100f))
    if (d.vin) drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xA6000000)), center = center, radius = maxOf(size.width, size.height) * .75f))
    if (d.grain) { val r = Random(11); repeat(2500) { drawCircle(if (r.nextBoolean()) Color(0x14FFFFFF) else Color(0x14000000), 1f, Offset(r.nextFloat() * size.width, r.nextFloat() * size.height)) } }
}

/** Trama, rayas y cuadros para las formas del collage. */
fun DrawScope.formaCollage(v: String, c: Color) {
    when (v) {
        "trama" -> { val paso = size.width / 8f; var y = paso / 2; while (y < size.height) { var x = paso / 2; while (x < size.width) { val dd = (Offset(x, y) - center).getDistance() / (size.width / 2); if (dd < 1f) drawCircle(c.copy(alpha = (1f - dd).coerceIn(0f, 1f)), paso * .32f, Offset(x, y)); x += paso }; y += paso } }
        "rayas" -> clipRect { rayas(c, size.width * .08f, 45f) }
        "cuadros" -> { val p = size.width / 5f; var y = 0f; var f = 0; while (y < size.height) { var x = if (f % 2 == 0) 0f else p; while (x < size.width) { drawRect(c, Offset(x, y), Size(p, p)); x += p * 2 }; y += p; f++ } }
        "spray" -> drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), center = center, radius = size.width / 2 * .68f / .5f * .5f), size.width / 2)
    }
}

/* ---------- Iconos dibujados (no dependen de ninguna letra) ---------- */
@Composable
fun Icono(tipo: String, color: Color, tam: Dp = 22.dp) {
    Canvas(Modifier.size(tam)) {
        val w = size.width; val s = Stroke(w * 0.09f, cap = StrokeCap.Round)
        when (tipo) {
            "corazon" -> { val p = Path().apply { moveTo(w * .5f, w * .86f); cubicTo(w * .05f, w * .55f, w * .05f, w * .12f, w * .5f, w * .3f); cubicTo(w * .95f, w * .12f, w * .95f, w * .55f, w * .5f, w * .86f); close() }; drawPath(p, color) }
            "globo" -> { drawRoundRect(color, Offset(w * .1f, w * .15f), Size(w * .8f, w * .55f), CornerRadius(w * .15f), style = s); drawLine(color, Offset(w * .32f, w * .7f), Offset(w * .22f, w * .88f), s.width, StrokeCap.Round) }
            "casa" -> { val p = Path().apply { moveTo(w * .15f, w * .48f); lineTo(w * .5f, w * .16f); lineTo(w * .85f, w * .48f); moveTo(w * .25f, w * .42f); lineTo(w * .25f, w * .85f); lineTo(w * .75f, w * .85f); lineTo(w * .75f, w * .42f) }; drawPath(p, color, style = s) }
            "pelicula" -> { drawRoundRect(color, Offset(w * .12f, w * .2f), Size(w * .76f, w * .6f), CornerRadius(w * .08f), style = s); val p = Path().apply { moveTo(w * .42f, w * .36f); lineTo(w * .64f, w * .5f); lineTo(w * .42f, w * .64f); close() }; drawPath(p, color) }
            "imagen" -> { drawRoundRect(color, Offset(w * .12f, w * .18f), Size(w * .76f, w * .64f), CornerRadius(w * .08f), style = s); val p = Path().apply { moveTo(w * .2f, w * .74f); lineTo(w * .42f, w * .5f); lineTo(w * .56f, w * .64f); lineTo(w * .66f, w * .54f); lineTo(w * .8f, w * .74f) }; drawPath(p, color, style = s); drawCircle(color, w * .06f, Offset(w * .66f, w * .36f)) }
            "menu" -> { listOf(.3f, .5f, .7f).forEach { y -> drawLine(color, Offset(w * .2f, w * y), Offset(w * .8f, w * y), s.width, StrokeCap.Round) } }
            "atras" -> { val p = Path().apply { moveTo(w * .62f, w * .2f); lineTo(w * .32f, w * .5f); lineTo(w * .62f, w * .8f) }; drawPath(p, color, style = s) }
            "compartir" -> { drawCircle(color, w * .1f, Offset(w * .72f, w * .22f)); drawCircle(color, w * .1f, Offset(w * .28f, w * .5f)); drawCircle(color, w * .1f, Offset(w * .72f, w * .78f)); drawLine(color, Offset(w * .28f, w * .5f), Offset(w * .72f, w * .22f), s.width * .8f); drawLine(color, Offset(w * .28f, w * .5f), Offset(w * .72f, w * .78f), s.width * .8f) }
            "play" -> { val p = Path().apply { moveTo(w * .3f, w * .18f); lineTo(w * .82f, w * .5f); lineTo(w * .3f, w * .82f); close() }; drawPath(p, color) }
            "nota" -> { drawCircle(color, w * .13f, Offset(w * .32f, w * .74f)); drawLine(color, Offset(w * .44f, w * .74f), Offset(w * .44f, w * .16f), s.width); drawLine(color, Offset(w * .44f, w * .16f), Offset(w * .74f, w * .26f), s.width) }
            "cerrar" -> { drawLine(color, Offset(w * .25f, w * .25f), Offset(w * .75f, w * .75f), s.width, StrokeCap.Round); drawLine(color, Offset(w * .75f, w * .25f), Offset(w * .25f, w * .75f), s.width, StrokeCap.Round) }
        }
    }
}
