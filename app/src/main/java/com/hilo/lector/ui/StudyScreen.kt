@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.hilo.lector.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.runtime.LaunchedEffect
import com.hilo.lector.data.Ficha
import com.hilo.lector.text.KeyWords
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hilo.lector.HiloViewModel
import com.hilo.lector.data.Book
import com.hilo.lector.data.Grade
import com.hilo.lector.data.STUDY_LANGS
import com.hilo.lector.data.Srs
import com.hilo.lector.data.StudyCard
import com.hilo.lector.data.today
import com.hilo.lector.text.Analysis
import com.hilo.lector.text.Question
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.produceState
import kotlinx.coroutines.CancellationException
import com.hilo.lector.pdf.AnnotStore
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/** Sesiones cortas: mejor 15 tarjetas hoy que 100 que no terminas. */
private const val SESSION = 15

/** Secciones que necesitan el análisis del texto. */
private val ANALYSIS_TABS = setOf("ruta", "guia", "palabras", "glosario", "mapa", "ley", "plazos", "autores", "tiempo")

private fun tabsFor(a: Analysis?): List<Pair<String, String>> = buildList {
    add("ruta" to "Ruta"); add("guia" to "Guía"); add("palabras" to "Palabras"); add("glosario" to "Glosario"); add("mapa" to "Mapa")
    if (a?.law?.isLaw == true) add("ley" to "Ley")
    if (a?.law?.deadlines?.isNotEmpty() == true) add("plazos" to "Plazos")
    if (a?.authors?.isNotEmpty() == true) add("autores" to "Autores")
    if ((a?.timeline?.size ?: 0) >= 2) add("tiempo" to "Línea de tiempo")
    add("fichas" to "Fichas"); add("citas" to "Citas")
    add("tarjetas" to "Tarjetas"); add("resaltados" to "Resaltados"); add("apuntes" to "Apuntes")
}

private fun kindLabel(k: String) = when (k) {
    "vocab" -> "Vocabulario"
    "cloze" -> "Completar"
    else -> "Pregunta"
}

/** Pedido para estudiar un fragmento que se tocó en el lector. */
data class StudyRequest(
    val bookId: String,
    val sentence: String,
    val wordIndex: Int,
    val flow: Boolean,
    val pos: Int,
    val source: String,
    val onHighlight: ((kind: String) -> Unit)?,
)

private val SENTENCE_END = Regex("[.!?…;:](?=[\\s»”\"')\\]]|$)")

/** Rango de la oración que contiene la posición [offset] dentro de [text]. */
fun sentenceAround(text: String, offset: Int): Pair<Int, Int> {
    if (text.isEmpty()) return 0 to 0
    val o = offset.coerceIn(0, text.length - 1)
    var start = 0
    var end = text.length
    for (m in SENTENCE_END.findAll(text)) {
        val e = m.range.last + 1
        if (e <= o) start = e else { end = e; break }
    }
    while (start < end && text[start].isWhitespace()) start++
    // Oraciones muy largas (leyes): se recortan a unas 45 palabras alrededor del toque
    val words = Regex("\\S+").findAll(text.substring(start, end)).toList()
    if (words.size > 45) {
        val rel = o - start
        val k = words.indexOfFirst { it.range.last >= rel }.coerceAtLeast(0)
        val a = max(0, k - 20)
        val b = minOf(words.size - 1, a + 44)
        return (start + words[a].range.first) to (start + words[b].range.last + 1)
    }
    return start to end
}

/** Índice de la palabra (separada por espacios) que contiene la posición [rel] dentro de [s]. */
fun wordIndexAt(s: String, rel: Int): Int {
    val ws = Regex("\\S+").findAll(s).toList()
    if (ws.isEmpty()) return -1
    return ws.indexOfFirst { it.range.last >= rel }.let { if (it < 0) ws.size - 1 else it }
}

private fun core(w: String) = w.trim { !it.isLetterOrDigit() }

/**
 * Busca una palabra o concepto fuera de Hilo: diccionario, Wikipedia o traductor.
 * Usa el idioma del documento (español si no elegiste otro).
 */
fun lookup(ctx: Context, term: String, where: String, lang: String) {
    val q = term.trim().trim { !it.isLetterOrDigit() && it != ' ' }
    if (q.isEmpty()) return
    val l = lang.ifBlank { "es" }
    val enc = Uri.encode(q)
    val url = when (where) {
        "dic" -> when (l) {
            "es" -> "https://dle.rae.es/$enc"
            "en" -> "https://dictionary.cambridge.org/dictionary/english/$enc"
            else -> "https://$l.wiktionary.org/wiki/${Uri.encode(q.lowercase())}"
        }
        "wiki" -> "https://$l.wikipedia.org/w/index.php?search=$enc"
        "jur" -> "https://dpej.rae.es/lema/${Uri.encode(q.lowercase())}"
        else -> "https://translate.google.com/?sl=${if (l == "es") "auto" else l}&tl=${if (l == "es") "en" else "es"}&text=$enc&op=translate"
    }
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Botones para buscar un término en el diccionario, Wikipedia o el traductor. */
@Composable
fun LookupRow(term: String, lang: String) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    if (term.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Buscar «${term.take(40)}»", color = c.muted, fontSize = 12.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { lookup(ctx, term, "dic", lang) }) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Text("  Diccionario", color = c.ink)
            }
            OutlinedButton(onClick = { lookup(ctx, term, "wiki", lang) }) {
                Icon(Icons.Rounded.Public, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Text("  Wikipedia", color = c.ink)
            }
            if (lang.isBlank() || lang == "es") {
                OutlinedButton(onClick = { lookup(ctx, term, "jur", lang) }) {
                    Icon(Icons.Rounded.Gavel, null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Text("  Jurídico", color = c.ink)
                }
            }
            OutlinedButton(onClick = { lookup(ctx, term, "tr", lang) }) {
                Icon(Icons.Rounded.Translate, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Text("  Traducir", color = c.ink)
            }
        }
    }
}

internal fun shareText(ctx: Context, title: String, text: String) {
    runCatching {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, title).putExtra(Intent.EXTRA_TEXT, text)
        ctx.startActivity(Intent.createChooser(i, "Compartir"))
    }
}

// =====================================================================
// Pantalla de estudio
// =====================================================================

@Composable
fun StudyScreen(vm: HiloViewModel, bookId: String?) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val books by vm.repo.books.collectAsStateWithLifecycle()
    val book = bookId?.let { id -> books.firstOrNull { it.id == id } }
    var review by remember { mutableStateOf<List<StudyCard>?>(null) }
    var deck by rememberSaveable { mutableStateOf<String?>(bookId) }
    var vocabOnly by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf("ruta") }
    var practice by remember { mutableStateOf<String?>(null) }
    var moreTabs by remember { mutableStateOf(false) }
    var cross by rememberSaveable { mutableStateOf(false) }
    var ficha by remember { mutableStateOf<Ficha?>(null) }
    var editing by remember { mutableStateOf<StudyCard?>(null) }
    var quiz by remember { mutableStateOf<Pair<String, List<Question>>?>(null) }
    val study = vm.study
    val speaker = remember { Speaker(ctx) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }

    BackHandler {
        when {
            review != null -> review = null
            quiz != null -> quiz = null
            cross -> cross = false
            deck != bookId || vocabOnly -> { deck = bookId; vocabOnly = false }
            else -> vm.closeStudy()
        }
    }

    val current = deck?.let { id -> books.firstOrNull { it.id == id } }

    // Análisis del documento para la guía, las palabras, el glosario y el mapa
    var anProg by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var anError by remember(current?.id) { mutableStateOf(false) }
    val removedNow = current?.let { study.removedFor(it.id) }
    // Al quitar una palabra se recalcula en segundo plano y se sigue mostrando el análisis anterior
    var analysis by remember(current?.id) { mutableStateOf<Analysis?>(null) }
    LaunchedEffect(current?.id, removedNow) {
        val b = current ?: return@LaunchedEffect
        try {
            val a = vm.analysis(b) { p, n -> anProg = p to n }
            if (a.totalWords == 0) anError = true else analysis = a
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            anError = true
        } catch (e: OutOfMemoryError) {
            anError = true
            vm.toast = "El documento es demasiado grande para analizarlo entero en este celular."
        }
    }
    val due = study.due(deck).filter { !vocabOnly || it.kind == "vocab" }

    fun startReview(n: Int) {
        // Primero las frágiles; el resto mezclado por documento y tipo (repaso intercalado)
        val list = mixQueue(due).take(n)
        if (list.isEmpty()) vm.toast = "No hay tarjetas pendientes. ¡Estás al día!" else review = list
    }

    Box(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 760.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = {
                    if (cross) cross = false
                    else if (deck != bookId || vocabOnly) { deck = bookId; vocabOnly = false } else vm.closeStudy()
                }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = c.ink) }
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            vocabOnly -> "Vocabulario de idiomas"
                            cross && current == null -> "Mapa entre documentos"
                            current != null -> current.title
                            else -> "Estudio"
                        },
                        style = MaterialTheme.typography.titleLarge, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (current != null && !vocabOnly) Text("Sala de estudio", color = c.muted, fontSize = 13.sp)
                }
                if (current != null) TextButton(onClick = { vm.open(current) }) { Text("Abrir") }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (cross && current == null) {
                    item { CrossMapView(vm) { id -> cross = false; deck = id; tab = "ruta" } }
                    return@LazyColumn
                }
                item { TodayCard(due.size, study.streak, study.todayCount, onStart = { startReview(SESSION) }, onQuick = { startReview(5) }) }
                if (current != null && !vocabOnly) item { ExamCard(vm, current, analysis) }

                if (current == null && !vocabOnly) {
                    // Todas las barajas: una por documento, más la de idiomas
                    val withCards = books.filter { b -> study.cards.any { it.bookId == b.id } }
                    val vocab = study.cards.filter { it.kind == "vocab" }.sortedBy { it.due }
                    // Mazos importados de Anki
                    val decks = study.cards.map { it.bookId }.filter { it.startsWith("mazo:") }.distinct().sorted()
                    if (decks.isNotEmpty()) items(decks, key = { it }) { d ->
                        val all = study.cards.filter { it.bookId == d }
                        DeckRow("Mazo · " + d.removePrefix("mazo:"), all.count { it.due <= today() }, all.size) {
                            val l = mixQueue(all.filter { it.due <= today() }).take(SESSION)
                            if (l.isEmpty()) vm.toast = "Ese mazo está al día." else review = l
                        }
                    }
                    if (withCards.isEmpty()) {
                        item { EmptyStudyHint() }
                    } else {
                        item { Label("Tus documentos") }
                        items(withCards, key = { it.id }) { b ->
                            val all = study.cardsOf(b.id)
                            DeckRow(b.title, all.count { it.due <= today() }, all.size) { deck = b.id; tab = "ruta" }
                        }
                        if (vocab.isNotEmpty()) {
                            item {
                                DeckRow("Vocabulario de idiomas", vocab.count { it.due <= today() }, vocab.size) { vocabOnly = true }
                            }
                        }
                    }
                    item {
                        Text(
                            "Las tarjetas vuelven justo antes de que las olvides: si respondes bien, tardan más en volver; si fallas, vuelven pronto.",
                            color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { practice = "stats" }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Estadísticas", color = c.ink) }
                            OutlinedButton(onClick = { practice = "simulacro" }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Simulacro", color = c.ink) }
                        }
                    }
                    item { HubTools(vm, onCrossMap = { cross = true }, onDeck = { id -> deck = id; tab = "ruta" }) }
                } else if (vocabOnly) {
                    val vocab = study.cards.filter { it.kind == "vocab" }
                    // Mazos importados de Anki
                    val decks = study.cards.map { it.bookId }.filter { it.startsWith("mazo:") }.distinct().sorted()
                    if (decks.isNotEmpty()) items(decks, key = { it }) { d ->
                        val all = study.cards.filter { it.bookId == d }
                        DeckRow("Mazo · " + d.removePrefix("mazo:"), all.count { it.due <= today() }, all.size) {
                            val l = mixQueue(all.filter { it.due <= today() }).take(SESSION)
                            if (l.isEmpty()) vm.toast = "Ese mazo está al día." else review = l
                        }
                    }
                    items(vocab, key = { it.id }) { card -> CardRow(card, speaker) { editing = card } }
                } else if (current != null) {
                    val simple = vm.settings.value.simple && !moreTabs
                    val tabs = tabsFor(analysis).filter { !simple || it.first in setOf("ruta", "tarjetas", "resaltados", "apuntes") }
                    if (analysis != null && tabs.none { it.first == tab }) tab = "ruta"
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (simple) Text(
                                "Más herramientas…", color = c.accent, fontSize = 14.sp,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(c.surface).clickable { moreTabs = true }.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                            tabs.forEach { (k, t) ->
                                val on = k == tab
                                Text(
                                    t, color = if (on) c.accentInk else c.ink, fontSize = 14.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.accent else c.surface)
                                        .clickable { tab = k }.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                    if (tab in ANALYSIS_TABS) {
                        val an = analysis
                        when {
                            anError -> item {
                                Text("No encontré texto para analizar en este documento. Si es un PDF escaneado, activa «Reconocer texto en escaneados» en Ajustes.", color = c.muted)
                            }
                            an == null -> item {
                                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(color = c.accent)
                                    Text(anProg?.let { (p, n) -> "Leyendo el documento… página ${p + 1} de $n" } ?: "Analizando el texto…", color = c.muted)
                                }
                            }
                            tab == "ruta" -> item {
                                RouteTab(vm, current, an, open = { practice = it }, goTab = { tab = it }, review = {
                                    val l = mixQueue(study.due(current.id)).take(SESSION)
                                    if (l.isEmpty()) vm.toast = "No hay tarjetas pendientes. ¡Estás al día!" else review = l
                                })
                            }
                            tab == "guia" -> item { GuideTab(vm, current, an) { t, qs -> quiz = t to qs } }
                            tab == "palabras" -> item { WordsTab(vm, current, an) { editing = it } }
                            tab == "glosario" -> item { GlossaryTab(vm, current, an) }
                            tab == "mapa" -> item { ConceptMapTab(vm, current, an) }
                            tab == "ley" -> {
                                item { OutlinedButton(onClick = { practice = "derecho" }, modifier = Modifier.fillMaxWidth()) { Text("Métodos para Derecho: silogismo, casos, plazos, interpretación…", color = c.ink) } }
                                item { LawTab(vm, current, an) }
                            }
                            tab == "plazos" -> item { DeadlinesTab(vm, current, an) }
                            tab == "autores" -> {
                                item { OutlinedButton(onClick = { practice = "sociologia" }, modifier = Modifier.fillMaxWidth()) { Text("Métodos para Sociología: paradigmas, Mills, diario, ensayo…", color = c.ink) } }
                                item { AuthorsTab(vm, current, an) { ficha = it } }
                            }
                            tab == "tiempo" -> item { TimelineTab(vm, current, an) }
                        }
                    }
                    when (tab) {
                        "fichas" -> item { FichasTab(vm, current, analysis) { ficha = it } }
                        "citas" -> item { CitesTab(vm, current) }
                        "tarjetas" -> {
                            item { NewCardButtons(onNew = { k -> editing = StudyCard(0, current.id, k, "", "", lang = study.langs[current.id].orEmpty(), due = today()) }) }
                            item { LangRow(study.langs[current.id].orEmpty()) { study.setLang(current.id, it) } }
                            item { TextButton(onClick = { exportDeck(vm, ctx, current) }) { Text("Exportar estas tarjetas a Anki") } }
                            val cards = study.cardsOf(current.id).sortedBy { it.due }
                            if (cards.isEmpty()) item { EmptyStudyHint() }
                            items(cards, key = { it.id }) { card -> CardRow(card, speaker) { editing = card } }
                        }
                        "resaltados" -> item { MarksTab(vm, current, onCard = { text, flow, pos, src ->
                            editing = StudyCard(0, current.id, "qa", "", text, source = src, sourcePos = pos, sourceFlow = flow, due = today())
                        }) }
                        "apuntes" -> item { NotesTab(vm, current) }
                    }
                }
            }
        }
    }

    editing?.let { card ->
        CardEditor(
            card = card,
            onDismiss = { editing = null },
            onSave = { nc ->
                if (nc.id == 0L) study.add(nc.copy(id = study.newId(), created = System.currentTimeMillis()))
                else study.update(nc)
                editing = null
                vm.toast = "Tarjeta guardada."
            },
            onDelete = if (card.id != 0L) ({ study.delete(card.id); editing = null; vm.undoable("Tarjeta borrada") { study.add(card) } }) else null,
        )
    }

    quiz?.let { (t, qs) -> current?.let { b -> QuizOverlay(vm, b, t, qs) { quiz = null } } }

    ficha?.let { f -> current?.let { b -> FichaEditor(vm, b, analysis, f) { ficha = null } } }

    practice?.let { p -> PracticeOverlay(vm, current, analysis, p, onReview = { review = it }) { practice = null } }

    review?.let { list ->
        ReviewSession(
            vm = vm, cards = list, speaker = speaker,
            remaining = { study.due(deck).count { !vocabOnly || it.kind == "vocab" } },
            onMore = { startReview(SESSION) },
            onClose = { review = null },
        )
    }
}

@Composable
private fun TodayCard(due: Int, streak: Int, todayCount: Int, onStart: () -> Unit, onQuick: () -> Unit) {
    val c = LocalHilo.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("PARA HOY", fontSize = 11.sp, letterSpacing = 0.8.sp, color = c.muted)
                Text(
                    if (due == 0) "Al día" else "$due ${if (due == 1) "tarjeta" else "tarjetas"}",
                    fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = c.ink,
                )
                Text(
                    if (due == 0) "Crea tarjetas mientras lees: mantén el dedo sobre una palabra o frase."
                    else max(1, ceil(minOf(due, SESSION) * 0.2).toInt()).let { m -> if (m == 1) "Un minuto para esta ronda." else "Unos $m minutos para esta ronda." },
                    fontSize = 13.sp, color = c.muted,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = if (streak > 0) Color(0xFFF08A24) else c.muted, modifier = Modifier.size(30.dp))
                Text("$streak ${if (streak == 1) "día" else "días"}", fontSize = 12.sp, color = c.ink, fontWeight = FontWeight.Medium)
                Text("hoy: $todayCount", fontSize = 11.sp, color = c.muted)
            }
        }
        if (due > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStart, modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
                ) { Text("Repasar ${minOf(due, SESSION)}", fontSize = 16.sp) }
                if (due > 5) {
                    OutlinedButton(onClick = onQuick, modifier = Modifier.height(52.dp)) { Text("Solo 5", color = c.ink) }
                }
            }
        }
    }
}

@Composable
private fun EmptyStudyHint() {
    val c = LocalHilo.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Cómo crear tarjetas", fontWeight = FontWeight.Bold, color = c.ink)
        listOf(
            "Mientras lees, mantén el dedo sobre una palabra o frase.",
            "Toca las palabras clave y elige Completar, Vocabulario o Pregunta.",
            "También puedes resaltar la oración para tenerla en tus resaltados.",
            "Vuelve aquí cada día: una ronda corta es suficiente.",
        ).forEachIndexed { i, t -> Text("${i + 1}. $t", color = c.ink, fontSize = 14.sp) }
    }
}

@Composable
private fun DeckRow(title: String, due: Int, total: Int, onClick: () -> Unit) {
    val c = LocalHilo.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.School, null, tint = c.accent)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = c.ink, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("$total ${if (total == 1) "tarjeta" else "tarjetas"}", color = c.muted, fontSize = 13.sp)
        }
        if (due > 0) {
            Text(
                "$due hoy", color = c.accentInk, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(c.accent).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        } else {
            Text("al día", color = c.muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun NewCardButtons(onNew: (String) -> Unit) {
    val c = LocalHilo.current
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("qa" to "+ Pregunta", "vocab" to "+ Vocabulario", "cloze" to "+ Completar").forEach { (k, l) ->
            OutlinedButton(onClick = { onNew(k) }) { Text(l, color = c.ink) }
        }
    }
}

@Composable
private fun LangRow(lang: String, onLang: (String) -> Unit) {
    val c = LocalHilo.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Label("Idioma del documento (para vocabulario y voz)")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            STUDY_LANGS.forEach { (code, name) ->
                val on = code == lang
                Text(
                    name, fontSize = 13.sp, color = if (on) c.accentInk else c.ink,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.accent else c.surface)
                        .clickable { onLang(code) }.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

private fun dueLabel(c: StudyCard): String {
    val d = c.due - today()
    return when {
        d <= 0 -> "hoy"
        d == 1L -> "mañana"
        d < 30 -> "en $d días"
        else -> "en ${(d / 30f).roundToInt()} meses"
    }
}

@Composable
private fun CardRow(card: StudyCard, speaker: Speaker, onClick: () -> Unit) {
    val c = LocalHilo.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("${kindLabel(card.kind)} · ${dueLabel(card)}", fontSize = 11.sp, color = c.muted)
            Text(card.front, color = c.ink, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (card.back.isNotBlank()) Text(card.back, color = c.accent, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (card.kind == "vocab" && card.lang.isNotBlank()) {
            IconButton(onClick = { speaker.say(card.front, card.lang) }) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Escuchar", tint = c.ink) }
        }
    }
}

/** Resaltados del documento (texto y PDF), cada uno se puede volver tarjeta. */
@Composable
private fun MarksTab(vm: HiloViewModel, book: Book, onCard: (text: String, flow: Boolean, pos: Int, src: String) -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val annots = remember(book.id) { if (book.type == "pdf") AnnotStore(ctx.applicationContext, book.id) else null }
    data class Item(val text: String, val flow: Boolean, val pos: Int, val src: String, val color: Color, val delete: (() -> Unit)?)
    val items = buildList {
        annots?.strokes?.filter { it.highlight && it.text.isNotBlank() }?.sortedBy { it.page }?.let { list ->
            // Une los resaltados seguidos de la misma página en un solo fragmento
            var cur: Item? = null
            for (s in list) {
                val prev = cur
                cur = if (prev != null && prev.pos == s.page && !prev.flow) prev.copy(text = prev.text + " " + s.text)
                else { if (prev != null) add(prev); Item(s.text, false, s.page, "pág. ${s.page + 1}", Color(s.color), null) }
            }
            cur?.let { add(it) }
        }
        vm.study.marksOf(book.id).sortedBy { it.para }.forEach { m ->
            add(Item(m.text, true, m.para, "párrafo ${m.para + 1}", Color(m.color), { vm.study.deleteMark(m.id); vm.undoable("Resaltado borrado") { vm.study.addMark(m) } }))
        }
        annots?.notes?.sortedBy { it.page }?.forEach { n ->
            add(Item("Nota: " + n.text, false, n.page, "pág. ${n.page + 1}", Color(0xFFFFC94D), null))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (items.isEmpty()) {
            Text(
                "Todavía no hay resaltados. En el PDF usa el pincel → Resaltar; en el modo Texto mantén el dedo sobre una oración y toca Resaltar.",
                color = c.muted, fontSize = 14.sp,
            )
        } else {
            OutlinedButton(onClick = {
                val note = vm.study.notes[book.id].orEmpty()
                val txt = buildString {
                    append(book.title).append("\n\n")
                    items.forEach { append("• ").append(it.text).append(" (").append(it.src).append(")\n") }
                    if (note.isNotBlank()) append("\nApuntes:\n").append(note)
                }
                shareText(ctx, "Resumen: ${book.title}", txt)
            }) { Text("Compartir resumen", color = c.ink) }
        }
        items.forEach { it ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(12.dp),
            ) {
                Box(Modifier.width(4.dp).heightIn(min = 40.dp).clip(RoundedCornerShape(2.dp)).background(it.color))
                Column(Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(it.text, color = c.ink, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(it.src, color = c.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onCard(it.text.removePrefix("Nota: "), it.flow, it.pos, it.src) }) { Text("Tarjeta") }
                        TextButton(onClick = { vm.openAt(book.id, it.flow, it.pos) }) { Text("Ir") }
                        it.delete?.let { del -> TextButton(onClick = del) { Text("Quitar", color = c.lines[0]) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesTab(vm: HiloViewModel, book: Book) {
    val c = LocalHilo.current
    var text by remember(book.id) { mutableStateOf(vm.study.notes[book.id].orEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tus apuntes de este documento. Se guardan solos.", color = c.muted, fontSize = 13.sp)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; vm.study.setNote(book.id, it) },
            placeholder = { Text("Ideas principales, conceptos, preguntas para la clase…") },
            minLines = 10,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// =====================================================================
// Editar tarjeta
// =====================================================================

@Composable
private fun CardEditor(card: StudyCard, onDismiss: () -> Unit, onSave: (StudyCard) -> Unit, onDelete: (() -> Unit)?) {
    val c = LocalHilo.current
    var kind by remember { mutableStateOf(card.kind) }
    var front by remember { mutableStateOf(if (card.kind == "cloze") card.context.ifBlank { card.front } else card.front) }
    var back by remember { mutableStateOf(card.back) }
    var ctxText by remember { mutableStateOf(card.context) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(if (card.id == 0L) "Nueva tarjeta" else "Editar tarjeta") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceRow(listOf("Pregunta", "Vocabulario", "Completar"), listOf("qa", "vocab", "cloze").indexOf(kind)) { i ->
                    kind = listOf("qa", "vocab", "cloze")[i]
                }
                when (kind) {
                    "vocab" -> {
                        OutlinedTextField(front, { front = it }, label = { Text("Palabra o expresión") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(back, { back = it }, label = { Text("Significado o traducción") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(ctxText, { ctxText = it }, label = { Text("Oración de ejemplo (opcional)") }, modifier = Modifier.fillMaxWidth())
                    }
                    "cloze" -> {
                        OutlinedTextField(front, { front = it }, label = { Text("Oración completa") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(back, { back = it }, label = { Text("Palabras que se ocultan") }, modifier = Modifier.fillMaxWidth())
                        Text("Al repasar, esas palabras aparecen como [ … ] y tienes que recordarlas.", fontSize = 12.sp, color = c.muted)
                    }
                    else -> {
                        OutlinedTextField(front, { front = it }, label = { Text("Pregunta") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(back, { back = it }, label = { Text("Respuesta") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (card.source.isNotBlank()) Text("De: ${card.source}", fontSize = 12.sp, color = c.muted)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (front.isBlank() || back.isBlank()) return@TextButton
                val nc = when (kind) {
                    "cloze" -> card.copy(kind = kind, context = front.trim(), front = clozeFront(front.trim(), back.trim()), back = back.trim())
                    "vocab" -> card.copy(kind = kind, front = front.trim(), back = back.trim(), context = ctxText.trim())
                    else -> card.copy(kind = kind, front = front.trim(), back = back.trim())
                }
                onSave(nc)
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Borrar", color = c.lines[0]) }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

/** Oculta las palabras de la respuesta dentro de la oración. */
fun clozeFront(sentence: String, answer: String): String {
    var s = sentence
    answer.split(Regex("\\s+")).map { core(it) }.filter { it.length > 1 }.forEach { w ->
        s = s.replace(Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(w) + "(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE), "[ … ]")
    }
    return if (s == sentence) "$sentence\n\n(¿Qué falta? [ … ])" else s
}

// =====================================================================
// Repaso
// =====================================================================

@Composable
private fun ReviewSession(
    vm: HiloViewModel,
    cards: List<StudyCard>,
    speaker: Speaker,
    remaining: () -> Int,
    onMore: () -> Unit,
    onClose: () -> Unit,
) {
    val c = LocalHilo.current
    val books by vm.repo.books.collectAsStateWithLifecycle()
    var queue by remember(cards) { mutableStateOf(cards) }
    var idx by remember(cards) { mutableIntStateOf(0) }
    var showBack by remember(cards) { mutableStateOf(false) }
    val again = remember(cards) { HashMap<Long, Int>() }
    val start = remember(cards) { mutableLongStateOf(System.currentTimeMillis()) }
    var done by remember(cards) { mutableIntStateOf(0) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var oral by rememberSaveable { mutableStateOf(false) }
    var spoken by remember(cards) { mutableStateOf("") }
    // Calibración: antes de ver la respuesta dices si la sabes; Hilo compara con tu acierto
    var conf by remember(cards) { mutableIntStateOf(0) }
    var overconf by remember(cards) { mutableIntStateOf(0) }
    var streakOk by remember(cards) { mutableIntStateOf(0) }
    val cur = queue.getOrNull(idx)
    val oralLang = cur?.lang?.ifBlank { null } ?: cur?.let { vm.study.langs[it.bookId] }.orEmpty()
    val dictate = rememberDictation(vm, oralLang) { spoken = it; showBack = true }
    LaunchedEffect(cur?.id, idx, oral) {
        spoken = ""
        if (oral && cur != null) speaker.say(cur.front.replace("[ … ]", "tal cosa").replace("\n", ". "), oralLang.ifBlank { "es" })
    }

    Box(
        Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing)
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Terminar", tint = c.ink) }
                LinearProgressIndicator(
                    progress = { if (queue.isEmpty()) 1f else idx.toFloat() / queue.size },
                    color = c.accent, trackColor = c.chip,
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                )
                Text("  ${minOf(idx + 1, queue.size)} / ${queue.size}", color = c.muted, fontSize = 14.sp)
                IconButton(onClick = { oral = !oral; if (!oral) speaker.stop(); vm.toast = if (oral) "Examen oral: escucha la pregunta y responde hablando." else "Examen oral apagado." }) {
                    Icon(if (oral) Icons.Rounded.Mic else Icons.Rounded.MicOff, "Examen oral", tint = if (oral) c.accent else c.muted)
                }
            }

            if (idx >= queue.size) {
                // Fin de la ronda
                val mins = max(1, ((System.currentTimeMillis() - start.longValue) / 60000).toInt())
                val left = remaining()
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFF08A24), modifier = Modifier.size(56.dp))
                    Text("¡Ronda terminada!", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = c.ink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Repasaste $done ${if (done == 1) "tarjeta" else "tarjetas"} en $mins min.\nRacha: ${vm.study.streak} ${if (vm.study.streak == 1) "día" else "días"}.",
                        color = c.muted, textAlign = TextAlign.Center, fontSize = 16.sp,
                    )
                    if (overconf > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ojo: en $overconf ${if (overconf == 1) "tarjeta dijiste" else "tarjetas dijiste"} «Lo sé» y fallaste. Esas vuelven pronto.",
                            color = c.lines[2], textAlign = TextAlign.Center, fontSize = 14.sp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    SessionClose(vm, cards.firstOrNull()?.bookId)
                    Spacer(Modifier.height(24.dp))
                    if (left > 0) {
                        Button(
                            onClick = onMore,
                            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) { Text("Otra ronda (quedan $left)") }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Terminar por hoy", color = c.ink) }
                }
                return@Column
            }

            val card = queue[idx]
            val src = books.firstOrNull { it.id == card.bookId }
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp).clip(RoundedCornerShape(22.dp)).background(c.surface)
                    .clickable { showBack = true }.padding(22.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(kindLabel(card.kind).uppercase(), fontSize = 11.sp, letterSpacing = 0.8.sp, color = c.muted, modifier = Modifier.weight(1f))
                    if (card.kind == "vocab" && card.lang.isNotBlank()) {
                        IconButton(onClick = { speaker.say(card.front, card.lang) }) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Escuchar", tint = c.accent) }
                    }
                }
                Text(card.front, color = c.ink, fontSize = if (card.front.length < 40) 26.sp else 19.sp, lineHeight = if (card.front.length < 40) 32.sp else 27.sp)
                if (showBack) {
                    if (spoken.isNotBlank()) {
                        val keys = remember(card.id) { KeyWords.of(card.back, emptyList(), 8) }
                        val (hit, miss) = KeyWords.coverage(spoken, keys)
                        val pct = if (keys.isEmpty()) 0 else hit.size * 100 / keys.size
                        Text("Dijiste: «$spoken»", color = c.ink, fontSize = 15.sp)
                        Text(
                            "Coincide en $pct %" + if (miss.isNotEmpty()) " · te faltó: " + miss.take(5).joinToString(", ") else "",
                            color = if (pct >= 60) c.lines[1] else c.lines[2], fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                    Text(card.back, color = c.accent, fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp)
                    if (card.kind == "cloze" && card.context.isNotBlank()) {
                        Text(card.context, color = c.ink, fontSize = 16.sp, lineHeight = 23.sp)
                    }
                    if (card.kind == "vocab" && card.context.isNotBlank()) {
                        val ans = core(card.front)
                        Text(
                            buildAnnotatedString {
                                val i = card.context.indexOf(ans, ignoreCase = true)
                                if (i >= 0 && ans.isNotEmpty()) {
                                    append(card.context.substring(0, i))
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.accent)) { append(card.context.substring(i, i + ans.length)) }
                                    append(card.context.substring(i + ans.length))
                                } else append(card.context)
                            },
                            color = c.ink, fontSize = 15.sp, lineHeight = 22.sp,
                        )
                    }
                    if (src != null && card.sourcePos >= 0) {
                        TextButton(onClick = { vm.openAt(card.bookId, card.sourceFlow, card.sourcePos) }) {
                            Text("Ver en «${src.title.take(30)}» · ${card.source}", fontSize = 13.sp)
                        }
                    } else if (src != null) {
                        Text("De: ${src.title}", color = c.muted, fontSize = 12.sp)
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text("Piensa la respuesta y toca la tarjeta.", color = c.muted, fontSize = 13.sp)
                }
            }

            if (!showBack) {
                if (oral) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = dictate,
                            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
                            modifier = Modifier.weight(1f).height(60.dp),
                        ) { Icon(Icons.Rounded.Mic, null); Text("  Responder hablando", fontSize = 16.sp) }
                        OutlinedButton(onClick = { showBack = true }, modifier = Modifier.height(60.dp)) { Text("Ver", color = c.ink) }
                    }
                } else Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("¿La sabes? Responde en tu cabeza y elige:", color = c.muted, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Triple(1, "Lo sé", c.accent), Triple(2, "Dudo", c.lines[2]), Triple(3, "No lo sé", c.muted)).forEach { (k, label, col) ->
                            Box(
                                Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(14.dp)).background(col.copy(alpha = 0.16f))
                                    .border(2.dp, col, RoundedCornerShape(14.dp)).clickable { conf = k; showBack = true },
                                contentAlignment = Alignment.Center,
                            ) { Text(label, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple(Grade.AGAIN, "Otra vez", c.lines[0]),
                        Triple(Grade.HARD, "Difícil", c.lines[2]),
                        Triple(Grade.GOOD, "Bien", c.accent),
                        Triple(Grade.EASY, "Fácil", c.lines[3]),
                    ).forEach { (g, label, col) ->
                        Column(
                            Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(14.dp)).background(col)
                                .clickable {
                                    if (vm.settings.value.haptics) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    vm.study.grade(card, g, conf)
                                    if (conf == 1 && g == Grade.AGAIN) overconf++
                                    conf = 0
                                    done++
                                    // Dificultad deseable: si aciertas 10 seguidas, adelanta las más difíciles
                                    streakOk = if (g >= Grade.GOOD) streakOk + 1 else 0
                                    if (streakOk == 10 && idx + 1 < queue.size) {
                                        val rest = queue.drop(idx + 1).sortedByDescending { it.diff }
                                        queue = queue.take(idx + 1) + rest
                                        vm.toast = "¡10 seguidas! Ahora vienen las más difíciles."
                                        streakOk = 0
                                    }
                                    val n = again[card.id] ?: 0
                                    if (g == Grade.AGAIN && n < 2) {
                                        again[card.id] = n + 1
                                        queue = queue + Srs.schedule(card, g)
                                    }
                                    idx++
                                    showBack = false
                                },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(Srs.preview(card, g), color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// Estudiar un fragmento desde el lector
// =====================================================================

/** Hoja que aparece al mantener el dedo sobre el texto: elegir palabras y crear tarjetas. */
@Composable
fun StudySheet(vm: HiloViewModel, req: StudyRequest, onDismiss: () -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val words = remember(req) { req.sentence.split(Regex("\\s+")).filter { it.isNotEmpty() } }
    var sel by remember(req) { mutableStateOf(if (req.wordIndex in words.indices) setOf(req.wordIndex) else emptySet()) }
    var mode by remember(req) { mutableStateOf("") }   // "", "vocab", "qa"
    val answer = words.indices.filter { it in sel }.joinToString(" ") { core(words[it]) }
    var meaning by remember(req) { mutableStateOf("") }
    var question by remember(req) { mutableStateOf("") }
    var reply by remember(req) { mutableStateOf(req.sentence) }
    val lang = vm.study.langs[req.bookId].orEmpty()

    fun create(card: StudyCard) {
        vm.study.add(card.copy(id = vm.study.newId(), created = System.currentTimeMillis(), due = today()))
        vm.toast = "Tarjeta creada. La repasas en Estudio."
        onDismiss()
    }
    fun base(kind: String) = StudyCard(
        0, req.bookId, kind, "", "", lang = lang, source = req.source, sourcePos = req.pos, sourceFlow = req.flow,
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(
            Modifier.padding(horizontal = 18.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Estudiar este fragmento", style = MaterialTheme.typography.titleLarge, color = c.ink)
            Text("Toca las palabras clave.", color = c.muted, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                words.forEachIndexed { i, w ->
                    val on = i in sel
                    Text(
                        w, fontSize = 16.sp, color = if (on) c.accentInk else c.ink,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (on) c.accent else c.bg)
                            .border(1.dp, if (on) c.accent else c.line, RoundedCornerShape(8.dp))
                            .clickable { sel = if (on) sel - i else sel + i }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
            }

            LookupRow(term = answer.ifBlank { words.getOrNull(req.wordIndex)?.let { core(it) }.orEmpty() }, lang = lang)
            ArticleRefsRow(vm, req.bookId, req.sentence)
            if (req.onHighlight != null) {
                // Subrayado con código de colores (lectura activa)
                Text("Subrayar como", color = c.muted, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.hilo.lector.data.MARK_KINDS.forEach { (k, label, col) ->
                        Text(
                            label, fontSize = 13.sp, color = Color(0xFF1B1B1B),
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(col)).clickable {
                                req.onHighlight.invoke(k)
                                if (k == "duda") vm.toast = "Marcada como duda: la verás en tu ruta de estudio."
                                onDismiss()
                            }.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            when (mode) {
                "vocab" -> {
                    Text("«$answer»", fontWeight = FontWeight.Bold, color = c.ink, fontSize = 17.sp)
                    OutlinedTextField(meaning, { meaning = it }, label = { Text("Significado o traducción") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { if (meaning.isNotBlank()) create(base("vocab").copy(front = answer, back = meaning.trim(), context = req.sentence)) },
                        enabled = meaning.isNotBlank(), modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
                    ) { Text("Guardar tarjeta de vocabulario") }
                }
                "qa" -> {
                    OutlinedTextField(question, { question = it }, label = { Text("Pregunta") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(reply, { reply = it }, label = { Text("Respuesta") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { if (question.isNotBlank() && reply.isNotBlank()) create(base("qa").copy(front = question.trim(), back = reply.trim())) },
                        enabled = question.isNotBlank() && reply.isNotBlank(), modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
                    ) { Text("Guardar pregunta") }
                }
                else -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (req.onHighlight != null) {
                            OutlinedButton(onClick = { req.onHighlight.invoke(""); onDismiss() }) { Text("Resaltar", color = c.ink) }
                        }
                        Button(
                            onClick = { create(base("cloze").copy(front = clozeFront(req.sentence, answer), back = answer, context = req.sentence)) },
                            enabled = sel.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
                        ) { Text("Completar") }
                        OutlinedButton(onClick = { mode = "vocab" }, enabled = sel.isNotEmpty()) { Text("Vocabulario", color = c.ink) }
                        OutlinedButton(onClick = {
                            question = if (sel.isNotEmpty()) "¿Qué es o qué significa «$answer»?" else ""
                            mode = "qa"
                        }) { Text("Pregunta", color = c.ink) }
                        OutlinedButton(onClick = {
                            // Cita: la oración completa, o las palabras elegidas si marcaste varias seguidas
                            val idx = sel.sorted()
                            val contiguous = idx.size >= 4 && idx.last() - idx.first() == idx.size - 1
                            saveQuote(vm, req, if (contiguous) idx.joinToString(" ") { words[it] } else req.sentence)
                            onDismiss()
                        }) { Text("Citar", color = c.ink) }
                        TextButton(onClick = {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Hilo", if (sel.isEmpty()) req.sentence else answer))
                            vm.toast = "Copiado."
                            onDismiss()
                        }) { Text("Copiar") }
                    }
                    Text(
                        "Completar: oculta las palabras elegidas. Vocabulario: para idiomas, con su significado. Pregunta: escribe tú la pregunta. Citar: la guarda con su página para tus trabajos (formato APA).",
                        color = c.muted, fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
