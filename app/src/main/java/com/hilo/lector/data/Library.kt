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
import androidx.documentfile.provider.DocumentFile
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.core.content.FileProvider
import com.hilo.lector.text.Formats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/** Un marcador: página (PDF) o párrafo (texto). */
data class Bookmark(val flow: Boolean, val pos: Int, val label: String, val time: Long)

data class Book(
    val id: String,
    val title: String,
    val author: String = "",
    val type: String,            // pdf, epub, txt, md, html, fb2, docx, odt
    val file: String,
    val added: Long,
    val updated: Long = 0,
    val progress: Float = 0f,
    val pos: Int = 0,            // página (PDF)
    val posOffset: Int = 0,
    val flowPos: Int = 0,        // párrafo (texto)
    val flowOffset: Int = 0,
    val flowMode: Boolean = false,
    val done: Boolean = false,
    val pages: Int = 0,
    val favorite: Boolean = false,
    val toRead: Boolean = false,
    val trashed: Boolean = false,
    val collections: List<String> = emptyList(),
    val srcKey: String = "",     // nombre|tamaño del archivo original, para no repetirlo
    val bookmarks: List<Bookmark> = emptyList(),
    /** Encontrado al revisar el celular y todavía no abierto. */
    val isNew: Boolean = false,
) {
    /** Documento que se lee desde su lugar en el celular, sin copiarlo. */
    val linked: Boolean get() = file.startsWith("/")

    fun toJson(): JSONObject {
        val bm = JSONArray()
        bookmarks.forEach { bm.put(JSONObject().put("flow", it.flow).put("pos", it.pos).put("label", it.label).put("time", it.time)) }
        return JSONObject()
            .put("id", id).put("title", title).put("author", author).put("type", type).put("file", file)
            .put("added", added).put("updated", updated).put("progress", progress.toDouble())
            .put("pos", pos).put("posOffset", posOffset).put("flowPos", flowPos).put("flowOffset", flowOffset)
            .put("flowMode", flowMode).put("done", done).put("pages", pages)
            .put("favorite", favorite).put("toRead", toRead).put("trashed", trashed)
            .put("collections", JSONArray(collections)).put("srcKey", srcKey).put("bookmarks", bm)
            .put("isNew", isNew)
    }

    companion object {
        fun from(o: JSONObject): Book {
            val cols = o.optJSONArray("collections")
            val bms = o.optJSONArray("bookmarks")
            return Book(
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
                favorite = o.optBoolean("favorite", false),
                toRead = o.optBoolean("toRead", false),
                trashed = o.optBoolean("trashed", false),
                collections = if (cols == null) emptyList() else (0 until cols.length()).map { cols.getString(it) },
                srcKey = o.optString("srcKey", ""),
                bookmarks = if (bms == null) emptyList() else (0 until bms.length()).map {
                    val b = bms.getJSONObject(it)
                    Bookmark(b.optBoolean("flow"), b.optInt("pos"), b.optString("label"), b.optLong("time"))
                },
                isNew = o.optBoolean("isNew", false),
            )
        }
    }
}

class LibraryRepo(private val ctx: Context) {
    private val dir = File(ctx.filesDir, "books").apply { mkdirs() }
    private val coverDir = File(ctx.filesDir, "covers").apply { mkdirs() }
    private val index = File(ctx.filesDir, "library.json")
    private val _books = MutableStateFlow(load())
    val books: StateFlow<List<Book>> = _books
    private var counter = 0

    fun fileOf(b: Book) = if (b.linked) File(b.file) else File(dir, b.file)
    fun coverOf(b: Book) = File(coverDir, b.id + ".jpg")
    fun get(id: String) = _books.value.firstOrNull { it.id == id }

    fun update(id: String, f: (Book) -> Book) {
        _books.value = _books.value.map { if (it.id == id) f(it) else it }
        save()
    }

    fun remove(id: String) {
        val b = get(id) ?: return
        if (!b.linked) fileOf(b).delete()   // los documentos del celular no se borran, solo salen de Hilo
        else ignoreSp.edit().putStringSet("keys", ignoreSp.getStringSet("keys", emptySet()).orEmpty() + b.srcKey).apply()
        coverOf(b).delete()
        File(ctx.filesDir, "annot/$id.json").delete()
        File(ctx.filesDir, "lines/$id").deleteRecursively()
        File(ctx.filesDir, "flow/$id.txt").delete()
        _books.value = _books.value.filter { it.id != id }
        save()
    }

    fun emptyTrash() { _books.value.filter { it.trashed }.forEach { remove(it.id) } }

    /** Enlace para compartir el archivo con otras apps. */
    fun shareUri(b: Book): Uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", fileOf(b))

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

    private var saveJob: kotlinx.coroutines.Job? = null

    /** Guarda la biblioteca en segundo plano (agrupa cambios seguidos, como el avance al leer). */
    @Synchronized
    private fun save() {
        val snapshot = _books.value
        saveJob?.cancel()
        saveJob = com.hilo.lector.Hilo.scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(250)
            val arr = JSONArray()
            snapshot.forEach { arr.put(it.toJson()) }
            val text = arr.toString()
            synchronized(index) {
                val tmp = File(index.path + ".tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(index)) { index.writeText(text); tmp.delete() }
            }
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

    private fun kindOf(uri: Uri, name: String): String? =
        Formats.typeOfName(name) ?: Formats.typeOfMime(ctx.contentResolver.getType(uri) ?: "")

    private fun sizeOf(uri: Uri): Long = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L
    } catch (e: Exception) {
        -1L
    }

    /** Copia los archivos a la app. Varias fotos juntas se convierten en un solo PDF. */
    suspend fun importUris(uris: List<Uri>, progress: (Int, Int) -> Unit = { _, _ -> }): List<Book> = withContext(Dispatchers.IO) {
        val items = uris.map { u -> val n = nameOf(u); Triple(u, n, kindOf(u, n)) }
        val out = ArrayList<Book>()
        val imgs = items.filter { it.third == "img" }
        if (imgs.isNotEmpty()) out.add(imagesToPdf(imgs.map { it.first }))
        val docs = items.filter { it.third != null && it.third != "img" }
        docs.forEachIndexed { i, (uri, name, kind) ->
            progress(i, docs.size)
            runCatching { importOne(uri, name, kind!!, "") }.getOrNull()?.let { out.add(it) }
        }
        if (out.isEmpty() && items.isNotEmpty() && items.all { it.third == null }) error("Formato no compatible")
        out
    }

    private fun importOne(uri: Uri, name: String, kind: String, srcKey: String): Book {
        val id = newId()
        val f = File(dir, "$id.$kind")
        ctx.contentResolver.openInputStream(uri)?.use { inp -> f.outputStream().use { inp.copyTo(it) } }
            ?: error("No pude abrir $name")
        val key = srcKey.ifEmpty { name + "|" + f.length() }
        val baseTitle = name.substringBeforeLast('.').replace('_', ' ').ifBlank { "Sin título" }
        val book = if (kind == "pdf") {
            val pages = pdfInfo(f, coverFile(id))
            Book(id, baseTitle, type = "pdf", file = f.name, added = now(), pages = pages, srcKey = key)
        } else {
            val m = Formats.meta(kind, f)
            m?.cover?.let { runCatching { saveCover(it, coverFile(id)) } }
            Book(
                id, m?.title?.ifBlank { null } ?: baseTitle, m?.author.orEmpty(), kind, f.name, now(), srcKey = key,
            )
        }
        return add(book)
    }

    // ---------- documentos del celular (como ReadEra) ----------

    /** Documentos del celular que quitaste de Hilo: no se vuelven a agregar solos. */
    private val ignoreSp = ctx.getSharedPreferences("ignorados", Context.MODE_PRIVATE)

    fun hasDeviceAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private val scanLock = Mutex()

    /**
     * Busca PDF, EPUB, Word y otros documentos en todo el celular y los agrega sin copiarlos.
     * Devuelve cuántos documentos nuevos encontró.
     */
    suspend fun scanDevice(): Int = scanLock.withLock {
        withContext(Dispatchers.IO) {
            if (!hasDeviceAccess()) return@withContext 0
            val known = HashSet<String>()
            _books.value.forEach { known.add(it.srcKey); known.add(it.file) }
            known.addAll(ignoreSp.getStringSet("keys", emptySet()).orEmpty())
            val exts = listOf("pdf", "epub", "mobi", "azw3", "azw", "docx", "odt", "fb2", "md", "markdown", "txt", "html", "htm")
            val col = MediaStore.Files.FileColumns.DATA
            val sel = exts.joinToString(" OR ") { "$col LIKE ?" }
            val args = exts.map { "%.$it" }.toTypedArray()
            val own = ctx.filesDir.parentFile?.path ?: ctx.filesDir.path
            val excluded = excludedFolders()
            // La primera búsqueda no marca todo como «Nuevo» (serían cientos): solo lo que aparezca después
            val firstScan = !ignoreSp.getBoolean("buscado", false)
            val found = ArrayList<Book>()
            try {
                ctx.contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    arrayOf(col, MediaStore.Files.FileColumns.SIZE), sel, args, null,
                )?.use { c ->
                    while (c.moveToNext()) {
                        val path = c.getString(0) ?: continue
                        val size = c.getLong(1)
                        if (path.contains("/Android/data/") || path.contains("/Android/obb/") || path.startsWith(own)) continue
                        if (path.split('/').any { it.startsWith(".") }) continue
                        if (excluded.any { path.startsWith("$it/") }) continue
                        val name = path.substringAfterLast('/')
                        val kind = Formats.typeOfName(name) ?: continue
                        if (kind == "img") continue
                        if ((kind == "txt" || kind == "html") && size < 2048) continue
                        val key = "dev|$path"
                        if (key in known || path in known) continue
                        val f = File(path)
                        if (!f.canRead() || f.length() == 0L) continue
                        known.add(key)
                        found.add(
                            Book(
                                newId(), cleanTitle(name), type = kind, file = path,
                                added = now(), srcKey = key, isNew = !firstScan,
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                return@withContext 0
            }
            ignoreSp.edit().putBoolean("buscado", true).apply()
            if (found.isEmpty()) return@withContext 0
            _books.value = found + _books.value
            save()
            // Portadas, páginas, títulos y autores, de a poco
            for (b in found) {
                runCatching {
                    val f = File(b.file)
                    if (b.type == "pdf") {
                        val pages = pdfInfo(f, coverFile(b.id))
                        // Si el nombre del archivo es un número o un código, se usa el título guardado dentro del PDF
                        val meta = if (badTitle(b.title)) pdfMeta(f) else null
                        update(b.id) { it.copy(pages = pages, title = meta?.first ?: it.title, author = it.author.ifBlank { meta?.second.orEmpty() }) }
                    } else {
                        val m = Formats.meta(b.type, f)
                        m?.cover?.let { runCatching { saveCover(it, coverFile(b.id)) } }
                        if (m != null && (m.title.isNotBlank() || m.author.isNotBlank())) {
                            update(b.id) { it.copy(title = m.title.ifBlank { it.title }, author = m.author.ifBlank { it.author }) }
                        }
                    }
                }
            }
            found.size
        }
    }

    // ---------- carpetas que no se leen ----------

    /** Carpetas del celular que el usuario pidió no revisar (rutas completas). */
    fun excludedFolders(): Set<String> = ignoreSp.getStringSet("carpetas", emptySet()).orEmpty()

    /** Carpetas donde Hilo encontró documentos, con cuántos hay en cada una. */
    fun deviceFolders(): List<Pair<String, Int>> =
        _books.value.filter { it.linked && it.file.startsWith("/") }.groupBy { it.file.substringBeforeLast('/') }
            .map { it.key to it.value.size }.sortedByDescending { it.second }

    /** Deja de leer una carpeta (y sus subcarpetas): sus documentos salen de Hilo; los archivos siguen en el celular. */
    fun excludeFolder(path: String): Int {
        ignoreSp.edit().putStringSet("carpetas", excludedFolders() + path).apply()
        val out = _books.value.filter { it.linked && (it.file.startsWith("$path/")) }
        if (out.isEmpty()) return 0
        val ids = out.map { it.id }.toSet()
        out.forEach { b -> coverOf(b).delete(); File(ctx.filesDir, "flow/${b.id}.txt").delete() }
        _books.value = _books.value.filter { it.id !in ids }
        save()
        return out.size
    }

    fun includeFolder(path: String) {
        ignoreSp.edit().putStringSet("carpetas", excludedFolders() - path).apply()
    }

    /** Quita la etiqueta «Nuevo» de todos. */
    fun clearNew() {
        if (_books.value.none { it.isNew }) return
        _books.value = _books.value.map { if (it.isNew) it.copy(isNew = false) else it }
        save()
    }

    /** Arregla los títulos que son solo números o códigos, con el título guardado dentro del PDF. */
    suspend fun fixTitles() = withContext(Dispatchers.IO) {
        for (b in _books.value.filter { it.type == "pdf" && badTitle(it.title) }) {
            val m = runCatching { pdfMeta(fileOf(b)) }.getOrNull() ?: continue
            update(b.id) { it.copy(title = m.first, author = it.author.ifBlank { m.second }) }
        }
    }

    // ---------- carpetas vigiladas ----------

    private val folderSp = ctx.getSharedPreferences("carpetas", Context.MODE_PRIVATE)

    fun folders(): List<Uri> = folderSp.getStringSet("uris", emptySet()).orEmpty().map { Uri.parse(it) }

    fun addFolder(uri: Uri) {
        runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        folderSp.edit().putStringSet("uris", folderSp.getStringSet("uris", emptySet()).orEmpty() + uri.toString()).apply()
    }

    fun removeFolder(uri: Uri) {
        runCatching { ctx.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        folderSp.edit().putStringSet("uris", folderSp.getStringSet("uris", emptySet()).orEmpty() - uri.toString()).apply()
    }

    fun folderName(uri: Uri): String =
        runCatching { DocumentFile.fromTreeUri(ctx, uri)?.name }.getOrNull() ?: uri.lastPathSegment.orEmpty()

    /** Busca documentos nuevos en las carpetas elegidas y los agrega a la biblioteca. */
    suspend fun scanFolders(only: Uri? = null, progress: (Int, Int) -> Unit = { _, _ -> }): Int = withContext(Dispatchers.IO) {
        val known = _books.value.map { it.srcKey }.toHashSet()
        val found = ArrayList<Triple<Uri, String, String>>()
        fun walk(d: DocumentFile, depth: Int) {
            if (depth > 6 || found.size > 2000) return
            for (f in d.listFiles()) {
                if (f.isDirectory) {
                    if (!(f.name ?: "").startsWith(".")) walk(f, depth + 1)
                    continue
                }
                val name = f.name ?: continue
                val kind = Formats.typeOfName(name) ?: continue
                if (kind == "img") continue
                val key = name + "|" + f.length()
                if (key in known) continue
                known.add(key)
                found.add(Triple(f.uri, name + "\u0000" + key, kind))
            }
        }
        for (u in (if (only != null) listOf(only) else folders())) {
            runCatching { DocumentFile.fromTreeUri(ctx, u)?.let { walk(it, 0) } }
        }
        var n = 0
        found.forEachIndexed { i, (uri, name, kind) ->
            progress(i, found.size)
            val (realName, key) = name.split("\u0000", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            if (runCatching { importOne(uri, realName, kind, key) }.isSuccess) n++
        }
        n
    }

    private fun sizeOrZero(uri: Uri): Long = sizeOf(uri).let { if (it < 0) 0 else it }

    suspend fun addText(title: String, text: String): Book = withContext(Dispatchers.IO) {
        val id = newId()
        val f = File(dir, "$id.txt")
        f.writeText(text)
        add(Book(id, title, type = "txt", file = f.name, added = now(), srcKey = "pegado|$id"))
    }

    private fun now() = System.currentTimeMillis()
    private fun coverFile(id: String) = File(coverDir, "$id.jpg")

    /** Cuenta páginas y guarda la primera como portada. */
    /** Título y autor guardados dentro del PDF, si son útiles. */
    private fun pdfMeta(f: File): Pair<String, String>? = try {
        com.tom_roush.pdfbox.pdmodel.PDDocument.load(f, com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly()).use { d ->
            val info = d.documentInformation
            val t = info?.title?.trim().orEmpty()
            val a = info?.author?.trim().orEmpty()
            if (t.length >= 3 && !badTitle(t) && !t.lowercase().let { it.startsWith("microsoft") || it.endsWith(".doc") || it.endsWith(".docx") || it == "untitled" || it == "sin título" }) t to a else null
        }
    } catch (_: Throwable) { null }

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

/** Título a partir del nombre del archivo: sin extensión, sin guiones bajos ni espacios de más. */
fun cleanTitle(fileName: String): String =
    fileName.substringBeforeLast('.').replace('_', ' ').replace(Regex("\\s+"), " ").trim()
        .removeSuffix(" (1)").removeSuffix(" (2)").ifBlank { fileName }

/** Un título «malo»: casi todo números o códigos (como «4 57714757040215178 40»). */
fun badTitle(t: String): Boolean {
    val letters = t.count { it.isLetter() }
    val digits = t.count { it.isDigit() }
    return letters < 3 || digits > letters || Regex("^[0-9a-fA-F-]{16,}$").matches(t.replace(" ", ""))
}
