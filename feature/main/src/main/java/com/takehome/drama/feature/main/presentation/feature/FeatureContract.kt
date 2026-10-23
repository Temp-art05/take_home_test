package com.takehome.drama.feature.main.presentation.feature

import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewState

/** `loadCount` tăng mỗi lần load thật — đổi tab qua lại mà số không đổi là tab không bị load lại. */
data class FeatureViewState(
    val isLoading: Boolean = false,
    val loadCount: Int = 0,
) : ViewState

sealed interface FeatureSingleEvent : SingleEvent
