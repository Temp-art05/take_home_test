package com.takehome.drama.feature.watch.fake

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Episode
import com.takehome.drama.domain.model.LocalizedText
import com.takehome.drama.domain.model.SavedShow
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.repository.CatalogRepository
import com.takehome.drama.domain.repository.SavedShowRepository
import com.takehome.drama.domain.repository.WatchProgressRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeCatalogRepository(
    var catalog: Result<Catalog>,
    var episodes: Map<String, Result<ShowEpisodes>> = emptyMap(),
) : CatalogRepository {
    /** Đặt để giữ `getEpisodes` treo tới khi test `complete` — giả lập mạng chậm. */
    var episodesGate: CompletableDeferred<Unit>? = null
    var catalogCalls = 0
    var episodesCalls = 0

    override suspend fun getCatalog(forceRefresh: Boolean): Result<Catalog> {
        catalogCalls++
        return catalog
    }

    override suspend fun getEpisodes(showId: String, forceRefresh: Boolean): Result<ShowEpisodes> {
        episodesCalls++
        episodesGate?.await()
        return episodes[showId] ?: Result.failure(NoSuchElementException(showId))
    }
}

class FakeWatchProgressRepository : WatchProgressRepository {
    val rows = MutableStateFlow<List<WatchProgress>>(emptyList())

    /** Tập có gate thì `get` (đọc vị trí resume) treo tới khi test `complete`. */
    val getGates = mutableMapOf<Int, CompletableDeferred<Unit>>()

    override suspend fun save(progress: WatchProgress): Result<Unit> {
        rows.update { list ->
            list.filterNot { it.showId == progress.showId && it.episodeIndex == progress.episodeIndex } + progress
        }
        return Result.success(Unit)
    }

    override suspend fun get(showId: String, episodeIndex: Int): WatchProgress? {
        getGates[episodeIndex]?.await()
        return rows.value.firstOrNull { it.showId == showId && it.episodeIndex == episodeIndex }
    }

    override suspend fun latestForShow(showId: String): WatchProgress? =
        rows.value.filter { it.showId == showId }.maxByOrNull { it.updatedAt }

    override fun observeLatestPerShow(): Flow<List<WatchProgress>> = rows

    override fun observeForShow(showId: String): Flow<List<WatchProgress>> =
        rows.map { list -> list.filter { it.showId == showId }.sortedBy { it.episodeIndex } }
}

class FakeSavedShowRepository : SavedShowRepository {
    val saved = MutableStateFlow<Set<String>>(emptySet())

    override suspend fun save(showId: String): Result<Unit> {
        saved.update { it + showId }
        return Result.success(Unit)
    }

    override suspend fun remove(showId: String): Result<Unit> {
        saved.update { it - showId }
        return Result.success(Unit)
    }

    override fun observeIsSaved(showId: String): Flow<Boolean> = saved.map { showId in it }

    override fun observeAll(): Flow<List<SavedShow>> = saved.map { ids -> ids.map { SavedShow(it, 0) } }
}

const val SHOW_ID = "12692"

fun testShow(id: String = SHOW_ID, episodeCount: Int = 65) = Show(
    id = id,
    title = LocalizedText(mapOf("en" to "The Ex-Wife Returns", "ar" to "عودة الزوجة")),
    synopsis = LocalizedText(mapOf("en" to "Synopsis")),
    hook = LocalizedText(mapOf("en" to "Hook")),
    posterUrl = "https://e/p.jpg",
    catalogEpisodeCount = episodeCount,
    status = ShowStatus.Completed,
    viewCount = 3_000_000,
    tagIds = listOf("romance"),
)

fun testCatalog(vararg shows: Show = arrayOf(testShow())) = Catalog(
    version = 1,
    defaultLanguage = "en",
    availableLanguages = listOf("en", "ar"),
    tags = listOf(com.takehome.drama.domain.model.Tag("romance", LocalizedText(mapOf("en" to "Romance")), true, 1)),
    heroShowId = null,
    shows = shows.toList(),
)

fun testEpisode(index: Int, isFree: Boolean = true, qualities: List<String> = listOf("720p")) = Episode(
    index = index,
    number = index + 1,
    title = LocalizedText.Empty,
    isFree = isFree,
    likeCount = 1_000,
    durationSeconds = 0,
    sources = mapOf(
        "en" to qualities.associateWith { "https://e/en/$index/$it.m3u8" },
        "ar" to qualities.associateWith { "https://e/ar/$index/$it.m3u8" },
    ),
)

/** [free] tập đầu free, còn lại khoá — giống fixtures. */
fun testEpisodes(count: Int, free: Int = 5, qualities: List<String> = listOf("720p")) =
    ShowEpisodes(SHOW_ID, List(count) { testEpisode(it, isFree = it < free, qualities = qualities) })

fun progress(index: Int, positionMs: Long, durationMs: Long = 100_000, updatedAt: Long = index.toLong()) =
    WatchProgress(SHOW_ID, index, index + 1, 50, positionMs, durationMs, updatedAt)
