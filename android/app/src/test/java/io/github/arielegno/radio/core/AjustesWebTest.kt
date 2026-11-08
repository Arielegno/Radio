package io.github.arielegno.radio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AjustesWebTest {
    @Test fun lee_el_archivo_que_exporta_la_web() {
        val web = """{"cloudBaseUrl":"https://x.netlify.app/","source":"spotify","shuffle":"1","wifiOnly":"0","gender":"locutora","accent":"neutro","style":"",
          "len":"media","tEvery":"1","avoid":"","persona":"","contextoMusical":"","host":"Luna","wMin":"30","pEvery":"2","voicePreset":"natural",
          "rate":"0.95","pitch":"0.9","vol":"1","voice":"Spanish (Spain)","clientId":"8b99f9c4866849c7bc7ecc679f077ee8","provider":"gemini",
          "geminiModelSelect":"gemini-3.5-flash-lite","geminiModel":"","model":"claude-sonnet-5-5","sbUrl":"","sbKey":"","listeningMode":"carretera",
          "mode_carretera":"","frases_hogar":"Ponte cómodo","efemerides":"","dedic":"","me":"","units":"c","h12":"auto","accentColor":"#f2c230",
          "cardSize":"medium","fsize":"15","diag":"0","nomEmail":"","memoria":"1"}"""
        val m = AjustesWeb.convertir(Json.obj(web))!!
        assertEquals("spotify", m["fuente"]); assertEquals(true, m["mezclar"]); assertEquals(false, m["soloWifi"])
        assertEquals("8b99f9c4866849c7bc7ecc679f077ee8", m["spClientId"]); assertEquals("carretera", m["modo"])
        assertEquals(1, m["cadaN"]); assertEquals(30, m["wMin"]); assertEquals(2, m["pEvery"]); assertEquals(15, m["letra"])
        assertEquals(95, m["velocidad"]); assertEquals(90, m["tono"]); assertEquals(100, m["volVoz"])
        assertEquals("Ponte cómodo", m["frases_hogar"]); assertEquals("#f2c230", m["acentoColor"]); assertEquals("medium", m["portadas"])
        assertEquals("", m["estilo"]); assertEquals(true, m["memoria"]); assertEquals("gemini-3.5-flash-lite", m["geminiModelSel"])
        assertFalse(m.containsKey("sbKey")); assertFalse(m.containsKey("cloudBaseUrl")); assertEquals("https://x.netlify.app/", m["nube"])
        // Valores raros se ignoran
        val raro = AjustesWeb.convertir(Json.obj("""{"host":"Sol","provider":"otro","listeningMode":"fiesta","tEvery":"999","accentColor":"#000000"}"""))!!
        assertEquals("Sol", raro["host"]); assertFalse(raro.containsKey("proveedor")); assertFalse(raro.containsKey("modo")); assertEquals(10, raro["cadaN"]); assertFalse(raro.containsKey("acentoColor"))
        assertNull(AjustesWeb.convertir(Json.obj("""{"app":"otra"}"""))); assertTrue(AjustesWeb.esDeLaWeb(Json.obj(web)))
    }
}
