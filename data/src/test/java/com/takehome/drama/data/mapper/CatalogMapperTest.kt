package com.takehome.drama.data.mapper

import com.takehome.drama.data.remote.dto.CatalogDto
import com.takehome.drama.data.remote.http.defaultJson
import com.takehome.drama.domain.model.ShowStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogMapperTest {

    private fun parse(json: String) = defaultJson.decodeFromString<CatalogDto>(json).toDomain()

    @Test
    fun `localized maps keep every language and drop non string values`() {
        val catalog = parse(
            """
            {"defaultLanguage":"en","shows":[{"id":"1",
              "title":{"en":"Wolf King","ar":"ملك الذئاب","hi":null,"ta":{"x":1}}}]}
            """,
        )
        assertEquals(mapOf("en" to "Wolf King", "ar" to "ملك الذئاب"), catalog.shows.single().title.values)
    }

    @Test
    fun `flat string instead of map is still readable`() {
        val title = parse("""{"shows":[{"id":"1","title":"Plain title"}]}""").shows.single().title
        assertEquals("Plain title", title.resolve(preferred = "ar", fallback = "en"))
    }

    @Test
    fun `counts beyond Int range parse as Long`() {
        val show = parse("""{"shows":[{"id":"1","viewCount":5000000000}]}""").shows.single()
        assertEquals(5_000_000_000L, show.viewCount)
    }

    @Test
    fun `free text status maps without failing`() {
        val shows = parse(
            """{"shows":[{"id":"1","status":"completed"},{"id":"2","status":"ongoing"},{"id":"3","status":"hiatus"},{"id":"4"}]}""",
        ).shows
        assertEquals(
            listOf(ShowStatus.Completed, ShowStatus.Ongoing, ShowStatus.Unknown("hiatus"), ShowStatus.Unknown("")),
            shows.map { it.status },
        )
    }

    @Test
    fun `tag without orderPriority goes last, without isCategory is a rail`() {
        val tags = parse("""{"tags":[{"id":"x","name":{"en":"X"}},{"id":"y","isCategory":true,"orderPriority":5}]}""").tags
        assertEquals(Int.MAX_VALUE, tags.first { it.id == "x" }.orderPriority)
        assertEquals(false, tags.first { it.id == "x" }.isCategory)
        assertEquals(true, tags.first { it.id == "y" }.isCategory)
    }

    @Test
    fun `broken show entry is dropped, others kept in order`() {
        val shows = parse(
            """{"shows":[{"id":"1"},{"id":"2","viewCount":"lots"},{"title":{"en":"no id"}},{"id":"3"}]}""",
        ).shows
        assertEquals(listOf("1", "3"), shows.map { it.id })
    }

    @Test
    fun `explicit null fields fall back to defaults instead of dropping the show`() {
        val show = parse(
            """{"shows":[{"id":"1","viewCount":null,"episodeCount":null,"posterUrl":null,"tagIds":null,"title":null}]}""",
        ).shows.single()
        assertEquals(0L, show.viewCount)
        assertEquals(0, show.catalogEpisodeCount)
        assertEquals("", show.posterUrl)
        assertEquals(emptyList<String>(), show.tagIds)
    }

    @Test
    fun `duplicate show id keeps the first entry`() {
        val shows = parse("""{"shows":[{"id":"1","viewCount":1},{"id":"1","viewCount":2}]}""").shows
        assertEquals(listOf(1L), shows.map { it.viewCount })
    }

    @Test
    fun `hero id and unknown top level keys`() {
        val catalog = parse("""{"home":{"heroShowId":"9","heroRank":1},"newField":{"a":1},"shows":[]}""")
        assertEquals("9", catalog.heroShowId)
        assertNull(parse("""{"shows":[]}""").heroShowId)
    }
}
