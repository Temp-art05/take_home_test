package com.takehome.drama.core.ads

import android.app.Activity
import android.content.Context
import com.takehome.drama.core.ads.sdk.AdsSdkFacade

/** Lib giả: ghi lại lời gọi; test tự gọi callback để giả lập load xong / lỗi / đóng ad. */
class FakeAdsSdk : AdsSdkFacade {
    val ready = mutableSetOf<String>()
    var throwOnShow = false
    var throwOnPreload = false
    val shown = mutableListOf<String>()
    val preloads = mutableListOf<String>()
    /** Callback lần preload gần nhất — test tự gọi để giả lập load xong/lỗi. */
    var pendingLoaded: (() -> Unit)? = null
    var pendingFailed: (() -> Unit)? = null
    /** Kết quả giả của rewarded khi đóng: earned hay không. */
    var rewardEarned = true
    var dismissInterstitial: (() -> Unit)? = null

    override fun isFullScreenReady(name: String) = name in ready

    override fun preloadFullScreen(context: Context, name: String, onLoaded: () -> Unit, onFailed: () -> Unit) {
        if (throwOnPreload) error("sdk not initialised")
        preloads += name
        pendingLoaded = { ready += name; onLoaded() }
        pendingFailed = onFailed
    }

    override fun showInterstitial(activity: Activity, name: String, onDone: () -> Unit) {
        if (throwOnShow) error("boom")
        shown += name
        dismissInterstitial = onDone
    }

    override fun showRewarded(activity: Activity, name: String, onResult: (Boolean) -> Unit) {
        if (throwOnShow) error("boom")
        shown += name
        onResult(rewardEarned)
    }

    val appOpenShown = mutableListOf<String>()
    var fullScreenShowing = false
    var sinceLastFullScreenMs = Long.MAX_VALUE

    override fun showAppOpen(activity: Activity, name: String) {
        appOpenShown += name
    }

    override fun requestNative(context: Context, name: String) = Unit

    override fun isFullScreenAdShowing() = fullScreenShowing

    override fun millisSinceLastFullScreenShown() = sinceLastFullScreenMs
}
