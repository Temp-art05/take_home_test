package com.takehome.drama.feature.main.presentation.library

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.takehome.drama.core.designsystem.component.ScreenSkeleton
import com.takehome.drama.core.designsystem.component.SkeletonAction
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.navigation.HistoryScreenProvider
import com.takehome.drama.feature.main.navigation.SavedScreenProvider
import com.takehome.drama.feature.main.presentation.SAMPLE_SHOW_ID

/** SCR-0401 — khung tab, UI thật ở Phase 6. */
@Composable
internal fun LibraryScreen(
    watchRoutes: WatchRoutes,
    onNavigate: (NavigationRequest) -> Unit,
) {
    ScreenSkeleton(
        code = "SCR-0401",
        title = stringResource(R.string.tab_library),
        onBack = null,
        actions = listOf(
            SkeletonAction(stringResource(R.string.skeleton_open_history)) {
                onNavigate(NavigationRequest.NavigateTo(HistoryScreenProvider.History))
            },
            SkeletonAction(stringResource(R.string.skeleton_open_saved)) {
                onNavigate(NavigationRequest.NavigateTo(SavedScreenProvider.Saved))
            },
            SkeletonAction(stringResource(R.string.skeleton_play_show, SAMPLE_SHOW_ID)) {
                onNavigate(NavigationRequest.NavigateTo(watchRoutes.player(SAMPLE_SHOW_ID, episodeIndex = 0)))
            },
        ),
    )
}
