package com.takehome.drama.domain.usecase.history

import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.repository.WatchProgressRepository

class SaveWatchProgressUseCase(
    private val watchProgressRepository: WatchProgressRepository,
) {
    suspend operator fun invoke(progress: WatchProgress): Result<Unit> = watchProgressRepository.save(progress)
}
