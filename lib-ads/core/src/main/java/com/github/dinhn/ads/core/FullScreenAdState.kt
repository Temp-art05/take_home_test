package com.github.dinhn.ads.core

/**
 * Khóa full-screen toàn cục (FullScreenAdGate) — đảm bảo KHÔNG show 2 ad full màn cùng lúc
 * (interstitial / app open / rewarded). Set tại một choke point duy nhất: callback show của adapter.
 *
 * - [isShowing] = true khi một full-screen ad đang hiển thị (set ở onAdShowed).
 * - App Open phải kiểm [isShowing] trước khi tự show (resume) → không đè lên inter/reward.
 * - [lastShownAt] = thời điểm full-screen gần nhất show (mọi format) → app-open dùng làm cooldown
 *   (tránh inter/reward → app-open liền nhau, P2-2).
 *
 * Đây là state HIỂN THỊ (process-wide), không phải ad object → không vi phạm "core không giữ state ad".
 */
object FullScreenAdState {

    @Volatile
    var isShowing: Boolean = false
        private set

    /** Timestamp (ms) lần show full-screen gần nhất; 0 = chưa show lần nào. */
    @Volatile
    var lastShownAt: Long = 0L
        private set

    fun onShown() {
        isShowing = true
        lastShownAt = System.currentTimeMillis()
        AdsLogger.d("[FS_GATE] showing")
    }

    fun onClosed() {
        isShowing = false
        AdsLogger.d("[FS_GATE] closed")
    }
}
