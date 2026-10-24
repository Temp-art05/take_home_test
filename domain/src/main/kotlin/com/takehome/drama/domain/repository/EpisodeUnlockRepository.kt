package com.takehome.drama.domain.repository

import kotlinx.coroutines.flow.Flow

/** Tập khoá đã mở bằng rewarded ad. Khoá = `Episode.index` (định danh tập), không phải số hiển thị. */
interface EpisodeUnlockRepository {
    fun observeUnlocked(showId: String): Flow<Set<Int>>

    suspend fun unlock(showId: String, episodeIndex: Int): Result<Unit>
}
