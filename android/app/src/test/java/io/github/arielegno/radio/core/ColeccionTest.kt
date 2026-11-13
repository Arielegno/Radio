package io.github.arielegno.radio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColeccionTest {
    private val lib = listOf(
        Track(1, "Persiana americana", "Soda Stereo", "Signos", 1986, "Rock", 291_000, "1.mp3", "audio/mpeg", addedAt = 10),
        Track(2, "De música ligera", "Soda Stereo", "Canción Animal", 1990, "rock, pop", 213_000, "2.flac", "audio/flac", addedAt = 30),
        Track(3, "Labios rotos", "Zoé", "Memo Rex Commander", 2006, "", 217_000, "3.m4a", "audio/mp4", addedAt = 20),
        Track(4, "Sin datos", "Artista desconocido", file = "4.mp3")
    )

    @Test fun limpia_nombres_como_la_web() {
        assertEquals("Café Tacvba", Limpieza.artista("Café Tacvba VEVO"))
        assertEquals("Carlos Sadness", Limpieza.artista("CarlosSadness - Topic"))
        assertEquals("Zoé", Limpieza.artista("Zoé Oficial"))
        assertEquals("Soda Stereo", Limpieza.artista("Soda Stereo"))
        assertEquals("Labios rotos", Limpieza.titulo("Zoé - Labios Rotos (Video Oficial)".replace("Rotos", "rotos"), "Zoé"))
        assertEquals("Persiana americana", Limpieza.titulo("Persiana americana - Soda Stereo", "Soda Stereo"))
        assertEquals("Eternamente", Limpieza.titulo("Eternamente (Official Music Video)", "Aczino"))
        assertEquals("Estocolmo", Limpieza.titulo("Estocolmo  Video Oficial", "ARAWATO"))
        assertEquals("Bang Bang - Nancy Sinatra", Limpieza.titulo("Bang Bang - Nancy Sinatra", "713blackjack"))
        assertEquals("Soda Stereo", Limpieza.artistaPrincipal("Soda Stereo feat. Andrea Echeverri"))
    }

    @Test fun junta_albumes_casi_iguales() {
        val a = Coleccion.albumBase("Ciencias Celestes")
        assertEquals(a, Coleccion.albumBase("Ciencias Celeste"))
        assertEquals(Coleccion.albumBase("Melophobia"), Coleccion.albumBase("Melophobia (Deluxe Edition)"))
        // los que difieren en un número son discos distintos
        org.junit.Assert.assertNotEquals(Coleccion.albumBase("Greatest Hits 2"), Coleccion.albumBase("Greatest Hits 3"))
    }

    @Test fun completa_con_itunes_y_musicbrainz() {
        val it = """{"results":[{"trackName":"Labios Rotos","artistName":"Zoé","collectionName":"Memo Rex Commander y el Corazón Atómico de la Vía Láctea","releaseDate":"2006-03-21T08:00:00Z","artworkUrl100":"https://is1.mzstatic.com/x/100x100bb.jpg"}]}"""
        val d = Limpieza.deItunes(it, "Labios rotos", "Zoé")!!
        assertEquals(2006, d.anio); assertEquals("https://is1.mzstatic.com/x/600x600bb.jpg", d.portada)
        assertNull(Limpieza.deItunes(it, "Otra", "Zoé"))
        val mb = """{"recordings":[{"score":100,"title":"Persiana Americana","first-release-date":"1986-11-10","artist-credit":[{"name":"Soda Stereo"}],
          "releases":[{"id":"c1","title":"Me Verás Volver","status":"Official","date":"2007","release-group":{"primary-type":"Album","secondary-types":["Live"]}},
                      {"id":"s1","title":"Signos","status":"Official","date":"1986-11-10","release-group":{"primary-type":"Album"}}]}]}"""
        val m = Limpieza.deMusicBrainz(mb, "Persiana americana", "Soda Stereo")!!
        assertEquals("Signos", m.album); assertEquals(1986, m.anio); assertEquals("https://coverartarchive.org/release/s1/front-500", m.portada)
        // Aplica solo lo que falta y respeta lo editado a mano
        val sinAlbum = Track(9, "Persiana americana", "Soda Stereo", file = "9.mp3")
        assertEquals(Pair("Signos", 1986), Limpieza.aplicar(sinAlbum, m).let { it.album to it.year })
        val malAlbum = sinAlbum.copy(album = "Soda Stereo Canal", year = 0)
        assertEquals("Signos", Limpieza.aplicar(malAlbum, m).album)
        val aMano = malAlbum.copy(manual = setOf("album"))
        assertEquals("Soda Stereo Canal", Limpieza.aplicar(aMano, m).album)
        // lo corregido a mano se respeta siempre, incluso al forzar
        assertEquals("Soda Stereo Canal", Limpieza.aplicar(aMano, m, forzar = true).album)
        // sin permiso para cambiar álbumes: solo se llena uno vacío, nunca se mueve la canción de álbum
        assertEquals("Soda Stereo Canal", Limpieza.aplicar(malAlbum, m, cambiarAlbum = false).album)
        assertEquals("Signos", Limpieza.aplicar(sinAlbum, m, cambiarAlbum = false).album)
        // colaboraciones con «/» y « x » van con el artista principal
        assertEquals("Carlos Sadness", Limpieza.artistaPrincipal("Carlos Sadness / Bomba Estéreo"))
        assertEquals("Bad Bunny", Limpieza.artistaPrincipal("Bad Bunny x Jhay Cortez"))
        assertEquals("Maxwell", Limpieza.artistaPrincipal("Maxwell"))
        assertEquals("Signos", Limpieza.aplicar(sinAlbum.copy(album = "Signos"), m).album)
        assertEquals("Soda Stereo", Limpieza.nombreOficial("""{"artists":[{"score":100,"name":"Soda Stereo"}]}""", "soda stereo"))
        assertNull(Limpieza.nombreOficial("""{"artists":[{"score":100,"name":"Gustavo Cerati"}]}""", "soda stereo"))
    }

    @Test fun vistas_orden_y_filtros() {
        val art = Coleccion.agrupar(lib, "artistas")
        assertEquals(listOf("Soda Stereo", "Zoé", "Artista desconocido"), art.map { it.clave })
        assertEquals(listOf("2006", "1990", "1986", "Año desconocido"), Coleccion.agrupar(lib, "anios").map { it.clave })
        assertEquals(listOf("Rock", "Otros géneros"), Coleccion.agrupar(lib, "generos").map { it.clave })
        assertEquals(2, Coleccion.agrupar(lib, "generos")[0].canciones.size)
        val alb = Coleccion.agrupar(lib, "albumes"); assertEquals("Soda Stereo", alb.first { it.clave == "Signos" }.subtitulo)
        assertEquals(listOf("Rock"), Coleccion.generos(lib))
        assertEquals(listOf(3L, 2L, 1L, 4L), Coleccion.ordenar(lib, "nuevo") { 0 }.map { it.id })
        assertEquals(listOf(1L, 2L, 3L, 4L), Coleccion.ordenar(lib, "viejo") { 0 }.map { it.id })
        assertEquals(listOf(2L, 3L, 1L, 4L), Coleccion.ordenar(lib, "recientes") { 0 }.map { it.id })
        assertEquals(listOf(3L, 1L, 2L, 4L), Coleccion.ordenar(lib, "escuchadas") { if (it.id == 3L) 9 else if (it.id == 1L) 2 else 0 }.map { it.id })
        val f = { x: Coleccion.Filtros -> lib.filter { Coleccion.pasa(it, x) { t -> t.id == 3L } }.map { it.id } }
        assertEquals(listOf(1L), f(Coleccion.Filtros(decada = "1980")))
        assertEquals(listOf(4L), f(Coleccion.Filtros(decada = "sin")))
        assertEquals(listOf(2L), f(Coleccion.Filtros(calidad = "hq")))
        assertEquals(listOf(1L, 2L), f(Coleccion.Filtros(genero = "rock")))
        assertEquals(listOf(3L), f(Coleccion.Filtros(favoritas = true)))
        assertEquals(2, Coleccion.Filtros(decada = "2000", favoritas = true).activos)
        assertTrue(Coleccion.buscar(lib[1], "cancion animal")); assertFalse(Coleccion.buscar(lib[1], "zoe"))
    }

    @Test fun playlists_compatibles_con_la_web() {
        val p = Playlists("""{"Viaje al mar":{"canciones":[3,1,3]},"__proto__":{"canciones":[1]}}""")
        assertEquals(listOf("Viaje al mar"), p.nombres); assertEquals(listOf(3L, 1L), p.canciones("Viaje al mar"))
        assertEquals("Ya existe una playlist con ese nombre.", p.crear("viaje al MAR"))
        assertNull(p.crear("Fiesta")); assertTrue(p.agregar("Fiesta", 2)); assertFalse(p.agregar("Fiesta", 2))
        p.mover("Viaje al mar", 1, -1); assertEquals(listOf(1L, 3L), p.canciones("Viaje al mar"))
        assertNull(p.renombrar("Fiesta", "Fiesta 2026")); assertEquals(listOf("Viaje al mar", "Fiesta 2026"), p.nombres)
        p.quitarDeTodas(setOf(3L)); assertEquals(listOf(1L), p.canciones("Viaje al mar"))
        val q = Playlists(p.toJson()); assertEquals(p.toJson(), q.toJson())
        assertTrue(p.toJson().contains("\"canciones\":[2]"))
    }

    @Test fun cancion_guarda_lo_editado_a_mano() {
        val t = lib[0].copy(manual = setOf("name", "album"), revisada = true)
        val u = Track.from(Json.obj(Json.write(t.toMap())))!!
        assertEquals(t, u); assertTrue(lib[1].sinPerdida); assertFalse(lib[0].sinPerdida)
    }

    @Test fun mas_escuchadas_por_ubicacion() {
        val e = Estadisticas(); val hoy = java.time.LocalDate.of(2026, 11, 8)
        repeat(3) { e.countPlay("Persiana americana", "Soda Stereo", "Ciudad Centro", null, null, hoy) }
        e.countPlay("Labios rotos", "Zoé", "Ciudad Centro", null, null, hoy)
        e.countPlay("Labios rotos", "Zoé", "Ciudad Norte", null, null, hoy)
        e.countPlay("Labios rotos", "Zoé", null, null, null, hoy)
        val u = e.porUbicacion(2)
        assertEquals(listOf("Ciudad Centro", "Ciudad Norte"), u.map { it.first })
        assertEquals(Triple("Persiana americana", "Soda Stereo", 3), u[0].second[0])
    }
}

class LunaExtraTest {
    @Test fun estilo_contexto_y_claude() {
        val s = Luna.sistema(LunaAjustes(estilo = "noche", contexto = "Rock latino de los 80"), true, 22)
        org.junit.Assert.assertTrue(s.contains("radio FM nocturna") && s.contains("Rock latino de los 80"))
        val b = Json.obj(Luna.peticionClaude("claude-sonnet-5-5", "sis", "usr"))
        assertEquals("claude-sonnet-5-5", b.str("model")); assertEquals("usr", (b.list("messages")[0] as Map<*, *>)["content"])
        assertEquals("Mira, Soda.", Luna.textoClaude("""{"content":[{"type":"text","text":"**Mira**, Soda."}]}"""))
        assertNull(Luna.textoClaude("""{"content":[]}"""))
    }

    @Test fun bio_de_wikipedia() {
        val busq = """{"query":{"search":[{"title":"Soda (bebida)","snippet":"refresco"},{"title":"Soda Stereo","snippet":"fue una <b>banda</b> argentina de rock"}]}}"""
        assertEquals("Soda Stereo", Bio.elegir(busq, "soda stereo"))
        val res = """{"type":"standard","title":"Soda Stereo","description":"banda argentina de rock","extract":"Soda Stereo fue una banda argentina de rock formada en Buenos Aires en 1982.","originalimage":{"source":"https://upload.wikimedia.org/x.jpg"},"content_urls":{"mobile":{"page":"https://es.m.wikipedia.org/wiki/Soda_Stereo"}}}"""
        val f = Bio.ficha(res, "Soda Stereo")!!
        assertEquals("https://upload.wikimedia.org/x.jpg", f.foto); org.junit.Assert.assertTrue(f.texto.startsWith("Soda Stereo fue"))
        assertNull(Bio.ficha("""{"type":"standard","title":"Soda Stereo","description":"bebida","extract":"Una bebida gaseosa muy popular en todo el mundo desde hace siglos."}""", "Soda Stereo"))
        assertNull(Bio.ficha("""{"type":"disambiguation","title":"Soda Stereo"}""", "Soda Stereo"))
    }
}

class MigrarTest {
    @Test fun escuchas_y_favorita_siguen_al_nombre_nuevo() {
        val e = Estadisticas(); val hoy = java.time.LocalDate.of(2026, 11, 8)
        repeat(2) { e.countPlay("Labios Rotos (Video Oficial)", "Zoé VEVO", "Ciudad", 1.0, 2.0, hoy) }
        e.toggleFav("Labios Rotos (Video Oficial)", "Zoé VEVO")
        e.migrar("Labios Rotos (Video Oficial)", "Zoé VEVO", "Labios Rotos", "Zoé")
        assertEquals(2, e.stats[keyOf("Labios Rotos", "Zoé")]!!.c)
        org.junit.Assert.assertTrue(e.isFav("Labios Rotos", "Zoé")); org.junit.Assert.assertFalse(e.isFav("Labios Rotos (Video Oficial)", "Zoé VEVO"))
        val r = e.resumen("month", hoy)!!
        assertEquals("Zoé" to 2, r.topArtistas(1)[0]); assertEquals(("Labios Rotos" to "Zoé") to 2, r.topCanciones(1)[0])
        assertEquals("Labios Rotos — Zoé" to 2, e.puntosMapa()[0].canciones[0])
    }
}
