package com.takehome.drama.domain.usecase.library

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.repository.SavedShowRepository
import com.takehome.drama.domain.usecase.history.HistoryItem
import com.takehome.drama.domain.usecase.history.ObserveWatchHistoryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Số trên thẻ Library + dải Recent (mockup 4.1 vẽ 2 dòng). */
data class LibrarySummary(
    val watchedCount: Int,
    val savedCount: Int,
    val recent: List<HistoryItem>,
)

/**
 * Watched = mọi show trong History (Watching + Finished), Saved = show đã lưu còn trong catalogue —
 * cùng nguồn với hai màn con nên số trên thẻ luôn khớp danh sách người dùng mở ra.
 */
class ObserveLibrarySummaryUseCase(
    private val observeWatchHistory: ObserveWatchHistoryUseCase,
    private val savedShowRepository: SavedShowRepository,
) {
    operator fun invoke(catalog: Catalog): Flow<LibrarySummary> {
        val showIds = catalog.shows.mapTo(HashSet()) { it.id }
        return combine(observeWatchHistory(catalog), savedShowRepository.observeAll()) { history, saved ->
            LibrarySummary(
                watchedCount = history.watching.size + history.finished.size,
                savedCount = saved.count { it.showId in showIds },
                recent = history.watching.take(RECENT_LIMIT),
            )
        }.distinctUntilChanged()
    }

    private companion object {
        const val RECENT_LIMIT = 2
    }
}
