package io.github.arielegno.radio.ui

import android.content.Intent
import android.net.Uri
import android.speech.tts.Voice
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.arielegno.radio.Datos
import io.github.arielegno.radio.RadioApp
import io.github.arielegno.radio.Spotify
import io.github.arielegno.radio.core.Luna
import io.github.arielegno.radio.core.Modos
import io.github.arielegno.radio.launchSafe

@Composable
fun AjustesPantalla(tema: String, cambiarTema: (String) -> Unit, verRecorrido: () -> Unit, pedirUbicacion: () -> Unit) {
    val p = RadioApp.prefs; val r = RadioApp.radio
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var borrarStats by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineLarge)

        Seccion("De dónde sale la música")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false to "Mi biblioteca", true to "Spotify").forEach { (sp, n) ->
                if (r.fuenteSpotify == sp) androidx.compose.material3.Button(onClick = {}) { Text(n) } else OutlinedButton(onClick = { r.ponerFuente(sp) }) { Text(n) }
            }
        }
        if (r.fuenteSpotify) {
            Text("Spotify necesita Premium. La música suena en la app oficial de Spotify y Radio de ruta la controla: Luna habla entre canciones igual que con tu biblioteca.",
                color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
            CampoTexto("spClientId", "Client ID de Spotify", "")
            Text("En developer.spotify.com › tu app › Settings › Redirect URIs agrega: radioderuta://spotify",
                color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                if (Spotify.conectado) {
                    Text("✓ Conectado", color = Amarillo, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { Spotify.desconectar() }) { Text("Desconectar") }
                } else androidx.compose.material3.Button(onClick = { Spotify.entrar(ctx) }) { Text("Conectar con Spotify") }
                TextButton(onClick = { abrir(ctx, "https://developer.spotify.com/dashboard") }) { Text("Abrir panel") }
            }
            if (Spotify.aviso.isNotEmpty()) Text(Spotify.aviso, color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall)
        }

        Seccion("Luna, tu locutora")
        CampoTexto("host", "Nombre de la locutora", "Luna")
        Opciones("genero", "Es", linkedMapOf("locutora" to "Locutora", "locutor" to "Locutor"), "locutora")
        Opciones("acento", "Acento", Luna.ACENTOS.mapValues { (k, v) -> if (k == "neutro") "Neutro" else "Español $v" }, "neutro")
        Opciones("largo", "Cuánto habla", linkedMapOf("corta" to "Poco", "media" to "Normal", "larga" to "Bastante"), "media")
        Opciones("cadaN", "Habla cada…", linkedMapOf("1" to "Cada canción", "2" to "Cada 2 canciones", "3" to "Cada 3 canciones", "5" to "Cada 5 canciones"), "1", entero = true)
        CampoTexto("me", "Tu nombre (para saludarte)", "")
        Interruptor("memoria", "Que Luna recuerde lo que más escuchas", true)

        Seccion("Inteligencia artificial (gratis con Gemini)")
        Text("Pega tu clave gratuita de Google AI Studio (aistudio.google.com › Get API key). Se guarda solo en este celular. Sin clave, Luna usa frases sencillas.",
            color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall)
        CampoTexto("geminiKey", "Clave de Gemini", "", secreto = true)
        Opciones("geminiModelSel", "Modelo", linkedMapOf("gemini-3.5-flash-lite" to "Gemini Lite (más peticiones)", "gemini-3.5-flash" to "Gemini Flash (equilibrado)", "gemini-flash-latest" to "Gemini Flash más reciente"), "gemini-3.5-flash-lite")
        CampoTexto("geminiModel", "Modelo personalizado (opcional)", "")
        OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))) }) { Text("Conseguir clave gratis") }

        Seccion("Cómo habla")
        val m = r.modo
        CampoTexto("modo_$m", "Estilo para el modo ${Modos.ETIQUETA[m]?.lowercase()}", "", lineas = 3, ayuda = Modos.DESCRIPCION[m] ?: "")
        CampoTexto("frases_$m", "Frases del modo ${Modos.ETIQUETA[m]?.lowercase()} (una por línea)", "", lineas = 3, ayuda = (Modos.FRASES[m] ?: emptyList()).joinToString(" / "))
        CampoTexto("persona", "Estilo que le pides a Luna (opcional)", "", lineas = 2)
        CampoTexto("evitar", "Temas que debe evitar (opcional)", "", lineas = 2)

        Seccion("Programas de Luna")
        Interruptor("progRec", "«La hora del recuerdo» una vez al día", false)
        Opciones("progRecH", "A las", (0..23).associate { h -> h.toString() to (if (h == 0) "12 a. m." else if (h < 12) "$h a. m." else if (h == 12) "12 p. m." else "${h - 12} p. m.") }, "20", entero = true)
        Interruptor("progSem", "«El resumen del domingo»: lo que más sonó en el mes", false)
        CampoTexto("dedic", "Dedicatoria (se lee una sola vez)", "", lineas = 2)
        CampoTexto("efemerides", "Efemérides propias (una por línea: MM-DD: texto)", "", lineas = 3, ayuda = "Ej: 03-14: Hoy cumple años mi mamá")

        Seccion("Voz")
        var versionVoz by remember { mutableStateOf(0) }
        val motores by produceState<List<Pair<String, String>>>(emptyList(), versionVoz) { value = r.voz.motores() }
        val voces by produceState<List<Voice>>(emptyList(), versionVoz) { value = r.voz.vocesEspanol() }
        if (motores.size > 1) Opciones("motorVoz", "Motor de voz", linkedMapOf("" to "El del teléfono") + motores.toMap(), "") { m -> r.voz.cambiarMotor(m); versionVoz++ }
        if (voces.isNotEmpty()) androidx.compose.runtime.key(versionVoz) {
            Opciones("voz", "Voz", linkedMapOf("" to "La de siempre") + voces.associate { it.name to (it.locale.displayName + " · " + it.name.substringAfterLast('#').takeLast(18)) }, "")
        }
        Text("Voces gratis que funcionan sin internet: instala «SherpaTTS» (voces Piper y Kokoro, código abierto) o «RHVoice» desde F-Droid o la Play Store, descarga una voz en español y elígela aquí arriba como motor de voz.",
            color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { abrir(ctx, "https://search.f-droid.org/?q=SherpaTTS&lang=es") }) { Text("SherpaTTS") }
            OutlinedButton(onClick = { abrir(ctx, "https://rhvoice.org/") }) { Text("RHVoice") }
        }
        Deslizador("velocidad", "Velocidad", 100, 60, 150)
        Deslizador("tono", "Tono", 100, 60, 150)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { scope.launchSafe { r.probarVoz() } }) { Text("Probar voz") }
            OutlinedButton(onClick = { scope.launchSafe { r.probarLuna() } }) { Text("Probar intervención") }
        }
        TextButton(onClick = { try { ctx.startActivity(Intent("com.android.settings.TTS_SETTINGS")) } catch (e: Exception) {} }) { Text("Instalar o cambiar voces del teléfono") }

        Seccion("Sonido y ubicación")
        Interruptor("fundido", "Entrada suave de cada canción", false)
        Interruptor("ubicacion", "Usar mi ubicación (clima, zona y mapa de viajes)", true) { on -> if (on) pedirUbicacion() else r.entorno.detener() }
        Opciones("unidades", "Temperatura", linkedMapOf("c" to "Grados Celsius", "f" to "Grados Fahrenheit"), "c")

        Seccion("Apariencia")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "Automático", "dark" to "Oscuro", "light" to "Claro").forEach { (k, n) ->
                if (tema == k) androidx.compose.material3.Button(onClick = {}) { Text(n) } else OutlinedButton(onClick = { cambiarTema(k) }) { Text(n) }
            }
        }

        Seccion("Tus datos")
        OutlinedButton(onClick = verRecorrido) { Text("Ver el recorrido de bienvenida") }
        OutlinedButton(onClick = { borrarStats = true }, modifier = Modifier.padding(top = 6.dp)) { Text("Borrar estadísticas e historial") }
        Text("Tu música, tus claves y tus estadísticas se guardan solo en este celular.", color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))

        Seccion("Acerca de")
        Text("Radio de ruta para Android · versión 1.1 · por Arielegno", fontWeight = FontWeight.SemiBold)
        Text("No manipules la app mientras conduces: configúrala antes de arrancar.", color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://arielegno.github.io/Radio/legal.html"))) }) { Text("Aviso legal y privacidad") }
        Text("Usa Media3 (Apache 2.0), Jetpack Compose (Apache 2.0), Leaflet (BSD-2) y datos de OpenStreetMap (ODbL), Open-Meteo, Wikipedia (CC BY-SA) y Google Gemini.",
            color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(32.dp))
    }
    if (borrarStats) AlertDialog(onDismissRequest = { borrarStats = false }, title = { Text("¿Borrar estadísticas?") },
        text = { Text("Se borran las más escuchadas, tu resumen y el mapa de viajes. Tus favoritas y tu música se quedan.") },
        confirmButton = { TextButton(onClick = { RadioApp.stats.clear(); Datos.guardarStats(); borrarStats = false }) { Text("Borrar") } },
        dismissButton = { TextButton(onClick = { borrarStats = false }) { Text("Cancelar") } })
}

private fun abrir(ctx: android.content.Context, url: String) {
    try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) {}
}

@Composable
private fun Seccion(t: String) {
    Spacer(Modifier.height(22.dp))
    Text(t, style = MaterialTheme.typography.titleMedium, color = Amarillo)
    HorizontalDivider(Modifier.padding(top = 6.dp, bottom = 8.dp), color = LocalExtra.current.borde)
}

@Composable
private fun CampoTexto(k: String, etiqueta: String, d: String, secreto: Boolean = false, lineas: Int = 1, ayuda: String = "") {
    val p = RadioApp.prefs
    var v by remember(k) { mutableStateOf(p.str(k, d)) }
    var ver by remember { mutableStateOf(false) }
    OutlinedTextField(value = v, onValueChange = { v = it; p.set(k, it) }, label = { Text(etiqueta) },
        singleLine = lineas == 1, minLines = lineas, placeholder = if (ayuda.isNotEmpty()) ({ Text(ayuda, maxLines = 3) }) else null,
        visualTransformation = if (secreto && !ver) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (secreto) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        trailingIcon = if (secreto) ({ TextButton(onClick = { ver = !ver }) { Text(if (ver) "Ocultar" else "Ver") } }) else null,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
}

@Composable
private fun Interruptor(k: String, t: String, d: Boolean, alCambiar: (Boolean) -> Unit = {}) {
    val p = RadioApp.prefs
    var v by remember(k) { mutableStateOf(p.bool(k, d)) }
    Row(Modifier.fillMaxWidth().clickable { v = !v; p.set(k, v); alCambiar(v) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(t, Modifier.weight(1f)); Switch(checked = v, onCheckedChange = { v = it; p.set(k, it); alCambiar(it) })
    }
}

@Composable
private fun Opciones(k: String, t: String, ops: Map<String, String>, d: String, entero: Boolean = false, alCambiar: (String) -> Unit = {}) {
    val p = RadioApp.prefs
    var v by remember(k) { mutableStateOf(if (entero) p.int(k, d.toInt()).toString() else p.str(k, d)) }
    var abierto by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().clickable { abierto = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t, Modifier.weight(1f)); Text(ops[v] ?: v.ifEmpty { "—" }, color = Amarillo, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            ops.forEach { (k2, n) ->
                DropdownMenuItem(text = { Text(n) }, onClick = {
                    v = k2; abierto = false
                    if (entero) p.set(k, k2.toInt()) else p.set(k, k2)
                    alCambiar(k2)
                })
            }
        }
    }
}

@Composable
private fun Deslizador(k: String, t: String, d: Int, min: Int, max: Int) {
    val p = RadioApp.prefs
    var v by remember(k) { mutableFloatStateOf(p.int(k, d).toFloat()) }
    Column(Modifier.padding(vertical = 4.dp)) {
        Text("$t: ${v.toInt()} %")
        Slider(value = v, onValueChange = { v = it }, valueRange = min.toFloat()..max.toFloat(), onValueChangeFinished = { p.set(k, v.toInt()) })
    }
}
