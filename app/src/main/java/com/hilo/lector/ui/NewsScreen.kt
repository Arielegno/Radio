package com.hilo.lector.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hilo.lector.HiloViewModel
import com.hilo.lector.Screen
import com.hilo.lector.data.Changelog

@Composable
private fun VersionCard(v: Changelog.Version, latest: Boolean) {
    val c = LocalHilo.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Versión ${v.name}", color = c.accent, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            if (latest) Text(
                "Actual", color = c.accentInk, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(c.accent).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Text("${v.title} · ${v.date}", color = c.muted, fontSize = 13.sp)
        if (v.added.isNotEmpty()) {
            Text("Nuevo", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
            v.added.forEach { Bullet(it, "＋") }
        }
        if (v.fixed.isNotEmpty()) {
            Text("Arreglado", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
            v.fixed.forEach { Bullet(it, "✓") }
        }
    }
}

@Composable
private fun Bullet(text: String, mark: String) {
    val c = LocalHilo.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(mark, color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text, color = c.ink, fontSize = 15.sp, lineHeight = 21.sp)
    }
}

/** Novedades: qué se agregó y qué se arregló en cada versión. */
@Composable
fun NewsScreen(vm: HiloViewModel) {
    val c = LocalHilo.current
    BackHandler { vm.screen = Screen.Library }
    Box(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = { vm.screen = Screen.Library }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = c.ink) }
                Text("Novedades", style = MaterialTheme.typography.titleLarge, color = c.ink)
            }
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Changelog.versions.forEachIndexed { i, v -> VersionCard(v, i == 0) }
            }
        }
    }
}

/** Aviso que aparece una vez después de actualizar Hilo. */
@Composable
fun NewsDialog(onAll: () -> Unit, onClose: () -> Unit) {
    val c = LocalHilo.current
    val v = Changelog.current
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = c.surface,
        title = { Text("Novedades de la versión ${v.name}") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(v.title, color = c.accent, fontWeight = FontWeight.Bold)
                (v.added + v.fixed).take(7).forEach { Bullet(it, "•") }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Entendido") } },
        dismissButton = { TextButton(onClick = onAll) { Text("Ver todas") } },
    )
}
