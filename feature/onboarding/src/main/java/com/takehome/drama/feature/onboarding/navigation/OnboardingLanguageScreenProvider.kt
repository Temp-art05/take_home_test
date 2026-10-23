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

/** SCR-0002 — khung, UI thật ở Phase 4. */
class OnboardingLanguageScreenProvider : ScreenProvider<OnboardingLanguageScreenProvider.OnboardingLanguage, SkeletonViewModel>() {

    @Serializable
    data object OnboardingLanguage : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: SkeletonViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        ScreenSkeleton(
            code = "SCR-0002",
            title = stringResource(R.string.onboarding_language_title),
            onBack = null,
            actions = listOf(
                SkeletonAction(stringResource(R.string.onboarding_continue)) {
                    onNavigationRequest(NavigationRequest.NavigateTo(ValuePropScreenProvider.ValueProp))
                },
            ),
        )
    }
}
