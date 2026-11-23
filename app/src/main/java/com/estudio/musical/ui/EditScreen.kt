package com.estudio.musical.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.estudio.musical.AppState
import com.estudio.musical.audio.AudioData
import com.estudio.musical.audio.Effects
import com.estudio.musical.audio.Exportar
import com.estudio.musical.audio.KeyDetector
import com.estudio.musical.audio.Wav
import com.estudio.musical.audio.dsp.Bpm
import com.estudio.musical.audio.dsp.Dynamics
import com.estudio.musical.audio.dsp.Eq
import com.estudio.musical.audio.dsp.NoiseReduction
import com.estudio.musical.audio.dsp.Reverb
import com.estudio.musical.data.Carpetas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun Seccion(titulo: String, inicialAbierta: Boolean = false, contenido: @Composable ColumnScope.() -> Unit) {
    var abierta by remember { mutableStateOf(inicialAbierta) }
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().clickable { abierta = !abierta }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(if (abierta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
        if (abierta) Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), content = contenido)
    }
}

@Composable
fun EditScreen(modifier: Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf(Carpetas.listaGrabaciones(ctx)) }
    var original by remember { mutableStateOf<File?>(null) }
    // historial para deshacer/rehacer: cada paso es un archivo temporal
    val historial = remember { mutableStateListOf<File>() }
    var indice by remember { mutableIntStateOf(-1) }
    val actual = historial.getOrNull(indice)
    var picos by remember { mutableStateOf<FloatArray?>(null) }
    var duracion by remember { mutableFloatStateOf(0f) }
    var ocupado by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var nombreGuardar by remember { mutableStateOf("") }

    fun abrir(f: File) {
        original = f
        historial.forEach { it.delete() }; historial.clear()
        val copia = File(Carpetas.temporal(ctx), "paso0.wav")
        f.copyTo(copia, overwrite = true)
        historial += copia; indice = 0
        nombreGuardar = f.nameWithoutExtension + " editado"
        mensaje = ""
    }

    LaunchedEffect(Unit) { AppState.archivoAEditar?.let { abrir(it); AppState.archivoAEditar = null } }
    LaunchedEffect(actual) {
        actual?.let {
            picos = picosDe(it)
            duracion = withContext(Dispatchers.IO) { Wav.info(it).let { i -> i.dataLen / (2f * i.channels * i.sampleRate) } }
        }
    }

    // parámetros
    var recorte by remember { mutableStateOf(0f..1f) }
    LaunchedEffect(duracion) { recorte = 0f..duracion }
    var ganancia by remember { mutableFloatStateOf(0f) }
    var ecoMs by remember { mutableFloatStateOf(300f) }
    var ecoFb by remember { mutableFloatStateOf(0.35f) }
    var ecoMix by remember { mutableFloatStateOf(0.3f) }
    var sala by remember { mutableFloatStateOf(0.5f) }
    var amort by remember { mutableFloatStateOf(0.4f) }
    var revMix by remember { mutableFloatStateOf(0.25f) }
    var graves by remember { mutableFloatStateOf(0f) }
    var medios by remember { mutableFloatStateOf(0f) }
    var agudos by remember { mutableFloatStateOf(0f) }
    var umbral by remember { mutableFloatStateOf(-18f) }
    var ratio by remember { mutableFloatStateOf(4f) }
    var intensidadRuido by remember { mutableFloatStateOf(2.5f) }
    var analisis by remember { mutableStateOf("") }

    /** Aplica un efecto sobre el paso actual y crea un paso nuevo (se puede deshacer) */
    fun aplicar(nombre: String, fx: (AudioData) -> AudioData) {
        val src = actual ?: return
        ocupado = true
        // descartar los pasos "rehacer"
        while (historial.size > indice + 1) historial.removeAt(historial.lastIndex).delete()
        scope.launch {
            val nuevo = withContext(Dispatchers.Default) {
                val r = fx(Wav.read(src))
                val f = File(Carpetas.temporal(ctx), "paso${System.nanoTime()}.wav")
                Wav.write(f, r); f
            }
            historial += nuevo; indice = historial.lastIndex
            mensaje = "Aplicado: $nombre"
            ocupado = false
        }
    }
    fun fx(nombre: String, f: (FloatArray, Int) -> FloatArray) = aplicar(nombre) { AudioData(f(it.samples, it.sampleRate), it.sampleRate) }

    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Encabezado("Editar", "Recorta, limpia y dale brillo a tu sonido", Icons.Filled.GraphicEq, dentroDeMargen = true)
        Selector("Pista a editar", files, original, { it.nameWithoutExtension }) { abrir(it) }
        if (actual == null) { Spacer(Modifier.height(24.dp)); Aviso("Elige una grabación para empezar a editar.", Icons.Filled.GraphicEq); return@Column }

        Spacer(Modifier.height(8.dp))
        FormaDeOnda(picos, recorte, duracion)
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayButton(actual)
            Text("%.1f s".format(duracion), Modifier.weight(1f))
            IconButton(enabled = indice > 0 && !ocupado, onClick = { indice-- }) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer")
            }
            IconButton(enabled = indice < historial.lastIndex && !ocupado, onClick = { indice++ }) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rehacer")
            }
        }
        if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (mensaje.isNotEmpty()) Aviso(mensaje)

        Seccion("✂ Recortar", inicialAbierta = true) {
            Text("Desde %.2f s hasta %.2f s".format(recorte.start, recorte.endInclusive))
            if (duracion > 0f) RangeSlider(recorte, { recorte = it }, valueRange = 0f..duracion)
            Button(enabled = !ocupado, onClick = { aplicar("recorte") { Effects.trim(it, recorte.start, recorte.endInclusive) } }) { Text("Recortar a la selección") }
        }
        Seccion("🔊 Volumen y fundidos") {
            Deslizador("Ganancia", ganancia, -12f..12f, { "%+.1f dB".format(it) }) { ganancia = it }
            Row {
                Button(enabled = !ocupado, onClick = { aplicar("volumen") { Effects.gain(it, ganancia) } }) { Text("Aplicar") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(enabled = !ocupado, onClick = { aplicar("normalizar") { Effects.normalize(it) } }) { Text("Normalizar") }
            }
            Row {
                OutlinedButton(enabled = !ocupado, onClick = { aplicar("fade in") { Effects.fadeIn(it, 2f) } }) { Text("Fade in 2 s") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(enabled = !ocupado, onClick = { aplicar("fade out") { Effects.fadeOut(it, 3f) } }) { Text("Fade out 3 s") }
            }
        }
        Seccion("🧹 Limpiar ruido") {
            Ayuda("Quita ventiladores, zumbidos y siseo. Funciona mejor si la grabación tiene 1 segundo de silencio.")
            Deslizador("Intensidad", intensidadRuido, 1f..5f, { "%.1f".format(it) }) { intensidadRuido = it }
            Button(enabled = !ocupado, onClick = { fx("reducción de ruido") { x, sr -> NoiseReduction.process(x, sr, intensidadRuido) } }) { Text("Reducir ruido") }
            Row {
                OutlinedButton(enabled = !ocupado, onClick = { fx("puerta de ruido") { x, sr -> Dynamics.puertaRuido(x, sr) } }) { Text("Puerta de ruido") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(enabled = !ocupado, onClick = { fx("quitar graves") { x, sr -> Eq.pasaAltos(x, sr) } }) { Text("Quitar retumbe") }
            }
        }
        Seccion("🎚 Ecualizador") {
            Deslizador("Graves (120 Hz)", graves, -12f..12f, { "%+.0f dB".format(it) }) { graves = it }
            Deslizador("Medios (1 kHz)", medios, -12f..12f, { "%+.0f dB".format(it) }) { medios = it }
            Deslizador("Agudos (8 kHz)", agudos, -12f..12f, { "%+.0f dB".format(it) }) { agudos = it }
            Ayuda("Voz más clara: graves −3, agudos +3. Voz más cálida: graves +2, agudos −2.")
            Button(enabled = !ocupado, onClick = { fx("ecualizador") { x, sr -> Eq.tresBandas(x, sr, graves, medios, agudos) } }) { Text("Aplicar EQ") }
        }
        Seccion("🗜 Compresor") {
            Ayuda("Empareja el volumen: las partes fuertes bajan y las suaves se escuchan mejor.")
            Deslizador("Umbral", umbral, -40f..0f, { "%.0f dB".format(it) }) { umbral = it }
            Deslizador("Proporción", ratio, 1f..12f, { "%.1f : 1".format(it) }) { ratio = it }
            Button(enabled = !ocupado, onClick = { fx("compresor") { x, sr -> Dynamics.compresor(x, sr, umbral, ratio) } }) { Text("Comprimir") }
        }
        Seccion("🔁 Eco") {
            Deslizador("Retardo", ecoMs, 50f..1000f, { "${it.toInt()} ms" }) { ecoMs = it }
            Deslizador("Repeticiones", ecoFb, 0f..0.85f, { "${(it * 100).toInt()} %" }) { ecoFb = it }
            Deslizador("Cantidad", ecoMix, 0f..1f, { "${(it * 100).toInt()} %" }) { ecoMix = it }
            Button(enabled = !ocupado, onClick = { aplicar("eco") { Effects.echo(it, ecoMs.toInt(), ecoFb, ecoMix) } }) { Text("Aplicar eco") }
        }
        Seccion("⛪ Reverb (sala)") {
            Deslizador("Tamaño de sala", sala, 0f..1f, { if (it < 0.33f) "pequeña" else if (it < 0.66f) "mediana" else "grande" }) { sala = it }
            Deslizador("Amortiguación", amort, 0f..1f, { "${(it * 100).toInt()} %" }) { amort = it }
            Deslizador("Cantidad", revMix, 0f..1f, { "${(it * 100).toInt()} %" }) { revMix = it }
            Button(enabled = !ocupado, onClick = { fx("reverb") { x, sr -> Reverb.process(x, sr, sala, amort, revMix) } }) { Text("Aplicar reverb") }
        }
        Seccion("🔍 Analizar") {
            Button(enabled = !ocupado, onClick = {
                ocupado = true
                scope.launch {
                    analisis = withContext(Dispatchers.Default) {
                        val a = Wav.read(actual)
                        val k = KeyDetector.detect(a)
                        "Tonalidad: ${k.tonalidad} (o ${k.alternativa})\nTempo: %.0f BPM".format(Bpm.detect(a.samples, a.sampleRate))
                    }
                    ocupado = false
                }
            }) { Text("Detectar tonalidad y tempo") }
            if (analisis.isNotEmpty()) Text(analisis)
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        SectionTitle("Guardar y compartir")
        OutlinedTextField(nombreGuardar, { nombreGuardar = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.padding(top = 8.dp)) {
            Button(enabled = !ocupado, onClick = {
                val dest = Carpetas.nombreLibre(Carpetas.grabaciones(ctx), nombreGuardar)
                actual.copyTo(dest); files = Carpetas.listaGrabaciones(ctx)
                mensaje = "Guardado en Mis tomas: ${dest.nameWithoutExtension}"
            }) { Text("Guardar WAV") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(enabled = !ocupado, onClick = {
                ocupado = true
                scope.launch {
                    val m4a = withContext(Dispatchers.IO) {
                        Carpetas.nombreLibre(Carpetas.exportados(ctx), nombreGuardar, "m4a").also { Exportar.wavAM4a(actual, it) }
                    }
                    ocupado = false
                    mensaje = "Exportado: ${m4a.name}"
                    Exportar.compartir(ctx, m4a)
                }
            }) { Icon(Icons.Filled.Share, null); Spacer(Modifier.width(4.dp)); Text("M4A") }
        }
        Ayuda("M4A pesa ~10 veces menos que WAV y se puede enviar por WhatsApp. WAV conserva la calidad máxima.")
    }
}
