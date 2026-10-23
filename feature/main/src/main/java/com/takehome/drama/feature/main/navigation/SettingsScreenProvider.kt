package com.takehome.drama.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.feature.main.presentation.settings.SettingsScreen
import com.takehome.drama.feature.main.presentation.settings.SettingsViewModel
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

class SettingsScreenProvider : ScreenProvider<SettingsScreenProvider.Settings, SettingsViewModel>() {

    @Serializable
    data object Settings : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: SettingsViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        SettingsScreen(
            viewModel = viewModel,
            onNavigate = onNavigationRequest
        )
    }
}
