package com.hilo.lector.ui

import android.content.Intent
import android.Manifest
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.rounded.FiberNew
import androidx.compose.material.icons.rounded.School
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material.icons.rounded.Class
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.isImeVisible
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
        "new" -> "Nuevos"
        else -> "Libros y documentos"
    }
}

private fun Book.reading() = !trashed && !done && updated > 0 && progress > 0f

private fun Book.inSection(sec: Section): Boolean = when (sec.kind) {
    "trash" -> trashed
    else -> !trashed && when (sec.kind) {
        "reading" -> reading()
        "new" -> isNew
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
    "mobi" -> "application/x-mobipocket-ebook"
    else -> "text/plain"
}

@Composable
fun LibraryScreen(vm: HiloViewModel) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val allBooks by vm.repo.books.collectAsStateWithLifecycle()
    // Formatos ocultos en Ajustes: siguen en el celular, solo no se muestran
    val hiddenTypes = vm.settings.state.collectAsStateWithLifecycle().value.hiddenTypes
    val books = remember(allBooks, hiddenTypes) { if (hiddenTypes.isEmpty()) allBooks else allBooks.filter { it.type !in hiddenTypes } }
    val hiddenCount = allBooks.size - books.size
    var section by remember { mutableStateOf(Section("all")) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var addMenu by remember { mutableStateOf(false) }
    var navH by remember { androidx.compose.runtime.mutableIntStateOf(220) }
    // Con el teclado abierto (buscando) la barra de abajo y «Agregar» se esconden para no tapar los resultados
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    val imeOpen = androidx.compose.foundation.layout.WindowInsets.isImeVisible
    var menuFor by remember { mutableStateOf<Book?>(null) }
    var editFor by remember { mutableStateOf<Book?>(null) }
    var collFor by remember { mutableStateOf<List<Book>?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    // Filtros combinables: estado, formato, etiqueta (colección) y fecha de examen
    var filters by rememberSaveable { mutableStateOf(listOf<String>()) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val selecting = selected.isNotEmpty()
    var bulkDelete by remember { mutableStateOf(false) }
    var dupOpen by remember { mutableStateOf(false) }
    val rs by vm.settings.state.collectAsStateWithLifecycle()
    val listView = rs.libraryView == 0
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    fun toggleSel(id: String) { selected = if (id in selected) selected - id else selected + id }
    var deleteFor by remember { mutableStateOf<Book?>(null) }
    var foldersOpen by remember { mutableStateOf(false) }
    var confirmEmpty by remember { mutableStateOf(false) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.onStart() }

    // Acceso a los documentos del celular (como ReadEra)
    var access by remember { mutableStateOf(vm.repo.hasDeviceAccess()) }
    var hideAccess by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = vm.repo.hasDeviceAccess()
        if (now && !access) vm.scanDevice(quiet = false)
        access = now
    }
    val askLegacy = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        access = ok
        if (ok) vm.scanDevice(quiet = false)
    }
    fun askAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + ctx.packageName))
            runCatching { ctx.startActivity(i) }.onFailure {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
            }
        } else {
            askLegacy.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val dueToday = vm.study.due().size
    BackHandler(enabled = drawer.isOpen || section.kind != "all" || searching || selecting || showFilters) {
        when {
            selecting -> selected = emptySet()
            showFilters -> showFilters = false
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

    // Búsqueda sin importar mayúsculas ni tildes: «codigo» encuentra «Código»
    fun fold(t: String) = java.text.Normalizer.normalize(t.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    val q = fold(query.trim())
    val list = books
        .filter { it.inSection(section) }
        .filter { q.isEmpty() || fold(it.title + " " + it.author + " " + it.collections.joinToString(" ")).contains(q) }
        .filter { b ->
            val st = filters.filter { it.startsWith("st:") }
            val fm = filters.filter { it.startsWith("fmt:") }.map { it.removePrefix("fmt:") }
            val tg = filters.filter { it.startsWith("tag:") }.map { it.removePrefix("tag:") }
            st.all { f -> when (f) {
                "st:reading" -> b.reading(); "st:toread" -> b.toRead; "st:done" -> b.done; "st:fav" -> b.favorite
                "st:new" -> b.isNew; "st:exam" -> vm.study.exams[b.id]?.let { it >= com.hilo.lector.data.today() } == true
                else -> true
            } } && (fm.isEmpty() || b.type in fm) && (tg.isEmpty() || tg.any { it in b.collections })
        }
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

    fun shareMany(bs: List<Book>) {
        runCatching {
            val uris = ArrayList(bs.mapNotNull { runCatching { vm.repo.shareUri(it) }.getOrNull() })
            if (uris.isEmpty()) error("sin archivos")
            val i = Intent(Intent.ACTION_SEND_MULTIPLE).setType("*/*")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ctx.startActivity(Intent.createChooser(i, "Compartir ${uris.size} archivos"))
        }.onFailure { vm.toast = "No pude compartir esos archivos." }
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            LibraryDrawer(
                books = books, section = section,
                onSection = { section = it; scope.launch { drawer.close() } },
                onFolders = { foldersOpen = true; scope.launch { drawer.close() } },
                onStudy = { scope.launch { drawer.close() }; vm.openStudy(null) },
                onScan = {
                    scope.launch { drawer.close() }
                    if (vm.repo.hasDeviceAccess()) vm.scanDevice(quiet = false) else askAccess()
                },
                due = dueToday,
                onTimer = { vm.sheet = "timer"; scope.launch { drawer.close() } },
                onSettings = { vm.sheet = "settings"; scope.launch { drawer.close() } },
                onGuide = { vm.screen = Screen.Guide },
                onNews = { vm.screen = Screen.News },
                onDuplicates = { dupOpen = true; scope.launch { drawer.close() } },
            )
        },
    ) {
        Box(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
          Column(Modifier.fillMaxSize()) {
            // Barra de arriba fija: título y botones, o la barra de selección
            if (selecting) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(c.band),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { selected = emptySet() }) { Icon(Icons.Rounded.Close, "Terminar selección", tint = c.ink) }
                    Text(
                        "${selected.size} ${if (selected.size == 1) "seleccionado" else "seleccionados"}",
                        color = c.ink, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f),
                    )
                    val all = list.isNotEmpty() && list.all { it.id in selected }
                    TextButton(onClick = { selected = if (all) emptySet() else list.map { it.id }.toSet() }) { Text(if (all) "Ninguno" else "Todos") }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                    IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Rounded.Menu, "Menú", tint = c.ink) }
                    if (section.kind == "all") Brand(Modifier.weight(1f))
                    else Text(
                        section.title(), style = MaterialTheme.typography.titleLarge, color = c.ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { searching = !searching; if (!searching) query = "" }) { Icon(Icons.Rounded.Search, "Buscar", tint = c.ink) }
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Rounded.FilterList, "Filtros", tint = if (filters.isNotEmpty()) c.accent else c.ink)
                    }
                    HelpButton(vm, "primeros", tint = c.ink)
                    val t = vm.timer
                    IconButton(onClick = { vm.sheet = "timer" }) {
                        if (t.running) Text(formatClock(t.remaining), color = c.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        else Icon(Icons.Rounded.Timer, "Temporizador", tint = c.ink)
                    }
                }
            }
            LazyVerticalGrid(
                columns = if (listView) GridCells.Adaptive(330.dp) else GridCells.Adaptive(112.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = with(LocalDensity.current) { navH.toDp() } + 84.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(if (listView) 10.dp else 12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (searching) {
                            // Al tocar la lupa, el cursor ya queda en el cuadro de búsqueda
                            val focus = remember { androidx.compose.ui.focus.FocusRequester() }
                            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                            OutlinedTextField(
                                value = query, onValueChange = { query = it },
                                placeholder = { Text("Buscar por título o autor") },
                                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = c.muted) },
                                singleLine = true, shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                                    unfocusedBorderColor = c.line, focusedBorderColor = c.accent,
                                ),
                                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            )
                        }
                        if (searching && query.trim().length >= 3) GlobalResults(vm, query.trim())
                        if (showFilters || filters.isNotEmpty()) FilterBar(vm, books, filters) { filters = it }
                        if (!access && !hideAccess && section.kind == "all") {
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("Encuentra tus documentos solo", style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp), color = c.ink)
                                Text(
                                    "Como ReadEra, Hilo puede buscar los PDF, EPUB y Word que ya tienes en el celular y mostrarlos aquí sin copiarlos. Android te pedirá permitir el acceso a los archivos.",
                                    color = c.muted, fontSize = 14.sp,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { askAccess() }) { Text("Permitir", fontWeight = FontWeight.Bold) }
                                    TextButton(onClick = { hideAccess = true }) { Text("Ahora no", color = c.muted) }
                                }
                            }
                        }
                        if (section.kind == "all" && q.isEmpty() && books.any { !it.trashed }) StreakCard(vm)
                        if (current != null) ContinueCard(vm, current)
                        if (dueToday > 0 && section.kind == "all" && q.isEmpty()) {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.band)
                                    .clickable { vm.openStudy(null) }.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.School, null, tint = c.accent)
                                Text(
                                    "Para repasar hoy: $dueToday ${if (dueToday == 1) "tarjeta" else "tarjetas"}",
                                    color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                                )
                                Text("Repasar", color = c.accent, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (section.kind == "trash" && list.isNotEmpty()) {
                            TextButton(onClick = { confirmEmpty = true }) { Text("Vaciar papelera", color = c.lines[0]) }
                        }
                        if (section.kind == "new" && list.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { vm.repo.clearNew(); vm.newFound = 0; section = Section("all"); vm.toast = "Listo, ya no aparecen como nuevos." }) {
                                    Text("Marcar todos como vistos", color = c.ink)
                                }
                                TextButton(onClick = { foldersOpen = true }) { Text("Elegir carpetas") }
                            }
                        }
                        if (list.size > 1 && !selecting && section.kind != "trash") {
                            Text("Mantén el dedo sobre un libro para elegir varios.", color = c.muted, fontSize = 12.sp)
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
                    val sel = b.id in selected
                    val open = { if (selecting) toggleSel(b.id) else if (b.trashed) menuFor = b else vm.open(b) }
                    val long = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); toggleSel(b.id) }
                    if (listView) BookRow(
                        vm, b, sel, selecting, onOpen = open, onLong = long, onMenu = { menuFor = b },
                        onColl = { collFor = listOf(b) },
                    ) else BookCard(vm, b, sel, selecting, onOpen = open, onLong = long, onMenu = { menuFor = b })
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    val n = books.count { !it.trashed }
                    Text(
                        "$n ${if (n == 1) "libro guardado" else "libros guardados"} en este celular." +
                            (if (hiddenCount > 0) " $hiddenCount ocultos por su formato (Ajustes → Formatos que se ven)." else ""),
                        color = c.muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
            // Barra inferior: lo principal siempre a un toque
            if (!selecting && !imeOpen) Row(
                Modifier.fillMaxWidth().background(c.surface).padding(vertical = 4.dp, horizontal = 4.dp)
                    .onSizeChanged { navH = it.height },
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // Con letra muy grande en el celular, los nombres se achican un poco para caber en una línea.
                val navFont = (12f * minOf(1f, 1.15f / LocalDensity.current.fontScale)).sp
                @Composable
                fun RowScope.NavItem(icon: ImageVector, label: String, on: Boolean, badge: Int = 0, onClick: () -> Unit) {
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box {
                            Box(Modifier.clip(RoundedCornerShape(50)).background(if (on) c.band else Color.Transparent).padding(horizontal = 14.dp, vertical = 3.dp)) {
                                Icon(icon, null, tint = if (on) c.accent else c.ink)
                            }
                            if (badge > 0) Box(Modifier.align(Alignment.TopEnd).size(8.dp).clip(CircleShape).background(c.lines[0]))
                        }
                        Text(label, color = if (on) c.accent else c.ink, fontSize = navFont, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
                NavItem(Icons.Rounded.LibraryBooks, "Biblioteca", true) { section = Section("all"); searching = false }
                NavItem(Icons.Rounded.Class, "Clases", false) { vm.screen = com.hilo.lector.Screen.Classes() }
                NavItem(Icons.Rounded.AutoStories, "Leyendo", false) {
                    val last = books.filter { it.reading() }.maxByOrNull { it.updated }
                    if (last != null) vm.open(last) else vm.toast = "Todavía no empezaste ningún libro."
                }
                NavItem(Icons.Rounded.School, "Estudiar", false, dueToday) { vm.openStudy(null) }
                NavItem(Icons.Rounded.Tune, "Ajustes", false) { vm.sheet = "settings" }
            }
          }

            if (vm.newFound > 0 && !selecting) {
                Row(
                    Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 150.dp, bottom = with(LocalDensity.current) { navH.toDp() } + 20.dp)
                        .clip(RoundedCornerShape(12.dp)).background(c.ink).padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Nuevos: ${vm.newFound}", color = c.surface, fontSize = 14.sp, maxLines = 1)
                    TextButton(onClick = { section = Section("new"); vm.newFound = 0 }) { Text("VER", color = Color(0xFFFFC94D), fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { foldersOpen = true }) { Text("CARPETAS", color = c.surface.copy(alpha = 0.8f), fontSize = 12.sp) }
                }
            }
            if (selecting) {
                val chosen = books.filter { it.id in selected }
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(20.dp))
                        .background(c.surface).padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    if (section.kind == "trash") {
                        BulkAction(Icons.Rounded.RestoreFromTrash, "Restaurar") {
                            chosen.forEach { b -> vm.repo.update(b.id) { it.copy(trashed = false) } }
                            vm.toast = "Restaurados: ${chosen.size}."; selected = emptySet()
                        }
                        BulkAction(Icons.Rounded.DeleteForever, "Borrar", danger = true) { bulkDelete = true }
                    } else {
                        BulkAction(Icons.Rounded.Share, "Compartir") { shareMany(chosen) }
                        val allFav = chosen.all { it.favorite }
                        BulkAction(if (allFav) Icons.Rounded.StarBorder else Icons.Rounded.Star, if (allFav) "Quitar ★" else "Favoritos") {
                            chosen.forEach { b -> vm.repo.update(b.id) { it.copy(favorite = !allFav) } }
                            vm.toast = if (allFav) "Quitados de favoritos." else "Agregados a favoritos."
                        }
                        BulkAction(Icons.Rounded.CollectionsBookmark, "Colección") { collFor = chosen }
                        BulkAction(Icons.Rounded.Check, "Leídos") {
                            chosen.forEach { b -> vm.repo.update(b.id) { it.copy(done = true, progress = 1f, toRead = false, isNew = false) } }
                            vm.toast = "Marcados como leídos."; selected = emptySet()
                        }
                        BulkAction(Icons.Rounded.Delete, "Papelera", danger = true) {
                            chosen.forEach { b -> vm.repo.update(b.id) { it.copy(trashed = true) } }
                            val ids = chosen.map { it.id }
                            vm.undoable("${chosen.size} a la papelera") { ids.forEach { id -> vm.repo.update(id) { it.copy(trashed = false) } } }
                            selected = emptySet()
                        }
                    }
                }
            }
            if (!selecting && !imeOpen) Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = with(LocalDensity.current) { navH.toDp() } + 16.dp)) {
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
                        text = { Text("Buscar en todo el celular") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        onClick = { addMenu = false; if (vm.repo.hasDeviceAccess()) vm.scanDevice(quiet = false) else askAccess() },
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
                            MenuLine("Estudiar: tarjetas y resaltados") { menuFor = null; vm.openStudy(b.id) }
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
                            if (b.type == "comic") MenuLine(if (vm.motor.flag("manga:${b.id}")) "Leer de izquierda a derecha" else "Leer como manga (derecha a izquierda)") {
                                menuFor = null; vm.motor.setFlag("manga:${b.id}", !vm.motor.flag("manga:${b.id}"))
                            }
                            MenuLine("Etiquetas y colecciones") { menuFor = null; collFor = listOf(b) }
                            MenuLine("Elegir varios") { menuFor = null; selected = setOf(b.id) }
                            MenuLine("Editar título y autor") { menuFor = null; editFor = b }
                            MenuLine("Compartir archivo") { menuFor = null; share(b) }
                            MenuLine("Mover a la papelera", danger = true) {
                                menuFor = null
                                vm.repo.update(b.id) { it.copy(trashed = true) }
                                vm.undoable("«${b.title.take(30)}» a la papelera") { vm.repo.update(b.id) { it.copy(trashed = false) } }
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

    collFor?.let { group ->
        val all = books.flatMap { it.collections }.distinct().sorted()
        val key = group.joinToString { it.id }
        var chosen by remember(key) { mutableStateOf(group.map { it.collections.toSet() }.reduce { a, b -> a intersect b }) }
        var newName by remember(key) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { collFor = null },
            containerColor = c.surface,
            title = { Text(if (group.size == 1) "Colecciones" else "Colecciones de ${group.size} libros") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (group.size > 1) Text("Se agregan a las colecciones que marques.", color = c.muted, fontSize = 13.sp)
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
                    group.forEach { b ->
                        vm.repo.update(b.id) { it.copy(collections = (if (group.size == 1) set else it.collections.toSet() + set).sorted()) }
                    }
                    collFor = null
                    if (group.size > 1) { selected = emptySet(); vm.toast = "Listo." }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { collFor = null }) { Text("Cancelar") } },
        )
    }

    if (bulkDelete) {
        val chosen = books.filter { it.id in selected }
        AlertDialog(
            onDismissRequest = { bulkDelete = false },
            containerColor = c.surface,
            title = { Text("¿Borrar ${chosen.size} para siempre?") },
            text = { Text("Salen de Hilo con su avance y anotaciones. Los documentos que estaban en tu celular siguen ahí.") },
            confirmButton = {
                TextButton(onClick = {
                    chosen.forEach { vm.study.removeBook(it.id); vm.repo.remove(it.id) }
                    bulkDelete = false; selected = emptySet(); vm.toast = "Borrados."
                }) { Text("Borrar", color = c.lines[0]) }
            },
            dismissButton = { TextButton(onClick = { bulkDelete = false }) { Text("Cancelar") } },
        )
    }

    deleteFor?.let { b ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            containerColor = c.surface,
            title = { Text("¿Borrar «${b.title}» para siempre?") },
            text = {
                Text(
                    if (b.linked) "Sale de Hilo con su avance, marcadores, tarjetas y anotaciones. El archivo sigue en tu celular."
                    else "Se borran la copia, tu avance, tus marcadores, tarjetas y anotaciones. El archivo original que agregaste no se toca."
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.study.removeBook(b.id); vm.repo.remove(b.id); deleteFor = null; vm.toast = "Libro borrado." }) {
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
                TextButton(onClick = {
                    books.filter { it.trashed }.forEach { vm.study.removeBook(it.id) }
                    vm.repo.emptyTrash(); confirmEmpty = false; vm.toast = "Papelera vacía."
                }) {
                    Text("Vaciar", color = c.lines[0])
                }
            },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("Cancelar") } },
        )
    }

    if (dupOpen) {
        val groups by androidx.compose.runtime.produceState<List<List<Book>>?>(null) { value = runCatching { vm.repo.duplicates() }.getOrDefault(emptyList()) }
        AlertDialog(
            onDismissRequest = { dupOpen = false },
            containerColor = c.surface,
            title = { Text("Duplicados") },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val g = groups
                    when {
                        g == null -> Text("Comparando archivos…", color = c.muted)
                        g.isEmpty() -> Text("No hay libros repetidos. ¡Todo en orden!", color = c.ink)
                        else -> {
                            Text("Son el mismo archivo aunque tengan otro nombre. Se queda el que más leíste; los demás salen de Hilo (los del celular no se borran).", color = c.muted, fontSize = 13.sp)
                            g.forEach { grp ->
                                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.band).padding(10.dp)) {
                                    grp.forEach { b -> Text("• ${b.title}" + if (b.progress > 0) " (${(b.progress * 100).roundToInt()} %)" else "", color = c.ink, fontSize = 14.sp) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val g = groups
                if (!g.isNullOrEmpty()) TextButton(onClick = {
                    var n = 0
                    g.forEach { grp ->
                        val keep = grp.maxWith(compareBy<Book>({ it.progress }, { it.bookmarks.size }, { -it.added }))
                        grp.filter { it.id != keep.id }.forEach { vm.study.removeBook(it.id); vm.repo.remove(it.id); n++ }
                    }
                    dupOpen = false; vm.toast = "Quité $n ${if (n == 1) "copia" else "copias"}."
                }) { Text("Quitar copias") }
            },
            dismissButton = { TextButton(onClick = { dupOpen = false }) { Text("Cerrar") } },
        )
    }

    if (foldersOpen) {
        FoldersDialog(vm, onAdd = { foldersOpen = false; pickFolder.launch(null) }) { foldersOpen = false }
    }
}

/** Filtros combinables de la biblioteca. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FilterBar(vm: HiloViewModel, books: List<Book>, filters: List<String>, onChange: (List<String>) -> Unit) {
    val c = LocalHilo.current
    fun toggle(f: String) = onChange(if (f in filters) filters - f else filters + f)
    val live = books.filter { !it.trashed }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Filtros", color = c.ink, fontWeight = FontWeight.Bold)
            HelpButton(vm, "filtros", size = 16.dp)
            Spacer(Modifier.weight(1f))
            if (filters.isNotEmpty()) TextButton(onClick = { onChange(emptyList()) }) { Text("Quitar filtros") }
        }
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("st:reading" to "Leyendo", "st:toread" to "Por leer", "st:done" to "Leídos", "st:fav" to "Favoritos", "st:new" to "Nuevos", "st:exam" to "Con examen")
                .forEach { (k, l) -> Pill(l, k in filters) { toggle(k) } }
        }
        val fmts = live.map { it.type }.distinct().sorted()
        if (fmts.size > 1) androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            fmts.forEach { t -> Pill(Formats.label(t), "fmt:$t" in filters) { toggle("fmt:$t") } }
        }
        Text(
            "Ocultar formatos siempre (por ejemplo HTML) →", color = c.accent, fontSize = 13.sp,
            modifier = Modifier.clip(RoundedCornerShape(50)).clickable { vm.sheet = "settings" }.padding(vertical = 4.dp, horizontal = 2.dp),
        )
        val tags = live.flatMap { it.collections }.distinct().sorted()
        if (tags.isNotEmpty()) androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tags.forEach { t -> Pill("# $t", "tag:$t" in filters) { toggle("tag:$t") } }
        }
        else Text("Agrega etiquetas o materias a tus libros desde el botón de colecciones.", color = c.muted, fontSize = 12.sp)
    }
}

/** Búsqueda en el texto de todos los libros y en tus tarjetas, apuntes y escritos. */
@Composable
private fun GlobalResults(vm: HiloViewModel, query: String) {
    val c = LocalHilo.current
    val all by vm.repo.books.collectAsStateWithLifecycle()
    val hidden = vm.settings.state.collectAsStateWithLifecycle().value.hiddenTypes
    val books = all.filter { it.type !in hidden }
    val hits by androidx.compose.runtime.produceState<List<com.hilo.lector.data.Hit>?>(null, query) {
        kotlinx.coroutines.delay(350)
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { vm.search.search(query, 120) }.getOrDefault(emptyList()) }
    }
    fun fold(t: String) = java.text.Normalizer.normalize(t.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    val fq = fold(query)
    val mine = remember(query) {
        val out = ArrayList<Triple<String, String, String>>()   // tipo, texto, libro
        vm.study.cards.filter { fold(it.front + " " + it.back).contains(fq) }.take(8).forEach { out.add(Triple("Tarjeta", it.front, it.bookId)) }
        vm.study.notes.filter { fold(it.value).contains(fq) }.forEach { (b, t) -> out.add(Triple("Apuntes", t.lines().firstOrNull { l -> fold(l).contains(fq) } ?: t.take(80), b)) }
        vm.study.marks.filter { fold(it.text).contains(fq) }.take(6).forEach { out.add(Triple("Resaltado", it.text, it.bookId)) }
        vm.study.fichas.filter { f -> fold(f.title + f.fields.joinToString { it.second }).contains(fq) }.take(5).forEach { out.add(Triple("Ficha", it.title, it.bookId)) }
        vm.motor.entries.filter { fold(it.title + " " + it.text).contains(fq) }.take(6).forEach { out.add(Triple("Escrito", it.title.ifBlank { it.text.take(60) }, it.book)) }
        out
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val h = hits
        Text("Dentro de tus libros", color = c.ink, fontWeight = FontWeight.Bold)
        when {
            h == null -> Text("Buscando…", color = c.muted, fontSize = 13.sp)
            h.isEmpty() -> Text("No aparece en el texto de los libros que Hilo ya leyó. (Los PDF se suman después de abrirlos en modo Texto o en su sala de estudio.)", color = c.muted, fontSize = 13.sp)
            else -> {
                val byBook = h.groupBy { it.bookId }
                Text("${h.size}${if (h.size >= 120) "+" else ""} resultados en ${byBook.size} ${if (byBook.size == 1) "libro" else "libros"}", color = c.muted, fontSize = 12.sp)
                byBook.entries.take(12).forEach { (id, l) ->
                    val b = books.firstOrNull { it.id == id } ?: return@forEach
                    Text(b.title, color = c.accent, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    l.take(3).forEach { hit ->
                        Text(
                            hit.snippet, color = c.ink, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { vm.openAt(id, true, hit.para) }.padding(vertical = 4.dp, horizontal = 6.dp),
                        )
                    }
                }
            }
        }
        if (mine.isNotEmpty()) {
            HorizontalDivider(color = c.line)
            Text("En tus tarjetas, apuntes y escritos", color = c.ink, fontWeight = FontWeight.Bold)
            mine.take(40).forEach { (kind, text, bookId) ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { vm.openStudy(bookId.takeIf { id -> books.any { it.id == id } }) }.padding(vertical = 4.dp)) {
                    Text(kind.uppercase(), color = c.muted, fontSize = 10.sp, modifier = Modifier.width(76.dp).padding(top = 2.dp))
                    Text(text, color = c.ink, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Nombre amable de una carpeta del celular. */
private fun folderLabel(path: String): String {
    val rel = path.removePrefix("/storage/emulated/0/").removePrefix("/sdcard/")
    return when {
        rel.contains("WhatsApp", true) -> "WhatsApp · " + rel.substringAfterLast('/')
        rel.contains("Telegram", true) -> "Telegram · " + rel.substringAfterLast('/')
        rel.equals("Download", true) || rel.equals("Downloads", true) -> "Descargas"
        rel.equals("Documents", true) -> "Documentos"
        rel.startsWith("Download/", true) -> "Descargas › " + rel.substringAfter('/')
        rel.startsWith("Documents/", true) -> "Documentos › " + rel.substringAfter('/')
        else -> rel.ifBlank { path }
    }
}

/** Carpetas: las que Hilo encontró en el celular (se pueden dejar de leer) y las que elegiste tú. */
@Composable
private fun FoldersDialog(vm: HiloViewModel, onAdd: () -> Unit, onClose: () -> Unit) {
    val c = LocalHilo.current
    val books by vm.repo.books.collectAsStateWithLifecycle()
    var excluded by remember { mutableStateOf(vm.repo.excludedFolders()) }
    var picked by remember { mutableStateOf(vm.repo.folders()) }
    val found = remember(books, excluded) {
        val live = vm.repo.deviceFolders().toMap()
        (live.keys + excluded).distinct().map { it to (live[it] ?: 0) }.sortedWith(compareBy({ it.first in excluded }, { -it.second }))
    }
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = c.surface,
        title = { Text("Carpetas") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (found.isNotEmpty()) {
                    Label("Encontradas en el celular")
                    Text("Apaga las carpetas que no quieres en Hilo (por ejemplo, documentos de WhatsApp). Los archivos no se borran.", color = c.muted, fontSize = 13.sp)
                    found.forEach { (path, n) ->
                        val on = path !in excluded
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Folder, null, tint = if (on) c.accent else c.muted)
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(folderLabel(path), color = if (on) c.ink else c.muted, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(if (on) "$n ${if (n == 1) "documento" else "documentos"}" else "No se lee", color = c.muted, fontSize = 12.sp)
                            }
                            androidx.compose.material3.Switch(checked = on, onCheckedChange = { want ->
                                if (want) { vm.repo.includeFolder(path); vm.scanDevice(quiet = false) }
                                else { val k = vm.repo.excludeFolder(path); vm.toast = "Quité $k ${if (k == 1) "documento" else "documentos"} de esa carpeta." }
                                excluded = vm.repo.excludedFolders()
                            })
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = c.line)
                }
                Label("Elegidas por ti")
                Text("Hilo revisa estas carpetas cada vez que abres la app y agrega lo nuevo.", color = c.muted, fontSize = 13.sp)
                if (picked.isEmpty()) Text("Todavía no elegiste ninguna.", color = c.ink, fontSize = 14.sp)
                picked.forEach { u ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Folder, null, tint = c.accent)
                        Text(vm.repo.folderName(u), color = c.ink, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        TextButton(onClick = { vm.repo.removeFolder(u); picked = vm.repo.folders() }) { Text("Quitar") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onAdd) { Text("Agregar carpeta") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cerrar") } },
    )
}

@Composable
private fun LibraryDrawer(
    books: List<Book>,
    section: Section,
    onSection: (Section) -> Unit,
    onFolders: () -> Unit,
    onStudy: () -> Unit,
    onScan: () -> Unit,
    due: Int,
    onTimer: () -> Unit,
    onSettings: () -> Unit,
    onGuide: () -> Unit,
    onNews: () -> Unit,
    onDuplicates: () -> Unit,
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
            line(Icons.Rounded.School, "Estudio", due, false, onStudy)
            val newCount = live.count { it.isNew }
            if (newCount > 0) line(Icons.Rounded.FiberNew, "Nuevos", newCount, section.kind == "new") { onSection(Section("new")) }
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
            line(Icons.Rounded.Search, "Buscar en el celular", null, false, onScan)
            line(Icons.Rounded.ContentCopy, "Duplicados", null, false, onDuplicates)
            line(Icons.Rounded.Delete, "Papelera", books.count { it.trashed }, section.kind == "trash") { onSection(Section("trash")) }

            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = c.line)
            line(Icons.Rounded.Timer, "Temporizador", null, false, onTimer)
            line(Icons.Rounded.Tune, "Ajustes", null, false, onSettings)
            line(Icons.AutoMirrored.Rounded.HelpOutline, "Cómo usar Hilo", null, false, onGuide)
            line(Icons.Rounded.NewReleases, "Novedades · versión ${com.hilo.lector.data.Changelog.current.name}", null, false, onNews)
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
            // Para los días en que cuesta empezar
            Text(
                "▶ Solo 2 min", color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(c.band)
                    .clickable { vm.twoMin = true; vm.open(b); vm.timer.start(2) }.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        Box(Modifier.size(44.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PlayArrow, "Seguir leyendo", tint = c.accentInk)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCard(vm: HiloViewModel, b: Book, sel: Boolean, selecting: Boolean, onOpen: () -> Unit, onLong: () -> Unit, onMenu: () -> Unit) {
    val c = LocalHilo.current
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).combinedClickable(onClick = onOpen, onLongClick = onLong)
            .then(if (sel) Modifier.border(3.dp, c.accent, RoundedCornerShape(12.dp)) else Modifier)
    ) {
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
        if (b.isNew && !b.done) {
            Text(
                "Nuevo", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(7.dp).clip(RoundedCornerShape(50)).background(c.lines[3]).padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
        if (b.favorite) {
            Icon(
                Icons.Rounded.Star, "Favorito", tint = Color(0xFFFFC94D),
                modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = if (b.done || b.isNew) 30.dp else 6.dp).size(20.dp),
            )
        }
        if (selecting) {
            SelectMark(sel, Modifier.align(Alignment.TopEnd).padding(6.dp))
        } else Box(
            Modifier.align(Alignment.TopEnd).padding(5.dp).size(32.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onMenu),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MoreVert, "Opciones de ${b.title}", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SelectMark(on: Boolean, modifier: Modifier = Modifier) {
    val c = LocalHilo.current
    Box(
        modifier.size(28.dp).clip(CircleShape).background(if (on) c.accent else Color.Black.copy(alpha = 0.35f))
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) { if (on) Icon(Icons.Rounded.Check, "Elegido", tint = c.accentInk, modifier = Modifier.size(18.dp)) }
}

private fun sizeLabel(bytes: Long): String = when {
    bytes >= 1_000_000 -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_000_000.0)
    bytes >= 1000 -> "${bytes / 1000} kB"
    else -> "$bytes B"
}

/** Un libro en la vista de lista, como en ReadEra: portada, datos, avance y botones rápidos. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookRow(
    vm: HiloViewModel, b: Book, sel: Boolean, selecting: Boolean,
    onOpen: () -> Unit, onLong: () -> Unit, onMenu: () -> Unit, onColl: () -> Unit,
) {
    val c = LocalHilo.current
    val size by androidx.compose.runtime.produceState<Long?>(null, b.id, b.file) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { vm.repo.fileOf(b).length() }.getOrNull() }
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (sel) c.band else c.surface)
            .then(if (sel) Modifier.border(2.dp, c.accent, RoundedCornerShape(18.dp)) else Modifier)
            .combinedClickable(onClick = onOpen, onLongClick = onLong).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.width(76.dp)) {
            BookCover(vm, b, Modifier.fillMaxWidth(), initialSize = 22)
            if (selecting) SelectMark(sel, Modifier.align(Alignment.TopStart).padding(4.dp))
            else if (b.isNew && !b.done) Text(
                "Nuevo", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp).clip(RoundedCornerShape(50)).background(c.lines[3]).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(b.title, color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (b.author.isNotBlank()) Text(b.author, color = c.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(Formats.label(b.type), size?.let { sizeLabel(it) }, if (b.type == "pdf" && b.pages > 0) "${b.pages} págs." else null).joinToString(" · "),
                color = c.muted, fontSize = 13.sp,
            )
            if (b.progress > 0f || b.done) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    LinearProgressIndicator(
                        progress = { if (b.done) 1f else b.progress }, color = c.accent, trackColor = c.chip,
                        modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)),
                    )
                    Text(if (b.done) "Leído" else "${(b.progress * 100).roundToInt()}%", color = c.muted, fontSize = 12.sp)
                }
            }
            if (!selecting) Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                QuickIcon(if (b.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (b.favorite) "Quitar de favoritos" else "Agregar a favoritos", b.favorite) {
                    vm.repo.update(b.id) { it.copy(favorite = !it.favorite) }
                }
                QuickIcon(Icons.Rounded.Schedule, if (b.toRead) "Quitar de por leer" else "Agregar a por leer", b.toRead) {
                    vm.repo.update(b.id) { it.copy(toRead = !it.toRead) }
                }
                QuickIcon(Icons.Rounded.DoneAll, if (b.done) "Marcar como no leído" else "Marcar como leído", b.done) {
                    vm.repo.update(b.id) { if (it.done) it.copy(done = false, progress = 0f) else it.copy(done = true, progress = 1f, toRead = false, isNew = false) }
                }
                QuickIcon(Icons.Rounded.CollectionsBookmark, "Colecciones", b.collections.isNotEmpty(), onColl)
                QuickIcon(Icons.Rounded.MoreVert, "Opciones de ${b.title}", false, onMenu)
            }
        }
    }
}

@Composable
private fun QuickIcon(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    val c = LocalHilo.current
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(icon, label, tint = if (on) (if (icon == Icons.Rounded.Star) Color(0xFFFFB300) else c.accent) else c.muted, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun BulkAction(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val c = LocalHilo.current
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = if (danger) c.lines[0] else c.ink)
        Text(label, color = if (danger) c.lines[0] else c.ink, fontSize = 11.sp, maxLines = 1)
    }
}
