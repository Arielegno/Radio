package io.github.arielegno.radio.core

/**
 * Lee el archivo de ajustes que exporta la versión web (radio_ruta_config.txt) y lo convierte a los ajustes de la app.
 * No trae claves: la web nunca las exporta.
 */
object AjustesWeb {
    private val TEXTOS = mapOf(
        "gender" to "genero", "accent" to "acento", "style" to "estilo", "len" to "largo", "avoid" to "evitar", "persona" to "persona",
        "contextoMusical" to "contexto", "host" to "host", "voicePreset" to "vozPreset", "clientId" to "spClientId", "provider" to "proveedor",
        "geminiModelSelect" to "geminiModelSel", "geminiModel" to "geminiModel", "model" to "claudeModel", "sbUrl" to "sbUrl",
        "efemerides" to "efemerides", "dedic" to "dedic", "me" to "me", "units" to "unidades", "h12" to "h12", "nomEmail" to "nomEmail",
        "listeningMode" to "modo", "cardSize" to "portadas"
    )
    private val ENTEROS = mapOf("tEvery" to ("cadaN" to 1..10), "wMin" to ("wMin" to 5..180), "pEvery" to ("pEvery" to 1..20), "fsize" to ("letra" to 15..24))
    private val SINO = mapOf("shuffle" to "mezclar", "wifiOnly" to "soloWifi", "memoria" to "memoria")
    private val VALIDOS = mapOf(
        "genero" to setOf("locutora", "locutor"), "acento" to Luna.ACENTOS.keys, "estilo" to Luna.ESTILOS.keys, "largo" to setOf("corta", "media", "larga"),
        "vozPreset" to setOf("natural", "radio", "narrador", "energico", "manual"), "proveedor" to setOf("gemini", "claude"), "unidades" to setOf("c", "f"),
        "h12" to setOf("auto", "1", "0"), "modo" to Modos.NOMBRES.toSet(), "portadas" to setOf("small", "medium", "large")
    )
    private val ACENTOS_COLOR = setOf("#f2c230", "#ff8a3d", "#4cd18a", "#5bc0ff", "#ff7eb6")

    /** true si parece un archivo de ajustes de la web. */
    fun esDeLaWeb(o: Map<String, Any?>): Boolean = o.isNotEmpty() && listOf("host", "provider", "listeningMode", "tEvery", "voicePreset", "source").count { it in o } >= 3

    private fun texto(v: Any?): String = when (v) { null -> ""; is String -> v; is Double -> if (v == Math.floor(v)) v.toLong().toString() else v.toString(); else -> v.toString() }

    /** Ajustes de la app (clave → valor) a partir del archivo de la web; null si no es de la web. */
    fun convertir(o: Map<String, Any?>): Map<String, Any?>? {
        if (!esDeLaWeb(o)) return null
        val m = linkedMapOf<String, Any?>()
        for ((w, a) in TEXTOS) {
            if (w !in o) continue
            val v = texto(o[w]).trim()
            if (v.isEmpty() && a in setOf("host", "proveedor", "modo", "genero", "acento", "largo", "vozPreset", "unidades", "h12", "portadas", "geminiModelSel", "claudeModel")) continue
            if (VALIDOS[a]?.let { v !in it } == true) continue
            m[a] = v.take(4000)
        }
        for ((w, d) in ENTEROS) texto(o[w]).toDoubleOrNull()?.let { m[d.first] = it.toInt().coerceIn(d.second) }
        for ((w, a) in SINO) if (w in o) m[a] = texto(o[w]).let { it == "1" || it == "true" }
        Modos.NOMBRES.forEach { k ->
            o["mode_$k"]?.let { m["modo_$k"] = texto(it).take(4000) }
            o["frases_$k"]?.let { m["frases_$k"] = texto(it).take(4000) }
        }
        texto(o["rate"]).toDoubleOrNull()?.let { m["velocidad"] = (it * 100).toInt().coerceIn(60, 150) }
        texto(o["pitch"]).toDoubleOrNull()?.let { m["tono"] = (it * 100).toInt().coerceIn(60, 150) }
        texto(o["vol"]).toDoubleOrNull()?.let { m["volVoz"] = (it * 100).toInt().coerceIn(10, 100) }
        texto(o["accentColor"]).lowercase().takeIf { it in ACENTOS_COLOR }?.let { m["acentoColor"] = it }
        texto(o["cloudBaseUrl"]).trim().takeIf { Regex("^https://[a-z0-9.-]+(/[\\w./-]*)?$", RegexOption.IGNORE_CASE).matches(it) }?.let { m["nube"] = it }
        texto(o["source"]).takeIf { it == "spotify" || it == "local" }?.let { m["fuente"] = it }
        return m
    }
}
