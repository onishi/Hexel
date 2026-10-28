package com.hexel.core.hex

/**
 * Spiral enumeration of hexes around the origin: index 0 is the origin, indices 1..6 form ring 1,
 * then ring 2 (12 tiles), and so on.
 */
object HexSpiral {

    /** Lazily yields hexes in spiral order, forever. */
    fun sequence(center: HexCoord = HexCoord.Origin): Sequence<HexCoord> = sequence {
        yield(center)
        var radius = 1
        while (true) {
            yieldAll(ring(center, radius))
            radius++
        }
    }

    /** The 6 * radius hexes at exactly [radius] steps from [center], walked in order. */
    fun ring(center: HexCoord, radius: Int): List<HexCoord> {
        require(radius > 0)
        val result = ArrayList<HexCoord>(6 * radius)
        // Start at the "south-west" corner of the ring and walk around it.
        var hex = HexCoord(center.q + HexDirection.SOUTH_WEST.offset.q * radius,
            center.r + HexDirection.SOUTH_WEST.offset.r * radius)
        for (side in WALK_ORDER) {
            repeat(radius) {
                result.add(hex)
                hex = hex.neighbor(side)
            }
        }
        return result
    }

    /** Number of rings needed to hold [count] tiles in spiral order (0 for a single tile). */
    fun ringsFor(count: Int): Int {
        var rings = 0
        var capacity = 1
        while (capacity < count) {
            rings++
            capacity += 6 * rings
        }
        return rings
    }

    private val WALK_ORDER = listOf(
        HexDirection.EAST,
        HexDirection.NORTH_EAST,
        HexDirection.NORTH_WEST,
        HexDirection.WEST,
        HexDirection.SOUTH_WEST,
        HexDirection.SOUTH_EAST,
    )
}
