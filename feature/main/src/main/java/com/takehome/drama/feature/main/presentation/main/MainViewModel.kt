package com.takehome.drama.feature.main.presentation.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.feature.main.navigation.MainScreenProvider
import com.takehome.drama.feature.main.presentation.main.model.MainTab
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class MainViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val mainTabRequests: MainTabRequests,
) : BaseViewModel<MainIntent, MainViewState, MainSingleEvent>() {

    init {
        // History/Saved "Find your next flow" → tab Browse khi quay về Main (kể cả Main mới dựng).
        viewModelScope.launch {
            mainTabRequests.pending.filterNotNull().collect { tab ->
                mainTabRequests.consume(tab)
                selectTab(tab)
            }
        }
    }

    override fun initState(): MainViewState {
        val initialTab = savedStateHandle.toRoute<MainScreenProvider.Main>().initialTab
            ?.let { name -> MainTab.entries.firstOrNull { it.name == name } }
            ?: MainTab.Home
        return MainViewState(selectedTab = initialTab, visitedTabs = setOf(initialTab))
    }

    override fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.SelectTab -> selectTab(intent.tab)
        }
    }

    private fun selectTab(tab: MainTab) {
        if (tab == currentState.selectedTab) return
        updateState { copy(selectedTab = tab, visitedTabs = visitedTabs + tab) }
    }
}
