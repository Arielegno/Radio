package com.hilo.lector.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.hilo.lector.Hilo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Un repaso: qué tarjeta, cuándo, cómo respondiste y qué tan seguro estabas. */
data class Review(
    val card: Long,
    val book: String,
    val day: Long,
    val grade: Int,       // 1 otra vez … 4 fácil
    val conf: Int,        // 0 sin decir, 1 seguro, 2 dudo, 3 no lo sé
    val kind: String,
) {
    val correct: Boolean get() = grade >= 3
}

/** Una entrada con fecha: diario sociológico, cierre de sesión, recuerdo libre, debate… */
data class Entry(val id: Long, val book: String, val kind: String, val title: String, val text: String, val time: Long, val score: Int = -1)

/**
 * Datos del motor de estudio: registro de repasos, entradas escritas y valores sueltos
 * (fases de la ruta, semáforo de conceptos, resúmenes, definiciones propias…). Se guarda en el celular.
 */
class Motor(ctx: Context) {
    private val file = File(ctx.filesDir, "estudio/motor.json")
    val reviews: SnapshotStateList<Review> = mutableStateListOf()
    val entries: SnapshotStateList<Entry> = mutableStateListOf()
    /** Valores sueltos por clave: "fase:<libro>:<n>", "sem:<libro>:<concepto>", "res:<libro>:<cap>", "def:<libro>:<término>"… */
    val kv = mutableStateMapOf<String, String>()
    private var saveJob: Job? = null
    private var nextId = System.currentTimeMillis()

    init { load() }

    fun newId() = nextId++

    fun log(c: StudyCard, g: Grade, conf: Int) {
        reviews.add(Review(c.id, c.bookId, today(), g.ordinal + 1, conf, c.kind))
        while (reviews.size > 30000) reviews.removeAt(0)
        save()
    }

    operator fun get(k: String): String = kv[k].orEmpty()
    fun put(k: String, v: String) { if (v.isEmpty()) kv.remove(k) else kv[k] = v; save() }
    fun flag(k: String): Boolean = kv[k] == "1"
    fun setFlag(k: String, on: Boolean) = put(k, if (on) "1" else "")

    fun add(e: Entry) { entries.add(e); save() }
    fun delete(id: Long) { entries.removeAll { it.id == id }; save() }
    fun entriesOf(kind: String, book: String? = null) = entries.filter { it.kind == kind && (book == null || it.book == book) }.sortedByDescending { it.time }

    fun removeBook(id: String) {
        reviews.removeAll { it.book == id }
        entries.removeAll { it.book == id }
        kv.keys.filter { it.split(':').getOrNull(1) == id }.forEach { kv.remove(it) }
        save()
    }

    // ---------- estadísticas ----------

    /** Calibración: de las veces que dijiste [conf], cuántas acertaste (0..1) y cuántas fueron. */
    fun calibration(conf: Int, book: String? = null, days: Int = 60): Pair<Float, Int> {
        val from = today() - days
        val l = reviews.filter { it.conf == conf && it.day >= from && (book == null || it.book == book) }
        return (if (l.isEmpty()) 0f else l.count { it.correct }.toFloat() / l.size) to l.size
    }

    /** Aciertos en los últimos [days] días. */
    fun accuracy(book: String? = null, days: Int = 30): Pair<Float, Int> {
        val from = today() - days
        val l = reviews.filter { it.day >= from && (book == null || it.book == book) }
        return (if (l.isEmpty()) 0f else l.count { it.correct }.toFloat() / l.size) to l.size
    }

    /** Repasos por día de los últimos [days] días (el último es hoy). */
    fun perDay(days: Int = 14): List<Int> {
        val t = today()
        val m = reviews.filter { it.day > t - days }.groupingBy { it.day }.eachCount()
        return (days - 1 downTo 0).map { m[t - it] ?: 0 }
    }

    // ---------- guardar ----------

    private fun load() {
        try {
            if (!file.exists()) return
            val o = JSONObject(file.readText())
            o.optJSONArray("r")?.let { a ->
                for (i in 0 until a.length()) {
                    val j = a.getJSONArray(i)
                    reviews.add(Review(j.getLong(0), j.getString(1), j.getLong(2), j.getInt(3), j.getInt(4), j.optString(5)))
                }
            }
            o.optJSONArray("e")?.let { a ->
                for (i in 0 until a.length()) {
                    val j = a.getJSONObject(i)
                    entries.add(Entry(j.getLong("id"), j.optString("b"), j.optString("k"), j.optString("t"), j.optString("x"), j.optLong("time"), j.optInt("s", -1)))
                }
            }
            o.optJSONObject("kv")?.let { n -> n.keys().forEach { k -> kv[k] = n.optString(k) } }
        } catch (_: Exception) {}
    }

    fun flush() { saveJob?.cancel(); write() }

    private fun save() {
        saveJob?.cancel()
        saveJob = Hilo.scope.launch(Dispatchers.IO) { delay(400); write() }
    }

    private fun write() {
        val ra = JSONArray()
        reviews.toList().forEach { r -> ra.put(JSONArray().put(r.card).put(r.book).put(r.day).put(r.grade).put(r.conf).put(r.kind)) }
        val ea = JSONArray()
        entries.toList().forEach { e ->
            ea.put(JSONObject().put("id", e.id).put("b", e.book).put("k", e.kind).put("t", e.title).put("x", e.text).put("time", e.time).put("s", e.score))
        }
        val kvo = JSONObject()
        kv.toMap().forEach { (k, v) -> kvo.put(k, v) }
        val text = JSONObject().put("r", ra).put("e", ea).put("kv", kvo).toString()
        synchronized(this) {
            file.parentFile?.mkdirs()
            val tmp = File(file.path + ".tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(file)) { file.writeText(text); tmp.delete() }
        }
    }
}
