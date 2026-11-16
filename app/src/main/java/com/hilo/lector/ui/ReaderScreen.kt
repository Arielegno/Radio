package com.hilo.lector.ui

import android.app.Activity
import android.content.Context
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
    val isPdf = book.type == "pdf"
    var flow by rememberSaveable(bookId) { mutableStateOf(!isPdf || book.flowMode) }
    var chrome by remember { mutableStateOf(true) }
    var annotating by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(Tool.NONE) }
    var penColor by remember { mutableStateOf(PEN_COLORS[0]) }
    var hlColor by remember { mutableStateOf(HL_COLORS[0]) }
    var rsvp by remember { mutableStateOf<List<String>?>(null) }
    val bridge = remember { ReaderBridge() }
    val scope = rememberCoroutineScope()
    val compact = LocalConfiguration.current.screenHeightDp < 500

    val engineState: EngineState? = if (isPdf) rememberEngine(ctx, vm.repo.fileOf(book), book.id) else null
    val annots = remember(bookId) { if (isPdf) AnnotStore(ctx.applicationContext, bookId) else null }

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

    // Pantalla encendida y pantalla completa al ocultar las barras
    val view = LocalView.current
    DisposableEffect(s.keepOn) {
        view.keepScreenOn = s.keepOn
        onDispose { view.keepScreenOn = false }
    }
    val window = (ctx as? Activity)?.window
    LaunchedEffect(chrome) {
        val w = window ?: return@LaunchedEffect
        val ctl = WindowCompat.getInsetsController(w, w.decorView)
        if (chrome) {
            ctl.show(WindowInsetsCompat.Type.systemBars())
        } else {
            ctl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctl.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            window?.let { WindowCompat.getInsetsController(it, it.decorView).show(WindowInsetsCompat.Type.systemBars()) }
        }
    }

    val toggleChrome = { if (!annotating) chrome = !chrome }

    Box(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.fillMaxSize()) {
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

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    !isPdf -> FlowView(vm, book, null, s, bridge, toggleChrome, null)
                    engineState is EngineState.Ready && flow ->
                        FlowView(vm, book, engineState.engine, s, bridge, toggleChrome) { flow = false; vm.repo.update(book.id) { it.copy(flowMode = false) } }
                    engineState is EngineState.Ready && annots != null ->
                        PdfView(vm, book, engineState.engine, annots, s, tool, penColor, hlColor, bridge, toggleChrome)
                    engineState is EngineState.Failed ->
                        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No pude abrir este PDF. Puede estar dañado o protegido con contraseña.", color = c.ink)
                            TextButton(onClick = { vm.closeReader() }) { Text("Volver a la biblioteca") }
                        }
                    else -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = c.accent)
                }

                if (s.ruler && tool == Tool.NONE) {
                    RulerButtons(
                        Modifier.align(Alignment.BottomEnd).padding(14.dp),
                        onUp = { scope.launch { bridge.move?.invoke(-1) } },
                        onDown = { scope.launch { bridge.move?.invoke(1) } },
                    )
                }
            }

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
        }
    }

    rsvp?.let { words ->
        RsvpOverlay(words, s, onSpeed = { v -> vm.settings.update { it.copy(wpm = v) } }, onClose = { rsvp = null })
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
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ToolButton(Icons.Rounded.PanTool, "Mover", tool == Tool.NONE) { onTool(Tool.NONE) }
            ToolButton(Icons.Rounded.BorderColor, "Resaltar", tool == Tool.HIGHLIGHT) { onTool(Tool.HIGHLIGHT) }
            ToolButton(Icons.Rounded.Edit, "Lápiz", tool == Tool.PEN) { onTool(Tool.PEN) }
            ToolButton(Icons.Rounded.AutoFixNormal, "Borrar", tool == Tool.ERASER) { onTool(Tool.ERASER) }
            ToolButton(Icons.Rounded.PushPin, "Nota", tool == Tool.NOTE) { onTool(Tool.NOTE) }
            Spacer(Modifier.width(6.dp))
            ToolButton(Icons.AutoMirrored.Rounded.Undo, "Deshacer", false, enabled = canUndo, onClick = onUndo)
            ToolButton(Icons.Rounded.Save, "Guardar PDF", false, onClick = onSave)
            ToolButton(Icons.Rounded.Close, "Listo", false, onClick = onClose)
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
private fun ToolButton(icon: ImageVector, label: String, on: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val c = LocalHilo.current
    Column(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(if (on) c.band else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (!enabled) c.muted.copy(alpha = 0.4f) else if (on) c.accent else c.ink, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 11.sp, color = if (enabled) c.ink else c.muted.copy(alpha = 0.5f))
    }
}
