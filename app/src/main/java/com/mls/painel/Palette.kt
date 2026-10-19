package com.mls.painel

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min

/** 0 = Vidro, 1 = Blocos */
const val THEME_GLASS = 0
const val THEME_BLOCKS = 1

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
    val round: Int,         // arredondamento extra do tema
    val tiles: List<Color>, // fundo dos 3 atalhos
    val tileFg: Color,
    val iconBg: Color,
    val iconFg: Color,
    val clockBg: Color,
    val clockFg: Color,
    val clockMut: Color,
    val speedBg: Color,
    val speedFg: Color,
    val miniBg: Color,
    val miniFg: Color,
    val miniIcon: Color,
    val map: MapPal,
    val night: Boolean,
    val light: Boolean
)

private fun c(hex: Long) = Color(hex)
private fun onFor(c: Color): Color =
    if (1.05f / (c.luminance() + 0.05f) >= 3f) Color.White else Color(0xFF141414)

// ---------------------------------------------------------------- paletas de referência (base azul)

private fun glassDay() = Pal(
    bg = c(0xFFD6E2F2), bg2 = c(0xFFC9D8EC),
    cd = c(0xD9FFFFFF), cd2 = c(0xFFE3EAF4), bd = c(0xFFB4C4DB),
    ac = c(0xFF1D4ED8), on = Color.White, tx = c(0xFF0B1220), tx2 = c(0xFF33415A),
    red = c(0xFFB3261E), gps = c(0xFF0F6E56), round = 8,
    tiles = List(3) { c(0xD9FFFFFF) }, tileFg = c(0xFF0B1220), iconBg = c(0xFF1D4ED8), iconFg = Color.White,
    clockBg = c(0xD9FFFFFF), clockFg = c(0xFF0B1220), clockMut = c(0xFF33415A),
    speedBg = c(0xD9FFFFFF), speedFg = c(0xFF0B1220),
    miniBg = c(0xD9FFFFFF), miniFg = c(0xFF33415A), miniIcon = c(0xFF1D4ED8),
    map = MapPal(c(0xFFEEF2F7), c(0xFFB9D0EA), c(0xFFFFFFFF), c(0xFFD2DBE6), c(0xFFFFFFFF), c(0xFF9FB2CC), c(0xFFDFE8E2), c(0xFF5A6B85)),
    night = false, light = true
)

private fun glassNight() = Pal(
    bg = c(0xFF0F1D40), bg2 = c(0xFF0A1430),
    cd = c(0x1DFFFFFF), cd2 = c(0x24FFFFFF), bd = c(0x42FFFFFF),
    ac = c(0xFF9DB8F5), on = c(0xFF0F1D40), tx = Color.White, tx2 = c(0xC2FFFFFF),
    red = c(0xFFE5534B), gps = c(0xFF4FB896), round = 8,
    tiles = List(3) { c(0x1DFFFFFF) }, tileFg = Color.White, iconBg = c(0xFF9DB8F5), iconFg = c(0xFF0F1D40),
    clockBg = c(0x1DFFFFFF), clockFg = Color.White, clockMut = c(0xC2FFFFFF),
    speedBg = c(0x1DFFFFFF), speedFg = Color.White,
    miniBg = c(0x1DFFFFFF), miniFg = c(0xC2FFFFFF), miniIcon = c(0xFF9DB8F5),
    map = MapPal(c(0xFF111D3B), c(0xFF0A1430), c(0xFF1F2F55), c(0xFF172446), c(0xFF34508A), c(0xFF253A66), c(0xFF13253B), c(0xFF8EA3C8)),
    night = true, light = false
)

private fun blocksDay() = Pal(
    bg = c(0xFFE6EDF6), bg2 = c(0xFFE6EDF6),
    cd = Color.White, cd2 = c(0xFFE8EEF7), bd = c(0xFFCBD7E8),
    ac = c(0xFF1D4ED8), on = Color.White, tx = c(0xFF0B1630), tx2 = c(0xFF45557A),
    red = c(0xFFB3261E), gps = c(0xFF0F6E56), round = 12,
    tiles = listOf(c(0xFF1E3A8A), c(0xFF1D4ED8), c(0xFF3B82F6)), tileFg = Color.White,
    iconBg = c(0x33FFFFFF), iconFg = Color.White,
    clockBg = c(0xFF0F1E3D), clockFg = Color.White, clockMut = c(0xFFA9BBDC),
    speedBg = c(0xFFDBEAFE), speedFg = c(0xFF0B1630),
    miniBg = c(0xFF0F1E3D), miniFg = Color.White, miniIcon = c(0xFF93C5FD),
    map = MapPal(c(0xFFEEF3FA), c(0xFFA9CDF2), c(0xFFFFFFFF), c(0xFFC6D5EA), c(0xFF93C5FD), c(0xFF3B82F6), c(0xFFDBE7F3), c(0xFF45557A)),
    night = false, light = true
)

private fun blocksNight() = Pal(
    bg = c(0xFF050A14), bg2 = c(0xFF050A14),
    cd = c(0xFF0F1A2E), cd2 = c(0xFF1A2A48), bd = c(0xFF1F2E4D),
    ac = c(0xFF93C5FD), on = c(0xFF0B1630), tx = c(0xFFEAF1FB), tx2 = c(0xFFA3B4D1),
    red = c(0xFFE5534B), gps = c(0xFF4FB896), round = 12,
    tiles = listOf(c(0xFF1E3A8A), c(0xFF1D4ED8), c(0xFF2563EB)), tileFg = Color.White,
    iconBg = c(0x33FFFFFF), iconFg = Color.White,
    clockBg = c(0xFF0F1A2E), clockFg = c(0xFFEAF1FB), clockMut = c(0xFF93A6C8),
    speedBg = c(0xFF10234A), speedFg = c(0xFFCFE1FF),
    miniBg = c(0xFF14213A), miniFg = c(0xFFE3ECF9), miniIcon = c(0xFF93C5FD),
    map = MapPal(c(0xFF0B1426), c(0xFF0C2347), c(0xFF1B2A47), c(0xFF121E35), c(0xFF2563EB), c(0xFF1E3A8A), c(0xFF0F1C2E), c(0xFF93A6C8)),
    night = true, light = false
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

fun palette(theme: Int, base: Int, night: Boolean): Pal {
    val ref = when {
        theme == THEME_GLASS && night -> glassNight()
        theme == THEME_GLASS -> glassDay()
        night -> blocksNight()
        else -> blocksDay()
    }
    val s = Shift(base)
    val ac = s.of(ref.ac)
    val iconBg = s.of(ref.iconBg)
    return ref.copy(
        bg = s.of(ref.bg), bg2 = s.of(ref.bg2),
        cd = s.of(ref.cd), cd2 = s.of(ref.cd2), bd = s.of(ref.bd),
        ac = ac, on = if (ref.on == Color.White || ref.on.luminance() < 0.1f) onFor(ac) else ref.on,
        tx = s.of(ref.tx), tx2 = s.of(ref.tx2),
        tiles = ref.tiles.map { s.of(it) }, tileFg = ref.tileFg,
        iconBg = iconBg, iconFg = if (iconBg.alpha < 0.5f) ref.iconFg else onFor(iconBg),
        clockBg = s.of(ref.clockBg), clockFg = s.of(ref.clockFg), clockMut = s.of(ref.clockMut),
        speedBg = s.of(ref.speedBg), speedFg = s.of(ref.speedFg),
        miniBg = s.of(ref.miniBg), miniFg = s.of(ref.miniFg), miniIcon = s.of(ref.miniIcon),
        map = MapPal(
            s.of(ref.map.land), s.of(ref.map.water), s.of(ref.map.road), s.of(ref.map.casing),
            s.of(ref.map.main), s.of(ref.map.mainCasing), s.of(ref.map.park), s.of(ref.map.label)
        )
    )
}

fun Color.argb(): Int = toArgb()

val LocalPal = staticCompositionLocalOf { palette(THEME_BLOCKS, DEFAULT_BASE, false) }
