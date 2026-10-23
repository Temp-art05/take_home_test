package com.github.dinhn.ads.core

/** Loại sự kiện vòng đời ad ([P3-3]). */
enum class AdEventType {
    LOAD_START, LOADED, LOAD_FAILED,
    SHOWED, IMPRESSION, CLICKED, DISMISSED,
    REWARD,
}

/**
 * Sự kiện vòng đời ad — lib PHÁT (qua [AdTracker]); app forward sang Adjust/Firebase.
 * Core/adapter KHÔNG phụ thuộc Adjust/Firebase (Rule 1). Doanh thu (ILRD) đi riêng qua [AdRevenue].
 */
data class AdEvent(
    val type: AdEventType,
    val format: String,        // Banner / Native / Interstitial / Rewarded / AppOpen
    val adUnitId: String,
    val network: String = "",  // mediationAdapterClassName khi biết
    val latencyMs: Long = -1,  // cho LOADED
    val error: String = "",    // cho *_FAILED
    val adSource: String = "", // adSourceName (cho track_ad_matched_request); rỗng nếu chưa biết
)

/** Listener nhận [AdEvent]. App đăng ký qua [AdTracker.setListener] / [AdsSdk.setOnAdEvent]. */
fun interface OnAdEvent {
    fun onEvent(event: AdEvent)
}
