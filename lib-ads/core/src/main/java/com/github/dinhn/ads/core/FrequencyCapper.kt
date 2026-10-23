package com.github.dinhn.ads.core

/**
 * Frequency capping cho full-screen ad (P2-2) — chống ad fatigue / churn.
 *
 * In-memory (theo vòng đời process). Persistence + chỉnh ngưỡng qua Remote Config để M5.
 * Lưu timestamp/đếm theo placement — KHÔNG giữ ad object (không vi phạm "core không giữ state ad").
 */
object FrequencyCapper {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Khoảng cách tối thiểu giữa 2 lần show (ms). Mặc định 15s. */
    var defaultMinIntervalMs: Long = 15_000L

    /** Số lần show tối đa trong 1 session. Mặc định không giới hạn. */
    var defaultMaxPerSession: Int = Int.MAX_VALUE

    /** Số lần show tối đa trong 24h (rolling window). Mặc định không giới hạn. In-memory → reset khi kill app. */
    var defaultMaxPerDay: Int = Int.MAX_VALUE

    private class Stat(
        var lastShownAt: Long = 0L,
        var sessionCount: Int = 0,
        var dayWindowStart: Long = 0L,
        var dayCount: Int = 0,
    )
    private val stats = HashMap<String, Stat>()

    fun canShow(
        placement: String,
        minIntervalMs: Long = defaultMinIntervalMs,
        maxPerSession: Int = defaultMaxPerSession,
        maxPerDay: Int = defaultMaxPerDay,
    ): Boolean {
        val s = stats[placement] ?: return true
        val now = System.currentTimeMillis()
        if (now - s.lastShownAt < minIntervalMs) return false
        if (s.sessionCount >= maxPerSession) return false
        // Rolling 24h: chỉ chặn nếu vẫn trong cửa sổ ngày hiện tại và đã đạt trần.
        if (now - s.dayWindowStart < DAY_MS && s.dayCount >= maxPerDay) return false
        return true
    }

    fun recordShown(placement: String) {
        val s = stats.getOrPut(placement) { Stat() }
        val now = System.currentTimeMillis()
        s.lastShownAt = now
        s.sessionCount++
        if (now - s.dayWindowStart >= DAY_MS) { s.dayWindowStart = now; s.dayCount = 1 } else s.dayCount++
    }

    fun reset(placement: String) { stats.remove(placement) }
}
