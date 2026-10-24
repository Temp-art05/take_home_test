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
import com.takehome.drama.core.ads.FakeAdsLogToggle
import com.takehome.drama.feature.onboarding.R
import com.takehome.drama.feature.onboarding.presentation.splash.SplashIntent
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
    private val logToggle = FakeAdsLogToggle()

    private fun viewModel(hold: Boolean = false) = SplashViewModel(
        savedStateHandle = SavedStateHandle(mapOf("holdForDebug" to hold)),
        getCatalog = GetCatalogUseCase(catalogRepository),
        isOnboardingDone = IsOnboardingDoneUseCase(preferences),
        remoteConfig = remoteConfig,
        adsLogToggle = logToggle,
    )

    /** [count] lần tap, mỗi lần cách nhau [gapMs]. */
    private fun SplashViewModel.tap(count: Int, startMs: Long = 1_000, gapMs: Long = 200) =
        repeat(count) { processIntent(SplashIntent.LogoTapped(atMs = startMs + it * gapMs)) }

    @Test
    fun `first launch goes to onboarding once catalogue and remote config are in`() = runTest(mainRule.dispatcher) {
        remoteConfig.gate.complete(true)
        viewModel().singleEvent.test {
            assertEquals(SplashSingleEvent.GoOnboarding, awaitItem())
        }
        assertEquals(1, catalogRepository.catalogCalls)
    }

    @Test
    fun `splash stays at least one second even when everything is instant - time for the 7 tap log toggle`() =
        runTest(mainRule.dispatcher) {
            remoteConfig.gate.complete(true)
            viewModel().singleEvent.test {
                advanceTimeBy(SplashViewModel.MIN_SHOW_MS - 1)
                expectNoEvents()
                advanceTimeBy(2)
                assertEquals(SplashSingleEvent.GoOnboarding, awaitItem())
            }
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

    @Test
    fun `seven quick logo taps toggle ad logs on, seven more toggle them off`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(hold = true)
        vm.singleEvent.test {
            vm.tap(6)
            expectNoEvents()
            vm.tap(1, startMs = 2_200)
            assertEquals(SplashSingleEvent.ShowToast(R.string.onboarding_splash_ad_logs_on), awaitItem())
            vm.tap(7, startMs = 3_000)
            assertEquals(SplashSingleEvent.ShowToast(R.string.onboarding_splash_ad_logs_off), awaitItem())
        }
        assertEquals(2, logToggle.toggles)
    }

    @Test
    fun `a pause longer than three seconds restarts the count`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(hold = true)
        vm.tap(6) // 1000 … 2000
        val resumed = 2_000 + SplashViewModel.LOG_TAP_RESET_MS + 1
        vm.tap(1, startMs = resumed)
        assertEquals(0, logToggle.toggles)
        vm.tap(6, startMs = resumed + 200)
        assertEquals(1, logToggle.toggles)
    }

    @Test
    fun `taps do nothing when toggling logs is not allowed`() = runTest(mainRule.dispatcher) {
        logToggle.allowed = false
        val vm = viewModel(hold = true)
        vm.singleEvent.test {
            vm.tap(14)
            expectNoEvents()
        }
        assertEquals(0, logToggle.toggles)
    }
}
