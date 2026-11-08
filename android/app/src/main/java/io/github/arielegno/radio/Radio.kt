package io.github.arielegno.radio

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import io.github.arielegno.radio.core.Efemerides
import io.github.arielegno.radio.core.Json
import io.github.arielegno.radio.core.Luna
import io.github.arielegno.radio.core.LunaAjustes
import io.github.arielegno.radio.core.LunaContexto
import io.github.arielegno.radio.core.Modos
import io.github.arielegno.radio.core.Programas
import io.github.arielegno.radio.core.SpotifyAhora
import io.github.arielegno.radio.core.Track
import io.github.arielegno.radio.core.num
import io.github.arielegno.radio.core.str
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

/** El motor de la radio: arma la lista, reproduce, cuenta escuchas y hace que Luna hable entre canciones. */
@OptIn(UnstableApi::class)
class Radio(private val ctx: Context) {
    val voz = VozLuna(ctx)
    val entorno = Entorno(ctx)
    private val p = RadioApp.prefs

    var actual by mutableStateOf<Track?>(null); private set
    var siguiente by mutableStateOf<Track?>(null); private set
    var sonando by mutableStateOf(false); private set
    var activa by mutableStateOf(false); private set
    var posMs by mutableStateOf(0L); private set
    var durMs by mutableStateOf(0L); private set
    var textoLuna by mutableStateOf(""); private set
    var lunaHablando by mutableStateOf(false); private set
    var mensaje by mutableStateOf(""); private set
    var aire by mutableStateOf(p.bool("aire", true)); private set
    var mezclar by mutableStateOf(p.bool("mezclar", true)); private set
    var repetir by mutableStateOf(p.int("repetir", 0)); private set   // 0 no, 1 lista, 2 canción
    var apagarEn by mutableStateOf(0L); private set                   // hora (ms) del temporizador; 0 = apagado
    var modo by mutableStateOf(Modos.valido(p.str("modo", "carretera"))); private set
    /** true = la música viene de Spotify; false = de tu biblioteca. */
    var fuenteSpotify by mutableStateOf(p.str("fuente", "local") == "spotify"); private set
    /** Carátula de lo que suena en Spotify. */
    var portadaUrl by mutableStateOf(""); private set

    private val player get() = Reproductor.get()
    private var cola: List<Track> = emptyList()
    private var cancionNo = 0
    private var guion: CompletableDeferred<String>? = null
    private var guionPara = -1L
    private var enPausaLuna = false
    private val anteriores = mutableListOf<String>()
    private var contadoId = -1L
    private var segundos = 0
    private var reloj: Job? = null
    private var fundido: Job? = null
    private var spJob: Job? = null
    private var spUri = ""
    private var spSiguiente: SpotifyAhora? = null
    private var spSaltoPropio = false

    init {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { alCambiar(mediaItem) }
            override fun onIsPlayingChanged(isPlaying: Boolean) { sonando = isPlaying }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) pausaDeLuna()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) { mensaje = "Fin de la lista."; activa = false }
            }
            override fun onAudioSessionIdChanged(audioSessionId: Int) { Efectos.aplicar(audioSessionId) }
            override fun onPlayerError(error: PlaybackException) {
                mensaje = "No se pudo reproducir «${actual?.name ?: "la canción"}». Paso a la siguiente."
                if (player.hasNextMediaItem()) { player.seekToNextMediaItem(); player.prepare(); player.play() }
            }
        })
        reloj = RadioApp.scope.launchSafe { while (true) { delay(1000); tic() } }
        try { Efectos.aplicar(player.audioSessionId) } catch (e: Exception) {}
    }

    // ---------- Controles ----------

    /** Arranca la radio con tu biblioteca (o con una parte de ella). */
    fun iniciar(filtro: ((Track) -> Boolean)? = null, desdeId: Long? = null, mezclarEsta: Boolean? = null) {
        if (fuenteSpotify && filtro == null && desdeId == null) { iniciarSpotify(); return }
        if (fuenteSpotify) { ponerFuente(false); mensaje = "Cambié a tu biblioteca para poner esas canciones." }
        val todas = RadioApp.biblioteca.pistas
        var lista = if (filtro == null) todas else todas.filter(filtro)
        if (lista.isEmpty()) { mensaje = if (todas.isEmpty()) "Agrega canciones en Biblioteca para empezar." else "No hay canciones para eso en tu biblioteca."; return }
        val mez = mezclarEsta ?: mezclar
        if (mez) lista = lista.shuffled()
        if (desdeId != null) lista.firstOrNull { it.id == desdeId }?.let { t -> lista = listOf(t) + lista.filter { it.id != desdeId } }
        cola = lista; cancionNo = 0; guion = null; guionPara = -1; enPausaLuna = false; anteriores.clear()
        voz.callar()
        player.setMediaItems(lista.map { item(it) }, 0, 0L)
        player.repeatMode = repModo()
        player.prepare()
        activa = true; mensaje = ""
        entorno.iniciar()
        if (aire) RadioApp.scope.launchSafe {
            val t = lista[0]
            val hola = "Hola, soy ${p.str("host", "Luna")}. ${io.github.arielegno.radio.core.saludo(LocalDateTime.now().hour, p.str("me", ""))}. Empezamos con ${t.name}, de ${t.artist}."
            hablar(hola)
            player.play()
        } else player.play()
    }

    fun alternarPausa() {
        if (fuenteSpotify) { if (!activa) iniciarSpotify() else if (lunaHablando) voz.callar() else sp(if (sonando) "/me/player/pause" else "/me/player/play", "PUT") { sonando = !sonando }; return }
        if (!activa || player.mediaItemCount == 0) { iniciar(); return }
        if (lunaHablando) { voz.callar(); return }
        if (player.isPlaying) player.pause() else { player.play() }
    }
    /** Mientras suena un video en la Revista la radio se queda en pausa (y Luna no sigue sola); al cerrarlo vuelve. */
    var retenida = false; private set
    private var reanudarTras = false
    fun retener(on: Boolean) {
        if (on == retenida) return
        retenida = on
        if (on) { reanudarTras = sonando || lunaHablando || enPausaLuna || (!fuenteSpotify && player.isPlaying); if (reanudarTras) { pausar(); mensaje = "Pausé la radio mientras ves el video." } }
        else if (reanudarTras) { reanudarTras = false; seguir() }
    }
    fun pausar() { if (lunaHablando) voz.callar(); if (fuenteSpotify) { sp("/me/player/pause", "PUT") { sonando = false }; return }; if (player.isPlaying) player.pause() }
    fun seguir() { if (fuenteSpotify) { if (!activa) iniciarSpotify() else sp("/me/player/play", "PUT") { sonando = true }; return }; if (!activa) iniciar() else if (!player.isPlaying) player.play() }
    fun siguienteCancion() { voz.callar(); if (fuenteSpotify) { sp("/me/player/next", "POST"); return }; if (player.hasNextMediaItem()) { player.seekToNextMediaItem(); player.play() } else mensaje = "No hay más canciones en la lista." }
    fun anteriorCancion() { voz.callar(); if (fuenteSpotify) { sp("/me/player/previous", "POST"); return }; if (player.currentPosition > 4000 || !player.hasPreviousMediaItem()) player.seekTo(0) else player.seekToPreviousMediaItem(); player.play() }
    fun irA(ms: Long) { if (fuenteSpotify) { sp("/me/player/seek?position_ms=${ms.coerceAtLeast(0)}", "PUT") { posMs = ms }; return }; player.seekTo(ms.coerceAtLeast(0)) }
    fun detener() {
        voz.callar()
        if (fuenteSpotify) { spJob?.cancel(); spJob = null; if (activa) sp("/me/player/pause", "PUT") }
        else { player.stop(); player.clearMediaItems() }
        activa = false; actual = null; siguiente = null; textoLuna = ""; portadaUrl = ""; spUri = ""; entorno.detener()
    }

    /** Cambia entre tu biblioteca y Spotify. */
    fun ponerFuente(spotify: Boolean) {
        if (spotify == fuenteSpotify) return
        if (activa) detener()
        fuenteSpotify = spotify; p.set("fuente", if (spotify) "spotify" else "local")
        mensaje = if (spotify) "Ahora la radio usa Spotify." else "Ahora la radio usa tu biblioteca."
    }

    /** «Pon Soda Stereo» con Spotify: busca y pone a sonar. */
    fun ponerEnSpotify(q: String) {
        RadioApp.scope.launchSafe {
            if (!Spotify.conectado) { mensaje = "Conecta Spotify en Ajustes."; return@launchSafe }
            if (Spotify.buscarYPoner(q)) { mensaje = "Poniendo $q en Spotify…"; if (!activa) iniciarSpotify() }
            else mensaje = "No encontré «$q» en Spotify (o no hay un dispositivo activo: abre Spotify)."
        }
    }

    private fun sp(ruta: String, metodo: String, alAcertar: () -> Unit = {}) {
        RadioApp.scope.launchSafe {
            val (c, _) = Spotify.api(metodo, ruta)
            when {
                c in 200..299 -> alAcertar()
                c == 404 -> { mensaje = "No hay un dispositivo de Spotify activo: abre Spotify y pon algo de música."; Spotify.abrirApp(ctx) }
                c == 403 -> mensaje = "Spotify necesita Premium para controlar la música."
                c == 0 -> mensaje = if (Spotify.conectado) "Sin conexión con Spotify." else "Conecta Spotify en Ajustes."
            }
        }
    }

    // ---------- Spotify ----------

    private fun iniciarSpotify() {
        if (!Spotify.conectado) { mensaje = "Conecta tu cuenta de Spotify en Ajustes."; return }
        activa = true; mensaje = ""; anteriores.clear(); cancionNo = 0; spUri = ""; guion = null; spSaltoPropio = false
        player.pause()
        entorno.iniciar()
        spJob?.cancel()
        spJob = RadioApp.scope.launchSafe {
            val a = Spotify.ahora()
            if (a == null || !a.sonando) {
                val (c, _) = Spotify.api("PUT", "/me/player/play")
                if (c == 404 || c == 0) { mensaje = "Abre Spotify y pon algo de música: la radio lo toma desde ahí."; Spotify.abrirApp(ctx) }
                else if (c == 403) mensaje = "Spotify necesita Premium para controlar la música."
            }
            if (aire && a != null) hablar("Hola, soy ${p.str("host", "Luna")}. ${io.github.arielegno.radio.core.saludo(LocalDateTime.now().hour, p.str("me", ""))}. Seguimos con Spotify.")
            while (activa && fuenteSpotify) {
                try { sondearSpotify() } catch (e: Exception) {}
                delay(if (sonando) 2500 else 4000)
            }
        }
    }

    private suspend fun sondearSpotify() {
        if (enPausaLuna) return
        val a = Spotify.ahora() ?: run { sonando = false; return }
        posMs = a.posMs; durMs = a.durMs; sonando = a.sonando; if (a.portada.isNotEmpty()) portadaUrl = a.portada
        if (a.uri != spUri) {
            val primera = spUri.isEmpty()
            spUri = a.uri; actual = a.comoTrack(); siguiente = null; spSiguiente = null; segundos = 0
            if (!primera) cancionNo++
            if (spSaltoPropio) { spSaltoPropio = false } else guion = null
            return
        }
        // Se acerca el final: Luna prepara lo que dirá y habla justo antes de la siguiente.
        val cada = p.int("cadaN", 1).coerceIn(1, 10)
        val falta = a.durMs - a.posMs
        val toca = aire && a.sonando && (cancionNo + 1) % cada == 0
        if (toca && falta in 1..25_000 && guion == null) {
            val sig = Spotify.siguiente(); spSiguiente = sig; siguiente = sig?.comoTrack()
            val d = CompletableDeferred<String>(); guion = d
            val prev = actual; val nxt = siguiente
            RadioApp.scope.launchSafe { d.complete(escribir(prev, nxt)) }
        }
        val g = guion
        if (toca && g != null && falta in 1..3_500) {
            enPausaLuna = true
            try {
                delay((falta - 800).coerceAtLeast(0))
                Spotify.api("PUT", "/me/player/pause"); sonando = false
                val texto = withTimeoutOrNull(10_000) { g.await() } ?: Luna.sinIA(LunaContexto(actual, siguiente), ajustes())
                if (aire && activa) hablar(texto)
                spSaltoPropio = true
                Spotify.api("POST", "/me/player/next"); sonando = true
            } finally { enPausaLuna = false; guion = null }
        }
    }

    fun ponerAire(on: Boolean) { aire = on; p.set("aire", on); if (!on) { player.pauseAtEndOfMediaItems = false; guion = null } else alCambiar(player.currentMediaItem, nueva = false) }
    fun ponerMezclar(on: Boolean) { mezclar = on; p.set("mezclar", on); if (fuenteSpotify && activa) sp("/me/player/shuffle?state=$on", "PUT") }
    fun ciclarRepetir() {
        repetir = (repetir + 1) % 3; p.set("repetir", repetir); player.repeatMode = repModo()
        if (fuenteSpotify && activa) sp("/me/player/repeat?state=" + when (repetir) { 1 -> "context"; 2 -> "track"; else -> "off" }, "PUT")
    }
    private fun repModo() = when (repetir) { 1 -> Player.REPEAT_MODE_ALL; 2 -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }
    fun ponerModo(m: String) { modo = Modos.valido(m); p.set("modo", modo); mensaje = "Modo ${Modos.ETIQUETA[modo]?.lowercase()}: Luna cambia su tono." }
    fun temporizador(min: Int) { apagarEn = if (min <= 0) 0L else System.currentTimeMillis() + min * 60_000L; mensaje = if (min <= 0) "Temporizador apagado." else "La radio se apaga en $min minutos." }

    fun alternarFavorita(): Boolean? {
        val t = actual ?: return null
        val on = RadioApp.stats.toggleFav(t.name, t.artist); Datos.guardarStats(); return on
    }

    // ---------- Lo que pasa entre canciones ----------

    private fun item(t: Track): MediaItem {
        val md = MediaMetadata.Builder().setTitle(t.name).setArtist(t.artist).setAlbumTitle(t.album.ifEmpty { null })
        RadioApp.biblioteca.caratula(t)?.let { md.setArtworkUri(Uri.fromFile(it)) }
        return MediaItem.Builder().setMediaId(t.id.toString()).setUri(Uri.fromFile(RadioApp.biblioteca.archivo(t))).setMediaMetadata(md.build()).build()
    }

    private fun trackDe(mi: MediaItem?): Track? = mi?.mediaId?.toLongOrNull()?.let { id -> cola.firstOrNull { it.id == id } ?: RadioApp.biblioteca.porId(id) }

    private fun alCambiar(mi: MediaItem?, nueva: Boolean = true) {
        val t = trackDe(mi)
        actual = t
        val idx = player.currentMediaItemIndex
        val nextMi = if (player.hasNextMediaItem() && idx + 1 < player.mediaItemCount) player.getMediaItemAt(idx + 1) else if (player.repeatMode == Player.REPEAT_MODE_ALL && player.mediaItemCount > 0) player.getMediaItemAt(0) else null
        siguiente = trackDe(nextMi)
        if (t == null) return
        if (nueva) {
            cancionNo++; segundos = 0
            p.set("ultima", Json.write(linkedMapOf("id" to t.id, "n" to t.name, "a" to t.artist)))
            if (p.bool("fundido", false)) fundir()
        }
        val cada = p.int("cadaN", 1).coerceIn(1, 10)
        val toca = aire && siguiente != null && player.repeatMode != Player.REPEAT_MODE_ONE && cancionNo % cada == 0
        player.pauseAtEndOfMediaItems = toca
        if (toca && guionPara != t.id) {
            guionPara = t.id
            val d = CompletableDeferred<String>(); guion = d
            val prev = t; val nxt = siguiente
            RadioApp.scope.launchSafe { d.complete(escribir(prev, nxt)) }
        }
    }

    private fun pausaDeLuna() {
        if (enPausaLuna) return
        val g = guion ?: run { player.play(); return }
        enPausaLuna = true
        RadioApp.scope.launchSafe {
            try {
                val texto = withTimeoutOrNull(12_000) { g.await() } ?: Luna.sinIA(LunaContexto(actual, siguiente), ajustes())
                guion = null
                if (aire && activa) hablar(texto)
            } finally {
                enPausaLuna = false
                if (activa && !retenida && (apagarEn == 0L || apagarEn > System.currentTimeMillis())) player.play()
            }
        }
    }

    private suspend fun hablar(texto: String) {
        textoLuna = texto
        anteriores += texto; while (anteriores.size > 6) anteriores.removeAt(0)
        lunaHablando = true
        try { if (!voz.decir(texto)) mensaje = "Luna no pudo hablar: revisa la voz del teléfono en Ajustes." }
        finally { lunaHablando = false }
    }

    fun ajustes(): LunaAjustes {
        val m = modo
        return LunaAjustes(
            host = p.str("host", "Luna"), genero = p.str("genero", "locutora"), acento = p.str("acento", "neutro"), largo = p.str("largo", "media"),
            modo = m, modoTexto = p.str("modo_$m", ""), persona = p.str("persona", ""), evitar = p.str("evitar", ""), nombre = p.str("me", ""),
            frasesPropias = p.str("frases_$m", "").lines().map { it.trim() }.filter { it.isNotEmpty() },
            estilo = p.str("estilo", ""), contexto = p.str("contexto", "")
        )
    }

    /** Escribe lo que dirá Luna (con Gemini si hay clave; si no, con frases sencillas). */
    suspend fun escribir(prev: Track?, next: Track?): String {
        val a = ajustes(); val now = LocalDateTime.now()
        val ded = p.str("dedic", "").trim().also { if (it.isNotEmpty()) p.remove("dedic") }
        val prog = if (ded.isNotEmpty()) null else Programas.elegir(now,
            Programas.Estado(p.bool("progRec", false), p.int("progRecH", 20), p.str("progRecHecho", ""), p.bool("progSem", false), p.str("progSemHecho", "")),
            prev?.year ?: 0, RadioApp.stats.resumen("month", LocalDate.now())).also { r -> r.marcarRec?.let { p.set("progRecHecho", it) }; r.marcarSem?.let { p.set("progSemHecho", it) } }
        val memoria = if (p.bool("memoria", true)) RadioApp.stats.resumen("all", LocalDate.now())?.topArtistas(3)?.joinToString(", ") { it.first }?.let { "Sus artistas más escuchados: $it" } ?: "" else ""
        // Hora y clima cada «wMin» minutos; la zona cada «pEvery» intervenciones (como en la web)
        intervenciones++
        val tocaClima = System.currentTimeMillis() - ultimoClima > p.int("wMin", 30).coerceIn(5, 180) * 60_000L
        if (tocaClima) { entorno.actualizarClima(); if (entorno.clima.isNotEmpty() || entorno.lat != null) ultimoClima = System.currentTimeMillis() }
        val tocaLugar = (intervenciones - 1) % p.int("pEvery", 2).coerceIn(1, 20) == 0
        val c = LunaContexto(prev, next,
            hora = if (tocaClima) horaTexto(now) else "",
            clima = if (tocaClima) entorno.clima else "", lugar = if (tocaLugar) entorno.zona else "",
            efemeride = Efemerides.propias(p.str("efemerides", ""), LocalDate.now()),
            dedicatoria = ded, programa = prog?.texto ?: "", memoria = memoria, anteriores = anteriores.toList())
        if (p.str("proveedor", "gemini") == "claude") return escribirClaude(a, c, next != null, now.hour)
        val clave = p.str("geminiKey", "").trim()
        if (clave.isEmpty() || !Red.hayInternet(ctx)) return Luna.sinIA(c, a)
        return try {
            val modelo = p.str("geminiModel", "").trim().ifEmpty { p.str("geminiModelSel", "gemini-3.5-flash-lite") }.filter { it.isLetterOrDigit() || it == '-' || it == '.' }
            val r = Red.post("https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent",
                Luna.peticionGemini(Luna.sistema(a, next != null, now.hour), Luna.usuario(c)), mapOf("x-goog-api-key" to clave), 25_000)
            Luna.textoGemini(r) ?: Luna.sinIA(c, a)
        } catch (e: Red.Error) {
            mensaje = when (e.codigo) { 400, 403 -> "Gemini rechazó la clave: revísala en Ajustes."; 404 -> "Ese modelo de Gemini no existe: cámbialo en Ajustes."; 429 -> "Gemini llegó a su límite gratis por ahora: Luna usa frases sencillas."; else -> "Gemini falló (${e.codigo}): Luna usa frases sencillas." }
            Luna.sinIA(c, a)
        } catch (e: Exception) { Luna.sinIA(c, a) }
    }

    private var intervenciones = 0
    private var ultimoClima = 0L

    /** «8:15» o «8:15 p. m.» según Ajustes › Apariencia › Formato de hora. */
    fun horaTexto(t: LocalDateTime = LocalDateTime.now()): String {
        val h12 = when (p.str("h12", "auto")) { "1" -> true; "0" -> false; else -> !android.text.format.DateFormat.is24HourFormat(ctx) }
        if (!h12) return String.format(Locale.ROOT, "%d:%02d", t.hour, t.minute)
        val h = if (t.hour % 12 == 0) 12 else t.hour % 12
        return String.format(Locale.ROOT, "%d:%02d %s", h, t.minute, if (t.hour < 12) "a. m." else "p. m.")
    }

    /** Claude (de pago, con tu propia clave de Anthropic). */
    private suspend fun escribirClaude(a: io.github.arielegno.radio.core.LunaAjustes, c: LunaContexto, conSiguiente: Boolean, hora: Int): String {
        val clave = p.str("claudeKey", "").trim()
        if (clave.isEmpty() || !Red.hayInternet(ctx)) return Luna.sinIA(c, a)
        val modelo = p.str("claudeModel", "claude-sonnet-5-5").trim().filter { it.isLetterOrDigit() || it == '-' || it == '.' }.ifEmpty { "claude-sonnet-5-5" }
        val (cod, txt) = try {
            Red.crudo("POST", "https://api.anthropic.com/v1/messages", Luna.peticionClaude(modelo, Luna.sistema(a, conSiguiente, hora), Luna.usuario(c)),
                mapOf("x-api-key" to clave, "anthropic-version" to "2023-06-01"), timeoutMs = 25_000)
        } catch (e: Exception) { return Luna.sinIA(c, a) }
        if (cod in 200..299) return Luna.textoClaude(txt) ?: Luna.sinIA(c, a)
        mensaje = when (cod) { 401, 403 -> "Claude rechazó la clave: revísala en Ajustes."; 404 -> "Ese modelo de Claude no existe: cámbialo en Ajustes."; 429, 529 -> "Claude está ocupado o llegaste a tu límite: Luna usa frases sencillas."; else -> "Claude falló ($cod): Luna usa frases sencillas." }
        return Luna.sinIA(c, a)
    }

    /** Para «Probar intervención» en Ajustes. */
    suspend fun probarLuna() {
        val lib = RadioApp.biblioteca.pistas
        val a = actual ?: lib.randomOrNull(); val b = lib.filter { it.id != a?.id }.randomOrNull()
        if (a == null) { hablar("Hola, soy ${p.str("host", "Luna")}. Agrega canciones y te acompaño en el camino."); return }
        hablar(escribir(a, b))
    }
    suspend fun probarVoz() { hablar("Hola, soy ${p.str("host", "Luna")}. ¿Me escuchas bien?") }

    private fun fundir() {
        fundido?.cancel()
        fundido = RadioApp.scope.launchSafe {
            player.volume = 0f
            for (i in 1..15) { delay(100); player.volume = i / 15f }
            player.volume = 1f
        }
    }

    private fun tic() {
        val pl = player
        val suena = if (fuenteSpotify) sonando && !enPausaLuna else pl.isPlaying
        if (fuenteSpotify) { if (suena) posMs = (posMs + 1000).coerceAtMost(durMs.coerceAtLeast(0)) }
        else { posMs = pl.currentPosition.coerceAtLeast(0); durMs = pl.duration.takeIf { it > 0 } ?: (actual?.durMs ?: 0L) }
        val t = actual
        if (t != null && suena) {
            if (contadoId != t.id) { segundos++; if (segundos >= 30) { contadoId = t.id; contar(t) } }
        }
        if (t != null && contadoId == t.id && segundos == 0) contadoId = -1L
        // Transición suave: baja el volumen en los últimos segundos de la canción
        if (!fuenteSpotify && p.bool("fundido", false) && pl.isPlaying && durMs > 8000 && durMs - posMs in 1..2500 && fundido?.isActive != true) {
            fundido = RadioApp.scope.launchSafe { for (i in 9 downTo 2) { player.volume = i / 10f; delay(220) } }
        }
        if (apagarEn > 0 && System.currentTimeMillis() >= apagarEn) { apagarEn = 0; pausar(); mensaje = "Temporizador: la radio se apagó. Buenas noches." }
    }

    private fun contar(t: Track) {
        RadioApp.stats.countPlay(t.name, t.artist, entorno.zona.ifEmpty { null }, entorno.lat, entorno.lon, LocalDate.now())
        Datos.guardarStats()
    }

    /** Lo último que sonó (para «Seguir escuchando»). */
    fun ultima(): Triple<Long, String, String>? {
        val o = Json.obj(p.str("ultima", "")); if (o.isEmpty()) return null
        return Triple(o.num("id", -1), o.str("n"), o.str("a"))
    }
}
