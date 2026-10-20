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
    object Colors : Overlay()
    object VolumeLab : Overlay()
}

class LauncherModel(val ctx: Context) {
    val prefs = Prefs(ctx)
    val gps = GpsRepo(ctx)
    val media = MediaRepo(ctx)
    val map = MapRepo(ctx, prefs)

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
    var theme by mutableStateOf(prefs.theme)
        private set
    var baseColor by mutableStateOf(prefs.baseColor)
        private set
    var showMap by mutableStateOf(prefs.showMap)
        private set
    var bgMotion by mutableStateOf(prefs.bgMotion)
        private set

    /** Mapa carregado e pronto para aparecer */
    var mapLive by mutableStateOf(false)

    /** Usa o layout com mapa: mapa pronto, ou ainda não baixado (mostra o convite para baixar) */
    val mapLayout: Boolean get() = showMap && (map.state != MapDl.DONE || mapLive)

    fun updBgMotion(v: Boolean) { bgMotion = v; prefs.bgMotion = v }
    var dimScreen by mutableStateOf(prefs.dimScreen)
        private set
    var alertKmh by mutableStateOf(prefs.alertKmh)
        private set

    /** Texto do último fechamento inesperado, se houver */
    var crash by mutableStateOf(CrashInfo.collect(ctx, prefs))

    init {
        // Launcher não pode ficar em loop: se fechou com o mapa ligado, desliga o mapa
        // Uma interrupção só (central desligada com a ignição, reinício) não conta:
        // o mapa só é desligado se o app morrer duas vezes seguidas abrindo o mapa.
        if (prefs.mapOpening) prefs.mapFails = prefs.mapFails + 1
        prefs.mapOpening = false
        if (prefs.mapFails >= 2 && prefs.showMap) {
            updMap(false)
            prefs.mapFails = 0
            crash = "Desliguei o mapa porque o Painel fechou duas vezes seguidas abrindo ele. " +
                "Pode religar em Ajustes.\n\n" + (crash ?: "")
        }
    }

    fun dismissCrash() {
        try {
            java.io.File(ctx.filesDir, PainelApp.CRASH_FILE).delete()
        } catch (e: Exception) {
        }
        crash = null
    }

    fun copyCrash() {
        val txt = crash ?: return
        try {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Erro do Painel", txt))
            show("Erro copiado")
        } catch (e: Exception) {
            show("Não deu para copiar")
        }
    }

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
    fun updTheme(v: Int) { theme = v; prefs.theme = v }

    /** Só atualiza a tela; salva quando soltar o controle. */
    fun previewBase(v: Int) { baseColor = v }
    fun saveBase() { prefs.baseColor = baseColor }

    /** Mapa ocupa o bloco grande; a mídia vai para o estreito. */
    fun updMap(v: Boolean) {
        showMap = v
        prefs.showMap = v
        updOrder(if (v) listOf("clock", "media", "speed") else listOf("clock", "speed", "media"))
    }

    fun downloadMap() {
        if (!online) {
            show("Conecte a central no Wi-Fi primeiro")
            return
        }
        map.download()
        show("Baixando mapa de Fortaleza…")
    }
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
        if (screenOff) setScreenOffState(false)
        if (edit) {
            edit = false
            sel = null
        }
    }

    /** Liga 15 min antes do pôr do sol (escurece rápido aqui) e desliga 15 min depois do nascer. */
    fun nightWindow(): Pair<Long, Long> {
        val loc = gps.lastLoc ?: gps.lastKnown()
        val lat = loc?.latitude ?: MapCfg.CENTER.latitude
        val lon = loc?.longitude ?: MapCfg.CENTER.longitude
        val (rise, set) = Sun.today(lat, lon, now) ?: run {
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, 6); c.set(Calendar.MINUTE, 0)
            val r = c.timeInMillis
            c.set(Calendar.HOUR_OF_DAY, 18)
            r to c.timeInMillis
        }
        return (set - 15 * 60_000L) to (rise + 15 * 60_000L)
    }

    fun isNight(): Boolean {
        if (nightMode == 2) return true
        if (nightMode == 0) return false
        val (on, off) = nightWindow()
        return now >= on || now < off
    }

    /** Carro andando: Ajustes travados (botão vira "apagar tela") */
    val moving: Boolean get() = gps.hasFix(now) && gps.speedKmh > 10f

    var screenOff by mutableStateOf(false)
        private set

    fun setScreenOffState(v: Boolean) {
        screenOff = v
        if (!Settings.System.canWrite(ctx)) return
        val cr = ctx.contentResolver
        try {
            if (v) {
                prefs.offSavedBrightness = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 120)
                prefs.offSavedMode = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, 1)
            } else if (prefs.offSavedBrightness >= 0) {
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, prefs.offSavedBrightness)
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, prefs.offSavedMode)
                prefs.offSavedBrightness = -1
            }
        } catch (e: Exception) {
        }
    }

    /** Brilho da tela 0..255 (precisa da permissão "Modificar configurações do sistema") */
    var brightness by mutableStateOf(128)
        private set

    fun readBrightness() {
        try {
            brightness = Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        } catch (e: Exception) {
        }
    }

    fun changeBrightness(v: Int) {
        val b = v.coerceIn(3, 255)
        brightness = b
        if (!Settings.System.canWrite(ctx)) return
        try {
            val cr = ctx.contentResolver
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, b)
        } catch (e: Exception) {
        }
    }

    /** Retoma o último player (tecla de mídia, sem permissão especial) */
    fun resumeMedia() {
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY))
            am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY))
        } catch (e: Exception) {
        }
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
    /**
     * Mesmo caminho do botão físico de volume: o sistema decide qual volume mexer e mostra
     * o painel de volume da central (em muitas centrais é aí que o fabricante intercepta).
     */
    fun volume(dir: Int) {
        volExec.execute { stepVolume(dir) }
    }

    /** Volume da central (o número que aparece na barra de status), ou -1 se não existir */
    private fun mcuVolume(): Int = try {
        Settings.System.getInt(ctx.contentResolver, "McuMainVolume", -1)
    } catch (e: Exception) {
        -1
    }

    private val volExec = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * Um toque = 1 ponto na central. O volume de música do Android tem mais degraus (63)
     * que a central, então ajusta e confere o McuMainVolume até ele mudar (no máx. 4 degraus).
     */
    private fun stepVolume(dir: Int) {
        try {
            val st = AudioManager.STREAM_MUSIC
            val max = audio.getStreamMaxVolume(st)
            if (savedVol >= 0 && dir > 0) {
                // estava mudo: sobe a partir do volume salvo
                audio.setStreamVolume(st, savedVol, 0)
                savedVol = -1
            }
            val before = mcuVolume()
            var idx = audio.getStreamVolume(st)
            var tries = 0
            while (tries < 4) {
                idx = (idx + dir).coerceIn(0, max)
                audio.setStreamVolume(st, idx, 0)
                tries++
                if (idx == 0 || idx == max) break
                if (before < 0) { if (tries >= 2) break; continue }
                var changed = false
                for (w in 0 until 4) {
                    Thread.sleep(40)
                    if (mcuVolume() != before) { changed = true; break }
                }
                if (changed) break
            }
            if (idx > 0) savedVol = -1
        } catch (e: Exception) {
        }
        main.post {
            readVolume()
            val mv = mcuVolume()
            show(if (muted) "Mudo" else "Volume " + (if (mv >= 0) mv else vol))
        }
    }

    /** Volume guardado ao ativar o mudo (-1 = não está mudo pelo Painel) */
    private var savedVol = -1

    /** Testes de volume: resultado de cada tentativa */
    var labLog by mutableStateOf("")

    fun exportVendorApps() {
        show("Salvando apps…")
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        Thread {
            val r = try { VolumeLab.exportApks(ctx) } catch (e: Throwable) { "Erro: $e" }
            main.post { info = "Apps da central" to r }
        }.start()
    }

    fun runVolumeTest(i: Int) {
        val t = VolumeLab.tests[i]
        val before = VolumeLab.read(ctx)
        val v = VolumeLab.mcuVolume(ctx) ?: vol
        val r = try { t.run(ctx, v) } catch (e: Exception) { "erro: ${e.message}" }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val after = VolumeLab.read(ctx)
            labLog = "${t.name}: $r\nantes:  $before\ndepois: $after\n\n" + labLog
        }, 700)
    }

    /** Janela de informação com botão de copiar (diagnósticos) */
    var info by mutableStateOf<Pair<String, String>?>(null)

    fun volumeDiagnostics() {
        show("Gerando diagnóstico…")
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        Thread {
            val txt = try {
                buildDiagnostics()
            } catch (e: Throwable) {
                "Erro ao gerar: $e"
            }
            main.post { info = "Diagnóstico de volume" to txt }
        }.start()
    }

    private fun buildDiagnostics(): String {
        val sb = StringBuilder()
        sb.append("Painel ").append(BuildConfigInfoCtx.version(ctx)).append('\n')
        sb.append("Fabricante: ").append(android.os.Build.MANUFACTURER).append(" / ").append(android.os.Build.BRAND).append('\n')
        sb.append("Modelo: ").append(android.os.Build.MODEL).append(" · placa ").append(android.os.Build.BOARD)
            .append(" · hw ").append(android.os.Build.HARDWARE).append('\n')
        sb.append("Produto: ").append(android.os.Build.PRODUCT).append(" / ").append(android.os.Build.DEVICE).append('\n')
        sb.append("Android ").append(android.os.Build.VERSION.RELEASE).append(" · ").append(android.os.Build.DISPLAY).append("\n\n")
        try {
            sb.append("Volume fixo: ").append(audio.isVolumeFixed).append('\n')
        } catch (e: Exception) {
        }
        val names = listOf("voz", "sistema", "toque", "música", "alarme", "notificação", "bt sco", "sistema forçado", "dtmf", "tts", "acessibilidade")
        for (i in 0..10) {
            try {
                sb.append("stream ").append(i).append(" (").append(names.getOrElse(i) { "?" }).append("): ")
                    .append(audio.getStreamVolume(i)).append('/').append(audio.getStreamMaxVolume(i)).append('\n')
            } catch (e: Exception) {
            }
        }
        sb.append("\nApps de sistema relacionados:\n")
        val keys = listOf("syu", "fyt", "mcu", "car", "audio", "volume", "canbus", "sound", "dsp", "eq", "yt", "ts.", "hct", "setting", "service", "radio", "bt", "launcher")
        try {
            val pkgs = ctx.packageManager.getInstalledApplications(0)
                .filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 }
                .map { it.packageName }
                .filter { p -> keys.any { p.lowercase().contains(it) } && !p.startsWith("com.google") }
                .sorted()
            pkgs.forEach { sb.append(it).append('\n') }
        } catch (e: Exception) {
            sb.append("(não foi possível listar)\n")
        }
        return sb.toString() + "\n--- configurações de volume ---\n" + VolumeLab.dumpSettings(ctx) +
            "\n--- sistema do fabricante ---" + VendorScan.run(ctx)
    }

    fun copyText(txt: String) {
        try {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Painel", txt))
            show("Copiado")
        } catch (e: Exception) {
            show("Não deu para copiar")
        }
    }

    fun toggleMute() {
        volExec.execute {
            try {
                val st = AudioManager.STREAM_MUSIC
                val cur = audio.getStreamVolume(st)
                if (cur > 0) {
                    savedVol = cur
                    audio.setStreamVolume(st, 0, 0)
                } else {
                    val back = if (savedVol > 0) savedVol else audio.getStreamMaxVolume(st) / 3
                    audio.setStreamVolume(st, back, 0)
                    savedVol = -1
                }
            } catch (e: Exception) {
            }
            main.post {
                readVolume()
                val mv = mcuVolume()
                show(if (muted) "Mudo" else "Volume " + (if (mv >= 0) mv else vol))
            }
        }
    }

    /** Nada tocando há alguns segundos: o card de mídia some e o outro bloco ocupa o espaço */
    var mediaIdle by mutableStateOf(false)
        private set
    private var lastPlaying = 0L

    fun tickSystem() {
        now = System.currentTimeMillis()
        // Sem permissão o card continua (é por ele que se libera o acesso)
        if (!media.hasAccess || media.title.isNotEmpty()) lastPlaying = now
        mediaIdle = now - lastPlaying > 4000
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
        const val NIGHT_BRIGHTNESS = 12
    }
}
