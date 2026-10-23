package com.takehome.drama.feature.watch.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.feature.watch.presentation.player.PlayerScreen
import com.takehome.drama.feature.watch.presentation.player.PlayerViewModel
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

/** SCR-0302 + sheet 0303 / 0304 + trạng thái 0604. */
class PlayerScreenProvider : ScreenProvider<PlayerScreenProvider.Player, PlayerViewModel>() {

    /**
     * [episodeIndex] là `Episode.index`, không phải `number`. Chỉ là tập mở đầu — đổi tập trong
     * player không navigate, tập hiện tại nằm ở SavedStateHandle của ViewModel.
     */
    @Serializable
    data class Player(val showId: String, val episodeIndex: Int) : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: PlayerViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        PlayerScreen(viewModel = viewModel, onNavigate = onNavigationRequest)
    }
}
