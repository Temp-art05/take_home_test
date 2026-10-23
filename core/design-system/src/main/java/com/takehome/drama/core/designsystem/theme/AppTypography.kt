package com.takehome.drama.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Type scale theo mockup. Font hệ thống (mockup dùng system sans) chứ không nhúng font riêng:
 * app hiện cả tên phim tiếng Ả Rập, Hindi, Tamil — font hệ thống có sẵn glyph cho mọi script đó.
 */
object AppTypography {

    private val family = FontFamily.Default

    /** Tiêu đề hero, tên phim ở màn chi tiết — 30sp. */
    val display = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.02).em,
    )

    /** Wordmark FLOW DRAMA — 20sp, giãn chữ rộng. */
    val wordmark = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        letterSpacing = 0.16.em,
    )

    /** Tiêu đề màn / sheet — 19sp. */
    val title = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.015).em,
    )

    /** Tiêu đề section / rail — 17sp. */
    val titleSmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.01).em,
    )

    /** Nhãn nút — 15sp. */
    val button = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
    )

    val bodyLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )

    val body = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    val bodySemibold = body.copy(fontWeight = FontWeight.SemiBold)

    /** Chip, metadata — 13sp. */
    val label = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )

    val labelSemibold = label.copy(fontWeight = FontWeight.SemiBold)

    val caption = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    val captionSemibold = caption.copy(fontWeight = FontWeight.SemiBold)

    /** Badge, eyebrow viết hoa — 11sp. */
    val micro = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        letterSpacing = 0.08.em,
    )

    /** Nhãn tab bar — 10.5sp. */
    val tab = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
    )
}
