package com.hilo.lector.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.hilo.lector.text.Epub
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class Book(
    val id: String,
    val title: String,
    val author: String = "",
    val type: String,            // pdf, epub, txt
    val file: String,
    val added: Long,
    val updated: Long = 0,
    val progress: Float = 0f,
    val pos: Int = 0,            // página (PDF) o párrafo (texto)
    val posOffset: Int = 0,
    val flowPos: Int = 0,
    val flowOffset: Int = 0,
    val flowMode: Boolean = false,
    val done: Boolean = false,
    val pages: Int = 0,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("title", title).put("author", author).put("type", type).put("file", file)
        .put("added", added).put("updated", updated).put("progress", progress.toDouble())
        .put("pos", pos).put("posOffset", posOffset).put("flowPos", flowPos).put("flowOffset", flowOffset)
        .put("flowMode", flowMode).put("done", done).put("pages", pages)

    companion object {
        fun from(o: JSONObject) = Book(
            id = o.getString("id"),
            title = o.optString("title", "Sin título"),
            author = o.optString("author", ""),
            type = o.optString("type", "txt"),
            file = o.getString("file"),
            added = o.optLong("added", 0),
            updated = o.optLong("updated", 0),
            progress = o.optDouble("progress", 0.0).toFloat(),
            pos = o.optInt("pos", 0),
            posOffset = o.optInt("posOffset", 0),
            flowPos = o.optInt("flowPos", 0),
            flowOffset = o.optInt("flowOffset", 0),
            flowMode = o.optBoolean("flowMode", false),
            done = o.optBoolean("done", false),
            pages = o.optInt("pages", 0),
        )
    }
}

class LibraryRepo(private val ctx: Context) {
    private val dir = File(ctx.filesDir, "books").apply { mkdirs() }
    private val coverDir = File(ctx.filesDir, "covers").apply { mkdirs() }
    private val index = File(ctx.filesDir, "library.json")
    private val _books = MutableStateFlow(load())
    val books: StateFlow<List<Book>> = _books
    private var counter = 0

    fun fileOf(b: Book) = File(dir, b.file)
    fun coverOf(b: Book) = File(coverDir, b.id + ".jpg")
    fun get(id: String) = _books.value.firstOrNull { it.id == id }

    fun update(id: String, f: (Book) -> Book) {
        _books.value = _books.value.map { if (it.id == id) f(it) else it }
        save()
    }

    fun remove(id: String) {
        val b = get(id) ?: return
        fileOf(b).delete()
        coverOf(b).delete()
        File(ctx.filesDir, "annot/$id.json").delete()
        File(ctx.filesDir, "lines/$id").deleteRecursively()
        File(ctx.filesDir, "flow/$id.txt").delete()
        _books.value = _books.value.filter { it.id != id }
        save()
    }

    private fun newId() = "b" + System.currentTimeMillis() + "_" + (counter++)

    private fun add(b: Book): Book {
        _books.value = listOf(b) + _books.value
        save()
        return b
    }

    private fun load(): List<Book> = try {
        if (!index.exists()) emptyList() else {
            val arr = JSONArray(index.readText())
            (0 until arr.length()).map { Book.from(arr.getJSONObject(it)) }
        }
    } catch (e: Exception) {
        emptyList()
    }

    @Synchronized
    private fun save() {
        val arr = JSONArray()
        _books.value.forEach { arr.put(it.toJson()) }
        val tmp = File(index.path + ".tmp")
        tmp.writeText(arr.toString())
        if (!tmp.renameTo(index)) {
            index.writeText(arr.toString()); tmp.delete()
        }
    }

    // ---------- importar ----------

    private fun nameOf(uri: Uri): String {
        try {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.let { return it }
            }
        } catch (_: Exception) {}
        return uri.lastPathSegment?.substringAfterLast('/') ?: "archivo"
    }

    private fun kindOf(uri: Uri, name: String): String {
        val mime = ctx.contentResolver.getType(uri) ?: ""
        val n = name.lowercase()
        return when {
            mime.startsWith("image/") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") ||
                n.endsWith(".webp") || n.endsWith(".heic") -> "img"
            mime == "application/pdf" || n.endsWith(".pdf") -> "pdf"
            mime == "application/epub+zip" || n.endsWith(".epub") -> "epub"
            else -> "txt"
        }
    }

    /** Copia los archivos a la app. Varias fotos juntas se convierten en un solo PDF. */
    suspend fun importUris(uris: List<Uri>): List<Book> = withContext(Dispatchers.IO) {
        val items = uris.map { u -> val n = nameOf(u); Triple(u, n, kindOf(u, n)) }
        val out = ArrayList<Book>()
        val imgs = items.filter { it.third == "img" }
        if (imgs.isNotEmpty()) out.add(imagesToPdf(imgs.map { it.first }))
        for ((uri, name, kind) in items) {
            if (kind == "img") continue
            val id = newId()
            val ext = if (kind == "txt") "txt" else kind
            val f = File(dir, "$id.$ext")
            ctx.contentResolver.openInputStream(uri)?.use { inp -> f.outputStream().use { inp.copyTo(it) } }
                ?: error("No pude abrir $name")
            val baseTitle = name.substringBeforeLast('.').ifBlank { "Sin título" }
            val book = when (kind) {
                "pdf" -> {
                    val pages = pdfInfo(f, coverFile(id))
                    Book(id, baseTitle, type = "pdf", file = f.name, added = now(), pages = pages)
                }
                "epub" -> {
                    val m = runCatching { Epub.meta(f) }.getOrNull()
                    m?.cover?.let { saveCover(it, coverFile(id)) }
                    Book(id, m?.title?.ifBlank { null } ?: baseTitle, m?.author.orEmpty(), "epub", f.name, now())
                }
                else -> Book(id, baseTitle, type = "txt", file = f.name, added = now())
            }
            out.add(add(book))
        }
        out
    }

    suspend fun addText(title: String, text: String): Book = withContext(Dispatchers.IO) {
        val id = newId()
        val f = File(dir, "$id.txt")
        f.writeText(text)
        add(Book(id, title, type = "txt", file = f.name, added = now()))
    }

    private fun now() = System.currentTimeMillis()
    private fun coverFile(id: String) = File(coverDir, "$id.jpg")

    /** Cuenta páginas y guarda la primera como portada. */
    private fun pdfInfo(f: File, cover: File): Int {
        ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { r ->
                if (r.pageCount > 0) {
                    r.openPage(0).use { p ->
                        val w = 300
                        val h = max(1, (w * p.height.toFloat() / p.width).roundToInt())
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        cover.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                    }
                }
                return r.pageCount
            }
        }
    }

    private fun saveCover(bytes: ByteArray, out: File) {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var sample = 1
        while (o.outWidth / (sample * 2) >= 300) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    }

    private fun decodeScaled(uri: Uri, maxSide: Int): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { dec, info, _ ->
                val big = max(info.size.width, info.size.height)
                var s = 1
                while (big / (s * 2) >= maxSide) s *= 2
                dec.setTargetSampleSize(s)
                dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
            var s = 1
            while (max(o.outWidth, o.outHeight) / (s * 2) >= maxSide) s *= 2
            ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s })
            }
        }
    } catch (e: Exception) {
        null
    }

    /** Une fotos de páginas, en el orden elegido, en un PDF. Luego Hilo reconoce su texto. */
    private fun imagesToPdf(uris: List<Uri>): Book {
        val id = newId()
        val f = File(dir, "$id.pdf")
        val doc = PdfDocument()
        var n = 0
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        for (u in uris) {
            val bmp = decodeScaled(u, 2200) ?: continue
            val pw = 595
            val ph = max(1, (595f * bmp.height / bmp.width).roundToInt())
            n++
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, n).create())
            page.canvas.drawColor(Color.WHITE)
            page.canvas.drawBitmap(bmp, null, Rect(0, 0, pw, ph), paint)
            doc.finishPage(page)
            bmp.recycle()
        }
        if (n == 0) { doc.close(); error("No pude abrir las fotos") }
        FileOutputStream(f).use { doc.writeTo(it) }
        doc.close()
        pdfInfo(f, coverFile(id))
        val title = "Fotos · " + SimpleDateFormat("d MMM, HH:mm", Locale("es")).format(Date())
        return add(Book(id, title, type = "pdf", file = f.name, added = now(), pages = n))
    }
}
