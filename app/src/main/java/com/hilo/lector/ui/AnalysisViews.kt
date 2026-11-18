@file:OptIn(ExperimentalLayoutApi::class)

package com.hilo.lector.ui

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
    val density = LocalDensity.current
    var phrases by remember { mutableStateOf(false) }
    var sel by remember(a) { mutableStateOf<Term?>(null) }
    var showWhere by remember(a) { mutableStateOf(false) }
    val list = if (phrases) a.phrases else a.terms
    val palette = c.palette(0) + c.lines[4]
    val removed = vm.study.removedFor(book.id)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "${"%,d".format(a.totalWords).replace(',', '.')} palabras · ${"%,d".format(a.distinct).replace(',', '.')} distintas. Toca una burbuja para ver cuántas veces aparece o quitarla si no es relevante.",
            color = c.muted, fontSize = 13.sp,
        )
        ChoiceRow(listOf("Palabras", "Frases clave"), if (phrases) 1 else 0) { phrases = it == 1; sel = null }

        // Nube de burbujas que flotan
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(20.dp)).background(c.surface)
        ) {
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val bubbles = remember(list, wPx, hPx) {
                packBubbles(list.take(if (phrases) 24 else 45), wPx, hPx, with(density) { 20.dp.toPx() }, with(density) { 54.dp.toPx() })
            }
            val tr = rememberInfiniteTransition(label = "flotar")
            val t by tr.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "t")
            val amp = with(density) { 4.dp.toPx() }
            if (bubbles.isEmpty()) {
                Text(
                    if (phrases) "No encontré frases que se repitan lo suficiente." else "No hay palabras para mostrar.",
                    color = c.muted, modifier = Modifier.align(Alignment.Center).padding(24.dp), textAlign = TextAlign.Center,
                )
            }
            bubbles.forEachIndexed { i, b ->
                val on = sel?.key == b.term.key
                val col = palette[i % palette.size]
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
                        .border(if (on) 3.dp else 0.dp, if (on) c.ink else Color.Transparent, CircleShape)
                        .clickable { sel = if (on) null else b.term; showWhere = false },
                    contentAlignment = Alignment.Center,
                ) {
                    // Letra que cabe en una sola línea dentro de la burbuja
                    val rDp = b.r / density.density
                    val longest = b.term.label.split(' ').maxOf { it.length }.coerceAtLeast(3)
                    val lines = if (b.term.phrase && b.term.label.contains(' ')) 2 else 1
                    val fs = minOf(rDp * 0.36f, rDp * 1.75f / (longest * 0.6f)).coerceIn(7f, 20f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            b.term.label, color = Color.White, fontSize = fs.sp, fontWeight = FontWeight.Bold,
                            maxLines = lines, softWrap = lines > 1, textAlign = TextAlign.Center, lineHeight = (fs * 1.05f).sp,
                            overflow = TextOverflow.Clip, modifier = Modifier.padding(horizontal = 2.dp),
                        )
                        if (b.r > with(density) { 34.dp.toPx() }) Text("${b.term.count}", color = Color.White.copy(alpha = 0.85f), fontSize = (fs * 0.75f).sp)
                    }
                }
            }
        }

        // Palabra elegida
        sel?.let { term ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(term.label, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.ink, modifier = Modifier.weight(1f))
                    Text("${term.count} veces", color = c.accent, fontWeight = FontWeight.Bold)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { vm.study.removeWord(book.id, term.key, false); sel = null }) { Text("Quitar", color = c.lines[0]) }
                    OutlinedButton(onClick = { vm.study.removeWord(book.id, term.key, true); sel = null }) { Text("Quitar en todos", color = c.lines[0]) }
                    OutlinedButton(onClick = { showWhere = !showWhere }) { Text(if (showWhere) "Ocultar" else "Dónde aparece", color = c.ink) }
                    OutlinedButton(onClick = {
                        val s = a.sentencesWith(term.key).firstOrNull()
                        vm.study.putGloss(book.id, Gloss(term.label.replaceFirstChar { it.uppercase() }, s?.text.orEmpty(), s?.para ?: -1), null)
                        vm.toast = "Agregado al glosario."
                    }) { Text("Al glosario", color = c.ink) }
                    OutlinedButton(onClick = {
                        val s = a.sentencesWith(term.key).firstOrNull()
                        onCard(StudyCard(0, book.id, "qa", "¿Qué es «${term.label}»?", s?.text.orEmpty(), source = s?.let { "párrafo ${it.para + 1}" }.orEmpty(), sourcePos = s?.para ?: -1, sourceFlow = true, due = today()))
                    }) { Text("Tarjeta", color = c.ink) }
                    OutlinedButton(onClick = {
                        vm.feynman = FeynmanReq(book.id, term.label, a.sentencesWith(term.key, 3).joinToString(" ") { it.text }, emptyList())
                    }) { Text("Explícalo", color = c.ink) }
                }
                LookupRow(term.label, vm.study.langs[book.id].orEmpty())
                if (showWhere) {
                    a.sentencesWith(term.key, 8).forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.text, color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.openAt(book.id, true, s.para) }) { Text("Ir") }
                        }
                    }
                }
            }
        }

        // Lista ordenada para depurar rápido
        Label(if (phrases) "Frases que más se repiten" else "Palabras que más se repiten")
        val maxC = list.firstOrNull()?.count ?: 1
        list.take(60).forEachIndexed { i, term ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { sel = term; showWhere = false }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${i + 1}", color = c.muted, fontSize = 12.sp, modifier = Modifier.width(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(term.label, color = c.ink, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.fillMaxWidth(term.count.toFloat() / maxC).height(4.dp).clip(RoundedCornerShape(2.dp)).background(palette[i % palette.size]))
                }
                Text("${term.count}", color = c.ink, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 10.dp))
                IconButton(onClick = { vm.study.removeWord(book.id, term.key, false) }) {
                    Icon(Icons.Rounded.Close, "Quitar ${term.label}", tint = c.muted, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (removed.isNotEmpty()) {
            Label("Quitadas (toca para devolver)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                removed.sorted().forEach { k ->
                    Text(
                        "$k  ↺", color = c.ink, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.chip).clickable { vm.study.restoreWord(book.id, k) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

// =====================================================================
// Mapa de conceptos (tipo Obsidian)
// =====================================================================

@Composable
fun ConceptMapTab(
    vm: HiloViewModel,
    book: Book?,
    a: Analysis,
    intro: String = "Los conceptos que más se repiten, unidos cuando aparecen en la misma oración. Pellizca para acercar, arrastra para moverte y toca un concepto para ver sus relaciones.",
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
        // Separa los círculos que quedaron encimados (dejando lugar para el nombre)
        repeat(120) {
            var moved = false
            for (i in 0 until n) for (j in i + 1 until n) {
                val need = mapR(i) + mapR(j) + 22f
                val d = pos[i] - pos[j]
                val dist = d.getDistance()
                if (dist < need) {
                    val dir = if (dist < 0.5f) Offset(cos(i * 1.7f), sin(i * 1.7f)) else d / dist
                    val push = dir * ((need - dist) / 2f)
                    pos[i] += push; pos[j] -= push; moved = true
                }
            }
            if (!moved) return@LaunchedEffect
            if (it % 4 == 0) withFrameNanos { }
        }
    }

    /** Encaja todo el mapa en el recuadro: escala y centro según dónde quedaron los conceptos. */
    fun fit(w: Float, h: Float): Pair<Float, Offset> {
        if (pos.isEmpty()) return 1f to Offset.Zero
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (p in pos) { minX = min(minX, p.x); maxX = max(maxX, p.x); minY = min(minY, p.y); maxY = max(maxY, p.y) }
        val bw = max(80f, maxX - minX)
        val bh = max(80f, maxY - minY)
        val sc = min((w * 0.82f) / bw, (h * 0.78f) / bh)
        return sc to Offset((minX + maxX) / 2f, (minY + maxY) / 2f)
    }

    val neighbors = sel?.let { s -> a.edges.filter { it.a == s || it.b == s }.sortedByDescending { it.w }.map { if (it.a == s) it.b else it.a } }.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(intro, color = c.muted, fontSize = 13.sp)
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
                    val style = TextStyle(fontSize = (if (on) 14 else 11).sp, color = c.ink.copy(alpha = if (near) 1f else 0.25f), fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
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
                LookupRow(t.label, vm.study.langs[book.id].orEmpty())
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
        entries.forEach { g ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g.term, color = c.accent, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    TextButton(onClick = { lookup(ctx, g.term, "dic", vm.study.langs[book.id].orEmpty()) }) { Text("Diccionario", fontSize = 12.sp) }
                    TextButton(onClick = { lookup(ctx, g.term, "wiki", vm.study.langs[book.id].orEmpty()) }) { Text("Wikipedia", fontSize = 12.sp) }
                }
                Text(g.definition, color = c.ink, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (g.para >= 0) "párrafo ${g.para + 1}" else "", color = c.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { saveCard(vm, book, "qa", "¿Qué es «${g.term}»?", g.definition, g.para); vm.toast = "Tarjeta creada." }) { Text("Tarjeta") }
                    TextButton(onClick = { vm.feynman = FeynmanReq(book.id, g.term, g.definition, emptyList()) }) { Text("Explícalo") }
                    TextButton(onClick = { edit = g to g.term }) { Text("Editar") }
                    if (g.para >= 0) TextButton(onClick = { vm.openAt(book.id, true, g.para) }) { Text("Ir") }
                    TextButton(onClick = { vm.study.hideGloss(book.id, g.term) }) { Text("Quitar", color = c.lines[0]) }
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
