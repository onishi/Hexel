package com.hexel.core

import com.hexel.core.gesture.GestureClassifier
import com.hexel.core.gesture.GestureSample
import com.hexel.core.gesture.SwipeOutcome
import com.hexel.core.hex.HexDirection
import com.hexel.core.hex.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureClassifierTest {

    private val classifier = GestureClassifier()

    /** Straight line from the origin to [end] over [durationMs], sampled every 8 ms. */
    private fun line(end: Vec2, durationMs: Long): List<GestureSample> =
        (0..durationMs step 8).map { t -> GestureSample(t, end * (t.toFloat() / durationMs)) } +
            GestureSample(durationMs, end)

    @Test
    fun quickFlickScrolls() {
        val outcome = classifier.classify(line(Vec2(0f, -90f), 110), Vec2(0f, -1400f))
        assertTrue(outcome is SwipeOutcome.Flick)
    }

    @Test
    fun deliberatePullLaunches() {
        val outcome = classifier.classify(line(Vec2(70f, 0f), 380), Vec2(40f, 0f))
        assertEquals(SwipeOutcome.Swipe(HexDirection.EAST), outcome)
    }

    @Test
    fun pullUpLeftIsNorthWest() {
        val outcome = classifier.classify(line(Vec2(-35f, -60f), 400), Vec2.Zero)
        assertEquals(SwipeOutcome.Swipe(HexDirection.NORTH_WEST), outcome)
    }

    @Test
    fun pullThenReturnCancels() {
        val out = line(Vec2(80f, 0f), 300)
        val back = line(Vec2(-75f, 0f), 300).map {
            GestureSample(it.timeMs + 300, Vec2(80f, 0f) + it.position)
        }
        val outcome = classifier.classify(out + back, Vec2(-50f, 0f))
        assertEquals(SwipeOutcome.Cancel("returned to start"), outcome)
    }

    @Test
    fun slowShortMoveCancels() {
        val outcome = classifier.classify(line(Vec2(20f, 0f), 400), Vec2(10f, 0f))
        assertTrue(outcome is SwipeOutcome.Cancel)
    }

    @Test
    fun longDragCancels() {
        val outcome = classifier.classify(line(Vec2(0f, 400f), 900), Vec2(0f, 100f))
        assertEquals(SwipeOutcome.Cancel("too far"), outcome)
    }

    @Test
    fun throwAtEndOfSlowPullIsFlick() {
        val outcome = classifier.classify(line(Vec2(100f, 0f), 500), Vec2(2600f, 0f))
        assertTrue(outcome is SwipeOutcome.Flick)
    }

    @Test
    fun veryQuickJabIsNeverASwipe() {
        val outcome = classifier.classify(line(Vec2(60f, 0f), 80), Vec2(300f, 0f))
        assertTrue(outcome is SwipeOutcome.Flick)
    }

    @Test
    fun previewOnlyAfterDelayAndInRing() {
        val start = GestureSample(0, Vec2.Zero)
        assertNull(classifier.preview(start, GestureSample(60, Vec2(70f, 0f)), 100f))
        assertNull(classifier.preview(start, GestureSample(300, Vec2(20f, 0f)), 100f))
        assertNull(classifier.preview(start, GestureSample(300, Vec2(70f, 0f)), 3000f))
        assertEquals(HexDirection.EAST, classifier.preview(start, GestureSample(300, Vec2(70f, 0f)), 100f))
        assertEquals(HexDirection.SOUTH_WEST, classifier.preview(start, GestureSample(300, Vec2(-30f, 55f)), 100f))
    }
}
