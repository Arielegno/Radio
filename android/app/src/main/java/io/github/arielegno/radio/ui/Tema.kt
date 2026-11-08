package io.github.arielegno.radio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Amarillo = Color(0xFFF2C230)
val Naranja = Color(0xFFFF8A5C)
val Rosa = Color(0xFFFF5F9E)
val Tinta = Color(0xFF1B1E23)

@Immutable
data class Extra(val tenue: Color, val tarjeta: Color, val borde: Color, val oscuro: Boolean)
val LocalExtra = staticCompositionLocalOf { Extra(Color(0xFFA3ABB5), Color(0xFF22262D), Color(0x1FFFFFFF), true) }

private val Oscuro = darkColorScheme(
    primary = Amarillo, onPrimary = Tinta, secondary = Naranja, onSecondary = Tinta, tertiary = Rosa,
    background = Color(0xFF14161A), onBackground = Color(0xFFE6E9ED),
    surface = Color(0xFF1B1E23), onSurface = Color(0xFFE6E9ED), surfaceVariant = Color(0xFF262A31), onSurfaceVariant = Color(0xFFA3ABB5),
    secondaryContainer = Color(0xFF3A3220), onSecondaryContainer = Amarillo, outline = Color(0xFF3A404A), error = Color(0xFFFF6B5E)
)
private val Claro = lightColorScheme(
    primary = Amarillo, onPrimary = Tinta, secondary = Naranja, onSecondary = Tinta, tertiary = Rosa,
    background = Color(0xFFF4F1EA), onBackground = Color(0xFF1C1D21),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1C1D21), surfaceVariant = Color(0xFFEDE9E0), onSurfaceVariant = Color(0xFF5B616C),
    secondaryContainer = Color(0xFFF7E7B0), onSecondaryContainer = Color(0xFF5C4600), outline = Color(0xFFD9D4C7), error = Color(0xFFC0392B)
)

private val Titulos = FontFamily.SansSerif

@Composable
fun TemaRadio(tema: String, contenido: @Composable () -> Unit) {
    val oscuro = when (tema) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val t = Typography()
    val tipo = t.copy(
        headlineLarge = TextStyle(fontFamily = Titulos, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 34.sp),
        headlineMedium = TextStyle(fontFamily = Titulos, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 29.sp),
        headlineSmall = TextStyle(fontFamily = Titulos, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 25.sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
    val extra = if (oscuro) Extra(Color(0xFFA3ABB5), Color(0xFF22262D), Color(0x1FFFFFFF), true) else Extra(Color(0xFF5B616C), Color(0xFFFFFFFF), Color(0x1F000000), false)
    MaterialTheme(colorScheme = if (oscuro) Oscuro else Claro, typography = tipo) {
        androidx.compose.runtime.CompositionLocalProvider(LocalExtra provides extra) { contenido() }
    }
}
