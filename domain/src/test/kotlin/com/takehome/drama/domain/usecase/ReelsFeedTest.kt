package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.domain.fake.episode
import com.takehome.drama.domain.fake.show
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.usecase.reels.GetReelClipUseCase
import com.takehome.drama.domain.usecase.reels.GetReelsFeedUseCase
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import com.takehome.drama.domain.usecase.watch.PickVideoSourceUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReelsFeedTest {

    private val repository = FakeCatalogRepository(Result.failure(Exception()))
    private val feed = GetReelsFeedUseCase()
    private val clip = GetReelClipUseCase(GetShowEpisodesUseCase(repository), PickVideoSourceUseCase())

    @Test
    fun `feed has one reel per show, taste first then views`() {
        val shows = listOf(show("a", viewCount = 5), show("b", viewCount = 50), show("c", tagIds = listOf("revenge"), viewCount = 1))
        assertEquals(listOf("c", "b", "a"), feed(catalog(shows), setOf("revenge")).map { it.id })
    }

    @Test
    fun `clip is the first free episode in array order and counts the episode file`() {
        // 13077: catalogue ghi 65 tập, file có 50 — "Watch all N" phải là 50.
        val episodes = ShowEpisodes(
            "13077",
            listOf(episode(index = 3, isFree = false, number = 1), episode(index = 7, number = 9)) +
                List(48) { episode(index = 10 + it, isFree = false) },
        )
        val clip = clip.clipOf(episodes, language = "en", defaultLanguage = "en")!!
        assertEquals(7, clip.episode.index)
        assertEquals("https://e/7.mp4", clip.url)
        assertEquals(50, clip.episodeTotal)
    }

    @Test
    fun `free episodes without a playable source are skipped`() {
        val episodes = ShowEpisodes("s", listOf(episode(index = 0, sources = emptyMap()), episode(index = 1)))
        assertEquals(1, clip.clipOf(episodes, "en", "en")?.episode?.index)
    }

    @Test
    fun `audio follows the content language, falling back to the default`() {
        val sources = mapOf("en" to mapOf("720p" to "en.mp4"), "ar" to mapOf("720p" to "ar.mp4"))
        val episodes = ShowEpisodes("s", listOf(episode(index = 0, sources = sources)))
        assertEquals("ar.mp4", clip.clipOf(episodes, "ar", "en")?.url)
        assertEquals("en.mp4", clip.clipOf(episodes, "ta", "en")?.url)
    }

    @Test
    fun `a show with no free playable episode has no reel`() {
        val episodes = ShowEpisodes("s", listOf(episode(index = 0, isFree = false), episode(index = 1, isFree = false)))
        assertNull(clip.clipOf(episodes, "en", "en"))
        assertNull(clip.clipOf(ShowEpisodes("s", emptyList()), "en", "en"))
    }
}
