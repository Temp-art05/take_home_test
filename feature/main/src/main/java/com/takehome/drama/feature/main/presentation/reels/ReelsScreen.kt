package com.takehome.drama.feature.main.presentation.reels

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.takehome.drama.core.designsystem.component.ScreenSkeleton
import com.takehome.drama.core.designsystem.component.SkeletonAction
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.presentation.SAMPLE_SHOW_ID

/** SCR-0203 — khung tab, UI thật ở Phase 4. */
@Composable
internal fun ReelsScreen(
    watchRoutes: WatchRoutes,
    onNavigate: (NavigationRequest) -> Unit,
) {
    ScreenSkeleton(
        code = "SCR-0203",
        title = stringResource(R.string.tab_reels),
        onBack = null,
        actions = listOf(
            SkeletonAction(stringResource(R.string.skeleton_open_show, SAMPLE_SHOW_ID)) {
                onNavigate(NavigationRequest.NavigateTo(watchRoutes.showDetail(SAMPLE_SHOW_ID)))
            },
        ),
    )
}
