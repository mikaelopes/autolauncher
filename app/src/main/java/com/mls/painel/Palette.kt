package com.mls.painel

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min

/** Cor base de referência: todas as paletas abaixo foram desenhadas em cima dela. */
const val DEFAULT_BASE = 0xFF1D4ED8.toInt()

/** Cores do mapa, repintadas em tempo real sobre o estilo base. */
data class MapPal(
    val land: Color,
    val water: Color,
    val road: Color,
    val casing: Color,
    val main: Color,
    val mainCasing: Color,
    val park: Color,
    val label: Color
)

data class Pal(
    val bg: Color,          // topo do fundo
    val bg2: Color,         // base do fundo (degradê suave)
    val cd: Color,          // cards
    val cd2: Color,         // botões, trilhos
    val bd: Color,          // bordas
    val ac: Color,          // destaque
    val on: Color,          // texto sobre o destaque
    val tx: Color,
    val tx2: Color,
    val red: Color,
    val gps: Color,
    val iconBg: Color,      // quadrado do ícone nos atalhos
    val iconFg: Color,
    val map: MapPal,
    /** Depois do pôr do sol: capa do álbum e ícones do clima ficam mais apagados */
    val night: Boolean,
    // Vidro: cards com degradê, borda com reflexo e brilho no fundo
    val cdTop: Color,
    val cdBottom: Color,
    val edgeTop: Color,
    val edgeBottom: Color,
    val glow: Color,
    /** Manchas de cor que passeiam devagar atrás do vidro */
    val blobs: List<Color>,
    /** Fundo sólido dos menus e janelas (nunca transparente) */
    val sheet: Color
)

private fun c(hex: Long) = Color(hex)
private fun onFor(c: Color): Color =
    if (1.05f / (c.luminance() + 0.05f) >= 3f) Color.White else Color(0xFF141414)

// ---------------------------------------------------------------- paletas de referência (base azul)

private fun glass() = Pal(
    bg = c(0xFF0F1D40), bg2 = c(0xFF0A1430),
    cd = c(0x1DFFFFFF), cd2 = c(0x24FFFFFF), bd = c(0x42FFFFFF),
    ac = c(0xFF9DB8F5), on = c(0xFF0F1D40), tx = Color.White, tx2 = c(0xC2FFFFFF),
    red = c(0xFFE5534B), gps = c(0xFF4FB896),
    iconBg = c(0xFF9DB8F5), iconFg = c(0xFF0F1D40),
    map = MapPal(c(0xFF111D3B), c(0xFF0A1430), c(0xFF1F2F55), c(0xFF172446), c(0xFF34508A), c(0xFF253A66), c(0xFF13253B), c(0xFF8EA3C8)),
    night = false,
    cdTop = c(0x2EFFFFFF), cdBottom = c(0x0FFFFFFF), edgeTop = c(0x73FFFFFF), edgeBottom = c(0x14FFFFFF),
    glow = c(0xFF1D3A78),
    blobs = listOf(c(0xFF2350B0), c(0xFF0F5F7E), c(0xFF3A2A8C)),
    sheet = c(0xFF15244A)
)

// ---------------------------------------------------------------- troca de cor base

private fun toHsl(col: Color): FloatArray {
    val r = col.red; val g = col.green; val b = col.blue
    val mx = max(r, max(g, b)); val mn = min(r, min(g, b))
    val l = (mx + mn) / 2f
    if (mx == mn) return floatArrayOf(0f, 0f, l)
    val d = mx - mn
    val s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
    val h = when (mx) {
        r -> ((g - b) / d + (if (g < b) 6f else 0f))
        g -> ((b - r) / d + 2f)
        else -> ((r - g) / d + 4f)
    } * 60f
    return floatArrayOf(h, s, l)
}

private fun fromHsl(h: Float, s: Float, l: Float, a: Float): Color {
    val hh = ((h % 360f) + 360f) % 360f / 360f
    if (s <= 0f) return Color(l, l, l, a)
    val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
    val p = 2 * l - q
    fun hue(t0: Float): Float {
        var t = t0
        if (t < 0) t += 1f
        if (t > 1) t -= 1f
        return when {
            t < 1f / 6 -> p + (q - p) * 6 * t
            t < 1f / 2 -> q
            t < 2f / 3 -> p + (q - p) * (2f / 3 - t) * 6
            else -> p
        }
    }
    return Color(hue(hh + 1f / 3), hue(hh), hue(hh - 1f / 3), a)
}

/**
 * Leva uma cor da paleta de referência (azul) para a nova cor base:
 * gira o matiz pela mesma diferença e escala a saturação na mesma proporção.
 * A luminosidade não muda, então o contraste entre fundo, botões e textos se mantém.
 * Cores neutras (branco, cinza) ficam como estão.
 */
private class Shift(base: Int) {
    private val ref = toHsl(Color(DEFAULT_BASE))
    private val b = toHsl(Color(base))
    private val dh = b[0] - ref[0]
    private val ks = (b[1] / ref[1]).coerceIn(0f, 1.3f)
    private val identity = base == DEFAULT_BASE

    fun of(col: Color): Color {
        if (identity) return col
        val hsl = toHsl(col)
        if (hsl[1] < 0.08f) return col
        return fromHsl(hsl[0] + dh, (hsl[1] * ks).coerceIn(0f, 1f), hsl[2], col.alpha)
    }
}

/** Único tema: Vidro escuro, com o matiz levado para a cor base escolhida. */
fun palette(base: Int, night: Boolean): Pal {
    val ref = glass()
    val s = Shift(base)
    val ac = s.of(ref.ac)
    val iconBg = s.of(ref.iconBg)
    return ref.copy(
        bg = s.of(ref.bg), bg2 = s.of(ref.bg2),
        cd = s.of(ref.cd), cd2 = s.of(ref.cd2), bd = s.of(ref.bd),
        ac = ac, on = onFor(ac),
        tx = s.of(ref.tx), tx2 = s.of(ref.tx2),
        iconBg = iconBg, iconFg = onFor(iconBg),
        night = night,
        glow = s.of(ref.glow), blobs = ref.blobs.map { s.of(it) },
        sheet = s.of(ref.sheet),
        map = MapPal(
            s.of(ref.map.land), s.of(ref.map.water), s.of(ref.map.road), s.of(ref.map.casing),
            s.of(ref.map.main), s.of(ref.map.mainCasing), s.of(ref.map.park), s.of(ref.map.label)
        )
    )
}

fun Color.argb(): Int = toArgb()

val LocalPal = staticCompositionLocalOf { palette(DEFAULT_BASE, false) }
