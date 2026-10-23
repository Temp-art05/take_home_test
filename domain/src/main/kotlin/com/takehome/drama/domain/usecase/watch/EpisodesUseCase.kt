package com.takehome.drama.domain.usecase.watch

import com.takehome.drama.domain.model.Episode
import com.takehome.drama.domain.model.Quality
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.VideoSource
import com.takehome.drama.domain.model.languageSubtag
import com.takehome.drama.domain.repository.CatalogRepository

class EpisodesUseCase(
    private val catalogRepository: CatalogRepository,
) {
    suspend fun load(showId: String, forceRefresh: Boolean = false): Result<ShowEpisodes> =
        catalogRepository.getEpisodes(showId, forceRefresh)

    /**
     * Số tập hiển thị. Lưới tin file tập (`13077`: catalogue ghi 65, file có 50 — ô nào cũng phải
     * phát được); `episodeCount` của catalogue chỉ dùng khi chưa tải tập (card, Reels).
     */
    fun episodeTotal(show: Show, episodes: ShowEpisodes?): Int =
        episodes?.episodes?.size ?: show.catalogEpisodeCount

    /**
     * URL để phát: ngôn ngữ xem → [defaultLanguage] → ngôn ngữ bất kỳ có source. Trong ngôn ngữ
     * đó: [preferredQuality] nếu có, không thì chất lượng cao nhất. Không có URL nào → null.
     */
    fun pickSource(
        episode: Episode,
        language: String,
        defaultLanguage: String,
        preferredQuality: String? = null,
    ): VideoSource? {
        val sourceLanguage = sourceLanguageOf(episode, language, defaultLanguage) ?: return null
        val renditions = episode.sources.getValue(sourceLanguage)
        val quality = preferredQuality?.takeIf { it in renditions }
            ?: renditions.keys.sortedWith(Quality.HighestFirst).first()
        return VideoSource(sourceLanguage, quality, renditions.getValue(quality))
    }

    /** Chất lượng có thật trong `sources` của ngôn ngữ sẽ phát — không bịa 1080p. */
    fun qualitiesOf(episode: Episode, language: String, defaultLanguage: String): List<String> {
        val sourceLanguage = sourceLanguageOf(episode, language, defaultLanguage) ?: return emptyList()
        return episode.sources.getValue(sourceLanguage).keys.sortedWith(Quality.HighestFirst)
    }

    private fun sourceLanguageOf(episode: Episode, language: String, defaultLanguage: String): String? {
        val playable = episode.sources.filterValues { it.isNotEmpty() }.keys
        // Khớp theo subtag như LocalizedText: "en-US" phải ra key "en".
        fun offered(wanted: String): String? =
            wanted.takeIf { it in playable } ?: playable.firstOrNull { it.languageSubtag() == wanted.languageSubtag() }
        return offered(language) ?: offered(defaultLanguage) ?: playable.firstOrNull()
    }
}
