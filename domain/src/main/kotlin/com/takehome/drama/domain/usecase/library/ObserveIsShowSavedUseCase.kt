package com.takehome.drama.domain.usecase.library

import com.takehome.drama.domain.repository.SavedShowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveIsShowSavedUseCase(
    private val savedShowRepository: SavedShowRepository,
) {
    operator fun invoke(showId: String): Flow<Boolean> = savedShowRepository.observeIsSaved(showId)

    /** Id mọi show đã lưu — cho màn hiện nhiều show (Reels). */
    fun savedIds(): Flow<Set<String>> = savedShowRepository.observeAll().map { saved -> saved.mapTo(HashSet()) { it.showId } }
}
