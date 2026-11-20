package app.shabbosends

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.util.Date

class MainActivity : Activity() {
    private lateinit var prefs: Prefs

    private lateinit var heroTitle: TextView
    private lateinit var heroDetail: TextView
    private lateinit var setupBox: LinearLayout
    private lateinit var placeLabel: TextView
    private lateinit var searchField: EditText
    private lateinit var searchButton: Button
    private lateinit var resultsBox: LinearLayout
    private lateinit var manualBox: LinearLayout
    private lateinit var nameField: EditText
    private lateinit var latField: EditText
    private lateinit var lngField: EditText
    private lateinit var tzField: EditText
    private lateinit var israelBox: CheckBox
    private lateinit var routineBox: LinearLayout
    private lateinit var followBox: LinearLayout
    private lateinit var upcomingView: TextView

    private var textColor = 0
    private var accentColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        textColor = themeColor(android.R.attr.textColorPrimary)
        accentColor = themeColor(android.R.attr.colorAccent)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(32))
        }
        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(page)
        })

        // What is coming up, front and centre.
        page.addView(card {
            addView(label("NEXT"))
            heroTitle = TextView(context).apply {
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(heroTitle)
            heroDetail = text("").also(::addView)
            addView(button("Preview the card for 5 minutes") {
                val ok = Engine.postTest(context)
                toast(if (ok) "Sent. Lock the phone to see it." else "No upcoming times found")
            }, spaced())
        })

        // Only lists what still needs doing.
        setupBox = card { }
        page.addView(setupBox)

        page.addView(card {
            addView(label("LOCATION"))
            placeLabel = text("").also(::addView)
            searchField = EditText(context).apply {
                hint = "Search a suburb, town, address or postcode"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                isSingleLine = true
                imeOptions = EditorInfo.IME_ACTION_SEARCH
                setOnEditorActionListener { _, action, _ ->
                    if (action == EditorInfo.IME_ACTION_SEARCH) search()
                    action == EditorInfo.IME_ACTION_SEARCH
                }
            }
            addView(searchField, spaced())
            searchButton = button("Search") { search() }.also(::addView)
            resultsBox = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            addView(resultsBox)
            addView(hint("Search needs a connection, so set the location before Shabbos when you travel."))

            addView(link("Enter coordinates by hand") {
                manualBox.visibility = if (manualBox.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            })
            manualBox = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
            }
            val decimal = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            nameField = field("Name", InputType.TYPE_CLASS_TEXT).also(manualBox::addView)
            latField = field("Latitude", decimal).also(manualBox::addView)
            lngField = field("Longitude", decimal).also(manualBox::addView)
            tzField = field("Time zone, e.g. America/New_York", InputType.TYPE_CLASS_TEXT).also(manualBox::addView)
            israelBox = CheckBox(context).apply { text = "In Israel (one day of Yom Tov)" }
            manualBox.addView(israelBox)
            manualBox.addView(button("Save location") { saveManual() })
            addView(manualBox)
        })

        page.addView(card {
            addView(label("TIMING"))
            addView(numberRow("Show the card this many minutes before candle lighting", prefs.leadMinutes, 0, 1440) {
                prefs.leadMinutes = it
            })
            addView(numberRow("Candle lighting, minutes before sunset", prefs.candleMinutes, 0, 120) {
                prefs.candleMinutes = it
            })
            addView(Switch(context).apply {
                text = "Show candle lighting first"
                textSize = 16f
                isChecked = prefs.candleCardEnabled
                setOnCheckedChangeListener { _, on ->
                    prefs.candleCardEnabled = on
                    resync()
                }
            }, spaced(16))
            addView(hint(
                "From 12:00 pm on Friday, or on the eve of Yom Tov, the card shows the candle lighting " +
                    "time. It changes to the end time at the point set above."
            ))
            addView(button("Preview the candle lighting card") {
                val ok = Engine.postTest(context, candle = true)
                toast(if (ok) "Sent. Lock the phone to see it." else "No upcoming times found")
            })
        })

        page.addView(card {
            addView(label("COMING UP"))
            upcomingView = text("").also(::addView)
        })

        // Optional, and last on the page: most people never need it.
        page.addView(card {
            addView(label("OPTIONAL: SHABBOS AUTOMATION"))
            addView(text(
                "For people who set their phone up for Shabbos automatically: flight mode on, " +
                    "Always On Display on, and so on. The app sends a signal at the right times, " +
                    "on Yom Tov as well, and a Samsung routine that you make does the switching."
            ))
            addView(hint("If you don't do this, leave it off. The card works without it."))
            addView(Switch(context).apply {
                text = "Signal Samsung Routines"
                textSize = 16f
                isChecked = prefs.routinesEnabled
                setOnCheckedChangeListener { _, on ->
                    prefs.routinesEnabled = on
                    routineBox.visibility = if (on) View.VISIBLE else View.GONE
                    resync()
                }
            })
            routineBox = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                visibility = if (prefs.routinesEnabled) View.VISIBLE else View.GONE
            }
            routineBox.addView(numberRow("Start, minutes before candle lighting", prefs.modeOnMinutes, 0, 720) {
                prefs.modeOnMinutes = it
            })
            routineBox.addView(numberRow("End, minutes after Shabbos ends", prefs.modeOffMinutes, 0, 720) {
                prefs.modeOffMinutes = it
            })
            routineBox.addView(text(
                "In Modes and Routines, make two routines. For each, choose If > Notification received, " +
                    "pick Shabbos Ends, then Notification with specific keyword.\n\n" +
                    "Routine 1, keyword \"${Engine.START_TITLE}\": flight mode on, and anything else you want.\n\n" +
                    "Routine 2, keyword \"${Engine.END_TITLE}\": flight mode off, and the rest put back."
            ), spaced())
            routineBox.addView(button("Send the start signal now") {
                Engine.postTrigger(context, Engine.START_TITLE)
                toast("Sent: ${Engine.START_TITLE}")
            }, spaced())
            routineBox.addView(button("Send the end signal now") {
                Engine.postTrigger(context, Engine.END_TITLE)
                toast("Sent: ${Engine.END_TITLE}")
            })

            // Optional third signal, some time after Shabbos has ended.
            routineBox.addView(Switch(context).apply {
                text = "Send a later follow-up signal"
                textSize = 16f
                isChecked = prefs.followEnabled
                setOnCheckedChangeListener { _, on ->
                    prefs.followEnabled = on
                    followBox.visibility = if (on) View.VISIBLE else View.GONE
                    resync()
                }
            }, spaced(16))
            followBox = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                visibility = if (prefs.followEnabled) View.VISIBLE else View.GONE
            }
            followBox.addView(numberRow("Follow-up, minutes after Shabbos ends", prefs.followMinutes, 0, 720) {
                prefs.followMinutes = it
            })
            followBox.addView(text(
                "Routine 3, keyword \"${Engine.FOLLOW_TITLE}\": whatever should happen later, " +
                    "such as Always On Display off."
            ), spaced())
            followBox.addView(button("Send the follow-up signal now") {
                Engine.postTrigger(context, Engine.FOLLOW_TITLE)
                toast("Sent: ${Engine.FOLLOW_TITLE}")
            }, spaced())
            routineBox.addView(followBox)
            addView(routineBox)
        })

        fillManual(prefs.place)
    }

    override fun onResume() {
        super.onResume()
        resync()
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        resync()
    }

    private fun resync() {
        try {
            Engine.sync(this)
        } catch (e: Exception) {
            toast("Couldn't schedule: ${e.message}")
        }
        refresh()
    }

    // ---- Location ----

    private fun search() {
        val query = searchField.text.toString().trim()
        if (query.length < 2) return toast("Type at least two letters")
        hideKeyboard()
        searchButton.isEnabled = false
        searchButton.text = "Searching…"
        resultsBox.removeAllViews()
        Thread {
            val hits = try {
                Geocoder.search(query)
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                searchButton.isEnabled = true
                searchButton.text = "Search"
                when {
                    hits == null -> resultsBox.addView(hint("Couldn't search. Check the connection and try again."))
                    hits.isEmpty() -> resultsBox.addView(hint("Nothing found. Try a nearby town or a postcode."))
                    else -> hits.forEach { hit ->
                        resultsBox.addView(result(hit.title, hit.detail) { pick(hit) })
                    }
                }
            }
        }.start()
    }

    /** A search result was tapped. Finds its time zone if the search didn't give one. */
    private fun pick(hit: Geocoder.Hit) {
        if (hit.tzId != null) {
            return choose(Place(hit.title, hit.lat, hit.lng, hit.tzId, hit.inIsrael))
        }
        resultsBox.removeAllViews()
        resultsBox.addView(hint("Setting location…"))
        Thread {
            val found = Geocoder.timeZoneAt(hit.lat, hit.lng)
            runOnUiThread {
                val tz = found ?: java.util.TimeZone.getDefault().id
                choose(Place(hit.title, hit.lat, hit.lng, tz, hit.inIsrael))
                if (found == null) {
                    toast("Couldn't look up the time zone, so the phone's own ($tz) is used. Check it below.")
                    manualBox.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    private fun choose(place: Place) {
        prefs.place = place
        resultsBox.removeAllViews()
        searchField.setText("")
        fillManual(place)
        resync()
        toast("Location set to ${place.name}")
    }

    private fun fillManual(place: Place) {
        nameField.setText(place.name)
        latField.setText(place.lat.toString())
        lngField.setText(place.lng.toString())
        tzField.setText(place.tzId)
        israelBox.isChecked = place.inIsrael
    }

    private fun saveManual() {
        val lat = latField.text.toString().toDoubleOrNull()
        val lng = lngField.text.toString().toDoubleOrNull()
        val tz = tzField.text.toString().trim()
        if (lat == null || lat < -90 || lat > 90) return toast("Latitude must be between -90 and 90")
        if (lng == null || lng < -180 || lng > 180) return toast("Longitude must be between -180 and 180")
        if (!Prefs.isKnownTimeZone(tz)) return toast("Unknown time zone: $tz")
        hideKeyboard()
        choose(
            Place(
                name = nameField.text.toString().trim().ifEmpty { "Home" },
                lat = lat,
                lng = lng,
                tzId = tz,
                inIsrael = israelBox.isChecked,
            )
        )
    }

    // ---- Display ----

    private fun refresh() {
        val place = prefs.place
        val tz = place.timeZone
        val spans = ShabbosCalc.upcoming(place, Date(), 8, prefs.candleMinutes)
        val next = spans.firstOrNull()

        placeLabel.text = place.name + (if (place.inIsrael) " (Israel)" else "") + "\n" + place.tzId

        if (next == null) {
            heroTitle.text = "No times found"
            heroDetail.text = "Check the location below."
        } else {
            heroTitle.text = "${next.label} ${Engine.time(this, next.ends, tz, true)}"
            heroDetail.text = "${Engine.date(next.ends, tz)}\n" +
                "Candle lighting ${Engine.date(next.candleLighting, tz)}, " +
                "${Engine.time(this, next.candleLighting, tz, true)}\n${place.name}"
        }

        upcomingView.text = spans.drop(1).joinToString("\n\n") { s ->
            "${s.label} ${Engine.date(s.ends, tz)}, ${Engine.time(this, s.ends, tz, true)}\n" +
                "Candle lighting ${Engine.date(s.candleLighting, tz)}, " +
                Engine.time(this, s.candleLighting, tz, true)
        }

        refreshSetup(next)
    }

    private fun refreshSetup(next: Span?) {
        val nm = getSystemService(NotificationManager::class.java)
        val notificationsOn = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED && nm.areNotificationsEnabled()
        val liveUpdatesOn = nm.canPostPromotedNotifications()
        val batteryFree = getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(packageName)
        val promotable = next == null ||
            Engine.buildCard(this, prefs.place, next, 60_000).hasPromotableCharacteristics()

        setupBox.removeAllViews()
        setupBox.addView(label("SETUP"))
        if (!notificationsOn) {
            setupBox.addView(todo("Notifications are off, so the card can't appear.", "Allow") {
                if (prefs.askedForNotifications) {
                    open(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    )
                } else {
                    prefs.askedForNotifications = true
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                }
            })
        }
        if (!liveUpdatesOn) {
            setupBox.addView(todo("Live Updates are off, so the card stays out of the Now Bar.", "Turn on") {
                open(
                    Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                )
            })
        }
        if (!batteryFree) {
            setupBox.addView(todo("Battery saving may stop the Friday alarm.", "Fix") {
                open(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:$packageName"))
                )
            })
        }
        if (!promotable) {
            setupBox.addView(text("This phone doesn't accept the card as a Live Update."))
        }
        if (notificationsOn && liveUpdatesOn && batteryFree && promotable) {
            setupBox.addView(text("✓  Ready. The card appears on its own each week."))
        }
    }

    // ---- Small building blocks ----

    private fun card(build: LinearLayout.() -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(16))
        background = GradientDrawable().apply {
            cornerRadius = dp(22).toFloat()
            // A faint tint of the text colour reads as a card in light and dark themes.
            setColor((textColor and 0x00FFFFFF) or 0x14000000)
        }
        layoutParams = spaced(12)
        build()
    }

    private fun spaced(top: Int = 8) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(top) }

    private fun label(value: String) = TextView(this).apply {
        text = value
        textSize = 12f
        letterSpacing = 0.12f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(accentColor)
        setPadding(0, 0, 0, dp(6))
    }

    private fun text(value: String) = TextView(this).apply {
        text = value
        textSize = 15f
        setLineSpacing(0f, 1.25f)
    }

    private fun hint(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        alpha = 0.65f
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun link(value: String, onClick: (View) -> Unit) = TextView(this).apply {
        text = value
        textSize = 16f
        setTextColor(accentColor)
        setPadding(0, dp(12), 0, dp(12))
        setOnClickListener(onClick)
    }

    /** A search result: the place in large type, where it is underneath. */
    private fun result(title: String, detail: String, onClick: (View) -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(10), 0, dp(10))
        addView(TextView(context).apply {
            text = title
            textSize = 16f
            setTextColor(accentColor)
        })
        if (detail.isNotEmpty()) {
            addView(TextView(context).apply {
                text = detail
                textSize = 13f
                alpha = 0.7f
            })
        }
        setOnClickListener(onClick)
    }

    private fun field(hintText: String, type: Int) = EditText(this).apply {
        hint = hintText
        inputType = type
        isSingleLine = true
    }

    private fun button(value: String, onClick: (View) -> Unit) = Button(this).apply {
        text = value
        isAllCaps = false
        setOnClickListener(onClick)
    }

    /** One thing left to set up, with the button that fixes it. */
    private fun todo(message: String, action: String, onClick: (View) -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(text(message), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(button(action, onClick))
    }

    /** A labelled number that saves itself as soon as it is valid. */
    private fun numberRow(title: String, value: Int, min: Int, max: Int, save: (Int) -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(text(title), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(EditText(context).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                isSingleLine = true
                gravity = Gravity.CENTER
                setText(value.toString())
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val number = s?.toString()?.toIntOrNull() ?: return
                        if (number < min || number > max) return
                        save(number)
                        resync()
                    }
                })
            }, LinearLayout.LayoutParams(dp(76), LinearLayout.LayoutParams.WRAP_CONTENT))
        }

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

    private fun hideKeyboard() {
        getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(searchField.windowToken, 0)
    }

    private fun themeColor(attr: Int): Int {
        val values = obtainStyledAttributes(intArrayOf(attr))
        val color = values.getColor(0, 0xFF888888.toInt())
        values.recycle()
        return color
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
