package com.takehome.drama.feature.main.presentation.common

import androidx.compose.runtime.Immutable
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowStatus

/**
 * Show đã resolve text theo ngôn ngữ nội dung — dùng chung cho thẻ của Home, Search, Browse. Số tập là
 * `episodeCount` của catalogue (chưa tải file tập ở các màn danh sách).
 */
@Immutable
data class ShowCard(
    val id: String,
    val title: String,
    val posterUrl: String,
    val episodeCount: Int,
    /** Genre đầu tiên (theo `orderPriority`) để hiện meta; null khi show không gắn genre nào. */
    val genre: String?,
    val status: ShowStatus,
)

fun Show.toCard(catalog: Catalog, language: String): ShowCard = ShowCard(
    id = id,
    title = title.resolve(language, catalog.defaultLanguage),
    posterUrl = posterUrl,
    episodeCount = catalogEpisodeCount,
    genre = catalog.tags
        .filter { it.isCategory && it.id in tagIds }
        .minByOrNull { it.orderPriority }
        ?.name?.resolveTagLabel(language, catalog.defaultLanguage),
    status = status,
)
