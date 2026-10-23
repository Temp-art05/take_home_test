package com.base.app.feature.main.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.AppTypography
import com.base.app.core.navigation.core.NavigationRequest
import com.base.app.core.ui.foundation.ext.CollectAsEffect
import com.base.app.feature.main.R
import com.base.app.feature.main.navigation.SettingsScreenProvider
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    onNavigate: (NavigationRequest) -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            ProfileSingleEvent.NavigateToSettings ->
                onNavigate(NavigationRequest.NavigateTo(SettingsScreenProvider.Settings))
        }
    }

    ProfileContent(
        viewState = viewState,
        onSettingsClick = { viewModel.onSettingsClick() },
    )
}

@Composable
private fun ProfileContent(
    viewState: ProfileViewState,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.tab_profile),
            style = AppTypography.headingH4Semibold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = viewState.userName ?: stringResource(R.string.main_profile_guest),
            style = AppTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onSettingsClick) {
            Text(text = stringResource(R.string.main_profile_open_settings))
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileContentPreview() {
    AppTheme {
        ProfileContent(viewState = ProfileViewState(), onSettingsClick = {})
    }
}
