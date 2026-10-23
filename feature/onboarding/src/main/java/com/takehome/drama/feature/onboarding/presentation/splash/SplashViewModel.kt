package com.takehome.drama.feature.onboarding.presentation.splash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.takehome.drama.core.analytics.remoteconfig.RemoteConfigSource
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.onboarding.IsOnboardingDoneUseCase
import com.takehome.drama.feature.onboarding.navigation.SplashScreenProvider.Splash
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * SCR-0001: chờ **cả** catalogue (nạp cache in-memory cho Home) **và** Remote Config — RC có trần
 * [RC_TIMEOUT_MS], cả lượt có trần [MAX_WAIT_MS], nên splash không bao giờ treo. Catalogue lỗi vẫn đi
 * tiếp: màn sau tự hiện trạng thái lỗi + Try again (0603).
 */
class SplashViewModel(
    savedStateHandle: SavedStateHandle,
    private val getCatalog: GetCatalogUseCase,
    private val isOnboardingDone: IsOnboardingDoneUseCase,
    private val remoteConfig: RemoteConfigSource,
) : BaseViewModel<SplashIntent, SplashViewState, SplashSingleEvent>() {

    private val holdForDebug = savedStateHandle.toRoute<Splash>().holdForDebug

    override fun initState() = SplashViewState

    init {
        if (!holdForDebug) start()
    }

    override fun processIntent(intent: SplashIntent) = Unit

    private fun start() {
        viewModelScope.launch {
            withTimeoutOrNull(MAX_WAIT_MS) {
                val catalog = async { getCatalog() }
                val config = async { remoteConfig.awaitFetch(RC_TIMEOUT_MS) }
                catalog.await()
                config.await()
            }
            sendEvent(if (isOnboardingDone()) SplashSingleEvent.GoHome else SplashSingleEvent.GoOnboarding)
        }
    }

    companion object {
        const val RC_TIMEOUT_MS = 3_000L
        const val MAX_WAIT_MS = 8_000L
    }
}
