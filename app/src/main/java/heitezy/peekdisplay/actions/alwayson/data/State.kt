package heitezy.peekdisplay.actions.alwayson.data

import android.graphics.Bitmap
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import heitezy.peekdisplay.R
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.services.NotificationService
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ClockState(
    val time: String = "",
    val date: String = "",
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0
)

@Immutable
data class BatteryState(
    val level: Int = 0,
    val isCharging: Boolean = false,
    val iconRes: Int = R.drawable.ic_battery_unknown
)

@Immutable
data class MusicState(
    val text: String = "",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val wasPlaying: Boolean = false
)

@Immutable
data class WeatherState(
    val weather: String = ""
)

@Immutable
data class CalendarState(
    val events: ImmutableList<String> = persistentListOf()
)

@Immutable
data class MessageState(
    val message: String = ""
)

@Immutable
data class NotificationState(
    val entries: ImmutableList<NotificationService.NotificationEntry> = persistentListOf(),
    val detailed: ImmutableList<StatusBarNotification> = persistentListOf(),
    val touchedIndex: Int? = null,
    val hoveredIndex: Int? = null,
    val isReplyMode: Boolean = false,
    val replyText: TextFieldValue = TextFieldValue(""),
    val replyActionIndex: Int? = null,
    val hasNew: Boolean = false
)

@Immutable
data class FingerprintState(
    val isTouched: Boolean = false,
    val iconRes: Int = R.drawable.ic_fingerprint_white
)

@Immutable
data class InteractionState(
    val driftY: Float = 0f,
    val fpDriftY: Float = 0f,
    val isInteracting: Boolean = false,
    val isDrifting: Boolean = false
)

@Immutable
data class AppearanceState(
    val theme: String = P.USER_THEME_DEFAULT,
    val clockColor: Color = Color.White,
    val dateColor: Color = Color.White,
    val batteryColor: Color = Color.White,
    val batteryArcColor: Color = Color.Cyan,
    val musicColor: Color = Color.White,
    val messageColor: Color = Color.White,
    val calendarColor: Color = Color.White,
    val weatherColor: Color = Color.White,
    val notificationColor: Color = Color.White,
    val textAlign: TextAlign = TextAlign.Center,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showBatteryIcon: Boolean = true,
    val showBatteryPercentage: Boolean = true,
    val showMusicControls: Boolean = false,
    val showAlbumArt: Boolean = false,
    val showWeather: Boolean = false,
    val showCalendar: Boolean = false,
    val showNotificationCount: Boolean = false,
    val showNotificationIcons: Boolean = true,
    val isSamsung3: Boolean = false,
    val isAnalog: Boolean = false,
    val isMultiline: Boolean = false,
    val isBigDate: Boolean = false,
    val isCapsDate: Boolean = false,
    val showFingerprint: Boolean = false,
    val useLockIcon: Boolean = false,
    val fingerprintColor: Color = Color.White,
    val fingerprintMargin: Int = 200,
    val fingerprintInteractionMode: String = "swipe",
    val edgeGlowEnabled: Boolean = false,
    val edgeGlowColor: Color = Color.Transparent,
    val edgeGlowDuration: Int = 2000,
    val edgeGlowDelay: Int = 2000,
    val edgeGlowStyle: String = "all",
    val disableDoubleTap: Boolean = false,
    val doubleTapSpeed: Long = 300L,
    val backgroundImageRes: Int? = null,
    val customBackground: Bitmap? = null,
    val fullScreenBackground: Boolean = false,
    val notificationIconSize: String = "standart",
    val notificationPreviewPosition: String = "above",
    val tintNotifications: Boolean = false,
    val interactiveNotifications: Boolean = true,
    val invertInteractionHighlight: Boolean = true,
    val swipeNotificationOpen: Boolean = false,
    val animateMotion: Boolean = false,
    val blindsEffect: Boolean = false,
    val blindsBackgroundOnly: Boolean = false,
    val topPadding: Int = 0,
    val notificationTopPadding: Int = 680,
    val scale: Float = 1f
)

data class ThemeSettings(
    val bigTextSize: TextUnit,
    val mediumTextSize: TextUnit,
    val smallTextSize: TextUnit,
    val fontFamily: FontFamily
)
