package com.takehome.drama.feature.main.presentation.language

import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState

data class LanguageViewState(
    val locale: AppLocale = AppLocale.System,
) : ViewState

sealed interface LanguageIntent : ViewIntent {
    data object BackClick : LanguageIntent
    data class SelectLocale(val locale: AppLocale) : LanguageIntent
}

sealed interface LanguageSingleEvent : SingleEvent {
    data object NavigateBack : LanguageSingleEvent
}
