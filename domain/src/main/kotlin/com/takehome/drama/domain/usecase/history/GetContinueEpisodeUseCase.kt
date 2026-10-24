package com.takehome.drama.domain.usecase.history

import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase

/**
 * Tập mở khi bấm một dòng "đang xem" (Library Recent, History Watching). Bản ghi mới nhất có thể là tập
 * đã xem hết — mở thẳng nó thì Player phát lại từ 0 — nên tải file tập (repo cache) để sang tập kế.
 * Lỗi tải → tập đã lưu: mở được còn hơn chặn.
 */
class GetContinueEpisodeUseCase(
    private val getShowEpisodes: GetShowEpisodesUseCase,
    private val getContinueTarget: GetContinueTargetUseCase,
) {
    suspend operator fun invoke(showId: String, savedEpisodeIndex: Int): Int =
        getShowEpisodes(showId).getOrNull()
            ?.let { getContinueTarget(showId, it.episodes).episodeIndex }
            ?: savedEpisodeIndex
}
