package com.estudio.musical.audio.synth

enum class TipoSintesis { SUSTRACTIVA, FM, ORGANO, CUERDA }

/**
 * Definición de un instrumento digital. Todos los parámetros se pueden cambiar
 * desde la pantalla "Sintetizador" para crear sonidos propios.
 */
data class Instrumento(
    override val nombre: String,
    val tipo: TipoSintesis = TipoSintesis.SUSTRACTIVA,
    val onda1: Onda = Onda.SIERRA,
    val onda2: Onda = Onda.CUADRADA,
    val mezcla2: Float = 0f,
    val octava2: Int = 0,
    val desafinar: Float = 0f,      // cents, da "grosor"
    val ataque: Float = 0.005f,
    val caida: Float = 0.3f,
    val sostener: Float = 0.7f,
    val liberar: Float = 0.3f,
    val corte: Float = 6000f,       // Hz (en CUERDA: brillo 0..1)
    val resonancia: Float = 0.2f,
    val envFiltro: Float = 0f,      // cuánto abre el filtro la envolvente (Hz)
    val caidaFiltro: Float = 0.3f,
    val seguirTecla: Float = 0f,
    val fmRatio: Float = 1f,
    val fmIndice: Float = 0f,
    val fmCaida: Float = 1f,
    val registros: FloatArray = floatArrayOf(0f, 1f, 0f, 0f, 0f, 0f),
    val vibrato: Float = 0f,        // cents
    val volumen: Float = 0.4f
) : FuenteSonido {
    override fun voz(nota: Int, velocidad: Float, sr: Int): Voz = when (tipo) {
        TipoSintesis.SUSTRACTIVA -> VozSustractiva(nota, velocidad, sr, this)
        TipoSintesis.FM -> VozFm(nota, velocidad, sr, this)
        TipoSintesis.ORGANO -> VozOrgano(nota, velocidad, sr, this)
        TipoSintesis.CUERDA -> VozCuerda(nota, velocidad, sr, this)
    }
}

object Instrumentos {
    val PIANO_ELECTRICO = Instrumento("Piano eléctrico", TipoSintesis.FM,
        ataque = 0.002f, caida = 1.8f, sostener = 0f, liberar = 0.4f,
        fmRatio = 1f, fmIndice = 2.2f, fmCaida = 0.6f, volumen = 0.45f)
    val PIANO = Instrumento("Piano", TipoSintesis.FM,
        ataque = 0.001f, caida = 2.5f, sostener = 0f, liberar = 0.3f,
        fmRatio = 2f, fmIndice = 1.3f, fmCaida = 0.35f, volumen = 0.42f)
    val ORGANO = Instrumento("Órgano", TipoSintesis.ORGANO,
        ataque = 0.01f, caida = 0.1f, sostener = 1f, liberar = 0.08f,
        registros = floatArrayOf(0.8f, 1f, 0.7f, 0.4f, 0.3f, 0.1f), volumen = 0.35f)
    val PAD = Instrumento("Pad atmosférico", onda1 = Onda.SIERRA, onda2 = Onda.SIERRA, mezcla2 = 0.4f,
        octava2 = 1, desafinar = 12f, ataque = 0.9f, caida = 1f, sostener = 0.8f, liberar = 1.8f,
        corte = 1600f, resonancia = 0.25f, volumen = 0.28f)
    val CUERDAS = Instrumento("Cuerdas", onda1 = Onda.SIERRA, onda2 = Onda.SIERRA, mezcla2 = 0.3f,
        desafinar = 9f, ataque = 0.35f, caida = 0.5f, sostener = 0.85f, liberar = 0.7f,
        corte = 3500f, vibrato = 8f, volumen = 0.28f)
    val METALES = Instrumento("Metales", onda1 = Onda.SIERRA, ataque = 0.06f, caida = 0.4f, sostener = 0.7f,
        liberar = 0.2f, corte = 700f, envFiltro = 3500f, caidaFiltro = 0.35f, resonancia = 0.15f, volumen = 0.35f)
    val LEAD = Instrumento("Lead", onda1 = Onda.CUADRADA, onda2 = Onda.SIERRA, mezcla2 = 0.3f, desafinar = 5f,
        ataque = 0.01f, caida = 0.2f, sostener = 0.8f, liberar = 0.15f, corte = 4000f, envFiltro = 2000f,
        resonancia = 0.3f, vibrato = 15f, volumen = 0.3f)
    val FLAUTA = Instrumento("Flauta", onda1 = Onda.SENO, onda2 = Onda.RUIDO, mezcla2 = 0.06f,
        ataque = 0.08f, caida = 0.2f, sostener = 0.9f, liberar = 0.15f, corte = 5000f, vibrato = 12f, volumen = 0.45f)
    val BAJO_SINTE = Instrumento("Bajo sintetizado", onda1 = Onda.SIERRA, onda2 = Onda.CUADRADA, mezcla2 = 0.5f,
        octava2 = -1, ataque = 0.003f, caida = 0.25f, sostener = 0.6f, liberar = 0.1f, corte = 300f,
        envFiltro = 1800f, caidaFiltro = 0.15f, resonancia = 0.35f, volumen = 0.55f)
    val BAJO_ELECTRICO = Instrumento("Bajo eléctrico", TipoSintesis.CUERDA, corte = 0.25f, sostener = 0.997f, volumen = 0.9f)
    val GUITARRA = Instrumento("Guitarra", TipoSintesis.CUERDA, corte = 0.55f, sostener = 0.996f, volumen = 0.7f)
    val ARPA = Instrumento("Arpa", TipoSintesis.CUERDA, corte = 0.8f, sostener = 0.998f, volumen = 0.6f)
    val CAMPANAS = Instrumento("Campanas", TipoSintesis.FM, ataque = 0.001f, caida = 3f, sostener = 0f,
        liberar = 1.5f, fmRatio = 3.5f, fmIndice = 4f, fmCaida = 1.2f, volumen = 0.3f)
    val MARIMBA = Instrumento("Marimba", TipoSintesis.FM, ataque = 0.001f, caida = 0.5f, sostener = 0f,
        liberar = 0.3f, fmRatio = 4f, fmIndice = 1.8f, fmCaida = 0.05f, volumen = 0.55f)

    val TODOS = listOf(PIANO, PIANO_ELECTRICO, ORGANO, PAD, CUERDAS, METALES, LEAD, FLAUTA,
        GUITARRA, ARPA, BAJO_SINTE, BAJO_ELECTRICO, CAMPANAS, MARIMBA)
}
