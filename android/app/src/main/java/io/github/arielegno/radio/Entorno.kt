package io.github.arielegno.radio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import io.github.arielegno.radio.core.Clima
import java.util.Locale

/** Ubicación aproximada, clima (Open-Meteo) y nombre de la zona (OpenStreetMap). Todo opcional. */
class Entorno(private val ctx: Context) {
    var lat by mutableStateOf<Double?>(null); private set
    var lon by mutableStateOf<Double?>(null); private set
    var zona by mutableStateOf(""); private set
    var clima by mutableStateOf(""); private set
    private var climaT = 0L
    private var zonaPos: Pair<Double, Double>? = null
    private var escuchando = false

    fun permitido(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private val oyente = object : LocationListener {
        override fun onLocationChanged(location: Location) { nueva(location) }
        @Deprecated("Requerido en Android viejos") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    /** Empieza a seguir la ubicación (cada ~2 minutos o 300 m). */
    @SuppressLint("MissingPermission")
    fun iniciar() {
        if (escuchando || !permitido() || !RadioApp.prefs.bool("ubicacion", true)) return
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        try {
            val provs = lm.getProviders(true)
            provs.mapNotNull { p -> try { lm.getLastKnownLocation(p) } catch (e: SecurityException) { null } }
                .maxByOrNull { it.time }?.let { nueva(it) }
            for (p in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)) {
                if (p in provs) lm.requestLocationUpdates(p, 120_000L, 300f, oyente, Looper.getMainLooper())
            }
            escuchando = true
        } catch (e: Exception) { /* sin ubicación: la radio sigue igual */ }
    }

    fun detener() {
        if (!escuchando) return
        try { (ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.removeUpdates(oyente) } catch (e: Exception) {}
        escuchando = false
    }

    private fun nueva(l: Location) {
        lat = l.latitude; lon = l.longitude
        val zp = zonaPos
        val lejos = zp == null || distanciaKm(zp.first, zp.second, l.latitude, l.longitude) > 1.5
        RadioApp.scope.launchSafe {
            if (lejos) actualizarZona(l.latitude, l.longitude)
            actualizarClima(forzar = lejos)
        }
    }

    private suspend fun actualizarZona(la: Double, lo: Double) {
        if (!Red.hayInternet(ctx)) return
        try {
            val j = Red.get(String.format(Locale.ROOT, "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=16&addressdetails=1&accept-language=es&lat=%.4f&lon=%.4f", la, lo))
            val z = Clima.zonaNominatim(j)
            if (z.isNotEmpty()) { zona = z; zonaPos = la to lo }
        } catch (e: Exception) {}
    }

    suspend fun actualizarClima(forzar: Boolean = false) {
        val la = lat ?: return; val lo = lon ?: return
        if (!forzar && System.currentTimeMillis() - climaT < 20 * 60_000L) return
        if (!Red.hayInternet(ctx)) return
        try {
            val f = RadioApp.prefs.str("unidades", "c") == "f"
            val j = Red.get(String.format(Locale.ROOT, "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code&timezone=auto%s", la, lo, if (f) "&temperature_unit=fahrenheit" else ""))
            Clima.deOpenMeteo(j, f)?.let { clima = it; climaT = System.currentTimeMillis() }
        } catch (e: Exception) {}
    }

    companion object {
        fun distanciaKm(a1: Double, o1: Double, a2: Double, o2: Double): Double {
            val r = 6371.0; val dA = Math.toRadians(a2 - a1); val dO = Math.toRadians(o2 - o1)
            val h = Math.sin(dA / 2) * Math.sin(dA / 2) + Math.cos(Math.toRadians(a1)) * Math.cos(Math.toRadians(a2)) * Math.sin(dO / 2) * Math.sin(dO / 2)
            return 2 * r * Math.asin(Math.sqrt(h))
        }
    }
}
