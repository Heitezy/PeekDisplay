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
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import heitezy.peekdisplay.Application
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.OffActivity
import heitezy.peekdisplay.actions.alwayson.data.BatteryState
import heitezy.peekdisplay.actions.alwayson.data.CalendarState
import heitezy.peekdisplay.actions.alwayson.data.ClockState
import heitezy.peekdisplay.actions.alwayson.data.Data
import heitezy.peekdisplay.actions.alwayson.data.FingerprintState
import heitezy.peekdisplay.actions.alwayson.data.InteractionState
import heitezy.peekdisplay.actions.alwayson.data.MessageState
import heitezy.peekdisplay.actions.alwayson.data.MusicState
import heitezy.peekdisplay.actions.alwayson.data.NotificationState
import heitezy.peekdisplay.actions.alwayson.data.AppearanceState
import heitezy.peekdisplay.actions.alwayson.data.WeatherState
import heitezy.peekdisplay.helpers.Global
import heitezy.peekdisplay.helpers.IconHelper
import heitezy.peekdisplay.helpers.KeyguardHelper
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.helpers.Root
import heitezy.peekdisplay.helpers.Rules
import heitezy.peekdisplay.receivers.CombinedServiceReceiver
import heitezy.peekdisplay.services.NotificationService
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.sqrt

class AlwaysOn : OffActivity(), NotificationService.OnNotificationsChangedListener {
    @JvmField
    internal var servicesRunning: Boolean = false

    internal lateinit var prefs: P

    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var clockState by mutableStateOf(ClockState())
    private var batteryState by mutableStateOf(BatteryState())
    private var musicState by mutableStateOf(MusicState())
    private var weatherState by mutableStateOf(WeatherState())
    private var calendarState by mutableStateOf(CalendarState())
    private var messageState by mutableStateOf(MessageState())
    private var notificationState by mutableStateOf(NotificationState())
    private var fingerprintState by mutableStateOf(FingerprintState())
    private var appearanceState by mutableStateOf(AppearanceState())
    private var interactionState by mutableStateOf(InteractionState())

    // Media Controls
    private var onActiveSessionsChangedListener: AlwaysOnOnActiveSessionsChangedListener? = null

    // Edge glow tracking
    private var initialNotificationCount: Int = 0
    private var lastNotificationCount: Int = 0
    private var isOpeningNotification: Boolean = false

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
        interactionState = interactionState.copy(isInteracting = false)
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
        
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                        status == BatteryManager.BATTERY_STATUS_FULL ||
                        plugged > 0

        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        clockState = ClockState(
            time = timeFormat.format(now),
            date = dateFormat.format(now),
            hour = calendar.get(java.util.Calendar.HOUR_OF_DAY),
            minute = calendar.get(java.util.Calendar.MINUTE),
            second = calendar.get(java.util.Calendar.SECOND)
        )

        batteryState = BatteryState(
            level = level,
            isCharging = isCharging,
            iconRes = IconHelper.getBatteryIcon(level)
        )

        messageState = MessageState(
            message = prefs.get(P.MESSAGE, P.MESSAGE_DEFAULT)
        )

        activityScope.launch {
            val events = withContext(Dispatchers.IO) {
                Data.getCalendar(this@AlwaysOn, prefs)
            }
            calendarState = CalendarState(
                events = events.toImmutableList()
            )
        }

        appearanceState = AppearanceState(
            theme = theme,
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

        fingerprintState = FingerprintState(
            iconRes = if (appearanceState.useLockIcon) R.drawable.ic_lock else R.drawable.ic_fingerprint_white
        )

        // Load custom background asynchronously
        activityScope.launch {
            val customBgPathOrEncoded = prefs.get(P.CUSTOM_BACKGROUND, "")
            val customBgBitmap = if (customBgPathOrEncoded.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    try {
                        val file = File(customBgPathOrEncoded)
                        if (file.exists() && file.isFile) {
                            // It's a file path
                            BitmapFactory.decodeFile(customBgPathOrEncoded)
                        } else {
                            // Legacy Base64
                            val bytes = Base64.decode(customBgPathOrEncoded, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            } else null

            appearanceState = appearanceState.copy(customBackground = customBgBitmap)
        }
    }

    private fun fetchWeather() {
        if (!prefs.get(P.SHOW_WEATHER, P.SHOW_WEATHER_DEFAULT)) return

        val url = prefs.getWeatherUrl()

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                weatherState = weatherState.copy(weather = response)
            },
            { error ->
                Log.e(Global.LOG_TAG, "Weather fetch failed: $error")
            }
        )
        stringRequest.tag = WEATHER_REQUEST_TAG
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
                    val isMultiline = appearanceState.theme == P.USER_THEME_SAMSUNG || appearanceState.theme == P.USER_THEME_ONEPLUS || appearanceState.theme == P.USER_THEME_ANALOG
                    val timeFormatString = if (isMultiline) {
                        prefs.getMultiLineTimeFormat()
                    } else {
                        prefs.getSingleLineTimeFormat()
                    }
                    val timeFormat = getTimeFormat(timeFormatString)
                    val dateFormat = getDateFormat(prefs.get(P.DATE_FORMAT, P.DATE_FORMAT_DEFAULT))
                    val now = System.currentTimeMillis()
                    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
                    clockState = clockState.copy(
                        time = timeFormat.format(now),
                        date = dateFormat.format(now),
                        hour = calendar.get(java.util.Calendar.HOUR_OF_DAY),
                        minute = calendar.get(java.util.Calendar.MINUTE),
                        second = calendar.get(java.util.Calendar.SECOND)
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
                        
                        batteryState = batteryState.copy(
                            level = level,
                            isCharging = isCharging,
                            iconRes = IconHelper.getBatteryIcon(level)
                        )
                    }

                    Intent.ACTION_POWER_CONNECTED -> {
                        if (!Rules(this@AlwaysOn, prefs).canShow(this@AlwaysOn)) finishAndOff()
                    }

                    Intent.ACTION_POWER_DISCONNECTED -> {
                        if (!Rules(this@AlwaysOn, prefs).canShow(this@AlwaysOn)) finishAndOff()
                    }

                    NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                        if (!Rules(this@AlwaysOn, prefs).canShow(this@AlwaysOn)) finishAndOff()
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
        mainHandler.postDelayed({
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
            targetValue = interactionState.driftY,
            animationSpec = if (appearanceState.animateMotion) tween(2000) else tween(0),
            label = "driftY"
        )

        val animatedFpDriftY by animateFloatAsState(
            targetValue = interactionState.fpDriftY,
            animationSpec = if (appearanceState.animateMotion) tween(2000) else tween(0),
            label = "fpDriftY"
        )
        
        val burnInDelay = remember { prefs.get("ao_animation_delay", 2) * 60000L }
        
        LaunchedEffect(Unit) {
            while (true) {
                delay(burnInDelay)

                // Wait until interaction ends
                snapshotFlow { interactionState.isInteracting }.first { !it }

                interactionState = interactionState.copy(driftY = 32f, fpDriftY = 64f, isDrifting = true)
                updateRefreshRate()
                delay(2000)
                interactionState = interactionState.copy(isDrifting = false)
                updateRefreshRate()

                delay(burnInDelay)

                // Wait until interaction ends
                snapshotFlow { interactionState.isInteracting }.first { !it }

                interactionState = interactionState.copy(driftY = 0f, fpDriftY = 0f, isDrifting = true)
                updateRefreshRate()
                delay(2000)
                interactionState = interactionState.copy(isDrifting = false)
                updateRefreshRate()
            }
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)) {
            Content(
                clockState = clockState,
                batteryState = batteryState,
                musicState = musicState,
                weatherState = weatherState,
                calendarState = calendarState,
                messageState = messageState,
                notificationState = notificationState,
                fingerprintState = fingerprintState,
                appearanceState = appearanceState,
                interactionState = interactionState.copy(driftY = animatedDriftY, fpDriftY = animatedFpDriftY),
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
                    notificationState = notificationState.copy(
                        touchedIndex = index,
                        isReplyMode = false,
                        replyText = TextFieldValue(""),
                        replyActionIndex = null
                    )
                    pauseTimeout()
                },
                onNotificationHoldFinished = {
                    onInteractionEnded()
                    notificationState = notificationState.copy(
                        touchedIndex = null,
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
                    val sbn = notificationState.detailed.getOrNull(notificationIndex)
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
                            notificationState = notificationState.copy(touchedIndex = null)
                        } catch (_: Exception) {
                            Log.e(Global.LOG_TAG, "Action send failed")
                        }
                    }
                    onInteractionEnded()
                },
                onReplyActionClick = { notificationIndex, actionIndex ->
                    onInteractionStarted()
                    resetTimeout()
                    notificationState = notificationState.copy(
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
                    val sbn = notificationState.detailed.getOrNull(index)
                    if (sbn != null) {
                        NotificationService.removeNotificationsByPackageAndId(sbn.packageName, sbn.id, sbn.tag)
                        notificationState = notificationState.copy(touchedIndex = null)
                    }
                    onInteractionEnded()
                },
                onReplyTextChange = { text ->
                    onInteractionStarted()
                    notificationState = notificationState.copy(replyText = text)
                    resetReplyTimeout()
                    onInteractionEnded()
                },
                onSendReply = { index ->
                    onInteractionStarted()
                    resetTimeout()
                    cancelReplyTimeout()
                    val sbn = notificationState.detailed.getOrNull(index)
                    val action = notificationState.replyActionIndex
                        ?.let { sbn?.notification?.actions?.getOrNull(it) }
                        ?: sbn?.notification?.actions?.find { it.remoteInputs != null }
                    if (action != null && notificationState.replyText.text.isNotEmpty()) {
                        val remoteInputs = action.remoteInputs ?: return@Content
                        val remoteInput = remoteInputs[0]
                        val results = Bundle().apply {
                            putCharSequence(remoteInput.resultKey, notificationState.replyText.text)
                        }
                        val intent = Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        RemoteInput.addResultsToIntent(remoteInputs, intent, results)
                        try {
                            action.actionIntent.send(this@AlwaysOn, 0, intent, null, null, null, getActivityOptionsBundle())
                            notificationState = notificationState.copy(
                                touchedIndex = null,
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
                    if (!appearanceState.disableDoubleTap && notificationState.touchedIndex == null) {
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

                    if (appearanceState.fingerprintInteractionMode == "swipe") {
                        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        val isLockOpen = distance > 300

                        val iconRes = when {
                            appearanceState.useLockIcon && isLockOpen -> R.drawable.ic_lock_open
                            appearanceState.useLockIcon -> R.drawable.ic_lock
                            else -> R.drawable.ic_fingerprint_white
                        }

                        fingerprintState = fingerprintState.copy(
                            isTouched = isTouched,
                            iconRes = iconRes
                        )
                        notificationState = notificationState.copy(
                            hoveredIndex = if (!isTouched) null else notificationState.hoveredIndex
                        )

                        if (!isTouched && isLockOpen) {
                            KeyguardHelper.dismissKeyguard(this@AlwaysOn)
                            finish()
                        }
                    } else {
                        fingerprintState = fingerprintState.copy(isTouched = isTouched)
                        notificationState = notificationState.copy(
                            hoveredIndex = if (!isTouched) null else notificationState.hoveredIndex
                        )
                    }
                },
                onFingerprintLongPress = {
                    onInteractionStarted()
                    if (appearanceState.fingerprintInteractionMode == "longpress") {
                        KeyguardHelper.dismissKeyguard(this@AlwaysOn)
                        finish()
                    }
                    onInteractionEnded()
                },
                onOpenNotification = { index ->
                    onInteractionStarted()
                    val sbn = notificationState.detailed.getOrNull(index)
                    if (sbn != null) {
                        try {
                            isOpeningNotification = true
                            sbn.notification.contentIntent?.send(this@AlwaysOn, 0, null, null, null, null, getActivityOptionsBundle())
                            KeyguardHelper.dismissKeyguard(this@AlwaysOn)
                            finish()
                        } catch (_: Exception) {
                            isOpeningNotification = false
                            Log.e(Global.LOG_TAG, "Notification open failed")
                        }
                    }
                    onInteractionEnded()
                },
                onNotificationHovered = { index ->
                    if (notificationState.hoveredIndex != index) {
                        notificationState = notificationState.copy(hoveredIndex = index)
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
                onMediaStateChanged = { musicString, albumArt, state ->
                    val isPlaying = state == PlaybackState.STATE_PLAYING
                    val wasPlaying = musicState.wasPlaying || isPlaying

                    if (musicString.isNotEmpty()) {
                        musicState = musicState.copy(
                            text = musicString,
                            albumArt = albumArt ?: musicState.albumArt,
                            isPlaying = isPlaying,
                            wasPlaying = wasPlaying
                        )
                    } else if (!wasPlaying) {
                        musicState = musicState.copy(
                            text = "",
                            albumArt = null,
                            isPlaying = false,
                            wasPlaying = false
                        )
                    } else {
                        musicState = musicState.copy(
                            isPlaying = isPlaying,
                            wasPlaying = wasPlaying
                        )
                    }
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
            musicState = musicState.copy(text = resources.getString(R.string.missing_permissions))
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

        val isMultiline = appearanceState.theme == P.USER_THEME_SAMSUNG || appearanceState.theme == P.USER_THEME_ONEPLUS || appearanceState.theme == P.USER_THEME_ANALOG
        val timeFormatString = if (isMultiline) {
            prefs.getMultiLineTimeFormat()
        } else {
            prefs.getSingleLineTimeFormat()
        }
        val hasSeconds = timeFormatString.contains("s", ignoreCase = true)

        val hasActiveAnimations = (appearanceState.edgeGlowEnabled && notificationState.hasNew) || interactionState.isDrifting || hasSeconds
        val isStatic = !interactionState.isInteracting && !hasActiveAnimations

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
        if (!interactionState.isInteracting) {
            interactionState = interactionState.copy(isInteracting = true)
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

        prefs = P.getP(this)
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
        
        val millisTillEnd: Long = Rules(this, prefs).millisTillEnd()
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
            notificationState = notificationState.copy(
                touchedIndex = null,
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
        activityScope.cancel()
        instance = null
        CombinedServiceReceiver.isAlwaysOnRunning = false
        
        timeoutRunnable = null

        Application.requestQueue.cancelAll(WEATHER_REQUEST_TAG)
        
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
        try {
            unregisterReceiver(systemReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(Global.LOG_TAG, e.toString())
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isProximate) return true // consume silently, do nothing
        return super.dispatchTouchEvent(ev)
    }

    override fun finishAndOff() {
        if (isOpeningNotification) return
        CombinedServiceReceiver.hasRequestedStop = true
        super.finishAndOff()
    }

    override fun onNotificationsChanged() {
        if (!servicesRunning) return
        
        val oldTouchedIndex = notificationState.touchedIndex
        val oldSbn = if (oldTouchedIndex != null) notificationState.detailed.getOrNull(oldTouchedIndex) else null

        val newNotifications = NotificationService.notifications.toList()
        val newDetailed = NotificationService.detailed.toList()

        var newTouchedIndex: Int? = null
        if (oldSbn != null) {
            newTouchedIndex = newDetailed.indexOfFirst { it.key == oldSbn.key }
            if (newTouchedIndex == -1) newTouchedIndex = null
        }

        notificationState = notificationState.copy(
            entries = newNotifications.toImmutableList(),
            detailed = newDetailed.toImmutableList(),
            touchedIndex = newTouchedIndex,
            hasNew = NotificationService.count > lastNotificationCount,
            isReplyMode = if (newTouchedIndex == null) false else notificationState.isReplyMode,
            replyText = if (newTouchedIndex == null) TextFieldValue("") else notificationState.replyText,
            replyActionIndex = if (newTouchedIndex == null) null else notificationState.replyActionIndex
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
        private const val WEATHER_REQUEST_TAG: String = "PeekDisplay.weather"

        private var instanceRef: java.lang.ref.WeakReference<AlwaysOn>? = null
        private var instance: AlwaysOn?
            get() = instanceRef?.get()
            set(value) {
                instanceRef = value?.let { java.lang.ref.WeakReference(it) }
            }

        fun finish() {
            instance?.finish()
        }

        fun finishAndOff() {
            instance?.finishAndOff()
        }

    }
}
