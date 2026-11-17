package com.hilo.lector.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.hilo.lector.Hilo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Una tarjeta de estudio.
 * - qa: pregunta y respuesta
 * - vocab: palabra o expresión y su significado (idiomas), con la oración donde apareció
 * - cloze: oración con un hueco para completar
 */
data class StudyCard(
    val id: Long,
    val bookId: String,
    val kind: String,
    val front: String,
    val back: String,
    val context: String = "",
    val lang: String = "",
    val source: String = "",       // "pág. 12", "párrafo 30"
    val sourcePos: Int = -1,
    val sourceFlow: Boolean = false,
    val ease: Float = 2.5f,
    val interval: Int = 0,         // días
    val reps: Int = 0,
    val lapses: Int = 0,
    val due: Long = 0,             // día (LocalDate.toEpochDay)
    val created: Long = 0,
)

/** Texto resaltado en el modo de texto: párrafo y rango de letras. */
data class TextMark(
    val id: Long,
    val bookId: String,
    val para: Int,
    val start: Int,
    val end: Int,
    val text: String,
    val color: Int,
    val time: Long,
)

enum class Grade { AGAIN, HARD, GOOD, EASY }

fun today(): Long = LocalDate.now().toEpochDay()

/** Repaso espaciado al estilo Anki (SM-2 simplificado). */
object Srs {
    fun schedule(c: StudyCard, g: Grade, day: Long = today()): StudyCard {
        val iv: Int
        var ease = c.ease
        var reps = c.reps
        var lapses = c.lapses
        when (g) {
            Grade.AGAIN -> { iv = 0; reps = 0; lapses++; ease = max(1.3f, ease - 0.2f) }
            Grade.HARD -> { iv = if (c.reps == 0) 1 else max(1, (c.interval * 1.2f).roundToInt()); ease = max(1.3f, ease - 0.15f); reps++ }
            Grade.GOOD -> { iv = when (c.reps) { 0 -> 1; 1 -> 3; else -> max(c.interval + 1, (c.interval * ease).roundToInt()) }; reps++ }
            Grade.EASY -> { iv = if (c.reps == 0) 4 else max(c.interval + 2, (c.interval * ease * 1.3f).roundToInt()); ease += 0.15f; reps++ }
        }
        return c.copy(interval = iv, ease = ease, reps = reps, lapses = lapses, due = day + iv)
    }

    /** Texto para el botón: cuándo vuelve a aparecer. */
    fun preview(c: StudyCard, g: Grade): String {
        val d = schedule(c, g, 0).interval
        return when {
            d == 0 -> "otra vez hoy"
            d == 1 -> "mañana"
            d < 30 -> "$d días"
            d < 365 -> "${(d / 30f).roundToInt()} meses"
            else -> "${(d / 365f).roundToInt()} años"
        }
    }
}

/** Tarjetas, resaltados, apuntes e idioma de cada documento. Se guarda solo en el celular. */
class StudyStore(ctx: Context) {
    private val file = File(ctx.filesDir, "estudio/estudio.json")
    private val sp = ctx.getSharedPreferences("estudio", Context.MODE_PRIVATE)
    val cards: SnapshotStateList<StudyCard> = mutableStateListOf()
    val marks: SnapshotStateList<TextMark> = mutableStateListOf()
    val notes = mutableStateMapOf<String, String>()
    val langs = mutableStateMapOf<String, String>()
    private var nextId = System.currentTimeMillis()
    private var saveJob: Job? = null

    var streak by mutableIntStateOf(0)
        private set
    var todayCount by mutableIntStateOf(0)
        private set

    init {
        load()
        val last = sp.getLong("lastDay", -10)
        val t = today()
        streak = if (last >= t - 1) sp.getInt("streak", 0) else 0
        todayCount = if (last == t) sp.getInt("todayCount", 0) else 0
    }

    fun newId() = nextId++

    fun due(bookId: String? = null, day: Long = today()): List<StudyCard> =
        cards.filter { (bookId == null || it.bookId == bookId) && it.due <= day }.sortedWith(compareBy({ it.due }, { it.created }))

    fun cardsOf(bookId: String) = cards.filter { it.bookId == bookId }

    fun add(c: StudyCard) { cards.add(c); save() }

    fun update(c: StudyCard) {
        val i = cards.indexOfFirst { it.id == c.id }
        if (i >= 0) { cards[i] = c; save() }
    }

    fun delete(id: Long) { cards.removeAll { it.id == id }; save() }

    fun grade(c: StudyCard, g: Grade) {
        update(Srs.schedule(c, g))
        val t = today()
        val last = sp.getLong("lastDay", -10)
        when {
            last == t -> todayCount++
            last == t - 1 -> { streak++; todayCount = 1 }
            else -> { streak = 1; todayCount = 1 }
        }
        sp.edit().putLong("lastDay", t).putInt("streak", streak).putInt("todayCount", todayCount).apply()
    }

    fun addMark(m: TextMark) { marks.add(m); save() }
    fun deleteMark(id: Long) { marks.removeAll { it.id == id }; save() }
    fun marksOf(bookId: String) = marks.filter { it.bookId == bookId }

    fun setNote(bookId: String, text: String) { notes[bookId] = text; save() }
    fun setLang(bookId: String, lang: String) { langs[bookId] = lang; save() }

    fun removeBook(bookId: String) {
        cards.removeAll { it.bookId == bookId }
        marks.removeAll { it.bookId == bookId }
        notes.remove(bookId)
        langs.remove(bookId)
        save()
    }

    private fun load() {
        try {
            if (!file.exists()) return
            val o = JSONObject(file.readText())
            o.optJSONArray("cards")?.let { a ->
                for (i in 0 until a.length()) {
                    val j = a.getJSONObject(i)
                    cards.add(
                        StudyCard(
                            id = j.getLong("id"), bookId = j.optString("book"), kind = j.optString("kind", "qa"),
                            front = j.optString("front"), back = j.optString("back"), context = j.optString("ctx"),
                            lang = j.optString("lang"), source = j.optString("src"), sourcePos = j.optInt("srcPos", -1),
                            sourceFlow = j.optBoolean("srcFlow"), ease = j.optDouble("ease", 2.5).toFloat(),
                            interval = j.optInt("iv"), reps = j.optInt("reps"), lapses = j.optInt("lapses"),
                            due = j.optLong("due"), created = j.optLong("created"),
                        )
                    )
                }
            }
            o.optJSONArray("marks")?.let { a ->
                for (i in 0 until a.length()) {
                    val j = a.getJSONObject(i)
                    marks.add(TextMark(j.getLong("id"), j.optString("book"), j.optInt("para"), j.optInt("start"), j.optInt("end"), j.optString("text"), j.optInt("color"), j.optLong("time")))
                }
            }
            o.optJSONObject("notes")?.let { n -> n.keys().forEach { k -> notes[k] = n.optString(k) } }
            o.optJSONObject("langs")?.let { n -> n.keys().forEach { k -> langs[k] = n.optString(k) } }
        } catch (_: Exception) {}
    }

    private fun save() {
        saveJob?.cancel()
        saveJob = Hilo.scope.launch(Dispatchers.IO) {
            delay(300)
            val ca = JSONArray()
            for (c in cards.toList()) {
                ca.put(
                    JSONObject().put("id", c.id).put("book", c.bookId).put("kind", c.kind).put("front", c.front)
                        .put("back", c.back).put("ctx", c.context).put("lang", c.lang).put("src", c.source)
                        .put("srcPos", c.sourcePos).put("srcFlow", c.sourceFlow).put("ease", c.ease.toDouble())
                        .put("iv", c.interval).put("reps", c.reps).put("lapses", c.lapses).put("due", c.due).put("created", c.created)
                )
            }
            val ma = JSONArray()
            for (m in marks.toList()) {
                ma.put(JSONObject().put("id", m.id).put("book", m.bookId).put("para", m.para).put("start", m.start).put("end", m.end).put("text", m.text).put("color", m.color).put("time", m.time))
            }
            val no = JSONObject(); notes.toMap().forEach { (k, v) -> no.put(k, v) }
            val la = JSONObject(); langs.toMap().forEach { (k, v) -> la.put(k, v) }
            val text = JSONObject().put("cards", ca).put("marks", ma).put("notes", no).put("langs", la).toString()
            synchronized(file) {
                file.parentFile?.mkdirs()
                val tmp = File(file.path + ".tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(file)) { file.writeText(text); tmp.delete() }
            }
        }
    }
}

/** Idiomas para las tarjetas de vocabulario y la voz. */
val STUDY_LANGS = listOf(
    "" to "Ninguno", "en" to "Inglés", "fr" to "Francés", "pt" to "Portugués",
    "de" to "Alemán", "it" to "Italiano", "es" to "Español",
)
