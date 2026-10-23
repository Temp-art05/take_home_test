package com.takehome.drama.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.takehome.drama.core.designsystem.ext.applyIf
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography

/** `.chip` / `.chip.on` của mockup: pill viền mảnh, chọn thì nền `surface-3`, không viền. */
@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = AppTheme.shapes.full
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 34.dp)
            .clip(shape)
            .applyIf(selected) { background(colors.surfaceContainerHighest, shape) }
            .applyIf(!selected) { border(1.dp, colors.outline, shape) }
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = if (selected) AppTypography.labelSemibold else AppTypography.label,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
