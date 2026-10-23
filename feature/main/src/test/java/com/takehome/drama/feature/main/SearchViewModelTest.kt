package com.takehome.drama.feature.main

import androidx.lifecycle.SavedStateHandle
import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.search.GetPopularShowsUseCase
import com.takehome.drama.domain.usecase.search.SearchShowsUseCase
import com.takehome.drama.feature.main.presentation.search.SearchIntent
import com.takehome.drama.feature.main.presentation.search.SearchPhase
import com.takehome.drama.feature.main.presentation.search.SearchViewModel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SearchViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val savedState = SavedStateHandle(mapOf("initialQuery" to null))

    private fun viewModel(handle: SavedStateHandle = savedState) = SearchViewModel(
        savedStateHandle = handle,
        getCatalog = GetCatalogUseCase(FakeCatalogRepository(Result.success(testCatalog))),
        searchShows = SearchShowsUseCase(),
        getPopularShows = GetPopularShowsUseCase(),
        resolveContentLanguage = ResolveContentLanguageUseCase(),
        userPreferencesRepository = FakeUserPreferencesRepository(),
    )

    @Test
    fun `idle state lists genres and popular shows`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        runCurrent()
        val state = vm.viewState.value
        assertEquals(SearchPhase.Idle, state.phase)
        assertEquals(listOf("Romance", "Revenge"), state.genres)
        assertEquals("c", state.popular.first().id)
    }

    @Test
    fun `typing is debounced - only the settled query is searched`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.processIntent(SearchIntent.QueryChanged("c"))
        advanceTimeBy(100)
        vm.processIntent(SearchIntent.QueryChanged("ce"))
        advanceTimeBy(100)
        vm.processIntent(SearchIntent.QueryChanged("ceo"))
        advanceTimeBy(299)
        assertEquals(SearchPhase.Idle, vm.viewState.value.phase)
        advanceTimeBy(2)
        assertEquals(SearchPhase.Results, vm.viewState.value.phase)
        assertEquals(listOf("c"), vm.viewState.value.matches.map { it.id })
        assertEquals("ceo", vm.viewState.value.query)
    }

    @Test
    fun `clear goes back to idle immediately`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.processIntent(SearchIntent.FillQuery("Revenge"))
        assertEquals(SearchPhase.Results, vm.viewState.value.phase)
        vm.processIntent(SearchIntent.Clear)
        assertEquals(SearchPhase.Idle, vm.viewState.value.phase)
        assertEquals("", vm.viewState.value.query)
    }

    @Test
    fun `clear then retyping the same query within the debounce still searches`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.processIntent(SearchIntent.QueryChanged("ceo"))
        advanceTimeBy(301)
        assertEquals(SearchPhase.Results, vm.viewState.value.phase)
        vm.processIntent(SearchIntent.Clear)
        vm.processIntent(SearchIntent.QueryChanged("ceo"))
        advanceTimeBy(301)
        assertEquals(SearchPhase.Results, vm.viewState.value.phase)
        assertEquals(listOf("c"), vm.viewState.value.matches.map { it.id })
    }

    @Test
    fun `no match shows the empty state`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.processIntent(SearchIntent.QueryChanged("zzzz"))
        advanceTimeBy(SearchViewModel.DEBOUNCE_MS + 1)
        assertEquals(SearchPhase.NoResults, vm.viewState.value.phase)
    }

    @Test
    fun `query survives process death and is searched again`() = runTest(mainRule.dispatcher) {
        viewModel().processIntent(SearchIntent.QueryChanged("dawn"))
        val restored = viewModel()
        runCurrent()
        assertEquals("dawn", restored.viewState.value.query)
        assertEquals(listOf("d"), restored.viewState.value.matches.map { it.id })
    }

    @Test
    fun `debug launcher can open with a query`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(SavedStateHandle(mapOf("initialQuery" to "alpha")))
        runCurrent()
        assertEquals(listOf("a"), vm.viewState.value.matches.map { it.id })
    }
}
