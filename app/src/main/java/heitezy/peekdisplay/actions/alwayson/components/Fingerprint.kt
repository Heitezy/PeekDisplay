package heitezy.peekdisplay.actions.alwayson.components

import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import heitezy.peekdisplay.actions.alwayson.data.FingerprintState
import heitezy.peekdisplay.actions.alwayson.data.AppearanceState
import heitezy.peekdisplay.actions.alwayson.data.InteractionState

@Composable
fun Fingerprint(
    fingerprintState: FingerprintState,
    appearanceState: AppearanceState,
    interactionState: InteractionState,
    onTouchStateChanged: (Boolean, Float, Float) -> Unit,
    onLongPress: () -> Unit,
    onPositioned: (Rect) -> Unit
) {
    if (!appearanceState.showFingerprint) return

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                bottom = if (isLandscape) 0.dp else appearanceState.fingerprintMargin.dp,
                end = if (isLandscape) appearanceState.fingerprintMargin.dp else 0.dp
            )
            .offset(y = interactionState.fpDriftY.dp),
        contentAlignment = if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter
    ) {
        Icon(
            painter = painterResource(id = fingerprintState.iconRes),
            contentDescription = null,
            tint = appearanceState.fingerprintColor,
            modifier = Modifier
                .size(64.dp)
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    val size = coordinates.size
                    onPositioned(
                        Rect(
                            position.x, position.y,
                            position.x + size.width, position.y + size.height
                        )
                    )
                }
                .pointerInput(appearanceState.fingerprintInteractionMode) {
                    if (appearanceState.fingerprintInteractionMode == "longpress") {
                        detectTapGestures(
                            onLongPress = { onLongPress() },
                            onPress = {
                                onTouchStateChanged(true, 0f, 0f)
                                tryAwaitRelease()
                                onTouchStateChanged(false, 0f, 0f)
                            }
                        )
                    } else {
                        var initialX = 0f
                        var initialY = 0f
                        var currentDx = 0f
                        var currentDy = 0f
                        detectDragGestures(
                            onDragStart = { offset ->
                                initialX = offset.x
                                initialY = offset.y
                                currentDx = 0f
                                currentDy = 0f
                                onTouchStateChanged(true, 0f, 0f)
                            },
                            onDrag = { change, dragAmount ->
                                currentDx = change.position.x - initialX
                                currentDy = change.position.y - initialY
                                onTouchStateChanged(true, currentDx, currentDy)
                            },
                            onDragEnd = { onTouchStateChanged(false, currentDx, currentDy) },
                            onDragCancel = { onTouchStateChanged(false, currentDx, currentDy) }
                        )
                    }
                }
        )
    }
}
