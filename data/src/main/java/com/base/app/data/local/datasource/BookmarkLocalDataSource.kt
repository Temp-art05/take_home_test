package com.base.app.data.local.datasource

import com.base.app.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

/** SAMPLE — gói DAO lại, chỉ nói chuyện bằng Entity. */
interface BookmarkLocalDataSource {
    fun observeIds(): Flow<List<Int>>
    suspend fun exists(id: Int): Boolean
    suspend fun insert(entity: BookmarkEntity)
    suspend fun delete(id: Int)
}
