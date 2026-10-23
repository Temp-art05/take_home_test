package com.takehome.drama.domain.usecase.reels

import com.takehome.drama.domain.model.Episode
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import com.takehome.drama.domain.usecase.watch.PickVideoSourceUseCase

/** Một reel đã biết phát gì: tập free đầu tiên có URL của show. */
data class ReelClip(
    val episode: Episode,
    val url: String,
    /** Số tập của **file tập** cho "Watch all N episodes" — không phải `episodeCount` của catalogue. */
    val episodeTotal: Int,
)

class GetReelClipUseCase(
    private val getShowEpisodes: GetShowEpisodesUseCase,
    private val pickVideoSource: PickVideoSourceUseCase,
) {
    /** Tải file tập rồi chọn clip. Thành công với null = show không có tập free phát được (bỏ khỏi feed). */
    suspend operator fun invoke(showId: String, language: String, defaultLanguage: String): Result<ReelClip?> =
        getShowEpisodes(showId).map { clipOf(it, language, defaultLanguage) }

    /**
     * Tập free đầu tiên **theo thứ tự mảng** (không theo `number`) mà có URL phát được trong ngôn ngữ
     * xem → `defaultLanguage`.
     */
    fun clipOf(episodes: ShowEpisodes, language: String, defaultLanguage: String): ReelClip? {
        episodes.episodes.forEach { episode ->
            if (!episode.isFree) return@forEach
            val source = pickVideoSource(episode, language, defaultLanguage) ?: return@forEach
            return ReelClip(episode = episode, url = source.url, episodeTotal = episodes.episodes.size)
        }
        return null
    }
}
