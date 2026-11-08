package io.github.arielegno.radio.core

import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/**
 * Copia de la música con audios: el mismo formato .rrlib de la versión web, así que
 * una copia hecha en la web se restaura en la app y al revés.
 *   "RRLIB1" + largo del encabezado (4 bytes, big-endian) + encabezado JSON + archivos uno tras otro.
 */
object CopiaMusica {
    const val MAGIA = "RRLIB1"
    const val MAX_PARTE = 900_000_000L

    class Archivo(val campo: String, val mime: String, val tam: Long, val abrir: () -> InputStream)
    class Elemento(val datos: Map<String, Any?>, val archivos: List<Archivo>) { val tam get() = archivos.sumOf { it.tam } }

    /** Reparte en partes de hasta MAX_PARTE bytes (una canción nunca se corta). */
    fun partir(items: List<Elemento>, max: Long = MAX_PARTE): List<List<Elemento>> {
        val partes = mutableListOf<MutableList<Elemento>>(); var tam = 0L
        for (it in items) {
            if (partes.isEmpty() || (tam + it.tam > max && partes.last().isNotEmpty())) { partes += mutableListOf<Elemento>(); tam = 0 }
            partes.last() += it; tam += it.tam
        }
        return partes
    }

    fun escribir(out: OutputStream, items: List<Elemento>, parte: Int, de: Int) {
        var idx = 0
        val lista = items.map { e ->
            val m = LinkedHashMap(e.datos)
            for (a in e.archivos) m[a.campo] = linkedMapOf("__b" to idx++, "t" to a.mime, "n" to a.tam)
            m
        }
        val h = Json.write(linkedMapOf("v" to 1, "part" to parte, "of" to de, "items" to lista)).toByteArray(Charsets.UTF_8)
        out.write(MAGIA.toByteArray(Charsets.US_ASCII))
        out.write(byteArrayOf((h.size ushr 24).toByte(), (h.size ushr 16).toByte(), (h.size ushr 8).toByte(), h.size.toByte()))
        out.write(h)
        val buf = ByteArray(64 * 1024)
        for (e in items) for (a in e.archivos) a.abrir().use { inp ->
            var falta = a.tam
            while (falta > 0) {
                val n = inp.read(buf, 0, minOf(buf.size.toLong(), falta).toInt())
                if (n < 0) throw EOFException("El archivo «${e.datos["name"]}» es más corto de lo esperado.")
                out.write(buf, 0, n); falta -= n
            }
        }
        out.flush()
    }

    class Ref(val campo: String, val mime: String, val tam: Long, val orden: Long)

    /**
     * Lee una copia. Por cada canción:
     *  - omitir(datos, refs) decide si ya está (se salta sin guardar);
     *  - guardar(datos, ref, flujo) guarda un archivo y devuelve su nombre;
     *  - listo(datos, guardados) registra la canción.
     * Devuelve (agregadas, omitidas).
     */
    fun leer(
        input: InputStream,
        omitir: (Map<String, Any?>, List<Ref>) -> Boolean,
        guardar: (Map<String, Any?>, Ref, InputStream) -> String,
        listo: (Map<String, Any?>, Map<String, String>) -> Unit
    ): Pair<Int, Int> {
        val din = DataInputStream(input)
        val magia = ByteArray(6)
        try { din.readFully(magia) } catch (e: EOFException) { throw IllegalArgumentException("No es una copia de Radio de ruta.") }
        if (String(magia, Charsets.US_ASCII) != MAGIA) throw IllegalArgumentException("No es una copia de Radio de ruta.")
        val largo = din.readInt()
        if (largo <= 0 || largo > 200_000_000) throw IllegalArgumentException("La copia está dañada.")
        val hb = ByteArray(largo); din.readFully(hb)
        val h = Json.obj(String(hb, Charsets.UTF_8))
        var agregadas = 0; var omitidas = 0
        for (x in h.list("items")) {
            @Suppress("UNCHECKED_CAST") val it = x.asObj() ?: continue
            val refs = it.entries.mapNotNull { (k, v) ->
                @Suppress("UNCHECKED_CAST") val m = v.asObj() ?: return@mapNotNull null
                if (!m.containsKey("__b")) null else Ref(k, m.str("t"), m.num("n"), m.num("__b"))
            }.sortedBy { it.orden }
            val datos = it.filterKeys { k -> refs.none { r -> r.campo == k } }
            if (omitir(datos, refs)) { for (r in refs) saltar(din, r.tam); omitidas++; continue }
            val guardados = linkedMapOf<String, String>()
            for (r in refs) {
                val lim = Limitado(din, r.tam)
                guardados[r.campo] = guardar(datos, r, lim)
                lim.agotar()
            }
            listo(datos, guardados); agregadas++
        }
        return agregadas to omitidas
    }

    private fun saltar(i: InputStream, n: Long) {
        var falta = n; val buf = ByteArray(64 * 1024)
        while (falta > 0) { val r = i.read(buf, 0, minOf(buf.size.toLong(), falta).toInt()); if (r < 0) throw EOFException("La copia está incompleta."); falta -= r }
    }

    /** Deja leer solo n bytes del flujo de la copia. */
    private class Limitado(val base: InputStream, var falta: Long) : InputStream() {
        override fun read(): Int { if (falta <= 0) return -1; val b = base.read(); if (b < 0) throw EOFException("La copia está incompleta."); falta--; return b }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (falta <= 0) return -1
            val n = base.read(b, off, minOf(len.toLong(), falta).toInt()); if (n < 0) throw EOFException("La copia está incompleta."); falta -= n; return n
        }
        override fun close() {}
        fun agotar() { saltar(base, falta); falta = 0 }
    }
}
