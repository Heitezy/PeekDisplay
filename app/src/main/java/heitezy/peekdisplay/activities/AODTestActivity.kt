package heitezy.peekdisplay.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import heitezy.peekdisplay.actions.alwayson.Content
import heitezy.peekdisplay.actions.alwayson.data.BatteryState
import heitezy.peekdisplay.actions.alwayson.data.CalendarState
import heitezy.peekdisplay.actions.alwayson.data.ClockState
import heitezy.peekdisplay.actions.alwayson.data.FingerprintState
import heitezy.peekdisplay.actions.alwayson.data.InteractionState
import heitezy.peekdisplay.actions.alwayson.data.MessageState
import heitezy.peekdisplay.actions.alwayson.data.MusicState
import heitezy.peekdisplay.actions.alwayson.data.NotificationState
import heitezy.peekdisplay.actions.alwayson.data.AppearanceState
import heitezy.peekdisplay.actions.alwayson.data.WeatherState

class AODTestActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toast.makeText(this, "testing mode", Toast.LENGTH_SHORT).show()

        setContent {
            BaseContent {
                AODTestScreen()
            }
        }
    }

    @Composable
    private fun AODTestScreen() {
        var clockState by remember { mutableStateOf(ClockState(time = "12:00", date = "Monday, Jan 1")) }
        var batteryState by remember { mutableStateOf(BatteryState(level = 100)) }
        var musicState by remember { mutableStateOf(MusicState(text = "Artist - Song")) }
        var weatherState by remember { mutableStateOf(WeatherState()) }
        var calendarState by remember { mutableStateOf(CalendarState()) }
        var messageState by remember { mutableStateOf(MessageState()) }
        var notificationState by remember { mutableStateOf(NotificationState()) }
        var fingerprintState by remember { mutableStateOf(FingerprintState()) }
        var appearanceState by remember { mutableStateOf(AppearanceState()) }
        var interactionState by remember { mutableStateOf(InteractionState()) }

        Box(modifier = Modifier.fillMaxSize()) {
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
                interactionState = interactionState,
                onSkipPrevious = { Toast.makeText(this@AODTestActivity, "left", Toast.LENGTH_SHORT).show() },
                onSkipNext = { Toast.makeText(this@AODTestActivity, "right", Toast.LENGTH_SHORT).show() },
                onTitleClick = { Toast.makeText(this@AODTestActivity, "center", Toast.LENGTH_SHORT).show() },
                onNotificationHoldStarted = { },
                onNotificationHoldFinished = { },
                onActionClick = { _, _ -> },
                onReplyActionClick = { _, _ -> },
                onDismissNotification = { },
                onReplyTextChange = { },
                onSendReply = { },
                onDoubleTap = { },
                onDown = { },
                onFingerprintTouch = { _, _, _ -> },
                onFingerprintLongPress = { },
                onOpenNotification = { },
                onNotificationHovered = { _ -> }
            )
        }
    }
}
