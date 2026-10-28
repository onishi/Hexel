package com.hexel.core.gesture

import com.hexel.core.hex.HexDirection
import com.hexel.core.hex.Vec2

/** One pointer position, in dp relative to an arbitrary fixed origin. */
data class GestureSample(val timeMs: Long, val position: Vec2)

/** Outcome of a gesture that started on an app tile and moved beyond the touch slop. */
sealed interface SwipeOutcome {
    /** Scroll the map with the given release velocity (dp/s). */
    data class Flick(val velocity: Vec2) : SwipeOutcome

    /** Launch whatever is assigned to [direction] (possibly nothing). */
    data class Swipe(val direction: HexDirection) : SwipeOutcome

    /** Do nothing. */
    data class Cancel(val reason: String) : SwipeOutcome
}

/** Metrics that the decision was based on; exposed for logging and tests. */
data class GestureMetrics(
    val durationMs: Long,
    val displacement: Vec2,
    val distance: Float,
    val maxDistance: Float,
    val pathLength: Float,
    val releaseSpeed: Float,
) {
    override fun toString(): String =
        "dur=${durationMs}ms dist=${"%.1f".format(distance)}dp max=${"%.1f".format(maxDistance)}dp " +
            "path=${"%.1f".format(pathLength)}dp v=${"%.0f".format(releaseSpeed)}dp/s"
}

fun interface GestureLogger {
    fun log(message: String)
}

/**
 * Pure decision logic for gestures that start on an app tile. Gestures that start on an empty tile
 * are always map drags and never reach the classifier.
 */
class GestureClassifier(
    val config: GestureConfig = GestureConfig(),
    private val logger: GestureLogger? = null,
) {

    /**
     * Direction to highlight while the finger is still down, or null for no highlight.
     * [currentSpeed] is the instantaneous pointer speed in dp/s.
     */
    fun preview(start: GestureSample, current: GestureSample, currentSpeed: Float): HexDirection? {
        val elapsed = current.timeMs - start.timeMs
        if (elapsed < config.previewDelayMs) return null
        if (currentSpeed >= config.swipeMaxReleaseVelocityDpPerSec) return null
        val d = current.position - start.position
        val dist = d.length()
        if (dist < config.swipeMinDistanceDp || dist > config.swipeMaxDistanceDp) return null
        return HexDirection.fromScreenVector(d.x, d.y)
    }

    /**
     * Classifies a finished gesture. [samples] must contain at least the down and up positions in
     * chronological order; [releaseVelocity] is in dp/s.
     */
    fun classify(samples: List<GestureSample>, releaseVelocity: Vec2): SwipeOutcome {
        require(samples.isNotEmpty())
        val metrics = metricsOf(samples, releaseVelocity)
        val outcome = decide(metrics, releaseVelocity)
        logger?.takeIf { config.debugLog }?.log("$outcome  $metrics")
        return outcome
    }

    fun metricsOf(samples: List<GestureSample>, releaseVelocity: Vec2): GestureMetrics {
        val first = samples.first()
        val last = samples.last()
        var maxDistance = 0f
        var path = 0f
        for (i in samples.indices) {
            maxDistance = maxOf(maxDistance, (samples[i].position - first.position).length())
            if (i > 0) path += (samples[i].position - samples[i - 1].position).length()
        }
        val displacement = last.position - first.position
        return GestureMetrics(
            durationMs = last.timeMs - first.timeMs,
            displacement = displacement,
            distance = displacement.length(),
            maxDistance = maxDistance,
            pathLength = path,
            releaseSpeed = releaseVelocity.length(),
        )
    }

    private fun decide(m: GestureMetrics, releaseVelocity: Vec2): SwipeOutcome {
        val c = config
        // 1. Fast and short: flick.
        if (m.durationMs <= c.flickMaxDurationMs && m.releaseSpeed >= c.flickMinVelocityDpPerSec) {
            return SwipeOutcome.Flick(releaseVelocity)
        }
        // 2. Thrown at the end, whatever happened before: flick.
        if (m.releaseSpeed >= c.swipeMaxReleaseVelocityDpPerSec) {
            return SwipeOutcome.Flick(releaseVelocity)
        }
        // 3. Too quick to be a deliberate pull: treat as a (weak) flick.
        if (m.durationMs < c.swipeMinDurationMs) {
            return SwipeOutcome.Flick(releaseVelocity)
        }
        // 4. Deliberate pull. The finger must end in the swipe ring.
        if (m.distance < c.swipeMinDistanceDp) {
            return SwipeOutcome.Cancel(
                if (m.maxDistance >= c.swipeMinDistanceDp) "returned to start" else "too short"
            )
        }
        if (m.distance > c.swipeMaxDistanceDp) {
            return SwipeOutcome.Cancel("too far")
        }
        val direction = HexDirection.fromScreenVector(m.displacement.x, m.displacement.y)
            ?: return SwipeOutcome.Cancel("no direction")
        return SwipeOutcome.Swipe(direction)
    }
}
