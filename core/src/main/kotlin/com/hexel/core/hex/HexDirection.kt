package com.hexel.core.hex

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor

/**
 * The six neighbour directions of a pointy-top hexagon, in counter-clockwise order
 * starting from east. Each direction points at the middle of one hexagon edge.
 *
 * [angleDegrees] is the mathematical angle (0° = right, 90° = up on screen).
 * [offset] is the axial offset to the neighbouring tile in that direction.
 */
enum class HexDirection(val angleDegrees: Int, val offset: HexCoord) {
    EAST(0, HexCoord(1, 0)),
    NORTH_EAST(60, HexCoord(1, -1)),
    NORTH_WEST(120, HexCoord(0, -1)),
    WEST(180, HexCoord(-1, 0)),
    SOUTH_WEST(240, HexCoord(-1, 1)),
    SOUTH_EAST(300, HexCoord(0, 1));

    companion object {
        /**
         * Classifies a screen-space displacement (x right, y down) into one of six 60° sectors.
         * Returns null for a zero vector.
         */
        fun fromScreenVector(dx: Float, dy: Float): HexDirection? {
            if (dx == 0f && dy == 0f) return null
            // Flip y so that "up on screen" is a positive angle.
            var deg = atan2(-dy.toDouble(), dx.toDouble()) * 180.0 / PI
            if (deg < 0) deg += 360.0
            val sector = floor((deg + 30.0) / 60.0).toInt() % 6
            return entries[sector]
        }
    }
}
