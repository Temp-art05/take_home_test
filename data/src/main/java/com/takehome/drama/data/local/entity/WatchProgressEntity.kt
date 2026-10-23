package com.takehome.drama.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/** Một dòng mỗi (show, tập): mở lại tập nào cũng xem tiếp đúng chỗ của tập đó. */
@Entity(tableName = "watch_progress", primaryKeys = ["show_id", "episode_index"])
data class WatchProgressEntity(
    @ColumnInfo(name = "show_id") val showId: String,
    /** Vị trí trong mảng tập — định danh tập, không dùng `number`. */
    @ColumnInfo(name = "episode_index") val episodeIndex: Int,
    @ColumnInfo(name = "episode_number") val episodeNumber: Int,
    @ColumnInfo(name = "episode_total") val episodeTotal: Int,
    @ColumnInfo(name = "position_ms") val positionMs: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
