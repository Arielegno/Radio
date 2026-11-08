package io.github.arielegno.radio

import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Log
import io.github.arielegno.radio.core.Json

/**
 * Ecualizador de 5 bandas y nivelador de volumen (los mismos estilos que la versión web).
 * Solo para tu biblioteca. Apagados por defecto para escuchar el sonido original.
 */
object Efectos {
    val FRECUENCIAS = intArrayOf(60, 250, 1000, 4000, 12000)
    val ETIQUETAS = listOf("60", "250", "1k", "4k", "12k")
    val ESTILOS = linkedMapOf("flat" to "Plano", "bass" to "Más graves", "vocal" to "Voces claras", "rock" to "Rock", "acoustic" to "Acústico",
        "car" to "Carro", "lessbass" to "Menos graves", "custom" to "Personalizado")
    private val PRESETS = mapOf(
        "flat" to intArrayOf(0, 0, 0, 0, 0), "bass" to intArrayOf(6, 3, 0, 0, 0), "vocal" to intArrayOf(-2, 0, 3, 3, 0),
        "rock" to intArrayOf(4, 1, -1, 2, 4), "acoustic" to intArrayOf(2, 1, 0, 2, 3), "car" to intArrayOf(5, 2, 0, 2, 4), "lessbass" to intArrayOf(-4, -2, 0, 0, 0)
    )

    private var sesion = 0
    private var eq: Equalizer? = null
    private var dp: DynamicsProcessing? = null
    private var le: LoudnessEnhancer? = null

    private val p get() = RadioApp.prefs

    /** Ganancias (dB) de las 5 bandas según el estilo elegido. */
    fun ganancias(): IntArray {
        val est = p.str("eqPreset", "flat")
        if (est == "custom") {
            val g = Json.arr(p.str("eqCustom", "[0,0,0,0,0]")).map { (it as? Number)?.toInt() ?: 0 }
            return IntArray(5) { (g.getOrNull(it) ?: 0).coerceIn(-12, 12) }
        }
        return (PRESETS[est] ?: PRESETS.getValue("flat")).copyOf()
    }

    fun guardarPersonalizado(g: IntArray) { p.set("eqCustom", Json.write(g.toList())); p.set("eqPreset", "custom") }

    /** Se llama cuando cambia la sesión de audio del reproductor o un ajuste. */
    fun aplicar(nuevaSesion: Int = sesion) {
        if (nuevaSesion != sesion) { liberar(); sesion = nuevaSesion }
        if (sesion == 0) return
        try {
            if (p.bool("eqOn", false)) {
                val e = eq ?: Equalizer(0, sesion).also { eq = it }
                val g = ganancias()
                val (min, max) = e.bandLevelRange.let { it[0].toInt() to it[1].toInt() }
                for (b in 0 until e.numberOfBands) {
                    val centro = e.getCenterFreq(b.toShort()) / 1000 // mHz → Hz
                    val i = FRECUENCIAS.indices.minByOrNull { kotlin.math.abs(Math.log(FRECUENCIAS[it].toDouble()) - Math.log(centro.coerceAtLeast(1).toDouble())) } ?: 0
                    e.setBandLevel(b.toShort(), (g[i] * 100).coerceIn(min, max).toShort())
                }
                e.enabled = true
            } else { eq?.enabled = false }
        } catch (e: Exception) { Log.w("Radio", "Ecualizador no disponible", e); eq = null }
        try {
            val on = p.bool("levelOn", false)
            if (Build.VERSION.SDK_INT >= 28) {
                if (on && dp == null) dp = crearNivelador()
                dp?.enabled = on
            } else {
                if (on && le == null) le = LoudnessEnhancer(sesion).apply { setTargetGain(400) }
                le?.enabled = on
            }
        } catch (e: Exception) { Log.w("Radio", "Nivelador no disponible", e); dp = null; le = null }
    }

    /** Compresor suave + limitador: sube lo bajito y frena los picos. */
    private fun crearNivelador(): DynamicsProcessing? {
        if (Build.VERSION.SDK_INT < 28) return null
        val cfg = DynamicsProcessing.Config.Builder(DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, 2, false, 0, true, 1, false, 0, true).build()
        val d = DynamicsProcessing(0, sesion, cfg)
        for (ch in 0 until 2) {
            val mbc = d.getMbcBandByChannelIndex(ch, 0)
            mbc.attackTime = 20f; mbc.releaseTime = 300f; mbc.ratio = 3f; mbc.threshold = -26f; mbc.kneeWidth = 6f; mbc.postGain = 6f; mbc.isEnabled = true
            d.setMbcBandByChannelIndex(ch, 0, mbc)
            val lim = d.getLimiterByChannelIndex(ch)
            lim.attackTime = 1f; lim.releaseTime = 60f; lim.ratio = 10f; lim.threshold = -2f; lim.postGain = 0f; lim.isEnabled = true
            d.setLimiterByChannelIndex(ch, lim)
        }
        return d
    }

    fun liberar() {
        try { eq?.release() } catch (e: Exception) {}
        try { if (Build.VERSION.SDK_INT >= 28) dp?.release() } catch (e: Exception) {}
        try { le?.release() } catch (e: Exception) {}
        eq = null; dp = null; le = null
    }
}
