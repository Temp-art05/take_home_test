package com.takehome.drama.domain.usecase.watch

import com.takehome.drama.domain.model.Episode
import com.takehome.drama.domain.model.WatchProgress

class GetEpisodeGridUseCase {

    /**
     * Lưới tập. Ưu tiên **Playing > Locked > Watched > Unwatched**: tập đang phát luôn phải nổi;
     * khoá thắng "đã xem" vì khoá là thứ user cần biết trước khi chạm. "Đã xem" =
     * [WatchProgress.isWatched] (xong hoặc đã xem ≥ 5s) — tập chỉ lướt qua vẫn trắng.
     */
    operator fun invoke(episodes: List<Episode>, progress: List<WatchProgress>, currentIndex: Int?): List<EpisodeCell> {
        val watched = progress.filter { it.isWatched }.mapTo(HashSet()) { it.episodeIndex }
        return episodes.map { episode ->
            val state = when {
                episode.index == currentIndex -> EpisodeCellState.Playing
                !episode.isFree -> EpisodeCellState.Locked
                episode.index in watched -> EpisodeCellState.Watched
                else -> EpisodeCellState.Unwatched
            }
            EpisodeCell(episode.index, episode.displayNumber, state)
        }
    }

    /** Số ô "đã xem" ([WatchProgress.isWatched]) — bản ghi của tập không còn trong file không tính. */
    fun watchedCount(episodes: List<Episode>, progress: List<WatchProgress>): Int {
        val indexes = episodes.mapTo(HashSet()) { it.index }
        return progress.count { it.isWatched && it.episodeIndex in indexes }
    }
}
