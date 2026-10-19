package com.mls.painel

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

val ACCENTS = listOf(
    Color(0xFF378ADD), // azul
    Color(0xFF7F77DD), // roxo
    Color(0xFF1D9E75), // verde
    Color(0xFFEF9F27), // âmbar
    Color(0xFFD4537E), // rosa
    Color(0xFFB4B2A9)  // neutro
)

data class Base(val name: String, val bg: Color, val cd: Color, val cd2: Color)

val BASES = listOf(
    Base("Grafite", Color(0xFF0E1014), Color(0xFF1A1D23), Color(0xFF2A2E36)),
    Base("Azul noite", Color(0xFF0A101C), Color(0xFF131C2B), Color(0xFF223049)),
    Base("Preto", Color(0xFF000000), Color(0xFF111214), Color(0xFF23262B))
)

data class Pal(
    val bg: Color,
    val cd: Color,
    val cd2: Color,
    val ac: Color,
    val on: Color,
    val tx: Color,
    val tx2: Color,
    val red: Color,
    val gps: Color,
    val night: Boolean
)

private fun onFor(c: Color): Color =
    if (1.05f / (c.luminance() + 0.05f) >= 3f) Color.White else Color(0xFF141414)

fun palette(accent: Int, base: Int, night: Boolean): Pal {
    val b = BASES[base.coerceIn(0, BASES.size - 1)]
    val a = ACCENTS[accent.coerceIn(0, ACCENTS.size - 1)]
    return if (night) {
        val ac = lerp(a, Color(0xFF3A3A3A), 0.35f)
        Pal(
            bg = lerp(b.bg, Color.Black, 0.6f),
            cd = lerp(b.cd, Color.Black, 0.45f),
            cd2 = lerp(b.cd2, Color.Black, 0.4f),
            ac = ac,
            on = onFor(ac),
            tx = Color(0xFFB9BEC6),
            tx2 = Color(0xFF80868F),
            red = Color(0xFFC94040),
            gps = Color(0xFF4A9C80),
            night = true
        )
    } else {
        Pal(
            bg = b.bg,
            cd = b.cd,
            cd2 = b.cd2,
            ac = a,
            on = onFor(a),
            tx = Color(0xFFECEEF1),
            tx2 = Color(0xFFA3A9B2),
            red = Color(0xFFE24B4A),
            gps = Color(0xFF5DCAA5),
            night = false
        )
    }
}

val LocalPal = staticCompositionLocalOf { palette(0, 0, false) }
