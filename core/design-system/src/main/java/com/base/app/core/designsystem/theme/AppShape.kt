package com.base.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * App shape definitions.
 * Contains all shape values used throughout the application.
 */
@Immutable
data class AppShape(
    val none: Shape,
    val extraSmall: Shape,
    val small: Shape,
    val medium: Shape,
    val large: Shape,
    val extraLarge: Shape,
    val full: Shape,

    // Component-specific shapes
    val button: Shape,
    val card: Shape,
    val dialog: Shape,
    val bottomSheet: Shape,
    val textField: Shape,
    val chip: Shape,
    val fab: Shape,
    val snackbar: Shape
)

/**
 * Default app shapes
 */
val DefaultAppShape = AppShape(
    none = RoundedCornerShape(0.dp),
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
    full = RoundedCornerShape(50),

    // Component-specific
    button = RoundedCornerShape(16.dp),
    card = RoundedCornerShape(16.dp),
    dialog = RoundedCornerShape(24.dp),
    bottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    textField = RoundedCornerShape(12.dp),
    chip = RoundedCornerShape(8.dp),
    fab = RoundedCornerShape(16.dp),
    snackbar = RoundedCornerShape(8.dp)
)

/**
 * CompositionLocal for AppShape
 */
val LocalAppShape = staticCompositionLocalOf { DefaultAppShape }
