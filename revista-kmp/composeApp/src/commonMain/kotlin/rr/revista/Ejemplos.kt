package rr.revista

/** Artículos de ejemplo (no están en la base de datos) para ver cómo luce la Revista cuando todavía no hay artículos. */
object Ejemplos {
    private var cache: List<Articulo>? = null

    suspend fun lista(): List<Articulo> {
        cache?.let { return it }
        val fotos = runCatching { Supa.posters() }.getOrDefault(emptyList()).map { it.grande }
        val f0 = fotos.getOrElse(0) { "" }; val f1 = fotos.getOrElse(1) { f0 }
        val autor = Autor("", "Ejemplo · Radio de ruta", "")
        val hoy = ahoraIso()
        val l = listOf(
            Articulo("ejemplo-1", "Punks not dead: el fanzine vuelve", "Cómo se ve un artículo con «portada de revista»", f0,
                Estilo.de(mapOf("th" to "punk", "ft" to "poster", "ac" to "rojo", "cv" to "m", "stk" to "¡Nuevo!", "mh" to "Radio de ruta", "dc" to 1L)), hoy, autor, 128, 14,
                texto = "Este es un **artículo de ejemplo** para que veas cómo luce la estética *Fanzine punk* con portada de revista: cabecera, franja, sticker y código de barras.\n\n## Lo que vas a encontrar\n\n- Reseñas de discos con nota\n- Listas de canciones que suenan en tu biblioteca\n- Historias de conciertos\n\n> La música no se escucha: se vive en la carretera.\n\n1977: Nace el fanzine en fotocopias\n1991: El grunge llena las paredes de carteles\n2026: La Revista de Radio de ruta",
                canciones = listOf("Iowa" to "Slipknot", "Smells Like Teen Spirit" to "Nirvana"), etiquetas = listOf("punk", "fanzine", "ejemplo")),
            Articulo("ejemplo-2", "No future", "Diseño avanzado: fondo grunge, panel oscuro y collage", "",
                Estilo.de(mapOf("th" to "asfalto", "ft" to "bloque", "ac" to "rosa", "al" to "c", "sz" to "g")), hoy, autor, 64, 7,
                texto = "Así se ve un artículo con **diseño avanzado**: el fondo queda fijo detrás mientras bajas, el texto va sobre un panel transparente y el collage lleva recortes, letras de revista, cinta y stickers.\n\nTodo esto se arma en la web con el *modo collage* y aquí se ve igual, dibujado por la versión nativa.\n\n> Pega, recorta, gira. La portada es tuya.",
                diseno = Diseno.de(mapOf("bg" to "grunge", "panel" to "negro", "pop" to 78L, "tf" to "mono", "hf" to "archivo", "vin" to 1L, "cpos" to "fondo",
                    "col" to listOf(
                        mapOf("k" to "img", "s" to f0, "x" to 30L, "y" to 22L, "w" to 52L, "r" to -7L, "z" to 2L, "cut" to "roto"),
                        mapOf("k" to "img", "s" to f1, "x" to 74L, "y" to 26L, "w" to 40L, "r" to 8L, "z" to 3L, "cut" to "polaroid"),
                        mapOf("k" to "txt", "t" to "NO FUTURE", "v" to "recorte", "x" to 50L, "y" to 6L, "w" to 40L, "r" to -3L, "z" to 5L),
                        mapOf("k" to "stk", "v" to "estallido", "t" to "¡Nuevo!", "x" to 86L, "y" to 44L, "w" to 22L, "r" to 12L, "z" to 6L, "c" to "#f2c230"),
                        mapOf("k" to "tape", "v" to "rosa", "x" to 74L, "y" to 12L, "w" to 22L, "r" to -18L, "z" to 4L),
                        mapOf("k" to "shp", "v" to "trama", "x" to 12L, "y" to 88L, "w" to 50L, "z" to 1L, "c" to "#ff3d8b"),
                        mapOf("k" to "stk", "v" to "rayo", "x" to 90L, "y" to 92L, "w" to 14L, "r" to -10L, "z" to 6L, "c" to "#f2c230")
                    )))),
            Articulo("ejemplo-3", "El disco que suena en la ruta", "Reseña con la estética Tokio kawaii", f0,
                Estilo.de(mapOf("th" to "tokio", "ft" to "burbuja", "ac" to "rosa", "cv" to "a")), hoy, autor, 31, 3,
                texto = "Una **reseña de ejemplo** con la estética *Tokio kawaii*: título con contorno y sombra, fondo a rayas suaves y la nota del disco arriba.\n\n## Lo mejor\n\n- Las guitarras del inicio\n- Que dura lo justo para un viaje corto\n\n> Para escuchar con las ventanas abajo.",
                resena = Resena("Disco de ejemplo", "Artista de ejemplo", 2026, 9), etiquetas = listOf("reseña", "ejemplo"))
        )
        cache = l; return l
    }

    suspend fun uno(id: String): Articulo? = lista().firstOrNull { it.id == id }
}
