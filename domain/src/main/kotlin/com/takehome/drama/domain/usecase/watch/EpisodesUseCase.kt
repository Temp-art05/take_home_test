package com.takehome.drama.domain.usecase.watch

import com.takehome.drama.domain.model.Episode
import com.takehome.drama.domain.model.Quality
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.VideoSource
import com.takehome.drama.domain.model.WatchProgress
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

    /**
     * Lưới tập. Ưu tiên **Playing > Locked > Watched > Unwatched**: tập đang phát luôn phải nổi;
     * khoá thắng "đã xem" vì khoá là thứ user cần biết trước khi chạm. "Đã xem" =
     * [WatchProgress.isWatched] (xong hoặc đã xem ≥ 5s) — tập chỉ lướt qua vẫn trắng.
     */
    fun grid(episodes: List<Episode>, progress: List<WatchProgress>, currentIndex: Int?): List<EpisodeCell> {
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

    /** Chia theo vị trí trong list (index có thể thủng lỗ), nhãn theo số hiển thị (`number` có thể nhảy cóc). */
    fun ranges(episodes: List<Episode>, size: Int = RANGE_SIZE): List<EpisodeRange> =
        episodes.indices.chunked(size).map { positions ->
            EpisodeRange(
                start = positions.first(),
                endExclusive = positions.last() + 1,
                firstLabel = episodes[positions.first()].displayNumber,
                lastLabel = episodes[positions.last()].displayNumber,
            )
        }

    /** Chunk chứa tập [episodeIndex]; không thấy tập thì chunk đầu. */
    fun rangeOf(ranges: List<EpisodeRange>, episodes: List<Episode>, episodeIndex: Int): EpisodeRange? {
        val position = episodes.indexOfFirst { it.index == episodeIndex }
        return ranges.firstOrNull { position in it.start until it.endExclusive } ?: ranges.firstOrNull()
    }

    /** Phần tử kế trong list — không phải `index + 1`. */
    fun nextOf(episodes: List<Episode>, currentIndex: Int): Episode? {
        val position = episodes.indexOfFirst { it.index == currentIndex }
        return if (position < 0) null else episodes.getOrNull(position + 1)
    }

    /** Tự sang tập kế chỉ khi bật autoplay **và** tập kế free — không bao giờ tự phát vào tập khoá. */
    fun afterEnded(episodes: List<Episode>, currentIndex: Int, autoplay: Boolean): EndAction {
        val next = nextOf(episodes, currentIndex) ?: return EndAction.ShowFinished
        return when {
            !next.isFree -> EndAction.NextLocked(next.index, next.displayNumber)
            autoplay -> EndAction.PlayNext(next.index)
            else -> EndAction.NextAvailable(next.index, next.displayNumber)
        }
    }

    private fun sourceLanguageOf(episode: Episode, language: String, defaultLanguage: String): String? {
        val playable = episode.sources.filterValues { it.isNotEmpty() }.keys
        // Khớp theo subtag như LocalizedText: "en-US" phải ra key "en".
        fun offered(wanted: String): String? =
            wanted.takeIf { it in playable } ?: playable.firstOrNull { it.languageSubtag() == wanted.languageSubtag() }
        return offered(language) ?: offered(defaultLanguage) ?: playable.firstOrNull()
    }

    private companion object {
        const val RANGE_SIZE = 20
    }
}
