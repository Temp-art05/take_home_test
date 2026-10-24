package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.FakeWatchProgressRepository
import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.domain.fake.episode
import com.takehome.drama.domain.fake.show
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.usecase.history.GetContinueEpisodeUseCase
import com.takehome.drama.domain.usecase.history.GetContinueTargetUseCase
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetContinueEpisodeUseCaseTest {

    private val progress = FakeWatchProgressRepository()
    private val catalogRepository = FakeCatalogRepository(
        Result.success(catalog(shows = listOf(show("a")))),
        episodes = mapOf("a" to ShowEpisodes("a", listOf(0, 1, 2).map { episode(it) })),
    )
    private val continueEpisode = GetContinueEpisodeUseCase(
        GetShowEpisodesUseCase(catalogRepository),
        GetContinueTargetUseCase(progress),
    )

    @Test
    fun `finished non-last episode continues with the next one`() = runTest {
        progress.save(WatchProgress("a", 0, 1, 3, 99_000, 100_000, 1))
        assertEquals(1, continueEpisode("a", savedEpisodeIndex = 0))
    }

    @Test
    fun `episode file failure falls back to the saved episode`() = runTest {
        progress.save(WatchProgress("b", 4, 5, 9, 1_000, 100_000, 1))
        assertEquals(4, continueEpisode("b", savedEpisodeIndex = 4))
    }
}
