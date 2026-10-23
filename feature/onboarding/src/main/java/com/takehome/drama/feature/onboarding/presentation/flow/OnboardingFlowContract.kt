package com.takehome.drama.feature.onboarding.presentation.flow

import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState

/** 4 bước onboarding theo thứ tự trang pager (SCR-0002 … 0005). */
enum class OnboardingStep { Language, ValueProp, Taste, NotificationPrompt }

data class OnboardingFlowViewState(
    val step: OnboardingStep = OnboardingStep.Language,
) : ViewState {
    /** Bước đầu là gốc sau Splash: back hệ thống thoát app như trước. */
    val canGoBack: Boolean get() = step.ordinal > 0
}

sealed interface OnboardingFlowIntent : ViewIntent {
    /** Bước [from] xong. Gắn bước nguồn để event lặp từ trang vừa rời không nhảy thêm bước. */
    data class Next(val from: OnboardingStep) : OnboardingFlowIntent

    data object Back : OnboardingFlowIntent
}

/** Kết thúc onboarding (→ Home) do bước con điều hướng; host không có event riêng. */
sealed interface OnboardingFlowSingleEvent : SingleEvent
