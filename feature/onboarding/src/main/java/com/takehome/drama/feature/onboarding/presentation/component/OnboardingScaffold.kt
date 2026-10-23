package com.takehome.drama.feature.onboarding.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.drama.core.designsystem.component.GradientButton
import com.takehome.drama.core.designsystem.ext.clickableNoRipple
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.feature.onboarding.R

const val ONBOARDING_STEPS = 4

private val ONBOARDING_TOP_PADDING = 56.dp

/**
 * Khung `.ob` chung của 4 bước onboarding: "STEP n / 4", h1 28sp, mô tả 14sp, phần riêng của màn,
 * footer (chấm tiến độ + CTA gradient full width + Skip/Not now). [background] vẽ key art phía sau
 * (concept "Golden Hour Cinema"). [titleAtBottom] đẩy tiêu đề xuống sát footer như card cuối trailer
 * (SCR-0003).
 */
@Composable
fun OnboardingScaffold(
    step: Int,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
    titleAtBottom: Boolean = false,
    background: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val dimensions = AppTheme.dimensions
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        background()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = dimensions.s6),
        ) {
            // Phần trên footer cuộn (máy ngang / chữ hệ thống to): footer luôn giữ đáy, CTA không bị ép.
            // heightIn(min = khung) để vẫn đẩy tiêu đề xuống đáy khi đủ chỗ ([titleAtBottom]).
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(top = ONBOARDING_TOP_PADDING),
                    verticalArrangement = if (titleAtBottom) Arrangement.SpaceBetween else Arrangement.Top,
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_step, step, ONBOARDING_STEPS),
                        style = AppTypography.eyebrow,
                        color = AppTheme.colors.text3,
                    )
                    Column {
                        Text(
                            text = title,
                            style = AppTypography.headline,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = dimensions.s3),
                        )
                        Text(
                            text = body,
                            style = AppTypography.body.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        content()
                    }
                }
            }
            Footer(
                step = step,
                primaryLabel = primaryLabel,
                onPrimary = onPrimary,
                primaryEnabled = primaryEnabled,
                secondaryLabel = secondaryLabel,
                onSecondary = onSecondary,
                modifier = Modifier.padding(top = if (titleAtBottom) 22.dp else dimensions.s4),
            )
        }
    }
}

@Composable
private fun Footer(
    step: Int,
    primaryLabel: String,
    onPrimary: () -> Unit,
    primaryEnabled: Boolean,
    secondaryLabel: String?,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        StepDots(step = step, modifier = Modifier.padding(bottom = 18.dp))
        GradientButton(
            text = primaryLabel,
            onClick = onPrimary,
            enabled = primaryEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.ctaHeight),
        )
        if (secondaryLabel != null) {
            Text(
                text = secondaryLabel,
                style = AppTypography.body.copy(fontSize = 13.5.sp),
                color = AppTheme.colors.text3,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = AppTheme.dimensions.s1)
                    .clickableNoRipple(role = Role.Button, onClick = onSecondary)
                    .padding(AppTheme.dimensions.s3),
            )
        }
    }
}

/** `.dots`: 4 chấm 6dp, chấm của bước hiện tại dài 20dp màu accent. */
@Composable
private fun StepDots(step: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(ONBOARDING_STEPS) { index ->
            val current = index == step - 1
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (current) 20.dp else 6.dp)
                    .background(
                        if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        AppTheme.shapes.full,
                    ),
            )
        }
    }
}
