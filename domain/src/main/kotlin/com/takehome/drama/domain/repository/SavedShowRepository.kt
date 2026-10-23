package com.takehome.drama.domain.repository

import com.takehome.drama.domain.model.SavedShow
import kotlinx.coroutines.flow.Flow

interface SavedShowRepository {
    suspend fun save(showId: String): Result<Unit>

    suspend fun remove(showId: String): Result<Unit>

    /** Lỗi đọc DB → `false` (đã log). */
    fun observeIsSaved(showId: String): Flow<Boolean>

    /** Mới lưu → cũ. Lỗi đọc DB → rỗng (đã log). */
    fun observeAll(): Flow<List<SavedShow>>
}
