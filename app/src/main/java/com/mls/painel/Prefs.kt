package com.mls.painel

import android.content.Context

data class Slot(val pkg: String, val label: String)

class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("painel", Context.MODE_PRIVATE)

    fun slot(i: Int): Slot = Slot(
        p.getString("s${i}_pkg", "") ?: "",
        p.getString("s${i}_label", DEFAULT_LABELS[i]) ?: DEFAULT_LABELS[i]
    )

    fun setSlot(i: Int, s: Slot) {
        p.edit().putString("s${i}_pkg", s.pkg).putString("s${i}_label", s.label).apply()
    }

    var initialized: Boolean
        get() = p.getBoolean("initialized", false)
        set(v) = p.edit().putBoolean("initialized", v).apply()

    var hintShown: Boolean
        get() = p.getBoolean("hint_shown", false)
        set(v) = p.edit().putBoolean("hint_shown", v).apply()

    var eqPkg: String
        get() = p.getString("eq_pkg", "") ?: ""
        set(v) = p.edit().putString("eq_pkg", v).apply()

    var leftSide: Boolean
        get() = p.getBoolean("left_side", true)
        set(v) = p.edit().putBoolean("left_side", v).apply()

    var order: List<String>
        get() {
            val raw = (p.getString("order", DEFAULT_ORDER) ?: DEFAULT_ORDER).split(",")
            return if (raw.size == 3 && raw.toSet() == DEFAULT_ORDER.split(",").toSet()) raw
            else DEFAULT_ORDER.split(",")
        }
        set(v) = p.edit().putString("order", v.joinToString(",")).apply()

    var accent: Int
        get() = p.getInt("accent", 0)
        set(v) = p.edit().putInt("accent", v).apply()

    var base: Int
        get() = p.getInt("base", 0)
        set(v) = p.edit().putInt("base", v).apply()

    /** 0 = desligado, 1 = automático, 2 = sempre */
    var nightMode: Int
        get() = p.getInt("night_mode", 1)
        set(v) = p.edit().putInt("night_mode", v).apply()

    /** true = tema claro de alto contraste durante o dia */
    var dayLight: Boolean
        get() = p.getBoolean("day_light", true)
        set(v) = p.edit().putBoolean("day_light", v).apply()

    /** 0 = Vidro, 1 = Blocos */
    var theme: Int
        get() = p.getInt("theme2", THEME_BLOCKS)
        set(v) = p.edit().putInt("theme2", v).apply()

    var baseColor: Int
        get() = p.getInt("base_color", DEFAULT_BASE)
        set(v) = p.edit().putInt("base_color", v).apply()

    var lastExitSeen: Long
        get() = p.getLong("last_exit_seen", 0L)
        set(v) = p.edit().putLong("last_exit_seen", v).apply()

    /** true enquanto o mapa está sendo criado; se o app morrer nesse meio, o mapa é desligado */
    var mapOpening: Boolean
        get() = p.getBoolean("map_opening", false)
        set(v) = p.edit().putBoolean("map_opening", v).commit().let { }

    var bgMotion: Boolean
        get() = p.getBoolean("bg_motion", true)
        set(v) = p.edit().putBoolean("bg_motion", v).apply()

    var offSavedBrightness: Int
        get() = p.getInt("off_saved_b", -1)
        set(v) = p.edit().putInt("off_saved_b", v).apply()

    var offSavedMode: Int
        get() = p.getInt("off_saved_m", 0)
        set(v) = p.edit().putInt("off_saved_m", v).apply()

    var showMap: Boolean
        get() = p.getBoolean("show_map", false)
        set(v) = p.edit().putBoolean("show_map", v).apply()

    var mapReady: Boolean
        get() = p.getBoolean("map_ready_v2", false)
        set(v) = p.edit().putBoolean("map_ready_v2", v).apply()

    var showMedia: Boolean
        get() = p.getBoolean("show_media", true)
        set(v) = p.edit().putBoolean("show_media", v).apply()

    var dimScreen: Boolean
        get() = p.getBoolean("dim_screen", false)
        set(v) = p.edit().putBoolean("dim_screen", v).apply()

    var savedBrightness: Int
        get() = p.getInt("saved_brightness", -1)
        set(v) = p.edit().putInt("saved_brightness", v).apply()

    var savedBrightnessMode: Int
        get() = p.getInt("saved_brightness_mode", 0)
        set(v) = p.edit().putInt("saved_brightness_mode", v).apply()

    /** 0 = desligado */
    var alertKmh: Int
        get() = p.getInt("alert_kmh", 110)
        set(v) = p.edit().putInt("alert_kmh", v).apply()

    val wTemp: Float get() = p.getFloat("w_temp", 0f)
    val wCode: Int get() = p.getInt("w_code", 0)
    val wIsDay: Boolean get() = p.getBoolean("w_day", true)
    val wTs: Long get() = p.getLong("w_ts", 0L)

    fun saveWeather(w: Weather, ts: Long) {
        p.edit()
            .putFloat("w_temp", w.temp)
            .putInt("w_code", w.code)
            .putBoolean("w_day", w.isDay)
            .putLong("w_ts", ts)
            .apply()
    }

    companion object {
        val DEFAULT_LABELS = listOf("CarPlay", "Áudio BT", "Rádio")
        const val DEFAULT_ORDER = "clock,speed,media"
    }
}
