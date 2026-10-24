package com.takehome.drama.feature.watch.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.takehome.drama.core.ads.reward.RewardAdManager
import com.takehome.drama.core.ui.foundation.error.ErrorKind
import com.takehome.drama.core.ui.foundation.error.UiError
import com.takehome.drama.core.ui.foundation.error.toErrorRes
import com.takehome.drama.core.ui.foundation.error.toUiError
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Episode
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.episodeTotal
import com.takehome.drama.domain.model.findShow
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.model.tagsOf
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.usecase.ads.IsAdPlacementEnabledUseCase
import com.takehome.drama.domain.usecase.ads.ObserveAdPlacementUseCase
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.history.GetContinueTargetUseCase
import com.takehome.drama.domain.usecase.history.ObserveShowProgressUseCase
import com.takehome.drama.domain.usecase.library.ObserveIsShowSavedUseCase
import com.takehome.drama.domain.usecase.library.SetShowSavedUseCase
import com.takehome.drama.domain.usecase.watch.EpisodeRange
import com.takehome.drama.domain.usecase.watch.GetEpisodeGridUseCase
import com.takehome.drama.domain.usecase.watch.GetEpisodeRangesUseCase
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import com.takehome.drama.domain.usecase.watch.ObserveUnlockedEpisodesUseCase
import com.takehome.drama.domain.usecase.watch.UnlockEpisodeUseCase
import com.takehome.drama.domain.usecase.watch.isWatchable
import com.takehome.drama.feature.watch.R
import com.takehome.drama.feature.watch.navigation.ShowDetailScreenProvider.ShowDetail
import com.takehome.drama.feature.watch.presentation.common.RewardedAdState
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ShowDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val getCatalog: GetCatalogUseCase,
    private val getShowEpisodes: GetShowEpisodesUseCase,
    private val resolveContentLanguage: ResolveContentLanguageUseCase,
    private val getEpisodeGrid: GetEpisodeGridUseCase,
    private val getEpisodeRanges: GetEpisodeRangesUseCase,
    private val observeShowProgress: ObserveShowProgressUseCase,
    private val getContinueTarget: GetContinueTargetUseCase,
    private val observeIsShowSaved: ObserveIsShowSavedUseCase,
    private val setShowSaved: SetShowSavedUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val observeUnlockedEpisodes: ObserveUnlockedEpisodesUseCase,
    private val unlockEpisode: UnlockEpisodeUseCase,
    private val isAdPlacementEnabled: IsAdPlacementEnabledUseCase,
    observeAdPlacement: ObserveAdPlacementUseCase,
    private val rewardAdManager: RewardAdManager,
) : BaseViewModel<ShowDetailIntent, ShowDetailViewState, ShowDetailSingleEvent>() {

    private val showId = savedStateHandle.toRoute<ShowDetail>().showId

    private var catalog: Catalog? = null
    private var show: Show? = null
    private var episodes: List<Episode> = emptyList()
    private var rangePickedByUser = false
    private var progressJob: Job? = null
    private var languageJob: Job? = null
    private var rewardedLoadJob: Job? = null

    /** Tập khoá đã mở bằng rewarded (`Episode.index`). */
    private var unlocked: Set<Int> = emptySet()

    /** Đã gửi [ShowDetailSingleEvent.ShowRewarded], chờ kết quả — chặn bấm "Watch ad" lặp. */
    private var rewardedShowing = false
    private var rewardedPreloaded = false

    override fun initState() = ShowDetailViewState()

    init {
        load()
        viewModelScope.launch {
            observeIsShowSaved(showId).collect { updateState { copy(isSaved = it) } }
        }
        // Giá trị đầu = trạng thái lúc mở (đã xét khi load); chỉ phản ứng khi Remote Config đổi sau đó.
        viewModelScope.launch { observeAdPlacement(AdPlacement.RewardedBonus).drop(1).collect(::onRewardedPlacementChanged) }
    }

    override fun processIntent(intent: ShowDetailIntent) {
        when (intent) {
            ShowDetailIntent.BackClick -> sendEvent(ShowDetailSingleEvent.NavigateBack)
            ShowDetailIntent.RetryClick -> if (!currentState.isLoading) load()
            ShowDetailIntent.RetryEpisodesClick -> retryEpisodes()
            ShowDetailIntent.ToggleSynopsis -> updateState { copy(synopsisExpanded = !synopsisExpanded) }
            ShowDetailIntent.ContinueClick -> currentState.continueCta?.let { openEpisode(it.episodeIndex) }
            is ShowDetailIntent.EpisodeClick -> openEpisode(intent.index)
            is ShowDetailIntent.RangeSelect -> selectRange(intent.range)
            ShowDetailIntent.SaveClick -> toggleSaved()
            ShowDetailIntent.UnlockWithAdClick -> showRewarded()
            ShowDetailIntent.UnlockSheetDismiss -> closeUnlockSheet()
            is ShowDetailIntent.RewardResult -> onRewardResult(intent.episodeIndex, intent.earned)
        }
    }

    /** Catalogue và file tập tải song song; file tập lỗi không kéo header lỗi theo. */
    private fun load() {
        updateState { copy(isLoading = true, error = null, isEpisodesLoading = true, episodesErrorRes = null) }
        viewModelScope.launch {
            val episodesResult = async { getShowEpisodes(showId) }
            val catalog = getCatalog().getOrElse { error ->
                updateState { copy(isLoading = false, error = error.toUiError()) }
                return@launch
            }
            val show = catalog.findShow(showId) ?: run {
                updateState { copy(isLoading = false, error = UiError(ErrorKind.NotFound, R.string.watch_show_not_found)) }
                return@launch
            }
            this@ShowDetailViewModel.catalog = catalog
            this@ShowDetailViewModel.show = show
            updateState {
                copy(
                    isLoading = false,
                    posterUrl = show.posterUrl,
                    episodeTotal = show.episodeTotal(episodes = null),
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
        viewModelScope.launch { applyEpisodes(show, getShowEpisodes(showId)) }
    }

    private fun applyEpisodes(show: Show, result: Result<ShowEpisodes>) {
        result
            .onSuccess { loaded ->
                episodes = loaded.episodes
                updateState {
                    copy(
                        isEpisodesLoading = false,
                        episodeTotal = show.episodeTotal(loaded),
                        ranges = getEpisodeRanges(episodes),
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
                    val language = resolveContentLanguage(saved, Locale.getDefault().language, catalog)
                    updateState {
                        copy(
                            title = show.title.resolve(language, catalog.defaultLanguage),
                            tags = catalog.tagsOf(show)
                                .filter { it.isCategory }
                                // Cùng cách hiển thị tên tag như Home/Browse ("SweetRomance", "رومانسية_حلوة" → có dấu cách).
                                .map { it.name.resolveTagLabel(language, catalog.defaultLanguage) },
                            synopsis = show.synopsis.resolve(language, catalog.defaultLanguage),
                        )
                    }
                }
        }
    }

    /** Quay lại từ Player thì Room đổi → Continue và ô "đã xem" cập nhật theo; mở tập ở Player cũng vậy. */
    private fun observeProgress() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            combine(observeShowProgress(showId), observeUnlockedEpisodes(showId)) { progress, unlocked -> progress to unlocked }
                .collect { (progress, unlocked) ->
                    this@ShowDetailViewModel.unlocked = unlocked
                    preloadRewardedIfLocked()
                    val target = episodes.takeIf { it.isNotEmpty() }?.let { getContinueTarget(showId, it) }
                    val targetEpisode = target?.let { t -> episodes.firstOrNull { it.index == t.episodeIndex } }
                    updateState {
                        copy(
                            // User đã tự chọn tab thì giữ; không thì tab chứa tập Continue.
                            selectedRange = if (rangePickedByUser) selectedRange else getEpisodeRanges.rangeOf(ranges, episodes, target?.episodeIndex ?: -1),
                            watchedCount = getEpisodeGrid.watchedCount(episodes, progress),
                            // Tập kế bị khoá không được tô "đang phát": ô phải lõm + khoá để user biết.
                            cells = getEpisodeGrid(
                                episodes,
                                progress,
                                currentIndex = target?.takeIf { it.hasProgress && targetEpisode?.isWatchable(unlocked) == true }?.episodeIndex,
                                unlocked = unlocked,
                            ),
                            continueCta = targetEpisode?.let {
                                ContinueCta(
                                    episodeIndex = it.index,
                                    episodeNumber = it.displayNumber,
                                    isResume = target.hasProgress,
                                    isLocked = !it.isWatchable(unlocked),
                                )
                            },
                        )
                    }
                }
        }
    }

    /**
     * Tập khoá: `rewarded_bonus` bật → sheet mời xem quảng cáo (ad load ngay sau sheet, user chưa phải chờ gì);
     * tắt → toast như Phase 3. Rewarded chỉ hiện khi user bấm nút trong sheet, không bao giờ tự mở.
     */
    private fun openEpisode(index: Int) {
        val episode = episodes.firstOrNull { it.index == index } ?: return
        when {
            episode.isWatchable(unlocked) -> sendEvent(ShowDetailSingleEvent.OpenPlayer(showId, index))
            isAdPlacementEnabled(AdPlacement.RewardedBonus) -> openUnlockSheet(episode)
            else -> sendEvent(ShowDetailSingleEvent.ShowToast(R.string.watch_episode_locked, episode.displayNumber, success = false))
        }
    }

    /**
     * Show còn tập khoá → load rewarded ngay khi mở Detail, để lúc user chạm tập khoá ad thường đã sẵn (không phải
     * chờ spinner trong sheet). Một lần cho tới khi ad được dùng.
     */
    private fun preloadRewardedIfLocked() {
        if (rewardedPreloaded || !isAdPlacementEnabled(AdPlacement.RewardedBonus) || episodes.all { it.isWatchable(unlocked) }) return
        rewardedPreloaded = true
        rewardAdManager.preload(RewardAdManager.AdType.UNLOCK_EPISODE)
    }

    /**
     * Remote Config đổi khi đang ở Detail: bật → load sẵn; tắt khi sheet đang mở (chưa show ad) → đóng sheet, toast
     * "tập bị khoá" như lúc placement tắt từ đầu — không để sheet treo "No ad available".
     */
    private fun onRewardedPlacementChanged(enabled: Boolean) {
        if (enabled) {
            preloadRewardedIfLocked()
            return
        }
        val sheet = currentState.unlockSheet ?: return
        if (rewardedShowing) return
        closeUnlockSheet()
        sendEvent(ShowDetailSingleEvent.ShowToast(R.string.watch_episode_locked, sheet.episodeNumber, success = false))
    }

    private fun openUnlockSheet(episode: Episode) {
        rewardedShowing = false
        updateState { copy(unlockSheet = UnlockSheet(episode.index, episode.displayNumber, RewardedAdState.Loading)) }
        rewardedLoadJob?.cancel()
        rewardedLoadJob = viewModelScope.launch {
            val available = rewardAdManager.load(RewardAdManager.AdType.UNLOCK_EPISODE)
            updateState {
                val sheet = unlockSheet?.takeIf { it.episodeIndex == episode.index } ?: return@updateState this
                copy(unlockSheet = sheet.copy(ad = if (available) RewardedAdState.Ready else RewardedAdState.Unavailable))
            }
        }
    }

    private fun showRewarded() {
        val sheet = currentState.unlockSheet?.takeIf { it.ad == RewardedAdState.Ready } ?: return
        if (rewardedShowing) return
        rewardedShowing = true
        sendEvent(ShowDetailSingleEvent.ShowRewarded(sheet.episodeIndex))
    }

    private fun closeUnlockSheet() {
        rewardedLoadJob?.cancel()
        rewardedShowing = false
        updateState { copy(unlockSheet = null) }
    }

    private fun onRewardResult(index: Int, earned: Boolean) {
        closeUnlockSheet()
        // Ad vừa xem đã dùng hết — load sẵn bản kế cho tập khoá tiếp theo.
        rewardedPreloaded = false
        preloadRewardedIfLocked()
        val episode = episodes.firstOrNull { it.index == index } ?: return
        if (!earned) {
            sendEvent(ShowDetailSingleEvent.ShowToast(R.string.watch_unlock_incomplete, formatArg = null, success = false))
            return
        }
        viewModelScope.launch {
            unlockEpisode(showId, index).onSuccess {
                sendEvent(ShowDetailSingleEvent.ShowToast(R.string.watch_unlock_success, episode.displayNumber, success = true))
                sendEvent(ShowDetailSingleEvent.OpenPlayer(showId, index))
            }.onFailure { error ->
                sendEvent(ShowDetailSingleEvent.ShowToast(error.toErrorRes(), formatArg = null, success = false))
            }
        }
    }

    private fun selectRange(range: EpisodeRange) {
        rangePickedByUser = true
        updateState { copy(selectedRange = range) }
    }

    private fun toggleSaved() {
        val save = !currentState.isSaved
        viewModelScope.launch {
            setShowSaved(showId, save).onSuccess {
                val message = if (save) R.string.watch_saved_to_library else R.string.watch_removed_from_library
                sendEvent(ShowDetailSingleEvent.ShowToast(message, formatArg = null, success = true))
            }.onFailure { error ->
                sendEvent(ShowDetailSingleEvent.ShowToast(error.toErrorRes(), formatArg = null, success = false))
            }
        }
    }
}
