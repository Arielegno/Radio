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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class DatosPerfil(val p: Map<String, Any?>, val seguidores: Int, val siguiendo: Int, val articulos: Int)
private data class Actividad(val texto: String, val sub: String, val post: String)

/** Perfil de una persona: foto, portada, bio, contadores y sus pestañas (como en la web). */
@Composable
fun PerfilPantalla(id: String) {
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf(0) }
    var pestana by remember(id) { mutableStateOf("posts") }
    var denunciar by remember { mutableStateOf(false) }
    var salir by remember { mutableStateOf(false) }
    val datos by produceState<Result<DatosPerfil?>?>(null, id, version) {
        value = runCatching {
            val p = Json.arr(Cuenta.api("GET", "/rest/v1/profiles?select=*&id=eq.$id")).firstOrNull()?.asObj() ?: return@runCatching null
            val mio = Cuenta.uid == id
            DatosPerfil(p, runCatching { Cuenta.contar("/rest/v1/follows?select=follower_id&author_id=eq.$id") }.getOrDefault(0),
                runCatching { Cuenta.contar("/rest/v1/follows?select=author_id&follower_id=eq.$id") }.getOrDefault(0),
                runCatching { Cuenta.contar("/rest/v1/posts?select=id&author_id=eq.$id" + if (mio) "" else Supa.publicados()) }.getOrDefault(0))
        }
    }
    val sel = "id,title,subtitle,cover,style,review,tags,created_at,author_id,profiles!author_id(name,avatar),likes(count),comments(count)"
    val lista by produceState<Result<Pair<List<Articulo>, List<Actividad>>>?>(null, id, pestana, version) {
        value = null
        value = runCatching {
            val mio = Cuenta.uid == id
            when (pestana) {
                "posts" -> Json.arr(Cuenta.api("GET", "/rest/v1/posts?select=$sel&author_id=eq.$id${if (mio) "" else Supa.publicados()}&order=created_at.desc&limit=30")).mapNotNull { it.asObj()?.let(Supa::deFila) } to emptyList()
                "likes" -> Json.arr(Cuenta.api("GET", "/rest/v1/likes?select=posts($sel)&user_id=eq.$id&limit=30")).mapNotNull { it.asObj()?.map("posts")?.takeIf { m -> m.isNotEmpty() }?.let(Supa::deFila) } to emptyList()
                "recs" -> Json.arr(Cuenta.api("GET", "/rest/v1/recommends?select=note,created_at,posts($sel)&user_id=eq.$id&order=created_at.desc&limit=30")).mapNotNull { it.asObj()?.map("posts")?.takeIf { m -> m.isNotEmpty() }?.let(Supa::deFila) } to emptyList()
                else -> emptyList<Articulo>() to Json.arr(Cuenta.api("GET", "/rest/v1/comments?select=id,body,created_at,post_id,posts(title)&author_id=eq.$id&order=created_at.desc&limit=30")).mapNotNull { it.asObj() }.map { c ->
                    Actividad(c.str("body"), "En «${c.map("posts").str("title").ifBlank { "artículo" }}» · ${Fechas.corta(c.str("created_at"))}", c.str("post_id"))
                }
            }
        }
    }
    val r = datos
    Box(Modifier.fillMaxSize().background(Asfalto)) {
        when {
            r == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }
            r.isFailure || r.getOrNull() == null -> Column(Modifier.padding(20.dp)) {
                Enlace("‹ Volver") { Nav.atras() }
                Text(if (r.isFailure) (r.exceptionOrNull()?.message ?: "") else "Ese perfil no existe.", color = Tenue, modifier = Modifier.padding(top = 12.dp))
            }
            else -> {
                val d = r.getOrThrow()!!; val p = d.p
                val mio = Cuenta.uid == id
                val est = Estilo.de(p.map("style")); val tema = Temas.de(est)
                val ban = imagen(Diseno.imagenOk(p.str("banner")))
                val av = imagen(Diseno.imagenOk(p.str("avatar")))
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Box(Modifier.fillMaxWidth().aspectRatio(2.6f).drawBehind { fondoTema(est, tema) }) {
                            if (ban != null) Image(ban, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Row(Modifier.padding(14.dp).clip(RoundedCornerShape(999.dp)).background(Color(0x59000000)).clickable { Nav.atras() }.padding(start = 8.dp, end = 14.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icono("atras", Color.White, 18.dp); Text("Volver", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Column(Modifier.padding(horizontal = 18.dp)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Box(Modifier.offset(y = (-40).dp).size(96.dp).clip(CircleShape).background(Asfalto).padding(4.dp).clip(CircleShape).background(Amarillo), contentAlignment = Alignment.Center) {
                                    if (av != null) Image(av, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    else Text(p.str("name").take(1).uppercase(), color = Color(0xFF1B1E23), fontWeight = FontWeight.Bold, fontSize = 38.sp)
                                }
                                Spacer(Modifier.weight(1f))
                                if (mio) Boton("Cerrar sesión", principal = false, modifier = Modifier.offset(y = (-24).dp)) { salir = true }
                                else Boton(if (id in Cuenta.sigo) "Siguiendo" else "Seguir", principal = id !in Cuenta.sigo, modifier = Modifier.offset(y = (-24).dp)) {
                                    if (Nav.necesitaCuenta()) scope.launch { try { Cuenta.alternarSeguir(id); version++ } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } }
                                }
                            }
                            Column(Modifier.offset(y = (-28).dp)) {
                                Text(p.str("name"), fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
                                val rol = when (p.str("role")) { "admin" -> "Admin"; "editor" -> "Editor"; else -> "" }
                                if (rol.isNotEmpty()) Text(rol, color = Color(0xFF1B1E23), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(999.dp)).background(Amarillo).padding(horizontal = 10.dp, vertical = 3.dp))
                                if (p.bool("banned")) Text("Cuenta suspendida", color = Color(0xFFFF8A7A), modifier = Modifier.padding(top = 6.dp))
                                if (p.str("bio").isNotBlank()) Text(p.str("bio"), fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 10.dp))
                                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    listOf("${d.seguidores}" to "seguidores", "${d.siguiendo}" to "siguiendo", "${d.articulos}" to "artículos").forEach { (n, t) ->
                                        Row { Text(n, fontWeight = FontWeight.Bold); Text(" $t", color = Tenue) }
                                    }
                                }
                                Text("desde ${Fechas.mesAnio(p.str("created_at")).lowercase()}", color = Tenue, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                                if (!mio) Enlace("Denunciar perfil", Tenue) { if (Nav.necesitaCuenta()) denunciar = true }
                            }
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("posts" to "Artículos", "recs" to "Recomendados", "comments" to "Comentarios", "likes" to "Me gusta").forEach { (k, n) ->
                                    val on = pestana == k
                                    Text(n, color = if (on) Color(0xFF1B1E23) else TextoClaro, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) Amarillo else Color.Transparent).border(1.dp, if (on) Amarillo else Color(0x33FFFFFF), RoundedCornerShape(999.dp)).clickable { pestana = k }.padding(horizontal = 14.dp, vertical = 8.dp))
                                }
                            }
                        }
                    }
                    val l = lista
                    when {
                        l == null -> item { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) } }
                        l.isFailure -> item { Text(l.exceptionOrNull()?.message ?: "", color = Tenue, modifier = Modifier.padding(18.dp)) }
                        else -> {
                            val (arts, acts) = l.getOrThrow()
                            if (arts.isEmpty() && acts.isEmpty()) item { Text(when (pestana) { "posts" -> "Aún no ha publicado artículos."; "recs" -> "Todavía no recomienda artículos."; "comments" -> "Sin comentarios todavía."; else -> "Todavía no le ha dado me gusta a nada." }, color = Tenue, modifier = Modifier.padding(18.dp)) }
                            items(arts, key = { "a" + it.id }) { a -> Tarjeta(a) { Nav.ir(Destino.Leer(a.id)) } }
                            items(acts, key = { "c" + it.post + it.texto.hashCode() }) { c ->
                                Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).clickable { Nav.ir(Destino.Leer(c.post)) }.padding(14.dp)) {
                                    Text(c.sub, color = Tenue, fontSize = 13.sp); Text(c.texto, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(30.dp)) }
                }
            }
        }
        if (denunciar) DialogoDenuncia("profile", id) { denunciar = false }
        if (salir) DialogoConfirmar("Cerrar sesión", "¿Salir de tu cuenta en este dispositivo?", "Cerrar sesión", { salir = false }) { Cuenta.salir(); salir = false; Nav.inicio(); Aviso.mostrar("Sesión cerrada.") }
    }
}
