package com.base.app.core.utils.toast

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.CheckResult
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.base.app.core.utils.R

/**
 * Toast có icon + màu theo loại, hiện ở mép trên màn hình.
 *
 * ```
 * Toasty.success(context, R.string.saved).show()
 * Toasty.error(context, event.messageRes, allowQueue = false).show()
 * ```
 *
 * Dùng `Toast.setView` (deprecated từ API 30): từ Android 11 toast custom **chỉ hiện khi app ở
 * foreground** — đúng với mọi chỗ gọi từ UI. Cần báo khi app ở nền thì dùng notification.
 * `allowQueue = false` huỷ toast đang hiện để toast mới hiện ngay (tránh dồn hàng khi bấm liên tục).
 */
@SuppressLint("InflateParams")
object Toasty {

    private const val TOP_OFFSET_DP = 72f

    private var lastToast: Toast? = null

    @CheckResult
    fun normal(context: Context, message: CharSequence, duration: Int = Toast.LENGTH_SHORT, allowQueue: Boolean = true): Toast =
        create(context, message, Type.Normal, duration, allowQueue)

    @CheckResult
    fun normal(context: Context, @StringRes message: Int, duration: Int = Toast.LENGTH_SHORT, allowQueue: Boolean = true): Toast =
        normal(context, context.getString(message), duration, allowQueue)

    @CheckResult
    fun success(context: Context, message: CharSequence, duration: Int = Toast.LENGTH_SHORT, allowQueue: Boolean = true): Toast =
        create(context, message, Type.Success, duration, allowQueue)

    @CheckResult
    fun success(context: Context, @StringRes message: Int, duration: Int = Toast.LENGTH_SHORT, allowQueue: Boolean = true): Toast =
        success(context, context.getString(message), duration, allowQueue)

    @CheckResult
    fun error(context: Context, message: CharSequence, duration: Int = Toast.LENGTH_SHORT, allowQueue: Boolean = true): Toast =
        create(context, message, Type.Error, duration, allowQueue)

    @CheckResult
    fun error(context: Context, @StringRes message: Int, duration: Int = Toast.LENGTH_SHORT, allowQueue: Boolean = true): Toast =
        error(context, context.getString(message), duration, allowQueue)

    @CheckResult
    fun info(context: Context, message: CharSequence, duration: Int = Toast.LENGTH_SHORT, allowQueue: Boolean = true): Toast =
        create(context, message, Type.Info, duration, allowQueue)

    @CheckResult
    fun info(context: Context, @StringRes message: Int, duration: Int = Toast.LENGTH_SHORT, allowQueue: Boolean = true): Toast =
        info(context, context.getString(message), duration, allowQueue)

    @CheckResult
    fun warning(context: Context, message: CharSequence, duration: Int = Toast.LENGTH_SHORT, allowQueue: Boolean = true): Toast =
        create(context, message, Type.Warning, duration, allowQueue)

    @CheckResult
    fun warning(context: Context, @StringRes message: Int, duration: Int = Toast.LENGTH_SHORT, allowQueue: Boolean = true): Toast =
        warning(context, context.getString(message), duration, allowQueue)

    private enum class Type(@param:DrawableRes val icon: Int, @param:ColorRes val iconColor: Int) {
        Normal(R.drawable.ic_toast_normal, R.color.toast_normal),
        Success(R.drawable.ic_toast_success, R.color.toast_success),
        Error(R.drawable.ic_toast_error, R.color.toast_error),
        Info(R.drawable.ic_toast_info, R.color.toast_info),
        Warning(R.drawable.ic_toast_warning, R.color.toast_warning),
    }

    @Suppress("DEPRECATION")
    private fun create(context: Context, message: CharSequence, type: Type, duration: Int, allowQueue: Boolean): Toast {
        val appContext = context.applicationContext
        val layout = LayoutInflater.from(appContext).inflate(R.layout.toast_layout, null)
        layout.background = ToastyUtils.drawable(appContext, R.drawable.toast_frame)
        layout.findViewById<ImageView>(R.id.toast_icon).setImageDrawable(
            ToastyUtils.drawable(appContext, type.icon)?.let { ToastyUtils.tint(it, ToastyUtils.color(appContext, type.iconColor)) }
        )
        layout.findViewById<TextView>(R.id.toast_text).apply {
            text = message
            setTextColor(ToastyUtils.color(appContext, R.color.toast_text))
        }

        val toast = Toast(appContext).apply {
            this.duration = duration
            view = layout
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, ToastyUtils.dpToPx(appContext, TOP_OFFSET_DP))
        }
        if (!allowQueue) {
            lastToast?.cancel()
            lastToast = toast
        }
        return toast
    }
}
