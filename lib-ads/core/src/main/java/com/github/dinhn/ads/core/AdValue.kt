package com.github.dinhn.ads.core

/**
 * Doanh thu của MỘT impression (impression-level ad revenue / ILRD).
 *
 * Lib chỉ PHÁT giá trị này (qua [AdRevenue]); tầng app quyết định forward đi đâu
 * (Firebase `ad_impression`, Adjust/AppsFlyer ad revenue). Core/adapter KHÔNG phụ thuộc
 * Firebase/MMP (Rule 1).
 */
data class AdValue(
    val valueMicros: Long,     // giá trị * 1_000_000
    val currencyCode: String,
    val precision: Int,        // 0=UNKNOWN, 1=ESTIMATED, 2=PUBLISHER_PROVIDED, 3=PRECISE
    val network: String,       // mediationAdapterClassName (network thắng impression)
    val adUnitId: String,
    val format: String,        // Banner / Native / Interstitial / Rewarded / AppOpen
    val adSource: String = "",          // adSourceName từ loadedAdapterResponseInfo (tên nguồn ad thắng)
    val adSourceUnitId: String? = null, // ad source unit id (Apero 9.0.3 luôn null)
)

/** Listener nhận paid event. App đăng ký qua [AdRevenue.setListener] / [AdsSdk.setOnPaidEvent]. */
fun interface OnPaidEvent {
    fun onPaid(value: AdValue)
}
