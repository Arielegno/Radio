package io.github.arielegno.radio.core

import java.time.LocalDate
import java.util.Locale

class SongStat(var n: String, var a: String, var c: Int = 0, val p: MutableMap<String, Int> = linkedMapOf())
class GeoPt(var c: Int = 0, var a: String = "", val s: MutableMap<String, Int> = linkedMapOf())
class MonthHist(
    var c: Int = 0,
    val s: MutableMap<String, Int> = linkedMapOf(),
    val a: MutableMap<String, Int> = linkedMapOf(),
    val p: MutableMap<String, Int> = linkedMapOf(),
    val d: MutableSet<Int> = sortedSetOf(),
    val n: MutableMap<String, Pair<String, String>> = linkedMapOf(),
    val g: MutableMap<String, GeoPt> = linkedMapOf()
)

data class Resumen(
    val c: Int, val days: Int,
    val s: Map<String, Int>, val a: Map<String, Int>, val p: Map<String, Int>,
    val n: Map<String, Pair<String, String>>
) {
    fun topArtistas(k: Int) = a.entries.sortedByDescending { it.value }.take(k).map { it.key to it.value }
    fun topCanciones(k: Int) = s.entries.sortedByDescending { it.value }.take(k).map { (n[it.key] ?: (it.key to "")) to it.value }
    val minutos: Int get() = Math.round(c * 3.6).toInt()
}

data class PuntoMapa(val lat: Double, val lon: Double, val c: Int, val lugar: String, val canciones: List<Pair<String, Int>>)

/** Favoritas, más escuchadas, historial por mes (para «Tu resumen») y puntos del mapa de viajes. */
class Estadisticas {
    val stats = linkedMapOf<String, SongStat>()
    val hist = linkedMapOf<String, MonthHist>()
    val favs = linkedMapOf<String, Pair<String, String>>()

    fun isFav(name: String, artist: String) = favs.containsKey(keyOf(name, artist))
    fun toggleFav(name: String, artist: String): Boolean {
        val k = keyOf(name, artist)
        return if (favs.remove(k) != null) false else { favs[k] = name to artist; true }
    }

    /** Se llama cuando una canción lleva 30 segundos sonando. */
    fun countPlay(name: String, artist: String, place: String?, lat: Double?, lon: Double?, today: LocalDate) {
        val k = keyOf(name, artist); val pl = place?.takeIf { it.isNotBlank() } ?: "Sin ubicación"
        val st = stats.getOrPut(k) { SongStat(name, artist) }
        st.c++; st.p[pl] = (st.p[pl] ?: 0) + 1
        val mk = String.format(Locale.ROOT, "%04d-%02d", today.year, today.monthValue)
        val e = hist.getOrPut(mk) { MonthHist() }
        e.c++; e.s[k] = (e.s[k] ?: 0) + 1; e.a[artist] = (e.a[artist] ?: 0) + 1; e.p[pl] = (e.p[pl] ?: 0) + 1; e.d.add(today.dayOfMonth)
        if (!e.n.containsKey(k)) e.n[k] = name.take(80) to artist.take(80)
        if (lat != null && lon != null && lat.isFinite() && lon.isFinite()) {
            val gk = String.format(Locale.ROOT, "%.2f,%.2f", lat, lon)
            val gp = e.g.getOrPut(gk) { GeoPt(0, pl) }
            gp.c++; gp.a = pl; gp.s[k] = (gp.s[k] ?: 0) + 1
            if (e.g.size > 400) e.g.entries.minByOrNull { it.value.c }?.let { e.g.remove(it.key) }
        }
        // Se guardan como máximo 24 meses de historial
        while (hist.size > 24) hist.remove(hist.keys.sorted().first())
    }

    /** range: "month", "year" o "all". */
    fun resumen(range: String, today: LocalDate): Resumen? {
        val y = today.year.toString(); val m = String.format(Locale.ROOT, "%04d-%02d", today.year, today.monthValue)
        val meses = when (range) { "month" -> listOf(m); "year" -> hist.keys.filter { it.startsWith(y) }; else -> emptyList() }
        val s = linkedMapOf<String, Int>(); val a = linkedMapOf<String, Int>(); val p = linkedMapOf<String, Int>(); val n = linkedMapOf<String, Pair<String, String>>()
        var c = 0; var days = 0
        if (range == "all" || meses.none { hist.containsKey(it) }) {
            if (range != "all") return null
            for ((k, e) in stats) { c += e.c; s[k] = e.c; n[k] = e.n to e.a; a[e.a] = (a[e.a] ?: 0) + e.c; for ((pl, x) in e.p) p[pl] = (p[pl] ?: 0) + x }
            return if (c > 0) Resumen(c, 0, s, a, p, n) else null
        }
        for (mk in meses) {
            val e = hist[mk] ?: continue
            c += e.c; days += e.d.size
            for ((k, x) in e.s) s[k] = (s[k] ?: 0) + x
            for ((k, x) in e.a) a[k] = (a[k] ?: 0) + x
            for ((k, x) in e.p) p[k] = (p[k] ?: 0) + x
            n.putAll(e.n)
        }
        return if (c > 0) Resumen(c, days, s, a, p, n) else null
    }

    fun puntosMapa(): List<PuntoMapa> {
        class Acc(var c: Int = 0, var a: String = "", val s: MutableMap<String, Int> = linkedMapOf(), val n: MutableMap<String, Pair<String, String>> = linkedMapOf())
        val pts = linkedMapOf<String, Acc>()
        for (e in hist.values) for ((k, g) in e.g) {
            val p = pts.getOrPut(k) { Acc(0, g.a) }
            p.c += g.c; if (g.a.isNotEmpty()) p.a = g.a
            for ((sk, c) in g.s) { p.s[sk] = (p.s[sk] ?: 0) + c; e.n[sk]?.let { p.n[sk] = it } }
        }
        return pts.mapNotNull { (k, p) ->
            val parts = k.split(','); val lat = parts.getOrNull(0)?.toDoubleOrNull(); val lon = parts.getOrNull(1)?.toDoubleOrNull()
            if (lat == null || lon == null) null
            else PuntoMapa(lat, lon, p.c, p.a, p.s.entries.sortedByDescending { it.value }.take(3).map { (sk, c) -> (p.n[sk]?.let { "${it.first} — ${it.second}" } ?: sk) to c })
        }
    }

    fun masEscuchadas(k: Int): List<SongStat> = stats.values.sortedByDescending { it.c }.take(k)

    /** Cuando cambia el nombre o el artista de una canción, sus escuchas y su favorita la siguen. */
    fun migrar(nombreAnt: String, artistaAnt: String, nombre: String, artista: String) {
        val ka = keyOf(nombreAnt, artistaAnt); val kn = keyOf(nombre, artista)
        if (ka == kn) return
        stats.remove(ka)?.let { e ->
            val d = stats.getOrPut(kn) { SongStat(nombre, artista) }
            d.n = nombre; d.a = artista; d.c += e.c; for ((pl, c) in e.p) d.p[pl] = (d.p[pl] ?: 0) + c
        }
        for (h in hist.values) {
            h.s.remove(ka)?.let { h.s[kn] = (h.s[kn] ?: 0) + it }
            h.n.remove(ka); if (h.s.containsKey(kn)) h.n[kn] = nombre to artista
            if (artistaAnt != artista) h.a.remove(artistaAnt)?.let { h.a[artista] = (h.a[artista] ?: 0) + it }
            for (g in h.g.values) g.s.remove(ka)?.let { g.s[kn] = (g.s[kn] ?: 0) + it }
        }
        if (favs.remove(ka) != null) favs[kn] = nombre to artista
    }

    /** Lugares donde más sonó tu música y sus canciones más escuchadas ahí. */
    fun porUbicacion(k: Int = 5): List<Pair<String, List<Triple<String, String, Int>>>> {
        val m = linkedMapOf<String, MutableList<Triple<String, String, Int>>>()
        for (e in stats.values) for ((pl, c) in e.p) if (pl != "Sin ubicación") m.getOrPut(pl) { mutableListOf() } += Triple(e.n, e.a, c)
        return m.entries.sortedByDescending { en -> en.value.sumOf { it.third } }.map { (pl, l) -> pl to l.sortedByDescending { it.third }.take(k) }
    }

    fun clear() { stats.clear(); hist.clear() }

    fun toJson(): String = Json.write(linkedMapOf(
        "v" to 1,
        "stats" to stats.mapValues { (_, e) -> linkedMapOf("n" to e.n, "a" to e.a, "c" to e.c, "p" to e.p) },
        "hist" to hist.mapValues { (_, e) -> linkedMapOf("c" to e.c, "s" to e.s, "a" to e.a, "p" to e.p, "d" to e.d.toList(),
            "n" to e.n.mapValues { listOf(it.value.first, it.value.second) },
            "g" to e.g.mapValues { (_, g) -> linkedMapOf("c" to g.c, "a" to g.a, "s" to g.s) }) },
        "favs" to favs.mapValues { linkedMapOf("n" to it.value.first, "a" to it.value.second) }
    ))

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun ints(m: Any?): MutableMap<String, Int> {
            val out = linkedMapOf<String, Int>(); (m.asObj())?.forEach { (k, v) -> (v as? Number)?.let { out[k] = it.toInt() } }; return out
        }

        fun fromJson(text: String?): Estadisticas {
            val e = Estadisticas(); val o = Json.obj(text)
            o.map("stats").forEach { (k, v) -> val m = v.asObj() ?: return@forEach; e.stats[k] = SongStat(m.str("n"), m.str("a"), m.num("c").toInt(), ints(m["p"])) }
            o.map("hist").forEach { (k, v) ->
                val m = v.asObj() ?: return@forEach
                val h = MonthHist(m.num("c").toInt(), ints(m["s"]), ints(m["a"]), ints(m["p"]))
                m.list("d").forEach { (it as? Number)?.let { d -> h.d.add(d.toInt()) } }
                m.map("n").forEach { (nk, nv) -> val l = nv as? List<*> ?: return@forEach; h.n[nk] = (l.getOrNull(0)?.toString() ?: "") to (l.getOrNull(1)?.toString() ?: "") }
                m.map("g").forEach { (gk, gv) -> val g = gv.asObj() ?: return@forEach; h.g[gk] = GeoPt(g.num("c").toInt(), g.str("a"), ints(g["s"])) }
                e.hist[k] = h
            }
            o.map("favs").forEach { (k, v) -> val m = v.asObj() ?: return@forEach; e.favs[k] = m.str("n") to m.str("a") }
            return e
        }
    }
}
