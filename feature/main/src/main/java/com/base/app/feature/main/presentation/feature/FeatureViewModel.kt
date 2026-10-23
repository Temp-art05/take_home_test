package com.base.app.feature.main.presentation.feature

import androidx.lifecycle.viewModelScope
import com.base.app.core.ui.foundation.vm.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FeatureViewModel : BaseViewModel<FeatureViewState, FeatureSingleEvent>() {

    // Load ở init chứ không ở LaunchedEffect của screen: screen compose lại không kéo theo load lại.
    init {
        load()
    }

    override fun initState() = FeatureViewState()

    fun onRefreshClick() {
        load()
    }

    private fun load() {
        if (currentState.isLoading) return
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            delay(1_000) // việc async mẫu — thay bằng use case thật
            updateState { copy(isLoading = false, loadCount = loadCount + 1) }
        }
    }
}
