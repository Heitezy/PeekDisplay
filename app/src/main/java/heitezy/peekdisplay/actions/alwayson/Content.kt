package heitezy.peekdisplay.actions.alwayson

import android.annotation.SuppressLint
import android.app.Notification
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.components.Battery
import heitezy.peekdisplay.actions.alwayson.components.Calendar
import heitezy.peekdisplay.actions.alwayson.components.Clock
import heitezy.peekdisplay.actions.alwayson.components.Date
import heitezy.peekdisplay.actions.alwayson.components.EdgeGlow
import heitezy.peekdisplay.actions.alwayson.components.Fingerprint
import heitezy.peekdisplay.actions.alwayson.components.Message
import heitezy.peekdisplay.actions.alwayson.components.Music
import heitezy.peekdisplay.actions.alwayson.components.NotificationCount
import heitezy.peekdisplay.actions.alwayson.components.NotificationPreview
import heitezy.peekdisplay.actions.alwayson.components.Notifications
import heitezy.peekdisplay.actions.alwayson.components.Weather
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
import heitezy.peekdisplay.actions.alwayson.styles.getThemeSettings
import heitezy.peekdisplay.actions.alwayson.styles.toAlignment
import heitezy.peekdisplay.helpers.P

@SuppressLint("RememberReturnType")
@Composable
fun Content(
    clockState: ClockState,
    batteryState: BatteryState,
    musicState: MusicState,
    weatherState: WeatherState,
    calendarState: CalendarState,
    messageState: MessageState,
    notificationState: NotificationState,
    fingerprintState: FingerprintState,
    appearanceState: AppearanceState,
    interactionState: InteractionState,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onTitleClick: () -> Unit,
    onNotificationHoldStarted: (Int) -> Unit,
    onNotificationHoldFinished: () -> Unit,
    onActionClick: (Int, Int) -> Unit,
    onReplyActionClick: (Int, Int) -> Unit,
    onDismissNotification: (Int) -> Unit,
    onReplyTextChange: (TextFieldValue) -> Unit,
    onSendReply: (Int) -> Unit,
    onDoubleTap: () -> Unit,
    onDown: () -> Unit,
    onFingerprintTouch: (Boolean, Float, Float) -> Unit,
    onFingerprintLongPress: () -> Unit,
    onOpenNotification: (Int) -> Unit,
    onNotificationHovered: (Int?) -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val iconBoundsMap = remember { SnapshotStateMap<Int, Rect>() }
    val actionBoundsMap = remember { SnapshotStateMap<Int, Rect>() }
    val fpBoundsRef = remember { mutableStateOf<Rect?>(null) }
    val previewBoundsRef = remember { mutableStateOf<Rect?>(null) }

    val currentInteractive = rememberUpdatedState(appearanceState.interactiveNotifications)
    val currentIsFpTouched = rememberUpdatedState(fingerprintState.isTouched)
    val currentTouchedIndex = rememberUpdatedState(notificationState.touchedIndex)
    val currentDetailedNotifications = rememberUpdatedState(notificationState.detailed)

    remember(notificationState.entries) {
        iconBoundsMap.clear()
        actionBoundsMap.clear()
    }

    val currentViewConfiguration = LocalViewConfiguration.current
    val customViewConfiguration = remember(appearanceState.doubleTapSpeed) {
        object : ViewConfiguration by currentViewConfiguration {
            override val doubleTapTimeoutMillis: Long
                get() = appearanceState.doubleTapSpeed
        }
    }

    CompositionLocalProvider(LocalViewConfiguration provides customViewConfiguration) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(appearanceState.doubleTapSpeed) {
                    awaitPointerEventScope {
                        var currentActionIndex: Int? = null
                        var isOverPreview = false
                        var previewActive = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.first().position

                            when (event.type) {
                                PointerEventType.Press -> {
                                    currentActionIndex = null
                                    isOverPreview = false
                                    previewActive = false
                                    onNotificationHovered(null)

                                    if (currentInteractive.value) {
                                        iconBoundsMap.forEach { (index, bounds) ->
                                            if (bounds.contains(position)) {
                                                previewActive = true
                                                actionBoundsMap.clear()
                                                onNotificationHoldStarted(index)
                                            }
                                        }
                                    }
                                }

                                PointerEventType.Move -> {
                                    if (previewActive) {
                                        var foundIcon = false
                                        iconBoundsMap.forEach { (index, bounds) ->
                                            if (bounds.contains(position)) {
                                                foundIcon = true
                                                if (currentTouchedIndex.value != index) {
                                                    currentActionIndex = null
                                                    isOverPreview = false
                                                    actionBoundsMap.clear()
                                                    onNotificationHoldStarted(index)
                                                }
                                            }
                                        }

                                        if (!foundIcon) {
                                            var foundAction = false
                                            actionBoundsMap.forEach { (actionIndex, bounds) ->
                                                if (bounds.contains(position)) {
                                                    currentActionIndex = actionIndex
                                                    isOverPreview = false
                                                    foundAction = true
                                                }
                                            }

                                            if (!foundAction) {
                                                isOverPreview =
                                                    previewBoundsRef.value?.contains(position) == true
                                                currentActionIndex = null
                                            }
                                        }
                                    }

                                    if (currentIsFpTouched.value && appearanceState.swipeNotificationOpen) {
                                        var hoveredIndex: Int? = null
                                        iconBoundsMap.forEach { (index, bounds) ->
                                            if (bounds.contains(position)) {
                                                hoveredIndex = index
                                            }
                                        }
                                        onNotificationHovered(hoveredIndex)
                                    }
                                }

                                PointerEventType.Release -> {
                                    val touchedIndex = currentTouchedIndex.value
                                    when {
                                        previewActive && touchedIndex != null -> {
                                            if (currentActionIndex != null) {
                                                if (currentActionIndex == -1) {
                                                    onDismissNotification(touchedIndex)
                                                    onNotificationHoldFinished()
                                                } else {
                                                    val sbn =
                                                        currentDetailedNotifications.value.getOrNull(
                                                            touchedIndex
                                                        )
                                                    val action = sbn?.notification?.actions?.getOrNull(
                                                        currentActionIndex
                                                    )

                                                    val isReply =
                                                        action?.remoteInputs?.any { it.allowFreeFormInput } == true ||
                                                                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                                                                        action?.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY)

                                                    if (isReply) {
                                                        onReplyActionClick(
                                                            touchedIndex,
                                                            currentActionIndex
                                                        )
                                                    } else {
                                                        onActionClick(touchedIndex, currentActionIndex)
                                                        onNotificationHoldFinished()
                                                    }
                                                }
                                            } else {
                                                if (isOverPreview) {
                                                    onOpenNotification(touchedIndex)
                                                }
                                                onNotificationHoldFinished()
                                            }
                                        }

                                        currentIsFpTouched.value -> {
                                            iconBoundsMap.forEach { (index, bounds) ->
                                                if (appearanceState.swipeNotificationOpen) {
                                                    if (bounds.contains(position)) {
                                                        onOpenNotification(index)
                                                    }
                                                }
                                            }
                                            onFingerprintTouch(false, 0f, 0f)
                                        }

                                        else -> {
                                            if (currentTouchedIndex.value != null) {
                                                onNotificationHoldFinished()
                                            }
                                        }
                                    }

                                    currentActionIndex = null
                                    isOverPreview = false
                                    previewActive = false
                                    onNotificationHovered(null)
                                }
                            }
                        }
                    }
                }
        ) {
            if ((musicState.albumArt == null || !appearanceState.showAlbumArt || (!musicState.isPlaying && !musicState.wasPlaying)) && appearanceState.backgroundImageRes != null) {
                Box(
                    modifier = Modifier
                        .then(
                            if (appearanceState.fullScreenBackground) Modifier.fillMaxSize()
                            else if (isLandscape) Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f)
                                .align(Alignment.CenterStart)
                                .padding(start = 16.dp, end = 16.dp)
                            else Modifier
                                .fillMaxSize()
                                .padding(top = appearanceState.topPadding.dp, start = 16.dp, end = 16.dp)
                        ),
                    contentAlignment = if (isLandscape && !appearanceState.fullScreenBackground) Alignment.Center else Alignment.TopCenter
                ) {
                    if (appearanceState.backgroundImageRes == -1) {
                        appearanceState.customBackground?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .then(if (appearanceState.fullScreenBackground) Modifier.fillMaxSize() else Modifier)
                                    .alpha(0.5f),
                                contentScale = if (appearanceState.fullScreenBackground) ContentScale.Crop else ContentScale.None
                            )
                        }
                    } else {
                        Image(
                            painter = painterResource(id = appearanceState.backgroundImageRes),
                            contentDescription = null,
                            modifier = Modifier
                                .then(if (appearanceState.fullScreenBackground) Modifier.fillMaxSize() else Modifier)
                                .alpha(0.5f),
                            contentScale = if (appearanceState.fullScreenBackground) ContentScale.Crop else ContentScale.None
                        )
                    }
                }
            }

            EdgeGlow(appearanceState, notificationState)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, interactionState.driftY.dp.roundToPx()) }) {
                if (appearanceState.showAlbumArt && (musicState.isPlaying || musicState.wasPlaying)) {
                    musicState.albumArt?.let { bitmap ->
                        Box(
                            modifier = Modifier
                                .then(
                                    if (isLandscape) Modifier
                                        .fillMaxHeight()
                                        .aspectRatio(1f)
                                        .align(Alignment.CenterStart)
                                    else Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .align(Alignment.TopCenter)
                                )
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.6f),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isLandscape) {
                                            Brush.horizontalGradient(
                                                colors = listOf(Color.Transparent, Color.Black),
                                                startX = 0f,
                                                endX = Float.POSITIVE_INFINITY
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black),
                                                startY = 0f,
                                                endY = Float.POSITIVE_INFINITY
                                            )
                                        }
                                    )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .then(
                            if (isLandscape) Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f)
                                .align(Alignment.CenterStart)
                            else Modifier.fillMaxSize()
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = appearanceState.topPadding.dp, start = 16.dp, end = 16.dp)
                            .scale(appearanceState.scale)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { onDoubleTap() },
                                    onPress = { onDown() }
                                )
                            },
                        horizontalAlignment = if (appearanceState.theme == P.USER_THEME_SAMSUNG2) Alignment.Start
                        else if (isLandscape) appearanceState.textAlign.toAlignment()
                        else appearanceState.textAlign.toAlignment(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        val themeSettings = getThemeSettings(appearanceState.theme)

                        if (appearanceState.theme == P.USER_THEME_MOTO) {
                            MStyle(clockState, batteryState, appearanceState)
                        } else if (appearanceState.isSamsung3) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Clock(clockState, appearanceState)
                                if (appearanceState.showClock || appearanceState.showDate) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .width(2.dp)
                                            .height(with(density) { themeSettings.bigTextSize.toDp() * 1.5f })
                                            .background(Color.White)
                                    )
                                }
                                Date(clockState, appearanceState)
                            }
                        } else {
                            Clock(clockState, appearanceState)
                            Date(clockState, appearanceState)
                        }

                        if (appearanceState.theme != P.USER_THEME_MOTO) {
                            Battery(batteryState, appearanceState)
                        }

                        Music(musicState, appearanceState, onSkipPrevious, onSkipNext, onTitleClick)
                        Calendar(calendarState, appearanceState)
                        Message(messageState, appearanceState)
                        Weather(weatherState, appearanceState)
                        NotificationCount(notificationState, appearanceState)

                        if (!isLandscape) {
                            Notifications(
                                notificationState = notificationState,
                                appearanceState = appearanceState,
                                fingerprintState = fingerprintState,
                                onPositioned = { index, bounds ->
                                    iconBoundsMap[index] = bounds
                                }
                            )
                        }
                    }
                }
            }

            if (isLandscape) {
                Notifications(
                    notificationState = notificationState,
                    appearanceState = appearanceState,
                    fingerprintState = fingerprintState,
                    onPositioned = { index, bounds ->
                        iconBoundsMap[index] = bounds
                    }
                )
            }

            Fingerprint(
                fingerprintState = fingerprintState,
                appearanceState = appearanceState,
                interactionState = interactionState,
                onTouchStateChanged = onFingerprintTouch,
                onLongPress = onFingerprintLongPress,
                onPositioned = { bounds ->
                    fpBoundsRef.value = bounds
                }
            )

            val iconBounds = notificationState.touchedIndex?.let { iconBoundsMap[it] }
            if (notificationState.touchedIndex != null && iconBounds != null) {
                val above = appearanceState.notificationPreviewPosition == "above"

                SubcomposeLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(10f)
                ) { constraints ->
                    val sidePaddingPx = with(density) { 16.dp.roundToPx() }
                    val gapPx = with(density) { 8.dp.roundToPx() }

                    var previewMaxWidth = if (isLandscape) (constraints.maxWidth / 3)
                    else (constraints.maxWidth * 0.9f).toInt()

                    if (isLandscape) {
                        val availableWidth = if (above) {
                            (iconBounds.left - sidePaddingPx - gapPx).toInt()
                        } else {
                            (constraints.maxWidth - iconBounds.right - sidePaddingPx - gapPx).toInt()
                        }
                        previewMaxWidth = previewMaxWidth.coerceAtMost(availableWidth.coerceAtLeast(0))
                    }

                    val previewConstraints = Constraints(
                        minWidth = 0,
                        maxWidth = previewMaxWidth,
                        minHeight = 0,
                        maxHeight = constraints.maxHeight
                    )

                    val previewPlaceable = subcompose("preview") {
                        NotificationPreview(
                            notificationState = notificationState,
                            onActionClick = onActionClick,
                            onReplyActionClick = onReplyActionClick,
                            onDismissNotification = onDismissNotification,
                            onReplyTextChange = onReplyTextChange,
                            onSendReply = onSendReply,
                            onPositioned = { bounds ->
                                previewBoundsRef.value = bounds
                            },
                            onActionPositioned = { actionIndex, bounds ->
                                actionBoundsMap[actionIndex] = bounds
                            }
                        )
                    }.first().measure(previewConstraints)

                    val previewW = previewPlaceable.width
                    val previewH = previewPlaceable.height

                    val previewX: Int
                    val previewY: Int

                    if (isLandscape) {
                        val iconCenterY = ((iconBounds.top + iconBounds.bottom) / 2f).toInt()
                        val maxY = (constraints.maxHeight - previewH - sidePaddingPx).coerceAtLeast(
                            sidePaddingPx
                        )
                        previewY = (iconCenterY - previewH / 2).coerceIn(sidePaddingPx, maxY)

                        if (above) {
                            previewX = (iconBounds.left - previewW - gapPx).toInt()
                                .coerceAtLeast(sidePaddingPx)
                        } else {
                            previewX = (iconBounds.right + gapPx).toInt()
                                .coerceAtMost(constraints.maxWidth - previewW - sidePaddingPx)
                        }
                    } else {
                        previewY = if (above) {
                            (iconBounds.top - previewH - gapPx).toInt()
                                .coerceAtLeast(sidePaddingPx)
                        } else {
                            (iconBounds.bottom + gapPx).toInt()
                                .coerceAtMost(constraints.maxHeight - previewH - sidePaddingPx)
                        }

                        previewX = (constraints.maxWidth - previewW) / 2
                    }

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        previewPlaceable.placeRelative(previewX, previewY)
                    }
                }
            }
        }
    }
}

@Composable
private fun MStyle(
    clockState: ClockState,
    batteryState: BatteryState,
    appearanceState: AppearanceState
) {

    val themeSettings = getThemeSettings(appearanceState.theme)
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val strokeWidthDp = 4.dp
    val strokeWidthPx = with(density) { strokeWidthDp.toPx() }
    val safetyPaddingPx = with(density) { 12.dp.toPx() }
    val smallGapPx = with(density) { 4.dp.toPx() }
    val tinyMarginPx = with(density) { 2.dp.toPx() }
    val stepPx = with(density) { 1.dp.toPx() }
    val clampGuardPx = with(density) { 8.dp.toPx() }
    val defaultRadius = (if (isLandscape) 0.12f else 0.3f) * screenWidthPx

    val showClock = appearanceState.showClock
    val showDate = appearanceState.showDate
    val showBatteryPercentage = appearanceState.showBatteryPercentage
    val showBatteryCircle = appearanceState.showBatteryIcon

    fun makeTextStyle(fontSize: TextUnit) = TextStyle(
        fontSize = fontSize,
        fontFamily = themeSettings.fontFamily,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeight = fontSize,
    )

    val timeTextStyle = makeTextStyle(themeSettings.bigTextSize)
    val dateTextStyle = makeTextStyle(themeSettings.smallTextSize)
    val batteryTextStyle = makeTextStyle(themeSettings.mediumTextSize)

    val displayedDate = if (appearanceState.isCapsDate) clockState.date.uppercase() else clockState.date
    val batteryLabel = "${batteryState.level}%"

    val timeContent: @Composable () -> Unit = {
        Text(
            clockState.time,
            color = appearanceState.clockColor,
            style = timeTextStyle,
            textAlign = TextAlign.Center
        )
    }
    val dateContent: @Composable () -> Unit = {
        Text(
            displayedDate,
            color = appearanceState.dateColor,
            style = dateTextStyle,
            textAlign = TextAlign.Center
        )
    }
    val battContent: @Composable () -> Unit = {
        Text(
            batteryLabel,
            color = appearanceState.batteryColor,
            style = batteryTextStyle,
            textAlign = TextAlign.Center
        )
    }

    val startAngle = if (batteryState.isCharging) -260f else -270f
    val sweepAngle = if (batteryState.isCharging) 340f else 360f
    val progressAngle = sweepAngle * batteryState.level / 100f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {

        SubcomposeLayout { constraints ->

            val freeConstraints = Constraints()

            val timePlaceable = if (showClock)
                subcompose(Slot.TIME, timeContent).first().measure(freeConstraints) else null
            val datePlaceable = if (showDate)
                subcompose(Slot.DATE, dateContent).first().measure(freeConstraints) else null
            val battPlaceable = if (showBatteryPercentage)
                subcompose(Slot.BATT, battContent).first().measure(freeConstraints) else null

            val timeW = timePlaceable?.width?.toFloat() ?: 0f
            val timeH = timePlaceable?.height?.toFloat() ?: 0f
            val dateW = datePlaceable?.width?.toFloat() ?: 0f
            val dateH = datePlaceable?.height?.toFloat() ?: 0f
            val battW = battPlaceable?.width?.toFloat() ?: 0f
            val battH = battPlaceable?.height?.toFloat() ?: 0f

            val dateIsLongerThanClock = dateW > timeW

            val centerPlaceable: Placeable?
            val centerW: Float
            val centerH: Float

            val topPlaceable: Placeable?
            val topW: Float
            val topH: Float

            if (dateIsLongerThanClock) {
                centerPlaceable = datePlaceable; centerW = dateW; centerH = dateH
                topPlaceable = timePlaceable; topW = timeW; topH = timeH
            } else {
                centerPlaceable = timePlaceable; centerW = timeW; centerH = timeH
                topPlaceable = datePlaceable; topW = dateW; topH = dateH
            }

            val calculatedRadius: Float = if (!showClock && !showDate && !showBatteryPercentage) {
                defaultRadius
            } else {
                val sideH = maxOf(topH, battH)
                val neededForHeight =
                    (centerH / 2f) + sideH + (strokeWidthPx * 6f) + safetyPaddingPx
                val widestHalfW = maxOf(timeW, dateW, battW) / 2f
                val neededForWidth =
                    (widestHalfW * 1.1f) + (strokeWidthPx * 4f) + safetyPaddingPx
                maxOf(defaultRadius, neededForHeight, neededForWidth)
            }

            val maxAllowedRadius = (if (isLandscape) screenHeightPx else screenWidthPx) / 2f - 10.dp.toPx()
            val radiusPx = calculatedRadius.coerceAtMost(maxAllowedRadius)
            val shrinkRatio = if (calculatedRadius > maxAllowedRadius) maxAllowedRadius / calculatedRadius else 1f

            val diameter = (radiusPx * 2 + strokeWidthPx).toInt()

            val innerRadiusPx = radiusPx - strokeWidthPx
            val centerTextTop = -centerH / 2f
            val centerTextBottom = centerH / 2f

            var topY = -(innerRadiusPx / 2f + centerH / 4f) * shrinkRatio
            var bottomY = (innerRadiusPx / 2f + centerH / 4f) * shrinkRatio

            val topVisible = topPlaceable != null
            val centerShow = centerPlaceable != null
            val bottomVisible = battPlaceable != null

            if (topVisible && centerShow) {
                if (topY + topH / 2f > centerTextTop) {
                    topY = centerTextTop - topH / 2f - smallGapPx
                }
            }

            if (bottomVisible && centerShow) {
                if (bottomY - battH / 2f < centerTextBottom) {
                    bottomY = centerTextBottom + battH / 2f + smallGapPx
                }
            }

            val checkRadius = radiusPx - strokeWidthPx - tinyMarginPx

            fun isOutsideCircle(y: Float, halfW: Float, halfH: Float): Boolean {
                val dy1 = y - halfH
                val dy2 = y + halfH
                return kotlin.math.sqrt(halfW * halfW + dy1 * dy1) > checkRadius ||
                        kotlin.math.sqrt(halfW * halfW + dy2 * dy2) > checkRadius
            }

            if (topVisible) {
                val halfW = topW / 2f
                val halfH = topH / 2f
                if (isOutsideCircle(topY, halfW, halfH)) {
                    while (isOutsideCircle(topY, halfW, halfH) &&
                        topY + halfH < centerTextTop - clampGuardPx
                    ) {
                        topY += stepPx
                    }
                }
            }

            if (bottomVisible) {
                val halfW = battW / 2f
                val halfH = battH / 2f
                if (isOutsideCircle(bottomY, halfW, halfH)) {
                    while (isOutsideCircle(bottomY, halfW, halfH) &&
                        bottomY - halfH > centerTextBottom + smallGapPx
                    ) {
                        bottomY -= stepPx
                    }
                }
            }

            val ringSize = (radiusPx * 2).toInt()
            val ringConstraints = Constraints.fixed(ringSize, ringSize)

            val ringPlaceable = if (showBatteryCircle) {
                subcompose(Slot.RING) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.2f),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                        )
                        drawArc(
                            color = appearanceState.batteryArcColor,
                            startAngle = startAngle,
                            sweepAngle = progressAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                        )
                    }
                }.first().measure(ringConstraints)
            } else null

            val chargingPlaceable = if (batteryState.isCharging) {
                subcompose(Slot.CHARGING) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_charging_white),
                        contentDescription = "Charging Icon",
                        tint = Color.White
                    )
                }.first().measure(freeConstraints)
            } else null

            val cx = diameter / 2
            val cy = diameter / 2

            layout(diameter, diameter) {

                ringPlaceable?.placeRelative(
                    x = cx - ringSize / 2,
                    y = cy - ringSize / 2,
                )

                topPlaceable?.placeRelative(
                    x = cx - topPlaceable.width / 2,
                    y = (cy + topY - topH / 2f).toInt(),
                )

                centerPlaceable?.placeRelative(
                    x = cx - centerPlaceable.width / 2,
                    y = (cy - centerH / 2f).toInt(),
                )

                battPlaceable?.placeRelative(
                    x = cx - battPlaceable.width / 2,
                    y = (cy + bottomY - battH / 2f).toInt(),
                )

                chargingPlaceable?.placeRelative(
                    x = cx - chargingPlaceable.width / 2,
                    y = cy + ringSize / 2 - chargingPlaceable.height / 2,
                )
            }
        }
    }
}

enum class Slot { TIME, DATE, BATT, RING, CHARGING }
