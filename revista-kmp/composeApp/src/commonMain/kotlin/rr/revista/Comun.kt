package rr.revista

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** A dónde se puede ir encima de las secciones. */
sealed class Destino {
    data class Leer(val id: String) : Destino()
    data class Perfil(val id: String) : Destino()
    /** Escribir (id = null) o editar un artículo. */
    data class Editar(val id: String?) : Destino()
    data class Poster(val id: String) : Destino()
    data class Coleccion(val id: String) : Destino()
    data class SubirPoster(val reto: String?) : Destino()
    data object BuscarOriginales : Destino()
    data class Artista(val id: String) : Destino()
    data class EditarArtista(val id: String?) : Destino()
}

/** Navegación con pila (el botón «Volver» quita la última pantalla). */
object Nav {
    var pila by mutableStateOf(listOf<Destino>()); private set
    /** Ventana abierta encima de todo: "cuenta", "normas"… */
    var dialogo by mutableStateOf<String?>(null)
    fun ir(d: Destino) { pila = pila + d }
    fun atras() { if (pila.isNotEmpty()) pila = pila.dropLast(1) }
    fun inicio() { pila = emptyList() }
    /** Si no entraste, abre la ventana de cuenta y devuelve false. */
    fun necesitaCuenta(): Boolean { if (Cuenta.dentro) return true; dialogo = "cuenta"; return false }
}

/** Mensajes cortos abajo de la pantalla. */
object Aviso {
    var texto by mutableStateOf(""); private set
    var error by mutableStateOf(false); private set
    var n by mutableStateOf(0); private set
    fun mostrar(t: String, esError: Boolean = false) { texto = t; error = esError; n++ }
}

@Composable
fun BoxScope.AvisoVista() {
    var visible by androidx.compose.runtime.remember { mutableStateOf(false) }
    LaunchedEffect(Aviso.n) { if (Aviso.n > 0) { visible = true; delay(if (Aviso.error) 6000 else 3500); visible = false } }
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp, start = 16.dp, end = 16.dp)) {
        Text(Aviso.texto, color = if (Aviso.error) Color.White else Color(0xFF1B1E23), fontWeight = FontWeight.SemiBold,
            modifier = Modifier.widthIn(max = 520.dp).clip(RoundedCornerShape(14.dp)).background(if (Aviso.error) Color(0xFFE5604D) else Color(0xFFF4F4F4)).padding(horizontal = 16.dp, vertical = 12.dp))
    }
}

/** Ventana encima de la pantalla (oscurece el fondo; tocar fuera la cierra). */
@Composable
fun Hoja(titulo: String, cerrar: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xB30A0C0F)).clickable(MutableInteractionSource(), null, onClick = cerrar), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(16.dp).widthIn(max = 520.dp).fillMaxWidth().heightIn(max = 760.dp).clip(RoundedCornerShape(22.dp)).background(Panel)
            .clickable(MutableInteractionSource(), null) {}.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.Bold, fontSize = 26.sp, modifier = Modifier.weight(1f))
                Box(Modifier.size(38.dp).clip(CircleShape).clickable(onClick = cerrar), contentAlignment = Alignment.Center) { Icono("cerrar", Tenue, 20.dp) }
            }
            contenido()
        }
    }
}

@Composable
fun Campo(valor: String, cambiar: (String) -> Unit, etiqueta: String, clave: Boolean = false, lineas: Int = 1, correo: Boolean = false, max: Int = 200, pista: String = "") {
    OutlinedTextField(valor, { cambiar(it.take(max)) }, label = { Text(etiqueta) }, singleLine = lineas == 1, minLines = lineas, placeholder = if (pista.isNotEmpty()) ({ Text(pista, color = Tenue) }) else null,
        visualTransformation = if (clave) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = when { clave -> KeyboardType.Password; correo -> KeyboardType.Email; else -> KeyboardType.Text }),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Amarillo, focusedLabelColor = Amarillo, cursorColor = Amarillo, unfocusedBorderColor = Color(0x33FFFFFF)),
        modifier = Modifier.fillMaxWidth())
}

@Composable
fun Boton(texto: String, principal: Boolean = true, ocupado: Boolean = false, habilitado: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val activo = habilitado && !ocupado
    Row(modifier.clip(RoundedCornerShape(12.dp)).background(if (principal) Amarillo.copy(alpha = if (activo) 1f else .5f) else Color(0x14FFFFFF))
        .then(if (principal) Modifier else Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)))
        .clickable(enabled = activo, onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (ocupado) { CircularProgressIndicator(Modifier.size(18.dp), color = if (principal) Color(0xFF1B1E23) else Amarillo, strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
        Text(texto, color = if (principal) Color(0xFF1B1E23) else TextoClaro, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Enlace(texto: String, color: Color = Amarillo, onClick: () -> Unit) =
    Text(texto, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 4.dp))

/** «hace 5 min», «hace 3 h», o la fecha. */
fun hace(iso: String): String {
    val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2})").find(iso) ?: return ""
    return Fechas.corta(iso).ifEmpty { m.value }
}
