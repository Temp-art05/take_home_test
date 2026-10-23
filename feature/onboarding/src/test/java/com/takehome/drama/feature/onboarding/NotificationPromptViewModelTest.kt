package com.takehome.drama.feature.onboarding

import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.model.UserPreferences
import com.takehome.drama.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.takehome.drama.feature.onboarding.presentation.notification.NotificationPromptIntent
import com.takehome.drama.feature.onboarding.presentation.notification.NotificationPromptSingleEvent
import com.takehome.drama.feature.onboarding.presentation.notification.NotificationPromptViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotificationPromptViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val preferences = FakeUserPreferencesRepository(UserPreferences())
    private fun viewModel() = NotificationPromptViewModel(CompleteOnboardingUseCase(preferences))

    @Test
    fun `opening the screen never asks for the permission by itself`() = runTest {
        viewModel().singleEvent.test { expectNoEvents() }
        assertFalse(preferences.state.value.onboardingDone)
    }

    @Test
    fun `turn on asks the system dialog, then any answer finishes onboarding`() = runTest {
        listOf(true, false).forEach { granted ->
            preferences.state.value = UserPreferences()
            val vm = viewModel()
            vm.singleEvent.test {
                vm.processIntent(NotificationPromptIntent.TurnOn)
                assertEquals(NotificationPromptSingleEvent.RequestPermission, awaitItem())
                assertFalse(preferences.state.value.onboardingDone)
                vm.processIntent(NotificationPromptIntent.PermissionResult(granted))
                assertEquals(NotificationPromptSingleEvent.Finish, awaitItem())
            }
            assertTrue(preferences.state.value.onboardingDone)
        }
    }

    @Test
    fun `double tap on not now finishes once`() = runTest {
        val vm = viewModel()
        val gate = CompletableDeferred<Unit>()
        preferences.writeGate = gate
        vm.singleEvent.test {
            vm.processIntent(NotificationPromptIntent.NotNow)
            vm.processIntent(NotificationPromptIntent.NotNow)
            gate.complete(Unit)
            assertEquals(NotificationPromptSingleEvent.Finish, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `not now finishes without asking`() = runTest {
        val vm = viewModel()
        vm.singleEvent.test {
            vm.processIntent(NotificationPromptIntent.NotNow)
            assertEquals(NotificationPromptSingleEvent.Finish, awaitItem())
        }
        assertTrue(preferences.state.value.onboardingDone)
    }
}
