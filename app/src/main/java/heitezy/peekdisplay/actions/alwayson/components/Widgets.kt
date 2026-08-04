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
import heitezy.peekdisplay.actions.alwayson.data.BatteryState
import heitezy.peekdisplay.actions.alwayson.data.CalendarState
import heitezy.peekdisplay.actions.alwayson.data.ClockState
import heitezy.peekdisplay.actions.alwayson.data.MessageState
import heitezy.peekdisplay.actions.alwayson.data.MusicState
import heitezy.peekdisplay.actions.alwayson.data.NotificationState
import heitezy.peekdisplay.actions.alwayson.data.AppearanceState
import heitezy.peekdisplay.actions.alwayson.data.WeatherState
import heitezy.peekdisplay.actions.alwayson.styles.getThemeSettings
import heitezy.peekdisplay.actions.alwayson.styles.toAlignment
import heitezy.peekdisplay.helpers.P
import kotlinx.coroutines.delay

@Composable
fun Date(clockState: ClockState, appearanceState: AppearanceState, modifier: Modifier = Modifier) {
    if (!appearanceState.showDate) return
    val themeSettings = getThemeSettings(appearanceState.theme)
    val fontSize = if (appearanceState.isBigDate) themeSettings.bigTextSize else themeSettings.mediumTextSize
    val dateText = if (appearanceState.isCapsDate) clockState.date.uppercase() else clockState.date

    Text(
        text = dateText,
        color = appearanceState.dateColor,
        fontSize = fontSize,
        fontFamily = themeSettings.fontFamily,
        textAlign = appearanceState.textAlign,
        lineHeight = fontSize,
        modifier = modifier
    )
}

@Composable
fun Battery(batteryState: BatteryState, appearanceState: AppearanceState, modifier: Modifier = Modifier) {
    if (!appearanceState.showBatteryIcon && !appearanceState.showBatteryPercentage) return
    val themeSettings = getThemeSettings(appearanceState.theme)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = 16.dp, bottom = 16.dp)
    ) {
        if (appearanceState.theme == P.USER_THEME_ONEPLUS || appearanceState.isCapsDate || appearanceState.isBigDate ||
            appearanceState.isSamsung3 || appearanceState.isAnalog
        ) {
            if (appearanceState.showBatteryPercentage) {
                Text(
                    text = "${batteryState.level}%",
                    color = appearanceState.batteryColor,
                    fontSize = themeSettings.mediumTextSize,
                    fontFamily = themeSettings.fontFamily
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (appearanceState.showBatteryIcon) {
                Icon(
                    painter = painterResource(id = batteryState.iconRes),
                    contentDescription = null,
                    tint = if (batteryState.isCharging) colorResource(id = R.color.charging) else appearanceState.batteryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            if (appearanceState.showBatteryIcon) {
                Icon(
                    painter = painterResource(id = batteryState.iconRes),
                    contentDescription = null,
                    tint = if (batteryState.isCharging) colorResource(id = R.color.charging) else appearanceState.batteryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (appearanceState.showBatteryPercentage) {
                Text(
                    text = "${batteryState.level}%",
                    color = appearanceState.batteryColor,
                    fontSize = themeSettings.mediumTextSize,
                    fontFamily = themeSettings.fontFamily
                )
            }
        }
    }
}

@Composable
fun Music(musicState: MusicState, appearanceState: AppearanceState, onSkipPrevious: () -> Unit, onSkipNext: () -> Unit, onTitleClick: () -> Unit) {
    if (!appearanceState.showMusicControls || musicState.text.isEmpty() || (!musicState.isPlaying && !musicState.wasPlaying)) return
    
    val themeSettings = getThemeSettings(appearanceState.theme)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
    ) {
        IconButton(onClick = onSkipPrevious) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_previous_white),
                contentDescription = null,
                tint = appearanceState.musicColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Text(
            text = musicState.text,
            color = appearanceState.musicColor,
            fontSize = themeSettings.smallTextSize,
            fontFamily = themeSettings.fontFamily,
            textAlign = appearanceState.textAlign,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clickable { onTitleClick() }
                .weight(1f)
        )
        
        IconButton(onClick = onSkipNext) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_next_white),
                contentDescription = null,
                tint = appearanceState.musicColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun Weather(weatherState: WeatherState, appearanceState: AppearanceState) {
    if (!appearanceState.showWeather || weatherState.weather.isEmpty()) return
    
    val themeSettings = getThemeSettings(appearanceState.theme)
    
    Text(
        text = weatherState.weather,
        color = appearanceState.weatherColor,
        fontSize = themeSettings.smallTextSize,
        fontFamily = themeSettings.fontFamily,
        textAlign = appearanceState.textAlign,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
    )
}

@Composable
fun Message(messageState: MessageState, appearanceState: AppearanceState) {
    if (messageState.message.isEmpty()) return
    
    val themeSettings = getThemeSettings(appearanceState.theme)
    
    Text(
        text = messageState.message,
        color = appearanceState.messageColor,
        fontSize = themeSettings.smallTextSize,
        fontFamily = themeSettings.fontFamily,
        textAlign = appearanceState.textAlign,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
    )
}

@Composable
fun Calendar(calendarState: CalendarState, appearanceState: AppearanceState) {
    if (!appearanceState.showCalendar || calendarState.events.isEmpty()) return
    
    val themeSettings = getThemeSettings(appearanceState.theme)
    
    Column(horizontalAlignment = appearanceState.textAlign.toAlignment(), modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)) {
        calendarState.events.forEach { event ->
            Text(
                text = event,
                color = appearanceState.calendarColor,
                fontSize = themeSettings.smallTextSize,
                fontFamily = themeSettings.fontFamily,
                textAlign = appearanceState.textAlign
            )
        }
    }
}

@Composable
fun NotificationCount(notificationState: NotificationState, appearanceState: AppearanceState) {
    if (!appearanceState.showNotificationCount || notificationState.entries.isEmpty()) return
    
    val themeSettings = getThemeSettings(appearanceState.theme)
    
    Text(
        text = notificationState.entries.size.toString(),
        color = appearanceState.notificationColor,
        fontSize = themeSettings.mediumTextSize,
        fontFamily = themeSettings.fontFamily,
        textAlign = appearanceState.textAlign,
        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
    )
}

@Composable
fun EdgeGlow(appearanceState: AppearanceState, notificationState: NotificationState) {
    if (!appearanceState.edgeGlowEnabled || !notificationState.hasNew) return

    val alpha = remember { Animatable(0f) }

    LaunchedEffect(appearanceState.edgeGlowDuration, appearanceState.edgeGlowDelay) {
        while (true) {
            alpha.animateTo(0.5f, animationSpec = tween(appearanceState.edgeGlowDuration))
            alpha.animateTo(0f, animationSpec = tween(appearanceState.edgeGlowDuration))
            delay(appearanceState.edgeGlowDelay.toLong())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val strokeWidth = 4.dp.toPx()
                val color = appearanceState.edgeGlowColor.copy(alpha = alpha.value)

                when (appearanceState.edgeGlowStyle) {
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
