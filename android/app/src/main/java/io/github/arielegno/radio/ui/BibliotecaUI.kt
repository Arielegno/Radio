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
import io.github.arielegno.radio.core.Fichas
import io.github.arielegno.radio.FichasRed
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Download
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

/** Cuántas veces abriste la página de cada artista (para «Artistas más vistos»). */
fun visitasArtistas(): Map<String, Int> = Json.obj(RadioApp.prefs.str("artVistos", "")).mapValues { (it.value as? Number)?.toInt() ?: 0 }
private fun contarVisita(nombre: String) {
    val m = visitasArtistas().toMutableMap(); m[nombre] = (m[nombre] ?: 0) + 1
    val top = m.entries.sortedByDescending { it.value }.take(60).associate { it.key to it.value }
    RadioApp.prefs.set("artVistos", Json.write(top))
}

private fun favArtistas(): MutableMap<String, Any?> = Json.obj(RadioApp.prefs.str("fav_artists", "")).toMutableMap()

@Composable
fun BibliotecaPantalla(pedirAgregar: Boolean = false, alAtender: () -> Unit = {}, abrirRevista: (String) -> Unit = {}, artistaPedido: String = "", alAbrirArtista: () -> Unit = {}) {
    val b = RadioApp.biblioteca; val r = RadioApp.radio; val p = RadioApp.prefs
    val scope = rememberCoroutineScope()
    var aviso by remember { mutableStateOf("") }
    var buscar by rememberSaveable { mutableStateOf("") }
    var vista by rememberSaveable { mutableStateOf(p.str("libVista", "artistas")) }
    var orden by rememberSaveable { mutableStateOf(p.str("libOrden", "original")) }
    var filtros by remember { mutableStateOf(Coleccion.Filtros()) }
    var carpeta by remember { mutableStateOf<Carpeta?>(null) }
    LaunchedEffect(artistaPedido) { if (artistaPedido.isNotEmpty()) { carpeta = Carpeta("artistas", artistaPedido); alAbrirArtista() } }
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
    val copiaOrdenada = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) scope.launchSafe { aviso = try { b.copiaOrdenada(uri) } catch (e: Exception) { "No se pudo hacer la copia ordenada: ${e.message ?: "error"}" } }
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

    // Agrupar miles de canciones se hace una vez por cambio (antes en cada redibujo de la lista)
    val gruposVista = remember(filtradas, vista) { if (vista == "canciones" || vista == "playlists") emptyList() else Coleccion.agrupar(filtradas, vista) }
    val listaBib = androidx.compose.foundation.lazy.rememberLazyListState()   // al volver de un artista o álbum, sigue donde estabas
    val c = carpeta
    if (c != null) {
        DetalleCarpeta(c, filtradas, acciones, cerrar = { carpeta = null }, abrir = { carpeta = it }, abrirRevista = abrirRevista,
            renombrar = { renombrar = it }, borrarLista = { borrarLista = it })
    } else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), state = listaBib) {
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
                        DropdownMenuItem(text = { Text("Copia ordenada por artista y álbum") }, leadingIcon = { Icon(Icons.Filled.Backup, null) }, onClick = { menu = false; aviso = "Elige (o crea) una carpeta: ahí se guardará tu música ordenada en Artista › Álbum, sin tocar tus carpetas originales."; copiaOrdenada.launch(null) })
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
                val nombres = b.versionListas.let { b.listas.nombres }
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
                val grupos = gruposVista
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
            "artistas" -> {
                val grupos = gruposVista
                if (grupos.isEmpty()) item { Text("No hay resultados.", color = LocalExtra.current.tenue, modifier = Modifier.padding(16.dp)) }
                items(grupos.chunked(3)) { fila ->   // sin clave: al agregar artistas la lista no salta
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        fila.forEach { g -> TarjetaArtista(g, Modifier.weight(1f)) { carpeta = Carpeta("artistas", g.clave) } }
                        repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            else -> {
                val grupos = gruposVista
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
                "albumes" -> CabeceraAlbum(c.clave, canciones, portada) { art -> abrir(Carpeta("artistas", art)) }
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
private fun CabeceraAlbum(album: String, canciones: List<Track>, portada: Track?, irAlArtista: (String) -> Unit) {
    val g = Coleccion.Grupo(album, canciones)
    val scope = rememberCoroutineScope()
    var aviso by remember { mutableStateOf("") }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launchSafe {
            aviso = "Cambiando la portada…"
            var ok = 0; canciones.forEach { if (RadioApp.biblioteca.cambiarCaratula(it, uri)) ok++ }
            aviso = if (ok > 0) "Portada cambiada en $ok ${if (ok == 1) "canción" else "canciones"}." else "No se pudo leer esa imagen."
        }
    }
    val elegir = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Box(Modifier.size(130.dp).clip(RoundedCornerShape(16.dp)).clickable { elegir() }) {
            CaratulaLlena(portada, album, Icons.Filled.MusicNote, RoundedCornerShape(16.dp))
            Box(Modifier.align(Alignment.BottomEnd).padding(6.dp).size(34.dp).clip(CircleShape).background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Edit, "Cambiar portada del álbum", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(album, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            val principal = io.github.arielegno.radio.core.Limpieza.artistaPrincipal(g.artistaPrincipal)
            Text(listOf(g.artistaPrincipal, if (g.anio > 0) g.anio.toString() else "").filter { it.isNotBlank() }.joinToString(" · "), color = Amarillo, modifier = Modifier.padding(top = 4.dp))
            if (principal.isNotBlank()) OutlinedButton(onClick = { irAlArtista(principal) }, modifier = Modifier.padding(top = 6.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Icon(Icons.Filled.Person, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Ir a $principal", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val min = canciones.sumOf { it.durMs } / 60_000
            Text("${canciones.size} ${if (canciones.size == 1) "canción" else "canciones"}" + if (min > 0) " · $min min" else "", color = LocalExtra.current.tenue)
            TextButton(onClick = elegir, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Icon(Icons.Filled.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Cambiar portada")
            }
        }
    }
    if (aviso.isNotEmpty()) Text(aviso, color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall)
}

/** Tarjeta grande de artista con su foto (como en la web). */
@Composable
fun TarjetaArtista(g: Coleccion.Grupo, modifier: Modifier, abrir: () -> Unit) {
    val foto by produceState<java.io.File?>(null, g.clave) { value = try { FichasRed.fotoArtista(g.clave) } catch (e: Exception) { null } }
    // Solo se lee una imagen por tarjeta (antes se leían dos) y del tamaño de la tarjeta
    val imgFoto = cargarImagen(foto, 360)
    val imgPortada = cargarImagen(if (foto == null) g.portada?.let { RadioApp.biblioteca.caratula(it) } else null, 300)
    val img = imgFoto ?: imgPortada
    val n = g.canciones.size
    Box(modifier.aspectRatio(0.68f).clip(RoundedCornerShape(18.dp)).clickable(onClick = abrir)) {
        if (img != null) Image(img, null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
        else Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A2E36), Color(0xFF15171B)))), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(48.dp))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color(0xE6000000))))
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(g.clave, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 19.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text("$n ${if (n == 1) "canción" else "canciones"}", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun Pildoras(items: List<String>, alTocar: ((Int) -> Unit)? = null) {
    if (items.isEmpty()) return
    Row(Modifier.padding(vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, t ->
            val m = Modifier.clip(RoundedCornerShape(999.dp)).background(if (LocalExtra.current.oscuro) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f))
                .then(if (alTocar != null) Modifier.clickable { alTocar(i) } else Modifier).padding(horizontal = 14.dp, vertical = 8.dp)
            Text(t, modifier = m, fontWeight = if (alTocar != null) FontWeight.SemiBold else FontWeight.Normal, color = if (alTocar != null) Amarillo else Color.Unspecified, maxLines = 1)
        }
    }
}

@Composable
private fun Seccion(t: String) = Text(t.uppercase(), style = MaterialTheme.typography.labelLarge, color = LocalExtra.current.tenue, letterSpacing = 1.5.sp,
    modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))

@Composable
private fun Fuente(f: String, url: String = "") {
    val t = when (f) { "nube" -> "Fuente: tu nube."; "wiki" -> "Fuente: Wikipedia (CC BY-SA)."; "ia" -> "Escrito con inteligencia artificial: puede tener errores."; "tuya" -> "Biografía escrita por ti."; else -> "" }
    if (t.isEmpty()) return
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
        Text(t, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue)
        if (url.isNotEmpty()) TextButton(onClick = { try { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) {} }) { Text("ver artículo", fontSize = 12.sp) }
    }
}

/** Ficha de integrante o festival (pantalla completa, con foto arriba). */
private data class FichaAbierta(val tipo: String, val nombre: String, val banda: String, val obj: Map<String, Any?> = emptyMap(), val id: String = "", val dato: String = "")

@Composable
private fun FichaPantalla(f: FichaAbierta, abrirRevista: (String) -> Unit, cerrar: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var version by remember { mutableStateOf(0) }
    val claveFoto = if (f.tipo == "miembro") f.banda + "-" + f.nombre else Fichas.festivalLimpio(f.nombre)
    val miembro by produceState<Fichas.Miembro?>(null, f) { if (f.tipo == "miembro") value = FichasRed.miembro(f.nombre, f.banda, f.obj) }
    val fest by produceState<Fichas.FichaFestival?>(null, f) { if (f.tipo == "festival") value = FichasRed.festival(f.nombre, f.id) }
    val conciertos by produceState<List<Concierto>>(emptyList(), f) { if (f.tipo == "festival") value = try { Revista.conciertosDeFestival(Fichas.festivalLimpio(f.nombre)) } catch (e: Exception) { emptyList() } }
    val propia = remember(version) { FichasRed.fotoPropia(f.tipo, claveFoto).takeIf { it.exists() } }
    val imgPropia = cargarImagen(propia, 900)
    val urlWeb = miembro?.imagen ?: fest?.imagen ?: ""
    val imgWeb = cargarImagenUrl(if (propia == null) urlWeb else "")
    val img = imgPropia ?: imgWeb
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launchSafe { if (FichasRed.guardarFotoPropia(f.tipo, claveFoto, uri)) version++ }
    }
    val descargar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri: Uri? ->
        if (uri != null) scope.launchSafe {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val bytes = propia?.readBytes() ?: urlWeb.takeIf { it.isNotEmpty() }?.let { u ->
                    val tmp = java.io.File(ctx.cacheDir, "ficha.jpg"); if (FichasRed.bajar(u, tmp)) tmp.readBytes() else null
                }
                if (bytes != null) ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            }
        }
    }
    Dialog(onDismissRequest = cerrar, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                if (img != null) Image(img, null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
                else Relleno(f.nombre, if (f.tipo == "miembro") Icons.Filled.Person else Icons.Filled.MusicNote, Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to MaterialTheme.colorScheme.background)))
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonRedondo(Icons.Filled.Edit, "Poner tu foto") { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    if (img != null) BotonRedondo(Icons.Filled.Download, "Descargar foto") { descargar.launch(Fichas.slug(f.nombre) + ".jpg") }
                    Spacer(Modifier.weight(1f))
                    BotonRedondo(Icons.Filled.Close, "Cerrar", cerrar)
                }
                Text(f.nombre, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
                if (f.tipo == "miembro") {
                    val m = miembro
                    if (m == null) Text("Buscando información…", color = LocalExtra.current.tenue, modifier = Modifier.padding(vertical = 12.dp))
                    else {
                        Pildoras(m.pills)
                        if (m.bio.isNotEmpty()) { Seccion("Biografía"); Text(m.bio, lineHeight = 24.sp) }
                        else Text("Aún no hay información de este integrante. Puedes escribirla en tu nube o configurar una clave de IA.", color = LocalExtra.current.tenue, modifier = Modifier.padding(vertical = 12.dp))
                        if (m.proyectos.isNotEmpty()) { Seccion("Otros proyectos"); Pildoras(m.proyectos) }
                        if (m.curiosidad.isNotEmpty()) { Seccion("Curiosidad"); Text(m.curiosidad, lineHeight = 24.sp) }
                        Fuente(m.fuente, m.url)
                    }
                } else {
                    val fe = fest
                    if (fe == null) Text("Buscando información…", color = LocalExtra.current.tenue, modifier = Modifier.padding(vertical = 12.dp))
                    else {
                        Pildoras(fe.pills)
                        if (f.banda.isNotEmpty()) {
                            Seccion("${f.banda} en este festival")
                            val anio = Fichas.festivalAnio(f.nombre)
                            Text(f.dato.ifEmpty { if (anio.isNotEmpty()) "Participó en la edición de $anio." else "Participó en este festival." }, lineHeight = 24.sp)
                        }
                        if (fe.historia.isNotEmpty()) { Seccion("Historia"); Text(fe.historia, lineHeight = 24.sp) }
                        else Text("Aún no hay historia de este festival. Puedes agregarla en tu nube (festivales/) o configurar una clave de IA.", color = LocalExtra.current.tenue, modifier = Modifier.padding(vertical = 12.dp))
                        if (fe.ediciones.isNotEmpty()) { Seccion("Ediciones relevantes"); fe.ediciones.forEach { Text("• $it", modifier = Modifier.padding(vertical = 3.dp), lineHeight = 22.sp) } }
                        if (fe.participantes.isNotEmpty()) { Seccion("Quiénes han participado"); Pildoras(fe.participantes) }
                        if (conciertos.isNotEmpty()) {
                            Seccion("En la bóveda del tiempo")
                            conciertos.forEach { v -> TextButton(onClick = { cerrar(); abrirRevista("#rvb=${v.id}") }) { Icon(Icons.Filled.Movie, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(v.titulo) } }
                        }
                        Fuente(fe.fuente, fe.url)
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun BotonRedondo(icono: ImageVector, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0x66000000)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icono, desc, tint = Color.White)
    }
}

@Composable
private fun CabeceraArtista(nombre: String, canciones: List<Track>, abrir: (Carpeta) -> Unit, abrirRevista: (String) -> Unit) {
    val p = RadioApp.prefs
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var fav by remember(nombre) { mutableStateOf(favArtistas()[nombre] == true) }
    var version by remember { mutableStateOf(0) }
    var editar by remember { mutableStateOf(false) }
    var ficha by remember { mutableStateOf<FichaAbierta?>(null) }
    val doc by produceState<Fichas.Artista?>(null, nombre, version) { value = try { FichasRed.artista(nombre) } catch (e: Exception) { null } }
    var cargado by remember(nombre, version) { mutableStateOf(false) }
    LaunchedEffect(doc) { if (doc != null) cargado = true }
    LaunchedEffect(nombre, version) { kotlinx.coroutines.delay(12_000); cargado = true }
    val conciertos by produceState<List<Concierto>>(emptyList(), nombre) { value = try { Revista.conciertosDe(nombre) } catch (e: Exception) { emptyList() } }
    val perfil by produceState<Revista.PerfilArtista?>(null, nombre) { value = try { Revista.perfilArtista(nombre) } catch (e: Exception) { null } }
    val posters by produceState<List<Revista.PosterMini>>(emptyList(), nombre) { value = try { Revista.postersDe(nombre) } catch (e: Exception) { emptyList() } }
    LaunchedEffect(nombre) { contarVisita(nombre) }
    val foto by produceState<java.io.File?>(null, nombre, version) { value = try { FichasRed.fotoArtista(nombre) } catch (e: Exception) { null } }
    val portada = canciones.firstOrNull { it.cover.isNotEmpty() }
    val img = cargarImagen(foto, 900) ?: cargarImagen(portada?.let { RadioApp.biblioteca.caratula(it) }, 600)
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launchSafe { if (FichasRed.guardarFotoPropia("artista", nombre, uri)) version++ }
    }
    val albumes = remember(canciones) { canciones.map { it.album }.filter { it.isNotBlank() }.distinct() }
    val generosLocales = remember(canciones) { canciones.map { Coleccion.generoPrincipal(it) }.filter { it != Coleccion.OTROS_GENEROS }.distinct() }
    val generos = (generosLocales + (doc?.generos ?: emptyList())).distinctBy { normNombre(it) }.take(4)
    Column(Modifier.fillMaxWidth()) {

    Box(Modifier.fillMaxWidth().aspectRatio(0.92f).clip(RoundedCornerShape(24.dp))) {
        if (img != null) Image(img, null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
        else Relleno(nombre, Icons.Filled.Person, Modifier.fillMaxSize(), RoundedCornerShape(24.dp))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.25f to Color.Transparent, 1f to Color(0xE60B0C0F))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(nombre.uppercase(), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Text("${canciones.size} ${if (canciones.size == 1) "canción" else "canciones"}" + if (albumes.isNotEmpty()) " · ${albumes.size} ${if (albumes.size == 1) "álbum" else "álbumes"}" else "",
                    color = Color.White.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
                BotonRedondo(Icons.Filled.Storefront, "Buscar en Bandcamp") {
                    val url = doc?.bandcamp?.ifEmpty { null } ?: ("https://bandcamp.com/search?q=" + java.net.URLEncoder.encode(nombre, "UTF-8"))
                    try { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) {}
                }
                Spacer(Modifier.width(8.dp))
                BotonRedondo(Icons.Filled.Edit, "Editar artista") { editar = true }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0x66000000)).clickable {
                    val m = favArtistas(); if (fav) m.remove(nombre) else m[nombre] = true
                    p.set("fav_artists", Json.write(m)); fav = !fav
                }, contentAlignment = Alignment.Center) {
                    Icon(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (fav) "Quitar de artistas favoritos" else "Artista favorito", tint = if (fav) Rosa else Color.White)
                }
            }
            if (generos.isNotEmpty()) Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                generos.forEach { gn -> Text(gn, color = Color.White, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0x55FFFFFF)).padding(horizontal = 12.dp, vertical = 6.dp)) }
            }
        }
    }

    val abrirUrl = { u: String -> try { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(u))) } catch (e: Exception) {} }
    val enlaces = perfil?.enlaces?.takeIf { it.isNotEmpty() } ?: run {
        val q = java.net.URLEncoder.encode(nombre, "UTF-8")
        listOf("Spotify" to "https://open.spotify.com/search/$q", "YouTube" to "https://www.youtube.com/results?search_query=$q")
    }
    Row(Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        enlaces.forEach { (n, u) -> OutlinedButton(onClick = { abrirUrl(u) }) { Text("$n ↗") } }
        perfil?.let { pf -> FilledTonalButton(onClick = { abrirRevista("#rva=${pf.id}") }) { Text("Perfil en la Revista") } }
    }
    if (posters.isNotEmpty()) {
        Seccion("Pósters")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(posters, key = { "ps:" + it.id }) { ps ->
                Column(Modifier.width(120.dp).clip(RoundedCornerShape(12.dp)).clickable { abrirRevista("#rvp=${ps.id}") }) {
                    val im = cargarImagenUrl(ps.imagen)
                    Box(Modifier.fillMaxWidth().aspectRatio(0.7f)) {
                        if (im != null) Image(im, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
                        else Relleno(ps.titulo, Icons.Filled.Image, Modifier.fillMaxSize(), RoundedCornerShape(10.dp))
                    }
                    Text(ps.titulo, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    if (ps.anio > 0) Text(ps.anio.toString(), fontSize = 12.sp, color = LocalExtra.current.tenue)
                }
            }
        }
    }

    if (conciertos.isNotEmpty()) {
        Seccion("En la bóveda del tiempo")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(conciertos, key = { it.id }) { v ->
                Column(Modifier.width(180.dp).clip(RoundedCornerShape(14.dp)).clickable { abrirRevista("#rvb=${v.id}") }) {
                    val im = cargarImagenUrl(v.miniatura())
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                        if (im != null) Image(im, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                        else Relleno(v.titulo, Icons.Filled.Movie, Modifier.fillMaxSize(), RoundedCornerShape(12.dp))
                    }
                    Text(v.titulo, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }

    val d = doc
    if (d == null) {
        Text(if (cargado) (if (FichasRed.hayIA() || FichasRed.nubeBase().isNotEmpty()) "No se encontró información de este artista." else "Configura una clave de IA en Ajustes (o tu nube) para ver la historia completa.")
            else "Cargando historia y detalles…", color = LocalExtra.current.tenue, modifier = Modifier.padding(vertical = 16.dp))
    } else {
        if (d.bio.isNotEmpty()) Text(d.bio, modifier = Modifier.padding(top = 16.dp), lineHeight = 25.sp, fontSize = 16.sp)
        val historia = listOfNotNull(d.formacion.takeIf { it.isNotEmpty() }?.let { "Formación: $it" }, d.separacion.takeIf { it.isNotEmpty() }?.let { "Separación: $it" })
        if (historia.isNotEmpty()) { Seccion("Historia"); Pildoras(historia) }
        if (d.miembros.isNotEmpty()) {
            Seccion("Miembros")
            Pildoras(d.miembros.map { it.nombre }) { i -> ficha = FichaAbierta("miembro", d.miembros[i].nombre, nombre, d.miembros[i].datos) }
            Text("Toca un integrante para ver su historia.", style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue)
        }
        val disco = listOfNotNull(d.debut.takeIf { it.isNotEmpty() }?.let { "Debut: $it" }, d.obraMaestra.takeIf { it.isNotEmpty() }?.let { "Obra maestra: $it" })
        if (disco.isNotEmpty()) { Seccion("Discografía clave"); Pildoras(disco) }
        if (d.festivales.isNotEmpty()) {
            Seccion("Festivales")
            Pildoras(d.festivales.map { it.nombre }) { i -> val fe = d.festivales[i]; ficha = FichaAbierta("festival", fe.nombre, nombre, id = fe.id, dato = fe.dato) }
            Text("Toca un festival para ver su historia y quiénes han tocado.", style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue)
        }
        if (d.curiosidades.isNotEmpty()) { Seccion("Curiosidades"); d.curiosidades.forEach { Text("• $it", modifier = Modifier.padding(vertical = 3.dp), lineHeight = 22.sp) } }
        Fuente(d.fuente)
    }

    SeccionAlbumes(canciones, abrir)

    if (editar) EditarArtista(nombre, cerrar = { editar = false }, foto = { editar = false; galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        quitarFoto = { FichasRed.fotoPropia("artista", nombre).delete(); p.remove("artimg_" + Fichas.slug(nombre)); version++; editar = false }) { bio ->
        FichasRed.guardarBioPropia(nombre, bio); version++; editar = false
    }
    ficha?.let { f -> FichaPantalla(f, abrirRevista) { ficha = null } }
    }
}

@Composable
private fun SeccionAlbumes(canciones: List<Track>, abrir: (Carpeta) -> Unit) {
    val p = RadioApp.prefs
    var orden by remember { mutableStateOf(p.str("albSort", "new")) }
    var lista by remember { mutableStateOf(p.str("albVista", "grid") == "list") }
    val albumes = remember(canciones, orden) {
        val g = Coleccion.agrupar(canciones, "albumes").filter { it.clave != Coleccion.SIN_ALBUM }
        when (orden) { "old" -> g.sortedBy { if (it.anio == 0) 9999 else it.anio }; "az" -> g.sortedBy { normNombre(it.clave) }; else -> g.sortedByDescending { it.anio } }
    }
    if (albumes.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)) {
        Text("ÁLBUMES · ${albumes.size}", style = MaterialTheme.typography.labelLarge, color = LocalExtra.current.tenue, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
        listOf("new" to "Nuevo", "old" to "Viejo", "az" to "A-Z").forEach { (k, n) ->
            TextButton(onClick = { orden = k; p.set("albSort", k) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
                Text(n, color = if (orden == k) Amarillo else LocalExtra.current.tenue, fontWeight = if (orden == k) FontWeight.Bold else FontWeight.Normal)
            }
        }
        IconButton(onClick = { lista = !lista; p.set("albVista", if (lista) "list" else "grid") }) {
            Icon(if (lista) Icons.Filled.GridView else Icons.AutoMirrored.Filled.List, if (lista) "Ver en cuadrícula" else "Ver en lista")
        }
    }
    if (lista) albumes.forEach { g ->
        FilaGrupo(g.clave, listOf(if (g.anio > 0) g.anio.toString() else "", "${g.canciones.size} ${if (g.canciones.size == 1) "canción" else "canciones"}").filter { it.isNotEmpty() }.joinToString(" · "),
            g.portada, Icons.Filled.MusicNote, redonda = false, play = { val ids = g.canciones.map { it.id }.toSet(); RadioApp.radio.iniciar({ it.id in ids }, mezclarEsta = false) }) { abrir(Carpeta("albumes", g.clave, g.subtitulo)) }
    } else albumes.chunked(2).forEach { fila ->
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            fila.forEach { g ->
                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(18.dp)).clickable { abrir(Carpeta("albumes", g.clave, g.subtitulo)) }) {
                    CaratulaLlena(g.portada, g.clave, Icons.Filled.MusicNote, RoundedCornerShape(18.dp))
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xD9000000))))
                    Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                        Text(g.clave, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(listOf(if (g.anio > 0) g.anio.toString() else "", "${g.canciones.size} ${if (g.canciones.size == 1) "canción" else "canciones"}").filter { it.isNotEmpty() }.joinToString(" · "),
                            color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
            if (fila.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun EditarArtista(nombre: String, cerrar: () -> Unit, foto: () -> Unit, quitarFoto: () -> Unit, guardar: (String) -> Unit) {
    var bio by remember { mutableStateOf(FichasRed.bioPropia(nombre)) }
    AlertDialog(onDismissRequest = cerrar, title = { Text("Editar artista") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = foto) { Icon(Icons.Filled.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Cambiar foto") }
                    if (FichasRed.fotoPropia("artista", nombre).exists()) TextButton(onClick = quitarFoto) { Text("Quitar foto") }
                }
                OutlinedTextField(bio, { bio = it.take(3000) }, label = { Text("Tu biografía de $nombre") }, minLines = 5,
                    placeholder = { Text("Si la escribes, se muestra en lugar de la que trae la nube o la IA.") })
                Text("Déjala vacía para usar la biografía automática.", style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue)
            }
        },
        confirmButton = { TextButton(onClick = { guardar(bio) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } })
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
