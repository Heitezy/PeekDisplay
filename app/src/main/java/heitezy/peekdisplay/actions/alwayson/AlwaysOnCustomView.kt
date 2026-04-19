package heitezy.peekdisplay.actions.alwayson

import android.app.Activity
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.core.graphics.scale
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.data.Data
import heitezy.peekdisplay.actions.alwayson.draw.Battery
import heitezy.peekdisplay.actions.alwayson.draw.Clock
import heitezy.peekdisplay.actions.alwayson.draw.Date
import heitezy.peekdisplay.actions.alwayson.draw.Message
import heitezy.peekdisplay.actions.alwayson.draw.MusicControls
import heitezy.peekdisplay.actions.alwayson.draw.NotificationCount
import heitezy.peekdisplay.actions.alwayson.draw.NotificationIcons
import heitezy.peekdisplay.actions.alwayson.draw.NotificationPreview
import heitezy.peekdisplay.actions.alwayson.draw.ThemeSpecials
import heitezy.peekdisplay.actions.alwayson.draw.Utils
import heitezy.peekdisplay.actions.alwayson.draw.Weather
import heitezy.peekdisplay.helpers.Global
import heitezy.peekdisplay.helpers.IconHelper
import heitezy.peekdisplay.helpers.KeyguardHelper
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.services.NotificationService
import java.lang.Integer.max
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

@Suppress("TooManyFunctions")
class AlwaysOnCustomView : View {
    private lateinit var utils: Utils
    private lateinit var timeFormat: SimpleDateFormat
    private lateinit var dateFormat: SimpleDateFormat
    private var customBackground: Bitmap? = null
    private var batteryIsCharging = false
    private var batteryLevel = 0
    private var batteryIcon = R.drawable.ic_battery_unknown
    private var events = listOf<String>()
    private var weather = ""
    private var albumArt: Bitmap? = null
    private var isAlbumArtOverlayVisible: Boolean = false
    private var alwaysOnActivity: Activity? = null
    private var isFingerprintTouched = false
    private var lastTouchedX = 0f
    private var lastTouchedY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var longPressDetected = false
    private var isNotificationTouched = false
    internal var touchedNotificationIndex: Int? = null
    private var isReplyModeActive = false
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val longPressHandler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        longPressDetected = true
        // If touching a notification icon, show preview
        touchedNotificationIndex?.let { index ->
            NotificationPreview.setCurrentNotification(index)
            isNotificationTouched = true
            invalidate()
        }
    }

    // Keyboard inactivity timeout
    private val keyboardTimeoutHandler = Handler(Looper.getMainLooper())
    private val keyboardTimeoutRunnable = Runnable {
        if (NotificationPreview.isReplyActive()) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken, 0)
            NotificationPreview.clearReplyMode()
            touchedNotificationIndex = null
            NotificationPreview.setCurrentNotification(null)
            invalidate()
            
            // Resume timeout if it was paused
            AlwaysOn.getInstance()?.resumeTimeout()
            AlwaysOn.getInstance()?.resetTimeout()
        }
    }
    private val KEYBOARD_TIMEOUT_DELAY = 60000L // 1 minute in milliseconds
    
    // Reset keyboard timeout on user interaction
    private fun resetKeyboardTimeout() {
        keyboardTimeoutHandler.removeCallbacks(keyboardTimeoutRunnable)
        if (NotificationPreview.isReplyActive()) {
            keyboardTimeoutHandler.postDelayed(keyboardTimeoutRunnable, KEYBOARD_TIMEOUT_DELAY)
        }
    }

    var musicVisible: Boolean = false
        set(value) {
            field = value
            checkAlbumArtState()
            invalidate()
        }
    var musicString: String = ""
        set(value) {
            field = value
            invalidate()
        }

    @JvmField
    var onSkipPreviousClicked: () -> Unit = {}

    @JvmField
    var onSkipNextClicked: () -> Unit = {}

    @JvmField
    var onTitleClicked: () -> Unit = {}

    @JvmField
    var onAlbumArtStateChanged: (Boolean, Bitmap?) -> Unit = { _, _ -> }

    private var skipPositions = intArrayOf(0, 0, 0)

    private val flags = booleanArrayOf(false, false, false, false, false, false)

    @JvmField
    internal val updateHandler = Handler(Looper.getMainLooper())

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle,
    ) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    @Suppress("MagicNumber", "CyclomaticComplexMethod", "LongMethod")
    private fun prepareTheme() {
        when (utils.prefs.get(P.USER_THEME, P.USER_THEME_DEFAULT)) {
            P.USER_THEME_MOTO -> {
                utils.bigTextSize = utils.spToPx(75f)
                utils.mediumTextSize = utils.spToPx(25f)
                utils.smallTextSize = utils.spToPx(18f)
                utils.setFont(R.font.roboto_light)
                flags[FLAG_MOTO] = true
            }

            P.USER_THEME_ONEPLUS -> {
                utils.bigTextSize = utils.spToPx(75f)
                utils.mediumTextSize = utils.spToPx(20f)
                utils.smallTextSize = utils.spToPx(15f)
                utils.setFont(R.font.roboto_medium)
                flags[FLAG_MULTILINE_CLOCK] = true
            }

            P.USER_THEME_SAMSUNG2 -> {
                utils.bigTextSize = utils.spToPx(36f)
                utils.mediumTextSize = utils.spToPx(18f)
                utils.smallTextSize = utils.spToPx(16f)
                utils.setFont(R.font.roboto_light)
                utils.paint.textAlign = Paint.Align.LEFT
                flags[FLAG_SAMSUNG_2] = true
            }

            else -> {
                utils.bigTextSize = utils.spToPx(75f)
                utils.mediumTextSize = utils.spToPx(25f)
                utils.smallTextSize = utils.spToPx(18f)
                when (utils.prefs.get(P.USER_THEME, P.USER_THEME_DEFAULT)) {
                    P.USER_THEME_GOOGLE -> {
                        utils.setFont(R.font.roboto_regular)
                    }
                    P.USER_THEME_SAMSUNG -> {
                        utils.setFont(R.font.roboto_light)
                        flags[FLAG_MULTILINE_CLOCK] = true
                        flags[FLAG_CAPS_DATE] = true
                    }
                    P.USER_THEME_SAMSUNG3 -> {
                        utils.setFont(R.font.roboto_regular)
                        flags[FLAG_SAMSUNG_3] = true
                    }
                    P.USER_THEME_80S -> {
                        utils.setFont(R.font.monoton_regular)
                    }
                    P.USER_THEME_FAST -> {
                        utils.setFont(R.font.faster_one_regular)
                    }
                    P.USER_THEME_FLOWER -> {
                        utils.setFont(R.font.akronim_regular)
                    }
                    P.USER_THEME_GAME -> {
                        utils.setFont(R.font.vt323_regular)
                    }
                    P.USER_THEME_HANDWRITTEN -> {
                        utils.setFont(R.font.patrick_hand_regular)
                    }
                    P.USER_THEME_JUNGLE -> {
                        utils.setFont(R.font.hanalei_regular)
                    }
                    P.USER_THEME_WESTERN -> {
                        utils.setFont(R.font.ewert_regular)
                    }
                    P.USER_THEME_ANALOG -> {
                        utils.setFont(R.font.roboto_regular)
                        flags[FLAG_MULTILINE_CLOCK] = true
                        flags[FLAG_ANALOG_CLOCK] = true
                    }
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun prepareBackground() {
        if (utils.prefs.get(
                P.BACKGROUND_IMAGE,
                P.BACKGROUND_IMAGE_DEFAULT,
            ) != P.BACKGROUND_IMAGE_NONE
        ) {
            if (utils.prefs.get(
                    P.BACKGROUND_IMAGE,
                    P.BACKGROUND_IMAGE_DEFAULT,
                ) == P.BACKGROUND_IMAGE_CUSTOM
            ) {
                val decoded = Base64.decode(utils.prefs.get(P.CUSTOM_BACKGROUND, ""), 0)
                customBackground = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            } else {
                val backgroundId = utils.prefs.backgroundImage()
                if (backgroundId != null) {
                    customBackground =
                        BitmapFactory.decodeResource(
                            resources, backgroundId,
                        )
                }
            }
        } else {
            // Clear background if set to "none"
            customBackground = null
        }
    }

    private fun prepareDateFormats() {
        timeFormat =
            SimpleDateFormat(
                if (utils.prefs.get(
                        P.USER_THEME,
                        P.USER_THEME_DEFAULT,
                    ) == P.USER_THEME_SAMSUNG || utils.prefs.get(
                        P.USER_THEME,
                        P.USER_THEME_DEFAULT,
                    ) == P.USER_THEME_ONEPLUS
                ) {
                    utils.prefs.getMultiLineTimeFormat()
                } else {
                    utils.prefs.getSingleLineTimeFormat()
                },
                Locale.getDefault(),
            )
        dateFormat =
            SimpleDateFormat(
                utils.prefs.get(P.DATE_FORMAT, P.DATE_FORMAT_DEFAULT),
                Locale.getDefault(),
            )
    }

    private fun prepareWeather() {
        if (utils.prefs.get(P.SHOW_WEATHER, P.SHOW_WEATHER_DEFAULT)) {
            Volley.newRequestQueue(context)
                .add(
                    object : StringRequest(
                        Method.GET,
                        utils.prefs.getWeatherUrl(),
                        { response ->
                            weather = response
                            invalidate()
                        },
                        {
                            Log.e(Global.LOG_TAG, it.toString())
                        },
                    ) {
                        override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                            val parsed = String(response.data, Charsets.UTF_8)
                            return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response))
                        }
                    },
                )
        }
    }

    private fun init(context: Context) {
        utils = Utils(context)
        utils.paint = Paint(Paint.ANTI_ALIAS_FLAG)
        utils.paint.textAlign = Paint.Align.CENTER

        prepareTheme()
        prepareBackground()
        prepareDateFormats()
        events = Data.getCalendar(utils)
        prepareWeather()
        
        // Make the view focusable to receive keyboard input
        isFocusable = true
        isFocusableInTouchMode = true
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
    private fun measureHeight(): Int {
        utils.viewHeight = 0f
        utils.viewHeight += paddingTop
        utils.viewHeight += utils.topPadding

        val tempHeight = utils.viewHeight
        if (utils.prefs.get(P.SHOW_CLOCK, P.SHOW_CLOCK_DEFAULT)) {
            utils.viewHeight += utils.padding16 + utils.padding2 +
                utils.getTextHeight(utils.bigTextSize).run {
                    if (flags[FLAG_MULTILINE_CLOCK]) {
                        this * 2
                    } else {
                        this
                    }
                }
        }
        if (utils.prefs.get(P.SHOW_DATE, P.SHOW_DATE_DEFAULT)) {
            if (flags[FLAG_SAMSUNG_3]) {
                utils.viewHeight =
                    tempHeight + utils.getTextHeight(utils.bigTextSize) + utils.padding16
            } else {
                utils.viewHeight += utils.getTextHeight(
                    if (flags[FLAG_BIG_DATE]) utils.bigTextSize else utils.mediumTextSize,
                ) + 2 * utils.padding2
            }
        }
        if (
            utils.prefs.get(P.SHOW_BATTERY_ICON, P.SHOW_BATTERY_ICON_DEFAULT) ||
            utils.prefs.get(P.SHOW_BATTERY_PERCENTAGE, P.SHOW_BATTERY_PERCENTAGE_DEFAULT)
        ) {
            utils.viewHeight += utils.getTextHeight(utils.mediumTextSize) + 2 * utils.padding16
        }
        if (utils.prefs.get(P.SHOW_MUSIC_CONTROLS, P.SHOW_MUSIC_CONTROLS_DEFAULT)) {
            utils.viewHeight += utils.getTextHeight(utils.smallTextSize) + 2 * utils.padding2
        }
        if (utils.prefs.get(P.SHOW_CALENDAR, P.SHOW_CALENDAR_DEFAULT)) {
            utils.viewHeight += 2 * utils.padding16 + events.size * (
                utils.getTextHeight(utils.smallTextSize) + 2 * utils.padding2
            )
        }
        if (utils.prefs.get(P.MESSAGE, P.MESSAGE_DEFAULT) != "") {
            utils.viewHeight += utils.getTextHeight(utils.smallTextSize) + 2 * utils.padding2
        }
        if (utils.prefs.get(P.SHOW_WEATHER, P.SHOW_WEATHER_DEFAULT)) {
            utils.viewHeight += utils.getTextHeight(utils.smallTextSize) + 2 * utils.padding2
        }
        if (utils.prefs.get(P.SHOW_NOTIFICATION_COUNT, P.SHOW_NOTIFICATION_COUNT_DEFAULT)) {
            utils.viewHeight += utils.getTextHeight(utils.mediumTextSize) + 2 * utils.padding16
        }
        if (utils.prefs.get(P.SHOW_NOTIFICATION_ICONS, P.SHOW_NOTIFICATION_ICONS_DEFAULT) &&
            resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        ) {
            utils.viewHeight += (NOTIFICATION_LIMIT / getNotificationRowLength(utils) + 1) * utils.drawableSize +
                2 * utils.padding16 + utils.prefs.get(P.NOTIFICATION_ICON_TOP_PADDING, P.NOTIFICATION_ICON_TOP_PADDING_DEFAULT)
        }
        if (utils.prefs.get(P.SHOW_NOTIFICATION_ICONS, P.SHOW_NOTIFICATION_ICONS_DEFAULT) &&
            utils.prefs.get(P.NOTIFICATION_PREVIEW_POSITION, P.NOTIFICATION_PREVIEW_POSITION_DEFAULT) == "below") {
            if (flags[FLAG_MOTO]) {
                utils.viewHeight += 240f
            } else {
                utils.viewHeight += 100f
            }
        }

        utils.viewHeight += paddingBottom

        if (flags[FLAG_MOTO]) {
            utils.viewHeight += 60f
        }

        // Scale background
        if (customBackground != null && measuredWidth > 0) {
            customBackground =
                (customBackground ?: error("Impossible state.")).scale(
                    measuredWidth,
                    measuredWidth,
                )
        }

        return max(
            max(
                utils.viewHeight.toInt(),
                suggestedMinimumHeight + paddingTop + paddingBottom,
            ),
            customBackground?.height ?: 0
        )
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), measureHeight())
    }

    private fun prepareDrawing() {
        utils.horizontalRelativePoint =
            if (utils.paint.textAlign == Paint.Align.LEFT) {
                utils.padding16.toFloat()
            } else {
                measuredWidth / 2f
            }
        utils.viewHeight = 0f
    }

    @Suppress("CyclomaticComplexMethod")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        prepareDrawing()

        // Regular background if not showing album art
        if (customBackground != null && !isAlbumArtOverlayVisible) {
            canvas.drawBitmap(
                customBackground ?: error("Impossible state."),
                0F,
                0F,
                null,
            )
        }

        val tempHeight = utils.viewHeight

        // Clock
        if (utils.prefs.get(P.SHOW_CLOCK, P.SHOW_CLOCK_DEFAULT) && !flags[FLAG_MOTO]) {
            Clock.draw(canvas, utils, flags, timeFormat, width)
        }

        // Date
        if (utils.prefs.get(P.SHOW_DATE, P.SHOW_DATE_DEFAULT) && !flags[FLAG_MOTO]) {
            Date.draw(canvas, utils, flags, tempHeight, dateFormat, width)
        }

        // Samsung 3 divider
        ThemeSpecials.drawDivider(canvas, utils, flags, tempHeight)

        // Moto Battery Circle draws time/date/battery percentage inside
        if (flags[FLAG_MOTO]) {
            ThemeSpecials.drawBatteryCircle(canvas, utils, flags, width, batteryLevel, timeFormat, dateFormat)
        }

        // Battery
        if (!flags[FLAG_MOTO]) {
            if (utils.prefs.get(P.SHOW_BATTERY_ICON, P.SHOW_BATTERY_ICON_DEFAULT) &&
                utils.prefs.get(P.SHOW_BATTERY_PERCENTAGE, P.SHOW_BATTERY_PERCENTAGE_DEFAULT)
            ) {
                Battery.drawIconAndPercentage(
                    canvas,
                    utils,
                    batteryIcon,
                    batteryLevel,
                    batteryIsCharging,
                    flags,
                    width,
                )
            } else if (utils.prefs.get(P.SHOW_BATTERY_ICON, P.SHOW_BATTERY_ICON_DEFAULT)) {
                Battery.drawIcon(canvas, utils, batteryIcon, batteryIsCharging, flags, width)
            } else if (utils.prefs.get(P.SHOW_BATTERY_PERCENTAGE, P.SHOW_BATTERY_PERCENTAGE_DEFAULT)) {
                Battery.drawPercentage(canvas, utils, batteryLevel, flags, width)
            }
        }

        // Music Controls
        if (musicVisible && utils.prefs.get(P.SHOW_MUSIC_CONTROLS, P.SHOW_MUSIC_CONTROLS_DEFAULT)) {
            skipPositions = MusicControls.draw(canvas, utils, musicString)
        }

        // Calendar
        if (utils.prefs.get(P.SHOW_CALENDAR, P.SHOW_CALENDAR_DEFAULT)) {
            heitezy.peekdisplay.actions.alwayson.draw.Calendar.draw(
                canvas,
                utils,
                events,
            )
        }

        // Message
        if (utils.prefs.get(P.MESSAGE, P.MESSAGE_DEFAULT) != "") {
            Message.draw(canvas, utils)
        }

        // Weather
        if (utils.prefs.get(P.SHOW_WEATHER, P.SHOW_WEATHER_DEFAULT)) {
            Weather.draw(canvas, utils, weather)
        }

        // Notification Count
        if (utils.prefs.get(P.SHOW_NOTIFICATION_COUNT, P.SHOW_NOTIFICATION_COUNT_DEFAULT)) {
            NotificationCount.draw(canvas, utils)
        }

        // Notification Icons
        if (utils.prefs.get(P.SHOW_NOTIFICATION_ICONS, P.SHOW_NOTIFICATION_ICONS_DEFAULT)) {
            NotificationIcons.draw(canvas, utils, width, isFingerprintTouched, touchedNotificationIndex)
        }
        
        // Draw notification preview if active
        if (longPressDetected && isNotificationTouched && NotificationPreview.getCurrentNotification() != null) {
            NotificationPreview.draw(canvas, utils, width, height)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    @Suppress("ReturnCount", "CyclomaticComplexMethod")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Reset timeout on any touch interaction
        if (event.action == MotionEvent.ACTION_DOWN) {
            AlwaysOn.getInstance()?.resetTimeout()
            resetKeyboardTimeout()
        }
        
        // Handle music controls
        if (abs(event.y.toInt() - skipPositions[2]) < utils.padding16 &&
            utils.prefs.get(P.SHOW_MUSIC_CONTROLS, P.SHOW_MUSIC_CONTROLS_DEFAULT)
        ) {
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    when {
                        abs(event.x.toInt() - skipPositions[0]) < utils.padding16 -> {
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                if (NotificationPreview.isReplyActive()) {
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.hideSoftInputFromWindow(windowToken, 0)
                                }
                                onSkipPreviousClicked()
                                return performClick()
                            }
                        }
                        abs(event.x.toInt() - skipPositions[1]) < utils.padding16 -> {
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                if (NotificationPreview.isReplyActive()) {
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.hideSoftInputFromWindow(windowToken, 0)
                                }
                                onSkipNextClicked()
                                return performClick()
                            }
                        }
                        abs(event.x.toInt() - utils.horizontalRelativePoint) <
                                abs(skipPositions[1] - utils.horizontalRelativePoint) -> {
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                if (NotificationPreview.isReplyActive()) {
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.hideSoftInputFromWindow(windowToken, 0)
                                }
                                onTitleClicked()
                                return performClick()
                            }
                        }
                    }
                }
            }
        }

        // Handle notification icon press-and-hold
        if (utils.prefs.get(P.SHOW_NOTIFICATION_ICONS, P.SHOW_NOTIFICATION_ICONS_DEFAULT) &&
            utils.prefs.get(P.INTERACTIVE_NOTIFICATION_ICONS, P.INTERACTIVE_NOTIFICATION_ICONS_DEFAULT)) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start long press detection
                    initialTouchX = event.x
                    initialTouchY = event.y
                    lastTouchedX = event.x
                    lastTouchedY = event.y
                    
                    // Check if touch is on a notification icon
                    touchedNotificationIndex = null
                    NotificationIcons.iconBounds.forEach { (index, rect) ->
                        if (rect.contains(event.x.toInt(), event.y.toInt())) {
                            touchedNotificationIndex = index
                            // Pause timeout when touching notification icon
                            AlwaysOn.getInstance()?.pauseTimeout()
                            // Update screen immediately to show highlight
                            invalidate()
                            return@forEach
                        }
                    }
                    
                    // If touching an icon, start long press detection
                    if (touchedNotificationIndex != null) {
                        longPressHandler.postDelayed(longPressRunnable, longPressTimeout)
                        if (NotificationPreview.isReplyActive()) {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(windowToken, 0)
                        }
                        return true
                    }

                    if (NotificationPreview.isReplyActive()) {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                        if (NotificationPreview.isPointInSendButton(lastTouchedX, lastTouchedY)) {
                            if (NotificationPreview.sendReply(context)) {
                                // Reset state after sending reply
                                isReplyModeActive = false

                                // Hide keyboard
                                imm.hideSoftInputFromWindow(windowToken, 0)

                                // Resume timeout
                                AlwaysOn.getInstance()?.resumeTimeout()
                                AlwaysOn.getInstance()?.resetTimeout()

                                touchedNotificationIndex = null
                                return true
                            }
                            invalidate()
                            return false
                        } else if (NotificationPreview.isPointInReplyField(lastTouchedX, lastTouchedY)) {
                            // Request focus so InputConnection works properly
                            requestFocus()

                            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                            
                            // Reset keyboard timeout
                            resetKeyboardTimeout()
                            
                            return false
                        } else {
                            AlwaysOn.getInstance()?.resumeTimeout()
                            AlwaysOn.getInstance()?.resetTimeout()
                            imm.hideSoftInputFromWindow(windowToken, 0)
                            NotificationPreview.clearReplyMode()
                            touchedNotificationIndex = null
                            invalidate()
                            return true
                        }
                    }
                }
                
                MotionEvent.ACTION_MOVE -> {
                    lastTouchedX = event.x
                    lastTouchedY = event.y
                    
                    // If we've started a long press and moved, update the preview
                    if (longPressDetected && isNotificationTouched) {
                        // Check if touch moved to another notification icon
                        var newIconFound = false
                        NotificationIcons.iconBounds.forEach { (index, rect) ->
                            if (rect.contains(event.x.toInt(), event.y.toInt()) && 
                                touchedNotificationIndex != index) {
                                // Switch to new notification
                                touchedNotificationIndex = index
                                NotificationPreview.setCurrentNotification(index)
                                newIconFound = true
                                invalidate()
                                return@forEach
                            }
                        }
                        
                        // If not on a new icon but still in long-press mode, just update the display
                        if (!newIconFound) {
                            invalidate()
                        }
                        return true
                    } else if (!longPressDetected && touchedNotificationIndex != null) {
                        // Check if touch moved to another notification icon, for highlighting
                        var iconChanged = false
                        NotificationIcons.iconBounds.forEach { (index, rect) ->
                            if (rect.contains(event.x.toInt(), event.y.toInt()) && 
                                touchedNotificationIndex != index) {
                                // Switch to new notification for highlighting
                                touchedNotificationIndex = index
                                iconChanged = true
                                invalidate()
                                return@forEach
                            }
                        }
                        
                        // If moved away from all icons, clear the touchedNotificationIndex
                        if (!iconChanged) {
                            var onAnyIcon = false
                            NotificationIcons.iconBounds.forEach { (_, rect) ->
                                if (rect.contains(event.x.toInt(), event.y.toInt())) {
                                    onAnyIcon = true
                                    return@forEach
                                }
                            }
                            
                            if (!onAnyIcon) {
                                touchedNotificationIndex = null
                                invalidate()
                            }
                        }
                        
                        // If moved too far before long press triggered, cancel it
                        val dx = event.x - initialTouchX
                        val dy = event.y - initialTouchY
                        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        
                        if (distance > ViewConfiguration.get(context).scaledTouchSlop) {
                            longPressHandler.removeCallbacks(longPressRunnable)
                        }
                    }
                }
                
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Cancel long press detection if not already triggered
                    longPressHandler.removeCallbacks(longPressRunnable)
                    
                    // Resume and reset timeout when finger is lifted
                    if (touchedNotificationIndex != null) {
                        AlwaysOn.getInstance()?.resumeTimeout()
                        AlwaysOn.getInstance()?.resetTimeout()
                    }
                    
                    // If long press was active and finger is released
                    if (longPressDetected && isNotificationTouched) {
                        val notificationIndex = NotificationPreview.getCurrentNotification()
                        
                        // Check if released on an action button
                        val actionIndex = NotificationPreview.isPointInAction(lastTouchedX, lastTouchedY)
                        if (actionIndex == NotificationPreview.DISMISS_ACTION_INDEX) {
                            // Handle dismiss button click
                            dismissCurrentNotification()
                            longPressDetected = false
                            isNotificationTouched = false
                            touchedNotificationIndex = null
                            invalidate()
                            return true
                        } else if (actionIndex >= 0 && notificationIndex != null) {
                            val notification = NotificationService.notifications.getOrNull(notificationIndex)
                            val packageName = notification?.packageName
                            
                            if (packageName != null) {
                                val detailedNotification = NotificationService.detailed.find { 
                                    it.packageName == packageName 
                                }
                                
                                detailedNotification?.notification?.actions?.getOrNull(actionIndex)?.let { action ->
                                    try {
                                        // Check if this is a reply action
                                        val isReplyAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                            action.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY
                                        } else {
                                            // For pre-P devices, check if action has RemoteInput
                                            action.remoteInputs != null && action.remoteInputs.isNotEmpty()
                                        }
                                        
                                        if (isReplyAction) {
                                            // Activate reply mode instead of sending the action
                                            NotificationPreview.activateReplyMode(action)
                                            isReplyModeActive = true
                                            
                                            // Pause timeout while in reply mode
                                            AlwaysOn.getInstance()?.pauseTimeout()
                                            
                                            // Request focus so InputConnection works properly
                                            requestFocus()
                                            
                                            // Request input method
                                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                                            
                                            // Start keyboard timeout
                                            resetKeyboardTimeout()
                                            
                                            invalidate()
                                            return true
                                        } else {
                                            // should check non semantic reply action by title
                                            if (action.title == context.getString(R.string.notification_action_reply)) {
                                                val activity = alwaysOnActivity ?: AlwaysOn.getInstance()
                                                activity?.let { KeyguardHelper.dismissKeyguard(it) }
                                            }

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                if (action.semanticAction == Notification.Action.SEMANTIC_ACTION_CALL) {
                                                    val activity = alwaysOnActivity ?: AlwaysOn.getInstance()
                                                    activity?.let { KeyguardHelper.dismissKeyguard(it) }
                                                }
                                            }

                                            if (Build.VERSION.SDK_INT >= 34) { // Android 14 (UPSIDE_DOWN_CAKE)
                                                val options = android.app.ActivityOptions.makeBasic()
                                                options.setPendingIntentBackgroundActivityStartMode(
                                                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                                )
                                                action.actionIntent.send(
                                                    null, 0, null, null, null, null,
                                                    options.toBundle()
                                                )
                                            } else {
                                                action.actionIntent.send()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e(Global.LOG_TAG, "Failed to send action intent: ${e.message}")
                                    }
                                }
                            }
                        }
                        // Check if released on the body area to open the app
                        else if (NotificationPreview.isPointInBodyArea(lastTouchedX, lastTouchedY) && notificationIndex != null) {
                            val notification = NotificationService.notifications.getOrNull(notificationIndex)
                            if (notification != null) {
                                val packageName = notification.packageName
                                val detailedNotification = NotificationService.detailed.find { 
                                    it.packageName == packageName 
                                }
                                
                                val pendingIntent = notification.contentIntent ?: detailedNotification?.notification?.contentIntent
                                
                                if (pendingIntent != null) {
                                    try {
                                        val activity = alwaysOnActivity ?: AlwaysOn.getInstance()
                                        activity?.let { 
                                            KeyguardHelper.dismissKeyguard(it)
                                            
                                            // Use appropriate send method based on Android version
                                            if (Build.VERSION.SDK_INT >= 34) { // Android 14 (UPSIDE_DOWN_CAKE)
                                                val options = android.app.ActivityOptions.makeBasic()
                                                options.setPendingIntentBackgroundActivityStartMode(
                                                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                                )
                                                pendingIntent.send(
                                                    null, 0, null, null, null, null,
                                                    options.toBundle()
                                                )
                                            } else {
                                                pendingIntent.send()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Fall back to launching the app if sending intent fails
                                        val packageManager = context.packageManager
                                        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                        val activity = alwaysOnActivity ?: AlwaysOn.getInstance()
                                        if (activity != null) {
                                            KeyguardHelper.dismissKeyguard(activity)
                                            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(launchIntent)
                                        }
                                    }
                                } else {
                                    // Fall back to launching the app if no pending intent is available
                                    val packageManager = context.packageManager
                                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                    val activity = alwaysOnActivity ?: AlwaysOn.getInstance()
                                    
                                    activity?.let {
                                        KeyguardHelper.dismissKeyguard(it)
                                        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(launchIntent)
                                    }
                                }
                            }
                        }

                        longPressDetected = false
                        isNotificationTouched = false
                        touchedNotificationIndex = null
                        NotificationPreview.setCurrentNotification(null)
                        invalidate()
                        return true
                    } else {
                        // If not in long-press mode, just clear the touchedNotificationIndex
                        touchedNotificationIndex = null
                        invalidate()
                    }
                }
            }
        }

        // If the touch events come directly to this view (not from fingerprint), 
        // we'll only show the visual highlight for notifications but not trigger them
        if (isFingerprintTouched && event.action == MotionEvent.ACTION_MOVE) {
            // Just invalidate to show highlights
            NotificationIcons.iconBounds.forEach { (_, rect) ->
                if (rect.contains(event.x.toInt(), event.y.toInt())) {
                    invalidate()
                    return true
                }
            }
        }
        
        return false
    }

    fun setBatteryStatus(
        level: Int,
        charging: Boolean,
    ) {
        batteryIsCharging = charging
        batteryLevel = level
        batteryIcon = IconHelper.getBatteryIcon(level)
        invalidate()
    }

    fun notifyNotificationDataChanged() {
        invalidate()
    }

    fun setActivity(activity: Activity) {
        this.alwaysOnActivity = activity
    }

    private fun dismissCurrentNotification() {
        val index = NotificationPreview.currentNotificationIndex ?: return
        val notification = NotificationService.notifications.getOrNull(index) ?: return

        try {
            NotificationService.removeNotificationsByPackageAndId(notification.packageName, notification.id, notification.tag)

            NotificationPreview.setCurrentNotification(null)
            invalidate()
        } catch (e: Exception) {
            Log.e(Global.LOG_TAG, "Failed to dismiss notification: ${e.message}")
        }
    }

    fun handleFingerprintTouch(isTouched: Boolean, event: MotionEvent) {
        // Save initial touch position when fingerprint first touched
        if (isTouched && !isFingerprintTouched) {
            initialTouchX = event.rawX
            initialTouchY = event.rawY
            
            // If we're starting a fingerprint touch, cancel any active notification preview
            // but not if in reply mode
            if (longPressDetected && isNotificationTouched && !NotificationPreview.isReplyActive()) {
                longPressDetected = false
                isNotificationTouched = false
                touchedNotificationIndex = null
                NotificationPreview.setCurrentNotification(null)
            }
        }

        // If in reply mode, don't process fingerprint touches to avoid accidental dismissal
        if (NotificationPreview.isReplyActive()) {
            return
        }

        isFingerprintTouched = isTouched

        lastTouchedX = event.rawX
        lastTouchedY = event.rawY

        if (!isTouched && event.action == MotionEvent.ACTION_UP) {
            val dx = lastTouchedX - initialTouchX
            val dy = lastTouchedY - initialTouchY
            val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            
            // We need to convert from raw screen coordinates to the local view coordinates
            val location = IntArray(2)
            getLocationOnScreen(location)
            
            // Adjust coordinates to be relative to this view
            val localX = lastTouchedX - location[0]
            val localY = lastTouchedY - location[1]

            var releasedOverNotification = false
            
            // Only process notification opening if the setting is enabled
            if (utils.prefs.get(P.SWIPE_NOTIFICATION_OPEN, P.SWIPE_NOTIFICATION_OPEN_DEFAULT)) {
                NotificationIcons.iconBounds.forEach { (index, rect) ->
                    if (rect.contains(localX.toInt(), localY.toInt())) {
                        releasedOverNotification = true
                        val notification = NotificationService.notifications.getOrNull(index)
                        if (notification != null) {
                            // Get the corresponding detailed notification to access the content intent
                            val detailedNotification = NotificationService.detailed.find { 
                                it.packageName == notification.packageName 
                            }
                            
                            val pendingIntent = notification.contentIntent ?: detailedNotification?.notification?.contentIntent
                            
                            if (pendingIntent != null) {
                                try {
                                    // Dismiss keyguard before sending the intent
                                    val activity = alwaysOnActivity ?: AlwaysOn.getInstance()
                                    activity?.let { 
                                        KeyguardHelper.dismissKeyguard(it)
                                        
                                        // Use appropriate send method based on Android version
                                        if (Build.VERSION.SDK_INT >= 34) { // Android 14 (UPSIDE_DOWN_CAKE)
                                            val options = android.app.ActivityOptions.makeBasic()
                                            options.setPendingIntentBackgroundActivityStartMode(
                                                android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                            )
                                            pendingIntent.send(
                                                null, 0, null, null, null, null,
                                                options.toBundle()
                                            )
                                        } else {
                                            pendingIntent.send()
                                        }
                                    }
                                } catch (e: Exception) {
                                    // If sending the intent fails, fall back to opening the app
                                    val packageName = notification.packageName
                                    val packageManager = context.packageManager
                                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                    val activity = alwaysOnActivity ?: AlwaysOn.getInstance()
                                    
                                    activity?.let {
                                        KeyguardHelper.dismissKeyguard(it)
                                        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(launchIntent)
                                    }
                                }
                            } else {
                                // Fall back to launching the app if no pending intent is available
                                val packageName = notification.packageName
                                val packageManager = context.packageManager
                                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                val activity = alwaysOnActivity ?: AlwaysOn.getInstance()
                                
                                activity?.let {
                                    KeyguardHelper.dismissKeyguard(it)
                                    launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(launchIntent)
                                }
                            }
                        }
                    }
                }
            }
            
            // If not released over a notification and distance is significant,
            // perform the normal unlock action
            if (!releasedOverNotification && distance > 300) {
                alwaysOnActivity?.let { activity ->
                    KeyguardHelper.dismissKeyguard(activity)
                }
            }
        }
        
        // Force redraw to handle notification icon visibility changes
        invalidate()
    }

    fun startClockHandler() {
        stopClockHandler()
        updateHandler.postDelayed(
            object : Runnable {
                override fun run() {
                    invalidate()
                    updateHandler.postDelayed(this, UPDATE_DELAY)
                }
            },
            UPDATE_DELAY,
        )
    }

    fun stopClockHandler() {
        updateHandler.removeCallbacksAndMessages(null)
    }

    fun handleKeyEvent(keyCode: Int, event: android.view.KeyEvent): Boolean {
        // Only process key events if in reply mode
        if (!NotificationPreview.isReplyActive()) {
            return false
        }

        // Reset timeout on any key interaction
        AlwaysOn.getInstance()?.resetTimeout()
        resetKeyboardTimeout()

        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                android.view.KeyEvent.KEYCODE_DEL -> {
                    NotificationPreview.deleteLastCharFromReply()
                    invalidate()
                    return true
                }
            }
        }

        return false
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        // Only setup input connection if reply mode is active
        if (!NotificationPreview.isReplyActive()) {
            // On Android 7 and below, super.onCreateInputConnection can return null
            // which causes a NullPointerException. Always return a BaseInputConnection instead.
            outAttrs.inputType = InputType.TYPE_NULL
            return BaseInputConnection(this, false)
        }

        // Setup outAttrs for proper keyboard behavior
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                            InputType.TYPE_TEXT_VARIATION_NORMAL
        outAttrs.imeOptions = EditorInfo.IME_ACTION_SEND

        // Create a custom InputConnection to handle the text input
        return object : BaseInputConnection(this, true) {
            // Handle committed text from keyboard, including Cyrillic and other scripts
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                if (text != null && NotificationPreview.isReplyActive()) {
                    for (i in 0 until text.length) {
                        NotificationPreview.appendCharToReply(text[i])
                    }
                    invalidate()
                    resetKeyboardTimeout()
                    return true
                }
                return false
            }

            // Handle deletion
            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (NotificationPreview.isReplyActive()) {
                    // Simple delete operation - just last character
                    NotificationPreview.deleteLastCharFromReply()
                    invalidate()
                    resetKeyboardTimeout()
                    return true
                }
                return false
            }

            // Handle enter key for sending
            override fun performEditorAction(actionCode: Int): Boolean {
                if (actionCode == EditorInfo.IME_ACTION_SEND && NotificationPreview.isReplyActive()) {
                    if (NotificationPreview.sendReply(context)) {
                        // Reset state after sending reply
                        isReplyModeActive = false

                        // Hide keyboard
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(windowToken, 0)

                        // Resume timeout
                        AlwaysOn.getInstance()?.resumeTimeout()
                        AlwaysOn.getInstance()?.resetTimeout()

                        touchedNotificationIndex = null
                        invalidate()
                        return true
                    }
                }
                return false
            }
        }
    }

    // Method to check album art state and notify the activity
    private fun checkAlbumArtState() {
        val shouldShowAlbumArt = albumArt != null &&
                             musicVisible &&
                             utils.prefs.get(P.SHOW_MUSIC_CONTROLS, P.SHOW_MUSIC_CONTROLS_DEFAULT) &&
                             utils.prefs.get(P.SHOW_ALBUM_ART, P.SHOW_ALBUM_ART_DEFAULT)
        isAlbumArtOverlayVisible = shouldShowAlbumArt
        onAlbumArtStateChanged(shouldShowAlbumArt, albumArt)
    }

    /**
     * Sets the album art bitmap
     */
    fun setAlbumArt(bitmap: Bitmap?) {
        val oldAlbumArt = this.albumArt
        this.albumArt = bitmap
        
        // Check if album art state has changed (appeared or disappeared)
        if ((oldAlbumArt == null && bitmap != null) || (oldAlbumArt != null && bitmap == null)) {
            // Refresh background when album art state changes
            prepareBackground()
        }
        
        checkAlbumArtState()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopClockHandler()
        longPressHandler.removeCallbacksAndMessages(null)
        keyboardTimeoutHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val UPDATE_DELAY: Long = 60_000
        private fun getNotificationRowLength(utils: Utils): Int {
            return when {
                utils.drawableSize == utils.dpToPx(Utils.DRAWABLE_SIZE_SMALL).toInt() -> 10
                utils.drawableSize == utils.dpToPx(Utils.DRAWABLE_SIZE).toInt() -> 7
                utils.drawableSize == utils.dpToPx(Utils.DRAWABLE_SIZE_ENLARGED).toInt() -> 6
                else -> 7 // Default fallback
            }
        }
        private const val NOTIFICATION_LIMIT: Int = 20
        const val FLAG_CAPS_DATE: Int = 0
        private const val FLAG_SAMSUNG_2: Int = 1
        const val FLAG_BIG_DATE: Int = 1
        const val FLAG_SAMSUNG_3: Int = 2
        private const val FLAG_MULTILINE_CLOCK: Int = 3
        const val FLAG_ANALOG_CLOCK: Int = 4
        const val FLAG_MOTO: Int = 5
    }
}
