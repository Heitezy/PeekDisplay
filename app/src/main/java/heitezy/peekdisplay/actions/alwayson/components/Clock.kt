package heitezy.peekdisplay.actions.alwayson.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import heitezy.peekdisplay.actions.alwayson.data.ClockState
import heitezy.peekdisplay.actions.alwayson.data.AppearanceState
import heitezy.peekdisplay.actions.alwayson.styles.getThemeSettings
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Clock(clockState: ClockState, appearanceState: AppearanceState, modifier: Modifier = Modifier) {
    if (!appearanceState.showClock) return
    val themeSettings = getThemeSettings(appearanceState.theme)
    
    Box(modifier = modifier) {
        if (appearanceState.isAnalog) {
            AnalogClock(clockState, appearanceState, themeSettings.bigTextSize)
        } else {
            Text(
                text = clockState.time,
                color = appearanceState.clockColor,
                fontSize = themeSettings.bigTextSize,
                fontFamily = themeSettings.fontFamily,
                textAlign = appearanceState.textAlign,
                lineHeight = themeSettings.bigTextSize
            )
        }
    }
}

@Composable
private fun AnalogClock(clockState: ClockState, appearanceState: AppearanceState, clockSize: TextUnit) {
    val hours = clockState.hour % 12
    val minutes = clockState.minute
    
    val density = LocalDensity.current
    val circleSize = with(density) { (clockSize.toPx() * 1.5f).toDp() }

    Box(
        contentAlignment = Alignment.Center, modifier = Modifier
            .size(circleSize * 2)
            .padding(bottom = 16.dp)
    ) {
        Canvas(modifier = Modifier.size(circleSize * 2)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = circleSize.toPx()

            drawCircle(
                color = appearanceState.clockColor,
                center = center,
                radius = radius,
                style = Stroke(width = 4.dp.toPx())
            )

            // Hour hand
            val hourAngle = (Math.PI * (hours * 5 + minutes / 12f) / 30.0 - Math.PI / 2.0).toFloat()
            val hourHandLen = radius * 0.5f
            drawLine(
                color = appearanceState.clockColor,
                start = center,
                end = Offset(
                    center.x + cos(hourAngle) * hourHandLen,
                    center.y + sin(hourAngle) * hourHandLen
                ),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Minute hand
            val minuteAngle = (Math.PI * minutes / 30.0 - Math.PI / 2.0).toFloat()
            val minuteHandLen = radius * 0.9f
            drawLine(
                color = appearanceState.clockColor,
                start = center,
                end = Offset(
                    center.x + cos(minuteAngle) * minuteHandLen,
                    center.y + sin(minuteAngle) * minuteHandLen
                ),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
