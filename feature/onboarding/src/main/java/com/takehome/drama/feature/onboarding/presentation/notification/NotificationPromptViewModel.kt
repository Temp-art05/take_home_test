package com.takehome.drama.feature.onboarding.presentation.notification

import androidx.lifecycle.viewModelScope
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.usecase.onboarding.CompleteOnboardingUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * SCR-0005 (chỉ tới đây khi API 33+ và chưa có quyền). Hệ thống chỉ cho hỏi tối đa 2 lần, nên dialog
 * thật chỉ bắn khi user bấm — không bao giờ tự hỏi lúc vào màn.
 */
class NotificationPromptViewModel(
    private val completeOnboarding: CompleteOnboardingUseCase,
) : BaseViewModel<NotificationPromptIntent, NotificationPromptViewState, NotificationPromptSingleEvent>() {

    /** Bấm lặp "Not now" khi đang ghi DataStore → một lần Finish, không đẩy Home hai lần. */
    private var finishJob: Job? = null

    override fun initState() = NotificationPromptViewState

    override fun processIntent(intent: NotificationPromptIntent) {
        when (intent) {
            NotificationPromptIntent.TurnOn -> sendEvent(NotificationPromptSingleEvent.RequestPermission)
            is NotificationPromptIntent.PermissionResult -> finish()
            NotificationPromptIntent.NotNow -> finish()
        }
    }

    private fun finish() {
        if (finishJob?.isActive == true) return
        finishJob = viewModelScope.launch {
            completeOnboarding()
            sendEvent(NotificationPromptSingleEvent.Finish)
        }
    }
}
