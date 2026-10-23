package com.takehome.drama.domain.model

import java.util.Locale

/**
 * 10 locale ra mắt (mockup SCR-0002, bộ ASO) — vừa là danh sách ngôn ngữ nội dung ở onboarding, vừa là
 * ngôn ngữ UI app có bản dịch (`AppLocale`). Thứ tự = thứ tự mockup.
 */
object LaunchLanguages {
    val codes: List<String> = listOf("en", "hi", "ta", "id", "ar", "fr", "es", "pt-BR", "tr", "de")

    /** Endonym cố định theo mockup (locale máy có thể không có tên chuẩn); mã lạ hỏi `Locale`. */
    fun endonym(code: String): String = ENDONYMS[code] ?: ENDONYMS[code.languageSubtag()]
        ?: Locale.forLanguageTag(code).let { locale ->
            locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }.ifBlank { code }
        }

    private val ENDONYMS = mapOf(
        "en" to "English (U.S.)",
        "hi" to "हिन्दी",
        "ta" to "தமிழ்",
        "id" to "Bahasa Indonesia",
        "ar" to "العربية",
        "fr" to "Français",
        "es" to "Español",
        "pt-BR" to "Português (BR)",
        "tr" to "Türkçe",
        "de" to "Deutsch",
    )
}
