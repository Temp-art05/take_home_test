package com.takehome.drama.data.local.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// File riêng (không chung `user_prefs`): thêm phần tử phải đọc-rồi-ghi trong một `edit`, còn
// UserPreferencesLocalDataSource chỉ có `set` ghi đè.
val Context.episodeUnlockDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "episode_unlocks",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

interface EpisodeUnlockLocalDataSource {
    /** Phần tử dạng `"<showId>#<episodeIndex>"`. */
    val entries: Flow<Set<String>>

    suspend fun add(entry: String)
}

class EpisodeUnlockLocalDataSourceImpl(
    private val dataStore: DataStore<Preferences>,
) : EpisodeUnlockLocalDataSource {

    override val entries: Flow<Set<String>> = dataStore.data.map { it[KEY].orEmpty() }

    override suspend fun add(entry: String) {
        dataStore.edit { it[KEY] = it[KEY].orEmpty() + entry }
    }

    private companion object {
        val KEY = stringSetPreferencesKey("unlocked_episodes")
    }
}
