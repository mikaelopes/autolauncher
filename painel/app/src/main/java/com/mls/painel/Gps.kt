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
    var speedKmh by mutableFloatStateOf(0f)
        private set
    var lastFix by mutableLongStateOf(0L)
        private set
    var lastLoc: Location? = null
        private set

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
        lastFix = System.currentTimeMillis()
        lastLoc = location
    }

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
