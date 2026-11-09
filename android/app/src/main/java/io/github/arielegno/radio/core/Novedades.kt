package io.github.arielegno.radio.core

/** Historial de cambios (se muestra en Ajustes › Acerca de › Novedades). Lo más nuevo arriba. */
object Novedades {
    val LISTA: List<Pair<String, List<String>>> = listOf(
        "2.0.2" to listOf(
            "Reportes de errores más completos: incluyen el detalle del error y las últimas líneas del registro de Android.",
            "Un error en una tarea de fondo ya no cierra la app: queda anotado en los reportes.",
        ),
        "2.0.1" to listOf(
            "Arreglado: la app se cerraba al probar las voces de la app (Daniela, Ald). Ahora la voz se genera frase por frase.",
            "Los reportes de errores dicen qué estaba haciendo la app justo antes de cerrarse.",
        ),
        "2.0" to listOf(
            "Reportes de errores: si la app se cierra sola, al abrirla otra vez puedes compartir qué pasó (también en Acerca de).",
            "Arreglado: la app se cerraba al tocar «Probar» en las voces de la app.",
            "Biografías, fichas, fotos y datos de la guía se guardan aparte y para siempre: la app ya no se traba con bibliotecas grandes.",
            "Biblioteca más rápida: menos lecturas y escrituras, imágenes más livianas y búsqueda de canciones al instante.",
            "«Limpiar y completar» solo revisa las canciones a las que les falta algo (y sigue donde quedó si lo cancelas).",
            "Luna guarda sus comentarios y los reutiliza: menos espera y menos gasto de tu IA.",
            "Copia ordenada: guarda tu música en una carpeta, ordenada por artista y álbum, con los nombres ya limpios.",
            "Novedades: este historial de cambios.",
        ),
        "1.9.1" to listOf("Luna habla encima de la música bajita, como en la radio (se puede apagar y elegir el volumen)."),
        "1.9" to listOf(
            "Dos locutores: Luna y un compañero conversan entre canciones, cada uno con su voz.",
            "Efectos de voz: Locutor FM, Teléfono, Eco de estadio y Walkie-talkie.",
            "La Revista lee los artículos con las voces de la app.",
            "Menos pausas al leer lo que escribe la IA.",
            "La biblioteca ya no salta arriba mientras se agregan canciones.",
        ),
        "1.8" to listOf("Voces propias sin internet ni SherpaTTS: Daniela (Argentina) y Ald (México)."),
        "1.7" to listOf(
            "Editor de fotos de la Revista: rotar, voltear, zoom, formatos, filtros y ajustes.",
            "Colaboraciones agrupadas con el artista principal.",
            "Al elegir otra vez la misma carpeta solo entran las canciones nuevas.",
            "La guía busca por título cuando el artista viene mal y usa tu IA como último recurso.",
            "Botón «Ir al artista» dentro de cada álbum.",
            "Voces de SherpaTTS sin sonido metálico; la voz trabada ya no detiene la radio.",
            "Se leen todas las subcarpetas al agregar música.",
        ),
        "1.6" to listOf("Revista nativa (beta) dentro de la app: chats, buscador, sugerencias, listas, editar perfil y más."),
        "1.5.1" to listOf("La radio se pausa mientras ves un video de la Bóveda (ya no se mezclan los audios)."),
        "1.5" to listOf("Saludo con banners de estilos (punk, Y2K, grunge…), ubicación solo por ciudad, «Artistas más vistos»."),
        "1.4" to listOf("Arreglo de la conexión con Spotify; secciones de la Revista en la página del artista."),
        "1.3" to listOf("Página de artista como en la web; cambiar portadas de los álbumes."),
    )

    /** El mismo historial como texto (para CHANGELOG o para compartir). */
    fun texto(): String = LISTA.joinToString("\n\n") { (v, l) -> "Versión $v\n" + l.joinToString("\n") { "• $it" } }
}
