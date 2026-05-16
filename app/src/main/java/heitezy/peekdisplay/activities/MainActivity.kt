package heitezy.peekdisplay.activities

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import heitezy.peekdisplay.BuildConfig
import heitezy.peekdisplay.R
import heitezy.peekdisplay.helpers.Global
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.helpers.Permissions
import heitezy.peekdisplay.services.ForegroundService
import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import heitezy.peekdisplay.ui.PeekScaffold
import heitezy.peekdisplay.ui.PreferenceDivider
import heitezy.peekdisplay.ui.PreferenceItem
import heitezy.peekdisplay.ui.SectionHeader
import heitezy.peekdisplay.ui.SwitchPreferenceItem
import heitezy.peekdisplay.ui.PermissionDialog

class MainActivity : BaseActivity() {

    private var debugClicker = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Global.currentAlwaysOnState(this)) {
            ContextCompat.startForegroundService(this, Intent(this, ForegroundService::class.java))
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            onBackPressedDispatcher.addCallback {
                startActivity(
                    Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }

        setContent {
            BaseContent {
                MainScreen(
                    onAlwaysOnToggled = { handleAlwaysOnClicked() },
                    onNavigate = { intent -> startActivity(intent) },
                )
            }
        }
    }

    private fun handleAlwaysOnClicked(): Boolean {
        val newValue = Global.changeAlwaysOnState(this)
        if (BuildConfig.DEBUG) {
            debugClicker++
            if (debugClicker == CLICKS_TIL_TEST) {
                debugClicker = 0
                startActivity(Intent(this, AODTestActivity::class.java))
            }
        }
        return newValue
    }

    companion object {
        private const val CLICKS_TIL_TEST = 4
    }
}

private enum class PermissionDialog {
    DEVICE_ADMIN,
    NOTIFICATION_ACCESS,
    DISPLAY_OVER_OTHER_APPS,
    PHONE_STATE,
    CALENDAR,
}

@Composable
private fun MainScreen(
    onAlwaysOnToggled: () -> Boolean,
    onNavigate: (Intent) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { P.getPreferences(context) }

    var alwaysOnChecked by remember {
        mutableStateOf(prefs.getBoolean(P.ALWAYS_ON, P.ALWAYS_ON_DEFAULT))
    }
    var chargingChecked by remember {
        mutableStateOf(prefs.getBoolean("charging_animation", false))
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            alwaysOnChecked = prefs.getBoolean(P.ALWAYS_ON, P.ALWAYS_ON_DEFAULT)
            chargingChecked = prefs.getBoolean("charging_animation", false)
        }
    }

    var pendingDialog by remember { mutableStateOf<PermissionDialog?>(null) }

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            pendingDialog = when {
                Permissions.needsNotificationPermissions(context) ->
                    PermissionDialog.NOTIFICATION_ACCESS
                Permissions.needsDeviceAdminOrRoot(context) ->
                    PermissionDialog.DEVICE_ADMIN
                Permissions.needsCalendarPermission(context) ->
                    PermissionDialog.CALENDAR
                !Permissions.hasPhoneStatePermission(context) ->
                    PermissionDialog.PHONE_STATE
                !Settings.canDrawOverlays(context) ->
                    PermissionDialog.DISPLAY_OVER_OTHER_APPS
                else -> null
            }
        }
    }

    pendingDialog?.let { dialog ->
        PermissionDialogForType(
            dialog = dialog,
            onConfirm = {
                pendingDialog = null
                when (dialog) {
                    PermissionDialog.DEVICE_ADMIN ->
                        onNavigate(Intent(context, PermissionsActivity::class.java))
                    PermissionDialog.DISPLAY_OVER_OTHER_APPS ->
                        onNavigate(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                    PermissionDialog.NOTIFICATION_ACCESS ->
                        onNavigate(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    PermissionDialog.PHONE_STATE ->
                        ActivityCompat.requestPermissions(
                            context as MainActivity,
                            arrayOf(Manifest.permission.READ_PHONE_STATE),
                            0,
                        )
                    PermissionDialog.CALENDAR ->
                        ActivityCompat.requestPermissions(
                            context as MainActivity,
                            arrayOf(Manifest.permission.READ_CALENDAR),
                            0,
                        )
                }
            },
            onDismiss = { pendingDialog = null },
        )
    }

    PeekScaffold(title = stringResource(R.string.app_name), titleCentered = true) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.pref_general))

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_always_on,
                title = stringResource(R.string.pref_always_on),
                summaryOn = stringResource(R.string.pref_enabled),
                summaryOff = stringResource(R.string.pref_disabled),
                checked = alwaysOnChecked,
                onCheckedChange = {
                    alwaysOnChecked = onAlwaysOnToggled()
                },
            )

            SwitchPreferenceItem(
                iconRes = R.drawable.ic_charging,
                title = stringResource(R.string.pref_charging),
                hasPermission = Permissions.isDeviceAdminOrRoot(context),
                permissionDeniedSummary = stringResource(R.string.permissions_device_admin_or_root),
                summaryOn = stringResource(R.string.pref_enabled),
                summaryOff = stringResource(R.string.pref_disabled),
                checked = chargingChecked,
                onCheckedChange = { checked ->
                    chargingChecked = checked
                    prefs.edit { putBoolean("charging_animation", checked) }
                },
            )

            PreferenceDivider()

            SectionHeader(stringResource(R.string.pref_look_and_feel))

            PreferenceItem(
                iconRes = R.drawable.ic_clock,
                title = stringResource(R.string.pref_ao_watch_face),
                summary = stringResource(R.string.pref_ao_watch_face_summary),
                onClick = { onNavigate(Intent(context, LAFWatchFaceActivity::class.java)) },
            )
            PreferenceItem(
                iconRes = R.drawable.ic_smartphone,
                title = stringResource(R.string.pref_ao_background),
                summary = stringResource(R.string.pref_ao_background_summary),
                onClick = { onNavigate(Intent(context, LAFBackgroundActivity::class.java)) },
            )
            PreferenceItem(
                iconRes = R.drawable.ic_build,
                title = stringResource(R.string.pref_look_and_feel_rules),
                summary = stringResource(R.string.pref_look_and_feel_rules_summary),
                onClick = { onNavigate(Intent(context, LAFRulesActivity::class.java)) },
            )
            PreferenceItem(
                iconRes = R.drawable.ic_speed,
                title = stringResource(R.string.pref_ao_behavior),
                summary = stringResource(R.string.pref_ao_behavior_summary),
                onClick = { onNavigate(Intent(context, LAFBehaviorActivity::class.java)) },
            )
            PreferenceItem(
                iconRes = R.drawable.ic_charging,
                title = stringResource(R.string.pref_look_and_feel_charging),
                hasPermission = Permissions.isDeviceAdminOrRoot(context),
                permissionDeniedSummary = stringResource(R.string.permissions_device_admin_or_root),
                summary = stringResource(R.string.pref_look_and_feel_charging_summary),
                onClick = { onNavigate(Intent(context, LAFChargingLookActivity::class.java)) },
            )

            PreferenceDivider()

            SectionHeader(stringResource(R.string.pref_ao_other))

            PreferenceItem(
                iconRes = R.drawable.ic_security,
                title = stringResource(R.string.pref_permissions),
                summary = stringResource(R.string.pref_permissions_summary),
                onClick = { onNavigate(Intent(context, PermissionsActivity::class.java)) },
            )
            PreferenceItem(
                iconRes = R.drawable.ic_help,
                title = stringResource(R.string.help_center),
                summary = stringResource(R.string.help_center_summary),
                onClick = { onNavigate(Intent(context, HelpActivity::class.java)) },
            )
            PreferenceItem(
                iconRes = R.drawable.ic_info,
                title = stringResource(R.string.about),
                summary = stringResource(R.string.about_summary),
                onClick = { onNavigate(Intent(context, AboutActivity::class.java)) },
            )
        }
    }
}


@Composable
private fun PermissionDialogForType(
    dialog: PermissionDialog,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (iconRes, titleRes, messageRes) = when (dialog) {
        PermissionDialog.DEVICE_ADMIN -> Triple(
            R.drawable.ic_color_mode,
            R.string.device_admin,
            R.string.device_admin_summary,
        )
        PermissionDialog.DISPLAY_OVER_OTHER_APPS -> Triple(
            R.drawable.ic_color_draw_over_other_apps,
            R.string.setup_draw_over_other_apps,
            R.string.setup_draw_over_other_apps_summary,
        )
        PermissionDialog.NOTIFICATION_ACCESS -> Triple(
            R.drawable.ic_color_notification,
            R.string.notification_listener_service,
            R.string.notification_listener_service_summary,
        )
        PermissionDialog.PHONE_STATE -> Triple(
            R.drawable.ic_color_phone_state,
            R.string.setup_phone_state,
            R.string.setup_phone_state_summary,
        )
        PermissionDialog.CALENDAR -> Triple(
            R.drawable.ic_color_calendar,
            R.string.setup_calendar,
            R.string.setup_calendar_summary,
        )
    }

    PermissionDialog(
        iconRes = iconRes,
        title = stringResource(titleRes),
        message = stringResource(messageRes),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}