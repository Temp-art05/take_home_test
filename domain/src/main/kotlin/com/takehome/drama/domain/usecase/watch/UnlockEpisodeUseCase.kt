package com.takehome.drama.domain.usecase.watch

import com.takehome.drama.domain.repository.EpisodeUnlockRepository

/** Mở một tập khoá sau khi user xem hết rewarded ad. Mở vĩnh viễn (sống qua restart). */
class UnlockEpisodeUseCase(
    private val episodeUnlockRepository: EpisodeUnlockRepository,
) {
    suspend operator fun invoke(showId: String, episodeIndex: Int): Result<Unit> =
        episodeUnlockRepository.unlock(showId, episodeIndex)
}
