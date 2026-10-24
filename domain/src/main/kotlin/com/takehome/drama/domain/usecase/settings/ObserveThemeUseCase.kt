package com.takehome.drama.domain.usecase.settings

import com.takehome.drama.domain.model.ThemeMode
import com.takehome.drama.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Chỉ phát khi theme đổi — ghi preference khác (ngôn ngữ, taste) không được làm Activity vẽ lại theme. */
class ObserveThemeUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<ThemeMode> = userPreferencesRepository.preferences.map { it.theme }.distinctUntilChanged()
}
