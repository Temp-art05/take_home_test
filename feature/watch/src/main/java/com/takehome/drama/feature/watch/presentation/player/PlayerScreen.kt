package com.takehome.drama.feature.watch.presentation.player

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.ads.reward.RewardAdManager
import com.takehome.drama.core.designsystem.component.AppAsyncImage
import com.takehome.drama.core.designsystem.component.AppBottomSheet
import com.takehome.drama.core.designsystem.component.AppToastHost
import com.takehome.drama.core.designsystem.component.GhostPillButton
import com.takehome.drama.core.designsystem.component.GradientButton
import com.takehome.drama.core.designsystem.component.rememberToastState
import com.takehome.drama.core.designsystem.ext.clickableNoRipple
import com.takehome.drama.core.designsystem.icon.AppIcons
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.media.KeepScreenOnWhilePlaying
import com.takehome.drama.core.media.PlaybackProgress
import com.takehome.drama.core.media.PlaybackState
import com.takehome.drama.core.media.VideoSlot
import com.takehome.drama.core.media.VideoSurface
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.core.ui.foundation.format.formatCompactCount
import com.takehome.drama.core.utils.extension.findActivity
import com.takehome.drama.domain.usecase.watch.EndAction
import com.takehome.drama.domain.usecase.watch.EpisodeRange
import com.takehome.drama.feature.watch.R
import com.takehome.drama.feature.watch.presentation.common.RewardedAdState
import com.takehome.drama.feature.watch.presentation.common.RewardedUnlockButton
import com.takehome.drama.feature.watch.presentation.player.component.CenterPlayIcon
import com.takehome.drama.feature.watch.presentation.player.component.EpisodePickerSheet
import com.takehome.drama.feature.watch.presentation.player.component.EpisodesPill
import com.takehome.drama.feature.watch.presentation.player.component.PlaybackOptionsSheet
import com.takehome.drama.feature.watch.presentation.player.component.PlayerInfo
import com.takehome.drama.feature.watch.presentation.player.component.PlayerRail
import com.takehome.drama.feature.watch.presentation.player.component.PlayerTopBar
import com.takehome.drama.feature.watch.presentation.player.component.PlayerTrack
import com.takehome.drama.feature.watch.presentation.player.model.PlayerPage
import com.takehome.drama.feature.watch.presentation.player.model.PlayerSheet
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Overlay mờ hết khi trang lệch ~60% chiều cao. */
private const val OVERLAY_FADE = 1.6f

/** Trượt sang tập kế khi tự chuyển tập: đủ chậm để mắt theo kịp, không giật như spring mặc định. */
private const val AUTO_SCROLL_MS = 650

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    rewardAdManager: RewardAdManager = koinInject(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    // Không `by`: chỉ thanh track đọc `.value` → tick 500ms không recompose cả màn.
    val progress = viewModel.playbackProgress.collectAsStateWithLifecycle()
    val slots by viewModel.player.slots.collectAsStateWithLifecycle()
    val toastState = rememberToastState()
    val pagerState = rememberPagerState { viewState.pages.size }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = LocalActivity.current

    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            PlayerSingleEvent.NavigateBack -> onBack()
            is PlayerSingleEvent.ShowToast -> toastState.show(
                text = event.formatArg?.let { resources.getString(event.messageRes, it) } ?: resources.getString(event.messageRes),
                success = event.success,
            )
            is PlayerSingleEvent.ShowRewarded -> {
                // VM đã pause player trước khi phát event này — không ad nào chạy đè video đang phát.
                val onResult = { earned: Boolean -> viewModel.processIntent(PlayerIntent.RewardResult(event.episodeIndex, earned)) }
                if (activity != null) rewardAdManager.show(activity, RewardAdManager.AdType.UNLOCK_EPISODE, onResult) else onResult(false)
            }
            is PlayerSingleEvent.ScrollToEpisode -> {
                val page = viewState.pages.indexOfFirst { it.episodeIndex == event.index }
                if (page >= 0) {
                    scope.launch {
                        // Tập liền kề: trượt chậm, mềm (tự sang tập / chọn tập kế); xa hơn thì nhảy thẳng.
                        if (abs(page - pagerState.currentPage) == 1) {
                            pagerState.animateScrollToPage(page, animationSpec = tween(AUTO_SCROLL_MS, easing = FastOutSlowInEasing))
                        } else {
                            pagerState.scrollToPage(page)
                        }
                    }
                }
            }
        }
    }
    // Vào nền → pause + lưu vị trí; trở lại → phát tiếp nếu trước đó đang phát. Quyết định ở VM.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.processIntent(PlayerIntent.ScreenStarted) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        val changingConfigurations = context.findActivity()?.isChangingConfigurations == true
        viewModel.processIntent(PlayerIntent.ScreenStopped(changingConfigurations))
    }
    BackHandler { viewModel.processIntent(PlayerIntent.BackClick) }
    KeepScreenOnWhilePlaying(viewModel.player)

    val pagesLoaded = viewState.pages.isNotEmpty()
    LaunchedEffect(pagerState, pagesLoaded) {
        if (!pagesLoaded) return@LaunchedEffect
        // VM là nguồn sự thật: lần đầu có trang (hoặc sau xoay máy) nhảy thẳng tới tập hiện tại.
        if (pagerState.currentPage != viewState.currentPage) pagerState.scrollToPage(viewState.currentPage)
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewState.pages.getOrNull(page)?.let { viewModel.processIntent(PlayerIntent.PageSettled(it.episodeIndex)) }
        }
    }

    Box(Modifier.fillMaxSize().background(AppTheme.colors.media.background)) {
        PlayerContent(
            viewState = viewState,
            pagerState = pagerState,
            progress = { progress.value },
            slots = slots,
            onBackClick = { viewModel.processIntent(PlayerIntent.BackClick) },
            onRetryClick = { viewModel.processIntent(PlayerIntent.RetryClick) },
            onTogglePlay = { viewModel.processIntent(PlayerIntent.TogglePlay) },
            onToggleDescription = { viewModel.processIntent(PlayerIntent.ToggleDescription) },
            onEpisodesClick = { viewModel.processIntent(PlayerIntent.EpisodesClick) },
            onUnlockWithAdClick = { viewModel.processIntent(PlayerIntent.UnlockWithAdClick) },
            onSwitchBackToHd = { viewModel.processIntent(PlayerIntent.SwitchBackToHd) },
            onEpisodeSelect = { index -> viewModel.processIntent(PlayerIntent.EpisodeSelect(index)) },
            onLikeClick = { viewModel.processIntent(PlayerIntent.LikeClick) },
            onSpeedClick = { viewModel.processIntent(PlayerIntent.SpeedClick) },
            onSpeedLongPress = { viewModel.processIntent(PlayerIntent.SpeedLongPress) },
            onSeek = { positionMs -> viewModel.processIntent(PlayerIntent.SeekTo(positionMs)) },
        )
        PlayerSheets(
            viewState = viewState,
            onDismiss = { viewModel.processIntent(PlayerIntent.DismissSheet) },
            onRangeSelect = { range -> viewModel.processIntent(PlayerIntent.RangeSelect(range)) },
            onEpisodeSelect = { index -> viewModel.processIntent(PlayerIntent.EpisodeSelect(index)) },
            onSpeedSelect = { speed -> viewModel.processIntent(PlayerIntent.SpeedSelect(speed)) },
            onQualitySelect = { quality -> viewModel.processIntent(PlayerIntent.QualitySelect(quality)) },
            onLanguageSelect = { language -> viewModel.processIntent(PlayerIntent.LanguageSelect(language)) },
            onAutoplayToggle = { viewModel.processIntent(PlayerIntent.AutoplayToggle) },
        )
        // Sheet vẽ cùng cửa sổ (AppBottomSheet) → toast đặt sau nó vẫn nổi trên scrim.
        AppToastHost(
            state = toastState,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 120.dp),
        )
    }
}

/** Sheet 0303 / 0304. Giữ sheet vừa đóng để animation thoát còn nội dung. */
@Composable
private fun PlayerSheets(
    viewState: PlayerViewState,
    onDismiss: () -> Unit,
    onRangeSelect: (EpisodeRange) -> Unit,
    onEpisodeSelect: (index: Int) -> Unit,
    onSpeedSelect: (Float) -> Unit,
    onQualitySelect: (String?) -> Unit,
    onLanguageSelect: (String) -> Unit,
    onAutoplayToggle: () -> Unit,
) {
    var shownSheet by remember { mutableStateOf(viewState.sheet) }
    if (viewState.sheet != PlayerSheet.None) shownSheet = viewState.sheet
    AppBottomSheet(
        visible = viewState.sheet != PlayerSheet.None,
        onDismiss = onDismiss,
    ) {
        when (shownSheet) {
            PlayerSheet.None -> Unit
            PlayerSheet.Episodes -> EpisodePickerSheet(
                cells = viewState.cells,
                ranges = viewState.ranges,
                selectedRange = viewState.selectedRange,
                watchedCount = viewState.watchedCount,
                onRangeSelect = onRangeSelect,
                onEpisodeSelect = onEpisodeSelect,
            )
            PlayerSheet.Options -> PlaybackOptionsSheet(
                speed = viewState.speed,
                qualities = viewState.qualities,
                quality = viewState.quality,
                languages = viewState.languages,
                language = viewState.language,
                autoplayNext = viewState.autoplayNext,
                onSpeedSelect = onSpeedSelect,
                onQualitySelect = onQualitySelect,
                onLanguageSelect = onLanguageSelect,
                onAutoplayToggle = onAutoplayToggle,
            )
        }
    }
}

@Composable
private fun PlayerContent(
    viewState: PlayerViewState,
    pagerState: PagerState,
    progress: () -> PlaybackProgress,
    slots: Map<Int, VideoSlot>,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleDescription: () -> Unit,
    onEpisodesClick: () -> Unit,
    onUnlockWithAdClick: () -> Unit,
    onSwitchBackToHd: () -> Unit,
    onEpisodeSelect: (index: Int) -> Unit,
    onLikeClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSpeedLongPress: () -> Unit,
    onSeek: (positionMs: Long) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (viewState.pages.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // Trang ±1 dựng sẵn để surface của player lân cận đã có khung hình khi vuốt nhẹ.
                beyondViewportPageCount = 1,
                key = { viewState.pages[it].episodeIndex },
            ) { page ->
                val item = viewState.pages[page]
                EpisodePage(
                    page = item,
                    slot = slots[page],
                    isCurrent = item.episodeIndex == viewState.episodeIndex,
                    viewState = viewState,
                    progress = progress,
                    // Overlay mờ dần theo độ lệch trang; đọc trong lambda của graphicsLayer → không recompose mỗi frame.
                    overlayAlpha = { (1f - abs(pagerState.getOffsetDistanceInPages(page)) * OVERLAY_FADE).coerceIn(0f, 1f) },
                    onBackClick = onBackClick,
                    onRetryClick = onRetryClick,
                    onTogglePlay = onTogglePlay,
                    onToggleDescription = onToggleDescription,
                    onEpisodesClick = onEpisodesClick,
                    onUnlockWithAdClick = onUnlockWithAdClick,
                    onSwitchBackToHd = onSwitchBackToHd,
                    onEpisodeSelect = onEpisodeSelect,
                    onLikeClick = onLikeClick,
                    onSpeedClick = onSpeedClick,
                    onSpeedLongPress = onSpeedLongPress,
                    onSeek = onSeek,
                )
            }
        }

        // Top bar cố định như TikTok: không trượt theo trang, đổi số tập khi trang dừng.
        Box(Modifier.fillMaxWidth().height(150.dp).background(AppTheme.colors.scrimTop))
        PlayerTopBar(
            title = viewState.showTitle,
            episodeNumber = viewState.episodeNumber,
            episodeTotal = viewState.episodeTotal,
            onBack = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = AppTheme.dimensions.s2, end = AppTheme.dimensions.s3),
        )

        when {
            viewState.isLoading -> CenterSpinner(label = null)
            viewState.errorRes != null -> CenterMessage(
                title = stringResource(viewState.errorRes),
                actionLabel = stringResource(R.string.watch_retry),
                onAction = onRetryClick,
            )
        }
    }
}

/**
 * Một trang của pager = một tập, **tự chứa** video + scrim + mô tả + rail + track + pill: vuốt là
 * cả trang trượt theo, overlay mờ dần ([overlayAlpha]). Trang lân cận vẽ surface của player đã
 * prepare + pause (khung hình thật); chưa có player/khung hình thì poster. Chỉ trang hiện tại nhận
 * tap play/pause, đọc vị trí live, hiện buffering/end card.
 */
@Composable
private fun EpisodePage(
    page: PlayerPage,
    slot: VideoSlot?,
    isCurrent: Boolean,
    viewState: PlayerViewState,
    progress: () -> PlaybackProgress,
    overlayAlpha: () -> Float,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleDescription: () -> Unit,
    onEpisodesClick: () -> Unit,
    onUnlockWithAdClick: () -> Unit,
    onSwitchBackToHd: () -> Unit,
    onEpisodeSelect: (index: Int) -> Unit,
    onLikeClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSpeedLongPress: () -> Unit,
    onSeek: (positionMs: Long) -> Unit,
) {
    val media = AppTheme.colors.media
    val playback = viewState.playback
    val itemId = page.episodeIndex.toString()
    val active = isCurrent && !viewState.isLoading && viewState.errorRes == null
    val buffering = active && !page.isLocked && playback.isBuffering && playback.playWhenReady
    val poster: @Composable () -> Unit = {
        AppAsyncImage(model = viewState.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Video dọc trên khung ngang (xoay máy): vừa khung, viền đen hai bên — crop thì chỉ còn một dải giữa hình.
        val videoScale = if (maxWidth > maxHeight) ContentScale.Fit else ContentScale.Crop
        if (slot != null && !page.isLocked) {
            VideoSurface(slot = slot, modifier = Modifier.fillMaxSize(), contentScale = videoScale, shutter = poster)
        } else {
            poster()
        }
        if (buffering) Box(Modifier.fillMaxSize().background(media.bufferingDim))
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.46f)
                .background(AppTheme.colors.scrimBottom),
        )
        if (active && !page.isLocked) {
            // Chạm vào video = play/pause; TalkBack đọc được hành động.
            val label = stringResource(if (playback.playWhenReady) R.string.watch_pause_cd else R.string.watch_play_cd)
            Box(
                Modifier
                    .fillMaxSize()
                    .clickableNoRipple(onClickLabel = label, role = Role.Button, onClick = onTogglePlay)
                    .semantics { contentDescription = label },
            )
        }
        val expanded = active && viewState.descriptionExpanded
        if (expanded) {
            // Mô tả mở rộng: nền tối hơn để đọc; chạm ra ngoài thì thu lại.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(media.bufferingDim)
                    .clickableNoRipple(onClick = onToggleDescription),
            )
        }

        Box(Modifier.fillMaxSize().graphicsLayer { alpha = overlayAlpha() }) {
            when {
                page.isLocked -> {
                    val unlockAd = viewState.unlockAd.takeIf { active }
                    CenterMessage(
                        title = stringResource(R.string.watch_locked_title, page.episodeNumber),
                        body = stringResource(if (unlockAd != null) R.string.watch_locked_body_ad else R.string.watch_locked_body),
                        actionLabel = stringResource(R.string.watch_episodes),
                        onAction = onEpisodesClick,
                        primaryAction = unlockAd?.let { state ->
                            {
                                RewardedUnlockButton(
                                    text = stringResource(R.string.watch_unlock_with_ad),
                                    state = state,
                                    onClick = onUnlockWithAdClick,
                                )
                                if (state == RewardedAdState.Unavailable) {
                                    Text(
                                        text = stringResource(R.string.watch_unlock_unavailable),
                                        style = AppTypography.caption,
                                        color = AppTheme.colors.media.textMuted,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        },
                    )
                }
                !active -> Unit
                viewState.sourceUnavailable -> CenterMessage(
                    title = stringResource(R.string.watch_error_no_source),
                    actionLabel = stringResource(R.string.watch_episodes),
                    onAction = onEpisodesClick,
                )
                viewState.hasPlaybackError -> CenterMessage(
                    title = stringResource(R.string.watch_error_playback),
                    actionLabel = stringResource(R.string.watch_retry),
                    onAction = onRetryClick,
                )
                viewState.endAction != null -> EndCard(
                    action = viewState.endAction,
                    onEpisodesClick = onEpisodesClick,
                    onEpisodeSelect = onEpisodeSelect,
                    onBackClick = onBackClick,
                )
                buffering -> BufferingOverlay(viewState.qualityDropped, onSwitchBack = onSwitchBackToHd)
                !playback.playWhenReady -> CenterPlayIcon(AppIcons.Play, Modifier.align(Alignment.Center))
            }
            if (!viewState.isLoading && viewState.errorRes == null) {
                BottomControls(
                    page = page,
                    isCurrent = isCurrent,
                    viewState = viewState,
                    expanded = expanded,
                    // Theo id tập, không theo `isCurrent`: khung đầu sau khi lật, player còn báo số đo của tập trước.
                    positionMs = { progress().positionFor(itemId) },
                    durationMs = if (playback.itemId == itemId && playback.durationMs > 0) playback.durationMs else page.durationMs,
                    buffering = buffering,
                    onToggleDescription = onToggleDescription,
                    onLikeClick = onLikeClick,
                    onSpeedClick = onSpeedClick,
                    onSpeedLongPress = onSpeedLongPress,
                    onEpisodesClick = onEpisodesClick,
                    onSeek = onSeek,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.BottomControls(
    page: PlayerPage,
    isCurrent: Boolean,
    viewState: PlayerViewState,
    expanded: Boolean,
    positionMs: () -> Long,
    durationMs: Long,
    buffering: Boolean,
    onToggleDescription: () -> Unit,
    onLikeClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSpeedLongPress: () -> Unit,
    onEpisodesClick: () -> Unit,
    onSeek: (positionMs: Long) -> Unit,
) {
    val gutter = AppTheme.dimensions.screenGutter
    // SCR-0604: đang buffering chỉ còn top bar + spinner + track mờ. Mờ dần thay vì gỡ khỏi layout: track không nhảy chỗ,
    // lần buffer ngắn (tua) không làm cả cụm chớp.
    val chromeAlpha by animateFloatAsState(if (buffering) 0f else 1f, label = "bufferingChrome")
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = gutter)
            .padding(bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.graphicsLayer { alpha = chromeAlpha }) {
            PlayerInfo(
                episodeNumber = page.episodeNumber,
                description = viewState.description,
                expanded = expanded,
                onToggle = { if (isCurrent && !buffering) onToggleDescription() },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = AppTheme.dimensions.s4),
            )
            if (!page.isLocked) {
                PlayerRail(
                    likeCount = formatCompactCount(page.likeCount, Locale.current.platformLocale),
                    isLiked = page.isLiked,
                    speed = viewState.speed,
                    onLike = { if (isCurrent && !buffering) onLikeClick() },
                    onSpeedClick = { if (isCurrent && !buffering) onSpeedClick() },
                    onSpeedLongPress = { if (isCurrent && !buffering) onSpeedLongPress() },
                    onEpisodes = { if (!buffering) onEpisodesClick() },
                    modifier = Modifier.padding(bottom = AppTheme.dimensions.s6),
                )
            }
        }
        if (!page.isLocked) {
            PlayerTrack(
                positionMs = positionMs,
                durationMs = durationMs,
                dimmed = buffering,
                seekable = isCurrent,
                onSeek = onSeek,
                modifier = Modifier.padding(top = AppTheme.dimensions.s3),
            )
        }
        EpisodesPill(
            onClick = { if (!buffering) onEpisodesClick() },
            modifier = Modifier
                .padding(top = AppTheme.dimensions.s2)
                .graphicsLayer { alpha = chromeAlpha },
        )
    }
}

/** SCR-0604: spinner + "Loading…"; dòng hạ quality chỉ khi Auto thật sự đã hạ. */
@Composable
private fun BoxScope.BufferingOverlay(qualityDropped: String?, onSwitchBack: () -> Unit) {
    val media = AppTheme.colors.media
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            color = media.text,
            trackColor = media.spinnerTrack,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(44.dp),
        )
        Text(
            text = stringResource(R.string.watch_loading),
            style = AppTypography.bodySemibold.copy(fontSize = 14.5.sp),
            color = media.text,
            modifier = Modifier.padding(top = 18.dp),
        )
        if (qualityDropped != null) {
            Text(
                text = stringResource(R.string.watch_quality_dropped, qualityDropped),
                style = AppTypography.caption.copy(fontSize = 12.5.sp),
                color = media.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 7.dp),
            )
            GhostPillButton(
                text = stringResource(R.string.watch_switch_back_hd),
                onClick = onSwitchBack,
                modifier = Modifier.padding(top = AppTheme.dimensions.s4),
            )
        }
    }
}

@Composable
private fun BoxScope.CenterSpinner(label: String?) {
    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            color = AppTheme.colors.media.text,
            trackColor = AppTheme.colors.media.spinnerTrack,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(44.dp),
        )
        if (label != null) Text(label, style = AppTypography.bodySemibold, color = AppTheme.colors.media.text)
    }
}

@Composable
private fun BoxScope.CenterMessage(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    body: String? = null,
    /** Nút chính phía trên nút phụ (mở tập bằng rewarded). */
    primaryAction: (@Composable () -> Unit)? = null,
) {
    val media = AppTheme.colors.media
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s2),
    ) {
        Text(
            text = title,
            style = AppTypography.titleSmall,
            color = media.text,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Text(body, style = AppTypography.label, color = media.textMuted, textAlign = TextAlign.Center)
        }
        if (primaryAction != null) {
            Spacer(Modifier.height(AppTheme.dimensions.s2))
            primaryAction()
        }
        GhostPillButton(text = actionLabel, onClick = onAction, modifier = Modifier.padding(top = AppTheme.dimensions.s2))
    }
}

/** Hết tập mà không tự sang được: tập kế khoá, user tắt autoplay, hoặc hết show. */
@Composable
private fun BoxScope.EndCard(
    action: EndAction,
    onEpisodesClick: () -> Unit,
    onEpisodeSelect: (index: Int) -> Unit,
    onBackClick: () -> Unit,
) {
    when (action) {
        is EndAction.NextLocked -> CenterMessage(
            title = stringResource(R.string.watch_locked_title, action.episodeNumber),
            body = stringResource(R.string.watch_locked_body),
            actionLabel = stringResource(R.string.watch_episodes),
            onAction = onEpisodesClick,
        )
        is EndAction.NextAvailable -> Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.watch_next_up, action.episodeNumber),
                style = AppTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AppTheme.colors.media.text,
            )
            GradientButton(
                text = stringResource(R.string.watch_play_next, action.episodeNumber),
                onClick = { onEpisodeSelect(action.index) },
                modifier = Modifier.padding(top = AppTheme.dimensions.s4),
                leadingIcon = {
                    Icon(AppIcons.Play, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                },
            )
        }
        EndAction.ShowFinished -> CenterMessage(
            title = stringResource(R.string.watch_show_finished),
            actionLabel = stringResource(R.string.watch_back),
            onAction = onBackClick,
        )
        is EndAction.PlayNext -> Unit
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun PlayerContentPreview() {
    AppTheme {
        Box(Modifier.background(AppTheme.colors.media.background)) {
            PlayerContent(
                viewState = PlayerViewState(
                    isLoading = false,
                    showTitle = "The Ex-Wife Returns",
                    description = "Lam Vy walks into the boardroom and every eye turns to her…",
                    episodeNumber = 13,
                    episodeTotal = 68,
                    likeCount = 24_100,
                    playback = PlaybackState(itemId = "12", isPlaying = true, playWhenReady = true, durationMs = 91_000),
                    pages = listOf(PlayerPage(episodeIndex = 12, isLocked = false, episodeNumber = 13, likeCount = 24_100)),
                    episodeIndex = 12,
                ),
                pagerState = rememberPagerState { 1 },
                progress = { PlaybackProgress(itemId = "12", positionMs = 42_000, durationMs = 91_000) },
                slots = emptyMap(),
                onBackClick = {},
                onRetryClick = {},
                onTogglePlay = {},
                onToggleDescription = {},
                onEpisodesClick = {},
                onUnlockWithAdClick = {},
                onSwitchBackToHd = {},
                onEpisodeSelect = {},
                onLikeClick = {},
                onSpeedClick = {},
                onSpeedLongPress = {},
                onSeek = {},
            )
        }
    }
}
