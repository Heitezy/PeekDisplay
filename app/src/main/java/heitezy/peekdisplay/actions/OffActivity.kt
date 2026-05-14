package heitezy.peekdisplay.actions

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.preference.PreferenceManager
import heitezy.peekdisplay.R
import heitezy.peekdisplay.helpers.Root
import heitezy.peekdisplay.receivers.AdminReceiver

@SuppressLint("Registered")
open class OffActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation =
            when (
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("orientation", "locked")
            ) {
                "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_LOCKED
            }
        super.onCreate(savedInstanceState)
    }

    @Composable
    fun OffContent(content: @Composable () -> Unit) {
        MaterialTheme(colorScheme = darkColorScheme(background = Color.Black, surface = Color.Black)) {
            Surface(color = Color.Black) {
                content()
            }
        }
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                (getSystemService(AUDIO_SERVICE) as AudioManager)
                    .adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                (getSystemService(AUDIO_SERVICE) as AudioManager)
                    .adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
            }
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .moveTaskToFront(taskId, 0)
    }

    protected fun turnOnScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    protected open fun finishAndOff() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("root_mode", false)) {
            Root.shell("input keyevent KEYCODE_POWER")
        } else {
            val policyManager =
                getSystemService(DEVICE_POLICY_SERVICE)
                    as DevicePolicyManager
            if (policyManager.isAdminActive(ComponentName(this, AdminReceiver::class.java))) {
                policyManager.lockNow()
            } else {
                Toast.makeText(this, R.string.pref_admin_summary, Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }
}
