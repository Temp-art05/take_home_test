package com.takehome.drama.feature.main.presentation.main.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.takehome.drama.core.designsystem.icon.AppIcons
import com.takehome.drama.feature.main.R

/**
 * 5 tab theo design (Home, Reels, Browse, Library, Settings). Icon stroke vẽ lại từ SVG `.tb` của
 * mockup — tab chọn đổi màu stroke sang accent và nhãn tô gradient, không đổi hình.
 */
enum class MainTab(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Home(R.string.tab_home, AppIcons.TabHome),
    Reels(R.string.tab_reels, AppIcons.TabReels),
    Browse(R.string.tab_browse, AppIcons.TabBrowse),
    Library(R.string.tab_library, AppIcons.TabLibrary),
    Settings(R.string.tab_settings, AppIcons.TabSettings),
}
