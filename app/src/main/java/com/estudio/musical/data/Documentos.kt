package com.estudio.musical.data

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.estudio.musical.music.TextoMusical
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object Documentos {

    /** Crea un PDF de la letra con los acordes encima de cada verso */
    fun letraAPdf(ctx: Context, c: Cancion): File {
        val doc = PdfDocument()
        val ancho = 595; val alto = 842; val margen = 48f
        val titulo = Paint().apply { textSize = 22f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val sub = Paint().apply { textSize = 11f; color = Color.DKGRAY; isAntiAlias = true }
        val letra = Paint().apply { textSize = 12.5f; typeface = Typeface.MONOSPACE; isAntiAlias = true }
        val acorde = Paint(letra).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); color = Color.rgb(25, 90, 200) }
        val seccion = Paint(letra).apply { typeface = Typeface.DEFAULT_BOLD; textSize = 13f }

        var num = 1
        var pagina = doc.startPage(PdfDocument.PageInfo.Builder(ancho, alto, num).create())
        var y = margen + 10
        fun nuevaPaginaSiHaceFalta(alturaNecesaria: Float) {
            if (y + alturaNecesaria > alto - margen) {
                doc.finishPage(pagina); num++
                pagina = doc.startPage(PdfDocument.PageInfo.Builder(ancho, alto, num).create())
                y = margen + 10
            }
        }
        pagina.canvas.drawText(c.titulo.ifBlank { "Sin título" }, margen, y, titulo); y += 20
        val usados = TextoMusical.acordesUsados(c.letra)
        val datos = listOfNotNull(c.tonalidad.takeIf { it.isNotBlank() }?.let { "Tono: $it" },
            usados.takeIf { it.isNotEmpty() }?.let { "Acordes: " + it.joinToString("  ") })
        datos.forEach { pagina.canvas.drawText(it, margen, y, sub); y += 15 }
        y += 12

        for (linea in c.letra.lines()) {
            val t = linea.trim()
            if (t.startsWith("[") && t.endsWith("]") && TextoMusical.acordesUsados(t).isEmpty()) {
                nuevaPaginaSiHaceFalta(30f); y += 6
                pagina.canvas.drawText(t.removeSurrounding("[", "]"), margen, y, seccion); y += 18
                continue
            }
            val (ac, le) = TextoMusical.separarAcordes(linea)
            nuevaPaginaSiHaceFalta(if (ac.isNotBlank()) 34f else 18f)
            if (ac.isNotBlank()) { pagina.canvas.drawText(ac, margen, y, acorde); y += 15 }
            pagina.canvas.drawText(le, margen, y, letra); y += 18
        }
        doc.finishPage(pagina)
        val out = Carpetas.nombreLibre(Carpetas.pdf(ctx), c.titulo.ifBlank { "Letra" }, "pdf")
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
        return out
    }

    // ───────── Respaldo local en un archivo .zip (lo guardas donde quieras: memoria, USB, PC) ─────────

    private fun raices(ctx: Context): Map<String, File> = mapOf(
        "datos" to ctx.filesDir,
        "musica" to ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)!!
    )

    fun crearRespaldo(ctx: Context, destino: Uri) {
        ctx.contentResolver.openOutputStream(destino)!!.use { os ->
            ZipOutputStream(os.buffered()).use { zip ->
                for ((prefijo, raiz) in raices(ctx)) {
                    raiz.walkTopDown().filter { it.isFile }.forEach { f ->
                        zip.putNextEntry(ZipEntry("$prefijo/" + f.relativeTo(raiz).path))
                        f.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    /** Restaura un respaldo. Devuelve cuántos archivos se recuperaron. */
    fun restaurar(ctx: Context, origen: Uri): Int {
        val r = raices(ctx)
        var n = 0
        ctx.contentResolver.openInputStream(origen)!!.use { ins ->
            ZipInputStream(ins.buffered()).use { zip ->
                var e = zip.nextEntry
                while (e != null) {
                    val prefijo = e.name.substringBefore("/")
                    val raiz = r[prefijo]
                    if (raiz != null && !e.isDirectory) {
                        val dest = File(raiz, e.name.substringAfter("/"))
                        // seguridad: no escribir fuera de las carpetas de la app
                        if (dest.canonicalPath.startsWith(raiz.canonicalPath + File.separator)) {
                            dest.parentFile?.mkdirs()
                            dest.outputStream().use { zip.copyTo(it) }
                            n++
                        }
                    }
                    e = zip.nextEntry
                }
            }
        }
        return n
    }
}
