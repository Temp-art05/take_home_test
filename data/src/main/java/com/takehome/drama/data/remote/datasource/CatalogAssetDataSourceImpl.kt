package com.takehome.drama.data.remote.datasource

import android.content.res.AssetManager
import com.takehome.drama.data.remote.dto.CatalogDto
import com.takehome.drama.data.remote.dto.ShowEpisodesDto
import com.takehome.drama.data.remote.error.ApiException
import java.io.FileNotFoundException
import kotlinx.serialization.json.Json

/**
 * Đọc `assets/fixtures/` (chỉ có trong APK flavor dev) — đúng JSON của API. File thiếu ném
 * [ApiException] 404 để chuỗi lỗi giống hệt khi gọi mạng.
 */
class CatalogAssetDataSourceImpl(
    private val assets: AssetManager,
    private val json: Json,
) : CatalogDataSource {
    override suspend fun getCatalog(): CatalogDto = json.decodeFromString(read("catalog.json"))

    override suspend fun getShowEpisodes(showId: String): ShowEpisodesDto =
        json.decodeFromString(read("shows/$showId.json"))

    private fun read(path: String): String = try {
        assets.open("$ROOT/$path").bufferedReader().use { it.readText() }
    } catch (e: FileNotFoundException) {
        throw ApiException(status = 404, errorCode = null, serverMessage = e.message)
    }

    private companion object {
        const val ROOT = "fixtures"
    }
}
