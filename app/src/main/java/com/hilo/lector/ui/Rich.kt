package com.hilo.lector.ui

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

// =====================================================================
// Fórmulas (LaTeX con KaTeX, sin internet) y código con colores
// =====================================================================

/** Fórmulas escritas como LaTeX: $V = I \cdot R$ o $$\frac{1}{2\pi RC}$$. Se dibujan con KaTeX guardado en la app. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MathView(tex: String, color: Color, sizeSp: Float = 18f, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    var h by remember(tex) { mutableIntStateOf(40) }
    val hex = String.format("#%06X", 0xFFFFFF and android.graphics.Color.argb((color.alpha * 255).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt()))
    val body = if (tex.contains("$")) tex else "$$" + tex + "$$"
    val html = """<html><head><meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="file:///android_asset/katex/katex.min.css">
<script src="file:///android_asset/katex/katex.min.js"></script>
<style>body{margin:0;padding:2px 0;background:transparent;color:$hex;font-size:${sizeSp}px;font-family:sans-serif;overflow-x:auto;}</style></head>
<body><div id="m"></div><script>
var src=${JSONObject.quote(body)};
var out=document.getElementById('m');
function esc(s){return s.replace(/&/g,'&amp;').replace(/</g,'&lt;');}
try{
 var parts=src.split(/(\$\$[\s\S]+?\$\$|\$[^$]+\$)/g), html='';
 for(var i=0;i<parts.length;i++){var p=parts[i]; if(!p)continue;
  if(p.indexOf('$$')==0){html+=katex.renderToString(p.slice(2,-2),{displayMode:true,throwOnError:false});}
  else if(p.charAt(0)=='$'){html+=katex.renderToString(p.slice(1,-1),{throwOnError:false});}
  else html+=esc(p).replace(/\n/g,'<br>');}
 out.innerHTML=html;
}catch(e){out.textContent=src;}
setTimeout(function(){Hilo.size(document.body.scrollHeight);},60);
</script></body></html>"""
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                isVerticalScrollBarEnabled = false
                addJavascriptInterface(object {
                    @JavascriptInterface fun size(px: Int) { post { h = px } }
                }, "Hilo")
            }
        },
        update = { it.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null) },
        modifier = modifier.fillMaxWidth().height(with(density) { (h.coerceAtLeast(20) + 4).dp }),
    )
}

private val KEYWORDS = setOf(
    "if", "else", "for", "while", "do", "return", "break", "continue", "switch", "case", "default", "try", "catch", "finally", "throw", "new",
    "class", "def", "fun", "val", "var", "let", "const", "function", "import", "from", "package", "public", "private", "protected", "static",
    "void", "int", "float", "double", "char", "bool", "boolean", "string", "String", "true", "false", "null", "None", "True", "False", "self", "this",
    "and", "or", "not", "in", "is", "lambda", "struct", "enum", "interface", "extends", "implements", "async", "await", "elif", "pass", "with", "as",
    "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "JOIN", "GROUP", "ORDER", "BY", "include", "using", "namespace", "std", "print", "println", "printf",
)

/** Código con colores sencillos: palabras clave, textos, números y comentarios. */
fun highlightCode(code: String, ink: Color, kw: Color, str: Color, num: Color, com: Color): AnnotatedString = buildAnnotatedString {
    val rx = Regex("(//[^\\n]*|#[^\\n]*|/\\*[\\s\\S]*?\\*/)|(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')|\\b(\\d+(?:\\.\\d+)?)\\b|\\b([A-Za-z_][A-Za-z0-9_]*)\\b")
    var last = 0
    for (m in rx.findAll(code)) {
        append(code.substring(last, m.range.first))
        val (style, text) = when {
            m.groups[1] != null -> SpanStyle(color = com, fontStyle = FontStyle.Italic) to m.value
            m.groups[2] != null -> SpanStyle(color = str) to m.value
            m.groups[3] != null -> SpanStyle(color = num) to m.value
            m.value in KEYWORDS -> SpanStyle(color = kw, fontWeight = FontWeight.Bold) to m.value
            else -> SpanStyle(color = ink) to m.value
        }
        pushStyle(style); append(text); pop()
        last = m.range.last + 1
    }
    append(code.substring(last))
}

@Composable
fun CodeBlock(code: String, modifier: Modifier = Modifier) {
    val c = LocalHilo.current
    val dark = Color(0xFF1E2230)
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(dark).horizontalScroll(rememberScrollState()).padding(12.dp)) {
        Text(
            highlightCode(code.trim('\n'), Color(0xFFE6E6E6), Color(0xFF82AAFF), Color(0xFFC3E88D), Color(0xFFF78C6C), Color(0xFF8A8FA3)),
            fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp, softWrap = false,
        )
    }
    @Suppress("UNUSED_VARIABLE") val unused = c
}

/** Texto de una tarjeta o un apunte: con bloques ``` de código y fórmulas entre $…$. */
@Composable
fun RichText(text: String, color: Color, size: TextUnit = 18.sp, weight: FontWeight = FontWeight.Normal) {
    val parts = remember(text) { text.split(Regex("```[a-zA-Z0-9+#]*\\n?")) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { i, p ->
            if (p.isBlank()) return@forEachIndexed
            when {
                i % 2 == 1 -> CodeBlock(p)
                Regex("\\$[^$\\s][^$]*\\$").containsMatchIn(p) -> MathView(p.trim(), color, size.value)
                else -> Text(p.trim(), color = color, fontSize = size, fontWeight = weight, lineHeight = (size.value * 1.3f).sp)
            }
        }
    }
}

/** ¿Hace falta dibujar el texto (código o fórmulas) o basta un Text común? */
fun isRich(text: String) = text.contains("```") || Regex("\\$[^$\\s][^$]*\\$").containsMatchIn(text)

// =====================================================================
// Tarjetas de imagen tapada y de problema paso a paso
// =====================================================================

/** Imagen con rectángulos que tapan partes: [i] es la parte que se pregunta. */
@Composable
fun OcclusionView(json: String, revealed: Boolean, modifier: Modifier = Modifier) {
    val c = LocalHilo.current
    val o = remember(json) { runCatching { JSONObject(json) }.getOrNull() } ?: return
    val path = o.optString("img")
    val boxes = remember(json) { o.optJSONArray("boxes")?.let { a -> (0 until a.length()).map { k -> a.getJSONArray(k).let { b -> floatArrayOf(b.getDouble(0).toFloat(), b.getDouble(1).toFloat(), b.getDouble(2).toFloat(), b.getDouble(3).toFloat()) } } } ?: emptyList() }
    val target = o.optInt("i", 0)
    val img by produceState<ImageBitmap?>(null, path) { value = withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() } }
    val b = img ?: run { Text("No encuentro la imagen.", color = c.muted); return }
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(b.width.toFloat() / b.height).clip(RoundedCornerShape(12.dp))) {
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
        Image(b, null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        boxes.forEachIndexed { k, r ->
            val isT = k == target
            if (isT && revealed) {
                Box(
                    Modifier.offset { IntOffset((r[0] * w).roundToInt(), (r[1] * h).roundToInt()) }
                        .size(with(LocalDensity.current) { ((r[2] - r[0]) * w).toDp() }, with(LocalDensity.current) { ((r[3] - r[1]) * h).toDp() })
                        .border(3.dp, c.accent, RoundedCornerShape(4.dp)),
                )
            } else Box(
                Modifier.offset { IntOffset((r[0] * w).roundToInt(), (r[1] * h).roundToInt()) }
                    .size(with(LocalDensity.current) { ((r[2] - r[0]) * w).toDp() }, with(LocalDensity.current) { ((r[3] - r[1]) * h).toDp() })
                    .clip(RoundedCornerShape(4.dp)).background(if (isT) Color(0xFFE0644F) else Color(0xFF5B7CFA).copy(alpha = 0.85f)),
            )
        }
    }
}

/** Problema: los pasos se muestran de a uno, para intentar el siguiente antes de verlo. */
@Composable
fun ProblemSteps(steps: List<String>, color: Color) {
    val c = LocalHilo.current
    var n by remember(steps) { mutableIntStateOf(1) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.take(n).forEachIndexed { i, s ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.band).padding(12.dp)) {
                Text("Paso ${i + 1}", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (isRich(s)) RichText(s, color, 17.sp) else Text(s, color = color, fontSize = 17.sp, lineHeight = 23.sp)
            }
        }
        if (n < steps.size) OutlinedButton(onClick = { n++ }) { Text("Ver el paso ${n + 1} de ${steps.size}", color = c.ink) }
    }
}

/** Guarda una copia de la imagen dentro de Hilo (para que la tarjeta no dependa de la galería). */
fun keepImage(ctx: android.content.Context, uri: android.net.Uri): String? = runCatching {
    val dir = File(ctx.filesDir, "imagenes").apply { mkdirs() }
    val f = File(dir, "img_${System.currentTimeMillis()}.jpg")
    ctx.contentResolver.openInputStream(uri)?.use { input ->
        val bmp = BitmapFactory.decodeStream(input) ?: return null
        val scale = minOf(1f, 1600f / maxOf(bmp.width, bmp.height))
        val out = if (scale < 1f) android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        f.outputStream().use { out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
    }
    f.path
}.getOrNull()
