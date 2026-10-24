package com.takehome.drama.domain.usecase.onboarding

import com.takehome.drama.domain.repository.UserPreferencesRepository

/**
 * Ngôn ngữ nội dung (title/audio) của cả app — chọn ở SCR-0002, đổi ở Settings 0502 (cùng ngôn ngữ UI) và
 * Player 0304. `null` = theo ngôn ngữ máy.
 */
class SetContentLanguageUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(code: String?): Result<Unit> = userPreferencesRepository.setContentLanguage(code)
}
