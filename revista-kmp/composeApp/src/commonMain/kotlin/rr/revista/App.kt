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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    var seccion by remember { mutableStateOf(when { h.startsWith("#posters") -> "posters"; h.startsWith("#boveda") -> "boveda"; h.startsWith("#artistas") -> "artistas"; h.startsWith("#mas") -> "mas"; else -> "inicio" }) }
    LaunchedEffect(Unit) {
        (Regex("^#post=([0-9a-f-]{36})$").find(h)?.groupValues?.get(1) ?: Regex("^#ejemplo=(\\d)$").find(h)?.groupValues?.get(1)?.let { "ejemplo-$it" })?.let { Nav.ir(Destino.Leer(it)) }
        Regex("^#perfil=([0-9a-f-]{36})$").find(h)?.groupValues?.get(1)?.let { Nav.ir(Destino.Perfil(it)) }
        if (h.startsWith("#escribir")) Nav.ir(Destino.Editar(null))
        if (h == "#buscar") Nav.ir(Destino.Buscar)
        Regex("^#artista=([0-9a-f-]{36})$").find(h)?.groupValues?.get(1)?.let { Nav.ir(Destino.Artista(it)) }
        Regex("^#lista=([0-9a-f-]{36})$").find(h)?.groupValues?.get(1)?.let { Nav.ir(Destino.Lista(it)) }
        if (h == "#entrar") Nav.dialogo = "cuenta"
        Cuenta.social()
    }
    val listaInicio = rememberLazyGridState()
    val foco = remember { FocusRequester() }
    // el foco (para la tecla Esc) se pide cuando la pantalla ya existe, no mientras cargan las letras
    LaunchedEffect(Fuentes.cargadas) { if (Fuentes.cargadas) { kotlinx.coroutines.delay(50); runCatching { foco.requestFocus() } } }
    val arriba = Nav.pila.lastOrNull()
    if (!Fuentes.cargadas) { Box(Modifier.fillMaxSize().background(Asfalto), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }; return }
    val base = Fuentes.de("barlow")
    MaterialTheme(colorScheme = darkColorScheme(primary = Amarillo, onPrimary = Color(0xFF1B1E23), background = Asfalto, surface = Panel, onBackground = TextoClaro, onSurface = TextoClaro), typography = tipografia(base)) {
        CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = base, color = TextoClaro, fontSize = 16.sp)) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Asfalto).focusRequester(foco).focusable().onPreviewKeyEvent { teclaEscape(it) }) {
                val ancho = maxWidth
                val ancha = ancho.esAncha
                CompositionLocalProvider(LocalAncho provides ancho) {
                Row(Modifier.fillMaxSize()) {
                    if (ancha) BarraLateral(seccion) { Nav.inicio(); seccion = it }
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    Column(Modifier.widthIn(max = if (ancha) 1320.dp else 760.dp).fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        when (seccion) {
                            "posters" -> GaleriaPantalla()
                            "artistas" -> ArtistasPantalla()
                            "boveda" -> Boveda()
                            "mas" -> Mas()
                            else -> Inicio(listaInicio) { Nav.ir(Destino.Leer(it)) }
                        }
                        if (arriba == null && seccion == "inicio" && !ancha) Box(Modifier.align(Alignment.BottomEnd).padding(18.dp).size(60.dp).clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFFF8A3D), Color(0xFFFF5F9E)))).mano().clickable {
                                if (Nav.necesitaCuenta()) { if (Cuenta.suspendida) Aviso.mostrar("Tu cuenta está suspendida: puedes leer, pero no publicar.", true) else Nav.ir(Destino.Editar(null)) }
                            }, contentAlignment = Alignment.Center) { Text("+", color = Color(0xFF1B1E23), fontSize = 34.sp, fontWeight = FontWeight.Bold) }
                        // Las pantallas abiertas (artículo, perfil, chat…) van encima, centradas y con un ancho cómodo para leer
                        if (arriba != null) Box(Modifier.fillMaxSize().background(Asfalto), contentAlignment = Alignment.TopCenter) {
                            Box(Modifier.widthIn(max = 920.dp).fillMaxSize()) {
                            when (arriba) {
                                is Destino.Leer -> androidx.compose.runtime.key(arriba.id) { Lectura(arriba.id) { Nav.atras() } }
                                is Destino.Perfil -> androidx.compose.runtime.key(arriba.id) { PerfilPantalla(arriba.id) }
                                is Destino.Editar -> androidx.compose.runtime.key(arriba.id ?: "nuevo") { Editor(arriba.id) }
                                is Destino.Poster -> androidx.compose.runtime.key(arriba.id) { PosterPantalla(arriba.id) }
                                is Destino.Coleccion -> androidx.compose.runtime.key(arriba.id) { ColeccionPantalla(arriba.id) }
                                is Destino.SubirPoster -> SubirPosterPantalla(arriba.reto)
                                Destino.BuscarOriginales -> BuscarOriginalesPantalla()
                                is Destino.Artista -> androidx.compose.runtime.key(arriba.id) { ArtistaPantalla(arriba.id) }
                                is Destino.EditarArtista -> androidx.compose.runtime.key(arriba.id ?: "nuevo") { EditarArtistaPantalla(arriba.id) }
                                Destino.Notificaciones -> NotificacionesPantalla()
                                Destino.Debates -> DebatesPantalla()
                                Destino.Agenda -> AgendaPantalla()
                                Destino.Club -> ClubPantalla()
                                is Destino.Concierto -> androidx.compose.runtime.key(arriba.id) { ConciertoPantalla(arriba.id) }
                                Destino.Admin -> AdminPantalla()
                                Destino.Buscar -> BuscarPantalla()
                                Destino.EditarPerfil -> EditarPerfilPantalla()
                                is Destino.Lista -> androidx.compose.runtime.key(arriba.id) { ListaPantalla(arriba.id) }
                                is Destino.EditarLista -> androidx.compose.runtime.key(arriba.id ?: "nueva") { EditarListaPantalla(arriba.id) }
                                Destino.Chats -> ChatsPantalla()
                                is Destino.Chat -> androidx.compose.runtime.key(arriba.id) { ChatPantalla(arriba.id) }
                                Destino.Ediciones -> EdicionesPantalla()
                                is Destino.Edicion -> androidx.compose.runtime.key(arriba.id) { EdicionPantalla(arriba.id) }
                                Destino.Guardados -> GuardadosPantalla()
                                is Destino.Comparar -> androidx.compose.runtime.key(arriba.artista) { CompararPantalla(arriba.artista) }
                                is Destino.EditarConcierto -> androidx.compose.runtime.key(arriba.id ?: "nuevo") { EditarConciertoPantalla(arriba.id) }
                                null -> {}
                            }
                            }
                        }
                    }
                    if (arriba == null && !ancha) BarraAbajo(seccion) { seccion = it }
                    }
                    }
                }
                if (Nav.dialogo == "cuenta") DialogoCuenta { Nav.dialogo = null }
                EditorFotosVista()
                VisorVista()
                AvisoVista()
                }
            }
        }
    }
}

@Composable
private fun BarraAbajo(actual: String, ir: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xF21B1E23)).padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceAround) {
        SECCIONES.forEach { (k, ic, n) ->
            val on = actual == k
            Column(Modifier.clip(RoundedCornerShape(14.dp)).mano().clickable { ir(k) }.background(if (on) Amarillo.copy(alpha = .16f) else Color.Transparent).padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icono(ic, if (on) Amarillo else Tenue, 24.dp)
                Text(n, fontSize = 12.sp, color = if (on) Amarillo else Tenue, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun Cabecera(titulo: String, sub: String, lateral: androidx.compose.ui.unit.Dp = 18.dp) {
    Column(Modifier.padding(start = lateral, end = lateral, top = 18.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!LocalAncho.current.esAncha) { Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Amarillo), contentAlignment = Alignment.Center) { Icono("nota", Color(0xFF1B1E23), 24.dp) }; Spacer(Modifier.width(10.dp)) }
            Text(titulo, fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 36.sp, modifier = Modifier.weight(1f))
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { Nav.ir(Destino.Buscar) }, contentAlignment = Alignment.Center) { Icono("lupa", TextoClaro, 23.dp) }
            BotonChats(); Campana(); Spacer(Modifier.width(6.dp))
            BotonCuenta()
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
private fun Inicio(lista: LazyGridState, abrir: (String) -> Unit) {
    var filtro by remember { mutableStateOf("todo") }
    var etiqueta by remember { mutableStateOf("") }
    val datos by produceState<Result<List<Articulo>>?>(null, filtro, if (filtro == "siguiendo") Cuenta.sigo else null) {
        value = null
        value = runCatching { if (filtro == "siguiendo") (if (Cuenta.sigo.isEmpty()) emptyList() else Supa.portada(autores = Cuenta.sigo)) else Supa.portada() }
    }
    val ejemplos by produceState<List<Articulo>>(emptyList(), datos) { val d = datos; if (d != null && d.getOrNull().isNullOrEmpty()) value = Ejemplos.lista() }
    val ancha = LocalAncho.current.esAncha
    // En pantallas anchas las tarjetas se acomodan en 2, 3 o 4 columnas
    LazyVerticalGrid(GridCells.Adaptive(330.dp), Modifier.fillMaxSize(), state = lista) {
        item(span = { GridItemSpan(maxLineSpan) }) { Cabecera("Revista", "Artículos, reseñas e historias de la comunidad de Radio de ruta") }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("todo" to "Lo último", "resenas" to "Reseñas de discos") + (if (Cuenta.dentro) listOf("siguiendo" to "Siguiendo") else emptyList()) + (if (etiqueta.isNotEmpty()) listOf("tag" to "#$etiqueta  ×") else emptyList())).forEach { (k, n) ->
                    val on = filtro == k || k == "tag"
                    Text(n, color = if (on) Color(0xFF1B1E23) else TextoClaro, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) Amarillo else Color.Transparent).border(1.dp, if (on) Amarillo else Color(0x33FFFFFF), RoundedCornerShape(999.dp))
                            .mano().clickable { if (k == "tag") etiqueta = "" else filtro = k }.padding(horizontal = 16.dp, vertical = 9.dp))
                }
            }
        }
        if (filtro == "todo" && etiqueta.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }, key = "hoy") { HoyEnLaMusica() }
        if (Cuenta.dentro && filtro == "todo" && etiqueta.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }, key = "sugerencias") { Sugerencias("Personas que quizá conozcas", Modifier.padding(top = 14.dp, bottom = 6.dp)) }
        val r = datos
        when {
            r == null -> item(span = { GridItemSpan(maxLineSpan) }) { Cargando("Cargando artículos…") }
            r.isFailure -> item(span = { GridItemSpan(maxLineSpan) }) { Text("No se pudo cargar la Revista: ${r.exceptionOrNull()?.message ?: ""}", color = Tenue, modifier = Modifier.padding(18.dp)) }
            else -> {
                val l = r.getOrThrow().filter { (filtro != "resenas" || it.resena != null) && (etiqueta.isEmpty() || etiqueta in it.etiquetas) }
                if (l.isEmpty() && filtro == "siguiendo") item(span = { GridItemSpan(maxLineSpan) }) { Text(if (Cuenta.sigo.isEmpty()) "Todavía no sigues a nadie. Abre el perfil de un autor y toca «Seguir»." else "Las personas que sigues aún no han publicado.", color = Tenue, modifier = Modifier.padding(18.dp)) }
                else if (l.isEmpty() && etiqueta.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Text("No hay artículos recientes con #$etiqueta.", color = Tenue, modifier = Modifier.padding(18.dp)) }
                else if (l.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("Todavía no hay artículos publicados.", color = Tenue, modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp)) }
                    if (ejemplos.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { Text("ASÍ SE VERÁN · EJEMPLOS", color = Amarillo, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 18.dp, top = 18.dp)) }
                        ejemplos.firstOrNull()?.let { a -> item(span = { GridItemSpan(maxLineSpan) }, key = "ej-hero") { Destacado(a, ancha) { abrir(a.id) } } }
                        items(ejemplos.drop(1), key = { it.id }) { a -> Tarjeta(a) { abrir(a.id) } }
                    }
                }
                l.firstOrNull()?.let { a -> item(span = { GridItemSpan(maxLineSpan) }, key = "hero") { Destacado(a, ancha) { abrir(a.id) } } }
                items(l.drop(1), key = { it.id }) { a -> Tarjeta(a, alEtiqueta = { etiqueta = it; filtro = "todo" }) { abrir(a.id) } }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(100.dp)) }   // para que el botón «+» no tape la última tarjeta
    }
}

@Composable
fun Avatar(a: Autor, tam: Int) {
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
private fun Destacado(a: Articulo, ancha: Boolean = false, abrir: () -> Unit) {
    val img = imagen(a.portada)
    val ac = col(Estilo.ACENTOS[a.estilo.ac] ?: 0xFFF2C230)
    Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp).fillMaxWidth().aspectRatio(LocalAncho.current.let { w -> if (w >= 900.dp) 2.3f else if (w >= 600.dp) 1.45f else .82f }).clip(RoundedCornerShape(24.dp)).background(Panel).mano().clickable(onClick = abrir)) {
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
fun Tarjeta(a: Articulo, alEtiqueta: ((String) -> Unit)? = null, abrir: () -> Unit) {
    val img = imagen(Supa.miniatura(a.portada), a.portada)
    val t = Temas.de(a.estilo)
    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).mano().clickable(onClick = abrir)) {
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
            if (alEtiqueta != null && a.etiquetas.isNotEmpty()) Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                a.etiquetas.forEach { t -> Text("#$t", color = Amarillo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Amarillo.copy(alpha = .12f)).mano().clickable { alEtiqueta(t) }.padding(horizontal = 9.dp, vertical = 3.dp)) }
            }
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
        LazyVerticalGrid(GridCells.Adaptive(165.dp), Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { Cabecera("Pósters", "Carteles de conciertos y giras, de la comunidad") }
            val r = datos
            if (r == null) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { Cargando("Cargando pósters…") }
            else if (r.isFailure) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { Text("No se pudieron cargar los pósters.", color = Tenue, modifier = Modifier.padding(8.dp)) }
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
    var version by remember { mutableStateOf(0) }
    var ciclo by remember { mutableStateOf("") }
    val datos by produceState<Result<List<Concierto>>?>(null, version) { value = runCatching { Supa.boveda() } }
    Box(Modifier.fillMaxSize()) {
    LazyVerticalGrid(GridCells.Adaptive(340.dp), Modifier.fillMaxSize()) {
        item(span = { GridItemSpan(maxLineSpan) }) { Cabecera("La bóveda del tiempo", "Conciertos legendarios, por tiempo limitado") }
        val r = datos
        val ciclos = r?.getOrNull()?.map { it.ciclo }?.filter { it.isNotBlank() }?.distinct().orEmpty()
        if (ciclos.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Ciclos:", color = Tenue)
                (listOf("" to "Todos") + ciclos.map { it to it }).forEach { (k, n) ->
                    val on = ciclo == k
                    Text(n, color = if (on) Color(0xFF1B1E23) else TextoClaro, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) Amarillo else Color(0x14FFFFFF)).mano().clickable { ciclo = k }.padding(horizontal = 14.dp, vertical = 7.dp))
                }
            }
        }
        when {
            r == null -> item(span = { GridItemSpan(maxLineSpan) }) { Cargando("Abriendo la bóveda…") }
            r.isFailure -> item(span = { GridItemSpan(maxLineSpan) }) { Text("No se pudo abrir la bóveda.", color = Tenue, modifier = Modifier.padding(18.dp)) }
            r.getOrThrow().isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) { Text("Hoy no hay conciertos en cartelera.", color = Tenue, modifier = Modifier.padding(18.dp)) }
            else -> items(r.getOrThrow().filter { ciclo.isEmpty() || it.ciclo == ciclo }, key = { it.id }) { v ->
                Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).mano().clickable { Nav.ir(Destino.Concierto(v.id)) }) {
                    val img = imagen(v.miniatura)
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF2A2E36))) {
                        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.align(Alignment.Center).size(60.dp).clip(CircleShape).background(Color(0xAA000000)), contentAlignment = Alignment.Center) { Icono("play", Color.White, 30.dp) }
                    }
                    Column(Modifier.padding(16.dp)) {
                        Text(v.titulo, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 23.sp)
                        Text(sinPuntos(v.artista, v.festival, if (v.anio > 0) v.anio.toString() else "", v.ciclo.takeIf { it.isNotBlank() }?.let { "Ciclo: $it" } ?: ""), color = Tenue, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(90.dp)) }
    }
    if (Cuenta.esEditor) Box(Modifier.align(Alignment.BottomEnd).padding(18.dp).size(60.dp).clip(RoundedCornerShape(20.dp)).background(Amarillo).mano().clickable { Nav.ir(Destino.EditarConcierto(null)) }, contentAlignment = Alignment.Center) {
        Text("+", color = Color(0xFF1B1E23), fontSize = 34.sp, fontWeight = FontWeight.Bold)
    }
    }
}

/* ---------------- Más ---------------- */
@Composable
private fun Mas() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)) {
        Cabecera("Más", "Todo lo de la Revista")
        (listOf(Triple("Chats", "Mensajes privados y contactos", Destino.Chats), Triple("Buscar", "Personas, artículos y artistas", Destino.Buscar),
            Triple("Ediciones", "Lo mejor de cada mes, como revista impresa", Destino.Ediciones), Triple("Guardados", "Artículos para leer sin internet", Destino.Guardados), Triple("Debates", "Encuestas de la comunidad", Destino.Debates), Triple("Agenda", "Conciertos y festivales", Destino.Agenda), Triple("Club de escucha", "Un disco por semana, canción por canción", Destino.Club),
            Triple("Notificaciones", "Me gusta, comentarios y seguidores", Destino.Notificaciones)) + (if (Cuenta.esAdmin) listOf(Triple("Admin", "Denuncias, usuarios, estadísticas y ajustes", Destino.Admin)) else emptyList())).forEach { (t, s, d) ->
            Column(Modifier.padding(horizontal = 18.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).clickable { if ((d == Destino.Notificaciones || d == Destino.Chats) && !Nav.necesitaCuenta()) return@clickable; Nav.ir(d) }.padding(16.dp)) {
                Text(t, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(s, color = Tenue, fontSize = 14.sp)
            }
        }
        Column(Modifier.padding(18.dp).clip(RoundedCornerShape(20.dp)).background(Panel).padding(18.dp)) {
            Text("Esta es la Revista hecha en Kotlin: el mismo código sirve para la app de Android y para el navegador.", fontSize = 15.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(12.dp))
            Boton("Abrir la Revista web") { abrirEnlace(Supa.WEB + "#revista") }
        }
    }
}
