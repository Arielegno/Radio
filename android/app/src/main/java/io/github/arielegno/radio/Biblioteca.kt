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
import kotlinx.coroutines.launch
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
    /** Índice por id (buscar una canción era recorrer toda la lista cada vez). */
    @Volatile private var indicePorId: Map<Long, Track> = pistas.associateBy { it.id }
    /** Texto de progreso mientras se agregan o restauran canciones (null si no hay nada en curso). */
    var progreso by mutableStateOf<String?>(null)
        private set

    // Al abrir solo se lee la lista; revisar que cada archivo exista se hace después, en segundo plano
    private fun cargar(): List<Track> = Json.arr(Datos.leer(indice)).mapNotNull { it.asObj()?.let(Track::from) }

    init {
        RadioApp.scope.launch(Dispatchers.IO) {
            val faltan = pistas.filter { !File(dirMusica, it.file).exists() }.map { it.id }.toSet()
            if (faltan.isNotEmpty()) { val l = pistas.filter { it.id !in faltan }; guardar(l); publicar(l) }
        }
    }

    // Guardar la lista completa (miles de canciones) cuesta: se junta y se guarda como mucho cada 8 segundos
    private var guardadoPendiente: kotlinx.coroutines.Job? = null
    private fun guardarLuego() {
        if (guardadoPendiente?.isActive == true) return
        guardadoPendiente = RadioApp.scope.launch(Dispatchers.IO) { kotlinx.coroutines.delay(8000); guardar(pistas) }
    }
    /** Guarda ya lo pendiente (al cerrar la guía o la app). */
    fun guardarAhora() { guardadoPendiente?.cancel(); RadioApp.scope.launch(Dispatchers.IO) { guardar(pistas) } }

    private fun guardar(lista: List<Track>) = Datos.escribir(indice, Json.write(lista.map { it.toMap() }))

    fun archivo(t: Track) = File(dirMusica, t.file)
    fun caratula(t: Track): File? = if (t.cover.isEmpty()) null else File(dirCaratulas, t.cover).takeIf { it.exists() }
    fun porId(id: Long): Track? = indicePorId[id]

    private fun claveDup(name: String, artist: String, size: Long) = normNombre(name) + "|" + normNombre(artist) + "|" + size

    private suspend fun publicar(lista: List<Track>) { val m = lista.associateBy { it.id }; withContext(Dispatchers.Main) { pistas = lista; indicePorId = m } }
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
        while (pendientes.isNotEmpty() && vistas < 20_000) {   // carpetas con muchas subcarpetas (una por álbum)
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
        // Archivos ya importados antes (nombre original + tamaño): si eliges otra vez la misma carpeta, solo entran las nuevas
        val origenes = Json.arr(RadioApp.prefs.str("origenes", "[]")).mapNotNull { it as? String }.toMutableSet()
        val porTamano = lista.groupBy { it.size }
        fun yaEsta(nombre: String, tam: Long): Boolean {
            if (tam <= 0) return false
            if (normNombre(nombre) + "|" + tam in origenes) return true
            val mismos = porTamano[tam] ?: return false
            val n = normNombre(nombre.substringBeforeLast('.'))
            return mismos.any { t -> val a = normNombre(t.name); a.isNotEmpty() && (n.contains(a) || a.contains(n)) }
        }
        var sig = (lista.maxOfOrNull { it.id } ?: 0L) + 1
        val ok = AtomicInteger(); val rep = AtomicInteger(); val mal = AtomicInteger(); val hechas = AtomicInteger()
        val ultimaVista = java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())
        val turnos = Semaphore(3)
        coroutineScope {
            uris.map { uri ->
                async {
                    turnos.withPermit {
                        var copia: File? = null
                        try {
                            var nombre = "cancion"; var tam = -1L
                            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                                if (c.moveToFirst()) {
                                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (ni >= 0 && !c.isNull(ni)) nombre = c.getString(ni)
                                    val si = c.getColumnIndex(OpenableColumns.SIZE); if (si >= 0 && !c.isNull(si)) tam = c.getLong(si)
                                }
                            }
                            val origen = normNombre(nombre) + "|" + tam
                            // Ya está en la app: ni siquiera se copia (mucho más rápido al volver a elegir la carpeta)
                            if (candado.withLock { yaEsta(nombre, tam) }) { rep.incrementAndGet(); candado.withLock { origenes += origen }; throw Saltar() }
                            val mime = cr.getType(uri) ?: "audio/mpeg"
                            val id = candado.withLock { sig++ }
                            val f = File(dirMusica, "$id.${extDe(nombre, mime)}"); copia = f
                            val input = cr.openInputStream(uri) ?: throw IllegalStateException("No se pudo abrir")
                            input.use { inp -> FileOutputStream(f).use { out -> inp.copyTo(out, 128 * 1024) } }
                            val t = leerEtiquetas(f, id, nombre, mime)
                            val k = claveDup(t.name, t.artist, f.length())
                            // Mismo tamaño y misma duración que una que ya tienes: es la misma canción aunque se llame distinto
                            val gemela = porTamano[f.length()]?.any { v -> t.durMs > 0 && kotlin.math.abs(v.durMs - t.durMs) < 1500 } == true
                            val nueva = candado.withLock { origenes += origen; if (k in dups || gemela) false else { dups += k; lista += t; true } }
                            if (nueva) ok.incrementAndGet() else { f.delete(); if (t.cover.isNotEmpty()) File(dirCaratulas, t.cover).delete(); rep.incrementAndGet() }
                        } catch (e: Saltar) {
                        } catch (e: Exception) {
                            copia?.delete(); mal.incrementAndGet()
                        }
                        val n = hechas.incrementAndGet()
                        avisar("Agregando $n de ${uris.size}…")
                        // La pantalla se actualiza cada 60 canciones o cada 5 segundos (antes cada 15: la lista se trababa)
                        val ahora = System.currentTimeMillis()
                        if (n % 60 == 0 || ahora - ultimaVista.get() > 5000) { ultimaVista.set(ahora); val copiaLista = candado.withLock { lista.toList() }; publicar(copiaLista) }
                    }
                }
            }.awaitAll()
        }
        val final = lista.sortedBy { it.id }
        guardar(final); publicar(final); avisar(null)
        RadioApp.prefs.set("origenes", Json.write(origenes.toList().takeLast(20000)))
        if (ok.get() > 0) RadioApp.scope.launchSafe { Guia.mantener(false) }
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
        withContext(Dispatchers.Main) { listas.quitarDeTodas(ids); guardarListas() }
    }

    /** Guarda cambios de varias canciones a la vez; las escuchas y favoritas siguen al nombre nuevo. */
    suspend fun actualizar(cambios: List<Track>) = withContext(Dispatchers.IO) {
        if (cambios.isEmpty()) return@withContext
        val porId = cambios.associateBy { it.id }
        var migro = false
        val l = pistas.map { v -> porId[v.id]?.also { n -> if (n.name != v.name || n.artist != v.artist) { RadioApp.stats.migrar(v.name, v.artist, n.name, n.artist); migro = true } } ?: v }
        publicar(l); guardarLuego()
        if (migro) Datos.guardarStats()
    }

    /** Edición a mano: lo que cambies queda protegido para que la guía no lo vuelva a tocar. */
    suspend fun editar(t: Track) {
        val v = porId(t.id) ?: return
        val campos = buildSet {
            if (t.name != v.name) add("name"); if (t.artist != v.artist) add("artist"); if (t.album != v.album) add("album")
            if (t.year != v.year) add("year"); if (t.genre != v.genre) add("genre"); if (t.cover != v.cover) add("cover")
        }
        actualizar(listOf(t.copy(manual = v.manual + campos)))
    }

    /** Carátula elegida desde la galería. */
    suspend fun cambiarCaratula(t: Track, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val bytes = try { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } } catch (e: Exception) { null } ?: return@withContext false
        val nombre = guardarCaratula(bytes, t.id).ifEmpty { return@withContext false }
        val v = porId(t.id) ?: return@withContext false
        actualizar(listOf(v.copy(cover = nombre, manual = v.manual + "cover")))
        true
    }

    /** Guarda una carátula descargada de internet (para la guía). */
    fun guardarCaratulaDe(bytes: ByteArray, id: Long): String = guardarCaratula(bytes, id)

    /** Espacio que ocupa tu música y el que queda libre en el teléfono. */
    fun espacio(): Pair<Long, Long> {
        val usado = (dirMusica.listFiles()?.sumOf { it.length() } ?: 0L) + (dirCaratulas.listFiles()?.sumOf { it.length() } ?: 0L)
        val libre = try { android.os.StatFs(ctx.filesDir.path).availableBytes } catch (e: Exception) { 0L }
        return usado to libre
    }

    // ---------- Playlists (mismo formato que la web) ----------
    var listas by mutableStateOf(io.github.arielegno.radio.core.Playlists(RadioApp.prefs.str("playlists", "")))
        private set
    /** Sube cada vez que cambian las playlists (para que la pantalla se redibuje). */
    var versionListas by mutableStateOf(0)
        private set
    fun guardarListas() { RadioApp.prefs.set("playlists", listas.toJson()); versionListas++ }
    fun recargarListas() { listas = io.github.arielegno.radio.core.Playlists(RadioApp.prefs.str("playlists", "")); versionListas++ }

    /** Copia de seguridad con audios en la carpeta que elija la persona. */
    /**
     * Copia ordenada: guarda tu música en una carpeta que elijas como «Artista/Álbum/Título.mp3»,
     * con los nombres ya limpios por la guía. No toca ni borra nada de tus carpetas originales.
     * Lo que ya está copiado se salta (puedes repetirla cuando agregues canciones).
     */
    suspend fun copiaOrdenada(arbol: Uri): String = withContext(Dispatchers.IO) {
        val cr = ctx.contentResolver
        fun limpio(t: String) = t.replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1f]"), " ").replace(Regex("\\s+"), " ").trim().trimEnd('.').take(80).ifEmpty { "Sin nombre" }
        val hijos = HashMap<String, MutableMap<String, String>>()   // carpeta → (nombre → id)
        fun listar(doc: String): MutableMap<String, String> = hijos.getOrPut(doc) {
            val m = HashMap<String, String>()
            try {
                cr.query(DocumentsContract.buildChildDocumentsUriUsingTree(arbol, doc), arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
                    while (c.moveToNext()) { val id = c.getString(0); val n = c.getString(1); if (id != null && n != null) m[n] = id }
                }
            } catch (e: Exception) {}
            m
        }
        fun carpeta(padre: String, nombre: String): String {
            listar(padre)[nombre]?.let { return it }
            val u = DocumentsContract.createDocument(cr, DocumentsContract.buildDocumentUriUsingTree(arbol, padre), DocumentsContract.Document.MIME_TYPE_DIR, nombre) ?: throw IllegalStateException("No se pudo crear la carpeta «$nombre»")
            val id = DocumentsContract.getDocumentId(u); listar(padre)[nombre] = id; return id
        }
        val raiz = DocumentsContract.getTreeDocumentId(arbol)
        val lista = pistas
        var hechas = 0; var saltadas = 0; var fallas = 0
        lista.forEachIndexed { i, t ->
            avisar("Copia ordenada: ${i + 1} de ${lista.size}…")
            try {
                val art = carpeta(raiz, limpio(io.github.arielegno.radio.core.Limpieza.artistaPrincipal(t.artist).ifBlank { "Artista desconocido" }))
                val alb = carpeta(art, limpio(t.album.ifBlank { "Sin álbum" }))
                val ext = t.file.substringAfterLast('.', "mp3")
                val nombre = limpio(t.name) + "." + ext
                if (listar(alb).containsKey(nombre)) { saltadas++; return@forEachIndexed }
                val u = DocumentsContract.createDocument(cr, DocumentsContract.buildDocumentUriUsingTree(arbol, alb), t.mime.ifBlank { "audio/mpeg" }, nombre) ?: throw IllegalStateException()
                cr.openOutputStream(u)?.use { out -> File(dirMusica, t.file).inputStream().use { it.copyTo(out, 256 * 1024) } }
                // la carátula del álbum, una vez por carpeta
                if (t.cover.isNotEmpty() && !listar(alb).containsKey("cover.jpg")) caratula(t)?.let { f ->
                    DocumentsContract.createDocument(cr, DocumentsContract.buildDocumentUriUsingTree(arbol, alb), "image/jpeg", "cover.jpg")?.let { cu -> cr.openOutputStream(cu)?.use { o -> f.inputStream().use { it.copyTo(o) } }; listar(alb)["cover.jpg"] = "" }
                }
                listar(alb)[nombre] = ""; hechas++
            } catch (e: Exception) { fallas++ }
        }
        avisar(null)
        "Copia ordenada lista: $hechas canciones copiadas" + (if (saltadas > 0) ", $saltadas ya estaban" else "") + (if (fallas > 0) ", $fallas no se pudieron copiar" else "") + "."
    }

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

/** Señal interna para saltar un archivo que ya está en la biblioteca. */
private class Saltar : Exception()
