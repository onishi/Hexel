package com.hexel.core.model

import com.hexel.core.hex.HexCoord

/** Placement of one app on the hex map. */
data class HexApp(
    val packageName: String,
    val q: Int,
    val r: Int,
) {
    val coord: HexCoord get() = HexCoord(q, r)
}
