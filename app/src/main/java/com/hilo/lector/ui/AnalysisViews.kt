@file:OptIn(ExperimentalLayoutApi::class)

package com.hilo.lector.ui

import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Switch
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hilo.lector.HiloViewModel
import com.hilo.lector.data.Book
import com.hilo.lector.data.StudyCard
import com.hilo.lector.data.today
import com.hilo.lector.text.Analysis
import com.hilo.lector.text.ChapterGuide
import com.hilo.lector.text.Gloss
import com.hilo.lector.text.Question
import com.hilo.lector.text.Term
import com.hilo.lector.text.Sent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal fun saveCard(vm: HiloViewModel, book: Book, kind: String, front: String, back: String, para: Int, context: String = "") {
    vm.study.add(
        StudyCard(
            vm.study.newId(), book.id, kind, front, back, context = context,
            source = if (para >= 0) "párrafo ${para + 1}" else "", sourcePos = para, sourceFlow = true,
            due = today(), created = System.currentTimeMillis(),
        )
    )
}

private fun questionCard(vm: HiloViewModel, book: Book, q: Question) {
    when (q.kind) {
        "cloze" -> saveCard(vm, book, "cloze", q.q, q.a, q.para, context = q.q.replace("[ … ]", q.a))
        "open" -> saveCard(vm, book, "qa", q.q, "Una respuesta posible: " + q.a, q.para)
        else -> saveCard(vm, book, "qa", q.q, q.a, q.para)
    }
}

// =====================================================================
// Palabras: nube flotante y depuración
// =====================================================================

private class Bubble(val term: Term, val r: Float, val x: Float, val y: Float)

private fun packBubbles(terms: List<Term>, w: Float, h: Float, minR: Float, maxR: Float): List<Bubble> {
    if (terms.isEmpty() || w <= 0f || h <= 0f) return emptyList()
    val maxC = terms.maxOf { it.count }.toFloat()
    val out = ArrayList<Bubble>()
    val cx = w / 2
    val cy = h / 2
    for (t in terms) {
        val r = minR + (maxR - minR) * sqrt(t.count / maxC)
        var a = 0f
        var d = 0f
        var placed = false
        while (d < max(w, h)) {
            val x = cx + d * cos(a) * 1.35f
            val y = cy + d * sin(a)
            val inside = x - r >= 2 && x + r <= w - 2 && y - r >= 2 && y + r <= h - 2
            if (inside && out.none { b -> val dx = b.x - x; val dy = b.y - y; dx * dx + dy * dy < (b.r + r + 5) * (b.r + r + 5) }) {
                out.add(Bubble(t, r, x, y)); placed = true; break
            }
            a += 0.35f
            d += 0.6f
        }
        if (!placed && out.size >= 45) break
    }
    return out
}

@Composable
fun WordsTab(vm: HiloViewModel, book: Book, a: Analysis, onCard: (StudyCard) -> Unit) {
    val c = LocalHilo.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val density = LocalDensity.current
    var phrases by remember { mutableStateOf(false) }
    var sel by remember(a) { mutableStateOf<Term?>(null) }
    var detail by remember(a) { mutableStateOf("") }          // "", donde, contexto, junto, mapa
    var howTo by remember { mutableStateOf(false) }
    var howMany by rememberSaveable { mutableStateOf(1) }      // 0: 25, 1: 45, 2: 80
    var withCommon by rememberSaveable { mutableStateOf(false) }
    var joining by remember { mutableStateOf<Term?>(null) }
    val palette = c.palette(0) + c.lines[4]
    val removed = vm.study.removedFor(book.id)

    // Palabras fijadas (siempre en burbujas) y unidas (alias → palabra principal), guardadas por libro
    var pinned by remember(book.id) { mutableStateOf(vm.motor["pin:${book.id}"].split('\n').filter { it.isNotBlank() }.toSet()) }
    var merged by remember(book.id) {
        mutableStateOf(vm.motor["unir:${book.id}"].split('\n').mapNotNull { l -> l.split('=').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap())
    }
    fun savePins(p: Set<String>) { pinned = p; vm.motor.put("pin:${book.id}", p.joinToString("\n")); vm.tuneVersion++ }
    fun saveMerged(m: Map<String, String>) { merged = m; vm.motor.put("unir:${book.id}", m.entries.joinToString("\n") { "${it.key}=${it.value}" }); vm.tuneVersion++ }

    // Todas las palabras con las uniones aplicadas
    val all = remember(a, merged, removed) {
        val base = a.allWords.ifEmpty { a.terms }
        val sum = LinkedHashMap<String, Term>()
        for (t in base) {
            val k = merged[t.key] ?: t.key
            val cur = sum[k]
            sum[k] = if (cur == null) Term(k, if (k == t.key) t.label else (base.firstOrNull { it.key == k }?.label ?: k), t.count, false)
            else cur.copy(count = cur.count + t.count)
        }
        sum.values.sortedByDescending { it.count }
    }
    val byKey = remember(all) { all.associateBy { it.key } }
    val rankOf = remember(all) { HashMap<String, Int>(all.size * 2).also { m -> all.forEachIndexed { i, t -> m[t.key] = i + 1 } } }
    val n = listOf(25, 45, 80)[howMany]
    val auto = remember(all, removed, withCommon, n) {
        all.filter { it.key !in removed && (withCommon || it.key !in a.common) && it.count >= 2 }
            .sortedByDescending { it.count * (if (withCommon) 1.0 else com.hilo.lector.text.Analyzer.shape(it.key)) }.take(n)
    }
    var wordEdit by remember { mutableStateOf<String?>(null) }
    wordEdit?.let { w ->
        val tune = vm.tuneOf(book.id)
        if (w == "add") ConceptTextDialog("Agregar palabra o frase", "Escribe un concepto que Hilo dejó afuera (por ejemplo «comunismo» o «lucha de clases»). Lo cuenta en todo el texto y lo deja fijado.", "") { v ->
            wordEdit = null
            if (v != null && v.isNotBlank()) {
                vm.study.restoreWord(book.id, v.trim().lowercase())
                vm.saveTune(book.id, tune.copy(extra = (tune.extra + v.trim()).distinctBy { it.lowercase() }, pinned = tune.pinned + v.trim().lowercase()))
                pinned = pinned + v.trim().lowercase()
            }
        } else {
            val k = w.removePrefix("rename:")
            ConceptTextDialog("Cambiar nombre", "Cómo quieres que se llame en las burbujas, el mapa y las tarjetas.", tune.labels[k] ?: byKey[k]?.label ?: k) { v ->
                wordEdit = null
                if (v != null && v.isNotBlank()) vm.saveTune(book.id, tune.copy(labels = tune.labels + (k to v.trim())))
            }
        }
    }
    val bubbleList = if (phrases) a.phrases.filter { it.key !in removed }.take(24)
    else (pinned.mapNotNull { byKey[it] } + auto).distinctBy { it.key }.take(n + pinned.size)
    val inBubbles = remember(bubbleList) { bubbleList.map { it.key }.toHashSet() }

    /** Dónde aparece una palabra (también las comunes), buscando en las oraciones del texto. */
    fun hitsOf(t: Term, limit: Int = 60): List<Pair<Sent, IntRange>> {
        val keys = listOf(t.key) + merged.filterValues { it == t.key }.keys
        val rx = Regex("(?<![\\p{L}\\p{N}])(" + keys.joinToString("|") { Regex.escape(it) } + ")(e?s)?(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
        val out = ArrayList<Pair<Sent, IntRange>>()
        for (s in a.sentences) {
            if (!t.phrase && t.key !in a.common && keys.none { it in s.keys } && !s.text.contains(t.key, true)) continue
            rx.findAll(s.text).forEach { m -> if (out.size < limit) out.add(s to m.range) }
            if (out.size >= limit) break
        }
        return out
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "${"%,d".format(a.totalWords).replace(',', '.')} palabras en total · ${"%,d".format(a.distinct).replace(',', '.')} distintas. Toca una burbuja para ver qué puedes hacer con esa palabra.",
            color = c.muted, fontSize = 13.sp,
        )
        // Cómo elige Hilo las palabras
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.band).clickable { howTo = !howTo }.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text((if (howTo) "▾ " else "▸ ") + "¿Cómo elige Hilo las palabras?", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (howTo) Text(
                "1. Cuenta todas las palabras del documento.\n" +
                    "2. Junta el singular y el plural (sociedad y sociedades).\n" +
                    "3. Deja aparte las palabras comunes que no dicen de qué trata el texto: artículos (el, la), preposiciones (de, en), conectores (que, pero), verbos muy usados (ser, haber) y adverbios en «-mente». No se borran: están en la lista de abajo como «comunes».\n" +
                    "4. Muestra en burbujas las que más se repiten (al menos 2 veces). La burbuja más grande es la más repetida.\n\n" +
                    "Abajo están TODAS las palabras. Toca el ojo de una oculta para fijarla en las burbujas, o activa «Incluir comunes».",
                color = c.ink, fontSize = 13.sp, lineHeight = 18.sp,
            )
        }
        ChoiceRow(listOf("Palabras", "Frases clave"), if (phrases) 1 else 0) { phrases = it == 1; sel = null }
        if (!phrases) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Burbujas", color = c.muted, fontSize = 13.sp)
            listOf("25", "45", "80").forEachIndexed { i, l ->
                Text(
                    l, color = if (i == howMany) c.accentInk else c.ink, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (i == howMany) c.accent else c.chip).clickable { howMany = i }.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text("Comunes", color = c.muted, fontSize = 13.sp, modifier = Modifier.clickable { withCommon = !withCommon })
            Switch(withCommon, { withCommon = it }, modifier = Modifier.padding(start = 4.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("+ Agregar palabra o frase") { wordEdit = "add" }
            Text("¿Falta un concepto? Agrégalo y queda fijado.", color = c.muted, fontSize = 12.sp)
        }

        // Nube de burbujas que flotan
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(if (n >= 80 && !phrases) 480.dp else 400.dp).clip(RoundedCornerShape(20.dp)).background(c.surface)
        ) {
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val bubbles = remember(bubbleList, wPx, hPx) {
                packBubbles(bubbleList, wPx, hPx, with(density) { (if (bubbleList.size > 50) 15.dp else 20.dp).toPx() }, with(density) { 54.dp.toPx() })
            }
            val tr = rememberInfiniteTransition(label = "flotar")
            val t by tr.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "t")
            val still = reduceMotion(ctx)
            val amp = if (still) 0f else with(density) { 4.dp.toPx() }
            if (bubbles.isEmpty()) {
                Text(
                    if (phrases) "No encontré frases que se repitan lo suficiente." else "No hay palabras para mostrar.",
                    color = c.muted, modifier = Modifier.align(Alignment.Center).padding(24.dp), textAlign = TextAlign.Center,
                )
            }
            bubbles.forEachIndexed { i, b ->
                val on = sel?.key == b.term.key
                val pin = b.term.key in pinned
                val col = if (b.term.key in a.common) c.muted else palette[i % palette.size]
                val size = with(density) { (b.r * 2).toDp() }
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (b.x - b.r + cos(t + i * 1.3f) * amp * 0.7f).roundToInt(),
                                (b.y - b.r + sin(t * 1.0f + i) * amp).roundToInt(),
                            )
                        }
                        .size(size).clip(CircleShape)
                        .background(col.copy(alpha = if (sel == null || on) 0.9f else 0.35f))
                        .border(if (on) 3.dp else if (pin) 2.dp else 0.dp, if (on) c.ink else if (pin) Color.White else Color.Transparent, CircleShape)
                        .clickable { sel = if (on) null else b.term; detail = "" },
                    contentAlignment = Alignment.Center,
                ) {
                    val rDp = b.r / density.density
                    // Palabras largas en burbujas chicas («comportamiento»): se parten en dos con guion para que quepan
                    val raw = b.term.label
                    val split = !raw.contains(' ') && raw.length > 6 && rDp * 1.75f / (raw.length * 0.6f) < 9f
                    val label = if (split) raw.substring(0, (raw.length + 1) / 2) + "-\n" + raw.substring((raw.length + 1) / 2) else raw
                    val longest = label.split(' ', '\n').maxOf { it.length }.coerceAtLeast(3)
                    val lines = if (split) 2 else if (raw.contains(' ')) minOf(3, raw.count { ch -> ch == ' ' } + 1) else 1
                    val fs = minOf(rDp * 0.36f, rDp * 1.75f / (longest * 0.6f)).coerceIn(6f, 20f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            label, color = Color.White, fontSize = fs.sp, fontWeight = FontWeight.Bold,
                            maxLines = lines, softWrap = lines > 1, textAlign = TextAlign.Center, lineHeight = (fs * 1.05f).sp,
                            overflow = TextOverflow.Clip, modifier = Modifier.padding(horizontal = 2.dp),
                        )
                        if (b.r > with(density) { 26.dp.toPx() }) Text("${b.term.count}", color = Color.White.copy(alpha = 0.9f), fontSize = (fs * 0.75f).coerceAtLeast(8f).sp)
                    }
                }
            }
        }

        // ---------- La palabra elegida ----------
        sel?.let { term ->
            val pct = term.count * 100f / max(1, a.totalWords)
            val rank = rankOf[term.key] ?: 0
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(term.label, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.ink)
                        val also = merged.filterValues { it == term.key }.keys
                        Text(
                            "${term.count} veces · ${"%.2f".format(pct)} % del texto" + (if (rank > 0 && !term.phrase) " · puesto $rank" else "") +
                                (if (term.key in a.common) " · palabra común" else "") + (if (also.isNotEmpty()) " · incluye: ${also.joinToString(", ")}" else ""),
                            color = c.muted, fontSize = 13.sp,
                        )
                    }
                }
                // Qué hacer con la palabra
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    WordAction("Dónde aparece", detail == "donde") { detail = if (detail == "donde") "" else "donde" }
                    WordAction("En contexto", detail == "contexto") { detail = if (detail == "contexto") "" else "contexto" }
                    WordAction("Va junto a…", detail == "junto") { detail = if (detail == "junto") "" else "junto" }
                    WordAction("En el libro", detail == "mapa") { detail = if (detail == "mapa") "" else "mapa" }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!term.phrase) OutlinedButton(onClick = { savePins(if (term.key in pinned) pinned - term.key else pinned + term.key) }) {
                        Text(if (term.key in pinned) "Soltar" else "Fijar en burbujas", color = c.ink)
                    }
                    if (!term.phrase) OutlinedButton(onClick = { joining = term }) { Text("Unir con…", color = c.ink) }
                    OutlinedButton(onClick = { wordEdit = "rename:" + term.key }) { Text("Cambiar nombre", color = c.ink) }
                    OutlinedButton(onClick = {
                        val s = hitsOf(term, 1).firstOrNull()?.first
                        onCard(StudyCard(0, book.id, "qa", "¿Qué es «${term.label}»?", s?.text.orEmpty(), source = s?.let { "párrafo ${it.para + 1}" }.orEmpty(), sourcePos = s?.para ?: -1, sourceFlow = true, due = today()))
                    }) { Text("Tarjeta", color = c.ink) }
                    OutlinedButton(onClick = {
                        val s = hitsOf(term, 1).firstOrNull()?.first
                        vm.study.putGloss(book.id, Gloss(term.label.replaceFirstChar { it.uppercase() }, s?.text.orEmpty(), s?.para ?: -1), null)
                        vm.toast = "Agregado al glosario."
                    }) { Text("Al glosario", color = c.ink) }
                    OutlinedButton(onClick = {
                        vm.feynman = FeynmanReq(book.id, term.label, hitsOf(term, 3).joinToString(" ") { it.first.text }, emptyList())
                    }) { Text("Explícalo", color = c.ink) }
                    OutlinedButton(onClick = {
                        vm.study.removeWord(book.id, term.key, false); savePins(pinned - term.key); sel = null
                        vm.undoable("«${term.label}» oculta") { vm.study.restoreWord(book.id, term.key) }
                    }) { Text("Ocultar", color = c.lines[0]) }
                    OutlinedButton(onClick = { vm.study.removeWord(book.id, term.key, true); savePins(pinned - term.key); sel = null }) { Text("Ocultar en todos", color = c.lines[0]) }
                }
                LookupRow(term.label, vm.study.langs[book.id].orEmpty())

                when (detail) {
                    "donde" -> hitsOf(term, 12).distinctBy { it.first }.forEach { (s, _) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.text, color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.openAt(book.id, true, s.para) }) { Text("Ir") }
                        }
                    }
                    // Concordancia: la palabra al centro con lo que tiene a cada lado (como en AntConc o MAXQDA)
                    "contexto" -> {
                        Text("Cada línea es una aparición: lo que va antes, la palabra y lo que sigue. Toca para ir.", color = c.muted, fontSize = 12.sp)
                        hitsOf(term, 40).forEach { (s, r) ->
                            val left = s.text.substring(0, r.first).takeLast(34)
                            val right = s.text.substring(r.last + 1).take(34)
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { vm.openAt(book.id, true, s.para) }.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(left, color = c.muted, fontSize = 13.sp, maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                                Text(" ${s.text.substring(r)} ", color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(right, color = c.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    // Las palabras que más salen en la misma oración
                    "junto" -> {
                        val near = remember(term, a) {
                            val cnt = HashMap<String, Int>()
                            hitsOf(term, 400).map { it.first }.distinct().forEach { s -> s.keys.forEach { k -> if (k != term.key && k !in removed && !k.contains(' ')) cnt[k] = (cnt[k] ?: 0) + 1 } }
                            cnt.entries.sortedByDescending { it.value }.take(14)
                        }
                        if (near.isEmpty()) Text("No aparece junto a otras palabras importantes.", color = c.muted, fontSize = 13.sp)
                        else {
                            Text("Palabras que salen en la misma oración (veces juntas). Toca una para verla.", color = c.muted, fontSize = 12.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                near.forEach { (k, v) ->
                                    Text(
                                        "${byKey[k]?.label ?: k} · $v", color = c.ink, fontSize = 13.sp,
                                        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.chip)
                                            .clickable { byKey[k]?.let { sel = it } }.padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                    // En qué partes del documento aparece
                    "mapa" -> {
                        val parts = 24
                        val total = max(1, a.paraCount.takeIf { it > 0 } ?: (a.sentences.maxOfOrNull { it.para } ?: 0) + 1)
                        val paras = remember(term, a, merged) { hitsOf(term, 5000).map { it.first.para } }
                        val bins = remember(paras) {
                            val b = IntArray(parts)
                            paras.forEach { p -> b[(p * parts / total).coerceIn(0, parts - 1)]++ }
                            b
                        }
                        val mx = max(1, bins.maxOrNull() ?: 1)
                        Text("Dónde se concentra en el documento, del inicio al final. Toca una barra para ir a esa parte.", color = c.muted, fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth().height(90.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            bins.forEachIndexed { i, v ->
                                Box(
                                    Modifier.weight(1f).fillMaxHeight((v.toFloat() / mx).coerceAtLeast(0.03f))
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(if (v == 0) c.line else c.accent)
                                        .clickable { vm.openAt(book.id, true, i * total / parts) },
                                )
                            }
                        }
                        Row { Text("Inicio", color = c.muted, fontSize = 11.sp, modifier = Modifier.weight(1f)); Text("Final", color = c.muted, fontSize = 11.sp) }
                        val chs = a.guides.map { it.chapter }.filter { ch -> paras.any { it in ch.startPara until ch.endPara } }
                        if (a.guides.size > 1 && chs.isNotEmpty()) Text("Aparece en: " + chs.take(8).joinToString(" · ") { it.title.take(40) }, color = c.ink, fontSize = 13.sp)
                    }
                }
            }
        }

        // ---------- Todas las palabras ----------
        if (!phrases) {
            var q by rememberSaveable { mutableStateOf("") }
            var show by rememberSaveable { mutableStateOf("todas") }
            var limit by remember { mutableStateOf(80) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TODAS LAS PALABRAS (${all.size})", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val csv = "palabra;veces;porcentaje;común\n" + all.joinToString("\n") { "${it.label};${it.count};${"%.3f".format(it.count * 100f / max(1, a.totalWords))};${if (it.key in a.common) "sí" else "no"}" }
                    share(ctx, csv)
                }) { Text("Exportar") }
            }
            OutlinedTextField(
                q, { q = it; limit = 80 }, placeholder = { Text("Buscar una palabra") }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = c.muted) }, modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("todas" to "Todas", "burbujas" to "En burbujas", "ocultas" to "No están", "comunes" to "Comunes", "quitadas" to "Ocultadas por ti").forEach { (k, l) ->
                    Text(
                        l, color = if (show == k) c.accentInk else c.ink, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (show == k) c.accent else c.chip).clickable { show = k; limit = 80 }.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            val fq = q.trim().lowercase()
            val rows = remember(all, fq, show, inBubbles, removed) { all.filter { t ->
                (fq.isEmpty() || t.key.contains(fq) || t.label.lowercase().contains(fq)) && when (show) {
                    "burbujas" -> t.key in inBubbles
                    "ocultas" -> t.key !in inBubbles && t.key !in removed
                    "comunes" -> t.key in a.common
                    "quitadas" -> t.key in removed
                    else -> true
                }
            }.let { l -> if (show == "todas") l.sortedBy { t -> if (t.key in a.common) 1 else 0 } else l } }
            Text("El ojo dice si la palabra está en las burbujas. Toca el ojo para ponerla o sacarla.", color = c.muted, fontSize = 12.sp)
            val maxC = all.firstOrNull()?.count ?: 1
            rows.take(limit).forEach { term ->
                val rank = rankOf[term.key] ?: 0
                val on = term.key in inBubbles
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { sel = term; detail = "" }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("$rank", color = c.muted, fontSize = 12.sp, modifier = Modifier.width(34.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            term.label + if (term.key in a.common) "  · común" else "", color = if (term.key in removed) c.muted else c.ink,
                            fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Box(Modifier.fillMaxWidth((term.count.toFloat() / maxC).coerceAtLeast(0.01f)).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (on) c.accent else c.line))
                    }
                    Text("${term.count}", color = c.ink, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 10.dp))
                    IconButton(onClick = {
                        when {
                            term.key in removed -> { vm.study.restoreWord(book.id, term.key); savePins(pinned + term.key) }
                            on -> { if (term.key in pinned) savePins(pinned - term.key) else vm.study.removeWord(book.id, term.key, false) }
                            else -> savePins(pinned + term.key)
                        }
                    }) {
                        Icon(
                            if (on) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            if (on) "Sacar ${term.label} de las burbujas" else "Poner ${term.label} en las burbujas",
                            tint = if (on) c.accent else c.muted, modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            if (rows.size > limit) TextButton(onClick = { limit += 200 }) { Text("Ver más (${rows.size - limit} quedan)") }
            if (rows.isEmpty()) Text("No hay palabras aquí.", color = c.muted, fontSize = 13.sp)
        }
    }

    // Unir una palabra con otra (por ejemplo «trabajador» con «trabajo»): cuentan juntas
    joining?.let { from ->
        var q by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { joining = null },
            title = { Text("Unir «${from.label}» con…") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Las dos palabras se cuentan como una sola burbuja. Sirve para variantes: trabajo, trabajador, laboral.", color = c.muted, fontSize = 13.sp)
                    OutlinedTextField(q, { q = it }, placeholder = { Text("Buscar la palabra principal") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    val opts = all.filter { it.key != from.key && it.key !in a.common && (q.isBlank() || it.key.contains(q.trim().lowercase())) }.take(8)
                    opts.forEach { o ->
                        Text(
                            "${o.label} · ${o.count}", color = c.ink, fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                                // Lo que estaba unido a «from» pasa a la nueva palabra principal
                                val m = merged.mapValues { if (it.value == from.key) o.key else it.value } + (from.key to o.key)
                                saveMerged(m); savePins(pinned - from.key); joining = null; sel = null
                                vm.toast = "«${from.label}» ahora cuenta como «${o.label}»."
                            }.padding(vertical = 8.dp, horizontal = 4.dp),
                        )
                    }
                    val mine = merged.filterValues { it == from.key }.keys
                    if (mine.isNotEmpty()) TextButton(onClick = { saveMerged(merged - mine); joining = null }) { Text("Separar las unidas a «${from.label}»") }
                    if (from.key in merged) TextButton(onClick = { saveMerged(merged - from.key); joining = null }) { Text("Separar «${from.label}»") }
                }
            },
            confirmButton = { TextButton(onClick = { joining = null }) { Text("Cerrar") } },
        )
    }
}

@Composable
private fun WordAction(label: String, on: Boolean, onClick: () -> Unit) {
    val c = LocalHilo.current
    Text(
        label, color = if (on) c.accentInk else c.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.accent else c.band).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

// =====================================================================
// Mapa de conceptos (tipo Obsidian)
// =====================================================================

@Composable
fun ConceptMapTab(
    vm: HiloViewModel,
    book: Book?,
    a: Analysis,
    intro: String = "Los conceptos que más se repiten en este documento. El número es cuántas veces aparece; las líneas unen los que salen en la misma oración (más gruesa, más veces juntos). Pellizca para acercar y toca un concepto para ver sus relaciones o quitarlo.",
    panel: (@Composable (sel: Int, neighbors: List<Int>, select: (Int) -> Unit) -> Unit)? = null,
) {
    val c = LocalHilo.current
    val n = a.mapTerms.size
    val pos = remember(a) {
        mutableStateListOf<Offset>().apply {
            for (i in 0 until n) {
                val ang = i * 2.39996f
                val rr = 60f + 14f * sqrt(i.toFloat()) * 6f
                add(Offset(cos(ang) * rr, sin(ang) * rr))
            }
        }
    }
    var run by remember(a) { mutableIntStateOf(0) }
    var sel by remember(a) { mutableStateOf<Int?>(null) }
    var scale by remember(a) { mutableFloatStateOf(1f) }
    var pan by remember(a) { mutableStateOf(Offset.Zero) }
    val measurer = rememberTextMeasurer()
    val maxC = a.mapTerms.maxOfOrNull { it.count }?.toFloat() ?: 1f
    val maxW = a.edges.maxOfOrNull { it.w }?.toFloat() ?: 1f
    fun radius(i: Int) = 10f + 20f * sqrt(a.mapTerms[i].count / maxC)
    /** Radio del concepto en las unidades del mapa: así crece y se achica junto con él y nunca se encima. */
    fun mapR(i: Int) = 1.4f * radius(i)

    // Simulación de fuerzas: los conceptos relacionados se atraen y el resto se separa
    LaunchedEffect(a, run) {
        // Fruchterman-Reingold en un espacio de 800 × 800
        val half = 400f
        val k = sqrt(800f * 800f / max(1, n)) * 0.45f
        var temp = 90f
        repeat(300) {
            val disp = Array(n) { Offset.Zero }
            for (i in 0 until n) for (j in i + 1 until n) {
                val d = pos[i] - pos[j]
                val dist = max(2f, d.getDistance())
                val u = d / dist * (k * k / dist)
                disp[i] += u; disp[j] -= u
            }
            for (e in a.edges) {
                val d = pos[e.a] - pos[e.b]
                val dist = max(2f, d.getDistance())
                val u = d / dist * (dist * dist / k * (0.6f + 0.4f * e.w / maxW))
                disp[e.a] -= u; disp[e.b] += u
            }
            for (i in 0 until n) {
                val dd = disp[i] - pos[i] * 0.6f
                val len = max(1f, dd.getDistance())
                val np = pos[i] + dd / len * min(len, temp)
                pos[i] = Offset(np.x.coerceIn(-half, half), np.y.coerceIn(-half, half))
            }
            temp = max(2f, temp * 0.98f)
            withFrameNanos { }
        }
        // Separa círculos y nombres encimados (cada concepto ocupa su círculo y el nombre debajo)
        val arr = Array(n) { pos[it] }
        untangle(arr, FloatArray(n) { mapR(it) }, FloatArray(n) { a.mapTerms[it].label.length * 13f }, 30f, 800f, 800f, 10f)
        for (i in 0 until n) pos[i] = arr[i]
    }

    /** Encaja todo el mapa en el recuadro: escala y centro según dónde quedaron los conceptos. */
    fun fit(w: Float, h: Float): Pair<Float, Offset> {
        if (pos.isEmpty()) return 1f to Offset.Zero
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for ((i, p) in pos.withIndex()) {
            val hw = max(mapR(i), a.mapTerms.getOrNull(i)?.label?.length?.times(6.5f) ?: 0f)
            minX = min(minX, p.x - hw); maxX = max(maxX, p.x + hw); minY = min(minY, p.y - mapR(i)); maxY = max(maxY, p.y + mapR(i) + 30f)
        }
        val bw = max(80f, maxX - minX)
        val bh = max(80f, maxY - minY)
        val sc = min((w * 0.94f) / bw, (h * 0.94f) / bh)
        return sc to Offset((minX + maxX) / 2f, (minY + maxY) / 2f)
    }

    val neighbors = sel?.let { s -> a.edges.filter { it.a == s || it.b == s }.sortedByDescending { it.w }.map { if (it.a == s) it.b else it.a } }.orEmpty()

    var editing by remember { mutableStateOf<String?>(null) }   // "add", "rename", "merge"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(intro, color = c.muted, fontSize = 13.sp)
        if (book != null) {
            val tune = vm.tuneOf(book.id)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill("+ Agregar concepto") { editing = "add" }
                if (tune.pinned.isNotEmpty() || tune.extra.isNotEmpty() || tune.merged.isNotEmpty() || tune.labels.isNotEmpty()) {
                    Text(
                        listOfNotNull(
                            tune.pinned.size.takeIf { it > 0 }?.let { "$it fijado(s)" },
                            tune.extra.size.takeIf { it > 0 }?.let { "$it agregado(s)" },
                            tune.merged.size.takeIf { it > 0 }?.let { "$it unido(s)" },
                            tune.labels.size.takeIf { it > 0 }?.let { "$it renombrado(s)" },
                        ).joinToString(" · "), color = c.muted, fontSize = 12.sp,
                    )
                    Pill("Deshacer mis cambios") {
                        val old = tune
                        vm.saveTune(book.id, com.hilo.lector.text.Tune())
                        vm.undoable("El mapa volvió a como lo armó Hilo") { vm.saveTune(book.id, old) }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(460.dp).clip(RoundedCornerShape(20.dp)).background(c.surface)) {
            Canvas(
                Modifier.fillMaxSize()
                    .pointerInput(a) {
                        detectTransformGestures { _, p, z, _ ->
                            scale = (scale * z).coerceIn(0.35f, 3.5f)
                            pan += p
                        }
                    }
                    .pointerInput(a) {
                        detectTapGestures { p ->
                            val (base, mid) = fit(size.width.toFloat(), size.height.toFloat())
                            val center = Offset(size.width / 2f, size.height / 2f)
                            var best: Int? = null
                            var bd = Float.MAX_VALUE
                            for (i in 0 until n) {
                                val sp = center + pan + (pos[i] - mid) * base * scale
                                val d = (sp - p).getDistance()
                                if (d < bd) { bd = d; best = i }
                            }
                            sel = if (best != null && bd < 48f * density) (if (sel == best) null else best) else null
                        }
                    }
            ) {
                val (base, mid) = fit(size.width, size.height)
                val center = Offset(size.width / 2f, size.height / 2f)
                fun sp(i: Int) = center + pan + (pos[i] - mid) * base * scale
                for (e in a.edges) {
                    val hi = sel != null && (e.a == sel || e.b == sel)
                    val alpha = if (sel == null) 0.15f + 0.45f * e.w / maxW else if (hi) 0.85f else 0.06f
                    drawLine(if (hi) c.accent else c.ink, sp(e.a), sp(e.b), strokeWidth = (1f + 3f * e.w / maxW) * density, alpha = alpha)
                }
                val pal = c.palette(0)
                for (i in 0 until n) {
                    val t = a.mapTerms[i]
                    val on = sel == i
                    val near = sel == null || on || i in neighbors
                    val r = (mapR(i) * base * scale).coerceIn(5f * density, 40f * density)
                    val col = if (t.phrase) c.accent else pal[i % pal.size]
                    drawCircle(col, r, sp(i), alpha = if (near) 1f else 0.2f)
                    if (on) drawCircle(c.ink, r + 4f, sp(i), alpha = 0.8f, style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
                    // Cuántas veces aparece, dentro del círculo (si cabe)
                    if (r >= 9f * density) {
                        val num = "${t.count}"
                        val fs = (r / density * (if (num.length >= 4) 0.62f else 0.8f)).coerceIn(9f, 18f)
                        val nl = measurer.measure(num, TextStyle(fontSize = fs.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14161C).copy(alpha = if (near) 0.9f else 0.25f)))
                        drawText(nl, topLeft = sp(i) - Offset(nl.size.width / 2f, nl.size.height / 2f))
                    }
                    // El nombre crece y se achica con el mapa: así no se encima con los vecinos
                    val lfs = (13f * base * scale / 0.56f / density / fontScale).coerceIn(7f, 15f) + (if (on) 2f else 0f)
                    val style = TextStyle(fontSize = lfs.sp, color = c.ink.copy(alpha = if (near) 1f else 0.25f), fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                    val lay = measurer.measure(t.label, style)
                    drawText(lay, topLeft = sp(i) + Offset(-lay.size.width / 2f, r + 3f))
                }
            }
            Row(Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                TextButton(onClick = { scale = 1f; pan = Offset.Zero; run++ }) { Text("Reorganizar") }
            }
        }

        if (panel != null) sel?.let { s -> panel(s, neighbors) { sel = it } }
        if (panel == null && book != null) sel?.let { s ->
            val t = a.mapTerms[s]
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.label, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.ink, modifier = Modifier.weight(1f))
                    Text("${t.count} veces", color = c.accent, fontWeight = FontWeight.Bold)
                }
                if (neighbors.isNotEmpty()) {
                    Label("Relacionado con")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        neighbors.forEach { j ->
                            Text(
                                a.mapTerms[j].label, color = c.ink, fontSize = 14.sp,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(c.band).clickable { sel = j }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                Text(
                    "${"%.1f".format(t.count * 100f / max(1, a.totalWords))} % de las ${"%,d".format(a.totalWords).replace(',', '.')} palabras del documento" +
                        (if (neighbors.isNotEmpty()) " · aparece junto a ${neighbors.size} conceptos" else ""),
                    color = c.muted, fontSize = 13.sp,
                )
                LookupRow(t.label, vm.study.langs[book.id].orEmpty())
                val tune = vm.tuneOf(book.id)
                val isPinned = t.key in tune.pinned
                Label("Corregir")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(if (isPinned) "Fijado ✓" else "Fijar en el mapa", isPinned) {
                        vm.saveTune(book.id, tune.copy(pinned = if (isPinned) tune.pinned - t.key else tune.pinned + t.key))
                        vm.toast = if (isPinned) "Ya no está fijado." else "Fijado: siempre va a estar en el mapa y en las burbujas."
                    }
                    Pill("Cambiar nombre") { editing = "rename" }
                    Pill("Unir con otro") { editing = "merge" }
                    Pill("Quitar (no es un concepto)") {
                        vm.study.removeWord(book.id, t.key, false); sel = null
                        vm.undoable("«${t.label}» quitado del mapa") { vm.study.restoreWord(book.id, t.key) }
                    }
                }
                TextButton(onClick = {
                    vm.feynman = FeynmanReq(book.id, t.label, a.sentencesWith(t.key, 3).joinToString(" ") { it.text }, neighbors.take(3).map { a.mapTerms[it].label })
                }) { Text("Explícalo con tus palabras") }
                val other = neighbors.firstOrNull()
                val sents = (if (other != null) a.sentences.filter { t.key in it.keys && a.mapTerms[other].key in it.keys } else emptyList())
                    .ifEmpty { a.sentencesWith(t.key) }.take(3)
                sents.forEach { st ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(st.text, color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.openAt(book.id, true, st.para) }) { Text("Ir") }
                    }
                }
            }
        }
        if (book != null) editing?.let { mode ->
            val tune = vm.tuneOf(book.id)
            val t = sel?.let { a.mapTerms.getOrNull(it) }
            when (mode) {
                "add" -> ConceptTextDialog("Agregar concepto", "Escribe una palabra o frase del texto (por ejemplo «comunismo» o «lucha de clases»). Hilo la cuenta en todo el documento y la pone en el mapa.", "") { v ->
                    editing = null
                    if (v != null && v.isNotBlank()) {
                        val k = v.trim().lowercase()
                        vm.study.restoreWord(book.id, k)
                        vm.saveTune(book.id, tune.copy(extra = (tune.extra + v.trim()).distinctBy { it.lowercase() }, pinned = tune.pinned + k))
                        vm.toast = "Contando «${v.trim()}» en el documento…"
                    }
                }
                "rename" -> if (t != null) ConceptTextDialog("Cambiar nombre", "Cómo quieres que se llame «${t.label}» en el mapa, las burbujas y las tarjetas.", t.label) { v ->
                    editing = null
                    if (v != null && v.isNotBlank()) vm.saveTune(book.id, tune.copy(labels = tune.labels + (t.key to v.trim())))
                } else editing = null
                "merge" -> if (t != null) MergeDialog(a, t) { other ->
                    editing = null
                    if (other != null) {
                        vm.saveTune(book.id, tune.copy(merged = tune.merged + (t.key to other.key), pinned = tune.pinned - t.key))
                        sel = null
                        vm.toast = "«${t.label}» ahora cuenta como «${other.label}»."
                    }
                } else editing = null
            }
        }
        // Tabla de frecuencias, como en los programas de análisis cualitativo (no en el mapa entre documentos)
        if (n > 0 && a.totalWords > 0) {
            var all by remember(a) { mutableStateOf(false) }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Frecuencias", color = c.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("veces · % del texto", color = c.muted, fontSize = 12.sp)
                }
                val order = a.mapTerms.indices.sortedByDescending { a.mapTerms[it].count }
                order.take(if (all) order.size else 10).forEach { i ->
                    val t = a.mapTerms[i]
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (sel == i) c.band else Color.Transparent)
                            .clickable { sel = if (sel == i) null else i }.padding(vertical = 3.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(t.label, color = c.ink, fontSize = 14.sp, maxLines = 1, modifier = Modifier.width(120.dp))
                        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)).background(c.line)) {
                            Box(Modifier.fillMaxWidth(t.count / maxC).fillMaxHeight().clip(RoundedCornerShape(50)).background(if (t.phrase) c.accent else c.palette(0)[i % c.palette(0).size]))
                        }
                        Text("${t.count}", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
                        Text("${"%.1f".format(t.count * 100f / max(1, a.totalWords))}%", color = c.muted, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.width(46.dp))
                    }
                }
                if (order.size > 10) TextButton(onClick = { all = !all }) { Text(if (all) "Ver menos" else "Ver los ${order.size}") }
                Text("Se cuentan juntos el singular y el plural (sociedad y sociedades). No se cuentan artículos, conectores ni palabras como «precisamente».", color = c.muted, fontSize = 12.sp)
            }
        }
    }
}

// =====================================================================
// Glosario
// =====================================================================

@Composable
fun GlossaryTab(vm: HiloViewModel, book: Book, a: Analysis) {
    val c = LocalHilo.current
    val hidden = vm.study.glossHidden[book.id].orEmpty()
    val custom = vm.study.glossCustom[book.id].orEmpty()
    val entries = (a.glossary.filter { it.term.lowercase() !in hidden && custom.none { cu -> cu.term.equals(it.term, true) } } + custom)
        .sortedWith(compareBy({ if (it.para < 0) Int.MAX_VALUE else it.para }, { it.term }))
    var edit by remember { mutableStateOf<Pair<Gloss, String?>?>(null) }   // entrada y término que reemplaza

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Definiciones que Hilo encontró en el texto (oraciones como «La anomia es…» o «Se entiende por…»). Corrígelas, agrega las tuyas y pásalas a tarjetas.",
            color = c.muted, fontSize = 13.sp,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { edit = Gloss("", "", -1) to null }) { Text("+ Agregar término", color = c.ink) }
            if (entries.isNotEmpty()) {
                Button(
                    onClick = {
                        val have = vm.study.cardsOf(book.id).map { it.front }.toSet()
                        var n = 0
                        entries.forEach { g ->
                            val q = "¿Qué es «${g.term}»?"
                            if (q !in have && g.definition.isNotBlank()) { saveCard(vm, book, "qa", q, g.definition, g.para); n++ }
                        }
                        vm.toast = if (n == 0) "Ya tenías todas en tarjetas." else "Creé $n tarjetas."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
                ) { Text("Pasar todo a tarjetas") }
            }
        }
        if (entries.isEmpty()) {
            Text("No encontré definiciones claras. Agrega términos desde aquí o desde la pestaña Palabras.", color = c.ink, fontSize = 14.sp)
        }
        var gq by remember { mutableStateOf("") }
        if (entries.size > 6) OutlinedTextField(
            gq, { gq = it }, placeholder = { Text("Buscar en el glosario") }, singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = c.muted) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
        )
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val lang = vm.study.langs[book.id].orEmpty()
        entries.filter { gq.isBlank() || (it.term + " " + it.definition).contains(gq.trim(), true) }.forEach { g ->
            Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).clip(RoundedCornerShape(18.dp)).background(c.surface)) {
                // Franja de color a la izquierda, como una ficha
                Box(Modifier.width(5.dp).fillMaxHeight().background(c.accent))
                Column(Modifier.weight(1f).padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(g.term, color = c.ink, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f))
                        if (g.para >= 0) Text(
                            "párr. ${g.para + 1} ›", color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(c.band).clickable { vm.openAt(book.id, true, g.para) }.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    // La definición, con el término resaltado
                    val styled = remember(g) {
                        androidx.compose.ui.text.buildAnnotatedString {
                            val t = g.definition
                            val i = t.indexOf(g.term, ignoreCase = true)
                            if (i >= 0) {
                                append(t.substring(0, i))
                                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.SemiBold))
                                append(t.substring(i, i + g.term.length)); pop()
                                append(t.substring(i + g.term.length))
                            } else append(t)
                        }
                    }
                    Text(styled, color = c.ink, fontSize = 16.sp, lineHeight = 24.sp)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlossChip("Tarjeta", primary = true) { saveCard(vm, book, "qa", "¿Qué es «${g.term}»?", g.definition, g.para); vm.toast = "Tarjeta creada." }
                        GlossChip("Explícalo") { vm.feynman = FeynmanReq(book.id, g.term, g.definition, emptyList()) }
                        GlossChip("Diccionario") { lookup(ctx, g.term, "dic", lang) }
                        GlossChip("Wikipedia") { lookup(ctx, g.term, "wiki", lang) }
                        GlossChip("Editar") { edit = g to g.term }
                        IconButton(onClick = {
                            vm.study.hideGloss(book.id, g.term)
                            vm.undoable("«${g.term}» quitado del glosario") { vm.study.putGloss(book.id, g, null) }
                        }) { Icon(Icons.Rounded.Delete, "Quitar del glosario", tint = c.muted, modifier = Modifier.size(20.dp)) }
                    }
                }
            }
        }
    }

    edit?.let { (g, replaces) ->
        var term by remember(g) { mutableStateOf(g.term) }
        var def by remember(g) { mutableStateOf(g.definition) }
        AlertDialog(
            onDismissRequest = { edit = null },
            containerColor = c.surface,
            title = { Text(if (replaces == null) "Nuevo término" else "Editar término") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(term, { term = it }, label = { Text("Término") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(def, { def = it }, label = { Text("Definición") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (term.isNotBlank() && def.isNotBlank()) vm.study.putGloss(book.id, Gloss(term.trim(), def.trim(), g.para), replaces)
                    edit = null
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { edit = null }) { Text("Cancelar") } },
        )
    }
}

// =====================================================================
// Guía de estudio por capítulo
// =====================================================================

@Composable
fun GuideTab(vm: HiloViewModel, book: Book, a: Analysis, onPractice: (String, List<Question>) -> Unit) {
    val c = LocalHilo.current
    val done = vm.study.chapDone[book.id].orEmpty()
    val n = a.guides.size
    val nDone = a.guides.count { it.chapter.startPara in done }
    var open by remember(a) { mutableStateOf(a.guides.firstOrNull { it.chapter.startPara !in done }?.chapter?.startPara) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Guía de estudio", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = c.ink)
            Text("$nDone de $n ${if (n == 1) "capítulo estudiado" else "capítulos estudiados"}. Un capítulo a la vez: lee, mira las ideas clave y practica las preguntas.", color = c.muted, fontSize = 13.sp)
            LinearProgressIndicator(
                progress = { if (n == 0) 0f else nDone.toFloat() / n }, color = c.accent, trackColor = c.chip,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            )
        }
        a.guides.forEach { g -> ChapterCard(vm, book, g, g.chapter.startPara in done, open == g.chapter.startPara, onToggle = {
            open = if (open == g.chapter.startPara) null else g.chapter.startPara
        }, onPractice = onPractice) }
    }
}

@Composable
private fun ChapterCard(
    vm: HiloViewModel, book: Book, g: ChapterGuide, done: Boolean, expanded: Boolean,
    onToggle: () -> Unit, onPractice: (String, List<Question>) -> Unit,
) {
    val c = LocalHilo.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onToggle)) {
            IconButton(onClick = { vm.study.toggleChapter(book.id, g.chapter.startPara) }) {
                Icon(
                    if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    if (done) "Estudiado" else "Marcar como estudiado", tint = if (done) c.accent else c.muted,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(g.chapter.title, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("≈ ${max(1, g.words / 200)} min de lectura · ${g.questions.size} preguntas", color = c.muted, fontSize = 12.sp)
            }
            Text(if (expanded) "▲" else "▼", color = c.muted)
        }
        if (expanded) {
            if (g.concepts.isNotEmpty()) {
                Label("Conceptos")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    g.concepts.forEach { k ->
                        Text(k, color = c.ink, fontSize = 14.sp, modifier = Modifier.clip(RoundedCornerShape(50)).background(c.band).padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            }
            if (g.ideas.isNotEmpty()) {
                Label("Ideas clave")
                g.ideas.forEach { s ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", color = c.accent)
                        Text(s.text, color = c.ink, fontSize = 15.sp, lineHeight = 21.sp)
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.openAt(book.id, true, g.chapter.startPara) }) { Text("Leer", color = c.ink) }
                if (g.questions.isNotEmpty()) {
                    Button(
                        onClick = { onPractice(g.chapter.title, g.questions) },
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
                    ) { Text("Practicar ${g.questions.size} preguntas") }
                    OutlinedButton(onClick = {
                        g.questions.forEach { questionCard(vm, book, it) }
                        vm.toast = "Agregué ${g.questions.size} tarjetas."
                    }) { Text("A tarjetas", color = c.ink) }
                }
                OutlinedButton(onClick = {
                    vm.feynman = FeynmanReq(book.id, g.chapter.title, g.ideas.joinToString(" ") { it.text }, g.concepts)
                }) { Text("Explica el capítulo", color = c.ink) }
            }
        }
    }
}

/** Práctica rápida de las preguntas de un capítulo. */
@Composable
fun QuizOverlay(vm: HiloViewModel, book: Book, title: String, qs: List<Question>, onClose: () -> Unit) {
    val c = LocalHilo.current
    var i by remember(qs) { mutableIntStateOf(0) }
    var show by remember(qs) { mutableStateOf(false) }
    val missed = remember(qs) { mutableStateListOf<Question>() }
    Box(
        Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing).pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Cerrar", tint = c.ink) }
                Text(title, color = c.ink, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${min(i + 1, qs.size)} / ${qs.size}", color = c.muted)
            }
            if (i >= qs.size) {
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${qs.size - missed.size} de ${qs.size}", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 44.sp, color = c.ink)
                    Text(if (missed.isEmpty()) "¡Todas bien! Marca el capítulo como estudiado." else "Las que no sabías conviene repasarlas.", color = c.muted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    if (missed.isNotEmpty()) {
                        Button(
                            onClick = { missed.forEach { questionCard(vm, book, it) }; vm.toast = "Guardé ${missed.size} tarjetas para repasar."; onClose() },
                            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) { Text("Guardar las que no sabía como tarjetas") }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Volver a la guía", color = c.ink) }
                }
                return@Column
            }
            val q = qs[i]
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp).clip(RoundedCornerShape(22.dp)).background(c.surface)
                    .clickable { show = true }.padding(22.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    when (q.kind) { "def" -> "DEFINICIÓN"; "cloze" -> "COMPLETAR"; else -> "EXPLICA CON TUS PALABRAS" },
                    fontSize = 11.sp, letterSpacing = 0.8.sp, color = c.muted,
                )
                Text(q.q, color = c.ink, fontSize = 20.sp, lineHeight = 27.sp)
                if (show) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                    Text(q.a, color = c.accent, fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium)
                    TextButton(onClick = { vm.openAt(book.id, true, q.para) }) { Text("Ver en el texto") }
                } else {
                    Text(if (q.kind == "open") "Dilo en voz alta o escríbelo en Apuntes, luego toca para comparar." else "Piensa la respuesta y toca la tarjeta.", color = c.muted, fontSize = 13.sp)
                }
            }
            if (!show) {
                Button(
                    onClick = { show = true }, modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
                ) { Text("Ver respuesta", fontSize = 17.sp) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { missed.add(q); i++; show = false }, modifier = Modifier.weight(1f).height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.lines[0], contentColor = Color.White),
                    ) { Text("No la sabía") }
                    Button(
                        onClick = { i++; show = false }, modifier = Modifier.weight(1f).height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
                    ) { Text("La sabía") }
                }
            }
        }
    }
}


@Composable
private fun GlossChip(label: String, primary: Boolean = false, onClick: () -> Unit) {
    val c = LocalHilo.current
    Text(
        label, color = if (primary) c.accentInk else c.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (primary) c.accent else c.chip).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}


@Composable
private fun ConceptTextDialog(title: String, help: String, start: String, onDone: (String?) -> Unit) {
    val c = LocalHilo.current
    var v by remember { mutableStateOf(start) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(help, color = c.muted, fontSize = 14.sp)
                androidx.compose.material3.OutlinedTextField(v, { v = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onDone(v) }) { Text("Listo") } },
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Cancelar") } },
    )
}

/** Elegir con qué concepto unir: «estados» con «estado», «marxista» con «marxismo»… */
@Composable
private fun MergeDialog(a: Analysis, t: Term, onDone: (Term?) -> Unit) {
    val c = LocalHilo.current
    var q by remember { mutableStateOf("") }
    val f = com.hilo.lector.text.Smart.fold(q)
    val stem = t.key.take(maxOf(4, t.key.length - 3))
    val list = (a.mapTerms + a.terms + a.phrases).distinctBy { it.key }.filter { it.key != t.key }
        .filter { q.isBlank() || com.hilo.lector.text.Smart.fold(it.label).contains(f) }
        .sortedByDescending { (if (it.key.startsWith(stem) || t.key.startsWith(it.key.take(maxOf(4, it.key.length - 3)))) 100000 else 0) + it.count }
        .take(40)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text("Unir «${t.label}» con…") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sus apariciones se suman al otro y deja de verse por separado.", color = c.muted, fontSize = 13.sp)
                androidx.compose.material3.OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text("Buscar") }, modifier = Modifier.fillMaxWidth())
                Column(Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
                    list.forEach { o ->
                        Row(Modifier.fillMaxWidth().clickable { onDone(o) }.padding(vertical = 10.dp)) {
                            Text(o.label, color = c.ink, modifier = Modifier.weight(1f))
                            Text("${o.count}", color = c.muted)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Cancelar") } },
    )
}
