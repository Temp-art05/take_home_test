package com.takehome.drama.data.repository

import com.takehome.drama.data.local.datasource.SavedShowLocalDataSource
import com.takehome.drama.data.local.entity.SavedShowEntity
import com.takehome.drama.data.mapper.toDomain
import com.takehome.drama.data.util.withLocalCatching
import com.takehome.drama.domain.model.SavedShow
import com.takehome.drama.domain.repository.SavedShowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

class SavedShowRepositoryImpl(
    private val local: SavedShowLocalDataSource,
    private val clock: () -> Long = System::currentTimeMillis,
) : SavedShowRepository {
    override suspend fun save(showId: String) = withLocalCatching { local.insert(SavedShowEntity(showId, savedAt = clock())) }

    override suspend fun remove(showId: String) = withLocalCatching { local.delete(showId) }

    override fun observeIsSaved(showId: String): Flow<Boolean> = local.observeIsSaved(showId)
        .catch { Timber.e(it, "Observe saved state failed"); emit(false) }

    override fun observeAll(): Flow<List<SavedShow>> = local.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .catch { Timber.e(it, "Observe saved shows failed"); emit(emptyList()) }
}
