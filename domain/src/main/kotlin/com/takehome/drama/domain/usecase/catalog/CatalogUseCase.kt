package com.takehome.drama.domain.usecase.catalog

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.Tag
import com.takehome.drama.domain.model.languageSubtag
import com.takehome.drama.domain.repository.CatalogRepository

/** Một rail merchandising ở Home: tag `isCategory = false` cùng các show gắn tag đó. */
data class ShowRail(val tag: Tag, val shows: List<Show>)

/**
 * Đọc catalogue và các rule chọn/xếp nội dung trên đó. Các hàm thuần nhận [Catalog] vào để
 * ViewModel tải một lần rồi tính nhiều view (Home, Browse, Search) trên cùng một bản.
 */
class CatalogUseCase(
    private val catalogRepository: CatalogRepository,
) {
    suspend fun load(forceRefresh: Boolean = false): Result<Catalog> =
        catalogRepository.getCatalog(forceRefresh)

    fun findShow(catalog: Catalog, showId: String): Show? = catalog.shows.firstOrNull { it.id == showId }

    /** Tra `heroShowId` theo id — nó có thể không nằm đầu mảng. Trỏ sai/thiếu thì lấy show đầu cho Home không trống. */
    fun hero(catalog: Catalog): Show? =
        catalog.heroShowId?.let { findShow(catalog, it) } ?: catalog.shows.firstOrNull()

    /** Rail không có show nào bị bỏ: hiện một tiêu đề trên hàng trống là ngõ cụt. */
    fun rails(catalog: Catalog): List<ShowRail> =
        catalog.tags.sortedTags(isCategory = false)
            .map { tag -> ShowRail(tag, catalog.shows.filter { tag.id in it.tagIds }) }
            .filter { it.shows.isNotEmpty() }

    /** Chip genre của Browse. Genre không có show bị bỏ vì chip đó chỉ dẫn tới lưới rỗng. */
    fun genres(catalog: Catalog): List<Tag> =
        catalog.tags.sortedTags(isCategory = true)
            .filter { tag -> catalog.shows.any { tag.id in it.tagIds } }

    /** [tagId] null = chip "All". */
    fun showsInGenre(catalog: Catalog, tagId: String?): List<Show> =
        if (tagId == null) catalog.shows else catalog.shows.filter { tagId in it.tagIds }

    /** Tag của một show theo thứ tự `orderPriority`; tagId không có trong `tags` bị bỏ. */
    fun tagsOf(show: Show, catalog: Catalog): List<Tag> =
        catalog.tags.filter { it.id in show.tagIds }.sortedBy { it.orderPriority }

    /**
     * Ngôn ngữ nội dung: lựa chọn đã lưu (nếu catalogue còn có) → ngôn ngữ máy (nếu có) →
     * `defaultLanguage`.
     */
    fun resolveContentLanguage(saved: String?, deviceLanguage: String, catalog: Catalog): String {
        val available = catalog.availableLanguages
        fun offered(language: String?): String? =
            language?.languageSubtag()?.let { base -> available.firstOrNull { it.languageSubtag() == base } }
        return offered(saved) ?: offered(deviceLanguage) ?: catalog.defaultLanguage
    }

    /** `sortedBy` là stable: hai tag cùng `orderPriority` giữ thứ tự server. */
    private fun List<Tag>.sortedTags(isCategory: Boolean): List<Tag> =
        filter { it.isCategory == isCategory }.sortedBy { it.orderPriority }
}
