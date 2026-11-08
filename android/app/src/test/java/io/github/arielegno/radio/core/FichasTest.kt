package io.github.arielegno.radio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FichasTest {
    @Test fun slug_como_la_web() {
        assertEquals("cafe-tacvba", Fichas.slug("Café Tacvba")); assertEquals("bad-bunny", Fichas.slug(" Bad Bunny! "))
    }

    @Test fun artista_de_la_nube_y_de_la_ia() {
        val nube = """{"nombre":"Café Tacvba","biografia_larga":"Banda de rock de Ciudad Satélite.","generos":["Rock alternativo"],"formacion":1989,
          "miembros":[{"nombre":"Joselo Rangel","rol":"Guitarrista","activo":true,"otros_proyectos":["JLS"]},"Rubén Albarrán"],
          "festivales":[{"festival":"Vive Latino","dato":"Cerraron en 2018"},"Coachella (2015)"],"datos_curiosos":["El nombre viene de un café"],"bandcampUrl":"https://x.bandcamp.com"}"""
        val a = Fichas.artistaDeNube(nube)!!
        assertEquals("1989", a.formacion); assertEquals(listOf("Joselo Rangel", "Rubén Albarrán"), a.miembros.map { it.nombre })
        assertEquals("Guitarrista", a.miembros[0].datos["rol"]); assertEquals(Fichas.Festival("Vive Latino", "vive-latino", "Cerraron en 2018"), a.festivales[0])
        assertEquals("Coachella (2015)", a.festivales[1].nombre); assertEquals("https://x.bandcamp.com", a.bandcamp); assertEquals("nube", a.fuente)
        val ia = Fichas.artistaDeIA("```json\n{\"bio\":\"Cantante puertorriqueño.\",\"generos\":[\"Reguetón\",null],\"formacion\":\"2013\",\"separacion\":null,\"miembros\":[\"Benito Antonio Martínez Ocasio\"],\"album_debut\":\"X 100pre\",\"obra_maestra\":\"Un Verano Sin Ti\",\"festivales\":[\"Coachella (2023)\"]}\n```")!!
        assertEquals("", ia.separacion); assertEquals("X 100pre", ia.debut); assertEquals(listOf("Reguetón"), ia.generos); assertEquals("Coachella (2023)", ia.festivales[0].nombre)
        assertNull(Fichas.artistaDeIA("no sé")); assertNull(Fichas.artistaDeNube("{}"))
    }

    @Test fun ficha_de_integrante_y_festival() {
        val w = Fichas.wiki("""{"type":"standard","title":"Joselo Rangel","description":"músico mexicano","extract":"José Alfredo Rangel Arroyo es un músico y escritor mexicano.","thumbnail":{"source":"https://upload.wikimedia.org/a/320px-j.jpg"},"content_urls":{"mobile":{"page":"https://es.m.wikipedia.org/wiki/Joselo_Rangel"}}}""")!!
        assertEquals("https://upload.wikimedia.org/a/960px-j.jpg", w.imagen); assertTrue(Fichas.esMusico(w, "Café Tacvba"))
        val obj = mapOf<String, Any?>("rol" to "Guitarrista", "activo" to true)
        assertTrue(Fichas.miembroIncompleto(obj))
        val m = Fichas.miembro(obj, w, mapOf("periodo" to "1989–actualidad", "otros_proyectos" to listOf("JLS"), "curiosidad" to "Escribe libros."))
        assertEquals(listOf("Guitarrista", "En la banda", "1989–actualidad"), m.pills); assertEquals("wiki", m.fuente); assertEquals(listOf("JLS"), m.proyectos)
        assertEquals("Ya no está en la banda", Fichas.miembro(emptyMap(), null, mapOf("en_la_banda" to "salió")).pills[0])
        assertEquals("Coachella", Fichas.festivalLimpio("Coachella (2023)")); assertEquals("2023", Fichas.festivalAnio("Coachella (2023)"))
        val f = Fichas.festival(emptyMap(), null, mapOf("historia" to "Festival en Indio, California.", "pais" to "Estados Unidos", "inicio" to 1999, "ediciones_relevantes" to listOf("2023: Bad Bunny encabeza"), "participantes_destacados" to listOf("Beyoncé")))
        assertEquals(listOf("Estados Unidos", "Desde 1999"), f.pills); assertEquals("ia", f.fuente); assertEquals(listOf("Beyoncé"), f.participantes)
        assertFalse(Fichas.esFestival(w))
    }

    @Test fun foto_de_spotify_sin_homonimos() {
        val j = """{"artists":{"items":[{"name":"Bad Bunny Fans","images":[{"url":"https://i.scdn.co/x"}]},{"name":"Bad Bunny","images":[{"url":"https://i.scdn.co/ok"}]}]}}"""
        assertEquals("https://i.scdn.co/ok", Fichas.fotoSpotify(j, "bad bunny")); assertEquals("", Fichas.fotoSpotify(j, "Zoé"))
    }
}
