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

        val millis = System.currentTimeMillis()
        val selection = "${CalendarContract.Events.DTEND} > ? AND ${CalendarContract.Events.DTSTART} < ?"
        val selectionArgs = arrayOf(millis.toString(), (millis + MILLISECONDS_PER_DAY).toString())

        val cursor =
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND
                ),
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC",
            )

        if (cursor == null || cursor.count == 0) {
            cursor?.close()
            return emptyList()
        }

        val eventArray = arrayListOf<String>()
        val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
        val startIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
        val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)

        while (cursor.moveToNext()) {
            val title = cursor.getString(titleIdx)
            val startTime = cursor.getLong(startIdx)
            val endTime = cursor.getLong(endIdx)

            val time = if (startTime + MILLISECONDS_PER_DAY - endTime != 0L) {
                singleLineClock.format(startTime) + " - " + singleLineClock.format(endTime) + " | "
            } else {
                ""
            }
            eventArray.add(time + title)
        }
        cursor.close()
        return eventArray
    }
}
