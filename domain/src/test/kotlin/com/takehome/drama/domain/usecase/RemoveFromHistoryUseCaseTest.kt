package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.fake.FakeWatchProgressRepository
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.usecase.history.RemoveFromHistoryUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoveFromHistoryUseCaseTest {

    private val repository = FakeWatchProgressRepository()
    private val remove = RemoveFromHistoryUseCase(repository)

    private fun row(showId: String, index: Int) = WatchProgress(showId, index, index + 1, 10, 1_000, 100_000, index.toLong())

    @Test
    fun `removes every episode of the show and leaves other shows`() = runTest {
        repository.save(row("a", 0))
        repository.save(row("a", 3))
        repository.save(row("b", 1))

        assertTrue(remove("a").isSuccess)

        assertEquals(listOf("b"), repository.rows.value.map { it.showId })
    }

    @Test
    fun `storage failure comes back as a failed result, not a throw`() = runTest {
        repository.removeFailure = IllegalStateException("disk")

        assertTrue(remove("a").isFailure)
    }
}
