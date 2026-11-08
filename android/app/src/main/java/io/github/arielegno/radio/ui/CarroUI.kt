package io.github.arielegno.radio.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.arielegno.radio.RadioApp
import io.github.arielegno.radio.core.Voz
import io.github.arielegno.radio.core.VozCmd

/** Modo carro: botones grandes, pantalla encendida y comandos de voz. */
@Composable
fun CarroPantalla(cerrar: () -> Unit) {
    val r = RadioApp.radio
    val vista = LocalView.current
    DisposableEffect(Unit) { vista.keepScreenOn = true; onDispose { vista.keepScreenOn = false } }
    BackHandler(onBack = cerrar)
    var oido by remember { mutableStateOf("Di «siguiente», «pausa» o «pon Soda Stereo»") }
    var fav by remember(r.actual?.id) { mutableStateOf(r.actual?.let { RadioApp.stats.isFav(it.name, it.artist) } ?: false) }

    fun ejecutar(frase: String) {
        val c = Voz.entender(frase, RadioApp.biblioteca.pistas)
        when (c) {
            VozCmd.Siguiente -> r.siguienteCancion()
            VozCmd.Anterior -> r.anteriorCancion()
            VozCmd.Pausa -> r.pausar()
            VozCmd.Sigue -> r.seguir()
            VozCmd.Favorita -> { val t = r.actual; if (t != null && !RadioApp.stats.isFav(t.name, t.artist)) r.alternarFavorita()?.let { fav = it } }
            VozCmd.SoloMusica -> r.ponerAire(false)
            VozCmd.Luna -> r.ponerAire(true)
            VozCmd.Salir -> cerrar()
            is VozCmd.Poner -> if (r.fuenteSpotify) r.ponerEnSpotify(c.etiqueta) else { val ids = c.ids.toSet(); r.iniciar({ ids.contains(it.id) }) }
            is VozCmd.NoEncontrado -> if (r.fuenteSpotify) r.ponerEnSpotify(c.busqueda)
            is VozCmd.NoEntendi -> {}
        }
        oido = if (r.fuenteSpotify && c is VozCmd.NoEncontrado) "Buscando «${c.busqueda}» en Spotify…" else Voz.respuesta(c)
    }

    val escuchar = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val frase = if (res.resultCode == Activity.RESULT_OK) res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() else null
        if (frase.isNullOrBlank()) oido = "No escuché nada. Toca el micrófono otra vez." else ejecutar(frase)
    }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF26222E), Color(0xFF0E0F12)))).systemBarsPadding().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(r.entorno.zona.ifEmpty { "Modo carro" }, color = Color.White.copy(alpha = 0.85f), fontSize = 18.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0x22FFFFFF)).clickable(onClick = cerrar), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, "Salir del modo carro", tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Text((r.actual?.name ?: "Nada sonando").uppercase(), color = Color.White, fontSize = 40.sp, lineHeight = 42.sp, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
        Text(r.actual?.artist ?: "Toca play para empezar", color = Amarillo, fontSize = 24.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.clip(RoundedCornerShape(999.dp)).border(2.dp, Color(0x2EFFFFFF), RoundedCornerShape(999.dp)).background(Color(0x10FFFFFF))
                .clickable {
                    oido = "Te escucho…"
                    val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                        .putExtra(RecognizerIntent.EXTRA_PROMPT, "Di un comando: siguiente, pausa, pon…")
                    try { escuchar.launch(i) } catch (e: ActivityNotFoundException) { oido = "Este teléfono no tiene reconocimiento de voz. Instala la app de Google." }
                }.padding(horizontal = 26.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Mic, null, tint = Color.White, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(10.dp))
                Text("Toca y habla", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(oido, color = Color(0xFFCFD5DD), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 18.dp))
        }
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonCarro(Icons.Filled.SkipPrevious, "Anterior", Modifier.weight(1f)) { r.anteriorCancion() }
            Box(Modifier.weight(1.3f).fillMaxSize().clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(Amarillo, Naranja))).clickable { r.alternarPausa() },
                contentAlignment = Alignment.Center) {
                Icon(if (r.sonando || r.lunaHablando) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (r.sonando) "Pausa" else "Reproducir", tint = Tinta, modifier = Modifier.size(64.dp))
            }
            BotonCarro(Icons.Filled.SkipNext, "Siguiente", Modifier.weight(1f)) { r.siguienteCancion() }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonCarro(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favorita", Modifier.weight(1f), texto = "Favorita") { r.alternarFavorita()?.let { fav = it } }
            BotonCarro(if (r.aire) Icons.Filled.Mic else Icons.Filled.MusicNote, if (r.aire) "Luna activa" else "Solo música", Modifier.weight(1f),
                texto = if (r.aire) "Luna activa" else "Solo música", resaltado = r.aire) { r.ponerAire(!r.aire) }
            BotonCarro(Icons.Filled.Bedtime, "Apagar en 30 minutos", Modifier.weight(1f), texto = if (r.apagarEn > 0) "Se apaga" else "Apagar") { r.temporizador(if (r.apagarEn > 0) 0 else 30) }
        }
        Text("Configura todo antes de arrancar. No manipules el celular mientras manejas.", color = Color(0x99FFFFFF), textAlign = TextAlign.Center,
            fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
    }
}

@Composable
private fun BotonCarro(icono: ImageVector, desc: String, modifier: Modifier, texto: String = "", resaltado: Boolean = false, onClick: () -> Unit) {
    Column(modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)).background(if (resaltado) Amarillo.copy(alpha = 0.22f) else Color(0xFF2A2D33)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icono, desc, tint = if (resaltado) Amarillo else Color.White, modifier = Modifier.size(if (texto.isEmpty()) 52.dp else 34.dp))
        if (texto.isNotEmpty()) Text(texto, color = if (resaltado) Amarillo else Color.White, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
