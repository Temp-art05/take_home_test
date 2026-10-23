package com.takehome.drama.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.takehome.drama.data.local.datasource.EpisodeUnlockLocalDataSourceImpl
import com.takehome.drama.data.repository.EpisodeUnlockRepositoryImpl
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EpisodeUnlockLocalDataSourceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Mỗi lần gọi = một "process" mới đọc cùng file (giả lập kill app rồi mở lại). */
    private fun <T> withRepository(file: File, block: suspend (EpisodeUnlockRepositoryImpl) -> T): T {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return try {
            val store = PreferenceDataStoreFactory.create(scope = scope) { file }
            runBlocking { block(EpisodeUnlockRepositoryImpl(EpisodeUnlockLocalDataSourceImpl(store))) }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `unlocked episodes survive a restart`() {
        val file = File(tmp.root, "episode_unlocks.preferences_pb")
        withRepository(file) { repo ->
            repo.unlock("12692", 5)
            repo.unlock("12692", 9)
            repo.unlock("13077", 5)
        }
        withRepository(file) { repo ->
            assertEquals(setOf(5, 9), repo.observeUnlocked("12692").first())
            assertEquals(setOf(5), repo.observeUnlocked("13077").first())
            assertEquals(emptySet<Int>(), repo.observeUnlocked("12485").first())
        }
    }

    @Test
    fun `unlocking the same episode twice keeps one entry`() {
        val file = File(tmp.root, "twice.preferences_pb")
        withRepository(file) { repo ->
            repo.unlock("s", 5)
            repo.unlock("s", 5)
            assertEquals(setOf(5), repo.observeUnlocked("s").first())
        }
    }

    @Test
    fun `show id that is a prefix of another does not leak`() {
        val file = File(tmp.root, "prefix.preferences_pb")
        withRepository(file) { repo ->
            repo.unlock("1", 3)
            repo.unlock("12", 4)
            assertEquals(setOf(3), repo.observeUnlocked("1").first())
            assertEquals(setOf(4), repo.observeUnlocked("12").first())
        }
    }
}
