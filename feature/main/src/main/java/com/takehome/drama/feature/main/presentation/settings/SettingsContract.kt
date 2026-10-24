package com.takehome.drama.feature.main.presentation.settings

import androidx.annotation.StringRes
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState
import com.takehome.drama.domain.model.ThemeMode

data class SettingsViewState(
    val theme: ThemeMode = ThemeMode.Dark,
    val versionName: String = "",
    val versionCode: Int = 0,
) : ViewState

sealed interface SettingsIntent : ViewIntent {
    data object OpenLanguage : SettingsIntent
    data class SelectTheme(val theme: ThemeMode) : SettingsIntent
    data object OpenTerms : SettingsIntent
    data object OpenPrivacy : SettingsIntent
    data object OpenLicenses : SettingsIntent
}

sealed interface SettingsSingleEvent : SingleEvent {
    data object OpenLanguage : SettingsSingleEvent
    data object OpenLicenses : SettingsSingleEvent
    data class ShowToast(@param:StringRes val messageRes: Int, val success: Boolean) : SettingsSingleEvent
}
