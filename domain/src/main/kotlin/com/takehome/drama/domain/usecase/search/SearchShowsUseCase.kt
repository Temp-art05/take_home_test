package com.takehome.drama.domain.usecase.search

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Show
import java.text.Normalizer
import java.util.Locale

sealed interface SearchResult {
    /** Chưa nhập (hoặc chỉ khoảng trắng). */
    data object Idle : SearchResult

    data object NoResults : SearchResult

    /** [suggestions] theo ngôn ngữ nội dung, tối đa [SearchShowsUseCase.MAX_SUGGESTIONS], đã khử trùng. */
    data class Matches(val suggestions: List<String>, val shows: List<Show>) : SearchResult
}

/**
 * Tìm client-side trên catalogue (không có API search): khớp title **mọi ngôn ngữ** và tên tag mọi
 * ngôn ngữ, không phân biệt hoa thường / dấu Latin / khoảng trắng-gạch dưới (tên tag server viết
 * `SweetRomance`, `حب_مؤلم`). Xếp: title bắt đầu bằng query > title chứa query > chỉ khớp tag; hoà
 * thì lượt xem giảm dần.
 */
class SearchShowsUseCase {

    operator fun invoke(catalog: Catalog, query: String, language: String): SearchResult {
        val needle = compact(query)
        if (needle.isEmpty()) return SearchResult.Idle

        val matchingTags = catalog.tags.filter { tag -> tag.name.values.values.any { compact(it).contains(needle) } }
        val matchingTagIds = matchingTags.mapTo(HashSet()) { it.id }
        val ranked = catalog.shows
            .mapNotNull { show -> rankOf(show, needle, matchingTagIds)?.let { show to it } }
            .sortedWith(compareBy<Pair<Show, Int>> { it.second }.thenByDescending { it.first.viewCount })
            .map { it.first }
        if (ranked.isEmpty()) return SearchResult.NoResults

        // Chỉ gợi ý tag có show trong kết quả: tag merchandising rỗng (`newest`) hứa một nhóm không tồn tại.
        val usedTagIds = ranked.flatMapTo(HashSet()) { it.tagIds }
        val suggestions = (
            matchingTags.filter { it.id in usedTagIds }.sortedBy { it.orderPriority }.map { it.name.resolveTagLabel(language, catalog.defaultLanguage) } +
                ranked.map { it.title.resolve(language, catalog.defaultLanguage) }
            )
            .filter { it.isNotBlank() }
            .distinctBy { compact(it) }
            .take(MAX_SUGGESTIONS)
        return SearchResult.Matches(suggestions = suggestions, shows = ranked)
    }

    /** null = không khớp. Số nhỏ đứng trước. */
    private fun rankOf(show: Show, needle: String, matchingTagIds: Set<String>): Int? {
        val titles = show.title.values.values.map(::compact)
        return when {
            titles.any { it.startsWith(needle) } -> RANK_PREFIX
            titles.any { it.contains(needle) } -> RANK_CONTAINS
            show.tagIds.any { it in matchingTagIds } -> RANK_TAG
            else -> null
        }
    }

    companion object {
        const val MAX_SUGGESTIONS = 4

        private const val RANK_PREFIX = 0
        private const val RANK_CONTAINS = 1
        private const val RANK_TAG = 2

        private val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")
        private val SEPARATORS = Regex("[\\s_\\-·.,:'’]+")

        /**
         * Chỉ bỏ dấu **Latin** (khối U+0300–036F): dấu nguyên âm của Devanagari/Tamil là một phần của
         * chữ, bỏ đi sẽ khớp bừa. Bỏ hết khoảng trắng để "sweet romance" khớp tag `SweetRomance`.
         */
        fun compact(text: String): String =
            Normalizer.normalize(text.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
                .replace(DIACRITICS, "")
                .replace(SEPARATORS, "")
    }
}
