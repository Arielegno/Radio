package com.hilo.lector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hilo.lector.ui.GuideScreen
import com.hilo.lector.ui.HiloTheme
import com.hilo.lector.ui.LibraryScreen
import com.hilo.lector.ui.LocalHilo
import com.hilo.lector.ui.PasteDialog
import com.hilo.lector.ui.ReaderScreen
import com.hilo.lector.ui.SettingsSheet
import com.hilo.lector.ui.StudyScreen
import com.hilo.lector.ui.TimerSheet

class MainActivity : ComponentActivity() {
    private val vm: HiloViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        runCatching { com.hilo.lector.data.Reminders.schedule(this) }
        setContent {
            val s by vm.settings.state.collectAsStateWithLifecycle()
            HiloTheme(s.theme) { HiloRoot(vm) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Recibe archivos y textos de "Abrir con" y del menú Compartir. */
    private fun handleIntent(i: Intent?) {
        if (i == null) return
        if (i.getBooleanExtra(com.hilo.lector.data.Reminders.EXTRA_STUDY, false)) {
            i.removeExtra(com.hilo.lector.data.Reminders.EXTRA_STUDY)
            vm.openStudy(null)
            return
        }
        when (i.action) {
            Intent.ACTION_VIEW -> i.data?.let { vm.import(listOf(it)) }
            Intent.ACTION_SEND -> {
                val uri = IntentCompat.getParcelableExtra(i, Intent.EXTRA_STREAM, Uri::class.java)
                if (uri != null) {
                    vm.import(listOf(uri))
                } else {
                    val text = i.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                    if (text.isNotBlank()) {
                        val words = text.trim().split(Regex("\\s+")).size
                        if (words > 8) vm.addText(i.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty(), text)
                        else vm.toast = "Llegó solo un enlace o una frase corta. Copia el texto del artículo y compártelo o pégalo en Hilo."
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val list = IntentCompat.getParcelableArrayListExtra(i, Intent.EXTRA_STREAM, Uri::class.java)
                if (!list.isNullOrEmpty()) vm.import(list)
            }
        }
    }
}

@Composable
fun HiloRoot(vm: HiloViewModel) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    Box(Modifier.fillMaxSize().background(c.bg)) {
        when (val sc = vm.screen) {
            Screen.Library -> LibraryScreen(vm)
            is Screen.Reader -> ReaderScreen(vm, sc.id)
            Screen.Guide -> GuideScreen(vm)
            is Screen.Study -> StudyScreen(vm, sc.bookId)
        }

        vm.busy?.let { msg ->
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.background(c.surface, RoundedCornerShape(20.dp)).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(36.dp), color = c.accent)
                    Text(msg, style = MaterialTheme.typography.bodyLarge, color = c.ink)
                }
            }
        }
    }

    when (vm.sheet) {
        "settings" -> SettingsSheet(vm) { vm.sheet = null }
        "timer" -> TimerSheet(vm) { vm.sheet = null }
    }

    vm.feynman?.let { r -> com.hilo.lector.ui.FeynmanDialog(vm, r) { vm.feynman = null } }

    if (vm.restored) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Copia restaurada") },
            text = { Text("Hilo se va a reiniciar para cargar tus datos.") },
            confirmButton = { TextButton(onClick = { com.hilo.lector.data.Backup.restart(ctx) }) { Text("Reiniciar") } },
        )
    }

    vm.pasteDraft?.let { draft ->
        PasteDialog(
            initial = draft,
            onDismiss = { vm.pasteDraft = null },
            onConfirm = { title, text -> vm.pasteDraft = null; vm.addText(title, text) },
        )
    }

    vm.timer.finished?.let { wasBreak ->
        val s = vm.settings.value
        val today = Stats.today(ctx)
        AlertDialog(
            onDismissRequest = { vm.timer.finished = null },
            title = { Text(if (wasBreak) "Se acabó el descanso" else "¡Lo lograste!") },
            text = {
                Text(
                    if (wasBreak) "¿Seguimos con otro bloque de lectura?"
                    else "${vm.timer.minutes} minutos de lectura. Hoy llevas ${today.sessions} " +
                        (if (today.sessions == 1) "sesión" else "sesiones") +
                        " y ${today.pages} páginas. Levántate, estírate y toma agua."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.timer.finished = null
                    if (wasBreak) vm.timer.start(s.timerMin) else vm.timer.start(5, breakTime = true)
                }) { Text(if (wasBreak) "Leer ${s.timerMin} min" else "Descanso de 5 min") }
            },
            dismissButton = {
                TextButton(onClick = { vm.timer.finished = null }) { Text(if (wasBreak) "Ahora no" else "Terminar por hoy") }
            },
        )
    }

    LaunchedEffect(vm.toast) {
        vm.toast?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_LONG).show()
            vm.toast = null
        }
    }
}
