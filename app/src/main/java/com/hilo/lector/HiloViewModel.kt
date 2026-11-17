package com.hilo.lector

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hilo.lector.data.Book
import com.hilo.lector.data.LibraryRepo
import com.hilo.lector.data.SettingsStore
import com.hilo.lector.pdf.InkStroke
import com.hilo.lector.pdf.PdfEngine
import com.hilo.lector.pdf.PdfExport
import com.hilo.lector.pdf.PdfNote
import com.hilo.lector.text.Formats
import com.hilo.lector.data.Bookmark
import com.hilo.lector.text.TextTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface Screen {
    data object Library : Screen
    data class Reader(val id: String) : Screen
    data object Guide : Screen
}

class HiloViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication()
    val repo = LibraryRepo(app)
    val settings = SettingsStore(app)
    val timer = FocusTimer(app, viewModelScope)

    var screen by mutableStateOf<Screen>(Screen.Library)
    /** Mensaje de trabajo en curso ("Abriendo…"); null si no hay nada. */
    var busy by mutableStateOf<String?>(null)
    var toast by mutableStateOf<String?>(null)
    var sheet by mutableStateOf<String?>(null)   // "settings" | "timer"
    var pasteDraft by mutableStateOf<String?>(null)

    fun open(b: Book) {
        settings.lastBook = b.id
        screen = Screen.Reader(b.id)
    }

    private var started = false

    /** Al abrir la app: busca documentos nuevos en las carpetas y abre el último libro si así lo pediste. */
    fun onStart() {
        if (started) return
        started = true
        if (settings.value.openLast) {
            settings.lastBook?.let { id -> repo.get(id)?.takeIf { !it.trashed }?.let { screen = Screen.Reader(it.id) } }
        }
        if (repo.folders().isNotEmpty()) {
            viewModelScope.launch {
                val n = runCatching { repo.scanFolders() }.getOrDefault(0)
                if (n > 0) toast = if (n == 1) "1 documento nuevo en tus carpetas." else "$n documentos nuevos en tus carpetas."
            }
        }
    }

    fun addFolder(uri: Uri) {
        repo.addFolder(uri)
        viewModelScope.launch {
            busy = "Buscando documentos…"
            val n = runCatching { repo.scanFolders(uri) { i, t -> busy = "Agregando ${i + 1} de $t…" } }.getOrDefault(0)
            busy = null
            toast = when (n) {
                0 -> "No encontré documentos nuevos en esa carpeta."
                1 -> "Agregué 1 documento."
                else -> "Agregué $n documentos."
            }
        }
    }

    /** Pone o quita un marcador en la posición actual. */
    fun toggleBookmark(book: Book, flow: Boolean, pos: Int, label: String) {
        val has = book.bookmarks.any { it.flow == flow && it.pos == pos }
        repo.update(book.id) { b ->
            b.copy(
                bookmarks = if (has) b.bookmarks.filterNot { it.flow == flow && it.pos == pos }
                else (b.bookmarks + Bookmark(flow, pos, label, System.currentTimeMillis())).sortedBy { it.pos },
            )
        }
        toast = if (has) "Marcador quitado." else "Marcador agregado."
    }
    fun closeReader() { screen = Screen.Library }

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            busy = if (uris.size > 1) "Agregando ${uris.size} archivos…" else "Agregando…"
            val r = runCatching { repo.importUris(uris) { i, t -> if (t > 1) busy = "Agregando ${i + 1} de $t…" } }
            busy = null
            r.onSuccess { list ->
                when {
                    list.isEmpty() -> toast = "No encontré nada que abrir."
                    list.size == 1 -> open(list[0])
                    else -> toast = "Agregué ${list.size} libros."
                }
            }.onFailure {
                toast = if (it.message == "Formato no compatible")
                    "Ese formato todavía no se puede abrir. Hilo lee PDF, EPUB, TXT, MD, HTML, FB2, DOCX, ODT y fotos."
                else "No pude abrir ese archivo. Si es un libro comprado, puede tener DRM."
            }
        }
    }

    fun addText(title: String, text: String) {
        val t = text.trim()
        if (t.isEmpty()) { toast = "Pega un texto primero."; return }
        viewModelScope.launch {
            val b = repo.addText(title.trim().ifBlank { TextTools.firstWords(t) }, t)
            open(b)
        }
    }

    fun exportPdf(book: Book, strokes: List<InkStroke>, notes: List<PdfNote>, uri: Uri) {
        viewModelScope.launch {
            busy = "Guardando PDF con tus anotaciones…"
            val r = runCatching {
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)?.use { out ->
                        PdfExport.export(repo.fileOf(book), strokes, notes, out)
                    } ?: error("sin salida")
                }
            }
            busy = null
            toast = if (r.isSuccess) "PDF guardado con tus anotaciones." else "No pude guardar el PDF. Prueba en otra carpeta."
        }
    }

    /** Párrafos para el modo de texto fluido. Se guardan para abrir rápido la próxima vez. */
    suspend fun flowParas(book: Book, engine: PdfEngine?, progress: (Int, Int) -> Unit): List<String> =
        withContext(Dispatchers.IO) {
            val cache = File(ctx.filesDir, "flow/${book.id}.txt")
            if (cache.exists()) {
                val c = cache.readText().split("\n\n").filter { it.isNotBlank() }
                if (c.isNotEmpty()) return@withContext c
            }
            val file = repo.fileOf(book)
            val paras = when (book.type) {
                "pdf" -> {
                    val e = engine ?: return@withContext emptyList()
                    val pages = e.allPagesText(settings.value.ocr, progress)
                    TextTools.reflow(TextTools.stripRepeats(pages).joinToString("\n"))
                }
                else -> Formats.paragraphs(book.type, file)
            }
            if (paras.isNotEmpty()) {
                cache.parentFile?.mkdirs()
                cache.writeText(paras.joinToString("\n\n"))
            }
            paras
        }
}

/** Temporizador de enfoque: bloques cortos de lectura con descanso. */
class FocusTimer(private val ctx: Context, private val scope: CoroutineScope) {
    var endAt by mutableLongStateOf(0L)
        private set
    var remaining by mutableLongStateOf(0L)
        private set
    var isBreak by mutableStateOf(false)
        private set
    var minutes by mutableStateOf(15)
        private set
    /** Se llena cuando termina un bloque: true si era descanso. */
    var finished by mutableStateOf<Boolean?>(null)
    private var job: Job? = null

    val running get() = endAt > 0L

    fun start(min: Int, breakTime: Boolean = false) {
        minutes = min
        isBreak = breakTime
        endAt = System.currentTimeMillis() + min * 60_000L
        remaining = min * 60_000L
        job?.cancel()
        job = scope.launch {
            while (true) {
                val left = endAt - System.currentTimeMillis()
                if (left <= 0L) break
                remaining = left
                delay(250)
            }
            endAt = 0L
            remaining = 0L
            if (!breakTime) Stats.add(ctx, sessions = 1, minutes = min)
            alarm()
            finished = breakTime
        }
    }

    fun stop() {
        job?.cancel()
        endAt = 0L
        remaining = 0L
    }

    fun addFive() { if (running) endAt += 5 * 60_000L }

    private fun alarm() {
        try {
            val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 90, 180), -1))
        } catch (_: Exception) {}
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 700)
            scope.launch { delay(1200); tg.release() }
        } catch (_: Exception) {}
    }
}

fun formatClock(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}
