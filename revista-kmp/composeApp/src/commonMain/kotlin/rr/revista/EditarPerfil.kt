package rr.revista

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

/** Editar tu perfil: nombre, bio, foto, imagen de fondo, muro, «escuchando ahora» y estética (como en la web). */
@Composable
fun EditarPerfilPantalla() {
    val scope = rememberCoroutineScope()
    val yo = Cuenta.yo
    var nombre by remember { mutableStateOf(yo.str("name")) }
    var bio by remember { mutableStateOf(yo.str("bio")) }
    var avatar by remember { mutableStateOf(Diseno.imagenOk(yo.str("avatar"))) }
    var nuevoAvatar by remember { mutableStateOf<FotoElegida?>(null) }
    var fondo by remember { mutableStateOf(Diseno.imagenOk(yo.str("banner"))) }
    var nuevoFondo by remember { mutableStateOf<FotoElegida?>(null) }
    var muroAbierto by remember { mutableStateOf(yo["wall_open"] != false) }
    var compartir by remember { mutableStateOf(leerDato(EscuchandoAhora.CLAVE) == "1") }
    var estilo by remember { mutableStateOf(Estilo.de(yo.map("style"))) }
    var guardando by remember { mutableStateOf(false) }
    if (!Cuenta.dentro) { Column(Modifier.fillMaxSize().background(Asfalto).padding(20.dp)) { Enlace("‹ Volver", TextoClaro) { Nav.atras() }; Text("Entra a tu cuenta.", color = Tenue) }; return }
    Column(Modifier.fillMaxSize().background(Asfalto).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Enlace("‹ Cancelar", TextoClaro) { Nav.atras() }
        Text("Editar perfil", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)

        // Vista previa con la estética elegida
        val tema = Temas.de(estilo)
        val banImg = imagen(nuevoFondo?.foto?.mini ?: fondo)
        val avImg = imagen(nuevoAvatar?.foto?.mini ?: avatar)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel)) {
            Box(Modifier.fillMaxWidth().aspectRatio(2.6f).drawBehind { fondoTema(estilo, tema) }) { if (banImg != null) Image(banImg, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Amarillo), contentAlignment = Alignment.Center) {
                    if (avImg != null) Image(avImg, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Text(nombre.take(1).uppercase(), color = Color(0xFF1B1E23), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column { Text(nombre.ifBlank { "Tu nombre" }, fontFamily = Fuentes.titulo(estilo.ft), fontWeight = FontWeight.Bold, fontSize = 22.sp); if (bio.isNotBlank()) Text(bio, color = Tenue, fontSize = 13.sp, maxLines = 2) }
            }
        }

        Campo(nombre, { nombre = it }, "Nombre público", max = 40)
        Campo(bio, { bio = it }, "Biografía (opcional)", lineas = 3, max = 300, pista = "Qué música te mueve, de dónde eres…")

        Text("Foto", color = Tenue)
        nuevoAvatar?.let { f -> EditorFoto(f) { nuevoAvatar = null } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Boton("Elegir foto", principal = false) { val e = elegirImagenes(800, 200, false); scope.launch { e.esperar().firstOrNull()?.let { nuevoAvatar = FotoElegida(it).apply { formato = "1x1" } } } }
            if (avatar.isNotEmpty() || nuevoAvatar != null) Boton("Quitar", principal = false) { avatar = ""; nuevoAvatar = null }
        }

        Text("Imagen de fondo", color = Tenue)
        nuevoFondo?.let { f -> EditorFoto(f) { nuevoFondo = null } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Boton("Elegir imagen de fondo", principal = false) { val e = elegirImagenes(1500, 600, false); scope.launch { e.esperar().firstOrNull()?.let { nuevoFondo = FotoElegida(it).apply { formato = "16x9" } } } }
            if (fondo.isNotEmpty() || nuevoFondo != null) Boton("Quitar", principal = false) { fondo = ""; nuevoFondo = null }
        }

        Casilla(muroAbierto, "Otras personas pueden escribir en mi muro") { muroAbierto = it }
        Casilla(compartir, "Mostrar en mi perfil lo que estoy escuchando en la radio de la app") { compartir = it }

        Text("Estética de tu perfil", color = Tenue)
        Fichas(Estilo.TEMAS.map { it to (Estilo.NOMBRES_TEMA[it] ?: it) }, estilo.th) { estilo = estilo.conTema(it) }
        Text("Color de acento", color = Tenue)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Estilo.ACENTOS.forEach { (k, c) ->
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color(c)).clickable { estilo = estilo.copy(ac = k) }, contentAlignment = Alignment.Center) {
                    if (estilo.ac == k) Box(Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                }
            }
        }
        Text("Letra de tu nombre", color = Tenue)
        Fichas(Estilo.FUENTES.map { it to (Estilo.NOMBRES_FUENTE[it] ?: it) }, estilo.ft) { estilo = estilo.copy(ft = it) }

        Boton("Guardar perfil", ocupado = guardando, modifier = Modifier.fillMaxWidth()) {
            if (nombre.trim().length < 2) { Aviso.mostrar("Tu nombre debe tener al menos 2 letras.", true); return@Boton }
            scope.launch {
                guardando = true
                try {
                    val av = nuevoAvatar?.let { Aviso.mostrar("Subiendo foto…"); subirFoto(it.lista(), "av") } ?: avatar.ifEmpty { null }
                    val ban = nuevoFondo?.let { Aviso.mostrar("Subiendo imagen de fondo…"); subirFoto(it.lista(), "fondo") } ?: fondo.ifEmpty { null }
                    val b = linkedMapOf<String, Any?>("name" to nombre.trim().take(40), "bio" to bio.trim().take(300).ifEmpty { null }, "avatar" to av, "style" to estilo.aMapa(), "banner" to ban, "wall_open" to muroAbierto)
                    if (!compartir && yo["now_playing"] != null) b["now_playing"] = null
                    Cuenta.api("PATCH", "/rest/v1/profiles?id=eq.${Cuenta.uid}", b, "return=minimal")
                    guardarDato(EscuchandoAhora.CLAVE, if (compartir) "1" else null)
                    Cuenta.social(true)
                    Aviso.mostrar("Perfil guardado."); Nav.atras()
                } catch (e: Throwable) { Aviso.mostrar(e.message ?: "", true) }
                guardando = false
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Casilla(v: Boolean, texto: String, cambiar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { cambiar(!v) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(v, cambiar, colors = CheckboxDefaults.colors(checkedColor = Amarillo, checkmarkColor = Color(0xFF1B1E23)))
        Text(texto, modifier = Modifier.weight(1f))
    }
}

/** «Escuchando ahora»: la app manda la canción que suena a tu perfil (si lo activaste en Editar perfil). */
object EscuchandoAhora {
    const val CLAVE = "rr_now_share"
    private var ultima = ""
    private var cuando = 0.0

    suspend fun publicar(cancion: String, artista: String) {
        if (leerDato(CLAVE) != "1" || !Cuenta.dentro || cancion.isBlank()) return
        val k = "$cancion|$artista"
        if (k == ultima && ahoraMs() - cuando < 10 * 60_000) return
        ultima = k; cuando = ahoraMs()
        runCatching { Cuenta.api("PATCH", "/rest/v1/profiles?id=eq.${Cuenta.uid}", mapOf("now_playing" to mapOf("n" to cancion.take(120), "a" to artista.take(120), "at" to ahoraMs().toLong())), "return=minimal") }
    }
}

/** Tarjeta «Escuchando ahora» en un perfil (dura 45 minutos, como en la web). */
@Composable
fun TarjetaEscuchando(p: Map<String, Any?>) {
    val n = p.map("now_playing")
    val at = n.num("at"); if (n.str("n").isBlank() || at <= 0) return
    val min = ((ahoraMs().toLong() - at) / 60_000).coerceAtLeast(0)
    if (min > 45) return
    Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Amarillo.copy(alpha = .12f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icono("nota", Amarillo, 26.dp); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("ESCUCHANDO AHORA · " + if (min < 2) "AHORA MISMO" else "HACE $min MIN", color = Amarillo, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(n.str("n"), fontWeight = FontWeight.Bold, maxLines = 1)
            if (n.str("a").isNotBlank()) Text(n.str("a"), color = Tenue, fontSize = 13.sp, maxLines = 1)
        }
    }
}
