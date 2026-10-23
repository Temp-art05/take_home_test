package com.github.dinhn.ads.core

import android.app.Activity
import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Orchestrate luồng **"loading-trước-show"** chuẩn của lib cho full-screen ad
 * (interstitial / rewarded / app-open). Đây là phần tái dùng nằm **trong lib** (ads-core):
 * mọi app integrate gọi qua [AdsSdk] (vd [AdsSdk.showInterstitial]) là có loading sẵn,
 * không phải tự ghép [AdsLoadingOverlay] + delay + load-on-demand.
 *
 * Luồng:
 * 1. Hiện [AdsLoadingOverlay] ngay.
 * 2. Có [ready] (preload sẵn) → dùng luôn; chưa có → gọi [load] on-demand (loading vẫn hiển thị),
 *    tối đa [loadTimeoutMillis].
 * 3. Giữ loading tối thiểu [minLoadingMs] (tránh nháy khi load nhanh / có sẵn).
 * 4. Có ad → [show] (ẩn loading khi ad đã hiển thị / lỗi show); không có ad → ẩn loading + [onNoAd].
 *
 * Network-agnostic & **không giữ state ad** (Rule 1/2): handle + việc reload do caller quản
 * qua [ready]/[load]/[show]. [onLoading] là hook phụ cho app cập nhật UI (vd status text).
 */
object FullScreenAdPresenter {

    suspend fun presentWithLoading(
        activity: Activity,
        minLoadingMs: Long,
        loadTimeoutMillis: Long,
        ready: AdHandle?,
        load: suspend () -> AdHandle?,
        show: (handle: AdHandle, onShowed: () -> Unit, onClosed: () -> Unit, onFailed: () -> Unit) -> Unit,
        onAdShowed: () -> Unit = {},
        onAdClosed: () -> Unit = {},
        onAdFailed: () -> Unit = {},
        onNoAd: () -> Unit = {},
        onLoading: (Boolean) -> Unit = {},
        onStartLoad: () -> Unit = {},
        onLoaded: () -> Unit = {},
        onLoadFailed: () -> Unit = {},
        showOverlay: Boolean = true,
    ) {
        if (showOverlay) AdsLoadingOverlay.show(activity)   // app-open: showOverlay=false → không loading
        val startedAt = SystemClock.elapsedRealtime()
        onLoading(true)

        val handle: AdHandle?
        if (ready != null) {
            handle = ready                                  // preload sẵn → không [START_LOAD]
            onLoaded()
        } else {
            onStartLoad()                                   // load on-demand
            handle = withTimeoutOrNull(loadTimeoutMillis) { load() }
            if (handle != null) onLoaded() else onLoadFailed()
        }
        onLoading(false)

        // Đảm bảo loading hiển thị đủ tối thiểu trước khi show / next.
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        if (showOverlay && elapsed < minLoadingMs) delay(minLoadingMs - elapsed)

        if (handle != null) {
            show(
                handle,
                { if (showOverlay) AdsLoadingOverlay.hide(activity); onAdShowed() },  // ad hiển thị → gỡ loading
                { onAdClosed() },                                                     // ad đóng → loading đã gỡ
                { if (showOverlay) AdsLoadingOverlay.hide(activity); onAdFailed() },  // show lỗi → gỡ loading
            )
        } else {
            if (showOverlay) AdsLoadingOverlay.hide(activity)   // hết hạn / không có ad → gỡ loading
            onNoAd()
        }
    }
}
