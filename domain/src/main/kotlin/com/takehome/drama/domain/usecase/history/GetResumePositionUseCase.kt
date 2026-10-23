package com.takehome.drama.domain.usecase.history

import com.takehome.drama.domain.repository.WatchProgressRepository

/** Vị trí mở lại một tập. Tập đã xem xong thì phát lại từ đầu thay vì kẹt ở giây cuối. */
class GetResumePositionUseCase(
    private val watchProgressRepository: WatchProgressRepository,
) {
    suspend operator fun invoke(showId: String, episodeIndex: Int): Long =
        watchProgressRepository.get(showId, episodeIndex)
            ?.takeUnless { it.isEpisodeCompleted }
            ?.positionMs
            ?: 0
}
