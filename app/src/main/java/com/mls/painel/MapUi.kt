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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlin.math.roundToInt

/** Mapa offline com a posição do carro e a velocidade no canto. */
@SuppressLint("MissingPermission")
@Composable
fun MapBlock(m: LauncherModel, requestLocation: () -> Unit) {
    val p = LocalPal.current
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val styleUrl = if (p.light) MapCfg.STYLE_DAY else MapCfg.STYLE_NIGHT

    val mapView = remember {
        MapRepo.init(ctx)
        MapView(ctx, MapLibreMapOptions.createFromAttributes(ctx).textureMode(true)).also { it.onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var follow by remember { mutableStateOf(true) }

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
            mm.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) follow = false
            }
            map = mm
        }
    }

    // Troca o estilo (claro de dia, escuro à noite) e liga o marcador de posição
    LaunchedEffect(map, styleUrl) {
        val mm = map ?: return@LaunchedEffect
        style = null
        mm.setStyle(Style.Builder().fromUri(styleUrl)) { st ->
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
            style = st
        }
    }

    // Segue o carro: aproxima mais devagar, afasta em velocidade, mapa gira com a direção
    LaunchedEffect(map, style, m.gps.lastFix, follow) {
        val mm = map ?: return@LaunchedEffect
        val l = m.gps.lastLoc ?: return@LaunchedEffect
        if (style != null) {
            try {
                mm.locationComponent.forceLocationUpdate(l)
            } catch (e: Exception) {
            }
        }
        if (!follow) return@LaunchedEffect
        val kmh = m.gps.speedKmh
        val zoom = when {
            kmh < 30 -> 16.0
            kmh < 70 -> 15.0
            else -> 14.0
        }
        val bearing = if (l.hasBearing() && kmh > 8) l.bearing.toDouble() else mm.cameraPosition.bearing
        mm.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(LatLng(l.latitude, l.longitude)).zoom(zoom).bearing(bearing).build()
            ),
            900
        )
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        SpeedBadge(m, Modifier.align(Alignment.TopStart).padding(10.dp))

        if (!follow) {
            Box(Modifier.align(Alignment.BottomEnd).padding(10.dp)) {
                CircleBtn(Icons.Rounded.MyLocation, 48, p.ac, p.on) { follow = true }
            }
        }

        if (m.map.state != MapDl.DONE) {
            Box(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Chip(
                    if (m.map.state == MapDl.RUNNING) "Baixando ${(m.map.progress * 100).roundToInt()}%"
                    else "Baixar mapa offline",
                    Icons.Rounded.CloudDownload
                ) { m.overlay = Overlay.Config }
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
        modifier.panel(p, 12, if (over) p.red else p.cd).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        T(if (fix) sp.roundToInt().toString() else "--", 30, if (over) androidx.compose.ui.graphics.Color.White else p.tx, FontWeight.Medium)
        W(4)
        T("km/h", 12, if (over) androidx.compose.ui.graphics.Color.White else p.tx2, modifier = Modifier.padding(bottom = 5.dp))
    }
}
