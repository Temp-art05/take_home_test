package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.domain.fake.show
import com.takehome.drama.domain.fake.tag
import com.takehome.drama.domain.usecase.catalog.CatalogUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogUseCaseTest {

    private val useCase = CatalogUseCase(FakeCatalogRepository(Result.failure(IllegalStateException())))

    @Test
    fun `hero is looked up by id, not taken from the first show`() {
        val data = catalog(shows = listOf(show("1"), show("2"), show("3")), heroShowId = "3")
        assertEquals("3", useCase.hero(data)?.id)
    }

    @Test
    fun `hero id that does not exist falls back to first show`() {
        assertEquals("1", useCase.hero(catalog(shows = listOf(show("1"), show("2")), heroShowId = "404"))?.id)
        assertEquals("1", useCase.hero(catalog(shows = listOf(show("1")), heroShowId = null))?.id)
    }

    @Test
    fun `empty catalog has no hero`() {
        assertNull(useCase.hero(catalog(shows = emptyList(), heroShowId = "1")))
    }

    @Test
    fun `rails and genres are different lists, each sorted by orderPriority`() {
        val data = catalog(
            shows = listOf(show("1", listOf("popular", "trending", "ceo", "fantasy"))),
            tags = listOf(
                tag("fantasy", isCategory = true, orderPriority = 40),
                tag("popular", isCategory = false, orderPriority = 20),
                tag("ceo", isCategory = true, orderPriority = 30),
                tag("trending", isCategory = false, orderPriority = 10),
            ),
        )

        assertEquals(listOf("trending", "popular"), useCase.rails(data).map { it.tag.id })
        assertEquals(listOf("ceo", "fantasy"), useCase.genres(data).map { it.id })
    }

    @Test
    fun `equal orderPriority keeps server order`() {
        val data = catalog(
            shows = listOf(show("1", listOf("b", "a"))),
            tags = listOf(tag("b", true, 10), tag("a", true, 10)),
        )
        assertEquals(listOf("b", "a"), useCase.genres(data).map { it.id })
    }

    @Test
    fun `rail holds the shows tagged with it, in catalogue order`() {
        val data = catalog(
            shows = listOf(show("1", listOf("trending")), show("2"), show("3", listOf("trending"))),
            tags = listOf(tag("trending", isCategory = false, orderPriority = 10)),
        )
        assertEquals(listOf("1", "3"), useCase.rails(data).single().shows.map { it.id })
    }

    @Test
    fun `rail and genre without any show are dropped`() {
        // Đúng như fixtures: trending/popular/newest không show nào gắn.
        val data = catalog(
            shows = listOf(show("1", listOf("ceo"))),
            tags = listOf(tag("trending", false, 10), tag("ceo", true, 50), tag("revenge", true, 60)),
        )
        assertEquals(emptyList<Any>(), useCase.rails(data))
        assertEquals(listOf("ceo"), useCase.genres(data).map { it.id })
    }

    @Test
    fun `unknown tag ids on a show are ignored, known ones sorted by orderPriority`() {
        val data = catalog(
            shows = listOf(show("1", listOf("revenge", "ghost-tag", "ceo"))),
            tags = listOf(tag("ceo", true, 50), tag("revenge", true, 40)),
        )
        assertEquals(listOf("revenge", "ceo"), useCase.tagsOf(data.shows.first(), data).map { it.id })
    }

    @Test
    fun `showsInGenre null returns everything, otherwise filters by tag`() {
        val data = catalog(shows = listOf(show("1", listOf("ceo")), show("2", listOf("love"))))
        assertEquals(listOf("1", "2"), useCase.showsInGenre(data, tagId = null).map { it.id })
        assertEquals(listOf("2"), useCase.showsInGenre(data, tagId = "love").map { it.id })
    }

    @Test
    fun `content language - saved choice wins while catalogue still offers it`() {
        assertEquals("ar", useCase.resolveContentLanguage(saved = "ar", deviceLanguage = "hi", catalog = catalog(emptyList())))
    }

    @Test
    fun `content language - device language when nothing saved or saved is gone`() {
        val data = catalog(emptyList(), availableLanguages = listOf("en", "hi"))
        assertEquals("hi", useCase.resolveContentLanguage(saved = null, deviceLanguage = "hi-IN", catalog = data))
        assertEquals("hi", useCase.resolveContentLanguage(saved = "fr", deviceLanguage = "hi", catalog = data))
    }

    @Test
    fun `content language - default language when device language is not offered`() {
        assertEquals("en", useCase.resolveContentLanguage(saved = null, deviceLanguage = "fr", catalog = catalog(emptyList())))
    }
}
