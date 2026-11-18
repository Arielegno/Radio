package com.hilo.lector.text

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipFile

/** Cómics y mangas: CBZ (zip con imágenes) y CBR (rar con imágenes). */
object Comic {
    private val IMG = Regex("(?i)\\.(jpe?g|png|webp|gif|bmp)$")

    /** Orden natural: "2.jpg" va antes que "10.jpg". */
    private fun natural(a: String, b: String): Int {
        val ra = Regex("\\d+|\\D+").findAll(a.lowercase()).map { it.value }.toList()
        val rb = Regex("\\d+|\\D+").findAll(b.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(ra.size, rb.size)) {
            val x = ra[i]; val y = rb[i]
            val c = if (x[0].isDigit() && y[0].isDigit()) (x.toBigInteger().compareTo(y.toBigInteger())) else x.compareTo(y)
            if (c != 0) return c
        }
        return ra.size - rb.size
    }

    private fun isRar(f: File): Boolean = f.inputStream().use { s ->
        val h = ByteArray(4); s.read(h) == 4 && h[0] == 'R'.code.toByte() && h[1] == 'a'.code.toByte() && h[2] == 'r'.code.toByte() && h[3] == '!'.code.toByte()
    }

    /** Nombres de las páginas, en orden. */
    fun pages(f: File): List<String> =
        if (isRar(f)) com.github.junrar.Archive(f).use { a -> a.fileHeaders.filter { !it.isDirectory && IMG.containsMatchIn(it.fileName) }.map { it.fileName } }
            .filterNot { it.contains("__MACOSX") }.sortedWith(::natural)
        else ZipFile(f).use { z -> z.entries().toList().filter { !it.isDirectory && IMG.containsMatchIn(it.name) && !it.name.contains("__MACOSX") }.map { it.name } }
            .sortedWith(::natural)

    /** Bytes de una página. */
    fun page(f: File, name: String): ByteArray =
        if (isRar(f)) com.github.junrar.Archive(f).use { a ->
            val h = a.fileHeaders.first { it.fileName == name }
            ByteArrayOutputStream().also { a.extractFile(h, it) }.toByteArray()
        } else ZipFile(f).use { z -> z.getInputStream(z.getEntry(name)).use { it.readBytes() } }

    fun isRarFile(f: File) = isRar(f)

    /** Un CBR se descomprime una vez a una carpeta (leer un rar página por página es muy lento). */
    fun extractRar(f: File, dir: File, progress: (Int, Int) -> Unit = { _, _ -> }): List<File> {
        val names = pages(f)
        val done = dir.listFiles()?.filter { IMG.containsMatchIn(it.name) }?.sortedWith { a, b -> natural(a.name, b.name) }.orEmpty()
        if (done.size == names.size && names.isNotEmpty()) return done
        dir.deleteRecursively(); dir.mkdirs()
        val order = names.withIndex().associate { it.value to it.index }
        com.github.junrar.Archive(f).use { a ->
            var n = 0
            for (h in a.fileHeaders) {
                val i = order[h.fileName] ?: continue
                val ext = h.fileName.substringAfterLast('.', "jpg").lowercase()
                File(dir, "%05d.%s".format(i, ext)).outputStream().use { a.extractFile(h, it) }
                progress(++n, names.size)
            }
        }
        return dir.listFiles()?.filter { IMG.containsMatchIn(it.name) }?.sortedWith { a, b -> natural(a.name, b.name) }.orEmpty()
    }

    /** Portada: la primera página. */
    fun cover(f: File): ByteArray? = runCatching { pages(f).firstOrNull()?.let { page(f, it) } }.getOrNull()
}
