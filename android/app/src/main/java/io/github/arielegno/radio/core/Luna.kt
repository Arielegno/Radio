package io.github.arielegno.radio.core

import java.time.DayOfWeek
import java.time.LocalDateTime
import kotlin.random.Random

/** Ajustes de Luna que vienen de la pantalla de Ajustes. */
data class LunaAjustes(
    val host: String = "Luna",
    val genero: String = "locutora",
    val acento: String = "neutro",
    val largo: String = "media",
    val modo: String = "carretera",
    val modoTexto: String = "",       // texto propio del modo (si lo escribió la persona)
    val persona: String = "",         // estilo pedido por la persona
    val evitar: String = "",
    val nombre: String = "",
    val frasesPropias: List<String> = emptyList(),
    val estilo: String = "",          // "", noche, deporte, humor, docu
    val contexto: String = ""         // contexto musical de tu biblioteca
)

/** Lo que Luna sabe en ese momento. */
data class LunaContexto(
    val prev: Track?,
    val next: Track?,
    val hora: String = "",
    val clima: String = "",
    val lugar: String = "",
    val efemeride: String = "",
    val dedicatoria: String = "",
    val programa: String = "",
    val memoria: String = "",
    val anteriores: List<String> = emptyList()
)

object Luna {
    val ACENTOS = linkedMapOf("neutro" to "neutro", "mx" to "de México", "ar" to "de Argentina", "es" to "de España", "co" to "de Colombia", "cl" to "de Chile", "hn" to "de Honduras")
    val LARGOS = linkedMapOf("corta" to "25 y 45", "media" to "50 y 90", "larga" to "90 y 140")
    val ESTILOS = linkedMapOf("" to "Natural", "noche" to "Radio FM nocturna", "deporte" to "Deportiva y enérgica", "humor" to "Cómica", "docu" to "Documental")
    private val STY = mapOf("noche" to "Estilo radio FM nocturna: voz suave, pausada e íntima.", "deporte" to "Estilo de locutor deportivo: enérgico, rápido y entusiasta.",
        "humor" to "Estilo cómico: ligero, con humor sano y juegos de palabras.", "docu" to "Estilo documental: sereno, curioso y narrativo.")

    fun animoHora(h: Int) = when { h < 6 -> "madrugada, voz baja y tranquila"; h < 12 -> "mañana, energía suave y fresca"; h < 19 -> "tarde, ritmo normal"; else -> "noche, ritmo calmado" }

    fun sistema(a: LunaAjustes, conSiguiente: Boolean, hora: Int): String {
        val modo = Modos.valido(a.modo)
        val prohibidos = Modos.NOMBRES.filter { it != modo && Modos.PROHIBIDOS.containsKey(it) }.joinToString("; ") { Modos.PROHIBIDOS.getValue(it) }
        val o = mutableListOf<String>()
        o += "Eres ${a.host}, ${a.genero} de una radio personal que acompaña a una persona. Hablas con chispa, humor seco y cero solemnidad, en español ${ACENTOS[a.acento] ?: "neutro"}. Usas muletillas como \"Fíjate\", \"Mira\" o \"Te cuento\" (sin abusar) e improvisas un poco. Suenas como una amiga que comparte música."
        o += "Tu texto se lee en voz alta: sin emojis, sin markdown, sin listas, sin paréntesis; escribe horas y números como se dicen. Frases cortas."
        o += "Entre ${LARGOS[a.largo] ?: LARGOS.getValue("media")} palabras. " + if (conSiguiente) "Cierra la canción que terminó con un dato curioso del artista o del disco y presenta la que viene." else "Comenta la canción que está sonando ahora con un dato curioso del artista o del disco; no hables de otra."
        o += "Afirma solo datos de los que estés muy segura. Nunca inventes colaboraciones, fechas ni premios. Si no tienes un dato bueno, comenta el estilo o la época de forma general."
        o += "Si te doy hora, clima o lugar, menciónalos de forma natural y breve, sin añadir nada que no esté en los datos. Si no te los doy, no los menciones."
        o += "Nunca digas que eres una IA y nunca menciones la velocidad. No repitas ideas de tus intervenciones anteriores."
        o += "La persona que escucha" + (if (a.nombre.isNotBlank()) " se llama ${a.nombre.trim()}" else "") + "."
        o += "Situación: " + (a.modoTexto.ifBlank { Modos.DESCRIPCION.getValue(modo) }) + if (prohibidos.isNotEmpty()) " No hables de: $prohibidos." else ""
        STY[a.estilo]?.let { o += it }
        if (a.persona.isNotBlank()) o += "Estilo pedido por la persona: ${a.persona.trim()}"
        if (a.contexto.isNotBlank()) o += "Contexto musical de la biblioteca de la persona: ${a.contexto.trim()}"
        if (a.evitar.isNotBlank()) o += "Evita por completo estos temas: ${a.evitar.trim()}."
        o += "Ánimo según la hora: ${animoHora(hora)}."
        o += "Si te doy efemeride_de_hoy, menciónala en una frase: es un dato real. Si te doy dedicatoria, léela con tus palabras y cariño, una sola vez. Si te doy programa, preséntalo como una sección especial de la radio, con su nombre, en pocas frases y con calidez."
        o += "Si te doy memoria_de_la_persona, úsala con naturalidad (\"la que más has escuchado\") sin recitar la lista."
        o += "Responde solo con el texto a leer."
        return o.joinToString("\n")
    }

    private fun cancion(t: Track): Map<String, Any?> = linkedMapOf<String, Any?>("titulo" to t.name, "artista" to t.artist).also {
        if (t.album.isNotBlank()) it["album"] = t.album
        if (t.year > 0) it["anio"] = t.year
        if (t.genre.isNotBlank()) it["genero"] = t.genre
    }

    /** El mensaje del usuario (JSON) que va a Gemini. */
    fun usuario(c: LunaContexto): String {
        val m = linkedMapOf<String, Any?>()
        val p = c.prev; val n = c.next
        if (p != null && n != null) { m["cancion_que_termino"] = cancion(p); m["cancion_que_viene"] = cancion(n) }
        else if (p != null) m["cancion_que_suena_ahora"] = cancion(p)
        else if (n != null) m["cancion_que_viene"] = cancion(n)
        if (c.hora.isNotBlank()) m["hora"] = c.hora
        if (c.clima.isNotBlank()) m["clima"] = c.clima
        if (c.lugar.isNotBlank()) m["lugar"] = c.lugar
        if (c.efemeride.isNotBlank()) m["efemeride_de_hoy"] = c.efemeride
        if (c.memoria.isNotBlank()) m["memoria_de_la_persona"] = c.memoria
        if (c.dedicatoria.isNotBlank()) m["dedicatoria"] = c.dedicatoria
        if (c.programa.isNotBlank()) m["programa"] = c.programa
        m["intervenciones_anteriores"] = c.anteriores.takeLast(4)
        return Json.write(m)
    }

    /** Cuerpo de la petición a Gemini (generateContent). */
    fun peticionGemini(sistema: String, usuario: String): String = Json.write(linkedMapOf(
        "systemInstruction" to linkedMapOf("parts" to listOf(linkedMapOf("text" to sistema))),
        "contents" to listOf(linkedMapOf("role" to "user", "parts" to listOf(linkedMapOf("text" to usuario)))),
        "generationConfig" to linkedMapOf("maxOutputTokens" to 4000)
    ))

    /** Cuerpo de la petición a Claude (Anthropic Messages). */
    fun peticionClaude(modelo: String, sistema: String, usuario: String): String = Json.write(linkedMapOf(
        "model" to modelo, "max_tokens" to 1000, "system" to sistema,
        "messages" to listOf(linkedMapOf("role" to "user", "content" to usuario))
    ))

    /** Saca el texto de la respuesta de Claude; null si no trae texto. */
    fun textoClaude(respuesta: String): String? =
        limpiar(Json.obj(respuesta).list("content").mapNotNull { it.asObj() }.filter { it.str("type") == "text" }.joinToString("") { it.str("text") }).ifBlank { null }

    /** Saca el texto de la respuesta de Gemini; null si no trae texto. */
    fun textoGemini(respuesta: String): String? {
        val o = Json.obj(respuesta)
        val cand = o.list("candidates").firstOrNull().asObj() ?: return null
        val parts = cand.map("content").list("parts")
        val t = parts.mapNotNull { it.asObj()?.let { p -> if (p.bool("thought")) null else p.str("text") } }.joinToString("").trim()
        return limpiar(t).ifBlank { null }
    }

    /** Quita markdown y emojis para que se lea bien. */
    fun limpiar(t: String): String = t
        .replace(Regex("[*_#`>]+"), "")
        .replace(Regex("[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{FE0F}]"), "")
        .replace(Regex("\\s+"), " ").trim()

    /** Sin clave de IA: frases sencillas (como en la web). */
    fun sinIA(c: LunaContexto, a: LunaAjustes, rnd: Random = Random.Default): String {
        val modo = Modos.valido(a.modo)
        val frases = a.frasesPropias.ifEmpty { Modos.FRASES[modo] ?: emptyList() } + (Modos.FRASES["neutral"] ?: emptyList())
        val o = StringBuilder()
        val p = c.prev; val n = c.next
        if (p != null) {
            val inicios = listOf("Te cuento, sonó ${p.name}, de ${p.artist}. ", "Esto fue ${p.name}, de ${p.artist}. ", "${frases[rnd.nextInt(frases.size)]}: acaba de sonar ${p.name}, de ${p.artist}. ")
            o.append(inicios[rnd.nextInt(inicios.size)])
        }
        if (c.programa.isNotBlank()) o.append(c.programa.substringBefore(':').trim().trim('«', '»').let { if (it.isNotBlank()) "Bienvenidos a «$it». " else "" })
        if (c.dedicatoria.isNotBlank()) o.append("Tengo una dedicatoria: ${c.dedicatoria}. ")
        if (c.efemeride.isNotBlank()) o.append("Un dato del día: ${c.efemeride}. ")
        if (c.clima.isNotBlank() && rnd.nextInt(3) == 0) o.append("Afuera, ${c.clima}. ")
        if (n != null) {
            val sig = listOf("Ahora viene ${n.name}, de ${n.artist}.", "Lo que sigue: ${n.name}, de ${n.artist}.", "Y para seguir, ${n.artist} con ${n.name}.")
            o.append(sig[rnd.nextInt(sig.size)])
        }
        return o.toString().trim().ifBlank { "Seguimos con más música." }
    }
}

/** Programas de Luna: «La hora del recuerdo» y «El resumen del domingo». */
object Programas {
    data class Estado(val recOn: Boolean, val recHora: Int, val recHecho: String, val semOn: Boolean, val semHecho: String)
    data class Resultado(val texto: String?, val marcarRec: String? = null, val marcarSem: String? = null)

    fun elegir(now: LocalDateTime, e: Estado, anioPrev: Int, mes: Resumen?): Resultado {
        val hoy = now.toLocalDate().toString()
        if (e.recOn && now.hour == e.recHora && e.recHecho != hoy) {
            val extra = if (anioPrev in 1900 until now.year - 10) " La canción que sonó es de $anioPrev: cuenta qué pasaba en la música ese año."
                else " Invita a recordar dónde estábamos cuando escuchamos estas canciones por primera vez."
            return Resultado("«La hora del recuerdo»: abre esta sección especial de la radio dedicada a canciones con historia, como un programa nocturno.$extra", marcarRec = hoy)
        }
        if (e.semOn && now.dayOfWeek == DayOfWeek.SUNDAY && now.hour >= 10 && e.semHecho != hoy && mes != null) {
            val ar = mes.topArtistas(3).joinToString(", ") { "${it.first} (${it.second})" }
            val so = mes.topCanciones(1).firstOrNull()?.first?.let { "${it.first} de ${it.second}" } ?: ""
            return Resultado("«El resumen del domingo»: cuenta, como una sección especial, lo que más ha sonado este mes en esta radio. Artistas: $ar. Canción más escuchada: $so. Total: ${mes.c} canciones.", marcarSem = hoy)
        }
        return Resultado(null)
    }
}
