package com.takehome.drama.domain.usecase

import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeSavedShowRepository
import com.takehome.drama.domain.fake.FakeWatchProgressRepository
import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.domain.fake.show
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.usecase.history.ObserveWatchHistoryUseCase
import com.takehome.drama.domain.usecase.library.ObserveLibrarySummaryUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySummaryUseCaseTest {

    private val progress = FakeWatchProgressRepository()
    private val saved = FakeSavedShowRepository()
    private val summary = ObserveLibrarySummaryUseCase(ObserveWatchHistoryUseCase(progress), saved)

    private val data = catalog(shows = listOf(show("a"), show("b"), show("c"), show("d")))

    private fun watching(showId: String, updatedAt: Long) =
        WatchProgress(showId, 0, 1, 10, 1_000, 100_000, updatedAt)

    private fun finished(showId: String, updatedAt: Long) =
        WatchProgress(showId, 9, 10, 10, 100_000, 100_000, updatedAt)

    @Test
    fun `nothing watched or saved gives zero counts and no recent`() = runTest {
        summary(data).test {
            val value = awaitItem()
            assertEquals(0, value.watchedCount)
            assertEquals(0, value.savedCount)
            assertTrue(value.recent.isEmpty())
        }
    }

    @Test
    fun `watched counts watching plus finished, recent is the two newest watching`() = runTest {
        progress.save(watching("a", updatedAt = 10))
        progress.save(watching("b", updatedAt = 30))
        progress.save(finished("c", updatedAt = 40))
        progress.save(watching("d", updatedAt = 20))

        summary(data).test {
            val value = awaitItem()
            assertEquals(4, value.watchedCount)
            assertEquals(listOf("b", "d"), value.recent.map { it.show.id })
        }
    }

    @Test
    fun `shows removed from the catalogue are not counted anywhere`() = runTest {
        progress.save(watching("gone", updatedAt = 50))
        progress.save(watching("a", updatedAt = 10))
        saved.save("gone")
        saved.save("b")

        summary(data).test {
            val value = awaitItem()
            assertEquals(1, value.watchedCount)
            assertEquals(1, value.savedCount)
            assertEquals(listOf("a"), value.recent.map { it.show.id })
        }
    }

    @Test
    fun `room changes update the counts on the same stream`() = runTest {
        summary(data).test {
            assertEquals(0, awaitItem().savedCount)
            saved.save("a")
            assertEquals(1, awaitItem().savedCount)
            progress.save(watching("b", updatedAt = 1))
            assertEquals(1, awaitItem().watchedCount)
        }
    }
}
