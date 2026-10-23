package com.takehome.drama.domain.repository

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.ShowEpisodes

interface CatalogRepository {
    /** Kết quả thành công được giữ trong bộ nhớ; [forceRefresh] bỏ qua bản đó. */
    suspend fun getCatalog(forceRefresh: Boolean = false): Result<Catalog>

    suspend fun getEpisodes(showId: String, forceRefresh: Boolean = false): Result<ShowEpisodes>
}
