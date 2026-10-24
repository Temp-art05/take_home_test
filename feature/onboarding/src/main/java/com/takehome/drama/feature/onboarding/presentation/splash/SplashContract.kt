package com.takehome.drama.feature.onboarding.presentation.splash

import androidx.annotation.StringRes
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState

/** Splash không có trạng thái hiển thị nào đổi theo dữ liệu (không progress bar — design). */
data object SplashViewState : ViewState

/** Splash tự đi tiếp; hành động duy nhất là easter egg tap logo để bật/tắt log ads. */
sealed interface SplashIntent : ViewIntent {
    /** [atMs]: thời điểm tap (đồng hồ đơn điệu) — VM đếm chuỗi tap liên tiếp. */
    data class LogoTapped(val atMs: Long) : SplashIntent
}

sealed interface SplashSingleEvent : SingleEvent {
    data object GoOnboarding : SplashSingleEvent
    data object GoHome : SplashSingleEvent
    data class ShowToast(@StringRes val messageRes: Int) : SplashSingleEvent
}
