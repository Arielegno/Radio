package com.hilo.lector.text

/** Un artículo de una ley o código. [para] es el párrafo donde empieza. */
data class LawArticle(
    val num: String,
    val title: String,
    val para: Int,
    val text: String,
    val path: List<String>,
    val refs: List<String>,
) : java.io.Serializable {
    val label: String get() = "Art. $num"
}

/** Libro, título, capítulo o sección. [level] 0 = el más grande. */
data class LawHeading(val level: Int, val label: String, val para: Int) : java.io.Serializable

/** Un plazo que aparece en el texto: "dentro de tres días hábiles". */
data class Deadline(val amount: String, val phrase: String, val article: String?, val para: Int, val sentence: String) : java.io.Serializable

class LawIndex(val articles: List<LawArticle>, val headings: List<LawHeading>, val deadlines: List<Deadline>) : java.io.Serializable {
    val isLaw: Boolean get() = articles.size >= 3
    private val byNum = HashMap<String, LawArticle>().apply { articles.forEach { putIfAbsent(Law.normNum(it.num), it) } }
    fun find(num: String): LawArticle? = byNum[Law.normNum(num)]

    /** Artículos que quedan bajo un encabezado (hasta el siguiente del mismo nivel o mayor). */
    fun articlesUnder(h: LawHeading): List<LawArticle> {
        val next = headings.firstOrNull { it.para > h.para && it.level <= h.level }?.para ?: Int.MAX_VALUE
        return articles.filter { it.para >= h.para && it.para < next }
    }
}

/**
 * Reconoce la estructura de leyes y códigos: libros, títulos, capítulos y artículos,
 * las remisiones entre artículos ("conforme al artículo 12") y los plazos.
 */
object Law {
    private const val NUM = "(\\d{1,4}(?:\\s?(?i:bis|ter|qu[áa]ter|quinquies)(?![\\p{L}]))?(?:\\s?-?\\s?[A-Z](?![\\p{L}]))?)"
    // Inicio de artículo: al principio del párrafo o después de un punto. Lleva separador: "Artículo 12.-", "ARTÍCULO 12:", "Art. 12."
    private val ART = Regex(
        "(?:^|(?<=[.;:]\\s{1,3}))(?:#\\s*)?(?:ART[ÍI]CULO|Art[íi]culo|ARTICULO|ART\\.|Art\\.)\\s*$NUM\\s*(?:[.:°º]+\\s*[-–—]*|[-–—]+|\\s(?=[\\p{Lu}]))",
    )
    private val ORD = Regex(
        "^(?:#\\s*)?(?:ART[ÍI]CULO|Art[íi]culo)\\s+(PRIMERO|SEGUNDO|TERCERO|CUARTO|QUINTO|SEXTO|S[ÉE]PTIMO|OCTAVO|NOVENO|D[ÉE]CIMO|[ÚU]NICO)\\b\\s*[.:\\-–—]*",
        RegexOption.IGNORE_CASE,
    )
    private val HEAD = Regex(
        "^(?:#\\s*)?(LIBRO|T[ÍI]TULO|CAP[ÍI]TULO|SECCI[ÓO]N|PARTE)\\s+([\\p{L}\\d]+)\\b[.:\\-–—\\s]*(.*)$",
        RegexOption.IGNORE_CASE,
    )
    // Remisiones: "artículo 12", "artículos 3, 5 y 8". Se recorren número por número (sin repeticiones anidadas,
    // que con listas muy largas desbordan la pila).
    private val REF_HEAD = Regex("(?<![\\p{L}])(?:art[íi]culos?|arts?\\.)\\s+(?=\\d)", RegexOption.IGNORE_CASE)
    private val REF_ITEM = java.util.regex.Pattern.compile(
        "(\\d{1,4}(?:\\s?(?:bis|ter))?)(?:\\s{0,3}(?:,|y|e|o|al|a|hasta)\\s{0,3}(?=\\d))?",
        java.util.regex.Pattern.CASE_INSENSITIVE,
    )

    private fun scanRefs(text: String, out: MutableCollection<String>, limit: Int = 400) {
        val m = REF_ITEM.matcher(text)
        for (h in REF_HEAD.findAll(text)) {
            var pos = h.range.last + 1
            var n = 0
            while (pos < text.length && n < 200 && out.size < limit) {
                m.region(pos, text.length)
                if (!m.lookingAt() || m.end() == pos) break
                out.add(m.group(1).replace(" ", ""))
                pos = m.end()
                n++
            }
            if (out.size >= limit) return
        }
    }

    fun normNum(n: String): String {
        val k = n.lowercase().replace(Regex("[\\s-]"), "")
        val t = k.trimStart('0')
        return if (t.isEmpty() || !t[0].isDigit()) k else t
    }

    private fun levelOf(word: String): Int = when (word.lowercase().replace('í', 'i').replace('ó', 'o')) {
        "parte" -> 0
        "libro" -> 1
        "titulo" -> 2
        "capitulo" -> 3
        else -> 4
    }

    private fun isUpperish(s: String): Boolean {
        val letters = s.filter { it.isLetter() }
        return letters.length >= 3 && letters.count { it.isUpperCase() } >= letters.length * 0.7
    }

    fun parse(paras: List<String>): LawIndex {
        data class Start(val para: Int, val off: Int, val end: Int, val num: String)
        val starts = ArrayList<Start>()
        val headings = ArrayList<LawHeading>()
        val headParas = HashSet<Int>()
        for ((pi, raw) in paras.withIndex()) {
            val p = raw.trim()
            val h = HEAD.find(p)
            if (h != null && p.length < 160 && !ART.containsMatchIn(p.take(30))) {
                val word = h.groupValues[1]
                var rest = h.groupValues[3].trim()
                // El nombre suele estar en el párrafo siguiente: "TÍTULO II" / "DE LAS PERSONAS"
                if (rest.isEmpty()) {
                    val nx = paras.getOrNull(pi + 1)?.trim()?.removePrefix("# ").orEmpty()
                    if (nx.length in 3..120 && (isUpperish(nx) || (nx.length <= 80 && !nx.endsWith(".") && nx.split(' ').size <= 10)) && !ART.containsMatchIn(nx) && HEAD.find(nx) == null) rest = nx
                }
                val label = (word.lowercase().replaceFirstChar { it.uppercase() } + " " + h.groupValues[2] + if (rest.isNotEmpty()) " · " + rest.lowercase().replaceFirstChar { it.uppercase() } else "").take(110)
                headings.add(LawHeading(levelOf(word), label, pi))
                headParas.add(pi)
                continue
            }
            ORD.find(p)?.let { m -> starts.add(Start(pi, 0, m.range.last + 1, m.groupValues[1].lowercase().replaceFirstChar { it.uppercase() })) }
            for (m in ART.findAll(raw)) starts.add(Start(pi, m.range.first, m.range.last + 1, m.groupValues[1].trim()))
        }
        // Normaliza niveles: los que existen pasan a 0, 1, 2…
        val levels = headings.map { it.level }.distinct().sorted()
        val heads = headings.map { it.copy(level = levels.indexOf(it.level)) }

        val articles = ArrayList<LawArticle>()
        for ((i, s) in starts.withIndex()) {
            val next = starts.getOrNull(i + 1)
            val sb = StringBuilder()
            var p = s.para
            var off = s.end
            while (p < paras.size && sb.length < 6000) {
                if (p != s.para && p in headParas) break
                val txt = paras[p]
                if (next != null && p == next.para) {
                    if (next.off > off) sb.append(txt.substring(off, next.off))
                    break
                }
                if (off < txt.length) sb.append(txt.substring(off))
                sb.append("\n")
                p++
                off = 0
                if (next == null && p - s.para > 12) break
            }
            val body = sb.toString().trim().removePrefix("# ").trim()
            val title = Regex("^([^.\\n]{3,70})\\.\\s+\\S").find(body)?.groupValues?.get(1)?.takeIf { t ->
                t.split(' ').size <= 8 && t.first().isUpperCase()
            }.orEmpty()
            // "artículo 5 de la Ley de…" remite a otra ley: igual se muestra, se aclara en la vista
            val refs = LinkedHashSet<String>()
            scanRefs(body, refs, 60)
            refs.removeAll { normNum(it) == normNum(s.num) }
            val path = heads.filter { it.para <= s.para }.let { hs ->
                val out = ArrayList<LawHeading>()
                for (h in hs) { while (out.isNotEmpty() && out.last().level >= h.level) out.removeAt(out.size - 1); out.add(h) }
                out.map { it.label }
            }
            articles.add(LawArticle(s.num, title, s.para, body, path, refs.toList()))
        }
        val deadlines = findDeadlines(paras, if (articles.size >= 3) articles else emptyList())
        return LawIndex(articles, heads, deadlines)
    }

    private const val AMOUNT = "(?:\\d{1,3}|un|una|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce|trece|catorce|quince|diecis[ée]is|diecisiete|dieciocho|diecinueve|veinte|veinti\\p{L}+|treinta|cuarenta|cincuenta|sesenta|noventa|cien|ciento\\s+\\p{L}+|(?:treinta|cuarenta|cincuenta|sesenta|setenta|ochenta|noventa)\\s+y\\s+\\p{L}+)"
    private val DEADLINE = Regex(
        "(?<![\\p{L}])(dentro\\s+(?:de\\s+)?(?:(?:el|los|las)\\s+)?(?:(?:plazo|t[ée]rmino)\\s+de\\s+)?(?:(?:los|las)\\s+)?|en\\s+(?:el\\s+|un\\s+)?(?:plazo|t[ée]rmino)\\s+(?:m[áa]ximo\\s+|perentorio\\s+|improrrogable\\s+)?de\\s+(?:(?:los|las)\\s+)?|(?:plazo|t[ée]rmino)\\s+(?:m[áa]ximo\\s+|perentorio\\s+|improrrogable\\s+|legal\\s+)?de\\s+(?:(?:los|las)\\s+)?|prescrib\\p{L}*\\s+(?:a\\s+los|en|al\\s+cabo\\s+de|por)\\s+|caduca\\p{L}*\\s+(?:a\\s+los|en)\\s+|a\\s+m[áa]s\\s+tardar\\s+(?:en\\s+|dentro\\s+de\\s+)?(?:(?:los|las)\\s+)?|no\\s+(?:podr[áa]\\s+)?exceder\\s+de\\s+|hasta\\s+(?:por\\s+)?)(" + AMOUNT + "(?:\\s*\\(\\d{1,3}\\))?\\s+(?:d[íi]as?(?:\\s+h[áa]biles|\\s+calendario|\\s+naturales|\\s+corridos)?|mes(?:es)?|a[ñn]os?|horas?|semanas?))",
        RegexOption.IGNORE_CASE,
    )

    private val DEADLINE_HINTS = listOf("plazo", "término", "termino", "dentro", "prescrib", "caduc", "tardar", "exceder", "hasta")

    private fun findDeadlines(paras: List<String>, articles: List<LawArticle>): List<Deadline> {
        val out = ArrayList<Deadline>()
        fun scan(text: String, para: Int, art: String?) {
            val low = text.lowercase()
            if (DEADLINE_HINTS.none { low.contains(it) }) return
            for (m in DEADLINE.findAll(text)) {
                val lead = m.groupValues[1].trim().lowercase()
                // "hasta" solo cuenta en leyes (en otros textos suele ser "hasta dos años después")
                if (lead.startsWith("hasta") && art == null) continue
                val (a, b) = sentenceBounds(text, m.range.first)
                val sent = text.substring(a, b).trim()
                out.add(Deadline(m.groupValues[2].replace(Regex("\\s+"), " ").trim(), m.value.trim(), art, para, if (sent.length > 420) sent.take(420) + "…" else sent))
                if (out.size >= 300) return
            }
        }
        if (articles.isNotEmpty()) articles.forEach { scan(it.text, it.para, it.num) }
        else paras.forEachIndexed { i, p -> scan(p, i, null) }
        return out.distinctBy { it.sentence + it.amount }
    }

    private fun sentenceBounds(t: String, at: Int): Pair<Int, Int> {
        var a = at
        while (a > 0 && !(t[a - 1] == '.' && (a >= t.length || t[a].isWhitespace())) && t[a - 1] != '\n') a--
        var b = at
        while (b < t.length && !(t[b] == '.' && (b + 1 >= t.length || t[b + 1].isWhitespace())) && t[b] != '\n') b++
        return a to minOf(t.length, b + 1)
    }

    /** Números de artículo mencionados en un texto ("según el artículo 12 y 13"). */
    fun refsIn(text: String): List<String> {
        val out = LinkedHashSet<String>()
        scanRefs(text, out, 40)
        return out.toList()
    }
}
