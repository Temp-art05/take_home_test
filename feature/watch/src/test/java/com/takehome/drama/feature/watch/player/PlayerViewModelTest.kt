package com.takehome.drama.feature.watch.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.turbine.test
import com.takehome.drama.core.ads.FakeRewardAdManager
import com.takehome.drama.domain.fake.FakeAdsConfigRepository
import com.takehome.drama.domain.fake.FakeEpisodeUnlockRepository
import com.takehome.drama.domain.fake.testAppConfig
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.model.ads.AdsConfig
import com.takehome.drama.domain.model.ads.PlacementConfig
import com.takehome.drama.domain.usecase.ads.IsAdPlacementEnabledUseCase
import com.takehome.drama.domain.usecase.ads.ObserveAdPlacementUseCase
import com.takehome.drama.domain.usecase.watch.ObserveUnlockedEpisodesUseCase
import com.takehome.drama.domain.usecase.watch.UnlockEpisodeUseCase
import com.takehome.drama.feature.watch.presentation.common.RewardedAdState
import com.takehome.drama.core.media.FakeFeedPlayer
import com.takehome.drama.core.media.FakeFeedPlayer.Load
import com.takehome.drama.core.media.PlaybackState
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.UserPreferences
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.history.GetResumePositionUseCase
import com.takehome.drama.domain.usecase.history.ObserveShowProgressUseCase
import com.takehome.drama.domain.usecase.history.SaveWatchProgressUseCase
import com.takehome.drama.domain.usecase.watch.EndAction
import com.takehome.drama.domain.usecase.watch.GetEndActionUseCase
import com.takehome.drama.domain.usecase.watch.GetEpisodeGridUseCase
import com.takehome.drama.domain.usecase.watch.GetEpisodeRangesUseCase
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import com.takehome.drama.domain.usecase.watch.PickVideoSourceUseCase
import com.takehome.drama.feature.watch.R
import com.takehome.drama.feature.watch.fake.FakeCatalogRepository
import com.takehome.drama.feature.watch.fake.FakeWatchProgressRepository
import com.takehome.drama.feature.watch.fake.SHOW_ID
import com.takehome.drama.feature.watch.fake.progress
import com.takehome.drama.feature.watch.fake.testCatalog
import com.takehome.drama.feature.watch.fake.testEpisode
import com.takehome.drama.feature.watch.fake.testEpisodes
import com.takehome.drama.feature.watch.presentation.player.PlayerIntent
import com.takehome.drama.feature.watch.presentation.player.PlayerSingleEvent
import com.takehome.drama.feature.watch.presentation.player.PlayerViewModel
import com.takehome.drama.feature.watch.presentation.player.model.PlayerSheet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val catalogRepository = FakeCatalogRepository(
        catalog = Result.success(testCatalog()),
        episodes = mapOf(SHOW_ID to Result.success(testEpisodes(count = 10, free = 5))),
    )
    private val progressRepository = FakeWatchProgressRepository()
    private val preferences = FakeUserPreferencesRepository()
    private val player = FakeFeedPlayer()
    private val unlockRepository = FakeEpisodeUnlockRepository()
    private val adsConfig = FakeAdsConfigRepository()
    private val ads = FakeRewardAdManager()

    private fun rewardedOff() {
        adsConfig.config.value = AdsConfig(placements = mapOf(AdPlacement.RewardedBonus.id to PlacementConfig(false)))
    }

    private fun viewModel(
        episodeIndex: Int = 0,
        saved: Map<String, Any?> = emptyMap(),
        handle: SavedStateHandle? = null,
    ) = PlayerViewModel(
        savedStateHandle = handle ?: SavedStateHandle(mapOf("showId" to SHOW_ID, "episodeIndex" to episodeIndex) + saved),
        getCatalog = GetCatalogUseCase(catalogRepository),
        getShowEpisodes = GetShowEpisodesUseCase(catalogRepository),
        resolveContentLanguage = ResolveContentLanguageUseCase(),
        pickVideoSource = PickVideoSourceUseCase(),
        getEpisodeGrid = GetEpisodeGridUseCase(),
        getEpisodeRanges = GetEpisodeRangesUseCase(),
        getEndAction = GetEndActionUseCase(),
        getResumePosition = GetResumePositionUseCase(progressRepository),
        saveWatchProgress = SaveWatchProgressUseCase(progressRepository),
        observeShowProgress = ObserveShowProgressUseCase(progressRepository),
        userPreferencesRepository = preferences,
        playerFactory = { player },
        observeUnlockedEpisodes = ObserveUnlockedEpisodesUseCase(unlockRepository),
        unlockEpisode = UnlockEpisodeUseCase(unlockRepository),
        isAdPlacementEnabled = IsAdPlacementEnabledUseCase(adsConfig, testAppConfig),
        observeAdPlacement = ObserveAdPlacementUseCase(adsConfig, testAppConfig),
        rewardAdManager = ads,
    )

    private fun url(index: Int, language: String = "en", quality: String = "720p") =
        "https://e/$language/$index/$quality.m3u8"

    @Test
    fun `opens the episode in the content language at the saved position`() = runTest(mainRule.dispatcher) {
        progressRepository.save(progress(index = 2, positionMs = 30_000))
        val state = viewModel(episodeIndex = 2).viewState.value
        assertEquals(listOf(Load(url(2), 30_000, playWhenReady = true)), player.loads)
        assertEquals(3, state.episodeNumber)
        assertEquals(10, state.episodeTotal)
        assertEquals("The Ex-Wife Returns", state.showTitle)
    }

    @Test
    fun `content language picks the matching audio source`() = runTest(mainRule.dispatcher) {
        preferences.state.value = UserPreferences(contentLanguage = "ar")
        viewModel()
        assertEquals(url(0, language = "ar"), player.loads.single().url)
    }

    @Test
    fun `locked episode is never loaded`() = runTest(mainRule.dispatcher) {
        val state = viewModel(episodeIndex = 7).viewState.value
        assertTrue(state.isLocked)
        assertTrue(player.loads.isEmpty())
    }

    @Test
    fun `episode restored after process death wins over the route argument`() = runTest(mainRule.dispatcher) {
        viewModel(episodeIndex = 0, saved = mapOf(PlayerViewModel.KEY_EPISODE_INDEX to 3))
        assertEquals(url(3), player.loads.single().url)
    }

    @Test
    fun `going to background pauses and saves, coming back resumes only if it was playing`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        player.playing(positionMs = 40_000)

        viewModel.processIntent(PlayerIntent.ScreenStopped())
        assertFalse(player.state.value.playWhenReady)
        assertEquals(40_000L, progressRepository.get(SHOW_ID, 0)?.positionMs)

        viewModel.processIntent(PlayerIntent.ScreenStarted)
        assertTrue(player.state.value.isPlaying)

        viewModel.processIntent(PlayerIntent.TogglePlay)
        viewModel.processIntent(PlayerIntent.ScreenStopped())
        viewModel.processIntent(PlayerIntent.ScreenStarted)
        assertFalse(player.state.value.isPlaying)
    }

    @Test
    fun `progress is saved every 5 seconds of playback, never before duration is known`() = runTest(mainRule.dispatcher) {
        viewModel()
        player.emit(PlaybackState(isPlaying = true, playWhenReady = true, positionMs = 6_000, durationMs = 0))
        assertNull(progressRepository.get(SHOW_ID, 0))

        player.playing(positionMs = 1_000)
        player.playing(positionMs = 3_000)
        assertEquals(1_000L, progressRepository.get(SHOW_ID, 0)?.positionMs)
        player.playing(positionMs = 6_500)
        assertEquals(6_500L, progressRepository.get(SHOW_ID, 0)?.positionMs)
    }

    @Test
    fun `progress belongs to the episode it was measured on, so a new page never shows the old one`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.playbackProgress.collect {} }
        player.playing(positionMs = 40_000, durationMs = 100_000)
        assertEquals(40_000L, viewModel.playbackProgress.value.positionFor("0"))
        assertEquals(0L, viewModel.playbackProgress.value.positionFor("1"))
        viewModel.processIntent(PlayerIntent.PageSettled(index = 1))
        assertEquals(0L, viewModel.playbackProgress.value.positionFor("1"))
        assertEquals(0L, viewModel.playbackProgress.value.positionFor("0"))
    }

    @Test
    fun `episode end with autoplay - marks it watched, scrolls first, plays the next one once the page settles`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.singleEvent.test {
            player.emit(PlaybackState(ended = true, positionMs = 100_000, durationMs = 100_000))
            assertEquals(PlayerSingleEvent.ScrollToEpisode(1), awaitItem())
        }
        assertTrue(progressRepository.get(SHOW_ID, 0)!!.isEpisodeCompleted)
        // Chưa đổi tập trong lúc trang đang trượt: không nạp, top bar vẫn tập cũ (không flash).
        assertEquals(1, player.loads.size)
        assertEquals(0, viewModel.viewState.value.episodeIndex)
        assertNull(viewModel.viewState.value.endAction)

        viewModel.processIntent(PlayerIntent.PageSettled(index = 1))
        assertEquals(Load(url(1), 0, playWhenReady = true), player.loads.last())
        assertEquals(1, viewModel.viewState.value.episodeIndex)
    }

    @Test
    fun `episode end with the next one locked - end card, nothing loaded`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel(episodeIndex = 4)
        player.emit(PlaybackState(ended = true, positionMs = 100_000, durationMs = 100_000))
        assertEquals(EndAction.NextLocked(index = 5, episodeNumber = 6), viewModel.viewState.value.endAction)
        assertEquals(1, player.loads.size)
    }

    @Test
    fun `episode end with autoplay off - offers the next episode`() = runTest(mainRule.dispatcher) {
        preferences.state.value = UserPreferences(contentLanguage = "en", autoplayNextEpisode = false)
        val viewModel = viewModel()
        player.emit(PlaybackState(ended = true, positionMs = 100_000, durationMs = 100_000))
        assertEquals(EndAction.NextAvailable(index = 1, episodeNumber = 2), viewModel.viewState.value.endAction)
        assertEquals(1, player.loads.size)
    }

    @Test
    fun `changing quality reloads at the current position`() = runTest(mainRule.dispatcher) {
        catalogRepository.episodes = mapOf(SHOW_ID to Result.success(testEpisodes(10, qualities = listOf("720p", "1080p"))))
        val viewModel = viewModel()
        assertEquals(url(0, quality = "1080p"), player.loads.single().url)
        assertEquals(listOf("1080p", "720p"), viewModel.viewState.value.qualities)

        player.playing(positionMs = 20_000)
        viewModel.processIntent(PlayerIntent.QualitySelect("720p"))
        assertEquals(Load(url(0, quality = "720p"), 20_000, playWhenReady = true), player.loads.last())
        assertEquals("720p", viewModel.viewState.value.quality)
    }

    @Test
    fun `episode without any source is an error, not a crash`() = runTest(mainRule.dispatcher) {
        val broken = testEpisode(0).copy(sources = emptyMap())
        catalogRepository.episodes = mapOf(SHOW_ID to Result.success(ShowEpisodes(SHOW_ID, listOf(broken))))
        assertEquals(R.string.watch_error_no_source, viewModel().viewState.value.errorRes)
        assertTrue(player.loads.isEmpty())
    }

    @Test
    fun `speed tap cycles and survives process death`() = runTest(mainRule.dispatcher) {
        val handle = SavedStateHandle(mapOf("showId" to SHOW_ID, "episodeIndex" to 0))
        val viewModel = viewModel(handle = handle)
        viewModel.processIntent(PlayerIntent.SpeedClick)
        assertEquals(1.5f, player.lastSpeed)
        assertEquals(1.5f, viewModel.viewState.value.speed)
        assertEquals(1.5f, handle.get<Float>(PlayerViewModel.KEY_SPEED))
    }

    @Test
    fun `long stall in Auto drops to a lower real quality, Switch back restores it`() = runTest(mainRule.dispatcher) {
        catalogRepository.episodes = mapOf(SHOW_ID to Result.success(testEpisodes(10, qualities = listOf("720p", "480p"))))
        val viewModel = viewModel()
        player.emit(PlaybackState(isBuffering = true, playWhenReady = true, positionMs = 12_000, durationMs = 100_000))
        advanceTimeBy(6_000)

        assertEquals(Load(url(0, quality = "480p"), 12_000, playWhenReady = true), player.loads.last())
        assertEquals("480p", viewModel.viewState.value.qualityDropped)

        viewModel.processIntent(PlayerIntent.SwitchBackToHd)
        assertEquals(url(0, quality = "720p"), player.loads.last().url)
        assertNull(viewModel.viewState.value.qualityDropped)
    }

    @Test
    fun `long stall with only one quality does not reload`() = runTest(mainRule.dispatcher) {
        viewModel()
        player.emit(PlaybackState(isBuffering = true, playWhenReady = true, positionMs = 12_000, durationMs = 100_000))
        advanceTimeBy(10_000)
        assertEquals(1, player.loads.size)
    }

    @Test
    fun `rewarded off - picking a locked episode in the sheet keeps the sheet and shows a toast`() = runTest(mainRule.dispatcher) {
        rewardedOff()
        val viewModel = viewModel()
        viewModel.processIntent(PlayerIntent.EpisodesClick)
        viewModel.singleEvent.test {
            viewModel.processIntent(PlayerIntent.EpisodeSelect(index = 6))
            assertEquals(PlayerSingleEvent.ShowToast(R.string.watch_episode_locked, 7, success = false), awaitItem())
        }
        assertEquals(PlayerSheet.Episodes, viewModel.viewState.value.sheet)
        assertEquals(1, player.loads.size)

        viewModel.singleEvent.test {
            viewModel.processIntent(PlayerIntent.EpisodeSelect(index = 3))
            assertEquals(PlayerSingleEvent.ScrollToEpisode(3), awaitItem())
        }
        assertEquals(PlayerSheet.None, viewModel.viewState.value.sheet)
        // Chưa nạp lúc trang còn trượt (trang cũ còn trên màn) — nạp khi trang mới đứng yên.
        assertEquals(1, player.loads.size)
        viewModel.processIntent(PlayerIntent.PageSettled(index = 3))
        assertEquals(url(3), player.loads.last().url)
    }

    @Test
    fun `switching episode saves where the previous one stopped`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        player.playing(positionMs = 33_000)
        viewModel.processIntent(PlayerIntent.EpisodeSelect(index = 2))
        assertEquals(33_000L, progressRepository.get(SHOW_ID, 0)?.positionMs)
    }

    @Test
    fun `like is a local toggle`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(PlayerIntent.LikeClick)
        assertTrue(viewModel.viewState.value.isLiked)
        assertEquals(1_001L, viewModel.viewState.value.likeCount)
        viewModel.processIntent(PlayerIntent.LikeClick)
        assertEquals(1_000L, viewModel.viewState.value.likeCount)
    }

    @Test
    fun `like belongs to the episode - kept when swiping away and back`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(PlayerIntent.LikeClick)
        viewModel.processIntent(PlayerIntent.PageSettled(1))
        assertFalse(viewModel.viewState.value.isLiked)
        assertEquals(1_001L, viewModel.viewState.value.pages[0].likeCount)
        assertTrue(viewModel.viewState.value.pages[0].isLiked)
        viewModel.processIntent(PlayerIntent.PageSettled(0))
        assertTrue(viewModel.viewState.value.isLiked)
        assertEquals(1_001L, viewModel.viewState.value.likeCount)
    }

    @Test
    fun `every page carries its own number and likes for the overlay`() = runTest(mainRule.dispatcher) {
        val page = viewModel().viewState.value.pages[1]
        assertEquals(2, page.episodeNumber)
        assertEquals(1_000L, page.likeCount)
        assertFalse(page.isLiked)
    }

    @Test
    fun `going to background while the episode is still loading - stays silent, plays when back`() = runTest(mainRule.dispatcher) {
        val gate = CompletableDeferred<Unit>()
        catalogRepository.episodesGate = gate
        val viewModel = viewModel()
        viewModel.processIntent(PlayerIntent.ScreenStopped())
        gate.complete(Unit)

        assertFalse(player.loads.single().playWhenReady)
        assertFalse(player.state.value.playWhenReady)

        viewModel.processIntent(PlayerIntent.ScreenStarted)
        assertTrue(player.state.value.isPlaying)
    }

    @Test
    fun `background stops preloading, foreground resumes it`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(PlayerIntent.ScreenStopped())
        assertFalse(player.isPreloadActive)
        viewModel.processIntent(PlayerIntent.ScreenStarted)
        assertTrue(player.isPreloadActive)
    }

    @Test
    fun `rotation (configuration change) neither pauses nor saves`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        player.playing(positionMs = 1_000)
        player.playing(positionMs = 3_000)
        viewModel.processIntent(PlayerIntent.ScreenStopped(isChangingConfigurations = true))
        assertTrue(player.state.value.isPlaying)
        assertEquals(1_000L, progressRepository.get(SHOW_ID, 0)?.positionMs)
    }

    @Test
    fun `switching episodes quickly - the last choice wins even if an earlier load finishes later`() = runTest(mainRule.dispatcher) {
        // Tập vào màn đọc vị trí resume từ Room (chậm); user chọn tập khác trước khi đọc xong.
        val slow = CompletableDeferred<Unit>()
        progressRepository.getGates[1] = slow
        val viewModel = viewModel(episodeIndex = 1)

        viewModel.processIntent(PlayerIntent.EpisodeSelect(index = 2))
        viewModel.processIntent(PlayerIntent.PageSettled(index = 2))
        slow.complete(Unit)

        assertEquals(url(2), player.loads.last().url)
        assertTrue(player.loads.none { it.url == url(1) })
        assertEquals(2, viewModel.viewState.value.episodeIndex)
    }

    @Test
    fun `playlist gives a url only to free episodes, in the content language`() = runTest(mainRule.dispatcher) {
        viewModel()
        assertEquals(10, player.items.size)
        assertEquals((0..4).map { url(it) }, player.items.take(5).map { it.url })
        assertTrue(player.items.drop(5).all { it.url == null })
    }

    @Test
    fun `pages follow the episode file, locked ones flagged`() = runTest(mainRule.dispatcher) {
        val state = viewModel(episodeIndex = 3).viewState.value
        assertEquals(10, state.pages.size)
        assertEquals(listOf(false, false, false, false, false, true), state.pages.take(6).map { it.isLocked })
        assertEquals(3, state.currentPage)
    }

    @Test
    fun `swiping to the next page saves the previous episode and plays the new one`() = runTest(mainRule.dispatcher) {
        val handle = SavedStateHandle(mapOf("showId" to SHOW_ID, "episodeIndex" to 0))
        val viewModel = viewModel(handle = handle)
        player.playing(positionMs = 21_000)

        viewModel.processIntent(PlayerIntent.PageSettled(index = 1))

        assertEquals(21_000L, progressRepository.get(SHOW_ID, 0)?.positionMs)
        assertEquals(Load(url(1), 0, playWhenReady = true), player.loads.last())
        assertEquals(1, viewModel.viewState.value.episodeIndex)
        assertEquals(1, handle.get<Int>(PlayerViewModel.KEY_EPISODE_INDEX))
    }

    @Test
    fun `swiping back to a half watched episode continues where it stopped`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        player.playing(positionMs = 30_000)
        viewModel.processIntent(PlayerIntent.PageSettled(index = 1))
        viewModel.processIntent(PlayerIntent.PageSettled(index = 0))
        assertEquals(Load(url(0), 30_000, playWhenReady = true), player.loads.last())
    }

    @Test
    fun `swiping onto a half watched episode continues from the saved position`() = runTest(mainRule.dispatcher) {
        progressRepository.save(progress(index = 1, positionMs = 40_000))
        val viewModel = viewModel()
        viewModel.processIntent(PlayerIntent.PageSettled(index = 1))
        assertEquals(Load(url(1), 40_000, playWhenReady = true), player.loads.last())
    }

    @Test
    fun `swiping onto an episode watched to the end starts it over`() = runTest(mainRule.dispatcher) {
        progressRepository.save(progress(index = 1, positionMs = 100_000, durationMs = 100_000))
        val viewModel = viewModel()
        viewModel.processIntent(PlayerIntent.PageSettled(index = 1))
        assertEquals(Load(url(1), 0, playWhenReady = true), player.loads.last())
    }

    @Test
    fun `picking a half watched episode in the sheet continues from the saved position`() = runTest(mainRule.dispatcher) {
        progressRepository.save(progress(index = 2, positionMs = 40_000))
        val viewModel = viewModel()
        viewModel.processIntent(PlayerIntent.EpisodeSelect(index = 2))
        viewModel.processIntent(PlayerIntent.PageSettled(index = 2))
        assertEquals(Load(url(2), 40_000, playWhenReady = true), player.loads.last())
    }

    @Test
    fun `swiping onto a locked page stops playback and loads nothing`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel(episodeIndex = 4)
        player.playing(positionMs = 10_000)
        viewModel.processIntent(PlayerIntent.PageSettled(index = 5))

        val state = viewModel.viewState.value
        assertTrue(state.isLocked)
        assertEquals(5, player.currents.last())
        assertEquals(1, player.loads.size)
        assertFalse(player.state.value.playWhenReady)
    }

    @Test
    fun `settling on the current page again does nothing`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(PlayerIntent.PageSettled(index = 0))
        assertEquals(1, player.loads.size)
    }

    @Test
    fun `picking an episode in the sheet scrolls the pager to it`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.singleEvent.test {
            viewModel.processIntent(PlayerIntent.EpisodeSelect(index = 3))
            assertEquals(PlayerSingleEvent.ScrollToEpisode(3), awaitItem())
        }
    }

    @Test
    fun `position ticks stay out of the screen state`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        player.playing(positionMs = 1_000)
        val before = viewModel.viewState.value
        player.playing(positionMs = 1_500)
        assertEquals(before, viewModel.viewState.value)
    }

    @Test
    fun `player is released with the view model`() = runTest(mainRule.dispatcher) {
        val store = ViewModelStore()
        ViewModelProvider.create(store, viewModelFactory { initializer { viewModel() } })[PlayerViewModel::class]
        store.clear()
        assertTrue(player.released)
    }

    @Test
    fun `locked page offers the rewarded unlock once the ad is ready`() = runTest(mainRule.dispatcher) {
        ads.rewardedGate = CompletableDeferred()
        val viewModel = viewModel(episodeIndex = 5)
        assertEquals(RewardedAdState.Loading, viewModel.viewState.value.unlockAd)
        ads.rewardedGate!!.complete(Unit)
        assertEquals(RewardedAdState.Ready, viewModel.viewState.value.unlockAd)
    }

    @Test
    fun `rewarded off - locked page has no ad button`() = runTest(mainRule.dispatcher) {
        rewardedOff()
        assertNull(viewModel(episodeIndex = 5).viewState.value.unlockAd)
        assertEquals(0, ads.rewardedLoads)
    }

    @Test
    fun `free page has no ad button`() = runTest(mainRule.dispatcher) {
        assertNull(viewModel(episodeIndex = 2).viewState.value.unlockAd)
    }

    @Test
    fun `watch ad pauses playback before asking the screen to show it`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel(episodeIndex = 5)
        player.play()
        viewModel.singleEvent.test {
            viewModel.processIntent(PlayerIntent.UnlockWithAdClick)
            viewModel.processIntent(PlayerIntent.UnlockWithAdClick)
            assertEquals(PlayerSingleEvent.ShowRewarded(episodeIndex = 5), awaitItem())
            expectNoEvents()
        }
        assertFalse(player.state.value.playWhenReady)
    }

    @Test
    fun `reward earned unlocks the page in place and plays it from the start`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel(episodeIndex = 5)
        assertTrue(player.loads.isEmpty())
        viewModel.singleEvent.test {
            viewModel.processIntent(PlayerIntent.UnlockWithAdClick)
            assertEquals(PlayerSingleEvent.ShowRewarded(episodeIndex = 5), awaitItem())
            viewModel.processIntent(PlayerIntent.RewardResult(episodeIndex = 5, earned = true))
            assertEquals(PlayerSingleEvent.ShowToast(R.string.watch_unlock_success, 6, success = true), awaitItem())
        }
        val state = viewModel.viewState.value
        assertFalse(state.isLocked)
        assertNull(state.unlockAd)
        assertFalse(state.pages[5].isLocked)
        assertEquals(PlayerLoad(url(5), 0), player.loads.single().let { PlayerLoad(it.url, it.startPositionMs) })
        assertEquals(url(5), player.items[5].url)
    }

    @Test
    fun `closing the ad early keeps the page locked`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel(episodeIndex = 5)
        viewModel.singleEvent.test {
            viewModel.processIntent(PlayerIntent.UnlockWithAdClick)
            assertEquals(PlayerSingleEvent.ShowRewarded(episodeIndex = 5), awaitItem())
            viewModel.processIntent(PlayerIntent.RewardResult(episodeIndex = 5, earned = false))
            assertEquals(PlayerSingleEvent.ShowToast(R.string.watch_unlock_incomplete, formatArg = null, success = false), awaitItem())
        }
        assertTrue(viewModel.viewState.value.isLocked)
        assertTrue(player.loads.isEmpty())
        // Xem lại được: ad mới load cho lần bấm sau.
        assertEquals(RewardedAdState.Ready, viewModel.viewState.value.unlockAd)
    }

    @Test
    fun `an unlocked episode plays and autoplay runs into it`() = runTest(mainRule.dispatcher) {
        unlockRepository.unlock(SHOW_ID, 5)
        val viewModel = viewModel(episodeIndex = 4)
        assertFalse(viewModel.viewState.value.pages[5].isLocked)
        viewModel.singleEvent.test {
            player.emit(PlaybackState(ended = true, positionMs = 100_000, durationMs = 100_000))
            assertEquals(PlayerSingleEvent.ScrollToEpisode(5), awaitItem())
        }
    }

    @Test
    fun `rewarded on - picking a locked episode in the sheet goes to its page where the unlock button is`() =
        runTest(mainRule.dispatcher) {
            val viewModel = viewModel()
            viewModel.processIntent(PlayerIntent.EpisodesClick)
            viewModel.singleEvent.test {
                viewModel.processIntent(PlayerIntent.EpisodeSelect(index = 6))
                assertEquals(PlayerSingleEvent.ScrollToEpisode(6), awaitItem())
            }
            assertEquals(PlayerSheet.None, viewModel.viewState.value.sheet)
        }

    private data class PlayerLoad(val url: String, val startPositionMs: Long)

    @Test
    fun `show with locked episodes preloads the rewarded ad before the locked page is reached`() = runTest(mainRule.dispatcher) {
        viewModel(episodeIndex = 0)
        assertEquals(1, ads.preloads)
        assertEquals(0, ads.rewardedLoads)
    }

    @Test
    fun `after a reward the next ad is preloaded`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel(episodeIndex = 5)
        viewModel.processIntent(PlayerIntent.RewardResult(episodeIndex = 5, earned = true))
        assertEquals(2, ads.preloads)
    }

    @Test
    fun `remote config turning rewarded off while on the locked page hides the button instead of no ad`() =
        runTest(mainRule.dispatcher) {
            ads.rewardedGate = CompletableDeferred()
            val viewModel = viewModel(episodeIndex = 5)
            assertEquals(RewardedAdState.Loading, viewModel.viewState.value.unlockAd)
            rewardedOff()
            assertNull(viewModel.viewState.value.unlockAd)
            // Load cũ trả về sau khi đã tắt: không hiện lại nút.
            ads.rewardedGate!!.complete(Unit)
            assertNull(viewModel.viewState.value.unlockAd)
        }

    @Test
    fun `kill switch or blocked version while on the locked page hides the button too`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel(episodeIndex = 5)
        assertEquals(RewardedAdState.Ready, viewModel.viewState.value.unlockAd)
        adsConfig.config.value = AdsConfig(enable = false)
        assertNull(viewModel.viewState.value.unlockAd)
        adsConfig.config.value = AdsConfig()
        assertEquals(RewardedAdState.Ready, viewModel.viewState.value.unlockAd)
        adsConfig.config.value = AdsConfig(disabledVersions = listOf(testAppConfig.versionName))
        assertNull(viewModel.viewState.value.unlockAd)
    }

    @Test
    fun `remote config turning rewarded on while on the locked page offers the ad`() = runTest(mainRule.dispatcher) {
        rewardedOff()
        val viewModel = viewModel(episodeIndex = 5)
        assertNull(viewModel.viewState.value.unlockAd)
        assertEquals(0, ads.preloads)
        adsConfig.config.value = AdsConfig()
        assertEquals(RewardedAdState.Ready, viewModel.viewState.value.unlockAd)
        assertEquals(1, ads.preloads)
    }

    @Test
    fun `rewarded off - nothing is preloaded`() = runTest(mainRule.dispatcher) {
        rewardedOff()
        viewModel(episodeIndex = 0)
        assertEquals(0, ads.preloads)
    }
}
