package com.takehome.drama.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.feature.onboarding.presentation.flow.OnboardingFlowIntent
import com.takehome.drama.feature.onboarding.presentation.flow.OnboardingFlowViewModel
import com.takehome.drama.feature.onboarding.presentation.flow.OnboardingStep
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

    private fun viewModel(startStep: Int = 0, savedState: SavedStateHandle = SavedStateHandle(mapOf("startStep" to startStep))) =
        OnboardingFlowViewModel(savedState)

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
}
