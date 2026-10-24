package com.takehome.drama.feature.main

import app.cash.turbine.test
import com.takehome.drama.core.ui.foundation.error.ErrorKind
import com.takehome.drama.domain.error.DomainException
import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.FakeSavedShowRepository
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.FakeWatchProgressRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.history.GetContinueEpisodeUseCase
import com.takehome.drama.domain.usecase.history.GetContinueTargetUseCase
import com.takehome.drama.domain.usecase.history.ObserveWatchHistoryUseCase
import com.takehome.drama.domain.usecase.library.ObserveLibrarySummaryUseCase
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import com.takehome.drama.feature.main.presentation.library.LibraryIntent
import com.takehome.drama.feature.main.presentation.library.LibrarySingleEvent
import com.takehome.drama.feature.main.presentation.library.LibraryViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val catalogRepository = FakeCatalogRepository(
        Result.success(testCatalog),
        episodes = testCatalog.shows.associate { it.id to testEpisodes(it.id) },
    )
    private val progress = FakeWatchProgressRepository()
    private val saved = FakeSavedShowRepository()

    private fun viewModel() = LibraryViewModel(
        getCatalog = GetCatalogUseCase(catalogRepository),
        observeLibrarySummary = ObserveLibrarySummaryUseCase(ObserveWatchHistoryUseCase(progress), saved),
        getContinueEpisode = GetContinueEpisodeUseCase(GetShowEpisodesUseCase(catalogRepository), GetContinueTargetUseCase(progress)),
        resolveContentLanguage = ResolveContentLanguageUseCase(),
        userPreferencesRepository = FakeUserPreferencesRepository(),
        epochClock = FakeEpochClock(),
    )

    private fun watching(showId: String, updatedAt: Long, index: Int = 1) =
        WatchProgress(showId, index, index + 1, 3, 30_000, 60_000, updatedAt)

    @Test
    fun `loads counts and the recent strip`() = runTest {
        progress.save(watching("a", updatedAt = 1))
        progress.save(watching("c", updatedAt = 2))
        progress.save(watching("d", updatedAt = 3))
        saved.save("hero")

        val state = viewModel().viewState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.watchedCount)
        assertEquals(1, state.savedCount)
        assertEquals(listOf("d", "c"), state.recent.map { it.showId })
        assertEquals(0.5f, state.recent.first().progress)
    }

    @Test
    fun `room changes update the counts without reloading the catalogue`() = runTest {
        val vm = viewModel()
        assertEquals(0, vm.viewState.value.savedCount)
        saved.save("a")
        progress.save(watching("c", updatedAt = 1))
        assertEquals(1, vm.viewState.value.savedCount)
        assertEquals(1, vm.viewState.value.watchedCount)
        assertEquals(1, catalogRepository.catalogCalls)
    }

    @Test
    fun `offline catalogue shows the offline state, retry recovers`() = runTest {
        catalogRepository.catalog = Result.failure(DomainException.Network())
        val vm = viewModel()
        assertEquals(ErrorKind.Offline, vm.viewState.value.error?.kind)

        catalogRepository.catalog = Result.success(testCatalog)
        vm.processIntent(LibraryIntent.Retry)
        assertNull(vm.viewState.value.error)
        assertFalse(vm.viewState.value.isLoading)
    }

    @Test
    fun `retry while loading is ignored`() = runTest {
        catalogRepository.catalogGate = CompletableDeferred()
        val vm = viewModel()
        assertTrue(vm.viewState.value.isLoading)
        vm.processIntent(LibraryIntent.Retry)
        vm.processIntent(LibraryIntent.Retry)
        catalogRepository.catalogGate?.complete(Unit)
        assertEquals(1, catalogRepository.catalogCalls)
    }

    @Test
    fun `recent row opens the player on the episode to continue`() = runTest {
        // Tập 0 đã xem hết (không phải tập cuối) → mở tập 1.
        progress.save(WatchProgress("a", 0, 1, 3, 59_500, 60_000, updatedAt = 1))
        val vm = viewModel()
        vm.singleEvent.test {
            vm.processIntent(LibraryIntent.OpenRecent("a", episodeIndex = 0))
            assertEquals(LibrarySingleEvent.OpenPlayer("a", 1), awaitItem())
        }
    }

    @Test
    fun `history and saved rows navigate`() = runTest {
        val vm = viewModel()
        vm.singleEvent.test {
            vm.processIntent(LibraryIntent.OpenHistory)
            assertEquals(LibrarySingleEvent.OpenHistory, awaitItem())
            vm.processIntent(LibraryIntent.OpenSaved)
            assertEquals(LibrarySingleEvent.OpenSaved, awaitItem())
        }
    }
}
