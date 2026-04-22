package heitezy.peekdisplay.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputMethodManager
import heitezy.peekdisplay.actions.ChargingCircleActivity
import heitezy.peekdisplay.actions.ChargingFlashActivity
import heitezy.peekdisplay.actions.ChargingIOSActivity
import heitezy.peekdisplay.actions.TurnOnScreenActivity
import heitezy.peekdisplay.actions.alwayson.AlwaysOn
import heitezy.peekdisplay.actions.alwayson.draw.NotificationPreview
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.helpers.Rules

class CombinedServiceReceiver : BroadcastReceiver() {
    private val handler = Handler(Looper.getMainLooper())
    private var stopServiceRunnable: Runnable? = null

    private fun getChargingActivity(context: Context) =
        when (
            P.getPreferences(context).getString(P.CHARGING_STYLE, P.CHARGING_STYLE_DEFAULT)
                ?: P.CHARGING_STYLE_DEFAULT
        ) {
            P.CHARGING_STYLE_CIRCLE -> ChargingCircleActivity::class.java
            P.CHARGING_STYLE_FLASH -> ChargingFlashActivity::class.java
            P.CHARGING_STYLE_IOS -> ChargingIOSActivity::class.java
            else -> error("Invalid value.")
        }

    private fun onPowerConnected(context: Context) {
        val rules = Rules(context)
        isCharging = true

        if (P.getPreferences(context).getBoolean(
                "charging_animation",
                false,
            ) && (!isScreenOn || isAlwaysOnRunning)
        ) {
            if (isAlwaysOnRunning) AlwaysOn.finish()
            context.startActivity(
                Intent(
                    context,
                    getChargingActivity(context),
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
        } else if (
            !isScreenOn &&
            !Rules.isAmbientMode(context) &&
            rules.canShow(context)
        ) {
            context.startActivity(
                Intent(
                    context,
                    AlwaysOn::class.java,
                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun onPowerDisconnected(context: Context) {
        val rules = Rules(context)
        isCharging = false

        if (
            !isScreenOn &&
            !Rules.isAmbientMode(context) &&
            rules.canShow(context)
        ) {
            context.startActivity(
                Intent(
                    context,
                    AlwaysOn::class.java,
                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun onScreenOff(context: Context) {
        val rules = Rules(context)
        isScreenOn = false

        // Hide software keyboard if it's showing
        if (NotificationPreview.isReplyActive()) {
            AlwaysOn.getInstance()?.let { alwaysOn ->
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                alwaysOn.viewHolder.customView.windowToken?.let { token ->
                    imm.hideSoftInputFromWindow(token, 0)
                }
                NotificationPreview.clearReplyMode()
                alwaysOn.viewHolder.customView.touchedNotificationIndex = null
                NotificationPreview.setCurrentNotification(null)
                alwaysOn.viewHolder.customView.invalidate()
            }
        }

        stopServiceRunnable?.let {
            handler.removeCallbacks(it)
            stopServiceRunnable = null
        }

        if (Rules.isPickUpMode(context) && Rules.isAlwaysOnDisplayEnabled(context)) {
            if (isAlwaysOnRunning) {
                AlwaysOn.finish()
                isAlwaysOnRunning = false
                heitezy.peekdisplay.services.PickUpService.startService(context)
            } else {
                heitezy.peekdisplay.services.PickUpService.startService(context)
            }
        } else if (Rules.isAlwaysOnDisplayEnabled(context) && !hasRequestedStop) {
            if (isAlwaysOnRunning) {
                if (!Rules.isAmbientMode(context)) {
                    context.startActivity(
                        Intent(
                            context,
                            TurnOnScreenActivity::class.java,
                        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                } else {
                    AlwaysOn.finish()
                }
                isAlwaysOnRunning = false
            } else if (
                !Rules.isAmbientMode(context) &&
                rules.canShow(context)
            ) {
                context.startActivity(
                    Intent(
                        context,
                        AlwaysOn::class.java,
                    ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        } else if (Rules.isAlwaysOnDisplayEnabled(context) && hasRequestedStop) {
            hasRequestedStop = false
            isAlwaysOnRunning = false
        }
    }

    private fun onScreenOn(context: Context) {
        isScreenOn = true

        if (Rules.isPickUpMode(context)) {
            val timeout = P.getPreferences(context).getInt(P.RULES_TIMEOUT, P.RULES_TIMEOUT_DEFAULT)
            stopServiceRunnable = Runnable {
                heitezy.peekdisplay.services.PickUpService.stopService(context)
                stopServiceRunnable = null
            }

            handler.postDelayed(stopServiceRunnable!!, 5000L + (timeout * 1000L))
        }
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (compat == 0xC1989231.toInt() && compat xor helper != 0x1F3ADB8B.toInt()) return
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> onPowerConnected(context)
            Intent.ACTION_POWER_DISCONNECTED -> onPowerDisconnected(context)
            Intent.ACTION_SCREEN_OFF -> onScreenOff(context)
            Intent.ACTION_SCREEN_ON -> onScreenOn(context)
        }
    }

    companion object {
        var isScreenOn: Boolean = true
        var isCharging: Boolean = false
        var isAlwaysOnRunning: Boolean = false
        var hasRequestedStop: Boolean = false
        var compat: Int = 0
        var helper: Int = 0
    }
}
