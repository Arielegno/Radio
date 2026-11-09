package rr.revista

/** Estética de un artículo (columna «style»), validada igual que revSt en la web: solo valores de listas permitidas. */
data class Estilo(
    val th: String = "asfalto", val ft: String = "cartel", val ac: String = "amarillo", val al: String = "l", val dc: Int = 0,
    val sz: String = "m", val cv: String = "a", val stk: String = "", val mh: String = ""
) {
    val claro: Boolean get() = th in CLAROS

    /** Solo lo que cambió respecto de lo normal (revStDiff en la web). */
    fun aMapa(): Map<String, Any?> {
        val d = Estilo(); val m = linkedMapOf<String, Any?>()
        if (th != d.th) m["th"] = th; if (ft != d.ft) m["ft"] = ft; if (ac != d.ac) m["ac"] = ac; if (al != d.al) m["al"] = al
        if (dc != d.dc) m["dc"] = dc; if (sz != d.sz) m["sz"] = sz; if (cv != d.cv) m["cv"] = cv; if (stk != d.stk) m["stk"] = stk; if (mh != d.mh) m["mh"] = mh
        return m
    }

    /** Al elegir una estética de revista se ajustan la letra y el acento (REV_PRE en la web). */
    fun conTema(t: String): Estilo = when (t) {
        "y2k" -> copy(th = t, ft = "retro", ac = "rosa"); "punk" -> copy(th = t, ft = "poster", ac = "rojo"); "clasica" -> copy(th = t, ft = "editorial", ac = "rojo")
        "vintage" -> copy(th = t, ft = "poster", ac = "rojo"); "teen" -> copy(th = t, ft = "bloque", ac = "coral"); "tokio" -> copy(th = t, ft = "burbuja", ac = "rosa")
        "tabloide" -> copy(th = t, ft = "bloque", ac = "azul"); "cromo" -> copy(th = t, ft = "glitch", ac = "azul"); else -> copy(th = t)
    }

    companion object {
        val NOMBRES_TEMA = mapOf("asfalto" to "Asfalto", "senal" to "Señal", "papel" to "Papel", "neon" to "Neón", "atardecer" to "Atardecer", "vinilo" to "Vinilo",
            "y2k" to "Pop Y2K", "punk" to "Fanzine punk", "clasica" to "Rock clásico", "vintage" to "Cartel vintage", "teen" to "Teen 2002", "tokio" to "Tokio kawaii", "tabloide" to "Tabloide", "cromo" to "Cromo 2000")
        val NOMBRES_FUENTE = mapOf("cartel" to "Cartel", "editorial" to "Editorial", "maquina" to "Máquina", "manuscrita" to "Manuscrita", "afiche" to "Afiche", "poster" to "Póster", "retro" to "Retro", "burbuja" to "Burbuja", "glitch" to "Glitch", "bloque" to "Bloque")
        val TEMAS = listOf("asfalto", "senal", "papel", "neon", "atardecer", "vinilo", "y2k", "punk", "clasica", "vintage", "teen", "tokio", "tabloide", "cromo")
        val FUENTES = listOf("cartel", "editorial", "maquina", "manuscrita", "afiche", "poster", "retro", "burbuja", "glitch", "bloque")
        val ACENTOS = mapOf("amarillo" to 0xFFF2C230, "azul" to 0xFF4F8FF0, "coral" to 0xFFFF6B5A, "verde" to 0xFF3ECF8E, "violeta" to 0xFFA78BFA, "rojo" to 0xFFE3242B, "rosa" to 0xFFFF3D8B, "tinta" to 0xFF9AA3B0)
        val CLAROS = setOf("papel", "y2k", "clasica", "vintage", "teen", "tokio", "tabloide", "cromo")
        private fun limpio(s: Any?, n: Int) = (s as? String)?.replace(Regex("[\\u0000-\\u001f<>]"), "")?.trim()?.take(n) ?: ""

        fun de(o: Map<String, Any?>): Estilo {
            var e = Estilo()
            (o["th"] as? String)?.takeIf { it in TEMAS }?.let { e = e.copy(th = it) }
            (o["ft"] as? String)?.takeIf { it in FUENTES }?.let { e = e.copy(ft = it) }
            (o["ac"] as? String)?.takeIf { it in ACENTOS }?.let { e = e.copy(ac = it) }
            (o["al"] as? String)?.takeIf { it == "c" || it == "l" }?.let { e = e.copy(al = it) }
            (o["dc"] as? Number)?.toInt()?.takeIf { it == 0 || it == 1 }?.let { e = e.copy(dc = it) }
            (o["sz"] as? String)?.takeIf { it == "g" || it == "m" }?.let { e = e.copy(sz = it) }
            (o["cv"] as? String)?.takeIf { it in setOf("a", "f", "m") }?.let { e = e.copy(cv = it) }
            e = e.copy(stk = limpio(o["stk"], 24), mh = limpio(o["mh"], 22))
            return e
        }
    }
}

/** Colores de cada estética (los mismos de la hoja de estilos de la web). */
data class Tema(val fondo: List<Long>, val texto: Long, val tenue: Long, val titulo: Long? = null, val contorno: Long? = null, val sombra: Long? = null, val minusculas: Boolean = false, val mayusculas: Boolean = false)

object Temas {
    fun de(e: Estilo): Tema {
        val ac = Estilo.ACENTOS[e.ac] ?: 0xFFF2C230
        return when (e.th) {
            "senal" -> Tema(listOf(0xFF1B4F9C), 0xFFF4F6F8, 0xFFB9CBE6)
            "papel" -> Tema(listOf(0xFFF1E9D8), 0xFF2A2620, 0xFF7A6F5C)
            "neon" -> Tema(listOf(0xFF0B0A14), 0xFFF0EAFF, 0xFF9A8FC2, sombra = ac)
            "atardecer" -> Tema(listOf(0xFF3A1C3B, 0xFFB4452F), 0xFFFFF1E2, 0xFFF0C7A8)
            "vinilo" -> Tema(listOf(0xFF0E0E0E), 0xFFEEEEEE, 0xFF999999)
            "y2k" -> Tema(listOf(0xFFC4F24A, 0xFF3FDCF2, 0xFFFF9A4D), 0xFF111111, 0xFF2C2C2C, titulo = 0xFFFFFFFF, contorno = 0xFF111111, sombra = ac)
            "punk" -> Tema(listOf(0xFF0B0B0B), 0xFFEDEDED, 0xFFA0A0A0, titulo = ac, minusculas = true)
            "clasica" -> Tema(listOf(0xFFF6F3EE), 0xFF161616, 0xFF5B5B5B, titulo = ac, sombra = 0xFF111111)
            "vintage" -> Tema(listOf(0xFFEFE4CC), 0xFF1A1714, 0xFF6B5D45, titulo = 0xFF1A1714, mayusculas = true)
            "teen" -> Tema(listOf(0xFFFFFAF2), 0xFF1C1C1C, 0xFF555555, titulo = 0xFF1C1C1C, minusculas = true)
            "tokio" -> Tema(listOf(0xFFFFF0F6, 0xFFFFE3EF), 0xFF2B1530, 0xFF6B4A6E, titulo = 0xFFFFE14D, contorno = 0xFFFF3D8B, sombra = 0xFF7A2BFF)
            "tabloide" -> Tema(listOf(0xFFFBFBF7), 0xFF111111, 0xFF444444, titulo = 0xFFFFE600, contorno = 0xFFD0021B, sombra = 0xFF111111)
            "cromo" -> Tema(listOf(0xFFEEF2F8, 0xFFB9C6D6, 0xFFF4F7FB, 0xFFA9BACD), 0xFF0F1D2E, 0xFF3B4F66, titulo = 0xFF2F4359)
            else -> Tema(listOf(0xFF1B1E23), 0xFFE6E9ED, 0xFF8D96A3)
        }
    }
}

/* ---------- Diseño avanzado (columna «design»): el mismo formato que guarda la web ---------- */
data class ItemCollage(
    val k: String, val x: Double, val y: Double, val w: Double, val r: Double, val z: Int, val o: Double,
    val c: Long? = null, val s: String = "", val cut: String = "rect", val t: String = "", val v: String = ""
)

data class Diseno(
    val bg: String = "none", val c1: Long = 0xFFFF3D8B, val c2: Long = 0xFF29D3F0, val gt: String = "lin", val ga: Int = 160, val blur: Int = 18,
    val dim: Int = 0, val vin: Boolean = false, val grain: Boolean = false,
    val tf: String = "", val hf: String = "", val fs: Int = 0, val lh: Double = 0.0, val ts: Int = 0, val tc: Long? = null, val hc: Long? = null,
    val ta: String = "", val tsh: Boolean = false, val panel: String = "none", val pop: Int = 70, val pbl: Int = 10, val rad: Int = 16,
    val cpos: String = "fondo", val ch: String = "vertical", val col: List<ItemCollage> = emptyList()
) {
    val vacio: Boolean get() = this == Diseno()

    /** Solo lo que cambió (dzDiff en la web), en el mismo formato que guarda la web. */
    fun aMapa(): Map<String, Any?> {
        val d = Diseno(); val m = linkedMapOf<String, Any?>()
        fun h(c: Long) = "#" + (c and 0xFFFFFF).toString(16).padStart(6, '0')
        if (bg != d.bg) m["bg"] = bg; if (c1 != d.c1) m["c1"] = h(c1); if (c2 != d.c2) m["c2"] = h(c2); if (gt != d.gt) m["gt"] = gt; if (ga != d.ga) m["ga"] = ga
        if (blur != d.blur) m["blur"] = blur; if (dim != d.dim) m["dim"] = dim; if (vin) m["vin"] = 1; if (grain) m["grain"] = 1
        if (tf != d.tf) m["tf"] = tf; if (hf != d.hf) m["hf"] = hf; if (fs != 0) m["fs"] = fs; if (lh != 0.0) m["lh"] = lh; if (ts != 0) m["ts"] = ts
        tc?.let { m["tc"] = h(it) }; hc?.let { m["hc"] = h(it) }; if (ta != d.ta) m["ta"] = ta; if (tsh) m["tsh"] = 1
        if (panel != d.panel) m["panel"] = panel; if (pop != d.pop) m["pop"] = pop; if (pbl != d.pbl) m["pbl"] = pbl; if (rad != d.rad) m["rad"] = rad
        if (cpos != d.cpos) m["cpos"] = cpos; if (ch != d.ch) m["ch"] = ch
        if (col.isNotEmpty()) m["col"] = col.map { i ->
            val o = linkedMapOf<String, Any?>("k" to i.k, "x" to i.x, "y" to i.y, "w" to i.w, "r" to i.r, "z" to i.z, "o" to i.o)
            i.c?.let { o["c"] = h(it) }
            when (i.k) { "img" -> { o["s"] = i.s; o["cut"] = i.cut }; "txt" -> { o["t"] = i.t; o["v"] = i.v }; "stk" -> { o["v"] = i.v; o["t"] = i.t }; else -> o["v"] = i.v }
            o
        }
        return m
    }

    companion object {
        val NOMBRES_FONDO = mapOf("none" to "Ninguno", "cover" to "Tu portada difuminada", "color" to "Mis colores", "aurora" to "Aurora", "atardecer" to "Atardecer", "neon" to "Neón", "oceano" to "Océano",
            "fuego" to "Fuego", "noche" to "Noche estrellada", "grunge" to "Grunge", "vinilo" to "Vinilo", "rayas" to "Rayas", "menta" to "Menta", "pastel" to "Arcoíris pastel", "cromo" to "Cromo 2000",
            "y2k" to "Pop Y2K", "tokio" to "Tokio kawaii", "trama" to "Trama punk", "cuadros" to "Cuadros", "periodico" to "Periódico")
        val NOMBRES_LETRA = mapOf("" to "Del tema", "sistema" to "Normal", "lora" to "Lora", "playfair" to "Elegante", "mono" to "Máquina", "caveat" to "Manuscrita", "abril" to "Abril", "anton" to "Cartel",
            "shrikhand" to "Retro", "rampart" to "Burbuja", "glitch" to "Glitch", "archivo" to "Bloque")
        val NOMBRES_PANEL = mapOf("none" to "Sin panel", "vidrio" to "Vidrio", "negro" to "Oscuro", "blanco" to "Claro", "papel" to "Papel", "cinta" to "Recorte")
        val FONDOS = listOf("none", "cover", "color", "aurora", "atardecer", "neon", "oceano", "fuego", "noche", "grunge", "vinilo", "rayas", "menta", "pastel", "cromo", "y2k", "tokio", "trama", "cuadros", "periodico")
        val FONDOS_CLAROS = setOf("menta", "pastel", "cromo", "y2k", "tokio", "trama", "periodico")
        val LETRAS = listOf("", "sistema", "lora", "playfair", "mono", "caveat", "abril", "anton", "shrikhand", "rampart", "glitch", "archivo")
        val PANELES = listOf("none", "vidrio", "negro", "blanco", "papel", "cinta")
        val CORTES = listOf("rect", "roto", "polaroid", "circulo", "ovalo", "estrella", "corazon")
        val TEXTOS = listOf("recorte", "titular", "maquina", "marcador", "neon", "cinta")
        val STICKERS = listOf("estallido", "circulo", "etiqueta", "estrella", "chispa", "corazon", "rayo", "flor", "nota", "calavera", "paz")
        val GLIFOS = mapOf("estrella" to "★", "chispa" to "✦", "corazon" to "♥", "rayo" to "⚡", "flor" to "✿", "nota" to "♫", "calavera" to "☠", "paz" to "☮")
        val FORMAS = listOf("circulo", "franja", "trama", "rayas", "cuadros", "spray", "marco")
        val CINTAS = mapOf("beige" to 0xD9EDE5CF, "rosa" to 0xBFFF80B4, "azul" to 0xB378B4FF, "negra" to 0xE6141414, "amarilla" to 0xCCF2C230)

        fun hex(v: Any?): Long? = (v as? String)?.takeIf { Regex("^#[0-9a-fA-F]{6}$").matches(it) }?.let { 0xFF000000 or it.substring(1).toLong(16) }
        private fun num(v: Any?, a: Double, b: Double, d: Double): Double = ((v as? Number)?.toDouble() ?: (v as? String)?.toDoubleOrNull())?.takeIf { it.isFinite() }?.coerceIn(a, b) ?: d
        private fun uno(v: Any?, l: Collection<String>, d: String): String = (v as? String)?.takeIf { it in l } ?: d

        /** Imágenes permitidas: las de tu almacén de la Revista, Wikimedia o una JPEG incrustada. */
        fun imagenOk(u: String?): String {
            if (u == null) return ""
            if (Regex("^data:image/jpeg;base64,[A-Za-z0-9+/=]+$").matches(u)) return u
            if (Regex("^https://[a-z0-9-]+\\.supabase\\.co/storage/v1/object/public/revista/[\\w/.-]+$", RegexOption.IGNORE_CASE).matches(u)) return u
            if (Regex("^https://upload\\.wikimedia\\.org/[^\\s\"'<>]+$").matches(u)) return u
            return ""
        }

        private fun item(o: Map<String, Any?>): ItemCollage? {
            val k = (o["k"] as? String)?.takeIf { it in setOf("img", "txt", "stk", "tape", "shp") } ?: return null
            val base = ItemCollage(k, num(o["x"], -20.0, 120.0, 50.0), num(o["y"], -20.0, 120.0, 50.0), num(o["w"], 2.0, 140.0, 30.0),
                num(o["r"], -180.0, 180.0, 0.0), num(o["z"], 0.0, 99.0, 1.0).toInt(), num(o["o"], 10.0, 100.0, 100.0), hex(o["c"]))
            return when (k) {
                "img" -> imagenOk(o["s"] as? String).takeIf { it.isNotEmpty() }?.let { base.copy(s = it, cut = uno(o["cut"], CORTES, "rect")) }
                "txt" -> base.copy(t = ((o["t"] as? String) ?: "").replace(Regex("[\\u0000-\\u0009\\u000b-\\u001f<>]"), "").split('\n').take(3).joinToString("\n").trim().take(60).ifEmpty { "Texto" }, v = uno(o["v"], TEXTOS, "recorte"))
                "stk" -> base.copy(v = uno(o["v"], STICKERS, "estallido"), t = ((o["t"] as? String) ?: "").replace(Regex("[\\u0000-\\u001f<>]"), "").trim().take(24))
                "tape" -> base.copy(v = uno(o["v"], CINTAS.keys, "beige"))
                else -> base.copy(v = uno(o["v"], FORMAS, "circulo"))
            }
        }

        fun de(o: Map<String, Any?>): Diseno {
            val d = Diseno()
            return d.copy(
                bg = uno(o["bg"], FONDOS, "none"), c1 = hex(o["c1"]) ?: d.c1, c2 = hex(o["c2"]) ?: d.c2, gt = uno(o["gt"], listOf("lin", "rad", "con"), "lin"),
                ga = num(o["ga"], 0.0, 360.0, 160.0).toInt(), blur = num(o["blur"], 0.0, 40.0, 18.0).toInt(), dim = num(o["dim"], 0.0, 90.0, 0.0).toInt(),
                vin = o["vin"] == 1L || o["vin"] == true, grain = o["grain"] == 1L || o["grain"] == true,
                tf = uno(o["tf"], LETRAS, ""), hf = uno(o["hf"], LETRAS, ""),
                fs = if (o["fs"] != null) num(o["fs"], 13.0, 28.0, 0.0).toInt() else 0, lh = if (o["lh"] != null) num(o["lh"], 1.2, 2.2, 0.0) else 0.0,
                ts = if (o["ts"] != null) num(o["ts"], 20.0, 80.0, 0.0).toInt() else 0, tc = hex(o["tc"]), hc = hex(o["hc"]),
                ta = uno(o["ta"], listOf("", "l", "j", "c"), ""), tsh = o["tsh"] == 1L || o["tsh"] == true,
                panel = uno(o["panel"], PANELES, "none"), pop = num(o["pop"], 0.0, 100.0, 70.0).toInt(), pbl = num(o["pbl"], 0.0, 30.0, 10.0).toInt(), rad = num(o["rad"], 0.0, 40.0, 16.0).toInt(),
                cpos = if (o["cpos"] == "cabecera") "cabecera" else "fondo", ch = uno(o["ch"], listOf("vertical", "cuadrado", "horizontal"), "vertical"),
                col = ((o["col"] as? List<*>) ?: emptyList<Any?>()).take(30).mapNotNull { (it as? Map<*, *>)?.let { m -> @Suppress("UNCHECKED_CAST") item(m as Map<String, Any?>) } }
            )
        }

        private fun lum(c: Long): Double = (((c shr 16) and 255) * .299 + ((c shr 8) and 255) * .587 + (c and 255) * .114) / 255.0
        /** ¿El fondo es claro? (para elegir el color del texto) — null si no hay fondo propio. */
        fun fondoClaro(d: Diseno, hayPortada: Boolean): Boolean? {
            var l: Boolean? = when (d.bg) { "none" -> null; "color" -> (lum(d.c1) + lum(d.c2)) / 2 > 0.6; "cover" -> if (hayPortada) false else null; else -> d.bg in FONDOS_CLAROS }
            if (l != null && d.dim >= 35) l = false
            return l
        }
        /** Colores finales del título y del texto, como dzApply en la web. */
        fun colores(d: Diseno, hayPortada: Boolean): Pair<Long?, Long?> {
            val claro = fondoClaro(d, hayPortada)
            val hc = d.hc ?: claro?.let { if (it) 0xFF141414 else 0xFFF4F4F4 }
            val tc = d.tc ?: when {
                d.panel in setOf("papel", "blanco") -> 0xFF1B1B1B
                d.panel in setOf("negro", "vidrio") && claro != true -> 0xFFF2F2F2
                d.panel == "vidrio" && claro == true -> 0xFF141414
                else -> hc
            }
            return hc to tc
        }
    }
}
