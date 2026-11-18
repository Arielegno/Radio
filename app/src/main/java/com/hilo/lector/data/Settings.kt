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
    val paraSpacing: Float = 0.15f,
    val margin: Int = 18,
    val theme: Int = 0,            // 0 sistema, 1 claro, 2 oscuro (menús de la app)
    val colorMode: Int = 0,        // modo de color de la página (ver ReadModes)
    val lastDayMode: Int = 0,
    val lastNightMode: Int = 1,
    val pageMode: Int = 0,         // 0 desplazar hacia abajo, 1 pasar páginas en horizontal
    val twoPages: Boolean = false, // dos páginas de PDF con el celular en horizontal
    val tapTurn: Boolean = true,   // tocar los bordes para pasar de página
    val fullScreen: Boolean = false,
    val brightnessSwipe: Boolean = false,
    val cornerDayNight: Boolean = true,
    val cornerBookmark: Boolean = true,
    val openLast: Boolean = false,
    val wpm: Int = 260,
    val ttsRate: Float = 1f,
    val timerMin: Int = 15,
    val ocr: Boolean = true,
    val keepOn: Boolean = true,
    val weight: Int = 400,         // grosor de la letra: 300 fina … 700 negrita
    val align: Int = 1,            // 0 izquierda, 1 justificado con guiones, 2 justificado sin guiones
    val indent: Boolean = true,    // sangría al empezar cada párrafo, como en un libro
    val pdfCrop: Boolean = true,   // recortar los márgenes blancos del PDF
    val libraryView: Int = 0,      // 0 lista (como ReadEra), 1 cuadrícula de portadas
)

class SettingsStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<ReaderSettings> = _state
    val value: ReaderSettings get() = _state.value

    var lastBook: String?
        get() = sp.getString("lastBook", null)
        set(v) { sp.edit().putString("lastBook", v).apply() }

    fun update(f: (ReaderSettings) -> ReaderSettings) {
        val n = f(_state.value)
        _state.value = n
        save(n)
    }

    fun reset() = update { ReaderSettings(theme = it.theme, colorMode = it.colorMode, pageMode = it.pageMode, libraryView = it.libraryView) }

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
            colorMode = sp.getInt("colorMode", if (sp.getBoolean("darkPages", false)) 1 else d.colorMode),
            lastDayMode = sp.getInt("lastDayMode", d.lastDayMode),
            lastNightMode = sp.getInt("lastNightMode", d.lastNightMode),
            pageMode = sp.getInt("pageMode", d.pageMode),
            twoPages = sp.getBoolean("twoPages", d.twoPages),
            tapTurn = sp.getBoolean("tapTurn", d.tapTurn),
            fullScreen = sp.getBoolean("fullScreen", d.fullScreen),
            brightnessSwipe = sp.getBoolean("brightnessSwipe", d.brightnessSwipe),
            cornerDayNight = sp.getBoolean("cornerDayNight", d.cornerDayNight),
            cornerBookmark = sp.getBoolean("cornerBookmark", d.cornerBookmark),
            openLast = sp.getBoolean("openLast", d.openLast),
            wpm = sp.getInt("wpm", d.wpm),
            ttsRate = sp.getFloat("ttsRate", d.ttsRate),
            timerMin = sp.getInt("timerMin", d.timerMin),
            ocr = sp.getBoolean("ocr", d.ocr),
            keepOn = sp.getBoolean("keepOn", d.keepOn),
            weight = sp.getInt("weight", d.weight),
            align = sp.getInt("align", d.align),
            indent = sp.getBoolean("indent", d.indent),
            pdfCrop = sp.getBoolean("pdfCrop", d.pdfCrop),
            libraryView = sp.getInt("libraryView", d.libraryView),
        ).let { r ->
            // Versión 1.7: el texto pasa a verse como la página de un libro (justificado, sangría, sin huecos entre párrafos)
            if (!sp.getBoolean("formatoLibro", false)) {
                val m = r.copy(paraSpacing = minOf(r.paraSpacing, 0.15f), align = 1, indent = true)
                sp.edit().putBoolean("formatoLibro", true).putFloat("paraSpacing", m.paraSpacing).putInt("align", 1).putBoolean("indent", true).apply()
                m
            } else r
        }
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
            .putInt("colorMode", s.colorMode)
            .putInt("lastDayMode", s.lastDayMode)
            .putInt("lastNightMode", s.lastNightMode)
            .putInt("pageMode", s.pageMode)
            .putBoolean("twoPages", s.twoPages)
            .putBoolean("tapTurn", s.tapTurn)
            .putBoolean("fullScreen", s.fullScreen)
            .putBoolean("brightnessSwipe", s.brightnessSwipe)
            .putBoolean("cornerDayNight", s.cornerDayNight)
            .putBoolean("cornerBookmark", s.cornerBookmark)
            .putBoolean("openLast", s.openLast)
            .putInt("wpm", s.wpm)
            .putFloat("ttsRate", s.ttsRate)
            .putInt("timerMin", s.timerMin)
            .putBoolean("ocr", s.ocr)
            .putBoolean("keepOn", s.keepOn)
            .putInt("weight", s.weight)
            .putInt("align", s.align)
            .putBoolean("indent", s.indent)
            .putBoolean("pdfCrop", s.pdfCrop)
            .putInt("libraryView", s.libraryView)
            .apply()
    }
}
