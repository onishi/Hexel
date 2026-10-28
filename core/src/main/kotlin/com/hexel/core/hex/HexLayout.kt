package com.hexel.core.hex

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val SQRT3 = sqrt(3.0)

/**
 * Pointy-top hex geometry. [radius] is the centre-to-vertex distance in whatever unit the
 * caller uses (pixels on screen). World coordinates have (0,0) at the centre of hex (0,0),
 * x to the right and y downwards.
 */
class HexLayout(val radius: Float) {

    /** Horizontal distance between neighbouring centres in a row (= hex width). */
    val width: Float get() = (SQRT3 * radius).toFloat()

    /** Vertical distance between rows. */
    val rowHeight: Float get() = 1.5f * radius

    fun hexToWorld(hex: HexCoord): Vec2 = Vec2(
        x = (radius * SQRT3 * (hex.q + hex.r / 2.0)).toFloat(),
        y = (radius * 1.5 * hex.r).toFloat(),
    )

    /** Returns the hex containing the given world point. */
    fun worldToHex(point: Vec2): HexCoord {
        val q = (SQRT3 / 3.0 * point.x - 1.0 / 3.0 * point.y) / radius
        val r = (2.0 / 3.0 * point.y) / radius
        return cubeRound(q, r)
    }

    /** The six corner positions relative to the hex centre, clockwise on screen. */
    fun corners(): List<Vec2> = (0 until 6).map { i -> cornerAt(-30.0 - 60.0 * i) }

    /**
     * The two corners (relative to the centre) of the edge that faces [direction].
     */
    fun edgeCorners(direction: HexDirection): Pair<Vec2, Vec2> {
        val a = direction.angleDegrees.toDouble()
        return cornerAt(a - 30.0) to cornerAt(a + 30.0)
    }

    /** Corner at the given mathematical angle (y up), converted to screen space (y down). */
    private fun cornerAt(mathDegrees: Double): Vec2 {
        val rad = Math.toRadians(mathDegrees)
        return Vec2(
            x = (radius * kotlin.math.cos(rad)).toFloat(),
            y = (-radius * kotlin.math.sin(rad)).toFloat(),
        )
    }

    /**
     * All hexes whose area may intersect the axis-aligned world rectangle. Used to draw only the
     * tiles around the visible viewport. Over-includes by at most one tile at each border.
     */
    fun hexesInRect(left: Float, top: Float, right: Float, bottom: Float): List<HexCoord> {
        val rMin = floor((top - radius) / rowHeight).toInt()
        val rMax = ceil((bottom + radius) / rowHeight).toInt()
        val result = ArrayList<HexCoord>()
        val w = width
        for (r in rMin..rMax) {
            // Centre x = w * (q + r/2)  ->  q = x / w - r / 2
            val qMin = floor((left - w / 2f) / w - r / 2f).toInt()
            val qMax = ceil((right + w / 2f) / w - r / 2f).toInt()
            for (q in qMin..qMax) result.add(HexCoord(q, r))
        }
        return result
    }

    private fun cubeRound(fq: Double, fr: Double): HexCoord {
        val fs = -fq - fr
        var q = fq.roundToInt()
        var r = fr.roundToInt()
        val s = fs.roundToInt()
        val dq = abs(q - fq)
        val dr = abs(r - fr)
        val ds = abs(s - fs)
        if (dq > dr && dq > ds) {
            q = -r - s
        } else if (dr > ds) {
            r = -q - s
        }
        return HexCoord(q, r)
    }
}
