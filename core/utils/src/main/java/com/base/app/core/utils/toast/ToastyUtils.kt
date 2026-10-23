package com.base.app.core.utils.toast

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

internal object ToastyUtils {

    fun tint(drawable: Drawable, @ColorInt color: Int): Drawable = drawable.mutate().apply {
        colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    fun drawable(context: Context, @DrawableRes id: Int): Drawable? = ContextCompat.getDrawable(context, id)

    @ColorInt
    fun color(context: Context, @ColorRes id: Int): Int = ContextCompat.getColor(context, id)

    fun dpToPx(context: Context, dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
}
