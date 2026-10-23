package com.takehome.drama.data.remote.datasource

import com.takehome.drama.data.remote.dto.CatalogDto
import com.takehome.drama.data.remote.dto.ShowEpisodesDto

/**
 * Nguồn catalogue. Hai impl cùng contract: [CatalogRemoteDataSourceImpl] gọi API thật,
 * [CatalogAssetDataSourceImpl] đọc fixtures (flavor dev — host thật trả 403 khi bị gọi dồn).
 * `DataModule` chọn theo `AppConfig.useFixtures`.
 */
interface CatalogDataSource {
    suspend fun getCatalog(): CatalogDto

    suspend fun getShowEpisodes(showId: String): ShowEpisodesDto
}
