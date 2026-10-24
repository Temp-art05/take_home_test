package com.takehome.drama.domain.fake

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.ads.AdsConfig
import com.takehome.drama.domain.model.SavedShow
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.ThemeMode
import com.takehome.drama.domain.model.UserPreferences
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.repository.AdsConfigRepository
import com.takehome.drama.domain.repository.CatalogRepository
import com.takehome.drama.domain.repository.EpisodeUnlockRepository
import com.takehome.drama.domain.repository.SavedShowRepository
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.repository.WatchProgressRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeCatalogRepository(
    var catalog: Result<Catalog>,
    var episodes: Map<String, ShowEpisodes> = emptyMap(),
) : CatalogRepository {
    /** Đặt để giữ `getCatalog` treo tới khi test `complete` — giả lập mạng chậm. */
    var catalogGate: CompletableDeferred<Unit>? = null
    var catalogCalls = 0
        private set

    /** showId theo thứ tự được gọi `getEpisodes`. */
    val episodesCalls = mutableListOf<String>()

    override suspend fun getCatalog(forceRefresh: Boolean): Result<Catalog> {
        catalogCalls++
        catalogGate?.await()
        return catalog
    }

    override suspend fun getEpisodes(showId: String, forceRefresh: Boolean): Result<ShowEpisodes> {
        episodesCalls += showId
        return episodes[showId]?.let { Result.success(it) } ?: Result.failure(NoSuchElementException(showId))
    }
}

class FakeWatchProgressRepository : WatchProgressRepository {
    val rows = MutableStateFlow<List<WatchProgress>>(emptyList())

    override suspend fun save(progress: WatchProgress): Result<Unit> {
        rows.update { list ->
            list.filterNot { it.showId == progress.showId && it.episodeIndex == progress.episodeIndex } + progress
        }
        return Result.success(Unit)
    }

    override suspend fun get(showId: String, episodeIndex: Int): WatchProgress? =
        rows.value.firstOrNull { it.showId == showId && it.episodeIndex == episodeIndex }

    override suspend fun latestForShow(showId: String): WatchProgress? =
        rows.value.filter { it.showId == showId }.maxByOrNull { it.updatedAt }

    override fun observeLatestPerShow(): Flow<List<WatchProgress>> = rows.map { list ->
        list.groupBy { it.showId }.values.map { it.maxBy { p -> p.updatedAt } }.sortedByDescending { it.updatedAt }
    }

    override fun observeForShow(showId: String): Flow<List<WatchProgress>> =
        rows.map { list -> list.filter { it.showId == showId }.sortedBy { it.episodeIndex } }
}

class FakeSavedShowRepository : SavedShowRepository {
    val rows = MutableStateFlow<List<SavedShow>>(emptyList())
    private var clock = 0L

    override suspend fun save(showId: String): Result<Unit> {
        rows.update { list -> list.filterNot { it.showId == showId } + SavedShow(showId, ++clock) }
        return Result.success(Unit)
    }

    override suspend fun remove(showId: String): Result<Unit> {
        rows.update { list -> list.filterNot { it.showId == showId } }
        return Result.success(Unit)
    }

    override fun observeIsSaved(showId: String): Flow<Boolean> = rows.map { list -> list.any { it.showId == showId } }

    override fun observeAll(): Flow<List<SavedShow>> = rows.map { list -> list.sortedByDescending { it.savedAt } }
}

class FakeUserPreferencesRepository(initial: UserPreferences = UserPreferences(contentLanguage = "en")) :
    UserPreferencesRepository {
    val state = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = state

    /** Đặt để mọi lần ghi treo tới khi test `complete` — giả lập DataStore chậm (bấm lặp). */
    var writeGate: CompletableDeferred<Unit>? = null

    override suspend fun setContentLanguage(language: String) = set { copy(contentLanguage = language) }

    override suspend fun setTasteTagIds(tagIds: Set<String>) = set { copy(tasteTagIds = tagIds) }

    override suspend fun setTheme(theme: ThemeMode) = set { copy(theme = theme) }

    override suspend fun setOnboardingDone(done: Boolean) = set { copy(onboardingDone = done) }

    override suspend fun setAutoplayPreviewWifiOnly(wifiOnly: Boolean) = set { copy(autoplayPreviewWifiOnly = wifiOnly) }

    override suspend fun setAutoplayNextEpisode(enabled: Boolean) = set { copy(autoplayNextEpisode = enabled) }

    private suspend fun set(block: UserPreferences.() -> UserPreferences): Result<Unit> {
        writeGate?.await()
        state.update(block)
        return Result.success(Unit)
    }
}

class FakeAdsConfigRepository(initial: AdsConfig = AdsConfig.DEFAULT) : AdsConfigRepository {
    override val config = MutableStateFlow(initial)
    override fun current(): AdsConfig = config.value
}

class FakeEpisodeUnlockRepository : EpisodeUnlockRepository {
    /** showId → index đã mở. */
    val unlocked = MutableStateFlow<Map<String, Set<Int>>>(emptyMap())

    override fun observeUnlocked(showId: String): Flow<Set<Int>> =
        unlocked.map { it[showId].orEmpty() }.distinctUntilChanged()

    override suspend fun unlock(showId: String, episodeIndex: Int): Result<Unit> {
        unlocked.update { it + (showId to (it[showId].orEmpty() + episodeIndex)) }
        return Result.success(Unit)
    }
}
