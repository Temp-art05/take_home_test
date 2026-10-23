package com.takehome.drama.domain.usecase.watch

import com.takehome.drama.domain.model.Episode

/** Việc làm khi một tập phát hết. */
class GetEndActionUseCase {

    /**
     * Tự sang tập kế chỉ khi bật autoplay **và** tập kế xem được (free hoặc đã mở, [unlocked]) —
     * không bao giờ tự phát vào tập khoá.
     */
    operator fun invoke(episodes: List<Episode>, currentIndex: Int, autoplay: Boolean, unlocked: Set<Int> = emptySet()): EndAction {
        val next = nextOf(episodes, currentIndex) ?: return EndAction.ShowFinished
        return when {
            !next.isWatchable(unlocked) -> EndAction.NextLocked(next.index, next.displayNumber)
            autoplay -> EndAction.PlayNext(next.index)
            else -> EndAction.NextAvailable(next.index, next.displayNumber)
        }
    }

    /** Phần tử kế trong list — không phải `index + 1`. */
    fun nextOf(episodes: List<Episode>, currentIndex: Int): Episode? {
        val position = episodes.indexOfFirst { it.index == currentIndex }
        return if (position < 0) null else episodes.getOrNull(position + 1)
    }
}
