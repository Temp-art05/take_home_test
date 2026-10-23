package com.takehome.drama.feature.watch.fake

import com.takehome.drama.core.media.EpisodeFeedPlayer
import com.takehome.drama.core.media.FeedItem
import com.takehome.drama.core.media.VideoSlot
import com.takehome.drama.core.media.PlaybackState
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Episode
import com.takehome.drama.domain.model.LocalizedText
import com.takehome.drama.domain.model.SavedShow
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.domain.model.ThemeMode
import com.takehome.drama.domain.model.UserPreferences
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.repository.CatalogRepository
import com.takehome.drama.domain.repository.SavedShowRepository
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.repository.WatchProgressRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

class FakeUserPreferencesRepository(initial: UserPreferences = UserPreferences(contentLanguage = "en")) :
    UserPreferencesRepository {
    val state = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = state

    override suspend fun setContentLanguage(language: String) = set { copy(contentLanguage = language) }

    override suspend fun setTasteTagIds(tagIds: Set<String>) = set { copy(tasteTagIds = tagIds) }

    override suspend fun setTheme(theme: ThemeMode) = set { copy(theme = theme) }

    override suspend fun setOnboardingDone(done: Boolean) = set { copy(onboardingDone = done) }

    override suspend fun setAutoplayPreviewWifiOnly(wifiOnly: Boolean) = set { copy(autoplayPreviewWifiOnly = wifiOnly) }

    override suspend fun setAutoplayNextEpisode(enabled: Boolean) = set { copy(autoplayNextEpisode = enabled) }

    private fun set(block: UserPreferences.() -> UserPreferences): Result<Unit> {
        state.update(block)
        return Result.success(Unit)
    }
}

/**
 * Player giả: ghi lại lệnh, test tự đẩy [PlaybackState] qua [emit]. Mỗi `setCurrent` vào mục có URL
 * được ghi thành một [Load] (URL lấy từ playlist) — giống "nạp nguồn" ở player thật.
 */
class FakeFeedPlayer : EpisodeFeedPlayer {
    data class Load(val url: String, val startPositionMs: Long, val playWhenReady: Boolean)

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state
    override val slots: StateFlow<Map<Int, VideoSlot>> = MutableStateFlow(emptyMap())

    var items: List<FeedItem> = emptyList()
        private set
    val loads = mutableListOf<Load>()

    /** Vị trí playlist của mọi lần `setCurrent`, kể cả mục khoá. */
    val currents = mutableListOf<Int>()
    var isPreloadActive = true
        private set
    var lastSpeed = 1f
    var released = false
    var position = 0L

    override fun setPlaylist(items: List<FeedItem>) {
        this.items = items
    }

    override fun setCurrent(position: Int, startPositionMs: Long, playWhenReady: Boolean) {
        currents += position
        val url = items[position].url
        if (url == null) {
            _state.value = PlaybackState()
            return
        }
        loads += Load(url, startPositionMs, playWhenReady)
        this.position = startPositionMs
        _state.value = PlaybackState(positionMs = startPositionMs, playWhenReady = playWhenReady, isBuffering = true)
    }

    override fun setPreloadActive(active: Boolean) {
        isPreloadActive = active
    }

    override fun play() = _state.update { it.copy(playWhenReady = true, isPlaying = true, ended = false) }

    override fun pause() = _state.update { it.copy(playWhenReady = false, isPlaying = false) }

    override fun seekTo(positionMs: Long) {
        position = positionMs
        _state.update { it.copy(positionMs = positionMs) }
    }

    override fun setSpeed(speed: Float) {
        lastSpeed = speed
    }

    override fun currentPositionMs(): Long = position

    override fun release() {
        released = true
    }

    /** Giả lập player đang phát ở [positionMs] / [durationMs]. */
    fun playing(positionMs: Long, durationMs: Long = 100_000) {
        position = positionMs
        _state.value = PlaybackState(isPlaying = true, playWhenReady = true, positionMs = positionMs, durationMs = durationMs)
    }

    fun emit(state: PlaybackState) {
        position = state.positionMs
        _state.value = state
    }
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
