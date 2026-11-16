package com.hilo.lector.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Una línea de texto en la página, en coordenadas de 0 a 1 (origen arriba a la izquierda). */
data class LineBox(val l: Float, val t: Float, val r: Float, val b: Float, val text: String)

/** Cómo se pinta la página: colores por línea y página oscura. */
data class PageStyle(
    val colors: Boolean,
    val dark: Boolean,
    val palette: List<Int>,
    val ink: Int,
    val intensity: Float,
) {
    val key: String = "$colors|$dark|${palette.joinToString(",")}|$ink|$intensity"
}

/**
 * Abre un PDF: dibuja páginas (formato original) y encuentra dónde está cada línea de texto.
 * Si la página es escaneada, reconoce el texto con ML Kit (sin internet).
 */
class PdfEngine(private val ctx: Context, private val file: File, bookId: String) {
    private val renderThread: ExecutorCoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val textThread: ExecutorCoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var pd: PDDocument? = null
    private val ocrLock = Mutex()
    private val lineCache = ConcurrentHashMap<Int, List<LineBox>>()
    private val lineDir = File(ctx.filesDir, "lines/$bookId").apply { mkdirs() }
    private var rec: TextRecognizer? = null
    private val recognizer: TextRecognizer
        get() = rec ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).also { rec = it }

    var pageCount = 0
        private set
    /** alto / ancho de cada página */
    var aspects = FloatArray(0)
        private set

    val bitmaps = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 5).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    suspend fun open() = withContext(renderThread) {
        val p = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pfd = p
        val r = PdfRenderer(p)
        renderer = r
        pageCount = r.pageCount
        aspects = FloatArray(pageCount) { i -> r.openPage(i).use { pg -> pg.height.toFloat() / max(1, pg.width) } }
    }

    suspend fun shutdown() {
        withContext(renderThread) {
            runCatching { renderer?.close() }
            runCatching { pfd?.close() }
            renderer = null; pfd = null
        }
        withContext(textThread) {
            runCatching { pd?.close() }
            pd = null
        }
        runCatching { rec?.close() }
        renderThread.close()
        textThread.close()
        bitmaps.evictAll()
    }

    // ---------- dibujar ----------

    suspend fun render(page: Int, widthPx: Int): Bitmap = withContext(renderThread) {
        val r = renderer ?: error("PDF cerrado")
        r.openPage(page).use { p ->
            val w = max(1, widthPx)
            val h = max(1, (w * p.height.toFloat() / max(1, p.width)).roundToInt())
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp
        }
    }

    fun rawKey(page: Int, w: Int) = "r$page@$w"
    fun styledKey(page: Int, w: Int, st: PageStyle) = "s$page@$w#${st.key}"

    // ---------- texto ----------

    fun peekLines(page: Int): List<LineBox>? = lineCache[page]

    suspend fun lines(page: Int, allowOcr: Boolean): List<LineBox> {
        lineCache[page]?.let { return it }
        val f = File(lineDir, "$page.json")
        if (f.exists()) {
            val cached = runCatching { decode(f.readText()) }.getOrNull()
            if (cached != null) { lineCache[page] = cached; return cached }
        }
        var res = withContext(textThread) { extract(page) }
        var settled = res.sumOf { it.text.length } >= 25
        if (!settled && allowOcr) {
            val o = ocr(page)
            if (o.isNotEmpty()) res = o
            settled = true
        }
        lineCache[page] = res
        if (settled) runCatching { f.writeText(encode(res)) }
        return res
    }

    private fun doc(): PDDocument {
        pd?.let { return it }
        val d = PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly().setTempDir(ctx.cacheDir))
        pd = d
        return d
    }

    private class Word(val text: String, val x0: Float, val x1: Float, val top: Float, val bottom: Float, val base: Float, val h: Float)

    private class LineStripper : PDFTextStripper() {
        val words = ArrayList<Word>()
        val breaks = HashSet<Int>()

        override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
            val tps = textPositions ?: return
            if (tps.isEmpty()) return
            var x0 = Float.MAX_VALUE
            var x1 = -Float.MAX_VALUE
            var base = 0f
            var h = 0f
            for (tp in tps) {
                x0 = min(x0, tp.x)
                x1 = max(x1, tp.x + tp.width)
                base = max(base, tp.y)
                h = max(h, max(tp.height, tp.fontSizeInPt * 0.7f))
            }
            if (h <= 0f) h = 8f
            words.add(Word(text ?: "", x0, x1, base - h * 1.1f, base + h * 0.3f, base, h))
        }

        override fun writeLineSeparator() {
            breaks.add(words.size)
        }
    }

    private fun extract(page: Int): List<LineBox> = try {
        val d = doc()
        val pg = d.getPage(page)
        val box = pg.cropBox
        val rot = ((pg.rotation % 360) + 360) % 360
        val pw = if (rot == 90 || rot == 270) box.height else box.width
        val ph = if (rot == 90 || rot == 270) box.width else box.height
        val st = LineStripper()
        st.sortByPosition = false
        st.startPage = page + 1
        st.endPage = page + 1
        st.getText(d)
        buildLines(st.words, st.breaks, pw, ph)
    } catch (e: Throwable) {
        emptyList()
    }

    private fun buildLines(words: List<Word>, breaks: Set<Int>, pw: Float, ph: Float): List<LineBox> {
        val out = ArrayList<LineBox>()
        var cur = ArrayList<Word>()
        fun flush() {
            if (cur.isEmpty()) return
            val text = cur.joinToString(" ") { it.text }.trim()
            if (text.isNotEmpty()) {
                out.add(
                    LineBox(
                        (cur.minOf { it.x0 } / pw).coerceIn(0f, 1f),
                        (cur.minOf { it.top } / ph).coerceIn(0f, 1f),
                        (cur.maxOf { it.x1 } / pw).coerceIn(0f, 1f),
                        (cur.maxOf { it.bottom } / ph).coerceIn(0f, 1f),
                        text,
                    )
                )
            }
            cur = ArrayList()
        }
        words.forEachIndexed { i, w ->
            if (cur.isNotEmpty()) {
                val last = cur.last()
                val hh = max(w.h, last.h)
                val newLine = i in breaks ||
                    abs(w.base - last.base) > hh * 0.5f ||
                    w.x0 < last.x1 - hh * 0.5f ||
                    (w.x0 - last.x1) > hh * 2.5f   // hueco grande: otra columna
                if (newLine) flush()
            }
            cur.add(w)
        }
        flush()
        return out
    }

    private suspend fun ocr(page: Int): List<LineBox> = ocrLock.withLock {
        val bmp = try { render(page, 1600) } catch (e: Exception) { return emptyList() }
        val result: Text? = suspendCancellableCoroutine { cont ->
            recognizer.process(InputImage.fromBitmap(bmp, 0))
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        result?.textBlocks?.flatMap { it.lines }?.mapNotNull { ln ->
            val bb = ln.boundingBox ?: return@mapNotNull null
            LineBox(bb.left / w, bb.top / h, bb.right / w, bb.bottom / h, ln.text)
        } ?: emptyList()
    }

    /** Texto de todas las páginas (para el modo de texto fluido). */
    suspend fun allPagesText(allowOcr: Boolean, progress: (Int, Int) -> Unit): List<String> {
        val out = ArrayList<String>(pageCount)
        for (p in 0 until pageCount) {
            progress(p, pageCount)
            val ls = lines(p, allowOcr)
            // Las líneas con letra bastante más grande que el resto son títulos
            val hs = ls.map { it.b - it.t }.sorted()
            val med = if (hs.isEmpty()) 0f else hs[hs.size / 2]
            out.add(ls.joinToString("\n") { l ->
                if (ls.size > 3 && med > 0f && (l.b - l.t) > med * 1.35f && l.text.length < 90) "# " + l.text else l.text
            })
        }
        return out
    }

    private fun encode(ls: List<LineBox>): String {
        val a = JSONArray()
        for (l in ls) a.put(JSONArray().put(l.l.toDouble()).put(l.t.toDouble()).put(l.r.toDouble()).put(l.b.toDouble()).put(l.text))
        return a.toString()
    }

    private fun decode(s: String): List<LineBox> {
        val a = JSONArray(s)
        return (0 until a.length()).map { i ->
            val o = a.getJSONArray(i)
            LineBox(o.getDouble(0).toFloat(), o.getDouble(1).toFloat(), o.getDouble(2).toFloat(), o.getDouble(3).toFloat(), o.getString(4))
        }
    }
}

/**
 * Pinta cada línea de la página con un degradado, sin tocar el formato.
 * Truco: con el modo "aclarar", el texto negro toma el color y el papel blanco queda igual.
 * En páginas oscuras se invierte la página y se usa "oscurecer".
 */
object Coloring {
    private val invert = ColorMatrixColorFilter(
        ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    )

    fun bake(src: Bitmap, lines: List<LineBox>, st: PageStyle): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val base = Paint(Paint.FILTER_BITMAP_FLAG)
        if (st.dark) base.colorFilter = invert
        c.drawBitmap(src, 0f, 0f, base)
        if (st.colors && lines.isNotEmpty() && st.palette.isNotEmpty()) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.xfermode = PorterDuffXfermode(if (st.dark) PorterDuff.Mode.DARKEN else PorterDuff.Mode.LIGHTEN)
            val w = out.width.toFloat()
            val h = out.height.toFloat()
            lines.forEachIndexed { i, ln ->
                val l = ln.l * w
                val r = ln.r * w
                val t = ln.t * h
                val b = ln.b * h
                if (r - l < 2f || b - t < 2f) return@forEachIndexed
                val acc = st.palette[(i / 2) % st.palette.size]
                val full = mix(st.ink, acc, st.intensity)
                val c0 = if (i % 2 == 0) st.ink else full
                val c1 = if (i % 2 == 0) full else st.ink
                p.shader = LinearGradient(l, 0f, r, 0f, c0, c1, Shader.TileMode.CLAMP)
                c.drawRect(l, t, r, b, p)
            }
        }
        return out
    }

    fun mix(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * k).roundToInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * k).roundToInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * k).roundToInt(),
        )
    }
}
