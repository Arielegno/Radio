@file:OptIn(ExperimentalLayoutApi::class)

package com.hilo.lector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hilo.lector.HiloViewModel
import com.hilo.lector.data.Book
import com.hilo.lector.data.Entry
import com.hilo.lector.data.Ficha
import com.hilo.lector.data.StudyCard
import com.hilo.lector.data.fichaFields
import com.hilo.lector.data.today
import com.hilo.lector.text.Analysis
import com.hilo.lector.text.KeyWords
import com.hilo.lector.text.Law
import com.hilo.lector.text.LawArticle
import com.hilo.lector.text.NormLevels
import com.hilo.lector.text.Plazos
import com.hilo.lector.text.SocioKb
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.random.Random

/** Prácticas de Derecho y de Sociología (se abren desde la ruta de estudio y las secciones). */
@Composable
fun extraPractice(vm: HiloViewModel, book: Book?, a: Analysis?, mode: String, onClose: () -> Unit) {
    when (mode) {
        "derecho" -> LawHub(vm, book, a, onClose)
        "sociologia" -> SocioHub(vm, book, a, onClose)
        "plazo-calc" -> DeadlineCalc(vm, null, onClose)
        "kb-autores" -> OverlayFrame("Autores y paradigmas", onClose) { SocioAuthors(vm, a) }
        else -> OverlayFrame("Práctica", onClose) { Text("Esta práctica no está disponible para este documento.", color = LocalHilo.current.ink) }
    }
}

@Composable
private fun ToolTile(title: String, desc: String, onClick: () -> Unit) {
    val c = LocalHilo.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(title, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(desc, color = c.muted, fontSize = 13.sp)
    }
}

private fun saveFicha(vm: HiloViewModel, book: Book?, kind: String, title: String, values: Map<String, String>) {
    val names = fichaFields(kind)
    vm.study.putFicha(Ficha(vm.study.newId(), book?.id.orEmpty(), kind, title, names.map { it to values[it].orEmpty() }, System.currentTimeMillis()))
}

// =====================================================================
// DERECHO
// =====================================================================

@Composable
private fun LawHub(vm: HiloViewModel, book: Book?, a: Analysis?, onClose: () -> Unit) {
    var sub by remember { mutableStateOf<String?>(null) }
    when (sub) {
        "plazos" -> { DeadlineCalc(vm, a, onClose = { sub = null }); return }
        "silogismo" -> { Syllogism(vm, book, a) { sub = null }; return }
        "casos" -> { StagedCases(vm, book, a) { sub = null }; return }
        "interpretar" -> { Interpret(vm, book, a) { sub = null }; return }
        "mnemo" -> { Mnemonics(vm, book, a) { sub = null }; return }
        "debate" -> { Debate(vm, book, a, null) { sub = null }; return }
        "kelsen" -> { Kelsen(book) { sub = null }; return }
        "cuadro" -> { InstitutionsTable(vm, book, a) { sub = null }; return }
        "oral" -> { LegalOral(vm, book, a) { sub = null }; return }
    }
    OverlayFrame("Métodos para Derecho", onClose) {
        ToolTile("Silogismo jurídico", "Norma (premisa mayor) + hechos (premisa menor) → conclusión. Hilo revisa que cites artículos que existen.") { sub = "silogismo" }
        ToolTile("Casos escalonados", "Fácil (norma directa) → medio (varias normas) → difícil (lagunas, analogía y principios).") { sub = "casos" }
        ToolTile("Simulador de plazos", "Calcula el vencimiento en días hábiles o calendario, con fines de semana y feriados de Honduras.") { sub = "plazos" }
        ToolTile("Interpretar un artículo", "Literal, sistemática, teleológica, histórica, por analogía y a contrario.") { sub = "interpretar" }
        ToolTile("Cuadro de instituciones", "Compara dos figuras: definición, requisitos, efectos y ejemplo.") { sub = "cuadro" }
        ToolTile("Examen oral jurídico", "Preguntas con 2 minutos y rúbrica: norma, hechos, conclusión y jurisprudencia.") { sub = "oral" }
        ToolTile("Debate jurídico", "Dos posturas sobre un caso, con argumentos y contraargumentos.") { sub = "debate" }
        ToolTile("Mnemotecnia de artículos", "Reglas para recordar los elementos de un artículo. Tú eliges la que te sirve.") { sub = "mnemo" }
        ToolTile("Jerarquía de las normas", "La pirámide de Kelsen y dónde está este documento.") { sub = "kelsen" }
        Text("Las fichas de caso (IRAC), de sentencia y de norma están en la sección Fichas.", color = LocalHilo.current.muted, fontSize = 13.sp)
    }
}

/** Elige un artículo del documento (o escribe uno). */
@Composable
private fun ArticlePicker(a: Analysis?, selected: LawArticle?, onPick: (LawArticle) -> Unit) {
    val c = LocalHilo.current
    var q by remember { mutableStateOf("") }
    val law = a?.law
    if (law == null || !law.isLaw) { Text("Este documento no parece una ley o código; escribe el artículo a mano.", color = c.muted, fontSize = 13.sp); return }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(q, { q = it }, label = { Text("Artículo n.º") }, singleLine = true, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { law.find(q.trim())?.let(onPick) }) { Text("Elegir", color = c.ink) }
        OutlinedButton(onClick = { law.articles.randomOrNull()?.let(onPick) }) { Text("Al azar", color = c.ink) }
    }
    selected?.let { Text("${it.label}${if (it.title.isNotBlank()) " · " + it.title else ""}: ${it.text.take(400)}", color = c.ink, fontSize = 14.sp) }
}

@Composable
private fun DeadlineCalc(vm: HiloViewModel, a: Analysis?, onClose: () -> Unit) {
    val c = LocalHilo.current
    var start by remember { mutableStateOf(LocalDate.now()) }
    var amount by remember { mutableStateOf("10") }
    var unit by remember { mutableIntStateOf(0) }
    var picking by remember { mutableStateOf(false) }
    OverlayFrame("Simulador de plazos", onClose) {
        Text("¿Desde cuándo y cuánto? El plazo empieza a correr el día siguiente al hecho o la notificación.", color = c.muted, fontSize = 14.sp)
        a?.law?.deadlines?.take(12)?.takeIf { it.isNotEmpty() }?.let { ds ->
            Label("Plazos de este documento")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ds.take(40).forEach { d -> Pill(d.amount + (d.article?.let { " · art. $it" } ?: "")) { Plazos.parse(d.amount)?.let { (n, u) -> amount = "$n"; unit = u } } }
            }
        }
        OutlinedButton(onClick = { picking = true }) { Text("Fecha del hecho o notificación: ${dayLabel(start.toEpochDay())}", color = c.ink) }
        OutlinedTextField(amount, { amount = it.filter { ch -> ch.isDigit() }.take(4) }, label = { Text("Cantidad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ChoiceRow(listOf("Días hábiles", "Días calendario", "Meses", "Años"), unit) { unit = it }
        // Días inhábiles que declara el Poder Judicial o el feriado movido de la Semana Morazánica de este año
        var extraRaw by remember { mutableStateOf(vm.motor["inhabiles"]) }
        val extra = extraRaw.split(',').mapNotNull { it.trim().toLongOrNull()?.let { d -> LocalDate.ofEpochDay(d) } }.toSet()
        var addingDay by remember { mutableStateOf(false) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("+ Día inhábil") { addingDay = true }
            extra.sorted().forEach { d ->
                Pill("${dayLabel(d.toEpochDay())} ✕") {
                    extraRaw = (extra - d).joinToString(",") { it.toEpochDay().toString() }; vm.motor.put("inhabiles", extraRaw)
                }
            }
        }
        if (extra.isEmpty()) Text("¿El Poder Judicial declaró un día inhábil, o la Semana Morazánica cae otro día este año? Agrégalo y no se cuenta.", color = c.muted, fontSize = 12.sp)
        if (addingDay) ExamDatePicker(LocalDate.now().toEpochDay(), onPick = { d ->
            extraRaw = (extra.map { it.toEpochDay() } + d).distinct().joinToString(","); vm.motor.put("inhabiles", extraRaw)
        }) { addingDay = false }
        val n = amount.toIntOrNull() ?: 0
        if (n > 0) {
            val r = remember(start, n, unit, extra) { Plazos.compute(start, n, unit, extra) }
            Panel {
                Text("Vence: ${dayLabel(r.end.toEpochDay())} de ${r.end.year}", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = c.ink)
                val left = r.end.toEpochDay() - LocalDate.now().toEpochDay()
                Text(if (left >= 0) "Faltan $left días desde hoy." else "Venció hace ${-left} días.", color = if (left in 0..3) c.lines[0] else c.muted, fontSize = 14.sp)
                if (r.skipped.isNotEmpty()) {
                    Label("Días que no se cuentan")
                    r.skipped.filter { !it.second.startsWith("fin de semana") }.take(10).forEach { (d, why) -> Text("• ${dayLabel(d.toEpochDay())}: $why", color = c.ink, fontSize = 13.sp) }
                    val weekends = r.skipped.count { it.second.startsWith("fin de semana") }
                    if (weekends > 0) Text("• $weekends días de fin de semana", color = c.ink, fontSize = 13.sp)
                }
                // Guardar el vencimiento en el calendario de una clase: el recordatorio diario lo avisa en la semana
                var naming by remember { mutableStateOf(false) }
                if (left >= 0) OutlinedButton(onClick = { naming = true }) { Text("Guardar en el calendario", color = c.ink) }
                if (naming) SaveDeadlineDialog(vm, r.end.toEpochDay(), "Vence plazo de $n " + listOf("días hábiles", "días", "meses", "años")[unit]) { naming = false }
            }
        }
        Panel {
            Text("Prescripción y caducidad", color = c.ink, fontWeight = FontWeight.Bold)
            Text("La prescripción se puede interrumpir (vuelve a empezar, por ejemplo con una demanda) o suspender (se detiene y luego sigue). La caducidad corre sin pausas: si vence, el derecho se pierde.", color = c.ink, fontSize = 14.sp)
            Text("Este cálculo es orientativo: revisa siempre la ley procesal aplicable y los días inhábiles que declare el Poder Judicial.", color = c.muted, fontSize = 12.sp)
        }
    }
    if (picking) ExamDatePicker(start.toEpochDay(), onPick = { start = LocalDate.ofEpochDay(it) }) { picking = false }
}

@Composable
private fun Syllogism(vm: HiloViewModel, book: Book?, a: Analysis?, onClose: () -> Unit) {
    val c = LocalHilo.current
    var art by remember { mutableStateOf<LawArticle?>(null) }
    var major by remember { mutableStateOf("") }
    var minor by remember { mutableStateOf("") }
    var concl by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    OverlayFrame("Silogismo jurídico", onClose) {
        Text("Premisa mayor: la norma. Premisa menor: los hechos del caso. Conclusión: la consecuencia jurídica.", color = c.muted, fontSize = 14.sp)
        ArticlePicker(a, art) { art = it; major = "Según el artículo ${it.num}: " + it.text.take(300) }
        OutlinedTextField(major, { major = it; checked = false }, label = { Text("1. Premisa mayor (norma)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(minor, { minor = it; checked = false }, label = { Text("2. Premisa menor (hechos)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(concl, { concl = it; checked = false }, label = { Text("3. Conclusión") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { checked = true }, enabled = major.isNotBlank() && minor.isNotBlank() && concl.isNotBlank()) { Text("Revisar", color = c.ink) }
        if (checked) Panel {
            val refs = Law.refsIn(major)
            val law = a?.law
            when {
                refs.isEmpty() -> Text("✗ La premisa mayor no cita un artículo. Escribe «artículo N».", color = c.lines[0])
                law != null && law.isLaw -> refs.take(60).forEach { r ->
                    val ok = law.find(r) != null
                    Text(if (ok) "✓ El artículo $r existe en este documento." else "? El artículo $r no está en este documento (puede ser de otra ley).", color = if (ok) c.lines[1] else c.lines[2])
                }
                else -> Text("✓ Citas el artículo ${refs.joinToString()}.", color = c.lines[1])
            }
            val k = KeyWords.of(major, emptyList(), 6)
            val (hit, _) = KeyWords.coverage(minor + " " + concl, k)
            Text(
                if (hit.size >= 2) "✓ Los hechos y la conclusión usan los elementos de la norma (${hit.joinToString()})."
                else "✗ Los hechos casi no se conectan con los elementos de la norma. ¿Cada requisito del artículo aparece en los hechos?",
                color = if (hit.size >= 2) c.lines[1] else c.lines[2],
            )
            Text(if (Regex("(?i)por (lo )?tanto|en consecuencia|procede|corresponde|debe").containsMatchIn(concl)) "✓ La conclusión dice qué consecuencia jurídica se aplica." else "Consejo: la conclusión debería decir qué procede («por lo tanto, corresponde…»).", color = c.ink, fontSize = 14.sp)
            TextButton(onClick = {
                saveFicha(vm, book, "irac", "Caso: " + minor.take(40), mapOf(
                    fichaFields("irac")[0] to "¿Se aplica ${art?.label ?: "la norma"} a estos hechos?",
                    fichaFields("irac")[1] to major, fichaFields("irac")[2] to minor, fichaFields("irac")[3] to concl,
                ))
                vm.toast = "Guardado como ficha IRAC."
            }) { Text("Guardar como ficha IRAC") }
        }
    }
}

@Composable
private fun StagedCases(vm: HiloViewModel, book: Book?, a: Analysis?, onClose: () -> Unit) {
    val c = LocalHilo.current
    val law = a?.law
    var level by remember { mutableIntStateOf(0) }
    var seed by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    var answer by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    OverlayFrame("Casos escalonados", onClose) {
        ChoiceRow(listOf("Fácil", "Medio", "Difícil"), level) { level = it; picked = null; answer = ""; reveal = false }
        if (law == null || law.articles.size < 4) { Text("Para generar casos necesito una ley o un código con artículos.", color = c.ink); return@OverlayFrame }
        val r = Random(seed * 31 + level)
        val withTitle = law.articles.filter { it.title.isNotBlank() }.ifEmpty { law.articles }
        when (level) {
            0 -> {
                val target = withTitle[r.nextInt(withTitle.size)]
                val opts = (withTitle.filter { it != target }.shuffled(r).take(3) + target).shuffled(r)
                Text("Un caso trata sobre «${target.title.ifBlank { KeyWords.of(target.text, emptyList(), 2).joinToString(" y ") }}». ¿Qué artículo lo regula?", color = c.ink, fontSize = 18.sp)
                opts.forEach { o ->
                    val col = when { picked == null -> c.surface; o.num == target.num -> c.lines[1].copy(alpha = 0.35f); o.num == picked -> c.lines[0].copy(alpha = 0.3f); else -> c.surface }
                    Text("${o.label} · ${o.text.take(90)}…", color = c.ink, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(col).border(1.dp, c.line, RoundedCornerShape(12.dp)).clickable(enabled = picked == null) { picked = o.num }.padding(12.dp))
                }
            }
            1 -> {
                // Dos artículos que comparten conceptos: concurso de normas
                val pairs = remember(law, seed) {
                    val arts = law.articles.shuffled(r).take(80)
                    var best: Pair<LawArticle, LawArticle>? = null; var bestN = 0
                    for (i in arts.indices) for (j in i + 1 until arts.size) {
                        val n = KeyWords.coverage(arts[j].text, KeyWords.of(arts[i].text, emptyList(), 6)).first.size
                        if (n > bestN) { bestN = n; best = arts[i] to arts[j] }
                    }
                    best
                }
                val p = pairs ?: return@OverlayFrame
                val shared = KeyWords.coverage(p.second.text, KeyWords.of(p.first.text, emptyList(), 6)).first
                Text("Un caso involucra «${shared.take(3).joinToString(", ")}». Dos normas parecen aplicar. ¿Cuáles y cómo se relacionan (una general y una especial, una remite a la otra, se aplican juntas)?", color = c.ink, fontSize = 17.sp)
                OutlinedTextField(answer, { answer = it }, minLines = 4, modifier = Modifier.fillMaxWidth())
                if (!reveal) OutlinedButton(onClick = { reveal = true }) { Text("Ver los artículos", color = c.ink) }
                else listOf(p.first, p.second).forEach { Text("${it.label}: ${it.text.take(350)}", color = c.ink, fontSize = 14.sp) }
            }
            else -> {
                val topic = KeyWords.of(law.articles[r.nextInt(law.articles.size)].text, emptyList(), 1).firstOrNull() ?: "este tema"
                Text("Laguna: aparece un caso sobre «$topic» que la ley no resuelve directamente (por ejemplo, con una tecnología nueva). ¿Cómo lo resolverías?", color = c.ink, fontSize = 17.sp)
                Text("Opciones: analogía con un artículo parecido · principios generales del derecho · la Constitución · la jurisprudencia · la costumbre.", color = c.muted, fontSize = 13.sp)
                OutlinedTextField(answer, { answer = it }, minLines = 5, modifier = Modifier.fillMaxWidth())
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (level > 0 && answer.isNotBlank()) OutlinedButton(onClick = {
                vm.motor.add(Entry(vm.motor.newId(), book?.id.orEmpty(), "caso", listOf("Fácil", "Medio", "Difícil")[level], answer, System.currentTimeMillis()))
                vm.toast = "Guardado."
            }) { Text("Guardar respuesta", color = c.ink) }
            TextButton(onClick = { seed++; picked = null; answer = ""; reveal = false }) { Text("Otro caso") }
        }
    }
}

@Composable
private fun Interpret(vm: HiloViewModel, book: Book?, a: Analysis?, onClose: () -> Unit) {
    val c = LocalHilo.current
    var art by remember { mutableStateOf<LawArticle?>(null) }
    val methods = listOf(
        "Literal" to "¿Qué dicen exactamente las palabras? ¿Hay alguna palabra con dos sentidos?",
        "Sistemática" to "¿Cómo se entiende junto con los artículos que lo rodean y con los que remite?",
        "Teleológica" to "¿Qué finalidad o bien jurídico quiere proteger?",
        "Histórica" to "¿Por qué se creó? ¿Qué problema quería resolver en su momento?",
        "Por analogía" to "¿A qué caso parecido, no previsto, podría extenderse?",
        "A contrario" to "Si dice esto para un caso, ¿qué se deduce para el caso contrario?",
    )
    val answers = remember { mutableStateListOf("", "", "", "", "", "") }
    OverlayFrame("Interpretar un artículo", onClose) {
        ArticlePicker(a, art) { art = it }
        art?.let { at -> if (at.refs.isNotEmpty()) Text("Remite a: " + at.refs.joinToString { "art. $it" }, color = c.muted, fontSize = 13.sp) }
        methods.forEachIndexed { i, (m, q) ->
            Text(m, color = c.accent, fontWeight = FontWeight.Bold)
            Text(q, color = c.ink, fontSize = 14.sp)
            OutlinedTextField(answers[i], { answers[i] = it }, minLines = 2, modifier = Modifier.fillMaxWidth())
        }
        OutlinedButton(onClick = {
            vm.motor.add(Entry(vm.motor.newId(), book?.id.orEmpty(), "interpretacion", art?.label ?: "Artículo", methods.mapIndexed { i, (m, _) -> "$m: ${answers[i]}" }.joinToString("\n"), System.currentTimeMillis()))
            vm.toast = "Interpretación guardada."
        }, enabled = answers.count { it.isNotBlank() } >= 2) { Text("Guardar", color = c.ink) }
    }
}

@Composable
private fun Mnemonics(vm: HiloViewModel, book: Book?, a: Analysis?, onClose: () -> Unit) {
    val c = LocalHilo.current
    var art by remember { mutableStateOf<LawArticle?>(null) }
    OverlayFrame("Mnemotecnia", onClose) {
        Text("Elige un artículo con varios elementos. Hilo propone reglas; quédate con la que te sirva y edítala.", color = c.muted, fontSize = 14.sp)
        ArticlePicker(a, art) { art = it }
        val at = art ?: return@OverlayFrame
        val keys = remember(at) { KeyWords.of(at.text, emptyList(), 6) }
        val acr = keys.joinToString("") { it.first().uppercase() }
        val key = "mnemo:${book?.id}:${at.num}"
        var mine by remember(at) { mutableStateOf(vm.motor[key]) }
        Panel {
            Text("Palabras clave: " + keys.joinToString(" · "), color = c.ink, fontSize = 14.sp)
            Text("Sigla: $acr", color = c.accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Frase: inventa una oración cuyas palabras empiecen con $acr (por ejemplo: «${keys.joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }}»).", color = c.ink, fontSize = 14.sp)
            Text("Número: asocia el ${at.num} a algo que conozcas (una fecha, una dirección, un precio).", color = c.ink, fontSize = 14.sp)
            Text("Imagen: imagina una escena exagerada con «${keys.firstOrNull() ?: ""}».", color = c.ink, fontSize = 14.sp)
        }
        OutlinedTextField(mine, { mine = it; vm.motor.put(key, it) }, label = { Text("Mi regla para el ${at.label}") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        if (mine.isNotBlank()) TextButton(onClick = {
            val card = vm.study.cards.firstOrNull { it.bookId == book?.id && it.source == "art. ${at.num}" }
            if (card != null) { vm.study.update(card.copy(back = card.back + "\n\nPara recordarlo: " + mine)); vm.toast = "Agregada a la tarjeta del artículo." }
            else if (book != null) { lawCard(vm, book, at); vm.study.cards.firstOrNull { it.bookId == book.id && it.source == "art. ${at.num}" }?.let { vm.study.update(it.copy(back = it.back + "\n\nPara recordarlo: " + mine)) }; vm.toast = "Tarjeta creada con tu regla." }
        }) { Text("Agregar a la tarjeta del artículo") }
    }
}

/** Debate: dos posturas con argumentos y contraargumentos (jurídico o sociológico). */
@Composable
private fun Debate(vm: HiloViewModel, book: Book?, a: Analysis?, preset: Pair<String, Pair<String, String>>?, onClose: () -> Unit) {
    val c = LocalHilo.current
    var topic by remember { mutableStateOf(preset?.first.orEmpty()) }
    var aName by remember { mutableStateOf(preset?.second?.first ?: "Postura A") }
    var bName by remember { mutableStateOf(preset?.second?.second ?: "Postura B") }
    val f = remember { mutableStateListOf("", "", "", "", "") }
    OverlayFrame("Debate", onClose) {
        Text("Defender las dos posturas entrena el examen oral y el ensayo: entiendes el tema desde los dos lados.", color = c.muted, fontSize = 14.sp)
        OutlinedTextField(topic, { topic = it }, label = { Text("Tema o caso") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(aName, { aName = it }, label = { Text("Postura A") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(bName, { bName = it }, label = { Text("Postura B") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        listOf("Argumentos de $aName", "Argumentos de $bName", "Qué responde $bName a $aName", "Qué responde $aName a $bName", "Mi conclusión").forEachIndexed { i, l ->
            OutlinedTextField(f[i], { f[i] = it }, label = { Text(l) }, minLines = 2, modifier = Modifier.fillMaxWidth())
        }
        Button(onClick = {
            vm.motor.add(Entry(vm.motor.newId(), book?.id.orEmpty(), "debate", topic.ifBlank { "Debate" }, "$aName: ${f[0]}\n$bName: ${f[1]}\nRéplica de $bName: ${f[2]}\nRéplica de $aName: ${f[3]}\nConclusión: ${f[4]}", System.currentTimeMillis()))
            vm.toast = "Debate guardado."; onClose()
        }, enabled = topic.isNotBlank() && f[0].isNotBlank() && f[1].isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("Guardar") }
        val old = vm.motor.entriesOf("debate").take(5)
        if (old.isNotEmpty()) { Label("Debates guardados"); old.take(20).forEach { Text("• ${it.title}", color = c.ink, fontSize = 14.sp) } }
    }
}

@Composable
private fun Kelsen(book: Book?, onClose: () -> Unit) {
    val c = LocalHilo.current
    val here = book?.let { NormLevels.levelOf(it.title) } ?: -1
    OverlayFrame("Jerarquía de las normas", onClose) {
        Text("Pirámide de Kelsen: una norma no puede contradecir a las de arriba. Si lo hace, se aplica la superior.", color = c.muted, fontSize = 14.sp)
        NormLevels.levels.forEachIndexed { i, (name, _) ->
            val w = 0.45f + 0.11f * i
            val on = i == here
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    name + if (on) "  ← este documento" else "", color = if (on) c.accentInk else c.ink, fontSize = 14.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth(w.coerceAtMost(1f)).clip(RoundedCornerShape(10.dp)).background(if (on) c.accent else c.band).padding(10.dp),
                )
            }
        }
        if (here < 0) Text("No reconocí el tipo de norma por el título del documento.", color = c.muted, fontSize = 13.sp)
    }
}

@Composable
private fun InstitutionsTable(vm: HiloViewModel, book: Book?, a: Analysis?, onClose: () -> Unit) {
    val c = LocalHilo.current
    var x by remember { mutableStateOf("") }
    var y by remember { mutableStateOf("") }
    val rows = listOf("Definición" to listOf("es ", "se entiende", "consiste", "se define"), "Requisitos o elementos" to listOf("requisito", "elemento", "deberá", "debe", "se requiere"), "Efectos" to listOf("efecto", "produce", "consecuencia", "tendrá"), "Ejemplo" to listOf("por ejemplo", "como cuando", "caso"))
    OverlayFrame("Cuadro de instituciones", onClose) {
        Text("Escribe dos figuras para comparar (por ejemplo: dolo y culpa, compraventa y permuta). Hilo busca en el texto lo que dice de cada una; puedes completarlo.", color = c.muted, fontSize = 14.sp)
        a?.let { an -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            an.terms.take(12).forEach { t -> Pill(t.label) { if (x.isBlank()) x = t.label else y = t.label } }
        } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(x, { x = it }, label = { Text("Figura 1") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(y, { y = it }, label = { Text("Figura 2") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        if (x.isBlank() || y.isBlank() || a == null) return@OverlayFrame
        fun find(term: String, hints: List<String>): String {
            val t = term.lowercase()
            return a.sentences.filter { it.text.lowercase().contains(t) }.firstOrNull { s -> hints.any { s.text.lowercase().contains(it) } }?.text ?: "—"
        }
        val cells = remember(x, y) { rows.map { (r, h) -> Triple(r, find(x, h), find(y, h)) } }
        val text = StringBuilder("Cuadro: $x / $y\n\n")
        cells.forEach { (r, cx, cy) ->
            Text(r, color = c.accent, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(cx, color = c.ink, fontSize = 13.sp, modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(c.surface).padding(8.dp))
                Text(cy, color = c.ink, fontSize = 13.sp, modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(c.surface).padding(8.dp))
            }
            text.append("■ $r\n  $x: $cx\n  $y: $cy\n\n")
        }
        val ctx = LocalContext.current
        Row {
            TextButton(onClick = { vm.motor.add(Entry(vm.motor.newId(), book?.id.orEmpty(), "cuadro", "$x / $y", text.toString(), System.currentTimeMillis())); vm.toast = "Cuadro guardado." }) { Text("Guardar") }
            TextButton(onClick = { shareText(ctx, "Cuadro $x / $y", text.toString()) }) { Text("Compartir") }
        }
    }
}

@Composable
private fun LegalOral(vm: HiloViewModel, book: Book?, a: Analysis?, onClose: () -> Unit) {
    val c = LocalHilo.current
    var seed by remember { mutableIntStateOf(0) }
    var left by remember { mutableIntStateOf(120) }
    var running by remember { mutableStateOf(false) }
    val rubric = remember { mutableStateListOf(false, false, false, false) }
    var said by remember { mutableStateOf("") }
    val dictate = rememberDictation(vm, "es") { said = if (said.isBlank()) it else "$said $it" }
    LaunchedEffect(running) { while (running && left > 0) { delay(1000); left-- }; running = false }
    val r = Random(seed)
    val terms = a?.terms?.take(14).orEmpty()
    val x = terms.getOrNull(r.nextInt(maxOf(1, terms.size)))?.label ?: "la institución"
    val y = terms.getOrNull(r.nextInt(maxOf(1, terms.size)))?.label?.takeIf { it != x } ?: "otra figura"
    val art = a?.law?.articles?.takeIf { it.isNotEmpty() }?.let { it[r.nextInt(it.size)] }
    val questions = listOf(
        "Explique la diferencia entre «$x» y «$y».",
        "Defina «$x» y diga en qué artículo se regula.",
        art?.let { "Resuelva en 2 minutos: una persona incumple lo que dice el ${it.label}. ¿Qué consecuencias tiene?" } ?: "Resuelva en 2 minutos un caso donde se aplique «$x».",
        "¿Qué opina la doctrina o la jurisprudencia sobre «$x»?",
    )
    val q = questions[r.nextInt(questions.size)]
    OverlayFrame("Examen oral jurídico", onClose) {
        Text(q, color = c.ink, fontSize = 20.sp, lineHeight = 27.sp)
        Text("%d:%02d".format(left / 60, left % 60), fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = if (left < 20) c.lines[0] else c.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { running = !running }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text(if (running) "Pausa" else "Empezar 2 min") }
            OutlinedButton(onClick = dictate) { Text("Responder hablando", color = c.ink) }
        }
        if (said.isNotBlank()) Text("Dijiste: «$said»", color = c.ink, fontSize = 14.sp)
        Label("Rúbrica: ¿tu respuesta…?")
        listOf("Identifica la norma o el artículo", "Aplica la norma a los hechos", "Llega a una conclusión clara", "Cita jurisprudencia o doctrina").forEachIndexed { i, l ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { rubric[i] = !rubric[i] }) {
                Checkbox(checked = rubric[i], onCheckedChange = { rubric[i] = it }); Text(l, color = c.ink, fontSize = 14.sp)
            }
        }
        Row {
            TextButton(onClick = {
                vm.motor.add(Entry(vm.motor.newId(), book?.id.orEmpty(), "oral", q, said, System.currentTimeMillis(), rubric.count { it } * 25))
                seed++; left = 120; running = false; said = ""; for (i in 0 until 4) rubric[i] = false
                vm.toast = "Guardado. Otra pregunta."
            }) { Text("Guardar y otra") }
            TextButton(onClick = { seed++; left = 120; running = false; said = "" }) { Text("Saltar") }
        }
    }
}

// =====================================================================
// SOCIOLOGÍA
// =====================================================================

@Composable
private fun SocioHub(vm: HiloViewModel, book: Book?, a: Analysis?, onClose: () -> Unit) {
    var sub by remember { mutableStateOf<String?>(null) }
    when (sub) {
        "mills" -> { Mills(vm, book, a) { sub = null }; return }
        "diario" -> { Diary(vm, book, a) { sub = null }; return }
        "ensayo" -> { Essay(vm, book, a) { sub = null }; return }
        "debate" -> { Debate(vm, book, a, "La acción social" to ("Weber" to "Marx")) { sub = null }; return }
        "autores" -> { OverlayFrame("Autores y paradigmas", { sub = null }) { SocioAuthors(vm, a) }; return }
        "escuelas" -> { Schools { sub = null }; return }
    }
    OverlayFrame("Métodos para Sociología", onClose) {
        ToolTile("Imaginación sociológica", "De una noticia a una pregunta sociológica y una hipótesis, paso a paso (Wright Mills).") { sub = "mills" }
        ToolTile("Diario sociológico", "Escenas breves de tu vida explicadas con un concepto. El mejor método para fijar la teoría.") { sub = "diario" }
        ToolTile("Ensayo guiado", "Tesis → argumentos → evidencia → contraargumento → conclusión, con tus citas.") { sub = "ensayo" }
        ToolTile("Autores y paradigmas", "Cuadro comparativo profundo y el mapa de paradigmas, con los autores de este texto resaltados.") { sub = "autores" }
        ToolTile("Escuelas", "Frankfurt, Chicago, Annales, Birmingham, dependencia y pensamiento decolonial.") { sub = "escuelas" }
        ToolTile("Debates cruzados", "Por ejemplo: Weber y Marx sobre la acción social, con argumentos y réplicas.") { sub = "debate" }
        Text("Las fichas de autor, de concepto, de método y de texto clásico están en la sección Fichas.", color = LocalHilo.current.muted, fontSize = 13.sp)
    }
}

/** Autores de la base de conocimiento: mapa de paradigmas y cuadro comparativo profundo. */
@Composable
fun SocioAuthors(vm: HiloViewModel, a: Analysis?) {
    val c = LocalHilo.current
    val inText = a?.authors?.map { it.key }?.toSet().orEmpty()
    var sel by remember { mutableStateOf(SocioKb.authors.filter { it.key in inText }.map { it.key }.take(3).ifEmpty { listOf("marx", "weber", "durkheim") }) }
    Text("Mapa de paradigmas", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    Text("Los autores de este documento llevan ●. Toca para compararlos (hasta 5).", color = c.muted, fontSize = 13.sp)
    SocioKb.paradigms.forEach { p ->
        Panel {
            Text(p.name, color = c.accent, fontWeight = FontWeight.Bold)
            Text(p.idea, color = c.ink, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                p.authors.mapNotNull { SocioKb.find(it) }.forEach { au ->
                    val on = au.key in sel
                    Text(
                        au.name.substringAfterLast(' ') + if (au.key in inText) " ●" else "", fontSize = 13.sp,
                        color = if (on) c.accentInk else c.ink,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.accent else c.band)
                            .clickable { sel = if (on) sel - au.key else (sel + au.key).takeLast(5) }.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
    val chosen = sel.mapNotNull { SocioKb.find(it) }
    if (chosen.isEmpty()) return
    Text("Cuadro comparativo", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    val ctx = LocalContext.current
    val out = StringBuilder("Cuadro comparativo: " + chosen.joinToString(" / ") { it.name } + "\n\n")
    // Con 4 o 5 autores las columnas no entran: el cuadro se desliza de lado
    val wide = chosen.size > 3
    val hs = rememberScrollState()
    SocioKb.compareRows.forEach { (row, get) ->
        Text(row, color = c.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(
            (if (wide) Modifier.horizontalScroll(hs) else Modifier.fillMaxWidth()).height(androidx.compose.foundation.layout.IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            chosen.forEach { au ->
                Column((if (wide) Modifier.width(150.dp) else Modifier.weight(1f)).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(c.surface).padding(8.dp)) {
                    Text(au.name.substringAfterLast(' '), color = c.muted, fontSize = 11.sp)
                    Text(get(au), color = c.ink, fontSize = 13.sp)
                }
            }
        }
        out.append("■ $row\n"); chosen.forEach { out.append("  ${it.name}: ${get(it)}\n") }; out.append("\n")
    }
    if (wide) Text("Desliza el cuadro de lado para ver todos los autores.", color = c.muted, fontSize = 12.sp)
    // Lo que dice este texto de cada autor
    a?.authors?.filter { au -> chosen.any { it.key == au.key } }?.forEach { au ->
        au.sentences.firstOrNull()?.let { Text("En este texto, ${au.name}: «${it.text.take(220)}»", color = c.ink, fontSize = 13.sp) }
    }
    Row {
        TextButton(onClick = {
            val rows = listOf(listOf("") + chosen.map { it.name }) + SocioKb.compareRows.map { (row, get) -> listOf(row) + chosen.map { get(it) } }
            com.hilo.lector.data.DocExport.shareTable(ctx, "Cuadro comparativo", listOf(chosen.joinToString(" · ") { "${it.name} (${it.years})" }), rows)
        }) { Text("Word") }
        TextButton(onClick = { shareText(ctx, "Cuadro comparativo", out.toString()) }) { Text("Texto") }
        TextButton(onClick = {
            chosen.forEach { au ->
                vm.study.add(StudyCard(vm.study.newId(), "", "qa", "¿Qué entiende ${au.name} por sociedad y cómo cambia?", au.society + "\n" + au.change, source = "Autores", due = today(), created = System.currentTimeMillis()))
                vm.study.add(StudyCard(vm.study.newId(), "", "qa", "Conceptos centrales de ${au.name}", au.concepts, source = "Autores", due = today(), created = System.currentTimeMillis()))
            }
            vm.toast = "Creé ${chosen.size * 2} tarjetas de autores."
        }) { Text("Tarjetas") }
    }
    AuthorGlossary(vm, a, chosen)
}

/** Una fila del glosario por autor: el concepto, el autor y qué significa para ese autor. */
private data class AuthorGloss(val concept: String, val author: String, val authorName: String, val meaning: String, val fromText: Int, val mine: Boolean)

/**
 * Glosario de autor en tres columnas. El significado sale de una oración del documento donde el concepto
 * aparece junto al autor (se marca «del texto»), o lo escribes tú. Hilo no inventa definiciones.
 */
@Composable
private fun AuthorGlossary(vm: HiloViewModel, a: Analysis?, chosen: List<com.hilo.lector.text.AuthorCard>) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    var ver by remember { mutableIntStateOf(0) }
    var edit by remember { mutableStateOf<AuthorGloss?>(null) }
    var adding by remember { mutableStateOf(false) }
    val rows = remember(chosen, a, ver) {
        val own = runCatching { org.json.JSONObject(vm.motor["glosautor"].ifBlank { "{}" }) }.getOrDefault(org.json.JSONObject())
        val extra = runCatching { org.json.JSONArray(vm.motor["glosautor+"].ifBlank { "[]" }) }.getOrDefault(org.json.JSONArray())
        fun fold(t: String) = java.text.Normalizer.normalize(t.lowercase(), java.text.Normalizer.Form.NFD).replace(com.hilo.lector.text.MARKS_RX, "")
        fun fromDoc(au: com.hilo.lector.text.AuthorCard, concept: String): com.hilo.lector.text.Sent? {
            if (a == null) return null
            val stem = fold(concept).split(' ').filter { it.length > 2 }.map { it.take(maxOf(4, it.length - 2)) }
            if (stem.isEmpty()) return null
            fun has(t: String) = fold(t).let { f -> stem.all { f.contains(it) } }
            val info = a.authors.firstOrNull { it.key == au.key }
            return info?.sentences?.firstOrNull { has(it.text) }
                ?: info?.paras?.let { ps -> a.sentences.firstOrNull { it.para in ps && has(it.text) } }
        }
        val list = ArrayList<AuthorGloss>()
        chosen.forEach { au ->
            au.concepts.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { cc ->
                val k = au.key + "|" + cc.lowercase()
                val mine = own.optString(k)
                val doc = if (mine.isBlank()) fromDoc(au, cc) else null
                list.add(AuthorGloss(cc, au.key, au.name, mine.ifBlank { doc?.text?.take(400).orEmpty() }, doc?.para ?: -1, mine.isNotBlank()))
            }
            for (i in 0 until extra.length()) {
                val o = extra.optJSONObject(i) ?: continue
                if (o.optString("a") == au.key) list.add(AuthorGloss(o.optString("c"), au.key, au.name, own.optString(au.key + "|" + o.optString("c").lowercase()).ifBlank { o.optString("m") }, -1, true))
            }
        }
        list.toList()
    }
    Text("Glosario por autor", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    Text("El mismo concepto cambia según el autor. «Del texto»: oración de tu documento donde aparece. Toca una fila para escribir tu definición.", color = c.muted, fontSize = 13.sp)
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Concepto", color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.3f))
        Text("Autor", color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.22f))
        Text("Qué significa para el autor", color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.48f))
    }
    rows.forEach { r ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.surface).clickable { edit = r }.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Palabras largas («racionalización») en letra algo menor, para que no se corten a la mitad
            Text(r.concept, color = c.accent, fontSize = if (r.concept.split(' ').maxOf { it.length } > 11) 11.sp else 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.3f))
            r.authorName.substringAfterLast(' ').let { n -> Text(n, color = c.ink, fontSize = if (n.length > 7) 11.sp else 13.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(0.22f)) }
            Column(Modifier.weight(0.48f)) {
                if (r.meaning.isBlank()) Text("Tocar para escribir", color = c.muted, fontSize = 13.sp)
                else Text(r.meaning, color = c.ink, fontSize = 13.sp, maxLines = 5, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (r.fromText >= 0) Text("del texto · párr. ${r.fromText + 1}", color = c.muted, fontSize = 11.sp)
                else if (r.mine && r.meaning.isNotBlank()) Text("tuya", color = c.muted, fontSize = 11.sp)
            }
        }
    }
    Row {
        TextButton(onClick = { adding = true }) { Text("Agregar concepto") }
        TextButton(onClick = {
            val t = listOf(listOf("Concepto", "Autor", "Qué significa para el autor")) + rows.map { listOf(it.concept, it.authorName, it.meaning) }
            com.hilo.lector.data.DocExport.shareTable(ctx, "Glosario por autor", listOf("Definiciones tomadas del texto o escritas por mí."), t)
        }) { Text("Word") }
        TextButton(onClick = {
            val filled = rows.filter { it.meaning.isNotBlank() }
            filled.forEach { r -> vm.study.add(StudyCard(vm.study.newId(), "", "qa", "¿Qué significa «${r.concept}» para ${r.authorName}?", r.meaning, source = "Glosario por autor", due = today(), created = System.currentTimeMillis())) }
            vm.toast = if (filled.isEmpty()) "Primero completa alguna definición." else "Creé ${filled.size} tarjetas."
        }) { Text("Tarjetas") }
    }
    edit?.let { r ->
        var t by remember(r) { mutableStateOf(r.meaning) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { edit = null },
            title = { Text("«${r.concept}» en ${r.authorName}") },
            text = { OutlinedTextField(t, { t = it }, minLines = 3, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Con tus palabras: qué entiende este autor por este concepto") }) },
            confirmButton = {
                TextButton(onClick = {
                    val own = runCatching { org.json.JSONObject(vm.motor["glosautor"].ifBlank { "{}" }) }.getOrDefault(org.json.JSONObject())
                    val k = r.author + "|" + r.concept.lowercase()
                    if (t.isBlank()) own.remove(k) else own.put(k, t.trim())
                    vm.motor.put("glosautor", own.toString()); ver++; edit = null
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { edit = null }) { Text("Cancelar") } },
        )
    }
    if (adding) {
        var cc by remember { mutableStateOf("") }
        var mm by remember { mutableStateOf("") }
        var who by remember { mutableStateOf(chosen.firstOrNull()?.key.orEmpty()) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Nuevo concepto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        chosen.forEach { au ->
                            val on = au.key == who
                            Text(au.name.substringAfterLast(' '), fontSize = 13.sp, color = if (on) c.accentInk else c.ink,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.accent else c.band).clickable { who = au.key }.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                    OutlinedTextField(cc, { cc = it }, singleLine = true, label = { Text("Concepto") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(mm, { mm = it }, minLines = 2, label = { Text("Qué significa para el autor") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(enabled = cc.isNotBlank() && who.isNotBlank(), onClick = {
                    val extra = runCatching { org.json.JSONArray(vm.motor["glosautor+"].ifBlank { "[]" }) }.getOrDefault(org.json.JSONArray())
                    extra.put(org.json.JSONObject().put("a", who).put("c", cc.trim()).put("m", mm.trim()))
                    vm.motor.put("glosautor+", extra.toString()); ver++; adding = false
                }) { Text("Agregar") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Schools(onClose: () -> Unit) {
    val c = LocalHilo.current
    OverlayFrame("Escuelas", onClose) {
        SocioKb.schools.forEach { s ->
            Panel {
                Text(s.name, color = c.accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(s.when_, color = c.muted, fontSize = 12.sp)
                Text(s.traits, color = c.ink, fontSize = 14.sp)
                Text("Autores: " + s.authors, color = c.ink, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Mills(vm: HiloViewModel, book: Book?, a: Analysis?, onClose: () -> Unit) {
    val c = LocalHilo.current
    val steps = listOf(
        "1. La noticia" to "Pega o resume una noticia actual (de Honduras o del mundo).",
        "2. El fenómeno" to "¿Qué pasa en la sociedad, más allá del caso? (desigualdad, migración, violencia, consumo…)",
        "3. El concepto o autor" to "¿Qué concepto o autor lo explica? Elige abajo o escribe.",
        "4. La pregunta sociológica" to "Convierte el problema personal en un problema público: ¿por qué pasa esto a tantas personas?",
        "5. La hipótesis" to "Una respuesta posible que se pueda comprobar: «si… entonces…».",
    )
    val f = remember { mutableStateListOf("", "", "", "", "") }
    OverlayFrame("Imaginación sociológica", onClose) {
        Text("Wright Mills: relacionar la biografía con la historia y los problemas personales con los problemas públicos.", color = c.muted, fontSize = 14.sp)
        steps.forEachIndexed { i, (t, h) ->
            Text(t, color = c.accent, fontWeight = FontWeight.Bold)
            Text(h, color = c.ink, fontSize = 13.sp)
            if (i == 2) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (a?.authors?.map { it.name }.orEmpty().take(4) + a?.terms?.take(8)?.map { it.label }.orEmpty()).forEach { k -> Pill(k) { f[2] = if (f[2].isBlank()) k else f[2] + ", " + k } }
            }
            OutlinedTextField(f[i], { f[i] = it }, minLines = if (i == 0) 3 else 2, modifier = Modifier.fillMaxWidth())
        }
        Button(onClick = {
            vm.motor.add(Entry(vm.motor.newId(), book?.id.orEmpty(), "mills", f[1].take(60).ifBlank { "Ejercicio" }, steps.mapIndexed { i, (t, _) -> "$t: ${f[i]}" }.joinToString("\n"), System.currentTimeMillis()))
            vm.toast = "Guardado."; onClose()
        }, enabled = f.count { it.isNotBlank() } >= 4, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("Guardar") }
    }
}

@Composable
private fun Diary(vm: HiloViewModel, book: Book?, a: Analysis?, onClose: () -> Unit) {
    val c = LocalHilo.current
    var concept by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    OverlayFrame("Diario sociológico", onClose) {
        Text("Una escena breve de tu día (en el bus, en clase, en la familia, en redes) explicada con un concepto. Dos o tres líneas bastan.", color = c.muted, fontSize = 14.sp)
        a?.let { an -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { an.terms.take(10).forEach { t -> Pill(t.label, t.label == concept) { concept = t.label } } } }
        OutlinedTextField(concept, { concept = it }, label = { Text("Concepto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(text, { text = it }, label = { Text("Lo que viste y cómo lo explica el concepto") }, minLines = 4, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            vm.motor.add(Entry(vm.motor.newId(), book?.id.orEmpty(), "diario", concept, text, System.currentTimeMillis()))
            concept = ""; text = ""; vm.toast = "Guardado en tu diario."
        }, enabled = concept.isNotBlank() && text.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("Guardar") }
        val old = vm.motor.entriesOf("diario").take(20)
        if (old.isNotEmpty()) Label("Tu diario")
        old.take(30).forEach { e ->
            Panel {
                Text(e.title, color = c.accent, fontWeight = FontWeight.Bold)
                Text(e.text, color = c.ink, fontSize = 14.sp)
                Text(java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.forLanguageTag("es")).format(java.util.Date(e.time)), color = c.muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun Essay(vm: HiloViewModel, book: Book?, a: Analysis?, onClose: () -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val key = "ensayo:${book?.id}"
    val parts = listOf("Título", "Tesis (lo que vas a defender)", "Argumento 1", "Argumento 2", "Evidencia y citas", "Contraargumento (y por qué no te convence)", "Conclusión")
    val saved = vm.motor[key].split("\u0001")
    val f = remember { mutableStateListOf(*Array(parts.size) { saved.getOrElse(it) { "" } }) }
    fun store() = vm.motor.put(key, f.joinToString("\u0001"))
    val quotes = book?.let { vm.study.quotesOf(it.id) }.orEmpty()
    val ref = book?.let { vm.study.refs[it.id] }
    OverlayFrame("Ensayo guiado", onClose) {
        parts.forEachIndexed { i, p ->
            OutlinedTextField(f[i], { f[i] = it; store() }, label = { Text(p) }, minLines = if (i == 0) 1 else 3, modifier = Modifier.fillMaxWidth())
            if (i == 4 && quotes.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                quotes.take(8).forEach { q -> Pill("+ " + q.text.take(30) + "…") { f[4] = f[4].trimEnd() + "\n" + com.hilo.lector.text.Apa.quote(q.text, ref?.authors ?: book?.author.orEmpty(), ref?.year.orEmpty(), q.loc); store() } }
            }
        }
        Label("Lista de revisión")
        val checks = listOf(
            "La tesis es una afirmación discutible, no un dato" to (f[1].length > 30),
            "Cada argumento tiene evidencia o una cita" to (f[4].isNotBlank()),
            "Hay un contraargumento" to f[5].isNotBlank(),
            "La conclusión retoma la tesis" to KeyWords.coverage(f[6], KeyWords.of(f[1], emptyList(), 4)).first.isNotEmpty(),
            "Usas conceptos del curso" to (a != null && KeyWords.coverage(f.joinToString(" "), a.terms.take(10).map { it.label }).first.size >= 2),
        )
        checks.forEach { (t, ok) -> Text((if (ok) "✓ " else "○ ") + t, color = if (ok) c.lines[1] else c.muted, fontSize = 14.sp) }
        Row {
            TextButton(onClick = { shareText(ctx, f[0].ifBlank { "Ensayo" }, parts.drop(1).mapIndexed { i, _ -> f[i + 1] }.filter { it.isNotBlank() }.joinToString("\n\n", prefix = f[0] + "\n\n")) }) { Text("Compartir texto") }
            TextButton(onClick = {
                runCatching { com.hilo.lector.data.DocExport.share(ctx, f[0].ifBlank { "Ensayo" }, f.drop(1).filter { it.isNotBlank() } + listOfNotNull(ref?.let { com.hilo.lector.text.Apa.reference(it.authors, it.year, it.title.ifBlank { book?.title.orEmpty() }, it.publisher, it.url) }?.let { "Referencias\n$it" })) }
                    .onFailure { vm.toast = "No pude crear el archivo." }
            }) { Text("Exportar a Word") }
        }
    }
}


/** Elegir clase y nombre para guardar un vencimiento como entrega (así entra en el recordatorio y en el widget). */
@Composable
private fun SaveDeadlineDialog(vm: HiloViewModel, day: Long, suggestion: String, onDone: () -> Unit) {
    val c = LocalHilo.current
    var title by remember { mutableStateOf(suggestion) }
    var cls by remember { mutableStateOf(vm.classes.courses.firstOrNull()?.id) }
    var newName by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Guardar el vencimiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Vence el ${dayLabel(day)}. Queda en el Calendario de la clase y te avisa el recordatorio diario la semana antes.", color = c.muted, fontSize = 14.sp)
                androidx.compose.material3.OutlinedTextField(title, { title = it }, label = { Text("Qué vence") }, modifier = Modifier.fillMaxWidth())
                val courses = vm.classes.courses
                if (courses.isEmpty()) {
                    Text("Todavía no tienes clases. Escribe un nombre y la creo:", color = c.muted, fontSize = 13.sp)
                    androidx.compose.material3.OutlinedTextField(newName, { newName = it }, label = { Text("Clase (por ejemplo Derecho Procesal)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                } else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    courses.forEach { cl -> Pill(cl.name, cl.id == cls) { cls = cl.id } }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val id = cls ?: newName.takeIf { it.isNotBlank() }?.let { vm.classes.create(it, com.hilo.lector.data.CLASS_COLORS[0], "", field = "derecho").id }
                if (id != null) {
                    vm.classes.putEvent(com.hilo.lector.data.ClassEvent(vm.classes.newId(), id, day, "entrega", title.trim().ifBlank { suggestion }))
                    com.hilo.lector.data.Reminders.schedule(vm.getApplication<android.app.Application>())
                    vm.toast = "Guardado en el calendario de la clase."
                }
                onDone()
            }, enabled = cls != null || newName.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Cancelar") } },
    )
}
