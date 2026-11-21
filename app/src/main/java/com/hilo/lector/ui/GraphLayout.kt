package com.hilo.lector.ui

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Ordena un mapa ya armado para que se lea: ningún círculo ni nombre encima de otro
 * y todo dentro del recuadro, sin quedar pegado a los bordes.
 *
 * Cada nodo ocupa una caja: el círculo (radio [r]) y debajo su nombre (ancho [lw], alto [lh]).
 */
/** Devuelve la escala para ver todo de una vez (1 si ya cabe): úsala como zoom inicial. */
fun untangle(pos: Array<Offset>, r: FloatArray, lw: FloatArray, lh: Float, w: Float, h: Float, pad: Float, gap: Float = 6f): Float {
    val n = pos.size
    if (n == 0) return 1f
    fun half(i: Int) = max(r[i], lw[i] / 2f)
    // 1) Separar cajas encimadas (empujando por el eje donde se encima menos)
    repeat(260) { round ->
        var moved = false
        for (i in 0 until n) for (j in i + 1 until n) {
            val dx = pos[j].x - pos[i].x
            val ox = half(i) + half(j) + gap - abs(dx)
            if (ox <= 0) continue
            // caja vertical: desde y - r hasta y + r + lh
            val topI = pos[i].y - r[i]; val botI = pos[i].y + r[i] + lh
            val topJ = pos[j].y - r[j]; val botJ = pos[j].y + r[j] + lh
            val oy = min(botI, botJ) - max(topI, topJ) + gap
            if (oy <= 0) continue
            moved = true
            if (ox < oy) {
                val s = if (dx >= 0) 1f else -1f
                val push = ox / 2f * s
                pos[i] = Offset(pos[i].x - push, pos[i].y); pos[j] = Offset(pos[j].x + push, pos[j].y)
            } else {
                val dy = pos[j].y - pos[i].y
                val s = if (dy > 0 || (dy == 0f && j % 2 == 0)) 1f else -1f
                val push = oy / 2f * s
                pos[i] = Offset(pos[i].x, pos[i].y - push); pos[j] = Offset(pos[j].x, pos[j].y + push)
            }
        }
        if (!moved && round > 0) return@repeat
    }
    // 2) Centrar en el recuadro y calcular cuánto alejar para verlo entero
    var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
    for (i in 0 until n) {
        minX = min(minX, pos[i].x - half(i)); maxX = max(maxX, pos[i].x + half(i))
        minY = min(minY, pos[i].y - r[i]); maxY = max(maxY, pos[i].y + r[i] + lh)
    }
    val bw = max(1f, maxX - minX); val bh = max(1f, maxY - minY)
    val sc = min(min((w - 2 * pad) / bw, (h - 2 * pad) / bh), 1f)
    val cx = (minX + maxX) / 2f; val cy = (minY + maxY) / 2f
    for (i in 0 until n) pos[i] = Offset(w / 2f + (pos[i].x - cx), h / 2f + (pos[i].y - cy))
    return sc
}
