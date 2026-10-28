package com.hexel.launcher.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.util.Log

/** Starts apps. Never throws: returns false when the app cannot be started. */
class AppLauncher(context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    fun launch(app: AppEntry, sourceBounds: Rect?): Boolean = try {
        if (!launcherApps.isActivityEnabled(app.component, app.user)) {
            Log.w(TAG, "Activity disabled: ${app.component}")
            false
        } else {
            launcherApps.startMainActivity(app.component, app.user, sourceBounds, null)
            true
        }
    } catch (e: Exception) {
        // ActivityNotFoundException, SecurityException, IllegalStateException (locked profile)...
        Log.w(TAG, "Failed to launch ${app.component}", e)
        false
    }

    private companion object {
        const val TAG = "HexelLaunch"
    }
}
