package com.takehome.drama.feature.main.presentation.language

import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState

/**
 * @param applying đã gửi lệnh đổi ngôn ngữ, đang chờ Activity dựng lại — tap tiếp bị bỏ qua (mỗi lần
 * đổi là một lần recreate, spam tap là nháy liên tục).
 */
data class LanguageViewState(
    val selected: AppLocale = AppLocale.System,
    val applying: Boolean = false,
) : ViewState

sealed interface LanguageIntent : ViewIntent {
    data object BackClick : LanguageIntent
    data class SelectLocale(val locale: AppLocale) : LanguageIntent

    /** `ON_START` — cũng là lúc Activity vừa dựng lại sau khi đổi ngôn ngữ. */
    data object ScreenStarted : LanguageIntent

    /** Đã áp xong mà ngôn ngữ hiển thị không đổi — Activity có thể không dựng lại, gỡ cờ ngay. */
    data object ApplyFinished : LanguageIntent
}

sealed interface LanguageSingleEvent : SingleEvent {
    data object NavigateBack : LanguageSingleEvent
    data class ApplyLocale(val locale: AppLocale) : LanguageSingleEvent
}
