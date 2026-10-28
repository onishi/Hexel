package com.hexel.core.hex

import kotlin.math.sqrt

/** Minimal 2D vector so the core module stays free of Android / Compose types. */
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    operator fun div(s: Float) = Vec2(x / s, y / s)
    fun length(): Float = sqrt(x * x + y * y)

    companion object {
        val Zero = Vec2(0f, 0f)
    }
}
