package com.takehome.drama.data.repository

import com.takehome.drama.data.local.datasource.CatalogCacheLocalDataSource
import com.takehome.drama.data.mapper.toDomain
import com.takehome.drama.data.remote.datasource.CatalogDataSource
import com.takehome.drama.data.remote.dto.ShowEpisodesDto
import com.takehome.drama.data.remote.error.toDomainException
import com.takehome.drama.data.util.mapError
import com.takehome.drama.data.util.withCatching
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.repository.CatalogRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Giữ bản thành công trong bộ nhớ và cho **một** request chạy mỗi lúc (single-flight): Splash,
 * Home, Browse cùng xin catalogue thì chỉ một lần gọi — host thật trả 403 khi bị gọi dồn. Lỗi
 * không được cache để Retry gọi lại thật. Mất khi process chết; lần mở sau tải lại.
 * File tập single-flight **theo từng show**: show A tải chậm không chặn show B (Reels lướt nhiều show).
 *
 * Tải thành công thì ghi [cache] đĩa; tải lỗi (mất mạng, 403...) mà đĩa có bản cũ thì trả bản đó thay vì lỗi.
 * Bản đĩa **không** giữ vào RAM — lần gọi sau (Try again, màn khác) vẫn thử mạng trước.
 */
class CatalogRepositoryImpl(
    private val dataSource: CatalogDataSource,
    private val cache: CatalogCacheLocalDataSource,
) : CatalogRepository {

    private val catalogMutex = Mutex()
    private var catalog: Catalog? = null

    private val episodeMutexesLock = Mutex()
    private val episodeMutexes = mutableMapOf<String, Mutex>()
    // Ghi dưới mutex của từng show nên các show ghi song song.
    private val episodes = ConcurrentHashMap<String, ShowEpisodes>()

    override suspend fun getCatalog(forceRefresh: Boolean): Result<Catalog> = catalogMutex.withLock {
        catalog?.takeUnless { forceRefresh }?.let { return@withLock Result.success(it) }
        withCatching {
            val dto = dataSource.getCatalog()
            // Sai kiểu cả mảng `shows` → parser khoan dung trả rỗng: coi là lỗi, không ghi đè bản tốt trên đĩa.
            dto.toDomain().also { check(it.shows.isNotEmpty()) { "Catalogue has no readable show" }; cache.writeCatalog(dto) }
        }
            .onSuccess { catalog = it }
            .recoverFromCache { cache.readCatalog()?.toDomain() }
            .mapError { it.toDomainException() }
    }

    override suspend fun getEpisodes(showId: String, forceRefresh: Boolean): Result<ShowEpisodes> =
        episodeMutex(showId).withLock {
            episodes[showId]?.takeUnless { forceRefresh }?.let { return@withLock Result.success(it) }
            withCatching { dataSource.getShowEpisodes(showId).also { cache.writeEpisodes(showId, it) }.toDomain(showId) }
                .onSuccess { episodes[showId] = it }
                .recoverFromCache { cache.readEpisodes(showId)?.toDomain(showId) }
                .mapError { it.toDomainException() }
        }

    // File tập thiếu showId thì dùng id đã xin — tra cứu về sau không lệch.
    private fun ShowEpisodesDto.toDomain(showId: String): ShowEpisodes =
        toDomain().let { if (it.showId.isBlank()) it.copy(showId = showId) else it }

    /** Lỗi tải mà đĩa có bản cũ → thành công với bản đó; không có → giữ lỗi gốc. */
    private suspend fun <T> Result<T>.recoverFromCache(read: suspend () -> T?): Result<T> {
        val error = exceptionOrNull() ?: return this
        return read()?.let { Result.success(it) } ?: Result.failure(error)
    }

    private suspend fun episodeMutex(showId: String): Mutex =
        episodeMutexesLock.withLock { episodeMutexes.getOrPut(showId) { Mutex() } }
}
