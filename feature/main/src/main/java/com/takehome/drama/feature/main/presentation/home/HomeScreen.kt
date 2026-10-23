package com.takehome.drama.feature.main.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.takehome.drama.core.designsystem.component.ScreenSkeleton
import com.takehome.drama.core.designsystem.component.SkeletonAction
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.navigation.SearchScreenProvider
import com.takehome.drama.feature.main.presentation.SAMPLE_SHOW_ID

/** SCR-0101 — khung tab, UI thật ở Phase 4. */
@Composable
internal fun HomeScreen(
    watchRoutes: WatchRoutes,
    onNavigate: (NavigationRequest) -> Unit,
) {
    ScreenSkeleton(
        code = "SCR-0101",
        title = stringResource(R.string.home_title),
        onBack = null,
        actions = listOf(
            SkeletonAction(stringResource(R.string.skeleton_open_search)) {
                onNavigate(NavigationRequest.NavigateTo(SearchScreenProvider.Search))
            },
            SkeletonAction(stringResource(R.string.skeleton_open_show, SAMPLE_SHOW_ID)) {
                onNavigate(NavigationRequest.NavigateTo(watchRoutes.showDetail(SAMPLE_SHOW_ID)))
            },
        ),
    )
}
