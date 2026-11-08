package rr.revista

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Amarillo = Color(0xFFF2C230)
val Asfalto = Color(0xFF14161A)
val Panel = Color(0xFF22262D)
val TextoClaro = Color(0xFFE6E9ED)
val Tenue = Color(0xFF8D96A3)

private fun tipografia(f: FontFamily): Typography {
    val t = Typography()
    fun TextStyle.c() = copy(fontFamily = f)
    return t.copy(displayLarge = t.displayLarge.c(), displayMedium = t.displayMedium.c(), displaySmall = t.displaySmall.c(), headlineLarge = t.headlineLarge.c(),
        headlineMedium = t.headlineMedium.c(), headlineSmall = t.headlineSmall.c(), titleLarge = t.titleLarge.c(), titleMedium = t.titleMedium.c(), titleSmall = t.titleSmall.c(),
        bodyLarge = t.bodyLarge.c(), bodyMedium = t.bodyMedium.c(), bodySmall = t.bodySmall.c(), labelLarge = t.labelLarge.c(), labelMedium = t.labelMedium.c(), labelSmall = t.labelSmall.c())
}

/** Pantallas: la sección de abajo y, encima, el artículo abierto. */
@Composable
fun App() {
    LaunchedEffect(Unit) { Fuentes.cargar(); listo() }
    val h = remember { hashInicial() }
    var seccion by remember { mutableStateOf(when { h.startsWith("#posters") -> "posters"; h.startsWith("#boveda") -> "boveda"; else -> "inicio" }) }
    var leyendo by remember { mutableStateOf(Regex("^#post=([0-9a-f-]{36})$").find(h)?.groupValues?.get(1)) }
    val listaInicio = rememberLazyListState()
    if (!Fuentes.cargadas) return
    val base = Fuentes.de("barlow")
    MaterialTheme(colorScheme = darkColorScheme(primary = Amarillo, onPrimary = Color(0xFF1B1E23), background = Asfalto, surface = Panel, onBackground = TextoClaro, onSurface = TextoClaro), typography = tipografia(base)) {
        CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = base, color = TextoClaro, fontSize = 16.sp)) {
            Box(Modifier.fillMaxSize().background(Asfalto), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        when (seccion) {
                            "posters" -> Posters()
                            "boveda" -> Boveda()
                            "mas" -> Mas()
                            else -> Inicio(listaInicio) { leyendo = it }
                        }
                        leyendo?.let { id -> Lectura(id) { leyendo = null } }
                    }
                    if (leyendo == null) BarraAbajo(seccion) { seccion = it }
                }
            }
        }
    }
}

@Composable
private fun BarraAbajo(actual: String, ir: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xF21B1E23)).padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceAround) {
        listOf(Triple("inicio", "casa", "Inicio"), Triple("boveda", "pelicula", "Bóveda"), Triple("posters", "imagen", "Pósters"), Triple("mas", "menu", "Más")).forEach { (k, ic, n) ->
            val on = actual == k
            Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable { ir(k) }.background(if (on) Amarillo.copy(alpha = .16f) else Color.Transparent).padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icono(ic, if (on) Amarillo else Tenue, 24.dp)
                Text(n, fontSize = 12.sp, color = if (on) Amarillo else Tenue, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun Cabecera(titulo: String, sub: String) {
    Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Amarillo), contentAlignment = Alignment.Center) { Icono("nota", Color(0xFF1B1E23), 24.dp) }
            Spacer(Modifier.width(10.dp))
            Text(titulo, fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 36.sp)
        }
        Text(sub, color = Tenue, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Cargando(texto: String = "Cargando…") {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Amarillo); Text(texto, color = Tenue, modifier = Modifier.padding(top = 12.dp))
    }
}

/* ---------------- Inicio ---------------- */
@Composable
private fun Inicio(lista: androidx.compose.foundation.lazy.LazyListState, abrir: (String) -> Unit) {
    var filtro by remember { mutableStateOf("todo") }
    val datos by produceState<Result<List<Articulo>>?>(null) { value = runCatching { Supa.portada() } }
    LazyColumn(Modifier.fillMaxSize(), state = lista) {
        item { Cabecera("Revista", "Artículos, reseñas e historias de la comunidad de Radio de ruta") }
        item {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("todo" to "Lo último", "resenas" to "Reseñas de discos").forEach { (k, n) ->
                    val on = filtro == k
                    Text(n, color = if (on) Color(0xFF1B1E23) else TextoClaro, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) Amarillo else Color.Transparent).border(1.dp, if (on) Amarillo else Color(0x33FFFFFF), RoundedCornerShape(999.dp))
                            .clickable { filtro = k }.padding(horizontal = 16.dp, vertical = 9.dp))
                }
            }
        }
        val r = datos
        when {
            r == null -> item { Cargando("Cargando artículos…") }
            r.isFailure -> item { Text("No se pudo cargar la Revista: ${r.exceptionOrNull()?.message ?: ""}", color = Tenue, modifier = Modifier.padding(18.dp)) }
            else -> {
                val l = r.getOrThrow().filter { filtro == "todo" || it.resena != null }
                if (l.isEmpty()) item { Text("Todavía no hay artículos aquí.", color = Tenue, modifier = Modifier.padding(18.dp)) }
                l.firstOrNull()?.let { a -> item(key = "hero") { Destacado(a) { abrir(a.id) } } }
                items(l.drop(1), key = { it.id }) { a -> Tarjeta(a) { abrir(a.id) } }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Avatar(a: Autor, tam: Int) {
    val img = imagen(a.avatar)
    Box(Modifier.size(tam.dp).clip(CircleShape).background(Amarillo), contentAlignment = Alignment.Center) {
        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(a.nombre.take(1).uppercase(), color = Color(0xFF1B1E23), fontWeight = FontWeight.Bold, fontSize = (tam * .45f).sp)
    }
}

@Composable
private fun Contadores(a: Articulo, color: Color = Tenue) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icono("corazon", color, 16.dp); Text("${a.likes}", color = color, fontSize = 14.sp); Spacer(Modifier.width(8.dp))
        Icono("globo", color, 16.dp); Text("${a.comentarios}", color = color, fontSize = 14.sp)
    }
}

private val NOMBRE_TEMA = mapOf("asfalto" to "Asfalto", "senal" to "Señal", "papel" to "Papel", "neon" to "Neón", "atardecer" to "Atardecer", "vinilo" to "Vinilo",
    "y2k" to "Pop Y2K", "punk" to "Fanzine punk", "clasica" to "Rock clásico", "vintage" to "Cartel vintage", "teen" to "Teen 2002", "tokio" to "Tokio kawaii", "tabloide" to "Tabloide", "cromo" to "Cromo 2000")

@Composable
private fun Destacado(a: Articulo, abrir: () -> Unit) {
    val img = imagen(a.portada)
    val ac = col(Estilo.ACENTOS[a.estilo.ac] ?: 0xFFF2C230)
    Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp).fillMaxWidth().aspectRatio(.82f).clip(RoundedCornerShape(24.dp)).background(Panel).clickable(onClick = abrir)) {
        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color(0xF20B0C0F))))
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Text("DESTACADO · ${NOMBRE_TEMA[a.estilo.th]?.uppercase() ?: ""}", color = ac, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(a.titulo, color = Color.White, fontFamily = Fuentes.titulo(a.estilo.ft), fontSize = 34.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            if (a.subtitulo.isNotBlank()) Text(a.subtitulo, color = Color.White.copy(alpha = .8f), fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
                Avatar(a.autor, 30); Spacer(Modifier.width(8.dp))
                Text("${a.autor.nombre} · ${Fechas.corta(a.fecha)}", color = Color.White.copy(alpha = .85f), fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Contadores(a, Color.White.copy(alpha = .85f))
            }
        }
    }
}

@Composable
private fun Tarjeta(a: Articulo, abrir: () -> Unit) {
    val img = imagen(Supa.miniatura(a.portada), a.portada)
    val t = Temas.de(a.estilo)
    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).clickable(onClick = abrir)) {
        if (a.portada.isNotEmpty()) Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF2A2E36))) {
            if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            a.resena?.let { rv -> Text("${rv.nota}/10", color = Color(0xFF1B1E23), fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(10.dp)).background(Amarillo).padding(horizontal = 10.dp, vertical = 4.dp)) }
        }
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Brush.linearGradient(t.fondo.map(::col).let { if (it.size == 1) it + it else it })))
                Spacer(Modifier.width(6.dp)); Text(NOMBRE_TEMA[a.estilo.th] ?: "", color = Tenue, fontSize = 12.sp)
            }
            Text(a.titulo, fontFamily = Fuentes.titulo(a.estilo.ft), fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 25.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            if (a.subtitulo.isNotBlank()) Text(a.subtitulo, color = Tenue, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Avatar(a.autor, 26); Spacer(Modifier.width(8.dp))
                Text("${a.autor.nombre} · ${Fechas.corta(a.fecha)}", color = Tenue, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Contadores(a)
            }
        }
    }
}

/* ---------------- Pósters ---------------- */
@Composable
private fun Posters() {
    val datos by produceState<Result<List<Poster>>?>(null) { value = runCatching { Supa.posters() } }
    var grande by remember { mutableStateOf<Poster?>(null) }
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) { Cabecera("Pósters", "Carteles de conciertos y giras, de la comunidad") }
            val r = datos
            if (r == null) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) { Cargando("Cargando pósters…") }
            else if (r.isFailure) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) { Text("No se pudieron cargar los pósters.", color = Tenue, modifier = Modifier.padding(8.dp)) }
            else items(r.getOrThrow(), key = { it.id }) { p ->
                Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable { grande = p }) {
                    val img = imagen(p.mini, p.grande)
                    Box(Modifier.fillMaxWidth().aspectRatio(.7f).clip(RoundedCornerShape(14.dp)).background(Panel)) { if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    Text(p.titulo, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                    Text(listOf(p.artista, if (p.anio > 0) p.anio.toString() else "").filter { it.isNotBlank() }.joinToString(" · "), color = Tenue, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
        grande?.let { p ->
            Box(Modifier.fillMaxSize().background(Color(0xF2000000)).clickable { grande = null }, contentAlignment = Alignment.Center) {
                val img = imagen(p.grande, p.mini)
                if (img != null) Image(img, p.titulo, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(16.dp))
                Box(Modifier.align(Alignment.TopEnd).padding(14.dp).size(44.dp).clip(CircleShape).background(Color(0x66FFFFFF)), contentAlignment = Alignment.Center) { Icono("cerrar", Color.White) }
            }
        }
    }
}

/* ---------------- Bóveda ---------------- */
@Composable
private fun Boveda() {
    val datos by produceState<Result<List<Concierto>>?>(null) { value = runCatching { Supa.boveda() } }
    LazyColumn(Modifier.fillMaxSize()) {
        item { Cabecera("La bóveda del tiempo", "Conciertos legendarios, por tiempo limitado") }
        val r = datos
        when {
            r == null -> item { Cargando("Abriendo la bóveda…") }
            r.isFailure -> item { Text("No se pudo abrir la bóveda.", color = Tenue, modifier = Modifier.padding(18.dp)) }
            r.getOrThrow().isEmpty() -> item { Text("Hoy no hay conciertos en cartelera.", color = Tenue, modifier = Modifier.padding(18.dp)) }
            else -> items(r.getOrThrow(), key = { it.id }) { v ->
                Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).clickable { abrirEnlace(Supa.WEB + "#rvb=" + v.id) }) {
                    val img = imagen(v.miniatura)
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF2A2E36))) {
                        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.align(Alignment.Center).size(60.dp).clip(CircleShape).background(Color(0xAA000000)), contentAlignment = Alignment.Center) { Icono("play", Color.White, 30.dp) }
                    }
                    Column(Modifier.padding(16.dp)) {
                        Text(v.titulo, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 23.sp)
                        Text(listOf(v.artista, v.festival, if (v.anio > 0) v.anio.toString() else "").filter { it.isNotBlank() }.joinToString(" · "), color = Tenue, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/* ---------------- Más ---------------- */
@Composable
private fun Mas() {
    Column(Modifier.fillMaxSize().padding(bottom = 20.dp)) {
        Cabecera("Más", "Versión nativa de la Revista (prototipo)")
        Column(Modifier.padding(18.dp).clip(RoundedCornerShape(20.dp)).background(Panel).padding(18.dp)) {
            Text("Esta es la Revista hecha en Kotlin: el mismo código sirve para la app de Android y para el navegador.", fontSize = 16.sp, lineHeight = 23.sp)
            Text("Por ahora es solo para leer. Escribir, comentar, dar me gusta, subir pósters y el panel de admin siguen en la Revista web.", color = Tenue, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 10.dp))
            Text("Abrir la Revista web", color = Color(0xFF1B1E23), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp).clip(RoundedCornerShape(12.dp)).background(Amarillo).clickable { abrirEnlace(Supa.WEB + "#revista") }.padding(horizontal = 18.dp, vertical = 12.dp))
        }
    }
}
