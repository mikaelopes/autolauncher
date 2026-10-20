package com.mls.painel

import android.content.Context
import android.content.pm.PackageManager
import java.util.zip.ZipFile

/**
 * Lê os apps de sistema do fabricante (xyauto) procurando como eles recebem comandos de volume:
 * componentes exportados e textos internos (ações de intent, nomes de métodos) ligados a volume.
 * Só leitura; nada é alterado.
 */
object VendorScan {
    private val PKGS = listOf("com.xyauto.services", "com.xyauto.Settings", "com.xyauto.dspSetting", "com.android.systemui")
    private val HIT = Regex("(?i)(volume|vol_|_vol|mute|amp|mcu|audio_?ctrl|sound)")
    private val ACTIONISH = Regex("^[A-Za-z0-9_.:/\\-]{6,120}$")

    fun run(ctx: Context): String {
        val pm = ctx.packageManager
        val sb = StringBuilder()
        for (pkg in PKGS) {
            val info = try {
                pm.getPackageInfo(
                    pkg,
                    PackageManager.GET_RECEIVERS or PackageManager.GET_SERVICES or PackageManager.GET_PROVIDERS or PackageManager.GET_ACTIVITIES
                )
            } catch (e: Exception) {
                continue
            }
            sb.append("\n== ").append(pkg).append(" ==\n")
            info.receivers?.forEach { sb.append("R ").append(if (it.exported) "+ " else "- ").append(it.name).append('\n') }
            info.services?.forEach { sb.append("S ").append(if (it.exported) "+ " else "- ").append(it.name).append('\n') }
            info.providers?.forEach { sb.append("P ").append(if (it.exported) "+ " else "- ").append(it.authority).append('\n') }
            info.activities?.filter { HIT.containsMatchIn(it.name) }
                ?.forEach { sb.append("A ").append(if (it.exported) "+ " else "- ").append(it.name).append('\n') }
            val apk = info.applicationInfo?.sourceDir ?: continue
            val found = sortedSetOf<String>()
            try {
                ZipFile(apk).use { z ->
                    for (e in z.entries()) {
                        val n = e.name
                        if (n != "AndroidManifest.xml" && !(n.startsWith("classes") && n.endsWith(".dex"))) continue
                        if (e.size > 30_000_000) continue
                        val bytes = z.getInputStream(e).use { it.readBytes() }
                        strings(bytes).filter { HIT.containsMatchIn(it) && ACTIONISH.matches(it) }.forEach { found.add(it) }
                        if (found.size > 400) break
                    }
                }
            } catch (e: Exception) {
                sb.append("(apk não legível: ").append(e.javaClass.simpleName).append(")\n")
            }
            found.take(160).forEach { sb.append("· ").append(it).append('\n') }
        }
        return sb.toString()
    }

    /** Sequências de texto em ASCII (dex) e UTF-16 (manifesto binário). */
    private fun strings(b: ByteArray): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        for (x in b) {
            val c = x.toInt() and 0xFF
            if (c in 0x20..0x7E) cur.append(c.toChar()) else {
                if (cur.length >= 6) out.add(cur.toString())
                cur.setLength(0)
            }
        }
        var i = 0
        cur.setLength(0)
        while (i + 1 < b.size) {
            val lo = b[i].toInt() and 0xFF
            val hi = b[i + 1].toInt() and 0xFF
            if (hi == 0 && lo in 0x20..0x7E) cur.append(lo.toChar()) else {
                if (cur.length >= 6) out.add(cur.toString())
                cur.setLength(0)
            }
            i += 2
        }
        return out
    }
}
