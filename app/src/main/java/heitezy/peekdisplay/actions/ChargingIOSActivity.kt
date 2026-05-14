package heitezy.peekdisplay.actions

import android.content.Intent
import android.content.IntentFilter
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import heitezy.peekdisplay.R
import heitezy.peekdisplay.helpers.IconHelper
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class ChargingIOSActivity : OffActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        setContent {
            OffContent {
                ChargingIOSScreen(
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
    private fun ChargingIOSScreen(onFinish: () -> Unit) {
        val context = LocalContext.current
        var level by remember { mutableIntStateOf(0) }
        val alphaAnim = remember { Animatable(1f) }
        val yOffsetAnim = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0

            val size = Point()
            (context.getSystemService(DISPLAY_SERVICE) as DisplayManager)
                .getDisplay(Display.DEFAULT_DISPLAY)
                .getSize(size)

            val screenHeightPx = size.y.toFloat()
            yOffsetAnim.snapTo(screenHeightPx / FRACTIONAL_VIEW_POSITION)

            delay(ANIMATION_DELAY)
            alphaAnim.animateTo(0f, animationSpec = tween(ANIMATION_DURATION.toInt()))
            onFinish()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alphaAnim.value)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, yOffsetAnim.value.roundToInt()) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = IconHelper.getBatteryIcon(level)),
                    contentDescription = null,
                    modifier = Modifier
                        .size(192.dp)
                        .rotate(90f)
                        .alpha(0.5f),
                    colorFilter = ColorFilter.tint(colorResource(id = R.color.charging))
                )

                Text(
                    text = stringResource(id = R.string.charged, level),
                    color = Color.White,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    companion object {
        private const val ANIMATION_DELAY = 3000L
        private const val ANIMATION_DURATION = 1000L
        private const val FRACTIONAL_VIEW_POSITION = 8f
    }
}
