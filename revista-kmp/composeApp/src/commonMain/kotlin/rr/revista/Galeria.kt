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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Un póster de la galería, con todos sus datos. */
data class PosterCompleto(
    val id: String, val titulo: String, val artista: String, val anio: Int, val tipo: String, val fuente: String, val mini: String, val grande: String,
    val credito: String, val disenador: String, val licencia: String, val urlFuente: String, val reto: String, val creador: String, val creadorNombre: String, val fecha: String, val votos: Int = 0
)

object Galeria {
    const val SEL = "id,title,artist,year,kind,source,thumb_url,image_url,credit,designer,license,source_url,challenge_id,created_by,created_at,profiles!created_by(name)"

    fun deFila(m: Map<String, Any?>): PosterCompleto? {
        val g = Diseno.imagenOk(m.str("image_url")); val mi = Diseno.imagenOk(m.str("thumb_url")).ifEmpty { g }
        if (mi.isEmpty()) return null
        return PosterCompleto(m.str("id"), m.str("title"), m.str("artist"), m.num("year").toInt(), m.str("kind"), m.str("source"), mi, g.ifEmpty { mi },
            m.str("credit"), m.str("designer"), m.str("license"), m.str("source_url").takeIf { it.startsWith("https://") } ?: "", m.str("challenge_id"),
            m.str("created_by"), m.map("profiles").str("name").ifBlank { "Alguien" }, m.str("created_at"))
    }

    private fun patron(t: String) = t.trim().replace(Regex("[*,()%]"), " ").replace(Regex("\\s+"), " ").trim()

    suspend fun lista(filtro: String, busqueda: String, desde: Int): List<PosterCompleto> {
        var q = "/rest/v1/posters?select=$SEL&order=created_at.desc&limit=20&offset=$desde"
        if (filtro == "original" || filtro == "comunidad") q += "&kind=eq.$filtro"
        val t = patron(busqueda)
        if (Regex("^(1[89]\\d\\d|20\\d\\d)$").matches(t)) q += "&year=eq.$t"
        else if (t.isNotEmpty()) q += "&or=" + Supa.enc("(title.ilike.*$t*,artist.ilike.*$t*)")
        return Json.arr(Cuenta.api("GET", q)).mapNotNull { it.asObj()?.let(::deFila) }
    }

    suspend fun uno(id: String): PosterCompleto? = Json.arr(Cuenta.api("GET", "/rest/v1/posters?select=$SEL&id=eq.$id")).firstOrNull()?.asObj()?.let(::deFila)

    /** Sube un póster de la comunidad (grande + miniatura) y lo guarda en la galería. */
    suspend fun subir(f: Foto, titulo: String, artista: String, disenador: String, anio: String, reto: String?): String {
        val g = subirFoto(f, "p")
        val y = anio.toIntOrNull()?.takeIf { it in 1800..2100 }
        val fila = linkedMapOf<String, Any?>("title" to titulo.trim().take(120).ifEmpty { "Póster" }, "artist" to artista.trim().take(80).ifEmpty { null }, "year" to y,
            "kind" to "comunidad", "source" to "subido", "image_url" to g, "thumb_url" to g.removeSuffix(".jpg") + "-t.jpg", "designer" to disenador.trim().take(80).ifEmpty { null },
            "challenge_id" to reto, "created_by" to Cuenta.uid)
        return Json.arr(Cuenta.api("POST", "/rest/v1/posters", fila, "return=representation")).firstOrNull()?.asObj()?.str("id") ?: ""
    }

    data class Original(val titulo: String, val url: String, val mini: String, val ficha: String, val licencia: String, val credito: String, val anio: Int)

    private fun sinHtml(s: String) = s.replace(Regex("<[^>]*>"), " ").replace("&amp;", "&").replace("&quot;", "\"").replace(Regex("\\s+"), " ").trim()

    /** Busca pósters con licencia libre en Wikimedia Commons (como la web). */
    suspend fun buscarCommons(q: String): List<Original> {
        val u = "https://commons.wikimedia.org/w/api.php?action=query&format=json&origin=*&generator=search&gsrnamespace=6&gsrlimit=30&gsrsearch=" + Supa.enc("$q poster filetype:bitmap") +
            "&prop=imageinfo&iiprop=url|extmetadata|mime&iiurlwidth=500&iiextmetadatafilter=LicenseShortName|Artist|DateTimeOriginal|ObjectName"
        val r = httpPedido("GET", u, emptyMap(), null)
        if (r.estado !in 200..299) throw ErrorRevista("Wikimedia no respondió (${r.estado}).")
        val pages = Json.obj(r.texto).map("query").map("pages").values.mapNotNull { it.asObj() }.sortedBy { it.num("index") }
        return pages.mapNotNull { p ->
            val ii = p.list("imageinfo").firstOrNull()?.asObj() ?: return@mapNotNull null
            val m = ii.map("extmetadata"); fun tx(k: String) = sinHtml(m.map(k).str("value"))
            val url = ii.str("url"); val th = ii.str("thumburl").ifEmpty { url }; val src = ii.str("descriptionurl")
            if (!url.startsWith("https://upload.wikimedia.org/") || !th.startsWith("https://upload.wikimedia.org/") || !src.startsWith("https://commons.wikimedia.org/")) return@mapNotNull null
            if (ii.str("mime") !in setOf("image/jpeg", "image/png", "image/webp")) return@mapNotNull null
            val y = Regex("\\b(1[89]\\d\\d|20\\d\\d)\\b").find(tx("DateTimeOriginal") + " " + p.str("title"))?.value?.toInt() ?: 0
            Original(tx("ObjectName").ifEmpty { p.str("title").removePrefix("File:").replace(Regex("\\.[a-zA-Z0-9]+$"), "") }.take(120), url, th, src, tx("LicenseShortName").take(120), tx("Artist").take(300), y)
        }
    }

    suspend fun guardarOriginal(o: Original, titulo: String, artista: String, anio: String) {
        val y = anio.toIntOrNull()?.takeIf { it in 1800..2100 }
        Cuenta.api("POST", "/rest/v1/posters", linkedMapOf("title" to titulo.trim().take(120).ifEmpty { "Póster" }, "artist" to artista.trim().take(80).ifEmpty { null }, "year" to y,
            "kind" to "original", "source" to "commons", "image_url" to o.url, "thumb_url" to o.mini, "credit" to o.credito.ifEmpty { null }, "license" to o.licencia.ifEmpty { null },
            "source_url" to o.ficha, "created_by" to Cuenta.uid), "return=minimal")
    }
}

@Composable
fun Insignia(tipo: String) {
    val og = tipo == "original"
    Text(if (og) "Original" else "De la comunidad", color = if (og) Color(0xFF1B1E23) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (og) Amarillo else Color(0xFF3D5AFE)).padding(horizontal = 7.dp, vertical = 2.dp))
}

@Composable
fun TarjetaPoster(p: PosterCompleto, extra: (@Composable () -> Unit)? = null, abrir: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = abrir)) {
        val img = imagen(p.mini, p.grande)
        Box(Modifier.fillMaxWidth().aspectRatio(.7f).clip(RoundedCornerShape(14.dp)).background(Panel)) {
            if (img != null) Image(img, p.titulo, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.padding(6.dp)) { Insignia(p.tipo) }
        }
        Text(p.titulo, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
        Text(listOf(p.artista, if (p.anio > 0) p.anio.toString() else "").filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { " " }, color = Tenue, fontSize = 13.sp, maxLines = 1)
        extra?.invoke()
    }
}

/** La galería: buscar, filtrar (originales, comunidad, retos), subir y buscar originales. */
@Composable
fun GaleriaPantalla() {
    var filtro by remember { mutableStateOf("all") }
    var busqueda by remember { mutableStateOf("") }
    var lista by remember { mutableStateOf<List<PosterCompleto>>(emptyList()) }
    var hayMas by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun cargar(mas: Boolean) {
        cargando = true; error = ""
        try { val n = Galeria.lista(filtro, busqueda, if (mas) lista.size else 0); lista = if (mas) lista + n else n; hayMas = n.size == 20 } catch (e: Throwable) { error = e.message ?: "" }
        cargando = false
    }
    LaunchedEffect(filtro, busqueda) { if (filtro != "retos") { delay(if (busqueda.isEmpty()) 0 else 350); cargar(false) } }
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(GridCells.Adaptive(165.dp), Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Cabecera("Pósters", "Carteles de conciertos y giras: originales con licencia libre y de la comunidad", lateral = 6.dp)
                    Box(Modifier.padding(horizontal = 6.dp)) { Campo(busqueda, { busqueda = it }, "Buscar por banda, título o año…", max = 60) }
                    Box(Modifier.padding(6.dp)) { Pestanas(listOf("all" to "Todos", "original" to "Originales", "comunidad" to "De la comunidad", "retos" to "Retos"), filtro) { filtro = it } }
                }
            }
            if (filtro == "retos") item(span = { GridItemSpan(maxLineSpan) }) { Retos() }
            else {
                if (error.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Text(error, color = Tenue, modifier = Modifier.padding(8.dp)) }
                if (!cargando && lista.isEmpty() && error.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Text(if (busqueda.isNotBlank()) "No encontré pósters con esa búsqueda." else "La galería está vacía. Toca + para buscar originales o subir uno.", color = Tenue, modifier = Modifier.padding(8.dp)) }
                items(lista, key = { it.id }) { p -> TarjetaPoster(p) { Nav.ir(Destino.Poster(p.id)) } }
                if (cargando) item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) } }
                else if (hayMas) item(span = { GridItemSpan(maxLineSpan) }) { Boton("Cargar más", principal = false) { scope.launch { cargar(true) } } }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(80.dp)) }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(18.dp).size(60.dp).clip(RoundedCornerShape(20.dp)).background(Amarillo).clickable { if (Nav.necesitaCuenta()) menu = true }, contentAlignment = Alignment.Center) {
            Text("+", color = Color(0xFF1B1E23), fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        if (menu) Hoja("Añadir un póster", { menu = false }) {
            Text("Busca pósters reales con su fuente, o sube un diseño tuyo. En la Revista el arte lo hacen personas.", color = Tenue)
            Boton("Buscar pósters originales (archivo libre)", modifier = Modifier.fillMaxWidth()) { menu = false; Nav.ir(Destino.BuscarOriginales) }
            Boton("Subir un póster mío", principal = false, modifier = Modifier.fillMaxWidth()) { menu = false; Nav.ir(Destino.SubirPoster(null)) }
        }
    }
}

/** Ficha de un póster: imagen en grande, datos, descargar, compartir, colección, artista, eliminar y denunciar. */
@Composable
fun PosterPantalla(id: String) {
    val scope = rememberCoroutineScope()
    var grande by remember { mutableStateOf(false) }
    var coleccion by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf(false) }
    var denunciar by remember { mutableStateOf(false) }
    val datos by produceState<Result<PosterCompleto?>?>(null, id) { value = runCatching { Galeria.uno(id) } }
    val artista by produceState<Pair<String, String>?>(null, datos) {
        val p = datos?.getOrNull() ?: return@produceState
        if (p.artista.isBlank()) return@produceState
        value = runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/artists?select=id,name,genre,years&name=ilike." + Supa.enc(p.artista.replace(Regex("[*,()%]"), " ").trim()) + "&limit=3")).firstOrNull()?.asObj()?.let { it.str("id") to listOf(it.str("genre"), it.str("years")).filter(String::isNotBlank).joinToString(" · ") } }.getOrNull()
    }
    val r = datos
    Box(Modifier.fillMaxSize().background(Asfalto)) {
        when {
            r == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }
            r.isFailure || r.getOrNull() == null -> Column(Modifier.padding(20.dp)) { Enlace("‹ Volver") { Nav.atras() }; Text("Ese póster ya no está en la galería.", color = Tenue) }
            else -> {
                val p = r.getOrThrow()!!
                val mio = Cuenta.dentro && Cuenta.uid == p.creador
                val img = imagen(p.grande, p.mini)
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Enlace("‹ Volver", TextoClaro) { Nav.atras() }; Spacer(Modifier.weight(1f)); if (mio || Cuenta.esAdmin) Enlace("Eliminar", Color(0xFFFF8A7A)) { borrar = true } }
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(Panel).clickable { grande = true }) {
                        if (img != null) Image(img, p.titulo, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
                        else Box(Modifier.fillMaxWidth().aspectRatio(.7f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }
                    }
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Insignia(p.tipo)
                        Text(p.titulo, fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 32.sp)
                        val sub = listOf(p.artista, if (p.anio > 0) p.anio.toString() else "").filter { it.isNotBlank() }.joinToString(" · ")
                        if (sub.isNotEmpty()) Text(sub, color = Tenue, fontSize = 17.sp)
                        if (p.tipo == "original") {
                            val c = p.credito.trim(); val mitad = if (c.length % 2 == 0) c.substring(0, c.length / 2) else ""
                            Text("Autor: ${(if (mitad.isNotEmpty() && mitad + mitad == c) mitad else c).ifEmpty { "desconocido" }}" + if (p.licencia.isNotEmpty()) " · Licencia: ${p.licencia}" else "", color = Tenue, fontSize = 14.sp)
                            if (p.urlFuente.isNotEmpty()) Enlace("Ver la ficha en Wikimedia Commons") { abrirEnlace(p.urlFuente) }
                        } else {
                            if (p.disenador.isNotBlank()) Text("Arte de ${p.disenador}", fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) { Text("Subido por ", color = Tenue, fontSize = 14.sp); Enlace(p.creadorNombre) { Nav.ir(Destino.Perfil(p.creador)) }; Text("· ${Fechas.corta(p.fecha)}", color = Tenue, fontSize = 14.sp) }
                        }
                        // Detalles
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val filas = buildList {
                                if (p.anio > 0) add("Año" to p.anio.toString())
                                if (p.artista.isNotBlank()) add("Artista" to p.artista + (artista?.second?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""))
                                add("Tipo" to if (p.tipo == "original") "Póster original de la época" else if (p.fuente == "ia") "Creado con IA" else "Diseño de la comunidad")
                                if (p.licencia.isNotBlank()) add("Licencia" to p.licencia)
                                img?.let { add("Tamaño" to "${it.width} × ${it.height} px · ${if (it.width > it.height) "horizontal" else if (it.width < it.height) "vertical" else "cuadrado"}") }
                                add("En la galería desde" to Fechas.corta(p.fecha))
                            }
                            filas.forEach { (k, v) -> Row { Text(k, color = Tenue, fontSize = 14.sp, modifier = Modifier.width(130.dp)); Text(v, fontSize = 14.sp) } }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Boton("Descargar", principal = false) { abrirEnlace(p.grande) }
                            Boton("Compartir", principal = false) { compartirEnlace("Mira esto en la Revista de Radio de ruta", Supa.WEB + "#rvp=" + p.id) }
                            if (p.artista.isNotBlank()) Boton("Comparar", principal = false) { Nav.ir(Destino.Comparar(p.artista)) }
                            Boton("Guardar en colección", principal = false) { if (Nav.necesitaCuenta()) coleccion = true }
                            artista?.let { (aid, _) -> Boton("Ver la ficha de ${p.artista}", principal = false) { Nav.ir(Destino.Artista(aid)) } }
                        }
                        if (!mio) Enlace("Denunciar", Tenue) { if (Nav.necesitaCuenta()) denunciar = true }
                    }
                }
                if (grande && img != null) Box(Modifier.fillMaxSize().background(Color(0xF5000000)).clickable { grande = false }, contentAlignment = Alignment.Center) {
                    Image(img, p.titulo, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(10.dp))
                }
                if (coleccion) DialogoColeccion(p.id) { coleccion = false }
                if (denunciar) DialogoDenuncia("poster", p.id) { denunciar = false }
                if (borrar) DialogoConfirmar("¿Quitar este póster de la galería?", "No se puede deshacer.", "Eliminar", { borrar = false }) {
                    borrar = false
                    scope.launch { try { Cuenta.api("DELETE", "/rest/v1/posters?id=eq.${p.id}"); Aviso.mostrar("Póster eliminado."); Nav.atras() } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } }
                }
            }
        }
    }
}

/** Guardar un póster en una colección tuya (o crear una nueva). */
@Composable
fun DialogoColeccion(poster: String, cerrar: () -> Unit) {
    val scope = rememberCoroutineScope()
    var nueva by remember { mutableStateOf("") }
    val cols by produceState<List<Pair<String, String>>?>(null) { value = runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/collections?select=id,title&owner=eq.${Cuenta.uid}&order=created_at.desc")).mapNotNull { it.asObj()?.let { c -> c.str("id") to c.str("title") } } }.getOrDefault(emptyList()) }
    fun guardar(id: String) = scope.launch {
        try { Cuenta.api("POST", "/rest/v1/collection_items", mapOf("collection_id" to id, "poster_id" to poster), "return=minimal"); Aviso.mostrar("Guardado en tu colección.") }
        catch (e: Throwable) { Aviso.mostrar(if ((e.message ?: "").contains("ya existe", true) || (e.message ?: "").contains("Eso ya existe")) "Ya estaba en esa colección." else e.message ?: "", true) }
        cerrar()
    }
    Hoja("Guardar en colección", cerrar) {
        val l = cols
        if (l == null) CircularProgressIndicator(color = Amarillo)
        else l.forEach { (id, t) -> Text(t, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x14FFFFFF)).clickable { guardar(id) }.padding(14.dp)) }
        Text("Nueva colección", color = Tenue, fontSize = 14.sp)
        Campo(nueva, { nueva = it }, "Nombre", max = 80, pista = "Mis pósters punk favoritos")
        Boton("Crear y guardar", habilitado = nueva.trim().length >= 2, modifier = Modifier.fillMaxWidth()) {
            scope.launch {
                try { val id = Json.arr(Cuenta.api("POST", "/rest/v1/collections", mapOf("owner" to Cuenta.uid, "title" to nueva.trim().take(80)), "return=representation")).firstOrNull()?.asObj()?.str("id") ?: return@launch; guardar(id) }
                catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
            }
        }
    }
}

/** Una colección de pósters. */
@Composable
fun ColeccionPantalla(id: String) {
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf(0) }
    var borrar by remember { mutableStateOf(false) }
    val datos by produceState<Result<Pair<Map<String, Any?>, List<PosterCompleto>>?>?>(null, id, version) {
        value = runCatching {
            val c = Json.arr(Cuenta.api("GET", "/rest/v1/collections?select=*,profiles!owner(name)&id=eq.$id")).firstOrNull()?.asObj() ?: return@runCatching null
            c to Json.arr(Cuenta.api("GET", "/rest/v1/collection_items?select=poster_id,posters(${Galeria.SEL})&collection_id=eq.$id&order=created_at.desc")).mapNotNull { it.asObj()?.map("posters")?.let(Galeria::deFila) }
        }
    }
    val r = datos
    Box(Modifier.fillMaxSize().background(Asfalto)) {
        when {
            r == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }
            r.isFailure || r.getOrNull() == null -> Column(Modifier.padding(20.dp)) { Enlace("‹ Volver") { Nav.atras() }; Text("Esa colección no existe o es privada.", color = Tenue) }
            else -> {
                val (c, ps) = r.getOrThrow()!!
                val mia = Cuenta.uid == c.str("owner")
                LazyVerticalGrid(GridCells.Adaptive(165.dp), Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Enlace("‹ Volver", TextoClaro) { Nav.atras() }
                            Text(c.str("title"), fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
                            Text("Colección de ${c.map("profiles").str("name")} · ${ps.size} pósters" + if (!c.bool("public", true)) " · privada" else "", color = Tenue)
                            if (mia) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Boton(if (c.bool("public", true)) "Hacer privada" else "Hacer pública", principal = false) { scope.launch { runCatching { Cuenta.api("PATCH", "/rest/v1/collections?id=eq.$id", mapOf("public" to !c.bool("public", true)), "return=minimal") }; version++ } }
                                Boton("Eliminar", principal = false) { borrar = true }
                            }
                            if (ps.isEmpty()) Text("Vacía. Guarda pósters desde la galería con «Guardar en colección».", color = Tenue)
                        }
                    }
                    items(ps, key = { it.id }) { p ->
                        TarjetaPoster(p, { if (mia) Enlace("Quitar", Tenue) { scope.launch { runCatching { Cuenta.api("DELETE", "/rest/v1/collection_items?collection_id=eq.$id&poster_id=eq.${p.id}") }; version++ } } }) { Nav.ir(Destino.Poster(p.id)) }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(30.dp)) }
                }
                if (borrar) DialogoConfirmar("¿Eliminar esta colección?", "Los pósters siguen en la galería.", "Eliminar", { borrar = false }) {
                    borrar = false; scope.launch { runCatching { Cuenta.api("DELETE", "/rest/v1/collections?id=eq.$id") }; Aviso.mostrar("Colección eliminada."); Nav.atras() }
                }
            }
        }
    }
}

/** Subir un póster tuyo (o participar en un reto). */
@Composable
fun SubirPosterPantalla(reto: String?) {
    val scope = rememberCoroutineScope()
    var foto by remember { mutableStateOf<Foto?>(null) }
    var titulo by remember { mutableStateOf("") }
    var artista by remember { mutableStateOf("") }
    var disenador by remember { mutableStateOf(Cuenta.yo.str("name")) }
    var anio by remember { mutableStateOf(ahoraIso().take(4)) }
    var subiendo by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Asfalto).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Enlace("Cancelar", Tenue) { Nav.atras() }; Text(if (reto != null) "Participar en el reto" else "Subir un póster", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp)) }
        Text("Sube solo diseños tuyos o que tengas permiso de compartir. Al subirlo lo confirmas.", color = Tenue)
        val img = imagen(foto?.mini ?: "")
        Box(Modifier.fillMaxWidth().aspectRatio(.75f).clip(RoundedCornerShape(16.dp)).background(Panel).clickable { val e = elegirImagenes(1800, 520, false); scope.launch { e.esperar().firstOrNull()?.let { foto = it } } }, contentAlignment = Alignment.Center) {
            if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) else Text("Toca para elegir la imagen", color = Tenue)
        }
        Campo(titulo, { titulo = it }, "Título", max = 120)
        Campo(artista, { artista = it }, "Banda o artista", max = 80)
        Campo(disenador, { disenador = it }, "Arte de (quién lo diseñó)", max = 80)
        Campo(anio, { anio = it.filter(Char::isDigit) }, "Año", max = 4)
        Boton("Subir", ocupado = subiendo, habilitado = foto != null && titulo.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            val f = foto ?: return@Boton
            scope.launch {
                subiendo = true
                try { Aviso.mostrar("Subiendo póster…"); val id = Galeria.subir(f, titulo, artista, disenador, anio, reto); Aviso.mostrar(if (reto != null) "¡Ya participas en el reto!" else "Póster publicado en la comunidad."); Nav.atras(); if (id.isNotEmpty()) Nav.ir(Destino.Poster(id)) }
                catch (e: Throwable) { Aviso.mostrar(e.message ?: "No se pudo subir.", true) }
                subiendo = false
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

/** Buscar pósters originales con licencia libre (Wikimedia Commons) y guardarlos en la galería. */
@Composable
fun BuscarOriginalesPantalla() {
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var res by remember { mutableStateOf<List<Galeria.Original>?>(null) }
    var buscando by remember { mutableStateOf(false) }
    var guardar by remember { mutableStateOf<Galeria.Original?>(null) }
    LazyVerticalGrid(GridCells.Adaptive(165.dp), Modifier.fillMaxSize().background(Asfalto).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Enlace("‹ Volver", TextoClaro) { Nav.atras() }; Text("Buscar pósters originales", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                Text("Busco en el archivo libre de Wikimedia Commons: imágenes con licencia libre que puedes guardar en la galería con su autor y su licencia.", color = Tenue, fontSize = 14.sp)
                Campo(q, { q = it }, "Ej: Woodstock, Nirvana 1991, Soda Stereo", max = 80)
                Boton("Buscar", ocupado = buscando, habilitado = q.trim().length >= 2) {
                    scope.launch { buscando = true; res = try { Galeria.buscarCommons(q.trim()) } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true); emptyList() }; buscando = false }
                }
                res?.let { if (it.isEmpty()) Text("No encontré pósters con licencia libre para esa búsqueda. Prueba con otro nombre, un festival o un año.", color = Tenue) }
            }
        }
        items(res ?: emptyList(), key = { it.url }) { o ->
            Column(Modifier.clip(RoundedCornerShape(14.dp))) {
                val img = imagen(o.mini)
                Box(Modifier.fillMaxWidth().aspectRatio(.7f).clip(RoundedCornerShape(14.dp)).background(Panel)) { if (img != null) Image(img, o.titulo, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                Text(o.titulo, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                Text(listOf(if (o.anio > 0) o.anio.toString() else "", o.licencia).filter { it.isNotBlank() }.joinToString(" · "), color = Tenue, fontSize = 12.sp, maxLines = 1)
                Boton("Guardar", principal = false, modifier = Modifier.padding(top = 4.dp)) { if (Nav.necesitaCuenta()) guardar = o }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(30.dp)) }
    }
    guardar?.let { o ->
        var t by remember(o) { mutableStateOf(o.titulo) }; var a by remember(o) { mutableStateOf(q) }; var y by remember(o) { mutableStateOf(if (o.anio > 0) o.anio.toString() else "") }
        Hoja("Guardar póster original", { guardar = null }) {
            Text("Autor: ${o.credito.ifEmpty { "desconocido" }} · Licencia: ${o.licencia.ifEmpty { "ver ficha" }}", color = Tenue, fontSize = 14.sp)
            Campo(t, { t = it }, "Título", max = 120); Campo(a, { a = it }, "Banda o artista", max = 80); Campo(y, { y = it.filter(Char::isDigit) }, "Año", max = 4)
            Boton("Guardar en la galería", modifier = Modifier.fillMaxWidth()) {
                scope.launch { try { Galeria.guardarOriginal(o, t, a, y); Aviso.mostrar("Guardado en la galería como Original.") } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }; guardar = null }
            }
        }
    }
}

/** Retos de pósters: el reto en curso con sus participantes y votos, y los anteriores con su ganador. */
@Composable
private fun Retos() {
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf(0) }
    var nuevo by remember { mutableStateOf(false) }
    val hoy = ahoraIso().take(10)
    val datos by produceState<Result<List<Triple<Map<String, Any?>, List<PosterCompleto>, String>>>?>(null, version) {
        value = runCatching {
            val cs = Json.arr(Cuenta.api("GET", "/rest/v1/challenges?select=*&order=ends_on.desc&limit=12")).mapNotNull { it.asObj() }
            val vivos = cs.filter { it.str("ends_on") >= hoy && it.str("starts_on") <= hoy }; val pasados = cs.filter { it.str("ends_on") < hoy }.take(4)
            (vivos + pasados).map { c ->
                val ps = Json.arr(Cuenta.api("GET", "/rest/v1/posters?select=${Galeria.SEL}&challenge_id=eq.${c.str("id")}&order=created_at.asc&limit=60")).mapNotNull { it.asObj()?.let(Galeria::deFila) }
                val votos = runCatching { Json.arr(Cuenta.api("POST", "/rest/v1/rpc/rr_challenge_results", mapOf("ch" to c.str("id")))).mapNotNull { it.asObj() }.associate { it.str("poster_id") to it.num("votes").toInt() } }.getOrDefault(emptyMap())
                val mio = if (Cuenta.dentro) runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/challenge_votes?select=poster_id&challenge_id=eq.${c.str("id")}&user_id=eq.${Cuenta.uid}")).firstOrNull()?.asObj()?.str("poster_id") ?: "" }.getOrDefault("") else ""
                Triple(c, ps.map { it.copy(votos = votos[it.id] ?: 0) }, mio)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 6.dp)) {
        if (Cuenta.esAdmin) Boton("+ Nuevo reto", principal = false) { nuevo = true }
        val r = datos
        when {
            r == null -> CircularProgressIndicator(color = Amarillo)
            r.isFailure -> Text(r.exceptionOrNull()?.message ?: "", color = Tenue)
            r.getOrThrow().none { it.first.str("ends_on") >= hoy } && r.getOrThrow().isEmpty() -> Text("No hay retos todavía.", color = Tenue)
            else -> {
                if (r.getOrThrow().none { it.first.str("ends_on") >= hoy }) Text("No hay un reto activo ahora mismo.", color = Tenue)
                r.getOrThrow().forEach { (c, ps, mio) ->
                    val vivo = c.str("ends_on") >= hoy
                    val est = Estilo().conTema(c.str("aesthetic").takeIf { it in Estilo.TEMAS } ?: "asfalto")
                    val t = Temas.de(est)
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).drawBehind { fondoTema(est, t) }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text((if (vivo) "Reto en curso" else "Reto terminado") + " · ${Fechas.corta(c.str("starts_on"))} – ${Fechas.corta(c.str("ends_on"))}", color = col(t.tenue), fontSize = 13.sp)
                        Text(c.str("title"), color = t.titulo?.let(::col) ?: col(t.texto), fontFamily = Fuentes.titulo(est.ft), fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 28.sp)
                        if (c.str("description").isNotBlank()) Text(c.str("description"), color = col(t.texto))
                        if (vivo) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Boton("Participar con mi póster") { if (Nav.necesitaCuenta()) Nav.ir(Destino.SubirPoster(c.str("id"))) }
                            if (Cuenta.esAdmin) Boton("Borrar reto", principal = false) { scope.launch { runCatching { Cuenta.api("DELETE", "/rest/v1/challenges?id=eq.${c.str("id")}") }; version++ } }
                        }
                    }
                    val orden = if (vivo) ps else ps.sortedByDescending { it.votos }
                    if (orden.isEmpty()) Text(if (vivo) "Aún no hay participantes. ¡Sé el primero!" else "Nadie participó.", color = Tenue)
                    orden.chunked(2).forEachIndexed { fi, fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            fila.forEachIndexed { k, p ->
                                Box(Modifier.weight(1f)) {
                                    TarjetaPoster(p, {
                                        Column {
                                            if (!vivo && fi == 0 && k == 0 && p.votos > 0) Text("Ganador", color = Color(0xFF1B1E23), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Amarillo).padding(horizontal = 7.dp, vertical = 2.dp))
                                            Text("Arte de ${p.disenador.ifBlank { p.creadorNombre }} · ${p.votos} ${if (p.votos == 1) "voto" else "votos"}", color = Tenue, fontSize = 12.sp)
                                            if (vivo && Cuenta.uid != p.creador) Boton(if (mio == p.id) "Tu voto" else "Votar", principal = mio == p.id) {
                                                if (Nav.necesitaCuenta()) scope.launch {
                                                    try {
                                                        val ch = c.str("id")
                                                        if (mio.isNotEmpty()) Cuenta.api("PATCH", "/rest/v1/challenge_votes?challenge_id=eq.$ch&user_id=eq.${Cuenta.uid}", mapOf("poster_id" to p.id), "return=minimal")
                                                        else Cuenta.api("POST", "/rest/v1/challenge_votes", mapOf("challenge_id" to ch, "poster_id" to p.id, "user_id" to Cuenta.uid), "return=minimal")
                                                        Aviso.mostrar("¡Voto registrado!"); version++
                                                    } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
                                                }
                                            }
                                        }
                                    }) { Nav.ir(Destino.Poster(p.id)) }
                                }
                            }
                            if (fila.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
    if (nuevo) {
        var titulo by remember { mutableStateOf("") }; var desc by remember { mutableStateOf("") }; var fin by remember { mutableStateOf(hoy.take(8) + "28") }; var est by remember { mutableStateOf("") }
        Hoja("Nuevo reto", { nuevo = false }) {
            Campo(titulo, { titulo = it }, "Título", max = 120, pista = "Diseña el póster de tu banda favorita")
            Campo(desc, { desc = it }, "Descripción", lineas = 3, max = 1000)
            Campo(fin, { fin = it }, "Termina el (AAAA-MM-DD)", max = 10)
            Text("Estética", color = Tenue, fontSize = 13.sp)
            Fichas(listOf("" to "Libre") + Estilo.TEMAS.drop(6).map { it to (Estilo.NOMBRES_TEMA[it] ?: it) }, est) { est = it }
            Boton("Crear reto", habilitado = titulo.trim().length >= 3 && Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(fin.trim()), modifier = Modifier.fillMaxWidth()) {
                scope.launch {
                    try { Cuenta.api("POST", "/rest/v1/challenges", mapOf("title" to titulo.trim().take(120), "description" to desc.trim().take(1000).ifEmpty { null }, "aesthetic" to est.ifEmpty { null }, "starts_on" to hoy, "ends_on" to fin.trim(), "created_by" to Cuenta.uid), "return=minimal"); Aviso.mostrar("Reto creado."); nuevo = false; version++ }
                    catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
                }
            }
        }
    }
}
