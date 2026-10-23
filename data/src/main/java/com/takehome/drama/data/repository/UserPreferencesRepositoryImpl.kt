package com.takehome.drama.data.repository

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.takehome.drama.data.local.datasource.UserPreferencesKeys
import com.takehome.drama.data.local.datasource.UserPreferencesLocalDataSource
import com.takehome.drama.data.util.withLocalCatching
import com.takehome.drama.domain.model.ThemeMode
import com.takehome.drama.domain.model.UserPreferences
import com.takehome.drama.domain.repository.UserPreferencesRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

class UserPreferencesRepositoryImpl(
    private val local: UserPreferencesLocalDataSource,
) : UserPreferencesRepository {

    // File DataStore hỏng/không đọc được → dùng mặc định thay vì làm chết Splash.
    override val preferences: Flow<UserPreferences> = local.data
        .catch { error ->
            if (error !is IOException) throw error
            Timber.e(error, "Read user preferences failed")
            emit(emptyPreferences())
        }
        .map { it.toDomain() }

    override suspend fun setContentLanguage(language: String) = write(UserPreferencesKeys.CONTENT_LANGUAGE, language)

    override suspend fun setTasteTagIds(tagIds: Set<String>) = write(UserPreferencesKeys.TASTE_TAG_IDS, tagIds)

    override suspend fun setTheme(theme: ThemeMode) = write(UserPreferencesKeys.THEME, theme.name)

    override suspend fun setOnboardingDone(done: Boolean) = write(UserPreferencesKeys.ONBOARDING_DONE, done)

    override suspend fun setAutoplayPreviewWifiOnly(wifiOnly: Boolean) =
        write(UserPreferencesKeys.AUTOPLAY_PREVIEW_WIFI_ONLY, wifiOnly)

    override suspend fun setAutoplayNextEpisode(enabled: Boolean) =
        write(UserPreferencesKeys.AUTOPLAY_NEXT_EPISODE, enabled)

    private suspend fun <T> write(key: Preferences.Key<T>, value: T): Result<Unit> =
        withLocalCatching { local.set(key, value) }

    private fun Preferences.toDomain(): UserPreferences {
        val defaults = UserPreferences()
        return UserPreferences(
            contentLanguage = this[UserPreferencesKeys.CONTENT_LANGUAGE],
            tasteTagIds = this[UserPreferencesKeys.TASTE_TAG_IDS] ?: defaults.tasteTagIds,
            theme = ThemeMode.entries.firstOrNull { it.name == this[UserPreferencesKeys.THEME] } ?: defaults.theme,
            onboardingDone = this[UserPreferencesKeys.ONBOARDING_DONE] ?: defaults.onboardingDone,
            autoplayPreviewWifiOnly = this[UserPreferencesKeys.AUTOPLAY_PREVIEW_WIFI_ONLY] ?: defaults.autoplayPreviewWifiOnly,
            autoplayNextEpisode = this[UserPreferencesKeys.AUTOPLAY_NEXT_EPISODE] ?: defaults.autoplayNextEpisode,
        )
    }
}
