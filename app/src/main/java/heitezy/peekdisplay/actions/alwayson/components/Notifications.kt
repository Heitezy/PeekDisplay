package heitezy.peekdisplay.actions.alwayson.components

import android.annotation.SuppressLint
import android.app.Notification
import android.content.res.Configuration
import android.os.Build
import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.data.State
import heitezy.peekdisplay.actions.alwayson.styles.toAlignment

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Notifications(
    state: State,
    onPositioned: (Int, Rect) -> Unit
) {
    if (!state.showNotificationIcons || state.notifications.isEmpty()) return

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val density = LocalDensity.current

    val iconSize = when (state.notificationIconSize) {
        "small" -> 38.dp
        "enlarged" -> 60.dp
        else -> 52.dp
    }
    val iconPadding =
        if (!state.interactiveNotifications && !state.swipeNotificationOpen && !state.invertInteractionHighlight) {
            0.dp
        } else {
            4.dp
        }

    val notificationIcons = @Composable {
        state.notifications.forEach { entry ->
            val shouldHighlight = if (!state.invertInteractionHighlight) {
                if (state.isFingerprintTouched && state.swipeNotificationOpen) {
                    state.hoveredNotificationIndex == entry.detailedIndex
                } else {
                    state.touchedNotificationIndex == entry.detailedIndex
                }
            } else {
                if (state.isFingerprintTouched && state.swipeNotificationOpen) {
                    state.hoveredNotificationIndex != entry.detailedIndex
                } else {
                    state.touchedNotificationIndex != entry.detailedIndex
                }
            }

            Box(
                modifier = Modifier
                    .padding(iconPadding)
                    .size(iconSize)
                    .onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInRoot()
                        val size = coordinates.size
                        onPositioned(
                            entry.detailedIndex, Rect(
                                position.x,
                                position.y,
                                position.x + size.width,
                                position.y + size.height
                            )
                        )
                    }
                    .then(
                        if (shouldHighlight) {
                            Modifier.border(
                                2.dp, if (state.tintNotifications) Color(
                                    entry.color
                                )
                                else state.notificationColor, CircleShape
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val drawable = entry.icon.loadDrawable(context)
                if (drawable != null) {
                    Image(
                        bitmap = drawable.toBitmap(64, 64).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize * 0.6f),
                        colorFilter = if (state.tintNotifications) ColorFilter.tint(
                            Color(
                                entry.color
                            )
                        ) else ColorFilter.tint(state.notificationColor)
                    )
                }
            }
        }
    }

    Column(
        horizontalAlignment = state.textAlign.toAlignment(),
        verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.Top,
        modifier = Modifier
            .then(
            if (isLandscape) Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(
                    start = with(density) { state.notificationTopPadding.toDp() })
            else Modifier
                .padding(
                    top = with(density) { state.notificationTopPadding.toDp() })
        )
    ) {
        if (isLandscape) {
            FlowColumn(maxLines = 2) {
                notificationIcons()
            }
        } else {
            FlowRow(maxLines = 2) {
                notificationIcons()
            }
        }
    }
}

@SuppressLint("RememberInComposition")
@Composable
fun NotificationPreview(
    state: State,
    onActionClick: (Int, Int) -> Unit,
    onReplyActionClick: (Int, Int) -> Unit,
    onDismissNotification: (Int) -> Unit,
    onReplyTextChange: (TextFieldValue) -> Unit,
    onSendReply: (Int) -> Unit,
    onPositioned: (Rect) -> Unit,
    onActionPositioned: (Int, Rect) -> Unit
) {
    val index = state.touchedNotificationIndex ?: return
    if (index >= state.detailedNotifications.size) return

    val sbn = state.detailedNotifications[index]
    val notification = sbn.notification
    val extras = notification.extras

    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
    val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

    val context = LocalContext.current
    val appName = try {
        context.packageManager.getApplicationLabel(
            context.packageManager.getApplicationInfo(sbn.packageName, 0)
        ).toString()
    } catch (_: Exception) {
        sbn.packageName
    }

    val formattedTime = DateUtils.getRelativeTimeSpanString(
        sbn.postTime,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()

    fun actionHasReplyInput(action: Notification.Action): Boolean =
        action.remoteInputs?.any { ri ->
            ri.allowFreeFormInput
        } == true || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                action.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY)

    val focusRequester = FocusRequester()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.isReplyMode) {
        if (state.isReplyMode) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = state.notifications.getOrNull(index)?.icon?.loadDrawable(context)
                val color = state.notifications.getOrNull(index)?.color
                if (icon != null && color != null) {
                    Image(
                        bitmap = icon.toBitmap(64, 64).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(Color(color))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = appName,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Text(
                    text = " • $formattedTime",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                val largeIconExtra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(
                        Notification.EXTRA_LARGE_ICON,
                        android.os.Parcelable::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    extras.getParcelable(Notification.EXTRA_LARGE_ICON)
                }
                val largeIconBitmap = when (largeIconExtra) {
                    is android.graphics.drawable.Icon -> largeIconExtra.loadDrawable(context)
                        ?.toBitmap(96, 96)

                    is android.graphics.Bitmap -> largeIconExtra
                    else -> null
                }
                largeIconBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInRoot()
                        val size = coordinates.size
                        onPositioned(
                            Rect(
                                position.x, position.y,
                                position.x + size.width, position.y + size.height
                            )
                        )
                    }
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = body,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isReplyMode) {
                TextField(
                    value = state.replyText,
                    onValueChange = onReplyTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            stringResource(R.string.notification_action_reply) + "…",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = TextStyle(fontSize = 14.sp),
                    singleLine = false,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { onSendReply(index) }
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onSendReply(index) },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    notification.actions?.forEachIndexed { actionIndex, action ->
                        val isReply = actionHasReplyInput(action)
                        Button(
                            onClick = {
                                if (isReply) onReplyActionClick(index, actionIndex)
                                else onActionClick(index, actionIndex)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isReply) Color(0xFF4285F4) else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                val position = coordinates.positionInRoot()
                                val size = coordinates.size
                                onActionPositioned(
                                    actionIndex,
                                    Rect(
                                        position.x, position.y,
                                        position.x + size.width, position.y + size.height
                                    )
                                )
                            }
                        ) {
                            Text(
                                text = action.title.toString(),
                                color = if (isReply) Color.White else Color.Black,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = { onDismissNotification(index) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val position = coordinates.positionInRoot()
                            val size = coordinates.size
                            onActionPositioned(
                                -1,
                                Rect(
                                    position.x, position.y,
                                    position.x + size.width, position.y + size.height
                                )
                            )
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.notification_dismiss),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}