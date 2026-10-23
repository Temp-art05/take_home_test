package com.takehome.drama.data.local.datasource

import com.takehome.drama.data.local.dao.SavedShowDao
import com.takehome.drama.data.local.entity.SavedShowEntity
import kotlinx.coroutines.flow.Flow

interface SavedShowLocalDataSource {
    suspend fun insert(entity: SavedShowEntity)

    suspend fun delete(showId: String)

    fun observeIsSaved(showId: String): Flow<Boolean>

    fun observeAll(): Flow<List<SavedShowEntity>>
}

class SavedShowLocalDataSourceImpl(
    private val dao: SavedShowDao,
) : SavedShowLocalDataSource {
    override suspend fun insert(entity: SavedShowEntity) = dao.insert(entity)

    override suspend fun delete(showId: String) = dao.delete(showId)

    override fun observeIsSaved(showId: String) = dao.observeIsSaved(showId)

    override fun observeAll() = dao.observeAll()
}
