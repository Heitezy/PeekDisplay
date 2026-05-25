package heitezy.peekdisplay.actions.alwayson.data

import android.graphics.Bitmap
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import heitezy.peekdisplay.R
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.services.NotificationService

data class State(
    val time: String = "",
    val date: String = "",
    val batteryLevel: Int = 0,
    val batteryIsCharging: Boolean = false,
    val batteryIconRes: Int = R.drawable.ic_battery_unknown,
    val musicString: String = "",
    val albumArt: Bitmap? = null,
    val weather: String = "",
    val message: String = "",
    val calendarEvents: List<String> = emptyList(),
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
    val notifications: List<NotificationService.NotificationEntry> = emptyList(),
    val detailedNotifications: List<StatusBarNotification> = emptyList(),
    val touchedNotificationIndex: Int? = null,
    val hoveredNotificationIndex: Int? = null,
    val isReplyMode: Boolean = false,
    val replyText: TextFieldValue = TextFieldValue(""),
    val replyActionIndex: Int? = null,
    val isFingerprintTouched: Boolean = false,
    val showFingerprint: Boolean = false,
    val useLockIcon: Boolean = false,
    val fingerprintIconRes: Int = R.drawable.ic_fingerprint_white,
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
    val hasNewNotifications: Boolean = false,
    val backgroundImageRes: Int? = null,
    val customBackground: Bitmap? = null,
    val notificationIconSize: String = "standart",
    val notificationPreviewPosition: String = "above",
    val tintNotifications: Boolean = false,
    val interactiveNotifications: Boolean = true,
    val invertInteractionHighlight: Boolean = true,
    val swipeNotificationOpen: Boolean = false,
    val animateMotion: Boolean = false,
    val topPadding: Int = 0,
    val notificationTopPadding: Int = 680,
    val scale: Float = 1f,
    val driftY: Float = 0f,
    val fpDriftY: Float = 0f,
    val isInteracting: Boolean = false,
    val isDrifting: Boolean = false
)

data class ThemeSettings(
    val bigTextSize: TextUnit,
    val mediumTextSize: TextUnit,
    val smallTextSize: TextUnit,
    val fontFamily: FontFamily
)