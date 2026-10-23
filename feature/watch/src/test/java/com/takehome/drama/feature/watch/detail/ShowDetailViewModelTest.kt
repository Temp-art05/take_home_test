package com.takehome.drama.feature.watch.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.usecase.catalog.CatalogUseCase
import com.takehome.drama.domain.usecase.history.WatchProgressUseCase
import com.takehome.drama.domain.usecase.library.SavedShowsUseCase
import com.takehome.drama.domain.usecase.watch.EpisodeCellState
import com.takehome.drama.domain.usecase.watch.EpisodesUseCase
import com.takehome.drama.domain.error.DomainException
import com.takehome.drama.feature.watch.R
import com.takehome.drama.feature.watch.fake.FakeCatalogRepository
import com.takehome.drama.feature.watch.fake.FakeSavedShowRepository
import com.takehome.drama.feature.watch.fake.FakeUserPreferencesRepository
import com.takehome.drama.feature.watch.fake.FakeWatchProgressRepository
import com.takehome.drama.feature.watch.fake.MainDispatcherRule
import com.takehome.drama.feature.watch.fake.SHOW_ID
import com.takehome.drama.feature.watch.fake.progress
import com.takehome.drama.feature.watch.fake.testCatalog
import com.takehome.drama.feature.watch.fake.testEpisodes
import com.takehome.drama.feature.watch.presentation.detail.ContinueCta
import com.takehome.drama.feature.watch.presentation.detail.ShowDetailIntent
import com.takehome.drama.feature.watch.presentation.detail.ShowDetailSingleEvent
import com.takehome.drama.feature.watch.presentation.detail.ShowDetailViewModel
import com.takehome.drama.domain.model.LocalizedText
import com.takehome.drama.domain.model.Tag
import com.takehome.drama.feature.watch.fake.testShow
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

    private fun viewModel(showId: String = SHOW_ID) = ShowDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("showId" to showId)),
        catalogUseCase = CatalogUseCase(catalogRepository),
        episodesUseCase = EpisodesUseCase(catalogRepository),
        watchProgressUseCase = WatchProgressUseCase(progressRepository),
        savedShowsUseCase = SavedShowsUseCase(savedRepository),
        userPreferencesRepository = preferences,
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
    fun `free cell opens the player, locked cell only shows a toast`() = runTest(mainRule.dispatcher) {
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
    fun `CTA pointing at a locked episode shows a toast instead of opening it`() = runTest(mainRule.dispatcher) {
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
        assertNull(failed.errorRes)
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
        assertNotNull(viewModel().viewState.value.errorRes)
    }

    @Test
    fun `unknown show id is a not found error, not a crash`() = runTest(mainRule.dispatcher) {
        val state = viewModel(showId = "nope").viewState.value
        assertEquals(R.string.watch_show_not_found, state.errorRes)
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
}
