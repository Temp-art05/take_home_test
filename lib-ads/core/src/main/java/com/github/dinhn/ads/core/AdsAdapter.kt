package com.github.dinhn.ads.core

import android.app.Activity
import android.content.Context
import android.view.View

/**
 * Cổng trừu tượng cho mỗi mạng quảng cáo (Facade + Adapter) — mô hình handle + callback.
 *
 * - Full-screen (inter / app open / rewarded): load theo id -> trả [AdHandle] qua [AdLoadResult];
 *   app giữ handle rồi gọi show kèm callback vòng đời.
 * - Banner / Native: tạo View theo id (native kèm layout custom).
 *
 * Phase 1: AdMob (module ads-admob). Phase 2: AppLovin MAX — không phải đổi core/app.
 */
interface AdsAdapter {
    val network: AdNetwork

    fun initialize(
        context: Context,
        testMode: Boolean,
        testDeviceIds: List<String>,
        onInitialized: () -> Unit,
    )

    // ---- Interstitial ----
    /**
     * @param preload true = load ngầm trước (log `[PRE-LOAD]`), false = load on-demand (`[START_LOAD]`).
     * @param placement tên placement (vd `InterHome`) để kèm vào log; rỗng = không kèm.
     */
    fun loadInterstitial(context: Context, adUnitId: String, result: AdLoadResult, preload: Boolean = false, placement: String = "")
    fun showInterstitial(activity: Activity, handle: AdHandle, callback: FullScreenAdCallback)

    // ---- App Open ----
    /**
     * @param preload true = load ngầm trước (log `[PRE-LOAD]`), false = load on-demand (`[START_LOAD]`).
     * @param placement tên placement để kèm vào log; rỗng = không kèm.
     */
    fun loadAppOpen(context: Context, adUnitId: String, result: AdLoadResult, preload: Boolean = false, placement: String = "")
    fun showAppOpen(activity: Activity, handle: AdHandle, callback: FullScreenAdCallback)

    // ---- Rewarded ----
    /**
     * @param preload true = load ngầm trước (log `[PRE-LOAD]`), false = load on-demand (`[START_LOAD]`).
     * @param placement tên placement để kèm vào log; rỗng = không kèm.
     */
    fun loadRewarded(context: Context, adUnitId: String, result: AdLoadResult, preload: Boolean = false, placement: String = "")
    fun showRewarded(activity: Activity, handle: AdHandle, callback: RewardAdCallback)

    // ---- Banner (single id, View-based) ----
    /** @param placement tên placement để kèm vào log; rỗng = không kèm. */
    fun createBanner(context: Context, adUnitId: String, callback: BannerAdCallback, placement: String = ""): View
    fun destroyBanner(view: View)

    // ---- Native (tách load / bind / destroy — M2a) ----
    /**
     * Load native, trả [AdHandle] bọc NativeAd qua [AdLoadResult] (KHÔNG bind ở đây).
     * @param preload true = load ngầm trước (log `[PRE-LOAD]`), false = load on-demand (`[START_LOAD]`).
     * @param placement tên placement để kèm vào log; rỗng = không kèm.
     */
    fun loadNative(context: Context, adUnitId: String, result: AdLoadResult, preload: Boolean = false, placement: String = "")

    /**
     * Bind [handle] vào layout XML và trả về View (NativeAdView) tại thời điểm render.
     * @param layoutResId layout XML (NativeAdView root). 0 = layout mặc định của adapter.
     *   Layout custom phải có view id chuẩn: ad_headline, ad_body, ad_call_to_action,
     *   ad_app_icon, ad_media (view thiếu sẽ bỏ qua an toàn).
     */
    fun bindNative(context: Context, handle: AdHandle, layoutResId: Int): View

    /** Huỷ View (NativeAdView) — KHÔNG huỷ NativeAd (handle giữ quyền đó). */
    fun destroyNative(view: View)

    /** Huỷ NativeAd (data) — chủ sở hữu handle gọi. */
    fun destroyNativeHandle(handle: AdHandle)
}
