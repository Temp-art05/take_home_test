package com.takehome.drama.feature.main.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.takehome.drama.core.designsystem.component.ScreenSkeleton
import com.takehome.drama.core.designsystem.component.SkeletonAction
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.domain.config.AppConfig
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.navigation.LanguageScreenProvider
import org.koin.compose.koinInject

/**
 * SCR-0501 — khung tab, UI thật ở Phase 6. Dòng version đã thật (Phase 5): người chấm copy đúng
 * `versionName` dán vào `disabled_versions` trên Remote Config.
 */
@Composable
internal fun SettingsScreen(
    onNavigate: (NavigationRequest) -> Unit,
    appConfig: AppConfig = koinInject(),
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenSkeleton(
            code = "SCR-0501",
            title = stringResource(R.string.tab_settings),
            onBack = null,
            modifier = Modifier.weight(1f),
            actions = listOf(
                SkeletonAction(stringResource(R.string.skeleton_open_language)) {
                    onNavigate(NavigationRequest.NavigateTo(LanguageScreenProvider.Language))
                },
            ),
        )
        VersionFooter(versionName = appConfig.versionName, versionCode = appConfig.versionCode)
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
