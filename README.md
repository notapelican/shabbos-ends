# Shabbos Ends

Shows when Shabbos or Yom Tov ends as an Android 16 Live Update, which Samsung
puts in the Now Bar and on the Always On Display.

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
2. Tap **Allow notifications**.
3. Tap **Live Update setting** and make sure it is on.
4. Tap **Turn off battery restrictions**.
5. Pick a location and tap **Save**.
6. Tap **Show a test card for 5 minutes**, lock the phone, and look at the
   Now Bar and the Always On Display.

If the test card only appears as an ordinary notification, turn on
Settings > Developer options > **Live notifications for all apps** and try again.

## What it does

- The card appears 2 hours before candle lighting (adjustable) and removes
  itself when Shabbos or Yom Tov ends.
- A two-day Yom Tov, or Yom Tov running into Shabbos, is one card showing the
  final end time with its day, e.g. "Shabbos ends Sat 8:28 PM".
- It reschedules itself after a reboot, a clock or time zone change, and an
  app update.

## The times

- Candle lighting: sea-level sunset minus 18 minutes (adjustable).
- Shabbos or Yom Tov ends: sun 8.5 degrees below the horizon.
- Both rounded to the nearest minute, the way chabad.org shows them.

Calculation is by the KosherJava zmanim library. The logic is in
`ShabbosCalc.kt` and has no Android code in it.
