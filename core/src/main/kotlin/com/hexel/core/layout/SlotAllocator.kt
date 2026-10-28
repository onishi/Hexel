package com.hexel.core.layout

import com.hexel.core.hex.HexCoord
import com.hexel.core.hex.HexSpiral
import com.hexel.core.model.HexApp

/**
 * Decides where each installed app lives on the map.
 *
 * - Apps that already have a slot keep it, so the map stays spatially stable.
 * - Uninstalled apps release their slot (the tile becomes empty).
 * - New apps take the free slots closest to the centre, in spiral order, in the given order.
 */
object SlotAllocator {

    fun allocate(existing: List<HexApp>, installedInOrder: List<String>): List<HexApp> {
        val installed = installedInOrder.toSet()
        val kept = existing
            .filter { it.packageName in installed }
            .distinctBy { it.packageName }
            .distinctBy { it.coord }
        val placed = kept.mapTo(HashSet()) { it.packageName }
        val occupied = kept.mapTo(HashSet()) { it.coord }

        val newcomers = installedInOrder.distinct().filter { it !in placed }
        if (newcomers.isEmpty()) return kept

        val result = kept.toMutableList()
        val freeSlots = HexSpiral.sequence().filter { it !in occupied }.iterator()
        for (pkg in newcomers) {
            val slot: HexCoord = freeSlots.next()
            result.add(HexApp(pkg, slot.q, slot.r))
        }
        return result
    }
}
