package com.github.dinhn.ads.core

/**
 * Dispatcher trung lập cho impression-level ad revenue (P2-1).
 *
 * - Adapter (ads-admob) gọi [emit] khi network phát paid event.
 * - App đăng ký [setListener] (hoặc [AdsSdk.setOnPaidEvent]) để forward sang Firebase/MMP.
 *
 * Core KHÔNG biết Firebase/Adjust — chỉ phát [AdValue] (Rule 1).
 */
object AdRevenue {

    @Volatile
    private var listener: OnPaidEvent? = null

    fun setListener(l: OnPaidEvent?) { listener = l }

    /** Adapter gọi khi có paid event. Log + chuyển cho listener của app (nếu có). */
    fun emit(value: AdValue) {
        AdsLogger.paid(value)
        listener?.onPaid(value)
    }
}
