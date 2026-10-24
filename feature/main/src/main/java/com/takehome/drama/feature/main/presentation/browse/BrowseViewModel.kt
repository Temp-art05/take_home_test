package com.takehome.drama.feature.main.presentation.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.takehome.drama.core.media.EpisodeFeedPlayer
import com.takehome.drama.core.media.EpisodeFeedPlayerFactory
import com.takehome.drama.core.media.FeedItem
import com.takehome.drama.core.ui.foundation.error.toErrorRes
import com.takehome.drama.core.ui.foundation.error.toUiError
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.core.utils.network.ConnectivityMonitor
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.findShow
import com.takehome.drama.domain.model.genres
import com.takehome.drama.domain.model.newestTag
import com.takehome.drama.domain.model.hero
import com.takehome.drama.domain.model.showsInGenre
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.discovery.PreviewAutoplayPolicy
import com.takehome.drama.domain.usecase.library.ObserveIsShowSavedUseCase
import com.takehome.drama.domain.usecase.library.SetShowSavedUseCase
import com.takehome.drama.domain.usecase.reels.GetReelClipUseCase
import com.takehome.drama.domain.usecase.reels.ReelClip
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.presentation.common.toCard
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * SCR-0202 Preview Dock. Preview = tập free đầu của show đang chọn, **câm**, lặp, trên một
 * [EpisodeFeedPlayer] một mục (pool chỉ dựng đúng một ExoPlayer). Chỉ phát khi tab Browse đang chọn,
 * màn STARTED và rule autoplay cho phép (Wi-Fi only mặc định) — hoặc user chạm vào preview; ngoài ra
 * player nhả decoder (`setActive(false)`). Chip + show đang chọn nằm trong [SavedStateHandle].
 */
class BrowseViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getCatalog: GetCatalogUseCase,
    private val getReelClip: GetReelClipUseCase,
    private val resolveContentLanguage: ResolveContentLanguageUseCase,
    private val observeIsShowSaved: ObserveIsShowSavedUseCase,
    private val setShowSaved: SetShowSavedUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    playerFactory: EpisodeFeedPlayerFactory,
) : BaseViewModel<BrowseIntent, BrowseViewState, BrowseSingleEvent>() {

    val player: EpisodeFeedPlayer = playerFactory.create()

    private var catalog: Catalog? = null
    private var language = ""
    private var clip: ReelClip? = null

    /** Tải file tập của preview lỗi (mất mạng) — thử lại khi user chạm preview / autoplay bật lại. */
    private var clipFailed = false
    private var tabVisible = false
    private var started = false

    /** User chạm preview khi autoplay tắt — chạy tới khi đổi show. */
    private var playRequested = false
    private var clipJob: Job? = null
    private var savedJob: Job? = null

    override fun initState() = BrowseViewState(selectedChip = savedStateHandle[KEY_CHIP])

    init {
        player.setMuted(true)
        player.setActive(false)
        viewModelScope.launch {
            player.state.collect { playback ->
                // Lặp như trailer: hết thì về đầu.
                if (playback.ended && shouldPlay()) {
                    player.seekTo(0)
                    player.play()
                }
            }
        }
        load()
    }

    override fun processIntent(intent: BrowseIntent) {
        when (intent) {
            is BrowseIntent.SelectChip -> selectChip(intent.tagId)
            is BrowseIntent.SelectPoster -> selectPoster(intent.showId)
            BrowseIntent.PlayPreview -> {
                playRequested = true
                if (!retryFailedClip()) applyPlayback()
            }
            BrowseIntent.PlayEpisode -> playEpisode()
            BrowseIntent.ToggleSave -> toggleSave()
            BrowseIntent.OpenSearch -> sendEvent(BrowseSingleEvent.OpenSearch)
            BrowseIntent.Retry -> if (!currentState.isLoading) load()
            is BrowseIntent.TabVisible -> {
                tabVisible = intent.visible
                applyPlayback()
            }
            BrowseIntent.ScreenStarted -> {
                started = true
                applyPlayback()
            }
            is BrowseIntent.ScreenStopped -> if (!intent.isChangingConfigurations) {
                started = false
                applyPlayback()
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
            val saved = userPreferencesRepository.preferences.first().contentLanguage
            language = resolveContentLanguage(saved, Locale.getDefault().language, loaded)
            catalog = loaded
            // All · genre… · New (cuối, như mockup) — New chỉ khi có show gắn tag `newest`.
            val chips = listOf(BrowseChip(null, "")) +
                (loaded.genres() + listOfNotNull(loaded.newestTag()))
                    .map { BrowseChip(it.id, it.name.resolveTagLabel(language, loaded.defaultLanguage)) }
            val selectedChip = currentState.selectedChip?.takeIf { id -> chips.any { it.tagId == id } }
            updateState {
                copy(
                    isLoading = false,
                    chips = chips,
                    selectedChip = selectedChip,
                    grid = loaded.showsInGenre(selectedChip).map { it.toCard(loaded, language) },
                )
            }
            val previewId = savedStateHandle.get<String>(KEY_SHOW)?.takeIf { loaded.findShow(it) != null }
                ?: loaded.hero()?.id
            previewId?.let(::showPreview)
            observeAutoplay()
        }
    }

    private fun observeAutoplay() {
        viewModelScope.launch {
            combine(userPreferencesRepository.preferences, connectivityMonitor.isUnmetered) { preferences, unmetered ->
                PreviewAutoplayPolicy.shouldAutoplay(preferences.autoplayPreviewWifiOnly, unmetered)
            }.collect { autoplay ->
                updateState { copy(autoplay = autoplay) }
                if (!(autoplay && retryFailedClip())) applyPlayback()
            }
        }
    }

    /** Đổi chip chỉ đổi lưới — preview giữ nguyên show đang chạy. */
    private fun selectChip(tagId: String?) {
        val catalog = catalog ?: return
        savedStateHandle[KEY_CHIP] = tagId
        updateState { copy(selectedChip = tagId, grid = catalog.showsInGenre(tagId).map { it.toCard(catalog, language) }) }
    }

    private fun selectPoster(showId: String) {
        if (showId == currentState.preview?.id) {
            sendEvent(BrowseSingleEvent.OpenShow(showId))
        } else {
            showPreview(showId)
        }
    }

    private fun showPreview(showId: String) {
        val catalog = catalog ?: return
        val show = catalog.findShow(showId) ?: return
        savedStateHandle[KEY_SHOW] = showId
        playRequested = false
        clip = null
        clipFailed = false
        updateState { copy(preview = show.toCard(catalog, language), previewEpisodeTotal = null) }
        savedJob?.cancel()
        savedJob = viewModelScope.launch { observeIsShowSaved(showId).collect { updateState { copy(isSaved = it) } } }
        clipJob?.cancel()
        clipJob = viewModelScope.launch {
            val result = getReelClip(showId, language, catalog.defaultLanguage)
            val loaded = result.getOrNull()
            clip = loaded
            clipFailed = result.isFailure
            updateState { copy(previewEpisodeTotal = loaded?.episodeTotal) }
            player.setPlaylist(listOf(FeedItem(id = showId, url = loaded?.url)))
            player.setCurrent(0, startPositionMs = 0, playWhenReady = shouldPlay())
            applyPlayback()
        }
    }

    /** true khi đã tải lại clip (job mới tự áp playback). Giữ `playRequested` của lần chạm này. */
    private fun retryFailedClip(): Boolean {
        if (!clipFailed) return false
        val showId = currentState.preview?.id ?: return false
        val requested = playRequested
        showPreview(showId)
        playRequested = requested
        return true
    }

    private fun shouldPlay(): Boolean =
        tabVisible && started && clip != null && (currentState.autoplay || playRequested)

    /** Một chỗ duy nhất quyết định preview chạy hay nhả decoder. */
    private fun applyPlayback() {
        if (shouldPlay()) {
            player.setActive(true)
            player.play()
        } else {
            player.pause()
            player.setActive(false)
        }
    }

    private fun playEpisode() {
        val showId = currentState.preview?.id ?: return
        val clip = clip
        sendEvent(if (clip != null) BrowseSingleEvent.OpenPlayer(showId, clip.episode.index) else BrowseSingleEvent.OpenShow(showId))
    }

    private fun toggleSave() {
        val showId = currentState.preview?.id ?: return
        val save = !currentState.isSaved
        viewModelScope.launch {
            setShowSaved(showId, save)
                .onSuccess {
                    sendEvent(BrowseSingleEvent.ShowToast(if (save) R.string.common_saved_to_library else R.string.common_removed_from_library, success = true))
                }
                .onFailure { sendEvent(BrowseSingleEvent.ShowToast(it.toErrorRes(), success = false)) }
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    private companion object {
        const val KEY_CHIP = "browse_chip"
        const val KEY_SHOW = "browse_show"
    }
}
