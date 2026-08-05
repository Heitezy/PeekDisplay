package heitezy.peekdisplay.actions.alwayson.controllers

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import heitezy.peekdisplay.actions.alwayson.AlwaysOnOnActiveSessionsChangedListener
import heitezy.peekdisplay.services.NotificationService

class MediaControllerWrapper(
    context: Context,
    private val onMediaStateChanged: (String, Bitmap?, Int) -> Unit
) {
    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val notificationListener = ComponentName(context, NotificationService::class.java)
    
    private var onActiveSessionsChangedListener: AlwaysOnOnActiveSessionsChangedListener? = null

    val controller: MediaController? get() = onActiveSessionsChangedListener?.controller
    val playbackState: Int get() = onActiveSessionsChangedListener?.state ?: 0

    fun start() {
        onActiveSessionsChangedListener = AlwaysOnOnActiveSessionsChangedListener().apply {
            this.onMediaStateChanged = this@MediaControllerWrapper.onMediaStateChanged
        }
        
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                onActiveSessionsChangedListener!!,
                notificationListener
            )
            onActiveSessionsChangedListener?.onActiveSessionsChanged(
                mediaSessionManager.getActiveSessions(notificationListener)
            )
        } catch (_: SecurityException) {
            // Error handling should be done via onMediaStateChanged if needed
        }
    }

    fun stop() {
        onActiveSessionsChangedListener?.let {
            try {
                mediaSessionManager.removeOnActiveSessionsChangedListener(it)
            } catch (_: Exception) {}
        }
    }
}
