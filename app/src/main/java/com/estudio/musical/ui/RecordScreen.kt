package com.estudio.musical.ui

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import com.estudio.musical.AppState
import com.estudio.musical.Pantalla
import com.estudio.musical.audio.AudioRecorder
import com.estudio.musical.audio.Metronomo
import com.estudio.musical.audio.MotorAudio
import com.estudio.musical.audio.Wav
import com.estudio.musical.data.Ajustes
import com.estudio.musical.data.Carpetas
import com.estudio.musical.data.PistaProyecto
import com.estudio.musical.data.ProyectosRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordScreen(modifier: Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ajustes = remember { Ajustes(ctx) }
    val recorder = remember { AudioRecorder(ctx) }
    val metro = remember { Metronomo() }
    val pedirPermiso = rememberPermisoMicrofono()

    var devices by remember { mutableStateOf(recorder.inputDevices()) }
    var device by remember { mutableStateOf<AudioDeviceInfo?>(null) }
    var estado by remember { mutableStateOf("listo") } // listo, cuenta, grabando
    var level by remember { mutableFloatStateOf(0f) }
    var seconds by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var files by remember { mutableStateOf(Carpetas.listaGrabaciones(ctx)) }
    var ultima by remember { mutableStateOf<File?>(null) }
    var mensaje by remember { mutableStateOf("") }

    // opciones
    var click by remember { mutableStateOf(false) }
    var cuenta by remember { mutableStateOf(false) }
    var bpm by remember { mutableFloatStateOf(ajustes.bpm.toFloat()) }
    var tiempoActual by remember { mutableIntStateOf(-1) }
    var base by remember { mutableStateOf(AppState.baseOverdub) }
    var volBase by remember { mutableFloatStateOf(0.8f) }
    var monitoreo by remember { mutableStateOf(false) }

    metro.onTiempo = { tiempoActual = it }

    DisposableEffect(Unit) {
        val am = ctx.getSystemService(AudioManager::class.java)
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) {
                devices = recorder.inputDevices()
                added.firstOrNull { it.isSource && it.type != AudioDeviceInfo.TYPE_BUILTIN_MIC }?.let { device = it }
            }
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) {
                devices = recorder.inputDevices()
                if (removed.any { it.id == device?.id }) device = null
            }
        }
        am.registerAudioDeviceCallback(cb, null)
        onDispose {
            am.unregisterAudioDeviceCallback(cb)
            if (recorder.isRecording) recorder.stop()
            metro.detener()
        }
    }

    LaunchedEffect(estado) {
        seconds = 0
        while (estado == "grabando") { delay(1000); seconds++ }
    }

    fun comenzar() = scope.launch {
        MotorAudio.detenerTodo()
        val nombreBase = name.ifBlank { "Toma " + SimpleDateFormat("dd-MM HH.mm.ss", Locale.getDefault()).format(Date()) }
        val out = Carpetas.nombreLibre(Carpetas.grabaciones(ctx), nombreBase)
        val datosBase = base?.let { f -> withContext(Dispatchers.IO) { Wav.readAt(f, MotorAudio.SR).samples } }
        metro.bpm = bpm
        ajustes.bpm = bpm.toInt()
        if (cuenta) {
            estado = "cuenta"
            metro.iniciar()
            delay((60_000 / bpm * metro.tiempos).toLong())
            if (!click) metro.detener()
        } else if (click) metro.iniciar()
        recorder.onLevel = { level = it }
        recorder.start(out, device, AudioRecorder.Opciones(
            base = datosBase, volumenBase = volBase, latenciaMs = ajustes.latenciaMs, monitoreo = monitoreo))
        ultima = out
        estado = "grabando"
    }

    fun detener() {
        recorder.stop(); metro.detener()
        estado = "listo"; level = 0f; tiempoActual = -1
        files = Carpetas.listaGrabaciones(ctx)
        mensaje = "Guardada: ${ultima?.nameWithoutExtension}"
    }

    fun agregarAlProyecto(f: File) {
        val repo = ProyectosRepo(ctx)
        val p = repo.obtener(ajustes.proyectoActual)
        if (p == null) { mensaje = "Primero crea un proyecto en la pestaña Estudio."; return }
        repo.guardar(p.copy(pistas = p.pistas + PistaProyecto(f.absolutePath, f.nameWithoutExtension)))
        mensaje = "“${f.nameWithoutExtension}” agregada al proyecto ${p.nombre}"
    }

    val ocupado = estado != "listo"

    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        SectionTitle("Entrada de audio")
        Ayuda("Conecta tu micrófono por USB tipo C o empareja uno Bluetooth; aparecerá aquí solo.")
        Row(Modifier.fillMaxWidth().clickable(enabled = !ocupado) { device = null }, verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = device == null, onClick = { device = null }, enabled = !ocupado)
            Text("Automático")
        }
        devices.forEach { d ->
            Row(Modifier.fillMaxWidth().clickable(enabled = !ocupado) { device = d }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = device?.id == d.id, onClick = { device = d }, enabled = !ocupado)
                Text(AudioRecorder.describe(d))
            }
        }
        if (device?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
            Text("⚠ Bluetooth graba a 16 kHz como máximo (calidad de llamada). Para calidad de estudio usa USB tipo C.",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        SectionTitle("Metrónomo")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(click, { click = it }, enabled = !ocupado); Spacer(Modifier.width(8.dp)); Text("Click mientras grabo")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(cuenta, { cuenta = it }, enabled = !ocupado); Spacer(Modifier.width(8.dp)); Text("Cuenta de 1 compás antes de grabar")
        }
        if (click || cuenta) {
            Deslizador("Tempo", bpm, 40f..220f, { "${it.toInt()} BPM" }) { bpm = it.toInt().toFloat() }
            Ayuda("Usa audífonos para que el click no quede en la grabación.")
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        SectionTitle("Grabar encima de una pista")
        Selector("Pista base", listOf<File?>(null) + files.filter { it != ultima || !ocupado }, base,
            { it?.nameWithoutExtension ?: "Ninguna (grabar solo)" }) { base = it; AppState.baseOverdub = it }
        if (base != null) {
            Deslizador("Volumen de la base", volBase, 0f..1f, { "${(it * 100).toInt()} %" }) { volBase = it }
            Ayuda("Escucha la base con audífonos. Compensación de latencia: ${ajustes.latenciaMs} ms (ajústala en Más → Ajustes).")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(monitoreo, { monitoreo = it }, enabled = !ocupado); Spacer(Modifier.width(8.dp))
            Text("Escucharme en los audífonos (monitoreo)")
        }
        if (monitoreo) Ayuda("Solo con audífonos, o habrá acople (pitido). Algunos teléfonos tienen algo de retraso.")

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        OutlinedTextField(name, { name = it }, enabled = !ocupado, label = { Text("Nombre de la toma (opcional)") },
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text("Nivel", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(progress = { level }, modifier = Modifier.fillMaxWidth().height(12.dp),
            color = if (level > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        if (level > 0.9f) Text("¡Saturando! Aléjate del micro o baja la ganancia.", color = MaterialTheme.colorScheme.error)

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LargeFloatingActionButton(
                onClick = { if (ocupado) detener() else pedirPermiso { comenzar() } },
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(if (ocupado) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                    contentDescription = if (ocupado) "Detener" else "Grabar")
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(when (estado) {
                    "cuenta" -> "Prepárate…"
                    "grabando" -> "Grabando  %02d:%02d".format(seconds / 60, seconds % 60)
                    else -> "WAV 48 kHz · 16 bits"
                }, style = MaterialTheme.typography.titleMedium)
                if (tiempoActual >= 0) Row {
                    repeat(metro.tiempos) { i ->
                        Text(if (i == tiempoActual) "●" else "○", style = MaterialTheme.typography.titleLarge,
                            color = if (i == 0 && i == tiempoActual) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        if (mensaje.isNotEmpty()) Text(mensaje, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        SectionTitle("Mis tomas")
        files.forEach { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(f.nameWithoutExtension)
                    Ayuda("%.1f MB".format(f.length() / 1_048_576f))
                }
                PlayButton(f)
                IconButton(onClick = { AppState.archivoAEditar = f; AppState.pantalla = Pantalla.EDITAR }) {
                    Icon(Icons.Filled.ContentCut, contentDescription = "Editar")
                }
                IconButton(onClick = { agregarAlProyecto(f) }) { Icon(Icons.Filled.PlaylistAdd, contentDescription = "Agregar al proyecto") }
                IconButton(onClick = { f.delete(); files = Carpetas.listaGrabaciones(ctx) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Borrar")
                }
            }
        }
    }
}
