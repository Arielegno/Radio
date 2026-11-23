package com.estudio.musical.music

import java.text.Normalizer

/**
 * Herramientas para letras: acordes sobre la letra, transposición,
 * conteo de sílabas (gramatical y métrico) y rimas consonantes/asonantes.
 */
object TextoMusical {

    // ─────────────── Acordes en la letra ───────────────
    // Formato: "[Do]Hoy te [Sol]vi pasar". Las etiquetas [Coro], [Verso] no se tocan.

    private val CORCHETES = Regex("\\[([^\\]]+)]")

    fun transponer(texto: String, semitonos: Int): String = CORCHETES.replace(texto) { m ->
        val partes = m.groupValues[1].split("/")
        val ac = Acorde.parse(partes[0]) ?: return@replace m.value
        val nuevo = Acorde((ac.raiz + semitonos + 120) % 12, ac.calidad).nombre
        val bajo = partes.getOrNull(1)?.let { Acorde.parse(it) }?.let {
            "/" + Teoria.NOTAS[(it.raiz + semitonos + 120) % 12]
        } ?: ""
        "[$nuevo$bajo]"
    }

    /** Separa una línea en (línea de acordes, línea de letra) alineadas para mostrar en monoespaciado */
    fun separarAcordes(linea: String): Pair<String, String> {
        if (!CORCHETES.containsMatchIn(linea)) return "" to linea
        val acordes = StringBuilder()
        val letra = StringBuilder()
        var i = 0
        for (m in CORCHETES.findAll(linea)) {
            letra.append(linea, i, m.range.first)
            val nombre = m.groupValues[1]
            if (Acorde.parse(nombre.substringBefore("/")) == null) { // etiqueta como [Coro]
                letra.append(m.value); i = m.range.last + 1; continue
            }
            while (acordes.length < letra.length) acordes.append(' ')
            if (acordes.isNotEmpty() && acordes.last() != ' ') acordes.append(' ')
            acordes.append(nombre)
            i = m.range.last + 1
        }
        letra.append(linea.substring(i))
        return acordes.toString() to letra.toString()
    }

    fun acordesUsados(texto: String): List<String> =
        CORCHETES.findAll(texto).map { it.groupValues[1] }.filter { Acorde.parse(it.substringBefore("/")) != null }
            .distinct().toList()

    fun quitarAcordes(texto: String): String = CORCHETES.replace(texto) { m ->
        if (Acorde.parse(m.groupValues[1].substringBefore("/")) != null) "" else m.value
    }

    // ─────────────── Sílabas ───────────────

    private const val FUERTES = "aeoáéóíú"   // í y ú acentuadas forman hiato
    private const val VOCALES = "aeiouáéíóúü"

    private data class Nucleo(val principal: Int) // índice de la vocal principal en la palabra normalizada

    private fun limpiar(p: String): String {
        var s = p.lowercase().filter { it.isLetter() }
        s = s.replace(Regex("(q|g)u([eéií])"), "$1$2") // 'que', 'gui': la u no suena
        // 'y' como vocal al final o entre consonantes
        s = s.replace(Regex("y$"), "i").replace(Regex("y(?=[^aeiouáéíóú])"), "i")
        return s
    }

    private fun nucleos(s: String): List<Nucleo> {
        val res = mutableListOf<Nucleo>()
        var i = 0
        while (i < s.length) {
            if (s[i] !in VOCALES) { i++; continue }
            var j = i
            while (j < s.length && s[j] in VOCALES) j++
            // grupo de vocales s[i until j]: dividir por hiatos (dos fuertes seguidas)
            var inicio = i
            for (k in i + 1 until j) {
                if (s[k - 1] in FUERTES && s[k] in FUERTES) {
                    res += Nucleo(principal(s, inicio, k)); inicio = k
                }
            }
            res += Nucleo(principal(s, inicio, j))
            i = j
        }
        return res
    }

    private fun principal(s: String, a: Int, b: Int): Int {
        for (k in a until b) if (s[k] in "áéíóú") return k
        for (k in a until b) if (s[k] in "aeo") return k
        return b - 1
    }

    fun silabas(palabra: String): Int = nucleos(limpiar(palabra)).size

    /** Posición de la sílaba tónica contando desde el final (1 = aguda, 2 = llana, 3 = esdrújula) */
    fun tonica(palabra: String): Int {
        val s = limpiar(palabra)
        val n = nucleos(s)
        if (n.size <= 1) return 1
        val conTilde = n.indexOfLast { s[it.principal] in "áéíóú" }
        if (conTilde >= 0) return n.size - conTilde
        return if (s.last() in "aeiouns") 2 else 1
    }

    /**
     * Sílabas de un verso.
     * gramaticales = suma simple. métricas = con sinalefa y ajuste por la última palabra
     * (aguda +1, esdrújula −1), como se cuenta en poesía y canción.
     */
    fun silabasVerso(linea: String): Pair<Int, Int> {
        val palabras = quitarAcordes(linea).split(Regex("[^\\p{L}]+")).filter { it.isNotBlank() }
        if (palabras.isEmpty()) return 0 to 0
        val gram = palabras.sumOf { silabas(it) }
        var metr = gram
        for (k in 0 until palabras.size - 1) {
            val a = limpiar(palabras[k]); val b = limpiar(palabras[k + 1])
            if (a.isNotEmpty() && b.isNotEmpty() && a.last() in VOCALES &&
                (b.first() in VOCALES || (b.first() == 'h' && b.length > 1 && b[1] in VOCALES))) metr--
        }
        when (tonica(palabras.last())) { 1 -> metr++; 3 -> metr-- }
        return gram to metr
    }

    // ─────────────── Rimas ───────────────

    private fun sinTildes(s: String) =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    /** Terminación desde la vocal tónica: "corazón" → "on", "camino" → "ino" */
    fun terminacion(palabra: String): String {
        val s = limpiar(palabra)
        val n = nucleos(s)
        if (n.isEmpty()) return sinTildes(s)
        val t = n[n.size - tonica(palabra).coerceAtMost(n.size)]
        return sinTildes(s.substring(t.principal))
    }

    /** Vocales desde la tónica (rima asonante): "camino" → "io", "cielo" → "eo" */
    fun asonancia(palabra: String): String {
        val s = limpiar(palabra)
        val n = nucleos(s)
        if (n.isEmpty()) return ""
        val desde = n.size - tonica(palabra).coerceAtMost(n.size)
        return sinTildes(n.drop(desde).map { s[it.principal] }.joinToString(""))
    }

    data class Rimas(val consonantes: List<String>, val asonantes: List<String>)

    fun rimas(palabra: String, extra: Collection<String> = emptyList()): Rimas {
        val p = palabra.trim().lowercase()
        if (p.isEmpty()) return Rimas(emptyList(), emptyList())
        val term = terminacion(p); val aso = asonancia(p)
        val dic = (Palabras.LISTA + extra.map { it.lowercase() }).filter { it != p && it.length > 1 }.toSortedSet()
        val cons = dic.filter { terminacion(it) == term }
        val ason = dic.filter { asonancia(it) == aso && it !in cons }
        return Rimas(cons.take(60), ason.take(60))
    }

    fun palabrasDe(texto: String): Set<String> =
        quitarAcordes(texto).split(Regex("[^\\p{L}]+")).filter { it.length > 2 }.map { it.lowercase() }.toSet()
}
