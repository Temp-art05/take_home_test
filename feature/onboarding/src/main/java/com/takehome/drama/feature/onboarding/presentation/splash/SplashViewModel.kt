package com.takehome.drama.feature.onboarding.presentation.splash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.takehome.drama.core.ads.AdsLogToggle
import com.takehome.drama.core.analytics.remoteconfig.RemoteConfigSource
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.onboarding.IsOnboardingDoneUseCase
import com.takehome.drama.feature.onboarding.R
import com.takehome.drama.feature.onboarding.navigation.SplashScreenProvider.Splash
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * SCR-0001: hiện tối thiểu [MIN_SHOW_MS], chờ **cả** catalogue (nạp cache in-memory cho Home) **và** Remote Config — RC có trần
 * [RC_TIMEOUT_MS], cả lượt có trần [MAX_WAIT_MS], nên splash không bao giờ treo. Catalogue lỗi vẫn đi
 * tiếp: màn sau tự hiện trạng thái lỗi + Try again (0603).
 *
 * Easter egg như HomeMorph: tap logo [LOG_TAPS] lần, mỗi lần cách nhau ≤ [LOG_TAP_RESET_MS] → bật/tắt
 * log ads (Logcat `FOR_TEST_ADS`). Chỉ khi [AdsLogToggle.canToggle] (debug, hoặc RC `enable_log_ads`).
 */
class SplashViewModel(
    savedStateHandle: SavedStateHandle,
    private val getCatalog: GetCatalogUseCase,
    private val isOnboardingDone: IsOnboardingDoneUseCase,
    private val remoteConfig: RemoteConfigSource,
    private val adsLogToggle: AdsLogToggle,
) : BaseViewModel<SplashIntent, SplashViewState, SplashSingleEvent>() {

    private val holdForDebug = savedStateHandle.toRoute<Splash>().holdForDebug
    private var logTaps = 0
    private var lastLogTapAt = Long.MIN_VALUE

    override fun initState() = SplashViewState

    init {
        if (!holdForDebug) start()
    }

    override fun processIntent(intent: SplashIntent) {
        when (intent) {
            is SplashIntent.LogoTapped -> onLogoTapped(intent.atMs)
        }
    }

    private fun onLogoTapped(atMs: Long) {
        if (lastLogTapAt == Long.MIN_VALUE || atMs - lastLogTapAt > LOG_TAP_RESET_MS) logTaps = 0
        lastLogTapAt = atMs
        logTaps++
        if (logTaps < LOG_TAPS || !adsLogToggle.canToggle()) return
        logTaps = 0
        val on = adsLogToggle.toggle()
        sendEvent(SplashSingleEvent.ShowToast(if (on) R.string.onboarding_splash_ad_logs_on else R.string.onboarding_splash_ad_logs_off))
    }

    private fun start() {
        viewModelScope.launch {
            // Splash ở lại tối thiểu MIN_SHOW_MS: không loé qua, và còn kịp tap logo 7 lần bật log ads.
            val minShow = async { delay(MIN_SHOW_MS) }
            withTimeoutOrNull(MAX_WAIT_MS) {
                val catalog = async { getCatalog() }
                val config = async { remoteConfig.awaitFetch(RC_TIMEOUT_MS) }
                catalog.await()
                config.await()
            }
            minShow.await()
            sendEvent(if (isOnboardingDone()) SplashSingleEvent.GoHome else SplashSingleEvent.GoOnboarding)
        }
    }

    companion object {
        const val RC_TIMEOUT_MS = 3_000L
        const val MAX_WAIT_MS = 8_000L
        const val MIN_SHOW_MS = 1_000L
        const val LOG_TAPS = 7
        const val LOG_TAP_RESET_MS = 3_000L
    }
}
