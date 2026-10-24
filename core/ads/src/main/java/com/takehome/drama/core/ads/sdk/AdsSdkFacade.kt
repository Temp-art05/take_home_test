package com.takehome.drama.core.ads.sdk

import android.app.Activity
import android.content.Context
import com.github.dinhn.ads.core.AdsSdk

/** Phần lib ads mà [com.takehome.drama.core.ads.AdsPresenterImpl] dùng — interface để test bằng fake. */
interface AdsSdkFacade {
    fun isFullScreenReady(name: String): Boolean

    fun preloadFullScreen(context: Context, name: String, onLoaded: () -> Unit, onFailed: () -> Unit)

    /** [onDone]: đóng / lỗi show / không có ad. */
    fun showInterstitial(activity: Activity, name: String, onDone: () -> Unit)

    /** [onResult]: `true` khi user nhận thưởng trước lúc đóng. */
    fun showRewarded(activity: Activity, name: String, onResult: (earned: Boolean) -> Unit)

    /** Không có ad sẵn thì lib load tại chỗ (không overlay); xong tự preload lại. */
    fun showAppOpen(activity: Activity, name: String)

    fun requestNative(context: Context, name: String)

    /** Đang có interstitial / rewarded / app-open trên màn hình. */
    fun isFullScreenAdShowing(): Boolean

    /** `Long.MAX_VALUE` khi chưa từng hiện full-screen nào. */
    fun millisSinceLastFullScreenShown(): Long
}

internal class LibAdsSdkFacade : AdsSdkFacade {
    override fun isFullScreenReady(name: String): Boolean = AdsSdk.isFullScreenReady(name)

    override fun preloadFullScreen(context: Context, name: String, onLoaded: () -> Unit, onFailed: () -> Unit) =
        AdsSdk.preloadFullScreen(context, name, onLoaded, onFailed)

    override fun showInterstitial(activity: Activity, name: String, onDone: () -> Unit) =
        AdsSdk.showInterstitial(activity, name, onNext = onDone)

    override fun showRewarded(activity: Activity, name: String, onResult: (Boolean) -> Unit) =
        AdsSdk.showRewarded(activity, name, onNext = onResult)

    override fun showAppOpen(activity: Activity, name: String) = AdsSdk.showAppOpen(activity, name)

    override fun requestNative(context: Context, name: String) = AdsSdk.preloadNative(context, name)

    override fun isFullScreenAdShowing(): Boolean = AdsSdk.isFullScreenAdShowing

    override fun millisSinceLastFullScreenShown(): Long = AdsSdk.millisSinceLastFullScreenShown()
}
