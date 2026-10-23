package com.base.app.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.base.app.core.navigation.core.NavigationRequest
import com.base.app.core.navigation.core.ScreenProvider
import com.base.app.core.navigation.destination.Destination
import com.base.app.feature.main.presentation.main.MainScreen
import com.base.app.feature.main.presentation.main.MainViewModel
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

class MainScreenProvider : ScreenProvider<MainScreenProvider.Main, MainViewModel>() {

    @Serializable
    data object Main : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: MainViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        MainScreen(
            viewModel = viewModel,
            onNavigate = onNavigationRequest
        )
    }
}
