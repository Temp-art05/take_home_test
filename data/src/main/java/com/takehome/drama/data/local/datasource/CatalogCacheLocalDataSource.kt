package com.takehome.drama.data.local.datasource

import com.takehome.drama.data.remote.dto.CatalogDto
import com.takehome.drama.data.remote.dto.ShowEpisodesDto
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Bản catalogue / file tập lần tải thành công gần nhất, để mở app khi mất mạng vẫn có dữ liệu.
 * Đọc lỗi (chưa có file, file hỏng) trả `null`; ghi lỗi chỉ log — cache là phần phụ, không làm hỏng
 * lượt tải mạng đã thành công.
 */
interface CatalogCacheLocalDataSource {
    suspend fun readCatalog(): CatalogDto?

    suspend fun writeCatalog(catalog: CatalogDto)

    suspend fun readEpisodes(showId: String): ShowEpisodesDto?

    suspend fun writeEpisodes(showId: String, episodes: ShowEpisodesDto)
}

class CatalogCacheLocalDataSourceImpl(
    private val directory: File,
    private val json: Json,
) : CatalogCacheLocalDataSource {

    override suspend fun readCatalog(): CatalogDto? = read(CATALOG_FILE, CatalogDto.serializer())

    override suspend fun writeCatalog(catalog: CatalogDto) = write(CATALOG_FILE, CatalogDto.serializer(), catalog)

    override suspend fun readEpisodes(showId: String): ShowEpisodesDto? =
        read(episodesFile(showId), ShowEpisodesDto.serializer())

    override suspend fun writeEpisodes(showId: String, episodes: ShowEpisodesDto) =
        write(episodesFile(showId), ShowEpisodesDto.serializer(), episodes)

    private suspend fun <T> read(name: String, serializer: KSerializer<T>): T? = withContext(Dispatchers.IO) {
        val file = File(directory, name)
        if (!file.exists()) return@withContext null
        try {
            json.decodeFromString(serializer, file.readText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Catalog cache %s unreadable", name)
            null
        }
    }

    private suspend fun <T> write(name: String, serializer: KSerializer<T>, value: T) = withContext(Dispatchers.IO) {
        try {
            directory.mkdirs()
            // Ghi file tạm rồi đổi tên: process chết giữa chừng không để lại file cụt.
            val tmp = File(directory, "$name.tmp")
            tmp.writeText(json.encodeToString(serializer, value))
            if (!tmp.renameTo(File(directory, name))) Timber.w("Catalog cache %s rename failed", name)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Catalog cache %s not written", name)
        }
    }

    // showId đi vào tên file: chỉ giữ ký tự an toàn.
    private fun episodesFile(showId: String) = "show_${showId.replace(Regex("[^A-Za-z0-9_-]"), "_")}.json"

    private companion object {
        const val CATALOG_FILE = "catalog.json"
    }
}
