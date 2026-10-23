package com.takehome.drama.domain.usecase.onboarding

import com.takehome.drama.domain.repository.UserPreferencesRepository

/** Lưu gu (`TasteSelection.tagIds`) cho Home / Reels xếp show khớp gu lên trước; Skip lưu rỗng. */
class SaveTasteUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(tagIds: Set<String>): Result<Unit> = userPreferencesRepository.setTasteTagIds(tagIds)
}
