package com.hilo.lector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hilo.lector.HiloViewModel
import com.hilo.lector.data.Book
import com.hilo.lector.data.ReaderSettings
import com.hilo.lector.pdf.PdfEngine
import com.hilo.lector.text.TextTools
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.ceil
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

/**
 * Un párrafo con colores por línea: cada línea va del color del texto a un color y la siguiente
 * vuelve desde ese mismo color, así el ojo encuentra el inicio de la línea siguiente.
 */
@Composable
fun FlowParagraph(
    text: String,
    s: ReaderSettings,
    rulerLine: Int?,
    rulerOn: Boolean,
    onLayout: (TextLayoutResult) -> Unit,
    onTap: (Int?) -> Unit,
) {
    val c = LocalHilo.current
    val heading = text.startsWith("# ")
    val body = if (heading) text.substring(2) else text
    val fs = if (heading) s.fontSize * 1.25f else s.fontSize
    val annotated = remember(body, s.bionic, heading) {
        if (s.bionic && !heading) bionic(body) else AnnotatedString(body)
    }
    val palette = c.palette(s.palette)
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val tap by rememberUpdatedState(onTap)
    val style = TextStyle(
        fontFamily = if (heading) Fonts.display else Fonts.reading(s.font),
        fontWeight = if (heading) FontWeight.Bold else FontWeight.Normal,
        fontSize = fs.sp,
        lineHeight = (fs * s.lineHeight).sp,
        color = c.ink,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )
    val gap = (s.fontSize * (if (heading) 0.6f else s.paraSpacing)).dp

    Text(
        annotated,
        style = style,
        onTextLayout = { layout = it; onLayout(it) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = gap)
            .drawBehind {
                val l = layout
                if (rulerOn && rulerLine != null && l != null && rulerLine < l.lineCount) {
                    val t = l.getLineTop(rulerLine)
                    val b = l.getLineBottom(rulerLine)
                    drawRoundRect(
                        c.band, Offset(-10.dp.toPx(), t), Size(size.width + 20.dp.toPx(), b - t),
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
                        val full = lerp(c.ink, palette[(i / 2) % palette.size], s.intensity)
                        val c0 = if (i % 2 == 0) c.ink else full
                        val c1 = if (i % 2 == 0) full else c.ink
                        drawRect(
                            Brush.horizontalGradient(listOf(c0, c1), startX = 0f, endX = size.width),
                            topLeft = Offset(0f, t), size = Size(size.width, b - t), blendMode = BlendMode.SrcAtop,
                        )
                    }
                }
                if (rulerOn) {
                    val scrim = c.bg.copy(alpha = 0.8f)
                    if (rulerLine == null) {
                        drawRect(scrim)
                    } else {
                        for (i in 0 until l.lineCount) {
                            if (i == rulerLine) continue
                            val t = l.getLineTop(i)
                            drawRect(scrim, Offset(0f, t), Size(size.width, l.getLineBottom(i) - t))
                        }
                    }
                }
            }
            .pointerInput(rulerOn) {
                detectTapGestures { pos ->
                    val l = layout
                    tap(if (rulerOn && l != null) l.getLineForVerticalPosition(pos.y) else null)
                }
            },
    )
}

/** Texto fluido: el libro como párrafos continuos, con interlineado compacto. */
@Composable
fun FlowView(
    vm: HiloViewModel,
    book: Book,
    engine: PdfEngine?,
    s: ReaderSettings,
    bridge: ReaderBridge,
    onTapPlain: () -> Unit,
    onBackToPdf: (() -> Unit)?,
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
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Preparando el texto…", style = MaterialTheme.typography.titleLarge, color = c.ink)
            Spacer(Modifier.height(14.dp))
            val pr = progress
            if (pr != null && pr.second > 0) {
                LinearProgressIndicator(
                    progress = { (pr.first + 1f) / pr.second }, color = c.accent, trackColor = c.chip,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("Página ${pr.first + 1} de ${pr.second}", color = c.muted)
                Text("Las páginas escaneadas tardan más la primera vez.", color = c.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
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
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "No encontré texto en este documento. Si es escaneado, activa «Reconocer texto en escaneados» en Ajustes.",
                color = c.ink, textAlign = TextAlign.Center,
            )
            if (onBackToPdf != null) TextButton(onClick = onBackToPdf) { Text("Volver a la página original") }
        }
        return
    }

    val listState = rememberLazyListState(book.flowPos.coerceIn(0, list.size - 1), book.flowOffset)
    val layouts = remember(book.id) { HashMap<Int, TextLayoutResult>() }
    var ruler by remember(book.id) { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(list) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (i, o) ->
                delay(700)
                val prog = ((i + 1f) / list.size).coerceIn(0.001f, 1f)
                vm.repo.update(book.id) {
                    it.copy(
                        flowPos = i, flowOffset = o, progress = prog, updated = System.currentTimeMillis(),
                        flowMode = it.type == "pdf",
                    )
                }
            }
    }
    LaunchedEffect(s.ruler) {
        if (s.ruler && ruler == null) ruler = listState.firstVisibleItemIndex.coerceAtMost(list.size - 1) to 0
    }

    suspend fun ensureVisible(p: Int, l: Int) {
        var item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == p }
        if (item == null) {
            listState.scrollToItem(p)
            withFrameNanos { }
            item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == p } ?: return
        }
        val lay = layouts[p] ?: return
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

    SideEffect {
        bridge.move = { d -> move(d) }
        bridge.words = {
            val start = ruler?.first ?: listState.firstVisibleItemIndex
            TextTools.words(list.drop(start).take(400))
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = s.margin.dp, vertical = 18.dp),
        ) {
            itemsIndexed(list, key = { i, _ -> i }) { i, p ->
                FlowParagraph(
                    text = p, s = s,
                    rulerLine = ruler?.takeIf { it.first == i }?.second,
                    rulerOn = s.ruler,
                    onLayout = { layouts[i] = it },
                    onTap = { line -> if (s.ruler && line != null) ruler = i to line else onTapPlain() },
                )
            }
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Fin", style = MaterialTheme.typography.titleLarge, color = c.muted)
                    OutlinedButton(onClick = {
                        vm.repo.update(book.id) { it.copy(done = true, progress = 1f, updated = System.currentTimeMillis()) }
                        vm.toast = "¡Un libro más!"
                    }) { Text("Marcar como terminado", color = c.ink) }
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
