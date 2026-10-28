package heitezy.peekdisplay.actions.alwayson

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.data.AppearanceState
import heitezy.peekdisplay.actions.alwayson.data.BatteryState
import heitezy.peekdisplay.actions.alwayson.data.CalendarState
import heitezy.peekdisplay.actions.alwayson.data.ClockState
import heitezy.peekdisplay.actions.alwayson.data.Data
import heitezy.peekdisplay.actions.alwayson.data.FingerprintState
import heitezy.peekdisplay.actions.alwayson.data.InteractionState
import heitezy.peekdisplay.actions.alwayson.data.MessageState
import heitezy.peekdisplay.actions.alwayson.data.MusicState
import heitezy.peekdisplay.actions.alwayson.data.NotificationState
import heitezy.peekdisplay.actions.alwayson.data.WeatherState
import heitezy.peekdisplay.helpers.Global
import heitezy.peekdisplay.helpers.IconHelper
import heitezy.peekdisplay.helpers.P
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class AlwaysOnViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = P.getP(application)

    var clockState by mutableStateOf(ClockState())
        private set
    var batteryState by mutableStateOf(BatteryState())
        internal set
    var musicState by mutableStateOf(MusicState())
        internal set
    var weatherState by mutableStateOf(WeatherState())
        internal set
    var calendarState by mutableStateOf(CalendarState())
        private set
    var messageState by mutableStateOf(MessageState())
        private set
    var notificationState by mutableStateOf(NotificationState())
        internal set
    var fingerprintState by mutableStateOf(FingerprintState())
        internal set
    var appearanceState by mutableStateOf(AppearanceState())
        private set
    var interactionState by mutableStateOf(InteractionState())
        internal set

    var isProximate by mutableStateOf(false)
        internal set

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

    fun updateInitialAODState() {
        val theme = prefs.get(P.USER_THEME, P.USER_THEME_DEFAULT)
        val isMultiline = theme == P.USER_THEME_SAMSUNG || theme == P.USER_THEME_ONEPLUS || theme == P.USER_THEME_ANALOG
        
        val timeFormatString = if (isMultiline) {
            prefs.getMultiLineTimeFormat()
        } else {
            prefs.getSingleLineTimeFormat()
        }
        
        val timeFormat = getTimeFormat(timeFormatString)
        val dateFormat = getDateFormat(prefs.get(P.DATE_FORMAT, P.DATE_FORMAT_DEFAULT))
        
        val batteryIntent = getApplication<heitezy.peekdisplay.Application>().registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
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

        viewModelScope.launch {
            val events = withContext(Dispatchers.IO) {
                Data.getCalendar(getApplication(), prefs)
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
            fullScreenBackground = prefs.get(P.FULL_SCREEN_BACKGROUND, P.FULL_SCREEN_BACKGROUND_DEFAULT),
            notificationIconSize = prefs.get(P.NOTIFICATION_ICON_SIZE, P.NOTIFICATION_ICON_SIZE_DEFAULT),
            notificationPreviewPosition = prefs.get(P.NOTIFICATION_PREVIEW_POSITION, P.NOTIFICATION_PREVIEW_POSITION_DEFAULT),
            tintNotifications = prefs.get(P.TINT_NOTIFICATIONS, P.TINT_NOTIFICATIONS_DEFAULT),
            interactiveNotifications = prefs.get(P.INTERACTIVE_NOTIFICATION_ICONS, P.INTERACTIVE_NOTIFICATION_ICONS_DEFAULT),
            invertInteractionHighlight = prefs.get(P.INVERT_INTERACTION_HIGHLIGHT, P.INVERT_INTERACTION_HIGHLIGHT_DEFAULT),
            swipeNotificationOpen = prefs.get(P.SWIPE_NOTIFICATION_OPEN, P.SWIPE_NOTIFICATION_OPEN_DEFAULT),
            animateMotion = prefs.get(P.ANIMATE_MOTION, P.ANIMATE_MOTION_DEFAULT),
            blindsEffect = prefs.get(P.BLINDS_EFFECT, P.BLINDS_EFFECT_DEFAULT),
            blindsBackgroundOnly = prefs.get(P.BLINDS_BACKGROUND_ONLY, P.BLINDS_BACKGROUND_ONLY_DEFAULT),
            topPadding = prefs.get(P.TOP_PADDING, P.TOP_PADDING_DEFAULT),
            notificationTopPadding = prefs.get(P.NOTIFICATION_ICON_TOP_PADDING, P.NOTIFICATION_ICON_TOP_PADDING_DEFAULT),
            scale = prefs.displayScale()
        )

        fingerprintState = FingerprintState(
            iconRes = if (appearanceState.useLockIcon) R.drawable.ic_lock else R.drawable.ic_fingerprint_white
        )

        // Load custom background asynchronously
        viewModelScope.launch {
            val customBgPathOrEncoded = prefs.get(P.CUSTOM_BACKGROUND, "")
            val isFullScreen = prefs.get(P.FULL_SCREEN_BACKGROUND, P.FULL_SCREEN_BACKGROUND_DEFAULT)
            val reqDim = if (isFullScreen) 2340 else 1080

            val customBgBitmap = if (customBgPathOrEncoded.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    try {
                        val file = File(customBgPathOrEncoded)
                        if (file.exists() && file.isFile) {
                            Global.decodeSampledBitmapFromFile(customBgPathOrEncoded, 1080, reqDim)
                        } else {
                            val bytes = Base64.decode(customBgPathOrEncoded, Base64.DEFAULT)
                            Global.decodeSampledBitmapFromByteArray(bytes, 0, bytes.size, 1080, reqDim)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            } else null

            appearanceState = appearanceState.copy(customBackground = customBgBitmap)
        }
    }

    fun startAODUpdateLoop(onRefreshRateNeeded: () -> Unit) {
        viewModelScope.launch {
            while (true) {
                val theme = prefs.get(P.USER_THEME, P.USER_THEME_DEFAULT)
                val isMultiline = theme == P.USER_THEME_SAMSUNG || theme == P.USER_THEME_ONEPLUS || theme == P.USER_THEME_ANALOG
                
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

                onRefreshRateNeeded()

                val hasSeconds = timeFormatString.contains("s", ignoreCase = true)
                val delayMillis = if (hasSeconds) 1000L else 60000L - (now % 60000L)
                delay(delayMillis)
            }
        }
    }

    fun startDriftLoop(onRefreshRateNeeded: () -> Unit) {
        val burnInDelay = prefs.get("ao_animation_delay", 2) * 60000L
        viewModelScope.launch {
            while (true) {
                delay(burnInDelay)

                while (interactionState.isInteracting) {
                    delay(500)
                }

                interactionState = interactionState.copy(driftY = 32f, fpDriftY = 64f, isDrifting = true)
                onRefreshRateNeeded()
                delay(2000)
                interactionState = interactionState.copy(isDrifting = false)
                onRefreshRateNeeded()

                delay(burnInDelay)

                while (interactionState.isInteracting) {
                    delay(500)
                }

                interactionState = interactionState.copy(driftY = 0f, fpDriftY = 0f, isDrifting = true)
                onRefreshRateNeeded()
                delay(2000)
                interactionState = interactionState.copy(isDrifting = false)
                onRefreshRateNeeded()
            }
        }
    }

    fun fetchWeather() {
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
        stringRequest.tag = AlwaysOn.WEATHER_REQUEST_TAG
        heitezy.peekdisplay.Application.requestQueue.add(stringRequest)
        
        val interval = prefs.get(P.WEATHER_REFRESH_INTERVAL, P.WEATHER_REFRESH_INTERVAL_DEFAULT)
        if (interval > 0) {
            viewModelScope.launch {
                delay(interval * 60 * 1000L)
                fetchWeather()
            }
        }
    }

    fun isStatic(): Boolean {
        if (!prefs.get(P.REFRESH_RATE_OPTIMIZATION, P.REFRESH_RATE_OPTIMIZATION_DEFAULT)) return false

        val theme = appearanceState.theme
        val isMultiline = theme == P.USER_THEME_SAMSUNG || theme == P.USER_THEME_ONEPLUS || theme == P.USER_THEME_ANALOG
        val timeFormatString = if (isMultiline) {
            prefs.getMultiLineTimeFormat()
        } else {
            prefs.getSingleLineTimeFormat()
        }
        val hasSeconds = timeFormatString.contains("s", ignoreCase = true)

        val hasActiveAnimations = (appearanceState.edgeGlowEnabled && notificationState.hasNew) || interactionState.isDrifting || hasSeconds
        return !interactionState.isInteracting && !hasActiveAnimations
    }

    override fun onCleared() {
        super.onCleared()
        heitezy.peekdisplay.Application.requestQueue.cancelAll(AlwaysOn.WEATHER_REQUEST_TAG)
    }
}
