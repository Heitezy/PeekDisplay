package heitezy.peekdisplay.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import heitezy.peekdisplay.R
import heitezy.peekdisplay.helpers.P
import androidx.core.content.edit
import heitezy.peekdisplay.ui.ColorPickerDialog
import heitezy.peekdisplay.ui.ColorPreferenceItem
import heitezy.peekdisplay.ui.PeekScaffold

class LAFWFColorsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                LAFWFColorsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun LAFWFColorsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { P.getPreferences(context) }

    val colorStates = remember {
        mutableStateMapOf<String, Int>().apply {
            COLOR_PREFS.forEach { spec ->
                put(spec.key, prefs.getInt(spec.key, spec.defaultColor))
            }
        }
    }

    var activeSpec by remember { mutableStateOf<ColorPrefSpec?>(null) }

    PeekScaffold(
        title = stringResource(R.string.pref_look_and_feel_colors),
        onBack = onBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues),
        ) {
            COLOR_PREFS.forEach { spec ->
                ColorPreferenceItem(
                    iconRes = spec.iconRes,
                    title = stringResource(spec.titleRes),
                    color = Color(colorStates[spec.key] ?: spec.defaultColor),
                    onClick = { activeSpec = spec },
                )
            }
        }
    }

    activeSpec?.let { spec ->
        ColorPickerDialog(
            initialColor = colorStates[spec.key] ?: spec.defaultColor,
            showAlpha = true,
            title = stringResource(spec.titleRes),
            onColorSelected = { picked ->
                prefs.edit { putInt(spec.key, picked) }
                colorStates[spec.key] = picked
                activeSpec = null
            },
            onDismiss = { activeSpec = null },
        )
    }
}

private data class ColorPrefSpec(
    val key: String,
    val defaultColor: Int,
    @param:DrawableRes val iconRes: Int,
    val titleRes: Int,
)

private val COLOR_PREFS = listOf(
    ColorPrefSpec(
        P.DISPLAY_COLOR_CLOCK,
        P.DISPLAY_COLOR_CLOCK_DEFAULT,
        R.drawable.ic_clock,
        R.string.pref_look_and_feel_colors_clock
    ),
    ColorPrefSpec(
        P.DISPLAY_COLOR_DATE,
        P.DISPLAY_COLOR_DATE_DEFAULT,
        R.drawable.ic_date,
        R.string.pref_look_and_feel_colors_date
    ),
    ColorPrefSpec(
        P.DISPLAY_COLOR_BATTERY,
        P.DISPLAY_COLOR_BATTERY_DEFAULT,
        R.drawable.ic_battery,
        R.string.pref_look_and_feel_colors_battery
    ),
    ColorPrefSpec(
        P.DISPLAY_COLOR_BATTERY_ARC,
        P.DISPLAY_COLOR_BATTERY_ARC_DEFAULT,
        R.drawable.ic_battery,
        R.string.pref_look_and_feel_colors_battery_arc
    ),
    ColorPrefSpec(
        P.DISPLAY_COLOR_MUSIC_CONTROLS,
        P.DISPLAY_COLOR_MUSIC_CONTROLS_DEFAULT,
        R.drawable.ic_music_note,
        R.string.pref_look_and_feel_colors_music_controls
    ),
    ColorPrefSpec(
        P.DISPLAY_COLOR_CALENDAR,
        P.DISPLAY_COLOR_CALENDAR_DEFAULT,
        R.drawable.ic_date,
        R.string.pref_look_and_feel_colors_calendar
    ),
    ColorPrefSpec(
        P.DISPLAY_COLOR_MESSAGE,
        P.DISPLAY_COLOR_MESSAGE_DEFAULT,
        R.drawable.ic_short_text,
        R.string.pref_look_and_feel_colors_message
    ),
    ColorPrefSpec(
        P.DISPLAY_COLOR_WEATHER,
        P.DISPLAY_COLOR_WEATHER_DEFAULT,
        R.drawable.ic_cloud,
        R.string.pref_look_and_feel_colors_weather
    ),
    ColorPrefSpec(
        P.DISPLAY_COLOR_NOTIFICATION,
        P.DISPLAY_COLOR_NOTIFICATION_DEFAULT,
        R.drawable.ic_notification,
        R.string.pref_look_and_feel_colors_notification
    ),
    ColorPrefSpec(
        P.DISPLAY_COLOR_FINGERPRINT,
        P.DISPLAY_COLOR_FINGERPRINT_DEFAULT,
        R.drawable.ic_fingerprint,
        R.string.pref_look_and_feel_colors_fingerprint
    ),
)