package com.github.dinhn.ads.core

/**
 * Callback **đầy đủ vòng đời** cho một lần show full-screen (inter / reward / app-open) — app
 * override đúng hàm cần để theo dõi. Tất cả mặc định no-op nên chỉ khai báo cái mình quan tâm.
 *
 * Thứ tự điển hình (chưa preload): [onLoading]`(true)` → [onStartLoad] → [onLoaded]/[onLoadFailed]
 * → [onLoading]`(false)` → [onAdShowed] → ([onUserEarnedReward]) → [onAdDismissed].
 * Đã preload sẵn thì bỏ qua [onStartLoad] (vào thẳng [onLoaded]). Không có ad → [onNoAd].
 */
open class FullScreenAdCallbacks {
    /** Bắt đầu load on-demand (chưa preload) — tương ứng log `[START_LOAD]`. */
    open fun onStartLoad() {}
    /** Đã có ad để show (preload sẵn hoặc vừa load xong). */
    open fun onLoaded() {}
    /** Load fail / timeout → không có ad để show. */
    open fun onLoadFailed() {}
    /** Overlay loading bật/tắt (chỉ placement bật loading — inter/reward). */
    open fun onLoading(showing: Boolean) {}
    /** Ad đã hiển thị. */
    open fun onAdShowed() {}
    /** Show lỗi (có handle nhưng show fail). */
    open fun onAdFailedToShow() {}
    /** Ad đóng (user dismiss / hết). */
    open fun onAdDismissed() {}
    /** Không show được vì: chưa đăng ký / đang có full-screen khác / frequency cap / placement tắt / hết hạn. */
    open fun onNoAd() {}
    /** Chỉ rewarded: user nhận thưởng. */
    open fun onUserEarnedReward(type: String, amount: Int) {}
}
