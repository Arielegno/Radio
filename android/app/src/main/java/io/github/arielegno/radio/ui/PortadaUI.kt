package io.github.arielegno.radio.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.arielegno.radio.RadioApp
import io.github.arielegno.radio.core.Concierto
import io.github.arielegno.radio.core.Efemerides
import io.github.arielegno.radio.core.Modos
import io.github.arielegno.radio.core.PortadaCfg
import io.github.arielegno.radio.core.Track
import io.github.arielegno.radio.core.fmtDur
import io.github.arielegno.radio.core.normNombre
import io.github.arielegno.radio.core.saludo
import io.github.arielegno.radio.Revista
import kotlinx.coroutines.isActive
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Acciones que la portada le pide a la app. */
class AccionesPortada(
    val carro: () -> Unit,
    val resumen: () -> Unit,
    val mapa: () -> Unit,
    val revista: (String) -> Unit,
    val biblioteca: () -> Unit,
    val ubicacion: () -> Unit,
    val compartir: () -> Unit,
    val probarVoz: () -> Unit,
    val probarLuna: () -> Unit,
    val notificaciones: () -> Unit
)

fun iconoModo(m: String): ImageVector = when (m) {
    "carretera" -> Icons.Filled.DirectionsCar; "hogar" -> Icons.Filled.Home; "estudiando" -> Icons.AutoMirrored.Filled.MenuBook
    "ejercicio" -> Icons.Filled.FitnessCenter; "videojuegos" -> Icons.Filled.SportsEsports; else -> Icons.Filled.RadioButtonUnchecked
}

@Composable
fun Portada(acc: AccionesPortada) {
    val r = RadioApp.radio; val p = RadioApp.prefs
    var cfg by remember { mutableStateOf(PortadaCfg.fromJson(p.str("portada", ""))) }
    var editar by remember { mutableStateOf(false) }
    fun guardar(c: PortadaCfg) { cfg = c; p.set("portada", c.toJson()) }

    val t = r.actual
    val ultima = remember(r.activa, t?.id) { r.ultima() }
    val pistaDisco: Track? = t ?: ultima?.let { u -> RadioApp.biblioteca.porId(u.first) }
    val imgLocal = cargarImagen(if (r.fuenteSpotify) null else pistaDisco?.let { RadioApp.biblioteca.caratula(it) }, 600)
    val imgSpotify = cargarImagenUrl(if (r.fuenteSpotify) r.portadaUrl else "")
    val portadaImg = if (r.fuenteSpotify) imgSpotify else imgLocal
    val tinte = if (cfg.tinte && portadaImg != null) remember(portadaImg) { Imagenes.colorPromedio(portadaImg.asAndroidBitmap()) } else Color.Transparent

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (cfg.visible("hello")) Saludo(acc)
        if (cfg.visible("place")) Zona(acc)
        if (cfg.visible("disc")) Disco(portadaImg, cfg.disco == "cuadrada", r.sonando, tinte, if (t == null && !r.fuenteSpotify) ultima?.let { it.second to it.third } else null) {
            val u = ultima ?: return@Disco
            if (RadioApp.biblioteca.porId(u.first) != null) r.iniciar(desdeId = u.first) else r.iniciar()
        }
        AhoraSuena(t, !cfg.visible("disc"))
        if (r.activa) Controles()
        r.siguiente?.let { s -> if (r.activa) Text("Sigue: ${s.name} — ${s.artist}", color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 10.dp)) }
        InterruptorAire()
        if (cfg.visible("modes")) ModosChips()
        TarjetaLuna()
        if (r.mensaje.isNotEmpty()) Text(r.mensaje, color = LocalExtra.current.tenue, modifier = Modifier.padding(vertical = 8.dp))
        BotonesPrincipales(acc)
        Spacer(Modifier.height(18.dp))
        cfg.orden.filter { cfg.visible(it) }.forEach { k ->
            when (k) {
                "foryou" -> ParaTi(acc)
                "day" -> HoyEnLaMusica(acc)
                "week" -> TuMes(acc)
                "club" -> ClubSemana(acc)
            }
        }
        OutlinedButton(onClick = { editar = true }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 24.dp)) {
            Icon(Icons.Filled.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Personalizar portada")
        }
    }
    if (editar) EditarPortada(cfg, { guardar(it) }, { editar = false })
}

@Composable
private fun Saludo(acc: AccionesPortada) {
    val r = RadioApp.radio; val p = RadioApp.prefs
    val ahora = LocalDateTime.now()
    val dia = ahora.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es")))
    val wx = listOf(r.entorno.clima, r.entorno.zona).filter { it.isNotBlank() }.joinToString(" · ")
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 14.dp)) {
        Column(Modifier.weight(1f)) {
            Etiqueta(dia)
            Text(saludo(ahora.hour, p.str("me", "")), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 2.dp))
            Text(wx.ifEmpty { if (r.entorno.permitido()) "Buscando el clima…" else "Activa la ubicación para ver el clima." },
                color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = acc.compartir, modifier = Modifier.clip(CircleShape).background(LocalExtra.current.tarjeta.copy(alpha = 0.6f))) { Icon(Icons.Filled.Share, "Compartir la app") }
    }
}

@Composable
private fun Zona(acc: AccionesPortada) {
    val e = RadioApp.radio.entorno
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xCC1B4F9C)).border(3.dp, Color(0x66F4F6F8), RoundedCornerShape(14.dp)).padding(14.dp)) {
        Text("Cerca de", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        Text(e.zona.ifEmpty { "Esperando ubicación" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 33.sp)
        if (!e.permitido()) TextButton(onClick = acc.ubicacion, contentPadding = ButtonDefaults.TextButtonContentPadding) { Text("Activar ubicación", color = Amarillo) }
    }
}

@Composable
private fun Disco(img: androidx.compose.ui.graphics.ImageBitmap?, cuadrada: Boolean, sonando: Boolean, tinte: Color, ultima: Pair<String, String>?, seguir: () -> Unit) {
    val giro = remember { Animatable(0f) }
    LaunchedEffect(sonando, cuadrada) {
        if (sonando && !cuadrada) while (isActive) { giro.animateTo(giro.value + 360f, tween(6000, easing = LinearEasing)); giro.snapTo(giro.value % 360f) }
    }
    Column(
        Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.radialGradient(listOf(tinte.copy(alpha = if (tinte == Color.Transparent) 0f else 0.55f), Color.Transparent))).padding(top = 20.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val forma = if (cuadrada) RoundedCornerShape(22.dp) else CircleShape
        Box(Modifier.width(240.dp).aspectRatio(1f).graphicsLayer { rotationZ = if (cuadrada) 0f else giro.value }.clip(forma)
            .background(if (cuadrada) Color(0xFF1F2228) else Color(0xFF15171B)), contentAlignment = Alignment.Center) {
            if (!cuadrada) for (i in 1..9) Box(Modifier.fillMaxSize(1f - i * 0.035f).clip(CircleShape).border(1.dp, Color(0x14FFFFFF), CircleShape))
            val interior = if (cuadrada) Modifier.fillMaxSize() else Modifier.fillMaxSize(0.64f).clip(CircleShape)
            if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = interior)
            else Box(interior.background(Brush.linearGradient(listOf(Amarillo, Naranja, Rosa))), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MusicNote, null, tint = Tinta, modifier = Modifier.fillMaxSize(0.4f))
            }
            if (!cuadrada) Box(Modifier.size(16.dp).clip(CircleShape).background(Color(0xFF0F1013)).border(3.dp, Color(0x26FFFFFF), CircleShape))
        }
        if (ultima != null) {
            Spacer(Modifier.height(12.dp)); Etiqueta("Lo último que sonó")
            Text(ultima.first, style = MaterialTheme.typography.titleMedium); Text(ultima.second, color = LocalExtra.current.tenue)
            Button(onClick = seguir, modifier = Modifier.padding(top = 8.dp)) { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Seguir escuchando") }
        }
    }
}

@Composable
private fun AhoraSuena(t: Track?, conCaratula: Boolean) {
    val r = RadioApp.radio
    var fav by remember(t?.id) { mutableStateOf(t?.let { RadioApp.stats.isFav(it.name, it.artist) } ?: false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Box(Modifier.width(5.dp).height(56.dp).background(Amarillo)); Spacer(Modifier.width(12.dp))
        if (conCaratula && t != null && !r.fuenteSpotify) { Caratula(RadioApp.biblioteca.caratula(t), t.album + t.artist, 64.dp, 8.dp); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(t?.name ?: "Nada sonando", style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(t?.let { listOf(it.artist, it.album).filter { s -> s.isNotBlank() }.joinToString(" · ") } ?: if (r.fuenteSpotify) "Toca «Iniciar radio» para escuchar Spotify" else "Toca «Iniciar radio» para escuchar tu biblioteca",
                color = Amarillo, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (t != null) IconButton(onClick = { r.alternarFavorita()?.let { fav = it } }) {
            Icon(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (fav) "Quitar de favoritas" else "Marcar como favorita", tint = if (fav) Rosa else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun Controles() {
    val r = RadioApp.radio
    var arrastre by remember { mutableFloatStateOf(-1f) }
    var menuTimer by remember { mutableStateOf(false) }
    val dur = r.durMs.coerceAtLeast(1L)
    Column(Modifier.padding(bottom = 8.dp)) {
        Slider(value = if (arrastre >= 0) arrastre else (r.posMs.toFloat() / dur).coerceIn(0f, 1f), onValueChange = { arrastre = it },
            onValueChangeFinished = { r.irA((arrastre * dur).toLong()); arrastre = -1f })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmtDur(r.posMs), style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.tenue)
            Text(fmtDur(r.durMs), style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.tenue)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { r.ponerMezclar(!r.mezclar) }) { Icon(Icons.Filled.Shuffle, "Orden aleatorio", tint = if (r.mezclar) Amarillo else LocalExtra.current.tenue) }
            IconButton(onClick = { r.anteriorCancion() }) { Icon(Icons.Filled.SkipPrevious, "Anterior", Modifier.size(34.dp)) }
            Box(Modifier.size(68.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Amarillo, Naranja))).clickable { r.alternarPausa() }, contentAlignment = Alignment.Center) {
                Icon(if (r.sonando || r.lunaHablando) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (r.sonando) "Pausa" else "Seguir", tint = Tinta, modifier = Modifier.size(38.dp))
            }
            IconButton(onClick = { r.siguienteCancion() }) { Icon(Icons.Filled.SkipNext, "Siguiente", Modifier.size(34.dp)) }
            IconButton(onClick = { r.ciclarRepetir() }) {
                Icon(if (r.repetir == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat, "Repetir", tint = if (r.repetir > 0) Amarillo else LocalExtra.current.tenue)
            }
            Box {
                IconButton(onClick = { menuTimer = true }) { Icon(Icons.Filled.Bedtime, "Temporizador de apagado", tint = if (r.apagarEn > 0) Amarillo else LocalExtra.current.tenue) }
                DropdownMenu(expanded = menuTimer, onDismissRequest = { menuTimer = false }) {
                    listOf(15, 30, 45, 60, 90).forEach { m -> DropdownMenuItem(text = { Text("Apagar en $m minutos") }, onClick = { r.temporizador(m); menuTimer = false }) }
                    if (r.apagarEn > 0) DropdownMenuItem(text = { Text("Quitar temporizador") }, onClick = { r.temporizador(0); menuTimer = false })
                }
            }
        }
    }
}

@Composable
private fun InterruptorAire() {
    val r = RadioApp.radio
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(999.dp)).background(LocalExtra.current.tarjeta.copy(alpha = 0.6f)).border(1.dp, LocalExtra.current.borde, RoundedCornerShape(999.dp)).padding(5.dp)) {
        listOf(true to "Con radio", false to "Solo música").forEach { (v, n) ->
            val on = r.aire == v
            Box(Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(if (on) Amarillo.copy(alpha = 0.22f) else Color.Transparent)
                .border(if (on) 1.5.dp else 0.dp, if (on) Amarillo else Color.Transparent, RoundedCornerShape(999.dp)).clickable { r.ponerAire(v) }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (v) Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFE5604D))) else Icon(Icons.Filled.MusicNote, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp)); Text(n, color = if (on) Amarillo else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ModosChips() {
    val r = RadioApp.radio
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
        items(Modos.NOMBRES) { m ->
            FilterChip(selected = r.modo == m, onClick = { r.ponerModo(m) }, label = { Text(Modos.ETIQUETA[m] ?: m) },
                leadingIcon = { Icon(iconoModo(m), null, Modifier.size(18.dp)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Amarillo, selectedLabelColor = Tinta, selectedLeadingIconColor = Tinta))
        }
    }
}

@Composable
private fun TarjetaLuna() {
    val r = RadioApp.radio; val host = RadioApp.prefs.str("host", "Luna")
    val pulso = rememberInfiniteTransition(label = "luna")
    val escala by pulso.animateFloat(1f, 1.12f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "escala")
    Tarjeta(Modifier.padding(bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(52.dp).graphicsLayer { val s = if (r.lunaHablando) escala else 1f; scaleX = s; scaleY = s }.clip(CircleShape)
                .background(Brush.linearGradient(listOf(Amarillo, Naranja, Rosa))), contentAlignment = Alignment.Center) {
                Text(inicial(host), color = Tinta, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(host, color = Amarillo, fontWeight = FontWeight.Bold)
                Text(r.textoLuna.ifEmpty { "Aquí aparecerá lo que diga entre canciones." }, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun BotonesPrincipales(acc: AccionesPortada) {
    val r = RadioApp.radio
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.padding(top = 8.dp)) {
        if (!r.activa) Button(onClick = { acc.notificaciones(); r.iniciar() }, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Iniciar radio", fontSize = 19.sp)
        } else OutlinedButton(onClick = { r.detener() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Filled.Stop, null); Spacer(Modifier.width(8.dp)); Text("Detener radio")
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = acc.carro, modifier = Modifier.weight(1f).height(52.dp)) {
                Icon(Icons.Filled.DirectionsCar, null); Spacer(Modifier.width(8.dp)); Text("Modo carro")
            }
            Spacer(Modifier.width(10.dp))
            Box {
                FilledTonalButton(onClick = { menu = true }, modifier = Modifier.height(52.dp)) { Icon(Icons.Filled.MoreHoriz, "Más herramientas") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Probar voz") }, onClick = { menu = false; acc.probarVoz() })
                    DropdownMenuItem(text = { Text("Probar intervención") }, onClick = { menu = false; acc.probarLuna() })
                    DropdownMenuItem(text = { Text("Compartir") }, onClick = { menu = false; acc.compartir() })
                }
            }
        }
    }
}

/* ---------- Tarjetas ---------- */

private class Mosaico(val tipo: String, val titulo: String, val sub: String, val pista: Track?, val icono: ImageVector, val valor: String = "")

@Composable
private fun ParaTi(acc: AccionesPortada) {
    val lib = RadioApp.biblioteca.pistas; val r = RadioApp.radio
    Tarjeta(Modifier.padding(bottom = 14.dp)) {
        Titulo("Para ti")
        if (lib.isEmpty()) {
            Text("Agrega tu música y aquí aparecerán tus artistas, tus favoritas y lo que hace rato no suena.", color = LocalExtra.current.tenue)
            OutlinedButton(onClick = acc.biblioteca, modifier = Modifier.padding(top = 10.dp)) { Text("Agregar canciones") }
            return@Tarjeta
        }
        val st = RadioApp.stats
        val mosaicos = remember(lib, st.stats.size, st.favs.size) {
            val conteo = HashMap<String, Int>(); st.stats.values.forEach { conteo[it.a] = (conteo[it.a] ?: 0) + it.c }
            val l = mutableListOf<Mosaico>()
            conteo.entries.sortedByDescending { it.value }.map { it.key }.filter { a -> lib.any { normNombre(it.artist) == normNombre(a) } }.take(4)
                .forEach { a -> l += Mosaico("art", a, "Tu artista", lib.firstOrNull { normNombre(it.artist) == normNombre(a) && it.cover.isNotEmpty() } ?: lib.first { normNombre(it.artist) == normNombre(a) }, Icons.Filled.MusicNote, a) }
            val fv = lib.filter { st.isFav(it.name, it.artist) }
            if (fv.isNotEmpty()) l += Mosaico("fav", "Tus favoritas", "${fv.size} ${if (fv.size == 1) "canción" else "canciones"}", fv.firstOrNull { it.cover.isNotEmpty() }, Icons.Filled.Favorite)
            val viejas = lib.filter { !st.stats.containsKey(it.key) }
            if (viejas.isNotEmpty()) l += Mosaico("old", "Hace rato no suenan", "${viejas.size} ${if (viejas.size == 1) "canción" else "canciones"}", viejas.firstOrNull { it.cover.isNotEmpty() }, Icons.Filled.AccessTime)
            l += Mosaico("all", "Mezcla de todo", "${lib.size} canciones", null, Icons.Filled.Shuffle)
            l
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(mosaicos, key = { it.tipo + it.titulo }) { m ->
                Column(Modifier.width(132.dp).clickable {
                    when (m.tipo) {
                        "art" -> r.iniciar({ normNombre(it.artist) == normNombre(m.valor) })
                        "fav" -> r.iniciar({ st.isFav(it.name, it.artist) })
                        "old" -> r.iniciar({ !st.stats.containsKey(it.key) })
                        else -> r.iniciar(null, mezclarEsta = true)
                    }
                }) {
                    val f = m.pista?.let { RadioApp.biblioteca.caratula(it) }
                    if (f != null) Caratula(f, m.titulo, 132.dp, 16.dp) else Relleno(m.titulo, m.icono, Modifier.size(132.dp), RoundedCornerShape(16.dp))
                    Text(m.titulo, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                    Text(m.sub, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue)
                }
            }
        }
    }
}

@Composable
private fun HoyEnLaMusica(acc: AccionesPortada) {
    val propia = remember { Efemerides.propias(RadioApp.prefs.str("efemerides", ""), LocalDate.now()) }
    val hoy by produceState<List<Pair<String, String>>>(emptyList()) { value = Revista.hoyEnLaMusica() }
    val conc by produceState<Concierto?>(null) { value = Revista.conciertoDelDia() }
    if (propia.isEmpty() && hoy.isEmpty() && conc == null) return
    Tarjeta(Modifier.padding(bottom = 14.dp)) {
        Titulo("Hoy en la música")
        if (propia.isNotEmpty()) Text(propia, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        hoy.firstOrNull()?.let { (y, t) ->
            Row(Modifier.padding(bottom = 12.dp)) { Text(y, color = Amarillo, fontWeight = FontWeight.Bold); Spacer(Modifier.width(6.dp)); Text(t) }
        }
        conc?.let { c ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x22000000)).clickable { acc.revista("#rvb=${c.id}") }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val img = cargarImagenUrl(c.miniatura())
                if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.width(112.dp).height(70.dp).clip(RoundedCornerShape(10.dp)))
                else Relleno(c.titulo, Icons.Filled.Movie, Modifier.width(112.dp).height(70.dp), RoundedCornerShape(10.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Concierto del día · La bóveda del tiempo", color = Amarillo, style = MaterialTheme.typography.labelSmall)
                    Text(c.titulo, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOf(c.artista, if (c.anio > 0) c.anio.toString() else "").filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue)
                }
            }
        }
    }
}

@Composable
private fun TuMes(acc: AccionesPortada) {
    val res = remember(RadioApp.stats.stats.values.sumOf { it.c }) { RadioApp.stats.resumen("month", LocalDate.now()) }
    Tarjeta(Modifier.padding(bottom = 14.dp)) {
        Titulo("Tu mes en la radio")
        if (res == null) Text("Cada canción cuenta después de 30 segundos sonando. Vuelve en un rato para ver tu resumen.", color = LocalExtra.current.tenue)
        else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(res.c.toString() to "canciones", "~${res.minutos}" to "minutos", res.days.toString() to "días").forEach { (n, l) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0x22000000)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(n, style = MaterialTheme.typography.headlineMedium); Text(l, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue)
                    }
                }
            }
            res.topArtistas(1).firstOrNull()?.let { (a, _) ->
                val so = res.topCanciones(1).firstOrNull()?.first?.first
                Text("Tu artista: $a" + (so?.let { " · tu canción: $it" } ?: ""), color = LocalExtra.current.tenue, modifier = Modifier.padding(top = 10.dp))
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (res != null) OutlinedButton(onClick = acc.resumen, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Tu resumen") }
            OutlinedButton(onClick = acc.mapa, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Map, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Tus viajes") }
        }
    }
}

@Composable
private fun ClubSemana(acc: AccionesPortada) {
    val club by produceState<Revista.Club?>(null) { value = Revista.clubDeLaSemana() }
    val c = club ?: return
    val lib = RadioApp.biblioteca.pistas
    val tiene = lib.any { normNombre(it.artist) == normNombre(c.artista) && normNombre(it.album) == normNombre(c.album) }
    Tarjeta(Modifier.padding(bottom = 14.dp)) {
        Titulo("Club de escucha")
        Row(verticalAlignment = Alignment.CenterVertically) {
            val img = cargarImagenUrl(c.portada)
            if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.size(74.dp).clip(RoundedCornerShape(12.dp)))
            else Relleno(c.album, Icons.Filled.MusicNote, Modifier.size(74.dp), RoundedCornerShape(12.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("DISCO DE LA SEMANA", color = Amarillo, style = MaterialTheme.typography.labelSmall)
                Text(c.album, style = MaterialTheme.typography.titleMedium)
                Text(listOf(c.artista, if (c.anio > 0) c.anio.toString() else "").filter { it.isNotBlank() }.joinToString(" · "), color = LocalExtra.current.tenue)
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tiene) OutlinedButton(onClick = { RadioApp.radio.iniciar({ normNombre(it.artist) == normNombre(c.artista) && normNombre(it.album) == normNombre(c.album) }, mezclarEsta = false) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Escuchar")
            }
            OutlinedButton(onClick = { acc.revista("#revista") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Comentar")
            }
        }
    }
}

@Composable
private fun EditarPortada(cfg: PortadaCfg, cambiar: (PortadaCfg) -> Unit, cerrar: () -> Unit) {
    val p = RadioApp.prefs
    var nombre by remember { mutableStateOf(p.str("me", "")) }
    Dialog(onDismissRequest = cerrar) {
        Column(Modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("Personalizar portada", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp)); Etiqueta("Arriba")
            PortadaCfg.ARRIBA.forEach { (k, n) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { cambiar(cfg.alternar(k, !cfg.visible(k))) }) {
                    Checkbox(checked = cfg.visible(k), onCheckedChange = { cambiar(cfg.alternar(k, it)) }); Text(n)
                }
            }
            Spacer(Modifier.height(8.dp)); Etiqueta("Tarjetas (en este orden)")
            val nombres = PortadaCfg.TARJETAS.toMap()
            cfg.orden.forEachIndexed { i, k ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = cfg.visible(k), onCheckedChange = { cambiar(cfg.alternar(k, it)) })
                    Text(nombres[k] ?: k, Modifier.weight(1f))
                    IconButton(onClick = { cambiar(cfg.mover(k, -1)) }, enabled = i > 0) { Text("↑", fontSize = 20.sp) }
                    IconButton(onClick = { cambiar(cfg.mover(k, 1)) }, enabled = i < cfg.orden.size - 1) { Text("↓", fontSize = 20.sp) }
                }
            }
            Spacer(Modifier.height(8.dp)); Etiqueta("Estilo")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                listOf("vinilo" to "Vinilo", "cuadrada" to "Cuadrada").forEach { (k, n) ->
                    FilterChip(selected = cfg.disco == k, onClick = { cambiar(cfg.copy(disco = k)) }, label = { Text(n) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Amarillo, selectedLabelColor = Tinta))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { cambiar(cfg.copy(tinte = !cfg.tinte)) }) {
                Checkbox(checked = cfg.tinte, onCheckedChange = { cambiar(cfg.copy(tinte = it)) }); Text("Color de fondo según la carátula")
            }
            OutlinedTextField(value = nombre, onValueChange = { nombre = it.take(40); p.set("me", nombre.trim()) }, label = { Text("Cómo te saludo") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { cambiar(PortadaCfg()) }, modifier = Modifier.weight(1f)) { Text("Restablecer") }
                Button(onClick = cerrar, modifier = Modifier.weight(1f)) { Text("Listo") }
            }
        }
    }
}
