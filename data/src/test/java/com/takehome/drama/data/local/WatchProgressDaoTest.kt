package com.takehome.drama.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.takehome.drama.data.local.database.AppDatabase
import com.takehome.drama.data.local.entity.WatchProgressEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WatchProgressDaoTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dao = db.watchProgressDao()

    @After
    fun tearDown() = db.close()

    private fun row(showId: String, index: Int, position: Long, updatedAt: Long) =
        WatchProgressEntity(showId, index, index + 1, 10, position, 100_000, updatedAt)

    @Test
    fun `upsert overwrites the same show and episode`() = runTest {
        dao.upsert(row("a", 0, position = 1_000, updatedAt = 1))
        dao.upsert(row("a", 0, position = 9_000, updatedAt = 2))
        assertEquals(9_000L, dao.get("a", 0)?.positionMs)
    }

    @Test
    fun `latest per show returns one row per show, newest first`() = runTest {
        dao.upsert(row("a", 0, 1, updatedAt = 10))
        dao.upsert(row("a", 1, 1, updatedAt = 30))
        dao.upsert(row("b", 4, 1, updatedAt = 20))
        dao.upsert(row("b", 2, 1, updatedAt = 5))

        dao.observeLatestPerShow().test {
            assertEquals(listOf("a" to 1, "b" to 4), awaitItem().map { it.showId to it.episodeIndex })
        }
        assertEquals(1, dao.latestForShow("a")?.episodeIndex)
    }

    @Test
    fun `two episodes with the same updatedAt still give one row for the show`() = runTest {
        dao.upsert(row("a", 0, 1, updatedAt = 10))
        dao.upsert(row("a", 1, 1, updatedAt = 10))

        dao.observeLatestPerShow().test {
            assertEquals(listOf("a"), awaitItem().map { it.showId })
        }
    }

    @Test
    fun `progress of one show only, sorted by episode index`() = runTest {
        dao.upsert(row("a", 3, 1, updatedAt = 1))
        dao.upsert(row("b", 0, 1, updatedAt = 2))
        dao.upsert(row("a", 1, 1, updatedAt = 3))

        dao.observeForShow("a").test {
            assertEquals(listOf(1, 3), awaitItem().map { it.episodeIndex })
        }
    }
}
