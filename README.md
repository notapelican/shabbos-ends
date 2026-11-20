# Shabbos Ends

Shows when Shabbos or Yom Tov ends as an Android 16 Live Update. Samsung puts
it in the Now Bar and on the Always On Display; other Android 16 phones show
Live Updates on the lock screen and Always On Display. Built and tested on a
Samsung Galaxy; other brands are untested.

Everything is calculated on the phone, so it works in flight mode.

## Build

1. Create a **private** GitHub repo (the signing key is in `app/sideload.jks`).
2. Push this folder to the `main` branch:

   ```
   git init -b main
   git add .
   git commit -m "Shabbos Ends"
   git remote add origin git@github.com:YOUR-USER/shabbos-ends.git
   git push -u origin main
   ```

3. Wait for the **Build APK** run under the Actions tab (a few minutes).
4. On the phone, open the repo's **Releases** page and download
   `shabbos-ends.apk` from the release called "Latest build".

Every push to `main` rebuilds and replaces that release. Builds are signed with
the same key, so a new APK installs over the old one.

## Set up on the phone

1. Install the APK and open the app.
2. The **Setup** box lists anything still to do, each with a button. Work
   through it until it says "Ready".
3. Under **Location**, search for your town, suburb or postcode and tap the
   result. Search needs a connection; everything else works offline.
4. Tap **Preview the card for 5 minutes**, lock the phone, and look at the
   Now Bar and the Always On Display.

If the preview only appears as an ordinary notification, turn on
Settings > Developer options > **Live notifications for all apps** and try again.

## What it does

- The "Shabbos ends" card appears at candle lighting (adjustable to earlier)
  and removes itself when Shabbos or Yom Tov ends.
- A two-day Yom Tov, or Yom Tov running into Shabbos, is one card showing the
  final end time with its day, e.g. "Shabbos ends Sat 8:28".
- Optionally ("Show candle lighting first"), the card appears earlier on
  Friday or the eve of Yom Tov showing "Candle lighting 4:16", then changes to
  the end time at the usual point. It starts at 12:00 pm unless you pick
  another time.
- No notification from the app makes a sound or vibrates.
- It reschedules itself after a reboot, a clock or time zone change, and an
  app update.

## Optional: Shabbos automation

This part is only for people who set their phone up for Shabbos automatically.
It is off by default and the card works without it. Everything in it runs at
the same two moments: a set time before candle lighting, and a set time after
Shabbos or Yom Tov ends.

The options sit in two submenus, **Samsung** and **Pixel and other phones**.
The one that matches the phone starts open.

### "Pixel and other phones": built in, works on any Android 16 phone

- **Do Not Disturb during Shabbos.** Needs Do Not Disturb access, which the
  app asks for when you switch it on. On recent Android versions it shows up in
  the phone's Modes list under the app's name, where you can adjust who may
  still get through. It is only turned off afterwards if the app turned it on.
- **Lock the screen orientation during Shabbos** (optional, separately).
  Choose portrait, landscape or flipped landscape. Needs the "modify system
  settings" permission. The phone's own setting is put back afterwards.

The two "Try it" buttons run the start and the end on demand.

### "Samsung": flight mode and the rest, through Samsung Routines

The app can't turn on flight mode itself, but Samsung Routines can, and the app
can tell it when. Turn on **Signal Samsung Routines** in the app, then make two
routines in Modes and Routines:

| Routine | If | Then |
|---|---|---|
| Start | Notification received > Shabbos Ends > keyword `Shabbos mode starting` | Flight mode on, and anything else |
| End | Notification received > Shabbos Ends > keyword `Shabbos mode ending` | Flight mode off, and the rest put back |

The app sends the first signal a set time before candle lighting and the second
a set time after Shabbos or Yom Tov ends. Each is a silent notification that
disappears after two minutes. The two "Try it" buttons send them on demand,
for testing the routines.

There is an optional third signal, **Send a later follow-up signal**, sent a
set time after Shabbos ends (30 minutes by default). Its keyword is
`Shabbos mode follow-up`. Use it for a routine that should run later than the
end routine, such as turning the Always On Display off.

The end and follow-up signals are only sent for a Shabbos or Yom Tov the app
sent a start signal for.

## The times

- Candle lighting: sea-level sunset minus 18 minutes (adjustable).
- Shabbos or Yom Tov ends: sun 8.5 degrees below the horizon.
- Both rounded to the nearest minute, the way chabad.org shows them.

Calculation is by the KosherJava zmanim library. The logic is in
`ShabbosCalc.kt` and has no Android code in it.

## Place search

Search uses OpenStreetMap (the Nominatim service), so it finds suburbs, towns,
addresses and postcodes and describes each result in full, for example
"Crown Heights: Brooklyn, Kings County, New York, United States". The time
zone for the chosen place comes from Open-Meteo. If OpenStreetMap can't be
reached, search falls back to Open-Meteo's simpler place list. Coordinates can
also be entered by hand.
