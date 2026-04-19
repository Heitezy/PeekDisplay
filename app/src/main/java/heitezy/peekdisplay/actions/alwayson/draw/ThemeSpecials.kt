package heitezy.peekdisplay.actions.alwayson.draw

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.content.res.Configuration
import java.text.SimpleDateFormat
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.AlwaysOnCustomView
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.receivers.CombinedServiceReceiver.Companion.isCharging

object ThemeSpecials {
    internal fun drawDivider(
        canvas: Canvas,
        utils: Utils,
        flags: BooleanArray,
        tempHeight: Float,
    ) {
        if (flags[AlwaysOnCustomView.FLAG_SAMSUNG_3] && (
                utils.prefs.get(
                    P.SHOW_CLOCK,
                    P.SHOW_CLOCK_DEFAULT,
                ) || utils.prefs.get(P.SHOW_DATE, P.SHOW_DATE_DEFAULT)
            )
        ) {
            canvas.drawRect(
                utils.horizontalRelativePoint - utils.padding2 / 2,
                tempHeight + utils.padding16 * 2 + utils.topPadding,
                utils.horizontalRelativePoint + utils.padding2 / 2,
                utils.viewHeight - utils.padding16 + utils.topPadding,
                utils.getPaint(utils.bigTextSize, Color.WHITE),
            )
        }
    }

    internal fun drawBatteryCircle(
        canvas: Canvas,
        utils: Utils,
        flags: BooleanArray,
        width: Int,
        batteryPercent: Int,
        timeFormat: SimpleDateFormat,
        dateFormat: SimpleDateFormat,
    ) {
        if (flags[AlwaysOnCustomView.FLAG_MOTO] && (
                utils.prefs.get(
                    P.SHOW_BATTERY_ICON,
                    P.SHOW_BATTERY_ICON_DEFAULT
                ) || utils.prefs.get(P.SHOW_BATTERY_PERCENTAGE, P.SHOW_BATTERY_PERCENTAGE_DEFAULT)
            )
        ) {
            // M-Style: Draw circular progress, time, date, and battery percent in the center
            val centerX = width / 2f
            val isLandscape = utils.context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val defaultRadius = (if (isLandscape) 0.12f else 0.3f) * width
            val strokeWidth = utils.dpToPx(4f)

            val showClock = utils.prefs.get(P.SHOW_CLOCK, P.SHOW_CLOCK_DEFAULT)
            val showDate = utils.prefs.get(P.SHOW_DATE, P.SHOW_DATE_DEFAULT)
            val showBatteryPercentage = utils.prefs.get(P.SHOW_BATTERY_PERCENTAGE, P.SHOW_BATTERY_PERCENTAGE_DEFAULT)
            val showBatteryCircle = utils.prefs.get(P.SHOW_BATTERY_ICON, P.SHOW_BATTERY_ICON_DEFAULT)

            val timeHeight = if (showClock) utils.getTextHeight(utils.bigTextSize) else 0f
            val dateHeight = if (showDate) utils.getTextHeight(utils.smallTextSize) else 0f
            val batteryTextHeight = if (showBatteryPercentage) utils.getTextHeight(utils.mediumTextSize) else 0f

            // Compute minimal radius to fit enabled texts inside inner circle area
            val radius = if (!showClock && !showDate && !showBatteryPercentage) {
                defaultRadius
            } else {
                val needed = (timeHeight / 2f) + (strokeWidth / 2f) + maxOf(dateHeight, batteryTextHeight, 0f)
                maxOf(defaultRadius, needed)
            }

            var startAngle = -270f
            if (isCharging) {
                startAngle = -260f
            }
            var sweepAngle = 360f
            if (isCharging) {
                sweepAngle = 340f
            }
            val angle = sweepAngle * batteryPercent / 100f

            if (showBatteryCircle) {
                // Draw background circle
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = 0x33FFFFFF
                    this.strokeWidth = strokeWidth
                }
                val oval = android.graphics.RectF(
                    centerX - radius,
                    strokeWidth / 2 + utils.topPadding,
                    centerX + radius,
                    radius * 2 + strokeWidth / 2 + utils.topPadding
                )
                canvas.drawArc(oval, startAngle, sweepAngle, false, bgPaint)

                // Draw progress arc
                val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = 0xFF00E5FF.toInt()
                    this.strokeWidth = strokeWidth
                    strokeCap = Paint.Cap.ROUND
                }
                canvas.drawArc(oval, startAngle, angle, false, arcPaint)
            }

            val centerY = utils.topPadding + strokeWidth / 2f + radius

            // Draw time centered vertically inside the circle (if enabled)
            if (showClock) {
                val timePaint = utils.getPaint(
                    utils.bigTextSize,
                    utils.prefs.get(P.DISPLAY_COLOR_CLOCK, P.DISPLAY_COLOR_CLOCK_DEFAULT),
                )
                val timeBaseline = centerY - (timePaint.ascent() + timePaint.descent()) / 2f
                canvas.drawText(
                    timeFormat.format(System.currentTimeMillis()),
                    centerX,
                    timeBaseline,
                    timePaint,
                )
            }

            // Inner area bounds (exclude stroke width)
            val innerTop = utils.topPadding + strokeWidth
            val innerBottom = utils.topPadding + 2f * radius
            val halfTimeHeight = timeHeight / 2f

            // Draw date using smallTextSize, vertically centered in the upper band (if enabled)
            if (showDate) {
                val datePaint = utils.getPaint(
                    utils.smallTextSize,
                    utils.prefs.get(P.DISPLAY_COLOR_DATE, P.DISPLAY_COLOR_DATE_DEFAULT),
                )
                val upperBandCenter = (innerTop + (centerY - halfTimeHeight)) / 2f
                val dateBaseline = upperBandCenter - (datePaint.ascent() + datePaint.descent()) / 2f
                val dateText = dateFormat.format(System.currentTimeMillis())
                canvas.drawText(
                    dateText,
                    centerX,
                    dateBaseline + strokeWidth,
                    datePaint,
                )
            }

            // Draw battery percentage text, vertically centered in the lower band (if enabled)
            if (showBatteryPercentage) {
                val battPaint = utils.getPaint(
                    utils.mediumTextSize,
                    utils.prefs.get(P.DISPLAY_COLOR_BATTERY, P.DISPLAY_COLOR_BATTERY_DEFAULT),
                )
                val lowerBandCenter = ((centerY + halfTimeHeight) + innerBottom) / 2f
                val battBaseline = lowerBandCenter - (battPaint.ascent() + battPaint.descent()) / 2f
                val percentText = "$batteryPercent%"
                canvas.drawText(
                    percentText,
                    centerX,
                    battBaseline,
                    battPaint,
                )
            }

            // Draw lightning icon at the bottom (placeholder)
            if (isCharging) {
                utils.drawVector(canvas,
                    R.drawable.ic_charging_white,
                    (width / 2f).toInt(),
                    (radius * 2).toInt() + utils.topPadding,
                    0xFFFFFFFF.toInt(),
                )
            }

            // Advance viewHeight to prevent overlap
            utils.viewHeight = utils.topPadding + 2f * radius + strokeWidth + utils.padding16
        }
    }
}
