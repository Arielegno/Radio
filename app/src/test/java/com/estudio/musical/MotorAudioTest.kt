package com.estudio.musical

import com.estudio.musical.audio.AudioData
import com.estudio.musical.audio.Effects
import com.estudio.musical.audio.KeyDetector
import com.estudio.musical.audio.Wav
import com.estudio.musical.audio.dsp.*
import com.estudio.musical.audio.synth.*
import com.estudio.musical.music.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/** Pruebas del motor de audio, sintetizadores, teoría y herramientas de letras (corren en la JVM, sin teléfono). */
class MotorAudioTest {
    private val sr = 48_000

    private fun sano(x: FloatArray) = x.none { it.isNaN() || it.isInfinite() }
    private fun pico(x: FloatArray) = x.maxOf { abs(it) }
    private fun tono(f: Double, n: Int = 4096, amp: Float = 0.5f) = FloatArray(n) { (sin(2 * PI * f * it / sr) * amp).toFloat() }

    private fun render(f: FuenteSonido, nota: Int, seg: Float): FloatArray {
        val v = f.voz(nota, 0.8f, sr); val b = FloatArray((sr * seg).toInt()); var p = 0
        while (p < b.size) { val k = minOf(256, b.size - p); if (p > b.size / 2) v.soltar(); v.render(b, p, k); p += k }
        return b
    }

    // ───────── Afinador ─────────
    @Test fun afinadorDetectaLa440() = assertEquals(440f, Pitch.yin(tono(440.0), sr), 1f)
    @Test fun afinadorDetectaMiGrave() {
        val x = FloatArray(4096) { (sin(2 * PI * 82.41 * it / sr) + 0.5 * sin(2 * PI * 164.8 * it / sr)).toFloat() * 0.4f }
        assertEquals(82.41f, Pitch.yin(x, sr), 1f)
    }
    @Test fun afinadorSilencioSinNota() = assertTrue(Pitch.yin(FloatArray(4096), sr) < 0)
    @Test fun nombresDeNotas() { assertEquals("La4", Pitch.nombre(69)); assertEquals("Do4", Pitch.nombre(60)) }

    // ───────── Instrumentos ─────────
    @Test fun todosLosInstrumentosSuenan() {
        for (i in Instrumentos.TODOS) {
            val b = render(i, 57, 1.5f)
            assertTrue("${i.nombre} produce valores inválidos", sano(b))
            assertTrue("${i.nombre} pico ${pico(b)}", pico(b) in 0.02f..1.5f)
        }
    }
    @Test fun pianoAfinado() {
        val b = render(Instrumentos.PIANO, 69, 1f)
        assertEquals(69, Pitch.notaPrincipal(b.copyOfRange(2000, sr / 2), sr))
    }
    @Test fun bateriaCompleta() {
        for (p in Pieza.entries) {
            val s = Bateria.sonido(p, sr)
            assertTrue(p.nombre, sano(s) && pico(s) > 0.05f)
        }
    }

    // ───────── Sampler ─────────
    @Test fun samplerDetectaYAfina() {
        val raw = FloatArray(sr) { (sin(2 * PI * 329.63 * it / sr) * exp(-it / (sr * 0.6))).toFloat() * 0.7f }
        val m = Muestra.desdeGrabacion("vaso", raw, sr)
        assertEquals("nota raíz", 64, m.notaRaiz)
        val t = render(m, 69, 0.5f)
        assertEquals(440f, Pitch.yin(t.copyOfRange(2000, 6096), sr), 3f)
    }

    // ───────── Teoría ─────────
    @Test fun acordesDeSolMayor() = assertEquals(
        "I=Sol ii=Lam iii=Sim IV=Do V=Re vi=Mim vii°=Fa#dim", Teoria.acordes(7, true).joinToString(" "))
    @Test fun gradosRomanos() = assertEquals(listOf("Do", "La#", "Fa", "Sol7", "Redim"),
        Grado.parseLista("I bVII IV V7 iio").map { it.acorde(0, true).nombre })
    @Test fun todasLasProgresionesSeEntienden() {
        for (e in Estilo.entries) for (pr in e.progMayor + e.progMenor)
            assertEquals("${e.nombre}: $pr", pr.split(" ").size, Grado.parseLista(pr).size)
    }
    @Test fun parseAcordes() {
        assertEquals("Fa#m", Acorde.parse("F#m")?.nombre)
        assertEquals("Lam7", Acorde.parse("Lam7")?.nombre)
        assertEquals("La#", Acorde.parse("Bb")?.nombre)
    }

    // ───────── Letras ─────────
    @Test fun transponer() = assertEquals("[Coro]\n[Re]Hoy te [La/Do#]vi [Sim]pasar",
        TextoMusical.transponer("[Coro]\n[Do]Hoy te [Sol/Si]vi [Lam]pasar", 2))
    @Test fun acordesSobreLaLetra() {
        val (ac, le) = TextoMusical.separarAcordes("[Do]Hoy te [Sol]vi pasar")
        assertEquals("Hoy te vi pasar", le)
        assertEquals(le.indexOf("vi"), ac.indexOf("Sol"))
    }
    @Test fun silabas() {
        mapOf("corazón" to 3, "canción" to 2, "poesía" to 4, "quiero" to 2, "guitarra" to 3, "aéreo" to 4,
            "ciudad" to 2, "hoy" to 1, "rey" to 1, "maíz" to 2, "huevo" to 2, "lluvia" to 2, "pingüino" to 3,
            "murciélago" to 4, "oía" to 3, "leer" to 2, "construir" to 2)
            .forEach { (w, n) -> assertEquals(w, n, TextoMusical.silabas(w)) }
    }
    @Test fun metricaOctosilabo() = assertEquals(8, TextoMusical.silabasVerso("En un lugar de la Mancha").second)
    @Test fun rimas() {
        val r = TextoMusical.rimas("corazón")
        assertTrue("canción" in r.consonantes && "razón" in r.consonantes)
        assertTrue("destino" in TextoMusical.rimas("camino").consonantes)
    }

    // ───────── Creador de pistas ─────────
    @Test fun pistaPopEnSolMayor() {
        val m = Muestra.desdeGrabacion("vaso", FloatArray(sr) { (sin(2 * PI * 329.63 * it / sr) * exp(-it / (sr * 0.6))).toFloat() * 0.7f }, sr)
        val pistas = GeneradorPistas.generar(Arreglo(7, true, 100f, Estilo.POP, Grado.parseLista("I V vi IV"), 8,
            acordes = Instrumentos.PIANO_ELECTRICO, bajo = Instrumentos.BAJO_ELECTRICO, melodia = m), sr)
        assertEquals(listOf("Batería", "Bajo", "Acordes", "Melodía"), pistas.keys.toList())
        pistas.forEach { (k, v) -> assertTrue(k, sano(v) && pico(v) > 0.3f) }
        val mezcla = GeneradorPistas.mezcla(pistas)
        val k = KeyDetector.detect(AudioData(mezcla, sr))
        assertTrue("tonalidad ${k.tonalidad}", k.tonalidad == "Sol mayor" || k.alternativa == "Sol mayor")
        assertEquals(100f, Bpm.detect(pistas["Batería"]!!, sr), 2f)
    }
    @Test fun todosLosEstilosGeneran() {
        for (e in Estilo.entries) {
            val p = GeneradorPistas.generar(Arreglo(9, false, e.bpm.toFloat(), e, Grado.parseLista(e.progMenor[0]), 4,
                acordes = Instrumentos.GUITARRA, bajo = Instrumentos.BAJO_SINTE), sr)
            assertTrue(e.nombre, p.values.all { sano(it) })
        }
    }
    @Test fun renderDelTeclado() {
        val a = GeneradorPistas.renderizar(listOf(Nota(0, sr / 2, 60, 0.8f), Nota(sr / 2, sr / 2, 64, 0.8f), Nota(sr, sr, 67, 0.8f)),
            Instrumentos.PIANO, sr)
        assertTrue(sano(a) && a.size >= sr * 2)
    }

    // ───────── Efectos ─────────
    private val voz by lazy { render(Instrumentos.FLAUTA, 64, 3f) }
    @Test fun reverb() = assertTrue(sano(Reverb.process(voz, sr)))
    @Test fun ecualizador() = assertTrue(sano(Eq.tresBandas(voz, sr, 6f, -3f, 4f)))
    @Test fun compresor() = assertTrue(sano(Dynamics.compresor(voz, sr)))
    @Test fun puertaRuido() = assertTrue(sano(Dynamics.puertaRuido(voz, sr)))
    @Test fun eco() = assertTrue(sano(Effects.echo(AudioData(voz, sr), 300, 0.4f, 0.5f).samples))
    @Test fun reduccionDeRuido() {
        val r = java.util.Random(1)
        val x = FloatArray(sr * 3) { (r.nextFloat() - 0.5f) * 0.05f + if (it > sr) (sin(2 * PI * 440 * it / sr) * 0.5).toFloat() else 0f }
        val y = NoiseReduction.process(x, sr)
        fun rms(a: FloatArray) = sqrt(a.take(sr / 2).sumOf { (it * it).toDouble() } / (sr / 2))
        assertTrue("ruido ${rms(x)} → ${rms(y)}", rms(y) < rms(x) * 0.25)
        assertEquals(440f, Pitch.yin(y.copyOfRange(sr * 2, sr * 2 + 4096), sr), 2f)
    }
    @Test fun mezcladorYWavEstereo() {
        val (l, r) = Mezclador.mezclar(listOf(PistaAudio(voz, paneo = -1f), PistaAudio(voz, inicioMs = 500)), sr)
        assertEquals(voz.size + sr / 2, l.size)
        val f = File.createTempFile("estereo", ".wav")
        Wav.writeStereo(f, l, r, sr)
        assertEquals(44L + l.size * 4, f.length())
        assertEquals(2, Wav.info(f).channels)
        f.delete()
    }
    @Test fun wavIdaYVuelta() {
        val f = File.createTempFile("mono", ".wav")
        Wav.write(f, AudioData(voz, sr))
        val b = Wav.read(f)
        assertEquals(voz.size, b.samples.size)
        assertEquals(voz[1000], b.samples[1000], 1e-3f)
        f.delete()
    }
}
