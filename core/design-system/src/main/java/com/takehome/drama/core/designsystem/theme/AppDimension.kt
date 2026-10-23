package com.takehome.drama.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing `--s1`…`--s6` của mockup (bước 4dp) và vài kích thước cố định của design. */
@Immutable
data class AppDimension(
    val s1: Dp = 4.dp,
    val s2: Dp = 8.dp,
    val s3: Dp = 12.dp,
    val s4: Dp = 16.dp,
    val s5: Dp = 20.dp,
    val s6: Dp = 24.dp,

    /** Lề ngang chuẩn của màn. */
    val screenGutter: Dp = 16.dp,

    /** Vùng chạm tối thiểu — design chốt 48dp cho Android. */
    val touchTarget: Dp = 48.dp,

    val buttonHeight: Dp = 46.dp,
    val iconButton: Dp = 40.dp,
    val icon: Dp = 22.dp,
    val tabIcon: Dp = 23.dp,
)

val LocalAppDimension = staticCompositionLocalOf { AppDimension() }
