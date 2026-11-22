package com.hilo.lector.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hilo.lector.HiloViewModel
import com.hilo.lector.data.Clase
import com.hilo.lector.data.Formula
import com.hilo.lector.data.StudyCard
import com.hilo.lector.data.today
import com.hilo.lector.text.EngMath
import com.hilo.lector.text.EngMath.eng
import com.hilo.lector.text.EngMath.num
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// =====================================================================
// Catálogo de herramientas
// =====================================================================

data class Tool(val key: String, val title: String, val desc: String, val group: String)

val TOOL_GROUPS = listOf(
    "electrica" to "Electricidad y electrónica",
    "matematicas" to "Matemáticas",
    "psicologia" to "Psicología y estadística",
    "programacion" to "Programación",
    "derecho" to "Derecho",
    "general" to "Para cualquier materia",
)

val TOOLS = listOf(
    Tool("ohm", "Ley de Ohm y potencia", "Pon dos de V, I, R o P y calcula las otras.", "electrica"),
    Tool("colores", "Código de colores", "De bandas a ohmios y de ohmios a bandas (4 y 5 bandas).", "electrica"),
    Tool("serie", "Serie y paralelo", "Resistencias, capacitores o bobinas: el equivalente.", "electrica"),
    Tool("divisor", "Divisor de tensión", "Vout, o qué resistencias usar para un Vout.", "electrica"),
    Tool("rc", "Circuitos RC y RL", "Constante de tiempo τ, frecuencia de corte y carga al 63 %.", "electrica"),
    Tool("rlc", "RLC: impedancia y resonancia", "Reactancias, |Z|, ángulo y frecuencia de resonancia.", "electrica"),
    Tool("potencia", "Potencia en alterna", "Monofásica y trifásica: P, Q, S y factor de potencia.", "electrica"),
    Tool("led", "Resistencia para un LED", "Con el valor comercial (E12) y su potencia.", "electrica"),
    Tool("t555", "555 astable", "Frecuencia y ciclo de trabajo con R1, R2 y C.", "electrica"),
    Tool("db", "Decibeles", "Ganancia de tensión y de potencia en dB, y al revés.", "electrica"),
    Tool("awg", "Calibre de cable (AWG)", "Diámetro, sección, resistencia y caída de tensión.", "electrica"),
    Tool("prefijos", "Prefijos de ingeniería", "k, M, m, µ, n, p: convierte entre ellos.", "electrica"),
    Tool("bases", "Bases numéricas", "Decimal, binario, octal, hexadecimal y complemento a 2.", "electrica"),
    Tool("verdad", "Tabla de verdad", "Escribe A·B + C' y ve la tabla completa.", "electrica"),
    Tool("calc", "Calculadora científica", "Escribe la operación completa: 2·sin(30)+√16.", "matematicas"),
    Tool("unidades", "Convertir unidades", "Longitud, masa, tiempo, energía, presión, temperatura…", "matematicas"),
    Tool("cuadratica", "Ecuación de segundo grado", "Raíces reales o complejas de ax² + bx + c.", "matematicas"),
    Tool("sistema", "Sistemas de ecuaciones", "2×2 o 3×3, con determinante.", "matematicas"),
    Tool("estadistica", "Estadística descriptiva", "Media, mediana, moda, desviación y cuartiles.", "psicologia"),
    Tool("correlacion", "Correlación de Pearson", "r, r² y su valor p entre dos variables.", "psicologia"),
    Tool("ttest", "Prueba t (dos grupos)", "Welch: t, gl, p y tamaño del efecto d.", "psicologia"),
    Tool("apa", "Referencia APA 7", "Libro, artículo, capítulo o página web.", "psicologia"),
    Tool("corrientes", "Corrientes y autores", "Las escuelas de la psicología, sus autores e ideas.", "psicologia"),
    Tool("regex", "Probar expresiones regulares", "Ve qué encuentra tu patrón en un texto.", "programacion"),
    Tool("codigo", "Tarjeta de código", "Pregunta + código con colores para repasar.", "programacion"),
    Tool("graficar", "Graficador de funciones", "Dibuja f(x), con raíces, máximos y mínimos.", "matematicas"),
    Tool("matrices", "Matrices", "Determinante, inversa, rango y sistemas de hasta 6×6.", "matematicas"),
    Tool("fasores", "Complejos y fasores", "Suma, multiplica y pone en paralelo impedancias en forma polar o rectangular.", "electrica"),
    Tool("plazos", "Plazos en días hábiles", "Vencimiento con fines de semana, feriados de Honduras y días inhábiles que agregues.", "derecho"),
    Tool("leydiff", "Comparar versiones de una ley", "Qué artículos cambiaron, se agregaron o se quitaron.", "derecho"),
    Tool("foto", "Foto a texto", "Saca una foto a la pizarra o a una página y conviértela en texto.", "general"),
    Tool("problema", "Tarjeta de problema", "Enunciado y pasos que se muestran de a uno.", "general"),
    Tool("imagen", "Imagen tapada", "Tapa partes de un diagrama o circuito y repásalas.", "general"),
    Tool("formula-card", "Tarjeta de fórmula", "La fórmula bien dibujada de un lado, su nombre del otro.", "general"),
)

/** Qué grupos mostrar primero según el campo de la clase o área. */
private fun groupsFor(field: String): List<String> = when (field) {
    "electrica" -> listOf("electrica", "matematicas", "general", "programacion")
    "matematicas" -> listOf("matematicas", "electrica", "general")
    "programacion" -> listOf("programacion", "matematicas", "general")
    "psicologia" -> listOf("psicologia", "general")
    "sociologia" -> listOf("general", "psicologia")
    "derecho" -> listOf("derecho", "general")
    else -> TOOL_GROUPS.map { it.first }
}

private fun toolsOf(group: String, field: String) = TOOLS.filter { it.group == group || (group == "programacion" && it.key in setOf("bases", "verdad", "calc")) }
    .let { if (field == "sociologia" && group == "psicologia") it.filter { t -> t.key in setOf("estadistica", "correlacion", "apa") } else it }
    .distinctBy { it.key }

/** Lista de herramientas (en una clase o área, o todas). Al tocar una se abre a pantalla completa. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsList(vm: HiloViewModel, field: String, classId: String? = null, header: Boolean = true) {
    val c = LocalHilo.current
    var open by remember { mutableStateOf<Tool?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (header) Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Herramientas", color = c.ink, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            HelpButton(vm, "herramientas")
        }
        Text("Funcionan sin internet. Las tarjetas que crees van a tu repaso.", color = c.muted, fontSize = 13.sp)
        groupsFor(field).forEach { g ->
            val list = toolsOf(g, field)
            if (list.isEmpty()) return@forEach
            Text(TOOL_GROUPS.first { it.first == g }.second, color = c.accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            list.forEach { t ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).clickable { open = t }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(c.band), contentAlignment = Alignment.Center) {
                        Text(toolGlyph(t.key), color = c.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(t.title, color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(t.desc, color = c.muted, fontSize = 13.sp, lineHeight = 17.sp)
                    }
                    Text("›", color = c.muted, fontSize = 22.sp)
                }
            }
        }
    }
    open?.let { t ->
        Dialog(onDismissRequest = { open = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            if (t.key == "plazos") extraPractice(vm, null, null, "plazo-calc") { open = null }
            else OverlayFrame(t.title, { open = null }) { ToolBody(vm, t.key, classId) }
        }
    }
}

/** Abre una herramienta suelta (desde la búsqueda universal). */
@Composable
fun ToolDialog(vm: HiloViewModel, key: String, onClose: () -> Unit) {
    val t = TOOLS.firstOrNull { it.key == key } ?: return
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        if (t.key == "plazos") extraPractice(vm, null, null, "plazo-calc", onClose)
        else OverlayFrame(t.title, onClose) { ToolBody(vm, t.key, null) }
    }
}

/** Pantalla con todas las herramientas (desde Estudiar). */
@Composable
fun ToolsScreen(vm: HiloViewModel, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        OverlayFrame("Herramientas", onClose) { ToolsList(vm, "otro", header = false) }
    }
}

private fun toolGlyph(k: String) = when (k) {
    "ohm" -> "Ω"; "colores" -> "▥"; "serie" -> "∥"; "divisor" -> "÷"; "rc" -> "τ"; "rlc" -> "Z"; "potencia" -> "W"; "led" -> "◗"
    "t555" -> "⏲"; "db" -> "dB"; "awg" -> "⌀"; "prefijos" -> "µ"; "bases" -> "01"; "verdad" -> "∧"; "calc" -> "√"; "unidades" -> "⇄"
    "cuadratica" -> "x²"; "sistema" -> "{ }"; "estadistica" -> "x̄"; "correlacion" -> "r"; "ttest" -> "t"; "apa" -> "“ ”"; "corrientes" -> "Ψ"
    "plazos" -> "⏳"; "graficar" -> "ƒ"; "matrices" -> "[ ]"; "fasores" -> "∠"; "leydiff" -> "§"; "foto" -> "📷"
    "regex" -> ".*"; "codigo" -> "</>"; "problema" -> "1·2"; "imagen" -> "▣"; "formula-card" -> "∑"; else -> "•"
}

@Composable
private fun ToolBody(vm: HiloViewModel, key: String, classId: String?) {
    when (key) {
        "ohm" -> OhmTool()
        "colores" -> ColorCodeTool()
        "serie" -> SeriesTool()
        "divisor" -> DividerTool()
        "rc" -> RcTool()
        "rlc" -> RlcTool()
        "potencia" -> AcPowerTool()
        "led" -> LedTool()
        "t555" -> Timer555Tool()
        "db" -> DbTool()
        "awg" -> AwgTool()
        "prefijos" -> PrefixTool()
        "bases" -> BasesTool()
        "verdad" -> TruthTool()
        "calc" -> CalcTool()
        "unidades" -> UnitsTool()
        "cuadratica" -> QuadraticTool()
        "sistema" -> SystemTool()
        "estadistica" -> StatsTool()
        "correlacion" -> CorrTool()
        "ttest" -> TTestTool()
        "apa" -> ApaTool(vm)
        "corrientes" -> PsyKbTool()
        "regex" -> RegexTool()
        "codigo" -> CodeCardTool(vm, classId)
        "problema" -> ProblemCardTool(vm, classId)
        "imagen" -> OcclusionEditor(vm, classId)
        "formula-card" -> FormulaCardTool(vm, classId)
        "graficar" -> GraphTool()
        "matrices" -> MatrixTool()
        "fasores" -> PhasorTool()
        "leydiff" -> LawDiffTool(vm)
        "foto" -> PhotoTextTool(vm, classId)
    }
}

// =====================================================================
// Piezas comunes
// =====================================================================

@Composable
private fun Num(label: String, value: String, hint: String = "", modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange, label = { Text(label) }, singleLine = true, modifier = modifier,
        placeholder = { if (hint.isNotEmpty()) Text(hint) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
    )
}

@Composable
internal fun Out(vararg kv: String) {
    val c = LocalHilo.current
    val lines = kv.toList().chunked(2).filter { it.size == 2 }.map { it[0] to it[1] }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.band).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        lines.forEach { (k, v) ->
            // Resultados largos (listas de raíces, fórmulas) van debajo de su nombre para no apretarlo
            if (v.length > 16 || v.contains('\n')) Column {
                if (k.isNotBlank()) Text(k, color = c.muted, fontSize = 14.sp)
                Text(v, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace, lineHeight = 22.sp)
            } else Row(verticalAlignment = Alignment.CenterVertically) {
                Text(k, color = c.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(v, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
internal fun Note(text: String) {
    val c = LocalHilo.current
    Text(text, color = c.muted, fontSize = 13.sp, lineHeight = 18.sp)
}

@Composable
internal fun Eq(tex: String) {
    val c = LocalHilo.current
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(horizontal = 10.dp, vertical = 4.dp)) { MathView("$$" + tex + "$$", c.ink, 17f) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Choice(options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (k, l) -> Pill(l, k == selected) { onPick(k) } }
    }
}

private fun p(s: String) = EngMath.parse(s)

// =====================================================================
// Electricidad y electrónica
// =====================================================================

@Composable
private fun OhmTool() {
    var v by remember { mutableStateOf("") }; var i by remember { mutableStateOf("") }
    var r by remember { mutableStateOf("") }; var w by remember { mutableStateOf("") }
    Eq("V = I\\cdot R \\qquad P = V\\cdot I = I^2R = \\frac{V^2}{R}")
    Note("Escribe dos valores (acepta 4.7k, 20m, 1M…). Deja vacíos los que quieras calcular.")
    Num("Tensión V (voltios)", v) { v = it }; Num("Corriente I (amperios)", i) { i = it }
    Num("Resistencia R (ohmios)", r) { r = it }; Num("Potencia P (vatios)", w) { w = it }
    val V = p(v); val I = p(i); val R = p(r); val P = p(w)
    val known = listOfNotNull(V, I, R, P).size
    if (known < 2) { Note("Faltan datos: necesito dos."); return }
    val (vv, ii) = when {
        V != null && I != null -> V to I
        V != null && R != null -> V to V / R
        V != null && P != null -> V to P / V
        I != null && R != null -> I * R to I
        I != null && P != null -> P / I to I
        R != null && P != null -> sqrt(P * R) to sqrt(P / R)
        else -> return
    }
    Out("Tensión V", eng(vv, "V"), "Corriente I", eng(ii, "A"), "Resistencia R", eng(vv / ii, "Ω"), "Potencia P", eng(vv * ii, "W"))
    if (known > 2) Note("Pusiste más de dos datos: usé los dos primeros (V, I, R, P en ese orden).")
}

private val BAND_LABELS = EngMath.COLORS

@Composable
private fun ColorCodeTool() {
    val c = LocalHilo.current
    var five by remember { mutableStateOf(false) }
    val bands = remember { mutableStateListOf(1, 0, 2, 10) }      // marrón negro rojo dorado = 1 kΩ ±5 %
    var toValue by remember { mutableStateOf("") }
    Choice(listOf("4" to "4 bandas", "5" to "5 bandas"), if (five) "5" else "4") {
        five = it == "5"
        bands.clear(); bands.addAll(if (five) listOf(1, 0, 0, 1, 1) else listOf(1, 0, 2, 10))
    }
    val n = if (five) 3 else 2
    val digits = bands.take(n).joinToString("") { it.toString() }.toInt()
    val multIdx = bands[n]
    val value = digits * EngMath.MULT[multIdx]
    val tolIdx = bands[n + 1]
    ResistorDrawing(bands.take(n).map { EngMath.COLOR_RGB[it] } + EngMath.COLOR_RGB[multIdx] + EngMath.COLOR_RGB[tolColor(tolIdx)])
    Out("Valor", eng(value, "Ω"), "Tolerancia", EngMath.TOL.firstOrNull { it.first == tolIdx }?.second ?: "")
    (0 until n).forEach { b ->
        Text("Cifra ${b + 1}", color = c.muted, fontSize = 13.sp)
        ColorRow((if (b == 0) 1 else 0)..9, bands[b]) { bands[b] = it }
    }
    Text("Multiplicador", color = c.muted, fontSize = 13.sp)
    ColorRow(0..11, multIdx) { bands[n] = it }
    Text("Tolerancia", color = c.muted, fontSize = 13.sp)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        EngMath.TOL.forEach { (k, l) -> Pill("${colorName(tolColor(k))} $l", k == tolIdx) { bands[n + 1] = k } }
    }
    SectionDivider("Al revés: de valor a colores")
    Num("Valor en ohmios", toValue, "por ej. 4.7k o 220") { toValue = it }
    p(toValue)?.let { x ->
        val b4 = EngMath.bands(x, 2); val b5 = EngMath.bands(x, 3)
        if (b4 == null && b5 == null) {
            val near = EngMath.nearestE(x, EngMath.E24)
            Note("Ese valor no existe con 2 o 3 cifras. El comercial más cercano (E24) es ${eng(near, "Ω")}.")
        }
        b4?.let { Text("4 bandas: " + it.joinToString(" · ") { k -> colorName(k) } + " · dorado", color = c.ink, fontSize = 15.sp); ResistorDrawing(it.map { k -> EngMath.COLOR_RGB[k] } + EngMath.COLOR_RGB[10]) }
        b5?.let { Text("5 bandas: " + it.joinToString(" · ") { k -> colorName(k) } + " · marrón", color = c.ink, fontSize = 15.sp); ResistorDrawing(it.map { k -> EngMath.COLOR_RGB[k] } + EngMath.COLOR_RGB[1]) }
    }
}

private fun tolColor(k: Int) = k               // los índices de tolerancia ya son colores (10 dorado, 11 plateado)
private fun colorName(k: Int) = when (k) { 10 -> "dorado"; 11 -> "plateado"; else -> EngMath.COLORS[k] }

@Composable
private fun SectionDivider(title: String) {
    val c = LocalHilo.current
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        Text(title, color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun ColorRow(range: IntRange, selected: Int, onPick: (Int) -> Unit) {
    val c = LocalHilo.current
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        range.forEach { k ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onPick(k) }) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Color(EngMath.COLOR_RGB[k]))
                        .border(if (k == selected) 3.dp else 1.dp, if (k == selected) c.accent else c.line, CircleShape),
                )
                Text(if (k >= 10) colorName(k).take(4) else colorName(k).take(4), color = if (k == selected) c.accent else c.muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ResistorDrawing(colors: List<Int>) {
    val c = LocalHilo.current
    Canvas(Modifier.fillMaxWidth().height(70.dp)) {
        val w = size.width; val h = size.height
        drawLine(c.muted, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 6f)
        val bw = w * 0.56f; val left = (w - bw) / 2
        drawRoundRect(Color(0xFFE8D3A9), Offset(left, h * 0.15f), Size(bw, h * 0.7f), CornerRadius(h * 0.3f))
        val step = bw / (colors.size + 1.5f)
        colors.forEachIndexed { i, col ->
            val x = left + step * (i + 0.8f) + if (i == colors.size - 1) step * 0.6f else 0f
            drawRect(Color(col), Offset(x, h * 0.15f), Size(step * 0.42f, h * 0.7f))
            if (col == 0xFFFFFFFF.toInt()) drawRect(Color.Gray, Offset(x, h * 0.15f), Size(step * 0.42f, h * 0.7f), style = Stroke(1f))
        }
    }
}

@Composable
private fun SeriesTool() {
    var kind by remember { mutableStateOf("R") }
    var vals by remember { mutableStateOf("") }
    Choice(listOf("R" to "Resistencias", "C" to "Capacitores", "L" to "Bobinas"), kind) { kind = it }
    val unit = when (kind) { "R" -> "Ω"; "C" -> "F"; else -> "H" }
    Eq(if (kind == "C") "C_{serie}=\\left(\\sum \\tfrac{1}{C_i}\\right)^{-1}\\qquad C_{par}=\\sum C_i" else "${kind}_{serie}=\\sum ${kind}_i\\qquad ${kind}_{par}=\\left(\\sum \\tfrac{1}{${kind}_i}\\right)^{-1}")
    OutlinedTextField(vals, { vals = it }, label = { Text("Valores separados por espacio o ;") }, placeholder = { Text("10k 4.7k 2.2k") }, modifier = Modifier.fillMaxWidth())
    val xs = vals.split(Regex("[;\\s]+")).mapNotNull { p(it) }.filter { it > 0 }
    if (xs.size < 2) { Note("Escribe al menos dos valores."); return }
    val sum = xs.sum(); val inv = 1 / xs.sumOf { 1 / it }
    val (ser, par) = if (kind == "C") inv to sum else sum to inv
    Out("En serie", eng(ser, unit), "En paralelo", eng(par, unit), "Cantidad", "${xs.size}")
}

@Composable
private fun DividerTool() {
    var vin by remember { mutableStateOf("12") }; var r1 by remember { mutableStateOf("10k") }; var r2 by remember { mutableStateOf("4.7k") }
    var want by remember { mutableStateOf("") }; var load by remember { mutableStateOf("") }
    Eq("V_{out}=V_{in}\\cdot\\frac{R_2}{R_1+R_2}")
    Num("Vin (V)", vin) { vin = it }; Num("R1 (arriba)", r1) { r1 = it }; Num("R2 (abajo)", r2) { r2 = it }
    Num("Carga en la salida (opcional)", load, "por ej. 10k") { load = it }
    val Vi = p(vin); val R1 = p(r1); val R2 = p(r2); val RL = p(load)
    if (Vi != null && R1 != null && R2 != null) {
        val r2e = if (RL != null && RL > 0) R2 * RL / (R2 + RL) else R2
        val vo = Vi * r2e / (R1 + r2e)
        Out("Vout", eng(vo, "V"), "Corriente", eng(Vi / (R1 + r2e), "A"), "Potencia en R1", eng((Vi - vo).pow(2) / R1, "W"))
        if (RL != null) Note("Con la carga, R2 queda en paralelo con ella (${eng(r2e, "Ω")}).")
    }
    SectionDivider("¿Qué R2 necesito?")
    Num("Vout que quieres (V)", want) { want = it }
    val W = p(want)
    if (Vi != null && R1 != null && W != null && W > 0 && W < Vi) {
        val r = R1 * W / (Vi - W)
        val e = EngMath.nearestE(r, EngMath.E12)
        Out("R2 exacta", eng(r, "Ω"), "R2 comercial (E12)", eng(e, "Ω"), "Vout con esa", eng(Vi * e / (R1 + e), "V"))
    }
}

@Composable
private fun RcTool() {
    var kind by remember { mutableStateOf("RC") }
    var r by remember { mutableStateOf("10k") }; var x by remember { mutableStateOf("100n") }
    Choice(listOf("RC" to "RC", "RL" to "RL"), kind) { kind = it }
    Eq(if (kind == "RC") "\\tau = R\\,C \\qquad f_c=\\frac{1}{2\\pi R C}" else "\\tau = \\frac{L}{R} \\qquad f_c=\\frac{R}{2\\pi L}")
    Num("R (ohmios)", r) { r = it }
    Num(if (kind == "RC") "C (faradios)" else "L (henrios)", x) { x = it }
    val R = p(r); val X = p(x)
    if (R == null || X == null || R <= 0 || X <= 0) return
    val tau = if (kind == "RC") R * X else X / R
    Out("τ (constante de tiempo)", eng(tau, "s"), "Frecuencia de corte", eng(1 / (2 * PI * tau), "Hz"), "Al 63 % (1τ)", eng(tau, "s"), "Casi completo (5τ)", eng(5 * tau, "s"))
    Note("En 1τ llega al 63 %, en 3τ al 95 % y en 5τ a más del 99 %.")
}

@Composable
private fun RlcTool() {
    var r by remember { mutableStateOf("100") }; var l by remember { mutableStateOf("10m") }; var cc by remember { mutableStateOf("1u") }; var f by remember { mutableStateOf("1k") }
    var par by remember { mutableStateOf(false) }
    Choice(listOf("s" to "En serie", "p" to "En paralelo"), if (par) "p" else "s") { par = it == "p" }
    Eq("X_L=2\\pi f L\\quad X_C=\\frac{1}{2\\pi f C}\\quad f_0=\\frac{1}{2\\pi\\sqrt{LC}}")
    Num("R (Ω)", r) { r = it }; Num("L (H)", l) { l = it }; Num("C (F)", cc) { cc = it }; Num("Frecuencia f (Hz)", f) { f = it }
    val R = p(r); val L = p(l); val C = p(cc); val F = p(f)
    if (R == null || L == null || C == null || F == null || F <= 0 || L <= 0 || C <= 0) return
    val xl = 2 * PI * F * L; val xc = 1 / (2 * PI * F * C)
    val f0 = 1 / (2 * PI * sqrt(L * C))
    val (zm, ang) = if (!par) {
        val x = xl - xc; sqrt(R * R + x * x) to Math.toDegrees(atan(x / R))
    } else {
        val g = 1 / R; val b = 1 / xc - 1 / xl
        val ym = sqrt(g * g + b * b); 1 / ym to -Math.toDegrees(atan(b / g))
    }
    val q = if (!par) sqrt(L / C) / R else R * sqrt(C / L)
    Out("X_L", eng(xl, "Ω"), "X_C", eng(xc, "Ω"), "|Z|", eng(zm, "Ω"), "Ángulo", num(ang, 3) + "°", "Resonancia f₀", eng(f0, "Hz"), "Factor Q", num(q, 3))
    Note(if (abs(ang) < 1) "Está casi en resonancia: se comporta como resistivo." else if (ang > 0) "Es inductivo: la corriente va atrasada." else "Es capacitivo: la corriente va adelantada.")
}

@Composable
private fun AcPowerTool() {
    var three by remember { mutableStateOf(false) }
    var v by remember { mutableStateOf("120") }; var i by remember { mutableStateOf("10") }; var pf by remember { mutableStateOf("0.85") }
    Choice(listOf("1" to "Monofásica", "3" to "Trifásica"), if (three) "3" else "1") { three = it == "3" }
    Eq(if (three) "S=\\sqrt{3}\\,V_L I_L\\quad P=S\\cos\\varphi\\quad Q=S\\sin\\varphi" else "S=V I\\quad P=S\\cos\\varphi\\quad Q=S\\sin\\varphi")
    Num(if (three) "Tensión de línea V_L (V)" else "Tensión V (V eficaces)", v) { v = it }
    Num("Corriente (A eficaces)", i) { i = it }; Num("Factor de potencia cos φ", pf) { pf = it }
    val V = p(v); val I = p(i); val PF = p(pf)
    if (V == null || I == null || PF == null || PF < 0 || PF > 1) return
    val s = (if (three) sqrt(3.0) else 1.0) * V * I
    val phi = kotlin.math.acos(PF)
    Out("Aparente S", eng(s, "VA"), "Activa P", eng(s * PF, "W"), "Reactiva Q", eng(s * kotlin.math.sin(phi), "VAR"), "Ángulo φ", num(Math.toDegrees(phi), 3) + "°")
    SectionDivider("Corregir el factor de potencia")
    var target by remember { mutableStateOf("0.95") }; var freq by remember { mutableStateOf("60") }
    Num("Factor que quieres", target) { target = it }; Num("Frecuencia de la red (Hz)", freq) { freq = it }
    val T = p(target); val Fq = p(freq)
    if (T != null && Fq != null && T > PF && T <= 1) {
        val P = s * PF
        val qc = P * (kotlin.math.tan(phi) - kotlin.math.tan(kotlin.math.acos(T)))
        val cap = qc / (2 * PI * Fq * V * V) / (if (three) 3 else 1)
        Out("Reactiva a compensar", eng(qc, "VAR"), if (three) "C por fase (en triángulo)" else "Capacitor", eng(cap, "F"))
    }
}

@Composable
private fun LedTool() {
    var vs by remember { mutableStateOf("5") }; var vf by remember { mutableStateOf("2") }; var ia by remember { mutableStateOf("20m") }; var n by remember { mutableStateOf("1") }
    Eq("R=\\frac{V_s - n\\,V_f}{I}")
    Num("Fuente Vs (V)", vs) { vs = it }; Num("Caída del LED Vf (V)", vf, "rojo ≈2, azul/blanco ≈3") { vf = it }
    Num("Corriente (A)", ia, "20m = 20 mA") { ia = it }; Num("LEDs en serie", n) { n = it }
    val Vs = p(vs); val Vf = p(vf); val I = p(ia); val N = n.toIntOrNull() ?: 1
    if (Vs == null || Vf == null || I == null || I <= 0) return
    val drop = Vs - N * Vf
    if (drop <= 0) { Note("La fuente no alcanza para encender $N LED(s) en serie."); return }
    val r = drop / I
    val e = EngMath.nearestE(r, EngMath.E12, up = true)
    val realI = drop / e; val pw = drop * realI
    val rating = listOf(0.125, 0.25, 0.5, 1.0, 2.0, 5.0).firstOrNull { it >= pw * 2 } ?: 5.0
    Out("R exacta", eng(r, "Ω"), "R comercial (E12, hacia arriba)", eng(e, "Ω"), "Corriente real", eng(realI, "A"), "Potencia en R", eng(pw, "W"), "Usa una de", "${num(rating)} W")
}

@Composable
private fun Timer555Tool() {
    var r1 by remember { mutableStateOf("1k") }; var r2 by remember { mutableStateOf("10k") }; var cc by remember { mutableStateOf("10u") }
    Eq("f=\\frac{1{,}44}{(R_1+2R_2)\\,C}\\qquad D=\\frac{R_1+R_2}{R_1+2R_2}")
    Num("R1", r1) { r1 = it }; Num("R2", r2) { r2 = it }; Num("C", cc) { cc = it }
    val R1 = p(r1); val R2 = p(r2); val C = p(cc)
    if (R1 == null || R2 == null || C == null) return
    val th = 0.693 * (R1 + R2) * C; val tl = 0.693 * R2 * C
    Out("Frecuencia", eng(1 / (th + tl), "Hz"), "Periodo", eng(th + tl, "s"), "Tiempo en alto", eng(th, "s"), "Tiempo en bajo", eng(tl, "s"), "Ciclo de trabajo", num(100 * th / (th + tl), 3) + " %")
}

@Composable
private fun DbTool() {
    var a by remember { mutableStateOf("1") }; var b by remember { mutableStateOf("10") }; var db by remember { mutableStateOf("") }
    Eq("G_V = 20\\log_{10}\\frac{V_2}{V_1}\\qquad G_P = 10\\log_{10}\\frac{P_2}{P_1}")
    Num("Entrada (V o W)", a) { a = it }; Num("Salida (V o W)", b) { b = it }
    val A = p(a); val B = p(b)
    if (A != null && B != null && A > 0 && B > 0) Out("Si son tensiones", num(20 * log10(B / A), 4) + " dB", "Si son potencias", num(10 * log10(B / A), 4) + " dB")
    SectionDivider("De dB a veces")
    Num("Decibeles", db) { db = it }
    p(db)?.let { d -> Out("En tensión", "×" + num(10.0.pow(d / 20)), "En potencia", "×" + num(10.0.pow(d / 10))) }
    Note("−3 dB = la mitad de potencia (≈0,707 en tensión). +6 dB ≈ el doble de tensión.")
}

@Composable
private fun AwgTool() {
    var g by remember { mutableStateOf("12") }; var len by remember { mutableStateOf("20") }; var amp by remember { mutableStateOf("15") }; var volt by remember { mutableStateOf("120") }
    Num("Calibre AWG", g) { g = it }; Num("Largo del cable (m, solo ida)", len) { len = it }
    Num("Corriente (A)", amp) { amp = it }; Num("Tensión (V)", volt) { volt = it }
    val G = g.toIntOrNull() ?: return; val Lm = p(len); val I = p(amp); val V = p(volt)
    if (G !in 0..40) return
    val ohm = EngMath.awgOhmM(G)
    Out("Diámetro", num(EngMath.awgMm(G), 3) + " mm", "Sección", num(EngMath.awgArea(G), 3) + " mm²", "Resistencia", eng(ohm, "Ω/m"))
    if (Lm != null && I != null) {
        val drop = 2 * Lm * ohm * I
        Out("Caída (ida y vuelta)", eng(drop, "V"), "En porcentaje", if (V != null && V > 0) num(100 * drop / V, 3) + " %" else "—", "Pérdida", eng(drop * I, "W"))
        if (V != null && 100 * drop / V > 3) Note("Más de 3 % de caída: usa un calibre más grueso (número AWG más bajo).")
    }
    Note("Cobre a 20 °C. La corriente máxima depende del aislante, la instalación y el código eléctrico de tu país.")
}

@Composable
private fun PrefixTool() {
    var v by remember { mutableStateOf("0.0047") }
    Num("Valor (acepta 4.7m, 22u, 1e-6…)", v) { v = it }
    val x = p(v) ?: return
    Out("Ingeniería", eng(x), "Científica", String.format(java.util.Locale.US, "%.4e", x).replace(".", ","),
        "En mili (m)", num(x * 1e3) + " m", "En micro (µ)", num(x * 1e6) + " µ", "En nano (n)", num(x * 1e9) + " n", "En kilo (k)", num(x / 1e3) + " k", "En mega (M)", num(x / 1e6) + " M")
}

@Composable
private fun BasesTool() {
    val c = LocalHilo.current
    var txt by remember { mutableStateOf("202") }; var base by remember { mutableStateOf("10") }; var bits by remember { mutableStateOf("8") }
    Choice(listOf("10" to "Decimal", "2" to "Binario", "8" to "Octal", "16" to "Hexadecimal"), base) { base = it }
    Num("Número", txt) { txt = it }
    val n = runCatching { java.lang.Long.parseLong(txt.trim().removePrefix("0x").removePrefix("0b").replace("_", "").replace(" ", ""), base.toInt()) }.getOrNull()
    if (n == null) { Note("Ese número no es válido en base $base."); return }
    fun grp(s: String, k: Int) = s.reversed().chunked(k).joinToString(" ").reversed()
    Out("Decimal", n.toString(), "Binario", grp(java.lang.Long.toBinaryString(n), 4), "Octal", java.lang.Long.toOctalString(n), "Hexadecimal", java.lang.Long.toHexString(n).uppercase())
    SectionDivider("Complemento a 2")
    Choice(listOf("8" to "8 bits", "16" to "16 bits", "32" to "32 bits"), bits) { bits = it }
    val b = bits.toInt()
    val min = -(1L shl (b - 1)); val max = (1L shl (b - 1)) - 1
    if (n < min || n > (1L shl b) - 1) { Note("No cabe en $b bits."); return }
    val pattern = java.lang.Long.toBinaryString(n and ((1L shl b) - 1)).padStart(b, '0')
    val signed = if (n > max) n - (1L shl b) else n
    val neg = java.lang.Long.toBinaryString((-n) and ((1L shl b) - 1)).padStart(b, '0')
    Out("Patrón", grp(pattern, 4), "Leído con signo", signed.toString(), "−$n en C2", grp(neg, 4))
    Text("Rango con signo en $b bits: $min a $max", color = c.muted, fontSize = 13.sp)
}

@Composable
private fun TruthTool() {
    val c = LocalHilo.current
    var e by remember { mutableStateOf("A·B + C'") }
    OutlinedTextField(e, { e = it }, label = { Text("Expresión") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Note("Letras A a F. Y: · & AND (o juntas: AB) · O: + | OR · NO: ' ! NOT · XOR: ^ ⊕ · también NAND y NOR.")
    val r = runCatching { EngMath.truthTable(e) }
    r.exceptionOrNull()?.let { Text(it.message ?: "No entiendo la expresión.", color = c.lines[0]); return }
    val (vars, rows) = r.getOrThrow()
    if (vars.isEmpty()) return
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(10.dp)) {
        Row { (vars + "S").forEach { Text(it, color = c.accent, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(40.dp)) } }
        rows.forEach { (vals, out) ->
            Row(Modifier.background(if (out) c.band else Color.Transparent)) {
                vals.forEach { Text(if (it) "1" else "0", color = c.ink, fontFamily = FontFamily.Monospace, modifier = Modifier.width(40.dp)) }
                Text(if (out) "1" else "0", color = if (out) c.accent else c.muted, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(40.dp))
            }
        }
    }
    val ones = rows.mapIndexedNotNull { i, (_, o) -> if (o) i else null }
    Out("Minitérminos Σm", if (ones.isEmpty()) "ninguno" else ones.joinToString(","))
    val sop = rows.filter { it.second }.joinToString(" + ") { (vals, _) -> vars.zip(vals).joinToString("") { (n, v) -> if (v) n else "$n'" } }
    if (sop.isNotEmpty() && ones.size < rows.size) Text("Suma de productos: $sop", color = c.ink, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
}

// =====================================================================
// Matemáticas
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalcTool() {
    val c = LocalHilo.current
    var e by remember { mutableStateOf("") }
    var deg by remember { mutableStateOf(true) }
    val hist = remember { mutableStateListOf<Pair<String, String>>() }
    OutlinedTextField(e, { e = it }, label = { Text("Operación") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, fontFamily = FontFamily.Monospace))
    val res = remember(e, deg) { if (e.isBlank()) null else runCatching { EngMath.eval(e, deg) } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(res?.getOrNull()?.let { "= " + num(it, 10) } ?: res?.exceptionOrNull()?.message ?: "", color = if (res?.isSuccess == true) c.accent else c.muted,
            fontSize = if (res?.isSuccess == true) 26.sp else 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("Grados", color = c.muted, fontSize = 13.sp); Switch(deg, { deg = it })
    }
    val keys = listOf("7", "8", "9", "÷", "sin(", "4", "5", "6", "×", "cos(", "1", "2", "3", "−", "tan(", "0", ",", "(", ")", "+", "√(", "^", "π", "e", "ln(", "log(", "!", "%")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = 5) {
        keys.forEach { k ->
            Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp)).background(c.surface).clickable { e += k }, contentAlignment = Alignment.Center) {
                Text(k.removeSuffix("("), color = c.ink, fontSize = 17.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { e = e.dropLast(1) }) { Text("⌫", color = c.ink) }
        OutlinedButton(onClick = { e = "" }) { Text("Borrar", color = c.ink) }
        Button(onClick = { res?.getOrNull()?.let { hist.add(0, e to num(it, 10)); e = num(it, 10).replace(",", ".") } },
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("=") }
    }
    hist.take(8).forEach { (q, a) -> Text("$q = $a", color = c.muted, fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.clickable { e = q }) }
    Note("Acepta prefijos: 4.7k, 100n, 2m. Puedes escribir 2pi o 3(4+1).")
}

private data class UnitDef(val name: String, val factor: Double)
private val UNITS: Map<String, List<UnitDef>> = linkedMapOf(
    "Longitud" to listOf(UnitDef("m", 1.0), UnitDef("km", 1e3), UnitDef("cm", 0.01), UnitDef("mm", 1e-3), UnitDef("µm", 1e-6), UnitDef("pulgada", 0.0254), UnitDef("pie", 0.3048), UnitDef("yarda", 0.9144), UnitDef("milla", 1609.344)),
    "Masa" to listOf(UnitDef("kg", 1.0), UnitDef("g", 1e-3), UnitDef("mg", 1e-6), UnitDef("tonelada", 1e3), UnitDef("libra", 0.45359237), UnitDef("onza", 0.028349523)),
    "Tiempo" to listOf(UnitDef("s", 1.0), UnitDef("ms", 1e-3), UnitDef("µs", 1e-6), UnitDef("min", 60.0), UnitDef("h", 3600.0), UnitDef("día", 86400.0)),
    "Área" to listOf(UnitDef("m²", 1.0), UnitDef("cm²", 1e-4), UnitDef("mm²", 1e-6), UnitDef("km²", 1e6), UnitDef("hectárea", 1e4), UnitDef("manzana (HN)", 6988.96), UnitDef("pie²", 0.09290304)),
    "Volumen" to listOf(UnitDef("L", 1.0), UnitDef("mL", 1e-3), UnitDef("m³", 1e3), UnitDef("galón (EE. UU.)", 3.785411784), UnitDef("pie³", 28.316846592)),
    "Velocidad" to listOf(UnitDef("m/s", 1.0), UnitDef("km/h", 1 / 3.6), UnitDef("mi/h", 0.44704), UnitDef("nudo", 0.514444)),
    "Energía" to listOf(UnitDef("J", 1.0), UnitDef("kJ", 1e3), UnitDef("Wh", 3600.0), UnitDef("kWh", 3.6e6), UnitDef("cal", 4.184), UnitDef("kcal", 4184.0), UnitDef("BTU", 1055.06), UnitDef("eV", 1.602176634e-19)),
    "Potencia" to listOf(UnitDef("W", 1.0), UnitDef("kW", 1e3), UnitDef("MW", 1e6), UnitDef("HP", 745.7), UnitDef("CV", 735.5), UnitDef("BTU/h", 0.29307107)),
    "Presión" to listOf(UnitDef("Pa", 1.0), UnitDef("kPa", 1e3), UnitDef("bar", 1e5), UnitDef("atm", 101325.0), UnitDef("psi", 6894.757), UnitDef("mmHg", 133.322)),
    "Datos" to listOf(UnitDef("byte", 1.0), UnitDef("bit", 0.125), UnitDef("KB", 1e3), UnitDef("MB", 1e6), UnitDef("GB", 1e9), UnitDef("KiB", 1024.0), UnitDef("MiB", 1048576.0), UnitDef("GiB", 1073741824.0)),
    "Ángulo" to listOf(UnitDef("grado", 1.0), UnitDef("radián", 180 / PI), UnitDef("gradián", 0.9), UnitDef("vuelta", 360.0)),
    "Temperatura" to listOf(UnitDef("°C", 0.0), UnitDef("°F", 0.0), UnitDef("K", 0.0)),
)

@Composable
private fun UnitsTool() {
    var cat by remember { mutableStateOf("Longitud") }
    var from by remember { mutableIntStateOf(0) }
    var v by remember { mutableStateOf("1") }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        UNITS.keys.forEach { k -> Pill(k, k == cat) { cat = k; from = 0 } }
    }
    val list = UNITS.getValue(cat)
    Choice(list.mapIndexed { i, u -> i.toString() to u.name }, from.toString()) { from = it.toInt() }
    Num("Valor en ${list[from].name}", v) { v = it }
    val x = v.replace(",", ".").toDoubleOrNull() ?: return
    val rows = if (cat == "Temperatura") {
        val cel = when (from) { 0 -> x; 1 -> (x - 32) * 5 / 9; else -> x - 273.15 }
        listOf("°C" to num(cel, 6), "°F" to num(cel * 9 / 5 + 32, 6), "K" to num(cel + 273.15, 6))
    } else list.map { u -> u.name to num(x * list[from].factor / u.factor, 6) }
    Out(*rows.flatMap { listOf(it.first, it.second) }.toTypedArray())
}

@Composable
private fun QuadraticTool() {
    var a by remember { mutableStateOf("1") }; var b by remember { mutableStateOf("-3") }; var cc by remember { mutableStateOf("2") }
    Eq("x=\\frac{-b\\pm\\sqrt{b^2-4ac}}{2a}")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Num("a", a, modifier = Modifier.weight(1f)) { a = it }; Num("b", b, modifier = Modifier.weight(1f)) { b = it }; Num("c", cc, modifier = Modifier.weight(1f)) { cc = it }
    }
    val A = a.replace(",", ".").toDoubleOrNull(); val B = b.replace(",", ".").toDoubleOrNull(); val C = cc.replace(",", ".").toDoubleOrNull()
    if (A == null || B == null || C == null) return
    val d = B * B - 4 * A * C
    val r = EngMath.quadratic(A, B, C)
    fun show(z: Pair<Double, Double>) = if (z.second == 0.0) num(z.first, 6) else num(z.first, 5) + (if (z.second > 0) " + " else " − ") + num(abs(z.second), 5) + "i"
    Out(
        "Discriminante", num(d, 6),
        *r.flatMapIndexed { i, z -> listOf("x${i + 1}", show(z)) }.toTypedArray(),
        *(if (A != 0.0) arrayOf("Vértice", "(${num(-B / (2 * A), 4)}; ${num(C - B * B / (4 * A), 4)})") else emptyArray()),
    )
    Note(when { A == 0.0 -> "Con a = 0 es una ecuación de primer grado."; d > 0 -> "Dos raíces reales distintas."; d == 0.0 -> "Una raíz doble."; else -> "Raíces complejas conjugadas." })
}

@Composable
private fun SystemTool() {
    val c = LocalHilo.current
    var n by remember { mutableIntStateOf(2) }
    val cells = remember { mutableStateListOf(*Array(12) { "" }) }
    Choice(listOf("2" to "2 × 2", "3" to "3 × 3"), n.toString()) { n = it.toInt() }
    val names = listOf("x", "y", "z")
    (0 until n).forEach { r ->
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            (0..n).forEach { col ->
                val k = r * 4 + col
                OutlinedTextField(cells[k], { cells[k] = it }, singleLine = true, modifier = Modifier.weight(1f),
                    label = { Text(if (col < n) names[col] else "=", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
            }
        }
    }
    val vals = (0 until n).map { r -> (0..n).map { col -> cells[r * 4 + col].replace(",", ".").toDoubleOrNull() } }
    if (vals.flatten().any { it == null }) { Note("Llena los coeficientes de cada ecuación (pon 0 si falta una letra)."); return }
    val a = Array(n) { r -> DoubleArray(n) { col -> vals[r][col]!! } }
    val b = DoubleArray(n) { r -> vals[r][n]!! }
    val d = EngMath.det(a)
    val s = EngMath.solve(a, b)
    if (s == null) { Out("Determinante", num(d, 6)); Text("No tiene solución única (det = 0).", color = c.lines[0]); return }
    Out("Determinante", num(d, 6), *s.toList().flatMapIndexed { i, v -> listOf(names[i], num(v, 6)) }.toTypedArray())
}

// =====================================================================
// Psicología y estadística
// =====================================================================

@Composable
private fun StatsTool() {
    var t by remember { mutableStateOf("") }
    OutlinedTextField(t, { t = it }, label = { Text("Datos (separados por espacio, coma o renglón)") }, modifier = Modifier.fillMaxWidth().height(140.dp))
    val xs = EngMath.numbers(t)
    val d = EngMath.describe(xs) ?: run { Note("Pega una lista de números, por ejemplo puntajes de un test."); return }
    Out(
        "n", d.n.toString(), "Media", num(d.mean, 5), "Mediana", num(d.median, 5),
        "Moda", if (d.mode.isEmpty()) "no hay" else d.mode.take(3).joinToString("; ") { num(it) },
        "Desviación típica (s)", num(d.sd, 5), "Varianza", num(d.sd * d.sd, 5),
        "Mínimo – máximo", "${num(d.min)} – ${num(d.max)}", "Rango", num(d.max - d.min),
        "Q1 – Q3", "${num(d.q1)} – ${num(d.q3)}", "Rango intercuartil", num(d.q3 - d.q1),
        "Coef. de variación", if (d.mean != 0.0) num(100 * d.sd / abs(d.mean), 3) + " %" else "—",
    )
    Note("La desviación es muestral (divide entre n − 1), como en SPSS y en APA.")
}

@Composable
private fun CorrTool() {
    var x by remember { mutableStateOf("") }; var y by remember { mutableStateOf("") }
    OutlinedTextField(x, { x = it }, label = { Text("Variable X") }, modifier = Modifier.fillMaxWidth().height(100.dp))
    OutlinedTextField(y, { y = it }, label = { Text("Variable Y (mismo orden)") }, modifier = Modifier.fillMaxWidth().height(100.dp))
    val a = EngMath.numbers(x); val b = EngMath.numbers(y)
    if (a.size != b.size && a.isNotEmpty() && b.isNotEmpty()) Note("X tiene ${a.size} datos y Y tiene ${b.size}: uso los primeros ${minOf(a.size, b.size)}.")
    val (r, pv) = EngMath.pearson(a, b) ?: run { Note("Necesito al menos 3 pares."); return }
    val strength = when (abs(r)) { in 0.0..0.1 -> "nula"; in 0.1..0.3 -> "débil"; in 0.3..0.5 -> "moderada"; else -> "fuerte" }
    Out("r de Pearson", num(r, 3), "r²", num(r * r, 3), "p (dos colas)", fmtP(pv), "n", minOf(a.size, b.size).toString())
    Note("Correlación $strength ${if (r > 0) "positiva" else "negativa"} (criterios de Cohen). ${if (pv < 0.05) "Es significativa (p < ,05)." else "No es significativa (p ≥ ,05)."} Correlación no es causa.")
    Note("En APA: r(${minOf(a.size, b.size) - 2}) = ${apaNum(r)}, p ${fmtPApa(pv)}")
}

@Composable
private fun TTestTool() {
    var x by remember { mutableStateOf("") }; var y by remember { mutableStateOf("") }
    OutlinedTextField(x, { x = it }, label = { Text("Grupo 1") }, modifier = Modifier.fillMaxWidth().height(100.dp))
    OutlinedTextField(y, { y = it }, label = { Text("Grupo 2") }, modifier = Modifier.fillMaxWidth().height(100.dp))
    val r = EngMath.welch(EngMath.numbers(x), EngMath.numbers(y)) ?: run { Note("Cada grupo necesita al menos 2 datos (y que no sean todos iguales)."); return }
    val d = abs(r.d)
    Out("Media 1 / Media 2", "${num(r.m1, 4)} / ${num(r.m2, 4)}", "t", num(r.t, 4), "gl (Welch)", num(r.df, 4), "p (dos colas)", fmtP(r.p), "d de Cohen", num(r.d, 3))
    Note("Efecto ${when { d < 0.2 -> "muy pequeño"; d < 0.5 -> "pequeño"; d < 0.8 -> "mediano"; else -> "grande" }}. ${if (r.p < 0.05) "La diferencia es significativa (p < ,05)." else "No hay diferencia significativa (p ≥ ,05)."}")
    Note("En APA: t(${num(r.df, 3)}) = ${apaNum(r.t)}, p ${fmtPApa(r.p)}, d = ${apaNum(r.d)}")
}

private fun fmtP(p: Double) = if (p < 0.001) "< 0,001" else num(p, 3)
private fun apaNum(x: Double) = String.format(java.util.Locale.US, "%.2f", x).replace("0.", ".").replace("-.", "−.")
private fun fmtPApa(p: Double) = if (p < 0.001) "< .001" else "= " + String.format(java.util.Locale.US, "%.3f", p).removePrefix("0")

@Composable
private fun ApaTool(vm: HiloViewModel) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    var kind by remember { mutableStateOf("libro") }
    var authors by remember { mutableStateOf("") }; var year by remember { mutableStateOf("") }; var title by remember { mutableStateOf("") }
    var where by remember { mutableStateOf("") }; var vol by remember { mutableStateOf("") }; var issue by remember { mutableStateOf("") }
    var pages by remember { mutableStateOf("") }; var doi by remember { mutableStateOf("") }; var eds by remember { mutableStateOf("") }; var book by remember { mutableStateOf("") }
    Choice(listOf("libro" to "Libro", "articulo" to "Artículo de revista", "capitulo" to "Capítulo de libro", "web" to "Página web"), kind) { kind = it }
    OutlinedTextField(authors, { authors = it }, label = { Text("Autores: Apellido, Nombre; Apellido, Nombre") }, modifier = Modifier.fillMaxWidth())
    Num("Año", year) { year = it }
    OutlinedTextField(title, { title = it }, label = { Text(if (kind == "capitulo") "Título del capítulo" else "Título") }, modifier = Modifier.fillMaxWidth())
    if (kind == "capitulo") {
        OutlinedTextField(eds, { eds = it }, label = { Text("Editores (Nombre Apellido)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(book, { book = it }, label = { Text("Título del libro") }, modifier = Modifier.fillMaxWidth())
    }
    OutlinedTextField(where, { where = it }, label = { Text(when (kind) { "articulo" -> "Revista"; "web" -> "Sitio web"; else -> "Editorial" }) }, modifier = Modifier.fillMaxWidth())
    if (kind == "articulo") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Num("Volumen", vol, modifier = Modifier.weight(1f)) { vol = it }; Num("Número", issue, modifier = Modifier.weight(1f)) { issue = it }
    }
    if (kind == "articulo" || kind == "capitulo") Num("Páginas", pages, "45-67") { pages = it }
    OutlinedTextField(doi, { doi = it }, label = { Text("DOI o URL (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    val ref = apaRef(kind, authors, year, title, where, vol, issue, pages, doi, eds, book)
    val cite = apaCite(authors, year)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Referencia", color = c.muted, fontSize = 12.sp)
        Text(ref, color = c.ink, fontSize = 15.sp, lineHeight = 22.sp)
        Text("Cita en el texto", color = c.muted, fontSize = 12.sp)
        Text(cite, color = c.ink, fontSize = 15.sp)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { copyText(ctx, ref.toString()); vm.toast = "Referencia copiada." }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("Copiar referencia") }
        OutlinedButton(onClick = { copyText(ctx, cite); vm.toast = "Cita copiada." }) { Text("Copiar cita", color = c.ink) }
    }
    Note("En el documento, el título del libro o la revista y el volumen van en cursiva (aquí se ven así).")
}

private fun parseAuthors(s: String): List<Pair<String, String>> = s.split(";").map { it.trim() }.filter { it.isNotEmpty() }.map { a ->
    val parts = a.split(",", limit = 2).map { it.trim() }
    if (parts.size == 2) parts[0] to parts[1] else a.substringAfterLast(' ') to a.substringBeforeLast(' ', "")
}

private fun initials(given: String) = given.split(Regex("[\\s-]+")).filter { it.isNotEmpty() }.joinToString(" ") { it.first().uppercase() + "." }

private fun apaAuthors(s: String): String {
    val a = parseAuthors(s).map { (last, given) -> if (given.isBlank()) last else "$last, ${initials(given)}" }
    return when {
        a.isEmpty() -> ""
        a.size == 1 -> a[0]
        a.size <= 20 -> a.dropLast(1).joinToString(", ") + ", & " + a.last()
        else -> a.take(19).joinToString(", ") + ", . . . " + a.last()
    }
}

private fun apaCite(s: String, year: String): String {
    val a = parseAuthors(s).map { it.first }
    val y = year.ifBlank { "s.f." }
    return when {
        a.isEmpty() -> "(Título, $y)"
        a.size == 1 -> "(${a[0]}, $y)"
        a.size == 2 -> "(${a[0]} & ${a[1]}, $y)"
        else -> "(${a[0]} et al., $y)"
    }
}

private fun apaRef(kind: String, authors: String, year: String, title: String, where: String, vol: String, issue: String, pages: String, doi: String, eds: String, book: String): AnnotatedString = buildAnnotatedString {
    val au = apaAuthors(authors)
    val y = year.ifBlank { "s.f." }
    val link = doi.trim().let { if (it.startsWith("10.")) "https://doi.org/$it" else it }
    val ital = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
    if (au.isNotBlank()) append("$au ")
    append("($y). ")
    when (kind) {
        "libro" -> { withStyle(ital) { append(title.trim().removeSuffix(".")) }; append(". "); if (where.isNotBlank()) append(where.trim().removeSuffix(".") + ".") }
        "articulo" -> {
            append(title.trim().removeSuffix(".") + ". ")
            withStyle(ital) { append(where.trim()); if (vol.isNotBlank()) append(", $vol") }
            if (issue.isNotBlank()) append("($issue)")
            if (pages.isNotBlank()) append(", ${pages.replace("-", "–")}")
            append(".")
        }
        "capitulo" -> {
            append(title.trim().removeSuffix(".") + ". ")
            if (eds.isNotBlank()) { append("En $eds (${if (eds.contains(",") || eds.contains(" y ") || eds.contains("&")) "Eds." else "Ed."}), ") } else append("En ")
            withStyle(ital) { append(book.trim()) }
            if (pages.isNotBlank()) append(" (pp. ${pages.replace("-", "–")})")
            append(". ")
            if (where.isNotBlank()) append(where.trim().removeSuffix(".") + ".")
        }
        else -> { withStyle(ital) { append(title.trim().removeSuffix(".")) }; append(". "); if (where.isNotBlank()) append(where.trim().removeSuffix(".") + ".") }
    }
    if (link.isNotBlank()) append(" $link")
}

private data class School(val name: String, val years: String, val authors: String, val idea: String, val method: String, val keys: String)

private val SCHOOLS = listOf(
    School("Estructuralismo", "1879 en adelante", "Wilhelm Wundt, Edward Titchener", "Descomponer la experiencia consciente en sus elementos (sensaciones, imágenes, sentimientos).", "Introspección entrenada en laboratorio.", "Primer laboratorio de psicología (Leipzig, 1879)."),
    School("Funcionalismo", "fines del siglo XIX", "William James, John Dewey", "Importa para qué sirve la mente: adaptarse al ambiente.", "Observación, introspección y estudio comparado.", "Corriente de conciencia; influencia de Darwin."),
    School("Psicoanálisis", "desde 1890–1900", "Sigmund Freud; luego Jung, Adler, Anna Freud, Klein, Lacan", "Lo inconsciente, los conflictos y las experiencias tempranas mueven la conducta.", "Asociación libre, análisis de sueños, transferencia.", "Ello, yo y superyó; mecanismos de defensa; etapas psicosexuales."),
    School("Conductismo", "1913 en adelante", "John B. Watson, Iván Pávlov, B. F. Skinner, Edward Thorndike", "Estudiar solo la conducta observable y cómo se aprende.", "Experimentos con estímulos, respuestas y consecuencias.", "Condicionamiento clásico y operante; refuerzo y castigo; ley del efecto."),
    School("Gestalt", "1910–1930", "Max Wertheimer, Wolfgang Köhler, Kurt Koffka, Kurt Lewin", "El todo es distinto de la suma de sus partes: percibimos formas organizadas.", "Experimentos de percepción y de solución de problemas (insight).", "Leyes de proximidad, semejanza, cierre, figura-fondo."),
    School("Humanismo", "1950–1960", "Abraham Maslow, Carl Rogers, Rollo May", "La persona tiende a crecer y realizarse; importa la experiencia subjetiva.", "Terapia centrada en la persona, entrevista, estudio de casos.", "Pirámide de necesidades; autorrealización; aceptación incondicional, empatía."),
    School("Cognitivismo", "1950–1960 en adelante", "Ulric Neisser, George Miller, Jean Piaget, Jerome Bruner", "La mente procesa información: atención, memoria, lenguaje, razonamiento.", "Experimentos de tiempos de reacción y memoria; modelos.", "Memoria de trabajo (7±2); esquemas; etapas de Piaget."),
    School("Terapia cognitivo-conductual", "1960 en adelante", "Aaron Beck, Albert Ellis; Donald Meichenbaum", "Lo que pensamos de lo que pasa influye en cómo nos sentimos y actuamos.", "Reestructuración cognitiva, registros de pensamientos, exposición.", "Distorsiones cognitivas; triada cognitiva; modelo ABC de Ellis."),
    School("Aprendizaje social", "1960 en adelante", "Albert Bandura, Julian Rotter", "Aprendemos observando a otros; las expectativas importan.", "Experimentos de modelado (muñeco Bobo).", "Aprendizaje vicario; autoeficacia; locus de control."),
    School("Psicología sociocultural", "1920–1930 (difusión desde 1970)", "Lev Vygotsky, Alexander Luria", "El desarrollo mental nace de la interacción social y la cultura.", "Estudio del desarrollo y la educación.", "Zona de desarrollo próximo; andamiaje; lenguaje como herramienta."),
    School("Psicología del desarrollo", "siglo XX", "Jean Piaget, Erik Erikson, John Bowlby, Mary Ainsworth", "Cómo cambiamos a lo largo de la vida: pensamiento, identidad, vínculos.", "Observación longitudinal, situaciones experimentales.", "Etapas de Piaget; 8 etapas psicosociales; apego seguro e inseguro."),
    School("Enfoque sistémico", "1950–1970", "Gregory Bateson, Paul Watzlawick, Salvador Minuchin, Virginia Satir", "Los problemas se entienden en el sistema de relaciones (la familia).", "Terapia familiar, genograma, intervenciones paradójicas.", "Axiomas de la comunicación; doble vínculo; homeostasis familiar."),
    School("Neurociencia cognitiva", "1980 en adelante", "Michael Gazzaniga, Antonio Damasio, Eric Kandel", "La mente se estudia junto con el cerebro que la produce.", "Neuroimagen (fMRI, EEG), estudio de lesiones.", "Hemisferios; marcador somático; plasticidad y memoria."),
    School("Psicología positiva", "1998 en adelante", "Martin Seligman, Mihaly Csikszentmihalyi", "Estudiar también el bienestar, las fortalezas y lo que funciona bien.", "Cuestionarios, intervenciones breves, estudios longitudinales.", "Modelo PERMA; flujo; indefensión y optimismo aprendidos."),
    School("Psicología evolucionista", "1990 en adelante", "Leda Cosmides, John Tooby, David Buss", "Rasgos mentales que pudieron surgir como adaptaciones.", "Comparación entre culturas y especies.", "Módulos mentales; selección sexual (con críticas metodológicas)."),
)

@Composable
private fun PsyKbTool() {
    val c = LocalHilo.current
    var q by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<String?>(null) }
    OutlinedTextField(q, { q = it }, label = { Text("Buscar corriente, autor o concepto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    val f = com.hilo.lector.text.Smart.fold(q)
    SCHOOLS.filter { s -> q.isBlank() || com.hilo.lector.text.Smart.fold("${s.name} ${s.authors} ${s.idea} ${s.keys}").contains(f) }.forEach { s ->
        val on = open == s.name || q.isNotBlank()
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).clickable { open = if (open == s.name) null else s.name }.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row { Text(s.name, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f)); Text(s.years, color = c.muted, fontSize = 12.sp) }
            Text(s.authors, color = c.accent, fontSize = 13.sp)
            if (on) {
                Text(s.idea, color = c.ink, fontSize = 15.sp, lineHeight = 21.sp)
                Text("Cómo investiga: ${s.method}", color = c.muted, fontSize = 13.sp, lineHeight = 18.sp)
                Text("Conceptos clave: ${s.keys}", color = c.muted, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
    Note("Es un resumen para ubicarte. Para citar, usa las fuentes de tu clase.")
}

// =====================================================================
// Programación
// =====================================================================

@Composable
private fun RegexTool() {
    val c = LocalHilo.current
    var pat by remember { mutableStateOf("\\b\\w+@\\w+\\.\\w+\\b") }
    var txt by remember { mutableStateOf("Escribe a ana@correo.hn o a luis@uni.edu") }
    var ic by remember { mutableStateOf(false) }
    OutlinedTextField(pat, { pat = it }, label = { Text("Patrón") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace))
    Row(verticalAlignment = Alignment.CenterVertically) { Text("Sin distinguir mayúsculas", color = c.muted, fontSize = 13.sp, modifier = Modifier.weight(1f)); Switch(ic, { ic = it }) }
    OutlinedTextField(txt, { txt = it }, label = { Text("Texto de prueba") }, modifier = Modifier.fillMaxWidth().height(130.dp))
    val rx = runCatching { if (ic) Regex(pat, RegexOption.IGNORE_CASE) else Regex(pat) }
    rx.exceptionOrNull()?.let { Text("El patrón tiene un error: ${it.message?.lineSequence()?.firstOrNull()}", color = c.lines[0], fontSize = 13.sp); return }
    val ms = rx.getOrThrow().findAll(txt).take(200).toList()
    Text(buildAnnotatedString {
        var last = 0
        ms.forEach { m -> if (m.range.first >= last && !m.range.isEmpty()) { append(txt.substring(last, m.range.first)); withStyle(SpanStyle(background = c.accent.copy(alpha = 0.3f), fontWeight = FontWeight.Bold)) { append(m.value) }; last = m.range.last + 1 } }
        append(txt.substring(last))
    }, color = c.ink, fontSize = 15.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(12.dp))
    Text("${ms.size} coincidencia(s)", color = c.accent, fontWeight = FontWeight.SemiBold)
    ms.take(20).forEachIndexed { i, m ->
        Text("${i + 1}. «${m.value}»" + if (m.groupValues.size > 1) "  grupos: " + m.groupValues.drop(1).joinToString(" | ") else "", color = c.ink, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
    Note("Repaso rápido: . cualquier carácter · \\d dígito · \\w letra o número · \\s espacio · * 0 o más · + 1 o más · ? opcional · [abc] uno de · ( ) grupo · ^ inicio · $ fin.")
}

// =====================================================================
// Creadores de tarjetas
// =====================================================================

private fun deckOf(vm: HiloViewModel, classId: String?): String = classId?.let { id -> vm.classes.get(id)?.books?.firstOrNull() } ?: "herramientas"

private fun addCard(vm: HiloViewModel, classId: String?, kind: String, front: String, back: String, context: String = "", source: String) {
    vm.study.add(StudyCard(vm.study.newId(), deckOf(vm, classId), kind, front.trim(), back.trim(), context = context, source = source, due = today(), created = System.currentTimeMillis()))
    vm.toast = "Tarjeta creada: ya está en tu repaso de hoy."
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    val c = LocalHilo.current
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("Crear tarjeta") }
}

@Composable
private fun CodeCardTool(vm: HiloViewModel, classId: String?) {
    var q by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }; var ans by remember { mutableStateOf("") }
    var codeFront by remember { mutableStateOf(true) }
    OutlinedTextField(q, { q = it }, label = { Text("Pregunta") }, placeholder = { Text("¿Qué imprime este código?") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(code, { code = it }, label = { Text("Código") }, modifier = Modifier.fillMaxWidth().height(160.dp), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp))
    OutlinedTextField(ans, { ans = it }, label = { Text("Respuesta") }, modifier = Modifier.fillMaxWidth())
    Choice(listOf("f" to "Código en el frente", "b" to "Código en la respuesta"), if (codeFront) "f" else "b") { codeFront = it == "f" }
    if (code.isNotBlank()) CodeBlock(code)
    SaveButton(q.isNotBlank() && code.isNotBlank()) {
        val block = "```\n${code.trimEnd()}\n```"
        if (codeFront) addCard(vm, classId, "codigo", "$q\n$block", ans, source = "Tarjeta de código")
        else addCard(vm, classId, "codigo", q, "$block\n$ans", source = "Tarjeta de código")
        q = ""; code = ""; ans = ""
    }
}

@Composable
private fun ProblemCardTool(vm: HiloViewModel, classId: String?) {
    val c = LocalHilo.current
    var q by remember { mutableStateOf("") }
    val steps = remember { mutableStateListOf("", "") }
    OutlinedTextField(q, { q = it }, label = { Text("Enunciado del problema") }, modifier = Modifier.fillMaxWidth().height(120.dp))
    Note("Escribe cada paso aparte. Al repasar, se muestran de a uno para que intentes el siguiente. Puedes usar fórmulas entre \$…\$, por ejemplo \$I = V/R\$.")
    steps.forEachIndexed { i, s ->
        OutlinedTextField(s, { steps[i] = it }, label = { Text("Paso ${i + 1}") }, modifier = Modifier.fillMaxWidth())
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { steps.add("") }) { Text("+ Paso", color = c.ink) }
        if (steps.size > 1) TextButton(onClick = { steps.removeAt(steps.size - 1) }) { Text("Quitar el último") }
    }
    val filled = steps.filter { it.isNotBlank() }
    SaveButton(q.isNotBlank() && filled.isNotEmpty()) {
        addCard(vm, classId, "problema", q, filled.joinToString("\n\n"), source = "Problema paso a paso")
        q = ""; steps.clear(); steps.addAll(listOf("", ""))
    }
}

@Composable
private fun FormulaCardTool(vm: HiloViewModel, classId: String?) {
    val c = LocalHilo.current
    var name by remember { mutableStateOf("") }; var tex by remember { mutableStateOf("") }; var front by remember { mutableStateOf("nombre") }
    OutlinedTextField(name, { name = it }, label = { Text("Nombre o pregunta") }, placeholder = { Text("Frecuencia de resonancia") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(tex, { tex = it }, label = { Text("Fórmula en LaTeX") }, placeholder = { Text("f_0 = \\frac{1}{2\\pi\\sqrt{LC}}") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace))
    TexHelp()
    if (tex.isNotBlank()) Eq(tex)
    Choice(listOf("nombre" to "Pregunta: el nombre", "formula" to "Pregunta: la fórmula"), front) { front = it }
    SaveButton(name.isNotBlank() && tex.isNotBlank()) {
        if (front == "nombre") addCard(vm, classId, "formula", name, "$$" + tex + "$$", source = "Fórmula") else addCard(vm, classId, "formula", "¿Qué es esto?\n$$" + tex + "$$", name, source = "Fórmula")
        name = ""; tex = ""
    }
    @Suppress("UNUSED_VARIABLE") val u = c
}

@Composable
private fun TexHelp() {
    Note("Ayuda LaTeX: \\frac{a}{b} fracción · x^2 potencia · x_1 subíndice · \\sqrt{x} raíz · \\pi π · \\Omega Ω · \\omega ω · \\Delta Δ · \\cdot · · \\sum ∑ · \\int ∫ · \\le ≤ · \\approx ≈")
}

/** Editor de imagen tapada: eliges una foto (un circuito, un mapa del cerebro…) y dibujas rectángulos encima. */
@Composable
private fun OcclusionEditor(vm: HiloViewModel, classId: String?) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    var path by remember { mutableStateOf<String?>(null) }
    val boxes = remember { mutableStateListOf<FloatArray>() }
    val labels = remember { mutableStateListOf<String>() }
    var title by remember { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { path = keepImage(ctx, uri); boxes.clear(); labels.clear() }
    }
    Note("1) Elige una imagen. 2) Arrastra el dedo para tapar cada parte. 3) Escribe qué es cada parte. Se crea una tarjeta por parte.")
    OutlinedButton(onClick = { pick.launch("image/*") }) { Text(if (path == null) "Elegir imagen" else "Cambiar imagen", color = c.ink) }
    val p = path ?: return
    val img by produceState<ImageBitmap?>(null, p) { value = withContext(Dispatchers.IO) { runCatching { android.graphics.BitmapFactory.decodeFile(p)?.asImageBitmap() }.getOrNull() } }
    val b = img ?: return
    var drag by remember { mutableStateOf<FloatArray?>(null) }
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(b.width.toFloat() / b.height).clip(RoundedCornerShape(12.dp))) {
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
        val dens = LocalDensity.current
        Image(b, null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { o -> drag = floatArrayOf(o.x / w, o.y / h, o.x / w, o.y / h) },
                    onDrag = { ch, _ -> drag = drag?.let { d -> floatArrayOf(d[0], d[1], (ch.position.x / w).coerceIn(0f, 1f), (ch.position.y / h).coerceIn(0f, 1f)) } },
                    onDragEnd = {
                        drag?.let { d ->
                            val r = floatArrayOf(minOf(d[0], d[2]), minOf(d[1], d[3]), maxOf(d[0], d[2]), maxOf(d[1], d[3]))
                            if (r[2] - r[0] > 0.02f && r[3] - r[1] > 0.02f) { boxes.add(r); labels.add("") }
                        }
                        drag = null
                    },
                )
            })
        (boxes + listOfNotNull(drag?.let { d -> floatArrayOf(minOf(d[0], d[2]), minOf(d[1], d[3]), maxOf(d[0], d[2]), maxOf(d[1], d[3])) })).forEachIndexed { k, r ->
            Box(
                Modifier.offset { IntOffset((r[0] * w).roundToInt(), (r[1] * h).roundToInt()) }
                    .size(with(dens) { ((r[2] - r[0]) * w).toDp() }, with(dens) { ((r[3] - r[1]) * h).toDp() })
                    .clip(RoundedCornerShape(4.dp)).background(Color(0xFF5B7CFA).copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center,
            ) { if (k < boxes.size) Text("${k + 1}", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
    boxes.indices.forEach { k ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(labels[k], { labels[k] = it }, label = { Text("Parte ${k + 1}: ¿qué es?") }, singleLine = true, modifier = Modifier.weight(1f))
            TextButton(onClick = { boxes.removeAt(k); labels.removeAt(k) }) { Text("Quitar") }
        }
    }
    OutlinedTextField(title, { title = it }, label = { Text("Pregunta (opcional)") }, placeholder = { Text("¿Qué es la parte roja?") }, modifier = Modifier.fillMaxWidth())
    SaveButton(boxes.isNotEmpty() && labels.all { it.isNotBlank() }) {
        val arr = JSONArray().apply { boxes.forEach { r -> put(JSONArray(r.map { it.toDouble() })) } }
        boxes.indices.forEach { k ->
            val ctxJson = JSONObject().put("img", p).put("boxes", arr).put("i", k).toString()
            vm.study.add(StudyCard(vm.study.newId(), deckOf(vm, classId), "imagen", title.ifBlank { "¿Qué es la parte roja?" }, labels[k], context = ctxJson, source = "Imagen tapada", due = today(), created = System.currentTimeMillis()))
        }
        vm.toast = "Listo: ${boxes.size} tarjeta(s) de imagen tapada."
        boxes.clear(); labels.clear(); path = null
    }
}

// =====================================================================
// Formulario de la clase
// =====================================================================

private val BASIC_FORMULAS = mapOf(
    "electrica" to listOf(
        Triple("Ley de Ohm", "V = I\\,R", "I*R"),
        Triple("Potencia", "P = V\\,I = I^2 R", "V*I"),
        Triple("Resistencias en paralelo (2)", "R_{eq} = \\frac{R_1 R_2}{R_1 + R_2}", "R1*R2/(R1+R2)"),
        Triple("Divisor de tensión", "V_{out} = V_{in}\\frac{R_2}{R_1+R_2}", "Vin*R2/(R1+R2)"),
        Triple("Reactancia inductiva", "X_L = 2\\pi f L", "2*pi*f*L"),
        Triple("Reactancia capacitiva", "X_C = \\frac{1}{2\\pi f C}", "1/(2*pi*f*C)"),
        Triple("Frecuencia de resonancia", "f_0 = \\frac{1}{2\\pi\\sqrt{LC}}", "1/(2*pi*sqrt(L*C))"),
        Triple("Constante de tiempo RC", "\\tau = R\\,C", "R*C"),
        Triple("Energía en un capacitor", "E = \\tfrac{1}{2} C V^2", "0.5*C*V^2"),
        Triple("Potencia trifásica", "P = \\sqrt{3}\\,V_L I_L \\cos\\varphi", "sqrt(3)*VL*IL*fp"),
        Triple("Ganancia amplificador inversor", "A_v = -\\frac{R_f}{R_{in}}", "-Rf/Rin"),
        Triple("Ganancia amplificador no inversor", "A_v = 1 + \\frac{R_f}{R_1}", "1+Rf/R1"),
    ),
    "matematicas" to listOf(
        Triple("Fórmula general", "x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}", "(-b+sqrt(b^2-4*a*c))/(2*a)"),
        Triple("Pitágoras", "c = \\sqrt{a^2+b^2}", "sqrt(a^2+b^2)"),
        Triple("Distancia entre puntos", "d=\\sqrt{(x_2-x_1)^2+(y_2-y_1)^2}", "sqrt((x2-x1)^2+(y2-y1)^2)"),
        Triple("Área del círculo", "A = \\pi r^2", "pi*r^2"),
        Triple("Segunda ley de Newton", "F = m\\,a", "m*a"),
        Triple("Caída libre", "h = \\tfrac{1}{2} g t^2", "0.5*9.81*t^2"),
        Triple("Energía cinética", "E_c = \\tfrac{1}{2} m v^2", "0.5*m*v^2"),
        Triple("Derivada de una potencia", "\\frac{d}{dx}x^n = n x^{n-1}", ""),
        Triple("Integral de una potencia", "\\int x^n\\,dx = \\frac{x^{n+1}}{n+1} + C", ""),
    ),
    "psicologia" to listOf(
        Triple("Media", "\\bar{x} = \\frac{\\sum x_i}{n}", ""),
        Triple("Desviación típica muestral", "s = \\sqrt{\\frac{\\sum (x_i-\\bar x)^2}{n-1}}", ""),
        Triple("Puntuación z", "z = \\frac{x - \\mu}{\\sigma}", "(x-mu)/sigma"),
        Triple("Puntuación T", "T = 50 + 10z", "50+10*z"),
        Triple("d de Cohen", "d = \\frac{\\bar x_1 - \\bar x_2}{s_p}", "(m1-m2)/sp"),
    ),
)

/** Formulario de la clase: fórmulas bien dibujadas y que se pueden calcular. */
@Composable
fun FormulasTab(vm: HiloViewModel, cl: Clase) {
    val c = LocalHilo.current
    val list = vm.classes.formulasOf(cl.id)
    var edit by remember { mutableStateOf<Formula?>(null) }
    var calc by remember { mutableStateOf<Formula?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Formulario", color = c.ink, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            HelpButton(vm, "clase-formulas")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { edit = Formula(System.currentTimeMillis(), cl.id, "", "", "") }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("+ Fórmula") }
            val basics = BASIC_FORMULAS[cl.field] ?: BASIC_FORMULAS["matematicas"]!!
            if (list.isEmpty()) OutlinedButton(onClick = {
                basics.forEachIndexed { i, (n, t, v) -> vm.classes.putFormula(Formula(System.currentTimeMillis() + i, cl.id, n, t, v)) }
            }) { Text("Poner las básicas", color = c.ink) }
        }
        if (list.isEmpty()) Note("Guarda aquí las fórmulas de la materia. Se ven como en el libro, puedes calcular con ellas y convertirlas en tarjetas.")
        list.forEach { f ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(f.name, color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                MathView("$$" + f.tex + "$$", c.ink, 18f)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (f.vars.isNotBlank()) Pill("Calcular") { calc = f }
                    Pill("Tarjeta") { addCard(vm, cl.id, "formula", f.name, "$$" + f.tex + "$$", source = "Formulario de ${cl.name}") }
                    Pill("Editar") { edit = f }
                    Pill("Borrar") { vm.classes.deleteFormula(f.id) }
                }
            }
        }
    }
    edit?.let { f ->
        var name by remember(f.id) { mutableStateOf(f.name) }; var tex by remember(f.id) { mutableStateOf(f.tex) }; var vars by remember(f.id) { mutableStateOf(f.vars) }
        AlertDialog(
            onDismissRequest = { edit = null },
            title = { Text(if (f.name.isBlank()) "Fórmula nueva" else "Editar fórmula") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(tex, { tex = it }, label = { Text("Cómo se ve (LaTeX)") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace))
                    if (tex.isNotBlank()) MathView("$$" + tex + "$$", c.ink, 17f)
                    OutlinedTextField(vars, { vars = it }, label = { Text("Para calcular (opcional)") }, placeholder = { Text("I*R  o  1/(2*pi*sqrt(L*C))") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace))
                    TexHelp()
                }
            },
            confirmButton = { TextButton(onClick = { if (name.isNotBlank() && tex.isNotBlank()) vm.classes.putFormula(f.copy(name = name.trim(), tex = tex.trim(), vars = vars.trim())); edit = null }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { edit = null }) { Text("Cancelar") } },
        )
    }
    calc?.let { f -> FormulaCalc(f) { calc = null } }
}

@Composable
private fun FormulaCalc(f: Formula, onDismiss: () -> Unit) {
    val c = LocalHilo.current
    val names = remember(f.vars) { EngMath.variables(f.vars) }
    val vals = remember(f.vars) { mutableStateListOf(*Array(names.size) { "" }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(f.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MathView("$$" + f.tex + "$$", c.ink, 17f)
                names.forEachIndexed { i, n -> Num(n, vals[i], "acepta 4.7k, 10m…") { vals[i] = it } }
                val m = names.mapIndexedNotNull { i, n -> p(vals[i])?.let { n to it } }.toMap()
                if (m.size == names.size) {
                    val r = runCatching { EngMath.eval(f.vars, deg = false, vars = m) }
                    Text(r.getOrNull()?.let { "= " + num(it, 6) + "   (" + eng(it) + ")" } ?: (r.exceptionOrNull()?.message ?: ""), color = c.accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                } else Spacer(Modifier.height(4.dp))
                if (Regex("\\b(a?sin|a?cos|a?tan|sen)\\b").containsMatchIn(f.vars)) Note("Los ángulos van en radianes.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
    )
}

