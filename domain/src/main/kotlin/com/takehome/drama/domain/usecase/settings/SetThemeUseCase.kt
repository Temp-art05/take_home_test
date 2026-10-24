package com.takehome.drama.domain.usecase.settings

import com.takehome.drama.domain.model.ThemeMode
import com.takehome.drama.domain.repository.UserPreferencesRepository

class SetThemeUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(theme: ThemeMode): Result<Unit> = userPreferencesRepository.setTheme(theme)
}
