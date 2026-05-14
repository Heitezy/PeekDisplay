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
import heitezy.peekdisplay.actions.alwayson.data.State

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
        var state by remember {
            mutableStateOf(
                State(
                    time = "12:00",
                    date = "Monday, Jan 1",
                    batteryLevel = 100,
                    musicString = "Artist - Song"
                )
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Content(
                state = state,
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
                onNotificationHovered = { _ -> },
                onBoundsUpdated = { _, _, _, _ -> }
            )
        }
    }
}
