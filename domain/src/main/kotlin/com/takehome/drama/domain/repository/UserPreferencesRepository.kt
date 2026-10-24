package com.takehome.drama.domain.repository

import com.takehome.drama.domain.model.ThemeMode
import com.takehome.drama.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    /** `null` = theo ngôn ngữ máy (xoá lựa chọn). */
    suspend fun setContentLanguage(language: String?): Result<Unit>

    suspend fun setTasteTagIds(tagIds: Set<String>): Result<Unit>

    suspend fun setTheme(theme: ThemeMode): Result<Unit>

    suspend fun setOnboardingDone(done: Boolean): Result<Unit>

    suspend fun setAutoplayPreviewWifiOnly(wifiOnly: Boolean): Result<Unit>

    suspend fun setAutoplayNextEpisode(enabled: Boolean): Result<Unit>
}
