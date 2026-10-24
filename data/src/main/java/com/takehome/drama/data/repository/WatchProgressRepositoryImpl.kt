package com.takehome.drama.data.repository

import com.takehome.drama.data.local.datasource.WatchProgressLocalDataSource
import com.takehome.drama.data.mapper.toDomain
import com.takehome.drama.data.mapper.toEntity
import com.takehome.drama.data.util.withLocalCatching
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

class WatchProgressRepositoryImpl(
    private val local: WatchProgressLocalDataSource,
) : WatchProgressRepository {
    override suspend fun save(progress: WatchProgress) = withLocalCatching { local.upsert(progress.toEntity()) }

    override suspend fun removeShow(showId: String) = withLocalCatching { local.deleteForShow(showId) }

    override suspend fun get(showId: String, episodeIndex: Int): WatchProgress? =
        withLocalCatching { local.get(showId, episodeIndex)?.toDomain() }
            .onFailure { Timber.e(it, "Read watch progress failed") }
            .getOrNull()

    override suspend fun latestForShow(showId: String): WatchProgress? =
        withLocalCatching { local.latestForShow(showId)?.toDomain() }
            .onFailure { Timber.e(it, "Read latest watch progress failed") }
            .getOrNull()

    override fun observeLatestPerShow(): Flow<List<WatchProgress>> =
        local.observeLatestPerShow()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { Timber.e(it, "Observe watch history failed"); emit(emptyList()) }

    override fun observeForShow(showId: String): Flow<List<WatchProgress>> =
        local.observeForShow(showId)
            .map { rows -> rows.map { it.toDomain() } }
            .catch { Timber.e(it, "Observe show progress failed"); emit(emptyList()) }
}
