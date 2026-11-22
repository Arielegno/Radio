package com.hilo.lector.text

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Una palabra o frase relevante y cuántas veces aparece. */
data class Term(val key: String, val label: String, val count: Int, val phrase: Boolean) : java.io.Serializable

/** Una oración del documento y el párrafo donde está. */
data class Sent(val text: String, val para: Int, val keys: Set<String>) : java.io.Serializable

data class Chapter(val title: String, val startPara: Int, val endPara: Int) : java.io.Serializable

data class Gloss(val term: String, val definition: String, val para: Int) : java.io.Serializable

data class Edge(val a: Int, val b: Int, val w: Int) : java.io.Serializable

/** kind: def (definición), cloze (completar), open (explicar con tus palabras). */
data class Question(val q: String, val a: String, val kind: String, val para: Int) : java.io.Serializable

data class ChapterGuide(
    val chapter: Chapter,
    val words: Int,
    val ideas: List<Sent>,
    val concepts: List<String>,
    val questions: List<Question>,
) : java.io.Serializable

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
) : java.io.Serializable {
    fun labelOf(key: String): String = (terms.firstOrNull { it.key == key } ?: phrases.firstOrNull { it.key == key })?.label ?: key

    fun sentencesWith(key: String, limit: Int = 30): List<Sent> = sentences.filter { key in it.keys }.take(limit)
}

/**
 * Análisis del texto para estudiar: palabras más repetidas (sin las palabras vacías),
 * frases clave, definiciones para el glosario, relaciones entre conceptos y una guía por capítulo.
 * Todo se calcula en el celular.
 */
/** Correcciones del usuario: conceptos fijados, unidos, renombrados y agregados a mano. */
data class Tune(
    val pinned: Set<String> = emptySet(),
    val merged: Map<String, String> = emptyMap(),
    val labels: Map<String, String> = emptyMap(),
    val extra: List<String> = emptyList(),
)

object Analyzer {
    /**
     * Palabras de relleno de los textos académicos: se repiten mucho pero no son conceptos
     * («siguiente», «paso», «total», «resultado»). No entran al mapa ni a las burbujas, salvo que las fijes.
     */
    val GENERIC: Set<String> = """
        siguiente siguientes anterior anteriores paso pasos número números total totales resultado resultados elemento elementos
        conjunto conjuntos ejemplo ejemplos figura figuras tabla tablas cuadro cuadros gráfica gráfico gráficos punto puntos cantidad cantidades
        sección secciones ejercicio ejercicios respuesta respuestas pregunta preguntas aspecto aspectos hecho hechos idea ideas término términos
        sentido año años día días persona personas libro libros texto textos autor autores obra obras estudio estudios uso usos momento momentos
        situación situaciones cuestión cuestiones tema temas lado fin fines importante importantes diferente diferentes distinto distintos distinta distintas
        diverso diversos diversas determinado determinada determinados dado dada dados dadas último última últimos últimas final finales principal principales
        posible posibles necesario necesaria necesarios propio propia propios propias simple simples particular particulares general generales
        obtener obtiene obtienen obtenemos obtenido obtenida utiliza utilizan utilizar utilizado utilizada usar usa usan usando usado observar observa
        vemos ver veamos mostrar indica indican indicar significa supongamos supone supuesto puesto realizar realizado llamado llamada denominado denominada
        mismo misma cualquiera demás nuevo nueva alto alta bajo baja largo larga cierto cierta varios varias pequeño pequeña único única
        primera primero segunda segundo tercera tercero cuarto quinto mitad vez veces lugar hora horas minuto minutos semana mes meses
        capítulo capítulos apartado apartados página páginas párrafo párrafos línea líneas letra letras nombre nombres caso casos forma formas
        manera maneras tipo tipos parte partes modo medida cosa cosas grupo grupos clase siguiente fórmula
        destacar destaca destacan relevante relevantes evidente evidentes limitado limitada agrupado agrupada agrupados circunstancia circunstancias
        claro clara claros claras actual actuales anteriormente posteriormente igual iguales mayoría menudo respecto
        palabra palabras adecuado adecuada intentado intentar servido servir pasado pasada
    """.trim().split(Regex("\\s+")).toSet() - setOf("grupo", "grupos", "clase")

    private val NOUN_AR = setOf("lugar", "poder", "mujer", "militar", "popular", "familiar", "particular", "nuclear", "escolar", "regular", "hogar", "bienestar", "placer", "saber", "deber", "ser", "par", "mar", "valor", "factor", "error", "sector", "actor", "autor", "vector", "motor", "color", "dolor", "amor", "orden", "humor", "nivel", "señor", "mayor", "menor", "interior", "exterior", "superior", "inferior", "anterior", "posterior")

    /** Cuánto «parece concepto» una palabra por su forma: comunismo, democracia, sociología, alienación sí; observado, utilizar no. */
    fun shape(k: String): Double = when {
        k.contains(' ') -> 1.25
        Regex("(ismos?|istas?|cracias?|log[ií]as?|graf[ií]as?|arqu[ií]as?)$").containsMatchIn(k) -> 1.9
        Regex("(ciones|ción|siones|sión|dades|dad|tades|tad|ncias?|mientos?|ajes?|uras?|ezas?|ismo)$").containsMatchIn(k) -> 1.35
        k in NOUN_AR -> 1.0
        k.length > 5 && Regex("(ados?|adas?|idos?|idas?)$").containsMatchIn(k) -> 0.55
        k.length > 5 && Regex("(ar|er|ir|ando|iendo)$").containsMatchIn(k) -> 0.6
        k.length > 6 && Regex("(ivos?|ivas?|osos?|osas?|ables?|ibles?)$").containsMatchIn(k) -> 0.8
        else -> 1.0
    }

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

    /** La palabra sin tildes ni diéresis («teoría» → «teoria»); la ñ se queda. */
    internal fun plain(k: String): String {
        if (k.all { it.code < 128 }) return k
        val sb = StringBuilder(k.length)
        for (ch in k) sb.append(when (ch) { 'á', 'à', 'â', 'ä' -> 'a'; 'é', 'è', 'ê', 'ë' -> 'e'; 'í', 'ì', 'î', 'ï' -> 'i'; 'ó', 'ò', 'ô', 'ö' -> 'o'; 'ú', 'ù', 'û', 'ü' -> 'u'; else -> ch })
        return sb.toString()
    }

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

    fun analyze(paras: List<String>, removed: Set<String>, tune: Tune = Tune()): Analysis {
        // ---------- palabras ----------
        val counts = HashMap<String, Int>()
        val labels = HashMap<String, HashMap<String, Int>>()
        val phraseCounts = HashMap<String, Int>()
        val stopCounts = HashMap<String, Int>()
        val sentRaw = ArrayList<Pair<String, Int>>()
        var total = 0
        // Encabezados que se repiten en cada página de un PDF («6 TEORÍA SOCIOLÓGICA CONTEMPORÁNEA»): no cuentan como texto
        fun headKey(p: String) = p.trim().lowercase().filter { !it.isDigit() }.trim()
        val shortSeen = HashMap<String, Int>()
        for (p in paras) if (p.length < 90) { val k = headKey(p); if (k.length >= 4) shortSeen[k] = (shortSeen[k] ?: 0) + 1 }
        val headers = shortSeen.filterValues { it >= maxOf(8, paras.size / 60) }.keys
        paras.forEachIndexed { pi, raw ->
            if (headers.isNotEmpty() && raw.length < 90 && headKey(raw) in headers) return@forEachIndexed
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
        // La misma palabra sin tilde (escaneos, títulos en mayúscula: «TEORIA SOCIOLOGICA») cuenta con la que la lleva
        val accented = HashMap<String, String>()
        for ((k, n) in counts) {
            val s = plain(k)
            if (s != k) { val cur = accented[s]; if (cur == null || n > (counts[cur] ?: 0)) accented[s] = k }
        }
        for (k in counts.keys.toList()) {
            val t = accented[k] ?: continue
            val nk = counts[k] ?: continue
            if ((counts[t] ?: 0) * 10 < nk * 3) continue   // la forma con tilde es rara: mejor no juntarlas
            counts[t] = (counts[t] ?: 0) + nk
            counts.remove(k)
            labels[k]?.forEach { (l, n) -> labels.getOrPut(t) { HashMap() }.let { it[l] = (it[l] ?: 0) + n } }
            alias.entries.forEach { e -> if (e.value == k) e.setValue(t) }
            alias[k] = t
        }
        // Uniones hechas por el usuario: «estados» y «estado nación» → «estado», etc.
        for ((src0, dst0) in tune.merged) {
            val src = alias[src0] ?: src0
            val dst = alias[dst0] ?: dst0
            if (src == dst) continue
            val n = (counts.remove(src) ?: 0) + (stopCounts.remove(src) ?: 0)
            if (n > 0) counts[dst] = (counts[dst] ?: 0) + n
            alias[src] = dst
            alias.entries.forEach { e -> if (e.value == src) e.setValue(dst) }
        }
        // Conceptos agregados a mano: se cuentan en todo el texto aunque Hilo los hubiera descartado
        val extraPhrases = ArrayList<Term>()
        if (tune.extra.isNotEmpty()) {
            val lowParas = paras.map { it.lowercase() }
            for (e in tune.extra) {
                val k = e.trim().lowercase()
                if (k.isEmpty()) continue
                val rx = Regex("(?<![\\p{L}])" + Regex.escape(k) + "(?![\\p{L}])")
                val n = lowParas.sumOf { p -> rx.findAll(p).count() }
                if (k.contains(' ')) extraPhrases.add(Term(k, tune.labels[k] ?: e.trim(), n, true))
                else { stopCounts.remove(k); counts[k] = max(counts[k] ?: 0, n); labels.getOrPut(k) { HashMap() }[e.trim()] = n + 1 }
            }
        }
        fun canon(k: String) = alias[k] ?: k
        fun labelOf(k: String): String {
            tune.labels[k]?.let { return it }
            val m = labels[k] ?: return k
            // Preferir la forma en minúscula si es común; si casi siempre va con mayúscula (nombres), dejarla
            val lower = m.filterKeys { it == it.lowercase() }.values.sum()
            val best = m.maxByOrNull { it.value }?.key ?: k
            return if (lower * 3 >= m.values.sum()) k else best
        }

        val distinct = counts.size
        val terms = counts.entries
            .filter { it.key !in removed && (it.value >= 2 || it.key in tune.pinned) }
            .sortedByDescending { it.value * (if (it.key in GENERIC) 0.3 else 1.0) }
            .take(150)
            .map { Term(it.key, labelOf(it.key), it.value, false) }

        val minPhrase = max(3, total / 25000)
        val phrases = (extraPhrases.filter { it.key !in removed } + phraseCounts.entries
            .filter { e ->
                val ws = e.key.split(' ')
                e.value >= minPhrase && e.key !in removed && ws.last() !in GENERIC && !(ws.first() in GENERIC && ws.size == 2) && ws.first() !in setOf("siguiente", "anterior", "dicho", "dicha", "mismo", "misma", "cada", "otro", "otra")
            }
            .sortedByDescending { it.value * (1 + it.key.count { c -> c == ' ' }) * shape(it.key.split(' ').first()) }
            .take(30)
            .map { Term(it.key, tune.labels[it.key] ?: it.key, it.value, true) })
            .distinctBy { it.key }

        // ---------- oraciones ----------
        val sentences = sentRaw.filter { it.first.length in 25..600 }.map { (s, pi) ->
            val keys = WORD.findAll(s).map { canon(it.value.lowercase()) }.filter { !isStop(it) }.toHashSet()
            val low = s.lowercase()
            phrases.forEach { ph -> if (low.contains(ph.key)) keys.add(ph.key) }
            Sent(s, pi, keys)
        }

        // ---------- mapa de conceptos ----------
        // Mapa: los más repetidos, pero pesando la forma de la palabra (los «-ismo», «-ción», «-dad» suben;
        // los verbos y el relleno bajan). Los fijados por el usuario entran siempre.
        fun mapScore(t: Term) = t.count * (if (t.phrase) 1.25 else shape(t.key))
        val pinnedTerms = (terms + phrases).filter { it.key in tune.pinned || it.key in tune.extra.map { e -> e.trim().lowercase() } }.distinctBy { it.key }
        val mapTerms = (pinnedTerms + (phrases.take(8) + terms)
            .filter { it.key !in GENERIC && it.key !in removed }
            .sortedByDescending { mapScore(it) })
            .distinctBy { it.key }.take(maxOf(28, pinnedTerms.size))
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
            allWords = allWords, common = (stopCounts.keys + counts.keys.filter { it in GENERIC && it !in tune.pinned }).toHashSet(), paraCount = paras.size,
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
    /** Palabras con las que NO empieza un término definido (demostrativos, pronombres, adverbios). */
    private val NOT_TERM = setOf("esta", "este", "estos", "estas", "esa", "ese", "esos", "esas", "eso", "esto", "aquello", "ello", "ella", "él", "ellos", "ellas",
        "lo", "que", "cual", "quien", "donde", "cuando", "como", "ya", "también", "tambien", "así", "asi", "aquí", "aqui", "hoy", "mi", "su", "sus", "nuestro", "nuestra",
        "tal", "dicha", "dicho", "dichos", "dichas", "otra", "otro", "otros", "otras", "cada", "todo", "toda", "todos", "todas", "nada", "algo", "mucho", "mucha", "poco", "situación", "situacion", "caso", "problema", "cosa", "hecho")
    /** Una definición suele empezar con un artículo o una palabra que nombra («un conjunto», «la facultad»…). */
    private val DEF_START = setOf("un", "una", "unos", "unas", "el", "la", "los", "las", "aquel", "aquella", "aquello", "aquellos", "aquellas", "lo", "todo", "toda", "cualquier", "cualquiera", "uno", "parte", "conjunto", "acción", "accion", "acto", "proceso", "facultad", "capacidad", "derecho", "instrumento", "medio", "forma", "tipo", "rama", "ciencia", "disciplina", "estudio", "relación", "relacion", "estado", "situación", "situacion", "principio", "norma", "regla")
    /** Si lo que sigue a «es» es una opinión, no es definición («es particularmente importante»). */
    private val OPINION = setOf("muy", "más", "mas", "menos", "particularmente", "especialmente", "sumamente", "bastante", "tan", "importante", "necesario", "necesaria", "posible", "fundamental", "claro", "clara", "evidente", "cierto", "cierta", "difícil", "dificil", "fácil", "facil", "útil", "util", "decir", "porque", "cuando", "ya", "también", "tambien", "así", "asi", "no", "sí", "si", "mejor", "peor", "bueno", "buena", "malo", "mala", "probable", "imposible", "obvio", "verdad", "falso", "suficiente")

    fun findDefinitions(sentences: List<Sent>, important: Set<String>): List<Gloss> {
        val found = LinkedHashMap<String, Pair<Gloss, Int>>()
        for (s in sentences) {
            if (s.text.length > 420) continue
            val m = DEF1.find(s.text) ?: DEF2.find(s.text) ?: continue
            val term = m.groupValues[1].trim().trimEnd(',', ':')
            val words = term.split(Regex("\\s+"))
            if (words.size > 5 || words.all { isStop(it.lowercase()) }) continue
            // No es una definición si empieza con «Esta…», «Eso…», «Situación que…», o si lo que sigue es una opinión
            val w0 = words.first().lowercase()
            if (w0 in NOT_TERM || words.any { it.lowercase() in setOf("que", "cual", "cuales", "donde", "cuando", "quien") }) continue
            val rest = m.groupValues[2].trimStart().lowercase()
            val strongVerb = Regex("(?i)\\b(se define|se entiende|consiste en|se refiere a|significa|designa|alude a|se denomina|se llama|llamamos|denominamos|definimos)\\b").containsMatchIn(s.text.take(term.length + 40))
            val r0 = rest.split(Regex("\\s+")).firstOrNull().orEmpty()
            if (!strongVerb && (r0 in OPINION || r0 !in DEF_START)) continue
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
