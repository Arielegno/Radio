package io.github.arielegno.radio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

class CoreTest {
    private val lib = listOf(
        Track(1, "Persiana americana", "Soda Stereo", "Signos", file = "1.mp3"),
        Track(2, "De música ligera", "Soda Stereo", "Canción Animal", file = "2.mp3"),
        Track(3, "Labios rotos", "Zoé", "Memo Rex Commander", file = "3.mp3"),
        Track(4, "Rayando el sol", "Maná", "Falta amor", file = "4.mp3")
    )

    @Test fun json_ida_y_vuelta() {
        val t = """{"a":"hola \"mundo\" ñ","b":[1,2.5,-3,true,false,null],"c":{"d":"\u00e1\n"},"e":12345678901}"""
        val o = Json.parse(t) as Map<*, *>
        assertEquals("hola \"mundo\" ñ", o["a"])
        assertEquals(listOf(1L, 2.5, -3L, true, false, null), o["b"])
        assertEquals("á\n", (o["c"] as Map<*, *>)["d"])
        assertEquals(12345678901L, o["e"])
        assertEquals(o, Json.parse(Json.write(o)))
        assertEquals(emptyMap<String, Any?>(), Json.obj("{roto"))
        assertEquals("\"\\u0001\"", Json.write("\u0001"))
    }

    @Test fun nombres_y_claves() {
        assertEquals("cancionanimal", normNombre("Canción Animal!"))
        assertEquals("persiana americana|soda stereo", keyOf("Persiana Americana", "Soda Stereo"))
        assertEquals("3:07", fmtDur(187_000))
        assertEquals("Buenas noches, Ari", saludo(22, " Ari "))
        assertEquals("Buenos días", saludo(8, ""))
    }

    @Test fun comandos_de_voz() {
        assertEquals(VozCmd.Poner(listOf(3L), "Zoé"), Voz.entender("Pon algo de Zoé", lib))
        assertEquals(listOf(1L, 2L), (Voz.entender("ponme soda stereo", lib) as VozCmd.Poner).ids)
        assertEquals(listOf(4L), (Voz.entender("quiero escuchar rayando el sol", lib) as VozCmd.Poner).ids)
        assertEquals(listOf(2L), (Voz.entender("reproduce canción animal", lib) as VozCmd.Poner).ids)
        assertTrue(Voz.entender("pon reggaeton viejo", lib) is VozCmd.NoEncontrado)
        assertEquals(VozCmd.Siguiente, Voz.entender("Siguiente", lib))
        assertEquals(VozCmd.Anterior, Voz.entender("la de antes", lib))
        assertEquals(VozCmd.Pausa, Voz.entender("pausa", lib))
        assertEquals(VozCmd.Pausa, Voz.entender("pon pausa", lib))
        assertEquals(VozCmd.Sigue, Voz.entender("dale", lib))
        assertEquals(VozCmd.Favorita, Voz.entender("favorita", lib))
        assertEquals(VozCmd.SoloMusica, Voz.entender("solo música", lib))
        assertEquals(VozCmd.Luna, Voz.entender("Luna", lib))
        assertEquals(VozCmd.Salir, Voz.entender("cerrar modo carro", lib))
        assertTrue(Voz.entender("qué hora es", lib) is VozCmd.NoEntendi)
        assertEquals("Poniendo Zoé…", Voz.respuesta(VozCmd.Poner(listOf(3), "Zoé")))
    }

    @Test fun estadisticas_resumen_y_mapa() {
        val e = Estadisticas(); val hoy = LocalDate.of(2026, 11, 8)
        repeat(3) { e.countPlay("Persiana americana", "Soda Stereo", "Ciudad Centro", 10.5000, -66.9000, hoy) }
        e.countPlay("Labios rotos", "Zoé", "Ciudad Norte", 11.2, -67.4, hoy)
        e.countPlay("Labios rotos", "Zoé", null, null, null, hoy.minusDays(1))
        val r = e.resumen("month", hoy)!!
        assertEquals(5, r.c); assertEquals(2, r.days)
        assertEquals("Soda Stereo" to 3, r.topArtistas(1)[0])
        assertEquals(("Persiana americana" to "Soda Stereo") to 3, r.topCanciones(1)[0])
        assertNull(e.resumen("month", hoy.plusMonths(1)))
        assertEquals(5, e.resumen("all", hoy.plusMonths(1))!!.c)
        val pts = e.puntosMapa().sortedBy { it.c }
        assertEquals(listOf(1, 3), pts.map { it.c })
        assertEquals("Ciudad Centro", pts[1].lugar)
        assertEquals("Persiana americana — Soda Stereo" to 3, pts[1].canciones[0])
        assertTrue(e.toggleFav("Labios rotos", "Zoé")); assertTrue(e.isFav("labios rotos", "ZOÉ"))
        val e2 = Estadisticas.fromJson(e.toJson())
        assertEquals(e.toJson(), e2.toJson())
        assertEquals(5, e2.resumen("month", hoy)!!.c)
        assertFalse(e2.toggleFav("Labios rotos", "Zoé"))
    }

    @Test fun luna_prompt_y_respuesta() {
        val a = LunaAjustes(nombre = "Ari", modo = "estudiando")
        val s = Luna.sistema(a, true, 22)
        assertTrue(s.contains("se llama Ari")); assertTrue(s.contains("estudiando o concentrada")); assertTrue(s.contains("noche"))
        assertFalse(s.contains("No hables de: estudiar"))
        val u = Json.obj(Luna.usuario(LunaContexto(lib[0], lib[2], clima = "24°, nublado", programa = "«La hora del recuerdo»: x")))
        assertEquals("Persiana americana", u.map("cancion_que_termino").str("titulo"))
        assertEquals("Zoé", u.map("cancion_que_viene").str("artista"))
        assertEquals("24°, nublado", u.str("clima"))
        val body = Json.obj(Luna.peticionGemini("sis", "usr"))
        assertEquals("sis", ((body.map("systemInstruction").list("parts")[0]) as Map<*, *>)["text"])
        val resp = """{"candidates":[{"content":{"parts":[{"text":"**Mira** esto 🎸 es","thought":false},{"text":" Soda."}]}}]}"""
        assertEquals("Mira esto es Soda.", Luna.textoGemini(resp))
        assertNull(Luna.textoGemini("""{"candidates":[]}"""))
        val sin = Luna.sinIA(LunaContexto(lib[0], lib[2]), a, Random(1))
        assertTrue(sin.contains("Persiana americana") && sin.contains("Labios rotos"))
    }

    @Test fun programas_de_luna() {
        val hoy = LocalDateTime.of(2026, 11, 8, 20, 5) // domingo
        val r1 = Programas.elegir(hoy, Programas.Estado(true, 20, "", false, ""), 1986, null)
        assertTrue(r1.texto!!.contains("La hora del recuerdo") && r1.texto!!.contains("1986")); assertEquals("2026-11-08", r1.marcarRec)
        assertNull(Programas.elegir(hoy, Programas.Estado(true, 20, "2026-11-08", false, ""), 1986, null).texto)
        val e = Estadisticas(); e.countPlay("Persiana americana", "Soda Stereo", null, null, null, hoy.toLocalDate())
        val r2 = Programas.elegir(hoy, Programas.Estado(false, 20, "", true, ""), 0, e.resumen("month", hoy.toLocalDate()))
        assertTrue(r2.texto!!.contains("resumen del domingo") && r2.texto!!.contains("Soda Stereo"))
        assertNull(Programas.elegir(hoy.minusDays(1), Programas.Estado(false, 20, "", true, ""), 0, e.resumen("month", hoy.toLocalDate())).texto)
    }

    @Test fun copia_de_musica_compatible() {
        val audio1 = ByteArray(4000) { 7 }; val cover1 = ByteArray(50) { 3 }; val audio2 = ByteArray(3000) { 9 }
        val items = listOf(
            CopiaMusica.Elemento(linkedMapOf("name" to "Persiana americana", "artist" to "Soda Stereo"), listOf(
                CopiaMusica.Archivo("blob", "audio/mpeg", 4000) { ByteArrayInputStream(audio1) },
                CopiaMusica.Archivo("cover", "image/jpeg", 50) { ByteArrayInputStream(cover1) })),
            CopiaMusica.Elemento(linkedMapOf("name" to "Labios rotos", "artist" to "Zoé"), listOf(
                CopiaMusica.Archivo("blob", "audio/mpeg", 3000) { ByteArrayInputStream(audio2) }))
        )
        val out = ByteArrayOutputStream(); CopiaMusica.escribir(out, items, 1, 1)
        val bytes = out.toByteArray()
        assertEquals("RRLIB1", String(bytes, 0, 6))
        val guardado = linkedMapOf<String, ByteArray>(); val listos = mutableListOf<String>()
        val (ag, om) = CopiaMusica.leer(ByteArrayInputStream(bytes),
            omitir = { d, _ -> d.str("name") == "Labios rotos" && listos.size > 5 },
            guardar = { d, r, s -> val k = d.str("name") + "/" + r.campo; guardado[k] = s.readBytes(); k },
            listo = { d, g -> listos += d.str("name") + ":" + g.keys.sorted().joinToString(",") })
        assertEquals(2 to 0, ag to om)
        assertTrue(guardado["Persiana americana/blob"]!!.contentEquals(audio1))
        assertTrue(guardado["Persiana americana/cover"]!!.contentEquals(cover1))
        assertTrue(guardado["Labios rotos/blob"]!!.contentEquals(audio2))
        assertEquals(listOf("Persiana americana:blob,cover", "Labios rotos:blob"), listos)
        // Omitir el primero salta sus bytes y el segundo se lee bien
        val g2 = linkedMapOf<String, ByteArray>()
        val (a2, o2) = CopiaMusica.leer(ByteArrayInputStream(bytes), omitir = { d, _ -> d.str("name") == "Persiana americana" },
            guardar = { d, r, s -> g2[d.str("name") + r.campo] = s.readBytes(); "x" }, listo = { _, _ -> })
        assertEquals(1 to 1, a2 to o2); assertTrue(g2["Labios rotosblob"]!!.contentEquals(audio2))
        // Partes
        assertEquals(listOf(1, 1), CopiaMusica.partir(items, 5000).map { it.size })
        assertEquals(listOf(2), CopiaMusica.partir(items).map { it.size })
        // Archivo equivocado
        try { CopiaMusica.leer(ByteArrayInputStream("hola que tal".toByteArray()), { _, _ -> false }, { _, _, _ -> "" }, { _, _ -> }); throw AssertionError("debía fallar") }
        catch (e: IllegalArgumentException) { assertTrue(e.message!!.contains("No es una copia")) }
    }

    @Test fun copia_hecha_por_la_web() {
        // Encabezado como lo escribe la web (campos en otro orden y con dur)
        val h = """{"v":1,"part":1,"of":1,"items":[{"name":"A","artist":"B","dur":200,"blob":{"__b":0,"t":"audio/mpeg","n":3},"cover":{"__b":1,"t":"image/jpeg","n":2}}]}""".toByteArray()
        val o = ByteArrayOutputStream(); o.write("RRLIB1".toByteArray()); o.write(byteArrayOf(0, 0, (h.size shr 8).toByte(), h.size.toByte())); o.write(h); o.write(byteArrayOf(1, 2, 3, 4, 5))
        val got = mutableMapOf<String, List<Byte>>(); var dur = 0L
        CopiaMusica.leer(ByteArrayInputStream(o.toByteArray()), { _, _ -> false }, { _, r, s -> got[r.campo] = s.readBytes().toList(); r.campo }, { d, _ -> dur = d.num("dur") })
        assertEquals(listOf<Byte>(1, 2, 3), got["blob"]); assertEquals(listOf<Byte>(4, 5), got["cover"]); assertEquals(200L, dur)
    }

    @Test fun portada_configurable() {
        var c = PortadaCfg.fromJson(null)
        assertEquals(listOf("artists", "foryou", "day", "week", "club"), c.orden)
        c = c.mover("week", -1).mover("week", -1).alternar("place", false).copy(disco = "cuadrada", tinte = false)
        assertEquals(listOf("artists", "week", "foryou", "day", "club"), c.orden)
        val c2 = PortadaCfg.fromJson(c.toJson())
        assertEquals(c, c2); assertFalse(c2.visible("place"))
        // Compatible con lo que guarda la web
        val w = PortadaCfg.fromJson("""{"off":["modes"],"order":["club","raro"],"disc":"square","tint":false}""")
        assertEquals(listOf("club", "artists", "foryou", "day", "week"), w.orden); assertEquals("cuadrada", w.disco); assertFalse(w.visible("modes"))
        assertEquals(c, c.mover("artists", -1))
    }

    @Test fun concierto_del_dia_y_clima() {
        val hoy = LocalDate.of(2026, 11, 8)
        val a = Concierto("a", "A", "", 0, "youtube", "abcdefghijk", "", "2026-11-01", "", "1")
        val b = Concierto("b", "B", "", 0, "vimeo", "1", "", "2026-11-08", "", "2")
        val c = Concierto("c", "C", "", 0, "archive", "x", "", "2026-10-20", "2026-11-08", "3")
        assertEquals("c", Concierto.delDia(listOf(a, b, c), hoy)!!.id)
        assertEquals("b", Concierto.delDia(listOf(a, b), hoy)!!.id)
        assertNotNull(Concierto.delDia(listOf(a), hoy)); assertNull(Concierto.delDia(emptyList(), hoy))
        assertEquals("https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg", a.miniatura())
        assertEquals("", a.copy(video = "<script>").miniatura())
        assertEquals("", a.copy(portada = "https://evil.com/x.jpg", proveedor = "vimeo").miniatura())
        assertEquals("24°, nublado", Clima.deOpenMeteo("""{"current":{"temperature_2m":23.6,"weather_code":3}}""", false))
        assertNull(Clima.deOpenMeteo("{}", false))
        assertEquals("Ciudad Centro", Clima.zonaNominatim("""{"address":{"neighbourhood":"Barrio Alto","city":"Ciudad Centro","state":"Provincia"}}"""))
        assertEquals("Provincia", Clima.zonaNominatim("""{"address":{"suburb":"X","state":"Provincia"}}"""))
        assertEquals("Hoy cumple años mi papá", Efemerides.propias("11-08: Hoy cumple años mi papá\n12-25: Navidad", hoy))
        val w = Efemerides.deWikipedia("""{"events":[{"year":1990,"text":"Soda Stereo lanza el álbum Canción Animal"},{"year":1900,"text":"Se funda una ciudad"}]}""")
        assertEquals(listOf("1990" to "Soda Stereo lanza el álbum Canción Animal"), w)
    }
}
