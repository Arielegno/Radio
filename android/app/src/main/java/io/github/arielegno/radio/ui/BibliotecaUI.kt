package io.github.arielegno.radio.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.arielegno.radio.Guia
import io.github.arielegno.radio.RadioApp
import io.github.arielegno.radio.Revista
import io.github.arielegno.radio.core.Bio
import io.github.arielegno.radio.core.Coleccion
import io.github.arielegno.radio.core.Concierto
import io.github.arielegno.radio.core.Json
import io.github.arielegno.radio.core.Track
import io.github.arielegno.radio.core.fmtDur
import io.github.arielegno.radio.core.normNombre
import io.github.arielegno.radio.launchSafe

/** Una carpeta abierta: un artista, un álbum, un año, un género o una playlist. */
private data class Carpeta(val vista: String, val clave: String, val artista: String = "")

private fun mb(b: Long): String = when { b >= 1_000_000_000 -> String.format("%.1f GB", b / 1e9); b >= 1_000_000 -> "${b / 1_000_000} MB"; else -> "${b / 1000} KB" }

private fun favArtistas(): MutableMap<String, Any?> = Json.obj(RadioApp.prefs.str("fav_artists", "")).toMutableMap()

@Composable
fun BibliotecaPantalla(pedirAgregar: Boolean = false, alAtender: () -> Unit = {}, abrirRevista: (String) -> Unit = {}) {
    val b = RadioApp.biblioteca; val r = RadioApp.radio; val p = RadioApp.prefs
    val scope = rememberCoroutineScope()
    var aviso by remember { mutableStateOf("") }
    var buscar by rememberSaveable { mutableStateOf("") }
    var vista by rememberSaveable { mutableStateOf(p.str("libVista", "artistas")) }
    var orden by rememberSaveable { mutableStateOf(p.str("libOrden", "original")) }
    var filtros by remember { mutableStateOf(Coleccion.Filtros()) }
    var carpeta by remember { mutableStateOf<Carpeta?>(null) }
    var agregar by remember { mutableStateOf(false) }
    var verFiltros by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var menuOrden by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Track?>(null) }
    var borrando by remember { mutableStateOf<Track?>(null) }
    var aPlaylist by remember { mutableStateOf<Track?>(null) }
    var nuevaLista by remember { mutableStateOf(false) }
    var renombrar by remember { mutableStateOf<String?>(null) }
    var borrarLista by remember { mutableStateOf<String?>(null) }
    var caratulaDe by remember { mutableStateOf<Track?>(null) }
    LaunchedEffect(pedirAgregar) { if (pedirAgregar) { agregar = true; alAtender() } }
    BackHandler(enabled = carpeta != null) { carpeta = null }

    fun resumen(ok: Int, rep: Int, mal: Int, pre: String) = buildString {
        append("$pre$ok canciones nuevas"); if (rep > 0) append(" · ya estaban: $rep"); if (mal > 0) append(" · no se pudieron leer: $mal")
        append(". La guía limpiará y completará los datos.")
    }
    val elegir = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) scope.launchSafe { val (ok, rep, mal) = b.importar(uris); aviso = resumen(ok, rep, mal, "Agregadas: ") }
    }
    val carpetaMusica = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) scope.launchSafe {
            val audios = b.audiosDeCarpeta(uri)
            if (audios.isEmpty()) { aviso = "No encontré música en esa carpeta."; return@launchSafe }
            val (ok, rep, mal) = b.importar(audios); aviso = resumen(ok, rep, mal, "De la carpeta: ")
        }
    }
    val guardarCopia = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) scope.launchSafe { aviso = try { b.exportar(uri) } catch (e: Exception) { "No se pudo guardar la copia: ${e.message ?: "error"}" } }
    }
    val copias = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) scope.launchSafe { aviso = b.restaurar(uris) }
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        val t = caratulaDe; caratulaDe = null
        if (uri != null && t != null) scope.launchSafe { aviso = if (b.cambiarCaratula(t, uri)) "Carátula cambiada." else "No se pudo usar esa imagen." }
    }

    val versionStats = RadioApp.stats.stats.size
    val escuchas = remember(versionStats, b.pistas) { { t: Track -> RadioApp.stats.stats[t.key]?.c ?: 0 } }
    val filtradas = remember(b.pistas, buscar, filtros, orden, versionStats, RadioApp.stats.favs.size) {
        Coleccion.ordenar(b.pistas.filter { Coleccion.buscar(it, buscar) && Coleccion.pasa(it, filtros) { t -> RadioApp.stats.isFav(t.name, t.artist) } }, orden, escuchas)
    }
    val acciones = AccionesCancion(
        play = { lista, t -> r.iniciar({ x -> lista.any { it.id == x.id } }, desdeId = t.id, mezclarEsta = false) },
        editar = { editando = it }, borrar = { borrando = it }, playlist = { aPlaylist = it },
        caratula = { caratulaDe = it; galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        completar = { t -> scope.launchSafe { aviso = Guia.completar(t.id) } }
    )

    val c = carpeta
    if (c != null) {
        DetalleCarpeta(c, filtradas, acciones, cerrar = { carpeta = null }, abrir = { carpeta = it }, abrirRevista = abrirRevista,
            renombrar = { renombrar = it }, borrarLista = { borrarLista = it })
    } else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Text("Biblioteca", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { r.iniciar(null, mezclarEsta = true) }, enabled = b.pistas.isNotEmpty()) { Icon(Icons.Filled.Shuffle, "Mezclar toda tu música") }
                IconButton(onClick = { agregar = true }) { Icon(Icons.Filled.LibraryAdd, "Agregar música y copias") }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Más opciones") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (Guia.trabajando) DropdownMenuItem(text = { Text("Detener la guía") }, leadingIcon = { Icon(Icons.Filled.Stop, null) }, onClick = { menu = false; Guia.detener() })
                        else DropdownMenuItem(text = { Text("Limpiar y completar") }, leadingIcon = { Icon(Icons.Filled.AutoAwesome, null) },
                            onClick = { menu = false; scope.launchSafe { aviso = Guia.mantener(true) } })
                        DropdownMenuItem(text = { Text("Guardar copia de mi música") }, leadingIcon = { Icon(Icons.Filled.Backup, null) }, onClick = { menu = false; guardarCopia.launch(null) })
                        DropdownMenuItem(text = { Text("Restaurar copia (.rrlib)") }, leadingIcon = { Icon(Icons.Filled.Restore, null) }, onClick = { menu = false; copias.launch(arrayOf("*/*")) })
                    }
                }
            }
            val esp by produceState(0L to 0L, b.pistas.size) { value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { b.espacio() } }
            Text("${b.pistas.size} canciones · ${mb(esp.first)} usados" + (if (esp.second > 0) " de ${mb(esp.second)} disponibles" else ""),
                color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall)
            (b.progreso ?: Guia.progreso.ifEmpty { null })?.let {
                Text(it, color = Amarillo, modifier = Modifier.padding(top = 8.dp)); LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 6.dp), color = Amarillo)
            }
            if (aviso.isNotEmpty()) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(aviso, color = LocalExtra.current.tenue, modifier = Modifier.weight(1f))
                IconButton(onClick = { aviso = "" }) { Icon(Icons.Filled.Close, "Cerrar aviso", Modifier.size(18.dp)) }
            }
            OutlinedTextField(value = buscar, onValueChange = { buscar = it }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Buscar canción, artista o álbum", maxLines = 1) }, shape = RoundedCornerShape(16.dp),
                trailingIcon = { if (buscar.isNotEmpty()) IconButton(onClick = { buscar = "" }) { Icon(Icons.Filled.Close, "Borrar búsqueda") } })
            Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Coleccion.VISTAS.forEach { (k, n) ->
                    FilterChip(selected = vista == k, onClick = { vista = k; p.set("libVista", k) }, label = { Text(n, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Amarillo, selectedLabelColor = Tinta), shape = RoundedCornerShape(12.dp))
                }
            }
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    TextButton(onClick = { menuOrden = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                        Text(Coleccion.ORDENES.toMap()[orden] ?: "Orden", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(expanded = menuOrden, onDismissRequest = { menuOrden = false }) {
                        Coleccion.ORDENES.forEach { (k, n) -> DropdownMenuItem(text = { Text(if (orden == k) "✓  $n" else n) }, onClick = { orden = k; p.set("libOrden", k); menuOrden = false }) }
                    }
                }
                TextButton(onClick = { verFiltros = true }) {
                    Icon(Icons.Filled.FilterList, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(if (filtros.activos > 0) "Filtros (${filtros.activos})" else "Filtros")
                }
                if (filtros.activos > 0) IconButton(onClick = { filtros = Coleccion.Filtros() }) { Icon(Icons.Filled.Close, "Quitar filtros", Modifier.size(18.dp)) }
            }
        }
        if (b.pistas.isEmpty()) item {
            Tarjeta(Modifier.padding(top = 8.dp)) {
                Titulo("Tu música va aquí")
                Text("Elige la carpeta donde guardas tu música: la app busca en todas sus subcarpetas, lee títulos, artistas y carátulas, y la deja lista para sonar sin internet.",
                    color = LocalExtra.current.tenue)
                FilledTonalButton(onClick = { agregar = true }, modifier = Modifier.padding(top = 12.dp)) { Icon(Icons.Filled.LibraryAdd, null); Spacer(Modifier.width(8.dp)); Text("Agregar música") }
            }
        } else when (vista) {
            "canciones" -> {
                if (filtradas.isEmpty()) item { Text("No hay resultados.", color = LocalExtra.current.tenue, modifier = Modifier.padding(16.dp)) }
                items(filtradas, key = { it.id }) { t -> FilaCancion(t, filtradas, acciones) }
            }
            "playlists" -> {
                val nombres = remember(b.versionListas) { b.listas.nombres }
                item {
                    FilledTonalButton(onClick = { nuevaLista = true }, modifier = Modifier.padding(vertical = 6.dp)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Crear playlist") }
                    if (nombres.isEmpty()) Text("Aún no tienes playlists. Créala aquí y añade canciones desde el menú ⋮ de cada canción.", color = LocalExtra.current.tenue, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(nombres, key = { "pl:$it" }) { n ->
                    val ids = b.listas.canciones(n)
                    val primera = ids.firstNotNullOfOrNull { id -> b.porId(id)?.takeIf { it.cover.isNotEmpty() } }
                    FilaGrupo(n, "${ids.size} ${if (ids.size == 1) "canción" else "canciones"}", primera, Icons.Filled.LibraryMusic, redonda = false,
                        play = { val orden2 = ids; r.iniciar({ it.id in orden2 }, desdeId = orden2.firstOrNull(), mezclarEsta = false) }) { carpeta = Carpeta("playlists", n) }
                }
            }
            "albumes" -> {
                val grupos = Coleccion.agrupar(filtradas, "albumes")
                val cols = when (p.str("portadas", "medium")) { "small" -> 4; "large" -> 2; else -> 3 }
                if (grupos.isEmpty()) item { Text("No hay resultados.", color = LocalExtra.current.tenue, modifier = Modifier.padding(16.dp)) }
                items(grupos.chunked(cols)) { fila ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        fila.forEach { g ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { carpeta = Carpeta("albumes", g.clave, g.subtitulo) }) {
                                Box(Modifier.fillMaxWidth().aspectRatio(1f)) { CaratulaLlena(g.portada, g.clave, Icons.Filled.MusicNote, RoundedCornerShape(14.dp)) }
                                Text(g.clave, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = if (cols >= 4) 12.sp else 14.sp, modifier = Modifier.padding(top = 6.dp))
                                Text(g.subtitulo, color = LocalExtra.current.tenue, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = if (cols >= 4) 11.sp else 12.sp)
                            }
                        }
                        repeat(cols - fila.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            else -> {
                val grupos = Coleccion.agrupar(filtradas, vista)
                if (grupos.isEmpty()) item { Text("No hay resultados.", color = LocalExtra.current.tenue, modifier = Modifier.padding(16.dp)) }
                items(grupos, key = { "$vista:${it.clave}" }) { g ->
                    val n = g.canciones.size
                    val sub = buildString {
                        append("$n ${if (n == 1) "canción" else "canciones"}")
                        if (vista == "artistas") { val a = g.canciones.map { it.album }.filter { it.isNotBlank() }.distinct().size; if (a > 0) append(" · $a ${if (a == 1) "álbum" else "álbumes"}") }
                    }
                    FilaGrupo(g.clave, sub, g.portada, if (vista == "artistas") Icons.Filled.Person else Icons.Filled.MusicNote, redonda = vista == "artistas",
                        play = { val ids = g.canciones.map { it.id }.toSet(); r.iniciar({ it.id in ids }, mezclarEsta = vista != "anios") }) { carpeta = Carpeta(vista, g.clave) }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (agregar) AgregarMusica(ocupado = b.progreso != null, cerrar = { agregar = false },
        carpeta = { agregar = false; carpetaMusica.launch(null) }, canciones = { agregar = false; elegir.launch(arrayOf("audio/*")) },
        restaurar = { agregar = false; copias.launch(arrayOf("*/*")) }, guardar = { agregar = false; guardarCopia.launch(null) }, hayMusica = b.pistas.isNotEmpty())
    if (verFiltros) DialogoFiltros(filtros, Coleccion.generos(b.pistas), { filtros = it }) { verFiltros = false }
    editando?.let { t -> EditarCancion(t, { editando = null }, { caratulaDe = t; editando = null; galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { n -> scope.launchSafe { b.editar(n) }; editando = null } }
    borrando?.let { t ->
        AlertDialog(onDismissRequest = { borrando = null }, title = { Text("¿Quitar «${t.name}»?") },
            text = { Text("Se borra la copia que guarda la app. El archivo original en tu teléfono no se toca.") },
            confirmButton = { TextButton(onClick = { scope.launchSafe { b.borrar(setOf(t.id)) }; borrando = null }) { Text("Quitar") } },
            dismissButton = { TextButton(onClick = { borrando = null }) { Text("Cancelar") } })
    }
    aPlaylist?.let { t ->
        ElegirPlaylist(t, cerrar = { aPlaylist = null }, nueva = { nuevaLista = true }) { n ->
            aviso = if (b.listas.agregar(n, t.id)) { b.guardarListas(); "Añadida a «$n»." } else "Ya estaba en «$n»."
            aPlaylist = null
        }
    }
    if (nuevaLista) PedirNombre("Nueva playlist", "", "Crear", { nuevaLista = false }) { n ->
        val err = b.listas.crear(n)
        if (err == null) { b.guardarListas(); aPlaylist?.let { t -> b.listas.agregar(n.trim().take(60), t.id); b.guardarListas(); aviso = "Añadida a «${n.trim()}»."; aPlaylist = null } }
        else aviso = err
        nuevaLista = false
    }
    renombrar?.let { viejo ->
        PedirNombre("Cambiar nombre", viejo, "Guardar", { renombrar = null }) { n ->
            val err = b.listas.renombrar(viejo, n)
            if (err == null) { b.guardarListas(); val c2 = carpeta; if (c2 != null && c2.vista == "playlists" && c2.clave == viejo) carpeta = c2.copy(clave = n.trim().take(60)) } else aviso = err
            renombrar = null
        }
    }
    borrarLista?.let { n ->
        AlertDialog(onDismissRequest = { borrarLista = null }, title = { Text("¿Borrar la playlist «$n»?") },
            text = { Text("Las canciones se quedan en tu biblioteca.") },
            confirmButton = { TextButton(onClick = { b.listas.borrar(n); b.guardarListas(); carpeta = null; borrarLista = null }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { borrarLista = null }) { Text("Cancelar") } })
    }
}

/** Lo que se puede hacer con una canción desde cualquier lista. */
private class AccionesCancion(
    val play: (List<Track>, Track) -> Unit, val editar: (Track) -> Unit, val borrar: (Track) -> Unit,
    val playlist: (Track) -> Unit, val caratula: (Track) -> Unit, val completar: (Track) -> Unit,
    val quitarDeLista: ((Track) -> Unit)? = null, val mover: ((Track, Int) -> Unit)? = null
)

@Composable
private fun CaratulaLlena(t: Track?, semilla: String, icono: ImageVector, forma: androidx.compose.ui.graphics.Shape) {
    val img = cargarImagen(t?.let { RadioApp.biblioteca.caratula(it) }, 400)
    if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(forma))
    else Relleno(semilla, icono, Modifier.fillMaxSize(), forma)
}

@Composable
private fun FilaGrupo(titulo: String, sub: String, portada: Track?, icono: ImageVector, redonda: Boolean, play: () -> Unit, abrir: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = abrir).padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp)) { CaratulaLlena(portada, titulo, icono, if (redonda) CircleShape else RoundedCornerShape(10.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = play) { Icon(Icons.Filled.PlayArrow, "Escuchar $titulo", tint = Amarillo) }
    }
}

@Composable
private fun FilaCancion(t: Track, lista: List<Track>, a: AccionesCancion) {
    var menu by remember { mutableStateOf(false) }
    val actual = RadioApp.radio.actual?.id == t.id
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { a.play(lista, t) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Caratula(RadioApp.biblioteca.caratula(t), t.album + t.artist, 50.dp, 8.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (actual) Amarillo else androidx.compose.ui.graphics.Color.Unspecified)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (t.sinPerdida) Text("FLAC/WAV", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Tinta,
                    modifier = Modifier.padding(end = 6.dp).clip(RoundedCornerShape(4.dp)).background(Amarillo).padding(horizontal = 4.dp, vertical = 1.dp))
                Text(listOf(t.artist, t.album, if (t.year > 0) t.year.toString() else "").filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (t.durMs > 0) Text(fmtDur(t.durMs), style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.tenue, modifier = Modifier.padding(start = 8.dp))
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opciones de ${t.name}") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (a.mover != null) {
                    DropdownMenuItem(text = { Text("Subir") }, leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, null) }, onClick = { menu = false; a.mover.invoke(t, -1) })
                    DropdownMenuItem(text = { Text("Bajar") }, leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, null) }, onClick = { menu = false; a.mover.invoke(t, 1) })
                }
                if (a.quitarDeLista != null) DropdownMenuItem(text = { Text("Quitar de la playlist") }, leadingIcon = { Icon(Icons.Filled.RemoveCircleOutline, null) }, onClick = { menu = false; a.quitarDeLista.invoke(t) })
                else DropdownMenuItem(text = { Text("Añadir a una playlist") }, leadingIcon = { Icon(Icons.Filled.Add, null) }, onClick = { menu = false; a.playlist(t) })
                DropdownMenuItem(text = { Text("Editar datos") }, leadingIcon = { Icon(Icons.Filled.Edit, null) }, onClick = { menu = false; a.editar(t) })
                DropdownMenuItem(text = { Text("Cambiar carátula") }, leadingIcon = { Icon(Icons.Filled.Image, null) }, onClick = { menu = false; a.caratula(t) })
                DropdownMenuItem(text = { Text("Completar con la guía") }, leadingIcon = { Icon(Icons.Filled.AutoAwesome, null) }, onClick = { menu = false; a.completar(t) })
                DropdownMenuItem(text = { Text("Quitar de la biblioteca") }, leadingIcon = { Icon(Icons.Filled.Delete, null) }, onClick = { menu = false; a.borrar(t) })
            }
        }
    }
}

@Composable
private fun DetalleCarpeta(c: Carpeta, filtradas: List<Track>, acc: AccionesCancion, cerrar: () -> Unit, abrir: (Carpeta) -> Unit, abrirRevista: (String) -> Unit,
                           renombrar: (String) -> Unit, borrarLista: (String) -> Unit) {
    val b = RadioApp.biblioteca; val r = RadioApp.radio
    val esLista = c.vista == "playlists"
    val canciones = remember(c, b.pistas, b.versionListas, filtradas) {
        if (esLista) b.listas.canciones(c.clave).mapNotNull { b.porId(it) }
        else filtradas.filter { Coleccion.clave(it, c.vista) == c.clave && (c.artista.isEmpty() || normNombre(io.github.arielegno.radio.core.Limpieza.artistaPrincipal(it.artist)) == normNombre(io.github.arielegno.radio.core.Limpieza.artistaPrincipal(c.artista))) }
    }
    val a2 = if (esLista) AccionesCancion(acc.play, acc.editar, acc.borrar, acc.playlist, acc.caratula, acc.completar,
        quitarDeLista = { t -> b.listas.quitar(c.clave, t.id); b.guardarListas() },
        mover = { t, d -> b.listas.mover(c.clave, t.id, d); b.guardarListas() }) else acc
    val portada = canciones.firstOrNull { it.cover.isNotEmpty() }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = cerrar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver a la biblioteca") }
                Text(when (c.vista) { "artistas" -> "Artista"; "albumes" -> "Álbum"; "anios" -> "Año"; "generos" -> "Género"; else -> "Playlist" },
                    color = LocalExtra.current.tenue, modifier = Modifier.weight(1f))
                if (esLista) {
                    IconButton(onClick = { renombrar(c.clave) }) { Icon(Icons.Filled.Edit, "Cambiar nombre") }
                    IconButton(onClick = { borrarLista(c.clave) }) { Icon(Icons.Filled.Delete, "Borrar playlist") }
                }
            }
            when (c.vista) {
                "artistas" -> CabeceraArtista(c.clave, canciones, abrir, abrirRevista)
                "albumes" -> CabeceraAlbum(c.clave, canciones, portada)
                else -> {
                    Text(c.clave, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 6.dp))
                    Text("${canciones.size} ${if (canciones.size == 1) "canción" else "canciones"}", color = LocalExtra.current.tenue)
                }
            }
            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { if (canciones.isNotEmpty()) acc.play(canciones, canciones.first()) }, enabled = canciones.isNotEmpty()) {
                    Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Reproducir todo")
                }
                OutlinedButton(onClick = { val ids = canciones.map { it.id }.toSet(); r.iniciar({ it.id in ids }, mezclarEsta = true) }, enabled = canciones.isNotEmpty()) {
                    Icon(Icons.Filled.Shuffle, null); Spacer(Modifier.width(6.dp)); Text("Mezclar")
                }
            }
            if (c.vista == "artistas") Text("Todas las canciones · ${canciones.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            if (esLista && canciones.isEmpty()) Text("Esta playlist está vacía. Añade canciones con el menú ⋮ de cada canción.", color = LocalExtra.current.tenue)
        }
        items(canciones, key = { it.id }) { t -> FilaCancion(t, canciones, a2) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun CabeceraAlbum(album: String, canciones: List<Track>, portada: Track?) {
    val g = Coleccion.Grupo(album, canciones)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Box(Modifier.size(130.dp)) { CaratulaLlena(portada, album, Icons.Filled.MusicNote, RoundedCornerShape(16.dp)) }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(album, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(listOf(g.artistaPrincipal, if (g.anio > 0) g.anio.toString() else "").filter { it.isNotBlank() }.joinToString(" · "), color = Amarillo, modifier = Modifier.padding(top = 4.dp))
            val min = canciones.sumOf { it.durMs } / 60_000
            Text("${canciones.size} ${if (canciones.size == 1) "canción" else "canciones"}" + if (min > 0) " · $min min" else "", color = LocalExtra.current.tenue)
        }
    }
}

@Composable
private fun CabeceraArtista(nombre: String, canciones: List<Track>, abrir: (Carpeta) -> Unit, abrirRevista: (String) -> Unit) {
    val p = RadioApp.prefs
    var fav by remember(nombre) { mutableStateOf(favArtistas()[nombre] == true) }
    val ficha by produceState<Bio.Ficha?>(null, nombre) { value = Guia.bio(nombre) }
    val conciertos by produceState<List<Concierto>>(emptyList(), nombre) { value = try { Revista.conciertosDe(nombre) } catch (e: Exception) { emptyList() } }
    val fotoWeb = cargarImagenUrl(ficha?.foto ?: "")
    val portada = canciones.firstOrNull { it.cover.isNotEmpty() }
    val fotoLocal = cargarImagen(portada?.let { RadioApp.biblioteca.caratula(it) }, 600)
    val foto = fotoWeb ?: fotoLocal
    Box(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(24.dp))) {
        if (foto != null) Image(foto, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Relleno(nombre, Icons.Filled.Person, Modifier.fillMaxSize(), RoundedCornerShape(24.dp))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC0B0C0F)))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(nombre, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val albumes = canciones.map { it.album }.filter { it.isNotBlank() }.distinct()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${canciones.size} canciones" + if (albumes.isNotEmpty()) " · ${albumes.size} ${if (albumes.size == 1) "álbum" else "álbumes"}" else "",
                    color = Color.White.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val m = favArtistas(); if (fav) m.remove(nombre) else m[nombre] = true
                    p.set("fav_artists", Json.write(m)); fav = !fav
                }, modifier = Modifier.clip(CircleShape).background(Color(0x33FFFFFF))) {
                    Icon(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (fav) "Quitar de artistas favoritos" else "Artista favorito", tint = if (fav) Rosa else Color.White)
                }
            }
        }
    }
    ficha?.let { f ->
        Tarjeta(Modifier.padding(top = 12.dp)) {
            Etiqueta("Historia")
            Text(f.texto, modifier = Modifier.padding(top = 6.dp), lineHeight = 22.sp)
            Text("Fuente: Wikipedia (CC BY-SA)", style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue, modifier = Modifier.padding(top = 8.dp))
        }
    }
    if (conciertos.isNotEmpty()) {
        Text("En la bóveda del tiempo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(conciertos, key = { it.id }) { v ->
                Column(Modifier.width(180.dp).clip(RoundedCornerShape(14.dp)).clickable { abrirRevista("#rvb=${v.id}") }) {
                    val img = cargarImagenUrl(v.miniatura())
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                        else Relleno(v.titulo, Icons.Filled.Movie, Modifier.fillMaxSize(), RoundedCornerShape(12.dp))
                    }
                    Text(v.titulo, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
    val albumes = remember(canciones) { Coleccion.agrupar(canciones, "albumes").filter { it.clave != Coleccion.SIN_ALBUM } }
    if (albumes.isNotEmpty()) {
        Text("Álbumes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(albumes, key = { it.clave }) { g ->
                Column(Modifier.width(120.dp).clip(RoundedCornerShape(14.dp)).clickable { abrir(Carpeta("albumes", g.clave, g.subtitulo)) }) {
                    Box(Modifier.size(120.dp)) { CaratulaLlena(g.portada, g.clave, Icons.Filled.MusicNote, RoundedCornerShape(14.dp)) }
                    Text(g.clave, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    if (g.anio > 0) Text("${g.anio}", color = LocalExtra.current.tenue, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DialogoFiltros(f: Coleccion.Filtros, generos: List<String>, cambiar: (Coleccion.Filtros) -> Unit, cerrar: () -> Unit) {
    Dialog(onDismissRequest = cerrar) {
        Column(Modifier.clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("Filtros", style = MaterialTheme.typography.headlineMedium)
            Etiqueta("Década", Modifier.padding(top = 14.dp, bottom = 6.dp))
            Chips(Coleccion.DECADAS, f.decada) { cambiar(f.copy(decada = it)) }
            if (generos.isNotEmpty()) {
                Etiqueta("Género", Modifier.padding(top = 14.dp, bottom = 6.dp))
                Chips(listOf("" to "Todos") + generos.map { it to it }, f.genero) { cambiar(f.copy(genero = it)) }
            }
            Etiqueta("Calidad", Modifier.padding(top = 14.dp, bottom = 6.dp))
            Chips(listOf("" to "Todas", "hq" to "Sin pérdida (FLAC, WAV)", "lossy" to "Comprimida (MP3, AAC…)"), f.calidad) { cambiar(f.copy(calidad = it)) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clickable { cambiar(f.copy(favoritas = !f.favoritas)) }) {
                Checkbox(checked = f.favoritas, onCheckedChange = { cambiar(f.copy(favoritas = it)) }); Text("Solo favoritas")
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { cambiar(Coleccion.Filtros()) }, modifier = Modifier.weight(1f)) { Text("Quitar filtros") }
                Button(onClick = cerrar, modifier = Modifier.weight(1f)) { Text("Listo") }
            }
        }
    }
}

@Composable
private fun Chips(ops: List<Pair<String, String>>, sel: String, elegir: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ops.forEach { (k, n) ->
            FilterChip(selected = sel == k, onClick = { elegir(k) }, label = { Text(n, maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Amarillo, selectedLabelColor = Tinta))
        }
    }
}

@Composable
private fun ElegirPlaylist(t: Track, cerrar: () -> Unit, nueva: () -> Unit, elegir: (String) -> Unit) {
    val nombres = RadioApp.biblioteca.listas.nombres
    AlertDialog(onDismissRequest = cerrar, title = { Text("Añadir a una playlist") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("«${t.name}»", color = LocalExtra.current.tenue, modifier = Modifier.padding(bottom = 8.dp))
                nombres.forEach { n ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { elegir(n) }.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LibraryMusic, null, tint = Amarillo); Spacer(Modifier.width(12.dp)); Text(n)
                    }
                }
                if (nombres.isEmpty()) Text("Todavía no tienes playlists.")
            }
        },
        confirmButton = { TextButton(onClick = nueva) { Text("Nueva playlist") } },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } })
}

@Composable
private fun PedirNombre(titulo: String, inicial: String, ok: String, cerrar: () -> Unit, listo: (String) -> Unit) {
    var n by remember { mutableStateOf(inicial) }
    AlertDialog(onDismissRequest = cerrar, title = { Text(titulo) },
        text = { OutlinedTextField(n, { n = it.take(60) }, label = { Text("Nombre") }, placeholder = { Text("Ej. Viaje al mar") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (n.isNotBlank()) listo(n) }) { Text(ok) } },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } })
}

/** Apartado «Agregar música»: carpetas, canciones sueltas y copias de seguridad. */
@Composable
fun AgregarMusica(ocupado: Boolean, cerrar: () -> Unit, carpeta: () -> Unit, canciones: () -> Unit, restaurar: () -> Unit, guardar: () -> Unit, hayMusica: Boolean) {
    Dialog(onDismissRequest = cerrar) {
        Column(Modifier.clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp)) {
            Text("Agregar música", style = MaterialTheme.typography.headlineMedium)
            Text("Se copia a la app y suena sin internet. La guía limpia los nombres y completa álbum, año y carátula.", color = LocalExtra.current.tenue, modifier = Modifier.padding(bottom = 12.dp))
            if (ocupado) Text("Espera a que termine lo que se está agregando.", color = Amarillo, modifier = Modifier.padding(bottom = 8.dp))
            OpcionGrande(Icons.Filled.CreateNewFolder, "Una carpeta completa", "Con todas sus subcarpetas (Música, Descargas…)", !ocupado, carpeta)
            OpcionGrande(Icons.Filled.Add, "Canciones sueltas", "Elige uno o varios archivos", !ocupado, canciones)
            Etiqueta("Copias de seguridad", Modifier.padding(top = 10.dp, bottom = 4.dp))
            OpcionGrande(Icons.Filled.Restore, "Restaurar una copia", "Archivos .rrlib de la app o de la web", !ocupado, restaurar)
            if (hayMusica) OpcionGrande(Icons.Filled.Backup, "Guardar copia de mi música", "Con audios y carátulas, en la carpeta que elijas", !ocupado, guardar)
            TextButton(onClick = cerrar, modifier = Modifier.align(Alignment.End)) { Text("Cerrar") }
        }
    }
}

@Composable
private fun OpcionGrande(icono: ImageVector, t: String, sub: String, activa: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp))
        .background(LocalExtra.current.tarjeta.copy(alpha = if (activa) 0.9f else 0.4f)).clickable(enabled = activa, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Amarillo.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(icono, null, tint = Amarillo) }
        Spacer(Modifier.width(14.dp))
        Column { Text(t, fontWeight = FontWeight.SemiBold); Text(sub, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue) }
    }
}

@Composable
private fun EditarCancion(t: Track, cerrar: () -> Unit, cambiarCaratula: () -> Unit, guardar: (Track) -> Unit) {
    var nombre by remember { mutableStateOf(t.name) }
    var artista by remember { mutableStateOf(t.artist) }
    var album by remember { mutableStateOf(t.album) }
    var anio by remember { mutableStateOf(if (t.year > 0) t.year.toString() else "") }
    var genero by remember { mutableStateOf(t.genre) }
    AlertDialog(onDismissRequest = cerrar, title = { Text("Editar canción") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Caratula(RadioApp.biblioteca.caratula(t), t.album + t.artist, 64.dp, 10.dp)
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = cambiarCaratula) { Icon(Icons.Filled.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Cambiar carátula") }
                }
                OutlinedTextField(nombre, { nombre = it.take(200) }, label = { Text("Título") }, singleLine = true)
                OutlinedTextField(artista, { artista = it.take(200) }, label = { Text("Artista") }, singleLine = true)
                OutlinedTextField(album, { album = it.take(200) }, label = { Text("Álbum") }, singleLine = true)
                OutlinedTextField(anio, { anio = it.filter(Char::isDigit).take(4) }, label = { Text("Año") }, singleLine = true)
                OutlinedTextField(genero, { genero = it.take(80) }, label = { Text("Género") }, singleLine = true)
                Text("Lo que corrijas a mano no lo vuelve a cambiar la guía. Luna usa estos datos.", style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue)
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
