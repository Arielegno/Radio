package com.estudio.musical.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estudio.musical.audio.KeyDetector
import com.estudio.musical.audio.Wav
import com.estudio.musical.audio.dsp.Bpm
import com.estudio.musical.data.Carpetas
import com.estudio.musical.music.Teoria
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun KeyScreen(modifier: Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val files = remember { Carpetas.listaGrabaciones(ctx) }
    var selected by remember { mutableStateOf<File?>(null) }
    var result by remember { mutableStateOf<KeyDetector.Resultado?>(null) }
    var bpm by remember { mutableFloatStateOf(0f) }
    var busy by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Ayuda("Elige una grabación (mejor con acordes o melodía completa, 20 s o más).")
        Selector("Grabación", files, selected, { it.nameWithoutExtension }) { selected = it; result = null }
        Button(enabled = selected != null && !busy, modifier = Modifier.padding(top = 8.dp), onClick = {
            busy = true
            scope.launch {
                withContext(Dispatchers.Default) {
                    val a = Wav.read(selected!!)
                    result = KeyDetector.detect(a); bpm = Bpm.detect(a.samples, a.sampleRate)
                }
                busy = false
            }
        }) { Text("Analizar") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))

        result?.let { r ->
            Spacer(Modifier.height(16.dp))
            Text(r.tonalidad, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text("Confianza: ${(r.confianza * 100).toInt()} %  ·  También podría ser: ${r.alternativa}")
            if (bpm > 0) Text("Tempo aproximado: ${bpm.toInt()} BPM (o ${(bpm / 2).toInt()} / ${(bpm * 2).toInt()})")
            Spacer(Modifier.height(12.dp))
            val (tonica, modo) = Teoria.parseTono(r.tonalidad) ?: (0 to true)
            val escala = Teoria.escala(tonica, modo)
            Text("Notas de la escala: " + escala.joinToString(" – ") { Teoria.NOTAS[it] })
            Text("Acordes que suenan bien: " + Teoria.acordes(tonica, modo).joinToString("  "))
            Spacer(Modifier.height(16.dp))
            Text("Presencia de cada nota", style = MaterialTheme.typography.titleMedium)
            val max = r.chroma.maxOrNull()?.takeIf { it > 0 } ?: 1.0
            Teoria.NOTAS.forEachIndexed { i, n ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n, Modifier.width(48.dp))
                    Box(Modifier.height(14.dp).fillMaxWidth((r.chroma[i] / max).toFloat().coerceAtLeast(0.01f))
                        .background(if (i in escala) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                }
                Spacer(Modifier.height(2.dp))
            }
            Ayuda("Toda tonalidad mayor tiene una relativa menor con las mismas notas (Do mayor ↔ La menor). Escucha qué acorde se siente como “casa”.")
        }
    }
}
