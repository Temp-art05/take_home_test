package com.github.dinhn.ads.core

/**
 * Dispatcher trung lập cho sự kiện vòng đời ad ([P3-3]).
 *
 * - Adapter (ads-admob) gọi [emit] tại các mốc load/show/click/reward.
 * - App đăng ký [setListener] (hoặc [AdsSdk.setOnAdEvent]) để forward sang Adjust/Firebase.
 *
 * Core KHÔNG biết Adjust/Firebase — chỉ phát [AdEvent] (Rule 1).
 */
object AdTracker {

    @Volatile
    private var listener: OnAdEvent? = null

    fun setListener(l: OnAdEvent?) { listener = l }

    fun emit(event: AdEvent) { listener?.onEvent(event) }
}
