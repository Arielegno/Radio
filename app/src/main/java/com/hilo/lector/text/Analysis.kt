package com.hilo.lector.text

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Una palabra o frase relevante y cuántas veces aparece. */
data class Term(val key: String, val label: String, val count: Int, val phrase: Boolean)

/** Una oración del documento y el párrafo donde está. */
data class Sent(val text: String, val para: Int, val keys: Set<String>)

data class Chapter(val title: String, val startPara: Int, val endPara: Int)

data class Gloss(val term: String, val definition: String, val para: Int)

data class Edge(val a: Int, val b: Int, val w: Int)

/** kind: def (definición), cloze (completar), open (explicar con tus palabras). */
data class Question(val q: String, val a: String, val kind: String, val para: Int)

data class ChapterGuide(
    val chapter: Chapter,
    val words: Int,
    val ideas: List<Sent>,
    val concepts: List<String>,
    val questions: List<Question>,
)

class Analysis(
    val totalWords: Int,
    val distinct: Int,
    val terms: List<Term>,
    val phrases: List<Term>,
    val sentences: List<Sent>,
    val mapTerms: List<Term>,
    val edges: List<Edge>,
    val glossary: List<Gloss>,
    val guides: List<ChapterGuide>,
    val law: LawIndex = LawIndex(emptyList(), emptyList(), emptyList()),
    val authors: List<AuthorInfo> = emptyList(),
    val timeline: List<TimeEvent> = emptyList(),
    /** Todas las palabras del documento con sus veces, también las comunes («de», «la», «que»). */
    val allWords: List<Term> = emptyList(),
    /** Las palabras comunes (artículos, conectores…) que no van a las burbujas salvo que las actives. */
    val common: Set<String> = emptySet(),
    val paraCount: Int = 0,
) {
    fun labelOf(key: String): String = (terms.firstOrNull { it.key == key } ?: phrases.firstOrNull { it.key == key })?.label ?: key

    fun sentencesWith(key: String, limit: Int = 30): List<Sent> = sentences.filter { key in it.keys }.take(limit)
}

/**
 * Análisis del texto para estudiar: palabras más repetidas (sin las palabras vacías),
 * frases clave, definiciones para el glosario, relaciones entre conceptos y una guía por capítulo.
 * Todo se calcula en el celular.
 */
object Analyzer {
    private val WORD = Regex("[\\p{L}][\\p{L}'’]*(?:-[\\p{L}]+)*")
    private val SENT_SPLIT = Regex("(?<=[.!?…])[»”\"')\\]]*\\s+(?=[\\p{Lu}¿¡«“\"(\\d])")

    val STOP: Set<String> = """
        a al algo algún alguna algunas alguno algunos allí ante antes aquel aquella aquellas aquello aquellos aquí así aun aún
        bajo bien cabe cada casi cierta ciertas cierto ciertos como cómo con contra cual cuál cuales cuáles cualquier cuando cuándo
        cuanto cuánto cuya cuyas cuyo cuyos de del dentro desde donde dónde dos durante e el él ella ellas ello ellos en entre era
        eran eres es esa esas ese eso esos esta está estaba estaban estado estados estamos están estar estas este esto estos estoy
        fue fueron fui ha había habían haber habrá han has hasta hay he hecho hemos hice hizo hoy la las le les lo los luego más
        mas me mediante menos mi mí mis misma mismas mismo mismos mucha muchas mucho muchos muy nada ni ningún ninguna ninguno no
        nos nosotros nuestra nuestras nuestro nuestros nunca o os otra otras otro otros para parece pero poco pocos por porque
        pues puede pueden pudo que qué quien quién quienes se sea sean según ser será sería si sí sido siempre siendo sin sino
        sobre solo sólo son su sus suya suyo tal también tampoco tan tanto te tenemos tener tenía tiene tienen toda todas todavía
        todo todos tras tu tú tus un una unas uno unos usted ustedes va vamos van varias varios vez veces y ya yo
        además ahora aquí allá cual cuales dicho dicha dichos dichas debe deben deber decir dice dicen hace hacen hacer haya
        hayan podemos poder podría podrían posible primer primera primero segundo misma tiempo forma manera parte partes caso
        casos cosa cosas tipo tipos modo medida mientras incluso aunque pues entonces asimismo embargo cuanto respecto través
        sí cierto ciertos otra otro nuevo nueva nuevos nuevas gran grande grandes mayor menor mejor peor sólo solamente solo
        esto ésta éste éstos éstas aquello cuyo cuya lugar uno dos tres cuatro cinco seis siete ocho nueve diez cap pág
        the of and to in a is that for it as was with be by on not he this are or his from at which but have an they you
        were had her she there been one all we their its has would more if will can when who so no what out up into them
        these than some could other then about only our also may such any over most those very just should because both
        each between through after where while how being however many must much own same well within without upon here
        capítulo capitulo sección seccion página pagina aparece aparecen existe existen permite permiten presenta presentan
        realiza realizan muestra muestran lleva llevan sigue siguen encuentra encuentran trata tratan refiere considera
        consideran concepto conceptos estudia estudió señala señalan plantea plantean afirma afirman cabe tiene tenían hacia ejemplo general
        de la que el en y a los se del las un por con no una su para es al lo como más pero sus le ya o este sí porque esta
        véase vease cfr ibid ibíd ídem idem op cit loc etc pp vol ed eds trad nota notas cita supra infra
        allí ahí acá cuyo dónde aquél aquélla aquéllos ése ésa ésos ésas sino quizá quizás acaso apenas
        hubo había hubiera hubiese habría tenido tuvo sean fuera fuese sería serían estaría estuvo
        dijo decía dice dicen digo cree creo creía queda quedan
    """.trim().split(Regex("\\s+")).toSet()

    /** Palabras que no aportan contenido: artículos, conectores, adverbios en «-mente» (claramente, precisamente), números. */
    internal fun isStop(k: String) = k.length < 3 || k in STOP || k.all { it.isDigit() } || (k.length > 7 && k.endsWith("mente"))

    private fun splitSentences(p: String): List<String> = p.split(SENT_SPLIT).map { it.trim() }.filter { it.isNotEmpty() }

    fun chapters(paras: List<String>): List<Chapter> {
        val heads = paras.indices.filter { paras[it].startsWith("# ") }
        if (heads.size >= 2) {
            val out = ArrayList<Chapter>()
            if (heads.first() > 2) out.add(Chapter("Inicio", 0, heads.first()))
            heads.forEachIndexed { i, h ->
                val end = if (i + 1 < heads.size) heads[i + 1] else paras.size
                if (end - h > 1 || i == heads.size - 1) out.add(Chapter(paras[h].substring(2).take(80), h, end))
            }
            return out
        }
        // Sin títulos: partes de unas 2500 palabras
        val out = ArrayList<Chapter>()
        var start = 0
        var words = 0
        for (i in paras.indices) {
            words += paras[i].count { it == ' ' } + 1
            if (words >= 2500 || i == paras.size - 1) {
                out.add(Chapter("Parte ${out.size + 1}", start, i + 1))
                start = i + 1
                words = 0
            }
        }
        return out
    }

    fun analyze(paras: List<String>, removed: Set<String>): Analysis {
        // ---------- palabras ----------
        val counts = HashMap<String, Int>()
        val labels = HashMap<String, HashMap<String, Int>>()
        val phraseCounts = HashMap<String, Int>()
        val stopCounts = HashMap<String, Int>()
        val sentRaw = ArrayList<Pair<String, Int>>()
        var total = 0
        paras.forEachIndexed { pi, raw ->
            val p = raw.removePrefix("# ")
            for (s in splitSentences(p)) {
                sentRaw.add(s to pi)
                val toks = WORD.findAll(s).map { it.value }.toList()
                total += toks.size
                val low = toks.map { it.lowercase() }
                for ((i, w) in low.withIndex()) {
                    if (isStop(w)) {
                        if (w.isNotBlank() && !w.all { it.isDigit() }) {
                            stopCounts[w] = (stopCounts[w] ?: 0) + 1
                            val lm = labels.getOrPut(w) { HashMap() }
                            lm[toks[i]] = (lm[toks[i]] ?: 0) + 1
                        }
                        continue
                    }
                    counts[w] = (counts[w] ?: 0) + 1
                    val lm = labels.getOrPut(w) { HashMap() }
                    lm[toks[i]] = (lm[toks[i]] ?: 0) + 1
                }
                // Frases: "división del trabajo", "capital cultural", "derecho de propiedad"
                for (i in low.indices) {
                    val a = low[i]
                    if (isStop(a)) continue
                    if (i + 1 < low.size && !isStop(low[i + 1])) {
                        val k = a + " " + low[i + 1]
                        phraseCounts[k] = (phraseCounts[k] ?: 0) + 1
                    }
                    if (i + 2 < low.size && (low[i + 1] == "de" || low[i + 1] == "del") && !isStop(low[i + 2])) {
                        val k = a + " " + low[i + 1] + " " + low[i + 2]
                        phraseCounts[k] = (phraseCounts[k] ?: 0) + 1
                    }
                    if (i + 3 < low.size && low[i + 1] == "de" && (low[i + 2] == "la" || low[i + 2] == "los" || low[i + 2] == "las") && !isStop(low[i + 3])) {
                        val k = a + " de " + low[i + 2] + " " + low[i + 3]
                        phraseCounts[k] = (phraseCounts[k] ?: 0) + 1
                    }
                }
            }
        }

        // Plurales con su singular: "sociedades" → "sociedad", "clases" → "clase"
        val alias = HashMap<String, String>()
        for (k in counts.keys.toList()) {
            val base = when {
                k.endsWith("es") && counts.containsKey(k.dropLast(2)) -> k.dropLast(2)
                k.endsWith("s") && counts.containsKey(k.dropLast(1)) -> k.dropLast(1)
                else -> null
            } ?: continue
            alias[k] = base
        }
        for ((k, base) in alias) {
            counts[base] = (counts[base] ?: 0) + (counts.remove(k) ?: 0)
            labels[k]?.forEach { (l, n) -> labels.getOrPut(base) { HashMap() }.let { it[l] = (it[l] ?: 0) + n } }
        }
        fun canon(k: String) = alias[k] ?: k
        fun labelOf(k: String): String {
            val m = labels[k] ?: return k
            // Preferir la forma en minúscula si es común; si casi siempre va con mayúscula (nombres), dejarla
            val lower = m.filterKeys { it == it.lowercase() }.values.sum()
            val best = m.maxByOrNull { it.value }?.key ?: k
            return if (lower * 3 >= m.values.sum()) k else best
        }

        val distinct = counts.size
        val terms = counts.entries
            .filter { it.key !in removed && it.value >= 2 }
            .sortedByDescending { it.value }
            .take(150)
            .map { Term(it.key, labelOf(it.key), it.value, false) }

        val minPhrase = max(3, total / 25000)
        val phrases = phraseCounts.entries
            .filter { it.value >= minPhrase && it.key !in removed }
            .sortedByDescending { it.value * (1 + it.key.count { c -> c == ' ' }) }
            .take(30)
            .map { Term(it.key, it.key, it.value, true) }

        // ---------- oraciones ----------
        val sentences = sentRaw.filter { it.first.length in 25..600 }.map { (s, pi) ->
            val keys = WORD.findAll(s).map { canon(it.value.lowercase()) }.filter { !isStop(it) }.toHashSet()
            val low = s.lowercase()
            phrases.forEach { ph -> if (low.contains(ph.key)) keys.add(ph.key) }
            Sent(s, pi, keys)
        }

        // ---------- mapa de conceptos ----------
        val mapTerms = (phrases.take(8) + terms).distinctBy { it.key }.sortedByDescending { it.count }.take(28)
        val idx = mapTerms.withIndex().associate { it.value.key to it.index }
        val pair = HashMap<Long, Int>()
        for (s in sentences) {
            val ids = s.keys.mapNotNull { idx[it] }.distinct().sorted()
            for (i in ids.indices) for (j in i + 1 until ids.size) {
                val k = ids[i].toLong() * 1000 + ids[j]
                pair[k] = (pair[k] ?: 0) + 1
            }
        }
        val allEdges = pair.filter { it.value >= 2 }.map { Edge((it.key / 1000).toInt(), (it.key % 1000).toInt(), it.value) }
            .sortedByDescending { it.w }
        val keep = HashSet<Edge>()
        for (n in mapTerms.indices) allEdges.filter { it.a == n || it.b == n }.take(3).forEach { keep.add(it) }
        allEdges.take(45).forEach { keep.add(it) }

        // ---------- glosario ----------
        val glossary = findDefinitions(sentences, terms.take(80).map { it.key }.toSet() + phrases.map { it.key })

        // ---------- guía por capítulo ----------
        val weight = HashMap<String, Double>()
        val maxC = (terms.firstOrNull()?.count ?: 1).toDouble()
        terms.forEach { weight[it.key] = 0.3 + it.count / maxC }
        phrases.forEach { weight[it.key] = 0.5 + it.count / maxC * 2 }
        val chs = chapters(paras)
        val guides = chs.map { ch ->
            val chSents = sentences.filter { it.para >= ch.startPara && it.para < ch.endPara }
            val chCount = HashMap<String, Int>()
            chSents.forEach { s -> s.keys.forEach { k -> if (weight.containsKey(k)) chCount[k] = (chCount[k] ?: 0) + 1 } }
            val globalCount = (terms + phrases).associate { it.key to it.count }
            val concepts = chCount.entries
                .sortedByDescending { e -> e.value * (1.0 + ln(1.0 + e.value.toDouble() / max(1, globalCount[e.key] ?: 1) * chs.size)) }
                .take(6).map { it.key }
            val labelFor = (terms + phrases).associate { it.key to it.label }
            fun score(s: Sent): Double {
                val base = s.keys.sumOf { k -> (weight[k] ?: 0.0) * (if (k in concepts) 2.0 else 1.0) }
                return base / sqrt(max(8.0, s.text.length / 6.0))
            }
            val ideas = chSents.filter { it.text.length in 60..420 }.sortedByDescending { score(it) }.take(3)
                .sortedBy { it.para }
            val qs = ArrayList<Question>()
            glossary.filter { it.para >= ch.startPara && it.para < ch.endPara }.take(3).forEach {
                qs.add(Question("¿Qué es «${it.term}»?", it.definition, "def", it.para))
            }
            for (k in concepts) {
                if (qs.size >= 5) break
                val lab = labelFor[k] ?: k
                if (qs.any { it.q.contains(lab, true) }) continue
                val best = chSents.filter { k in it.keys && it.text.length in 50..350 }.maxByOrNull { score(it) } ?: continue
                val hole = Regex("(?<![\\p{L}])" + Regex.escape(lab) + "s?(?![\\p{L}])", RegexOption.IGNORE_CASE)
                if (!hole.containsMatchIn(best.text)) continue
                qs.add(Question(best.text.replace(hole, "[ … ]"), lab, "cloze", best.para))
            }
            if (concepts.size >= 2) {
                val a = concepts[0]
                val b = concepts.drop(1).firstOrNull { o -> chSents.any { a in it.keys && o in it.keys } } ?: concepts[1]
                val both = chSents.filter { a in it.keys && b in it.keys }.maxByOrNull { score(it) }
                qs.add(
                    Question(
                        "Explica con tus palabras la relación entre «${labelFor[a] ?: a}» y «${labelFor[b] ?: b}» en este capítulo.",
                        both?.text ?: (ideas.firstOrNull()?.text ?: ""), "open", both?.para ?: ch.startPara,
                    )
                )
            }
            val words = paras.subList(ch.startPara, min(ch.endPara, paras.size)).sumOf { it.count { c -> c == ' ' } + 1 }
            ChapterGuide(ch, words, ideas, concepts.map { labelFor[it] ?: it }, qs)
        }

        val law = runCatching { Law.parse(paras) }.getOrElse { LawIndex(emptyList(), emptyList(), emptyList()) }
        val authors = runCatching { Social.authors(paras, sentences, terms, phrases) }.getOrDefault(emptyList())
        val timeline = runCatching { Social.timeline(sentences, authors) }.getOrDefault(emptyList())
        // Todas las palabras: las de contenido y las comunes, de la más a la menos repetida
        val allWords = (counts.entries.map { Term(it.key, labelOf(it.key), it.value, false) } +
            stopCounts.entries.map { Term(it.key, labelOf(it.key), it.value, false) })
            .sortedByDescending { it.count }
        return Analysis(
            total, distinct + stopCounts.size, terms, phrases, sentences, mapTerms, keep.toList(), glossary, guides, law, authors, timeline,
            allWords = allWords, common = stopCounts.keys.toHashSet(), paraCount = paras.size,
        )
    }

    private val DEF1 = Regex(
        "^(?:(?:el|la|los|las|un|una|lo)\\s+)?([\\p{L}][\\p{L}\\s-]{2,48}?)\\s+(?:es|son|era|fue|será|se define como|se entiende como|consiste en|se refiere a|significa|alude a|designa|constituye|implica)\\s+(.{15,})$",
        RegexOption.IGNORE_CASE,
    )
    private val DEF2 = Regex(
        "^(?:se entiende por|entendemos por|llamamos|denominamos|se denomina|se llama|definimos)\\s+([\\p{L}][\\p{L}\\s-]{2,48}?)\\s*(?:,|\\sa\\s|\\sal\\s|\\scomo\\s|\\s(?=(?:el|la|los|las|un|una|todo|toda|aquel|aquella)\\s))\\s*(.{15,})$",
        RegexOption.IGNORE_CASE,
    )

    /** Busca oraciones que definen un concepto: "La anomia es…", "Se entiende por capital cultural…". */
    fun findDefinitions(sentences: List<Sent>, important: Set<String>): List<Gloss> {
        val found = LinkedHashMap<String, Pair<Gloss, Int>>()
        for (s in sentences) {
            if (s.text.length > 420) continue
            val m = DEF1.find(s.text) ?: DEF2.find(s.text) ?: continue
            val term = m.groupValues[1].trim().trimEnd(',', ':')
            val words = term.split(Regex("\\s+"))
            if (words.size > 5 || words.all { isStop(it.lowercase()) }) continue
            if (term.firstOrNull()?.isDigit() == true) continue
            val key = term.lowercase()
            val score = (if (words.any { it.lowercase() in important } || key in important) 10 else 0) +
                (if (s.text.contains(" se define ") || s.text.contains(" se entiende ") || s.text.contains(" consiste ")) 5 else 0)
            if (score == 0 && words.size > 3) continue
            val prev = found[key]
            if (prev == null || score > prev.second) found[key] = Gloss(term.replaceFirstChar { it.uppercase() }, s.text, s.para) to score
        }
        return found.values.sortedWith(compareByDescending<Pair<Gloss, Int>> { it.second }.thenBy { it.first.para })
            .take(80).map { it.first }.sortedBy { it.para }
    }
}
