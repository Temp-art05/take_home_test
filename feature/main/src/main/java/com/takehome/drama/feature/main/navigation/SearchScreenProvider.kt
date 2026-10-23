package com.takehome.drama.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import com.takehome.drama.core.designsystem.component.ScreenSkeleton
import com.takehome.drama.core.designsystem.component.SkeletonAction
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.core.SkeletonViewModel
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.presentation.SAMPLE_SHOW_ID
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

/** SCR-0201 — khung, UI thật ở Phase 4. */
class SearchScreenProvider(
    private val watchRoutes: WatchRoutes,
) : ScreenProvider<SearchScreenProvider.Search, SkeletonViewModel>() {

    @Serializable
    data object Search : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: SkeletonViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        ScreenSkeleton(
            code = "SCR-0201",
            title = stringResource(R.string.search_title),
            onBack = { onNavigationRequest(NavigationRequest.PopBackStack) },
            actions = listOf(
                SkeletonAction(stringResource(R.string.skeleton_open_show, SAMPLE_SHOW_ID)) {
                    onNavigationRequest(NavigationRequest.NavigateTo(watchRoutes.showDetail(SAMPLE_SHOW_ID)))
                },
            ),
        )
    }
}
