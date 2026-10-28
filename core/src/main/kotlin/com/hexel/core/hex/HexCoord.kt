package com.hexel.core.hex

import kotlin.math.abs
import kotlin.math.max

/** Axial hex coordinate (q, r). The implicit cube coordinate is s = -q - r. */
data class HexCoord(val q: Int, val r: Int) {
    val s: Int get() = -q - r

    operator fun plus(o: HexCoord) = HexCoord(q + o.q, r + o.r)

    fun neighbor(direction: HexDirection): HexCoord = this + direction.offset

    fun distanceTo(o: HexCoord): Int = max(max(abs(q - o.q), abs(r - o.r)), abs(s - o.s))

    companion object {
        val Origin = HexCoord(0, 0)
    }
}
