package com.mls.painel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

object MapCfg {
    // OpenFreeMap: grátis, sem chave. Download offline permitido até 3.000 tiles / 50 MB.
    // Um estilo neutro só; as cores são repintadas em tempo real conforme tema e cor base
    const val STYLE = "https://tiles.openfreemap.org/styles/positron"
    const val USER_AGENT = "PainelLauncher/1.9 (+https://github.com/mikaelopes/autolauncher)"

    // Fortaleza-CE (cidade inteira + bordas de Caucaia, Maracanaú e Eusébio)
    const val NORTH = -3.68
    const val SOUTH = -3.91
    const val EAST = -38.39
    const val WEST = -38.66
    const val MIN_ZOOM = 10.0
    const val MAX_ZOOM = 14.0 // tiles vetoriais vão até z14; acima disso o mapa só amplia
    val CENTER = LatLng(-3.7319, -38.5267)
}

enum class MapDl { IDLE, RUNNING, DONE, ERROR }

class MapRepo(private val ctx: Context, private val prefs: Prefs) {
    var state by mutableStateOf(if (prefs.mapReady) MapDl.DONE else MapDl.IDLE)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var error by mutableStateOf("")
        private set

    fun download() {
        if (state == MapDl.RUNNING) return
        state = MapDl.RUNNING
        progress = 0f
        error = ""
        init(ctx)
        val om = OfflineManager.getInstance(ctx)
        om.setOfflineMapboxTileCountLimit(3000)
        // Remove downloads anteriores para não duplicar
        om.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                offlineRegions?.forEach {
                    it.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                        override fun onDelete() {}
                        override fun onError(error: String) {}
                    })
                }
                downloadStyle(om, 0)
            }

            override fun onError(error: String) = downloadStyle(om, 0)
        })
    }

    private fun downloadStyle(om: OfflineManager, i: Int) {
        val styles = listOf(MapCfg.STYLE)
        if (i >= styles.size) {
            state = MapDl.DONE
            progress = 1f
            prefs.mapReady = true
            return
        }
        val def = OfflineTilePyramidRegionDefinition(
            styles[i],
            LatLngBounds.from(MapCfg.NORTH, MapCfg.EAST, MapCfg.SOUTH, MapCfg.WEST),
            MapCfg.MIN_ZOOM,
            MapCfg.MAX_ZOOM,
            ctx.resources.displayMetrics.density
        )
        om.createOfflineRegion(def, "fortaleza-$i".toByteArray(), object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion) {
                offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                    override fun onStatusChanged(status: OfflineRegionStatus) {
                        val req = status.requiredResourceCount.coerceAtLeast(1L)
                        progress = (i + status.completedResourceCount.toFloat() / req) / styles.size
                        if (status.isComplete) {
                            offlineRegion.setDownloadState(OfflineRegion.STATE_INACTIVE)
                            offlineRegion.setObserver(null)
                            downloadStyle(om, i + 1)
                        }
                    }

                    // Erros de rede passageiros: a MapLibre tenta de novo sozinha
                    override fun onError(error: OfflineRegionError) {
                        this@MapRepo.error = error.message
                    }

                    override fun mapboxTileCountLimitExceeded(limit: Long) {
                        offlineRegion.setDownloadState(OfflineRegion.STATE_INACTIVE)
                        state = MapDl.ERROR
                        this@MapRepo.error = "Área grande demais ($limit blocos)"
                    }
                })
                offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
            }

            override fun onError(error: String) {
                state = MapDl.ERROR
                this@MapRepo.error = error
            }
        })
    }

    companion object {
        private var ready = false

        /** Chamar uma vez antes de criar qualquer MapView. */
        fun init(ctx: Context) {
            if (ready) return
            MapLibre.getInstance(ctx.applicationContext)
            val client = OkHttpClient.Builder()
                .dispatcher(Dispatcher().apply {
                    maxRequests = 8
                    maxRequestsPerHost = 4
                })
                .addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder().header("User-Agent", MapCfg.USER_AGENT).build())
                }
                .build()
            HttpRequestUtil.setOkHttpClient(client)
            ready = true
        }
    }
}
