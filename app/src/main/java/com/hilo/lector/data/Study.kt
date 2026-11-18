package com.hilo.lector.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.hilo.lector.Hilo
import com.hilo.lector.text.Gloss
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
    val stab: Float = 0f,          // FSRS: estabilidad (días para bajar al 90 % de recuerdo)
    val diff: Float = 0f,          // FSRS: dificultad (1 fácil … 10 difícil)
    val last: Long = -1,           // último repaso (día)
) {
    /** Probabilidad de recordarla hoy (0 a 1). */
    fun recall(day: Long = today()): Float = if (last < 0 || stab <= 0f) (if (reps == 0) 0f else 0.5f) else Fsrs.retrievability((day - last).toFloat(), stab)
    /** Tarjeta frágil: la fallaste varias veces o es muy probable que la olvides. */
    val fragile: Boolean get() = lapses >= 3 || (reps > 0 && diff >= 8.5f)
}

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
    val kind: String = "",         // definicion | tesis | ejemplo | critica | dato | duda ("" = resaltado común)
)

/** Tipos de subrayado con su color, como en la lectura activa con código de colores. */
val MARK_KINDS = listOf(
    Triple("definicion", "Definición", 0xFF4FC3F7.toInt()),
    Triple("tesis", "Tesis", 0xFFFFD54F.toInt()),
    Triple("ejemplo", "Ejemplo", 0xFF81C784.toInt()),
    Triple("critica", "Crítica", 0xFFF48FB1.toInt()),
    Triple("dato", "Dato", 0xFFCE93D8.toInt()),
    Triple("duda", "No entiendo", 0xFFFF8A65.toInt()),
)

enum class Grade { AGAIN, HARD, GOOD, EASY }

/** Ficha de estudio: de caso o sentencia, de lectura o de autor. Cada campo es (nombre, contenido). */
data class Ficha(
    val id: Long,
    val bookId: String,
    val kind: String,              // caso | lectura | autor
    val title: String,
    val fields: List<Pair<String, String>>,
    val time: Long,
)

val FICHA_KINDS = listOf(
    "caso" to "Ficha de caso o sentencia",
    "lectura" to "Ficha de lectura",
    "autor" to "Ficha de autor",
)

fun fichaFields(kind: String): List<String> = when (kind) {
    "caso" -> listOf("Datos (tribunal, fecha, partes)", "Hechos", "Problema jurídico", "Norma aplicable", "Argumentos", "Decisión o fallo", "Mi comentario")
    "autor" -> listOf("Autor", "Contexto y época", "Conceptos clave", "Ideas principales", "Obras y fechas", "Citas")
    else -> listOf("Referencia", "Tesis central", "Argumentos principales", "Conceptos clave", "Citas importantes", "Crítica personal")
}

/** Cita textual guardada para un trabajo, con su página o párrafo. */
data class Quote(val id: Long, val bookId: String, val text: String, val loc: String, val pos: Int, val flow: Boolean, val time: Long)

/** Datos del documento para la referencia APA. */
data class BookRef(val authors: String = "", val year: String = "", val title: String = "", val publisher: String = "", val url: String = "")

fun today(): Long = LocalDate.now().toEpochDay()

/**
 * FSRS-5: el algoritmo de repaso espaciado moderno (el mismo de Anki desde 2023).
 * Estima para cada tarjeta su estabilidad y dificultad, y la programa justo antes de que la olvides.
 */
object Fsrs {
    private val w = floatArrayOf(
        0.40255f, 1.18385f, 3.173f, 15.69105f, 7.1949f, 0.5345f, 1.4604f, 0.0046f, 1.54575f, 0.1192f,
        1.01925f, 1.9395f, 0.11f, 0.29605f, 2.2698f, 0.2315f, 2.9898f, 0.51655f, 0.6621f,
    )
    private const val DECAY = -0.5f
    private const val FACTOR = 19f / 81f
    const val RETENTION = 0.9f

    fun retrievability(t: Float, s: Float): Float = Math.pow((1.0 + FACTOR * t / s), DECAY.toDouble()).toFloat()

    private fun d0(g: Int) = (w[4] - kotlin.math.exp(w[5] * (g - 1)) + 1f).coerceIn(1f, 10f)

    private fun nextD(d: Float, g: Int): Float {
        val dp = d + (-w[6] * (g - 3)) * (10f - d) / 9f
        return (w[7] * d0(4) + (1 - w[7]) * dp).coerceIn(1f, 10f)
    }

    private fun success(d: Float, s: Float, r: Float, g: Int): Float {
        val hard = if (g == 2) w[15] else 1f
        val easy = if (g == 4) w[16] else 1f
        return s * (1f + kotlin.math.exp(w[8]) * (11f - d) * Math.pow(s.toDouble(), -w[9].toDouble()).toFloat() *
            (kotlin.math.exp(w[10] * (1f - r)) - 1f) * hard * easy)
    }

    private fun fail(d: Float, s: Float, r: Float): Float =
        minOf(s, w[11] * Math.pow(d.toDouble(), -w[12].toDouble()).toFloat() * (Math.pow((s + 1.0), w[13].toDouble()).toFloat() - 1f) * kotlin.math.exp(w[14] * (1f - r)))

    fun interval(s: Float): Int = (s / FACTOR * (Math.pow(RETENTION.toDouble(), (1.0 / DECAY)).toFloat() - 1f)).roundToInt().coerceIn(1, 36500)

    /** Estado nuevo de la tarjeta después de responder [g] (1 otra vez, 2 difícil, 3 bien, 4 fácil). */
    fun next(c: StudyCard, g: Int, day: Long): StudyCard {
        var s = c.stab
        var d = c.diff
        // Tarjetas que venían del sistema anterior: se estima su estado con lo que había
        if (s <= 0f && c.reps > 0) { s = maxOf(0.5f, c.interval.toFloat()); d = (11f - (c.ease - 1.3f) * 5f).coerceIn(1f, 10f) }
        val first = s <= 0f
        if (first) {
            s = w[g - 1]; d = d0(g)
        } else {
            val elapsed = if (c.last >= 0) (day - c.last).toFloat().coerceAtLeast(0f) else c.interval.toFloat()
            val r = retrievability(elapsed, s)
            if (elapsed < 1f) {
                s *= kotlin.math.exp(w[17] * (g - 3 + w[18]))
            } else {
                s = if (g == 1) fail(d, s, r) else success(d, s, r, g)
            }
            d = nextD(d, g)
        }
        s = s.coerceIn(0.1f, 36500f)
        val iv = if (g == 1) 0 else interval(s)
        return c.copy(
            stab = s, diff = d, last = day, interval = iv, due = day + iv,
            reps = if (g == 1) 0 else c.reps + 1, lapses = if (g == 1 && !first) c.lapses + 1 else c.lapses,
        )
    }
}

/** Repaso espaciado: usa FSRS. Se mantiene el nombre para el resto de la app. */
object Srs {
    fun schedule(c: StudyCard, g: Grade, day: Long = today()): StudyCard = Fsrs.next(c, g.ordinal + 1, day)

    /** Texto para el botón: cuándo vuelve a aparecer. */
    fun preview(c: StudyCard, g: Grade): String {
        val d = schedule(c, g, today()).interval
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
    /** Palabras quitadas del análisis, por documento ("*" = en todos). */
    val removed = mutableStateMapOf<String, Set<String>>()
    /** Glosario: términos automáticos que quitaste y los que agregaste o corregiste. */
    val glossHidden = mutableStateMapOf<String, Set<String>>()
    val glossCustom = mutableStateMapOf<String, List<Gloss>>()
    /** Capítulos marcados como estudiados (párrafo donde empieza). */
    val chapDone = mutableStateMapOf<String, Set<Int>>()
    val fichas: SnapshotStateList<Ficha> = mutableStateListOf()
    val quotes: SnapshotStateList<Quote> = mutableStateListOf()
    val refs = mutableStateMapOf<String, BookRef>()
    /** Fecha de examen por documento (día, LocalDate.toEpochDay). */
    val exams = mutableStateMapOf<String, Long>()
    private var nextId = System.currentTimeMillis()

    var remindOn: Boolean
        get() = sp.getBoolean("remindOn", false)
        set(v) { sp.edit().putBoolean("remindOn", v).apply(); remindTick++ }
    var remindHour: Int
        get() = sp.getInt("remindHour", 19)
        set(v) { sp.edit().putInt("remindHour", v).apply(); remindTick++ }
    var remindTick by mutableIntStateOf(0)
        private set
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

    /** Agrega muchas tarjetas de una vez (tarjetas de toda una ley, todos los plazos…). */
    fun addAll(list: List<StudyCard>) { if (list.isNotEmpty()) { cards.addAll(list); save() } }

    fun update(c: StudyCard) {
        val i = cards.indexOfFirst { it.id == c.id }
        if (i >= 0) { cards[i] = c; save() }
    }

    fun delete(id: Long) { cards.removeAll { it.id == id }; save() }

    /** Registro de repasos (para estadísticas y calibración), lo guarda [Motor]. */
    var onReview: ((StudyCard, Grade, Int) -> Unit)? = null

    fun grade(c: StudyCard, g: Grade, conf: Int = 0) {
        onReview?.invoke(c, g, conf)
        val t = today()
        var nc = Srs.schedule(c, g)
        // Con examen cerca, la tarjeta vuelve antes del examen
        val ex = exams[c.bookId]
        if (ex != null && ex > t && nc.due >= ex) nc = nc.copy(due = max(t + if (g == Grade.AGAIN) 0 else 1, ex - 1))
        update(nc)
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

    fun removedFor(bookId: String): Set<String> = removed[bookId].orEmpty() + removed["*"].orEmpty()

    fun removeWord(bookId: String, key: String, everywhere: Boolean) {
        val k = if (everywhere) "*" else bookId
        removed[k] = removed[k].orEmpty() + key
        save()
    }

    fun restoreWord(bookId: String, key: String) {
        removed[bookId] = removed[bookId].orEmpty() - key
        removed["*"] = removed["*"].orEmpty() - key
        save()
    }

    fun hideGloss(bookId: String, term: String) {
        glossHidden[bookId] = glossHidden[bookId].orEmpty() + term.lowercase()
        glossCustom[bookId] = glossCustom[bookId].orEmpty().filterNot { it.term.equals(term, true) }
        save()
    }

    fun putGloss(bookId: String, g: Gloss, replaces: String?) {
        if (replaces != null) glossHidden[bookId] = glossHidden[bookId].orEmpty() + replaces.lowercase()
        glossCustom[bookId] = glossCustom[bookId].orEmpty().filterNot { it.term.equals(g.term, true) || (replaces != null && it.term.equals(replaces, true)) } + g
        glossHidden[bookId] = glossHidden[bookId].orEmpty() - g.term.lowercase()
        save()
    }

    fun toggleChapter(bookId: String, start: Int) {
        val cur = chapDone[bookId].orEmpty()
        chapDone[bookId] = if (start in cur) cur - start else cur + start
        save()
    }

    fun fichasOf(bookId: String) = fichas.filter { it.bookId == bookId }.sortedByDescending { it.time }
    fun putFicha(f: Ficha) {
        val i = fichas.indexOfFirst { it.id == f.id }
        if (i >= 0) fichas[i] = f else fichas.add(f)
        save()
    }
    fun deleteFicha(id: Long) { fichas.removeAll { it.id == id }; save() }

    fun quotesOf(bookId: String) = quotes.filter { it.bookId == bookId }.sortedBy { it.time }
    fun addQuote(q: Quote) { quotes.add(q); save() }
    fun deleteQuote(id: Long) { quotes.removeAll { it.id == id }; save() }
    fun setRef(bookId: String, r: BookRef) { refs[bookId] = r; save() }

    fun setExam(bookId: String, day: Long?) {
        if (day == null) exams.remove(bookId) else exams[bookId] = day
        // Las tarjetas que vencerían después del examen vuelven antes
        if (day != null) {
            val t = today()
            for (i in cards.indices) {
                val c = cards[i]
                if (c.bookId == bookId && c.due >= day && day > t) cards[i] = c.copy(due = max(t, day - 1))
            }
        }
        save()
    }

    var onRemoveBook: ((String) -> Unit)? = null

    fun removeBook(bookId: String) {
        onRemoveBook?.invoke(bookId)
        cards.removeAll { it.bookId == bookId }
        marks.removeAll { it.bookId == bookId }
        notes.remove(bookId)
        langs.remove(bookId)
        fichas.removeAll { it.bookId == bookId }
        quotes.removeAll { it.bookId == bookId }
        exams.remove(bookId)
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
                            stab = j.optDouble("s", 0.0).toFloat(), diff = j.optDouble("d", 0.0).toFloat(), last = j.optLong("last", -1),
                        )
                    )
                }
            }
            o.optJSONArray("marks")?.let { a ->
                for (i in 0 until a.length()) {
                    val j = a.getJSONObject(i)
                    marks.add(TextMark(j.getLong("id"), j.optString("book"), j.optInt("para"), j.optInt("start"), j.optInt("end"), j.optString("text"), j.optInt("color"), j.optLong("time"), j.optString("k")))
                }
            }
            o.optJSONObject("notes")?.let { n -> n.keys().forEach { k -> notes[k] = n.optString(k) } }
            o.optJSONObject("langs")?.let { n -> n.keys().forEach { k -> langs[k] = n.optString(k) } }
            fun strSets(name: String, into: MutableMap<String, Set<String>>) {
                o.optJSONObject(name)?.let { n -> n.keys().forEach { k -> val a = n.getJSONArray(k); into[k] = (0 until a.length()).map { a.getString(it) }.toSet() } }
            }
            strSets("removed", removed)
            strSets("glossHidden", glossHidden)
            o.optJSONObject("chapDone")?.let { n -> n.keys().forEach { k -> val a = n.getJSONArray(k); chapDone[k] = (0 until a.length()).map { a.getInt(it) }.toSet() } }
            o.optJSONObject("glossCustom")?.let { n ->
                n.keys().forEach { k ->
                    val a = n.getJSONArray(k)
                    glossCustom[k] = (0 until a.length()).map { val j = a.getJSONObject(it); Gloss(j.optString("t"), j.optString("d"), j.optInt("p", -1)) }
                }
            }
            o.optJSONArray("fichas")?.let { a ->
                for (i in 0 until a.length()) {
                    val j = a.getJSONObject(i)
                    val fa = j.optJSONArray("f") ?: JSONArray()
                    fichas.add(Ficha(j.getLong("id"), j.optString("book"), j.optString("kind"), j.optString("title"),
                        (0 until fa.length()).map { val x = fa.getJSONObject(it); x.optString("n") to x.optString("v") }, j.optLong("time")))
                }
            }
            o.optJSONArray("quotes")?.let { a ->
                for (i in 0 until a.length()) {
                    val j = a.getJSONObject(i)
                    quotes.add(Quote(j.getLong("id"), j.optString("book"), j.optString("text"), j.optString("loc"), j.optInt("pos", -1), j.optBoolean("flow"), j.optLong("time")))
                }
            }
            o.optJSONObject("refs")?.let { n ->
                n.keys().forEach { k -> val j = n.getJSONObject(k); refs[k] = BookRef(j.optString("a"), j.optString("y"), j.optString("t"), j.optString("p"), j.optString("u")) }
            }
            o.optJSONObject("exams")?.let { n -> n.keys().forEach { k -> exams[k] = n.optLong(k) } }
        } catch (_: Exception) {}
    }

    /** Guarda ya mismo (antes de exportar una copia de seguridad). */
    fun flush() { saveJob?.cancel(); writeNow() }

    private fun save() {
        saveJob?.cancel()
        saveJob = Hilo.scope.launch(Dispatchers.IO) {
            delay(300)
            writeNow()
        }
    }

    private fun writeNow() {
        run {
            val ca = JSONArray()
            for (c in cards.toList()) {
                ca.put(
                    JSONObject().put("id", c.id).put("book", c.bookId).put("kind", c.kind).put("front", c.front)
                        .put("back", c.back).put("ctx", c.context).put("lang", c.lang).put("src", c.source)
                        .put("srcPos", c.sourcePos).put("srcFlow", c.sourceFlow).put("ease", c.ease.toDouble())
                        .put("iv", c.interval).put("reps", c.reps).put("lapses", c.lapses).put("due", c.due).put("created", c.created)
                        .put("s", c.stab.toDouble()).put("d", c.diff.toDouble()).put("last", c.last)
                )
            }
            val ma = JSONArray()
            for (m in marks.toList()) {
                ma.put(JSONObject().put("id", m.id).put("book", m.bookId).put("para", m.para).put("start", m.start).put("end", m.end).put("text", m.text).put("color", m.color).put("time", m.time).put("k", m.kind))
            }
            val no = JSONObject(); notes.toMap().forEach { (k, v) -> no.put(k, v) }
            val la = JSONObject(); langs.toMap().forEach { (k, v) -> la.put(k, v) }
            fun sets(m: Map<String, Set<*>>): JSONObject { val o = JSONObject(); m.forEach { (k, v) -> o.put(k, JSONArray(v.toList())) }; return o }
            val gc = JSONObject()
            glossCustom.toMap().forEach { (k, v) ->
                val a = JSONArray(); v.forEach { a.put(JSONObject().put("t", it.term).put("d", it.definition).put("p", it.para)) }; gc.put(k, a)
            }
            val text = JSONObject().put("cards", ca).put("marks", ma).put("notes", no).put("langs", la)
                .put("removed", sets(removed.toMap())).put("glossHidden", sets(glossHidden.toMap()))
                .put("chapDone", sets(chapDone.toMap())).put("glossCustom", gc)
                .put("fichas", JSONArray().also { arr ->
                    fichas.toList().forEach { f ->
                        val fa = JSONArray(); f.fields.forEach { (n, v) -> fa.put(JSONObject().put("n", n).put("v", v)) }
                        arr.put(JSONObject().put("id", f.id).put("book", f.bookId).put("kind", f.kind).put("title", f.title).put("f", fa).put("time", f.time))
                    }
                })
                .put("quotes", JSONArray().also { arr ->
                    quotes.toList().forEach { q -> arr.put(JSONObject().put("id", q.id).put("book", q.bookId).put("text", q.text).put("loc", q.loc).put("pos", q.pos).put("flow", q.flow).put("time", q.time)) }
                })
                .put("refs", JSONObject().also { o2 -> refs.toMap().forEach { (k, r) -> o2.put(k, JSONObject().put("a", r.authors).put("y", r.year).put("t", r.title).put("p", r.publisher).put("u", r.url)) } })
                .put("exams", JSONObject().also { o2 -> exams.toMap().forEach { (k, v) -> o2.put(k, v) } })
                .toString()
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
