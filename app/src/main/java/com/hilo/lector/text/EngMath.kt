package com.hilo.lector.text

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/** Cálculos para las herramientas de ingeniería, matemáticas y estadística. Todo sin internet. */
object EngMath {

    // ---------------------------------------------------------------- números con prefijos
    private val PREFIX = linkedMapOf(
        "T" to 1e12, "G" to 1e9, "M" to 1e6, "k" to 1e3, "K" to 1e3, "" to 1.0,
        "m" to 1e-3, "u" to 1e-6, "µ" to 1e-6, "μ" to 1e-6, "n" to 1e-9, "p" to 1e-12,
    )

    /** «4.7k», «100n», «2,2u», «1M5» (=1.5M) → número. Null si no se entiende. */
    fun parse(s: String): Double? {
        val t = s.trim().replace(",", ".").replace(" ", "").replace("Ω", "").replace("ohm", "", true)
            .removeSuffix("F").removeSuffix("H").removeSuffix("V").removeSuffix("A").removeSuffix("W").removeSuffix("Hz")
        if (t.isEmpty()) return null
        t.toDoubleOrNull()?.let { return it }
        Regex("^([-+]?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?)([TGMkKmuµμnp])$").find(t)?.let { m ->
            return m.groupValues[1].toDouble() * PREFIX.getValue(m.groupValues[2])
        }
        Regex("^(\\d+)([TGMkKmuµμnpR])(\\d+)$").find(t)?.let { m ->
            val mul = if (m.groupValues[2] == "R") 1.0 else PREFIX.getValue(m.groupValues[2])
            return "${m.groupValues[1]}.${m.groupValues[3]}".toDouble() * mul
        }
        return null
    }

    /** 0.0047 → «4,7 m». */
    fun eng(x: Double, unit: String = "", digits: Int = 4): String {
        if (x.isNaN()) return "—"
        if (x.isInfinite()) return "∞ $unit".trim()
        if (x == 0.0) return "0 $unit".trim()
        val e = floor(log10(abs(x)) / 3).toInt().coerceIn(-4, 4)
        val p = when (e) { 4 -> "T"; 3 -> "G"; 2 -> "M"; 1 -> "k"; 0 -> ""; -1 -> "m"; -2 -> "µ"; -3 -> "n"; else -> "p" }
        val v = x / 10.0.pow(e * 3)
        return (num(v, digits) + " " + p + unit).trim()
    }

    /** Número corto y legible, con coma decimal. */
    fun num(x: Double, digits: Int = 4): String {
        if (x.isNaN()) return "—"
        if (x.isInfinite()) return "∞"
        if (x != 0.0 && (abs(x) >= 1e9 || abs(x) < 1e-4)) return String.format(java.util.Locale.US, "%.${digits - 1}e", x).replace(".", ",")
        val s = java.math.BigDecimal(x).round(java.math.MathContext(digits)).stripTrailingZeros().toPlainString()
        return s.replace(".", ",")
    }

    // ---------------------------------------------------------------- calculadora
    /** Evalúa «2*sin(30)+sqrt(16)^2». [deg]: ángulos en grados. [vars]: valores de letras. */
    fun eval(expr: String, deg: Boolean = true, vars: Map<String, Double> = emptyMap()): Double = Parser(expr, deg, vars).run()

    /** Letras de una expresión que no son funciones ni constantes: lo que hay que pedir al usuario. */
    fun variables(expr: String): List<String> =
        Regex("[A-Za-zÀ-ÿ_][A-Za-z0-9_À-ÿ]*").findAll(expr).map { it.value }
            .filter { it !in FUNCS && it !in setOf("pi", "e", "π") }.distinct().toList()

    private val FUNCS = setOf("sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh", "sqrt", "raiz", "ln", "log", "exp", "abs", "sen", "round", "cbrt")

    private class Parser(val s: String, val deg: Boolean, val vars: Map<String, Double>) {
        var i = 0
        fun run(): Double { val v = sum(); skip(); if (i < s.length) error("Sobra «${s.substring(i)}»"); return v }
        fun skip() { while (i < s.length && s[i] == ' ') i++ }
        fun peek(): Char { skip(); return if (i < s.length) s[i] else '\u0000' }
        fun sum(): Double {
            var v = prod()
            while (true) when (peek()) { '+' -> { i++; v += prod() }; '-', '−' -> { i++; v -= prod() }; else -> return v }
        }
        fun prod(): Double {
            var v = unary()
            while (true) when (peek()) {
                '*', '×', '·' -> { i++; v *= unary() }
                '/', '÷' -> { i++; v /= unary() }
                '(' -> v *= unary()                       // 2(3) = 6
                else -> if (peek().isLetter()) v *= unary() else return v   // 2pi
            }
        }
        fun unary(): Double = when (peek()) { '-', '−' -> { i++; -unary() }; '+' -> { i++; unary() }; else -> power() }
        fun power(): Double {
            val b = postfix()
            return if (peek() == '^') { i++; b.pow(unary()) } else b
        }
        fun postfix(): Double {
            var v = atom()
            while (peek() == '!') { i++; v = fact(v) }
            if (peek() == '%') { i++; v /= 100 }
            return v
        }
        fun atom(): Double {
            val ch = peek()
            if (ch == '(') { i++; val v = sum(); if (peek() == ')') i++; return v }
            if (ch.isDigit() || ch == '.' || ch == ',') {
                val st = i
                while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == ',')) i++
                if (i < s.length && (s[i] == 'e' || s[i] == 'E') && i + 1 < s.length && (s[i + 1].isDigit() || s[i + 1] == '-' || s[i + 1] == '+')) {
                    i += 2; while (i < s.length && s[i].isDigit()) i++
                }
                var v = s.substring(st, i).replace(",", ".").toDouble()
                if (i < s.length && s[i] in "TGMkmunpµ" && (i + 1 >= s.length || !s[i + 1].isLetter())) { v *= PREFIX.getValue(s[i].toString()); i++ }
                return v
            }
            if (ch.isLetter() || ch == 'π' || ch == '√') {
                if (ch == '√') { i++; return sqrt(atom()) }
                val st = i
                while (i < s.length && (s[i].isLetterOrDigit() || s[i] == '_' || s[i] == 'π')) i++
                val name = s.substring(st, i)
                vars[name]?.let { return it }
                when (name) { "pi", "π" -> return Math.PI; "e" -> return Math.E }
                if (name !in FUNCS) error("No conozco «$name»")
                val a = atom()
                val r = if (deg) Math.toRadians(a) else a
                fun inv(x: Double) = if (deg) Math.toDegrees(x) else x
                return when (name) {
                    "sin", "sen" -> kotlin.math.sin(r); "cos" -> kotlin.math.cos(r); "tan" -> kotlin.math.tan(r)
                    "asin" -> inv(kotlin.math.asin(a)); "acos" -> inv(kotlin.math.acos(a)); "atan" -> inv(kotlin.math.atan(a))
                    "sinh" -> kotlin.math.sinh(a); "cosh" -> kotlin.math.cosh(a); "tanh" -> kotlin.math.tanh(a)
                    "sqrt", "raiz" -> sqrt(a); "cbrt" -> Math.cbrt(a); "ln" -> ln(a); "log" -> log10(a); "exp" -> exp(a); "abs" -> abs(a); "round" -> round(a)
                    else -> error("No conozco «$name»")
                }
            }
            error(if (i >= s.length) "Falta un número al final" else "No entiendo «$ch»")
        }
        fun fact(n: Double): Double { var r = 1.0; var k = 2; while (k <= n.toInt()) { r *= k; k++ }; return r }
    }

    // ---------------------------------------------------------------- código de colores
    val COLORS = listOf("negro", "marrón", "rojo", "naranja", "amarillo", "verde", "azul", "violeta", "gris", "blanco")
    val COLOR_RGB = listOf(0xFF111111, 0xFF7A4A21, 0xFFD32F2F, 0xFFF57C00, 0xFFFBC02D, 0xFF2E7D32, 0xFF1565C0, 0xFF7B1FA2, 0xFF8E8E8E, 0xFFFFFFFF, 0xFFC9A227, 0xFFB8B8B8).map { it.toInt() }
    /** Multiplicador: índices 0..9 de COLORS más 10 = dorado (×0,1) y 11 = plateado (×0,01). */
    val MULT = listOf(1.0, 10.0, 100.0, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 0.1, 0.01)
    val TOL = listOf(1 to "±1 %", 2 to "±2 %", 5 to "±0,5 %", 6 to "±0,25 %", 7 to "±0,1 %", 10 to "±5 %", 11 to "±10 %")

    /** De valor a bandas: 4 bandas (2 cifras) o 5 (3 cifras). Null si no se puede. */
    fun bands(value: Double, digits: Int): List<Int>? {
        if (value <= 0) return null
        for (m in MULT.indices.sortedBy { MULT[it] }) {
            val d = value / MULT[m]
            val r = round(d)
            if (abs(d - r) < 1e-6 * maxOf(1.0, d) && r >= 10.0.pow(digits - 1) && r < 10.0.pow(digits)) {
                return r.toLong().toString().map { it.digitToInt() } + m
            }
        }
        return null
    }

    // ---------------------------------------------------------------- series E
    val E12 = listOf(1.0, 1.2, 1.5, 1.8, 2.2, 2.7, 3.3, 3.9, 4.7, 5.6, 6.8, 8.2)
    val E24 = listOf(1.0, 1.1, 1.2, 1.3, 1.5, 1.6, 1.8, 2.0, 2.2, 2.4, 2.7, 3.0, 3.3, 3.6, 3.9, 4.3, 4.7, 5.1, 5.6, 6.2, 6.8, 7.5, 8.2, 9.1)

    /** El valor comercial más cercano (por arriba si [up]). */
    fun nearestE(x: Double, series: List<Double> = E12, up: Boolean = false): Double {
        if (x <= 0) return 0.0
        val dec = 10.0.pow(floor(log10(x)))
        val cands = (series.map { it * dec } + series.map { it * dec * 10 } + series.map { it * dec / 10 }).sorted()
        return if (up) cands.first { it >= x * 0.9999 } else cands.minBy { abs(ln(it / x)) }
    }

    // ---------------------------------------------------------------- AWG
    /** Diámetro (mm) de un calibre AWG. */
    fun awgMm(n: Int) = 0.127 * 92.0.pow((36 - n) / 39.0)
    fun awgArea(n: Int) = Math.PI / 4 * awgMm(n).pow(2)
    /** Resistencia del cobre a 20 °C, ohmios por metro. */
    fun awgOhmM(n: Int) = 1.724e-8 / (awgArea(n) * 1e-6)

    // ---------------------------------------------------------------- estadística
    data class Desc(val n: Int, val mean: Double, val median: Double, val sd: Double, val min: Double, val max: Double, val q1: Double, val q3: Double, val mode: List<Double>)

    fun numbers(s: String): List<Double> = Regex("[-+]?\\d+(?:[.,]\\d+)?").findAll(s.replace(Regex("(\\d),(\\d{3})(?!\\d)"), "$1$2"))
        .mapNotNull { it.value.replace(",", ".").toDoubleOrNull() }.toList()

    private fun quant(sorted: List<Double>, q: Double): Double {
        if (sorted.size == 1) return sorted[0]
        val h = (sorted.size - 1) * q
        val lo = floor(h).toInt()
        return sorted[lo] + (h - lo) * (sorted[minOf(lo + 1, sorted.size - 1)] - sorted[lo])
    }

    fun describe(xs: List<Double>): Desc? {
        if (xs.isEmpty()) return null
        val s = xs.sorted()
        val m = xs.average()
        val sd = if (xs.size > 1) sqrt(xs.sumOf { (it - m).pow(2) } / (xs.size - 1)) else 0.0
        val counts = xs.groupingBy { it }.eachCount()
        val top = counts.values.max()
        val mode = if (top == 1) emptyList() else counts.filter { it.value == top }.keys.sorted()
        return Desc(xs.size, m, quant(s, 0.5), sd, s.first(), s.last(), quant(s, 0.25), quant(s, 0.75), mode)
    }

    /** Correlación de Pearson r, y su valor p (dos colas). */
    fun pearson(x: List<Double>, y: List<Double>): Pair<Double, Double>? {
        val n = minOf(x.size, y.size)
        if (n < 3) return null
        val a = x.take(n); val b = y.take(n)
        val ma = a.average(); val mb = b.average()
        val num = (0 until n).sumOf { (a[it] - ma) * (b[it] - mb) }
        val den = sqrt(a.sumOf { (it - ma).pow(2) } * b.sumOf { (it - mb).pow(2) })
        if (den == 0.0) return null
        val r = num / den
        val df = n - 2
        val t = r * sqrt(df / maxOf(1e-12, 1 - r * r))
        return r to tP(t, df.toDouble())
    }

    data class TTest(val t: Double, val df: Double, val p: Double, val d: Double, val m1: Double, val m2: Double)

    /** Prueba t de Welch para dos grupos independientes (no supone varianzas iguales). */
    fun welch(x: List<Double>, y: List<Double>): TTest? {
        if (x.size < 2 || y.size < 2) return null
        val m1 = x.average(); val m2 = y.average()
        val v1 = x.sumOf { (it - m1).pow(2) } / (x.size - 1)
        val v2 = y.sumOf { (it - m2).pow(2) } / (y.size - 1)
        val se2 = v1 / x.size + v2 / y.size
        if (se2 == 0.0) return null
        val t = (m1 - m2) / sqrt(se2)
        val df = se2.pow(2) / ((v1 / x.size).pow(2) / (x.size - 1) + (v2 / y.size).pow(2) / (y.size - 1))
        val sp = sqrt(((x.size - 1) * v1 + (y.size - 1) * v2) / (x.size + y.size - 2))
        return TTest(t, df, tP(t, df), if (sp == 0.0) 0.0 else (m1 - m2) / sp, m1, m2)
    }

    /** p de dos colas para t con df grados de libertad. */
    fun tP(t: Double, df: Double): Double = betaI(df / 2, 0.5, df / (df + t * t)).coerceIn(0.0, 1.0)

    private fun lnGamma(x: Double): Double {
        val g = doubleArrayOf(76.18009172947146, -86.50532032941677, 24.01409824083091, -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5)
        var y = x; val tmp = x + 5.5 - (x + 0.5) * ln(x + 5.5)
        var ser = 1.000000000190015
        for (c in g) { y += 1; ser += c / y }
        return -tmp + ln(2.5066282746310005 * ser / x)
    }

    /** Beta incompleta regularizada I_x(a,b). */
    fun betaI(a: Double, b: Double, x: Double): Double {
        if (x <= 0) return 0.0
        if (x >= 1) return 1.0
        val bt = exp(lnGamma(a + b) - lnGamma(a) - lnGamma(b) + a * ln(x) + b * ln(1 - x))
        return if (x < (a + 1) / (a + b + 2)) bt * betacf(a, b, x) / a else 1 - bt * betacf(b, a, 1 - x) / b
    }

    private fun betacf(a: Double, b: Double, x: Double): Double {
        val fpmin = 1e-300
        var c = 1.0; var d = 1 - (a + b) * x / (a + 1)
        if (abs(d) < fpmin) d = fpmin
        d = 1 / d; var h = d
        for (m in 1..300) {
            val m2 = 2 * m
            var aa = m * (b - m) * x / ((a + m2 - 1) * (a + m2))
            d = 1 + aa * d; if (abs(d) < fpmin) d = fpmin
            c = 1 + aa / c; if (abs(c) < fpmin) c = fpmin
            d = 1 / d; h *= d * c
            aa = -(a + m) * (a + b + m) * x / ((a + m2) * (a + m2 + 1))
            d = 1 + aa * d; if (abs(d) < fpmin) d = fpmin
            c = 1 + aa / c; if (abs(c) < fpmin) c = fpmin
            d = 1 / d
            val del = d * c; h *= del
            if (abs(del - 1) < 3e-12) break
        }
        return h
    }

    // ---------------------------------------------------------------- álgebra
    /** Raíces de ax²+bx+c: reales o complejas (parte real, imaginaria). */
    fun quadratic(a: Double, b: Double, c: Double): List<Pair<Double, Double>> {
        if (a == 0.0) return if (b == 0.0) emptyList() else listOf(-c / b to 0.0)
        val d = b * b - 4 * a * c
        return if (d >= 0) listOf((-b + sqrt(d)) / (2 * a) to 0.0, (-b - sqrt(d)) / (2 * a) to 0.0)
        else listOf(-b / (2 * a) to sqrt(-d) / (2 * a), -b / (2 * a) to -sqrt(-d) / (2 * a))
    }

    /** Resuelve A·x = b (Gauss con pivoteo). Null si no tiene solución única. */
    fun solve(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val n = b.size
        val m = Array(n) { r -> DoubleArray(n + 1) { c -> if (c < n) a[r][c] else b[r] } }
        for (col in 0 until n) {
            val piv = (col until n).maxBy { abs(m[it][col]) }
            if (abs(m[piv][col]) < 1e-12) return null
            val t = m[col]; m[col] = m[piv]; m[piv] = t
            for (r in 0 until n) if (r != col) {
                val f = m[r][col] / m[col][col]
                for (c in col..n) m[r][c] -= f * m[col][c]
            }
        }
        return DoubleArray(n) { m[it][n] / m[it][it] }
    }

    fun det(a: Array<DoubleArray>): Double = when (a.size) {
        2 -> a[0][0] * a[1][1] - a[0][1] * a[1][0]
        3 -> a[0][0] * (a[1][1] * a[2][2] - a[1][2] * a[2][1]) - a[0][1] * (a[1][0] * a[2][2] - a[1][2] * a[2][0]) + a[0][2] * (a[1][0] * a[2][1] - a[1][1] * a[2][0])
        else -> Double.NaN
    }

    // ---------------------------------------------------------------- lógica
    /** Tabla de verdad de una expresión con letras A..F y AND/OR/NOT/XOR (o · + ' ! ^ & |). */
    fun truthTable(expr: String): Pair<List<String>, List<Pair<List<Boolean>, Boolean>>> {
        val vars = Regex("\\b[A-Fa-f]\\b").findAll(expr.replace(Regex("(?i)\\b(and|or|not|xor|nand|nor)\\b"), " ")).map { it.value.uppercase() }.distinct().sorted().toList()
        val rows = (0 until (1 shl vars.size)).map { k ->
            val vals = vars.indices.map { (k shr (vars.size - 1 - it)) and 1 == 1 }
            vals to Logic(expr, vars.zip(vals).toMap()).run()
        }
        return vars to rows
    }

    private class Logic(src: String, val v: Map<String, Boolean>) {
        val s = src.replace(Regex("(?i)\\bnand\\b"), "↑").replace(Regex("(?i)\\bnor\\b"), "↓").replace(Regex("(?i)\\band\\b"), "&").replace(Regex("(?i)\\bor\\b"), "|")
            .replace(Regex("(?i)\\bxor\\b"), "^").replace(Regex("(?i)\\bnot\\b"), "!").replace("·", "&").replace("*", "&").replace("+", "|").replace("⊕", "^").replace("¬", "!").replace("~", "!")
        var i = 0
        fun run(): Boolean { val r = or(); return r }
        fun peek(): Char { while (i < s.length && s[i] == ' ') i++; return if (i < s.length) s[i] else '\u0000' }
        fun or(): Boolean { var a = xor(); while (true) when (peek()) { '|' -> { i++; a = xor() or a }; '↓' -> { i++; a = !(a or xor()) }; else -> return a } }
        fun xor(): Boolean { var a = and(); while (peek() == '^') { i++; a = a xor and() }; return a }
        fun and(): Boolean {
            var a = not()
            while (true) when {
                peek() == '&' -> { i++; a = not() and a }
                peek() == '↑' -> { i++; a = !(a and not()) }
                peek().isLetter() || peek() == '(' || peek() == '!' -> a = not() and a   // AB = A·B
                else -> return a
            }
        }
        fun not(): Boolean {
            if (peek() == '!') { i++; return !not() }
            var a = atom()
            while (peek() == '\'') { i++; a = !a }
            return a
        }
        fun atom(): Boolean {
            val ch = peek()
            if (ch == '(') { i++; val r = or(); if (peek() == ')') i++; return r }
            if (ch == '0' || ch == '1') { i++; return ch == '1' }
            if (ch.isLetter()) { i++; return v[ch.uppercase()] ?: error("¿Qué es «$ch»?") }
            error(if (ch == '\u0000') "La expresión quedó incompleta" else "No entiendo «$ch»")
        }
    }
}
