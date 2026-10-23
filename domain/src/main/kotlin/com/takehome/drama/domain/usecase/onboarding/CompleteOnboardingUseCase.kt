package com.takehome.drama.domain.usecase.onboarding

import com.takehome.drama.domain.repository.UserPreferencesRepository

/** Mở lại app sẽ vào thẳng Home. Gọi ở bước cuối (SCR-0005, hoặc SCR-0004 khi không cần pre-prompt). */
class CompleteOnboardingUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(): Result<Unit> = userPreferencesRepository.setOnboardingDone(true)
}
