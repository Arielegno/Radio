package com.hilo.lector.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hilo.lector.HiloViewModel
import com.hilo.lector.Screen
import com.hilo.lector.data.Book
import com.hilo.lector.formatClock
import com.hilo.lector.text.Formats
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Sección de la biblioteca, como en el menú lateral. */
private data class Section(val kind: String, val value: String = "") {
    fun title() = when (kind) {
        "reading" -> "Leyendo ahora"
        "fav" -> "Favoritos"
        "toread" -> "Por leer"
        "read" -> "Leídos"
        "author" -> value.ifBlank { "Sin autor" }
        "collection" -> value
        "format" -> Formats.label(value)
        "trash" -> "Papelera"
        else -> "Libros y documentos"
    }
}

private fun Book.reading() = !trashed && !done && updated > 0 && progress > 0f

private fun Book.inSection(sec: Section): Boolean = when (sec.kind) {
    "trash" -> trashed
    else -> !trashed && when (sec.kind) {
        "reading" -> reading()
        "fav" -> favorite
        "toread" -> toRead
        "read" -> done
        "author" -> author == sec.value
        "collection" -> sec.value in collections
        "format" -> type == sec.value
        else -> true
    }
}

private fun Book.status(): String = when {
    done -> "Terminado"
    reading() -> "${(progress * 100).roundToInt()}% leído"
    type == "pdf" && pages > 0 -> "$pages págs."
    else -> Formats.label(type)
}

private fun mimeOf(type: String) = when (type) {
    "pdf" -> "application/pdf"
    "epub" -> "application/epub+zip"
    "md" -> "text/markdown"
    "html" -> "text/html"
    "fb2" -> "application/x-fictionbook+xml"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "odt" -> "application/vnd.oasis.opendocument.text"
    else -> "text/plain"
}

@Composable
fun LibraryScreen(vm: HiloViewModel) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val books by vm.repo.books.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf(Section("all")) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var addMenu by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<Book?>(null) }
    var editFor by remember { mutableStateOf<Book?>(null) }
    var collFor by remember { mutableStateOf<Book?>(null) }
    var deleteFor by remember { mutableStateOf<Book?>(null) }
    var foldersOpen by remember { mutableStateOf(false) }
    var confirmEmpty by remember { mutableStateOf(false) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.onStart() }
    BackHandler(enabled = drawer.isOpen || section.kind != "all" || searching) {
        when {
            drawer.isOpen -> scope.launch { drawer.close() }
            searching -> { searching = false; query = "" }
            else -> section = Section("all")
        }
    }

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> vm.import(uris) }
    val pickPhotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(60)) { uris -> vm.import(uris) }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) vm.addFolder(uri)
    }

    val q = query.trim().lowercase()
    val list = books
        .filter { it.inSection(section) }
        .filter { q.isEmpty() || (it.title + " " + it.author).lowercase().contains(q) }
        .sortedByDescending { maxOf(it.updated, it.added) }
    val current = if (section.kind == "all" && q.isEmpty()) books.filter { it.reading() }.maxByOrNull { it.updated } else null

    fun share(b: Book) {
        runCatching {
            val i = Intent(Intent.ACTION_SEND).setType(mimeOf(b.type))
                .putExtra(Intent.EXTRA_STREAM, vm.repo.shareUri(b))
                .putExtra(Intent.EXTRA_SUBJECT, b.title)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ctx.startActivity(Intent.createChooser(i, "Compartir «${b.title}»"))
        }.onFailure { vm.toast = "No pude compartir este archivo." }
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            LibraryDrawer(
                books = books, section = section,
                onSection = { section = it; scope.launch { drawer.close() } },
                onFolders = { foldersOpen = true; scope.launch { drawer.close() } },
                onTimer = { vm.sheet = "timer"; scope.launch { drawer.close() } },
                onSettings = { vm.sheet = "settings"; scope.launch { drawer.close() } },
                onGuide = { vm.screen = Screen.Guide },
            )
        },
    ) {
        Box(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(112.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Rounded.Menu, "Menú", tint = c.ink) }
                            if (section.kind == "all") Brand(Modifier.weight(1f))
                            else Text(
                                section.title(), style = MaterialTheme.typography.titleLarge, color = c.ink,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { searching = !searching; if (!searching) query = "" }) { Icon(Icons.Rounded.Search, "Buscar", tint = c.ink) }
                            val t = vm.timer
                            IconButton(onClick = { vm.sheet = "timer" }) {
                                if (t.running) Text(formatClock(t.remaining), color = c.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                else Icon(Icons.Rounded.Timer, "Temporizador", tint = c.ink)
                            }
                            IconButton(onClick = { vm.sheet = "settings" }) { Icon(Icons.Rounded.Tune, "Ajustes", tint = c.ink) }
                        }
                        if (searching) {
                            OutlinedTextField(
                                value = query, onValueChange = { query = it },
                                placeholder = { Text("Buscar por título o autor") },
                                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = c.muted) },
                                singleLine = true, shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                                    unfocusedBorderColor = c.line, focusedBorderColor = c.accent,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (current != null) ContinueCard(vm, current)
                        if (section.kind == "trash" && list.isNotEmpty()) {
                            TextButton(onClick = { confirmEmpty = true }) { Text("Vaciar papelera", color = c.lines[0]) }
                        }
                    }
                }

                if (list.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            when {
                                q.isNotEmpty() -> "No hay libros que coincidan con «$query»."
                                section.kind == "trash" -> "La papelera está vacía."
                                books.none { !it.trashed } -> "Todavía no hay libros. Toca Agregar para abrir un PDF, un EPUB, un documento, fotos de páginas o un texto copiado. También puedes elegir una carpeta en el menú."
                                else -> "No hay libros en esta sección."
                            },
                            color = c.muted, fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(24.dp),
                        )
                    }
                }

                items(list, key = { it.id }) { b ->
                    BookCard(vm, b, onOpen = { if (b.trashed) menuFor = b else vm.open(b) }, onMenu = { menuFor = b })
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    val n = books.count { !it.trashed }
                    Text(
                        "$n ${if (n == 1) "libro guardado" else "libros guardados"} en este celular.",
                        color = c.muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }

            Box(Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { addMenu = true },
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text("Agregar") },
                    containerColor = c.accent, contentColor = c.accentInk,
                )
                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Archivos (PDF, EPUB, Word, TXT…)") },
                        leadingIcon = { Icon(Icons.Rounded.Description, null) },
                        onClick = { addMenu = false; pickFiles.launch(arrayOf("*/*")) },
                    )
                    DropdownMenuItem(
                        text = { Text("Fotos de páginas") },
                        leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null) },
                        onClick = {
                            addMenu = false
                            pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Pegar texto") },
                        leadingIcon = { Icon(Icons.Rounded.ContentPaste, null) },
                        onClick = { addMenu = false; vm.pasteDraft = "" },
                    )
                    DropdownMenuItem(
                        text = { Text("Buscar en una carpeta") },
                        leadingIcon = { Icon(Icons.Rounded.Folder, null) },
                        onClick = { addMenu = false; pickFolder.launch(null) },
                    )
                }
            }
        }
    }

    // ---------- menú de cada libro ----------

    menuFor?.let { b ->
        AlertDialog(
            onDismissRequest = { menuFor = null },
            containerColor = c.surface,
            title = { Text(b.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (b.author.isNotBlank()) Text(b.author, color = c.muted)
                    Text(b.status() + " · " + Formats.label(b.type), color = c.muted)
                    Column(Modifier.padding(top = 10.dp)) {
                        if (b.trashed) {
                            MenuLine("Restaurar") { menuFor = null; vm.repo.update(b.id) { it.copy(trashed = false) } }
                            MenuLine("Borrar para siempre", danger = true) { menuFor = null; deleteFor = b }
                        } else {
                            MenuLine("Abrir") { menuFor = null; vm.open(b) }
                            MenuLine(if (b.favorite) "Quitar de favoritos" else "Agregar a favoritos") {
                                menuFor = null; vm.repo.update(b.id) { it.copy(favorite = !it.favorite) }
                            }
                            MenuLine(if (b.toRead) "Quitar de por leer" else "Agregar a por leer") {
                                menuFor = null; vm.repo.update(b.id) { it.copy(toRead = !it.toRead) }
                            }
                            if (b.done) {
                                MenuLine("Empezar de nuevo") {
                                    menuFor = null
                                    vm.repo.update(b.id) { it.copy(done = false, progress = 0f, pos = 0, posOffset = 0, flowPos = 0, flowOffset = 0) }
                                }
                            } else {
                                MenuLine("Marcar como leído") {
                                    menuFor = null
                                    vm.repo.update(b.id) { it.copy(done = true, progress = 1f, toRead = false, updated = System.currentTimeMillis()) }
                                    vm.toast = "¡Un libro más!"
                                }
                            }
                            MenuLine("Colecciones") { menuFor = null; collFor = b }
                            MenuLine("Editar título y autor") { menuFor = null; editFor = b }
                            MenuLine("Compartir archivo") { menuFor = null; share(b) }
                            MenuLine("Mover a la papelera", danger = true) {
                                menuFor = null
                                vm.repo.update(b.id) { it.copy(trashed = true) }
                                vm.toast = "Movido a la papelera."
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { menuFor = null }) { Text("Cerrar") } },
        )
    }

    editFor?.let { b ->
        var name by remember(b.id) { mutableStateOf(b.title) }
        var author by remember(b.id) { mutableStateOf(b.author) }
        AlertDialog(
            onDismissRequest = { editFor = null },
            containerColor = c.surface,
            title = { Text("Editar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(author, { author = it }, label = { Text("Autor") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) vm.repo.update(b.id) { it.copy(title = name.trim(), author = author.trim()) }
                    editFor = null
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { editFor = null }) { Text("Cancelar") } },
        )
    }

    collFor?.let { b ->
        val all = books.flatMap { it.collections }.distinct().sorted()
        var chosen by remember(b.id) { mutableStateOf(b.collections.toSet()) }
        var newName by remember(b.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { collFor = null },
            containerColor = c.surface,
            title = { Text("Colecciones") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    all.forEach { name ->
                        Row(
                            Modifier.fillMaxWidth().clickable { chosen = if (name in chosen) chosen - name else chosen + name },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = name in chosen, onCheckedChange = { on -> chosen = if (on) chosen + name else chosen - name })
                            Text(name, color = c.ink)
                        }
                    }
                    OutlinedTextField(
                        newName, { newName = it }, label = { Text("Nueva colección") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val set = if (newName.isNotBlank()) chosen + newName.trim() else chosen
                    vm.repo.update(b.id) { it.copy(collections = set.sorted()) }
                    collFor = null
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { collFor = null }) { Text("Cancelar") } },
        )
    }

    deleteFor?.let { b ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            containerColor = c.surface,
            title = { Text("¿Borrar «${b.title}» para siempre?") },
            text = { Text("Se borran el archivo, tu avance, tus marcadores y tus anotaciones de este celular. El archivo original que agregaste no se toca.") },
            confirmButton = {
                TextButton(onClick = { vm.repo.remove(b.id); deleteFor = null; vm.toast = "Libro borrado." }) {
                    Text("Borrar", color = c.lines[0])
                }
            },
            dismissButton = { TextButton(onClick = { deleteFor = null }) { Text("Cancelar") } },
        )
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            containerColor = c.surface,
            title = { Text("¿Vaciar la papelera?") },
            text = { Text("Se borran para siempre los libros de la papelera, con su avance y anotaciones.") },
            confirmButton = {
                TextButton(onClick = { vm.repo.emptyTrash(); confirmEmpty = false; vm.toast = "Papelera vacía." }) {
                    Text("Vaciar", color = c.lines[0])
                }
            },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("Cancelar") } },
        )
    }

    if (foldersOpen) {
        var folders by remember { mutableStateOf(vm.repo.folders()) }
        AlertDialog(
            onDismissRequest = { foldersOpen = false },
            containerColor = c.surface,
            title = { Text("Carpetas") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Hilo busca documentos en estas carpetas cada vez que abres la app y agrega los nuevos.",
                        color = c.muted, fontSize = 14.sp,
                    )
                    if (folders.isEmpty()) Text("Todavía no elegiste ninguna carpeta.", color = c.ink)
                    folders.forEach { u ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Folder, null, tint = c.accent)
                            Text(vm.repo.folderName(u), color = c.ink, modifier = Modifier.weight(1f).padding(start = 8.dp))
                            TextButton(onClick = { vm.repo.removeFolder(u); folders = vm.repo.folders() }) { Text("Quitar") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { foldersOpen = false; pickFolder.launch(null) }) { Text("Agregar carpeta") }
            },
            dismissButton = { TextButton(onClick = { foldersOpen = false }) { Text("Cerrar") } },
        )
    }
}

@Composable
private fun LibraryDrawer(
    books: List<Book>,
    section: Section,
    onSection: (Section) -> Unit,
    onFolders: () -> Unit,
    onTimer: () -> Unit,
    onSettings: () -> Unit,
    onGuide: () -> Unit,
) {
    val c = LocalHilo.current
    var open by remember { mutableStateOf("") }
    val live = books.filter { !it.trashed }
    ModalDrawerSheet(drawerContainerColor = c.surface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
            Brand(Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            @Composable
            fun line(icon: ImageVector, label: String, count: Int?, on: Boolean, onClick: () -> Unit) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(50))
                        .background(if (on) c.band else Color.Transparent).clickable(onClick = onClick)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = if (on) c.accent else c.muted)
                    Text(label, color = c.ink, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 16.dp))
                    if (count != null) Text("$count", color = c.muted, fontSize = 14.sp)
                }
            }
            @Composable
            fun sub(label: String, count: Int, on: Boolean, onClick: () -> Unit) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 56.dp, end = 12.dp).clip(RoundedCornerShape(50))
                        .background(if (on) c.band else Color.Transparent).clickable(onClick = onClick)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(label, color = c.ink, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("$count", color = c.muted, fontSize = 13.sp)
                }
            }
            line(Icons.Rounded.AutoStories, "Leyendo ahora", live.count { it.reading() }, section.kind == "reading") { onSection(Section("reading")) }
            line(Icons.Rounded.LibraryBooks, "Libros y documentos", live.size, section.kind == "all") { onSection(Section("all")) }
            line(Icons.Rounded.Star, "Favoritos", live.count { it.favorite }, section.kind == "fav") { onSection(Section("fav")) }
            line(Icons.Rounded.Schedule, "Por leer", live.count { it.toRead }, section.kind == "toread") { onSection(Section("toread")) }
            line(Icons.Rounded.Check, "Leídos", live.count { it.done }, section.kind == "read") { onSection(Section("read")) }

            val authors = live.groupBy { it.author }.toSortedMap()
            line(Icons.Rounded.Person, "Autores", authors.size, false) { open = if (open == "author") "" else "author" }
            if (open == "author") authors.forEach { (a, l) ->
                sub(a.ifBlank { "Sin autor" }, l.size, section.kind == "author" && section.value == a) { onSection(Section("author", a)) }
            }
            val cols = live.flatMap { it.collections }.distinct().sorted()
            line(Icons.Rounded.CollectionsBookmark, "Colecciones", cols.size, false) { open = if (open == "col") "" else "col" }
            if (open == "col") {
                if (cols.isEmpty()) Text(
                    "Agrega libros a una colección desde su menú ⋮.", color = c.muted, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 70.dp, end = 16.dp, bottom = 6.dp),
                )
                cols.forEach { name ->
                    sub(name, live.count { name in it.collections }, section.kind == "collection" && section.value == name) { onSection(Section("collection", name)) }
                }
            }
            val formats = live.groupBy { it.type }.toSortedMap()
            line(Icons.Rounded.Layers, "Formatos", formats.size, false) { open = if (open == "fmt") "" else "fmt" }
            if (open == "fmt") formats.forEach { (t, l) ->
                sub(Formats.label(t), l.size, section.kind == "format" && section.value == t) { onSection(Section("format", t)) }
            }
            line(Icons.Rounded.Folder, "Carpetas", null, false, onFolders)
            line(Icons.Rounded.Delete, "Papelera", books.count { it.trashed }, section.kind == "trash") { onSection(Section("trash")) }

            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = c.line)
            line(Icons.Rounded.Timer, "Temporizador", null, false, onTimer)
            line(Icons.Rounded.Tune, "Ajustes", null, false, onSettings)
            line(Icons.AutoMirrored.Rounded.HelpOutline, "Cómo usar Hilo", null, false, onGuide)
        }
    }
}

@Composable
fun Brand(modifier: Modifier = Modifier) {
    val c = LocalHilo.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Hilo", style = MaterialTheme.typography.headlineSmall, color = c.ink)
        Box(
            Modifier.width(22.dp).height(7.dp).clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(c.lines[0], c.lines[2], c.lines[1], c.lines[3])))
        )
    }
}

@Composable
private fun MenuLine(text: String, danger: Boolean = false, onClick: () -> Unit) {
    val c = LocalHilo.current
    Text(
        text,
        color = if (danger) c.lines[0] else c.ink, fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
    )
}

@Composable
private fun ContinueCard(vm: HiloViewModel, b: Book) {
    val c = LocalHilo.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).clickable { vm.open(b) }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BookCover(vm, b, Modifier.width(64.dp), initialSize = 26)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("SIGUE LEYENDO", fontSize = 11.sp, letterSpacing = 0.8.sp, color = c.muted)
            Text(b.title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp), color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(b.status(), fontSize = 13.sp, color = c.muted)
            LinearProgressIndicator(
                progress = { b.progress }, color = c.accent, trackColor = c.chip,
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            )
        }
        Box(Modifier.size(44.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PlayArrow, "Seguir leyendo", tint = c.accentInk)
        }
    }
}

@Composable
private fun BookCard(vm: HiloViewModel, b: Book, onOpen: () -> Unit, onMenu: () -> Unit) {
    val c = LocalHilo.current
    Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpen)) {
        BookCover(vm, b, Modifier.fillMaxWidth())
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.9f))))
                .padding(start = 9.dp, end = 9.dp, top = 26.dp, bottom = 12.dp),
        ) {
            Text(
                b.title, color = Color.White, fontFamily = Fonts.display, fontWeight = FontWeight.Bold,
                fontSize = 14.sp, lineHeight = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
            Text(b.status(), color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
        }
        if (b.progress > 0f) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.2f))) {
                Box(Modifier.fillMaxWidth(b.progress.coerceIn(0f, 1f)).height(4.dp).background(c.accent))
            }
        }
        if (b.done) {
            Text(
                "Terminado", color = c.accentInk, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(7.dp).clip(RoundedCornerShape(50)).background(c.accent).padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
        if (b.favorite) {
            Icon(
                Icons.Rounded.Star, "Favorito", tint = Color(0xFFFFC94D),
                modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = if (b.done) 30.dp else 6.dp).size(20.dp),
            )
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(5.dp).size(32.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onMenu),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MoreVert, "Opciones de ${b.title}", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
