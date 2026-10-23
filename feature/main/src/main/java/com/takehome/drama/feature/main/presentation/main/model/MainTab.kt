package com.takehome.drama.feature.main.presentation.main.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.takehome.drama.feature.main.R

/**
 * 5 tab theo design (Home, Reels, Browse, Library, Settings). Mỗi tab có 2 icon: [selectedIcon]
 * (filled) và [unselectedIcon] (outlined) — đổi cả hình dáng chứ không chỉ màu, nhìn ra tab đang
 * chọn kể cả khi khó phân biệt màu. Icon Material tạm; Phase 4 thay bằng icon vẽ theo mockup.
 */
enum class MainTab(
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    Reels(R.string.tab_reels, Icons.Filled.PlayArrow, Icons.Outlined.PlayArrow),
    Browse(R.string.tab_browse, Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List),
    Library(R.string.tab_library, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    Settings(R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
}
