package com.hilo.lector.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hilo.lector.HiloViewModel
import com.hilo.lector.Screen
import com.hilo.lector.data.Book
import com.hilo.lector.data.CLASS_COLORS
import com.hilo.lector.data.ClassEvent
import com.hilo.lector.data.ClassNote
import com.hilo.lector.data.Clase
import com.hilo.lector.data.DocExport
import com.hilo.lector.data.EVENT_KINDS
import com.hilo.lector.data.StudyCard
import com.hilo.lector.data.today
import com.hilo.lector.text.Analysis
import com.hilo.lector.text.ClassGraph
import com.hilo.lector.text.Idea
import com.hilo.lector.text.Quiz
import com.hilo.lector.text.Smart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val TOPIC_COLORS = listOf(0xFF2E9E8F, 0xFF5B7CFA, 0xFFE0644F, 0xFFD39B1E, 0xFF9C5BD1, 0xFF3BA55C, 0xFFE05297, 0xFF78909C).map { Color(it) }
private fun topicColor(t: Int) = TOPIC_COLORS[t.coerceAtLeast(0) % TOPIC_COLORS.size]
private val DAY_FMT = DateTimeFormatter.ofPattern("EEE d 'de' MMM", Locale("es"))
private fun dayText(d: Long) = LocalDate.ofEpochDay(d).format(DAY_FMT).replaceFirstChar { it.uppercase() }
private fun inDays(d: Long): String = when (val n = (d - today()).toInt()) {
    0 -> "hoy"; 1 -> "mañana"; -1 -> "ayer"
    else -> if (n > 0) "en $n días" else "hace ${-n} días"
}

// =====================================================================
// Lista de clases
// =====================================================================

@Composable
fun ClassesScreen(vm: HiloViewModel, id: String?) {
    val clase = id?.let { vm.classes.get(it) }
    if (clase != null) ClassDetail(vm, clase) else ClassList(vm)
}

@Composable
private fun ClassList(vm: HiloViewModel) {
    val c = LocalHilo.current
    var newOne by remember { mutableStateOf(false) }
    var areasTab by rememberSaveable { mutableStateOf(false) }
    BackHandler { vm.screen = Screen.Library }
    Box(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 760.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = { vm.screen = Screen.Library }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = c.ink) }
                Text("Clases", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = c.ink, modifier = Modifier.weight(1f))
                HelpButton(vm, if (areasTab) "areas" else "clases", tint = c.ink)
            }
            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                ChoiceRow(listOf("Mis clases", "Áreas de referencia"), if (areasTab) 1 else 0) { areasTab = it == 1 }
            }
            val list = (if (areasTab) vm.classes.areas else vm.classes.courses).sortedBy { cl -> vm.classes.nextExam(cl.id)?.day ?: Long.MAX_VALUE }
            LazyColumn(
                Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (list.isEmpty() && areasTab) item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Tu biblioteca de referencia por campo", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Un área junta todos los documentos de un campo para siempre: por ejemplo, todas las leyes de Honduras. Hilo aprende de ellos: " +
                                "enlaza las remisiones entre normas, junta las definiciones legales y el vocabulario técnico, arma plantillas a partir de tus modelos y, " +
                                "al leer cualquier documento, te muestra qué dicen tus fuentes sobre lo mismo.",
                            color = c.muted, fontSize = 14.sp, lineHeight = 20.sp,
                        )
                        Button(onClick = { newOne = true }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("Crear mi primera área") }
                    }
                }
                if (list.isEmpty() && !areasTab) item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Organiza tu estudio por materia", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Crea una clase (por ejemplo «Sociología General») y agrégale libros, documentos y fotos de la pizarra. " +
                                "Hilo junta todo en un mapa de conceptos, arma un resumen por temas, crea preguntas de examen y te ayuda a planificar.",
                            color = c.muted, fontSize = 14.sp, lineHeight = 20.sp,
                        )
                        Button(onClick = { newOne = true }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("Crear mi primera clase") }
                    }
                }
                items(list, key = { it.id }) { cl -> ClassCard(vm, cl) { vm.screen = Screen.Classes(cl.id) } }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { newOne = true }, containerColor = c.accent, contentColor = c.accentInk,
            icon = { Icon(Icons.Rounded.Add, null) }, text = { Text(if (areasTab) "Nueva área" else "Nueva clase") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }
    if (newOne) ClassDialog(vm, null, area = areasTab) { cl -> newOne = false; if (cl != null) vm.screen = Screen.Classes(cl.id) }
}

@Composable
private fun ClassCard(vm: HiloViewModel, cl: Clase, onOpen: () -> Unit) {
    val c = LocalHilo.current
    val books = cl.books.mapNotNull { vm.repo.get(it) }.filter { !it.trashed }
    val exam = vm.classes.nextExam(cl.id)
    val prog = if (books.isEmpty()) 0f else books.map { it.progress }.average().toFloat()
    val due = vm.study.due(null).count { it.bookId in cl.books || it.bookId == "clase:${cl.id}" }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).clickable(onClick = onOpen),
    ) {
        Box(Modifier.width(8.dp).height(118.dp).background(Color(cl.color)))
        Column(Modifier.weight(1f).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(cl.name, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    if (cl.isArea) com.hilo.lector.data.FIELDS.firstOrNull { it.first == cl.field }?.second else cl.teacher.takeIf { it.isNotBlank() },
                    "${books.size} ${if (books.size == 1) "material" else "materiales"}", if (due > 0) "$due tarjetas para hoy" else null,
                ).joinToString(" · "),
                color = c.muted, fontSize = 13.sp, maxLines = 1,
            )
            if (exam != null) Text("📅 ${exam.title.ifBlank { "Examen" }} ${inDays(exam.day)}", color = if (exam.day - today() <= 7) c.lines[0] else c.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            LinearProgressIndicator(progress = { prog }, color = Color(cl.color), trackColor = c.chip, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(5.dp).clip(RoundedCornerShape(3.dp)))
        }
    }
}

/** Crear o editar una clase. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ClassDialog(vm: HiloViewModel, edit: Clase?, area: Boolean = edit?.isArea == true, onDone: (Clase?) -> Unit) {
    val c = LocalHilo.current
    var name by remember { mutableStateOf(edit?.name.orEmpty()) }
    var teacher by remember { mutableStateOf(edit?.teacher.orEmpty()) }
    var color by remember { mutableIntStateOf(edit?.color ?: CLASS_COLORS[vm.classes.classes.size % CLASS_COLORS.size]) }
    var field by remember { mutableStateOf(edit?.field ?: "otro") }
    AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text(if (edit == null) (if (area) "Nueva área de referencia" else "Nueva clase") else "Editar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(if (area) "Nombre (ej.: Derecho hondureño)" else "Nombre (ej.: Sociología General)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (!area) OutlinedTextField(teacher, { teacher = it }, label = { Text("Profesor o profesora (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Campo (activa sus herramientas)", color = c.muted, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.hilo.lector.data.FIELDS.forEach { (k, l) ->
                        Text(l, color = if (k == field) Color.White else c.ink, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(50)).background(if (k == field) Color(color) else c.chip).clickable { field = k }.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
                Text("Color", color = c.muted, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CLASS_COLORS.forEach { col ->
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(Color(col))
                                .border(3.dp, if (col == color) c.ink else Color.Transparent, CircleShape).clickable { color = col },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (edit == null) onDone(vm.classes.create(name, color, teacher, if (area) "area" else "clase", field))
                else { vm.classes.update(edit.id) { it.copy(name = name.trim().ifBlank { it.name }, teacher = teacher.trim(), color = color, field = field) }; onDone(edit) }
            }, enabled = name.isNotBlank()) { Text(if (edit == null) "Crear" else "Guardar") }
        },
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Cancelar") } },
    )
}

// =====================================================================
// Una clase
// =====================================================================

/** Pestañas de una clase o un área, según su campo. */
private fun tabsFor(cl: Clase): List<Pair<String, String>> {
    val t = ArrayList<Pair<String, String>>()
    t += "materiales" to "Materiales"; t += "mapa" to "Mapa"; t += "sintesis" to "Síntesis"
    if (cl.isArea) { t += "buscar" to "Buscar"; t += "glosario" to "Glosario"; t += "vocabulario" to "Vocabulario" }
    else { t += "estudiar" to "Estudiar"; t += "diario" to "Diario"; t += "calendario" to "Calendario"; t += "buscar" to "Buscar" }
    if (cl.field == "derecho") { t += "normas" to "Normas"; t += "plantillas" to "Plantillas"; t += "plazos" to "Plazos y sanciones" }
    if (cl.field == "electrica" || cl.field == "matematicas") t += "formulas" to "Fórmulas"
    if (cl.field != "otro") t += "herramientas" to "Herramientas"
    return t
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassDetail(vm: HiloViewModel, cl: Clase) {
    val c = LocalHilo.current
    var tab by rememberSaveable(cl.id) { mutableStateOf(if (cl.books.isEmpty()) "materiales" else "mapa") }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    BackHandler { vm.screen = Screen.Classes() }

    // Análisis de todos los materiales (se calcula al abrir Mapa, Síntesis o Estudiar)
    val books = cl.books.mapNotNull { vm.repo.get(it) }.filter { !it.trashed }
    val needs = tab in setOf("mapa", "sintesis", "estudiar", "glosario", "vocabulario", "normas", "plazos")
    var progress by remember { mutableStateOf("") }
    val state by produceState<Pair<List<Pair<String, Analysis>>, ClassGraph>?>(null, cl.books, needs) {
        if (!needs && value == null) return@produceState
        val docs = ArrayList<Pair<String, Analysis>>()
        books.forEachIndexed { i, b ->
            progress = "Leyendo ${i + 1} de ${books.size}: ${b.title.take(40)}"
            try { docs.add(b.id to vm.analysis(b) { _, _ -> }) }
            catch (e: CancellationException) { throw e }
            catch (_: Throwable) { }
        }
        progress = "Uniendo los conceptos…"
        val g = withContext(Dispatchers.Default) { Smart.buildClass(docs) }
        value = docs to g
    }

    Box(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 760.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = { vm.screen = Screen.Classes() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = c.ink) }
                Box(Modifier.size(12.dp).clip(CircleShape).background(Color(cl.color)))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(cl.name, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = if (cl.isArea) "Área de referencia · " + (com.hilo.lector.data.FIELDS.firstOrNull { it.first == cl.field }?.second ?: "") else cl.teacher
                if (sub.isNotBlank()) Text(sub, color = c.muted, fontSize = 13.sp, maxLines = 1)
                }
                IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, "Editar clase", tint = c.muted) }
                IconButton(onClick = { deleting = true }) { Icon(Icons.Rounded.Delete, "Borrar clase", tint = c.muted) }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                HelpButton(vm, "clase-$tab", tint = c.accent)
                tabsFor(cl).forEach { (k, t) ->
                    val on = k == tab
                    Text(
                        t, color = if (on) c.accentInk else c.ink, fontSize = 14.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Color(cl.color) else c.surface).clickable { tab = k }.padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    "materiales" -> MaterialsTab(vm, cl, books)
                    "diario" -> DiaryTab(vm, cl)
                    "calendario" -> CalendarTab(vm, cl, books)
                    "buscar" -> AreaSearchTab(vm, cl, (books + vm.classes.referenceBooks(cl).mapNotNull { vm.repo.get(it) }).distinctBy { it.id }.filter { !it.trashed })
                    "plantillas" -> TemplatesTab(vm, cl, books)
                    "formulas" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { FormulasTab(vm, cl); Spacer(Modifier.height(40.dp)) }
                    "herramientas" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ToolsList(vm, cl.field, cl.id); Spacer(Modifier.height(40.dp)) }
                    else -> {
                        val st = state
                        when {
                            books.isEmpty() -> Empty("Agrega materiales en la pestaña Materiales para ver el mapa, la síntesis y las preguntas.")
                            st == null -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                CircularProgressIndicator(color = Color(cl.color))
                                Text(progress.ifBlank { "Preparando la clase…" }, color = c.muted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                            }
                            st.second.concepts.isEmpty() -> Empty("No encontré texto en estos materiales. Si son fotos o PDF escaneados, activa «Reconocer texto en escaneados» en Ajustes.")
                            tab == "mapa" -> ClassMapTab(vm, cl, books, st.first, st.second)
                            tab == "sintesis" -> SynthesisTab(vm, cl, books, st.first, st.second)
                            tab == "glosario" -> AreaGlossaryTab(vm, cl, books, st.first)
                            tab == "vocabulario" -> AreaVocabTab(vm, cl, books, st.first)
                            tab == "normas" -> NormsTab(vm, cl, books, st.first)
                            tab == "plazos" -> DeadlinesTab(vm, cl, books, st.first)
                            else -> ClassStudyTab(vm, cl, books, st.first, st.second)
                        }
                    }
                }
            }
        }
    }
    if (editing) ClassDialog(vm, cl) { editing = false }
    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text("¿Borrar «${cl.name}»?") },
        text = { Text("Se borran la clase, su diario y su calendario. Tus libros, tarjetas y subrayados quedan en la biblioteca.") },
        confirmButton = { TextButton(onClick = { deleting = false; vm.screen = Screen.Classes(); vm.classes.delete(cl.id) }) { Text("Borrar", color = c.lines[0]) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancelar") } },
    )
}

@Composable
private fun Empty(text: String) {
    val c = LocalHilo.current
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = c.muted, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
    }
}

@Composable
private fun SectionCard(title: String, color: Color? = null, content: @Composable () -> Unit) {
    val c = LocalHilo.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface)
            .then(if (color != null) Modifier.border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(18.dp)) else Modifier).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = color ?: c.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        content()
    }
}

// ---------------------------------------------------------------------
// Materiales
// ---------------------------------------------------------------------

@Composable
private fun MaterialsTab(vm: HiloViewModel, cl: Clase, books: List<Book>) {
    val c = LocalHilo.current
    var picking by remember { mutableStateOf(false) }
    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        vm.importInto(uris) { list -> vm.classes.addBooks(cl.id, list.map { it.id }); vm.toast = "Agregué ${list.size} a la clase." }
    }
    val pickPhotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(60)) { uris ->
        vm.importInto(uris) { list -> vm.classes.addBooks(cl.id, list.map { it.id }); vm.toast = "Fotos agregadas. Hilo reconoce su texto para el mapa." }
    }
    LazyColumn(TABLET_WIDTH, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Libros, documentos, artículos, leyes y fotos de la pizarra o de tus apuntes. Todo lo que agregues entra al mapa y a la síntesis.", color = c.muted, fontSize = 13.sp)
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { picking = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(cl.color), contentColor = Color.White)) { Text("+ De tu biblioteca") }
                OutlinedButton(onClick = { pickFiles.launch(arrayOf("*/*")) }) { Text("+ Archivos", color = c.ink) }
                OutlinedButton(onClick = { pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("+ Fotos", color = c.ink) }
            }
        }
        if (!cl.isArea) item {
            var pickAreas by remember { mutableStateOf(false) }
            val used = cl.areas.mapNotNull { vm.classes.get(it) }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.band).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Áreas de referencia que usa", color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(if (used.isEmpty()) "Ninguna. Conecta un área (por ejemplo, tus leyes) para buscar en ella desde esta clase." else used.joinToString(" · ") { it.name }, color = c.muted, fontSize = 13.sp)
                TextButton(onClick = { pickAreas = true }) { Text(if (used.isEmpty()) "Conectar un área" else "Cambiar") }
            }
            if (pickAreas) AlertDialog(
                onDismissRequest = { pickAreas = false },
                title = { Text("Áreas que usa ${cl.name}") },
                text = {
                    Column {
                        if (vm.classes.areas.isEmpty()) Text("Todavía no tienes áreas. Créalas en Clases → Áreas de referencia.", color = c.muted)
                        vm.classes.areas.forEach { a ->
                            val on = a.id in cl.areas
                            Row(Modifier.fillMaxWidth().clickable { vm.classes.update(cl.id) { it.copy(areas = if (on) it.areas - a.id else it.areas + a.id) } }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(on, { vm.classes.update(cl.id) { it.copy(areas = if (on) it.areas - a.id else it.areas + a.id) } })
                                Text(a.name, color = c.ink)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { pickAreas = false }) { Text("Listo") } },
            )
        }
        if (books.isEmpty()) item { Text("Todavía no hay materiales.", color = c.muted, modifier = Modifier.padding(vertical = 20.dp)) }
        items(books, key = { it.id }) { b ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).clickable { vm.open(b) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BookCover(vm, b, Modifier.size(width = 46.dp, height = 64.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(b.title, color = c.ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(b.author.takeIf { it.isNotBlank() }, com.hilo.lector.text.Formats.label(b.type)).joinToString(" · "), color = c.muted, fontSize = 12.sp, maxLines = 1)
                    LinearProgressIndicator(progress = { b.progress }, color = Color(cl.color), trackColor = c.chip, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)))
                }
                TextButton(onClick = { vm.openStudy(b.id) }) { Text("Estudiar") }
                IconButton(onClick = { vm.classes.removeBook(cl.id, b.id); vm.undoable("Quitado de la clase") { vm.classes.addBooks(cl.id, listOf(b.id)) } }) {
                    Icon(Icons.Rounded.Close, "Quitar de la clase", tint = c.muted)
                }
            }
        }
    }
    if (picking) LibraryPicker(vm, cl) { picking = false }
}

/** Elegir libros de la biblioteca para la clase. */
@Composable
private fun LibraryPicker(vm: HiloViewModel, cl: Clase, onDone: () -> Unit) {
    val c = LocalHilo.current
    var q by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(cl.books.toSet()) }
    val all = vm.repo.books.value.filter { !it.trashed && it.type != "comic" }
    val shown = all.filter { q.isBlank() || Smart.fold(it.title + " " + it.author).contains(Smart.fold(q.trim())) }.sortedByDescending { it.updated }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Agregar de tu biblioteca") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(q, { q = it }, placeholder = { Text("Buscar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Column(Modifier.height(380.dp).verticalScroll(rememberScrollState())) {
                    shown.take(200).forEach { b ->
                        val on = b.id in chosen
                        Row(Modifier.fillMaxWidth().clickable { chosen = if (on) chosen - b.id else chosen + b.id }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(on, { chosen = if (on) chosen - b.id else chosen + b.id })
                            Column(Modifier.weight(1f)) {
                                Text(b.title, color = c.ink, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                if (b.author.isNotBlank()) Text(b.author, color = c.muted, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.classes.update(cl.id) { it.copy(books = it.books.filter { id -> id in chosen } + (chosen - it.books.toSet())) }
                onDone()
            }) { Text("Listo (${chosen.size})") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Cancelar") } },
    )
}

// ---------------------------------------------------------------------
// Mapa de la clase
// ---------------------------------------------------------------------

/** Acomoda los conceptos: se repelen entre sí y las uniones los atraen (los temas quedan juntos). */
private fun layoutGraph(g: ClassGraph, w: Float, h: Float, dens: Float = 2.75f): Pair<Array<Offset>, Float> {
    val n = g.concepts.size
    val r = java.util.Random(7)
    val nt = max(1, g.topics.size)
    val pos = Array(n) { i ->
        val t = g.concepts[i].topic
        val ang = 2 * Math.PI * t / nt
        Offset((w / 2 + w * 0.28f * kotlin.math.cos(ang)).toFloat() + r.nextFloat() * 60 - 30, (h / 2 + h * 0.28f * kotlin.math.sin(ang)).toFloat() + r.nextFloat() * 60 - 30)
    }
    val k = sqrt(w * h / max(1, n)) * 0.75f
    var temp = w / 8f
    repeat(260) {
        val disp = Array(n) { Offset.Zero }
        for (i in 0 until n) for (j in i + 1 until n) {
            val d = pos[i] - pos[j]
            val dist = max(1f, d.getDistance())
            val f = k * k / dist * (if (g.concepts[i].topic == g.concepts[j].topic) 0.8f else 1.2f)
            val v = d / dist * f
            disp[i] += v; disp[j] -= v
        }
        for ((a, b, wgt) in g.edges) {
            val d = pos[a] - pos[b]
            val dist = max(1f, d.getDistance())
            val f = dist * dist / k * (0.4f + 0.25f * kotlin.math.ln(1f + wgt))
            val v = d / dist * f
            disp[a] -= v; disp[b] += v
        }
        for (i in 0 until n) {
            disp[i] += (Offset(w / 2, h / 2) - pos[i]) * 0.02f
            val l = max(1f, disp[i].getDistance())
            pos[i] += disp[i] / l * min(l, temp)
        }
        temp *= 0.985f
    }
    // Sin círculos ni nombres encimados, y centrado
    val maxScore = g.concepts.maxOf { it.score }
    val rr = FloatArray(n) { (12f + 26f * sqrt(g.concepts[it].score / maxScore)) * dens * 0.55f }
    val lw = FloatArray(n) { g.concepts[it].label.length * 6.3f * dens }
    val fit = untangle(pos, rr, lw, 15f * dens, w, h, 14f * dens)
    return pos to fit
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassMapTab(vm: HiloViewModel, cl: Clase, books: List<Book>, docs: List<Pair<String, Analysis>>, g: ClassGraph) {
    val c = LocalHilo.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    var sel by remember(g) { mutableStateOf<Int?>(null) }
    var topicOn by remember(g) { mutableStateOf<Int?>(null) }
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val titles = books.associate { it.id to it.title }
    val maxScore = g.concepts.maxOf { it.score }

    LazyColumn(TABLET_WIDTH, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "${g.concepts.size} conceptos de ${docs.size} ${if (docs.size == 1) "material" else "materiales"}, agrupados en ${g.topics.size} temas. " +
                    "El número es cuántas veces aparece; las líneas unen los que salen en la misma oración. Pellizca para acercar y toca un concepto.",
                color = c.muted, fontSize = 13.sp,
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                g.topics.forEach { t ->
                    val on = topicOn == t.id
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(if (on) topicColor(t.id) else topicColor(t.id).copy(alpha = 0.15f))
                            .clickable { topicOn = if (on) null else t.id }.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(topicColor(t.id)))
                        Text(t.name, color = if (on) Color.White else c.ink, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp), maxLines = 1)
                    }
                }
            }
        }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth().height(460.dp).clip(RoundedCornerShape(20.dp)).background(c.surface)) {
                val w = with(density) { maxWidth.toPx() }
                val h = with(density) { maxHeight.toPx() }
                val laid by produceState<Pair<Array<Offset>, Float>?>(null, g, w, h) {
                    value = withContext(Dispatchers.Default) { layoutGraph(g, w, h, density.density) }
                }
                val p = laid?.first
                LaunchedEffect(laid) { laid?.let { (_, fit) -> zoom = fit; pan = Offset(w / 2f, h / 2f) * (1f - fit) } }
                if (p == null) CircularProgressIndicator(color = Color(cl.color), modifier = Modifier.align(Alignment.Center))
                else Canvas(
                    Modifier.fillMaxSize()
                        .pointerInput(g) { detectTransformGestures { centroid, dp, z, _ -> val nz = (zoom * z).coerceIn(0.3f, 4f); pan = (pan + dp) + (centroid - pan) * (1 - nz / zoom); zoom = nz } }
                        .pointerInput(g, p) {
                            detectTapGestures { o ->
                                val gp = (o - pan) / zoom
                                val hit = g.concepts.indices.minByOrNull { (p[it] - gp).getDistance() }
                                sel = if (hit != null && (p[hit] - gp).getDistance() < 46.dp.toPx() / zoom) hit else null
                            }
                        },
                ) {
                    val near = sel?.let { s -> g.neighbors(s).map { it.first }.toSet() + s }
                    fun on(i: Int) = (near == null || i in near) && (topicOn == null || g.concepts[i].topic == topicOn)
                    val tr = { o: Offset -> o * zoom + pan }
                    for ((a, b, wgt) in g.edges) {
                        val vis = on(a) && on(b)
                        drawLine(c.muted.copy(alpha = if (vis) (0.18f + 0.06f * min(wgt, 8)) else 0.05f), tr(p[a]), tr(p[b]), strokeWidth = (1f + kotlin.math.ln(1f + wgt)) * zoom)
                    }
                    g.concepts.forEachIndexed { i, cc ->
                        val rad = (12f + 26f * sqrt(cc.score / maxScore)) * density.density * 0.55f * zoom
                        val col = topicColor(cc.topic)
                        val alpha = if (on(i)) 1f else 0.15f
                        drawCircle(col, rad, tr(p[i]), alpha = alpha * 0.9f)
                        if (sel == i) drawCircle(c.ink, rad + 4f, tr(p[i]), style = Stroke(3f))
                        val num = measurer.measure("${cc.total}", TextStyle(fontSize = (9f * zoom).coerceIn(6f, 16f).sp, color = Color.White.copy(alpha = alpha), fontWeight = FontWeight.Bold))
                        if (rad > 9f * density.density) drawText(num, topLeft = tr(p[i]) - Offset(num.size.width / 2f, num.size.height / 2f))
                        val lab = measurer.measure(cc.label, TextStyle(fontSize = (11f * zoom).coerceIn(6f, 18f).sp, color = c.ink.copy(alpha = alpha), fontWeight = if (sel == i) FontWeight.Bold else FontWeight.Medium))
                        drawText(lab, topLeft = tr(p[i]) + Offset(-lab.size.width / 2f, rad + 2f))
                    }
                }
                TextButton(onClick = { val f = laid?.second ?: 1f; zoom = f; pan = Offset(w / 2f, h / 2f) * (1f - f); sel = null; topicOn = null }, modifier = Modifier.align(Alignment.BottomEnd)) { Text("Centrar") }
            }
        }
        sel?.let { i -> item { ConceptPanel(vm, cl, books, docs, g, i, titles) { sel = it } } }
    }
}

/** Todo sobre un concepto de la clase: dónde aparece, cómo lo define cada material, con qué se une. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConceptPanel(vm: HiloViewModel, cl: Clase, books: List<Book>, docs: List<Pair<String, Analysis>>, g: ClassGraph, i: Int, titles: Map<String, String>, onSelect: (Int) -> Unit) {
    val c = LocalHilo.current
    val cc = g.concepts[i]
    val defs = g.defs[cc.key].orEmpty()
    val marks = vm.study.marks.filter { it.bookId in cl.books && Smart.matches(it.text, listOf(cc.label)) }.take(4)
    val cards = vm.study.cards.filter { (it.bookId in cl.books || it.bookId == "clase:${cl.id}") && Smart.matches(it.front + " " + it.back, listOf(cc.label)) }
    SectionCard(cc.label, topicColor(cc.topic)) {
        Text("${cc.total} veces en total · tema: ${g.topics.getOrNull(cc.topic)?.name.orEmpty()}", color = c.muted, fontSize = 13.sp)
        Text("En tus materiales", color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        cc.docs.entries.sortedByDescending { it.value }.forEach { (bid, n) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { vm.openStudy(bid) }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(titles[bid] ?: "", color = c.ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("$n veces", color = c.muted, fontSize = 13.sp)
            }
        }
        if (defs.isNotEmpty()) {
            Text(if (defs.map { it.bookId }.distinct().size > 1) "Cómo lo define cada material" else "Definición", color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            defs.distinctBy { it.bookId to it.definition.take(60) }.take(6).forEach { d ->
                val b = books.firstOrNull { it.id == d.bookId }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.band).clickable { vm.openAt(d.bookId, true, d.para) }.padding(10.dp)) {
                    Text(listOfNotNull(b?.author?.takeIf { it.isNotBlank() }, b?.title).joinToString(" — "), color = topicColor(cc.topic), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("«${d.definition}»", color = c.ink, fontSize = 14.sp, lineHeight = 19.sp)
                }
            }
        } else Text("No encontré una definición escrita en los materiales («${cc.label} es…»).", color = c.muted, fontSize = 13.sp)
        val nb = g.neighbors(i).take(10)
        if (nb.isNotEmpty()) {
            Text("Se relaciona con", color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                nb.forEach { (j, w) ->
                    Text(
                        "${g.concepts[j].label} · $w", color = c.ink, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(topicColor(g.concepts[j].topic).copy(alpha = 0.18f)).clickable { onSelect(j) }.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
        if (marks.isNotEmpty()) {
            Text("Tus subrayados", color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            marks.forEach { m -> Text("«${m.text.take(200)}»", color = c.ink, fontSize = 13.sp, modifier = Modifier.clickable { vm.openAt(m.bookId, true, m.para) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val d = defs.firstOrNull()
                vm.study.add(StudyCard(vm.study.newId(), d?.bookId ?: "clase:${cl.id}", "qa", "¿Qué es «${cc.label}»?", d?.definition ?: "(escribe la respuesta)", source = cl.name, due = today(), created = System.currentTimeMillis()))
                vm.toast = "Tarjeta creada."
            }) { Text("Crear tarjeta", color = c.ink) }
            Text(if (cards.isEmpty()) "Sin tarjetas aún" else "${cards.size} tarjetas", color = c.muted, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

// ---------------------------------------------------------------------
// Síntesis
// ---------------------------------------------------------------------

private class TopicSummary(val topic: com.hilo.lector.text.ClassTopic, val defs: List<Pair<String, com.hilo.lector.text.ClassDef>>, val ideas: List<Idea>, val marks: List<String>)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SynthesisTab(vm: HiloViewModel, cl: Clase, books: List<Book>, docs: List<Pair<String, Analysis>>, g: ClassGraph) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val titles = books.associate { it.id to it.title }
    var sheet by remember { mutableStateOf(false) }
    val sums by produceState<List<TopicSummary>?>(null, g) {
        value = withContext(Dispatchers.Default) {
            g.topics.map { t ->
                val defs = t.concepts.mapNotNull { k -> g.defs[k]?.firstOrNull()?.let { (g.concepts[g.byKey.getValue(k)].label) to it } }.take(5)
                val labels = t.concepts.mapNotNull { g.byKey[it]?.let { i -> g.concepts[i].label } }
                val marks = vm.study.marks.filter { it.bookId in cl.books && Smart.matches(it.text, labels.take(6)) }.map { it.text }.distinct().take(3)
                TopicSummary(t, defs, Smart.topicIdeas(docs, t, 4), marks)
            }
        }
    }
    val important = vm.classes.notesOf(cl.id).filter { it.important && it.text.isNotBlank() }

    fun export(full: Boolean): List<String> {
        val s = sums.orEmpty()
        val out = ArrayList<String>()
        if (full) out.add("Materiales: " + books.joinToString("; ") { listOf(it.author, it.title).filter { x -> x.isNotBlank() }.joinToString(", ") })
        s.forEachIndexed { k, ts ->
            out.add("TEMA ${k + 1}: ${ts.topic.name.uppercase()}")
            val labels = ts.topic.concepts.mapNotNull { g.byKey[it]?.let { i -> g.concepts[i].label } }
            out.add("Conceptos clave: " + labels.take(if (full) 10 else 6).joinToString(", "))
            ts.defs.take(if (full) 5 else 3).forEach { (l, d) -> out.add("• $l: ${if (full) d.definition else d.definition.take(140)}") }
            if (full) {
                ts.ideas.forEach { out.add("— ${it.text} (${titles[it.bookId].orEmpty()})") }
                ts.marks.forEach { out.add("Subrayado: «$it»") }
            } else ts.ideas.firstOrNull()?.let { out.add("Idea central: ${it.text.take(200)}") }
        }
        if (important.isNotEmpty()) {
            out.add("LO QUE EL PROFESOR DIJO QUE VA PARA EL EXAMEN")
            important.forEach { out.add("★ ${it.text} (${dayText(it.day)})") }
        }
        return out
    }

    val s = sums
    if (s == null) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(color = Color(cl.color)); Text("Armando la síntesis por temas…", color = c.muted, modifier = Modifier.padding(16.dp))
        }
        return
    }
    LazyColumn(TABLET_WIDTH, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Un resumen por temas armado con tus materiales: conceptos, definiciones, las ideas centrales de cada texto y tus subrayados.", color = c.muted, fontSize = 13.sp)
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { DocExport.share(ctx, "Síntesis — ${cl.name}", export(true)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(cl.color), contentColor = Color.White)) { Text("Exportar a Word") }
                OutlinedButton(onClick = { sheet = true }) { Text("Hoja de repaso", color = c.ink) }
                OutlinedButton(onClick = { share(ctx, "Síntesis — ${cl.name}\n\n" + export(true).joinToString("\n\n")) }) { Text("Compartir texto", color = c.ink) }
            }
        }
        if (important.isNotEmpty()) item {
            SectionCard("★ Va para el examen (según tu diario)", c.lines[0]) {
                important.take(8).forEach { Text("• ${it.text}", color = c.ink, fontSize = 14.sp) }
            }
        }
        items(s.size) { k ->
            val ts = s[k]
            val col = topicColor(ts.topic.id)
            SectionCard("Tema ${k + 1}: ${ts.topic.name}", col) {
                val labels = ts.topic.concepts.mapNotNull { g.byKey[it]?.let { i -> g.concepts[i].label } }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    labels.take(10).forEach { Text(it, color = c.ink, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(50)).background(col.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 5.dp)) }
                }
                if (ts.defs.isNotEmpty()) {
                    Text("Definiciones", color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    ts.defs.forEach { (l, d) ->
                        Text("$l: ", color = col, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(d.definition, color = c.ink, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.clickable { vm.openAt(d.bookId, true, d.para) })
                    }
                }
                if (ts.ideas.isNotEmpty()) {
                    Text("Ideas centrales", color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    ts.ideas.forEach { idea ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { vm.openAt(idea.bookId, true, idea.para) }.padding(vertical = 4.dp)) {
                            Text("• ${idea.text}", color = c.ink, fontSize = 14.sp, lineHeight = 19.sp)
                            Text(titles[idea.bookId].orEmpty(), color = c.muted, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                if (ts.marks.isNotEmpty()) {
                    Text("Tus subrayados", color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    ts.marks.forEach { Text("«${it.take(220)}»", color = c.ink, fontSize = 13.sp) }
                }
            }
        }
        item {
            Text("La síntesis elige y ordena frases de tus propios materiales (no inventa texto). Revísala y complétala con tus palabras.", color = c.muted, fontSize = 12.sp)
        }
    }
    if (sheet) AlertDialog(
        onDismissRequest = { sheet = false },
        title = { Text("Hoja de repaso") },
        text = { Column(Modifier.height(420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) { export(false).forEach { Text(it, color = c.ink, fontSize = 13.sp) } } },
        confirmButton = { TextButton(onClick = { DocExport.share(ctx, "Hoja de repaso — ${cl.name}", export(false)) }) { Text("Exportar") } },
        dismissButton = { TextButton(onClick = { sheet = false }) { Text("Cerrar") } },
    )
}

// ---------------------------------------------------------------------
// Estudiar la clase
// ---------------------------------------------------------------------

@Composable
private fun ClassStudyTab(vm: HiloViewModel, cl: Clase, books: List<Book>, docs: List<Pair<String, Analysis>>, g: ClassGraph) {
    val c = LocalHilo.current
    var quiz by remember { mutableStateOf<List<Quiz>?>(null) }
    val titles = books.associate { it.id to it.title }
    val cards = vm.study.cards.filter { it.bookId in cl.books || it.bookId == "clase:${cl.id}" }
    val due = cards.filter { it.due <= today() }
    val exam = vm.classes.nextExam(cl.id)

    LazyColumn(TABLET_WIDTH, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("Mazo de la clase") {
                Text("${cards.size} tarjetas de todos los materiales · ${due.size} para hoy", color = c.muted, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (due.isEmpty()) vm.toast = "No hay tarjetas pendientes en esta clase."
                        else { vm.pendingReview = mixQueue(due).take(30); vm.openStudy(null) }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(cl.color), contentColor = Color.White)) { Text("Repasar las de hoy") }
                    OutlinedButton(onClick = {
                        if (cards.isEmpty()) vm.toast = "Esta clase todavía no tiene tarjetas."
                        else { vm.pendingReview = mixQueue(cards).take(25); vm.openStudy(null) }
                    }) { Text("Todo mezclado", color = c.ink) }
                }
            }
        }
        // Semáforo por tema
        item {
            SectionCard("Cómo vas en cada tema") {
                Text("Según tus repasos de tarjetas que hablan de cada tema.", color = c.muted, fontSize = 12.sp)
                g.topics.forEach { t ->
                    val labels = t.concepts.mapNotNull { g.byKey[it]?.let { i -> g.concepts[i].label } }
                    val tc = cards.filter { Smart.matches(it.front + " " + it.back + " " + it.context, labels) }
                    val rec = if (tc.isEmpty()) -1f else tc.map { it.recall() }.average().toFloat()
                    val (dot, txt) = when {
                        rec < 0f -> c.line to "sin tarjetas"
                        rec >= 0.8f -> c.lines[2] to "lo dominas"
                        rec >= 0.5f -> c.lines[3] to "a medias"
                        else -> c.lines[0] to "repásalo"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(dot))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(t.name, color = c.ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${tc.size} tarjetas · $txt", color = c.muted, fontSize = 12.sp)
                        }
                        if (tc.isEmpty()) TextButton(onClick = {
                            var n = 0
                            t.concepts.forEach { k ->
                                val i = g.byKey[k] ?: return@forEach
                                val d = g.defs[k]?.firstOrNull() ?: return@forEach
                                vm.study.add(StudyCard(vm.study.newId(), d.bookId, "qa", "¿Qué es «${g.concepts[i].label}»?", d.definition, source = cl.name, due = today(), created = System.currentTimeMillis()))
                                n++
                            }
                            vm.toast = if (n == 0) "No encontré definiciones en este tema: crea tarjetas desde el texto." else "Creé $n tarjetas de este tema."
                        }) { Text("Crear") }
                    }
                }
            }
        }
        item {
            SectionCard("Simulacro de la clase") {
                Text("Preguntas armadas con todos tus materiales: definiciones, relaciones entre conceptos, autores y en qué texto se trata cada tema.", color = c.muted, fontSize = 13.sp)
                Button(onClick = {
                    val q = Smart.questions(g, docs, titles, 12, (System.currentTimeMillis() % 100000).toInt())
                    if (q.isEmpty()) vm.toast = "Hace falta más texto para armar preguntas." else quiz = q
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(cl.color), contentColor = Color.White)) { Text("Empezar simulacro") }
            }
        }
        item { ExamPlan(vm, cl, books, exam) }
        item {
            SectionCard("Avance de lectura") {
                books.forEach { b ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(b.title, color = c.ink, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("${(b.progress * 100).roundToInt()} %", color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    LinearProgressIndicator(progress = { b.progress }, color = Color(cl.color), trackColor = c.chip, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)))
                }
            }
        }
    }
    quiz?.let { q -> ClassQuiz(vm, cl, q) { quiz = null } }
}

/** Plan hasta el examen: reparte lo que falta leer y deja los últimos días para repasar. */
@Composable
private fun ExamPlan(vm: HiloViewModel, cl: Clase, books: List<Book>, exam: ClassEvent?) {
    val c = LocalHilo.current
    SectionCard("Plan hasta el examen") {
        if (exam == null) {
            Text("Pon la fecha del examen en Calendario y aquí te reparto lo que falta leer por día.", color = c.muted, fontSize = 13.sp)
            return@SectionCard
        }
        val days = (exam.day - today()).toInt()
        Text("${exam.title.ifBlank { "Examen" }}: ${dayText(exam.day)} (${inDays(exam.day)})", color = c.ink, fontWeight = FontWeight.SemiBold)
        val left = books.filter { it.progress < 0.98f }
        if (days <= 0) { Text("Hoy toca repasar: tarjetas, hoja de repaso y simulacro.", color = c.ink, fontSize = 14.sp); return@SectionCard }
        if (left.isEmpty()) { Text("Ya leíste todo. Usa los días que quedan para repasar y hacer simulacros.", color = c.ink, fontSize = 14.sp); return@SectionCard }
        val readDays = max(1, days - if (days > 4) 2 else 0)
        Text("Leer en $readDays ${if (readDays == 1) "día" else "días"}" + (if (days > 4) " y repasar los últimos 2." else "."), color = c.muted, fontSize = 13.sp)
        left.forEach { b ->
            val pages = if (b.pages > 0) ((1f - b.progress) * b.pages).roundToInt() else -1
            val perDay = if (pages > 0) "~${(pages + readDays - 1) / readDays} págs. por día" else "~${(((1f - b.progress) * 100) / readDays).roundToInt().coerceAtLeast(1)} % por día"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(b.title, color = c.ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(perDay, color = Color(cl.color), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun ClassQuiz(vm: HiloViewModel, cl: Clase, qs: List<Quiz>, onClose: () -> Unit) {
    val c = LocalHilo.current
    var i by remember { mutableIntStateOf(0) }
    var pick by remember { mutableStateOf<Int?>(null) }
    var right by remember { mutableIntStateOf(0) }
    val wrong = remember { mutableListOf<Quiz>() }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (i < qs.size) "Pregunta ${i + 1} de ${qs.size}" else "Resultado") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (i < qs.size) {
                    val q = qs[i]
                    Text(q.prompt, color = c.ink, fontSize = 15.sp, lineHeight = 21.sp)
                    q.options.forEachIndexed { k, o ->
                        val bg = when {
                            pick == null -> c.chip
                            k == q.answer -> c.lines[2].copy(alpha = 0.35f)
                            k == pick -> c.lines[0].copy(alpha = 0.3f)
                            else -> c.chip
                        }
                        Text(
                            o, color = c.ink, fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg)
                                .clickable(enabled = pick == null) { pick = k; if (k == q.answer) right++ else wrong.add(q) }.padding(12.dp),
                        )
                    }
                    if (pick != null) Text((if (pick == q.answer) "¡Correcto! " else "Era «${q.options[q.answer]}». ") + q.explain, color = c.muted, fontSize = 13.sp)
                } else {
                    Text("Acertaste $right de ${qs.size}.", color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(if (right * 10 >= qs.size * 8) "¡Muy bien preparado!" else if (right * 2 >= qs.size) "Vas bien. Repasa lo que fallaste." else "Conviene repasar los temas en rojo de «Cómo vas en cada tema».", color = c.muted)
                    if (wrong.isNotEmpty()) TextButton(onClick = {
                        wrong.forEach { q -> vm.study.add(StudyCard(vm.study.newId(), "clase:${cl.id}", "qa", q.prompt, q.options[q.answer], context = q.explain, source = cl.name, due = today(), created = System.currentTimeMillis())) }
                        vm.toast = "Creé ${wrong.size} tarjetas con lo que fallaste."; onClose()
                    }) { Text("Pasar las falladas a tarjetas") }
                }
            }
        },
        confirmButton = {
            if (i < qs.size) TextButton(onClick = { i++; pick = null }, enabled = pick != null) { Text(if (i == qs.size - 1) "Ver resultado" else "Siguiente") }
            else TextButton(onClick = onClose) { Text("Cerrar") }
        },
        dismissButton = { if (i < qs.size) TextButton(onClick = onClose) { Text("Salir") } },
    )
}

// ---------------------------------------------------------------------
// Diario de clase
// ---------------------------------------------------------------------

@Composable
private fun DiaryTab(vm: HiloViewModel, cl: Clase) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    var important by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var playing by remember { mutableStateOf<Long?>(null) }
    val player = remember { MediaPlayer() }
    DisposableEffect(Unit) { onDispose { runCatching { recorder?.stop(); recorder?.release() }; runCatching { player.release() } } }

    fun startRec() {
        val f = File(vm.classes.audioDir(cl.id), "clase_${System.currentTimeMillis()}.m4a")
        val r = (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder())
        runCatching {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64000); r.setAudioSamplingRate(22050)
            r.setOutputFile(f.path)
            r.prepare(); r.start()
            recorder = r; audioFile = f
        }.onFailure { runCatching { r.release() }; vm.toast = "No pude empezar a grabar." }
    }
    fun stopRec() { runCatching { recorder?.stop() }; runCatching { recorder?.release() }; recorder = null }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) startRec() else vm.toast = "Sin permiso del micrófono no puedo grabar." }

    val notes = vm.classes.notesOf(cl.id)
    LazyColumn(TABLET_WIDTH, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard("Hoy, ${dayText(today())}") {
                OutlinedTextField(text, { text = it }, placeholder = { Text("¿Qué se vio en clase? ¿Qué dijo el profe?") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.clip(RoundedCornerShape(50)).clickable { important = !important }.padding(end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (important) Icons.Rounded.Star else Icons.Rounded.StarBorder, null, tint = if (important) c.lines[3] else c.muted)
                        Text("Va para el examen", color = c.ink, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        if (recorder != null) stopRec()
                        else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRec()
                        else perm.launch(Manifest.permission.RECORD_AUDIO)
                    }) {
                        Icon(if (recorder != null) Icons.Rounded.Stop else Icons.Rounded.Mic, if (recorder != null) "Detener grabación" else "Grabar la clase", tint = if (recorder != null) c.lines[0] else c.accent)
                    }
                }
                if (recorder != null) Text("● Grabando… toca ■ para terminar.", color = c.lines[0], fontSize = 13.sp)
                else if (audioFile != null) Text("Grabación lista: se guarda con esta entrada.", color = c.muted, fontSize = 13.sp)
                Button(
                    onClick = {
                        stopRec()
                        if (text.isBlank() && audioFile == null) return@Button
                        vm.classes.putNote(ClassNote(vm.classes.newId(), cl.id, today(), text.trim(), important, audioFile?.path.orEmpty()))
                        text = ""; important = false; audioFile = null
                        vm.toast = "Guardado en el diario."
                    },
                    enabled = text.isNotBlank() || audioFile != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(cl.color), contentColor = Color.White),
                ) { Text("Guardar") }
            }
        }
        if (notes.isEmpty()) item { Text("Anota lo de cada clase: lo que se vio, las dudas y lo que el profe dice que va para el examen (★). Lo marcado con ★ aparece en la síntesis.", color = c.muted, fontSize = 13.sp) }
        var lastDay = -1L
        notes.forEach { n ->
            if (n.day != lastDay) { val d = n.day; item(key = "d$d") { Text(dayText(d), color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) } }
            lastDay = n.day
            item(key = n.id) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        if (n.important) Icon(Icons.Rounded.Star, "Va para el examen", tint = c.lines[3], modifier = Modifier.size(18.dp).padding(end = 4.dp))
                        Text(n.text.ifBlank { "(grabación)" }, color = c.ink, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (n.audio.isNotBlank() && File(n.audio).exists()) IconButton(onClick = {
                            runCatching {
                                if (playing == n.id) { player.stop(); playing = null }
                                else { player.reset(); player.setDataSource(n.audio); player.prepare(); player.start(); playing = n.id; player.setOnCompletionListener { playing = null } }
                            }
                        }) { Icon(if (playing == n.id) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, "Escuchar grabación", tint = c.accent) }
                        TextButton(onClick = { vm.classes.putNote(n.copy(important = !n.important)) }) { Text(if (n.important) "Quitar ★" else "★ Examen") }
                        if (n.text.isNotBlank()) TextButton(onClick = {
                            vm.study.add(StudyCard(vm.study.newId(), "clase:${cl.id}", "qa", "Clase del ${dayText(n.day)}: ¿qué se dijo sobre esto?", n.text, source = cl.name, due = today(), created = System.currentTimeMillis()))
                            vm.toast = "Tarjeta creada."
                        }) { Text("Tarjeta") }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { vm.classes.deleteNote(n.id) }) { Icon(Icons.Rounded.Delete, "Borrar", tint = c.muted, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// Calendario
// ---------------------------------------------------------------------

@Composable
private fun CalendarTab(vm: HiloViewModel, cl: Clase, books: List<Book>) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    var kind by remember { mutableStateOf("examen") }
    var title by remember { mutableStateOf("") }
    var day by remember { mutableStateOf(today() + 7) }
    val evs = vm.classes.eventsOf(cl.id)
    val (up, past) = evs.partition { it.day >= today() && !it.done }
    LazyColumn(TABLET_WIDTH, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard("Agregar fecha") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EVENT_KINDS.forEach { (k, l) ->
                        Text(l, color = if (k == kind) Color.White else c.ink, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(50)).background(if (k == kind) Color(cl.color) else c.chip).clickable { kind = k }.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                }
                OutlinedTextField(title, { title = it }, placeholder = { Text(when (kind) { "examen" -> "Ej.: Primer parcial"; "entrega" -> "Ej.: Ensayo sobre Durkheim"; "lectura" -> "Ej.: Leer cap. 3"; else -> "Qué es" }) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📅 ${dayText(day)} (${inDays(day)})", color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val d = LocalDate.ofEpochDay(day)
                        DatePickerDialog(ctx, { _, y, m, dd -> day = LocalDate.of(y, m + 1, dd).toEpochDay() }, d.year, d.monthValue - 1, d.dayOfMonth).show()
                    }) { Text("Cambiar fecha") }
                }
                Button(onClick = {
                    vm.classes.putEvent(ClassEvent(vm.classes.newId(), cl.id, day, kind, title.trim().ifBlank { EVENT_KINDS.first { it.first == kind }.second }))
                    title = ""; vm.toast = "Fecha agregada."
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(cl.color), contentColor = Color.White)) { Text("Agregar") }
            }
        }
        if (up.isNotEmpty()) item { Text("PRÓXIMAS", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        items(up, key = { it.id }) { e -> EventRow(vm, cl, e) }
        if (up.isEmpty()) item { Text("No hay fechas próximas. Agrega tus exámenes y entregas: Hilo arma el plan de estudio en «Estudiar».", color = c.muted, fontSize = 13.sp) }
        if (past.isNotEmpty()) item { Text("PASADAS O HECHAS", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        items(past.reversed(), key = { it.id }) { e -> EventRow(vm, cl, e) }
    }
}

@Composable
private fun EventRow(vm: HiloViewModel, cl: Clase, e: ClassEvent) {
    val c = LocalHilo.current
    val soon = !e.done && e.day >= today() && e.day - today() <= 7
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(e.done, { vm.classes.putEvent(e.copy(done = it)) })
        Column(Modifier.weight(1f)) {
            Text(e.title, color = if (e.done) c.muted else c.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("${EVENT_KINDS.firstOrNull { it.first == e.kind }?.second ?: ""} · ${dayText(e.day)} · ${inDays(e.day)}", color = if (soon) c.lines[0] else c.muted, fontSize = 13.sp)
        }
        IconButton(onClick = { vm.classes.deleteEvent(e.id); vm.undoable("Fecha borrada") { vm.classes.putEvent(e) } }) { Icon(Icons.Rounded.Delete, "Borrar", tint = c.muted, modifier = Modifier.size(18.dp)) }
    }
}
