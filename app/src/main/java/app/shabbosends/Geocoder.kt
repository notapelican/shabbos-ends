package app.shabbosends

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Place search using Open-Meteo's free geocoding service, which is built on
 * the open GeoNames database. It finds towns, neighbourhoods and postcodes
 * (not street addresses) and returns the time zone with each one.
 *
 * Only used when the user searches. Never on Shabbos.
 */
object Geocoder {
    data class Hit(val place: Place, val label: String)

    /** Blocking network call: run it off the main thread. */
    fun search(query: String): List<Hit> {
        val url = URL(
            "https://geocoding-api.open-meteo.com/v1/search?count=8&language=en&format=json&name=" +
                URLEncoder.encode(query.trim(), "UTF-8")
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "ShabbosEnds/1.1")
        try {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
            val hits = ArrayList<Hit>()
            for (i in 0 until results.length()) {
                val r = results.getJSONObject(i)
                val name = r.optString("name")
                val tz = r.optString("timezone")
                if (name.isEmpty() || !r.has("latitude") || !r.has("longitude")) continue
                if (!Prefs.isKnownTimeZone(tz)) continue
                val label = listOf(name, r.optString("admin1"), r.optString("country"))
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .joinToString(", ")
                hits.add(
                    Hit(
                        Place(
                            name = name,
                            lat = r.getDouble("latitude"),
                            lng = r.getDouble("longitude"),
                            tzId = tz,
                            inIsrael = r.optString("country_code") == "IL",
                        ),
                        label,
                    )
                )
            }
            return hits
        } finally {
            conn.disconnect()
        }
    }
}
