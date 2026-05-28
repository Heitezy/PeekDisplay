package heitezy.peekdisplay.actions.alwayson

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Base64
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import heitezy.peekdisplay.Application
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.OffActivity
import heitezy.peekdisplay.actions.alwayson.data.Data
import heitezy.peekdisplay.actions.alwayson.data.State
import heitezy.peekdisplay.helpers.Global
import heitezy.peekdisplay.helpers.IconHelper
import heitezy.peekdisplay.helpers.KeyguardHelper
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.helpers.Root
import heitezy.peekdisplay.helpers.Rules
import heitezy.peekdisplay.receivers.CombinedServiceReceiver
import heitezy.peekdisplay.services.NotificationService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.sqrt

class AlwaysOn : OffActivity(), NotificationService.OnNotificationsChangedListener {
    @JvmField
    internal var servicesRunning: Boolean = false

    internal lateinit var prefs: P

    private var peekState by mutableStateOf(State())

    // Media Controls
    private var onActiveSessionsChangedListener: AlwaysOnOnActiveSessionsChangedListener? = null

    // Edge glow tracking
    private var initialNotificationCount: Int = 0
    private var lastNotificationCount: Int = 0

    // Battery saver
    private var userPowerSaving: Boolean = false

    // Proximity
    private var sensorManager: SensorManager? = null
    private var sensorEventListener: SensorEventListener? = null
    private var isProximate by mutableStateOf(false)

    // DND
    private var notificationManager: NotificationManager? = null
    private var notificationAccess: Boolean = false
    private var userDND: Int = NotificationManager.INTERRUPTION_FILTER_ALL

    // Call recognition
    private var onModeChangedListener: AudioManager.OnModeChangedListener? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Keyboard/Reply timeout
    private var replyTimeoutRunnable: Runnable? = null
    private val REPLY_TIMEOUT_DELAY = 60000L

    // Interaction cooldown
    private val interactionCooldownRunnable = Runnable {
        peekState = peekState.copy(isInteracting = false)
        updateRefreshRate()
    }

    // Timeout tracking
    private var timeoutRunnable: Runnable? = null
    private var timeoutDuration: Long = 0
    private var isTimeoutPaused: Boolean = false
    private var remainingTimeoutTime: Long = 0
    private var lastTimeoutResetTime: Long = 0

    // Cached Formatters
    private var cachedTimeFormat: SimpleDateFormat? = null
    private var cachedDateFormat: SimpleDateFormat? = null
    private var lastTimeFormatString: String? = null
    private var lastDateFormatString: String? = null

    private fun getTimeFormat(format: String): SimpleDateFormat {
        if (cachedTimeFormat == null || lastTimeFormatString != format) {
            cachedTimeFormat = SimpleDateFormat(format, Locale.getDefault())
            lastTimeFormatString = format
        }
        return cachedTimeFormat!!
    }

    private fun getDateFormat(format: String): SimpleDateFormat {
        if (cachedDateFormat == null || lastDateFormatString != format) {
            cachedDateFormat = SimpleDateFormat(format, Locale.getDefault())
            lastDateFormatString = format
        }
        return cachedDateFormat!!
    }

    private fun updateInitialAODState() {
        val theme = prefs.get(P.USER_THEME, P.USER_THEME_DEFAULT)
        val isMultiline = theme == P.USER_THEME_SAMSUNG || theme == P.USER_THEME_ONEPLUS || theme == P.USER_THEME_ANALOG
        
        val timeFormatString = if (isMultiline) {
            prefs.getMultiLineTimeFormat()
        } else {
            prefs.getSingleLineTimeFormat()
        }
        
        val timeFormat = getTimeFormat(timeFormatString)
        val dateFormat = getDateFormat(prefs.get(P.DATE_FORMAT, P.DATE_FORMAT_DEFAULT))
        
        val customBgEncoded = prefs.get(P.CUSTOM_BACKGROUND, "")
        val customBgBitmap = if (customBgEncoded.isNotEmpty()) {
            try {
                val bytes = Base64.decode(customBgEncoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) {
                null
            }
        } else null

        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                        status == BatteryManager.BATTERY_STATUS_FULL ||
                        plugged > 0

        peekState = peekState.copy(
            theme = theme,
            time = timeFormat.format(System.currentTimeMillis()),
            date = dateFormat.format(System.currentTimeMillis()),
            batteryLevel = level,
            batteryIsCharging = isCharging,
            batteryIconRes = IconHelper.getBatteryIcon(level),
            clockColor = Color(prefs.get(P.DISPLAY_COLOR_CLOCK, P.DISPLAY_COLOR_CLOCK_DEFAULT)),
            dateColor = Color(prefs.get(P.DISPLAY_COLOR_DATE, P.DISPLAY_COLOR_DATE_DEFAULT)),
            batteryColor = Color(prefs.get(P.DISPLAY_COLOR_BATTERY, P.DISPLAY_COLOR_BATTERY_DEFAULT)),
            batteryArcColor = Color(prefs.get(P.DISPLAY_COLOR_BATTERY_ARC, P.DISPLAY_COLOR_BATTERY_ARC_DEFAULT)),
            musicColor = Color(prefs.get(P.DISPLAY_COLOR_MUSIC_CONTROLS, P.DISPLAY_COLOR_MUSIC_CONTROLS_DEFAULT)),
            messageColor = Color(prefs.get(P.DISPLAY_COLOR_MESSAGE, P.DISPLAY_COLOR_MESSAGE_DEFAULT)),
            calendarColor = Color(prefs.get(P.DISPLAY_COLOR_CALENDAR, P.DISPLAY_COLOR_CALENDAR_DEFAULT)),
            weatherColor = Color(prefs.get(P.DISPLAY_COLOR_WEATHER, P.DISPLAY_COLOR_WEATHER_DEFAULT)),
            notificationColor = Color(prefs.get(P.DISPLAY_COLOR_NOTIFICATION, P.DISPLAY_COLOR_NOTIFICATION_DEFAULT)),
            textAlign = if (theme == P.USER_THEME_SAMSUNG2) TextAlign.Left else TextAlign.Center,
            showClock = prefs.get(P.SHOW_CLOCK, P.SHOW_CLOCK_DEFAULT),
            showDate = prefs.get(P.SHOW_DATE, P.SHOW_DATE_DEFAULT),
            showBatteryIcon = prefs.get(P.SHOW_BATTERY_ICON, P.SHOW_BATTERY_ICON_DEFAULT),
            showBatteryPercentage = prefs.get(P.SHOW_BATTERY_PERCENTAGE, P.SHOW_BATTERY_PERCENTAGE_DEFAULT),
            showMusicControls = prefs.get(P.SHOW_MUSIC_CONTROLS, P.SHOW_MUSIC_CONTROLS_DEFAULT),
            showAlbumArt = prefs.get(P.SHOW_ALBUM_ART, P.SHOW_ALBUM_ART_DEFAULT),
            showWeather = prefs.get(P.SHOW_WEATHER, P.SHOW_WEATHER_DEFAULT),
            showCalendar = prefs.get(P.SHOW_CALENDAR, P.SHOW_CALENDAR_DEFAULT),
            showNotificationCount = prefs.get(P.SHOW_NOTIFICATION_COUNT, P.SHOW_NOTIFICATION_COUNT_DEFAULT),
            showNotificationIcons = prefs.get(P.SHOW_NOTIFICATION_ICONS, P.SHOW_NOTIFICATION_ICONS_DEFAULT),
            message = prefs.get(P.MESSAGE, P.MESSAGE_DEFAULT),
            calendarEvents = Data.getCalendar(this, prefs),
            isSamsung3 = theme == P.USER_THEME_SAMSUNG3,
            isBigDate = theme == P.USER_THEME_SAMSUNG2,
            isCapsDate = theme == P.USER_THEME_SAMSUNG,
            isAnalog = theme == P.USER_THEME_ANALOG,
            isMultiline = isMultiline,
            showFingerprint = prefs.get(P.SHOW_FINGERPRINT_ICON, P.SHOW_FINGERPRINT_ICON_DEFAULT),
            useLockIcon = prefs.get(P.LOCK_ICON, P.LOCK_ICON_DEFAULT),
            fingerprintColor = Color(prefs.get(P.DISPLAY_COLOR_FINGERPRINT, P.DISPLAY_COLOR_FINGERPRINT_DEFAULT)),
            fingerprintMargin = prefs.get(P.FINGERPRINT_MARGIN, P.FINGERPRINT_MARGIN_DEFAULT),
            fingerprintInteractionMode = prefs.get(P.FINGERPRINT_INTERACTION_MODE, P.FINGERPRINT_INTERACTION_MODE_DEFAULT),
            edgeGlowEnabled = prefs.get(P.EDGE_GLOW, P.EDGE_GLOW_DEFAULT),
            edgeGlowColor = Color(prefs.get(P.DISPLAY_COLOR_EDGE_GLOW, P.DISPLAY_COLOR_EDGE_GLOW_DEFAULT)),
            edgeGlowDuration = prefs.get(P.EDGE_GLOW_DURATION, P.EDGE_GLOW_DURATION_DEFAULT),
            edgeGlowDelay = prefs.get(P.EDGE_GLOW_DELAY, P.EDGE_GLOW_DELAY_DEFAULT),
            edgeGlowStyle = prefs.get(P.EDGE_GLOW_STYLE, P.EDGE_GLOW_STYLE_DEFAULT),
            disableDoubleTap = prefs.get(P.DISABLE_DOUBLE_TAP, P.DISABLE_DOUBLE_TAP_DEFAULT),
            doubleTapSpeed = prefs.get(P.DOUBLE_TAP_SPEED, P.DOUBLE_TAP_SPEED_DEFAULT).toLong(),
            backgroundImageRes = prefs.backgroundImage(),
            customBackground = customBgBitmap,
            notificationIconSize = prefs.get(P.NOTIFICATION_ICON_SIZE, P.NOTIFICATION_ICON_SIZE_DEFAULT),
            notificationPreviewPosition = prefs.get(P.NOTIFICATION_PREVIEW_POSITION, P.NOTIFICATION_PREVIEW_POSITION_DEFAULT),
            tintNotifications = prefs.get(P.TINT_NOTIFICATIONS, P.TINT_NOTIFICATIONS_DEFAULT),
            interactiveNotifications = prefs.get(P.INTERACTIVE_NOTIFICATION_ICONS, P.INTERACTIVE_NOTIFICATION_ICONS_DEFAULT),
            invertInteractionHighlight = prefs.get(P.INVERT_INTERACTION_HIGHLIGHT, P.INVERT_INTERACTION_HIGHLIGHT_DEFAULT),
            swipeNotificationOpen = prefs.get(P.SWIPE_NOTIFICATION_OPEN, P.SWIPE_NOTIFICATION_OPEN_DEFAULT),
            animateMotion = prefs.get(P.ANIMATE_MOTION, P.ANIMATE_MOTION_DEFAULT),
            topPadding = prefs.get(P.TOP_PADDING, P.TOP_PADDING_DEFAULT),
            notificationTopPadding = prefs.get(P.NOTIFICATION_ICON_TOP_PADDING, P.NOTIFICATION_ICON_TOP_PADDING_DEFAULT),
            scale = prefs.displayScale()
        )

        peekState = peekState.copy(
            fingerprintIconRes = if (peekState.useLockIcon) R.drawable.ic_lock else R.drawable.ic_fingerprint_white,
        )
    }

    private fun fetchWeather() {
        if (!prefs.get(P.SHOW_WEATHER, P.SHOW_WEATHER_DEFAULT)) return

        val url = prefs.getWeatherUrl()

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                peekState = peekState.copy(weather = response)
            },
            { error ->
                Log.e(Global.LOG_TAG, "Weather fetch failed: $error")
            }
        )
        Application.requestQueue.add(stringRequest)
        
        val interval = prefs.get(P.WEATHER_REFRESH_INTERVAL, P.WEATHER_REFRESH_INTERVAL_DEFAULT)
        if (interval > 0) {
            mainHandler.postDelayed({ fetchWeather() }, interval * 60 * 1000L)
        }
    }

    private fun startAODUpdateLoop() {
        val runnable = object : Runnable {
            override fun run() {
                if (servicesRunning) {
                    val isMultiline = peekState.theme == P.USER_THEME_SAMSUNG || peekState.theme == P.USER_THEME_ONEPLUS || peekState.theme == P.USER_THEME_ANALOG
                    val timeFormatString = if (isMultiline) {
                        prefs.getMultiLineTimeFormat()
                    } else {
                        prefs.getSingleLineTimeFormat()
                    }
                    val timeFormat = getTimeFormat(timeFormatString)
                    val dateFormat = getDateFormat(prefs.get(P.DATE_FORMAT, P.DATE_FORMAT_DEFAULT))
                    peekState = peekState.copy(
                        time = timeFormat.format(System.currentTimeMillis()),
                        date = dateFormat.format(System.currentTimeMillis())
                    )
                    
                    val hasSeconds = timeFormatString.contains("s", ignoreCase = true)
                    mainHandler.postDelayed(this, if (hasSeconds) 1000 else 60000)
                }
            }
        }
        mainHandler.post(runnable)
    }

    // BroadcastReceiver
    private val systemFilter: IntentFilter = IntentFilter()
    private val systemReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                c: Context,
                intent: Intent,
            ) {
                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                        if (level <= prefs.get(P.RULES_BATTERY, P.RULES_BATTERY_DEFAULT)) {
                            finishAndOff()
                            return
                        }
                        
                        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                                        status == BatteryManager.BATTERY_STATUS_FULL ||
                                        plugged > 0
                        
                        peekState = peekState.copy(
                            batteryLevel = level,
                            batteryIsCharging = isCharging,
                            batteryIconRes = IconHelper.getBatteryIcon(level)
                        )
                    }

                    Intent.ACTION_POWER_CONNECTED -> {
                        if (!Rules.matchesChargingState(this@AlwaysOn)) finishAndOff()
                    }

                    Intent.ACTION_POWER_DISCONNECTED -> {
                        if (!Rules.matchesChargingState(this@AlwaysOn)) finishAndOff()
                    }

                    NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                        if (!Rules.matchesDoNotDisturbState(this@AlwaysOn)) finishAndOff()
                    }
                }
            }
        }


    private fun prepareView() {
        // Cutouts
        if (prefs.get("hide_display_cutouts", false)) {
            setTheme(R.style.CutoutHide)
        } else {
            setTheme(R.style.CutoutIgnore)
        }

        setContent {
            OffContent {
                Screen()
            }
        }

        // Brightness
        if (prefs.get(P.FORCE_BRIGHTNESS, P.FORCE_BRIGHTNESS_DEFAULT)) {
            val attributes = window.attributes
            attributes.screenBrightness = prefs.get(
                P.FORCE_BRIGHTNESS_VALUE,
                P.FORCE_BRIGHTNESS_VALUE_DEFAULT,
            ) / 255.toFloat()
            window.attributes = attributes
        }

        // Show on lock screen
        Handler(Looper.getMainLooper()).postDelayed({
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            turnOnScreen()
        }, SMALL_DELAY)

        // Hide UI
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    @Composable
    private fun Screen() {
        val alpha by animateFloatAsState(
            targetValue = if (isProximate) 0f else 1f,
            animationSpec = tween(1000),
            label = "alpha"
        )

        val animatedDriftY by animateFloatAsState(
            targetValue = peekState.driftY,
            animationSpec = if (peekState.animateMotion) tween(2000) else tween(0),
            label = "driftY"
        )

        val animatedFpDriftY by animateFloatAsState(
            targetValue = peekState.fpDriftY,
            animationSpec = if (peekState.animateMotion) tween(2000) else tween(0),
            label = "fpDriftY"
        )
        
        val burnInDelay = remember { prefs.get("ao_animation_delay", 2) * 60000L }
        
        LaunchedEffect(Unit) {
            while (true) {
                delay(burnInDelay)

                // Wait until interaction ends
                snapshotFlow { peekState.isInteracting }.first { !it }

                peekState = peekState.copy(driftY = 32f, fpDriftY = 64f, isDrifting = true)
                updateRefreshRate()
                delay(2000)
                peekState = peekState.copy(isDrifting = false)
                updateRefreshRate()

                delay(burnInDelay)

                // Wait until interaction ends
                snapshotFlow { peekState.isInteracting }.first { !it }

                peekState = peekState.copy(driftY = 0f, fpDriftY = 0f, isDrifting = true)
                updateRefreshRate()
                delay(2000)
                peekState = peekState.copy(isDrifting = false)
                updateRefreshRate()
            }
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)) {
            Content(
                state = peekState.copy(driftY = animatedDriftY, fpDriftY = animatedFpDriftY),
                onSkipPrevious = {
                    onInteractionStarted()
                    onActiveSessionsChangedListener?.controller?.transportControls?.skipToPrevious()
                    onInteractionEnded()
                },
                onSkipNext = {
                    onInteractionStarted()
                    onActiveSessionsChangedListener?.controller?.transportControls?.skipToNext()
                    onInteractionEnded()
                },
                onTitleClick = {
                    onInteractionStarted()
                    resetTimeout()
                    if (onActiveSessionsChangedListener?.state == PlaybackState.STATE_PLAYING) {
                        onActiveSessionsChangedListener?.controller?.transportControls?.pause()
                    } else if (onActiveSessionsChangedListener?.state == PlaybackState.STATE_PAUSED) {
                        onActiveSessionsChangedListener?.controller?.transportControls?.play()
                    }
                    onInteractionEnded()
                },
                onNotificationHoldStarted = { index ->
                    onInteractionStarted()
                    resetTimeout()
                    peekState = peekState.copy(
                        touchedNotificationIndex = index,
                        isReplyMode = false,
                        replyText = TextFieldValue(""),
                        replyActionIndex = null
                    )
                    pauseTimeout()
                },
                onNotificationHoldFinished = {
                    onInteractionEnded()
                    peekState = peekState.copy(
                        touchedNotificationIndex = null,
                        isReplyMode = false,
                        replyText = TextFieldValue(""),
                        replyActionIndex = null
                    )
                    resumeTimeout()
                    resetTimeout()
                },
                onActionClick = { notificationIndex, actionIndex ->
                    onInteractionStarted()
                    resetTimeout()
                    val sbn = peekState.detailedNotifications.getOrNull(notificationIndex)
                    val action = sbn?.notification?.actions?.getOrNull(actionIndex)
                    if (action != null) {
                        try {
                            if (action.title == this@AlwaysOn.getString(R.string.notification_action_reply)) {
                                this@AlwaysOn.let { KeyguardHelper.dismissKeyguard(it) }
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                if (action.semanticAction == Notification.Action.SEMANTIC_ACTION_CALL) {
                                    this@AlwaysOn.let { KeyguardHelper.dismissKeyguard(it) }
                                }
                            }

                            action.actionIntent.send(this@AlwaysOn, 0, null, null, null, null, getActivityOptionsBundle())
                            peekState = peekState.copy(touchedNotificationIndex = null)
                        } catch (_: Exception) {
                            Log.e(Global.LOG_TAG, "Action send failed")
                        }
                    }
                    onInteractionEnded()
                },
                onReplyActionClick = { notificationIndex, actionIndex ->
                    onInteractionStarted()
                    resetTimeout()
                    peekState = peekState.copy(
                        isReplyMode = true,
                        replyActionIndex = actionIndex
                    )
                    pauseTimeout()
                    resetReplyTimeout()
                    onInteractionEnded()
                },
                onDismissNotification = { index ->
                    onInteractionStarted()
                    resetTimeout()
                    val sbn = peekState.detailedNotifications.getOrNull(index)
                    if (sbn != null) {
                        NotificationService.removeNotificationsByPackageAndId(sbn.packageName, sbn.id, sbn.tag)
                        peekState = peekState.copy(touchedNotificationIndex = null)
                    }
                    onInteractionEnded()
                },
                onReplyTextChange = { text ->
                    onInteractionStarted()
                    peekState = peekState.copy(replyText = text)
                    resetReplyTimeout()
                    onInteractionEnded()
                },
                onSendReply = { index ->
                    onInteractionStarted()
                    resetTimeout()
                    cancelReplyTimeout()
                    val sbn = peekState.detailedNotifications.getOrNull(index)
                    val action = peekState.replyActionIndex
                        ?.let { sbn?.notification?.actions?.getOrNull(it) }
                        ?: sbn?.notification?.actions?.find { it.remoteInputs != null }
                    if (action != null && peekState.replyText.text.isNotEmpty()) {
                        val remoteInputs = action.remoteInputs ?: return@Content
                        val remoteInput = remoteInputs[0]
                        val results = Bundle().apply {
                            putCharSequence(remoteInput.resultKey, peekState.replyText.text)
                        }
                        val intent = Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        RemoteInput.addResultsToIntent(remoteInputs, intent, results)
                        try {
                            action.actionIntent.send(this@AlwaysOn, 0, intent, null, null, null, getActivityOptionsBundle())
                            peekState = peekState.copy(
                                touchedNotificationIndex = null,
                                isReplyMode = false,
                                replyText = TextFieldValue(""),
                                replyActionIndex = null
                            )
                            sbn?.let { NotificationService.removeNotificationsByPackageAndId(it.packageName, sbn.id, sbn.tag) }
                        } catch (_: Exception) {
                            Log.e(Global.LOG_TAG, "Reply failed")
                        }
                    }
                    onInteractionEnded()
                },
                onDoubleTap = {
                    onInteractionStarted()
                    if (!peekState.disableDoubleTap && peekState.touchedNotificationIndex == null) {
                        val duration =
                            prefs.get(P.VIBRATION_DURATION, P.VIBRATION_DURATION_DEFAULT).toLong()
                        if (duration > 0) {
                            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(
                                        duration,
                                        VibrationEffect.DEFAULT_AMPLITUDE
                                    )
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(duration)
                            }
                        }
                        KeyguardHelper.dismissKeyguard(this@AlwaysOn)
                    }
                    onInteractionEnded()
                },
                onDown = {
                    onInteractionStarted()
                    resetTimeout()
                    onInteractionEnded()
                },
                onFingerprintTouch = { isTouched, dx, dy ->
                    if (isTouched) {
                        onInteractionStarted()
                        pauseTimeout()
                    } else {
                        onInteractionEnded()
                        resumeTimeout()
                        resetTimeout()
                    }

                    if (peekState.fingerprintInteractionMode == "swipe") {
                        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        val isLockOpen = distance > 300

                        val iconRes = when {
                            peekState.useLockIcon && isLockOpen -> R.drawable.ic_lock_open
                            peekState.useLockIcon -> R.drawable.ic_lock
                            else -> R.drawable.ic_fingerprint_white
                        }

                        peekState = peekState.copy(
                            isFingerprintTouched = isTouched,
                            fingerprintIconRes = iconRes,
                            hoveredNotificationIndex = if (!isTouched) null else peekState.hoveredNotificationIndex
                        )

                        if (!isTouched && isLockOpen) {
                            KeyguardHelper.dismissKeyguard(this@AlwaysOn)
                            finish()
                        }
                    } else {
                        peekState = peekState.copy(
                            isFingerprintTouched = isTouched,
                            hoveredNotificationIndex = if (!isTouched) null else peekState.hoveredNotificationIndex
                        )
                    }
                },
                onFingerprintLongPress = {
                    onInteractionStarted()
                    if (peekState.fingerprintInteractionMode == "longpress") {
                        KeyguardHelper.dismissKeyguard(this@AlwaysOn)
                        finish()
                    }
                    onInteractionEnded()
                },
                onOpenNotification = { index ->
                    onInteractionStarted()
                    val sbn = peekState.detailedNotifications.getOrNull(index)
                    if (sbn != null) {
                        try {
                            sbn.notification.contentIntent?.send(this@AlwaysOn, 0, null, null, null, null, getActivityOptionsBundle())
                            KeyguardHelper.dismissKeyguard(this@AlwaysOn)
                            finish()
                        } catch (_: Exception) {
                            Log.e(Global.LOG_TAG, "Notification open failed")
                        }
                    }
                    onInteractionEnded()
                },
                onNotificationHovered = { index ->
                    if (peekState.hoveredNotificationIndex != index) {
                        peekState = peekState.copy(hoveredNotificationIndex = index)
                    }
                }
            )
        }
    }

    private fun getActivityOptionsBundle(): Bundle? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val options = ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= 36) {
                options.pendingIntentBackgroundActivityStartMode = 
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
            } else {
                @Suppress("DEPRECATION")
                options.pendingIntentBackgroundActivityStartMode = 
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
            return options.toBundle()
        }
        return null
    }

    private fun prepareMusicControls() {
        val mediaSessionManager =
            getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val notificationListener =
            ComponentName(applicationContext, NotificationService::class.java.name)
        onActiveSessionsChangedListener =
            AlwaysOnOnActiveSessionsChangedListener().apply {
                onMediaStateChanged = { musicString, albumArt ->
                    peekState = peekState.copy(
                        musicString = musicString,
                        albumArt = albumArt
                    )
                }
            }
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                onActiveSessionsChangedListener
                    ?: return,
                notificationListener,
            )
            onActiveSessionsChangedListener?.onActiveSessionsChanged(
                mediaSessionManager.getActiveSessions(
                    notificationListener,
                ),
            )
        } catch (exception: SecurityException) {
            Log.w(Global.LOG_TAG, exception.toString())
            peekState = peekState.copy(musicString = resources.getString(R.string.missing_permissions))
        }
    }

    private fun prepareProximity() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                    isProximate = event.values[0] != event.sensor.maximumRange
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    private fun prepareDoNotDisturb() {
        notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationAccess = notificationManager?.isNotificationPolicyAccessGranted ?: false
        if (notificationAccess) {
            userDND = notificationManager?.currentInterruptionFilter
                ?: NotificationManager.INTERRUPTION_FILTER_ALL
        }
    }

    private fun prepareCallRecognition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            onModeChangedListener =
                AudioManager.OnModeChangedListener { mode ->
                    if (mode == AudioManager.MODE_RINGTONE) finish()
                }
            (getSystemService(AUDIO_SERVICE) as AudioManager).addOnModeChangedListener(
                mainExecutor,
                onModeChangedListener ?: error("onModeChangedListener is null."),
            )
        }
    }

    private fun updateRefreshRate() {
        if (!prefs.get(P.REFRESH_RATE_OPTIMIZATION, P.REFRESH_RATE_OPTIMIZATION_DEFAULT)) {
            setRefreshRate(false)
            return
        }

        val isMultiline = peekState.theme == P.USER_THEME_SAMSUNG || peekState.theme == P.USER_THEME_ONEPLUS || peekState.theme == P.USER_THEME_ANALOG
        val timeFormatString = if (isMultiline) {
            prefs.getMultiLineTimeFormat()
        } else {
            prefs.getSingleLineTimeFormat()
        }
        val hasSeconds = timeFormatString.contains("s", ignoreCase = true)

        val hasActiveAnimations = (peekState.edgeGlowEnabled && peekState.hasNewNotifications) || peekState.isDrifting || hasSeconds
        val isStatic = !peekState.isInteracting && !hasActiveAnimations

        setRefreshRate(isStatic)
    }

    private fun setRefreshRate(low: Boolean) {
        val params = window.attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val rate = if (low) getLowestRefreshRate() else 0f
            if (params.preferredRefreshRate != rate) {
                params.preferredRefreshRate = rate
                window.attributes = params
            }
        } else {
            val modeId = if (low) getLowestRefreshRateMode() else 0
            if (params.preferredDisplayModeId != modeId) {
                params.preferredDisplayModeId = modeId
                window.attributes = params
            }
        }
    }

    private fun getLowestRefreshRate(): Float {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(DISPLAY_SERVICE) as DisplayManager)
                .getDisplay(Display.DEFAULT_DISPLAY)
        }

        val modes = display?.supportedModes
        if (!modes.isNullOrEmpty()) {
            val currentMode = display.mode
            var lowestRate = currentMode.refreshRate

            for (mode in modes) {
                if (mode.physicalWidth == currentMode.physicalWidth &&
                    mode.physicalHeight == currentMode.physicalHeight
                ) {
                    if (mode.refreshRate < lowestRate) {
                        lowestRate = mode.refreshRate
                    }
                }
            }
            return lowestRate
        }
        return 0f
    }

    private fun getLowestRefreshRateMode(): Int {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(DISPLAY_SERVICE) as DisplayManager)
                .getDisplay(Display.DEFAULT_DISPLAY)
        }

        val modes = display?.supportedModes
        if (!modes.isNullOrEmpty()) {
            val currentMode = display.mode
            var lowestRate = Float.MAX_VALUE
            var lowestModeId = currentMode.modeId

            for (mode in modes) {
                if (mode.physicalWidth == currentMode.physicalWidth &&
                    mode.physicalHeight == currentMode.physicalHeight
                ) {
                    if (mode.refreshRate < lowestRate) {
                        lowestRate = mode.refreshRate
                        lowestModeId = mode.modeId
                    }
                }
            }
            return lowestModeId
        }
        return 0
    }

    private fun onInteractionStarted() {
        mainHandler.removeCallbacks(interactionCooldownRunnable)
        if (!peekState.isInteracting) {
            peekState = peekState.copy(isInteracting = true)
            updateRefreshRate()
        }
    }

    private fun onInteractionEnded() {
        mainHandler.removeCallbacks(interactionCooldownRunnable)
        mainHandler.postDelayed(interactionCooldownRunnable, 1500)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

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

        instance = this

        prefs = P(getDefaultSharedPreferences(this))
        userPowerSaving = (getSystemService(POWER_SERVICE) as PowerManager).isPowerSaveMode

        updateInitialAODState()
        prepareView()

        // Add DND state change listener
        systemFilter.addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)

        // Battery
        systemFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
        systemFilter.addAction(Intent.ACTION_POWER_CONNECTED)
        systemFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)

        // Music Controls
        if (prefs.get(P.SHOW_MUSIC_CONTROLS, P.SHOW_MUSIC_CONTROLS_DEFAULT)) {
            prepareMusicControls()
        }

        // Notifications
        if (
            prefs.get(P.SHOW_NOTIFICATION_COUNT, P.SHOW_NOTIFICATION_COUNT_DEFAULT) ||
            prefs.get(P.SHOW_NOTIFICATION_ICONS, P.SHOW_NOTIFICATION_ICONS_DEFAULT) ||
            prefs.get(P.EDGE_GLOW, P.EDGE_GLOW_DEFAULT)
        ) {
            NotificationService.listeners.add(this)
        }

        // Proximity
        if (prefs.get(P.POCKET_MODE, P.POCKET_MODE_DEFAULT)) {
            prepareProximity()
        }

        // DND
        if (prefs.get(P.DO_NOT_DISTURB, P.DO_NOT_DISTURB_DEFAULT)) {
            prepareDoNotDisturb()
        }

        // Call recognition
        prepareCallRecognition()

        // Broadcast Receivers
        if (Build.VERSION.SDK_INT >= 34) { // Android 14 (UPSIDE_DOWN_CAKE)
            registerReceiver(systemReceiver, systemFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(systemReceiver, systemFilter)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onStart() {
        super.onStart()
        CombinedServiceReceiver.isAlwaysOnRunning = true
        servicesRunning = true
        
        // Record initial notification count for edge glow tracking
        initialNotificationCount = NotificationService.count
        lastNotificationCount = NotificationService.count
        
        if (
            prefs.get(P.SHOW_NOTIFICATION_COUNT, P.SHOW_NOTIFICATION_COUNT_DEFAULT) ||
            prefs.get(P.SHOW_NOTIFICATION_ICONS, P.SHOW_NOTIFICATION_ICONS_DEFAULT) ||
            prefs.get(P.EDGE_GLOW, P.EDGE_GLOW_DEFAULT)
        ) {
            NotificationService.activeService?.refreshNotifications()
            onNotificationsChanged()
        }
        
        // Update loop for clock and weather
        startAODUpdateLoop()
        fetchWeather()
        
        val millisTillEnd: Long = Rules(this).millisTillEnd()
        if (millisTillEnd > -1L) mainHandler.postDelayed({ finishAndOff() }, millisTillEnd)

        val timeoutSetting = prefs.get(P.RULES_TIMEOUT, P.RULES_TIMEOUT_DEFAULT)
        if (timeoutSetting != 0) {
            timeoutDuration = timeoutSetting * MILLISECONDS_PER_SECOND
            setupTimeoutRunnable()
        }

        updateRefreshRate()
        
        if (prefs.get(
                P.DO_NOT_DISTURB,
                P.DO_NOT_DISTURB_DEFAULT,
            ) && notificationAccess
        ) {
            notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
        if (prefs.get(P.ROOT_MODE, P.ROOT_MODE_DEFAULT) &&
            prefs.get(
                P.POWER_SAVING_MODE,
                P.POWER_SAVING_MODE_DEFAULT,
            )
        ) {
            Root.shell("settings put global low_power 1 & dumpsys deviceidle force-idle")
        }
        if (prefs.get(
                P.DISABLE_HEADS_UP_NOTIFICATIONS,
                P.DISABLE_HEADS_UP_NOTIFICATIONS_DEFAULT,
            )
        ) {
            Root.shell("settings put global heads_up_notifications_enabled 0")
        }
        if (prefs.get(P.POCKET_MODE, P.POCKET_MODE_DEFAULT)) {
            sensorManager?.registerListener(
                sensorEventListener,
                sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                SENSOR_DELAY_SLOW,
                SENSOR_DELAY_SLOW,
            )
        }
    }
    
    private fun setupTimeoutRunnable() {
        timeoutRunnable = Runnable { finishAndOff() }
        lastTimeoutResetTime = System.currentTimeMillis()
        isTimeoutPaused = false
        remainingTimeoutTime = timeoutDuration
        mainHandler.postDelayed(timeoutRunnable!!, timeoutDuration)
    }
    
    fun resetTimeout() {
        if (timeoutRunnable != null && timeoutDuration > 0 && !isTimeoutPaused) {
            mainHandler.removeCallbacks(timeoutRunnable!!)
            lastTimeoutResetTime = System.currentTimeMillis()
            mainHandler.postDelayed(timeoutRunnable!!, timeoutDuration)
        }
    }
    
    fun pauseTimeout() {
        if (timeoutRunnable != null && timeoutDuration > 0 && !isTimeoutPaused) {
            mainHandler.removeCallbacks(timeoutRunnable!!)
            isTimeoutPaused = true
            remainingTimeoutTime = timeoutDuration - (System.currentTimeMillis() - lastTimeoutResetTime)
            if (remainingTimeoutTime < 0) remainingTimeoutTime = 0
        }
    }
    
    fun resumeTimeout() {
        if (timeoutRunnable != null && timeoutDuration > 0 && isTimeoutPaused) {
            lastTimeoutResetTime = System.currentTimeMillis()
            isTimeoutPaused = false
            mainHandler.postDelayed(timeoutRunnable!!, remainingTimeoutTime)
        }
    }

    private fun resetReplyTimeout() {
        cancelReplyTimeout()
        replyTimeoutRunnable = Runnable {
            peekState = peekState.copy(
                touchedNotificationIndex = null,
                isReplyMode = false,
                replyText = TextFieldValue(""),
                replyActionIndex = null
            )
            resumeTimeout()
            resetTimeout()
        }
        mainHandler.postDelayed(replyTimeoutRunnable!!, REPLY_TIMEOUT_DELAY)
    }

    private fun cancelReplyTimeout() {
        replyTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
    }

    override fun onStop() {
        super.onStop()
        servicesRunning = false
        mainHandler.removeCallbacksAndMessages(null)
        if (prefs.get(
                P.DO_NOT_DISTURB,
                P.DO_NOT_DISTURB_DEFAULT,
            ) && notificationAccess
        ) {
            notificationManager?.setInterruptionFilter(userDND)
        }
        if (prefs.get(P.ROOT_MODE, P.ROOT_MODE_DEFAULT) &&
            prefs.get(
                P.POWER_SAVING_MODE,
                P.POWER_SAVING_MODE_DEFAULT,
            ) && !userPowerSaving
        ) {
            Root.shell(
                "settings put global low_power 0 & " +
                    "dumpsys deviceidle unforce & dumpsys battery reset",
            )
        }
        if (prefs.get(
                P.DISABLE_HEADS_UP_NOTIFICATIONS,
                P.DISABLE_HEADS_UP_NOTIFICATIONS_DEFAULT,
            )
        ) {
            Root.shell("settings put global heads_up_notifications_enabled 1")
        }
        if (prefs.get(P.POCKET_MODE, P.POCKET_MODE_DEFAULT)) {
            sensorManager?.unregisterListener(
                sensorEventListener,
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        CombinedServiceReceiver.isAlwaysOnRunning = false
        
        timeoutRunnable = null
        
        if (
            prefs.get(P.SHOW_NOTIFICATION_COUNT, P.SHOW_NOTIFICATION_COUNT_DEFAULT) ||
            prefs.get(P.SHOW_NOTIFICATION_ICONS, P.SHOW_NOTIFICATION_ICONS_DEFAULT) ||
            prefs.get(P.EDGE_GLOW, P.EDGE_GLOW_DEFAULT)
        ) {
            NotificationService.listeners.remove(this)
        }
        
        if (onActiveSessionsChangedListener != null) {
            try {
                val mediaSessionManager =
                    getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
                mediaSessionManager.removeOnActiveSessionsChangedListener(
                    onActiveSessionsChangedListener ?: return
                )
            } catch (e: Exception) {
                Log.w(Global.LOG_TAG, e.toString())
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && onModeChangedListener != null) {
            (getSystemService(AUDIO_SERVICE) as AudioManager).removeOnModeChangedListener(
                onModeChangedListener ?: error("onModeChangedListener is null."),
            )
        }
        unregisterReceiver(systemReceiver)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isProximate) return true // consume silently, do nothing
        return super.dispatchTouchEvent(ev)
    }

    override fun finishAndOff() {
        CombinedServiceReceiver.hasRequestedStop = true
        super.finishAndOff()
    }

    override fun onNotificationsChanged() {
        if (!servicesRunning) return
        
        val oldTouchedIndex = peekState.touchedNotificationIndex
        val oldSbn = if (oldTouchedIndex != null) peekState.detailedNotifications.getOrNull(oldTouchedIndex) else null

        val newNotifications = NotificationService.notifications.toList()
        val newDetailed = NotificationService.detailed.toList()

        var newTouchedIndex: Int? = null
        if (oldSbn != null) {
            newTouchedIndex = newDetailed.indexOfFirst { it.key == oldSbn.key }
            if (newTouchedIndex == -1) newTouchedIndex = null
        }

        peekState = peekState.copy(
            notifications = newNotifications,
            detailedNotifications = newDetailed,
            touchedNotificationIndex = newTouchedIndex,
            hasNewNotifications = NotificationService.count > lastNotificationCount,
            isReplyMode = if (newTouchedIndex == null) false else peekState.isReplyMode,
            replyText = if (newTouchedIndex == null) TextFieldValue("") else peekState.replyText,
            replyActionIndex = if (newTouchedIndex == null) null else peekState.replyActionIndex
        )
        
        if (oldTouchedIndex != null && newTouchedIndex == null) {
            onInteractionEnded()
            resumeTimeout()
            resetTimeout()
        }

        // Update the last count for next comparison
        lastNotificationCount = NotificationService.count
        updateRefreshRate()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val SMALL_DELAY: Long = 300
        private const val MILLISECONDS_PER_SECOND: Long = 1_000
        private const val SENSOR_DELAY_SLOW: Int = 1_000_000
        private var instance: AlwaysOn? = null

        fun finish() {
            instance?.finish()
        }

        fun finishAndOff() {
            instance?.finishAndOff()
        }

    }
}
