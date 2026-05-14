package heitezy.peekdisplay.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.preference.PreferenceManager
import heitezy.peekdisplay.actions.alwayson.AlwaysOn
import heitezy.peekdisplay.helpers.ColorHelper
import heitezy.peekdisplay.helpers.Global
import heitezy.peekdisplay.helpers.JSON
import heitezy.peekdisplay.helpers.Rules
import heitezy.peekdisplay.receivers.CombinedServiceReceiver
import org.json.JSONArray

class NotificationService : NotificationListenerService() {
    private var sentRecently: Boolean = false
    private var previousCount: Int = -1

    data class NotificationEntry(
        val icon: Icon,
        val color: Int,
        val contentIntent: PendingIntent?,
        val packageName: String,
        val id: Int,
        val tag: String?
    )

    interface OnNotificationsChangedListener {
        fun onNotificationsChanged()
    }

    override fun onCreate() {
        super.onCreate()
        activeService = this
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateValues()
    }

    override fun onDestroy() {
        super.onDestroy()
        activeService = null
    }

    override fun onNotificationPosted(notification: StatusBarNotification) {
        updateValues()

        val rules = Rules(this)
        @Suppress("ComplexCondition")
        if (
            isValidNotification(notification) &&
            !CombinedServiceReceiver.isScreenOn &&
            !CombinedServiceReceiver.isAlwaysOnRunning &&
            Rules.isAmbientMode(this) &&
            rules.canShow(this) &&
            count >= 1
        ) {
            startActivity(
                Intent(
                    this,
                    AlwaysOn::class.java,
                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    override fun onNotificationRemoved(notification: StatusBarNotification) {
        updateValues()

        if (
            CombinedServiceReceiver.isAlwaysOnRunning &&
            Rules.isAmbientMode(this) &&
            count < 1
        ) {
            AlwaysOn.finish()
        }
    }

    // Public method to force refresh notifications
    fun refreshNotifications() {
        // Reset sentRecently flag to allow immediate update
        sentRecently = false
        updateValues()
    }

    private fun updateValues() {
        if (sentRecently) return

        sentRecently = true
        try {
            val apps = ArrayList<String>(detailed.size)
            detailed = activeNotifications
            notifications = ArrayList(detailed.size)
            count = 0
            for (notification in detailed) {
                if (!isValidNotification(notification)) continue
                if (
                    notification.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0
                ) {
                    count++
                }
                if (!apps.contains(notification.packageName)) {
                    apps += notification.packageName
                    notifications.add(
                        NotificationEntry(
                            icon = notification.notification.smallIcon,
                            color = ColorHelper.boostColor(notification.notification.color),
                            contentIntent = notification.notification.contentIntent,
                            packageName = notification.packageName,
                            id = notification.id,
                            tag = notification.tag
                        )
                    )
                }
            }
        } catch (exception: SecurityException) {
            Log.e(Global.LOG_TAG, exception.toString())
            count = 0
            notifications = arrayListOf()
        }
        if (previousCount != count) {
            previousCount = count
            listeners.forEach { it.onNotificationsChanged() }
        }
        Handler(Looper.getMainLooper()).postDelayed({ sentRecently = false }, MINIMUM_UPDATE_DELAY)
    }

    private fun isValidNotification(notification: StatusBarNotification): Boolean =
        !notification.isOngoing &&
            !JSON.contains(
                JSONArray(
                    PreferenceManager.getDefaultSharedPreferences(this)
                        .getString("blocked_notifications", "[]"),
                ),
                notification.packageName,
            )

    companion object {
        const val MINIMUM_UPDATE_DELAY: Long = 1000
        internal var count: Int = 0
            private set
        internal var detailed: Array<StatusBarNotification> = arrayOf()
            private set
        internal var notifications: ArrayList<NotificationEntry> = arrayListOf()
            private set

        @JvmField
        internal val listeners: ArrayList<OnNotificationsChangedListener> = arrayListOf()

        internal var activeService: NotificationService? = null

        fun removeNotificationsByPackageAndId(packageName: String, id: Int, tag: String?) {
            if (activeService != null) {
                try {
                    val notificationToRemove = detailed.find {
                        it.packageName == packageName && it.id == id && it.tag == tag
                    }

                    if (notificationToRemove != null) {
                        activeService?.cancelNotification(notificationToRemove.key)
                    } else {
                        for (notification in detailed) {
                            if (notification.packageName == packageName) {
                                activeService?.cancelNotification(notification.key)
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(Global.LOG_TAG, "Failed to cancel notification: ${e.message}")
                }
            } else {
                Log.e(Global.LOG_TAG, "Cannot cancel notification - no active service instance")
            }
        }
    }
}
