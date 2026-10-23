package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.domain.fake.show
import com.takehome.drama.domain.fake.tag
import com.takehome.drama.domain.usecase.discovery.GetHomeFeedUseCase
import com.takehome.drama.domain.usecase.discovery.PreviewAutoplayPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFeedTest {

    private val homeFeed = GetHomeFeedUseCase()

    @Test
    fun `hero is looked up by id, not taken from the head of the list`() {
        val feed = homeFeed(catalog(listOf(show("a"), show("b"), show("c")), heroShowId = "b"), emptySet())
        assertEquals("b", feed.hero?.id)
    }

    @Test
    fun `also flowing drops the hero and sorts by views, including counts above Int range`() {
        val shows = listOf(show("hero"), show("small", viewCount = 10), show("huge", viewCount = 4_000_000_000L), show("mid", viewCount = 500))
        val feed = homeFeed(catalog(shows, heroShowId = "hero"), emptySet())
        assertEquals(listOf("huge", "mid", "small"), feed.alsoFlowing.map { it.id })
    }

    @Test
    fun `shows matching the taste come first, then views`() {
        val shows = listOf(
            show("popular", viewCount = 9_000),
            show("revenge-low", tagIds = listOf("revenge"), viewCount = 10),
            show("revenge-high", tagIds = listOf("revenge"), viewCount = 100),
        )
        val feed = homeFeed(catalog(shows, heroShowId = "none"), setOf("revenge"))
        // heroShowId trỏ sai → hero là show đầu, các show còn lại vẫn xếp theo gu.
        assertEquals("popular", feed.hero?.id)
        assertEquals(listOf("revenge-high", "revenge-low"), feed.alsoFlowing.map { it.id })
    }

    @Test
    fun `merchandising rails without shows are dropped, the rest follow orderPriority`() {
        val tags = listOf(
            tag("trending", isCategory = false, orderPriority = 10),
            tag("newest", isCategory = false, orderPriority = 5),
            tag("popular", isCategory = false, orderPriority = 20),
            tag("romance", isCategory = true, orderPriority = 1),
        )
        val shows = listOf(show("a", tagIds = listOf("popular", "romance")), show("b", tagIds = listOf("newest")))
        val feed = homeFeed(catalog(shows, tags = tags), emptySet())
        assertEquals(listOf("newest", "popular"), feed.rails.map { it.tag.id })
    }

    @Test
    fun `preview autoplays when wifi-only is off or the network is unmetered`() {
        assertTrue(PreviewAutoplayPolicy.shouldAutoplay(wifiOnly = false, isUnmetered = false))
        assertTrue(PreviewAutoplayPolicy.shouldAutoplay(wifiOnly = false, isUnmetered = true))
        assertTrue(PreviewAutoplayPolicy.shouldAutoplay(wifiOnly = true, isUnmetered = true))
        assertFalse(PreviewAutoplayPolicy.shouldAutoplay(wifiOnly = true, isUnmetered = false))
    }
}
