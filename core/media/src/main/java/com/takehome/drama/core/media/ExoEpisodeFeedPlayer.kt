package com.takehome.drama.core.media

import android.content.Context
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager.PreloadStatus
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * [EpisodeFeedPlayer] trên **một** ExoPlayer + `DefaultPreloadManager` dựng từ cùng một builder
 * (chung load control, bandwidth meter, renderer, nguồn qua cache đĩa). Một decoder duy nhất → hợp
 * máy yếu; preload chỉ tốn RAM buffer. Tạo và gọi trên main thread.
 *
 * Kiểu TikTok: tập hiện tại được ưu tiên băng thông — preload chỉ chạy (`invalidate`) sau khi tập
 * hiện tại lên khung đầu và không đang buffer.
 */
@OptIn(UnstableApi::class)
class ExoEpisodeFeedPlayer(
    context: Context,
    private val profile: MediaProfile,
    dataSourceFactory: DataSource.Factory,
) : EpisodeFeedPlayer {

    private var items: List<FeedItem> = emptyList()
    private var current = 0
    private var preloadActive = true

    /** Vị trí → MediaItem đang nằm trong preload manager. */
    private val managed = mutableMapOf<Int, MediaItem>()

    /** Mục đã ra khỏi manager nhưng player còn đang dùng nguồn của nó — gỡ sau khi player đổi nguồn. */
    private val retired = mutableListOf<MediaItem>()
    private var playerItem: MediaItem? = null
    private var firstFrameRequestedAt = 0L
    private var lastSetFromPreload = false

    private val statusControl = TargetPreloadStatusControl<Int, PreloadStatus> { position ->
        val item = items.getOrNull(position)
        when (val target = preloadTarget(position - current, isLocked = item?.url == null, profile = profile)) {
            is PreloadTarget.Range -> PreloadStatus.specifiedRangeLoaded(target.durationMs)
            PreloadTarget.TracksSelected -> PreloadStatus.PRELOAD_STATUS_TRACKS_SELECTED
            null -> PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
        }
    }

    private val preloadBuilder = DefaultPreloadManager.Builder(context.applicationContext, statusControl)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    profile.minBufferMs,
                    profile.maxBufferMs,
                    profile.bufferForPlaybackMs,
                    minOf(REBUFFER_MS, profile.minBufferMs),
                )
                .apply { profile.targetBufferBytes?.let(::setTargetBufferBytes) }
                .build(),
        )
        .setRenderersFactory(
            DefaultRenderersFactory(context.applicationContext)
                // Decoder đầu tiên khởi tạo lỗi thì thử decoder kế thay vì dừng phát.
                .setEnableDecoderFallback(true)
                .setMediaCodecSelector(EmulatorLastCodecSelector),
        )
        .setTrackSelectorFactory { trackContext ->
            DefaultTrackSelector(trackContext).apply {
                profile.maxVideoSize?.let { (width, height) ->
                    setParameters(buildUponParameters().setMaxVideoSize(width, height))
                }
            }
        }

    internal val exoPlayer: ExoPlayer = preloadBuilder.buildExoPlayer(
        ExoPlayer.Builder(context.applicationContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true),
    )
    private val preloadManager: DefaultPreloadManager = preloadBuilder.build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _firstFrameRendered = MutableStateFlow(false)
    override val firstFrameRendered: StateFlow<Boolean> = _firstFrameRendered.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish()
            if (player.isPlaying) startTicker() else stopTicker()
            if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) maybeInvalidate()
        }

        override fun onRenderedFirstFrame() {
            Timber.d("FirstFrame %d ms (preloaded=%b)", SystemClock.elapsedRealtime() - firstFrameRequestedAt, lastSetFromPreload)
            _firstFrameRendered.value = true
            maybeInvalidate()
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.w(error, "Playback error %s", error.errorCodeName)
            _state.update { it.copy(error = PlaybackError(error.errorCodeName)) }
        }
    }

    init {
        exoPlayer.addListener(listener)
    }

    override fun setPlaylist(items: List<FeedItem>) {
        this.items = items
        // URL đổi (quality/ngôn ngữ) → bản preload cũ vô dụng.
        managed.entries.removeAll { (position, mediaItem) ->
            val changed = items.getOrNull(position)?.url != mediaItem.localConfiguration?.uri?.toString()
            if (changed) retire(mediaItem)
            changed
        }
        syncWindow()
    }

    override fun setCurrent(position: Int, startPositionMs: Long, playWhenReady: Boolean) {
        val item = items.getOrNull(position) ?: return
        current = position
        _firstFrameRendered.value = false
        preloadManager.setCurrentPlayingIndex(position)
        val url = item.url
        if (url == null) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            playerItem = null
            _state.value = PlaybackState()
        } else {
            val mediaItem = managed[position] ?: mediaItemOf(item, url)
            val preloaded = preloadManager.getMediaSource(mediaItem)
            lastSetFromPreload = preloaded != null
            _state.value = PlaybackState(positionMs = startPositionMs, playWhenReady = playWhenReady, isBuffering = true)
            if (preloaded != null) {
                exoPlayer.setMediaSource(preloaded, startPositionMs)
            } else {
                exoPlayer.setMediaItem(mediaItem, startPositionMs)
            }
            exoPlayer.playWhenReady = playWhenReady
            exoPlayer.prepare()
            firstFrameRequestedAt = SystemClock.elapsedRealtime()
            playerItem = mediaItem
        }
        val stale = retired.filter { it != playerItem }
        stale.forEach(preloadManager::remove)
        retired.removeAll(stale)
        syncWindow()
    }

    override fun setPreloadActive(active: Boolean) {
        if (preloadActive == active) return
        preloadActive = active
        syncWindow()
        maybeInvalidate()
    }

    override fun play() {
        // Hết tập rồi bấm play = xem lại từ đầu, như mọi player.
        if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0)
        exoPlayer.play()
    }

    override fun pause() = exoPlayer.pause()

    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        publish()
    }

    override fun setSpeed(speed: Float) = exoPlayer.setPlaybackSpeed(speed)

    override fun currentPositionMs(): Long = exoPlayer.currentPosition

    override fun release() {
        scope.cancel()
        exoPlayer.removeListener(listener)
        // Manager trước: nó giữ nguồn mà player có thể đang dùng.
        preloadManager.release()
        exoPlayer.release()
    }

    /**
     * Đưa preload manager về đúng cửa sổ quanh [current]. Tập hiện tại luôn nằm trong manager khi
     * player đang dùng nguồn của nó (gỡ ra = nhả nguồn player đang phát).
     */
    private fun syncWindow() {
        val window = if (preloadActive) {
            preloadWindow(current, items.size, profile) { items[it].url != null }
        } else {
            emptySet()
        }
        val wanted = window + listOfNotNull(current.takeIf { items.getOrNull(it)?.url != null })
        val diff = PreloadWindowDiff.between(managed.keys.toSet(), wanted)
        diff.toRemove.forEach { position -> managed.remove(position)?.let(::retire) }
        diff.toAdd.forEach { position ->
            val item = items[position]
            val mediaItem = mediaItemOf(item, item.url ?: return@forEach)
            managed[position] = mediaItem
            preloadManager.add(mediaItem, position)
        }
    }

    private fun retire(mediaItem: MediaItem) {
        if (mediaItem == playerItem) retired += mediaItem else preloadManager.remove(mediaItem)
    }

    /** Preload chỉ chạy khi tập hiện tại đã lên hình và không buffer — không giành băng thông. */
    private fun maybeInvalidate() {
        if (!preloadActive) return
        val currentReady = playerItem == null ||
            (_firstFrameRendered.value && exoPlayer.playbackState != Player.STATE_BUFFERING)
        if (currentReady) preloadManager.invalidate()
    }

    private fun mediaItemOf(item: FeedItem, url: String): MediaItem =
        MediaItem.Builder().setMediaId(item.id).setUri(url).build()

    private fun publish() {
        val duration = exoPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0
        _state.update { current ->
            current.copy(
                isPlaying = exoPlayer.isPlaying,
                playWhenReady = exoPlayer.playWhenReady,
                isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING,
                positionMs = exoPlayer.currentPosition,
                durationMs = duration,
                bufferedMs = exoPlayer.bufferedPosition,
                ended = exoPlayer.playbackState == Player.STATE_ENDED,
                // Chuẩn bị lại (đổi tập / retry) thì xoá lỗi cũ.
                error = if (exoPlayer.playerError == null) null else current.error,
            )
        }
    }

    /** Vị trí không có callback riêng: chỉ poll khi đang phát, dừng thì thôi để không tốn pin. */
    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                publish()
                delay(TICK_MS)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private companion object {
        const val TICK_MS = 500L
        const val REBUFFER_MS = 1_500

        /**
         * Decoder `c2.goldfish.*` của Android Emulator lỗi giữa chừng với H.264 1080×1920 của fixtures
         * (`ERROR_CODE_DECODING_FAILED`) → đẩy nó xuống cuối để dùng decoder phần mềm. Máy thật không
         * có decoder goldfish nên thứ tự giữ nguyên.
         */
        val EmulatorLastCodecSelector = MediaCodecSelector { mimeType, requiresSecure, requiresTunneling ->
            MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecure, requiresTunneling)
                .sortedBy { it.name.startsWith("c2.goldfish") }
        }
    }
}
