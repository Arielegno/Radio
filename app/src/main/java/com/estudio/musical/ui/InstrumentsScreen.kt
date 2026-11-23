package com.estudio.musical.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.estudio.musical.audio.AudioData
import com.estudio.musical.audio.AudioRecorder
import com.estudio.musical.audio.Metronomo
import com.estudio.musical.audio.MotorAudio
import com.estudio.musical.audio.Wav
import com.estudio.musical.audio.dsp.Pitch
import com.estudio.musical.audio.synth.*
import com.estudio.musical.data.*
import com.estudio.musical.music.GeneradorPistas
import com.estudio.musical.music.Nota
import com.estudio.musical.music.Teoria
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun InstrumentsScreen(modifier: Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        Encabezado("Tocar", "Instrumentos, batería y tus propios sonidos", Icons.Filled.Piano)
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp, containerColor = MaterialTheme.colorScheme.background, divider = {}) {
            listOf("Teclado", "Batería", "Sampler", "Crear sonido").forEachIndexed { i, t ->
                Tab(tab == i, { tab = i; MotorAudio.detenerTodo() }, text = { Text(t) })
            }
        }
        when (tab) {
            0 -> TecladoTab()
            1 -> BateriaTab()
            2 -> SamplerTab()
            3 -> SintetizadorTab()
        }
    }
}

// ───────────────────────── Teclado: tocar y grabar lo que tocas ─────────────────────────

@Composable
private fun TecladoTab() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var fuentes by remember { mutableStateOf<List<FuenteSonido>>(Instrumentos.TODOS) }
    LaunchedEffect(Unit) { fuentes = withContext(Dispatchers.IO) { Fuentes.todas(ctx) } }
    var fuente by remember { mutableStateOf<FuenteSonido>(Instrumentos.PIANO) }
    var octava by remember { mutableIntStateOf(4) }
    var tono by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    var grabando by remember { mutableStateOf(false) }
    var conClick by remember { mutableStateOf(false) }
    var bpm by remember { mutableFloatStateOf(Ajustes(ctx).bpm.toFloat()) }
    val metro = remember { Metronomo() }
    DisposableEffect(Unit) { onDispose { metro.detener() } }
    val eventos = remember { mutableListOf<Nota>() }
    val abiertas = remember { HashMap<Int, Long>() }
    var inicio by remember { mutableLongStateOf(0L) }
    var mensaje by remember { mutableStateOf("") }

    fun muestraAhora() = ((System.nanoTime() - inicio) / 1e9 * MotorAudio.SR).toInt()

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Selector("Instrumento", fuentes, fuente, { (if (it is Muestra) "🎙 " else "") + it.nombre }) { fuente = it }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            IconButton(onClick = { if (octava > 1) octava-- }) { Icon(Icons.Filled.Remove, contentDescription = "Bajar octava") }
            Text("Octava $octava", Modifier.width(80.dp), textAlign = TextAlign.Center)
            IconButton(onClick = { if (octava < 6) octava++ }) { Icon(Icons.Filled.Add, contentDescription = "Subir octava") }
            Spacer(Modifier.width(8.dp))
            Selector("Resaltar escala", listOf<Pair<Int, Boolean>?>(null) + (0..11).flatMap { listOf(it to true, it to false) },
                tono, { it?.let { t -> Teoria.nombreTono(t.first, t.second) } ?: "Ninguna" }, Modifier.weight(1f)) { tono = it }
        }
        PianoTeclado(
            desde = octava * 12 + 12, octavas = 2,
            resaltadas = tono?.let { Teoria.escala(it.first, it.second).toSet() } ?: emptySet(),
            raiz = tono?.first,
            onNotaOn = { n ->
                MotorAudio.notaOn(fuente, n, 0.85f)
                if (grabando) abiertas[n] = muestraAhora().toLong()
            },
            onNotaOff = { n ->
                MotorAudio.notaOff(n)
                if (grabando) abiertas.remove(n)?.let { t0 ->
                    eventos += Nota(t0.toInt(), (muestraAhora() - t0.toInt()).coerceAtLeast(MotorAudio.SR / 50), n, 0.85f)
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        SectionTitle("Grabar lo que toco")
        Ayuda("Se guarda como pista de audio con el sonido elegido, lista para el multipista.")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(conClick, { conClick = it }, enabled = !grabando); Spacer(Modifier.width(8.dp)); Text("Con metrónomo")
        }
        if (conClick) Deslizador("Tempo", bpm, 40f..220f, { "${it.toInt()} BPM" }) { bpm = it.toInt().toFloat() }
        Button(onClick = {
            if (!grabando) {
                eventos.clear(); abiertas.clear(); inicio = System.nanoTime(); grabando = true; mensaje = "Grabando… toca el teclado"
                if (conClick) { metro.bpm = bpm; metro.iniciar() }
            } else {
                grabando = false; metro.detener()
                val lista = eventos.toList()
                val f = fuente
                if (lista.isEmpty()) { mensaje = "No tocaste ninguna nota"; return@Button }
                scope.launch {
                    val out = withContext(Dispatchers.Default) {
                        val desplazamiento = lista.minOf { it.inicio }
                        val datos = GeneradorPistas.renderizar(lista.map { Nota(it.inicio - desplazamiento, it.dur, it.nota, it.vel) }, f, MotorAudio.SR)
                        Carpetas.nombreLibre(Carpetas.grabaciones(ctx), "Teclado - ${f.nombre}").also { Wav.write(it, AudioData(datos, MotorAudio.SR)) }
                    }
                    mensaje = "Guardado en Mis tomas: ${out.nameWithoutExtension}"
                }
            }
        }) {
            Icon(if (grabando) Icons.Filled.Stop else Icons.Filled.FiberManualRecord, null)
            Spacer(Modifier.width(4.dp)); Text(if (grabando) "Detener y guardar" else "Grabar interpretación")
        }
        if (mensaje.isNotEmpty()) Text(mensaje, color = MaterialTheme.colorScheme.primary)
    }
}

// ───────────────────────── Pads de batería ─────────────────────────

@Composable
private fun BateriaTab() {
    val piezas = Pieza.entries
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Ayuda("Toca los pads. Para crear un ritmo completo usa Estudio → Crear pista.")
        Spacer(Modifier.height(8.dp))
        piezas.chunked(3).forEach { fila ->
            Row(Modifier.fillMaxWidth()) {
                fila.forEach { p -> Pad(p.nombre, COLORES_PAD[p.ordinal % COLORES_PAD.size], Modifier.weight(1f)) { MotorAudio.tocar(Bateria.sonido(p, MotorAudio.SR)) } }
                repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private val COLORES_PAD = listOf(Marca.Coral, Marca.Violeta, Marca.Magenta, Marca.Ambar, Marca.Verde,
    androidx.compose.ui.graphics.Color(0xFF38BDF8))

@Composable
private fun Pad(texto: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onGolpe: () -> Unit) {
    var activo by remember { mutableStateOf(false) }
    val golpe by rememberUpdatedState(onGolpe)
    Box(
        modifier.padding(4.dp).aspectRatio(1.2f).clip(RoundedCornerShape(16.dp))
            .background(if (activo) color else color.copy(alpha = 0.22f))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    golpe(); activo = true
                    tryAwaitRelease(); activo = false
                })
            },
        contentAlignment = Alignment.Center
    ) { Text(texto, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = if (activo) androidx.compose.ui.graphics.Color.White else color) }
}

// ───────────────────────── Sampler: graba un sonido y tócalo en cualquier nota ─────────────────────────

@Composable
private fun SamplerTab() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { MuestrasRepo(ctx) }
    val recorder = remember { AudioRecorder(ctx) }
    val pedirPermiso = rememberPermisoMicrofono()
    var lista by remember { mutableStateOf(repo.todas()) }
    var muestra by remember { mutableStateOf<Muestra?>(null) }
    var info by remember { mutableStateOf<InfoMuestra?>(null) }
    var grabando by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("Mi sonido") }
    var mensaje by remember { mutableStateOf("") }
    var origen by remember { mutableStateOf<File?>(null) }
    val tomas = remember { Carpetas.listaGrabaciones(ctx) }
    val temp = remember { File(ctx.cacheDir, "muestra.wav") }
    DisposableEffect(Unit) { onDispose { if (recorder.isRecording) recorder.stop() } }

    fun crearDesde(f: File, n: String) = scope.launch {
        mensaje = "Analizando…"
        val m = withContext(Dispatchers.Default) {
            val a = Wav.readAt(f, MotorAudio.SR)
            // máximo 6 segundos
            Muestra.desdeGrabacion(n, a.samples.copyOf(minOf(a.samples.size, MotorAudio.SR * 6)), MotorAudio.SR)
        }
        muestra = m; info = null
        mensaje = if (m.notaRaiz > 0) "Nota detectada: ${Pitch.nombre(m.notaRaiz)}. Ajústala si suena desafinado." else "No se detectó nota; se usará Do4."
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Ayuda("Graba cualquier sonido (tu voz cantando una nota, un silbido, un vaso, una cuerda). La app detecta su nota y lo afina para tocarlo en cualquier tono.")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre del sonido") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.padding(vertical = 8.dp)) {
            Button(onClick = {
                if (grabando) {
                    recorder.stop(); grabando = false; crearDesde(temp, nombre)
                } else pedirPermiso {
                    MotorAudio.detenerTodo()
                    recorder.start(temp, null); grabando = true; mensaje = "Grabando… haz el sonido y luego detén"
                    scope.launch { delay(6000); if (recorder.isRecording) { recorder.stop(); grabando = false; crearDesde(temp, nombre) } }
                }
            }) {
                Icon(if (grabando) Icons.Filled.Stop else Icons.Filled.Mic, null)
                Spacer(Modifier.width(4.dp)); Text(if (grabando) "Detener" else "Grabar sonido (máx. 6 s)")
            }
        }
        Selector("…o usar una toma existente", tomas, origen, { it.nameWithoutExtension }) {
            origen = it; nombre = it.nameWithoutExtension; crearDesde(it, it.nameWithoutExtension)
        }
        if (mensaje.isNotEmpty()) Text(mensaje, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))

        muestra?.let { m ->
            var raiz by remember(m) { mutableIntStateOf(m.notaRaiz) }
            var bucle by remember(m) { mutableStateOf(m.bucle) }
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(m.nombre, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nota original:")
                        IconButton(onClick = { raiz--; m.notaRaiz = raiz }) { Icon(Icons.Filled.Remove, contentDescription = "Bajar") }
                        Text(Pitch.nombre(raiz), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { raiz++; m.notaRaiz = raiz }) { Icon(Icons.Filled.Add, contentDescription = "Subir") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(bucle, { bucle = it; m.bucle = it }); Spacer(Modifier.width(8.dp))
                        Text("Sostener mientras mantengo la tecla (bucle)")
                    }
                    Row {
                        Button(onClick = {
                            val i = info
                            if (i == null) { info = repo.guardar(m); mensaje = "Guardado. Ya aparece en Teclado y en Crear pista." }
                            else { repo.actualizar(i.copy(notaRaiz = raiz, bucle = bucle)); mensaje = "Actualizado" }
                            lista = repo.todas()
                        }) { Text(if (info == null) "Guardar sonido" else "Guardar cambios") }
                    }
                }
            }
            PianoTeclado(desde = 48, octavas = 2, raiz = raiz % 12,
                onNotaOn = { MotorAudio.notaOn(m, it, 0.9f) }, onNotaOff = { MotorAudio.notaOff(it) })
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        SectionTitle("Mis sonidos")
        if (lista.isEmpty()) Ayuda("Aún no guardas sonidos.")
        lista.forEach { i ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${i.nombre} · ${Pitch.nombre(i.notaRaiz)}", Modifier.weight(1f))
                TextButton(onClick = { muestra = repo.cargar(i); info = i; mensaje = "" }) { Text("Tocar") }
                IconButton(onClick = { repo.borrar(i); lista = repo.todas(); if (info == i) { info = null; muestra = null } }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Borrar")
                }
            }
        }
    }
}

// ───────────────────────── Crear sonido: editor de sintetizador ─────────────────────────

@Composable
private fun SintetizadorTab() {
    val ctx = LocalContext.current
    val repo = remember { SonidosRepo(ctx) }
    var propios by remember { mutableStateOf(repo.propios()) }
    var inst by remember { mutableStateOf(Instrumentos.PAD) }
    var nombre by remember { mutableStateOf("Mi sonido") }
    var mensaje by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Selector("Partir de", Instrumentos.TODOS + propios, inst, { it.nombre }) { inst = it; nombre = it.nombre + " (mío)" }
        PianoTeclado(desde = 48, octavas = 2, onNotaOn = { MotorAudio.notaOn(inst, it, 0.85f) }, onNotaOff = { MotorAudio.notaOff(it) })
        Spacer(Modifier.height(8.dp))
        Selector("Tipo de síntesis", TipoSintesis.entries, inst.tipo, {
            when (it) { TipoSintesis.SUSTRACTIVA -> "Sustractiva (analógica)"; TipoSintesis.FM -> "FM (pianos, campanas)"
                TipoSintesis.ORGANO -> "Órgano (armónicos)"; TipoSintesis.CUERDA -> "Cuerda pulsada" }
        }) { inst = inst.copy(tipo = it) }

        Seccion("Envolvente (cómo empieza y termina)", inicialAbierta = true) {
            Deslizador("Ataque", inst.ataque, 0.001f..2f, { "%.0f ms".format(it * 1000) }) { inst = inst.copy(ataque = it) }
            Deslizador("Caída", inst.caida, 0.02f..4f, { "%.2f s".format(it) }) { inst = inst.copy(caida = it) }
            if (inst.tipo != TipoSintesis.CUERDA)
                Deslizador("Sostenido", inst.sostener, 0f..1f, { "${(it * 100).toInt()} %" }) { inst = inst.copy(sostener = it) }
            Deslizador("Liberación", inst.liberar, 0.01f..3f, { "%.2f s".format(it) }) { inst = inst.copy(liberar = it) }
        }
        when (inst.tipo) {
            TipoSintesis.SUSTRACTIVA -> {
                Seccion("Osciladores") {
                    Selector("Onda 1", Onda.entries, inst.onda1, { it.name.lowercase() }) { inst = inst.copy(onda1 = it) }
                    Selector("Onda 2", Onda.entries, inst.onda2, { it.name.lowercase() }) { inst = inst.copy(onda2 = it) }
                    Deslizador("Mezcla onda 2", inst.mezcla2, 0f..1f, { "${(it * 100).toInt()} %" }) { inst = inst.copy(mezcla2 = it) }
                    Deslizador("Octava onda 2", inst.octava2.toFloat(), -2f..2f, { "%+d".format(it.toInt()) }) { inst = inst.copy(octava2 = it.toInt()) }
                    Deslizador("Grosor (desafinar)", inst.desafinar, 0f..30f, { "${it.toInt()} cents" }) { inst = inst.copy(desafinar = it) }
                    Deslizador("Vibrato", inst.vibrato, 0f..40f, { "${it.toInt()} cents" }) { inst = inst.copy(vibrato = it) }
                }
                Seccion("Filtro (brillo)") {
                    Deslizador("Corte", inst.corte, 100f..7000f, { "${it.toInt()} Hz" }) { inst = inst.copy(corte = it) }
                    Deslizador("Resonancia", inst.resonancia, 0f..0.9f, { "${(it * 100).toInt()} %" }) { inst = inst.copy(resonancia = it) }
                    Deslizador("Apertura con envolvente", inst.envFiltro, 0f..6000f, { "${it.toInt()} Hz" }) { inst = inst.copy(envFiltro = it) }
                    Deslizador("Caída del filtro", inst.caidaFiltro, 0.02f..2f, { "%.2f s".format(it) }) { inst = inst.copy(caidaFiltro = it) }
                }
            }
            TipoSintesis.FM -> Seccion("Modulación FM", inicialAbierta = true) {
                Deslizador("Relación", inst.fmRatio, 0.5f..8f, { "%.2f".format(it) }) { inst = inst.copy(fmRatio = (it * 2).toInt() / 2f) }
                Deslizador("Intensidad", inst.fmIndice, 0f..8f, { "%.1f".format(it) }) { inst = inst.copy(fmIndice = it) }
                Deslizador("Caída del brillo", inst.fmCaida, 0.02f..3f, { "%.2f s".format(it) }) { inst = inst.copy(fmCaida = it) }
                Ayuda("Relación 1 = piano eléctrico, 3,5 = campana, 4 = marimba.")
            }
            TipoSintesis.ORGANO -> Seccion("Registros", inicialAbierta = true) {
                listOf("Sub (16')", "Fundamental (8')", "Octava (4')", "Quinta", "2'", "Brillo").forEachIndexed { i, n ->
                    Deslizador(n, inst.registros[i], 0f..1f, { "${(it * 8).toInt()}" }) {
                        inst = inst.copy(registros = inst.registros.copyOf().also { r -> r[i] = it })
                    }
                }
            }
            TipoSintesis.CUERDA -> Seccion("Cuerda", inicialAbierta = true) {
                Deslizador("Brillo de la púa", inst.corte.coerceIn(0.05f, 1f), 0.05f..1f, { "${(it * 100).toInt()} %" }) { inst = inst.copy(corte = it) }
                Deslizador("Duración", inst.sostener.coerceIn(0.98f, 0.9995f), 0.98f..0.9995f, { "%.1f".format((it - 0.98f) * 1000) }) { inst = inst.copy(sostener = it) }
            }
        }
        Deslizador("Volumen", inst.volumen, 0.05f..1f, { "${(it * 100).toInt()} %" }) { inst = inst.copy(volumen = it) }

        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.padding(vertical = 8.dp)) {
            Button(onClick = {
                repo.guardar(inst.copy(nombre = nombre.ifBlank { "Mi sonido" })); propios = repo.propios()
                mensaje = "Guardado. Úsalo en Teclado y en Crear pista."
            }) { Text("Guardar sonido") }
            Spacer(Modifier.width(8.dp))
            if (propios.any { it.nombre == inst.nombre }) OutlinedButton(onClick = {
                repo.borrar(inst.nombre); propios = repo.propios(); inst = Instrumentos.PAD
            }) { Text("Borrar") }
        }
        if (mensaje.isNotEmpty()) Text(mensaje, color = MaterialTheme.colorScheme.primary)
    }
}
