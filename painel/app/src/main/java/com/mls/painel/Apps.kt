package com.mls.painel

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.drawable.toBitmap

data class AppInfo(val pkg: String, val label: String, val icon: ImageBitmap)

fun loadApps(ctx: Context): List<AppInfo> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != ctx.packageName }
        .mapNotNull {
            try {
                AppInfo(
                    pkg = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString(),
                    icon = it.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
                )
            } catch (e: Exception) {
                null
            }
        }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}

fun launch(ctx: Context, pkg: String): Boolean {
    if (pkg.isEmpty()) return false
    val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        ctx.startActivity(i)
        true
    } catch (e: Exception) {
        false
    }
}

fun guess(apps: List<AppInfo>, keys: List<String>): AppInfo? =
    apps.firstOrNull { a ->
        val s = (a.label + " " + a.pkg).lowercase()
        keys.any { s.contains(it) }
    }

private val BT_WORD = Regex("\\bbt\\b")

/** Ícone desenhado para atalhos conhecidos; null = usa o ícone do próprio app. */
fun glyphFor(label: String, pkg: String): ImageVector? {
    val s = "$label $pkg".lowercase()
    return when {
        "carplay" in s || "zlink" in s || "android auto" in s || "autokit" in s -> Icons.Rounded.PhoneIphone
        "bluetooth" in s || BT_WORD.containsMatchIn(s) -> Icons.Rounded.Bluetooth
        "rádio" in s || "radio" in s -> Icons.Rounded.Radio
        "equaliz" in s -> Icons.Rounded.Tune
        "usb" in s || "música" in s || "music" in s -> Icons.Rounded.LibraryMusic
        "câmera" in s || "camera" in s -> Icons.Rounded.PhotoCamera
        "telefone" in s || "dialer" in s || "phone" in s -> Icons.Rounded.Call
        "igo" in s || "navega" in s || "gps" in s -> Icons.Rounded.Navigation
        else -> null
    }
}
