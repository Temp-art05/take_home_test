package com.takehome.drama.feature.main.presentation.reels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.takehome.drama.core.media.EpisodeFeedPlayer
import com.takehome.drama.core.media.EpisodeFeedPlayerFactory
import com.takehome.drama.core.media.FeedItem
import com.takehome.drama.core.media.PlaybackProgress
import com.takehome.drama.core.media.PlaybackSpeed
import com.takehome.drama.core.media.PlaybackState
import com.takehome.drama.core.media.progress
import com.takehome.drama.core.ui.foundation.error.toErrorRes
import com.takehome.drama.core.ui.foundation.error.toUiError
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.library.ObserveIsShowSavedUseCase
import com.takehome.drama.domain.usecase.library.SetShowSavedUseCase
import com.takehome.drama.domain.usecase.reels.GetReelClipUseCase
import com.takehome.drama.domain.usecase.reels.GetReelsFeedUseCase
import com.takehome.drama.domain.usecase.reels.ReelClip
import com.takehome.drama.feature.main.R
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * SCR-0203 — feed dọc mỗi show một reel, **cùng engine với Player**: [EpisodeFeedPlayer] pool (−1/0/+1,
 * lân cận prepare + pause, cache đĩa theo phiên). VM sở hữu player (xoay máy không rebuffer).
 *
 * - Tập/URL nạp lười cho reel hiện tại −1…+2; show không có tập free phát được bị bỏ khỏi feed.
 * - Chỉ phát khi tab Reels đang chọn **và** màn STARTED; ngoài ra `setActive(false)` nhả hết decoder —
 *   mở Player từ đây không bao giờ có hai pool cùng giữ decoder (máy yếu).
 * - Quy ước feed như Player: vuốt tới reel nào cũng phát từ đầu; không ghi tiến độ Room (xem thử).
 */
class ReelsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getCatalog: GetCatalogUseCase,
    private val getReelsFeed: GetReelsFeedUseCase,
    private val getReelClip: GetReelClipUseCase,
    private val resolveContentLanguage: ResolveContentLanguageUseCase,
    private val observeIsShowSaved: ObserveIsShowSavedUseCase,
    private val setShowSaved: SetShowSavedUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    playerFactory: EpisodeFeedPlayerFactory,
) : BaseViewModel<ReelsIntent, ReelsViewState, ReelsSingleEvent>() {

    val player: EpisodeFeedPlayer = playerFactory.create()

    /** Tiến độ phát tách khỏi [viewState] (tick không recompose cả màn), gắn với reel nó đo — xem [PlaybackProgress]. */
    val playbackProgress: StateFlow<PlaybackProgress> = player.state
        .map { it.progress }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlaybackProgress())

    private var catalog: Catalog? = null
    private var language = ""
    private var shows: List<Show> = emptyList()
    private val clips = mutableMapOf<String, ReelClip>()
    private val statuses = mutableMapOf<String, ClipStatus>()
    private val liked = mutableSetOf<String>()
    private var savedIds: Set<String> = emptySet()

    private var tabVisible = false
    private var started = false
    private var active = false

    /** Lần đầu hiện tab thì tự phát; user pause rồi rời tab thì quay lại vẫn pause. */
    private var playOnActive = true

    override fun initState() = ReelsViewState(
        current = savedStateHandle[KEY_CURRENT] ?: 0,
        speed = savedStateHandle[KEY_SPEED] ?: 1f,
    )

    init {
        player.setActive(false)
        player.setSpeed(currentState.speed)
        viewModelScope.launch { player.state.collect(::onPlayback) }
        viewModelScope.launch {
            observeIsShowSaved.savedIds().collect {
                savedIds = it
                refreshPages()
            }
        }
        load()
    }

    override fun processIntent(intent: ReelsIntent) {
        when (intent) {
            is ReelsIntent.PageSettled -> onPageSettled(intent.page)
            ReelsIntent.TogglePlay -> togglePlay()
            ReelsIntent.CycleSpeed -> cycleSpeed()
            ReelsIntent.ToggleLike -> toggleLike()
            ReelsIntent.ToggleSave -> toggleSave()
            ReelsIntent.ToggleDescription -> updateState { copy(descriptionExpanded = !descriptionExpanded) }
            ReelsIntent.WatchAll -> watchAll()
            ReelsIntent.Retry -> if (!currentState.isLoading) load()
            ReelsIntent.RetryClip -> retryCurrent()
            is ReelsIntent.TabVisible -> {
                tabVisible = intent.visible
                applyActive()
            }
            ReelsIntent.ScreenStarted -> {
                started = true
                applyActive()
            }
            is ReelsIntent.ScreenStopped -> if (!intent.isChangingConfigurations) {
                started = false
                applyActive()
            }
        }
    }

    private fun load() {
        updateState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val loaded = getCatalog().getOrElse { error ->
                updateState { copy(isLoading = false, error = error.toUiError()) }
                return@launch
            }
            val preferences = userPreferencesRepository.preferences.first()
            language = resolveContentLanguage(preferences.contentLanguage, Locale.getDefault().language, loaded)
            catalog = loaded
            shows = getReelsFeed(loaded, preferences.tasteTagIds)
            val current = currentState.current.coerceIn(0, (shows.size - 1).coerceAtLeast(0))
            updateState { copy(isLoading = false, current = current) }
            refreshPages()
            refreshPlaylist()
            requestClips()
        }
    }

    /** Nạp tập cho reel hiện tại, một reel trước và hai reel sau — song song, cache ở repository. */
    private fun requestClips() {
        val catalog = catalog ?: return
        val current = currentState.current
        ((current - 1)..(current + 2))
            .mapNotNull { shows.getOrNull(it) }
            .filter { it.id !in statuses }
            .forEach { show ->
                statuses[show.id] = ClipStatus.Loading
                viewModelScope.launch {
                    getReelClip(show.id, language, catalog.defaultLanguage)
                        .onSuccess { clip -> onClip(show, clip) }
                        .onFailure {
                            statuses[show.id] = ClipStatus.Failed
                            refreshPages()
                        }
                }
            }
        refreshPages()
    }

    private fun onClip(show: Show, clip: ReelClip?) {
        val position = shows.indexOf(show)
        if (clip == null) {
            // Show không có tập free phát được: bỏ nếu nằm sau trang hiện tại (không làm lệch vị trí
            // đang phát); nằm trước thì để trang báo "không xem được".
            if (position > currentState.current) {
                shows = shows - show
                statuses.remove(show.id)
                refreshPages()
                refreshPlaylist()
                requestClips()
            } else {
                statuses[show.id] = ClipStatus.Unavailable
                refreshPages()
            }
            return
        }
        clips[show.id] = clip
        statuses[show.id] = ClipStatus.Ready
        refreshPages()
        refreshPlaylist()
        if (position == currentState.current) player.setCurrent(position, startPositionMs = 0, playWhenReady = active && playOnActive)
    }

    private fun refreshPlaylist() {
        player.setPlaylist(shows.map { FeedItem(id = it.id, url = clips[it.id]?.url) })
    }

    /** Tập đã có URL (lỗi phát) → nạp lại vào player; chưa có (lỗi tải file tập) → tải lại. */
    private fun retryCurrent() {
        val show = currentShow() ?: return
        if (show.id in clips) {
            player.setCurrent(currentState.current, startPositionMs = 0, playWhenReady = active)
        } else {
            statuses.remove(show.id)
            requestClips()
        }
    }

    private fun onPageSettled(page: Int) {
        if (page == currentState.current || page !in shows.indices) return
        savedStateHandle[KEY_CURRENT] = page
        updateState { copy(current = page, descriptionExpanded = false) }
        // Vuốt sang reel mới = muốn xem: phát (nếu tab đang hiện) dù reel trước đang pause.
        playOnActive = true
        player.setCurrent(page, startPositionMs = 0, playWhenReady = active)
        requestClips()
    }

    private fun applyActive() {
        val shouldBeActive = tabVisible && started
        if (shouldBeActive == active) return
        active = shouldBeActive
        if (active) {
            player.setActive(true)
            if (playOnActive && currentShow()?.let { it.id in clips } == true) player.play()
        } else {
            // Đang phát → quay lại phát tiếp; reel còn đang nạp thì giữ ý định cũ; user đã pause thì giữ pause.
            val stillLoading = currentShow()?.let { it.id !in clips } == true
            playOnActive = player.state.value.playWhenReady || (stillLoading && playOnActive)
            player.pause()
            player.setActive(false)
        }
    }

    private fun onPlayback(playback: PlaybackState) {
        val wasEnded = currentState.playback.ended
        updateState { copy(playback = playback.copy(positionMs = 0, bufferedMs = 0)) }
        if (playback.ended && !wasEnded) {
            val next = currentState.current + 1
            if (next in shows.indices) {
                // Chỉ cuộn; reel kế mở ở PageSettled như user tự vuốt.
                sendEvent(ReelsSingleEvent.ScrollToPage(next))
            } else {
                player.seekTo(0)
                player.play()
            }
        }
    }

    private fun togglePlay() {
        if (player.state.value.playWhenReady) {
            player.pause()
            playOnActive = false
        } else {
            player.play()
            playOnActive = true
        }
    }

    private fun cycleSpeed() {
        val speed = PlaybackSpeed.next(currentState.speed)
        savedStateHandle[KEY_SPEED] = speed
        player.setSpeed(speed)
        updateState { copy(speed = speed) }
    }

    private fun toggleLike() {
        val showId = currentShow()?.id ?: return
        if (!liked.add(showId)) liked.remove(showId)
        refreshPages()
    }

    private fun toggleSave() {
        val showId = currentShow()?.id ?: return
        val save = showId !in savedIds
        viewModelScope.launch {
            setShowSaved(showId, save)
                .onSuccess {
                    sendEvent(ReelsSingleEvent.ShowToast(if (save) R.string.common_saved_to_library else R.string.common_removed_from_library, true))
                }
                .onFailure { sendEvent(ReelsSingleEvent.ShowToast(it.toErrorRes(), false)) }
        }
    }

    private fun watchAll() {
        val show = currentShow() ?: return
        val clip = clips[show.id] ?: return
        sendEvent(ReelsSingleEvent.OpenPlayer(show.id, clip.episode.index))
    }

    private fun currentShow(): Show? = shows.getOrNull(currentState.current)

    private fun refreshPages() {
        val catalog = catalog ?: return
        val default = catalog.defaultLanguage
        updateState {
            copy(
                pages = shows.map { show ->
                    val clip = clips[show.id]
                    val isLiked = show.id in liked
                    ReelPage(
                        showId = show.id,
                        title = show.title.resolve(language, default),
                        description = show.hook.resolve(language, default).ifBlank { show.synopsis.resolve(language, default) },
                        posterUrl = show.posterUrl,
                        status = statuses[show.id] ?: ClipStatus.Loading,
                        episodeIndex = clip?.episode?.index,
                        episodeNumber = clip?.episode?.displayNumber,
                        episodeTotal = clip?.episodeTotal,
                        likeCount = (clip?.episode?.likeCount ?: 0) + if (isLiked) 1 else 0,
                        isLiked = isLiked,
                        isSaved = show.id in savedIds,
                    )
                },
            )
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    companion object {
        const val KEY_CURRENT = "reels_current"
        const val KEY_SPEED = "reels_speed"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
