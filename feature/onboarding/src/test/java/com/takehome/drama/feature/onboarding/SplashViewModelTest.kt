package com.takehome.drama.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.domain.fake.show
import com.takehome.drama.domain.model.UserPreferences
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.onboarding.IsOnboardingDoneUseCase
import com.takehome.drama.feature.onboarding.presentation.splash.SplashSingleEvent
import com.takehome.drama.feature.onboarding.presentation.splash.SplashViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SplashViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val catalogRepository = FakeCatalogRepository(Result.success(catalog(listOf(show("a")))))
    private val preferences = FakeUserPreferencesRepository(UserPreferences())
    private val remoteConfig = FakeRemoteConfigSource()

    private fun viewModel(hold: Boolean = false) = SplashViewModel(
        savedStateHandle = SavedStateHandle(mapOf("holdForDebug" to hold)),
        getCatalog = GetCatalogUseCase(catalogRepository),
        isOnboardingDone = IsOnboardingDoneUseCase(preferences),
        remoteConfig = remoteConfig,
    )

    @Test
    fun `first launch goes to onboarding once catalogue and remote config are in`() = runTest(mainRule.dispatcher) {
        remoteConfig.gate.complete(true)
        viewModel().singleEvent.test {
            assertEquals(SplashSingleEvent.GoOnboarding, awaitItem())
        }
        assertEquals(1, catalogRepository.catalogCalls)
    }

    @Test
    fun `finished onboarding goes straight home`() = runTest(mainRule.dispatcher) {
        preferences.state.value = UserPreferences(onboardingDone = true)
        remoteConfig.gate.complete(true)
        viewModel().singleEvent.test {
            assertEquals(SplashSingleEvent.GoHome, awaitItem())
        }
    }

    @Test
    fun `a hanging remote config never holds the splash longer than its timeout`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.singleEvent.test {
            advanceTimeBy(2_900)
            expectNoEvents()
            advanceTimeBy(200)
            assertEquals(SplashSingleEvent.GoOnboarding, awaitItem())
        }
    }

    @Test
    fun `splash waits for the catalogue but a failed catalogue still moves on`() = runTest(mainRule.dispatcher) {
        val gate = CompletableDeferred<Unit>()
        catalogRepository.catalogGate = gate
        catalogRepository.catalog = Result.failure(Exception("offline"))
        remoteConfig.gate.complete(false)
        viewModel().singleEvent.test {
            advanceTimeBy(1_000)
            expectNoEvents()
            gate.complete(Unit)
            assertEquals(SplashSingleEvent.GoOnboarding, awaitItem())
        }
    }

    @Test
    fun `a catalogue that never answers is capped too`() = runTest(mainRule.dispatcher) {
        catalogRepository.catalogGate = CompletableDeferred()
        remoteConfig.gate.complete(true)
        viewModel().singleEvent.test {
            advanceTimeBy(SplashViewModel.MAX_WAIT_MS + 1)
            assertEquals(SplashSingleEvent.GoOnboarding, awaitItem())
        }
    }

    @Test
    fun `debug hold keeps the splash on screen`() = runTest(mainRule.dispatcher) {
        remoteConfig.gate.complete(true)
        viewModel(hold = true).singleEvent.test {
            advanceTimeBy(20_000)
            expectNoEvents()
        }
        assertEquals(0, catalogRepository.catalogCalls)
    }
}
