package com.takehome.drama.feature.main

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.takehome.drama.core.media.FakeFeedPlayer
import com.takehome.drama.core.media.PlaybackState
import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.FakeSavedShowRepository
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.fake.episode
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.library.ObserveIsShowSavedUseCase
import com.takehome.drama.domain.usecase.library.SetShowSavedUseCase
import com.takehome.drama.domain.usecase.reels.GetReelClipUseCase
import com.takehome.drama.domain.usecase.reels.GetReelsFeedUseCase
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import com.takehome.drama.domain.usecase.watch.PickVideoSourceUseCase
import com.takehome.drama.feature.main.presentation.reels.ClipStatus
import com.takehome.drama.feature.main.presentation.reels.ReelsIntent
import com.takehome.drama.feature.main.presentation.reels.ReelsSingleEvent
import com.takehome.drama.feature.main.presentation.reels.ReelsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReelsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    /** Feed theo lượt xem: c, hero, d, a. */
    private val catalogRepository = FakeCatalogRepository(
        Result.success(testCatalog),
        episodes = testCatalog.shows.associate { it.id to testEpisodes(it.id) },
    )
    private val savedRepository = FakeSavedShowRepository()
    private val player = FakeFeedPlayer()
    private val preferences = FakeUserPreferencesRepository()
    private val savedState = SavedStateHandle()

    private fun viewModel() = ReelsViewModel(
        savedStateHandle = savedState,
        getCatalog = GetCatalogUseCase(catalogRepository),
        getReelsFeed = GetReelsFeedUseCase(),
        getReelClip = GetReelClipUseCase(GetShowEpisodesUseCase(catalogRepository), PickVideoSourceUseCase()),
        resolveContentLanguage = ResolveContentLanguageUseCase(),
        observeIsShowSaved = ObserveIsShowSavedUseCase(savedRepository),
        setShowSaved = SetShowSavedUseCase(savedRepository),
        userPreferencesRepository = preferences,
        playerFactory = { player },
    )

    private fun ReelsViewModel.visible() {
        processIntent(ReelsIntent.ScreenStarted)
        processIntent(ReelsIntent.TabVisible(true))
    }

    @Test
    fun `changing content language reloads clips in the new language`() = runTest {
        val bilingual = FakeCatalogRepository(
            Result.success(testCatalog),
            episodes = testCatalog.shows.associate { show ->
                show.id to ShowEpisodes(
                    show.id,
                    listOf(episode(index = 0, sources = mapOf("en" to mapOf("720p" to "en/${show.id}"), "id" to mapOf("720p" to "id/${show.id}")))),
                )
            },
        )
        val vm = ReelsViewModel(
            savedStateHandle = savedState,
            getCatalog = GetCatalogUseCase(bilingual),
            getReelsFeed = GetReelsFeedUseCase(),
            getReelClip = GetReelClipUseCase(GetShowEpisodesUseCase(bilingual), PickVideoSourceUseCase()),
            resolveContentLanguage = ResolveContentLanguageUseCase(),
            observeIsShowSaved = ObserveIsShowSavedUseCase(savedRepository),
            setShowSaved = SetShowSavedUseCase(savedRepository),
            userPreferencesRepository = preferences,
            playerFactory = { player },
        )
        assertEquals("en/c", player.items.first().url)
        preferences.setContentLanguage("id")
        assertEquals("id/c", player.items.first().url)
        assertEquals(ClipStatus.Ready, vm.viewState.value.pages.first().status)
    }

    @Test
    fun `one reel per show, episodes loaded only around the current reel`() = runTest {
        val vm = viewModel()
        assertEquals(listOf("c", "hero", "d", "a"), vm.viewState.value.pages.map { it.showId })
        // Hiện tại 0 → nạp 0, 1, 2 (không có −1); reel thứ 4 chưa nạp.
        assertEquals(listOf("c", "hero", "d"), catalogRepository.episodesCalls)
        assertEquals(ClipStatus.Loading, vm.viewState.value.pages[3].status)
        vm.processIntent(ReelsIntent.PageSettled(1))
        assertEquals(listOf("c", "hero", "d", "a"), catalogRepository.episodesCalls)
    }

    @Test
    fun `reel shows the first free episode and the real episode count`() = runTest {
        val page = viewModel().viewState.value.pages.first()
        assertEquals(1, page.episodeIndex)
        assertEquals(2, page.episodeNumber)
        assertEquals(3, page.episodeTotal)
        assertEquals(1_000L, page.likeCount)
    }

    @Test
    fun `nothing plays until the tab is visible and started`() = runTest {
        val vm = viewModel()
        assertFalse(player.isActive)
        vm.processIntent(ReelsIntent.TabVisible(true))
        assertFalse(player.isActive)
        vm.processIntent(ReelsIntent.ScreenStarted)
        assertTrue(player.isActive)
        assertTrue(player.state.value.playWhenReady)
    }

    @Test
    fun `swiping plays the new reel from the start through the pool`() = runTest {
        val vm = viewModel()
        vm.visible()
        vm.processIntent(ReelsIntent.PageSettled(1))
        assertEquals(1, player.currents.last())
        assertEquals(FakeFeedPlayer.Load(clipUrl("hero"), 0, playWhenReady = true), player.loads.last())
        assertEquals(1, savedState.get<Int>(ReelsViewModel.KEY_CURRENT))
    }

    @Test
    fun `progress belongs to the reel it was measured on, so a new reel never shows the old one`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.playbackProgress.collect {} }
        vm.visible()
        player.playing(positionMs = 40_000, durationMs = 100_000)
        assertEquals(0.4f, vm.playbackProgress.value.fractionFor("c"), 0.001f)
        assertEquals(0f, vm.playbackProgress.value.fractionFor("hero"), 0f)
        vm.processIntent(ReelsIntent.PageSettled(1))
        // Reel mới bắt đầu từ 0 ngay khi lật trang — không có khung nào vẽ % của reel trước.
        assertEquals(0f, vm.playbackProgress.value.fractionFor("hero"), 0f)
        assertEquals(0f, vm.playbackProgress.value.fractionFor("c"), 0f)
    }

    @Test
    fun `leaving the tab or the screen frees every decoder, coming back resumes`() = runTest {
        val vm = viewModel()
        vm.visible()
        vm.processIntent(ReelsIntent.TabVisible(false))
        assertFalse(player.isActive)
        vm.processIntent(ReelsIntent.TabVisible(true))
        assertTrue(player.isActive)
        assertTrue(player.state.value.playWhenReady)
        vm.processIntent(ReelsIntent.ScreenStopped())
        assertFalse(player.isActive)
        assertEquals(listOf(false, true, false, true, false), player.activeCalls)
    }

    @Test
    fun `a reel paused by the user stays paused after switching tabs`() = runTest {
        val vm = viewModel()
        vm.visible()
        vm.processIntent(ReelsIntent.TogglePlay)
        vm.processIntent(ReelsIntent.TabVisible(false))
        vm.processIntent(ReelsIntent.TabVisible(true))
        assertFalse(player.state.value.playWhenReady)
    }

    @Test
    fun `rotation does not stop playback`() = runTest {
        val vm = viewModel()
        vm.visible()
        vm.processIntent(ReelsIntent.ScreenStopped(isChangingConfigurations = true))
        assertTrue(player.isActive)
    }

    @Test
    fun `speed cycles 1 to 1_5 to 2 and back`() = runTest {
        val vm = viewModel()
        listOf(1.5f, 2f, 1f).forEach { expected ->
            vm.processIntent(ReelsIntent.CycleSpeed)
            assertEquals(expected, vm.viewState.value.speed)
            assertEquals(expected, player.lastSpeed)
        }
    }

    @Test
    fun `watch all opens the player at the reel's episode`() = runTest {
        val vm = viewModel()
        vm.singleEvent.test {
            vm.processIntent(ReelsIntent.WatchAll)
            assertEquals(ReelsSingleEvent.OpenPlayer("c", 1), awaitItem())
        }
    }

    @Test
    fun `end of a reel scrolls to the next one`() = runTest {
        val vm = viewModel()
        vm.visible()
        vm.singleEvent.test {
            player.emit(PlaybackState(ended = true, durationMs = 10_000, positionMs = 10_000))
            assertEquals(ReelsSingleEvent.ScrollToPage(1), awaitItem())
        }
    }

    @Test
    fun `a show without a free playable episode is dropped from the feed`() = runTest {
        catalogRepository.episodes = catalogRepository.episodes +
            ("hero" to ShowEpisodes("hero", listOf(episode(index = 0, isFree = false))))
        val vm = viewModel()
        assertEquals(listOf("c", "d", "a"), vm.viewState.value.pages.map { it.showId })
        assertEquals(listOf("c", "d", "a"), player.items.map { it.id })
    }

    @Test
    fun `like and save act on the current reel`() = runTest {
        val vm = viewModel()
        vm.processIntent(ReelsIntent.ToggleLike)
        vm.processIntent(ReelsIntent.ToggleSave)
        val page = vm.viewState.value.pages.first()
        assertTrue(page.isLiked)
        assertEquals(1_001L, page.likeCount)
        assertTrue(page.isSaved)
    }
}
