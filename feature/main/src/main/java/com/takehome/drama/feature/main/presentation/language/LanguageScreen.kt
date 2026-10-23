package com.takehome.drama.feature.main.presentation.language

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.AppTopBar
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.feature.main.R

@Composable
fun LanguageScreen(
    viewModel: LanguageViewModel,
    onNavigate: (NavigationRequest) -> Unit
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            LanguageSingleEvent.NavigateBack -> onNavigate(NavigationRequest.NavigateUp())
        }
    }

    LanguageContent(
        viewState = viewState,
        onIntent = { intent -> viewModel.processIntent(intent) },
    )
}

/** SCR-0502 — mới đủ chức năng; giao diện theo design (Match device + danh sách endonym) làm ở Phase 6. */
@Composable
private fun LanguageContent(
    viewState: LanguageViewState,
    onIntent: (LanguageIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppTopBar(
            title = stringResource(R.string.language_title),
            onBack = { onIntent(LanguageIntent.BackClick) },
        )
        Column(modifier = Modifier.padding(horizontal = AppTheme.dimensions.s2)) {
            AppLocale.entries.forEach { locale ->
                LanguageRow(
                    locale = locale,
                    selected = viewState.locale == locale,
                    onClick = { onIntent(LanguageIntent.SelectLocale(locale)) }
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
            .defaultMinSize(minHeight = AppTheme.dimensions.touchTarget)
            .clip(AppTheme.shapes.md)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = AppTheme.dimensions.s1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = locale.endonym ?: stringResource(R.string.language_system),
            style = AppTypography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = AppTheme.dimensions.s3),
        )
    }
}

@PreviewLightDark
@Composable
private fun LanguageContentPreview() {
    AppTheme {
        LanguageContent(viewState = LanguageViewState(), onIntent = {})
    }
}
