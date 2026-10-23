package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.domain.fake.show
import com.takehome.drama.domain.fake.tag
import com.takehome.drama.domain.usecase.search.GetPopularShowsUseCase
import com.takehome.drama.domain.usecase.search.SearchResult
import com.takehome.drama.domain.usecase.search.SearchShowsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchShowsUseCaseTest {

    private val search = SearchShowsUseCase()
    private val popular = GetPopularShowsUseCase()

    private val catalog = catalog(
        shows = listOf(
            show("masked", title = mapOf("en" to "The Masked CEO"), viewCount = 100, tagIds = listOf("love")),
            show("contract", title = mapOf("en" to "CEO's Contract Wife", "ar" to "زوجة الرئيس"), viewCount = 50),
            show("ceo-tag", title = mapOf("en" to "Wolf King"), tagIds = listOf("ceo"), viewCount = 999),
            show("cafe", title = mapOf("en" to "Café Romance", "hi" to "कैफ़े रोमांस"), viewCount = 10),
        ),
        tags = listOf(
            tag("ceo", isCategory = true, orderPriority = 50, name = mapOf("en" to "CEO", "hi" to "दबंगCEO")),
            tag("love", isCategory = true, orderPriority = 60, name = mapOf("en" to "SweetRomance")),
        ),
    )

    private fun matches(query: String, language: String = "en") =
        search(catalog, query, language) as SearchResult.Matches

    @Test
    fun `blank query is idle`() {
        assertEquals(SearchResult.Idle, search(catalog, "", "en"))
        assertEquals(SearchResult.Idle, search(catalog, "   ", "en"))
    }

    @Test
    fun `ranks title prefix, then title contains, then tag-only, ties by views`() {
        assertEquals(listOf("contract", "masked", "ceo-tag"), matches("ceo").shows.map { it.id })
    }

    @Test
    fun `ignores case and Latin accents`() {
        assertEquals(listOf("cafe"), matches("CAFE").shows.map { it.id })
    }

    @Test
    fun `matches titles in other languages while browsing in English`() {
        assertEquals(listOf("contract"), matches("زوجة").shows.map { it.id })
        assertEquals(listOf("cafe"), matches("कैफ़े").shows.map { it.id })
    }

    @Test
    fun `matches tag names ignoring spaces, in any language`() {
        assertEquals(listOf("masked"), matches("sweet romance").shows.map { it.id })
        assertTrue(matches("दबंग").shows.any { it.id == "ceo-tag" })
    }

    @Test
    fun `suggestions are tag names then titles in the content language, capped at four`() {
        val result = matches("ceo")
        assertEquals("CEO", result.suggestions.first())
        assertTrue(result.suggestions.size <= SearchShowsUseCase.MAX_SUGGESTIONS)
        assertEquals(result.suggestions.distinct(), result.suggestions)
        assertEquals("दबंगCEO", matches("ceo", language = "hi").suggestions.first())
    }

    @Test
    fun `a matching tag no result show carries is not suggested`() {
        val withEmptyTag = catalog.copy(tags = catalog.tags + tag("ceo-new", isCategory = false, orderPriority = 1, name = mapOf("en" to "CEO Newest")))
        val result = search(withEmptyTag, "ceo", "en") as SearchResult.Matches
        assertTrue("CEO Newest" !in result.suggestions)
        assertEquals("CEO", result.suggestions.first())
    }

    @Test
    fun `no match yields NoResults`() {
        assertEquals(SearchResult.NoResults, search(catalog, "zzzz", "en"))
    }

    @Test
    fun `popular now is the top shows by views`() {
        assertEquals(listOf("ceo-tag", "masked"), popular(catalog, limit = 2).map { it.id })
    }
}
