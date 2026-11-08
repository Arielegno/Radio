package io.github.arielegno.radio

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import io.github.arielegno.radio.ui.App
import io.github.arielegno.radio.ui.TemaRadio

class MainActivity : ComponentActivity() {
    private var controlador: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var tema by remember { mutableStateOf(RadioApp.prefs.str("tema", "system")) }
            TemaRadio(tema) {
                App(tema) { t -> tema = t; RadioApp.prefs.set("tema", t) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Conecta con el servicio: así la música sigue sonando con la pantalla apagada y aparece en la notificación.
        if (controlador == null) controlador = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()
    }

    override fun onStop() {
        controlador?.let { MediaController.releaseFuture(it) }
        controlador = null
        super.onStop()
    }
}
