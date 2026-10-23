package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.fake.episode
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.usecase.watch.EpisodeCell
import com.takehome.drama.domain.usecase.watch.EpisodeCellState
import com.takehome.drama.domain.usecase.watch.GetEpisodeGridUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeGridTest {

    private val useCase = GetEpisodeGridUseCase()

    private fun progress(index: Int, positionMs: Long, durationMs: Long = 100_000) =
        WatchProgress("s", index, index + 1, 10, positionMs, durationMs, updatedAt = index.toLong())

    @Test
    fun `free cells without progress are unwatched, paid cells are locked`() {
        val episodes = listOf(episode(0), episode(1, isFree = false))
        assertEquals(
            listOf(EpisodeCell(0, 1, EpisodeCellState.Unwatched), EpisodeCell(1, 2, EpisodeCellState.Locked)),
            useCase(episodes, progress = emptyList(), currentIndex = null),
        )
    }

    @Test
    fun `completed or partly watched episodes are watched, a skim under 5s is not`() {
        val episodes = listOf(episode(0), episode(1), episode(2), episode(3))
        val cells = useCase(
            episodes,
            progress = listOf(
                progress(0, positionMs = 99_000),
                progress(1, positionMs = 40_000),
                progress(2, positionMs = 5_000),
                progress(3, positionMs = 4_999),
            ),
            currentIndex = null,
        )
        assertEquals(
            listOf(EpisodeCellState.Watched, EpisodeCellState.Watched, EpisodeCellState.Watched, EpisodeCellState.Unwatched),
            cells.map { it.state },
        )
    }

    @Test
    fun `progress saved before the duration is known still counts once past 5s`() {
        val cells = useCase(listOf(episode(0)), listOf(progress(0, positionMs = 6_000, durationMs = 0)), null)
        assertEquals(EpisodeCellState.Watched, cells[0].state)
    }

    @Test
    fun `playing wins over watched and locked`() {
        val episodes = listOf(episode(0), episode(1, isFree = false))
        assertEquals(
            EpisodeCellState.Playing,
            useCase(episodes, listOf(progress(0, 99_000)), currentIndex = 0)[0].state,
        )
        assertEquals(EpisodeCellState.Playing, useCase(episodes, emptyList(), currentIndex = 1)[1].state)
    }

    @Test
    fun `locked wins over watched`() {
        val episodes = listOf(episode(0, isFree = false))
        assertEquals(EpisodeCellState.Locked, useCase(episodes, listOf(progress(0, 99_000)), null)[0].state)
    }

    @Test
    fun `cells keep the episode index and display number when the array has holes`() {
        val episodes = listOf(episode(0), episode(3, number = 7), episode(5, number = null))
        val cells = useCase(episodes, listOf(progress(3, 99_000)), currentIndex = 5)
        assertEquals(listOf(0 to 1, 3 to 7, 5 to 6), cells.map { it.index to it.label })
        assertEquals(
            listOf(EpisodeCellState.Unwatched, EpisodeCellState.Watched, EpisodeCellState.Playing),
            cells.map { it.state },
        )
    }

    @Test
    fun `watched count matches the watched cells and ignores episodes no longer in the file`() {
        val episodes = listOf(episode(0), episode(1), episode(2))
        val progress = listOf(progress(0, 99_000), progress(1, 10_000), progress(2, 1_000), progress(9, 99_000))
        assertEquals(2, useCase.watchedCount(episodes, progress))
    }
}
