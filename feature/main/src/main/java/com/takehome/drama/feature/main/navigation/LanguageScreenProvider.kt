package com.takehome.drama.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.navigation.core.ScreenProvider
import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.feature.main.presentation.language.LanguageScreen
import com.takehome.drama.feature.main.presentation.language.LanguageViewModel
import kotlin.reflect.KType
import kotlinx.serialization.Serializable

/** SCR-0502 — chọn ngôn ngữ giao diện, mở từ tab Settings. */
class LanguageScreenProvider : ScreenProvider<LanguageScreenProvider.Language, LanguageViewModel>() {

    @Serializable
    data object Language : Destination

    override val typeMap: Map<KType, NavType<*>> = emptyMap()

    override fun invoke(
        viewModel: LanguageViewModel,
        onNavigationRequest: (NavigationRequest) -> Unit
    ): @Composable () -> Unit = {
        LanguageScreen(
            viewModel = viewModel,
            onNavigate = onNavigationRequest
        )
    }
}
