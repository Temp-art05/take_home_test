package com.takehome.drama.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.takehome.drama.data.local.entity.WatchProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {
    @Upsert
    suspend fun upsert(entity: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE show_id = :showId")
    suspend fun deleteForShow(showId: String)

    @Query("SELECT * FROM watch_progress WHERE show_id = :showId AND episode_index = :episodeIndex")
    suspend fun get(showId: String, episodeIndex: Int): WatchProgressEntity?

    @Query("SELECT * FROM watch_progress WHERE show_id = :showId ORDER BY updated_at DESC LIMIT 1")
    suspend fun latestForShow(showId: String): WatchProgressEntity?

    /** Dòng mới nhất của mỗi show (GROUP BY chặn trùng khi hai tập cùng `updated_at`). */
    @Query(
        """
        SELECT * FROM watch_progress AS p
        WHERE p.updated_at = (SELECT MAX(updated_at) FROM watch_progress WHERE show_id = p.show_id)
        GROUP BY p.show_id
        ORDER BY p.updated_at DESC
        """,
    )
    fun observeLatestPerShow(): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE show_id = :showId ORDER BY episode_index")
    fun observeForShow(showId: String): Flow<List<WatchProgressEntity>>
}
