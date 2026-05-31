package heitezy.peekdisplay.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.AlwaysOn
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.helpers.Permissions
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.text.input.KeyboardType
import heitezy.peekdisplay.ui.EditTextDialog
import heitezy.peekdisplay.ui.FormatDialog
import heitezy.peekdisplay.ui.PeekScaffold
import heitezy.peekdisplay.ui.PreferenceDivider
import heitezy.peekdisplay.ui.PreferenceItem
import heitezy.peekdisplay.ui.RadioButtonDialog
import heitezy.peekdisplay.ui.SeekBarPreferenceItem
import heitezy.peekdisplay.ui.SwitchPreferenceItem

class LAFWatchFaceActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                LAFWatchFaceScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun LAFWatchFaceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { P.getP(context) }

    val hasNotificationPermission = Permissions.isNotificationServiceEnabled(context)

    val clockEnabled by prefs.getBooleanFlow(P.SHOW_CLOCK, P.SHOW_CLOCK_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.SHOW_CLOCK, P.SHOW_CLOCK_DEFAULT))

    val use12Hour by prefs.getBooleanFlow(P.USE_12_HOUR_CLOCK, P.USE_12_HOUR_CLOCK_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.USE_12_HOUR_CLOCK, P.USE_12_HOUR_CLOCK_DEFAULT))

    val showAmPm by prefs.getBooleanFlow(P.SHOW_AM_PM, P.SHOW_AM_PM_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.SHOW_AM_PM, P.SHOW_AM_PM_DEFAULT))

    val dateEnabled by prefs.getBooleanFlow(P.SHOW_DATE, P.SHOW_DATE_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.SHOW_DATE, P.SHOW_DATE_DEFAULT))

    val dateFormat by prefs.getStringFlow(P.DATE_FORMAT, P.DATE_FORMAT_DEFAULT)
        .collectAsState(initial = prefs.getString(P.DATE_FORMAT, P.DATE_FORMAT_DEFAULT))

    val batteryIcon by prefs.getBooleanFlow(P.SHOW_BATTERY_ICON, P.SHOW_BATTERY_ICON_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.SHOW_BATTERY_ICON, P.SHOW_BATTERY_ICON_DEFAULT))

    val batteryPercentage by prefs.getBooleanFlow(P.SHOW_BATTERY_PERCENTAGE, P.SHOW_BATTERY_PERCENTAGE_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.SHOW_BATTERY_PERCENTAGE, P.SHOW_BATTERY_PERCENTAGE_DEFAULT))

    val musicControls by prefs.getBooleanFlow(P.SHOW_MUSIC_CONTROLS, P.SHOW_MUSIC_CONTROLS_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.SHOW_MUSIC_CONTROLS, P.SHOW_MUSIC_CONTROLS_DEFAULT))

    val albumArt by prefs.getBooleanFlow(P.SHOW_ALBUM_ART, P.SHOW_ALBUM_ART_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.SHOW_ALBUM_ART, P.SHOW_ALBUM_ART_DEFAULT))

    val calendar by prefs.getBooleanFlow(P.SHOW_CALENDAR, P.SHOW_CALENDAR_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.SHOW_CALENDAR, P.SHOW_CALENDAR_DEFAULT))

    val message by prefs.getStringFlow(P.MESSAGE, P.MESSAGE_DEFAULT)
        .collectAsState(initial = prefs.getString(P.MESSAGE, P.MESSAGE_DEFAULT))

    val notifications by prefs.getBooleanFlow(P.SHOW_NOTIFICATION_COUNT, P.SHOW_NOTIFICATION_COUNT_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.SHOW_NOTIFICATION_COUNT, P.SHOW_NOTIFICATION_COUNT_DEFAULT))

    val notificationIcons by prefs.getBooleanFlow(P.SHOW_NOTIFICATION_ICONS, P.SHOW_NOTIFICATION_ICONS_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.SHOW_NOTIFICATION_ICONS, P.SHOW_NOTIFICATION_ICONS_DEFAULT))

    val interactiveIcons by prefs.getBooleanFlow(P.INTERACTIVE_NOTIFICATION_ICONS, P.INTERACTIVE_NOTIFICATION_ICONS_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.INTERACTIVE_NOTIFICATION_ICONS, P.INTERACTIVE_NOTIFICATION_ICONS_DEFAULT))

    val invertHighlight by prefs.getBooleanFlow(P.INVERT_INTERACTION_HIGHLIGHT, P.INVERT_INTERACTION_HIGHLIGHT_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.INVERT_INTERACTION_HIGHLIGHT, P.INVERT_INTERACTION_HIGHLIGHT_DEFAULT))

    val tintNotifications by prefs.getBooleanFlow(P.TINT_NOTIFICATIONS, P.TINT_NOTIFICATIONS_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.TINT_NOTIFICATIONS, P.TINT_NOTIFICATIONS_DEFAULT))

    val notificationIconSize by prefs.getStringFlow(P.NOTIFICATION_ICON_SIZE, P.NOTIFICATION_ICON_SIZE_DEFAULT)
        .collectAsState(initial = prefs.getString(P.NOTIFICATION_ICON_SIZE, P.NOTIFICATION_ICON_SIZE_DEFAULT))

    val notificationTopPadding by prefs.getIntFlow(P.NOTIFICATION_ICON_TOP_PADDING, P.NOTIFICATION_ICON_TOP_PADDING_DEFAULT)
        .collectAsState(initial = prefs.getInt(P.NOTIFICATION_ICON_TOP_PADDING, P.NOTIFICATION_ICON_TOP_PADDING_DEFAULT))

    val notificationPreviewPosition by prefs.getStringFlow(P.NOTIFICATION_PREVIEW_POSITION, P.NOTIFICATION_PREVIEW_POSITION_DEFAULT)
        .collectAsState(initial = prefs.getString(P.NOTIFICATION_PREVIEW_POSITION, P.NOTIFICATION_PREVIEW_POSITION_DEFAULT))

    val fingerprintIcon by prefs.getBooleanFlow(P.SHOW_FINGERPRINT_ICON, P.SHOW_FINGERPRINT_ICON_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.SHOW_FINGERPRINT_ICON, P.SHOW_FINGERPRINT_ICON_DEFAULT))

    val lockIcon by prefs.getBooleanFlow(P.LOCK_ICON, P.LOCK_ICON_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.LOCK_ICON, P.LOCK_ICON_DEFAULT))

    val fingerprintMargin by prefs.getIntFlow(P.FINGERPRINT_MARGIN, P.FINGERPRINT_MARGIN_DEFAULT)
        .collectAsState(initial = prefs.getInt(P.FINGERPRINT_MARGIN, P.FINGERPRINT_MARGIN_DEFAULT))

    val fingerprintInteractionMode by prefs.getStringFlow(P.FINGERPRINT_INTERACTION_MODE, P.FINGERPRINT_INTERACTION_MODE_DEFAULT)
        .collectAsState(initial = prefs.getString(P.FINGERPRINT_INTERACTION_MODE, P.FINGERPRINT_INTERACTION_MODE_DEFAULT))

    val swipeNotificationOpen by prefs.getBooleanFlow(P.SWIPE_NOTIFICATION_OPEN, P.SWIPE_NOTIFICATION_OPEN_DEFAULT)
        .collectAsState(initial = prefs.getBoolean(P.SWIPE_NOTIFICATION_OPEN, P.SWIPE_NOTIFICATION_OPEN_DEFAULT))

    val displayScale by prefs.getIntFlow("pref_aod_scale_2", 100)
        .collectAsState(initial = prefs.getInt("pref_aod_scale_2", 100))

    val topPadding by prefs.getIntFlow(P.TOP_PADDING, P.TOP_PADDING_DEFAULT)
        .collectAsState(initial = prefs.getInt(P.TOP_PADDING, P.TOP_PADDING_DEFAULT))

    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var showNotificationIconSizeDialog by remember { mutableStateOf(false) }
    var showNotificationPreviewPositionDialog by remember { mutableStateOf(false) }
    var showFingerprintMarginDialog by remember { mutableStateOf(false) }
    var showFingerprintInteractionModeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        prefs.getChangeFlow().collect {
            AlwaysOn.finish()
        }
    }

    val notificationIconSizeEntries =
        stringArrayResource(R.array.pref_ao_notification_icon_size_display).toList()
    val notificationIconSizeValues =
        stringArrayResource(R.array.pref_ao_notification_icon_size_values).toList()
    val notificationPreviewEntries =
        stringArrayResource(R.array.pref_ao_notification_preview_position_display).toList()
    val notificationPreviewValues =
        stringArrayResource(R.array.pref_ao_notification_preview_position_values).toList()
    val fingerprintInteractionEntries =
        stringArrayResource(R.array.pref_fingerprint_interaction_mode_display).toList()
    val fingerprintInteractionValues =
        stringArrayResource(R.array.pref_fingerprint_interaction_mode_values).toList()

    fun persistBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
        AlwaysOn.finish()
    }

    fun persistString(key: String, value: String) {
        prefs.edit { putString(key, value) }
        AlwaysOn.finish()
    }

    fun persistInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
        AlwaysOn.finish()
    }

    PeekScaffold(
        title = stringResource(R.string.pref_ao_watch_face),
        onBack = onBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {

            // Style / Colors
            PreferenceItem(
                iconRes = R.drawable.ic_style,
                title = stringResource(R.string.pref_look_and_feel_ao),
                summary = stringResource(R.string.pref_look_and_feel_ao_summary),
                onClick = {
                    context.startActivity(
                        Intent(
                            context,
                            LAFAlwaysOnLookActivity::class.java
                        )
                    )
                },
            )

            PreferenceItem(
                iconRes = R.drawable.ic_palette,
                title = stringResource(R.string.pref_look_and_feel_colors),
                summary = stringResource(R.string.pref_look_and_feel_colors_summary),
                onClick = {
                    context.startActivity(
                        Intent(
                            context,
                            LAFWFColorsActivity::class.java
                        )
                    )
                },
            )

            PreferenceDivider()

            // Clock
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_clock,
                title = stringResource(R.string.pref_ao_showClock),
                checked = clockEnabled,
                summaryOn = stringResource(R.string.pref_enabled),
                summaryOff = stringResource(R.string.pref_disabled),
                onCheckedChange = {
                    persistBoolean(P.SHOW_CLOCK, it)
                },
            )

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_clock,
                title = stringResource(R.string.pref_look_and_feel_hour),
                checked = use12Hour,
                summaryOn = stringResource(R.string.pref_look_and_feel_hour_summary_on),
                summaryOff = stringResource(R.string.pref_look_and_feel_hour_summary_off),
                enabled = clockEnabled,
                onCheckedChange = {
                    persistBoolean(P.USE_12_HOUR_CLOCK, it)
                },
            )

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_clock,
                title = stringResource(R.string.pref_look_and_feel_am_pm),
                summary = stringResource(R.string.pref_look_and_feel_am_pm_summary),
                checked = showAmPm,
                enabled = use12Hour && clockEnabled,
                onCheckedChange = {
                    persistBoolean(P.SHOW_AM_PM, it)
                },
            )

            PreferenceDivider()

            // Date
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_date,
                title = stringResource(R.string.pref_ao_showDate),
                checked = dateEnabled,
                summaryOn = stringResource(R.string.pref_enabled),
                summaryOff = stringResource(R.string.pref_disabled),
                onCheckedChange = {
                    persistBoolean(P.SHOW_DATE, it)
                },
            )

            PreferenceItem(
                iconRes = R.drawable.ic_date,
                title = stringResource(R.string.pref_ao_date_format),
                summary = stringResource(R.string.pref_ao_date_format_summary),
                enabled = dateEnabled,
                onClick = { showDateFormatDialog = true },
            )

            PreferenceDivider()

            // Battery
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_battery,
                title = stringResource(R.string.pref_ao_showBatteryIcn),
                checked = batteryIcon,
                summaryOn = stringResource(R.string.pref_enabled),
                summaryOff = stringResource(R.string.pref_disabled),
                onCheckedChange = {
                    persistBoolean(P.SHOW_BATTERY_ICON, it)
                },
            )

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_percent,
                title = stringResource(R.string.pref_ao_showBattery),
                checked = batteryPercentage,
                summaryOn = stringResource(R.string.pref_enabled),
                summaryOff = stringResource(R.string.pref_disabled),
                onCheckedChange = {
                    persistBoolean(P.SHOW_BATTERY_PERCENTAGE, it)
                },
            )

            PreferenceDivider()

            // Music
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_music_note,
                title = stringResource(R.string.pref_ao_showMusicControls),
                checked = musicControls,
                summaryOn = stringResource(R.string.pref_enabled),
                summaryOff = stringResource(R.string.pref_disabled),
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                onCheckedChange = {
                    persistBoolean(P.SHOW_MUSIC_CONTROLS, it)
                },
            )

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_image,
                title = stringResource(R.string.pref_ao_showAlbumArt),
                checked = albumArt,
                summaryOn = stringResource(R.string.pref_enabled),
                summaryOff = stringResource(R.string.pref_disabled),
                enabled = musicControls,
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                onCheckedChange = {
                    persistBoolean(P.SHOW_ALBUM_ART, it)
                },
            )

            PreferenceDivider()

            // Calendar
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_date,
                title = stringResource(R.string.pref_ao_calendar),
                summary = stringResource(R.string.pref_ao_calendar_summary),
                checked = calendar,
                onCheckedChange = { newValue ->
                    persistBoolean(P.SHOW_CALENDAR, newValue)
                    if (newValue) {
                        ActivityCompat.requestPermissions(
                            context as LAFWatchFaceActivity,
                            arrayOf(Manifest.permission.READ_CALENDAR),
                            0,
                        )
                    }
                },
            )

            // Message
            PreferenceItem(
                iconRes = R.drawable.ic_short_text,
                title = stringResource(R.string.pref_ao_message),
                summary = stringResource(R.string.pref_ao_message_summary),
                onClick = { showMessageDialog = true },
            )

            // Weather
            PreferenceItem(
                iconRes = R.drawable.ic_cloud,
                title = stringResource(R.string.pref_look_and_feel_weather_info),
                summary = stringResource(R.string.pref_look_and_feel_weather_info_summary),
                onClick = {
                    context.startActivity(
                        Intent(
                            context,
                            LAFWeatherActivity::class.java
                        )
                    )
                },
            )

            PreferenceDivider()

            // Notifications
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_notification,
                title = stringResource(R.string.pref_ao_showNotifications),
                checked = notifications,
                summaryOn = stringResource(R.string.pref_enabled),
                summaryOff = stringResource(R.string.pref_disabled),
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                onCheckedChange = {
                    persistBoolean(P.SHOW_NOTIFICATION_COUNT, it)
                },
            )

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_notification,
                title = stringResource(R.string.pref_ao_showNotificationIcons),
                checked = notificationIcons,
                summaryOn = stringResource(R.string.pref_enabled),
                summaryOff = stringResource(R.string.pref_disabled),
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                onCheckedChange = {
                    persistBoolean(P.SHOW_NOTIFICATION_ICONS, it)
                },
            )

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_notification,
                title = stringResource(R.string.pref_ao_interactive_notification_icons),
                summary = stringResource(R.string.pref_ao_interactive_notification_icons_summary),
                checked = interactiveIcons,
                enabled = notificationIcons,
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                onCheckedChange = {
                    persistBoolean(P.INTERACTIVE_NOTIFICATION_ICONS, it)
                },
            )

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_notification,
                title = stringResource(R.string.pref_ao_invert_interaction_highlight),
                summary = stringResource(R.string.pref_ao_invert_interaction_highlight_summary),
                checked = invertHighlight,
                enabled = notificationIcons,
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                onCheckedChange = {
                    persistBoolean(P.INVERT_INTERACTION_HIGHLIGHT, it)
                },
            )

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_palette,
                title = stringResource(R.string.pref_look_and_feel_tint_notifications),
                summary = stringResource(R.string.pref_look_and_feel_tint_notifications_summary),
                checked = tintNotifications,
                enabled = notificationIcons,
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                onCheckedChange = {
                    persistBoolean(P.TINT_NOTIFICATIONS, it)
                },
            )

            // Notification icon size
            PreferenceItem(
                iconRes = R.drawable.ic_notification,
                title = stringResource(R.string.pref_ao_notification_icon_size),
                summary = stringResource(R.string.pref_ao_notification_icon_size_summary),
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                enabled = notificationIcons,
                onClick = { showNotificationIconSizeDialog = true },
            )

            // Notification icon top padding
            SeekBarPreferenceItem(
                iconRes = R.drawable.ic_notification,
                title = stringResource(R.string.pref_ao_notification_icon_top_padding),
                value = notificationTopPadding,
                valueRange = 0f..800f,
                summaryProvider = { stringResource(R.string.pref_ao_notification_icon_top_padding_summary) },
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                enabled = notificationIcons,
                onValueChange = { v ->
                    persistInt(P.NOTIFICATION_ICON_TOP_PADDING, v)
                },
            )

            // Notification preview position
            PreferenceItem(
                iconRes = R.drawable.ic_notification,
                title = stringResource(R.string.pref_ao_notification_preview_position),
                summary = stringResource(R.string.pref_ao_notification_preview_position_summary),
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                enabled = interactiveIcons && notificationIcons,
                onClick = { showNotificationPreviewPositionDialog = true },
            )

            PreferenceDivider()

            // Fingerprint
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_fingerprint,
                title = stringResource(R.string.pref_ao_fingerprint),
                checked = fingerprintIcon,
                summaryOn = stringResource(R.string.pref_enabled),
                summaryOff = stringResource(R.string.pref_disabled),
                onCheckedChange = {
                    persistBoolean(P.SHOW_FINGERPRINT_ICON, it)
                },
            )

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_lock_black,
                title = stringResource(R.string.pref_ao_lock_icon),
                summary = stringResource(R.string.pref_ao_lock_icon_summary),
                checked = lockIcon,
                enabled = fingerprintIcon,
                onCheckedChange = {
                    persistBoolean(P.LOCK_ICON, it)
                },
            )

            // Fingerprint margin
            PreferenceItem(
                iconRes = R.drawable.ic_fingerprint,
                title = stringResource(R.string.pref_ao_fingerprint_margin),
                summary = stringResource(R.string.pref_ao_fingerprint_margin_summary),
                enabled = fingerprintIcon,
                onClick = { showFingerprintMarginDialog = true },
            )

            // Fingerprint interaction mode
            PreferenceItem(
                iconRes = R.drawable.ic_fingerprint,
                title = stringResource(R.string.pref_ao_fingerprint_interaction),
                summary = stringResource(R.string.pref_ao_fingerprint_interaction_summary),
                enabled = fingerprintIcon,
                onClick = { showFingerprintInteractionModeDialog = true },
            )

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_fingerprint,
                title = stringResource(R.string.pref_ao_swipe_notification_open),
                summary = stringResource(R.string.pref_ao_swipe_notification_open_summary),
                checked = swipeNotificationOpen,
                enabled = fingerprintIcon,
                onCheckedChange = {
                    persistBoolean(P.SWIPE_NOTIFICATION_OPEN, it)
                },
            )

            PreferenceDivider()

            // Display scale
            SeekBarPreferenceItem(
                iconRes = R.drawable.ic_scale,
                title = stringResource(R.string.pref_look_and_feel_display_size),
                value = displayScale,
                valueRange = 0f..200f,
                summaryProvider = { v ->
                    stringResource(R.string.pref_look_and_feel_display_size_summary, v)
                },
                onValueChange = { v ->
                    persistInt("pref_aod_scale_2", v)
                },
            )

            // Top padding
            SeekBarPreferenceItem(
                iconRes = R.drawable.ic_scale,
                title = stringResource(R.string.pref_ao_top_padding),
                value = topPadding,
                valueRange = 0f..100f,
                summaryProvider = { stringResource(R.string.pref_ao_top_padding_summary) },
                onValueChange = { v ->
                    persistInt(P.TOP_PADDING, v)
                },
            )
        }
    }

    if (showDateFormatDialog) {
        FormatDialog(
            title = stringResource(R.string.pref_ao_date_format),
            current = dateFormat,
            onConfirm = { newFormat ->
                persistString(P.DATE_FORMAT, newFormat)
                showDateFormatDialog = false
            },
            onDismiss = { showDateFormatDialog = false },
            onMore = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).setData(
                        ("https://developer.android.com/reference/java/text" +
                                "/SimpleDateFormat#date-and-time-patterns").toUri()
                    )
                )
            },
            validate = { format ->
                try {
                    SimpleDateFormat(format, Locale.getDefault())
                    true
                } catch (e: IllegalArgumentException) {
                    Log.w("LAFWatchFace", e.toString())
                    Toast.makeText(context, R.string.pref_ao_date_format_illegal, Toast.LENGTH_LONG).show()
                    false
                }
            },
        )
    }

    if (showMessageDialog) {
        EditTextDialog(
            title = stringResource(R.string.pref_ao_message),
            current = message,
            keyboardType = KeyboardType.Text,
            onConfirm = { newMessage ->
                persistString(P.MESSAGE, newMessage)
                showMessageDialog = false
            },
            onDismiss = { showMessageDialog = false },
        )
    }

    if (showNotificationIconSizeDialog) {
        RadioButtonDialog(
            title = stringResource(R.string.pref_ao_notification_icon_size),
            entries = notificationIconSizeEntries,
            entryValues = notificationIconSizeValues,
            selectedValue = notificationIconSize,
            onValueSelected = { v ->
                persistString(P.NOTIFICATION_ICON_SIZE, v)
            },
            onDismiss = { showNotificationIconSizeDialog = false },
        )
    }

    if (showNotificationPreviewPositionDialog) {
        RadioButtonDialog(
            title = stringResource(R.string.pref_ao_notification_preview_position),
            entries = notificationPreviewEntries,
            entryValues = notificationPreviewValues,
            selectedValue = notificationPreviewPosition,
            onValueSelected = { v ->
                persistString(P.NOTIFICATION_PREVIEW_POSITION, v)
            },
            onDismiss = { showNotificationPreviewPositionDialog = false },
        )
    }

    if (showFingerprintMarginDialog) {
        EditTextDialog(
            title = stringResource(R.string.pref_ao_fingerprint_margin),
            current = fingerprintMargin.toString(),
            keyboardType = KeyboardType.Number,
            onConfirm = { raw ->
                val parsed = raw.toIntOrNull()
                if (parsed != null) {
                    persistInt(P.FINGERPRINT_MARGIN, parsed)
                } else {
                    Toast.makeText(context, R.string.pref_int_failed, Toast.LENGTH_LONG).show()
                }
                showFingerprintMarginDialog = false
            },
            onDismiss = { showFingerprintMarginDialog = false },
        )
    }

    if (showFingerprintInteractionModeDialog) {
        RadioButtonDialog(
            title = stringResource(R.string.pref_ao_fingerprint_interaction),
            entries = fingerprintInteractionEntries,
            entryValues = fingerprintInteractionValues,
            selectedValue = fingerprintInteractionMode,
            onValueSelected = { v ->
                persistString(P.FINGERPRINT_INTERACTION_MODE, v)
            },
            onDismiss = { showFingerprintInteractionModeDialog = false },
        )
    }
}
