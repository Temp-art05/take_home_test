package com.takehome.drama.domain.usecase.reels

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.usecase.discovery.rankForTaste

/**
 * Tab Reels (SCR-0203): mỗi show một reel, show khớp gu trước rồi lượt xem giảm dần. Feed chỉ cần
 * catalogue; tập/URL nạp lười cho vài reel quanh trang đang xem (`GetReelClipUseCase`).
 */
class GetReelsFeedUseCase {
    operator fun invoke(catalog: Catalog, tasteTagIds: Set<String>): List<Show> = rankForTaste(catalog.shows, tasteTagIds)
}
