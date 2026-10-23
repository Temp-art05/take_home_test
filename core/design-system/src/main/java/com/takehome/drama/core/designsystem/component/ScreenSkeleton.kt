package com.takehome.drama.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography

@Immutable
data class SkeletonAction(
    val label: String,
    val onClick: () -> Unit,
)

/**
 * Khung tạm cho màn chưa dựng UI thật: mã SCR + tên màn + các nút đi tiếp, đủ để đi hết luồng.
 * Mỗi phase thay dần bằng UI thật; hết chỗ gọi thì xoá file này.
 */
@Composable
fun ScreenSkeleton(
    code: String,
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: List<SkeletonAction> = emptyList(),
) {
    val dimensions = AppTheme.dimensions
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AppTopBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(dimensions.screenGutter),
            verticalArrangement = Arrangement.spacedBy(dimensions.s3),
        ) {
            Text(
                text = code,
                style = AppTypography.micro,
                color = AppTheme.colors.text3,
            )
            actions.forEachIndexed { index, action ->
                val buttonModifier = Modifier.fillMaxWidth()
                if (index == 0) {
                    GradientButton(text = action.label, onClick = action.onClick, modifier = buttonModifier)
                } else {
                    OutlinePillButton(text = action.label, onClick = action.onClick, modifier = buttonModifier)
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ScreenSkeletonPreview() {
    AppTheme {
        ScreenSkeleton(
            code = "SCR-0301",
            title = "Show detail",
            onBack = {},
            actions = listOf(SkeletonAction("Play Ep. 1") {}, SkeletonAction("Episodes") {}),
        )
    }
}
