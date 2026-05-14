package heitezy.peekdisplay.actions

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import heitezy.peekdisplay.R
import kotlinx.coroutines.delay

class ChargingCircleActivity : OffActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        setContent {
            OffContent {
                ChargingCircleScreen(
                    onFinish = { finishAndOff() }
                )
            }
        }

        turnOnScreen()
        
        // Hide UI and disable edge gestures
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    @Composable
    private fun ChargingCircleScreen(onFinish: () -> Unit) {
        val context = LocalContext.current
        var level by remember { mutableIntStateOf(0) }
        val alphaAnim = remember { Animatable(1f) }

        LaunchedEffect(Unit) {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0

            delay(ANIMATION_DELAY)
            alphaAnim.animateTo(0f, animationSpec = tween(ANIMATION_DURATION.toInt()))
            onFinish()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alphaAnim.value),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { level / 100f },
                modifier = Modifier.size(192.dp),
                color = colorResource(R.color.charging),
                strokeWidth = 4.dp,
                trackColor = colorResource(R.color.progressBackground)
            )

            Text(
                text = stringResource(id = R.string.percent, level),
                color = Color.White,
                fontSize = 24.sp
            )
        }
    }

    companion object {
        private const val ANIMATION_DELAY = 3000L
        private const val ANIMATION_DURATION = 1000L
    }
}
