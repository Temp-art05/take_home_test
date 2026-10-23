package com.takehome.drama.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class QualityTest {

    @Test
    fun `sorts high to low with unknown keys last`() {
        val sorted = listOf("abc", "720p", "2160p", "1080p", "hd").sortedWith(Quality.HighestFirst)
        assertEquals(listOf("2160p", "1080p", "720p", "abc", "hd"), sorted)
    }
}
