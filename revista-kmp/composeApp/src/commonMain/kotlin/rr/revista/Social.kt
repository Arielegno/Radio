package rr.revista

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** «hace 5 min», «hace 3 h», «hace 2 d» o la fecha (como revAgo en la web). */
fun haceTiempo(iso: String): String {
    val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):?(\\d{2})?").find(iso) ?: return Fechas.corta(iso)
    val (a, mes, d, h, mi) = m.destructured
    // días desde 1970 de esa fecha (UTC) para comparar con ahora
    fun dias(y: Int, mo: Int, dd: Int): Long { var t = 0L; for (yy in 1970 until y) t += if ((yy % 4 == 0 && yy % 100 != 0) || yy % 400 == 0) 366 else 365
        val dm = intArrayOf(31, if ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31); for (k in 0 until mo - 1) t += dm[k]; return t + dd - 1 }
    val ms = (dias(a.toInt(), mes.toInt(), d.toInt()) * 86400 + h.toInt() * 3600 + mi.toInt() * 60) * 1000.0
    val min = ((ahoraMs() - ms) / 60000).toLong()
    return when { min < 1 -> "ahora"; min < 60 -> "hace $min min"; min < 1440 -> "hace ${min / 60} h"; min < 10080 -> "hace ${min / 1440} d"; else -> Fechas.corta(iso) }
}

@Composable
private fun Encabezado(titulo: String, sub: String = "") {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)) {
        Enlace("‹ Volver", TextoClaro) { Nav.atras() }
        Text(titulo, fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        if (sub.isNotEmpty()) Text(sub, color = Tenue, fontSize = 14.sp)
    }
}

@Composable
private fun Cargando() = Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }

/* ---------------- Notificaciones ---------------- */
private val TIPOS_NOTIF = mapOf("like" to "le dio me gusta a", "comment" to "comentó en", "follow" to "empezó a seguirte", "reply" to "respondió tu comentario en", "recommend" to "recomendó",
    "wall" to "escribió en tu muro", "mention" to "te mencionó en un muro", "wreply" to "respondió tu nota del muro")

@Composable
fun NotificacionesPantalla() {
    val datos by produceState<Result<List<Map<String, Any?>>>?>(null) {
        value = runCatching {
            val base = "/rest/v1/notifications?select=id,kind,read,created_at,post_id,actor_id,actor:profiles!notif_actor_fk(name,avatar),posts(title)"; val cola = "&user_id=eq.${Cuenta.uid}&order=created_at.desc&limit=60"
            val a = Json.arr(runCatching { Cuenta.api("GET", "$base,wall_owner,vault_id,vault(title)$cola") }.getOrElse { Cuenta.api("GET", base + cola) }).mapNotNull { it.asObj() }
            if (a.any { !it.bool("read") }) runCatching { Cuenta.api("PATCH", "/rest/v1/notifications?user_id=eq.${Cuenta.uid}&read=eq.false", mapOf("read" to true), "return=minimal") }
            Social.sinLeer = 0
            a
        }
    }
    Column(Modifier.fillMaxSize().background(Asfalto).verticalScroll(rememberScrollState())) {
        Encabezado("Notificaciones")
        val r = datos
        when {
            r == null -> Cargando()
            r.isFailure -> Text(r.exceptionOrNull()?.message ?: "", color = Tenue, modifier = Modifier.padding(16.dp))
            r.getOrThrow().isEmpty() -> Text("Aún no tienes notificaciones. Cuando alguien te siga, comente o te dé me gusta, aparecerá aquí.", color = Tenue, modifier = Modifier.padding(16.dp))
            else -> r.getOrThrow().forEach { n ->
                val k = n.str("kind"); val actor = n.map("actor")
                val (texto, ir) = when (k) {
                    "vault", "vault_live" -> ((if (k == "vault") "Ya está en cartelera " else "Empieza pronto la función en vivo de ") + "«${n.map("vault").str("title").ifBlank { "un concierto" }}»") to { Nav.ir(Destino.Concierto(n.str("vault_id"))) }
                    else -> {
                        val muro = k in setOf("wall", "mention", "wreply")
                        val t = "${actor.str("name").ifBlank { "Alguien" }} ${TIPOS_NOTIF[k] ?: ""}" + (n.map("posts").str("title").takeIf { it.isNotBlank() }?.let { " «$it»" } ?: "")
                        t to { if (n.str("post_id").isNotBlank()) Nav.ir(Destino.Leer(n.str("post_id"))) else Nav.ir(Destino.Perfil(if (muro) n.str("wall_owner").ifBlank { Cuenta.uid } else n.str("actor_id"))) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(if (n.bool("read")) Panel else Color(0x26F2C230)).clickable { ir() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (k.startsWith("vault")) Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) { Icono("pelicula", Amarillo, 22.dp) }
                    else Avatar(Autor(n.str("actor_id"), actor.str("name"), Diseno.imagenOk(actor.str("avatar"))), 40)
                    Spacer(Modifier.width(10.dp))
                    Column { Text(texto, fontSize = 15.sp); Text(haceTiempo(n.str("created_at")), color = Tenue, fontSize = 12.sp) }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

object Social {
    var sinLeer by mutableStateOf(0)
    suspend fun contarSinLeer() { if (Cuenta.dentro) sinLeer = runCatching { Cuenta.contar("/rest/v1/notifications?select=id&user_id=eq.${Cuenta.uid}&read=eq.false") }.getOrDefault(0) }
}

/** Campana con el número de notificaciones sin leer. */
@Composable
fun Campana() {
    androidx.compose.runtime.LaunchedEffect(Cuenta.sesion) { Social.contarSinLeer() }
    if (!Cuenta.dentro) return
    Box(Modifier.size(40.dp).clip(CircleShape).clickable { Nav.ir(Destino.Notificaciones) }, contentAlignment = Alignment.Center) {
        IconoCampana(TextoClaro)
        if (Social.sinLeer > 0) Text(if (Social.sinLeer > 9) "9+" else "${Social.sinLeer}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).clip(CircleShape).background(Color(0xFFE5604D)).padding(horizontal = 5.dp, vertical = 1.dp))
    }
}

@Composable
private fun IconoCampana(c: Color) = androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
    val w = size.width; val s = androidx.compose.ui.graphics.drawscope.Stroke(w * .09f)
    val p = androidx.compose.ui.graphics.Path().apply { moveTo(w * .2f, w * .72f); lineTo(w * .8f, w * .72f); cubicTo(w * .7f, w * .6f, w * .74f, w * .48f, w * .72f, w * .38f); cubicTo(w * .68f, w * .18f, w * .32f, w * .18f, w * .28f, w * .38f); cubicTo(w * .26f, w * .48f, w * .3f, w * .6f, w * .2f, w * .72f); close() }
    drawPath(p, c, style = s); drawCircle(c, w * .07f, androidx.compose.ui.geometry.Offset(w * .5f, w * .84f))
}

/* ---------------- Debates (encuestas) ---------------- */
@Composable
fun DebatesPantalla() {
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf(0) }
    var nuevo by remember { mutableStateOf(false) }
    var denuncia by remember { mutableStateOf<String?>(null) }
    val datos by produceState<Result<Triple<List<Map<String, Any?>>, Map<String, List<Int>>, Map<String, Int>>>?>(null, version) {
        value = runCatching {
            val a = Json.arr(Cuenta.api("GET", "/rest/v1/polls?select=id,question,options,closes_at,created_at,author_id,profiles!author_id(name,avatar)&order=created_at.desc&limit=30")).mapNotNull { it.asObj() }
            val ids = a.map { it.str("id") }
            val res = if (ids.isEmpty()) emptyMap() else Json.arr(Cuenta.api("POST", "/rest/v1/rpc/rr_polls_results", mapOf("ids" to ids))).mapNotNull { it.asObj() }.associate { it.str("poll_id") to it.list("counts").map { n -> (n as? Number)?.toInt() ?: 0 } }
            val mios = if (!Cuenta.dentro || ids.isEmpty()) emptyMap() else Json.arr(Cuenta.api("GET", "/rest/v1/poll_votes?select=poll_id,option&user_id=eq.${Cuenta.uid}")).mapNotNull { it.asObj() }.associate { it.str("poll_id") to it.num("option").toInt() }
            Triple(a, res, mios)
        }
    }
    Column(Modifier.fillMaxSize().background(Asfalto).verticalScroll(rememberScrollState())) {
        Encabezado("Debates", "Tu voto es privado: solo se ven los totales.")
        Box(Modifier.padding(horizontal = 16.dp)) { Boton("+ Nuevo debate") { if (Nav.necesitaCuenta()) nuevo = true } }
        val r = datos
        when {
            r == null -> Cargando()
            r.isFailure -> Text(r.exceptionOrNull()?.message ?: "", color = Tenue, modifier = Modifier.padding(16.dp))
            r.getOrThrow().first.isEmpty() -> Text("Todavía no hay debates. Abre el primero.", color = Tenue, modifier = Modifier.padding(16.dp))
            else -> {
                val (a, res, mios) = r.getOrThrow()
                a.forEach { p ->
                    val cs = res[p.str("id")] ?: emptyList(); val tot = cs.sum(); val mio = mios[p.str("id")]
                    val cerrado = p.str("closes_at").isNotBlank() && p.str("closes_at") < ahoraIso(); val ver = mio != null || cerrado
                    Column(Modifier.fillMaxWidth().padding(16.dp, 6.dp).clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { Nav.ir(Destino.Perfil(p.str("author_id"))) }) {
                            Avatar(Autor(p.str("author_id"), p.map("profiles").str("name"), Diseno.imagenOk(p.map("profiles").str("avatar"))), 22); Spacer(Modifier.width(6.dp))
                            Text("${p.map("profiles").str("name")} · ${haceTiempo(p.str("created_at"))}" + if (cerrado) " · cerrado" else if (p.str("closes_at").isNotBlank()) " · cierra ${Fechas.corta(p.str("closes_at"))}" else "", color = Tenue, fontSize = 13.sp)
                        }
                        Text(p.str("question"), fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        p.list("options").forEachIndexed { i, o ->
                            val n = cs.getOrElse(i) { 0 }; val pc = if (tot > 0) n * 100 / tot else 0
                            Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x14FFFFFF)).border(if (mio == i) 2.dp else 0.dp, Amarillo, RoundedCornerShape(12.dp))
                                .clickable(enabled = !cerrado) {
                                    if (Nav.necesitaCuenta()) scope.launch {
                                        try { val id = p.str("id")
                                            if (mio != null) Cuenta.api("PATCH", "/rest/v1/poll_votes?poll_id=eq.$id&user_id=eq.${Cuenta.uid}", mapOf("option" to i), "return=minimal")
                                            else Cuenta.api("POST", "/rest/v1/poll_votes", mapOf("poll_id" to id, "user_id" to Cuenta.uid, "option" to i), "return=minimal"); version++ } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
                                    }
                                }) {
                                if (ver) Box(Modifier.fillMaxHeight().fillMaxWidth(pc / 100f).background(Amarillo.copy(alpha = .3f)))
                                Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(o.toString(), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); if (ver) Text("$pc%", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$tot ${if (tot == 1) "voto" else "votos"}" + if (mio != null && !cerrado) " · toca otra opción para cambiar tu voto" else "", color = Tenue, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            if (Cuenta.uid == p.str("author_id") || Cuenta.esAdmin) Enlace("Borrar", Tenue) { scope.launch { runCatching { Cuenta.api("DELETE", "/rest/v1/polls?id=eq.${p.str("id")}") }; version++ } }
                            if (Cuenta.dentro && Cuenta.uid != p.str("author_id")) Enlace("Denunciar", Tenue) { denuncia = p.str("id") }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
    denuncia?.let { DialogoDenuncia("poll", it) { denuncia = null } }
    if (nuevo) {
        var q by remember { mutableStateOf("") }; var ops by remember { mutableStateOf("") }; var dias by remember { mutableStateOf("") }
        Hoja("Nuevo debate", { nuevo = false }) {
            Campo(q, { q = it }, "Pregunta", max = 200, pista = "¿Mejor disco de 1991?")
            Campo(ops, { ops = it }, "Opciones (una por línea, de 2 a 6)", lineas = 5, max = 600, pista = "Nevermind\nTen\nLoveless")
            Campo(dias, { dias = it.filter(Char::isDigit) }, "Cerrar en cuántos días (vacío = nunca)", max = 3)
            Boton("Publicar", modifier = Modifier.fillMaxWidth()) {
                val o = ops.split('\n').map { it.trim().take(80) }.filter { it.isNotEmpty() }.distinct().take(6)
                if (q.trim().length < 5) { Aviso.mostrar("Escribe una pregunta (mínimo 5 letras).", true); return@Boton }
                if (o.size < 2) { Aviso.mostrar("Pon al menos 2 opciones distintas.", true); return@Boton }
                scope.launch {
                    try { val d = dias.toIntOrNull() ?: 0
                        Cuenta.api("POST", "/rest/v1/polls", mapOf("author_id" to Cuenta.uid, "question" to q.trim().take(200), "options" to o, "closes_at" to if (d > 0) isoDesdeMs(ahoraMs() + d * 86_400_000.0) else null), "return=minimal")
                        Aviso.mostrar("Debate publicado."); nuevo = false; version++ } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
                }
            }
        }
    }
}

/* ---------------- Agenda de conciertos ---------------- */
@Composable
fun AgendaPantalla() {
    val scope = rememberCoroutineScope()
    var modo by remember { mutableStateOf("list") }
    var version by remember { mutableStateOf(0) }
    var nuevo by remember { mutableStateOf(false) }
    var denuncia by remember { mutableStateOf<String?>(null) }
    val datos by produceState<Result<List<Map<String, Any?>>>?>(null, modo, version) {
        value = null
        value = runCatching {
            val desde = Supa.enc(isoDesdeMs(ahoraMs() - 6 * 3600_000.0))
            val q = if (modo == "past") "starts_at=lt.$desde&order=starts_at.desc" else "starts_at=gte.$desde&order=starts_at.asc"
            Json.arr(Cuenta.api("GET", "/rest/v1/events?select=*,profiles!created_by(name),posters(thumb_url,image_url)&$q&limit=200")).mapNotNull { it.asObj() }
        }
    }
    Column(Modifier.fillMaxSize().background(Asfalto).verticalScroll(rememberScrollState())) {
        Encabezado("Agenda", "Conciertos y festivales que publica la comunidad")
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pestanas(listOf("list" to "Próximos", "past" to "Pasados"), modo) { modo = it }
            Spacer(Modifier.weight(1f)); Boton("+ Evento") { if (Nav.necesitaCuenta()) nuevo = true }
        }
        val r = datos
        when {
            r == null -> Cargando()
            r.isFailure -> Text(r.exceptionOrNull()?.message ?: "", color = Tenue, modifier = Modifier.padding(16.dp))
            r.getOrThrow().isEmpty() -> Text(if (modo == "past") "Aún no hay eventos pasados." else "No hay conciertos próximos en la agenda. Añade uno con «+ Evento».", color = Tenue, modifier = Modifier.padding(16.dp))
            else -> {
                var mes = ""
                r.getOrThrow().forEach { e ->
                    val f = e.str("starts_at"); val m = Fechas.mesAnio(f)
                    if (m != mes) { mes = m; Text(m, color = Amarillo, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp)) }
                    val th = Diseno.imagenOk(e.map("posters").str("thumb_url")).ifEmpty { Diseno.imagenOk(e.map("posters").str("image_url")) }
                    Row(Modifier.fillMaxWidth().padding(16.dp, 4.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(12.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.width(54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(f.substring(8, 10).trimStart('0'), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold); Text(f.substring(11, 16) + " UTC", color = Tenue, fontSize = 11.sp)
                        }
                        Column(Modifier.weight(1f).padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(e.str("title"), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(listOf(e.str("artist"), e.str("venue"), e.str("city"), e.str("country")).filter { it.isNotBlank() }.joinToString(" · "), color = Tenue, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (e.str("url").startsWith("https://")) Enlace("Info") { abrirEnlace(e.str("url")) }
                                Enlace("+ Calendario") {
                                    val ini = f.replace("-", "").replace(":", "").take(15) + "Z"
                                    abrirEnlace("https://calendar.google.com/calendar/render?action=TEMPLATE&text=" + Supa.enc(e.str("title")) + "&dates=$ini/$ini&location=" + Supa.enc(listOf(e.str("venue"), e.str("city"), e.str("country")).filter { it.isNotBlank() }.joinToString(", ")))
                                }
                                if (Cuenta.uid == e.str("created_by") || Cuenta.esAdmin) Enlace("Borrar", Tenue) { scope.launch { runCatching { Cuenta.api("DELETE", "/rest/v1/events?id=eq.${e.str("id")}") }; version++ } }
                                if (Cuenta.dentro && Cuenta.uid != e.str("created_by")) Enlace("Denunciar", Tenue) { denuncia = e.str("id") }
                            }
                        }
                        if (th.isNotEmpty()) { val img = imagen(th); Box(Modifier.size(60.dp, 84.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x22FFFFFF))) { if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) } }
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
    denuncia?.let { DialogoDenuncia("event", it) { denuncia = null } }
    if (nuevo) {
        var t by remember { mutableStateOf("") }; var a by remember { mutableStateOf("") }; var v by remember { mutableStateOf("") }; var c by remember { mutableStateOf("") }
        var p by remember { mutableStateOf(leerDato("rvPais") ?: "") }; var d by remember { mutableStateOf("") }; var h by remember { mutableStateOf("20:00") }; var u by remember { mutableStateOf("") }
        var guardando by remember { mutableStateOf(false) }
        Hoja("Nuevo concierto o festival", { nuevo = false }) {
            Campo(t, { t = it }, "Nombre del evento", max = 120); Campo(a, { a = it }, "Banda o artista", max = 80); Campo(v, { v = it }, "Lugar", max = 100, pista = "Ej: Teatro Nacional")
            Campo(c, { c = it }, "Ciudad", max = 80); Campo(p, { p = it }, "País", max = 60)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { Campo(d, { d = it }, "Fecha (AAAA-MM-DD)", max = 10) }; Box(Modifier.weight(1f)) { Campo(h, { h = it }, "Hora (HH:MM)", max = 5) } }
            Campo(u, { u = it }, "Enlace con información (https://…, opcional)", max = 300)
            Text("La hora se guarda como hora de tu teléfono convertida a UTC por la web; aquí se guarda tal cual en UTC.", color = Tenue, fontSize = 12.sp)
            Boton("Publicar", ocupado = guardando, modifier = Modifier.fillMaxWidth()) {
                if (t.trim().length < 3) { Aviso.mostrar("Escribe el nombre del evento.", true); return@Boton }
                if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(d.trim()) || !Regex("^\\d{2}:\\d{2}$").matches(h.trim())) { Aviso.mostrar("La fecha debe ser AAAA-MM-DD y la hora HH:MM.", true); return@Boton }
                if (u.isNotBlank() && !Regex("^https://\\S+$").matches(u.trim())) { Aviso.mostrar("El enlace debe empezar con https://", true); return@Boton }
                scope.launch {
                    guardando = true
                    try {
                        if (p.isNotBlank()) guardarDato("rvPais", p.trim().take(60))
                        var lat: Double? = null; var lng: Double? = null
                        if (c.isNotBlank()) runCatching {
                            val g = Json.arr(httpPedido("GET", "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + Supa.enc(listOf(c, p).filter { it.isNotBlank() }.joinToString(", ")), emptyMap(), null).texto).firstOrNull()?.asObj()
                            lat = g?.str("lat")?.toDoubleOrNull(); lng = g?.str("lon")?.toDoubleOrNull()
                        }
                        Cuenta.api("POST", "/rest/v1/events", mapOf("title" to t.trim().take(120), "artist" to a.trim().take(80).ifEmpty { null }, "venue" to v.trim().take(100).ifEmpty { null }, "city" to c.trim().take(80).ifEmpty { null },
                            "country" to p.trim().take(60).ifEmpty { null }, "starts_at" to "${d.trim()}T${h.trim()}:00Z", "url" to u.trim().ifEmpty { null }, "lat" to lat, "lng" to lng, "created_by" to Cuenta.uid), "return=minimal")
                        Aviso.mostrar("Evento publicado."); nuevo = false; version++
                    } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
                    guardando = false
                }
            }
        }
    }
}

/* ---------------- Bóveda: ficha de un concierto ---------------- */
private fun pagina(prov: String, vid: String): String = when {
    prov == "youtube" && Regex("^[\\w-]{6,20}$").matches(vid) -> "https://www.youtube.com/watch?v=$vid"
    prov == "vimeo" && Regex("^\\d{3,15}$").matches(vid) -> "https://vimeo.com/$vid"
    prov == "archive" && Regex("^[\\w.-]{3,100}$").matches(vid) -> "https://archive.org/details/$vid"
    else -> ""
}
private fun reloj(n: Int): String { val h = n / 3600; val m = n % 3600 / 60; val s = (n % 60).toString().padStart(2, '0'); return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$s" else "$m:$s" }

@Composable
fun ConciertoPantalla(id: String) {
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf(0) }
    var texto by remember { mutableStateOf("") }
    var denuncia by remember { mutableStateOf<Pair<String, String>?>(null) }
    val datos by produceState<Result<Triple<Map<String, Any?>, List<Map<String, Any?>>, Boolean>?>?>(null, id, version) {
        value = runCatching {
            val v = Json.arr(Cuenta.api("GET", "/rest/v1/vault?select=*,profiles!created_by(name,avatar)&id=eq.$id")).firstOrNull()?.asObj() ?: return@runCatching null
            val cm = Json.arr(Cuenta.api("GET", "/rest/v1/vault_comments?select=*,profiles!author_id(name,avatar)&vault_id=eq.$id&order=created_at.asc&limit=300")).mapNotNull { it.asObj() }
            val aviso = Cuenta.dentro && runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/vault_watch?select=vault_id&vault_id=eq.$id")).isNotEmpty() }.getOrDefault(false)
            Triple(v, cm, aviso)
        }
    }
    val r = datos
    Box(Modifier.fillMaxSize().background(Asfalto)) {
        when {
            r == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }
            r.isFailure || r.getOrNull() == null -> Column(Modifier.padding(20.dp)) { Enlace("‹ Volver") { Nav.atras() }; Text("Ese concierto ya no está en la bóveda.", color = Tenue) }
            else -> {
                val (v, coms, avisar) = r.getOrThrow()!!
                val hoy = ahoraIso().take(10)
                val estado = if (v.str("starts_on") > hoy) "soon" else if (v.str("ends_on") <= hoy) "past" else "now"
                val prov = v.str("provider"); val vid = v.str("video_id"); val url = pagina(prov, vid)
                val nombreProv = mapOf("youtube" to "YouTube", "vimeo" to "Vimeo", "archive" to "Internet Archive")[prov] ?: ""
                val mini = Diseno.imagenOk(v.str("cover")).ifEmpty { if (prov == "youtube" && Regex("^[\\w-]{6,20}$").matches(vid)) "https://i.ytimg.com/vi/$vid/hqdefault.jpg" else if (prov == "archive") "https://archive.org/services/img/$vid" else "" }
                val img = imagen(mini)
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
                    Box(Modifier.padding(14.dp)) { Enlace("‹ Volver", TextoClaro) { Nav.atras() } }
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)).background(Color.Black).clickable(enabled = url.isNotEmpty() && estado == "now") { abrirEnlace(url) }, contentAlignment = Alignment.Center) {
                        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        if (estado == "now") Box(Modifier.size(66.dp).clip(CircleShape).background(Color(0xCCE3242B)), contentAlignment = Alignment.Center) { Icono("play", Color.White, 32.dp) }
                        else Text(if (estado == "soon") "Se estrena el ${Fechas.corta(v.str("starts_on"))}" else "Ya salió de la cartelera", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xB3000000)).padding(10.dp))
                    }
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(when (estado) { "now" -> "● En cartelera · hasta el ${Fechas.corta(v.str("ends_on"))}"; "soon" -> "● Próximamente · entra el ${Fechas.corta(v.str("starts_on"))}"; else -> "Salió de cartelera" }, color = if (estado == "now") Color(0xFF3ECF8E) else Tenue, fontWeight = FontWeight.SemiBold)
                        Text(v.str("title"), fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 32.sp)
                        Text(listOf(v.str("artist"), v.str("festival"), v.str("year").takeIf { it != "0" } ?: "", v.str("place")).filter { it.isNotBlank() }.joinToString(" · "), color = Tenue)
                        if (v.str("synopsis").isNotBlank()) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(14.dp)) { Text("SINOPSIS", color = Amarillo, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp); Text(v.str("synopsis"), lineHeight = 22.sp, modifier = Modifier.padding(top = 4.dp)) }
                        Text("Fuente: video" + (v.str("source_name").takeIf { it.isNotBlank() }?.let { " de $it" } ?: "") + " en $nombreProv" + if (v.bool("official")) " · canal oficial" else "", color = Tenue, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (url.isNotEmpty() && estado == "now") Boton("Ver el concierto") { abrirEnlace(url) }
                            Boton("Compartir", principal = false) { abrirEnlace(Supa.WEB + "#rvb=" + id) }
                            if (Cuenta.dentro && estado == "soon") Boton(if (avisar) "Te avisaremos" else "Avísame", principal = !avisar) {
                                scope.launch { try { if (avisar) Cuenta.api("DELETE", "/rest/v1/vault_watch?vault_id=eq.$id&user_id=eq.${Cuenta.uid}") else Cuenta.api("POST", "/rest/v1/vault_watch", mapOf("vault_id" to id, "user_id" to Cuenta.uid), "return=minimal"); Aviso.mostrar(if (avisar) "Aviso quitado." else "Te llegará un aviso en la revista cuando empiece."); version++ } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } }
                            }
                        }
                        if (v.str("note").isNotBlank()) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(14.dp)) {
                            Text("NOTA DEL CURADOR", color = Amarillo, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Text(v.str("note"), lineHeight = 22.sp, modifier = Modifier.padding(top = 4.dp)); Text("— ${v.map("profiles").str("name")}", color = Tenue, fontSize = 13.sp)
                        }
                        val set = v.list("setlist").mapNotNull { it.asObj() }.filter { it.str("s").isNotBlank() }
                        if (set.isNotEmpty()) {
                            Text("Lista de canciones", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            set.forEachIndexed { i, x ->
                                val t = (x["t"] as? Number)?.toInt()
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(enabled = t != null && prov == "youtube" && estado == "now") { abrirEnlace("$url&t=${t}s") }.padding(vertical = 6.dp, horizontal = 4.dp)) {
                                    Text("${i + 1}.", color = Tenue, modifier = Modifier.width(30.dp)); if (t != null) Text(reloj(t), color = Amarillo, modifier = Modifier.width(64.dp)); Text(x.str("s"))
                                }
                            }
                        }
                        Text("Comentarios (${coms.size})", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        if (coms.isEmpty()) Text("¿Lo viste? Cuenta qué te pareció.", color = Tenue)
                        coms.forEach { c ->
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { Nav.ir(Destino.Perfil(c.str("author_id"))) }) {
                                    Avatar(Autor(c.str("author_id"), c.map("profiles").str("name"), Diseno.imagenOk(c.map("profiles").str("avatar"))), 22); Spacer(Modifier.width(6.dp))
                                    Text("${c.map("profiles").str("name")} · ${Fechas.corta(c.str("created_at"))}", color = Amarillo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text(c.str("body"), modifier = Modifier.padding(top = 4.dp))
                                Row {
                                    if (Cuenta.uid == c.str("author_id") || Cuenta.esAdmin) Enlace("borrar", Tenue) { scope.launch { runCatching { Cuenta.api("DELETE", "/rest/v1/vault_comments?id=eq.${c.str("id")}") }; version++ } }
                                    if (Cuenta.dentro && Cuenta.uid != c.str("author_id")) Enlace("denunciar", Tenue) { denuncia = "vcomment" to c.str("id") }
                                }
                            }
                        }
                        Campo(texto, { texto = it }, if (Cuenta.dentro) "Escribe un comentario…" else "Entra para comentar", lineas = 2, max = 1000)
                        Boton("Comentar", habilitado = texto.isNotBlank()) {
                            if (Nav.necesitaCuenta()) scope.launch { try { Cuenta.api("POST", "/rest/v1/vault_comments", mapOf("vault_id" to id, "author_id" to Cuenta.uid, "body" to texto.trim().take(1000)), "return=minimal"); texto = ""; version++ } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } }
                        }
                        if (Cuenta.dentro) Enlace("Denunciar este concierto", Tenue) { denuncia = "vault" to id }
                    }
                }
            }
        }
        denuncia?.let { (t, i) -> DialogoDenuncia(t, i) { denuncia = null } }
    }
}

/* ---------------- Club de escucha ---------------- */
@Composable
fun ClubPantalla() {
    val scope = rememberCoroutineScope()
    var elegido by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableStateOf(0) }
    var abierta by remember { mutableStateOf(-2) }
    var texto by remember { mutableStateOf("") }
    var denuncia by remember { mutableStateOf<String?>(null) }
    val hoy = ahoraIso().take(10)
    fun estado(c: Map<String, Any?>) = if (c.str("starts_on") > hoy) "soon" else if (c.str("ends_on") <= hoy) "past" else "now"
    val datos by produceState<Result<Pair<List<Map<String, Any?>>, List<Map<String, Any?>>>>?>(null, elegido, version) {
        value = runCatching {
            val l = Json.arr(Cuenta.api("GET", "/rest/v1/clubs?select=*&order=starts_on.desc&limit=40")).mapNotNull { it.asObj() }
            val c = l.firstOrNull { it.str("id") == elegido } ?: l.firstOrNull { estado(it) == "now" }
            if (c != null && elegido == null) elegido = c.str("id")
            l to (if (c == null) emptyList() else Json.arr(Cuenta.api("GET", "/rest/v1/club_comments?select=*,profiles!author_id(name,avatar)&club_id=eq.${c.str("id")}&order=created_at.asc&limit=600")).mapNotNull { it.asObj() })
        }
    }
    Column(Modifier.fillMaxSize().background(Asfalto).verticalScroll(rememberScrollState())) {
        Encabezado("Club de escucha", "Un disco por semana, comentado canción por canción.")
        val r = datos
        when {
            r == null -> Cargando()
            r.isFailure -> Text(r.exceptionOrNull()?.message ?: "", color = Tenue, modifier = Modifier.padding(16.dp))
            else -> {
                val (l, coms) = r.getOrThrow(); val c = l.firstOrNull { it.str("id") == elegido }
                if (c == null) Text("No hay disco esta semana. Pronto habrá uno nuevo para escuchar juntos.", color = Tenue, modifier = Modifier.padding(16.dp))
                else {
                    val img = imagen(Diseno.imagenOk(c.str("cover")))
                    Row(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x22FFFFFF))) { if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                        Column(Modifier.padding(start = 14.dp)) {
                            Text((when (estado(c)) { "now" -> "Disco de la semana"; "soon" -> "Próximamente"; else -> "Ya pasó" }) + " · ${Fechas.corta(c.str("starts_on"))}", color = Amarillo, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(c.str("album"), fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                            Text(listOf(c.str("artist"), c.str("year").takeIf { it != "0" } ?: "").filter { it.isNotBlank() }.joinToString(" · "), color = Tenue)
                        }
                    }
                    if (c.str("note").isNotBlank()) Text(c.str("note"), lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    val pistas = c.list("tracks").map { it.toString() }
                    Text("Canción por canción", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
                    if (pistas.isEmpty()) Text("El editor todavía no agregó la lista de canciones.", color = Tenue, modifier = Modifier.padding(horizontal = 16.dp))
                    (pistas.indices.toList() + (-1)).forEach { i ->
                        val hilo = coms.filter { (it["track"] as? Number)?.toInt() == i }
                        Column(Modifier.fillMaxWidth().padding(16.dp, 4.dp).clip(RoundedCornerShape(14.dp)).background(Panel)) {
                            Row(Modifier.fillMaxWidth().clickable { abierta = if (abierta == i) -2 else i; texto = "" }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (i < 0) "★" else "${i + 1}", color = Amarillo, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                                Text(if (i < 0) "El disco completo" else pistas[i], fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Icono("globo", Tenue, 16.dp); Text(" ${hilo.size}", color = Tenue, fontSize = 13.sp)
                            }
                            if (abierta == i) Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (hilo.isEmpty()) Text(if (i < 0) "Cuenta qué te pareció el disco completo." else "Nadie ha comentado esta canción todavía.", color = Tenue, fontSize = 14.sp)
                                hilo.forEach { x ->
                                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x14FFFFFF)).padding(10.dp)) {
                                        Text("${x.map("profiles").str("name")} · ${haceTiempo(x.str("created_at"))}", color = Amarillo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(x.str("body"), fontSize = 15.sp)
                                        Row {
                                            if (Cuenta.uid == x.str("author_id") || Cuenta.esEditor) Enlace("borrar", Tenue) { scope.launch { runCatching { Cuenta.api("DELETE", "/rest/v1/club_comments?id=eq.${x.str("id")}") }; version++ } }
                                            if (Cuenta.dentro && Cuenta.uid != x.str("author_id")) Enlace("denunciar", Tenue) { denuncia = x.str("id") }
                                        }
                                    }
                                }
                                if (Cuenta.dentro) {
                                    Campo(texto, { texto = it }, if (i < 0) "Tu opinión del disco…" else "Qué te hizo sentir esta canción…", lineas = 2, max = 1000)
                                    Boton("Comentar", habilitado = texto.isNotBlank()) { scope.launch { try { Cuenta.api("POST", "/rest/v1/club_comments", mapOf("club_id" to c.str("id"), "track" to i, "author_id" to Cuenta.uid, "body" to texto.trim().take(1000)), "return=minimal"); texto = ""; version++ } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } } }
                                } else Enlace("Entra para comentar") { Nav.dialogo = "cuenta" }
                            }
                        }
                    }
                }
                val otros = l.filter { it.str("id") != elegido }
                if (otros.isNotEmpty()) {
                    Text("Otros discos del club", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp))
                    otros.forEach { o -> Row(Modifier.fillMaxWidth().padding(16.dp, 3.dp).clip(RoundedCornerShape(12.dp)).background(Panel).clickable { elegido = o.str("id"); abierta = -2 }.padding(12.dp)) {
                        Text(o.str("album"), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Text("${o.str("artist")} · ${when (estado(o)) { "soon" -> "próximamente"; "past" -> "ya pasó"; else -> "esta semana" }}", color = Tenue, fontSize = 13.sp)
                    } }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
    denuncia?.let { DialogoDenuncia("ccomment", it) { denuncia = null } }
}
