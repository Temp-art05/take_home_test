package com.takehome.drama.feature.main.presentation.main.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.feature.main.presentation.main.model.MainTab

/**
 * Tab bar `.tb` của mockup: viền trên mảnh, tab active icon màu accent + nhãn tô gradient,
 * tab khác màu text-3. Gradient chỉ thuộc về tab active (quy tắc của design).
 */
@Composable
fun MainBottomNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(horizontal = 6.dp)
                .padding(top = 9.dp, bottom = 4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                BottomNavItem(
                    tab = tab,
                    isSelected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f),
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
    val inactiveColor = AppTheme.colors.text3
    val labelStyle = if (isSelected) {
        AppTypography.tab.copy(brush = AppTheme.colors.gradientCta)
    } else {
        AppTypography.tab.copy(color = inactiveColor)
    }

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = AppTheme.dimensions.touchTarget)
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
            // Nhãn bên dưới đã đọc tên tab cho TalkBack.
            contentDescription = null,
            modifier = Modifier.size(AppTheme.dimensions.tabIcon),
            tint = if (isSelected) MaterialTheme.colorScheme.primary else inactiveColor,
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.s1))
        Text(
            text = stringResource(tab.labelRes),
            style = labelStyle,
            maxLines = 1,
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
