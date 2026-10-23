package com.takehome.drama.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreloadTargetTest {

    private val normal = MediaProfile.forTier(DeviceTier.NORMAL)
    private val low = MediaProfile.forTier(DeviceTier.LOW)

    @Test
    fun `normal device - next 5s, previous 2s, +2 only tracks, further nothing`() {
        assertEquals(PreloadTarget.Range(5_000), preloadTarget(distance = 1, isLocked = false, profile = normal))
        assertEquals(PreloadTarget.Range(2_000), preloadTarget(distance = -1, isLocked = false, profile = normal))
        assertEquals(PreloadTarget.TracksSelected, preloadTarget(distance = 2, isLocked = false, profile = normal))
        assertNull(preloadTarget(distance = 3, isLocked = false, profile = normal))
        assertNull(preloadTarget(distance = -2, isLocked = false, profile = normal))
    }

    @Test
    fun `low device - only the next episode, shorter`() {
        assertEquals(PreloadTarget.Range(2_500), preloadTarget(distance = 1, isLocked = false, profile = low))
        assertNull(preloadTarget(distance = -1, isLocked = false, profile = low))
        assertNull(preloadTarget(distance = 2, isLocked = false, profile = low))
    }

    @Test
    fun `locked episode is never preloaded`() {
        (-2..3).forEach { assertNull(preloadTarget(distance = it, isLocked = true, profile = normal)) }
    }

    @Test
    fun `current episode is the player's job, not the preload manager's`() {
        assertNull(preloadTarget(distance = 0, isLocked = false, profile = normal))
    }
}
