package com.takehome.drama.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.takehome.drama.data.local.entity.SavedShowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedShowDao {
    /** IGNORE: lưu lại show đã lưu không đổi `saved_at` (thứ tự trong Saved giữ nguyên). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: SavedShowEntity)

    @Query("DELETE FROM saved_show WHERE show_id = :showId")
    suspend fun delete(showId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_show WHERE show_id = :showId)")
    fun observeIsSaved(showId: String): Flow<Boolean>

    @Query("SELECT * FROM saved_show ORDER BY saved_at DESC")
    fun observeAll(): Flow<List<SavedShowEntity>>
}
