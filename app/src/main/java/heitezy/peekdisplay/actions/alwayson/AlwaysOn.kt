package heitezy.peekdisplay.actions.alwayson

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationManager
import android.app.RemoteInput
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.OffActivity
import heitezy.peekdisplay.actions.alwayson.blinds.BlindsEffect
import heitezy.peekdisplay.actions.alwayson.controllers.*
import heitezy.peekdisplay.helpers.Global
import heitezy.peekdisplay.helpers.IconHelper
import heitezy.peekdisplay.helpers.KeyguardHelper
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.helpers.Root
import heitezy.peekdisplay.helpers.Rules
import heitezy.peekdisplay.receivers.CombinedServiceReceiver
import heitezy.peekdisplay.services.NotificationService
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.sqrt

class AlwaysOn : OffActivity(), NotificationService.OnNotificationsChangedListener {
    private val viewModel: AlwaysOnViewModel by viewModels()

    internal lateinit var prefs: P

    private lateinit var sensorController: SensorController
    private lateinit var mediaController: MediaControllerWrapper
    private lateinit var dndController: DndController
    private lateinit var refreshRateController: RefreshRateController
    private lateinit var systemReceiverController: SystemReceiverController

    // Edge glow tracking
    private var initialNotificationCount: Int = 0
    private var lastNotificationCount: Int = 0
    private var isOpeningNotification: Boolean = false

    // Call recognition
    private var onModeChangedListener: AudioManager.OnModeChangedListener? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Keyboard/Reply timeout
    private var replyTimeoutRunnable: Runnable? = null
    private val REPLY_TIMEOUT_DELAY = 60000L

    // Interaction cooldown
    private val interactionCooldownRunnable = Runnable {
        viewModel.interactionState = viewModel.interactionState.copy(isInteracting = false)
        updateRefreshRate()
    }

    // Timeout tracking
    private var timeoutRunnable: Runnable? = null
    private var timeoutDuration: Long = 0
    private var isTimeoutPaused: Boolean = false
    private var remainingTimeoutTime: Long = 0
    private var lastTimeoutResetTime: Long = 0

    private fun updateRefreshRate() {
        refreshRateController.setRefreshRate(viewModel.isStatic())
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
        
        // Initialize Controllers
        sensorController = SensorController(this) { isProximate ->
            viewModel.isProximate = isProximate
        }
        mediaController = MediaControllerWrapper(this) { musicString, albumArt, state ->
            val isPlaying = state == PlaybackState.STATE_PLAYING
            val wasPlaying = viewModel.musicState.wasPlaying || isPlaying

            if (musicString.isNotEmpty()) {
                viewModel.musicState = viewModel.musicState.copy(
                    text = musicString,
                    albumArt = albumArt ?: viewModel.musicState.albumArt,
                    isPlaying = isPlaying,
                    wasPlaying = wasPlaying
                )
            } else if (!wasPlaying) {
                viewModel.musicState = viewModel.musicState.copy(
                    text = "",
                    albumArt = null,
                    isPlaying = false,
                    wasPlaying = false
                )
            } else {
                viewModel.musicState = viewModel.musicState.copy(
                    isPlaying = isPlaying,
                    wasPlaying = wasPlaying
                )
            }
        }
        dndController = DndController(this)
        refreshRateController = RefreshRateController(window, this)
        systemReceiverController = SystemReceiverController(this) { intent ->
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                    if (level <= prefs.get(P.RULES_BATTERY, P.RULES_BATTERY_DEFAULT)) {
                        finishAndOff()
                        return@SystemReceiverController
                    }
                    
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                                    status == BatteryManager.BATTERY_STATUS_FULL ||
                                    plugged > 0
                    
                    viewModel.batteryState = viewModel.batteryState.copy(
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
                    dndController.captureUserDnd()
                    if (!Rules(this@AlwaysOn, prefs).canShow(this@AlwaysOn)) finishAndOff()
                }
            }
        }

        viewModel.updateInitialAODState()
        prepareView()

        // Music Controls
        if (prefs.get(P.SHOW_MUSIC_CONTROLS, P.SHOW_MUSIC_CONTROLS_DEFAULT)) {
            mediaController.start()
        }

        // Notifications
        if (
            prefs.get(P.SHOW_NOTIFICATION_COUNT, P.SHOW_NOTIFICATION_COUNT_DEFAULT) ||
            prefs.get(P.SHOW_NOTIFICATION_ICONS, P.SHOW_NOTIFICATION_ICONS_DEFAULT) ||
            prefs.get(P.EDGE_GLOW, P.EDGE_GLOW_DEFAULT)
        ) {
            NotificationService.listeners.add(this)
        }

        // Call recognition
        prepareCallRecognition()

        systemReceiverController.start()
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
            targetValue = if (viewModel.isProximate) 0f else 1f,
            animationSpec = tween(1000),
            label = "alpha"
        )

        val animatedDriftY by animateFloatAsState(
            targetValue = viewModel.interactionState.driftY,
            animationSpec = if (viewModel.appearanceState.animateMotion) tween(2000) else tween(0),
            label = "driftY"
        )

        val animatedFpDriftY by animateFloatAsState(
            targetValue = viewModel.interactionState.fpDriftY,
            animationSpec = if (viewModel.appearanceState.animateMotion) tween(2000) else tween(0),
            label = "fpDriftY"
        )

        BlindsEffect(
            enabled = viewModel.appearanceState.blindsEffect,
            backgroundOnly = viewModel.appearanceState.blindsBackgroundOnly,
            onActiveChanged = { active ->
                // Keep the refresh rate high while the blinds animate.
                if (active) onInteractionStarted() else onInteractionEnded()
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha),
        ) {
            Content(
                clockState = viewModel.clockState,
                batteryState = viewModel.batteryState,
                musicState = viewModel.musicState,
                weatherState = viewModel.weatherState,
                calendarState = viewModel.calendarState,
                messageState = viewModel.messageState,
                notificationState = viewModel.notificationState,
                fingerprintState = viewModel.fingerprintState,
                appearanceState = viewModel.appearanceState,
                interactionState = viewModel.interactionState.copy(driftY = animatedDriftY, fpDriftY = animatedFpDriftY),
                onSkipPrevious = {
                    onInteractionStarted()
                    mediaController.controller?.transportControls?.skipToPrevious()
                    onInteractionEnded()
                },
                onSkipNext = {
                    onInteractionStarted()
                    mediaController.controller?.transportControls?.skipToNext()
                    onInteractionEnded()
                },
                onTitleClick = {
                    onInteractionStarted()
                    resetTimeout()
                    if (mediaController.playbackState == PlaybackState.STATE_PLAYING) {
                        mediaController.controller?.transportControls?.pause()
                    } else if (mediaController.playbackState == PlaybackState.STATE_PAUSED) {
                        mediaController.controller?.transportControls?.play()
                    }
                    onInteractionEnded()
                },
                onNotificationHoldStarted = { index ->
                    onInteractionStarted()
                    resetTimeout()
                    viewModel.notificationState = viewModel.notificationState.copy(
                        touchedIndex = index,
                        isReplyMode = false,
                        replyText = TextFieldValue(""),
                        replyActionIndex = null
                    )
                    pauseTimeout()
                },
                onNotificationHoldFinished = {
                    onInteractionEnded()
                    viewModel.notificationState = viewModel.notificationState.copy(
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
                    val sbn = viewModel.notificationState.detailed.getOrNull(notificationIndex)
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
                            viewModel.notificationState = viewModel.notificationState.copy(touchedIndex = null)
                        } catch (_: Exception) {
                            Log.e(Global.LOG_TAG, "Action send failed")
                        }
                    }
                    onInteractionEnded()
                },
                onReplyActionClick = { notificationIndex, actionIndex ->
                    onInteractionStarted()
                    resetTimeout()
                    viewModel.notificationState = viewModel.notificationState.copy(
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
                    val sbn = viewModel.notificationState.detailed.getOrNull(index)
                    if (sbn != null) {
                        NotificationService.removeNotificationsByPackageAndId(sbn.packageName, sbn.id, sbn.tag)
                        viewModel.notificationState = viewModel.notificationState.copy(touchedIndex = null)
                    }
                    onInteractionEnded()
                },
                onReplyTextChange = { text ->
                    onInteractionStarted()
                    viewModel.notificationState = viewModel.notificationState.copy(replyText = text)
                    resetReplyTimeout()
                    onInteractionEnded()
                },
                onSendReply = { index ->
                    onInteractionStarted()
                    resetTimeout()
                    cancelReplyTimeout()
                    val sbn = viewModel.notificationState.detailed.getOrNull(index)
                    val action = viewModel.notificationState.replyActionIndex
                        ?.let { sbn?.notification?.actions?.getOrNull(it) }
                        ?: sbn?.notification?.actions?.find { it.remoteInputs != null }
                    if (action != null && viewModel.notificationState.replyText.text.isNotEmpty()) {
                        val remoteInputs = action.remoteInputs ?: return@Content
                        val remoteInput = remoteInputs[0]
                        val results = Bundle().apply {
                            putCharSequence(remoteInput.resultKey, viewModel.notificationState.replyText.text)
                        }
                        val intent = Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        RemoteInput.addResultsToIntent(remoteInputs, intent, results)
                        try {
                            action.actionIntent.send(this@AlwaysOn, 0, intent, null, null, null, getActivityOptionsBundle())
                            viewModel.notificationState = viewModel.notificationState.copy(
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
                    if (!viewModel.appearanceState.disableDoubleTap && viewModel.notificationState.touchedIndex == null) {
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

                    if (viewModel.appearanceState.fingerprintInteractionMode == "swipe") {
                        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        val isLockOpen = distance > 300

                        val iconRes = when {
                            viewModel.appearanceState.useLockIcon && isLockOpen -> R.drawable.ic_lock_open
                            viewModel.appearanceState.useLockIcon -> R.drawable.ic_lock
                            else -> R.drawable.ic_fingerprint_white
                        }

                        viewModel.fingerprintState = viewModel.fingerprintState.copy(
                            isTouched = isTouched,
                            iconRes = iconRes
                        )
                        viewModel.notificationState = viewModel.notificationState.copy(
                            hoveredIndex = if (!isTouched) null else viewModel.notificationState.hoveredIndex
                        )

                        if (!isTouched && isLockOpen) {
                            KeyguardHelper.dismissKeyguard(this@AlwaysOn)
                            finish()
                        }
                    } else {
                        viewModel.fingerprintState = viewModel.fingerprintState.copy(isTouched = isTouched)
                        viewModel.notificationState = viewModel.notificationState.copy(
                            hoveredIndex = if (!isTouched) null else viewModel.notificationState.hoveredIndex
                        )
                    }
                },
                onFingerprintLongPress = {
                    onInteractionStarted()
                    if (viewModel.appearanceState.fingerprintInteractionMode == "longpress") {
                        KeyguardHelper.dismissKeyguard(this@AlwaysOn)
                        finish()
                    }
                    onInteractionEnded()
                },
                onOpenNotification = { index ->
                    onInteractionStarted()
                    val sbn = viewModel.notificationState.detailed.getOrNull(index)
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
                    if (viewModel.notificationState.hoveredIndex != index) {
                        viewModel.notificationState = viewModel.notificationState.copy(hoveredIndex = index)
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

    private fun onInteractionStarted() {
        mainHandler.removeCallbacks(interactionCooldownRunnable)
        if (!viewModel.interactionState.isInteracting) {
            viewModel.interactionState = viewModel.interactionState.copy(isInteracting = true)
            updateRefreshRate()
        }
    }

    private fun onInteractionEnded() {
        mainHandler.removeCallbacks(interactionCooldownRunnable)
        mainHandler.postDelayed(interactionCooldownRunnable, 1500)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onStart() {
        super.onStart()
        CombinedServiceReceiver.isAlwaysOnRunning = true
        
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
        
        // Update loops
        viewModel.startAODUpdateLoop { updateRefreshRate() }
        viewModel.startDriftLoop { updateRefreshRate() }
        viewModel.fetchWeather()
        
        val millisTillEnd: Long = Rules(this, prefs).millisTillEnd()
        if (millisTillEnd > -1L) mainHandler.postDelayed({ finishAndOff() }, millisTillEnd)

        val timeoutSetting = prefs.get(P.RULES_TIMEOUT, P.RULES_TIMEOUT_DEFAULT)
        if (timeoutSetting != 0) {
            timeoutDuration = timeoutSetting * MILLISECONDS_PER_SECOND
            setupTimeoutRunnable()
        }

        updateRefreshRate()
        
        if (prefs.get(P.DO_NOT_DISTURB, P.DO_NOT_DISTURB_DEFAULT)) {
            dndController.enableAodDnd()
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
            sensorController.start(SENSOR_DELAY_SLOW)
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
            viewModel.notificationState = viewModel.notificationState.copy(
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
        mainHandler.removeCallbacksAndMessages(null)
        
        if (prefs.get(P.DO_NOT_DISTURB, P.DO_NOT_DISTURB_DEFAULT)) {
            dndController.restoreUserDnd()
        }

        if (prefs.get(P.ROOT_MODE, P.ROOT_MODE_DEFAULT) &&
            prefs.get(
                P.POWER_SAVING_MODE,
                P.POWER_SAVING_MODE_DEFAULT,
            )
        ) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isPowerSaveMode) {
                Root.shell(
                    "settings put global low_power 0 & " +
                        "dumpsys deviceidle unforce & dumpsys battery reset",
                )
            }
        }
        if (prefs.get(
                P.DISABLE_HEADS_UP_NOTIFICATIONS,
                P.DISABLE_HEADS_UP_NOTIFICATIONS_DEFAULT,
            )
        ) {
            Root.shell("settings put global heads_up_notifications_enabled 1")
        }
        if (prefs.get(P.POCKET_MODE, P.POCKET_MODE_DEFAULT)) {
            sensorController.stop()
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
        
        mediaController.stop()
        systemReceiverController.stop()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && onModeChangedListener != null) {
            (getSystemService(AUDIO_SERVICE) as AudioManager).removeOnModeChangedListener(
                onModeChangedListener ?: return
            )
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (viewModel.isProximate) return true // consume silently
        return super.dispatchTouchEvent(ev)
    }

    override fun finishAndOff() {
        if (isOpeningNotification) return
        CombinedServiceReceiver.hasRequestedStop = true
        super.finishAndOff()
    }

    override fun onNotificationsChanged() {
        val count = NotificationService.count
        val detailed = NotificationService.detailed
        
        val oldTouchedIndex = viewModel.notificationState.touchedIndex
        val oldSbn = if (oldTouchedIndex != null) viewModel.notificationState.detailed.getOrNull(oldTouchedIndex) else null

        val newDetailed = detailed.toImmutableList()
        var newTouchedIndex: Int? = null
        if (oldSbn != null) {
            newTouchedIndex = newDetailed.indexOfFirst { it.key == oldSbn.key }
            if (newTouchedIndex == -1) newTouchedIndex = null
        }

        viewModel.notificationState = viewModel.notificationState.copy(
            entries = NotificationService.notifications.toImmutableList(),
            detailed = newDetailed,
            touchedIndex = newTouchedIndex,
            hasNew = count > lastNotificationCount && !isOpeningNotification,
            isReplyMode = if (newTouchedIndex == null) false else viewModel.notificationState.isReplyMode,
            replyText = if (newTouchedIndex == null) TextFieldValue("") else viewModel.notificationState.replyText,
            replyActionIndex = if (newTouchedIndex == null) null else viewModel.notificationState.replyActionIndex
        )
        
        if (oldTouchedIndex != null && newTouchedIndex == null) {
            onInteractionEnded()
            resumeTimeout()
            resetTimeout()
        }

        lastNotificationCount = count
        updateRefreshRate()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        const val SMALL_DELAY: Long = 300
        const val MILLISECONDS_PER_SECOND: Long = 1_000
        const val SENSOR_DELAY_SLOW: Int = 1_000_000
        const val WEATHER_REQUEST_TAG: String = "PeekDisplay.weather"

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
