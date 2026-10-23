package com.takehome.drama.domain.usecase

import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeWatchProgressRepository
import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.domain.fake.episode
import com.takehome.drama.domain.fake.show
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.usecase.history.ContinueTarget
import com.takehome.drama.domain.usecase.history.WatchProgressUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchProgressUseCaseTest {

    private val repository = FakeWatchProgressRepository()
    private val useCase = WatchProgressUseCase(repository)
    private val tenEpisodes = List(10) { episode(it) }

    private fun progress(
        showId: String,
        index: Int,
        position: Long,
        duration: Long = 100_000,
        total: Int = 10,
        updatedAt: Long = 1,
    ) = WatchProgress(showId, index, index + 1, total, position, duration, updatedAt)

    @Test
    fun `never watched - start at first episode`() = runTest {
        assertEquals(ContinueTarget(episodeIndex = 0, positionMs = 0, hasProgress = false), useCase.continueTarget("1", tenEpisodes))
    }

    @Test
    fun `half watched - resume same episode at saved position`() = runTest {
        repository.save(progress("1", index = 4, position = 40_000))
        assertEquals(ContinueTarget(4, 40_000, hasProgress = true), useCase.continueTarget("1", tenEpisodes))
    }

    @Test
    fun `finished episode with a next one - continue at next episode from start`() = runTest {
        repository.save(progress("1", index = 4, position = 99_000))
        assertEquals(ContinueTarget(5, 0, hasProgress = true), useCase.continueTarget("1", tenEpisodes))
    }

    @Test
    fun `finished last episode - stay on it from start`() = runTest {
        repository.save(progress("1", index = 9, position = 99_000))
        assertEquals(ContinueTarget(9, 0, hasProgress = true), useCase.continueTarget("1", tenEpisodes))
    }

    @Test
    fun `next episode skips a gap left by a dropped broken entry`() = runTest {
        val gapped = listOf(episode(0), episode(1), episode(3))
        repository.save(progress("1", index = 1, position = 99_000))
        assertEquals(ContinueTarget(3, 0, hasProgress = true), useCase.continueTarget("1", gapped))
    }

    @Test
    fun `saved episode gone from a shorter file - nearest earlier episode from start, not the old position`() = runTest {
        repository.save(progress("1", index = 12, position = 40_000))
        assertEquals(ContinueTarget(9, 0, hasProgress = true), useCase.continueTarget("1", tenEpisodes))
    }

    @Test
    fun `resume position of a finished episode restarts at zero`() = runTest {
        repository.save(progress("1", index = 0, position = 99_500))
        repository.save(progress("1", index = 1, position = 30_000))
        assertEquals(0L, useCase.resumePosition("1", 0))
        assertEquals(30_000L, useCase.resumePosition("1", 1))
        assertEquals(0L, useCase.resumePosition("1", 2))
    }

    @Test
    fun `history splits watching and finished, newest first, drops shows gone from catalogue`() = runTest {
        repository.save(progress("a", index = 3, position = 10_000, updatedAt = 1))
        repository.save(progress("b", index = 9, position = 99_000, total = 10, updatedAt = 2))
        repository.save(progress("c", index = 1, position = 5_000, updatedAt = 3))
        repository.save(progress("gone", index = 0, position = 5_000, updatedAt = 4))

        val data = catalog(shows = listOf(show("a"), show("b"), show("c")))

        useCase.observeHistory(data).test {
            val history = awaitItem()
            assertEquals(listOf("c", "a"), history.watching.map { it.show.id })
            assertEquals(listOf("b"), history.finished.map { it.show.id })
        }
    }

    @Test
    fun `progress of one show is observed sorted by episode index`() = runTest {
        repository.save(progress("1", index = 3, position = 1, updatedAt = 1))
        repository.save(progress("2", index = 0, position = 1, updatedAt = 2))
        repository.save(progress("1", index = 1, position = 1, updatedAt = 3))
        useCase.observeForShow("1").test {
            assertEquals(listOf(1, 3), awaitItem().map { it.episodeIndex })
        }
    }
}
