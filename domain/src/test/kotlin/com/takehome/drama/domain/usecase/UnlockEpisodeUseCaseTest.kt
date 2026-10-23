package com.takehome.drama.domain.usecase

import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeEpisodeUnlockRepository
import com.takehome.drama.domain.fake.episode
import com.takehome.drama.domain.usecase.watch.ObserveUnlockedEpisodesUseCase
import com.takehome.drama.domain.usecase.watch.UnlockEpisodeUseCase
import com.takehome.drama.domain.usecase.watch.isWatchable
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockEpisodeUseCaseTest {

    private val repo = FakeEpisodeUnlockRepository()
    private val unlock = UnlockEpisodeUseCase(repo)
    private val observe = ObserveUnlockedEpisodesUseCase(repo)

    @Test
    fun `unlocked episode shows up for its show only`() = runTest {
        observe("12692").test {
            assertEquals(emptySet<Int>(), awaitItem())
            assertTrue(unlock("12692", 5).isSuccess)
            assertEquals(setOf(5), awaitItem())
            unlock("13077", 6)
            unlock("12692", 7)
            assertEquals(setOf(5, 7), awaitItem())
        }
        observe("13077").test { assertEquals(setOf(6), awaitItem()) }
    }

    @Test
    fun `key is the episode index, not the displayed number`() = runTest {
        // Ep "12" ở vị trí 5 (dãy number nhảy cóc): mở theo index 5.
        val ep = episode(5, number = 12, isFree = false)
        unlock("s", ep.index)
        observe("s").test {
            val unlocked = awaitItem()
            assertTrue(ep.isWatchable(unlocked))
            assertFalse(episode(11, number = 5, isFree = false).isWatchable(unlocked))
        }
    }

    @Test
    fun `free episodes are always watchable`() {
        assertTrue(episode(0).isWatchable(emptySet()))
        assertFalse(episode(0, isFree = false).isWatchable(emptySet()))
    }
}
