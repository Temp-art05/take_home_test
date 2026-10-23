package com.takehome.drama.domain.usecase.catalog

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.languageSubtag

/**
 * Ngôn ngữ nội dung: đã chọn → bản catalogue có, không có (SCR-0002 liệt kê 10 locale ra mắt, catalogue
 * chỉ có 5) thì `defaultLanguage` — không lấy ngôn ngữ máy vì user đã chọn rõ. Chưa chọn → ngôn ngữ máy
 * (nếu có) → `defaultLanguage`. Khớp theo language subtag (`en-US` → `en`).
 */
class ResolveContentLanguageUseCase {
    operator fun invoke(saved: String?, deviceLanguage: String, catalog: Catalog): String =
        if (saved != null) {
            offered(saved, catalog.availableLanguages) ?: catalog.defaultLanguage
        } else {
            offered(deviceLanguage, catalog.availableLanguages) ?: catalog.defaultLanguage
        }

    /** Mã trong [available] cùng subtag với [language]; không có → null. */
    fun offered(language: String?, available: List<String>): String? =
        language?.languageSubtag()?.let { base -> available.firstOrNull { it.languageSubtag() == base } }
}
