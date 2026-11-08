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
        prefs = Prefs(getSharedPreferences("radio", Context.MODE_PRIVATE))
        biblioteca = Biblioteca(this)
        stats = Datos.cargarStats(this)
        radio = Radio(this)
    }

    companion object {
        lateinit var app: RadioApp; private set
        lateinit var prefs: Prefs; private set
        lateinit var biblioteca: Biblioteca; private set
        lateinit var stats: Estadisticas; private set
        lateinit var radio: Radio; private set
        /** Tareas de fondo de la app (sobreviven a girar la pantalla). */
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}

/** Ajustes guardados en el teléfono. */
class Prefs(private val sp: SharedPreferences) {
    fun str(k: String, d: String = ""): String = sp.getString(k, null)?.takeIf { it.isNotEmpty() } ?: d
    fun int(k: String, d: Int): Int = sp.getInt(k, d)
    fun bool(k: String, d: Boolean): Boolean = sp.getBoolean(k, d)
    fun set(k: String, v: String) = sp.edit().putString(k, v).apply()
    fun set(k: String, v: Int) = sp.edit().putInt(k, v).apply()
    fun set(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()
    fun remove(k: String) = sp.edit().remove(k).apply()
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
