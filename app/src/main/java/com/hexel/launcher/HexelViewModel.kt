package com.hexel.launcher

import android.app.Application
import android.graphics.Rect
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hexel.core.hex.HexCoord
import com.hexel.core.hex.HexDirection
import com.hexel.core.layout.SlotAllocator
import com.hexel.core.model.HexApp
import com.hexel.launcher.apps.AppEntry
import com.hexel.launcher.ui.home.MapCamera
import com.hexel.launcher.ui.home.TileMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything the home map needs to draw and to resolve gestures. */
data class HomeState(
    val apps: Map<String, AppEntry> = emptyMap(),
    val tiles: Map<HexCoord, String> = emptyMap(),
    val assignments: Map<String, Map<HexDirection, String>> = emptyMap(),
    /** Hex distance of the outermost app from the centre. */
    val maxRing: Int = 0,
) {
    /** The installed app a swipe from [source] towards [direction] launches, if any. */
    fun target(source: String, direction: HexDirection): AppEntry? =
        assignments[source]?.get(direction)?.let { apps[it] }
}

class HexelViewModel(application: Application) : AndroidViewModel(application) {

    private val hexel = application as HexelApplication
    private val repository = hexel.appRepository
    private val launcher = hexel.appLauncher
    private val store = hexel.store

    val camera = MapCamera()

    /** Package whose swipe directions are being edited, or null on the home map. */
    var editingPackage by mutableStateOf<String?>(null)
        private set

    /** Direction whose target is being picked inside the editor. */
    var pickingDirection by mutableStateOf<HexDirection?>(null)
        private set

    private val placed = MutableStateFlow<Pair<List<AppEntry>, List<HexApp>>?>(null)

    private val _icons = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val icons: StateFlow<Map<String, ImageBitmap>> = _icons.asStateFlow()

    val state: StateFlow<HomeState> = combine(placed.filterNotNull(), store.assignments) { (apps, placements), assignments ->
        HomeState(
            apps = apps.associateBy { it.packageName },
            tiles = placements.associate { it.coord to it.packageName },
            assignments = assignments
                .groupBy { it.sourcePackage }
                .mapValues { (_, list) -> list.associate { it.direction to it.targetPackage } },
            maxRing = placements.maxOfOrNull { it.coord.distanceTo(HexCoord.Origin) } ?: 0,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeState())

    init {
        viewModelScope.launch {
            repository.observeApps().collectLatest { apps ->
                val existing = store.placements.first()
                val placements = SlotAllocator.allocate(existing, apps.map { it.packageName })
                if (placements.toSet() != existing.toSet()) store.savePlacements(placements)
                placed.value = apps to placements
                loadIcons(apps)
            }
        }
    }

    private suspend fun loadIcons(apps: List<AppEntry>) = withContext(Dispatchers.IO) {
        val sizePx = (TileMetrics.IconSizeDp * getApplication<Application>().resources.displayMetrics.density).toInt()
        val loaded = HashMap<String, ImageBitmap>(_icons.value)
        loaded.keys.retainAll(apps.mapTo(HashSet()) { it.packageName })
        _icons.value = HashMap(loaded)
        // Publish in chunks so the first screen fills in quickly.
        for (chunk in apps.chunked(ICON_CHUNK)) {
            for (app in chunk) repository.loadIcon(app, sizePx)?.let { loaded[app.packageName] = it }
            _icons.value = HashMap(loaded)
        }
    }

    fun launchApp(packageName: String, sourceBounds: Rect?) {
        val app = state.value.apps[packageName] ?: return
        if (!launcher.launch(app, sourceBounds)) {
            val context = getApplication<Application>()
            Toast.makeText(context, context.getString(R.string.launch_failed, app.label), Toast.LENGTH_SHORT).show()
        }
    }

    fun openEditor(packageName: String) {
        pickingDirection = null
        editingPackage = packageName
    }

    fun openPicker(direction: HexDirection) {
        pickingDirection = direction
    }

    fun closePicker() {
        pickingDirection = null
    }

    fun closeEditor() {
        pickingDirection = null
        editingPackage = null
    }

    fun assign(direction: HexDirection, target: String?) {
        val source = editingPackage ?: return
        viewModelScope.launch {
            if (target == null) store.clearAssignment(source, direction) else store.setAssignment(source, direction, target)
        }
        pickingDirection = null
    }

    private companion object {
        const val ICON_CHUNK = 24
    }
}
