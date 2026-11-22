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
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.hilo.lector.text.TextTools
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
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
    /** Papel oscuro y letra clara (noche, crepúsculo, consola). */
    val dark: Boolean,
    val palette: List<Int>,
    val ink: Int,
    val intensity: Float,
    /** Color del papel; con blanco y tinta casi negra la página no se toca. */
    val paper: Int = Color.WHITE,
    val identity: Boolean = true,
) {
    val key: String = "$colors|$dark|${palette.joinToString(",")}|$ink|$intensity|$paper|$identity"
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
    private val lineDir = File(ctx.filesDir, "lines/$bookId")   // se crea al guardar (no en la pantalla)
    private var rec: TextRecognizer? = null
    private val recognizer: TextRecognizer
        get() = rec ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).also { rec = it }

    var pageCount = 0
        private set
    /** alto / ancho de cada página */
    var aspects = FloatArray(0)
        private set

    // Páginas dibujadas en memoria: hasta 1/8 de lo que Android le da a Hilo y nunca más de 64 MB
    // (con más, un PDF grande subía a cientos de MB y Android cerraba Hilo apenas pasaba a segundo plano)
    val bitmaps = object : LruCache<String, Bitmap>(minOf(Runtime.getRuntime().maxMemory() / 8, 64L shl 20).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (oldValue !== newValue) recycleLater(oldValue)
        }
    }

    // La memoria de cada página dibujada está fuera de Java: Android la devuelve recién cuando limpia la memoria
    // de Java, que en un PDF casi no se usa. Sin devolverla a mano, la prueba de estrés llegó a 645 MB.
    // Se devuelve cuando la página ya no está en memoria ni en pantalla (con un segundo de margen).
    private val inUse = HashMap<Bitmap, Int>()
    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    /** Una página en pantalla (o en uso) avisa que la está usando, para no liberarla. */
    fun acquire(b: Bitmap) { synchronized(inUse) { inUse[b] = (inUse[b] ?: 0) + 1 } }
    fun release(b: Bitmap) {
        synchronized(inUse) { val n = (inUse[b] ?: 1) - 1; if (n <= 0) inUse.remove(b) else inUse[b] = n }
        recycleLater(b)
    }

    private fun recycleLater(b: Bitmap) {
        main.postDelayed({
            val busy = synchronized(inUse) { inUse.containsKey(b) }
            if (!busy && !b.isRecycled && !bitmaps.snapshot().values.any { it === b }) b.recycle()
        }, 1000)
    }

    init { synchronized(live) { live.add(this) } }

    companion object {
        private val live = java.util.Collections.newSetFromMap(java.util.WeakHashMap<PdfEngine, Boolean>())
        /** Al salir de Hilo: suelta las páginas dibujadas (se vuelven a dibujar al volver). */
        fun trimAll() { synchronized(live) { live.toList() }.forEach { it.bitmaps.evictAll() } }
    }

    suspend fun open() = withContext(renderThread) {
        val p = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pfd = p
        val r = PdfRenderer(p)
        renderer = r
        pageCount = r.pageCount
        // Abrir cada página para medirla tarda segundos en un PDF de cientos de páginas: se guarda la medida
        val saved = File(lineDir, "aspectos.txt")
        val stamp = "${file.length()}|${file.lastModified()}|$pageCount"
        val cached = runCatching {
            if (!saved.exists()) null else saved.readText().split('\n', limit = 2).takeIf { it.size == 2 && it[0] == stamp }
                ?.get(1)?.split(',')?.map { it.toFloat() }?.takeIf { it.size == pageCount }?.toFloatArray()
        }.getOrNull()
        aspects = cached ?: FloatArray(pageCount) { i -> r.openPage(i).use { pg -> pg.height.toFloat() / max(1, pg.width) } }.also { a ->
            runCatching { lineDir.mkdirs(); saved.writeText(stamp + "\n" + a.joinToString(",")) }
        }
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
        synchronized(live) { live.remove(this) }
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

    // ---------- recorte de márgenes ----------

    /** Recorte de cada página (izquierda, arriba, derecha, abajo en fracciones de 0 a 1). Se calcula al verla. */
    val crops = androidx.compose.runtime.mutableStateMapOf<Int, CropBox>()

    /** Busca el borde del contenido en una miniatura: lo que no es blanco es texto o imagen. */
    suspend fun crop(page: Int): CropBox {
        crops[page]?.let { return it }
        val box = withContext(renderThread) {
            val r = renderer ?: return@withContext CropBox.FULL
            r.openPage(page).use { p ->
                val w = 220
                val h = max(1, (w * p.height.toFloat() / max(1, p.width)).roundToInt())
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val px = IntArray(w * h)
                bmp.getPixels(px, 0, w, 0, 0, w, h)
                bmp.recycle()
                fun ink(v: Int): Boolean {
                    val lum = (Color.red(v) * 299 + Color.green(v) * 587 + Color.blue(v) * 114) / 1000
                    return lum < 232
                }
                // Filas y columnas con algo de tinta (ignora motas sueltas)
                val rowInk = IntArray(h); val colInk = IntArray(w)
                for (y in 0 until h) for (x in 0 until w) if (ink(px[y * w + x])) { rowInk[y]++; colInk[x]++ }
                val minRow = max(1, w / 150); val minCol = max(1, h / 150)
                val top = rowInk.indexOfFirst { it >= minRow }
                val bottom = rowInk.indexOfLast { it >= minRow }
                val left = colInk.indexOfFirst { it >= minCol }
                val right = colInk.indexOfLast { it >= minCol }
                if (top < 0 || left < 0 || bottom <= top || right <= left) return@use CropBox.FULL
                val padX = 0.025f; val padY = 0.02f
                val b = CropBox(
                    (left.toFloat() / w - padX).coerceAtLeast(0f), (top.toFloat() / h - padY).coerceAtLeast(0f),
                    ((right + 1f) / w + padX).coerceAtMost(1f), ((bottom + 1f) / h + padY).coerceAtMost(1f),
                )
                // Si casi no hay margen, o el contenido es diminuto (página casi vacía), no se recorta
                if (b.width > 0.94f && b.height > 0.94f) CropBox.FULL
                else if (b.width < 0.3f || b.height < 0.2f) CropBox.FULL
                else b
            }
        }
        crops[page] = box
        return box
    }

    /** Alto / ancho de la página tal como se ve (con o sin recorte). */
    fun shownAspect(page: Int, cropOn: Boolean): Float {
        val a = aspects.getOrElse(page) { 1.41f }.coerceAtLeast(0.05f)
        val c = if (cropOn) crops[page] ?: return a else return a
        return a * c.height / c.width
    }

    fun rawKey(page: Int, w: Int) = "r$page@$w"
    fun styledKey(page: Int, w: Int, st: PageStyle) = "s$page@$w#${st.key}"

    // ---------- texto ----------

    fun peekLines(page: Int): List<LineBox>? = lineCache[page]

    suspend fun lines(page: Int, allowOcr: Boolean): List<LineBox> {
        lineCache[page]?.let { return it }
        val f = File(lineDir, "$page.json")
        // Leer y escribir el archivo fuera de la pantalla (el modo estricto lo marcaba)
        val cached = withContext(Dispatchers.IO) { if (f.exists()) runCatching { decode(f.readText()) }.getOrNull() else null }
        if (cached != null) { lineCache[page] = cached; return cached }
        var res = withContext(textThread) { extract(page) }
        var settled = res.sumOf { it.text.length } >= 25
        if (!settled && allowOcr) {
            val o = ocr(page)
            if (o.isNotEmpty()) res = o
            settled = true
        }
        // Una página escaneada que todavía no se reconoció no se guarda: así se reconoce cuando haga falta
        if (settled) {
            lineCache[page] = res
            val enc = encode(res)
            withContext(Dispatchers.IO) { runCatching { f.parentFile?.mkdirs(); f.writeText(enc) } }
        }
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

    /** Índice del PDF (marcadores del documento): título y página. */
    suspend fun outline(): List<Pair<String, Int>> = withContext(textThread) {
        val out = ArrayList<Pair<String, Int>>()
        try {
            val d = doc()
            val root = d.documentCatalog.documentOutline ?: return@withContext out
            fun walk(first: PDOutlineItem?, depth: Int) {
                var item = first
                while (item != null && out.size < 600) {
                    val pg = try { item.findDestinationPage(d) } catch (e: Exception) { null }
                    val idx = if (pg != null) d.pages.indexOf(pg) else -1
                    val title = item.title?.trim().orEmpty()
                    if (idx >= 0 && title.isNotEmpty()) out.add(("    ".repeat(depth.coerceAtMost(3)) + title) to idx)
                    walk(item.firstChild, depth + 1)
                    item = item.nextSibling
                }
            }
            walk(root.firstChild, 0)
        } catch (e: Throwable) {
        }
        out
    }

    /** Texto de una página en párrafos, para leer en voz alta. */
    suspend fun pageText(page: Int, allowOcr: Boolean): String =
        TextTools.reflow(lines(page, allowOcr).joinToString("\n") { it.text }).joinToString("\n") { it.removePrefix("# ") }

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
    /** Lleva el blanco al color del papel y el negro al de la tinta (sepia, noche, consola…). */
    private fun recolor(paper: Int, ink: Int): ColorMatrixColorFilter {
        val pr = Color.red(paper).toFloat(); val pg = Color.green(paper).toFloat(); val pb = Color.blue(paper).toFloat()
        val ir = Color.red(ink).toFloat(); val ig = Color.green(ink).toFloat(); val ib = Color.blue(ink).toFloat()
        return ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    (pr - ir) / 255f, 0f, 0f, 0f, ir,
                    0f, (pg - ig) / 255f, 0f, 0f, ig,
                    0f, 0f, (pb - ib) / 255f, 0f, ib,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        )
    }

    fun bake(src: Bitmap, lines: List<LineBox>, st: PageStyle): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val base = Paint(Paint.FILTER_BITMAP_FLAG)
        if (!st.identity) base.colorFilter = recolor(st.paper, st.ink)
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

/** Zona de la página que tiene contenido, en fracciones (0 a 1). */
data class CropBox(val l: Float, val t: Float, val r: Float, val b: Float) {
    val width get() = r - l
    val height get() = b - t
    companion object { val FULL = CropBox(0f, 0f, 1f, 1f) }
}
