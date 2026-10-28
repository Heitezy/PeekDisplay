package heitezy.peekdisplay.actions.alwayson.blinds

import android.graphics.Bitmap
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The screen is cut into [BlindsSettings.numberOfBlinds] horizontal slats. The few slats
 * around the touch point tilt in 3D (android.graphics.Camera), get lighter/darker depending on
 * their tilt, drop a shadow on the slat above and get a thin dark outline. A spring
 * ([SpringDynamics]) drives the tilt amount: it goes to 1000 while the finger is down and back
 * to 0 after release.
 *
 * Values are intentionally raw pixels / Camera units.
 */
internal data class BlindsSettings(
    val numberOfBlinds: Int,
    val blindsAffectedByTouch: Int,
    val maxSidewaysRotationDegrees: Float,
) {
    companion object {
        /** Resource qualifiers (default / sw600dp / sw600dp-land). */
        fun forScreen(smallestWidthDp: Int, landscape: Boolean): BlindsSettings =
            when {
                smallestWidthDp < TABLET_WIDTH_DP -> BlindsSettings(17, 5, 3.0f)
                landscape -> BlindsSettings(21, 6, 0.8f)
                else -> BlindsSettings(34, 6, 1.04f)
            }

        private const val TABLET_WIDTH_DP = 600
    }
}

/** Velocity-Verlet spring, fixed 10 ms sub-steps. */
internal class SpringDynamics {
    var position = 0f
        private set
    var velocity = 0f
        private set
    var minPosition = -Float.MAX_VALUE
    var maxPosition = Float.MAX_VALUE

    private var lastTime = 0L
    private var stiffness = 0f
    private var damping = 0f
    private val friction = 0f

    fun setSpring(stiffness: Float, dampingRatio: Float) {
        this.stiffness = stiffness
        damping = 2f * dampingRatio * sqrt(stiffness.toDouble()).toFloat()
    }

    fun setState(position: Float, velocity: Float, time: Long) {
        this.velocity = velocity
        this.position = position
        lastTime = time
    }

    fun update(now: Long) {
        var dt = (now - lastTime).toInt()
        if (dt > MAX_TIMESTEP) dt = MAX_TIMESTEP
        while (dt > 0) {
            onUpdate(min(dt, TIMESTEP))
            dt -= TIMESTEP
        }
        lastTime = now
    }

    fun isAtRest(maxVelocity: Float, maxDistance: Float): Boolean {
        val velocityOk = abs(velocity) < maxVelocity
        val positionOk = position - maxDistance < maxPosition && position + maxDistance > minPosition
        return velocityOk && positionOk
    }

    private fun distanceToLimit(): Float =
        when {
            position > maxPosition -> maxPosition - position
            position < minPosition -> minPosition - position
            else -> 0f
        }

    private fun acceleration(): Float {
        val distance = distanceToLimit()
        return if (distance != 0f) stiffness * distance - damping * velocity else -friction * velocity
    }

    private fun onUpdate(stepMs: Int) {
        val dt = stepMs.toFloat() / 1000f
        val a = acceleration()
        position += velocity * dt + 0.5f * a * dt * dt
        velocity += 0.5f * a * dt
        val a2 = acceleration()
        velocity += 0.5f * a2 * dt
    }

    private companion object {
        const val TIMESTEP = 10
        const val MAX_TIMESTEP = 500
    }
}

/**
 * Touch behavior (press / drag / release).
 * [pressX] and [pressY] are normalized (0..1) touch coordinates.
 */
internal class BlindsDrawer(settings: BlindsSettings) {
    var pressX = 0f
    var pressY = 0f

    private val numberOfBlinds = settings.numberOfBlinds
    private val blindHeight = 1f / numberOfBlinds
    private val blindsAffectedSize = settings.blindsAffectedByTouch.toFloat() / numberOfBlinds.toFloat()
    private val maxSidewaysRotationDegrees = settings.maxSidewaysRotationDegrees

    private val spring = SpringDynamics().apply { setSpring(SPRING_STIFFNESS, SPRING_DAMPING) }

    private var applySpeed = false
    private var pressXVel = 0f
    private var pressYVel = 0f
    private var startPressX = 0f
    private var startPressY = 0f
    private var speedStartTime = 0L

    private val paint = Paint().apply { isAntiAlias = true }
    private val strokePaint = Paint().apply {
        color = STROKE_COLOR
        isAntiAlias = true
        strokeWidth = 2f
    }
    private val shadowRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = 0
        isAntiAlias = true
    }
    private val camera = Camera()
    private val drawMatrix = Matrix()
    private val tempMatrix = Matrix()
    private val sourceRect = Rect()

    private val lighteningFilters = Array(FILTER_COUNT) {
        PorterDuffColorFilter(Color.argb(it, 255, 255, 255), PorterDuff.Mode.OVERLAY)
    }
    private val darkeningFilters = Array(FILTER_COUNT) {
        val level = 255 - it
        LightingColorFilter(Color.rgb(level, level, level), 0)
    }

    init {
        setPressed(false)
    }

    /** Finger down -> spring target 1000 (blinds tilt); finger up -> target 0 (blinds relax). */
    fun setPressed(pressed: Boolean) {
        val target = if (pressed) SPRING_PRESSED else 0f
        spring.minPosition = target
        spring.maxPosition = target
    }

    fun startAnimation(now: Long) {
        spring.setState(spring.position, spring.velocity, now)
    }

    fun disableApplySpeed() {
        applySpeed = false
    }

    /** After release the touch point keeps moving with the finger's release velocity. */
    fun enableApplySpeed(velX: Float, velY: Float, now: Long) {
        pressXVel = velX
        pressYVel = velY
        applySpeed = true
        speedStartTime = now
        startPressX = pressX
        startPressY = pressY
    }

    /** @return true when the spring has come to rest. */
    fun update(now: Long): Boolean {
        if (applySpeed) {
            val t = (now - speedStartTime).toFloat() / 1000f
            pressX = startPressX + pressXVel * t
            pressY = startPressY + pressYVel * t
        }
        spring.update(now)
        return spring.isAtRest(REST_VELOCITY, REST_DISTANCE)
    }

    fun drawBlinds(canvas: Canvas, bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        canvas.drawColor(BACKGROUND_LAYER_COLOR)

        val top = pressY - blindsAffectedSize / 2f
        val bottom = pressY + blindsAffectedSize / 2f
        var first = floor((top / blindHeight + 0.5f).toDouble()).toInt()
        first = min(max(first, 0), numberOfBlinds - 1)
        var last = ceil((bottom / blindHeight - 0.5f).toDouble()).toInt()
        last = min(max(last, 0), numberOfBlinds)
        val middle = (last - first) / 2 + first

        for (i in middle downTo first) drawAffectedBlind(canvas, bitmap, i)
        for (i in middle + 1 until last) drawAffectedBlind(canvas, bitmap, i)

        drawUnaffectedBlinds(canvas, bitmap, 0, first)
        drawUnaffectedBlinds(canvas, bitmap, last, numberOfBlinds)
    }

    private fun calculateRect(width: Int, height: Int, from: Int, to: Int): Rect {
        val top = ((from * height).toFloat() * blindHeight).toInt()
        val bottom = ((to * height).toFloat() * blindHeight).toInt()
        sourceRect.set(0, top, width, bottom)
        return sourceRect
    }

    private fun drawUnaffectedBlinds(canvas: Canvas, bitmap: Bitmap, from: Int, to: Int) {
        val rect = calculateRect(bitmap.width, bitmap.height, from, to)
        if (rect.height() > 0) canvas.drawBitmap(bitmap, rect, rect, null)
    }

    private fun drawAffectedBlind(canvas: Canvas, bitmap: Bitmap, index: Int) {
        val rect = calculateRect(bitmap.width, bitmap.height, index, index + 1)

        val center = blindHeight * index + blindHeight / 2f
        val x = 2f * (center - pressY) / blindsAffectedSize

        val rotation = mapToRotation(x) * spring.position / 1000f
        val forwardRotation = rotation * FORWARD_ROTATION_DEGREES
        val depth = mapToDepthOffset(x) * spring.position / 1000f

        drawMatrix.setTranslate(-rect.centerX().toFloat(), -rect.centerY().toFloat())

        val pivotX = if (pressX < 0.5f) rect.centerX().toFloat() else (-rect.centerX()).toFloat()
        val sidewaysRotation = (0.5f - pressX) * maxSidewaysRotationDegrees * depth
        drawMatrix.postRotate(sidewaysRotation, pivotX, rect.width().toFloat() / 2f)

        camera.save()
        camera.translate(0f, 0f, DEPTH_OFFSET * depth)
        camera.rotateX(forwardRotation)
        camera.getMatrix(tempMatrix)
        camera.restore()
        drawMatrix.postConcat(tempMatrix)
        drawMatrix.postTranslate(rect.centerX().toFloat(), rect.centerY().toFloat())

        // Tilted away -> darker, tilted towards the viewer -> lighter.
        paint.colorFilter = if (rotation > 0f) {
            val level = min((99f * rotation * LIGHTING_FACTOR).toInt(), FILTER_COUNT - 1)
            lighteningFilters[max(level, 0)]
        } else {
            val level = min((-rotation * 99f * LIGHTING_FACTOR).toInt(), FILTER_COUNT - 1)
            darkeningFilters[max(level, 0)]
        }

        canvas.save()
        canvas.concat(drawMatrix)

        canvas.drawBitmap(bitmap, rect, rect, paint)

        if (spring.position > DROP_SHADOW_MIN_SPRING) {
            val shadowHeight = (1f - abs(x)) * MAX_DROP_SHADOW_HEIGHT
            shadowRectPaint.color = Color.BLACK
            shadowRectPaint.shader = LinearGradient(
                rect.left.toFloat(),
                rect.top.toFloat() - 0.5f,
                rect.left.toFloat(),
                rect.top.toFloat() + shadowHeight,
                SHADOW_COLOR,
                0,
                Shader.TileMode.CLAMP,
            )
            val shadowRect = Rect(
                rect.left,
                (rect.top.toFloat() - 0.5f).toInt(),
                rect.right,
                (rect.top.toFloat() + shadowHeight).toInt(),
            )
            canvas.drawRect(shadowRect, shadowRectPaint)
        }

        if (depth > OUTLINE_MIN_DEPTH) {
            canvas.drawLine(
                rect.left.toFloat(), rect.top.toFloat() - 1f,
                rect.right.toFloat(), rect.top.toFloat() - 1f,
                strokePaint,
            )
            canvas.drawLine(
                rect.left.toFloat(), rect.bottom.toFloat(),
                rect.right.toFloat(), rect.bottom.toFloat(),
                strokePaint,
            )
        }

        canvas.restore()
    }

    private fun mapToRotation(x: Float): Float = sin(x.toDouble() * PI).toFloat()

    private fun mapToDepthOffset(x: Float): Float = (cos(x.toDouble() * PI) + 1.0).toFloat()

    private companion object {
        const val SPRING_STIFFNESS = 400f
        const val SPRING_DAMPING = 0.85f
        const val SPRING_PRESSED = 1000f
        const val REST_VELOCITY = 0.1f
        const val REST_DISTANCE = 10f

        const val FORWARD_ROTATION_DEGREES = 17f
        const val DEPTH_OFFSET = 3f
        const val LIGHTING_FACTOR = 2f
        const val MAX_DROP_SHADOW_HEIGHT = 50f
        const val DROP_SHADOW_MIN_SPRING = 500f
        const val OUTLINE_MIN_DEPTH = 0.1f
        const val FILTER_COUNT = 100

        val BACKGROUND_LAYER_COLOR = 0x9F000000.toInt()
        val SHADOW_COLOR = 0xBB000000.toInt()
        val STROKE_COLOR = 0xFF2B2B2B.toInt()
    }
}
