package com.takehome.drama.feature.onboarding.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import com.takehome.drama.core.designsystem.component.ScreenSkeleton
import com.takehome.drama.core.designsystem.component.SkeletonAction
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.core.SkeletonViewModel
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.feature.onboarding.R
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

/** SCR-0001 — khung, UI thật ở Phase 4. */
class SplashScreenProvider(
    private val home: Destination,
) : ScreenProvider<SplashScreenProvider.Splash, SkeletonViewModel>() {

    @Serializable
    data object Splash : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: SkeletonViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        ScreenSkeleton(
            code = "SCR-0001",
            title = stringResource(R.string.onboarding_splash_title),
            onBack = null,
            actions = listOf(
                SkeletonAction(stringResource(R.string.onboarding_continue)) {
                    onNavigationRequest(
                        NavigationRequest.NavigateToAndClearBackStack(OnboardingLanguageScreenProvider.OnboardingLanguage)
                    )
                },
                SkeletonAction(stringResource(R.string.onboarding_skip_to_home)) {
                    onNavigationRequest(NavigationRequest.NavigateToAndClearBackStack(home))
                },
            ),
        )
    }
}
