package com.takehome.drama.domain.usecase.onboarding

import com.takehome.drama.domain.model.LaunchLanguages
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import kotlinx.coroutines.flow.first

/**
 * SCR-0002: 10 locale ra mắt của mockup (bộ ASO) + mã catalogue ngoài danh sách đó (nối cuối). Chọn ngôn
 * ngữ catalogue chưa có thì nội dung lùi về `defaultLanguage` ([ResolveContentLanguageUseCase]).
 */
class GetLanguageChoiceUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val resolveContentLanguage: ResolveContentLanguageUseCase,
) {
    /** Chọn sẵn: đã lưu → ngôn ngữ máy → `defaultLanguage`. Mục đang chọn ghim đầu danh sách (mockup). */
    suspend operator fun invoke(available: List<String>, defaultLanguage: String, deviceLanguage: String): LanguageChoice {
        val saved = userPreferencesRepository.preferences.first().contentLanguage
        return choose(available, defaultLanguage, deviceLanguage, saved)
    }

    fun choose(available: List<String>, defaultLanguage: String, deviceLanguage: String, saved: String?): LanguageChoice {
        // Mã catalogue thay mã ra mắt cùng subtag (giữ đúng mã để tra map title/sources).
        val launch = LaunchLanguages.codes.map { code -> resolveContentLanguage.offered(code, available) ?: code }
        val codes = launch + available.filter { resolveContentLanguage.offered(it, launch) == null }
        val selected = resolveContentLanguage.offered(saved, codes)
            ?: resolveContentLanguage.offered(deviceLanguage, codes)
            ?: resolveContentLanguage.offered(defaultLanguage, codes)
            ?: codes.first()
        val ordered = listOf(selected) + codes.filterNot { it == selected }
        return LanguageChoice(options = ordered.map { LanguageOption(it, endonym(it)) }, selected = selected)
    }

    fun endonym(code: String): String = LaunchLanguages.endonym(code)

    companion object {
        const val FALLBACK_DEFAULT_LANGUAGE = "en"
    }
}
