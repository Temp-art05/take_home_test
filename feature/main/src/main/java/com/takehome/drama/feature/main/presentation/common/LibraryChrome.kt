package com.takehome.drama.feature.main.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.takehome.drama.core.designsystem.component.AppBottomSheet
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography

/** "Edit" / "Done" ở góc header History/Saved — chữ 13.5sp text-3, vùng chạm 48dp. */
@Composable
internal fun HeaderTextAction(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minWidth = AppTheme.dimensions.touchTarget, minHeight = AppTheme.dimensions.touchTarget)
            .clip(AppTheme.shapes.full)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppTheme.dimensions.s3),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = AppTypography.textAction, color = AppTheme.colors.text3)
    }
}

/** Một hành động trong sheet ⋯. */
internal data class SheetAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)

/**
 * Sheet ⋯ của History/Saved (AppBottomSheet — không popup Material). Giữ tiêu đề cũ trong lúc
 * animation đóng để chữ không biến mất giữa chừng. Nội dung cũ giữ trong holder thường (không phải
 * snapshot state): ghi state trong chính composition đọc nó là backwards write → recompose thừa.
 */
@Composable
internal fun RowActionsSheet(
    title: String?,
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
) {
    val retained = remember { RetainedSheetContent() }
    if (title != null) {
        retained.title = title
        retained.actions = actions
    }
    val shownTitle = retained.title
    val shownActions = retained.actions
    AppBottomSheet(visible = title != null, onDismiss = onDismiss) {
        Text(
            text = shownTitle.orEmpty(),
            style = AppTypography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            modifier = Modifier.padding(bottom = AppTheme.dimensions.s2),
        )
        Column {
            shownActions.forEach { action ->
                SheetActionRow(action)
            }
        }
    }
}

private class RetainedSheetContent {
    var title: String? = null
    var actions: List<SheetAction> = emptyList()
}

@Composable
private fun SheetActionRow(action: SheetAction) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(AppTheme.shapes.md)
            .clickable(role = Role.Button, onClick = action.onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(action.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Text(
            text = action.label,
            style = AppTypography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = AppTheme.dimensions.s4),
        )
    }
}
