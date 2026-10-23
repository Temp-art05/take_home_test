package com.base.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class AppComponentTokens(
    val button: ButtonTokens,
    val card: CardTokens,
    val textField: TextFieldTokens,
    val chip: ChipTokens,
    val list: ListTokens
)

@Immutable
data class ButtonTokens(
    val minWidth: Dp,
    val height: Dp,
    val smallHeight: Dp,
    val largeHeight: Dp,
    val horizontalPadding: Dp,
    val iconSpacing: Dp,
    val textStyle: TextStyle
)

@Immutable
data class CardTokens(
    val elevation: Dp,
    val padding: Dp,
    val cornerRadius: Dp
)

@Immutable
data class TextFieldTokens(
    val height: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val cornerRadius: Dp,
    val borderWidth: Dp,
    val focusedBorderWidth: Dp
)

@Immutable
data class ChipTokens(
    val height: Dp,
    val horizontalPadding: Dp,
    val iconSize: Dp,
    val iconSpacing: Dp
)

@Immutable
data class ListTokens(
    val itemMinHeight: Dp,
    val itemPadding: Dp,
    val iconSize: Dp,
    val dividerIndent: Dp
)

val defaultComponentTokens = AppComponentTokens(
    button = ButtonTokens(
        minWidth = 64.dp,
        height = 48.dp,
        smallHeight = 36.dp,
        largeHeight = 56.dp,
        horizontalPadding = 24.dp,
        iconSpacing = 8.dp,
        textStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            letterSpacing = 0.1.sp
        )
    ),
    card = CardTokens(
        elevation = 2.dp,
        padding = 16.dp,
        cornerRadius = 16.dp
    ),
    textField = TextFieldTokens(
        height = 56.dp,
        horizontalPadding = 16.dp,
        verticalPadding = 16.dp,
        cornerRadius = 12.dp,
        borderWidth = 1.dp,
        focusedBorderWidth = 2.dp
    ),
    chip = ChipTokens(
        height = 32.dp,
        horizontalPadding = 12.dp,
        iconSize = 18.dp,
        iconSpacing = 8.dp
    ),
    list = ListTokens(
        itemMinHeight = 56.dp,
        itemPadding = 16.dp,
        iconSize = 24.dp,
        dividerIndent = 16.dp
    )
)

val LocalAppComponentTokens = staticCompositionLocalOf { defaultComponentTokens }
