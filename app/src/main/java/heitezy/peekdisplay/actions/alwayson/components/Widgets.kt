package heitezy.peekdisplay.actions.alwayson.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.data.State
import heitezy.peekdisplay.actions.alwayson.styles.getThemeSettings
import heitezy.peekdisplay.actions.alwayson.styles.toAlignment
import heitezy.peekdisplay.helpers.P
import kotlinx.coroutines.delay

@Composable
fun Date(state: State, modifier: Modifier = Modifier) {
    if (!state.showDate) return
    val themeSettings = getThemeSettings(state.theme)
    val fontSize = if (state.isBigDate) themeSettings.bigTextSize else themeSettings.mediumTextSize
    val dateText = if (state.isCapsDate) state.date.uppercase() else state.date

    Text(
        text = dateText,
        color = state.dateColor,
        fontSize = fontSize,
        fontFamily = themeSettings.fontFamily,
        textAlign = state.textAlign,
        lineHeight = fontSize,
        modifier = modifier
    )
}

@Composable
fun Battery(state: State, modifier: Modifier = Modifier) {
    if (!state.showBatteryIcon && !state.showBatteryPercentage) return
    val themeSettings = getThemeSettings(state.theme)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = 16.dp, bottom = 16.dp)
    ) {
        if (state.theme == P.USER_THEME_ONEPLUS || state.isCapsDate || state.isBigDate ||
            state.isSamsung3 || state.isAnalog
        ) {
            if (state.showBatteryPercentage) {
                Text(
                    text = "${state.batteryLevel}%",
                    color = state.batteryColor,
                    fontSize = themeSettings.mediumTextSize,
                    fontFamily = themeSettings.fontFamily
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (state.showBatteryIcon) {
                Icon(
                    painter = painterResource(id = state.batteryIconRes),
                    contentDescription = null,
                    tint = if (state.batteryIsCharging) colorResource(id = R.color.charging) else state.batteryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            if (state.showBatteryIcon) {
                Icon(
                    painter = painterResource(id = state.batteryIconRes),
                    contentDescription = null,
                    tint = if (state.batteryIsCharging) colorResource(id = R.color.charging) else state.batteryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (state.showBatteryPercentage) {
                Text(
                    text = "${state.batteryLevel}%",
                    color = state.batteryColor,
                    fontSize = themeSettings.mediumTextSize,
                    fontFamily = themeSettings.fontFamily
                )
            }
        }
    }
}

@Composable
fun Music(state: State, onSkipPrevious: () -> Unit, onSkipNext: () -> Unit, onTitleClick: () -> Unit) {
    if (!state.showMusicControls || state.musicString.isEmpty()) return
    
    val themeSettings = getThemeSettings(state.theme)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
    ) {
        IconButton(onClick = onSkipPrevious) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_previous_white),
                contentDescription = null,
                tint = state.musicColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Text(
            text = state.musicString,
            color = state.musicColor,
            fontSize = themeSettings.smallTextSize,
            fontFamily = themeSettings.fontFamily,
            textAlign = state.textAlign,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clickable { onTitleClick() }
                .weight(1f)
        )
        
        IconButton(onClick = onSkipNext) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_next_white),
                contentDescription = null,
                tint = state.musicColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun Weather(state: State) {
    if (!state.showWeather || state.weather.isEmpty()) return
    
    val themeSettings = getThemeSettings(state.theme)
    
    Text(
        text = state.weather,
        color = state.weatherColor,
        fontSize = themeSettings.smallTextSize,
        fontFamily = themeSettings.fontFamily,
        textAlign = state.textAlign,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
    )
}

@Composable
fun Message(state: State) {
    if (state.message.isEmpty()) return
    
    val themeSettings = getThemeSettings(state.theme)
    
    Text(
        text = state.message,
        color = state.messageColor,
        fontSize = themeSettings.smallTextSize,
        fontFamily = themeSettings.fontFamily,
        textAlign = state.textAlign,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
    )
}

@Composable
fun Calendar(state: State) {
    if (!state.showCalendar || state.calendarEvents.isEmpty()) return
    
    val themeSettings = getThemeSettings(state.theme)
    
    Column(horizontalAlignment = state.textAlign.toAlignment(), modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)) {
        state.calendarEvents.forEach { event ->
            Text(
                text = event,
                color = state.calendarColor,
                fontSize = themeSettings.smallTextSize,
                fontFamily = themeSettings.fontFamily,
                textAlign = state.textAlign
            )
        }
    }
}

@Composable
fun NotificationCount(state: State) {
    if (!state.showNotificationCount || state.notifications.isEmpty()) return
    
    val themeSettings = getThemeSettings(state.theme)
    
    Text(
        text = state.notifications.size.toString(),
        color = state.notificationColor,
        fontSize = themeSettings.mediumTextSize,
        fontFamily = themeSettings.fontFamily,
        textAlign = state.textAlign,
        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
    )
}

@Composable
fun EdgeGlow(state: State) {
    if (!state.edgeGlowEnabled || !state.hasNewNotifications) return

    val alpha = remember { Animatable(0f) }

    LaunchedEffect(state.edgeGlowDuration, state.edgeGlowDelay) {
        while (true) {
            alpha.animateTo(0.5f, animationSpec = tween(state.edgeGlowDuration))
            alpha.animateTo(0f, animationSpec = tween(state.edgeGlowDuration))
            delay(state.edgeGlowDelay.toLong())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val strokeWidth = 4.dp.toPx()
                val color = state.edgeGlowColor.copy(alpha = alpha.value)

                when (state.edgeGlowStyle) {
                    P.EDGE_GLOW_STYLE_VERTICAL -> {
                        drawLine(
                            color = color,
                            start = androidx.compose.ui.geometry.Offset(strokeWidth / 2, 0f),
                            end = androidx.compose.ui.geometry.Offset(strokeWidth / 2, size.height),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = color, start = androidx.compose.ui.geometry.Offset(
                                size.width - strokeWidth / 2, 0f
                            ), end = androidx.compose.ui.geometry.Offset(
                                size.width - strokeWidth / 2, size.height
                            ), strokeWidth = strokeWidth
                        )
                    }

                    P.EDGE_GLOW_STYLE_HORIZONTAL -> {
                        drawLine(
                            color = color,
                            start = androidx.compose.ui.geometry.Offset(0f, strokeWidth / 2),
                            end = androidx.compose.ui.geometry.Offset(size.width, strokeWidth / 2),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = color, start = androidx.compose.ui.geometry.Offset(
                                0f, size.height - strokeWidth / 2
                            ), end = androidx.compose.ui.geometry.Offset(
                                size.width, size.height - strokeWidth / 2
                            ), strokeWidth = strokeWidth
                        )
                    }

                    else -> {
                        drawRect(
                            color = color,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                strokeWidth / 2, strokeWidth / 2
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                size.width - strokeWidth, size.height - strokeWidth
                            ),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                    }
                }
            }
    )
}
