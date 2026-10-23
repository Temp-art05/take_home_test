package com.takehome.drama.feature.watch.presentation.player.model

import androidx.compose.runtime.Immutable

/** Tốc độ phát của SCR-0302 (rail) và SCR-0304 (chip). */
object PlaybackSpeed {
    val options = listOf(0.5f, 1f, 1.5f, 2f, 3f)

    private val railCycle = listOf(1f, 1.5f, 2f)

    /** Chạm "1×" trên rail: 1 → 1.5 → 2 → 1. Tốc độ ngoài vòng (0.5, 3 chọn ở sheet) về 1. */
    fun next(current: Float): Float {
        val position = railCycle.indexOf(current)
        return if (position < 0) 1f else railCycle[(position + 1) % railCycle.size]
    }

    /** `1.0` → `"1"`, `1.5` → `"1.5"`. */
    fun label(speed: Float): String =
        if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
}

enum class PlayerSheet { None, Episodes, Options }

/**
 * Một trang của pager dọc — đủ thông tin để overlay (số tập, like, track) trượt cùng trang, kể cả
 * trang lân cận chưa phát. Ổn định cho Compose: trang không recompose khi state khác đổi.
 */
@Immutable
data class PlayerPage(
    val episodeIndex: Int,
    val isLocked: Boolean,
    val episodeNumber: Int = episodeIndex + 1,
    /** Đã cộng like cục bộ. */
    val likeCount: Long = 0,
    val isLiked: Boolean = false,
    /** Từ bản ghi tiến độ (track tĩnh của trang lân cận); 0 = chưa xem lần nào. */
    val durationMs: Long = 0,
)
