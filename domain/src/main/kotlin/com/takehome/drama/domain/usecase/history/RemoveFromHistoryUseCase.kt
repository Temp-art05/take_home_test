package com.takehome.drama.domain.usecase.history

import com.takehome.drama.domain.repository.WatchProgressRepository

/**
 * "Remove from history" xoá **mọi** tập của show, không chỉ dòng mới nhất: còn tập cũ thì show lại
 * hiện trong History, và còn resume thì "Continue" vẫn nhớ chỗ — trái kỳ vọng của người vừa xoá.
 */
class RemoveFromHistoryUseCase(
    private val watchProgressRepository: WatchProgressRepository,
) {
    suspend operator fun invoke(showId: String): Result<Unit> = watchProgressRepository.removeShow(showId)
}
