package com.hilo.lector.ui

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Ordena un mapa ya armado para que se lea: los círculos y sus nombres no se enciman
 * y todo queda dentro del recuadro [w]×[h] (con margen [pad]).
 *
 * Cada nodo ocupa una caja: el círculo (radio [r]) y debajo su nombre (ancho [lw], alto [lh]).
 * Primero estira el dibujo para que use todo el recuadro; después separa las cajas que se tocan,
 * sin dejar que ninguna se salga (si no entra todo, quedan lo más separadas posible).
 */
fun untangle(pos: Array<Offset>, r: FloatArray, lw: FloatArray, lh: Float, w: Float, h: Float, pad: Float, gap: Float = 6f): Float {
    val n = pos.size
    if (n == 0) return 1f
    fun half(i: Int) = max(r[i], lw[i] / 2f)
    fun clampX(i: Int, x: Float) = x.coerceIn(pad + half(i), max(pad + half(i), w - pad - half(i)))
    fun clampY(i: Int, y: Float) = y.coerceIn(pad + r[i], max(pad + r[i], h - pad - r[i] - lh))

    // 1) Usar todo el recuadro: llevar el dibujo de la simulación al espacio disponible
    var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
    for (p in pos) { minX = min(minX, p.x); maxX = max(maxX, p.x); minY = min(minY, p.y); maxY = max(maxY, p.y) }
    val mx = pad + 50f; val my = pad + 30f
    val sx = (w - 2 * mx) / max(1f, maxX - minX)
    val sy = (h - 2 * my - lh) / max(1f, maxY - minY)
    for (i in 0 until n) pos[i] = Offset(clampX(i, mx + (pos[i].x - minX) * sx), clampY(i, my + (pos[i].y - minY) * sy))

    // 2) Separar cajas que se tocan, empujando por el eje donde se tocan menos
    repeat(400) {
        var moved = false
        for (i in 0 until n) for (j in i + 1 until n) {
            val dx = pos[j].x - pos[i].x
            val ox = half(i) + half(j) + gap - abs(dx)
            if (ox <= 0) continue
            val topI = pos[i].y - r[i]; val botI = pos[i].y + r[i] + lh
            val topJ = pos[j].y - r[j]; val botJ = pos[j].y + r[j] + lh
            val oy = min(botI, botJ) - max(topI, topJ) + gap
            if (oy <= 0) continue
            moved = true
            if (ox < oy) {
                val s = if (dx > 0 || (dx == 0f && (i + j) % 2 == 0)) 1f else -1f
                val push = ox / 2f * s
                pos[i] = Offset(clampX(i, pos[i].x - push), pos[i].y)
                pos[j] = Offset(clampX(j, pos[j].x + push), pos[j].y)
            } else {
                val dy = pos[j].y - pos[i].y
                val s = if (dy > 0 || (dy == 0f && (i + j) % 2 == 0)) 1f else -1f
                val push = oy / 2f * s
                pos[i] = Offset(pos[i].x, clampY(i, pos[i].y - push))
                pos[j] = Offset(pos[j].x, clampY(j, pos[j].y + push))
            }
        }
        if (!moved) return 1f
    }
    return 1f
}
