package com.hilo.lector.pdf

import android.content.Context
import android.graphics.Color
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.hilo.lector.Hilo
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream

enum class Tool { NONE, HIGHLIGHT, PEN, ERASER, NOTE }

/** Un trazo sobre una página. Puntos y grosor en proporción al ancho/alto de la página. */
class InkStroke(
    val id: Long,
    val page: Int,
    val highlight: Boolean,
    val color: Int,
    val width: Float,
    val pts: FloatArray,
    /** Texto resaltado (cuando el resaltado se ajustó a una línea). */
    val text: String = "",
)

data class PdfNote(val id: Long, val page: Int, val x: Float, val y: Float, val text: String)

/** Anotaciones de un libro: se guardan solas en la app y se pueden exportar a un PDF. */
class AnnotStore(ctx: Context, bookId: String) {
    private val file = File(ctx.filesDir, "annot/$bookId.json")
    val strokes: SnapshotStateList<InkStroke> = mutableStateListOf()
    val notes: SnapshotStateList<PdfNote> = mutableStateListOf()
    private val history = ArrayList<Long>()
    private var nextId = System.currentTimeMillis()

    @Volatile private var loaded = false
    private var dirty = false

    // Leer el archivo fuera de la pantalla: con muchos trazos tardaba al abrir el PDF
    init {
        Hilo.scope.launch(Dispatchers.IO) {
            val (s, n) = load()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                strokes.addAll(0, s); notes.addAll(0, n)
                loaded = true
                if (dirty) save()   // lo que dibujaste mientras se leía también queda guardado
            }
        }
    }

    val isEmpty get() = strokes.isEmpty() && notes.isEmpty()
    val canUndo get() = history.isNotEmpty()

    fun newId() = nextId++

    fun addStroke(s: InkStroke) { strokes.add(s); history.add(s.id); save() }

    fun addNote(n: PdfNote) { notes.add(n); history.add(n.id); save() }

    fun editNote(id: Long, text: String) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) { notes[i] = notes[i].copy(text = text); save() }
    }

    fun deleteNote(id: Long) { notes.removeAll { it.id == id }; save() }

    /** Borra los trazos que pasan cerca del punto (x, y). */
    fun eraseAt(page: Int, x: Float, y: Float, aspect: Float, radius: Float = 0.025f): Boolean {
        val r2 = radius * radius
        val hit = strokes.filter { s ->
            if (s.page != page) return@filter false
            var k = 0
            var found = false
            while (k + 1 < s.pts.size) {
                val dx = s.pts[k] - x
                val dy = (s.pts[k + 1] - y) * aspect
                if (dx * dx + dy * dy < r2 + (s.width / 2) * (s.width / 2)) { found = true; break }
                k += 2
            }
            found
        }
        if (hit.isEmpty()) return false
        strokes.removeAll(hit)
        save()
        return true
    }

    fun undo() {
        val id = history.removeLastOrNull() ?: return
        if (!strokes.removeAll { it.id == id }) notes.removeAll { it.id == id }
        save()
    }

    private fun load(): Pair<List<InkStroke>, List<PdfNote>> {
        val strokes = ArrayList<InkStroke>()
        val notes = ArrayList<PdfNote>()
        try {
            if (!file.exists()) return strokes to notes
            val o = JSONObject(file.readText())
            val sa = o.optJSONArray("strokes") ?: JSONArray()
            for (i in 0 until sa.length()) {
                val s = sa.getJSONObject(i)
                val p = s.getJSONArray("p")
                strokes.add(
                    InkStroke(
                        s.getLong("id"), s.getInt("page"), s.optBoolean("hl"), s.getInt("color"),
                        s.getDouble("w").toFloat(), FloatArray(p.length()) { p.getDouble(it).toFloat() },
                        s.optString("t", ""),
                    )
                )
            }
            val na = o.optJSONArray("notes") ?: JSONArray()
            for (i in 0 until na.length()) {
                val n = na.getJSONObject(i)
                notes.add(PdfNote(n.getLong("id"), n.getInt("page"), n.getDouble("x").toFloat(), n.getDouble("y").toFloat(), n.getString("text")))
            }
        } catch (_: Exception) {}
        return strokes to notes
    }

    private fun save() {
        if (!loaded) { dirty = true; return }
        val sa = JSONArray()
        for (s in strokes) {
            val p = JSONArray()
            s.pts.forEach { p.put(it.toDouble()) }
            sa.put(JSONObject().put("id", s.id).put("page", s.page).put("hl", s.highlight).put("color", s.color).put("w", s.width.toDouble()).put("p", p).put("t", s.text))
        }
        val na = JSONArray()
        for (n in notes) na.put(JSONObject().put("id", n.id).put("page", n.page).put("x", n.x.toDouble()).put("y", n.y.toDouble()).put("text", n.text))
        val text = JSONObject().put("strokes", sa).put("notes", na).toString()
        Hilo.scope.launch(Dispatchers.IO) {
            synchronized(file) {
                file.parentFile?.mkdirs()
                file.writeText(text)
            }
        }
    }
}

/** Escribe los trazos y notas dentro de una copia del PDF original. */
object PdfExport {
    fun export(src: File, strokes: List<InkStroke>, notes: List<PdfNote>, out: OutputStream) {
        PDDocument.load(src).use { doc ->
            val sBy = strokes.groupBy { it.page }
            val nBy = notes.groupBy { it.page }
            for (i in 0 until doc.numberOfPages) {
                val ps = sBy[i]
                val pn = nBy[i]
                if (ps == null && pn == null) continue
                val page = doc.getPage(i)
                val box = page.cropBox
                val rot = ((page.rotation % 360) + 360) % 360
                val w = box.width
                val h = box.height
                val dispW = if (rot == 90 || rot == 270) h else w

                // De coordenadas en pantalla (0..1) a coordenadas del PDF
                fun map(dx: Float, dy: Float): Pair<Float, Float> {
                    val u: Float
                    val v: Float
                    when (rot) {
                        90 -> { u = dy; v = 1f - dx }
                        180 -> { u = 1f - dx; v = 1f - dy }
                        270 -> { u = 1f - dy; v = dx }
                        else -> { u = dx; v = dy }
                    }
                    return (box.lowerLeftX + u * w) to (box.upperRightY - v * h)
                }

                if (ps != null) {
                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        for (s in ps) {
                            if (s.pts.size < 2) continue
                            val gs = PDExtendedGraphicsState()
                            gs.strokingAlphaConstant = if (s.highlight) 0.4f else 1f
                            cs.setGraphicsStateParameters(gs)
                            cs.setStrokingColor(Color.red(s.color) / 255f, Color.green(s.color) / 255f, Color.blue(s.color) / 255f)
                            cs.setLineWidth(s.width * dispW)
                            cs.setLineCapStyle(if (s.highlight) 0 else 1)
                            cs.setLineJoinStyle(1)
                            val (x0, y0) = map(s.pts[0], s.pts[1])
                            cs.moveTo(x0, y0)
                            if (s.pts.size == 2) cs.lineTo(x0 + 0.5f, y0)
                            var k = 2
                            while (k + 1 < s.pts.size) {
                                val (x, y) = map(s.pts[k], s.pts[k + 1])
                                cs.lineTo(x, y)
                                k += 2
                            }
                            cs.stroke()
                        }
                    }
                }
                if (pn != null) {
                    val anns: MutableList<PDAnnotation> = page.annotations
                    for (n in pn) {
                        val (x, y) = map(n.x, n.y)
                        val a = PDAnnotationText()
                        a.contents = n.text
                        a.rectangle = PDRectangle(x - 10f, y - 10f, 20f, 20f)
                        a.name = PDAnnotationText.NAME_COMMENT
                        a.color = PDColor(floatArrayOf(1f, 0.78f, 0.2f), PDDeviceRGB.INSTANCE)
                        anns.add(a)
                    }
                    page.annotations = anns
                }
            }
            doc.save(out)
        }
    }
}
