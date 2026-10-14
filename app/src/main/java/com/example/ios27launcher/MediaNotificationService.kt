package com.example.ios27launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object MediaStateHolder {
    var isListenerConnected by mutableStateOf(false)
    var hasActiveMedia by mutableStateOf(false)
    var isPlaying by mutableStateOf(false)
    var trackTitle by mutableStateOf("Spotify")
    var trackArtist by mutableStateOf("Putar lagu di Spotify")
    var albumTitle by mutableStateOf("")
    var trackDurationMs by mutableLongStateOf(0L)
    var trackPositionMs by mutableLongStateOf(0L)
    var albumArtBitmap by mutableStateOf<ImageBitmap?>(null)
    var activePackage by mutableStateOf<String?>("com.spotify.music")

    var mediaController: MediaController? = null

    fun isNotificationAccessGranted(context: Context): Boolean {
        return try {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            val myComponent = ComponentName(context, MediaNotificationService::class.java).flattenToString()
            enabledListeners?.contains(myComponent) == true
        } catch (_: Exception) {
            false
        }
    }

    fun openNotificationAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openSpotify(context: Context) {
        val pkg = activePackage ?: "com.spotify.music"
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: context.packageManager.getLaunchIntentForPackage("com.spotify.music")
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.spotify.music")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(storeIntent)
            } catch (_: Exception) {}
        }
    }

    fun playPause(context: Context) {
        val mc = mediaController
        if (mc != null) {
            val state = mc.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                mc.transportControls.pause()
            } else {
                mc.transportControls.play()
            }
            return
        }
        // Universal key event fallback
        sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun skipNext(context: Context) {
        val mc = mediaController
        if (mc != null) {
            mc.transportControls.skipToNext()
            return
        }
        sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun skipPrevious(context: Context) {
        val mc = mediaController
        if (mc != null) {
            mc.transportControls.skipToPrevious()
            return
        }
        sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    private fun sendMediaKeyEvent(context: Context, keyCode: Int) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        } catch (_: Exception) {}
    }
}

class MediaNotificationService : NotificationListenerService() {
    private var sessionManager: MediaSessionManager? = null
    private var sessionListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var currentCallback: MediaController.Callback? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        MediaStateHolder.isListenerConnected = true
        setupSessionListener()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        MediaStateHolder.isListenerConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        checkActiveSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        checkActiveSessions()
    }

    private fun setupSessionListener() {
        try {
            sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val comp = ComponentName(this, MediaNotificationService::class.java)
            sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                findAndAttachMediaController(controllers)
            }
            sessionManager?.addOnActiveSessionsChangedListener(sessionListener!!, comp)
            checkActiveSessions()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkActiveSessions() {
        try {
            val comp = ComponentName(this, MediaNotificationService::class.java)
            val controllers = sessionManager?.getActiveSessions(comp)
            findAndAttachMediaController(controllers)
        } catch (_: Exception) {}
    }

    private fun findAndAttachMediaController(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            return
        }

        // Prioritaskan Spotify atau media yang sedang aktif playing
        val targetController = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull { it.packageName.contains("spotify", ignoreCase = true) }
            ?: controllers.first()

        attachController(targetController)
    }

    private fun attachController(controller: MediaController) {
        currentCallback?.let {
            try { MediaStateHolder.mediaController?.unregisterCallback(it) } catch (_: Exception) {}
        }

        MediaStateHolder.mediaController = controller
        MediaStateHolder.activePackage = controller.packageName
        updateFromController(controller)

        currentCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                updatePlaybackState(state)
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                updateMetadata(metadata)
            }

            override fun onSessionDestroyed() {
                super.onSessionDestroyed()
                checkActiveSessions()
            }
        }
        controller.registerCallback(currentCallback!!)
    }

    private fun updateFromController(controller: MediaController) {
        updateMetadata(controller.metadata)
        updatePlaybackState(controller.playbackState)
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        if (metadata != null) {
            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: "Spotify"
            val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR)
                ?: "Musik"
            val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
            val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

            val rawBmp: Bitmap? = try {
                metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            } catch (_: Exception) {
                null
            }

            MediaStateHolder.trackTitle = title
            MediaStateHolder.trackArtist = artist
            MediaStateHolder.albumTitle = album
            MediaStateHolder.trackDurationMs = if (duration > 0) duration else 0L
            MediaStateHolder.albumArtBitmap = rawBmp?.asImageBitmap()
            MediaStateHolder.hasActiveMedia = true
        }
    }

    private fun updatePlaybackState(state: PlaybackState?) {
        if (state != null) {
            val isPlaying = state.state == PlaybackState.STATE_PLAYING
            MediaStateHolder.isPlaying = isPlaying
            MediaStateHolder.trackPositionMs = state.position
            MediaStateHolder.hasActiveMedia = true
        } else {
            MediaStateHolder.isPlaying = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionListener?.let { sessionManager?.removeOnActiveSessionsChangedListener(it) }
        currentCallback?.let {
            try { MediaStateHolder.mediaController?.unregisterCallback(it) } catch (_: Exception) {}
        }
    }
}
