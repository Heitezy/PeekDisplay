package heitezy.peekdisplay.ui


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp


@Composable
fun CustomHSVPicker(
    color: Color,
    onColorChanged: (Color) -> Unit
) {
    val initialHsv = remember {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), arr)
        arr
    }

    var hue        by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value      by remember { mutableFloatStateOf(initialHsv[2]) }
    var alpha      by remember { mutableFloatStateOf(color.alpha) }

    var lastEmitted by remember { mutableIntStateOf(color.toArgb()) }

    val incomingArgb = color.toArgb()
    if (incomingArgb != lastEmitted) {
        lastEmitted = incomingArgb
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(incomingArgb, arr)
        hue        = arr[0]
        saturation = arr[1]
        value      = arr[2]
        alpha      = color.alpha
    }

    fun emit() {
        val emitted = Color.hsv(hue, saturation, value, alpha)
        lastEmitted = emitted.toArgb()
        onColorChanged(emitted)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // SV Selection Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .pointerInput(hue) {
                        detectTapGestures { offset ->
                            saturation = (offset.x / size.width).coerceIn(0f, 1f)
                            value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                            emit()
                        }
                    }
                    .pointerInput(hue) {
                        detectDragGestures { change, _ ->
                            saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                            value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            emit()
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = Color.hsv(hue, 1f, 1f))
                    drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                    drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    drawCircle(
                        color = if (value > 0.5f) Color.Black else Color.White,
                        radius = 8.dp.toPx(),
                        center = Offset(
                            x = saturation * size.width,
                            y = (1f - value) * size.height
                        ),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Vertical Hue Bar
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            hue = (offset.y / size.height).coerceIn(0f, 1f) * 360f
                            emit()
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            hue = (change.position.y / size.height).coerceIn(0f, 1f) * 360f
                            emit()
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val hueColors = listOf(
                        Color.Red, Color.Yellow, Color.Green,
                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    )
                    drawRect(brush = Brush.verticalGradient(hueColors))
                    val yPos = (hue / 360f) * size.height
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(0f, yPos - 2.dp.toPx()),
                        size = Size(size.width, 4.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // Horizontal Alpha Bar
        val opaqueColor = Color.hsv(hue, saturation, value, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        alpha = (offset.x / size.width).coerceIn(0f, 1f)
                        emit()
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        alpha = (change.position.x / size.width).coerceIn(0f, 1f)
                        emit()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(brush = Brush.horizontalGradient(listOf(Color.Transparent, opaqueColor)))
                val xPos = alpha * size.width
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(xPos - 2.dp.toPx(), 0f),
                    size = Size(4.dp.toPx(), size.height),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}