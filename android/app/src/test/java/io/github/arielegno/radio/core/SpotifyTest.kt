package io.github.arielegno.radio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SpotifyTest {
    @Test fun pkce_como_dice_spotify() {
        // Ejemplo del estándar RFC 7636
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", SpotifyDatos.reto("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
        val v = SpotifyDatos.verificador(Random(3)); assertEquals(64, v.length); assertTrue(v.all { it.isLetterOrDigit() || it in "-._~" })
        val u = SpotifyDatos.urlEntrar("abc123", v, "e1")
        assertTrue(u.startsWith("https://accounts.spotify.com/authorize?response_type=code&client_id=abc123"))
        assertTrue(u.contains("redirect_uri=radioderuta%3A%2F%2Fspotify") && u.contains("code_challenge_method=S256") && u.contains("state=e1"))
        assertTrue(SpotifyDatos.cuerpoCanje("abc", "c d", "v").contains("code=c+d"))
    }

    @Test fun lo_que_suena() {
        val j = """{"progress_ms":61000,"is_playing":true,"item":{"type":"track","uri":"spotify:track:1","name":"Persiana americana","duration_ms":291000,
          "artists":[{"name":"Soda Stereo"}],"album":{"name":"Signos","release_date":"1986-11-10","images":[{"url":"https://i.scdn.co/image/a","width":640},{"url":"https://i.scdn.co/image/b","width":64}]}}}"""
        val a = SpotifyDatos.ahora(j)!!
        assertEquals("Persiana americana", a.nombre); assertEquals("Soda Stereo", a.artista); assertEquals(1986, a.anio)
        assertEquals("https://i.scdn.co/image/a", a.portada); assertEquals(61000L, a.posMs); assertTrue(a.sonando)
        assertTrue(a.comoTrack().id < 0); assertEquals(a.comoTrack().key, keyOf("Persiana americana", "Soda Stereo"))
        assertNull(SpotifyDatos.ahora("""{"item":{"type":"episode","name":"x"}}""")); assertNull(SpotifyDatos.ahora(""))
        val q = SpotifyDatos.siguienteEnCola("""{"queue":[{"type":"track","uri":"spotify:track:2","name":"Labios rotos","artists":[{"name":"Zoé"}],"album":{"name":"Memo Rex"}}]}""")!!
        assertEquals("Labios rotos", q.nombre)
        val b = """{"artists":{"items":[{"name":"Soda Stereo","uri":"spotify:artist:9"}]},"tracks":{"items":[{"name":"Zoom","uri":"spotify:track:7"}]}}"""
        assertEquals("spotify:artist:9", SpotifyDatos.elegirBusqueda(b, "soda stereo"))
        assertEquals("spotify:track:7", SpotifyDatos.elegirBusqueda(b, "zoom"))
    }
}
