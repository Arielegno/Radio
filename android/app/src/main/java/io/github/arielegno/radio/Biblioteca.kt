package io.github.arielegno.radio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.arielegno.radio.core.CopiaMusica
import io.github.arielegno.radio.core.Json
import io.github.arielegno.radio.core.Track
import io.github.arielegno.radio.core.asObj
import io.github.arielegno.radio.core.normNombre
import io.github.arielegno.radio.core.num
import io.github.arielegno.radio.core.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Tu música: los audios se copian a la carpeta privada de la app (así funcionan sin permisos y sin internet). */
class Biblioteca(private val ctx: Context) {
    val dirMusica = File(ctx.filesDir, "music").apply { mkdirs() }
    val dirCaratulas = File(ctx.filesDir, "covers").apply { mkdirs() }
    private val indice = File(ctx.filesDir, "biblioteca.json")

    var pistas by mutableStateOf(cargar())
        private set
    /** Texto de progreso mientras se agregan o restauran canciones (null si no hay nada en curso). */
    var progreso by mutableStateOf<String?>(null)
        private set

    private fun cargar(): List<Track> =
        Json.arr(Datos.leer(indice)).mapNotNull { it.asObj()?.let(Track::from) }.filter { File(dirMusica, it.file).exists() }

    private fun guardar(lista: List<Track>) = Datos.escribir(indice, Json.write(lista.map { it.toMap() }))

    fun archivo(t: Track) = File(dirMusica, t.file)
    fun caratula(t: Track): File? = if (t.cover.isEmpty()) null else File(dirCaratulas, t.cover).takeIf { it.exists() }
    fun porId(id: Long): Track? = pistas.firstOrNull { it.id == id }

    private fun claveDup(name: String, artist: String, size: Long) = normNombre(name) + "|" + normNombre(artist) + "|" + size

    private suspend fun publicar(lista: List<Track>) = withContext(Dispatchers.Main) { pistas = lista }
    private suspend fun avisar(t: String?) = withContext(Dispatchers.Main) { progreso = t }

    private fun extDe(nombre: String, mime: String): String {
        val e = nombre.substringAfterLast('.', "").lowercase().filter { it.isLetterOrDigit() }.take(5)
        if (e.isNotEmpty() && nombre.contains('.')) return e
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "mp3"
    }

    /** Busca todos los audios dentro de una carpeta (y sus subcarpetas) elegida por la persona. */
    suspend fun audiosDeCarpeta(carpeta: Uri): List<Uri> = withContext(Dispatchers.IO) {
        val cr = ctx.contentResolver
        val out = mutableListOf<Uri>()
        val pendientes = ArrayDeque<String>().apply { add(DocumentsContract.getTreeDocumentId(carpeta)) }
        val exts = setOf("mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav", "wma", "alac", "aiff", "aif", "mka", "weba", "webm", "3gp", "amr")
        var vistas = 0
        while (pendientes.isNotEmpty() && vistas < 200) {
            val doc = pendientes.removeFirst(); vistas++
            avisar("Buscando música… ${out.size} canciones encontradas")
            val hijos = DocumentsContract.buildChildDocumentsUriUsingTree(carpeta, doc)
            try {
                cr.query(hijos, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0) ?: continue
                        val mime = c.getString(1).orEmpty(); val nombre = c.getString(2).orEmpty()
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) { if (!nombre.startsWith(".")) pendientes.add(id) }
                        else if (mime.startsWith("audio/") || nombre.substringAfterLast('.', "").lowercase() in exts) out += DocumentsContract.buildDocumentUriUsingTree(carpeta, id)
                    }
                }
            } catch (e: Exception) { /* carpeta sin permiso: se salta */ }
        }
        avisar(null)
        out
    }

    /** Agrega archivos de audio elegidos por la persona (varios a la vez). Devuelve (agregadas, repetidas, fallidas). */
    suspend fun importar(uris: List<Uri>): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        val cr = ctx.contentResolver
        val candado = Mutex()
        val lista = pistas.toMutableList()
        val dups = lista.map { claveDup(it.name, it.artist, it.size) }.toMutableSet()
        var sig = (lista.maxOfOrNull { it.id } ?: 0L) + 1
        val ok = AtomicInteger(); val rep = AtomicInteger(); val mal = AtomicInteger(); val hechas = AtomicInteger()
        val turnos = Semaphore(3)
        coroutineScope {
            uris.map { uri ->
                async {
                    turnos.withPermit {
                        var copia: File? = null
                        try {
                            var nombre = "cancion"
                            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                                if (c.moveToFirst()) { val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (ni >= 0 && !c.isNull(ni)) nombre = c.getString(ni) }
                            }
                            val mime = cr.getType(uri) ?: "audio/mpeg"
                            val id = candado.withLock { sig++ }
                            val f = File(dirMusica, "$id.${extDe(nombre, mime)}"); copia = f
                            val input = cr.openInputStream(uri) ?: throw IllegalStateException("No se pudo abrir")
                            input.use { inp -> FileOutputStream(f).use { out -> inp.copyTo(out, 128 * 1024) } }
                            val t = leerEtiquetas(f, id, nombre, mime)
                            val k = claveDup(t.name, t.artist, f.length())
                            val nueva = candado.withLock { if (k in dups) false else { dups += k; lista += t; true } }
                            if (nueva) ok.incrementAndGet() else { f.delete(); if (t.cover.isNotEmpty()) File(dirCaratulas, t.cover).delete(); rep.incrementAndGet() }
                        } catch (e: Exception) {
                            copia?.delete(); mal.incrementAndGet()
                        }
                        val n = hechas.incrementAndGet()
                        avisar("Agregando $n de ${uris.size}…")
                        if (n % 15 == 0) { val copiaLista = candado.withLock { lista.toList() }; publicar(copiaLista) }
                    }
                }
            }.awaitAll()
        }
        val final = lista.sortedBy { it.id }
        guardar(final); publicar(final); avisar(null)
        Triple(ok.get(), rep.get(), mal.get())
    }

    /** Lee título, artista, álbum, año, género, duración y carátula del archivo. */
    private fun leerEtiquetas(f: File, id: Long, nombreArchivo: String, mime: String): Track {
        val mmr = MediaMetadataRetriever()
        var titulo = ""; var artista = ""; var album = ""; var anio = 0; var genero = ""; var dur = 0L; var cover = ""
        try {
            mmr.setDataSource(f.absolutePath)
            titulo = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().trim()
            artista = (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)).orEmpty().trim()
            album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty().trim()
            val y = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR) ?: mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            anio = Regex("(19|20)\\d{2}").find(y.orEmpty())?.value?.toInt() ?: 0
            genero = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE).orEmpty().trim().removePrefix("(").let { if (Regex("^\\d+\\)?$").matches(it)) "" else it }
            dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            mmr.embeddedPicture?.let { cover = guardarCaratula(it, id) }
        } catch (e: Exception) {
            // archivo sin etiquetas legibles: se usa el nombre del archivo
        } finally {
            try { mmr.release() } catch (e: Exception) {}
        }
        if (titulo.isEmpty() || artista.isEmpty()) {
            val base = nombreArchivo.substringBeforeLast('.').replace('_', ' ').trim()
            val partes = base.split(" - ", limit = 2)
            if (partes.size == 2) { if (artista.isEmpty()) artista = partes[0].trim(); if (titulo.isEmpty()) titulo = partes[1].trim() }
            if (titulo.isEmpty()) titulo = base.ifEmpty { "Sin título" }
        }
        return Track(id, titulo.take(200), artista.ifEmpty { "Artista desconocido" }.take(200), album.take(200), anio, genero.take(80), dur,
            f.name, mime, f.length(), cover, System.currentTimeMillis())
    }

    /** Guarda la carátula reducida a 600 px como JPG. */
    private fun guardarCaratula(bytes: ByteArray, id: Long): String = try {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var s = 1; while (o.outWidth / (s * 2) >= 600 && o.outHeight / (s * 2) >= 600) s *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s })
        if (bmp == null) "" else {
            val k = minOf(1f, 600f / maxOf(bmp.width, bmp.height))
            val final = if (k < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt().coerceAtLeast(1), (bmp.height * k).toInt().coerceAtLeast(1), true) else bmp
            val nombre = "$id.jpg"
            FileOutputStream(File(dirCaratulas, nombre)).use { final.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            nombre
        }
    } catch (e: Exception) { "" }

    suspend fun borrar(ids: Set<Long>) = withContext(Dispatchers.IO) {
        val quedan = pistas.filter { t ->
            if (t.id in ids) { File(dirMusica, t.file).delete(); if (t.cover.isNotEmpty()) File(dirCaratulas, t.cover).delete(); false } else true
        }
        guardar(quedan); publicar(quedan)
    }

    suspend fun editar(t: Track) = withContext(Dispatchers.IO) {
        val l = pistas.map { if (it.id == t.id) t else it }; guardar(l); publicar(l)
    }

    /** Copia de seguridad con audios en la carpeta que elija la persona. */
    suspend fun exportar(carpeta: Uri): String = withContext(Dispatchers.IO) {
        val lista = pistas
        if (lista.isEmpty()) return@withContext "Tu biblioteca está vacía."
        val cr = ctx.contentResolver
        val items = lista.map { t ->
            val archivos = mutableListOf(CopiaMusica.Archivo("blob", t.mime.ifEmpty { "audio/mpeg" }, archivo(t).length()) { archivo(t).inputStream() })
            caratula(t)?.let { c -> archivos += CopiaMusica.Archivo("cover", "image/jpeg", c.length()) { c.inputStream() } }
            CopiaMusica.Elemento(linkedMapOf("name" to t.name, "artist" to t.artist, "album" to t.album, "year" to (if (t.year > 0) t.year.toString() else ""), "genre" to t.genre, "durMs" to t.durMs), archivos)
        }
        val partes = CopiaMusica.partir(items)
        val raiz = DocumentsContract.buildDocumentUriUsingTree(carpeta, DocumentsContract.getTreeDocumentId(carpeta))
        partes.forEachIndexed { i, p ->
            avisar("Guardando copia ${i + 1} de ${partes.size}…")
            val nombre = if (partes.size > 1) "mi-musica-radio-de-ruta-parte-${i + 1}-de-${partes.size}.rrlib" else "mi-musica-radio-de-ruta.rrlib"
            val doc = DocumentsContract.createDocument(cr, raiz, "application/octet-stream", nombre) ?: throw IllegalStateException("No se pudo crear el archivo en esa carpeta.")
            (cr.openOutputStream(doc) ?: throw IllegalStateException("No se pudo escribir en esa carpeta.")).use { out ->
                CopiaMusica.escribir(out.buffered(256 * 1024), p, i + 1, partes.size)
            }
        }
        avisar(null)
        val mb = items.sumOf { it.tam } / 1_000_000
        "Copia lista: ${lista.size} canciones en ${partes.size} ${if (partes.size == 1) "archivo" else "archivos"} ($mb MB). Guárdalos también fuera del celular."
    }

    /** Restaura copias .rrlib (de esta app o de la versión web) sin duplicar canciones. */
    suspend fun restaurar(uris: List<Uri>): String = withContext(Dispatchers.IO) {
        val cr = ctx.contentResolver
        val lista = pistas.toMutableList()
        val dups = lista.map { claveDup(it.name, it.artist, it.size) }.toMutableSet()
        var sig = (lista.maxOfOrNull { it.id } ?: 0L) + 1
        var agregadas = 0; var omitidas = 0; val errores = mutableListOf<String>()
        uris.forEachIndexed { i, uri ->
            avisar("Restaurando copia ${i + 1} de ${uris.size}…")
            var actual = 0L; var mimeActual = "audio/mpeg"
            try {
                val input = cr.openInputStream(uri) ?: throw IllegalStateException("No se pudo abrir la copia.")
                val (a, o) = input.buffered(256 * 1024).use { inp ->
                    CopiaMusica.leer(inp,
                        omitir = { d, refs ->
                            val tam = refs.firstOrNull { it.campo == "blob" }?.tam ?: 0L
                            val repetida = claveDup(d.str("name"), d.str("artist"), tam) in dups || refs.none { it.campo == "blob" }
                            if (!repetida) actual = sig++
                            repetida
                        },
                        guardar = { _, r, flujo ->
                            if (r.campo == "blob") {
                                mimeActual = r.mime.ifEmpty { "audio/mpeg" }
                                val f = File(dirMusica, "$actual.${MimeTypeMap.getSingleton().getExtensionFromMimeType(r.mime) ?: "mp3"}")
                                FileOutputStream(f).use { flujo.copyTo(it, 64 * 1024) }; f.name
                            } else if (r.campo == "cover") {
                                val f = File(dirCaratulas, "$actual.jpg")
                                FileOutputStream(f).use { flujo.copyTo(it, 64 * 1024) }; f.name
                            } else ""
                        },
                        listo = { d, g ->
                            val file = g["blob"].orEmpty(); val f = File(dirMusica, file)
                            if (file.isNotEmpty() && f.exists()) {
                                val anio = Regex("(19|20)\\d{2}").find(d.str("year"))?.value?.toInt() ?: 0
                                val dur = d.num("durMs").takeIf { it > 0 } ?: (d.num("dur") * 1000)
                                val t = Track(actual, d.str("name").ifBlank { "Sin título" }, d.str("artist").ifBlank { "Artista desconocido" }, d.str("album"), anio,
                                    d.str("genre"), dur, file, mimeActual, f.length(), g["cover"].orEmpty(), System.currentTimeMillis())
                                lista += t; dups += claveDup(t.name, t.artist, t.size)
                            }
                        })
                }
                agregadas += a; omitidas += o
                guardar(lista); publicar(lista.toList())
            } catch (e: Exception) {
                errores += (e.message ?: "No se pudo leer la copia.")
            }
        }
        guardar(lista); publicar(lista.toList()); avisar(null)
        buildString {
            append("Copia restaurada: $agregadas canciones nuevas")
            if (omitidas > 0) append(", $omitidas ya estaban")
            append('.')
            if (errores.isNotEmpty()) append(" ").append(errores.distinct().joinToString(" "))
        }
    }
}
