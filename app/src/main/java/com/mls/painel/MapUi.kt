package com.mls.painel

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import android.view.ViewGroup
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.sources.VectorSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString
import org.maplibre.geojson.Point
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

/** Estado do mapa, criado uma vez só e reaproveitado quando ele aparece na tela. */
class MapCtl(val view: MapView) {
    var map by mutableStateOf<MapLibreMap?>(null)
    var style by mutableStateOf<Style?>(null)
    var follow by mutableStateOf(true)
    var street by mutableStateOf("")
    /** Estilo carregado e primeiros blocos desenhados: pronto para ir para a tela */
    var shown by mutableStateOf(false)
    /** Rua em que o carro foi "encaixado" por último (evita pular para ruas paralelas) */
    var roadKey: String? = null
    /** Posição mostrada (encaixada na rua quando faz sentido) */
    var shownLoc: android.location.Location? = null
}

val LocalMap = staticCompositionLocalOf<MapCtl?> { null }

/**
 * Motor do mapa: cria, carrega e mantém o mapa (posição, cores, rua) enquanto a opção está ligada.
 * Ele começa escondido; a tela inicial usa o layout normal até o mapa ficar pronto.
 */
@SuppressLint("MissingPermission")
@Composable
fun rememberMapEngine(m: LauncherModel): MapCtl {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mp = LocalPal.current.map
    val ctl = remember {
        m.prefs.mapOpening = true
        MapRepo.init(ctx)
        MapCtl(MapView(ctx, MapLibreMapOptions.createFromAttributes(ctx).textureMode(true)).also { it.onCreate(null) })
    }
    val mapView = ctl.view

    DisposableEffect(lifecycle, mapView) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycle.addObserver(obs)
        onDispose {
            lifecycle.removeObserver(obs)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
            m.mapLive = false
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { mm ->
            mm.uiSettings.isCompassEnabled = false
            mm.uiSettings.isLogoEnabled = false
            mm.uiSettings.isRotateGesturesEnabled = false
            mm.uiSettings.isTiltGesturesEnabled = false
            mm.uiSettings.isAttributionEnabled = true
            mm.cameraPosition = CameraPosition.Builder().target(MapCfg.CENTER).zoom(12.0).build()
            // Toque no mapa abre o CarPlay (ZLink) para navegar; segurar volta a seguir o carro
            mm.addOnMapClickListener { m.tapSlot(0); true }
            mm.addOnMapLongClickListener { ctl.follow = true; true }
            mm.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) ctl.follow = false
            }
            ctl.map = mm
        }
    }

    // Carrega o estilo uma vez e liga o marcador de posição
    LaunchedEffect(ctl.map) {
        val mm = ctl.map ?: return@LaunchedEffect
        mm.setStyle(Style.Builder().fromUri(MapCfg.STYLE)) { st ->
            paintMap(st, mp)
            if (m.gps.hasPermission()) {
                try {
                    val lc = mm.locationComponent
                    lc.activateLocationComponent(
                        LocationComponentActivationOptions.builder(ctx, st)
                            .useDefaultLocationEngine(false)
                            .build()
                    )
                    lc.isLocationComponentEnabled = true
                    lc.cameraMode = CameraMode.NONE
                    lc.renderMode = RenderMode.GPS
                    m.gps.lastLoc?.let { lc.forceLocationUpdate(it) }
                } catch (e: Exception) {
                }
            }
            ctl.style = st
        }
    }

    // Estilo pronto: dá um instante para desenhar as ruas e só então mostra o mapa
    LaunchedEffect(ctl.style) {
        if (ctl.style != null) {
            kotlinx.coroutines.delay(900)
            ctl.shown = true
            m.mapLive = true
            kotlinx.coroutines.delay(3000)
            m.prefs.mapOpening = false
        }
    }

    // Repinta quando muda tema, dia/noite ou cor base
    LaunchedEffect(ctl.style, mp) {
        ctl.style?.let { paintMap(it, mp) }
    }

    // Segue o carro: aproxima mais devagar, afasta em velocidade, mapa gira com a direção
    LaunchedEffect(ctl.map, ctl.style, m.gps.lastFix, ctl.follow) {
        val mm = ctl.map ?: return@LaunchedEffect
        val raw = m.gps.lastLoc ?: return@LaunchedEffect
        val kmh = m.gps.speedKmh
        // Encaixa na rua só andando e com GPS razoável; parado (estacionamento) ou GPS ruim, posição real
        val acc = if (raw.hasAccuracy()) raw.accuracy else 20f
        val st = ctl.style
        val snap = if (st != null && kmh >= 8f && acc <= 50f)
            snapToRoad(st, raw.latitude, raw.longitude, m.gps.heading, acc, ctl.roadKey) else null
        ctl.roadKey = snap?.key
        val l = android.location.Location(raw).apply {
            if (snap != null) {
                latitude = snap.lat
                longitude = snap.lon
                bearing = snap.bearing
            }
        }
        ctl.shownLoc = l
        val bearing = (snap?.bearing ?: m.gps.heading).toDouble()
        if (st != null) {
            try {
                mm.locationComponent.forceLocationUpdate(l)
            } catch (e: Exception) {
            }
        }
        if (!ctl.follow) return@LaunchedEffect
        val zoom = when {
            kmh < 25 -> 17.5
            kmh < 60 -> 17.0
            kmh < 90 -> 16.2
            else -> 15.5
        }
        // Frente do carro para cima; carro no terço de baixo mostrando o caminho à frente
        val ahead = mapView.height * 0.38
        mm.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(l.latitude, l.longitude))
                    .zoom(zoom)
                    .bearing(bearing)
                    .padding(0.0, ahead, 0.0, 0.0)
                    .build()
            ),
            900
        )
    }

    // Nome da rua atual, lido do mapa offline (a cada ~2 s)
    var lastQuery by remember { mutableStateOf(0L) }
    LaunchedEffect(ctl.style, m.gps.lastFix) {
        val st = ctl.style ?: return@LaunchedEffect
        val l = ctl.shownLoc ?: m.gps.lastLoc ?: return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (now - lastQuery < 2000) return@LaunchedEffect
        lastQuery = now
        nearestStreet(st, l.latitude, l.longitude, m.gps.heading)?.let { ctl.street = it }
    }
    return ctl
}

/** Coloca a view do mapa numa posição da tela (tirando de onde estava antes). */
@Composable
fun MapSurface(ctl: MapCtl, modifier: Modifier) {
    AndroidView(
        factory = {
            (ctl.view.parent as? ViewGroup)?.removeView(ctl.view)
            ctl.view
        },
        modifier = modifier
    )
}

/** Bloco do mapa na tela inicial (só aparece quando o mapa já está pronto). */
@Composable
fun MapBlock(m: LauncherModel, requestLocation: () -> Unit) {
    val ctl = LocalMap.current
    if (m.map.state != MapDl.DONE || ctl == null) {
        MapPlaceholder(m, requestLocation)
        return
    }
    val p = LocalPal.current
    Box(Modifier.fillMaxSize()) {
        MapSurface(ctl, Modifier.fillMaxSize())

        if (ctl.street.isNotEmpty()) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp, start = 70.dp, end = 70.dp)
                    .panel(p, 12, p.sheet).padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                T(ctl.street, 15, p.tx, FontWeight.Medium, align = TextAlign.Center)
            }
        }

        SpeedBadge(m, Modifier.align(Alignment.TopStart).padding(10.dp))

        if (!ctl.follow) {
            Box(Modifier.align(Alignment.BottomEnd).padding(10.dp)) {
                CircleBtn(Icons.Rounded.MyLocation, 48, p.ac, p.on) { ctl.follow = true }
            }
        }
    }
}

@Composable
private fun MapPlaceholder(m: LauncherModel, requestLocation: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Box(Modifier.align(Alignment.Center)) { SpeedBlock(m, Variant.BIG, requestLocation) }
        Box(Modifier.align(Alignment.BottomCenter)) {
            when (m.map.state) {
                MapDl.RUNNING -> Chip("Baixando mapa ${(m.map.progress * 100).roundToInt()}%", Icons.Rounded.CloudDownload) {}
                MapDl.ERROR -> Chip("Erro no download · tentar de novo", Icons.Rounded.CloudDownload, primary = true) { m.downloadMap() }
                else -> Chip("Baixar mapa de Fortaleza", Icons.Rounded.CloudDownload, primary = true) { m.downloadMap() }
            }
        }
    }
}

@Composable
fun SpeedBadge(m: LauncherModel, modifier: Modifier) {
    val p = LocalPal.current
    val fix = m.gps.hasFix(m.now)
    val sp by animateFloatAsState(if (fix) m.gps.speedKmh else 0f, tween(450), label = "badge")
    val over = m.alertKmh > 0 && sp > m.alertKmh
    Row(
        modifier.panel(p, 12, if (over) p.red else if (p.glass) p.sheet else p.speedBg).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        T(if (fix) sp.roundToInt().toString() else "--", 30, if (over) androidx.compose.ui.graphics.Color.White else if (p.glass) p.tx else p.speedFg, FontWeight.Medium)
        W(4)
        T("km/h", 12, if (over) androidx.compose.ui.graphics.Color.White else if (p.glass) p.tx2 else p.speedFg.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 5.dp))
    }
}


private val MAJOR = Regex("motorway|trunk|primary|major")

/** Repinta as camadas do estilo base com as cores do tema atual. */
fun paintMap(st: Style, mp: MapPal) {
    val building = lerp(mp.land, mp.casing, 0.45f).toArgb()
    for (layer in st.layers) {
        val id = layer.id.lowercase()
        try {
            when (layer) {
                is BackgroundLayer -> layer.setProperties(PropertyFactory.backgroundColor(mp.land.toArgb()))
                is FillLayer -> {
                    val col = when {
                        "water" in id -> mp.water.toArgb()
                        "park" in id || "wood" in id || "grass" in id || "landcover" in id -> mp.park.toArgb()
                        "building" in id -> building
                        else -> mp.land.toArgb()
                    }
                    layer.setProperties(
                        PropertyFactory.fillColor(col),
                        PropertyFactory.fillOutlineColor(col),
                        // Prédios poluem o mapa no carro: escondidos
                        PropertyFactory.visibility(if ("building" in id) Property.NONE else Property.VISIBLE)
                    )
                }
                is LineLayer -> when {
                    "waterway" in id || "water" in id -> layer.setProperties(PropertyFactory.lineColor(mp.water.toArgb()))
                    "boundary" in id || "admin" in id -> layer.setProperties(PropertyFactory.lineColor(mp.casing.toArgb()))
                    "casing" in id || "outline" in id -> layer.setProperties(
                        PropertyFactory.lineColor(if (MAJOR.containsMatchIn(id)) mp.mainCasing.toArgb() else mp.casing.toArgb())
                    )
                    "road" in id || "highway" in id || "street" in id || "bridge" in id || "tunnel" in id || "path" in id ->
                        layer.setProperties(
                            PropertyFactory.lineColor(if (MAJOR.containsMatchIn(id)) mp.main.toArgb() else mp.road.toArgb())
                        )
                    "rail" in id -> layer.setProperties(PropertyFactory.lineColor(mp.casing.toArgb()))
                }
                is SymbolLayer -> layer.setProperties(
                    PropertyFactory.textColor(mp.label.toArgb()),
                    PropertyFactory.textHaloColor(mp.land.toArgb())
                )
            }
        } catch (e: Exception) {
        }
    }
}


/**
 * Rua mais próxima do carro (até ~30 m), procurando nos nomes de vias do mapa baixado.
 * Entre ruas quase à mesma distância, prefere a que segue a direção do carro.
 */
fun nearestStreet(st: Style, lat: Double, lon: Double, heading: Float): String? {
    val src = st.getSourceAs<VectorSource>("openmaptiles") ?: return null
    val feats = try {
        src.querySourceFeatures(arrayOf("transportation_name"), null)
    } catch (e: Exception) {
        return null
    }
    val kx = cos(Math.toRadians(lat)) * 111_320.0
    val ky = 110_540.0
    var best: String? = null
    var bestScore = Double.MAX_VALUE
    for (f in feats) {
        val name = (f.getStringProperty("name:pt") ?: f.getStringProperty("name") ?: f.getStringProperty("name:latin"))
            ?.takeIf { it.isNotBlank() } ?: continue
        val lines: List<List<Point>> = when (val g = f.geometry()) {
            is LineString -> listOf(g.coordinates())
            is MultiLineString -> g.coordinates()
            else -> continue
        }
        for (pts in lines) {
            for (i in 0 until pts.size - 1) {
                val ax = (pts[i].longitude() - lon) * kx
                val ay = (pts[i].latitude() - lat) * ky
                val bx = (pts[i + 1].longitude() - lon) * kx
                val by = (pts[i + 1].latitude() - lat) * ky
                val dx = bx - ax
                val dy = by - ay
                val len2 = dx * dx + dy * dy
                if (len2 < 0.01) continue
                val t = (-(ax * dx + ay * dy) / len2).coerceIn(0.0, 1.0)
                val px = ax + t * dx
                val py = ay + t * dy
                val d = sqrt(px * px + py * py)
                if (d > 30) continue
                // Ângulo do trecho vs. direção do carro (0 = mesma direção, sentido não importa)
                val segDeg = Math.toDegrees(atan2(dx, dy))
                var diff = abs(((segDeg - heading) % 180 + 180) % 180)
                if (diff > 90) diff = 180 - diff
                val score = d + diff * 0.15
                if (score < bestScore) {
                    bestScore = score
                    best = name
                }
            }
        }
    }
    return best
}


class Snap(val lat: Double, val lon: Double, val bearing: Float, val key: String)

private val DRIVABLE_SKIP = setOf("path", "track", "rail", "transit", "ferry", "raceway", "pier", "bridge_path")

/**
 * "Map matching" simples: procura nas vias do mapa offline o trecho mais provável para o carro.
 * Pontua por distância, por alinhamento com a direção do carro e dá preferência à via atual.
 * Devolve null se nenhuma via compatível estiver perto o bastante (aí usa a posição real).
 */
fun snapToRoad(st: Style, lat: Double, lon: Double, heading: Float, accuracy: Float, prevKey: String?): Snap? {
    val src = st.getSourceAs<VectorSource>("openmaptiles") ?: return null
    val feats = try {
        src.querySourceFeatures(arrayOf("transportation"), null)
    } catch (e: Exception) {
        return null
    }
    val maxDist = (accuracy * 1.5).coerceIn(12.0, 35.0)
    val kx = cos(Math.toRadians(lat)) * 111_320.0
    val ky = 110_540.0
    var best: Snap? = null
    var bestScore = Double.MAX_VALUE
    for (f in feats) {
        val cls = f.getStringProperty("class") ?: ""
        if (cls in DRIVABLE_SKIP) continue
        val lines: List<List<Point>> = when (val g = f.geometry()) {
            is LineString -> listOf(g.coordinates())
            is MultiLineString -> g.coordinates()
            else -> continue
        }
        for (pts in lines) {
            if (pts.size < 2) continue
            val key = "%.5f,%.5f".format(pts[0].latitude(), pts[0].longitude())
            for (i in 0 until pts.size - 1) {
                val ax = (pts[i].longitude() - lon) * kx
                val ay = (pts[i].latitude() - lat) * ky
                val bx = (pts[i + 1].longitude() - lon) * kx
                val by = (pts[i + 1].latitude() - lat) * ky
                val dx = bx - ax
                val dy = by - ay
                val len2 = dx * dx + dy * dy
                if (len2 < 0.25) continue
                val t = (-(ax * dx + ay * dy) / len2).coerceIn(0.0, 1.0)
                val px = ax + t * dx
                val py = ay + t * dy
                val d = sqrt(px * px + py * py)
                if (d > maxDist) continue
                // Direção do trecho no sentido mais próximo da direção do carro
                var segDeg = Math.toDegrees(atan2(dx, dy)).toFloat()
                var diff = abs(((segDeg - heading) % 360 + 540) % 360 - 180)
                if (diff > 90) {
                    segDeg += 180f
                    diff = 180 - diff
                }
                if (diff > 50) continue // via cruzando a direção do carro: não é essa
                val score = d + diff * 0.3 - (if (key == prevKey) 6.0 else 0.0)
                if (score < bestScore) {
                    bestScore = score
                    best = Snap(
                        lat = lat + py / ky,
                        lon = lon + px / kx,
                        bearing = ((segDeg % 360) + 360) % 360,
                        key = key
                    )
                }
            }
        }
    }
    return best
}
