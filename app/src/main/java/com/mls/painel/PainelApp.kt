package com.mls.painel

import android.app.Application
import java.io.File

/** Guarda o motivo de qualquer fechamento inesperado para mostrar na próxima abertura. */
class PainelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val text = buildString {
                    append("Painel ").append(BuildConfigInfo.version(this@PainelApp)).append('\n')
                    append("Android ").append(android.os.Build.VERSION.RELEASE)
                        .append(" · ").append(android.os.Build.MODEL).append('\n')
                    append("Thread: ").append(t.name).append("\n\n")
                    append(e.stackTraceToString().take(6000))
                }
                File(filesDir, CRASH_FILE).writeText(text)
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(t, e)
        }
    }

    companion object {
        const val CRASH_FILE = "ultimo_erro.txt"
    }
}

object BuildConfigInfo {
    fun version(app: Application): String = try {
        app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "?"
    } catch (_: Exception) {
        "?"
    }
}


object BuildConfigInfoCtx {
    fun version(ctx: android.content.Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
    } catch (_: Exception) {
        "?"
    }
}
