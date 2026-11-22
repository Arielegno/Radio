package com.hilo.lector.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.hilo.lector.data.StudyCard
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hilo.lector.HiloViewModel
import com.hilo.lector.data.Book
import com.hilo.lector.data.ClassNote
import com.hilo.lector.data.today
import com.hilo.lector.text.EngMath
import com.hilo.lector.text.EngMath.num
import com.hilo.lector.text.Law
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// =====================================================================
// Foto a texto (sin internet)
// =====================================================================

/** Reconoce el texto de una foto (pizarra, página, apuntes) con el lector de Google que corre en el celular. */
suspend fun ocrImage(ctx: android.content.Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    var sample = 1
    while (maxOf(opts.outWidth, opts.outHeight) / sample > 2400) sample *= 2
    val bmp: Bitmap = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        ?: return@withContext ""
    val rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        suspendCancellableCoroutine { cont ->
            rec.process(InputImage.fromBitmap(bmp, 0))
                .addOnSuccessListener { t -> cont.resume(t.textBlocks.joinToString("\n\n") { b -> b.lines.joinToString(" ") { it.text } }) }
                .addOnFailureListener { cont.resume("") }
        }
    } finally { rec.close() }
}

@Composable
fun PhotoTextTool(vm: HiloViewModel, classId: String?) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    val shot = remember { File(ctx.cacheDir, "export/foto.jpg").apply { parentFile?.mkdirs() } }
    val shotUri = remember { FileProvider.getUriForFile(ctx, ctx.packageName + ".files", shot) }
    fun run(u: Uri) = scope.launch {
        working = true
        val t = runCatching { ocrImage(ctx, u) }.getOrDefault("")
        working = false
        text = if (text.isBlank()) t else text + "\n\n" + t
        if (t.isBlank()) vm.toast = "No encontré texto. Prueba con más luz y la foto derecha."
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) run(shotUri) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u -> u?.let { run(it) } }
    Note("Saca una foto a la pizarra, a una página o a tus apuntes. Hilo reconoce el texto sin internet y puedes corregirlo antes de guardarlo.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { camera.launch(shotUri) }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("Sacar foto") }
        OutlinedButton(onClick = { gallery.launch("image/*") }) { Text("Elegir de la galería", color = c.ink) }
    }
    if (working) { LinearProgressIndicator(Modifier.fillMaxWidth()); Note("Reconociendo el texto…") }
    if (text.isNotBlank()) {
        OutlinedTextField(text, { text = it }, label = { Text("Texto reconocido (puedes corregirlo)") }, modifier = Modifier.fillMaxWidth().height(260.dp))
        Note("Reconoce letra de imprenta muy bien; la letra a mano, solo si es clara. Revisa números y fórmulas.")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { copyText(ctx, text); vm.toast = "Texto copiado." }) { Text("Copiar", color = c.ink) }
            OutlinedButton(onClick = {
                val f = File(ctx.cacheDir, "export/Apunte ${java.time.LocalDate.now()}.txt")
                f.writeText(text)
                vm.import(listOf(Uri.fromFile(f)))
                vm.toast = "Lo agregué a tu biblioteca como documento."
            }) { Text("Guardar como documento", color = c.ink) }
        }
        val courses = vm.classes.courses
        if (courses.isNotEmpty()) {
            Text("Guardar en el diario de una clase:", color = c.muted, fontSize = 13.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                courses.sortedByDescending { it.id == classId }.forEach { cl ->
                    Pill(cl.name) {
                        vm.classes.putNote(ClassNote(vm.classes.newId(), cl.id, today(), text.trim()))
                        vm.toast = "Guardado en el diario de «${cl.name}»."
                    }
                }
            }
        }
    }
}

// =====================================================================
// Graficador de funciones
// =====================================================================

@Composable
fun GraphTool() {
    val c = LocalHilo.current
    var f1 by remember { mutableStateOf("sin(x)/x") }
    var f2 by remember { mutableStateOf("") }
    var xa by remember { mutableStateOf("-10") }
    var xb by remember { mutableStateOf("10") }
    var probe by remember { mutableStateOf<Double?>(null) }
    OutlinedTextField(f1, { f1 = it }, label = { Text("f(x) =") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace))
    OutlinedTextField(f2, { f2 = it }, label = { Text("g(x) = (opcional, para comparar)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(xa, { xa = it }, label = { Text("x desde") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
        OutlinedTextField(xb, { xb = it }, label = { Text("hasta") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
    }
    val a = EngMath.parse(xa) ?: -10.0
    val b = (EngMath.parse(xb) ?: 10.0).let { if (it <= a) a + 1 else it }
    fun fn(e: String): ((Double) -> Double)? {
        if (e.isBlank()) return null
        return runCatching { EngMath.eval(e, deg = false, vars = mapOf("x" to 1.0)); { x: Double -> runCatching { EngMath.eval(e, deg = false, vars = mapOf("x" to x, "t" to x)) }.getOrDefault(Double.NaN) } }.getOrNull()
    }
    val g1 = fn(f1); val g2 = fn(f2)
    if (g1 == null) { Text("No entiendo f(x). Usa x como variable: 2*x^2 - 3, sin(x), e^(-x/2)…", color = c.lines[0], fontSize = 13.sp); return }
    val n = 600
    val xs = remember(a, b) { DoubleArray(n + 1) { a + (b - a) * it / n } }
    val y1 = remember(f1, a, b) { DoubleArray(n + 1) { g1(xs[it]) } }
    val y2 = remember(f2, a, b) { g2?.let { g -> DoubleArray(n + 1) { g(xs[it]) } } }
    val all = (y1.toList() + (y2?.toList() ?: emptyList())).filter { it.isFinite() }.sorted()
    if (all.isEmpty()) { Note("La función no da valores en ese rango."); return }
    // Rango vertical sin los picos (asíntotas): percentiles 2–98
    var lo = all[(all.size * 0.02).toInt()]; var hi = all[((all.size - 1) * 0.98).toInt()]
    if (hi - lo < 1e-9) { lo -= 1; hi += 1 }
    val pad = (hi - lo) * 0.08; lo -= pad; hi += pad
    val col1 = c.accent; val col2 = c.lines[0]; val axis = c.muted; val grid = c.line
    Canvas(
        Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(14.dp)).background(c.surface)
            .pointerInput(a, b) { detectTapGestures { o -> probe = a + (b - a) * o.x / size.width } },
    ) {
        val w = size.width; val h = size.height
        fun px(x: Double) = ((x - a) / (b - a) * w).toFloat()
        fun py(y: Double) = (h - (y - lo) / (hi - lo) * h).toFloat()
        // cuadrícula
        val stepX = niceStep(b - a); val stepY = niceStep(hi - lo)
        var gx = kotlin.math.ceil(a / stepX) * stepX
        while (gx <= b) { drawLine(grid, Offset(px(gx), 0f), Offset(px(gx), h), 1f); gx += stepX }
        var gy = kotlin.math.ceil(lo / stepY) * stepY
        while (gy <= hi) { drawLine(grid, Offset(0f, py(gy)), Offset(w, py(gy)), 1f); gy += stepY }
        if (a < 0 && b > 0) drawLine(axis, Offset(px(0.0), 0f), Offset(px(0.0), h), 2f)
        if (lo < 0 && hi > 0) drawLine(axis, Offset(0f, py(0.0)), Offset(w, py(0.0)), 2f)
        fun curve(ys: DoubleArray, col: Color) {
            val p = Path(); var pen = false
            for (i in 0..n) {
                val y = ys[i]
                if (!y.isFinite() || y < lo - (hi - lo) * 3 || y > hi + (hi - lo) * 3) { pen = false; continue }
                if (pen && i > 0 && ys[i - 1].isFinite() && abs(y - ys[i - 1]) > (hi - lo) * 1.5) pen = false
                if (!pen) { p.moveTo(px(xs[i]), py(y)); pen = true } else p.lineTo(px(xs[i]), py(y))
            }
            drawPath(p, col, style = Stroke(4f))
        }
        curve(y1, col1)
        y2?.let { curve(it, col2) }
        probe?.let { x -> drawLine(axis, Offset(px(x), 0f), Offset(px(x), h), 2f); g1(x).takeIf { it.isFinite() }?.let { y -> drawCircle(col1, 9f, Offset(px(x), py(y))) } }
    }
    Row { Text("— f(x)", color = col1, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 14.dp)); if (y2 != null) Text("— g(x)", color = col2, fontWeight = FontWeight.Bold) }
    probe?.let { x -> Out("x", num(x, 5), "f(x)", num(g1(x), 6), *(if (g2 != null) arrayOf("g(x)", num(g2(x), 6)) else emptyArray())) }
    Note("Toca el gráfico para ver el valor en un punto. y de ${num(lo, 3)} a ${num(hi, 3)}.")
    // Raíces, máximos y mínimos (por cambio de signo y bisección)
    val roots = remember(f1, a, b) { findRoots(g1, xs) }
    val ext = remember(f1, a, b) { findExtrema(g1, xs) }
    Out(
        "Raíces (f = 0)", if (roots.isEmpty()) "ninguna en el rango" else roots.take(6).joinToString("; ") { num(it, 5) },
        "Máximos", ext.filter { it.second }.take(4).joinToString("; ") { "x=${num(it.first, 4)}" }.ifEmpty { "—" },
        "Mínimos", ext.filter { !it.second }.take(4).joinToString("; ") { "x=${num(it.first, 4)}" }.ifEmpty { "—" },
    )
    if (g2 != null) {
        val cross = remember(f1, f2, a, b) { findRoots({ x -> g1(x) - g2(x) }, xs) }
        Out("Cortes f = g", if (cross.isEmpty()) "ninguno" else cross.take(6).joinToString("; ") { num(it, 5) })
    }
    Note("Escribe x como variable: 5*e^(-x/2), x^3-2x, sqrt(x), 1/(1+x^2). Los ángulos van en radianes.")
}

private fun niceStep(range: Double): Double {
    val raw = range / 8
    val p = Math.pow(10.0, kotlin.math.floor(kotlin.math.log10(raw)))
    return listOf(1.0, 2.0, 5.0, 10.0).map { it * p }.first { it >= raw }
}

private fun findRoots(f: (Double) -> Double, xs: DoubleArray): List<Double> {
    val out = ArrayList<Double>()
    for (i in 0 until xs.size - 1) {
        var lo = xs[i]; var hi = xs[i + 1]
        val fl = f(lo); val fh = f(hi)
        if (!fl.isFinite() || !fh.isFinite()) continue
        if (fl == 0.0) { out.add(lo); continue }
        if (fl * fh > 0) continue
        var a0 = fl
        repeat(60) { val m = (lo + hi) / 2; val fm = f(m); if (fm * a0 <= 0) hi = m else { lo = m; a0 = fm } }
        val r = (lo + hi) / 2
        // descarta saltos (asíntotas) que cambian de signo sin pasar por cero
        if (abs(f(r)) < 1e-6 * (1 + abs(fl) + abs(fh))) out.add(r)
    }
    return out.distinctBy { (it * 1e6).toLong() }
}

private fun findExtrema(f: (Double) -> Double, xs: DoubleArray): List<Pair<Double, Boolean>> {
    val out = ArrayList<Pair<Double, Boolean>>()
    for (i in 1 until xs.size - 1) {
        val a = f(xs[i - 1]); val m = f(xs[i]); val b = f(xs[i + 1])
        if (!a.isFinite() || !m.isFinite() || !b.isFinite()) continue
        if (m > a && m >= b) out.add(xs[i] to true) else if (m < a && m <= b) out.add(xs[i] to false)
    }
    return out
}

// =====================================================================
// Números complejos y fasores
// =====================================================================

data class Cx(val re: Double, val im: Double) {
    operator fun plus(o: Cx) = Cx(re + o.re, im + o.im)
    operator fun minus(o: Cx) = Cx(re - o.re, im - o.im)
    operator fun times(o: Cx) = Cx(re * o.re - im * o.im, re * o.im + im * o.re)
    operator fun div(o: Cx): Cx { val d = o.re * o.re + o.im * o.im; return Cx((re * o.re + im * o.im) / d, (im * o.re - re * o.im) / d) }
    val mag get() = hypot(re, im)
    val angDeg get() = Math.toDegrees(atan2(im, re))
    fun rect() = num(re, 5) + (if (im < 0) " − " else " + ") + "j" + num(abs(im), 5)
    fun polar() = num(mag, 5) + " ∠ " + num(angDeg, 4) + "°"
    companion object {
        fun polar(m: Double, deg: Double) = Cx(m * cos(Math.toRadians(deg)), m * sin(Math.toRadians(deg)))

        /** «3+4j», «3-4i», «-2j», «5∠53.13», «10<-30», «4.7k» */
        fun parse(s0: String): Cx? {
            val s = s0.replace(" ", "").replace(",", ".").replace("−", "-")
            if (s.isEmpty()) return null
            Regex("^([^∠<]+)[∠<]([^∠<]+?)°?$").find(s)?.let { m ->
                val r = EngMath.parse(m.groupValues[1]) ?: return null
                val a = m.groupValues[2].toDoubleOrNull() ?: return null
                return polar(r, a)
            }
            val t = s.replace("i", "j")
            // Separar parte real e imaginaria buscando el último + o - que no sea del exponente
            if (!t.contains('j')) return EngMath.parse(t)?.let { Cx(it, 0.0) }
            val body = t.replace("j", "")
            val idx = (body.length - 1 downTo 1).firstOrNull { (body[it] == '+' || body[it] == '-') && body[it - 1] != 'e' && body[it - 1] != 'E' }
            val (reS, imS) = if (idx == null) "" to body else body.substring(0, idx) to body.substring(idx)
            val re = if (reS.isEmpty()) 0.0 else EngMath.parse(reS) ?: return null
            val im = when (imS) { "", "+" -> 1.0; "-" -> -1.0; else -> EngMath.parse(imS) ?: return null }
            return Cx(re, im)
        }
    }
}

@Composable
fun PhasorTool() {
    val c = LocalHilo.current
    var a by remember { mutableStateOf("3+4j") }
    var b by remember { mutableStateOf("10∠-30") }
    Eq("Z = a + jb = |Z|\\angle\\theta \\qquad Z_1\\parallel Z_2 = \\frac{Z_1 Z_2}{Z_1+Z_2}")
    Note("Escribe en rectangular (3+4j) o polar (5∠53.13 o 5<53.13). Acepta prefijos: 4.7k, 10m.")
    OutlinedTextField(a, { a = it }, label = { Text("A") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace))
    OutlinedTextField(b, { b = it }, label = { Text("B") }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("∠", "j", "-", "°").forEach { k -> Pill(k) { b += k } } }
    val A = Cx.parse(a); val B = Cx.parse(b)
    if (A == null) { Text("No entiendo A.", color = c.lines[0]); return }
    Out("A", A.rect(), "", A.polar())
    if (B == null) { if (b.isNotBlank()) Text("No entiendo B.", color = c.lines[0]); return }
    Out("B", B.rect(), "", B.polar())
    fun show(z: Cx) = z.rect() + "\n" + z.polar()
    val par = (A * B) / (A + B)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.band).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("A + B" to A + B, "A − B" to A - B, "A × B" to A * B, "A ÷ B" to A / B, "A ∥ B (paralelo)" to par, "1 / A" to Cx(1.0, 0.0) / A).forEach { (k, z) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(k, color = c.muted, fontSize = 14.sp, modifier = Modifier.width(110.dp))
                Text(show(z), color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
    PhasorDiagram(listOf(A to c.accent, B to c.lines[0], (A + B) to c.lines[3]))
    Row { Text("— A  ", color = c.accent, fontWeight = FontWeight.Bold); Text("— B  ", color = c.lines[0], fontWeight = FontWeight.Bold); Text("— A+B", color = c.lines[3], fontWeight = FontWeight.Bold) }
    SectionTitleSmall("Impedancias de R, L y C")
    var r by remember { mutableStateOf("100") }; var l by remember { mutableStateOf("50m") }; var cc by remember { mutableStateOf("10u") }; var f by remember { mutableStateOf("60") }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(Triple("R (Ω)", r) { v: String -> r = v }, Triple("L (H)", l) { v: String -> l = v }, Triple("C (F)", cc) { v: String -> cc = v }, Triple("f (Hz)", f) { v: String -> f = v }).forEach { (lab, v, set) ->
            OutlinedTextField(v, set, label = { Text(lab, fontSize = 11.sp) }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
        }
    }
    val R = EngMath.parse(r); val L = EngMath.parse(l); val C = EngMath.parse(cc); val F = EngMath.parse(f)
    if (R != null && L != null && C != null && F != null && F > 0) {
        val w = 2 * Math.PI * F
        val zl = Cx(0.0, w * L); val zc = Cx(0.0, -1 / (w * C)); val zr = Cx(R, 0.0)
        val ser = zr + zl + zc
        val prl = Cx(1.0, 0.0) / (Cx(1.0, 0.0) / zr + Cx(1.0, 0.0) / zl + Cx(1.0, 0.0) / zc)
        Out("Z_L", zl.rect(), "Z_C", zc.rect(), "R + L + C en serie", ser.rect(), "", ser.polar(), "R ∥ L ∥ C", prl.rect(), " ", prl.polar())
        Note("Toca un resultado de arriba para usarlo: cópialo en A o B.")
    }
}

@Composable
private fun SectionTitleSmall(t: String) {
    val c = LocalHilo.current
    Text(t, color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun PhasorDiagram(vs: List<Pair<Cx, Color>>) {
    val c = LocalHilo.current
    val m = vs.maxOf { it.first.mag }.takeIf { it > 0 } ?: 1.0
    Canvas(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(14.dp)).background(c.surface)) {
        val cx = size.width / 2; val cy = size.height / 2
        val sc = (minOf(size.width, size.height) / 2 - 16f) / m
        drawLine(c.line, Offset(0f, cy), Offset(size.width, cy), 2f)
        drawLine(c.line, Offset(cx, 0f), Offset(cx, size.height), 2f)
        vs.forEach { (z, col) ->
            val end = Offset(cx + (z.re * sc).toFloat(), cy - (z.im * sc).toFloat())
            drawLine(col, Offset(cx, cy), end, 6f)
            drawCircle(col, 9f, end)
        }
    }
}

// =====================================================================
// Matrices
// =====================================================================

@Composable
fun MatrixTool() {
    val c = LocalHilo.current
    var n by remember { mutableIntStateOf(3) }
    val cells = remember { mutableStateListOf(*Array(36) { "" }) }
    val rhs = remember { mutableStateListOf(*Array(6) { "" }) }
    Choice((2..6).map { it.toString() to "$it×$it" }, n.toString()) { n = it.toInt() }
    Note("Llena la matriz A (y si quieres resolver A·x = b, la columna b). Pon 0 donde no hay término.")
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        (0 until n).forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                (0 until n).forEach { col ->
                    val k = r * 6 + col
                    OutlinedTextField(cells[k], { cells[k] = it }, singleLine = true, modifier = Modifier.width(64.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                }
                Text("│", color = c.muted, fontSize = 20.sp)
                OutlinedTextField(rhs[r], { rhs[r] = it }, singleLine = true, modifier = Modifier.width(64.dp), placeholder = { Text("b") },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Pill("Identidad") { (0 until 36).forEach { k -> cells[k] = if (k / 6 == k % 6) "1" else "0" } }
        Pill("Vaciar") { (0 until 36).forEach { cells[it] = "" }; (0 until 6).forEach { rhs[it] = "" } }
    }
    val vals = (0 until n).map { r -> (0 until n).map { col -> EngMath.parse(cells[r * 6 + col]) } }
    if (vals.flatten().any { it == null }) return
    val a = Array(n) { r -> DoubleArray(n) { col -> vals[r][col]!! } }
    val det = determinant(a)
    val rank = rankOf(a)
    Out("Determinante", num(det, 6), "Rango", "$rank de $n")
    val inv = inverse(a)
    if (inv == null) Note("No tiene inversa (det = 0): las filas dependen unas de otras.")
    else MatrixShow("Inversa A⁻¹", inv)
    MatrixShow("Transpuesta Aᵀ", Array(n) { r -> DoubleArray(n) { col -> a[col][r] } })
    val b = (0 until n).map { EngMath.parse(rhs[it]) }
    if (b.all { it != null }) {
        val x = EngMath.solve(a.map { it.copyOf() }.toTypedArray(), DoubleArray(n) { b[it]!! })
        if (x == null) Note("El sistema no tiene solución única.")
        else Out(*x.toList().flatMapIndexed { i, v -> listOf("x${i + 1}", num(v, 6)) }.toTypedArray())
    }
}

@Composable
private fun MatrixShow(title: String, m: Array<DoubleArray>) {
    val c = LocalHilo.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.band).padding(12.dp)) {
        Text(title, color = c.muted, fontSize = 13.sp)
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            m.forEach { row ->
                Row { row.forEach { v -> Text(num(v, 4), color = c.ink, fontFamily = FontFamily.Monospace, fontSize = 14.sp, modifier = Modifier.width(78.dp)) } }
            }
        }
    }
}

private fun determinant(a0: Array<DoubleArray>): Double {
    val n = a0.size; val a = Array(n) { a0[it].copyOf() }; var det = 1.0
    for (col in 0 until n) {
        val piv = (col until n).maxBy { abs(a[it][col]) }
        if (abs(a[piv][col]) < 1e-12) return 0.0
        if (piv != col) { val t = a[piv]; a[piv] = a[col]; a[col] = t; det = -det }
        det *= a[col][col]
        for (r in col + 1 until n) { val f = a[r][col] / a[col][col]; for (k in col until n) a[r][k] -= f * a[col][k] }
    }
    return det
}

private fun rankOf(a0: Array<DoubleArray>): Int {
    val n = a0.size; val a = Array(n) { a0[it].copyOf() }; var rank = 0
    for (col in 0 until n) {
        val piv = (rank until n).maxByOrNull { abs(a[it][col]) } ?: break
        if (abs(a[piv][col]) < 1e-10) continue
        val t = a[piv]; a[piv] = a[rank]; a[rank] = t
        for (r in 0 until n) if (r != rank) { val f = a[r][col] / a[rank][col]; for (k in col until n) a[r][k] -= f * a[rank][k] }
        rank++
    }
    return rank
}

private fun inverse(a0: Array<DoubleArray>): Array<DoubleArray>? {
    val n = a0.size
    val m = Array(n) { r -> DoubleArray(2 * n) { k -> if (k < n) a0[r][k] else if (k - n == r) 1.0 else 0.0 } }
    for (col in 0 until n) {
        val piv = (col until n).maxBy { abs(m[it][col]) }
        if (abs(m[piv][col]) < 1e-12) return null
        val t = m[piv]; m[piv] = m[col]; m[col] = t
        val d = m[col][col]; for (k in 0 until 2 * n) m[col][k] /= d
        for (r in 0 until n) if (r != col) { val f = m[r][col]; for (k in 0 until 2 * n) m[r][k] -= f * m[col][k] }
    }
    return Array(n) { r -> DoubleArray(n) { k -> m[r][k + n] } }
}

// =====================================================================
// Comparar dos versiones de una ley
// =====================================================================

@Composable
fun LawDiffTool(vm: HiloViewModel) {
    val c = LocalHilo.current
    val books by vm.repo.books.collectAsStateCompat()
    var a by remember { mutableStateOf<Book?>(null) }
    var b by remember { mutableStateOf<Book?>(null) }
    var picking by remember { mutableStateOf(0) }
    var show by remember { mutableStateOf("cambiados") }
    var tick by remember { mutableIntStateOf(0) }
    Note("Elige la versión vieja y la nueva de una ley o código (por ejemplo una reforma). Hilo compara artículo por artículo.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { picking = 1 }, modifier = Modifier.weight(1f)) { Text(a?.title?.take(22) ?: "Versión vieja", color = c.ink, maxLines = 1) }
        OutlinedButton(onClick = { picking = 2 }, modifier = Modifier.weight(1f)) { Text(b?.title?.take(22) ?: "Versión nueva", color = c.ink, maxLines = 1) }
    }
    if (picking != 0) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(8.dp)) {
            books.forEach { bk ->
                Text(bk.title, color = c.ink, modifier = Modifier.fillMaxWidth().clickable { if (picking == 1) a = bk else b = bk; picking = 0 }.padding(10.dp))
            }
        }
    }
    val x = a; val y = b
    if (x == null || y == null) return
    val result by produceState<Pair<LawDiff, Map<String, IntRange>>?>(null, x.id, y.id) {
        value = withContext(Dispatchers.Default) {
            val pa = Law.parse(vm.studyText(x) { _, _ -> }); val pb = Law.parse(vm.studyText(y) { _, _ -> })
            // Párrafos que ocupa cada artículo de la versión vieja, para saber qué resaltaste en cada uno
            val sorted = pa.articles.sortedBy { it.para }
            val ranges = HashMap<String, IntRange>()
            sorted.forEachIndexed { i, ar -> ranges.putIfAbsent(Law.normNum(ar.num), ar.para until (sorted.getOrNull(i + 1)?.para ?: (ar.para + 1)).coerceAtLeast(ar.para + 1)) }
            diffLaws(pa.articles.associate { Law.normNum(it.num) to it.text }, pb.articles.associate { Law.normNum(it.num) to it.text }) to ranges
        }
    }
    val (r, ranges) = result ?: run { LinearProgressIndicator(Modifier.fillMaxWidth()); Note("Leyendo y comparando…"); return }
    if (r.same + r.changed.size + r.added.size + r.removed.size == 0) { Note("No encontré artículos («Artículo 1.-…») en estos documentos."); return }
    // Lo que estudiaste de la versión vieja en los artículos que cambiaron o se quitaron
    val study = vm.study
    fun touch(num: String, oldText: String): Triple<Int, List<StudyCard>, Int> {
        val rg = ranges[num]
        val flat = normWs(oldText).lowercase()
        val marks = study.marks.count { m -> m.bookId == x.id && ((rg != null && m.para in rg) || (m.text.length >= 12 && flat.contains(normWs(m.text).lowercase()))) }
        val cards = study.cards.filter { k -> k.bookId == x.id && (Law.normNum(k.source.removePrefix("art.").trim()) == num && k.source.startsWith("art.") || (rg != null && k.sourceFlow && k.sourcePos in rg)) }
        val quotes = study.quotes.count { q -> q.bookId == x.id && q.flow && rg != null && q.pos in rg }
        return Triple(marks, cards, quotes)
    }
    val hits = remember(r, study.cards.size, study.marks.size, tick) {
        (r.changed.map { Triple(it.first, it.second, it.third) } + r.removed.map { Triple(it.first, it.second, "") })
            .mapNotNull { (n, o, nw) -> touch(n, o).takeIf { it.first + it.second.size + it.third > 0 }?.let { n to Triple(o, nw, it) } }.toMap()
    }
    if (hits.isNotEmpty()) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.lines[2].copy(alpha = 0.14f)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("⚠ La reforma toca lo que estudiaste", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                "${hits.size} ${if (hits.size == 1) "artículo cambiado o quitado tiene" else "artículos cambiados o quitados tienen"} tus resaltados, citas o tarjetas en «${x.title.take(40)}».",
                color = c.ink, fontSize = 14.sp,
            )
            val cardsAll = hits.values.flatMap { it.third.second }
            if (cardsAll.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    var up = 0; var held = 0
                    hits.values.forEach { (_, nw, t) -> t.second.forEach { k -> if (nw.isBlank()) { study.hold(k, "Derogado o quitado en la reforma"); held++ } else { study.update(updatedCard(k, nw)); up++ } } }
                    tick++
                    vm.toast = listOfNotNull(up.takeIf { it > 0 }?.let { "$it ${if (it == 1) "tarjeta actualizada" else "tarjetas actualizadas"} con el texto nuevo" }, held.takeIf { it > 0 }?.let { "$it en revisión (derogadas)" }).joinToString(" · ") + "."
                }) { Text("Actualizar mis ${cardsAll.size} ${if (cardsAll.size == 1) "tarjeta" else "tarjetas"}", color = c.ink) }
            }
            Text("Actualizar cambia la respuesta por el texto nuevo y la pone para repasar hoy; las de artículos quitados quedan «En revisión».", color = c.muted, fontSize = 12.sp)
        }
    }
    Out("Iguales", "${r.same}", "Cambiados", "${r.changed.size}", "Nuevos", "${r.added.size}", "Derogados o quitados", "${r.removed.size}")
    Choice(listOfNotNull("cambiados" to "Cambiados", "nuevos" to "Nuevos", "quitados" to "Quitados", if (hits.isNotEmpty()) "mios" to "Lo que estudié" else null), show) { show = it }
    val list = when (show) {
        "cambiados" -> r.changed
        "nuevos" -> r.added.map { Triple(it.first, "", it.second) }
        "mios" -> hits.map { (n, v) -> Triple(n, v.first, v.second) }
        else -> r.removed.map { Triple(it.first, it.second, "") }
    }
    if (list.isEmpty()) Note("Ninguno.")
    list.take(80).forEach { (num, old, new) ->
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Artículo $num", color = c.accent, fontWeight = FontWeight.Bold)
            hits[num]?.third?.let { (m, k, q) ->
                Text(
                    "Estudiaste: " + listOfNotNull(
                        m.takeIf { it > 0 }?.let { "$it ${if (it == 1) "resaltado" else "resaltados"}" },
                        k.size.takeIf { it > 0 }?.let { "$it ${if (it == 1) "tarjeta" else "tarjetas"}" },
                        q.takeIf { it > 0 }?.let { "$it ${if (it == 1) "cita" else "citas"}" },
                    ).joinToString(" · "),
                    color = c.lines[2], fontSize = 12.sp, fontWeight = FontWeight.Medium,
                )
            }
            Text(wordDiff(old, new, c.lines[0], c.lines[3]), color = c.ink, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
    Note("Tachado en rojo: lo que se quitó. En verde: lo que se agregó. Compara por número de artículo: si la reforma renumeró, verás artículos «nuevos» y «quitados».")
}

/** La tarjeta de un artículo con la respuesta reemplazada por el texto reformado; se repasa hoy. */
private fun updatedCard(k: StudyCard, newText: String): StudyCard {
    val body = normWs(newText).replace(Regex("^(Art[íi]culo|ART[ÍI]CULO|Art\\.)\\s*[\\w.]+\\s*[.:\\-–]*\\s*"), "").take(1500)
    return k.copy(back = body.ifBlank { newText.take(1500) }, due = today(), held = "")
}

data class LawDiff(val same: Int, val changed: List<Triple<String, String, String>>, val added: List<Pair<String, String>>, val removed: List<Pair<String, String>>)

private fun normWs(s: String) = s.replace(Regex("\\s+"), " ").trim()

fun diffLaws(a: Map<String, String>, b: Map<String, String>): LawDiff {
    var same = 0
    val changed = ArrayList<Triple<String, String, String>>()
    for ((k, v) in a) {
        val w = b[k] ?: continue
        if (normWs(v) == normWs(w)) same++ else changed.add(Triple(k, v, w))
    }
    fun key(s: String) = Regex("\\d+").find(s)?.value?.toIntOrNull() ?: 0
    return LawDiff(
        same, changed.sortedBy { key(it.first) },
        b.filterKeys { it !in a }.toList().sortedBy { key(it.first) },
        a.filterKeys { it !in b }.toList().sortedBy { key(it.first) },
    )
}

/** Diferencia palabra por palabra (LCS), con lo quitado tachado y lo agregado resaltado. */
fun wordDiff(old: String, new: String, del: Color, add: Color) = buildAnnotatedString {
    val a = normWs(old).split(' ').filter { it.isNotEmpty() }.take(600)
    val b = normWs(new).split(' ').filter { it.isNotEmpty() }.take(600)
    val dp = Array(a.size + 1) { IntArray(b.size + 1) }
    for (i in a.size - 1 downTo 0) for (j in b.size - 1 downTo 0) dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
    var i = 0; var j = 0
    while (i < a.size || j < b.size) {
        when {
            i < a.size && j < b.size && a[i] == b[j] -> { append(a[i] + " "); i++; j++ }
            j < b.size && (i >= a.size || dp[i][j + 1] >= dp[i + 1][j]) -> { withStyle(SpanStyle(color = add, fontWeight = FontWeight.Bold, background = add.copy(alpha = 0.12f))) { append(b[j]) }; append(" "); j++ }
            else -> { withStyle(SpanStyle(color = del, textDecoration = TextDecoration.LineThrough)) { append(a[i]) }; append(" "); i++ }
        }
    }
}

@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateCompat() = collectAsState()
