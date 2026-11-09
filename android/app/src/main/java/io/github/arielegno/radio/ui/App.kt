package io.github.arielegno.radio.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.arielegno.radio.RadioApp
import io.github.arielegno.radio.launchSafe

private data class Pestana(val id: String, val nombre: String, val icono: ImageVector)
private val PESTANAS = listOf(
    Pestana("radio", "Radio", Icons.Filled.Radio), Pestana("biblioteca", "Biblioteca", Icons.Filled.LibraryMusic),
    Pestana("stats", "Resumen", Icons.Filled.BarChart), Pestana("revista", "Revista", Icons.Filled.Newspaper),
    Pestana("ajustes", "Ajustes", Icons.Filled.Settings)
)

@Composable
fun App(tema: String, cambiarTema: (String) -> Unit) {
    val ctx = LocalContext.current
    val p = RadioApp.prefs; val r = RadioApp.radio
    val scope = rememberCoroutineScope()
    var pestana by rememberSaveable { mutableStateOf("radio") }
    var carro by rememberSaveable { mutableStateOf(false) }
    var resumen by remember { mutableStateOf(false) }
    var mapa by remember { mutableStateOf(false) }
    var recorrido by remember { mutableStateOf(!p.bool("tourDone", false) && RadioApp.stats.stats.isEmpty()) }
    var destinoRevista by remember { mutableStateOf("") }
    var pedirAgregar by remember { mutableStateOf(false) }
    val vistaRevista = remember { VistaRevista(ctx) { pestana = "radio" } }

    val permisoUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.any { it }) { p.set("ubicacion", true); r.entorno.iniciar() }
    }
    fun pedirUbicacion() = permisoUbicacion.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    val permisoNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { p.set("notifPedido", true) }
    fun pedirNotif() { if (Build.VERSION.SDK_INT >= 33 && !p.bool("notifPedido", false)) permisoNotif.launch(Manifest.permission.POST_NOTIFICATIONS) }

    LaunchedEffect(Unit) { if (r.entorno.permitido()) r.entorno.iniciar() }

    var artistaPedido by remember { mutableStateOf("") }
    val acc = AccionesPortada(
        carro = { carro = true }, resumen = { resumen = true }, mapa = { mapa = true },
        revista = { d -> destinoRevista = d; pestana = "revista" }, biblioteca = { pedirAgregar = true; pestana = "biblioteca" },
        ubicacion = { pedirUbicacion() },
        compartir = {
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "Radio de ruta: tu música con una locutora con IA. https://arielegno.github.io/Radio/"), "Compartir Radio de ruta"))
        },
        probarVoz = { scope.launchSafe { r.probarVoz() } }, probarLuna = { scope.launchSafe { r.probarLuna() } },
        notificaciones = { pedirNotif() },
        abrirArtista = { n -> artistaPedido = n; pestana = "biblioteca" }
    )

    if (carro) { CarroPantalla { carro = false }; return }

    BackHandler(enabled = pestana != "radio" && pestana != "revista") { pestana = "radio" }

    Box(Modifier.fillMaxSize()) {
    FondoCristal()
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            Column {
                MiniReproductor { pestana = "radio" }
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f), tonalElevation = 0.dp) {
                    PESTANAS.forEach { t ->
                        NavigationBarItem(selected = pestana == t.id, onClick = { if (t.id == "revista") destinoRevista = ""; pestana = t.id },
                            icon = { Icon(t.icono, null) }, label = { Text(t.nombre, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Amarillo, selectedIconColor = Tinta))
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (pestana) {
                "radio" -> Portada(acc)
                "biblioteca" -> BibliotecaPantalla(pedirAgregar, { pedirAgregar = false }, { d -> destinoRevista = d; pestana = "revista" }, artistaPedido, { artistaPedido = "" })
                "stats" -> EstadisticasPantalla({ resumen = true }, { mapa = true })
                "revista" -> if (RadioApp.prefs.bool("revistaNativa", false)) rr.revista.RevistaNativa(destinoRevista) { pestana = "radio" }
                    else RevistaPantalla(vistaRevista, destinoRevista) { pestana = "radio" }
                else -> AjustesPantalla(tema, cambiarTema, { recorrido = true }, { pedirUbicacion() }) { pedirAgregar = true; pestana = "biblioteca" }
            }
        }
    }
    }
    if (resumen) ResumenDialogo { resumen = false }
    if (mapa) MapaPantalla { mapa = false }
    if (recorrido) Recorrido { quiereUbicacion ->
        recorrido = false; p.set("tourDone", true)
        if (quiereUbicacion) pedirUbicacion()
    }
}

/** Barra pequeña de lo que suena (visible fuera de la pestaña Radio). */
@Composable
private fun MiniReproductor(irRadio: () -> Unit) {
    val r = RadioApp.radio; val t = r.actual ?: return
    if (!r.activa) return
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Caratula(RadioApp.biblioteca.caratula(t), t.album + t.artist, 40.dp, 6.dp)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(t.name, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
            Text(t.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.tenue)
        }
        androidx.compose.material3.IconButton(onClick = { r.alternarPausa() }) {
            Icon(if (r.sonando) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (r.sonando) "Pausa" else "Seguir")
        }
        androidx.compose.material3.IconButton(onClick = irRadio) { Icon(Icons.Filled.Radio, "Ir a la radio") }
    }
}

private val PASOS = listOf(
    Triple(Icons.Filled.Radio, "Bienvenido a Radio de ruta", "Tu música con Luna, una locutora con inteligencia artificial que entre canción y canción te cuenta del artista, del clima y del camino."),
    Triple(Icons.Filled.LibraryMusic, "Agrega tu música", "En Biblioteca toca «Agregar canciones» y elige tus archivos: se guardan en este celular y suenan sin internet. Si tenías la versión web, restaura tu copia .rrlib."),
    Triple(Icons.Filled.Mic, "Dale voz a Luna", "En Ajustes pega tu clave gratuita de Gemini para que Luna hable con inteligencia artificial. Sin clave también funciona, con frases más sencillas."),
    Triple(Icons.Filled.DirectionsCar, "Para el camino", "El modo carro tiene botones grandes, pantalla encendida y comandos de voz. Configúralo antes de arrancar: no manipules el celular mientras manejas."),
    Triple(Icons.AutoMirrored.Filled.MenuBook, "La Revista", "Artículos, pósters, la Bóveda del tiempo con conciertos históricos, muros y el club de escucha. Está en la pestaña Revista.")
)

@Composable
fun Recorrido(terminar: (Boolean) -> Unit) {
    var i by remember { mutableIntStateOf(0) }
    val (icono, titulo, texto) = PASOS[i]
    val ultimo = i == PASOS.size - 1
    Dialog(onDismissRequest = { terminar(false) }) {
        Column(Modifier.clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Amarillo, Naranja, Rosa))), contentAlignment = Alignment.Center) {
                Icon(icono, null, tint = Tinta, modifier = Modifier.size(36.dp))
            }
            Text(titulo.uppercase(), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
            Text(texto, textAlign = TextAlign.Center, color = LocalExtra.current.tenue)
            Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PASOS.indices.forEach { k ->
                    Box(Modifier.size(width = if (k == i) 22.dp else 8.dp, height = 8.dp).clip(CircleShape).background(if (k == i) Amarillo else LocalExtra.current.borde))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { terminar(false) }, modifier = Modifier.weight(1f)) { Text(if (ultimo) "Cerrar" else "Saltar") }
                Button(onClick = { if (ultimo) terminar(true) else i++ }, modifier = Modifier.weight(1f)) { Text(if (ultimo) "Empezar" else "Siguiente") }
            }
            if (ultimo) Text("Al empezar te pediremos la ubicación (opcional) para el clima y el mapa de viajes.", style = MaterialTheme.typography.bodySmall,
                color = LocalExtra.current.tenue, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        }
    }
}
