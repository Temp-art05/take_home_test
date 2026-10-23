package com.takehome.drama.feature.main.presentation.main

import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.feature.main.presentation.main.model.MainTab

class MainViewModel : BaseViewModel<MainViewState, MainSingleEvent>() {

    override fun initState() = MainViewState()

    fun selectTab(tab: MainTab) {
        if (tab == currentState.selectedTab) return
        updateState { copy(selectedTab = tab, visitedTabs = visitedTabs + tab) }
    }
}
