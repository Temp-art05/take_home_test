package com.takehome.drama.domain.fake

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.SavedShow
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.repository.CatalogRepository
import com.takehome.drama.domain.repository.SavedShowRepository
import com.takehome.drama.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeCatalogRepository(
    var catalog: Result<Catalog>,
    var episodes: Map<String, ShowEpisodes> = emptyMap(),
) : CatalogRepository {
    override suspend fun getCatalog(forceRefresh: Boolean) = catalog

    override suspend fun getEpisodes(showId: String, forceRefresh: Boolean): Result<ShowEpisodes> =
        episodes[showId]?.let { Result.success(it) } ?: Result.failure(NoSuchElementException(showId))
}

class FakeWatchProgressRepository : WatchProgressRepository {
    val rows = MutableStateFlow<List<WatchProgress>>(emptyList())

    override suspend fun save(progress: WatchProgress): Result<Unit> {
        rows.update { list ->
            list.filterNot { it.showId == progress.showId && it.episodeIndex == progress.episodeIndex } + progress
        }
        return Result.success(Unit)
    }

    override suspend fun get(showId: String, episodeIndex: Int): WatchProgress? =
        rows.value.firstOrNull { it.showId == showId && it.episodeIndex == episodeIndex }

    override suspend fun latestForShow(showId: String): WatchProgress? =
        rows.value.filter { it.showId == showId }.maxByOrNull { it.updatedAt }

    override fun observeLatestPerShow(): Flow<List<WatchProgress>> = rows.map { list ->
        list.groupBy { it.showId }.values.map { it.maxBy { p -> p.updatedAt } }.sortedByDescending { it.updatedAt }
    }

    override fun observeForShow(showId: String): Flow<List<WatchProgress>> =
        rows.map { list -> list.filter { it.showId == showId }.sortedBy { it.episodeIndex } }
}

class FakeSavedShowRepository : SavedShowRepository {
    val rows = MutableStateFlow<List<SavedShow>>(emptyList())
    private var clock = 0L

    override suspend fun save(showId: String): Result<Unit> {
        rows.update { list -> list.filterNot { it.showId == showId } + SavedShow(showId, ++clock) }
        return Result.success(Unit)
    }

    override suspend fun remove(showId: String): Result<Unit> {
        rows.update { list -> list.filterNot { it.showId == showId } }
        return Result.success(Unit)
    }

    override fun observeIsSaved(showId: String): Flow<Boolean> = rows.map { list -> list.any { it.showId == showId } }

    override fun observeAll(): Flow<List<SavedShow>> = rows.map { list -> list.sortedByDescending { it.savedAt } }
}
