package com.hilo.lector.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hilo.lector.HiloViewModel
import com.hilo.lector.Screen
import com.hilo.lector.data.Book
import com.hilo.lector.formatClock
import kotlin.math.roundToInt

private enum class Shelf(val label: String) { ALL("Todos"), READING("Leyendo"), NEW("Sin empezar"), DONE("Terminados") }

private fun Book.shelf(): Shelf = when {
    done || progress >= 0.98f -> Shelf.DONE
    updated > 0 && progress > 0f -> Shelf.READING
    else -> Shelf.NEW
}

private fun Book.status(): String = when (shelf()) {
    Shelf.DONE -> "Terminado"
    Shelf.READING -> "${(progress * 100).roundToInt()}% leído"
    else -> when (type) {
        "pdf" -> if (pages > 0) "$pages págs." else "PDF"
        "epub" -> "EPUB"
        else -> "Texto"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(vm: HiloViewModel) {
    val c = LocalHilo.current
    val books by vm.repo.books.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var shelf by rememberSaveable { mutableStateOf(Shelf.ALL) }
    var addMenu by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<Book?>(null) }
    var renameFor by remember { mutableStateOf<Book?>(null) }
    var deleteFor by remember { mutableStateOf<Book?>(null) }

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        vm.import(uris)
    }
    val pickPhotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(60)) { uris ->
        vm.import(uris)
    }

    val q = query.trim().lowercase()
    val list = books
        .filter { shelf == Shelf.ALL || it.shelf() == shelf }
        .filter { q.isEmpty() || (it.title + " " + it.author).lowercase().contains(q) }
        .sortedByDescending { maxOf(it.updated, it.added) }
    val current = books.filter { it.shelf() == Shelf.READING }.maxByOrNull { it.updated }

    Box(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(112.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Barra superior
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Brand(Modifier.weight(1f))
                        val t = vm.timer
                        IconButton(onClick = { vm.sheet = "timer" }) {
                            if (t.running) Text(formatClock(t.remaining), color = c.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            else Icon(Icons.Rounded.Timer, "Temporizador", tint = c.ink)
                        }
                        IconButton(onClick = { vm.screen = Screen.Guide }) { Icon(Icons.AutoMirrored.Rounded.HelpOutline, "Cómo usar Hilo", tint = c.ink) }
                        IconButton(onClick = { vm.sheet = "settings" }) { Icon(Icons.Rounded.Tune, "Ajustes", tint = c.ink) }
                    }
                    if (current != null) ContinueCard(vm, current)
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
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Shelf.entries.forEach { sh ->
                            val n = if (sh == Shelf.ALL) books.size else books.count { it.shelf() == sh }
                            val on = sh == shelf
                            Text(
                                "${sh.label}  $n",
                                modifier = Modifier.clip(RoundedCornerShape(50))
                                    .background(if (on) c.accent else c.surface)
                                    .clickable { shelf = sh }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                color = if (on) c.accentInk else c.ink, fontSize = 14.sp,
                            )
                        }
                    }
                }
            }

            if (list.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        when {
                            q.isNotEmpty() -> "No hay libros que coincidan con «$query»."
                            books.isEmpty() -> "Todavía no hay libros. Toca Agregar para abrir un PDF, un EPUB, fotos de páginas o un texto copiado."
                            else -> "No hay libros en esta sección."
                        },
                        color = c.muted, fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(24.dp),
                    )
                }
            }

            items(list, key = { it.id }) { b ->
                BookCard(vm, b, onOpen = { vm.open(b) }, onMenu = { menuFor = b })
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "${books.size} ${if (books.size == 1) "libro guardado" else "libros guardados"} en este celular.",
                    color = c.muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }

        // Botón Agregar
        Box(Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            ExtendedFloatingActionButton(
                onClick = { addMenu = true },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("Agregar") },
                containerColor = c.accent, contentColor = c.accentInk,
            )
            DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Archivo (PDF, EPUB, TXT)") },
                    leadingIcon = { Icon(Icons.Rounded.Description, null) },
                    onClick = {
                        addMenu = false
                        pickFiles.launch(arrayOf("application/pdf", "application/epub+zip", "text/plain"))
                    },
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
            }
        }
    }

    menuFor?.let { b ->
        AlertDialog(
            onDismissRequest = { menuFor = null },
            containerColor = c.surface,
            title = { Text(b.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    if (b.author.isNotBlank()) Text(b.author, color = c.muted)
                    Text(b.status(), color = c.muted)
                    Column(Modifier.padding(top = 12.dp)) {
                        MenuLine("Abrir") { menuFor = null; vm.open(b) }
                        MenuLine("Cambiar nombre") { menuFor = null; renameFor = b }
                        if (b.shelf() == Shelf.DONE) {
                            MenuLine("Empezar de nuevo") {
                                menuFor = null
                                vm.repo.update(b.id) { it.copy(done = false, progress = 0f, pos = 0, posOffset = 0, flowPos = 0, flowOffset = 0) }
                            }
                        } else {
                            MenuLine("Marcar como terminado") {
                                menuFor = null
                                vm.repo.update(b.id) { it.copy(done = true, progress = 1f, updated = System.currentTimeMillis()) }
                                vm.toast = "¡Un libro más!"
                            }
                        }
                        MenuLine("Borrar", danger = true) { menuFor = null; deleteFor = b }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { menuFor = null }) { Text("Cerrar") } },
        )
    }

    renameFor?.let { b ->
        var name by remember(b.id) { mutableStateOf(b.title) }
        AlertDialog(
            onDismissRequest = { renameFor = null },
            containerColor = c.surface,
            title = { Text("Cambiar nombre") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) vm.repo.update(b.id) { it.copy(title = name.trim()) }
                    renameFor = null
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { renameFor = null }) { Text("Cancelar") } },
        )
    }

    deleteFor?.let { b ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            containerColor = c.surface,
            title = { Text("¿Borrar «${b.title}»?") },
            text = { Text("Se borran el archivo, tu avance y tus anotaciones de este celular. El archivo original que subiste no se toca.") },
            confirmButton = {
                TextButton(onClick = { vm.repo.remove(b.id); deleteFor = null; vm.toast = "Libro borrado." }) {
                    Text("Borrar", color = c.lines[0])
                }
            },
            dismissButton = { TextButton(onClick = { deleteFor = null }) { Text("Cancelar") } },
        )
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
        if (b.shelf() == Shelf.DONE) {
            Text(
                "Terminado", color = c.accentInk, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(7.dp).clip(RoundedCornerShape(50)).background(c.accent).padding(horizontal = 7.dp, vertical = 3.dp),
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
