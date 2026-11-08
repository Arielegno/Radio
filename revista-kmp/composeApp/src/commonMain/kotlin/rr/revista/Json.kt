package rr.revista

/**
 * JSON mínimo y sin dependencias (sirve igual en Android y en las pruebas).
 * Objetos -> LinkedHashMap<String, Any?>, listas -> ArrayList<Any?>, números -> Long o Double.
 */
object Json {
    fun parse(text: String): Any? {
        val p = Parser(text)
        p.ws()
        val v = p.value()
        p.ws()
        if (p.i != text.length) throw IllegalArgumentException("JSON con datos de sobra en ${p.i}")
        return v
    }

    @Suppress("UNCHECKED_CAST")
    fun obj(text: String?): Map<String, Any?> =
        try { (if (text.isNullOrBlank()) null else parse(text)) as? Map<String, Any?> ?: emptyMap() } catch (e: Exception) { emptyMap() }

    fun arr(text: String?): List<Any?> =
        try { (if (text.isNullOrBlank()) null else parse(text)) as? List<Any?> ?: emptyList() } catch (e: Exception) { emptyList() }

    fun write(v: Any?): String = StringBuilder().also { w(it, v) }.toString()

    private fun w(sb: StringBuilder, v: Any?) {
        when (v) {
            null -> sb.append("null")
            is String -> str(sb, v)
            is Boolean -> sb.append(if (v) "true" else "false")
            is Int, is Long, is Short, is Byte -> sb.append(v.toString())
            is Double -> if (v.isFinite()) sb.append(if (v == kotlin.math.floor(v) && kotlin.math.abs(v) < 1e15) v.toLong().toString() else v.toString()) else sb.append("null")
            is Float -> w(sb, v.toDouble())
            is Number -> sb.append(v.toString())
            is Map<*, *> -> {
                sb.append('{'); var first = true
                for ((k, x) in v) { if (!first) sb.append(','); first = false; str(sb, k.toString()); sb.append(':'); w(sb, x) }
                sb.append('}')
            }
            is Iterable<*> -> {
                sb.append('['); var first = true
                for (x in v) { if (!first) sb.append(','); first = false; w(sb, x) }
                sb.append(']')
            }
            is Array<*> -> w(sb, v.toList())
            else -> str(sb, v.toString())
        }
    }

    private fun str(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\""); '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n"); '\r' -> sb.append("\\r"); '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b"); '\u000C' -> sb.append("\\f")
                else -> if (c < ' ' || c == ' ' || c == ' ') sb.append("\\u" + c.code.toString(16).padStart(4, '0')) else sb.append(c)
            }
        }
        sb.append('"')
    }

    private class Parser(val s: String) {
        var i = 0
        fun ws() { while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == '\t')) i++ }
        fun value(): Any? {
            if (i >= s.length) throw IllegalArgumentException("JSON incompleto")
            return when (val c = s[i]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> string()
                't' -> lit("true", true)
                'f' -> lit("false", false)
                'n' -> lit("null", null)
                else -> if (c == '-' || c in '0'..'9') num() else throw IllegalArgumentException("JSON inválido en $i")
            }
        }
        fun lit(w: String, v: Any?): Any? { if (!s.startsWith(w, i)) throw IllegalArgumentException("JSON inválido en $i"); i += w.length; return v }
        fun obj(): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>(); i++; ws()
            if (i < s.length && s[i] == '}') { i++; return m }
            while (true) {
                ws(); if (i >= s.length || s[i] != '"') throw IllegalArgumentException("Se esperaba una clave en $i")
                val k = string(); ws()
                if (i >= s.length || s[i] != ':') throw IllegalArgumentException("Se esperaba ':' en $i"); i++
                ws(); m[k] = value(); ws()
                if (i >= s.length) throw IllegalArgumentException("JSON incompleto")
                if (s[i] == ',') { i++; continue }
                if (s[i] == '}') { i++; return m }
                throw IllegalArgumentException("Se esperaba ',' o '}' en $i")
            }
        }
        fun arr(): List<Any?> {
            val a = ArrayList<Any?>(); i++; ws()
            if (i < s.length && s[i] == ']') { i++; return a }
            while (true) {
                ws(); a.add(value()); ws()
                if (i >= s.length) throw IllegalArgumentException("JSON incompleto")
                if (s[i] == ',') { i++; continue }
                if (s[i] == ']') { i++; return a }
                throw IllegalArgumentException("Se esperaba ',' o ']' en $i")
            }
        }
        fun string(): String {
            val sb = StringBuilder(); i++
            while (true) {
                if (i >= s.length) throw IllegalArgumentException("Texto sin cerrar")
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (i >= s.length) throw IllegalArgumentException("Escape incompleto")
                        when (val e = s[i++]) {
                            '"' -> sb.append('"'); '\\' -> sb.append('\\'); '/' -> sb.append('/')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C'); 'n' -> sb.append('\n')
                            'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'u' -> { if (i + 4 > s.length) throw IllegalArgumentException("Escape incompleto"); sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                            else -> throw IllegalArgumentException("Escape inválido \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }
        fun num(): Any {
            val st = i
            if (s[i] == '-') i++
            while (i < s.length && s[i] in '0'..'9') i++
            var frac = false
            if (i < s.length && s[i] == '.') { frac = true; i++; while (i < s.length && s[i] in '0'..'9') i++ }
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) { frac = true; i++; if (i < s.length && (s[i] == '+' || s[i] == '-')) i++; while (i < s.length && s[i] in '0'..'9') i++ }
            val t = s.substring(st, i)
            if (t == "-" || t.isEmpty()) throw IllegalArgumentException("Número inválido en $st")
            return if (frac) t.toDouble() else (t.toLongOrNull() ?: t.toDouble())
        }
    }
}

/* Ayudas para leer mapas JSON sin sustos */
fun Map<String, Any?>.str(k: String, d: String = ""): String = (this[k] as? String) ?: (this[k]?.let { if (it is Number || it is Boolean) it.toString() else null }) ?: d
fun Map<String, Any?>.num(k: String, d: Long = 0): Long = when (val v = this[k]) { is Number -> v.toLong(); is String -> v.toLongOrNull() ?: d; else -> d }
fun Map<String, Any?>.dbl(k: String, d: Double = 0.0): Double = when (val v = this[k]) { is Number -> v.toDouble(); is String -> v.toDoubleOrNull() ?: d; else -> d }
fun Map<String, Any?>.bool(k: String, d: Boolean = false): Boolean = when (val v = this[k]) { is Boolean -> v; is String -> v == "1" || v == "true"; is Number -> v.toInt() != 0; else -> d }
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.map(k: String): Map<String, Any?> = (this[k] as? Map<String, Any?>) ?: emptyMap()
@Suppress("UNCHECKED_CAST")
fun Any?.asObj(): Map<String, Any?>? = this as? Map<String, Any?>
fun Map<String, Any?>.list(k: String): List<Any?> = (this[k] as? List<Any?>) ?: emptyList()
