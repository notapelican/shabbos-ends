package app.shabbosends

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Place search. Only used when the user searches, never on Shabbos.
 *
 * Search is OpenStreetMap's Nominatim, which knows suburbs, streets and
 * postcodes and describes each result in full ("Crown Heights, Brooklyn,
 * Kings County, New York"). It doesn't know time zones, so the zone for the
 * chosen place comes from Open-Meteo. If Nominatim can't be reached, search
 * falls back to Open-Meteo's own, simpler place list.
 */
object Geocoder {
    data class Hit(
        val title: String,
        val detail: String,
        val lat: Double,
        val lng: Double,
        /** Null when the search didn't supply one; look it up with [timeZoneAt]. */
        val tzId: String?,
        val inIsrael: Boolean,
    )

    private const val AGENT = "ShabbosMode/2.0 (github.com/notapelican)"

    /** Blocking network call: run it off the main thread. */
    fun search(query: String): List<Hit> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val fromMaps = try {
            nominatim(q)
        } catch (e: Exception) {
            emptyList()
        }
        return fromMaps.ifEmpty { openMeteo(q) }
    }

    /** Blocking network call. Returns null if the zone can't be found. */
    fun timeZoneAt(lat: Double, lng: Double): String? = try {
        val body = get(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng" +
                "&current=temperature_2m&timezone=auto"
        )
        JSONObject(body).optString("timezone").takeIf { Prefs.isKnownTimeZone(it) }
    } catch (e: Exception) {
        null
    }

    private fun nominatim(q: String): List<Hit> {
        val body = get(
            "https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1" +
                "&limit=8&accept-language=en&q=$q"
        )
        val results = JSONArray(body)
        val hits = ArrayList<Hit>()
        for (i in 0 until results.length()) {
            val r = results.getJSONObject(i)
            val full = r.optString("display_name")
            val lat = r.optString("lat").toDoubleOrNull()
            val lng = r.optString("lon").toDoubleOrNull()
            if (full.isEmpty() || lat == null || lng == null) continue
            val parts = full.split(", ")
            val title = r.optString("name").ifEmpty { parts.first() }
            val detail = (if (parts.first() == title) parts.drop(1) else parts).joinToString(", ")
            val country = r.optJSONObject("address")?.optString("country_code").orEmpty()
            if (hits.any { it.title == title && it.detail == detail }) continue
            // One day of Yom Tov is kept throughout both.
            hits.add(Hit(title, detail, lat, lng, null, country == "il" || country == "ps"))
        }
        return hits
    }

    private fun openMeteo(q: String): List<Hit> {
        val body = get(
            "https://geocoding-api.open-meteo.com/v1/search?count=8&language=en&format=json&name=$q"
        )
        val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
        val hits = ArrayList<Hit>()
        for (i in 0 until results.length()) {
            val r = results.getJSONObject(i)
            val name = r.optString("name")
            val tz = r.optString("timezone")
            if (name.isEmpty() || !r.has("latitude") || !r.has("longitude")) continue
            val detail = listOf("admin3", "admin2", "admin1", "country")
                .map { r.optString(it) }
                .filter { it.isNotEmpty() && it != name }
                .distinct()
                .joinToString(", ")
            hits.add(
                Hit(
                    title = name,
                    detail = detail,
                    lat = r.getDouble("latitude"),
                    lng = r.getDouble("longitude"),
                    tzId = tz.takeIf { Prefs.isKnownTimeZone(it) },
                    inIsrael = r.optString("country_code") == "IL",
                )
            )
        }
        return hits
    }

    private fun get(address: String): String {
        val conn = URL(address).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", AGENT)
        try {
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
