package com.github.dinhn.ads.core

/** Kết quả load full-screen ad: trả handle để giữ & show sau, hoặc lỗi. */
interface AdLoadResult {
    fun onLoaded(handle: AdHandle)
    fun onFailed(error: String)
}

/** Vòng đời show full-screen ad. */
interface FullScreenAdCallback {
    fun onAdShowed() {}
    fun onAdImpression() {}
    fun onAdClicked() {}
    fun onAdDismissed() {}
    fun onAdFailedToShow(error: String) {}
}

/** Show rewarded (kèm phần thưởng). */
interface RewardAdCallback : FullScreenAdCallback {
    fun onUserEarnedReward(type: String, amount: Int) {}
}

/** Callback banner (View-based). */
interface BannerAdCallback {
    fun onLoaded() {}
    fun onFailed(error: String) {}
    fun onImpression() {}
    fun onClicked() {}
}
