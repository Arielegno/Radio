package com.hilo.lector.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ShortText
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hilo.lector.data.MARK_KINDS
import kotlin.math.roundToInt

/** Texto elegido dentro de un párrafo: caracteres [start, end). */
data class TextSel(val para: Int, val start: Int, val end: Int)

/** Color del resaltado común (sin tipo). */
const val PLAIN_HL = 0xFFFFE066.toInt()

/** Estado de la selección en el texto fluido y dónde quedó dibujado cada párrafo. */
class FlowSelection {
    var sel by mutableStateOf<TextSel?>(null)
    /** Sube cada vez que el párrafo elegido se mueve en pantalla, para que las manijas lo sigan. */
    var tick by mutableIntStateOf(0)
    val placed = HashMap<Int, Pair<LayoutCoordinates, TextLayoutResult>>()
    /** En el modo por páginas: la parte del párrafo que se ve en la página actual. */
    val visible = HashMap<Int, IntRange>()

    fun place(para: Int, coords: LayoutCoordinates, layout: TextLayoutResult, range: IntRange? = null) {
        placed[para] = coords to layout
        if (range != null) visible[para] = range else visible.remove(para)
        if (sel?.para == para) tick++
    }

    /** Si hay algo elegido, un toque lo suelta (y no pasa la página). */
    fun consumeTap(): Boolean {
        if (sel == null) return false
        sel = null
        return true
    }
}

private val WORD_CHAR = Regex("[\\p{L}\\p{N}'’\\-]")

/** La palabra que está en [offset]. */
fun wordRange(text: String, offset: Int): Pair<Int, Int> {
    if (text.isEmpty()) return 0 to 0
    var o = offset.coerceIn(0, text.length - 1)
    if (!WORD_CHAR.matches(text[o].toString()) && o > 0 && WORD_CHAR.matches(text[o - 1].toString())) o--
    if (!WORD_CHAR.matches(text[o].toString())) return o to (o + 1)
    var a = o
    var b = o + 1
    while (a > 0 && WORD_CHAR.matches(text[a - 1].toString())) a--
    while (b < text.length && WORD_CHAR.matches(text[b].toString())) b++
    // Sin guiones ni apóstrofos sueltos en los bordes
    while (b > a + 1 && text[b - 1] in "-'’") b--
    while (a < b - 1 && text[a] in "-'’") a++
    return a to b
}

/** Manija de selección en forma de gota, como en el resto de Android. [at] es la esquina que toca el texto. */
@Composable
fun SelHandle(at: Offset, start: Boolean, color: Color, onMove: (Offset) -> Unit, onEnd: () -> Unit = {}) {
    val d = LocalDensity.current
    val sz = with(d) { 22.dp.toPx() }
    val move by rememberUpdatedState(onMove)
    val end by rememberUpdatedState(onEnd)
    var drag by remember { mutableStateOf(Offset.Zero) }
    // Zona táctil más grande que el dibujo, para que sea fácil de agarrar
    val touch = with(d) { 44.dp.toPx() }
    Box(
        Modifier
            .offset { IntOffset((at.x - touch / 2f).roundToInt(), (at.y - with(d) { 4.dp.toPx() }).roundToInt()) }
            .size(44.dp)
            .pointerInput(start) {
                detectDragGestures(
                    onDragStart = { drag = at },
                    onDragEnd = { end() },
                    onDragCancel = { end() },
                ) { ch, amount ->
                    ch.consume()
                    drag += amount
                    move(drag)
                }
            },
    ) {
        Canvas(Modifier.size(44.dp)) {
            val cx = size.width / 2f
            val top = 4.dp.toPx()
            val r = sz / 2f
            // Círculo con una punta que señala el texto
            drawCircle(color, r, Offset(if (start) cx - r else cx + r, top + r))
            drawRect(color, topLeft = Offset(if (start) cx - r else cx, top), size = Size(r, r))
        }
    }
}

/** Lo que el menú necesita saber y hacer con el texto elegido. */
class SelMenu(
    val text: String,
    val lang: String,
    val onHighlight: (kind: String) -> Unit,
    val onNote: (note: String) -> Unit,
    val onStudy: () -> Unit,
    val onCite: () -> Unit,
    val onSentence: (() -> Unit)?,
    val onParagraph: (() -> Unit)?,
    val onSpeak: (() -> Unit)? = null,
    val onDone: () -> Unit,
)

/** Menú pequeño que flota junto al texto elegido (arriba si hay lugar; si no, abajo). */
@Composable
fun SelectionToolbar(anchor: Rect, menu: SelMenu) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val d = LocalDensity.current
    var panel by remember(menu.text) { mutableStateOf("main") }   // main | color | more
    var note by remember { mutableStateOf<String?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val W = constraints.maxWidth.toFloat()
        val H = constraints.maxHeight.toFloat()
        val gap = with(d) { 12.dp.toPx() }
        val topSafe = with(d) { 96.dp.toPx() }
        val handleRoom = with(d) { 34.dp.toPx() }
        val above = anchor.top - gap - size.height
        val below = anchor.bottom + handleRoom
        val y = when {
            above >= topSafe -> above
            below + size.height <= H - with(d) { 24.dp.toPx() } -> below
            else -> ((H - size.height) / 2f)
        }
        val x = (anchor.center.x - size.width / 2f).coerceIn(with(d) { 8.dp.toPx() }, (W - size.width - with(d) { 8.dp.toPx() }).coerceAtLeast(0f))

        Surface(
            shape = RoundedCornerShape(18.dp), color = c.surface, shadowElevation = 10.dp, tonalElevation = 2.dp,
            modifier = Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }.widthIn(max = 420.dp)
                .onSizeChanged { size = it }.border(1.dp, c.line, RoundedCornerShape(18.dp)),
        ) {
            when (panel) {
                "color" -> Row(Modifier.horizontalScroll(rememberScrollState()).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ToolItem(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") { panel = "main" }
                    (listOf(Triple("", "Resaltar", PLAIN_HL)) + MARK_KINDS.map { (k, l, col) -> Triple(k, if (k == "duda") "Duda" else l, col) }).forEach { (k, l, col) ->
                        Column(
                            Modifier.width(58.dp).clip(RoundedCornerShape(12.dp)).clickable { menu.onHighlight(k); menu.onDone() }.padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.size(26.dp).clip(CircleShape).background(Color(col)).border(1.dp, c.line, CircleShape))
                            Text(l, color = c.ink, fontSize = 10.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                "more" -> Column(Modifier.width(260.dp).padding(vertical = 6.dp)) {
                    MoreItem(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") { panel = "main" }
                    MoreItem(Icons.Rounded.School, "Estudiar: tarjetas y palabras clave") { menu.onStudy(); menu.onDone() }
                    MoreItem(Icons.Rounded.FormatQuote, "Guardar como cita (APA)") { menu.onCite(); menu.onDone() }
                    MoreItem(Icons.Rounded.Public, "Buscar en Wikipedia") { lookup(ctx, menu.text, "wiki", menu.lang); menu.onDone() }
                    MoreItem(Icons.Rounded.Search, "Buscar en internet") { webSearch(ctx, menu.text); menu.onDone() }
                    MoreItem(Icons.Rounded.Share, "Compartir") { share(ctx, menu.text); menu.onDone() }
                    menu.onSpeak?.let { sp -> MoreItem(Icons.Rounded.Style, "Leer en voz alta desde aquí") { sp(); menu.onDone() } }
                    menu.onSentence?.let { f -> MoreItem(Icons.AutoMirrored.Rounded.ShortText, "Elegir toda la oración") { f(); panel = "main" } }
                    menu.onParagraph?.let { f -> MoreItem(Icons.Rounded.Notes, "Elegir todo el párrafo") { f(); panel = "main" } }
                }
                else -> Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ToolItem(Icons.Rounded.ContentCopy, "Copiar") { copyText(ctx, menu.text); menu.onDone() }
                    ToolItem(Icons.Rounded.BorderColor, "Subrayar", Color(PLAIN_HL)) { panel = "color" }
                    ToolItem(Icons.Rounded.EditNote, "Nota") { note = "" }
                    ToolItem(Icons.Rounded.Translate, "Traducir") { lookup(ctx, menu.text, "tr", menu.lang); menu.onDone() }
                    ToolItem(Icons.AutoMirrored.Rounded.MenuBook, "Diccionario") { lookup(ctx, menu.text, "dic", menu.lang); menu.onDone() }
                    ToolItem(Icons.Rounded.MoreVert, "Más") { panel = "more" }
                }
            }
        }
    }

    note?.let { n ->
        var t by remember { mutableStateOf(n) }
        AlertDialog(
            onDismissRequest = { note = null },
            title = { Text("Nota") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("«${menu.text.take(220)}»", color = c.muted, fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    OutlinedTextField(t, { t = it }, label = { Text("Lo que piensas de esto") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Text("Se guarda en tus Apuntes de este documento y el texto queda resaltado.", color = c.muted, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { menu.onNote(t.trim()); note = null; menu.onDone() }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { note = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ToolItem(icon: ImageVector, label: String, dot: Color? = null, onClick: () -> Unit) {
    val c = LocalHilo.current
    Column(
        Modifier.width(60.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(icon, null, tint = c.ink, modifier = Modifier.size(22.dp))
            if (dot != null) Box(Modifier.align(Alignment.BottomEnd).size(8.dp).clip(CircleShape).background(dot))
        }
        Text(label, color = c.ink, fontSize = 10.5.sp, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun MoreItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = LocalHilo.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.accent, modifier = Modifier.size(20.dp))
        Text(label, color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 14.dp))
    }
}

fun share(ctx: Context, text: String) {
    runCatching {
        ctx.startActivity(
            Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Compartir")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

fun webSearch(ctx: Context, text: String) {
    runCatching {
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(text.take(200))))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

// =====================================================================
// Selección sobre la página original del PDF (por líneas)
// =====================================================================

/** Desde la línea [l1] carácter [c1] hasta la línea [l2] carácter [c2] (sin incluirlo). */
data class PdfSel(val page: Int, val l1: Int, val c1: Int, val l2: Int, val c2: Int) {
    fun ordered(): PdfSel = if (l1 < l2 || (l1 == l2 && c1 <= c2)) this else PdfSel(page, l2, c2, l1, c1)
}

class PdfSelection {
    var sel by mutableStateOf<PdfSel?>(null)
    var tick by mutableIntStateOf(0)
    val placed = HashMap<Int, Pair<LayoutCoordinates, List<com.hilo.lector.pdf.LineBox>>>()

    fun place(page: Int, coords: LayoutCoordinates, lines: List<com.hilo.lector.pdf.LineBox>) {
        placed[page] = coords to lines
        if (sel?.page == page) tick++
    }

    fun consumeTap(): Boolean {
        if (sel == null) return false
        sel = null
        return true
    }
}

/** Posición horizontal (0..1) del carácter [ch] de la línea: se reparte el ancho entre las letras. */
fun lineCharX(ln: com.hilo.lector.pdf.LineBox, ch: Int): Float =
    ln.l + (ln.r - ln.l) * ch.coerceIn(0, ln.text.length) / ln.text.length.coerceAtLeast(1)

fun lineCharAt(ln: com.hilo.lector.pdf.LineBox, x: Float): Int =
    (((x - ln.l) / (ln.r - ln.l).coerceAtLeast(0.0001f)) * ln.text.length).roundToInt().coerceIn(0, ln.text.length)

/** El texto elegido, uniendo las líneas (y las palabras cortadas con guion al final de línea). */
fun pdfSelText(lines: List<com.hilo.lector.pdf.LineBox>, s0: PdfSel): String {
    val s = s0.ordered()
    val sb = StringBuilder()
    for (i in s.l1..s.l2) {
        val t = lines.getOrNull(i)?.text ?: continue
        val a = if (i == s.l1) s.c1.coerceIn(0, t.length) else 0
        val b = if (i == s.l2) s.c2.coerceIn(a, t.length) else t.length
        val part = t.substring(a, b)
        if (sb.isNotEmpty()) {
            if (sb.endsWith("-") && part.firstOrNull()?.isLowerCase() == true) sb.setLength(sb.length - 1) else sb.append(' ')
        }
        sb.append(part)
    }
    return sb.toString().trim()
}
