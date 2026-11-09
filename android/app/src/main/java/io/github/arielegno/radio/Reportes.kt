package io.github.arielegno.radio

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Reportes de errores: si la app se cierra sola, se guarda qué pasó para poder arreglarlo.
 * Al volver a abrirla se ofrece copiar o compartir el reporte (no se manda nada solo).
 */
object Reportes {
    private val dir get() = File(RadioApp.app.filesDir, "reportes").apply { mkdirs() }
    /** Reporte nuevo que todavía no se mostró (para el aviso al abrir). */
    var pendiente by mutableStateOf<String?>(null)

    private fun fecha(ms: Long) = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms))

    private fun encabezado(): String {
        val v = try { RadioApp.app.packageManager.getPackageInfo(RadioApp.app.packageName, 0).versionName } catch (e: Exception) { "?" }
        return "Radio de ruta $v · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}\n" +
            "Memoria libre: ${Runtime.getRuntime().let { (it.maxMemory() - (it.totalMemory() - it.freeMemory())) / 1_048_576 }} MB\n"
    }

    private fun guardar(texto: String) {
        try {
            File(dir, "reporte-${System.currentTimeMillis()}.txt").writeText(texto.take(60_000))
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(15)?.forEach { it.delete() }   // los 15 más recientes
        } catch (e: Exception) {}
    }

    /** Se llama al abrir la app: atrapa los errores de Kotlin y revisa por qué se cerró la vez anterior. */
    fun iniciar(ctx: Context) {
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, e ->
            try {
                guardar("CIERRE INESPERADO · ${fecha(System.currentTimeMillis())}\n" + encabezado() + "Hilo: ${hilo.name}\n\n" + android.util.Log.getStackTraceString(e))
                RadioApp.prefs.set("reporteNuevo", true)
            } catch (x: Throwable) {}
            anterior?.uncaughtException(hilo, e)
        }
        // Android 11+: también los cierres del motor nativo (voces), por falta de memoria o por «no responde»
        if (Build.VERSION.SDK_INT >= 30) try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val visto = RadioApp.prefs.str("ultimoCierreVisto", "0").toLongOrNull() ?: 0L
            val salidas = am.getHistoricalProcessExitReasons(ctx.packageName, 0, 5).filter { it.timestamp > visto }
            val malas = salidas.filter { it.reason in setOf(ApplicationExitInfo.REASON_CRASH, ApplicationExitInfo.REASON_CRASH_NATIVE, ApplicationExitInfo.REASON_ANR, ApplicationExitInfo.REASON_LOW_MEMORY) }
            salidas.maxOfOrNull { it.timestamp }?.let { RadioApp.prefs.set("ultimoCierreVisto", it.toString()) }
            for (s in malas) {
                if (s.reason == ApplicationExitInfo.REASON_CRASH && RadioApp.prefs.bool("reporteNuevo", false)) continue   // ya lo guardó el atrapador
                val motivo = when (s.reason) {
                    ApplicationExitInfo.REASON_CRASH_NATIVE -> "Cierre del motor nativo (por ejemplo, voces de la app)"
                    ApplicationExitInfo.REASON_ANR -> "La app dejó de responder (ANR)"
                    ApplicationExitInfo.REASON_LOW_MEMORY -> "Android la cerró por falta de memoria"
                    else -> "Cierre inesperado"
                }
                val traza = try { s.traceInputStream?.use { it.readBytes().toString(Charsets.UTF_8).take(20_000) } } catch (e: Exception) { null }
                guardar("${motivo.uppercase()} · ${fecha(s.timestamp)}\n" + encabezado() + "Descripción: ${s.description ?: "-"} · Memoria usada: ${s.pss / 1024} MB\nLo último que hacía: ${ultimaAccion()}\n\n" + (traza ?: "(Android no guardó más detalles)"))
                RadioApp.prefs.set("reporteNuevo", true)
            }
        } catch (e: Exception) {}
        if (RadioApp.prefs.bool("reporteNuevo", false)) { pendiente = ultimo(); RadioApp.prefs.set("reporteNuevo", false) }
    }

    /** Deja anotado qué estaba haciendo la app (si se cierra de golpe, el reporte lo dice). */
    fun marca(texto: String) { try { File(RadioApp.app.filesDir, "ultima_accion.txt").writeText(java.time.LocalTime.now().withNano(0).toString() + " · " + texto) } catch (e: Exception) {} }
    private fun ultimaAccion(): String = try { File(RadioApp.app.filesDir, "ultima_accion.txt").readText() } catch (e: Exception) { "-" }

    fun todos(): List<File> = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    fun ultimo(): String? = todos().firstOrNull()?.readText()
    fun borrarTodos() { dir.listFiles()?.forEach { it.delete() } }

    /** Para compartir por WhatsApp, correo, etc. */
    fun compartir(ctx: Context, texto: String) {
        val i = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, texto)
        ctx.startActivity(android.content.Intent.createChooser(i, "Mandar reporte").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
