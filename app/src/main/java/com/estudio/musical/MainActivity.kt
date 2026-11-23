package com.estudio.musical

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.estudio.musical.audio.MotorAudio
import com.estudio.musical.data.Carpetas
import com.estudio.musical.ui.*
import java.io.File

enum class Pantalla(val titulo: String, val icono: ImageVector) {
    GRABAR("Grabar", Icons.Filled.Mic),
    EDITAR("Editar", Icons.Filled.GraphicEq),
    ESTUDIO("Estudio", Icons.Filled.LibraryMusic),
    INSTRUMENTOS("Tocar", Icons.Filled.Piano),
    LETRAS("Letras", Icons.Filled.EditNote),
    MAS("Más", Icons.Filled.Apps)
}

/** Estado compartido entre pantallas */
object AppState {
    var pantalla by mutableStateOf(Pantalla.GRABAR)
    /** Pista base elegida en Estudio para grabar encima */
    var baseOverdub by mutableStateOf<File?>(null)
    /** Grabación que se quiere abrir en el editor */
    var archivoAEditar by mutableStateOf<File?>(null)
}

// Atajos usados por varias pantallas
fun carpetaGrabaciones(ctx: Context): File = Carpetas.grabaciones(ctx)
fun listaGrabaciones(ctx: Context): List<File> = Carpetas.listaGrabaciones(ctx)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colores = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colores) {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            Pantalla.entries.forEach { p ->
                                NavigationBarItem(
                                    selected = AppState.pantalla == p,
                                    onClick = { AppState.pantalla = p },
                                    icon = { Icon(p.icono, contentDescription = p.titulo) },
                                    label = { Text(p.titulo, maxLines = 1) },
                                    alwaysShowLabel = false
                                )
                            }
                        }
                    }
                ) { padding ->
                    val m = Modifier.padding(padding)
                    when (AppState.pantalla) {
                        Pantalla.GRABAR -> RecordScreen(m)
                        Pantalla.EDITAR -> EditScreen(m)
                        Pantalla.ESTUDIO -> StudioScreen(m)
                        Pantalla.INSTRUMENTOS -> InstrumentsScreen(m)
                        Pantalla.LETRAS -> LyricsScreen(m)
                        Pantalla.MAS -> MoreScreen(m)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        MotorAudio.detenerTodo()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) MotorAudio.apagar()
    }
}
