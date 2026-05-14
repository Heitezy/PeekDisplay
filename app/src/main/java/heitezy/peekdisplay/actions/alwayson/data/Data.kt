package heitezy.peekdisplay.actions.alwayson.data

import android.content.Context
import android.provider.CalendarContract
import heitezy.peekdisplay.R
import heitezy.peekdisplay.helpers.P
import heitezy.peekdisplay.helpers.Permissions
import java.text.SimpleDateFormat
import java.util.Locale

object Data {
    private const val MILLISECONDS_PER_DAY: Long = 24 * 60 * 60 * 1000

    @Suppress("ReturnCount")
    internal fun getCalendar(context: Context, prefs: P): List<String> {
        if (!prefs.get(P.SHOW_CALENDAR, P.SHOW_CALENDAR_DEFAULT)) return listOf()
        if (!Permissions.hasCalendarPermission(context)) {
            return listOf(context.resources.getString(R.string.missing_permissions))
        }
        val singleLineClock =
            SimpleDateFormat(
                prefs.getSingleLineTimeFormat(),
                Locale.getDefault(),
            )
        val cursor =
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf("title", "dtstart", "dtend"),
                null,
                null,
                null,
            )

        if (cursor?.count == 0) return emptyList()

        cursor?.moveToFirst()
        val millis = System.currentTimeMillis()
        val eventArray = arrayListOf<Pair<Long, String>>()
        var startTime: Long
        var endTime: Long
        do {
            startTime = (cursor?.getString(1) ?: "0").toLong()
            endTime = (cursor?.getString(2) ?: "0").toLong()
            if (endTime > millis && startTime < millis + MILLISECONDS_PER_DAY) {
                val time =
                    if (startTime + MILLISECONDS_PER_DAY - endTime != 0L) {
                        singleLineClock.format(startTime) + " - " + singleLineClock.format(endTime) + " | "
                    } else {
                        ""
                    }
                eventArray.add(Pair(startTime, time + cursor?.getString(0)))
            }
        } while (cursor?.moveToNext() == true)
        cursor?.close()
        eventArray.sortBy { it.first }
        return eventArray.map { it.second }
    }
}
