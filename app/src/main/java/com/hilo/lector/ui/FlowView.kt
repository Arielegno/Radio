package com.hilo.lector.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.hilo.lector.HiloViewModel
import com.hilo.lector.data.Book
import com.hilo.lector.data.ReaderSettings
import com.hilo.lector.data.TextMark
import com.hilo.lector.pdf.PdfEngine
import com.hilo.lector.text.TextTools
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private val WORD = Regex("\\S+")

/** Negrita guía: resalta el inicio de cada palabra. */
fun bionic(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in WORD.findAll(text)) {
        append(text.substring(last, m.range.first))
        val w = m.value
        val lead = w.indexOfFirst { it.isLetterOrDigit() }
        if (lead < 0) {
            append(w)
        } else {
            val core = w.substring(lead).takeWhile { it.isLetterOrDigit() }
            val k = if (core.length <= 3) 1 else ceil(core.length * 0.45).toInt()
            append(w.substring(0, lead))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(w.substring(lead, lead + k)) }
            append(w.substring(lead + k))
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

// El mismo texto, estilo y espacio se usan para dibujar y para dividir en páginas.
private fun paraText(body: String, heading: Boolean, s: ReaderSettings): AnnotatedString =
    if (s.bionic && !heading) bionic(body) else AnnotatedString(body)

internal fun paraStyle(heading: Boolean, s: ReaderSettings, ink: androidx.compose.ui.graphics.Color, length: Int = 0): TextStyle {
    // En párrafos enormes, los guiones y el corte equilibrado tardan segundos: ahí se usa el corte simple
    val heavy = length > 2500
    val fs = if (heading) s.fontSize * 1.25f else s.fontSize
    val justify = !heading && s.align >= 1
    return TextStyle(
        fontFamily = if (heading) Fonts.display else Fonts.reading(s.font, s.weight),
        fontWeight = if (heading) FontWeight.Bold else Fonts.weightFor(s.font, s.weight),
        fontSize = fs.sp,
        lineHeight = (fs * s.lineHeight).sp,
        color = ink,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        // Como la página de un libro: justificado, con guiones al cortar palabras y sangría al empezar el párrafo
        textAlign = if (justify) TextAlign.Justify else TextAlign.Start,
        hyphens = if (!heading && s.align == 1 && !heavy) Hyphens.Auto else Hyphens.None,
        lineBreak = if (justify && !heavy) LineBreak.Paragraph else LineBreak.Simple,
        textIndent = if (!heading && s.indent) TextIndent(firstLine = (fs * 1.4f).sp) else TextIndent.None,
    )
}

private fun paraGap(heading: Boolean, s: ReaderSettings): Dp = (s.fontSize * (if (heading) 0.7f else s.paraSpacing)).dp

/**
 * Un párrafo con colores por línea: cada línea va del color del texto a un color y la siguiente
 * vuelve desde ese mismo color, así el ojo encuentra el inicio de la línea siguiente.
 */
@Composable
fun FlowParagraph(
    text: String,
    s: ReaderSettings,
    mode: ReadMode,
    rulerLine: Int?,
    rulerOn: Boolean,
    onLayout: (TextLayoutResult) -> Unit,
    onTap: (line: Int?, xFrac: Float) -> Unit,
    marks: List<Pair<IntRange, Int>> = emptyList(),
    onLongPress: ((offset: Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    gap: Boolean = true,
    selection: IntRange? = null,
    onPlaced: ((androidx.compose.ui.layout.LayoutCoordinates, TextLayoutResult) -> Unit)? = null,
    spoken: IntRange? = null,
) {
    val heading = text.startsWith("# ")
    val longPress by rememberUpdatedState(onLongPress)
    val body = if (heading) text.substring(2) else text
    val annotated = remember(body, s.bionic, heading) { paraText(body, heading, s) }
    val palette = mode.palette(s.palette)
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val tap by rememberUpdatedState(onTap)
    val ink = mode.ink

    Text(
        annotated,
        style = paraStyle(heading, s, ink, body.length),
        onTextLayout = { layout = it; onLayout(it) },
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = if (gap) paraGap(heading, s) else 0.dp)
            .then(
                if (onPlaced != null) Modifier.onGloballyPositioned { co -> layout?.let { onPlaced(co, it) } } else Modifier
            )
            .drawBehind {
                val l = layout
                // La palabra que está diciendo la voz (como un karaoke)
                if (l != null && spoken != null) {
                    val n = l.layoutInput.text.length
                    val a = spoken.first.coerceIn(0, n)
                    val b = (spoken.last + 1).coerceIn(a, n)
                    if (b > a) drawPath(l.getPathForRange(a, b), Color(0xFFFFB300).copy(alpha = if (mode.dark) 0.55f else 0.45f))
                }
                // Texto elegido con las manijas
                if (l != null && selection != null) {
                    val n = l.layoutInput.text.length
                    val a = selection.first.coerceIn(0, n)
                    val b = selection.last.coerceIn(a, n)
                    if (b > a) drawPath(l.getPathForRange(a, b), Color(0xFF4FA8FF).copy(alpha = if (mode.dark) 0.45f else 0.38f))
                }
                // Resaltados del estudio
                if (l != null && marks.isNotEmpty()) {
                    val n = l.layoutInput.text.length
                    for ((r, col) in marks) {
                        val hl = Color(col).copy(alpha = if (mode.dark) 0.30f else 0.55f)
                        val a = r.first.coerceIn(0, n)
                        val b = (r.last + 1).coerceIn(a, n)
                        if (b > a) drawPath(l.getPathForRange(a, b), hl)
                    }
                }
                if (rulerOn && rulerLine != null && l != null && l.lineCount > 0) {
                    val rl = rulerLine.coerceIn(0, l.lineCount - 1)
                    val t = l.getLineTop(rl)
                    val b = l.getLineBottom(rl)
                    drawRoundRect(
                        mode.band, Offset(-10.dp.toPx(), t), Size(size.width + 20.dp.toPx(), b - t),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                    )
                }
            }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val l = layout ?: return@drawWithContent
                if (s.colors && palette.isNotEmpty()) {
                    for (i in 0 until l.lineCount) {
                        val t = l.getLineTop(i)
                        val b = l.getLineBottom(i)
                        val full = lerp(ink, palette[(i / 2) % palette.size], s.intensity)
                        val c0 = if (i % 2 == 0) ink else full
                        val c1 = if (i % 2 == 0) full else ink
                        drawRect(
                            Brush.horizontalGradient(listOf(c0, c1), startX = 0f, endX = size.width),
                            topLeft = Offset(0f, t), size = Size(size.width, b - t), blendMode = BlendMode.SrcAtop,
                        )
                    }
                }
                if (rulerOn) {
                    val scrim = mode.paper.copy(alpha = 0.8f)
                    if (rulerLine == null || l.lineCount == 0) {
                        drawRect(scrim)
                    } else {
                        val rl = rulerLine.coerceIn(0, l.lineCount - 1)
                        for (i in 0 until l.lineCount) {
                            if (i == rl) continue
                            val t = l.getLineTop(i)
                            drawRect(scrim, Offset(0f, t), Size(size.width, l.getLineBottom(i) - t))
                        }
                    }
                }
            }
            .pointerInput(rulerOn) {
                detectTapGestures(
                    onLongPress = { pos ->
                        val l = layout
                        if (l != null) longPress?.invoke(l.getOffsetForPosition(pos))
                    },
                ) { pos ->
                    val l = layout
                    tap(if (rulerOn && l != null) l.getLineForVerticalPosition(pos.y) else null, pos.x / size.width)
                }
            },
    )
}

/** Trozo de un párrafo que cae en una página: caracteres [start, end) y su franja en píxeles dentro del párrafo. */
private data class Chunk(val para: Int, val start: Int, val end: Int, val top: Float = 0f, val bottom: Float = 0f, val endsPara: Boolean = true)
private data class PagedRuler(val page: Int, val chunk: Int, val line: Int)

private class Pagination {
    val pages = mutableStateListOf<List<Chunk>>()
    var done by mutableStateOf(false)
}

/** Texto fluido: el libro como párrafos continuos o en páginas que se pasan hacia el lado. */
@Composable
fun FlowView(
    vm: HiloViewModel,
    book: Book,
    engine: PdfEngine?,
    s: ReaderSettings,
    mode: ReadMode,
    bridge: ReaderBridge,
    onTapPlain: () -> Unit,
    onBackToPdf: (() -> Unit)?,
    onStudy: (StudyRequest) -> Unit = {},
) {
    val c = LocalHilo.current
    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val paras by produceState<List<String>?>(null, book.id, engine) {
        value = try {
            vm.flowParas(book, engine) { p, n -> progress = p to n }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            emptyList()
        }
    }

    val list = paras
    if (list == null) {
        Column(
            Modifier.fillMaxSize().background(mode.paper).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Preparando el texto…", style = MaterialTheme.typography.titleLarge, color = mode.ink)
            Spacer(Modifier.height(14.dp))
            val pr = progress
            if (pr != null && pr.second > 0) {
                LinearProgressIndicator(
                    progress = { (pr.first + 1f) / pr.second }, color = c.accent, trackColor = c.chip,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("Página ${pr.first + 1} de ${pr.second}", color = mode.muted)
                Text("Las páginas escaneadas tardan más la primera vez.", color = mode.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            if (onBackToPdf != null) {
                Spacer(Modifier.height(18.dp))
                TextButton(onClick = onBackToPdf) { Text("Volver a la página original") }
            }
        }
        return
    }
    if (list.isEmpty()) {
        Column(
            Modifier.fillMaxSize().background(mode.paper).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "No encontré texto en este documento. Si es escaneado, activa «Reconocer texto en escaneados» en Ajustes.",
                color = mode.ink, textAlign = TextAlign.Center,
            )
            if (onBackToPdf != null) TextButton(onClick = onBackToPdf) { Text("Volver a la página original") }
        }
        return
    }

    val fsel = remember(book.id) { FlowSelection() }

    /** Resalta [a, b) del párrafo con el color de su tipo. */
    fun highlight(para: Int, a: Int, b: Int, kind: String) {
        val body = list[para].removePrefix("# ")
        val col = com.hilo.lector.data.MARK_KINDS.firstOrNull { it.first == kind }?.third ?: PLAIN_HL
        val mk = TextMark(vm.study.newId(), book.id, para, a, b, body.substring(a, b).trim(), col, System.currentTimeMillis(), kind)
        vm.study.addMark(mk)
        if (kind == "duda") vm.toast = "Marcada como duda: la verás en tu ruta de estudio."
        vm.undoable(if (kind.isBlank()) "Resaltado" else "Subrayado: " + (com.hilo.lector.data.MARK_KINDS.firstOrNull { it.first == kind }?.second ?: "")) { vm.study.deleteMark(mk.id) }
    }

    /** El panel completo de estudio, con las palabras elegidas ya marcadas. */
    fun request(para: Int, selA: Int, selB: Int): StudyRequest? {
        val body = list[para].removePrefix("# ")
        val (s0, _) = sentenceAround(body, selA)
        val (_, e1) = sentenceAround(body, (selB - 1).coerceAtLeast(selA))
        val sentence = body.substring(s0, e1).trim()
        if (sentence.isEmpty()) return null
        val lead = body.substring(s0, e1).length - body.substring(s0, e1).trimStart().length
        return StudyRequest(
            bookId = book.id, sentence = sentence, wordIndex = wordIndexAt(body.substring(s0, e1), selA - s0),
            flow = true, pos = para, source = "párrafo ${para + 1}",
            onHighlight = { kind -> highlight(para, selA, selB, kind) },
            selStart = selA - s0 - lead, selEnd = selB - s0 - lead,
        )
    }

    // Mantener el dedo sobre el texto: elegir la palabra (después se ajusta con las manijas)
    val study: (Int, Int) -> Unit = { para, offset ->
        val body = list[para].removePrefix("# ")
        if (body.isNotBlank()) {
            val (a, b) = wordRange(body, offset)
            fsel.sel = TextSel(para, a, b)
        }
    }

    var root by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    Box(Modifier.fillMaxSize().onGloballyPositioned { root = it }) {
        if (s.pageMode == 1) PagedFlow(vm, book, list, s, mode, bridge, onTapPlain, study, fsel)
        else ScrollFlow(vm, book, list, s, mode, bridge, onTapPlain, study, fsel)
        FlowSelectionOverlay(vm, book, list, fsel, root, c.accent, ::highlight, ::request, onStudy)
    }
}

/** Manijas y menú del texto elegido, encima del texto. */
@Composable
private fun FlowSelectionOverlay(
    vm: HiloViewModel,
    book: Book,
    list: List<String>,
    fsel: FlowSelection,
    root: androidx.compose.ui.layout.LayoutCoordinates?,
    color: Color,
    highlight: (Int, Int, Int, String) -> Unit,
    request: (Int, Int, Int) -> StudyRequest?,
    onStudy: (StudyRequest) -> Unit,
) {
    val sel = fsel.sel ?: return
    @Suppress("UNUSED_VARIABLE") val tick = fsel.tick
    val (pc, lay) = fsel.placed[sel.para] ?: return
    val rc = root ?: return
    if (!pc.isAttached || !rc.isAttached) return
    val body = list[sel.para].removePrefix("# ")
    val n = minOf(lay.layoutInput.text.length, body.length)
    val vis = fsel.visible[sel.para] ?: (0 until n + 1)
    val lo = vis.first.coerceIn(0, n)
    val hi = (vis.last).coerceIn(lo, n)
    val a = sel.start.coerceIn(lo, hi)
    val b = sel.end.coerceIn(a, hi)
    if (b <= a) return
    fun toRoot(p: Offset) = rc.localPositionOf(pc, p)
    val ca = lay.getCursorRect(a)
    val cb = lay.getCursorRect(b)
    val startPt = toRoot(Offset(ca.left, ca.bottom))
    val endPt = toRoot(Offset(cb.left, cb.bottom))
    val topPt = toRoot(Offset(0f, lay.getLineTop(lay.getLineForOffset(a))))
    val lineH = ca.bottom - ca.top
    val anchor = androidx.compose.ui.geometry.Rect(minOf(startPt.x, endPt.x), topPt.y, maxOf(startPt.x, endPt.x), endPt.y)

    fun offsetAt(p: Offset): Int {
        val local = pc.localPositionOf(rc, p - Offset(0f, lineH * 0.6f))
        return lay.getOffsetForPosition(local).coerceIn(lo, hi)
    }
    SelHandle(startPt, true, color, onMove = { p ->
        val o = offsetAt(p)
        fsel.sel = if (o < sel.end) sel.copy(start = o) else sel.copy(start = (sel.end - 1).coerceAtLeast(0))
    })
    SelHandle(endPt, false, color, onMove = { p ->
        val o = offsetAt(p)
        fsel.sel = if (o > sel.start) sel.copy(end = o) else sel.copy(end = sel.start + 1)
    })
    val text = body.substring(a, b).trim()
    SelectionToolbar(
        anchor,
        SelMenu(
            text = text,
            lang = vm.study.langs[book.id].orEmpty(),
            onHighlight = { kind -> highlight(sel.para, a, b, kind) },
            onNote = { note ->
                highlight(sel.para, a, b, "")
                val old = vm.study.notes[book.id].orEmpty()
                val entry = "> «$text» (párrafo ${sel.para + 1})" + (if (note.isNotBlank()) "\n$note" else "")
                vm.study.setNote(book.id, if (old.isBlank()) entry else old.trimEnd() + "\n\n" + entry)
                vm.toast = "Nota guardada en Apuntes."
            },
            onStudy = { request(sel.para, a, b)?.let(onStudy) },
            onCite = { request(sel.para, a, b)?.let { saveQuote(vm, it, text) } },
            onSentence = {
                val (s0, s1) = sentenceAround(body, a)
                fsel.sel = TextSel(sel.para, s0.coerceAtLeast(lo), s1.coerceAtMost(hi))
            },
            onParagraph = { fsel.sel = TextSel(sel.para, lo, hi) },
            onDone = { fsel.sel = null },
            onHelp = { vm.help = "menu-sel" },
        ),
    )
}

@Composable
private fun ScrollFlow(
    vm: HiloViewModel,
    book: Book,
    list: List<String>,
    s: ReaderSettings,
    mode: ReadMode,
    bridge: ReaderBridge,
    onTapPlain: () -> Unit,
    study: (para: Int, offset: Int) -> Unit,
    fsel: FlowSelection,
) {
    val c = LocalHilo.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState(book.flowPos.coerceIn(0, list.size - 1), book.flowOffset)
    val layouts = remember(book.id) { HashMap<Int, TextLayoutResult>() }
    var ruler by remember(book.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    // Resaltados agrupados por párrafo: con miles de resaltados, cada párrafo busca solo los suyos
    val byPara by remember(book.id) { androidx.compose.runtime.derivedStateOf { vm.study.marks.filter { it.bookId == book.id }.groupBy { it.para } } }
    fun marksFor(para: Int, start: Int, end: Int): List<Pair<IntRange, Int>> = byPara[para].orEmpty().filter { it.end > start && it.start < end }
        .map { ((maxOf(it.start, start) - start) until (minOf(it.end, end) - start)) to it.color }

    LaunchedEffect(list) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (i, o) ->
                delay(700)
                val prog = ((i + 1f) / list.size).coerceIn(0.001f, 1f)
                vm.repo.update(book.id) {
                    it.copy(flowPos = i, flowOffset = o, progress = prog, updated = System.currentTimeMillis(), flowMode = it.type == "pdf")
                }
            }
    }
    LaunchedEffect(s.ruler) {
        if (s.ruler && ruler == null) ruler = listState.firstVisibleItemIndex.coerceAtMost(list.size - 1) to 0
    }
    var focusPara by remember(book.id) { mutableIntStateOf(-1) }
    LaunchedEffect(s.paraFocus) { if (s.paraFocus) focusPara = (listState.firstVisibleItemIndex + if (listState.firstVisibleItemScrollOffset > 40) 1 else 0).coerceAtMost(list.size - 1) }

    suspend fun ensureVisible(p: Int, l: Int) {
        var item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == p }
        if (item == null) {
            listState.scrollToItem(p)
            withFrameNanos { }
            item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == p } ?: return
        }
        val lay = layouts[p] ?: return
        if (lay.lineCount == 0) return
        val li = l.coerceIn(0, lay.lineCount - 1)
        val vp = listState.layoutInfo.viewportSize.height.toFloat()
        val top = item.offset + lay.getLineTop(li)
        val bottom = item.offset + lay.getLineBottom(li)
        if (top < vp * 0.18f || bottom > vp * 0.72f) listState.animateScrollBy(top - vp * 0.33f)
    }

    suspend fun move(d: Int) {
        val cur = ruler ?: (listState.firstVisibleItemIndex to -1)
        var p = cur.first
        var l = cur.second + d
        if (l >= (layouts[p]?.lineCount ?: 1)) {
            p++; l = 0
            if (p >= list.size) { vm.toast = "Terminaste el texto. ¡Bien hecho!"; return }
        } else if (l < 0) {
            p--
            if (p < 0) return
            l = (layouts[p]?.lineCount ?: 1) - 1
        }
        ruler = p to l
        ensureVisible(p, l)
    }

    fun tapAt(frac: Float) {
        if (fsel.consumeTap()) return
        val vp = listState.layoutInfo.viewportSize.height.toFloat()
        // Foco de un párrafo: el centro avanza, el borde izquierdo vuelve y el derecho muestra los menús
        if (s.paraFocus) {
            when {
                frac > 0.72f -> onTapPlain()
                else -> {
                    focusPara = (focusPara + if (frac < 0.28f) -1 else 1).coerceIn(0, list.size - 1)
                    scope.launch { listState.animateScrollToItem(focusPara, -(vp * 0.22f).toInt()) }
                }
            }
            return
        }
        when {
            s.tapTurn && frac < 0.28f -> scope.launch { listState.animateScrollBy(-vp * 0.85f) }
            s.tapTurn && frac > 0.72f -> scope.launch { listState.animateScrollBy(vp * 0.85f) }
            else -> onTapPlain()
        }
    }

    // La posición se sigue sin volver a dibujar toda la vista en cada párrafo que pasa
    LaunchedEffect(listState, bridge) {
        androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex }.collect { bridge.position = it }
    }
    SideEffect {
        bridge.total = list.size
        bridge.unit = "Párr."
        bridge.move = { d -> move(d) }
        bridge.seek = { p -> listState.scrollToItem(p.coerceIn(0, list.size - 1)) }
        bridge.goPara = { p -> listState.scrollToItem(p.coerceIn(0, list.size - 1)) }
        bridge.goPage = null
        bridge.follow = { p -> listState.animateScrollToItem(p.coerceIn(0, list.size - 1)) }
        bridge.here = {
            val p = listState.firstVisibleItemIndex
            p to TextTools.firstWords(list[p].removePrefix("# "), 8)
        }
        bridge.speakFrom = { after ->
            val start = after?.plus(1) ?: (ruler?.first ?: listState.firstVisibleItemIndex)
            (start until minOf(list.size, start + 30)).map { it to list[it].removePrefix("# ") }
        }
        bridge.words = {
            val start = ruler?.first ?: listState.firstVisibleItemIndex
            TextTools.words(list.drop(start).take(400))
        }
        bridge.textBefore = { n ->
            val p = listState.firstVisibleItemIndex
            (maxOf(0, p - n)..minOf(p, list.size - 1)).map { list[it].removePrefix("# ") }
        }
        bridge.scrollBy = { px -> listState.scrollBy(px) }
        bridge.chapterEnd = {
            val p = listState.firstVisibleItemIndex
            ((p + 1) until list.size).firstOrNull { list[it].startsWith("# ") }
        }
    }

    Box(Modifier.fillMaxSize().background(mode.paper), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { o -> tapAt(o.x / size.width) } },
            // Espacio arriba y abajo para que las barras no tapen el inicio ni el final
            contentPadding = PaddingValues(start = s.margin.dp, end = s.margin.dp, top = 112.dp, bottom = 120.dp),
        ) {
            itemsIndexed(list, key = { i, _ -> i }) { i, p ->
                FlowParagraph(
                    text = p, s = s, mode = mode,
                    rulerLine = ruler?.takeIf { it.first == i }?.second,
                    rulerOn = s.ruler,
                    onLayout = { layouts[i] = it },
                    onTap = { line, fx -> if (fsel.sel == null && s.ruler && line != null) ruler = i to line else tapAt(fx) },
                    marks = marksFor(i, 0, p.removePrefix("# ").length),
                    onLongPress = { off -> study(i, off) },
                    selection = fsel.sel?.takeIf { it.para == i }?.let { it.start..it.end },
                    onPlaced = { co, l -> fsel.place(i, co, l) },
                    spoken = vm.ttsWord?.takeIf { it.first == i }?.second,
                    modifier = if (s.paraFocus && focusPara >= 0 && i != focusPara) Modifier.alpha(0.16f) else Modifier,
                )
            }
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Fin", style = MaterialTheme.typography.titleLarge, color = mode.muted)
                    OutlinedButton(onClick = {
                        vm.repo.update(book.id) { it.copy(done = true, progress = 1f, updated = System.currentTimeMillis()) }
                        vm.toast = "¡Un libro más!"
                    }) { Text("Marcar como terminado", color = mode.ink) }
                }
            }
        }
    }
}

/** Divide el texto en páginas que llenan la pantalla, midiendo cada párrafo con el mismo estilo con que se dibuja. */
@Composable
private fun rememberPagination(list: List<String>, s: ReaderSettings, mode: ReadMode, widthPx: Int, heightPx: Int): Pagination {
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val density = LocalDensity.current
    val pag = remember(list, s.font, s.weight, s.align, s.indent, s.fontSize, s.lineHeight, s.paraSpacing, s.bionic, widthPx, heightPx) { Pagination() }
    LaunchedEffect(pag) {
        if (widthPx <= 0 || heightPx <= 0) return@LaunchedEffect
        val cur = ArrayList<Chunk>()
        var y = 0f
        fun push() {
            if (cur.isNotEmpty()) pag.pages.add(cur.toList())
            cur.clear()
            y = 0f
        }
        val bodyLine = with(density) { (s.fontSize * s.lineHeight).sp.toPx() }
        for (pi in list.indices) {
            val raw = list[pi]
            val heading = raw.startsWith("# ")
            val body = if (heading) raw.substring(2) else raw
            // Medir fuera del hilo principal: con guiones y cortes de párrafo, un párrafo enorme tarda segundos
            val lay = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                measurer.measure(
                    paraText(body, heading, s), paraStyle(heading, s, mode.ink, body.length),
                    constraints = Constraints(maxWidth = widthPx),
                )
            }
            val gap = with(density) { paraGap(heading, s).toPx() }
            // Un título no se queda solo al final de la página
            if (heading && cur.isNotEmpty() && y + lay.size.height + bodyLine * 2 > heightPx) push()
            var line = 0
            while (line < lay.lineCount) {
                val top0 = lay.getLineTop(line)
                var last = line
                while (last < lay.lineCount && lay.getLineBottom(last) - top0 <= heightPx - y) last++
                if (last == line) {
                    if (cur.isEmpty()) last = line + 1 else { push(); continue }
                }
                val st = lay.getLineStart(line)
                val en = if (last >= lay.lineCount) body.length else lay.getLineStart(last)
                cur.add(Chunk(pi, st, en, top0, lay.getLineBottom(last - 1), last >= lay.lineCount))
                y += lay.getLineBottom(last - 1) - top0
                line = last
                if (line < lay.lineCount) push() else y += gap
            }
        }
        push()
        pag.done = true
    }
    return pag
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagedFlow(
    vm: HiloViewModel,
    book: Book,
    list: List<String>,
    s: ReaderSettings,
    mode: ReadMode,
    bridge: ReaderBridge,
    onTapPlain: () -> Unit,
    study: (para: Int, offset: Int) -> Unit,
    fsel: FlowSelection,
) {
    val c = LocalHilo.current
    val density = LocalDensity.current
    // Resaltados agrupados por párrafo: con miles de resaltados, cada párrafo busca solo los suyos
    val byPara by remember(book.id) { androidx.compose.runtime.derivedStateOf { vm.study.marks.filter { it.bookId == book.id }.groupBy { it.para } } }
    fun marksFor(para: Int, start: Int, end: Int): List<Pair<IntRange, Int>> = byPara[para].orEmpty().filter { it.end > start && it.start < end }
        .map { ((maxOf(it.start, start) - start) until (minOf(it.end, end) - start)) to it.color }
    val scope = rememberCoroutineScope()
    val layouts = remember(book.id) { HashMap<Int, TextLayoutResult>() }
    var ruler by remember(book.id) { mutableStateOf<PagedRuler?>(null) }
    var curPara by remember(book.id) { mutableIntStateOf(book.flowPos.coerceIn(0, list.size - 1)) }
    var pFocus by remember(book.id) { mutableIntStateOf(-1) }

    BoxWithConstraints(Modifier.fillMaxSize().background(mode.paper)) {
        // Con el celular acostado y «Dos páginas», se ven dos columnas como un libro abierto
        val two = s.twoPages && maxWidth > maxHeight && maxWidth > 600.dp
        val per = if (two) 2 else 1
        val gutter = if (two) (s.margin * 2 + 16).dp else 0.dp
        val colW = if (two) min((maxWidth - (s.margin * 2).dp - gutter) / 2, 620.dp) else min(maxWidth - (s.margin * 2).dp, 760.dp)
        val topPad = (10 + s.margin / 2).dp
        val footer = 26.dp
        val colH = maxHeight - topPad - footer
        val wPx = with(density) { colW.roundToPx() }
        val hPx = with(density) { colH.roundToPx() }
        val pag = rememberPagination(list, s, mode, wPx, hPx)
        val spreads = max(1, (pag.pages.size + per - 1) / per)
        val pagerState = rememberPagerState { spreads }
        var placed by remember(pag, per) { mutableStateOf(false) }

        fun pageOfPara(p: Int): Int = pag.pages.indexOfFirst { pg -> pg.any { it.para >= p } }
        fun curPage(): Int = pagerState.currentPage * per

        // Volver al mismo párrafo cuando se rearma la paginación (tamaño de letra, giro de pantalla)
        LaunchedEffect(pag, per) {
            val target = curPara
            while (true) {
                val idx = pageOfPara(target)
                if (idx >= 0) {
                    val lastChunkBefore = pag.pages.getOrNull(idx - 1)?.lastOrNull()
                    val use = if (lastChunkBefore != null && lastChunkBefore.para == target) idx - 1 else idx
                    pagerState.scrollToPage(use.coerceAtLeast(0) / per)
                    break
                }
                if (pag.done) break
                delay(40)
            }
            placed = true
        }

        // Guardar avance
        LaunchedEffect(pag, per) {
            snapshotFlow { pagerState.currentPage }.collectLatest { i ->
                if (!placed) return@collectLatest
                val first = pag.pages.getOrNull(i * per)?.firstOrNull() ?: return@collectLatest
                curPara = first.para
                delay(700)
                val prog = if (pag.done) ((i * per + per).toFloat() / pag.pages.size) else (first.para + 1f) / list.size
                vm.repo.update(book.id) {
                    it.copy(flowPos = first.para, flowOffset = 0, progress = prog.coerceIn(0.001f, 1f), updated = System.currentTimeMillis(), flowMode = it.type == "pdf")
                }
            }
        }

        // Líneas del párrafo que caen en el trozo k de la página pg
        fun lineRange(pg: Int, k: Int): IntRange? {
            val ch = pag.pages.getOrNull(pg)?.getOrNull(k) ?: return null
            val l = layouts[pg * 1000 + k] ?: return null
            return l.getLineForOffset(ch.start)..l.getLineForOffset(maxOf(ch.start, ch.end - 1))
        }
        LaunchedEffect(s.ruler) { if (s.ruler && ruler == null) ruler = PagedRuler(curPage(), 0, -1) }

        suspend fun goPara(p: Int) {
            var idx = pageOfPara(p)
            while (idx < 0 && !pag.done) { delay(40); idx = pageOfPara(p) }
            if (idx >= 0) pagerState.scrollToPage(idx / per)
        }

        suspend fun move(d: Int) {
            val r = ruler ?: PagedRuler(curPage(), 0, Int.MIN_VALUE)
            var pg = r.page
            var k = r.chunk
            val rg = lineRange(pg, k) ?: (0..0)
            var l = when (r.line) { Int.MIN_VALUE -> rg.first - 1; -1 -> rg.first; Int.MAX_VALUE -> rg.last; else -> r.line } + d
            if (l > rg.last) {
                k++
                if (k >= (pag.pages.getOrNull(pg)?.size ?: 0)) {
                    pg++; k = 0
                    if (pg >= pag.pages.size) { vm.toast = "Terminaste el texto. ¡Bien hecho!"; return }
                }
                l = lineRange(pg, k)?.first ?: -1
            } else if (l < rg.first) {
                k--
                if (k < 0) {
                    pg--
                    if (pg < 0) return
                    k = (pag.pages.getOrNull(pg)?.size ?: 1) - 1
                }
                l = lineRange(pg, k)?.last ?: Int.MAX_VALUE
            }
            ruler = PagedRuler(pg, k, l)
            if (pagerState.currentPage != pg / per) pagerState.animateScrollToPage(pg / per)
        }

        fun tapAt(frac: Float) {
            if (fsel.consumeTap()) return
            if (s.paraFocus) {
                if (frac > 0.72f) { onTapPlain(); return }
                val onPage = (curPage() until curPage() + per).flatMap { pg -> pag.pages.getOrNull(pg).orEmpty().map { it.para } }.distinct()
                val next = (pFocus + if (frac < 0.28f) -1 else 1).coerceIn(0, list.size - 1)
                pFocus = next
                if (onPage.isNotEmpty() && next !in onPage) scope.launch {
                    val target = pag.pages.indexOfFirst { pg -> pg.any { it.para == next } }
                    if (target >= 0) pagerState.animateScrollToPage(target / per)
                }
                return
            }
            when {
                s.tapTurn && frac < 0.28f -> scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) }
                s.tapTurn && frac > 0.72f -> scope.launch {
                    if (pagerState.currentPage >= spreads - 1 && pag.done) vm.toast = "Terminaste el texto. ¡Bien hecho!"
                    else pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
                else -> onTapPlain()
            }
        }

        LaunchedEffect(s.paraFocus) { if (s.paraFocus) pFocus = pag.pages.getOrNull(curPage())?.firstOrNull { it.start == 0 }?.para ?: pag.pages.getOrNull(curPage())?.firstOrNull()?.para ?: 0 }
        val cur = pagerState.currentPage
        SideEffect {
            bridge.position = cur
            bridge.total = spreads
            bridge.unit = "Pág."
            bridge.move = { d -> move(d) }
            bridge.seek = { p -> pagerState.scrollToPage(p.coerceIn(0, max(0, spreads - 1))) }
            bridge.goPara = { p -> goPara(p) }
            bridge.goPage = null
            bridge.follow = { p -> goPara(p) }
            bridge.here = {
                val p = pag.pages.getOrNull(curPage())?.firstOrNull()?.para ?: curPara
                p to TextTools.firstWords(list[p].removePrefix("# "), 8)
            }
            bridge.speakFrom = { after ->
                val start = after?.plus(1) ?: (pag.pages.getOrNull(curPage())?.firstOrNull()?.para ?: curPara)
                (start until minOf(list.size, start + 30)).map { it to list[it].removePrefix("# ") }
            }
            bridge.words = {
                val start = pag.pages.getOrNull(curPage())?.firstOrNull()?.para ?: curPara
                TextTools.words(list.drop(start).take(400))
            }
            bridge.textBefore = { n ->
                val cp = curPage()
                ((cp - n).coerceAtLeast(0)..cp).flatMap { pg -> pag.pages.getOrNull(pg).orEmpty().map { it.para } }.distinct()
                    .mapNotNull { list.getOrNull(it)?.removePrefix("# ") }
            }
            bridge.scrollBy = null
            bridge.chapterEnd = {
                val cp = pagerState.currentPage
                ((cp + 1) until spreads).firstOrNull { sp -> pag.pages.getOrNull(sp * per)?.any { ch -> ch.start == 0 && list.getOrNull(ch.para)?.startsWith("# ") == true } == true }
            }
        }

        // Una página: cada trozo es el párrafo entero recortado a sus líneas, así el justificado,
        // la sangría y los colores de línea quedan iguales aunque el párrafo siga en la página siguiente.
        @Composable
        fun PageColumn(pi: Int) {
            val chunks = pag.pages.getOrNull(pi).orEmpty()
            Column(Modifier.width(colW).height(colH)) {
                chunks.forEachIndexed { k, ch ->
                    val raw = list[ch.para]
                    val heading = raw.startsWith("# ")
                    val h = with(density) { (ch.bottom - ch.top).toDp() }
                    val r = ruler?.takeIf { it.page == pi && it.chunk == k }
                    val lay = layouts[pi * 1000 + k]
                    val rLine = r?.let { rr ->
                        when (rr.line) {
                            -1, Int.MIN_VALUE -> lay?.getLineForOffset(ch.start)
                            Int.MAX_VALUE -> lay?.getLineForOffset(maxOf(ch.start, ch.end - 1))
                            else -> rr.line
                        }
                    }
                    Box(Modifier.width(colW).height(h).clipToBounds()) {
                        FlowParagraph(
                            text = raw, s = s, mode = mode,
                            rulerLine = rLine,
                            rulerOn = s.ruler,
                            onLayout = { layouts[pi * 1000 + k] = it },
                            onTap = { line, fx -> if (fsel.sel == null && s.ruler && line != null) ruler = PagedRuler(pi, k, line) else tapAt(fx) },
                            marks = marksFor(ch.para, 0, raw.length + 1),
                            onLongPress = { off -> study(ch.para, off) },
                            selection = fsel.sel?.takeIf { it.para == ch.para }?.let { it.start..it.end },
                            spoken = vm.ttsWord?.takeIf { it.first == ch.para }?.second,
                            onPlaced = { co, l -> if (pi / per == pagerState.currentPage) fsel.place(ch.para, co, l, ch.start..ch.end) },
                            gap = false,
                            modifier = Modifier.wrapContentHeight(Alignment.Top, unbounded = true)
                                .offset { androidx.compose.ui.unit.IntOffset(0, -ch.top.roundToInt()) }
                                .then(if (s.paraFocus && pFocus >= 0 && ch.para != pFocus) Modifier.alpha(0.16f) else Modifier),
                        )
                    }
                    if (ch.endsPara && k < chunks.size - 1) Spacer(Modifier.height(paraGap(heading, s)))
                }
            }
        }

        // Al pasar de página se suelta el texto elegido
        LaunchedEffect(pagerState) { snapshotFlow { pagerState.currentPage }.collect { fsel.sel = null } }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { sp ->
            Box(
                Modifier.fillMaxSize().pageTurn(pagerState, sp).background(mode.paper)
                    .pointerInput(Unit) { detectTapGestures { o -> tapAt(o.x / size.width) } },
                contentAlignment = Alignment.TopCenter,
            ) {
                Row(Modifier.padding(top = topPad), horizontalArrangement = Arrangement.spacedBy(gutter)) {
                    for (j in 0 until per) {
                        val pi = sp * per + j
                        if (pi < pag.pages.size) PageColumn(pi) else Spacer(Modifier.width(colW))
                    }
                }
                Text(
                    if (per == 2) "${sp * 2 + 1}–${minOf(sp * 2 + 2, pag.pages.size)} / ${pag.pages.size}${if (pag.done) "" else "…"}"
                    else "${sp + 1} / ${pag.pages.size}${if (pag.done) "" else "…"}",
                    color = mode.muted, fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                )
            }
        }

        if (!placed) {
            Box(Modifier.fillMaxSize().background(mode.paper), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = c.accent)
                    Text("Armando las páginas…", color = mode.ink)
                }
            }
        }
    }
}

// ---------- palabra a palabra ----------

private fun pivotIndex(n: Int) = when {
    n <= 1 -> 0
    n <= 5 -> 1
    n <= 9 -> 2
    n <= 13 -> 3
    else -> 4
}

/** Las palabras aparecen una por una en el mismo punto, sin nada más que mirar. */
@Composable
fun RsvpOverlay(words: List<String>, s: ReaderSettings, onSpeed: (Int) -> Unit, onClose: () -> Unit) {
    val c = LocalHilo.current
    var i by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }

    LaunchedEffect(playing, i, s.wpm) {
        if (!playing) return@LaunchedEffect
        if (i >= words.size) { playing = false; return@LaunchedEffect }
        val w = words[i]
        var d = 60_000f / s.wpm
        if (Regex("[.!?…][»”\"')\\]]*$").containsMatchIn(w)) d *= 2.2f
        else if (Regex("[,;:—][»”\"')\\]]*$").containsMatchIn(w)) d *= 1.5f
        if (w.length > 8) d *= 1.25f
        delay(d.toLong())
        if (i < words.size - 1) i++ else playing = false
    }

    val w = words.getOrElse(i) { "" }
    val lead = w.indexOfFirst { it.isLetterOrDigit() }.coerceAtLeast(0)
    val coreLen = w.drop(lead).takeWhile { it.isLetterOrDigit() }.length
    val p = (lead + pivotIndex(coreLen)).coerceIn(0, (w.length - 1).coerceAtLeast(0))

    Column(
        Modifier.fillMaxSize().background(c.surface).windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Palabra a palabra", style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp), color = c.ink, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Salir") }
        }
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
            val big = TextStyle(fontFamily = Fonts.reading(s.font), fontSize = 40.sp, fontWeight = FontWeight.Medium, color = c.ink)
            Box(Modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(2.dp).height(14.dp).background(c.accent.copy(alpha = 0.5f)))
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (w.isEmpty()) "" else w.substring(0, p), style = big, textAlign = TextAlign.End, modifier = Modifier.weight(1f), maxLines = 1, softWrap = false)
                Text(if (w.isEmpty()) " " else w[p].toString(), style = big.copy(color = c.lines[0], fontWeight = FontWeight.Bold), maxLines = 1)
                Text(if (w.isEmpty()) "" else w.substring(p + 1), style = big, textAlign = TextAlign.Start, modifier = Modifier.weight(1f), maxLines = 1, softWrap = false)
            }
            Box(Modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(2.dp).height(14.dp).background(c.accent.copy(alpha = 0.5f)))
            }
            Text(
                "Palabra ${(i + 1).coerceAtMost(words.size)} de ${words.size}", color = c.muted, fontSize = 13.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { i = (i - 10).coerceAtLeast(0) }) { Icon(Icons.Rounded.Replay10, "Retroceder 10 palabras", tint = c.ink) }
            Spacer(Modifier.width(18.dp))
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(c.accent).clickable {
                    if (i >= words.size - 1) i = 0
                    playing = !playing
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pausar" else "Reproducir", tint = c.accentInk, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(18.dp))
            IconButton(onClick = { i = (i + 10).coerceAtMost((words.size - 1).coerceAtLeast(0)) }) { Icon(Icons.Rounded.Forward10, "Avanzar 10 palabras", tint = c.ink) }
        }
        SliderRow("Velocidad", "${s.wpm} palabras por minuto", s.wpm.toFloat(), 120f..600f, 23) { v ->
            onSpeed((v / 20).roundToInt() * 20)
        }
    }
}
