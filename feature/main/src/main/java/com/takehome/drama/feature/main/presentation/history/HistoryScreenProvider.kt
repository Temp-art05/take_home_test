package com.takehome.drama.feature.main.presentation.history

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.feature.main.presentation.main.MainScreenProvider
import com.takehome.drama.feature.main.presentation.main.model.MainTab
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

/** SCR-0402 — mở từ tab Library. */
class HistoryScreenProvider(
    private val watchRoutes: WatchRoutes,
) : ScreenProvider<HistoryScreenProvider.History, HistoryViewModel>() {

    @Serializable
    data object History : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: HistoryViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        HistoryScreen(
            viewModel = viewModel,
            onBack = { onNavigationRequest(NavigationRequest.PopBackStack) },
            onOpenPlayer = { showId, episodeIndex ->
                onNavigationRequest(NavigationRequest.NavigateTo(watchRoutes.player(showId, episodeIndex)))
            },
            onOpenShowDetail = { showId ->
                onNavigationRequest(NavigationRequest.NavigateTo(watchRoutes.showDetail(showId)))
            },
            // Launcher debug mở thẳng màn này (không có Main bên dưới) → dựng Main ở tab Browse.
            onOpenBrowseTab = {
                onNavigationRequest(
                    NavigationRequest.NavigateUp(fallBackDestination = MainScreenProvider.Main(MainTab.Browse.name))
                )
            },
        )
    }
}
