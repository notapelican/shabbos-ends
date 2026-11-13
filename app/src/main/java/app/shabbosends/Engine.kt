package app.shabbosends

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Decides what should be on screen right now, puts it there, and sets one
 * alarm for the next moment something changes. Everything runs offline.
 */
object Engine {
    const val ACTION_SYNC = "app.shabbosends.SYNC"
    private const val CHANNEL = "shabbos_ends"
    private const val CARD_ID = 1
    private const val TEST_ID = 2
    private const val MINUTE = 60_000L

    fun sync(context: Context) {
        val prefs = Prefs(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        ensureChannel(nm)

        val now = System.currentTimeMillis()
        val span = ShabbosCalc.upcoming(prefs.place, Date(now), 1, prefs.candleMinutes).firstOrNull()
        if (span == null) {
            nm.cancel(CARD_ID)
            return
        }

        val showAt = span.candleLighting.time - prefs.leadMinutes * MINUTE
        if (now >= showAt) {
            nm.notify(CARD_ID, buildCard(context, prefs.place, span, span.ends.time - now))
            // Wake once more when it is over, to clear the card and line up next week.
            schedule(context, span.ends.time + 1000)
        } else {
            nm.cancel(CARD_ID)
            schedule(context, showAt)
        }
    }

    /** Posts the next card for five minutes, to see how the phone displays it. */
    fun postTest(context: Context): Boolean {
        val prefs = Prefs(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        ensureChannel(nm)
        val span = ShabbosCalc.upcoming(prefs.place, Date(), 1, prefs.candleMinutes).firstOrNull()
            ?: return false
        nm.notify(TEST_ID, buildCard(context, prefs.place, span, 5 * MINUTE))
        return true
    }

    fun buildCard(context: Context, place: Place, span: Span, lifetimeMillis: Long): Notification {
        val tz = place.timeZone
        // Name the day whenever it is not simply "tomorrow night".
        val endDay = if (span.days > 1 || !span.endsOnShabbos) day(span.ends, tz) + " " else ""
        // Kept short so the Now Bar shows all of it: "Shabbos ends 5:22", "Candles Fri 4:21".
        val title = "${span.label} $endDay${time(context, span.ends, tz, false)}"
        val text = "Candles ${day(span.candleLighting, tz)} ${time(context, span.candleLighting, tz, false)}"

        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_stars)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_EVENT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setTimeoutAfter(lifetimeMillis.coerceAtLeast(MINUTE))
            // These two make it an Android 16 Live Update. The promotion request
            // only became a named method in API 36.1, so it is set by its key.
            .setShortCriticalText(time(context, span.ends, tz, false))
            .addExtras(Bundle().apply { putBoolean("android.requestPromotedOngoing", true) })
            .build()
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL) != null) return
        val channel = NotificationChannel(CHANNEL, "Shabbos ends", NotificationManager.IMPORTANCE_LOW)
        channel.description = "The card showing when Shabbos or Yom Tov ends"
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        channel.setShowBadge(false)
        nm.createNotificationChannel(channel)
    }

    private fun schedule(context: Context, atMillis: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_SYNC),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    fun time(context: Context, date: Date, tz: TimeZone, amPm: Boolean): String {
        val pattern = when {
            DateFormat.is24HourFormat(context) -> "HH:mm"
            amPm -> "h:mm a"
            else -> "h:mm"
        }
        return SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = tz }.format(date)
    }

    fun day(date: Date, tz: TimeZone): String =
        SimpleDateFormat("EEE", Locale.getDefault()).apply { timeZone = tz }.format(date)

    fun date(date: Date, tz: TimeZone): String =
        SimpleDateFormat("EEE d MMM", Locale.getDefault()).apply { timeZone = tz }.format(date)
}
