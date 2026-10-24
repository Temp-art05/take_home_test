package com.takehome.drama.feature.watch.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.takehome.drama.core.ads.FakeRewardAdManager
import com.takehome.drama.core.ui.foundation.error.ErrorKind
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
import com.takehome.drama.feature.watch.presentation.detail.UnlockSheet
import com.takehome.drama.domain.error.DomainException
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.model.LocalizedText
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.Tag
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.history.GetContinueTargetUseCase
import com.takehome.drama.domain.usecase.history.ObserveShowProgressUseCase
import com.takehome.drama.domain.usecase.library.ObserveIsShowSavedUseCase
import com.takehome.drama.domain.usecase.library.SetShowSavedUseCase
import com.takehome.drama.domain.usecase.watch.EpisodeCellState
import com.takehome.drama.domain.usecase.watch.GetEpisodeGridUseCase
import com.takehome.drama.domain.usecase.watch.GetEpisodeRangesUseCase
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import com.takehome.drama.feature.watch.R
import com.takehome.drama.feature.watch.fake.FakeCatalogRepository
import com.takehome.drama.feature.watch.fake.FakeSavedShowRepository
import com.takehome.drama.feature.watch.fake.FakeWatchProgressRepository
import com.takehome.drama.feature.watch.fake.SHOW_ID
import com.takehome.drama.feature.watch.fake.progress
import com.takehome.drama.feature.watch.fake.testCatalog
import com.takehome.drama.feature.watch.fake.testEpisodes
import com.takehome.drama.feature.watch.fake.testShow
import com.takehome.drama.feature.watch.presentation.detail.ContinueCta
import com.takehome.drama.feature.watch.presentation.detail.ShowDetailIntent
import com.takehome.drama.feature.watch.presentation.detail.ShowDetailSingleEvent
import com.takehome.drama.feature.watch.presentation.detail.ShowDetailViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShowDetailViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val catalogRepository = FakeCatalogRepository(
        catalog = Result.success(testCatalog()),
        episodes = mapOf(SHOW_ID to Result.success(testEpisodes(count = 50))),
    )
    private val progressRepository = FakeWatchProgressRepository()
    private val savedRepository = FakeSavedShowRepository()
    private val preferences = FakeUserPreferencesRepository()
    private val unlockRepository = FakeEpisodeUnlockRepository()
    private val adsConfig = FakeAdsConfigRepository()
    private val ads = FakeRewardAdManager()

    private fun rewardedOff() {
        adsConfig.config.value = AdsConfig(placements = mapOf(AdPlacement.RewardedBonus.id to PlacementConfig(false)))
    }

    private fun viewModel(showId: String = SHOW_ID) = ShowDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("showId" to showId)),
        getCatalog = GetCatalogUseCase(catalogRepository),
        getShowEpisodes = GetShowEpisodesUseCase(catalogRepository),
        resolveContentLanguage = ResolveContentLanguageUseCase(),
        getEpisodeGrid = GetEpisodeGridUseCase(),
        getEpisodeRanges = GetEpisodeRangesUseCase(),
        observeShowProgress = ObserveShowProgressUseCase(progressRepository),
        getContinueTarget = GetContinueTargetUseCase(progressRepository),
        observeIsShowSaved = ObserveIsShowSavedUseCase(savedRepository),
        setShowSaved = SetShowSavedUseCase(savedRepository),
        userPreferencesRepository = preferences,
        observeUnlockedEpisodes = ObserveUnlockedEpisodesUseCase(unlockRepository),
        unlockEpisode = UnlockEpisodeUseCase(unlockRepository),
        isAdPlacementEnabled = IsAdPlacementEnabledUseCase(adsConfig, testAppConfig),
        observeAdPlacement = ObserveAdPlacementUseCase(adsConfig, testAppConfig),
        rewardAdManager = ads,
    )

    @Test
    fun `header and grid come from catalogue and the episode file`() = runTest(mainRule.dispatcher) {
        val state = viewModel().viewState.value
        assertFalse(state.isLoading)
        assertEquals("The Ex-Wife Returns", state.title)
        assertEquals(listOf("Romance"), state.tags)
        assertEquals(3_000_000L, state.viewCount)
        // Catalogue ghi 65 tập, file có 50 — tin file.
        assertEquals(50, state.episodeTotal)
        assertEquals(50, state.cells.size)
        assertEquals(EpisodeCellState.Locked, state.cells[5].state)
        assertEquals(3, state.ranges.size)
    }

    @Test
    fun `never watched - Play Ep 1, no cell playing`() = runTest(mainRule.dispatcher) {
        val state = viewModel().viewState.value
        assertEquals(ContinueCta(episodeIndex = 0, episodeNumber = 1, isResume = false, isLocked = false), state.continueCta)
        assertTrue(state.cells.none { it.state == EpisodeCellState.Playing })
    }

    @Test
    fun `half watched episode - Continue Ep N and its cell is playing`() = runTest(mainRule.dispatcher) {
        progressRepository.save(progress(index = 2, positionMs = 30_000))
        val state = viewModel().viewState.value
        assertEquals(ContinueCta(episodeIndex = 2, episodeNumber = 3, isResume = true, isLocked = false), state.continueCta)
        assertEquals(EpisodeCellState.Playing, state.cells[2].state)
    }

    @Test
    fun `progress saved while on the player updates the CTA on return`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        progressRepository.save(progress(index = 0, positionMs = 99_000))
        val state = viewModel.viewState.value
        assertEquals(1, state.continueCta?.episodeIndex)
        assertEquals(EpisodeCellState.Watched, state.cells[0].state)
    }

    @Test
    fun `rewarded off - free cell opens the player, locked cell only shows a toast`() = runTest(mainRule.dispatcher) {
        rewardedOff()
        val viewModel = viewModel()
        viewModel.singleEvent.test {
            viewModel.processIntent(ShowDetailIntent.EpisodeClick(index = 3))
            assertEquals(ShowDetailSingleEvent.OpenPlayer(SHOW_ID, 3), awaitItem())

            viewModel.processIntent(ShowDetailIntent.EpisodeClick(index = 7))
            assertEquals(ShowDetailSingleEvent.ShowToast(R.string.watch_episode_locked, 8, success = false), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `rewarded off - CTA pointing at a locked episode shows a toast instead of opening it`() = runTest(mainRule.dispatcher) {
        rewardedOff()
        // Ep. 5 (index 4) xem xong → tập kế là index 5, khoá.
        progressRepository.save(progress(index = 4, positionMs = 99_000))
        val viewModel = viewModel()
        assertEquals(true, viewModel.viewState.value.continueCta?.isLocked)
        // Ô của tập khoá vẫn là khoá, không tô "đang phát".
        assertEquals(EpisodeCellState.Locked, viewModel.viewState.value.cells[5].state)
        viewModel.singleEvent.test {
            viewModel.processIntent(ShowDetailIntent.ContinueClick)
            assertEquals(ShowDetailSingleEvent.ShowToast(R.string.watch_episode_locked, 6, success = false), awaitItem())
        }
    }

    @Test
    fun `save toggles to the opposite of what is shown and confirms with a toast`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.singleEvent.test {
            viewModel.processIntent(ShowDetailIntent.SaveClick)
            assertEquals(ShowDetailSingleEvent.ShowToast(R.string.watch_saved_to_library, null, success = true), awaitItem())
            assertTrue(viewModel.viewState.value.isSaved)

            viewModel.processIntent(ShowDetailIntent.SaveClick)
            assertEquals(ShowDetailSingleEvent.ShowToast(R.string.watch_removed_from_library, null, success = true), awaitItem())
            assertFalse(viewModel.viewState.value.isSaved)
        }
    }

    @Test
    fun `episode file failing keeps the header and shows an inline error, retry recovers`() = runTest(mainRule.dispatcher) {
        catalogRepository.episodes = mapOf(SHOW_ID to Result.failure(DomainException.Network()))
        val viewModel = viewModel()
        val failed = viewModel.viewState.value
        assertEquals("The Ex-Wife Returns", failed.title)
        assertNull(failed.error)
        assertNotNull(failed.episodesErrorRes)
        assertNull(failed.continueCta)

        catalogRepository.episodes = mapOf(SHOW_ID to Result.success(testEpisodes(count = 10)))
        val catalogCalls = catalogRepository.catalogCalls
        viewModel.processIntent(ShowDetailIntent.RetryEpisodesClick)
        assertNull(viewModel.viewState.value.episodesErrorRes)
        assertEquals(10, viewModel.viewState.value.cells.size)
        // Chỉ tải lại file tập: header không bị thay bằng spinner toàn màn.
        assertEquals(catalogCalls, catalogRepository.catalogCalls)
        assertFalse(viewModel.viewState.value.isLoading)
    }

    @Test
    fun `retry while the episode file is loading keeps the header on screen`() = runTest(mainRule.dispatcher) {
        catalogRepository.episodes = mapOf(SHOW_ID to Result.failure(DomainException.Network()))
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        catalogRepository.episodesGate = gate
        viewModel.processIntent(ShowDetailIntent.RetryEpisodesClick)

        val loading = viewModel.viewState.value
        assertFalse(loading.isLoading)
        assertTrue(loading.isEpisodesLoading)
        assertEquals("The Ex-Wife Returns", loading.title)
        gate.complete(Unit)
    }

    @Test
    fun `content language changed elsewhere re-resolves the header texts`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        assertEquals("The Ex-Wife Returns", viewModel.viewState.value.title)
        preferences.setContentLanguage("ar")
        assertEquals("عودة الزوجة", viewModel.viewState.value.title)
    }

    @Test
    fun `merchandising tags are not shown as genre chips`() = runTest(mainRule.dispatcher) {
        val trending = Tag("trending", LocalizedText(mapOf("en" to "Trending")), isCategory = false, orderPriority = 0)
        val catalog = testCatalog(testShow().copy(tagIds = listOf("romance", "trending")))
        catalogRepository.catalog = Result.success(catalog.copy(tags = catalog.tags + trending))
        assertEquals(listOf("Romance"), viewModel().viewState.value.tags)
    }

    @Test
    fun `catalogue failing is a full screen error`() = runTest(mainRule.dispatcher) {
        catalogRepository.catalog = Result.failure(DomainException.Network())
        assertNotNull(viewModel().viewState.value.error)
    }

    @Test
    fun `unknown show id is a not found error, not a crash`() = runTest(mainRule.dispatcher) {
        val state = viewModel(showId = "nope").viewState.value
        assertEquals(R.string.watch_show_not_found, state.error?.messageRes)
        assertEquals(ErrorKind.NotFound, state.error?.kind)
    }

    @Test
    fun `range tabs filter the grid - first tab when nothing was watched`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        val ranges = viewModel.viewState.value.ranges
        assertEquals(ranges[0], viewModel.viewState.value.selectedRange)

        viewModel.processIntent(ShowDetailIntent.RangeSelect(ranges[2]))
        val state = viewModel.viewState.value
        assertEquals(ranges[2], state.selectedRange)
        assertEquals((40 until 50).toList(), state.visibleCells.map { it.index })
    }

    @Test
    fun `default tab is the one holding the continue episode, a tab the user picked is kept`() = runTest(mainRule.dispatcher) {
        catalogRepository.episodes = mapOf(SHOW_ID to Result.success(testEpisodes(count = 50, free = 50)))
        progressRepository.save(progress(index = 25, positionMs = 30_000))
        val viewModel = viewModel()
        val ranges = viewModel.viewState.value.ranges
        assertEquals(ranges[1], viewModel.viewState.value.selectedRange)

        viewModel.processIntent(ShowDetailIntent.RangeSelect(ranges[0]))
        progressRepository.save(progress(index = 26, positionMs = 30_000, updatedAt = 99))
        assertEquals(ranges[0], viewModel.viewState.value.selectedRange)
    }

    @Test
    fun `header counts episodes watched`() = runTest(mainRule.dispatcher) {
        progressRepository.save(progress(index = 0, positionMs = 99_000))
        progressRepository.save(progress(index = 1, positionMs = 1_000))
        assertEquals(1, viewModel().viewState.value.watchedCount)
    }

    @Test
    fun `episode with empty show file still renders zero cells without crashing`() = runTest(mainRule.dispatcher) {
        catalogRepository.episodes = mapOf(SHOW_ID to Result.success(ShowEpisodes(SHOW_ID, emptyList())))
        val state = viewModel().viewState.value
        assertEquals(0, state.episodeTotal)
        assertNull(state.continueCta)
    }

    @Test
    fun `locked cell with rewarded on opens the unlock sheet, ad loads behind it`() = runTest(mainRule.dispatcher) {
        ads.rewardedGate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.singleEvent.test {
            viewModel.processIntent(ShowDetailIntent.EpisodeClick(index = 5))
            expectNoEvents()
        }
        assertEquals(UnlockSheet(episodeIndex = 5, episodeNumber = 6, ad = RewardedAdState.Loading), viewModel.viewState.value.unlockSheet)
        ads.rewardedGate!!.complete(Unit)
        assertEquals(RewardedAdState.Ready, viewModel.viewState.value.unlockSheet?.ad)
    }

    @Test
    fun `no ad to show - the sheet says so instead of spinning`() = runTest(mainRule.dispatcher) {
        ads.rewardedAvailable = false
        val viewModel = viewModel()
        viewModel.processIntent(ShowDetailIntent.EpisodeClick(index = 5))
        assertEquals(RewardedAdState.Unavailable, viewModel.viewState.value.unlockSheet?.ad)
        viewModel.singleEvent.test {
            viewModel.processIntent(ShowDetailIntent.UnlockWithAdClick)
            expectNoEvents()
        }
    }

    @Test
    fun `watch ad is only asked once and only when the ad is ready`() = runTest(mainRule.dispatcher) {
        ads.rewardedGate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.processIntent(ShowDetailIntent.EpisodeClick(index = 6))
        viewModel.singleEvent.test {
            viewModel.processIntent(ShowDetailIntent.UnlockWithAdClick) // đang load → bỏ qua
            expectNoEvents()
            ads.rewardedGate!!.complete(Unit)
            viewModel.processIntent(ShowDetailIntent.UnlockWithAdClick)
            viewModel.processIntent(ShowDetailIntent.UnlockWithAdClick)
            assertEquals(ShowDetailSingleEvent.ShowRewarded(episodeIndex = 6), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `reward earned unlocks the episode, keeps it unlocked and opens it`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(ShowDetailIntent.EpisodeClick(index = 5))
        viewModel.singleEvent.test {
            viewModel.processIntent(ShowDetailIntent.UnlockWithAdClick)
            assertEquals(ShowDetailSingleEvent.ShowRewarded(episodeIndex = 5), awaitItem())
            viewModel.processIntent(ShowDetailIntent.RewardResult(episodeIndex = 5, earned = true))
            assertEquals(ShowDetailSingleEvent.ShowToast(R.string.watch_unlock_success, 6, success = true), awaitItem())
            assertEquals(ShowDetailSingleEvent.OpenPlayer(SHOW_ID, 5), awaitItem())
        }
        val state = viewModel.viewState.value
        assertNull(state.unlockSheet)
        assertEquals(EpisodeCellState.Unwatched, state.cells[5].state)
        assertEquals(EpisodeCellState.Locked, state.cells[6].state)
        assertEquals(setOf(5), unlockRepository.unlocked.value[SHOW_ID])
    }

    @Test
    fun `closing the ad early keeps the episode locked`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(ShowDetailIntent.EpisodeClick(index = 5))
        viewModel.singleEvent.test {
            viewModel.processIntent(ShowDetailIntent.UnlockWithAdClick)
            assertEquals(ShowDetailSingleEvent.ShowRewarded(episodeIndex = 5), awaitItem())
            viewModel.processIntent(ShowDetailIntent.RewardResult(episodeIndex = 5, earned = false))
            assertEquals(ShowDetailSingleEvent.ShowToast(R.string.watch_unlock_incomplete, null, success = false), awaitItem())
            expectNoEvents()
        }
        assertNull(viewModel.viewState.value.unlockSheet)
        assertEquals(EpisodeCellState.Locked, viewModel.viewState.value.cells[5].state)
    }

    @Test
    fun `not now closes the sheet`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(ShowDetailIntent.EpisodeClick(index = 5))
        viewModel.processIntent(ShowDetailIntent.UnlockSheetDismiss)
        assertNull(viewModel.viewState.value.unlockSheet)
    }

    @Test
    fun `an episode unlocked earlier opens straight away and the CTA is not locked`() = runTest(mainRule.dispatcher) {
        unlockRepository.unlock(SHOW_ID, 5)
        progressRepository.save(progress(index = 4, positionMs = 99_000))
        val viewModel = viewModel()
        assertEquals(false, viewModel.viewState.value.continueCta?.isLocked)
        viewModel.singleEvent.test {
            viewModel.processIntent(ShowDetailIntent.EpisodeClick(index = 5))
            assertEquals(ShowDetailSingleEvent.OpenPlayer(SHOW_ID, 5), awaitItem())
        }
        assertNull(viewModel.viewState.value.unlockSheet)
    }

    @Test
    fun `show with locked episodes preloads the rewarded ad once on open`() = runTest(mainRule.dispatcher) {
        viewModel()
        // Progress đổi → lưới tính lại, không preload thêm lần nữa.
        progressRepository.save(progress(index = 1, positionMs = 10_000))
        assertEquals(1, ads.preloads)
        assertEquals(0, ads.rewardedLoads)
    }

    @Test
    fun `every episode free - nothing to preload`() = runTest(mainRule.dispatcher) {
        catalogRepository.episodes = mapOf(SHOW_ID to Result.success(testEpisodes(count = 5, free = 5)))
        viewModel()
        assertEquals(0, ads.preloads)
    }

    @Test
    fun `after a reward the next ad is preloaded for the next locked episode`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(ShowDetailIntent.EpisodeClick(index = 5))
        viewModel.processIntent(ShowDetailIntent.RewardResult(episodeIndex = 5, earned = true))
        assertEquals(2, ads.preloads)
    }

    @Test
    fun `remote config turning rewarded off while the unlock sheet is open closes it with the locked toast`() =
        runTest(mainRule.dispatcher) {
            ads.rewardedGate = CompletableDeferred()
            val viewModel = viewModel()
            viewModel.processIntent(ShowDetailIntent.EpisodeClick(index = 5))
            viewModel.singleEvent.test {
                rewardedOff()
                assertEquals(ShowDetailSingleEvent.ShowToast(R.string.watch_episode_locked, 6, success = false), awaitItem())
            }
            assertNull(viewModel.viewState.value.unlockSheet)
            ads.rewardedGate!!.complete(Unit)
            assertNull(viewModel.viewState.value.unlockSheet)
        }

    @Test
    fun `kill switch and blocked version behave like rewarded off - locked toast, no sheet, no preload`() =
        runTest(mainRule.dispatcher) {
            adsConfig.config.value = AdsConfig(enable = false)
            val killed = viewModel()
            killed.singleEvent.test {
                killed.processIntent(ShowDetailIntent.EpisodeClick(index = 5))
                assertEquals(ShowDetailSingleEvent.ShowToast(R.string.watch_episode_locked, 6, success = false), awaitItem())
            }
            adsConfig.config.value = AdsConfig(disabledVersions = listOf(testAppConfig.versionName))
            val blocked = viewModel()
            blocked.processIntent(ShowDetailIntent.EpisodeClick(index = 5))
            assertNull(blocked.viewState.value.unlockSheet)
            assertEquals(0, ads.preloads)
        }

    @Test
    fun `rewarded off at open then turned on - preloads then, and the sheet opens`() = runTest(mainRule.dispatcher) {
        rewardedOff()
        val viewModel = viewModel()
        assertEquals(0, ads.preloads)
        adsConfig.config.value = AdsConfig()
        assertEquals(1, ads.preloads)
        viewModel.processIntent(ShowDetailIntent.EpisodeClick(index = 5))
        assertEquals(RewardedAdState.Ready, viewModel.viewState.value.unlockSheet?.ad)
    }
}
