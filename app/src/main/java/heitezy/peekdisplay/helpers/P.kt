@file:Suppress("HardCodedStringLiteral")

package heitezy.peekdisplay.helpers

import android.content.Context
import android.content.SharedPreferences
import android.view.ViewConfiguration
import androidx.preference.PreferenceManager
import heitezy.peekdisplay.R
import java.net.URLEncoder

internal class P(private val prefs: SharedPreferences) {
    fun get(
        key: String,
        default: Boolean,
    ): Boolean = prefs.getBoolean(key, default)

    fun get(
        key: String,
        default: String,
    ): String = prefs.getString(key, default) ?: default

    fun get(
        key: String,
        default: Int,
    ): Int = prefs.getInt(key, default)

    fun displayScale(): Float = prefs.getInt("pref_aod_scale_2", DISPLAY_SCALE_DEFAULT) / NUMBER_TO_PERCENT

    fun backgroundImage(): Int? =
        when (get(BACKGROUND_IMAGE, BACKGROUND_IMAGE_DEFAULT)) {
            BACKGROUND_IMAGE_DANIEL_OLAH_1 -> R.drawable.unsplash_daniel_olah_1
            BACKGROUND_IMAGE_DANIEL_OLAH_2 -> R.drawable.unsplash_daniel_olah_2
            BACKGROUND_IMAGE_DANIEL_OLAH_3 -> R.drawable.unsplash_daniel_olah_3
            BACKGROUND_IMAGE_DANIEL_OLAH_4 -> R.drawable.unsplash_daniel_olah_4
            BACKGROUND_IMAGE_DANIEL_OLAH_5 -> R.drawable.unsplash_daniel_olah_5
            BACKGROUND_IMAGE_DANIEL_OLAH_6 -> R.drawable.unsplash_daniel_olah_6
            BACKGROUND_IMAGE_DANIEL_OLAH_7 -> R.drawable.unsplash_daniel_olah_7
            BACKGROUND_IMAGE_DANIEL_OLAH_8 -> R.drawable.unsplash_daniel_olah_8
            BACKGROUND_IMAGE_FILIP_BAOTIC_1 -> R.drawable.unsplash_filip_baotic_1
            BACKGROUND_IMAGE_TYLER_LASTOVICH_1 ->
                R.drawable.unsplash_tyler_lastovich_1

            BACKGROUND_IMAGE_TYLER_LASTOVICH_2 ->
                R.drawable.unsplash_tyler_lastovich_2

            BACKGROUND_IMAGE_TYLER_LASTOVICH_3 ->
                R.drawable.unsplash_tyler_lastovich_3

            else -> null
        }

    fun getSingleLineTimeFormat() =
        if (get(USE_12_HOUR_CLOCK, USE_12_HOUR_CLOCK_DEFAULT)) {
            if (get(SHOW_AM_PM, SHOW_AM_PM_DEFAULT)) {
                "h:mm a"
            } else {
                "h:mm"
            }
        } else {
            "H:mm"
        }

    fun getMultiLineTimeFormat(): String {
        val singleLineFormat = getSingleLineTimeFormat()
        return singleLineFormat[0] +
            singleLineFormat
                .replace(':', '\n')
                .replace(' ', '\n')
    }

    fun getWeatherUrl(): String =
        "https://wttr.in/" +
            URLEncoder.encode(
                get(
                    WEATHER_LOCATION,
                    WEATHER_LOCATION_DEFAULT,
                ),
                "utf-8",
            ) +
            if (get(WEATHER_IMPERIAL, WEATHER_IMPERIAL_DEFAULT)) {
                "?u"
            } else {
                "?m"
            } +
            "T&format=" +
            URLEncoder.encode(
                get(
                    WEATHER_FORMAT,
                    WEATHER_FORMAT_DEFAULT,
                ),
                "utf-8",
            )

    companion object {
        const val RULES_DISABLE_IN_DO_NOT_DISTURB = "rules_disable_in_do_not_disturb"
        const val RULES_CHARGING_STATE = "rules_charging_state"
        const val RULES_BATTERY = "rules_battery_level"
        const val RULES_TIMEOUT = "rules_timeout_sec"
        const val PICKUP_SENSITIVITY = "pickup_sensitivity"

        const val ALWAYS_ON = "always_on"
        const val ROOT_MODE = "root_mode"
        const val POWER_SAVING_MODE = "ao_power_saving"
        const val USER_THEME = "ao_style"
        const val SHOW_CLOCK = "ao_clock"
        const val SHOW_DATE = "ao_date"
        const val SHOW_BATTERY_ICON = "ao_batteryIcn"
        const val SHOW_BATTERY_PERCENTAGE = "ao_battery"
        const val SHOW_CALENDAR = "ao_calendar"
        const val SHOW_NOTIFICATION_COUNT = "ao_notifications"
        const val SHOW_NOTIFICATION_ICONS = "ao_notification_icons"
        const val INTERACTIVE_NOTIFICATION_ICONS = "ao_interactive_notification_icons"
        const val INVERT_INTERACTION_HIGHLIGHT = "ao_invert_interaction_highlight"
        const val SHOW_FINGERPRINT_ICON = "ao_fingerprint"
        const val FINGERPRINT_MARGIN = "ao_fingerprint_margin"
        const val FINGERPRINT_INTERACTION_MODE = "ao_fingerprint_interaction_mode"
        const val SWIPE_NOTIFICATION_OPEN = "ao_swipe_notification_open"
        const val LOCK_ICON = "ao_lock_icon"
        const val BACKGROUND_IMAGE = "ao_background_image"
        const val CUSTOM_BACKGROUND = "custom_background"
        const val EDGE_GLOW = "ao_edgeGlow"
        const val POCKET_MODE = "ao_pocket_mode"
        const val DO_NOT_DISTURB = "ao_dnd"
        const val DISABLE_HEADS_UP_NOTIFICATIONS = "heads_up"
        const val USE_12_HOUR_CLOCK = "hour"
        const val SHOW_AM_PM = "am_pm"
        const val DATE_FORMAT = "ao_date_format"
        const val FORCE_BRIGHTNESS = "ao_force_brightness"
        const val DISABLE_DOUBLE_TAP = "ao_double_tap_disabled"
        const val DOUBLE_TAP_SPEED = "ao_double_tap_speed"
        const val SHOW_MUSIC_CONTROLS = "ao_musicControls"
        const val SHOW_ALBUM_ART = "ao_album_art"
        const val MESSAGE = "ao_message"
        const val SHOW_WEATHER = "ao_weather"
        const val WEATHER_LOCATION = "ao_weather_location"
        const val WEATHER_FORMAT = "ao_weather_format"
        const val WEATHER_IMPERIAL = "ao_weather_imperial"
        const val TINT_NOTIFICATIONS = "ao_tint_notifications"
        const val ANIMATE_MOTION = "ao_smooth_animation"
        const val DISPLAY_COLOR_CLOCK = "display_color_clock"
        const val DISPLAY_COLOR_DATE = "display_color_date"
        const val DISPLAY_COLOR_BATTERY = "display_color_battery"
        const val DISPLAY_COLOR_BATTERY_ARC = "display_color_battery_arc"
        const val DISPLAY_COLOR_MUSIC_CONTROLS = "display_color_music_controls"
        const val DISPLAY_COLOR_CALENDAR = "display_color_calendar"
        const val DISPLAY_COLOR_NOTIFICATION = "display_color_notification"
        const val DISPLAY_COLOR_MESSAGE = "display_color_message"
        const val DISPLAY_COLOR_WEATHER = "display_color_weather"
        const val DISPLAY_COLOR_FINGERPRINT = "display_color_fingerprint"
        const val DISPLAY_COLOR_EDGE_GLOW = "display_color_edge_glow"
        const val FORCE_BRIGHTNESS_VALUE = "ao_force_brightness_value"
        const val VIBRATION_DURATION = "ao_vibration"
        const val EDGE_GLOW_DURATION = "ao_glowDuration"
        const val EDGE_GLOW_DELAY = "ao_glowDelay"
        const val EDGE_GLOW_STYLE = "ao_glowStyle"

        const val CHARGING_STYLE = "charging_style"
        const val NOTIFICATION_ICON_SIZE = "ao_notification_icon_size"
        const val NOTIFICATION_ICON_TOP_PADDING = "ao_notification_icon_top_padding"
        const val NOTIFICATION_PREVIEW_POSITION = "ao_notification_preview_position"
        const val TOP_PADDING = "ao_top_padding"

        const val RULES_CHARGING_STATE_CHARGING = "charging"
        const val RULES_CHARGING_STATE_DISCHARGING = "discharging"

        const val USER_THEME_MOTO = "moto"
        const val USER_THEME_GOOGLE = "google"
        const val USER_THEME_ONEPLUS = "oneplus"
        const val USER_THEME_SAMSUNG = "samsung"
        const val USER_THEME_SAMSUNG2 = "samsung2"
        const val USER_THEME_SAMSUNG3 = "samsung3"
        const val USER_THEME_80S = "80s"
        const val USER_THEME_FAST = "fast"
        const val USER_THEME_FLOWER = "flower"
        const val USER_THEME_GAME = "game"
        const val USER_THEME_HANDWRITTEN = "handwritten"
        const val USER_THEME_JUNGLE = "jungle"
        const val USER_THEME_WESTERN = "western"
        const val USER_THEME_ANALOG = "analog"

        const val BACKGROUND_IMAGE_NONE = "none"
        const val BACKGROUND_IMAGE_DANIEL_OLAH_1 = "daniel_olah_1"
        const val BACKGROUND_IMAGE_DANIEL_OLAH_2 = "daniel_olah_2"
        const val BACKGROUND_IMAGE_DANIEL_OLAH_3 = "daniel_olah_3"
        const val BACKGROUND_IMAGE_DANIEL_OLAH_4 = "daniel_olah_4"
        const val BACKGROUND_IMAGE_DANIEL_OLAH_5 = "daniel_olah_5"
        const val BACKGROUND_IMAGE_DANIEL_OLAH_6 = "daniel_olah_6"
        const val BACKGROUND_IMAGE_DANIEL_OLAH_7 = "daniel_olah_7"
        const val BACKGROUND_IMAGE_DANIEL_OLAH_8 = "daniel_olah_8"
        const val BACKGROUND_IMAGE_FILIP_BAOTIC_1 = "filip_baotic_1"
        const val BACKGROUND_IMAGE_TYLER_LASTOVICH_1 = "tyler_lastovich_1"
        const val BACKGROUND_IMAGE_TYLER_LASTOVICH_2 = "tyler_lastovich_2"
        const val BACKGROUND_IMAGE_TYLER_LASTOVICH_3 = "tyler_lastovich_3"
        const val BACKGROUND_IMAGE_CUSTOM = "custom"

        const val CHARGING_STYLE_CIRCLE = "circle"
        const val CHARGING_STYLE_FLASH = "flash"
        const val CHARGING_STYLE_IOS = "ios"

        private const val EDGE_GLOW_STYLE_ALL = "all"
        const val EDGE_GLOW_STYLE_VERTICAL = "vertical"
        const val EDGE_GLOW_STYLE_HORIZONTAL = "horizontal"

        const val RULES_DISABLE_IN_DO_NOT_DISTURB_DEFAULT = false
        const val RULES_CHARGING_STATE_DEFAULT = "always"
        const val RULES_BATTERY_DEFAULT = 0
        const val RULES_TIMEOUT_DEFAULT = 0
        const val PICKUP_SENSITIVITY_DEFAULT = "2"

        const val ALWAYS_ON_DEFAULT = false
        const val ROOT_MODE_DEFAULT = false
        const val POWER_SAVING_MODE_DEFAULT = false
        const val USER_THEME_DEFAULT = USER_THEME_MOTO
        const val SHOW_CLOCK_DEFAULT = true
        const val SHOW_DATE_DEFAULT = true
        const val SHOW_BATTERY_ICON_DEFAULT = true
        const val SHOW_BATTERY_PERCENTAGE_DEFAULT = true
        const val SHOW_CALENDAR_DEFAULT = false
        const val SHOW_NOTIFICATION_COUNT_DEFAULT = false
        const val SHOW_NOTIFICATION_ICONS_DEFAULT = true
        const val INTERACTIVE_NOTIFICATION_ICONS_DEFAULT = true
        const val INVERT_INTERACTION_HIGHLIGHT_DEFAULT = true
        const val SHOW_FINGERPRINT_ICON_DEFAULT = false
        const val FINGERPRINT_MARGIN_DEFAULT = 200
        const val FINGERPRINT_INTERACTION_MODE_DEFAULT = "swipe"
        const val SWIPE_NOTIFICATION_OPEN_DEFAULT = false
        const val LOCK_ICON_DEFAULT = false
        const val BACKGROUND_IMAGE_DEFAULT = BACKGROUND_IMAGE_NONE
        const val EDGE_GLOW_DEFAULT = false
        const val POCKET_MODE_DEFAULT = false
        const val DO_NOT_DISTURB_DEFAULT = false
        const val DISABLE_HEADS_UP_NOTIFICATIONS_DEFAULT = false
        const val USE_12_HOUR_CLOCK_DEFAULT = false
        const val SHOW_AM_PM_DEFAULT = false
        const val DATE_FORMAT_DEFAULT = "EEE, MMM d"
        const val FORCE_BRIGHTNESS_DEFAULT = false
        const val DISABLE_DOUBLE_TAP_DEFAULT = false
        const val SHOW_MUSIC_CONTROLS_DEFAULT = false
        const val SHOW_ALBUM_ART_DEFAULT = false
        const val MESSAGE_DEFAULT = ""
        const val SHOW_WEATHER_DEFAULT = false
        const val WEATHER_LOCATION_DEFAULT = ""
        const val WEATHER_FORMAT_DEFAULT = "%t"
        const val WEATHER_IMPERIAL_DEFAULT = false
        const val TINT_NOTIFICATIONS_DEFAULT = false
        const val ANIMATE_MOTION_DEFAULT = false
        const val DISPLAY_COLOR_CLOCK_DEFAULT = -1
        const val DISPLAY_COLOR_DATE_DEFAULT = -1
        const val DISPLAY_COLOR_BATTERY_DEFAULT = -1
        const val DISPLAY_COLOR_BATTERY_ARC_DEFAULT = -1
        const val DISPLAY_COLOR_MUSIC_CONTROLS_DEFAULT = -1
        const val DISPLAY_COLOR_CALENDAR_DEFAULT = -1
        const val DISPLAY_COLOR_NOTIFICATION_DEFAULT = -1
        const val DISPLAY_COLOR_MESSAGE_DEFAULT = -1
        const val DISPLAY_COLOR_WEATHER_DEFAULT = -1
        const val DISPLAY_COLOR_FINGERPRINT_DEFAULT = -1
        const val DISPLAY_COLOR_EDGE_GLOW_DEFAULT = -1
        const val FORCE_BRIGHTNESS_VALUE_DEFAULT = 50
        const val VIBRATION_DURATION_DEFAULT = 64
        const val EDGE_GLOW_DURATION_DEFAULT = 2000
        const val EDGE_GLOW_DELAY_DEFAULT = 2000

        const val CHARGING_STYLE_DEFAULT = CHARGING_STYLE_CIRCLE
        const val EDGE_GLOW_STYLE_DEFAULT = EDGE_GLOW_STYLE_ALL
        const val NOTIFICATION_ICON_SIZE_DEFAULT = "standart"
        const val NOTIFICATION_ICON_TOP_PADDING_DEFAULT = 680
        const val NOTIFICATION_PREVIEW_POSITION_DEFAULT = "above"
        const val TOP_PADDING_DEFAULT = 100

        val DOUBLE_TAP_SPEED_DEFAULT = ViewConfiguration.getDoubleTapTimeout()

        private const val DISPLAY_SCALE_DEFAULT = 100
        private const val NUMBER_TO_PERCENT = 100f

        @Suppress("NOTHING_TO_INLINE")
        inline fun getPreferences(context: Context): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(
                context,
            )
    }
}
