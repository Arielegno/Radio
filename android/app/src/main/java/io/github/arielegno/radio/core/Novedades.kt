package io.github.arielegno.radio.core

/** Historial de cambios (se muestra en Ajustes › Acerca de › Novedades). Lo más nuevo arriba. */
object Novedades {
    val LISTA: List<Pair<String, List<String>>> = listOf(
        "2.1.3" to listOf(
            "Revista: los nombres largos ya no se salen de su casilla. En un artículo, el botón «Seguir» queda siempre dentro aunque el autor tenga un nombre muy largo.",
            "Revista: nombres en perfiles, comentarios, búsqueda de artistas y panel de administración se recortan con «…» en vez de desbordarse.",
        ),
        "2.1.2" to listOf(
            "Pantalla más fluida: las portadas y fotos de artistas ya no leen el disco mientras se dibuja la pantalla (eso podía trabarla en teléfonos ocupados).",
            "Las fotos de artistas se buscan siempre en segundo plano.",
            "Al cambiar una carátula o foto, se ve la nueva al instante.",
        ),
        "2.1.1" to listOf(
            "La app abre más rápido después de un cierre: la revisión de qué pasó se hace en segundo plano (antes podía causar «no responde» al abrir).",
            "Revista: en la computadora, «Compartir» ahora sí copia el enlace (antes fallaba en silencio en algunos navegadores).",
            "Revista: la tecla Esc funciona desde que se abre (pedía el foco antes de tiempo).",
            "Pruebas automáticas de estrés: la app se usa a lo loco en un teléfono virtual (con Luna hablando) y la Revista en Chromium y Firefox, buscando cierres y errores.",
        ),
        "2.1" to listOf(
            "Arreglado: Luna a veces se quedaba callada o se saltaba un comentario de por medio cuando hablaba encima de la música.",
            "Arreglado: agregar música, «Limpiar y completar» y descargar voces se cortaban si te ibas a otra pestaña (la barra se quedaba congelada en un número). Ahora siguen trabajando.",
            "Arreglado: al eliminar un artista o álbum, a veces seguían apareciendo sus canciones.",
            "Solo se agrega música de una vez: tocar «Agregar» dos veces ya no hace que se pisen y se pierdan canciones.",
            "Saltar canciones seguido ya no deja a la voz preparando comentarios que no se van a decir.",
            "Tras pausar a Luna, cambiar de canción o volver a empezar la radio ya no deja la música detenida.",
        ),
        "2.0.9" to listOf(
            "Cambiar portada de un álbum: eliges si va «solo para el álbum» o para «el álbum y todas sus canciones» (también se ve en la radio).",
            "Biblioteca más fluida: la lista ya no se reordena entera cada vez que escuchas una canción nueva, y los cuadros de álbum calculan su portada, artista y año una sola vez.",
            "Las portadas que ya estaban en memoria aparecen al instante al desplazarte (sin parpadeo).",
            "El buscador de la biblioteca ya no corta el texto a la mitad en pantallas angostas.",
        ),
        "2.0.8" to listOf(
            "Arreglado: las canciones, artistas o álbumes que eliminaste ya se pueden volver a agregar (antes decía que «ya estaban»).",
        ),
        "2.0.7" to listOf(
            "Pausa de verdad mientras Luna habla: calla a Luna y también detiene la canción (antes la música seguía).",
            "Spotify: si falta conectar tu cuenta, al darle play se abre directo la página para entrar (ya no solo dice «conecta Spotify»).",
            "Spotify: avisa si lo pegado no es un Client ID (por ejemplo, el «Client secret») y muestra en la radio si la conexión salió bien o qué falló.",
        ),
        "2.0.6" to listOf(
            "Al editar una canción o el nombre de un álbum, salen de sugerencia los álbumes de ese artista que ya tienes y sus álbumes oficiales según internet (con el año): un toque y listo.",
            "Eliminar un artista o un álbum completo (botón de basurero al abrirlo). Tus archivos originales no se tocan.",
            "Cambiar el nombre de un álbum en todas sus canciones, o juntarlo con otro álbum del mismo artista con un toque (lápiz al abrir el álbum).",
        ),
        "2.0.5" to listOf(
            "Luna habla al instante y sin pausas: con las voces de la app, su voz se prepara mientras suena la canción.",
            "Dos locutores: Luna ya no lee la parte de Leo; cada uno habla con su voz aunque la IA escriba todo seguido, y en pantalla se ve cada uno en su renglón.",
            "La música baja de verdad mientras Luna habla (el nivelador de volumen ya no la vuelve a subir) y empieza bajita enseguida, sin silencio largo.",
            "Álbumes: las canciones ya no se van a recopilatorios (Promo Only, Spotify Singles, radios en vivo…); la guía busca el álbum original y une «Deluxe», «Remastered» y similares con su álbum.",
            "Las canciones que ya estaban en recopilatorios se revisan solas una vez para volver a su álbum real.",
            "Agregar muchas canciones ya no se queda trabado: un archivo dañado o lento se salta, y el avance se guarda cada 300 canciones.",
        ),
        "2.0.4" to listOf(
            "Arreglado: la app se trababa («no responde») al abrir la Biblioteca o al empezar la radio con muchas canciones.",
            "Ordenar, buscar y agrupar la biblioteca es mucho más rápido: los nombres limpios se calculan una sola vez.",
        ),
        "2.0.3" to listOf(
            "Arreglado: la app se cerraba al poner la radio. Las listas de reproducción (.m3u, .m3u8) se habían metido en la biblioteca como si fueran canciones.",
            "Esas listas se quitan solas de tu biblioteca y ya no entran al agregar carpetas (tus canciones no se tocan).",
            "Si una canción no se puede poner, la radio avisa en lugar de cerrarse.",
        ),
        "2.0.2" to listOf(
            "Reportes de errores más completos: incluyen el detalle del error y las últimas líneas del registro de Android.",
            "Un error en una tarea de fondo ya no cierra la app: queda anotado en los reportes.",
            "Botón «Compartir todos»: manda todos los reportes juntos de una vez (se guardan los últimos 30).",
            "Cuando Android pide memoria, la app suelta portadas y la voz que no está hablando (antes la cerraba por falta de memoria).",
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
