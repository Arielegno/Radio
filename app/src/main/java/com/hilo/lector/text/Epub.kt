package com.hilo.lector.text

import android.net.Uri
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.io.File
import java.util.zip.ZipFile

/** Lee EPUB sin DRM: título, autor, portada y texto en orden. */
object Epub {
    class Meta(val title: String, val author: String, val cover: ByteArray?)
    private class Item(val href: String, val type: String, val props: String)

    private fun read(z: ZipFile, path: String): String? =
        z.getEntry(path)?.let { e -> z.getInputStream(e).bufferedReader().use { it.readText() } }

    private fun normPath(p: String): String {
        val out = ArrayList<String>()
        for (s in p.split('/')) when (s) {
            ".." -> if (out.isNotEmpty()) out.removeAt(out.size - 1)
            "", "." -> {}
            else -> out.add(s)
        }
        return out.joinToString("/")
    }

    private fun resolve(base: String, href: String): String =
        normPath((if (base.isEmpty()) "" else "$base/") + Uri.decode(href.substringBefore('#')))

    private fun opf(z: ZipFile): Triple<Document, String, Map<String, Item>> {
        val container = read(z, "META-INF/container.xml") ?: error("Sin container.xml")
        val path = Jsoup.parse(container, "", Parser.xmlParser()).selectFirst("rootfile")?.attr("full-path")
            ?: error("Sin OPF")
        val doc = Jsoup.parse(read(z, path) ?: error("OPF vacío"), "", Parser.xmlParser())
        val base = path.substringBeforeLast('/', "")
        val items = HashMap<String, Item>()
        for (it in doc.getElementsByTag("item")) {
            items[it.attr("id")] = Item(it.attr("href"), it.attr("media-type"), it.attr("properties"))
        }
        return Triple(doc, base, items)
    }

    fun meta(f: File): Meta = ZipFile(f).use { z ->
        val (doc, base, items) = opf(z)
        val title = doc.getElementsByTag("dc:title").firstOrNull()?.text()?.trim().orEmpty()
        val author = doc.getElementsByTag("dc:creator").firstOrNull()?.text()?.trim().orEmpty()
        var href: String? = items.values.firstOrNull { "cover-image" in it.props.split(' ') }?.href
        if (href == null) {
            val id = doc.getElementsByTag("meta").firstOrNull { it.attr("name") == "cover" }?.attr("content")
            href = id?.let { items[it]?.href }
        }
        if (href == null) {
            href = items.values.firstOrNull { it.type.startsWith("image") && it.href.contains("cover", true) }?.href
        }
        val cover = href?.let { h -> z.getEntry(resolve(base, h))?.let { e -> z.getInputStream(e).use { it.readBytes() } } }
        Meta(title, author, cover)
    }

    fun paragraphs(f: File): List<String> = ZipFile(f).use { z ->
        val (doc, base, items) = opf(z)
        val out = ArrayList<String>()
        for (ref in doc.getElementsByTag("itemref")) {
            val item = items[ref.attr("idref")] ?: continue
            val html = read(z, resolve(base, item.href)) ?: continue
            val body = Jsoup.parse(html).body() ?: continue
            val els = body.select("h1, h2, h3, p")
            if (els.isNotEmpty()) {
                for (e in els) {
                    val t = e.text().trim()
                    if (t.isEmpty()) continue
                    out.add(if (e.tagName().startsWith("h")) "# $t" else t)
                }
            } else {
                out.addAll(TextTools.textToParas(body.wholeText()))
            }
        }
        out
    }
}
