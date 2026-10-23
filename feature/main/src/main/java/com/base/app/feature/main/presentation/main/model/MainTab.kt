package com.base.app.feature.main.presentation.main.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.base.app.feature.main.R

/**
 * Mỗi tab có 2 icon: [selectedIcon] (filled) và [unselectedIcon] (outlined).
 * Đổi cả hình dáng icon chứ không chỉ đổi màu — nhìn ra tab đang chọn kể cả khi
 * người dùng khó phân biệt màu.
 */
enum class MainTab(
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    Feature(R.string.tab_feature, Icons.Filled.Star, Icons.Outlined.Star),
    Profile(R.string.tab_profile, Icons.Filled.Person, Icons.Outlined.Person),
}
