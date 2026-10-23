package com.takehome.drama.domain.usecase.history

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Episode
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Tập sẽ mở khi bấm "Continue Ep. N" / "Play Ep 1". */
data class ContinueTarget(
    val episodeIndex: Int,
    val positionMs: Long,
    val hasProgress: Boolean,
)

data class HistoryItem(val show: Show, val progress: WatchProgress)

data class WatchHistory(
    val watching: List<HistoryItem>,
    val finished: List<HistoryItem>,
)

class WatchProgressUseCase(
    private val watchProgressRepository: WatchProgressRepository,
) {
    suspend fun save(progress: WatchProgress): Result<Unit> = watchProgressRepository.save(progress)

    /** Vị trí mở lại một tập. Tập đã xem xong thì phát lại từ đầu thay vì kẹt ở giây cuối. */
    suspend fun resumePosition(showId: String, episodeIndex: Int): Long =
        watchProgressRepository.get(showId, episodeIndex)
            ?.takeUnless { it.isEpisodeCompleted }
            ?.positionMs
            ?: 0

    /**
     * Từ bản ghi mới nhất của show: tập dở → xem tiếp đúng chỗ; tập đã xong mà còn tập kế →
     * sang tập kế từ đầu; tập cuối đã xong → ở lại tập cuối. [episodes] là file tập đã tải —
     * `index` có thể thủng lỗ nên "tập kế" là phần tử kế trong list, không phải `index + 1`.
     * Tập đã lưu không còn trong file → tập gần nhất trước nó, từ đầu (vị trí cũ thuộc tập khác).
     */
    suspend fun continueTarget(showId: String, episodes: List<Episode>): ContinueTarget {
        val firstIndex = episodes.firstOrNull()?.index ?: 0
        val latest = watchProgressRepository.latestForShow(showId)
            ?: return ContinueTarget(episodeIndex = firstIndex, positionMs = 0, hasProgress = false)
        val position = episodes.indexOfFirst { it.index == latest.episodeIndex }
        return when {
            position < 0 -> ContinueTarget(
                episodes.lastOrNull { it.index < latest.episodeIndex }?.index ?: firstIndex, 0, true,
            )
            !latest.isEpisodeCompleted -> ContinueTarget(latest.episodeIndex, latest.positionMs, true)
            else -> ContinueTarget(episodes.getOrNull(position + 1)?.index ?: latest.episodeIndex, 0, true)
        }
    }

    /**
     * History tách hai ý định: xem tiếp (Watching) và tìm lại show đã xong (Finished = bản ghi
     * mới nhất là tập cuối và đã xem hết). Show không còn trong catalogue bị bỏ khỏi danh sách.
     */
    fun observeHistory(catalog: Catalog): Flow<WatchHistory> =
        watchProgressRepository.observeLatestPerShow().map { latest ->
            val showsById = catalog.shows.associateBy { it.id }
            val items = latest.mapNotNull { progress -> showsById[progress.showId]?.let { HistoryItem(it, progress) } }
            val (finished, watching) = items.partition { it.progress.isLastEpisode && it.progress.isEpisodeCompleted }
            WatchHistory(watching = watching, finished = finished)
        }

}
