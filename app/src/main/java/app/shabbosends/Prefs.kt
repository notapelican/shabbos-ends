package app.shabbosends

import android.content.Context
import java.util.TimeZone

/** Saved settings. Defaults to Crown Heights until changed. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var place: Place
        get() = Place(
            name = sp.getString("name", null) ?: "Crown Heights",
            lat = sp.getString("lat", null)?.toDoubleOrNull() ?: 40.667471,
            lng = sp.getString("lng", null)?.toDoubleOrNull() ?: -73.9435662,
            tzId = sp.getString("tz", null) ?: "America/New_York",
            inIsrael = sp.getBoolean("israel", false),
        )
        set(p) {
            sp.edit()
                .putString("name", p.name)
                .putString("lat", p.lat.toString())
                .putString("lng", p.lng.toString())
                .putString("tz", p.tzId)
                .putBoolean("israel", p.inIsrael)
                .apply()
        }

    /** Minutes before sunset for candle lighting. */
    var candleMinutes: Int
        get() = sp.getInt("candle", 18)
        set(v) { sp.edit().putInt("candle", v).apply() }

    /** How long before candle lighting the card appears. */
    var leadMinutes: Int
        get() = sp.getInt("lead", 120)
        set(v) { sp.edit().putInt("lead", v).apply() }

    companion object {
        fun isKnownTimeZone(id: String): Boolean = TimeZone.getAvailableIDs().contains(id)
    }
}
