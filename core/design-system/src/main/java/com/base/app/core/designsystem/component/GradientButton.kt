package com.base.app.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.AppColors
import com.base.app.core.designsystem.theme.AppTypography
import com.base.app.core.designsystem.theme.DefaultAppShape

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    height: Dp = 56.dp,
    enabled: Boolean = true,
) {
    val buttonShape = DefaultAppShape.button

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = 8.dp,
                shape = buttonShape,
                ambientColor = AppColors.Shadow.dropShadow,
                spotColor = AppColors.Shadow.dropShadow
            )
            .clip(buttonShape)
            .background(brush = AppColors.Gradient.button, shape = buttonShape)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = AppColors.Icon.base
                )
            }
            Text(
                text = text,
                style = AppTypography.bodyLargeSemibold,
                color = AppColors.Text.base
            )
        }
    }
}

@Preview
@Composable
private fun GradientButtonPreview() {
    Box(
        modifier = Modifier
            .background(AppColors.Surface.dark)
            .padding(16.dp)
    ) {
        GradientButton(
            text = "Start Creating",
            onClick = {}
        )
    }
}
