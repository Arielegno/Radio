package io.github.arielegno.radio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

/** Color de acento (Ajustes › Apariencia). Se llama «Amarillo» por el color de siempre. */
var Amarillo by mutableStateOf(Color(0xFFF2C230))
val Naranja = Color(0xFFFF8A5C)
val Rosa = Color(0xFFFF5F9E)
val Tinta = Color(0xFF1B1E23)

/** Tamaño de letra elegido (15–24, 17 es el normal, igual que la web). */
var TamanoLetra by mutableStateOf(17)

val ACENTOS_COLOR = linkedMapOf("#f2c230" to "Amarillo", "#ff8a3d" to "Naranja", "#4cd18a" to "Verde", "#5bc0ff" to "Celeste", "#ff7eb6" to "Rosa")

fun aplicarApariencia(acento: String, letra: Int) {
    Amarillo = try { Color(android.graphics.Color.parseColor(acento.takeIf { it in ACENTOS_COLOR } ?: "#f2c230")) } catch (e: Exception) { Color(0xFFF2C230) }
    TamanoLetra = letra.coerceIn(15, 24)
}

@Immutable
data class Extra(val tenue: Color, val tarjeta: Color, val borde: Color, val oscuro: Boolean)
val LocalExtra = staticCompositionLocalOf { Extra(Color(0xFFA3ABB5), Color(0xFF22262D), Color(0x1FFFFFFF), true) }

private val TextoOscuro = Color(0xFFE6E9ED)
private val TextoClaro = Color(0xFF1C1D21)
private val Titulos = FontFamily.SansSerif

@Composable
fun TemaRadio(tema: String, contenido: @Composable () -> Unit) {
    val oscuro = when (tema) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val acento = Amarillo
    val colores = if (oscuro) darkColorScheme(
        primary = acento, onPrimary = Tinta, secondary = Naranja, onSecondary = Tinta, tertiary = Rosa,
        background = Color(0xFF14161A), onBackground = TextoOscuro,
        surface = Color(0xFF1B1E23), onSurface = TextoOscuro, surfaceVariant = Color(0xFF262A31), onSurfaceVariant = Color(0xFFA3ABB5),
        secondaryContainer = lerp(Color(0xFF22262D), acento, 0.22f), onSecondaryContainer = acento, outline = Color(0xFF3A404A), error = Color(0xFFFF6B5E)
    ) else lightColorScheme(
        primary = acento, onPrimary = Tinta, secondary = Naranja, onSecondary = Tinta, tertiary = Rosa,
        background = Color(0xFFF4F1EA), onBackground = TextoClaro,
        surface = Color(0xFFFFFFFF), onSurface = TextoClaro, surfaceVariant = Color(0xFFEDE9E0), onSurfaceVariant = Color(0xFF5B616C),
        secondaryContainer = lerp(Color(0xFFFFFFFF), acento, 0.35f), onSecondaryContainer = lerp(acento, Color.Black, 0.55f), outline = Color(0xFFD9D4C7), error = Color(0xFFC0392B)
    )
    val t = Typography()
    val tipo = t.copy(
        headlineLarge = TextStyle(fontFamily = Titulos, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 34.sp),
        headlineMedium = TextStyle(fontFamily = Titulos, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 29.sp),
        headlineSmall = TextStyle(fontFamily = Titulos, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 25.sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
    val extra = if (oscuro) Extra(Color(0xFFA3ABB5), Color(0xFF22262D), Color(0x1FFFFFFF), true) else Extra(Color(0xFF5B616C), Color(0xFFFFFFFF), Color(0x1F000000), false)
    val d = LocalDensity.current
    MaterialTheme(colorScheme = colores, typography = tipo) {
        CompositionLocalProvider(
            LocalExtra provides extra,
            // Texto claro sobre fondo oscuro (y al revés) en toda la app, también fuera de las tarjetas
            LocalContentColor provides (if (oscuro) TextoOscuro else TextoClaro),
            LocalDensity provides Density(d.density, d.fontScale * TamanoLetra / 17f)
        ) { contenido() }
    }
}
