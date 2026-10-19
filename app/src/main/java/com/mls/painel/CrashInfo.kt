package com.mls.painel

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import java.io.File

/**
 * Junta o motivo do último fechamento: erro em Kotlin (arquivo gravado pelo PainelApp)
 * ou erro nativo do mapa (motivo do sistema + log do próprio app).
 */
object CrashInfo {
    fun collect(ctx: Context, prefs: Prefs): String? {
        val file = File(ctx.filesDir, PainelApp.CRASH_FILE)
        var text = try {
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            null
        }
        var crashed = text != null
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                val am = ctx.getSystemService(ActivityManager::class.java)
                val info = am.getHistoricalProcessExitReasons(ctx.packageName, 0, 1).firstOrNull()
                if (info != null && info.timestamp > prefs.lastExitSeen) {
                    prefs.lastExitSeen = info.timestamp
                    val reason = when (info.reason) {
                        ApplicationExitInfo.REASON_CRASH -> "erro (Java/Kotlin)"
                        ApplicationExitInfo.REASON_CRASH_NATIVE -> "erro nativo"
                        ApplicationExitInfo.REASON_ANR -> "travou (ANR)"
                        else -> null
                    }
                    if (reason != null) {
                        crashed = true
                        if (text == null) text = "Motivo: $reason\n${info.description ?: ""}\n"
                    }
                }
            } catch (e: Exception) {
            }
        }
        if (prefs.mapOpening) {
            crashed = true
            text = (text ?: "") + "\nO app fechou enquanto o mapa abria.\n"
        }
        if (!crashed) return null
        return (text ?: "") + "\n--- log ---\n" + logcat()
    }

    private fun logcat(): String = try {
        val p = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "400", "*:W"))
        val out = p.inputStream.bufferedReader().use { it.readText() }
        p.destroy()
        out.lines()
            .filter { l -> listOf("mbgl", "maplibre", "fatal", "painel", "androidruntime", "libc", "debug", "gl", "egl").any { l.lowercase().contains(it) } }
            .takeLast(80)
            .joinToString("\n")
            .takeLast(7000)
    } catch (e: Exception) {
        "(sem acesso ao log: ${e.message})"
    }
}
