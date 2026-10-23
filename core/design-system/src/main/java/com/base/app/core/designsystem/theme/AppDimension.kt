package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * App dimension definitions.
 * Contains all spacing and sizing values used throughout the application.
 */
@Immutable
data class AppDimension(
    // Spacing
    val spaceNone: Dp,
    val spaceXxs: Dp,
    val spaceXs: Dp,
    val spaceSm: Dp,
    val spaceMd: Dp,
    val spaceLg: Dp,
    val spaceXl: Dp,
    val spaceXxl: Dp,
    val spaceXxxl: Dp,

    // Padding
    val paddingScreen: Dp,
    val paddingCard: Dp,
    val paddingButton: Dp,
    val paddingTextField: Dp,
    val paddingChip: Dp,
    val paddingListItem: Dp,

    // Icon sizes
    val iconXs: Dp,
    val iconSm: Dp,
    val iconMd: Dp,
    val iconLg: Dp,
    val iconXl: Dp,

    // Component sizes
    val buttonHeight: Dp,
    val buttonHeightSmall: Dp,
    val buttonHeightLarge: Dp,
    val textFieldHeight: Dp,
    val appBarHeight: Dp,
    val bottomNavHeight: Dp,
    val fabSize: Dp,
    val fabSizeSmall: Dp,
    val chipHeight: Dp,
    val avatarSizeSm: Dp,
    val avatarSizeMd: Dp,
    val avatarSizeLg: Dp,

    // Divider
    val dividerThickness: Dp,

    // Border
    val borderWidth: Dp,
    val borderWidthThick: Dp,

    // Elevation
    val elevationNone: Dp,
    val elevationSm: Dp,
    val elevationMd: Dp,
    val elevationLg: Dp,
    val elevationXl: Dp
)

/**
 * Default app dimensions
 */
val DefaultAppDimension = AppDimension(
    // Spacing
    spaceNone = 0.dp,
    spaceXxs = 2.dp,
    spaceXs = 4.dp,
    spaceSm = 8.dp,
    spaceMd = 16.dp,
    spaceLg = 24.dp,
    spaceXl = 32.dp,
    spaceXxl = 48.dp,
    spaceXxxl = 64.dp,

    // Padding
    paddingScreen = 16.dp,
    paddingCard = 16.dp,
    paddingButton = 16.dp,
    paddingTextField = 16.dp,
    paddingChip = 8.dp,
    paddingListItem = 16.dp,

    // Icon sizes
    iconXs = 16.dp,
    iconSm = 20.dp,
    iconMd = 24.dp,
    iconLg = 32.dp,
    iconXl = 48.dp,

    // Component sizes
    buttonHeight = 48.dp,
    buttonHeightSmall = 36.dp,
    buttonHeightLarge = 56.dp,
    textFieldHeight = 56.dp,
    appBarHeight = 64.dp,
    bottomNavHeight = 80.dp,
    fabSize = 56.dp,
    fabSizeSmall = 40.dp,
    chipHeight = 32.dp,
    avatarSizeSm = 32.dp,
    avatarSizeMd = 48.dp,
    avatarSizeLg = 64.dp,

    // Divider
    dividerThickness = 1.dp,

    // Border
    borderWidth = 1.dp,
    borderWidthThick = 2.dp,

    // Elevation
    elevationNone = 0.dp,
    elevationSm = 2.dp,
    elevationMd = 4.dp,
    elevationLg = 8.dp,
    elevationXl = 16.dp
)

/**
 * CompositionLocal for AppDimension
 */
val LocalAppDimension = staticCompositionLocalOf { DefaultAppDimension }
