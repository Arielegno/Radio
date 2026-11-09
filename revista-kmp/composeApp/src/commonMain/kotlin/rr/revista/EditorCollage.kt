package rr.revista

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Modo collage: pegar fotos, letras recortadas, stickers, cintas y formas; moverlos con el dedo, agrandar y girar con dos dedos. */
@Composable
fun EditorCollage(d: Diseno, portada: String, cerrar: () -> Unit, listo: (List<ItemCollage>) -> Unit) {
    val scope = rememberCoroutineScope()
    var col by remember { mutableStateOf(d.col) }
    var sel by remember { mutableStateOf(-1) }
    var pidiendoTexto by remember { mutableStateOf(false) }
    val ar = if (d.cpos == "cabecera") when (d.ch) { "cuadrado" -> 1f; "horizontal" -> 16f / 9f; else -> 9f / 16f } else 9f / 16f
    fun arriba() = (col.maxOfOrNull { it.z } ?: 0) + 1
    fun agregar(i: ItemCollage) {
        if (col.size >= 30) { Aviso.mostrar("Máximo 30 recortes."); return }
        col = col + i.copy(x = (32 + Random.nextInt(36)).toDouble(), y = (28 + Random.nextInt(40)).toDouble(), r = (Random.nextInt(11) - 5).toDouble(), z = arriba()); sel = col.size - 1
    }
    fun cambiar(f: (ItemCollage) -> ItemCollage) { if (sel in col.indices) col = col.toMutableList().also { it[sel] = f(it[sel]) } }

    Column(Modifier.fillMaxSize().background(Color(0xFF101114))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Enlace("Cancelar", Tenue, cerrar)
            Text("Collage", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
            Boton("Listo") { listo(col) }
        }
        // Lienzo con el mismo fondo del artículo
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF17191D)).padding(10.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxHeight().aspectRatio(ar, matchHeightConstraintsFirst = true).clip(RoundedCornerShape(6.dp))
                .drawBehind { if (d.bg == "none" || d.bg == "cover") drawRect(Brush.linearGradient(listOf(Color(0xFF2A2E36), Color(0xFF15171B)))) else fondoDiseno(d) }
                .clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) { sel = -1 }) {
                Collage(col, Modifier.fillMaxSize(), sel, { sel = it }) { i, dx, dy, zoom, giro ->
                    if (i in col.indices) col = col.toMutableList().also { l ->
                        val it = l[i]
                        var r = it.r + giro; if (r > 180) r -= 360; if (r < -180) r += 360
                        l[i] = it.copy(x = (it.x + dx * 100).coerceIn(-20.0, 120.0), y = (it.y + dy * 100).coerceIn(-20.0, 120.0), w = (it.w * zoom).coerceIn(2.0, 140.0), r = r)
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().heightIn(max = 330.dp).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Boton("Foto", principal = false) { val e = elegirImagenes(900, 300, true); scope.launch { e.esperar().forEach { f -> agregar(ItemCollage("img", 50.0, 50.0, 50.0, 0.0, 1, 100.0, s = f.grande, cut = "rect")) } } }
                if (portada.isNotEmpty()) Boton("Portada", principal = false) { agregar(ItemCollage("img", 50.0, 50.0, 60.0, 0.0, 1, 100.0, s = portada, cut = "roto")) }
                Boton("Aa Letras", principal = false) { pidiendoTexto = true }
                Boton("Sticker", principal = false) { agregar(ItemCollage("stk", 50.0, 50.0, 30.0, 0.0, 1, 100.0, v = "estallido", t = "¡Nuevo!")) }
                Boton("Cinta", principal = false) { agregar(ItemCollage("tape", 50.0, 50.0, 30.0, -20.0, 1, 100.0, v = "beige")) }
                Boton("Forma", principal = false) { agregar(ItemCollage("shp", 50.0, 50.0, 40.0, 0.0, 0, 100.0, c = 0xFFFF3D8B, v = "circulo")) }
            }
            val it = col.getOrNull(sel)
            if (it == null) Text("Toca un recorte para editarlo. Arrástralo para moverlo; con dos dedos lo agrandas y lo giras.", color = Tenue, fontSize = 14.sp)
            else {
                Control("Tamaño", it.w.toFloat(), 2f..140f) { v -> cambiar { c -> c.copy(w = v.toDouble()) } }
                Control("Giro", it.r.toFloat(), -180f..180f) { v -> cambiar { c -> c.copy(r = v.toDouble()) } }
                Control("Transparencia", it.o.toFloat(), 10f..100f) { v -> cambiar { c -> c.copy(o = v.toDouble()) } }
                when (it.k) {
                    "img" -> Pestanas(Diseno.CORTES.map { k -> k to mapOf("rect" to "Foto", "roto" to "Papel roto", "polaroid" to "Polaroid", "circulo" to "Círculo", "ovalo" to "Óvalo", "estrella" to "Estrella", "corazon" to "Corazón")[k]!! }, it.cut) { v -> cambiar { c -> c.copy(cut = v) } }
                    "txt" -> {
                        Campo(it.t, { v -> cambiar { c -> c.copy(t = v.take(60).ifEmpty { " " }) } }, "Texto", lineas = 2, max = 60)
                        Pestanas(Diseno.TEXTOS.map { k -> k to mapOf("recorte" to "Letras recortadas", "titular" to "Titular", "maquina" to "Máquina", "marcador" to "Marcador", "neon" to "Neón", "cinta" to "Cinta")[k]!! }, it.v) { v -> cambiar { c -> c.copy(v = v) } }
                        if (it.v != "recorte") ColoresCollage(it.c) { v -> cambiar { c -> c.copy(c = v) } }
                    }
                    "stk" -> {
                        Pestanas(Diseno.STICKERS.map { k -> k to (Diseno.GLIFOS[k] ?: mapOf("estallido" to "Estallido", "circulo" to "Círculo", "etiqueta" to "Etiqueta")[k] ?: k) }, it.v) { v -> cambiar { c -> c.copy(v = v) } }
                        if (Diseno.GLIFOS[it.v] == null) Campo(it.t, { v -> cambiar { c -> c.copy(t = v.take(24)) } }, "Texto del sticker", max = 24)
                        ColoresCollage(it.c) { v -> cambiar { c -> c.copy(c = v) } }
                    }
                    "tape" -> Pestanas(Diseno.CINTAS.keys.map { k -> k to k.replaceFirstChar { ch -> ch.uppercase() } }, it.v) { v -> cambiar { c -> c.copy(v = v) } }
                    else -> { Pestanas(Diseno.FORMAS.map { k -> k to mapOf("circulo" to "Círculo", "franja" to "Franja", "trama" to "Trama", "rayas" to "Rayas", "cuadros" to "Cuadros", "spray" to "Spray", "marco" to "Marco")[k]!! }, it.v) { v -> cambiar { c -> c.copy(v = v) } }; ColoresCollage(it.c) { v -> cambiar { c -> c.copy(c = v) } } }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Boton("Al frente", principal = false) { cambiar { c -> c.copy(z = arriba()) } }
                    Boton("Atrás", principal = false) { val i = sel; col = col.mapIndexed { k, c -> if (k == i) c.copy(z = 0) else c.copy(z = minOf(99, c.z + 1)) } }
                    Boton("Duplicar", principal = false) { if (col.size < 30) { col = col + it.copy(x = minOf(100.0, it.x + 6), y = minOf(100.0, it.y + 6), z = arriba()); sel = col.size - 1 } }
                    Boton("Borrar", principal = false) { col = col.filterIndexed { k, _ -> k != sel }; sel = -1 }
                }
            }
        }
    }
    if (pidiendoTexto) DialogoTexto("Letras para el collage", "", "Texto (máximo 60)", "Pegar", max = 60, cerrar = { pidiendoTexto = false }) { t ->
        pidiendoTexto = false
        if (t.isNotBlank()) agregar(ItemCollage("txt", 50.0, 50.0, 40.0, 0.0, 1, 100.0, t = t.trim().take(60), v = "recorte"))
    }
}

@Composable
private fun Control(etiqueta: String, valor: Float, rango: ClosedFloatingPointRange<Float>, cambiar: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
        Slider(valor.coerceIn(rango), cambiar, valueRange = rango, colors = SliderDefaults.colors(thumbColor = Amarillo, activeTrackColor = Amarillo), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ColoresCollage(actual: Long?, elegir: (Long) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0xFF141414, 0xFFFFFFFF, 0xFFF2C230, 0xFFFF3D8B, 0xFFE3242B, 0xFF29D3F0, 0xFF3ECF8E, 0xFFA78BFA, 0xFFFF8A3D, 0xFF2D47C4).forEach { c ->
            Box(Modifier.size(32.dp).clip(CircleShape).background(col(c)).border(if (actual == c) 3.dp else 1.dp, if (actual == c) Amarillo else Color(0x55FFFFFF), CircleShape).clickable { elegir(c) })
        }
    }
}
