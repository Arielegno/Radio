package com.hilo.lector.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Una clase o materia: junta libros, documentos, fotos y apuntes de un mismo curso. */
data class Clase(
    val id: String,
    val name: String,
    val color: Int,
    val teacher: String = "",
    val books: List<String> = emptyList(),
    val created: Long = System.currentTimeMillis(),
)

/** Una entrada del diario de la clase. [important]: «va para el examen». [audio]: grabación. */
data class ClassNote(
    val id: Long,
    val classId: String,
    val day: Long,
    val text: String,
    val important: Boolean = false,
    val audio: String = "",
)

/** Algo con fecha: examen, entrega o lectura. */
data class ClassEvent(
    val id: Long,
    val classId: String,
    val day: Long,
    val kind: String,          // examen | entrega | lectura | otro
    val title: String,
    val done: Boolean = false,
)

val CLASS_COLORS = listOf(0xFF2E9E8F, 0xFF5B7CFA, 0xFFE0644F, 0xFFD39B1E, 0xFF9C5BD1, 0xFF3BA55C, 0xFFE05297, 0xFF607D8B).map { it.toInt() }

val EVENT_KINDS = listOf("examen" to "Examen", "entrega" to "Entrega", "lectura" to "Lectura", "otro" to "Otro")

class ClassStore(private val ctx: Context) {
    val classes: SnapshotStateList<Clase> = mutableStateListOf()
    val notes: SnapshotStateList<ClassNote> = mutableStateListOf()
    val events: SnapshotStateList<ClassEvent> = mutableStateListOf()
    private val file = File(ctx.filesDir, "clases.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null

    /** Carpeta para las grabaciones de una clase. */
    fun audioDir(classId: String) = File(ctx.filesDir, "clases/$classId").apply { mkdirs() }

    init { load() }

    fun get(id: String) = classes.firstOrNull { it.id == id }
    fun notesOf(id: String) = notes.filter { it.classId == id }.sortedByDescending { it.day * 1_000_000 + it.id % 1_000_000 }
    fun eventsOf(id: String) = events.filter { it.classId == id }.sortedBy { it.day }
    fun classesOf(bookId: String) = classes.filter { bookId in it.books }
    fun newId() = System.currentTimeMillis() * 10 + (0..9).random()

    fun create(name: String, color: Int, teacher: String): Clase {
        val c = Clase("c" + System.currentTimeMillis().toString(36), name.trim().ifBlank { "Clase nueva" }, color, teacher.trim())
        classes.add(c); save()
        return c
    }

    fun update(id: String, f: (Clase) -> Clase) {
        val i = classes.indexOfFirst { it.id == id }
        if (i >= 0) { classes[i] = f(classes[i]); save() }
    }

    fun delete(id: String) {
        classes.removeAll { it.id == id }
        notes.removeAll { it.classId == id }
        events.removeAll { it.classId == id }
        runCatching { audioDir(id).deleteRecursively() }
        save()
    }

    fun addBooks(id: String, ids: Collection<String>) = update(id) { c -> c.copy(books = (c.books + ids).distinct()) }
    fun removeBook(id: String, bookId: String) = update(id) { c -> c.copy(books = c.books - bookId) }

    /** Si un libro se borra de la biblioteca, sale de sus clases. */
    fun forgetBook(bookId: String) {
        classes.toList().forEach { c -> if (bookId in c.books) update(c.id) { it.copy(books = it.books - bookId) } }
    }

    fun putNote(n: ClassNote) {
        val i = notes.indexOfFirst { it.id == n.id }
        if (i >= 0) notes[i] = n else notes.add(n)
        save()
    }

    fun deleteNote(id: Long) {
        notes.firstOrNull { it.id == id }?.audio?.takeIf { it.isNotBlank() }?.let { runCatching { File(it).delete() } }
        notes.removeAll { it.id == id }; save()
    }

    fun putEvent(e: ClassEvent) {
        val i = events.indexOfFirst { it.id == e.id }
        if (i >= 0) events[i] = e else events.add(e)
        save()
    }

    fun deleteEvent(id: Long) { events.removeAll { it.id == id }; save() }

    /** Próximo examen de la clase (o null). */
    fun nextExam(id: String, from: Long = today()): ClassEvent? =
        events.filter { it.classId == id && it.kind == "examen" && !it.done && it.day >= from }.minByOrNull { it.day }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            val o = JSONObject(file.readText())
            o.optJSONArray("clases")?.let { a ->
                for (i in 0 until a.length()) {
                    val j = a.getJSONObject(i)
                    val bs = j.optJSONArray("libros")
                    classes.add(
                        Clase(
                            j.getString("id"), j.optString("nombre"), j.optInt("color", CLASS_COLORS[0]), j.optString("profe"),
                            if (bs == null) emptyList() else (0 until bs.length()).map { bs.getString(it) }, j.optLong("creada"),
                        )
                    )
                }
            }
            o.optJSONArray("diario")?.let { a ->
                for (i in 0 until a.length()) {
                    val j = a.getJSONObject(i)
                    notes.add(ClassNote(j.getLong("id"), j.getString("clase"), j.getLong("dia"), j.optString("texto"), j.optBoolean("importante"), j.optString("audio")))
                }
            }
            o.optJSONArray("fechas")?.let { a ->
                for (i in 0 until a.length()) {
                    val j = a.getJSONObject(i)
                    events.add(ClassEvent(j.getLong("id"), j.getString("clase"), j.getLong("dia"), j.optString("tipo", "otro"), j.optString("titulo"), j.optBoolean("hecho")))
                }
            }
        }
    }

    private fun save() {
        val snap = Triple(classes.toList(), notes.toList(), events.toList())
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            val o = JSONObject()
            o.put("clases", JSONArray().apply {
                snap.first.forEach { c ->
                    put(JSONObject().put("id", c.id).put("nombre", c.name).put("color", c.color).put("profe", c.teacher)
                        .put("libros", JSONArray(c.books)).put("creada", c.created))
                }
            })
            o.put("diario", JSONArray().apply {
                snap.second.forEach { n ->
                    put(JSONObject().put("id", n.id).put("clase", n.classId).put("dia", n.day).put("texto", n.text).put("importante", n.important).put("audio", n.audio))
                }
            })
            o.put("fechas", JSONArray().apply {
                snap.third.forEach { e ->
                    put(JSONObject().put("id", e.id).put("clase", e.classId).put("dia", e.day).put("tipo", e.kind).put("titulo", e.title).put("hecho", e.done))
                }
            })
            val tmp = File(file.path + ".tmp")
            tmp.writeText(o.toString())
            tmp.renameTo(file)
        }
    }
}
