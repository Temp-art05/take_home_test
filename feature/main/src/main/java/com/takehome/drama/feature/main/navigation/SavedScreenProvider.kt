package com.takehome.drama.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.feature.main.presentation.saved.SavedScreen
import com.takehome.drama.feature.main.presentation.saved.SavedViewModel
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
        SavedScreen(viewModel = viewModel, watchRoutes = watchRoutes, onNavigate = onNavigationRequest)
    }
}
