package com.hilo.lector.text

import kotlin.math.ln

private val DEFS_START_RX = Regex("(?i)(para\\s+(los\\s+)?efectos|se\\s+entender[áa]|definiciones)")

/** «artículo 45 del Código Civil»: una remisión a otra norma. */
data class CrossRef(val article: String, val law: String, val para: Int, val sentence: String)

/** Una definición legal: «Para efectos de esta ley se entiende por X: …». */
data class LegalDef(val term: String, val definition: String, val para: Int)

/** Algo que se repite en el área: una autoridad o institución, con cuántas veces aparece. */
data class Named(val name: String, val count: Int)

/** Una parte de los documentos modelo (demanda, contrato…) y en cuántos modelos aparece. */
data class TemplatePart(val heading: String, val inModels: Int, val example: String)

/**
 * Lo que Hilo aprende de un conjunto de documentos de un mismo campo (un «área de referencia»):
 * remisiones entre normas, definiciones legales, autoridades, sanciones, vocabulario propio y plantillas.
 */
object Reference {
    private val LAWNAME = "((?:la\\s+|el\\s+)?(?:Constituci[óo]n(?:\\s+de\\s+la\\s+Rep[úu]blica)?|C[óo]digo\\s+[\\p{L} ]{3,40}?|Ley\\s+[\\p{L}0-9 ,]{3,70}?|Reglamento\\s+[\\p{L} ]{3,60}?|Decreto\\s+(?:Legislativo|Ejecutivo)?\\s*(?:No\\.?|N[úu]mero)?\\s*[0-9-]+(?:-[0-9]{2,4})?|Convenci[óo]n\\s+[\\p{L} ]{3,60}?|Pacto\\s+[\\p{L} ]{3,60}?))"
    private val XREF = Regex(
        "(?i)\\b(?:art[íi]culos?|arts?\\.)\\s+(\\d{1,4}(?:\\s?(?:bis|ter))?)(?:\\s*(?:,|y|al|a)\\s*\\d{1,4})*\\s+(?:de\\s+(?:la|el)|del|de)\\s+" + LAWNAME + "(?=[,.;:)]|\\s+(?:y|que|en|para|sobre|vigente)\\b)",
    )

    /** Remisiones a artículos de OTRAS normas, con el nombre de la norma. */
    fun crossRefs(paras: List<String>): List<CrossRef> {
        val out = ArrayList<CrossRef>()
        paras.forEachIndexed { i, p ->
            if (p.length < 20) return@forEachIndexed
            XREF.findAll(p).forEach { m ->
                val law = m.groupValues[2].trim().replace(Regex("^(?i)(la|el)\\s+"), "").replace(com.hilo.lector.text.WS_RX, " ")
                if (law.length < 6) return@forEach
                val start = p.lastIndexOfAny(charArrayOf('.', ';'), m.range.first).let { if (it < 0) 0 else it + 1 }
                val end = p.indexOfAny(charArrayOf('.', ';'), m.range.last).let { if (it < 0) p.length else it + 1 }
                out.add(CrossRef(m.groupValues[1].replace(" ", ""), law, i, p.substring(start, end).trim().take(320)))
            }
        }
        return out
    }

    private fun fold(s: String) = Smart.fold(s)
    private val NAME_STOP = setOf("de", "la", "el", "del", "los", "las", "y", "para", "en", "ley", "codigo", "republica", "honduras")

    /** ¿Cuál de tus documentos es la norma nombrada? (por las palabras importantes del nombre). */
    fun resolve(lawName: String, titles: Map<String, String>): String? {
        val f = fold(lawName)
        val key = Regex("[a-z0-9]+").findAll(f).map { it.value }.filter { it !in NAME_STOP && it.length > 2 }.toList()
        val kind = when {
            f.startsWith("constitucion") -> "constitucion"
            f.startsWith("codigo") -> "codigo"
            f.startsWith("reglamento") -> "reglamento"
            f.startsWith("ley") -> "ley"
            else -> ""
        }
        var best: String? = null
        var bestScore = 0.0
        for ((id, t) in titles) {
            val ft = fold(t)
            if (kind.isNotEmpty() && !ft.contains(kind)) continue
            val hit = key.count { ft.contains(it) }
            val score = if (key.isEmpty()) (if (kind == "constitucion") 1.0 else 0.0) else hit.toDouble() / key.size
            if (score > bestScore) { bestScore = score; best = id }
        }
        return if (bestScore >= 0.6) best else null
    }

    private val DEF1 = Regex("(?i)se\\s+entender[áa]n?\\s+por\\s+([^:,;.]{2,70})[,:]\\s*([^.;]{15,400})")
    private val DEF2 = Regex("(?i)se\\s+entiende(?:n)?\\s+por\\s+([^:,;.]{2,70})[,:]?\\s+(?:el|la|los|las|todo|toda|aquel|aquella|cualquier)\\s+([^.;]{15,400})")
    private val DEF3 = Regex("^\\s*(?:\\d+\\)|[a-zñ]\\)|[IVX]+\\.|-)?\\s*([A-ZÁÉÍÓÚÑ][\\p{L} ]{2,50}):\\s+([^.]{15,400})")

    /** Definiciones que trae la propia norma («Para efectos de esta Ley se entiende por…» y listas «Término: definición»). */
    fun legalDefinitions(paras: List<String>): List<LegalDef> {
        val out = ArrayList<LegalDef>()
        var inDefs = 0
        paras.forEachIndexed { i, p ->
            if (DEFS_START_RX.containsMatchIn(p)) inDefs = 12
            DEF1.findAll(p).forEach { out.add(LegalDef(it.groupValues[1].trim(), it.groupValues[2].trim(), i)) }
            DEF2.findAll(p).forEach { out.add(LegalDef(it.groupValues[1].trim(), (it.value.substringAfter(it.groupValues[1]).trimStart(',', ':', ' ')).trim(), i)) }
            if (inDefs > 0) {
                DEF3.find(p)?.let { out.add(LegalDef(it.groupValues[1].trim(), it.groupValues[2].trim(), i)) }
                inDefs--
            }
        }
        return out.distinctBy { fold(it.term) }
    }

    private val AUTH = Regex("\\b((?:Secretar[íi]a|Ministerio|Juzgado|Tribunal|Corte|Sala|Procuradur[íi]a|Fiscal[íi]a|Ministerio\\s+P[úu]blico|Instituto|Comisi[óo]n|Comisionado|Superintendencia|Direcci[óo]n|Consejo|Congreso|Registro|Alcald[íi]a|Municipalidad|Polic[íi]a|Tribunal\\s+Superior)(?:\\s+(?:de|del|de\\s+la|para|en)?\\s*[A-ZÁÉÍÓÚÑ][\\p{L}]+){1,5})")

    fun authorities(paras: List<String>): List<Named> {
        val cnt = HashMap<String, Int>()
        for (p in paras) AUTH.findAll(p).forEach { m ->
            val k = m.value.trim().replace(com.hilo.lector.text.WS_RX, " ")
            cnt[k] = (cnt[k] ?: 0) + 1
        }
        return cnt.entries.filter { it.value >= 2 }.sortedByDescending { it.value }.take(40).map { Named(it.key, it.value) }
    }

    private val SANC = Regex("(?i)\\b(multa|sanci[óo]n|sancionad[oa]|pena\\s+de|reclusi[óo]n|prisi[óo]n|inhabilitaci[óo]n|suspensi[óo]n|clausura|destituci[óo]n|amonestaci[óo]n|cancelaci[óo]n)\\b")

    /** Oraciones que establecen sanciones o penas. */
    fun sanctions(paras: List<String>, limit: Int = 60): List<Pair<Int, String>> {
        val out = ArrayList<Pair<Int, String>>()
        paras.forEachIndexed { i, p ->
            if (out.size >= limit) return@forEachIndexed
            p.split(Regex("(?<=[.;])\\s+")).forEach { s -> if (out.size < limit && s.length in 40..420 && SANC.containsMatchIn(s)) out.add(i to s.trim()) }
        }
        return out
    }

    /**
     * Vocabulario propio del área: palabras que en estos documentos aparecen mucho más
     * que en el resto de tu biblioteca (lo que hace «técnico» a un texto).
     */
    fun vocabulary(area: List<Analysis>, rest: List<Analysis>, limit: Int = 80): List<Pair<Term, Float>> {
        val a = HashMap<String, Int>(); val labels = HashMap<String, String>()
        var aTotal = 0
        for (x in area) { aTotal += x.totalWords; for (t in x.terms) { a[t.key] = (a[t.key] ?: 0) + t.count; labels.putIfAbsent(t.key, t.label) } }
        val r = HashMap<String, Int>()
        var rTotal = 0
        for (x in rest) { rTotal += x.totalWords; for (t in x.terms) r[t.key] = (r[t.key] ?: 0) + t.count }
        val inDocs = HashMap<String, Int>()
        for (x in area) x.terms.forEach { inDocs[it.key] = (inDocs[it.key] ?: 0) + 1 }
        return a.entries.filter { it.value >= 3 }.map { (k, c) ->
            val pa = c.toFloat() / aTotal.coerceAtLeast(1)
            val pr = ((r[k] ?: 0) + 0.5f) / (rTotal + 20_000f)
            val keyness = (pa / pr) * (1f + ln(1f + (inDocs[k] ?: 1)))
            Term(k, labels.getValue(k), c, false) to keyness
        }.sortedByDescending { it.second }.take(limit)
    }

    private val HEAD_WORDS = Regex("(?i)^(hechos|fundamentos?\\s+de\\s+derecho|fundamentos?|petici[óo]n|petitorio|pruebas|antecedentes|considerando|por\\s+tanto|resultando|fallo|cl[áa]usula\\s+\\w+|objeto|precio|plazo|vigencia|partes|comparecientes|declaraciones|competencia|legitimaci[óo]n|de\\s+las\\s+partes|introducci[óo]n|conclusi[óo]n(?:es)?|referencias|bibliograf[íi]a|m[ée]todo|resultados|discusi[óo]n|resumen|abstract|anexos?)\\b")

    private fun isHeading(p: String): Boolean {
        val t = p.removePrefix("# ").trim().trimEnd(':', '.')
        if (t.length !in 3..70) return false
        if (p.startsWith("# ")) return true
        val letters = t.filter { it.isLetter() }
        val upper = letters.count { it.isUpperCase() }
        return (letters.length >= 4 && upper >= letters.length * 0.8) || HEAD_WORDS.containsMatchIn(t) && t.split(' ').size <= 6
    }

    private fun canon(h: String) = fold(h.removePrefix("# ").trim().trimEnd(':', '.')).replace(Regex("^(primero|segundo|tercero|cuarto|quinto|[ivx]+|\\d+)[.)\\-:]?\\s+"), "").replace(com.hilo.lector.text.WS_RX, " ")

    /** Partes que se repiten en varios documentos modelo, en el orden en que suelen ir. */
    fun template(models: List<List<String>>): List<TemplatePart> {
        val seen = HashMap<String, Int>()
        val pos = HashMap<String, Double>()
        val example = HashMap<String, String>()
        for (paras in models) {
            val heads = paras.withIndex().filter { isHeading(it.value) }
            val keys = heads.map { canon(it.value) }.distinct()
            keys.forEach { seen[it] = (seen[it] ?: 0) + 1 }
            heads.forEach { (i, h) ->
                val k = canon(h)
                pos[k] = (pos[k] ?: 0.0) + i.toDouble() / paras.size.coerceAtLeast(1)
                if (!example.containsKey(k)) example[k] = paras.drop(i + 1).firstOrNull { !isHeading(it) && it.length > 30 }.orEmpty().take(240)
            }
        }
        val min = if (models.size >= 3) 2 else 1
        return seen.entries.filter { it.value >= min }.sortedBy { (pos[it.key] ?: 0.0) / it.value }
            .map { TemplatePart(it.key.replaceFirstChar { c -> c.uppercase() }, it.value, example[it.key].orEmpty()) }.take(25)
    }

    /** Palabras clave de un texto (para buscar pasajes parecidos en el área). */
    fun keywords(text: String, n: Int = 7): List<String> {
        val freq = HashMap<String, Int>()
        Regex("[\\p{L}]{4,}").findAll(text.lowercase()).forEach { m -> if (!Analyzer.isStop(m.value)) freq[m.value] = (freq[m.value] ?: 0) + 1 }
        return freq.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key.length }).take(n).map { fold(it.key).let { k -> if (k.length > 5 && k.endsWith("s")) k.dropLast(1) else k } }
    }
}
