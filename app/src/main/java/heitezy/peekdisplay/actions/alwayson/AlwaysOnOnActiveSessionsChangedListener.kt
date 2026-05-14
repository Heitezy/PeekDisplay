package heitezy.peekdisplay.actions.alwayson

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import heitezy.peekdisplay.helpers.Global

class AlwaysOnOnActiveSessionsChangedListener(
    var onMediaStateChanged: ((String, Bitmap?) -> Unit)? = null
) : MediaSessionManager.OnActiveSessionsChangedListener {
    @JvmField
    internal var controller: MediaController? = null

    @JvmField
    internal var state: Int = 0

    override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
        try {
            controller = controllers?.firstOrNull()
            state = controller?.playbackState?.state ?: 0
            updateMediaState()
            controller?.registerCallback(
                object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(playbackState: PlaybackState?) {
                        super.onPlaybackStateChanged(playbackState)
                        state = playbackState?.state ?: 0
                    }

                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        super.onMetadataChanged(metadata)
                        updateMediaState()
                    }
                },
            )
        } catch (e: java.lang.Exception) {
            Log.e(Global.LOG_TAG, e.toString())
        }
    }

    internal fun updateMediaState() {
        if (controller != null) {
            var artist = controller?.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
            var title = controller?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
            
            // Fetch album art
            val albumArt = controller?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: controller?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            
            var musicString = ""
            if (artist.length > MAX_STRING_LENGTH) {
                artist = artist.substring(0, MAX_STRING_LENGTH - 1) + '…'
            }
            if (title.length > MAX_STRING_LENGTH) {
                title = title.substring(0, MAX_STRING_LENGTH - 1) + '…'
            }
            when {
                artist == "" -> musicString = title
                title == "" -> musicString = artist
                else -> musicString = "$artist - $title"
            }
            onMediaStateChanged?.invoke(musicString, albumArt)
        } else {
            onMediaStateChanged?.invoke("", null)
        }
    }

    companion object {
        private const val MAX_STRING_LENGTH = 20
    }
}
