package com.hilo.lector.ui

import android.graphics.Bitmap
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import com.hilo.lector.text.TextTools
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hilo.lector.HiloViewModel
import com.hilo.lector.Stats
import com.hilo.lector.data.Book
import com.hilo.lector.data.ReaderSettings
import com.hilo.lector.pdf.AnnotStore
import com.hilo.lector.pdf.Coloring
import com.hilo.lector.pdf.InkStroke
import com.hilo.lector.pdf.LineBox
import com.hilo.lector.pdf.PageStyle
import com.hilo.lector.pdf.PdfEngine
import com.hilo.lector.pdf.PdfNote
import com.hilo.lector.pdf.Tool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private class PinchAnchor(val zoom0: Float, val fx: Float, val cx: Float, val idx: Int, val off: Int, val cy: Float)

/** Pellizco con dos dedos. Se detecta antes que el desplazamiento para que no se mezclen. */
private fun Modifier.pinch(onStart: (Offset) -> Unit, onZoom: (Float) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var started = false
        do {
            val ev = awaitPointerEvent(PointerEventPass.Initial)
            if (ev.changes.count { it.pressed } >= 2) {
                if (!started) {
                    started = true
                    onStart(ev.calculateCentroid(useCurrent = true))
                }
                val z = ev.calculateZoom()
                if (z != 1f && !z.isNaN()) onZoom(z)
                ev.changes.forEach { it.consume() }
            }
        } while (ev.changes.any { it.pressed })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfView(
    vm: HiloViewModel,
    book: Book,
    engine: PdfEngine,
    annots: AnnotStore,
    s: ReaderSettings,
    mode: ReadMode,
    tool: Tool,
    penColor: Color,
    hlColor: Color,
    bridge: ReaderBridge,
    onTapPlain: () -> Unit,
) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val count = engine.pageCount
    val paged = s.pageMode == 1
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val per = if (paged && s.twoPages && landscape) 2 else 1
    val itemCount = (count + per - 1) / per

    val listState = rememberLazyListState(book.pos.coerceIn(0, max(0, count - 1)), book.posOffset)
    val pagerState = rememberPagerState(initialPage = (book.pos / per).coerceIn(0, max(0, itemCount - 1))) { itemCount }
    var zoom by rememberSaveable(book.id) { mutableFloatStateOf(1f) }
    var pz by remember { mutableFloatStateOf(1f) }        // zoom al pasar páginas
    var pan by remember { mutableStateOf(Offset.Zero) }
    val hScroll = rememberScrollState()
    var anchor by remember { mutableStateOf<PinchAnchor?>(null) }
    var ruler by remember(book.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    var newNote by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }
    var editNote by remember { mutableStateOf<PdfNote?>(null) }

    val style = remember(s.colors, s.colorMode, s.palette, s.intensity) {
        PageStyle(
            colors = s.colors, dark = mode.dark,
            palette = mode.palette(s.palette).map { it.toArgb() },
            ink = mode.ink.toArgb(), intensity = s.intensity,
            paper = mode.paper.toArgb(), identity = s.colorMode == 0,
        )
    }
    val paper = mode.paper
    val needLines = s.colors || s.ruler || tool == Tool.HIGHLIGHT

    fun currentPage(): Int =
        if (paged) (pagerState.currentPage * per).coerceIn(0, max(0, count - 1)) else listState.firstVisibleItemIndex

    // Mantener la página al cambiar entre desplazar y pasar páginas
    LaunchedEffect(paged, per) {
        val p = book.pos.coerceIn(0, max(0, count - 1))
        if (paged) pagerState.scrollToPage(p / per) else listState.scrollToItem(p)
    }

    // Guardar avance y contar páginas
    LaunchedEffect(engine, paged, per) {
        var maxSeen = currentPage()
        snapshotFlow { currentPage() to (if (paged) 0 else listState.firstVisibleItemScrollOffset) }
            .collectLatest { (i, o) ->
                if (i > maxSeen) {
                    if (i - maxSeen <= 3) Stats.add(ctx, pages = i - maxSeen)
                    maxSeen = i
                }
                delay(700)
                val last = if (paged) min(count - 1, i + per - 1)
                else listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: i
                val prog = if (count <= 1) 1f else ((i + 1).toFloat() / count).coerceIn(0.001f, 1f)
                vm.repo.update(book.id) {
                    it.copy(
                        pos = i, posOffset = o, progress = prog, updated = System.currentTimeMillis(),
                        done = it.done || (count > 2 && last >= count - 1), pages = count, flowMode = false,
                    )
                }
            }
    }
    // Al pasar de página, el zoom vuelve a ajustarse
    LaunchedEffect(pagerState.currentPage) { pz = 1f; pan = Offset.Zero }

    // Poner la regla en la primera línea visible al encenderla
    LaunchedEffect(s.ruler) {
        if (s.ruler && ruler == null) {
            val p = currentPage()
            if (engine.lines(p, s.ocr).isNotEmpty()) ruler = p to 0
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(if (paged) paper else c.bg)) {
        val pad = 8.dp
        val vw = maxWidth
        val vh = maxHeight
        val pageW: Dp = (vw - pad * 2) * zoom
        val pagePx = with(density) { pageW.toPx() }
        val padPx = with(density) { pad.toPx() }
        val viewPx = with(density) { vw.toPx() }
        val viewHPx = with(density) { vh.toPx() }
        val renderW = ((pagePx / 128f).roundToInt().coerceAtLeast(2) * 128).coerceAtMost(2000)
        fun contentPx(z: Float) = with(density) { ((vw - pad * 2) * z + pad * 2).toPx() }

        suspend fun seek(p: Int) {
            val q = p.coerceIn(0, max(0, count - 1))
            if (paged) pagerState.scrollToPage(q / per) else listState.scrollToItem(q)
        }

        suspend fun turn(d: Int) {
            if (paged) {
                val t = (pagerState.currentPage + d).coerceIn(0, max(0, itemCount - 1))
                pagerState.animateScrollToPage(t)
            } else {
                listState.animateScrollBy(d * viewHPx * 0.85f)
            }
        }

        fun tapAt(frac: Float) {
            when {
                s.tapTurn && frac < 0.28f -> scope.launch { turn(-1) }
                s.tapTurn && frac > 0.72f -> scope.launch { turn(1) }
                else -> onTapPlain()
            }
        }

        suspend fun ensureVisible(p: Int, ln: LineBox) {
            if (paged) {
                val target = p / per
                if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
                return
            }
            var item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == p }
            if (item == null) {
                listState.scrollToItem(p)
                withFrameNanos { }
                item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == p } ?: return
            }
            val vp = listState.layoutInfo.viewportSize.height.toFloat()
            val top = item.offset + ln.t * item.size
            val bottom = item.offset + ln.b * item.size
            if (top < vp * 0.18f || bottom > vp * 0.75f) listState.animateScrollBy(top - vp * 0.33f)
            if (zoom > 1.01f) {
                val lx = padPx + ln.l * pagePx
                if (lx < hScroll.value || lx > hScroll.value + viewPx * 0.6f) {
                    hScroll.animateScrollTo((lx - padPx * 3).roundToInt().coerceIn(0, hScroll.maxValue))
                }
            }
        }

        suspend fun move(d: Int) {
            val cur = ruler
            var p = cur?.first ?: currentPage()
            var l = if (cur == null) 0 else cur.second + d
            var ls = engine.lines(p, s.ocr)
            var guard = 0
            while (l >= ls.size) {
                p++
                if (p >= count) { vm.toast = "Terminaste el documento. ¡Bien hecho!"; return }
                ls = engine.lines(p, s.ocr); l = 0
                if (++guard > 60) return
            }
            while (l < 0) {
                p--
                if (p < 0) return
                ls = engine.lines(p, s.ocr); l = ls.size - 1
                if (++guard > 60) return
            }
            ruler = p to l
            ensureVisible(p, ls[l])
        }

        val cur = currentPage()
        SideEffect {
            bridge.position = cur
            bridge.total = count
            bridge.unit = "Pág."
            bridge.move = { d -> move(d) }
            bridge.seek = { p -> seek(p) }
            bridge.goPage = { p -> seek(p) }
            bridge.goPara = null
            bridge.follow = { p -> seek(p) }
            bridge.here = {
                val p = currentPage()
                val first = engine.peekLines(p)?.firstOrNull { it.text.length > 3 }?.text
                p to ("Página ${p + 1}" + (first?.let { " · " + TextTools.firstWords(it, 6) } ?: ""))
            }
            bridge.speakFrom = { after ->
                val start = after?.plus(1) ?: (ruler?.first ?: currentPage())
                (start until min(count, start + 4)).map { p -> p to engine.pageText(p, s.ocr) }.filter { it.second.isNotBlank() }
            }
            bridge.words = {
                val start = ruler
                val p0 = start?.first ?: currentPage()
                val out = ArrayList<String>()
                for (p in p0 until min(count, p0 + 6)) {
                    val ls = engine.lines(p, s.ocr)
                    val from = if (start != null && p == start.first) start.second else 0
                    ls.drop(from).forEach { ln -> out.addAll(ln.text.split(Regex("\\s+")).filter { it.isNotEmpty() }) }
                }
                out
            }
        }

        @Composable
        fun page(p: Int, width: Dp, rw: Int) {
            PdfPage(
                engine = engine, page = p, width = width, renderW = rw, style = style, paper = paper,
                ocr = s.ocr, needLines = needLines, rulerOn = s.ruler,
                rulerLine = ruler?.takeIf { it.first == p }?.second,
                tool = tool, penColor = penColor, hlColor = hlColor, annots = annots,
                onRulerTap = { line -> ruler = p to line },
                onTapPlain = { fx ->
                    if (!paged) onTapPlain()
                    else if (per == 2) tapAt(if (p % 2 == 0) fx / 2f else 0.5f + fx / 2f)
                    else tapAt(fx)
                },
                onNoteAdd = { x, y -> newNote = Triple(p, x, y) },
                onNoteOpen = { editNote = it },
            )
        }

        if (paged) {
            Box(
                Modifier.fillMaxSize()
                    .pinch(onStart = {}, onZoom = { f ->
                        pz = (pz * f).coerceIn(1f, 4f)
                        if (pz <= 1.01f) pan = Offset.Zero
                    })
                    .pointerInput(pz > 1.01f, tool) {
                        if (pz > 1.01f && tool == Tool.NONE) {
                            detectDragGestures { ch, d ->
                                ch.consume()
                                val mx = (pz - 1f) * size.width / 2f
                                val my = (pz - 1f) * size.height / 2f
                                pan = Offset((pan.x + d.x).coerceIn(-mx, mx), (pan.y + d.y).coerceIn(-my, my))
                            }
                        }
                    }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = pz; scaleY = pz; translationX = pan.x; translationY = pan.y
                    },
                    userScrollEnabled = tool == Tool.NONE && pz <= 1.01f,
                    beyondViewportPageCount = 1,
                    key = { it },
                ) { item ->
                    Box(
                        Modifier.fillMaxSize().pageTurn(pagerState, item).background(paper)
                            .pointerInput(per) { detectTapGestures { o -> tapAt(o.x / size.width) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            for (k in 0 until per) {
                                val p = item * per + k
                                if (p >= count) break
                                val a = engine.aspects.getOrElse(p) { 1.41f }.coerceAtLeast(0.05f)
                                val w = min(((vw - pad * (per + 1)) / per).value, ((vh - pad * 2) / a).value).dp
                                val rw = ((with(density) { w.toPx() } * pz / 128f).roundToInt().coerceAtLeast(2) * 128).coerceAtMost(2000)
                                page(p, w, rw)
                            }
                        }
                    }
                }
            }
        } else {
            val startPinch by rememberUpdatedState { o: Offset ->
                anchor = PinchAnchor(
                    zoom, (hScroll.value + o.x) / contentPx(zoom), o.x,
                    listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, o.y,
                )
            }
            LaunchedEffect(zoom) {
                val a = anchor ?: return@LaunchedEffect
                withFrameNanos { }
                val ratio = zoom / a.zoom0
                hScroll.scrollTo((a.fx * contentPx(zoom) - a.cx).roundToInt().coerceIn(0, hScroll.maxValue))
                listState.scrollToItem(a.idx, ((a.off + a.cy) * ratio - a.cy).roundToInt().coerceAtLeast(0))
            }
            Box(
                Modifier.fillMaxSize()
                    .pinch(onStart = { startPinch(it) }, onZoom = { f -> zoom = (zoom * f).coerceIn(1f, 4f) })
                    .horizontalScroll(hScroll, enabled = zoom > 1.01f && tool == Tool.NONE)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.width(pageW + pad * 2).fillMaxHeight(),
                    contentPadding = PaddingValues(pad),
                    verticalArrangement = Arrangement.spacedBy(pad),
                    userScrollEnabled = tool == Tool.NONE,
                ) {
                    items(count, key = { it }) { p -> page(p, pageW, renderW) }
                }
            }
        }

        // Zoom
        val zl = if (paged) pz else zoom
        if (zl > 1.01f) {
            Text(
                "${(zl * 100).roundToInt()}%  ·  ajustar",
                color = c.ink, fontSize = 13.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                    .clip(RoundedCornerShape(50)).background(c.surface.copy(alpha = 0.92f))
                    .clickable {
                        if (paged) { pz = 1f; pan = Offset.Zero } else { anchor = null; zoom = 1f; scope.launch { hScroll.scrollTo(0) } }
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }

    // ---------- diálogos ----------

    newNote?.let { (p, x, y) ->
        NoteDialog(
            title = "Nueva nota", initial = "",
            onDismiss = { newNote = null },
            onSave = { t -> if (t.isNotBlank()) annots.addNote(PdfNote(annots.newId(), p, x, y, t.trim())); newNote = null },
        )
    }
    editNote?.let { n ->
        NoteDialog(
            title = "Nota · página ${n.page + 1}", initial = n.text,
            onDismiss = { editNote = null },
            onSave = { t -> annots.editNote(n.id, t.trim()); editNote = null },
            onDelete = { annots.deleteNote(n.id); editNote = null },
        )
    }
}

@Composable
private fun NoteDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit, onDelete: (() -> Unit)? = null) {
    val c = LocalHilo.current
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                text, { text = it }, placeholder = { Text("Escribe tu nota…") },
                modifier = Modifier.fillMaxWidth(), minLines = 3,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Guardar") } },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Borrar", color = c.lines[0]) }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

private fun nearestLine(lines: List<LineBox>, x: Float, y: Float): Int? =
    lines.indices.minByOrNull { i ->
        val l = lines[i]
        val dy = if (y < l.t) l.t - y else if (y > l.b) y - l.b else 0f
        val dx = if (x < l.l) l.l - x else if (x > l.r) x - l.r else 0f
        dy + dx * 0.3f
    }

/**
 * Resaltador inteligente: si arrastras sobre texto, el resaltado se ajusta a las líneas
 * (como seleccionar texto). Si no hay texto debajo, queda a mano alzada.
 */
private fun snapHighlight(pts: List<Offset>, lines: List<LineBox>, aspect: Float): List<Pair<FloatArray, Float>>? {
    if (lines.isEmpty() || pts.isEmpty()) return null
    val a = pts.first()
    val b = pts.last()
    val minY = pts.minOf { it.y }
    val maxY = pts.maxOf { it.y }
    val minX = pts.minOf { it.x }
    val maxX = pts.maxOf { it.x }
    val hit = lines.filter { ln ->
        val cy = (ln.t + ln.b) / 2
        val hh = ln.b - ln.t
        cy >= minY - hh * 0.6f && cy <= maxY + hh * 0.6f && ln.r > minX - 0.02f && ln.l < maxX + 0.02f
    }.sortedBy { it.t }
    if (hit.isEmpty()) return null
    val first = if (a.y <= b.y) a else b
    val last = if (a.y <= b.y) b else a
    return hit.mapIndexedNotNull { i, ln ->
        val x0: Float
        val x1: Float
        if (hit.size == 1) {
            x0 = max(minX, ln.l); x1 = min(maxX, ln.r)
        } else {
            x0 = if (i == 0) max(first.x, ln.l) else ln.l
            x1 = if (i == hit.size - 1) min(last.x, ln.r) else ln.r
        }
        if (x1 - x0 < 0.008f) return@mapIndexedNotNull null
        val cy = (ln.t + ln.b) / 2
        floatArrayOf(x0, cy, x1, cy) to (ln.b - ln.t) * aspect * 1.05f
    }.ifEmpty { null }
}

private fun DrawScope.drawInk(st: InkStroke) {
    val w = size.width
    val h = size.height
    val col = Color(st.color).copy(alpha = if (st.highlight) 0.4f else 1f)
    val sw = max(1.5f, st.width * w)
    if (st.pts.size < 4) {
        drawCircle(col, sw / 2, Offset(st.pts[0] * w, st.pts[1] * h))
        return
    }
    val path = Path()
    path.moveTo(st.pts[0] * w, st.pts[1] * h)
    var k = 2
    while (k + 1 < st.pts.size) {
        path.lineTo(st.pts[k] * w, st.pts[k + 1] * h)
        k += 2
    }
    drawPath(path, col, style = Stroke(width = sw, cap = if (st.highlight) StrokeCap.Butt else StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
private fun PdfPage(
    engine: PdfEngine,
    page: Int,
    width: Dp,
    renderW: Int,
    style: PageStyle,
    paper: Color,
    ocr: Boolean,
    needLines: Boolean,
    rulerOn: Boolean,
    rulerLine: Int?,
    tool: Tool,
    penColor: Color,
    hlColor: Color,
    annots: AnnotStore,
    onRulerTap: (Int) -> Unit,
    onTapPlain: (Float) -> Unit,
    onNoteAdd: (Float, Float) -> Unit,
    onNoteOpen: (PdfNote) -> Unit,
) {
    val c = LocalHilo.current
    val aspect = engine.aspects.getOrElse(page) { 1.41f }.takeIf { it > 0.05f } ?: 1.41f

    val bmp by produceState<Bitmap?>(
        engine.bitmaps.get(engine.styledKey(page, renderW, style)) ?: engine.bitmaps.get(engine.rawKey(page, renderW)),
        page, renderW, style,
    ) {
        val sk = engine.styledKey(page, renderW, style)
        engine.bitmaps.get(sk)?.let { value = it; return@produceState }
        val rk = engine.rawKey(page, renderW)
        val raw = engine.bitmaps.get(rk) ?: try {
            engine.render(page, renderW).also { engine.bitmaps.put(rk, it) }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            null
        } ?: return@produceState
        if (!style.colors && !style.dark) { value = raw; return@produceState }
        if (style.dark) {
            value = withContext(Dispatchers.Default) { Coloring.bake(raw, emptyList(), style) }
        } else if (value == null) {
            value = raw
        }
        if (!style.colors) { value?.let { engine.bitmaps.put(sk, it) }; return@produceState }
        val lines = engine.lines(page, ocr)
        val baked = withContext(Dispatchers.Default) { Coloring.bake(raw, lines, style) }
        engine.bitmaps.put(sk, baked)
        value = baked
    }

    val lines by produceState(engine.peekLines(page) ?: emptyList(), page, ocr, needLines) {
        if (needLines) value = engine.lines(page, ocr)
    }

    val live = remember { mutableStateListOf<Offset>() }
    val tapPlain by rememberUpdatedState(onTapPlain)
    val rulerTap by rememberUpdatedState(onRulerTap)
    val noteAdd by rememberUpdatedState(onNoteAdd)
    val currentLines by rememberUpdatedState(lines)

    fun commit() {
        val pts = live.toList()
        live.clear()
        if (pts.isEmpty()) return
        if (tool == Tool.HIGHLIGHT) {
            val snapped = snapHighlight(pts, currentLines, aspect)
            if (snapped != null) {
                snapped.forEach { (p, w) -> annots.addStroke(InkStroke(annots.newId(), page, true, hlColor.toArgb(), w, p)) }
                return
            }
        }
        val flat = FloatArray(pts.size * 2)
        pts.forEachIndexed { i, o -> flat[i * 2] = o.x; flat[i * 2 + 1] = o.y }
        val hl = tool == Tool.HIGHLIGHT
        annots.addStroke(
            InkStroke(annots.newId(), page, hl, (if (hl) hlColor else penColor).toArgb(), if (hl) 0.022f else 0.0035f, flat)
        )
    }

    val gestures = when (tool) {
        Tool.NONE -> Modifier.pointerInput(rulerOn) {
            detectTapGestures { pos ->
                if (rulerOn) {
                    nearestLine(currentLines, pos.x / size.width, pos.y / size.height)?.let { rulerTap(it) }
                } else tapPlain(pos.x / size.width)
            }
        }
        Tool.PEN, Tool.HIGHLIGHT -> Modifier.pointerInput(tool, penColor, hlColor) {
            detectDragGestures(
                onDragStart = { o -> live.clear(); live.add(Offset(o.x / size.width, o.y / size.height)) },
                onDragEnd = { commit() },
                onDragCancel = { live.clear() },
            ) { ch, _ ->
                ch.consume()
                live.add(Offset(ch.position.x / size.width, ch.position.y / size.height))
            }
        }
        Tool.ERASER -> Modifier
            .pointerInput(tool) {
                detectDragGestures(
                    onDragStart = { o -> annots.eraseAt(page, o.x / size.width, o.y / size.height, aspect) },
                ) { ch, _ ->
                    ch.consume()
                    annots.eraseAt(page, ch.position.x / size.width, ch.position.y / size.height, aspect)
                }
            }
            .pointerInput("tap-erase") {
                detectTapGestures { o -> annots.eraseAt(page, o.x / size.width, o.y / size.height, aspect) }
            }
        Tool.NOTE -> Modifier.pointerInput(tool) {
            detectTapGestures { o -> noteAdd(o.x / size.width, o.y / size.height) }
        }
    }

    Box(
        Modifier.width(width).aspectRatio(1f / aspect).clip(RoundedCornerShape(3.dp)).background(paper)
    ) {
        val b = bmp
        if (b != null) {
            val img = remember(b) { b.asImageBitmap() }
            Image(img, "Página ${page + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        } else {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp), color = c.accent, strokeWidth = 2.dp)
        }

        val liveColor = if (tool == Tool.HIGHLIGHT) hlColor.copy(alpha = 0.4f) else penColor
        Canvas(Modifier.fillMaxSize().then(gestures)) {
            for (st in annots.strokes) if (st.page == page) drawInk(st)
            if (live.size > 1) {
                val p = Path()
                p.moveTo(live[0].x * size.width, live[0].y * size.height)
                for (i in 1 until live.size) p.lineTo(live[i].x * size.width, live[i].y * size.height)
                val sw = if (tool == Tool.HIGHLIGHT) 0.022f * size.width else max(1.5f, 0.0035f * size.width)
                drawPath(p, liveColor, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            if (rulerOn) {
                val scrim = paper.copy(alpha = 0.78f)
                val ln = rulerLine?.let { lines.getOrNull(it) }
                if (ln == null) {
                    drawRect(scrim)
                } else {
                    val extra = (ln.b - ln.t) * size.height * 0.35f
                    val top = (ln.t * size.height - extra).coerceAtLeast(0f)
                    val bottom = (ln.b * size.height + extra).coerceAtMost(size.height)
                    drawRect(scrim, Offset.Zero, Size(size.width, top))
                    drawRect(scrim, Offset(0f, bottom), Size(size.width, size.height - bottom))
                    drawRoundRect(
                        c.accent, Offset(2f, top), Size(4.dp.toPx(), bottom - top),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            }
        }

        // Notas
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wPx = constraints.maxWidth
            val hPx = constraints.maxHeight
            val half = with(LocalDensity.current) { 14.dp.roundToPx() }
            for (n in annots.notes) {
                if (n.page != page) continue
                Box(
                    Modifier
                        .offset { IntOffset((n.x * wPx).roundToInt() - half, (n.y * hPx).roundToInt() - half) }
                        .size(28.dp).clip(CircleShape).background(Color(0xFFFFC94D))
                        .clickable { onNoteOpen(n) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PushPin, "Nota", tint = Color(0xFF5A3B00), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** Botones para mover la regla línea por línea. */
@Composable
fun RulerButtons(modifier: Modifier, onUp: () -> Unit, onDown: () -> Unit) {
    val c = LocalHilo.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(c.surface).clickable(onClick = onUp),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.KeyboardArrowUp, "Línea anterior", tint = c.ink) }
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(c.accent).clickable(onClick = onDown),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.KeyboardArrowDown, "Siguiente línea", tint = c.accentInk) }
    }
}
