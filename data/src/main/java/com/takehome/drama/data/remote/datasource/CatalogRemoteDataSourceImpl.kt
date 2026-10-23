package com.takehome.drama.data.remote.datasource

import com.takehome.drama.data.remote.dto.CatalogDto
import com.takehome.drama.data.remote.dto.ShowEpisodesDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class CatalogRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : CatalogDataSource {
    override suspend fun getCatalog(): CatalogDto = httpClient.get("catalog.json").body()

    override suspend fun getShowEpisodes(showId: String): ShowEpisodesDto =
        httpClient.get("shows/$showId.json").body()
}
