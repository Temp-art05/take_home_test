package com.takehome.drama.domain.usecase.history

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * History tách hai ý định: xem tiếp (Watching — cũng là "Pick up your flow" ở Home) và tìm lại show
 * đã xong (Finished = bản ghi mới nhất là tập cuối và đã xem hết). Show không còn trong catalogue bị bỏ.
 */
class ObserveWatchHistoryUseCase(
    private val watchProgressRepository: WatchProgressRepository,
) {
    operator fun invoke(catalog: Catalog): Flow<WatchHistory> =
        watchProgressRepository.observeLatestPerShow().map { latest ->
            val showsById = catalog.shows.associateBy { it.id }
            val items = latest.mapNotNull { progress -> showsById[progress.showId]?.let { HistoryItem(it, progress) } }
            val (finished, watching) = items.partition { it.progress.isLastEpisode && it.progress.isEpisodeCompleted }
            WatchHistory(watching = watching, finished = finished)
        }
}
