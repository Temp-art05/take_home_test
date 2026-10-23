package com.takehome.drama.feature.onboarding.presentation.language

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.KeyArt
import com.takehome.drama.core.designsystem.icon.AppIcons
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.domain.usecase.onboarding.LanguageOption
import com.takehome.drama.feature.onboarding.R
import com.takehome.drama.feature.onboarding.presentation.component.OnboardingScaffold

@Composable
fun OnboardingLanguageScreen(
    viewModel: OnboardingLanguageViewModel,
    onNext: () -> Unit,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            is OnboardingLanguageSingleEvent.Next -> {
                // Activity khai configChanges=locale|layoutDirection: chuỗi đổi tại chỗ, không recreate giữa lúc trượt.
                AppLocale.apply(event.locale)
                onNext()
            }
        }
    }
    OnboardingLanguageContent(viewState = viewState, onIntent = viewModel::processIntent)
}

@Composable
private fun OnboardingLanguageContent(
    viewState: OnboardingLanguageViewState,
    onIntent: (OnboardingLanguageIntent) -> Unit,
) {
    OnboardingScaffold(
        step = 1,
        title = stringResource(R.string.onboarding_language_title),
        body = stringResource(R.string.onboarding_language_body),
        primaryLabel = stringResource(R.string.onboarding_continue),
        onPrimary = { onIntent(OnboardingLanguageIntent.Continue) },
        primaryEnabled = !viewState.isLoading,
        background = {
            // Mockup `.art a4` (gradient ấm) + scrim, mờ dần vào nền ở 58% chiều cao; không dùng poster thật
            // vì chưa chọn ngôn ngữ nên chưa có nội dung nào để "đúng".
            KeyArt(posterUrl = null, seed = viewState.artSeed, modifier = Modifier.fillMaxSize(), paletteIndex = ART_A4)
            Box(Modifier.fillMaxSize().background(AppTheme.colors.scrimOnboardingList))
        },
    ) {
        // Column thường (≤ 5 ngôn ngữ): khung onboarding đã cuộn cả phần trên footer.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.dimensions.s4),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            viewState.options.forEach { option ->
                LanguageRow(
                    option = option,
                    selected = option.code == viewState.selected,
                    onClick = { onIntent(OnboardingLanguageIntent.Select(option.code)) },
                )
            }
        }
    }
}

/** `.g` / `.g.on`: hàng pill full width 48dp; đang chọn nền accent + icon check. */
@Composable
private fun LanguageRow(option: LanguageOption, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = AppTheme.shapes.full
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.dimensions.touchTarget)
            .clip(shape)
            .then(if (selected) Modifier.background(colors.primary, shape) else Modifier.border(1.dp, colors.outline, shape))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = AppTheme.dimensions.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = option.label,
            style = AppTypography.body.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(AppIcons.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

@Preview
@Composable
private fun OnboardingLanguagePreview() {
    AppTheme {
        OnboardingLanguageContent(
            viewState = OnboardingLanguageViewState(
                isLoading = false,
                options = listOf(LanguageOption("en", "English"), LanguageOption("hi", "हिन्दी"), LanguageOption("ar", "العربية")),
                selected = "en",
            ),
            onIntent = {},
        )
    }
}

private const val ART_A4 = 3
