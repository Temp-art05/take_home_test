package com.takehome.drama.domain.repository

import com.takehome.drama.domain.model.WatchProgress
import kotlinx.coroutines.flow.Flow

interface WatchProgressRepository {
    suspend fun save(progress: WatchProgress): Result<Unit>

    /** Lỗi đọc DB → null, coi như chưa xem (đã log): mở từ đầu tốt hơn chặn phát. */
    suspend fun get(showId: String, episodeIndex: Int): WatchProgress?

    /** Lỗi đọc DB → null như [get]. */
    suspend fun latestForShow(showId: String): WatchProgress?

    /** Bản ghi mới nhất của mỗi show, mới → cũ. Lỗi đọc DB → rỗng (đã log). */
    fun observeLatestPerShow(): Flow<List<WatchProgress>>
}
