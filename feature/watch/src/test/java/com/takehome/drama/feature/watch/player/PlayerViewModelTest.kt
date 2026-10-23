package com.takehome.drama.feature.watch.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.turbine.test
import com.takehome.drama.core.media.PlaybackState
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.UserPreferences
import com.takehome.drama.domain.usecase.catalog.CatalogUseCase
import com.takehome.drama.domain.usecase.history.WatchProgressUseCase
import com.takehome.drama.domain.usecase.watch.EndAction
import com.takehome.drama.domain.usecase.watch.EpisodesUseCase
import com.takehome.drama.feature.watch.R
import com.takehome.drama.feature.watch.fake.FakeCatalogRepository
import com.takehome.drama.feature.watch.fake.FakeUserPreferencesRepository
import com.takehome.drama.feature.watch.fake.FakeFeedPlayer
import com.takehome.drama.feature.watch.fake.FakeFeedPlayer.Load
import com.takehome.drama.feature.watch.fake.FakeWatchProgressRepository
import com.takehome.drama.feature.watch.fake.MainDispatcherRule
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

    private fun viewModel(
        episodeIndex: Int = 0,
        saved: Map<String, Any?> = emptyMap(),
    ) = PlayerViewModel(
        savedStateHandle = SavedStateHandle(mapOf("showId" to SHOW_ID, "episodeIndex" to episodeIndex) + saved),
        catalogUseCase = CatalogUseCase(catalogRepository),
        episodesUseCase = EpisodesUseCase(catalogRepository),
        watchProgressUseCase = WatchProgressUseCase(progressRepository),
        userPreferencesRepository = preferences,
        playerFactory = { player },
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
    fun `episode end with autoplay - marks it watched and plays the next one from the start`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        player.emit(PlaybackState(ended = true, positionMs = 100_000, durationMs = 100_000))

        assertTrue(progressRepository.get(SHOW_ID, 0)!!.isEpisodeCompleted)
        assertEquals(Load(url(1), 0, playWhenReady = true), player.loads.last())
        assertEquals(1, viewModel.viewState.value.episodeIndex)
        assertNull(viewModel.viewState.value.endAction)
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
        val viewModel = PlayerViewModel(
            handle, CatalogUseCase(catalogRepository), EpisodesUseCase(catalogRepository),
            WatchProgressUseCase(progressRepository), preferences, { player },
        )
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
    fun `picking a locked episode in the sheet keeps the sheet and shows a toast`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(PlayerIntent.EpisodesClick)
        viewModel.singleEvent.test {
            viewModel.processIntent(PlayerIntent.EpisodeSelect(index = 6))
            assertEquals(PlayerSingleEvent.ShowToast(R.string.watch_episode_locked, 7), awaitItem())
        }
        assertEquals(PlayerSheet.Episodes, viewModel.viewState.value.sheet)
        assertEquals(1, player.loads.size)

        viewModel.processIntent(PlayerIntent.EpisodeSelect(index = 3))
        assertEquals(PlayerSheet.None, viewModel.viewState.value.sheet)
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
        val viewModel = viewModel()
        val slow = CompletableDeferred<Unit>()
        progressRepository.getGates[1] = slow

        viewModel.processIntent(PlayerIntent.EpisodeSelect(index = 1))
        viewModel.processIntent(PlayerIntent.EpisodeSelect(index = 2))
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
        val viewModel = PlayerViewModel(
            handle, CatalogUseCase(catalogRepository), EpisodesUseCase(catalogRepository),
            WatchProgressUseCase(progressRepository), preferences, { player },
        )
        player.playing(positionMs = 21_000)

        viewModel.processIntent(PlayerIntent.PageSettled(index = 1))

        assertEquals(21_000L, progressRepository.get(SHOW_ID, 0)?.positionMs)
        assertEquals(Load(url(1), 0, playWhenReady = true), player.loads.last())
        assertEquals(1, viewModel.viewState.value.episodeIndex)
        assertEquals(1, handle.get<Int>(PlayerViewModel.KEY_EPISODE_INDEX))
    }

    @Test
    fun `swiping back resumes where that episode stopped`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        player.playing(positionMs = 30_000)
        viewModel.processIntent(PlayerIntent.PageSettled(index = 1))
        viewModel.processIntent(PlayerIntent.PageSettled(index = 0))
        assertEquals(Load(url(0), 30_000, playWhenReady = true), player.loads.last())
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
    fun `autoplay to the next episode scrolls the pager to it`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.singleEvent.test {
            player.emit(PlaybackState(ended = true, positionMs = 100_000, durationMs = 100_000))
            assertEquals(PlayerSingleEvent.ScrollToEpisode(1), awaitItem())
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
}
