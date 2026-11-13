package app.shabbosends

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Fires on our own alarm, and after a reboot, clock change or app update. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            Engine.sync(context)
        } catch (e: Exception) {
            Log.e("ShabbosEnds", "sync failed", e)
        }
    }
}
