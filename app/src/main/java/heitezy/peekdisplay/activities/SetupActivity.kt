package heitezy.peekdisplay.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import heitezy.peekdisplay.R
import heitezy.peekdisplay.helpers.P
import androidx.core.content.edit
import heitezy.peekdisplay.ui.SetupScreenContent

class SetupActivity : BaseActivity() {

    private var isActionRequired = false

    private val phoneStatePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            finishSetup()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefsEditor = PreferenceManager.getDefaultSharedPreferences(this).edit()

        prefsEditor.putBoolean(P.USE_12_HOUR_CLOCK, !DateFormat.is24HourFormat(this))
        prefsEditor.putInt(P.DOUBLE_TAP_SPEED, P.DOUBLE_TAP_SPEED_DEFAULT)
        prefsEditor.apply()

        setContent {
            BaseContent {
                var step by rememberSaveable { mutableIntStateOf(0) }

                Scaffold { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = step,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            modifier = Modifier.align(Alignment.Center),
                            label = "setup_step",
                        ) { currentStep ->
                            when (currentStep) {
                                0 -> SetupScreenContent(
                                    iconRes = R.drawable.ic_color_draw_over_other_apps,
                                    title = stringResource(R.string.setup_draw_over_other_apps),
                                    summary = stringResource(R.string.setup_draw_over_other_apps_summary),
                                )

                                1 -> SetupScreenContent(
                                    iconRes = R.drawable.ic_color_phone_state,
                                    title = stringResource(R.string.setup_phone_state),
                                    summary = stringResource(R.string.setup_phone_state_summary),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(onClick = {
                                when (step) {
                                    0 -> step = 1
                                    1 -> navigateToMain()
                                }
                            }) {
                                Text(stringResource(R.string.setup_skip))
                            }
                            TextButton(onClick = {
                                when (step) {
                                    0 -> onContinueDrawOverOtherApps { step = 1 }
                                    1 -> onContinuePhoneState(prefsEditor)
                                }
                            }) {
                                Text(stringResource(R.string.setup_continue))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (isActionRequired) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.setup_error, Toast.LENGTH_LONG).show()
            }
            isActionRequired = false
        }
    }

    private fun onContinueDrawOverOtherApps(onGranted: () -> Unit) {
        if (Settings.canDrawOverlays(this)) {
            onGranted()
        } else {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            isActionRequired = true
        }
    }

    private fun onContinuePhoneState(
        prefsEditor: android.content.SharedPreferences.Editor,
    ) {
        if (applicationContext.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        } else {
            prefsEditor.putBoolean("setup_complete", true).apply()
            finishSetup()
        }
    }

    private fun finishSetup() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit {
                putBoolean("setup_complete", true)
            }
        navigateToMain()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}