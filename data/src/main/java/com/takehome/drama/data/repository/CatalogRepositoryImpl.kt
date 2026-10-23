package com.takehome.drama.data.repository

import com.takehome.drama.data.mapper.toDomain
import com.takehome.drama.data.remote.datasource.CatalogDataSource
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
 */
class CatalogRepositoryImpl(
    private val dataSource: CatalogDataSource,
) : CatalogRepository {

    private val catalogMutex = Mutex()
    private var catalog: Catalog? = null

    private val episodeMutexesLock = Mutex()
    private val episodeMutexes = mutableMapOf<String, Mutex>()
    // Ghi dưới mutex của từng show nên các show ghi song song.
    private val episodes = ConcurrentHashMap<String, ShowEpisodes>()

    override suspend fun getCatalog(forceRefresh: Boolean): Result<Catalog> = catalogMutex.withLock {
        catalog?.takeUnless { forceRefresh }?.let { return@withLock Result.success(it) }
        withCatching { dataSource.getCatalog().toDomain() }
            .mapError { it.toDomainException() }
            .onSuccess { catalog = it }
    }

    override suspend fun getEpisodes(showId: String, forceRefresh: Boolean): Result<ShowEpisodes> =
        episodeMutex(showId).withLock {
            episodes[showId]?.takeUnless { forceRefresh }?.let { return@withLock Result.success(it) }
            withCatching {
                // File tập thiếu showId thì dùng id đã xin — tra cứu về sau không lệch.
                dataSource.getShowEpisodes(showId).toDomain().let { if (it.showId.isBlank()) it.copy(showId = showId) else it }
            }
                .mapError { it.toDomainException() }
                .onSuccess { episodes[showId] = it }
        }

    private suspend fun episodeMutex(showId: String): Mutex =
        episodeMutexesLock.withLock { episodeMutexes.getOrPut(showId) { Mutex() } }
}
