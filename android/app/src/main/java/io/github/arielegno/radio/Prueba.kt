package io.github.arielegno.radio

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

/**
 * Solo en la versión de prueba (la que usa el teléfono virtual): un «control remoto» para las pruebas automáticas.
 * Deja cambiar ajustes, pedirle a Luna que diga un texto y guarda lo que dice como audio para revisarlo.
 * En la app que instalas no existe: no se activa nunca.
 */
object Prueba {
    @Volatile var activa = false; private set
    private val n = AtomicInteger()

    fun log(s: String) { if (activa) Log.i("RadioPrueba", s) }

    /** Guarda lo que dijo la voz (ya con su efecto) como .wav en files/prueba-voz. */
    fun grabar(nombre: String, sr: Int, partes: List<FloatArray>) {
        if (!activa) return
        try {
            val total = partes.sumOf { it.size }
            val dir = File(RadioApp.app.filesDir, "prueba-voz").apply { mkdirs() }
            val f = File(dir, "%03d-%s.wav".format(n.incrementAndGet(), nombre.replace(Regex("[^A-Za-z0-9_.-]"), "_")))
            val b = ByteBuffer.allocate(44 + total * 2).order(ByteOrder.LITTLE_ENDIAN)
            b.put("RIFF".toByteArray()).putInt(36 + total * 2).put("WAVE".toByteArray()).put("fmt ".toByteArray())
                .putInt(16).putShort(1).putShort(1).putInt(sr).putInt(sr * 2).putShort(2).putShort(16).put("data".toByteArray()).putInt(total * 2)
            partes.forEach { a -> a.forEach { x -> b.putShort((x.coerceIn(-1f, 1f) * 32767).toInt().toShort()) } }
            FileOutputStream(f).use { it.write(b.array()) }
            log("grabado ${f.name} ${total * 1000L / sr} ms")
        } catch (e: Throwable) { Log.w("RadioPrueba", "no se pudo grabar", e) }
    }

    fun iniciar(app: Application) {
        if ((app.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0) return
        activa = true
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { try { orden(i) } catch (e: Throwable) { Log.w("RadioPrueba", "orden falló", e) } }
        }
        val f = IntentFilter("io.github.arielegno.radio.PRUEBA")
        if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(r, f, Context.RECEIVER_EXPORTED) else app.registerReceiver(r, f)
    }

    private fun orden(i: Intent) {
        val p = RadioApp.prefs; val radio = RadioApp.radio
        val o = i.getStringExtra("orden") ?: return
        val k = i.getStringExtra("k") ?: ""
        val v = if (i.getBooleanExtra("vacio", false)) "" else i.getStringExtra("b64")?.let { String(Base64.decode(it, Base64.DEFAULT), Charsets.UTF_8) } ?: i.getStringExtra("v") ?: ""
        when (o) {
            "texto" -> p.set(k, v)
            "bool" -> p.set(k, v.toBoolean())
            "int" -> p.set(k, v.toInt())
            "motor" -> radio.voz.cambiarMotor(v)
            "decir" -> RadioApp.scope.launchSafe { log("decir: empieza"); radio.probarTexto(v); log("decir: termina · ${radio.estadoPrueba()}") }
            "estado" -> {}
        }
        log("orden $o $k listo · ${radio.estadoPrueba()}")
    }
}
