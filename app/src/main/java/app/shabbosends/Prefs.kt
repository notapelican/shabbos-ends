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

    /** Whether the card shows candle lighting from midday on the eve. */
    var candleCardEnabled: Boolean
        get() = sp.getBoolean("candleCard", false)
        set(v) { sp.edit().putBoolean("candleCard", v).apply() }

    /** Whether to post the two notifications that Samsung Routines listens for. */
    var routinesEnabled: Boolean
        get() = sp.getBoolean("routines", false)
        set(v) { sp.edit().putBoolean("routines", v).apply() }

    /** Minutes before candle lighting for the "starting" trigger. */
    var modeOnMinutes: Int
        get() = sp.getInt("modeOn", 10)
        set(v) { sp.edit().putInt("modeOn", v).apply() }

    /** Minutes after Shabbos ends for the "ending" trigger. */
    var modeOffMinutes: Int
        get() = sp.getInt("modeOff", 10)
        set(v) { sp.edit().putInt("modeOff", v).apply() }

    /** End time of the last Shabbos or Yom Tov the "starting" trigger fired for. */
    var lastOn: Long
        get() = sp.getLong("lastOn", 0)
        set(v) { sp.edit().putLong("lastOn", v).apply() }

    /** End time of the last Shabbos or Yom Tov the "ending" trigger fired for. */
    var lastOff: Long
        get() = sp.getLong("lastOff", 0)
        set(v) { sp.edit().putLong("lastOff", v).apply() }

    /** Whether the app turns on Do Not Disturb for Shabbos itself. */
    var dndEnabled: Boolean
        get() = sp.getBoolean("dnd", false)
        set(v) { sp.edit().putBoolean("dnd", v).apply() }

    /** True while Do Not Disturb is on because this app turned it on. */
    var dndSetByApp: Boolean
        get() = sp.getBoolean("dndSet", false)
        set(v) { sp.edit().putBoolean("dndSet", v).apply() }

    /** Whether the app locks the screen orientation for Shabbos itself. */
    var rotationEnabled: Boolean
        get() = sp.getBoolean("rotation", false)
        set(v) { sp.edit().putBoolean("rotation", v).apply() }

    /** Index into PhoneMode.ROTATION_LABELS. Landscape by default. */
    var rotationChoice: Int
        get() = sp.getInt("rotationChoice", 1)
        set(v) { sp.edit().putInt("rotationChoice", v).apply() }

    /** True while the phone's own orientation settings are saved for putting back. */
    var rotationSaved: Boolean
        get() = sp.getBoolean("rotationSaved", false)
        set(v) { sp.edit().putBoolean("rotationSaved", v).apply() }

    var savedAutoRotate: Int
        get() = sp.getInt("savedAuto", 1)
        set(v) { sp.edit().putInt("savedAuto", v).apply() }

    var savedRotation: Int
        get() = sp.getInt("savedRotation", 0)
        set(v) { sp.edit().putInt("savedRotation", v).apply() }

    /** Whether to send a third, later signal after Shabbos ends. */
    var followEnabled: Boolean
        get() = sp.getBoolean("follow", false)
        set(v) { sp.edit().putBoolean("follow", v).apply() }

    /** Minutes after Shabbos ends for the "follow-up" trigger. */
    var followMinutes: Int
        get() = sp.getInt("followMin", 30)
        set(v) { sp.edit().putInt("followMin", v).apply() }

    /** End time of the last Shabbos or Yom Tov the "follow-up" trigger fired for. */
    var lastFollow: Long
        get() = sp.getLong("lastFollow", 0)
        set(v) { sp.edit().putLong("lastFollow", v).apply() }

    var askedForNotifications: Boolean
        get() = sp.getBoolean("askedNotif", false)
        set(v) { sp.edit().putBoolean("askedNotif", v).apply() }

    companion object {
        fun isKnownTimeZone(id: String): Boolean = TimeZone.getAvailableIDs().contains(id)
    }
}
