package com.takehome.drama.feature.main

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.takehome.drama.core.media.FakeFeedPlayer
import com.takehome.drama.core.media.FeedItem
import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.FakeSavedShowRepository
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.model.UserPreferences
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.library.ObserveIsShowSavedUseCase
import com.takehome.drama.domain.usecase.library.SetShowSavedUseCase
import com.takehome.drama.domain.usecase.reels.GetReelClipUseCase
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import com.takehome.drama.domain.usecase.watch.PickVideoSourceUseCase
import com.takehome.drama.feature.main.presentation.browse.BrowseIntent
import com.takehome.drama.feature.main.presentation.browse.BrowseSingleEvent
import com.takehome.drama.feature.main.presentation.browse.BrowseViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BrowseViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val catalogRepository = FakeCatalogRepository(
        Result.success(testCatalog),
        episodes = testCatalog.shows.associate { it.id to testEpisodes(it.id) },
    )
    private val preferences = FakeUserPreferencesRepository(UserPreferences(contentLanguage = "en", autoplayPreviewWifiOnly = true))
    private val connectivity = FakeConnectivityMonitor(unmetered = true)
    private val savedRepository = FakeSavedShowRepository()
    private val player = FakeFeedPlayer()
    private val savedState = SavedStateHandle()

    private fun viewModel() = BrowseViewModel(
        savedStateHandle = savedState,
        getCatalog = GetCatalogUseCase(catalogRepository),
        getReelClip = GetReelClipUseCase(GetShowEpisodesUseCase(catalogRepository), PickVideoSourceUseCase()),
        resolveContentLanguage = ResolveContentLanguageUseCase(),
        observeIsShowSaved = ObserveIsShowSavedUseCase(savedRepository),
        setShowSaved = SetShowSavedUseCase(savedRepository),
        userPreferencesRepository = preferences,
        connectivityMonitor = connectivity,
        playerFactory = { player },
    )

    private fun BrowseViewModel.visible() {
        processIntent(BrowseIntent.ScreenStarted)
        processIntent(BrowseIntent.TabVisible(true))
    }

    @Test
    fun `preview starts on the hero, muted, with its first free episode`() = runTest {
        val vm = viewModel()
        vm.visible()
        assertEquals("hero", vm.viewState.value.preview?.id)
        assertTrue(player.isMuted)
        assertEquals(listOf(FeedItem("hero", clipUrl("hero"))), player.items)
        assertTrue(player.isActive)
        assertTrue(player.state.value.playWhenReady)
    }

    @Test
    fun `changing chip changes the grid but keeps the preview`() = runTest {
        val vm = viewModel()
        vm.visible()
        vm.processIntent(BrowseIntent.SelectChip("romance"))
        assertEquals(listOf("a", "d"), vm.viewState.value.grid.map { it.id })
        assertEquals("hero", vm.viewState.value.preview?.id)
        assertEquals(listOf(FeedItem("hero", clipUrl("hero"))), player.items)
    }

    @Test
    fun `tapping another poster swaps the preview, tapping it again opens the show`() = runTest {
        val vm = viewModel()
        vm.visible()
        vm.processIntent(BrowseIntent.SelectPoster("d"))
        assertEquals("d", vm.viewState.value.preview?.id)
        assertEquals(clipUrl("d"), player.items.single().url)
        vm.singleEvent.test {
            vm.processIntent(BrowseIntent.SelectPoster("d"))
            assertEquals(BrowseSingleEvent.OpenShow("d"), awaitItem())
            vm.processIntent(BrowseIntent.PlayEpisode)
            assertEquals(BrowseSingleEvent.OpenPlayer("d", 1), awaitItem())
        }
    }

    @Test
    fun `wifi-only on a metered network shows the poster and holds no decoder`() = runTest {
        connectivity.isUnmetered.value = false
        val vm = viewModel()
        vm.visible()
        assertFalse(vm.viewState.value.autoplay)
        assertFalse(player.isActive)
        // Chạm vào preview thì chạy.
        vm.processIntent(BrowseIntent.PlayPreview)
        assertTrue(player.isActive)
        // Rời Wi-Fi giữa chừng với autoplay: dừng.
        connectivity.isUnmetered.value = true
        vm.processIntent(BrowseIntent.SelectPoster("a"))
        connectivity.isUnmetered.value = false
        assertFalse(player.isActive)
    }

    @Test
    fun `hidden tab or stopped screen frees the player`() = runTest {
        val vm = viewModel()
        vm.visible()
        assertTrue(player.isActive)
        vm.processIntent(BrowseIntent.TabVisible(false))
        assertFalse(player.isActive)
        vm.processIntent(BrowseIntent.TabVisible(true))
        assertTrue(player.isActive)
        vm.processIntent(BrowseIntent.ScreenStopped())
        assertFalse(player.isActive)
    }

    @Test
    fun `rotation keeps the preview playing`() = runTest {
        val vm = viewModel()
        vm.visible()
        vm.processIntent(BrowseIntent.ScreenStopped(isChangingConfigurations = true))
        assertTrue(player.isActive)
    }

    @Test
    fun `preview meta uses the episode file total once the clip is loaded`() = runTest {
        val vm = viewModel()
        vm.visible()
        assertEquals(3, vm.viewState.value.previewEpisodeTotal)
    }

    @Test
    fun `failed episode file is retried when the user taps the preview`() = runTest {
        val episodes = catalogRepository.episodes
        catalogRepository.episodes = emptyMap()
        val vm = viewModel()
        vm.visible()
        assertFalse(player.isActive)
        catalogRepository.episodes = episodes
        vm.processIntent(BrowseIntent.PlayPreview)
        assertEquals(listOf(FeedItem("hero", clipUrl("hero"))), player.items)
        assertTrue(player.isActive)
    }

    @Test
    fun `save toggles the preview show`() = runTest {
        val vm = viewModel()
        vm.visible()
        vm.processIntent(BrowseIntent.ToggleSave)
        assertTrue(vm.viewState.value.isSaved)
        vm.processIntent(BrowseIntent.ToggleSave)
        assertFalse(vm.viewState.value.isSaved)
    }

    @Test
    fun `chip and preview survive process death`() = runTest {
        val vm = viewModel()
        vm.processIntent(BrowseIntent.SelectChip("romance"))
        vm.processIntent(BrowseIntent.SelectPoster("a"))
        val restored = viewModel().viewState.value
        assertEquals("romance", restored.selectedChip)
        assertEquals("a", restored.preview?.id)
    }
}
