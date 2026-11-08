package io.github.arielegno.radio.core

import java.text.Normalizer

/** Una canción de la biblioteca. Los archivos viven en la carpeta privada de la app. */
data class Track(
    val id: Long,
    val name: String,
    val artist: String,
    val album: String = "",
    val year: Int = 0,
    val genre: String = "",
    val durMs: Long = 0,
    val file: String = "",      // nombre del archivo de audio dentro de music/
    val mime: String = "",
    val size: Long = 0,
    val cover: String = "",     // nombre de la carátula dentro de covers/ ("" si no tiene)
    val addedAt: Long = 0
) {
    val key: String get() = keyOf(name, artist)

    fun toMap(): Map<String, Any?> = linkedMapOf(
        "id" to id, "name" to name, "artist" to artist, "album" to album, "year" to year, "genre" to genre,
        "durMs" to durMs, "file" to file, "mime" to mime, "size" to size, "cover" to cover, "addedAt" to addedAt
    )

    companion object {
        fun from(m: Map<String, Any?>): Track? {
            val id = m.num("id", -1); val name = m.str("name").trim()
            if (id < 0 || name.isEmpty() || m.str("file").isEmpty()) return null
            return Track(id, name, m.str("artist").trim().ifEmpty { "Artista desconocido" }, m.str("album"), m.num("year").toInt(), m.str("genre"),
                m.num("durMs"), m.str("file"), m.str("mime"), m.num("size"), m.str("cover"), m.num("addedAt"))
        }
    }
}

/** Igual que en la versión web: minúsculas, sin tildes ni signos. */
fun normNombre(t: String?): String =
    Normalizer.normalize((t ?: "").lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").replace(Regex("[^a-z0-9]"), "")

/** Clave de una canción (compatible con la versión web: «nombre|artista» en minúsculas). */
fun keyOf(name: String, artist: String): String = ("$name|$artist").lowercase()

/** Duración «3:07». */
fun fmtDur(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }

/** Saludo según la hora. */
fun saludo(hour: Int, name: String): String {
    val base = when { hour < 5 -> "Buenas noches"; hour < 12 -> "Buenos días"; hour < 19 -> "Buenas tardes"; else -> "Buenas noches" }
    return if (name.isBlank()) base else "$base, ${name.trim()}"
}

/** Modos de escucha de Luna (iguales a la web). */
object Modos {
    val NOMBRES = listOf("carretera", "hogar", "estudiando", "ejercicio", "videojuegos", "neutral")
    val ETIQUETA = mapOf("carretera" to "Carretera", "hogar" to "Hogar", "estudiando" to "Estudiando", "ejercicio" to "Ejercicio", "videojuegos" to "Videojuegos", "neutral" to "Neutral")
    val DESCRIPCION = mapOf(
        "neutral" to "Sin escenario particular: tono natural y cercano, sin mencionar viajes, casa, estudio, juegos ni ejercicio.",
        "carretera" to "Estás acompañando a la persona en un viaje por carretera. Habla del camino y del paisaje, con un tono cercano y amigable.",
        "hogar" to "La persona está en casa, relajada. Habla con calma, con un tono íntimo y cálido, como si estuvieran tomando un café.",
        "estudiando" to "La persona está estudiando o concentrada. Habla con un tono pausado, claro y muy conciso. Evita distraerla.",
        "videojuegos" to "La persona está jugando videojuegos. Usa un tono épico, motivador y con energía, como un comentarista.",
        "ejercicio" to "La persona está haciendo ejercicio. Habla con energía, motivación y ritmo rápido para mantenerla activa."
    )
    val PROHIBIDOS = mapOf(
        "carretera" to "carretera, camino, viaje, ruta, kilómetros, manejar", "hogar" to "la casa, el sillón, el café, el sofá",
        "estudiando" to "estudiar, exámenes, tareas", "videojuegos" to "videojuegos, misiones, niveles, partidas, jugar",
        "ejercicio" to "ejercicio, entrenar, correr, gimnasio"
    )
    val FRASES = mapOf(
        "neutral" to listOf("Oye, escucha esto", "Te cuento algo", "Mira lo que viene", "Fíjate en esto", "Y ahora, atención"),
        "carretera" to listOf("Que siga el camino", "Kilómetro a kilómetro", "Con la carretera por delante", "Buen viaje con esta"),
        "hogar" to listOf("Con calma, como en casa", "Ponte cómodo", "Para el sillón y el café"),
        "estudiando" to listOf("Para mantener el enfoque", "Sin distraerte", "Concentración activada"),
        "videojuegos" to listOf("Nueva misión musical", "Siguiente nivel", "Modo épico activado"),
        "ejercicio" to listOf("Vamos con toda la energía", "Un esfuerzo más", "Ritmo arriba")
    )
    fun valido(m: String?): String = if (m != null && m in NOMBRES) m else "carretera"
}
