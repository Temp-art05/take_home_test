package com.base.app.core.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Primary.p500,
    onPrimary = AppColors.Base.base0,
    primaryContainer = AppColors.Primary.p700,
    onPrimaryContainer = AppColors.Primary.p100,
    secondary = AppColors.Neutral.n600,
    onSecondary = AppColors.Base.base0,
    background = AppColors.Base.base950,
    onBackground = AppColors.Base.base0,
    surface = AppColors.Base.base950,
    onSurface = AppColors.Base.base0,
    surfaceVariant = AppColors.Neutral.n950,
    onSurfaceVariant = AppColors.Neutral.n300,
    outline = AppColors.Neutral.n700,
    outlineVariant = AppColors.Neutral.n950,
)

private val LightColorScheme = lightColorScheme(
    primary = AppColors.Primary.p600,
    onPrimary = AppColors.Base.base0,
    primaryContainer = AppColors.Primary.p100,
    onPrimaryContainer = AppColors.Primary.p900,
    secondary = AppColors.Neutral.n700,
    onSecondary = AppColors.Base.base0,
    background = AppColors.Base.base0,
    onBackground = AppColors.Base.base950,
    surface = AppColors.Base.base0,
    onSurface = AppColors.Base.base950,
    surfaceVariant = AppColors.Neutral.n100,
    onSurfaceVariant = AppColors.Neutral.n800,
    outline = AppColors.Neutral.n400,
    outlineVariant = AppColors.Neutral.n200,
)

/**
 * Chủ ý KHÔNG bật dynamic color (Material You): base này dùng cho app có brand riêng,
 * màu hệ thống sẽ ghi đè brand. Project nào cần thì thêm dynamicDarkColorScheme ở đây.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            // Không set window.statusBarColor: deprecated từ API 35 và no-op khi edge-to-edge.
            // Chỉ cần báo cho hệ thống biết nên vẽ icon status bar sáng hay tối.
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalAppShape provides DefaultAppShape,
        LocalAppDimension provides DefaultAppDimension,
        LocalAppComponentTokens provides defaultComponentTokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

object AppTheme {
    val shapes: AppShape
        @Composable
        @ReadOnlyComposable
        get() = LocalAppShape.current

    val dimensions: AppDimension
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDimension.current

    val tokens: AppComponentTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalAppComponentTokens.current
}
