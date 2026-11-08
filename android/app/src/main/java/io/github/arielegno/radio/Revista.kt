package io.github.arielegno.radio

import io.github.arielegno.radio.core.Concierto
import io.github.arielegno.radio.core.Efemerides
import io.github.arielegno.radio.core.Json
import io.github.arielegno.radio.core.asObj
import io.github.arielegno.radio.core.num
import io.github.arielegno.radio.core.str
import java.time.LocalDate

/** Datos públicos de tu Revista para la portada (solo lectura) y la dirección de la Revista web. */
object Revista {
    /** Tu proyecto de Supabase. La clave es la «publishable»: es pública por diseño; la seguridad la dan las reglas SQL. */
    const val URL = "https://qfgncdzpcssiirxijdgj.supabase.co"
    const val CLAVE = "sb_publishable_VWLMFbw4lhkV6lW4-DQ2FA_x6E9lVLT"
    /** La Revista completa (artículos, Bóveda, club, muros…) se abre desde tu página. */
    const val WEB = "https://arielegno.github.io/Radio/index.html#revista"

    /** Otro proyecto de Supabase si lo pusiste en Ajustes (si no, el tuyo). */
    fun url(): String = RadioApp.prefs.str("sbUrl", "").trim().trimEnd('/').takeIf { Regex("^https://[a-z0-9-]+\\.supabase\\.co$").matches(it) } ?: URL
    fun clave(): String = RadioApp.prefs.str("sbKey", "").trim().takeIf { url() != URL && it.length > 20 } ?: CLAVE

    /** Conciertos de la Bóveda de un artista (los que están en cartelera). */
    suspend fun conciertosDe(artista: String, hoy: LocalDate = LocalDate.now()): List<Concierto> {
        val q = artista.trim().replace(Regex("[*,()%]"), " ").trim(); if (q.length < 2) return emptyList()
        val t = hoy.toString()
        val j = leer("/rest/v1/vault?select=id,title,artist,festival,year,provider,video_id,cover,starts_on,ends_on,featured_on,created_at&hidden=eq.false&starts_on=lte.$t&ends_on=gt.$t&artist=ilike.*" +
            java.net.URLEncoder.encode(q, "UTF-8").replace("+", "%20") + "*&limit=12", 3_600_000L) ?: return emptyList()
        return Json.arr(j).mapNotNull { it.asObj()?.let(Concierto::from) }
    }

    /** Conciertos de la Bóveda grabados en un festival. */
    suspend fun conciertosDeFestival(festival: String, hoy: LocalDate = LocalDate.now()): List<Concierto> {
        val q = festival.trim().replace(Regex("[*,()%]"), " ").trim(); if (q.length < 3) return emptyList()
        val t = hoy.toString()
        val j = leer("/rest/v1/vault?select=id,title,artist,festival,year,provider,video_id,cover,starts_on,ends_on,featured_on,created_at&hidden=eq.false&starts_on=lte.$t&ends_on=gt.$t&festival=ilike.*" +
            java.net.URLEncoder.encode(q, "UTF-8").replace("+", "%20") + "*&limit=12", 3_600_000L) ?: return emptyList()
        return Json.arr(j).mapNotNull { it.asObj()?.let(Concierto::from) }
    }

    data class Club(val id: String, val album: String, val artista: String, val anio: Int, val portada: String)

    private val cache = HashMap<String, Pair<Long, String>>()

    private suspend fun leer(ruta: String, ttlMs: Long): String? {
        cache[ruta]?.let { (t, v) -> if (System.currentTimeMillis() - t < (if (v == "[]") minOf(ttlMs, 300_000L) else ttlMs)) return v }
        if (!Red.hayInternet(RadioApp.app)) return cache[ruta]?.second
        return try {
            val v = Red.get(url() + ruta, mapOf("apikey" to clave(), "Authorization" to "Bearer ${clave()}"), 8000)
            cache[ruta] = System.currentTimeMillis() to v; v
        } catch (e: Exception) { cache[ruta]?.second }
    }

    suspend fun conciertoDelDia(hoy: LocalDate = LocalDate.now()): Concierto? {
        val t = hoy.toString()
        val j = leer("/rest/v1/vault?select=id,title,artist,festival,year,provider,video_id,cover,starts_on,ends_on,featured_on,created_at&starts_on=lte.$t&ends_on=gt.$t&hidden=eq.false&limit=60", 3_600_000L) ?: return null
        return Concierto.delDia(Json.arr(j).mapNotNull { it.asObj()?.let(Concierto::from) }, hoy)
    }

    suspend fun clubDeLaSemana(hoy: LocalDate = LocalDate.now()): Club? {
        val t = hoy.toString()
        val j = leer("/rest/v1/clubs?select=id,album,artist,year,cover,starts_on,ends_on&starts_on=lte.$t&ends_on=gt.$t&order=starts_on.desc&limit=1", 3_600_000L) ?: return null
        val m = Json.arr(j).firstOrNull().asObj() ?: return null
        val cover = m.str("cover").takeIf { Regex("^https://[a-z0-9-]+\\.supabase\\.co/storage/v1/object/public/revista/[\\w/.-]+$", RegexOption.IGNORE_CASE).matches(it) } ?: ""
        return Club(m.str("id"), m.str("album"), m.str("artist"), m.num("year").toInt(), cover)
    }

    private var hoyCache: Pair<String, List<Pair<String, String>>>? = null
    /** «Un día como hoy» de Wikipedia, solo lo de música. */
    suspend fun hoyEnLaMusica(hoy: LocalDate = LocalDate.now()): List<Pair<String, String>> {
        val k = hoy.toString(); hoyCache?.let { if (it.first == k) return it.second }
        if (!Red.hayInternet(RadioApp.app)) return emptyList()
        return try {
            val j = Red.get(String.format("https://api.wikimedia.org/feed/v1/wikipedia/es/onthisday/events/%02d/%02d", hoy.monthValue, hoy.dayOfMonth), timeoutMs = 7000)
            Efemerides.deWikipedia(j).also { hoyCache = k to it }
        } catch (e: Exception) { emptyList() }
    }
}
