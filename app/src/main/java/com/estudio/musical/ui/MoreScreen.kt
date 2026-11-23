package com.estudio.musical.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estudio.musical.audio.AfinadorEnVivo
import com.estudio.musical.audio.AudioRecorder
import com.estudio.musical.audio.Metronomo
import com.estudio.musical.audio.MotorAudio
import com.estudio.musical.audio.dsp.Pitch
import com.estudio.musical.audio.synth.Instrumentos
import com.estudio.musical.data.Ajustes
import com.estudio.musical.data.Documentos
import com.estudio.musical.music.Calidad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class Herramienta(val titulo: String, val desc: String, val icono: ImageVector) {
    AFINADOR("Afinador", "Guitarra, bajo, voz o cualquier instrumento", Icons.Filled.Tune),
    METRONOMO("Metrónomo", "Con tap tempo y compases", Icons.Filled.Timer),
    TONO("Tonalidad y tempo", "Descubre en qué tono y BPM está una grabación", Icons.Filled.MusicNote),
    OIDO("Entrenamiento de oído", "Reconoce intervalos y acordes", Icons.Filled.Hearing),
    TEORIA("Teoría, piano y guitarra", "Lecciones, escalas y guía para grabar", Icons.Filled.School),
    AJUSTES("Ajustes y respaldo", "Latencia, copia de seguridad local", Icons.Filled.Settings)
}

@Composable
fun MoreScreen(modifier: Modifier) {
    var abierta by remember { mutableStateOf<Herramienta?>(null) }
    val h = abierta
    if (h != null) {
        BackHandler { abierta = null; MotorAudio.detenerTodo() }
        Column(modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                IconButton(onClick = { abierta = null; MotorAudio.detenerTodo() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                Text(h.titulo, style = MaterialTheme.typography.titleLarge)
            }
            val m = Modifier.weight(1f)
            when (h) {
                Herramienta.AFINADOR -> AfinadorScreen(m)
                Herramienta.METRONOMO -> MetronomoScreen(m)
                Herramienta.TONO -> KeyScreen(m)
                Herramienta.OIDO -> OidoScreen(m)
                Herramienta.TEORIA -> LearnScreen(m)
                Herramienta.AJUSTES -> AjustesScreen(m)
            }
        }
        return
    }
    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        SectionTitle("Herramientas")
        Herramienta.entries.forEach { t ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { abierta = t }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(t.icono, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column { Text(t.titulo, fontWeight = FontWeight.Bold); Ayuda(t.desc) }
                }
            }
        }
    }
}

// ───────────────────────── Afinador ─────────────────────────

@Composable
private fun AfinadorScreen(modifier: Modifier) {
    val afinador = remember { AfinadorEnVivo() }
    val pedirPermiso = rememberPermisoMicrofono()
    var freq by remember { mutableFloatStateOf(-1f) }
    var activo by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { afinador.detener() } }
    LaunchedEffect(Unit) { pedirPermiso { afinador.iniciar { freq = it }; activo = true } }

    val midiExacto = if (freq > 0) Pitch.midi(freq) else Float.NaN
    val nota = if (freq > 0) midiExacto.roundToInt() else -1
    val cents = if (freq > 0) ((midiExacto - nota) * 100).roundToInt() else 0
    val afinado = freq > 0 && abs(cents) <= 5
    val verde = androidx.compose.ui.graphics.Color(0xFF2E7D32)

    Column(modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!activo) Button(onClick = { pedirPermiso { afinador.iniciar { freq = it }; activo = true } }) { Text("Activar micrófono") }
        Text(if (nota >= 0) Pitch.nombre(nota) else "—", fontSize = 72.sp, fontWeight = FontWeight.Bold,
            color = if (afinado) verde else MaterialTheme.colorScheme.onSurface)
        Text(if (freq > 0) "%.1f Hz".format(freq) else "Toca o canta una nota", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        val cAguja = if (afinado) verde else MaterialTheme.colorScheme.error
        val cEscala = MaterialTheme.colorScheme.outline
        Canvas(Modifier.fillMaxWidth().height(60.dp)) {
            val centro = size.width / 2
            for (c in -50..50 step 10) {
                val x = centro + c / 50f * centro * 0.9f
                drawLine(cEscala, Offset(x, if (c == 0) 0f else 20f), Offset(x, size.height - 20f), if (c == 0) 4f else 2f)
            }
            if (freq > 0) {
                val x = centro + cents.coerceIn(-50, 50) / 50f * centro * 0.9f
                drawCircle(cAguja, 16f, Offset(x, size.height / 2))
            }
        }
        Text(when { freq <= 0 -> ""; afinado -> "¡Afinado!"; cents < 0 -> "Bajo $cents cents ↑ sube"; else -> "Alto +$cents cents ↓ baja" },
            style = MaterialTheme.typography.titleMedium, color = cAguja)
        Spacer(Modifier.height(24.dp))
        Text("Notas de referencia (guitarra estándar)", style = MaterialTheme.typography.labelLarge)
        Row {
            listOf(40, 45, 50, 55, 59, 64).forEach { n ->
                OutlinedButton(onClick = { MotorAudio.notaOn(Instrumentos.GUITARRA, n, 0.8f) }, modifier = Modifier.padding(2.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)) { Text(Pitch.nombre(n)) }
            }
        }
        Ayuda("De la 6ª cuerda (Mi grave) a la 1ª (Mi agudo). Bajo: Mi1 La1 Re2 Sol2.")
    }
}

// ───────────────────────── Metrónomo ─────────────────────────

@Composable
private fun MetronomoScreen(modifier: Modifier) {
    val ctx = LocalContext.current
    val ajustes = remember { Ajustes(ctx) }
    val metro = remember { Metronomo() }
    var bpm by remember { mutableFloatStateOf(ajustes.bpm.toFloat()) }
    var tiempos by remember { mutableIntStateOf(4) }
    var activo by remember { mutableStateOf(false) }
    var actual by remember { mutableIntStateOf(-1) }
    val toques = remember { mutableListOf<Long>() }
    metro.onTiempo = { actual = it }
    DisposableEffect(Unit) { onDispose { metro.detener() } }
    LaunchedEffect(bpm, tiempos) { metro.bpm = bpm; metro.tiempos = tiempos; ajustes.bpm = bpm.toInt() }

    Column(modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${bpm.toInt()}", fontSize = 80.sp, fontWeight = FontWeight.Bold)
        Text("BPM · " + when { bpm < 66 -> "Largo"; bpm < 76 -> "Adagio"; bpm < 108 -> "Andante / Moderato"; bpm < 120 -> "Allegretto"; bpm < 168 -> "Allegro"; else -> "Presto" })
        Row(Modifier.padding(vertical = 16.dp)) {
            repeat(tiempos) { i ->
                Box(Modifier.padding(6.dp).size(28.dp).clip(CircleShape).background(
                    when { i == actual && i == 0 -> MaterialTheme.colorScheme.error; i == actual -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant }))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { bpm = (bpm - 1).coerceAtLeast(30f) }) { Icon(Icons.Filled.Remove, "Menos") }
            Slider(bpm, { bpm = it.roundToInt().toFloat() }, valueRange = 30f..240f, modifier = Modifier.weight(1f))
            IconButton(onClick = { bpm = (bpm + 1).coerceAtMost(240f) }) { Icon(Icons.Filled.Add, "Más") }
        }
        Row {
            listOf(2, 3, 4, 6).forEach { t ->
                FilterChip(tiempos == t, { tiempos = t }, label = { Text("$t/${if (t == 6) 8 else 4}") }, modifier = Modifier.padding(4.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = { if (activo) { metro.detener(); actual = -1 } else metro.iniciar(); activo = !activo }) {
                Icon(if (activo) Icons.Filled.Stop else Icons.Filled.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text(if (activo) "Detener" else "Iniciar")
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = {
                val ahora = System.currentTimeMillis()
                if (toques.isNotEmpty() && ahora - toques.last() > 2000) toques.clear()
                toques += ahora
                if (toques.size > 6) toques.removeAt(0)
                if (toques.size >= 3) {
                    val prom = (toques.last() - toques.first()) / (toques.size - 1f)
                    bpm = (60_000f / prom).roundToInt().toFloat().coerceIn(30f, 240f)
                }
            }) { Text("Tap tempo") }
        }
        Ayuda("Toca “Tap tempo” al ritmo de la canción 4 veces o más para medir su tempo.")
    }
}

// ───────────────────────── Entrenamiento de oído ─────────────────────────

private val INTERVALOS = listOf("2ª menor", "2ª mayor", "3ª menor", "3ª mayor", "4ª justa", "Tritono",
    "5ª justa", "6ª menor", "6ª mayor", "7ª menor", "7ª mayor", "Octava")

@Composable
private fun OidoScreen(modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var modo by remember { mutableIntStateOf(0) } // 0 intervalos, 1 acordes
    var facil by remember { mutableStateOf(true) }
    var raiz by remember { mutableIntStateOf(60) }
    var respuesta by remember { mutableIntStateOf(-1) }
    var elegida by remember { mutableIntStateOf(-1) }
    var aciertos by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    val calidades = listOf(Calidad.MAYOR, Calidad.MENOR, Calidad.DISMINUIDO, Calidad.AUMENTADO, Calidad.SEPTIMA, Calidad.MAYOR7, Calidad.MENOR7)
    val nombresAc = listOf("Mayor", "Menor", "Disminuido", "Aumentado", "Séptima (7)", "Mayor 7", "Menor 7")
    val opciones = if (modo == 0) (if (facil) listOf(2, 3, 4, 6, 11) else (0..11).toList()) else (if (facil) listOf(0, 1) else calidades.indices.toList())

    fun tocar() = scope.launch {
        val piano = Instrumentos.PIANO
        if (modo == 0) {
            val n2 = raiz + respuesta + 1
            MotorAudio.notaOn(piano, raiz); delay(650); MotorAudio.notaOff(raiz)
            MotorAudio.notaOn(piano, n2); delay(650); MotorAudio.notaOff(n2)
            delay(200); MotorAudio.notaOn(piano, raiz); MotorAudio.notaOn(piano, n2); delay(900)
            MotorAudio.notaOff(raiz); MotorAudio.notaOff(n2)
        } else {
            val notas = calidades[respuesta].intervalos.map { raiz + it }
            notas.forEach { MotorAudio.notaOn(piano, it, 0.7f) }; delay(1300); notas.forEach { MotorAudio.notaOff(it) }
        }
    }
    fun nueva() { raiz = (55..67).random(); respuesta = opciones.random(); elegida = -1; tocar() }

    Column(modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
        TabRow(selectedTabIndex = modo) {
            Tab(modo == 0, { modo = 0; respuesta = -1 }, text = { Text("Intervalos") })
            Tab(modo == 1, { modo = 1; respuesta = -1 }, text = { Text("Acordes") })
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Switch(facil, { facil = it; respuesta = -1 }); Spacer(Modifier.width(8.dp)); Text("Modo fácil (menos opciones)")
        }
        Text("Aciertos: $aciertos de $total", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            Button(onClick = { nueva() }) { Text(if (respuesta < 0) "Empezar" else "Siguiente") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(enabled = respuesta >= 0, onClick = { tocar() }) { Text("Repetir") }
        }
        if (respuesta >= 0) {
            Text("¿Qué ${if (modo == 0) "intervalo" else "acorde"} escuchaste?")
            opciones.chunked(2).forEach { fila ->
                Row {
                    fila.forEach { o ->
                        val correcto = elegida >= 0 && o == respuesta
                        val mal = elegida == o && o != respuesta
                        OutlinedButton(
                            onClick = {
                                if (elegida < 0) { elegida = o; total++; if (o == respuesta) aciertos++ }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = when {
                                correcto -> MaterialTheme.colorScheme.primaryContainer
                                mal -> MaterialTheme.colorScheme.errorContainer
                                else -> androidx.compose.ui.graphics.Color.Transparent }),
                            modifier = Modifier.weight(1f).padding(4.dp)
                        ) { Text(if (modo == 0) INTERVALOS[o] else nombresAc[o], textAlign = TextAlign.Center) }
                    }
                }
            }
            if (elegida >= 0) Text(if (elegida == respuesta) "¡Correcto! 🎉" else "Era: ${if (modo == 0) INTERVALOS[respuesta] else nombresAc[respuesta]}",
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Ayuda("Truco: 4ª justa = inicio de “Aquí viene la novia”; 5ª justa = “Estrellita”; 3ª mayor suena alegre y 3ª menor triste.")
    }
}

// ───────────────────────── Ajustes y respaldo ─────────────────────────

@Composable
private fun AjustesScreen(modifier: Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ajustes = remember { Ajustes(ctx) }
    val pedirPermiso = rememberPermisoMicrofono()
    var latencia by remember { mutableFloatStateOf(ajustes.latenciaMs.toFloat()) }
    var mensaje by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }

    val crear = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) { ocupado = true; scope.launch {
            mensaje = runCatching { withContext(Dispatchers.IO) { Documentos.crearRespaldo(ctx, uri) }; "Respaldo guardado ✔" }
                .getOrElse { "No se pudo crear el respaldo: ${it.message}" }
            ocupado = false } }
    }
    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { ocupado = true; scope.launch {
            mensaje = runCatching { "Restaurados ${withContext(Dispatchers.IO) { Documentos.restaurar(ctx, uri) }} archivos ✔. Reinicia la app." }
                .getOrElse { "No se pudo restaurar: ${it.message}" }
            ocupado = false } }
    }

    Column(modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
        SectionTitle("Latencia al grabar encima")
        Ayuda("Es el retraso entre lo que escuchas y lo que se graba. Si tu voz queda atrasada respecto a la base, súbelo.")
        Deslizador("Compensación", latencia, 0f..500f, { "${it.toInt()} ms" }) { latencia = it; ajustes.latenciaMs = it.toInt() }
        Button(enabled = !ocupado, onClick = {
            pedirPermiso {
                ocupado = true; mensaje = "Midiendo… silencio por favor (sin audífonos, volumen medio)"
                scope.launch {
                    val ms = withContext(Dispatchers.IO) { AudioRecorder(ctx).calibrarLatencia(null) }
                    if (ms > 0) { latencia = ms.toFloat(); ajustes.latenciaMs = ms; mensaje = "Latencia medida: $ms ms ✔" }
                    else mensaje = "No se escucharon los clicks. Sube el volumen e inténtalo otra vez."
                    ocupado = false
                }
            }
        }) { Text("Calibrar automáticamente") }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        SectionTitle("Copia de seguridad local")
        Ayuda("Todo vive solo en tu teléfono: nada se sube a internet. Guarda un archivo .zip con letras, grabaciones, proyectos y sonidos en la memoria, una USB o pásalo a tu computadora.")
        Row(Modifier.padding(vertical = 8.dp)) {
            Button(enabled = !ocupado, onClick = {
                crear.launch("respaldo-estudio-" + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) + ".zip")
            }) { Text("Crear respaldo") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(enabled = !ocupado, onClick = { abrir.launch(arrayOf("application/zip", "application/octet-stream")) }) { Text("Restaurar") }
        }
        if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (mensaje.isNotEmpty()) Text(mensaje, color = MaterialTheme.colorScheme.primary)

        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        SectionTitle("Dictado sin internet")
        Ayuda("En Ajustes de Android → Sistema → Idioma → Reconocimiento de voz, descarga el paquete de Español para dictar letras sin conexión.")
    }
}
