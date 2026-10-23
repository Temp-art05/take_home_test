package com.takehome.drama.domain.model

/**
 * Text đa ngôn ngữ của catalogue (`title`, `synopsis`, `hook`, tên tag): key là mã ngôn ngữ.
 * Không phải entry nào cũng đủ ngôn ngữ, nhiều entry chỉ có `en`.
 */
@JvmInline
value class LocalizedText(val values: Map<String, String>) {

    /**
     * Ngôn ngữ người dùng → [fallback] (`defaultLanguage` của catalogue) → giá trị đầu tiên có.
     * Hiện tiếng khác còn hơn ô trống: show vẫn nhận ra được, và `defaultLanguage` là bản server
     * điền đủ nhất. Chuỗi rỗng coi như thiếu — server để `""` ở field chưa điền.
     */
    fun resolve(preferred: String, fallback: String): String =
        find(preferred)
            ?: find(fallback)
            ?: values.values.firstOrNull { it.isNotBlank() }
            ?: ""

    val isEmpty: Boolean get() = values.values.all { it.isBlank() }

    /** Khớp chính xác trước, rồi theo language subtag: `en-US` dùng được bản `en`. */
    private fun find(language: String): String? {
        values[language]?.takeIf { it.isNotBlank() }?.let { return it }
        val base = language.languageSubtag()
        return values.entries.firstOrNull { (key, value) ->
            key.languageSubtag() == base && value.isNotBlank()
        }?.value
    }

    companion object {
        val Empty = LocalizedText(emptyMap())
    }
}

/** `en-US` / `en_US` → `en`. */
fun String.languageSubtag(): String = substringBefore('-').substringBefore('_').lowercase()
