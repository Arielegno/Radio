package com.estudio.musical.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estudio.musical.audio.AudioRecorder
import com.estudio.musical.audio.MotorAudio
import com.estudio.musical.audio.synth.Instrumentos
import com.estudio.musical.data.Cancion
import com.estudio.musical.data.Carpetas
import com.estudio.musical.data.Documentos
import com.estudio.musical.data.LyricsRepository
import com.estudio.musical.audio.Exportar
import com.estudio.musical.music.Acorde
import com.estudio.musical.music.Estilo
import com.estudio.musical.music.Grado
import com.estudio.musical.music.Teoria
import com.estudio.musical.music.TextoMusical
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LyricsScreen(modifier: Modifier) {
    val ctx = LocalContext.current
    val repo = remember { LyricsRepository(ctx) }
    var songs by remember { mutableStateOf(repo.all()) }
    var editing by remember { mutableStateOf<Cancion?>(null) }
    var buscar by remember { mutableStateOf("") }

    val e = editing
    if (e != null) {
        LyricEditor(modifier, e,
            onSave = { repo.save(it); songs = repo.all(); editing = null },
            onDelete = { repo.delete(e.id); songs = repo.all(); editing = null })
        return
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Encabezado("Letras", "Tus canciones, acordes y rimas", Icons.Filled.EditNote, dentroDeMargen = true)
                OutlinedTextField(buscar, { buscar = it }, label = { Text("Buscar") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            if (songs.isEmpty()) item { Text("Toca + para escribir o dictar tu primera letra.") }
            items(songs.filter { buscar.isBlank() || it.titulo.contains(buscar, true) || it.letra.contains(buscar, true) }, key = { it.id }) { s ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { editing = s },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(s.titulo.ifBlank { "Sin título" }, style = MaterialTheme.typography.titleMedium)
                        if (s.tonalidad.isNotBlank()) Text("Tono: ${s.tonalidad}", style = MaterialTheme.typography.labelMedium)
                        Text(TextoMusical.quitarAcordes(s.letra).lines().filter { it.isNotBlank() }.take(2).joinToString(" / "),
                            maxLines = 2, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        ExtendedFloatingActionButton(onClick = { editing = Cancion(titulo = "", letra = "") },
            icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Nueva canción") },
            containerColor = Marca.Magenta, contentColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}

@Composable
private fun LyricEditor(modifier: Modifier, song: Cancion, onSave: (Cancion) -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(song.titulo) }
    var body by remember { mutableStateOf(TextFieldValue(song.letra)) }
    var key by remember { mutableStateOf(song.tonalidad) }
    var modo by remember { mutableIntStateOf(0) }
    var confirmarBorrar by remember { mutableStateOf(false) }
    fun actual() = song.copy(titulo = title, letra = body.text, tonalidad = key)

    BackHandler { onSave(actual()) }

    fun insertar(texto: String, nuevaLinea: Boolean = false) {
        val pos = body.selection.start.coerceIn(0, body.text.length)
        val antes = body.text.substring(0, pos)
        val sep = if (nuevaLinea && antes.isNotEmpty() && !antes.endsWith("\n")) "\n" else ""
        val ins = sep + texto + if (nuevaLinea) "\n" else ""
        body = TextFieldValue(antes + ins + body.text.substring(pos), TextRange(pos + ins.length))
    }

    // Dictado con el reconocedor de voz del teléfono (puede funcionar sin internet si descargas el idioma)
    val dictate = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
                insertar(it.replaceFirstChar { c -> c.uppercase() }, nuevaLinea = true)
            }
        }
    }
    fun startDictation() = runCatching {
        dictate.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-419")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta un verso…")
        })
    }

    val tono = Teoria.parseTono(key)

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onSave(actual()) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Guardar y volver") }
            OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val pdf = Documentos.letraAPdf(ctx, actual()); Exportar.compartir(ctx, pdf)
            }) { Icon(Icons.Filled.PictureAsPdf, contentDescription = "Exportar PDF") }
            IconButton(onClick = { confirmarBorrar = true }) { Icon(Icons.Filled.Delete, contentDescription = "Borrar canción") }
        }
        Selector("Tonalidad", listOf("") + (0..11).flatMap { listOf(Teoria.nombreTono(it, true), Teoria.nombreTono(it, false)) },
            key, { it.ifBlank { "Sin definir" } }) { key = it }
        TabRow(selectedTabIndex = modo, modifier = Modifier.padding(top = 4.dp)) {
            Tab(modo == 0, { modo = 0 }, text = { Text("Escribir") })
            Tab(modo == 1, { modo = 1 }, text = { Text("Ver") })
            Tab(modo == 2, { modo = 2 }, text = { Text("Ayudas") })
        }

        when (modo) {
            0 -> {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
                    listOf("[Intro]", "[Verso]", "[Pre-coro]", "[Coro]", "[Puente]", "[Final]").forEach { tag ->
                        AssistChip(onClick = { insertar(tag, nuevaLinea = true) }, label = { Text(tag) }, modifier = Modifier.padding(end = 6.dp))
                    }
                }
                if (tono != null) Row(Modifier.horizontalScroll(rememberScrollState())) {
                    Teoria.acordes(tono.first, tono.second).map { it.substringAfter("=") }.forEach { ac ->
                        SuggestionChip(onClick = { insertar("[$ac]"); Acorde.parse(ac)?.let { tocarAcorde(it) } },
                            label = { Text(ac) }, modifier = Modifier.padding(end = 4.dp))
                    }
                } else Ayuda("Elige una tonalidad para tener sus acordes a mano.")
                OutlinedTextField(body, { body = it }, label = { Text("Letra  ([Do] pone un acorde)") },
                    modifier = Modifier.fillMaxWidth().weight(1f))
                val lineaActual = body.text.substring(0, body.selection.start.coerceIn(0, body.text.length)).substringAfterLast("\n") +
                    body.text.substring(body.selection.start.coerceIn(0, body.text.length)).substringBefore("\n")
                val (g, m) = TextoMusical.silabasVerso(lineaActual)
                if (g > 0) Ayuda("Verso actual: $m sílabas métricas ($g gramaticales)")
                Row(Modifier.padding(top = 4.dp)) {
                    Button(onClick = { startDictation() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Mic, null); Spacer(Modifier.width(6.dp)); Text("Dictar")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { onSave(actual()) }, modifier = Modifier.weight(1f)) { Text("Guardar") }
                }
            }
            1 -> VistaConAcordes(body.text, onTransponer = { s ->
                body = TextFieldValue(TextoMusical.transponer(body.text, s))
                tono?.let { key = Teoria.nombreTono((it.first + s + 12) % 12, it.second) }
            })
            2 -> AyudasLetra(song.id, body.text, tono, onInsertar = { insertar(it, nuevaLinea = true) })
        }
    }

    if (confirmarBorrar) AlertDialog(
        onDismissRequest = { confirmarBorrar = false },
        title = { Text("¿Borrar “${title.ifBlank { "Sin título" }}”?") },
        text = { Text("Se borrará la letra y sus notas de voz. No se puede deshacer.") },
        confirmButton = { TextButton(onClick = { Carpetas.notasVoz(ctx, song.id).deleteRecursively(); onDelete() }) { Text("Borrar") } },
        dismissButton = { TextButton(onClick = { confirmarBorrar = false }) { Text("Cancelar") } }
    )
}

private fun tocarAcorde(a: Acorde) = a.notas(60).forEach { MotorAudio.notaOn(Instrumentos.GUITARRA, it, 0.6f) }

@Composable
private fun ColumnScope.VistaConAcordes(texto: String, onTransponer: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text("Transponer:")
        TextButton(onClick = { onTransponer(-1) }) { Text("−½ tono") }
        TextButton(onClick = { onTransponer(1) }) { Text("+½ tono") }
    }
    val usados = TextoMusical.acordesUsados(texto)
    if (usados.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState())) {
        usados.forEach { ac ->
            SuggestionChip(onClick = { Acorde.parse(ac.substringBefore("/"))?.let { tocarAcorde(it) } }, label = { Text(ac) },
                modifier = Modifier.padding(end = 4.dp))
        }
    }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        texto.lines().forEach { linea ->
            val t = linea.trim()
            if (t.startsWith("[") && t.endsWith("]") && TextoMusical.acordesUsados(t).isEmpty()) {
                Text(t.removeSurrounding("[", "]"), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                return@forEach
            }
            val (ac, le) = TextoMusical.separarAcordes(linea)
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    if (ac.isNotBlank()) Text(ac, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    Text(le, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                }
                val m = TextoMusical.silabasVerso(le).second
                if (m > 0) Text("$m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
    Ayuda("El número a la derecha son las sílabas métricas de cada verso.")
}

@Composable
private fun ColumnScope.AyudasLetra(cancionId: String, texto: String, tono: Pair<Int, Boolean>?, onInsertar: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var palabra by remember { mutableStateOf("") }
    var rimas by remember { mutableStateOf<TextoMusical.Rimas?>(null) }
    var estilo by remember { mutableStateOf(Estilo.POP) }
    val recorder = remember { AudioRecorder(ctx) }
    val pedirPermiso = rememberPermisoMicrofono()
    var grabando by remember { mutableStateOf(false) }
    var notas by remember { mutableStateOf(Carpetas.notasVoz(ctx, cancionId).listFiles()?.sortedBy { it.name } ?: emptyList()) }
    var segundos by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) { onDispose { if (recorder.isRecording) recorder.stop() } }

    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Seccion("🎤 Notas de voz (ideas de melodía)", inicialAbierta = true) {
            Ayuda("Tararea la melodía o graba una idea; queda guardada junto a esta letra.")
            Button(onClick = {
                if (grabando) { recorder.stop(); grabando = false; notas = Carpetas.notasVoz(ctx, cancionId).listFiles()?.sortedBy { it.name } ?: emptyList() }
                else pedirPermiso {
                    val f = File(Carpetas.notasVoz(ctx, cancionId), "Idea " + SimpleDateFormat("dd-MM HH.mm", Locale.getDefault()).format(Date()) + ".wav")
                    if (recorder.start(f, null) == null) {
                        grabando = true; segundos = 0
                        scope.launch { while (grabando) { delay(1000); segundos++ } }
                    }
                }
            }) {
                Icon(if (grabando) Icons.Filled.Stop else Icons.Filled.Mic, null); Spacer(Modifier.width(4.dp))
                Text(if (grabando) "Detener (${segundos}s)" else "Grabar idea")
            }
            notas.forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayButton(f); Text(f.nameWithoutExtension, Modifier.weight(1f))
                    IconButton(onClick = { f.delete(); notas = notas - f }) { Icon(Icons.Filled.Delete, contentDescription = "Borrar nota") }
                }
            }
        }
        Seccion("🔤 Buscador de rimas", inicialAbierta = true) {
            OutlinedTextField(palabra, { palabra = it; rimas = null }, label = { Text("Palabra") }, singleLine = true,
                trailingIcon = { IconButton(onClick = { rimas = TextoMusical.rimas(palabra, TextoMusical.palabrasDe(texto)) }) { Icon(Icons.Filled.Search, "Buscar rimas") } },
                modifier = Modifier.fillMaxWidth())
            rimas?.let { r ->
                Text("Consonantes (riman igual, termina en “${TextoMusical.terminacion(palabra)}”):", fontWeight = FontWeight.Bold)
                Text(r.consonantes.joinToString(", ").ifBlank { "—" })
                Spacer(Modifier.height(6.dp))
                Text("Asonantes (mismas vocales “${TextoMusical.asonancia(palabra)}”):", fontWeight = FontWeight.Bold)
                Text(r.asonantes.joinToString(", ").ifBlank { "—" })
                Ayuda("El diccionario está en el teléfono y aprende de las palabras de tus propias letras.")
            }
        }
        Seccion("🎼 Generador de progresiones") {
            if (tono == null) Ayuda("Primero elige la tonalidad de la canción.")
            else {
                Selector("Estilo", Estilo.entries, estilo, { it.nombre }) { estilo = it }
                estilo.progresiones(tono.second).forEach { pr ->
                    val acordes = Grado.parseLista(pr).map { it.acorde(tono.first, tono.second).nombre }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(acordes.joinToString(" – "), fontWeight = FontWeight.Bold)
                            Ayuda(pr)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                for (a in Grado.parseLista(pr)) {
                                    val ac = a.acorde(tono.first, tono.second)
                                    tocarAcorde(ac); delay(700); ac.notas(60).forEach { MotorAudio.notaOff(it) }
                                }
                            }
                        }) { Icon(Icons.Filled.PlayArrow, contentDescription = "Escuchar") }
                        TextButton(onClick = { onInsertar(acordes.joinToString(" ") { "[$it]" }) }) { Text("Insertar") }
                    }
                }
            }
        }
    }
}
