package com.takehome.drama.feature.main

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.FakeSavedShowRepository
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.library.ObserveSavedShowsUseCase
import com.takehome.drama.domain.usecase.library.SavedFilter
import com.takehome.drama.domain.usecase.library.SetShowSavedUseCase
import com.takehome.drama.feature.main.presentation.main.MainTabRequests
import com.takehome.drama.feature.main.presentation.main.model.MainTab
import com.takehome.drama.feature.main.presentation.saved.SavedIntent
import com.takehome.drama.feature.main.presentation.saved.SavedSingleEvent
import com.takehome.drama.feature.main.presentation.saved.SavedViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SavedViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val catalogRepository = FakeCatalogRepository(Result.success(testCatalog))
    private val saved = FakeSavedShowRepository()
    private val tabRequests = MainTabRequests()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = SavedViewModel(
        savedStateHandle = handle,
        getCatalog = GetCatalogUseCase(catalogRepository),
        observeSavedShows = ObserveSavedShowsUseCase(saved),
        setShowSaved = SetShowSavedUseCase(saved),
        resolveContentLanguage = ResolveContentLanguageUseCase(),
        userPreferencesRepository = FakeUserPreferencesRepository(),
        mainTabRequests = tabRequests,
    )

    @Test
    fun `nothing saved is the 0602 empty state`() = runTest {
        val state = viewModel().viewState.value
        assertTrue(state.isShelfEmpty)
    }

    @Test
    fun `saved shows newest first with genre and count for all`() = runTest {
        saved.save("a")
        saved.save("c")
        val state = viewModel().viewState.value
        assertEquals(listOf("c", "a"), state.items.map { it.showId })
        assertEquals("Revenge", state.items.first().genre)
        assertEquals(2, state.totalCount)
    }

    @Test
    fun `a chip with no match is a small empty, not 0602, and does not reload the catalogue`() = runTest {
        saved.save("a")
        val vm = viewModel()
        vm.processIntent(SavedIntent.SelectFilter(SavedFilter.Airing))
        val state = vm.viewState.value
        assertTrue(state.items.isEmpty())
        assertFalse(state.isShelfEmpty)
        assertEquals(1, state.totalCount)
        assertEquals(1, catalogRepository.catalogCalls)
    }

    @Test
    fun `filter survives process death`() = runTest {
        val handle = SavedStateHandle()
        viewModel(handle).processIntent(SavedIntent.SelectFilter(SavedFilter.Complete))
        assertEquals(SavedFilter.Complete, viewModel(handle).viewState.value.filter)
    }

    @Test
    fun `remove unsaves once and toasts`() = runTest {
        saved.save("a")
        val vm = viewModel()
        vm.singleEvent.test {
            vm.processIntent(SavedIntent.Remove("a"))
            vm.processIntent(SavedIntent.Remove("a"))
            assertTrue((awaitItem() as SavedSingleEvent.ShowToast).success)
            expectNoEvents()
        }
        assertEquals(1, saved.removeCalls)
        assertTrue(vm.viewState.value.isShelfEmpty)
    }

    @Test
    fun `row opens the show and the empty cta goes to browse`() = runTest {
        val vm = viewModel()
        vm.singleEvent.test {
            vm.processIntent(SavedIntent.OpenRow("a"))
            assertEquals(SavedSingleEvent.OpenDetail("a"), awaitItem())
            vm.processIntent(SavedIntent.FindNext)
            assertEquals(SavedSingleEvent.GoBrowse, awaitItem())
        }
        assertEquals(MainTab.Browse, tabRequests.pending.value)
    }
}
