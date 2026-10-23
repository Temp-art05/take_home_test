package com.takehome.drama.domain.usecase.discovery

/** Ba khung giờ của Home Day Cycle (SCR-0101) — cũng là ba lần ping của SCR-0005. */
enum class Daypart { Morning, Lunch, Night }

/** Giờ:phút của đồng hồ máy. Không dùng `java.time`: minSdk 24 chưa bật desugaring. */
data class ClockTime(val hour: Int, val minute: Int)

/** Nguồn giờ máy — sau interface để test (và flavor dev) ép được một giờ cố định. */
fun interface WallClock {
    fun now(): ClockTime
}

/** Giờ mở của mỗi khung trên thẻ Home — cũng là giờ ping ở SCR-0005. */
val Daypart.opensAt: ClockTime
    get() = when (this) {
        Daypart.Morning -> ClockTime(7, 0)
        Daypart.Lunch -> ClockTime(12, 30)
        Daypart.Night -> ClockTime(21, 0)
    }

/** Morning 05:00–10:59, Lunch 11:00–16:59, còn lại (17:00–04:59) là Night. */
fun daypartAt(time: ClockTime): Daypart = when (time.hour) {
    in MORNING_START until LUNCH_START -> Daypart.Morning
    in LUNCH_START until NIGHT_START -> Daypart.Lunch
    else -> Daypart.Night
}

/** Hai khung giờ còn lại theo thứ tự sắp tới — Night → Morning, Lunch (như mockup lúc 21:45). */
fun Daypart.upcoming(): List<Daypart> {
    val all = Daypart.entries
    return List(all.size - 1) { offset -> all[(ordinal + offset + 1) % all.size] }
}

private const val MORNING_START = 5
private const val LUNCH_START = 11
private const val NIGHT_START = 17
