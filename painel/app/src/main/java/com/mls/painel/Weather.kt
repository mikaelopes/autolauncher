package com.mls.painel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Weather(val temp: Float, val code: Int, val isDay: Boolean)

/** Open-Meteo: grátis, sem chave. Só é chamado quando há internet; o último valor fica salvo. */
suspend fun fetchWeather(lat: Double, lon: Double): Weather? = withContext(Dispatchers.IO) {
    var conn: HttpURLConnection? = null
    try {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code,is_day"
        )
        conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val cur = JSONObject(text).getJSONObject("current")
        Weather(
            temp = cur.getDouble("temperature_2m").toFloat(),
            code = cur.getInt("weather_code"),
            isDay = cur.optInt("is_day", 1) == 1
        )
    } catch (e: Exception) {
        null
    } finally {
        conn?.disconnect()
    }
}
