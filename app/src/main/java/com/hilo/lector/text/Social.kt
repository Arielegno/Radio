package com.hilo.lector.text

import java.text.Normalizer
import kotlin.math.sqrt

/** Un autor mencionado en el texto, con sus conceptos y las oraciones donde aparece. */
data class AuthorInfo(
    val key: String,
    val name: String,
    val mentions: Int,
    val years: List<String>,
    val concepts: List<String>,       // claves de Term
    val conceptLabels: List<String>,
    val sentences: List<Sent>,
    val paras: Set<Int>,
) : java.io.Serializable

/** Un hecho con fecha: el año y la oración donde aparece. */
data class TimeEvent(val year: Int, val label: String, val text: String, val para: Int, val author: String?) : java.io.Serializable

/** Una fila del cuadro comparativo: el concepto y lo que dice cada autor (o null si no lo trata). */
data class CompareRow(val concept: String, val cells: List<Sent?>)

object Social {
    /** Autores frecuentes en sociología, derecho, filosofía y ciencias sociales. Clave → nombre completo. */
    val KNOWN: Map<String, String> = linkedMapOf(
        "marx" to "Karl Marx", "engels" to "Friedrich Engels", "weber" to "Max Weber", "durkheim" to "Émile Durkheim",
        "bourdieu" to "Pierre Bourdieu", "foucault" to "Michel Foucault", "giddens" to "Anthony Giddens", "parsons" to "Talcott Parsons",
        "merton" to "Robert K. Merton", "simmel" to "Georg Simmel", "comte" to "Auguste Comte", "mannheim" to "Karl Mannheim",
        "luckmann" to "Thomas Luckmann", "habermas" to "Jürgen Habermas", "goffman" to "Erving Goffman", "bauman" to "Zygmunt Bauman",
        "elias" to "Norbert Elias", "luhmann" to "Niklas Luhmann", "tönnies" to "Ferdinand Tönnies", "tonnies" to "Ferdinand Tönnies",
        "spencer" to "Herbert Spencer", "gramsci" to "Antonio Gramsci", "blumer" to "Herbert Blumer", "garfinkel" to "Harold Garfinkel",
        "schutz" to "Alfred Schütz", "schütz" to "Alfred Schütz", "adorno" to "Theodor W. Adorno", "horkheimer" to "Max Horkheimer",
        "marcuse" to "Herbert Marcuse", "wallerstein" to "Immanuel Wallerstein", "castells" to "Manuel Castells", "touraine" to "Alain Touraine",
        "dahrendorf" to "Ralf Dahrendorf", "veblen" to "Thorstein Veblen", "tocqueville" to "Alexis de Tocqueville",
        "montesquieu" to "Montesquieu", "rousseau" to "Jean-Jacques Rousseau", "hobbes" to "Thomas Hobbes", "locke" to "John Locke",
        "kant" to "Immanuel Kant", "hegel" to "G. W. F. Hegel", "kelsen" to "Hans Kelsen", "dworkin" to "Ronald Dworkin",
        "beccaria" to "Cesare Beccaria", "savigny" to "Friedrich Carl von Savigny", "jhering" to "Rudolf von Jhering", "ihering" to "Rudolf von Jhering",
        "bobbio" to "Norberto Bobbio", "ferrajoli" to "Luigi Ferrajoli", "alexy" to "Robert Alexy", "rawls" to "John Rawls",
        "radbruch" to "Gustav Radbruch", "ulpiano" to "Ulpiano", "gayo" to "Gayo", "bentham" to "Jeremy Bentham", "maquiavelo" to "Nicolás Maquiavelo",
        "aristóteles" to "Aristóteles", "platón" to "Platón", "quijano" to "Aníbal Quijano", "freire" to "Paulo Freire",
        "germani" to "Gino Germani", "dussel" to "Enrique Dussel", "mariátegui" to "José Carlos Mariátegui", "prebisch" to "Raúl Prebisch",
        "arendt" to "Hannah Arendt", "beck" to "Ulrich Beck", "lukács" to "György Lukács", "althusser" to "Louis Althusser",
        "lévi-strauss" to "Claude Lévi-Strauss", "malinowski" to "Bronisław Malinowski", "mead" to "George H. Mead", "goffman" to "Erving Goffman",
        "zaffaroni" to "Eugenio Raúl Zaffaroni", "roxin" to "Claus Roxin", "welzel" to "Hans Welzel", "jakobs" to "Günther Jakobs",
        "carnelutti" to "Francesco Carnelutti", "chiovenda" to "Giuseppe Chiovenda", "couture" to "Eduardo J. Couture", "bidart" to "Germán Bidart Campos",
    )

    private val NAME = Regex("(?<![\\p{L}])([\\p{Lu}][\\p{Ll}áéíóúüöñ'’-]{2,}(?:\\s(?:de|von|van|y)\\s[\\p{Lu}][\\p{Ll}]+)?)(?![\\p{L}])")
    private val CITE = Regex("\\(([\\p{Lu}][\\p{L}'’-]{2,})(?:\\s(?:y|&|et al\\.?)\\s?[\\p{Lu}]?[\\p{L}'’-]*)?,?\\s(1[5-9]\\d\\d|20[0-2]\\d)[a-z]?(?:[,:]\\s?p+\\.\\s?\\d+(?:-\\d+)?)?\\)")
    private val CITE2 = Regex("([\\p{Lu}][\\p{L}'’-]{2,})\\s\\((1[5-9]\\d\\d|20[0-2]\\d)[a-z]?\\)")
    private val VERBED = Regex("(?:(?:[Pp]ara|[Ss]egún|[Cc]omo\\s(?:señala|afirma|plantea|sostiene))\\s)([\\p{Lu}][\\p{Ll}'’-]{3,})(?=[,\\s])")

    private val NOT_NAMES = setOf(
        "para", "según", "como", "este", "esta", "estos", "estas", "pero", "también", "sin", "con", "desde", "cuando", "donde", "entre",
        "dios", "estado", "iglesia", "europa", "américa", "francia", "alemania", "inglaterra", "españa", "honduras", "méxico",
        "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
        "artículo", "capítulo", "ley", "código", "constitución", "república", "tribunal", "corte", "sala", "congreso",
    )

    fun deaccent(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    fun authors(paras: List<String>, sentences: List<Sent>, terms: List<Term>, phrases: List<Term>): List<AuthorInfo> {
        val counts = HashMap<String, Int>()
        val display = HashMap<String, String>()
        val years = HashMap<String, LinkedHashSet<String>>()
        val known = KNOWN.keys
        val candidate = HashMap<String, Int>()
        for (p in paras) {
            for (m in NAME.findAll(p)) {
                val k = m.groupValues[1].lowercase()
                if (k in known) { counts[k] = (counts[k] ?: 0) + 1; display[k] = KNOWN[k]!! }
            }
            for (m in CITE.findAll(p)) {
                val k = m.groupValues[1].lowercase()
                if (k in NOT_NAMES) continue
                candidate[k] = (candidate[k] ?: 0) + 2
                years.getOrPut(k) { LinkedHashSet() }.add(m.groupValues[2])
                display.putIfAbsent(k, m.groupValues[1])
            }
            for (m in CITE2.findAll(p)) {
                val k = m.groupValues[1].lowercase()
                if (k in NOT_NAMES) continue
                candidate[k] = (candidate[k] ?: 0) + 2
                years.getOrPut(k) { LinkedHashSet() }.add(m.groupValues[2])
                display.putIfAbsent(k, m.groupValues[1])
            }
            for (m in VERBED.findAll(p)) {
                val k = m.groupValues[1].lowercase()
                if (k in NOT_NAMES || k in Analyzer.STOP) continue
                candidate[k] = (candidate[k] ?: 0) + 1
                display.putIfAbsent(k, m.groupValues[1])
            }
        }
        // Autores no conocidos: los que aparecen citados al menos dos veces
        for ((k, n) in candidate) {
            if (k in counts) continue
            if (n < 2) continue
            val name = display[k] ?: continue
            val nrx = Regex("(?<![\\p{L}])" + Regex.escape(name) + "(?![\\p{L}])")
            val total = paras.sumOf { p -> if (p.contains(name)) nrx.findAll(p).count() else 0 }
            if (total >= 2) counts[k] = total
        }
        if (counts.isEmpty()) return emptyList()

        val labelOf = (terms + phrases).associate { it.key to it.label }
        val globalCount = (terms + phrases).associate { it.key to it.count }
        val important = (terms.take(120) + phrases).map { it.key }.toSet()
        val bySent = sentences.groupBy { it.para }

        return counts.entries.sortedByDescending { it.value }.take(25).mapNotNull { (k, n) ->
            val name = display[k] ?: k
            val short = KNOWN[k]?.substringAfterLast(' ') ?: name
            val rx = Regex("(?<![\\p{L}])" + Regex.escape(short) + "(?![\\p{L}])")
            val mentionSents = sentences.filter { it.text.contains(short) && rx.containsMatchIn(it.text) }
            if (mentionSents.isEmpty()) return@mapNotNull null
            val paraSet = mentionSents.map { it.para }.toSet()
            // Conceptos: los que aparecen en los párrafos donde se habla del autor
            val local = HashMap<String, Int>()
            for (pi in paraSet) bySent[pi]?.forEach { s -> s.keys.forEach { key -> if (key in important && key != k) local[key] = (local[key] ?: 0) + 1 } }
            val concepts = local.entries
                .filter { deaccent(it.key) != deaccent(k) && !short.lowercase().contains(it.key) }
                .sortedByDescending { it.value / sqrt((globalCount[it.key] ?: 1).toDouble()) * (1 + it.value) }
                .take(8).map { it.key }
            val yrx = Regex(Regex.escape(short) + "\\s?\\(?(1[5-9]\\d\\d|20[0-2]\\d)\\)?")
            paras.forEach { p ->
                if (p.contains(short)) yrx.findAll(p).forEach { m ->
                    years.getOrPut(k) { LinkedHashSet() }.add(m.groupValues[1])
                }
            }
            AuthorInfo(
                key = k, name = name, mentions = n,
                years = years[k].orEmpty().sorted().take(6),
                concepts = concepts, conceptLabels = concepts.map { labelOf[it] ?: it },
                sentences = mentionSents.sortedByDescending { s -> s.keys.count { it in concepts } }.take(8).sortedBy { it.para },
                paras = paraSet,
            )
        }
    }

    /** Cuadro comparativo entre autores: los conceptos que tratan y qué dice cada uno. */
    fun compare(authors: List<AuthorInfo>, sentences: List<Sent>, labelOf: (String) -> String): List<CompareRow> {
        if (authors.isEmpty()) return emptyList()
        val shared = HashMap<String, Int>()
        authors.forEach { a -> a.concepts.forEachIndexed { i, k -> shared[k] = (shared[k] ?: 0) + 10 + (8 - i) } }
        val rows = shared.entries.sortedByDescending { e -> e.value + 100 * authors.count { e.key in it.concepts } }.take(8).map { it.key }
        return rows.map { key ->
            CompareRow(labelOf(key), authors.map { a ->
                val short = KNOWN[a.key]?.substringAfterLast(' ') ?: a.name
                val inParas = sentences.filter { it.para in a.paras && key in it.keys }
                inParas.firstOrNull { it.text.contains(short) } ?: inParas.minByOrNull { it.text.length }
            })
        }
    }

    private val YEAR = Regex("(?<![\\d.,/-])(1[0-9]\\d\\d|20[0-4]\\d)(?![\\d.,]\\d)")
    private val NOT_YEAR_BEFORE = Regex("(?:art[íi]culo|art\\.|n[°º.]|no\\.|n[úu]mero|p[áa]g(?:ina)?\\.?|pp?\\.|decreto|ley|acuerdo|expediente|exp\\.)\\s*$", RegexOption.IGNORE_CASE)
    private val CITES = Regex("\\(([^()]*\\d{4}[^()]*)\\)")
    private val CENTURY = Regex("(?<![\\p{L}])siglo\\s+([XVI]{1,5})(?![\\p{L}])", RegexOption.IGNORE_CASE)

    private fun roman(s: String): Int {
        val v = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100)
        var t = 0
        for (i in s.indices) { val a = v[s[i]] ?: 0; val b = if (i + 1 < s.length) v[s[i + 1]] ?: 0 else 0; t += if (a < b) -a else a }
        return t
    }

    /** Fechas del texto ordenadas: años y siglos con la oración donde aparecen. */
    fun timeline(sentences: List<Sent>, authors: List<AuthorInfo>, maxYear: Int = 2100): List<TimeEvent> {
        val out = ArrayList<TimeEvent>()
        val authorRx = authors.map { a -> a.name to Regex("(?<![\\p{L}])" + Regex.escape(KNOWN[a.key]?.substringAfterLast(' ') ?: a.name) + "(?![\\p{L}])") }
        val shorts = authors.map { a -> KNOWN[a.key]?.substringAfterLast(' ') ?: a.name }
        for (s in sentences) {
            if (s.text.length > 500) continue
            // Solo oraciones con algún dígito o con "siglo"
            val hasDigit = s.text.any { it in '0'..'9' }
            if (!hasDigit && !s.text.contains("iglo")) continue
            val who = authorRx.withIndex().firstOrNull { (i, r) -> s.text.contains(shorts[i]) && r.second.containsMatchIn(s.text) }?.value?.first
            val ys = YEAR.findAll(s.text).filter { m ->
                val y = m.value.toInt()
                y in 1000..maxYear && !NOT_YEAR_BEFORE.containsMatchIn(s.text.substring(0, m.range.first).takeLast(14))
            }.map { it.value.toInt() }.distinct().toList()
            // Oraciones que solo citan (muchos años entre paréntesis) no son hechos
            val cites = if (ys.size >= 3) CITES.findAll(s.text).count() else 0
            if (ys.isNotEmpty() && cites >= 2 && ys.size >= 3) continue
            ys.take(2).forEach { y -> out.add(TimeEvent(y, y.toString(), s.text, s.para, who)) }
            CENTURY.findAll(s.text).take(1).forEach { m ->
                val n = roman(m.groupValues[1].uppercase())
                if (n in 1..22 && ys.isEmpty()) out.add(TimeEvent((n - 1) * 100 + 50, "Siglo ${m.groupValues[1].uppercase()}", s.text, s.para, who))
            }
        }
        return out.distinctBy { it.label + "|" + it.text }.sortedWith(compareBy({ it.year }, { it.para })).take(150)
    }
}

/** Palabras clave de un texto y cuánto de ellas usaste en tu explicación (modo Feynman y examen oral). */
object KeyWords {
    private val WORD = Regex("[\\p{L}][\\p{L}'’-]*")

    private fun stem(w: String): String {
        val d = Social.deaccent(w.lowercase())
        return if (d.length > 6) d.take(6) else d.trimEnd('s')
    }

    /** Las palabras más informativas de [reference], sin palabras vacías. [extra] son conceptos que conviene incluir. */
    fun of(reference: String, extra: List<String> = emptyList(), max: Int = 8): List<String> {
        val counts = LinkedHashMap<String, Pair<String, Int>>()
        for (m in WORD.findAll(reference)) {
            val w = m.value
            val low = w.lowercase()
            if (low.length < 4 || low in Analyzer.STOP) continue
            val k = stem(low)
            val prev = counts[k]
            counts[k] = (prev?.first ?: low) to ((prev?.second ?: 0) + 1)
        }
        val ranked = counts.values.sortedByDescending { it.second * 10 + it.first.length }.map { it.first }
        return (extra.map { it.lowercase() } + ranked).distinctBy { stem(it.split(' ').first()) }.take(max)
    }

    /** Qué palabras clave aparecen en [answer] (comparando por raíz, sin tildes). */
    fun coverage(answer: String, keys: List<String>): Pair<List<String>, List<String>> {
        val have = WORD.findAll(answer).map { stem(it.value) }.toSet()
        val hit = keys.filter { k -> k.split(' ').filter { it.length >= 4 && it !in Analyzer.STOP }.ifEmpty { listOf(k) }.all { stem(it) in have } }
        return hit to (keys - hit.toSet())
    }
}

/** Referencias y citas en formato APA (7.ª edición). */
object Apa {
    /** "Max Weber; Émile Durkheim" → lista de (apellido, iniciales). Acepta también "Weber, Max". */
    fun people(authors: String): List<Pair<String, String>> =
        authors.split(Regex("\\s*(?:;|\\sy\\s|&|\\n)\\s*")).map { it.trim() }.filter { it.isNotEmpty() }.map { a ->
            if (a.contains(',')) {
                val (last, first) = a.split(',', limit = 2).map { it.trim() }
                last to initials(first)
            } else {
                val parts = a.split(Regex("\\s+"))
                if (parts.size == 1) parts[0] to ""
                else {
                    // Partículas: "de Tocqueville", "von Savigny"
                    var i = parts.size - 1
                    while (i > 1 && parts[i - 1].lowercase() in setOf("de", "von", "van", "del", "da", "di")) i--
                    parts.subList(i, parts.size).joinToString(" ") to initials(parts.subList(0, i).joinToString(" "))
                }
            }
        }

    private fun initials(first: String) = first.split(Regex("[\\s-]+")).filter { it.isNotEmpty() && it[0].isLetter() }
        .joinToString(" ") { it[0].uppercaseChar() + "." }

    fun reference(authors: String, year: String, title: String, publisher: String, url: String): String {
        val ps = people(authors)
        val who = when {
            ps.isEmpty() -> ""
            ps.size == 1 -> fmt(ps[0])
            ps.size <= 20 -> ps.dropLast(1).joinToString(", ") { fmt(it) } + ", & " + fmt(ps.last())
            else -> ps.take(19).joinToString(", ") { fmt(it) } + ", … " + fmt(ps.last())
        }
        val y = year.trim().ifBlank { "s.f." }
        val t = title.trim().trimEnd('.')
        return buildString {
            if (who.isNotEmpty()) append(who).append(if (who.endsWith(".")) " " else ". ")
            append("(").append(y).append("). ")
            append(t).append(". ")
            if (publisher.isNotBlank()) append(publisher.trim().trimEnd('.')).append(". ")
            if (url.isNotBlank()) append(url.trim())
        }.trim()
    }

    private fun fmt(p: Pair<String, String>) = if (p.second.isBlank()) p.first else "${p.first}, ${p.second}"

    /** Cita dentro del texto: (Weber, 1922, p. 15). */
    fun inText(authors: String, year: String, loc: String): String {
        val ps = people(authors)
        val who = when {
            ps.isEmpty() -> "Anónimo"
            ps.size == 1 -> ps[0].first
            ps.size == 2 -> ps[0].first + " y " + ps[1].first
            else -> ps[0].first + " et al."
        }
        return "(" + who + ", " + year.trim().ifBlank { "s.f." } + (if (loc.isNotBlank()) ", $loc" else "") + ")"
    }

    /** Cita textual completa: «…» (Autor, año, p. X). Las de 40 palabras o más van en bloque, sin comillas. */
    fun quote(text: String, authors: String, year: String, loc: String): String {
        val t = text.trim()
        val words = t.split(Regex("\\s+")).size
        return if (words >= 40) "$t ${inText(authors, year, loc)}" else "“$t” ${inText(authors, year, loc)}"
    }
}
