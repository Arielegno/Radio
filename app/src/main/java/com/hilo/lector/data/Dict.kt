package com.hilo.lector.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.zip.GZIPInputStream

/** Una acepción: definición, ejemplo y etiquetas (coloquial, derecho…). */
data class Sense(val def: String, val example: String, val tags: List<String>)

/** Una entrada del diccionario. [tr]: traducción (al inglés en el diccionario de español; al español en el de inglés). */
data class DictEntry(val word: String, val pos: String, val senses: List<Sense>, val ipa: String, val tr: String, val ety: String, val lang: String, val from: String? = null)

/**
 * Diccionarios de español e inglés que funcionan sin internet. Se descargan una vez desde la página de
 * Hilo en GitHub (datos del Wikcionario, licencia CC BY-SA 4.0) y quedan guardados en el celular.
 */
object Dict {
    const val BASE = "https://github.com/Arielegno/Radio/releases/download/dic-1/"
    val NAMES = mapOf("es" to "Español", "en" to "Inglés")

    private fun dir(ctx: Context) = File(ctx.filesDir, "diccionarios").apply { mkdirs() }
    fun file(ctx: Context, lang: String) = File(dir(ctx), "$lang.db")
    fun has(ctx: Context, lang: String) = file(ctx, lang).let { it.exists() && it.length() > 1000 }
    fun any(ctx: Context) = has(ctx, "es") || has(ctx, "en")
    fun sizeMb(ctx: Context, lang: String) = file(ctx, lang).length() / 1_048_576

    private val open = HashMap<String, SQLiteDatabase>()

    private fun db(ctx: Context, lang: String): SQLiteDatabase? = synchronized(open) {
        open[lang]?.takeIf { it.isOpen } ?: if (!has(ctx, lang)) null else runCatching {
            SQLiteDatabase.openDatabase(file(ctx, lang).path, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
        }.getOrNull()?.also { open[lang] = it }
    }

    fun delete(ctx: Context, lang: String) {
        synchronized(open) { open.remove(lang)?.close() }
        file(ctx, lang).delete()
    }

    /** Descarga y descomprime un diccionario. [progress] recibe (bajado, total) en bytes. */
    fun download(ctx: Context, lang: String, progress: (Long, Long) -> Unit) {
        var url = URL("$BASE$lang.db.gz")
        var conn: HttpURLConnection
        var hops = 0
        while (true) {
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000; readTimeout = 60_000; instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Hilo")
            }
            val code = conn.responseCode
            if (code in 300..399 && hops < 6) { url = URL(url, conn.getHeaderField("Location")); conn.disconnect(); hops++; continue }
            if (code != 200) { conn.disconnect(); error("El servidor respondió $code") }
            break
        }
        val total = conn.contentLengthLong
        val tmp = File(dir(ctx), "$lang.db.part")
        var read = 0L
        conn.inputStream.use { raw ->
            val counting = object : java.io.FilterInputStream(raw) {
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    val n = super.read(b, off, len)
                    if (n > 0) { read += n; progress(read, total) }
                    return n
                }
            }
            GZIPInputStream(counting, 1 shl 16).use { gz -> tmp.outputStream().use { gz.copyTo(it, 1 shl 16) } }
        }
        conn.disconnect()
        delete(ctx, lang)
        if (!tmp.renameTo(file(ctx, lang))) error("No pude guardar el diccionario")
    }

    fun count(ctx: Context, lang: String): Int = runCatching {
        db(ctx, lang)?.rawQuery("SELECT v FROM meta WHERE k='entradas'", null)?.use { if (it.moveToFirst()) it.getString(0).toInt() else 0 } ?: 0
    }.getOrDefault(0)

    private fun fold(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    private fun rows(d: SQLiteDatabase, where: String, args: Array<String>, lang: String, from: String?): List<DictEntry> =
        d.rawQuery("SELECT word, pos, defs, ipa, tr, ety FROM w WHERE $where LIMIT 12", args).use { c ->
            val out = ArrayList<DictEntry>()
            while (c.moveToNext()) {
                val arr = runCatching { JSONArray(c.getString(2)) }.getOrDefault(JSONArray())
                val senses = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    val t = o.optJSONArray("t")
                    Sense(o.optString("d"), o.optString("e"), if (t == null) emptyList() else (0 until t.length()).map { t.getString(it) })
                }
                out.add(DictEntry(c.getString(0), c.getString(1).orEmpty(), senses, c.getString(3).orEmpty(), c.getString(4).orEmpty(), c.getString(5).orEmpty(), lang, from))
            }
            out
        }

    /**
     * Busca una palabra: tal cual, sin mayúsculas ni tildes, y si es una forma (plural, conjugación)
     * también su palabra base («dijeron» → «decir»).
     */
    fun lookup(ctx: Context, word: String, lang: String): List<DictEntry> {
        val d = db(ctx, lang) ?: return emptyList()
        val w = word.trim().trim('.', ',', ';', ':', '«', '»', '"', '(', ')', '¿', '?', '¡', '!')
        if (w.isEmpty()) return emptyList()
        return runCatching {
            var res = rows(d, "word = ? OR word = ?", arrayOf(w, w.lowercase()), lang, null)
            if (res.isEmpty()) res = rows(d, "norm = ?", arrayOf(fold(w)), lang, null)
            // Formas: plural, femenino, conjugaciones
            val lemmas = d.rawQuery("SELECT DISTINCT lemma FROM f WHERE form = ? LIMIT 4", arrayOf(fold(w))).use { c ->
                val l = ArrayList<String>(); while (c.moveToNext()) l.add(c.getString(0)); l
            }.filter { it.lowercase() != w.lowercase() }
            val base = lemmas.flatMap { l -> rows(d, "word = ?", arrayOf(l), lang, w) }
            (res + base).distinctBy { it.word + "|" + it.pos + "|" + it.senses.firstOrNull()?.def }
        }.getOrDefault(emptyList())
    }

    /** Busca en los dos diccionarios; primero el idioma más probable. */
    fun lookupAny(ctx: Context, word: String, preferEn: Boolean): List<DictEntry> {
        val order = if (preferEn) listOf("en", "es") else listOf("es", "en")
        return order.flatMap { lookup(ctx, word, it) }
    }

    /** Palabras que empiezan así (para el buscador del diccionario). */
    fun suggest(ctx: Context, prefix: String, lang: String, limit: Int = 20): List<String> {
        val d = db(ctx, lang) ?: return emptyList()
        val p = fold(prefix.trim())
        if (p.length < 2) return emptyList()
        return runCatching {
            d.rawQuery("SELECT DISTINCT word FROM w WHERE norm >= ? AND norm < ? ORDER BY length(word) LIMIT $limit", arrayOf(p, p + "￿")).use { c ->
                val l = ArrayList<String>(); while (c.moveToNext()) l.add(c.getString(0)); l
            }
        }.getOrDefault(emptyList())
    }
}
