package com.takehome.drama.feature.main.presentation.licenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.takehome.drama.core.designsystem.component.AppTopBar
import com.takehome.drama.core.designsystem.component.SettingsRow
import com.takehome.drama.core.designsystem.component.SettingsRows
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.feature.main.R

/** Thư viện mã nguồn mở app kèm theo — tên + license là tên riêng, không dịch. */
private data class OpenSourceLibrary(val name: String, val license: String)

private const val APACHE_2 = "Apache License 2.0"

private val libraries = listOf(
    OpenSourceLibrary("AndroidX (Compose, Navigation, Lifecycle, Room, DataStore, Media3)", APACHE_2),
    OpenSourceLibrary("Kotlin, Kotlin Coroutines, kotlinx.serialization", APACHE_2),
    OpenSourceLibrary("Ktor", APACHE_2),
    OpenSourceLibrary("Koin", APACHE_2),
    OpenSourceLibrary("Coil", APACHE_2),
    OpenSourceLibrary("OkHttp", APACHE_2),
    OpenSourceLibrary("Timber", APACHE_2),
    OpenSourceLibrary("Firebase Android SDK", APACHE_2),
    OpenSourceLibrary("Google Mobile Ads SDK", "Google APIs Terms of Service"),
)

/** Open-source licenses (từ SCR-0501). */
@Composable
fun LicensesScreen(viewModel: LicensesViewModel, onNavigate: (NavigationRequest) -> Unit) {
    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            LicensesSingleEvent.NavigateBack -> onNavigate(NavigationRequest.PopBackStack)
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AppTopBar(
            title = stringResource(R.string.settings_licenses),
            onBack = { viewModel.processIntent(LicensesIntent.Back) },
            backContentDescription = stringResource(R.string.common_back),
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = AppTheme.dimensions.s6),
        ) {
            SettingsRows(topPadding = AppTheme.dimensions.s2) {
                libraries.forEachIndexed { index, library ->
                    SettingsRow(
                        label = library.name,
                        supporting = library.license,
                        onClick = null,
                        divider = index < libraries.lastIndex,
                    )
                }
            }
        }
    }
}
