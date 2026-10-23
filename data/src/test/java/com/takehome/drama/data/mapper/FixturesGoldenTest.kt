package com.takehome.drama.data.mapper

import com.takehome.drama.data.remote.dto.CatalogDto
import com.takehome.drama.data.remote.dto.ShowEpisodesDto
import com.takehome.drama.data.remote.http.defaultJson
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.ShowStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parse đúng bản fixtures nhúng trong flavor dev (= payload thật của API). */
class FixturesGoldenTest {

    private fun read(path: String): String =
        requireNotNull(javaClass.classLoader?.getResource("fixtures/$path")) { "missing fixtures/$path" }.readText()

    private val catalog = defaultJson.decodeFromString<CatalogDto>(read("catalog.json")).toDomain()

    private val episodes: Map<String, ShowEpisodes> = catalog.shows.associate { show ->
        show.id to defaultJson.decodeFromString<ShowEpisodesDto>(read("shows/${show.id}.json")).toDomain()
    }

    @Test
    fun `catalog shape`() {
        assertEquals("en", catalog.defaultLanguage)
        assertEquals(listOf("en", "ar", "hi", "id", "ta"), catalog.availableLanguages)
        assertEquals(7, catalog.shows.size)
        assertEquals(18, catalog.tags.size)
        assertEquals(3, catalog.tags.count { !it.isCategory })
        assertEquals(15, catalog.tags.count { it.isCategory })
        assertEquals("12485", catalog.heroShowId)
        assertTrue(catalog.shows.all { it.status == ShowStatus.Completed })
    }

    @Test
    fun `tag that only has english still resolves in arabic`() {
        val tag = catalog.tags.first { it.id == "marriage-first-love-later" }
        assertEquals("MarriageFirstLoveLater", tag.name.resolve(preferred = "ar", fallback = catalog.defaultLanguage))
    }

    @Test
    fun `all 299 episodes parse, 74 free, titles empty`() {
        val all = episodes.values.flatMap { it.episodes }
        assertEquals(299, all.size)
        assertEquals(74, all.count { it.isFree })
        assertTrue(all.all { it.title.isEmpty })
        assertTrue(all.all { it.sources["en"]?.containsKey("720p") == true })
    }

    @Test
    fun `13077 catalogue says 65 episodes but the file has 50`() {
        assertEquals(65, catalog.shows.first { it.id == "13077" }.catalogEpisodeCount)
        assertEquals(50, episodes.getValue("13077").episodes.size)
    }
}
