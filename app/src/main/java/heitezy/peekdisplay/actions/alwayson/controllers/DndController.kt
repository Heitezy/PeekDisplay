package heitezy.peekdisplay.actions.alwayson.controllers

import android.app.NotificationManager
import android.content.Context

class DndController(context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var userDND: Int = NotificationManager.INTERRUPTION_FILTER_ALL
    private val hasAccess: Boolean get() = notificationManager.isNotificationPolicyAccessGranted

    fun captureUserDnd() {
        if (hasAccess) {
            val current = notificationManager.currentInterruptionFilter
            if (current != NotificationManager.INTERRUPTION_FILTER_NONE) {
                userDND = current
            }
        }
    }

    fun enableAodDnd() {
        if (hasAccess) {
            userDND = notificationManager.currentInterruptionFilter
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
    }

    fun restoreUserDnd() {
        if (hasAccess) {
            notificationManager.setInterruptionFilter(userDND)
        }
    }
}
