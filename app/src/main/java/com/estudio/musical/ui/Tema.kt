package com.estudio.musical.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colores de marca de Tonalia (los mismos del ícono) */
object Marca {
    val Violeta = Color(0xFF8B5CF6)
    val VioletaOscuro = Color(0xFF6D28D9)
    val Magenta = Color(0xFFC026D3)
    val Coral = Color(0xFFFF5A6E)
    val Ambar = Color(0xFFFFB547)
    val Verde = Color(0xFF34D399)
    val Fondo = Color(0xFF0F0B1E)
    val Superficie = Color(0xFF19142E)
    val Superficie2 = Color(0xFF241D3F)
    val Superficie3 = Color(0xFF2F2752)

    val degradado = Brush.linearGradient(listOf(VioletaOscuro, Magenta, Coral))
    val degradadoSuave = Brush.linearGradient(listOf(Color(0xFF2A1F55), Color(0xFF3B1F4F)))
    val degradadoGrabar = Brush.linearGradient(listOf(Coral, Color(0xFFE0336B)))
}

private val colores = darkColorScheme(
    primary = Color(0xFFB794FF),
    onPrimary = Color(0xFF1D0B45),
    primaryContainer = Color(0xFF4C2A9E),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFF472D0),
    onSecondary = Color(0xFF3D0A33),
    secondaryContainer = Color(0xFF5B1E58),
    onSecondaryContainer = Color(0xFFFFD7F3),
    tertiary = Marca.Ambar,
    onTertiary = Color(0xFF3A2500),
    error = Marca.Coral,
    errorContainer = Color(0xFF6B1A2A),
    onErrorContainer = Color(0xFFFFDADF),
    background = Marca.Fondo,
    onBackground = Color(0xFFECE6FF),
    surface = Marca.Fondo,
    onSurface = Color(0xFFECE6FF),
    surfaceVariant = Marca.Superficie2,
    onSurfaceVariant = Color(0xFFBDB3DC),
    surfaceContainerLowest = Marca.Fondo,
    surfaceContainerLow = Marca.Superficie,
    surfaceContainer = Marca.Superficie,
    surfaceContainerHigh = Marca.Superficie2,
    surfaceContainerHighest = Marca.Superficie3,
    outline = Color(0xFF6E6491),
    outlineVariant = Color(0xFF3A3258),
)

private val tipografia = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp),
    )
}

private val formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun TemaTonalia(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colores, typography = tipografia, shapes = formas, content = contenido)
}

val EstiloNumeroGrande = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
