@file:OptIn(ExperimentalMaterial3Api::class)

package com.hilo.lector.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hilo.lector.HiloViewModel
import com.hilo.lector.Stats
import com.hilo.lector.data.Book
import com.hilo.lector.formatClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ---------- piezas pequeñas ----------

/** Interruptor tipo píldora para las ayudas de lectura. */
@Composable
fun AidChip(label: String, on: Boolean, onClick: () -> Unit) {
    val c = LocalHilo.current
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (on) c.surface else c.chip)
            .border(1.dp, if (on) c.accent else Color.Transparent, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier.size(9.dp).clip(CircleShape)
                .background(if (on) c.accent else Color.Transparent)
                .border(1.5.dp, if (on) c.accent else c.muted, CircleShape)
        )
        Text(label, fontSize = 13.sp, color = c.ink, fontFamily = Fonts.inter)
    }
}

/** Fila de opciones donde solo una está elegida. */
@Composable
fun ChoiceRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = LocalHilo.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) c.band else c.bg)
                    .border(1.dp, if (on) c.accent else c.line, RoundedCornerShape(12.dp))
                    .clickable { onSelect(i) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(o, fontSize = 14.sp, color = c.ink, fontFamily = Fonts.inter, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
fun Label(text: String, value: String? = null) {
    val c = LocalHilo.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text.uppercase(), fontSize = 11.sp, letterSpacing = 0.8.sp, color = c.muted, fontFamily = Fonts.inter)
        if (value != null) Text(value, fontSize = 13.sp, color = c.ink, fontFamily = Fonts.inter, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SliderRow(
    label: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>, steps: Int,
    onChange: (Float) -> Unit,
) {
    val c = LocalHilo.current
    Column {
        Label(label, value)
        Slider(
            value = v, onValueChange = onChange, valueRange = range, steps = steps,
            colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.chip),
        )
    }
}

@Composable
fun SwitchRow(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalHilo.current
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!on) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = c.ink, fontFamily = Fonts.inter, fontWeight = FontWeight.Medium)
            Text(sub, fontSize = 13.sp, color = c.muted, fontFamily = Fonts.inter)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = on, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.accentInk),
        )
    }
}

// ---------- ajustes ----------

@Composable
fun SettingsSheet(vm: HiloViewModel, onDismiss: () -> Unit) {
    val s by vm.settings.state.collectAsStateWithLifecycle()
    val c = LocalHilo.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Ajustes de lectura", style = MaterialTheme.typography.titleLarge, color = c.ink)

            val mode = readMode(s.colorMode)
            // Vista previa con el interlineado, el modo y los colores actuales
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(mode.paper)
                    .border(1.dp, c.line, RoundedCornerShape(14.dp))
                    .padding(horizontal = s.margin.coerceAtMost(24).dp, vertical = 12.dp)
            ) {
                FlowParagraph(
                    text = "Así se ve tu texto. Cada línea cambia de color y la siguiente empieza donde terminó la anterior, para que la vista no se pierda.",
                    s = s, mode = mode, rulerLine = null, rulerOn = false, onLayout = {}, onTap = { _, _ -> },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label("Modo de color", mode.name)
                ModeSwatches(s.colorMode) { i ->
                    vm.settings.update {
                        val m = readMode(i)
                        it.copy(colorMode = i, lastDayMode = if (m.dark) it.lastDayMode else i, lastNightMode = if (m.dark) i else it.lastNightMode)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label("Pasar páginas")
                ChoiceRow(listOf("Hacia abajo", "Horizontal"), s.pageMode) { i -> vm.settings.update { it.copy(pageMode = i) } }
            }
            if (s.pageMode == 1) {
                SwitchRow("Dos páginas con el celular acostado", "Solo en PDF, como un libro abierto.", s.twoPages) { on ->
                    vm.settings.update { it.copy(twoPages = on) }
                }
            }
            SwitchRow("Tocar los bordes para pasar", "Borde derecho: siguiente. Borde izquierdo: anterior. Centro: mostrar u ocultar menús.", s.tapTurn) { on ->
                vm.settings.update { it.copy(tapTurn = on) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label("Tipo de letra")
                ChoiceRow(Fonts.names, s.font) { i -> vm.settings.update { it.copy(font = i) } }
            }
            SliderRow("Tamaño", "${s.fontSize.roundToInt()}", s.fontSize, 14f..30f, 15) { v ->
                vm.settings.update { it.copy(fontSize = v.roundToInt().toFloat()) }
            }
            val lhName = when {
                s.lineHeight < 1.3f -> "Compacto"
                s.lineHeight < 1.6f -> "Normal"
                else -> "Amplio"
            }
            SliderRow("Interlineado", "$lhName · ${"%.1f".format(s.lineHeight)}", s.lineHeight, 1.1f..2.0f, 8) { v ->
                vm.settings.update { it.copy(lineHeight = (v * 10).roundToInt() / 10f) }
            }
            SliderRow("Espacio entre párrafos", "%.1f".format(s.paraSpacing), s.paraSpacing, 0f..1.5f, 5) { v ->
                vm.settings.update { it.copy(paraSpacing = (v * 10).roundToInt() / 10f) }
            }
            SliderRow("Márgenes", "${s.margin} dp", s.margin.toFloat(), 4f..48f, 10) { v ->
                vm.settings.update { it.copy(margin = v.roundToInt()) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label("Colores de línea")
                ChoiceRow(listOf("Arcoíris", "Frío", "Cálido"), s.palette) { i -> vm.settings.update { it.copy(palette = i) } }
            }
            SliderRow("Intensidad del color", "${(s.intensity * 100).roundToInt()}%", s.intensity, 0.4f..1f, 11) { v ->
                vm.settings.update { it.copy(intensity = (v * 20).roundToInt() / 20f) }
            }
            SwitchRow("Reconocer texto en escaneados", "Para que los colores y la regla funcionen en fotos y PDF escaneados.", s.ocr) { on ->
                vm.settings.update { it.copy(ocr = on) }
            }
            SliderRow("Palabra a palabra", "${s.wpm} ppm", s.wpm.toFloat(), 120f..600f, 23) { v ->
                vm.settings.update { it.copy(wpm = (v / 20).roundToInt() * 20) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label("Tema de los menús")
                ChoiceRow(listOf("Sistema", "Claro", "Oscuro"), s.theme) { i -> vm.settings.update { it.copy(theme = i) } }
            }
            SwitchRow("Mantener pantalla encendida", "Mientras estás leyendo.", s.keepOn) { on ->
                vm.settings.update { it.copy(keepOn = on) }
            }
            Label("Toques y pantalla")
            SwitchRow("Pantalla completa", "Oculta la barra de estado y la de navegación al leer.", s.fullScreen) { on ->
                vm.settings.update { it.copy(fullScreen = on) }
            }
            SwitchRow("Día y noche con un toque", "Toca la esquina superior izquierda para cambiar de modo.", s.cornerDayNight) { on ->
                vm.settings.update { it.copy(cornerDayNight = on) }
            }
            SwitchRow("Marcador con un toque", "Toca la esquina superior derecha para marcar la página.", s.cornerBookmark) { on ->
                vm.settings.update { it.copy(cornerBookmark = on) }
            }
            SwitchRow("Brillo deslizando", "Desliza arriba o abajo por el borde izquierdo.", s.brightnessSwipe) { on ->
                vm.settings.update { it.copy(brightnessSwipe = on) }
            }
            SwitchRow("Abrir el último libro al iniciar", "Sigue donde te quedaste apenas abres Hilo.", s.openLast) { on ->
                vm.settings.update { it.copy(openLast = on) }
            }
            SliderRow("Velocidad de la voz", "%.1f×".format(s.ttsRate), s.ttsRate, 0.5f..2f, 14) { v ->
                vm.settings.update { it.copy(ttsRate = (v * 10).roundToInt() / 10f) }
            }
            OutlinedButton(onClick = { vm.settings.reset() }, modifier = Modifier.fillMaxWidth()) {
                Text("Restablecer ajustes", color = c.ink)
            }
        }
    }
}

// ---------- temporizador ----------

@Composable
fun TimerSheet(vm: HiloViewModel, onDismiss: () -> Unit) {
    val s by vm.settings.state.collectAsStateWithLifecycle()
    val c = LocalHilo.current
    val t = vm.timer
    val today = Stats.today(LocalContext.current)
    val opts = listOf(10, 15, 25, 45)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Temporizador", style = MaterialTheme.typography.titleLarge, color = c.ink)
            if (!t.running) {
                Text("Lee en un bloque corto. Cuando suene, descansa 5 minutos y vuelve.", color = c.muted, fontFamily = Fonts.inter)
                ChoiceRow(opts.map { "$it min" }, opts.indexOf(s.timerMin)) { i -> vm.settings.update { it.copy(timerMin = opts[i]) } }
                Button(
                    onClick = { t.start(s.timerMin); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
                ) { Text("Empezar ${s.timerMin} min") }
            } else {
                Text(
                    formatClock(t.remaining), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    fontSize = 56.sp, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, color = c.ink,
                )
                Text(
                    if (t.isBreak) "Descanso" else "Leyendo", modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center, color = c.muted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { t.addFive() }, modifier = Modifier.weight(1f)) { Text("+5 min", color = c.ink) }
                    Button(
                        onClick = { t.stop() }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = c.lines[0], contentColor = Color.White),
                    ) { Text("Detener") }
                }
            }
            Label("Hoy")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(today.sessions to "sesiones", today.minutes to "minutos", today.pages to "páginas").forEach { (n, l) ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(c.bg).padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("$n", fontSize = 22.sp, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, color = c.ink)
                        Text(l, fontSize = 12.sp, color = c.muted)
                    }
                }
            }
        }
    }
}

// ---------- pegar texto ----------

@Composable
fun PasteDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf(initial) }
    val c = LocalHilo.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Pegar texto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Copia el texto en otra app (notas, navegador, un PDF) y mantén el dedo en el cuadro para pegarlo. Las líneas cortadas se unen solas.",
                    fontSize = 13.sp, color = c.muted,
                )
                OutlinedTextField(title, { title = it }, label = { Text("Título (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    text, { text = it }, label = { Text("Texto") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(title, text) }) { Text("Abrir en Hilo") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

// ---------- portadas ----------

@Composable
fun BookCover(vm: HiloViewModel, book: Book, modifier: Modifier = Modifier, initialSize: Int = 44) {
    val file = vm.repo.coverOf(book)
    val img by produceState<ImageBitmap?>(null, book.id) {
        value = withContext(Dispatchers.IO) {
            if (file.exists()) BitmapFactory.decodeFile(file.path)?.asImageBitmap() else null
        }
    }
    Box(modifier.aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp))) {
        val i = img
        if (i != null) {
            Image(i, null, Modifier.fillMaxSize().background(Color.White), contentScale = ContentScale.Crop)
        } else {
            GeneratedCover(book.title, initialSize)
        }
    }
}

/** Portada de colores con la inicial, para libros sin imagen. */
@Composable
fun GeneratedCover(title: String, initialSize: Int) {
    val c = LocalHilo.current
    var h = 7
    for (ch in title) h = h * 31 + ch.code
    val a = Math.floorMod(h, 5)
    var b = Math.floorMod(h shr 4, 5)
    if (b == a) b = (a + 2) % 5
    val initial = title.trimStart { !it.isLetterOrDigit() }.firstOrNull()?.uppercase() ?: "H"
    Box(
        Modifier.fillMaxSize()
            .background(Brush.linearGradient(listOf(c.lines[a], c.lines[b])))
            .drawBehind {
                val step = size.height / 9f
                var y = step
                while (y < size.height) {
                    drawLine(Color.White.copy(alpha = 0.16f), Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
                    y += step
                }
            }
            .padding(10.dp),
    ) {
        Text(initial, fontSize = initialSize.sp, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
    }
}

/** Muestras de los modos de color: papel con unas líneas de texto encima. */
@Composable
fun ModeSwatches(selected: Int, onSelect: (Int) -> Unit) {
    val c = LocalHilo.current
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        READ_MODES.forEachIndexed { i, m ->
            val on = i == selected
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(74.dp).clickable { onSelect(i) }) {
                Box(
                    Modifier.size(width = 64.dp, height = 80.dp).clip(RoundedCornerShape(10.dp)).background(m.paper)
                        .border(if (on) 3.dp else 1.dp, if (on) c.accent else c.line, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1f, 0.85f, 1f, 0.6f).forEach { f ->
                            Box(Modifier.fillMaxWidth(f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(m.ink))
                        }
                    }
                }
                Text(
                    m.name, fontSize = 11.sp, color = c.ink, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
