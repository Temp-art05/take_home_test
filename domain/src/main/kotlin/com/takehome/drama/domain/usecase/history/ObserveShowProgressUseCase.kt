package com.takehome.drama.domain.usecase.history

import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow

/** Tiến độ từng tập của một show — cho ô "đã xem" của lưới tập. */
class ObserveShowProgressUseCase(
    private val watchProgressRepository: WatchProgressRepository,
) {
    operator fun invoke(showId: String): Flow<List<WatchProgress>> = watchProgressRepository.observeForShow(showId)
}
