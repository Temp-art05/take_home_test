package com.takehome.drama.feature.main.presentation.settings

import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewState

data class SettingsViewState(
    val locale: AppLocale = AppLocale.System,
) : ViewState

sealed interface SettingsSingleEvent : SingleEvent {
    data object NavigateBack : SettingsSingleEvent
}
