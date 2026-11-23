package com.estudio.musical.data

import android.content.Context
import android.os.Environment
import com.estudio.musical.audio.AudioData
import com.estudio.musical.audio.MotorAudio
import com.estudio.musical.audio.Wav
import com.estudio.musical.audio.synth.Instrumento
import com.estudio.musical.audio.synth.Instrumentos
import com.estudio.musical.audio.synth.Muestra
import com.estudio.musical.audio.synth.Onda
import com.estudio.musical.audio.synth.TipoSintesis
import com.estudio.musical.audio.synth.FuenteSonido
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Todas las carpetas de la app. Todo queda en el teléfono: nada se sube a internet. */
object Carpetas {
    fun grabaciones(ctx: Context) = File(ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "grabaciones").apply { mkdirs() }
    fun exportados(ctx: Context) = File(ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "exportados").apply { mkdirs() }
    fun muestras(ctx: Context) = File(ctx.filesDir, "muestras").apply { mkdirs() }
    fun proyectos(ctx: Context) = File(ctx.filesDir, "proyectos").apply { mkdirs() }
    fun notasVoz(ctx: Context, cancionId: String) = File(ctx.filesDir, "notas/$cancionId").apply { mkdirs() }
    fun pdf(ctx: Context) = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "letras").apply { mkdirs() }
    fun temporal(ctx: Context) = File(ctx.cacheDir, "edicion").apply { mkdirs() }

    fun listaGrabaciones(ctx: Context): List<File> =
        grabaciones(ctx).listFiles { f -> f.extension == "wav" }?.sortedByDescending { it.lastModified() } ?: emptyList()

    /** Nombre libre: "Toma", "Toma 2", "Toma 3"… */
    fun nombreLibre(dir: File, base: String, ext: String = "wav"): File {
        val limpio = base.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "Sin nombre" }
        var f = File(dir, "$limpio.$ext"); var i = 2
        while (f.exists()) { f = File(dir, "$limpio $i.$ext"); i++ }
        return f
    }
}

/** Ajustes simples guardados en el teléfono */
class Ajustes(ctx: Context) {
    private val p = ctx.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
    var latenciaMs: Int
        get() = p.getInt("latencia", 120)
        set(v) = p.edit().putInt("latencia", v).apply()
    var bpm: Int
        get() = p.getInt("bpm", 100)
        set(v) = p.edit().putInt("bpm", v).apply()
    var proyectoActual: String?
        get() = p.getString("proyecto", null)
        set(v) = p.edit().putString("proyecto", v).apply()
}

// ───────────────────────── Proyectos multipista ─────────────────────────

data class PistaProyecto(
    val archivo: String,
    val nombre: String,
    val volumen: Float = 1f,
    val paneo: Float = 0f,
    val inicioMs: Int = 0,
    val silencio: Boolean = false,
    val solo: Boolean = false
)

data class Proyecto(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val tonalidad: String = "",
    val bpm: Int = 100,
    val pistas: List<PistaProyecto> = emptyList(),
    val actualizado: Long = System.currentTimeMillis()
)

class ProyectosRepo(private val ctx: Context) {
    private fun archivo(id: String) = File(Carpetas.proyectos(ctx), "$id.json")

    fun todos(): List<Proyecto> = Carpetas.proyectos(ctx).listFiles { f -> f.extension == "json" }
        ?.mapNotNull { runCatching { leer(JSONObject(it.readText())) }.getOrNull() }
        ?.sortedByDescending { it.actualizado } ?: emptyList()

    fun obtener(id: String?): Proyecto? = id?.let { archivo(it) }?.takeIf { it.exists() }
        ?.let { runCatching { leer(JSONObject(it.readText())) }.getOrNull() }

    fun guardar(p: Proyecto) {
        val o = JSONObject().put("id", p.id).put("nombre", p.nombre).put("tonalidad", p.tonalidad)
            .put("bpm", p.bpm).put("actualizado", System.currentTimeMillis())
        val arr = JSONArray()
        p.pistas.forEach {
            arr.put(JSONObject().put("archivo", it.archivo).put("nombre", it.nombre).put("volumen", it.volumen.toDouble())
                .put("paneo", it.paneo.toDouble()).put("inicioMs", it.inicioMs).put("silencio", it.silencio).put("solo", it.solo))
        }
        o.put("pistas", arr)
        archivo(p.id).writeText(o.toString(2))
    }

    fun borrar(id: String) { archivo(id).delete() }

    private fun leer(o: JSONObject): Proyecto {
        val arr = o.optJSONArray("pistas") ?: JSONArray()
        val pistas = (0 until arr.length()).map {
            val t = arr.getJSONObject(it)
            PistaProyecto(t.getString("archivo"), t.getString("nombre"), t.optDouble("volumen", 1.0).toFloat(),
                t.optDouble("paneo", 0.0).toFloat(), t.optInt("inicioMs"), t.optBoolean("silencio"), t.optBoolean("solo"))
        }
        return Proyecto(o.getString("id"), o.getString("nombre"), o.optString("tonalidad"), o.optInt("bpm", 100),
            pistas, o.optLong("actualizado"))
    }
}

// ───────────────────────── Muestras (sampler) ─────────────────────────

data class InfoMuestra(val nombre: String, val archivo: File, val notaRaiz: Int, val bucle: Boolean)

class MuestrasRepo(private val ctx: Context) {
    private val meta get() = File(Carpetas.muestras(ctx), "muestras.json")

    fun todas(): List<InfoMuestra> {
        if (!meta.exists()) return emptyList()
        val arr = JSONArray(meta.readText())
        return (0 until arr.length()).map { arr.getJSONObject(it) }.map {
            InfoMuestra(it.getString("nombre"), File(Carpetas.muestras(ctx), it.getString("archivo")),
                it.getInt("raiz"), it.optBoolean("bucle"))
        }.filter { it.archivo.exists() }
    }

    fun guardar(m: Muestra): InfoMuestra {
        val f = Carpetas.nombreLibre(Carpetas.muestras(ctx), m.nombre)
        Wav.write(f, AudioData(m.datos, m.sr))
        val info = InfoMuestra(f.nameWithoutExtension, f, m.notaRaiz, m.bucle)
        escribir(todas().filter { it.nombre != info.nombre } + info)
        return info
    }

    fun actualizar(info: InfoMuestra) = escribir(todas().map { if (it.archivo == info.archivo) info else it })

    fun borrar(info: InfoMuestra) { info.archivo.delete(); escribir(todas().filter { it.archivo != info.archivo }) }

    fun cargar(info: InfoMuestra): Muestra {
        val a = Wav.readAt(info.archivo, MotorAudio.SR)
        return Muestra(info.nombre, a.samples, a.sampleRate, info.notaRaiz, info.bucle)
    }

    private fun escribir(l: List<InfoMuestra>) {
        val arr = JSONArray()
        l.forEach { arr.put(JSONObject().put("nombre", it.nombre).put("archivo", it.archivo.name).put("raiz", it.notaRaiz).put("bucle", it.bucle)) }
        meta.writeText(arr.toString())
    }
}

// ───────────────────────── Sonidos de sintetizador propios ─────────────────────────

class SonidosRepo(ctx: Context) {
    private val f = File(ctx.filesDir, "sonidos.json")

    fun propios(): List<Instrumento> {
        if (!f.exists()) return emptyList()
        val arr = JSONArray(f.readText())
        return (0 until arr.length()).mapNotNull { runCatching { leer(arr.getJSONObject(it)) }.getOrNull() }
    }

    fun guardar(i: Instrumento) = escribir(propios().filter { it.nombre != i.nombre } + i)
    fun borrar(nombre: String) = escribir(propios().filter { it.nombre != nombre })

    private fun escribir(l: List<Instrumento>) {
        val arr = JSONArray(); l.forEach { arr.put(json(it)) }; f.writeText(arr.toString())
    }

    private fun json(i: Instrumento) = JSONObject().apply {
        put("nombre", i.nombre); put("tipo", i.tipo.name); put("onda1", i.onda1.name); put("onda2", i.onda2.name)
        put("mezcla2", i.mezcla2.toDouble()); put("octava2", i.octava2); put("desafinar", i.desafinar.toDouble())
        put("ataque", i.ataque.toDouble()); put("caida", i.caida.toDouble()); put("sostener", i.sostener.toDouble())
        put("liberar", i.liberar.toDouble()); put("corte", i.corte.toDouble()); put("resonancia", i.resonancia.toDouble())
        put("envFiltro", i.envFiltro.toDouble()); put("caidaFiltro", i.caidaFiltro.toDouble())
        put("fmRatio", i.fmRatio.toDouble()); put("fmIndice", i.fmIndice.toDouble()); put("fmCaida", i.fmCaida.toDouble())
        put("registros", JSONArray(i.registros.map { it.toDouble() })); put("vibrato", i.vibrato.toDouble())
        put("volumen", i.volumen.toDouble())
    }

    private fun leer(o: JSONObject): Instrumento {
        fun fl(k: String, d: Float) = o.optDouble(k, d.toDouble()).toFloat()
        val reg = o.optJSONArray("registros")
        return Instrumento(
            o.getString("nombre"), TipoSintesis.valueOf(o.getString("tipo")),
            Onda.valueOf(o.optString("onda1", "SIERRA")), Onda.valueOf(o.optString("onda2", "CUADRADA")),
            fl("mezcla2", 0f), o.optInt("octava2"), fl("desafinar", 0f), fl("ataque", 0.005f), fl("caida", 0.3f),
            fl("sostener", 0.7f), fl("liberar", 0.3f), fl("corte", 6000f), fl("resonancia", 0.2f), fl("envFiltro", 0f),
            fl("caidaFiltro", 0.3f), 0f, fl("fmRatio", 1f), fl("fmIndice", 0f), fl("fmCaida", 1f),
            if (reg != null) FloatArray(reg.length()) { reg.getDouble(it).toFloat() } else floatArrayOf(0f, 1f, 0f, 0f, 0f, 0f),
            fl("vibrato", 0f), fl("volumen", 0.4f)
        )
    }
}

/** Todas las fuentes de sonido disponibles: instrumentos de fábrica, sonidos propios y muestras grabadas */
object Fuentes {
    fun todas(ctx: Context): List<FuenteSonido> =
        Instrumentos.TODOS + SonidosRepo(ctx).propios() + MuestrasRepo(ctx).todas().map { MuestrasRepo(ctx).cargar(it) }
}
