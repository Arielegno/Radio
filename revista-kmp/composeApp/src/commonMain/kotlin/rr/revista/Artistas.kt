package rr.revista

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val ENLACES = listOf("bandcamp" to "Bandcamp", "spotify" to "Spotify", "youtube" to "YouTube", "instagram" to "Instagram", "web" to "Sitio oficial")
private fun enlaces(m: Map<String, Any?>): Map<String, String> = ENLACES.mapNotNull { (k, _) -> m.str(k).trim().takeIf { Regex("^https://[^\\s\"'<>]{4,300}$").matches(it) }?.let { k to it } }.toMap()
private fun patron(t: String) = t.trim().replace(Regex("[*,()%]"), " ").replace(Regex("\\s+"), " ").trim()

/** Lista de artistas con buscador. */
@Composable
fun ArtistasPantalla() {
    var q by remember { mutableStateOf("") }
    val datos by produceState<Result<List<Map<String, Any?>>>?>(null) {
        value = runCatching { Json.arr(runCatching { Cuenta.api("GET", "/rest/v1/artists?select=id,name,genre,years,cover,style,verified&order=name.asc&limit=1000") }.getOrElse { Cuenta.api("GET", "/rest/v1/artists?select=id,name,genre,years,cover,style&order=name.asc&limit=1000") }).mapNotNull { it.asObj() } }
    }
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    Cabecera("Artistas", "Perfiles de bandas y artistas hechos por la Revista")
                    Box(Modifier.padding(horizontal = 6.dp)) { Campo(q, { q = it }, "Buscar artista, banda o género…", max = 60) }
                }
            }
            val r = datos
            when {
                r == null -> item(span = { GridItemSpan(2) }) { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) } }
                r.isFailure -> item(span = { GridItemSpan(2) }) { Text(r.exceptionOrNull()?.message ?: "", color = Tenue, modifier = Modifier.padding(8.dp)) }
                else -> {
                    val n = normNombre(q)
                    val l = r.getOrThrow().filter { n.isEmpty() || normNombre(it.str("name") + " " + it.str("genre") + " " + it.str("years")).contains(n) }
                    if (l.isEmpty()) item(span = { GridItemSpan(2) }) { Text(if (r.getOrThrow().isEmpty()) "Aún no hay perfiles de artistas." + if (Cuenta.esEditor) " Toca + para crear uno." else "" else "No encontré ese artista.", color = Tenue, modifier = Modifier.padding(8.dp)) }
                    items(l, key = { it.str("id") }) { a -> TarjetaArtista(a) { Nav.ir(Destino.Artista(a.str("id"))) } }
                }
            }
            item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(80.dp)) }
        }
        if (Cuenta.esEditor) Box(Modifier.align(Alignment.BottomEnd).padding(18.dp).size(60.dp).clip(RoundedCornerShape(20.dp)).background(Amarillo).clickable { Nav.ir(Destino.EditarArtista(null)) }, contentAlignment = Alignment.Center) {
            Text("+", color = Color(0xFF1B1E23), fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Nombre normalizado (sin tildes ni mayúsculas) para buscar. */
fun normNombre(t: String): String {
    val sin = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n')
    return t.lowercase().map { sin[it] ?: it }.joinToString("").replace(Regex("[^a-z0-9]+"), " ").trim()
}

@Composable
private fun TarjetaArtista(a: Map<String, Any?>, abrir: () -> Unit) {
    val img = imagen(Supa.miniatura(Diseno.imagenOk(a.str("cover"))), Diseno.imagenOk(a.str("cover")))
    val e = Estilo.de(a.map("style")); val t = Temas.de(e)
    Box(Modifier.fillMaxWidth().aspectRatio(.72f).clip(RoundedCornerShape(18.dp)).drawBehind { fondoTema(e, t) }.clickable(onClick = abrir)) {
        if (img != null) Image(img, a.str("name"), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(a.str("name").take(1).uppercase(), color = col(t.texto).copy(alpha = .5f), fontSize = 64.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color(0xE6000000))))
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(a.str("name") + if (a.bool("verified")) " · verificada" else "", color = Color.White, fontFamily = Fuentes.titulo(e.ft), fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 22.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            val sub = listOf(a.str("genre"), a.str("years")).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, color = Color.White.copy(alpha = .8f), fontSize = 12.sp, maxLines = 1)
        }
    }
}

private data class DatosArtista(val a: Map<String, Any?>, val posters: List<PosterCompleto>, val articulos: List<Articulo>, val resenas: List<Map<String, Any?>>, val gestores: List<Pair<String, String>>)

/** Perfil de un artista: portada estilo revista, enlaces, biografía, reseñas, pósters y artículos. */
@Composable
fun ArtistaPantalla(id: String) {
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf(0) }
    var borrar by remember { mutableStateOf(false) }
    var denunciar by remember { mutableStateOf(false) }
    var gestor by remember { mutableStateOf(false) }
    val datos by produceState<Result<DatosArtista?>?>(null, id, version) {
        value = runCatching {
            val a = Json.arr(Cuenta.api("GET", "/rest/v1/artists?select=*&id=eq.$id")).firstOrNull()?.asObj() ?: return@runCatching null
            val pat = patron(a.str("name")); val w = Supa.enc("*$pat*")
            val sel = "id,title,subtitle,cover,style,review,tags,created_at,author_id,profiles!author_id(name,avatar),likes(count),comments(count)"
            DatosArtista(a,
                if (pat.isEmpty()) emptyList() else runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/posters?select=${Galeria.SEL}&artist=ilike.$w&order=year.asc.nullslast&limit=30")).mapNotNull { it.asObj()?.let(Galeria::deFila) } }.getOrDefault(emptyList()),
                if (pat.isEmpty()) emptyList() else runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/posts?select=$sel&or=" + Supa.enc("(title.ilike.*$pat*,subtitle.ilike.*$pat*)") + Supa.publicados() + "&order=created_at.desc&limit=12")).mapNotNull { it.asObj()?.let(Supa::deFila) } }.getOrDefault(emptyList()),
                if (pat.isEmpty()) emptyList() else runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/posts?select=id,title,review,created_at,author_id,profiles!author_id(name)&review->>artist=ilike.$w&order=created_at.desc&limit=30")).mapNotNull { it.asObj() }.filter { it.map("review").num("score") in 1..10 } }.getOrDefault(emptyList()),
                runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/artist_managers?select=user_id,profiles!user_id(name)&artist_id=eq.$id")).mapNotNull { it.asObj()?.let { m -> m.str("user_id") to m.map("profiles").str("name") } } }.getOrDefault(emptyList()))
        }
    }
    val r = datos
    Box(Modifier.fillMaxSize().background(Asfalto)) {
        when {
            r == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }
            r.isFailure || r.getOrNull() == null -> Column(Modifier.padding(20.dp)) { Enlace("‹ Volver") { Nav.atras() }; Text("Ese artista ya no existe.", color = Tenue) }
            else -> {
                val d = r.getOrThrow()!!; val a = d.a
                val e = Estilo.de(a.map("style")).let { if (a.map("style")["cv"] == null) it.copy(cv = "m") else it }
                val info = listOf(a.str("genre"), a.str("years"), a.str("origin")).filter { it.isNotBlank() }.joinToString(" · ")
                val art = Articulo("ejemplo-artista", a.str("name"), info, Diseno.imagenOk(a.str("cover")), e, a.str("updated_at").ifEmpty { a.str("created_at") }, Autor("", "", ""), 0, 0, texto = a.str("bio"))
                val L = enlaces(a.map("links"))
                val puedeEditar = Cuenta.esEditor || d.gestores.any { it.first == Cuenta.uid }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Enlace("‹ Artistas", TextoClaro) { Nav.atras() }; Spacer(Modifier.weight(1f))
                        if (puedeEditar) Enlace("Editar") { Nav.ir(Destino.EditarArtista(id)) }
                        if (Cuenta.esEditor) Enlace("Eliminar", Color(0xFFFF8A7A)) { borrar = true }
                    }
                    Box(Modifier.padding(horizontal = 16.dp)) { CabeceraEstilo(art) }
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (a.bool("verified")) Text("Banda verificada por la Revista", color = Amarillo, fontWeight = FontWeight.Bold)
                        if (L.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ENLACES.filter { it.first in L }.forEach { (k, n) -> Boton(n, principal = false) { abrirEnlace(L.getValue(k)) } }
                        }
                        if (a.str("bio").isBlank()) Text("Sin biografía todavía.", color = Tenue) else TextoArticulo(art)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Boton("Compartir", principal = false) { abrirEnlace(Supa.WEB + "#rva=" + id) }; Enlace("Denunciar", Tenue) { if (Nav.necesitaCuenta()) denunciar = true } }
                        if (d.gestores.isNotEmpty()) Text("Perfil gestionado por: " + d.gestores.joinToString(", ") { it.second }, color = Tenue, fontSize = 14.sp)
                        if (Cuenta.esAdmin) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x1AF2C230)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Solo tú ves esto", fontWeight = FontWeight.Bold)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Boton(if (a.bool("verified")) "Quitar verificación" else "Verificar banda", principal = false) { scope.launch { runCatching { Cuenta.api("PATCH", "/rest/v1/artists?id=eq.$id", mapOf("verified" to !a.bool("verified")), "return=minimal") }; version++ } }
                                Boton("Añadir gestor", principal = false) { gestor = true }
                            }
                            d.gestores.forEach { (uid, n) -> Row(verticalAlignment = Alignment.CenterVertically) { Text(n, modifier = Modifier.weight(1f)); Enlace("quitar", Tenue) { scope.launch { runCatching { Cuenta.api("DELETE", "/rest/v1/artist_managers?artist_id=eq.$id&user_id=eq.$uid") }; version++ } } } }
                        }
                        Text("Reseñas de la comunidad", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        if (d.resenas.isEmpty()) Text("Aún no hay reseñas de sus discos. Escribe una marcando «reseña de disco» en el editor.", color = Tenue)
                        else {
                            val prom = d.resenas.sumOf { it.map("review").num("score").toInt() }.toDouble() / d.resenas.size
                            Text("${(prom * 10).toInt() / 10.0}/10 · promedio de la comunidad (${d.resenas.size} ${if (d.resenas.size == 1) "reseña" else "reseñas"})", fontWeight = FontWeight.Bold)
                            d.resenas.forEach { p ->
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).clickable { Nav.ir(Destino.Leer(p.str("id"))) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${p.map("review").num("score")}", color = Color(0xFF1B1E23), fontWeight = FontWeight.ExtraBold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Amarillo).padding(horizontal = 9.dp, vertical = 3.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column { Text(p.map("review").str("album"), fontWeight = FontWeight.SemiBold); Text("${p.str("title")} · por ${p.map("profiles").str("name")}", color = Tenue, fontSize = 13.sp) }
                                }
                            }
                        }
                        Text("Pósters", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        if (d.posters.isEmpty()) Text("Aún no hay pósters de este artista. Búscalos en la pestaña Pósters.", color = Tenue)
                        else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            d.posters.forEach { p -> Box(Modifier.width(130.dp)) { TarjetaPoster(p) { Nav.ir(Destino.Poster(p.id)) } } }
                        }
                        Text("En la Revista", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        if (d.articulos.isEmpty()) Text("Todavía no hay artículos que lo mencionen en el título.", color = Tenue)
                    }
                    d.articulos.forEach { p -> Tarjeta(p) { Nav.ir(Destino.Leer(p.id)) } }
                }
                if (denunciar) DialogoDenuncia("artist", id) { denunciar = false }
                if (borrar) DialogoConfirmar("¿Eliminar el perfil de ${a.str("name")}?", "Los pósters y artículos no se borran.", "Eliminar", { borrar = false }) {
                    borrar = false; scope.launch { try { Cuenta.api("DELETE", "/rest/v1/artists?id=eq.$id"); Aviso.mostrar("Perfil eliminado."); Nav.atras() } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } }
                }
                if (gestor) DialogoGestor(id, { gestor = false }) { gestor = false; version++ }
            }
        }
    }
}

@Composable
private fun DialogoGestor(artista: String, cerrar: () -> Unit, listo: () -> Unit) {
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var res by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(q) {
        val t = patron(q); if (t.length < 2) { res = emptyList(); return@LaunchedEffect }
        kotlinx.coroutines.delay(300)
        res = runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/profiles?select=id,name&name=ilike." + Supa.enc("*$t*") + "&limit=8")).mapNotNull { it.asObj()?.let { p -> p.str("id") to p.str("name") } } }.getOrDefault(emptyList())
    }
    Hoja("Añadir gestor", cerrar) {
        Text("Escribe el nombre público de su cuenta en la Revista.", color = Tenue)
        Campo(q, { q = it }, "Nombre", max = 40)
        res.forEach { (uid, n) ->
            Text(n, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x14FFFFFF)).clickable {
                scope.launch { try { Cuenta.api("POST", "/rest/v1/artist_managers", mapOf("artist_id" to artista, "user_id" to uid), "return=minimal"); Aviso.mostrar("Gestor añadido.") } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }; listo() }
            }.padding(14.dp))
        }
    }
}

/** Crear o editar el perfil de un artista (admin, editores y gestores de la banda). */
@Composable
fun EditarArtistaPantalla(id: String?) {
    val scope = rememberCoroutineScope()
    var nombre by remember { mutableStateOf("") }; var genero by remember { mutableStateOf("") }; var anios by remember { mutableStateOf("") }; var origen by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }; var portada by remember { mutableStateOf("") }; var foto by remember { mutableStateOf<Foto?>(null) }
    var estilo by remember { mutableStateOf(Estilo(cv = "m")) }
    var links by remember { mutableStateOf(ENLACES.associate { it.first to "" }) }
    var cargando by remember { mutableStateOf(id != null) }; var guardando by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (id == null) return@LaunchedEffect
        runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/artists?select=*&id=eq.$id")).firstOrNull()?.asObj() }.getOrNull()?.let { a ->
            nombre = a.str("name"); genero = a.str("genre"); anios = a.str("years"); origen = a.str("origin"); bio = a.str("bio"); portada = Diseno.imagenOk(a.str("cover"))
            estilo = Estilo.de(a.map("style")).let { if (a.map("style")["cv"] == null) it.copy(cv = "m") else it }
            val l = enlaces(a.map("links")); links = ENLACES.associate { it.first to (l[it.first] ?: "") }
        }
        cargando = false
    }
    Box(Modifier.fillMaxSize().background(Asfalto)) {
        if (cargando) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }; return@Box }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Enlace("Cancelar", Tenue) { Nav.atras() }; Text(if (id == null) "Nuevo artista" else "Editar artista", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp)) }
            Campo(nombre, { nombre = it }, "Nombre del artista o banda", max = 80)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { Campo(genero, { genero = it }, "Género", max = 60, pista = "Grunge") }; Box(Modifier.weight(1f)) { Campo(anios, { anios = it }, "Años", max = 40, pista = "1987–1994") } }
            Campo(origen, { origen = it }, "Origen", max = 60, pista = "Aberdeen, Washington")
            Text("Enlaces oficiales (https://…)", color = Tenue, fontSize = 13.sp)
            ENLACES.forEach { (k, n) -> Campo(links[k] ?: "", { v -> links = links + (k to v) }, n, max = 300, pista = "https://") }
            Campo(bio, { bio = it }, "Biografía", lineas = 8, max = 6000, pista = "Su historia, sus discos clave…")
            val img = imagen(foto?.mini ?: portada)
            if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(14.dp)))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Boton(if (img == null) "Elegir imagen de portada" else "Cambiar imagen", principal = false) { val e = elegirImagenes(1400, 520, false); scope.launch { e.esperar().firstOrNull()?.let { foto = it } } }
                if (img != null) Boton("Quitar", principal = false) { foto = null; portada = "" }
            }
            Text("Estética del artista", fontWeight = FontWeight.Bold)
            Pestanas(Estilo.TEMAS.map { it to (Estilo.NOMBRES_TEMA[it] ?: it) }, estilo.th) { estilo = estilo.conTema(it) }
            Pestanas(Estilo.FUENTES.map { it to (Estilo.NOMBRES_FUENTE[it] ?: it) }, estilo.ft) { estilo = estilo.copy(ft = it) }
            Pestanas(listOf("a" to "Imagen arriba", "f" to "Imagen de fondo", "m" to "Portada de revista"), estilo.cv) { estilo = estilo.copy(cv = it) }
            Box(Modifier.fillMaxWidth()) { CabeceraEstilo(Articulo("ejemplo-artista", nombre.ifBlank { "Artista" }, listOf(genero, anios).filter { it.isNotBlank() }.joinToString(" · "), foto?.grande ?: portada, estilo, ahoraIso(), Autor("", "", ""), 0, 0)) }
            Boton(if (id == null) "Crear artista" else "Guardar cambios", ocupado = guardando, habilitado = nombre.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                val malos = links.filter { (_, v) -> v.isNotBlank() && !Regex("^https://[^\\s\"'<>]{4,300}$").matches(v.trim()) }
                if (malos.isNotEmpty()) { Aviso.mostrar("Los enlaces deben empezar con https://", true); return@Boton }
                scope.launch {
                    guardando = true
                    try {
                        val cov = foto?.let { subirFoto(it, "a") } ?: portada.ifEmpty { null }
                        val fila = linkedMapOf<String, Any?>("name" to nombre.trim().take(80), "genre" to genero.trim().ifEmpty { null }, "years" to anios.trim().ifEmpty { null }, "origin" to origen.trim().ifEmpty { null },
                            "bio" to bio.trim().ifEmpty { null }, "cover" to cov, "style" to estilo.copy(cv = estilo.cv).aMapa().let { m -> if (estilo.cv == "m") m - "cv" + ("cv" to "m") else m },
                            "links" to links.mapValues { it.value.trim() }.filterValues { it.isNotEmpty() }, "updated_at" to ahoraIsoMenos(0))
                        val nuevo = if (id != null) { Cuenta.api("PATCH", "/rest/v1/artists?id=eq.$id", fila, "return=minimal"); id }
                        else Json.arr(Cuenta.api("POST", "/rest/v1/artists", fila + ("created_by" to Cuenta.uid), "return=representation")).firstOrNull()?.asObj()?.str("id") ?: ""
                        Aviso.mostrar(if (id != null) "Artista actualizado." else "Artista creado.")
                        Nav.atras(); if (id == null && nuevo.isNotEmpty()) Nav.ir(Destino.Artista(nuevo))
                    } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
                    guardando = false
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}
