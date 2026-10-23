package com.takehome.drama.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography

private const val DISABLED_ALPHA = 0.4f

/** `.btn-p` của mockup: nền gradient CTA, chữ ink ấm. Chỉ cho hành động chính của màn. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    PillButton(
        text = text,
        onClick = onClick,
        modifier = modifier.background(AppTheme.colors.gradientCta, AppTheme.shapes.full),
        enabled = enabled,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        leadingIcon = leadingIcon,
    )
}

/** `.btn-out` của mockup: viền mảnh, nền trong suốt — hành động phụ. */
@Composable
fun OutlinePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    PillButton(
        text = text,
        onClick = onClick,
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, AppTheme.shapes.full),
        enabled = enabled,
        contentColor = MaterialTheme.colorScheme.onBackground,
        leadingIcon = leadingIcon,
    )
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    contentColor: Color,
    leadingIcon: (@Composable () -> Unit)?,
) {
    val dimensions = AppTheme.dimensions
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = dimensions.touchTarget)
            .clip(AppTheme.shapes.full)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.s2),
        ) {
            leadingIcon?.invoke()
            Text(
                text = text,
                style = AppTypography.button,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PillButtonsPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GradientButton(text = "Step into the flow", onClick = {})
            OutlinePillButton(text = "Not now", onClick = {})
            GradientButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
