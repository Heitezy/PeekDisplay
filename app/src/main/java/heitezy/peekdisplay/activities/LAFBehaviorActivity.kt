package heitezy.peekdisplay.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import heitezy.peekdisplay.R
import heitezy.peekdisplay.ui.EditTextDialog
import heitezy.peekdisplay.ui.PeekScaffold
import heitezy.peekdisplay.ui.PreferenceItem
import heitezy.peekdisplay.ui.RadioButtonDialog
import heitezy.peekdisplay.ui.SwitchPreferenceItem
import heitezy.peekdisplay.helpers.P
import androidx.core.content.edit

class LAFBehaviorActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                LAFBehaviorScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun LAFBehaviorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { P.getPreferences(context) }
    val isRootMode = remember { prefs.getBoolean(P.ROOT_MODE, P.ROOT_MODE_DEFAULT) }

    var orientation by remember {
        mutableStateOf(prefs.getString("orientation", "locked") ?: "locked")
    }
    var showOrientationDialog by remember { mutableStateOf(false) }

    var vibration by remember { mutableIntStateOf(prefs.getInt(P.VIBRATION_DURATION, 64)) }
    var showVibrationDialog by remember { mutableStateOf(false) }

    var animationDelay by remember { mutableIntStateOf(prefs.getInt("ao_animation_delay", 2)) }
    var showAnimationDelayDialog by remember { mutableStateOf(false) }

    var doubleTapSpeed by remember { mutableIntStateOf(prefs.getInt(P.DOUBLE_TAP_SPEED, 300)) }
    var showDoubleTapSpeedDialog by remember { mutableStateOf(false) }

    var smoothAnimation by remember {
        mutableStateOf(
            prefs.getBoolean(
                P.ANIMATE_MOTION,
                false
            )
        )
    }
    var doubleTapDisabled by remember {
        mutableStateOf(
            prefs.getBoolean(
                P.DISABLE_DOUBLE_TAP,
                false
            )
        )
    }
    var pocketMode by remember { mutableStateOf(prefs.getBoolean(P.POCKET_MODE, false)) }
    var dnd by remember { mutableStateOf(prefs.getBoolean(P.DO_NOT_DISTURB, false)) }
    var powerSaving by remember {
        mutableStateOf(
            prefs.getBoolean(
                P.POWER_SAVING_MODE,
                P.POWER_SAVING_MODE_DEFAULT
            )
        )
    }
    var headsUp by remember {
        mutableStateOf(
            prefs.getBoolean(
                P.DISABLE_HEADS_UP_NOTIFICATIONS,
                P.DISABLE_HEADS_UP_NOTIFICATIONS_DEFAULT
            )
        )
    }

    val orientationEntries =
        stringArrayResource(R.array.pref_look_and_feel_orientation_display).toList()
    val orientationValues =
        stringArrayResource(R.array.pref_look_and_feel_orientation_values).toList()

    if (showOrientationDialog) {
        RadioButtonDialog(
            title = stringResource(R.string.pref_look_and_feel_orientation),
            entries = orientationEntries,
            entryValues = orientationValues,
            selectedValue = orientation,
            onValueSelected = { value ->
                orientation = value
                prefs.edit { putString("orientation", value) }
            },
            onDismiss = { showOrientationDialog = false },
        )
    }

    if (showVibrationDialog) {
        EditTextDialog(
            title = stringResource(R.string.pref_ao_vibration),
            current = vibration.toString(),
            keyboardType = KeyboardType.Number,
            onConfirm = { raw ->
                raw.toIntOrNull()?.let { newValue ->
                    vibration = newValue
                    prefs.edit { putInt(P.VIBRATION_DURATION, newValue) }
                }
                showVibrationDialog = false
            },
            onDismiss = { showVibrationDialog = false },
            validate = { it.toIntOrNull() != null },
        )
    }

    if (showAnimationDelayDialog) {
        EditTextDialog(
            title = stringResource(R.string.pref_ao_animation_delay),
            current = animationDelay.toString(),
            keyboardType = KeyboardType.Number,
            onConfirm = { raw ->
                raw.toIntOrNull()?.let { newValue ->
                    animationDelay = newValue
                    prefs.edit { putInt("ao_animation_delay", newValue) }
                }
                showAnimationDelayDialog = false
            },
            onDismiss = { showAnimationDelayDialog = false },
            validate = { it.toIntOrNull() != null },
        )
    }

    if (showDoubleTapSpeedDialog) {
        EditTextDialog(
            title = stringResource(R.string.pref_ao_double_tap_speed),
            current = doubleTapSpeed.toString(),
            keyboardType = KeyboardType.Number,
            onConfirm = { raw ->
                raw.toIntOrNull()?.let { newValue ->
                    doubleTapSpeed = newValue
                    prefs.edit { putInt(P.DOUBLE_TAP_SPEED, newValue) }
                }
                showDoubleTapSpeedDialog = false
            },
            onDismiss = { showDoubleTapSpeedDialog = false },
            validate = { it.toIntOrNull() != null },
        )
    }

    PeekScaffold(
        title = stringResource(R.string.pref_ao_behavior),
        onBack = onBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // Orientation
            PreferenceItem(
                iconRes = R.drawable.ic_orientation,
                title = stringResource(R.string.pref_look_and_feel_orientation),
                summary = orientationEntries.getOrElse(orientationValues.indexOf(orientation)) {
                    stringResource(R.string.pref_look_and_feel_orientation_summary)
                },
                onClick = { showOrientationDialog = true },
            )

            // Vibration
            PreferenceItem(
                iconRes = R.drawable.ic_vibration,
                title = stringResource(R.string.pref_ao_vibration),
                summary = stringResource(R.string.pref_ao_vibration_summary),
                onClick = { showVibrationDialog = true },
            )

            // Animation delay
            PreferenceItem(
                iconRes = R.drawable.ic_speed,
                title = stringResource(R.string.pref_ao_animation_delay),
                summary = stringResource(R.string.pref_ao_animation_delay_summary),
                onClick = { showAnimationDelayDialog = true },
            )

            // Smooth animation
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_speed,
                title = stringResource(R.string.pref_ao_smooth_animation),
                summary = stringResource(R.string.pref_ao_smooth_animation_summary),
                checked = smoothAnimation,
                onCheckedChange = { checked ->
                    smoothAnimation = checked
                    prefs.edit { putBoolean(P.ANIMATE_MOTION, checked) }
                },
            )

            // Force brightness
            PreferenceItem(
                iconRes = R.drawable.ic_brightness,
                title = stringResource(R.string.pref_ao_force_brightness),
                summary = stringResource(R.string.pref_ao_force_brightness_summary),
                onClick = {
                    context.startActivity(
                        Intent(
                            context,
                            LAFBrightnessActivity::class.java
                        )
                    )
                },
            )

            // Double-tap speed
            PreferenceItem(
                iconRes = R.drawable.ic_touch_app,
                title = stringResource(R.string.pref_ao_double_tap_speed),
                summary = stringResource(R.string.pref_ao_double_tap_speed_summary),
                onClick = { showDoubleTapSpeedDialog = true },
            )

            // Disable double-tap
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_touch_app,
                title = stringResource(R.string.pref_ao_double_tap_disabled),
                summary = stringResource(R.string.pref_ao_double_tap_disabled_summary),
                checked = doubleTapDisabled,
                onCheckedChange = { checked ->
                    doubleTapDisabled = checked
                    prefs.edit { putBoolean(P.DISABLE_DOUBLE_TAP, checked) }
                },
            )

            // Pocket mode
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_do_not_touch,
                title = stringResource(R.string.pref_ao_pocket_mode),
                summary = stringResource(R.string.pref_ao_pocket_mode_summary),
                checked = pocketMode,
                onCheckedChange = { checked ->
                    pocketMode = checked
                    prefs.edit { putBoolean(P.POCKET_MODE, checked) }
                },
            )

            // DND
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_dnd,
                title = stringResource(R.string.pref_ao_dnd),
                summary = stringResource(R.string.pref_ao_dnd_summary),
                checked = dnd,
                onCheckedChange = { checked ->
                    dnd = checked
                    prefs.edit { putBoolean(P.DO_NOT_DISTURB, checked) }
                },
            )

            // Power saving
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_battery,
                title = stringResource(R.string.pref_ao_power_saving),
                summary = stringResource(R.string.pref_ao_power_saving_summary),
                checked = powerSaving,
                enabled = isRootMode,
                onCheckedChange = { checked ->
                    powerSaving = checked
                    prefs.edit { putBoolean(P.POWER_SAVING_MODE, checked) }
                },
            )

            // Disable heads-up notifications
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_notification,
                title = stringResource(R.string.pref_look_and_feel_heads_up),
                summary = stringResource(R.string.pref_look_and_feel_heads_up_summary),
                checked = headsUp,
                enabled = isRootMode,
                onCheckedChange = { checked ->
                    headsUp = checked
                    prefs.edit { putBoolean(P.DISABLE_HEADS_UP_NOTIFICATIONS, checked) }
                },
            )
        }
    }
}