package com.hilo.lector.text

import java.text.Normalizer
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/** Un concepto de toda la clase: en qué documentos aparece y a qué tema pertenece. */
data class ClassConcept(
    val key: String,
    val label: String,
    val total: Int,
    val docs: Map<String, Int>,     // libro → veces
    val score: Float,
    var topic: Int = 0,
)

data class ClassTopic(val id: Int, val name: String, val concepts: List<String>)

/** Una definición encontrada en un documento. */
data class ClassDef(val bookId: String, val term: String, val definition: String, val para: Int)

/** Una idea (oración) con su documento. */
data class Idea(val bookId: String, val text: String, val para: Int)

class ClassGraph(
    val concepts: List<ClassConcept>,
    val edges: List<Triple<Int, Int, Int>>,      // índice, índice, veces juntos
    val topics: List<ClassTopic>,
    val defs: Map<String, List<ClassDef>>,       // concepto → definiciones
) {
    val byKey: Map<String, Int> = concepts.withIndex().associate { it.value.key to it.index }
    fun neighbors(i: Int): List<Pair<Int, Int>> =
        edges.mapNotNull { (a, b, w) -> if (a == i) b to w else if (b == i) a to w else null }.sortedByDescending { it.second }
}

/** Pregunta de opción múltiple generada desde el material. */
data class Quiz(val prompt: String, val options: List<String>, val answer: Int, val explain: String, val kind: String)

/**
 * La «inteligencia» de Hilo, sin internet: elige las frases centrales de un texto (TextRank),
 * arma la red de conceptos de varios documentos, la agrupa en temas y crea preguntas.
 */
object Smart {
    private val WORD = Regex("[\\p{L}]{4,}")

    fun fold(s: String) = s.lowercase().let { l -> if (l.all { it.code < 128 }) l else Normalizer.normalize(l, Normalizer.Form.NFD).replace(com.hilo.lector.text.MARKS_RX, "") }

    private fun words(s: String): Set<String> =
        WORD.findAll(s.lowercase()).map { it.value }.filter { !Analyzer.isStop(it) }.map { if (it.length > 4 && it.endsWith("s")) it.dropLast(1) else it }.toSet()

    /**
     * TextRank: cada oración «vota» por las que se le parecen; ganan las que están en el centro del texto.
     * Devuelve los índices de las [n] mejores en su orden original, sin repetir ideas casi iguales.
     */
    fun rank(sentences: List<String>, n: Int, boost: Set<String> = emptySet()): List<Int> {
        if (sentences.isEmpty() || n <= 0) return emptyList()
        // Con muchas oraciones se toma una muestra pareja, para que sea rápido
        val idx = if (sentences.size <= 320) sentences.indices.toList()
        else (0 until 320).map { it * sentences.size / 320 }
        val ws = idx.map { words(sentences[it]) }
        val m = idx.size
        val sim = Array(m) { FloatArray(m) }
        for (i in 0 until m) for (j in i + 1 until m) {
            val a = ws[i]; val b = ws[j]
            if (a.size < 2 || b.size < 2) continue
            val common = if (a.size < b.size) a.count { it in b } else b.count { it in a }
            if (common == 0) continue
            val v = (common / (ln(a.size.toDouble()) + ln(b.size.toDouble()) + 1.0)).toFloat()
            sim[i][j] = v; sim[j][i] = v
        }
        val out = FloatArray(m) { r -> sim[r].sum() }
        var pr = FloatArray(m) { 1f }
        repeat(25) {
            val next = FloatArray(m) { 0.15f }
            for (j in 0 until m) {
                if (out[j] == 0f) continue
                val share = 0.85f * pr[j] / out[j]
                val row = sim[j]
                for (i in 0 until m) if (row[i] > 0f) next[i] += share * row[i]
            }
            pr = next
        }
        // Preferir oraciones de largo medio y las que tocan los conceptos pedidos
        val score = FloatArray(m) { i ->
            val len = sentences[idx[i]].length
            val lenF = if (len < 50) 0.6f else if (len > 300) 0.75f else 1f
            val b = if (boost.isEmpty()) 1f else 1f + 0.5f * ws[i].count { it in boost }
            pr[i] * lenF * b
        }
        val chosen = ArrayList<Int>()
        for (i in (0 until m).sortedByDescending { score[it] }) {
            if (chosen.size >= n) break
            val dup = chosen.any { c ->
                val a = ws[i]; val b = ws[c]
                val inter = a.count { it in b }
                inter > 0 && inter.toFloat() / (a.size + b.size - inter).coerceAtLeast(1) > 0.55f
            }
            if (!dup) chosen.add(i)
        }
        return chosen.map { idx[it] }.sorted()
    }

    /** Las [n] frases que mejor resumen estos párrafos. */
    fun summarize(texts: List<String>, n: Int): List<String> {
        val sents = texts.flatMap { it.split(Regex("(?<=[.!?…])\\s+")) }.map { it.trim() }.filter { it.length in 40..360 }
        if (sents.isEmpty()) return texts.map { it.take(220) }.filter { it.isNotBlank() }.take(n)
        return rank(sents, n).map { sents[it] }
    }

    // =================================================================
    // Red de conceptos de una clase (varios documentos)
    // =================================================================

    fun buildClass(docs: List<Pair<String, Analysis>>, max: Int = 48): ClassGraph {
        if (docs.isEmpty()) return ClassGraph(emptyList(), emptyList(), emptyList(), emptyMap())
        // Importancia: cuánto pesa en cada documento (por cada mil palabras) y en cuántos documentos aparece
        val per = HashMap<String, HashMap<String, Int>>()
        val labels = HashMap<String, String>()
        val isPhrase = HashMap<String, Boolean>()
        for ((id, a) in docs) {
            for (t in a.terms.take(200) + a.phrases.take(40)) {
                per.getOrPut(t.key) { HashMap() }[id] = t.count
                labels.putIfAbsent(t.key, t.label)
                isPhrase[t.key] = t.phrase
            }
        }
        val totals = docs.associate { (id, a) -> id to max(1, a.totalWords) }
        val nDocs = docs.size
        val scored = per.map { (k, m) ->
            val tf = m.entries.sumOf { (d, c) -> c * 1000.0 / totals.getValue(d) }
            val spread = 1.0 + ln(1.0 + m.size) * (if (nDocs > 1) 1.6 else 1.0)
            val bonus = if (isPhrase[k] == true) 1.35 else Analyzer.shape(k)
            ClassConcept(k, labels.getValue(k), m.values.sum(), m, (tf * spread * bonus).toFloat())
        }.filter { it.total >= 2 && it.key !in Analyzer.GENERIC }.sortedByDescending { it.score }
        // Si una frase («división del trabajo») ya está, no repetir sus palabras sueltas con menos peso
        val picked = ArrayList<ClassConcept>()
        for (c in scored) {
            if (picked.size >= max) break
            if (picked.any { p -> p.key.contains(' ') && c.key in p.key.split(' ') && c.score < p.score * 1.5f }) continue
            picked.add(c)
        }
        val index = picked.withIndex().associate { it.value.key to it.index }

        // Uniones: conceptos en la misma oración, en todos los documentos
        val pair = HashMap<Long, Int>()
        for ((_, a) in docs) for (s in a.sentences) {
            val ks = s.keys.mapNotNull { index[it] }.distinct().sorted()
            for (i in ks.indices) for (j in i + 1 until ks.size) {
                val key = ks[i].toLong() * 10_000 + ks[j]
                pair[key] = (pair[key] ?: 0) + 1
            }
        }
        val edges = pair.entries.filter { it.value >= 2 }.map { Triple((it.key / 10_000).toInt(), (it.key % 10_000).toInt(), it.value) }
            .sortedByDescending { it.third }.take(picked.size * 4)

        val topics = clusters(picked, edges)

        // Definiciones de cada concepto, en cada documento
        val defs = HashMap<String, MutableList<ClassDef>>()
        for ((id, a) in docs) for (g in a.glossary) {
            val f = fold(g.term)
            val c = picked.firstOrNull { fold(it.label) == f || fold(it.key) == f } ?: continue
            defs.getOrPut(c.key) { ArrayList() }.add(ClassDef(id, g.term, g.definition, g.para))
        }
        return ClassGraph(picked, edges, topics, defs)
    }

    /**
     * Agrupa los conceptos en temas: cada concepto adopta el tema que más pesa entre sus vecinos
     * (propagación de etiquetas), y los temas muy chicos se juntan con el más cercano.
     */
    private fun clusters(cs: List<ClassConcept>, edges: List<Triple<Int, Int, Int>>): List<ClassTopic> {
        val n = cs.size
        if (n == 0) return emptyList()
        val adj = Array(n) { HashMap<Int, Int>() }
        for ((a, b, w) in edges) { adj[a][b] = w; adj[b][a] = w }
        val label = IntArray(n) { it }
        repeat(20) {
            var changed = false
            for (i in 0 until n) {      // en orden de importancia: los más fuertes arrastran a los demás
                if (adj[i].isEmpty()) continue
                val votes = HashMap<Int, Double>()
                for ((j, w) in adj[i]) votes[label[j]] = (votes[label[j]] ?: 0.0) + w * (1.0 + cs[j].score / (cs[0].score + 1))
                val best = votes.maxByOrNull { it.value }!!.key
                if (best != label[i] && (votes[best] ?: 0.0) > (votes[label[i]] ?: 0.0)) { label[i] = best; changed = true }
            }
            if (!changed) return@repeat
        }
        // Juntar temas de un solo concepto con el vecino más fuerte; los sueltos van a «Otros»
        fun groups() = (0 until n).groupBy { label[it] }
        for ((l, members) in groups()) {
            if (members.size > 1) continue
            val i = members[0]
            val nb = adj[i].maxByOrNull { it.value }?.key ?: continue
            label[i] = label[nb]
        }
        // Como mucho 7 temas: los más chicos se unen al tema con el que más se conectan
        var gs = groups().toMutableMap()
        while (gs.size > 7) {
            val small = gs.minByOrNull { (_, m) -> m.sumOf { cs[it].score.toDouble() } }!!
            val link = HashMap<Int, Int>()
            for (i in small.value) for ((j, w) in adj[i]) if (label[j] != small.key) link[label[j]] = (link[label[j]] ?: 0) + w
            val target = link.maxByOrNull { it.value }?.key ?: gs.keys.first { it != small.key }
            small.value.forEach { label[it] = target }
            gs = groups().toMutableMap()
        }
        val ordered = gs.entries.sortedByDescending { (_, m) -> m.sumOf { cs[it].score.toDouble() } }
        return ordered.mapIndexed { t, (_, m) ->
            val sorted = m.sortedByDescending { cs[it].score }
            sorted.forEach { cs[it].topic = t }
            val loose = sorted.all { adj[it].isEmpty() }
            ClassTopic(t, if (loose && sorted.size > 2) "Otros conceptos" else sorted.take(3).joinToString(" · ") { cs[it].label }, sorted.map { cs[it].key })
        }
    }

    /** Ideas principales de un tema: las frases centrales que tocan sus conceptos, en todos los documentos. */
    fun topicIdeas(docs: List<Pair<String, Analysis>>, topic: ClassTopic, n: Int): List<Idea> {
        val keys = topic.concepts.toSet()
        val pool = ArrayList<Idea>()
        for ((id, a) in docs) for (s in a.sentences) {
            if (s.keys.count { it in keys } >= 1 && s.text.length in 50..340) pool.add(Idea(id, s.text, s.para))
        }
        if (pool.isEmpty()) return emptyList()
        val sample = if (pool.size > 600) pool.filterIndexed { i, _ -> i % (pool.size / 600 + 1) == 0 } else pool
        val boost = keys.flatMap { it.split(' ') }.map { if (it.length > 4 && it.endsWith("s")) it.dropLast(1) else it }.toSet()
        return rank(sample.map { it.text }, n, boost).map { sample[it] }
    }

    // =================================================================
    // Preguntas automáticas para el simulacro de la clase
    // =================================================================

    fun questions(g: ClassGraph, docs: List<Pair<String, Analysis>>, titles: Map<String, String>, n: Int, seed: Int): List<Quiz> {
        val r = java.util.Random(seed.toLong())
        val out = ArrayList<Quiz>()
        val cs = g.concepts
        if (cs.size < 4) return out
        fun others(not: Set<String>, prefer: Int? = null): List<String> {
            val same = cs.filter { it.label !in not && prefer != null && it.topic == prefer }.shuffled(r)
            val rest = cs.filter { it.label !in not }.shuffled(r)
            return (same + rest).map { it.label }.distinct().take(3)
        }
        fun mc(prompt: String, right: String, wrong: List<String>, explain: String, kind: String) {
            if (wrong.size < 2) return
            val opts = (wrong.take(3) + right).shuffled(r)
            out.add(Quiz(prompt, opts, opts.indexOf(right), explain, kind))
        }
        // 1. ¿Qué concepto se define así?
        for ((k, ds) in g.defs) {
            val c = cs.getOrNull(g.byKey[k] ?: continue) ?: continue
            val d = ds.first()
            val masked = d.definition.replace(Regex(Regex.escape(c.label), RegexOption.IGNORE_CASE), "____")
            mc("¿Qué concepto se define así?\n«${masked.take(260)}»", c.label, others(setOf(c.label), c.topic), "Según ${titles[d.bookId] ?: "el material"}.", "definicion")
        }
        // 2. ¿Con qué se relaciona más?
        for (i in cs.indices.shuffled(r).take(12)) {
            val nb = g.neighbors(i)
            if (nb.isEmpty()) continue
            val right = cs[nb[0].first].label
            val linked = nb.map { cs[it.first].label }.toSet()
            val wrong = cs.filter { it.label !in linked && it.label != cs[i].label }.shuffled(r).map { it.label }.take(3)
            mc("¿Con cuál de estos conceptos aparece más veces «${cs[i].label}» en tus materiales?", right, wrong, "Aparecen juntos ${nb[0].second} veces en la misma oración.", "relacion")
        }
        // 3. ¿En qué material se trata más? (si hay varios)
        if (titles.size >= 2) for (c in cs.shuffled(r).take(8)) {
            if (c.docs.size < 1) continue
            val top = c.docs.maxByOrNull { it.value } ?: continue
            val right = titles[top.key] ?: continue
            val wrong = titles.filterKeys { it != top.key }.values.shuffled(r).take(3)
            mc("¿Qué material trata más el concepto «${c.label}»?", right, wrong, "Aparece ${top.value} veces ahí.", "material")
        }
        // 4. ¿Qué autor se asocia con este concepto? (si el texto menciona autores)
        val authors = docs.flatMap { it.second.authors }.groupBy { it.name }.mapValues { (_, l) -> l.flatMap { it.conceptLabels }.toSet() }
        if (authors.size >= 3) for ((name, labels) in authors.entries.shuffled(r).take(6)) {
            val concept = labels.firstOrNull { l -> authors.count { l in it.value } == 1 } ?: continue
            val wrong = authors.keys.filter { it != name }.shuffled(r).take(3)
            mc("¿Qué autor se asocia con «$concept» en tus materiales?", name, wrong, "Se menciona junto a $name.", "autor")
        }
        // 5. Completar una idea principal
        for ((id, a) in docs.shuffled(r).take(4)) {
            val sents = a.sentences.filter { it.text.length in 60..260 }
            for (i in rank(sents.map { it.text }, 3)) {
                val s = sents[i]
                val c = cs.filter { it.key in s.keys && !it.key.contains(' ') }.maxByOrNull { it.score } ?: continue
                val masked = s.text.replace(Regex("(?i)\\b${Regex.escape(c.label)}\\w*"), "____")
                if (masked == s.text) continue
                mc("Completa la idea:\n«$masked»", c.label, others(setOf(c.label), c.topic), "De ${titles[id] ?: "tu material"}.", "completar")
            }
        }
        return out.shuffled(r).distinctBy { it.prompt }.take(n)
    }

    /** Para el semáforo: qué tarjetas hablan de un tema. */
    fun matches(text: String, labels: List<String>): Boolean {
        val f = fold(text)
        return labels.any { l -> val k = fold(l); k.length >= 4 && f.contains(k.take(min(k.length, max(4, k.length - 2)))) }
    }
}
