package com.hilo.lector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hilo.lector.HiloViewModel
import com.hilo.lector.data.Book
import com.hilo.lector.data.Clase
import com.hilo.lector.data.DocExport
import com.hilo.lector.data.SearchIndex
import com.hilo.lector.data.StudyCard
import com.hilo.lector.data.today
import com.hilo.lector.text.Analysis
import com.hilo.lector.text.NormLevels
import com.hilo.lector.text.Reference
import com.hilo.lector.text.Smart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** El texto (párrafos) de varios libros, cargado una vez y agregado a la búsqueda. */
@Composable
fun rememberTexts(vm: HiloViewModel, books: List<Book>): Pair<Map<String, List<String>>?, String> {
    var msg by remember { mutableStateOf("") }
    val texts by produceState<Map<String, List<String>>?>(null, books.map { it.id }) {
        val out = LinkedHashMap<String, List<String>>()
        books.forEachIndexed { i, b ->
            msg = "Leyendo ${i + 1} de ${books.size}: ${b.title.take(40)}"
            try {
                val paras = vm.studyText(b) { _, _ -> }
                out[b.id] = paras
                withContext(Dispatchers.IO) { runCatching { vm.search.ensure(b.id, paras) } }
            } catch (e: CancellationException) { throw e } catch (_: Throwable) { }
        }
        value = out
    }
    return texts to msg
}

@Composable
private fun Loading(msg: String) {
    val c = LocalHilo.current
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = c.accent)
        Text(msg.ifBlank { "Preparando…" }, color = c.muted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun Card(title: String, sub: String? = null, content: @Composable () -> Unit) {
    val c = LocalHilo.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (sub != null) Text(sub, color = c.muted, fontSize = 13.sp)
        content()
    }
}

// =====================================================================
// Buscar en el área (palabras o párrafos parecidos)
// =====================================================================

@Composable
fun AreaSearchTab(vm: HiloViewModel, cl: Clase, books: List<Book>) {
    val c = LocalHilo.current
    val (texts, msg) = rememberTexts(vm, books)
    var q by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(0) }   // 0 palabras, 1 parecido
    var run by remember { mutableStateOf("") }
    val titles = books.associate { it.id to it.title }
    val ids = books.map { it.id }.toSet()
    if (texts == null) { Loading(msg); return }
    val results by produceState<List<SearchIndex.Passage>?>(emptyList(), run, mode) {
        if (run.isBlank()) { value = emptyList(); return@produceState }
        value = null
        value = withContext(Dispatchers.IO) {
            if (mode == 0) vm.search.search(run, 300).filter { it.bookId in ids }.map { h -> SearchIndex.Passage(h.bookId, h.para, texts[h.bookId]?.getOrNull(h.para) ?: h.snippet, 1f) }.take(60)
            else vm.search.similar(Reference.keywords(run), ids, 30)
        }
    }
    LazyColumn(TABLET_WIDTH, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                if (mode == 0) "Busca una palabra o frase en todos los materiales de ${if (cl.isArea) "esta área" else "esta clase"}."
                else "Pega un párrafo (por ejemplo, de un caso o de otro documento) y Hilo encuentra los párrafos más parecidos en tus materiales: qué norma o autor trata lo mismo.",
                color = c.muted, fontSize = 13.sp,
            )
        }
        item { ChoiceRow(listOf("Palabras", "Párrafos parecidos"), mode) { mode = it; run = "" } }
        item {
            OutlinedTextField(
                q, { q = it }, modifier = Modifier.fillMaxWidth(), minLines = if (mode == 1) 4 else 1, singleLine = mode == 0,
                placeholder = { Text(if (mode == 0) "Ej.: prescripción" else "Pega aquí el texto") },
            )
        }
        item { Button(onClick = { run = q }, enabled = q.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Color(cl.color), contentColor = Color.White)) { Text("Buscar") } }
        val r = results
        if (r == null) item { CircularProgressIndicator(color = c.accent) }
        else {
            if (run.isNotBlank() && r.isEmpty()) item { Text("No encontré nada parecido.", color = c.muted) }
            if (mode == 1 && run.isNotBlank()) item { Text("Palabras clave: " + Reference.keywords(run).joinToString(", "), color = c.muted, fontSize = 12.sp) }
            items(r) { p ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).clickable { vm.openAt(p.bookId, true, p.para) }.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(titles[p.bookId].orEmpty(), color = Color(cl.color), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(p.text.removePrefix("# ").take(600), color = c.ink, fontSize = 14.sp, lineHeight = 19.sp)
                }
            }
        }
    }
}

// =====================================================================
// Glosario del área: definiciones de los textos y definiciones legales
// =====================================================================

@Composable
fun AreaGlossaryTab(vm: HiloViewModel, cl: Clase, books: List<Book>, docs: List<Pair<String, Analysis>>) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val (texts, msg) = rememberTexts(vm, books)
    var q by remember { mutableStateOf("") }
    if (texts == null) { Loading(msg); return }
    val titles = books.associate { it.id to it.title }
    val all = remember(docs, texts) {
        val legal = texts.flatMap { (id, ps) -> Reference.legalDefinitions(ps).map { Triple(id, it.term, it.definition) to it.para } }
        val gl = docs.flatMap { (id, a) -> a.glossary.map { Triple(id, it.term, it.definition) to it.para } }
        (legal + gl).distinctBy { Smart.fold(it.first.second) + "|" + it.first.third.take(40) }.sortedBy { Smart.fold(it.first.second) }
    }
    val shown = all.filter { q.isBlank() || Smart.fold(it.first.second + " " + it.first.third).contains(Smart.fold(q)) }
    LazyColumn(TABLET_WIDTH, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                "${all.size} definiciones encontradas en tus materiales: las que el texto escribe («X es…») y las definiciones legales («Para efectos de esta ley se entiende por…»).",
                color = c.muted, fontSize = 13.sp,
            )
        }
        item { OutlinedTextField(q, { q = it }, placeholder = { Text("Buscar un término") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { DocExport.share(ctx, "Glosario — ${cl.name}", all.map { "${it.first.second}: ${it.first.third} (${titles[it.first.first].orEmpty()})" }) }) { Text("Exportar a Word", color = c.ink) }
                OutlinedButton(onClick = {
                    shown.take(60).forEach { (t, para) ->
                        vm.study.add(StudyCard(vm.study.newId(), t.first, "qa", "¿Qué es «${t.second}»?", t.third, source = titles[t.first].orEmpty(), sourcePos = para, sourceFlow = true, due = today(), created = System.currentTimeMillis()))
                    }
                    vm.toast = "Creé ${minOf(60, shown.size)} tarjetas."
                }) { Text("A tarjetas", color = c.ink) }
            }
        }
        items(shown.take(400)) { (t, para) ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).clickable { vm.openAt(t.first, true, para) }.padding(12.dp)) {
                Text(t.second, color = Color(cl.color), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(t.third, color = c.ink, fontSize = 14.sp, lineHeight = 19.sp)
                Text(titles[t.first].orEmpty(), color = c.muted, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

// =====================================================================
// Vocabulario propio del área
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AreaVocabTab(vm: HiloViewModel, cl: Clase, books: List<Book>, docs: List<Pair<String, Analysis>>) {
    val c = LocalHilo.current
    var msg by remember { mutableStateOf("") }
    val vocab by produceState<List<Pair<com.hilo.lector.text.Term, Float>>?>(null, docs) {
        // Comparar con otros libros de tu biblioteca (los últimos que leíste, fuera del área)
        val ids = cl.books.toSet()
        val others = vm.repo.books.value.filter { !it.trashed && it.id !in ids && it.type != "comic" }.sortedByDescending { it.updated }.take(4)
        val rest = ArrayList<Analysis>()
        others.forEachIndexed { i, b ->
            msg = "Comparando con otro libro tuyo (${i + 1} de ${others.size})…"
            try { rest.add(vm.analysis(b) { _, _ -> }) } catch (e: CancellationException) { throw e } catch (_: Throwable) { }
        }
        value = withContext(Dispatchers.Default) { Reference.vocabulary(docs.map { it.second }, rest) }
    }
    val v = vocab
    if (v == null) { Loading(msg); return }
    val defs = remember(docs) { docs.flatMap { it.second.glossary }.associateBy { Smart.fold(it.term) } }
    LazyColumn(TABLET_WIDTH, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                "Las palabras que hacen «técnico» a este campo: aparecen en estos materiales mucho más que en el resto de tu biblioteca. Toca una para buscarla en el diccionario.",
                color = c.muted, fontSize = 13.sp,
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                v.forEach { (t, _) ->
                    val has = defs.containsKey(Smart.fold(t.label))
                    Text(
                        t.label + (if (has) " ✓" else ""), color = c.ink, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(cl.color).copy(alpha = if (has) 0.25f else 0.12f))
                            .clickable { vm.dictWord = t.label to false }.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
        item { Text("✓ = tiene definición en tus materiales (míralas en Glosario).", color = c.muted, fontSize = 12.sp) }
        item {
            OutlinedButton(onClick = {
                var n = 0
                v.take(40).forEach { (t, _) ->
                    val d = defs[Smart.fold(t.label)]
                    vm.study.add(StudyCard(vm.study.newId(), "area:${cl.id}", "qa", "¿Qué significa «${t.label}» en ${cl.name}?", d?.definition ?: "(escribe la definición)", source = cl.name, due = today(), created = System.currentTimeMillis()))
                    n++
                }
                vm.toast = "Creé $n tarjetas de vocabulario técnico."
            }) { Text("Pasar a tarjetas", color = c.ink) }
        }
    }
}

// =====================================================================
// Derecho: normas, remisiones entre normas, plantillas, plazos y sanciones
// =====================================================================

@Composable
fun NormsTab(vm: HiloViewModel, cl: Clase, books: List<Book>, docs: List<Pair<String, Analysis>>) {
    val c = LocalHilo.current
    val (texts, msg) = rememberTexts(vm, books)
    if (texts == null) { Loading(msg); return }
    val titles = books.associate { it.id to it.title }
    val an = docs.toMap()
    // Remisiones: de cada norma hacia otras normas del área
    val refs = remember(texts) {
        texts.flatMap { (id, ps) -> Reference.crossRefs(ps).map { id to it } }
            .map { (from, r) -> Triple(from, r, Reference.resolve(r.law, titles - from)) }
    }
    val byLevel = books.groupBy { NormLevels.levelOf(it.title) }.toSortedMap()
    LazyColumn(TABLET_WIDTH, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Tus normas ordenadas por jerarquía, con cuántos artículos tiene cada una y a qué otras normas remiten.", color = c.muted, fontSize = 13.sp)
        }
        byLevel.forEach { (lv, list) ->
            item {
                Text((NormLevels.levels.getOrNull(lv)?.first ?: "Otros documentos").uppercase(), color = Color(cl.color), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            }
            items(list, key = { it.id }) { b ->
                val a = an[b.id]
                val out = refs.filter { it.first == b.id }
                val inc = refs.filter { it.third == b.id }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(b.title, color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.clickable { vm.openStudy(b.id) })
                    Text(
                        listOfNotNull(
                            a?.law?.articles?.size?.takeIf { it > 0 }?.let { "$it artículos" },
                            a?.law?.deadlines?.size?.takeIf { it > 0 }?.let { "$it plazos" },
                            if (out.isNotEmpty()) "remite ${out.size} veces a otras normas" else null,
                            if (inc.isNotEmpty()) "otras normas la citan ${inc.size} veces" else null,
                        ).joinToString(" · ").ifBlank { "Sin artículos reconocidos" },
                        color = c.muted, fontSize = 12.sp,
                    )
                    out.groupBy { it.second.law }.entries.take(6).forEach { (law, list2) ->
                        val target = list2.first().third
                        Text(
                            "→ $law: " + list2.map { "art. ${it.second.article}" }.distinct().take(8).joinToString(", ") + if (target == null) "  (no está en el área)" else "",
                            color = if (target != null) c.accent else c.muted, fontSize = 13.sp,
                            modifier = Modifier.clickable(enabled = target != null) {
                                val art = an[target]?.law?.find(list2.first().second.article)
                                vm.openAt(target!!, true, art?.para ?: -1)
                            },
                        )
                    }
                }
            }
        }
        val missing = refs.filter { it.third == null }.map { it.second.law }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(10)
        if (missing.isNotEmpty()) item {
            Card("Normas citadas que te faltan", "Si las agregas al área, las remisiones quedan enlazadas.") {
                missing.forEach { (law, n) -> Text("• $law ($n ${if (n == 1) "vez" else "veces"})", color = c.ink, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
fun DeadlinesTab(vm: HiloViewModel, cl: Clase, books: List<Book>, docs: List<Pair<String, Analysis>>) {
    val c = LocalHilo.current
    val (texts, msg) = rememberTexts(vm, books)
    if (texts == null) { Loading(msg); return }
    val titles = books.associate { it.id to it.title }
    var show by remember { mutableStateOf(0) }
    val authorities = remember(texts) { Reference.authorities(texts.values.flatten()) }
    LazyColumn(TABLET_WIDTH, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ChoiceRow(listOf("Plazos", "Sanciones", "Autoridades"), show) { show = it } }
        when (show) {
            0 -> {
                val all = docs.flatMap { (id, a) -> a.law.deadlines.map { id to it } }
                item { Text("${all.size} plazos en todas tus normas. Toca uno para ir al artículo.", color = c.muted, fontSize = 13.sp) }
                items(all.take(400)) { (id, d) ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).clickable { vm.openAt(id, true, d.para) }.padding(12.dp)) {
                        Text("${d.amount} · ${d.article?.let { "art. $it" } ?: ""}", color = Color(cl.color), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(d.sentence.take(300), color = c.ink, fontSize = 14.sp, lineHeight = 19.sp)
                        Text(titles[id].orEmpty(), color = c.muted, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
            1 -> {
                val all = texts.flatMap { (id, ps) -> Reference.sanctions(ps, 80).map { id to it } }
                item { Text("Oraciones que establecen multas, penas o sanciones.", color = c.muted, fontSize = 13.sp) }
                items(all.take(400)) { (id, s) ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).clickable { vm.openAt(id, true, s.first) }.padding(12.dp)) {
                        Text(s.second, color = c.ink, fontSize = 14.sp, lineHeight = 19.sp)
                        Text(titles[id].orEmpty(), color = c.muted, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
            else -> {
                val all = authorities
                item { Text("Instituciones y autoridades que más aparecen en tus normas.", color = c.muted, fontSize = 13.sp) }
                items(all) { n ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(12.dp)) {
                        Text(n.name, color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("${n.count}", color = c.muted, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplatesTab(vm: HiloViewModel, cl: Clase, books: List<Book>) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val key = "modelos:${cl.id}"
    var models by remember { mutableStateOf(vm.motor[key].split('\n').filter { it.isNotBlank() }.toSet()) }
    fun save(m: Set<String>) { models = m; vm.motor.put(key, m.joinToString("\n")) }
    val chosen = books.filter { it.id in models }
    val (texts, msg) = rememberTexts(vm, chosen)
    var check by remember { mutableStateOf<String?>(null) }
    LazyColumn(TABLET_WIDTH, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                "Marca los documentos que sirven de modelo (demandas, recursos, contratos, informes…). Hilo encuentra las partes que se repiten y arma una plantilla con su orden.",
                color = c.muted, fontSize = 13.sp,
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                books.forEach { b ->
                    val on = b.id in models
                    Text(
                        (if (on) "✓ " else "") + b.title.take(36), color = if (on) Color.White else c.ink, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Color(cl.color) else c.chip).clickable { save(if (on) models - b.id else models + b.id) }.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
        if (chosen.isEmpty()) return@LazyColumn
        if (texts == null) { item { CircularProgressIndicator(color = c.accent) }; item { Text(msg, color = c.muted) }; return@LazyColumn }
        val parts = Reference.template(chosen.mapNotNull { texts[it.id] })
        item {
            Card("Plantilla (${chosen.size} ${if (chosen.size == 1) "modelo" else "modelos"})", if (parts.isEmpty()) "No encontré títulos de partes. Los modelos necesitan encabezados como HECHOS, FUNDAMENTOS DE DERECHO o PETICIÓN." else "Las partes en el orden en que suelen ir; el número dice en cuántos modelos aparece.") {
                parts.forEachIndexed { i, p ->
                    Column {
                        Text("${i + 1}. ${p.heading}  ·  ${p.inModels}/${chosen.size}", color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (p.example.isNotBlank()) Text("Ej.: ${p.example}", color = c.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (parts.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        DocExport.share(ctx, "Plantilla — ${cl.name}", parts.flatMap { listOf(it.heading.uppercase(), "[Escribe aquí]") })
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(cl.color), contentColor = Color.White)) { Text("Crear documento en Word") }
                }
            }
        }
        if (parts.isNotEmpty()) item {
            Card("Revisar un borrador", "Elige uno de tus documentos y Hilo te dice qué partes de la plantilla le faltan.") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    vm.repo.books.value.filter { !it.trashed && it.id !in models }.sortedByDescending { it.updated }.take(12).forEach { b ->
                        Text(b.title.take(30), color = c.ink, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(50)).background(if (check == b.id) Color(cl.color).copy(alpha = 0.3f) else c.chip).clickable { check = b.id }.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
                check?.let { id ->
                    val b = vm.repo.get(id)
                    val draft by produceState<List<String>?>(null, id) { value = b?.let { runCatching { vm.studyText(it) { _, _ -> } }.getOrNull() } }
                    val d = draft
                    if (d == null) CircularProgressIndicator(color = c.accent)
                    else {
                        val have = Reference.template(listOf(d)).map { Smart.fold(it.heading) }.toSet()
                        parts.forEach { p ->
                            val ok = Smart.fold(p.heading) in have
                            Text((if (ok) "✓ " else "✗ ") + p.heading, color = if (ok) c.lines[2] else c.lines[0], fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// «Buscar en mis áreas» desde el texto elegido
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefSheet(vm: HiloViewModel, text: String, onDismiss: () -> Unit) {
    val c = LocalHilo.current
    val areas = vm.classes.areas
    val ids = areas.flatMap { it.books }.toSet()
    val titles = vm.repo.books.value.associate { it.id to it.title }
    val res by produceState<Pair<List<Pair<String, com.hilo.lector.text.LawArticle>>, List<SearchIndex.Passage>>?>(null, text) {
        // 1) Si el texto nombra «artículo N de la Ley X», ir directo a ese artículo
        val direct = ArrayList<Pair<String, com.hilo.lector.text.LawArticle>>()
        for (r in Reference.crossRefs(listOf(text))) {
            val id = Reference.resolve(r.law, titles.filterKeys { ids.isEmpty() || it in ids }) ?: continue
            val b = vm.repo.get(id) ?: continue
            val a = try { vm.analysis(b) { _, _ -> } } catch (e: CancellationException) { throw e } catch (_: Throwable) { continue }
            a.law.find(r.article)?.let { direct.add(id to it) }
        }
        // 2) Párrafos parecidos en tus áreas (o en toda la biblioteca si no tienes áreas)
        value = direct to withContext(Dispatchers.IO) { vm.search.similar(Reference.keywords(text), ids, 15) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("En mis áreas", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.ink, modifier = Modifier.weight(1f))
                HelpButton(vm, "areas")
            }
            Text("«${text.take(160)}»", color = c.muted, fontSize = 13.sp)
            if (areas.isEmpty()) Text("Todavía no tienes áreas de referencia: busco en toda tu biblioteca. Crea una en Clases → Áreas.", color = c.muted, fontSize = 12.sp)
            val r = res
            if (r == null) Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.accent) }
            else {
                r.first.forEach { (id, art) ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.band).clickable { onDismiss(); vm.openAt(id, true, art.para) }.padding(12.dp)) {
                        Text("${art.label} · ${titles[id].orEmpty()}", color = c.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(art.text.take(700), color = c.ink, fontSize = 14.sp, lineHeight = 19.sp)
                    }
                }
                if (r.second.isEmpty() && r.first.isEmpty()) Text("No encontré párrafos parecidos. Agrega más documentos a tus áreas o abre los PDF una vez en modo Texto para que entren a la búsqueda.", color = c.muted, fontSize = 13.sp)
                if (r.second.isNotEmpty()) Text("Párrafos que tratan lo mismo", color = c.ink, fontWeight = FontWeight.SemiBold)
                r.second.forEach { p ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.bg).clickable { onDismiss(); vm.openAt(p.bookId, true, p.para) }.padding(12.dp)) {
                        Text(titles[p.bookId].orEmpty(), color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(p.text.removePrefix("# ").take(500), color = c.ink, fontSize = 14.sp, lineHeight = 19.sp)
                    }
                }
            }
        }
    }
}
