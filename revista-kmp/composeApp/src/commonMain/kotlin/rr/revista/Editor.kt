package rr.revista

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Lo que estás escribiendo (también se guarda como borrador en el teléfono). */
private data class Borrador(
    val titulo: String = "", val subtitulo: String = "", val portada: Foto? = null, val portadaUrl: String = "", val texto: String = "",
    val estilo: Estilo = Estilo(), val diseno: Diseno = Diseno(), val galeria: List<Foto> = emptyList(), val galeriaUrls: List<String> = emptyList(),
    val etiquetas: String = "", val resena: Boolean = false, val disco: String = "", val artista: String = "", val anio: String = "", val nota: Float = 7f
)

private const val CLAVE_BORRADOR = "rr_rev_draft_nativa"

private fun borradorAJson(b: Borrador): String = Json.write(linkedMapOf(
    "title" to b.titulo, "subtitle" to b.subtitulo, "body" to b.texto, "cover" to b.portadaUrl, "st" to b.estilo.aMapa(), "dz" to b.diseno.aMapa(),
    "gallery" to b.galeriaUrls, "tags" to b.etiquetas, "rv" to b.resena, "album" to b.disco, "artist" to b.artista, "year" to b.anio, "score" to b.nota.toInt()))

private fun borradorDeJson(t: String?): Borrador? {
    val o = Json.obj(t); if (o.isEmpty()) return null
    return Borrador(o.str("title"), o.str("subtitle"), null, Diseno.imagenOk(o.str("cover")), o.str("body"), Estilo.de(o.map("st")), Diseno.de(o.map("dz")),
        emptyList(), o.list("gallery").mapNotNull { (it as? String)?.let(Diseno::imagenOk)?.takeIf { u -> u.isNotEmpty() } }, o.str("tags"),
        o.bool("rv"), o.str("album"), o.str("artist"), o.str("year"), o.num("score", 7).toFloat())
}

/** Etiquetas como en la web: minúsculas, sin signos, hasta 5. */
fun etiquetas(s: String): List<String> = s.lowercase().split(Regex("[\\s,#]+")).map { it.replace(Regex("[^a-z0-9ñáéíóúü]"), "").take(24) }.filter { it.isNotEmpty() }.distinct().take(5)

/** Sube una foto (grande + miniatura «-t.jpg») a tu carpeta del almacén de la Revista. */
suspend fun subirFoto(f: Foto, prefijo: String): String {
    val uid = Cuenta.uid; Cuenta.api("GET", "/rest/v1/profiles?select=id&id=eq.$uid") // renueva la sesión si hace falta
    val s = Cuenta.sesion ?: throw ErrorRevista("Entra a tu cuenta para subir imágenes.")
    val nombre = "$prefijo-" + ahoraMs().toLong().toString(36) + Random.nextLong(1_000_000_000L).toString(36)
    val cab = mapOf("apikey" to Supa.CLAVE, "Authorization" to "Bearer " + s.access, "x-upsert" to "false")
    val r = subirArchivo("${Supa.URL}/storage/v1/object/revista/$uid/$nombre.jpg", cab, f.grande.substringAfter(','), "image/jpeg")
    if (r.estado !in 200..299) { val o = Json.obj(r.texto); throw ErrorRevista(Cuenta.traducir(o.str("message").ifBlank { o.str("error") }.ifBlank { "No se pudo subir la imagen." })) }
    runCatching { subirArchivo("${Supa.URL}/storage/v1/object/revista/$uid/$nombre-t.jpg", cab, f.mini.substringAfter(','), "image/jpeg") } // la miniatura es opcional
    return "${Supa.URL}/storage/v1/object/public/revista/$uid/$nombre.jpg"
}

/** Escribir o editar un artículo, con estética, diseño avanzado, collage, galería, reseña y etiquetas. */
@Composable
fun Editor(id: String?) {
    val scope = rememberCoroutineScope()
    var b by remember { mutableStateOf(Borrador()) }
    var cuerpo by remember { mutableStateOf(TextFieldValue("")) }
    var cargando by remember { mutableStateOf(id != null) }
    var guardando by remember { mutableStateOf(false) }
    var vista by remember { mutableStateOf(false) }
    var collage by remember { mutableStateOf(false) }
    var original by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var pestana by remember { mutableStateOf("estetica") }

    LaunchedEffect(id) {
        if (id == null && hashInicial().startsWith("#escribir-")) { // demostración para las capturas
            Ejemplos.uno("ejemplo-2")?.let { e -> b = Borrador(e.titulo, e.subtitulo, null, "", e.texto, e.estilo, e.diseno); cuerpo = TextFieldValue(e.texto) }
            if (hashInicial() == "#escribir-collage") collage = true
            if (hashInicial() == "#escribir-fondo") pestana = "fondo"
            return@LaunchedEffect
        }
        if (id == null) { borradorDeJson(leerDato(CLAVE_BORRADOR))?.let { b = it; cuerpo = TextFieldValue(it.texto) }; return@LaunchedEffect }
        try {
            val o = Json.arr(Cuenta.api("GET", "/rest/v1/posts?select=*&id=eq.$id")).firstOrNull()?.asObj() ?: throw ErrorRevista("Ese artículo ya no existe.")
            original = o
            val rv = o.map("review")
            b = Borrador(o.str("title"), o.str("subtitle"), null, Diseno.imagenOk(o.str("cover")), o.str("body"), Estilo.de(o.map("style")), Diseno.de(o.map("design")),
                emptyList(), o.list("gallery").mapNotNull { (it as? String)?.let(Diseno::imagenOk)?.takeIf { u -> u.isNotEmpty() } }, o.list("tags").mapNotNull { it as? String }.joinToString(", "),
                rv.isNotEmpty(), rv.str("album"), rv.str("artist"), rv.str("year").takeIf { it != "0" } ?: "", rv.num("score", 7).toFloat())
            cuerpo = TextFieldValue(b.texto)
        } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
        cargando = false
    }
    // Borrador automático (solo para artículos nuevos; las fotos sin subir no se guardan)
    LaunchedEffect(b, cuerpo.text) { if (id == null && !cargando) guardarDato(CLAVE_BORRADOR, borradorAJson(b.copy(texto = cuerpo.text))) }

    fun publicar(borrador: Boolean) {
        val titulo = b.titulo.trim(); val texto = cuerpo.text.trim()
        if (titulo.length < 3) { Aviso.mostrar("Escribe un título (mínimo 3 letras).", true); return }
        if (texto.length < 20) { Aviso.mostrar("El texto es muy corto (mínimo 20 letras).", true); return }
        if (b.resena && (b.disco.isBlank())) { Aviso.mostrar("Completa la reseña: el nombre del disco.", true); return }
        scope.launch {
            guardando = true
            try {
                val portada = b.portada?.let { Aviso.mostrar("Subiendo imagen…"); subirFoto(it, "c") } ?: b.portadaUrl.ifEmpty { null }
                val gal = b.galeriaUrls + b.galeria.map { Aviso.mostrar("Subiendo fotos…"); subirFoto(it, "g") }
                // Las fotos del collage que todavía están en el teléfono también se suben
                val col = b.diseno.col.map { i -> if (i.k == "img" && i.s.startsWith("data:")) i.copy(s = subirFoto(Foto(i.s, i.s), "k")) else i }
                val dz = b.diseno.copy(col = col)
                val fila = linkedMapOf<String, Any?>("title" to titulo.take(150), "subtitle" to b.subtitulo.trim().take(200).ifEmpty { null }, "cover" to portada, "body" to texto.take(20000),
                    "style" to b.estilo.aMapa(), "tags" to Hashtags.juntar(etiquetas(b.etiquetas), texto), "gallery" to gal.take(8),
                    "status" to if (borrador) "draft" else "published", "publish_at" to ahoraIsoMenos(60))
                if (id == null) fila["tracks"] = emptyList<Any>()
                fila["review"] = if (b.resena) linkedMapOf("album" to b.disco.trim().take(100), "artist" to b.artista.trim().take(80), "year" to b.anio.trim().toIntOrNull()?.takeIf { it in 1900..2100 }, "score" to b.nota.toInt().coerceIn(1, 10)) else null
                val nuevo = if (id != null) { Cuenta.api("PATCH", "/rest/v1/posts?id=eq.$id", fila, "return=minimal"); id }
                else Json.arr(Cuenta.api("POST", "/rest/v1/posts", fila + ("author_id" to Cuenta.uid), "return=representation")).firstOrNull()?.asObj()?.str("id") ?: throw ErrorRevista("No se pudo publicar.")
                // El diseño avanzado va en su propia columna (si la base todavía no la tiene, el artículo igual queda guardado)
                val dm = dz.aMapa()
                if (dm.isNotEmpty() || original.map("design").isNotEmpty()) runCatching { Cuenta.api("PATCH", "/rest/v1/posts?id=eq.$nuevo", mapOf("design" to dm), "return=minimal") }
                    .onFailure { Aviso.mostrar("El texto se guardó, pero el diseño no: falta ejecutar revista_supabase_v6.sql en Supabase.", true) }
                if (id == null) guardarDato(CLAVE_BORRADOR, null)
                Aviso.mostrar(if (borrador) "Borrador guardado. Solo tú lo ves." else if (id != null) "Cambios guardados." else "¡Artículo publicado!")
                Nav.atras(); Nav.ir(Destino.Leer(nuevo))
            } catch (e: Throwable) { Aviso.mostrar(e.message ?: "No se pudo guardar.", true) }
            guardando = false
        }
    }

    Box(Modifier.fillMaxSize().background(Asfalto)) {
        if (cargando) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }; return@Box }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Color(0xF21B1E23)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Enlace("Cancelar", Tenue) { Nav.atras() }
                Text(if (id == null) "Nuevo artículo" else "Editar artículo", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
                Enlace("Vista previa") { b = b.copy(texto = cuerpo.text); vista = true }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Campo(b.titulo, { b = b.copy(titulo = it) }, "Título", max = 150)
                Campo(b.subtitulo, { b = b.copy(subtitulo = it) }, "Subtítulo (opcional)", max = 200)

                Titulito("Imagen de portada")
                val img = imagen(b.portada?.mini ?: b.portadaUrl)
                if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Boton(if (img == null) "Elegir imagen" else "Cambiar", principal = false) { val e = elegirImagenes(1400, 640, false); scope.launch { e.esperar().firstOrNull()?.let { EditorFotos.abrir(FotoElegida(it).apply { formato = "16x9" }) { listo -> b = b.copy(portada = listo, portadaUrl = "") } } } }
                    if (img != null) Boton("Quitar", principal = false) { b = b.copy(portada = null, portadaUrl = "") }
                }

                Titulito("Cómo se ve")
                Pestanas(listOf("estetica" to "Estética", "fondo" to "Fondo", "letra" to "Letra", "panel" to "Transparencia", "collage" to "Collage"), pestana) { pestana = it }
                when (pestana) {
                    "estetica" -> SelectorEstetica(b.estilo) { b = b.copy(estilo = it) }
                    "fondo" -> SelectorFondo(b.diseno, b.portada != null || b.portadaUrl.isNotEmpty()) { b = b.copy(diseno = it) }
                    "letra" -> SelectorLetra(b.diseno) { b = b.copy(diseno = it) }
                    "panel" -> SelectorPanel(b.diseno) { b = b.copy(diseno = it) }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Pega recortes, fotos, letras de revista, stickers y cintas. Arrástralos con el dedo; con dos dedos los agrandas y giras.", color = Tenue, fontSize = 14.sp)
                        Fichas(listOf("fondo" to "Collage de fondo", "cabecera" to "Collage arriba del artículo"), b.diseno.cpos) { b = b.copy(diseno = b.diseno.copy(cpos = it)) }
                        if (b.diseno.cpos == "cabecera") Fichas(listOf("vertical" to "Vertical", "cuadrado" to "Cuadrado", "horizontal" to "Horizontal"), b.diseno.ch) { b = b.copy(diseno = b.diseno.copy(ch = it)) }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Boton("Abrir modo collage" + if (b.diseno.col.isNotEmpty()) " (${b.diseno.col.size})" else "") { collage = true }
                            if (b.diseno.col.isNotEmpty()) Boton("Borrar collage", principal = false) { b = b.copy(diseno = b.diseno.copy(col = emptyList())) }
                        }
                    }
                }

                // Reseña de disco
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { b = b.copy(resena = !b.resena) }) {
                    Checkbox(b.resena, { b = b.copy(resena = it) }, colors = CheckboxDefaults.colors(checkedColor = Amarillo, checkmarkColor = Color(0xFF1B1E23)))
                    Text("Este artículo es una reseña de disco", fontWeight = FontWeight.SemiBold)
                }
                if (b.resena) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Campo(b.disco, { b = b.copy(disco = it) }, "Disco", max = 100)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(2f)) { Campo(b.artista, { b = b.copy(artista = it) }, "Artista", max = 80) }
                        Box(Modifier.weight(1f)) { Campo(b.anio, { b = b.copy(anio = it.filter(Char::isDigit)) }, "Año", max = 4) }
                    }
                    Text("Puntuación: ${b.nota.toInt()}/10", fontWeight = FontWeight.Bold)
                    Slider(b.nota, { b = b.copy(nota = it) }, valueRange = 1f..10f, steps = 8, colors = SliderDefaults.colors(thumbColor = Amarillo, activeTrackColor = Amarillo))
                }

                Titulito("Texto")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("b" to "N", "i" to "K", "h" to "Título", "q" to "Cita", "l" to "Lista").forEach { (k, n) ->
                        Text(n, fontWeight = if (k == "b") FontWeight.ExtraBold else FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x14FFFFFF)).clickable { cuerpo = formato(cuerpo, k) }.padding(horizontal = 14.dp, vertical = 9.dp))
                    }
                }
                OutlinedTextField(cuerpo, { if (it.text.length <= 20000) cuerpo = it }, minLines = 10, placeholder = { Text("Escribe tu reseña, tu lista o tu historia musical…", color = Tenue) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Amarillo, cursorColor = Amarillo, unfocusedBorderColor = Color(0x33FFFFFF)), modifier = Modifier.fillMaxWidth())
                AyudaIA(b.titulo, cuerpo.text, { cuerpo = TextFieldValue(it) }, { b = b.copy(titulo = it) }, { b = b.copy(subtitulo = it) })
                Text("Deja una línea en blanco entre párrafos. **negrita** · *cursiva* · «## » título · «> » cita · «- » lista. Pega un enlace de YouTube solo en un párrafo para que se vea el video.", color = Tenue, fontSize = 13.sp)

                Titulito("Galería de fotos (hasta 8)")
                val total = b.galeriaUrls.size + b.galeria.size
                if (total > 0) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    b.galeriaUrls.forEach { u -> MiniFoto(Supa.miniatura(u)) { b = b.copy(galeriaUrls = b.galeriaUrls - u) } }
                    b.galeria.forEach { f -> MiniFoto(f.mini) { b = b.copy(galeria = b.galeria - f) } }
                }
                if (total < 8) Boton("Añadir fotos", principal = false) { val e = elegirImagenes(1600, 520, true); scope.launch { val fs = e.esperar(); b = b.copy(galeria = (b.galeria + fs).take(8 - b.galeriaUrls.size)) } }

                Campo(b.etiquetas, { b = b.copy(etiquetas = it) }, "Etiquetas (hasta 5)", max = 140, pista = "grunge, en vivo, reseña · o escribe #palabra en el texto")

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Boton("Guardar borrador", principal = false, ocupado = guardando, modifier = Modifier.weight(1f)) { publicar(true) }
                    Boton(if (id != null && original.str("status") != "draft") "Guardar cambios" else "Publicar", ocupado = guardando, modifier = Modifier.weight(1f)) { publicar(false) }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
        if (vista) {
            val a = Articulo("ejemplo-vista", b.titulo.ifBlank { "Tu título" }, b.subtitulo, b.portada?.grande ?: b.portadaUrl, b.estilo, ahoraIso(),
                Autor(Cuenta.uid, Cuenta.yo.str("name").ifBlank { "Tú" }, Diseno.imagenOk(Cuenta.yo.str("avatar"))), 0, 0, texto = cuerpo.text.ifBlank { "Aquí se verá tu texto con esta estética." }, diseno = b.diseno,
                resena = if (b.resena && b.disco.isNotBlank()) Resena(b.disco, b.artista, b.anio.toIntOrNull() ?: 0, b.nota.toInt()) else null,
                galeria = b.galeriaUrls + b.galeria.map { it.mini }, etiquetas = Hashtags.juntar(etiquetas(b.etiquetas), cuerpo.text))
            Box(Modifier.fillMaxSize()) { VistaArticulo(a, emptyList(), { vista = false }) {} }
        }
        if (collage) EditorCollage(b.diseno, b.portada?.grande ?: b.portadaUrl, cerrar = { collage = false }) { col -> b = b.copy(diseno = b.diseno.copy(col = col)); collage = false }
    }
}

/** Fecha y hora de hace unos segundos (para que el artículo salga publicado ya). */
fun ahoraIsoMenos(seg: Int): String {
    val ms = ahoraMs() - seg * 1000
    return isoDesdeMs(ms)
}

/** ms desde 1970 → «2026-11-08T23:10:00.000Z» (sin librerías de fechas). */
fun isoDesdeMs(ms: Double): String {
    var t = (ms / 1000).toLong(); val s = t % 60; t /= 60; val mi = t % 60; t /= 60; val h = t % 24; var d = t / 24
    var y = 1970
    while (true) { val dy = if ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0) 366 else 365; if (d < dy) break; d -= dy; y++ }
    val bis = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
    val dm = intArrayOf(31, if (bis) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31); var m = 0
    while (d >= dm[m]) { d -= dm[m]; m++ }
    fun p(n: Long, w: Int = 2) = n.toString().padStart(w, '0')
    return "$y-${p((m + 1).toLong())}-${p(d + 1)}T${p(h)}:${p(mi)}:${p(s)}.000Z"
}

/** Botones de formato (como la web): envuelven la selección o agregan el prefijo a la línea. */
private fun formato(v: TextFieldValue, k: String): TextFieldValue {
    val t = v.text; val a = v.selection.min; val z = v.selection.max; val sel = t.substring(a, z)
    fun envolver(i: String, f: String): TextFieldValue { val dentro = sel.ifEmpty { "texto" }; val nuevo = t.substring(0, a) + i + dentro + f + t.substring(z); return TextFieldValue(nuevo, TextRange(a + i.length, a + i.length + dentro.length)) }
    fun prefijo(p: String): TextFieldValue { val ini = t.lastIndexOf('\n', (a - 1).coerceAtLeast(0)).let { if (it < 0 || a == 0) 0 else it + 1 }; return TextFieldValue(t.substring(0, ini) + p + t.substring(ini), TextRange(a + p.length)) }
    return when (k) { "b" -> envolver("**", "**"); "i" -> envolver("*", "*"); "h" -> prefijo("## "); "q" -> prefijo("> "); else -> prefijo("- ") }
}

@Composable
private fun Titulito(t: String) = Text(t, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))

@Composable
private fun MiniFoto(url: String, quitar: () -> Unit) {
    val img = imagen(url)
    Box(Modifier.size(84.dp).clip(RoundedCornerShape(12.dp)).background(Panel)) {
        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp).clip(CircleShape).background(Color(0xB3000000)).clickable(onClick = quitar), contentAlignment = Alignment.Center) { Icono("cerrar", Color.White, 14.dp) }
    }
}

@Composable
fun Pestanas(ops: List<Pair<String, String>>, actual: String, elegir: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ops.forEach { (k, n) ->
            val on = actual == k
            Text(n, color = if (on) Color(0xFF1B1E23) else TextoClaro, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) Amarillo else Color.Transparent).border(1.dp, if (on) Amarillo else Color(0x33FFFFFF), RoundedCornerShape(999.dp)).clickable { elegir(k) }.padding(horizontal = 14.dp, vertical = 8.dp))
        }
    }
}

/** Fichas que se acomodan en varias filas. */
@Composable
fun Fichas(ops: List<Pair<String, String>>, actual: String, muestra: (@Composable (String) -> Unit)? = null, elegir: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ops.chunked(3).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                fila.forEach { (k, n) ->
                    val on = actual == k
                    Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) Amarillo else Color(0x14FFFFFF)).clickable { elegir(k) }.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (muestra != null) { muestra(k); Spacer(Modifier.width(6.dp)) }
                        Text(n, color = if (on) Color(0xFF1B1E23) else TextoClaro, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                    }
                }
                repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Puntos(actual: Long?, conAuto: Boolean, elegir: (Long?) -> Unit) {
    val cols = listOf(0xFF141414, 0xFFFFFFFF, 0xFFF2C230, 0xFFFF3D8B, 0xFFE3242B, 0xFF29D3F0, 0xFF3ECF8E, 0xFFA78BFA, 0xFFFF8A3D, 0xFF2D47C4)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (conAuto) Text("Auto", color = if (actual == null) Color(0xFF1B1E23) else TextoClaro, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (actual == null) Amarillo else Color(0x14FFFFFF)).clickable { elegir(null) }.padding(horizontal = 12.dp, vertical = 7.dp))
        cols.forEach { c -> Box(Modifier.size(32.dp).clip(CircleShape).background(col(c)).border(if (actual == c) 3.dp else 1.dp, if (actual == c) Amarillo else Color(0x55FFFFFF), CircleShape).clickable { elegir(c) }) }
    }
}

@Composable
private fun Deslizador(etiqueta: String, valor: Float, rango: ClosedFloatingPointRange<Float>, texto: String, cambiar: (Float) -> Unit) {
    Column {
        Row { Text(etiqueta, fontSize = 14.sp, modifier = Modifier.weight(1f)); Text(texto, color = Tenue, fontSize = 14.sp) }
        Slider(valor, cambiar, valueRange = rango, colors = SliderDefaults.colors(thumbColor = Amarillo, activeTrackColor = Amarillo))
    }
}

@Composable
private fun SelectorEstetica(e: Estilo, cambiar: (Estilo) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Estéticas de revista", color = Tenue, fontSize = 13.sp)
        Fichas(Estilo.TEMAS.drop(6).map { it to (Estilo.NOMBRES_TEMA[it] ?: it) }, e.th, { t -> Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).drawBehind { fondoTema(Estilo(th = t), Temas.de(Estilo(th = t))) }) }) { cambiar(e.conTema(it)) }
        Text("Básicas", color = Tenue, fontSize = 13.sp)
        Fichas(Estilo.TEMAS.take(6).map { it to (Estilo.NOMBRES_TEMA[it] ?: it) }, e.th, { t -> Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).drawBehind { fondoTema(Estilo(th = t), Temas.de(Estilo(th = t))) }) }) { cambiar(e.conTema(it)) }
        Text("Tipografía del título", color = Tenue, fontSize = 13.sp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Estilo.FUENTES.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    fila.forEach { f ->
                        val on = e.ft == f
                        Text(Estilo.NOMBRES_FUENTE[f] ?: f, fontFamily = Fuentes.titulo(f), fontSize = 20.sp, color = if (on) Color(0xFF1B1E23) else TextoClaro,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) Amarillo else Color(0x14FFFFFF)).clickable { cambiar(e.copy(ft = f)) }.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                    if (fila.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Text("Color de acento", color = Tenue, fontSize = 13.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Estilo.ACENTOS.forEach { (k, c) -> Box(Modifier.size(34.dp).clip(CircleShape).background(col(c)).border(if (e.ac == k) 3.dp else 0.dp, Color.White, CircleShape).clickable { cambiar(e.copy(ac = k)) }) }
        }
        Text("Portada", color = Tenue, fontSize = 13.sp)
        Fichas(listOf("a" to "Imagen arriba", "f" to "Imagen de fondo", "m" to "Portada de revista"), e.cv) { cambiar(e.copy(cv = it)) }
        if (e.cv == "m") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Campo(e.mh, { cambiar(e.copy(mh = it)) }, "Nombre de la revista", max = 22, pista = "Radio de ruta") }
            Box(Modifier.weight(1f)) { Campo(e.stk, { cambiar(e.copy(stk = it)) }, "Sticker", max = 24, pista = "¡Nuevo!") }
        }
        Text("Detalles", color = Tenue, fontSize = 13.sp)
        Fichas(listOf("al" to "Títulos centrados", "dc" to "Letra capital", "sz" to "Título grande"), "") {
            cambiar(when (it) { "al" -> e.copy(al = if (e.al == "c") "l" else "c"); "dc" -> e.copy(dc = 1 - e.dc); else -> e.copy(sz = if (e.sz == "g") "m" else "g") })
        }
        Text(listOfNotNull("centrados".takeIf { e.al == "c" }, "letra capital".takeIf { e.dc == 1 }, "título grande".takeIf { e.sz == "g" }).joinToString(" · ").ifEmpty { "Sin detalles" }, color = Tenue, fontSize = 13.sp)
    }
}

@Composable
private fun SelectorFondo(d: Diseno, hayPortada: Boolean, cambiar: (Diseno) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("El fondo que se ve detrás del artículo al abrirlo.", color = Tenue, fontSize = 14.sp)
        Fichas(Diseno.FONDOS.map { it to (Diseno.NOMBRES_FONDO[it] ?: it) }, d.bg, { k -> if (k !in setOf("none", "cover", "color")) Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).drawBehind { fondoDiseno(Diseno(bg = k)) }) }) { cambiar(d.copy(bg = it)) }
        if (d.bg == "color") {
            Text("Color 1", fontSize = 14.sp); Puntos(d.c1, false) { cambiar(d.copy(c1 = it ?: d.c1)) }
            Text("Color 2", fontSize = 14.sp); Puntos(d.c2, false) { cambiar(d.copy(c2 = it ?: d.c2)) }
            Fichas(listOf("lin" to "Difuminado lineal", "rad" to "Radial", "con" to "Cónico"), d.gt) { cambiar(d.copy(gt = it)) }
            if (d.gt != "rad") Deslizador("Ángulo", d.ga.toFloat(), 0f..360f, "${d.ga}°") { cambiar(d.copy(ga = it.toInt())) }
        }
        if (d.bg == "cover") { Deslizador("Desenfoque", d.blur.toFloat(), 0f..40f, "${d.blur} px") { cambiar(d.copy(blur = it.toInt())) }; if (!hayPortada) Text("Primero elige una imagen de portada.", color = Tenue, fontSize = 13.sp) }
        Deslizador("Oscurecer", d.dim.toFloat(), 0f..90f, "${d.dim}%") { cambiar(d.copy(dim = (it / 5).toInt() * 5)) }
        Fichas(listOf("vin" to "Viñeta" + if (d.vin) ": sí" else "", "grain" to "Grano de foto" + if (d.grain) ": sí" else ""), "") { if (it == "vin") cambiar(d.copy(vin = !d.vin)) else cambiar(d.copy(grain = !d.grain)) }
        Box(Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(14.dp)).drawBehind { if (d.bg == "none" || d.bg == "cover") drawRect(Brush.linearGradient(listOf(Color(0xFF2A2E36), Color(0xFF15171B)))) else fondoDiseno(d); capasEncima(d) }, contentAlignment = Alignment.Center) {
            Text("Así se verá el fondo", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SelectorLetra(d: Diseno, cambiar: (Diseno) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Letra del texto", color = Tenue, fontSize = 13.sp)
        Fichas(Diseno.LETRAS.map { it to (Diseno.NOMBRES_LETRA[it] ?: it) }, d.tf) { cambiar(d.copy(tf = it)) }
        Text("Letra del título", color = Tenue, fontSize = 13.sp)
        Fichas(Diseno.LETRAS.map { it to (Diseno.NOMBRES_LETRA[it] ?: it) }, d.hf) { cambiar(d.copy(hf = it)) }
        Deslizador("Tamaño del texto", (if (d.fs == 0) 17 else d.fs).toFloat(), 13f..28f, if (d.fs == 0) "auto" else "${d.fs} px") { cambiar(d.copy(fs = it.toInt())) }
        Deslizador("Interlineado", (if (d.lh == 0.0) 1.6 else d.lh).toFloat(), 1.2f..2.2f, if (d.lh == 0.0) "auto" else ((d.lh * 10).toInt() / 10.0).toString()) { cambiar(d.copy(lh = (it * 10).toInt() / 10.0)) }
        Deslizador("Tamaño del título", (if (d.ts == 0) 34 else d.ts).toFloat(), 20f..80f, if (d.ts == 0) "auto" else "${d.ts} px") { cambiar(d.copy(ts = it.toInt())) }
        Fichas(listOf("" to "Del tema", "l" to "Izquierda", "j" to "Justificado", "c" to "Centrado"), d.ta) { cambiar(d.copy(ta = it)) }
        Fichas(listOf("tsh" to "Sombra en las letras" + if (d.tsh) ": sí" else ""), "") { cambiar(d.copy(tsh = !d.tsh)) }
        Text("Color del texto", color = Tenue, fontSize = 13.sp); Puntos(d.tc, true) { cambiar(d.copy(tc = it)) }
        Text("Color del título", color = Tenue, fontSize = 13.sp); Puntos(d.hc, true) { cambiar(d.copy(hc = it)) }
        Enlace("Volver a la letra del tema") { cambiar(d.copy(tf = "", hf = "", fs = 0, lh = 0.0, ts = 0, tc = null, hc = null, ta = "", tsh = false)) }
    }
}

@Composable
private fun SelectorPanel(d: Diseno, cambiar: (Diseno) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Un panel detrás del texto para que se lea bien sobre el fondo.", color = Tenue, fontSize = 14.sp)
        Fichas(Diseno.PANELES.map { it to (Diseno.NOMBRES_PANEL[it] ?: it) }, d.panel) { cambiar(d.copy(panel = it)) }
        if (d.panel != "none") {
            Deslizador("Opacidad", d.pop.toFloat(), 0f..100f, "${d.pop}%") { cambiar(d.copy(pop = (it / 5).toInt() * 5)) }
            Deslizador("Desenfoque detrás", d.pbl.toFloat(), 0f..30f, "${d.pbl} px") { cambiar(d.copy(pbl = it.toInt())) }
            Deslizador("Bordes redondeados", d.rad.toFloat(), 0f..40f, "${d.rad} px") { cambiar(d.copy(rad = it.toInt())) }
        }
    }
}

@Suppress("unused") private fun ColumnScope.nada() {}
