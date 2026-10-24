package com.takehome.drama.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.feature.main.presentation.licenses.LicensesScreen
import com.takehome.drama.feature.main.presentation.licenses.LicensesViewModel
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

/** Open-source licenses — mở từ nhóm Policies của SCR-0501. */
class LicensesScreenProvider : ScreenProvider<LicensesScreenProvider.Licenses, LicensesViewModel>() {

    @Serializable
    data object Licenses : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: LicensesViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        LicensesScreen(viewModel = viewModel, onNavigate = onNavigationRequest)
    }
}
