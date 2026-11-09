package io.github.arielegno.radio

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import io.github.arielegno.radio.core.Estadisticas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/** Punto de entrada: crea una sola vez la biblioteca, las estadísticas y el motor de la radio. */
class RadioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
        rr.revista.PlataformaAndroid.ctx = this
        prefs = Prefs(getSharedPreferences("radio", Context.MODE_PRIVATE))
        Reportes.iniciar(this)
        biblioteca = Biblioteca(this)
        stats = Datos.cargarStats(this)
        radio = Radio(this)
    }

    // Cuando Android avisa que queda poca memoria, se sueltan las portadas en memoria y la voz si no está hablando
    // (antes Android cerraba la app por falta de memoria)
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) io.github.arielegno.radio.ui.Imagenes.vaciar()
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW || level == android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            if (level != android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN || !radio.activa) VozPropia.soltarMemoria()
        }
    }

    companion object {
        lateinit var app: RadioApp; private set
        lateinit var prefs: Prefs; private set
        lateinit var biblioteca: Biblioteca; private set
        lateinit var stats: Estadisticas; private set
        lateinit var radio: Radio; private set
        /** Tareas de fondo de la app (sobreviven a girar la pantalla). */
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate +
            kotlinx.coroutines.CoroutineExceptionHandler { _, e -> Reportes.error("tarea de fondo", e) })   // un error en segundo plano ya no cierra la app
    }
}

/**
 * Datos ya generados (biografías, fichas, fotos, IA, guiones…): cada uno en su propio archivo.
 * Antes iban en los ajustes y, con miles de canciones, los ajustes crecían tanto que la app se trababa.
 * Se conservan al actualizar la app.
 */
object Almacen {
    private val dir by lazy { java.io.File(RadioApp.app.filesDir, "datos").apply { mkdirs() } }
    private val memoria = object : android.util.LruCache<String, String>(2 * 1024 * 1024) { override fun sizeOf(key: String, value: String) = value.length * 2 }
    private val PREFIJOS = listOf("bio_v", "artof_", "artimg_", "wiki_v", "nube_", "miembro_v", "festival_v", "origenes", "guiaRonda", "guion_")
    fun esDato(k: String) = PREFIJOS.any { k.startsWith(it) } || (k.startsWith("bio_") && !k.startsWith("bio_user_"))
    private fun archivo(k: String): java.io.File {
        val limpio = k.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return java.io.File(dir, if (limpio.length <= 120) limpio else limpio.take(80) + "_" + k.hashCode().toUInt().toString(16))
    }
    fun leer(k: String): String? { memoria.get(k)?.let { return it }; return try { archivo(k).takeIf { it.exists() }?.readText()?.also { memoria.put(k, it) } } catch (e: Exception) { null } }
    fun escribir(k: String, v: String) { memoria.put(k, v); try { Datos.escribir(archivo(k), v) } catch (e: Exception) {} }
    fun borrar(k: String) { memoria.remove(k); archivo(k).delete() }
    /** Cuánto ocupan los datos guardados. */
    fun tamano(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L
    fun vaciar() { memoria.evictAll(); dir.listFiles()?.forEach { it.delete() } }
}

/** Ajustes guardados en el teléfono. */
class Prefs(private val sp: SharedPreferences) {
    init {
        // Mudanza única: los datos grandes salen de los ajustes a sus propios archivos
        try {
            val viejos = sp.all.filterKeys { Almacen.esDato(it) }
            if (viejos.isNotEmpty()) {
                viejos.forEach { (k, v) -> if (v != null) Almacen.escribir(k, v.toString()) }
                val e = sp.edit(); viejos.keys.forEach { e.remove(it) }; e.apply()
            }
        } catch (e: Exception) {}
    }
    // Si un ajuste se guardó con otro tipo (por ejemplo, al importar una copia), se lee igual sin cerrar la app.
    fun str(k: String, d: String = ""): String {
        if (Almacen.esDato(k)) return Almacen.leer(k)?.takeIf { it.isNotEmpty() } ?: d
        return try { sp.getString(k, null) } catch (e: ClassCastException) { sp.all[k]?.toString() }?.takeIf { it.isNotEmpty() } ?: d
    }
    fun int(k: String, d: Int): Int = try { sp.getInt(k, d) } catch (e: ClassCastException) {
        when (val v = sp.all[k]) { is Long -> v.toInt(); is Float -> v.toInt(); is String -> v.toIntOrNull() ?: d; else -> d }
    }
    fun bool(k: String, d: Boolean): Boolean = try { sp.getBoolean(k, d) } catch (e: ClassCastException) {
        when (val v = sp.all[k]) { is String -> v == "1" || v == "true"; is Int -> v != 0; else -> d }
    }
    /** Todos los ajustes (para la copia de ajustes). */
    fun todo(): Map<String, Any?> = sp.all.toMap()
    /** Restaura ajustes de una copia (sin tocar los que no vienen). */
    fun ponerTodo(m: Map<String, Any?>) {
        val e = sp.edit()
        m.forEach { (k, v) -> when (v) { is Boolean -> e.putBoolean(k, v); is Number -> if (v is Double && v != Math.floor(v)) e.putFloat(k, v.toFloat()) else e.putInt(k, v.toInt()); is String -> e.putString(k, v); else -> {} } }
        e.apply()
    }
    fun set(k: String, v: String) { if (Almacen.esDato(k)) Almacen.escribir(k, v) else sp.edit().putString(k, v).apply() }
    fun set(k: String, v: Int) = sp.edit().putInt(k, v).apply()
    fun set(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()
    fun remove(k: String) { if (Almacen.esDato(k)) Almacen.borrar(k) else sp.edit().remove(k).apply() }
}

/** Guardado de archivos de datos (escritura segura: primero a un temporal y luego se renombra). */
object Datos {
    fun escribir(f: File, texto: String) {
        f.parentFile?.mkdirs()
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(texto)
        if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
    }

    fun leer(f: File): String? = try { if (f.exists()) f.readText() else null } catch (e: Exception) { null }

    fun cargarStats(c: Context): Estadisticas = Estadisticas.fromJson(leer(File(c.filesDir, "estadisticas.json")))

    /** Guarda las estadísticas en segundo plano. */
    fun guardarStats() {
        val c = RadioApp.app; val texto = RadioApp.stats.toJson()
        RadioApp.scope.launch(Dispatchers.IO) { synchronized(this@Datos) { escribir(File(c.filesDir, "estadisticas.json"), texto) } }
    }
}
