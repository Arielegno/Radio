@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.hilo.lector.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hilo.lector.HiloViewModel
import com.hilo.lector.data.AnkiExport
import com.hilo.lector.data.Book
import com.hilo.lector.data.BookRef
import com.hilo.lector.data.FICHA_KINDS
import com.hilo.lector.data.Ficha
import com.hilo.lector.data.Quote
import com.hilo.lector.data.StudyCard
import com.hilo.lector.data.fichaFields
import com.hilo.lector.data.today
import com.hilo.lector.pdf.AnnotStore
import com.hilo.lector.text.Analysis
import com.hilo.lector.text.Apa
import com.hilo.lector.text.AuthorInfo
import com.hilo.lector.text.Edge
import com.hilo.lector.text.KeyWords
import com.hilo.lector.text.Law
import com.hilo.lector.text.LawArticle
import com.hilo.lector.text.LawHeading
import com.hilo.lector.text.LawIndex
import com.hilo.lector.text.Social
import com.hilo.lector.text.Term
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

// =====================================================================
// Piezas comunes
// =====================================================================

@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = LocalHilo.current
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
internal fun Pill(text: String, on: Boolean = false, onClick: () -> Unit) {
    val c = LocalHilo.current
    Text(
        text, fontSize = 13.sp, color = if (on) c.accentInk else c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.accent else c.band)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

internal fun copyText(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("Hilo", text))
}

/** Dictado: abre el reconocimiento de voz del celular y devuelve lo que dijiste. */
@Composable
internal fun rememberDictation(vm: HiloViewModel, lang: String, onText: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onText)
        }
    }
    return {
        val tag = when (lang.ifBlank { "es" }) { "es" -> "es-419"; "en" -> "en-US"; "pt" -> "pt-BR"; else -> lang }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora")
        try { launcher.launch(i) } catch (_: ActivityNotFoundException) {
            vm.toast = "Este celular no tiene reconocimiento de voz. Instala o activa «Google» o «Voz de Google»."
        }
    }
}

/** Fragmentos resaltados del documento (texto y PDF), para fichas y sugerencias. */
@Composable
internal fun rememberHighlights(vm: HiloViewModel, book: Book): List<String> {
    val ctx = LocalContext.current
    val annots = remember(book.id) { if (book.type == "pdf") AnnotStore(ctx.applicationContext, book.id) else null }
    return buildList {
        vm.study.marksOf(book.id).sortedBy { it.para }.forEach { add(it.text) }
        annots?.strokes?.filter { it.highlight && it.text.isNotBlank() }?.sortedBy { it.page }?.forEach { add(it.text + " (pág. ${it.page + 1})") }
    }
}

// =====================================================================
// Ley o código: índice, artículos enlazados y tarjetas
// =====================================================================

private fun lawCardOf(vm: HiloViewModel, book: Book, art: LawArticle): StudyCard {
    val body = art.text.removePrefix(art.title).trimStart('.', ' ', '-', '–').trim()
    return StudyCard(
        vm.study.newId(), book.id, "qa",
        "¿Qué establece el artículo ${art.num}" + (if (art.title.isNotBlank()) " («${art.title}»)" else "") + "?",
        body.take(1500).ifBlank { art.text.take(1500) },
        source = "art. ${art.num}", sourcePos = art.para, sourceFlow = true, due = today(), created = System.currentTimeMillis(),
    )
}

internal fun lawCard(vm: HiloViewModel, book: Book, art: LawArticle): Boolean {
    val src = "art. ${art.num}"
    if (vm.study.cards.any { it.bookId == book.id && it.source == src }) return false
    vm.study.add(lawCardOf(vm, book, art))
    return true
}

/** Tarjetas de muchos artículos de una vez: revisa duplicados con un conjunto, no tarjeta por tarjeta. */
internal fun lawCards(vm: HiloViewModel, book: Book, arts: List<LawArticle>): Int {
    val have = vm.study.cards.filter { it.bookId == book.id }.map { it.source }.toHashSet()
    val list = arts.filter { have.add("art. ${it.num}") }.map { lawCardOf(vm, book, it) }
    vm.study.addAll(list)
    return list.size
}

@Composable
fun ArticleDialog(vm: HiloViewModel, book: Book, law: LawIndex, start: LawArticle, onDismiss: () -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val history = remember(start) { mutableStateListOf(start) }
    val art = history.last()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Column {
                Text("Artículo ${art.num}", color = c.ink)
                if (art.title.isNotBlank()) Text(art.title, color = c.accent, fontSize = 15.sp)
            }
        },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (art.path.isNotEmpty()) Text(art.path.joinToString(" › "), color = c.muted, fontSize = 12.sp)
                Text(art.text, color = c.ink, fontSize = 15.sp, lineHeight = 21.sp)
                if (art.refs.isNotEmpty()) {
                    Label("Remite a")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        art.refs.forEach { r ->
                            val target = law.find(r)
                            Pill("Art. $r" + if (target == null) " ↗" else "") {
                                if (target != null) history.add(target)
                                else vm.toast = "El artículo $r no está en este documento: puede ser de otra ley."
                            }
                        }
                    }
                }
                val citedBy = remember(art) { law.articles.filter { o -> art.num in o.refs }.take(12) }
                if (citedBy.isNotEmpty()) {
                    Label("Lo mencionan")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        citedBy.forEach { o -> Pill(o.label) { history.add(o) } }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { vm.openAt(book.id, true, art.para); onDismiss() }) { Text("Leer en el libro") }
                    TextButton(onClick = { vm.toast = if (lawCard(vm, book, art)) "Tarjeta creada." else "Ese artículo ya tiene tarjeta." }) { Text("Tarjeta") }
                    TextButton(onClick = { copyText(ctx, "Artículo ${art.num}. ${art.text}"); vm.toast = "Copiado." }) { Text("Copiar") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        dismissButton = if (history.size > 1) ({ TextButton(onClick = { history.removeAt(history.size - 1) }) { Text("‹ Volver") } }) else null,
    )
}

@Composable
fun LawTab(vm: HiloViewModel, book: Book, a: Analysis) {
    val c = LocalHilo.current
    val law = a.law
    var open by remember { mutableStateOf<LawArticle?>(null) }
    var q by remember { mutableStateOf("") }
    var expanded by remember(book.id) { mutableStateOf(setOf<Int>()) }
    var limit by remember(book.id) { mutableStateOf(40) }
    var confirmAll by remember { mutableStateOf<List<LawArticle>?>(null) }

    fun makeCards(list: List<LawArticle>) {
        val n = lawCards(vm, book, list)
        vm.toast = if (n == 0) "Esos artículos ya tenían tarjeta." else "Creé $n ${if (n == 1) "tarjeta" else "tarjetas"}."
    }

    @Composable
    fun ArticleRow(art: LawArticle, indent: Int) {
        Row(
            Modifier.fillMaxWidth().padding(start = (indent * 14).dp).clip(RoundedCornerShape(10.dp)).clickable { open = art }
                .padding(vertical = 7.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(art.label, color = c.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(72.dp))
            Text(art.title.ifBlank { art.text.take(110) }, color = c.ink, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
    }

    @Composable
    fun HeadingNode(i: Int) {
        val h = law.headings[i]
        val endIdx = (i + 1 until law.headings.size).firstOrNull { law.headings[it].level <= h.level } ?: law.headings.size
        val subs = (i + 1 until endIdx).toList()
        val childLevel = subs.minOfOrNull { law.headings[it].level }
        val children = subs.filter { law.headings[it].level == childLevel }
        val nextPara = law.headings.getOrNull(endIdx)?.para ?: Int.MAX_VALUE
        val firstChild = children.firstOrNull()?.let { law.headings[it].para } ?: nextPara
        val own = law.articles.filter { it.para >= h.para && it.para < firstChild }
        val all = law.articles.filter { it.para >= h.para && it.para < nextPara }
        val on = h.para in expanded
        Row(
            Modifier.fillMaxWidth().padding(start = (h.level * 14).dp).clip(RoundedCornerShape(10.dp))
                .background(if (h.level == 0) c.band else Color.Transparent)
                .clickable { expanded = if (on) expanded - h.para else expanded + h.para }.padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(h.label, color = c.ink, fontWeight = if (h.level == 0) FontWeight.Bold else FontWeight.Medium, fontSize = 15.sp)
                if (all.isNotEmpty()) Text(
                    "${all.size} ${if (all.size == 1) "artículo" else "artículos"} · ${all.first().label}" + if (all.size > 1) "–${all.last().num}" else "",
                    color = c.muted, fontSize = 12.sp,
                )
            }
            Icon(if (on) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = c.muted)
        }
        if (on) {
            if (all.isNotEmpty()) TextButton(onClick = { if (all.size > 40) confirmAll = all else makeCards(all) }, modifier = Modifier.padding(start = (h.level * 14 + 6).dp)) {
                Text("Tarjetas de esta parte (${all.size})", fontSize = 13.sp)
            }
            own.forEach { ArticleRow(it, h.level + 1) }
            children.forEach { HeadingNode(it) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Encontré ${law.articles.size} artículos. Toca uno para leerlo, ver a qué artículos remite y crear su tarjeta.",
            color = c.muted, fontSize = 13.sp,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                q, { q = it }, label = { Text("Ir al artículo…") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { law.find(q.trim())?.let { open = it } ?: run { vm.toast = "No encontré el artículo ${q.trim()}." } },
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
            ) { Text("Ver") }
        }
        Panel {
            if (law.headings.isNotEmpty()) {
                val top = law.headings.minOf { it.level }
                val before = law.articles.filter { it.para < law.headings.first().para }
                before.forEach { ArticleRow(it, 0) }
                law.headings.indices.filter { law.headings[it].level == top }.forEach { HeadingNode(it) }
            } else {
                law.articles.take(limit).forEach { ArticleRow(it, 0) }
                if (law.articles.size > limit) TextButton(onClick = { limit += 60 }) { Text("Mostrar más (${law.articles.size - limit})") }
            }
        }
        OutlinedButton(onClick = { if (law.articles.size > 40) confirmAll = law.articles else makeCards(law.articles) }) {
            Text("Tarjetas de todos los artículos (${law.articles.size})", color = c.ink)
        }
    }

    open?.let { art -> ArticleDialog(vm, book, law, art) { open = null } }
    confirmAll?.let { list ->
        AlertDialog(
            onDismissRequest = { confirmAll = null },
            containerColor = c.surface,
            title = { Text("¿${list.size} tarjetas?") },
            text = { Text("Son muchas para empezar. Te conviene crear las de un título o capítulo a la vez y repasar en rondas cortas.", color = c.ink) },
            confirmButton = { TextButton(onClick = { makeCards(list); confirmAll = null }) { Text("Crear igual") } },
            dismissButton = { TextButton(onClick = { confirmAll = null }) { Text("Mejor no") } },
        )
    }
}

/** Plazos del texto en una tabla: cuánto, en qué artículo y la oración completa. */
@Composable
fun DeadlinesTab(vm: HiloViewModel, book: Book, a: Analysis) {
    val c = LocalHilo.current
    var open by remember { mutableStateOf<LawArticle?>(null) }
    val list = a.law.deadlines
    fun card(d: com.hilo.lector.text.Deadline): Boolean {
        val front = d.sentence.replace(d.amount, "[ … ]")
        if (vm.study.cards.any { it.bookId == book.id && it.front == front }) return false
        vm.study.add(
            StudyCard(
                vm.study.newId(), book.id, "cloze", front, d.amount, context = d.sentence,
                source = d.article?.let { "art. $it" } ?: "párrafo ${d.para + 1}", sourcePos = d.para, sourceFlow = true,
                due = today(), created = System.currentTimeMillis(),
            )
        )
        return true
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${list.size} plazos encontrados en el texto. Repásalos como tarjetas para completar: se oculta el plazo.", color = c.muted, fontSize = 13.sp)
        OutlinedButton(onClick = {
            val have = vm.study.cards.filter { it.bookId == book.id }.map { it.front }.toHashSet()
            val add = list.filter { have.add(it.sentence.replace(it.amount, "[ … ]")) }.map { d ->
                StudyCard(vm.study.newId(), book.id, "cloze", d.sentence.replace(d.amount, "[ … ]"), d.amount, context = d.sentence,
                    source = d.article?.let { "art. $it" } ?: "párrafo ${d.para + 1}", sourcePos = d.para, sourceFlow = true,
                    due = today(), created = System.currentTimeMillis())
            }
            vm.study.addAll(add)
            vm.toast = if (add.isEmpty()) "Ya estaban todas." else "Creé ${add.size} tarjetas de plazos."
        }) {
            Text("Pasar todos a tarjetas", color = c.ink)
        }
        list.forEach { d ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.amount, color = c.accent, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    d.article?.let { n -> Pill("Art. $n") { a.law.find(n)?.let { open = it } } }
                }
                Text(d.sentence, color = c.ink, fontSize = 14.sp, lineHeight = 20.sp)
                Row {
                    TextButton(onClick = { vm.openAt(book.id, true, d.para) }) { Text("Ir") }
                    TextButton(onClick = { vm.toast = if (card(d)) "Tarjeta creada." else "Ya tenía tarjeta." }) { Text("Tarjeta") }
                }
            }
        }
    }
    open?.let { art -> ArticleDialog(vm, book, a.law, art) { open = null } }
}

/** En la hoja del lector: artículos que menciona la oración tocada, para abrirlos sin perder el lugar. */
@Composable
fun ArticleRefsRow(vm: HiloViewModel, bookId: String, sentence: String) {
    val refs = remember(sentence) { Law.refsIn(sentence) }
    if (refs.isEmpty()) return
    val book = vm.repo.get(bookId) ?: return
    var want by remember(sentence) { mutableStateOf<String?>(null) }
    val an by produceState<Analysis?>(null, want != null) {
        if (want != null) value = runCatching { vm.analysis(book) { _, _ -> } }.getOrNull()
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Label("Artículos mencionados")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            refs.forEach { r -> Pill("Ver art. $r") { want = r } }
        }
    }
    val w = want ?: return
    val a = an
    if (a == null) {
        AlertDialog(onDismissRequest = { want = null }, confirmButton = {}, text = {
            Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp)); Text("   Buscando el artículo…") }
        })
    } else {
        val art = a.law.find(w)
        if (art == null) LaunchedEffect(w) { want = null; vm.toast = "El artículo $w no está en este documento: puede ser de otra ley." }
        else ArticleDialog(vm, book, a.law, art) { want = null }
    }
}

// =====================================================================
// Autores y cuadro comparativo
// =====================================================================

@Composable
fun AuthorsTab(vm: HiloViewModel, book: Book, a: Analysis, onFicha: (Ficha) -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    var sel by remember(book.id) { mutableStateOf(listOf<String>()) }
    var comparing by remember(book.id) { mutableStateOf(false) }
    val chosen = a.authors.filter { it.key in sel }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Autores que aparecen en el texto, con los conceptos que se tratan junto a ellos. Marca dos o tres para compararlos en un cuadro.",
            color = c.muted, fontSize = 13.sp,
        )
        if (chosen.size >= 2) {
            Button(
                onClick = { comparing = !comparing }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
            ) { Text(if (comparing) "Ocultar cuadro" else "Comparar ${chosen.joinToString(", ") { shortName(it) }}") }
        }
        if (comparing && chosen.size >= 2) CompareView(vm, book, a, chosen)
        a.authors.forEach { au ->
            val on = au.key in sel
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(au.name, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = c.ink)
                        Text(
                            "${au.mentions} ${if (au.mentions == 1) "mención" else "menciones"}" + if (au.years.isNotEmpty()) " · " + au.years.joinToString(", ") else "",
                            color = c.muted, fontSize = 12.sp,
                        )
                    }
                    Pill(if (on) "✓ Comparar" else "Comparar", on) {
                        sel = if (on) sel - au.key else (sel + au.key).takeLast(3)
                        comparing = false
                    }
                }
                if (au.conceptLabels.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        au.conceptLabels.forEach { Text(it, fontSize = 13.sp, color = c.ink, modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, c.line, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)) }
                    }
                }
                au.sentences.take(2).forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.text, color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 4, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = { vm.openAt(book.id, true, s.para) }) { Text("Ir") }
                    }
                }
                FlowRow {
                    TextButton(onClick = { onFicha(authorFicha(vm, book, au)) }) { Text("Ficha de autor") }
                    TextButton(onClick = {
                        vm.feynman = FeynmanReq(book.id, shortName(au), au.sentences.take(3).joinToString(" ") { it.text }, au.conceptLabels.take(4))
                    }) { Text("Explícalo") }
                    TextButton(onClick = { lookup(ctx, au.name, "wiki", "es") }) { Text("Wikipedia") }
                }
            }
        }
    }
}

private fun shortName(a: AuthorInfo) = Social.KNOWN[a.key]?.substringAfterLast(' ') ?: a.name

private fun authorFicha(vm: HiloViewModel, book: Book, au: AuthorInfo): Ficha {
    val names = fichaFields("autor")
    val values = listOf(
        au.name,
        "",
        au.conceptLabels.joinToString(", "),
        au.sentences.take(4).joinToString("\n") { "• " + it.text },
        au.years.joinToString(", "),
        "",
    )
    return Ficha(0, book.id, "autor", au.name, names.zip(values), System.currentTimeMillis())
}

@Composable
private fun CompareView(vm: HiloViewModel, book: Book, a: Analysis, authors: List<AuthorInfo>) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val rows = remember(a, authors) { Social.compare(authors, a.sentences) { a.labelOf(it) } }
    Panel {
        Text("Cuadro comparativo", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = c.ink)
        if (rows.isEmpty()) Text("No encontré conceptos en común.", color = c.muted)
        rows.forEach { r ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
            Text(r.concept.replaceFirstChar { it.uppercase() }, color = c.accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            authors.forEachIndexed { i, au ->
                val s = r.cells[i]
                Row(verticalAlignment = Alignment.Top) {
                    Text(shortName(au), fontWeight = FontWeight.Bold, color = c.lines[(i + 3) % c.lines.size], fontSize = 13.sp, modifier = Modifier.width(86.dp))
                    Text(s?.text ?: "— No lo trata en este texto.", color = if (s == null) c.muted else c.ink, fontSize = 13.sp, lineHeight = 18.sp,
                        modifier = Modifier.weight(1f).clickable(enabled = s != null) { s?.let { vm.openAt(book.id, true, it.para) } })
                }
            }
        }
        TextButton(onClick = {
            val txt = buildString {
                append("Cuadro comparativo: ").append(authors.joinToString(" / ") { it.name }).append("\n\n")
                rows.forEach { r ->
                    append("■ ").append(r.concept).append("\n")
                    authors.forEachIndexed { i, au -> append("  ").append(shortName(au)).append(": ").append(r.cells[i]?.text ?: "—").append("\n") }
                    append("\n")
                }
            }
            shareText(ctx, "Cuadro comparativo", txt)
        }) { Text("Compartir cuadro") }
    }
}

// =====================================================================
// Línea de tiempo
// =====================================================================

@Composable
fun TimelineTab(vm: HiloViewModel, book: Book, a: Analysis) {
    val c = LocalHilo.current
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text("Fechas y siglos del texto, en orden. Toca «Tarjeta» para memorizar una fecha.", color = c.muted, fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp))
        // Contexto: hechos históricos y obras de los autores, en paralelo con las fechas del texto
        var show by remember { mutableIntStateOf(0) }
        ChoiceRow(listOf("Del texto", "Con contexto", "Solo contexto"), show) { show = it }
        Spacer(Modifier.height(10.dp))
        val minY = (a.timeline.minOfOrNull { it.year } ?: 1750) - 30
        val maxY = (a.timeline.maxOfOrNull { it.year } ?: 2030) + 30
        val ctxEvents = com.hilo.lector.text.SocioKb.history.filter { it.year in minY..maxY }.map { com.hilo.lector.text.TimeEvent(it.year, it.year.toString(), it.text, -1, it.place) } +
            com.hilo.lector.text.SocioKb.authors.filter { au -> a.authors.any { it.key == au.key } }.mapNotNull { au ->
                Regex("\\((\\d{4})").find(au.works)?.let { m -> com.hilo.lector.text.TimeEvent(m.groupValues[1].toInt(), m.groupValues[1], au.works.substringBefore(";"), -1, au.name) }
            }
        val events = when (show) { 0 -> a.timeline; 1 -> (a.timeline + ctxEvents).sortedBy { it.year }; else -> ctxEvents.sortedBy { it.year } }
        var prev = ""
        events.forEach { ev ->
            val same = ev.label == prev
            prev = ev.label
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(if (same) "" else ev.label, color = c.accent, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(78.dp).padding(top = 10.dp))
                Box(Modifier.width(18.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.width(2.dp).fillMaxHeight().background(c.line))
                    Box(Modifier.padding(top = 14.dp).size(if (same) 6.dp else 11.dp).clip(CircleShape).background(c.accent))
                }
                Column(Modifier.weight(1f).padding(start = 8.dp, top = 6.dp, bottom = 6.dp)) {
                    Text(ev.text, color = c.ink, fontSize = 14.sp, lineHeight = 19.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ev.author?.let { Text(it, color = c.muted, fontSize = 12.sp, modifier = Modifier.weight(1f)) } ?: Spacer(Modifier.weight(1f))
                        if (ev.para >= 0) TextButton(onClick = { vm.openAt(book.id, true, ev.para) }) { Text("Ir", fontSize = 13.sp) }
                        else Text("contexto", color = c.lines[3], fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                        TextButton(onClick = {
                            val front = ev.text.replace(ev.label, "[ … ]").replace(ev.label.lowercase(), "[ … ]")
                            if (vm.study.cards.any { it.bookId == book.id && it.front == front }) vm.toast = "Ya tenía tarjeta."
                            else {
                                vm.study.add(StudyCard(vm.study.newId(), book.id, "cloze", front, ev.label, context = ev.text,
                                    source = if (ev.para >= 0) "párrafo ${ev.para + 1}" else "contexto", sourcePos = ev.para, sourceFlow = true, due = today(), created = System.currentTimeMillis()))
                                vm.toast = "Tarjeta creada."
                            }
                        }) { Text("Tarjeta", fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

// =====================================================================
// Fichas: de caso, de lectura y de autor
// =====================================================================

internal fun newFicha(vm: HiloViewModel, book: Book, kind: String, a: Analysis?): Ficha {
    val names = fichaFields(kind)
    val ref = vm.study.refs[book.id] ?: BookRef(authors = book.author, title = book.title)
    val values = names.map { n ->
        when {
            kind == "lectura" && n == "Referencia" -> Apa.reference(ref.authors, ref.year, ref.title.ifBlank { book.title }, ref.publisher, ref.url)
            kind == "lectura" && n == "Conceptos clave" -> a?.let { (it.phrases.take(4) + it.terms.take(8)).distinctBy { t -> t.key }.take(10).joinToString(", ") { t -> t.label } }.orEmpty()
            kind == "lectura" && n == "Citas importantes" -> vm.study.quotesOf(book.id).take(5).joinToString("\n") { "• " + Apa.quote(it.text, ref.authors, ref.year, it.loc) }
            else -> ""
        }
    }
    val title = when (kind) { "caso", "irac", "sentencia" -> "Caso: "; "norma" -> "Art. "; "autor", "concepto", "metodo" -> ""; else -> book.title }
    return Ficha(0, book.id, kind, title, names.zip(values), System.currentTimeMillis())
}

@Composable
fun FichasTab(vm: HiloViewModel, book: Book, a: Analysis?, onEdit: (Ficha) -> Unit) {
    val c = LocalHilo.current
    val list = vm.study.fichasOf(book.id)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Plantillas para ordenar lo que lees: casos y sentencias (hechos, problema jurídico, norma, fallo), lecturas (tesis, argumentos, crítica) y autores.",
            color = c.muted, fontSize = 13.sp,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FICHA_KINDS.forEach { (k, l) -> OutlinedButton(onClick = { onEdit(newFicha(vm, book, k, a)) }) { Text("+ " + l.removePrefix("Ficha de "), color = c.ink) } }
        }
        list.forEach { f ->
            Panel(Modifier.clickable { onEdit(f) }) {
                Text(FICHA_KINDS.firstOrNull { it.first == f.kind }?.second.orEmpty().uppercase(), fontSize = 11.sp, letterSpacing = 0.8.sp, color = c.muted)
                Text(f.title.ifBlank { "Sin título" }, fontWeight = FontWeight.Bold, color = c.ink, fontSize = 16.sp)
                val filled = f.fields.count { it.second.isNotBlank() }
                Text("$filled de ${f.fields.size} campos completos", color = c.muted, fontSize = 12.sp)
                f.fields.firstOrNull { it.second.isNotBlank() && it.first != "Referencia" }?.let { (n, v) ->
                    Text("$n: $v", color = c.ink, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun fichaText(f: Ficha) = buildString {
    append(f.title).append("\n\n")
    f.fields.forEach { (n, v) -> if (v.isNotBlank()) append(n.uppercase()).append("\n").append(v.trim()).append("\n\n") }
}.trim()

@Composable
fun FichaEditor(vm: HiloViewModel, book: Book, a: Analysis?, start: Ficha, onClose: () -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    var title by remember(start) { mutableStateOf(start.title) }
    val values = remember(start) { mutableStateListOf(*start.fields.map { it.second }.toTypedArray()) }
    var pickFor by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val highlights = rememberHighlights(vm, book)

    fun current() = start.copy(title = title.trim(), fields = start.fields.mapIndexed { i, (n, _) -> n to values[i] }, time = System.currentTimeMillis())
    fun save() {
        val f = current()
        if (f.title.isBlank() && f.fields.all { it.second.isBlank() }) return
        vm.study.putFicha(if (f.id == 0L) f.copy(id = vm.study.newId()) else f)
    }
    fun close() { save(); onClose() }
    BackHandler { close() }

    fun suggestion(name: String): String? = when {
        name.startsWith("Conceptos") -> a?.let { (it.phrases.take(4) + it.terms.take(8)).distinctBy { t -> t.key }.take(10).joinToString(", ") { t -> t.label } }
        name.startsWith("Norma") -> {
            val refs = LinkedHashSet<String>()
            highlights.forEach { refs.addAll(Law.refsIn(it)) }
            vm.study.notes[book.id]?.let { refs.addAll(Law.refsIn(it)) }
            if (refs.isEmpty()) null else refs.joinToString(", ") { "Art. $it" } + " — " + book.title
        }
        name == "Referencia" -> (vm.study.refs[book.id] ?: BookRef(book.author, "", book.title)).let { r -> Apa.reference(r.authors, r.year, r.title.ifBlank { book.title }, r.publisher, r.url) }
        name.startsWith("Citas") -> vm.study.quotesOf(book.id).takeIf { it.isNotEmpty() }?.let { qs ->
            val r = vm.study.refs[book.id] ?: BookRef(book.author)
            qs.take(6).joinToString("\n") { "• " + Apa.quote(it.text, r.authors, r.year, it.loc) }
        }
        else -> null
    }

    Box(
        Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing).pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = { close() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Guardar y volver", tint = c.ink) }
                Text(FICHA_KINDS.firstOrNull { it.first == start.kind }?.second ?: "Ficha", color = c.ink, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { save(); shareText(ctx, title, fichaText(current())) }) { Text("Compartir") }
            }
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                start.fields.forEachIndexed { i, (name, _) ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        OutlinedTextField(values[i], { values[i] = it }, label = { Text(name) }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        Row {
                            if (highlights.isNotEmpty()) TextButton(onClick = { pickFor = i }) { Text("+ Resaltado", fontSize = 13.sp) }
                            suggestion(name)?.takeIf { it.isNotBlank() }?.let { s ->
                                TextButton(onClick = { values[i] = if (values[i].isBlank()) s else values[i].trimEnd() + "\n" + s }) { Text("Sugerir", fontSize = 13.sp) }
                            }
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val f = current()
                        var n = 0
                        f.fields.filter { it.second.isNotBlank() && it.first != "Referencia" }.forEach { (name, v) ->
                            vm.study.add(StudyCard(vm.study.newId(), book.id, "qa", "${f.title.ifBlank { book.title }}\n$name", v.trim(),
                                source = "ficha", due = today(), created = System.currentTimeMillis()))
                            n++
                        }
                        vm.toast = if (n == 0) "Completa algún campo primero." else "Creé $n tarjetas con la ficha."
                    }) { Text("Pasar a tarjetas", color = c.ink) }
                    if (start.id != 0L) TextButton(onClick = { confirmDelete = true }) { Text("Borrar ficha", color = c.lines[0]) }
                }
            }
        }
    }

    pickFor?.let { i ->
        AlertDialog(
            onDismissRequest = { pickFor = null },
            containerColor = c.surface,
            title = { Text("Elige un resaltado") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    highlights.forEach { h ->
                        Text(h, color = c.ink, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.band)
                            .clickable { values[i] = if (values[i].isBlank()) h else values[i].trimEnd() + "\n" + h; pickFor = null }.padding(10.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickFor = null }) { Text("Cerrar") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = c.surface,
            title = { Text("¿Borrar la ficha?") },
            confirmButton = { TextButton(onClick = { vm.study.deleteFicha(start.id); confirmDelete = false; onClose() }) { Text("Borrar", color = c.lines[0]) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

// =====================================================================
// Citas en formato APA
// =====================================================================

/** "pág. 12" → "p. 12"; "párrafo 30" → "párr. 30". */
fun apaLoc(source: String): String {
    Regex("p[áa]g\\.?\\s*(\\d+)").find(source)?.let { return "p. ${it.groupValues[1]}" }
    Regex("p[áa]rrafo\\s*(\\d+)").find(source)?.let { return "párr. ${it.groupValues[1]}" }
    return ""
}

@Composable
fun CitesTab(vm: HiloViewModel, book: Book) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val saved = vm.study.refs[book.id]
    var authors by remember(book.id) { mutableStateOf(saved?.authors ?: book.author) }
    var year by remember(book.id) { mutableStateOf(saved?.year.orEmpty()) }
    var title by remember(book.id) { mutableStateOf(saved?.title?.ifBlank { null } ?: book.title) }
    var publisher by remember(book.id) { mutableStateOf(saved?.publisher.orEmpty()) }
    var url by remember(book.id) { mutableStateOf(saved?.url.orEmpty()) }
    fun store() = vm.study.setRef(book.id, BookRef(authors.trim(), year.trim(), title.trim(), publisher.trim(), url.trim()))
    val ref = Apa.reference(authors, year, title, publisher, url)
    val quotes = vm.study.quotesOf(book.id)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel {
            Text("Referencia APA", fontWeight = FontWeight.Bold, color = c.ink, fontSize = 16.sp)
            OutlinedTextField(authors, { authors = it; store() }, label = { Text("Autor o autores (sepáralos con ;)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(year, { year = it.filter { ch -> ch.isDigit() || ch in "abs.f" }.take(8); store() }, label = { Text("Año") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(110.dp))
                OutlinedTextField(publisher, { publisher = it; store() }, label = { Text("Editorial") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(title, { title = it; store() }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(url, { url = it; store() }, label = { Text("Enlace (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(ref, color = c.ink, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.band).padding(12.dp))
            Row {
                TextButton(onClick = { copyText(ctx, ref); vm.toast = "Referencia copiada." }) { Text("Copiar") }
                TextButton(onClick = {
                    runCatching { com.hilo.lector.data.DocExport.shareBib(ctx, title.ifBlank { book.title }, com.hilo.lector.data.DocExport.bibtex(BookRef(authors, year, title, publisher, url), book.title)) }
                }) { Text("BibTeX") }
                if (quotes.isNotEmpty()) TextButton(onClick = {
                    runCatching { com.hilo.lector.data.DocExport.share(ctx, "Citas de " + book.title, quotes.map { Apa.quote(it.text, authors, year, it.loc) } + listOf("Referencia", ref)) }
                }) { Text("Word") }
                if (quotes.isNotEmpty()) TextButton(onClick = {
                    copyText(ctx, quotes.joinToString("\n\n") { Apa.quote(it.text, authors, year, it.loc) } + "\n\nReferencia\n" + ref)
                    vm.toast = "Citas y referencia copiadas."
                }) { Text("Copiar todo") }
            }
            Text("El título va en cursiva cuando lo pegues en tu trabajo.", color = c.muted, fontSize = 12.sp)
        }
        if (quotes.isEmpty()) {
            Text("Para guardar una cita, mantén el dedo sobre el texto mientras lees y toca «Citar». Queda con su página o párrafo.", color = c.muted, fontSize = 14.sp)
        }
        // Banco de citas por tema
        val themes = quotes.mapNotNull { vm.motor["qtag:${it.id}"].ifBlank { null } }.distinct().sorted()
        var theme by remember(book.id) { mutableStateOf("") }
        if (themes.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill("Todas", theme.isEmpty()) { theme = "" }
            themes.forEach { t -> Pill(t, t == theme) { theme = t } }
        }
        var tagFor by remember { mutableStateOf<Quote?>(null) }
        quotes.filter { theme.isEmpty() || vm.motor["qtag:${it.id}"] == theme }.forEach { q ->
            Panel {
                vm.motor["qtag:${q.id}"].takeIf { it.isNotBlank() }?.let { Text(it.uppercase(), color = c.accent, fontSize = 11.sp, letterSpacing = 0.8.sp) }
                Text(Apa.quote(q.text, authors, year, q.loc), color = c.ink, fontSize = 15.sp, lineHeight = 21.sp)
                Row {
                    TextButton(onClick = { copyText(ctx, Apa.quote(q.text, authors, year, q.loc)); vm.toast = "Cita copiada." }) { Text("Copiar") }
                    if (q.pos >= 0) TextButton(onClick = { vm.openAt(book.id, q.flow, q.pos) }) { Text("Ir") }
                    TextButton(onClick = { tagFor = q }) { Text("Tema") }
                    TextButton(onClick = { vm.study.deleteQuote(q.id) }) { Text("Borrar", color = c.lines[0]) }
                }
            }
        }
        tagFor?.let { q ->
            var t by remember(q.id) { mutableStateOf(vm.motor["qtag:${q.id}"]) }
            AlertDialog(
                onDismissRequest = { tagFor = null }, containerColor = c.surface, title = { Text("Tema de la cita") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(t, { t = it }, singleLine = true, label = { Text("Por ejemplo: anomia, poder, contratos") }, modifier = Modifier.fillMaxWidth())
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { themes.forEach { th -> Pill(th) { t = th } } }
                    }
                },
                confirmButton = { TextButton(onClick = { vm.motor.put("qtag:${q.id}", t.trim()); tagFor = null }) { Text("Guardar") } },
            )
        }
    }
}

internal fun saveQuote(vm: HiloViewModel, req: StudyRequest, text: String) {
    vm.study.addQuote(Quote(vm.study.newId(), req.bookId, text.trim(), apaLoc(req.source), req.pos, req.flow, System.currentTimeMillis()))
    vm.toast = "Cita guardada en Estudio → Citas."
}

// =====================================================================
// Modo Feynman: explícalo con tus palabras
// =====================================================================

data class FeynmanReq(val bookId: String, val term: String, val reference: String, val extra: List<String>)

@Composable
fun FeynmanDialog(vm: HiloViewModel, req: FeynmanReq, onDismiss: () -> Unit) {
    val c = LocalHilo.current
    val lang = vm.study.langs[req.bookId].orEmpty()
    var text by remember(req) { mutableStateOf("") }
    var checked by remember(req) { mutableStateOf(false) }
    val keys = remember(req) { KeyWords.of(req.reference, (listOf(req.term) + req.extra).distinct(), 8).filterNot { it.equals(req.term, true) } }
    val dictate = rememberDictation(vm, lang) { t -> text = if (text.isBlank()) t else "$text $t"; checked = false }
    val (hit, miss) = KeyWords.coverage(text, keys)
    val pct = if (keys.isEmpty()) 0 else hit.size * 100 / keys.size
    // Rúbrica de Feynman: además de las palabras clave, cómo lo explicaste
    val rubric = remember(req) { mutableStateListOf(false, false, false, false) }
    val rubricNames = listOf("Lo dije claro y en orden", "Sin palabras difíciles (o las expliqué)", "Puse un ejemplo propio", "Lo conecté con otro concepto")
    fun saveScore() {
        val score = (pct * 0.5f + rubric.count { it } * 12.5f).toInt()
        vm.motor.add(com.hilo.lector.data.Entry(vm.motor.newId(), req.bookId, "feynman", req.term, text, System.currentTimeMillis(), score))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Explica «${req.term}»") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Escríbelo o dilo como si se lo contaras a alguien que no sabe nada del tema. Sin mirar el texto.", color = c.muted, fontSize = 13.sp)
                OutlinedTextField(text, { text = it; checked = false }, minLines = 4, placeholder = { Text("Con mis palabras…") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = dictate) {
                    Icon(Icons.Rounded.Mic, null, tint = c.accent, modifier = Modifier.size(18.dp)); Text("  Decirlo en voz alta", color = c.ink)
                }
                if (checked) {
                    Text("Usaste $pct % de las ideas clave", fontWeight = FontWeight.Bold, color = if (pct >= 60) c.lines[1] else c.lines[2], fontSize = 16.sp)
                    if (hit.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        hit.forEach { Text("✓ $it", fontSize = 13.sp, color = Color.White, modifier = Modifier.clip(RoundedCornerShape(50)).background(c.lines[1]).padding(horizontal = 10.dp, vertical = 4.dp)) }
                    }
                    if (miss.isNotEmpty()) {
                        Text("Te faltó mencionar:", color = c.ink, fontSize = 13.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            miss.forEach { Text(it, fontSize = 13.sp, color = c.ink, modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, c.lines[0], RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)) }
                        }
                    }
                    Label("Tu explicación tuvo…")
                    rubricNames.forEachIndexed { i, n ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { rubric[i] = !rubric[i] }) {
                            androidx.compose.material3.Checkbox(checked = rubric[i], onCheckedChange = { rubric[i] = it })
                            Text(n, color = c.ink, fontSize = 14.sp)
                        }
                    }
                    Text("Puntaje: ${(pct * 0.5f + rubric.count { it } * 12.5f).toInt()} / 100", color = c.accent, fontWeight = FontWeight.Bold)
                    Label("Lo que dice el texto")
                    Text(req.reference, color = c.ink, fontSize = 14.sp, lineHeight = 20.sp)
                    TextButton(onClick = {
                        vm.study.add(StudyCard(vm.study.newId(), req.bookId, "qa", "Explica con tus palabras: ${req.term}", req.reference.take(1200),
                            source = "Feynman", due = today(), created = System.currentTimeMillis()))
                        vm.toast = "Tarjeta creada."
                    }) { Text("Guardar como tarjeta") }
                }
            }
        },
        confirmButton = {
            if (!checked) TextButton(onClick = { checked = true }, enabled = text.isNotBlank()) { Text("Comparar") }
            else TextButton(onClick = { saveScore(); onDismiss() }) { Text("Listo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (checked) "Cerrar" else "Cancelar") } },
    )
}

// =====================================================================
// Fecha de examen y plan
// =====================================================================

private val DATE_FMT = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es"))

fun dayLabel(day: Long): String = LocalDate.ofEpochDay(day).format(DATE_FMT)

@Composable
fun ExamDatePicker(initial: Long?, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val todayMs = today() * 86_400_000L
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.let { it * 86_400_000L },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMs
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { ms -> onPick(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) }
                onDismiss()
            }) { Text("Listo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) { DatePicker(state = state, title = { Text("Fecha del examen", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) }) }
}

@Composable
fun ExamCard(vm: HiloViewModel, book: Book, a: Analysis?) {
    val c = LocalHilo.current
    val ex = vm.study.exams[book.id]
    var picking by remember { mutableStateOf(false) }
    val t = today()
    if (ex == null || ex < t) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).clickable { picking = true }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Event, null, tint = c.accent)
            Text(if (ex != null) "  El examen ya pasó. ¿Tienes otro?" else "  ¿Tienes examen? Pon la fecha y te armo un plan", color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        }
    } else {
        val days = (ex - t).toInt()
        val guides = a?.guides.orEmpty()
        val done = vm.study.chapDone[book.id].orEmpty()
        val pending = guides.filter { it.chapter.startPara !in done }
        val studyDays = max(1, days - 1)
        val perDay = if (pending.isEmpty()) 0 else ceil(pending.size / studyDays.toDouble()).toInt()
        val today = if (days <= 1) emptyList() else pending.take(perDay)
        val due = vm.study.cards.count { it.bookId == book.id && it.due <= t }
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Event, null, tint = if (days <= 3) c.lines[0] else c.accent)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        when (days) { 0 -> "¡El examen es hoy!"; 1 -> "El examen es mañana"; else -> "Examen en $days días" },
                        fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = c.ink,
                    )
                    Text(dayLabel(ex), color = c.muted, fontSize = 13.sp)
                }
                TextButton(onClick = { picking = true }) { Text("Cambiar") }
            }
            Label("Plan para hoy")
            when {
                days <= 1 -> Text("Repaso general: tus tarjetas, tus resaltados y la guía. Nada nuevo; descansa bien.", color = c.ink, fontSize = 14.sp)
                today.isNotEmpty() -> today.take(3).forEach { g ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("• ${g.chapter.title}", color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = { vm.openAt(book.id, true, g.chapter.startPara) }) { Text("Leer") }
                        TextButton(onClick = { vm.study.toggleChapter(book.id, g.chapter.startPara) }) { Text("Hecho") }
                    }
                }
                guides.isNotEmpty() -> Text("Ya estudiaste todas las partes. Ahora repasa con tarjetas y preguntas de la guía.", color = c.ink, fontSize = 14.sp)
                else -> Text("Lee un poco cada día y repasa tus tarjetas.", color = c.ink, fontSize = 14.sp)
            }
            if (today.size > 3) Text("…y ${today.size - 3} partes más hoy. Empieza por estas.", color = c.muted, fontSize = 13.sp)
            if (due > 0) Text("Y repasa $due ${if (due == 1) "tarjeta" else "tarjetas"}.", color = c.ink, fontSize = 14.sp)
            if (pending.isNotEmpty() && days > 1) Text("Te faltan ${pending.size} de ${guides.size} partes: unas $perDay por día.", color = c.muted, fontSize = 12.sp)
            TextButton(onClick = { vm.study.setExam(book.id, null) }) { Text("Quitar fecha", color = c.muted) }
        }
    }
    if (picking) ExamDatePicker(ex, onPick = { vm.study.setExam(book.id, it); vm.toast = "Listo. Tus tarjetas vuelven antes del examen." }) { picking = false }
}

// =====================================================================
// Mapa entre documentos
// =====================================================================

private class CrossData(val analysis: Analysis, val docs: List<Book>, val per: List<Analysis>)

@Composable
fun CrossMapView(vm: HiloViewModel, onOpenDeck: (String) -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val books by vm.repo.books.collectAsStateWithLifecycle()
    var prog by remember { mutableStateOf("") }
    val data by produceState<CrossData?>(null) {
        val cand = books.filter { b -> !b.trashed && (b.type != "pdf" || File(ctx.filesDir, "flow/${b.id}.txt").exists()) }
            .sortedByDescending { b -> (if (vm.study.cards.any { it.bookId == b.id }) 1L shl 50 else 0L) + b.updated }.take(10)
        val docs = ArrayList<Book>()
        val per = ArrayList<Analysis>()
        cand.forEachIndexed { i, b ->
            prog = "Leyendo ${i + 1} de ${cand.size}: ${b.title.take(40)}"
            runCatching { vm.analysis(b) { _, _ -> } }.getOrNull()?.takeIf { it.totalWords > 50 }?.let { docs.add(b); per.add(it) }
        }
        val occ = HashMap<String, HashMap<Int, Int>>()
        val label = HashMap<String, String>()
        per.forEachIndexed { di, a ->
            (a.phrases.take(15) + a.terms.take(70)).forEach { t ->
                occ.getOrPut(t.key) { HashMap() }[di] = t.count
                label.putIfAbsent(t.key, t.label)
            }
        }
        // Conceptos importantes en al menos dos documentos (no palabras sueltas que aparecen de paso)
        val shared = occ.entries.filter { e -> e.value.count { it.value >= 3 } >= 2 }
            .sortedByDescending { e -> e.value.size * 1000 + e.value.values.sum() }.take(18)
        val usedDocs = shared.flatMap { it.value.keys }.toSortedSet().toList()
        val nodes = ArrayList<Term>()
        val idxDoc = HashMap<Int, Int>()
        usedDocs.forEach { d -> idxDoc[d] = nodes.size; nodes.add(Term("#doc$d", docs[d].title.take(26), shared.sumOf { it.value[d] ?: 0 }, true)) }
        val edges = ArrayList<Edge>()
        shared.forEach { e ->
            val ti = nodes.size
            nodes.add(Term(e.key, label[e.key] ?: e.key, e.value.values.sum(), false))
            e.value.forEach { (d, n) -> idxDoc[d]?.let { edges.add(Edge(ti, it, n)) } }
        }
        val an = Analysis(0, 0, emptyList(), emptyList(), emptyList(), nodes, edges, emptyList(), emptyList())
        value = CrossData(an, docs, per)
    }
    val d = data
    when {
        d == null -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(color = c.accent)
            Text(prog.ifBlank { "Buscando conceptos en común…" }, color = c.muted)
        }
        d.analysis.mapTerms.count { it.phrase } < 2 -> Text(
            "Necesito al menos dos documentos con temas en común. Los PDF entran al mapa después de abrir su sala de estudio una vez.",
            color = c.muted,
        )
        else -> ConceptMapTab(
            vm, null, d.analysis,
            intro = "Los conceptos que comparten tus documentos. Los círculos grandes en verde azulado son tus documentos; toca un concepto para ver qué dice cada uno.",
        ) { s, neighbors, select ->
            val t = d.analysis.mapTerms[s]
            Panel {
                Text(t.label, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.ink)
                if (t.key.startsWith("#doc")) {
                    val di = t.key.removePrefix("#doc").toInt()
                    Text("Comparte ${neighbors.size} conceptos con tus otros documentos.", color = c.muted, fontSize = 13.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        neighbors.forEach { j -> Pill(d.analysis.mapTerms[j].label) { select(j) } }
                    }
                    Button(onClick = { onOpenDeck(d.docs[di].id) }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) {
                        Text("Abrir su sala de estudio")
                    }
                } else {
                    Label("Aparece en")
                    neighbors.forEach { j ->
                        val di = d.analysis.mapTerms[j].key.removePrefix("#doc").toIntOrNull() ?: return@forEach
                        val b = d.docs[di]
                        val st = d.per[di].sentencesWith(t.key, 40).minByOrNull { kotlin.math.abs(it.text.length - 160) }
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.band).padding(10.dp)) {
                            Text(b.title, fontWeight = FontWeight.Bold, color = c.ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            st?.let { Text(it.text, color = c.ink, fontSize = 13.sp, maxLines = 5, overflow = TextOverflow.Ellipsis) }
                            Row {
                                st?.let { s2 -> TextButton(onClick = { vm.openAt(b.id, true, s2.para) }) { Text("Ir") } }
                                TextButton(onClick = { onOpenDeck(b.id) }) { Text("Sala de estudio") }
                            }
                        }
                    }
                    TextButton(onClick = {
                        val ref = neighbors.mapNotNull { j -> d.analysis.mapTerms[j].key.removePrefix("#doc").toIntOrNull() }
                            .mapNotNull { di -> d.per[di].sentencesWith(t.key, 3).firstOrNull()?.text }.joinToString(" ")
                        vm.feynman = FeynmanReq(d.docs.first().id, t.label, ref, emptyList())
                    }) { Text("Explícalo con tus palabras") }
                    TextButton(onClick = {
                        val parts = neighbors.mapNotNull { j -> d.analysis.mapTerms[j].key.removePrefix("#doc").toIntOrNull() }
                            .mapNotNull { di -> d.per[di].sentencesWith(t.key, 3).firstOrNull()?.let { s -> d.docs[di].title + ": " + s.text } }
                        if (parts.size >= 2) {
                            vm.study.add(StudyCard(vm.study.newId(), d.docs.first().id, "qa", "¿Cómo tratan «${t.label}» tus distintos textos? ¿En qué coinciden y en qué no?", parts.joinToString("\n\n"),
                                source = "pregunta cruzada", due = today(), created = System.currentTimeMillis()))
                            vm.toast = "Pregunta cruzada creada."
                        }
                    }) { Text("Crear pregunta cruzada") }
                    LookupRow(t.label, "")
                }
            }
        }
    }
}

// =====================================================================
// Herramientas generales: recordatorio, Anki y copia de seguridad
// =====================================================================

@Composable
fun HubTools(vm: HiloViewModel, onCrossMap: () -> Unit, onDeck: (String) -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val books by vm.repo.books.collectAsStateWithLifecycle()
    var withBooks by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    @Suppress("UNUSED_VARIABLE") val tick = vm.study.remindTick // se vuelve a dibujar al cambiar el recordatorio
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        vm.study.remindOn = ok
        com.hilo.lector.data.Reminders.schedule(ctx)
        vm.toast = if (ok) "Te aviso cada día a las ${vm.study.remindHour}:00." else "Sin permiso de notificaciones no puedo avisarte."
    }
    val saveZip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> uri?.let { vm.backup(it, withBooks) } }
    val openZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> confirmRestore = uri }
    val pickAnki = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.importAnki(it) } }

    val t = today()
    val exams = vm.study.exams.entries.filter { it.value >= t }.sortedBy { it.value }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (exams.isNotEmpty()) {
            Label("Próximos exámenes")
            exams.forEach { (id, day) ->
                val b = books.firstOrNull { it.id == id } ?: return@forEach
                val d = (day - t).toInt()
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).clickable { onDeck(id) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Event, null, tint = if (d <= 3) c.lines[0] else c.accent)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(b.title, color = c.ink, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(dayLabel(day), color = c.muted, fontSize = 12.sp)
                    }
                    Text(when (d) { 0 -> "hoy"; 1 -> "mañana"; else -> "en $d días" }, color = c.accent, fontWeight = FontWeight.Bold)
                }
            }
        }
        Label("Herramientas")
        Panel(Modifier.clickable(onClick = onCrossMap)) {
            Text("Mapa entre tus documentos", fontWeight = FontWeight.Bold, color = c.ink, fontSize = 16.sp)
            Text("Une los conceptos que comparten tus textos: por ejemplo «ideología» en Marx, en Mannheim y en tus apuntes.", color = c.muted, fontSize = 13.sp)
        }
        Panel {
            SwitchRow(
                "Recordatorio diario", "Te avisa si tienes tarjetas para hoy o un examen cerca.", vm.study.remindOn,
            ) { on ->
                if (on && com.hilo.lector.data.Reminders.needsPermission(ctx)) notifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                else {
                    vm.study.remindOn = on
                    com.hilo.lector.data.Reminders.schedule(ctx)
                    if (on) vm.toast = "Te aviso cada día a las ${vm.study.remindHour}:00."
                }
            }
            if (vm.study.remindOn) {
                val hours = listOf(8, 13, 17, 20)
                ChoiceRow(hours.map { "$it:00" }, hours.indexOf(vm.study.remindHour)) { i ->
                    vm.study.remindHour = hours[i]
                    com.hilo.lector.data.Reminders.schedule(ctx)
                }
            }
        }
        Panel {
            Text("Anki", fontWeight = FontWeight.Bold, color = c.ink, fontSize = 16.sp)
            Text("Importa tus mazos (.apkg o .txt) para repasarlos en Hilo, o exporta tus tarjetas para usarlas en Anki.", color = c.muted, fontSize = 13.sp)
            OutlinedButton(onClick = { pickAnki.launch(arrayOf("*/*")) }) { Text("Importar un mazo", color = c.ink) }
            OutlinedButton(onClick = {
                val cards = vm.study.cards.toList()
                if (cards.isEmpty()) vm.toast = "Todavía no tienes tarjetas."
                else runCatching { AnkiExport.share(ctx, AnkiExport.file(ctx, cards, { id -> books.firstOrNull { it.id == id }?.title ?: "Hilo" }, "Hilo_tarjetas")) }
                    .onFailure { vm.toast = "No pude crear el archivo." }
            }) { Text(if (vm.study.cards.size == 1) "Exportar 1 tarjeta" else "Exportar ${vm.study.cards.size} tarjetas", color = c.ink) }
        }
        Panel {
            Text("Copia de seguridad", fontWeight = FontWeight.Bold, color = c.ink, fontSize = 16.sp)
            Text("Guarda tus tarjetas, resaltados, fichas, citas, anotaciones y avance en un archivo. Guárdalo en Drive para no perder nada si cambias de celular.", color = c.muted, fontSize = 13.sp)
            SwitchRow("Incluir los libros importados", "El archivo pesa más. Los documentos que Hilo encontró en el celular no se copian.", withBooks) { withBooks = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { saveZip.launch("Hilo-copia-${LocalDate.now()}.zip") },
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
                ) { Text("Guardar copia") }
                OutlinedButton(onClick = { openZip.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }) { Text("Restaurar", color = c.ink) }
            }
        }
    }
    confirmRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            containerColor = c.surface,
            title = { Text("¿Restaurar la copia?") },
            text = { Text("Se reemplazan tus tarjetas, notas y avance actuales por los de la copia. Hilo se reinicia al terminar.", color = c.ink) },
            confirmButton = { TextButton(onClick = { confirmRestore = null; vm.restore(uri) }) { Text("Restaurar") } },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("Cancelar") } },
        )
    }
}

/** Exporta solo las tarjetas de un documento. */
fun exportDeck(vm: HiloViewModel, ctx: Context, book: Book) {
    val cards = vm.study.cardsOf(book.id)
    if (cards.isEmpty()) { vm.toast = "Este documento no tiene tarjetas."; return }
    runCatching { AnkiExport.share(ctx, AnkiExport.file(ctx, cards, { book.title }, book.title)) }.onFailure { vm.toast = "No pude crear el archivo." }
}
