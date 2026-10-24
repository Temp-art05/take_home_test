package com.takehome.drama.feature.main.presentation.language

import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel

/**
 * SCR-0502. VM quyết no-op / chống bấm lặp; Screen gọi `AppLocale.apply` (cần Activity AppCompat).
 * VM sống qua lần Activity dựng lại, nên cờ [LanguageViewState.applying] được gỡ ở `ScreenStarted`, hoặc ở
 * `ApplyFinished` khi ngôn ngữ hiển thị không đổi (không chắc có lần dựng lại).
 */
class LanguageViewModel : BaseViewModel<LanguageIntent, LanguageViewState, LanguageSingleEvent>() {

    override fun initState() = LanguageViewState(selected = AppLocale.current())

    override fun processIntent(intent: LanguageIntent) {
        when (intent) {
            LanguageIntent.BackClick -> sendEvent(LanguageSingleEvent.NavigateBack)
            is LanguageIntent.SelectLocale -> selectLocale(intent.locale)
            LanguageIntent.ScreenStarted -> updateState { copy(selected = AppLocale.current(), applying = false) }
            LanguageIntent.ApplyFinished -> updateState { copy(applying = false) }
        }
    }

    private fun selectLocale(locale: AppLocale) {
        val state = currentState
        if (locale == state.selected || state.applying) return
        // ✓ đổi ngay; hệ thống dựng lại Activity với locale mới.
        updateState { copy(selected = locale, applying = true) }
        sendEvent(LanguageSingleEvent.ApplyLocale(locale))
    }
}
