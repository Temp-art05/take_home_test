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
import com.takehome.drama.core.ads.interstitial.InterstitialAdManager
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.feature.onboarding.presentation.flow.language.OnboardingLanguageScreen
import com.takehome.drama.feature.onboarding.presentation.flow.notification.NotificationPromptScreen
import com.takehome.drama.feature.onboarding.presentation.flow.taste.TastePickerScreen
import com.takehome.drama.feature.onboarding.presentation.flow.valueprop.ValuePropScreen
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
    onOpenHome: () -> Unit,
    interstitialAdManager: InterstitialAdManager = koinInject(),
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
                if (activity != null) {
                    interstitialAdManager.showIfReady(activity, InterstitialAdManager.AdType.ONBOARDING, adClosed)
                } else {
                    adClosed()
                }
            }
            OnboardingFlowSingleEvent.NavigateHome -> onOpenHome()
        }
    }

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
            OnboardingStep.Language -> OnboardingLanguageScreen(
                viewModel = koinViewModel(),
                onNext = { viewModel.processIntent(OnboardingFlowIntent.Next(from = OnboardingStep.Language)) },
            )
            OnboardingStep.ValueProp -> ValuePropScreen(
                viewModel = koinViewModel(),
                onNext = { viewModel.processIntent(OnboardingFlowIntent.Next(from = OnboardingStep.ValueProp)) },
            )
            OnboardingStep.Taste -> TastePickerScreen(
                viewModel = koinViewModel(),
                onNext = { viewModel.processIntent(OnboardingFlowIntent.Next(from = OnboardingStep.Taste)) },
                onFinish = { viewModel.processIntent(OnboardingFlowIntent.Finish) },
            )
            OnboardingStep.NotificationPrompt -> NotificationPromptScreen(
                viewModel = koinViewModel(),
                onFinish = { viewModel.processIntent(OnboardingFlowIntent.Finish) },
            )
        }
    }
}
