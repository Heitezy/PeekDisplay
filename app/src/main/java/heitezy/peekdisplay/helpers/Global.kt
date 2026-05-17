package heitezy.peekdisplay.helpers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import heitezy.peekdisplay.actions.alwayson.AlwaysOn
import heitezy.peekdisplay.receivers.AlwaysOnAppWidgetProvider
import heitezy.peekdisplay.services.AlwaysOnTileService
import heitezy.peekdisplay.services.ForegroundService
import heitezy.peekdisplay.services.PickUpService

internal object Global {
    private var previousAmbientModeState: Boolean = false
    private var previousPickupModeState: Boolean = false

    const val LOG_TAG: String = "PeekDisplay"

    const val ALWAYS_ON_STATE_CHANGED: String =
        "heitezy.peekdisplay.ALWAYS_ON_STATE_CHANGED"

    fun currentAlwaysOnState(context: Context): Boolean =
        getDefaultSharedPreferences(context)
            .getBoolean(P.ALWAYS_ON, P.ALWAYS_ON_DEFAULT)

    fun changeAlwaysOnState(context: Context): Boolean {
        val prefs = getDefaultSharedPreferences(context)
        val value = !prefs.getBoolean(P.ALWAYS_ON, P.ALWAYS_ON_DEFAULT)

        prefs.edit { putBoolean(P.ALWAYS_ON, value) }

        TileService.requestListeningState(
            context,
            ComponentName(context, AlwaysOnTileService::class.java),
        )
        context.sendBroadcast(
            Intent(context, AlwaysOnAppWidgetProvider::class.java)
                .setAction(ALWAYS_ON_STATE_CHANGED),
        )
        
        if (value) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ForegroundService::class.java)
            )

            prefs.edit {
                putBoolean("rules_ambient_mode", previousAmbientModeState)
                putBoolean("rules_pickup_mode", previousPickupModeState)
            }
        } else {
            previousAmbientModeState = prefs.getBoolean("rules_ambient_mode", false)
            previousPickupModeState = prefs.getBoolean("rules_pickup_mode", false)

            prefs.edit {
                putBoolean("rules_ambient_mode", false)
                putBoolean("rules_pickup_mode", false)
            }

            context.stopService(Intent(context, ForegroundService::class.java))
            PickUpService.stopService(context)
            AlwaysOn.finish()
        }
        
        return value
    }
}
