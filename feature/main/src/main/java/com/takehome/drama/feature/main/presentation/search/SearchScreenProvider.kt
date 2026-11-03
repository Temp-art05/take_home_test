package com.takehome.drama.feature.main.presentation.search

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.core.navigation.destination.WatchRoutes
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

/** SCR-0201 — mở từ icon tìm ở app bar Home và header Browse. */
class SearchScreenProvider(
    private val watchRoutes: WatchRoutes,
) : ScreenProvider<SearchScreenProvider.Search, SearchViewModel>() {

    /** [initialQuery] chỉ launcher debug dùng (chụp trạng thái đang gõ). */
    @Serializable
    data class Search(val initialQuery: String? = null) : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: SearchViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        SearchScreen(
            viewModel = viewModel,
            onBack = { onNavigationRequest(NavigationRequest.PopBackStack) },
            onOpenShowDetail = { showId ->
                onNavigationRequest(NavigationRequest.NavigateTo(watchRoutes.showDetail(showId)))
            },
        )
    }
}
