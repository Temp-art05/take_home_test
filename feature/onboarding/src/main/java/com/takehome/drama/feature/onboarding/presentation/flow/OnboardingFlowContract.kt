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

    /** Bước cuối (Taste khi API < 33, Notification prompt khi 33+) đã lưu onboarding xong. */
    data object Finish : OnboardingFlowIntent

    /**
     * Interstitial đã đóng (hoặc không có). Đi qua VM chứ không gọi navigate thẳng trong callback:
     * Activity có thể bị dựng lại lúc ad đang hiện, NavController của composition cũ đã huỷ.
     */
    data object AdClosed : OnboardingFlowIntent
}

sealed interface OnboardingFlowSingleEvent : SingleEvent {
    /** Screen hiện interstitial `interstitial_onboarding` nếu đã load sẵn, đóng (hoặc không có) → Home. */
    data object FinishWithAd : OnboardingFlowSingleEvent

    /** Vào Home, xoá back stack onboarding. */
    data object NavigateHome : OnboardingFlowSingleEvent
}
