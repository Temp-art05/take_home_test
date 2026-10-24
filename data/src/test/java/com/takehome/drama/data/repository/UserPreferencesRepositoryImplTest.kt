package com.takehome.drama.data.repository

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.takehome.drama.data.local.datasource.UserPreferencesKeys
import com.takehome.drama.data.local.datasource.UserPreferencesLocalDataSource
import com.takehome.drama.domain.error.DomainException
import com.takehome.drama.domain.model.ThemeMode
import com.takehome.drama.domain.model.UserPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPreferencesRepositoryImplTest {

    private class FakeLocal(
        override val data: Flow<Preferences>,
        private val writeError: Throwable? = null,
    ) : UserPreferencesLocalDataSource {
        val written = mutableMapOf<Preferences.Key<*>, Any?>()

        override suspend fun <T> set(key: Preferences.Key<T>, value: T) {
            writeError?.let { throw it }
            written[key] = value
        }

        override suspend fun <T> remove(key: Preferences.Key<T>) {
            writeError?.let { throw it }
            written.remove(key)
        }
    }

    @Test
    fun `unreadable file falls back to defaults`() = runTest {
        val repository = UserPreferencesRepositoryImpl(FakeLocal(flow { throw IOException("corrupt") }))
        assertEquals(UserPreferences(), repository.preferences.first())
    }

    @Test
    fun `unknown theme value falls back to Dark`() = runTest {
        val stored = mutablePreferencesOf(UserPreferencesKeys.THEME to "Sepia")
        assertEquals(ThemeMode.Dark, UserPreferencesRepositoryImpl(FakeLocal(flowOf(stored))).preferences.first().theme)
    }

    @Test
    fun `failed write is a Result failure, not a crash - and not reported as a network error`() = runTest {
        val repository = UserPreferencesRepositoryImpl(FakeLocal(flowOf(mutablePreferencesOf()), IOException("disk full")))
        val error = repository.setOnboardingDone(true).exceptionOrNull()
        assertTrue(error is DomainException.Unknown)
    }

    @Test
    fun `autoplay next episode defaults to on and reads the stored value`() = runTest {
        assertTrue(UserPreferencesRepositoryImpl(FakeLocal(flowOf(mutablePreferencesOf()))).preferences.first().autoplayNextEpisode)
        val stored = mutablePreferencesOf(UserPreferencesKeys.AUTOPLAY_NEXT_EPISODE to false)
        assertFalse(UserPreferencesRepositoryImpl(FakeLocal(flowOf(stored))).preferences.first().autoplayNextEpisode)
    }

    @Test
    fun `setting autoplay next episode writes its key`() = runTest {
        val local = FakeLocal(flowOf(mutablePreferencesOf()))
        UserPreferencesRepositoryImpl(local).setAutoplayNextEpisode(false)
        assertEquals(mapOf<Preferences.Key<*>, Any?>(UserPreferencesKeys.AUTOPLAY_NEXT_EPISODE as Preferences.Key<*> to false), local.written)
    }
}
