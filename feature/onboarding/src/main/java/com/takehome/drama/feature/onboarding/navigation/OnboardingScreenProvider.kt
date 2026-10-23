package com.takehome.drama.feature.onboarding.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.feature.onboarding.presentation.flow.OnboardingFlowScreen
import com.takehome.drama.feature.onboarding.presentation.flow.OnboardingFlowViewModel
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

/** SCR-0002 … 0005 — onboarding một màn, các bước trượt trong pager. */
class OnboardingScreenProvider(
    private val home: Destination,
) : ScreenProvider<OnboardingScreenProvider.Onboarding, OnboardingFlowViewModel>() {

    /** [startStep] (0…3) chỉ DebugScreenLauncher dùng để mở thẳng một bước. */
    @Serializable
    data class Onboarding(val startStep: Int = 0) : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: OnboardingFlowViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        OnboardingFlowScreen(viewModel = viewModel, home = home, onNavigate = onNavigationRequest)
    }
}
