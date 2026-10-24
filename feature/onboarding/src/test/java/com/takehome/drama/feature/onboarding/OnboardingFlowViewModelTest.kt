package com.takehome.drama.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.takehome.drama.core.ads.FakeInterstitialAdManager
import com.takehome.drama.core.ads.interstitial.InterstitialAdManager
import com.takehome.drama.domain.fake.FakeAdsConfigRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.fake.testAppConfig
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.model.ads.AdsConfig
import com.takehome.drama.domain.model.ads.PlacementConfig
import com.takehome.drama.domain.usecase.ads.ObserveAdPlacementUseCase
import com.takehome.drama.feature.onboarding.presentation.flow.OnboardingFlowIntent
import com.takehome.drama.feature.onboarding.presentation.flow.OnboardingFlowSingleEvent
import com.takehome.drama.feature.onboarding.presentation.flow.OnboardingFlowViewModel
import com.takehome.drama.feature.onboarding.presentation.flow.OnboardingStep
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OnboardingFlowViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val ads = FakeInterstitialAdManager()
    private val adsConfig = FakeAdsConfigRepository()

    private fun viewModel(startStep: Int = 0, savedState: SavedStateHandle = SavedStateHandle(mapOf("startStep" to startStep))) =
        OnboardingFlowViewModel(savedState, ads, ObserveAdPlacementUseCase(adsConfig, testAppConfig))

    private fun interstitialOff() {
        adsConfig.config.value = AdsConfig(placements = mapOf(AdPlacement.InterstitialOnboarding.id to PlacementConfig(false)))
    }

    @Test
    fun `interstitial is preloaded when the flow opens so the end never waits for it`() {
        viewModel()
        assertEquals(listOf(InterstitialAdManager.AdType.ONBOARDING), ads.preloaded)
    }

    @Test
    fun `finish asks the screen for the interstitial exactly once`() = runTest {
        val vm = viewModel(startStep = 3)
        vm.singleEvent.test {
            vm.processIntent(OnboardingFlowIntent.Finish)
            vm.processIntent(OnboardingFlowIntent.Finish)
            assertEquals(OnboardingFlowSingleEvent.FinishWithAd, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `closing the interstitial navigates home exactly once`() = runTest {
        val vm = viewModel(startStep = 3)
        vm.singleEvent.test {
            vm.processIntent(OnboardingFlowIntent.Finish)
            assertEquals(OnboardingFlowSingleEvent.FinishWithAd, awaitItem())
            vm.processIntent(OnboardingFlowIntent.AdClosed)
            vm.processIntent(OnboardingFlowIntent.AdClosed)
            assertEquals(OnboardingFlowSingleEvent.NavigateHome, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `navigate home waits for a new collector when the activity is recreated during the ad`() = runTest {
        val vm = viewModel(startStep = 3)
        vm.singleEvent.test {
            vm.processIntent(OnboardingFlowIntent.Finish)
            assertEquals(OnboardingFlowSingleEvent.FinishWithAd, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        // Composition cũ đã huỷ; ad đóng khi chưa có ai collect → event phải chờ composition mới.
        vm.processIntent(OnboardingFlowIntent.AdClosed)
        vm.singleEvent.test {
            assertEquals(OnboardingFlowSingleEvent.NavigateHome, awaitItem())
        }
    }

    @Test
    fun `ad closed before finish is ignored`() = runTest {
        val vm = viewModel(startStep = 3)
        vm.singleEvent.test {
            vm.processIntent(OnboardingFlowIntent.AdClosed)
            expectNoEvents()
        }
    }

    @Test
    fun `starts at the first step and moves forward one step at a time`() {
        val vm = viewModel()
        assertEquals(OnboardingStep.Language, vm.viewState.value.step)
        assertFalse(vm.viewState.value.canGoBack)
        vm.processIntent(OnboardingFlowIntent.Next(from = OnboardingStep.Language))
        assertEquals(OnboardingStep.ValueProp, vm.viewState.value.step)
        assertTrue(vm.viewState.value.canGoBack)
    }

    @Test
    fun `a repeated next from a step already left is ignored`() {
        val vm = viewModel()
        vm.processIntent(OnboardingFlowIntent.Next(from = OnboardingStep.Language))
        vm.processIntent(OnboardingFlowIntent.Next(from = OnboardingStep.Language))
        assertEquals(OnboardingStep.ValueProp, vm.viewState.value.step)
    }

    @Test
    fun `back goes to the previous step and never before the first`() {
        val vm = viewModel(startStep = 2)
        assertEquals(OnboardingStep.Taste, vm.viewState.value.step)
        vm.processIntent(OnboardingFlowIntent.Back)
        assertEquals(OnboardingStep.ValueProp, vm.viewState.value.step)
        vm.processIntent(OnboardingFlowIntent.Back)
        vm.processIntent(OnboardingFlowIntent.Back)
        assertEquals(OnboardingStep.Language, vm.viewState.value.step)
    }

    @Test
    fun `next on the last step stays there`() {
        val vm = viewModel(startStep = 3)
        vm.processIntent(OnboardingFlowIntent.Next(from = OnboardingStep.NotificationPrompt))
        assertEquals(OnboardingStep.NotificationPrompt, vm.viewState.value.step)
    }

    @Test
    fun `out of range start step is clamped`() {
        assertEquals(OnboardingStep.NotificationPrompt, viewModel(startStep = 9).viewState.value.step)
    }

    @Test
    fun `current step survives process death`() {
        val savedState = SavedStateHandle(mapOf("startStep" to 0))
        viewModel(savedState = savedState).processIntent(OnboardingFlowIntent.Next(from = OnboardingStep.Language))
        assertEquals(OnboardingStep.ValueProp, viewModel(savedState = savedState).viewState.value.step)
    }

    @Test
    fun `interstitial off at open - nothing preloaded`() {
        interstitialOff()
        viewModel()
        assertEquals(emptyList<InterstitialAdManager.AdType>(), ads.preloaded)
    }

    @Test
    fun `remote config turning the interstitial on later preloads it then`() {
        interstitialOff()
        viewModel()
        adsConfig.config.value = AdsConfig()
        assertEquals(listOf(InterstitialAdManager.AdType.ONBOARDING), ads.preloaded)
    }

    @Test
    fun `kill switch or blocked version at open - nothing preloaded`() {
        adsConfig.config.value = AdsConfig(enable = false)
        viewModel()
        adsConfig.config.value = AdsConfig(disabledVersions = listOf(testAppConfig.versionName))
        viewModel()
        assertEquals(emptyList<InterstitialAdManager.AdType>(), ads.preloaded)
    }
}
