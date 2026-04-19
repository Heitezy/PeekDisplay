package heitezy.peekdisplay.actions.alwayson.draw

import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.res.ResourcesCompat
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.AlwaysOnCustomView
import heitezy.peekdisplay.helpers.P

object Battery {
    private const val PERCENT = "%"

    internal fun drawIconAndPercentage(
        canvas: Canvas,
        utils: Utils,
        batteryIcon: Int,
        batteryLevel: Int,
        batteryIsCharging: Boolean,
        flags: BooleanArray,
        width: Int,
    ) {
        utils.drawVector(
            canvas,
            batteryIcon,
            (
                    utils.horizontalRelativePoint +
                            utils.getPaint(utils.mediumTextSize)
                                .measureText(batteryLevel.toString() + PERCENT).run {
                                if (utils.paint.textAlign == Paint.Align.LEFT) {
                                    this
                                } else {
                                    this / 2
                                }
                            }
                    ).toInt(),
            if (flags[AlwaysOnCustomView.FLAG_SAMSUNG_3]) {
                (
                        utils.viewHeight + utils.padding16 + utils.topPadding +
                                utils.getVerticalCenter(
                                    utils.getPaint(utils.mediumTextSize),
                                )
                        ).toInt()
            } else {
                (
                        utils.viewHeight + utils.padding16 +
                                utils.getVerticalCenter(
                                    utils.getPaint(utils.mediumTextSize),
                                )
                        ).toInt()
            },
            if (batteryIsCharging) {
                ResourcesCompat.getColor(utils.resources, R.color.charging, null)
            } else {
                utils.prefs.get(P.DISPLAY_COLOR_BATTERY, P.DISPLAY_COLOR_BATTERY_DEFAULT)
            },
        )
        utils.drawRelativeText(
            canvas,
            batteryLevel.toString() + PERCENT,
            if (flags[AlwaysOnCustomView.FLAG_SAMSUNG_3]) {
                utils.padding16 + utils.topPadding
            } else {
                utils.padding16
            },
            if (flags[AlwaysOnCustomView.FLAG_MOTO]) {
                (width * 0.13f).toInt()
            } else {
                utils.padding16
            },
            utils.getPaint(
                utils.mediumTextSize,
                utils.prefs.get(P.DISPLAY_COLOR_BATTERY, P.DISPLAY_COLOR_BATTERY_DEFAULT),
            ),
            if (utils.paint.textAlign == Paint.Align.LEFT) 0 else -utils.drawableSize / 2,
        )
    }

    internal fun drawIcon(
        canvas: Canvas,
        utils: Utils,
        batteryIcon: Int,
        batteryIsCharging: Boolean,
        flags: BooleanArray,
    ) {
        utils.drawVector(
            canvas,
            batteryIcon,
            utils.horizontalRelativePoint.toInt(),
            if (flags[AlwaysOnCustomView.FLAG_SAMSUNG_3]) {
                (
                        utils.viewHeight + utils.padding16 + utils.topPadding +
                                utils.getVerticalCenter(
                                    utils.getPaint(utils.mediumTextSize),
                                )
                        ).toInt()
            } else {
                (
                        utils.viewHeight + utils.padding16 +
                                utils.getVerticalCenter(
                                    utils.getPaint(utils.mediumTextSize),
                                )
                        ).toInt()
            },
            if (batteryIsCharging) {
                ResourcesCompat.getColor(utils.resources, R.color.charging, null)
            } else {
                utils.prefs.get(P.DISPLAY_COLOR_BATTERY, P.DISPLAY_COLOR_BATTERY_DEFAULT)
            },
        )
        utils.viewHeight += utils.padding16 + utils.getTextHeight() + utils.padding16
    }

    internal fun drawPercentage(
        canvas: Canvas,
        utils: Utils,
        batteryLevel: Int,
        flags: BooleanArray,
        width: Int,
    ) {
        utils.drawRelativeText(
            canvas,
            batteryLevel.toString() + PERCENT,
            if (flags[AlwaysOnCustomView.FLAG_SAMSUNG_3]) {
                utils.padding16 + utils.topPadding
            } else {
                utils.padding16
            },
            if (flags[AlwaysOnCustomView.FLAG_MOTO]) {
                (width * 0.13f).toInt()
            } else {
                utils.padding16
            },
            utils.getPaint(
                utils.mediumTextSize,
                utils.prefs.get(P.DISPLAY_COLOR_BATTERY, P.DISPLAY_COLOR_BATTERY_DEFAULT),
            ),
        )
    }
}
