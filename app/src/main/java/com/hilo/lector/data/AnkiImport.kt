package com.hilo.lector.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import org.jsoup.Jsoup
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Importar mazos de Anki: archivos .apkg (paquete de Anki) o .txt/.csv separados por tabulador o punto y coma.
 * Las tarjetas quedan en un mazo propio («mazo:Nombre») dentro de Estudio.
 */
object AnkiImport {
    class Result(val deck: String, val cards: List<StudyCard>)

    private val CLOZE = Regex("\\{\\{c\\d+::(.*?)(?:::(.*?))?\\}\\}")

    private fun clean(html: String): String {
        val withBreaks = html.replace(Regex("(?i)<br\\s*/?>|</div>|</p>"), "\n")
        return Jsoup.parse(withBreaks).wholeText().replace(Regex("\\[sound:[^]]+]"), "").replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun toCards(deckId: String, rows: List<Pair<String, String>>, start: Long): List<StudyCard> {
        var id = start
        val t = today()
        return rows.mapNotNull { (f0, b0) ->
            val front = clean(f0); val back = clean(b0)
            if (front.isBlank()) return@mapNotNull null
            val cz = CLOZE.find(f0)
            if (cz != null) {
                val answer = clean(cz.groupValues[1])
                val f = clean(CLOZE.replace(f0) { m -> if (m == cz) "[ … ]" else m.groupValues[1] })
                StudyCard(id++, deckId, "cloze", f, answer, context = clean(CLOZE.replace(f0) { it.groupValues[1] }), source = "Anki", due = t, created = System.currentTimeMillis())
            } else {
                StudyCard(id++, deckId, "qa", front, back.ifBlank { "—" }, source = "Anki", due = t, created = System.currentTimeMillis())
            }
        }
    }

    fun read(ctx: Context, uri: Uri, name: String, startId: Long): Result {
        val deck = name.substringBeforeLast('.').replace('_', ' ').trim().ifBlank { "Mazo de Anki" }
        val deckId = "mazo:$deck"
        val lower = name.lowercase()
        if (lower.endsWith(".txt") || lower.endsWith(".csv") || lower.endsWith(".tsv")) {
            val text = ctx.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: error("No pude leer el archivo")
            val sep = if (text.lineSequence().take(20).count { it.contains('\t') } >= 2) '\t' else ';'
            val rows = text.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.mapNotNull { l ->
                val p = l.split(sep)
                if (p.size >= 2) p[0] to p[1] else null
            }.toList()
            return Result(deck, toCards(deckId, rows, startId))
        }
        // .apkg: zip con la base de datos de Anki
        val tmp = File(ctx.cacheDir, "anki").apply { deleteRecursively(); mkdirs() }
        var db: File? = null
        var newOnly = false
        ctx.contentResolver.openInputStream(uri)?.use { inp ->
            ZipInputStream(inp).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    when (e.name) {
                        "collection.anki21", "collection.anki2" -> {
                            val f = File(tmp, e.name)
                            f.outputStream().use { z.copyTo(it) }
                            if (db == null || e.name == "collection.anki21") db = f
                        }
                        "collection.anki21b" -> newOnly = true
                    }
                }
            }
        } ?: error("No pude leer el archivo")
        val f = db ?: error(if (newOnly) "nuevo" else "No es un mazo de Anki")
        val rows = ArrayList<Pair<String, String>>()
        SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY).use { d ->
            d.rawQuery("SELECT flds FROM notes", null).use { c ->
                while (c.moveToNext()) {
                    val parts = c.getString(0).split('\u001f')
                    rows.add(parts.getOrElse(0) { "" } to parts.getOrElse(1) { "" })
                }
            }
        }
        tmp.deleteRecursively()
        // Los paquetes nuevos traen una base «de compatibilidad» con una sola nota de aviso
        if (rows.size <= 1 && newOnly) error("nuevo")
        return Result(deck, toCards(deckId, rows, startId))
    }
}
