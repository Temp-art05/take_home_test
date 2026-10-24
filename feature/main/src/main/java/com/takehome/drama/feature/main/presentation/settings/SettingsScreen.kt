package com.takehome.drama.feature.main.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.AppToastHost
import com.takehome.drama.core.designsystem.component.AppTopBar
import com.takehome.drama.core.designsystem.component.RowTrailing
import com.takehome.drama.core.designsystem.component.SettingsGroupLabel
import com.takehome.drama.core.designsystem.component.SettingsRow
import com.takehome.drama.core.designsystem.component.SettingsRows
import com.takehome.drama.core.designsystem.component.rememberToastState
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.core.ui.foundation.locale.LanguageLabels
import com.takehome.drama.domain.model.ThemeMode
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.navigation.LanguageScreenProvider
import com.takehome.drama.feature.main.navigation.LicensesScreenProvider
import com.takehome.drama.feature.main.presentation.language.rememberDeviceLocale
import org.koin.compose.viewmodel.koinViewModel

/**
 * SCR-0501 — ba nhóm: Language, Appearance (Dark/Light là radio, không phải toggle), Policies. Dòng version
 * cuối: người chấm copy đúng `versionName` dán vào `disabled_versions` trên Remote Config.
 */
@Composable
internal fun SettingsScreen(
    onNavigate: (NavigationRequest) -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val toastState = rememberToastState()
    val resources = LocalResources.current

    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            SettingsSingleEvent.OpenLanguage -> onNavigate(NavigationRequest.NavigateTo(LanguageScreenProvider.Language))
            SettingsSingleEvent.OpenLicenses -> onNavigate(NavigationRequest.NavigateTo(LicensesScreenProvider.Licenses))
            is SettingsSingleEvent.ShowToast -> toastState.show(resources.getString(event.messageRes), event.success)
        }
    }

    // Ngôn ngữ đổi = Activity dựng lại = đọc lại ở đây.
    val languageLabel = LanguageLabels.appLanguage(AppLocale.current(), rememberDeviceLocale())

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SettingsContent(viewState = viewState, languageLabel = languageLabel, onIntent = viewModel::processIntent)
        // Tab bar nằm dưới nội dung tab → đáy Box chính là mép trên tab bar.
        AppToastHost(
            state = toastState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AppTheme.dimensions.s4),
        )
    }
}

@Composable
private fun SettingsContent(
    viewState: SettingsViewState,
    languageLabel: String,
    onIntent: (SettingsIntent) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.tab_settings), onBack = null)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsGroupLabel(stringResource(R.string.settings_group_language), topPadding = 6.dp)
            SettingsRows {
                SettingsRow(
                    label = stringResource(R.string.settings_app_language),
                    trailing = RowTrailing.Chevron(languageLabel),
                    onClick = { onIntent(SettingsIntent.OpenLanguage) },
                    divider = false,
                )
            }
            SettingsGroupLabel(stringResource(R.string.settings_group_appearance))
            SettingsRows {
                ThemeMode.entries.forEachIndexed { index, theme ->
                    SettingsRow(
                        label = stringResource(if (theme == ThemeMode.Dark) R.string.settings_theme_dark else R.string.settings_theme_light),
                        trailing = if (viewState.theme == theme) RowTrailing.Selected else RowTrailing.None,
                        selectable = true,
                        onClick = { onIntent(SettingsIntent.SelectTheme(theme)) },
                        divider = index < ThemeMode.entries.lastIndex,
                    )
                }
            }
            SettingsGroupLabel(stringResource(R.string.settings_group_policies))
            SettingsRows {
                SettingsRow(stringResource(R.string.settings_terms), trailing = RowTrailing.Chevron(), onClick = { onIntent(SettingsIntent.OpenTerms) })
                SettingsRow(stringResource(R.string.settings_privacy), trailing = RowTrailing.Chevron(), onClick = { onIntent(SettingsIntent.OpenPrivacy) })
                SettingsRow(
                    stringResource(R.string.settings_licenses),
                    trailing = RowTrailing.Chevron(),
                    onClick = { onIntent(SettingsIntent.OpenLicenses) },
                    divider = false,
                )
            }
            VersionFooter(versionName = viewState.versionName, versionCode = viewState.versionCode)
        }
    }
}

/** `FlowDrama v1.0.0 (1)` giữa đáy — chọn/copy được. */
@Composable
internal fun VersionFooter(versionName: String, versionCode: Int, modifier: Modifier = Modifier) {
    SelectionContainer(modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_version, versionName, versionCode),
            style = AppTypography.versionFooter,
            color = AppTheme.colors.text4,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 30.dp),
        )
    }
}

@Preview(heightDp = 760)
@Composable
private fun SettingsContentPreview() {
    AppTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            SettingsContent(SettingsViewState(versionName = "1.0.0", versionCode = 1), "English (U.S.)", onIntent = {})
        }
    }
}
