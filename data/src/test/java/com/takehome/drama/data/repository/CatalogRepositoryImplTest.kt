package com.takehome.drama.data.repository

import com.takehome.drama.data.local.datasource.CatalogCacheLocalDataSource
import com.takehome.drama.data.remote.datasource.CatalogDataSource
import com.takehome.drama.data.remote.dto.CatalogDto
import com.takehome.drama.data.remote.dto.ShowDto
import com.takehome.drama.data.remote.dto.ShowEpisodesDto
import com.takehome.drama.data.remote.error.ApiException
import com.takehome.drama.domain.error.DomainException
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryImplTest {

    private class FakeDataSource : CatalogDataSource {
        val catalogCalls = AtomicInteger()
        var gate: CompletableDeferred<Unit>? = null
        var catalogError: Throwable? = null
        var episodesError: Throwable? = null
        val episodeGates = mutableMapOf<String, CompletableDeferred<Unit>>()
        val episodeCalls = AtomicInteger()

        override suspend fun getCatalog(): CatalogDto {
            catalogCalls.incrementAndGet()
            gate?.await()
            catalogError?.let { throw it }
            return CatalogDto(shows = listOf(ShowDto(id = "1")))
        }

        override suspend fun getShowEpisodes(showId: String): ShowEpisodesDto {
            episodeCalls.incrementAndGet()
            episodeGates[showId]?.await()
            episodesError?.let { throw it }
            return ShowEpisodesDto(showId = "", episodes = emptyList())
        }
    }

    private class FakeCache : CatalogCacheLocalDataSource {
        var catalog: CatalogDto? = null
        val episodes = mutableMapOf<String, ShowEpisodesDto>()

        override suspend fun readCatalog(): CatalogDto? = catalog
        override suspend fun writeCatalog(catalog: CatalogDto) {
            this.catalog = catalog
        }

        override suspend fun readEpisodes(showId: String): ShowEpisodesDto? = episodes[showId]
        override suspend fun writeEpisodes(showId: String, episodes: ShowEpisodesDto) {
            this.episodes[showId] = episodes
        }
    }

    private val source = FakeDataSource()
    private val cache = FakeCache()
    private val repository = CatalogRepositoryImpl(source, cache)

    @Test
    fun `concurrent callers share one request - host rate limits with 403`() = runTest {
        source.gate = CompletableDeferred()
        val calls = List(3) { async { repository.getCatalog() } }
        testScheduler.runCurrent()
        source.gate?.complete(Unit)

        assertTrue(calls.awaitAll().all { it.isSuccess })
        assertEquals(1, source.catalogCalls.get())
    }

    @Test
    fun `cached result is reused until forceRefresh`() = runTest {
        repository.getCatalog()
        repository.getCatalog()
        assertEquals(1, source.catalogCalls.get())

        repository.getCatalog(forceRefresh = true)
        assertEquals(2, source.catalogCalls.get())
    }

    @Test
    fun `failure is not cached so retry hits the source again`() = runTest {
        source.catalogError = IOException("offline")
        assertTrue(repository.getCatalog().exceptionOrNull() is DomainException.Network)

        source.catalogError = null
        assertTrue(repository.getCatalog().isSuccess)
        assertEquals(2, source.catalogCalls.get())
    }

    @Test
    fun `http errors map to domain errors`() = runTest {
        source.catalogError = ApiException(status = 403, errorCode = null, serverMessage = null)
        assertEquals(403, (repository.getCatalog().exceptionOrNull() as DomainException.Server).statusCode)

        source.episodesError = ApiException(status = 404, errorCode = null, serverMessage = null)
        assertTrue(repository.getEpisodes("x").exceptionOrNull() is DomainException.NotFound)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is rethrown, not turned into a failure`() = runTest {
        source.catalogError = CancellationException("cancelled")
        repository.getCatalog()
    }

    @Test
    fun `slow episodes of one show do not block another show`() = runTest {
        source.episodeGates["A"] = CompletableDeferred()
        val slow = async { repository.getEpisodes("A") }
        testScheduler.runCurrent()

        assertTrue(repository.getEpisodes("B").isSuccess)
        assertTrue(slow.isActive)

        source.episodeGates.getValue("A").complete(Unit)
        assertTrue(slow.await().isSuccess)
    }

    @Test
    fun `concurrent callers for the same show share one episodes request`() = runTest {
        source.episodeGates["A"] = CompletableDeferred()
        val calls = List(3) { async { repository.getEpisodes("A") } }
        testScheduler.runCurrent()
        source.episodeGates.getValue("A").complete(Unit)

        assertTrue(calls.awaitAll().all { it.isSuccess })
        assertEquals(1, source.episodeCalls.get())
    }

    @Test
    fun `episodes file without showId gets the requested id`() = runTest {
        assertEquals("42", repository.getEpisodes("42").getOrThrow().showId)
    }

    @Test
    fun `successful catalogue is written to the disk cache`() = runTest {
        repository.getCatalog()
        assertEquals(listOf("1"), cache.catalog?.shows?.map { it.id })
    }

    @Test
    fun `offline falls back to the last catalogue on disk`() = runTest {
        cache.catalog = CatalogDto(shows = listOf(ShowDto(id = "cached")))
        source.catalogError = IOException("offline")

        assertEquals(listOf("cached"), repository.getCatalog().getOrThrow().shows.map { it.id })
    }

    @Test
    fun `disk fallback is not kept in memory so the next call tries the network`() = runTest {
        cache.catalog = CatalogDto(shows = listOf(ShowDto(id = "cached")))
        source.catalogError = IOException("offline")
        repository.getCatalog()

        source.catalogError = null
        assertEquals(listOf("1"), repository.getCatalog().getOrThrow().shows.map { it.id })
        assertEquals(2, source.catalogCalls.get())
    }

    @Test
    fun `offline episodes fall back to disk, keeping the requested id`() = runTest {
        repository.getEpisodes("7")
        source.episodesError = IOException("offline")

        val cached = CatalogRepositoryImpl(source, cache).getEpisodes("7")
        assertEquals("7", cached.getOrThrow().showId)
    }
}
