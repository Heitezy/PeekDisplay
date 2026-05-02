package heitezy.peekdisplay.activities

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context.DEVICE_POLICY_SERVICE
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import heitezy.peekdisplay.R
import heitezy.peekdisplay.receivers.AdminReceiver
import heitezy.peekdisplay.ui.HelpCard
import heitezy.peekdisplay.ui.PeekScaffold

class HelpActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseContent {
                HelpScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    PeekScaffold(
        title = stringResource(R.string.help_center),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Missing permissions section
            HelpCard(
                title = stringResource(R.string.missing_permissions),
                body = stringResource(R.string.help_center_missing_permissions_text),
            )

            // Uninstall section
            HelpCard(
                title = stringResource(R.string.help_center_uninstall),
                body = stringResource(R.string.help_center_uninstall_text),
                rightButtonText = stringResource(R.string.help_center_uninstall_button),
                onClickRight = {
                    (context.getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager)
                        .removeActiveAdmin(ComponentName(context, AdminReceiver::class.java))
                    context.startActivity(
                        Intent(Intent.ACTION_DELETE)
                            .setData("package:${context.packageName}".toUri()),
                    )
                },
            )

            // Delays section
            HelpCard(
                title = stringResource(R.string.help_center_delays),
                body = stringResource(R.string.help_center_delays_text),
                leftButtonText = stringResource(R.string.help_center_delays_button_one),
                rightButtonText = stringResource(R.string.help_center_delays_button_two),
                onClickLeft = {
                    context.startActivity(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                    )
                },
                onClickRight = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData("https://dontkillmyapp.com/".toUri()),
                    )
                },
            )

            // Manufacturer-specific battery section
            HelpCard(
                title = stringResource(R.string.help_center_battery_drain),
                body = stringResource(R.string.help_center_battery_drain_text),
            )
        }
    }
}