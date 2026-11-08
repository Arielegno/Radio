package io.github.arielegno.radio.core

import java.time.LocalDate

/** Qué se ve en la portada y en qué orden (se guarda como JSON, igual que en la web). */
data class PortadaCfg(
    val ocultos: Set<String> = emptySet(),
    val orden: List<String> = TARJETAS.map { it.first },
    val disco: String = "vinilo",       // "vinilo" o "cuadrada"
    val tinte: Boolean = true
) {
    fun visible(k: String) = k !in ocultos

    fun alternar(k: String, ver: Boolean) = copy(ocultos = if (ver) ocultos - k else ocultos + k)

    fun mover(k: String, delta: Int): PortadaCfg {
        val i = orden.indexOf(k); val j = i + delta
        if (i < 0 || j < 0 || j >= orden.size) return this
        val o = orden.toMutableList(); o[i] = o[j].also { o[j] = o[i] }
        return copy(orden = o)
    }

    fun toJson(): String = Json.write(linkedMapOf("off" to ocultos.toList(), "order" to orden, "disc" to (if (disco == "cuadrada") "square" else "vinyl"), "tint" to tinte))

    companion object {
        val ARRIBA = listOf("hello" to "Saludo, fecha y clima", "place" to "Cerca de (tu zona)", "disc" to "Carátula grande", "modes" to "Modos de Luna")
        val TARJETAS = listOf("artists" to "Artistas más vistos", "foryou" to "Para ti", "day" to "Hoy en la música y concierto del día", "week" to "Tu mes en la radio", "club" to "Club de escucha")

        fun fromJson(t: String?): PortadaCfg {
            val o = Json.obj(t); val ids = TARJETAS.map { it.first }
            val orden = o.list("order").mapNotNull { it as? String }.filter { it in ids }.distinct().toMutableList()
            ids.forEach { if (it !in orden) orden += it }
            return PortadaCfg(o.list("off").mapNotNull { it as? String }.toSet(), orden, if (o.str("disc") == "square") "cuadrada" else "vinilo", o["tint"] != false)
        }
    }
}

/** Concierto de la Bóveda: lo mínimo para la tarjeta de la portada. */
data class Concierto(val id: String, val titulo: String, val artista: String, val anio: Int, val proveedor: String, val video: String,
                     val portada: String, val empieza: String, val fijadoEl: String, val creado: String) {
    /** Miniatura segura (solo de los sitios permitidos, como en la web). */
    fun miniatura(): String {
        if (Regex("^https://[a-z0-9-]+\\.supabase\\.co/storage/v1/object/public/revista/[\\w/.-]+$", RegexOption.IGNORE_CASE).matches(portada) ||
            Regex("^https://upload\\.wikimedia\\.org/[^\\s\"'<>]+$").matches(portada)) return portada
        if (proveedor == "youtube" && Regex("^[\\w-]{6,20}$").matches(video)) return "https://i.ytimg.com/vi/$video/hqdefault.jpg"
        if (proveedor == "archive" && Regex("^[\\w.-]{3,100}$").matches(video)) return "https://archive.org/services/img/$video"
        return ""
    }

    companion object {
        fun from(m: Map<String, Any?>) = Concierto(m.str("id"), m.str("title"), m.str("artist"), m.num("year").toInt(), m.str("provider"), m.str("video_id"),
            m.str("cover"), m.str("starts_on"), m.str("featured_on"), m.str("created_at"))

        /** El concierto del día: el fijado por el curador; si no, el que entró hoy; si no, va rotando. */
        fun delDia(lista: List<Concierto>, hoy: LocalDate): Concierto? {
            if (lista.isEmpty()) return null
            val t = hoy.toString()
            lista.firstOrNull { it.fijadoEl == t }?.let { return it }
            lista.filter { it.empieza == t }.maxByOrNull { it.creado }?.let { return it }
            val s = lista.sortedWith(compareBy<Concierto>({ it.empieza }, { it.id }))
            return s[(hoy.toEpochDay() % s.size).toInt()]
        }
    }
}

object Efemerides {
    /** Líneas «MM-DD: texto» propias de la persona. */
    fun propias(texto: String, hoy: LocalDate): String {
        val md = String.format("%02d-%02d", hoy.monthValue, hoy.dayOfMonth)
        return texto.lines().mapNotNull { l -> Regex("^(\\d{2}-\\d{2})\\s*:\\s*(.+)$").find(l.trim())?.takeIf { it.groupValues[1] == md }?.groupValues?.get(2) }.joinToString(". ")
    }

    private val MUSICA = Regex("\\b(banda|grupo musical|cantante|m[uú]sico|compositor|[aá]lbum|disco|sencillo|canci[oó]n|concierto|festival|gira|rock|jazz|punk|pop|orquesta|guitarrista|baterista|bajista|rapero|reguet[oó]n|salsa|cumbia)\\b", RegexOption.IGNORE_CASE)

    /** De la respuesta de Wikipedia «un día como hoy», solo lo que es de música. */
    fun deWikipedia(json: String): List<Pair<String, String>> =
        Json.obj(json).list("events").mapNotNull { it.asObj() }
            .filter { MUSICA.containsMatchIn(it.str("text")) }.take(4)
            .map { it.str("year") to it.str("text").take(220) }
}

object Clima {
    /** Códigos WMO de Open-Meteo, en palabras. */
    fun wmo(c: Int): String = when (c) {
        0 -> "despejado"; 1 -> "mayormente despejado"; 2 -> "parcialmente nublado"; 3 -> "nublado"
        45, 48 -> "con niebla"; 51, 53, 55 -> "con llovizna"; 56, 57 -> "con llovizna helada"
        61, 63, 65 -> "con lluvia"; 66, 67 -> "con lluvia helada"; 71, 73, 75, 77 -> "con nieve"
        80, 81, 82 -> "con chubascos"; 85, 86 -> "con chubascos de nieve"; 95, 96, 99 -> "con tormenta"
        else -> ""
    }

    /** "24°, nublado" a partir de la respuesta de Open-Meteo. */
    fun deOpenMeteo(json: String, fahrenheit: Boolean): String? {
        val c = Json.obj(json).map("current"); if (c.isEmpty() || c["temperature_2m"] == null) return null
        val t = Math.round(c.dbl("temperature_2m")); val w = wmo(c.num("weather_code").toInt())
        return "$t°${if (fahrenheit) "F" else ""}" + if (w.isNotEmpty()) ", $w" else ""
    }

    /** Nombre de la zona a partir de Nominatim. */
    /** Solo la ciudad o el pueblo (nunca la colonia o el barrio: no hace falta tanta precisión). */
    fun zonaNominatim(json: String): String {
        val a = Json.obj(json).map("address")
        val ciudad = listOf("city", "town", "village", "municipality", "county").map { a.str(it) }.firstOrNull { it.isNotBlank() } ?: ""
        return ciudad.ifEmpty { a.str("state") }
    }
}
