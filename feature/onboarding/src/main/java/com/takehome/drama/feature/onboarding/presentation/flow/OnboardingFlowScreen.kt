package com.takehome.drama.feature.onboarding.presentation.flow

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.ads.AdsPresenter
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.feature.onboarding.presentation.language.OnboardingLanguageScreen
import com.takehome.drama.feature.onboarding.presentation.notification.NotificationPromptScreen
import com.takehome.drama.feature.onboarding.presentation.taste.TastePickerScreen
import com.takehome.drama.feature.onboarding.presentation.valueprop.ValuePropScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * SCR-0002 … 0005 trong **một** destination: 4 bước là 4 trang `HorizontalPager` trượt bằng code
 * (`userScrollEnabled = false` — chỉ CTA mới sang bước), thay cho 4 lần navigate. Mỗi trang lấy VM
 * riêng bằng `koinViewModel()` (cùng ViewModelStore của entry nên state giữ khi trượt qua lại).
 * Back hệ thống lùi một bước; ở bước đầu thì để hệ thống xử lý (thoát app như trước).
 * Kết thúc: interstitial `interstitial_onboarding` (chỉ khi đã load sẵn) rồi mới vào Home — ranh giới luồng
 * tự nhiên, user chưa xem dở gì.
 */
@Composable
fun OnboardingFlowScreen(
    viewModel: OnboardingFlowViewModel,
    home: Destination,
    onNavigate: (NavigationRequest) -> Unit,
    adsPresenter: AdsPresenter = koinInject(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = viewState.step.ordinal) { OnboardingStep.entries.size }
    LaunchedEffect(viewState.step) {
        if (pagerState.currentPage != viewState.step.ordinal) pagerState.animateScrollToPage(viewState.step.ordinal)
    }
    BackHandler(enabled = viewState.canGoBack) { viewModel.processIntent(OnboardingFlowIntent.Back) }

    val activity = LocalActivity.current
    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            OnboardingFlowSingleEvent.FinishWithAd -> {
                // Callback chỉ báo VM (VM sống qua recreate); điều hướng do composition đang sống làm.
                val adClosed = { viewModel.processIntent(OnboardingFlowIntent.AdClosed) }
                if (activity != null) adsPresenter.showInterstitialIfReady(activity, AdPlacement.InterstitialOnboarding, adClosed) else adClosed()
            }
            OnboardingFlowSingleEvent.NavigateHome -> onNavigate(NavigationRequest.NavigateToAndClearBackStack(home))
        }
    }

    val next = { step: OnboardingStep -> viewModel.processIntent(OnboardingFlowIntent.Next(from = step)) }
    val finish = { viewModel.processIntent(OnboardingFlowIntent.Finish) }
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = false,
        // Nền giữa hai trang khi trượt cùng màu nền của bước, không lộ khoảng trống.
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        key = { OnboardingStep.entries[it] },
    ) { page ->
        when (OnboardingStep.entries[page]) {
            OnboardingStep.Language ->
                OnboardingLanguageScreen(viewModel = koinViewModel(), onNext = { next(OnboardingStep.Language) })
            OnboardingStep.ValueProp ->
                ValuePropScreen(viewModel = koinViewModel(), onNext = { next(OnboardingStep.ValueProp) })
            OnboardingStep.Taste ->
                TastePickerScreen(viewModel = koinViewModel(), onNext = { next(OnboardingStep.Taste) }, onFinish = finish)
            OnboardingStep.NotificationPrompt ->
                NotificationPromptScreen(viewModel = koinViewModel(), onFinish = finish)
        }
    }
}
