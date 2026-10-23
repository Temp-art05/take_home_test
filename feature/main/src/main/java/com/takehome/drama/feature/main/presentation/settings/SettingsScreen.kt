package com.takehome.drama.feature.main.presentation.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.feature.main.R

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (NavigationRequest) -> Unit
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            SettingsSingleEvent.NavigateBack -> onNavigate(NavigationRequest.NavigateUp())
        }
    }

    SettingsContent(
        viewState = viewState,
        onBackClick = { viewModel.onBackClick() },
        onLocaleSelected = { locale -> viewModel.selectLocale(locale) },
    )
}

@Composable
private fun SettingsContent(
    viewState: SettingsViewState,
    onBackClick: () -> Unit,
    onLocaleSelected: (AppLocale) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(R.string.title_settings),
                style = AppTypography.subtitleBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_language),
                style = AppTypography.bodyBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            AppLocale.entries.forEach { locale ->
                LanguageRow(
                    locale = locale,
                    selected = viewState.locale == locale,
                    onClick = { onLocaleSelected(locale) }
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(
    locale: AppLocale,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = stringResource(locale.labelRes()),
            style = AppTypography.bodyRegular,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@StringRes
private fun AppLocale.labelRes(): Int = when (this) {
    AppLocale.System -> R.string.language_system
    AppLocale.English -> R.string.language_en
    AppLocale.Vietnamese -> R.string.language_vi
}
