package com.takehome.drama.feature.main

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.FakeWatchProgressRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.history.GetContinueEpisodeUseCase
import com.takehome.drama.domain.usecase.history.GetContinueTargetUseCase
import com.takehome.drama.domain.usecase.history.HistoryRecency
import com.takehome.drama.domain.usecase.history.ObserveWatchHistoryUseCase
import com.takehome.drama.domain.usecase.history.RemoveFromHistoryUseCase
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import com.takehome.drama.feature.main.presentation.history.HistoryIntent
import com.takehome.drama.feature.main.presentation.history.HistorySingleEvent
import com.takehome.drama.feature.main.presentation.history.HistoryTab
import com.takehome.drama.feature.main.presentation.history.HistoryViewModel
import com.takehome.drama.feature.main.presentation.main.MainTabRequests
import com.takehome.drama.feature.main.presentation.main.model.MainTab
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val catalogRepository = FakeCatalogRepository(
        Result.success(testCatalog),
        episodes = testCatalog.shows.associate { it.id to testEpisodes(it.id) },
    )
    private val progress = FakeWatchProgressRepository()
    private val tabRequests = MainTabRequests()
    private val clock = FakeEpochClock()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = HistoryViewModel(
        savedStateHandle = handle,
        getCatalog = GetCatalogUseCase(catalogRepository),
        observeWatchHistory = ObserveWatchHistoryUseCase(progress),
        removeFromHistory = RemoveFromHistoryUseCase(progress),
        getContinueEpisode = GetContinueEpisodeUseCase(GetShowEpisodesUseCase(catalogRepository), GetContinueTargetUseCase(progress)),
        resolveContentLanguage = ResolveContentLanguageUseCase(),
        userPreferencesRepository = FakeUserPreferencesRepository(),
        epochClock = clock,
        mainTabRequests = tabRequests,
    )

    private fun watching(showId: String, updatedAt: Long = clock.millis) =
        WatchProgress(showId, 1, 2, 3, 30_000, 60_000, updatedAt)

    private fun finished(showId: String) = WatchProgress(showId, 2, 3, 3, 60_000, 60_000, clock.millis)

    @Test
    fun `splits watching and finished, with minutes left for today`() = runTest {
        progress.save(watching("a"))
        progress.save(finished("c"))

        val state = viewModel().viewState.value
        assertEquals(listOf("a"), state.watching.map { it.showId })
        assertEquals(HistoryRecency.MinutesLeft(1), state.watching.single().recency)
        assertEquals(listOf("c"), state.finished.map { it.showId })
        assertEquals(1f, state.finished.single().progress)
    }

    @Test
    fun `selected tab survives process death`() = runTest {
        val handle = SavedStateHandle()
        viewModel(handle).processIntent(HistoryIntent.SelectTab(HistoryTab.Finished))

        assertEquals(HistoryTab.Finished, viewModel(handle).viewState.value.tab)
    }

    @Test
    fun `remove tapped twice while writing removes once and toasts once`() = runTest {
        progress.save(watching("a"))
        progress.removeGate = CompletableDeferred()
        val vm = viewModel()

        vm.singleEvent.test {
            vm.processIntent(HistoryIntent.Remove("a"))
            vm.processIntent(HistoryIntent.Remove("a"))
            progress.removeGate?.complete(Unit)
            assertTrue((awaitItem() as HistorySingleEvent.ShowToast).success)
            expectNoEvents()
        }
        assertEquals(1, progress.removeCalls)
        assertTrue(vm.viewState.value.watching.isEmpty())
    }

    @Test
    fun `remove failure shows an error toast and keeps the row`() = runTest {
        progress.save(watching("a"))
        progress.removeFailure = IllegalStateException("disk")
        val vm = viewModel()

        vm.singleEvent.test {
            vm.processIntent(HistoryIntent.Remove("a"))
            assertFalse((awaitItem() as HistorySingleEvent.ShowToast).success)
        }
        assertEquals(listOf("a"), vm.viewState.value.watching.map { it.showId })
    }

    @Test
    fun `watching opens the player, finished opens the show`() = runTest {
        progress.save(watching("a"))
        progress.save(finished("c"))
        val vm = viewModel()
        val state = vm.viewState.value

        vm.singleEvent.test {
            vm.processIntent(HistoryIntent.OpenRow(state.watching.single()))
            assertEquals(HistorySingleEvent.OpenPlayer("a", 1), awaitItem())
            vm.processIntent(HistoryIntent.OpenRow(state.finished.single()))
            assertEquals(HistorySingleEvent.OpenDetail("c"), awaitItem())
        }
    }

    @Test
    fun `menu opens a sheet and remove from it closes the sheet`() = runTest {
        progress.save(watching("a"))
        val vm = viewModel()
        val row = vm.viewState.value.watching.single()

        vm.processIntent(HistoryIntent.OpenMenu(row))
        assertEquals(row, vm.viewState.value.menuFor)
        vm.processIntent(HistoryIntent.Remove("a"))
        assertNull(vm.viewState.value.menuFor)
    }

    @Test
    fun `empty watching cta asks main for the browse tab`() = runTest {
        val vm = viewModel()
        vm.singleEvent.test {
            vm.processIntent(HistoryIntent.FindNext)
            assertEquals(HistorySingleEvent.GoBrowse, awaitItem())
        }
        assertEquals(MainTab.Browse, tabRequests.pending.value)
    }

    @Test
    fun `edit mode turns off when the list it edits becomes empty`() = runTest {
        progress.save(watching("a"))
        val vm = viewModel()
        vm.processIntent(HistoryIntent.ToggleEdit)
        assertTrue(vm.viewState.value.editing)
        vm.processIntent(HistoryIntent.Remove("a"))
        assertFalse(vm.viewState.value.editing)
    }
}
