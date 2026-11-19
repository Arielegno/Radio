package com.hilo.lector.text

import android.util.Base64
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.File
import java.util.zip.ZipFile

/** Formatos de texto además de EPUB y PDF: Markdown, HTML, FB2, Word (DOCX) y OpenDocument (ODT). */
object Formats {
    /** Extensión → tipo interno. */
    val TEXT_TYPES = setOf("txt", "md", "html", "fb2", "docx", "odt", "epub", "mobi")

    fun typeOfName(name: String): String? {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf" -> "pdf"
            "epub" -> "epub"
            "txt", "text", "log" -> "txt"
            "md", "markdown", "mdown" -> "md"
            "html", "htm", "xhtml" -> "html"
            "fb2" -> "fb2"
            "docx" -> "docx"
            "odt" -> "odt"
            "mobi", "azw", "azw3", "prc", "pdb" -> "mobi"
            "cbz", "cbr" -> "comic"
            "jpg", "jpeg", "png", "webp", "heic", "heif" -> "img"
            else -> null
        }
    }

    fun typeOfMime(mime: String): String? = when {
        mime.startsWith("image/") -> "img"
        mime == "application/pdf" -> "pdf"
        mime == "application/epub+zip" -> "epub"
        mime == "text/markdown" || mime == "text/x-markdown" -> "md"
        mime == "text/html" || mime == "application/xhtml+xml" -> "html"
        mime.contains("fictionbook") -> "fb2"
        mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
        mime == "application/vnd.oasis.opendocument.text" -> "odt"
        mime.contains("mobipocket") || mime.contains("amazon.ebook") || mime.contains("kindle") -> "mobi"
        mime.contains("comicbook") || mime.contains("cbz") || mime.contains("cbr") -> "comic"
        mime == "text/plain" -> "txt"
        else -> null
    }

    /** Los formatos que Hilo abre, en el orden en que se muestran en Ajustes. */
    val ALL_TYPES = listOf("pdf", "epub", "docx", "mobi", "odt", "fb2", "html", "md", "txt", "comic")

    fun label(type: String) = when (type) {
        "pdf" -> "PDF"; "epub" -> "EPUB"; "md" -> "Markdown"; "html" -> "HTML"
        "fb2" -> "FB2"; "docx" -> "Word"; "odt" -> "ODT"; "mobi" -> "MOBI"; "comic" -> "Cómic"; else -> "Texto"
    }

    class Meta(val title: String, val author: String, val cover: ByteArray?)

    fun meta(type: String, f: File): Meta? = try {
        when (type) {
            "epub" -> Epub.meta(f).let { Meta(it.title, it.author, it.cover) }
            "fb2" -> fb2Meta(f)
            "docx" -> docxMeta(f)
            "mobi" -> Mobi.read(f, withText = false).let { Meta(it.title, it.author, it.cover) }
            "comic" -> Meta("", "", Comic.cover(f))
            "html" -> Jsoup.parse(f, null).title().takeIf { it.isNotBlank() }?.let { Meta(it, "", null) }
            else -> null
        }
    } catch (e: Exception) {
        null
    }

    fun paragraphs(type: String, f: File): List<String> = when (type) {
        "epub" -> Epub.paragraphs(f)
        "md" -> markdown(f.readText())
        "html" -> html(Jsoup.parse(f, null))
        "fb2" -> fb2(f)
        "docx" -> docx(f)
        "odt" -> odt(f)
        "mobi" -> html(Jsoup.parse(Mobi.read(f).html))
        "comic" -> emptyList()
        else -> TextTools.smartParas(f.readText())
    }

    // ---------- Markdown ----------

    private val MD_LINK = Regex("!?\\[([^\\]]*)]\\([^)]*\\)")
    private val MD_EMPH = Regex("(\\*\\*|__|\\*|_|`)")
    private val MD_HEAD = Regex("^#{1,6}\\s+")
    private val MD_LIST = Regex("^\\s*([-*+]|\\d+[.)])\\s+")
    private val MD_QUOTE = Regex("^>\\s?")

    fun markdown(t: String): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var inCode = false
        fun flush() {
            val s = cur.toString().trim()
            if (s.isNotEmpty()) out.add(s)
            cur.setLength(0)
        }
        for (raw in t.replace("\r", "").split("\n")) {
            if (raw.trimStart().startsWith("```")) { flush(); inCode = !inCode; continue }
            if (inCode) { if (raw.isNotBlank()) out.add(raw.trim()); continue }
            var line = raw.trim()
            if (line.isEmpty() || line.matches(Regex("^[-*_=]{3,}$"))) { flush(); continue }
            line = line.replace(MD_LINK, "$1").replace(MD_EMPH, "")
            when {
                MD_HEAD.containsMatchIn(line) -> { flush(); out.add("# " + line.replace(MD_HEAD, "")) }
                MD_LIST.containsMatchIn(line) -> { flush(); cur.append("• ").append(line.replace(MD_LIST, "")) }
                else -> {
                    line = line.replace(MD_QUOTE, "")
                    if (line.startsWith("|")) line = line.trim('|').split('|').joinToString(" · ") { it.trim() }
                    if (cur.isNotEmpty()) cur.append(' ')
                    cur.append(line)
                }
            }
        }
        flush()
        return out
    }

    // ---------- HTML ----------

    fun html(doc: Document): List<String> {
        val body = doc.body() ?: return emptyList()
        val out = ArrayList<String>()
        val els = body.select("h1, h2, h3, h4, p, li, blockquote, pre")
        if (els.isEmpty()) return TextTools.textToParas(body.wholeText())
        for (e in els) {
            if (e.tagName() == "p" && e.parents().any { it.tagName() == "li" || it.tagName() == "blockquote" }) continue
            val t = e.text().trim()
            if (t.isEmpty()) continue
            out.add(
                when {
                    e.tagName().startsWith("h") -> "# $t"
                    e.tagName() == "li" -> "• $t"
                    else -> t
                }
            )
        }
        return out
    }

    // ---------- FB2 (FictionBook) ----------

    private fun fb2Doc(f: File): Document = Jsoup.parse(f.readText(), "", Parser.xmlParser())

    private fun fb2Meta(f: File): Meta {
        val d = fb2Doc(f)
        val info = d.getElementsByTag("title-info").firstOrNull()
        val title = info?.getElementsByTag("book-title")?.firstOrNull()?.text().orEmpty()
        val a = info?.getElementsByTag("author")?.firstOrNull()
        val author = listOfNotNull(
            a?.getElementsByTag("first-name")?.firstOrNull()?.text(),
            a?.getElementsByTag("last-name")?.firstOrNull()?.text(),
        ).joinToString(" ")
        val href = info?.getElementsByTag("coverpage")?.firstOrNull()?.getElementsByTag("image")?.firstOrNull()
            ?.attributes()?.firstOrNull { it.key.endsWith("href") }?.value?.removePrefix("#")
        val bin = href?.let { id -> d.getElementsByTag("binary").firstOrNull { it.attr("id") == id } }
        val cover = bin?.let { runCatching { Base64.decode(it.text(), Base64.DEFAULT) }.getOrNull() }
        return Meta(title, author, cover)
    }

    private fun fb2(f: File): List<String> {
        val d = fb2Doc(f)
        val out = ArrayList<String>()
        for (body in d.getElementsByTag("body")) {
            if (body.attr("name") == "notes") continue
            walkFb2(body, out)
        }
        return out
    }

    private fun walkFb2(e: Element, out: MutableList<String>) {
        for (ch in e.children()) {
            when (ch.tagName()) {
                "title" -> ch.text().trim().takeIf { it.isNotEmpty() }?.let { out.add("# $it") }
                "p", "v", "subtitle", "text-author" -> ch.text().trim().takeIf { it.isNotEmpty() }?.let { out.add(it) }
                "empty-line", "image", "binary" -> {}
                else -> walkFb2(ch, out)
            }
        }
    }

    // ---------- Word (DOCX) ----------

    private fun zipText(f: File, path: String): String? = ZipFile(f).use { z ->
        z.getEntry(path)?.let { e -> z.getInputStream(e).bufferedReader().use { it.readText() } }
    }

    private fun docxMeta(f: File): Meta? {
        val core = zipText(f, "docProps/core.xml") ?: return null
        val d = Jsoup.parse(core, "", Parser.xmlParser())
        val title = d.getElementsByTag("dc:title").firstOrNull()?.text().orEmpty()
        val author = d.getElementsByTag("dc:creator").firstOrNull()?.text().orEmpty()
        return if (title.isBlank() && author.isBlank()) null else Meta(title, author, null)
    }

    private fun docx(f: File): List<String> {
        val xml = zipText(f, "word/document.xml") ?: return emptyList()
        val d = Jsoup.parse(xml, "", Parser.xmlParser())
        val out = ArrayList<String>()
        for (p in d.getElementsByTag("w:p")) {
            val sb = StringBuilder()
            for (node in p.getAllElements()) {
                when (node.tagName()) {
                    "w:t" -> sb.append(node.text())
                    "w:tab" -> sb.append(' ')
                    "w:br" -> sb.append(' ')
                }
            }
            val t = sb.toString().trim()
            if (t.isEmpty()) continue
            val style = p.getElementsByTag("w:pStyle").firstOrNull()?.attr("w:val").orEmpty().lowercase()
            val heading = style.startsWith("heading") || style.startsWith("ttulo") || style.startsWith("titulo") || style == "title"
            out.add(if (heading) "# $t" else t)
        }
        return out
    }

    // ---------- OpenDocument (ODT) ----------

    private fun odt(f: File): List<String> {
        val xml = zipText(f, "content.xml") ?: return emptyList()
        val d = Jsoup.parse(xml, "", Parser.xmlParser())
        val out = ArrayList<String>()
        for (e in d.getAllElements()) {
            when (e.tagName()) {
                "text:h" -> e.text().trim().takeIf { it.isNotEmpty() }?.let { out.add("# $it") }
                "text:p" -> if (e.parents().none { it.tagName() == "text:p" }) {
                    e.text().trim().takeIf { it.isNotEmpty() }?.let { out.add(it) }
                }
            }
        }
        return out
    }
}
