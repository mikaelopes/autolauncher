package com.mls.painel

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** Nascer e pôr do sol calculados pela posição (sem internet). Algoritmo do Almanaque Náutico. */
object Sun {
    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun n360(x: Double) = ((x % 360) + 360) % 360
    private fun n24(x: Double) = ((x % 24) + 24) % 24

    /** (nascer, pôr) de hoje em milissegundos, ou null perto dos polos. */
    fun today(lat: Double, lon: Double, now: Long = System.currentTimeMillis()): Pair<Long, Long>? {
        val local = Calendar.getInstance().apply { timeInMillis = now }
        val day = local.get(Calendar.DAY_OF_YEAR)
        fun utcHour(rise: Boolean): Double? {
            val lngHour = lon / 15.0
            val t = day + ((if (rise) 6.0 else 18.0) - lngHour) / 24.0
            val m = 0.9856 * t - 3.289
            val l = n360(m + 1.916 * sin(rad(m)) + 0.020 * sin(rad(2 * m)) + 282.634)
            var ra = n360(deg(atan(0.91764 * tan(rad(l)))))
            ra = (ra + (floor(l / 90) * 90 - floor(ra / 90) * 90)) / 15.0
            val sinDec = 0.39782 * sin(rad(l))
            val cosDec = cos(asin(sinDec))
            val cosH = (cos(rad(90.833)) - sinDec * sin(rad(lat))) / (cosDec * cos(rad(lat)))
            if (cosH > 1 || cosH < -1) return null
            val h = (if (rise) 360 - deg(acos(cosH)) else deg(acos(cosH))) / 15.0
            return n24(h + ra - 0.06571 * t - 6.622 - lngHour)
        }
        val r = utcHour(true) ?: return null
        val s = utcHour(false) ?: return null
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        }.timeInMillis
        var riseMs = utc + (r * 3_600_000).toLong()
        var setMs = utc + (s * 3_600_000).toLong()
        if (setMs < riseMs) setMs += 86_400_000L
        // Ajusta para o dia local de hoje
        val dayStart = (local.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        while (riseMs < dayStart) { riseMs += 86_400_000L; setMs += 86_400_000L }
        while (riseMs >= dayStart + 86_400_000L) { riseMs -= 86_400_000L; setMs -= 86_400_000L }
        return riseMs to setMs
    }
}
