package io.github.arielegno.radio.core

import java.text.Normalizer

/**
 * Fichas de artistas, integrantes y festivales (igual que la versión web):
 * primero tu nube (archivos JSON), luego Wikipedia y, si falta algo, la inteligencia artificial.
 */
object Fichas {
    fun slug(t: String?): String = Normalizer.normalize((t ?: "").lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9]+"), "-").trim('-')

    private fun norm(t: String?) = Normalizer.normalize((t ?: "").lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    private fun v(x: Any?): String { val s = when (x) { null -> ""; is Double -> if (x == Math.floor(x)) x.toLong().toString() else x.toString(); else -> x.toString() }.trim(); return if (s == "null") "" else s }

    private fun lista(x: Any?): List<String> = ((x as? List<*>) ?: emptyList<Any?>()).mapNotNull { e ->
        when (e) { is String -> e.trim(); is Map<*, *> -> v(e["nombre"] ?: e["texto"] ?: e["dato"] ?: e["name"]); else -> null }?.takeIf { it.isNotEmpty() && it != "null" }
    }

    fun cortar(t: String, n: Int = 700): String {
        val s = t.trim(); if (s.length <= n) return s
        val c = s.take(n); val k = c.lastIndexOf(". ")
        return if (k > 300) c.take(k + 1) else "$c…"
    }

    data class Integrante(val nombre: String, val datos: Map<String, Any?> = emptyMap())
    data class Festival(val nombre: String, val id: String = "", val dato: String = "")

    data class Artista(
        val bio: String = "", val generos: List<String> = emptyList(), val formacion: String = "", val separacion: String = "",
        val miembros: List<Integrante> = emptyList(), val festivales: List<Festival> = emptyList(), val debut: String = "", val obraMaestra: String = "",
        val bandcamp: String = "", val curiosidades: List<String> = emptyList(), val fuente: String = ""
    ) {
        val vacia: Boolean get() = bio.isEmpty() && miembros.isEmpty() && festivales.isEmpty() && formacion.isEmpty()
    }

    private fun integrantes(x: Any?): List<Integrante> = ((x as? List<*>) ?: emptyList<Any?>()).mapNotNull { e ->
        when (e) {
            is String -> e.trim().takeIf { it.isNotEmpty() && it != "null" }?.let { Integrante(it) }
            is Map<*, *> -> { @Suppress("UNCHECKED_CAST") val m = e as Map<String, Any?>; v(m["nombre"] ?: m["name"]).takeIf { it.isNotEmpty() }?.let { Integrante(it, m) } }
            else -> null
        }
    }

    private fun festivales(x: Any?): List<Festival> = ((x as? List<*>) ?: emptyList<Any?>()).mapNotNull { e ->
        when (e) {
            is String -> e.trim().takeIf { it.isNotEmpty() && it != "null" }?.let { Festival(it) }
            is Map<*, *> -> {
                val id = slug(v(e["festival"] ?: e["id"] ?: e["nombre"])); val n = v(e["nombre"] ?: e["festival"]).ifEmpty { id.replace('-', ' ') }
                if (n.isEmpty()) null else Festival(n, id, v(e["dato"] ?: e["texto"] ?: e["contexto"]))
            }
            else -> null
        }
    }

    /** Archivo de tu nube: artistas/<nombre>.json (acepta los dos esquemas de la web). */
    fun artistaDeNube(json: String?): Artista? {
        val j = Json.obj(json); if (j.isEmpty()) return null
        val bio = v(j["biografia_larga"] ?: j["biografia"]).ifEmpty { v(j["bio"]) }
        if (bio.isEmpty() && j["miembros"] == null) return null
        return Artista(bio, lista(j["generos"]), v(j["formacion"]), v(j["separacion"]), integrantes(j["miembros"]), festivales(j["festivales"]),
            v(j["album_debut"]), v(j["obra_maestra"]), v(j["bandcampUrl"]).takeIf { it.startsWith("https://") } ?: "", lista(j["datos_curiosos"] ?: j["curiosidades"]), "nube")
    }

    /** JSON que devuelve la IA (a veces viene entre ```). */
    fun jsonDeTexto(t: String?): Map<String, Any?>? {
        val s = (t ?: "").replace(Regex("```json", RegexOption.IGNORE_CASE), "").replace("```", "")
        val a = s.indexOf('{'); val b = s.lastIndexOf('}'); if (a < 0 || b <= a) return null
        return Json.obj(s.substring(a, b + 1)).takeIf { it.isNotEmpty() }
    }

    fun artistaDeIA(t: String?): Artista? {
        val j = jsonDeTexto(t) ?: return null
        return Artista(v(j["bio"]), lista(j["generos"]), v(j["formacion"]), v(j["separacion"]), integrantes(j["miembros"]), festivales(j["festivales"]),
            v(j["album_debut"]), v(j["obra_maestra"]), "", lista(j["curiosidades"]), "ia").takeIf { !it.vacia }
    }

    const val PROMPT_ARTISTA = "Eres un crítico musical experto. Escribes en español. Devuelve SOLO un objeto JSON válido (sin markdown, sin bloques de código) con la siguiente estructura exacta:\n" +
        "{\n  \"bio\": \"Biografía de máximo 100 palabras.\",\n  \"generos\": [\"Género 1\", \"Género 2\"],\n  \"formacion\": \"Año (ej. 1989)\",\n  \"separacion\": \"Año o null si sigue activo\",\n" +
        "  \"miembros\": [\"Nombre 1\", \"Nombre 2\"],\n  \"album_debut\": \"Nombre del álbum\",\n  \"obra_maestra\": \"Nombre del álbum más aclamado\",\n  \"festivales\": [\"Festival (año)\"]\n}\n" +
        "Afirma solo datos de los que estés muy seguro. Si no sabes un dato o dudas, usa null o un array vacío; nunca inventes. En festivales incluye solo aquellos en los que estés seguro de que la banda tocó; si dudas, []."

    const val PROMPT_MIEMBRO = "Eres un periodista musical riguroso. Escribes en español. Devuelve SOLO un objeto JSON válido (sin markdown) con esta estructura exacta:\n" +
        "{\"rol\":\"instrumento o función en la banda o null\",\"bio\":\"Biografía de máximo 70 palabras o null\",\"en_la_banda\":\"activo | salió | falleció | desconocido\",\"periodo\":\"ej. 1989–actualidad o null\",\"otros_proyectos\":[\"proyecto 1\"],\"curiosidad\":\"un dato verificable o null\"}\n" +
        "Afirma solo datos de los que estés muy seguro. Si dudas, usa null, \"desconocido\" o []; nunca inventes."

    const val PROMPT_FESTIVAL = "Eres un periodista musical riguroso. Escribes en español. Devuelve SOLO un objeto JSON válido (sin markdown) con esta estructura exacta:\n" +
        "{\"historia\":\"Historia en máximo 90 palabras o null\",\"pais\":\"País o null\",\"inicio\":\"año de la primera edición o null\",\"ediciones_relevantes\":[\"Año: hecho destacado\"],\"participantes_destacados\":[\"Artista\"]}\n" +
        "Afirma solo datos de los que estés muy seguro. Si dudas, usa null o []; nunca inventes."

    /** Resumen de Wikipedia (lo que guardamos). */
    data class Wiki(val texto: String, val desc: String, val imagen: String, val url: String)

    fun wiki(json: String?): Wiki? {
        val o = Json.obj(json); if (o.str("type") != "standard" || o.str("extract").isBlank()) return null
        val img = o.map("thumbnail").str("source").let { if (it.startsWith("https://upload.wikimedia.org/")) it.replace(Regex("/\\d+px-"), "/960px-") else "" }
        return Wiki(o.str("extract"), o.str("description"), img, o.map("content_urls").map("mobile").str("page"))
    }

    fun esMusico(w: Wiki, banda: String): Boolean {
        val t = norm(w.desc + " " + w.texto)
        return Regex("music|cantante|compositor|guitarr|bajist|bateris|teclist|vocalist|productor|rapero|multiinstrument|percusion|\\bdj\\b").containsMatchIn(t) || t.contains(norm(banda))
    }

    fun esFestival(w: Wiki): Boolean = norm(w.desc + " " + w.texto).contains("festival")

    data class Miembro(val pills: List<String>, val bio: String, val fuente: String, val proyectos: List<String>, val curiosidad: String, val imagen: String, val url: String)

    /** ¿Hace falta preguntarle a la IA por este integrante? */
    fun miembroIncompleto(obj: Map<String, Any?>): Boolean = (obj["activo"] == null && obj["estado"] == null) || (obj["otros_proyectos"] == null && obj["otrosProyectos"] == null)

    fun miembro(obj: Map<String, Any?>, w: Wiki?, ia: Map<String, Any?>?): Miembro {
        val crudo: Any? = obj["activo"] ?: obj["estado"] ?: ia?.get("en_la_banda")
        val t = norm(v(crudo))
        val estado = when {
            crudo == true || Regex("^(activo|true|si|en la banda)$").matches(t) -> "En la banda"
            crudo == false || Regex("sali|\\bex\\b|dej|ya no").containsMatchIn(t) -> "Ya no está en la banda"
            Regex("falle|muri").containsMatchIn(t) -> "Falleció"
            else -> ""
        }
        val bio = v(obj["bio"]).ifEmpty { w?.let { cortar(it.texto) } ?: "" }.ifEmpty { v(ia?.get("bio")) }
        val fuente = if (v(obj["bio"]).isNotEmpty()) "nube" else if (w != null) "wiki" else "ia"
        val pills = listOf(v(obj["rol"]).ifEmpty { v(ia?.get("rol")) }, estado, v(obj["periodo"]).ifEmpty { v(ia?.get("periodo")) }).filter { it.isNotEmpty() }
        val proyectos = lista(obj["otros_proyectos"] ?: obj["otrosProyectos"] ?: ia?.get("otros_proyectos"))
        return Miembro(pills, bio, if (bio.isEmpty()) "" else fuente, proyectos, v(obj["curiosidad"]).ifEmpty { v(ia?.get("curiosidad")) }, v(obj["imagen"]).ifEmpty { w?.imagen ?: "" }, w?.url ?: "")
    }

    data class FichaFestival(val pills: List<String>, val historia: String, val fuente: String, val ediciones: List<String>, val participantes: List<String>, val imagen: String, val url: String)

    /** Nombre sin el año ni paréntesis: «Coachella (2023)» → «Coachella». */
    fun festivalLimpio(n: String): String = n.replace(Regex("\\s*\\([^)]*\\)\\s*"), " ").replace(Regex("\\b(19|20)\\d{2}\\b"), "").replace(Regex("\\s+"), " ").trim().ifEmpty { n }
    fun festivalAnio(n: String): String = Regex("\\b(19|20)\\d{2}\\b").find(n)?.value ?: ""

    fun festival(nube: Map<String, Any?>, w: Wiki?, ia: Map<String, Any?>?): FichaFestival {
        var historia = v(nube["historia"] ?: nube["descripcion"] ?: nube["biografia_larga"] ?: nube["bio"]); var fuente = if (historia.isNotEmpty()) "nube" else ""
        var img = v(nube["imagen"]); var url = ""
        if (historia.isEmpty() && w != null) { historia = cortar(w.texto, 800); fuente = "wiki"; url = w.url; if (img.isEmpty()) img = w.imagen }
        if (historia.isEmpty() && v(ia?.get("historia")).isNotEmpty()) { historia = v(ia?.get("historia")); fuente = "ia" }
        val ed = lista(nube["ediciones_relevantes"] ?: nube["ediciones"] ?: nube["hitos"]).ifEmpty { lista(ia?.get("ediciones_relevantes")) }
        val par = lista(nube["participantes_destacados"] ?: nube["participantes"] ?: nube["artistas"]).ifEmpty { lista(ia?.get("participantes_destacados")) }
        val pais = v(nube["pais"]).ifEmpty { v(ia?.get("pais")) }; val inicio = v(nube["inicio"] ?: nube["anio_inicio"]).ifEmpty { v(ia?.get("inicio")) }
        return FichaFestival(listOf(pais, if (inicio.isNotEmpty()) "Desde $inicio" else "").filter { it.isNotEmpty() }, historia, fuente, ed, par, img, url)
    }

    /** Foto del artista desde la búsqueda de Spotify: solo si el nombre coincide (nunca un homónimo). */
    fun fotoSpotify(json: String, nombre: String): String {
        val n = normNombre(nombre)
        return Json.obj(json).map("artists").list("items").mapNotNull { it.asObj() }
            .firstOrNull { normNombre(it.str("name")) == n && it.list("images").isNotEmpty() }
            ?.list("images")?.firstOrNull()?.asObj()?.str("url").orEmpty()
    }
}
