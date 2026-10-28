package com.hexel.launcher.ui.home

import androidx.compose.animation.core.FloatDecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Map scroll position in pixels: a world point `w` is drawn at `origin + offset + w`.
 * Lives in the ViewModel so the position survives app launches and configuration changes.
 */
@Stable
class MapCamera {

    var offset by mutableStateOf(Offset.Zero)
        private set

    /** Maximum |offset| per axis, so the outermost apps can reach the screen centre but no further. */
    var limit: Offset = Offset.Zero
        set(value) {
            field = value
            offset = clamp(offset)
        }

    private var flingJob: Job? = null

    val isFlinging: Boolean get() = flingJob?.isActive == true

    fun stopFling() {
        flingJob?.cancel()
        flingJob = null
    }

    fun moveBy(delta: Offset) {
        offset = clamp(offset + delta)
    }

    /** Inertial scroll with the given velocity (px/s). Each axis stops on its own at the bounds. */
    fun fling(scope: CoroutineScope, velocity: Offset, decay: FloatDecayAnimationSpec) {
        stopFling()
        flingJob = scope.launch {
            launch {
                flingAxis(offset.x, velocity.x, decay) { x ->
                    offset = clamp(Offset(x, offset.y))
                    offset.x == x
                }
            }
            launch {
                flingAxis(offset.y, velocity.y, decay) { y ->
                    offset = clamp(Offset(offset.x, y))
                    offset.y == y
                }
            }
        }
    }

    private suspend fun flingAxis(
        start: Float,
        velocity: Float,
        decay: FloatDecayAnimationSpec,
        apply: (Float) -> Boolean,
    ) {
        if (abs(velocity) < MIN_FLING_VELOCITY) return
        val job = currentCoroutineContext()[Job]
        animateDecay(start, velocity, decay) { value, _ ->
            // Stop this axis as soon as it hits the boundary.
            if (!apply(value)) job?.cancel()
        }
    }

    private fun clamp(o: Offset) = Offset(
        o.x.coerceIn(-limit.x, limit.x),
        o.y.coerceIn(-limit.y, limit.y),
    )

    private companion object {
        const val MIN_FLING_VELOCITY = 50f
    }
}
