package io.github.arielegno.radio.core

import java.text.Normalizer

/** Lo que se entiende de una frase dicha en el modo carro. */
sealed class VozCmd {
    object Siguiente : VozCmd()
    object Anterior : VozCmd()
    object Pausa : VozCmd()
    object Sigue : VozCmd()
    object Favorita : VozCmd()
    object SoloMusica : VozCmd()
    object Luna : VozCmd()
    object Salir : VozCmd()
    data class Poner(val ids: List<Long>, val etiqueta: String) : VozCmd()
    data class NoEncontrado(val busqueda: String) : VozCmd()
    data class NoEntendi(val frase: String) : VozCmd()
}

object Voz {
    fun norm(t: String): String =
        Normalizer.normalize(t.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[¿?¡!.,]"), " ").replace(Regex("\\s+"), " ").trim()

    private val PONER = Regex("(?:^|\\b)(?:pon(?:me)?|toca|reproduce|quiero oir|quiero escuchar)\\s+(?:algo\\s+|musica\\s+|canciones\\s+|una\\s+)?(?:de\\s+)?(.+)$")

    /** Mismo orden de reglas que la versión web. */
    fun entender(raw: String, biblioteca: List<Track>): VozCmd {
        val t = norm(raw)
        val m = PONER.find(t)
        if (m != null && !Regex("^(pausa|musica|la musica)$").matches(m.groupValues[1])) {
            val dicho = m.groupValues[1]; val q = normNombre(dicho)
            if (q.length < 2) return VozCmd.NoEntendi(raw)
            val porArtista = biblioteca.filter { normNombre(it.artist).contains(q) }
            val porCancion = biblioteca.filter { normNombre(it.name).contains(q) }
            val porAlbum = biblioteca.filter { normNombre(it.album).contains(q) }
            val set = when { porArtista.isNotEmpty() -> porArtista; porCancion.isNotEmpty() -> porCancion; else -> porAlbum }
            if (set.isEmpty()) return VozCmd.NoEncontrado(dicho)
            return VozCmd.Poner(set.map { it.id }, if (porArtista.isNotEmpty()) set[0].artist else dicho)
        }
        fun has(p: String) = Regex(p).containsMatchIn(t)
        return when {
            has("\\b(siguiente|otra|salta|cambia|adelante)\\b") -> VozCmd.Siguiente
            has("\\b(anterior|atras|regresa|la de antes)\\b") -> VozCmd.Anterior
            has("\\b(pausa|pausar|para|detente|deten|silencio)\\b") -> VozCmd.Pausa
            has("\\b(sigue|seguir|continua|play|reproduce|dale|reanuda)\\b") -> VozCmd.Sigue
            has("\\b(favorit|me gusta|guarda(la)?)") -> VozCmd.Favorita
            has("\\b(solo musica|calla(te)?|sin luna|sin locutora)\\b") -> VozCmd.SoloMusica
            has("\\b(sal(ir)?|cierra|cerrar) (el )?modo carro\\b") -> VozCmd.Salir
            has("\\b(luna|con radio|habla|locutora)\\b") -> VozCmd.Luna
            else -> VozCmd.NoEntendi(raw)
        }
    }

    fun respuesta(c: VozCmd): String = when (c) {
        VozCmd.Siguiente -> "Siguiente canción"
        VozCmd.Anterior -> "Canción anterior"
        VozCmd.Pausa -> "En pausa"
        VozCmd.Sigue -> "Sigue la música"
        VozCmd.Favorita -> "Marcada como favorita"
        VozCmd.SoloMusica -> "Solo música"
        VozCmd.Luna -> "Luna activa"
        VozCmd.Salir -> "Saliendo del modo carro"
        is VozCmd.Poner -> "Poniendo ${c.etiqueta}…"
        is VozCmd.NoEncontrado -> "No encontré «${c.busqueda}» en tu biblioteca."
        is VozCmd.NoEntendi -> "No entendí «${c.frase}». Prueba: «siguiente», «pausa» o «pon Soda Stereo»."
    }
}
