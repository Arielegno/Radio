package rr.revista

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/* Listas de canciones (como en la web): cada persona hace las suyas y puede abrirlas para que otros agreguen. */

private fun tono(t: String): Color {
    val h = (t.fold(7) { a, c -> a * 31 + c.code } and 0x7fffffff) % 360
    return Color.hsl(h.toFloat(), .55f, .42f)
}

/** Las listas de un perfil (pestaña «Listas»). */
@Composable
fun ListasDePerfil(dueno: String) {
    val l by produceState<Result<List<Map<String, Any?>>>?>(null, dueno) {
        value = runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/song_lists?select=id,title,note,open,song_list_items(count)&owner_id=eq.$dueno&order=created_at.desc&limit=40")).mapNotNull { it.asObj() } }
    }
    Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val r = l
        when {
            r == null -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }
            r.isFailure -> Text(r.exceptionOrNull()?.message ?: "", color = Tenue)
            r.getOrThrow().isEmpty() -> Text(if (dueno == Cuenta.uid) "Aún no tienes listas." else "Aún no tiene listas.", color = Tenue)
            else -> r.getOrThrow().forEach { x ->
                val n = ((x["song_list_items"] as? List<*>)?.firstOrNull() as? Map<*, *>)?.get("count")?.toString() ?: "0"
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).clickable { Nav.ir(Destino.Lista(x.str("id"))) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(tono(x.str("title")), Color(0xFF1B1E23)))), contentAlignment = Alignment.Center) { Icono("nota", Color.White, 26.dp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(x.str("title"), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$n canciones" + if (x.bool("open")) " · abierta" else "", color = Tenue, fontSize = 13.sp)
                    }
                }
            }
        }
        if (dueno == Cuenta.uid && !Cuenta.suspendida) Boton("+ Nueva lista", principal = false) { Nav.ir(Destino.EditarLista(null)) }
    }
}

/** Una lista con sus canciones. */
@Composable
fun ListaPantalla(id: String) {
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf(0) }
    var titulo by remember { mutableStateOf("") }
    var artista by remember { mutableStateOf("") }
    var enlace by remember { mutableStateOf("") }
    var borrar by remember { mutableStateOf(false) }
    val datos by produceState<Result<Pair<Map<String, Any?>?, List<Map<String, Any?>>>>?>(null, id, version) {
        value = runCatching {
            Json.arr(Cuenta.api("GET", "/rest/v1/song_lists?select=*,profiles!owner_id(name,avatar)&id=eq.$id")).firstOrNull()?.asObj() to
                Json.arr(Cuenta.api("GET", "/rest/v1/song_list_items?select=*,profiles!added_by(name)&list_id=eq.$id&order=created_at.asc&limit=200")).mapNotNull { it.asObj() }
        }
    }
    Column(Modifier.fillMaxSize().background(Asfalto).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val r = datos
        val l = r?.getOrNull()?.first
        Row(verticalAlignment = Alignment.CenterVertically) {
            Enlace("‹ Volver", TextoClaro) { Nav.atras() }; Spacer(Modifier.weight(1f))
            if (l != null && l.str("owner_id") == Cuenta.uid) { Enlace("Editar") { Nav.ir(Destino.EditarLista(id)) }; Enlace("Eliminar", Color(0xFFFF8A7A)) { borrar = true } }
        }
        when {
            r == null -> CircularProgressIndicator(color = Amarillo)
            r.isFailure -> Text(r.exceptionOrNull()?.message ?: "", color = Tenue)
            l == null -> Text("Esa lista ya no existe.", color = Tenue)
            else -> {
                val items = r.getOrThrow().second
                val mia = l.str("owner_id") == Cuenta.uid
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(tono(l.str("title")), Color(0xFF1B1E23)))).padding(18.dp)) {
                    Text(if (l.bool("open")) "LISTA ABIERTA · CUALQUIERA CON CUENTA PUEDE AGREGAR" else "LISTA DE ${l.map("profiles").str("name").uppercase()}", color = Color.White.copy(alpha = .8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(l.str("title"), color = Color.White, fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 32.sp)
                    Text("${items.size} canciones", color = Color.White.copy(alpha = .8f))
                }
                if (l.str("note").isNotBlank()) Text(l.str("note"), fontSize = 16.sp, lineHeight = 22.sp)
                if (items.isEmpty()) Text("La lista está vacía.", color = Tenue)
                items.forEachIndexed { i, x ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", color = Tenue, modifier = Modifier.width(28.dp))
                        Column(Modifier.weight(1f)) {
                            Text(x.str("title"), fontWeight = FontWeight.Bold)
                            Text(x.str("artist") + if (x.str("added_by") != l.str("owner_id")) " · la agregó ${x.map("profiles").str("name")}" else "", color = Tenue, fontSize = 13.sp)
                        }
                        if (x.str("link").isNotEmpty()) Enlace("Escuchar") { abrirEnlace(x.str("link")) }
                        if (Cuenta.dentro && (x.str("added_by") == Cuenta.uid || mia || Cuenta.esAdmin)) Enlace("quitar", Tenue) {
                            scope.launch { runCatching { Cuenta.api("DELETE", "/rest/v1/song_list_items?id=eq.${x.str("id")}") }.onFailure { Aviso.mostrar(it.message ?: "", true) }; version++ }
                        }
                    }
                }
                val puede = Cuenta.dentro && !Cuenta.suspendida && (mia || l.bool("open")) && items.size < 200
                if (puede) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Agregar una canción", fontWeight = FontWeight.Bold)
                    Campo(titulo, { titulo = it }, "Canción", max = 120, pista = "Ej: De música ligera")
                    Campo(artista, { artista = it }, "Artista", max = 100)
                    Campo(enlace, { enlace = it }, "Enlace (opcional)", max = 300, pista = "Spotify, YouTube, Bandcamp o SoundCloud")
                    Boton("Agregar", habilitado = titulo.isNotBlank() && artista.isNotBlank()) {
                        val e = enlace.trim()
                        if (e.isNotEmpty() && !Regex("^https://(open\\.spotify\\.com|(www\\.|music\\.)?youtube\\.com|youtu\\.be|[a-z0-9-]+\\.bandcamp\\.com|soundcloud\\.com)/").containsMatchIn(e)) { Aviso.mostrar("El enlace debe ser de Spotify, YouTube, Bandcamp o SoundCloud.", true); return@Boton }
                        scope.launch {
                            try {
                                Cuenta.api("POST", "/rest/v1/song_list_items", mapOf("list_id" to id, "added_by" to Cuenta.uid, "title" to titulo.trim(), "artist" to artista.trim(), "link" to e.ifEmpty { null }), "return=minimal")
                                titulo = ""; artista = ""; enlace = ""; version++
                            } catch (x: Throwable) { Aviso.mostrar(x.message ?: "", true) }
                        }
                    }
                } else if (!Cuenta.dentro) Text("Entra con tu cuenta para agregar canciones.", color = Tenue)
            }
        }
    }
    if (borrar) DialogoConfirmar("¿Eliminar esta lista?", "Se borra con todas sus canciones.", "Eliminar", { borrar = false }) {
        borrar = false
        scope.launch { try { Cuenta.api("DELETE", "/rest/v1/song_lists?id=eq.$id"); Aviso.mostrar("Lista eliminada."); Nav.atras() } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } }
    }
}

/** Crear o editar una lista. */
@Composable
fun EditarListaPantalla(id: String?) {
    val scope = rememberCoroutineScope()
    var titulo by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    var abierta by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    val cargada by produceState(id == null, id) {
        if (id != null) runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/song_lists?select=*&id=eq.$id")).firstOrNull()?.asObj() }.getOrNull()?.let { titulo = it.str("title"); nota = it.str("note"); abierta = it.bool("open") }
        value = true
    }
    Column(Modifier.fillMaxSize().background(Asfalto).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Enlace("‹ Cancelar", TextoClaro) { Nav.atras() }
        Text(if (id == null) "Nueva lista" else "Editar lista", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        if (!cargada) { CircularProgressIndicator(color = Amarillo); return@Column }
        Campo(titulo, { titulo = it }, "Nombre", max = 80, pista = "Ej: Para el viaje a la playa")
        Campo(nota, { nota = it }, "Descripción (opcional)", lineas = 3, max = 500)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { abierta = !abierta }) {
            Checkbox(abierta, { abierta = it }, colors = CheckboxDefaults.colors(checkedColor = Amarillo, checkmarkColor = Color(0xFF1B1E23)))
            Text("Lista abierta: cualquiera con cuenta puede agregar canciones")
        }
        Boton(if (id == null) "Crear lista" else "Guardar cambios", ocupado = guardando) {
            if (titulo.trim().length < 2) { Aviso.mostrar("Ponle un nombre de al menos 2 letras.", true); return@Boton }
            scope.launch {
                guardando = true
                try {
                    val b = mapOf("title" to titulo.trim(), "note" to nota.trim().ifEmpty { null }, "open" to abierta)
                    if (id != null) { Cuenta.api("PATCH", "/rest/v1/song_lists?id=eq.$id", b, "return=minimal"); Nav.atras() }
                    else {
                        val r = Json.arr(Cuenta.api("POST", "/rest/v1/song_lists", b + ("owner_id" to Cuenta.uid), "return=representation")).firstOrNull()?.asObj()
                        Nav.atras(); r?.let { Nav.ir(Destino.Lista(it.str("id"))) }; Aviso.mostrar("Lista creada. Ahora agrega canciones.")
                    }
                } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
                guardando = false
            }
        }
    }
}
