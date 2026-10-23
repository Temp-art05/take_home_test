package com.takehome.drama.feature.onboarding.presentation.language

import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState
import com.takehome.drama.domain.usecase.onboarding.LanguageOption

data class OnboardingLanguageViewState(
    val isLoading: Boolean = true,
    /** Mục đang chọn ghim đầu (đã sắp ở domain). */
    val options: List<LanguageOption> = emptyList(),
    val selected: String = "",
    /** Poster show hero làm key art mờ phía sau; null = chỉ nền gradient. */
    val posterUrl: String? = null,
    val artSeed: String = "",
) : ViewState

sealed interface OnboardingLanguageIntent : ViewIntent {
    data class Select(val code: String) : OnboardingLanguageIntent
    data object Continue : OnboardingLanguageIntent
}

sealed interface OnboardingLanguageSingleEvent : SingleEvent {
    /** [locale] = ngôn ngữ UI áp ngay trước khi sang bước sau (cùng mã với ngôn ngữ nội dung đã lưu). */
    data class Next(val locale: AppLocale) : OnboardingLanguageSingleEvent
}
