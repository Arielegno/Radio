package com.hilo.lector.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.hilo.lector.text.Formats
import java.io.File
import java.text.Normalizer

/** Un resultado de la búsqueda en toda la biblioteca. */
data class Hit(val bookId: String, val para: Int, val snippet: String)

/**
 * Índice de texto completo de la biblioteca (SQLite FTS4): permite buscar una palabra en todos los libros al instante.
 * Se arma de a poco en segundo plano y solo con los libros que ya tienen su texto a mano.
 */
class SearchIndex(private val ctx: Context) : SQLiteOpenHelper(ctx, "busqueda.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        // Sin tildes si el SQLite del celular lo permite; si no, el tokenizador simple
        try { db.execSQL("CREATE VIRTUAL TABLE docs USING fts4(book, para, body, tokenize=unicode61 \"remove_diacritics=1\")") }
        catch (_: Exception) { db.execSQL("CREATE VIRTUAL TABLE docs USING fts4(book, para, body)") }
        db.execSQL("CREATE TABLE meta(book TEXT PRIMARY KEY, size INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    private fun fold(s: String) = s.lowercase().let { l -> if (l.all { it.code < 128 }) l else Normalizer.normalize(l, Normalizer.Form.NFD).replace(com.hilo.lector.text.MARKS_RX, "") }

    /** Agrega o actualiza el texto de un libro si cambió. Devuelve true si lo indexó. */
    fun index(bookId: String, paras: List<String>, stamp: Long): Boolean {
        val db = writableDatabase
        val old = db.rawQuery("SELECT size FROM meta WHERE book=?", arrayOf(bookId)).use { if (it.moveToFirst()) it.getLong(0) else -1L }
        if (old == stamp) return false
        db.beginTransaction()
        try {
            db.delete("docs", "book=?", arrayOf(bookId))
            val cv = ContentValues()
            paras.forEachIndexed { i, p ->
                if (p.isBlank()) return@forEachIndexed
                cv.clear(); cv.put("book", bookId); cv.put("para", i); cv.put("body", p.removePrefix("# "))
                db.insert("docs", null, cv)
            }
            db.insertWithOnConflict("meta", null, ContentValues().apply { put("book", bookId); put("size", stamp) }, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        return true
    }

    fun remove(bookId: String) {
        writableDatabase.delete("docs", "book=?", arrayOf(bookId))
        writableDatabase.delete("meta", "book=?", arrayOf(bookId))
    }

    fun indexed(): Set<String> = readableDatabase.rawQuery("SELECT book FROM meta", null).use { c ->
        val s = HashSet<String>(); while (c.moveToNext()) s.add(c.getString(0)); s
    }

    /** Busca todas las palabras (también el comienzo de palabra: «sociol» encuentra «sociología»). */
    fun search(query: String, limit: Int = 200): List<Hit> {
        val words = Regex("[\\p{L}\\p{N}]+").findAll(fold(query)).map { it.value }.filter { it.length >= 2 }.toList()
        if (words.isEmpty()) return emptyList()
        val q = if (query.trim().startsWith("\"") && query.trim().endsWith("\"")) "\"" + words.joinToString(" ") + "\""
        else words.joinToString(" ") { "$it*" }
        return runCatching {
            readableDatabase.rawQuery(
                "SELECT book, para, snippet(docs, '«', '»', '…', 2, 14) FROM docs WHERE body MATCH ? LIMIT $limit", arrayOf(q),
            ).use { c ->
                val out = ArrayList<Hit>()
                while (c.moveToNext()) out.add(Hit(c.getString(0), c.getString(1).toIntOrNull() ?: 0, c.getString(2)))
                out
            }
        }.getOrDefault(emptyList())
    }

    /** Un párrafo parecido, con su texto completo y qué tanto se parece. */
    data class Passage(val bookId: String, val para: Int, val text: String, val score: Float)

    /**
     * Párrafos parecidos a un texto: los que comparten más palabras clave (y las más raras pesan más).
     * Solo en [books] si no está vacío.
     */
    fun similar(words: List<String>, books: Set<String>, limit: Int = 25): List<Passage> {
        val ws = words.map { fold(it) }.filter { it.length >= 3 }.distinct().take(8)
        if (ws.isEmpty()) return emptyList()
        val q = ws.joinToString(" OR ") { "$it*" }
        val rows = ArrayList<Triple<String, Int, String>>()
        runCatching {
            readableDatabase.rawQuery("SELECT book, para, body FROM docs WHERE body MATCH ? LIMIT 3000", arrayOf(q)).use { c ->
                while (c.moveToNext()) {
                    val b = c.getString(0)
                    if (books.isEmpty() || b in books) rows.add(Triple(b, c.getString(1).toIntOrNull() ?: 0, c.getString(2)))
                }
            }
        }
        if (rows.isEmpty()) return emptyList()
        val folded = rows.map { fold(it.third) }
        val df = ws.associateWith { w -> folded.count { it.contains(w) }.coerceAtLeast(1) }
        return rows.indices.map { i ->
            val f = folded[i]
            val sc = ws.sumOf { w -> if (f.contains(w)) Math.log(1.0 + rows.size.toDouble() / df.getValue(w)) else 0.0 } / Math.sqrt(1.0 + f.length / 400.0)
            Passage(rows[i].first, rows[i].second, rows[i].third, sc.toFloat())
        }.filter { it.score > 0f }.sortedByDescending { it.score }.distinctBy { it.bookId to it.para }.take(limit)
    }

    /** Agrega al índice el texto de un libro (para las áreas y clases). */
    fun ensure(bookId: String, paras: List<String>) {
        index(bookId, paras, paras.size.toLong() * 1_000_003L + paras.sumOf { it.length.toLong() })
    }

    /**
     * Indexa los libros que faltan: los que ya tienen texto guardado (modo Texto o sala de estudio)
     * y los formatos de texto (EPUB, Word, MOBI…). Los PDF se indexan después de abrirlos en modo Texto.
     */
    fun catchUp(books: List<Book>, fileOf: (Book) -> File, max: Int = 40): Int {
        var n = 0
        for (b in books) {
            if (n >= max) break
            if (b.trashed || b.type == "comic") continue
            val cache = File(ctx.filesDir, "flow/${b.id}.txt")
            val src = fileOf(b)
            val paras: List<String>
            val stamp: Long
            if (cache.exists()) {
                stamp = cache.length() + cache.lastModified()
                if (readableDatabase.rawQuery("SELECT size FROM meta WHERE book=?", arrayOf(b.id)).use { it.moveToFirst() && it.getLong(0) == stamp }) continue
                paras = cache.readText().split("\n\n").filter { it.isNotBlank() }
            } else if (b.type != "pdf" && src.exists() && src.length() < 40_000_000) {
                stamp = src.length() + src.lastModified()
                if (readableDatabase.rawQuery("SELECT size FROM meta WHERE book=?", arrayOf(b.id)).use { it.moveToFirst() && it.getLong(0) == stamp }) continue
                paras = runCatching { Formats.paragraphs(b.type, src) }.getOrDefault(emptyList())
            } else continue
            if (index(b.id, paras, stamp)) n++
        }
        return n
    }
}
