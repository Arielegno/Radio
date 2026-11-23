package com.estudio.musical.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.estudio.musical.AppState
import com.estudio.musical.Pantalla
import com.estudio.musical.audio.AudioData
import com.estudio.musical.audio.Exportar
import com.estudio.musical.audio.MotorAudio
import com.estudio.musical.audio.Wav
import com.estudio.musical.data.*
import com.estudio.musical.music.Mezclador
import com.estudio.musical.music.PistaAudio
import com.estudio.musical.music.Teoria
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun StudioScreen(modifier: Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ProyectosRepo(ctx) }
    val ajustes = remember { Ajustes(ctx) }
    var proyectos by remember { mutableStateOf(repo.todos()) }
    var proyecto by remember { mutableStateOf(repo.obtener(ajustes.proyectoActual) ?: proyectos.firstOrNull()) }
    var creando by remember { mutableStateOf(false) }
    var dialogoNuevo by remember { mutableStateOf(false) }
    var dialogoAgregar by remember { mutableStateOf(false) }
    var ocupado by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var mezclaArchivo by remember { mutableStateOf<File?>(null) }

    fun guardar(p: Proyecto) { repo.guardar(p); proyecto = p; ajustes.proyectoActual = p.id; proyectos = repo.todos(); mezclaArchivo = null }

    val p = proyecto
    if (creando && p != null) {
        TrackCreatorScreen(modifier, p, onCerrar = { nuevas ->
            creando = false
            if (nuevas.isNotEmpty()) { guardar(p.copy(pistas = p.pistas + nuevas)); mensaje = "Se agregaron ${nuevas.size} pistas" }
        })
        return
    }

    /** Mezcla todas las pistas en un WAV estéreo */
    suspend fun renderMezcla(destino: File): File = withContext(Dispatchers.Default) {
        val pistas = p!!.pistas.mapNotNull { t ->
            val f = File(t.archivo)
            if (!f.exists()) null
            else PistaAudio(Wav.readAt(f, MotorAudio.SR).samples, t.volumen, t.paneo, t.inicioMs, t.silencio, t.solo)
        }
        val (l, r) = Mezclador.mezclar(pistas, MotorAudio.SR)
        Wav.writeStereo(destino, l, r, MotorAudio.SR); destino
    }

    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Encabezado("Estudio", "Proyectos multipista y mezcla", Icons.Filled.LibraryMusic, dentroDeMargen = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Selector("Proyecto", proyectos, p, { it.nombre }, Modifier.weight(1f)) {
                proyecto = it; ajustes.proyectoActual = it.id; mezclaArchivo = null
            }
            IconButton(onClick = { dialogoNuevo = true }) { Icon(Icons.Filled.Add, contentDescription = "Nuevo proyecto") }
        }
        if (p == null) {
            Spacer(Modifier.height(16.dp))
            Ayuda("Un proyecto junta varias pistas (voz, guitarra, batería…) para mezclarlas en una canción.")
            Button(onClick = { dialogoNuevo = true }) { Text("Crear mi primer proyecto") }
        } else {
            Ayuda(listOfNotNull(p.tonalidad.takeIf { it.isNotBlank() }, "${p.bpm} BPM", "${p.pistas.size} pistas").joinToString(" · "))
            if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp))
            if (mensaje.isNotEmpty()) Aviso(mensaje)

            Row(Modifier.padding(vertical = 8.dp)) {
                Button(onClick = { creando = true }, colors = ButtonDefaults.buttonColors(containerColor = Marca.Magenta, contentColor = androidx.compose.ui.graphics.Color.White)) { Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(4.dp)); Text("Crear pista") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { dialogoAgregar = true }) { Icon(Icons.Filled.PlaylistAdd, null); Spacer(Modifier.width(4.dp)); Text("Agregar toma") }
            }

            p.pistas.forEachIndexed { i, t ->
                PistaCard(t,
                    onCambio = { nueva -> guardar(p.copy(pistas = p.pistas.toMutableList().also { it[i] = nueva })) },
                    onBorrar = { guardar(p.copy(pistas = p.pistas.filterIndexed { k, _ -> k != i })) })
            }
            if (p.pistas.isEmpty()) Ayuda("Sin pistas todavía. Crea una pista instrumental o agrega tus tomas grabadas.")

            if (p.pistas.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                SectionTitle("Mezcla")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(enabled = !ocupado, onClick = {
                        ocupado = true
                        scope.launch {
                            mezclaArchivo = renderMezcla(File(ctx.cacheDir, "mezcla.wav"))
                            ocupado = false; mensaje = "Mezcla lista: toca ▶"
                        }
                    }) { Text("Preparar mezcla") }
                    PlayButton(mezclaArchivo)
                }
                Row {
                    OutlinedButton(enabled = !ocupado, onClick = {
                        ocupado = true
                        scope.launch {
                            val f = renderMezcla(Carpetas.nombreLibre(Carpetas.grabaciones(ctx), "Base - ${p.nombre}"))
                            AppState.baseOverdub = f
                            ocupado = false
                            AppState.pantalla = Pantalla.GRABAR
                        }
                    }) { Icon(Icons.Filled.Mic, null); Spacer(Modifier.width(4.dp)); Text("Grabar encima") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(enabled = !ocupado, onClick = {
                        ocupado = true
                        scope.launch {
                            val wav = renderMezcla(Carpetas.nombreLibre(Carpetas.exportados(ctx), p.nombre))
                            val m4a = withContext(Dispatchers.IO) {
                                File(wav.parentFile, wav.nameWithoutExtension + ".m4a").also { Exportar.wavAM4a(wav, it) }
                            }
                            ocupado = false
                            mensaje = "Exportado: ${wav.name} y ${m4a.name} (Android/data/…/Music/exportados)"
                            Exportar.compartir(ctx, m4a)
                        }
                    }) { Icon(Icons.Filled.Share, null); Spacer(Modifier.width(4.dp)); Text("Exportar") }
                }
                Ayuda("Exportar crea un WAV (máxima calidad) y un M4A liviano para compartir.")
            }
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = {
                repo.borrar(p.id); proyectos = repo.todos(); proyecto = proyectos.firstOrNull()
                ajustes.proyectoActual = proyecto?.id
            }) { Text("Borrar este proyecto (las grabaciones se conservan)", color = MaterialTheme.colorScheme.error) }
        }
    }

    if (dialogoNuevo) NuevoProyectoDialog(onCancelar = { dialogoNuevo = false }) { nuevo ->
        dialogoNuevo = false; guardar(nuevo)
    }
    if (dialogoAgregar && p != null) {
        val tomas = remember { Carpetas.listaGrabaciones(ctx) }
        AlertDialog(
            onDismissRequest = { dialogoAgregar = false },
            title = { Text("Agregar toma al proyecto") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (tomas.isEmpty()) Ayuda("No hay grabaciones.")
                    tomas.forEach { f ->
                        TextButton(onClick = {
                            guardar(p.copy(pistas = p.pistas + PistaProyecto(f.absolutePath, f.nameWithoutExtension)))
                            dialogoAgregar = false
                        }) { Text(f.nameWithoutExtension) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialogoAgregar = false }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun PistaCard(t: PistaProyecto, onCambio: (PistaProyecto) -> Unit, onBorrar: () -> Unit) {
    var vol by remember(t) { mutableFloatStateOf(t.volumen) }
    var pan by remember(t) { mutableFloatStateOf(t.paneo) }
    var ini by remember(t) { mutableFloatStateOf(t.inicioMs / 1000f) }
    var abierta by remember { mutableStateOf(false) }
    val existe = File(t.archivo).exists()
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayButton(File(t.archivo).takeIf { existe })
                Text(t.nombre + if (!existe) " (archivo no encontrado)" else "", fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f), maxLines = 1)
                FilterChip(t.silencio, { onCambio(t.copy(silencio = !t.silencio)) }, label = { Text("M") })
                Spacer(Modifier.width(4.dp))
                FilterChip(t.solo, { onCambio(t.copy(solo = !t.solo)) }, label = { Text("S") })
                IconButton(onClick = { abierta = !abierta }) { Icon(Icons.Filled.Tune, contentDescription = "Ajustes de pista") }
            }
            Slider(vol, { vol = it }, valueRange = 0f..1.5f, onValueChangeFinished = { onCambio(t.copy(volumen = vol)) })
            if (abierta) {
                Text("Paneo: " + when { pan < -0.05f -> "izquierda ${(-pan * 100).toInt()} %"; pan > 0.05f -> "derecha ${(pan * 100).toInt()} %"; else -> "centro" })
                Slider(pan, { pan = it }, valueRange = -1f..1f, onValueChangeFinished = { onCambio(t.copy(paneo = pan)) })
                Text("Empieza en: %.2f s".format(ini))
                Slider(ini, { ini = it }, valueRange = 0f..30f, onValueChangeFinished = { onCambio(t.copy(inicioMs = (ini * 1000).toInt())) })
                Ayuda("Mueve la pista en el tiempo si quedó adelantada o atrasada.")
                TextButton(onClick = onBorrar) { Text("Quitar del proyecto", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun NuevoProyectoDialog(onCancelar: () -> Unit, onCrear: (Proyecto) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var tonica by remember { mutableIntStateOf(0) }
    var mayor by remember { mutableStateOf(true) }
    var bpm by remember { mutableFloatStateOf(100f) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nuevo proyecto") },
        text = {
            Column {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre de la canción") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Selector("Tónica", (0..11).toList(), tonica, { Teoria.NOTAS[it] }) { tonica = it }
                Row {
                    FilterChip(mayor, { mayor = true }, label = { Text("Mayor") }); Spacer(Modifier.width(8.dp))
                    FilterChip(!mayor, { mayor = false }, label = { Text("Menor") })
                }
                Deslizador("Tempo", bpm, 50f..200f, { "${it.toInt()} BPM" }) { bpm = it.toInt().toFloat() }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCrear(Proyecto(nombre = nombre.ifBlank { "Mi canción" }, tonalidad = Teoria.nombreTono(tonica, mayor), bpm = bpm.toInt())) }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
