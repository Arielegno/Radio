package com.hilo.lector.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Ajustes de lectura. El interlineado por defecto es compacto (1.35)
 * para que el ojo no pierda el hilo entre una línea y la siguiente.
 */
data class ReaderSettings(
    val colors: Boolean = true,
    val palette: Int = 0,          // 0 arcoíris, 1 frío, 2 cálido
    val intensity: Float = 0.85f,
    val bionic: Boolean = false,
    val ruler: Boolean = false,
    val font: Int = 0,             // 0 Inter, 1 Lora, 2 letra del sistema
    val fontSize: Float = 18f,
    val lineHeight: Float = 1.35f,
    val paraSpacing: Float = 0.5f,
    val margin: Int = 18,
    val theme: Int = 0,            // 0 sistema, 1 claro, 2 oscuro
    val darkPages: Boolean = false,
    val wpm: Int = 260,
    val timerMin: Int = 15,
    val ocr: Boolean = true,
    val keepOn: Boolean = true,
)

class SettingsStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<ReaderSettings> = _state
    val value: ReaderSettings get() = _state.value

    fun update(f: (ReaderSettings) -> ReaderSettings) {
        val n = f(_state.value)
        _state.value = n
        save(n)
    }

    fun reset() = update { ReaderSettings(theme = it.theme) }

    private fun load(): ReaderSettings {
        val d = ReaderSettings()
        return ReaderSettings(
            colors = sp.getBoolean("colors", d.colors),
            palette = sp.getInt("palette", d.palette),
            intensity = sp.getFloat("intensity", d.intensity),
            bionic = sp.getBoolean("bionic", d.bionic),
            ruler = sp.getBoolean("ruler", d.ruler),
            font = sp.getInt("font", d.font),
            fontSize = sp.getFloat("fontSize", d.fontSize),
            lineHeight = sp.getFloat("lineHeight", d.lineHeight),
            paraSpacing = sp.getFloat("paraSpacing", d.paraSpacing),
            margin = sp.getInt("margin", d.margin),
            theme = sp.getInt("theme", d.theme),
            darkPages = sp.getBoolean("darkPages", d.darkPages),
            wpm = sp.getInt("wpm", d.wpm),
            timerMin = sp.getInt("timerMin", d.timerMin),
            ocr = sp.getBoolean("ocr", d.ocr),
            keepOn = sp.getBoolean("keepOn", d.keepOn),
        )
    }

    private fun save(s: ReaderSettings) {
        sp.edit()
            .putBoolean("colors", s.colors)
            .putInt("palette", s.palette)
            .putFloat("intensity", s.intensity)
            .putBoolean("bionic", s.bionic)
            .putBoolean("ruler", s.ruler)
            .putInt("font", s.font)
            .putFloat("fontSize", s.fontSize)
            .putFloat("lineHeight", s.lineHeight)
            .putFloat("paraSpacing", s.paraSpacing)
            .putInt("margin", s.margin)
            .putInt("theme", s.theme)
            .putBoolean("darkPages", s.darkPages)
            .putInt("wpm", s.wpm)
            .putInt("timerMin", s.timerMin)
            .putBoolean("ocr", s.ocr)
            .putBoolean("keepOn", s.keepOn)
            .apply()
    }
}
