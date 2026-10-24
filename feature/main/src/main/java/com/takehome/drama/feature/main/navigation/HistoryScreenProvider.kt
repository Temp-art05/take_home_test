package com.takehome.drama.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.feature.main.presentation.history.HistoryScreen
import com.takehome.drama.feature.main.presentation.history.HistoryViewModel
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
        HistoryScreen(viewModel = viewModel, watchRoutes = watchRoutes, onNavigate = onNavigationRequest)
    }
}
