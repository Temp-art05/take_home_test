package com.takehome.drama.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Palette "Sunset Flow" — giá trị lấy nguyên từ `:root` CSS của mockup (design system E).
 * Màn hình không đọc thẳng object này: màu Material đi qua `MaterialTheme.colorScheme`,
 * phần còn lại qua [AppTheme.colors].
 */
object AppColors {

    object Dark {
        val bg = Color(0xFF160F0A)
        val surface = Color(0xFF241812)
        val surface2 = Color(0xFF2F2018)
        val surface3 = Color(0xFF3B2A1E)
        val text1 = Color(0xFFFFFFFF)
        val text2 = Color(0xFFC9BCAE)
        val text3 = Color(0xFF8F8172)
        val text4 = Color(0xFF5E5347)
        val border = Color(0xFF4A3325)
    }

    // Design chưa có bộ light ("Light needs a light token set, still to be designed") —
    // đây là bộ tạm suy từ palette tối, giữ đúng hue ấm, đủ tương phản cho chữ.
    object Light {
        val bg = Color(0xFFFFF8F2)
        val surface = Color(0xFFFFFFFF)
        val surface2 = Color(0xFFF6ECE3)
        val surface3 = Color(0xFFEDDDCF)
        val text1 = Color(0xFF1A0E05)
        val text2 = Color(0xFF5C4838)
        val text3 = Color(0xFF85725F)
        val text4 = Color(0xFFB3A291)
        val border = Color(0xFFE3CFBE)
    }

    object Brand {
        val accent = Color(0xFFFF7A3C)
        val accentPress = Color(0xFFE5602A)
        val onAccent = Color(0xFF1A0E05)
        val gradientStart = Color(0xFFFFB020)
        val gradientMid = Color(0xFFFF6A3D)
        val gradientEnd = Color(0xFFF5405F)

        /** Chỉ dùng cho khoá tập — mọi ô vàng đơn sắc trong app được hiểu là khoá. */
        val gold = Color(0xFFE0B44C)
    }

    object Status {
        val success = Color(0xFF2ECC71)
        val warning = Color(0xFFF5A623)
        val error = Color(0xFFFF453A)
    }

    object Scrim {
        val bottom = Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0f),
            0.55f to Color.Black.copy(alpha = 0.45f),
            1f to Color.Black.copy(alpha = 0.88f),
        )
        val top = Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.60f),
            1f to Color.Black.copy(alpha = 0f),
        )
    }

    /**
     * Chữ/nút đè lên video (`.pl`, `.btn-g`, `.eppill`, `.ptrack` của mockup). Luôn trắng/đen trên
     * media nên không đổi theo theme.
     */
    object Media {
        val glass = Color.White.copy(alpha = 0.12f)
        val glassStrong = Color.White.copy(alpha = 0.14f)
        val glassBorder = Color.White.copy(alpha = 0.20f)
        val track = Color.White.copy(alpha = 0.25f)
        /** Progress khi đang buffer (SCR-0604) — mờ để không gợi ý đang phát. */
        val trackDimmed = Color.White.copy(alpha = 0.45f)
        val spinnerTrack = Color.White.copy(alpha = 0.22f)
        val textBody = Color.White.copy(alpha = 0.82f)
        val textSecondary = Color.White.copy(alpha = 0.70f)
        val textMuted = Color.White.copy(alpha = 0.60f)
        val textFaint = Color.White.copy(alpha = 0.55f)
        /** Nền mờ sau sheet (`.dim`). */
        val sheetScrim = Color.Black.copy(alpha = 0.72f)
        /** Art tối 50% lúc buffering (`filter: brightness(.5)`). */
        val bufferingDim = Color.Black.copy(alpha = 0.50f)
        val textShadow = Color.Black.copy(alpha = 0.60f)
    }

    /** CTA chính, progress, tab active, chữ DRAMA của wordmark — ngoài mấy chỗ đó không dùng. */
    val gradientCta = Brush.horizontalGradient(
        0f to Brand.gradientStart,
        0.55f to Brand.gradientMid,
        1f to Brand.gradientEnd,
    )
}

/** Token không có chỗ trong Material `ColorScheme`. Đọc qua [AppTheme.colors]. */
@Immutable
data class AppExtendedColors(
    val text3: Color,
    val text4: Color,
    val lock: Color,
    val success: Color,
    val warning: Color,
    val gradientCta: Brush,
    val scrimTop: Brush,
    val scrimBottom: Brush,
    val media: MediaColors = MediaColors(),
)

/** Màu cho lớp UI đè lên video — xem [AppColors.Media]. */
@Immutable
data class MediaColors(
    val glass: Color = AppColors.Media.glass,
    val glassStrong: Color = AppColors.Media.glassStrong,
    val glassBorder: Color = AppColors.Media.glassBorder,
    val track: Color = AppColors.Media.track,
    val trackDimmed: Color = AppColors.Media.trackDimmed,
    val spinnerTrack: Color = AppColors.Media.spinnerTrack,
    val text: Color = Color.White,
    val textBody: Color = AppColors.Media.textBody,
    val textSecondary: Color = AppColors.Media.textSecondary,
    val textMuted: Color = AppColors.Media.textMuted,
    val textFaint: Color = AppColors.Media.textFaint,
    val sheetScrim: Color = AppColors.Media.sheetScrim,
    val bufferingDim: Color = AppColors.Media.bufferingDim,
    val textShadow: Color = AppColors.Media.textShadow,
    val background: Color = Color.Black,
)

internal val DarkExtendedColors = AppExtendedColors(
    text3 = AppColors.Dark.text3,
    text4 = AppColors.Dark.text4,
    lock = AppColors.Brand.gold,
    success = AppColors.Status.success,
    warning = AppColors.Status.warning,
    gradientCta = AppColors.gradientCta,
    scrimTop = AppColors.Scrim.top,
    scrimBottom = AppColors.Scrim.bottom,
)

internal val LightExtendedColors = DarkExtendedColors.copy(
    text3 = AppColors.Light.text3,
    text4 = AppColors.Light.text4,
)

val LocalAppExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
