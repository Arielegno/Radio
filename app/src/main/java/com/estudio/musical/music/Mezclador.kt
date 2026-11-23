package com.estudio.musical.music

import com.estudio.musical.audio.dsp.Dynamics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Pista del multipista ya cargada en memoria */
class PistaAudio(
    val muestras: FloatArray,
    val volumen: Float = 1f,     // 0..1.5
    val paneo: Float = 0f,       // -1 izquierda .. 1 derecha
    val inicioMs: Int = 0,       // desplazamiento en la línea de tiempo
    val silencio: Boolean = false,
    val solo: Boolean = false
)

object Mezclador {
    /** Mezcla las pistas en estéreo con paneo de potencia constante. Devuelve (izquierda, derecha). */
    fun mezclar(pistas: List<PistaAudio>, sr: Int, volumenGeneral: Float = 0.9f): Pair<FloatArray, FloatArray> {
        val haySolo = pistas.any { it.solo }
        val activas = pistas.filter { !it.silencio && (!haySolo || it.solo) }
        val largo = pistas.maxOfOrNull { it.muestras.size + it.inicioMs * sr / 1000 } ?: 0
        val l = FloatArray(largo); val r = FloatArray(largo)
        for (p in activas) {
            val ang = (p.paneo.coerceIn(-1f, 1f) + 1) * PI / 4
            val gl = (cos(ang) * p.volumen).toFloat()
            val gr = (sin(ang) * p.volumen).toFloat()
            val off = p.inicioMs * sr / 1000
            for (i in p.muestras.indices) {
                val j = i + off
                if (j < 0 || j >= largo) continue
                l[j] += p.muestras[i] * gl; r[j] += p.muestras[i] * gr
            }
        }
        // ganancia segura según el número de pistas y limitador suave
        var peak = 0f
        for (i in 0 until largo) peak = maxOf(peak, kotlin.math.abs(l[i]), kotlin.math.abs(r[i]))
        val g = if (peak > 1f) volumenGeneral / peak else volumenGeneral
        for (i in 0 until largo) { l[i] = Dynamics.softClip(l[i] * g); r[i] = Dynamics.softClip(r[i] * g) }
        return l to r
    }
}
