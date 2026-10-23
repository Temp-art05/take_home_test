package com.takehome.drama.feature.watch.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.toRoute
import com.takehome.drama.core.designsystem.component.ScreenSkeleton
import com.takehome.drama.core.designsystem.component.SkeletonAction
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.core.SkeletonViewModel
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.feature.watch.R
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

/** SCR-0301 — khung, UI thật ở Phase 3. */
class ShowDetailScreenProvider : ScreenProvider<ShowDetailScreenProvider.ShowDetail, SkeletonViewModel>() {

    @Serializable
    data class ShowDetail(val showId: String) : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: SkeletonViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        val showId = viewModel.savedStateHandle.toRoute<ShowDetail>().showId
        ScreenSkeleton(
            code = "SCR-0301",
            title = stringResource(R.string.watch_show_detail_title, showId),
            onBack = { onNavigationRequest(NavigationRequest.PopBackStack) },
            actions = listOf(
                SkeletonAction(stringResource(R.string.watch_play_episode, 1)) {
                    onNavigationRequest(
                        NavigationRequest.NavigateTo(PlayerScreenProvider.Player(showId = showId, episodeIndex = 0))
                    )
                },
            ),
        )
    }
}
