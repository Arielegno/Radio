package com.estudio.musical.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.estudio.musical.audio.Wav
import com.estudio.musical.music.Teoria
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun Ayuda(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Botón de reproducir/detener para un archivo de audio */
@Composable
fun PlayButton(file: File?) {
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(file) { onDispose { player?.release(); player = null } }
    IconButton(
        enabled = file != null,
        onClick = {
            if (player != null) {
                player?.release(); player = null
            } else if (file != null && file.exists()) {
                player = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener { it.release(); player = null }
                    start()
                }
            }
        }
    ) {
        Icon(if (player != null) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = if (player != null) "Detener" else "Reproducir")
    }
}

/** Lista de grabaciones seleccionable */
@Composable
fun RecordingPicker(files: List<File>, selected: File?, onSelect: (File) -> Unit) {
    if (files.isEmpty()) {
        Ayuda("Aún no hay grabaciones. Ve a la pestaña Grabar o crea una pista en Estudio.")
        return
    }
    Column {
        files.forEach { f ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(f) }.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = f == selected, onClick = { onSelect(f) })
                Text(f.nameWithoutExtension, Modifier.weight(1f))
                PlayButton(f)
            }
        }
    }
}

/** Menú desplegable genérico */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Selector(
    etiqueta: String, opciones: List<T>, seleccion: T?, nombre: (T) -> String,
    modifier: Modifier = Modifier, onSelect: (T) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }, modifier = modifier) {
        OutlinedTextField(
            value = when { seleccion != null -> nombre(seleccion); opciones.any { it == null } -> @Suppress("UNCHECKED_CAST") nombre(null as T); else -> "—" }, onValueChange = {}, readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(abierto) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { o ->
                DropdownMenuItem(text = { Text(nombre(o)) }, onClick = { onSelect(o); abierto = false })
            }
        }
    }
}

@Composable
fun Deslizador(etiqueta: String, valor: Float, rango: ClosedFloatingPointRange<Float>, formato: (Float) -> String, onChange: (Float) -> Unit) {
    Column {
        Text("$etiqueta: ${formato(valor)}", style = MaterialTheme.typography.bodyMedium)
        Slider(valor, onChange, valueRange = rango)
    }
}

/** Pide el permiso de micrófono (y Bluetooth) y ejecuta [accion] si se concede */
@Composable
fun rememberPermisoMicrofono(): (accion: () -> Unit) -> Unit {
    val ctx = LocalContext.current
    var pendiente by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permisos = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
    }.toTypedArray()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r[Manifest.permission.RECORD_AUDIO] == true) pendiente?.invoke()
        pendiente = null
    }
    return { accion ->
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) accion()
        else { pendiente = accion; launcher.launch(permisos) }
    }
}

// ───────────────────────── Forma de onda ─────────────────────────

/** Calcula picos para dibujar (en segundo plano) */
suspend fun picosDe(file: File, columnas: Int = 500): FloatArray = withContext(Dispatchers.IO) {
    val a = Wav.read(file)
    val por = (a.samples.size / columnas).coerceAtLeast(1)
    FloatArray(columnas) { c ->
        var m = 0f
        val ini = c * por
        for (i in ini until minOf(ini + por, a.samples.size)) m = maxOf(m, abs(a.samples[i]))
        m
    }
}

@Composable
fun FormaDeOnda(picos: FloatArray?, seleccion: ClosedFloatingPointRange<Float>? = null, duracion: Float = 1f, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val sel = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
    val fondo = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.fillMaxWidth().height(110.dp)) {
        drawRoundRect(fondo, cornerRadius = CornerRadius(12f))
        if (picos == null || picos.isEmpty()) return@Canvas
        val medio = size.height / 2
        val ancho = size.width / picos.size
        if (seleccion != null && duracion > 0) {
            val x0 = seleccion.start / duracion * size.width
            val x1 = seleccion.endInclusive / duracion * size.width
            drawRect(sel, Offset(x0, 0f), Size(x1 - x0, size.height))
        }
        val max = picos.maxOrNull()?.coerceAtLeast(0.01f) ?: 1f
        picos.forEachIndexed { i, p ->
            val h = (p / max) * medio * 0.95f
            drawLine(color, Offset(i * ancho, medio - h), Offset(i * ancho, medio + h), strokeWidth = ancho.coerceAtLeast(1f))
        }
    }
}

// ───────────────────────── Teclado de piano multitáctil ─────────────────────────

private val NEGRAS = setOf(1, 3, 6, 8, 10)

@Composable
fun PianoTeclado(
    desde: Int, octavas: Int,
    modifier: Modifier = Modifier,
    resaltadas: Set<Int> = emptySet(),   // clases de nota (0..11) de la escala
    raiz: Int? = null,
    onNotaOn: (Int) -> Unit, onNotaOff: (Int) -> Unit
) {
    val notas = (desde..desde + octavas * 12)
    val blancas = notas.filter { it % 12 !in NEGRAS }
    val negras = notas.filter { it % 12 in NEGRAS }
    val presionadas = remember { mutableStateMapOf<Long, Int>() }
    val on by rememberUpdatedState(onNotaOn)
    val off by rememberUpdatedState(onNotaOff)
    val medidor = rememberTextMeasurer()
    val cPrim = MaterialTheme.colorScheme.primary
    val cEsc = MaterialTheme.colorScheme.primaryContainer
    val cTexto = MaterialTheme.colorScheme.onSurfaceVariant

    fun xDeNegra(n: Int, w: Float): Float = (blancas.indexOfFirst { it > n }) * w
    fun notaEn(p: Offset, ancho: Float, alto: Float): Int? {
        val w = ancho / blancas.size
        if (p.y < alto * 0.62f) negras.firstOrNull { abs(p.x - xDeNegra(it, w)) < w * 0.32f }?.let { return it }
        return blancas.getOrNull((p.x / w).toInt().coerceIn(0, blancas.size - 1))
    }

    Canvas(modifier.fillMaxWidth().height(170.dp).pointerInput(desde, octavas) {
        awaitPointerEventScope {
            while (true) {
                val ev = awaitPointerEvent()
                for (c in ev.changes) {
                    val id = c.id.value
                    if (c.pressed) {
                        val n = notaEn(c.position, size.width.toFloat(), size.height.toFloat())
                        val prev = presionadas[id]
                        if (n != prev) {
                            prev?.let { off(it) }
                            if (n != null) { presionadas[id] = n; on(n) } else presionadas.remove(id)
                        }
                        c.consume()
                    } else presionadas.remove(id)?.let { off(it) }
                }
            }
        }
    }) {
        val w = size.width / blancas.size
        val activas = presionadas.values.toSet()
        blancas.forEachIndexed { i, n ->
            val color = when { n in activas -> cPrim; n % 12 in resaltadas -> cEsc; else -> Color.White }
            drawRect(color, Offset(i * w, 0f), Size(w - 2, size.height))
            drawRect(Color.Gray, Offset(i * w, 0f), Size(w - 2, size.height), style = Stroke(1f))
            if (n % 12 == 0 || n % 12 == raiz) {
                val t = medidor.measure(Teoria.NOTAS[n % 12] + if (n % 12 == 0) "${n / 12 - 1}" else "",
                    TextStyle(fontSize = 9.sp, color = cTexto))
                drawText(t, topLeft = Offset(i * w + (w - t.size.width) / 2, size.height - t.size.height - 6))
            }
        }
        negras.forEach { n ->
            val x = xDeNegra(n, w)
            val color = when { n in activas -> cPrim; n % 12 in resaltadas -> cPrim.copy(alpha = 0.7f); else -> Color(0xFF222222) }
            drawRoundRect(color, Offset(x - w * 0.3f, 0f), Size(w * 0.6f, size.height * 0.62f), CornerRadius(4f))
        }
    }
}

// ───────────────────────── Diapasón de guitarra ─────────────────────────

val AFINACION_GUITARRA = listOf(64, 59, 55, 50, 45, 40) // de la 1ª (Mi agudo) a la 6ª (Mi grave)

@Composable
fun Diapason(resaltadas: Set<Int>, raiz: Int?, trastes: Int = 12, modifier: Modifier = Modifier, onNota: (Int) -> Unit) {
    val medidor = rememberTextMeasurer()
    val cPrim = MaterialTheme.colorScheme.primary
    val cEsc = MaterialTheme.colorScheme.secondary
    val cLinea = MaterialTheme.colorScheme.outline
    val cMadera = MaterialTheme.colorScheme.surfaceVariant
    val toque by rememberUpdatedState(onNota)
    Canvas(modifier.fillMaxWidth().height(180.dp).pointerInput(trastes) {
        detectTapGestures { p ->
            val cuerda = (p.y / (size.height / 6f)).toInt().coerceIn(0, 5)
            val traste = (p.x / (size.width / (trastes + 1f))).toInt().coerceIn(0, trastes)
            toque(AFINACION_GUITARRA[cuerda] + traste)
        }
    }) {
        val hc = size.height / 6f
        val wt = size.width / (trastes + 1f)
        drawRect(cMadera, Offset(wt, 0f), Size(size.width - wt, size.height))
        for (t in 1..trastes) drawLine(cLinea, Offset(t * wt, 0f), Offset(t * wt, size.height), if (t == 1) 6f else 2f)
        listOf(3, 5, 7, 9, 12).filter { it <= trastes }.forEach { t ->
            drawCircle(cLinea.copy(alpha = 0.3f), hc * 0.15f, Offset((t + 0.5f) * wt, size.height / 2))
        }
        AFINACION_GUITARRA.forEachIndexed { c, abierta ->
            val y = (c + 0.5f) * hc
            drawLine(cLinea, Offset(0f, y), Offset(size.width, y), 1f + c * 0.6f)
            for (t in 0..trastes) {
                val n = abierta + t
                val pc = n % 12
                if (pc !in resaltadas) continue
                val centro = Offset((t + 0.5f) * wt, y)
                drawCircle(if (pc == raiz) cPrim else cEsc, hc * 0.36f, centro)
                val txt = medidor.measure(Teoria.NOTAS[pc], TextStyle(fontSize = 7.sp, color = Color.White))
                drawText(txt, topLeft = centro - Offset(txt.size.width / 2f, txt.size.height / 2f))
            }
        }
    }
}
