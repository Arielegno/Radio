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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* Chats personales: mensajes privados entre dos personas (necesita revista_supabase_v7.sql). */

data class Mensaje(val id: String, val de: String, val para: String, val texto: String, val foto: String, val leido: Boolean, val fecha: String)

object Chats {
    var sinLeer by mutableStateOf(0)
    private const val SEL = "id,sender,recipient,body,image,read_at,created_at"

    private fun mensaje(m: Map<String, Any?>) = Mensaje(m.str("id"), m.str("sender"), m.str("recipient"), m.str("body"), Diseno.imagenOk(m.str("image")), m.str("read_at").isNotEmpty(), m.str("created_at"))

    suspend fun contarSinLeer() { if (Cuenta.dentro) sinLeer = runCatching { Cuenta.contar("/rest/v1/messages?select=id&recipient=eq.${Cuenta.uid}&read_at=is.null") }.getOrDefault(0) }

    /** Tus últimos mensajes (para la lista de conversaciones). */
    suspend fun recientes(): List<Mensaje> = Json.arr(Cuenta.api("GET", "/rest/v1/messages?select=$SEL&or=(sender.eq.${Cuenta.uid},recipient.eq.${Cuenta.uid})&order=created_at.desc&limit=400")).mapNotNull { it.asObj()?.let(::mensaje) }

    suspend fun conversacion(otro: String): List<Mensaje> {
        val yo = Cuenta.uid
        return Json.arr(Cuenta.api("GET", "/rest/v1/messages?select=$SEL&or=(and(sender.eq.$yo,recipient.eq.$otro),and(sender.eq.$otro,recipient.eq.$yo))&order=created_at.desc&limit=150")).mapNotNull { it.asObj()?.let(::mensaje) }.reversed()
    }

    suspend fun marcarLeidos(otro: String) {
        runCatching { Cuenta.api("PATCH", "/rest/v1/messages?sender=eq.$otro&recipient=eq.${Cuenta.uid}&read_at=is.null", mapOf("read_at" to ahoraIso()), "return=minimal") }
        contarSinLeer()
    }

    suspend fun enviar(otro: String, texto: String, foto: String?) {
        try {
            Cuenta.api("POST", "/rest/v1/messages", mapOf("sender" to Cuenta.uid, "recipient" to otro, "body" to texto.trim().take(2000), "image" to foto), "return=minimal")
        } catch (e: ErrorRevista) {
            throw if (e.message == "No tienes permiso para hacer eso.") ErrorRevista("Esta persona no recibe mensajes tuyos: solo acepta de quien sigue, los tiene apagados o hay un bloqueo.") else e
        }
    }

    suspend fun perfiles(ids: Collection<String>): Map<String, Autor> {
        val l = ids.filter { Regex("^[0-9a-f-]{36}$").matches(it) }.distinct()
        if (l.isEmpty()) return emptyMap()
        return Json.arr(Cuenta.api("GET", "/rest/v1/profiles?select=id,name,avatar&id=in.(${l.joinToString(",")})")).mapNotNull { it.asObj() }
            .associate { it.str("id") to Autor(it.str("id"), it.str("name").ifBlank { "Alguien" }, Diseno.imagenOk(it.str("avatar"))) }
    }

    suspend fun bloqueado(otro: String): Boolean = runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/blocks?select=blocked&blocker=eq.${Cuenta.uid}&blocked=eq.$otro")).isNotEmpty() }.getOrDefault(false)
    suspend fun bloquear(otro: String, si: Boolean) {
        if (si) Cuenta.api("POST", "/rest/v1/blocks", mapOf("blocker" to Cuenta.uid, "blocked" to otro), "return=minimal,resolution=ignore-duplicates")
        else Cuenta.api("DELETE", "/rest/v1/blocks?blocker=eq.${Cuenta.uid}&blocked=eq.$otro")
    }
}

/** Botón de chats (con los mensajes sin leer) para la cabecera. */
@Composable
fun BotonChats() {
    LaunchedEffect(Cuenta.sesion) { while (true) { Chats.contarSinLeer(); delay(45_000) } }
    if (!Cuenta.dentro) return
    Box(Modifier.size(40.dp).clip(CircleShape).clickable { Nav.ir(Destino.Chats) }, contentAlignment = Alignment.Center) {
        Icono("chat", TextoClaro, 23.dp)
        if (Chats.sinLeer > 0) Text(if (Chats.sinLeer > 9) "9+" else "${Chats.sinLeer}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).clip(CircleShape).background(Color(0xFFE5604D)).padding(horizontal = 5.dp, vertical = 1.dp))
    }
}

private data class Conversacion(val otro: Autor, val ultimo: Mensaje, val sinLeer: Int)
private data class Contacto(val a: Autor, val etiqueta: String)

/** Chats y contactos. */
@Composable
fun ChatsPantalla() {
    var pestana by remember { mutableStateOf("chats") }
    var version by remember { mutableStateOf(0) }
    if (!Cuenta.dentro) { Column(Modifier.fillMaxSize().background(Asfalto).padding(20.dp)) { Enlace("‹ Volver", TextoClaro) { Nav.atras() }; Text("Entra a tu cuenta para usar los chats.", color = Tenue) }; return }
    val chats by produceState<Result<List<Conversacion>>?>(null, version) {
        value = runCatching {
            val ms = Chats.recientes(); val yo = Cuenta.uid
            val porOtro = ms.groupBy { if (it.de == yo) it.para else it.de }
            val ps = Chats.perfiles(porOtro.keys)
            porOtro.map { (o, l) -> Conversacion(ps[o] ?: Autor(o, "Alguien", ""), l.first(), l.count { it.para == yo && !it.leido }) }
        }
    }
    val contactos by produceState<Result<List<Contacto>>?>(null, version) {
        value = runCatching {
            val yo = Cuenta.uid
            fun lista(t: String) = Json.arr(t).mapNotNull { it.asObj()?.map("profiles") }.filter { it.str("id").isNotEmpty() }
            val sigo = lista(Cuenta.api("GET", "/rest/v1/follows?select=profiles!author_id(id,name,avatar)&follower_id=eq.$yo&limit=500"))
            val meSiguen = lista(Cuenta.api("GET", "/rest/v1/follows?select=profiles!follower_id(id,name,avatar)&author_id=eq.$yo&limit=500"))
            val idsS = sigo.map { it.str("id") }.toSet(); val idsM = meSiguen.map { it.str("id") }.toSet()
            (sigo + meSiguen).distinctBy { it.str("id") }.map { p ->
                val i = p.str("id")
                Contacto(Autor(i, p.str("name").ifBlank { "Alguien" }, Diseno.imagenOk(p.str("avatar"))), when { i in idsS && i in idsM -> "Se siguen"; i in idsS -> "Lo sigues"; else -> "Te sigue" })
            }.sortedWith(compareBy({ if (it.etiqueta == "Se siguen") 0 else 1 }, { it.a.nombre.lowercase() }))
        }
    }
    Column(Modifier.fillMaxSize().background(Asfalto)) {
        Column(Modifier.padding(16.dp, 12.dp, 16.dp, 6.dp)) {
            Enlace("‹ Volver", TextoClaro) { Nav.atras() }
            Text("Chats", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
            Pestanas(listOf("chats" to "Conversaciones", "contactos" to "Contactos", "ajustes" to "Privacidad"), pestana) { pestana = it }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (pestana) {
                "contactos" -> {
                    val r = contactos
                    when {
                        r == null -> item { Cargador() }
                        r.isFailure -> item { Text(r.exceptionOrNull()?.message ?: "", color = Tenue) }
                        r.getOrThrow().isEmpty() -> item { Text("Aún no tienes contactos. Sigue a alguien (o que te sigan) y aparecerá aquí.", color = Tenue) }
                        else -> items(r.getOrThrow(), key = { it.a.id }) { c ->
                            Fila(c.a, c.etiqueta, 0) { Nav.ir(Destino.Chat(c.a.id)) }
                        }
                    }
                }
                "ajustes" -> item { Privacidad() }
                else -> {
                    val r = chats
                    when {
                        r == null -> item { Cargador() }
                        r.isFailure -> item { Text(r.exceptionOrNull()?.message ?: "", color = Tenue) }
                        r.getOrThrow().isEmpty() -> item { Text("No tienes conversaciones. Abre «Contactos» o el perfil de alguien y toca «Mensaje».", color = Tenue) }
                        else -> items(r.getOrThrow(), key = { it.otro.id }) { c ->
                            val t = (if (c.ultimo.de == Cuenta.uid) "Tú: " else "") + c.ultimo.texto.ifBlank { "Foto" }
                            Fila(c.otro, t + " · " + haceTiempo(c.ultimo.fecha), c.sinLeer) { Nav.ir(Destino.Chat(c.otro.id)) }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { while (true) { delay(20_000); version++ } }
}

@Composable
private fun Cargador() = Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }

@Composable
private fun Fila(a: Autor, sub: String, sinLeer: Int, abrir: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).clickable(onClick = abrir).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(a, 46); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.nombre, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = if (sinLeer > 0) TextoClaro else Tenue, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (sinLeer > 0) FontWeight.SemiBold else FontWeight.Normal)
        }
        if (sinLeer > 0) Text("$sinLeer", color = Color(0xFF1B1E23), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.clip(CircleShape).background(Amarillo).padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
private fun Privacidad() {
    val scope = rememberCoroutineScope()
    var modo by remember { mutableStateOf(Cuenta.yo.str("dm", "todos")) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("¿Quién puede escribirte?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Fichas(listOf("todos" to "Todos", "sigo" to "Quien sigo", "nadie" to "Nadie"), modo) { k ->
            val antes = modo; modo = k
            scope.launch {
                try { Cuenta.api("PATCH", "/rest/v1/profiles?id=eq.${Cuenta.uid}", mapOf("dm" to k), "return=minimal"); Cuenta.social(true); Aviso.mostrar("Guardado.") }
                catch (e: Throwable) { modo = antes; Aviso.mostrar(e.message ?: "", true) }
            }
        }
        Text("Para bloquear a alguien, abre su chat y toca «Bloquear». Quien bloqueas no puede escribirte y tú tampoco a esa persona.", color = Tenue, fontSize = 14.sp)
    }
}

/** Una conversación con otra persona. */
@Composable
fun ChatPantalla(otro: String) {
    val scope = rememberCoroutineScope()
    var mensajes by remember { mutableStateOf<List<Mensaje>?>(null) }
    var error by remember { mutableStateOf("") }
    var texto by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<FotoElegida?>(null) }
    var enviando by remember { mutableStateOf(false) }
    var elegido by remember { mutableStateOf<String?>(null) }
    var denuncia by remember { mutableStateOf<String?>(null) }
    var bloq by remember { mutableStateOf(false) }
    var confirmarBloqueo by remember { mutableStateOf(false) }
    val persona by produceState<Autor?>(null, otro) { value = runCatching { Chats.perfiles(listOf(otro))[otro] }.getOrNull() ?: Autor(otro, "Alguien", "") }
    val lista = androidx.compose.foundation.lazy.rememberLazyListState()
    suspend fun cargar() {
        runCatching { Chats.conversacion(otro) }.onSuccess { l ->
            val nuevo = l.size != mensajes?.size || l.lastOrNull()?.id != mensajes?.lastOrNull()?.id
            mensajes = l; error = ""
            if (nuevo && l.isNotEmpty()) lista.scrollToItem(l.size - 1)
            if (l.any { it.para == Cuenta.uid && !it.leido }) Chats.marcarLeidos(otro)
        }.onFailure { if (mensajes == null) error = it.message ?: "" }
    }
    LaunchedEffect(otro) { bloq = Chats.bloqueado(otro); while (true) { cargar(); delay(4_000) } }
    if (!Cuenta.dentro) { Column(Modifier.fillMaxSize().background(Asfalto).padding(20.dp)) { Enlace("‹ Volver", TextoClaro) { Nav.atras() }; Text("Entra a tu cuenta para usar los chats.", color = Tenue) }; return }
    Column(Modifier.fillMaxSize().background(Asfalto).imePadding()) {
        Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { Nav.atras() }, contentAlignment = Alignment.Center) { Icono("atras", TextoClaro) }
            val p = persona
            Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { Nav.ir(Destino.Perfil(otro)) }.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (p != null) { Avatar(p, 36); Spacer(Modifier.width(10.dp)); Text(p.nombre, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Enlace(if (bloq) "Desbloquear" else "Bloquear", Tenue) { if (bloq) scope.launch { runCatching { Chats.bloquear(otro, false); bloq = false; Aviso.mostrar("Desbloqueado.") }.onFailure { Aviso.mostrar(it.message ?: "", true) } } else confirmarBloqueo = true }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val l = mensajes
            when {
                error.isNotEmpty() -> Text(error, color = Tenue, modifier = Modifier.padding(20.dp))
                l == null -> Cargador()
                l.isEmpty() -> Text("Escribe el primer mensaje. Sé amable: los mensajes se pueden denunciar.", color = Tenue, modifier = Modifier.padding(20.dp))
                else -> LazyColumn(Modifier.fillMaxSize(), state = lista, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(l, key = { it.id }) { m ->
                        val mio = m.de == Cuenta.uid
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mio) Alignment.End else Alignment.Start) {
                            Column(Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(18.dp)).background(if (mio) Amarillo else Panel).clickable { elegido = if (elegido == m.id) null else m.id }.padding(4.dp)) {
                                if (m.foto.isNotEmpty()) FotoPublicada(m.foto, Modifier.widthIn(max = 292.dp))
                                if (m.texto.isNotBlank()) Text(m.texto, color = if (mio) Color(0xFF1B1E23) else TextoClaro, fontSize = 16.sp, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                            if (elegido == m.id) Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(haceTiempo(m.fecha) + if (mio && m.leido) " · visto" else "", color = Tenue, fontSize = 12.sp)
                                if (mio) Enlace("borrar", Tenue) { scope.launch { runCatching { Cuenta.api("DELETE", "/rest/v1/messages?id=eq.${m.id}") }.onFailure { Aviso.mostrar(it.message ?: "", true) }; elegido = null; cargar() } }
                                else Enlace("denunciar", Tenue) { denuncia = m.id }
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(Panel).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            foto?.let { f -> EditorFoto(f) { foto = null } }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x14FFFFFF)).clickable { val e = elegirImagenes(1600, 600, false); scope.launch { foto = e.esperar().firstOrNull()?.let { FotoElegida(it) } } }, contentAlignment = Alignment.Center) { Icono("imagen", TextoClaro) }
                Box(Modifier.weight(1f)) { Campo(texto, { texto = it }, "Mensaje…", lineas = 1, max = 2000) }
                val puede = (texto.isNotBlank() || foto != null) && !enviando
                Box(Modifier.size(48.dp).clip(CircleShape).background(if (puede) Amarillo else Amarillo.copy(alpha = .4f)).clickable(enabled = puede) {
                    scope.launch {
                        enviando = true
                        try {
                            val url = foto?.let { subirFoto(it.lista(), "chat") }
                            Chats.enviar(otro, texto, url); texto = ""; foto = null; cargar()
                        } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
                        enviando = false
                    }
                }, contentAlignment = Alignment.Center) { if (enviando) CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF1B1E23), strokeWidth = 2.dp) else Icono("enviar", Color(0xFF1B1E23)) }
            }
        }
        Spacer(Modifier.height(0.dp))
    }
    if (confirmarBloqueo) DialogoConfirmar("¿Bloquear a ${persona?.nombre ?: "esta persona"}?", "No podrá escribirte y tú tampoco a esa persona. Puedes desbloquearla después.", "Bloquear", { confirmarBloqueo = false }) {
        confirmarBloqueo = false
        scope.launch { runCatching { Chats.bloquear(otro, true); bloq = true; Aviso.mostrar("Bloqueado.") }.onFailure { Aviso.mostrar(it.message ?: "", true) } }
    }
    denuncia?.let { DialogoDenuncia("message", it) { denuncia = null } }
}
