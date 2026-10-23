package com.takehome.drama.feature.watch.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.takehome.drama.core.ui.foundation.error.toErrorRes
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Episode
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.usecase.catalog.CatalogUseCase
import com.takehome.drama.domain.usecase.history.WatchProgressUseCase
import com.takehome.drama.domain.usecase.library.SavedShowsUseCase
import com.takehome.drama.domain.usecase.watch.EpisodeRange
import com.takehome.drama.domain.usecase.watch.EpisodesUseCase
import com.takehome.drama.feature.watch.R
import com.takehome.drama.feature.watch.navigation.ShowDetailScreenProvider.ShowDetail
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ShowDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val catalogUseCase: CatalogUseCase,
    private val episodesUseCase: EpisodesUseCase,
    private val watchProgressUseCase: WatchProgressUseCase,
    private val savedShowsUseCase: SavedShowsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : BaseViewModel<ShowDetailIntent, ShowDetailViewState, ShowDetailSingleEvent>() {

    private val showId = savedStateHandle.toRoute<ShowDetail>().showId

    private var catalog: Catalog? = null
    private var show: Show? = null
    private var episodes: List<Episode> = emptyList()
    private var progressJob: Job? = null
    private var languageJob: Job? = null

    override fun initState() = ShowDetailViewState()

    init {
        load()
        viewModelScope.launch {
            savedShowsUseCase.observeIsSaved(showId).collect { updateState { copy(isSaved = it) } }
        }
    }

    override fun processIntent(intent: ShowDetailIntent) {
        when (intent) {
            ShowDetailIntent.BackClick -> sendEvent(ShowDetailSingleEvent.NavigateBack)
            ShowDetailIntent.RetryClick -> load()
            ShowDetailIntent.RetryEpisodesClick -> retryEpisodes()
            ShowDetailIntent.ToggleSynopsis -> updateState { copy(synopsisExpanded = !synopsisExpanded) }
            ShowDetailIntent.ContinueClick -> currentState.continueCta?.let { openEpisode(it.episodeIndex) }
            is ShowDetailIntent.EpisodeClick -> openEpisode(intent.index)
            is ShowDetailIntent.RangeSelect -> scrollTo(intent.range)
            ShowDetailIntent.SaveClick -> toggleSaved()
        }
    }

    /** Catalogue và file tập tải song song; file tập lỗi không kéo header lỗi theo. */
    private fun load() {
        updateState { copy(isLoading = true, errorRes = null, isEpisodesLoading = true, episodesErrorRes = null) }
        viewModelScope.launch {
            val episodesResult = async { episodesUseCase.load(showId) }
            val catalog = catalogUseCase.load().getOrElse { error ->
                updateState { copy(isLoading = false, errorRes = error.toErrorRes()) }
                return@launch
            }
            val show = catalogUseCase.findShow(catalog, showId) ?: run {
                updateState { copy(isLoading = false, errorRes = R.string.watch_show_not_found) }
                return@launch
            }
            this@ShowDetailViewModel.catalog = catalog
            this@ShowDetailViewModel.show = show
            updateState {
                copy(
                    isLoading = false,
                    posterUrl = show.posterUrl,
                    episodeTotal = episodesUseCase.episodeTotal(show, episodes = null),
                    viewCount = show.viewCount,
                )
            }
            observeContentLanguage(catalog, show)
            applyEpisodes(show, episodesResult.await())
        }
    }

    /** Lỗi inline của lưới: chỉ tải lại file tập, header giữ nguyên. */
    private fun retryEpisodes() {
        val show = show ?: return load()
        updateState { copy(isEpisodesLoading = true, episodesErrorRes = null) }
        viewModelScope.launch { applyEpisodes(show, episodesUseCase.load(showId)) }
    }

    private fun applyEpisodes(show: Show, result: Result<ShowEpisodes>) {
        result
            .onSuccess { loaded ->
                episodes = loaded.episodes
                updateState {
                    copy(
                        isEpisodesLoading = false,
                        episodeTotal = episodesUseCase.episodeTotal(show, loaded),
                        ranges = episodesUseCase.ranges(episodes),
                    )
                }
                observeProgress()
            }
            .onFailure { error ->
                updateState { copy(isEpisodesLoading = false, episodesErrorRes = error.toErrorRes()) }
            }
    }

    /**
     * Ngôn ngữ nội dung là của cả app — đổi ở Player (0304) thì quay về Detail phải thấy ngay.
     * Chip chỉ lấy tag thể loại (`isCategory`); tag merchandising (Trending, Newest) không phải genre.
     */
    private fun observeContentLanguage(catalog: Catalog, show: Show) {
        languageJob?.cancel()
        languageJob = viewModelScope.launch {
            userPreferencesRepository.preferences
                .map { it.contentLanguage }
                .distinctUntilChanged()
                .collect { saved ->
                    val language = catalogUseCase.resolveContentLanguage(saved, Locale.getDefault().language, catalog)
                    updateState {
                        copy(
                            title = show.title.resolve(language, catalog.defaultLanguage),
                            tags = catalogUseCase.tagsOf(show, catalog)
                                .filter { it.isCategory }
                                .map { it.name.resolve(language, catalog.defaultLanguage) },
                            synopsis = show.synopsis.resolve(language, catalog.defaultLanguage),
                        )
                    }
                }
        }
    }

    /** Quay lại từ Player thì Room đổi → Continue và ô "đã xem" cập nhật theo. */
    private fun observeProgress() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            watchProgressUseCase.observeForShow(showId).collect { progress ->
                val target = episodes.takeIf { it.isNotEmpty() }?.let { watchProgressUseCase.continueTarget(showId, it) }
                val targetEpisode = target?.let { t -> episodes.firstOrNull { it.index == t.episodeIndex } }
                updateState {
                    copy(
                        // Tập kế bị khoá không được tô "đang phát": ô phải lõm + khoá để user biết.
                        cells = episodesUseCase.grid(
                            episodes,
                            progress,
                            currentIndex = target?.takeIf { it.hasProgress && targetEpisode?.isFree == true }?.episodeIndex,
                        ),
                        continueCta = targetEpisode?.let {
                            ContinueCta(
                                episodeIndex = it.index,
                                episodeNumber = it.displayNumber,
                                isResume = target.hasProgress,
                                isLocked = !it.isFree,
                            )
                        },
                    )
                }
            }
        }
    }

    private fun openEpisode(index: Int) {
        val episode = episodes.firstOrNull { it.index == index } ?: return
        if (episode.isFree) {
            sendEvent(ShowDetailSingleEvent.OpenPlayer(showId, index))
        } else {
            sendEvent(ShowDetailSingleEvent.ShowToast(R.string.watch_episode_locked, episode.displayNumber, success = false))
        }
    }

    private fun scrollTo(range: EpisodeRange) {
        sendEvent(ShowDetailSingleEvent.ScrollToEpisode(range.start))
    }

    private fun toggleSaved() {
        val save = !currentState.isSaved
        viewModelScope.launch {
            savedShowsUseCase.setSaved(showId, save).onSuccess {
                val message = if (save) R.string.watch_saved_to_library else R.string.watch_removed_from_library
                sendEvent(ShowDetailSingleEvent.ShowToast(message, formatArg = null, success = true))
            }.onFailure { error ->
                sendEvent(ShowDetailSingleEvent.ShowToast(error.toErrorRes(), formatArg = null, success = false))
            }
        }
    }
}
