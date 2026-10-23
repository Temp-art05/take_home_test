package com.takehome.drama.feature.main.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.takehome.drama.core.designsystem.component.ScreenSkeleton
import com.takehome.drama.core.designsystem.component.SkeletonAction
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.navigation.LanguageScreenProvider

/** SCR-0501 — khung tab, UI thật ở Phase 6. */
@Composable
internal fun SettingsScreen(
    onNavigate: (NavigationRequest) -> Unit,
) {
    ScreenSkeleton(
        code = "SCR-0501",
        title = stringResource(R.string.tab_settings),
        onBack = null,
        actions = listOf(
            SkeletonAction(stringResource(R.string.skeleton_open_language)) {
                onNavigate(NavigationRequest.NavigateTo(LanguageScreenProvider.Language))
            },
        ),
    )
}
