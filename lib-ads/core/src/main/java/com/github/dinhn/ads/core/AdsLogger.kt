package com.github.dinhn.ads.core

import android.util.Log

/**
 * Logger của SDK (public để module adapter ads-admob dùng chung).
 *
 * - Tag cố định [TAG] = "FOR_TEST_ADS" để dễ filter logcat khi test ad.
 * - [enabled] do app bật/tắt qua AdsConfig.isShowLogAds (set trong AdsSdk.init / setShowLogAds).
 *   Mặc định false để release không spam log.
 *
 * Vòng đời 1 ad:
 *   load:  startLoad -> loaded / failed
 *   show (full-screen): impression / clicked -> dismissed (hoặc failedToShow)
 *   rewarded: ... -> reward -> dismissed
 */
object AdsLogger {
    const val TAG = "FOR_TEST_ADS"

    @Volatile
    var enabled: Boolean = false

    fun d(msg: String) { if (enabled) Log.d(TAG, msg) }
    fun w(msg: String) { if (enabled) Log.w(TAG, msg) }
    fun e(msg: String, tr: Throwable? = null) { if (enabled) Log.e(TAG, msg, tr) }

    /**
     * Nhãn ad trong log = `format` (+ `[placement]` nếu tầng impl có truyền tên placement).
     * VD `Banner` (không placement) hoặc `Banner[BannerHome]` (có placement) — dễ trace theo vị trí.
     */
    private fun label(format: String, placement: String) =
        if (placement.isBlank()) format else "$format[$placement]"

    /* ---- Load ---- */
    /** [preload] = true ⇒ tag `[PRE-LOAD]` (load ngầm trước, chưa cần show); false ⇒ `[START_LOAD]`. */
    fun startLoad(format: String, unitId: String, preload: Boolean = false, placement: String = "") =
        d("[${if (preload) "PRE-LOAD" else "START_LOAD"}] ${label(format, placement)} unitId='$unitId'")
    fun loaded(format: String, unitId: String, placement: String = "") = d("[LOADED] ${label(format, placement)} unitId='$unitId'")
    fun failed(format: String, unitId: String, error: String, placement: String = "") = w("[FAILED] ${label(format, placement)} unitId='$unitId' error=$error")

    /* ---- Show (full-screen) ---- */
    fun showed(format: String, unitId: String, placement: String = "") = d("[SHOWED] ${label(format, placement)} unitId='$unitId'")
    fun impression(format: String, unitId: String, placement: String = "") = d("[IMPRESSION] ${label(format, placement)} unitId='$unitId'")
    fun clicked(format: String, unitId: String, placement: String = "") = d("[CLICKED] ${label(format, placement)} unitId='$unitId'")
    fun dismissed(format: String, unitId: String, placement: String = "") = d("[DISMISSED] ${label(format, placement)} unitId='$unitId'")
    fun failedToShow(format: String, unitId: String, error: String, placement: String = "") = w("[SHOW_FAILED] ${label(format, placement)} unitId='$unitId' error=$error")
    fun reward(format: String, unitId: String, type: String, amount: Int, placement: String = "") = d("[REWARD] ${label(format, placement)} unitId='$unitId' type='$type' amount=$amount")

    /* ---- Revenue (ILRD) ---- */
    fun paid(v: AdValue) = d("[PAID] ${v.format} unitId='${v.adUnitId}' micros=${v.valueMicros} ${v.currencyCode} prec=${v.precision} net='${v.network}'")
}
