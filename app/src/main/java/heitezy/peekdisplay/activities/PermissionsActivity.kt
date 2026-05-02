package heitezy.peekdisplay.activities

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.os.Bundle
import androidx.core.content.edit
import heitezy.peekdisplay.R
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.helpers.Root
import heitezy.peekdisplay.ui.DeviceAdminDialog
import heitezy.peekdisplay.ui.PeekScaffold
import heitezy.peekdisplay.ui.PreferenceItem
import heitezy.peekdisplay.ui.SwitchPreferenceItem

class PermissionsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                PermissionsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { P.getPreferences(context) }
    var showDeviceAdminDialog by remember { mutableStateOf(false) }
    var rootChecked by remember {
        mutableStateOf(prefs.getBoolean(P.ROOT_MODE, P.ROOT_MODE_DEFAULT))
    }

    PeekScaffold(
        title = stringResource(R.string.pref_permissions),
        onBack = onBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Battery optimizations
            PreferenceItem(
                iconRes = R.drawable.ic_battery,
                title = stringResource(R.string.pref_ignore_battery_optimizations),
                summary = stringResource(R.string.pref_ignore_battery_optimizations_summary),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    )
                },
            )

            // Notification access
            PreferenceItem(
                iconRes = R.drawable.ic_notification,
                title = stringResource(R.string.pref_notification),
                summary = stringResource(R.string.pref_notification_summary),
                onClick = {
                    context.startActivity(
                        Intent().apply {
                            component = ComponentName(
                                "com.android.settings",
                                $$"com.android.settings.Settings$NotificationAccessSettingsActivity",
                            )
                        }
                    )
                },
            )

            // Draw over other apps
            PreferenceItem(
                iconRes = R.drawable.ic_brush,
                title = stringResource(R.string.pref_draw_over_other_apps),
                summary = stringResource(R.string.pref_draw_over_other_apps_summary),
                onClick = {
                    context.startActivity(
                        Intent().apply {
                            component = ComponentName(
                                "com.android.settings",
                                $$"com.android.settings.Settings$OverlaySettingsActivity",
                            )
                        }
                    )
                },
            )

            // Device admin
            PreferenceItem(
                iconRes = R.drawable.ic_security,
                title = stringResource(R.string.pref_admin),
                summary = stringResource(R.string.pref_admin_summary),
                onClick = { showDeviceAdminDialog = true },
            )

            // Root mode
            SwitchPreferenceItem(
                iconRes = R.drawable.ic_hashtag,
                title = stringResource(R.string.pref_root_mode),
                summary = stringResource(R.string.pref_root_mode_summary),
                checked = rootChecked,
                onCheckedChange = { checked ->
                    if (checked && !Root.request()) {
                        Toast.makeText(
                            context,
                            R.string.setup_root_failed,
                            Toast.LENGTH_LONG,
                        ).show()
                    } else {
                        rootChecked = checked
                        prefs.edit { putBoolean(P.ROOT_MODE, checked) }
                    }
                },
            )
        }
    }

    if (showDeviceAdminDialog) {
        DeviceAdminDialog(
            onDismiss = { showDeviceAdminDialog = false },
            onConfirm = { input ->
                showDeviceAdminDialog = false
                if (input.trim() == "19") {
                    context.startActivity(
                        Intent().apply {
                            component = ComponentName(
                                "com.android.settings",
                                $$"com.android.settings.Settings$DeviceAdminSettingsActivity",
                            )
                        }
                    )
                } else {
                    Toast.makeText(
                        context,
                        R.string.dialog_device_admin_error,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )
    }
}