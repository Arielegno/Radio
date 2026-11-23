package com.estudio.musical.ui

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

private fun iconoEntrada(d: AudioDeviceInfo?): ImageVector = when (d?.type) {
    null -> Icons.Filled.AutoMode
    AudioDeviceInfo.TYPE_BUILTIN_MIC -> Icons.Filled.Smartphone
    AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_ACCESSORY -> Icons.Filled.Usb
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> Icons.Filled.Bluetooth
    AudioDeviceInfo.TYPE_WIRED_HEADSET -> Icons.Filled.Headset
    else -> Icons.Filled.Mic
}

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
    var error by remember { mutableStateOf(false) }
    var prueba by remember { mutableStateOf("") }
    var probando by remember { mutableStateOf(false) }

    var click by remember { mutableStateOf(false) }
    var cuenta by remember { mutableStateOf(false) }
    var bpm by remember { mutableFloatStateOf(ajustes.bpm.toFloat()) }
    var tiempoActual by remember { mutableIntStateOf(-1) }
    var base by remember { mutableStateOf(AppState.baseOverdub) }
    var volBase by remember { mutableFloatStateOf(0.8f) }
    var monitoreo by remember { mutableStateOf(false) }

    metro.onTiempo = { tiempoActual = it }
    recorder.onError = { msg ->
        if (recorder.isRecording) recorder.stop()
        metro.detener(); estado = "listo"; level = 0f; tiempoActual = -1
        files = Carpetas.listaGrabaciones(ctx)
        mensaje = msg; error = true
    }
    recorder.onAviso = { msg -> mensaje = msg; error = true }

    DisposableEffect(Unit) {
        val am = ctx.getSystemService(AudioManager::class.java)
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) {
                devices = recorder.inputDevices()
                added.firstOrNull { it.isSource && it.type != AudioDeviceInfo.TYPE_BUILTIN_MIC }?.let {
                    device = it; mensaje = "Micrófono conectado: ${AudioRecorder.describe(it)}"
                }
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
        mensaje = ""; error = false
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
        val problema = withContext(Dispatchers.IO) {
            recorder.start(out, device, AudioRecorder.Opciones(
                base = datosBase, volumenBase = volBase, latenciaMs = ajustes.latenciaMs, monitoreo = monitoreo))
        }
        if (problema != null) {
            metro.detener(); estado = "listo"; tiempoActual = -1
            out.delete(); mensaje = problema; error = true
            return@launch
        }
        ultima = out
        estado = "grabando"
    }

    fun detener() {
        recorder.stop(); metro.detener()
        estado = "listo"; level = 0f; tiempoActual = -1
        files = Carpetas.listaGrabaciones(ctx)
        name = ""
        val f = ultima
        if (f != null && f.exists() && !error) {
            mensaje = "Guardada: ${f.nameWithoutExtension} (%.1f s)".format(AudioRecorder.duracion(f)); error = false
        }
    }

    fun agregarAlProyecto(f: File) {
        val repo = ProyectosRepo(ctx)
        val p = repo.obtener(ajustes.proyectoActual)
        if (p == null) { mensaje = "Primero crea un proyecto en Estudio."; return }
        repo.guardar(p.copy(pistas = p.pistas + PistaProyecto(f.absolutePath, f.nameWithoutExtension)))
        mensaje = "Agregada al proyecto “${p.nombre}”"
    }

    val ocupado = estado != "listo"
    val resumenOpciones = listOfNotNull(
        if (click) "Metrónomo ${bpm.toInt()}" else null, if (cuenta) "Cuenta" else null,
        base?.let { "Sobre: ${it.nameWithoutExtension}" }, if (monitoreo) "Monitoreo" else null
    ).joinToString(" · ").ifBlank { "Metrónomo, grabar encima de una base, monitoreo" }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Encabezado("Grabar", "Calidad de estudio · WAV 48 kHz", Icons.Filled.Mic)

        // ── Entrada: chips con íconos ──
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            FilterChip(device == null, { if (!ocupado) device = null }, label = { Text("Automático") },
                leadingIcon = { Icon(iconoEntrada(null), null, Modifier.size(18.dp)) }, modifier = Modifier.padding(end = 8.dp))
            devices.forEach { d ->
                FilterChip(device?.id == d.id, { if (!ocupado) device = d }, label = { Text(AudioRecorder.describe(d)) },
                    leadingIcon = { Icon(iconoEntrada(d), null, Modifier.size(18.dp)) }, modifier = Modifier.padding(end = 8.dp))
            }
        }
        if (device?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) Box(Modifier.padding(horizontal = 16.dp)) {
            Aviso("Bluetooth graba con calidad de llamada. Para calidad de estudio usa USB tipo C.", error = true)
        }

        // ── Zona central de grabación ──
        Card(Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().background(Marca.degradadoSuave).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (estado) { "cuenta" -> "Prepárate…"; "grabando" -> "%02d:%02d".format(seconds / 60, seconds % 60); else -> "00:00" },
                    style = EstiloNumeroGrande,
                    color = if (estado == "grabando") Marca.Coral else MaterialTheme.colorScheme.onSurface
                )
                Text(when (estado) { "grabando" -> "● Grabando · ${recorder.fuenteActual}"; "cuenta" -> "Cuenta regresiva"; else -> "Toca el botón para grabar" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (tiempoActual >= 0) Row(Modifier.padding(top = 8.dp)) {
                    repeat(metro.tiempos) { i ->
                        Box(Modifier.padding(4.dp).size(12.dp).clip(CircleShape).background(
                            when { i == tiempoActual && i == 0 -> Marca.Coral; i == tiempoActual -> Marca.Violeta; else -> Marca.Superficie3 }))
                    }
                }
                Spacer(Modifier.height(8.dp))
                BotonGrabar(grabando = estado == "grabando", preparando = estado == "cuenta") {
                    if (ocupado) detener() else pedirPermiso { comenzar() }
                }
                Vumetro(level, Modifier.padding(top = 8.dp))
                if (level > 0.9f) Text("Muy fuerte: aléjate un poco del micrófono", color = Marca.Coral,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                if (!ocupado) OutlinedTextField(name, { name = it }, label = { Text("Nombre (opcional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            }
        }
        if (mensaje.isNotEmpty()) Box(Modifier.padding(horizontal = 16.dp)) { Aviso(mensaje, error = error) }

        // ── Opciones plegadas ──
        Column(Modifier.padding(horizontal = 16.dp)) {
            Seccion("Opciones de grabación") {
                Ayuda(resumenOpciones)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(enabled = !ocupado && !probando, onClick = {
                    pedirPermiso {
                        probando = true; prueba = "Habla o canta durante 3 segundos…"
                        scope.launch {
                            prueba = withContext(Dispatchers.IO) { recorder.probar(device) { level = it } }
                            level = 0f; probando = false
                        }
                    }
                }) { Icon(Icons.Filled.GraphicEq, null); Spacer(Modifier.width(6.dp)); Text(if (probando) "Escuchando…" else "Probar micrófono") }
                if (prueba.isNotEmpty()) Text(prueba, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Spacer(Modifier.height(8.dp))
                FilaInterruptor(Icons.Filled.Timer, "Metrónomo mientras grabo", click, !ocupado) { click = it }
                FilaInterruptor(Icons.Filled.HourglassTop, "Cuenta de 1 compás antes", cuenta, !ocupado) { cuenta = it }
                if (click || cuenta) Deslizador("Tempo", bpm, 40f..220f, { "${it.toInt()} BPM" }) { bpm = it.toInt().toFloat() }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Selector("Grabar encima de", listOf<File?>(null) + files, base,
                    { it?.nameWithoutExtension ?: "Nada (grabar solo)" }) { base = it; AppState.baseOverdub = it }
                if (base != null) {
                    Deslizador("Volumen de la base", volBase, 0f..1f, { "${(it * 100).toInt()} %" }) { volBase = it }
                    Ayuda("Escucha la base con audífonos. Latencia compensada: ${ajustes.latenciaMs} ms (Más → Ajustes).")
                }
                FilaInterruptor(Icons.Filled.Headphones, "Escucharme en los audífonos", monitoreo, !ocupado) { monitoreo = it }
                if (click || monitoreo || base != null) Ayuda("Usa audífonos para que el sonido no se cuele en la grabación.")
            }
        }

        // ── Tomas ──
        Row(Modifier.padding(start = 20.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Mis tomas", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("${files.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (files.isEmpty()) Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { Ayuda("Tus grabaciones aparecerán aquí.") }
        files.forEach { f ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(Modifier.padding(start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlayButton(f)
                    Column(Modifier.weight(1f)) {
                        Text(f.nameWithoutExtension, fontWeight = FontWeight.Medium, maxLines = 1)
                        val seg = remember(f, f.length()) { AudioRecorder.duracion(f) }
                        val fecha = SimpleDateFormat("d MMM, HH:mm", Locale("es")).format(Date(f.lastModified()))
                        if (seg < 0.05f) Text("Vacía (sin audio) · $fecha", style = MaterialTheme.typography.bodySmall, color = Marca.Coral)
                        else Ayuda("%d:%02d · %.1f MB · %s".format(seg.toInt() / 60, seg.toInt() % 60, f.length() / 1_048_576f, fecha))
                    }
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Editar") }, leadingIcon = { Icon(Icons.Filled.ContentCut, null) },
                                onClick = { menu = false; AppState.archivoAEditar = f; AppState.pantalla = Pantalla.EDITAR })
                            DropdownMenuItem(text = { Text("Agregar al proyecto") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                                onClick = { menu = false; agregarAlProyecto(f) })
                            DropdownMenuItem(text = { Text("Compartir") }, leadingIcon = { Icon(Icons.Filled.Share, null) },
                                onClick = { menu = false; com.estudio.musical.audio.Exportar.compartir(ctx, f) })
                            DropdownMenuItem(text = { Text("Borrar", color = Marca.Coral) }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Marca.Coral) },
                                onClick = { menu = false; f.delete(); files = Carpetas.listaGrabaciones(ctx) })
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun FilaInterruptor(icono: ImageVector, texto: String, valor: Boolean, habilitado: Boolean = true, onCambio: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(texto, Modifier.weight(1f))
        Switch(valor, onCambio, enabled = habilitado)
    }
}
