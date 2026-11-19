package com.hilo.lector.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hilo.lector.FocusTimer
import com.hilo.lector.HiloViewModel
import com.hilo.lector.formatClock
import com.hilo.lector.data.Book
import com.hilo.lector.data.Entry
import com.hilo.lector.data.today
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Random
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

// =====================================================================
// Datos: minutos leídos, racha, velocidad, resúmenes
// =====================================================================

/** Racha amable: días seguidos con lectura o repaso; hasta 2 días de descanso por semana no la rompen. */
class Streak(val days: Int, val restLeft: Int, val last14: List<Int>)   // 0 nada, 1 descanso, 2 activo

object Adhd {
    fun minutes(vm: HiloViewModel, day: Long = today()): Int = vm.motor["min:$day"].toIntOrNull() ?: 0

    /** Suma un minuto de lectura al día de hoy. Devuelve el total de hoy. */
    fun addMinute(vm: HiloViewModel): Int {
        val d = today()
        val n = minutes(vm, d) + 1
        vm.motor.put("min:$d", n.toString())
        return n
    }

    private fun active(vm: HiloViewModel, day: Long, reviewDays: Set<Long>) = minutes(vm, day) >= 5 || day in reviewDays

    fun streak(vm: HiloViewModel): Streak {
        val t = today()
        val reviewDays = vm.motor.reviews.map { it.day }.toHashSet()
        val state = HashMap<Long, Int>()
        val rests = ArrayList<Long>()
        var d = if (active(vm, t, reviewDays)) t else t - 1
        if (d != t) state[t] = 0
        var n = 0
        while (t - d < 400) {
            if (active(vm, d, reviewDays)) { n++; state[d] = 2 }
            else if (rests.count { it in d..(d + 6) } < 2 && n > 0) { rests.add(d); state[d] = 1 }
            else break
            d--
        }
        // Los descansos al final de la racha (antes de empezar) no cuentan
        while (rests.isNotEmpty() && rests.last() == d + 1) { state.remove(rests.removeAt(rests.size - 1)); d++ }
        val usedThisWeek = rests.count { it > t - 7 }
        val last14 = (13 downTo 0).map { k -> state[t - k] ?: if (active(vm, t - k, reviewDays)) 2 else 0 }
        return Streak(n, (2 - usedThisWeek).coerceAtLeast(0), last14)
    }

    /** Velocidad de lectura por libro: unidades (páginas o párrafos) por minuto. */
    fun speed(vm: HiloViewModel, bookId: String, unit: String): Float? {
        val v = vm.motor["vel:$bookId"].split('|')
        return if (v.size == 2 && v[0] == unit) v[1].toFloatOrNull()?.takeIf { it > 0f } else null
    }

    fun learnSpeed(vm: HiloViewModel, bookId: String, unit: String, delta: Int) {
        if (delta <= 0 || delta > 60) return
        val old = speed(vm, bookId, unit)
        val v = if (old == null) delta.toFloat() else old * 0.75f + delta * 0.25f
        vm.motor.put("vel:$bookId", "$unit|$v")
    }

    fun fmtMin(m: Float): String = when {
        m < 1.5f -> "1 min"
        m < 60f -> "${m.roundToInt()} min"
        else -> { val h = (m / 60f).toInt(); val r = (m - h * 60).roundToInt(); if (r == 0) "$h h" else "$h h $r min" }
    }

    private val SMALL = setOf("porque", "cuando", "donde", "sobre", "entre", "desde", "hasta", "también", "puede", "pueden", "estos", "estas", "otros", "otras", "todos", "todas", "mismo", "misma", "siempre", "nunca", "parte", "manera", "forma", "hacer", "había", "habían", "tiene", "tienen", "decir", "cada", "aunque", "mientras", "según", "tanto", "solo", "sólo", "luego", "ahora", "entonces")

    /** Las [n] oraciones que mejor resumen estos textos (por las palabras que más se repiten), en su orden. */
    fun keySentences(texts: List<String>, n: Int = 3): List<String> {
        val all = texts.joinToString(" ")
        val freq = HashMap<String, Int>()
        Regex("[\\p{L}]{5,}").findAll(all.lowercase()).forEach { m -> if (m.value !in SMALL) freq[m.value] = (freq[m.value] ?: 0) + 1 }
        val sents = texts.flatMap { it.split(Regex("(?<=[.!?…])\\s+")) }.map { it.trim() }.filter { it.length in 40..320 }
        if (sents.isEmpty()) return texts.map { it.take(220) }.filter { it.isNotBlank() }.take(n)
        val scored = sents.mapIndexed { i, s ->
            val ws = Regex("[\\p{L}]{5,}").findAll(s.lowercase()).map { it.value }.toSet()
            Triple(i, s, ws.sumOf { (freq[it] ?: 0).coerceAtMost(12) } / sqrt(max(6.0, s.length / 6.0)))
        }
        return scored.sortedByDescending { it.third }.take(n).sortedBy { it.first }.map { it.second }
    }
}

// =====================================================================
// Sonidos de fondo hechos por el celular (sin archivos ni internet)
// =====================================================================

object NoisePlayer {
    @Volatile private var kind = ""
    @Volatile var volume = 0.35f
    private var thread: Thread? = null

    fun play(k: String, vol: Float) {
        volume = vol
        if (k == kind && thread?.isAlive == true) return
        stop()
        if (k.isEmpty()) return
        kind = k
        thread = Thread({ loop(k) }, "hilo-sonido").apply { isDaemon = true; start() }
    }

    fun stop() {
        kind = ""
        runCatching { thread?.join(400) }
        thread = null
    }

    private fun loop(k: String) {
        val rate = 22050
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(min * 2).setTransferMode(AudioTrack.MODE_STREAM).build()
        }.getOrNull() ?: return
        val r = Random()
        val out = ShortArray(1024)
        var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var brown = 0.0; var drop = 0.0; var fade = 0.0
        runCatching {
            track.play()
            while (kind == k) {
                for (i in out.indices) {
                    val w = r.nextGaussian() * 0.3
                    b0 = 0.99765 * b0 + w * 0.0990460
                    b1 = 0.96300 * b1 + w * 0.2965164
                    b2 = 0.57000 * b2 + w * 1.0526913
                    val pink = (b0 + b1 + b2 + w * 0.1848) * 0.2
                    brown = (brown + 0.02 * w) / 1.02
                    val x = when (k) {
                        "blanco" -> w * 0.45
                        "rosa" -> pink
                        "lluvia" -> {
                            if (r.nextDouble() < 0.0009) drop = 0.25 + r.nextDouble() * 0.45
                            drop *= 0.993
                            pink * 0.55 + w * drop
                        }
                        else -> brown * 4.0   // marrón
                    }
                    fade = (fade + 1.0 / rate).coerceAtMost(1.0)   // entra suave, sin golpe
                    out[i] = (x * volume * fade * 32767).coerceIn(-32767.0, 32767.0).toInt().toShort()
                }
                track.write(out, 0, out.size)
            }
        }
        runCatching { track.stop(); track.release() }
    }
}

// =====================================================================
// Panel «Ayudas para concentrarte»
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AdhdSheet(
    vm: HiloViewModel,
    book: Book,
    bridge: ReaderBridge,
    flow: Boolean,
    speaking: Boolean,
    onSpeak: () -> Unit,
    onRecap: () -> Unit,
    onNotebook: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalHilo.current
    val s = vm.settings.value
    var friend by remember { mutableStateOf(false) }
    var energy by remember { mutableStateOf(false) }
    val today = Adhd.minutes(vm)
    val notes = vm.motor.entriesOf("distraccion").filter { it.score != 1 }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(
            Modifier.padding(horizontal = 18.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ayudas para concentrarte", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.ink, modifier = Modifier.weight(1f))
                HelpButton(vm, "tdah")
            }
            Text("Activa una o dos a la vez. Cada una tiene su «?».", color = c.muted, fontSize = 13.sp)

            // ---------- Empezar ----------
            AdhdGroup("Empezar")
            AdhdRow(vm, "Solo 2 minutos", "Un bloque cortito para arrancar. Después decides si sigues.", "dos-min") {
                SmallBtn("Empezar") { vm.twoMin = true; vm.timer.start(2); onDismiss() }
            }
            AdhdRow(vm, "¿Dónde me quedé?", "Las ideas de lo último que leíste y tus subrayados.", "donde-quede") {
                SmallBtn("Ver") { onRecap(); onDismiss() }
            }
            AdhdRow(vm, "Meta de hoy", "Llevas $today de ${s.goalMin} min.", "meta") {}
            GoalBar(today, s.goalMin)
            ChipsRow(listOf(5, 10, 15, 20, 30), s.goalMin, { "$it min" }) { m -> vm.settings.update { it.copy(goalMin = m) } }

            // ---------- Mientras lees ----------
            AdhdGroup("Mientras lees")
            AdhdRow(vm, "Foco de un párrafo", if (flow) "Atenúa todo menos el párrafo que lees. Toca el centro para avanzar." else "Funciona en el modo Texto (cámbialo arriba).", "foco-parrafo") {
                Switch(s.paraFocus, { on -> vm.settings.update { it.copy(paraFocus = on) } }, enabled = flow)
            }
            AdhdRow(vm, "Voz con resaltado", "Lee en voz alta y marca cada palabra, como un karaoke.", "voz-resaltado") {
                SmallBtn(if (speaking) "Detener" else "Escuchar") { onSpeak(); if (!speaking) onDismiss() }
            }
            if (flow) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Marcar la palabra que suena", color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Switch(s.ttsHighlight, { on -> vm.settings.update { it.copy(ttsHighlight = on) } })
            }
            AdhdRow(vm, "Desplazamiento automático", "El texto avanza solo, como un teleprompter.", "teleprompter") {
                SmallBtn(if (vm.autoScroll > 0) "Detener" else "Iniciar") { vm.autoScroll = if (vm.autoScroll > 0) 0 else 3; if (vm.autoScroll > 0) onDismiss() }
            }
            AdhdRow(vm, "Preguntas de control", "Cada cierto tramo: ¿de qué trataba? Para notar si leíste sin entender.", "control") {}
            ChipsRow(listOf(0, 3, 5, 10), s.checkEvery, { if (it == 0) "Apagado" else "Cada $it pág." }) { n -> vm.settings.update { it.copy(checkEvery = n) } }
            AdhdRow(vm, "Libreta de distracciones", if (notes.isEmpty()) "Anota lo que se te cruza por la cabeza y sigue leyendo." else "Tienes ${notes.size} ${if (notes.size == 1) "pendiente" else "pendientes"}.", "distracciones") {
                SmallBtn("Abrir") { onNotebook(); onDismiss() }
            }

            // ---------- Tiempo ----------
            AdhdGroup("Tiempo")
            AdhdRow(vm, "Temporizador visual", "Un círculo que se vacía arriba de la página.", "timer-visual") {
                Switch(s.visualTimer, { on -> vm.settings.update { it.copy(visualTimer = on) } })
            }
            ChipsRow(listOf(5, 10, 15, 25, 45), if (vm.timer.running) vm.timer.minutes else -1, { "$it min" }) { m -> vm.timer.start(m); onDismiss() }
            val sp = Adhd.speed(vm, book.id, bridge.unit)
            AdhdRow(
                vm, "Cuánto falta",
                if (sp == null) "Lee unos minutos y Hilo calcula cuánto te falta según tu ritmo."
                else {
                    val left = (bridge.total - 1 - bridge.position).coerceAtLeast(0)
                    val ch = bridge.chapterEnd?.invoke()?.let { e -> (e - bridge.position).coerceAtLeast(0) }
                    (if (ch != null && ch in 1 until left) "~${Adhd.fmtMin(ch / sp)} para terminar el capítulo · " else "") + "~${Adhd.fmtMin(left / sp)} para terminar el libro."
                },
                "tiempo-falta",
            ) {}
            AdhdRow(vm, "Aviso de pausa", "Si llevas mucho rato seguido, te recuerda tomar agua y estirarte.", "hiperfoco") {}
            ChipsRow(listOf(0, 45, 60, 90), s.hyperMin, { if (it == 0) "Apagado" else "$it min" }) { n -> vm.settings.update { it.copy(hyperMin = n) } }
            AdhdRow(vm, "Plan según tu energía", "Elige cómo te sientes y Hilo te propone qué hacer.", "energia") {
                SmallBtn("Elegir") { energy = true }
            }

            // ---------- Ambiente ----------
            AdhdGroup("Ambiente")
            AdhdRow(vm, "Sonidos de fondo", "Hechos por el celular, sin internet. Se apagan al salir de Hilo.", "sonidos") {}
            ChipsRow(listOf("", "marron", "rosa", "blanco", "lluvia"), vm.noise, {
                when (it) { "" -> "Apagado"; "marron" -> "Marrón"; "rosa" -> "Rosa"; "blanco" -> "Blanco"; else -> "Lluvia" }
            }) { k -> vm.noise = k; NoisePlayer.play(k, vm.noiseVol) }
            if (vm.noise.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Volumen", color = c.muted, fontSize = 13.sp, modifier = Modifier.width(72.dp))
                Slider(vm.noiseVol, { v -> vm.noiseVol = v; NoisePlayer.volume = v }, valueRange = 0.05f..1f, modifier = Modifier.weight(1f))
            }
            AdhdRow(vm, "Leer con un amigo", "Quedan a la misma hora, cada uno en su lugar. Hilo arma el mensaje.", "amigo") {
                SmallBtn("Invitar") { friend = true }
            }

            // ---------- Motivación ----------
            AdhdGroup("Motivación")
            val st = Adhd.streak(vm)
            AdhdRow(vm, "Racha amable", "${st.days} ${if (st.days == 1) "día" else "días"} seguidos · te quedan ${st.restLeft} días de descanso esta semana.", "racha") {}
            StreakDots(st)
        }
    }
    if (friend) FriendDialog(vm) { friend = false }
    if (energy) EnergyDialog(vm, onSpeak = { onSpeak() }, onDismiss = { energy = false; onDismiss() })
}

@Composable
private fun AdhdGroup(t: String) {
    val c = LocalHilo.current
    Text(t.uppercase(), color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun AdhdRow(vm: HiloViewModel, title: String, sub: String, help: String, control: @Composable () -> Unit) {
    val c = LocalHilo.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                HelpButton(vm, help, size = 16.dp)
            }
            Text(sub, color = c.muted, fontSize = 13.sp, lineHeight = 17.sp)
        }
        control()
    }
}

@Composable
private fun SmallBtn(t: String, onClick: () -> Unit) {
    val c = LocalHilo.current
    Text(
        t, color = c.accentInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(50)).background(c.accent).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun <T> ChipsRow(opts: List<T>, sel: T, label: (T) -> String, onPick: (T) -> Unit) {
    val c = LocalHilo.current
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opts.forEach { o ->
            val on = o == sel
            Text(
                label(o), color = if (on) c.accentInk else c.ink, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.accent else c.chip).clickable { onPick(o) }.padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
fun GoalBar(done: Int, goal: Int) {
    val c = LocalHilo.current
    val f = (done.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(c.line)) {
        Box(Modifier.fillMaxWidth(f).height(10.dp).clip(RoundedCornerShape(50)).background(if (f >= 1f) c.lines[2] else c.accent))
    }
}

@Composable
fun StreakDots(st: Streak) {
    val c = LocalHilo.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        st.last14.forEachIndexed { i, v ->
            Box(
                Modifier.size(if (i == 13) 16.dp else 13.dp).clip(CircleShape)
                    .background(when (v) { 2 -> c.accent; 1 -> c.accent.copy(alpha = 0.35f); else -> c.line })
                    .then(if (i == 13) Modifier.border(2.dp, c.ink, CircleShape) else Modifier),
            )
        }
    }
    Text("Últimos 14 días · claro = día de descanso · el borde es hoy", color = c.muted, fontSize = 11.sp)
}

// =====================================================================
// Tarjetas que aparecen sobre la página
// =====================================================================

/** Tarjeta flotante con un título, el contenido y botones. */
@Composable
fun FloatCard(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalHilo.current
    Column(
        Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(12.dp)
            .clip(RoundedCornerShape(20.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(20.dp))
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = c.ink, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Cerrar", tint = c.muted) }
        }
        content()
    }
}

/** «¿Dónde me quedé?»: las ideas de lo último que leíste. */
@Composable
fun RecapCard(vm: HiloViewModel, ago: String, ideas: List<String>, marks: List<String>, onBack: () -> Unit, onClose: () -> Unit) {
    val c = LocalHilo.current
    FloatCard("¿Dónde me quedé?", onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("La última vez ($ago) leíste sobre esto:", color = c.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            HelpButton(vm, "donde-quede", size = 16.dp)
        }
        ideas.forEach { Text("• $it", color = c.ink, fontSize = 15.sp, lineHeight = 21.sp) }
        if (marks.isNotEmpty()) {
            Text("Tus subrayados de esa parte:", color = c.muted, fontSize = 13.sp)
            marks.take(3).forEach { Text("«${it.take(160)}»", color = c.ink, fontSize = 14.sp, lineHeight = 19.sp) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Volver un poco", color = c.ink) }
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk), modifier = Modifier.weight(1f)) { Text("Seguir leyendo") }
        }
    }
}

/** Pregunta de control: ¿de qué trataba lo que acabas de leer? */
@Composable
fun CheckCard(vm: HiloViewModel, bookId: String, ideas: List<String>, onReread: () -> Unit, onClose: () -> Unit) {
    val c = LocalHilo.current
    var text by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    fun rate(score: Int) {
        vm.motor.add(Entry(vm.motor.newId(), bookId, "control", "Pregunta de control", text.trim(), System.currentTimeMillis(), score))
        if (score == 0) onReread() else onClose()
        vm.toast = when (score) { 2 -> "¡Bien! Sigue así."; 1 -> "Vale. Si quieres, subraya la idea principal."; else -> "Volvamos a leer ese tramo con calma." }
    }
    FloatCard("Pausa de comprensión", onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("En una frase: ¿de qué trataba lo que acabas de leer?", color = c.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
            HelpButton(vm, "control", size = 16.dp)
        }
        OutlinedTextField(text, { text = it }, placeholder = { Text("Escríbelo o solo piénsalo") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        if (!show) Button(onClick = { show = true }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk), modifier = Modifier.fillMaxWidth()) { Text("Ver ideas clave") }
        else {
            Text("Ideas clave de ese tramo:", color = c.muted, fontSize = 13.sp)
            ideas.forEach { Text("• $it", color = c.ink, fontSize = 14.sp, lineHeight = 19.sp) }
            Text("¿Lo entendiste?", color = c.ink, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { rate(0) }, modifier = Modifier.weight(1f)) { Text("No", color = c.ink) }
                OutlinedButton(onClick = { rate(1) }, modifier = Modifier.weight(1f)) { Text("Más o menos", color = c.ink, maxLines = 1) }
                Button(onClick = { rate(2) }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk), modifier = Modifier.weight(1f)) { Text("Sí") }
            }
        }
    }
}

/** Aviso suave tras mucho rato seguido. */
@Composable
fun HyperCard(vm: HiloViewModel, minutes: Int, onSnooze: () -> Unit, onClose: () -> Unit) {
    val c = LocalHilo.current
    FloatCard("Llevas $minutes minutos seguidos", onClose) {
        Text("¡Qué buena concentración! Tu cerebro rinde más si descansa un poco: toma agua, mira lejos y estírate.", color = c.ink, fontSize = 15.sp, lineHeight = 21.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSnooze) { Text("Sigo 30 min más", color = c.ink) }
            Button(onClick = { vm.timer.start(5, breakTime = true); onClose() }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk), modifier = Modifier.weight(1f)) { Text("Pausa de 5 min") }
        }
    }
}

/** Libreta de distracciones: anotar en segundos y seguir leyendo. */
@Composable
fun NotebookDialog(vm: HiloViewModel, bookId: String, onDismiss: () -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    val list = vm.motor.entriesOf("distraccion")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Libreta de distracciones") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Qué se te cruzó por la cabeza? Anótalo y vuelve a leer: aquí te espera.", color = c.muted, fontSize = 13.sp)
                OutlinedTextField(text, { text = it }, placeholder = { Text("Ej.: pagar la luz") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                list.take(30).forEach { e ->
                    val done = e.score == 1
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (done) c.accent else c.line, CircleShape)
                                .background(if (done) c.accent else Color.Transparent)
                                .clickable { vm.motor.delete(e.id); vm.motor.add(e.copy(score = if (done) -1 else 1)) },
                            contentAlignment = Alignment.Center,
                        ) { if (done) Icon(Icons.Rounded.Check, null, tint = c.accentInk, modifier = Modifier.size(14.dp)) }
                        Text(e.text, color = if (done) c.muted else c.ink, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
                        IconButton(onClick = { vm.motor.delete(e.id) }) { Icon(Icons.Rounded.Close, "Borrar", tint = c.muted, modifier = Modifier.size(16.dp)) }
                    }
                }
                if (list.isNotEmpty()) TextButton(onClick = { share(ctx, "Pendientes:\n" + list.filter { it.score != 1 }.joinToString("\n") { "• " + it.text }) }) { Text("Compartir pendientes") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isNotBlank()) {
                    vm.motor.add(Entry(vm.motor.newId(), bookId, "distraccion", "", text.trim(), System.currentTimeMillis()))
                    vm.toast = "Anotado. Sigue leyendo."
                }
                onDismiss()
            }) { Text(if (text.isNotBlank()) "Guardar y seguir" else "Cerrar") }
        },
    )
}

/** Círculo que se vacía mientras corre el temporizador. */
@Composable
fun VisualTimer(timer: FocusTimer, size: Dp, modifier: Modifier = Modifier) {
    val c = LocalHilo.current
    val total = timer.minutes * 60_000f
    val f = if (total > 0f) (timer.remaining / total).coerceIn(0f, 1f) else 0f
    val col = if (timer.isBreak) c.lines[3] else c.accent
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val st = size.toPx() * 0.12f
            drawCircle(col.copy(alpha = 0.18f), radius = this.size.minDimension / 2 - st / 2, style = Stroke(st))
            drawArc(col, -90f, 360f * f, useCenter = true, alpha = 0.85f, topLeft = Offset(st, st), size = Size(this.size.width - st * 2, this.size.height - st * 2))
        }
        if (size >= 64.dp) Text(formatClock(timer.remaining), color = c.ink, fontWeight = FontWeight.Bold, fontSize = (size.value / 5).sp)
    }
}

/** Control del desplazamiento automático. */
@Composable
fun AutoScrollBar(vm: HiloViewModel, modifier: Modifier = Modifier) {
    val c = LocalHilo.current
    Row(
        modifier.clip(RoundedCornerShape(50)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(50)).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { vm.autoScroll = 0 }) { Icon(Icons.Rounded.Pause, "Detener desplazamiento", tint = c.accent) }
        IconButton(onClick = { vm.autoScroll = (vm.autoScroll - 1).coerceAtLeast(1) }) { Icon(Icons.Rounded.Remove, "Más lento", tint = c.ink) }
        Text("Velocidad ${vm.autoScroll}", color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        IconButton(onClick = { vm.autoScroll = (vm.autoScroll + 1).coerceAtMost(10) }) { Icon(Icons.Rounded.Add, "Más rápido", tint = c.ink) }
    }
}

/** Pequeña celebración (confeti) al cumplir la meta. */
@Composable
fun Celebration(vm: HiloViewModel) {
    val text = vm.celebrate ?: return
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val still = reduceMotion(ctx)
    val t = remember(text) { Animatable(0f) }
    LaunchedEffect(text) {
        t.animateTo(1f, tween(if (still) 1 else 2200, easing = LinearEasing))
        kotlinx.coroutines.delay(if (still) 2500 else 400)
        vm.celebrate = null
    }
    val pieces = remember(text) { val r = Random(text.hashCode().toLong()); List(60) { Triple(r.nextFloat(), r.nextFloat() * 0.5f + 0.5f, r.nextInt(6)) } }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!still) Canvas(Modifier.fillMaxSize()) {
            pieces.forEach { (x, sp, ci) ->
                val y = (t.value * sp * 1.3f) * size.height - 40f
                drawRect(c.lines[ci % c.lines.size], Offset(x * size.width, y), Size(14f, 22f), alpha = (1f - t.value).coerceIn(0f, 1f))
            }
        }
        Text(
            text, color = c.accentInk, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp).clip(RoundedCornerShape(20.dp)).background(c.accent).padding(horizontal = 22.dp, vertical = 14.dp),
        )
    }
}

// =====================================================================
// Racha en la biblioteca y en Estudiar
// =====================================================================

@Composable
fun StreakCard(vm: HiloViewModel, modifier: Modifier = Modifier) {
    val c = LocalHilo.current
    var open by remember { mutableStateOf(false) }
    val st = Adhd.streak(vm)
    val goal = vm.settings.value.goalMin
    val today = Adhd.minutes(vm)
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).clickable { open = true }.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(40.dp)) {
                val w = 5.dp.toPx()
                drawArc(c.line, 0f, 360f, false, style = Stroke(w), topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w))
                drawArc(if (today >= goal) c.lines[2] else c.accent, -90f, 360f * (today.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f), false, style = Stroke(w, cap = StrokeCap.Round), topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w))
            }
            Text("🔥", fontSize = 15.sp)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("${st.days} ${if (st.days == 1) "día" else "días"} de racha", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(if (today >= goal) "Meta de hoy cumplida: $today min 🎉" else "Hoy: $today de $goal min", color = c.muted, fontSize = 13.sp)
        }
        HelpButton(vm, "racha", size = 18.dp)
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text("Tu racha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${st.days} ${if (st.days == 1) "día seguido" else "días seguidos"} leyendo o repasando. Cuenta un día si lees 5 minutos o repasas una tarjeta.", color = c.ink, fontSize = 14.sp)
                StreakDots(st)
                Text("Tienes 2 días de descanso por semana que no rompen la racha. Te quedan ${st.restLeft} esta semana.", color = c.muted, fontSize = 13.sp)
                Text("Meta de lectura diaria", color = c.ink, fontWeight = FontWeight.SemiBold)
                GoalBar(today, goal)
                ChipsRow(listOf(5, 10, 15, 20, 30), goal, { "$it min" }) { m -> vm.settings.update { it.copy(goalMin = m) } }
            }
        },
        confirmButton = { TextButton(onClick = { open = false }) { Text("Listo") } },
    )
}

// =====================================================================
// Plan según tu energía y leer con un amigo
// =====================================================================

@Composable
fun EnergyDialog(vm: HiloViewModel, onSpeak: (() -> Unit)? = null, onReview: (() -> Unit)? = null, onSimulacro: (() -> Unit)? = null, onDismiss: () -> Unit) {
    val c = LocalHilo.current
    var level by remember { mutableStateOf(-1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cómo está tu energía?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Poca", "Media", "Mucha").forEachIndexed { i, l ->
                        Text(
                            l, color = if (level == i) c.accentInk else c.ink, fontSize = 14.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (level == i) c.accent else c.chip).clickable { level = i }.padding(vertical = 12.dp),
                        )
                    }
                }
                when (level) {
                    0 -> {
                        Text("Hoy, algo suave. Lo importante es no cortar el hábito.", color = c.ink, fontSize = 14.sp)
                        EnergyOption("Escuchar el libro 10 min", "La voz lee por ti; tú solo sigues.") { vm.timer.start(10); if (onSpeak != null) onSpeak() else vm.startSmall(10, speak = true); onDismiss() }
                        if (onReview != null) EnergyOption("Repasar 10 tarjetas", "Pocas y rápidas.") { onReview(); onDismiss() }
                        EnergyOption("Solo 2 minutos de lectura", "Si después quieres, sigues.") { vm.startSmall(2); onDismiss() }
                    }
                    1 -> {
                        Text("Un bloque normal con una pausa al final.", color = c.ink, fontSize = 14.sp)
                        EnergyOption("Leer 15 minutos", "Con temporizador y pausa de 5.") { vm.startSmall(15); onDismiss() }
                        if (onReview != null) EnergyOption("Repasar tus tarjetas de hoy", "Las que tocan según tu calendario.") { onReview(); onDismiss() }
                    }
                    2 -> {
                        Text("Aprovecha para lo que más cuesta.", color = c.ink, fontSize = 14.sp)
                        EnergyOption("Material nuevo 25 minutos", "Lee con la ruta: prueba previa y subrayado por tipo.") { vm.startSmall(25); onDismiss() }
                        if (onSimulacro != null) EnergyOption("Simulacro de examen", "Preguntas con tiempo, mezclando temas.") { onSimulacro(); onDismiss() }
                    }
                    else -> Text("Elige lo que más se parece a cómo te sientes ahora. No hay respuesta mala.", color = c.muted, fontSize = 13.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

@Composable
private fun EnergyOption(title: String, sub: String, onClick: () -> Unit) {
    val c = LocalHilo.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.band).clickable(onClick = onClick).padding(12.dp)) {
        Text(title, color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(sub, color = c.muted, fontSize = 13.sp)
    }
}

@Composable
fun FriendDialog(vm: HiloViewModel, onDismiss: () -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    var min by remember { mutableStateOf(25) }
    var wait by remember { mutableStateOf(5) }
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val start = LocalTime.now().plusMinutes(wait.toLong())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Leer con un amigo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Leer «acompañado» ayuda a empezar y a no dejarlo. Tu amigo no necesita Hilo: solo leer a la misma hora.", color = c.muted, fontSize = 13.sp)
                Text("¿Cuánto tiempo?", color = c.ink, fontWeight = FontWeight.SemiBold)
                ChipsRow(listOf(15, 25, 45), min, { "$it min" }) { min = it }
                Text("¿Cuándo empiezan?", color = c.ink, fontWeight = FontWeight.SemiBold)
                ChipsRow(listOf(0, 5, 10, 15), wait, { if (it == 0) "Ahora" else "En $it min" }) { wait = it }
                Text("Empiezan a las ${start.format(fmt)} y terminan a las ${start.plusMinutes(min.toLong()).format(fmt)}.", color = c.ink, fontSize = 14.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                val msg = "📚 ¿Leemos juntos? Empiezo a leer a las ${start.format(fmt)} durante $min minutos. Tú también lee lo tuyo y al final nos contamos cómo nos fue."
                share(ctx, msg)
                vm.friendDone = "✅ ¡Terminé mis $min minutos de lectura! ¿Cómo te fue a ti?"
                if (wait == 0) vm.timer.start(min)
                else {
                    vm.toast = "El temporizador empieza solo a las ${start.format(fmt)}."
                    vm.startLater(wait, min)
                }
                onDismiss()
            }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("Enviar invitación") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private val DAY_NAMES = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

/** «hace 2 días», «ayer», «hace 5 horas». */
fun agoText(ms: Long): String {
    val h = ((System.currentTimeMillis() - ms) / 3_600_000L).toInt()
    val d = (LocalDate.now().toEpochDay() - java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()).toInt()
    return when {
        d <= 0 -> if (h <= 1) "hace un rato" else "hace $h horas"
        d == 1 -> "ayer"
        d < 7 -> "el ${DAY_NAMES[java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).dayOfWeek.value - 1]}"
        else -> "hace $d días"
    }
}
