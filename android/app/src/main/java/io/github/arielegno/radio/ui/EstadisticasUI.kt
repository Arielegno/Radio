package io.github.arielegno.radio.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.arielegno.radio.Datos
import io.github.arielegno.radio.RadioApp
import io.github.arielegno.radio.core.Json
import io.github.arielegno.radio.core.normNombre
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EstadisticasPantalla(abrirResumen: () -> Unit, abrirMapa: () -> Unit) {
    val st = RadioApp.stats
    var version by remember { mutableIntStateOf(0) }
    val favs = remember(version) { st.favs.entries.toList() }
    val top = remember(version) { st.masEscuchadas(20) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Tu resumen", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        BotonGrande("Tus viajes musicales", "El mapa de dónde sonó cada canción", Icons.Filled.Map, listOf(androidx.compose.ui.graphics.Color(0xFF3ECF8E), androidx.compose.ui.graphics.Color(0xFF2B8FD6)), abrirMapa)
        BotonGrande("Tu resumen", "Tus artistas y canciones del mes, del año o de siempre", Icons.Filled.AutoAwesome, listOf(Amarillo, Naranja, Rosa), abrirResumen)
        Tarjeta(Modifier.padding(bottom = 14.dp)) {
        Titulo("Favoritas (${favs.size})")
        if (favs.isEmpty()) Text("Toca el corazón mientras suena una canción para guardarla aquí.", color = LocalExtra.current.tenue)
        favs.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, null, tint = Rosa, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable {
                    RadioApp.radio.iniciar({ normNombre(it.name) == normNombre(v.first) && normNombre(it.artist) == normNombre(v.second) })
                }) { Text(v.first, fontWeight = FontWeight.SemiBold); Text(v.second, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue) }
                IconButton(onClick = { st.favs.remove(k); Datos.guardarStats(); version++ }) { Icon(Icons.Filled.Close, "Quitar de favoritas") }
            }
        }
        }
        Tarjeta(Modifier.padding(bottom = 14.dp)) {
        Titulo("Más escuchadas")
        if (top.isEmpty()) Text("Cada canción cuenta después de 30 segundos sonando.", color = LocalExtra.current.tenue)
        top.forEachIndexed { i, s ->
            val lugar = s.p.entries.maxByOrNull { it.value }?.key ?: ""
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", color = Amarillo, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.n, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOf(s.a, if (lugar != "Sin ubicación") lugar else "").filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue)
                }
                Text("${s.c}", fontWeight = FontWeight.Bold)
            }
        }
        }
        val lugares = remember(version) { st.porUbicacion(5) }
        Tarjeta(Modifier.padding(bottom = 14.dp)) {
            Titulo("Más escuchadas por ubicación")
            if (lugares.isEmpty()) Text("Activa la ubicación y aquí verás qué suena más en cada lugar: en casa, en el trabajo, en la carretera…", color = LocalExtra.current.tenue)
            lugares.take(8).forEachIndexed { i, (lugar, canciones) ->
                if (i > 0) androidx.compose.material3.HorizontalDivider(color = LocalExtra.current.borde, modifier = Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Map, null, tint = Amarillo, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(lugar, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${canciones.sumOf { it.third }}", color = LocalExtra.current.tenue)
                }
                canciones.take(3).forEach { (n, a, c) ->
                    Row(Modifier.fillMaxWidth().padding(start = 26.dp, top = 4.dp)) {
                        Text("$n — $a", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$c", style = MaterialTheme.typography.bodyMedium, color = LocalExtra.current.tenue)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BotonGrande(t: String, sub: String, icono: androidx.compose.ui.graphics.vector.ImageVector, colores: List<androidx.compose.ui.graphics.Color>, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(colores)).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(t, color = Tinta, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp); Text(sub, color = Tinta.copy(alpha = 0.85f)) }
        Icon(icono, null, tint = Tinta, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun ResumenDialogo(cerrar: () -> Unit) {
    var rango by remember { mutableStateOf("month") }
    val ctx = LocalContext.current
    val hoy = LocalDate.now()
    val r = remember(rango) { RadioApp.stats.resumen(rango, hoy) }
    val etiqueta = when (rango) { "month" -> hoy.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es"))); "year" -> "Tu ${hoy.year}"; else -> "Desde que usas la radio" }
    Dialog(onDismissRequest = cerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth().padding(14.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tu resumen", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = cerrar) { Icon(Icons.Filled.Close, "Cerrar") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                listOf("month" to "Este mes", "year" to "Este año", "all" to "Siempre").forEach { (k, n) ->
                    FilterChip(selected = rango == k, onClick = { rango = k }, label = { Text(n) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Amarillo, selectedLabelColor = Tinta))
                }
            }
            if (r == null) { Text("Todavía no hay escuchas en este periodo. Cada canción cuenta después de 30 segundos sonando.", color = LocalExtra.current.tenue); return@Column }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Amarillo, Naranja, Rosa))).padding(18.dp)) {
                Text(etiqueta.replaceFirstChar { it.uppercase() }, color = Tinta, fontWeight = FontWeight.Bold)
                Text("${r.c}", color = Tinta, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold); Text("canciones escuchadas", color = Tinta)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column { Text("~${r.minutos}", color = Tinta, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp); Text("minutos", color = Tinta) }
                    Column { Text("${r.a.size}", color = Tinta, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp); Text("artistas", color = Tinta) }
                    if (r.days > 0) Column { Text("${r.days}", color = Tinta, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp); Text("días con música", color = Tinta) }
                }
            }
            Titulo("Tus artistas", Modifier.padding(top = 14.dp))
            r.topArtistas(5).forEachIndexed { i, (a, c) -> FilaTop(i + 1, a, "", c) }
            Titulo("Tus canciones", Modifier.padding(top = 10.dp))
            r.topCanciones(5).forEachIndexed { i, (nc, c) -> FilaTop(i + 1, nc.first, nc.second, c) }
            r.p.entries.filter { it.key != "Sin ubicación" }.maxByOrNull { it.value }?.let { Titulo("Donde más sonó", Modifier.padding(top = 10.dp)); Text(it.key, style = MaterialTheme.typography.titleMedium) }
            OutlinedButton(onClick = {
                val texto = buildString {
                    append("Mi resumen en Radio de ruta (${etiqueta}): ${r.c} canciones.\n")
                    r.topArtistas(3).forEachIndexed { i, (a, _) -> append("${i + 1}. $a\n") }
                    r.topCanciones(1).firstOrNull()?.let { append("Mi canción: ${it.first.first} — ${it.first.second}\n") }
                }
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), "Compartir resumen"))
            }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Icon(Icons.Filled.Share, null); Spacer(Modifier.width(8.dp)); Text("Compartir") }
        }
    }
}

@Composable
private fun FilaTop(n: Int, t: String, sub: String, c: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$n", color = Amarillo, fontWeight = FontWeight.Bold, modifier = Modifier.width(26.dp))
        Column(Modifier.weight(1f)) {
            Text(t, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue)
        }
        Text("$c", fontWeight = FontWeight.Bold)
    }
}

/** Mapa de tus viajes: Leaflet + mosaicos de OpenStreetMap dentro de una vista web. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapaPantalla(cerrar: () -> Unit) {
    val pts = remember { RadioApp.stats.puntosMapa() }
    Dialog(onDismissRequest = cerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Tus viajes musicales", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = cerrar) { Icon(Icons.Filled.Close, "Cerrar") }
            }
            if (pts.isEmpty()) {
                Text("Todavía no hay viajes en el mapa. Activa la ubicación y escucha música en el camino: cada canción queda marcada en el lugar donde sonó (solo en tu celular).",
                    color = LocalExtra.current.tenue, modifier = Modifier.padding(16.dp))
                return@Column
            }
            val datos = Json.write(pts.map { p -> linkedMapOf("lat" to p.lat, "lon" to p.lon, "c" to p.c, "a" to p.lugar, "s" to p.canciones.map { listOf(it.first, it.second) }) })
            val html = """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css">
<style>html,body,#m{height:100%;margin:0;background:#e8e4da;font-family:sans-serif}#e{padding:16px;color:#333}</style></head>
<body><div id="m"></div><script src="https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js"></script><script>
var P=$datos;function esc(t){return String(t).replace(/[&<>"]/g,function(c){return{'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]})}
if(!window.L){document.body.innerHTML='<p id="e">No se pudo cargar el mapa. Revisa tu conexión.</p>'}else{
var m=L.map('m');L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:18,attribution:'© colaboradores de OpenStreetMap'}).addTo(m);
var mx=Math.max.apply(null,P.map(function(p){return p.c}));
P.forEach(function(p){var s=p.s.map(function(x){return '<li>'+esc(x[0])+' ('+x[1]+')</li>'}).join('');
L.circleMarker([p.lat,p.lon],{radius:6+14*Math.sqrt(p.c/mx),color:'#ff8a5c',weight:2,fillColor:'#f2c230',fillOpacity:.75}).addTo(m)
.bindPopup('<b>'+esc(p.a||'Aquí')+'</b><br>'+p.c+(p.c==1?' canción':' canciones')+'<ul style="margin:6px 0 0;padding-left:18px">'+s+'</ul>')});
m.fitBounds(L.latLngBounds(P.map(function(p){return [p.lat,p.lon]})).pad(.25),{maxZoom:13});}
</script></body></html>"""
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(factory = { c ->
                    WebView(c).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL("https://localhost/", html, "text/html", "utf-8", null)
                    }
                }, modifier = Modifier.fillMaxSize())
            }
            Text("Cada punto es un lugar donde sonó tu música. Se guarda solo en este celular.", style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue, modifier = Modifier.padding(12.dp))
        }
    }
}
