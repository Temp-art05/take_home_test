package com.takehome.drama.feature.watch.presentation.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.takehome.drama.core.ads.reward.RewardAdManager
import com.takehome.drama.core.media.EpisodeFeedPlayer
import com.takehome.drama.core.media.EpisodeFeedPlayerFactory
import com.takehome.drama.core.media.FeedItem
import com.takehome.drama.core.media.PlaybackProgress
import com.takehome.drama.core.media.PlaybackSpeed
import com.takehome.drama.core.media.PlaybackState
import com.takehome.drama.core.media.progress
import com.takehome.drama.core.ui.foundation.error.toErrorRes
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Episode
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.WatchProgress
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.model.findShow
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.usecase.ads.IsAdPlacementEnabledUseCase
import com.takehome.drama.domain.usecase.ads.ObserveAdPlacementUseCase
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.history.GetResumePositionUseCase
import com.takehome.drama.domain.usecase.history.ObserveShowProgressUseCase
import com.takehome.drama.domain.usecase.history.SaveWatchProgressUseCase
import com.takehome.drama.domain.usecase.watch.EndAction
import com.takehome.drama.domain.usecase.watch.GetEndActionUseCase
import com.takehome.drama.domain.usecase.watch.GetEpisodeGridUseCase
import com.takehome.drama.domain.usecase.watch.GetEpisodeRangesUseCase
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import com.takehome.drama.domain.usecase.watch.ObserveUnlockedEpisodesUseCase
import com.takehome.drama.domain.usecase.watch.PickVideoSourceUseCase
import com.takehome.drama.domain.usecase.watch.PlaybackQualityPolicy
import com.takehome.drama.domain.usecase.watch.UnlockEpisodeUseCase
import com.takehome.drama.domain.usecase.watch.isWatchable
import com.takehome.drama.feature.watch.R
import com.takehome.drama.feature.watch.navigation.PlayerScreenProvider.Player
import com.takehome.drama.feature.watch.presentation.common.RewardedAdState
import com.takehome.drama.feature.watch.presentation.player.model.PlayerPage
import com.takehome.drama.feature.watch.presentation.player.model.PlayerSheet
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

/**
 * ViewModel **sở hữu** [EpisodeFeedPlayer]: xoay máy không dựng lại player, không rebuffer, không mất
 * vị trí. Đổi tập (vuốt pager, sheet, autoplay) diễn ra trong VM (không navigate) nên tốc độ/quality
 * giữ nguyên; VM là nguồn sự thật của tập hiện tại, pager chỉ báo `PageSettled` và cuộn theo
 * `ScrollToEpisode`. Tập, tốc độ, quality nằm trong [SavedStateHandle] → sống qua process death; vị
 * trí phát lấy lại từ Room.
 */
class PlayerViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getCatalog: GetCatalogUseCase,
    private val getShowEpisodes: GetShowEpisodesUseCase,
    private val resolveContentLanguage: ResolveContentLanguageUseCase,
    private val pickVideoSource: PickVideoSourceUseCase,
    private val getEpisodeGrid: GetEpisodeGridUseCase,
    private val getEpisodeRanges: GetEpisodeRangesUseCase,
    private val getEndAction: GetEndActionUseCase,
    private val getResumePosition: GetResumePositionUseCase,
    private val saveWatchProgress: SaveWatchProgressUseCase,
    private val observeShowProgress: ObserveShowProgressUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    playerFactory: EpisodeFeedPlayerFactory,
    private val observeUnlockedEpisodes: ObserveUnlockedEpisodesUseCase,
    private val unlockEpisode: UnlockEpisodeUseCase,
    private val isAdPlacementEnabled: IsAdPlacementEnabledUseCase,
    observeAdPlacement: ObserveAdPlacementUseCase,
    private val rewardAdManager: RewardAdManager,
) : BaseViewModel<PlayerIntent, PlayerViewState, PlayerSingleEvent>() {

    private val route = savedStateHandle.toRoute<Player>()
    private val showId = route.showId

    val player: EpisodeFeedPlayer = playerFactory.create()

    /**
     * Tiến độ tách khỏi [viewState]: chỉ thanh track đọc, mỗi tick không recompose cả màn. Gắn với tập nó đo
     * ([PlaybackProgress]) để lật sang tập mới không vẽ vị trí tập trước.
     */
    val playbackProgress: StateFlow<PlaybackProgress> = player.state
        .map { it.progress }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlaybackProgress())

    private var catalog: Catalog? = null
    private var show: Show? = null
    private var episodes: List<Episode> = emptyList()
    private var progress: List<WatchProgress> = emptyList()

    /** Like cục bộ theo tập (không có API) — vuốt đi rồi quay lại vẫn còn. */
    private val likedEpisodes = mutableSetOf<Int>()
    private var contentLanguage: String = ""

    /** Quality của nguồn đang nạp (Auto thì là quality `pickSource` chọn). */
    private var playingQuality: String? = null

    /** Trần quality do Auto tự hạ khi mạng chậm; null = không hạ. */
    private var autoQualityCap: String? = null
    private var lastSavedPositionMs: Long? = null
    private var wasPlayingBeforeStop = false

    /** Màn đang hiện (giữa ON_START và ON_STOP). Tập nạp xong khi đang ở nền thì không được phát tiếng. */
    private var isScreenStarted = true
    private var openJob: Job? = null
    private var stallJob: Job? = null
    private var observersStarted = false
    private var rewardedLoadJob: Job? = null

    /** Tập khoá đã mở bằng rewarded (`Episode.index`). Ở đây mọi quyết định "xem được không" đi qua [isWatchable]. */
    private var unlocked: Set<Int> = emptySet()

    /** Đã gửi [PlayerSingleEvent.ShowRewarded], chờ kết quả — chặn bấm lặp. */
    private var rewardedShowing = false

    private val currentEpisode: Episode?
        get() = episodes.firstOrNull { it.index == currentState.episodeIndex }

    override fun initState() = PlayerViewState(
        episodeIndex = savedStateHandle[KEY_EPISODE_INDEX] ?: route.episodeIndex,
        speed = savedStateHandle[KEY_SPEED] ?: 1f,
        quality = savedStateHandle[KEY_QUALITY],
    )

    init {
        player.setSpeed(currentState.speed)
        viewModelScope.launch { player.state.collect(::onPlayback) }
        // Giá trị đầu = trạng thái lúc mở (load() tự xét); chỉ phản ứng khi Remote Config đổi sau đó.
        viewModelScope.launch { observeAdPlacement(AdPlacement.RewardedBonus).drop(1).collect(::onRewardedPlacementChanged) }
        load()
    }

    override fun processIntent(intent: PlayerIntent) {
        when (intent) {
            PlayerIntent.BackClick -> navigateBack()
            PlayerIntent.RetryClick -> retry()
            PlayerIntent.TogglePlay -> togglePlay()
            is PlayerIntent.SeekTo -> seekTo(intent.positionMs)
            PlayerIntent.LikeClick -> toggleLike()
            PlayerIntent.SpeedClick -> setSpeed(PlaybackSpeed.next(currentState.speed))
            PlayerIntent.SpeedLongPress -> updateState { copy(sheet = PlayerSheet.Options) }
            PlayerIntent.EpisodesClick -> openEpisodeSheet()
            PlayerIntent.DismissSheet -> updateState { copy(sheet = PlayerSheet.None) }
            is PlayerIntent.RangeSelect -> updateState { copy(selectedRange = intent.range) }
            is PlayerIntent.EpisodeSelect -> selectEpisode(intent.index)
            is PlayerIntent.SpeedSelect -> setSpeed(intent.speed)
            is PlayerIntent.QualitySelect -> selectQuality(intent.quality)
            is PlayerIntent.LanguageSelect -> selectLanguage(intent.language)
            PlayerIntent.AutoplayToggle -> toggleAutoplay()
            PlayerIntent.ToggleDescription -> updateState { copy(descriptionExpanded = !descriptionExpanded) }
            PlayerIntent.SwitchBackToHd -> switchBackToHd()
            PlayerIntent.UnlockWithAdClick -> showRewarded()
            is PlayerIntent.RewardResult -> onRewardResult(intent.episodeIndex, intent.earned)
            is PlayerIntent.PageSettled -> onPageSettled(intent.index)
            PlayerIntent.ScreenStarted -> onScreenStarted()
            is PlayerIntent.ScreenStopped -> if (!intent.isChangingConfigurations) onScreenStopped()
        }
    }

    private fun load() {
        updateState { copy(isLoading = true, errorRes = null) }
        viewModelScope.launch {
            val episodesResult = async { getShowEpisodes(showId) }
            val loadedCatalog = getCatalog().getOrElse { return@launch fail(it.toErrorRes()) }
            val loadedShow = loadedCatalog.findShow(showId)
                ?: return@launch fail(R.string.watch_show_not_found)
            val loaded = episodesResult.await().getOrElse { return@launch fail(it.toErrorRes()) }
            if (loaded.episodes.isEmpty()) return@launch fail(R.string.watch_error_no_source)

            val preferences = userPreferencesRepository.preferences.first()
            unlocked = observeUnlockedEpisodes(showId).first()
            catalog = loadedCatalog
            show = loadedShow
            episodes = loaded.episodes
            preloadRewardedIfLocked()
            contentLanguage = resolveContentLanguage(
                saved = preferences.contentLanguage,
                deviceLanguage = Locale.getDefault().language,
                catalog = loadedCatalog,
            )
            updateState {
                copy(
                    isLoading = false,
                    posterUrl = loadedShow.posterUrl,
                    episodeTotal = episodes.size,
                    ranges = getEpisodeRanges(episodes),
                    autoplayNext = preferences.autoplayNextEpisode,
                )
            }
            applyShowTexts()
            refreshPages()
            startObservers()
            refreshPlaylist()
            openEpisode(currentState.episodeIndex)
        }
    }

    private fun fail(errorRes: Int) {
        updateState { copy(isLoading = false, errorRes = errorRes) }
    }

    private fun startObservers() {
        if (observersStarted) return
        observersStarted = true
        viewModelScope.launch {
            userPreferencesRepository.preferences.collect { updateState { copy(autoplayNext = it.autoplayNextEpisode) } }
        }
        viewModelScope.launch {
            observeShowProgress(showId).collect {
                progress = it
                refreshGrid()
                refreshPages()
            }
        }
    }

    /**
     * Mở một tập trong player hiện có. Tập khoá (vuốt tới, hoặc vào từ route) không nạp.
     * [positionMs] null = vị trí resume đã lưu (chỉ tập vào màn — Continue từ Detail); đổi tập trong
     * feed truyền 0. Mở tập mới huỷ lần mở trước còn dở — không thì lần
     * xong sau thắng và player phát nhầm tập.
     */
    private fun openEpisode(index: Int, positionMs: Long? = null) {
        val episode = episodes.firstOrNull { it.index == index } ?: episodes.first()
        openJob?.cancel()
        savedStateHandle[KEY_EPISODE_INDEX] = episode.index
        lastSavedPositionMs = null
        stallJob?.cancel()
        updateState {
            copy(
                episodeIndex = episode.index,
                episodeNumber = episode.displayNumber,
                isLocked = !episode.isWatchable(),
                unlockAd = null,
                likeCount = episode.likeCount + if (episode.index in likedEpisodes) 1 else 0,
                isLiked = episode.index in likedEpisodes,
                endAction = null,
                errorRes = null,
                selectedRange = getEpisodeRanges.rangeOf(ranges, episodes, episode.index),
                languages = episode.sources.filterValues { it.isNotEmpty() }.keys.toList(),
            )
        }
        refreshGrid()
        val position = episodes.indexOf(episode)
        if (!episode.isWatchable()) {
            // Dừng tập trước, giữ cửa sổ preload quanh trang khoá để vuốt tiếp vẫn nhanh.
            player.setCurrent(position, startPositionMs = 0, playWhenReady = false)
            offerRewardedUnlock(episode.index)
            return
        }
        openJob = viewModelScope.launch {
            val start = positionMs ?: getResumePosition(showId, episode.index)
            startPlayback(episode, start, playWhenReady = isScreenStarted)
            // Vào nền lúc còn đang nạp: quay lại thì phát như user mong đợi.
            if (!isScreenStarted) wasPlayingBeforeStop = true
        }
    }

    /** URL của mọi tập theo ngôn ngữ nội dung + quality (hoặc trần Auto); tập khoá không có URL. */
    private fun refreshPlaylist() {
        val defaultLanguage = catalog?.defaultLanguage ?: return
        val preferred = currentState.quality ?: autoQualityCap
        player.setPlaylist(
            episodes.map { episode ->
                val url = if (episode.isWatchable()) {
                    pickVideoSource(episode, contentLanguage, defaultLanguage, preferred)?.url
                } else {
                    null
                }
                FeedItem(id = episode.index.toString(), url = url)
            },
        )
    }

    private fun startPlayback(episode: Episode, positionMs: Long, playWhenReady: Boolean) {
        val defaultLanguage = catalog?.defaultLanguage ?: return
        val preferred = currentState.quality ?: autoQualityCap
        val source = pickVideoSource(episode, contentLanguage, defaultLanguage, preferred)
            ?: return fail(R.string.watch_error_no_source)
        playingQuality = source.quality
        updateState {
            copy(
                language = source.language,
                qualities = pickVideoSource.qualitiesOf(episode, contentLanguage, defaultLanguage),
            )
        }
        player.setCurrent(episodes.indexOf(episode), positionMs, playWhenReady)
    }

    /** Ngôn ngữ / quality đổi: dựng lại URL cả playlist rồi nạp tập hiện tại tại vị trí đang phát. */
    private fun reloadAtCurrentPosition(playWhenReady: Boolean = player.state.value.playWhenReady) {
        // Đọc vị trí trước: playlist mới đổi URL tập đang phát → player giải phóng bản cũ, vị trí về 0.
        val positionMs = player.currentPositionMs()
        refreshPlaylist()
        val episode = currentEpisode?.takeIf { it.isWatchable() } ?: return
        startPlayback(episode, positionMs, playWhenReady && isScreenStarted)
    }

    private fun onPlayback(playback: PlaybackState) {
        val wasEnded = currentState.playback.ended
        updateState { copy(playback = playback.copy(positionMs = 0, bufferedMs = 0)) }
        if (playback.isPlaying && playback.durationMs > 0) {
            val last = lastSavedPositionMs
            if (last == null || abs(playback.positionMs - last) >= SAVE_INTERVAL_MS) {
                saveProgress(playback.positionMs, playback.durationMs)
            }
        }
        if (playback.ended && !wasEnded) onEnded(playback)
        trackStall(playback)
    }

    private fun onEnded(playback: PlaybackState) {
        saveProgress(playback.durationMs, playback.durationMs)
        val action = getEndAction(episodes, currentState.episodeIndex, currentState.autoplayNext, unlocked)
        if (action is EndAction.PlayNext) {
            // Chỉ cuộn; tập kế mở khi trang dừng (PageSettled) như user tự vuốt. Đổi tập trước khi cuộn
            // thì trang cũ (player bị tua về 0) và top bar đổi giữa lúc trượt → flash.
            sendEvent(PlayerSingleEvent.ScrollToEpisode(action.index))
        } else {
            updateState { copy(endAction = action) }
        }
    }

    /** Buffer quá ngưỡng khi đang muốn phát → Auto hạ xuống quality thấp hơn có thật. */
    private fun trackStall(playback: PlaybackState) {
        if (!(playback.isBuffering && playback.playWhenReady)) {
            stallJob?.cancel()
            stallJob = null
            return
        }
        if (stallJob?.isActive == true) return
        stallJob = viewModelScope.launch {
            delay(STALL_CHECK_MS)
            if (PlaybackQualityPolicy.shouldDowngrade(isAuto = currentState.quality == null, bufferingMs = STALL_CHECK_MS)) {
                downgradeQuality()
            }
        }
    }

    private fun downgradeQuality() {
        val current = playingQuality ?: return
        val target = PlaybackQualityPolicy.downgradeTarget(current, currentState.qualities) ?: return
        autoQualityCap = target
        updateState { copy(qualityDropped = target) }
        reloadAtCurrentPosition(playWhenReady = true)
    }

    private fun switchBackToHd() {
        autoQualityCap = null
        updateState { copy(qualityDropped = null) }
        reloadAtCurrentPosition()
    }

    /** [final] = lần lưu khi rời/ẩn màn: không để `onCleared` huỷ giữa chừng. */
    private fun saveProgress(positionMs: Long, durationMs: Long, final: Boolean = false) {
        val episode = currentEpisode ?: return
        if (durationMs <= 0 || !episode.isWatchable()) return
        lastSavedPositionMs = positionMs
        val record = WatchProgress(
            showId = showId,
            episodeIndex = episode.index,
            episodeNumber = episode.displayNumber,
            episodeTotal = episodes.last().index + 1,
            positionMs = positionMs.coerceAtMost(durationMs),
            durationMs = durationMs,
            updatedAt = System.currentTimeMillis(),
        )
        if (final) {
            viewModelScope.launch(NonCancellable) { saveWatchProgress(record) }
        } else {
            viewModelScope.launch { saveWatchProgress(record) }
        }
    }

    private fun saveCurrentProgress(final: Boolean = false) {
        saveProgress(player.currentPositionMs(), player.state.value.durationMs, final)
    }

    private fun onScreenStopped() {
        isScreenStarted = false
        wasPlayingBeforeStop = player.state.value.playWhenReady || openJob?.isActive == true
        player.pause()
        player.setPreloadActive(false)
        saveCurrentProgress(final = true)
    }

    /** Chỉ phát lại khi trước lúc vào nền đang phát — user đã pause thì giữ pause. */
    private fun onScreenStarted() {
        isScreenStarted = true
        player.setPreloadActive(true)
        if (wasPlayingBeforeStop) player.play()
        wasPlayingBeforeStop = false
    }

    /**
     * User vuốt tới tập khác: lưu chỗ tập cũ dừng rồi phát tập mới **từ đầu** như một feed (player lân
     * cận đã đứng sẵn ở giây 0 → lên hình tức thì). Tiến độ vẫn lưu cho Continue / ô đã xem.
     */
    private fun onPageSettled(index: Int) {
        if (index == currentState.episodeIndex || episodes.none { it.index == index }) return
        saveCurrentProgress()
        updateState { copy(sheet = PlayerSheet.None) }
        // Vuốt / chọn lại tập xem dở → xem tiếp đúng chỗ (đề §2.2: "mở lại tập là xem tiếp"); xem hết thì từ đầu.
        openEpisode(index)
    }

    private fun togglePlay() {
        if (currentState.isLocked) return
        if (player.state.value.playWhenReady) {
            player.pause()
            saveCurrentProgress()
        } else {
            updateState { copy(endAction = null) }
            player.play()
        }
    }

    private fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        updateState { copy(endAction = null) }
    }

    private fun toggleLike() {
        val index = currentState.episodeIndex
        if (!likedEpisodes.add(index)) likedEpisodes.remove(index)
        updateState { copy(isLiked = !isLiked, likeCount = likeCount + if (isLiked) -1 else 1) }
        refreshPages()
    }

    private fun setSpeed(speed: Float) {
        savedStateHandle[KEY_SPEED] = speed
        player.setSpeed(speed)
        updateState { copy(speed = speed) }
    }

    private fun openEpisodeSheet() {
        updateState {
            copy(
                sheet = PlayerSheet.Episodes,
                selectedRange = getEpisodeRanges.rangeOf(ranges, episodes, episodeIndex),
            )
        }
    }

    private fun selectEpisode(index: Int) {
        val episode = episodes.firstOrNull { it.index == index } ?: return
        if (!episode.isWatchable()) {
            if (isAdPlacementEnabled(AdPlacement.RewardedBonus)) {
                // Tới trang khoá của tập đó — nút mở bằng rewarded nằm ở đấy, user tự quyết có bấm không.
                updateState { copy(sheet = PlayerSheet.None) }
                saveCurrentProgress()
                sendEvent(PlayerSingleEvent.ScrollToEpisode(index))
            } else {
                sendEvent(PlayerSingleEvent.ShowToast(R.string.watch_episode_locked, episode.displayNumber, success = false))
            }
            return
        }
        updateState { copy(sheet = PlayerSheet.None) }
        if (index == currentState.episodeIndex) {
            // Cùng trang, không cuộn: chỉ phát lại khi đã hết tập.
            if (currentState.playback.ended) openEpisode(index, positionMs = 0)
            return
        }
        saveCurrentProgress()
        // Chỉ cuộn; tập mở ở PageSettled như vuốt tay / tự sang tập (xem tiếp từ vị trí đã lưu).
        sendEvent(PlayerSingleEvent.ScrollToEpisode(index))
    }

    private fun selectQuality(quality: String?) {
        savedStateHandle[KEY_QUALITY] = quality
        autoQualityCap = null
        updateState { copy(quality = quality, qualityDropped = null) }
        reloadAtCurrentPosition()
    }

    /** Ngôn ngữ nội dung là của cả app (title, audio) — ghi lại để màn khác cũng đổi theo. */
    private fun selectLanguage(language: String) {
        if (language == currentState.language) return
        contentLanguage = language
        viewModelScope.launch { userPreferencesRepository.setContentLanguage(language) }
        applyShowTexts()
        reloadAtCurrentPosition()
    }

    private fun toggleAutoplay() {
        val enabled = !currentState.autoplayNext
        updateState { copy(autoplayNext = enabled) }
        viewModelScope.launch { userPreferencesRepository.setAutoplayNextEpisode(enabled) }
    }

    private fun retry() {
        if (episodes.isEmpty() || catalog == null) {
            load()
        } else {
            updateState { copy(errorRes = null) }
            reloadAtCurrentPosition(playWhenReady = true)
        }
    }

    private fun navigateBack() {
        saveCurrentProgress(final = true)
        sendEvent(PlayerSingleEvent.NavigateBack)
    }

    private fun applyShowTexts() {
        val show = show ?: return
        val defaultLanguage = catalog?.defaultLanguage ?: return
        updateState {
            copy(
                showTitle = show.title.resolve(contentLanguage, defaultLanguage),
                description = show.hook.resolve(contentLanguage, defaultLanguage)
                    .ifBlank { show.synopsis.resolve(contentLanguage, defaultLanguage) },
            )
        }
    }

    private fun refreshPages() {
        updateState {
            copy(
                pages = episodes.map { episode ->
                    val record = progress.firstOrNull { it.episodeIndex == episode.index }
                    val liked = episode.index in likedEpisodes
                    PlayerPage(
                        episodeIndex = episode.index,
                        isLocked = !episode.isWatchable(),
                        episodeNumber = episode.displayNumber,
                        likeCount = episode.likeCount + if (liked) 1 else 0,
                        isLiked = liked,
                        durationMs = record?.durationMs ?: 0,
                    )
                },
            )
        }
    }

    private fun refreshGrid() {
        updateState {
            copy(
                cells = getEpisodeGrid(episodes, progress, currentIndex = episodeIndex, unlocked = unlocked),
                watchedCount = getEpisodeGrid.watchedCount(episodes, progress),
            )
        }
    }

    private fun Episode.isWatchable(): Boolean = isWatchable(unlocked)

    /**
     * Remote Config đổi khi đang ở Player (fetch về muộn, Console bật/tắt): tắt → ẩn nút "Watch an ad" (trang khoá
     * chỉ còn thông báo khoá), không báo "No ad available"; bật → load sẵn và mời xem nếu đang ở trang khoá.
     */
    private fun onRewardedPlacementChanged(enabled: Boolean) {
        if (episodes.isEmpty()) return
        if (enabled) {
            preloadRewardedIfLocked()
            if (currentState.isLocked && currentState.unlockAd == null) offerRewardedUnlock(currentState.episodeIndex)
        } else if (!rewardedShowing) {
            rewardedLoadJob?.cancel()
            updateState { copy(unlockAd = null) }
        }
    }

    /** Show còn tập khoá → load rewarded sẵn từ lúc mở Player, tới trang khoá ad thường đã sẵn. */
    private fun preloadRewardedIfLocked() {
        if (!isAdPlacementEnabled(AdPlacement.RewardedBonus) || episodes.all { it.isWatchable() }) return
        rewardAdManager.preload(RewardAdManager.AdType.UNLOCK_EPISODE)
    }

    /** Trang khoá + `rewarded_bonus` bật → nút mở bằng ad; ad load ngay để lúc bấm khỏi chờ. */
    private fun offerRewardedUnlock(index: Int) {
        rewardedLoadJob?.cancel()
        rewardedShowing = false
        if (!isAdPlacementEnabled(AdPlacement.RewardedBonus)) return
        updateState { copy(unlockAd = RewardedAdState.Loading) }
        rewardedLoadJob = viewModelScope.launch {
            val available = rewardAdManager.load(RewardAdManager.AdType.UNLOCK_EPISODE)
            if (currentState.episodeIndex != index || !currentState.isLocked) return@launch
            updateState {
                copy(
                    unlockAd = when {
                        // Bị tắt trong lúc load: ẩn nút, không báo "No ad available".
                        !isAdPlacementEnabled(AdPlacement.RewardedBonus) -> null
                        available -> RewardedAdState.Ready
                        else -> RewardedAdState.Unavailable
                    },
                )
            }
        }
    }

    /** Không ad nào giữa lúc video phát: trang khoá vốn đã dừng, vẫn pause chắc chắn trước khi show. */
    private fun showRewarded() {
        if (currentState.unlockAd != RewardedAdState.Ready || rewardedShowing) return
        rewardedShowing = true
        player.pause()
        sendEvent(PlayerSingleEvent.ShowRewarded(currentState.episodeIndex))
    }

    /** Xem hết → mở tập và phát ngay tại trang này (không rời Player); đóng sớm → giữ khoá, cho xem lại. */
    private fun onRewardResult(index: Int, earned: Boolean) {
        rewardedShowing = false
        // Ad vừa xem đã dùng hết — load sẵn bản kế cho tập khoá tiếp theo.
        preloadRewardedIfLocked()
        val episode = episodes.firstOrNull { it.index == index } ?: return
        if (!earned) {
            sendEvent(PlayerSingleEvent.ShowToast(R.string.watch_unlock_incomplete, formatArg = null, success = false))
            if (currentState.episodeIndex == index) offerRewardedUnlock(index)
            return
        }
        viewModelScope.launch {
            unlockEpisode(showId, index).onSuccess {
                unlocked = unlocked + index
                sendEvent(PlayerSingleEvent.ShowToast(R.string.watch_unlock_success, episode.displayNumber, success = true))
                refreshPlaylist()
                refreshPages()
                if (currentState.episodeIndex == index) openEpisode(index, positionMs = 0) else refreshGrid()
            }.onFailure { error ->
                sendEvent(PlayerSingleEvent.ShowToast(error.toErrorRes(), formatArg = null, success = false))
                if (currentState.episodeIndex == index) offerRewardedUnlock(index)
            }
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    companion object {
        const val KEY_EPISODE_INDEX = "player_episode_index"
        const val KEY_SPEED = "player_speed"
        const val KEY_QUALITY = "player_quality"

        private const val SAVE_INTERVAL_MS = 5_000L
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val STALL_CHECK_MS = PlaybackQualityPolicy.STALL_THRESHOLD_MS + 100
    }
}
