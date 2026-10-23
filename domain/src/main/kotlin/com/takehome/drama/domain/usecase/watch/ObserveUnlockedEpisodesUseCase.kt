package com.takehome.drama.domain.usecase.watch

import com.takehome.drama.domain.repository.EpisodeUnlockRepository
import kotlinx.coroutines.flow.Flow

class ObserveUnlockedEpisodesUseCase(
    private val episodeUnlockRepository: EpisodeUnlockRepository,
) {
    operator fun invoke(showId: String): Flow<Set<Int>> = episodeUnlockRepository.observeUnlocked(showId)
}
