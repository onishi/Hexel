package com.hexel.launcher.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hexel.core.hex.HexDirection
import com.hexel.core.model.GestureAssignment
import com.hexel.core.model.HexApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.hexelDataStore: DataStore<Preferences> by preferencesDataStore(name = "hexel")

/**
 * Persists tile placements and swipe assignments in a Preferences DataStore.
 *
 * Keys:
 * - `slot/<package>`              -> "q,r"
 * - `swipe/<package>/<DIRECTION>` -> target package
 */
class HexelStore(context: Context) {

    private val store = context.applicationContext.hexelDataStore

    private val prefs: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) {
            Log.w(TAG, "DataStore read failed", e)
            emit(emptyPreferences())
        } else {
            throw e
        }
    }

    val placements: Flow<List<HexApp>> = prefs.map { p ->
        p.asMap().mapNotNull { (key, value) ->
            val pkg = key.name.removePrefix(SLOT_PREFIX).takeIf { key.name.startsWith(SLOT_PREFIX) }
                ?: return@mapNotNull null
            val parts = (value as? String)?.split(',') ?: return@mapNotNull null
            val q = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val r = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            HexApp(pkg, q, r)
        }
    }

    val assignments: Flow<List<GestureAssignment>> = prefs.map { p ->
        p.asMap().mapNotNull { (key, value) ->
            if (!key.name.startsWith(SWIPE_PREFIX)) return@mapNotNull null
            val rest = key.name.removePrefix(SWIPE_PREFIX)
            val source = rest.substringBeforeLast('/')
            val direction = runCatching { HexDirection.valueOf(rest.substringAfterLast('/')) }.getOrNull()
                ?: return@mapNotNull null
            val target = value as? String ?: return@mapNotNull null
            GestureAssignment(source, direction, target)
        }
    }

    suspend fun savePlacements(apps: List<HexApp>) {
        store.edit { p ->
            p.asMap().keys.filter { it.name.startsWith(SLOT_PREFIX) }.forEach { p.remove(it) }
            apps.forEach { p[slotKey(it.packageName)] = "${it.q},${it.r}" }
        }
    }

    suspend fun setAssignment(source: String, direction: HexDirection, target: String) {
        store.edit { it[swipeKey(source, direction)] = target }
    }

    suspend fun clearAssignment(source: String, direction: HexDirection) {
        store.edit { it.remove(swipeKey(source, direction)) }
    }

    private fun slotKey(pkg: String) = stringPreferencesKey("$SLOT_PREFIX$pkg")
    private fun swipeKey(pkg: String, dir: HexDirection) = stringPreferencesKey("$SWIPE_PREFIX$pkg/${dir.name}")

    private companion object {
        const val TAG = "HexelStore"
        const val SLOT_PREFIX = "slot/"
        const val SWIPE_PREFIX = "swipe/"
    }
}
