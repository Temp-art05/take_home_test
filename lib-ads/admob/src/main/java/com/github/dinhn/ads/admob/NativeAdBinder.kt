package com.github.dinhn.ads.admob

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Bind dữ liệu NativeAd vào layout và trả về NativeAdView đã populate.
 *
 * Layout dùng: [layoutResId] do app truyền vào (custom, bắt buộc khác 0 — FlowDrama bỏ layout mặc định).
 * Mọi view id đều tra cứu null-safe -> layout custom có thể bỏ bớt view (vd không cần media).
 */
internal object NativeAdBinder {

    // N07/N08: cắt phần HIỂN THỊ theo policy Google (không sửa asset gửi setNativeAd).
    private const val HEADLINE_MAX = 25
    private const val BODY_MAX = 90

    private fun truncate(s: String, max: Int): String =
        if (s.length > max) s.take(max).trimEnd() + "…" else s

    fun bind(
        context: Context,
        nativeAd: NativeAd,
        layoutResId: Int = 0,
    ): View {
        // FlowDrama: bỏ layout XML mặc định (đề cấm XML layout) — app dựng NativeAdView bằng code
        // từ `AdHandle.payload`; đường bind theo layout chỉ còn cho caller tự truyền layout.
        require(layoutResId != 0) { "FlowDrama: no default native layout - build the NativeAdView in code" }
        val adView = LayoutInflater.from(context).inflate(layoutResId, null) as NativeAdView

        adView.findViewById<TextView?>(R.id.ad_headline)?.let { v ->
            v.text = truncate(nativeAd.headline ?: "", HEADLINE_MAX)
            adView.headlineView = v
        }

        adView.findViewById<TextView?>(R.id.ad_body)?.let { v ->
            val body = nativeAd.body
            if (body == null) {
                v.visibility = View.GONE
            } else {
                v.visibility = View.VISIBLE
                v.text = truncate(body, BODY_MAX)
            }
            adView.bodyView = v
        }

        adView.findViewById<Button?>(R.id.ad_call_to_action)?.let { v ->
            val cta = nativeAd.callToAction
            if (cta == null) {
                v.visibility = View.GONE
            } else {
                v.visibility = View.VISIBLE
                v.text = cta
            }
            adView.callToActionView = v
        }

        adView.findViewById<ImageView?>(R.id.ad_app_icon)?.let { v ->
            val iconAsset = nativeAd.icon
            if (iconAsset == null) {
                v.visibility = View.GONE
            } else {
                v.setImageDrawable(iconAsset.drawable)
                v.visibility = View.VISIBLE
            }
            adView.iconView = v
        }

        adView.findViewById<MediaView?>(R.id.ad_media)?.let { v ->
            v.mediaContent = nativeAd.mediaContent
            adView.mediaView = v
        }

        adView.setNativeAd(nativeAd)
        return adView
    }

    /** Chỉ huỷ NativeAdView (unbind). KHÔNG huỷ NativeAd — chủ sở hữu handle giữ quyền đó (M2a). */
    fun destroyView(view: View) {
        (view as? NativeAdView)?.destroy()
    }
}
