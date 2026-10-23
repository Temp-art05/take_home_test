package com.takehome.drama.data.local.datasource

import com.takehome.drama.data.local.dao.BookmarkDao
import com.takehome.drama.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

class BookmarkLocalDataSourceImpl(
    private val bookmarkDao: BookmarkDao,
) : BookmarkLocalDataSource {
    override fun observeIds(): Flow<List<Int>> = bookmarkDao.observeIds()
    override suspend fun exists(id: Int): Boolean = bookmarkDao.exists(id)
    override suspend fun insert(entity: BookmarkEntity) = bookmarkDao.insert(entity)
    override suspend fun delete(id: Int) = bookmarkDao.deleteById(id)
}
