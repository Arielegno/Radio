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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
            Screen.News -> com.hilo.lector.ui.NewsScreen(vm)
            Screen.Welcome -> com.hilo.lector.ui.WelcomeScreen(vm) {
                if (vm.settings.value.profile.isBlank()) vm.settings.update { it.copy(profile = "ninguno") }
                vm.screen = Screen.Library
            }
            is Screen.Study -> StudyScreen(vm, sc.bookId)
            is Screen.Classes -> com.hilo.lector.ui.ClassesScreen(vm, sc.id)
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

    // Bienvenida por perfil: solo la primera vez que se instala Hilo
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (vm.settings.value.profile.isBlank() && !java.io.File(ctx.filesDir, "library.json").exists() && vm.screen == Screen.Library) vm.screen = Screen.Welcome
    }
    // Si Hilo se cerró la última vez, ofrece enviar el informe
    var crash by remember { mutableStateOf(com.hilo.lector.ui.CrashLog.file(ctx).takeIf { it.exists() }?.readText()) }
    crash?.let { text ->
        AlertDialog(
            onDismissRequest = { crash = null },
            title = { Text("Hilo se cerró la última vez") },
            text = { Text("Perdón por eso. Si envías el informe (solo datos técnicos, sin tus libros), es más fácil arreglarlo.") },
            confirmButton = { TextButton(onClick = {
                com.hilo.lector.ui.shareText(ctx, "Informe de Hilo", text)
                com.hilo.lector.ui.CrashLog.file(ctx).delete(); crash = null
            }) { Text("Enviar informe") } },
            dismissButton = { TextButton(onClick = { com.hilo.lector.ui.CrashLog.file(ctx).delete(); crash = null }) { Text("Descartar") } },
        )
    }

    // Novedades: una vez después de actualizar (no en la primera instalación)
    var news by androidx.compose.runtime.remember {
        val sp = ctx.getSharedPreferences("novedades", android.content.Context.MODE_PRIVATE)
        val seen = sp.getInt("vista", 0)
        val upgrading = seen == 0 && java.io.File(ctx.filesDir, "library.json").exists()
        if (seen == 0 && !upgrading) sp.edit().putInt("vista", com.hilo.lector.data.Changelog.current.code).apply()
        androidx.compose.runtime.mutableStateOf(upgrading || (seen in 1 until com.hilo.lector.data.Changelog.current.code))
    }
    if (news && vm.screen == Screen.Library) {
        fun seen() {
            news = false
            ctx.getSharedPreferences("novedades", android.content.Context.MODE_PRIVATE).edit().putInt("vista", com.hilo.lector.data.Changelog.current.code).apply()
        }
        com.hilo.lector.ui.NewsDialog(onAll = { seen(); vm.screen = Screen.News }, onClose = { seen() })
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

    // Terminó un bloque empezado con «Solo 2 minutos»: lo más difícil ya pasó
    if (vm.timer.finished == false && vm.twoMin) {
        AlertDialog(
            onDismissRequest = { vm.timer.finished = null; vm.twoMin = false },
            title = { Text("¡Empezaste! 🎉") },
            text = { Text("Lo más difícil ya pasó. ¿Sigues 10 minutos más? Si no, también está bien: hoy leíste.") },
            confirmButton = { TextButton(onClick = { vm.timer.finished = null; vm.twoMin = false; vm.timer.start(10) }) { Text("Sigo 10 min") } },
            dismissButton = { TextButton(onClick = { vm.timer.finished = null; vm.twoMin = false }) { Text("Basta por ahora") } },
        )
    } else vm.timer.finished?.let { wasBreak ->
        val s = vm.settings.value
        val today = Stats.today(ctx)
        val friendMsg = if (!wasBreak) vm.friendDone else null
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
                androidx.compose.foundation.layout.Row {
                    if (friendMsg != null) TextButton(onClick = {
                        com.hilo.lector.ui.share(ctx, friendMsg); vm.friendDone = null
                    }) { Text("Avisar a mi amigo") }
                    TextButton(onClick = { vm.timer.finished = null }) { Text(if (wasBreak) "Ahora no" else "Terminar por hoy") }
                }
            },
        )
    }

    // Ayuda de cada apartado, celebración y sonidos de fondo
    vm.help?.let { com.hilo.lector.ui.HelpSheet(vm, it) }
    com.hilo.lector.ui.Celebration(vm)
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            when (e) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> com.hilo.lector.ui.NoisePlayer.stop()
                androidx.lifecycle.Lifecycle.Event.ON_START -> if (vm.noise.isNotEmpty()) com.hilo.lector.ui.NoisePlayer.play(vm.noise, vm.noiseVol)
                else -> {}
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    // Barra de «Deshacer» durante unos segundos
    vm.undo?.let { (text, revert) ->
        LaunchedEffect(vm.undo) { kotlinx.coroutines.delay(6000); vm.undo = null }
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp), contentAlignment = Alignment.BottomCenter) {
            androidx.compose.foundation.layout.Row(
                androidx.compose.ui.Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(androidx.compose.ui.graphics.Color(0xFF263238)).padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text, color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp, modifier = androidx.compose.ui.Modifier.weight(1f, fill = false))
                TextButton(onClick = { revert(); vm.undo = null }) { Text("DESHACER", color = androidx.compose.ui.graphics.Color(0xFFFFC94D), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
            }
        }
    }

    LaunchedEffect(vm.toast) {
        vm.toast?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_LONG).show()
            vm.toast = null
        }
    }
}
