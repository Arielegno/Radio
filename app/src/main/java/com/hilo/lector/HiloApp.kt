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
