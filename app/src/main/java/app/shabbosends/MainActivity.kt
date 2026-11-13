package app.shabbosends

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Date

class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var nameField: EditText
    private lateinit var latField: EditText
    private lateinit var lngField: EditText
    private lateinit var tzField: EditText
    private lateinit var israelBox: CheckBox
    private lateinit var candleField: EditText
    private lateinit var leadField: EditText
    private lateinit var statusView: TextView
    private lateinit var upcomingView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(20)
            setPadding(pad, pad, pad, pad)
        }
        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(column)
        })

        val signedDecimal = InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED

        column.addView(heading("Upcoming"))
        upcomingView = body().also(column::addView)

        column.addView(heading("Status"))
        statusView = body().also(column::addView)
        column.addView(button("Show a test card for 5 minutes") {
            val ok = Engine.postTest(this)
            toast(if (ok) "Test card sent. Lock the phone to check the Now Bar." else "No upcoming times found")
        })
        column.addView(button("Allow notifications") {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        })
        column.addView(button("Live Update setting") {
            open(
                Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        })
        column.addView(button("Turn off battery restrictions") {
            open(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName"))
            )
        })

        column.addView(heading("Location"))
        column.addView(caption("Set by hand, because the phone is offline on Shabbos. Change it before Shabbos when you travel."))
        val presets = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        presets.addView(button("Crown Heights") {
            fill(Place("Crown Heights", 40.667471, -73.9435662, "America/New_York", false))
        })
        presets.addView(button("Bondi") {
            fill(Place("Bondi", -33.8930556, 151.2633333, "Australia/Sydney", false))
        })
        column.addView(presets)
        nameField = field("Name", InputType.TYPE_CLASS_TEXT).also(column::addView)
        latField = field("Latitude", signedDecimal).also(column::addView)
        lngField = field("Longitude", signedDecimal).also(column::addView)
        tzField = field("Time zone, e.g. America/New_York", InputType.TYPE_CLASS_TEXT).also(column::addView)
        israelBox = CheckBox(this).apply { text = "In Israel (one day of Yom Tov)" }.also(column::addView)

        column.addView(heading("Timing"))
        column.addView(caption("Candle lighting, minutes before sunset"))
        candleField = field("18", InputType.TYPE_CLASS_NUMBER).also(column::addView)
        column.addView(caption("Show the card this many minutes before candle lighting"))
        leadField = field("120", InputType.TYPE_CLASS_NUMBER).also(column::addView)

        column.addView(button("Save") { save() })

        fill(prefs.place)
        candleField.setText(prefs.candleMinutes.toString())
        leadField.setText(prefs.leadMinutes.toString())
    }

    override fun onResume() {
        super.onResume()
        Engine.sync(this)
        refresh()
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        Engine.sync(this)
        refresh()
    }

    private fun fill(place: Place) {
        nameField.setText(place.name)
        latField.setText(place.lat.toString())
        lngField.setText(place.lng.toString())
        tzField.setText(place.tzId)
        israelBox.isChecked = place.inIsrael
    }

    private fun save() {
        val lat = latField.text.toString().toDoubleOrNull()
        val lng = lngField.text.toString().toDoubleOrNull()
        val tz = tzField.text.toString().trim()
        if (lat == null || lat < -90 || lat > 90) return toast("Latitude must be between -90 and 90")
        if (lng == null || lng < -180 || lng > 180) return toast("Longitude must be between -180 and 180")
        if (!Prefs.isKnownTimeZone(tz)) return toast("Unknown time zone: $tz")

        prefs.place = Place(
            name = nameField.text.toString().trim().ifEmpty { "Home" },
            lat = lat,
            lng = lng,
            tzId = tz,
            inIsrael = israelBox.isChecked,
        )
        prefs.candleMinutes = candleField.text.toString().toIntOrNull()?.coerceIn(0, 120) ?: 18
        prefs.leadMinutes = leadField.text.toString().toIntOrNull()?.coerceIn(0, 1440) ?: 120
        Engine.sync(this)
        refresh()
        toast("Saved")
    }

    private fun refresh() {
        val place = prefs.place
        val tz = place.timeZone
        val spans = ShabbosCalc.upcoming(place, Date(), 8, prefs.candleMinutes)

        upcomingView.text = if (spans.isEmpty()) "No times found for this location." else
            spans.joinToString("\n\n") { s ->
                "${s.label} ${Engine.date(s.ends, tz)}, ${Engine.time(this, s.ends, tz, true)}\n" +
                    "Candle lighting ${Engine.date(s.candleLighting, tz)}, " +
                    Engine.time(this, s.candleLighting, tz, true)
            }

        val nm = getSystemService(NotificationManager::class.java)
        val notificationsOn = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED && nm.areNotificationsEnabled()
        val liveUpdatesOn = nm.canPostPromotedNotifications()
        val batteryFree = getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(packageName)
        val promotable = spans.firstOrNull()
            ?.let { Engine.buildCard(this, place, it, 60_000).hasPromotableCharacteristics() }

        statusView.text = listOf(
            mark(notificationsOn) + "Notifications allowed",
            mark(liveUpdatesOn) + "Live Updates allowed for this app",
            mark(promotable == true) + "Card qualifies as a Live Update",
            mark(batteryFree) + "Battery restrictions off",
        ).joinToString("\n")
    }

    private fun mark(ok: Boolean) = if (ok) "✓  " else "✗  "

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Fall back to the app's own settings page.
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:$packageName"))
            )
        }
    }

    private fun heading(text: String) = TextView(this).apply {
        this.text = text
        textSize = 20f
        setPadding(0, dp(24), 0, dp(8))
    }

    private fun caption(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        alpha = 0.7f
        setPadding(0, dp(8), 0, 0)
    }

    private fun body() = TextView(this).apply {
        textSize = 15f
        setLineSpacing(0f, 1.2f)
    }

    private fun field(hint: String, type: Int) = EditText(this).apply {
        this.hint = hint
        inputType = type
        isSingleLine = true
    }

    private fun button(text: String, onClick: (View) -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener(onClick)
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
