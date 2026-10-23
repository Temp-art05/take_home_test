package com.takehome.drama.feature.watch.presentation.player.model

import androidx.compose.runtime.Immutable

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
