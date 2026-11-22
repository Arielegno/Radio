package com.hilo.lector

import android.app.Application
import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HiloApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.hilo.lector.ui.CrashLog.install(applicationContext)
        // Lee la biblioteca, las tarjetas y las clases en paralelo mientras Android arma la pantalla
        Preload.start(this)
        PDFBoxResourceLoader.init(applicationContext)
        // Si Android marcó a Hilo como «no responde», guarda qué estaba haciendo para poder arreglarlo
        Thread { runCatching { com.hilo.lector.ui.CrashLog.captureAnr(applicationContext) } }.start()
        // Modo de depuración: registra en el log el trabajo lento en la pantalla y los archivos sin cerrar.
        // Solo se activa si existe el archivo files/depurar (lo crea la prueba automática).
        if (java.io.File(filesDir, "depurar").exists()) {
            android.os.StrictMode.setThreadPolicy(android.os.StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().detectLeakedSqlLiteObjects()
                    .detectActivityLeaks().detectFileUriExposure().penaltyLog().build()
            )
        }
    }
}

/**
 * Carga anticipada: abrir la biblioteca, las tarjetas, el motor y las clases leía y armaba varios archivos
 * uno tras otro en el hilo de la pantalla (más de 1 s al abrir, según la prueba de estrés).
 * Ahora empiezan juntos, en segundo plano, apenas arranca Hilo; la pantalla solo espera lo que falte.
 */
object Preload {
    private val pool = java.util.concurrent.Executors.newFixedThreadPool(3) { r -> Thread(r, "hilo-carga") }
    private var repo: java.util.concurrent.Future<com.hilo.lector.data.LibraryRepo>? = null
    private var settings: java.util.concurrent.Future<com.hilo.lector.data.SettingsStore>? = null
    private var study: java.util.concurrent.Future<com.hilo.lector.data.StudyStore>? = null
    private var motor: java.util.concurrent.Future<com.hilo.lector.data.Motor>? = null
    private var classes: java.util.concurrent.Future<com.hilo.lector.data.ClassStore>? = null

    @Synchronized fun start(app: Application) {
        if (repo != null) return
        fun <T> timed(name: String, make: () -> T): java.util.concurrent.Future<T> = pool.submit(java.util.concurrent.Callable {
            val t = System.currentTimeMillis()
            make().also { android.util.Log.i("HiloPerf", "carga $name: ${System.currentTimeMillis() - t} ms") }
        })
        study = timed("tarjetas") { com.hilo.lector.data.StudyStore(app) }
        repo = timed("biblioteca") { com.hilo.lector.data.LibraryRepo(app) }
        motor = timed("motor") { com.hilo.lector.data.Motor(app) }
        settings = timed("ajustes") { com.hilo.lector.data.SettingsStore(app) }
        classes = timed("clases") { com.hilo.lector.data.ClassStore(app) }
    }

    private fun <T> take(f: java.util.concurrent.Future<T>?, make: () -> T): T =
        f?.let { runCatching { it.get() }.getOrNull() } ?: make()

    fun repo(app: Application) = take(repo) { com.hilo.lector.data.LibraryRepo(app) }
    fun settings(app: Application) = take(settings) { com.hilo.lector.data.SettingsStore(app) }
    fun study(app: Application) = take(study) { com.hilo.lector.data.StudyStore(app) }
    fun motor(app: Application) = take(motor) { com.hilo.lector.data.Motor(app) }
    fun classes(app: Application) = take(classes) { com.hilo.lector.data.ClassStore(app) }
}

/** Trabajo que debe terminar aunque se cierre una pantalla (guardar, cerrar archivos). */
object Hilo {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

/** Lo que llevas hoy: sesiones del temporizador, minutos de enfoque y páginas. */
object Stats {
    data class Today(val sessions: Int, val minutes: Int, val pages: Int)

    private fun key() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun today(ctx: Context): Today {
        val sp = ctx.getSharedPreferences("stats", Context.MODE_PRIVATE)
        if (sp.getString("day", "") != key()) return Today(0, 0, 0)
        return Today(sp.getInt("s", 0), sp.getInt("m", 0), sp.getInt("p", 0))
    }

    fun add(ctx: Context, sessions: Int = 0, minutes: Int = 0, pages: Int = 0) {
        val sp = ctx.getSharedPreferences("stats", Context.MODE_PRIVATE)
        val t = today(ctx)
        sp.edit()
            .putString("day", key())
            .putInt("s", t.sessions + sessions)
            .putInt("m", t.minutes + minutes)
            .putInt("p", t.pages + pages)
            .putInt("m_" + key(), t.minutes + minutes)
            .putInt("p_" + key(), t.pages + pages)
            .apply()
    }

    /** Minutos y páginas de los últimos [days] días (el último es hoy). */
    fun history(ctx: Context, days: Int = 14): List<Pair<Int, Int>> {
        val sp = ctx.getSharedPreferences("stats", Context.MODE_PRIVATE)
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -(days - 1))
        return (0 until days).map {
            val k = f.format(cal.time)
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            sp.getInt("m_$k", 0) to sp.getInt("p_$k", 0)
        }
    }
}
