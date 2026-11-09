package rr.revista

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

/** Botón de arriba a la derecha: tu foto (abre tu perfil) o «Entrar». */
@Composable
fun BotonCuenta() {
    val s = Cuenta.sesion
    if (s == null) {
        Text("Entrar", color = Color(0xFF1B1E23), fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Amarillo).clickable { Nav.dialogo = "cuenta" }.padding(horizontal = 16.dp, vertical = 8.dp))
    } else {
        val av = imagen(Diseno.imagenOk(Cuenta.yo.str("avatar")))
        Box(Modifier.size(40.dp).clip(CircleShape).background(Amarillo).clickable { Nav.ir(Destino.Perfil(s.uid)) }, contentAlignment = Alignment.Center) {
            if (av != null) Image(av, "Tu perfil", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Text((Cuenta.yo.str("name").ifBlank { s.nombre }).take(1).uppercase().ifEmpty { "?" }, color = Color(0xFF1B1E23), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

/** Entrar, crear cuenta o recuperar la contraseña (igual que la web). */
@Composable
fun DialogoCuenta(cerrar: () -> Unit) {
    var modo by remember { mutableStateOf("entrar") }
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var invitacion by remember { mutableStateOf("") }
    var acepto by remember { mutableStateOf(false) }
    var ocupado by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var sinConfirmar by remember { mutableStateOf(false) }
    var conInvitacion by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(modo) { error = ""; sinConfirmar = false; if (modo == "crear") conInvitacion = Cuenta.soloConInvitacion() }
    val correoOk = Regex("^\\S+@\\S+\\.\\S+$").matches(correo.trim())

    Hoja(when (modo) { "crear" -> "Crear cuenta"; "recuperar" -> "Recuperar contraseña"; else -> "Entrar" }, cerrar) {
        when (modo) {
            "recuperar" -> {
                Text("Te enviaremos un enlace a tu correo. Ábrelo en este mismo teléfono.", color = Tenue)
                Campo(correo, { correo = it }, "Correo de tu cuenta", correo = true)
                Boton("Enviar enlace", ocupado = ocupado, habilitado = correoOk, modifier = Modifier.fillMaxWidth()) {
                    scope.launch {
                        ocupado = true
                        val e = runCatching { Cuenta.recuperar(correo.trim(), Supa.WEB) }.getOrDefault(0)
                        ocupado = false
                        if (e == 429) error = "Ya pediste varios enlaces. Espera unos minutos."
                        else { Aviso.mostrar("Si ese correo tiene cuenta, te llegó un enlace. Revisa también spam."); cerrar() }
                    }
                }
                Enlace("Volver a entrar") { modo = "entrar" }
            }
            else -> {
                Text("Con una cuenta puedes escribir, comentar, dar me gusta, seguir autores y subir pósters.", color = Tenue)
                if (modo == "crear") Campo(nombre, { nombre = it }, "Tu nombre público", max = 40, pista = "Así te verán en tus artículos")
                Campo(correo, { correo = it }, "Correo", correo = true)
                Campo(clave, { clave = it }, "Contraseña (mínimo 6)", clave = true, max = 100)
                if (modo == "crear") {
                    if (conInvitacion) Campo(invitacion, { invitacion = it }, "Código de invitación", max = 60, pista = "Pídeselo al administrador")
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x14FFFFFF)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Normas de la comunidad", fontWeight = FontWeight.Bold)
                        Cuenta.NORMAS.forEachIndexed { i, (t, d) -> Text("${i + 1}. $t: $d", color = Tenue, fontSize = 14.sp, lineHeight = 19.sp) }
                        Text("Al crear tu cuenta aceptas también el aviso legal y de privacidad. Debes tener 13 años o más.", color = Tenue, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { acepto = !acepto }) {
                            Checkbox(acepto, { acepto = it }, colors = CheckboxDefaults.colors(checkedColor = Amarillo, checkmarkColor = Color(0xFF1B1E23)))
                            Text("Acepto las normas", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (error.isNotEmpty()) Text(error, color = Color(0xFFFF8A7A))
                if (sinConfirmar) Boton("Reenviar correo de confirmación", principal = false, modifier = Modifier.fillMaxWidth()) {
                    scope.launch {
                        val e = runCatching { Cuenta.reenviar(correo.trim(), Supa.WEB) }.getOrDefault(0)
                        Aviso.mostrar(if (e in 200..299) "Listo: revisa tu correo (y spam) y toca el enlace." else if (e == 429) "Ya pediste varios correos. Espera unos minutos." else "No se pudo reenviar. Inténtalo en unos minutos.", e !in 200..299)
                    }
                }
                val listo = correoOk && clave.length >= 6 && (modo == "entrar" || (nombre.trim().length >= 2 && acepto && (!conInvitacion || invitacion.isNotBlank())))
                Boton(if (modo == "crear") "Crear cuenta" else "Entrar", ocupado = ocupado, habilitado = listo, modifier = Modifier.fillMaxWidth()) {
                    scope.launch {
                        ocupado = true; error = ""; sinConfirmar = false
                        try {
                            if (modo == "crear") {
                                val dentro = Cuenta.crear(correo.trim(), clave, nombre.trim().take(40), invitacion.trim(), Supa.WEB)
                                Aviso.mostrar(if (dentro) "¡Listo! Ya estás dentro." else "Cuenta creada. Te llegó un correo: toca el enlace para confirmarla y luego entra.")
                            } else { Cuenta.entrar(correo.trim(), clave); Aviso.mostrar("¡Listo! Ya estás dentro.") }
                            cerrar()
                        } catch (e: Throwable) {
                            error = e.message ?: "Algo salió mal."
                            sinConfirmar = error.startsWith("Confirma tu correo")
                        }
                        ocupado = false
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (modo == "entrar") { Enlace("Crear cuenta") { modo = "crear" }; Enlace("Olvidé mi contraseña") { modo = "recuperar" } }
                    else Enlace("Ya tengo cuenta") { modo = "entrar" }
                }
            }
        }
    }
}

/** Denunciar algo (solo lo ve el administrador). */
@Composable
fun DialogoDenuncia(tipo: String, id: String, cerrar: () -> Unit) {
    var cat by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Hoja("Denunciar", cerrar) {
        Text("¿Qué pasa con esto? Solo lo verá el administrador.", color = Tenue)
        Cuenta.CATEGORIAS.forEach { (k, n) ->
            Text(n, fontWeight = if (cat == k) FontWeight.Bold else FontWeight.Normal, color = if (cat == k) Color(0xFF1B1E23) else TextoClaro,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (cat == k) Amarillo else Color(0x14FFFFFF)).clickable { cat = k }.padding(14.dp))
        }
        Campo(motivo, { motivo = it }, "Cuéntanos más (opcional)", lineas = 3, max = 300)
        Boton("Enviar denuncia", ocupado = ocupado, habilitado = cat.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            scope.launch {
                ocupado = true
                try { Cuenta.denunciar(tipo, id, cat, motivo); Aviso.mostrar("Gracias. El administrador revisará la denuncia."); cerrar() }
                catch (e: Throwable) { Aviso.mostrar(e.message ?: "No se pudo enviar.", true) }
                ocupado = false
            }
        }
    }
}

/** Ventana con un campo de texto (por ejemplo, la nota al recomendar). */
@Composable
fun DialogoTexto(titulo: String, explicacion: String, etiqueta: String, boton: String, max: Int = 280, cerrar: () -> Unit, listo: (String) -> Unit) {
    var t by remember { mutableStateOf("") }
    Hoja(titulo, cerrar) {
        if (explicacion.isNotEmpty()) Text(explicacion, color = Tenue)
        Campo(t, { t = it }, etiqueta, lineas = 2, max = max)
        Boton(boton, modifier = Modifier.fillMaxWidth()) { listo(t) }
    }
}

/** Confirmar algo que no se puede deshacer. */
@Composable
fun DialogoConfirmar(titulo: String, texto: String, boton: String, cerrar: () -> Unit, si: () -> Unit) {
    Hoja(titulo, cerrar) {
        Text(texto, color = Tenue)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Boton("Cancelar", principal = false, modifier = Modifier.weight(1f), onClick = cerrar)
            Boton(boton, modifier = Modifier.weight(1f)) { si() }
        }
    }
}
