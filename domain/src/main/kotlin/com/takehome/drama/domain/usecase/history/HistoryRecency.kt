package com.takehome.drama.domain.usecase.history

import com.takehome.drama.domain.model.WatchProgress
import java.util.TimeZone

/** Giờ máy dạng epoch millis — tách interface để test ép mốc "bây giờ". */
fun interface EpochClock {
    fun nowMillis(): Long
}

/**
 * Chữ "khi nào" của một dòng History (mockup 4.2: "4 min left", "yesterday", "2 days ago", "last week").
 * Đếm theo **ngày lịch của máy**, không theo 24 h: 23:59 → 00:01 là hôm qua. Không dùng `java.time`
 * (minSdk 24 chưa bật desugaring) — `TimeZone.getOffset` cho đúng cả giờ mùa hè.
 */
sealed interface HistoryRecency {
    /** Tập đang dở và xem hôm nay — làm tròn lên, ít nhất 1 phút. */
    data class MinutesLeft(val minutes: Int) : HistoryRecency

    data object Today : HistoryRecency

    data object Yesterday : HistoryRecency

    /** 2–6 ngày. */
    data class DaysAgo(val days: Int) : HistoryRecency

    /** 7–13 ngày. */
    data object LastWeek : HistoryRecency

    /** Từ 14 ngày — UI format ngày theo locale. */
    data class OnDate(val epochMillis: Long) : HistoryRecency

    companion object {
        private const val DAY_MS = 86_400_000L
        private const val MINUTE_MS = 60_000L
        private const val LAST_WEEK_FROM = 7
        private const val DATE_FROM = 14

        fun of(progress: WatchProgress, nowMillis: Long, timeZone: TimeZone): HistoryRecency {
            // Mốc tương lai (đổi giờ máy) coi như hôm nay thay vì "-1 days ago".
            val days = (localDay(nowMillis, timeZone) - localDay(progress.updatedAt, timeZone)).coerceAtLeast(0)
            return when {
                days == 0L && progress.durationMs > 0 && !progress.isEpisodeCompleted ->
                    MinutesLeft(((progress.remainingMs + MINUTE_MS - 1) / MINUTE_MS).toInt().coerceAtLeast(1))
                days == 0L -> Today
                days == 1L -> Yesterday
                days < LAST_WEEK_FROM -> DaysAgo(days.toInt())
                days < DATE_FROM -> LastWeek
                else -> OnDate(progress.updatedAt)
            }
        }

        private fun localDay(epochMillis: Long, timeZone: TimeZone): Long =
            Math.floorDiv(epochMillis + timeZone.getOffset(epochMillis), DAY_MS)
    }
}
