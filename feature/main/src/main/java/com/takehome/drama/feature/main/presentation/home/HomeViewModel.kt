package com.takehome.drama.feature.main.presentation.home

import androidx.lifecycle.viewModelScope
import com.takehome.drama.core.ui.foundation.error.toUiError
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.usecase.ads.ObserveAdPlacementUseCase
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.discovery.GetHomeFeedUseCase
import com.takehome.drama.domain.usecase.discovery.WallClock
import com.takehome.drama.domain.usecase.discovery.daypartAt
import com.takehome.drama.domain.usecase.discovery.upcoming
import com.takehome.drama.domain.usecase.history.GetContinueTargetUseCase
import com.takehome.drama.domain.usecase.history.ObserveWatchHistoryUseCase
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import com.takehome.drama.feature.main.presentation.common.toCard
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * SCR-0101 Day Cycle. Catalogue tải một lần; ngôn ngữ nội dung, gu và tiến độ Room là stream nên
 * đổi ngôn ngữ ở Player / xem dở một tập rồi quay lại thì Home tự cập nhật.
 */
class HomeViewModel(
    private val getCatalog: GetCatalogUseCase,
    private val getHomeFeed: GetHomeFeedUseCase,
    private val observeWatchHistory: ObserveWatchHistoryUseCase,
    private val getShowEpisodes: GetShowEpisodesUseCase,
    private val getContinueTarget: GetContinueTargetUseCase,
    private val resolveContentLanguage: ResolveContentLanguageUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val wallClock: WallClock,
    private val observeAdPlacement: ObserveAdPlacementUseCase,
) : BaseViewModel<HomeIntent, HomeViewState, HomeSingleEvent>() {

    private var feedJob: Job? = null

    override fun initState(): HomeViewState = HomeViewState().withClock()

    init {
        load()
        viewModelScope.launch {
            observeAdPlacement(AdPlacement.NativeHome).collect { enabled -> updateState { copy(showNativeAd = enabled) } }
        }
    }

    override fun processIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.ScreenStarted -> updateState { withClock() }
            HomeIntent.Retry -> if (!currentState.isLoading) load()
            is HomeIntent.OpenShow -> sendEvent(HomeSingleEvent.OpenShow(intent.showId))
            is HomeIntent.ContinueShow -> continueShow(intent.showId, intent.episodeIndex)
            HomeIntent.OpenSearch -> sendEvent(HomeSingleEvent.OpenSearch)
            HomeIntent.SeeAll -> sendEvent(HomeSingleEvent.OpenBrowse)
            HomeIntent.OpenBell -> sendEvent(HomeSingleEvent.OpenSettings)
        }
    }

    private fun HomeViewState.withClock(): HomeViewState {
        val now = wallClock.now()
        val daypart = daypartAt(now)
        return copy(clock = now, daypart = daypart, upcoming = daypart.upcoming())
    }

    /**
     * Bản ghi mới nhất có thể là tập đã xem hết (tắt autoplay, thoát ở end card) — mở thẳng nó thì
     * Player phát lại từ 0. Tải file tập (repo cache) để sang tập kế; lỗi thì mở tập đã lưu.
     */
    private fun continueShow(showId: String, savedEpisodeIndex: Int) {
        viewModelScope.launch {
            val target = getShowEpisodes(showId).getOrNull()
                ?.let { getContinueTarget(showId, it.episodes).episodeIndex }
                ?: savedEpisodeIndex
            sendEvent(HomeSingleEvent.OpenPlayer(showId, target))
        }
    }

    private fun load() {
        updateState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getCatalog()
                .onSuccess(::observeFeed)
                .onFailure { error -> updateState { copy(isLoading = false, error = error.toUiError()) } }
        }
    }

    private fun observeFeed(catalog: Catalog) {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            combine(userPreferencesRepository.preferences, observeWatchHistory(catalog)) { preferences, history ->
                preferences to history
            }.collect { (preferences, history) ->
                val language = resolveContentLanguage(preferences.contentLanguage, Locale.getDefault().language, catalog)
                val feed = getHomeFeed(catalog, preferences.tasteTagIds)
                updateState {
                    copy(
                        isLoading = false,
                        error = null,
                        hero = feed.hero?.toCard(catalog, language),
                        rails = feed.rails.map { rail ->
                            HomeRail(
                                title = rail.tag.name.resolveTagLabel(language, catalog.defaultLanguage),
                                shows = rail.shows.map { it.toCard(catalog, language) },
                            )
                        },
                        alsoFlowing = feed.alsoFlowing.map { it.toCard(catalog, language) },
                        continueRow = history.watching.map { item ->
                            val progress = item.progress
                            ContinueCard(
                                show = item.show.toCard(catalog, language),
                                episodeIndex = progress.episodeIndex,
                                episodeNumber = progress.episodeNumber,
                                progress = if (progress.durationMs > 0) {
                                    (progress.positionMs.toFloat() / progress.durationMs).coerceIn(0f, 1f)
                                } else {
                                    0f
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
