package com.takehome.drama.core.ads.nativead

import android.content.Context
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.takehome.drama.core.ads.R

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
private const val ICON_DP = 40
private const val TOUCH_TARGET_DP = 48
private const val RADIUS_CARD_DP = 16
private const val RADIUS_MEDIA_DP = 12
private const val RADIUS_BUTTON_DP = 12
private const val RADIUS_ICON_DP = 8
private const val RADIUS_BADGE_DP = 6

/** Màu (ARGB) + cỡ chữ (sp) lấy từ token design-system ở phía Compose. */
internal data class NativeAdStyle(
    val surface: Int,
    val button: Int,
    val badge: Int,
    val border: Int,
    val text1: Int,
    val text2: Int,
    val text3: Int,
    val headlineSp: Float,
    val bodySp: Float,
    val badgeSp: Float,
    val buttonSp: Float,
)

/**
 * Card native kiểu Sunset Flow, dựng bằng code (đề cấm XML layout): nền `surface` bo 16dp, icon 40dp +
 * headline + badge "Ad", body 2 dòng, media 16:9, CTA nền `surface2` viền `border` — **không** gradient,
 * gradient để dành cho nút chính của app. AdChoices do SDK tự đặt góc trên.
 */
internal class NativeAdCard(private val context: Context, private val style: NativeAdStyle) {

    /** `NativeAdView` là class final nên card bọc nó thay vì kế thừa. */
    val view = NativeAdView(context).also { it.tag = this }

    private val icon = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        clipToOutline = true
        outlineProvider = roundedOutline(dp(RADIUS_ICON_DP))
    }
    private val headline = text(style.headlineSp, style.text1, bold = true).apply { maxLines = 1 }
    private val badge = text(style.badgeSp, style.text2, bold = true).apply {
        setText(R.string.ads_native_badge)
        background = rounded(style.badge, dp(RADIUS_BADGE_DP))
        setPadding(dp(6), dp(1), dp(6), dp(1))
    }
    private val advertiser = text(style.bodySp, style.text3, bold = false).apply { maxLines = 1 }
    private val body = text(style.bodySp, style.text2, bold = false).apply { maxLines = 2 }
    private val media = MediaView(context)
    private val cta = text(style.buttonSp, style.text1, bold = true).apply {
        gravity = Gravity.CENTER
        maxLines = 1
        minHeight = dp(TOUCH_TARGET_DP)
        background = rounded(style.button, dp(RADIUS_BUTTON_DP)).apply { setStroke(dp(1), style.border) }
    }

    init {
        view.layoutDirection = View.LAYOUT_DIRECTION_LOCALE
        val titleRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(badge, LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = dp(6) })
            addView(headline, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        val titles = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(titleRow, LinearLayout.LayoutParams(MATCH, WRAP))
            addView(advertiser, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(icon, LinearLayout.LayoutParams(dp(ICON_DP), dp(ICON_DP)).apply { marginEnd = dp(12) })
            // Chừa góc trên-cuối cho nút AdChoices của SDK.
            addView(titles, LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginEnd = dp(20) })
        }
        val mediaFrame = AspectRatioFrame(context).apply {
            clipToOutline = true
            outlineProvider = roundedOutline(dp(RADIUS_MEDIA_DP))
            addView(media, FrameLayout.LayoutParams(MATCH, MATCH))
        }
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(style.surface, dp(RADIUS_CARD_DP))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            addView(header, LinearLayout.LayoutParams(MATCH, WRAP))
            addView(body, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })
            addView(mediaFrame, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })
            addView(cta, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })
        }
        view.addView(card, FrameLayout.LayoutParams(MATCH, WRAP))

        view.headlineView = headline
        view.bodyView = body
        view.advertiserView = advertiser
        view.iconView = icon
        view.mediaView = media
        view.callToActionView = cta
    }

    fun bind(ad: NativeAd) {
        headline.text = ad.headline.orEmpty()
        body.showText(ad.body)
        advertiser.showText(ad.advertiser)
        cta.showText(ad.callToAction)
        val drawable = ad.icon?.drawable
        icon.setImageDrawable(drawable)
        icon.visibility = if (drawable == null) View.GONE else View.VISIBLE
        media.mediaContent = ad.mediaContent
        media.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
        view.setNativeAd(ad)
    }

    companion object {
        fun from(view: NativeAdView): NativeAdCard? = view.tag as? NativeAdCard
    }

    private fun TextView.showText(value: String?) {
        text = value.orEmpty()
        visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private fun text(sizeSp: Float, color: Int, bold: Boolean) = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        ellipsize = TextUtils.TruncateAt.END
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun roundedOutline(radius: Int) = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius.toFloat())
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    /** Khung giữ media ở tỉ lệ 16:9 theo bề ngang card. */
    private class AspectRatioFrame(context: Context) : FrameLayout(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(width * 9 / 16, MeasureSpec.EXACTLY),
            )
        }
    }
}
