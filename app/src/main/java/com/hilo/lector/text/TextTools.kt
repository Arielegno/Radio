package com.hilo.lector.text

/**
 * Convierte texto con líneas partidas (PDF, fotos, texto copiado) en párrafos
 * que se leen de corrido. Los títulos se marcan con "# " al inicio.
 */
object TextTools {
    private val TERM = Regex("[.!?…:»\"”)\\]]$")
    private val WS = com.hilo.lector.text.WS_RX
    private val TRAIL_WS = Regex("[ \\t]+\\n")
    private val TWO_LETTERS = Regex("\\p{L}{2}")
    private val JUNK = Regex("^[^\\p{L}]{0,6}$")
    private val LOWER_START = Regex("^[a-záéíóúñü]")
    private val HYPHEN_END = Regex("\\p{L}-$")
    private val LOWER_ANY_START = Regex("^\\p{Ll}")
    private val NEW_PARA_START = Regex("^[\\p{Lu}¿¡\"“«—–\\-]")
    private val HEADING_MD = Regex("^#+\\s*")
    private val HYPHEN_BREAK = Regex("\\p{L}-\\n\\p{Ll}")
    private val BLANK_LINES = Regex("\\n\\s*\\n")
    private val DIGITS = Regex("\\d+")

    fun cleanRaw(t: String): String = t
        .replace("\r", "")
        .replace("­", "")
        .replace("ﬁ", "fi")
        .replace("ﬂ", "fl")
        .replace("\u000c", "\n\n")
        .replace(TRAIL_WS, "\n")

    /** Une líneas cortadas y palabras partidas con guion ("socio-" + "logía"). */
    fun reflow(raw: String): List<String> {
        val all = cleanRaw(raw).split("\n").map { it.replace(WS, " ").trim() }
        // Quita números de página y restos sueltos
        val lines = all.filter { x ->
            x.isEmpty() || x.startsWith("# ") || TWO_LETTERS.containsMatchIn(x) || !JUNK.matches(x)
        }
        val lens = lines.filter { it.length > 20 }.map { it.length }.sorted()
        val med = if (lens.isNotEmpty()) lens[(lens.size * 0.6).toInt().coerceAtMost(lens.size - 1)] else 60

        val out = mutableListOf<String>()
        val cur = StringBuilder()
        fun flush() {
            val s = cur.toString().trim()
            if (s.isNotEmpty()) out.add(s)
            cur.setLength(0)
        }

        for (i in lines.indices) {
            val ln = lines[i]
            if (ln.startsWith("# ")) { flush(); out.add(ln); continue }
            if (ln.isEmpty()) {
                // Línea en blanco: corta el párrafo salvo que la frase continúe (cambio de página)
                var nx = ""
                for (j in i + 1 until lines.size) if (lines[j].isNotEmpty()) { nx = lines[j]; break }
                if (cur.isNotEmpty() && !TERM.containsMatchIn(cur) && LOWER_START.containsMatchIn(nx)) continue
                flush(); continue
            }
            if (cur.isNotEmpty()) {
                if (HYPHEN_END.containsMatchIn(cur) && LOWER_ANY_START.containsMatchIn(ln)) {
                    cur.setLength(cur.length - 1)
                    cur.append(ln)
                } else {
                    cur.append(' ').append(ln)
                }
            } else {
                cur.append(ln)
            }
            val nx = lines.getOrElse(i + 1) { "" }
            if (TERM.containsMatchIn(ln) && ln.length < med * 0.8 &&
                (nx.isEmpty() || NEW_PARA_START.containsMatchIn(nx))
            ) flush()
        }
        flush()
        return out
    }

    /** Párrafos separados por línea en blanco (o por salto simple si no hay). */
    fun textToParas(t: String): List<String> {
        val c = t.replace("\r", "")
        var ps = c.split(BLANK_LINES)
        if (ps.size < 3) ps = c.split("\n")
        return ps.map { it.replace(WS, " ").trim() }
            .filter { it.isNotEmpty() }
            .map { if (it.startsWith("#")) it.replace(HEADING_MD, "# ") else it }
    }

    /** ¿Parece texto con líneas de ancho fijo (copiado de un PDF)? */
    fun isWrapped(t: String): Boolean {
        val ls = cleanRaw(t).split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (ls.size < 4) return false
        val lens = ls.map { it.length }.sorted()
        val med = lens[lens.size / 2]
        if (med > 140) return false
        val near = ls.count { it.length >= med * 0.7 && it.length <= med * 1.25 }
        val blanks = BLANK_LINES.findAll(t).count()
        return near.toFloat() / ls.size > 0.55f && blanks < ls.size * 0.5f
    }

    fun smartParas(t: String): List<String> =
        if (isWrapped(t) || HYPHEN_BREAK.containsMatchIn(t)) reflow(t) else textToParas(t)

    /** Quita encabezados o pies que se repiten en muchas páginas (título del libro, capítulo). */
    fun stripRepeats(pages: List<String>): List<String> {
        if (pages.size < 3) return pages
        fun norm(x: String) = x.lowercase().replace(DIGITS, "").replace(WS, " ").trim()
        fun edges(t: String): Pair<List<String>, Set<Int>> {
            val ls = t.split("\n")
            val ix = ls.indices.filter { ls[it].isNotBlank() }
            return ls to (ix.take(2) + ix.takeLast(2)).toSet()
        }
        val count = HashMap<String, Int>()
        for (p in pages) {
            val (ls, ix) = edges(p)
            ix.map { norm(ls[it]) }.filter { it.length > 2 }.toSet().forEach { count[it] = (count[it] ?: 0) + 1 }
        }
        val minHits = maxOf(3, (pages.size * 0.3f).toInt())
        val rep = count.filterValues { it >= minHits }.keys
        if (rep.isEmpty()) return pages
        return pages.map { p ->
            val (ls, ix) = edges(p)
            val kill = ix.filter { norm(ls[it]) in rep }.toSet()
            ls.filterIndexed { i, _ -> i !in kill }.joinToString("\n")
        }
    }

    fun firstWords(t: String, n: Int = 6): String {
        val w = t.split(WS).filter { it.isNotEmpty() }
        return w.take(n).joinToString(" ") + if (w.size > n) "…" else ""
    }

    fun words(paras: List<String>): List<String> =
        paras.flatMap { p -> (if (p.startsWith("# ")) p.substring(2) else p).split(WS).filter { it.isNotEmpty() } }
}
