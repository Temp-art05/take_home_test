package com.takehome.drama.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.takehome.drama.data.local.database.AppDatabase
import com.takehome.drama.data.local.entity.SavedShowEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SavedShowDaoTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dao = db.savedShowDao()

    @After
    fun tearDown() = db.close()

    @Test
    fun `saving again keeps the original savedAt so Saved order does not jump`() = runTest {
        dao.insert(SavedShowEntity("a", savedAt = 1))
        dao.insert(SavedShowEntity("b", savedAt = 2))
        dao.insert(SavedShowEntity("a", savedAt = 3))

        dao.observeAll().test {
            assertEquals(listOf("b" to 2L, "a" to 1L), awaitItem().map { it.showId to it.savedAt })
        }
    }
}
