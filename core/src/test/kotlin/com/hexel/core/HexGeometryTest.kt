package com.hexel.core

import com.hexel.core.hex.HexCoord
import com.hexel.core.hex.HexDirection
import com.hexel.core.hex.HexLayout
import com.hexel.core.hex.HexSpiral
import com.hexel.core.hex.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class HexGeometryTest {

    private val layout = HexLayout(radius = 44f)

    @Test
    fun centreMatchesSpecFormula() {
        val p = layout.hexToWorld(HexCoord(2, -3))
        assertEquals(44f * sqrt(3f) * (2 - 1.5f), p.x, 0.01f)
        assertEquals(44f * 1.5f * -3, p.y, 0.01f)
    }

    @Test
    fun worldToHexRoundTripsCentres() {
        for (q in -6..6) for (r in -6..6) {
            val hex = HexCoord(q, r)
            assertEquals(hex, layout.worldToHex(layout.hexToWorld(hex)))
        }
    }

    @Test
    fun pointsInsideHexMapToThatHex() {
        val hex = HexCoord(3, -2)
        val c = layout.hexToWorld(hex)
        // Inscribed circle radius = r * sqrt(3) / 2; stay slightly inside it.
        val inner = 44f * sqrt(3f) / 2f - 0.5f
        for (deg in 0 until 360 step 7) {
            val rad = Math.toRadians(deg.toDouble())
            val p = Vec2(c.x + inner * cos(rad).toFloat(), c.y + inner * sin(rad).toFloat())
            assertEquals("deg=$deg", hex, layout.worldToHex(p))
        }
    }

    @Test
    fun neighboursAreAdjacentOnScreenInTheirDirection() {
        val origin = layout.hexToWorld(HexCoord.Origin)
        for (dir in HexDirection.entries) {
            val n = layout.hexToWorld(HexCoord.Origin.neighbor(dir))
            val d = n - origin
            assertEquals(layout.width, d.length(), 0.01f)
            assertEquals(dir, HexDirection.fromScreenVector(d.x, d.y))
        }
    }

    @Test
    fun directionSectors() {
        assertEquals(HexDirection.EAST, HexDirection.fromScreenVector(10f, 0f))
        assertEquals(HexDirection.EAST, HexDirection.fromScreenVector(10f, -5f)) // ~27° up
        assertEquals(HexDirection.NORTH_EAST, HexDirection.fromScreenVector(10f, -7f)) // ~35° up
        assertEquals(HexDirection.NORTH_WEST, HexDirection.fromScreenVector(-5f, -10f))
        assertEquals(HexDirection.WEST, HexDirection.fromScreenVector(-10f, 1f))
        assertEquals(HexDirection.SOUTH_WEST, HexDirection.fromScreenVector(-5f, 10f))
        assertEquals(HexDirection.SOUTH_EAST, HexDirection.fromScreenVector(5f, 10f))
        assertEquals(HexDirection.EAST, HexDirection.fromScreenVector(10f, 5f)) // ~27° down
        assertNull(HexDirection.fromScreenVector(0f, 0f))
    }

    @Test
    fun edgeCornersAreOnTheFacingSide() {
        for (dir in HexDirection.entries) {
            val (a, b) = layout.edgeCorners(dir)
            val mid = (a + b) / 2f
            assertEquals(dir, HexDirection.fromScreenVector(mid.x, mid.y))
            assertEquals(44f, (a - b).length(), 0.01f)
        }
    }

    @Test
    fun spiralIsUniqueAndRingOrdered() {
        val first = HexSpiral.sequence().take(1 + 6 + 12 + 18).toList()
        assertEquals(first.size, first.toSet().size)
        assertEquals(HexCoord.Origin, first[0])
        first.forEachIndexed { i, hex ->
            val ring = when {
                i == 0 -> 0
                i <= 6 -> 1
                i <= 18 -> 2
                else -> 3
            }
            assertEquals(ring, hex.distanceTo(HexCoord.Origin))
        }
        // Consecutive tiles inside a ring touch each other.
        val ring2 = HexSpiral.ring(HexCoord.Origin, 2)
        for (i in ring2.indices) {
            assertEquals(1, ring2[i].distanceTo(ring2[(i + 1) % ring2.size]))
        }
    }

    @Test
    fun ringsFor() {
        assertEquals(0, HexSpiral.ringsFor(1))
        assertEquals(1, HexSpiral.ringsFor(7))
        assertEquals(2, HexSpiral.ringsFor(8))
        assertEquals(2, HexSpiral.ringsFor(19))
    }

    @Test
    fun visibleRectCoversEveryPointInside() {
        val left = -300f; val top = -500f; val right = 250f; val bottom = 420f
        val hexes = layout.hexesInRect(left, top, right, bottom).toSet()
        var y = top
        while (y <= bottom) {
            var x = left
            while (x <= right) {
                assertTrue(layout.worldToHex(Vec2(x, y)) in hexes)
                x += 9f
            }
            y += 9f
        }
    }
}
