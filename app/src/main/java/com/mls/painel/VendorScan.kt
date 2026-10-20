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
    private val PKGS = listOf("com.xyauto.services", "com.xyauto.Settings", "com.xyauto.dspSetting")
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
                        val isManifest = n == "AndroidManifest.xml"
                        if (!isManifest && !(n.startsWith("classes") && n.endsWith(".dex"))) continue
                        // Lê em pedaços e guarda só o que interessa: memória baixa mesmo em apk grande
                        z.getInputStream(e).buffered(64 * 1024).use { input ->
                            scan(input, isManifest) { str ->
                                if (found.size < 400 && HIT.containsMatchIn(str) && ACTIONISH.matches(str)) found.add(str)
                            }
                        }
                        if (found.size >= 400) break
                    }
                }
            } catch (e: Throwable) {
                sb.append("(apk não legível: ").append(e.javaClass.simpleName).append(")\n")
            }
            found.take(160).forEach { sb.append("· ").append(it).append('\n') }
        }
        return sb.toString()
    }

    /** Sequências de texto em ASCII (dex) ou UTF-16 (manifesto binário), lidas em fluxo. */
    private fun scan(input: java.io.InputStream, utf16: Boolean, onString: (String) -> Unit) {
        val cur = StringBuilder()
        val buf = ByteArray(64 * 1024)
        var odd = -1
        while (true) {
            val n = input.read(buf)
            if (n <= 0) break
            var i = 0
            while (i < n) {
                if (utf16) {
                    if (odd < 0) { odd = buf[i].toInt() and 0xFF; i++; continue }
                    val lo = odd
                    val hi = buf[i].toInt() and 0xFF
                    odd = -1
                    if (hi == 0 && lo in 0x20..0x7E) cur.append(lo.toChar()) else flush(cur, onString)
                } else {
                    val c = buf[i].toInt() and 0xFF
                    if (c in 0x20..0x7E) cur.append(c.toChar()) else flush(cur, onString)
                }
                if (cur.length > 200) cur.setLength(0)
                i++
            }
        }
        flush(cur, onString)
    }

    private fun flush(cur: StringBuilder, onString: (String) -> Unit) {
        if (cur.length >= 6) onString(cur.toString())
        cur.setLength(0)
    }
}
