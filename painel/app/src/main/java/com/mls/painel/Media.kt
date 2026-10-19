package com.mls.painel

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat

/** Só existe para o Android liberar o acesso às sessões de mídia (capa e controles). */
class MediaListener : NotificationListenerService()

/** Lê o que está tocando (BT, ZLink, USB, Spotify...) e controla play/pause/próxima. Funciona offline. */
class MediaRepo(private val ctx: Context) {
    var hasAccess by mutableStateOf(false)
        private set
    var title by mutableStateOf("")
        private set
    var artist by mutableStateOf("")
        private set
    var source by mutableStateOf("")
        private set
    var art by mutableStateOf<ImageBitmap?>(null)
        private set
    var playing by mutableStateOf(false)
        private set
    var duration by mutableLongStateOf(0L)
        private set

    private var posBase = 0L
    private var posTime = 0L
    private var speed = 1f
    private var controller: MediaController? = null
    private var listening = false

    private val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val component = ComponentName(ctx, MediaListener::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = read()
        override fun onPlaybackStateChanged(state: PlaybackState?) = read()
        override fun onSessionDestroyed() = refresh()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        pick(list ?: emptyList())
    }

    fun start() {
        hasAccess = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
        if (!hasAccess) return
        try {
            if (!listening) {
                msm.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
                listening = true
            }
            pick(msm.getActiveSessions(component))
        } catch (e: SecurityException) {
            hasAccess = false
        }
    }

    fun stop() {
        if (listening) {
            try {
                msm.removeOnActiveSessionsChangedListener(sessionsListener)
            } catch (e: Exception) {
            }
            listening = false
        }
        controller?.unregisterCallback(callback)
        controller = null
    }

    fun restart() {
        stop()
        start()
    }

    private fun refresh() {
        try {
            pick(msm.getActiveSessions(component))
        } catch (e: Exception) {
            pick(emptyList())
        }
    }

    private fun pick(list: List<MediaController>) {
        val c = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
        if (c?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = c
            c?.registerCallback(callback, handler)
        }
        read()
    }

    private fun read() {
        val c = controller
        if (c == null) {
            title = ""; artist = ""; source = ""; art = null; playing = false; duration = 0L
            posBase = 0L
            return
        }
        val m = c.metadata
        title = m?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: m?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: ""
        artist = m?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: m?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: m?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val bmp = m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: m?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        art = bmp?.asImageBitmap()
        duration = m?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val s = c.playbackState
        playing = s?.state == PlaybackState.STATE_PLAYING
        posBase = s?.position ?: 0L
        posTime = s?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime()
        speed = s?.playbackSpeed ?: 1f
        source = try {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString()
        } catch (e: Exception) {
            c.packageName
        }
    }

    fun position(): Long {
        val p = if (playing) posBase + ((SystemClock.elapsedRealtime() - posTime) * speed).toLong() else posBase
        return if (duration > 0) p.coerceIn(0L, duration) else p.coerceAtLeast(0L)
    }

    fun toggle() {
        val t = controller?.transportControls ?: return
        if (playing) t.pause() else t.play()
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun prev() {
        controller?.transportControls?.skipToPrevious()
    }
}
