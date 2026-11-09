package rr.revista

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Todo lo que define cómo se ve un artículo, ya resuelto (estética + diseño avanzado). */
private class Look(a: Articulo) {
    val e = a.estilo
    val t = Temas.de(e)
    val d = a.diseno
    val acento = col(Estilo.ACENTOS[e.ac] ?: 0xFFF2C230)
    private val cols = Diseno.colores(d, a.portada.isNotEmpty())
    val titulo: Color = cols.first?.let(::col) ?: t.titulo?.let(::col) ?: col(t.texto)
    val texto: Color = cols.second?.let(::col) ?: col(t.texto)
    val tenue: Color = if (cols.first != null) titulo.copy(alpha = .75f) else col(t.tenue)
    /** Para nombres y secciones: si el título lleva contorno (Tokio, Y2K, Tabloide), se usa el color del texto para que se lea. */
    val encabezado: Color = if (cols.first == null && t.contorno != null) col(t.texto) else titulo
    val letraTitulo: FontFamily = Fuentes.diseno(d.hf) ?: Fuentes.titulo(e.ft)
    val letraTexto: FontFamily = Fuentes.diseno(d.tf) ?: Fuentes.de("barlow")
    val tamTitulo: TextUnit = if (d.ts > 0) d.ts.sp else if (e.sz == "g") 42.sp else 32.sp
    val tamTexto: TextUnit = (if (d.fs > 0) d.fs else 18).sp
    val interlineado: Float = if (d.lh > 0) d.lh.toFloat() else 1.6f
    val centrado = e.al == "c"
    val alineado: TextAlign = when (d.ta) { "j" -> TextAlign.Justify; "c" -> TextAlign.Center; "l" -> TextAlign.Start; else -> TextAlign.Start }
    val sombraTexto: Shadow? = if (d.tsh) Shadow(Color(0x8C000000), Offset(0f, 2f), 8f) else null
}

@Composable
fun Lectura(id: String, cerrar: () -> Unit) {
    val datos by produceState<Result<Articulo?>?>(null, id) { value = runCatching { if (id.startsWith("ejemplo-")) Ejemplos.uno(id) else Supa.articulo(id) } }
    var versionComs by remember { mutableStateOf(0) }
    val coms by produceState<List<Comentario>>(emptyList(), id, versionComs) { value = if (id.startsWith("ejemplo-")) listOf(Comentario("", "", "Ejemplo", "", "Así se ven los comentarios de la comunidad.", ahoraIso())) else runCatching { Supa.comentarios(id) }.getOrDefault(emptyList()) }
    val r = datos
    Box(Modifier.fillMaxSize().background(Asfalto)) {
        when {
            r == null -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.CircularProgressIndicator(color = Amarillo); Text("Abriendo el artículo…", color = Tenue, modifier = Modifier.padding(top = 12.dp))
            }
            r.isFailure || r.getOrNull() == null -> Column(Modifier.padding(24.dp)) {
                BotonVolver(cerrar, TextoClaro)
                Text(if (r.isFailure) "No se pudo abrir: ${r.exceptionOrNull()?.message ?: ""}" else "Ese artículo ya no existe.", color = Tenue, modifier = Modifier.padding(top = 16.dp))
            }
            else -> VistaArticulo(r.getOrThrow()!!, coms, cerrar) { versionComs++ }
        }
    }
}

@Composable
private fun BotonVolver(cerrar: () -> Unit, color: Color) {
    Row(Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0x59000000)).clickable(onClick = cerrar).padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icono("atras", Color.White, 20.dp); Text("Volver", color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun VistaArticulo(a: Articulo, coms: List<Comentario>, cerrar: () -> Unit, recargarComs: () -> Unit) {
    val L = Look(a)
    val portada = imagen(a.portada)
    val scope = rememberCoroutineScope()
    val ejemplo = a.id.startsWith("ejemplo-")
    var likes by remember(a.id) { mutableStateOf(a.likes) }
    var recs by remember(a.id) { mutableStateOf(a.recomendados) }
    var denuncia by remember { mutableStateOf<Pair<String, String>?>(null) }
    var notaRec by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf<String?>(null) }
    val mio = Cuenta.dentro && Cuenta.uid == a.autor.id
    fun accion(bloque: suspend () -> Unit) {
        if (ejemplo) { Aviso.mostrar("Es un ejemplo: en los artículos de verdad esto funciona."); return }
        if (!Nav.necesitaCuenta()) return
        scope.launch { try { bloque() } catch (e: Throwable) { Aviso.mostrar(e.message ?: "Algo salió mal.", true) } }
    }
    Box(Modifier.fillMaxSize()) {
        // Fondo fijo (no se mueve al bajar), como el fondo «pegado» de la web
        Box(Modifier.fillMaxSize().drawBehind { if (L.d.bg == "none" || (L.d.bg == "cover" && portada == null)) fondoTema(L.e, L.t) else fondoDiseno(L.d) })
        if (L.d.bg == "cover" && portada != null) Image(portada, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.15f; scaleY = 1.15f }.blur(L.d.blur.dp))
        if (L.d.col.isNotEmpty() && L.d.cpos == "fondo") Collage(L.d.col, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().drawBehind { capasEncima(L.d) })

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                BotonVolver(cerrar, L.titulo); Spacer(Modifier.weight(1f))
                Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0x59000000)).clickable { abrirEnlace(Supa.WEB + "#rv=" + a.id) }, contentAlignment = Alignment.Center) { Icono("compartir", Color.White, 20.dp) }
            }
            Column(Modifier.padding(horizontal = 18.dp)) {
                if (L.d.col.isNotEmpty() && L.d.cpos == "cabecera")
                    Collage(L.d.col, Modifier.fillMaxWidth().aspectRatio(when (L.d.ch) { "cuadrado" -> 1f; "horizontal" -> 16f / 9f; else -> 9f / 16f }).heightIn(max = 640.dp).clip(RoundedCornerShape(14.dp)))
                when {
                    L.e.cv == "m" -> PortadaRevista(a, L, portada)
                    L.e.cv == "f" && portada != null -> Box(Modifier.fillMaxWidth().aspectRatio(.8f).clip(RoundedCornerShape(18.dp))) {
                        Image(portada, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color(0xE6000000))))
                        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) { Titulo(a, L, Color.White); Subtitulo(a, L, Color.White.copy(alpha = .85f)) }
                    }
                    else -> {
                        if (portada != null) Image(portada, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(16.dp)))
                        Spacer(Modifier.height(14.dp)); Titulo(a, L, L.titulo); Subtitulo(a, L, L.tenue)
                    }
                }
                // Autor
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 14.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled = a.autor.id.isNotEmpty()) { Nav.ir(Destino.Perfil(a.autor.id)) }) {
                    val av = imagen(a.autor.avatar)
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Amarillo), contentAlignment = Alignment.Center) {
                        if (av != null) Image(av, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) else Text(a.autor.nombre.take(1).uppercase(), color = Color(0xFF1B1E23), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(a.autor.nombre, color = L.encabezado, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${Fechas.corta(a.fecha)} · ${Formato.minutos(a.texto)} min de lectura", color = L.tenue, fontSize = 13.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    if (!mio && a.autor.id.isNotEmpty()) {
                        val sigo = a.autor.id in Cuenta.sigo
                        Text(if (sigo) "Siguiendo" else "Seguir", color = if (sigo) L.encabezado else Color(0xFF1B1E23), fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (sigo) Color(0x33000000) else L.acento).clickable { accion { Cuenta.alternarSeguir(a.autor.id) } }.padding(horizontal = 14.dp, vertical = 7.dp))
                    }
                }
                if (mio && a.estado == "draft") Text("Borrador · solo tú lo ves", color = Color(0xFF1B1E23), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp).clip(RoundedCornerShape(8.dp)).background(Amarillo).padding(horizontal = 10.dp, vertical = 4.dp))
                a.resena?.let { Resena(it, L) }
                Cuerpo(a, L)
                if (a.galeria.isNotEmpty()) Galeria(a.galeria)
                if (a.canciones.isNotEmpty()) {
                    Seccion("Canciones del artículo", L)
                    a.canciones.forEach { (n, ar) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x26000000)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icono("nota", L.acento, 20.dp); Spacer(Modifier.width(10.dp))
                            Column { Text(n, color = L.texto, fontWeight = FontWeight.SemiBold); Text(ar, color = L.tenue, fontSize = 13.sp) }
                        }
                    }
                }
                if (a.etiquetas.isNotEmpty()) Row(Modifier.padding(top = 14.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    a.etiquetas.forEach { Text("#$it", color = L.acento, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, L.acento.copy(alpha = .6f), RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) }
                }
                // Me gusta, recomendar, compartir y denunciar
                val gusta = a.id in Cuenta.likes; val recomendado = a.id in Cuenta.recomiendo
                Row(Modifier.padding(top = 18.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pastilla("corazon", (if (gusta) "Te gusta" else "Me gusta") + " · $likes", gusta, L.acento) { accion { val on = Cuenta.alternarLike(a.id); likes = maxOf(0, likes + if (on) 1 else -1) } }
                    if (!mio) Pastilla("compartir", (if (recomendado) "Recomendado" else "Recomendar") + " · $recs", recomendado, L.acento) {
                        if (recomendado) accion { Cuenta.dejarDeRecomendar(a.id); recs = maxOf(0, recs - 1); Aviso.mostrar("Ya no lo recomiendas.") }
                        else accion { notaRec = true }
                    }
                    Pastilla("compartir", "Compartir", false, L.acento) { abrirEnlace(Supa.WEB + "#rv=" + a.id) }
                }
                Row(Modifier.padding(top = 6.dp)) {
                    if (!mio) Enlace("Denunciar", L.tenue) { accion { denuncia = "post" to a.id } }
                    if (mio || Cuenta.esAdmin) Enlace("Eliminar artículo", Color(0xFFFF8A7A)) { accion { borrar = "post" } }
                }
                Comentarios(a, coms, L, ejemplo, recargarComs, { tipo, cid -> accion { denuncia = tipo to cid } }) { cid -> accion { borrar = cid } }
            }
        }
        denuncia?.let { (t, i) -> DialogoDenuncia(t, i) { denuncia = null } }
        if (notaRec) DialogoTexto("Recomendar en tu perfil", "Quienes te siguen lo verán en «Siguiendo».", "Una nota (opcional)", "Recomendar", cerrar = { notaRec = false }) { nota ->
            notaRec = false
            scope.launch { try { Cuenta.recomendar(a.id, nota); recs++; Aviso.mostrar("Recomendado en tu perfil.") } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } }
        }
        borrar?.let { b ->
            if (b == "post") DialogoConfirmar("¿Eliminar este artículo?", "Se borrarán también sus comentarios y me gusta.", "Eliminar", { borrar = null }) {
                borrar = null
                scope.launch { try { Cuenta.api("DELETE", "/rest/v1/posts?id=eq.${a.id}"); Aviso.mostrar("Artículo eliminado."); Nav.atras() } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } }
            } else DialogoConfirmar("¿Borrar comentario?", "No se puede deshacer.", "Borrar", { borrar = null }) {
                borrar = null
                scope.launch { try { Cuenta.borrarComentario(b); recargarComs() } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } }
            }
        }
    }
}

@Composable
private fun Pastilla(icono: String, texto: String, activo: Boolean, acento: Color, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(999.dp)).background(if (activo) acento else Color(0x40000000)).border(1.dp, if (activo) acento else Color(0x33FFFFFF), RoundedCornerShape(999.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val c = if (activo) Color(0xFF1B1E23) else Color.White
        Icono(icono, c, 18.dp); Spacer(Modifier.width(6.dp)); Text(texto, color = c, fontWeight = FontWeight.SemiBold)
    }
}

/** Comentarios con respuestas (un nivel, como la web), escribir, responder, borrar y denunciar. */
@Composable
private fun Comentarios(a: Articulo, coms: List<Comentario>, L: Look, ejemplo: Boolean, recargar: () -> Unit, denunciar: (String, String) -> Unit, borrar: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var texto by remember(a.id) { mutableStateOf("") }
    var responderA by remember(a.id) { mutableStateOf<Comentario?>(null) }
    var enviando by remember { mutableStateOf(false) }
    Seccion("Comentarios (${coms.size})", L)
    val ids = coms.map { it.id }.toSet()
    val principales = coms.filter { it.padre.isEmpty() || it.padre !in ids }
    if (coms.isEmpty()) Text("Sé la primera persona en comentar.", color = L.tenue)
    principales.forEach { c ->
        UnComentario(c, L, false, { responderA = c }, denunciar, borrar)
        coms.filter { it.padre == c.id }.forEach { h -> UnComentario(h, L, true, { responderA = c }, denunciar, borrar) }
    }
    Column(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xE61B1E23)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        responderA?.let { r -> Row(verticalAlignment = Alignment.CenterVertically) { Text("Respondiendo a ${r.nombre}", color = Tenue, fontSize = 14.sp, modifier = Modifier.weight(1f)); Enlace("cancelar", Tenue) { responderA = null } } }
        Campo(texto, { texto = it }, if (Cuenta.dentro) "Escribe un comentario…" else "Entra para comentar", lineas = 2, max = 1000)
        Boton("Comentar", ocupado = enviando, habilitado = texto.isNotBlank()) {
            if (ejemplo) { Aviso.mostrar("Es un ejemplo: en los artículos de verdad esto funciona."); return@Boton }
            if (!Nav.necesitaCuenta()) return@Boton
            scope.launch {
                enviando = true
                try { Cuenta.comentar(a.id, texto, responderA?.id); texto = ""; responderA = null; recargar() }
                catch (e: Throwable) { Aviso.mostrar(e.message ?: "No se pudo comentar.", true) }
                enviando = false
            }
        }
    }
}

@Composable
private fun UnComentario(c: Comentario, L: Look, respuesta: Boolean, responder: () -> Unit, denunciar: (String, String) -> Unit, borrar: (String) -> Unit) {
    val mio = Cuenta.dentro && c.autorId == Cuenta.uid
    Column(Modifier.fillMaxWidth().padding(start = if (respuesta) 26.dp else 0.dp, top = 5.dp, bottom = 5.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x33000000)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(enabled = c.autorId.isNotEmpty()) { Nav.ir(Destino.Perfil(c.autorId)) }) {
            Avatar(Autor(c.autorId, c.nombre, c.avatar), 24); Spacer(Modifier.width(8.dp))
            Text("${c.nombre} · ${Fechas.corta(c.fecha)}", color = L.acento, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Text(c.texto, color = L.texto, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 6.dp))
        if (c.id.isNotEmpty()) Row {
            Enlace("Responder", L.tenue, responder)
            if (mio || Cuenta.esAdmin) Enlace("Borrar", L.tenue) { borrar(c.id) }
            if (!mio) Enlace("Denunciar", L.tenue) { denunciar("comment", c.id) }
        }
    }
}

@Composable
private fun Seccion(t: String, L: Look) = Text(t, color = L.encabezado, fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))

/** Título con los efectos de cada estética: contorno, sombra dura, minúsculas o mayúsculas. */
@Composable
private fun Titulo(a: Articulo, L: Look, color: Color, tam: TextUnit = L.tamTitulo, texto: String = a.titulo) {
    val t = L.t
    val txt = when { t.minusculas -> texto.lowercase(); t.mayusculas -> texto.uppercase(); else -> texto }
    val al = if (L.centrado) TextAlign.Center else TextAlign.Start
    val estilo = TextStyle(fontFamily = L.letraTitulo, fontWeight = FontWeight.Bold, fontSize = tam, lineHeight = tam * 1.06f, textAlign = al, shadow = L.sombraTexto)
    val c = if (L.d.hc != null) color else (if (color == Color.White) Color.White else (t.titulo?.let(::col) ?: color))
    Box(Modifier.fillMaxWidth()) {
        t.contorno?.let { k ->
            Text(txt, style = estilo.copy(color = col(k), drawStyle = Stroke(width = 7f), shadow = t.sombra?.let { Shadow(col(it), Offset(5f, 5f), 0f) }), modifier = Modifier.fillMaxWidth())
        }
        val sombra = when {
            t.contorno != null -> null
            L.e.th == "neon" -> Shadow(L.acento, Offset.Zero, 22f)
            t.sombra != null -> Shadow(col(t.sombra), Offset(3f, 3f), 0f)
            else -> L.sombraTexto
        }
        Text(txt, style = estilo.copy(color = c, shadow = sombra), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Subtitulo(a: Articulo, L: Look, color: Color) {
    if (a.subtitulo.isBlank()) return
    Text(a.subtitulo, color = if (L.e.th == "teen") L.acento else color, fontSize = 18.sp, lineHeight = 24.sp, textAlign = if (L.centrado) TextAlign.Center else TextAlign.Start,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
}

/** «Portada de revista» (cv = m): cabecera, franja, sticker, titular y código de barras. */
@Composable
private fun PortadaRevista(a: Articulo, L: Look, portada: androidx.compose.ui.graphics.ImageBitmap?) {
    val fecha = Fechas.mesAnio(a.fecha)
    Box(Modifier.fillMaxWidth().aspectRatio(.74f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF262A31))) {
        if (portada != null) Image(portada, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x66000000), 0.25f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color(0xD9000000))))
        Column(Modifier.fillMaxSize()) {
            Text("EDICIÓN ESPECIAL · $fecha · EDICIÓN ESPECIAL · $fecha · EDICIÓN ESPECIAL", color = Color(0xFF141414), fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp, maxLines = 1, softWrap = false,
                modifier = Modifier.fillMaxWidth().background(Amarillo).padding(horizontal = 8.dp, vertical = 4.dp))
            val mast = (L.e.mh.ifBlank { "Radio de ruta" }).uppercase()
            Text(mast, color = L.acento.takeIf { L.e.ac != "amarillo" } ?: Amarillo, fontFamily = Fuentes.de("anton"), fontSize = 46.sp, lineHeight = 48.sp, maxLines = 1, softWrap = false,
                style = TextStyle(shadow = Shadow(Color(0x99000000), Offset(3f, 4f), 6f)), modifier = Modifier.padding(start = 16.dp, top = 6.dp))
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Titulo(a, L, Color.White, 34.sp)
                    if (a.subtitulo.isNotBlank()) Text(a.subtitulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 6.dp),
                        style = TextStyle(shadow = Shadow(Color(0xCC000000), Offset(0f, 2f), 6f)))
                }
                Spacer(Modifier.width(12.dp))
                Canvas(Modifier.width(74.dp).height(46.dp).clip(RoundedCornerShape(3.dp)).background(Color.White).padding(6.dp)) {
                    var x = 0f; var i = 0
                    while (x < size.width) { val g = if (i % 3 == 0) 3f else 1.5f; drawRect(Color(0xFF141414), Offset(x, 0f), Size(g, size.height)); x += g + if (i % 2 == 0) 2f else 3f; i++ }
                }
            }
        }
        if (L.e.stk.isNotBlank()) Box(Modifier.align(Alignment.TopEnd).offset(x = (-10).dp, y = 84.dp).size(92.dp).rotate(12f).clip(Formas.estallido).background(L.acento), contentAlignment = Alignment.Center) {
            Text(L.e.stk.uppercase(), color = Color(0xFF141414), fontFamily = Fuentes.de("archivo"), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 14.sp, modifier = Modifier.padding(14.dp))
        }
    }
}

@Composable
private fun Resena(r: Resena, L: Look) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x40000000)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(buildAnnotatedString { append("${r.nota}"); withStyle(SpanStyle(fontSize = 14.sp)) { append("/10") } }, color = Color(0xFF1B1E23), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(L.acento).padding(horizontal = 12.dp, vertical = 6.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(r.disco, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(listOf(r.artista, if (r.anio > 0) r.anio.toString() else "").filter { it.isNotBlank() }.joinToString(" · "), color = Color.White.copy(alpha = .75f), fontSize = 14.sp)
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x33FFFFFF))) {
                Box(Modifier.fillMaxWidth(r.nota / 10f).height(6.dp).background(L.acento))
            }
        }
    }
}

private fun texto(linea: String, acento: Color): AnnotatedString = buildAnnotatedString {
    Formato.trozos(linea).forEach { t ->
        when {
            t.negrita -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(t.texto) }
            t.cursiva -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(t.texto) }
            t.enlace.isNotEmpty() -> withStyle(SpanStyle(color = acento, textDecoration = TextDecoration.Underline)) { append(t.texto) }
            else -> append(t.texto)
        }
    }
}

/** El texto del artículo, con el panel transparente del diseño avanzado si lo tiene. */
@Composable
private fun Cuerpo(a: Articulo, L: Look) {
    val d = L.d; val pop = d.pop / 100f
    val panel = when (d.panel) {
        "vidrio" -> Modifier.clip(RoundedCornerShape(d.rad.dp)).background(Color.White.copy(alpha = pop * .22f)).border(1.dp, Color.White.copy(alpha = pop * .4f), RoundedCornerShape(d.rad.dp)).padding(horizontal = 18.dp, vertical = 16.dp)
        "negro" -> Modifier.clip(RoundedCornerShape(d.rad.dp)).background(Color(0xFF0C0C0E).copy(alpha = pop)).padding(horizontal = 18.dp, vertical = 16.dp)
        "blanco" -> Modifier.clip(RoundedCornerShape(d.rad.dp)).background(Color.White.copy(alpha = pop)).padding(horizontal = 18.dp, vertical = 16.dp)
        "papel" -> Modifier.clip(RoundedCornerShape(d.rad.dp)).background(Color(0xFFF1E9D8).copy(alpha = pop)).padding(horizontal = 18.dp, vertical = 16.dp)
        "cinta" -> Modifier.clip(Formas.papelPanel).background(Color(0xFFFAF8F0).copy(alpha = pop)).padding(horizontal = 18.dp, vertical = 18.dp)
        else -> Modifier
    }
    val estilo = TextStyle(fontFamily = L.letraTexto, fontSize = L.tamTexto, lineHeight = L.tamTexto * L.interlineado, color = L.texto, textAlign = L.alineado, shadow = L.sombraTexto)
    val bloques = Formato.bloques(a.texto)
    Column(Modifier.fillMaxWidth().then(panel), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        bloques.forEachIndexed { i, b ->
            when (b) {
                is Bloque.Parrafo -> {
                    val s = texto(b.lineas.joinToString("\n"), L.acento)
                    if (i == 0 && L.e.dc == 1 && s.text.isNotEmpty()) Text(buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = L.letraTitulo, fontSize = L.tamTexto * 2.6f, color = L.acento, fontWeight = FontWeight.Bold)) { append(s.text.take(1)) }
                        append(s.subSequence(1, s.length))
                    }, style = estilo) else Text(s, style = estilo)
                }
                is Bloque.Titulo -> Text(texto(b.texto, L.acento), style = estilo.copy(fontFamily = L.letraTitulo, fontWeight = FontWeight.Bold, fontSize = if (b.grande) 26.sp else 22.sp, lineHeight = if (b.grande) 30.sp else 26.sp, color = if (d.hc != null) L.titulo else L.texto))
                is Bloque.Cita -> {
                    val (fondo, tinta, giro) = when (L.e.th) {
                        "punk" -> Triple(L.acento, Color(0xFF111111), 1f); "teen" -> Triple(Color(0xFF3D5AFE), Color.White, -1f); "tabloide" -> Triple(Color(0xFF1D4ED8), Color.White, 0f)
                        else -> Triple(Color.Transparent, L.texto, 0f)
                    }
                    Box(Modifier.fillMaxWidth().rotate(giro).background(fondo).then(if (fondo == Color.Transparent) Modifier.drawBehind { drawRect(L.acento, size = Size(4.dp.toPx(), size.height)) } else Modifier).padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)) {
                        Text(texto(b.lineas.joinToString("\n"), L.acento), style = estilo.copy(color = tinta, fontStyle = FontStyle.Italic, fontSize = L.tamTexto * 1.08f))
                    }
                }
                is Bloque.Lista -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    b.items.forEach { li -> Row { Text("•  ", style = estilo.copy(color = L.acento, fontWeight = FontWeight.Bold)); Text(texto(li, L.acento), style = estilo) } }
                }
                is Bloque.LineaTiempo -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    b.items.forEach { (y, txt) -> Row { Text(y, style = estilo.copy(color = L.acento, fontWeight = FontWeight.Bold), modifier = Modifier.width(62.dp)); Text(texto(txt, L.acento), style = estilo) } }
                }
                is Bloque.Video -> {
                    val img = imagen("https://i.ytimg.com/vi/${b.id}/hqdefault.jpg")
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)).background(Color.Black).clickable { abrirEnlace("https://www.youtube.com/watch?v=${b.id}") }, contentAlignment = Alignment.Center) {
                        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.size(62.dp).clip(CircleShape).background(Color(0xCCE3242B)), contentAlignment = Alignment.Center) { Icono("play", Color.White, 30.dp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Galeria(fotos: List<String>) {
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        fotos.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { u ->
                    val img = imagen(Supa.miniatura(u), u)
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Panel)) { if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                }
                if (fila.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
