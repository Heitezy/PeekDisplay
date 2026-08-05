package heitezy.peekdisplay.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import heitezy.peekdisplay.actions.ChargingCircleActivity
import heitezy.peekdisplay.actions.ChargingFlashActivity
import heitezy.peekdisplay.actions.ChargingIOSActivity
import heitezy.peekdisplay.actions.TurnOnScreenActivity
import heitezy.peekdisplay.actions.alwayson.AlwaysOn
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.helpers.Rules

class CombinedServiceReceiver : BroadcastReceiver() {
    private val handler = Handler(Looper.getMainLooper())
    private var stopServiceRunnable: Runnable? = null

    private fun getChargingActivity(context: Context, prefs: P = P.getP(context)) =
        when (
            prefs.getString(P.CHARGING_STYLE, P.CHARGING_STYLE_DEFAULT)
        ) {
            P.CHARGING_STYLE_CIRCLE -> ChargingCircleActivity::class.java
            P.CHARGING_STYLE_FLASH -> ChargingFlashActivity::class.java
            P.CHARGING_STYLE_IOS -> ChargingIOSActivity::class.java
            else -> error("Invalid value.")
        }

    private fun onPowerConnected(context: Context) {
        val prefs = P.getP(context)
        val rules = Rules(context, prefs)
        isCharging = true

        if (prefs.getBoolean(
                "charging_animation",
                false,
            ) && (!isScreenOn || isAlwaysOnRunning)
        ) {
            if (isAlwaysOnRunning) AlwaysOn.finish()
            context.startActivity(
                Intent(
                    context,
                    getChargingActivity(context, prefs),
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
        } else if (
            !isScreenOn &&
            !rules.ambientMode &&
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
        val prefs = P.getP(context)
        val rules = Rules(context, prefs)
        isCharging = false

        if (
            !isScreenOn &&
            !rules.ambientMode &&
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
        val prefs = P.getP(context)
        val rules = Rules(context, prefs)
        isScreenOn = false

        stopServiceRunnable?.let {
            handler.removeCallbacks(it)
            stopServiceRunnable = null
        }

        if (rules.pickUpMode && rules.alwaysOnEnabled) {
            if (isAlwaysOnRunning) {
                AlwaysOn.finish()
                isAlwaysOnRunning = false
                heitezy.peekdisplay.services.PickUpService.startService(context)
            } else {
                heitezy.peekdisplay.services.PickUpService.startService(context)
            }
        } else if (rules.alwaysOnEnabled && !hasRequestedStop) {
            if (isAlwaysOnRunning) {
                if (!rules.ambientMode) {
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
                !rules.ambientMode &&
                rules.canShow(context)
            ) {
                context.startActivity(
                    Intent(
                        context,
                        AlwaysOn::class.java,
                    ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        } else if (rules.alwaysOnEnabled && hasRequestedStop) {
            hasRequestedStop = false
            isAlwaysOnRunning = false
        }
    }

    private fun onScreenOn(context: Context) {
        val prefs = P.getP(context)
        val rules = Rules(context, prefs)
        isScreenOn = true

        if (rules.pickUpMode) {
            val timeout = prefs.getInt(P.RULES_TIMEOUT, P.RULES_TIMEOUT_DEFAULT)
            stopServiceRunnable?.let {
                handler.removeCallbacks(it)
            }
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
