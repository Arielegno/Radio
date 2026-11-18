package com.hilo.lector.data

/** Qué cambió en cada versión de Hilo. La primera es la más nueva. */
object Changelog {
    class Version(val code: Int, val name: String, val date: String, val title: String, val added: List<String>, val fixed: List<String>)

    val versions = listOf(
        Version(
            17, "1.7", "18 nov 2026", "Leer como en un libro",
            added = listOf(
                "EPUB, MOBI, Word y los demás formatos se ven como la página de un libro: texto justificado, guiones al cortar palabras, sangría y sin huecos entre párrafos. Puedes volver a los párrafos separados en Ajustes → Forma del texto.",
                "Fuentes nuevas: Literata, Comfortaa, Lexend y Atkinson Hyperlegible (pensada para leer con menos esfuerzo). Cada una se muestra con su propia letra.",
                "Grosor de la letra: de fina a gruesa.",
                "Libros MOBI y AZW3 de Kindle (sin DRM).",
                "Dos páginas con el celular acostado también en el texto (EPUB, MOBI, Word…).",
                "PDF sin franjas negras: el fondo toma el color del papel y «Recortar márgenes» quita el borde blanco para que la letra se vea más grande.",
                "Biblioteca en lista, como ReadEra: portada, formato, tamaño, avance y botones rápidos. La vista de portadas sigue en Ajustes → Vista de la biblioteca.",
                "Elegir varios libros: mantén el dedo sobre uno y marca otros para compartirlos, pasarlos a favoritos, a una colección, marcarlos como leídos o mandarlos a la papelera.",
                "Carpetas que no leer: en Menú → Carpetas apaga, por ejemplo, la carpeta de WhatsApp. Sus documentos salen de Hilo sin borrarse del celular.",
                "Este apartado de Novedades.",
            ),
            fixed = listOf(
                "Los libros cuyo nombre era un número (como «4 57714757040215178 40») toman el título guardado dentro del PDF.",
                "La primera búsqueda en el celular ya no marca cientos de documentos como «Nuevo». Si ya los tienes así, en Nuevos toca «Marcar todos como vistos».",
                "El texto en páginas ya no corta un párrafo en dos trozos distintos: la línea siguiente continúa en la otra página con el mismo formato.",
                "Ajustes de lectura ordenados por secciones: Página, Letra, Ayudas, PDF, Biblioteca y Toques.",
            ),
        ),
        Version(
            16, "1.6", "17 nov 2026", "Más estable y más rápido",
            added = listOf("Pruebas automáticas de depuración y de estrés con documentos enormes, miles de tarjetas y miles de toques al azar."),
            fixed = listOf(
                "Una oración con una lista muy larga de artículos («artículos 1, 2, 3…») cerraba la app.",
                "En Android 11 o más nuevo, el sistema bloqueaba la voz: la lectura en voz alta y el examen oral podían no funcionar.",
                "Al analizar libros muy grandes la pantalla se trababa unos segundos.",
                "El análisis de un libro enorme pasó de no terminar a tardar unos 9 segundos; un PDF de 400 páginas, unos 23.",
                "Guardar la biblioteca, crear muchas tarjetas de una vez y desplazar el texto ya no traban la pantalla.",
                "El plan de examen ya no tapa las secciones de la sala de estudio.",
            ),
        ),
        Version(
            15, "1.5", "17 nov 2026", "Derecho y Sociología",
            added = listOf(
                "Leyes y códigos: índice por libros, títulos y capítulos, «Ir al artículo», artículos enlazados entre sí y tarjetas de artículos.",
                "Tabla de plazos de la ley.",
                "Autores con sus conceptos y cuadro comparativo entre autores.",
                "Línea de tiempo con las fechas del texto.",
                "Fichas de caso, de lectura y de autor.",
                "Citas en formato APA con su página.",
                "Modo Feynman (explícalo con tus palabras) y examen oral.",
                "Fecha de examen con plan diario, recordatorio diario, exportar a Anki y copia de seguridad.",
                "Mapa de conceptos entre tus documentos y diccionario jurídico de la RAE.",
            ),
            fixed = emptyList(),
        ),
        Version(
            14, "1.4", "17 nov 2026", "Entender el texto",
            added = listOf(
                "Nube de palabras flotante: quita las que no importan.",
                "Glosario automático, mapa de conceptos y guía de estudio con preguntas por capítulo.",
                "Buscar una palabra en el diccionario, en Wikipedia o en el traductor.",
            ),
            fixed = listOf("El mapa de conceptos ya no amontona los círculos."),
        ),
        Version(
            13, "1.3", "17 nov 2026", "Sala de estudio",
            added = listOf(
                "Tarjetas con repaso espaciado (como Anki) para cada documento.",
                "Resaltados, apuntes, buscar en el libro y artículos de leyes.",
                "Hilo encuentra solo los documentos del celular, como ReadEra.",
            ),
            fixed = listOf("El texto en páginas a veces se quedaba en «Armando las páginas…»."),
        ),
        Version(
            12, "1.2", "17 nov 2026", "Como un lector de verdad",
            added = listOf(
                "Pasar páginas en horizontal, como un libro.",
                "Modos de color: Día, Noche, Sepia, Crepúsculo, Consola y más.",
                "Marcadores, índice, voz, pantalla completa y brillo deslizando.",
                "Biblioteca con secciones y formatos nuevos: Word, ODT, FB2, HTML y Markdown.",
            ),
            fixed = emptyList(),
        ),
        Version(
            11, "1.1", "16 nov 2026", "Detalles",
            added = emptyList(),
            fixed = listOf(
                "Los títulos ya no se pegan al primer párrafo en el modo Texto.",
                "Los textos cortos ya no aparecen como «Terminado» sin leerlos.",
                "La barra para anotar el PDF entra completa en la pantalla.",
            ),
        ),
        Version(
            10, "1.0", "16 nov 2026", "Hilo nace",
            added = listOf(
                "Colores que pasan de una línea a la siguiente para no perder el hilo, y la regla de lectura.",
                "PDF con su formato original y anotaciones encima: resaltar, lápiz y notas.",
                "Modo Texto con interlineado compacto, negrita guía y palabra a palabra.",
                "Temporizador de lectura.",
            ),
            fixed = emptyList(),
        ),
    )

    val current get() = versions.first()
}
