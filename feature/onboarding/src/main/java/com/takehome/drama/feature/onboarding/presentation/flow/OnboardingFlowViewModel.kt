package com.takehome.drama.feature.onboarding.presentation.flow

import androidx.lifecycle.SavedStateHandle
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel

/**
 * Host của onboarding một màn: chỉ giữ bước hiện tại (pager trượt theo nó). Mỗi bước vẫn có VM riêng.
 * Bước ghi vào [SavedStateHandle] để sống qua process death; `startStep` (arg destination) chỉ
 * DebugScreenLauncher dùng để mở thẳng một bước.
 */
class OnboardingFlowViewModel(
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<OnboardingFlowIntent, OnboardingFlowViewState, OnboardingFlowSingleEvent>() {

    override fun initState(): OnboardingFlowViewState {
        val index = savedStateHandle[KEY_STEP] ?: savedStateHandle[KEY_START_STEP] ?: 0
        return OnboardingFlowViewState(step = OnboardingStep.entries[index.coerceIn(0, OnboardingStep.entries.lastIndex)])
    }

    override fun processIntent(intent: OnboardingFlowIntent) {
        when (intent) {
            is OnboardingFlowIntent.Next -> if (intent.from == currentState.step) move(+1)
            OnboardingFlowIntent.Back -> move(-1)
        }
    }

    private fun move(delta: Int) {
        val index = (currentState.step.ordinal + delta).coerceIn(0, OnboardingStep.entries.lastIndex)
        savedStateHandle[KEY_STEP] = index
        updateState { copy(step = OnboardingStep.entries[index]) }
    }

    private companion object {
        /** Tên property của `OnboardingScreenProvider.Onboarding`. */
        const val KEY_START_STEP = "startStep"
        const val KEY_STEP = "onboarding_step"
    }
}
