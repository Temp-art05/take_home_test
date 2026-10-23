package com.takehome.drama.feature.watch.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.feature.watch.presentation.detail.ShowDetailScreen
import com.takehome.drama.feature.watch.presentation.detail.ShowDetailViewModel
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

/** SCR-0301 — chi tiết show. */
class ShowDetailScreenProvider : ScreenProvider<ShowDetailScreenProvider.ShowDetail, ShowDetailViewModel>() {

    @Serializable
    data class ShowDetail(val showId: String) : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: ShowDetailViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        ShowDetailScreen(viewModel = viewModel, onNavigate = onNavigationRequest)
    }
}
