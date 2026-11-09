package rr.revista

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/* Funciones que tenía la web: guardar sin conexión, «Hoy en la música», ediciones mensuales,
   comparar pósters, editar conciertos de la Bóveda y la ayuda de IA del editor. */

// ---------------------------------------------------------------- Guardados sin conexión
object Guardados {
    private const val CLAVE = "rr_guardados_v1"
    var ids by mutableStateOf(lista().map { it.str("id") }.toSet()); private set

    private fun lista(): List<Map<String, Any?>> = Json.arr(leerDato(CLAVE) ?: "[]").mapNotNull { it.asObj() }

    fun esta(id: String) = id in ids

    /** Guarda el artículo completo (texto y datos) para leerlo sin internet. Como mucho 30. */
    suspend fun guardar(id: String) {
        val fila = Json.arr(Cuenta.api("GET", "/rest/v1/posts?select=*,profiles!author_id(name,avatar),likes(count),comments(count)&id=eq.$id")).firstOrNull()?.asObj() ?: throw ErrorRevista("No encontré el artículo.")
        val nueva = (listOf(fila + ("_guardado" to ahoraIso())) + lista().filter { it.str("id") != id }).take(30)
        guardarDato(CLAVE, Json.write(nueva)); ids = nueva.map { it.str("id") }.toSet()
    }

    fun quitar(id: String) { val n = lista().filter { it.str("id") != id }; guardarDato(CLAVE, Json.write(n)); ids = n.map { it.str("id") }.toSet() }

    fun leer(id: String): Articulo? = lista().firstOrNull { it.str("id") == id }?.let { Supa.deFila(it) }
    fun todos(): List<Articulo> = lista().map { Supa.deFila(it) }
}

@Composable
fun GuardadosPantalla() {
    val lista = remember(Guardados.ids) { Guardados.todos() }
    LazyVerticalGrid(GridCells.Adaptive(330.dp), Modifier.fillMaxSize().background(Asfalto), contentPadding = PaddingValues(bottom = 30.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)) {
                Enlace("‹ Volver", TextoClaro) { Nav.atras() }
                Text("Guardados", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
                Text("Artículos que puedes leer sin internet en este dispositivo (hasta 30).", color = Tenue)
            }
        }
        if (lista.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Text("Aún no guardas nada. Abre un artículo y toca «Guardar».", color = Tenue, modifier = Modifier.padding(16.dp)) }
        items(lista, key = { it.id }) { a -> Tarjeta(a) { Nav.ir(Destino.Leer(a.id)) } }
    }
}

// ---------------------------------------------------------------- Hoy en la música
private data class Efemeride(val anio: Int, val texto: String, val url: String)

/** Lo que pasó en la música un día como hoy (Wikipedia en español), guardado un día. */
@Composable
fun HoyEnLaMusica(modifier: Modifier = Modifier) {
    val hoy = ahoraIso().take(10)
    val lista by produceState<List<Efemeride>?>(null, hoy) {
        val clave = "rr_hoy_m_" + hoy
        val guardado = leerDato(clave)
        val texto = guardado ?: runCatching {
            val mm = hoy.substring(5, 7); val dd = hoy.substring(8, 10)
            val r = httpPedido("GET", "https://api.wikimedia.org/feed/v1/wikipedia/es/onthisday/events/$mm/$dd", emptyMap(), null)
            if (r.estado !in 200..299) "[]" else {
                val mus = Regex("\\b(banda|grupo musical|cantante|m[uú]sico|compositor|[aá]lbum|disco|sencillo|canci[oó]n|concierto|festival|gira|rock|jazz|punk|pop|orquesta|guitarrista|baterista|bajista|rapero|reguet[oó]n|salsa|cumbia)\\b", RegexOption.IGNORE_CASE)
                val ev = (Json.obj(r.texto)["events"] as? List<*>).orEmpty().mapNotNull { it.asObj() }.filter { mus.containsMatchIn(it.str("text")) && !Regex("terrorist|militar|ej[eé]rcito|guerra|atentado|asesin|pol[ií]tic|presos", RegexOption.IGNORE_CASE).containsMatchIn(it.str("text")) }.take(4)
                Json.write(ev.map { e ->
                    val pag = (e["pages"] as? List<*>)?.firstOrNull()?.asObj()
                    mapOf("y" to e.num("year"), "t" to e.str("text").take(220), "u" to (pag?.map("content_urls")?.map("mobile")?.str("page") ?: ""))
                })
            }
        }.getOrDefault("[]")
        if (guardado == null) guardarDato(clave, texto)
        value = Json.arr(texto).mapNotNull { it.asObj() }.map { Efemeride(it.num("y").toInt(), it.str("t"), it.str("u")) }
    }
    val l = lista ?: return
    if (l.isEmpty()) return
    Column(modifier.padding(horizontal = 18.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("HOY EN LA MÚSICA · ${Fechas.corta(hoy + "T12:00:00").uppercase()}", color = Amarillo, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        l.forEach { e ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).then(if (e.url.isNotEmpty()) Modifier.mano().clickable { abrirEnlace(e.url) } else Modifier).padding(vertical = 2.dp)) {
                Text("${e.anio}", fontWeight = FontWeight.ExtraBold, color = Amarillo, modifier = Modifier.width(52.dp))
                Text(e.texto, fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ---------------------------------------------------------------- Ediciones mensuales
@Composable
fun EdicionesPantalla() {
    val datos by produceState<Result<List<Map<String, Any?>>>?>(null) {
        value = runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/editions?select=id,title,intro,issue_month,cover,style,published&order=issue_month.desc&limit=24")).mapNotNull { it.asObj() }.filter { it.bool("published", true) || Cuenta.esAdmin } }
    }
    LazyVerticalGrid(GridCells.Adaptive(300.dp), Modifier.fillMaxSize().background(Asfalto), contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 30.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(8.dp, 12.dp, 8.dp, 4.dp)) {
                Enlace("‹ Volver", TextoClaro) { Nav.atras() }
                Text("Ediciones", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
                Text("Cada mes, lo mejor de la Revista reunido como en una revista impresa.", color = Tenue)
            }
        }
        val r = datos
        when {
            r == null -> item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) } }
            r.isFailure -> item(span = { GridItemSpan(maxLineSpan) }) { Text(r.exceptionOrNull()?.message ?: "", color = Tenue, modifier = Modifier.padding(8.dp)) }
            r.getOrThrow().isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) { Text("Todavía no hay ediciones publicadas.", color = Tenue, modifier = Modifier.padding(8.dp)) }
            else -> items(r.getOrThrow(), key = { it.str("id") }) { e ->
                val a = Articulo(e.str("id"), e.str("title"), Fechas.mesAnio(e.str("issue_month") + "T12:00:00"), Diseno.imagenOk(e.str("cover")),
                    Estilo.de(mapOf("cv" to "m", "th" to "clasica", "ft" to "editorial", "ac" to "rojo") + e.map("style")), e.str("issue_month") + "T12:00:00", Autor("", "Radio de ruta", ""), 0, 0)
                Box(Modifier.padding(8.dp)) { Tarjeta(a) { Nav.ir(Destino.Edicion(e.str("id"))) } }
            }
        }
    }
}

@Composable
fun EdicionPantalla(id: String) {
    val datos by produceState<Result<Triple<Map<String, Any?>, List<Articulo>, List<PosterCompleto>>?>?>(null, id) {
        value = runCatching {
            val e = Json.arr(Cuenta.api("GET", "/rest/v1/editions?select=*&id=eq.$id")).firstOrNull()?.asObj() ?: return@runCatching null
            val items = (e["items"] as? List<*>).orEmpty().mapNotNull { it.asObj() }
            val pid = items.filter { it.str("t") == "post" }.map { it.str("id") }.filter { Regex("^[0-9a-f-]{36}$").matches(it) }
            val poid = items.filter { it.str("t") == "poster" }.map { it.str("id") }.filter { Regex("^[0-9a-f-]{36}$").matches(it) }
            val ps = if (pid.isEmpty()) emptyList() else Json.arr(Cuenta.api("GET", "/rest/v1/posts?select=id,title,subtitle,cover,style,review,tags,created_at,author_id,profiles!author_id(name,avatar),likes(count),comments(count)&id=in.(${pid.joinToString(",")})")).mapNotNull { it.asObj()?.let(Supa::deFila) }
            val po = poid.mapNotNull { runCatching { Galeria.uno(it) }.getOrNull() }
            Triple(e, pid.mapNotNull { i -> ps.firstOrNull { it.id == i } }, po)
        }
    }
    val r = datos
    LazyVerticalGrid(GridCells.Adaptive(300.dp), Modifier.fillMaxSize().background(Asfalto), contentPadding = PaddingValues(bottom = 30.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.padding(16.dp, 12.dp)) { Enlace("‹ Ediciones", TextoClaro) { Nav.atras() } } }
        when {
            r == null -> item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) } }
            r.isFailure || r.getOrNull() == null -> item(span = { GridItemSpan(maxLineSpan) }) { Text(r.exceptionOrNull()?.message ?: "Esa edición no existe.", color = Tenue, modifier = Modifier.padding(16.dp)) }
            else -> {
                val (e, arts, posters) = r.getOrThrow()!!
                val portada = Articulo(e.str("id"), e.str("title"), Fechas.mesAnio(e.str("issue_month") + "T12:00:00"), Diseno.imagenOk(e.str("cover")),
                    Estilo.de(mapOf("cv" to "m", "th" to "clasica", "ft" to "editorial", "ac" to "rojo") + e.map("style")), e.str("issue_month") + "T12:00:00", Autor("", "Radio de ruta", ""), 0, 0)
                item(span = { GridItemSpan(maxLineSpan) }) { CabeceraEstilo(portada) }
                if (e.str("intro").isNotBlank()) item(span = { GridItemSpan(maxLineSpan) }) { Text(e.str("intro"), fontSize = 17.sp, lineHeight = 25.sp, modifier = Modifier.padding(18.dp)) }
                if (arts.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("EN ESTA EDICIÓN", color = Amarillo, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 13.sp, modifier = Modifier.padding(start = 18.dp, top = 8.dp)) }
                    items(arts, key = { "a" + it.id }) { a -> Tarjeta(a) { Nav.ir(Destino.Leer(a.id)) } }
                }
                if (posters.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("PÓSTERS DE LA EDICIÓN", color = Amarillo, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 13.sp, modifier = Modifier.padding(start = 18.dp, top = 8.dp)) }
                    items(posters, key = { "p" + it.id }) { p -> Box(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) { TarjetaPoster(p) { Nav.ir(Destino.Poster(p.id)) } } }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Comparar pósters del mismo artista
@Composable
fun CompararPantalla(artista: String) {
    val datos by produceState<Result<List<PosterCompleto>>?>(null, artista) { value = runCatching { Galeria.lista("all", artista, 0) } }
    LazyVerticalGrid(GridCells.Adaptive(165.dp), Modifier.fillMaxSize().background(Asfalto), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Enlace("‹ Volver", TextoClaro) { Nav.atras() }
                Text("Pósters de $artista", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 30.sp)
                Text("Compara los carteles de distintas giras y años, lado a lado.", color = Tenue)
            }
        }
        val r = datos
        when {
            r == null -> item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) } }
            r.isFailure -> item(span = { GridItemSpan(maxLineSpan) }) { Text(r.exceptionOrNull()?.message ?: "", color = Tenue) }
            else -> items(r.getOrThrow().sortedBy { if (it.anio > 0) it.anio else 9999 }, key = { it.id }) { p -> TarjetaPoster(p) { Nav.ir(Destino.Poster(p.id)) } }
        }
    }
}

// ---------------------------------------------------------------- Editar un concierto de la Bóveda (editores)
/** De un enlace de YouTube, Vimeo o Internet Archive saca el proveedor y el código del video. */
fun videoDeEnlace(u: String): Pair<String, String>? {
    val t = u.trim()
    Regex("(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|shorts/|embed/|live/)|youtu\\.be/)([\\w-]{11})").find(t)?.let { return "youtube" to it.groupValues[1] }
    Regex("vimeo\\.com/(?:video/)?(\\d{5,12})").find(t)?.let { return "vimeo" to it.groupValues[1] }
    Regex("archive\\.org/(?:details|embed)/([\\w.-]{3,100})").find(t)?.let { return "archive" to it.groupValues[1] }
    return null
}

@Composable
fun EditarConciertoPantalla(id: String?) {
    val scope = rememberCoroutineScope()
    var titulo by remember { mutableStateOf("") }
    var artista by remember { mutableStateOf("") }
    var festival by remember { mutableStateOf("") }
    var anio by remember { mutableStateOf("") }
    var lugar by remember { mutableStateOf("") }
    var enlace by remember { mutableStateOf("") }
    var sinopsis by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    var ciclo by remember { mutableStateOf("") }
    var fuente by remember { mutableStateOf("") }
    var oficial by remember { mutableStateOf(false) }
    var oculto by remember { mutableStateOf(false) }
    var desde by remember { mutableStateOf(ahoraIso().take(10)) }
    var dias by remember { mutableStateOf("30") }
    var guardando by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf(false) }
    val listo by produceState(id == null, id) {
        if (id != null) runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/vault?select=*&id=eq.$id")).firstOrNull()?.asObj() }.getOrNull()?.let { v ->
            titulo = v.str("title"); artista = v.str("artist"); festival = v.str("festival"); anio = v.num("year").takeIf { it > 0 }?.toString() ?: ""; lugar = v.str("place")
            enlace = when (v.str("provider")) { "youtube" -> "https://youtu.be/" + v.str("video_id"); "vimeo" -> "https://vimeo.com/" + v.str("video_id"); else -> "https://archive.org/details/" + v.str("video_id") }
            sinopsis = v.str("synopsis"); nota = v.str("note"); ciclo = v.str("cycle"); fuente = v.str("source_name"); oficial = v.bool("official"); oculto = v.bool("hidden")
            desde = v.str("starts_on").take(10).ifEmpty { desde }; dias = v.num("days", 30).toString()
        }
        value = true
    }
    if (!Cuenta.esEditor) { Column(Modifier.fillMaxSize().background(Asfalto).padding(20.dp)) { Enlace("‹ Volver", TextoClaro) { Nav.atras() }; Text("Solo editores y el administrador pueden curar la Bóveda.", color = Tenue) }; return }
    Column(Modifier.fillMaxSize().background(Asfalto).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Enlace("‹ Cancelar", TextoClaro) { Nav.atras() }; Spacer(Modifier.weight(1f)); if (id != null) Enlace("Eliminar", Color(0xFFFF8A7A)) { borrar = true } }
        Text(if (id == null) "Nuevo concierto" else "Editar concierto", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        if (!listo) { CircularProgressIndicator(color = Amarillo); return@Column }
        Campo(titulo, { titulo = it }, "Título", max = 140, pista = "Ej: Soda Stereo en Viña del Mar 1987")
        Campo(enlace, { enlace = it }, "Enlace del video", max = 300, pista = "YouTube, Vimeo o Internet Archive")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Campo(artista, { artista = it }, "Artista", max = 100) }
            Box(Modifier.weight(1f)) { Campo(anio, { anio = it.filter(Char::isDigit) }, "Año", max = 4) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Campo(festival, { festival = it }, "Festival (opcional)", max = 100) }
            Box(Modifier.weight(1f)) { Campo(lugar, { lugar = it }, "Lugar (opcional)", max = 120) }
        }
        Campo(sinopsis, { sinopsis = it }, "Sinopsis", lineas = 3, max = 1500)
        Campo(nota, { nota = it }, "Nota del curador (opcional)", lineas = 3, max = 3000)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Campo(ciclo, { ciclo = it }, "Ciclo (opcional)", max = 60, pista = "Ej: Rock en español") }
            Box(Modifier.weight(1f)) { Campo(fuente, { fuente = it }, "Fuente (opcional)", max = 100) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Campo(desde, { desde = it }, "En cartelera desde (AAAA-MM-DD)", max = 10) }
            Box(Modifier.weight(1f)) { Campo(dias, { dias = it.filter(Char::isDigit) }, "Días en cartelera", max = 3) }
        }
        CasillaExtra(oficial, "Video oficial (subido por el artista o el sello)") { oficial = it }
        CasillaExtra(oculto, "Oculto (no aparece en la Bóveda)") { oculto = it }
        Boton(if (id == null) "Publicar en la Bóveda" else "Guardar cambios", ocupado = guardando, modifier = Modifier.fillMaxWidth()) {
            val v = videoDeEnlace(enlace)
            when {
                titulo.trim().length < 3 -> { Aviso.mostrar("El título debe tener al menos 3 letras.", true); return@Boton }
                v == null -> { Aviso.mostrar("Pega un enlace de YouTube, Vimeo o Internet Archive.", true); return@Boton }
                !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(desde.trim()) -> { Aviso.mostrar("La fecha debe ser AAAA-MM-DD.", true); return@Boton }
            }
            val b = linkedMapOf<String, Any?>("title" to titulo.trim(), "artist" to artista.trim().ifEmpty { null }, "festival" to festival.trim().ifEmpty { null },
                "year" to anio.toIntOrNull()?.takeIf { it in 1900..2100 }, "place" to lugar.trim().ifEmpty { null }, "provider" to v!!.first, "video_id" to v.second,
                "synopsis" to sinopsis.trim().ifEmpty { null }, "note" to nota.trim().ifEmpty { null }, "cycle" to ciclo.trim().ifEmpty { null }, "source_name" to fuente.trim().ifEmpty { null },
                "official" to oficial, "hidden" to oculto, "starts_on" to desde.trim(), "days" to (dias.toIntOrNull() ?: 30).coerceIn(1, 365))
            scope.launch {
                guardando = true
                try {
                    if (id == null) Cuenta.api("POST", "/rest/v1/vault", b + ("created_by" to Cuenta.uid), "return=minimal") else Cuenta.api("PATCH", "/rest/v1/vault?id=eq.$id", b, "return=minimal")
                    Aviso.mostrar("Guardado en la Bóveda."); Nav.atras()
                } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
                guardando = false
            }
        }
        Spacer(Modifier.height(30.dp))
    }
    if (borrar && id != null) DialogoConfirmar("¿Eliminar este concierto?", "Se borra de la Bóveda con sus comentarios.", "Eliminar", { borrar = false }) {
        borrar = false
        scope.launch { try { Cuenta.api("DELETE", "/rest/v1/vault?id=eq.$id"); Aviso.mostrar("Concierto eliminado."); Nav.inicio() } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } }
    }
}

@Composable
private fun CasillaExtra(v: Boolean, texto: String, cambiar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { cambiar(!v) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(v, cambiar, colors = CheckboxDefaults.colors(checkedColor = Amarillo, checkmarkColor = Color(0xFF1B1E23)))
        Text(texto, modifier = Modifier.weight(1f))
    }
}

// ---------------------------------------------------------------- Ayuda de IA en el editor
/** Botones «corregir ortografía», «sugerir títulos» y «resumir en subtítulo» (la IA no escribe por ti). */
@Composable
fun AyudaIA(titulo: String, texto: String, ponerTexto: (String) -> Unit, ponerTitulo: (String) -> Unit, ponerSubtitulo: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var pensando by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf<Pair<String, String>?>(null) }
    fun pedir(tipo: String) {
        if (texto.trim().length < 40) { Aviso.mostrar("Escribe un poco más primero (la IA no escribe por ti)."); return }
        val p = when (tipo) {
            "fix" -> "Corrige SOLO ortografía, acentos y puntuación de este texto en español. No cambies palabras, estilo, orden ni el formato (**, *, ##, >, -). Devuelve únicamente el texto corregido.\n\n$texto"
            "titles" -> "Propón 5 títulos cortos y atractivos para este artículo de música, uno por línea, sin numerar ni comillas.\n\n$titulo\n${texto.take(4000)}"
            else -> "Escribe un subtítulo de máximo 140 caracteres que resuma este artículo de música. Devuelve solo el subtítulo.\n\n$titulo\n${texto.take(4000)}"
        }
        scope.launch {
            pensando = tipo
            try { resultado = tipo to pedirIA(p) } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
            pensando = ""
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("Ayuda de tu IA:", color = Tenue, fontSize = 14.sp)
        listOf("fix" to "corregir ortografía", "titles" to "sugerir títulos", "sum" to "resumir").forEach { (k, n) ->
            if (pensando == k) CircularProgressIndicator(Modifier.padding(horizontal = 8.dp).width(16.dp).height(16.dp), color = Amarillo, strokeWidth = 2.dp)
            else Enlace(n) { if (pensando.isEmpty()) pedir(k) }
        }
    }
    resultado?.let { (tipo, out) ->
        Hoja(when (tipo) { "fix" -> "Corrección"; "titles" -> "Títulos sugeridos"; else -> "Subtítulo sugerido" }, { resultado = null }) {
            when (tipo) {
                "titles" -> out.lines().map { it.trim().trim('"', '«', '»', '-', '•', ' ') }.filter { it.isNotEmpty() }.take(6).forEach { t ->
                    Text(t, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x14FFFFFF)).mano().clickable { ponerTitulo(t.take(120)); resultado = null; Aviso.mostrar("Título puesto.") }.padding(12.dp))
                }
                else -> {
                    if (tipo == "fix" && out.trim() == texto.trim()) Text("No encontré errores.", color = Tenue)
                    else Text(out.take(1500) + if (out.length > 1500) "…" else "", fontSize = 15.sp, lineHeight = 21.sp, maxLines = 30, overflow = TextOverflow.Ellipsis)
                    Boton("Aplicar") { if (tipo == "fix") ponerTexto(out) else ponerSubtitulo(out.take(140)); resultado = null; Aviso.mostrar("Listo.") }
                }
            }
        }
    }
}
