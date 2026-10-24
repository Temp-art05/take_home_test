package com.takehome.drama.feature.main

import androidx.lifecycle.SavedStateHandle
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.feature.main.presentation.main.MainIntent
import com.takehome.drama.feature.main.presentation.main.MainTabRequests
import com.takehome.drama.feature.main.presentation.main.MainViewModel
import com.takehome.drama.feature.main.presentation.main.model.MainTab
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val tabRequests = MainTabRequests()

    private fun viewModel(initialTab: String? = null) = MainViewModel(
        savedStateHandle = SavedStateHandle(mapOf("initialTab" to initialTab)),
        mainTabRequests = tabRequests,
    )

    @Test
    fun `a tab request from a child screen switches the tab once`() = runTest {
        val vm = viewModel(initialTab = "Library")
        assertEquals(MainTab.Library, vm.viewState.value.selectedTab)
        tabRequests.request(MainTab.Browse)
        assertEquals(MainTab.Browse, vm.viewState.value.selectedTab)
        assertNull(tabRequests.pending.value)
        vm.processIntent(MainIntent.SelectTab(MainTab.Home))
        assertEquals(MainTab.Home, vm.viewState.value.selectedTab)
    }

    @Test
    fun `a request made before main exists is picked up when it starts`() = runTest {
        tabRequests.request(MainTab.Browse)
        assertEquals(MainTab.Browse, viewModel().viewState.value.selectedTab)
    }
}
