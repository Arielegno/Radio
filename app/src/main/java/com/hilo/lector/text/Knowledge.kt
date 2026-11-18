package com.hilo.lector.text

import java.time.DayOfWeek
import java.time.LocalDate

/** Ficha de un autor clásico de las ciencias sociales (información estable, para comparar y ubicar). */
data class AuthorCard(
    val key: String,
    val name: String,
    val years: String,
    val paradigm: String,
    val works: String,
    val concepts: String,
    val method: String,
    val society: String,
    val change: String,
    val individual: String,
)

data class Paradigm(val name: String, val idea: String, val authors: List<String>)
data class School(val name: String, val when_: String, val traits: String, val authors: String)
data class HistEvent(val year: Int, val text: String, val place: String)

/** Conocimiento básico de Sociología que Hilo trae incorporado (sin internet). */
object SocioKb {
    val authors: List<AuthorCard> = listOf(
        AuthorCard("comte", "Auguste Comte", "1798–1857", "Positivismo", "Curso de filosofía positiva (1830–1842)",
            "ley de los tres estadios, estática y dinámica social, positivismo",
            "Observación, comparación y experimentación, como en las ciencias naturales.",
            "Un organismo ordenado por el consenso de ideas.", "Progreso por estadios: teológico, metafísico y positivo.",
            "Subordinado al orden social."),
        AuthorCard("marx", "Karl Marx", "1818–1883", "Teoría del conflicto (materialismo histórico)", "Manifiesto comunista (1848); El capital (1867)",
            "modo de producción, lucha de clases, plusvalía, alienación, ideología",
            "Materialismo histórico y dialéctico: partir de las condiciones materiales.",
            "La base económica condiciona la superestructura (derecho, política, ideas).",
            "Por las contradicciones del modo de producción y la lucha de clases; la revolución.",
            "Lo define su posición de clase; puede alienarse en el trabajo."),
        AuthorCard("durkheim", "Émile Durkheim", "1858–1917", "Funcionalismo", "La división del trabajo social (1893); Las reglas del método sociológico (1895); El suicidio (1897)",
            "hecho social, solidaridad mecánica y orgánica, anomia, conciencia colectiva",
            "Tratar los hechos sociales como cosas; estadística y comparación.",
            "Realidad propia que se impone al individuo y lo integra.",
            "Crece la división del trabajo: de la solidaridad mecánica a la orgánica.",
            "Moldeado por la conciencia colectiva; sin normas cae en anomia."),
        AuthorCard("weber", "Max Weber", "1864–1920", "Sociología comprensiva", "La ética protestante y el espíritu del capitalismo (1905); Economía y sociedad (1922)",
            "acción social, tipo ideal, racionalización, burocracia, dominación legal, tradicional y carismática",
            "Comprender (Verstehen) el sentido de la acción; tipos ideales.",
            "Entramado de acciones con sentido; órdenes de dominación legítima.",
            "Racionalización y burocratización; las ideas (como la religión) también mueven la historia.",
            "Actor que da sentido a su acción."),
        AuthorCard("simmel", "Georg Simmel", "1858–1918", "Sociología formal", "Filosofía del dinero (1900); La metrópolis y la vida mental (1903)",
            "formas de socialización, interacción, el extranjero, la moda, el dinero",
            "Estudiar las formas de interacción más que sus contenidos.",
            "Red de interacciones entre individuos.", "La economía del dinero y la gran ciudad cambian la vida mental.",
            "Se protege de los estímulos de la ciudad con indiferencia (actitud blasé)."),
        AuthorCard("mead", "George H. Mead", "1863–1931", "Interaccionismo simbólico", "Espíritu, persona y sociedad (1934)",
            "self, el yo y el mí, otro generalizado, símbolos significantes",
            "Estudiar la interacción y los símbolos que se comparten.",
            "Surge de la interacción simbólica.", "Cambia cuando cambian los significados compartidos.",
            "La persona (self) se forma al ponerse en el lugar del otro."),
        AuthorCard("parsons", "Talcott Parsons", "1902–1979", "Funcionalismo estructural", "La estructura de la acción social (1937); El sistema social (1951)",
            "sistema social, esquema AGIL, rol, normas, socialización",
            "Teoría general de los sistemas de acción.", "Sistema que tiende al equilibrio y cumple funciones.",
            "Diferenciación y adaptación graduales.", "Ocupa roles e interioriza las normas."),
        AuthorCard("merton", "Robert K. Merton", "1910–2003", "Funcionalismo de alcance medio", "Teoría y estructura sociales (1949)",
            "funciones manifiestas y latentes, disfunción, anomia como desfase entre metas y medios, profecía autocumplida",
            "Teorías de alcance medio, comprobables con datos.", "Estructuras con funciones y también disfunciones.",
            "Por tensiones entre metas culturales y medios legítimos.", "Se adapta: conformidad, innovación, ritualismo, retraimiento o rebelión."),
        AuthorCard("elias", "Norbert Elias", "1897–1990", "Sociología figuracional", "El proceso de la civilización (1939)",
            "figuración, proceso civilizatorio, autocontrol, interdependencia",
            "Análisis histórico de largo plazo.", "Figuraciones de personas interdependientes.",
            "Proceso civilizatorio: más autocontrol y monopolio estatal de la violencia.", "No hay individuo sin sociedad: es interdependiente."),
        AuthorCard("goffman", "Erving Goffman", "1922–1982", "Interaccionismo simbólico (dramatúrgico)", "La presentación de la persona en la vida cotidiana (1959); Estigma (1963)",
            "dramaturgia, fachada, rol, estigma, institución total",
            "Observación etnográfica de la vida cotidiana.", "Escenarios donde se representan papeles.",
            "Cambia el orden de la interacción.", "Actor que gestiona la impresión que da."),
        AuthorCard("berger", "Peter Berger y Thomas Luckmann", "1966", "Construccionismo social", "La construcción social de la realidad (1966)",
            "institucionalización, legitimación, socialización primaria y secundaria, sociología del conocimiento",
            "Fenomenología de la vida cotidiana.", "Realidad construida socialmente que se vuelve objetiva.",
            "Cuando cambian las legitimaciones y las instituciones.", "Internaliza el mundo social al socializarse."),
        AuthorCard("habermas", "Jürgen Habermas", "1929–", "Teoría crítica (Escuela de Frankfurt)", "Teoría de la acción comunicativa (1981)",
            "acción comunicativa, mundo de la vida, sistema, esfera pública, colonización",
            "Reconstrucción racional; teoría crítica.", "Mundo de la vida y sistemas (dinero y poder).",
            "Racionalización comunicativa frente a la colonización del mundo de la vida.", "Hablante capaz de argumentar y llegar a acuerdos."),
        AuthorCard("foucault", "Michel Foucault", "1926–1984", "Posestructuralismo", "Vigilar y castigar (1975); Historia de la sexualidad (1976)",
            "poder y saber, disciplina, panóptico, biopoder, discurso",
            "Arqueología y genealogía de los saberes.", "Red de relaciones de poder que atraviesa todo.",
            "Rupturas entre regímenes de saber y poder.", "Sujeto producido por el poder y los discursos."),
        AuthorCard("bourdieu", "Pierre Bourdieu", "1930–2002", "Estructuralismo constructivista", "La reproducción (1970); La distinción (1979)",
            "habitus, campo, capital económico, cultural, social y simbólico, violencia simbólica",
            "Reflexividad; encuestas y etnografía combinadas.", "Espacio de campos donde se lucha por capitales.",
            "Luchas dentro de los campos, aunque tienden a reproducirse.", "Agente con un habitus que incorporó de su clase."),
        AuthorCard("luhmann", "Niklas Luhmann", "1927–1998", "Teoría de sistemas", "Sistemas sociales (1984)",
            "autopoiesis, comunicación, diferenciación funcional, entorno",
            "Teoría de sistemas autorreferentes.", "Sistemas de comunicación que se reproducen solos.",
            "Diferenciación funcional creciente.", "Está en el entorno del sistema social, no dentro."),
        AuthorCard("giddens", "Anthony Giddens", "1938–", "Teoría de la estructuración", "La constitución de la sociedad (1984); Consecuencias de la modernidad (1990)",
            "estructuración, dualidad de la estructura, reflexividad, modernidad tardía",
            "Integrar acción y estructura.", "Las estructuras se producen y reproducen en la acción.",
            "Modernidad reflexiva y globalización.", "Agente que conoce y puede transformar las estructuras."),
        AuthorCard("bauman", "Zygmunt Bauman", "1925–2017", "Sociología de la modernidad", "Modernidad líquida (2000)",
            "modernidad líquida, consumo, incertidumbre, individualización",
            "Ensayo sociológico interpretativo.", "Lazos frágiles y cambiantes.", "De la modernidad sólida a la líquida.",
            "Individuo consumidor, con más libertad y más inseguridad."),
        AuthorCard("quijano", "Aníbal Quijano", "1928–2018", "Pensamiento decolonial latinoamericano", "Colonialidad del poder (1992; 2000)",
            "colonialidad del poder, raza, eurocentrismo, sistema-mundo",
            "Crítica histórico-estructural desde América Latina.", "Patrón mundial de poder nacido de la colonización.",
            "Descolonización del poder y del saber.", "Clasificado socialmente por la idea de raza."),
    )

    fun find(key: String): AuthorCard? = authors.firstOrNull { it.key == key || it.name.lowercase().contains(key.lowercase()) }

    val paradigms = listOf(
        Paradigm("Funcionalismo", "La sociedad es un sistema cuyas partes cumplen funciones para mantener el equilibrio.", listOf("comte", "durkheim", "parsons", "merton", "luhmann")),
        Paradigm("Teoría del conflicto", "La sociedad está atravesada por desigualdades y luchas de intereses que producen el cambio.", listOf("marx", "weber", "habermas", "quijano")),
        Paradigm("Interaccionismo simbólico", "La sociedad se crea en las interacciones cotidianas y los significados compartidos.", listOf("mead", "goffman", "simmel")),
        Paradigm("Construccionismo y estructuración", "Estructura y acción se producen mutuamente; la realidad social se construye.", listOf("berger", "bourdieu", "giddens", "elias")),
        Paradigm("Posestructuralismo y modernidad", "Poder, discurso y modernidad tardía: identidades y saberes son históricos y cambiantes.", listOf("foucault", "bauman")),
    )

    val schools = listOf(
        School("Escuela de Frankfurt", "1923 en adelante", "Teoría crítica: une marxismo y psicoanálisis; crítica de la cultura de masas y la razón instrumental.", "Adorno, Horkheimer, Marcuse, Habermas"),
        School("Escuela de Chicago", "1920–1950", "Sociología urbana y estudios de campo; la ciudad como laboratorio; origen del interaccionismo.", "Park, Burgess, Thomas, Blumer"),
        School("Escuela de los Annales", "1929 en adelante", "Historia de larga duración, mentalidades y estructuras sociales y económicas.", "Bloch, Febvre, Braudel"),
        School("Escuela de Birmingham", "1964 en adelante", "Estudios culturales: cultura popular, juventud, medios, clase y resistencia.", "Hoggart, Williams, Hall"),
        School("Teoría de la dependencia (CEPAL)", "1950–1970", "El subdesarrollo de América Latina se explica por su lugar subordinado en la economía mundial.", "Prebisch, Cardoso, Faletto, Gunder Frank"),
        School("Pensamiento decolonial", "1990 en adelante", "Colonialidad del poder, del saber y del ser; crítica del eurocentrismo.", "Quijano, Dussel, Mignolo, Lugones"),
    )

    /** Contexto histórico para la línea de tiempo (mundo, América Latina y Honduras). */
    val history = listOf(
        HistEvent(1776, "Independencia de Estados Unidos", "Mundo"),
        HistEvent(1789, "Revolución Francesa", "Mundo"),
        HistEvent(1821, "Independencia de Centroamérica", "Honduras"),
        HistEvent(1848, "Revoluciones en Europa; Manifiesto comunista", "Mundo"),
        HistEvent(1870, "Segunda Revolución Industrial", "Mundo"),
        HistEvent(1876, "Reforma Liberal en Honduras (Marco Aurelio Soto)", "Honduras"),
        HistEvent(1914, "Primera Guerra Mundial (1914–1918)", "Mundo"),
        HistEvent(1917, "Revolución Rusa", "Mundo"),
        HistEvent(1929, "Gran Depresión", "Mundo"),
        HistEvent(1939, "Segunda Guerra Mundial (1939–1945)", "Mundo"),
        HistEvent(1948, "Declaración Universal de los Derechos Humanos", "Mundo"),
        HistEvent(1954, "Gran Huelga Bananera", "Honduras"),
        HistEvent(1959, "Revolución Cubana", "América Latina"),
        HistEvent(1968, "Mayo francés y movimientos estudiantiles", "Mundo"),
        HistEvent(1969, "Guerra entre Honduras y El Salvador", "Honduras"),
        HistEvent(1982, "Constitución vigente de Honduras", "Honduras"),
        HistEvent(1989, "Caída del Muro de Berlín", "Mundo"),
        HistEvent(1998, "Huracán Mitch", "Honduras"),
        HistEvent(2008, "Crisis financiera mundial", "Mundo"),
        HistEvent(2009, "Crisis política y golpe de Estado en Honduras", "Honduras"),
        HistEvent(2020, "Pandemia de COVID-19 y tormentas Eta e Iota", "Mundo"),
    )

    /** Filas fijas del cuadro comparativo profundo. */
    val compareRows = listOf(
        "Paradigma" to { a: AuthorCard -> a.paradigm },
        "Obra clave" to { a: AuthorCard -> a.works },
        "Conceptos" to { a: AuthorCard -> a.concepts },
        "Método" to { a: AuthorCard -> a.method },
        "Qué es la sociedad" to { a: AuthorCard -> a.society },
        "Cómo cambia" to { a: AuthorCard -> a.change },
        "El individuo" to { a: AuthorCard -> a.individual },
    )
}

/** Jerarquía de las normas (pirámide de Kelsen), con los nombres usados en Honduras. */
object NormLevels {
    val levels = listOf(
        "Constitución de la República" to listOf("constituci"),
        "Tratados y convenios internacionales" to listOf("tratado", "convenci", "convenio", "pacto", "declaraci"),
        "Leyes y códigos (Congreso Nacional)" to listOf("código", "codigo", "ley "),
        "Reglamentos y decretos ejecutivos" to listOf("reglamento", "decreto ejecutivo", "acuerdo ejecutivo"),
        "Ordenanzas municipales y resoluciones" to listOf("ordenanza", "resoluci", "circular"),
        "Normas individuales: sentencias y contratos" to listOf("sentencia", "contrato", "fallo"),
    )

    fun levelOf(title: String): Int {
        val t = title.lowercase()
        return levels.indexOfFirst { (_, keys) -> keys.any { t.contains(it) } }
    }
}

/** Cómputo de plazos: días hábiles, días calendario, meses y años, con feriados de Honduras. */
object Plazos {
    /** Domingo de Pascua (algoritmo de Gauss/Meeus). */
    fun easter(y: Int): LocalDate {
        val a = y % 19; val b = y / 100; val c = y % 100; val d = b / 4; val e = b % 4
        val f = (b + 8) / 25; val g = (b - f + 1) / 3; val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4; val k = c % 4; val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(y, month, day)
    }

    /** Feriados nacionales de Honduras (fijos y Semana Santa; la Semana Morazánica se toma del 1 al 3 de octubre). */
    fun holidays(y: Int): Map<LocalDate, String> {
        val e = easter(y)
        return mapOf(
            LocalDate.of(y, 1, 1) to "Año Nuevo",
            e.minusDays(3) to "Jueves Santo",
            e.minusDays(2) to "Viernes Santo",
            LocalDate.of(y, 4, 14) to "Día de las Américas",
            LocalDate.of(y, 5, 1) to "Día del Trabajo",
            LocalDate.of(y, 9, 15) to "Independencia",
            LocalDate.of(y, 10, 1) to "Semana Morazánica",
            LocalDate.of(y, 10, 2) to "Semana Morazánica",
            LocalDate.of(y, 10, 3) to "Semana Morazánica",
            LocalDate.of(y, 12, 25) to "Navidad",
        )
    }

    data class Result(val end: LocalDate, val skipped: List<Pair<LocalDate, String>>)

    /**
     * Vencimiento de un plazo que empieza a correr el día siguiente a [start].
     * unit: 0 días hábiles, 1 días calendario, 2 meses, 3 años.
     */
    fun compute(start: LocalDate, amount: Int, unit: Int, extra: Set<LocalDate> = emptySet()): Result {
        val skipped = ArrayList<Pair<LocalDate, String>>()
        fun nonWorking(d: LocalDate): String? {
            if (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) return "fin de semana"
            holidays(d.year)[d]?.let { return it }
            if (d in extra) return "día inhábil agregado"
            return null
        }
        var end = when (unit) {
            0 -> {
                var d = start; var n = 0
                while (n < amount) {
                    d = d.plusDays(1)
                    val why = nonWorking(d)
                    if (why == null) n++ else skipped.add(d to why)
                }
                d
            }
            1 -> start.plusDays(amount.toLong())
            2 -> start.plusMonths(amount.toLong())
            else -> start.plusYears(amount.toLong())
        }
        // Si vence en un día inhábil, pasa al siguiente hábil
        if (unit != 0) while (true) { val why = nonWorking(end) ?: break; skipped.add(end to "$why (vence el siguiente día hábil)"); end = end.plusDays(1) }
        return Result(end, skipped)
    }

    private val NUMS = mapOf(
        "un" to 1, "una" to 1, "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5, "seis" to 6, "siete" to 7,
        "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12, "trece" to 13, "catorce" to 14, "quince" to 15,
        "veinte" to 20, "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50, "sesenta" to 60, "noventa" to 90, "cien" to 100,
    )

    /** "treinta (30) días hábiles" → (30, 0). */
    fun parse(amount: String): Pair<Int, Int>? {
        val t = amount.lowercase()
        val n = Regex("\\d{1,3}").find(t)?.value?.toInt() ?: NUMS.entries.firstOrNull { (k, _) -> Regex("(?<![\\p{L}])$k(?![\\p{L}])").containsMatchIn(t) }?.value ?: return null
        val unit = when {
            t.contains("hábil") || t.contains("habil") -> 0
            t.contains("mes") -> 2
            t.contains("año") || t.contains("ano") -> 3
            else -> 1
        }
        return n to unit
    }
}
