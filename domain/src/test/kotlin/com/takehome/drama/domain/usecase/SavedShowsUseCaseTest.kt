package com.takehome.drama.domain.usecase

import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeSavedShowRepository
import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.domain.fake.show
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.domain.usecase.library.ObserveSavedShowsUseCase
import com.takehome.drama.domain.usecase.library.SavedFilter
import com.takehome.drama.domain.usecase.library.SetShowSavedUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedShowsUseCaseTest {

    private val repository = FakeSavedShowRepository()
    private val setSaved = SetShowSavedUseCase(repository)
    private val observeSaved = ObserveSavedShowsUseCase(repository)

    private val data = catalog(
        shows = listOf(
            show("done", status = ShowStatus.Completed),
            show("airing", status = ShowStatus.Ongoing),
            show("odd", status = ShowStatus.Unknown("hiatus")),
        ),
    )

    @Test
    fun `setSaved is idempotent - double tap keeps the intended state`() = runTest {
        assertTrue(setSaved("done", saved = true).isSuccess)
        setSaved("done", saved = true)
        assertEquals(listOf("done"), repository.rows.value.map { it.showId })
        setSaved("done", saved = false)
        setSaved("done", saved = false)
        assertTrue(repository.rows.value.isEmpty())
    }

    @Test
    fun `filters mirror status, unknown status only shows under All`() = runTest {
        listOf("done", "airing", "odd", "removed-from-catalogue").forEach { repository.save(it) }

        observeSaved(data).test {
            val shelf = awaitItem()
            assertEquals(listOf("odd", "airing", "done"), shelf.shows(SavedFilter.All).map { it.id })
            assertEquals(listOf("airing"), shelf.shows(SavedFilter.Airing).map { it.id })
            assertEquals(listOf("done"), shelf.shows(SavedFilter.Complete).map { it.id })
        }
    }

    @Test
    fun `counts per chip ignore shows gone from the catalogue`() = runTest {
        listOf("done", "airing", "odd", "removed-from-catalogue").forEach { repository.save(it) }

        observeSaved(data).test {
            val shelf = awaitItem()
            assertEquals(3, shelf.count(SavedFilter.All))
            assertEquals(1, shelf.count(SavedFilter.Airing))
            assertEquals(1, shelf.count(SavedFilter.Complete))
            assertEquals(false, shelf.isEmpty)
        }
    }

    @Test
    fun `nothing saved is an empty shelf, a filter with no match is not`() = runTest {
        observeSaved(data).test {
            assertTrue(awaitItem().isEmpty)
            repository.save("done")
            val shelf = awaitItem()
            assertEquals(false, shelf.isEmpty)
            assertTrue(shelf.shows(SavedFilter.Airing).isEmpty())
        }
    }
}
