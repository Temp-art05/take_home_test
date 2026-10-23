package com.takehome.drama.feature.onboarding.presentation.flow

import androidx.lifecycle.SavedStateHandle
import com.takehome.drama.core.ads.AdsPresenter
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.ads.AdPlacement

/**
 * Host của onboarding một màn: chỉ giữ bước hiện tại (pager trượt theo nó). Mỗi bước vẫn có VM riêng.
 * Bước ghi vào [SavedStateHandle] để sống qua process death; `startStep` (arg destination) chỉ
 * DebugScreenLauncher dùng để mở thẳng một bước.
 *
 * Interstitial `interstitial_onboarding` load ngay khi flow mở (user còn 3–4 bước) để lúc kết thúc chỉ
 * hiện nếu đã sẵn — chưa sẵn thì vào Home luôn, không bắt user chờ ad.
 */
class OnboardingFlowViewModel(
    private val savedStateHandle: SavedStateHandle,
    adsPresenter: AdsPresenter,
) : BaseViewModel<OnboardingFlowIntent, OnboardingFlowViewState, OnboardingFlowSingleEvent>() {

    private var finished = false
    private var navigatedHome = false

    init {
        adsPresenter.preload(AdPlacement.InterstitialOnboarding)
    }

    override fun initState(): OnboardingFlowViewState {
        val index = savedStateHandle[KEY_STEP] ?: savedStateHandle[KEY_START_STEP] ?: 0
        return OnboardingFlowViewState(step = OnboardingStep.entries[index.coerceIn(0, OnboardingStep.entries.lastIndex)])
    }

    override fun processIntent(intent: OnboardingFlowIntent) {
        when (intent) {
            is OnboardingFlowIntent.Next -> if (intent.from == currentState.step) move(+1)
            OnboardingFlowIntent.Back -> move(-1)
            OnboardingFlowIntent.Finish -> if (!finished) {
                finished = true
                sendEvent(OnboardingFlowSingleEvent.FinishWithAd)
            }
            OnboardingFlowIntent.AdClosed -> if (finished && !navigatedHome) {
                navigatedHome = true
                sendEvent(OnboardingFlowSingleEvent.NavigateHome)
            }
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
