package com.base.app.core.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {

    // ======================
    // Palette - Base
    // ======================
    object Base {
        val base0 = Color(0xFFFDFBFF)
        val base0Op16 = Color(0x29FDFBFF)
        val base0Op32 = Color(0x52FDFBFF)
        val base0Op64 = Color(0xA3FDFBFF)
        val base950 = Color(0xFF020202)
        val base950Op30 = Color(0x66020202)
        val base950Op80 = Color(0xCC020202)
        val base950Op90 = Color(0xE6020202)
    }

    // ======================
    // Palette - Neutral
    // ======================
    object Neutral {
        val n50 = Color(0xFFFAFAFA)
        val n100 = Color(0xFFF4F4F5)
        val n200 = Color(0xFFE9E9EC)
        val n300 = Color(0xFFD6D6DA)
        val n400 = Color(0xFFBEBEC4)
        val n500 = Color(0xFFA3A3AA)
        val n600 = Color(0xFF8A8A92)
        val n700 = Color(0xFF71717A)
        val n800 = Color(0xFF5A5A62)
        val n900 = Color(0xFF3F3F46)
        val n950 = Color(0xFF27272A)
    }

    // ======================
    // Palette - Primary
    // ======================
    // PLACEHOLDER — đổi cả thang này theo brand của project.
    // Giữ nguyên tên token (p50..p950) thì không phải sửa chỗ nào khác.
    object Primary {
        val p50 = Color(0xFFF2F6FC)
        val p100 = Color(0xFFE2EBF8)
        val p200 = Color(0xFFC7D9F2)
        val p300 = Color(0xFF9FBEE8)
        val p400 = Color(0xFF6F9CDA)
        val p500 = Color(0xFF4A7CC7)
        val p600 = Color(0xFF3A63A8)
        val p700 = Color(0xFF305089)
        val p800 = Color(0xFF2A4470)
        val p900 = Color(0xFF263A5E)
        val p950 = Color(0xFF19253D)
    }

    // ======================
    // Tokens - Border
    // ======================
    object Border {
        val baseOp16 = Base.base0Op16
        val baseOp32 = Base.base0Op32
        val primary = Primary.p500
    }

    // ======================
    // Tokens - Icon
    // ======================
    object Icon {
        val base = Base.base0
        val baseOp64 = Base.base0Op64
        val dark = Base.base950
        val grey = Neutral.n700
    }

    // ======================
    // Tokens - Surface
    // ======================
    object Surface {
        val base = Base.base0
        val baseOp16 = Base.base0Op16
        val baseOp32 = Base.base0Op32
        val dark = Base.base950
        val darkOp50 = Base.base950Op30
        val darkOp80 = Base.base950Op80
        val darkOp90 = Base.base950Op90
        val primary = Primary.p500
    }

    // ======================
    // Tokens - Text
    // ======================
    object Text {
        val base = Base.base0
        val baseOp64 = Base.base0Op64
        val grey = Neutral.n700
        val inverse = Base.base950
        val primary = Primary.p500
    }

    // ======================
    // Tokens - Gradient
    // ======================
    object Gradient {
        // Suy ra từ thang Primary -> đổi brand là gradient đổi theo, không phải sửa ở đây.
        val button = Brush.linearGradient(
            colors = listOf(Primary.p700, Primary.p500, Primary.p300),
            start = Offset(0f, Float.POSITIVE_INFINITY),
            end = Offset(Float.POSITIVE_INFINITY, 0f)
        )
    }

    // ======================
    // Tokens - Shadow
    // ======================
    object Shadow {
        val dropShadow = Primary.p500.copy(alpha = 0.5f)
        val innerShadow = Base.base0.copy(alpha = 0.8f)
    }
}
