package com.github.dinhn.ads.core

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.MainThread

/**
 * Nền **opaque phủ full-screen** gắn vào `decorView` của activity — che nội dung app phía sau trong
 * lúc show full-screen ad (đặc biệt **app-open resume**: khoảng chuyển từ background về foreground,
 * activity chưa vẽ lại kịp / ad activity của AdMob chưa đè lên → dễ "lộ" nội dung cũ hoặc launcher,
 * rõ nhất ở build release do timing nhanh hơn).
 *
 * Khác [AdsLoadingOverlay] (nền đen + spinner cho loading): đây chỉ là 1 tấm màu đơn (mặc định trắng),
 * KHÔNG spinner — chỉ để "bịt" nền. Network-agnostic (Rule 1). [show]/[hide] idempotent theo tag.
 */
object AdScreenGuard {

    private const val TAG = "ads_screen_guard"

    /** Phủ nền màu [color] (ARGB, mặc định trắng đục) lên trên cùng [activity]. Gọi trước khi show ad. */
    @MainThread
    fun show(activity: Activity, @ColorInt color: Int = Color.WHITE) {
        val root = activity.window?.decorView as? ViewGroup ?: return
        root.findViewWithTag<View>(TAG)?.let { return }   // đã có → không add trùng
        val guard = FrameLayout(activity).apply {
            tag = TAG
            setBackgroundColor(color)
            isClickable = true    // chặn tap xuyên xuống UI bên dưới
            isFocusable = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        root.addView(guard)
        AdsLogger.d("Screen guard shown")
    }

    /** Gỡ nền nếu đang hiện. An toàn khi gọi lúc chưa có. */
    @MainThread
    fun hide(activity: Activity) {
        val root = activity.window?.decorView as? ViewGroup ?: return
        root.findViewWithTag<View>(TAG)?.let {
            root.removeView(it)
            AdsLogger.d("Screen guard hidden")
        }
    }
}
