package com.base.app.feature.main.presentation.main

import com.base.app.core.ui.foundation.vm.BaseViewModel
import com.base.app.feature.main.presentation.main.model.MainTab

class MainViewModel : BaseViewModel<MainViewState, MainSingleEvent>() {

    override fun initState() = MainViewState()

    fun selectTab(tab: MainTab) {
        if (tab == currentState.selectedTab) return
        updateState { copy(selectedTab = tab, visitedTabs = visitedTabs + tab) }
    }
}
