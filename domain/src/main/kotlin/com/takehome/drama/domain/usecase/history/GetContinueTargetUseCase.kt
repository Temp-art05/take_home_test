package com.takehome.drama.domain.usecase.history

import com.takehome.drama.domain.model.Episode
import com.takehome.drama.domain.repository.WatchProgressRepository

class GetContinueTargetUseCase(
    private val watchProgressRepository: WatchProgressRepository,
) {
    /**
     * Từ bản ghi mới nhất của show: tập dở → xem tiếp đúng chỗ; tập đã xong mà còn tập kế →
     * sang tập kế từ đầu; tập cuối đã xong → ở lại tập cuối. [episodes] là file tập đã tải —
     * `index` có thể thủng lỗ nên "tập kế" là phần tử kế trong list, không phải `index + 1`.
     * Tập đã lưu không còn trong file → tập gần nhất trước nó, từ đầu (vị trí cũ thuộc tập khác).
     */
    suspend operator fun invoke(showId: String, episodes: List<Episode>): ContinueTarget {
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
}
