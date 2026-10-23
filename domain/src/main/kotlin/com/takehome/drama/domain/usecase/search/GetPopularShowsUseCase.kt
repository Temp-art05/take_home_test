package com.takehome.drama.domain.usecase.search

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Show

/** "Popular now" của Search lúc chưa nhập. */
class GetPopularShowsUseCase {
    operator fun invoke(catalog: Catalog, limit: Int = POPULAR_COUNT): List<Show> =
        catalog.shows.sortedByDescending { it.viewCount }.take(limit)

    private companion object {
        const val POPULAR_COUNT = 5
    }
}
