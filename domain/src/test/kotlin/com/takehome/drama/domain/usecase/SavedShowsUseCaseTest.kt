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

        observeSaved(data, SavedFilter.All).test {
            assertEquals(listOf("odd", "airing", "done"), awaitItem().map { it.id })
        }
        observeSaved(data, SavedFilter.Airing).test {
            assertEquals(listOf("airing"), awaitItem().map { it.id })
        }
        observeSaved(data, SavedFilter.Complete).test {
            assertEquals(listOf("done"), awaitItem().map { it.id })
        }
    }
}
