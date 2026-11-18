package com.hilo.lector

import com.hilo.lector.data.Grade
import com.hilo.lector.data.Srs
import com.hilo.lector.data.StudyCard
import com.hilo.lector.text.Analyzer
import com.hilo.lector.text.Apa
import com.hilo.lector.text.KeyWords
import com.hilo.lector.text.Law
import com.hilo.lector.text.Sent
import com.hilo.lector.text.Social
import com.hilo.lector.text.TextTools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de la lógica de Hilo: leyes, autores, citas, repaso y textos raros. */
class LogicTest {
    private val ley = listOf(
        "LIBRO PRIMERO", "DISPOSICIONES GENERALES", "TÍTULO I", "DE LOS PRINCIPIOS",
        "ARTÍCULO 1.- Tutela. Toda persona puede acudir a los tribunales, conforme a los artículos 3 y 5.",
        "ARTÍCULO 2.- Igualdad. Las partes son iguales.",
        "ARTÍCULO 3.- Plazos. La demanda se contesta dentro del plazo de treinta (30) días hábiles. Artículo 4.- Publicidad. Ver el artículo 2.",
        "CAPÍTULO II", "De los recursos",
        "ARTÍCULO 5.- Apelación. Se interpone en el plazo de cinco días. La acción prescribe a los diez años.",
        "Artículo 6 bis.- Casación. Según los artículos 5, 6 y 8.",
    )

    @Test fun reconoceArticulosYEstructura() {
        val l = Law.parse(ley)
        assertEquals(listOf("1", "2", "3", "4", "5", "6 bis"), l.articles.map { it.num })
        assertEquals(listOf("3", "5"), l.find("1")!!.refs)
        assertEquals(listOf("2"), l.find("4")!!.refs)
        assertNotNull(l.find("6bis"))
        assertTrue(l.headings.size >= 3)
        assertEquals("Tutela", l.find("1")!!.title)
    }

    @Test fun encuentraPlazos() {
        val p = Law.parse(ley).deadlines.map { it.amount to it.article }
        assertTrue(p.toString(), ("treinta (30) días hábiles" to "3") in p)
        assertTrue(p.toString(), ("cinco días" to "5") in p)
        assertTrue(p.toString(), ("diez años" to "5") in p)
    }

    @Test fun remisionesLargasNoRompen() {
        val big = "Ver los artículos " + (1..20000).joinToString(", ")
        assertTrue(Law.refsIn(big).size <= 40)
        val l = Law.parse(listOf("ARTÍCULO 1.- $big.", "ARTÍCULO 2.- a.", "ARTÍCULO 3.- b."))
        assertEquals(3, l.articles.size)
    }

    @Test fun ceroIzquierdaYBis() {
        val l = Law.parse(listOf("Art. 0005. Algo.", "ARTÍCULO 2 BIS.- B.", "Artículo 1bis.- A."))
        assertNotNull(l.find("5")); assertNotNull(l.find("2bis")); assertNotNull(l.find("1 bis"))
    }

    private val soc = listOf(
        "# Clásicos",
        "Para Marx, la estructura económica determina la conciencia social. Marx sostiene que la historia es la lucha de clases.",
        "Weber plantea que la religión influyó en el capitalismo (Weber, 1905). La burocracia es central en Weber.",
        "Durkheim estudió la división del trabajo en 1893 y el suicidio en 1897. Para Durkheim, la anomia es la falta de normas.",
        "# Contemporáneos",
        "Bourdieu (1979) propone el capital cultural. En el siglo XIX nace la sociología.",
        "Marx analiza el capitalismo y las clases sociales. Weber analiza el capitalismo y la burocracia. El capitalismo transforma la sociedad.",
        "Para Weber, el capitalismo moderno depende de la racionalidad. Para Marx, el capitalismo explota el trabajo de las clases.",
    )

    @Test fun autoresYLineaDeTiempo() {
        val a = Analyzer.analyze(soc, emptySet())
        val names = a.authors.map { it.name }
        assertTrue(names.toString(), names.containsAll(listOf("Karl Marx", "Max Weber", "Émile Durkheim", "Pierre Bourdieu")))
        val years = a.timeline.map { it.label }
        assertTrue(years.toString(), years.containsAll(listOf("1893", "1897", "1905", "1979", "Siglo XIX")))
        assertEquals(years.sortedBy { y -> a.timeline.first { it.label == y }.year }, years)
        val rows = Social.compare(a.authors.take(2), a.sentences) { a.labelOf(it) }
        assertTrue(rows.isNotEmpty())
    }

    @Test fun citasApa() {
        assertEquals("Weber, M. (1922). Economía y sociedad. FCE.", Apa.reference("Max Weber", "1922", "Economía y sociedad", "FCE", ""))
        assertEquals("Berger, P., & Luckmann, T. (1966). La construcción social de la realidad.", Apa.reference("Peter Berger; Thomas Luckmann", "1966", "La construcción social de la realidad", "", ""))
        assertEquals("(Berger y Luckmann, 1966, p. 15)", Apa.inText("Peter Berger y Thomas Luckmann", "1966", "p. 15"))
        assertEquals("(Anónimo, s.f.)", Apa.inText("", "", ""))
        assertTrue(Apa.quote("hola", "Max Weber", "1922", "p. 3").startsWith("“hola”"))
    }

    @Test fun palabrasClave() {
        val keys = KeyWords.of("La anomia es la falta de normas que regulen la conducta y debilita la cohesión social", listOf("anomia"))
        assertEquals("anomia", keys.first())
        val (hit, miss) = KeyWords.coverage("cuando faltan normas la cohesión se debilita", keys)
        assertTrue(hit.containsAll(listOf("normas", "cohesión", "debilita")))
        assertTrue("anomia" in miss)
        assertEquals(0, KeyWords.coverage("", emptyList()).first.size)
    }

    @Test fun repasoEspaciadoFsrs() {
        var c = StudyCard(1, "b", "qa", "f", "r", due = 0)
        c = Srs.schedule(c, Grade.GOOD, 100); assertEquals(103, c.due)           // primera vez «Bien»: unos 3 días
        val first = c.stab
        c = Srs.schedule(c, Grade.GOOD, 103); assertTrue("crece: ${c.stab}", c.stab > first * 2 && c.due > 108)
        val easy = Srs.schedule(c, Grade.EASY, c.due); val good = Srs.schedule(c, Grade.GOOD, c.due)
        assertTrue(easy.interval > good.interval)
        val lapse = Srs.schedule(c, Grade.AGAIN, 200)
        assertEquals(200, lapse.due); assertEquals(1, lapse.lapses); assertTrue(lapse.stab < c.stab)
        var e = StudyCard(2, "b", "qa", "f", "r")
        repeat(30) { e = Srs.schedule(e, Grade.AGAIN, it.toLong()) }
        assertTrue(e.diff in 1f..10f && e.stab > 0f)
        // Tarjeta del sistema anterior (SM-2): se convierte sin perder su intervalo
        val old = StudyCard(3, "b", "qa", "f", "r", ease = 2.5f, interval = 20, reps = 4, due = 120)
        val n = Srs.schedule(old, Grade.GOOD, 120)
        assertTrue("intervalo ${n.interval}", n.interval > 20)
        assertTrue(n.recall(n.last) > 0.99f)
    }

    @Test fun textosRarosNoRompen() {
        val casos = listOf(
            emptyList(), listOf(""), listOf("# ", "#", "# # #"), List(200) { "123 ### ((((" },
            List(100) { "😀 社会学 социология مجتمع Sociedad." }, listOf("palabra ".repeat(100000)),
            List(2000) { "Artículo" }, listOf("artículos " + "y ".repeat(5000)),
        )
        for (p in casos) {
            val a = Analyzer.analyze(p, emptySet())
            assertTrue(a.totalWords >= 0)
        }
        assertEquals(0, TextTools.reflow("").size)
        assertTrue(Social.timeline(listOf(Sent("En el siglo MMMMXX y en 99999.", 0, emptySet())), emptyList()).isEmpty())
    }

    @Test fun libroGrandeEnTiempoRazonable() {
        val words = listOf("sociedad", "capitalismo", "ideología", "clase", "estado", "poder", "norma", "Marx", "Weber", "de", "la", "que", "en")
        val r = kotlin.random.Random(3)
        val paras = List(6000) { (1..60).joinToString(" ") { words[r.nextInt(words.size)] } + " en ${1800 + it % 200}." }
        val t0 = System.currentTimeMillis()
        val a = Analyzer.analyze(paras, emptySet())
        val ms = System.currentTimeMillis() - t0
        println("Libro de ${a.totalWords} palabras analizado en $ms ms")
        assertTrue("tardó $ms ms", ms < 20000)
    }
}
