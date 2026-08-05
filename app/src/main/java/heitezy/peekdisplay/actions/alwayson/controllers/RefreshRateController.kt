package heitezy.peekdisplay.actions.alwayson.controllers

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Window

class RefreshRateController(
    private val window: Window,
    private val context: Context
) {
    fun setRefreshRate(low: Boolean) {
        val params = window.attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val rate = if (low) getLowestRefreshRate() else 0f
            if (params.preferredRefreshRate != rate) {
                params.preferredRefreshRate = rate
                window.attributes = params
            }
        } else {
            val modeId = if (low) getLowestRefreshRateMode() else 0
            if (params.preferredDisplayModeId != modeId) {
                params.preferredDisplayModeId = modeId
                window.attributes = params
            }
        }
    }

    private fun getLowestRefreshRate(): Float {
        val display = getDisplay()
        val modes = display?.supportedModes
        if (!modes.isNullOrEmpty()) {
            val currentMode = display.mode
            var lowestRate = currentMode.refreshRate

            for (mode in modes) {
                if (mode.physicalWidth == currentMode.physicalWidth &&
                    mode.physicalHeight == currentMode.physicalHeight
                ) {
                    if (mode.refreshRate < lowestRate) {
                        lowestRate = mode.refreshRate
                    }
                }
            }
            return lowestRate
        }
        return 0f
    }

    private fun getLowestRefreshRateMode(): Int {
        val display = getDisplay()
        val modes = display?.supportedModes
        if (!modes.isNullOrEmpty()) {
            val currentMode = display.mode
            var lowestRate = Float.MAX_VALUE
            var lowestModeId = currentMode.modeId

            for (mode in modes) {
                if (mode.physicalWidth == currentMode.physicalWidth &&
                    mode.physicalHeight == currentMode.physicalHeight
                ) {
                    if (mode.refreshRate < lowestRate) {
                        lowestRate = mode.refreshRate
                        lowestModeId = mode.modeId
                    }
                }
            }
            return lowestModeId
        }
        return 0
    }

    private fun getDisplay(): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.display
            } catch (_: Exception) {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                    .getDisplay(Display.DEFAULT_DISPLAY)
            }
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                .getDisplay(Display.DEFAULT_DISPLAY)
        }
    }
}
