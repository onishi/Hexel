package com.hexel.core

import com.hexel.core.hex.HexCoord
import com.hexel.core.hex.HexSpiral
import com.hexel.core.layout.SlotAllocator
import com.hexel.core.model.HexApp
import org.junit.Assert.assertEquals
import org.junit.Test

class SlotAllocatorTest {

    @Test
    fun initialLayoutFollowsSpiral() {
        val apps = (0 until 10).map { "app$it" }
        val result = SlotAllocator.allocate(emptyList(), apps)
        val spiral = HexSpiral.sequence().take(10).toList()
        assertEquals(spiral, result.map { it.coord })
        assertEquals(apps, result.map { it.packageName })
    }

    @Test
    fun existingAppsKeepTheirSlots() {
        val first = SlotAllocator.allocate(emptyList(), listOf("a", "b", "c"))
        val second = SlotAllocator.allocate(first, listOf("0new", "a", "b", "c"))
        for (app in first) assertEquals(app, second.single { it.packageName == app.packageName })
        assertEquals(HexSpiral.sequence().elementAt(3), second.single { it.packageName == "0new" }.coord)
    }

    @Test
    fun uninstalledAppFreesSlotForNextNewApp() {
        val first = SlotAllocator.allocate(emptyList(), listOf("a", "b", "c"))
        val removed = SlotAllocator.allocate(first, listOf("a", "c"))
        assertEquals(listOf("a", "c"), removed.map { it.packageName })
        val added = SlotAllocator.allocate(removed, listOf("a", "c", "d"))
        val bSlot = first.single { it.packageName == "b" }.coord
        assertEquals(bSlot, added.single { it.packageName == "d" }.coord)
    }

    @Test
    fun corruptDuplicatesAreRepaired() {
        val broken = listOf(HexApp("a", 0, 0), HexApp("b", 0, 0))
        val fixed = SlotAllocator.allocate(broken, listOf("a", "b"))
        assertEquals(2, fixed.map { it.coord }.toSet().size)
        assertEquals(HexCoord.Origin, fixed.single { it.packageName == "a" }.coord)
    }
}
