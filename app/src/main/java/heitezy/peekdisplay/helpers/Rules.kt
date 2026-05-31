package heitezy.peekdisplay.helpers

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.util.Calendar
import android.os.BatteryManager
import heitezy.peekdisplay.R

class Rules(context: Context) {
    private var startMinutes = 0
    private var endMinutes = 0

    init {
        val prefs = P.getP(context)
        val startString = prefs.getString("rules_time_start", "0:00")
        val endString = prefs.getString("rules_time_end", "0:00")
        startMinutes =
            startString.substringBefore(":").toInt() * 60 +
                    startString.substringAfter(":").toInt()
        endMinutes =
            endString.substringBefore(":").toInt() * 60 +
                    endString.substringAfter(":").toInt()
    }

    private fun nowMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun isInTimePeriod(): Boolean {
        if (startMinutes == endMinutes) return true
        val now = nowMinutes()
        return if (startMinutes < endMinutes) {
            now in startMinutes..<endMinutes
        } else {
            now !in endMinutes..<startMinutes
        }
    }

    fun millisTillEnd(): Long {
        if (startMinutes == endMinutes) return -1
        val endCal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, endMinutes / 60)
            set(Calendar.MINUTE, endMinutes % 60)
        }
        if (endCal.timeInMillis <= System.currentTimeMillis()) {
            endCal.add(Calendar.DATE, 1)
        }
        return endCal.timeInMillis - System.currentTimeMillis()
    }

    fun canShow(context: Context): Boolean =
        isAlwaysOnDisplayEnabled(context) &&
            matchesDoNotDisturbState(context) &&
            matchesChargingState(context) &&
            matchesBatteryPercentage(context) &&
            isInTimePeriod()

    companion object {
        const val BATTERY_FULL: Int = 100

        private fun getBatteryStatus(c: Context): Intent? =
            IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                c.registerReceiver(
                    null,
                    filter,
                )
            }

        private fun isCharging(context: Context): Boolean {
            val chargingState: Int =
                getBatteryStatus(context)
                    ?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                    ?: return true
            return if (chargingState > 0) {
                P.getP(context).getStringSet(
                    "rules_charger_type",
                    context.resources.getStringArray(R.array.pref_look_and_feel_rules_charger_values)
                        .toSet(),
                ).contains(
                    when (chargingState) {
                        BatteryManager.BATTERY_PLUGGED_AC -> "ac"
                        BatteryManager.BATTERY_PLUGGED_USB -> "usb"
                        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
                        else -> ""
                    },
                )
            } else {
                false
            }
        }

        fun matchesDoNotDisturbState(context: Context): Boolean {
            val ruleDisableInDoNotDisturb =
                P.getP(context).getBoolean(
                    P.RULES_DISABLE_IN_DO_NOT_DISTURB,
                    P.RULES_DISABLE_IN_DO_NOT_DISTURB_DEFAULT,
                )
            if (!ruleDisableInDoNotDisturb) return true
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL
        }

        fun matchesChargingState(context: Context): Boolean {
            val ruleChargingState =
                P.getP(context).getString(
                    P.RULES_CHARGING_STATE,
                    P.RULES_CHARGING_STATE_DEFAULT,
                )
            if (ruleChargingState == P.RULES_CHARGING_STATE_DEFAULT) return true
            val charging = isCharging(context)
            return (ruleChargingState == P.RULES_CHARGING_STATE_CHARGING && charging) ||
                (ruleChargingState == P.RULES_CHARGING_STATE_DISCHARGING && !charging)
        }

        fun isAlwaysOnDisplayEnabled(context: Context): Boolean =
            P.getP(context).getBoolean(
                P.ALWAYS_ON,
                P.ALWAYS_ON_DEFAULT,
            )

        fun isAmbientMode(context: Context): Boolean =
            P.getP(context).getBoolean(
                "rules_ambient_mode",
                false,
            )

        fun isPickUpMode(context: Context): Boolean =
            P.getP(context).getBoolean(
                "rules_pickup_mode",
                false
            )

        fun matchesBatteryPercentage(context: Context): Boolean =
            (
                getBatteryStatus(context)?.getIntExtra(
                    BatteryManager.EXTRA_LEVEL,
                    0,
                ) ?: BATTERY_FULL
            ) > P.getP(context).getInt(P.RULES_BATTERY, P.RULES_BATTERY_DEFAULT)
    }
}
