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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val TIPOS = mapOf("post" to "Artículo", "comment" to "Comentario", "poster" to "Póster", "profile" to "Perfil", "artist" to "Artista", "poll" to "Debate", "event" to "Evento",
    "vault" to "Concierto de la Bóveda", "vcomment" to "Comentario de la Bóveda", "wall" to "Nota de un muro", "ccomment" to "Comentario del club", "list" to "Lista")
private val TABLAS = mapOf("post" to "posts", "comment" to "comments", "poster" to "posters", "artist" to "artists", "poll" to "polls", "event" to "events", "vault" to "vault",
    "vcomment" to "vault_comments", "wall" to "wall_posts", "ccomment" to "club_comments", "list" to "song_lists")
private val VER = mapOf("post" to "title", "comment" to "body", "poster" to "title", "profile" to "name", "artist" to "name", "poll" to "question", "event" to "title",
    "vault" to "title", "vcomment" to "body", "wall" to "body", "ccomment" to "body", "list" to "title")

/** Panel del administrador: denuncias, usuarios, estadísticas y ajustes (como en la web). */
@Composable
fun AdminPantalla() {
    var pestana by remember { mutableStateOf("reports") }
    if (!Cuenta.esAdmin) { Column(Modifier.fillMaxSize().background(Asfalto).padding(20.dp)) { Enlace("‹ Volver") { Nav.atras() }; Text("Solo el administrador puede ver esto.", color = Tenue) }; return }
    Column(Modifier.fillMaxSize().background(Asfalto).verticalScroll(rememberScrollState())) {
        Column(Modifier.padding(16.dp, 12.dp, 16.dp, 6.dp)) {
            Enlace("‹ Volver", TextoClaro) { Nav.atras() }
            Text("Admin", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
            Pestanas(listOf("reports" to "Denuncias", "users" to "Usuarios", "stats" to "Estadísticas", "settings" to "Ajustes"), pestana) { pestana = it }
        }
        when (pestana) { "users" -> Usuarios(); "stats" -> Estadisticas(); "settings" -> AjustesAdmin(); else -> Denuncias() }
        Spacer(Modifier.height(30.dp))
    }
}

private data class Grupo(val tipo: String, val id: String, val lista: List<Map<String, Any?>>, val que: String?)

@Composable
private fun Denuncias() {
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf(0) }
    var borrar by remember { mutableStateOf<Grupo?>(null) }
    val datos by produceState<Result<List<Grupo>>?>(null, version) {
        value = runCatching {
            val a = Json.arr(Cuenta.api("GET", "/rest/v1/reports?select=id,target_type,target_id,category,reason,created_at,profiles!reporter(name)&status=eq.abierta&order=created_at.desc&limit=200")).mapNotNull { it.asObj() }
            val g = a.groupBy { it.str("target_type") + ":" + it.str("target_id") }
            val nombres = HashMap<String, String>()
            g.keys.map { it.substringBefore(':') }.distinct().forEach { t ->
                val ids = g.keys.filter { it.startsWith("$t:") }.map { it.substringAfter(':') }.filter { Regex("^[0-9a-f-]{36}$").matches(it) }
                val tabla = if (t == "profile") "profiles" else TABLAS[t] ?: return@forEach; val campo = VER[t] ?: return@forEach
                if (ids.isNotEmpty()) runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/$tabla?select=id,$campo&id=in.(${ids.joinToString(",")})")).mapNotNull { it.asObj() }.forEach { r -> nombres["$t:${r.str("id")}"] = r.str(campo) } }
            }
            g.map { (k, l) -> Grupo(k.substringBefore(':'), k.substringAfter(':'), l, nombres[k]) }
        }
    }
    suspend fun cerrar(g: Grupo, estado: String) { Cuenta.api("PATCH", "/rest/v1/reports?target_type=eq.${g.tipo}&target_id=eq.${g.id}&status=eq.abierta", mapOf("status" to estado), "return=minimal") }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val r = datos
        when {
            r == null -> Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }
            r.isFailure -> Text(r.exceptionOrNull()?.message ?: "", color = Tenue)
            r.getOrThrow().isEmpty() -> Text("No hay denuncias pendientes.", color = Tenue)
            else -> r.getOrThrow().forEach { g ->
                val existe = g.que != null
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${TIPOS[g.tipo] ?: g.tipo} · ${g.lista.size} ${if (g.lista.size == 1) "denuncia" else "denuncias"}" + if (!existe) " · ya no existe" else "", color = Tenue, fontSize = 13.sp)
                    Text((g.que ?: "(eliminado)").take(160), fontWeight = FontWeight.Bold)
                    g.lista.forEach { d -> Text("• ${Cuenta.CATEGORIAS[d.str("category")] ?: d.str("category")}" + (d.str("reason").takeIf { it.isNotBlank() }?.let { ": $it" } ?: "") + " — ${d.map("profiles").str("name")}, ${Fechas.corta(d.str("created_at"))}", fontSize = 14.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (existe) Boton("Ver", principal = false) {
                            when (g.tipo) {
                                "post" -> Nav.ir(Destino.Leer(g.id)); "poster" -> Nav.ir(Destino.Poster(g.id)); "profile" -> Nav.ir(Destino.Perfil(g.id)); "artist" -> Nav.ir(Destino.Artista(g.id))
                                "vault" -> Nav.ir(Destino.Concierto(g.id)); "poll" -> Nav.ir(Destino.Debates); "event" -> Nav.ir(Destino.Agenda)
                                else -> scope.launch {
                                    val (tabla, col, dest) = when (g.tipo) { "comment" -> Triple("comments", "post_id", "leer"); "vcomment" -> Triple("vault_comments", "vault_id", "boveda"); "wall" -> Triple("wall_posts", "owner_id", "perfil"); else -> Triple("club_comments", "club_id", "club") }
                                    val x = runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/$tabla?select=$col&id=eq.${g.id}")).firstOrNull()?.asObj()?.str(col) }.getOrNull()
                                    if (x == null) Aviso.mostrar("Ya no existe.") else when (dest) { "leer" -> Nav.ir(Destino.Leer(x)); "boveda" -> Nav.ir(Destino.Concierto(x)); "perfil" -> Nav.ir(Destino.Perfil(x)); else -> Nav.ir(Destino.Club) }
                                }
                            }
                        }
                        if (existe) Boton(if (g.tipo == "profile") "Suspender cuenta" else "Eliminar contenido", principal = false) { borrar = g }
                        Boton(if (existe) "Descartar" else "Cerrar", principal = false) { scope.launch { runCatching { cerrar(g, if (existe) "descartada" else "resuelta") }; Aviso.mostrar("Denuncia cerrada."); version++ } }
                    }
                }
            }
        }
    }
    borrar?.let { g ->
        DialogoConfirmar(if (g.tipo == "profile") "¿Suspender esta cuenta?" else "¿Eliminar este ${(TIPOS[g.tipo] ?: "").lowercase()}?", if (g.tipo == "profile") "Podrá leer, pero no publicar, comentar, dar me gusta ni seguir." else "Se borra para todos y la denuncia queda resuelta.",
            if (g.tipo == "profile") "Suspender" else "Eliminar", { borrar = null }) {
            borrar = null
            scope.launch {
                try {
                    if (g.tipo == "profile") Cuenta.api("PATCH", "/rest/v1/profiles?id=eq.${g.id}", mapOf("banned" to true), "return=minimal")
                    else Cuenta.api("DELETE", "/rest/v1/${TABLAS[g.tipo]}?id=eq.${g.id}")
                    cerrar(g, "resuelta"); Aviso.mostrar(if (g.tipo == "profile") "Cuenta suspendida." else "Contenido eliminado."); version++
                } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
            }
        }
    }
}

@Composable
private fun Usuarios() {
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var version by remember { mutableStateOf(0) }
    var lista by remember { mutableStateOf<List<Map<String, Any?>>?>(null) }
    LaunchedEffect(q, version) {
        kotlinx.coroutines.delay(if (q.isEmpty()) 0 else 350)
        val t = q.trim().replace(Regex("[*,()%]"), " ").trim()
        lista = runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/profiles?select=id,name,avatar,role,banned,created_at&order=created_at.desc&limit=60" + if (t.isNotEmpty()) "&name=ilike." + Supa.enc("*$t*") else "")).mapNotNull { it.asObj() } }.getOrDefault(emptyList())
    }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Campo(q, { q = it }, "Buscar usuario por nombre…", max = 40)
        val l = lista
        if (l == null) CircularProgressIndicator(color = Amarillo)
        else if (l.isEmpty()) Text("Sin resultados.", color = Tenue)
        else l.forEach { u ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(Autor(u.str("id"), u.str("name"), Diseno.imagenOk(u.str("avatar"))), 40); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable { Nav.ir(Destino.Perfil(u.str("id"))) }) {
                    Text(u.str("name"), fontWeight = FontWeight.Bold)
                    Text(when (u.str("role")) { "admin" -> "Admin"; "editor" -> "Editor"; else -> "Usuario" } + (if (u.bool("banned")) " · suspendida" else "") + " · desde ${Fechas.mesAnio(u.str("created_at")).lowercase()}", color = if (u.bool("banned")) Color(0xFFFF8A7A) else Tenue, fontSize = 13.sp)
                }
                if (u.str("role") != "admin" && u.str("id") != Cuenta.uid) Column(horizontalAlignment = Alignment.End) {
                    Enlace(if (u.bool("banned")) "Reactivar" else "Suspender") { scope.launch { runCatching { Cuenta.api("PATCH", "/rest/v1/profiles?id=eq.${u.str("id")}", mapOf("banned" to !u.bool("banned")), "return=minimal") }.onFailure { Aviso.mostrar(it.message ?: "", true) }; version++ } }
                    Enlace(if (u.str("role") == "editor") "Quitar editor" else "Hacer editor", Tenue) { scope.launch { runCatching { Cuenta.api("PATCH", "/rest/v1/profiles?id=eq.${u.str("id")}", mapOf("role" to if (u.str("role") == "editor") "user" else "editor"), "return=minimal") }.onFailure { Aviso.mostrar(it.message ?: "", true) }; version++ } }
                }
            }
        }
    }
}

@Composable
private fun Estadisticas() {
    val datos by produceState<Result<Pair<List<Pair<String, Int>>, List<Map<String, Any?>>>>?>(null) {
        value = runCatching {
            val n = listOf("cuentas" to "/rest/v1/profiles?select=id", "artículos" to "/rest/v1/posts?select=id", "comentarios" to "/rest/v1/comments?select=id", "pósters" to "/rest/v1/posters?select=id",
                "artistas" to "/rest/v1/artists?select=id", "denuncias abiertas" to "/rest/v1/reports?select=id&status=eq.abierta").map { (k, q) -> k to runCatching { Cuenta.contar(q) }.getOrDefault(0) }
            n to runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/posts?select=id,title,views&views=gt.0&order=views.desc&limit=5")).mapNotNull { it.asObj() } }.getOrDefault(emptyList())
        }
    }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val r = datos
        when {
            r == null -> CircularProgressIndicator(color = Amarillo)
            r.isFailure -> Text(r.exceptionOrNull()?.message ?: "", color = Tenue)
            else -> {
                val (n, leidos) = r.getOrThrow()
                n.chunked(2).forEach { fila -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { fila.forEach { (k, v) -> Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)) { Text("$v", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Amarillo); Text(k, color = Tenue) } } } }
                if (leidos.isNotEmpty()) { Text("Más leídos", fontWeight = FontWeight.Bold, fontSize = 18.sp); leidos.forEach { p -> Row(Modifier.fillMaxWidth().clickable { Nav.ir(Destino.Leer(p.str("id"))) }.padding(vertical = 6.dp)) { Text(p.str("title"), modifier = Modifier.weight(1f)); Text("${p.num("views")} lecturas", color = Tenue) } } }
            }
        }
    }
}

@Composable
private fun AjustesAdmin() {
    val scope = rememberCoroutineScope()
    var cargado by remember { mutableStateOf(false) }
    var soloInv by remember { mutableStateOf(false) }
    var codigo by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/rr_settings?select=invite_only,invite_code&id=eq.1")).firstOrNull()?.asObj() }.getOrNull()?.let { soloInv = it.bool("invite_only"); codigo = it.str("invite_code") }
        cargado = true
    }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!cargado) { CircularProgressIndicator(color = Amarillo); return@Column }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { soloInv = !soloInv }) {
            Checkbox(soloInv, { soloInv = it }, colors = CheckboxDefaults.colors(checkedColor = Amarillo, checkmarkColor = Color(0xFF1B1E23)))
            Text("Solo con invitación (las cuentas nuevas necesitan un código)", fontWeight = FontWeight.SemiBold)
        }
        Campo(codigo, { codigo = it }, "Código de invitación", max = 40, pista = "Ej: rock2026")
        Text("Compártelo solo con quien quieras dejar entrar. Las cuentas que ya existen no se ven afectadas.", color = Tenue, fontSize = 14.sp)
        Boton("Guardar") {
            if (soloInv && codigo.trim().length < 4) { Aviso.mostrar("El código debe tener al menos 4 caracteres.", true); return@Boton }
            scope.launch { try { Cuenta.api("PATCH", "/rest/v1/rr_settings?id=eq.1", mapOf("invite_only" to soloInv, "invite_code" to codigo.trim().ifEmpty { null }), "return=minimal"); Aviso.mostrar(if (soloInv) "Listo: la revista ahora es solo con invitación." else "Listo: cualquiera puede crear una cuenta.") } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } }
        }
        Text("Límites automáticos contra spam: por persona y por hora, 10 artículos, 30 comentarios, 15 pósters y 20 denuncias. Tú no tienes límite.", color = Tenue, fontSize = 14.sp)
    }
}
