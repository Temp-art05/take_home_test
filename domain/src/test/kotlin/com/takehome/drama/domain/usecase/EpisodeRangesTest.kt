package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.episode
import com.takehome.drama.domain.usecase.watch.EpisodeRange
import com.takehome.drama.domain.usecase.watch.EpisodesUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeRangesTest {

    private val useCase = EpisodesUseCase(FakeCatalogRepository(Result.failure(IllegalStateException())))

    @Test
    fun `68 episodes split into chunks of 20, last one short`() {
        val ranges = useCase.ranges(List(68) { episode(it) })
        assertEquals(listOf(1 to 20, 21 to 40, 41 to 60, 61 to 68), ranges.map { it.firstLabel to it.lastLabel })
        assertEquals(EpisodeRange(start = 60, endExclusive = 68, firstLabel = 61, lastLabel = 68), ranges.last())
    }

    @Test
    fun `exactly one chunk when the show has 20 episodes`() {
        assertEquals(1, useCase.ranges(List(20) { episode(it) }).size)
        assertEquals(listOf(1 to 20, 21 to 40, 41 to 44), useCase.ranges(List(44) { episode(it) }).map { it.firstLabel to it.lastLabel })
    }

    @Test
    fun `no episodes means no ranges`() {
        assertEquals(emptyList<EpisodeRange>(), useCase.ranges(emptyList()))
    }

    @Test
    fun `labels come from display numbers, chunks from list positions`() {
        // number nhảy cóc: 1, 2, 10, 11 — chia theo vị trí, nhãn theo số hiển thị.
        val episodes = listOf(episode(0, number = 1), episode(1, number = 2), episode(2, number = 10), episode(4, number = 11))
        assertEquals(
            listOf(EpisodeRange(0, 2, 1, 2), EpisodeRange(2, 4, 10, 11)),
            useCase.ranges(episodes, size = 2),
        )
    }

    @Test
    fun `range containing an episode index is found by list position`() {
        val episodes = List(44) { episode(it) }
        val ranges = useCase.ranges(episodes)
        assertEquals(ranges[1], useCase.rangeOf(ranges, episodes, episodeIndex = 25))
        assertEquals(ranges[0], useCase.rangeOf(ranges, episodes, episodeIndex = 999))
    }
}
