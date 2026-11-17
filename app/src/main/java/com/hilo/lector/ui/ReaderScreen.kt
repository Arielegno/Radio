package com.hilo.lector.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.ui.zIndex
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixNormal
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hilo.lector.Hilo
import com.hilo.lector.HiloViewModel
import com.hilo.lector.data.Book
import com.hilo.lector.formatClock
import com.hilo.lector.pdf.AnnotStore
import com.hilo.lector.pdf.PdfEngine
import com.hilo.lector.pdf.Tool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

private sealed interface EngineState {
    data object Loading : EngineState
    data class Ready(val engine: PdfEngine) : EngineState
    data object Failed : EngineState
}

@Composable
private fun rememberEngine(ctx: Context, file: File, id: String): EngineState {
    val st by produceState<EngineState>(EngineState.Loading, id) {
        val e = PdfEngine(ctx.applicationContext, file, id)
        val ok = try {
            e.open(); true
        } catch (ce: CancellationException) {
            Hilo.scope.launch { e.shutdown() }
            throw ce
        } catch (t: Throwable) {
            false
        }
        if (ok) {
            value = EngineState.Ready(e)
            awaitDispose { Hilo.scope.launch { e.shutdown() } }
        } else {
            value = EngineState.Failed
            Hilo.scope.launch { runCatching { e.shutdown() } }
        }
    }
    return st
}

private val PEN_COLORS = listOf(Color(0xFF1B2030), Color(0xFFC8372D), Color(0xFF2457C5), Color(0xFF1E8538))
private val HL_COLORS = listOf(Color(0xFFFFD54F), Color(0xFF9BE59F), Color(0xFFF7A8C8), Color(0xFF9CCBFF))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderScreen(vm: HiloViewModel, bookId: String) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val books by vm.repo.books.collectAsStateWithLifecycle()
    val book: Book? = books.firstOrNull { it.id == bookId }
    if (book == null) {
        LaunchedEffect(Unit) { vm.closeReader() }
        return
    }
    val s by vm.settings.state.collectAsStateWithLifecycle()
    val mode = readMode(s.colorMode)
    val isPdf = book.type == "pdf"
    var flow by rememberSaveable(bookId) { mutableStateOf(!isPdf || book.flowMode) }
    var chrome by remember { mutableStateOf(true) }
    var annotating by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(Tool.NONE) }
    var penColor by remember { mutableStateOf(PEN_COLORS[0]) }
    var hlColor by remember { mutableStateOf(HL_COLORS[0]) }
    var rsvp by remember { mutableStateOf<List<String>?>(null) }
    var contents by remember { mutableStateOf(false) }
    var goTo by remember { mutableStateOf(false) }
    var brightnessHint by remember { mutableStateOf<Int?>(null) }
    val bridge = remember { ReaderBridge() }
    val scope = rememberCoroutineScope()
    val compact = LocalConfiguration.current.screenHeightDp < 500

    val engineState: EngineState? = if (isPdf) rememberEngine(ctx, vm.repo.fileOf(book), book.id) else null
    val annots = remember(bookId) { if (isPdf) AnnotStore(ctx.applicationContext, bookId) else null }

    // Voz
    val speaker = remember { Speaker(ctx) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    fun speakFrom(after: Int?) {
        scope.launch {
            val items = bridge.speakFrom?.invoke(after).orEmpty()
            if (items.isEmpty()) { if (after == null) vm.toast = "No encontré texto para leer aquí."; return@launch }
            val lastPos = items.last().first
            val ok = speaker.speak(
                items, s.ttsRate,
                onItem = { pos -> scope.launch { bridge.follow?.invoke(pos) } },
                onEnd = { speakFrom(lastPos) },
            )
            if (!ok) vm.toast = "La voz del celular todavía no está lista. Prueba de nuevo en un momento."
        }
    }
    LaunchedEffect(flow) { speaker.stop() }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null && annots != null) vm.exportPdf(book, annots.strokes.toList(), annots.notes.toList(), uri)
    }

    BackHandler {
        when {
            rsvp != null -> rsvp = null
            annotating -> { annotating = false; tool = Tool.NONE }
            !chrome -> chrome = true
            else -> vm.closeReader()
        }
    }

    // Pantalla encendida, pantalla completa y brillo
    val view = LocalView.current
    DisposableEffect(s.keepOn) {
        view.keepScreenOn = s.keepOn
        onDispose { view.keepScreenOn = false }
    }
    val window = (ctx as? Activity)?.window
    LaunchedEffect(chrome, s.fullScreen) {
        val w = window ?: return@LaunchedEffect
        val ctl = WindowCompat.getInsetsController(w, w.decorView)
        if (chrome && !s.fullScreen) {
            ctl.show(WindowInsetsCompat.Type.systemBars())
        } else {
            ctl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctl.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            window?.let {
                WindowCompat.getInsetsController(it, it.decorView).show(WindowInsetsCompat.Type.systemBars())
                it.attributes = it.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
            }
        }
    }

    val toggleChrome = { if (!annotating) chrome = !chrome }
    val here = bridge.here?.invoke()
    val marked = here != null && book.bookmarks.any { it.flow == flow && it.pos == here.first }
    fun toggleMark() {
        val h = bridge.here?.invoke() ?: return
        vm.toggleBookmark(book, flow, h.first, h.second)
    }
    fun dayNight() {
        vm.settings.update { if (mode.dark) it.copy(colorMode = it.lastDayMode) else it.copy(colorMode = it.lastNightMode) }
        vm.toast = readMode(if (mode.dark) s.lastDayMode else s.lastNightMode).name
    }

    val noInsets = WindowInsets(0, 0, 0, 0)
    Box(Modifier.fillMaxSize().background(mode.paper)) {
        // Las barras flotan sobre la página: mostrarlas u ocultarlas no cambia el tamaño del texto
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().zIndex(2f)
                .background(if (chrome) c.bg else Color.Transparent)
                .windowInsetsPadding(if (chrome) WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.displayCutout) else noInsets)
        ) {
            AnimatedVisibility(visible = chrome) {
                TopBars(
                    book = book, isPdf = isPdf, flow = flow, compact = compact, annotating = annotating,
                    vm = vm,
                    onBack = { vm.closeReader() },
                    onFlow = { f ->
                        flow = f
                        annotating = false; tool = Tool.NONE
                        vm.repo.update(book.id) { it.copy(flowMode = f) }
                    },
                    onAnnotate = {
                        annotating = !annotating
                        tool = if (annotating) Tool.HIGHLIGHT else Tool.NONE
                    },
                    onRsvp = {
                        scope.launch {
                            val w = bridge.words?.invoke().orEmpty()
                            if (w.isEmpty()) vm.toast = "No encontré texto en esta parte." else rsvp = w
                        }
                    },
                )
            }

        }
            Box(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility.union(WindowInsets.displayCutout))
            ) {
                when {
                    !isPdf -> FlowView(vm, book, null, s, mode, bridge, toggleChrome, null)
                    engineState is EngineState.Ready && flow ->
                        FlowView(vm, book, engineState.engine, s, mode, bridge, toggleChrome) {
                            flow = false; vm.repo.update(book.id) { it.copy(flowMode = false) }
                        }
                    engineState is EngineState.Ready && annots != null ->
                        PdfView(vm, book, engineState.engine, annots, s, mode, tool, penColor, hlColor, bridge, toggleChrome)
                    engineState is EngineState.Failed ->
                        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No pude abrir este PDF. Puede estar dañado o protegido con contraseña.", color = mode.ink)
                            TextButton(onClick = { vm.closeReader() }) { Text("Volver a la biblioteca") }
                        }
                    else -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = c.accent)
                }

                if (tool == Tool.NONE) {
                    // Esquina superior izquierda: día / noche
                    if (s.cornerDayNight) {
                        Box(Modifier.align(Alignment.TopStart).size(56.dp).pointerInput(Unit) { detectTapGestures { dayNight() } })
                    }
                    // Esquina superior derecha: marcador
                    if (s.cornerBookmark) {
                        Box(Modifier.align(Alignment.TopEnd).size(56.dp).pointerInput(Unit) { detectTapGestures { toggleMark() } })
                    }
                    // Borde izquierdo: brillo
                    if (s.brightnessSwipe) {
                        Box(
                            Modifier.align(Alignment.CenterStart).padding(top = 56.dp).width(26.dp).fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragEnd = { brightnessHint = null },
                                        onDragCancel = { brightnessHint = null },
                                    ) { ch, dy ->
                                        ch.consume()
                                        val w = window ?: return@detectVerticalDragGestures
                                        var b = w.attributes.screenBrightness
                                        if (b < 0f) b = systemBrightness(ctx)
                                        b = (b - dy / size.height * 1.2f).coerceIn(0.02f, 1f)
                                        w.attributes = w.attributes.apply { screenBrightness = b }
                                        brightnessHint = (b * 100).roundToInt()
                                    }
                                }
                        )
                    }
                }
                if (marked) {
                    Icon(
                        Icons.Rounded.Bookmark, "Página con marcador", tint = c.lines[0],
                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 14.dp).size(26.dp),
                    )
                }
                brightnessHint?.let {
                    Text(
                        "Brillo $it%", color = Color.White, fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }

                if (s.ruler && tool == Tool.NONE) {
                    RulerButtons(
                        Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = if (chrome || annotating) 130.dp else 14.dp),
                        onUp = { scope.launch { bridge.move?.invoke(-1) } },
                        onDown = { scope.launch { bridge.move?.invoke(1) } },
                    )
                }
            }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().zIndex(2f)
                .background(if (chrome || annotating) c.bg else Color.Transparent)
                .windowInsetsPadding(if (chrome || annotating) WindowInsets.navigationBarsIgnoringVisibility else noInsets)
        ) {
            AnimatedVisibility(visible = annotating && !flow && isPdf) {
                AnnotBar(
                    tool = tool, penColor = penColor, hlColor = hlColor,
                    canUndo = annots?.strokes?.isNotEmpty() == true || annots?.notes?.isNotEmpty() == true,
                    onTool = { tool = it },
                    onPen = { penColor = it }, onHl = { hlColor = it },
                    onUndo = { annots?.undo() },
                    onSave = {
                        if (annots == null || annots.isEmpty) vm.toast = "Todavía no hay anotaciones para guardar."
                        else exportLauncher.launch("${book.title} (anotado).pdf")
                    },
                    onClose = { annotating = false; tool = Tool.NONE },
                )
            }
            AnimatedVisibility(visible = chrome && !annotating) {
                PageBar(
                    bridge = bridge, marked = marked, speaking = speaker.speaking,
                    onContents = { contents = true },
                    onMark = { toggleMark() },
                    onSpeak = { if (speaker.speaking) speaker.stop() else speakFrom(null) },
                    onGoTo = { goTo = true },
                    onSeek = { p -> scope.launch { bridge.seek?.invoke(p) } },
                )
            }
        }
    }

    if (contents) {
        ContentsSheet(
            vm = vm, book = book, flow = flow, isPdf = isPdf,
            engine = (engineState as? EngineState.Ready)?.engine, annots = annots,
            onDismiss = { contents = false },
            onGo = { targetFlow, pos ->
                contents = false
                if (targetFlow == flow) {
                    scope.launch { if (flow) bridge.goPara?.invoke(pos) else bridge.goPage?.invoke(pos) }
                } else {
                    vm.repo.update(book.id) { if (targetFlow) it.copy(flowPos = pos, flowOffset = 0, flowMode = true) else it.copy(pos = pos, posOffset = 0, flowMode = false) }
                    flow = targetFlow
                }
            },
        )
    }

    if (goTo) {
        var txt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { goTo = false },
            containerColor = c.surface,
            title = { Text(if (bridge.unit == "Pág.") "Ir a la página" else "Ir al párrafo") },
            text = {
                OutlinedTextField(
                    txt, { v -> txt = v.filter { it.isDigit() }.take(6) },
                    placeholder = { Text("1 a ${bridge.total}") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    txt.toIntOrNull()?.let { n -> scope.launch { bridge.seek?.invoke(n - 1) } }
                    goTo = false
                }) { Text("Ir") }
            },
            dismissButton = { TextButton(onClick = { goTo = false }) { Text("Cancelar") } },
        )
    }

    rsvp?.let { words ->
        RsvpOverlay(words, s, onSpeed = { v -> vm.settings.update { it.copy(wpm = v) } }, onClose = { rsvp = null })
    }
}

private fun systemBrightness(ctx: Context): Float = try {
    Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
} catch (e: Exception) {
    0.5f
}

/** Barra inferior: índice, marcador, posición con deslizador y lectura en voz alta. */
@Composable
private fun PageBar(
    bridge: ReaderBridge,
    marked: Boolean,
    speaking: Boolean,
    onContents: () -> Unit,
    onMark: () -> Unit,
    onSpeak: () -> Unit,
    onGoTo: () -> Unit,
    onSeek: (Int) -> Unit,
) {
    val c = LocalHilo.current
    val total = bridge.total.coerceAtLeast(1)
    var drag by remember { mutableStateOf<Float?>(null) }
    val shown = drag?.roundToInt() ?: bridge.position
    Column(Modifier.fillMaxWidth().background(c.bg).padding(horizontal = 6.dp).padding(top = 2.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onContents) { Icon(Icons.Rounded.Bookmarks, "Contenido y marcadores", tint = c.ink) }
            IconButton(onClick = onMark) {
                Icon(if (marked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, if (marked) "Quitar marcador" else "Agregar marcador", tint = if (marked) c.lines[0] else c.ink)
            }
            Text(
                "${bridge.unit} ${shown + 1} de $total",
                color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(onClick = onGoTo).padding(vertical = 6.dp),
            )
            IconButton(onClick = onSpeak) {
                Icon(if (speaking) Icons.Rounded.Stop else Icons.Rounded.RecordVoiceOver, if (speaking) "Detener la voz" else "Escuchar", tint = if (speaking) c.accent else c.ink)
            }
        }
        // Siempre ocupa el mismo alto, para que el texto no se vuelva a dividir en páginas
        Box(Modifier.height(28.dp).fillMaxWidth()) {
            if (total > 1) Slider(
                value = (drag ?: bridge.position.toFloat()).coerceIn(0f, (total - 1).toFloat()),
                onValueChange = { drag = it },
                onValueChangeFinished = { drag?.let { onSeek(it.roundToInt()) }; drag = null },
                valueRange = 0f..(total - 1).toFloat(),
                colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.chip),
                modifier = Modifier.padding(horizontal = 12.dp).height(28.dp),
            )
        }
    }
}

/** Índice del libro, marcadores y notas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentsSheet(
    vm: HiloViewModel,
    book: Book,
    flow: Boolean,
    isPdf: Boolean,
    engine: PdfEngine?,
    annots: AnnotStore?,
    onDismiss: () -> Unit,
    onGo: (flow: Boolean, pos: Int) -> Unit,
) {
    val c = LocalHilo.current
    var tab by remember { mutableIntStateOf(0) }
    val toc by produceState<List<Pair<String, Int>>?>(null, flow, book.id) {
        value = if (isPdf && !flow) {
            engine?.outline().orEmpty()
        } else {
            vm.flowParas(book, engine) { _, _ -> }
                .mapIndexedNotNull { i, p -> if (p.startsWith("# ")) p.substring(2).take(90) to i else null }
        }
    }
    val tabs = listOfNotNull("Contenido", "Marcadores", if (isPdf) "Notas" else null)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                tabs.forEachIndexed { i, t ->
                    val on = i == tab
                    Text(
                        t, color = if (on) c.accentInk else c.ink, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.accent else c.chip)
                            .clickable { tab = i }.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                when (tab) {
                    0 -> {
                        val list = toc
                        if (list == null) item { Text("Buscando el índice…", color = c.muted, modifier = Modifier.padding(12.dp)) }
                        else if (list.isEmpty()) item {
                            Text(
                                if (isPdf && !flow) "Este PDF no trae índice. Usa la barra de páginas o los marcadores."
                                else "No encontré títulos en este texto.",
                                color = c.muted, modifier = Modifier.padding(12.dp),
                            )
                        }
                        else items(list.size) { i ->
                            val (t, pos) = list[i]
                            ContentsRow(t, if (isPdf && !flow) "pág. ${pos + 1}" else "") { onGo(flow, pos) }
                        }
                    }
                    1 -> {
                        if (book.bookmarks.isEmpty()) item {
                            Text("Todavía no hay marcadores. Toca el marcador abajo o la esquina superior derecha de la página.", color = c.muted, modifier = Modifier.padding(12.dp))
                        }
                        else items(book.bookmarks.size) { i ->
                            val b = book.bookmarks[i]
                            ContentsRow(b.label, if (b.flow) "Texto" else "Página") { onGo(b.flow, b.pos) }
                        }
                    }
                    else -> {
                        val notes = annots?.notes?.sortedBy { it.page }.orEmpty()
                        if (notes.isEmpty()) item { Text("Todavía no hay notas en este PDF.", color = c.muted, modifier = Modifier.padding(12.dp)) }
                        else items(notes.size) { i ->
                            val n = notes[i]
                            ContentsRow(n.text, "pág. ${n.page + 1}") { onGo(false, n.page) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentsRow(title: String, meta: String, onClick: () -> Unit) {
    val c = LocalHilo.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = c.ink, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (meta.isNotEmpty()) Text(meta, color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun TopBars(
    book: Book,
    isPdf: Boolean,
    flow: Boolean,
    compact: Boolean,
    annotating: Boolean,
    vm: HiloViewModel,
    onBack: () -> Unit,
    onFlow: (Boolean) -> Unit,
    onAnnotate: () -> Unit,
    onRsvp: () -> Unit,
) {
    val c = LocalHilo.current
    val s by vm.settings.state.collectAsStateWithLifecycle()
    val t = vm.timer

    val actions: @Composable () -> Unit = {
        IconButton(onClick = { vm.sheet = "timer" }) {
            if (t.running) {
                Text(
                    formatClock(t.remaining), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (t.isBreak) c.lines[3] else c.accent,
                )
            } else Icon(Icons.Rounded.Timer, "Temporizador", tint = c.ink)
        }
        IconButton(onClick = { vm.sheet = "settings" }) { Icon(Icons.Rounded.TextFields, "Ajustes de lectura", tint = c.ink) }
        if (isPdf && !flow) {
            IconButton(onClick = onAnnotate) {
                Icon(Icons.Rounded.Brush, "Anotar sobre el PDF", tint = if (annotating) c.accent else c.ink)
            }
        }
        Box(
            Modifier.padding(start = 4.dp, end = 4.dp).size(40.dp).clip(CircleShape).background(c.accent).clickable(onClick = onRsvp),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.PlayArrow, "Leer palabra por palabra", tint = c.accentInk) }
    }

    val chips: @Composable () -> Unit = {
        AidChip("Colores", s.colors) { vm.settings.update { it.copy(colors = !it.colors) } }
        if (flow) AidChip("Negrita guía", s.bionic) { vm.settings.update { it.copy(bionic = !it.bionic) } }
        AidChip("Regla", s.ruler) {
            vm.settings.update { it.copy(ruler = !it.ruler) }
            if (!s.ruler) vm.toast = "Toca una línea para enfocarla, o usa las flechas."
        }
        if (isPdf) ModeToggle(flow, onFlow)
    }

    Column(
        Modifier.fillMaxWidth().background(c.bg).padding(horizontal = 6.dp).padding(top = 2.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Biblioteca", tint = c.ink) }
            Column(Modifier.weight(1f)) {
                Text(book.title, color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!compact) {
                    Text(
                        if (flow) "Texto fluido · ${(book.progress * 100).toInt()}%" else "Página original · ${(book.progress * 100).toInt()}%",
                        color = c.muted, fontSize = 12.sp, maxLines = 1,
                    )
                }
            }
            if (compact) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { chips() }
            }
            actions()
        }
        if (!compact) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { chips() }
        }
    }
}

/** Cambia entre la página original del PDF y el texto fluido. */
@Composable
private fun ModeToggle(flow: Boolean, onFlow: (Boolean) -> Unit) {
    val c = LocalHilo.current
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(c.chip).padding(3.dp),
    ) {
        listOf(false to "Página", true to "Texto").forEach { (f, label) ->
            val on = f == flow
            Text(
                label, fontSize = 13.sp, color = if (on) c.accentInk else c.ink,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.accent else Color.Transparent)
                    .clickable { onFlow(f) }.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun AnnotBar(
    tool: Tool,
    penColor: Color,
    hlColor: Color,
    canUndo: Boolean,
    onTool: (Tool) -> Unit,
    onPen: (Color) -> Unit,
    onHl: (Color) -> Unit,
    onUndo: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val c = LocalHilo.current
    Column(
        Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Todas las herramientas caben a lo ancho, sin desplazar
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val m = Modifier.weight(1f)
            ToolButton(Icons.Rounded.PanTool, "Mover", tool == Tool.NONE, m) { onTool(Tool.NONE) }
            ToolButton(Icons.Rounded.BorderColor, "Resaltar", tool == Tool.HIGHLIGHT, m) { onTool(Tool.HIGHLIGHT) }
            ToolButton(Icons.Rounded.Edit, "Lápiz", tool == Tool.PEN, m) { onTool(Tool.PEN) }
            ToolButton(Icons.Rounded.AutoFixNormal, "Borrar", tool == Tool.ERASER, m) { onTool(Tool.ERASER) }
            ToolButton(Icons.Rounded.PushPin, "Nota", tool == Tool.NOTE, m) { onTool(Tool.NOTE) }
            ToolButton(Icons.AutoMirrored.Rounded.Undo, "Deshacer", false, m, enabled = canUndo, onClick = onUndo)
            ToolButton(Icons.Rounded.Save, "Guardar", false, m, onClick = onSave)
            ToolButton(Icons.Rounded.Close, "Listo", false, m, onClick = onClose)
        }
        if (tool == Tool.HIGHLIGHT || tool == Tool.PEN) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (colors, cur, pick) = if (tool == Tool.HIGHLIGHT) Triple(HL_COLORS, hlColor, onHl) else Triple(PEN_COLORS, penColor, onPen)
                colors.forEach { col ->
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(col)
                            .border(if (col == cur) 3.dp else 1.dp, if (col == cur) c.accent else c.line, CircleShape)
                            .clickable { pick(col) },
                    )
                }
                Text(
                    if (tool == Tool.HIGHLIGHT) "Arrastra sobre el texto: se ajusta a las líneas." else "Dibuja o escribe sobre la página.",
                    color = c.muted, fontSize = 12.sp, modifier = Modifier.weight(1f),
                )
            }
        } else if (tool != Tool.NONE) {
            Text(
                when (tool) {
                    Tool.ERASER -> "Toca o arrastra sobre un trazo para borrarlo."
                    Tool.NOTE -> "Toca la página donde quieras dejar una nota."
                    else -> ""
                },
                color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        } else {
            Text(
                "Modo mover: desplázate y pellizca para hacer zoom.",
                color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector, label: String, on: Boolean, modifier: Modifier = Modifier,
    enabled: Boolean = true, onClick: () -> Unit,
) {
    val c = LocalHilo.current
    Column(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(if (on) c.band else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (!enabled) c.muted.copy(alpha = 0.4f) else if (on) c.accent else c.ink, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 10.sp, maxLines = 1, color = if (enabled) c.ink else c.muted.copy(alpha = 0.5f))
    }
}
