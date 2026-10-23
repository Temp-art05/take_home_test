package com.base.app.feature.main.presentation.main.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.AppTypography
import com.base.app.feature.main.presentation.main.model.MainTab

@Composable
fun MainBottomNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                BottomNavItem(
                    tab = tab,
                    isSelected = selectedTab == tab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Icon nằm TRÊN pill primaryContainer nên phải tint bằng onPrimaryContainer,
    // không phải primary — dùng primary thì dark mode ra xanh trên xanh, đọc không nổi.
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "navIconColor"
    )
    // Label nằm trên nền surface nên dùng primary được.
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "navLabelColor"
    )
    // Pill sau icon: 0 khi không chọn -> co giãn mượt khi đổi tab
    val indicatorWidth by animateDpAsState(
        targetValue = if (isSelected) 56.dp else 0.dp,
        animationSpec = spring(),
        label = "navIndicatorWidth"
    )

    val label = stringResource(tab.labelRes)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(width = indicatorWidth, height = 32.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = iconColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = AppTypography.microMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor
        )
    }
}

@PreviewLightDark
@Composable
private fun MainBottomNavBarPreview() {
    AppTheme {
        MainBottomNavBar(
            selectedTab = MainTab.Home,
            onTabSelected = {}
        )
    }
}
