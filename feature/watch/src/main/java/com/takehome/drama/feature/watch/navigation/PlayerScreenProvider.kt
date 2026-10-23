package com.takehome.drama.feature.watch.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.toRoute
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.core.SkeletonViewModel
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.feature.watch.presentation.player.PlayerSkeleton
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

/** SCR-0302 (+ sheet 0303, 0304) — khung, UI thật ở Phase 3. */
class PlayerScreenProvider : ScreenProvider<PlayerScreenProvider.Player, SkeletonViewModel>() {

    /** [episodeIndex] là vị trí trong mảng `episodes`, không phải `number`. */
    @Serializable
    data class Player(val showId: String, val episodeIndex: Int) : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: SkeletonViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        val player = viewModel.savedStateHandle.toRoute<Player>()
        PlayerSkeleton(
            showId = player.showId,
            episodeIndex = player.episodeIndex,
            onBack = { onNavigationRequest(NavigationRequest.PopBackStack) },
            onNextEpisode = {
                onNavigationRequest(
                    NavigationRequest.NavigateTo(player.copy(episodeIndex = player.episodeIndex + 1), popCurrent = true)
                )
            },
        )
    }
}
