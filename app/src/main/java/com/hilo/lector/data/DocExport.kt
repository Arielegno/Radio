package com.hilo.lector.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Crea documentos de Word (.docx) sencillos y archivos BibTeX para exportar ensayos, citas y referencias. */
object DocExport {
    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun para(text: String, bold: Boolean = false, size: Int = 24): String {
        val runs = text.split("\n").joinToString("<w:br/>") { "<w:t xml:space=\"preserve\">${esc(it)}</w:t>" }
        return "<w:p><w:r><w:rPr>${if (bold) "<w:b/>" else ""}<w:sz w:val=\"$size\"/></w:rPr>$runs</w:r></w:p>"
    }

    /** Escribe un .docx con un título y párrafos. */
    fun docx(out: File, title: String, paragraphs: List<String>) {
        val body = para(title, bold = true, size = 32) + paragraphs.joinToString("") { para(it) }
        val doc = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$body<w:sectPr><w:pgSz w:w="12240" w:h="15840"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr></w:body></w:document>"""
        val types = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>"""
        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""
        ZipOutputStream(out.outputStream()).use { z ->
            fun put(name: String, s: String) { z.putNextEntry(ZipEntry(name)); z.write(s.toByteArray()); z.closeEntry() }
            put("[Content_Types].xml", types)
            put("_rels/.rels", rels)
            put("word/document.xml", doc)
        }
    }

    private fun file(ctx: Context, name: String, ext: String): File {
        val dir = File(ctx.cacheDir, "export").apply { mkdirs() }
        return File(dir, name.replace(Regex("[^\\p{L}\\p{N}_ -]+"), "_").trim().take(60).ifBlank { "Hilo" } + "." + ext)
    }

    private fun send(ctx: Context, f: File, mime: String) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val i = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, f.nameWithoutExtension).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, "Enviar «${f.name}»").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun share(ctx: Context, title: String, paragraphs: List<String>) {
        val f = file(ctx, title, "docx")
        docx(f, title, paragraphs)
        send(ctx, f, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    }

    /** Referencia en BibTeX (para LaTeX o Zotero). */
    fun bibtex(ref: BookRef, fallbackTitle: String): String {
        val last = ref.authors.split(Regex("[;,]")).firstOrNull()?.trim()?.split(" ")?.lastOrNull().orEmpty()
        val key = (last.ifBlank { "ref" } + ref.year).replace(Regex("[^A-Za-z0-9]"), "")
        val authors = ref.authors.split(";").map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" and ")
        return buildString {
            append("@book{").append(key).append(",\n")
            if (authors.isNotBlank()) append("  author = {").append(authors).append("},\n")
            append("  title = {").append(ref.title.ifBlank { fallbackTitle }).append("},\n")
            if (ref.year.isNotBlank()) append("  year = {").append(ref.year).append("},\n")
            if (ref.publisher.isNotBlank()) append("  publisher = {").append(ref.publisher).append("},\n")
            if (ref.url.isNotBlank()) append("  url = {").append(ref.url).append("},\n")
            append("}")
        }
    }

    fun shareBib(ctx: Context, name: String, text: String) {
        val f = file(ctx, name, "bib")
        f.writeText(text)
        send(ctx, f, "text/plain")
    }
}
