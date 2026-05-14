package heitezy.peekdisplay.actions

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import heitezy.peekdisplay.R
import kotlinx.coroutines.delay

class ChargingFlashActivity : OffActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        setContent {
            OffContent {
                ChargingFlashScreen(
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
    private fun ChargingFlashScreen(onFinish: () -> Unit) {
        val alphaAnim = remember { Animatable(1f) }

        LaunchedEffect(Unit) {
            delay(ANIMATION_DELAY)
            alphaAnim.animateTo(0f, animationSpec = tween(ANIMATION_DURATION.toInt()))
            onFinish()
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_charging_white),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .alpha(alphaAnim.value)
            )
        }
    }

    companion object {
        private const val ANIMATION_DELAY = 1500L
        private const val ANIMATION_DURATION = 1000L
    }
}
