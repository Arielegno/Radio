package io.github.arielegno.radio.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.arielegno.radio.RadioApp
import io.github.arielegno.radio.core.Track
import io.github.arielegno.radio.core.fmtDur
import io.github.arielegno.radio.core.normNombre
import io.github.arielegno.radio.launchSafe

@Composable
fun BibliotecaPantalla() {
    val b = RadioApp.biblioteca; val r = RadioApp.radio
    val scope = rememberCoroutineScope()
    var aviso by remember { mutableStateOf("") }
    var buscar by remember { mutableStateOf("") }
    var vista by remember { mutableStateOf("canciones") }
    var orden by remember { mutableStateOf("artista") }
    var menuOrden by remember { mutableStateOf(false) }
    var menuCopia by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Track?>(null) }
    var borrando by remember { mutableStateOf<Track?>(null) }

    val elegir = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) scope.launchSafe {
            val (ok, rep, mal) = b.importar(uris)
            aviso = buildString {
                append("Agregadas: $ok")
                if (rep > 0) append(" · ya estaban: $rep")
                if (mal > 0) append(" · no se pudieron leer: $mal")
                append('.')
            }
        }
    }
    val carpeta = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) scope.launchSafe { aviso = try { b.exportar(uri) } catch (e: Exception) { "No se pudo guardar la copia: ${e.message ?: "error"}" } }
    }
    val copias = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) scope.launchSafe { aviso = b.restaurar(uris) }
    }

    val q = normNombre(buscar)
    val lista = remember(b.pistas, q, orden) {
        val f = if (q.isEmpty()) b.pistas else b.pistas.filter { normNombre(it.name).contains(q) || normNombre(it.artist).contains(q) || normNombre(it.album).contains(q) }
        when (orden) {
            "titulo" -> f.sortedBy { normNombre(it.name) }
            "recientes" -> f.sortedByDescending { it.addedAt }
            "anio" -> f.sortedWith(compareBy<Track>({ if (it.year == 0) 9999 else it.year }, { normNombre(it.artist) }))
            "escuchadas" -> f.sortedByDescending { RadioApp.stats.stats[it.key]?.c ?: 0 }
            else -> f.sortedWith(compareBy<Track>({ normNombre(it.artist) }, { normNombre(it.album) }, { normNombre(it.name) }))
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            Text("Biblioteca", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = { menuCopia = true }) { Icon(Icons.Filled.MoreVert, "Copia de seguridad") }
                DropdownMenu(expanded = menuCopia, onDismissRequest = { menuCopia = false }) {
                    DropdownMenuItem(text = { Text("Guardar copia de mi música") }, leadingIcon = { Icon(Icons.Filled.Backup, null) }, onClick = { menuCopia = false; carpeta.launch(null) })
                    DropdownMenuItem(text = { Text("Restaurar copia (.rrlib)") }, leadingIcon = { Icon(Icons.Filled.Restore, null) }, onClick = { menuCopia = false; copias.launch(arrayOf("*/*")) })
                }
            }
        }
        Text("${b.pistas.size} canciones guardadas en este celular", color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { elegir.launch(arrayOf("audio/*")) }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Agregar canciones") }
            OutlinedButton(onClick = { r.iniciar(null, mezclarEsta = true) }, enabled = b.pistas.isNotEmpty()) { Icon(Icons.Filled.Shuffle, "Mezclar todo") }
        }
        b.progreso?.let { Text(it, color = Amarillo); LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 6.dp)) }
        if (aviso.isNotEmpty()) Text(aviso, color = LocalExtra.current.tenue, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(value = buscar, onValueChange = { buscar = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Buscar canción, artista o álbum") },
            trailingIcon = { if (buscar.isNotEmpty()) IconButton(onClick = { buscar = "" }) { Icon(Icons.Filled.Close, "Borrar búsqueda") } })
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("canciones" to "Canciones", "artistas" to "Artistas", "albumes" to "Álbumes").forEach { (k, n) ->
                FilterChip(selected = vista == k, onClick = { vista = k }, label = { Text(n) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Amarillo, selectedLabelColor = Tinta))
            }
            Spacer(Modifier.weight(1f))
            androidx.compose.foundation.layout.Box {
                TextButton(onClick = { menuOrden = true }) { Text("Ordenar") }
                DropdownMenu(expanded = menuOrden, onDismissRequest = { menuOrden = false }) {
                    listOf("artista" to "Por artista", "titulo" to "Por título", "anio" to "Por año", "recientes" to "Recién agregadas", "escuchadas" to "Más escuchadas").forEach { (k, n) ->
                        DropdownMenuItem(text = { Text(if (orden == k) "✓ $n" else n) }, onClick = { orden = k; menuOrden = false })
                    }
                }
            }
        }
        if (b.pistas.isEmpty()) {
            Text("Aún no tienes canciones. Toca «Agregar canciones» y elige tus archivos de música: se copian a la app y suenan sin internet. " +
                "Si tenías una copia de la versión web (.rrlib), restáurala desde el menú ⋮.", color = LocalExtra.current.tenue, modifier = Modifier.padding(top = 20.dp))
        }
        when (vista) {
            "artistas", "albumes" -> {
                val grupos = remember(lista, vista) {
                    lista.groupBy { if (vista == "artistas") it.artist else (it.album.ifBlank { "Sin álbum" } + "\u0000" + it.artist) }.entries.sortedBy { normNombre(it.key) }.map { it.key to it.value }
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(grupos, key = { it.first }) { (k, ts) ->
                        val nombre = k.substringBefore('\u0000'); val sub = if (vista == "albumes") k.substringAfter('\u0000') else ""
                        Row(Modifier.fillMaxWidth().clickable { r.iniciar({ t -> ts.any { it.id == t.id } }, mezclarEsta = vista == "artistas") }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            val conCaratula = ts.firstOrNull { it.cover.isNotEmpty() }
                            Caratula(conCaratula?.let { b.caratula(it) }, nombre, 56.dp, if (vista == "artistas") 28.dp else 8.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(listOf(sub, "${ts.size} ${if (ts.size == 1) "canción" else "canciones"}").filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue)
                            }
                            Icon(Icons.Filled.PlayArrow, "Escuchar", tint = Amarillo)
                        }
                    }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(lista, key = { it.id }) { t ->
                    FilaCancion(t, onPlay = { r.iniciar({ x -> lista.any { it.id == x.id } }, desdeId = t.id, mezclarEsta = false) },
                        onEditar = { editando = t }, onBorrar = { borrando = t })
                }
            }
        }
    }

    editando?.let { t -> EditarCancion(t, { editando = null }) { n -> scope.launchSafe { b.editar(n) }; editando = null } }
    borrando?.let { t ->
        AlertDialog(onDismissRequest = { borrando = null }, title = { Text("¿Quitar «${t.name}»?") },
            text = { Text("Se borra el archivo de la app. El original en tu teléfono no se toca.") },
            confirmButton = { TextButton(onClick = { scope.launchSafe { b.borrar(setOf(t.id)) }; borrando = null }) { Text("Quitar") } },
            dismissButton = { TextButton(onClick = { borrando = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun FilaCancion(t: Track, onPlay: () -> Unit, onEditar: () -> Unit, onBorrar: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val actual = RadioApp.radio.actual?.id == t.id
    Row(Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Caratula(RadioApp.biblioteca.caratula(t), t.album + t.artist, 50.dp, 8.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (actual) Amarillo else MaterialTheme.colorScheme.onSurface)
            Text(listOf(t.artist, t.album, if (t.year > 0) t.year.toString() else "").filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (t.durMs > 0) Text(fmtDur(t.durMs), style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.tenue)
        androidx.compose.foundation.layout.Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opciones de ${t.name}") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Editar datos") }, leadingIcon = { Icon(Icons.Filled.Edit, null) }, onClick = { menu = false; onEditar() })
                DropdownMenuItem(text = { Text("Quitar de la biblioteca") }, leadingIcon = { Icon(Icons.Filled.Delete, null) }, onClick = { menu = false; onBorrar() })
            }
        }
    }
}

@Composable
private fun EditarCancion(t: Track, cerrar: () -> Unit, guardar: (Track) -> Unit) {
    var nombre by remember { mutableStateOf(t.name) }
    var artista by remember { mutableStateOf(t.artist) }
    var album by remember { mutableStateOf(t.album) }
    var anio by remember { mutableStateOf(if (t.year > 0) t.year.toString() else "") }
    var genero by remember { mutableStateOf(t.genre) }
    AlertDialog(onDismissRequest = cerrar, title = { Text("Editar canción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(nombre, { nombre = it.take(200) }, label = { Text("Título") }, singleLine = true)
                OutlinedTextField(artista, { artista = it.take(200) }, label = { Text("Artista") }, singleLine = true)
                OutlinedTextField(album, { album = it.take(200) }, label = { Text("Álbum") }, singleLine = true)
                OutlinedTextField(anio, { anio = it.filter(Char::isDigit).take(4) }, label = { Text("Año") }, singleLine = true)
                OutlinedTextField(genero, { genero = it.take(80) }, label = { Text("Género") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isNotBlank()) guardar(t.copy(name = nombre.trim(), artist = artista.trim().ifEmpty { "Artista desconocido" }, album = album.trim(),
                    year = anio.toIntOrNull()?.takeIf { it in 1900..2100 } ?: 0, genre = genero.trim()))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } })
}
