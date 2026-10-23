package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.fake.episode
import com.takehome.drama.domain.fake.show
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.VideoSource
import com.takehome.drama.domain.model.episodeTotal
import com.takehome.drama.domain.usecase.watch.PickVideoSourceUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PickVideoSourceUseCaseTest {

    private val pickSource = PickVideoSourceUseCase()

    private val multi = episode(
        index = 0,
        sources = mapOf(
            "en" to mapOf("720p" to "en720", "1080p" to "en1080", "abc" to "enAbc"),
            "ar" to mapOf("480p" to "ar480"),
        ),
    )

    @Test
    fun `picks viewer language with highest quality by default`() {
        assertEquals(VideoSource("en", "1080p", "en1080"), pickSource(multi, language = "en", defaultLanguage = "en"))
        assertEquals(VideoSource("ar", "480p", "ar480"), pickSource(multi, language = "ar", defaultLanguage = "en"))
    }

    @Test
    fun `missing viewer language falls back to default language`() {
        assertEquals("en", pickSource(multi, language = "ta", defaultLanguage = "en")?.language)
    }

    @Test
    fun `region tag matches the base language key like LocalizedText does`() {
        assertEquals("ar", pickSource(multi, language = "ar-EG", defaultLanguage = "en")?.language)
    }

    @Test
    fun `missing default language falls back to any language`() {
        val onlyHindi = episode(0, sources = mapOf("hi" to mapOf("720p" to "hi720")))
        assertEquals(VideoSource("hi", "720p", "hi720"), pickSource(onlyHindi, language = "ar", defaultLanguage = "en"))
    }

    @Test
    fun `requested quality is used when present, otherwise highest`() {
        assertEquals("en720", pickSource(multi, "en", "en", preferredQuality = "720p")?.url)
        assertEquals("en1080", pickSource(multi, "en", "en", preferredQuality = "2160p")?.url)
    }

    @Test
    fun `episode without any source yields null instead of crashing`() {
        assertNull(pickSource(episode(0, sources = emptyMap()), "en", "en"))
    }

    @Test
    fun `qualities come from sources, high to low, unknown keys last`() {
        assertEquals(listOf("1080p", "720p", "abc"), pickSource.qualitiesOf(multi, language = "en", defaultLanguage = "en"))
        assertEquals(listOf("480p"), pickSource.qualitiesOf(multi, language = "ar", defaultLanguage = "en"))
    }

    @Test
    fun `grid trusts the episode file, catalogue count only before it is loaded`() {
        val show = show("13077", episodeCount = 65)
        val loaded = ShowEpisodes("13077", List(50) { episode(it) })
        assertEquals(50, show.episodeTotal(loaded))
        assertEquals(65, show.episodeTotal(episodes = null))
    }
}
