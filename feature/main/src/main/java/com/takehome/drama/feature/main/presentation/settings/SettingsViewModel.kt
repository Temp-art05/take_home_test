package com.takehome.drama.feature.main.presentation.settings

import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel

class SettingsViewModel : BaseViewModel<SettingsViewState, SettingsSingleEvent>() {

    override fun initState() = SettingsViewState(locale = AppLocale.current())

    fun onBackClick() {
        sendEvent(SettingsSingleEvent.NavigateBack)
    }

    fun selectLocale(locale: AppLocale) {
        updateState { copy(locale = locale) }
        // Hệ thống sẽ recreate activity với locale mới; state ở trên chỉ để radio đổi ngay.
        AppLocale.apply(locale)
    }
}
