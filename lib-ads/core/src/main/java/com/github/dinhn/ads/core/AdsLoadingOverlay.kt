package com.github.dinhn.ads.core

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.MainThread

/**
 * Màn loading toàn màn **chuẩn của lib**: nền đen + vòng xoay loading mặc định của Android
 * ([ProgressBar] indeterminate, ~48dp, màu trắng). Dùng để che màn trong lúc chờ load ad
 * full-screen (vd interstitial) **trước khi show**.
 *
 * Network-agnostic — không phụ thuộc network cụ thể (Rule 1). Overlay gắn trực tiếp vào
 * `decorView` của activity nên phủ lên mọi thứ; [show]/[hide] đều idempotent (gọi nhiều
 * lần không tạo trùng / không lỗi).
 */
object AdsLoadingOverlay {

    /** Tag để tìm lại overlay đã gắn trên decorView, tránh add trùng. */
    private const val OVERLAY_TAG = "ads_loading_overlay"

    /** Kích thước vòng xoay loading (theo yêu cầu placement: 48dp). */
    private const val SPINNER_DP = 48

    /** Hiện overlay lên trên cùng [activity]. Gọi khi đã có sẵn loading thì không tạo thêm. */
    @MainThread
    fun show(activity: Activity) {
        val root = activity.window?.decorView as? ViewGroup ?: return
        if (root.findViewWithTag<View>(OVERLAY_TAG) != null) return  // đã hiện rồi

        val density = activity.resources.displayMetrics.density
        val spinnerSize = (SPINNER_DP * density).toInt()

        val overlay = FrameLayout(activity).apply {
            tag = OVERLAY_TAG
            setBackgroundColor(Color.BLACK)
            isClickable = true   // chặn tap xuyên qua xuống UI bên dưới
            isFocusable = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        val spinner = ProgressBar(activity).apply {
            isIndeterminate = true
            indeterminateTintList = ColorStateList.valueOf(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(spinnerSize, spinnerSize, Gravity.CENTER)
        }
        overlay.addView(spinner)
        root.addView(overlay)
        AdsLogger.d("Loading overlay shown")
    }

    /** Ẩn & gỡ overlay nếu đang hiện. An toàn khi gọi lúc chưa có overlay. */
    @MainThread
    fun hide(activity: Activity) {
        val root = activity.window?.decorView as? ViewGroup ?: return
        root.findViewWithTag<View>(OVERLAY_TAG)?.let {
            root.removeView(it)
            AdsLogger.d("Loading overlay hidden")
        }
    }
}
