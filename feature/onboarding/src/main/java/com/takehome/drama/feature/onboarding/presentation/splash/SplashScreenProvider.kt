package com.takehome.drama.feature.onboarding.presentation.splash

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.feature.onboarding.presentation.flow.OnboardingScreenProvider
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

/** SCR-0001 — chờ catalogue + Remote Config rồi vào onboarding hoặc Home. */
class SplashScreenProvider(
    private val home: Destination,
) : ScreenProvider<SplashScreenProvider.Splash, SplashViewModel>() {

    /** [holdForDebug] chỉ DebugScreenLauncher bật: đứng yên ở splash để chụp so mockup. */
    @Serializable
    data class Splash(val holdForDebug: Boolean = false) : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: SplashViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        SplashScreen(
            viewModel = viewModel,
            onOpenOnboarding = {
                onNavigationRequest(NavigationRequest.NavigateToAndClearBackStack(OnboardingScreenProvider.Onboarding()))
            },
            onOpenHome = { onNavigationRequest(NavigationRequest.NavigateToAndClearBackStack(home)) },
        )
    }
}
