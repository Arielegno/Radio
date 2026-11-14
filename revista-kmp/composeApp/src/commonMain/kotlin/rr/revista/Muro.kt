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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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

private data class DatosMuro(val notas: List<Map<String, Any?>>, val gustan: Set<String>, val votos: List<Map<String, Any?>>)

/** El muro de un perfil: notas, fotos, encuestas, dedicatorias para la radio, me gusta y respuestas. */
@Composable
fun Muro(perfil: Map<String, Any?>) {
    val scope = rememberCoroutineScope()
    val id = perfil.str("id"); val mio = Cuenta.uid == id
    var version by remember { mutableStateOf(0) }
    var texto by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<FotoElegida?>(null) }
    var encuesta by remember { mutableStateOf(false) }
    var opciones by remember { mutableStateOf(List(4) { "" }) }
    var dedicatoria by remember { mutableStateOf(false) }
    var publicando by remember { mutableStateOf(false) }
    var responder by remember { mutableStateOf<String?>(null) }
    var respuesta by remember { mutableStateOf("") }
    var denuncia by remember { mutableStateOf<String?>(null) }
    val datos by produceState<Result<DatosMuro>?>(null, id, version) {
        value = runCatching {
            val a = Json.arr(Cuenta.api("GET", "/rest/v1/wall_posts?select=*,profiles!author_id(name,avatar),wall_likes(count)&owner_id=eq.$id&order=created_at.desc&limit=80")).mapNotNull { it.asObj() }
            val ids = a.joinToString(",") { it.str("id") }; val enc = a.filter { it["poll"] is List<*> }.joinToString(",") { it.str("id") }
            DatosMuro(a,
                if (Cuenta.dentro && a.isNotEmpty()) runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/wall_likes?select=post_id&user_id=eq.${Cuenta.uid}&post_id=in.($ids)")).mapNotNull { it.asObj()?.str("post_id") }.toSet() }.getOrDefault(emptySet()) else emptySet(),
                if (enc.isNotEmpty()) runCatching { Json.arr(Cuenta.api("GET", "/rest/v1/wall_votes?select=post_id,option,user_id&post_id=in.($enc)")).mapNotNull { it.asObj() } }.getOrDefault(emptyList()) else emptyList())
        }
    }
    val puede = Cuenta.dentro && !Cuenta.suspendida && (mio || perfil["wall_open"] != false)
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (puede) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Campo(texto, { texto = it }, if (mio) "¿Qué estás escuchando hoy?" else "Escribe algo en el muro de ${perfil.str("name")}…", lineas = 2, max = 1000)
            if (encuesta) opciones.forEachIndexed { i, o -> Campo(o, { v -> opciones = opciones.toMutableList().also { it[i] = v } }, "Opción ${i + 1}" + if (i > 1) " (opcional)" else "", max = 60) }
            foto?.let { f -> EditorFoto(f) { foto = null } }
            if (dedicatoria) Text("Dedicatoria: Luna la leerá al aire en la radio de ${perfil.str("name")}.", color = Amarillo, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Enlace(if (foto == null) "Foto" else "Quitar foto") { if (foto == null) { val e = elegirImagenes(1600, 600, false); scope.launch { foto = e.esperar().firstOrNull()?.let { FotoElegida(it) } } } else foto = null }
                Enlace(if (encuesta) "Sin encuesta" else "Encuesta") { encuesta = !encuesta }
                if (!mio) Enlace(if (dedicatoria) "Sin dedicatoria" else "Dedicatoria") { dedicatoria = !dedicatoria }
                Spacer(Modifier.weight(1f))
                Boton("Publicar", ocupado = publicando, habilitado = texto.isNotBlank()) {
                    val ops = opciones.map { it.trim().take(60) }.filter { it.isNotEmpty() }
                    if (encuesta && ops.size < 2) { Aviso.mostrar("Escribe al menos 2 opciones para la encuesta.", true); return@Boton }
                    scope.launch {
                        publicando = true
                        try {
                            val b = linkedMapOf<String, Any?>("owner_id" to id, "author_id" to Cuenta.uid, "body" to texto.trim().take(1000))
                            if (encuesta) b["poll"] = ops; if (dedicatoria && !mio) b["dedication"] = true
                            foto?.let { b["image"] = subirFoto(it.lista(), "muro") }
                            Cuenta.api("POST", "/rest/v1/wall_posts", b, "return=minimal")
                            Aviso.mostrar(if (b["dedication"] == true) "Dedicatoria enviada: Luna la leerá en su radio." else "Publicado en el muro.")
                            texto = ""; foto = null; encuesta = false; opciones = List(4) { "" }; dedicatoria = false; version++
                        } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
                        publicando = false
                    }
                }
            }
        } else if (!Cuenta.dentro) Text("Entra a tu cuenta para escribir en los muros.", color = Tenue)
        else if (!mio && perfil["wall_open"] == false) Text("${perfil.str("name")} tiene su muro solo para sus propias notas.", color = Tenue)

        val r = datos
        when {
            r == null -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amarillo) }
            r.isFailure -> Text(r.exceptionOrNull()?.message ?: "", color = Tenue)
            else -> {
                val d = r.getOrThrow()
                val raiz = d.notas.filter { it.str("parent_id").isEmpty() }
                if (raiz.isEmpty()) Text(if (mio) "Tu muro está vacío. Cuenta qué estás escuchando, comparte una foto de tu colección o un recuerdo de concierto." else "${perfil.str("name")} todavía no tiene notas en su muro.", color = Tenue)
                raiz.forEach { w ->
                    val hijos = d.notas.filter { it.str("parent_id") == w.str("id") }.sortedBy { it.str("created_at") }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (w.bool("dedication")) Color(0x1FF2C230) else Panel).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (w.bool("dedication")) Text("Dedicatoria para la radio de ${perfil.str("name")}", color = Amarillo, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Nota(w, d, id, scope, { version++ }, { denuncia = it })
                        val im = Diseno.imagenOk(w.str("image"))
                        if (im.isNotEmpty()) FotoPublicada(im)
                        (w["poll"] as? List<*>)?.let { ops ->
                            val vs = d.votos.filter { it.str("post_id") == w.str("id") }; val tot = vs.size; val mioV = vs.firstOrNull { it.str("user_id") == Cuenta.uid }?.num("option")?.toInt()
                            ops.forEachIndexed { i, o ->
                                val n = vs.count { it.num("option").toInt() == i }; val pc = if (tot > 0) n * 100 / tot else 0
                                Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x14FFFFFF)).border(if (mioV == i) 2.dp else 0.dp, Amarillo, RoundedCornerShape(10.dp)).clickable {
                                    if (Nav.necesitaCuenta()) scope.launch { runCatching { Cuenta.api("DELETE", "/rest/v1/wall_votes?post_id=eq.${w.str("id")}&user_id=eq.${Cuenta.uid}"); Cuenta.api("POST", "/rest/v1/wall_votes", mapOf("post_id" to w.str("id"), "user_id" to Cuenta.uid, "option" to i), "return=minimal") }.onFailure { Aviso.mostrar(it.message ?: "", true) }; version++ }
                                }) {
                                    if (mioV != null) Box(Modifier.fillMaxHeight().fillMaxWidth(pc / 100f).background(Amarillo.copy(alpha = .3f)))
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp).align(Alignment.CenterStart)) { Text(o.toString(), modifier = Modifier.weight(1f)); if (mioV != null) Text("$pc%", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Gusta(w, d, scope) { version++ }
                            if (puede) Enlace(if (hijos.isEmpty()) "Responder" else "Responder (${hijos.size})") { responder = if (responder == w.str("id")) null else w.str("id"); respuesta = "" }
                        }
                        hijos.forEach { h -> Column(Modifier.fillMaxWidth().padding(start = 20.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x14FFFFFF)).padding(10.dp)) { Nota(h, d, id, scope, { version++ }, { denuncia = it }); Gusta(h, d, scope) { version++ } } }
                        if (responder == w.str("id")) {
                            Campo(respuesta, { respuesta = it }, "Escribe una respuesta…", lineas = 2, max = 1000)
                            Boton("Responder", habilitado = respuesta.isNotBlank()) { scope.launch { try { Cuenta.api("POST", "/rest/v1/wall_posts", mapOf("owner_id" to id, "author_id" to Cuenta.uid, "body" to respuesta.trim().take(1000), "parent_id" to w.str("id")), "return=minimal"); responder = null; respuesta = ""; version++ } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) } } }
                        }
                    }
                }
            }
        }
    }
    denuncia?.let { DialogoDenuncia("wall", it) { denuncia = null } }
}

@Composable
private fun Nota(w: Map<String, Any?>, d: DatosMuro, dueno: String, scope: kotlinx.coroutines.CoroutineScope, recargar: () -> Unit, denunciar: (String) -> Unit) {
    val p = w.map("profiles")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clickable { Nav.ir(Destino.Perfil(w.str("author_id"))) }, verticalAlignment = Alignment.CenterVertically) {
            Avatar(Autor(w.str("author_id"), p.str("name"), Diseno.imagenOk(p.str("avatar"))), 30); Spacer(Modifier.width(8.dp))
            Column { Text(p.str("name").ifBlank { "Alguien" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis); Text(haceTiempo(w.str("created_at")), color = Tenue, fontSize = 12.sp) }
        }
        if (Cuenta.uid == w.str("author_id") || Cuenta.uid == dueno || Cuenta.esAdmin) Enlace("borrar", Tenue) { scope.launch { runCatching { Cuenta.api("DELETE", "/rest/v1/wall_posts?id=eq.${w.str("id")}") }; recargar() } }
        if (Cuenta.dentro && Cuenta.uid != w.str("author_id")) Enlace("denunciar", Tenue) { denunciar(w.str("id")) }
    }
    Text(Hashtags.texto(w.str("body"), Amarillo), fontSize = 16.sp, lineHeight = 22.sp)
}

@Composable
private fun Gusta(w: Map<String, Any?>, d: DatosMuro, scope: kotlinx.coroutines.CoroutineScope, recargar: () -> Unit) {
    val on = w.str("id") in d.gustan
    val n = ((w["wall_likes"] as? List<*>)?.firstOrNull() as? Map<*, *>)?.get("count")?.let { (it as? Number)?.toInt() } ?: 0
    Row(Modifier.clip(RoundedCornerShape(999.dp)).clickable {
        if (Nav.necesitaCuenta()) scope.launch {
            runCatching { if (on) Cuenta.api("DELETE", "/rest/v1/wall_likes?post_id=eq.${w.str("id")}&user_id=eq.${Cuenta.uid}") else Cuenta.api("POST", "/rest/v1/wall_likes", mapOf("post_id" to w.str("id"), "user_id" to Cuenta.uid), "return=minimal") }.onFailure { Aviso.mostrar(it.message ?: "", true) }
            recargar()
        }
    }.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icono("corazon", if (on) Color(0xFFFF5F9E) else Tenue, 16.dp); Text(" $n", color = Tenue, fontSize = 13.sp) }
}
