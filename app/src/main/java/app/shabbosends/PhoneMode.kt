package app.shabbosends

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.Surface

/**
 * The built-in Shabbos automation, which works on any Android phone:
 * Do Not Disturb, and locking the screen orientation. Each is optional,
 * each needs its own permission, and each is put back afterwards.
 *
 * Flight mode is not here because Android does not let ordinary apps switch it.
 */
object PhoneMode {
    /** Choices offered for the orientation lock, in menu order. */
    val ROTATION_LABELS = listOf("Portrait", "Landscape", "Landscape, flipped")
    private val ROTATION_VALUES = listOf(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_270)

    fun canSetDnd(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun canSetRotation(context: Context): Boolean = Settings.System.canWrite(context)

    fun start(context: Context, prefs: Prefs) {
        if (prefs.dndEnabled) attempt("dnd on") {
            if (canSetDnd(context)) {
                // On Android 15 and later this appears in the phone's Modes list under
                // the app's name, where the user can adjust who may still get through.
                context.getSystemService(NotificationManager::class.java)
                    .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                prefs.dndSetByApp = true
            }
        }
        if (prefs.rotationEnabled) attempt("rotation lock") {
            if (canSetRotation(context)) {
                val resolver = context.contentResolver
                if (!prefs.rotationSaved) {
                    // Remember how the phone was, to put it back afterwards.
                    prefs.savedAutoRotate = Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                    prefs.savedRotation = Settings.System.getInt(resolver, Settings.System.USER_ROTATION, Surface.ROTATION_0)
                    prefs.rotationSaved = true
                }
                val choice = ROTATION_VALUES.getOrElse(prefs.rotationChoice) { Surface.ROTATION_0 }
                Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                Settings.System.putInt(resolver, Settings.System.USER_ROTATION, choice)
            }
        }
    }

    fun end(context: Context, prefs: Prefs) {
        endDnd(context, prefs)
        endRotation(context, prefs)
    }

    /** Turns Do Not Disturb back off, but only if this app turned it on. */
    fun endDnd(context: Context, prefs: Prefs) {
        if (!prefs.dndSetByApp) return
        attempt("dnd off") {
            if (canSetDnd(context)) {
                context.getSystemService(NotificationManager::class.java)
                    .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }
        prefs.dndSetByApp = false
    }

    /** Restores the orientation settings saved by [start]. */
    fun endRotation(context: Context, prefs: Prefs) {
        if (!prefs.rotationSaved) return
        attempt("rotation restore") {
            if (canSetRotation(context)) {
                val resolver = context.contentResolver
                Settings.System.putInt(resolver, Settings.System.USER_ROTATION, prefs.savedRotation)
                Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, prefs.savedAutoRotate)
            }
        }
        prefs.rotationSaved = false
    }

    /** One failing action must not stop the others. */
    private fun attempt(what: String, action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            Log.e("ShabbosEnds", "$what failed", e)
        }
    }
}
