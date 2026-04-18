package heitezy.peekdisplay.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import heitezy.peekdisplay.actions.alwayson.AlwaysOn
import heitezy.peekdisplay.helpers.Rules
import heitezy.peekdisplay.receivers.CombinedServiceReceiver.Companion.isAlwaysOnRunning
import heitezy.peekdisplay.services.NotificationService

class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val currentPhoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            ?: return

        when (currentPhoneState) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                previousPhoneState = TelephonyManager.EXTRA_STATE_RINGING
                AlwaysOn.finish()
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                previousPhoneState = TelephonyManager.EXTRA_STATE_OFFHOOK
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (previousPhoneState == TelephonyManager.EXTRA_STATE_RINGING) {
                    if (!isAlwaysOnRunning) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            NotificationService.activeService?.refreshNotifications()
                            if (Rules.isAmbientMode(context)) {
                                context.startActivity(
                                    Intent(context, AlwaysOn::class.java)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }, 1500L) // give system time to post missed call notification
                    }
                }
                previousPhoneState = TelephonyManager.EXTRA_STATE_IDLE
            }
        }
    }

    companion object {
        private var previousPhoneState: String? = null
    }
}
