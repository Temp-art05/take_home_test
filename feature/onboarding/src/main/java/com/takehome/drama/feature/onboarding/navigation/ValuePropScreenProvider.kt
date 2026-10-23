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

/** SCR-0003 — khung, UI thật ở Phase 4. */
class ValuePropScreenProvider : ScreenProvider<ValuePropScreenProvider.ValueProp, SkeletonViewModel>() {

    @Serializable
    data object ValueProp : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: SkeletonViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        ScreenSkeleton(
            code = "SCR-0003",
            title = stringResource(R.string.onboarding_value_prop_title),
            onBack = { onNavigationRequest(NavigationRequest.PopBackStack) },
            actions = listOf(
                SkeletonAction(stringResource(R.string.onboarding_step_into_flow)) {
                    onNavigationRequest(NavigationRequest.NavigateTo(TastePickerScreenProvider.TastePicker))
                },
            ),
        )
    }
}
