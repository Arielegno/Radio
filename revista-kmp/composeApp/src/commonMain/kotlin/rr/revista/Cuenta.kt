package rr.revista

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Sesión de la Revista: la misma cuenta de Supabase que usa la web (se guarda en el teléfono o el navegador). */
data class Sesion(val access: String, val refresh: String, val exp: Double, val uid: String, val nombre: String)

/** Un error con el mensaje ya en español, listo para mostrar. */
class ErrorRevista(mensaje: String, val crudo: String = "") : Exception(mensaje)

object Cuenta {
    private const val CLAVE_SESION = "rr_rev_ses"
    var sesion by mutableStateOf(cargar()); private set
    /** Perfil propio (rol, si está suspendida, nombre, foto…). */
    var yo by mutableStateOf<Map<String, Any?>>(emptyMap()); private set
    var likes by mutableStateOf(setOf<String>()); private set
    var sigo by mutableStateOf(setOf<String>()); private set
    var recomiendo by mutableStateOf(setOf<String>()); private set
    private val candado = Mutex()

    val dentro: Boolean get() = sesion != null
    val uid: String get() = sesion?.uid ?: ""
    val esAdmin: Boolean get() = yo.str("role") == "admin"
    val esEditor: Boolean get() = yo.str("role") == "editor" || esAdmin
    val suspendida: Boolean get() = yo.bool("banned")

    private fun cargar(): Sesion? = Json.obj(leerDato(CLAVE_SESION)).let { o ->
        if (o.str("uid").isBlank() || o.str("access").isBlank()) null
        else Sesion(o.str("access"), o.str("refresh"), o.dbl("exp"), o.str("uid"), o.str("name"))
    }

    private fun guardar(s: Sesion?) {
        sesion = s
        guardarDato(CLAVE_SESION, s?.let { Json.write(linkedMapOf("access" to it.access, "refresh" to it.refresh, "exp" to it.exp, "uid" to it.uid, "name" to it.nombre)) })
    }

    private fun deAuth(o: Map<String, Any?>, nombreAnterior: String = ""): Sesion {
        val u = o.map("user")
        return Sesion(o.str("access_token"), o.str("refresh_token"), ahoraMs() + o.dbl("expires_in", 3600.0) * 1000, u.str("id"),
            u.map("user_metadata").str("name").take(40).ifBlank { nombreAnterior })
    }

    /** Traduce los errores de Supabase (igual que revErr en la web). */
    fun traducir(m: String): String = when {
        Regex("Invalid login", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Correo o contraseña incorrectos."
        Regex("already registered|already been registered", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Ese correo ya tiene cuenta. Usa «Entrar»."
        Regex("Email not confirmed", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Confirma tu correo (te llegó un mensaje) y vuelve a entrar."
        Regex("Password should be", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "La contraseña debe tener al menos 6 caracteres."
        Regex("Failed to fetch|NetworkError|Load failed", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Sin conexión con el servidor de la revista."
        Regex("duplicate key|already exists", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Eso ya existe."
        Regex("row-level security", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "No tienes permiso para hacer eso."
        Regex("violates foreign key", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Eso ya no existe."
        Regex("violates check", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Eso no se puede hacer (revisa el texto o la imagen)."
        Regex("relation .* does not exist|Could not find the table", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Falta actualizar la base de datos de la Revista (SQL más reciente)."
        Regex("Límite alcanzado|suspendida|invitación incorrecto|solo acepta", RegexOption.IGNORE_CASE).containsMatchIn(m) -> m
        else -> m.ifBlank { "Algo salió mal." }
    }

    private suspend fun renovar() = candado.withLock {
        val s = sesion ?: return@withLock
        if (ahoraMs() < s.exp - 60_000) return@withLock
        val r = try { httpPedido("POST", Supa.URL + "/auth/v1/token?grant_type=refresh_token", mapOf("apikey" to Supa.CLAVE, "Content-Type" to "application/json"), Json.write(mapOf("refresh_token" to s.refresh))) } catch (e: Throwable) { return@withLock }
        if (r.estado in 200..299) guardar(deAuth(Json.obj(r.texto), s.nombre))
        else if (r.estado == 400 || r.estado == 401) guardar(null)
    }

    /** Pedido a la base de datos con tu sesión (si entraste). Lanza ErrorRevista con el mensaje en español. */
    suspend fun api(metodo: String, ruta: String, cuerpo: Any? = null, prefer: String? = null): String {
        val habiaSesion = sesion != null
        if (habiaSesion) renovar()
        // Si la sesión venció al renovarla, no se manda el pedido sin cuenta (fallaría con «sin permiso»)
        if (habiaSesion && sesion == null && metodo != "GET") throw ErrorRevista("Tu sesión venció. Vuelve a entrar a tu cuenta.")
        val cab = linkedMapOf("apikey" to Supa.CLAVE, "Content-Type" to "application/json")
        sesion?.let { cab["Authorization"] = "Bearer " + it.access }
        prefer?.let { cab["Prefer"] = it }
        val r = try { httpPedido(metodo, Supa.URL + ruta, cab, cuerpo?.let { Json.write(it) }) } catch (e: Throwable) { throw ErrorRevista(traducir(e.message ?: "Failed to fetch")) }
        if (r.estado !in 200..299) {
            val o = Json.obj(r.texto)
            val m = o.str("message").ifBlank { o.str("msg") }.ifBlank { o.str("error_description") }.ifBlank { o.str("error") }.ifBlank { "HTTP ${r.estado}" }
            if (r.estado == 401 && sesion != null && Regex("JWT|expired", RegexOption.IGNORE_CASE).containsMatchIn(m)) guardar(null)
            throw ErrorRevista(traducir(m), r.texto)
        }
        return r.texto
    }

    /** ¿Cuántas filas hay? (usa la cabecera Content-Range) */
    suspend fun contar(ruta: String): Int {
        if (sesion != null) renovar()
        val cab = linkedMapOf("apikey" to Supa.CLAVE, "Prefer" to "count=exact", "Range" to "0-0")
        sesion?.let { cab["Authorization"] = "Bearer " + it.access }
        val r = httpPedido("GET", Supa.URL + ruta, cab, null)
        return r.rango.substringAfter('/', "0").toIntOrNull() ?: 0
    }

    suspend fun entrar(correo: String, clave: String) {
        val r = httpPedido("POST", Supa.URL + "/auth/v1/token?grant_type=password", mapOf("apikey" to Supa.CLAVE, "Content-Type" to "application/json"), Json.write(mapOf("email" to correo, "password" to clave)))
        val o = Json.obj(r.texto)
        if (r.estado !in 200..299) throw ErrorRevista(traducir(o.str("msg").ifBlank { o.str("error_description") }.ifBlank { o.str("message") }.ifBlank { o.str("error") }))
        guardar(deAuth(o)); social(true)
    }

    /** true = ya quedaste dentro; false = hay que confirmar el correo. */
    suspend fun crear(correo: String, clave: String, nombre: String, invitacion: String, volverA: String): Boolean {
        guardarDato("rr_last_auth", "signup")
        val datos = linkedMapOf<String, Any?>("name" to nombre, "rules" to "2026-11"); if (invitacion.isNotBlank()) datos["invite"] = invitacion
        val r = httpPedido("POST", Supa.URL + "/auth/v1/signup?redirect_to=" + Supa.enc(volverA), mapOf("apikey" to Supa.CLAVE, "Content-Type" to "application/json"),
            Json.write(mapOf("email" to correo, "password" to clave, "data" to datos)))
        val o = Json.obj(r.texto)
        if (r.estado !in 200..299) throw ErrorRevista(traducir(o.str("msg").ifBlank { o.str("message") }.ifBlank { o.str("error_description") }.ifBlank { o.str("error") }))
        if (o.str("access_token").isNotBlank()) { guardar(deAuth(o)); social(true); return true }
        return false
    }

    suspend fun recuperar(correo: String, volverA: String): Int {
        guardarDato("rr_last_auth", "recover")
        return httpPedido("POST", Supa.URL + "/auth/v1/recover?redirect_to=" + Supa.enc(volverA), mapOf("apikey" to Supa.CLAVE, "Content-Type" to "application/json"), Json.write(mapOf("email" to correo))).estado
    }

    suspend fun reenviar(correo: String, volverA: String): Int =
        httpPedido("POST", Supa.URL + "/auth/v1/resend?redirect_to=" + Supa.enc(volverA), mapOf("apikey" to Supa.CLAVE, "Content-Type" to "application/json"), Json.write(mapOf("type" to "signup", "email" to correo))).estado

    suspend fun soloConInvitacion(): Boolean = try { api("POST", "/rest/v1/rpc/rr_invite_only", emptyMap<String, Any>()).trim() == "true" } catch (e: Throwable) { false }

    fun salir() { guardar(null); yo = emptyMap(); likes = emptySet(); sigo = emptySet(); recomiendo = emptySet() }

    /** Tus me gusta, a quién sigues, qué recomiendas y tu perfil. */
    suspend fun social(forzar: Boolean = false) {
        val s = sesion ?: return
        if (!forzar && yo.str("id") == s.uid) return
        runCatching { likes = Json.arr(api("GET", "/rest/v1/likes?select=post_id&user_id=eq.${s.uid}")).mapNotNull { it.asObj()?.str("post_id") }.toSet() }
        runCatching { sigo = Json.arr(api("GET", "/rest/v1/follows?select=author_id&follower_id=eq.${s.uid}")).mapNotNull { it.asObj()?.str("author_id") }.toSet() }
        runCatching { recomiendo = Json.arr(api("GET", "/rest/v1/recommends?select=post_id&user_id=eq.${s.uid}")).mapNotNull { it.asObj()?.str("post_id") }.toSet() }
        yo = runCatching { Json.arr(api("GET", "/rest/v1/profiles?select=*&id=eq.${s.uid}")).firstOrNull()?.asObj() }.getOrNull() ?: mapOf("id" to s.uid, "name" to s.nombre, "role" to "user")
    }

    suspend fun alternarLike(post: String): Boolean {
        val on = post in likes
        if (on) api("DELETE", "/rest/v1/likes?post_id=eq.$post&user_id=eq.$uid") else api("POST", "/rest/v1/likes", mapOf("post_id" to post, "user_id" to uid), "return=minimal")
        likes = if (on) likes - post else likes + post
        return !on
    }

    suspend fun alternarSeguir(autor: String): Boolean {
        if (!dentro) throw ErrorRevista("Entra a tu cuenta para seguir perfiles.")
        if (autor == uid) throw ErrorRevista("No puedes seguirte a ti mismo.")
        // Pregunta a la base si ya lo sigues (por si se siguió desde la web u otro teléfono)
        val ya = runCatching { Json.arr(api("GET", "/rest/v1/follows?select=author_id&follower_id=eq.$uid&author_id=eq.$autor")).isNotEmpty() }.getOrDefault(autor in sigo)
        if (ya) api("DELETE", "/rest/v1/follows?follower_id=eq.$uid&author_id=eq.$autor")
        else api("POST", "/rest/v1/follows", mapOf("follower_id" to uid, "author_id" to autor), "return=minimal,resolution=ignore-duplicates")
        sigo = if (ya) sigo - autor else sigo + autor
        return !ya
    }

    suspend fun recomendar(post: String, nota: String) {
        api("POST", "/rest/v1/recommends", mapOf("post_id" to post, "user_id" to uid, "note" to nota.trim().take(280).ifBlank { null }), "return=minimal")
        recomiendo = recomiendo + post
    }

    suspend fun dejarDeRecomendar(post: String) {
        api("DELETE", "/rest/v1/recommends?post_id=eq.$post&user_id=eq.$uid"); recomiendo = recomiendo - post
    }

    suspend fun comentar(post: String, texto: String, padre: String?) {
        val b = linkedMapOf<String, Any?>("post_id" to post, "author_id" to uid, "body" to texto.trim().take(1000)); if (!padre.isNullOrBlank()) b["parent_id"] = padre
        api("POST", "/rest/v1/comments", b, "return=minimal")
    }

    suspend fun borrarComentario(id: String) { api("DELETE", "/rest/v1/comments?id=eq.$id") }

    val CATEGORIAS = linkedMapOf("spam" to "Spam o publicidad", "ofensivo" to "Ofensivo o acoso", "copyright" to "Copia sin permiso", "falso" to "Información falsa", "otro" to "Otro")
    suspend fun denunciar(tipo: String, id: String, categoria: String, motivo: String) {
        try { api("POST", "/rest/v1/reports", mapOf("target_type" to tipo, "target_id" to id, "category" to categoria, "reason" to motivo.trim().take(300).ifBlank { null }, "reporter" to uid), "return=minimal") }
        catch (e: ErrorRevista) { if (Regex("ya existe|duplicate|Eso ya existe", RegexOption.IGNORE_CASE).containsMatchIn(e.message + e.crudo)) throw ErrorRevista("Ya habías denunciado esto.") else throw e }
    }

    val NORMAS = listOf(
        "Respeto" to "Critica la música, no a las personas. Nada de insultos, acoso, odio ni amenazas.",
        "Arte propio" to "Sube solo pósters e imágenes que hiciste tú o que tienes permiso de compartir. Da crédito a quien los diseñó.",
        "Fuentes" to "Si citas algo de otro sitio, enlázalo. No copies artículos completos de otras personas.",
        "Nada de spam" to "Sin publicidad repetida, cadenas ni enlaces engañosos.",
        "Contenido apto" to "Nada sexual explícito, violento o ilegal. La revista la leen personas de todas las edades.",
        "Moderación" to "El administrador puede quitar contenido o suspender cuentas que rompan estas normas. Si ves algo así, usa «Denunciar»."
    )
}
