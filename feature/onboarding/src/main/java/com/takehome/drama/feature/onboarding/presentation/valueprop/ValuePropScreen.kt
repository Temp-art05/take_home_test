package com.takehome.drama.feature.onboarding.presentation.valueprop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.KeyArt
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.feature.onboarding.R
import com.takehome.drama.feature.onboarding.presentation.component.OnboardingScaffold

@Composable
fun ValuePropScreen(
    viewModel: ValuePropViewModel,
    onNext: () -> Unit,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            ValuePropSingleEvent.Next -> onNext()
        }
    }
    ValuePropContent(viewState = viewState, onIntent = viewModel::processIntent)
}

/** Cả màn là một cảnh full-bleed, headline nằm dưới như card title cuối trailer. */
@Composable
private fun ValuePropContent(viewState: ValuePropViewState, onIntent: (ValuePropIntent) -> Unit) {
    OnboardingScaffold(
        step = 2,
        title = stringResource(R.string.onboarding_value_prop_title),
        body = stringResource(R.string.onboarding_value_prop_body),
        primaryLabel = stringResource(R.string.onboarding_step_into_flow),
        onPrimary = { onIntent(ValuePropIntent.Continue) },
        titleAtBottom = true,
        background = {
            // Mockup: nền tối phẳng, không ảnh — palette ấm thay poster thật (fallback xanh lệch tông).
            KeyArt(posterUrl = null, seed = viewState.artSeed, modifier = Modifier.fillMaxSize(), paletteIndex = 0)
            Box(Modifier.fillMaxSize().background(AppTheme.colors.scrimValueProp))
        },
    ) {
        // FlowRow: bản dịch dài hơn (fr/de/ta…) thì pill xuống dòng thay vì bị cắt.
        FlowRow(
            modifier = Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s2),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s2),
        ) {
            listOf(R.string.onboarding_pill_full_seasons, R.string.onboarding_pill_no_signup, R.string.onboarding_pill_short)
                .forEach { GlassPill(stringResource(it)) }
        }
    }
}

@Composable
private fun GlassPill(text: String) {
    val media = AppTheme.colors.media
    val shape = AppTheme.shapes.full
    Text(
        text = text,
        style = AppTypography.caption.copy(fontWeight = FontWeight.Bold),
        color = media.text,
        maxLines = 1,
        modifier = Modifier
            .background(media.glassSoft, shape)
            .border(1.dp, media.glassSoftBorder, shape)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

@Preview
@Composable
private fun ValuePropPreview() {
    AppTheme { ValuePropContent(viewState = ValuePropViewState(artSeed = "12485"), onIntent = {}) }
}
