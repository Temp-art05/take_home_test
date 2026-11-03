package com.takehome.drama.feature.main.presentation.main

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.feature.main.presentation.history.HistoryScreenProvider
import com.takehome.drama.feature.main.presentation.language.LanguageScreenProvider
import com.takehome.drama.feature.main.presentation.licenses.LicensesScreenProvider
import com.takehome.drama.feature.main.presentation.saved.SavedScreenProvider
import com.takehome.drama.feature.main.presentation.search.SearchScreenProvider
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

class MainScreenProvider(
    private val watchRoutes: WatchRoutes,
) : ScreenProvider<MainScreenProvider.Main, MainViewModel>() {

    /**
     * [initialTab] là tên một `MainTab` (null = Home). Dùng khi một lối vào cần mở thẳng một tab —
     * hiện là launcher debug mở màn theo mã SCR để chụp so mockup.
     */
    @Serializable
    data class Main(val initialTab: String? = null) : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: MainViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        MainScreen(
            viewModel = viewModel,
            onOpenShowDetail = { showId ->
                onNavigationRequest(NavigationRequest.NavigateTo(watchRoutes.showDetail(showId)))
            },
            onOpenPlayer = { showId, episodeIndex ->
                onNavigationRequest(NavigationRequest.NavigateTo(watchRoutes.player(showId, episodeIndex)))
            },
            onOpenSearch = { onNavigationRequest(NavigationRequest.NavigateTo(SearchScreenProvider.Search())) },
            onOpenHistory = { onNavigationRequest(NavigationRequest.NavigateTo(HistoryScreenProvider.History)) },
            onOpenSaved = { onNavigationRequest(NavigationRequest.NavigateTo(SavedScreenProvider.Saved)) },
            onOpenLanguage = { onNavigationRequest(NavigationRequest.NavigateTo(LanguageScreenProvider.Language)) },
            onOpenLicenses = { onNavigationRequest(NavigationRequest.NavigateTo(LicensesScreenProvider.Licenses)) },
        )
    }
}
