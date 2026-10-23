package com.takehome.drama.domain.usecase

import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeSavedShowRepository
import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.domain.fake.show
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.domain.usecase.library.SavedFilter
import com.takehome.drama.domain.usecase.library.SavedShowsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedShowsUseCaseTest {

    private val repository = FakeSavedShowRepository()
    private val useCase = SavedShowsUseCase(repository)

    private val data = catalog(
        shows = listOf(
            show("done", status = ShowStatus.Completed),
            show("airing", status = ShowStatus.Ongoing),
            show("odd", status = ShowStatus.Unknown("hiatus")),
        ),
    )

    @Test
    fun `setSaved is idempotent - double tap keeps the intended state`() = runTest {
        assertTrue(useCase.setSaved("done", saved = true).isSuccess)
        useCase.setSaved("done", saved = true)
        assertEquals(listOf("done"), repository.rows.value.map { it.showId })
        useCase.setSaved("done", saved = false)
        useCase.setSaved("done", saved = false)
        assertTrue(repository.rows.value.isEmpty())
    }

    @Test
    fun `filters mirror status, unknown status only shows under All`() = runTest {
        listOf("done", "airing", "odd", "removed-from-catalogue").forEach { repository.save(it) }

        useCase.observeSaved(data, SavedFilter.All).test {
            assertEquals(listOf("odd", "airing", "done"), awaitItem().map { it.id })
        }
        useCase.observeSaved(data, SavedFilter.Airing).test {
            assertEquals(listOf("airing"), awaitItem().map { it.id })
        }
        useCase.observeSaved(data, SavedFilter.Complete).test {
            assertEquals(listOf("done"), awaitItem().map { it.id })
        }
    }
}
