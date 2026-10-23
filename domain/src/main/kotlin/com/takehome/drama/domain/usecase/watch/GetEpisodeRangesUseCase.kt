package com.takehome.drama.domain.usecase.watch

import com.takehome.drama.domain.model.Episode

class GetEpisodeRangesUseCase {

    /** Chia theo vị trí trong list (index có thể thủng lỗ), nhãn theo số hiển thị (`number` có thể nhảy cóc). */
    operator fun invoke(episodes: List<Episode>, size: Int = RANGE_SIZE): List<EpisodeRange> =
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

    private companion object {
        const val RANGE_SIZE = 20
    }
}
