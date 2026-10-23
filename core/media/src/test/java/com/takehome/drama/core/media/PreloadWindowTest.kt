package com.takehome.drama.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class PreloadWindowTest {

    private val normal = MediaProfile.forTier(DeviceTier.NORMAL)
    private val low = MediaProfile.forTier(DeviceTier.LOW)
    private val allFree: (Int) -> Boolean = { true }

    @Test
    fun `normal window is one behind and two ahead, current excluded`() {
        assertEquals(setOf(4, 6, 7), preloadWindow(current = 5, size = 10, profile = normal, isPreloadable = allFree))
    }

    @Test
    fun `window is clipped at both ends`() {
        assertEquals(setOf(1, 2), preloadWindow(current = 0, size = 10, profile = normal, isPreloadable = allFree))
        assertEquals(setOf(8), preloadWindow(current = 9, size = 10, profile = normal, isPreloadable = allFree))
        assertEquals(emptySet<Int>(), preloadWindow(current = 0, size = 1, profile = normal, isPreloadable = allFree))
    }

    @Test
    fun `low device window is only the next one`() {
        assertEquals(setOf(6), preloadWindow(current = 5, size = 10, profile = low, isPreloadable = allFree))
    }

    @Test
    fun `locked episodes are left out`() {
        assertEquals(setOf(4), preloadWindow(current = 5, size = 10, profile = normal, isPreloadable = { it < 6 }))
    }

    @Test
    fun `moving the current episode removes what fell out of the window`() {
        val before = preloadWindow(current = 2, size = 10, profile = normal, isPreloadable = allFree)
        val after = preloadWindow(current = 3, size = 10, profile = normal, isPreloadable = allFree)
        val diff = PreloadWindowDiff.between(before, after)
        assertEquals(setOf(1, 3), diff.toRemove)
        assertEquals(setOf(2, 5), diff.toAdd)
    }
}
