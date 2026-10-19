package com.mls.painel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.roundToInt

/*
 * A interface é desenhada numa "prancheta" de 640x400 (metade de 1280x800) e escalada
 * para a tela real. Assim fica idêntica ao protótipo, seja qual for a densidade da central.
 */
private const val DESIGN_W = 640f
private const val DESIGN_H = 400f

enum class Variant { WIDE, NARROW, BIG }

// ---------------------------------------------------------------- raiz

@Composable
fun LauncherApp(m: LauncherModel, requestLocation: () -> Unit) {
    LaunchedEffect(Unit) {
        while (true) {
            m.tickSystem()
            delay(1000)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val ok = m.refreshWeather()
            delay(if (ok) 30 * 60_000L else 2 * 60_000L)
        }
    }

    val tick = m.now
    val night = remember(tick, m.nightMode) { m.isNight() }
    LaunchedEffect(night, m.dimScreen, m.writeOk) { m.applyBrightness(night) }

    LaunchedEffect(m.toastId) {
        if (m.toast != null) {
            delay(1800)
            m.toast = null
        }
    }
    LaunchedEffect(Unit) {
        if (!m.prefs.hintShown) {
            delay(1500)
            m.show("Dica: segure um atalho para trocar o app")
            m.prefs.hintShown = true
        }
    }

    val pal = remember(m.theme, m.baseColor, night) { palette(m.theme, m.baseColor, night) }

    // Fundo do Vidro: três manchas de cor dando uma volta a cada ~50 s (só redesenha o fundo)
    val bgAnim = rememberInfiniteTransition(label = "bg")
    val bgT = bgAnim.animateFloat(0f, 1f, infiniteRepeatable(tween(50_000, easing = LinearEasing)), label = "bgT")
    val moving = pal.glass && m.bgMotion

    // Motor do mapa: começa um pouco depois da tela aparecer e roda escondido até ficar pronto
    var engineOn by remember { mutableStateOf(false) }
    LaunchedEffect(m.showMap, m.map.state) {
        if (m.showMap && m.map.state == MapDl.DONE) {
            delay(600)
            engineOn = true
        } else {
            engineOn = false
        }
    }

    CompositionLocalProvider(LocalPal provides pal) {
      val mapCtl = if (engineOn) rememberMapEngine(m) else null
      CompositionLocalProvider(LocalMap provides mapCtl) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(pal.bg, pal.bg2)))
                .drawBehind {
                    // Vidro: brilho suave vindo de cima, sem manchas fortes atrás do texto
                    if (pal.glass && pal.blobs.size >= 3) {
                        val a = (if (moving) bgT.value else 0.15f) * 2f * PI.toFloat()
                        val w = size.width
                        val h = size.height
                        val spots = listOf(
                            Offset(w * (0.22f + 0.14f * sin(a)), h * (0.25f + 0.18f * cos(a))),
                            Offset(w * (0.78f + 0.12f * cos(a + 1.3f)), h * (0.35f + 0.22f * sin(a + 1.3f))),
                            Offset(w * (0.50f + 0.25f * sin(a + 2.6f)), h * (0.92f + 0.12f * cos(a + 2.6f)))
                        )
                        spots.forEachIndexed { i, c ->
                            drawRect(
                                Brush.radialGradient(
                                    listOf(pal.blobs[i].copy(alpha = 0.75f), Color.Transparent),
                                    center = c,
                                    radius = w * 0.5f
                                )
                            )
                        }
                    }
                    if (pal.glass) {
                        drawRect(
                            Brush.radialGradient(
                                listOf(pal.glow.copy(alpha = 0.85f), Color.Transparent),
                                center = Offset(size.width * 0.3f, 0f),
                                radius = size.width * 0.75f
                            )
                        )
                    }
                }
        ) {
            val s = minOf(constraints.maxWidth / DESIGN_W, constraints.maxHeight / DESIGN_H)
            val w = constraints.maxWidth / s
            val h = constraints.maxHeight / s
            CompositionLocalProvider(LocalDensity provides Density(s, 1f)) {
                Box(Modifier.size(w.dp, h.dp)) {
                    // Mapa carregando por trás, quase invisível (precisa ser desenhado para carregar)
                    if (mapCtl != null && !mapCtl.shown) {
                        Box(Modifier.padding(start = 196.dp, top = 104.dp).size(420.dp, 270.dp).alpha(0.01f)) {
                            MapSurface(mapCtl, Modifier.fillMaxSize())
                        }
                    }
                    Screen(m, night, requestLocation)
                    m.overlay?.let { OverlayHost(m, it) }
                    if (m.screenOff) ScreenOff(m)
                    m.crash?.let { CrashPanel(m, it) }
                    m.toast?.let {
                        ToastPill(it, Modifier.align(Alignment.TopCenter).padding(top = 36.dp))
                    }
                }
            }
        }
    }
  }
    }

@Composable
fun Screen(m: LauncherModel, night: Boolean, requestLocation: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        // A central já mostra relógio e status no topo; a barra do Painel só aparece no modo de edição
        if (m.edit) StatusBar(m, night)
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = if (m.edit) 4.dp else 12.dp)
        ) {
            val gap = 12.dp
            // 4 linhas iguais: as divisões dos atalhos coincidem com as dos blocos da direita
            val rowH = (maxHeight - gap * 3) / 4
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                if (m.left) {
                    Shortcuts(m, rowH, gap)
                    RightArea(m, rowH, gap, Modifier.weight(1f), requestLocation)
                } else {
                    RightArea(m, rowH, gap, Modifier.weight(1f), requestLocation)
                    Shortcuts(m, rowH, gap)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- barra de status

@Composable
fun StatusBar(m: LauncherModel, night: Boolean) {
    val p = LocalPal.current
    val bg = if (m.edit) lerp(p.bg, p.ac, 0.2f) else Color.Transparent
    Row(
        Modifier.fillMaxWidth().height(28.dp).background(bg).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (m.edit) {
            T("Toque em dois blocos para trocar", 12)
            Spacer(Modifier.weight(1f))
            Chip("Inverter lados", Icons.Rounded.SwapHoriz, compact = true) {
                m.updLeft(!m.left)
                m.show(if (m.left) "Atalhos à esquerda" else "Atalhos à direita")
            }
            W(6)
            Chip("Concluir", Icons.Rounded.Check, primary = true, compact = true) { m.finishEdit() }
        } else {
            if (m.media.hasAccess && m.media.title.isNotEmpty() && m.media.source.isNotEmpty()) {
                Ic(Icons.Rounded.MusicNote, 13, p.tx2)
                W(4)
                T(m.media.source, 11, p.tx2)
            }
            Spacer(Modifier.weight(1f))
            if (night) {
                Ic(Icons.Rounded.DarkMode, 13, p.tx2)
                W(4)
                T("Noturno", 11, p.tx2)
                W(12)
            }
            Ic(
                if (m.online) Icons.Rounded.Wifi else Icons.Rounded.WifiOff, 14,
                if (m.online) p.tx2 else p.tx2.copy(alpha = 0.45f)
            )
            W(12)
            val fix = m.gps.hasFix(m.now)
            val gc = if (fix) p.gps else p.tx2
            Ic(if (fix) Icons.Rounded.GpsFixed else Icons.Rounded.GpsOff, 13, gc)
            W(4)
            T(if (fix) "GPS" else "Sem GPS", 11, gc)
        }
    }
}

// ---------------------------------------------------------------- atalhos (lado do motorista)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Shortcuts(m: LauncherModel, rowH: Dp, gap: Dp) {
    val p = LocalPal.current
    val haptic = LocalHapticFeedback.current
    Column(
        Modifier.width(172.dp).fillMaxHeight().alpha(if (m.edit) 0.35f else 1f),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        for (i in 0..2) {
            val s = m.slots[i]
            val app = m.apps.firstOrNull { it.pkg == s.pkg }
            val glyph = glyphFor(s.label, s.pkg)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(rowH)
                    .panel(p, 14, p.tiles[i])
                    .combinedClickable(
                        enabled = !m.edit,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            m.overlay = Overlay.Picker(i)
                        },
                        onClick = { m.tapSlot(i) }
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape((12 + p.round / 2).dp)).background(p.iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        glyph != null -> Ic(glyph, 26, p.iconFg)
                        app != null -> Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(34.dp))
                        m.apps.isEmpty() && s.pkg.isNotEmpty() -> Skeleton(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)))
                        else -> Ic(Icons.Rounded.Add, 26, p.iconFg)
                    }
                }
                W(12)
                T(s.label, 16, p.tileFg, FontWeight.Medium)
            }
        }
        Row(Modifier.fillMaxWidth().height(rowH), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallBtn("EQ", Icons.Rounded.Tune, Modifier.weight(1f), !m.edit, onLong = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                m.overlay = Overlay.Picker(3)
            }) { m.tapEq() }
            // Andando: toque apaga a tela; segurar abre os Ajustes mesmo assim
            if (m.moving) {
                SmallBtn("Apagar", Icons.Rounded.DarkMode, Modifier.weight(1f), !m.edit, onLong = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    m.overlay = Overlay.Config
                }) { m.setScreenOffState(true) }
            } else {
                SmallBtn("Ajustes", Icons.Rounded.Settings, Modifier.weight(1f), !m.edit, onLong = {
                    m.setScreenOffState(true)
                }) { m.overlay = Overlay.Config }
            }
            SmallBtn("Apps", Icons.Rounded.Apps, Modifier.weight(1f), !m.edit) { m.overlay = Overlay.Apps }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmallBtn(
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    enabled: Boolean,
    onLong: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val p = LocalPal.current
    Column(
        modifier
            .fillMaxHeight()
            .panel(p, 14, p.miniBg)
            .combinedClickable(enabled = enabled, onLongClick = onLong, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Ic(icon, 24, p.miniIcon)
        H(4)
        T(label, 12, p.miniFg)
    }
}

// ---------------------------------------------------------------- blocos da direita

@Composable
fun RightArea(m: LauncherModel, rowH: Dp, gap: Dp, modifier: Modifier, requestLocation: () -> Unit) {
    // Mapa ligado mas ainda carregando: usa o layout normal até ele ficar pronto
    val o = (if (m.showMap && !m.mapLayout) listOf("clock", "speed", "media") else m.order).toMutableList()
    var hide = -1
    if ((!m.showMedia || m.mediaIdle) && !m.edit) {
        if (o[0] == "media") {
            o[0] = o[2]
            o[2] = "media"
        }
        hide = if (o[1] == "media") 1 else 2
    }
    Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
        Block(m, 0, o[0], Variant.WIDE, Modifier.fillMaxWidth().height(rowH), requestLocation)
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
            if (hide >= 0) {
                val k = if (hide == 1) 2 else 1
                Block(m, k, o[k], Variant.BIG, Modifier.weight(1f).fillMaxHeight(), requestLocation)
            } else {
                val narrow = Modifier.width(160.dp).fillMaxHeight()
                if (m.left) {
                    Block(m, 1, o[1], Variant.NARROW, narrow, requestLocation)
                    Block(m, 2, o[2], Variant.BIG, Modifier.weight(1f).fillMaxHeight(), requestLocation)
                } else {
                    Block(m, 2, o[2], Variant.BIG, Modifier.weight(1f).fillMaxHeight(), requestLocation)
                    Block(m, 1, o[1], Variant.NARROW, narrow, requestLocation)
                }
            }
        }
    }
}

@Composable
fun Block(m: LauncherModel, slot: Int, kind: String, v: Variant, modifier: Modifier, requestLocation: () -> Unit) {
    val p = LocalPal.current
    val selected = m.edit && m.sel == slot
    val isMap = kind == "speed" && m.mapLayout
    // Cada bloco tem a sua cor no tema Blocos; o texto dentro acompanha
    val local = when {
        kind == "clock" -> p.copy(cd = p.clockBg, tx = p.clockFg, tx2 = p.clockMut)
        kind == "speed" && !isMap -> p.copy(cd = p.speedBg, tx = p.speedFg, tx2 = p.speedFg.copy(alpha = 0.75f))
        else -> p
    }
    Box(modifier.panel(p, 14, if (selected) lerp(local.cd, p.ac, 0.28f) else local.cd)) {
        CompositionLocalProvider(LocalPal provides local) {
            if (isMap) {
                MapBlock(m, requestLocation)
            } else {
                Box(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (kind) {
                        "clock" -> ClockBlock(m, v)
                        "speed" -> SpeedBlock(m, v, requestLocation)
                        else -> MediaBlock(m, v)
                    }
                }
            }
        }
        if (m.edit) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val i = 1.dp.toPx()
                        drawRoundRect(
                            color = p.ac,
                            topLeft = Offset(i, i),
                            size = Size(size.width - 2 * i, size.height - 2 * i),
                            cornerRadius = CornerRadius(13.dp.toPx()),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                            )
                        )
                    }
                    .clickable { m.pick(slot) }
            )
        }
    }
}

private val DIAS = arrayOf("Domingo", "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado")

@Composable
fun ClockBlock(m: LauncherModel, v: Variant) {
    val p = LocalPal.current
    val c = Calendar.getInstance().apply { timeInMillis = m.now }
    val time = "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    val weekday = DIAS[c.get(Calendar.DAY_OF_WEEK) - 1]
    val date = "%02d/%02d/%d".format(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
    when (v) {
        Variant.WIDE -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            T(time, 46, p.tx, FontWeight.Medium, ls = -1f)
            W(16)
            Box(Modifier.width(2.dp).height(36.dp).background(p.bd))
            W(16)
            Column {
                T(weekday, 16, p.tx, FontWeight.Medium)
                H(2)
                T(date, 15, p.tx2)
            }
            Spacer(Modifier.weight(1f))
            WeatherView(m)
        }
        Variant.NARROW -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                T(time, 42, p.tx, FontWeight.Medium, ls = -1f)
                H(6)
                T(weekday, 15, p.tx, FontWeight.Medium)
                T(date, 14, p.tx2)
            }
            WeatherView(m)
        }
        Variant.BIG -> Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            T(time, 72, p.tx, FontWeight.Medium, align = TextAlign.Center, ls = -1f)
            T("$weekday, $date", 16, p.tx2, align = TextAlign.Center)
            WeatherView(m)
        }
    }
}

private data class WInfo(val icon: ImageVector, val desc: String, val tint: Color, val tintLight: Color = tint)

private fun weatherInfo(code: Int, day: Boolean): WInfo = when (code) {
    0 -> if (day) WInfo(Icons.Rounded.WbSunny, "Ensolarado", Color(0xFFEF9F27), Color(0xFFF08C00))
    else WInfo(Icons.Rounded.NightsStay, "Céu limpo", Color(0xFFAFA9EC))
    1, 2 -> WInfo(Icons.Rounded.WbCloudy, "Poucas nuvens", Color(0xFFD3D1C7), Color(0xFF4F8FD6))
    3 -> WInfo(Icons.Rounded.Cloud, "Nublado", Color(0xFFB4B2A9), Color(0xFF6B7A90))
    45, 48 -> WInfo(Icons.Rounded.Cloud, "Neblina", Color(0xFFB4B2A9))
    in 51..67, in 80..82 -> WInfo(Icons.Rounded.Umbrella, "Chuva", Color(0xFF85B7EB), Color(0xFF1A73E8))
    in 71..77, 85, 86 -> WInfo(Icons.Rounded.AcUnit, "Neve", Color(0xFFB5D4F4))
    in 95..99 -> WInfo(Icons.Rounded.FlashOn, "Tempestade", Color(0xFFFAC775))
    else -> WInfo(Icons.Rounded.Cloud, "Tempo", Color(0xFFB4B2A9))
}

/** Clima só aparece se já foi buscado alguma vez; some depois de 6h sem atualizar. */
@Composable
fun WeatherView(m: LauncherModel) {
    val p = LocalPal.current
    if (m.wTs <= 0L) return
    val age = m.now - m.wTs
    if (age > 6 * 3_600_000L) return
    val info = weatherInfo(m.wCode, m.wIsDay)
    val label = when {
        age < 40 * 60_000L -> info.desc
        age < 3_600_000L -> "Há ${age / 60_000} min"
        else -> "Há ${age / 3_600_000}h"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        val tint = when {
            p.night -> lerp(info.tint, p.bg, 0.25f)
            p.light -> info.tintLight
            else -> info.tint
        }
        Ic(info.icon, 32, tint)
        W(10)
        Column {
            T("${m.wTemp.roundToInt()}°", 26, p.tx, FontWeight.Medium)
            T(label, 12, p.tx2)
        }
    }
}

@Composable
fun SpeedBlock(m: LauncherModel, v: Variant, requestLocation: () -> Unit) {
    val p = LocalPal.current
    val now = m.now
    if (!m.gps.hasPermission()) {
        Column(
            Modifier.fillMaxSize().clickable { requestLocation() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Ic(Icons.Rounded.LocationOn, 30, p.ac)
            H(8)
            T("Permitir GPS", 14, p.tx, FontWeight.Medium, align = TextAlign.Center)
            T("Toque aqui", 12, p.tx2, align = TextAlign.Center)
        }
        return
    }
    val fix = m.gps.hasFix(now)
    val target = if (fix) m.gps.speedKmh else 0f
    val sp by animateFloatAsState(target, tween(450), label = "speed")
    val over = m.alertKmh > 0 && sp > m.alertKmh
    val c = if (over) p.red else p.ac
    val num = if (fix) sp.roundToInt().toString() else "--"
    val frac = (sp / 180f).coerceIn(0f, 1f)
    when (v) {
        Variant.WIDE -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.width(112.dp), verticalAlignment = Alignment.Bottom) {
                T(num, 42, if (over) p.red else p.tx, FontWeight.Medium)
                W(6)
                T(if (fix) "km/h" else "Sem GPS", 12, p.tx2, modifier = Modifier.padding(bottom = 8.dp))
            }
            W(16)
            Box(
                Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(p.cd2)
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(RoundedCornerShape(5.dp)).background(c))
            }
        }
        else -> {
            val w = if (v == Variant.BIG) 220f else 124f
            Box(Modifier.size(w.dp, (w * 0.7f + 24f).dp)) {
                Canvas(Modifier.size(w.dp, (w * 0.7f).dp).align(Alignment.TopCenter)) {
                    val sw = size.width * 14f / 220f
                    val r = size.width * 90f / 220f
                    val cx = size.width / 2f
                    val cy = size.height * 140f / 150f
                    val tl = Offset(cx - r, cy - r)
                    val sz = Size(2 * r, 2 * r)
                    drawArc(p.cd2, 180f, 180f, false, tl, sz, style = Stroke(width = sw, cap = StrokeCap.Round))
                    if (frac > 0.005f) {
                        drawArc(c, 180f, 180f * frac, false, tl, sz, style = Stroke(width = sw, cap = StrokeCap.Round))
                    }
                }
                Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    T(num, if (v == Variant.BIG) 60 else 44, if (over) p.red else p.tx, FontWeight.Medium)
                    T(if (fix) "km/h" else "Sem GPS", 12, p.tx2)
                }
            }
        }
    }
}

@Composable
fun MediaBlock(m: LauncherModel, v: Variant) {
    val p = LocalPal.current
    val md = m.media
    @Suppress("UNUSED_VARIABLE") val tick = m.now
    if (!md.hasAccess) {
        Column(
            Modifier.fillMaxSize().clickable { m.openNotifAccess() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Ic(Icons.Rounded.Album, 30, p.ac)
            H(8)
            T("Permitir acesso à mídia", 14, p.tx, FontWeight.Medium, align = TextAlign.Center)
            H(2)
            T("Para mostrar capa e controles", 12, p.tx2, align = TextAlign.Center, maxLines = 2)
        }
        return
    }
    val empty = md.title.isEmpty()
    if (empty) {
        MediaEmpty(m, v)
        return
    }
    val title = if (empty) "Nada tocando" else md.title
    val artist = if (empty) "Abra um app de música" else md.artist
    val pos = md.position()
    val dur = md.duration
    val frac = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    when (v) {
        Variant.WIDE -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Art(md.art, 56, md.title)
            W(14)
            Column(Modifier.weight(1f)) {
                T(title, 16, p.tx, FontWeight.Medium)
                H(3)
                T(artist, 13, p.tx2)
            }
            W(10)
            Controls(md, 44, 40)
            W(12)
            MuteBtn(m, 40)
        }
        Variant.NARROW -> Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Art(md.art, 64, md.title)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                T(title, 16, p.tx, FontWeight.Medium, align = TextAlign.Center)
                H(3)
                T(artist, 13, p.tx2, align = TextAlign.Center)
            }
            Progress(frac, pos, dur)
            Controls(md, 44, 38, Modifier.fillMaxWidth(), Arrangement.SpaceBetween)
            VolButtons(m, 38, Modifier.fillMaxWidth())
        }
        Variant.BIG -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Art(md.art, 84, md.title)
                W(14)
                Column(Modifier.weight(1f)) {
                    T(title, 16, p.tx, FontWeight.Medium)
                    H(3)
                    T(artist, 13, p.tx2)
                    if (md.source.isNotEmpty()) {
                        H(8)
                        T(md.source, 12, p.ac)
                    }
                }
            }
            Progress(frac, pos, dur)
            Controls(md, 56, 48, Modifier.fillMaxWidth(), Arrangement.SpaceEvenly)
            VolButtons(m, 40, Modifier.fillMaxWidth().padding(horizontal = 24.dp))
        }
    }
}

@Composable
fun Art(bmp: ImageBitmap?, size: Int, title: String) {
    val p = LocalPal.current
    // Sem capa: bloco colorido com a inicial da música (cor fixa por música)
    val tile = if (title.isEmpty()) p.cd2 else {
        val list = listOf(p.ac, lerp(p.ac, Color.Black, 0.25f), lerp(p.ac, Color.White, 0.2f))
        val c = list[Math.floorMod(title.hashCode(), list.size)]
        if (p.night) lerp(c, Color.Black, 0.35f) else c
    }
    Box(
        Modifier.size(size.dp).panel(p, 12, if (bmp != null) p.cd2 else tile),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = if (p.night) ColorFilter.tint(Color(0x59000000), BlendMode.SrcAtop) else null
            )
        } else if (title.isNotEmpty()) {
            val initial = title.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "♪"
            T(initial, (size * 0.45f).toInt(), Color.White, FontWeight.Medium, align = TextAlign.Center)
        } else {
            Ic(Icons.Rounded.Album, (size * 0.45f).toInt(), p.tx2)
        }
    }
}

@Composable
fun Progress(frac: Float, pos: Long, dur: Long) {
    val p = LocalPal.current
    // Sem duração (comum no Bluetooth) a barra ficaria parada em 0:00: melhor não mostrar
    if (dur <= 0) return
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(p.cd2)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(p.ac))
        }
        if (dur > 0) {
            H(5)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                T(fmt(pos), 11, p.tx2)
                T(fmt(dur), 11, p.tx2)
            }
        }
    }
}

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun Controls(
    md: MediaRepo,
    play: Int,
    side: Int,
    modifier: Modifier = Modifier,
    arrangement: Arrangement.Horizontal = Arrangement.spacedBy(10.dp)
) {
    val p = LocalPal.current
    Row(modifier, horizontalArrangement = arrangement, verticalAlignment = Alignment.CenterVertically) {
        CircleBtn(Icons.Rounded.SkipPrevious, side, p.cd2, p.tx) { md.prev() }
        CircleBtn(if (md.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, play, p.ac, p.on) { md.toggle() }
        CircleBtn(Icons.Rounded.SkipNext, side, p.cd2, p.tx) { md.next() }
    }
}

@Composable
fun VolButtons(m: LauncherModel, size: Int, modifier: Modifier = Modifier) {
    val p = LocalPal.current
    Row(modifier, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        CircleBtn(Icons.AutoMirrored.Rounded.VolumeDown, size, p.cd2, p.tx) { m.volume(-1) }
        MuteBtn(m, size)
        CircleBtn(Icons.AutoMirrored.Rounded.VolumeUp, size, p.cd2, p.tx) { m.volume(1) }
    }
}

@Composable
fun MuteBtn(m: LauncherModel, size: Int) {
    val p = LocalPal.current
    CircleBtn(
        if (m.muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
        size, if (m.muted) p.red else p.cd2, if (m.muted) Color.White else p.ac
    ) { m.toggleMute() }
}

@Composable
fun CircleBtn(icon: ImageVector, size: Int, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bg)
            .then(if (bg == LocalPal.current.ac) Modifier else Modifier.border(1.5.dp, LocalPal.current.bd, CircleShape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Ic(icon, (size * 0.5f).toInt(), fg)
    }
}

// ---------------------------------------------------------------- painéis sobrepostos

@Composable
fun OverlayHost(m: LauncherModel, o: Overlay) {
    val p = LocalPal.current
    val scrimSrc = remember { MutableInteractionSource() }
    val cardSrc = remember { MutableInteractionSource() }
    fun card(size: Modifier) = size
        .panel(p, 14, p.sheet)
        .clickable(interactionSource = cardSrc, indication = null) {}
        .padding(16.dp)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(interactionSource = scrimSrc, indication = null) { m.overlay = null },
        contentAlignment = Alignment.Center
    ) {
        when (o) {
            is Overlay.Picker -> PickerPanel(m, o.target, card(Modifier.width(500.dp)))
            Overlay.Config -> ConfigPanel(m, card(Modifier.width(580.dp)))
            Overlay.Apps -> AppsPanel(m, card(Modifier.fillMaxSize(0.94f)))
            Overlay.Colors -> Column(card(Modifier.width(580.dp))) {
                PanelHeader("Cor base") { m.overlay = Overlay.Config }
                BaseColorPicker(m)
            }
        }
    }
}

@Composable
fun PanelHeader(title: String, onClose: () -> Unit) {
    val p = LocalPal.current
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        T(title, 16, p.tx, FontWeight.Medium, modifier = Modifier.weight(1f))
        CircleBtn(Icons.Rounded.Close, 36, p.cd2, p.tx, onClose)
    }
}

@Composable
fun PickerPanel(m: LauncherModel, target: Int, modifier: Modifier) {
    val p = LocalPal.current
    val isEq = target == 3
    val curPkg = if (isEq) m.eqPkg else m.slots[target].pkg
    var name by remember(target) { mutableStateOf(if (isEq) "" else m.slots[target].label) }
    Column(modifier) {
        PanelHeader(if (isEq) "App do equalizador" else "Editar atalho") { m.overlay = null }
        if (!isEq) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = name,
                    onValueChange = { if (it.length <= 16) name = it },
                    singleLine = true,
                    textStyle = TextStyle(color = p.tx, fontSize = 15.sp),
                    cursorBrush = SolidColor(p.ac),
                    modifier = Modifier
                        .weight(1f)
                        .panel(p, 8, p.cd2)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
                W(8)
                Chip("Salvar nome", primary = true) {
                    val n = name.trim()
                    if (n.isEmpty()) {
                        m.show("Digite um nome")
                    } else {
                        m.setSlot(target, m.slots[target].copy(label = n))
                        m.overlay = null
                        m.show("Nome salvo")
                    }
                }
            }
        }
        if (m.apps.isEmpty()) {
            T("Carregando apps…", 13, p.tx2)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxWidth().height(if (isEq) 280.dp else 230.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(m.apps, key = { it.pkg }) { a ->
                    AppCell(a, a.pkg == curPkg) {
                        if (isEq) {
                            m.setEq(a.pkg)
                            m.show("EQ agora abre ${a.label}")
                        } else {
                            m.setSlot(target, Slot(a.pkg, a.label))
                            m.show("Atalho agora abre ${a.label}")
                        }
                        m.overlay = null
                    }
                }
            }
        }
    }
}

@Composable
fun AppCell(a: AppInfo, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPal.current
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) lerp(p.cd, p.ac, 0.3f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(bitmap = a.icon, contentDescription = null, modifier = Modifier.size(40.dp))
        H(4)
        T(a.label, 11, p.tx2, align = TextAlign.Center)
    }
}

@Composable
fun AppsPanel(m: LauncherModel, modifier: Modifier) {
    Column(modifier) {
        PanelHeader("Apps") { m.overlay = null }
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(m.apps, key = { it.pkg }) { a ->
                AppCell(a, false) {
                    m.overlay = null
                    if (!launch(m.ctx, a.pkg)) m.show("Não foi possível abrir")
                }
            }
        }
    }
}

private val ALERT_OPTS = listOf(0, 100, 110, 120)

@Composable
fun ConfigPanel(m: LauncherModel, modifier: Modifier) {
    val p = LocalPal.current
    Column(modifier) {
        PanelHeader("Ajustes do launcher") { m.overlay = null }
        LaunchedEffect(Unit) { m.readBrightness() }
        // Brilho no topo: é o ajuste mais usado
        Row(
            Modifier.fillMaxWidth().panel(p, 10, p.cd2).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (m.writeOk) {
                Ic(Icons.Rounded.WbSunny, 18, p.tx2)
                W(8)
                Box(Modifier.weight(1f)) {
                    ChannelSlider("", m.brightness, Color(0xFF15171A), Color(0xFFFFFFFF), { m.changeBrightness(it) }) {}
                }
                W(4)
                Ic(Icons.Rounded.WbSunny, 26, p.ac)
            } else {
                Ic(Icons.Rounded.WbSunny, 20, p.tx2)
                W(10)
                T("Brilho da tela", 15, p.tx, modifier = Modifier.weight(1f))
                Chip("Liberar permissão", primary = true) { m.openWriteSettings() }
            }
        }
        H(14)
        Column(
            Modifier.heightIn(max = 310.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SetRow("Layout") {
                Chip("Editar layout", Icons.Rounded.Dashboard, primary = true) {
                    m.overlay = null
                    m.sel = null
                    m.edit = true
                }
            }
            SetRow("Tema") {
                Seg(listOf("Vidro", "Blocos"), m.theme) { m.updTheme(it) }
            }
            if (m.theme == THEME_GLASS) {
                SetRow("Fundo em movimento") {
                    Seg(listOf("Não", "Sim"), if (m.bgMotion) 1 else 0) { m.updBgMotion(it == 1) }
                }
            }
            SetRow("Cor base") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(Color(m.baseColor)).border(2.dp, p.tx, CircleShape))
                    W(10)
                    Chip("Alterar", Icons.Rounded.ChevronRight) { m.overlay = Overlay.Colors }
                }
            }
            SetRow("Mapa na tela inicial") {
                Seg(listOf("Não", "Sim"), if (m.showMap) 1 else 0) { m.updMap(it == 1) }
            }
            SetRow("Mapa offline de Fortaleza") {
                when (m.map.state) {
                    MapDl.RUNNING -> Chip("Baixando ${(m.map.progress * 100).roundToInt()}%") {}
                    MapDl.DONE -> Chip("Baixado · atualizar", Icons.Rounded.Check) { m.downloadMap() }
                    MapDl.ERROR -> Chip("Erro · tentar de novo", primary = true) { m.downloadMap() }
                    MapDl.IDLE -> Chip("Baixar (~20 MB)", primary = true) { m.downloadMap() }
                }
            }
            SetRow("Modo noturno") {
                Seg(listOf("Desligado", "Automático", "Sempre"), m.nightMode) { m.updNight(it) }
            }
            if (m.nightMode == 1) {
                val (on, off) = m.nightWindow()
                val f = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                T("Pelo pôr do sol: liga às ${f.format(java.util.Date(on))}, desliga às ${f.format(java.util.Date(off))}",
                    13, p.tx2, align = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }
            SetRow("Baixar brilho à noite") {
                Seg(listOf("Não", "Sim"), if (m.dimScreen) 1 else 0) { v ->
                    if (v == 1 && !m.writeOk) {
                        m.openWriteSettings()
                        m.show("Libere a permissão e volte")
                    }
                    m.updDim(v == 1)
                }
            }
            SetRow("Alerta de velocidade") {
                Seg(listOf("Desligado", "100", "110", "120"), ALERT_OPTS.indexOf(m.alertKmh).coerceAtLeast(0)) {
                    m.updAlert(ALERT_OPTS[it])
                }
            }
            SetRow("Mostrar mídia") {
                Seg(listOf("Não", "Sim"), if (m.showMedia) 1 else 0) { m.updMedia(it == 1) }
            }
            SetRow("Acesso à mídia") {
                Chip(if (m.media.hasAccess) "Liberado" else "Liberar", Icons.Rounded.Notifications) {
                    m.openNotifAccess()
                }
            }
            SetRow("Configurações do Android", muted = true) {
                Chip("Abrir", Icons.Rounded.ChevronRight) {
                    m.overlay = null
                    m.openAndroidSettings()
                }
            }
        }
    }
}

@Composable
fun SetRow(label: String, muted: Boolean = false, content: @Composable () -> Unit) {
    val p = LocalPal.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        T(label, 15, if (muted) p.tx2 else p.tx, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
fun Seg(opts: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPal.current
    Row(
        Modifier.panel(p, 9, p.cd2).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        opts.forEachIndexed { i, o ->
            Box(
                Modifier
                    .clip(RoundedCornerShape((7 + p.round).dp))
                    .background(if (i == selected) p.ac else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 11.dp)
            ) {
                T(o, 14, if (i == selected) p.on else p.tx)
            }
        }
    }
}

// ---------------------------------------------------------------- peças básicas

@Composable
fun Chip(
    text: String,
    icon: ImageVector? = null,
    primary: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val p = LocalPal.current
    val bg = if (primary) p.ac else p.cd2
    val fg = if (primary) p.on else p.tx
    Row(
        Modifier
            .panel(p, 8, bg, border = !primary)
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 10.dp else 16.dp, vertical = if (compact) 3.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Ic(icon, if (compact) 14 else 18, fg)
            W(6)
        }
        T(text, if (compact) 12 else 14, fg)
    }
}

@Composable
fun ToastPill(text: String, modifier: Modifier) {
    val p = LocalPal.current
    Box(modifier.panel(p, 16, p.cd2).padding(horizontal = 16.dp, vertical = 8.dp)) {
        T(text, 12, p.tx)
    }
}

@Composable
fun T(
    text: String,
    size: Int,
    color: Color = LocalPal.current.tx,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
    ls: Float = 0f
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = weight,
            textAlign = align,
            letterSpacing = ls.sp,
            lineHeight = (size * 1.2f).sp
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun Ic(icon: ImageVector, size: Int, color: Color, modifier: Modifier = Modifier) {
    Image(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier.size(size.dp),
        colorFilter = ColorFilter.tint(color)
    )
}

@Composable
fun W(n: Int) = Spacer(Modifier.width(n.dp))

@Composable
fun H(n: Int) = Spacer(Modifier.height(n.dp))

/** Fundo + borda visível: no sol forte a borda é o que separa um botão do outro. */
fun Modifier.panel(p: Pal, radius: Int, bg: Color = p.cd, border: Boolean = true): Modifier {
    val shape = RoundedCornerShape((radius + p.round).dp)
    if (p.glass && bg == p.cd) {
        // Vidro: sombra, degradê de cima para baixo e borda que reflete a luz no topo
        val lifted = if (radius >= 14) this.shadow(8.dp, shape, ambientColor = p.shadow, spotColor = p.shadow) else this
        return lifted
            .clip(shape)
            .background(Brush.verticalGradient(listOf(p.cdTop, p.cdBottom)))
            .border(1.dp, Brush.verticalGradient(listOf(p.edgeTop, p.edgeBottom)), shape)
    }
    val m = this.clip(shape).background(bg)
    return if (border && bg.alpha > 0f && p.bd.alpha > 0f) m.border(1.dp, p.bd, shape) else m
}


// ---------------------------------------------------------------- seletor de cor base

private val BASE_PRESETS = listOf(0xFF1D4ED8, 0xFF0E7490, 0xFF15803D, 0xFF7C3AED, 0xFFBE185D, 0xFFB45309, 0xFF475569).map { it.toInt() }

/** Três trilhos R, G e B. A tela inteira muda ao arrastar; salva ao soltar. */
@Composable
fun BaseColorPicker(m: LauncherModel) {
    val p = LocalPal.current
    val base = Color(m.baseColor)
    Column(Modifier.fillMaxWidth().panel(p, 10, p.cd2).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(base).border(2.dp, p.tx, CircleShape))
            W(10)
            T("Cor base", 13, p.tx, modifier = Modifier.weight(1f))
            T("#%06X".format(m.baseColor and 0xFFFFFF), 12, p.tx2)
            W(10)
            Chip("Padrão") { m.previewBase(DEFAULT_BASE); m.saveBase() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BASE_PRESETS.forEach { c ->
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(Color(c))
                        .border(2.dp, if (c == m.baseColor) p.tx else Color.Transparent, CircleShape)
                        .clickable { m.previewBase(c); m.saveBase() }
                )
            }
        }
        val r = (m.baseColor shr 16) and 0xFF
        val g = (m.baseColor shr 8) and 0xFF
        val b = m.baseColor and 0xFF
        fun set(nr: Int, ng: Int, nb: Int) = m.previewBase((0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb)
        ChannelSlider("R", r, Color(0, g, b), Color(255, g, b), { set(it, g, b) }) { m.saveBase() }
        ChannelSlider("G", g, Color(r, 0, b), Color(r, 255, b), { set(r, it, b) }) { m.saveBase() }
        ChannelSlider("B", b, Color(r, g, 0), Color(r, g, 255), { set(r, g, it) }) { m.saveBase() }
    }
}

@Composable
fun ChannelSlider(label: String, value: Int, from: Color, to: Color, onChange: (Int) -> Unit, onDone: () -> Unit) {
    val p = LocalPal.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        T(label, 12, p.tx2, modifier = Modifier.width(16.dp))
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .height(28.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(onDragEnd = onDone) { change, _ ->
                        onChange(((change.position.x / size.width) * 255f).roundToInt().coerceIn(0, 255))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        onChange(((pos.x / size.width) * 255f).roundToInt().coerceIn(0, 255))
                        onDone()
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(from, to)))
                    .border(1.dp, p.bd, RoundedCornerShape(5.dp))
            )
            val x = maxWidth * (value / 255f) - 11.dp
            Box(
                Modifier.padding(start = x.coerceAtLeast(0.dp)).size(22.dp).clip(CircleShape)
                    .background(Color.White).border(2.dp, Color(0xFF1B1F24), CircleShape)
            )
        }
        T(value.toString(), 12, p.tx2, align = TextAlign.End, modifier = Modifier.width(34.dp))
    }
}


@Composable
fun CrashPanel(m: LauncherModel, text: String) {
    val p = LocalPal.current
    Box(
        Modifier.fillMaxSize().background(Color(0xB3000000)).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) {},
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.width(520.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF15181D)).padding(16.dp)) {
            T("O Painel fechou da última vez", 16, Color.White, FontWeight.Medium)
            H(4)
            T("Copie o erro e mande no chat para eu corrigir.", 12, Color(0xFFB7BEC8))
            H(10)
            Box(
                Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF0B0D10))
                    .verticalScroll(rememberScrollState()).padding(8.dp)
            ) {
                BasicText(
                    text,
                    style = TextStyle(color = Color(0xFFE6E9EE), fontSize = 9.sp, lineHeight = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                )
            }
            H(12)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Copiar erro", primary = true) { m.copyCrash() }
                Chip("Fechar") { m.dismissCrash() }
            }
        }
    }
}


/** Nada tocando: atalhos grandes para começar a ouvir */
@Composable
fun MediaEmpty(m: LauncherModel, v: Variant) {
    val p = LocalPal.current
    val opts: List<Triple<String, ImageVector, () -> Unit>> = listOf(
        Triple(m.slots[1].label, glyphFor(m.slots[1].label, m.slots[1].pkg) ?: Icons.Rounded.MusicNote) { m.tapSlot(1) },
        Triple(m.slots[2].label, glyphFor(m.slots[2].label, m.slots[2].pkg) ?: Icons.Rounded.MusicNote) { m.tapSlot(2) },
        Triple("Retomar", Icons.Rounded.PlayArrow) { m.resumeMedia() }
    )
    // Pilha centralizada, mesma largura para os três botões
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
    ) {
        T("Nada tocando", 15, p.tx2, FontWeight.Medium, align = TextAlign.Center)
        val w = if (v == Variant.NARROW) Modifier.fillMaxWidth() else Modifier.widthIn(max = 260.dp).fillMaxWidth()
        opts.forEach { MediaEmptyBtn(w, it) }
    }
}

@Composable
private fun MediaEmptyBtn(mod: Modifier, o: Triple<String, ImageVector, () -> Unit>) {
    val p = LocalPal.current
    Row(
        mod.height(56.dp).panel(p, 12, p.cd2).clickable { o.third() }.padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Ic(o.second, 22, p.ac)
        W(8)
        T(o.first, 14, p.tx, FontWeight.Medium)
    }
}

/** Tela "apagada": preto com o relógio bem fraco. Toque em qualquer lugar acorda. */
@Composable
fun ScreenOff(m: LauncherModel) {
    val c = Calendar.getInstance().apply { timeInMillis = m.now }
    Box(
        Modifier.fillMaxSize().background(Color.Black).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) { m.setScreenOffState(false) },
        contentAlignment = Alignment.Center
    ) {
        T("%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)), 40, Color(0xFF2A2D33), FontWeight.Medium)
    }
}


/** Bloco "carregando": faixa de luz que passa devagar sobre um fundo neutro do tema. */
@Composable
fun Skeleton(modifier: Modifier) {
    val p = LocalPal.current
    val anim = rememberInfiniteTransition(label = "sk")
    val x = anim.animateFloat(-1f, 2f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "skx")
    val base = lerp(p.cd2, p.bg, 0.3f).copy(alpha = 1f)
    val shine = lerp(base, p.tx, 0.12f)
    Box(
        modifier.drawBehind {
            drawRect(base)
            val w = size.width
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, shine, Color.Transparent),
                    start = Offset(w * x.value - w * 0.4f, 0f),
                    end = Offset(w * x.value + w * 0.4f, size.height)
                )
            )
        }
    )
}
