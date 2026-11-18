package com.hilo.lector.text

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset

/**
 * Lector de libros MOBI / AZW / AZW3 sin DRM (formato de Kindle).
 * Descomprime el texto (PalmDOC o HUFF/CDIC) y devuelve el HTML del libro, el título, el autor y la portada.
 */
object Mobi {
    class Book(val title: String, val author: String, val html: String, val cover: ByteArray?)

    class DrmException : Exception("Este libro tiene DRM (protección de Amazon) y no se puede abrir.")

    private fun u16(b: ByteArray, o: Int) = ((b[o].toInt() and 0xFF) shl 8) or (b[o + 1].toInt() and 0xFF)
    private fun u32(b: ByteArray, o: Int): Long =
        ((b[o].toLong() and 0xFF) shl 24) or ((b[o + 1].toLong() and 0xFF) shl 16) or
            ((b[o + 2].toLong() and 0xFF) shl 8) or (b[o + 3].toLong() and 0xFF)

    private class Pdb(val data: ByteArray) {
        val count = u16(data, 76)
        val offsets = IntArray(count) { u32(data, 78 + it * 8).toInt() }
        fun record(i: Int): ByteArray {
            val a = offsets[i]
            val b = if (i + 1 < count) offsets[i + 1] else data.size
            return data.copyOfRange(a, maxOf(a, minOf(b, data.size)))
        }
    }

    fun isMobi(f: File): Boolean = try {
        f.inputStream().use { s ->
            val h = ByteArray(68)
            s.read(h) == 68 && String(h, 60, 8, Charsets.ISO_8859_1).let { it == "BOOKMOBI" || it == "TEXtREAd" }
        }
    } catch (_: Exception) { false }

    fun read(f: File, withText: Boolean = true): Book {
        val pdb = Pdb(f.readBytes())
        val r0 = pdb.record(0)
        val compression = u16(r0, 0)
        val textRecords = u16(r0, 8)
        val encryption = u16(r0, 12)
        if (encryption != 0) throw DrmException()
        val hasMobi = r0.size > 20 && String(r0, 16, 4, Charsets.ISO_8859_1) == "MOBI"
        var title = String(pdb.data, 0, 32, Charsets.ISO_8859_1).trimEnd('\u0000').replace('_', ' ')
        var author = ""
        var cover: ByteArray? = null
        var charset: Charset = Charset.forName("windows-1252")
        var extraFlags = 0
        var huffRecord = -1
        var huffCount = 0
        if (hasMobi) {
            val headerLen = u32(r0, 20).toInt()
            if (u32(r0, 28) == 65001L) charset = Charsets.UTF_8
            val nameOff = u32(r0, 0x54).toInt()
            val nameLen = u32(r0, 0x58).toInt()
            if (nameOff > 0 && nameOff + nameLen <= r0.size && nameLen in 1..500) title = String(r0, nameOff, nameLen, charset)
            val firstImage = if (r0.size >= 0x70) u32(r0, 0x6C).toInt() else -1
            if (headerLen >= 0x78 && r0.size >= 0x78) { huffRecord = u32(r0, 0x70).toInt(); huffCount = u32(r0, 0x74).toInt() }
            if (headerLen >= 0xE4 && r0.size >= 0xF4) extraFlags = u16(r0, 0xF2)
            val exthFlag = if (r0.size >= 0x84) u32(r0, 0x80) else 0L
            val ex = 16 + headerLen
            if (exthFlag and 0x40L != 0L && ex + 12 <= r0.size && String(r0, ex, 4, Charsets.ISO_8859_1) == "EXTH") {
                val n = u32(r0, ex + 8).toInt()
                var o = ex + 12
                var coverOff = -1
                repeat(n) {
                    if (o + 8 > r0.size) return@repeat
                    val type = u32(r0, o).toInt()
                    val len = u32(r0, o + 4).toInt()
                    if (len < 8 || o + len > r0.size) return@repeat
                    val body = r0.copyOfRange(o + 8, o + len)
                    when (type) {
                        100 -> author = if (author.isEmpty()) String(body, charset) else "$author; ${String(body, charset)}"
                        503 -> title = String(body, charset)
                        201 -> if (body.size >= 4) coverOff = u32(body, 0).toInt()
                    }
                    o += len
                }
                if (coverOff >= 0 && firstImage > 0 && firstImage + coverOff < pdb.count) {
                    cover = pdb.record(firstImage + coverOff).takeIf { it.size > 100 }
                }
            }
        }
        if (!withText) return Book(title.trim(), author.trim(), "", cover)

        val huff = if (compression == 17480) Huff(pdb, huffRecord, huffCount) else null
        val out = ByteArrayOutputStream()
        for (i in 1..minOf(textRecords, pdb.count - 1)) {
            var rec = pdb.record(i)
            rec = trimTrailing(rec, extraFlags)
            when (compression) {
                1 -> out.write(rec)
                2 -> out.write(palmDoc(rec))
                17480 -> out.write(huff!!.unpack(rec))
                else -> error("Compresión MOBI desconocida")
            }
        }
        return Book(title.trim(), author.trim(), String(out.toByteArray(), charset), cover)
    }

    /** Quita los datos de relleno que Kindle agrega al final de cada bloque de texto. */
    private fun trimTrailing(rec: ByteArray, flags: Int): ByteArray {
        var size = rec.size
        var f = flags shr 1
        while (f != 0) {
            if (f and 1 != 0 && size > 0) {
                var num = 0
                for (k in maxOf(0, size - 4) until size) {
                    val v = rec[k].toInt() and 0xFF
                    if (v and 0x80 != 0) num = 0
                    num = (num shl 7) or (v and 0x7F)
                }
                size = maxOf(0, size - num)
            }
            f = f shr 1
        }
        if (flags and 1 != 0 && size > 0) size = maxOf(0, size - ((rec[size - 1].toInt() and 0x3) + 1))
        return if (size == rec.size) rec else rec.copyOf(size)
    }

    /** Compresión PalmDOC (LZ77 sencillo). */
    fun palmDoc(d: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(d.size * 2)
        val buf = ArrayList<Byte>(0)
        var o = ByteArray(8192)
        var n = 0
        fun put(b: Int) { if (n == o.size) o = o.copyOf(o.size * 2); o[n++] = b.toByte() }
        var i = 0
        while (i < d.size) {
            val c = d[i++].toInt() and 0xFF
            when {
                c in 1..8 -> repeat(c) { if (i < d.size) put(d[i++].toInt()) }
                c < 0x80 -> put(c)
                c >= 0xC0 -> { put(' '.code); put(c xor 0x80) }
                else -> {
                    if (i >= d.size) break
                    val m = ((c shl 8) or (d[i++].toInt() and 0xFF)) and 0x3FFF
                    val dist = m shr 3
                    val len = (m and 7) + 3
                    if (dist in 1..n) repeat(len) { put(o[n - dist].toInt()) }
                }
            }
        }
        buf.clear()
        out.write(o, 0, n)
        return out.toByteArray()
    }

    /** Compresión HUFF/CDIC de los libros hechos por Amazon. */
    private class Huff(pdb: Pdb, first: Int, count: Int) {
        private val dict1 = LongArray(256 * 3)
        private val minCode = LongArray(33)
        private val maxCode = LongArray(33)
        private val dict = ArrayList<ByteArray?>()
        private val dictFlag = ArrayList<Boolean>()

        init {
            require(first > 0 && count > 1) { "MOBI sin tabla de compresión" }
            val h = pdb.record(first)
            require(String(h, 0, 4, Charsets.ISO_8859_1) == "HUFF") { "MOBI con compresión dañada" }
            val off1 = u32(h, 8).toInt()
            val off2 = u32(h, 12).toInt()
            for (i in 0 until 256) {
                val v = u32(h, off1 + i * 4)
                val codelen = (v and 0x1F)
                val term = (v and 0x80) != 0L
                val mc = v ushr 8
                dict1[i * 3] = codelen
                dict1[i * 3 + 1] = if (term) 1 else 0
                dict1[i * 3 + 2] = ((mc + 1) shl (32 - codelen.toInt())) - 1
            }
            minCode[0] = 0; maxCode[0] = (1L shl 32) - 1
            for (cl in 1..32) {
                val mn = u32(h, off2 + (cl - 1) * 8)
                val mx = u32(h, off2 + (cl - 1) * 8 + 4)
                minCode[cl] = mn shl (32 - cl)
                maxCode[cl] = ((mx + 1) shl (32 - cl)) - 1
            }
            for (k in 1 until count) {
                val c = pdb.record(first + k)
                if (String(c, 0, 4, Charsets.ISO_8859_1) != "CDIC") continue
                val phrases = u32(c, 8).toInt()
                val bits = u32(c, 12).toInt()
                val n = minOf(1 shl bits, phrases - dict.size)
                for (j in 0 until n) {
                    val off = u16(c, 16 + j * 2)
                    val blen = u16(c, 16 + off)
                    val len = blen and 0x7FFF
                    dict.add(c.copyOfRange(18 + off, minOf(c.size, 18 + off + len)))
                    dictFlag.add(blen and 0x8000 != 0)
                }
            }
        }

        fun unpack(src: ByteArray, depth: Int = 0): ByteArray {
            val data = src.copyOf(src.size + 8)
            var bitsLeft = src.size.toLong() * 8
            var pos = 0
            fun q(p: Int): Long {
                var x = 0L
                for (k in 0 until 8) x = (x shl 8) or (data[p + k].toLong() and 0xFF)
                return x
            }
            var x = q(pos)
            var n = 32
            val out = ByteArrayOutputStream(src.size * 3)
            while (true) {
                if (n <= 0) { pos += 4; x = q(pos); n += 32 }
                val code = (x ushr n) and 0xFFFFFFFFL
                val top = (code ushr 24).toInt()
                var codelen = dict1[top * 3].toInt()
                val term = dict1[top * 3 + 1] == 1L
                var mc = dict1[top * 3 + 2]
                if (!term) {
                    while (codelen < 32 && code < minCode[codelen]) codelen++
                    mc = maxCode[codelen]
                }
                n -= codelen
                bitsLeft -= codelen
                if (bitsLeft < 0 || codelen == 0) break
                val r = ((mc - code) ushr (32 - codelen)).toInt()
                if (r < 0 || r >= dict.size) break
                var slice = dict[r] ?: break
                if (!dictFlag[r]) {
                    if (depth > 30) break
                    dict[r] = null
                    slice = unpack(slice, depth + 1)
                    dict[r] = slice
                    dictFlag[r] = true
                }
                out.write(slice)
            }
            return out.toByteArray()
        }
    }
}
