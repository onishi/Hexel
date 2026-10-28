package com.hexel.launcher.ui.home

import android.graphics.Rect
import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import com.hexel.core.gesture.GestureClassifier
import com.hexel.core.gesture.GestureSample
import com.hexel.core.gesture.SwipeOutcome
import com.hexel.core.hex.Vec2
import com.hexel.launcher.HomeState

interface HexMapGestureCallbacks {
    fun onTap(packageName: String, bounds: Rect)
    fun onLongPress(packageName: String)
    fun onPreview(value: SwipePreview?)
    fun onSwipe(packageName: String, bounds: Rect)
    fun onFling(velocityPx: Offset)
}

private const val TAG = "HexelGesture"

private enum class FirstPhase { Tap, Moved, Abort }

/**
 * Gesture loop for the hex map.
 *
 * - Start on an empty tile (or while the map is still gliding): drag pans, release flings.
 * - Start on an app tile:
 *   - release within the touch slop -> tap (launch the app)
 *   - hold still for `longPressMs`  -> long press (edit swipe directions)
 *   - move beyond the touch slop    -> the map stays put; on release [GestureClassifier] decides
 *     between flick (fling the map), swipe (launch the assigned app) and cancel.
 * - A second finger cancels whatever is in progress; nothing is launched.
 */
internal suspend fun PointerInputScope.hexMapGestures(
    classifier: GestureClassifier,
    camera: MapCamera,
    geometry: () -> MapGeometry,
    state: () -> HomeState,
    callbacks: HexMapGestureCallbacks,
) {
    val config = classifier.config
    val slopPx = config.touchSlopDp * density
    fun dp(p: Offset) = Vec2(p.x / density, p.y / density)

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val interruptedFling = camera.isFlinging
        camera.stopFling()

        val geo = geometry()
        val home = state()
        val origin = geo.origin(size.width.toFloat(), size.height.toFloat())
        val startHex = geo.hexAt(down.position, origin, camera.offset)
        // Touching a gliding map only stops it; it never launches anything.
        val startApp = if (interruptedFling) null else home.tiles[startHex]?.takeIf { it in home.apps }

        val tracker = VelocityTracker().apply { addPointerInputChange(down) }
        val samples = arrayListOf(GestureSample(down.uptimeMillis, dp(down.position)))

        // ---- Phase 1: tap, long press, or start of a movement. --------------------------------
        val first: FirstPhase? = if (startApp != null) {
            withTimeoutOrNull(config.longPressMs) { awaitSlopOrUp(down.id, down.position, slopPx, tracker, samples, ::dp) }
        } else {
            awaitSlopOrUp(down.id, down.position, slopPx, tracker, samples, ::dp)
        }

        when (first) {
            null -> {
                // Long press timeout fired while the finger stayed inside the slop.
                if (config.debugLog) Log.d(TAG, "LongPress $startApp")
                callbacks.onLongPress(startApp!!)
                return@awaitEachGesture
            }
            FirstPhase.Abort -> return@awaitEachGesture
            FirstPhase.Tap -> {
                if (startApp != null) {
                    if (config.debugLog) Log.d(TAG, "Tap $startApp")
                    callbacks.onTap(startApp, geo.boundsOf(startHex, origin, camera.offset))
                }
                return@awaitEachGesture
            }
            FirstPhase.Moved -> Unit
        }

        // ---- Phase 2a: map drag from an empty tile. -------------------------------------------
        if (startApp == null) {
            var last = down.position
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                tracker.addPointerInputChange(change)
                camera.moveBy(change.position - last)
                last = change.position
                change.consume()
                if (change.changedToUp() || !change.pressed) {
                    val v = tracker.calculateVelocity()
                    if (config.debugLog) Log.d(TAG, "Pan release v=(${v.x / density}, ${v.y / density})dp/s")
                    callbacks.onFling(Offset(v.x, v.y))
                    break
                }
            }
            return@awaitEachGesture
        }

        // ---- Phase 2b: flick-or-swipe from an app tile. ---------------------------------------
        try {
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.count { it.pressed } > 1) {
                    if (config.debugLog) Log.d(TAG, "Cancel: multi-touch")
                    return@awaitEachGesture
                }
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                tracker.addPointerInputChange(change)
                samples.add(GestureSample(change.uptimeMillis, dp(change.position)))
                change.consume()

                if (change.changedToUp() || !change.pressed) {
                    val v = tracker.calculateVelocity()
                    val outcome = classifier.classify(samples, Vec2(v.x / density, v.y / density))
                    callbacks.onPreview(null)
                    when (outcome) {
                        is SwipeOutcome.Flick -> callbacks.onFling(Offset(v.x, v.y))
                        is SwipeOutcome.Swipe -> {
                            val target = state().target(startApp, outcome.direction)
                            if (target != null) {
                                val targetHex = startHex.neighbor(outcome.direction)
                                callbacks.onSwipe(target.packageName, geo.boundsOf(targetHex, origin, camera.offset))
                            } else if (config.debugLog) {
                                Log.d(TAG, "Swipe ${outcome.direction}: nothing assigned")
                            }
                        }
                        is SwipeOutcome.Cancel -> Unit
                    }
                    return@awaitEachGesture
                }

                val v = tracker.calculateVelocity()
                val speedDp = Vec2(v.x, v.y).length() / density
                val direction = classifier.preview(samples.first(), samples.last(), speedDp)
                callbacks.onPreview(
                    direction?.let { SwipePreview(startHex, it, state().assignments[startApp]?.get(it)) }
                )
            }
        } finally {
            callbacks.onPreview(null)
        }
    }
}

/**
 * Waits until the pointer leaves the touch slop ([FirstPhase.Moved]), is released inside it
 * ([FirstPhase.Tap]), or a second finger / cancellation happens ([FirstPhase.Abort]).
 */
private suspend fun AwaitPointerEventScope.awaitSlopOrUp(
    id: PointerId,
    start: Offset,
    slopPx: Float,
    tracker: VelocityTracker,
    samples: MutableList<GestureSample>,
    dp: (Offset) -> Vec2,
): FirstPhase {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.count { it.pressed } > 1) return FirstPhase.Abort
        val change: PointerInputChange = event.changes.firstOrNull { it.id == id } ?: return FirstPhase.Abort
        tracker.addPointerInputChange(change)
        samples.add(GestureSample(change.uptimeMillis, dp(change.position)))
        if (change.changedToUp()) return FirstPhase.Tap
        if (change.isConsumed) return FirstPhase.Abort
        if ((change.position - start).getDistance() > slopPx) return FirstPhase.Moved
    }
}
