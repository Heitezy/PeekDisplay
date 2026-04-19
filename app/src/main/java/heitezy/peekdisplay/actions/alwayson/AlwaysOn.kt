package heitezy.peekdisplay.actions.alwayson

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.TransitionDrawable
import android.hardware.Sensor
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
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.OffActivity
import heitezy.peekdisplay.custom.DoubleTapDetector
import heitezy.peekdisplay.custom.FingerprintView
import heitezy.peekdisplay.custom.LongPressDetector
import heitezy.peekdisplay.helpers.Global
import heitezy.peekdisplay.helpers.KeyguardHelper
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.helpers.Root
import heitezy.peekdisplay.helpers.Rules
import heitezy.peekdisplay.receivers.CombinedServiceReceiver
import heitezy.peekdisplay.services.NotificationService
import android.graphics.drawable.BitmapDrawable
import android.view.inputmethod.InputMethodManager
import heitezy.peekdisplay.actions.alwayson.draw.NotificationPreview
import androidx.core.view.isVisible
import androidx.core.graphics.scale
import androidx.core.graphics.drawable.toDrawable

@Suppress("TooManyFunctions")
class AlwaysOn : OffActivity(), NotificationService.OnNotificationsChangedListener {
    @JvmField
    internal var servicesRunning: Boolean = false

    private var offsetX: Float = 0f
    internal lateinit var viewHolder: AlwaysOnViewHolder
    internal lateinit var prefs: P

    // Threads
    private var edgeGlowThread: EdgeGlowThread = EdgeGlowThread(this, null)
    private var animationThread: Thread = Thread()

    // Media Controls
    private var onActiveSessionsChangedListener: AlwaysOnOnActiveSessionsChangedListener? = null

    // Edge glow tracking
    private var initialNotificationCount: Int = 0
    private var lastNotificationCount: Int = 0

    // Battery saver
    private var userPowerSaving: Boolean = false

    // Proximity
    private var sensorManager: SensorManager? = null
    private var sensorEventListener: AlwaysOnSensorEventListener? = null

    // DND
    private var notificationManager: NotificationManager? = null
    private var notificationAccess: Boolean = false
    private var userDND: Int = NotificationManager.INTERRUPTION_FILTER_ALL

    // Call recognition
    private var onModeChangedListener: AudioManager.OnModeChangedListener? = null

    // Rules
    private val rulesHandler: Handler = Handler(Looper.getMainLooper())
    
    // Timeout tracking
    private var timeoutRunnable: Runnable? = null
    private var timeoutDuration: Long = 0
    private var isTimeoutPaused: Boolean = false
    private var remainingTimeoutTime: Long = 0
    private var lastTimeoutResetTime: Long = 0

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
                        } else if (!servicesRunning) {
                            return
                        }
                        viewHolder.customView.setBatteryStatus(
                            level,
                            intent.getIntExtra(
                                BatteryManager.EXTRA_STATUS,
                                -1,
                            ) == BatteryManager.BATTERY_STATUS_CHARGING,
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

        setContentView(R.layout.activity_aod)

        // View
        viewHolder = AlwaysOnViewHolder(this)
        viewHolder.customView.setActivity(this)
        viewHolder.customView.scaleX = prefs.displayScale()
        viewHolder.customView.scaleY = prefs.displayScale()
        
        // Set up album art callback
        viewHolder.customView.onAlbumArtStateChanged = { shouldShow, bitmap ->
            handleAlbumArtDisplay(shouldShow, bitmap)
        }
        
        if (prefs.get(P.USER_THEME, P.USER_THEME_DEFAULT) == P.USER_THEME_SAMSUNG2) {
            val size = Point()
            (getSystemService(DISPLAY_SERVICE) as DisplayManager)
                .getDisplay(Display.DEFAULT_DISPLAY)
                .getSize(size)
            offsetX = (size.x - size.x * prefs.displayScale()) * -HALF
            viewHolder.customView.translationX = offsetX
        }

        // Brightness
        if (prefs.get(P.FORCE_BRIGHTNESS, P.FORCE_BRIGHTNESS_DEFAULT)) {
            // Turning this into a single statement will not work!
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
        fullscreen(viewHolder.frame)
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                fullscreen(viewHolder.frame)
            }
        }
        /*window.decorView.setOnApplyWindowInsetsListener { _, windowInsets ->
            if (WindowInsetsCompat.toWindowInsetsCompat(windowInsets).isVisible(
                    WindowInsetsCompat.Type.statusBars()
                            or WindowInsetsCompat.Type.captionBar()
                            or WindowInsetsCompat.Type.navigationBars()
                )
            ) fullscreen(viewHolder.frame)
            windowInsets
        }*/
    }

    private fun prepareMusicControls() {
        val mediaSessionManager =
            getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val notificationListener =
            ComponentName(applicationContext, NotificationService::class.java.name)
        onActiveSessionsChangedListener =
            AlwaysOnOnActiveSessionsChangedListener(viewHolder.customView)
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
            viewHolder.customView.musicString =
                resources.getString(R.string.missing_permissions)
            // Clear album art if there's an exception
            viewHolder.customView.setAlbumArt(null)
        }
        viewHolder.customView.onTitleClicked = {
            resetTimeout()
            if (onActiveSessionsChangedListener?.state == PlaybackState.STATE_PLAYING) {
                onActiveSessionsChangedListener?.controller?.transportControls?.pause()
            } else if (onActiveSessionsChangedListener?.state == PlaybackState.STATE_PAUSED) {
                onActiveSessionsChangedListener?.controller?.transportControls?.play()
            }
        }
        viewHolder.customView.onSkipPreviousClicked = {
            resetTimeout()
            onActiveSessionsChangedListener?.controller?.transportControls?.skipToPrevious()
        }
        viewHolder.customView.onSkipNextClicked = {
            resetTimeout()
            onActiveSessionsChangedListener?.controller?.transportControls?.skipToNext()
        }
    }

    private fun prepareFingerprintIcon() {
        viewHolder.fingerprintIcn.visibility = View.VISIBLE

        updateFingerprintIconPosition()
        
        // Check the interaction mode preference
        val interactionMode = prefs.get(P.FINGERPRINT_INTERACTION_MODE, P.FINGERPRINT_INTERACTION_MODE_DEFAULT)
        
        if (interactionMode == "longpress") {
            // Long press behavior
            val longPressDetector = LongPressDetector({
                KeyguardHelper.dismissKeyguard(this)
                finish()
            })
            viewHolder.fingerprintIcn.setOnTouchListener { v, event ->
                longPressDetector.onTouchEvent(event)
                v.performClick()
            }
        } else {
            // Default swipe behavior
            viewHolder.fingerprintIcn.setOnFingerprintTouchListener(object : FingerprintView.OnFingerprintTouchListener {
                override fun onFingerprintTouchStateChanged(isTouched: Boolean, event: MotionEvent) {
                    if (isTouched) {
                        pauseTimeout()
                    } else {
                        resumeTimeout()
                        resetTimeout()
                    }
                    viewHolder.customView.handleFingerprintTouch(isTouched, event)
                }
            })
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun updateFingerprintIconPosition() {
        // Get the layout parameters
        val layoutParams = viewHolder.fingerprintIcn.layoutParams as FrameLayout.LayoutParams
        val margin = prefs.get(P.FINGERPRINT_MARGIN, P.FINGERPRINT_MARGIN_DEFAULT)
        
        // Check if we're in landscape orientation
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        
        if (isLandscape) {
            // Landscape: center vertically, align to right, use right margin
            layoutParams.gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            layoutParams.rightMargin = margin
            layoutParams.bottomMargin = 0
        } else {
            // Portrait: center horizontally, align to bottom, use bottom margin
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            layoutParams.bottomMargin = margin
            layoutParams.rightMargin = 0
        }
        
        // Apply the changes
        viewHolder.fingerprintIcn.layoutParams = layoutParams
    }

    private fun prepareProximity() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorEventListener = AlwaysOnSensorEventListener(viewHolder)
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

    private fun prepareEdgeGlow() {
        if (prefs.get(P.EDGE_GLOW_DURATION, P.EDGE_GLOW_DURATION_DEFAULT) >= MINIMUM_ANIMATION_DURATION) {
            viewHolder.frame.background =
                when (prefs.get(P.EDGE_GLOW_STYLE, P.EDGE_GLOW_STYLE_DEFAULT)) {
                    P.EDGE_GLOW_STYLE_VERTICAL ->
                        ContextCompat.getDrawable(
                            this, R.drawable.edge_glow_vertical,
                        )

                    P.EDGE_GLOW_STYLE_HORIZONTAL ->
                        ContextCompat.getDrawable(
                            this, R.drawable.edge_glow_horizontal,
                        )

                    else -> ContextCompat.getDrawable(this, R.drawable.edge_glow)
                }
            viewHolder.frame.background.setTint(
                prefs.get(
                    P.DISPLAY_COLOR_EDGE_GLOW,
                    P.DISPLAY_COLOR_EDGE_GLOW_DEFAULT,
                ),
            )
            edgeGlowThread = EdgeGlowThread(this, viewHolder.frame.background as TransitionDrawable)
            edgeGlowThread.start()
            // Initialize the edge glow thread state - only trigger for new notifications
            edgeGlowThread.shouldTriggerGlow = true
        }
    }

    private fun prepareDoubleTap() {
        val doubleTapDetector =
            DoubleTapDetector({
                val duration = prefs.get(P.VIBRATION_DURATION, P.VIBRATION_DURATION_DEFAULT).toLong()
                if (duration > 0) {
                    val vibrator =
                        getSystemService(VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                duration,
                                VibrationEffect.DEFAULT_AMPLITUDE,
                            ),
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(duration)
                    }
                }
                KeyguardHelper.dismissKeyguard(this)
            }, prefs.get(P.DOUBLE_TAP_SPEED, P.DOUBLE_TAP_SPEED_DEFAULT))
        viewHolder.frame.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                resetTimeout()
            }
            doubleTapDetector.onTouchEvent(event)
            v.performClick()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // Fingerprint icon
        if (prefs.get(P.SHOW_FINGERPRINT_ICON, P.SHOW_FINGERPRINT_ICON_DEFAULT)) {
            prepareFingerprintIcon()
        }

        // Proximity
        if (prefs.get(P.POCKET_MODE, P.POCKET_MODE_DEFAULT)) {
            prepareProximity()
        }

        // DND
        if (prefs.get(P.DO_NOT_DISTURB, P.DO_NOT_DISTURB_DEFAULT)) {
            prepareDoNotDisturb()
        }

        // Edge Glow
        if (prefs.get(P.EDGE_GLOW, P.EDGE_GLOW_DEFAULT)) {
            prepareEdgeGlow()
        }

        // Animation
        animationThread = AlwaysOnAnimationThread(this, viewHolder, offsetX)
        animationThread.start()

        // DoubleTap
        if (!prefs.get(P.DISABLE_DOUBLE_TAP, P.DISABLE_DOUBLE_TAP_DEFAULT)) {
            prepareDoubleTap()
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
        (animationThread as? AlwaysOnAnimationThread)?.updateScreenSize()

        // Update fingerprint icon positioning on orientation change
        if (viewHolder.fingerprintIcn.isVisible) {
            updateFingerprintIconPosition()
        }

        // Rebuild album art gradient orientation immediately on orientation change
        val albumArtView = findViewById<ImageView>(R.id.album_art_overlay)
        if (albumArtView != null && albumArtView.drawable is LayerDrawable) {
            val currentDrawable = albumArtView.drawable as LayerDrawable
            val bitmapLayer = currentDrawable.getDrawable(0) as? BitmapDrawable
            val currentBitmap = bitmapLayer?.bitmap
            if (currentBitmap != null) {
                val screenWidth = resources.displayMetrics.widthPixels
                val bitmapDrawable =
                    currentBitmap.scale(screenWidth, screenWidth).toDrawable(resources)
                val tintDrawable = android.graphics.Color.argb(100, 0, 0, 0).toDrawable()
                val gradientDrawable = GradientDrawable()
                gradientDrawable.orientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                    GradientDrawable.Orientation.LEFT_RIGHT else GradientDrawable.Orientation.TOP_BOTTOM
                gradientDrawable.shape = GradientDrawable.RECTANGLE
                gradientDrawable.colors = intArrayOf(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.BLACK
                )
                gradientDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT
                val gradientOverlay = LayerDrawable(arrayOf(tintDrawable, gradientDrawable))
                val layers = LayerDrawable(arrayOf(bitmapDrawable, gradientOverlay))
                albumArtView.setImageDrawable(layers)
                albumArtView.visibility = View.VISIBLE
            }
        }
    }

    @Suppress("LongMethod")
    override fun onStart() {
        super.onStart()
        CombinedServiceReceiver.isAlwaysOnRunning = true
        servicesRunning = true
        
        // Record initial notification count for edge glow tracking
        initialNotificationCount = NotificationService.count
        lastNotificationCount = NotificationService.count
        
        if (prefs.get(P.SHOW_CLOCK, P.SHOW_CLOCK_DEFAULT) ||
            prefs.get(
                P.SHOW_DATE,
                P.SHOW_DATE_DEFAULT,
            )
        ) {
            viewHolder.customView.startClockHandler()
        }
        if (
            prefs.get(P.SHOW_NOTIFICATION_COUNT, P.SHOW_NOTIFICATION_COUNT_DEFAULT) ||
            prefs.get(P.SHOW_NOTIFICATION_ICONS, P.SHOW_NOTIFICATION_ICONS_DEFAULT) ||
            prefs.get(P.EDGE_GLOW, P.EDGE_GLOW_DEFAULT)
        ) {
            onNotificationsChanged()
        }
        val millisTillEnd: Long = Rules(this).millisTillEnd()
        if (millisTillEnd > -1L) rulesHandler.postDelayed({ finishAndOff() }, millisTillEnd)

        val timeoutSetting = prefs.get(P.RULES_TIMEOUT, P.RULES_TIMEOUT_DEFAULT)
        if (timeoutSetting != 0) {
            timeoutDuration = timeoutSetting * MILLISECONDS_PER_SECOND
            setupTimeoutRunnable()
        }
        
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
        rulesHandler.postDelayed(timeoutRunnable!!, timeoutDuration)
    }
    
    fun resetTimeout() {
        if (timeoutRunnable != null && timeoutDuration > 0 && !isTimeoutPaused) {
            rulesHandler.removeCallbacks(timeoutRunnable!!)
            lastTimeoutResetTime = System.currentTimeMillis()
            rulesHandler.postDelayed(timeoutRunnable!!, timeoutDuration)
        }
    }
    
    fun pauseTimeout() {
        if (timeoutRunnable != null && timeoutDuration > 0 && !isTimeoutPaused) {
            rulesHandler.removeCallbacks(timeoutRunnable!!)
            isTimeoutPaused = true
            remainingTimeoutTime = timeoutDuration - (System.currentTimeMillis() - lastTimeoutResetTime)
            if (remainingTimeoutTime < 0) remainingTimeoutTime = 0
        }
    }
    
    fun resumeTimeout() {
        if (timeoutRunnable != null && timeoutDuration > 0 && isTimeoutPaused) {
            lastTimeoutResetTime = System.currentTimeMillis()
            isTimeoutPaused = false
            rulesHandler.postDelayed(timeoutRunnable!!, remainingTimeoutTime)
        }
    }

    override fun onStop() {
        super.onStop()
        servicesRunning = false
        if (prefs.get(P.SHOW_CLOCK, P.SHOW_CLOCK_DEFAULT) ||
            prefs.get(
                P.SHOW_DATE,
                P.SHOW_DATE_DEFAULT,
            )
        ) {
            viewHolder.customView.stopClockHandler()
        }
        rulesHandler.removeCallbacksAndMessages(null)
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
        
        // Clear any album art to avoid memory leaks
        viewHolder.customView.setAlbumArt(null)
        
        // Reset and stop edge glow thread
        if (prefs.get(P.EDGE_GLOW, P.EDGE_GLOW_DEFAULT)) {
            edgeGlowThread.reset()
            edgeGlowThread.interrupt()
        }
        animationThread.interrupt()
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

    override fun finishAndOff() {
        CombinedServiceReceiver.hasRequestedStop = true
        // Reset edge glow state before finishing
        if (prefs.get(P.EDGE_GLOW, P.EDGE_GLOW_DEFAULT)) {
            edgeGlowThread.reset()
        }
        super.finishAndOff()
    }

    override fun onNotificationsChanged() {
        if (!servicesRunning) return
        
        // If keyboard is showing and we're in reply mode, hide keyboard when a new notification arrives
        if (NotificationPreview.isReplyActive()) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            viewHolder.customView.windowToken?.let { token ->
                imm.hideSoftInputFromWindow(token, 0)
            }
            NotificationPreview.clearReplyMode()
            viewHolder.customView.touchedNotificationIndex = null
            NotificationPreview.setCurrentNotification(null)
            viewHolder.customView.invalidate()
            resumeTimeout()
            resetTimeout()
        }
        
        viewHolder.customView.notifyNotificationDataChanged()
        if (prefs.get(P.EDGE_GLOW, P.EDGE_GLOW_DEFAULT)) {
            // Check if notification count has changed
            if (NotificationService.count > lastNotificationCount) {
                // New notification received while AlwaysOn is active
                edgeGlowThread.hasNewNotifications = true
            } else if (NotificationService.count < lastNotificationCount) {
                // Notification count decreased - stop edge glow
                edgeGlowThread.hasNewNotifications = false
            }
            
            // Update the last count for next comparison
            lastNotificationCount = NotificationService.count
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        //Pass key events to the custom view for handling replies
        if (viewHolder.customView.handleKeyEvent(keyCode, event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleAlbumArtDisplay(shouldShow: Boolean, albumArt: Bitmap?) {
        if (shouldShow && albumArt != null) {
            // Check if we already have an album art overlay view
            var albumArtView = findViewById<ImageView>(R.id.album_art_overlay)
            
            // If not, create one
            if (albumArtView == null) {
                albumArtView = ImageView(this)
                albumArtView.id = R.id.album_art_overlay
                
                // Set layout parameters to match full screen width and be square
                val screenWidth = resources.displayMetrics.widthPixels
                val params = FrameLayout.LayoutParams(screenWidth, screenWidth)
                params.gravity = Gravity.TOP
                
                // Apply tint and add gradient
                val drawable = GradientDrawable()
                drawable.orientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                    GradientDrawable.Orientation.LEFT_RIGHT else GradientDrawable.Orientation.TOP_BOTTOM
                drawable.shape = GradientDrawable.RECTANGLE
                drawable.colors = intArrayOf(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.BLACK
                )
                drawable.gradientType = GradientDrawable.LINEAR_GRADIENT
                
                // Create a layer list for the gradient overlay
                val gradientOverlay = LayerDrawable(arrayOf(
                    android.graphics.Color.argb(100, 0, 0, 0).toDrawable(),
                    drawable
                ))
                
                // Use BitmapDrawable as the image source
                val bitmapDrawable =
                    albumArt.scale(screenWidth, screenWidth)
                        .toDrawable(resources)
                
                // Combine both in a layer list
                val layers = LayerDrawable(arrayOf(bitmapDrawable, gradientOverlay))
                
                albumArtView.setImageDrawable(layers)
                
                // Add to the root view at index 0 (behind everything else)
                val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
                rootView.addView(albumArtView, 0, params)
            } else {
                // Just update the existing view
                val screenWidth = resources.displayMetrics.widthPixels
                
                // Use BitmapDrawable as the image source
                val bitmapDrawable =
                    albumArt.scale(screenWidth, screenWidth)
                        .toDrawable(resources)
                    
                // Create the tint color drawable
                val tintDrawable = android.graphics.Color.argb(100, 0, 0, 0).toDrawable()
                
                // Create the gradient drawable
                val gradientDrawable = GradientDrawable()
                gradientDrawable.orientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                    GradientDrawable.Orientation.LEFT_RIGHT else GradientDrawable.Orientation.TOP_BOTTOM
                gradientDrawable.shape = GradientDrawable.RECTANGLE
                gradientDrawable.colors = intArrayOf(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.BLACK
                )
                gradientDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT
                
                // Combine gradient and tint
                val gradientOverlay = LayerDrawable(arrayOf(tintDrawable, gradientDrawable))
                
                // Combine both in a layer list
                val layers = LayerDrawable(arrayOf(bitmapDrawable, gradientOverlay))
                
                albumArtView.setImageDrawable(layers)
                albumArtView.visibility = View.VISIBLE
            }
        } else {
            // Hide the album art view if it exists
            findViewById<ImageView>(R.id.album_art_overlay)?.visibility = View.GONE
        }
    }

    companion object {
        private const val SMALL_DELAY: Long = 300
        private const val MILLISECONDS_PER_SECOND: Long = 1_000
        private const val SENSOR_DELAY_SLOW: Int = 1_000_000
        private const val MINIMUM_ANIMATION_DURATION: Int = 100
        private const val HALF: Float = 0.5f
        private var instance: AlwaysOn? = null

        fun finish() {
            instance?.finish()
        }
        
        fun getInstance(): AlwaysOn? {
            return instance
        }
    }
}
