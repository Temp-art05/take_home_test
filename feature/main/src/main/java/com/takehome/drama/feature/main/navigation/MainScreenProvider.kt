package com.takehome.drama.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.feature.main.presentation.main.MainScreen
import com.takehome.drama.feature.main.presentation.main.MainViewModel
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

class MainScreenProvider(
    private val watchRoutes: WatchRoutes,
) : ScreenProvider<MainScreenProvider.Main, MainViewModel>() {

    @Serializable
    data object Main : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: MainViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        MainScreen(
            viewModel = viewModel,
            watchRoutes = watchRoutes,
            onNavigate = onNavigationRequest
        )
    }
}
