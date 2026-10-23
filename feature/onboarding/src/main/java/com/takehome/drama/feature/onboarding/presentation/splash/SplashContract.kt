package com.takehome.drama.feature.onboarding.presentation.splash

import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState

/** Splash không có trạng thái hiển thị nào đổi theo dữ liệu (không progress bar — design). */
data object SplashViewState : ViewState

/** Không có hành động của user: splash tự đi tiếp. */
sealed interface SplashIntent : ViewIntent

sealed interface SplashSingleEvent : SingleEvent {
    data object GoOnboarding : SplashSingleEvent
    data object GoHome : SplashSingleEvent
}
