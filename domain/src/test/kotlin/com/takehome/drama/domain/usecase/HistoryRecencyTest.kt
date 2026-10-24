package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.usecase.history.HistoryRecency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class HistoryRecencyTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val bangkok = TimeZone.getTimeZone("GMT+07:00")

    private fun at(zone: TimeZone, year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    private fun progress(updatedAt: Long, positionMs: Long = 100_000, durationMs: Long = 100_000) =
        WatchProgress("a", 0, 1, 10, positionMs, durationMs, updatedAt)

    private fun recency(updatedAt: Long, now: Long, zone: TimeZone = utc, positionMs: Long = 100_000) =
        HistoryRecency.of(progress(updatedAt, positionMs), nowMillis = now, timeZone = zone)

    private val now = at(utc, 2026, 10, 24, 15)

    @Test
    fun `episode in progress today shows minutes left, rounded up`() {
        // còn 61 s → 2 phút
        assertEquals(
            HistoryRecency.MinutesLeft(2),
            HistoryRecency.of(progress(at(utc, 2026, 10, 24, 9), positionMs = 39_000), now, utc),
        )
        // còn 10 s → 1 phút (không bao giờ "0 min left")
        assertEquals(
            HistoryRecency.MinutesLeft(1),
            HistoryRecency.of(progress(at(utc, 2026, 10, 24, 9), positionMs = 90_000), now, utc),
        )
    }

    @Test
    fun `finished episode or unknown duration today is just today`() {
        assertEquals(HistoryRecency.Today, recency(at(utc, 2026, 10, 24, 1), now))
        assertEquals(
            HistoryRecency.Today,
            HistoryRecency.of(progress(at(utc, 2026, 10, 24, 1), positionMs = 0, durationMs = 0), now, utc),
        )
    }

    @Test
    fun `crossing midnight is yesterday even two minutes apart`() {
        assertEquals(
            HistoryRecency.Yesterday,
            recency(at(utc, 2026, 10, 23, 23, 59), now = at(utc, 2026, 10, 24, 0, 1), positionMs = 10_000),
        )
    }

    @Test
    fun `two to six days are days ago, seven to thirteen last week, then a date`() {
        assertEquals(HistoryRecency.DaysAgo(2), recency(at(utc, 2026, 10, 22, 20), now))
        assertEquals(HistoryRecency.DaysAgo(6), recency(at(utc, 2026, 10, 18, 1), now))
        assertEquals(HistoryRecency.LastWeek, recency(at(utc, 2026, 10, 17, 23), now))
        assertEquals(HistoryRecency.LastWeek, recency(at(utc, 2026, 10, 11, 0), now))
        val old = at(utc, 2026, 10, 10, 12)
        assertEquals(HistoryRecency.OnDate(old), recency(old, now))
    }

    @Test
    fun `timestamp in the future after a clock change counts as today`() {
        assertEquals(HistoryRecency.Today, recency(now + 3 * 86_400_000L, now))
    }

    @Test
    fun `days follow the device time zone, not UTC`() {
        // 01:00 ở UTC+7 ngày 24 = 18:00 UTC ngày 23. Xem lúc 23:30 ngày 23 (giờ máy) → hôm qua.
        val watched = at(bangkok, 2026, 10, 23, 23, 30)
        val viewedAt = at(bangkok, 2026, 10, 24, 1)
        assertEquals(HistoryRecency.Yesterday, recency(watched, viewedAt, zone = bangkok))
        // Cùng hai mốc đó theo UTC là cùng một ngày.
        assertEquals(HistoryRecency.Today, recency(watched, viewedAt, zone = utc))
    }
}
