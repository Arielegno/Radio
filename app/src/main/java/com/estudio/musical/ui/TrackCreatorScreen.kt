package com.estudio.musical.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.estudio.musical.audio.AudioData
import com.estudio.musical.audio.MotorAudio
import com.estudio.musical.audio.Wav
import com.estudio.musical.audio.synth.FuenteSonido
import com.estudio.musical.audio.synth.Instrumentos
import com.estudio.musical.audio.synth.Muestra
import com.estudio.musical.audio.synth.Pieza
import com.estudio.musical.data.Carpetas
import com.estudio.musical.data.Fuentes
import com.estudio.musical.data.PistaProyecto
import com.estudio.musical.data.Proyecto
import com.estudio.musical.music.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Creador de pistas: eliges tonalidad, estilo y progresión, y la app genera
 * batería, bajo, acordes y una melodía (con un instrumento o con tu propio sonido grabado).
 */
@Composable
fun TrackCreatorScreen(modifier: Modifier, proyecto: Proyecto, onCerrar: (List<PistaProyecto>) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    BackHandler { MotorAudio.detenerTodo(); onCerrar(emptyList()) }

    val tonoInicial = Teoria.parseTono(proyecto.tonalidad) ?: (0 to true)
    var tonica by remember { mutableIntStateOf(tonoInicial.first) }
    var mayor by remember { mutableStateOf(tonoInicial.second) }
    var estilo by remember { mutableStateOf(Estilo.POP) }
    var bpm by remember { mutableFloatStateOf(proyecto.bpm.toFloat()) }
    var compases by remember { mutableIntStateOf(8) }
    var progresion by remember { mutableStateOf(estilo.progresiones(mayor).first()) }
    var fuentes by remember { mutableStateOf<List<FuenteSonido>>(Instrumentos.TODOS) }
    LaunchedEffect(Unit) { fuentes = withContext(Dispatchers.IO) { Fuentes.todas(ctx) } }

    var conBateria by remember { mutableStateOf(true) }
    val patron = remember { mutableStateMapOf<Pieza, String>().apply { putAll(Patrones.bateria(Estilo.POP)) } }
    var conBajo by remember { mutableStateOf(true) }
    var fBajo by remember { mutableStateOf<FuenteSonido>(Instrumentos.BAJO_ELECTRICO) }
    var pBajo by remember { mutableStateOf(Patrones.bajo(estilo)) }
    var conAcordes by remember { mutableStateOf(true) }
    var fAcordes by remember { mutableStateOf<FuenteSonido>(Instrumentos.PIANO_ELECTRICO) }
    var pAcordes by remember { mutableStateOf(Patrones.acordes(estilo)) }
    var conMelodia by remember { mutableStateOf(false) }
    var fMelodia by remember { mutableStateOf<FuenteSonido>(Instrumentos.MARIMBA) }
    LaunchedEffect(fuentes) { fuentes.firstOrNull { it is Muestra }?.let { fMelodia = it } }

    var ocupado by remember { mutableStateOf(false) }
    var sonando by remember { mutableStateOf(false) }
    var ultimas by remember { mutableStateOf<Map<String, FloatArray>?>(null) }

    fun aplicarEstilo(e: Estilo) {
        estilo = e; bpm = e.bpm.toFloat()
        progresion = e.progresiones(mayor).first()
        patron.clear(); patron.putAll(Patrones.bateria(e))
        pBajo = Patrones.bajo(e); pAcordes = Patrones.acordes(e)
        ultimas = null
    }

    val grados = Grado.parseLista(progresion)
    val acordesTexto = grados.joinToString(" – ") { it.acorde(tonica, mayor).nombre }

    fun arreglo() = Arreglo(
        tonica, mayor, bpm, estilo, grados, compases,
        acordes = fAcordes.takeIf { conAcordes }, patronAcordes = pAcordes,
        bajo = fBajo.takeIf { conBajo }, patronBajo = pBajo,
        bateria = if (conBateria) patron.toMutableMap() else null,
        melodia = fMelodia.takeIf { conMelodia }
    )

    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { MotorAudio.detenerTodo(); onCerrar(emptyList()) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Column { Text("Crear pista", style = MaterialTheme.typography.headlineSmall); Ayuda("Batería, bajo, acordes y melodía en tu tono") }
        }

        Seccion("1 · Tonalidad y estilo", inicialAbierta = true) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Teoria.NOTAS.forEachIndexed { i, n ->
                    FilterChip(tonica == i, { tonica = i; ultimas = null }, label = { Text(n) }, modifier = Modifier.padding(end = 4.dp))
                }
            }
            Row {
                FilterChip(mayor, { mayor = true; progresion = estilo.progresiones(true).first() }, label = { Text("Mayor") })
                Spacer(Modifier.width(8.dp))
                FilterChip(!mayor, { mayor = false; progresion = estilo.progresiones(false).first() }, label = { Text("Menor") })
            }
            Selector("Estilo", Estilo.entries, estilo, { it.nombre }) { aplicarEstilo(it) }
            Deslizador("Tempo", bpm, 50f..200f, { "${it.toInt()} BPM" }) { bpm = it.toInt().toFloat() }
            Selector("Largo", listOf(4, 8, 12, 16, 24, 32), compases, { "$it compases" }) { compases = it }
        }

        Seccion("2 · Progresión de acordes", inicialAbierta = true) {
            OutlinedTextField(progresion, { progresion = it }, label = { Text("Grados (ej. I V vi IV)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text(acordesTexto.ifBlank { "Escribe grados en números romanos" }, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
            Ayuda("Mayúscula = acorde mayor, minúscula = menor, ° = disminuido, 7 = séptima, b = bemol (bVII).")
            Text("Sugerencias para ${estilo.nombre}:", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                estilo.progresiones(mayor).forEach { pr ->
                    AssistChip(onClick = { progresion = pr }, label = { Text(pr) }, modifier = Modifier.padding(end = 6.dp))
                }
                AssistChip(onClick = {
                    progresion = Estilo.entries.flatMap { it.progresiones(mayor) }.random()
                }, label = { Text("🎲 Sorpréndeme") })
            }
        }

        Seccion("3 · Batería") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(conBateria, { conBateria = it }); Spacer(Modifier.width(8.dp)); Text("Incluir batería")
            }
            if (conBateria) {
                Ayuda("Toca las casillas: vacía → golpe fuerte → golpe suave. Cada columna es una semicorchea.")
                CuadriculaBateria(patron, estilo.tiempos * 4)
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    Pieza.entries.filter { it !in patron.keys }.forEach { pz ->
                        AssistChip(onClick = { patron[pz] = ".".repeat(estilo.tiempos * 4) }, label = { Text("+ ${pz.nombre}") },
                            modifier = Modifier.padding(end = 4.dp))
                    }
                }
            }
        }
        Seccion("4 · Bajo") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(conBajo, { conBajo = it }); Spacer(Modifier.width(8.dp)); Text("Incluir bajo")
            }
            if (conBajo) {
                Selector("Sonido", fuentes, fBajo, { it.nombre }) { fBajo = it }
                Selector("Ritmo", PatronBajo.entries, pBajo, { it.nombre }) { pBajo = it }
            }
        }
        Seccion("5 · Acordes") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(conAcordes, { conAcordes = it }); Spacer(Modifier.width(8.dp)); Text("Incluir acordes")
            }
            if (conAcordes) {
                Selector("Sonido", fuentes, fAcordes, { it.nombre }) { fAcordes = it }
                Selector("Ritmo", PatronAcordes.entries, pAcordes, { it.nombre }) { pAcordes = it }
            }
        }
        Seccion("6 · Melodía / tu sonido") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(conMelodia, { conMelodia = it }); Spacer(Modifier.width(8.dp)); Text("Incluir melodía")
            }
            Ayuda("Toca notas de la escala sobre los acordes. Elige una de tus muestras del Sampler para que suene con tu propio sonido afinado en la tonalidad.")
            if (conMelodia) Selector("Sonido", fuentes, fMelodia, { (if (it is Muestra) "🎙 " else "") + it.nombre }) { fMelodia = it }
        }

        if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        Row(Modifier.padding(vertical = 12.dp)) {
            Button(enabled = !ocupado && grados.isNotEmpty(), onClick = {
                if (sonando) { MotorAudio.detenerTodo(); sonando = false; return@Button }
                ocupado = true
                scope.launch {
                    val pistas = withContext(Dispatchers.Default) { GeneradorPistas.generar(arreglo(), MotorAudio.SR) }
                    ultimas = pistas
                    MotorAudio.tocar(GeneradorPistas.mezcla(pistas), 0.9f)
                    ocupado = false; sonando = true
                }
            }) {
                Icon(if (sonando) Icons.Filled.Stop else Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(4.dp)); Text(if (sonando) "Detener" else "Escuchar")
            }
            Spacer(Modifier.width(8.dp))
            Button(enabled = !ocupado && grados.isNotEmpty(), onClick = {
                MotorAudio.detenerTodo()
                ocupado = true
                scope.launch {
                    val nuevas = withContext(Dispatchers.Default) {
                        val pistas = ultimas ?: GeneradorPistas.generar(arreglo(), MotorAudio.SR)
                        pistas.map { (nombre, datos) ->
                            val f = Carpetas.nombreLibre(Carpetas.grabaciones(ctx), "${proyecto.nombre} - $nombre")
                            Wav.write(f, AudioData(datos, MotorAudio.SR))
                            PistaProyecto(f.absolutePath, "$nombre (${estilo.nombre})",
                                volumen = when (nombre) { "Batería" -> 0.8f; "Bajo" -> 0.75f; "Acordes" -> 0.6f; else -> 0.55f },
                                paneo = when (nombre) { "Acordes" -> -0.25f; "Melodía" -> 0.3f; else -> 0f })
                        }
                    }
                    ocupado = false
                    onCerrar(nuevas)
                }
            }) { Icon(Icons.Filled.Save, null); Spacer(Modifier.width(4.dp)); Text("Guardar en el proyecto") }
        }
        Ayuda("Cada parte se guarda como una pista separada para que ajustes su volumen en la mezcla.")
    }
}

@Composable
private fun CuadriculaBateria(patron: MutableMap<Pieza, String>, pasos: Int) {
    val fuerte = MaterialTheme.colorScheme.primary
    val suave = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val vacio = MaterialTheme.colorScheme.surfaceVariant
    val borde = MaterialTheme.colorScheme.outline
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        patron.keys.sortedBy { it.ordinal }.forEach { pieza ->
            val fila = (patron[pieza] ?: "").padEnd(pasos, '.').take(pasos)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Text(pieza.nombre, Modifier.width(92.dp).clickable { MotorAudio.tocar(com.estudio.musical.audio.synth.Bateria.sonido(pieza, MotorAudio.SR)) },
                    style = MaterialTheme.typography.labelMedium)
                fila.forEachIndexed { i, ch ->
                    Box(
                        Modifier.padding(end = if (i % 4 == 3) 6.dp else 2.dp).size(22.dp).clip(RoundedCornerShape(4.dp))
                            .background(when (ch) { 'x' -> fuerte; 'o' -> suave; else -> vacio })
                            .border(if (i % 4 == 0) 1.dp else 0.dp, borde, RoundedCornerShape(4.dp))
                            .clickable {
                                val nuevo = when (ch) { '.' -> 'x'; 'x' -> 'o'; else -> '.' }
                                patron[pieza] = fila.substring(0, i) + nuevo + fila.substring(i + 1)
                            }
                    )
                }
                IconButton(onClick = { patron.remove(pieza) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Quitar ${pieza.nombre}")
                }
            }
        }
    }
}
