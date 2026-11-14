package rr.revista

/* Lo que la plataforma (navegador o Android) tiene que dar: red, fecha y abrir enlaces. */
/** Respuesta de un pedido HTTP: estado, texto y el rango de contenido (para contar filas). */
data class Respuesta(val estado: Int, val texto: String, val rango: String = "")
expect suspend fun httpPedido(metodo: String, url: String, cabeceras: Map<String, String>, cuerpo: String?): Respuesta
expect fun leerDato(clave: String): String?
expect fun guardarDato(clave: String, valor: String?)
expect fun ahoraMs(): Double

/** Algo que se pide ahora (por ejemplo, abrir el selector de fotos al tocar) y se espera después. */
interface Espera<T> { suspend fun esperar(): T }
/** Imagen elegida y achicada a JPEG: grande y miniatura (data:image/jpeg;base64,…). */
data class Foto(val grande: String, val mini: String)
/** Abre el selector de imágenes del teléfono. Hay que llamarlo directo al tocar un botón. */
expect fun elegirImagenes(maxGrande: Int, maxMini: Int, varias: Boolean): Espera<List<Foto>>
/** Sube un archivo (en base64) al almacén. */
/** Recorta una foto (data:image/jpeg) a una proporción ancho/alto; encuadre 0 = izquierda/arriba, 1 = derecha/abajo. */
/** Procesa una foto: giro (0/90/180/270), espejo, proporción (0 = la suya), zoom, encuadre (0–1), matriz de color 4×5 ("" = sin color) y viñeta (0–1). */
expect suspend fun procesarFoto(dataUrl: String, giro: Int, espejo: Boolean, proporcion: Float, zoom: Float, cx: Float, cy: Float, matriz: String, vineta: Float, max: Int): String
expect suspend fun recortarFoto(dataUrl: String, proporcion: Float, encuadre: Float, max: Int): String
expect suspend fun subirArchivo(url: String, cabeceras: Map<String, String>, base64: String, tipo: String): Respuesta

suspend fun httpTexto(url: String, cabeceras: Map<String, String>): String {
    val r = httpPedido("GET", url, cabeceras, null)
    if (r.estado !in 200..299) throw Exception("HTTP ${r.estado} ${r.texto.take(160)}")
    return r.texto
}
expect suspend fun httpBytes(url: String): ByteArray
expect fun ahoraIso(): String
expect fun hashInicial(): String
expect fun abrirEnlace(url: String)
/** Lee un texto en voz alta (null = callar). */
expect fun leerEnVoz(texto: String?)
/** Pide algo a la IA de la persona (Gemini o Claude, con su clave). Lanza ErrorRevista si no hay clave. */
expect suspend fun pedirIA(prompt: String): String
/** Pone en la radio de la app las canciones que tengas (canción, artista); o todo un artista. Devuelve cuántas encontró, o -1 si aquí no hay radio. */
expect fun ponerEnRadio(canciones: List<Pair<String, String>>, artista: String?): Int
/** Historial del navegador: así el botón «atrás» (o el del ratón) vuelve dentro de la Revista. */
expect fun historialEmpujar()
expect fun historialAtras(n: Int)
expect fun alVolverNavegador(f: () -> Unit)
/** Abre el menú de compartir del teléfono; devuelve false si solo se pudo copiar el enlace. */
expect fun compartir(texto: String, url: String): Boolean
fun compartirEnlace(texto: String, url: String) { if (!compartir(texto, url)) Aviso.mostrar("Enlace copiado: pégalo donde quieras.") }

data class Autor(val id: String, val nombre: String, val avatar: String)
data class Resena(val disco: String, val artista: String, val anio: Int, val nota: Int)
data class Articulo(
    val id: String, val titulo: String, val subtitulo: String, val portada: String, val estilo: Estilo, val fecha: String, val autor: Autor,
    val likes: Int, val comentarios: Int, val texto: String = "", val diseno: Diseno = Diseno(), val resena: Resena? = null,
    val canciones: List<Pair<String, String>> = emptyList(), val galeria: List<String> = emptyList(), val etiquetas: List<String> = emptyList(),
    val recomendados: Int = 0, val estado: String = "", val lecturas: Int = 0
)
data class Comentario(val id: String, val autorId: String, val nombre: String, val avatar: String, val texto: String, val fecha: String, val padre: String = "")
data class Poster(val id: String, val titulo: String, val artista: String, val anio: Int, val mini: String, val grande: String)
data class Concierto(val id: String, val titulo: String, val artista: String, val anio: Int, val miniatura: String, val festival: String, val ciclo: String = "")

/** La misma base de datos que usa la web (Supabase), solo lectura y con la clave pública. */
object Supa {
    const val URL = "https://qfgncdzpcssiirxijdgj.supabase.co"
    const val CLAVE = "sb_publishable_VWLMFbw4lhkV6lW4-DQ2FA_x6E9lVLT"
    const val WEB = "https://arielegno.github.io/Radio/"
    private val UUID = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
    private var sinEstado = false // bases viejas sin la mejora v5 (borradores / programados)

    private suspend fun get(ruta: String): String = Cuenta.api("GET", ruta)
    fun enc(s: String) = s.encodeToByteArray().joinToString("") { b ->
        val c = b.toInt() and 0xFF
        if ((c in 0x30..0x39) || (c in 0x41..0x5A) || (c in 0x61..0x7A) || c == 0x2D || c == 0x2E || c == 0x5F || c == 0x7E) c.toChar().toString() else "%" + c.toString(16).uppercase().padStart(2, '0')
    }
    internal fun publicados() = if (sinEstado) "" else "&status=eq.published&publish_at=lte.${enc(ahoraIso())}"
    private fun cuenta(v: Any?): Int = ((v as? List<*>)?.firstOrNull() as? Map<*, *>)?.get("count")?.let { (it as? Number)?.toInt() } ?: 0

    /** Miniatura del almacén (-t.jpg) para listas; si no existe, la vista usa la grande. */
    fun miniatura(u: String): String = if (Regex("/storage/v1/object/public/revista/.+\\.jpg$").containsMatchIn(u) && !u.endsWith("-t.jpg")) u.removeSuffix(".jpg") + "-t.jpg" else u

    internal fun autor(m: Map<String, Any?>): Autor { val p = m.map("profiles"); return Autor(m.str("author_id"), p.str("name").ifBlank { "Alguien" }, Diseno.imagenOk(p.str("avatar"))) }

    internal fun deFila(m: Map<String, Any?>): Articulo {
        val rv = m.map("review").let { r -> val s = r.num("score").toInt(); if (s in 1..10 && r.str("album").isNotBlank()) Resena(r.str("album").take(100), r.str("artist").take(80), r.num("year").toInt(), s) else null }
        return Articulo(
            id = m.str("id"), titulo = m.str("title"), subtitulo = m.str("subtitle"), portada = Diseno.imagenOk(m.str("cover")), estilo = Estilo.de(m.map("style")),
            fecha = m.str("created_at"), autor = autor(m), likes = cuenta(m["likes"]), comentarios = cuenta(m["comments"]), texto = m.str("body"),
            diseno = Diseno.de(m.map("design")), resena = rv,
            canciones = m.list("tracks").take(12).mapNotNull { it.asObj()?.let { t -> t.str("name") to t.str("artist") } },
            galeria = m.list("gallery").mapNotNull { (it as? String)?.let(Diseno::imagenOk)?.takeIf { u -> u.isNotEmpty() } }.take(8),
            etiquetas = m.list("tags").mapNotNull { it as? String }.take(5),
            recomendados = cuenta(m["recommends"]), estado = m.str("status"), lecturas = m.num("views").toInt()
        )
    }

    suspend fun portada(limite: Int = 30, autores: Set<String>? = null, etiqueta: String? = null): List<Articulo> {
        val sel = "id,title,subtitle,cover,style,review,tags,created_at,author_id,profiles!author_id(name,avatar),likes(count),comments(count)"
        val de = (autores?.filter { UUID.matches(it) }?.take(200)?.joinToString(",")?.let { "&author_id=in.($it)" } ?: "") +
            (etiqueta?.let(Hashtags::normal)?.takeIf { it.isNotEmpty() }?.let { "&tags=cs.%7B${enc(it)}%7D" } ?: "")
        val t = try { get("/rest/v1/posts?select=$sel$de${publicados()}&order=created_at.desc&limit=$limite") }
        catch (e: Exception) { sinEstado = true; get("/rest/v1/posts?select=id,title,subtitle,cover,style,created_at,author_id,profiles!author_id(name),likes(count),comments(count)&order=created_at.desc&limit=$limite") }
        return Json.arr(t).mapNotNull { it.asObj()?.let(::deFila) }
    }

    suspend fun articulo(id: String): Articulo? {
        if (!UUID.matches(id)) return null
        val t = try { get("/rest/v1/posts?select=*,profiles!author_id(name,avatar),likes(count),comments(count),recommends(count)&id=eq.$id") }
            catch (e: Exception) { get("/rest/v1/posts?select=*,profiles!author_id(name,avatar),likes(count),comments(count)&id=eq.$id") }
        return Json.arr(t).firstOrNull()?.asObj()?.let(::deFila)
    }

    suspend fun comentarios(id: String): List<Comentario> {
        if (!UUID.matches(id)) return emptyList()
        val t = get("/rest/v1/comments?select=*,profiles!author_id(name,avatar)&post_id=eq.$id&order=created_at.asc&limit=200")
        return Json.arr(t).mapNotNull { it.asObj()?.let { c -> Comentario(c.str("id"), c.str("author_id"), c.map("profiles").str("name").ifBlank { "Alguien" }, Diseno.imagenOk(c.map("profiles").str("avatar")), c.str("body"), c.str("created_at"), c.str("parent_id")) } }
    }

    suspend fun posters(): List<Poster> {
        val t = get("/rest/v1/posters?select=id,title,artist,year,thumb_url,image_url&order=created_at.desc&limit=40")
        return Json.arr(t).mapNotNull { it.asObj() }.mapNotNull { m ->
            val g = Diseno.imagenOk(m.str("image_url")); val mi = Diseno.imagenOk(m.str("thumb_url")).ifEmpty { g }
            if (mi.isEmpty()) null else Poster(m.str("id"), m.str("title"), m.str("artist"), m.num("year").toInt(), mi, g.ifEmpty { mi })
        }
    }

    suspend fun boveda(): List<Concierto> {
        val hoy = ahoraIso().take(10)
        val t = get("/rest/v1/vault?select=id,title,artist,festival,year,provider,video_id,cover,starts_on,ends_on,cycle&hidden=eq.false&starts_on=lte.$hoy&ends_on=gt.$hoy&order=starts_on.desc&limit=30")
        return Json.arr(t).mapNotNull { it.asObj() }.map { m ->
            val cover = Diseno.imagenOk(m.str("cover")); val p = m.str("provider"); val v = m.str("video_id")
            val mini = cover.ifEmpty { if (p == "youtube" && Regex("^[\\w-]{6,20}$").matches(v)) "https://i.ytimg.com/vi/$v/hqdefault.jpg" else if (p == "archive" && Regex("^[\\w.-]{3,100}$").matches(v)) "https://archive.org/services/img/$v" else "" }
            Concierto(m.str("id"), m.str("title"), m.str("artist"), m.num("year").toInt(), mini, m.str("festival"), m.str("cycle"))
        }
    }
}
