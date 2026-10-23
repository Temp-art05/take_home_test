package com.takehome.drama.feature.main.presentation.language

import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel

class LanguageViewModel : BaseViewModel<LanguageIntent, LanguageViewState, LanguageSingleEvent>() {

    override fun initState() = LanguageViewState(locale = AppLocale.current())

    override fun processIntent(intent: LanguageIntent) {
        when (intent) {
            LanguageIntent.BackClick -> navigateBack()
            is LanguageIntent.SelectLocale -> selectLocale(intent.locale)
        }
    }

    private fun navigateBack() {
        sendEvent(LanguageSingleEvent.NavigateBack)
    }

    private fun selectLocale(locale: AppLocale) {
        updateState { copy(locale = locale) }
        // Hệ thống sẽ recreate activity với locale mới; state ở trên chỉ để radio đổi ngay.
        AppLocale.apply(locale)
    }
}
