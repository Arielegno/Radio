package rr.revista

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sistema de #: las etiquetas que se escriben dentro del texto (#grunge) se vuelven enlaces,
 * se agregan solas al artículo, y cada # tiene su página. También las tendencias (los # más usados).
 */
object Hashtags {
    // «#palabra» al inicio o después de un espacio o signo (así «## Título», «C#» o una dirección web no cuentan)
    private val RE = Regex("(^|[\\s(¡¿\"'«])#([A-Za-z0-9_ñÑáéíóúüÁÉÍÓÚÜ]{2,24})")

    /** Como se guarda: minúsculas y solo letras/números (lo mismo que acepta la base de datos). */
    fun normal(t: String): String = t.lowercase().replace(Regex("[^a-z0-9ñáéíóúü]"), "").take(24)

    /** Los # escritos en un texto, sin repetir. */
    fun extraer(texto: String): List<String> =
        RE.findAll(texto).map { normal(it.groupValues[2]) }.filter { it.length >= 2 }.distinct().toList()

    /** Junta las etiquetas del campo con las del texto (máximo 5, como permite la base de datos). */
    fun juntar(campo: List<String>, texto: String): List<String> = (campo + extraer(texto)).filter { it.isNotEmpty() }.distinct().take(5)

    fun abrir(t: String) { val n = normal(t); if (n.isNotEmpty()) Nav.ir(Destino.Etiqueta(n)) }

    /** Desde un enlace «#tag=rese%C3%B1a»: deshace los %XX (acentos y ñ). */
    fun abrirDesdeEnlace(crudo: String) {
        val bytes = mutableListOf<Byte>(); var i = 0
        while (i < crudo.length) {
            val c = crudo[i]
            val hex = if (c == '%' && i + 2 <= crudo.lastIndex) crudo.substring(i + 1, i + 3).toIntOrNull(16) else null
            if (hex != null) { bytes += hex.toByte(); i += 3 }
            else { c.toString().encodeToByteArray().forEach { bytes += it }; i++ }
        }
        abrir(bytes.toByteArray().decodeToString())
    }

    /** Agrega un texto convirtiendo cada #palabra en un enlace tocable. */
    fun agregar(b: AnnotatedString.Builder, s: String, color: Color) {
        var desde = 0
        for (m in RE.findAll(s)) {
            val g = m.groups[2] ?: continue
            val inicioHash = g.range.first - 1
            b.append(s.substring(desde, inicioHash))
            val tag = normal(g.value)
            b.withLink(LinkAnnotation.Clickable("tag:$tag", TextLinkStyles(SpanStyle(color = color, fontWeight = FontWeight.SemiBold))) { abrir(tag) }) { append("#" + g.value) }
            desde = g.range.last + 1
        }
        b.append(s.substring(desde))
    }

    fun texto(s: String, color: Color): AnnotatedString = buildAnnotatedString { agregar(this, s, color) }

    // Tendencias: los # más usados en los artículos de los últimos 30 días (se recuerdan 10 minutos)
    private var cache: Pair<Double, List<Pair<String, Int>>>? = null
    suspend fun tendencias(): List<Pair<String, Int>> {
        cache?.let { (t, l) -> if (ahoraMs() - t < 600_000.0) return l }
        val desde = Supa.enc(ahoraIsoMenos(30 * 86_400))
        val l = runCatching {
            Json.arr(Cuenta.api("GET", "/rest/v1/posts?select=tags&tags=neq.%7B%7D&created_at=gte.$desde${Supa.publicados()}&order=created_at.desc&limit=400"))
                .mapNotNull { it.asObj() }.flatMap { o -> o.list("tags").mapNotNull { it as? String } }
                .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(15).map { it.key to it.value }
        }.getOrDefault(emptyList())
        if (l.isNotEmpty()) cache = ahoraMs() to l
        return l
    }
}

/** Fila de tendencias (#… más usados). */
@Composable
fun FilaTendencias(titulo: String = "Tendencias") {
    val l by produceState<List<Pair<String, Int>>>(emptyList()) { value = Hashtags.tendencias() }
    if (l.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)) {
        Text(titulo, color = Tenue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            l.forEach { (t, n) ->
                Text("#$t  ·  $n", color = Amarillo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, Amarillo.copy(alpha = .45f), RoundedCornerShape(999.dp))
                        .mano().clickable { Hashtags.abrir(t) }.padding(horizontal = 12.dp, vertical = 7.dp))
            }
        }
    }
}

/** La página de un #: todos los artículos que lo usan. */
@Composable
fun EtiquetaPantalla(tag: String) {
    val r by produceState<Result<List<Articulo>>?>(null, tag) { value = runCatching { Supa.portada(limite = 60, etiqueta = tag) } }
    LazyVerticalGrid(GridCells.Adaptive(330.dp), Modifier.fillMaxSize()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Enlace("‹ Volver") { Nav.atras() }
                Text("#$tag", color = Amarillo, fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, maxLines = 2, modifier = Modifier.padding(top = 8.dp))
                val n = r?.getOrNull()?.size
                if (n != null) Text(if (n >= 60) "Más de 60 artículos" else "$n ${if (n == 1) "artículo" else "artículos"}", color = Tenue)
            }
        }
        val res = r
        when {
            res == null -> item(span = { GridItemSpan(maxLineSpan) }) { CircularProgressIndicator(color = Amarillo, modifier = Modifier.padding(24.dp)) }
            res.isFailure -> item(span = { GridItemSpan(maxLineSpan) }) { Text(res.exceptionOrNull()?.message ?: "Sin conexión.", color = Tenue, modifier = Modifier.padding(18.dp)) }
            res.getOrThrow().isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Todavía no hay artículos con #$tag. Escribe uno y ponle #$tag en el texto o en las etiquetas.", color = Tenue, modifier = Modifier.padding(18.dp))
            }
            else -> items(res.getOrThrow(), key = { it.id }) { a -> Tarjeta(a, alEtiqueta = { Hashtags.abrir(it) }) { Nav.ir(Destino.Leer(a.id)) } }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { FilaTendencias("Otros # populares") }
    }
}
