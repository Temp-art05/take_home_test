package com.takehome.drama.domain.usecase.discovery

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowRail
import com.takehome.drama.domain.model.hero
import com.takehome.drama.domain.model.rails

/**
 * Phần catalogue của Home (hero, rail theo tag, "Also flowing"). Hàng "Pick up your flow" là
 * `ObserveWatchHistoryUseCase` (Watching) — tiến độ là stream riêng, không trộn vào đây.
 */
data class HomeFeed(
    val hero: Show?,
    /** Tag `isCategory = false` có ít nhất một show, theo `orderPriority`. Fixtures: rỗng. */
    val rails: List<ShowRail>,
    /** Mọi show trừ hero, show khớp gu lên trước rồi lượt xem giảm dần. */
    val alsoFlowing: List<Show>,
)

class GetHomeFeedUseCase {
    operator fun invoke(catalog: Catalog, tasteTagIds: Set<String>): HomeFeed {
        val hero = catalog.hero()
        return HomeFeed(
            hero = hero,
            rails = catalog.rails(),
            alsoFlowing = rankForTaste(catalog.shows.filterNot { it.id == hero?.id }, tasteTagIds),
        )
    }
}

/**
 * Show có ít nhất một tag trong gu (chọn ở SCR-0004) đứng trước, trong mỗi nhóm `viewCount` giảm
 * dần. Không có gu (Skip) thì chỉ còn thứ tự lượt xem.
 */
fun rankForTaste(shows: List<Show>, tasteTagIds: Set<String>): List<Show> =
    shows.sortedWith(
        compareByDescending<Show> { show -> show.tagIds.any { it in tasteTagIds } }
            .thenByDescending { it.viewCount },
    )

/** Preview Browse (SCR-0202) tự chạy khi không giới hạn Wi-Fi, hoặc đang ở mạng không tính phí. */
object PreviewAutoplayPolicy {
    fun shouldAutoplay(wifiOnly: Boolean, isUnmetered: Boolean): Boolean = !wifiOnly || isUnmetered
}
