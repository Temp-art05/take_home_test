package com.base.app.feature.main.presentation.settings

import com.base.app.core.ui.foundation.locale.AppLocale
import com.base.app.core.ui.foundation.vm.SingleEvent
import com.base.app.core.ui.foundation.vm.ViewState

data class SettingsViewState(
    val locale: AppLocale = AppLocale.System,
) : ViewState

sealed interface SettingsSingleEvent : SingleEvent {
    data object NavigateBack : SettingsSingleEvent
}
