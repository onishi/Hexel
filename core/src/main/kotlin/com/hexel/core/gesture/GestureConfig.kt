package com.hexel.core.gesture

/**
 * All thresholds used to tell a tap, a flick (scroll the map) and a swipe (launch an app) apart.
 *
 * Distances are in dp, velocities in dp/s, times in ms. The defaults are first guesses meant to be
 * tuned on a real device; enable debug logging and watch the `HexelGesture` tag while adjusting.
 *
 * Flick = fast and short  -> map scroll.
 * Swipe = deliberate pull -> launch the app assigned to the direction.
 * When in doubt the classifier prefers "flick" or "cancel" over "swipe", because a wrong scroll is
 * cheap while a wrong app launch is not.
 */
data class GestureConfig(
    /** Movement below this is still a tap / long press. */
    val touchSlopDp: Float = 10f,
    /** Holding still this long on an app opens the direction editor. */
    val longPressMs: Long = 450L,

    /** A release within this time of touch-down at [flickMinVelocityDpPerSec] or faster is a flick. */
    val flickMaxDurationMs: Long = 250L,
    val flickMinVelocityDpPerSec: Float = 650f,
    /** A release this fast is always a flick, however long the finger was down. */
    val swipeMaxReleaseVelocityDpPerSec: Float = 2000f,

    /** A gesture shorter than this is never a swipe (too quick to be a deliberate pull). */
    val swipeMinDurationMs: Long = 120L,
    /** The finger must end at least this far from where it started for a swipe. */
    val swipeMinDistanceDp: Float = 48f,
    /** Ending further than this away is treated as a failed map drag and cancelled. */
    val swipeMaxDistanceDp: Float = 240f,

    /** The direction highlight only appears after this long, so flicks never flash it. */
    val previewDelayMs: Long = 120L,

    /** Log every classification decision with its metrics. */
    val debugLog: Boolean = false,
)
