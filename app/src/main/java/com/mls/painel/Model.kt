package com.mls.painel

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

sealed class Overlay {
    /** target 0..2 = atalhos, 3 = equalizador */
    data class Picker(val target: Int) : Overlay()
    object Config : Overlay()
    object Apps : Overlay()
}

class LauncherModel(val ctx: Context) {
    val prefs = Prefs(ctx)
    val gps = GpsRepo(ctx)
    val media = MediaRepo(ctx)

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set
    val slots = mutableStateListOf(prefs.slot(0), prefs.slot(1), prefs.slot(2))
    var eqPkg by mutableStateOf(prefs.eqPkg)
        private set

    var left by mutableStateOf(prefs.leftSide)
        private set
    var order by mutableStateOf(prefs.order)
        private set
    var accent by mutableStateOf(prefs.accent)
        private set
    var base by mutableStateOf(prefs.base)
        private set
    var nightMode by mutableStateOf(prefs.nightMode)
        private set
    var showMedia by mutableStateOf(prefs.showMedia)
        private set
    var dayLight by mutableStateOf(prefs.dayLight)
        private set
    var dimScreen by mutableStateOf(prefs.dimScreen)
        private set
    var alertKmh by mutableStateOf(prefs.alertKmh)
        private set

    var edit by mutableStateOf(false)
    var sel by mutableStateOf<Int?>(null)
    var overlay by mutableStateOf<Overlay?>(null)
    var toast by mutableStateOf<String?>(null)
    var toastId by mutableLongStateOf(0L)
    var now by mutableLongStateOf(System.currentTimeMillis())
    var online by mutableStateOf(false)
    var writeOk by mutableStateOf(false)

    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    var vol by mutableStateOf(0)
        private set
    var volMax by mutableStateOf(1)
        private set
    var muted by mutableStateOf(false)
        private set

    var wTemp by mutableStateOf(prefs.wTemp)
        private set
    var wCode by mutableStateOf(prefs.wCode)
        private set
    var wIsDay by mutableStateOf(prefs.wIsDay)
        private set
    var wTs by mutableLongStateOf(prefs.wTs)
        private set

    suspend fun reloadApps() {
        val list = withContext(Dispatchers.Default) { loadApps(ctx) }
        apps = list
        if (!prefs.initialized && list.isNotEmpty()) {
            autoAssign(list)
            prefs.initialized = true
        }
    }

    /** Primeira execução: tenta achar ZLink, áudio BT, rádio e EQ pelo nome. */
    private fun autoAssign(list: List<AppInfo>) {
        val keys = listOf(
            listOf("zlink", "carplay", "autokit"),
            listOf("bluetooth", "btmusic", "bt music", "a2dp"),
            listOf("rádio", "radio")
        )
        keys.forEachIndexed { i, k ->
            guess(list, k)?.let { setSlot(i, Slot(it.pkg, Prefs.DEFAULT_LABELS[i])) }
        }
        guess(list, listOf("equaliz", "equalis", "sound effect", "dsp"))?.let { setEq(it.pkg) }
    }

    fun show(msg: String) {
        toast = msg
        toastId = toastId + 1
    }

    fun setSlot(i: Int, s: Slot) {
        slots[i] = s
        prefs.setSlot(i, s)
    }

    fun setEq(pkg: String) {
        eqPkg = pkg
        prefs.eqPkg = pkg
    }

    fun updLeft(v: Boolean) { left = v; prefs.leftSide = v }
    fun updOrder(v: List<String>) { order = v; prefs.order = v }
    fun updAccent(v: Int) { accent = v; prefs.accent = v }
    fun updBase(v: Int) { base = v; prefs.base = v }
    fun updNight(v: Int) { nightMode = v; prefs.nightMode = v }
    fun updMedia(v: Boolean) { showMedia = v; prefs.showMedia = v }
    fun updDayLight(v: Boolean) { dayLight = v; prefs.dayLight = v }
    fun updAlert(v: Int) { alertKmh = v; prefs.alertKmh = v }

    fun updDim(v: Boolean) {
        dimScreen = v
        prefs.dimScreen = v
        if (!v) restoreBrightness()
    }

    fun tapSlot(i: Int) {
        val s = slots[i]
        if (!launch(ctx, s.pkg)) {
            if (s.pkg.isNotEmpty()) show("${s.label} não encontrado, escolha o app")
            overlay = Overlay.Picker(i)
        }
    }

    fun tapEq() {
        if (!launch(ctx, eqPkg)) overlay = Overlay.Picker(3)
    }

    fun pick(slot: Int) {
        val s = sel
        when {
            s == null -> sel = slot
            s == slot -> sel = null
            else -> {
                val o = order.toMutableList()
                val x = o[s]
                o[s] = o[slot]
                o[slot] = x
                updOrder(o)
                sel = null
                show("Blocos trocados")
            }
        }
    }

    fun finishEdit() {
        edit = false
        sel = null
        show("Layout salvo")
    }

    fun closeAll() {
        overlay = null
        if (edit) {
            edit = false
            sel = null
        }
    }

    fun isNight(): Boolean {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return nightMode == 2 || (nightMode == 1 && (h >= 18 || h < 6))
    }

    fun readVolume() {
        try {
            vol = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            volMax = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            muted = audio.isStreamMute(AudioManager.STREAM_MUSIC) || vol == 0
        } catch (e: Exception) {
        }
    }

    /** dir: +1 aumenta, -1 diminui. Sem permissão especial. */
    fun volume(dir: Int) {
        try {
            if (muted && dir > 0) audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (dir > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                0
            )
        } catch (e: Exception) {
        }
        readVolume()
    }

    fun toggleMute() {
        try {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0)
        } catch (e: Exception) {
        }
        readVolume()
        show(if (muted) "Som mudo" else "Som ligado")
    }

    fun tickSystem() {
        now = System.currentTimeMillis()
        readVolume()
        writeOk = Settings.System.canWrite(ctx)
        online = try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            false
        }
    }

    /** Retorna true se atualizou. Sem internet ou sem posição, mantém o último clima salvo. */
    suspend fun refreshWeather(): Boolean {
        if (!online) return false
        val loc = gps.lastLoc ?: gps.lastKnown() ?: return false
        val w = fetchWeather(loc.latitude, loc.longitude) ?: return false
        val ts = System.currentTimeMillis()
        wTemp = w.temp
        wCode = w.code
        wIsDay = w.isDay
        wTs = ts
        prefs.saveWeather(w, ts)
        return true
    }

    fun applyBrightness(night: Boolean) {
        if (!dimScreen || !Settings.System.canWrite(ctx)) return
        val cr = ctx.contentResolver
        try {
            if (night) {
                if (prefs.savedBrightness < 0) {
                    prefs.savedBrightness = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 200)
                    prefs.savedBrightnessMode = Settings.System.getInt(
                        cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    )
                }
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, NIGHT_BRIGHTNESS)
            } else {
                restoreBrightness()
            }
        } catch (e: Exception) {
        }
    }

    private fun restoreBrightness() {
        if (prefs.savedBrightness < 0 || !Settings.System.canWrite(ctx)) return
        val cr = ctx.contentResolver
        try {
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, prefs.savedBrightness)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, prefs.savedBrightnessMode)
        } catch (e: Exception) {
        }
        prefs.savedBrightness = -1
    }

    fun openWriteSettings() =
        startSafe(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.packageName)))

    fun openNotifAccess() = startSafe(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    fun openAndroidSettings() = startSafe(Intent(Settings.ACTION_SETTINGS))

    private fun startSafe(i: Intent) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        } catch (e: Exception) {
            show("Essa tela não existe nesta central")
        }
    }

    companion object {
        const val NIGHT_BRIGHTNESS = 45
    }
}
