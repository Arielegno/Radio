package com.hilo.lector.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.hilo.lector.R

/** Colores de Hilo: neutros con un toque azul, acento verde azulado, y los colores de línea. */
@Immutable
data class HiloColors(
    val bg: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val accentInk: Color,
    val band: Color,
    val chip: Color,
    val dark: Boolean,
    /** rojo, verde, ámbar, azul, violeta */
    val lines: List<Color>,
) {
    fun palette(p: Int): List<Color> = when (p) {
        1 -> listOf(lines[3], lines[4])
        2 -> listOf(lines[0], lines[2])
        else -> listOf(lines[0], lines[1], lines[2], lines[3])
    }
}

val LightHilo = HiloColors(
    bg = Color(0xFFE9EDF3), surface = Color(0xFFFFFFFF), ink = Color(0xFF1B2030), muted = Color(0xFF5D6578),
    line = Color(0xFFD6DBE5), accent = Color(0xFF0F766E), accentInk = Color.White, band = Color(0xFFDCEFEB),
    chip = Color(0xFFDFE4EC), dark = false,
    lines = listOf(Color(0xFFC8372D), Color(0xFF1E8538), Color(0xFFAD6C00), Color(0xFF2457C5), Color(0xFF7A3DC2)),
)

val DarkHilo = HiloColors(
    bg = Color(0xFF0F1218), surface = Color(0xFF171B24), ink = Color(0xFFE5E8EF), muted = Color(0xFF9299AB),
    line = Color(0xFF2A303D), accent = Color(0xFF3EC4B5), accentInk = Color(0xFF06201D), band = Color(0xFF1A2C2D),
    chip = Color(0xFF232835), dark = true,
    lines = listOf(Color(0xFFFF7A6D), Color(0xFF5CD27B), Color(0xFFF3B642), Color(0xFF7DA8FF), Color(0xFFB992FF)),
)

val LocalHilo = staticCompositionLocalOf { LightHilo }

object Fonts {
    val inter = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_bold, FontWeight.Bold),
    )
    val lora = FontFamily(
        Font(R.font.lora_regular, FontWeight.Normal),
        Font(R.font.lora_bold, FontWeight.Bold),
    )
    val display = FontFamily(
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_bold, FontWeight.Bold),
    )

    fun reading(i: Int): FontFamily = when (i) {
        1 -> lora
        2 -> FontFamily.SansSerif
        else -> inter
    }

    val names = listOf("Inter", "Lora", "Sistema")
}

private fun TextStyle.ui() = copy(fontFamily = Fonts.inter)

@Composable
fun HiloTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val c = if (dark) DarkHilo else LightHilo
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.accentInk, primaryContainer = c.band, onPrimaryContainer = c.ink,
            secondary = c.accent, onSecondary = c.accentInk, secondaryContainer = c.band, onSecondaryContainer = c.ink,
            background = c.bg, onBackground = c.ink, surface = c.surface, onSurface = c.ink,
            surfaceVariant = c.chip, onSurfaceVariant = c.muted, outline = c.line, outlineVariant = c.line,
            surfaceContainerLowest = c.bg, surfaceContainerLow = c.surface, surfaceContainer = c.surface,
            surfaceContainerHigh = c.surface, surfaceContainerHighest = c.chip, error = c.lines[0],
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.accentInk, primaryContainer = c.band, onPrimaryContainer = c.ink,
            secondary = c.accent, onSecondary = c.accentInk, secondaryContainer = c.band, onSecondaryContainer = c.ink,
            background = c.bg, onBackground = c.ink, surface = c.surface, onSurface = c.ink,
            surfaceVariant = c.chip, onSurfaceVariant = c.muted, outline = c.line, outlineVariant = c.line,
            surfaceContainerLowest = c.surface, surfaceContainerLow = c.surface, surfaceContainer = c.surface,
            surfaceContainerHigh = c.surface, surfaceContainerHighest = c.chip, error = c.lines[0],
        )
    }
    val t = Typography()
    val typo = Typography(
        displayLarge = t.displayLarge.copy(fontFamily = Fonts.display),
        displayMedium = t.displayMedium.copy(fontFamily = Fonts.display),
        displaySmall = t.displaySmall.copy(fontFamily = Fonts.display),
        headlineLarge = t.headlineLarge.copy(fontFamily = Fonts.display, fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontFamily = Fonts.display, fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontFamily = Fonts.display, fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontFamily = Fonts.display, fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.ui().copy(fontWeight = FontWeight.Medium),
        titleSmall = t.titleSmall.ui(),
        bodyLarge = t.bodyLarge.ui(),
        bodyMedium = t.bodyMedium.ui(),
        bodySmall = t.bodySmall.ui(),
        labelLarge = t.labelLarge.ui(),
        labelMedium = t.labelMedium.ui(),
        labelSmall = t.labelSmall.ui(),
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val ctl = WindowCompat.getInsetsController(window, view)
            ctl.isAppearanceLightStatusBars = !dark
            ctl.isAppearanceLightNavigationBars = !dark
            window.decorView.setBackgroundColor(c.bg.toArgb())
        }
    }

    CompositionLocalProvider(LocalHilo provides c) {
        MaterialTheme(colorScheme = scheme, typography = typo, content = content)
    }
}

/** Modo de color de la página, como en un lector de libros. */
@Immutable
data class ReadMode(val name: String, val paper: Color, val ink: Color, val dark: Boolean) {
    /** Colores de línea que se leen bien sobre este papel. */
    fun palette(p: Int): List<Color> = (if (dark) DarkHilo else LightHilo).palette(p)
    val band: Color get() = if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFF0F766E).copy(alpha = 0.12f)
    val muted: Color get() = ink.copy(alpha = 0.6f)
}

val READ_MODES = listOf(
    ReadMode("Día", Color.White, Color(0xFF1B2030), false),
    ReadMode("Noche", Color(0xFF15181D), Color(0xFFC9CED6), true),
    ReadMode("Noche contraste", Color.Black, Color.White, true),
    ReadMode("Sepia", Color(0xFFF4ECD8), Color(0xFF5B4636), false),
    ReadMode("Sepia contraste", Color(0xFFF4ECD8), Color(0xFF241608), false),
    ReadMode("Crepúsculo", Color(0xFF3A3F47), Color(0xFFD6D9DE), true),
    ReadMode("Consola", Color.Black, Color(0xFF39FF7A), true),
)

fun readMode(i: Int): ReadMode = READ_MODES.getOrElse(i) { READ_MODES[0] }
