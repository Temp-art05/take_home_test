package com.takehome.drama.feature.main

import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.FakeWatchProgressRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.model.UserPreferences
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.discovery.ClockTime
import com.takehome.drama.domain.usecase.discovery.Daypart
import com.takehome.drama.domain.usecase.discovery.GetHomeFeedUseCase
import com.takehome.drama.domain.usecase.history.GetContinueTargetUseCase
import com.takehome.drama.domain.usecase.history.ObserveWatchHistoryUseCase
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import com.takehome.drama.feature.main.presentation.home.HomeIntent
import com.takehome.drama.feature.main.presentation.home.HomeSingleEvent
import com.takehome.drama.feature.main.presentation.home.HomeViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val catalogRepository = FakeCatalogRepository(
        Result.success(testCatalog),
        episodes = testCatalog.shows.associate { it.id to testEpisodes(it.id) },
    )
    private val progressRepository = FakeWatchProgressRepository()
    private val preferences = FakeUserPreferencesRepository(UserPreferences(contentLanguage = "en"))
    private val clock = FakeWallClock()

    private fun viewModel() = HomeViewModel(
        getCatalog = GetCatalogUseCase(catalogRepository),
        getHomeFeed = GetHomeFeedUseCase(),
        observeWatchHistory = ObserveWatchHistoryUseCase(progressRepository),
        getShowEpisodes = GetShowEpisodesUseCase(catalogRepository),
        getContinueTarget = GetContinueTargetUseCase(progressRepository),
        resolveContentLanguage = ResolveContentLanguageUseCase(),
        userPreferencesRepository = preferences,
        wallClock = clock,
    )

    @Test
    fun `hero comes from heroShowId and the rest flow by views`() = runTest {
        val state = viewModel().viewState.value
        assertEquals("hero", state.hero?.id)
        assertEquals(listOf("c", "d", "a"), state.alsoFlowing.map { it.id })
        assertEquals("Revenge", state.hero?.genre)
    }

    @Test
    fun `taste from onboarding moves matching shows first`() = runTest {
        preferences.state.value = UserPreferences(contentLanguage = "en", tasteTagIds = setOf("romance"))
        assertEquals(listOf("d", "a", "c"), viewModel().viewState.value.alsoFlowing.map { it.id })
    }

    @Test
    fun `daypart follows the device clock and is recomputed on start`() = runTest {
        clock.time = ClockTime(10, 59)
        val vm = viewModel()
        assertEquals(Daypart.Morning, vm.viewState.value.daypart)
        assertEquals(listOf(Daypart.Lunch, Daypart.Night), vm.viewState.value.upcoming)
        clock.time = ClockTime(11, 0)
        vm.processIntent(HomeIntent.ScreenStarted)
        assertEquals(Daypart.Lunch, vm.viewState.value.daypart)
        assertEquals(ClockTime(11, 0), vm.viewState.value.clock)
    }

    @Test
    fun `pick up your flow comes from Room, newest first, and updates live`() = runTest {
        val vm = viewModel()
        assertEquals(emptyList<Any>(), vm.viewState.value.continueRow)
        progressRepository.save(WatchProgress("a", 1, 2, 10, 30_000, 60_000, updatedAt = 1))
        progressRepository.save(WatchProgress("c", 3, 4, 10, 15_000, 60_000, updatedAt = 2))
        val row = vm.viewState.value.continueRow
        assertEquals(listOf("c", "a"), row.map { it.show.id })
        assertEquals(0.5f, row.last().progress)
        vm.singleEvent.test {
            vm.processIntent(HomeIntent.ContinueShow("a", row.last().episodeIndex))
            assertEquals(HomeSingleEvent.OpenPlayer("a", 1), awaitItem())
        }
    }

    @Test
    fun `pick up opens the next episode when the newest record is a finished episode`() = runTest {
        // Tập 0 xem hết (không phải tập cuối) → mở tập 1 từ đầu, không phát lại tập 0.
        progressRepository.save(WatchProgress("a", 0, 1, 3, 59_000, 60_000, updatedAt = 1))
        val vm = viewModel()
        val card = vm.viewState.value.continueRow.single()
        vm.singleEvent.test {
            vm.processIntent(HomeIntent.ContinueShow("a", card.episodeIndex))
            assertEquals(HomeSingleEvent.OpenPlayer("a", 1), awaitItem())
        }
    }

    @Test
    fun `pick up falls back to the saved episode when the episode file fails`() = runTest {
        catalogRepository.episodes = emptyMap()
        progressRepository.save(WatchProgress("a", 0, 1, 3, 59_000, 60_000, updatedAt = 1))
        val vm = viewModel()
        vm.singleEvent.test {
            vm.processIntent(HomeIntent.ContinueShow("a", 0))
            assertEquals(HomeSingleEvent.OpenPlayer("a", 0), awaitItem())
        }
    }

    @Test
    fun `catalogue error shows retry, retry recovers`() = runTest {
        catalogRepository.catalog = Result.failure(Exception("offline"))
        val vm = viewModel()
        assertEquals(false, vm.viewState.value.errorRes == null)
        catalogRepository.catalog = Result.success(testCatalog)
        vm.processIntent(HomeIntent.Retry)
        assertNull(vm.viewState.value.errorRes)
        assertEquals("hero", vm.viewState.value.hero?.id)
    }

    @Test
    fun `see all and bell switch tabs`() = runTest {
        val vm = viewModel()
        vm.singleEvent.test {
            vm.processIntent(HomeIntent.SeeAll)
            assertEquals(HomeSingleEvent.OpenBrowse, awaitItem())
            vm.processIntent(HomeIntent.OpenBell)
            assertEquals(HomeSingleEvent.OpenSettings, awaitItem())
        }
    }
}
