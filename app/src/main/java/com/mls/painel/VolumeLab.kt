package com.mls.painel

import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Experimentos para achar o caminho do volume da xyauto (MCU). Cada teste informa antes/depois. */
object VolumeLab {
    private val KEYS = listOf("McuMainVolume", "audio_vol", "audio_vol_mute", "xy_default_music_vol", "volume_music", "volume_music_speaker")

    fun read(ctx: Context): String = KEYS.joinToString("  ") { k ->
        val v = try { Settings.System.getString(ctx.contentResolver, k) } catch (e: Exception) { "erro" }
        "$k=${v ?: "—"}"
    }

    fun mcuVolume(ctx: Context): Int? = try {
        Settings.System.getString(ctx.contentResolver, "McuMainVolume")?.toIntOrNull()
    } catch (e: Exception) { null }

    /** Todas as chaves de sistema que parecem ligadas a volume/xyauto. */
    fun dumpSettings(ctx: Context): String {
        val sb = StringBuilder()
        for (table in listOf("system", "global", "secure")) {
            try {
                ctx.contentResolver.query(
                    android.net.Uri.parse("content://settings/$table"), arrayOf("name", "value"), null, null, null
                )?.use { c ->
                    while (c.moveToNext()) {
                        val n = c.getString(0) ?: continue
                        if (Regex("(?i)vol|mute|amp|mcu|xy|sound|audio").containsMatchIn(n)) {
                            sb.append(table).append('.').append(n).append(" = ").append(c.getString(1)).append('\n')
                        }
                    }
                }
            } catch (e: Exception) {
                sb.append("($table: ").append(e.javaClass.simpleName).append(")\n")
            }
        }
        return sb.toString()
    }

    data class Test(val name: String, val run: (Context, Int) -> String)

    val tests = listOf(
        Test("1 · Broadcast xy.android.mute") { c, _ ->
            c.sendBroadcast(Intent("xy.android.mute")); "enviado"
        },
        Test("2 · xy.android.mute → serviço xyauto") { c, _ ->
            c.sendBroadcast(Intent("xy.android.mute").setPackage("com.xyauto.services")); "enviado"
        },
        Test("3 · Gravar McuMainVolume +1") { c, v ->
            try {
                Settings.System.putInt(c.contentResolver, "McuMainVolume", v + 1); "gravado ${v + 1}"
            } catch (e: Exception) { "recusado: ${e.message?.take(80)}" }
        },
        Test("4 · Gravar McuMainVolume −1") { c, v ->
            try {
                Settings.System.putInt(c.contentResolver, "McuMainVolume", (v - 1).coerceAtLeast(0)); "gravado ${v - 1}"
            } catch (e: Exception) { "recusado: ${e.message?.take(80)}" }
        },
        Test("5 · Broadcast main.volume.changed +1") { c, v ->
            val i = Intent("xy.android.main.volume.changed")
                .putExtra("volume", v + 1).putExtra("value", v + 1).putExtra("vol", v + 1)
            c.sendBroadcast(i); "enviado (${v + 1})"
        },
        Test("6 · Broadcast action.ampctrl.set") { c, v ->
            c.sendBroadcast(Intent("action.ampctrl.set").putExtra("volume", v + 1)); "enviado"
        }
        // Teste de tecla pelo MCU removido: código errado reinicia a central
    )

    /** Salva os apps da xyauto (e a barra de sistema) na pasta Download para análise. */
    fun exportApks(ctx: Context): String {
        val pkgs = listOf("com.xyauto.Settings", "com.xyauto.services", "com.xyauto.dspSetting", "com.android.systemui")
        val sb = StringBuilder()
        for (p in pkgs) {
            try {
                val src = java.io.File(ctx.packageManager.getApplicationInfo(p, 0).sourceDir)
                val dir = (ctx.getExternalFilesDir(null) ?: ctx.filesDir)
                val dst = java.io.File(dir, "painel_$p.apk")
                src.inputStream().use { inp -> dst.outputStream().use { inp.copyTo(it) } }
                sb.append("✓ ").append(p).append(" (").append(src.length() / 1024).append(" KB)\n")
            } catch (e: Exception) {
                sb.append("✗ ").append(p).append(": ").append(e.message?.take(60)).append('\n')
            }
        }
        sb.append("\nSalvos em Android/data/com.mls.painel/files")
        return sb.toString()
    }
}
