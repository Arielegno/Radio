@file:OptIn(ExperimentalLayoutApi::class)

package com.hilo.lector.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hilo.lector.HiloViewModel
import com.hilo.lector.Stats
import com.hilo.lector.data.Book
import com.hilo.lector.data.Entry
import com.hilo.lector.data.MARK_KINDS
import com.hilo.lector.data.StudyCard
import com.hilo.lector.data.TextMark
import com.hilo.lector.data.today
import com.hilo.lector.text.Analysis
import com.hilo.lector.text.ChapterGuide
import com.hilo.lector.text.KeyWords
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

// =====================================================================
// Cola de repaso: frágiles primero y el resto intercalado
// =====================================================================

/** Repaso intercalado: primero las tarjetas frágiles y luego se alternan documentos y tipos de pregunta. */
fun mixQueue(cards: List<StudyCard>): List<StudyCard> {
    val fragile = cards.filter { it.fragile }.sortedBy { it.recall() }
    val rest = cards.filterNot { it.fragile }
    val groups = rest.groupBy { it.bookId + "|" + it.kind }.values.map { it.sortedWith(compareBy({ it.due }, { it.recall() })).toMutableList() }
    val out = ArrayList<StudyCard>(cards.size)
    out.addAll(fragile)
    var any = true
    while (any) {
        any = false
        for (g in groups) if (g.isNotEmpty()) { out.add(g.removeAt(0)); any = true }
    }
    return out
}

/** Nota estimada (0 a 10) si el examen fuera hoy: el recuerdo promedio de las tarjetas ya estudiadas. */
fun predictedGrade(cards: List<StudyCard>): Float? {
    val studied = cards.filter { it.reps > 0 || it.stab > 0f }
    if (studied.size < 3) return null
    return (studied.map { it.recall() }.average() * 10).toFloat()
}

/** Disciplina del documento, para elegir plantillas: derecho, sociologia o general. */
fun disciplineOf(a: Analysis?): String = when {
    a == null -> "general"
    a.law.isLaw -> "derecho"
    a.authors.size >= 2 -> "sociologia"
    else -> "general"
}

// =====================================================================
// Pantalla completa reutilizable
// =====================================================================

@Composable
fun OverlayFrame(title: String, onClose: () -> Unit, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalHilo.current
    BackHandler(onBack = onClose)
    Box(
        Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing).pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Cerrar", tint = c.ink) }
                Text(title, color = c.ink, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(
                (if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(horizontal = 16.dp).padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val c = LocalHilo.current
    Button(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
    ) { Text(text, fontSize = 16.sp) }
}

private fun conceptsOf(a: Analysis, n: Int = 12): List<Pair<String, String>> =
    (a.phrases.take(n / 3) + a.terms.take(n)).distinctBy { it.key }.take(n).map { it.key to it.label }

// =====================================================================
// Ruta de estudio en 6 fases
// =====================================================================

private val PHASES = listOf(
    "Activar" to "Antes de leer: prueba previa y qué ya sabes.",
    "Leer activo" to "Subraya con colores y marca tus dudas.",
    "Consolidar" to "Resume con tus palabras, explícalo y arma tu mapa.",
    "Recuperar" to "Recuerda sin mirar: tarjetas, recuerdo libre y preguntas por niveles.",
    "Aplicar" to "Usa lo aprendido en ejemplos y casos.",
    "Repasar y cerrar" to "Repaso espaciado, simulacro y cierre del día.",
)

/** Las 6 fases de la ruta, con progreso, para un documento. */
@Composable
fun RouteTab(
    vm: HiloViewModel, book: Book, a: Analysis,
    open: (String) -> Unit, goTab: (String) -> Unit, review: () -> Unit,
) {
    val c = LocalHilo.current
    val m = vm.motor
    val marks = vm.study.marksOf(book.id)
    val doubts = marks.filter { it.kind == "duda" } + emptyList()
    val pdfDoubts = m.entriesOf("duda", book.id)
    fun done(i: Int) = m.flag("fase:${book.id}:$i")
    val count = (0 until 6).count { done(it) }
    val disc = disciplineOf(a)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { count / 6f }, color = c.accent, trackColor = c.chip, strokeWidth = 6.dp, modifier = Modifier.size(58.dp))
                    Text("$count/6", color = c.ink, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text("Ruta de estudio", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = c.ink)
                    Text(
                        "Seis pasos que siguen cómo aprende la memoria. Puedes saltar a cualquiera.",
                        color = c.muted, fontSize = 13.sp,
                    )
                }
            }
            val next = (0 until 6).firstOrNull { !done(it) }
            if (next != null) Text("Siguiente: ${PHASES[next].first} — ${PHASES[next].second}", color = c.accent, fontSize = 14.sp)
            else Text("¡Completaste la ruta! Sigue con el repaso espaciado cada día.", color = c.accent, fontSize = 14.sp)
        }

        @Composable
        fun Phase(i: Int, body: @Composable ColumnScope.() -> Unit) {
            val on = done(i)
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(if (on) c.accent else c.band),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", color = if (on) c.accentInk else c.ink, fontWeight = FontWeight.Bold) }
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(PHASES[i].first, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(PHASES[i].second, color = c.muted, fontSize = 13.sp)
                    }
                    IconButton(onClick = { m.setFlag("fase:${book.id}:$i", !on) }) {
                        Icon(if (on) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, if (on) "Fase hecha" else "Marcar fase como hecha", tint = if (on) c.accent else c.muted)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), content = body)
            }
        }

        Phase(0) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { open("pretest") }) { Text("Prueba previa", color = c.ink) }
                OutlinedButton(onClick = { open("guia") }) { Text("Preguntas guía", color = c.ink) }
            }
            Text("¿Qué ya sabes? Toca el color de cada concepto:", color = c.ink, fontSize = 14.sp)
            Semaforo(vm, book, a)
        }
        Phase(1) {
            Text("Mientras lees, mantén el dedo sobre una oración y elige el color:", color = c.ink, fontSize = 14.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MARK_KINDS.forEach { (k, label, col) ->
                    val n = marks.count { it.kind == k }
                    Text(
                        "$label${if (n > 0) " · $n" else ""}", fontSize = 13.sp, color = Color(0xFF1B1B1B),
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(col)).padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.open(book) }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("Leer") }
                OutlinedButton(onClick = { open("cornell") }) { Text("Notas Cornell", color = c.ink) }
            }
            val nd = doubts.size + pdfDoubts.size
            if (nd > 0) TextButton(onClick = { open("dudas") }) { Text("Tienes $nd ${if (nd == 1) "duda" else "dudas"} por resolver") }
        }
        Phase(2) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { open("resumen") }) { Text("Resumen propio", color = c.ink) }
                OutlinedButton(onClick = {
                    val k = conceptsOf(a, 3).firstOrNull()
                    if (k != null) vm.feynman = FeynmanReq(book.id, k.second, a.sentencesWith(k.first, 3).joinToString(" ") { it.text }, emptyList())
                }) { Text("Explícalo (Feynman)", color = c.ink) }
                OutlinedButton(onClick = { open("mapa-mio") }) { Text("Mapa de mis subrayados", color = c.ink) }
                OutlinedButton(onClick = { goTab("glosario") }) { Text("Glosario", color = c.ink) }
            }
            Text(
                when (disc) {
                    "derecho" -> "Para Derecho, el resumen sigue: institución → requisitos → efectos → excepciones → artículos."
                    "sociologia" -> "Para Sociología, el resumen sigue: autor → concepto → tesis → ejemplo actual → crítica."
                    else -> "El resumen sigue: idea principal → ideas que la sostienen → ejemplo propio."
                },
                color = c.muted, fontSize = 12.sp,
            )
        }
        Phase(3) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val n = cardsFromMarks(vm, book)
                    vm.toast = if (n == 0) "No hay subrayados nuevos para convertir." else "Creé $n tarjetas de tus subrayados. Revísalas en Tarjetas: editarlas te ayuda a recordar."
                }) { Text("Tarjetas de mis subrayados", color = c.ink) }
                OutlinedButton(onClick = { open("recuerdo") }) { Text("Recuerdo libre (3 min)", color = c.ink) }
                OutlinedButton(onClick = { open("bloom") }) { Text("Preguntas por niveles", color = c.ink) }
            }
        }
        Phase(4) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { open("aplicar") }) { Text("Ejemplo resuelto → solo", color = c.ink) }
                when (disc) {
                    "derecho" -> OutlinedButton(onClick = { open("derecho") }) { Text("Casos y método jurídico", color = c.ink) }
                    "sociologia" -> OutlinedButton(onClick = { open("sociologia") }) { Text("Imaginación sociológica", color = c.ink) }
                    else -> {}
                }
            }
        }
        Phase(5) {
            val due = vm.study.due(book.id).size
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = review, enabled = due > 0, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) {
                    Text(if (due > 0) "Repasar $due" else "Sin tarjetas para hoy")
                }
                OutlinedButton(onClick = { open("simulacro") }) { Text("Simulacro de examen", color = c.ink) }
                OutlinedButton(onClick = { open("cierre") }) { Text("Cierre del día", color = c.ink) }
            }
            predictedGrade(vm.study.cardsOf(book.id))?.let { g ->
                Text("Si el examen fuera hoy: nota estimada ${"%.1f".format(g)} / 10", color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            val fragile = vm.study.cardsOf(book.id).count { it.fragile }
            if (fragile > 0) Text("$fragile ${if (fragile == 1) "tarjeta frágil" else "tarjetas frágiles"}: aparecen primero en el repaso.", color = c.lines[2], fontSize = 13.sp)
        }
    }
}

/** Semáforo de conceptos: lo sé (verde), lo medio sé (amarillo), no lo sé (rojo). */
@Composable
private fun Semaforo(vm: HiloViewModel, book: Book, a: Analysis) {
    val c = LocalHilo.current
    val cols = listOf(Color(0xFF43A047), Color(0xFFFFB300), Color(0xFFE53935))
    conceptsOf(a, 10).forEach { (k, label) ->
        val key = "sem:${book.id}:$k"
        val v = vm.motor[key].toIntOrNull() ?: 0
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
            cols.forEachIndexed { i, col ->
                val on = v == i + 1
                Box(
                    Modifier.padding(horizontal = 3.dp).size(28.dp).clip(CircleShape).background(if (on) col else col.copy(alpha = 0.18f))
                        .clickable { vm.motor.put(key, if (on) "" else "${i + 1}") },
                    contentAlignment = Alignment.Center,
                ) { if (on) Text("✓", color = Color.White, fontSize = 13.sp) }
            }
        }
    }
    Text("Verde: lo sé · Amarillo: lo medio sé · Rojo: no lo sé", color = c.muted, fontSize = 11.sp)
}

/** Tarjetas para completar a partir de los subrayados (se pueden editar después). */
fun cardsFromMarks(vm: HiloViewModel, book: Book): Int {
    val have = vm.study.cardsOf(book.id).map { it.source }.toHashSet()
    val list = vm.study.marksOf(book.id).filter { it.kind != "duda" && "subrayado ${it.id}" !in have }.mapNotNull { m ->
        val key = KeyWords.of(m.text, emptyList(), 1).firstOrNull() ?: return@mapNotNull null
        val i = m.text.indexOf(key, ignoreCase = true)
        if (i < 0) return@mapNotNull null
        val ans = m.text.substring(i, i + key.length)
        val front = if (m.kind == "definicion") "¿Qué es «$ans»?" else m.text.replaceRange(i, i + key.length, "[ … ]")
        StudyCard(
            vm.study.newId(), book.id, if (m.kind == "definicion") "qa" else "cloze", front,
            if (m.kind == "definicion") m.text else ans, context = m.text,
            source = "subrayado ${m.id}", sourcePos = m.para, sourceFlow = true, due = today(), created = System.currentTimeMillis(),
        )
    }
    vm.study.addAll(list)
    return list.size
}

// =====================================================================
// Ventanas de práctica
// =====================================================================

/** Abre la práctica [mode] a pantalla completa. */
@Composable
fun PracticeOverlay(vm: HiloViewModel, book: Book?, a: Analysis?, mode: String, onReview: (List<StudyCard>) -> Unit, onClose: () -> Unit) {
    when (mode) {
        "pretest" -> if (book != null && a != null) Pretest(vm, book, a, onClose)
        "guia" -> if (book != null && a != null) GuideQuestions(a, onClose)
        "cornell" -> if (book != null) Cornell(vm, book, onClose)
        "dudas" -> if (book != null) Doubts(vm, book, onClose)
        "resumen" -> if (book != null && a != null) OwnSummary(vm, book, a, onClose)
        "mapa-mio" -> if (book != null) MarksMap(vm, book, onClose)
        "recuerdo" -> if (book != null && a != null) FreeRecall(vm, book, a, onClose)
        "bloom" -> if (book != null && a != null) Bloom(vm, book, a, onClose)
        "aplicar" -> if (book != null && a != null) Apply(vm, book, a, onClose)
        "simulacro" -> Simulacro(vm, book, onReview, onClose)
        "cierre" -> OverlayFrame("Cierre del día", onClose) { SessionClose(vm, book?.id) }
        "stats" -> StatsView(vm, onClose)
        else -> extraPractice(vm, book, a, mode, onClose)
    }
}

@Composable
private fun Pretest(vm: HiloViewModel, book: Book, a: Analysis, onClose: () -> Unit) {
    val c = LocalHilo.current
    val qs = remember(a) {
        val all = a.guides.flatMap { it.questions }
        all.filter { it.kind != "open" }.shuffled(Random(book.id.hashCode())).take(5).ifEmpty { all.take(5) }
    }
    var i by remember { mutableIntStateOf(0) }
    var show by remember { mutableStateOf(false) }
    var ok by remember { mutableIntStateOf(0) }
    var ans by remember { mutableStateOf("") }
    OverlayFrame("Prueba previa", onClose) {
        Text("Antes de leer, intenta responder. Equivocarte aquí también ayuda: tu cerebro queda atento a encontrar la respuesta.", color = c.muted, fontSize = 14.sp)
        when {
            qs.isEmpty() -> Text("No encontré preguntas en este documento todavía.", color = c.ink)
            i < qs.size -> {
                val q = qs[i]
                LinearProgressIndicator(progress = { i / qs.size.toFloat() }, color = c.accent, trackColor = c.chip, modifier = Modifier.fillMaxWidth())
                Text("Pregunta ${i + 1} de ${qs.size}", color = c.muted, fontSize = 13.sp)
                Text(q.q, color = c.ink, fontSize = 20.sp, lineHeight = 27.sp)
                OutlinedTextField(ans, { ans = it }, placeholder = { Text("Lo que creas (o «ni idea»)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                if (!show) BigButton("Ver respuesta", { show = true })
                else {
                    Text(q.a, color = c.accent, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { ok++; i++; show = false; ans = "" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = c.lines[1])) { Text("Acerté") }
                        OutlinedButton(onClick = { i++; show = false; ans = "" }, modifier = Modifier.weight(1f)) { Text("No la sabía", color = c.ink) }
                    }
                }
            }
            else -> {
                LaunchedEffect(Unit) {
                    vm.motor.add(Entry(vm.motor.newId(), book.id, "pretest", "Prueba previa", "", System.currentTimeMillis(), ok * 100 / qs.size))
                    vm.motor.setFlag("fase:${book.id}:0", true)
                }
                Text("Acertaste $ok de ${qs.size}", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = c.ink)
                Text(
                    if (ok * 2 < qs.size) "Es normal antes de leer. Ahora lee buscando estas respuestas: así se te quedan mejor."
                    else "¡Ya sabías bastante! Lee para completar lo que falta.",
                    color = c.ink, fontSize = 15.sp,
                )
                BigButton("Listo", onClose)
            }
        }
    }
}

@Composable
private fun GuideQuestions(a: Analysis, onClose: () -> Unit) {
    val c = LocalHilo.current
    OverlayFrame("Preguntas guía", onClose) {
        Text("Tenlas presentes mientras lees cada parte. Leer con una pregunta en la cabeza ayuda a no perder el hilo.", color = c.muted, fontSize = 14.sp)
        a.guides.take(30).forEach { g ->
            Panel {
                Text(g.chapter.title, color = c.accent, fontWeight = FontWeight.Bold)
                Text("• ¿Qué problema o pregunta responde esta parte?", color = c.ink, fontSize = 14.sp)
                g.concepts.take(2).forEach { k -> Text("• ¿Qué significa «$k» y por qué importa aquí?", color = c.ink, fontSize = 14.sp) }
                Text("• ¿Con qué otra parte del texto se conecta?", color = c.ink, fontSize = 14.sp)
            }
        }
    }
}

/** Notas Cornell: preguntas a la izquierda, tus subrayados a la derecha y tu resumen abajo. */
@Composable
private fun Cornell(vm: HiloViewModel, book: Book, onClose: () -> Unit) {
    val c = LocalHilo.current
    val marks = vm.study.marksOf(book.id).sortedBy { it.para }
    val pdf = rememberHighlights(vm, book).filter { it.contains("(pág.") }
    fun question(m: TextMark): String {
        val k = KeyWords.of(m.text, emptyList(), 1).firstOrNull() ?: "esto"
        return when (m.kind) {
            "definicion" -> "¿Qué es «$k»?"
            "tesis" -> "¿Qué sostiene el texto sobre «$k»?"
            "ejemplo" -> "¿Qué ejemplo muestra «$k»?"
            "critica" -> "¿Qué crítica se hace a «$k»?"
            "dato" -> "¿Qué dato hay sobre «$k»?"
            "duda" -> "Duda: ¿qué quiere decir esto?"
            else -> "¿Qué idea clave hay sobre «$k»?"
        }
    }
    var summary by remember { mutableStateOf(vm.motor["cornell:${book.id}"]) }
    OverlayFrame("Notas Cornell", onClose) {
        Text("Tapa la columna derecha y responde las preguntas de la izquierda: así repasas.", color = c.muted, fontSize = 13.sp)
        if (marks.isEmpty() && pdf.isEmpty()) Text("Todavía no hay subrayados. Mientras lees, mantén el dedo sobre una oración y elige un color.", color = c.ink)
        marks.forEach { m ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface)) {
                Text(question(m), color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.38f).padding(10.dp))
                Box(Modifier.width(1.dp).height(60.dp).background(c.line))
                Text(m.text, color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(0.62f).padding(10.dp))
            }
        }
        pdf.forEach { t ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface)) {
                Text("¿Qué idea clave hay aquí?", color = c.accent, fontSize = 13.sp, modifier = Modifier.weight(0.38f).padding(10.dp))
                Text(t, color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(0.62f).padding(10.dp))
            }
        }
        Label("Resumen (con tus palabras)")
        OutlinedTextField(summary, { summary = it; vm.motor.put("cornell:${book.id}", it) }, minLines = 4, modifier = Modifier.fillMaxWidth())
        if (marks.isNotEmpty()) OutlinedButton(onClick = {
            val have = vm.study.cardsOf(book.id).map { it.front + "|" + it.back }.toHashSet()
            val add = marks.filter { it.kind != "duda" }.map { m ->
                StudyCard(vm.study.newId(), book.id, "qa", question(m), m.text, source = "Cornell", sourcePos = m.para, sourceFlow = true, due = today(), created = System.currentTimeMillis())
            }.filter { have.add(it.front + "|" + it.back) }
            vm.study.addAll(add)
            vm.toast = if (add.isEmpty()) "Ya estaban como tarjetas." else "Creé ${add.size} tarjetas con estas preguntas."
        }) { Text("Pasar preguntas a tarjetas", color = c.ink) }
    }
}

@Composable
private fun Doubts(vm: HiloViewModel, book: Book, onClose: () -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val marks = vm.study.marksOf(book.id).filter { it.kind == "duda" }
    val pdf = vm.motor.entriesOf("duda", book.id)
    OverlayFrame("Mis dudas", onClose) {
        Text("Lo que marcaste como «No entiendo». Búscalo, pregúntalo en clase o explícalo cuando lo entiendas.", color = c.muted, fontSize = 13.sp)
        if (marks.isEmpty() && pdf.isEmpty()) Text("No tienes dudas pendientes. ¡Bien!", color = c.ink)
        marks.forEach { m ->
            Panel {
                Text(m.text, color = c.ink, fontSize = 15.sp)
                Row {
                    TextButton(onClick = { vm.openAt(book.id, true, m.para) }) { Text("Ir") }
                    TextButton(onClick = { KeyWords.of(m.text, emptyList(), 1).firstOrNull()?.let { lookup(ctx, it, "wiki", "es") } }) { Text("Buscar") }
                    TextButton(onClick = { vm.study.deleteMark(m.id); vm.toast = "Duda resuelta." }) { Text("Ya la entendí") }
                }
            }
        }
        pdf.forEach { e ->
            Panel {
                Text(e.text, color = c.ink, fontSize = 15.sp)
                Text(e.title, color = c.muted, fontSize = 12.sp)
                TextButton(onClick = { vm.motor.delete(e.id); vm.toast = "Duda resuelta." }) { Text("Ya la entendí") }
            }
        }
    }
}

private fun summaryTemplate(disc: String) = when (disc) {
    "derecho" -> "Institución o figura:\n\nRequisitos o elementos:\n\nEfectos:\n\nExcepciones:\n\nArtículos que la regulan:\n"
    "sociologia" -> "Autor y época:\n\nConcepto central:\n\nTesis (qué sostiene):\n\nEjemplo actual:\n\nCrítica o límite:\n"
    else -> "Idea principal:\n\nIdeas que la sostienen:\n\nEjemplo propio:\n"
}

@Composable
private fun OwnSummary(vm: HiloViewModel, book: Book, a: Analysis, onClose: () -> Unit) {
    val c = LocalHilo.current
    var g by remember { mutableStateOf<ChapterGuide?>(a.guides.firstOrNull()) }
    OverlayFrame("Resumen propio", onClose) {
        Text("Escríbelo sin mirar el texto y después compara. Copiar no sirve: lo que fija es reconstruirlo.", color = c.muted, fontSize = 13.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            a.guides.take(40).forEach { x -> Pill(x.chapter.title.take(28), x == g) { g = x } }
        }
        val cur = g ?: return@OverlayFrame
        val key = "res:${book.id}:${cur.chapter.startPara}"
        var text by remember(key) { mutableStateOf(vm.motor[key].ifEmpty { summaryTemplate(disciplineOf(a)) }) }
        OutlinedTextField(text, { text = it; vm.motor.put(key, it) }, minLines = 10, modifier = Modifier.fillMaxWidth())
        val (hit, miss) = KeyWords.coverage(text, cur.concepts.take(8))
        Text("Mencionas ${hit.size} de ${minOf(8, cur.concepts.size)} conceptos clave de esta parte.", color = c.ink, fontSize = 14.sp)
        if (miss.isNotEmpty()) Text("Podrías agregar: " + miss.joinToString(", "), color = c.muted, fontSize = 13.sp)
    }
}

@Composable
private fun MarksMap(vm: HiloViewModel, book: Book, onClose: () -> Unit) {
    val c = LocalHilo.current
    val texts = vm.study.marksOf(book.id).map { it.text } + rememberHighlights(vm, book).filter { it.contains("(pág.") }
    val an = remember(texts) { if (texts.size < 2) null else runCatching { com.hilo.lector.text.Analyzer.analyze(texts, emptySet()) }.getOrNull() }
    OverlayFrame("Mapa de mis subrayados", onClose, scroll = true) {
        if (an == null || an.mapTerms.size < 3) Text("Necesito al menos unos 5 subrayados para armar el mapa. Subraya mientras lees.", color = c.ink)
        else ConceptMapTab(vm, book, an, intro = "Los conceptos de lo que tú subrayaste, unidos cuando aparecen juntos. Es tu mapa, no el del libro.")
    }
}

@Composable
private fun FreeRecall(vm: HiloViewModel, book: Book, a: Analysis, onClose: () -> Unit) {
    val c = LocalHilo.current
    var g by remember { mutableStateOf<ChapterGuide?>(null) }
    var left by remember { mutableIntStateOf(180) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val dictate = rememberDictation(vm, vm.study.langs[book.id].orEmpty()) { t -> text = if (text.isBlank()) t else "$text $t" }
    LaunchedEffect(running) {
        while (running && left > 0) { delay(1000); left-- }
        if (running && left <= 0) { running = false; finished = true }
    }
    OverlayFrame("Recuerdo libre", onClose) {
        val cur = g
        if (cur == null) {
            Text("Elige una parte. Tendrás 3 minutos para escribir (o decir) todo lo que recuerdes, sin mirar. Después Hilo compara con el texto.", color = c.ink, fontSize = 15.sp)
            a.guides.take(40).forEach { x ->
                Text(x.chapter.title, color = c.ink, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).clickable { g = x; running = true }.padding(14.dp))
            }
            return@OverlayFrame
        }
        Text(cur.chapter.title, color = c.accent, fontWeight = FontWeight.Bold)
        Text("%d:%02d".format(left / 60, left % 60), fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 34.sp, color = if (left < 30) c.lines[0] else c.ink)
        OutlinedTextField(text, { text = it }, enabled = !finished, minLines = 8, placeholder = { Text("Todo lo que recuerdes…") }, modifier = Modifier.fillMaxWidth())
        if (!finished) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = dictate) { Icon(Icons.Rounded.Mic, null, tint = c.accent); Text("  Hablar", color = c.ink) }
                Button(onClick = { running = false; finished = true }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("Terminé") }
            }
        } else {
            val keys = remember(cur) { KeyWords.of(cur.ideas.joinToString(" ") { it.text }, cur.concepts.take(6), 12) }
            val (hit, miss) = KeyWords.coverage(text, keys)
            val pct = if (keys.isEmpty()) 0 else hit.size * 100 / keys.size
            LaunchedEffect(Unit) {
                vm.motor.add(Entry(vm.motor.newId(), book.id, "recuerdo", cur.chapter.title, text, System.currentTimeMillis(), pct))
            }
            Text("Recordaste $pct % de las ideas clave", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (pct >= 60) c.lines[1] else c.lines[2])
            if (miss.isNotEmpty()) Text("Te faltó: " + miss.joinToString(", "), color = c.ink, fontSize = 14.sp)
            Label("Ideas principales del texto")
            cur.ideas.take(4).forEach { Text("• " + it.text, color = c.ink, fontSize = 14.sp) }
            TextButton(onClick = { g = null; left = 180; finished = false; text = "" }) { Text("Otra parte") }
        }
    }
}

/** Preguntas por niveles de Bloom: de recordar a crear. */
@Composable
private fun Bloom(vm: HiloViewModel, book: Book, a: Analysis, onClose: () -> Unit) {
    val c = LocalHilo.current
    val cs = remember(a) { conceptsOf(a, 8).shuffled(Random(System.currentTimeMillis())) }
    val x = cs.getOrNull(0)?.second ?: "el tema"
    val y = cs.getOrNull(1)?.second ?: "otro concepto"
    val levels = remember(a) {
        listOf(
            "Recordar" to "¿Qué es «$x»? Defínelo.",
            "Entender" to "Explica con tus palabras cómo se relacionan «$x» y «$y».",
            "Aplicar" to "Da un ejemplo actual (de tu vida o de las noticias) de «$x».",
            "Analizar" to "¿En qué se parecen y en qué se diferencian «$x» y «$y»?",
            "Evaluar" to "¿Estás de acuerdo con lo que dice el texto sobre «$x»? Da una razón a favor y una en contra.",
            "Crear" to "Formula una pregunta de investigación o un caso nuevo que use «$x».",
        )
    }
    var i by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val scores = remember { mutableStateListOf<Int>() }
    val dictate = rememberDictation(vm, "") { t -> text = if (text.isBlank()) t else "$text $t" }
    OverlayFrame("Preguntas por niveles", onClose) {
        if (i >= levels.size) {
            Text("¡Recorriste los 6 niveles!", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = c.ink)
            levels.forEachIndexed { k, (lv, _) ->
                val s = scores.getOrNull(k) ?: 0
                Text("$lv: " + listOf("—", "bien", "a medias", "me costó")[s], color = c.ink, fontSize = 15.sp)
            }
            Text("Los niveles altos (analizar, evaluar, crear) son los que más piden en los exámenes orales y en los ensayos.", color = c.muted, fontSize = 13.sp)
            BigButton("Listo", onClose)
            return@OverlayFrame
        }
        val (lv, q) = levels[i]
        Text("Nivel ${i + 1} de 6 · $lv", color = c.accent, fontWeight = FontWeight.Bold)
        Text(q, color = c.ink, fontSize = 20.sp, lineHeight = 27.sp)
        OutlinedTextField(text, { text = it }, minLines = 4, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = dictate) { Icon(Icons.Rounded.Mic, null, tint = c.accent); Text("  Hablar", color = c.ink) }
            if (!show) Button(onClick = { show = true }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("Ver en el texto") }
        }
        if (show) {
            cs.getOrNull(0)?.let { (k, _) -> a.sentencesWith(k, 2).forEach { Text("«${it.text}»", color = c.ink, fontSize = 14.sp) } }
            Text("¿Cómo te fue?", color = c.muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Bien" to 1, "A medias" to 2, "Me costó" to 3).forEach { (l, v) ->
                    OutlinedButton(onClick = {
                        scores.add(v)
                        vm.motor.add(Entry(vm.motor.newId(), book.id, "bloom", lv, text, System.currentTimeMillis(), (4 - v) * 33))
                        i++; text = ""; show = false
                    }, modifier = Modifier.weight(1f)) { Text(l, color = c.ink) }
                }
            }
        }
    }
}

/** Aplicación con andamiaje que se retira: ejemplo resuelto → con pistas → solo. */
@Composable
private fun Apply(vm: HiloViewModel, book: Book, a: Analysis, onClose: () -> Unit) {
    val c = LocalHilo.current
    val red = conceptsOf(a, 10).firstOrNull { vm.motor["sem:${book.id}:${it.first}"] == "3" } ?: conceptsOf(a, 3).firstOrNull()
    var concept by remember { mutableStateOf(red) }
    var step by remember { mutableIntStateOf(0) }
    var t1 by remember { mutableStateOf("") }
    var t2 by remember { mutableStateOf("") }
    OverlayFrame("Aplicar lo aprendido", onClose) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            conceptsOf(a, 8).forEach { k -> Pill(k.second, k == concept) { concept = k; step = 0; t1 = ""; t2 = "" } }
        }
        val (key, label) = concept ?: return@OverlayFrame
        val example = vm.study.marksOf(book.id).firstOrNull { it.kind == "ejemplo" && it.text.contains(label, true) }?.text
            ?: a.sentencesWith(key, 20).firstOrNull { s -> listOf("por ejemplo", "como ", "caso", "ejemplo").any { s.text.lowercase().contains(it) } }?.text
            ?: a.sentencesWith(key, 1).firstOrNull()?.text.orEmpty()
        Panel {
            Text("1 · Ejemplo resuelto", color = c.accent, fontWeight = FontWeight.Bold)
            Text("El texto aplica «$label» así:", color = c.ink, fontSize = 14.sp)
            Text("«$example»", color = c.ink, fontSize = 15.sp)
            Text("Fíjate: quién participa, qué regla o fuerza actúa y qué consecuencia tiene.", color = c.muted, fontSize = 13.sp)
            if (step == 0) TextButton(onClick = { step = 1 }) { Text("Entendido, sigo") }
        }
        if (step >= 1) Panel {
            Text("2 · Con pistas", color = c.accent, fontWeight = FontWeight.Bold)
            Text("Piensa una situación de tu vida o de las noticias donde aparezca «$label».", color = c.ink, fontSize = 14.sp)
            Text("Pistas: ¿quiénes participan? ¿qué regla, norma o fuerza social actúa? ¿qué pasa como consecuencia?", color = c.muted, fontSize = 13.sp)
            OutlinedTextField(t1, { t1 = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
            if (step == 1) TextButton(onClick = { step = 2 }, enabled = t1.isNotBlank()) { Text("Siguiente") }
        }
        if (step >= 2) Panel {
            Text("3 · Solo", color = c.accent, fontWeight = FontWeight.Bold)
            Text("Otro ejemplo distinto de «$label», sin pistas.", color = c.ink, fontSize = 14.sp)
            OutlinedTextField(t2, { t2 = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
            BigButton("Guardar", {
                vm.motor.add(Entry(vm.motor.newId(), book.id, "aplicacion", label, "Con pistas: $t1\n\nSolo: $t2", System.currentTimeMillis()))
                vm.motor.setFlag("fase:${book.id}:4", true)
                vm.toast = "Guardado. Aplicar es lo que más fija un concepto."
                onClose()
            }, enabled = t2.isNotBlank())
        }
    }
}

/** Cierre de sesión: tres preguntas para consolidar lo del día. */
@Composable
fun SessionClose(vm: HiloViewModel, bookId: String?) {
    val c = LocalHilo.current
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var d by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Cierre del día (1 minuto)", color = c.ink, fontWeight = FontWeight.Bold)
        if (saved) { Text("Guardado. Mañana lo verás en tus estadísticas.", color = c.accent); return@Column }
        OutlinedTextField(a, { a = it }, label = { Text("¿Qué aprendí hoy?") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(b, { b = it }, label = { Text("¿Qué me costó?") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(d, { d = it }, label = { Text("¿Qué repaso mañana?") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            vm.motor.add(Entry(vm.motor.newId(), bookId.orEmpty(), "cierre", "Cierre", "Aprendí: $a\nMe costó: $b\nMañana: $d", System.currentTimeMillis()))
            if (bookId != null) vm.motor.setFlag("fase:$bookId:5", true)
            saved = true
        }, enabled = a.isNotBlank()) { Text("Guardar cierre", color = c.ink) }
    }
}

// =====================================================================
// Simulacro de examen
// =====================================================================

@Composable
private fun Simulacro(vm: HiloViewModel, book: Book?, onReview: (List<StudyCard>) -> Unit, onClose: () -> Unit) {
    val c = LocalHilo.current
    var n by remember { mutableIntStateOf(20) }
    var minutes by remember { mutableIntStateOf(0) }
    var all by remember { mutableStateOf(book == null) }
    var qs by remember { mutableStateOf<List<StudyCard>?>(null) }
    var i by remember { mutableIntStateOf(0) }
    var show by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<String?>(null) }
    val results = remember { mutableStateListOf<Pair<StudyCard, Boolean>>() }
    var left by remember { mutableIntStateOf(0) }
    val pool = if (all || book == null) vm.study.cards.toList() else vm.study.cardsOf(book.id)

    LaunchedEffect(qs, minutes) {
        if (qs == null || minutes == 0) return@LaunchedEffect
        left = minutes * 60
        while (left > 0 && i < (qs?.size ?: 0)) { delay(1000); left-- }
        if (left <= 0) i = qs?.size ?: 0
    }

    OverlayFrame("Simulacro de examen", onClose) {
        val list = qs
        if (list == null) {
            Text("Preguntas mezcladas de tus tarjetas, con tiempo si quieres. Al final ves tu nota y un plan con tus fallos.", color = c.muted, fontSize = 14.sp)
            Label("Cantidad de preguntas")
            ChoiceRow(listOf("10", "20", "30"), listOf(10, 20, 30).indexOf(n)) { n = listOf(10, 20, 30)[it] }
            Label("Tiempo")
            ChoiceRow(listOf("Sin tiempo", "15 min", "30 min"), listOf(0, 15, 30).indexOf(minutes)) { minutes = listOf(0, 15, 30)[it] }
            if (book != null) { Label("De dónde"); ChoiceRow(listOf("Este documento", "Todos"), if (all) 1 else 0) { all = it == 1 } }
            Text("${pool.size} tarjetas disponibles.", color = c.muted, fontSize = 13.sp)
            BigButton("Empezar", {
                qs = mixQueue(pool.shuffled().take(n)).take(n)
                i = 0; results.clear()
            }, enabled = pool.size >= 3)
            return@OverlayFrame
        }
        if (i >= list.size) {
            val ok = results.count { it.second }
            val pct = if (results.isEmpty()) 0 else ok * 100 / results.size
            LaunchedEffect(Unit) {
                vm.motor.add(Entry(vm.motor.newId(), book?.id.orEmpty(), "simulacro", "Simulacro", "$ok de ${results.size}", System.currentTimeMillis(), pct))
            }
            Text("Nota: ${"%.1f".format(pct / 10f)} / 10", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = c.ink)
            Text("Acertaste $ok de ${results.size}.", color = c.ink, fontSize = 16.sp)
            val failed = results.filter { !it.second }.map { it.first }
            if (failed.isNotEmpty()) {
                Label("Plan de repaso")
                Text("Tus ${failed.size} fallos vuelven hoy a tu repaso, primero que el resto.", color = c.ink, fontSize = 14.sp)
                failed.take(8).forEach { Text("• " + it.front.take(90), color = c.muted, fontSize = 13.sp) }
                BigButton("Repasar mis fallos ahora", {
                    val t = today()
                    failed.forEach { f -> vm.study.update(f.copy(due = t)) }
                    onReview(failed.map { it.copy(due = t) })
                    onClose()
                })
            }
            TextButton(onClick = onClose) { Text("Cerrar") }
            return@OverlayFrame
        }
        val q = list[i]
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pregunta ${i + 1} de ${list.size}", color = c.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            if (minutes > 0) Text("%d:%02d".format(left / 60, left % 60), color = if (left < 60) c.lines[0] else c.ink, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { i / list.size.toFloat() }, color = c.accent, trackColor = c.chip, modifier = Modifier.fillMaxWidth())
        Text(q.front, color = c.ink, fontSize = 20.sp, lineHeight = 27.sp)
        // Opción múltiple cuando la respuesta es corta; si no, te autocorriges
        val options = remember(q.id) {
            if (q.back.length > 70) null
            else {
                val others = pool.filter { it.id != q.id && it.kind == q.kind && it.back.length <= 70 && it.back != q.back }.map { it.back }.distinct().shuffled().take(3)
                if (others.size < 2) null else (others + q.back).shuffled()
            }
        }
        fun next(correct: Boolean) {
            results.add(q to correct)
            i++; show = false; picked = null
        }
        if (options != null) {
            options.forEach { o ->
                val col = when {
                    picked == null -> c.surface
                    o == q.back -> c.lines[1].copy(alpha = 0.35f)
                    o == picked -> c.lines[0].copy(alpha = 0.3f)
                    else -> c.surface
                }
                Text(o, color = c.ink, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(col)
                    .border(1.dp, c.line, RoundedCornerShape(14.dp)).clickable(enabled = picked == null) { picked = o }.padding(14.dp))
            }
            if (picked != null) BigButton("Siguiente", { next(picked == q.back) })
        } else {
            if (!show) BigButton("Ver respuesta", { show = true })
            else {
                Text(q.back, color = c.accent, fontSize = 17.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { next(true) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = c.lines[1])) { Text("Acerté") }
                    OutlinedButton(onClick = { next(false) }, modifier = Modifier.weight(1f)) { Text("Fallé", color = c.ink) }
                }
            }
        }
    }
}

// =====================================================================
// Estadísticas
// =====================================================================

@Composable
private fun Bars(values: List<Int>, color: Color, labels: List<String> = emptyList()) {
    val c = LocalHilo.current
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val w = size.width / values.size
        values.forEachIndexed { i, v ->
            val h = size.height * v / max
            drawRoundRect(color, Offset(i * w + w * 0.15f, size.height - h), Size(w * 0.7f, h.coerceAtLeast(2f)), CornerRadius(6f, 6f))
        }
    }
    if (labels.isNotEmpty()) Row(Modifier.fillMaxWidth()) { labels.forEach { Text(it, color = c.muted, fontSize = 10.sp, modifier = Modifier.weight(1f)) } }
}

@Composable
fun StatsView(vm: HiloViewModel, onClose: () -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val m = vm.motor
    val cards = vm.study.cards.toList()
    val studied = cards.filter { it.reps > 0 || it.stab > 0f }
    val health = if (studied.isEmpty()) null else (studied.map { it.recall() }.average() * 100).roundToInt()
    val hist = remember { Stats.history(ctx, 14) }
    val (acc, nAcc) = m.accuracy(null, 30)
    OverlayFrame("Estadísticas", onClose) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            @Composable
            fun Tile(v: String, l: String, mod: Modifier) {
                Column(mod.clip(RoundedCornerShape(16.dp)).background(c.surface).padding(12.dp)) {
                    Text(v, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.ink)
                    Text(l, color = c.muted, fontSize = 12.sp)
                }
            }
            Tile("${vm.study.streak}", "días de racha", Modifier.weight(1f))
            Tile("${hist.sumOf { it.first }} min", "leídos en 14 días", Modifier.weight(1f))
            Tile(health?.let { "$it %" } ?: "—", "salud del conocimiento", Modifier.weight(1f))
        }
        Panel {
            Text("Minutos de lectura por día", color = c.ink, fontWeight = FontWeight.Bold)
            Bars(hist.map { it.first }, c.accent)
            Text("Tarjetas repasadas por día", color = c.ink, fontWeight = FontWeight.Bold)
            Bars(m.perDay(14), c.lines[3])
            Text(if (nAcc == 0) "Todavía no hay repasos." else "Aciertos en 30 días: ${(acc * 100).roundToInt()} % de $nAcc respuestas.", color = c.ink, fontSize = 14.sp)
        }
        Panel {
            Text("¿Sabes lo que crees que sabes?", color = c.ink, fontWeight = FontWeight.Bold)
            listOf(1 to "Cuando dices «Lo sé»", 2 to "Cuando dices «Dudo»", 3 to "Cuando dices «No lo sé»").forEach { (k, l) ->
                val (p, n) = m.calibration(k)
                if (n > 0) Text("$l aciertas el ${(p * 100).roundToInt()} % ($n veces)", color = c.ink, fontSize = 14.sp)
            }
            val (sure, ns) = m.calibration(1)
            if (ns >= 10 && sure < 0.7f) Text("Ojo: crees que sabes más de lo que recuerdas. Esas tarjetas vuelven antes; explícalas con tus palabras.", color = c.lines[2], fontSize = 13.sp)
            else if (ns >= 10) Text("Tu confianza coincide con lo que sabes. ¡Buena señal!", color = c.lines[1], fontSize = 13.sp)
            if (m.reviews.none { it.conf > 0 }) Text("En el repaso, antes de ver la respuesta, elige «Lo sé», «Dudo» o «No lo sé».", color = c.muted, fontSize = 13.sp)
        }
        val books by vm.repo.books.collectAsStateWithLifecycle()
        val byBook = cards.groupBy { it.bookId }
        if (byBook.isNotEmpty()) Panel {
            Text("Por documento", color = c.ink, fontWeight = FontWeight.Bold)
            byBook.entries.sortedBy { (_, l) -> l.filter { it.reps > 0 }.map { it.recall() }.average().takeIf { !it.isNaN() } ?: 1.0 }.forEach { (id, l) ->
                val title = books.firstOrNull { it.id == id }?.title ?: return@forEach
                val r = l.filter { it.reps > 0 || it.stab > 0f }.map { it.recall() }.average().takeIf { !it.isNaN() }
                val fr = l.count { it.fragile }
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text(title, color = c.ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(progress = { (r ?: 0.0).toFloat() }, color = if ((r ?: 0.0) < 0.7) c.lines[2] else c.accent, trackColor = c.chip, modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)))
                        Text(
                            "  " + (r?.let { "nota ${"%.1f".format(it * 10)}" } ?: "sin repasos") + if (fr > 0) " · $fr frágiles" else "",
                            color = c.muted, fontSize = 12.sp,
                        )
                    }
                }
            }
        }
        val sims = m.entriesOf("simulacro").take(6)
        if (sims.isNotEmpty()) Panel {
            Text("Simulacros", color = c.ink, fontWeight = FontWeight.Bold)
            sims.forEach { e ->
                Text("${java.text.SimpleDateFormat("d MMM", java.util.Locale.forLanguageTag("es")).format(java.util.Date(e.time))}: nota ${"%.1f".format(e.score / 10f)} (${e.text})", color = c.ink, fontSize = 14.sp)
            }
        }
        val recalls = m.entriesOf("recuerdo").take(5)
        if (recalls.isNotEmpty()) Panel {
            Text("Recuerdo libre", color = c.ink, fontWeight = FontWeight.Bold)
            recalls.forEach { e -> Text("${e.title.take(40)}: ${e.score} %", color = c.ink, fontSize = 14.sp) }
        }
    }
}
