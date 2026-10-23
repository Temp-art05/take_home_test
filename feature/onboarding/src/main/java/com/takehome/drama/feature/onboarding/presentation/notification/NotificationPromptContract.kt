package com.takehome.drama.feature.onboarding.presentation.notification

import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState

/** Màn tĩnh: ba khung giờ là nội dung cố định của design (SCR-0005). */
data object NotificationPromptViewState : ViewState

sealed interface NotificationPromptIntent : ViewIntent {
    data object TurnOn : NotificationPromptIntent
    data object NotNow : NotificationPromptIntent

    /** Kết quả dialog hệ thống — cho hay từ chối (kể cả từ chối vĩnh viễn) đều đi tiếp. */
    data class PermissionResult(val granted: Boolean) : NotificationPromptIntent
}

sealed interface NotificationPromptSingleEvent : SingleEvent {
    /** Bắn dialog `POST_NOTIFICATIONS` thật — chỉ sau khi user bấm "Turn on notifications". */
    data object RequestPermission : NotificationPromptSingleEvent

    /** Onboarding xong → Home. Phase 5: chỗ đặt interstitial `interstitial_onboarding`. */
    data object Finish : NotificationPromptSingleEvent
}
