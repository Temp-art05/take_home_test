package com.takehome.drama.domain.usecase.onboarding

import com.takehome.drama.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class IsOnboardingDoneUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(): Boolean = userPreferencesRepository.preferences.first().onboardingDone
}
