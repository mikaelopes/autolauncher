package com.mls.painel

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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

/** Velocidade direto do GPS da central. Não usa internet nem Google Play Services. */
class GpsRepo(private val ctx: Context) : LocationListener {
    private val hp = ctx.getSharedPreferences("gps", Context.MODE_PRIVATE)
    private var prefsHeading: Float
        get() = hp.getFloat("heading", 0f)
        set(v) = hp.edit().putFloat("heading", v).apply()

    var speedKmh by mutableFloatStateOf(0f)
        private set
    var lastFix by mutableLongStateOf(0L)
        private set
    var lastLoc: Location? = null
        private set

    /** Direção do carro em graus (0 = norte). Fica guardada quando o carro para. */
    var heading by mutableFloatStateOf(prefsHeading)
        private set
    private var headingAnchor: Location? = null

    private var running = false
    private val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasFix(now: Long): Boolean = now - lastFix < 5000

    @SuppressLint("MissingPermission")
    fun start() {
        if (running || !hasPermission()) return
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, this, Looper.getMainLooper())
            running = true
            if (lastLoc == null) lastLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            running = false
        }
    }

    fun stop() {
        if (!running) return
        try {
            lm.removeUpdates(this)
        } catch (e: Exception) {
        }
        running = false
    }

    @SuppressLint("MissingPermission")
    fun lastKnown(): Location? {
        if (!hasPermission()) return null
        return try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            null
        }
    }

    override fun onLocationChanged(location: Location) {
        val prev = lastLoc
        var s = when {
            location.hasSpeed() -> location.speed * 3.6f
            prev != null && location.time > prev.time ->
                prev.distanceTo(location) / ((location.time - prev.time) / 1000f) * 3.6f
            else -> 0f
        }
        if (s < 2f) s = 0f
        speedKmh = s
        // Direção: usa a do GPS em movimento; senão calcula pelo deslocamento (mín. 8 m)
        val anchor = headingAnchor
        when {
            location.hasBearing() && s > 6f -> {
                heading = location.bearing
                headingAnchor = location
            }
            anchor == null -> headingAnchor = location
            anchor.distanceTo(location) > 8f -> {
                heading = anchor.bearingTo(location).let { if (it < 0) it + 360f else it }
                headingAnchor = location
            }
        }
        prefsHeading = heading
        lastFix = System.currentTimeMillis()
        lastLoc = location
    }

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
