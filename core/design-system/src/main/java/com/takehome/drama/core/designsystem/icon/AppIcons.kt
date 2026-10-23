package com.takehome.drama.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Icon vẽ lại từ SVG của mockup (viewBox 24) — `material-icons-core` không có pause, heart,
 * episodes, volume; ký tự chữ (✓) thì hiển thị lệch theo font nên cũng vẽ ở đây.
 * Màu thật lấy từ `tint` của `Icon`, ở đây chỉ cần màu đặc bất kỳ.
 */
object AppIcons {

    /** Tim viền (`.rail` chưa like). */
    val Heart: ImageVector by lazy { stroked("Heart", HEART) }

    /** Tim đặc (`.rail .lk`). */
    val HeartFilled: ImageVector by lazy { filled("HeartFilled", HEART) }

    /** Ô lưới tập (`Eps` trên rail). */
    val Episodes: ImageVector by lazy {
        stroked(
            "Episodes",
            "M6 4h12a2.5 2.5 0 0 1 2.5 2.5v11a2.5 2.5 0 0 1-2.5 2.5H6a2.5 2.5 0 0 1-2.5-2.5v-11A2.5 2.5 0 0 1 6 4z",
            "M3.5 9.5h17M9 9.5V20",
        )
    }

    /** Loa (dòng Autoplay ở SCR-0304). */
    val Volume: ImageVector by lazy { stroked("Volume", "M5 9v6h4l5 4V5L9 9H5z", "M17 9.5a4 4 0 0 1 0 5") }

    /** Dấu chọn (dòng quality/ngôn ngữ đang chọn ở SCR-0304). */
    val Check: ImageVector by lazy { stroked("Check", "M5 12.5l4.5 4.5L19 7.5") }

    val Play: ImageVector by lazy { filled("Play", "M8 5.5v13l10.5-6.5z") }

    val Pause: ImageVector by lazy { filled("Pause", "M7 5h3.5v14H7zM13.5 5H17v14h-3.5z") }

    private const val HEART = "M12 20s-7-4.6-7-9.6A3.9 3.9 0 0 1 12 7.6a3.9 3.9 0 0 1 7 2.8C19 15.4 12 20 12 20z"

    private fun builder(name: String) = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = false,
    )

    private fun stroked(name: String, vararg paths: String): ImageVector = builder(name).apply {
        paths.forEach { path ->
            addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

    private fun filled(name: String, path: String): ImageVector = builder(name).apply {
        addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
    }.build()
}
