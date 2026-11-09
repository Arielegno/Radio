package rr.revista

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Resultados(val personas: List<Map<String, Any?>>, val articulos: List<Articulo>, val artistas: List<Map<String, Any?>>)

/** Buscar personas, artículos y artistas. Sin texto, muestra sugerencias de perfiles. */
@Composable
fun BuscarPantalla() {
    var q by remember { mutableStateOf("") }
    var res by remember { mutableStateOf<Result<Resultados>?>(null) }
    LaunchedEffect(q) {
        val t = q.trim().replace(Regex("[*,()%\\\\]"), " ").trim()
        if (t.length < 2) { res = null; return@LaunchedEffect }
        delay(350)
        res = null
        val pat = Supa.enc("*$t*")
        res = runCatching {
            Resultados(
                runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/profiles?select=id,name,avatar,bio,role&name=ilike.$pat&limit=20")).mapNotNull { it.asObj() } }.getOrDefault(emptyList()),
                runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/posts?select=id,title,subtitle,cover,style,review,tags,created_at,author_id,profiles!author_id(name,avatar),likes(count),comments(count)&title=ilike.$pat${Supa.publicados()}&order=created_at.desc&limit=15")).mapNotNull { it.asObj()?.let(Supa::deFila) } }.getOrDefault(emptyList()),
                runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/artists?select=id,name,genre&name=ilike.$pat&limit=15")).mapNotNull { it.asObj() } }.getOrDefault(emptyList()))
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Asfalto), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 30.dp)) {
        item {
            Column(Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Enlace("‹ Volver", TextoClaro) { Nav.atras() }
                Text("Buscar", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
                Campo(q, { q = it }, "Personas, artículos o artistas", max = 60)
            }
        }
        val r = res
        when {
            q.trim().length < 2 -> item { Sugerencias("Sugerencias para ti", Modifier.padding(top = 10.dp)) }
            r == null -> item { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) } }
            r.isFailure -> item { Text(r.exceptionOrNull()?.message ?: "", color = Tenue, modifier = Modifier.padding(16.dp)) }
            else -> {
                val d = r.getOrThrow()
                if (d.personas.isEmpty() && d.articulos.isEmpty() && d.artistas.isEmpty()) item { Text("No encontré nada con «${q.trim()}».", color = Tenue, modifier = Modifier.padding(16.dp)) }
                if (d.personas.isNotEmpty()) {
                    item { Titulo("Personas") }
                    items(d.personas, key = { "p" + it.str("id") }) { p -> FilaPersona(p) }
                }
                if (d.artistas.isNotEmpty()) {
                    item { Titulo("Artistas") }
                    items(d.artistas, key = { "a" + it.str("id") }) { a ->
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).clickable { Nav.ir(Destino.Artista(a.str("id"))) }.padding(12.dp)) {
                            Text(a.str("name"), fontWeight = FontWeight.Bold)
                            val g = sinPuntos(a.str("genre")); if (g.isNotEmpty()) Text(g, color = Tenue, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (d.articulos.isNotEmpty()) {
                    item { Titulo("Artículos") }
                    items(d.articulos, key = { "r" + it.id }) { a -> Tarjeta(a) { Nav.ir(Destino.Leer(a.id)) } }
                }
            }
        }
    }
}

@Composable
private fun Titulo(t: String) = Text(t.uppercase(), color = Amarillo, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))

@Composable
private fun FilaPersona(p: Map<String, Any?>) {
    val scope = rememberCoroutineScope()
    val id = p.str("id")
    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).clickable { Nav.ir(Destino.Perfil(id)) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(Autor(id, p.str("name"), Diseno.imagenOk(p.str("avatar"))), 44); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(p.str("name"), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOf(when (p.str("role")) { "admin" -> "Admin"; "editor" -> "Editor"; else -> "" }, p.str("bio")).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, color = Tenue, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (Cuenta.dentro && id != Cuenta.uid) Boton(if (id in Cuenta.sigo) "Siguiendo" else "Seguir", principal = id !in Cuenta.sigo) {
            scope.launch { try { Cuenta.alternarSeguir(id) } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } }
        }
    }
}

private data class Sugerido(val p: Map<String, Any?>, val motivo: String)

/** Perfiles que quizá quieras seguir: quien te sigue y no sigues, amigos de amigos y gente nueva. */
@Composable
fun Sugerencias(titulo: String, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val lista by produceState<List<Sugerido>?>(null, Cuenta.sesion) {
        value = runCatching {
            val yo = Cuenta.uid; val sigo = Cuenta.sigo
            val meSiguen = if (yo.isEmpty()) emptyList() else Json.arr(Cuenta.api("GET", "/rest/v1/follows?select=follower_id&author_id=eq.$yo&limit=200")).mapNotNull { it.asObj()?.str("follower_id") }
            val amigos = if (sigo.isEmpty()) emptyList() else Json.arr(Cuenta.api("GET", "/rest/v1/follows?select=author_id&follower_id=in.(${sigo.take(40).joinToString(",")})&limit=400")).mapNotNull { it.asObj()?.str("author_id") }
            val nuevos = Json.arr(Cuenta.api("GET", "/rest/v1/profiles?select=id,name,avatar,bio,banned&order=created_at.desc&limit=40")).mapNotNull { it.asObj() }
            val motivo = LinkedHashMap<String, String>()
            fun poner(k: String, v: String) { if (k !in motivo) motivo[k] = v }
            meSiguen.forEach { poner(it, "Te sigue") }
            amigos.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.forEach { poner(it.key, "Lo siguen ${it.value} de tus contactos".replace("Lo siguen 1 de tus contactos", "Lo sigue alguien que sigues")) }
            nuevos.forEach { poner(it.str("id"), "Nuevo en la Revista") }
            val ids = motivo.keys.filter { it != yo && it !in sigo }.take(15)
            val faltan = ids.filter { i -> nuevos.none { it.str("id") == i } }
            val perfiles = nuevos + if (faltan.isEmpty()) emptyList() else Json.arr(Cuenta.api("GET", "/rest/v1/profiles?select=id,name,avatar,bio,banned&id=in.(${faltan.joinToString(",")})")).mapNotNull { it.asObj() }
            ids.mapNotNull { i -> perfiles.firstOrNull { it.str("id") == i }?.takeIf { !it.bool("banned") }?.let { Sugerido(it, motivo[i] ?: "") } }
        }.getOrDefault(emptyList())
    }
    val l = lista?.filter { it.p.str("id") !in Cuenta.sigo } ?: return
    if (l.isEmpty()) return
    Column(modifier) {
        Text(titulo.uppercase(), color = Amarillo, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 18.dp, bottom = 8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            l.forEach { s ->
                val id = s.p.str("id")
                Column(Modifier.width(150.dp).clip(RoundedCornerShape(18.dp)).background(Panel).clickable { Nav.ir(Destino.Perfil(id)) }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Avatar(Autor(id, s.p.str("name"), Diseno.imagenOk(s.p.str("avatar"))), 64)
                    Text(s.p.str("name"), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    Text(s.motivo, color = Tenue, fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.Center, lineHeight = 15.sp)
                    if (Cuenta.dentro) Boton("Seguir", modifier = Modifier.fillMaxWidth()) { scope.launch { try { Cuenta.alternarSeguir(id) } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } } }
                }
            }
        }
    }
}
