package com.takehome.drama.data.repository

import com.takehome.drama.data.local.datasource.EpisodeUnlockLocalDataSource
import com.takehome.drama.data.util.withLocalCatching
import com.takehome.drama.domain.repository.EpisodeUnlockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

class EpisodeUnlockRepositoryImpl(
    private val local: EpisodeUnlockLocalDataSource,
) : EpisodeUnlockRepository {

    override fun observeUnlocked(showId: String): Flow<Set<Int>> {
        val prefix = "$showId$SEPARATOR"
        return local.entries
            .map { entries ->
                entries.mapNotNullTo(HashSet<Int>()) { entry -> entry.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)?.toIntOrNull() }
            }
            .distinctUntilChanged<Set<Int>>()
            .catch { Timber.e(it, "Observe unlocked episodes failed"); emit(emptySet()) }
    }

    override suspend fun unlock(showId: String, episodeIndex: Int): Result<Unit> =
        withLocalCatching { local.add("$showId$SEPARATOR$episodeIndex") }

    private companion object {
        const val SEPARATOR = "#"
    }
}
