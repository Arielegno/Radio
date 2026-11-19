package com.hilo.lector.ui

import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hilo.lector.HiloViewModel
import com.hilo.lector.data.ReaderSettings
import java.io.File

/** Perfiles de lectura: cada uno trae ajustes pensados para esa forma de leer. */
private val PROFILES = listOf(
    Triple("tdah", "Me cuesta mantener la atención", "Colores por línea, interlineado compacto, bloques de 15 minutos y la ruta de estudio."),
    Triple("dislexia", "Me cuesta distinguir las letras", "Letra Atkinson, más grande, más espacio entre líneas, texto alineado a la izquierda y sin guiones."),
    Triple("vision", "Necesito letra grande", "Letra grande y gruesa, alto contraste y márgenes amplios."),
    Triple("estudio", "Estudio intensivo", "Todas las herramientas de estudio a la vista, recordatorio y repaso diario."),
)

fun applyProfile(s: ReaderSettings, p: String): ReaderSettings = when (p) {
    "tdah" -> s.copy(profile = p, colors = true, intensity = 0.85f, lineHeight = 1.35f, align = 1, indent = true, paraSpacing = 0.15f, timerMin = 15, font = 0, fontSize = 18f, weight = 400, checkEvery = 5, goalMin = 15, hyperMin = 90)
    "dislexia" -> s.copy(profile = p, font = 6, fontSize = 20f, lineHeight = 1.7f, align = 0, indent = false, paraSpacing = 0.7f, colors = true, intensity = 0.6f, weight = 400, margin = 22)
    "vision" -> s.copy(profile = p, fontSize = 26f, weight = 550, lineHeight = 1.5f, colorMode = 2, lastNightMode = 2, margin = 14, align = 0, indent = false)
    "estudio" -> s.copy(profile = p, simple = false, colors = true)
    else -> s.copy(profile = p)
}

/** Bienvenida de un minuto: eliges cómo lees y Hilo se ajusta. */
@Composable
fun WelcomeScreen(vm: HiloViewModel, onDone: () -> Unit) {
    val c = LocalHilo.current
    var pick by remember { mutableStateOf(vm.settings.value.profile.ifBlank { "tdah" }) }
    var simple by remember { mutableStateOf(vm.settings.value.simple) }
    BackHandler { onDone() }
    Box(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 620.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Brand() }
                TextButton(onClick = onDone) { Text("Ahora no", color = c.muted) }
            }
            Text("¿Cómo lees mejor?", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = c.ink)
            Text("Elige lo que más se parece a ti. Puedes cambiarlo cuando quieras en Ajustes.", color = c.muted, fontSize = 15.sp)
            PROFILES.forEach { (k, t, d) ->
                val on = k == pick
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (on) c.band else c.surface)
                        .border(2.dp, if (on) c.accent else Color.Transparent, RoundedCornerShape(18.dp)).clickable { pick = k }.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(t, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(d, color = c.muted, fontSize = 14.sp)
                }
            }
            SwitchRow("Empezar en modo simple", "Solo lo esencial: leer, resaltar y repasar. Lo demás queda a un toque.", simple) { simple = it }
            Button(
                onClick = { vm.settings.update { applyProfile(it, pick).copy(simple = simple) }; onDone() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk),
            ) { Text("Empezar", fontSize = 17.sp) }
        }
    }
}

/** Guía de gestos la primera vez que abres un libro. */
@Composable
fun GestureCoach(onDone: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)).pointerInput(Unit) { detectTapGestures { onDone() } },
    ) {
        Row(Modifier.fillMaxSize()) {
            listOf("‹\nPágina\nanterior" to 0.28f, "Toca aquí para ver u ocultar los menús\n\nMantén el dedo sobre una palabra para elegirla; ajusta con las manijas" to 0.44f, "›\nPágina\nsiguiente" to 0.28f).forEachIndexed { i, (t, w) ->
                Box(
                    Modifier.weight(w).fillMaxHeight().padding(6.dp).border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text(t, color = Color.White, fontSize = if (i == 1) 16.sp else 18.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium) }
            }
        }
        Text("◤ Día / noche", color = Color.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.safeDrawing).padding(18.dp))
        Text("Marcador ◥", color = Color.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.safeDrawing).padding(18.dp))
        Text(
            "Toca para empezar", color = Color(0xFFFFC94D), fontWeight = FontWeight.Bold, fontSize = 15.sp,
            modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(28.dp),
        )
    }
}

/** El sistema pide «reducir movimiento» (animaciones desactivadas). */
fun reduceMotion(ctx: Context): Boolean = runCatching {
    android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)

/** Informe de fallos local: si Hilo se cerró, queda guardado para que puedas enviarlo. */
object CrashLog {
    fun file(ctx: Context) = File(ctx.filesDir, "fallo.txt")

    fun install(ctx: Context) {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                file(ctx).writeText(
                    "Hilo ${info.versionName} · Android ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                        "Fecha: ${java.util.Date()}\nHilo: ${t.name}\n\n" + e.stackTraceToString().take(12000)
                )
            }
            prev?.uncaughtException(t, e)
        }
    }

    /** Datos para soporte: versión, equipo, memoria, biblioteca y el registro reciente de Hilo. */
    fun diagnostics(ctx: Context, vm: HiloViewModel): String {
        val info = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull()
        val rt = Runtime.getRuntime()
        val log = runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "200", "--pid=${android.os.Process.myPid()}"))
            p.inputStream.bufferedReader().readText().takeLast(15000)
        }.getOrDefault("(sin registro)")
        return buildString {
            append("Hilo ${info?.versionName} (${info?.longVersionCode})\n")
            append("Android ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Memoria: ${(rt.totalMemory() - rt.freeMemory()) / 1_048_576} MB usados de ${rt.maxMemory() / 1_048_576} MB\n")
            append("Libros: ${vm.repo.books.value.size} · Tarjetas: ${vm.study.cards.size} · Repasos: ${vm.motor.reviews.size}\n")
            append("Libros en la búsqueda: ${runCatching { vm.search.indexed().size }.getOrDefault(0)}\n")
            file(ctx).takeIf { it.exists() }?.let { append("\nÚLTIMO CIERRE INESPERADO\n").append(it.readText()).append("\n") }
            append("\nREGISTRO RECIENTE\n").append(log)
        }
    }
}
