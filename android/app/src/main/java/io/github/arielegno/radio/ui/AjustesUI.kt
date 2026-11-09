package io.github.arielegno.radio.ui

import android.content.Intent
import android.net.Uri
import android.speech.tts.Voice
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.arielegno.radio.Datos
import io.github.arielegno.radio.Efectos
import io.github.arielegno.radio.Guia
import io.github.arielegno.radio.RadioApp
import io.github.arielegno.radio.Spotify
import io.github.arielegno.radio.VozPropia
import io.github.arielegno.radio.core.AjustesWeb
import io.github.arielegno.radio.core.Estadisticas
import io.github.arielegno.radio.core.Json
import io.github.arielegno.radio.core.Luna
import io.github.arielegno.radio.core.Modos
import io.github.arielegno.radio.launchSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class Categoria(val id: String, val titulo: String, val sub: String, val icono: ImageVector)
private val CATEGORIAS = listOf(
    Categoria("musica", "Tu música", "Biblioteca o Spotify, agregar música, la guía", Icons.Filled.LibraryMusic),
    Categoria("luna", "Luna, tu locutora", "Nombre, acento, estilo, cada cuánto habla", Icons.Filled.RecordVoiceOver),
    Categoria("ia", "Inteligencia artificial", "Gemini gratis o Claude con tu clave", Icons.Filled.AutoAwesome),
    Categoria("modos", "Modos y frases", "Cómo habla en cada situación", Icons.Filled.Tune),
    Categoria("programas", "Programas y dedicatorias", "La hora del recuerdo, el resumen del domingo", Icons.Filled.Event),
    Categoria("voz", "Voz", "Motor, voces sin internet, estilo y volumen", Icons.Filled.GraphicEq),
    Categoria("sonido", "Sonido", "Ecualizador, nivelador y transición suave", Icons.Filled.Equalizer),
    Categoria("ubicacion", "Ubicación y clima", "Clima, zona y temperatura", Icons.Filled.LocationOn),
    Categoria("apariencia", "Apariencia", "Tema, color, tamaño de letra y portadas", Icons.Filled.Palette),
    Categoria("revista", "Revista", "Conexión con tu Revista", Icons.Filled.Newspaper),
    Categoria("datos", "Copia de ajustes y datos", "Exportar, importar y borrar", Icons.Filled.Save),
    Categoria("guia", "Guía de uso", "Cómo empezar en 4 pasos", Icons.AutoMirrored.Filled.MenuBook),
    Categoria("acerca", "Acerca de", "Versión, créditos y aviso legal", Icons.Filled.Info)
)

/** Claves que nunca salen en la copia de ajustes. */
private val SECRETAS = setOf("geminiKey", "claudeKey", "spToken", "spRefresh", "spVerif", "spEstado", "spExp", "sbKey")

@Composable
fun AjustesPantalla(tema: String, cambiarTema: (String) -> Unit, verRecorrido: () -> Unit, pedirUbicacion: () -> Unit, agregarMusica: () -> Unit = {}) {
    val p = RadioApp.prefs; val r = RadioApp.radio
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var borrarStats by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf("") }
    BackHandler(enabled = cat.isNotEmpty()) { cat = ""; aviso = "" }

    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) scope.launchSafe {
            val datos = linkedMapOf<String, Any?>(
                "app" to "radio-de-ruta", "v" to 1,
                "ajustes" to p.todo().filterKeys { it !in SECRETAS && !it.startsWith("bio_") && !it.startsWith("artof_") },
                "estadisticas" to Json.parse(RadioApp.stats.toJson())
            )
            aviso = try {
                withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)?.use { it.write(Json.write(datos).toByteArray()) } }
                "Copia de ajustes guardada (sin tus claves ni tus audios)."
            } catch (e: Exception) { "No se pudo guardar la copia." }
        }
    }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launchSafe {
            aviso = try {
                val t = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } } ?: ""
                val o = Json.obj(t)
                val deLaWeb = if (o["app"] != "radio-de-ruta") AjustesWeb.convertir(o) else null
                if (deLaWeb != null) {
                    // Archivo exportado desde la página web (radio_ruta_config.txt)
                    p.ponerTodo(deLaWeb)
                    r.ponerFuente(p.str("fuente", "local") == "spotify"); r.ponerModo(p.str("modo", "carretera")); r.ponerMezclar(p.bool("mezclar", true))
                    aplicarApariencia(p.str("acentoColor", "#f2c230"), p.int("letra", 17))
                    "Ajustes de la página web importados (${deLaWeb.size}). Las claves no vienen en ese archivo: pégalas en Inteligencia artificial."
                } else if (o["app"] != "radio-de-ruta") "Ese archivo no es una copia de ajustes de Radio de ruta."
                else {
                    @Suppress("UNCHECKED_CAST")
                    (o["ajustes"] as? Map<String, Any?>)?.let { m -> p.ponerTodo(m.filterKeys { it !in SECRETAS }) }
                    o["estadisticas"]?.let { st ->
                        val nuevas = Estadisticas.fromJson(Json.write(st))
                        RadioApp.stats.stats.putAll(nuevas.stats); RadioApp.stats.hist.putAll(nuevas.hist); RadioApp.stats.favs.putAll(nuevas.favs)
                        Datos.guardarStats()
                    }
                    RadioApp.biblioteca.recargarListas()
                    aplicarApariencia(p.str("acentoColor", "#f2c230"), p.int("letra", 17)); Efectos.aplicar()
                    r.ponerFuente(p.str("fuente", "local") == "spotify"); r.ponerModo(p.str("modo", "carretera")); r.ponerMezclar(p.bool("mezclar", true))
                    "Ajustes restaurados."
                }
            } catch (e: Exception) { "No se pudo leer esa copia." }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (cat.isEmpty()) {
            Text("Ajustes", style = MaterialTheme.typography.headlineLarge)
            Text("Todo se guarda solo en este celular.", color = LocalExtra.current.tenue, modifier = Modifier.padding(bottom = 14.dp))
            Tarjeta {
                CATEGORIAS.forEachIndexed { i, c ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { cat = c.id; aviso = "" }.padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Amarillo.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                            Icon(c.icono, null, tint = Amarillo, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.titulo, fontWeight = FontWeight.SemiBold)
                            Text(c.sub, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue, maxLines = 1)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = LocalExtra.current.tenue)
                    }
                    if (i < CATEGORIAS.size - 1) HorizontalDivider(color = LocalExtra.current.borde, modifier = Modifier.padding(start = 58.dp))
                }
            }
            Text("Radio de ruta para Android · versión 2.0", color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 14.dp).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(24.dp))
            return@Column
        }
        val c = CATEGORIAS.first { it.id == cat }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            IconButton(onClick = { cat = ""; aviso = "" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver a Ajustes") }
            Text(c.titulo, style = MaterialTheme.typography.headlineMedium)
        }
        if (aviso.isNotEmpty()) Text(aviso, color = Amarillo, modifier = Modifier.padding(bottom = 10.dp))
        when (cat) {
            "musica" -> {
                Grupo("De dónde sale la música") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(false to "Mi biblioteca", true to "Spotify").forEach { (sp, n) -> Elegible(n, r.fuenteSpotify == sp) { r.ponerFuente(sp) } }
                    }
                    if (r.fuenteSpotify) {
                        Ayuda("Spotify necesita Premium. La música suena en la app oficial de Spotify y Radio de ruta la controla: Luna habla entre canciones igual que con tu biblioteca.")
                        CampoTexto("spClientId", "Client ID de Spotify", "")
                        var red by remember { mutableStateOf(Spotify.redireccion()) }
                        Text("Dirección de regreso (Redirect URI)", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        Ayuda("Tiene que estar escrita EXACTAMENTE igual en developer.spotify.com › tu app › Settings › Redirect URIs (si no, Spotify dice «redirect_uri: Not matching configuration»). Si ya usas Spotify en la web, elige la de la web: la página te devuelve a la app.")
                        io.github.arielegno.radio.core.SpotifyDatos.REDIRECCIONES.forEach { u ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { red = u; p.set("spRedir", u) }.padding(vertical = 2.dp)) {
                                androidx.compose.material3.RadioButton(selected = red == u, onClick = { red = u; p.set("spRedir", u) })
                                Text(u, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                IconButton(onClick = {
                                    (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("Redirect URI", u))
                                    android.widget.Toast.makeText(ctx, "Copiado", android.widget.Toast.LENGTH_SHORT).show()
                                }) { Icon(Icons.Filled.ContentCopy, "Copiar") }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            if (Spotify.conectado) {
                                Text("✓ Conectado", color = Amarillo, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = { Spotify.desconectar() }) { Text("Desconectar") }
                            } else Boton("Conectar con Spotify") { Spotify.entrar(ctx) }
                            TextButton(onClick = { abrir(ctx, "https://developer.spotify.com/dashboard") }) { Text("Abrir panel") }
                        }
                        if (Spotify.aviso.isNotEmpty()) Ayuda(Spotify.aviso)
                    }
                }
                Grupo("Tu biblioteca") {
                    Ayuda("${RadioApp.biblioteca.pistas.size} canciones guardadas en este celular, tal cual el archivo original (sin comprimir).")
                    Boton("Agregar música o restaurar copia", agregarMusica)
                    Interruptor("mezclar", "Orden aleatorio al iniciar la radio", true) { r.ponerMezclar(it) }
                }
                Grupo("Tu nube de datos") {
                    Ayuda("La misma dirección que usas en la web (Netlify, GitHub Pages…). De ahí salen las fichas de artistas (artistas/<nombre>.json) y festivales (festivales/<nombre>.json). Si no hay, se usa Wikipedia y la IA.")
                    CampoTexto("nube", "Dirección de tu nube (https://…)", "")
                }
                Grupo("La guía (limpiar y completar)") {
                    Ayuda("Limpia títulos («Video oficial», «VEVO», «Lyrics»…), completa álbum, año y carátula con MusicBrainz e iTunes, y respeta lo que corriges a mano. Se hace sola al agregar canciones.")
                    if (Guia.trabajando) {
                        Text(Guia.progreso.ifEmpty { "Trabajando…" }, color = Amarillo)
                        TextButton(onClick = { Guia.detener() }) { Text("Detener") }
                    } else Boton("Limpiar y completar ahora") { scope.launchSafe { aviso = Guia.mantener(true) } }
                    Interruptor("soloWifi", "Descargar carátulas solo por Wi-Fi", false)
                }
            }
            "luna" -> {
                Grupo("Quién es") {
                    CampoTexto("host", "Nombre de la locutora", "Luna")
                    Opciones("genero", "Es", linkedMapOf("locutora" to "Locutora", "locutor" to "Locutor"), "locutora")
                    Opciones("acento", "Acento", Luna.ACENTOS.mapValues { (k, v) -> if (k == "neutro") "Neutro" else "Español $v" }, "neutro")
                    Opciones("estilo", "Estilo", Luna.ESTILOS, "")
                    CampoTexto("persona", "Personalidad (opcional)", "", lineas = 2, ayuda = "Ej: bromista, fan del rock, le encantan los datos curiosos")
                }
                Grupo("Cuándo y cuánto habla") {
                    Opciones("largo", "Largo de cada intervención", linkedMapOf("corta" to "Corta", "media" to "Media", "larga" to "Larga"), "media")
                    Numero("cadaN", "Habla cada cuántas canciones", 1, 1, 10)
                    Numero("wMin", "Hora y clima cada cuántos minutos", 30, 5, 180, paso = 5)
                    Numero("pEvery", "Zona cada cuántas intervenciones", 2, 1, 20)
                }
                Grupo("Lo que sabe de ti") {
                    CampoTexto("me", "Tu nombre (para saludarte)", "")
                    Interruptor("memoria", "Que Luna recuerde lo que más escuchas", true)
                    CampoTexto("contexto", "Contexto musical de tu biblioteca (opcional)", "", lineas = 2, ayuda = "Ej: rock latino de los 80 y 90, algo de cumbia y baladas")
                    CampoTexto("evitar", "Temas a evitar (opcional)", "", lineas = 2)
                }
            }
            "ia" -> {
                Grupo("Quién escribe lo que dice Luna") {
                    var prov by remember { mutableStateOf(p.str("proveedor", "gemini")) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("gemini" to "Gemini (gratis)", "claude" to "Claude (de pago)").forEach { (k, n) -> Elegible(n, prov == k) { prov = k; p.set("proveedor", k) } }
                    }
                    Ayuda("Las claves se guardan solo en este celular y no salen en la copia de ajustes. Sin clave, Luna usa frases sencillas.")
                }
                Grupo("Gemini (Google AI Studio)") {
                    CampoTexto("geminiKey", "Clave de Gemini", "", secreto = true)
                    Opciones("geminiModelSel", "Modelo", linkedMapOf("gemini-3.5-flash-lite" to "Lite (más peticiones)", "gemini-3.5-flash" to "Flash (equilibrado)", "gemini-flash-latest" to "Flash más reciente"), "gemini-3.5-flash-lite")
                    CampoTexto("geminiModel", "Modelo personalizado (opcional)", "")
                    Boton("Conseguir clave gratis") { abrir(ctx, "https://aistudio.google.com/apikey") }
                }
                Grupo("Claude (Anthropic)") {
                    CampoTexto("claudeKey", "Clave de Anthropic", "", secreto = true)
                    CampoTexto("claudeModel", "Modelo de Claude", "claude-sonnet-5-5")
                    Ayuda("Claude cobra por uso según tu cuenta de Anthropic. Úsala solo en tu propio celular.")
                }
            }
            "modos" -> {
                var m by rememberSaveable { mutableStateOf(r.modo) }
                Grupo("Modo a personalizar") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Modos.NOMBRES.forEach { k -> Elegible(Modos.ETIQUETA[k] ?: k, m == k) { m = k } }
                    }
                    Ayuda("El modo que suena ahora se cambia en la portada de la radio.")
                }
                key(m) {
                    Grupo("Cómo habla en «${Modos.ETIQUETA[m]}»") {
                        CampoTexto("modo_$m", "Situación (vacío = texto por defecto)", "", lineas = 3, ayuda = Modos.DESCRIPCION[m] ?: "")
                        CampoTexto("frases_$m", "Frases propias (una por línea)", "", lineas = 4, ayuda = (Modos.FRASES[m] ?: emptyList()).joinToString(" / "))
                        Ayuda("Cada modo usa solo sus frases y las de Neutral.")
                    }
                }
            }
            "programas" -> {
                Grupo("Programas de Luna") {
                    Interruptor("progRec", "«La hora del recuerdo» una vez al día", false)
                    Opciones("progRecH", "A las", (0..23).associate { h -> h.toString() to (if (h == 0) "12 a. m." else if (h < 12) "$h a. m." else if (h == 12) "12 p. m." else "${h - 12} p. m.") }, "20", entero = true)
                    Interruptor("progSem", "«El resumen del domingo»: lo que más sonó en el mes", false)
                }
                Grupo("Para alguien especial") {
                    CampoTexto("dedic", "Dedicatoria (se lee una sola vez)", "", lineas = 2)
                    CampoTexto("efemerides", "Efemérides propias (una por línea, MM-DD: texto)", "", lineas = 3, ayuda = "Ej: 03-14: Hoy cumple años mi mamá")
                }
            }
            "voz" -> {
                var versionVoz by remember { mutableIntStateOf(0) }
                val motores by produceState<List<Pair<String, String>>>(emptyList(), versionVoz) { value = r.voz.motores() }
                val voces by produceState<List<Voice>>(emptyList(), versionVoz) { value = r.voz.vocesEspanol() }
                Grupo("Motor y voz") {
                    val propias = remember(VozPropia.version) { VozPropia.VOCES.filter { VozPropia.instalada(it) }.associate { (VozPropia.PREFIJO + it.id) to "${it.nombre} (de la app, sin internet)" } }
                    if (motores.size > 1 || propias.isNotEmpty()) key(VozPropia.version) {
                        Opciones("motorVoz", "Motor de voz", linkedMapOf("" to "El del teléfono") + propias + motores.filter { it.first != "" }.toMap(), "") { mv -> r.voz.cambiarMotor(mv); versionVoz++ }
                    }
                    key(versionVoz) {
                        if (voces.isNotEmpty()) Opciones("voz", "Voz", linkedMapOf("" to "La de siempre") + voces.associate { it.name to (it.locale.displayName + " · " + it.name.substringAfterLast('#').takeLast(18)) }, "")
                    }
                    TextButton(onClick = { try { ctx.startActivity(Intent("com.android.settings.TTS_SETTINGS")) } catch (e: Exception) {} }) { Text("Ajustes de voz del teléfono") }
                }
                Grupo("Cómo suena") {
                    var preset by remember { mutableStateOf(p.str("vozPreset", "natural")) }
                    Opciones("vozPreset", "Estilo de voz", linkedMapOf("natural" to "Natural (recomendado)", "radio" to "Radio FM (cálido)", "narrador" to "Narrador (claro)", "energico" to "Enérgico (rápido)", "manual" to "Manual"), "natural") { preset = it }
                    if (preset == "manual") { Deslizador("velocidad", "Velocidad", 100, 60, 150); Deslizador("tono", "Tono", 100, 60, 150) }
                    Deslizador("volVoz", "Volumen de la voz", 100, 10, 100)
                    Interruptor("hablarEncima", "Hablar encima de la música (la canción sigue bajita, como en la radio)", true)
                    Deslizador("nivelEncima", "Volumen de la música mientras habla", 18, 5, 60)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        Boton("Probar voz") { scope.launchSafe { r.probarVoz() } }
                        Boton("Probar a Luna") { scope.launchSafe { r.probarLuna() } }
                    }
                }
                Grupo("Voces de la app (sin internet)") {
                    Ayuda("Voces naturales que funcionan dentro de la app, sin SherpaTTS ni internet. Se descargan una sola vez (usa Wi-Fi).")
                    if (!VozPropia.disponible) Ayuda("Este teléfono no admite las voces de la app; usa el motor del teléfono.")
                    else VozPropia.VOCES.forEach { v ->
                        val lista = remember(VozPropia.version, VozPropia.descargando) { VozPropia.instalada(v) }
                        val enUso = p.str("motorVoz", "") == VozPropia.PREFIJO + v.id
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(v.nombre + if (enUso) " · en uso" else "", fontWeight = FontWeight.SemiBold)
                            Text(v.detalle + " · ${v.mb} MB", color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall)
                            if (VozPropia.descargando == v.id) {
                                androidx.compose.material3.LinearProgressIndicator(progress = { VozPropia.progreso }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                                Text(if (VozPropia.progreso < .85f) "Descargando… ${(VozPropia.progreso / .85f * 100).toInt()} %" else "Preparando la voz…", color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall)
                            } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                if (!lista) Boton("Descargar") { scope.launchSafe { aviso = VozPropia.descargar(v); versionVoz++ } }
                                else {
                                    if (!enUso) Boton("Usar") { p.set("motorVoz", VozPropia.PREFIJO + v.id); r.voz.cambiarMotor(VozPropia.PREFIJO + v.id); versionVoz++ }
                                    Boton("Probar") { scope.launchSafe { if (!VozPropia.decir(v.id, "Hola, soy ${p.str("host", "Luna")}. Así sueno con la voz de ${v.nombre}.", 1f, p.int("volVoz", 100) / 100f)) aviso = "No se pudo usar la voz." } }
                                    Boton("Borrar") { VozPropia.borrar(v); versionVoz++ }
                                }
                            }
                        }
                    }
                }
                val instaladas = remember(VozPropia.version) { VozPropia.VOCES.filter { VozPropia.instalada(it) } }
                if (instaladas.isNotEmpty()) Grupo("Efectos y dos locutores") {
                    Opciones("efectoVoz", "Efecto de voz", linkedMapOf("" to "Ninguno", "fm" to "Locutor FM (grave y con cuerpo)", "telefono" to "Teléfono", "estadio" to "Eco de estadio", "walkie" to "Walkie-talkie"), "")
                    Ayuda("Los efectos funcionan con las voces de la app.")
                    if (instaladas.size >= 2) {
                        Interruptor("dosLocutores", "Dos locutores: conversan entre canciones", false) { versionVoz++ }
                        key(versionVoz) {
                            if (p.bool("dosLocutores", false)) {
                                CampoTexto("host2", "Nombre del compañero", "Leo")
                                Opciones("vozPareja", "Voz del compañero", linkedMapOf("" to "Elige una voz") + instaladas.associate { it.id to it.nombre }, "")
                                Ayuda("La voz de ${p.str("host", "Luna")} es la que elegiste arriba como motor de voz; el compañero usa esta otra. Con IA, escriben un diálogo corto entre los dos.")
                            }
                        }
                    } else Ayuda("Descarga las dos voces para activar «Dos locutores».")
                }
                Grupo("Otras voces gratis (apps aparte)") {
                    Ayuda("Instala «SherpaTTS» (voces Piper y Kokoro, código abierto) o «RHVoice», descarga una voz en español y elígela arriba como motor de voz.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Boton("SherpaTTS") { abrir(ctx, "https://search.f-droid.org/?q=SherpaTTS&lang=es") }
                        Boton("RHVoice") { abrir(ctx, "https://rhvoice.org/") }
                    }
                }
            }
            "sonido" -> {
                var eqOn by remember { mutableStateOf(p.bool("eqOn", false)) }
                var est by remember { mutableStateOf(p.str("eqPreset", "flat")) }
                var g by remember { mutableStateOf(Efectos.ganancias().toList()) }
                Grupo("Ecualizador") {
                    Interruptor("eqOn", "Usar ecualizador", false) { eqOn = it; Efectos.aplicar() }
                    if (eqOn) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Efectos.ESTILOS.forEach { (k, n) -> Elegible(n, est == k) { est = k; p.set("eqPreset", k); g = Efectos.ganancias().toList(); Efectos.aplicar() } }
                        }
                        Efectos.ETIQUETAS.forEachIndexed { i, et ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$et Hz", modifier = Modifier.width(64.dp), style = MaterialTheme.typography.bodySmall)
                                Slider(value = g[i].toFloat(), onValueChange = { v -> g = g.toMutableList().also { it[i] = v.toInt() } }, valueRange = -12f..12f, steps = 23,
                                    onValueChangeFinished = { Efectos.guardarPersonalizado(g.toIntArray()); est = "custom"; Efectos.aplicar() }, modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(thumbColor = Amarillo, activeTrackColor = Amarillo))
                                Text("${if (g[i] > 0) "+" else ""}${g[i]} dB", modifier = Modifier.width(56.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Grupo("Volumen y transición") {
                    Interruptor("levelOn", "Nivelar volumen entre canciones (útil en el carro)", false) { Efectos.aplicar() }
                    Interruptor("fundido", "Transición suave entre canciones (al empezar y al terminar)", false)
                    Ayuda("Apagados por defecto para escuchar el sonido original. Solo aplican a tu biblioteca; para Spotify, sube la calidad en la app de Spotify. Si notas cortes con la pantalla apagada, apágalos.")
                }
            }
            "ubicacion" -> Grupo("Ubicación y clima") {
                Interruptor("ubicacion", "Usar mi ubicación (clima, zona y mapa de viajes)", true) { on -> if (on) pedirUbicacion() else r.entorno.detener() }
                Opciones("unidades", "Temperatura", linkedMapOf("c" to "Grados Celsius", "f" to "Grados Fahrenheit"), "c")
                CampoTexto("nomEmail", "Correo para OpenStreetMap (opcional)", "", ayuda = "Solo si usas mucho la app")
                Ayuda("La ubicación se usa solo en tu celular para el clima (Open-Meteo), el nombre de la zona (OpenStreetMap) y tu mapa de viajes.")
            }
            "apariencia" -> {
                Grupo("Tema") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "Automático", "dark" to "Oscuro", "light" to "Claro").forEach { (k, n) -> Elegible(n, tema == k) { cambiarTema(k) } }
                    }
                }
                Grupo("Banners de la portada") {
                    Ayuda("El estilo del saludo y de «Cerca de» en la portada.")
                    var ban by remember { mutableStateOf(estiloBanner()) }
                    Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ESTILOS_BANNER.forEach { (k, n) -> Elegible(n, ban == k) { ban = k; p.set("banner", k) } }
                    }
                    Banner(ban, "Vista previa", "Cerca de tu ciudad", "", variante = 1, modifier = Modifier.padding(top = 8.dp), tamTitulo = 24.sp)
                }
                Grupo("Color de acento") {
                    var sel by remember { mutableStateOf(p.str("acentoColor", "#f2c230")) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        ACENTOS_COLOR.forEach { (hex, n) ->
                            val col = Color(android.graphics.Color.parseColor(hex))
                            Box(Modifier.size(44.dp).clip(CircleShape).background(col).border(if (sel == hex) 3.dp else 0.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                .clickable { sel = hex; p.set("acentoColor", hex); aplicarApariencia(hex, p.int("letra", 17)) }, contentAlignment = Alignment.Center) {
                                if (sel == hex) Icon(Icons.Filled.Check, n, tint = Tinta)
                            }
                        }
                    }
                }
                Grupo("Tamaño de letra") {
                    var l by remember { mutableFloatStateOf(p.int("letra", 17).toFloat()) }
                    Text("Así se ve el texto (${l.toInt()})", fontSize = (l * 0.94f).sp)
                    Slider(value = l, onValueChange = { l = it }, valueRange = 15f..24f, steps = 8,
                        onValueChangeFinished = { p.set("letra", l.toInt()); aplicarApariencia(p.str("acentoColor", "#f2c230"), l.toInt()) },
                        colors = SliderDefaults.colors(thumbColor = Amarillo, activeTrackColor = Amarillo))
                }
                Grupo("Otros") {
                    Opciones("portadas", "Tamaño de las portadas", linkedMapOf("small" to "Pequeño (4 columnas)", "medium" to "Mediano (3 columnas)", "large" to "Grande (2 columnas)"), "medium")
                    Opciones("h12", "Formato de hora", linkedMapOf("auto" to "Según el teléfono", "1" to "12 horas", "0" to "24 horas"), "auto")
                    Ayuda("La portada de la radio (vinilo, tarjetas, colores) se cambia con «Personalizar portada», al final de la Radio.")
                }
            }
            "revista" -> Grupo("Revista (Supabase)") {
                Interruptor("revistaNativa", "Revista nativa (beta): más rápida, hecha en Kotlin", false)
                Ayuda("Es la misma Revista, con tu misma cuenta y los mismos artículos, pero dibujada por la app en vez de una página web. Si algo no funciona, apágalo y vuelves a la de siempre.")
                Ayuda("La Revista ya viene conectada. Llena esto solo si quieres usar otro proyecto de Supabase en la portada de la app.")
                CampoTexto("sbUrl", "URL del proyecto (opcional)", "", ayuda = "https://xxxx.supabase.co")
                CampoTexto("sbKey", "Clave pública (opcional)", "", secreto = true)
                Ayuda("Nunca pegues aquí la clave «secret» ni «service_role».")
            }
            "datos" -> {
                Grupo("Copia de ajustes") {
                    Ayuda("Guarda tus ajustes, favoritas, playlists y estadísticas en un archivo, sin tus claves ni tus audios (para los audios usa «Guardar copia de mi música» en Biblioteca). También puedes importar el archivo que exporta la página web (radio_ruta_config.txt).")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Boton("Exportar copia") { exportar.launch("radio-de-ruta-ajustes.json") }
                        Boton("Importar copia") { importar.launch(arrayOf("application/json", "text/*", "*/*")) }
                    }
                }
                Grupo("Borrar") {
                    Boton("Borrar estadísticas e historial") { borrarStats = true }
                    Ayuda("Tu música, tus claves y tus estadísticas se guardan solo en este celular.")
                }
            }
            "guia" -> {
                listOf(
                    "1. Configura tu música" to "Elige tu fuente en Ajustes › Tu música: tu biblioteca (agrega una carpeta completa) o Spotify (requiere Premium).",
                    "2. Dale vida a la locutora" to "Consigue una clave gratis de Gemini en Google AI Studio y pégala en Ajustes › Inteligencia artificial. Para una voz más natural, instala SherpaTTS (Piper o Kokoro) y elígela en Ajustes › Voz.",
                    "3. Personaliza tu experiencia" to "Cambia el modo de escucha en la portada, la personalidad de Luna y cada cuántos minutos da la hora y el clima.",
                    "4. ¡A rodar!" to "Vuelve a Radio, toca Iniciar radio y acepta el permiso de ubicación para el clima y la zona. En el carro usa el modo carro."
                ).forEach { (t, d) -> Grupo(t) { Text(d) } }
                Boton("Ver el recorrido de bienvenida", verRecorrido)
                Ayuda("No manipules la app mientras conduces.")
            }
            else -> {
                Grupo("Novedades") {
                    var todas by remember { mutableStateOf(false) }
                    io.github.arielegno.radio.core.Novedades.LISTA.take(if (todas) 99 else 2).forEach { (v, l) ->
                        Text("Versión $v", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                        l.forEach { Ayuda("• $it") }
                    }
                    if (!todas) TextButton(onClick = { todas = true }) { Text("Ver todas las versiones") }
                }
                Grupo("Reportes de errores") {
                    var version by remember { mutableIntStateOf(0) }
                    val lista = remember(version) { io.github.arielegno.radio.Reportes.todos() }
                    Ayuda(if (lista.isEmpty()) "No hay reportes: la app no se ha cerrado sola." else "Si la app se cerró sola, aquí está qué pasó. Compártelo para que se pueda arreglar (no incluye tus claves ni tu música).")
                    lista.take(5).forEach { f ->
                        val t = remember(f) { runCatching { f.readText() }.getOrDefault("") }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(t.lineSequence().firstOrNull().orEmpty(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = { io.github.arielegno.radio.Reportes.compartir(ctx, t) }) { Text("Compartir") }
                        }
                    }
                    if (lista.isNotEmpty()) TextButton(onClick = { io.github.arielegno.radio.Reportes.borrarTodos(); version++ }) { Text("Borrar reportes") }
                }
                Grupo("Datos guardados") {
                    var version by remember { mutableIntStateOf(0) }
                    val mb = remember(version) { io.github.arielegno.radio.Almacen.tamano() / 1_048_576.0 }
                    Ayuda("Biografías, fichas de artistas y comentarios de Luna ya generados: %.1f MB. Se conservan al actualizar la app.".format(mb))
                    TextButton(onClick = { io.github.arielegno.radio.Almacen.vaciar(); version++; aviso = "Datos guardados borrados: se volverán a generar cuando hagan falta." }) { Text("Borrar datos guardados") }
                }
                Grupo("Radio de ruta para Android") {
                Text("Versión 2.0 · por Arielegno", fontWeight = FontWeight.SemiBold)
                Ayuda("No manipules la app mientras conduces: configúrala antes de arrancar.")
                TextButton(onClick = { abrir(ctx, "https://arielegno.github.io/Radio/legal.html") }) { Text("Aviso legal y privacidad") }
                Ayuda("Usa Media3 (Apache 2.0), Jetpack Compose (Apache 2.0), sherpa-onnx (Apache 2.0) con voces Piper, Apache Commons Compress (Apache 2.0), Leaflet (BSD-2) y datos de OpenStreetMap (ODbL), Open-Meteo, Wikipedia (CC BY-SA), MusicBrainz (CC0), iTunes Search, Spotify, Google Gemini y Anthropic Claude.")
            }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
    if (borrarStats) AlertDialog(onDismissRequest = { borrarStats = false }, title = { Text("¿Borrar estadísticas?") },
        text = { Text("Se borran las más escuchadas, tu resumen y el mapa de viajes. Tus favoritas y tu música se quedan.") },
        confirmButton = { TextButton(onClick = { RadioApp.stats.clear(); Datos.guardarStats(); borrarStats = false; aviso = "Estadísticas borradas." }) { Text("Borrar") } },
        dismissButton = { TextButton(onClick = { borrarStats = false }) { Text("Cancelar") } })
}

/** Grupo de ajustes en una tarjeta de cristal. */
@Composable
private fun Grupo(titulo: String, contenido: @Composable () -> Unit) {
    Tarjeta(Modifier.padding(bottom = 14.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = Amarillo, modifier = Modifier.padding(bottom = 6.dp))
        contenido()
    }
}

private fun abrir(ctx: android.content.Context, url: String) {
    try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) {}
}

@Composable
private fun Ayuda(t: String) = Text(t, color = LocalExtra.current.tenue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))

@Composable
private fun Boton(t: String, onClick: () -> Unit) = FilledTonalButton(onClick = onClick, shape = RoundedCornerShape(14.dp)) { Text(t) }

@Composable
private fun Elegible(t: String, on: Boolean, onClick: () -> Unit) =
    FilterChip(selected = on, onClick = onClick, label = { Text(t, maxLines = 1) }, shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Amarillo, selectedLabelColor = Tinta))

@Composable
private fun CampoTexto(k: String, etiqueta: String, d: String, secreto: Boolean = false, lineas: Int = 1, ayuda: String = "") {
    val p = RadioApp.prefs
    var v by remember(k) { mutableStateOf(p.str(k, d)) }
    var ver by remember { mutableStateOf(false) }
    OutlinedTextField(value = v, onValueChange = { v = it; p.set(k, it) }, label = { Text(etiqueta) }, shape = RoundedCornerShape(14.dp),
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
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { v = !v; p.set(k, v); alCambiar(v) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(t, Modifier.weight(1f).padding(end = 10.dp))
        Switch(checked = v, onCheckedChange = { v = it; p.set(k, it); alCambiar(it) }, colors = SwitchDefaults.colors(checkedTrackColor = Amarillo, checkedThumbColor = Tinta))
    }
}

@Composable
private fun Opciones(k: String, t: String, ops: Map<String, String>, d: String, entero: Boolean = false, alCambiar: (String) -> Unit = {}) {
    val p = RadioApp.prefs
    var v by remember(k) { mutableStateOf(if (entero) p.int(k, d.toInt()).toString() else p.str(k, d)) }
    var abierto by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { abierto = true }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t, Modifier.weight(1f).padding(end = 10.dp))
            Text(ops[v] ?: v.ifEmpty { "—" }, color = Amarillo, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            ops.forEach { (k2, n) ->
                DropdownMenuItem(text = { Text(if (k2 == v) "✓  $n" else n) }, onClick = {
                    v = k2; abierto = false
                    if (entero) p.set(k, k2.toInt()) else p.set(k, k2)
                    alCambiar(k2)
                })
            }
        }
    }
}

@Composable
private fun Numero(k: String, t: String, d: Int, min: Int, max: Int, paso: Int = 1) {
    val p = RadioApp.prefs
    var v by remember(k) { mutableIntStateOf(p.int(k, d).coerceIn(min, max)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(t, Modifier.weight(1f))
        TextButton(onClick = { v = (v - paso).coerceAtLeast(min); p.set(k, v) }, enabled = v > min) { Text("−", fontSize = 20.sp) }
        Text("$v", fontWeight = FontWeight.Bold, color = Amarillo, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
        TextButton(onClick = { v = (v + paso).coerceAtMost(max); p.set(k, v) }, enabled = v < max) { Text("+", fontSize = 20.sp) }
    }
}

@Composable
private fun Deslizador(k: String, t: String, d: Int, min: Int, max: Int) {
    val p = RadioApp.prefs
    var v by remember(k) { mutableFloatStateOf(p.int(k, d).toFloat()) }
    Column(Modifier.padding(vertical = 4.dp)) {
        Text("$t: ${v.toInt()} %")
        Slider(value = v, onValueChange = { v = it }, valueRange = min.toFloat()..max.toFloat(), onValueChangeFinished = { p.set(k, v.toInt()) },
            colors = SliderDefaults.colors(thumbColor = Amarillo, activeTrackColor = Amarillo))
    }
}
