package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.fake.episode
import com.takehome.drama.domain.usecase.watch.EndAction
import com.takehome.drama.domain.usecase.watch.GetEndActionUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AfterEndedTest {

    private val useCase = GetEndActionUseCase()

    private val episodes = listOf(episode(0), episode(1), episode(2, isFree = false))

    @Test
    fun `autoplay on and next is free - play it`() {
        assertEquals(EndAction.PlayNext(index = 1), useCase(episodes, currentIndex = 0, autoplay = true))
    }

    @Test
    fun `next is locked - never auto play into it`() {
        assertEquals(
            EndAction.NextLocked(index = 2, episodeNumber = 3),
            useCase(episodes, currentIndex = 1, autoplay = true),
        )
    }

    @Test
    fun `autoplay off - offer the next episode instead of playing it`() {
        assertEquals(
            EndAction.NextAvailable(index = 1, episodeNumber = 2),
            useCase(episodes, currentIndex = 0, autoplay = false),
        )
    }

    @Test
    fun `last episode finishes the show`() {
        assertEquals(EndAction.ShowFinished, useCase(episodes, currentIndex = 2, autoplay = true))
    }

    @Test
    fun `next episode is the next element even when indexes have holes`() {
        val holes = listOf(episode(0), episode(3), episode(7))
        assertEquals(EndAction.PlayNext(index = 3), useCase(holes, currentIndex = 0, autoplay = true))
        assertEquals(3, useCase.nextOf(holes, currentIndex = 0)?.index)
        assertNull(useCase.nextOf(holes, currentIndex = 7))
    }

    @Test
    fun `next locked episode already unlocked plays like a free one`() {
        assertEquals(PlayNextIndex2, useCase(episodes, currentIndex = 1, autoplay = true, unlocked = setOf(2)))
        assertEquals(
            EndAction.NextAvailable(index = 2, episodeNumber = 3),
            useCase(episodes, currentIndex = 1, autoplay = false, unlocked = setOf(2)),
        )
    }

    @Test
    fun `unlocking another episode does not open the next one`() {
        assertEquals(
            EndAction.NextLocked(index = 2, episodeNumber = 3),
            useCase(episodes, currentIndex = 1, autoplay = true, unlocked = setOf(5)),
        )
    }

    private companion object {
        val PlayNextIndex2 = EndAction.PlayNext(index = 2)
    }
}
