package com.takehome.drama.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Bookmark cấp show. Chỉ lưu id; title/poster/status lấy từ catalogue lúc hiển thị. */
@Entity(tableName = "saved_show")
data class SavedShowEntity(
    @PrimaryKey @ColumnInfo(name = "show_id") val showId: String,
    @ColumnInfo(name = "saved_at") val savedAt: Long,
)
