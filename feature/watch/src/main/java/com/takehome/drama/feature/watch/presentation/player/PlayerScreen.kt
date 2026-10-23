package com.takehome.drama.feature.watch.presentation.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.takehome.drama.core.designsystem.component.AppAsyncImage
import com.takehome.drama.core.designsystem.component.ToastState
import com.takehome.drama.core.media.EpisodeFeedPlayer
import com.takehome.drama.core.utils.extension.findActivity
import kotlin.math.abs
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takehome.drama.core.designsystem.component.AppToastHost
import com.takehome.drama.core.designsystem.component.GhostPillButton
import com.takehome.drama.core.designsystem.component.GradientButton
import com.takehome.drama.core.designsystem.component.rememberToastState
import com.takehome.drama.core.designsystem.ext.clickableNoRipple
import com.takehome.drama.core.designsystem.icon.AppIcons
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.core.media.PlaybackState
import com.takehome.drama.core.media.VideoSurface
import com.takehome.drama.core.navigation.core.NavigationRequest
import com.takehome.drama.core.ui.foundation.ext.CollectAsEffect
import com.takehome.drama.core.ui.foundation.format.formatCompactCount
import com.takehome.drama.domain.usecase.watch.EndAction
import com.takehome.drama.feature.watch.R
import com.takehome.drama.feature.watch.presentation.player.component.CenterPlayIcon
import com.takehome.drama.feature.watch.presentation.player.component.EpisodePickerSheet
import com.takehome.drama.feature.watch.presentation.player.component.EpisodesPill
import com.takehome.drama.feature.watch.presentation.player.component.PlaybackOptionsSheet
import com.takehome.drama.feature.watch.presentation.player.component.PlayerInfo
import com.takehome.drama.feature.watch.presentation.player.component.PlayerRail
import com.takehome.drama.feature.watch.presentation.player.component.PlayerTopBar
import com.takehome.drama.feature.watch.presentation.player.component.PlayerTrack
import com.takehome.drama.feature.watch.presentation.player.model.PlayerSheet

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onNavigate: (NavigationRequest) -> Unit,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    // Không `by`: chỉ thanh track đọc `.value` → tick 500ms không recompose cả màn.
    val positionMs = viewModel.positionMs.collectAsStateWithLifecycle()
    val firstFrameRendered by viewModel.player.firstFrameRendered.collectAsStateWithLifecycle()
    val toastState = rememberToastState()
    val pagerState = rememberPagerState { viewState.pages.size }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    viewModel.singleEvent.CollectAsEffect { event ->
        when (event) {
            PlayerSingleEvent.NavigateBack -> onNavigate(NavigationRequest.PopBackStack)
            is PlayerSingleEvent.ShowToast -> toastState.show(
                text = event.formatArg?.let { context.getString(event.messageRes, it) } ?: context.getString(event.messageRes),
                success = false,
            )
            is PlayerSingleEvent.ScrollToEpisode -> {
                val page = viewState.pages.indexOfFirst { it.episodeIndex == event.index }
                if (page >= 0) {
                    scope.launch {
                        // Tập liền kề: vuốt có animation như user tự vuốt; xa hơn thì nhảy thẳng.
                        if (abs(page - pagerState.currentPage) == 1) pagerState.animateScrollToPage(page) else pagerState.scrollToPage(page)
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
            positionMs = { positionMs.value },
            firstFrameRendered = firstFrameRendered,
            player = viewModel.player,
            toastState = toastState,
            onIntent = viewModel::processIntent,
        )
        // Sheet nằm ở cửa sổ riêng có scrim: khi sheet mở, toast hiện trong sheet (xem PlayerSheets).
        if (viewState.sheet == PlayerSheet.None) {
            AppToastHost(
                state = toastState,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 120.dp),
            )
        }
    }
}

@Composable
private fun PlayerContent(
    viewState: PlayerViewState,
    pagerState: PagerState,
    positionMs: () -> Long,
    firstFrameRendered: Boolean,
    player: EpisodeFeedPlayer?,
    toastState: ToastState,
    onIntent: (PlayerIntent) -> Unit,
) {
    val media = AppTheme.colors.media
    val playback = viewState.playback
    val buffering = playback.isBuffering && playback.playWhenReady && !viewState.isLocked
    val showsVideo = !viewState.isLoading && viewState.errorRes == null && !viewState.isLocked

    Box(Modifier.fillMaxSize()) {
        if (viewState.pages.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // Một player duy nhất: trang lân cận chỉ là poster, không dựng sẵn.
                beyondViewportPageCount = 0,
                key = { viewState.pages[it].episodeIndex },
            ) { page ->
                val item = viewState.pages[page]
                val isCurrent = item.episodeIndex == viewState.episodeIndex
                EpisodePage(
                    posterUrl = viewState.posterUrl,
                    player = player.takeIf { isCurrent && showsVideo },
                    showPoster = !(isCurrent && showsVideo && firstFrameRendered),
                    isPlaying = playback.playWhenReady,
                    onTogglePlay = { onIntent(PlayerIntent.TogglePlay) },
                )
            }
        }
        if (buffering) Box(Modifier.fillMaxSize().background(media.bufferingDim))

        Box(Modifier.fillMaxWidth().height(150.dp).background(AppTheme.colors.scrimTop))
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.46f)
                .background(AppTheme.colors.scrimBottom),
        )

        PlayerTopBar(
            title = viewState.showTitle,
            episodeNumber = viewState.episodeNumber,
            episodeTotal = viewState.episodeTotal,
            onBack = { onIntent(PlayerIntent.BackClick) },
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = AppTheme.dimensions.s2, end = AppTheme.dimensions.s3),
        )

        when {
            viewState.isLoading -> CenterSpinner(label = null)
            viewState.errorRes != null -> CenterMessage(
                title = stringResource(viewState.errorRes),
                actionLabel = stringResource(R.string.watch_retry),
                onAction = { onIntent(PlayerIntent.RetryClick) },
            )
            viewState.isLocked -> CenterMessage(
                title = stringResource(R.string.watch_locked_title, viewState.episodeNumber),
                body = stringResource(R.string.watch_locked_body),
                actionLabel = stringResource(R.string.watch_episodes),
                onAction = { onIntent(PlayerIntent.EpisodesClick) },
            )
            viewState.hasPlaybackError -> CenterMessage(
                title = stringResource(R.string.watch_error_playback),
                actionLabel = stringResource(R.string.watch_retry),
                onAction = { onIntent(PlayerIntent.RetryClick) },
            )
            viewState.endAction != null -> EndCard(viewState.endAction, onIntent)
            buffering -> BufferingOverlay(viewState.qualityDropped, onSwitchBack = { onIntent(PlayerIntent.SwitchBackToHd) })
            !playback.playWhenReady -> CenterPlayIcon(AppIcons.Play, Modifier.align(Alignment.Center))
        }

        if (!viewState.isLoading && viewState.errorRes == null) {
            BottomControls(viewState, positionMs, buffering, onIntent)
        }
    }

    when (viewState.sheet) {
        PlayerSheet.None -> Unit
        PlayerSheet.Episodes -> EpisodePickerSheet(
            cells = viewState.cells,
            ranges = viewState.ranges,
            selectedRange = viewState.selectedRange,
            watchedCount = viewState.watchedCount,
            toastState = toastState,
            onRangeSelect = { onIntent(PlayerIntent.RangeSelect(it)) },
            onEpisodeSelect = { onIntent(PlayerIntent.EpisodeSelect(it)) },
            onDismiss = { onIntent(PlayerIntent.DismissSheet) },
        )
        PlayerSheet.Options -> PlaybackOptionsSheet(
            speed = viewState.speed,
            qualities = viewState.qualities,
            quality = viewState.quality,
            languages = viewState.languages,
            language = viewState.language,
            autoplayNext = viewState.autoplayNext,
            toastState = toastState,
            onSpeedSelect = { onIntent(PlayerIntent.SpeedSelect(it)) },
            onQualitySelect = { onIntent(PlayerIntent.QualitySelect(it)) },
            onLanguageSelect = { onIntent(PlayerIntent.LanguageSelect(it)) },
            onAutoplayToggle = { onIntent(PlayerIntent.AutoplayToggle) },
            onDismiss = { onIntent(PlayerIntent.DismissSheet) },
        )
    }
}

/**
 * Một trang của pager: poster của show, video của tập hiện tại vẽ dưới poster và lộ ra khi đã có
 * khung hình đầu (không chớp đen lúc vuốt). [player] null = trang lân cận / khoá / lỗi.
 */
@Composable
private fun EpisodePage(
    posterUrl: String,
    player: EpisodeFeedPlayer?,
    showPoster: Boolean,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (player != null) VideoSurface(player = player, modifier = Modifier.fillMaxSize())
        if (showPoster) {
            AppAsyncImage(model = posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        if (player != null) {
            // Chạm vào video = play/pause; TalkBack đọc được hành động.
            val label = stringResource(if (isPlaying) R.string.watch_pause_cd else R.string.watch_play_cd)
            Box(
                Modifier
                    .fillMaxSize()
                    .clickableNoRipple(onClickLabel = label, role = Role.Button, onClick = onTogglePlay)
                    .semantics { contentDescription = label },
            )
        }
    }
}

@Composable
private fun BoxScope.BottomControls(
    viewState: PlayerViewState,
    positionMs: () -> Long,
    buffering: Boolean,
    onIntent: (PlayerIntent) -> Unit,
) {
    val gutter = AppTheme.dimensions.screenGutter
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = gutter)
            .padding(bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            PlayerInfo(
                episodeNumber = viewState.episodeNumber,
                description = viewState.description,
                expanded = viewState.descriptionExpanded,
                onToggle = { onIntent(PlayerIntent.ToggleDescription) },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = AppTheme.dimensions.s4),
            )
            if (!viewState.isLocked) {
                PlayerRail(
                    likeCount = formatCompactCount(viewState.likeCount, Locale.current.platformLocale),
                    isLiked = viewState.isLiked,
                    speed = viewState.speed,
                    onLike = { onIntent(PlayerIntent.LikeClick) },
                    onSpeedClick = { onIntent(PlayerIntent.SpeedClick) },
                    onSpeedLongPress = { onIntent(PlayerIntent.SpeedLongPress) },
                    onEpisodes = { onIntent(PlayerIntent.EpisodesClick) },
                    modifier = Modifier.padding(bottom = AppTheme.dimensions.s6),
                )
            }
        }
        if (!viewState.isLocked) {
            PlayerTrack(
                positionMs = positionMs,
                durationMs = viewState.playback.durationMs,
                dimmed = buffering,
                onSeek = { onIntent(PlayerIntent.SeekTo(it)) },
                modifier = Modifier.padding(top = AppTheme.dimensions.s3),
            )
        }
        EpisodesPill(
            onClick = { onIntent(PlayerIntent.EpisodesClick) },
            modifier = Modifier.padding(top = AppTheme.dimensions.s2),
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
        GhostPillButton(text = actionLabel, onClick = onAction, modifier = Modifier.padding(top = AppTheme.dimensions.s2))
    }
}

/** Hết tập mà không tự sang được: tập kế khoá, user tắt autoplay, hoặc hết show. */
@Composable
private fun BoxScope.EndCard(action: EndAction, onIntent: (PlayerIntent) -> Unit) {
    when (action) {
        is EndAction.NextLocked -> CenterMessage(
            title = stringResource(R.string.watch_locked_title, action.episodeNumber),
            body = stringResource(R.string.watch_locked_body),
            actionLabel = stringResource(R.string.watch_episodes),
            onAction = { onIntent(PlayerIntent.EpisodesClick) },
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
                onClick = { onIntent(PlayerIntent.EpisodeSelect(action.index)) },
                modifier = Modifier.padding(top = AppTheme.dimensions.s4),
                leadingIcon = {
                    Icon(AppIcons.Play, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                },
            )
        }
        EndAction.ShowFinished -> CenterMessage(
            title = stringResource(R.string.watch_show_finished),
            actionLabel = stringResource(R.string.watch_back),
            onAction = { onIntent(PlayerIntent.BackClick) },
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
                    playback = PlaybackState(isPlaying = true, playWhenReady = true, durationMs = 91_000),
                ),
                pagerState = rememberPagerState { 0 },
                positionMs = { 42_000 },
                firstFrameRendered = false,
                player = null,
                toastState = rememberToastState(),
                onIntent = {},
            )
        }
    }
}
