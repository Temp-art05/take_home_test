package com.takehome.drama.data.local

import com.takehome.drama.data.local.datasource.CatalogCacheLocalDataSourceImpl
import com.takehome.drama.data.remote.dto.CatalogDto
import com.takehome.drama.data.remote.dto.ShowDto
import com.takehome.drama.data.remote.dto.ShowEpisodesDto
import com.takehome.drama.data.remote.http.defaultJson
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CatalogCacheLocalDataSourceTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dir by lazy { File(folder.root, "catalog_cache") }
    private val cache by lazy { CatalogCacheLocalDataSourceImpl(dir, defaultJson) }

    @Test
    fun `nothing cached yet reads null`() = runTest {
        assertNull(cache.readCatalog())
        assertNull(cache.readEpisodes("1"))
    }

    @Test
    fun `catalogue survives a round trip`() = runTest {
        cache.writeCatalog(CatalogDto(shows = listOf(ShowDto(id = "a"), ShowDto(id = "b"))))
        assertEquals(listOf("a", "b"), cache.readCatalog()?.shows?.map { it.id })
    }

    @Test
    fun `episodes are kept per show`() = runTest {
        cache.writeEpisodes("1", ShowEpisodesDto(showId = "1", episodes = emptyList()))
        cache.writeEpisodes("2", ShowEpisodesDto(showId = "2", episodes = emptyList()))
        assertEquals("1", cache.readEpisodes("1")?.showId)
        assertEquals("2", cache.readEpisodes("2")?.showId)
    }

    @Test
    fun `corrupt file reads as null instead of crashing`() = runTest {
        dir.mkdirs()
        File(dir, "catalog.json").writeText("{not json")
        assertNull(cache.readCatalog())
    }

    @Test
    fun `odd show ids cannot escape the cache directory`() = runTest {
        cache.writeEpisodes("../x", ShowEpisodesDto(showId = "x", episodes = emptyList()))
        assertEquals(listOf("show____x.json"), dir.list()?.toList())
    }
}
