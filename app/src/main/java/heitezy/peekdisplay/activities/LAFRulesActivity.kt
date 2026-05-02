package heitezy.peekdisplay.activities

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
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
import androidx.compose.ui.res.pluralStringResource
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
import heitezy.peekdisplay.helpers.Rules
import heitezy.peekdisplay.services.PickUpService
import androidx.core.content.edit
import heitezy.peekdisplay.ui.ChainedTimePickerDialog
import heitezy.peekdisplay.ui.MultiSelectDialog
import heitezy.peekdisplay.helpers.Permissions

private const val DEFAULT_START_TIME = "0:00"
private const val DEFAULT_END_TIME = "0:00"

class LAFRulesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                LAFRulesScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun LAFRulesScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val hasNotificationPermission = Permissions.isNotificationServiceEnabled(context)

    val prefs = remember { P.getPreferences(context) }
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    val alwaysOnEnabled = remember { prefs.getBoolean(P.ALWAYS_ON, P.ALWAYS_ON_DEFAULT) }

    var disableInDnd by remember {
        mutableStateOf(
            prefs.getBoolean(
                P.RULES_DISABLE_IN_DO_NOT_DISTURB,
                P.RULES_DISABLE_IN_DO_NOT_DISTURB_DEFAULT
            )
        )
    }
    var ambientMode by remember { mutableStateOf(prefs.getBoolean("rules_ambient_mode", false)) }
    var pickupMode by remember { mutableStateOf(prefs.getBoolean("rules_pickup_mode", false)) }

    var pickupSensitivity by remember {
        mutableStateOf(
            prefs.getString(P.PICKUP_SENSITIVITY, P.PICKUP_SENSITIVITY_DEFAULT)
                ?: P.PICKUP_SENSITIVITY_DEFAULT
        )
    }
    var showPickupSensitivityDialog by remember { mutableStateOf(false) }
    val pickupSensitivityEntries =
        stringArrayResource(R.array.pref_pickup_sensitivity_display).toList()
    val pickupSensitivityValues =
        stringArrayResource(R.array.pref_pickup_sensitivity_values).toList()

    var chargingState by remember {
        mutableStateOf(
            prefs.getString(P.RULES_CHARGING_STATE, P.RULES_CHARGING_STATE_DEFAULT)
                ?: P.RULES_CHARGING_STATE_DEFAULT
        )
    }
    var showChargingStateDialog by remember { mutableStateOf(false) }
    val chargingStateEntries =
        stringArrayResource(R.array.pref_look_and_feel_rules_charging_state_display).toList()
    val chargingStateValues =
        stringArrayResource(R.array.pref_look_and_feel_rules_charging_state_values).toList()

    val chargerDefaultValues =
        stringArrayResource(R.array.pref_look_and_feel_rules_charger_values).toSet()
    var chargerTypes by remember {
        mutableStateOf(
            prefs.getStringSet("rules_charger_type", chargerDefaultValues) ?: chargerDefaultValues
        )
    }
    var showChargerTypeDialog by remember { mutableStateOf(false) }
    val chargerTypeEntries =
        stringArrayResource(R.array.pref_look_and_feel_rules_charger_display).toList()
    val chargerTypeValues =
        stringArrayResource(R.array.pref_look_and_feel_rules_charger_values).toList()

    var batteryLevel by remember {
        mutableIntStateOf(
            prefs.getInt(P.RULES_BATTERY, P.RULES_BATTERY_DEFAULT).coerceAtMost(Rules.BATTERY_FULL)
        )
    }
    var showBatteryDialog by remember { mutableStateOf(false) }

    var timeStart by remember {
        mutableStateOf(
            prefs.getString(
                "rules_time_start",
                DEFAULT_START_TIME
            ) ?: DEFAULT_START_TIME
        )
    }
    var timeEnd by remember {
        mutableStateOf(
            prefs.getString("rules_time_end", DEFAULT_END_TIME) ?: DEFAULT_END_TIME
        )
    }
    var showTimePicker by remember { mutableStateOf(false) }

    var timeout by remember {
        mutableIntStateOf(
            prefs.getInt(
                P.RULES_TIMEOUT,
                P.RULES_TIMEOUT_DEFAULT
            )
        )
    }
    var showTimeoutDialog by remember { mutableStateOf(false) }

    val batterySummary = if (batteryLevel > 0)
        stringResource(R.string.pref_look_and_feel_rules_battery_level_summary, batteryLevel)
    else
        stringResource(R.string.pref_look_and_feel_rules_battery_level_summary_zero)

    val timeSummary = stringResource(
        R.string.pref_look_and_feel_rules_time_summary,
        timeStart,
        timeEnd,
    )

    val timeoutSummary = if (timeout > 0)
        pluralStringResource(R.plurals.pref_look_and_feel_rules_timeout_summary, timeout, timeout)
    else
        stringResource(R.string.pref_look_and_feel_rules_timeout_summary_zero)

    val chargingStateSummary =
        chargingStateEntries.getOrElse(chargingStateValues.indexOf(chargingState)) { chargingState }
    val pickupSensitivitySummary =
        pickupSensitivityEntries.getOrElse(pickupSensitivityValues.indexOf(pickupSensitivity)) {
            stringResource(R.string.pref_pickup_sensitivity_summary)
        }

    if (showPickupSensitivityDialog) {
        RadioButtonDialog(
            title = stringResource(R.string.pref_pickup_sensitivity),
            entries = pickupSensitivityEntries,
            entryValues = pickupSensitivityValues,
            selectedValue = pickupSensitivity,
            onValueSelected = { value ->
                pickupSensitivity = value
                prefs.edit { putString(P.PICKUP_SENSITIVITY, value) }
            },
            onDismiss = { showPickupSensitivityDialog = false },
        )
    }

    if (showChargingStateDialog) {
        RadioButtonDialog(
            title = stringResource(R.string.pref_look_and_feel_rules_charging_state),
            entries = chargingStateEntries,
            entryValues = chargingStateValues,
            selectedValue = chargingState,
            onValueSelected = { value ->
                chargingState = value
                prefs.edit { putString(P.RULES_CHARGING_STATE, value) }
            },
            onDismiss = { showChargingStateDialog = false },
        )
    }

    if (showChargerTypeDialog) {
        MultiSelectDialog(
            title = stringResource(R.string.pref_look_and_feel_rules_charger),
            entries = chargerTypeEntries,
            entryValues = chargerTypeValues,
            selectedValues = chargerTypes,
            onConfirm = { selected ->
                chargerTypes = selected
                prefs.edit { putStringSet("rules_charger_type", selected) }
            },
            onDismiss = { showChargerTypeDialog = false },
        )
    }

    if (showBatteryDialog) {
        EditTextDialog(
            title = stringResource(R.string.pref_look_and_feel_rules_battery_level),
            current = batteryLevel.toString(),
            keyboardType = KeyboardType.Number,
            onConfirm = { raw ->
                val newValue = (raw.toIntOrNull() ?: P.RULES_BATTERY_DEFAULT)
                    .coerceAtMost(Rules.BATTERY_FULL)
                batteryLevel = newValue
                prefs.edit { putInt(P.RULES_BATTERY, newValue) }
                showBatteryDialog = false
            },
            onDismiss = { showBatteryDialog = false },
            validate = { it.toIntOrNull() != null },
        )
    }

    if (showTimePicker) {
        ChainedTimePickerDialog(
            startTime = timeStart,
            endTime = timeEnd,
            is24Hour = is24Hour,
            onConfirm = { start, end ->
                timeStart = start
                timeEnd = end
                prefs.edit {
                    putString("rules_time_start", start)
                        .putString("rules_time_end", end)
                }
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }

    if (showTimeoutDialog) {
        EditTextDialog(
            title = stringResource(R.string.pref_look_and_feel_rules_timeout),
            current = timeout.toString(),
            keyboardType = KeyboardType.Number,
            onConfirm = { raw ->
                val newValue = raw.toIntOrNull() ?: P.RULES_TIMEOUT_DEFAULT
                timeout = newValue
                prefs.edit { putInt(P.RULES_TIMEOUT, newValue) }
                showTimeoutDialog = false
            },
            onDismiss = { showTimeoutDialog = false },
            validate = { it.toIntOrNull() != null },
        )
    }

    PeekScaffold(
        title = stringResource(R.string.pref_look_and_feel_rules),
        onBack = onBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {

            // Disable in DND
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_dnd,
                title = stringResource(R.string.pref_ao_disable_on_dnd),
                summary = stringResource(R.string.pref_ao_disable_on_dnd_summary),
                checked = disableInDnd,
                onCheckedChange = { checked ->
                    disableInDnd = checked
                    prefs.edit { putBoolean(P.RULES_DISABLE_IN_DO_NOT_DISTURB, checked) }
                },
            )

            // Ambient mode
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_notification,
                title = stringResource(R.string.pref_look_and_feel_rules_ambient_mode),
                summary = stringResource(R.string.pref_look_and_feel_rules_ambient_mode_summary),
                checked = ambientMode,
                enabled = alwaysOnEnabled,
                onCheckedChange = { checked ->
                    ambientMode = checked
                    prefs.edit { putBoolean("rules_ambient_mode", checked) }
                },
            )

            // Pick-up mode
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_vibration,
                title = stringResource(R.string.pref_look_and_feel_rules_pickup_mode),
                summary = stringResource(R.string.pref_look_and_feel_rules_pickup_mode_summary),
                checked = pickupMode,
                enabled = alwaysOnEnabled,
                onCheckedChange = { checked ->
                    pickupMode = checked
                    prefs.edit { putBoolean("rules_pickup_mode", checked) }
                    if (checked) PickUpService.startService(context)
                    else PickUpService.stopService(context)
                },
            )

            // Pick-up sensitivity
            PreferenceItem(
                iconRes = R.drawable.ic_vibration,
                title = stringResource(R.string.pref_pickup_sensitivity),
                summary = pickupSensitivitySummary,
                enabled = pickupMode,
                onClick = { showPickupSensitivityDialog = true },
            )

            // Filter notifications
            PreferenceItem(
                iconRes = R.drawable.ic_notification,
                title = stringResource(R.string.pref_look_and_feel_filter_notifications),
                summary = stringResource(R.string.pref_look_and_feel_filter_notifications_summary),
                hasPermission = hasNotificationPermission,
                permissionDeniedSummary = stringResource(R.string.permissions_notification_access),
                onClick = {
                    context.startActivity(
                        Intent(
                            context,
                            LAFFilterNotificationsActivity::class.java
                        )
                    )
                },
            )

            // Charging state
            PreferenceItem(
                iconRes = R.drawable.ic_battery,
                title = stringResource(R.string.pref_look_and_feel_rules_charging_state),
                summary = chargingStateSummary,
                hasPermission = Permissions.isDeviceAdminOrRoot(context),
                permissionDeniedSummary = stringResource(R.string.permissions_device_admin_or_root),
                onClick = { showChargingStateDialog = true },
            )

            // Charger type
            PreferenceItem(
                iconRes = R.drawable.ic_battery,
                title = stringResource(R.string.pref_look_and_feel_rules_charger),
                summary = stringResource(R.string.pref_look_and_feel_rules_charger_summary),
                onClick = { showChargerTypeDialog = true },
            )

            // Battery level
            PreferenceItem(
                iconRes = R.drawable.ic_percent,
                title = stringResource(R.string.pref_look_and_feel_rules_battery_level),
                summary = batterySummary,
                hasPermission = Permissions.isDeviceAdminOrRoot(context),
                permissionDeniedSummary = stringResource(R.string.permissions_device_admin_or_root),
                onClick = { showBatteryDialog = true },
            )

            // Time range
            PreferenceItem(
                iconRes = R.drawable.ic_clock,
                title = stringResource(R.string.pref_look_and_feel_rules_time),
                summary = timeSummary,
                hasPermission = Permissions.isDeviceAdminOrRoot(context),
                permissionDeniedSummary = stringResource(R.string.permissions_device_admin_or_root),
                onClick = { showTimePicker = true },
            )

            // Timeout
            PreferenceItem(
                iconRes = R.drawable.ic_timer,
                title = stringResource(R.string.pref_look_and_feel_rules_timeout),
                summary = timeoutSummary,
                hasPermission = Permissions.isDeviceAdminOrRoot(context),
                permissionDeniedSummary = stringResource(R.string.permissions_device_admin_or_root),
                onClick = { showTimeoutDialog = true },
            )
        }
    }
}