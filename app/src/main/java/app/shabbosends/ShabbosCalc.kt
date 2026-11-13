package app.shabbosends

import com.kosherjava.zmanim.ZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/** Where the times are calculated for. Set by hand: the phone is offline on Shabbos. */
data class Place(
    val name: String,
    val lat: Double,
    val lng: Double,
    val tzId: String,
    val inIsrael: Boolean,
) {
    val timeZone: TimeZone get() = TimeZone.getTimeZone(tzId)
}

/**
 * One unbroken stretch of Shabbos and/or Yom Tov: a plain Shabbos, a two-day
 * Yom Tov, or a three-day run where Yom Tov meets Shabbos.
 */
data class Span(
    /** Candle lighting on the eve of the first day. */
    val candleLighting: Date,
    /** Nightfall at the end of the last day. */
    val ends: Date,
    val days: Int,
    val endsOnShabbos: Boolean,
) {
    val label: String get() = if (endsOnShabbos) "Shabbos ends" else "Yom Tov ends"
}

/**
 * Pure calculation, no Android and no network.
 *
 * Checked against chabad.org: candle lighting is sea-level sunset minus 18
 * minutes, Shabbos ends when the sun is 8.5 degrees below the horizon, and
 * both are rounded to the nearest minute.
 */
object ShabbosCalc {
    private const val TZEIS_ZENITH = 90.0 + 8.5
    private const val MINUTE = 60_000L

    fun upcoming(place: Place, from: Date, count: Int, candleMinutes: Int = 18): List<Span> {
        val tz = place.timeZone
        val geo = GeoLocation(place.name, place.lat, place.lng, 0.0, tz)
        val zmanim = ZmanimCalendar(geo)

        // Start a few days back so a span already in progress is found from its first day.
        val day = Calendar.getInstance(tz).apply {
            time = from
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, -4)
        }

        val spans = ArrayList<Span>()
        var guard = 0
        while (spans.size < count && guard++ < 800) {
            if (!isAssur(day, place.inIsrael) || isAssur(shifted(day, -1), place.inIsrael)) {
                day.add(Calendar.DAY_OF_MONTH, 1)
                continue
            }
            val first = day.clone() as Calendar
            val last = day.clone() as Calendar
            var days = 1
            while (isAssur(shifted(last, 1), place.inIsrael)) {
                last.add(Calendar.DAY_OF_MONTH, 1)
                days++
            }

            zmanim.calendar = shifted(first, -1)
            val sunset = zmanim.seaLevelSunset
            zmanim.calendar = last
            val nightfall = zmanim.getSunsetOffsetByDegrees(TZEIS_ZENITH)

            // Null only where the sun never sets or never gets that far down.
            if (sunset != null && nightfall != null && nightfall.after(from)) {
                spans.add(
                    Span(
                        candleLighting = roundToMinute(sunset.time - candleMinutes * MINUTE),
                        ends = roundToMinute(nightfall.time),
                        days = days,
                        endsOnShabbos = last.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY,
                    )
                )
            }
            day.time = last.time
            day.add(Calendar.DAY_OF_MONTH, 1)
        }
        return spans
    }

    private fun isAssur(day: Calendar, inIsrael: Boolean): Boolean {
        val jc = JewishCalendar(day)
        jc.inIsrael = inIsrael
        return jc.isAssurBemelacha
    }

    private fun shifted(day: Calendar, by: Int): Calendar =
        (day.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, by) }

    private fun roundToMinute(millis: Long): Date =
        Date(Math.floorDiv(millis + MINUTE / 2, MINUTE) * MINUTE)
}
