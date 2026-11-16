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
        PDFBoxResourceLoader.init(applicationContext)
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
            .apply()
    }
}
