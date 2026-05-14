package heitezy.peekdisplay.actions.alwayson.styles

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import heitezy.peekdisplay.R
import heitezy.peekdisplay.actions.alwayson.data.ThemeSettings
import heitezy.peekdisplay.helpers.P

fun TextAlign.toAlignment(): Alignment.Horizontal {
    return when (this) {
        TextAlign.Left -> Alignment.Start
        TextAlign.Right -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
}

private val themeSettingsCache = mutableMapOf<String, ThemeSettings>()

fun getThemeSettings(theme: String): ThemeSettings {
    return themeSettingsCache.getOrPut(theme) {
        when (theme) {
            P.USER_THEME_MOTO -> ThemeSettings(
                75.sp, 25.sp, 18.sp, FontFamily(Font(R.font.roboto_light))
            )
            P.USER_THEME_ONEPLUS -> ThemeSettings(
                75.sp, 20.sp, 15.sp, FontFamily(Font(R.font.roboto_medium))
            )
            P.USER_THEME_SAMSUNG2 -> ThemeSettings(
                36.sp, 18.sp, 16.sp, FontFamily(Font(R.font.roboto_light))
            )
            P.USER_THEME_GOOGLE -> ThemeSettings(
                75.sp, 25.sp, 18.sp, FontFamily(Font(R.font.roboto_regular))
            )
            P.USER_THEME_SAMSUNG -> ThemeSettings(
                75.sp, 25.sp, 18.sp, FontFamily(Font(R.font.roboto_light))
            )
            P.USER_THEME_SAMSUNG3 -> ThemeSettings(
                75.sp, 25.sp, 18.sp, FontFamily(Font(R.font.roboto_regular))
            )
            P.USER_THEME_80S -> ThemeSettings(
                75.sp, 25.sp, 18.sp, FontFamily(Font(R.font.monoton_regular))
            )
            P.USER_THEME_FAST -> ThemeSettings(
                75.sp, 25.sp, 18.sp, FontFamily(Font(R.font.faster_one_regular))
            )
            P.USER_THEME_FLOWER -> ThemeSettings(
                75.sp, 25.sp, 18.sp, FontFamily(Font(R.font.akronim_regular))
            )
            P.USER_THEME_GAME -> ThemeSettings(
                75.sp, 25.sp, 18.sp, FontFamily(Font(R.font.vt323_regular))
            )
            P.USER_THEME_HANDWRITTEN -> ThemeSettings(
                75.sp, 25.sp, 18.sp, FontFamily(Font(R.font.patrick_hand_regular))
            )
            P.USER_THEME_JUNGLE -> ThemeSettings(
                75.sp, 25.sp, 18.sp, FontFamily(Font(R.font.hanalei_regular))
            )
            P.USER_THEME_WESTERN -> ThemeSettings(
                75.sp, 25.sp, 18.sp, FontFamily(Font(R.font.ewert_regular))
            )
            else -> ThemeSettings(
                75.sp, 25.sp, 18.sp, FontFamily(Font(R.font.roboto_regular))
            )
        }
    }
}
