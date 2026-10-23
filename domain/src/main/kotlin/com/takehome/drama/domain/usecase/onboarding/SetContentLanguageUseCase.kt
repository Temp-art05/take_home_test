package com.takehome.drama.domain.usecase.onboarding

import com.takehome.drama.domain.repository.UserPreferencesRepository

/** Ngôn ngữ nội dung (title/audio) của cả app — chọn ở SCR-0002, đổi lại ở Player 0304. */
class SetContentLanguageUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(code: String): Result<Unit> = userPreferencesRepository.setContentLanguage(code)
}
