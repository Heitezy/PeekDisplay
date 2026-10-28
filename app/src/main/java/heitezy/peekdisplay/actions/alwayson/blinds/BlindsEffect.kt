package heitezy.peekdisplay.actions.alwayson.blinds

import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import heitezy.peekdisplay.helpers.Global
import kotlinx.coroutines.CancellationException

/** Everything the drawing surface needs; shared with [BlindsBackground] in background-only mode. */
internal class BlindsHost(
    val state: BlindsState,
    val layer: GraphicsLayer,
)

/** Set only while the effect is limited to the background image. */
internal val LocalBlindsBackground = compositionLocalOf<BlindsHost?> { null }

/**
 * Wraps [content] and, while the user touches the screen, replaces it with the
 * "blinds" (jalousie) rendering of a snapshot of [content]. When [enabled] is false this is a
 * plain [Box] and nothing else changes.
 *
 * With [backgroundOnly] the touch is still tracked on the whole screen, but only the part of
 * [content] wrapped in [BlindsBackground] is replaced; the rest stays flat.
 *
 * [onActiveChanged] is called with true when the effect starts and false when it has settled,
 * so the host can keep the display refresh rate high while it animates.
 */
@Composable
internal fun BlindsEffect(
    enabled: Boolean,
    backgroundOnly: Boolean,
    onActiveChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!enabled) {
        Box(modifier = modifier, content = content)
        return
    }

    val configuration = LocalConfiguration.current
    val settings = remember(configuration.smallestScreenWidthDp, configuration.orientation) {
        BlindsSettings.forScreen(
            smallestWidthDp = configuration.smallestScreenWidthDp,
            landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
        )
    }
    val currentOnActiveChanged by rememberUpdatedState(onActiveChanged)
    val state = remember(settings) { BlindsState(settings) { currentOnActiveChanged(it) } }
    val layer = rememberGraphicsLayer()
    val host = remember(state, layer) { BlindsHost(state, layer) }

    // (Re)take the snapshot of the content whenever it is requested.
    LaunchedEffect(state.captureRequest) {
        if (state.captureRequest > 0) state.capture(layer)
    }
    // Animation loop, runs only while the effect is visible.
    LaunchedEffect(state.active) {
        while (state.active) {
            withFrameNanos { state.onFrame() }
        }
    }

    Box(modifier = modifier.pointerInput(state) { detectBlindsGestures(state) }) {
        if (backgroundOnly) {
            CompositionLocalProvider(LocalBlindsBackground provides host) {
                content()
            }
        } else {
            BlindsSurface(host, Modifier.fillMaxSize(), content)
        }
    }
}

/**
 * Marks the part of the screen that gets the effect when "background only" is on. Without it
 * (or with the effect off) this is a transparent pass-through and layout is unchanged.
 */
@Composable
internal fun BoxScope.BlindsBackground(content: @Composable BoxScope.() -> Unit) {
    val host = LocalBlindsBackground.current
    if (host == null) {
        content()
    } else {
        BlindsSurface(host, Modifier.fillMaxSize(), content)
    }
}

/** Draws [content] normally, or as blinds from a snapshot while the effect is active. */
@Composable
private fun BlindsSurface(
    host: BlindsHost,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = host.state
    val layer = host.layer
    Box(
        modifier = modifier
            // Own layer, so that animation frames don't re-record the rest of the screen.
            .graphicsLayer { }
            .drawWithContent {
                // Reading `frame` re-runs this block on every animation frame.
                @Suppress("UNUSED_VARIABLE")
                val frame = state.frame
                val snapshot = state.snapshot
                // The recorder below draws nothing itself, but drawing the child is what keeps
                // `layer` up to date.
                drawContent()
                if (state.active && snapshot != null) {
                    drawIntoCanvas { state.drawer.drawBlinds(it.nativeCanvas, snapshot) }
                } else {
                    drawLayer(layer)
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Own layer, so that animation frames don't re-record the (static) content.
                .graphicsLayer { }
                .drawWithContent {
                    layer.record {
                        // Opaque black base: the blinds lighting filters would otherwise tint
                        // transparent pixels.
                        drawRect(Color.Black)
                        this@drawWithContent.drawContent()
                    }
                    state.onContentRecorded()
                },
            content = content,
        )
    }
}

private suspend fun PointerInputScope.detectBlindsGestures(state: BlindsState) {
    awaitPointerEventScope {
        var trackedId: PointerId? = null
        while (true) {
            // Initial pass + never consuming: the existing AOD gestures are not affected.
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val id = trackedId
            if (id == null) {
                val down = event.changes.firstOrNull { it.pressed && !it.previousPressed }
                if (down != null) {
                    trackedId = down.id
                    state.onDown(down.position, size, down.uptimeMillis)
                }
            } else {
                val change = event.changes.firstOrNull { it.id == id }
                if (change == null || !change.pressed) {
                    trackedId = null
                    state.onUp(size)
                } else {
                    state.onMove(change.position, size, change.uptimeMillis)
                }
            }
        }
    }
}

/** Touch / drawing-cache handling. */
internal class BlindsState(
    settings: BlindsSettings,
    private val onActiveChanged: (Boolean) -> Unit,
) {
    val drawer = BlindsDrawer(settings)

    /** The blinds are being drawn. */
    var active by mutableStateOf(false)
        private set

    /** Bumped every frame to invalidate drawing. */
    var frame by mutableLongStateOf(0L)
        private set

    /** Offscreen copy of the content. */
    var snapshot by mutableStateOf<Bitmap?>(null)
        private set

    var captureRequest by mutableIntStateOf(0)
        private set

    private val velocityTracker = VelocityTracker()
    private var touching = false
    private var capturing = false
    private var contentChanged = false
    private var lastCaptureTime = 0L

    fun onContentRecorded() {
        contentChanged = true
    }

    fun onDown(position: Offset, size: IntSize, uptimeMillis: Long) {
        if (size.width <= 0 || size.height <= 0) return
        velocityTracker.resetTracking()
        velocityTracker.addPosition(uptimeMillis, position)
        drawer.setPressed(true)
        drawer.pressX = position.x / size.width
        drawer.pressY = position.y / size.height
        drawer.startAnimation(now())
        drawer.disableApplySpeed()
        touching = true
        contentChanged = false
        captureRequest++
        if (!active) {
            active = true
            onActiveChanged(true)
        }
    }

    fun onMove(position: Offset, size: IntSize, uptimeMillis: Long) {
        if (size.width <= 0 || size.height <= 0) return
        velocityTracker.addPosition(uptimeMillis, position)
        drawer.pressX = position.x / size.width
        drawer.pressY = position.y / size.height
    }

    fun onUp(size: IntSize) {
        if (size.width <= 0 || size.height <= 0) return
        val velocity = velocityTracker.calculateVelocity()
        drawer.setPressed(false)
        drawer.disableApplySpeed()
        drawer.enableApplySpeed(velocity.x / size.width, velocity.y / size.height, now())
        touching = false
    }

    fun onFrame() {
        val atRest = drawer.update(now())
        if (atRest && !touching) {
            stop()
        } else if (contentChanged && !capturing && now() - lastCaptureTime > RECAPTURE_INTERVAL_MS) {
            // Content changed while blinds are shown (e.g. notification preview): refresh.
            contentChanged = false
            captureRequest++
        }
        frame++
    }

    suspend fun capture(layer: GraphicsLayer) {
        capturing = true
        try {
            if (layer.size.width <= 0 || layer.size.height <= 0) {
                // Nothing was recorded (e.g. background-only mode without a background image).
                stop()
                return
            }
            val bitmap = layer.toImageBitmap().asAndroidBitmap()
            if (!active) return // released before the snapshot was ready
            val first = snapshot == null
            snapshot = bitmap
            lastCaptureTime = now()
            // Start the spring when the blinds actually become visible.
            if (first && touching) drawer.startAnimation(now())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(Global.LOG_TAG, "Blinds snapshot failed", e)
            if (snapshot == null) stop()
        } finally {
            capturing = false
        }
    }

    private fun stop() {
        snapshot = null
        if (active) {
            active = false
            onActiveChanged(false)
        }
    }

    private fun now(): Long = AnimationUtils.currentAnimationTimeMillis()

    private companion object {
        const val RECAPTURE_INTERVAL_MS = 150L
    }
}
