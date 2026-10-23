package com.takehome.drama.data.local.datasource

import com.takehome.drama.data.local.dao.WatchProgressDao
import com.takehome.drama.data.local.entity.WatchProgressEntity
import kotlinx.coroutines.flow.Flow

interface WatchProgressLocalDataSource {
    suspend fun upsert(entity: WatchProgressEntity)

    suspend fun get(showId: String, episodeIndex: Int): WatchProgressEntity?

    suspend fun latestForShow(showId: String): WatchProgressEntity?

    fun observeLatestPerShow(): Flow<List<WatchProgressEntity>>
}

class WatchProgressLocalDataSourceImpl(
    private val dao: WatchProgressDao,
) : WatchProgressLocalDataSource {
    override suspend fun upsert(entity: WatchProgressEntity) = dao.upsert(entity)

    override suspend fun get(showId: String, episodeIndex: Int) = dao.get(showId, episodeIndex)

    override suspend fun latestForShow(showId: String) = dao.latestForShow(showId)

    override fun observeLatestPerShow() = dao.observeLatestPerShow()
}
