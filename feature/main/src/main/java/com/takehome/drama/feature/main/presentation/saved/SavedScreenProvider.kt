package com.takehome.drama.feature.main.presentation.saved

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

/** SCR-0403 — mở từ tab Library. */
class SavedScreenProvider(
    private val watchRoutes: WatchRoutes,
) : ScreenProvider<SavedScreenProvider.Saved, SavedViewModel>() {

    @Serializable
    data object Saved : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: SavedViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        SavedScreen(
            viewModel = viewModel,
            onBack = { onNavigationRequest(NavigationRequest.PopBackStack) },
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
