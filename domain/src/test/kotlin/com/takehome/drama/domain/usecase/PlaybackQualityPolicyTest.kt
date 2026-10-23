package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.usecase.watch.PlaybackQualityPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQualityPolicyTest {

    @Test
    fun `drops to the nearest lower real quality`() {
        assertEquals("720p", PlaybackQualityPolicy.downgradeTarget("1080p", listOf("480p", "1080p", "720p")))
    }

    @Test
    fun `nothing lower - no downgrade, never invent 480p`() {
        assertNull(PlaybackQualityPolicy.downgradeTarget("720p", listOf("720p")))
    }

    @Test
    fun `unknown keys are never a downgrade target, unknown current is left alone`() {
        assertEquals("480p", PlaybackQualityPolicy.downgradeTarget("720p", listOf("abc", "720p", "480p")))
        assertNull(PlaybackQualityPolicy.downgradeTarget("720p", listOf("abc", "720p")))
        assertNull(PlaybackQualityPolicy.downgradeTarget("abc", listOf("abc", "720p")))
    }

    @Test
    fun `only downgrades in Auto after a long stall`() {
        assertTrue(PlaybackQualityPolicy.shouldDowngrade(isAuto = true, bufferingMs = 5_001))
        assertFalse(PlaybackQualityPolicy.shouldDowngrade(isAuto = true, bufferingMs = 4_000))
        assertFalse(PlaybackQualityPolicy.shouldDowngrade(isAuto = false, bufferingMs = 60_000))
    }
}
