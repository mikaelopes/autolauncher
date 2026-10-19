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

/** Tons vivos da mesma paleta para o tema claro: saturados, mas escuros o bastante para ler no sol. */
val ACCENTS_LIGHT = listOf(
    Color(0xFF1A73E8), // azul
    Color(0xFF6A4FE0), // roxo
    Color(0xFF0E9F6E), // verde
    Color(0xFFE07B00), // âmbar
    Color(0xFFD6336C), // rosa
    Color(0xFF5B6472)  // neutro
)

data class Base(val name: String, val bg: Color, val cd: Color, val cd2: Color)

val BASES = listOf(
    Base("Grafite", Color(0xFF0E1014), Color(0xFF22262E), Color(0xFF343944)),
    Base("Azul noite", Color(0xFF0A101C), Color(0xFF18233A), Color(0xFF2A3A57)),
    Base("Preto", Color(0xFF000000), Color(0xFF17191C), Color(0xFF2C3036))
)

data class Pal(
    val bg: Color,
    val cd: Color,
    val cd2: Color,
    val bd: Color,
    val ac: Color,
    val on: Color,
    val tx: Color,
    val tx2: Color,
    val red: Color,
    val gps: Color,
    val night: Boolean,
    val light: Boolean
)

private fun onFor(c: Color): Color =
    if (1.05f / (c.luminance() + 0.05f) >= 3f) Color.White else Color(0xFF141414)

/**
 * night  = modo noturno ativo (tudo escuro e apagado)
 * light  = tema claro de dia (alto contraste para sol forte)
 */
fun palette(accent: Int, base: Int, night: Boolean, light: Boolean): Pal {
    val ai = accent.coerceIn(0, ACCENTS.size - 1)
    val b = BASES[base.coerceIn(0, BASES.size - 1)]
    return when {
        night -> {
            val ac = lerp(ACCENTS[ai], Color(0xFF3A3A3A), 0.35f)
            Pal(
                bg = lerp(b.bg, Color.Black, 0.6f),
                cd = lerp(b.cd, Color.Black, 0.5f),
                cd2 = lerp(b.cd2, Color.Black, 0.45f),
                bd = lerp(b.cd2, Color.Black, 0.3f),
                ac = ac,
                on = onFor(ac),
                tx = Color(0xFFB9BEC6),
                tx2 = Color(0xFF80868F),
                red = Color(0xFFC94040),
                gps = Color(0xFF4A9C80),
                night = true,
                light = false
            )
        }
        light -> {
            val ac = ACCENTS_LIGHT[ai]
            Pal(
                bg = Color(0xFFE4E8ED),
                cd = Color(0xFFFFFFFF),
                cd2 = Color(0xFFDDE2E8),
                bd = Color(0xFF9AA3AF),
                ac = ac,
                on = onFor(ac),
                tx = Color(0xFF0B0D10),
                tx2 = Color(0xFF3E444D),
                red = Color(0xFFB3261E),
                gps = Color(0xFF0F6E56),
                night = false,
                light = true
            )
        }
        else -> {
            val ac = ACCENTS[ai]
            Pal(
                bg = b.bg,
                cd = b.cd,
                cd2 = b.cd2,
                bd = Color(0xFF59606C),
                ac = ac,
                on = onFor(ac),
                tx = Color(0xFFFFFFFF),
                tx2 = Color(0xFFC3C8D0),
                red = Color(0xFFFF5A55),
                gps = Color(0xFF5DCAA5),
                night = false,
                light = false
            )
        }
    }
}

val LocalPal = staticCompositionLocalOf { palette(0, 0, night = false, light = true) }
