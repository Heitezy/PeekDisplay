package heitezy.peekdisplay.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import heitezy.peekdisplay.R
import heitezy.peekdisplay.ui.ColorPickerDialog
import heitezy.peekdisplay.ui.ColorPreferenceItem
import heitezy.peekdisplay.ui.EditTextDialog
import heitezy.peekdisplay.ui.PeekScaffold
import heitezy.peekdisplay.ui.PreferenceDivider
import heitezy.peekdisplay.ui.PreferenceItem
import heitezy.peekdisplay.ui.RadioButtonDialog
import heitezy.peekdisplay.ui.SwitchPreferenceItem
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.helpers.Permissions

class LAFBackgroundActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                BackgroundSettingsScreen(
                    onBack = { finish() },
                    onNavigateToImagePicker = {
                        startActivity(Intent(this, LAFBackgroundImageActivity::class.java))
                    },
                )
            }
        }
    }
}

@Composable
private fun BackgroundSettingsScreen(
    onBack: () -> Unit,
    onNavigateToImagePicker: () -> Unit,
) {
    val context = LocalContext.current

    val prefs = remember { P.getP(context) }

    val hasNotificationPermission = Permissions.isNotificationServiceEnabled(context)

    val hideDisplayCutouts by prefs.getBooleanFlow("hide_display_cutouts", false)
        .collectAsState(initial = prefs.getBoolean("hide_display_cutouts", false))
    
    val edgeGlowEnabled by prefs.getBooleanFlow(P.EDGE_GLOW, false)
        .collectAsState(initial = prefs.getBoolean(P.EDGE_GLOW, false))
    
    val glowDuration by prefs.getIntFlow(P.EDGE_GLOW_DURATION, 2000)
        .collectAsState(initial = prefs.getInt(P.EDGE_GLOW_DURATION, 2000))
    
    val glowDelay by prefs.getIntFlow(P.EDGE_GLOW_DELAY, 4000)
        .collectAsState(initial = prefs.getInt(P.EDGE_GLOW_DELAY, 4000))
    
    val glowStyle by prefs.getStringFlow(P.EDGE_GLOW_STYLE, "all")
        .collectAsState(initial = prefs.getString(P.EDGE_GLOW_STYLE, "all"))
    
    val glowColor by prefs.getIntFlow(P.DISPLAY_COLOR_EDGE_GLOW, Color.White.toArgb())
        .collectAsState(initial = prefs.getInt(P.DISPLAY_COLOR_EDGE_GLOW, Color.White.toArgb()))

    var showDurationDialog by remember { mutableStateOf(false) }
    var showDelayDialog    by remember { mutableStateOf(false) }
    var showStyleDialog    by remember { mutableStateOf(false) }
    var showColorDialog    by remember { mutableStateOf(false) }

    if (showDurationDialog) {
        EditTextDialog(
            title = stringResource(R.string.pref_ao_glowDuration),
            current = glowDuration.toString(),
            label = stringResource(R.string.pref_ao_glowDuration_summary),
            keyboardType = KeyboardType.Number,
            validate = { it.toIntOrNull() != null },
            onConfirm = { value ->
                val ms = value.toIntOrNull() ?: glowDuration
                prefs.edit { putInt(P.EDGE_GLOW_DURATION, ms) }
                showDurationDialog = false
            },
            onDismiss = { showDurationDialog = false },
        )
    }

    if (showDelayDialog) {
        EditTextDialog(
            title = stringResource(R.string.pref_ao_glowDelay),
            current = glowDelay.toString(),
            label = stringResource(R.string.pref_ao_glowDelay_summary),
            keyboardType = KeyboardType.Number,
            validate = { it.toIntOrNull() != null },
            onConfirm = { value ->
                val ms = value.toIntOrNull() ?: glowDelay
                prefs.edit { putInt(P.EDGE_GLOW_DELAY, ms) }
                showDelayDialog = false
            },
            onDismiss = { showDelayDialog = false },
        )
    }

    if (showStyleDialog) {
        val entries      = stringArrayResource(R.array.pref_ao_glowStyle_display).toList()
        val entryValues  = stringArrayResource(R.array.pref_ao_glowStyle_values).toList()
        RadioButtonDialog(
            title = stringResource(R.string.pref_ao_glowStyle),
            entries = entries,
            entryValues = entryValues,
            selectedValue = glowStyle,
            onValueSelected = { value ->
                prefs.edit { putString(P.EDGE_GLOW_STYLE, value) }
                showStyleDialog = false
            },
            onDismiss = { showStyleDialog = false },
        )
    }

    if (showColorDialog) {
        ColorPickerDialog(
            initialColor = glowColor,
            showAlpha = true,
            title = stringResource(R.string.pref_ao_glowColor),
            onColorSelected = { color ->
                prefs.edit { putInt(P.DISPLAY_COLOR_EDGE_GLOW, color) }
                showColorDialog = false
            },
            onDismiss = { showColorDialog = false },
        )
    }

    PeekScaffold(
        title = stringResource(R.string.pref_ao_background_image),
        onBack = onBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // Image picker sub-screen
            PreferenceItem(
                iconRes = R.drawable.ic_image,
                title = stringResource(R.string.pref_ao_background_image),
                summary = stringResource(R.string.pref_ao_background_image_summary),
                onClick = onNavigateToImagePicker,
            )

            // Only available on API 28+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                SwitchPreferenceItem(
                    iconRes = R.drawable.ic_smartphone,
                    title = stringResource(R.string.pref_look_and_feel_hide_display_cutouts),
                    summary = stringResource(R.string.pref_look_and_feel_hide_display_cutouts_summary),
                    checked = hideDisplayCutouts,
                    onCheckedChange = { checked ->
                        prefs.edit { putBoolean("hide_display_cutouts", checked) }
                    },
                )
            }

            PreferenceDivider()

            // Edge glow master toggle
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_portrait,
                title = stringResource(R.string.pref_ao_edgeGlow),
                summary = stringResource(R.string.pref_ao_edgeGlow_summary),
                checked = edgeGlowEnabled,
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                onCheckedChange = { checked ->
                    prefs.edit { putBoolean(P.EDGE_GLOW, checked) }
                },
            )

            // Glow sub-preferences
            PreferenceItem(
                iconRes = R.drawable.ic_timer,
                title = stringResource(R.string.pref_ao_glowDuration),
                summary = stringResource(R.string.pref_ao_glowDuration_summary),
                enabled = edgeGlowEnabled,
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                onClick = { showDurationDialog = true },
            )

            PreferenceItem(
                iconRes = R.drawable.ic_timer,
                title = stringResource(R.string.pref_ao_glowDelay),
                summary = stringResource(R.string.pref_ao_glowDelay_summary),
                enabled = edgeGlowEnabled,
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                onClick = { showDelayDialog = true },
            )

            PreferenceItem(
                iconRes = R.drawable.ic_palette,
                title = stringResource(R.string.pref_ao_glowStyle),
                summary = stringResource(R.string.pref_ao_glowStyle_summary),
                enabled = edgeGlowEnabled,
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                onClick = { showStyleDialog = true },
            )

            ColorPreferenceItem(
                iconRes = R.drawable.ic_palette,
                title = stringResource(R.string.pref_ao_glowColor),
                summary = stringResource(R.string.pref_ao_glowColor_summary),
                color = Color(glowColor),
                enabled = edgeGlowEnabled,
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                onClick = { showColorDialog = true },
            )
        }
    }
}
