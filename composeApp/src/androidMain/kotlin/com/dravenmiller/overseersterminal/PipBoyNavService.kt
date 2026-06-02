package com.dravenmiller.overseersterminal

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.dravenmiller.overseersterminal.components.MediaState
import com.dravenmiller.overseersterminal.components.NavState
import kotlinx.coroutines.*

class PipBoyNavService : NotificationListenerService() {

    private var audioScannerJob: Job? = null

    // When the Pip-Boy Service successfully connects to the Android Kernel...
    override fun onListenerConnected() {
        super.onListenerConnected()

        val mediaManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(this, PipBoyNavService::class.java)

        // Start a radar loop that scans the audio chip every 1 second!
        audioScannerJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // Ask the OS: "Who is currently holding the audio microphone?"
                    val controllers = mediaManager.getActiveSessions(component)

                    if (controllers.isNotEmpty()) {
                        val activeController = controllers[0]
                        val metadata = activeController.metadata
                        val pbState = activeController.playbackState

                        // Beam the exact Song and Time to the UI!
                        MediaState.trackTitle.value = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "UNKNOWN FREQUENCY"
                        MediaState.trackArtist.value = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "UNKNOWN ARTIST"
                        MediaState.trackDuration.value = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                        MediaState.currentPosition.value = pbState?.position ?: 0L
                    } else {
                        MediaState.trackTitle.value = "NO ACTIVE SIGNAL"
                        MediaState.trackArtist.value = ""
                        MediaState.currentPosition.value = 0L
                        MediaState.trackDuration.value = 0L
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(1000) // Wait 1 second and scan again
            }
        }
    }

    override fun onListenerDisconnected() {
        audioScannerJob?.cancel()
        super.onListenerDisconnected()
    }

    // (Keep your existing Navigation 'onNotificationPosted' code right here!)
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.packageName == "com.google.android.apps.maps") {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

            if (title != null) {
                NavState.currentDirection.value = title
                if (text == "null" || text.isNullOrEmpty()) {
                    NavState.currentDistance.value = null
                } else {
                    NavState.currentDistance.value = text
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn?.packageName == "com.google.android.apps.maps") {
            NavState.currentDirection.value = null
            NavState.currentDistance.value = null
        }
    }
}
