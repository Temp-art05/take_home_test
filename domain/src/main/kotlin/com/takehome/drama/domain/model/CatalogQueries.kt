package com.takehome.drama.domain.model

/*
 * Truy vấn thuần trên một bản [Catalog] đã tải — ViewModel tải một lần (GetCatalogUseCase) rồi tính
 * nhiều view (Home, Browse, Search) trên cùng bản đó.
 */

/** Một rail merchandising ở Home: tag `isCategory = false` cùng các show gắn tag đó. */
data class ShowRail(val tag: Tag, val shows: List<Show>)

fun Catalog.findShow(showId: String): Show? = shows.firstOrNull { it.id == showId }

/** Tra `heroShowId` theo id — nó có thể không nằm đầu mảng. Trỏ sai/thiếu thì lấy show đầu cho Home không trống. */
fun Catalog.hero(): Show? = heroShowId?.let(::findShow) ?: shows.firstOrNull()

/** Rail không có show nào bị bỏ: hiện một tiêu đề trên hàng trống là ngõ cụt. */
fun Catalog.rails(): List<ShowRail> =
    tags.sortedTags(isCategory = false)
        .map { tag -> ShowRail(tag, shows.filter { tag.id in it.tagIds }) }
        .filter { it.shows.isNotEmpty() }

/** Chip genre của Browse. Genre không có show bị bỏ vì chip đó chỉ dẫn tới lưới rỗng. */
fun Catalog.genres(): List<Tag> =
    tags.sortedTags(isCategory = true).filter { tag -> shows.any { tag.id in it.tagIds } }

/** [tagId] null = chip "All". */
fun Catalog.showsInGenre(tagId: String?): List<Show> =
    if (tagId == null) shows else shows.filter { tagId in it.tagIds }

/** Tag của một show theo thứ tự `orderPriority`; tagId không có trong `tags` bị bỏ. */
fun Catalog.tagsOf(show: Show): List<Tag> = tags.filter { it.id in show.tagIds }.sortedBy { it.orderPriority }

/** `sortedBy` là stable: hai tag cùng `orderPriority` giữ thứ tự server. */
private fun List<Tag>.sortedTags(isCategory: Boolean): List<Tag> =
    filter { it.isCategory == isCategory }.sortedBy { it.orderPriority }

/**
 * Số tập hiển thị. Lưới tin file tập (`13077`: catalogue ghi 65, file có 50 — ô nào cũng phải
 * phát được); `episodeCount` của catalogue chỉ dùng khi chưa tải tập (card).
 */
fun Show.episodeTotal(episodes: ShowEpisodes?): Int = episodes?.episodes?.size ?: catalogEpisodeCount
