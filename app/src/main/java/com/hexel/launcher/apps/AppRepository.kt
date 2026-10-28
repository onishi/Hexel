package com.hexel.launcher.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.text.Collator

/** Reads installed launchable apps through [LauncherApps] and watches for package changes. */
class AppRepository(context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val ownPackage = context.packageName
    private val densityDpi = context.resources.displayMetrics.densityDpi

    /**
     * Emits the launchable apps of the current user, sorted by label, and emits again whenever a
     * package is installed, removed or updated. Loading runs on [Dispatchers.IO].
     */
    fun observeApps(): Flow<List<AppEntry>> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageAdded(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(Unit)
            }

            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(Unit)
            }
        }
        trySend(Unit)
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }
        .conflate()
        .map { loadApps() }
        .flowOn(Dispatchers.IO)

    private fun loadApps(): List<AppEntry> {
        val collator = Collator.getInstance()
        return try {
            launcherApps.getActivityList(null, Process.myUserHandle())
                .asSequence()
                .map { AppEntry(it) }
                .filter { it.packageName != ownPackage }
                .distinctBy { it.packageName }
                .sortedWith { a, b -> collator.compare(a.label, b.label) }
                .toList()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query launcher activities", e)
            emptyList()
        }
    }

    /** Renders the app icon to a bitmap. Call off the main thread. */
    fun loadIcon(app: AppEntry, sizePx: Int): ImageBitmap? = try {
        app.info.getIcon(densityDpi)?.toBitmap(sizePx, sizePx)?.asImageBitmap()
    } catch (e: Exception) {
        Log.w(TAG, "Failed to load icon for ${app.packageName}", e)
        null
    }

    private companion object {
        const val TAG = "HexelApps"
    }
}
